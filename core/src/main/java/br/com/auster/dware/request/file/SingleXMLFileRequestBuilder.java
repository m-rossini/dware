/*
 * Copyright (c) 2004 TTI Tecnologia. All Rights Reserved.
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
 * Created on Jul 4, 2005
 */
package br.com.auster.dware.request.file;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.io.IOUtils;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

/**
 * Builds requests where the input is a XML file. This builder expects that each XML file represents only one request. For multiple 
 * 	requests in a single XML file, another builder needs to be implemented.
 * <p>
 * When defining the configuration of this builder, a request key can be specified. In its definition, the components of this key can be
 * 	xpath-like expressions defining the path to the xml element, and the attribute to be included in the construction of the key. Also, 
 * 	each information added to the key can be delimited by a character, specified in the configuration. Remember : all fields selected to
 *  compose the request key will be treated as <code>String</code> when retrieved from the XML file.
 * <p>
 * For extracting request level parameters from the XML file, a list of <code>&lt;request-info&gt;</code> should be defined. Each one 
 * 	must have a unique name - they will be handled in a Map structure, and a xpath-like expression just like the one mentioned in the
 * 	request key configuration. 
 * <p>
 * Note : All xpath-like expressions <strong>must</strong> be specified from root tag. They cannot be set as relative paths. 
 * 
 * @author framos
 * @version $Id: SingleXMLFileRequestBuilder.java 95 2005-08-12 21:47:31Z framos $
 */
public class SingleXMLFileRequestBuilder implements RequestBuilder {

	
	private static final Logger log = LogFactory.getLogger(SingleXMLFileRequestBuilder.class);
	private final I18n i18n = I18n.getInstance(SingleXMLFileRequestBuilder.class);
	
	// configuration elements
	public static final String REQUEST_KEY_ELEMENT = "request-key";
	public static final String REQUEST_KEY_DELIMITER_ATTR = "delimiter";
	public static final String REQUEST_KEY_FIELD_ELEMENT = "field";
	
	public static final String REQUEST_ATTRIBUTES_LIST_ELEMENT = "request-attrs";
	public static final String REQUEST_ATTRIBUTES_EACH_ELEMENT = "attribute";

	// used for key & info definition
	public static final String REQUEST_ATTRIBUTES_NAME_ATTR = "name";
	public static final String REQUEST_ATTRIBUTES_PATH_ATTR = "path";
	
	
	// parameters sent by API call, at runtime
	public static final String REQUEST_INPUT_FILE_ARG = "filename";
	public static final String REQUEST_TRANSACTION_ID_ARG = "transaction-id";
	public static final String REQUEST_REQUEST_PARAMS_ARG = "request-params";
	
	
	
	private String name;

	private String keyDelimiter;
	private List keyMap;
	private Map attrMap;
	
	
	
	public SingleXMLFileRequestBuilder(String _name, Element _config) {
		this.name = _name;
		log.debug("configuring builder " + this.name);
		Element keyDefinition = DOMUtils.getElement(_config, REQUEST_KEY_ELEMENT, true);
		this.keyDelimiter = DOMUtils.getAttribute(keyDefinition, REQUEST_KEY_DELIMITER_ATTR, false);
		log.debug("request key delimiter : " + this.keyDelimiter);
		
		keyMap = new ArrayList();
		NodeList elements = DOMUtils.getElements(keyDefinition, REQUEST_KEY_FIELD_ELEMENT);
		for (int i=0; i < elements.getLength(); i++) {
			String path = DOMUtils.getAttribute((Element)elements.item(i), REQUEST_ATTRIBUTES_PATH_ATTR, true);
			log.debug("using path " + path + " to create request key");
			keyMap.add(path);
		}

		elements = DOMUtils.getElements(DOMUtils.getElement(_config, REQUEST_ATTRIBUTES_LIST_ELEMENT, true), REQUEST_ATTRIBUTES_EACH_ELEMENT);
		attrMap = new HashMap();
		for (int i=0; i < elements.getLength(); i++) {
			String name = DOMUtils.getAttribute((Element)elements.item(i), REQUEST_ATTRIBUTES_NAME_ATTR, true);
			String path = DOMUtils.getAttribute((Element)elements.item(i), REQUEST_ATTRIBUTES_PATH_ATTR, true);
			log.debug("configuring additional attribute " + name + " with path "  + path);
			attrMap.put(name, path);
		}
		
	}
	
	
	/**
	 * @see br.com.auster.dware.request.RequestBuilder#getName()
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(java.util.Map)
	 */
	public RequestFilter createRequests(Map _args) {
		return this.createRequests(null, _args);
	}

	/**
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(br.com.auster.dware.request.RequestFilter, java.util.Map)
	 */
	public RequestFilter createRequests(RequestFilter _filter, Map _args) {
		if (_filter == null) {
			log.debug(i18n.getString("noFilterFound"));
			_filter = new HashRequestFilter(); 
		}
		
		String filename = (String) _args.get(REQUEST_INPUT_FILE_ARG);
		log.debug(i18n.getString("parsingFile",filename));
		if (filename == null) {
			log.error(i18n.getString("problemOpeningFile"));
			throw new RuntimeException(i18n.getString("problemOpeningFile"));
		}
		File file = new File(filename);
		if (! file.exists()) {
			log.error(i18n.getString("problemOpeningFile"));
			throw new RuntimeException(i18n.getString("problemOpeningFile"));
		}

		try {
			Map results = parseFile(file);
			
			String requestKey = buildKey(results);
			Map addAttributes = buildAddAtributes(results);
		
			PartialFileRequest request = new PartialFileRequest(requestKey, 0, file.length(), file);
			if (_filter.accept(request)) {
				log.debug(i18n.getString("foundRequest", request));
				// setting transaction id
				if (_args.get(REQUEST_TRANSACTION_ID_ARG) != null) {
					request.setTransactionId((String) _args.get(REQUEST_TRANSACTION_ID_ARG));
				}
				// setting add. atributes
				request.setAttributes(new HashMap());
				request.getAttributes().putAll(addAttributes);
				// setting atributes from request API call
				Map attrs = (Map) _args.get(REQUEST_REQUEST_PARAMS_ARG);
				if (attrs != null) {
					request.getAttributes().putAll(attrs);
				}
			} else {
				log.warn(i18n.getString("discardedRequest", request));
			}
			
		} catch (Exception e) {
			log.error(i18n.getString("parsingError"),e);
			throw new RuntimeException(e);
		}
		return _filter;
	}
	
	/*
	 * parses the XML file, collecting information to create the request key and to fill the map of additional parameters
	 */
	private Map parseFile(File _file) throws Exception {
		Map searchingPaths = buildSearchPaths();
		String currentPath = "";
		int counter = 0, event = -1, total = searchingPaths.size();
		Map searchingResults = new HashMap();

		InputStream stream = IOUtils.openFileForRead(_file);
		XMLInputFactory factory = XMLInputFactory.newInstance();
		XMLStreamReader parser = factory.createXMLStreamReader(stream);
		// walking through the XML file
		while (((event = parser.next()) != XMLStreamConstants.END_DOCUMENT) && (counter < total)) {
			if (event == XMLStreamConstants.START_ELEMENT) {
				//builds current path
				currentPath += "/" + parser.getLocalName();
				// if its a path we are looking for
				log.debug("lookup at " + currentPath);
				if (searchingPaths.containsKey(currentPath)) {
					// add path counter
					counter++;
					// get map of attributes we are interested in
					Map searchingAttributes = (Map) searchingPaths.get(currentPath);
					// iterate over the attributes and get the values for those we are looking for
					for (int i=0; i<parser.getAttributeCount(); i++) {
						String localName = parser.getAttributeLocalName(i);
						if (searchingAttributes.containsKey(localName)) {
							searchingResults.put(currentPath + "@" + localName, parser.getAttributeValue(i));
						}
					}
//					String namespace = parser.getNamespaceURI();
//					for (String attrs : searchingAttributes.keySet()) {
//						String value = parser.getAttributeValue(namespace, attrs);
//						log.debug("lookup at " + currentPath + "@" + attrs + " = value '" + value + "'");
//						searchingResults.put(currentPath + "@" + attrs, value);
//					}
				}
			} else if (event == XMLStreamConstants.END_ELEMENT) {
				// remove the path element from the current
				int elementLen = parser.getLocalName().length()  + 1; // +1 for the "/"
				currentPath = currentPath.substring(0, currentPath.length()-elementLen+1);
			}
		}
		parser.close();
		stream.close();
		return searchingResults;
	}
	
	/*
	 * Builds map of paths to search for in the xml element. The first map identifies the path up to the 
	 * 	element name, and the second map points to each attribute of that element.
	 */
	private Map buildSearchPaths() {
		Map paths = new HashMap();
		paths = buildSearchPaths(keyMap, paths);
		paths = buildSearchPaths(attrMap.values(), paths);
		return paths;
	}
	
	private Map buildSearchPaths(Collection _source, Map paths) {
		for (Iterator it = _source.iterator(); it.hasNext();) {
            String path = (String) it.next();
			int index = path.indexOf("@");
			String attrPath = null;
			// need to find the position of the attribute
			if (index <= 0) {
				throw new RuntimeException(i18n.getString("pathConfigurationError"));
			} 
			attrPath = path.substring(index+1);
			Map attrSet = (Map) paths.get(path.substring(0, index));
			if (attrSet == null) {
				attrSet = new HashMap();
				paths.put(path.substring(0, index), attrSet);
			}
			attrSet.put(attrPath, null);
		}
		return paths;
	}
	
	/*
	 * Creates the key to the newly created request, as defined
	 */
	private String buildKey(Map _results) {
		if (keyMap.size() == 0) {
			return null;
		}
		String key = "";
		for (Iterator it = this.keyMap.iterator(); it.hasNext();) {
            String keyPath = (String) it.next();
			if ((_results.containsKey(keyPath)) && (_results.get(keyPath) != null)) {
				key += _results.get(keyPath) + (this.keyDelimiter == null ? "" : this.keyDelimiter);
			}
		}
		if (key != null) {
			return key.substring(0, key.length() - (this.keyDelimiter == null ? 0 : this.keyDelimiter.length()));
		}
		log.warn(i18n.getString("nullKeyCreated"));
		return null;
	}
	
	/*
	 * Extracts additional information to be added to the map of parameters of the request 
	 */
	private Map buildAddAtributes(Map _results) {
		if (attrMap.size() == 0) {
			return null;
		}
		Map resultMap = new HashMap();
		for (Iterator it = attrMap.keySet().iterator(); it.hasNext();) {
             String key = (String) it.next();
			 String path = (String) attrMap.get(key);
			 if (_results.containsKey(path)) {
				 resultMap.put(key, _results.get(path));
			 }
		}
		return resultMap;
	}
	
}
