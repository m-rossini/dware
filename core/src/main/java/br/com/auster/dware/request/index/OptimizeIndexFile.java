package br.com.auster.dware.request.index;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;
import br.com.auster.common.io.NIOBufferUtils;
import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.comparators.RequestUserKeyComparator;
import br.com.auster.dware.request.file.PartialFileRequest;
import br.com.auster.dware.request.utils.RequestUtils;

/**
 * Esta classe otimiza os indices gerados pelo request builder.
 * 
 * A Otimiza��o realizada � b�sica e suporta apenas a vers�o 001 do indice.
 * 
 * Quando outras vers�es e layouts forem incorporados esta classe dever� ser
 * alterada.
 * 
 * Esta classe � thread-safe e projetada para trabalhar com um �nico arquivo de
 * indice.
 * 
 * Esta classe suporta arquivos compriidos e n�o comprimidos, de acordo com sua
 * extens�o. GZ e ZIP (Any Letter Case) s�o considerados como arquivos
 * comprimidos e o restante como n�o comprimidos.
 * 
 * A Otimiza��o realizada, � basicamente ler o arquivo de indice e transformar
 * cada entrada do indice em um request.
 * 
 * Os requests ent�o s�o ordenados por user_key (Melhoria Futura: Permitir
 * Ordena��o por outros campos via configura��o), em order crescente (Melhoria
 * Futura: Permitir ordena��o ASC e DSC por configura��o).
 * 
 * O arquivo utilizado como fonte de entrada � ent�o transferido para outro com
 * nome "OLD_"<nome do arquivo>. (Melhoria Futura: Permitir configura��o do
 * nome do arquivo antigo).
 * 
 * O Resultado desta ordena��o � ent�o gravada em OUTRO arquivo de indice
 * (Respeitando se comprimido ou n�o).
 * 
 * Finalmente um arquivo com extens�o ".pointer" (Melhoria Futura: Permitir
 * configura��o da extens�o) � tamb�m gerado.
 * 
 * Este arquivo contem duas entradas. A Primeira com a requisi��o cuja user_key
 * � a menor do arquivo e outra com uma entrada para a maior requisi��o do
 * arquivo correspondente.
 * 
 * 
 * @author mtengelm
 * @version $Id$
 * @since 17/12/2007
 */
public class OptimizeIndexFile implements Callable<List<Request>>, Runnable {
	private final I18n					i18n											= I18n
																														.getInstance(OptimizeIndexFile.class);
	private static final Logger	log												= Logger
																														.getLogger(OptimizeIndexFile.class);

	// TODO Document this variables.
	public static final String	TYPE_HEADER								= "HEADER0001";
	public static final String	TYPE_TRAILER							= "TRAILER0001";
	public static final char		SEP												= ';';
	public static final String	INDEX_FORMAT_VERSION_001	= "001";

	private int									bSize;
	private Charset							charset;
	private File								file;
	private boolean							compressed;

	/**
	 * Creates a new instance of the class <code>OptimizeIndexFile</code>.
	 * 
	 * @param file
	 */
	public OptimizeIndexFile(File file) {
		this.file = file;
	}

	/**
	 * 
	 * @return List<Request>. Uma lista com as requisi��es obtidas no arquivo de
	 *         indice, j� ordenada por user_key.
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public List<Request> call() throws Exception {

		final ByteBuffer bb = ByteBuffer.allocate(bSize);
		final CharsetDecoder indexDecoder = this.charset.newDecoder();
		final CharBuffer cc = CharBuffer.allocate((int) (bSize * indexDecoder
				.averageCharsPerByte()));
		ReadableByteChannel channel = NIOUtils.openFileForRead(file);

		compressed = isFileCompressed(file);

		List<Request> founds = buildRequestList(bb, indexDecoder, cc, channel);

		// Ascending. For descending use false at constructor.
		RequestUserKeyComparator comp = new RequestUserKeyComparator(true);
		Collections.sort(founds, comp);

		channel.close();

		try {
			transferData(file, getRenamedFile(file, "OLD_", ""));
		} catch (IOException e) {
			log.fatal(i18n.getString("error.transfer"));
			throw e;
		}

		try {
			writeFile(file, founds, compressed);
		} catch (IOException e) {
			log.fatal(i18n.getString("error.index.file", file.getName()), e);
			throw e;
		}

		return founds;
	}

	/**
	 * Dada uma lista de requisi��es este m�todo grava o arquivo de indice dessas
	 * requisi��es.
	 * 
	 * @param file
	 *          Arquivo onde o indice ser� gravado
	 * @param founds
	 *          Lista de requisi��es
	 * @param comp
	 *          Se o arquivo dever� ser ou n�o comprimido. True Ser� comprimido.
	 * @throws IOException
	 */
	private void writeFile(File file, List<Request> founds, boolean comp)
			throws IOException {
		File pointer = getRenamedFile(file, "", ".pointer");
		writePointerFile(founds, pointer);

		// Create Output Channel
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
			log.fatal(i18n.getString("error.index.file", file.getName()), e);
			throw e;
		}

		WritableByteChannel channel;
		GZIPOutputStream gos = null;
		if (comp) {
			try {
				gos = new GZIPOutputStream(fos);
				channel = Channels.newChannel(gos);
			} catch (IOException e) {
				log.fatal(i18n.getString("error.index.file", file.getName()), e);
				throw e;
			}
		} else {
			channel = fos.getChannel();
		}

		// Allocate Buffers
		ByteBuffer bb = ByteBuffer.allocateDirect(bSize);
		CharsetEncoder encoder = charset.newEncoder();
		CharBuffer cc = CharBuffer.allocate((int) (bSize * encoder.averageBytesPerChar()));
		
		for (Request req : founds) {			
			RequestUtils.formatFullRecord((PartialFileRequest) req, cc,
					INDEX_FORMAT_VERSION_001);
			cc.flip();
			encoder.encode(cc, bb, true);
			encoder.flush(bb);
			encoder.reset();
			bb.flip();
			channel.write(bb);
			bb.clear();
			cc.clear();
		}

		if (comp) {
			gos.flush();
			gos.close();
		}
		fos.flush();
		fos.close();
		channel.close();
	}

	/**
	 * Grava arquivo de Pointer para o arquivo de indice. O Arquivo de pointer
	 * contem a entrada de indice cuja user key � a menor e a maior do arquivo do
	 * indice.
	 * 
	 * Estas requisis��es ser�o a primeira e a ultima da lista informada para o
	 * metodo respectivamente, portanto a lista informada j� dever� estar
	 * ordenada.
	 * 
	 * @param founds
	 *          Lista de Requisi��es ordenada
	 * @param pointer
	 *          Arquivo onde o pointer ser� armazenado.
	 */
	private void writePointerFile(List<Request> founds, File pointer) {
		try {
			FileWriter fos = new FileWriter(pointer);
			BufferedWriter bos = new BufferedWriter(fos);

			StringBuffer sb = new StringBuffer();

			// Creates Header Pointer
			sb.append(TYPE_HEADER);
			sb.append(SEP);
			sb.append(founds.get(0).getUserKey());
			sb.append(System.getProperty("line.separator"));
			// Write Header
			bos.write(sb.toString());

			// Creates pointer trailer
			sb.setLength(0);
			sb.append(TYPE_TRAILER);
			sb.append(SEP);
			sb.append(founds.get(founds.size() - 1).getUserKey());
			sb.append(System.getProperty("line.separator"));
			// Write Trailer
			bos.write(sb.toString());

			bos.flush();
			fos.flush();
			bos.close();
			fos.close();
		} catch (FileNotFoundException e) {
			// we can proceed....not a big deal.
			log.error(i18n.getString("error.index.file", pointer.getName()), e);
		} catch (IOException e) {
			// we can proceed....not a big deal.
			log.error(i18n.getString("error.index.file", pointer.getName()), e);
		}
	}

	/*****************************************************************************
	 * 
	 * @param bb
	 *          Buffer de leitura
	 * @param indexDecoder
	 *          Decodificador de Bytes para Chars
	 * @param cc
	 *          Buffer de Caracteres decodificados
	 * @param channel
	 *          Canal de Leitura do arquivo de indice
	 * @throws IOException
	 * @return Lista de Requisi��es Criadas.
	 * @throws IOException
	 */
	public List<Request> buildRequestList(final ByteBuffer bb,
			final CharsetDecoder indexDecoder, final CharBuffer cc, ReadableByteChannel channel)
			throws IOException {

		List<Request> founds = new ArrayList<Request>();

		int readBytes = 0;
		int recordsFound = 0;
		int physicalReads = 0;
		boolean doit = true;
		
		while (doit) {
			int read = channel.read(bb);
			physicalReads++;
			readBytes += read;
			if ((read <= 0 && bb.hasRemaining())) {
				//Last record without a record separator.
				bb.flip();
				decodeByteBuffer(bb, indexDecoder, cc);
				
				recordsFound++;
				
				cc.flip();
				
				Request req=extractRequest(cc);

				if (req != null) {
					founds.add(req);
				} else {
					log.trace("Request is NULL.");
				}
				
				doit=false;
				continue;
			}
			
			bb.flip();
			while (bb.hasRemaining()) {
				int pos = NIOBufferUtils.findToken(bb, new byte[] { '\n' });
				if (pos == -1) {
					bb.compact();
					break;
				}
				
				int oldLimit = bb.limit();
				bb.limit(pos);
				
				decodeByteBuffer(bb, indexDecoder, cc);

				recordsFound++;
				
				cc.flip();
				
				Request req=extractRequest(cc);

				if (req != null) {
					founds.add(req);
				} else {
					log.trace("Request is NULL.");
				}
				
				bb.limit(oldLimit);
				bb.position(pos + 1);
			} // Buffer Management
		} // Physical Read

		log.debug("Records Found:" + recordsFound);
		log.debug("Request Created:" + founds.size());
		return founds;
	}

	/**
	 * @param cc
	 * @param req
	 * @return
	 */
	protected Request extractRequest(final CharBuffer cc) {
		Request req=null;
		try {
			req = RequestUtils.createPartialFileRequestFromCharBuffer(cc);
			cc.clear();
		} catch (IOException e) {
			//Here we can ignore the request if not created.
			log.fatal(i18n.getString("error.",cc.toString()),e);
		}
		return req;
	}

	/**
	 * 
	 * @param bb
	 * @param indexDecoder
	 * @param cc
	 */
	protected void decodeByteBuffer(final ByteBuffer bb, final CharsetDecoder indexDecoder,
			final CharBuffer cc) {
		
		indexDecoder.decode(bb, cc, true);
		indexDecoder.flush(cc);
		indexDecoder.reset();
		
	}

	/**
	 * Copia o conteudo do arquivo entrada para o arquivo sa�da.
	 * 
	 * @param entrada
	 * @param saida
	 * @throws IOException
	 */
	private void transferData(File ori, File dst) throws IOException {
		FileInputStream fis = new FileInputStream(ori);
		FileChannel oriChannel = fis.getChannel();

		FileOutputStream fos = new FileOutputStream(dst);
		FileChannel dstChannel = fos.getChannel();

		dstChannel.transferFrom(oriChannel, 0, oriChannel.size());

		dstChannel.close();
		oriChannel.close();
	}

	/*****************************************************************************
	 * Gera um nome de arquivo. Utiliza o prefix ANTES do nome (E ap�s o path) e
	 * sufix apos o nome (Ap�s a extens�o) para gerar um novo nome.
	 * 
	 * @param file
	 * @param prefix
	 * @param sufix
	 * @return File. Retorna um novo File com o nome gerado.
	 */
	private File getRenamedFile(File file, String prefix, String sufix) {
		StringBuilder sb = new StringBuilder();
		sb.append(file.getParent());
		sb.append(System.getProperty("file.separator"));
		sb.append(prefix);
		sb.append(file.getName());
		sb.append(sufix);
		return new File(sb.toString());
	}

	/**
	 * Determina se um arquivo est� ou n�o comprimido baseado ems eu nome. Se a
	 * extens�o do arquivo terminar em GZ ou ZIP (Maisuculo ou minusculo) oo
	 * arquivo ser� considerado como comprimido.
	 * 
	 * @param file
	 * @return true se comprimido ou false caso contr�rio.
	 */
	protected boolean isFileCompressed(File file) {
		if (file.getName().endsWith(".gz") || file.getName().endsWith(".GZ")
				|| file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP")) {
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			this.call();
		} catch (Exception e) {
			log.fatal(i18n.getString("error.unexpected"));
		}
	}

	/**
	 * 
	 * @param size
	 */
	public void setBufferSize(int size) {
		this.bSize = size;
	}

	/**
	 * 
	 * @param charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public boolean isCompressed() {
		return this.compressed;
	}

}
