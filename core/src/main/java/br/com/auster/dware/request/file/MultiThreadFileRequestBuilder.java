/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on Mar 31, 2005
 */
package br.com.auster.dware.request.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import br.com.auster.common.io.FileSet;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.filter.FilenameBuilder;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.BaseRequestBuilder;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;
import br.com.auster.dware.request.index.RequestIndexWriter;
import br.com.auster.dware.request.utils.EncodedString;
import br.com.auster.dware.request.utils.FieldDefinition;
import br.com.auster.dware.request.utils.FileBuilderArgs;

/**
 * This request builder will process a list of files to create requests.
 * 
 * This implementation finds and creates requests without decoding the whole
 * file. The record splitting and key searches are all performed using raw
 * (binary) data that is compared to the encoded value of the record delimiter,
 * record key, request key, desired records (for field extraction) and the field
 * delimiter (if any).
 * 
 * <p>
 * Configuration example:
 * 
 * <pre>
 * 
 *  
 *   
 *    &lt;config bufferSize=&quot;262144&quot; 
 *               encoding=&quot;ISO-8859-1&quot; 
 *               maxRecordSize=&quot;32768&quot; 
 *               field-delimiter=&quot;;&quot;&gt;
 *      &lt;!--
 *        Tells how to fetch a record-key. That's essential for the file
 *        request builder, since it will only process records that have a
 *        valid request key. The positional attributes follow the same rules
 *        as the &quot;field&quot; element of a record definition (see bellow).
 *        The &quot;new-request-pattern&quot; is a regex that will be matched against the
 *        record-key to determine if the record is the beginning of a new request.
 *      --&gt;
 *      &lt;record-key index=&quot;2&quot; new-request-pattern=&quot;*&quot;/&gt;
 *       &lt;!-- 
 *         Configures a request-key that will be used as the userKey attribute of 
 *         the created request. The pattern is a regex that will be
 *         matched against the record-key and if it matches, the request-key will
 *         be extracted.
 *       --&gt;
 *      &lt;request-key record-key-pattern=&quot;keys&quot;&gt;
 *        &lt;!--
 *          The field of a request key WON'T be stored as a request
 *          attribute, but the name is still mandatory.
 *        &lt;field name=&quot;campo-3&quot; index=&quot;3&quot;/&gt;
 *      &lt;/request-key&gt;
 *       &lt;!-- 
 *         Configures a record for field extraction
 *         the &quot;key&quot; attribute is the record-key name from which the 
 *         fields will be extracted and stored in the request. Finally,
 *         the &quot;key&quot; can be &quot;*&quot;
 *       --&gt;
 *      &lt;record key=&quot;atts&quot;&gt;
 *        &lt;!-- 
 *          A field MUST have a name and positional attributes, that depends
 *          whether the &quot;field-delimiter&quot; attribute has been configured - if
 *          so, the &quot;index&quot; is mandatory, otherwise you must provide a
 *          &quot;position&quot; and &quot;length&quot; attributes.
 *        --&gt;  
 *        &lt;field name=&quot;campo-1&quot; index=&quot;1&quot;/&gt;
 *      &lt;/record&gt;
 *      &lt;!--
 *        Configures the list of (optional) static attributes to be added to the request. This is a simple
 *        	shortcut to add default values to attributes. 
 *        This attributes can be single values or a list of simple types. Each attribute type must be defined 
 *          as a fully qualified classname, with a
 * <code>
 * (java.lang.String)
 * </code>
 *  constructor. For lists, the 
 *          type must be one of the many
 * <code>
 * java.util.Collection
 * </code>
 *  implementations.        	
 *      --&gt;
 *      &lt;static-attributes&gt;
 *        &lt;static name=&quot;campo-1&quot; type=&quot;class-name&quot;&gt;attribute-value&lt;/static&gt;
 *        (...)
 *        &lt;static-list type=&quot;list-class-name&quot; element-type=&quot;elements-class-name&quot;&gt;
 *          &lt;value&gt;element-value&lt;/value&gt;
 *        &lt;/static-list&gt;
 *      &lt;/static-attributes&gt;
 *    &lt;/config&gt;
 *    
 *   
 *  
 * </pre>
 * 
 * @author Ricardo Barone
 * @version $Id: FileRequestBuilder.java 320 2007-11-21 11:31:43Z mtengelm $]
 * 
 * Incluida a geração de Indices de requisições.
 * Para gerar indices são necessárias 2 condições:
 * 1-Exista o argumento indexFile={file} na linha de comando, que aponta o diretório base dos indices a serem criados.
 * 2-O Elemento &lt; index &gt; na configuração do Builder.
 * O elemento index, possui alguns atributos que indicam se o indice sera comprimido ou não, será sobrescrito ou
 * "appendado".
 * Dentro do elemento index, deve tb. ser confgurado um elemento filename, que cria os nomes dos arquivos de indices,
 *  da mesma forma como o OutputFilter, utilizando a classe {@linkplain FilenameBuilder}.
 * 
 */
public class MultiThreadFileRequestBuilder extends BaseRequestBuilder {

	/**
	 * "{@value}": configuration attribute for the size of the input buffer that
	 * will be used to read the files. Value = {@value}.
	 */
	public static final String																BUFFER_SIZE_ATT						= "bufferSize";
	/**
	 * "{@value}": configuration attribute for the encoding of the input files.
	 */
	public static final String																ENCODING_ATT							= "encoding";
	/**
	 * "{@value}": configuration attribute for the maximum size of a record, in
	 * characters.
	 */
	public static final String																MAX_RECORD_SIZE_ATT				= "maxRecordSize";
	/**
	 * "{@value}": configuration attribute for the record delimiter. If none is
	 * given, the default will be {@link #DELIMITER_ATT}.
	 */
	public static final String																RECORD_DELIMITER_ATT			= "record-delimiter";
	/**
	 * "{@value}": default record delimiter, overridden by
	 * {@link #RECORD_DELIMITER_ATT}.
	 */
	public static final String																DEFAULT_RECORD_DELIMITER	= "\n";
	/**
	 * "{@value}": configuration attribute for the record's field delimiter.
	 */
	public static final String																DELIMITER_ATT							= "field-delimiter";
	/**
	 * "{@value}": configuration element for a record-key [there can be only
	 * one].
	 */
	public static final String																RECORD_KEY_ELEMENT				= "record-key";
	/**
	 * "{@value}": configuration attribute of {@link #RECORD_KEY_ELEMENT} for
	 * the pattern matched against a record-key and that indicates that a new
	 * request has begun.
	 */
	public static final String																NEW_REQUEST_PATTERN_ATT		= "new-request-pattern";
	/**
	 * "{@value}": configuration element of a request-key [there can be only
	 * one].
	 */
	public static final String																REQUEST_KEY_ELEMENT				= "request-key";
	/**
	 * "{@value}": configuration attribute of {@link #REQUEST_KEY_ELEMENT} for
	 * the pattern matched against a record-key and that indicates that the record
	 * has a request-key.
	 */
	public static final String																REQUEST_KEY_PATTERN_ATT		= "record-key-pattern";
	/**
	 * "{@value}": configuration attribute of {@link #REQUEST_KEY_ELEMENT} for
	 * the request-key delimiter that will be used when building composite keys.
	 * If none is given and the key is composite, the value of
	 * {@link Request#COMPOSITE_KEY_DELIMITER} will be used.
	 */
	public static final String																KEY_DELIMITER_ATT					= "composite-key-delimiter";
	/**
	 * "{@value}": configuration element of a record that will have some of it's
	 * fields extracted to be stored in the request [there can be 0..n].
	 */
	public static final String																RECORD_ELEMENT						= "record";
	/**
	 * "{@value}": configuration attribute of {@link #RECORD_ELEMENT} for the
	 * name of the record-key.
	 */
	public static final String																KEY_ATT										= "key";
	/**
	 * "{@value}": configuration element, child of {@link #RECORD_ELEMENT} and
	 * {@link #REQUEST_KEY_ELEMENT}, for a field definition.
	 */
	public static final String																FIELD_ELEMENT							= "field";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} for the
	 * name of the field.
	 */
	public static final String																NAME_ATT									= "name";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the index of a field, used only if a
	 * {@link #DELIMITER_ATT} was provided.
	 */
	public static final String																INDEX_ATT									= "index";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the start position of a field, used only
	 * if a {@link #DELIMITER_ATT} was NOT provided.
	 */
	public static final String																POSITION_ATT							= "position";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the length of a field, used only if a
	 * {@link #DELIMITER_ATT} was NOT provided.
	 */
	public static final String																LENGTH_ATT								= "length";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, that indicates if the field value must be
	 * trimmed after being extracted. Default is <code>true</code>.
	 */
	public static final String																TRIM_ATT									= "trim";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the class name of the field type.
	 */
	public static final String																TYPE_ATT									= "type";

	/**
	 * "{@value}": builder argument for a String representing the input filename
	 * list.
	 */
	public static final String																FILENAMES_ARG							= "filenames";
	/**
	 * "{@value}": builder argument for a File[] representing the input file
	 * list.
	 */
	public static final String																FILES_ARG									= "files";

	/*****************************************************************************
	 * This option allows to define in configuration if we want the builder to
	 * proceed in case of an IO ERROR.
	 * 
	 * The values are true OR false.
	 * 
	 * Once this is a new Option and the behavior BEFORE this implementation is
	 * like false, the default value for this option IS false.
	 * 
	 * In the case of an IO Error is found during creation of request if this flag
	 * is false or not informed: This Builder will throw a Runtime Exception and
	 * stop all its processing.
	 * 
	 * In the case this option is true, this builder will: Send an ERROR message
	 * log, stating the file and tentativly the position on the file the error had
	 * happen. Empty the request list for requests of the current file (Might take
	 * long) once they are suspicius now. Skip the rest of current file Normal
	 * process the next file in the sequence.
	 */
	public static final String																P_ATTR_IGNORE_IO_ERRORS		= "ignore-io-errors";

	protected boolean																					ignoreIOErrorsFlag				= false;

	protected int																							threads;
	// if you don't know what this is, you're fired!
	private static final Logger																log												= LogFactory
																																													.getLogger(MultiThreadFileRequestBuilder.class);

	/*** Opção para quantidade de threads deste Request Builder ***/
	public static final String																THREADS_ATTR							= "threads";
	
	/*** Diretório Base onde os índices estão armazenados ***/
	public static final String																INDEX_FILE_ARGUMENT				= "indexFile";
	
	/*** Tamanho do Buffer de Gravação do Índice. Futuramente poderá ser configurável ***/
	public static final int																		INDEX_BUFFER_SIZE					= 1024;
	
	/*** Nome do elemento para definir o nome dos arquivos de indice ***/
	public static final String																FILENAME_ELEMENT					= "filename";
	
	/*** Nome do elemento na confiuração, onde o indice pode ser configurado ***/
	public static final String																INDEX_ELEMENT							= "index";
	
	/*** Atributo da conf. que indica se um indice serpa comprimido ou não. Se for será GZip Format ***/
	public static final String																COMPRESS_ATTR							= "compress";
	
	/*** Atributo que indica se os indices serão abertos em write ou appen mode. Se true, então append mode ***/
	public static final String																APPEND_ATTR								= "append";
	
	/*** Quantidade máxima de arquivo de indice abertos simultaneamente para gravação ***/
	public static final String																MAX_OPEN_FILES						= "file-cache-size";

	// used to create decoders and encoders for buffers - configured
	protected final Charset																		charset;

	// used to calculate the capacity of the record buffer - configured
	protected final int																				maxRecordSize;

	// List<FieldDefinition> defines how to create
	// a request key (ID) from a record - configured
	protected final List<FieldDefinition>											requestKeyFields;

	// used in composite keys - configured, but default is DEFAULT_KEY_DELIMITER
	protected final String																		requestKeyDelimiter;

	// List<FieldDefinition> how to fetch a record key (singleton List)
	protected final List<FieldDefinition>											recordKeyDefinition;

	// configured field definitions as a
	// Map<String(record name), List<FieldDefinition>> - configured
	protected final Map<EncodedString, List<FieldDefinition>>	recordDefinitions;

	private final FieldDefinition															recordKeyDef;

	private final char[]																			fieldDelimiter;

	// HashSet<EncodedString>
	private final HashSet<EncodedString>											desiredRecords						= new HashSet<EncodedString>();

	private final EncodedString																encodedRecordDelimiter;
	private final EncodedString																encodedFieldDelimiter;

	private final EncodedString																newRequestToken;
	private final EncodedString																requestKeyToken;
	private final EncodedString																wildcard;

	private final boolean																			anyRecordIsNewRequest;
	private final boolean																			anyRecordHasRequestKey;

	// private final CharsetDecoder decoder;
	private final CharsetEncoder															encoder;

	private final I18n																				i18n											= I18n
																																													.getInstance(MultiThreadFileRequestBuilder.class);
	private int																								bufferSize;
	private String																						charsetName;

	// Fields to handle index generation
	/*** Charset para geração de indices ***/
	private Charset																						indexCharset;
	/*** Byte Buffer para gravação de indices ***/
	private ByteBuffer																				indexByteBuffer;
	/*** Charbuffer para gravação de indices ***/
	private CharBuffer																				indexCharBuffer;
	/***Encoder para gravação de indices***/
	private CharsetEncoder																		indexEncoder;
	/*** Nome do diretório base onde os indices serão gravados. ***/
	private String																						indexFileName;

	/*** Define o pattern de nomes, por configuração, onde os indices serão gravados ***/
	private FilenameBuilder																		indexFileNamePattern;
	/*** Instância que fará a gravação dos índices ***/
	private RequestIndexWriter																requestIndexWriter;
	/** Se true os indices serão comprimidos no formato GZip ***/
	private boolean																						indexCompress;
	/*** Indica se os indices serão (true) abertos em modo append ou se serão sobrescritos (false) ***/
	private boolean																						indexAppend;
	/*** Quantidade máxima de arquivos de indice abertos ***/
	private int																								maxIndexFilesOpen;

	/**
	 * Contructor used by child classes only
	 */
	protected MultiThreadFileRequestBuilder(final Element config, final String name) {
		super(name, config);

		this.charset = null;
		this.encoder = null;
		// this.decoder = null;
		this.maxRecordSize = 0;

		this.requestKeyDelimiter = null;
		this.encodedRecordDelimiter = null;
		this.encodedFieldDelimiter = null;
		this.wildcard = null;

		this.fieldDelimiter = null;
		this.newRequestToken = null;
		this.anyRecordHasRequestKey = false;
		this.anyRecordIsNewRequest = false;
		this.requestKeyToken = null;
		this.recordKeyDefinition = null;
		this.recordKeyDef = null;
		this.requestKeyFields = null;
		this.recordDefinitions = null;
		this.maxIndexFilesOpen = -1;
	}

	/**
	 * Creates a new <code>FileRequestBuilder</code>.
	 * 
	 * @param name
	 *          the name of this builder.
	 * @param config
	 *          the configuration element, as described above.
	 * @throws IllegalArgumentException
	 *           if there's a configuration error.
	 */
	public MultiThreadFileRequestBuilder(final String name, final Element config) {
		super(name, config);

		log.debug(i18n.getString("startConfig", "request builder '" + name + "'"));

		// CONFIGURE OTHER OPTIONS
		bufferSize = DOMUtils.getIntAttribute(config, BUFFER_SIZE_ATT, true);

		this.maxRecordSize = DOMUtils.getIntAttribute(config, MAX_RECORD_SIZE_ATT, true);

		this.charsetName = DOMUtils.getAttribute(config, ENCODING_ATT, true);
		this.charset = Charset.forName(DOMUtils.getAttribute(config, ENCODING_ATT, true));
		this.encoder = this.charset.newEncoder();

		// configure wildcard
		try {
			this.wildcard = new EncodedString(this.encoder.encode(
					CharBuffer.wrap(new char[] { '*' })).array(), charsetName);
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException(
					"The wildcard string '*' could not be encoded using charset "
							+ this.charset.name(), e);
		}

		// configure record delimiter
		String delimiter = DOMUtils.getAttribute(config, RECORD_DELIMITER_ATT, false);
		if (delimiter == null || delimiter.length() == 0) {
			delimiter = DEFAULT_RECORD_DELIMITER;
		}
		final String recordDelimiter = StringEscapeUtils.unescapeJava(delimiter);
		try {
			this.encodedRecordDelimiter = new EncodedString(this.encoder.encode(
					CharBuffer.wrap(recordDelimiter)).array(), charsetName);
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("The string '" + recordDelimiter
					+ "' could not be encoded using charset " + this.charset.name(), e);
		}

		// configure field delimiter
		delimiter = DOMUtils.getAttribute(config, DELIMITER_ATT, false);
		if (delimiter != null && delimiter.length() == 0) {
			this.fieldDelimiter = new char[0];
			this.encodedFieldDelimiter = null;
		} else {
			try {
				this.fieldDelimiter = StringEscapeUtils.unescapeJava(delimiter).toCharArray();
				this.encodedFieldDelimiter = new EncodedString(this.encoder.encode(
						CharBuffer.wrap(delimiter)).array(), charsetName);
			} catch (CharacterCodingException e) {
				throw new IllegalArgumentException("The string '" + delimiter
						+ "' could not be encoded using charset " + this.charset.name(), e);
			}
		}

		// configure record-key
		final Element recordKey = DOMUtils.getElement(config, RECORD_KEY_ELEMENT, true);
		this.recordKeyDefinition = Collections.singletonList(getFieldPosition(recordKey));
		this.recordKeyDef = this.recordKeyDefinition.get(0);
		final Element recordKeyElt = DOMUtils.getElement(config, RECORD_KEY_ELEMENT, true);
		String newRequest = DOMUtils
				.getAttribute(recordKeyElt, NEW_REQUEST_PATTERN_ATT, true);
		this.anyRecordIsNewRequest = newRequest.length() == 0 || "*".equals(newRequest);
		try {
			// get the token that indicates that a new request has begun
			this.newRequestToken = new EncodedString(this.encoder.encode(
					CharBuffer.wrap(newRequest)).array(), charsetName);
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("The string '" + newRequest
					+ "' could not be encoded using charset " + this.charset.name(), e);
		}

		// configure request-key
		final Element requestKeyElt = DOMUtils.getElement(config, REQUEST_KEY_ELEMENT, true);
		delimiter = DOMUtils.getAttribute(requestKeyElt, KEY_DELIMITER_ATT, false);
		if (delimiter == null || delimiter.length() == 0) {
			delimiter = Request.COMPOSITE_KEY_DELIMITER;
		}
		this.requestKeyDelimiter = delimiter;
		this.requestKeyFields = configFields(requestKeyElt);
		// get the token that indicates that a request key has been found
		String requestKey = DOMUtils.getAttribute(requestKeyElt, REQUEST_KEY_PATTERN_ATT,
				true);
		this.anyRecordHasRequestKey = requestKey.length() == 0 || "*".equals(newRequest);
		try {
			this.requestKeyToken = new EncodedString(this.encoder.encode(
					CharBuffer.wrap(requestKey)).array(), charsetName);
			this.desiredRecords.add(this.requestKeyToken);
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("The string '" + requestKey
					+ "' could not be encoded using charset " + this.charset.name(), e);
		}

		// configure records
		this.recordDefinitions = new HashMap<EncodedString, List<FieldDefinition>>();
		final NodeList records = DOMUtils.getElements(config, RECORD_ELEMENT);
		for (int i = 0; i < records.getLength(); i++) {
			final Element record = (Element) records.item(i);
			String recordName = DOMUtils.getAttribute(record, KEY_ATT, true);
			if (recordName.length() == 0) {
				recordName = "*";
			}
			EncodedString encodedName;
			try {
				encodedName = new EncodedString(this.encoder.encode(CharBuffer.wrap(recordName))
						.array(), charsetName);
			} catch (CharacterCodingException e) {
				throw new IllegalArgumentException("The string '" + recordName
						+ "' could not be encoded using charset " + this.charset.name(), e);
			}
			this.recordDefinitions.put(encodedName, configFields(record));
			this.desiredRecords.add(encodedName);
		}

		this.encoder.reset();

		configureIgnoreOption(config);
		configureThreads(config);
		configureIndexCreation(config);
		log.debug(i18n.getString("endConfig", "Request builder '" + name + "'"));
	}

	/**
	 * Confira a criação de indices. Método chamado pelo contrutor.
	 * Receb como parametro a configuração do builder.
	 * Procura um {@value #INDEX_ELEMENT}. Se não encontrar retorna e não serão gerados indices.
	 * Se encontrar, busca a partir dele:
	 * Os atributos de de compressão {@value #COMPRESS_ATTR}, append {@value #APPEND_ATTR} e o 
	 * de máximo numero de arquivos abertos {@value #MAX_OPEN_FILES}, e finalmente obtém a configuração de nomes
	 * de arquivos de índices no elemento {@value #FILENAME_ELEMENT}.
	 * 
	 * As configurações de nome de arquivo de indices, seguem o indicado pela classe {@linkplain FilenameBuilder}
	 * 
	 * @param config
	 */
	protected void configureIndexCreation(Element config) {
		Element indexElmt = DOMUtils.getElement(config, INDEX_ELEMENT, false);
		if (indexElmt == null) {
			return;
		}
		indexCompress = DOMUtils.getBooleanAttribute(indexElmt, COMPRESS_ATTR, true);
		indexAppend = DOMUtils.getBooleanAttribute(indexElmt, APPEND_ATTR, true);
		maxIndexFilesOpen = DOMUtils.getIntAttribute(indexElmt, MAX_OPEN_FILES, false);
		Element filenameElmt = DOMUtils.getElement(indexElmt, FILENAME_ELEMENT, false);
		if (null == filenameElmt) {
			return;
		}
		indexFileNamePattern = new FilenameBuilder(filenameElmt);
	}

	/**
	 * Configura a quantidade de threads a serem utilizadas na busca de requisições.
	 * Caso não informado ou invalida, a seguinte formula será utilizada:
	 * Qtde de Processdores * 4.
	 *  
	 * @param config Configuração do Builder
	 */
	protected void configureThreads(Element config) {
		int tempThreads = DOMUtils.getIntAttribute(config, THREADS_ATTR, false);
		if (tempThreads == 0) {
			tempThreads = Runtime.getRuntime().availableProcessors() * 4;
		}
		this.threads = tempThreads;
		log.info(i18n.getString("processing.threads", new Integer(this.threads),
				" os arquivos de entrada."));
	}

	/*****************************************************************************
	 * Configures the Ignore IO Errors Flag.
	 * 
	 * @see P_ATTR_IGNORE_IO_ERRORS field comments.
	 *      <p>
	 *      Example:
	 * 
	 * <pre>
	 *    Create a use example.
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param config
	 */
	protected void configureIgnoreOption(Element config) {
		this.ignoreIOErrorsFlag = DOMUtils.getBooleanAttribute(config,
				P_ATTR_IGNORE_IO_ERRORS);
	}

	/**
	 * Configure all fields from a given record element.
	 * 
	 * <p>
	 * All field attributes are mandatory (including the name).
	 * 
	 * @param record
	 *          the record element that containd the field elements to be parsed.
	 * @return a List<FieldDefinition> of all fields configured.
	 */
	protected List<FieldDefinition> configFields(Element record) {
		final List<FieldDefinition> fieldsDef = new ArrayList<FieldDefinition>();
		final NodeList fields = DOMUtils.getElements(record, FIELD_ELEMENT);
		for (int j = 0; j < fields.getLength(); j++) {
			final Element field = (Element) fields.item(j);
			String fieldName = DOMUtils.getAttribute(field, NAME_ATT, true);
			final FieldDefinition fieldDef = getFieldPosition(field);
			fieldDef.setFieldName(fieldName);
			fieldsDef.add(fieldDef);
		}
		return fieldsDef;
	}

	/**
	 * Extracts the position, length or index of the specified field and returns
	 * it as a <code>FieldDefinition</code>.
	 * 
	 * <p>
	 * The <code>name</code> attribute of the return value is not populated.
	 * 
	 * @param field
	 *          the field element whose position info you want to extract.
	 * @return a {@link FieldDefinition} instance with the positional info
	 *         populated.
	 * @throws IllegalArgumentException
	 *           if any of the field's attributes is invalid.
	 */
	protected FieldDefinition getFieldPosition(Element field) {
		final FieldDefinition fieldDef;
		if (this.fieldDelimiter.length == 0) {
			final int fieldPos = DOMUtils.getIntAttribute(field, POSITION_ATT, true) - 1;
			if (fieldPos < 0) {
				throw new IllegalArgumentException("Field position must be greater than 0.");
			}
			final int fieldLength = DOMUtils.getIntAttribute(field, LENGTH_ATT, true);
			if (fieldLength <= 0) {
				throw new IllegalArgumentException("Field length must be greater than 0.");
			}
			fieldDef = new FieldDefinition(fieldPos, fieldLength);
		} else {
			final int fieldIndex = DOMUtils.getIntAttribute(field, INDEX_ATT, true) - 1;
			if (fieldIndex < 0) {
				throw new IllegalArgumentException("Field index must be greater than 0.");
			}
			fieldDef = new FieldDefinition(fieldIndex);
		}
		String trim = DOMUtils.getAttribute(field, TRIM_ATT, false);
		if (trim != null && trim.length() > 0) {
			fieldDef.setTrimValue(Boolean.valueOf(trim).booleanValue());
		} else {
			fieldDef.setTrimValue(true);
		}
		String type = DOMUtils.getAttribute(field, TYPE_ATT, false);
		if (type != null && type.length() > 0) {
			try {
				fieldDef.setType(type);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid type for field: [" + type + "] - "
						+ e.getMessage());
			}
		}
		return fieldDef;
	}

	/**
	 * Parses all arguments. This is kind of the command-line arguments of the
	 * buider, except that is not necessarily coming from the command-line!
	 * 
	 * <p>
	 * This implementation accepts the following args:
	 * <ul>
	 * <li>{@link #FILENAMES_ARG}: (String) list of filenames to be parsed
	 * <li>{@link #FILES_ARG}: (File[]) files to be parsed
	 * <li>{@link br.com.auster.dware.request.BaseRequestBuilder#TRANSACTION_ID_ARG}:
	 * (String) will be stored in each request
	 * <li>{@link br.com.auster.dware.request.BaseRequestBuilder#REQUEST_PARAMS_ARG}:
	 * (Map<String, Object>) parameters that will be stored in each request
	 * </ul>
	 * 
	 * <p>
	 * At least one of {@link #FILENAMES_ARG} or {@link #FILES_ARG} must be
	 * provided, but you can specify both if you want.
	 * 
	 * <p>
	 * List arguments must be delimited by "{@link br.com.auster.dware.request.RequestBuilder#ARG_LIST_DELIMITER}".
	 * 
	 * @param args
	 *          Map<String, Object> with all arguments for this File Builder.
	 * @return an instance of {@link FileBuilderArgs} with all parsed arguments.
	 */
	protected FileBuilderArgs parseFileArgs(Map args) {
		if (args == null) {
			args = new HashMap();
		}

		final List<File> files = new ArrayList<File>();
		Object value;

		final String[] fileNames;
		value = args.get(FILENAMES_ARG);
		if (value != null) {
			checkArgType(FILENAMES_ARG, value, String.class);
			fileNames = ((String) value).split(RequestBuilder.ARG_LIST_DELIMITER);
			for (int i = 0; i < fileNames.length; i++) {
				files.addAll(Arrays.asList(FileSet.getFiles(fileNames[i])));
			}
		}

		value = args.get(FILES_ARG);
		if (value != null) {
			checkArgType(FILES_ARG, value, File[].class);
			files.addAll(Arrays.asList((File[]) value));
		}

		if (files.size() == 0) {
			throw new IllegalArgumentException("No files found to process!");
		}

		BuilderArgs baseArgs = super.parseArgs(args);
		return new FileBuilderArgs(files, baseArgs.getTransactionId(), baseArgs
				.getRequestParams());
	}

	/**
	 * @inheritDoc
	 */
	public RequestFilter createRequests(Map args) {
		return createRequests(null, args);
	}

	/**
	 * @inheritDoc
	 */
	public RequestFilter createRequests(RequestFilter filter, Map args) {
		final RequestFilter requestFilter;
		if (filter == null) {
			requestFilter = new HashRequestFilter();
		} else {
			requestFilter = filter;
		}

		indexFileName = (String) args.get(INDEX_FILE_ARGUMENT);
		createIndexWriter(indexFileName, indexFileNamePattern, indexCompress, indexAppend);

		final FileBuilderArgs params = parseFileArgs(args);

		int numberOfAllowedRequests = (filter.getAllowedRequests() == null) ? -1 : filter
				.getAllowedRequests().size();

		ThreadPoolExecutor myPool = new ThreadPoolExecutor(this.threads, this.threads, 10,
				TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());

		AtomicBoolean canRun = new AtomicBoolean(true);
		log.info(i18n.getString("files.quantity", new Integer(params.getFiles().size())));
 
		for (Iterator<File> it = params.getFiles().iterator(); it.hasNext()
				&& requestFilter.canAccept();) {

			final File file = it.next();

			Runnable findRequests = new FindRequests(this, requestFilter, file, params,
					this.encodedRecordDelimiter.getData(), requestIndexWriter, canRun);

			myPool.submit(findRequests);
		} // FOR EACH FILE

		myPool.shutdown();
		try {
			int duration = 10;
			while (!myPool.isTerminated()) {
				StringBuilder sb = new StringBuilder();
				sb.append(myPool.getActiveCount());
				sb.append('/');
				sb.append(myPool.getCompletedTaskCount());
				sb.append('/');
				sb.append(myPool.getCorePoolSize());
				sb.append('/');
				sb.append(myPool.getPoolSize());
				sb.append('/');
				sb.append(myPool.getMaximumPoolSize());
				sb.append('/');
				sb.append(myPool.getLargestPoolSize());
				sb.append('/');
				sb.append(requestFilter.getAcceptedRequests().size());
				sb.append('(');
				sb.append( (requestIndexWriter != null) ? requestIndexWriter.getRecordsWritten() : "?");
				sb.append(')');

				log.info(i18n.getString("thread.wait", new Integer(duration), sb.toString()));

				myPool.awaitTermination(duration, TimeUnit.SECONDS);

				// IF it is null, means no filter, so we have to proceed with all
				// threads to the end.
				if ((null != requestFilter.getAcceptedRequests())
						&& (requestFilter.getAcceptedRequests().size() == numberOfAllowedRequests)) {
					log.info(i18n.getString("all.found"));
					if (requestIndexWriter == null) {
						log.info(i18n.getString("found.requests", new Integer(numberOfAllowedRequests)));
						canRun.set(false);
						Thread.sleep(3000);
						List<Runnable> now = myPool.shutdownNow();
						log.info(i18n.getString("pool.shutdown", new Integer(now.size())));
					} else {
						log.info(i18n.getString("find.cannot.stop"));
					}

				}
			}
		} catch (InterruptedException e) {
			RuntimeException rte = new RuntimeException(e);
			log.fatal(i18n.getString("thread.interrupted"), e);
			throw rte;
		}

		try {
			if (requestIndexWriter != null) {
				log.info(i18n.getString("index.request.counter", new Long(requestIndexWriter.getRecordsWritten())));
				requestIndexWriter.closeAllChannels();
			}
		} catch (IOException e) {
			// we can leave with this exception.
			log.error(i18n.getString("index.close"), e);
		}

		return requestFilter;
	}

	/***
	 * Cria o gravador de indices. 
	 * 
	 * @param baseName Nome do diretório base onde os indices serão gravados;
	 * @param pattern Padrão de nomes de acordo com a classe {@linkplain FilenameBuilder}
	 * @param compress Indica se o indice será comprimido.
	 * @param indexAppend Indica se o indice será sobrescrito (false) ou aumentado (true)
	 */
	protected void createIndexWriter(String baseName, FilenameBuilder pattern,
			boolean compress, boolean indexAppend) {

		// If we do not have a base point, there is no writer too.
		if ((null == baseName || "".equals(baseName)) || (pattern == null)) {
			log.info( i18n.getString("no.index.defined"));
			requestIndexWriter = null;
			return;
		}
		requestIndexWriter = new RequestIndexWriter(this.maxIndexFilesOpen);
		requestIndexWriter.setBaseDir(baseName);
		requestIndexWriter.setFileNameBuilder(pattern);
		requestIndexWriter.setCompress(compress);
		requestIndexWriter.setAppend(indexAppend);

		this.indexCharset = Charset.defaultCharset();
		this.indexEncoder = this.indexCharset.newEncoder();
		log.trace("Using charset:" + indexCharset);
		this.indexCharBuffer = CharBuffer.allocate((int) (this.indexEncoder
				.averageBytesPerChar() * INDEX_BUFFER_SIZE));
		this.indexByteBuffer = ByteBuffer.allocateDirect(INDEX_BUFFER_SIZE);

	}

	public int getBufferSize() {
		return this.bufferSize;
	}

	public int getMaxRecordSize() {
		return this.maxRecordSize;
	}

	public String getCharsetName() {
		return this.charsetName;
	}

	public boolean getIgnoreIOErrors() {
		return this.ignoreIOErrorsFlag;
	}

	public Set<EncodedString> getDesiredRecords() {
		return this.desiredRecords;
	}

	public Map<EncodedString, List<FieldDefinition>> getRecordDefinitions() {
		return this.recordDefinitions;
	}

	public char[] getFieldDelimiter() {
		return this.fieldDelimiter;
	}

	public EncodedString getEncodedFieldDelimiter() {
		return encodedFieldDelimiter;
	}

	public List<FieldDefinition> getRequestKeyFields() {
		return this.requestKeyFields;
	}

	public EncodedString getWildCard() {
		return this.wildcard;
	}

	public StaticAttributes getStaticAttributes() {
		return this.staticAttributes;
	}

	public boolean isAnyRecordHasRequestKey() {
		return this.anyRecordHasRequestKey;
	}

	public EncodedString getNewRequestToken() {
		return newRequestToken;
	}

	public boolean isAnyRecordIsNewRequest() {
		return anyRecordIsNewRequest;
	}

	public String getRequestKeyDelimiter() {
		return requestKeyDelimiter;
	}

	public FieldDefinition getRecordKeyDef() {
		return recordKeyDef;
	}

	/**
	 * Return the value of a attribute <code>indexCharset</code>.
	 * 
	 * @return return the value of <code>indexCharset</code>.
	 */
	public Charset getIndexCharset() {
		return this.indexCharset;
	}

	/**
	 * Return the value of a attribute <code>indexByteBuffer</code>.
	 * 
	 * @return return the value of <code>indexByteBuffer</code>.
	 */
	public ByteBuffer getIndexByteBuffer() {
		return this.indexByteBuffer;
	}

	/**
	 * Return the value of a attribute <code>indexCharBuffer</code>.
	 * 
	 * @return return the value of <code>indexCharBuffer</code>.
	 */
	public CharBuffer getIndexCharBuffer() {
		return this.indexCharBuffer;
	}

	/**
	 * Return the value of a attribute <code>indexEncoder</code>.
	 * 
	 * @return return the value of <code>indexEncoder</code>.
	 */
	public CharsetEncoder getIndexEncoder() {
		return this.indexEncoder;
	}

	/**
	 * Return the value of a attribute <code>indexFileName</code>.
	 * 
	 * @return return the value of <code>indexFileName</code>.
	 */
	public String getIndexFileName() {
		return this.indexFileName;
	}
}
