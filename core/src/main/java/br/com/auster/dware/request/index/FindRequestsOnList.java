package br.com.auster.dware.request.index;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import br.com.auster.common.io.NIOBufferUtils;
import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.utils.RequestUtils;

/**
 * Obtem requisições a partir do Indice. Esta classe é projetada para ser
 * dispatched como uma thread e buscar entradas em um único arquivo de indice.
 * Para isso cada registro do indice é pesquisado em posições especificas para
 * obtenção da User_key do request indexado.
 * 
 * A User Key é então convertida para texto e comparada com a lista de
 * requisições informadas pela Classe Controller. Se forem iguais, achou a
 * requisição, que é então retirada da lista de requisições desejadas. A Entrada
 * do indice é transformada em uma requisição que é adicionada a lista de
 * requisições encontradas.
 * 
 * @author mtengelm
 * @version $Id$
 * @since 22/12/2007
 */
public class FindRequestsOnList implements Serializable, Runnable,
		Callable<List<Request>> {

	/**
	 * Used to store the values of <code>serialVersionUID</code>.
	 */
	private static final long		serialVersionUID	= 1L;
	private static final Logger	log								= Logger
																										.getLogger(FindRequestsOnList.class);

	private static final I18n		i18n							= I18n
																										.getInstance(FindRequestsOnList.class);

	private static final Lock		lock							= new ReentrantLock();

	/** * Arquivo de INDICE que será pesquisado ** */
	private File								file;

	private List<Request>				requestList;

	/** * Lista Final de requisções (Global, de todas as threads) .** */
	private List<Request>				results;

	/**
	 * * Indica se no caso de erros de IO, a busca deverá continuar ou ser
	 * interrompida por esta thread **
	 */
	private boolean							ignoreIOErrors;

	/** * Tamanho do BUffer de Leitura do arquivo de Indices** */
	private int									bufferSize;

	/** * Charset no qual o texto do arquivo de indices foi armazenado ** */
	private Charset							charset;

	/**
	 * Creates a new instance of the class <code>FindRequestsOnList</code>.
	 * 
	 * @param results
	 *          A Lista onde esta instância deverá armazenar as requisições
	 *          encontradas no arquivo de indice.
	 */
	public FindRequestsOnList(List<Request> results) {
		this.results = results;
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			this.call();
		} catch (Exception e) {
			log.fatal(i18n.getString("error.unexpected"), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Faz a busca das requisições no indice.
	 * 
	 * @return Esta implementação sempre retorna NULOS.
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public List<Request> call() throws Exception {
		log.debug("Finding requests of the list on file:" + file);
		//TODO Create a method to look at pointer index file BEFORE any buffer allocation and index file reading. And try to discard entries on current index file.
		
		ByteBuffer bb = ByteBuffer.allocateDirect(bufferSize);

		ReadableByteChannel channel = NIOUtils.openFileForRead(file);

		// We are assuming Index Format Version 001.
		byte[] sep = new byte[] { ';' };
		byte[] rec = new byte[] { '\n' };

		int read;
		try {
		while ((read = channel.read(bb)) > 0) {
			bb.flip();
			while (bb.hasRemaining()) {
				if (!isAcceptableVersion(bb.get(0), bb.get(1), bb.get(2))) {
					StringBuilder sb = new StringBuilder();
					sb.append(bb.get(0));
					sb.append(bb.get(1));
					sb.append(bb.get(2));
					log.fatal(i18n.getString("", "001", sb.toString()));
					return null;
				}

				// Find record Separator
				int pos = NIOBufferUtils.findToken(bb, rec);
				if (pos == -1) {
					bb.compact();
					break;
				}

				// We have a record on buffer.Save it current position.
				int oldPosition = bb.position();
				// Saves the limit. We are gonna set this to decode.
				int oldLimit = bb.limit();
				
				String cc = extractUserKey(bb, sep);
				
				log.trace("Extract an Index Request:" + cc);
				// We are looking for this user Key?
				matchUserKey(bb, pos, oldPosition, cc);
				if (requestList.size() == 0) {
					channel.close();
					return null;
				}
				bb.limit(oldLimit);
				bb.position(pos + 1);
			} // Buffer
		} // File
		} catch (IOException e) {
			if (!ignoreIOErrors) {
				throw e;
			}
			log.fatal(i18n.getString("error.index.file", file.getName()),e);
		}
		if (bb.hasRemaining()) {
			bb.flip();	
			int oldLimit = bb.limit();
			int oldPosition = bb.position();		
			String userKey = extractUserKey(bb, sep);
			
			bb.limit(oldLimit);
			bb.position(oldPosition);
			matchUserKey(bb, oldLimit, oldPosition, userKey);
		}
		
		channel.close();
		return null;
	}

	/**
	 * 
	 * @param bb
	 * @param limitAfterMatch
	 * @param positionAfterMatch
	 * @param userKey
	 * @throws IOException
	 */
	private void matchUserKey(ByteBuffer bb, int limitAfterMatch, int positionAfterMatch, String userKey)
			throws IOException {
		if (isUserKeySuitable(userKey)) {
			// Sets the buffer to decode.					
			bb.limit(limitAfterMatch);
			bb.position(positionAfterMatch);
			Request toAdd = RequestUtils.createPartialFileRequestFromCharBuffer(charset
					.decode(bb));
			if (toAdd != null) {
				// Remove from requestList. We found it.
				requestList.remove(userKey);
				lock.lock();
				try {
					results.add(toAdd);
				} finally {
					lock.unlock();
				}
			}
		}
	}

	/**
	 * TODO what this method is responsible for
	 * <p>
	 * Example:
	 * <pre>
	 *    Create a use example.
	 * </pre>
	 * </p>
	 * 
	 * @param bb
	 * @param sep
	 * @return
	 */
	private String extractUserKey(ByteBuffer bb, byte[] sep) {
		// Find the second field (In Version 001 2nd field is the userKey)
		bb.position(NIOBufferUtils.findToken(bb, sep) + 1);
		// Find userKey final position.
		bb.limit(NIOBufferUtils.findToken(bb, sep));
		// Decodes User Key
		CharBuffer cc = charset.decode(bb);
		return cc.toString();
	}

	/*****************************************************************************
	 * 
	 * /** Decide se uma user key na lista de chaves desejadas foi encontrada.
	 * 
	 * @param string
	 *          User Key a ser pesquisada
	 * @return true se a User Key deve ser inserida na lista de encontrados ou
	 *         false caso contrário.
	 */
	protected boolean isUserKeySuitable(String userKey) {	
		for (Request request : requestList) {
			log.trace("RequestList UserKey:" + request.getUserKey());
			//TODO Here we can check if userKey is smaller then request. If so, we can leave.
			if (request.getUserKey().equals(userKey)) {
				log.trace("We got a match.");
				return true;
			}
		}
		return false;
	}

	/*****************************************************************************
	 * Decide se esta classe pode tratar a versão do indice, Cada versão de indice
	 * pode ter um layout diferente, exceto pelos 3 primeiros bytes, que são
	 * sempre o numero da versão.
	 * 
	 * @param b
	 *          Byte com a primeira posição do registro de indice
	 * @param c
	 *          Byte com a segunda posição do registro de indice
	 * @param d
	 *          Byte com a terceira posição do registro de indice
	 * @return Se a versão é suportada true, caso contrário false.
	 */
	protected boolean isAcceptableVersion(byte b, byte c, byte d) {
		// INDEX FORMAT Version 001
		if ((b == '0') && (c == '0') && (d == '1')) {
			return true;
		}
		return false;
	}

	public File getFile() {
		return this.file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public List<Request> getRequestList() {
		return this.requestList;
	}

	public void setRequestList(List<Request> requestList) {
		this.requestList = requestList;
	}

	/**
	 * 
	 * @param ignoreIOErrors
	 *          Indica se deve ignorar erros de IO
	 */
	public void setIgnoreIOErrors(boolean ignoreIOErrors) {
		this.ignoreIOErrors = ignoreIOErrors;
	}

	/**
	 * 
	 * @param bufferSize
	 */
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	/**
	 * 
	 * @param charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

}
