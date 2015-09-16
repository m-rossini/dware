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
 * Created on 27/11/2007
 */
package br.com.auster.dware.request.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.log4j.Logger;
import br.com.auster.common.io.NIOBufferUtils;
import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestFilter;
import br.com.auster.dware.request.file.PartialFileRequest;
import br.com.auster.dware.request.index.RequestIndexWriter;
import br.com.auster.dware.request.utils.EncodedString;
import br.com.auster.dware.request.utils.FieldDefinition;
import br.com.auster.dware.request.utils.FileBuilderArgs;

/**
 * Esta classe procura requisições dentro de um arquivo.
 * É um desmembramento da classe {@link #FileRequestBuilder} que trata um único arquivo.
 * Projetada para executar em ambiente multi-thread, esta classe é thread-safe.
 * 
 * @author mtengelm
 * @version $Id$
 * @since JDK1.4
 */
public class FindRequests implements Callable<RequestFilter>, Runnable {

	private final I18n										i18n								= I18n
																																.getInstance(FindRequests.class);

	private final Logger									log									= Logger
																																.getLogger(FindRequests.class);

	/*** Filtro onde serão armazenados as requisições encontradas ***/
	private RequestFilter									requestFilter;
	
	/*** Arquivo no qual as requisições serão obtidas ***/
	private File													file;
	
	/*** Classe controladora, e que iniciou esta thread ***/
	private MultiThreadFileRequestBuilder	builder;
	
	/*** Argumentos de arquivos ***/
	private FileBuilderArgs								params;

	/*** Quantidade de entradas no índice de requisições gravadas por esta classe ***/
	private int														indexRecordCounter	= 0;

	/*** Classe que fará a gravação de entradas no índice de requisições ***/
	private RequestIndexWriter						requestIndexWriter;

	/*** Representação em bytes do delimitador de registros físicos do arquivo a ser processado ***/
	private byte[]												recordDelimiter;

	/*** Flag que pode ser alterada pelo Controller e indica se esta classe deve encerrar seu processamento ***/
	private AtomicBoolean									canRun;

	/*** Lock para sincronização ***/
	private static final Lock							lock								= new ReentrantLock();


	/***
	 * 	/**
	 * Creates a new instance of the class <code>FindRequests</code>.
	 * 
	 * @param builder A Classe que fez o dispatch desta thread. 
	 * 	Esta classe guarda a referência ao Dispatcher até o final do processamento dela mesma.
	 * @param requestFilter Filtro onde as requisições encontradas serão armazenadas
	 * @param file Arquivo onde as requisições serão procuradas
	 * @param params Parametros de controle do processo.
	 * @param delimiter Delimitador de campos no arquivo.
	 * @param requestIndexWriter O Gravador de Indices
	 * @param canRun Se a classe pode ou não continuar o processamento.
	 */
	public FindRequests(MultiThreadFileRequestBuilder builder, RequestFilter requestFilter,
			File file, FileBuilderArgs params, byte[] delimiter,
			RequestIndexWriter requestIndexWriter, AtomicBoolean canRun) {
		this.requestFilter = requestFilter;
		this.file = file;
		this.builder = builder;
		this.params = params;
		this.requestIndexWriter = requestIndexWriter;
		this.recordDelimiter = delimiter;
		this.canRun = canRun;
	}

	/**
	 * Metodo que efetivamente faz a identificação de requisições.
	 * O Metodo irá 
	 * 
	 * @return
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public RequestFilter call() throws Exception {
		log.info(i18n.getString("parsingFile", file.getName()));

		// request attributes
		Map<String, String> atts = new HashMap<String, String>();
		// record attributes
		Map<String, String> recordAtts = new HashMap<String, String>();

		final ReadableByteChannel reader;
		try {
			try {
				reader = NIOUtils.openFileForRead(file);
			} catch (IOException e) {
				if (!builder.getIgnoreIOErrors()) {
					throw new RuntimeException(i18n.getString("problemOpeningFile"), e);
				}
				log.error("Error processing file.File:" + file.getAbsolutePath(), e);
				return null;
			}

			final CharBuffer record = CharBuffer.allocate(builder.getMaxRecordSize());
			final ByteBuffer inputBuffer = ByteBuffer.allocate(builder.getBufferSize());

			final Charset charset = Charset.forName(builder.getCharsetName());
			final CharsetDecoder decoder = charset.newDecoder();

			record.clear();
			atts.clear();
			decoder.reset();

			// Reads all the input, mapping where each request starts and how much is
			// its length
			String pendingRequestKey = null;
			int recordLimit = 0;
			int recordLength = 0;
			long bytesRead, fileOffset = 0, pendingRequestOffset = 0, pendingRequestLength = 0;
			boolean hasPendingRequest = false;

			List<Request> toProcessRequests = new ArrayList<Request>(1000);
			boolean hasIOErrors = false;
			try {
				for (;;) {
					final int currentPos = inputBuffer.position();
					boolean isEOF = (bytesRead = read(reader, inputBuffer)) <= currentPos;
					inputBuffer.flip();

					for (;;) {
						final ByteBuffer rawRecord;

						// try to find a record delimiter
						recordLimit = NIOBufferUtils.findToken(inputBuffer, recordDelimiter);
						if (recordLimit < 0) {
							if (isEOF && inputBuffer.hasRemaining()) {
								// process last record (assume implicit record delimiter at EOF)
								rawRecord = inputBuffer.slice();
								recordLength = rawRecord.remaining();
							} else {
								break;
							}
						} else {
							final int limit = inputBuffer.limit();
							inputBuffer.limit(recordLimit);
							rawRecord = inputBuffer.slice();
							recordLength = rawRecord.remaining() + recordDelimiter.length;
							inputBuffer.limit(limit);
							inputBuffer.position(recordLimit + recordDelimiter.length);
						}

						// get record key
						final EncodedString recordKey = new EncodedString(
								getRecordKeyToken(rawRecord), builder.getCharsetName());

						if (recordKey.getData() != null) {
							// search for new request
							final boolean isNewRequest = Arrays.equals(recordKey.getData(), builder
									.getNewRequestToken().getData());
							if (isNewRequest || builder.isAnyRecordIsNewRequest()) {
								if (hasPendingRequest) {
									lock.lock();
									try {
										builder.getStaticAttributes().insertStatics(atts);
										toProcessRequests.add(createRequest(pendingRequestKey,
												pendingRequestOffset, pendingRequestLength, file, atts, params));
									} finally {
										lock.unlock();
									}
									pendingRequestOffset += pendingRequestLength;
									pendingRequestLength = 0;
									hasPendingRequest = false;
								}
								pendingRequestKey = null;
								hasPendingRequest = true;
							} // IF NEW REQUEST

							if ((builder.getDesiredRecords().contains(recordKey))
									|| (builder.isAnyRecordHasRequestKey())) {
								rawRecord.rewind();
								decoder.reset();
								decoder.decode(rawRecord, record, true);
								decoder.flush(record);
								record.flip();
								// we will manipulate the record's content as a String,
								// otherwise
								// we would have to worry about the record's position everytime.
								final CharSequence content = record.subSequence(record.position(), record
										.limit());

								// optimization: if we need to split fields, let's do it only
								// once!
								final List<CharSequence> fields = (builder.getFieldDelimiter().length != 0 ? getFields(content)
										: null);

								// prepare to read attributes
								recordAtts.clear();

								// parse record attributes
								if (builder.getRecordDefinitions().containsKey(builder.getWildCard())) {
									List<FieldDefinition> fieldDefs = this.builder.getRecordDefinitions()
											.get(builder.getWildCard());
									parseAttributes(record, fieldDefs, fields, recordAtts);
								}
								if (builder.getRecordDefinitions().containsKey(recordKey)) {
									List<FieldDefinition> fieldDefs = builder.getRecordDefinitions().get(
											recordKey);
									parseAttributes(record, fieldDefs, fields, recordAtts);
								}

								// try to fetch the requestKey
								final String requestKey = getRequestKey(content, recordKey, fields);
								if (requestKey != null) {
									// found it! we need to store it as the key of the pending
									// request;
									// that means that if there is more than one requestKey in the
									// same
									// request, only the last one found will be considered.
									pendingRequestKey = requestKey;
								}
								atts.putAll(recordAtts);
								record.clear();
							}

						} // IF VALID RECORD KEY

						pendingRequestLength += recordLength;
						if (isEOF) {
							// record is not valid and reader has reached EOF
							inputBuffer.position(inputBuffer.limit());
							break;
						}

					} // FOR EACH NEW RECORD

					if (isEOF && !inputBuffer.hasRemaining()) {
						break;
					}

					// Prepare byte buffer for next channel read
					inputBuffer.compact();

					// update file offset (points to end of last record found)
					fileOffset += bytesRead - inputBuffer.remaining();
				} // READ FILE

				// process last pending request after EOF (if any)
				if (hasPendingRequest) {
					lock.lock();
					try {
						builder.getStaticAttributes().insertStatics(atts);
						toProcessRequests.add(createRequest(pendingRequestKey, pendingRequestOffset,
								pendingRequestLength, file, atts, params));
					} finally {
						lock.unlock();
					}
				}

			} catch (IOException e) {
				if (e instanceof ClosedByInterruptException) {
					log.error(i18n.getString("problemReadingFile") + ":" + file, e);
				} else {
					log.error(i18n.getString("problemReadingFile") + ":" + file, e);
				}
				if (builder.getIgnoreIOErrors()) {
					hasIOErrors = true;
				} else {
					throw new RuntimeException(i18n.getString("problemReadingFile"), e);
				}
			}

			if (!hasIOErrors) {
				for (Iterator<Request> it = toProcessRequests.iterator(); it.hasNext();) {
					Request request = it.next();
					lock.lock();
					try {
						requestFilter.accept(request);
						if (this.requestIndexWriter != null) {
							writeIndex(request);
						}
					} finally {
						lock.unlock();
					}
				}
			}

			toProcessRequests.clear();

			try {
				reader.close();
			} catch (IOException e) {
				log.error(i18n.getString("problemClosingFile") + ":" + file, e);
				if (!builder.getIgnoreIOErrors()) {
					throw new RuntimeException(i18n.getString("problemClosingFile"), e);
				}
			}

			inputBuffer.clear();

			log
					.debug("Filter has:" + requestFilter.getAcceptedRequests().size()
							+ " requests.");
			return requestFilter;
		} finally {
			;
		}
	}


	/**
	 * Reads from the input until it completes the buffer or no more data is
	 * available.
	 * 
	 * @param input
	 *          the input channel to be read.
	 * @param bb
	 *          the buffer used to hold the information read from the input.
	 * @return the number of bytes read.
	 */
	protected int read(ReadableByteChannel input, ByteBuffer bb) throws IOException {
		if (!canRun.get()) {
			return -1;
		}
		int total = bb.position();
		for (int size = 0; (total < bb.capacity()) && ((size = input.read(bb)) >= 0); total += size) {
			;
		}
		return total;
	}

	/***
	 * Cria um PartialFileRequest a partir do arquivo.
	 * Faz o set do transaction Id caso seja diferente de nulos.
	 * Cria os atributos do request. 
	 * 
	 * @param pendingRequestKey User Key da requisição
	 * @param pendingRequestOffset Inicio da requisição dentro do arquivo.
	 * @param pendingRequestLength Tamanho da requisição dentro do arquivo.
	 * @param file Arquivo onde a requisição foi obtida
	 * @param atts Atributos da requisição.
	 * @param params De onde é obtido o transaction ID.
	 * @return
	 */
	protected PartialFileRequest createRequest(final String pendingRequestKey,
			final long pendingRequestOffset, final long pendingRequestLength, final File file,
			final Map<String, String> atts, final FileBuilderArgs params) {

		final PartialFileRequest request = new PartialFileRequest(pendingRequestKey,
				pendingRequestOffset, pendingRequestLength, file);

		if (params.getTransactionId() != null) {
			request.setTransactionId(params.getTransactionId());
		}

		lock.lock();
		try {
			if (params.getRequestParams() != null) {
				atts.putAll(params.getRequestParams());
			}
			request.setAttributes(atts);
			atts.clear();
		} finally {
			lock.unlock();
		}

		return request;
	}


	/**
	 * O método faz a gravação de uma entrada no indice.
	 * Especificamente de um PartialFileRequest. 
	 * Se outro tipo de requisição for enviada, um class cast exception poderá ocorrer.
	 * 
	 * Este método não é sincronizado nem thread-safe.
	 * Ele requer sincronização externa.
	 * 
	 * Veja o método call {@link #call()} para saber como sincroniza-lo.
	 * 
	 * @param request Requisição que será gravada no indice.
	 * @throws IOException Caso a gravação no indice tenha problemas.
	 */
	protected void writeIndex(Request request) throws IOException {
		// sanity check
		if (request == null) {
			return;
		}
		if (!(request instanceof PartialFileRequest)) {
			return;
		}

		PartialFileRequest fileRequest = (PartialFileRequest) request;

		this.requestIndexWriter.buildAndPrepare(fileRequest, this.builder
				.getIndexCharBuffer(), this.builder.getIndexByteBuffer(), this.builder
				.getIndexEncoder());

		try {
			if (!canRun.get()) {
				return;
			}
			this.requestIndexWriter.write(this.builder.getIndexByteBuffer(), fileRequest);
		} catch (FilterException e) {
			// We can live with that.
			log.error(i18n.getString("index.write.error"), e);
		}
		indexRecordCounter++;
	}

	/**
	 * Reads the specified record buffer and extracts all attributes from the
	 * record's fields. The fields definition must be also informed.
	 * 
	 * <p>
	 * This implementation also receives the list of all fields in the record,
	 * splitted by the configured field delimiter. This is necessary to avoid
	 * repeated parsings of the same record.
	 * 
	 * @param recordBuffer
	 *          the buffer holding an entire record.
	 * @param fieldsDef
	 *          List<FieldDefinition> with the definition of each field to be
	 *          extracted.
	 * @param fields
	 *          fields a List<FieldDefinition> of all fields in the record (can
	 *          be null if no field delimiter has been configured).
	 * @param destination
	 *          Map that will be used to store all attributes found.
	 * @return the destination Map, populated with the attributes found in the
	 *         record.
	 */
	protected Map<String, String> parseAttributes(CharSequence record,
			List<FieldDefinition> fieldsDef, List<CharSequence> fields,
			Map<String, String> destination) {
		if (destination == null) {
			throw new IllegalArgumentException("Attribute destination map cannot be null.");
		}
		if (fieldsDef != null && fieldsDef.size() > 0) {
			List<CharSequence> values = null;
			if (builder.getFieldDelimiter().length != 0) {
				values = (fields != null ? fields : this.getFields(record));
			}
			for (int i = 0; i < fieldsDef.size(); i++) {
				FieldDefinition fieldDef = (FieldDefinition) fieldsDef.get(i);
				String value;
				if (fieldDef.isFixed()) {
					value = getField(record, fieldDef.getPosition(), fieldDef.getLength());
				} else {
					if (fieldDef.getIndex() >= values.size()) {
						continue;
					}
					value = (String) values.get(fieldDef.getIndex());
				}
				if (fieldDef.isTrimValue()) {
					value = value.trim();
				}
				if (fieldDef.hasType()) {
					try {
						value = fieldDef.getType().newInstance(new Object[] { value }).toString();
					} catch (Exception e) {
						throw new RuntimeException("Field '" + fieldDef.getFieldName()
								+ "' returned error " + "while trying to apply declared type: "
								+ e.getMessage(), e);
					}
				}
				destination.put(fieldDef.getFieldName(), value);
			}
		}
		return destination;
	}

	/**
	 * Reads the specified record buffer and extracts all attributes from the
	 * record's fields. The fields definition must be also informed.
	 * 
	 * @param recordBuffer
	 *          the buffer holding an entire record.
	 * @param fieldsDef
	 *          List<FieldDefinition> with the definition of each field to be
	 *          extracted.
	 * @param destination
	 *          Map that will be used to store all attributes found.
	 * @return the destination Map, populated with the attributes found in the
	 *         record.
	 */
	protected Map<String, String> parseAttributes(CharSequence record,
			List<FieldDefinition> fieldsDef, Map<String, String> destination) {
		return parseAttributes(record, fieldsDef, null, destination);
	}

	/**
	 * Fetch the request-key from a record.
	 * 
	 * First, this method will detect if the record has a request-key by matching
	 * the given <code>recordKey</code> against the configured request-key
	 * pattern.
	 * 
	 * Then, it will parse all request-key fields (could be composite) and build a
	 * new request key using ":" as a field delimiter of the key String.
	 * 
	 * @param record
	 *          the record from where the request-key will be extracted.
	 * @param recordKey
	 *          the record-key of the specified record.
	 * @param fields
	 *          fields a List<FieldDefinition> of all fields in the record (can
	 *          be null if no field delimiter has been configured).
	 * @return the request-key, or <code>null</code> if the record-key is
	 *         <code>null</code> or doesn't match with the request-key pattern.
	 */
	protected String getRequestKey(final CharSequence record,
			final EncodedString recordKey, final List<CharSequence> fields) {
		if (recordKey == null
				|| !(builder.isAnyRecordHasRequestKey() || recordKey.equals(builder
						.getNewRequestToken()))) {
			return null;
		}

		String requestKey = "";
		Map<String, String> atts = parseAttributes(record, builder.getRequestKeyFields(),
				fields, new HashMap<String, String>());
		for (int i = 0; i < builder.getRequestKeyFields().size(); i++) {
			FieldDefinition field = (FieldDefinition) builder.getRequestKeyFields().get(i);
			String fieldValue;
			if (atts.containsKey(field.getFieldName())) {
				fieldValue = atts.get(field.getFieldName()).toString();
			} else {
				fieldValue = "";
			}
			requestKey += (i > 0 ? builder.getRequestKeyDelimiter() : "") + fieldValue;
		}
		return (requestKey.length() == 0 ? null : requestKey);
	}

	/**
	 * Get all fields from the record, using a field delimiter.
	 * 
	 * <p>
	 * The field list will be empty if the record is <code>null</code> or is an
	 * empty String.
	 * 
	 * @param record
	 *          the record from where the fields will be extracted.
	 * @return a List<String> of all fields from the record.
	 * @throws IllegalStateException
	 *           if there is no field delimiter configured.
	 */
	protected List<CharSequence> getFields(CharSequence record) {
		if (builder.getFieldDelimiter().length == 0) {
			throw new IllegalStateException(
					"Cannot split fields because no delimiter was specified.");
		}
		final CharBuffer buffer = CharBuffer.wrap(record);
		buffer.rewind();
		if (buffer == null || buffer.remaining() == 0) {
			return null;
		}
		ArrayList<CharSequence> result = new ArrayList<CharSequence>();
		for (int i = 0; i < record.length();) {
			if ((i = NIOBufferUtils.findToken(buffer, builder.getFieldDelimiter())) < 0) {
				break;
			}
			result.add(buffer.subSequence(0, i - buffer.position()).toString());
			buffer.position(i + builder.getFieldDelimiter().length);
		}
		// add last field, if any
		if (buffer.hasRemaining()) {
			result.add(buffer.subSequence(0, buffer.remaining()).toString());
		}
		return result;
	}

	/**
	 * Get a field value from the record.
	 * 
	 * @param record
	 *          the record with all the contents.
	 * @param pos
	 *          the position to start reading the field.
	 * @param length
	 *          the length of the field.
	 * @return The value of the field. Return <code>null</code> if the pos and
	 *         length arguments index characters outside the bounds of the record.
	 */
	protected String getField(CharSequence record, int pos, int length) {
		if (record.length() < pos + length) {
			return "";
		}
		return record.subSequence(pos, pos + length).toString();
	}

	/**
	 * Try to fetch the record-key from the specified record.
	 * 
	 * <p>
	 * The <code>fields</code> parameter will be used only if a record delimiter
	 * has benn configured (for optimization purposes, this method won't parse the
	 * record again).
	 * 
	 * @param record
	 *          the record whose key will be extracted.
	 * @param fields
	 *          a List<FieldDefinition> of all fields in the record (can be null
	 *          if no field delimiter has been configured).
	 * @return the record-key, or <code>null</code> if it wasn't found.
	 */
	private byte[] getRecordKeyToken(final ByteBuffer record) {
		record.rewind();
		final byte[] token;
		if (builder.getRecordKeyDef().isFixed()) {
			if (builder.getRecordKeyDef().getPosition() + builder.getRecordKeyDef().getLength() > record
					.limit()) {
				token = null;
			} else {
				token = new byte[builder.getRecordKeyDef().getLength()];
				record.position(builder.getRecordKeyDef().getPosition());
				record.get(token);
			}
		} else {
			final ByteBuffer field = getField(record, builder.getRecordKeyDef().getIndex());
			if (field == null) {
				token = null;
			} else {
				token = new byte[field.remaining()];
				field.get(token);
			}
		}
		return token;
	}

	/**
	 * Get a field value from the record.
	 * 
	 * @param record
	 *          the buffer holding the record.
	 * @param pos
	 *          the position to start reading the field.
	 * @param length
	 *          the length of the field.
	 * @return The value of the field. Return <code>null</code> if the pos and
	 *         length arguments are outside the bounds of the record.
	 */
	protected ByteBuffer getField(final ByteBuffer record, final int pos, final int length)
			throws CharacterCodingException {
		if (record.remaining() < pos + length) {
			return null;
		}
		final ByteBuffer buffer = record.asReadOnlyBuffer();
		buffer.position(pos).limit(pos + length);
		return buffer.slice();
	}

	/**
	 * Get a field value from the record.
	 * 
	 * @param record
	 *          the buffer holding the record.
	 * @param index
	 *          the index of the wanted field.
	 * @return The value of the field. Return <code>null</code> if the buffer is
	 *         empty or if the index was not found.
	 */
	protected ByteBuffer getField(final ByteBuffer record, final int index) {
		if (builder.getEncodedFieldDelimiter() == null) {
			throw new IllegalStateException(
					"Cannot split fields because no delimiter was specified.");
		}
		final ByteBuffer buffer = record.asReadOnlyBuffer();
		buffer.rewind();
		if (buffer == null || buffer.remaining() == 0) {
			return null;
		}
		ByteBuffer result = null;
		int i, pos;
		for (i = 0, pos = 0; (pos = NIOBufferUtils.findToken(buffer, builder
				.getEncodedFieldDelimiter().getData())) >= 0; i++, buffer.position(pos
				+ builder.getEncodedFieldDelimiter().getData().length)) {
			if (i == index) {
				buffer.limit(pos);
				result = buffer.slice();
				break;
			}
		}
		// process last field, if any
		if (buffer.hasRemaining() && index == ++i) {
			result = buffer.slice();
		}
		return result;
	}

	/**
	 * 
	 * Método de runnable. Este método chama o método call() desta classe, de forma a ser compativel,
	 * com as interfaces Callable & Runnable.
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			this.call();
		} catch (Exception e) {
			log.error(i18n.getString("thread.exception"), e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Return the value of a attribute <code>indexRecordCounter</code>.
	 * 
	 * @return return the value of <code>indexRecordCounter</code>.
	 */
	public int getIndexRecordCounter() {
		return this.indexRecordCounter;
	}

}
