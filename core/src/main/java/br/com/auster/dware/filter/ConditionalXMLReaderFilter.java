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
 * Created on Aug 31, 2005
 */
package br.com.auster.dware.filter;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.common.xml.sax.NIOInputSource;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;
 
/**
 * An extension of the {@link br.com.auster.dware.filter.XMLReader XML Reader} filter, to allow multiple configurations. Each time the  
 * 	filter is executed, it checks the request information to select the best reader configuration.
 * <p>
 * Each condition should describe the {@link br.com.auster.dware.graph.Request} attribute and the expected value for it. When more than 
 *  one condition are configured for a single reader, then <strong>ALL</strong> conditions must match. Also, the configurations and their
 *  conditions are evaluated without a defined order, so if a request matches the conditions of two or more reader configurations, 
 *  there is no guarantee that the <i>expected</i> xml reader will be selected.  
 * <p>
 * The syntax for defining request attributes in the conditions is described in <a href="http://jakarta.apache.org/commons/beanutils/" target="_top">Apache 
 *   BeanUtils Project</a>. This syntax allows for searcing attributes, in Javabeans-like objects, with simple direct attributes, nested attributes, 
 *   collections or maps. For example :
 *   <center>
 *   <table width="90%" border="1" cellpadding="3">
 *   	<tr><td><strong> script syntax</strong></td><td><strong> looking for...</strong></td></tr>
 *   	<tr><td><code>offset</code></td><td>attribute <code>offset</code> from the request object</td></tr>
 *      <tr><td><code>attributes(account)</code></td><td>value of key 'account', as configured, in the <code>attribute</code> map of the request</td></tr>
 *      <tr><td><code>attributes.account</code></td><td>same as above</td></tr>
 *      <tr><td><code>attributes.filenames.xml</code></td><td>the value of the key 'xml', from the map contained in the key 'filenames' of the <code>attribute</code> map of the request</td></tr>
 *   </table>
 *   </center>
 * <p>
 * Each xml reader is instantiated and initialized during the filter configuration. During the process phase, only conditions 
 *  are evaluated, to select the appropriate xml reader instance.  
 * <p>
 * The XML configuration for this filter works like bellow :
 * <br><br>
 * <code>
 * &lt;config&gt;<br>
 * &nbsp;&nbsp;&nbsp;&lt;-- for as many configurations as needed --&gt;<br>
 * &nbsp;&nbsp;&nbsp;&lt;xml-reader class-name="a.b.c"&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;-- for as many conditions as needed --&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;condition attribute="attr1.attr2" value="vlr1" check="operator"/&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;(...)<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;-- the configuration needed by the reader --&gt;<br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;config ... /&gt;<br>
 * &nbsp;&nbsp;&nbsp;&lt;/xml-reader&gt;<br>
 * &nbsp;&nbsp;&nbsp;(...)<br>
 * &lt;/config&gt;<br>
 * </code>
 * <p>
 * For possible operators, in the <code>check</code> attribute, can be used : <code>equal</code>, <code>lower</code>, 
 * 	<code>lower-equal</code>, <code>greater</code> or <code>greater-equal</code>. If another operator is specified, then
 * 	it will warn the misconfiguration in the log file and will default to <code>equal</code>.
 * 
 * @author framos
 * @version $Id: ConditionalXMLReaderFilter.java 115 2005-09-06 19:55:06Z mtengelm $
 */
public class ConditionalXMLReaderFilter extends ThreadedFilter {

	
	
	protected static final String XML_READER_ELEMENT = "xml-reader";
	protected static final String CLASS_NAME_ATTR = "class-name";
    protected static final String CONFIG_NAME_ATTR = "name";

	protected static final String XML_READER_CONFIG_ELEMENT = "config";
	protected static final String XML_READER_CONDITION_ELEMENT = "condition";
	protected static final String XML_READER_CONDITION_ATTRNAME_ELEMENT = "attribute";
	protected static final String XML_READER_CONDITION_ATTRVALUE_ELEMENT = "value";
	protected static final String XML_READER_CONDITION_CHECK_ELEMENT = "check";

	protected static final String CONDITION_CHECK_EQUAL = "equal";
	protected static final String CONDITION_CHECK_LOWER = "lower";
	protected static final String CONDITION_CHECK_LOWER_EQUAL = "lower-equal";
	protected static final String CONDITION_CHECK_GREATER = "greater";
	protected static final String CONDITION_CHECK_GREATER_EQUAL = "greater-equal";
	
	
	protected Map xmlReaders = null;
    protected Map configNames = null;

	protected Result result = null;
	protected Source source = null;
	protected InputSource inputSource = null;

	protected Transformer transformer;
	protected XMLReader currentReader;

	protected static final Logger log = Logger.getLogger(ConditionalXMLReaderFilter.class);

	private final I18n i18n = I18n.getInstance(ConditionalXMLReaderFilter.class);

	public ConditionalXMLReaderFilter(String name) {
		super(name);
		this.xmlReaders = new HashMap();
        this.configNames = new HashMap();
	}

	/**
	 * Configures this filter.
	 */
	public synchronized void configure(Element _config) throws FilterException {

		try {
			this.xmlReaders.clear();
			NodeList readersConfiguration = DOMUtils.getElements(_config, XML_READER_ELEMENT);
			for (int i=0; i < readersConfiguration.getLength(); i++) {
				Element readerConf = (Element) readersConfiguration.item(i);
				Set conditions = createConditions(DOMUtils.getElements(readerConf, XML_READER_CONDITION_ELEMENT));
				XMLReader readerObj = getXMLReaderInstance(readerConf);
				if (readerObj != null) {
					log.debug("Using the following class as the XMLReader for input: " + readerObj.getClass().getName());
				} else {
					log.warn(i18n.getString("defaultXMLReader"));
				}
				this.xmlReaders.put(conditions, readerObj);
                String configName = DOMUtils.getAttribute(readerConf, CONFIG_NAME_ATTR, false);
                this.configNames.put(readerObj, configName);
			}
			log.debug("map of XML readers is : " + this.xmlReaders);
			
			this.transformer = SAXTransformerFactory.newInstance().newTransformer();
		} catch (Exception e) {
			throw new FilterException(e);
		}
	}

	/**
	 * Populates a set with the conditions configured for each xml reader option
	 */
	private Set createConditions(NodeList _conditionsConfiguration) {
		Set conditionSet = new HashSet();
		for (int i=0; i < _conditionsConfiguration.getLength(); i++) {
			Condition c = new Condition(DOMUtils.getAttribute((Element)_conditionsConfiguration.item(i), XML_READER_CONDITION_ATTRNAME_ELEMENT, true),
			                            DOMUtils.getAttribute((Element)_conditionsConfiguration.item(i), XML_READER_CONDITION_ATTRVALUE_ELEMENT, true),
			                            DOMUtils.getAttribute((Element)_conditionsConfiguration.item(i), XML_READER_CONDITION_CHECK_ELEMENT, false) );
			log.debug("added condition : " + c);
			conditionSet.add(c);
		}
		return conditionSet;
	}
	
	/**
	 * Given a XMLReader configuration, creates an instance of it and returns
	 * it.
	 * 
	 * @param config
	 *            the DOM tree corresponding to the XMLReader configuration to
	 *            be passed to its constructor.
	 * @return a XMLReader instance based on the information of the given config
	 *         element.
	 */
	private XMLReader getXMLReaderInstance(Element config) 
					throws ClassNotFoundException, NoSuchMethodException, 
						   InstantiationException, IllegalAccessException, InvocationTargetException {
		
		String className = null;
		try {
			className = DOMUtils.getAttribute(config, CLASS_NAME_ATTR, true);
			config = DOMUtils.getElement(config, XML_READER_CONFIG_ELEMENT, true);
		} catch (IllegalArgumentException e) {
			return null;
		}
		Class[] c = { Element.class };
		Object[] o = { config };
		return (XMLReader) Class.forName(className).getConstructor(c) .newInstance(o);
	}

	
	/**
	 * @see br.com.auster.dware.graph.ThreadedFilter#prepare(br.com.auster.dware.graph.Request)
	 */
	public void prepare(Request _request) throws FilterException {
		super.prepare(_request);
		
		this.source = null;
		this.result = null;
		this.inputSource = null;
		
		this.currentReader = findReaderForCurrentRequest(_request);
	}
	
	/**
	 * Searches the internal Map, populated with the information read during configuration-time, looking for the xml reader
	 *	that best suits the current request. If no xml reader was found, the <code>null</code> will be returned. This is not
	 *	a error situation, since some inputs can be handled without a XML reader.
	 */
	private XMLReader findReaderForCurrentRequest(Request _request) throws FilterException {
		
		Iterator iterator = this.xmlReaders.entrySet().iterator();
		while (iterator.hasNext()) {
   			Map.Entry entry = (Map.Entry) iterator.next();
            if (validateConditions(_request, (Collection) entry.getKey())) {
               if (log.isDebugEnabled()) {
                  log.info("using XML reader : " + entry.getValue()
                        + " for handling request " + _request);
                  log.info("For above Reader the named configuration is "
                        + this.configNames.get(entry.getValue()));
               }
               return (XMLReader) entry.getValue();
            }
		}
		return null;
	}
	
	/**
	 * Validates the conditions against the request object
	 */
	private boolean validateConditions(Request _request, Collection _conditions) throws FilterException {
		
		try {
			log.debug("request being evaluated is " + _request);
			if ((_conditions == null) || (_conditions.size() <= 0)) {
				return true;
			}
			Iterator iterator = _conditions.iterator();
			while (iterator.hasNext()) {
				Condition cond = (Condition) iterator.next();
				String value = null;
				log.debug("evaluating condition : " + cond);
				value = BeanUtils.getNestedProperty(_request, cond.getAttributePath());
				log.debug("found value : " + value);
				
				int comparison = cond.getConditionValue().compareTo(value);
				// checking by configured option
				if ( (cond.getCheckCondition().equals(CONDITION_CHECK_EQUAL) && (comparison != 0))       ||  
				     (cond.getCheckCondition().equals(CONDITION_CHECK_LOWER) && (comparison <= 0))       || 
				     (cond.getCheckCondition().equals(CONDITION_CHECK_LOWER_EQUAL) && (comparison < 0))  ||
				     (cond.getCheckCondition().equals(CONDITION_CHECK_GREATER) && (comparison >= 0))     || 
				     (cond.getCheckCondition().equals(CONDITION_CHECK_GREATER_EQUAL) && (comparison > 0)) ) {
					return false;
				}
			}
			return true;
		} catch (NoSuchMethodException nsme) {
			throw new FilterException(nsme);
		} catch (IllegalAccessException iae) {
			throw new FilterException(iae);
		} catch (InvocationTargetException ite) {
			throw new FilterException(ite);
		}
	}
	
	/**
	 * Starts the XMLReader, reading from the input and sending events to the
	 * output.
	 */
	public synchronized void process() throws FilterException {

		if (this.result == null) {
			throw new FilterException(i18n.getString("outputNotSet"));
		}
		
		if (this.source == null) {
			if ((this.currentReader == null) && (this.inputSource == null)) {
				throw new FilterException(i18n.getString("inputNotSet"));
			}
			// dinamically define which reader to use for this request
			if (this.currentReader == null) {
				this.source = new SAXSource(this.inputSource);
			} else {
				this.source = new SAXSource(this.currentReader, this.inputSource);
			}
		}
		
		try {
			// Starts the input interpretation (transformation)
			this.transformer.transform(this.source, this.result);
		} catch (TransformerException e) {
			throw new FilterException(e);
		} finally {
			this.source = null;
			this.result = null;
		}
	}

	/**
	 * Sets the input for this filter.
	 */
	public synchronized void setInput(String sourceName, Object input) throws ConnectException, UnsupportedOperationException {
		
		if (input instanceof Node) {
			this.source = new DOMSource((Node) input);
		} else {
			if (input instanceof ReadableByteChannel) {
				this.inputSource = new NIOInputSource((ReadableByteChannel) input);
			} else if (input instanceof InputStream) {
				this.inputSource = new InputSource((InputStream) input);
			} else if (input instanceof Reader) {
				this.inputSource = new InputSource((Reader) input);
			} else {
				throw new ConnectException(i18n.getString( "unsupportedInputType", input.getClass(), XMLReaderFilter.class.getName()));
			}
		}
	}

	/**
	 * Sets the output for this filter.
	 */
	public void setOutput(String sinkName, Object output)
			throws ConnectException, UnsupportedOperationException {
		if (output instanceof ContentHandler) {
			this.result = new SAXResult((ContentHandler) output);
		} else if (output instanceof WritableByteChannel) {
			this.result = new StreamResult(Channels
					.newOutputStream((WritableByteChannel) output));
		} else if (output instanceof OutputStream) {
			this.result = new StreamResult((OutputStream) output);
		} else if (output instanceof Writer) {
			this.result = new StreamResult((Writer) output);
		} else if (output instanceof File) {
			this.result = new StreamResult((File) output);
		} else if (output instanceof Node) {
			this.result = new DOMResult((Node) output);
		} else {
			throw new ConnectException(i18n.getString("unsupportedOutputType", output.getClass(), XMLReaderFilter.class.getName()));
		}
	}

	
	/**
	 * Internal representation of a condition
	 */
	static class Condition {
		
		private String attributePath;
		private String value;
		private String check;
		
		public Condition(String _attribute, String _value, String _check) {
			this.attributePath = _attribute;
			this.value = _value;
			if ((_check == null) || (_check.trim().length() <= 0)) {
				_check = CONDITION_CHECK_EQUAL;
			} else  if (! (_check.equals(CONDITION_CHECK_GREATER_EQUAL) || _check.equals(CONDITION_CHECK_LOWER) || _check.equals(CONDITION_CHECK_LOWER_EQUAL) ||
				_check.equals(CONDITION_CHECK_GREATER) || _check.equals(CONDITION_CHECK_GREATER_EQUAL))) {
				_check = CONDITION_CHECK_GREATER_EQUAL;
			}
			this.check = _check;
		}
		
		public String getAttributePath() {
			return this.attributePath;
		}
		
		public String getConditionValue() {
			return this.value;
		}
		
		public String getCheckCondition() {
			return this.check;
		}
		
		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object _obj) {
			try {
				Condition c = (Condition) _obj;
				return (this.attributePath.equals(c.attributePath) && this.value.equals(c.value) && this.check.equals(c.check));
			} catch (ClassCastException cce) {
				return false;
			}
		}
		
		/**
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return "[Condition] {" + attributePath + " " + check + " '" + value + "' }";
		}
	}
}
