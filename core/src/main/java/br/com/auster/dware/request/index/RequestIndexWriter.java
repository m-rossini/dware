/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created on 05/12/2007
 */
package br.com.auster.dware.request.index;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharsetEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.filter.FilenameBuilder;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.request.file.PartialFileRequest;
import br.com.auster.dware.request.utils.RequestUtils;

/**
 * Esta classe faz a gravação de entradas de indice de requisições.
 * Esta classe NÃO é thread safe e sincronização externa deverá ser feita nos métodos:
 * {@link #buildAndPrepare(PartialFileRequest, CharBuffer, ByteBuffer, CharsetEncoder)}
 * {@link #closeAllChannels()}
 * {@link #getChannel(PartialFileRequest)}
 * {@link #prepareBuffers(ByteBuffer, CharBuffer, CharsetEncoder)}
 * {@link #write(ByteBuffer, PartialFileRequest)}
 * 
 * A Classe foi projetada para gravar TODAS as entradas de TODOS os arquivos de indice.
 * 
 * Cada entrada de indice corresponde a uma requisição.
 * 
 * O Nome do arquivo no qual a entrada será grava depende da configuração informada.
 * 
 * Esta configuração utiliza a classe {@linkplain FilenameBuilder} e portanto depende de atributos da requisição.
 * 
 * O controle do número de arquivos abertos é feita pela classe. Caso uma quantidade seja informada no contrutor, a mesma
 * será respeitada. Caso não seja informada, ou seja inválida, o número {@value #DEFAULT_MAX_OPEN_FILES} será assumido.
 * 
 *  A Versão 001 de indice será gravada (Melhoria Futura: Configurar versões distintas)
 * 
 * Esta classe suporta arquivos de indices comprimidos e também append mode via configuração.
 * 
 * @author mtengelm
 * @version $Id$
 */
public class RequestIndexWriter {
	
	private class ChannelLRUMap extends LRUMap {
		public ChannelLRUMap() {
			super();
		}

		public ChannelLRUMap(int maxSize, boolean scanUntilRemovable) {
			super(maxSize, scanUntilRemovable);
		}

		public ChannelLRUMap(int maxSize, float loadFactor, boolean scanUntilRemovable) {
			super(maxSize, loadFactor, scanUntilRemovable);
		}

		public ChannelLRUMap(int maxSize, float loadFactor) {
			super(maxSize, loadFactor);
		}

		public ChannelLRUMap(int maxSize) {
			super(maxSize);
		}

		public ChannelLRUMap(Map map, boolean scanUntilRemovable) {
			super(map, scanUntilRemovable);
		}

		public ChannelLRUMap(Map map) {
			super(map);
		}

		protected void removeEntry(HashEntry entry, int hashIndex, HashEntry previous) {
			WritableByteChannel value = (WritableByteChannel) entry.getValue();
			if (value != null) {
				try {
					value.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
			super.removeEntry(entry, hashIndex, previous);
		}

		public void clear() {
			HashEntry[] data = this.data;
			for (int i = data.length - 1; i >= 0; i--) {
				if (data[i] == null) {
					continue;
				}
				WritableByteChannel value = (WritableByteChannel) data[i].getValue();
				if (value != null) {
					try {
						value.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			super.clear();
		}
	}

	private final Logger											log											= Logger
																																				.getLogger(RequestIndexWriter.class);
	private final I18n												i18n										= I18n
																																				.getInstance(RequestIndexWriter.class);

	public static final String								VERSIONID								= "001";
	public static final int										DEFAULT_MAX_OPEN_FILES	= 50;

	private String														baseDir;
	private FilenameBuilder										fileNameBuilder;
	private boolean														compress;
	private boolean														append;
	private int																maxOpenFiles;
	private long															recordsWritten;

	private Map<String, WritableByteChannel>	indexFiles;
	private Set<String>												alreadyCreatedFiles;

	/**
	 * Creates a new instance of the class <code>RequestIndexWritter</code>.
	 */
	public RequestIndexWriter(int maxOpenFiles) {
		baseDir = null;
		fileNameBuilder = null;
		recordsWritten = 0;
		compress = true;
		this.maxOpenFiles = (maxOpenFiles <= 0) ? DEFAULT_MAX_OPEN_FILES : maxOpenFiles;
		alreadyCreatedFiles = new HashSet<String>();
		indexFiles = new ChannelLRUMap(this.maxOpenFiles);
	}

	/**
	 * Return the value of a attribute <code>baseDir</code>.
	 * 
	 * @return return the value of <code>baseDir</code>.
	 */
	public String getBaseDir() {
		return this.baseDir;
	}

	/**
	 * Set the value of attribute <code>baseDir</code>.
	 * 
	 * @param baseDir
	 */
	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	/**
	 * Return the value of a attribute <code>fileNameBuilder</code>.
	 * 
	 * @return return the value of <code>fileNameBuilder</code>.
	 */
	public FilenameBuilder getFileNameBuilder() {
		return this.fileNameBuilder;
	}

	/**
	 * Set the value of attribute <code>fileNameBuilder</code>.
	 * 
	 * @param fileNameBuilder
	 */
	public void setFileNameBuilder(FilenameBuilder fileNameBuilder) {
		this.fileNameBuilder = fileNameBuilder;
	}

	/**
	 * Grava uma entrada no indice.
	 * 
	 * @param indexByteBuffer 
	 * @param request
	 * @throws FilterException
	 * @throws IOException
	 */
	public void write(ByteBuffer indexByteBuffer, PartialFileRequest request)
			throws FilterException, IOException {

		WritableByteChannel channel = getChannel(request);
		channel.write(indexByteBuffer);
		recordsWritten++;
	}

	/**
	 * Este metodo obtem um channel para gravação do request informado.
	 * 
	 * Aqui é feita a validação de nome, verificação de quantidade maxima de arquivo abertos.
	 * 
	 * Também é realizado o tratamento de arquivos comprimidos e eventualmente de append mode no arquivo de indice.
	 * 
	 * @param request O Request de cujos atributos será derivado o nome do arquivo no qual o mesmo será gravado.
	 * @return Um channel que será utilizado para gravar a requisição.
	 * @throws IOException
	 * @throws FilterException
	 * @throws FileNotFoundException
	 */
	protected WritableByteChannel getChannel(PartialFileRequest request)
			throws IOException, FilterException, FileNotFoundException {

		String filename = getFileName(request);

		WritableByteChannel channel = null;
		if (!this.indexFiles.containsKey(filename)) {
			File out = new File(filename);
			out.getParentFile().mkdirs();
			boolean appendable = (alreadyCreatedFiles.contains(filename)) ? true : isAppend();
			alreadyCreatedFiles.add(filename);
			FileOutputStream fos = new FileOutputStream(out, appendable);
			if (compress) {
				GZIPOutputStream gos = new GZIPOutputStream(fos);
				channel = Channels.newChannel(new BufferedOutputStream(gos));
			} else {
				channel = Channels.newChannel(new BufferedOutputStream(fos));
			}
			WritableByteChannel toClose = this.indexFiles.put(filename, channel);
			if (toClose != null) {
				toClose.close();
			}
		}
		return this.indexFiles.get(filename);
	}

	/**
	 * Define o nome do arquivo no qual uma entrada de indice da requisição será gravada.
	 * 
	 * @param request Requisição que determinará o arquivo no qual a mesma será gravada
	 * @return filename. Uma String representando o nome do arquivo no qual a entrada de indice da requisição será gravada 
	 * @throws IOException
	 * @throws FilterException
	 */
	private String getFileName(PartialFileRequest request) throws IOException,
			FilterException {

		String filename = "";
		File baseDir = new File(getBaseDir());
		if (baseDir.isDirectory()) {
			filename = baseDir.getCanonicalPath();
		} else {
			filename = baseDir.getParent();
		}

		filename += System.getProperty("file.separator")
				+ this.fileNameBuilder.getFilename(request);
		return filename;
	}

	/***
	 * Onbtém uma lista de todos os channels correntemente abertos e mantidos pela gravador de indices. 
	 * 
	 * @return Collections.unmodifiableCollection com todos channels abertos no momento. 
	 */
	public Collection<WritableByteChannel> getAllChannels() {
		return Collections.unmodifiableCollection(this.indexFiles.values());
	}

	/*****************************************************************************
	 * Um Set<String> com todos os nomes de arquivos que terão entradas de indices gravadas.
	 * 
	 * @return Collections.unmodifiableSet com nomes de arquivos.
	 */
	public Set<String> getAllFileNames() {
		return Collections.unmodifiableSet(this.indexFiles.keySet());
	}

	/*****************************************************************************
	 * Dado um nome de arquivo, retorna o seu channel.
	 * 
	 * @param filename
	 * @return WritableByteChannel 
	 */
	public WritableByteChannel getChannelByFileName(String filename) {
		return this.indexFiles.get(filename);
	}

	/*****************************************************************************
	 * Fecha todos os channels que possam estar abertos.
	 * 
	 * @throws IOException
	 */
	public void closeAllChannels() throws IOException {
		for (Iterator<WritableByteChannel> it = this.indexFiles.values().iterator(); it
				.hasNext();) {
			WritableByteChannel channel = it.next();
			if (channel.isOpen()) {
				channel.close();
			}
		}
	}

	/**
	 * Return the value of a attribute <code>compress</code>.
	 * 
	 * @return return the value of <code>compress</code>.
	 */
	public boolean isCompress() {
		return this.compress;
	}

	/**
	 * Set the value of attribute <code>compress</code>.
	 * 
	 * @param compress
	 */
	public void setCompress(boolean compress) {
		this.compress = compress;
	}

	/**
	 * Formata a requisição para gravação.
	 * Ver também o método {@link #prepareBuffers(ByteBuffer, CharBuffer, CharsetEncoder)}
	 * 
	 * @param request Requisição a ser gravada
	 * @param indexCharBuffer Char Buffer para formatação
	 * @param indexByteBuffer ByteBuffer para gravação
	 * @param indexEncoder Codificador para transforma ro Char em um Byte Buffer.
	 */
	public void buildAndPrepare(PartialFileRequest request, CharBuffer cc, ByteBuffer bb,
			CharsetEncoder encoder) {
		// Create CharBuffer with data
		RequestUtils.formatFullRecord(request, cc, VERSIONID);
		prepareBuffers(bb, cc, encoder);
	}

	/***
	 * Este metodo recebe um CharBuffer um ByteBuffer e um encoder para gerar uma entrada no indice.
	 * O ByteBuffer é limpo (clear()) , e encoded e na saida do metodo sofre o flip().
	 * O Encoder é resetado no inicio do metodo e ao final é flushed.
	 * O CharBuffer sofre o flip() no inicio do metodo, e ao final é populado com o resultado do encode.
	 * 
	 * @param bb
	 * @param cc
	 * @param encoder
	 */
	private void prepareBuffers(ByteBuffer bb, CharBuffer cc, CharsetEncoder encoder) {
		bb.clear();
		encoder.reset();
		cc.flip();
		encoder.encode(cc, bb, true);
		encoder.flush(bb);
		bb.flip();
	}

	/**
	 * Return the value of a attribute <code>append</code>.
	 * 
	 * @return return the value of <code>append</code>.
	 */
	public boolean isAppend() {
		return this.append;
	}

	/**
	 * Set the value of attribute <code>append</code>.
	 * 
	 * @param append
	 */
	public void setAppend(boolean append) {
		this.append = append;
	}

	/*****************************************************************************
	 * 
	 * 
	 * @return int com o numero maximo de arquivos abertos para esta instância
	 */
	public int getMaxOpenFiles() {
		return this.maxOpenFiles;
	}

	public long getRecordsWritten() {
		return this.recordsWritten;
	}
}
