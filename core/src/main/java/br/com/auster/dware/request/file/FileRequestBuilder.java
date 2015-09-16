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
import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import br.com.auster.common.io.FileSet;
import br.com.auster.common.io.NIOBufferUtils;
import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.filter.OutputToFile;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.BaseRequestBuilder;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

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
 * @version $Id: FileRequestBuilder.java 320 2007-11-21 11:31:43Z mtengelm $
 */
public class FileRequestBuilder extends BaseRequestBuilder {

	/**
	 * "{@value}": configuration attribute for the size of the input buffer that
	 * will be used to read the files. Value = {@value}.
	 */
	public static final String	BUFFER_SIZE_ATT						= "bufferSize";
	/**
	 * "{@value}": configuration attribute for the encoding of the input files.
	 */
	public static final String	ENCODING_ATT							= "encoding";
	/**
	 * "{@value}": configuration attribute for the maximum size of a record, in
	 * characters.
	 */
	public static final String	MAX_RECORD_SIZE_ATT				= "maxRecordSize";
	/**
	 * "{@value}": configuration attribute for the record delimiter. If none is
	 * given, the default will be {@link #DELIMITER_ATT}.
	 */
	public static final String	RECORD_DELIMITER_ATT			= "record-delimiter";
	/**
	 * "{@value}": default record delimiter, overridden by
	 * {@link #RECORD_DELIMITER_ATT}.
	 */
	public static final String	DEFAULT_RECORD_DELIMITER	= "\n";
	/**
	 * "{@value}": configuration attribute for the record's field delimiter.
	 */
	public static final String	DELIMITER_ATT							= "field-delimiter";
	/**
	 * "{@value}": configuration element for a record-key [there can be only
	 * one].
	 */
	public static final String	RECORD_KEY_ELEMENT				= "record-key";
	/**
	 * "{@value}": configuration attribute of {@link #RECORD_KEY_ELEMENT} for
	 * the pattern matched against a record-key and that indicates that a new
	 * request has begun.
	 */
	public static final String	NEW_REQUEST_PATTERN_ATT		= "new-request-pattern";
	/**
	 * "{@value}": configuration element of a request-key [there can be only
	 * one].
	 */
	public static final String	REQUEST_KEY_ELEMENT				= "request-key";
	/**
	 * "{@value}": configuration attribute of {@link #REQUEST_KEY_ELEMENT} for
	 * the pattern matched against a record-key and that indicates that the record
	 * has a request-key.
	 */
	public static final String	REQUEST_KEY_PATTERN_ATT		= "record-key-pattern";
	/**
	 * "{@value}": configuration attribute of {@link #REQUEST_KEY_ELEMENT} for
	 * the request-key delimiter that will be used when building composite keys.
	 * If none is given and the key is composite, the value of
	 * {@link Request#COMPOSITE_KEY_DELIMITER} will be used.
	 */
	public static final String	KEY_DELIMITER_ATT					= "composite-key-delimiter";
	/**
	 * "{@value}": configuration element of a record that will have some of it's
	 * fields extracted to be stored in the request [there can be 0..n].
	 */
	public static final String	RECORD_ELEMENT						= "record";
	/**
	 * "{@value}": configuration attribute of {@link #RECORD_ELEMENT} for the
	 * name of the record-key.
	 */
	public static final String	KEY_ATT										= "key";
	/**
	 * "{@value}": configuration element, child of {@link #RECORD_ELEMENT} and
	 * {@link #REQUEST_KEY_ELEMENT}, for a field definition.
	 */
	public static final String	FIELD_ELEMENT							= "field";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} for the
	 * name of the field.
	 */
	public static final String	NAME_ATT									= "name";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the index of a field, used only if a
	 * {@link #DELIMITER_ATT} was provided.
	 */
	public static final String	INDEX_ATT									= "index";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the start position of a field, used only
	 * if a {@link #DELIMITER_ATT} was NOT provided.
	 */
	public static final String	POSITION_ATT							= "position";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the length of a field, used only if a
	 * {@link #DELIMITER_ATT} was NOT provided.
	 */
	public static final String	LENGTH_ATT								= "length";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, that indicates if the field value must be
	 * trimmed after being extracted. Default is <code>true</code>.
	 */
	public static final String	TRIM_ATT									= "trim";
	/**
	 * "{@value}": configuration attribute of {@link #FIELD_ELEMENT} and
	 * {@link #RECORD_KEY_ELEMENT}, for the class name of the field type.
	 */
	public static final String	TYPE_ATT									= "type";

	/**
	 * "{@value}": builder argument for a String representing the input filename
	 * list.
	 */
	public static final String	FILENAMES_ARG							= "filenames";
	/**
	 * "{@value}": builder argument for a File[] representing the input file
	 * list.
	 */
	public static final String	FILES_ARG									= "files";

	/**
	 * "{@value}": top-level configuration element for the dumper feature.
	 * 
	 * <p>
	 * For available options, see the
	 * {@link br.com.auster.dware.filter.OutputToFile OutputToFile filter}.
	 */
	public static final String	DUMPER_ELT								= "dumper";

	/***
	 * This option allows to define in configuration if we want the builder to proceed in case of an IO ERROR.
	 * 
	 * The values are true OR false.
	 * 
	 * Once this is a new Option and the behavior BEFORE this implementation is like false, the
	 * default value for this option IS false.
	 * 
	 * In the case of an IO Error is found during creation of request if this flag is false or not informed:
	 * This Builder will throw a Runtime Exception and stop all its processing.
	 * 
	 * In the case this option is true, this builder will:
	 * Send an ERROR message log, stating the file and tentativly the position on the file the error had happen.
	 * Empty the request list for requests of the current file (Might take long) once they are suspicius now.
	 * Skip the rest of current file
	 * Normal process the next file in the sequence.
	 */
	public static final String P_ATTR_IGNORE_IO_ERRORS = "ignore-io-errors";
	
	protected boolean ignoreIOErrorsFlag = false;
	
	// if you don't know what this is, you're fired!
	private static final Logger	log												= LogFactory
																														.getLogger(FileRequestBuilder.class);

	public class FileBuilderArgs extends BuilderArgs {

		private List	files;

		protected FileBuilderArgs(List files, String transactionId,
				Map requestParams) {
			super(transactionId, requestParams);
			this.files = files;
		}

		public List getFiles() {
			return this.files;
		}

	}

	public class FieldDefinition {

		public String				fieldName;

		public int					position, length, index;

		public Constructor	type	= null;

		public boolean			isFixed, trimValue = true;

		public FieldDefinition(String name, int position, int length) {
			this.fieldName = name;
			this.position = position;
			this.length = length;
			this.index = 0;
			this.isFixed = true;
		}

		public FieldDefinition(int position, int length) {
			this(null, position, length);
		}

		public FieldDefinition(String name, int index) {
			this.fieldName = name;
			this.index = index;
			this.position = this.length = 0;
			this.isFixed = false;
		}

		public FieldDefinition(int index) {
			this(null, index);
		}

		public boolean hasType() {
			return (this.type != null);
		}

		public void setType(String className) throws Exception {
			Class clazz = Class.forName(className);
			// type must have a constructor that receives a String as param
			this.type = clazz.getConstructor(new Class[] { String.class });
		}
	}

	private class EncodedString {

		private final byte[]	data;

		public EncodedString(byte[] field) {
			this.data = field;
		}

		public boolean equals(Object obj) {
			if (!(obj instanceof EncodedString)) {
				return false;
			}
			return Arrays.equals(this.data, ((EncodedString) obj).data);
		}

		public int hashCode() {
			return new HashCodeBuilder().append(this.data).toHashCode();
		}

		public String toString() {
			if (this.data == null) {
				return null;
			}
			String result;
			try {
				ByteBuffer buffer = ByteBuffer.wrap(this.data);
				result = FileRequestBuilder.this.decoder.decode(buffer).toString();
			} catch (Exception e) {
				result = data.toString();
			}
			return result;
		}
	}

	// buffer used to read chunks of the input file
	protected final ByteBuffer		inputBuffer;

	// used to create decoders and encoders for buffers - configured
	protected final Charset				charset;

	// used to calculate the capacity of the record buffer - configured
	protected final int						maxRecordSize;

	// List<FieldDefinition> defines how to create
	// a request key (ID) from a record - configured
	protected final List					requestKeyFields;

	// used in composite keys - configured, but default is DEFAULT_KEY_DELIMITER
	protected final String				requestKeyDelimiter;

	// List<FieldDefinition> how to fetch a record key (singleton List)
	protected final List					recordKeyDefinition;

	// configured field definitions as a
	// Map<String(record name), List<FieldDefinition>> - configured
	protected final Map						recordDefinitions;

	private final FieldDefinition	recordKeyDef;

	private char[]								fieldDelimiter;

	// HashSet<EncodedString>
	private final HashSet					desiredRecords	= new HashSet();

	private final EncodedString		encodedRecordDelimiter;
	private final EncodedString		encodedFieldDelimiter;

	private final EncodedString		newRequestToken;
	private final EncodedString		requestKeyToken;
	private final EncodedString		wildcard;

	private final boolean					anyRecordIsNewRequest;
	private final boolean					anyRecordHasRequestKey;

	private final CharsetDecoder	decoder;
	private final CharsetEncoder	encoder;

	private final OutputToFile		dumper;

	private final I18n						i18n						= I18n
																										.getInstance(FileRequestBuilder.class);

	/**
	 * Contructor used by child classes only
	 */
	protected FileRequestBuilder(final Element config, final String name) {
		super(name, config);

		this.charset = null;
		this.encoder = null;
		this.decoder = null;
		this.inputBuffer = null;
		this.maxRecordSize = 0;
		this.dumper = null;

		this.requestKeyDelimiter = null;
		this.encodedRecordDelimiter = null;
		this.encodedFieldDelimiter = null;
		this.wildcard = null;

		this.newRequestToken = null;
		this.anyRecordHasRequestKey = false;
		this.anyRecordIsNewRequest = false;
		this.requestKeyToken = null;
		this.recordKeyDefinition = null;
		this.recordKeyDef = null;
		this.requestKeyFields = null;
		this.recordDefinitions = null;
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
	public FileRequestBuilder(final String name, final Element config) {
	super(name, config);
	
    log.debug(i18n.getString("startConfig", "request builder '" + name + "'"));
    
    // CONFIGURE OTHER OPTIONS
    final int bufferSize = DOMUtils.getIntAttribute(config, BUFFER_SIZE_ATT, true);
    this.inputBuffer = ByteBuffer.allocateDirect(bufferSize);
    this.maxRecordSize = DOMUtils.getIntAttribute(config, MAX_RECORD_SIZE_ATT, true);
    
    this.charset = Charset.forName(DOMUtils.getAttribute(config, ENCODING_ATT, true));
    this.decoder = this.charset.newDecoder();
    this.encoder = this.charset.newEncoder();
    
    // configure wildcard
    try {
      this.wildcard = 
        new EncodedString(this.encoder.encode(CharBuffer.wrap(new char[]{'*'})).array());
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("The wildcard string '*' could not be encoded using charset " + 
                                         this.charset.name(), e);
    }

    // configure record delimiter
    String delimiter = DOMUtils.getAttribute(config, RECORD_DELIMITER_ATT, false);
    if (delimiter == null || delimiter.length() == 0) {
      delimiter = DEFAULT_RECORD_DELIMITER;
    }
    final String recordDelimiter = StringEscapeUtils.unescapeJava(delimiter);
    try {
      this.encodedRecordDelimiter = 
        new EncodedString(this.encoder.encode(CharBuffer.wrap(recordDelimiter)).array());
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("The string '" + recordDelimiter + 
                                         "' could not be encoded using charset " + 
                                         this.charset.name(), e);
    }

    // configure field delimiter
    delimiter = DOMUtils.getAttribute(config, DELIMITER_ATT, false);
    if (delimiter != null && delimiter.length() == 0) {
      this.fieldDelimiter = new char[0];
      this.encodedFieldDelimiter = null;
    } else {
      try {
        this.fieldDelimiter = StringEscapeUtils.unescapeJava(delimiter).toCharArray();
        this.encodedFieldDelimiter = 
          new EncodedString(this.encoder.encode(CharBuffer.wrap(delimiter)).array());
      } catch (CharacterCodingException e) {
        throw new IllegalArgumentException("The string '" + delimiter + 
                                           "' could not be encoded using charset " + 
                                           this.charset.name(), e);
      }
    }
    
    // configure record-key
    final Element recordKey = DOMUtils.getElement(config, RECORD_KEY_ELEMENT, true);
    this.recordKeyDefinition = Collections.singletonList(getFieldPosition(recordKey)); 
    this.recordKeyDef = (FieldDefinition) this.recordKeyDefinition.get(0);
    final Element recordKeyElt = DOMUtils.getElement(config, RECORD_KEY_ELEMENT, true);
    String newRequest = DOMUtils.getAttribute(recordKeyElt, NEW_REQUEST_PATTERN_ATT, true);
    this.anyRecordIsNewRequest = newRequest.length() == 0 || "*".equals(newRequest);
    try {
      // get the token that indicates that a new request has begun
      this.newRequestToken = 
        new EncodedString(this.encoder.encode(CharBuffer.wrap(newRequest)).array());
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("The string '" + newRequest + 
                                         "' could not be encoded using charset " + 
                                         this.charset.name(), e);
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
    String requestKey = DOMUtils.getAttribute(requestKeyElt, REQUEST_KEY_PATTERN_ATT, true);
    this.anyRecordHasRequestKey = requestKey.length() == 0 || "*".equals(newRequest);
    try {
      this.requestKeyToken = 
        new EncodedString(this.encoder.encode(CharBuffer.wrap(requestKey)).array());
      this.desiredRecords.add(this.requestKeyToken);
    } catch (CharacterCodingException e) {
      throw new IllegalArgumentException("The string '" + requestKey + 
                                         "' could not be encoded using charset " + 
                                         this.charset.name(), e);
    }

    // configure records
    this.recordDefinitions = new HashMap();
    final NodeList records = DOMUtils.getElements(config, RECORD_ELEMENT);
    for (int i = 0; i < records.getLength(); i++) {
      final Element record = (Element) records.item(i);
      String recordName = DOMUtils.getAttribute(record, KEY_ATT, true);
      if (recordName.length() == 0) {
        recordName = "*";
      }
      EncodedString encodedName;
      try {
        encodedName = new EncodedString(this.encoder.encode(CharBuffer.wrap(recordName)).array());
      } catch (CharacterCodingException e) {
        throw new IllegalArgumentException("The string '" + recordName + 
                                           "' could not be encoded using charset " + 
                                           this.charset.name(), e);
      }
      this.recordDefinitions.put(encodedName, configFields(record));
      this.desiredRecords.add(encodedName);
    }
    
    this.encoder.reset();
    
    // Dumper config
    Element dumperElt = DOMUtils.getElement(config, DUMPER_ELT, false);
    if (dumperElt != null) {
      this.dumper = new OutputToFile(name + "::Dumper");
      try {
        this.dumper.configure(dumperElt);
      } catch (FilterException e) {
        throw new RuntimeException("Problems configuring request builder dumper.", e);
      }
    } else {
      this.dumper = null;
    }
    
    configureIgnoreOption(config);
    
    log.debug(i18n.getString("endConfig", "Request builder '" + name + "'"));
  }

	/***
	 * Configures the Ignore IO Errors Flag.
	 * @see P_ATTR_IGNORE_IO_ERRORS field comments.
	 * <p>
	 * Example:
	 * <pre>
	 *    Create a use example.
	 * </pre>
	 * </p>
	 * 
	 * @param config
	 */
	protected void configureIgnoreOption(Element config) {
		this.ignoreIOErrorsFlag = DOMUtils.getBooleanAttribute(config, P_ATTR_IGNORE_IO_ERRORS);
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
	protected List configFields(Element record) {
		final List fieldsDef = new ArrayList();
		final NodeList fields = DOMUtils.getElements(record, FIELD_ELEMENT);
		for (int j = 0; j < fields.getLength(); j++) {
			final Element field = (Element) fields.item(j);
			String fieldName = DOMUtils.getAttribute(field, NAME_ATT, true);
			final FieldDefinition fieldDef = getFieldPosition(field);
			fieldDef.fieldName = fieldName;
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
				throw new IllegalArgumentException(
						"Field position must be greater than 0.");
			}
			final int fieldLength = DOMUtils.getIntAttribute(field, LENGTH_ATT, true);
			if (fieldLength <= 0) {
				throw new IllegalArgumentException(
						"Field length must be greater than 0.");
			}
			fieldDef = new FieldDefinition(fieldPos, fieldLength);
		} else {
			final int fieldIndex = DOMUtils.getIntAttribute(field, INDEX_ATT, true) - 1;
			if (fieldIndex < 0) {
				throw new IllegalArgumentException(
						"Field index must be greater than 0.");
			}
			fieldDef = new FieldDefinition(fieldIndex);
		}
		String trim = DOMUtils.getAttribute(field, TRIM_ATT, false);
		if (trim != null && trim.length() > 0) {
			fieldDef.trimValue = Boolean.valueOf(trim).booleanValue();
		} else {
			fieldDef.trimValue = true;
		}
		String type = DOMUtils.getAttribute(field, TYPE_ATT, false);
		if (type != null && type.length() > 0) {
			try {
				fieldDef.setType(type);
			} catch (Exception e) {
				throw new IllegalArgumentException("Invalid type for field: [" + type
						+ "] - " + e.getMessage());
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

		final List files = new ArrayList();
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
	 * Reads from the input until it completes the buffer or no more data is
	 * available.
	 * 
	 * @param input
	 *          the input channel to be read.
	 * @param bb
	 *          the buffer used to hold the information read from the input.
	 * @return the number of bytes read.
	 */
	protected int read(ReadableByteChannel input, ByteBuffer bb)
			throws IOException {
		int total = bb.position();
		for (int size = 0; (total < bb.capacity())
				&& ((size = input.read(bb)) >= 0); total += size)
			;
		return total;
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
	protected ByteBuffer getField(final ByteBuffer record, final int pos,
			final int length) throws CharacterCodingException {
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
		if (this.encodedFieldDelimiter == null) {
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
		for (i = 0, pos = 0; (pos = NIOBufferUtils.findToken(buffer,
				this.encodedFieldDelimiter.data)) >= 0; i++, buffer.position(pos
				+ this.encodedFieldDelimiter.data.length)) {
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
	protected List getFields(CharSequence record) {
		if (this.fieldDelimiter.length == 0) {
			throw new IllegalStateException(
					"Cannot split fields because no delimiter was specified.");
		}
		final CharBuffer buffer = CharBuffer.wrap(record);
		buffer.rewind();
		if (buffer == null || buffer.remaining() == 0) {
			return null;
		}
		ArrayList result = new ArrayList();
		for (int i = 0; i < record.length();) {
			if ((i = NIOBufferUtils.findToken(buffer, this.fieldDelimiter)) < 0) {
				break;
			}
			result.add(buffer.subSequence(0, i - buffer.position()).toString());
			buffer.position(i + this.fieldDelimiter.length);
		}
		// add last field, if any
		if (buffer.hasRemaining()) {
			result.add(buffer.subSequence(0, buffer.remaining()).toString());
		}
		return result;
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
		if (this.recordKeyDef.isFixed) {
			if (this.recordKeyDef.position + this.recordKeyDef.length > record
					.limit()) {
				token = null;
			} else {
				token = new byte[this.recordKeyDef.length];
				record.position(this.recordKeyDef.position);
				record.get(token);
			}
		} else {
			final ByteBuffer field = getField(record, this.recordKeyDef.index);
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
			final EncodedString recordKey, final List fields) {
		if (recordKey == null
				|| !(this.anyRecordHasRequestKey || this.newRequestToken
						.equals(recordKey))) {
			return null;
		}

		String requestKey = "";
		Map atts = parseAttributes(record, this.requestKeyFields, fields,
				new HashMap());
		for (int i = 0; i < this.requestKeyFields.size(); i++) {
			FieldDefinition field = (FieldDefinition) this.requestKeyFields.get(i);
			String fieldValue;
			if (atts.containsKey(field.fieldName)) {
				fieldValue = atts.get(field.fieldName).toString();
			} else {
				fieldValue = "";
			}
			requestKey += (i > 0 ? this.requestKeyDelimiter : "") + fieldValue;
		}
		return (requestKey.length() == 0 ? null : requestKey);
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
	protected Map parseAttributes(CharSequence record, List fieldsDef,
			Map destination) {
		return parseAttributes(record, fieldsDef, null, destination);
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
	protected Map parseAttributes(CharSequence record, List fieldsDef,
			List fields, Map destination) {
		if (destination == null) {
			throw new IllegalArgumentException(
					"Attribute destination map cannot be null.");
		}
		if (fieldsDef != null && fieldsDef.size() > 0) {
			List values = null;
			if (this.fieldDelimiter.length != 0) {
				values = (fields != null ? fields : this.getFields(record));
			}
			for (int i = 0; i < fieldsDef.size(); i++) {
				FieldDefinition fieldDef = (FieldDefinition) fieldsDef.get(i);
				String value;
				if (fieldDef.isFixed) {
					value = getField(record, fieldDef.position, fieldDef.length);
				} else {
					if (fieldDef.index >= values.size()) {
						continue;
					}
					value = (String) values.get(fieldDef.index);
				}
				if (fieldDef.trimValue) {
					value = value.trim();
				}
				if (fieldDef.hasType()) {
					try {
						value = fieldDef.type.newInstance(new Object[] { value })
								.toString();
					} catch (Exception e) {
						throw new RuntimeException("Field '" + fieldDef.fieldName
								+ "' returned error " + "while trying to apply declared type: "
								+ e.getMessage(), e);
					}
				}
				destination.put(fieldDef.fieldName, value);
			}
		}
		return destination;
	}

	private boolean createRequest(final RequestFilter filter,
			final String pendingRequestKey, final long pendingRequestOffset,
			final long pendingRequestLength, final File file, final Map atts,
			final FileBuilderArgs params) {
		final boolean result;

		final PartialFileRequest request = new PartialFileRequest(
				pendingRequestKey, pendingRequestOffset, pendingRequestLength, file);
		if (params.getTransactionId() != null) {
			request.setTransactionId(params.getTransactionId());
		}
		if (params.getRequestParams() != null) {
			atts.putAll(params.getRequestParams());
		}
		request.setAttributes(atts);
		//Builds LOG messages.
		StringBuilder sb = new StringBuilder();		
		sb.append(this.name);
		sb.append(';');
		try {
			sb.append(file.getCanonicalPath());
		} catch (IOException e) {
		}
		sb.append(';');
		sb.append(file.getName());
		sb.append(';');
		sb.append(request.offset);
		sb.append(';');			
		sb.append(request.length);
		sb.append(';');	
		sb.append(request.getUserKey());
		sb.append(';');			
		//LOG the request
		BaseRequestBuilder.LOG_ALL.debug(sb.toString());			
		if (result = filter.accept(request)) {
			sb.append("ACCEPTED");
			BaseRequestBuilder.LOG_ACCEPTED.debug(sb.toString());		
			log.debug(i18n.getString("foundRequest", request));
		} else {
			sb.append("REJECTED");
			BaseRequestBuilder.LOG_REJECTED.debug(sb.toString());
			log.debug(i18n.getString("discardedRequest", request));
		}
		// prepare the atts map for the next request
		atts.clear();

		return result;
	}

//	private void logRequest(Writer writer, PartialFileRequest request) {
//		if (writer == null) {
//			return;
//		}
//
//		BufferedWriter w = (BufferedWriter) ((writer instanceof BufferedWriter) ? writer
//				: new BufferedWriter(writer));
//		StringBuffer sb = new StringBuffer();
//		try {
//			for (Iterator it = request.getAttributes().entrySet().iterator(); it
//					.hasNext();) {
//				Map.Entry entry = (Entry) it.next();
//				String key = (String) entry.getKey();
//				Object value = entry.getValue();
//				sb.append(key);
//				sb.append(';');
//				sb.append(value);
//				sb.append(';');				
//			}
//			w.append(sb.toString());			
//			w.newLine();
//			w.flush();
//			sb.setLength(0);
//		} catch (IOException e) {
//			log.error("Error during writer logging for request:" + request
//					+ ".Processing will continue and some log events will be lost.", e);
//		}
//	}

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

		final FileBuilderArgs params = parseFileArgs(args);

		final CharBuffer record = CharBuffer.allocate(this.maxRecordSize);
		final ByteBuffer delimiter = ByteBuffer
				.wrap(this.encodedRecordDelimiter.data);

		// request attributes
		Map atts = new HashMap();
		// record attributes
		Map recordAtts = new HashMap();

		for (Iterator it = params.getFiles().iterator(); it.hasNext()
				&& requestFilter.canAccept();) {

			final File file = (File) it.next();
			log.info(i18n.getString("parsingFile", file.getName()));

			final ReadableByteChannel reader;
			try {
				reader = NIOUtils.openFileForRead(file);
			} catch (IOException e) {
				if (!this.ignoreIOErrorsFlag) {
					throw new RuntimeException(i18n.getString("problemOpeningFile"), e);
				}
				log.error("Error processing file.File:" + file.getAbsolutePath(), e);
				continue;
			}

			this.inputBuffer.clear();
			record.clear();
			atts.clear();
			this.decoder.reset();

			// Reads all the input, mapping where each request starts and how much is
			// its length
			String pendingRequestKey = null;
			int recordLimit = 0, recordLength = 0;
			long bytesRead, fileOffset = 0, pendingRequestOffset = 0, pendingRequestLength = 0;
			boolean hasPendingRequest = false, dumpStream = false;
			WritableByteChannel dumperChannel = null;
			;
			try {
				for (;;) {
					final int currentPos = this.inputBuffer.position();
					final boolean isEOF = (bytesRead = read(reader, this.inputBuffer)) <= currentPos;
					this.inputBuffer.flip();

					for (;;) {
						final ByteBuffer rawRecord;

						// try to find a record delimiter
						recordLimit = NIOBufferUtils.findToken(this.inputBuffer,
								this.encodedRecordDelimiter.data);
						if (recordLimit < 0) {
							if (isEOF && this.inputBuffer.hasRemaining()) {
								// process last record (assume implicit record delimiter at EOF)
								rawRecord = this.inputBuffer.slice();
								recordLength = rawRecord.remaining();
							} else {
								break;
							}
						} else {
							final int limit = this.inputBuffer.limit();
							this.inputBuffer.limit(recordLimit);
							rawRecord = this.inputBuffer.slice();
							recordLength = rawRecord.remaining()
									+ this.encodedRecordDelimiter.data.length;
							this.inputBuffer.limit(limit);
							this.inputBuffer.position(recordLimit
									+ this.encodedRecordDelimiter.data.length);
						}

						// get record key
						final EncodedString recordKey = new EncodedString(
								getRecordKeyToken(rawRecord));
						if (recordKey.data != null) {
							// search for new request
							final boolean isNewRequest = Arrays.equals(recordKey.data,
									this.newRequestToken.data);
							if (isNewRequest || this.anyRecordIsNewRequest) {
								if (hasPendingRequest) {
									this.staticAttributes.insertStatics(atts);
									// Changed below line by MT in 2006-jun-19;
									// filter instead of requestFilter.
									createRequest(requestFilter, pendingRequestKey,
											pendingRequestOffset, pendingRequestLength, file, atts,
											params);
									pendingRequestOffset += pendingRequestLength;
									pendingRequestLength = 0;
									hasPendingRequest = false;
								}
								pendingRequestKey = null;
								hasPendingRequest = true;
							} // IF NEW REQUEST

							if ((this.desiredRecords.contains(recordKey))
									|| (this.anyRecordHasRequestKey)) {
								rawRecord.rewind();
								this.decoder.reset();
								this.decoder.decode(rawRecord, record, true);
								this.decoder.flush(record);
								record.flip();
								// we will manipulate the record's content as a String,
								// otherwise
								// we would have to worry about the record's position everytime.
								final CharSequence content = record.subSequence(record
										.position(), record.limit());

								// optimization: if we need to split fields, let's do it only
								// once!
								final List fields = (this.fieldDelimiter.length != 0 ? getFields(content)
										: null);

								// prepare to read attributes
								recordAtts.clear();

								// parse record attributes
								if (this.recordDefinitions.containsKey(this.wildcard)) {
									List fieldDefs = (List) this.recordDefinitions
											.get(this.wildcard);
									parseAttributes(record, fieldDefs, fields, recordAtts);
								}
								if (this.recordDefinitions.containsKey(recordKey)) {
									List fieldDefs = (List) this.recordDefinitions.get(recordKey);
									parseAttributes(record, fieldDefs, fields, recordAtts);
								}

								// try to fetch the requestKey
								final String requestKey = getRequestKey(content, recordKey,
										fields);
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

							// DUMPER FEATURE
							if (this.dumper != null) {
								if (isNewRequest || this.anyRecordIsNewRequest) {
									if (dumpStream) {
										// commit previous dump
										try {
											this.dumper.commit();
										} catch (Exception e) {
											this.dumper.rollback();
										} finally {
											dumperChannel = null;
											dumpStream = false;
										}
									}
									// dump new request
									final PartialFileRequest request = new PartialFileRequest(
											pendingRequestKey, pendingRequestOffset,
											pendingRequestLength, file);
									if (params.getTransactionId() != null) {
										request.setTransactionId(params.getTransactionId());
									}
									if (params.getRequestParams() != null) {
										atts.putAll(params.getRequestParams());
									}
									request.setAttributes(atts);
									// Changed below line by MT in 2006-jun-19;
									// filter instead of requestFilter.
									if (requestFilter.willAccept(request)) {
										try {
											this.dumper.prepare(request);
											dumperChannel = (WritableByteChannel) this.dumper
													.getInput(this.name);
											dumpStream = true;
										} catch (Exception e) {
											log.fatal("Could not dump request " + request, e);
											this.dumper.rollback();
											dumperChannel = null;
											dumpStream = false;
										}
									}
								}
								if (dumpStream) {
									rawRecord.rewind();
									NIOUtils.flush(dumperChannel, rawRecord.asReadOnlyBuffer());
									delimiter.rewind();
									NIOUtils.flush(dumperChannel, delimiter);
								}
							}

						} // IF VALID RECORD KEY

						pendingRequestLength += recordLength;
						if (isEOF) {
							// record is not valid and reader has reached EOF
							this.inputBuffer.position(this.inputBuffer.limit());
							break;
						}

					} // FOR EACH NEW RECORD

					if (isEOF && !this.inputBuffer.hasRemaining()) {
						break;
					}

					// Prepare byte buffer for next channel read
					this.inputBuffer.compact();

					// update file offset (points to end of last record found)
					fileOffset += bytesRead - this.inputBuffer.remaining();
				} // READ FILE

				// process last pending request after EOF (if any)
				if (hasPendingRequest) {
					this.staticAttributes.insertStatics(atts);
					// Changed below line by MT in 2006-jun-19;
					// filter instead of requestFilter.
					createRequest(requestFilter, pendingRequestKey, pendingRequestOffset,
							pendingRequestLength, file, atts, params);
				}

			} catch (IOException e) {
				log.error(i18n.getString("problemReadingFile") + ":" + file, e);				
				if (this.ignoreIOErrorsFlag) {
					removeRequestsFromFile(file, requestFilter);
				} else {
					throw new RuntimeException(i18n.getString("problemReadingFile"), e);
				}
			}

			// DUMPER FEATURE
			if (this.dumper != null && dumpStream) {
				try {
					this.dumper.commit();
				} catch (Exception e) {
					this.dumper.rollback();
				} finally {
					dumperChannel = null;
					dumpStream = false;
				}
			}

			try {
				reader.close();
			} catch (IOException e) {
				log.error(i18n.getString("problemClosingFile") + ":" + file, e);				
				if (this.ignoreIOErrorsFlag) {
					removeRequestsFromFile(file, requestFilter);					
				} else {
					throw new RuntimeException(i18n.getString("problemClosingFile"), e);
				}				
			}
		} // FOR EACH FILE

		// Changed below line by MT in 2006-jun-19;
		// filter instead of requestFilter.
		return requestFilter;
	}
	
	protected void removeRequestsFromFile(File file, RequestFilter filter) {
		Collection requests = filter.getAcceptedRequests();
		List newList = new ArrayList(requests);
		filter.reset();
		for (Iterator it = newList.iterator(); it.hasNext();) {
			FileRequest req = (FileRequest) it.next();
			File reqFile = req.getFile();
			if (!file.getAbsoluteFile().equals(reqFile.getAbsoluteFile())) {
				filter.accept(req);
			} else {
				log.info("Removing request from filter. Request:" + req);
			}
		}
		
	}
}
