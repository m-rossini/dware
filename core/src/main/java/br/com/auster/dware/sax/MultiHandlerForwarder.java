/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 * Created on Jun 5, 2006
 */
package br.com.auster.dware.sax;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import br.com.auster.common.xml.DOMUtils;

/**
 * This is a <code>ContentHandler</code> implementation which, based on the path being handled, forwards
 * 	the SAX events to other <code>ContentHandler</code>s. This other handlers must implement the <code>MultiHandlerReceiver</code>
 * 	interface.
 * <p>
 * When forwarding a specific path to another <code>ContentHandler</code>, all the path block (i.e., the sub-elements) 
 *   will also be forwarded. That, of course, unless another <code>ContentHandler</code> is configured for a more inner
 *   path. 
 * <p>
 * An optional <code>ContentHandler</code> can be set for all those paths not included in the path configuration. If this
 * 	default handler is not defined, then the SAX events will be discarded.
 * <p>
 * To enable information to walk through all those handlers, an instance of <code>MultiHandlerContext</code> is always
 * 	present. 
 * 
 * @author framos
 * @version $Id$
 */
public class MultiHandlerForwarder extends DefaultHandler {

	
	public static final String XML_PATH_SEPARATOR = "/";
	
	public static final String CONFIG_PATHLIST_ELEMENT = "xml-paths";
	public static final String CONFIG_PATH_ELEMENT = "path";
	public static final String CONFIG_DEFAULT_ELEMENT = "default";
	public static final String CONFIG_PATH_VALUE_ATTR = "value";
	public static final String CONFIG_CONTENTHANDLER_ATTR = "handler";
	
	
	private static final Logger log = Logger.getLogger(MultiHandlerForwarder.class);
	
	
	protected MultiHandlerContext context;
	protected Map configuredPaths;
	protected Stack inPath;
	protected MultiHandlerReceiver defaultHandler;
	

	
	public MultiHandlerForwarder() {
		this(null);
	}
	
	public MultiHandlerForwarder(Element _configuration) {		
		this.context = new MultiHandlerContext();
		this.configuredPaths = new HashMap();
		if (_configuration != null) {
			this.init(_configuration);
		}
		this.inPath = new Stack();
	}
	
	public void init(Element _configuration) {
		Map tmp = new HashMap();
		// building configuration map
		Element pathListXML = DOMUtils.getElement(_configuration, CONFIG_PATHLIST_ELEMENT, true);
		NodeList pathList = DOMUtils.getElements(pathListXML, CONFIG_PATH_ELEMENT);
		for (int i=0; i < pathList.getLength(); i++) {
			Element handlerInfo = (Element) pathList.item(i);
			String path = normalizePath(DOMUtils.getAttribute(handlerInfo, CONFIG_PATH_VALUE_ATTR, true));
			ContentHandler ch = this.initHandler(path, handlerInfo);
			if (! tmp.containsKey(path)) {
				tmp.put(path, new LinkedList());
			}			
			((List)tmp.get(path)).add(ch);
		}
		// reading optional default handler configuration
		Element defaultInfo = (Element) DOMUtils.getElement(pathListXML, CONFIG_DEFAULT_ELEMENT, false);
		if (defaultInfo != null) {
			String path = normalizePath(DOMUtils.getAttribute(defaultInfo, CONFIG_PATH_VALUE_ATTR, true));
			this.defaultHandler = this.initHandler(path, defaultInfo);
		}
		// save all contenthandlers as ContentHandler[] for faster access
		for (Iterator it = tmp.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry entry = (Map.Entry) it.next();
			List handlers = (List) entry.getValue();
			this.configuredPaths.put(entry.getKey(), 
					                 (ContentHandler[]) handlers.toArray(new ContentHandler[0]));
		}
	}

	public void setDefaultHandler(MultiHandlerReceiver _handler) {
		this.defaultHandler = _handler;
	}
	
	private String normalizePath(String _path) {
		// adding starting and ending slash 
		if (! _path.startsWith(XML_PATH_SEPARATOR)) { _path = XML_PATH_SEPARATOR + _path; }
		if (! _path.endsWith(XML_PATH_SEPARATOR)) { _path += XML_PATH_SEPARATOR; }
		return _path;
	}
	
	private MultiHandlerReceiver initHandler(String _path, Element _handlerInfo) {
		// building content handler instance
		try {
			Class clazz = Class.forName(DOMUtils.getAttribute(_handlerInfo, CONFIG_CONTENTHANDLER_ATTR, true));
			log.debug("configured " + clazz.getName() + " for path " + _path);
			MultiHandlerReceiver handler = (MultiHandlerReceiver) clazz.newInstance();
			handler.setContext(this.context);
			return handler;
		} catch (ClassNotFoundException cnfe) {
			throw new IllegalArgumentException(cnfe);
		} catch (InstantiationException ie) {
			throw new IllegalArgumentException(ie);
		} catch (IllegalAccessException iae) {
			throw new IllegalArgumentException(iae);
		}
	}

	public MultiHandlerContext getCurrentContext() {
		return this.context;
	}
	
	//
	// ContentHandler API methods
	//
	public void startDocument() throws SAXException {
		this.context.reset();
		if (this.configuredPaths.containsKey(XML_PATH_SEPARATOR)) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get(XML_PATH_SEPARATOR);
			for (int i=0; i < ch.length; i++) {
				ch[i].startDocument();
			}
			this.inPath.push(XML_PATH_SEPARATOR);
		} else if (this.defaultHandler != null) {
			this.defaultHandler.startDocument();
		}
	}

	public void endDocument() throws SAXException {
		if (this.configuredPaths.containsKey(XML_PATH_SEPARATOR)) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get(XML_PATH_SEPARATOR);
			for (int i=0; i < ch.length; i++) {
				ch[i].endDocument();
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.endDocument();
		}
	}

	public void startElement(String _uri, String _localName, String _qName, Attributes _attributes) throws SAXException {
		// updating xml path
		this.context.startedPath(_localName);
		String key = null;
		// first checks for a pre configured path
		if (this.configuredPaths.containsKey(this.context.getCurrentPath())) {
			key = this.context.getCurrentPath();
			this.inPath.push(key);
		// then checks if there any path was previously found 
		} else if (!this.inPath.empty()) {
			key = (String)this.inPath.peek();
		}
		// if any path was found, forward this event; otherwise, ignore it
		if (key != null) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get(key);
			for (int i=0; i < ch.length; i++) {
				ch[i].startElement(_uri, _localName, _qName, _attributes);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.startElement(_uri, _localName, _qName, _attributes);
		}
	}

	public void endElement(String _uri, String _localName, String _qName) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			String key = (String) this.inPath.peek();
			// pop only when found end of initial tag
			if (this.context.getCurrentPath().equals(key)) {
				this.inPath.pop();
			}
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get(key);
			for (int i=0; i < ch.length; i++) {
				ch[i].endElement(_uri, _localName, _qName);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.endElement(_uri, _localName, _qName);
		}
		// updating xml path
		this.context.endedPath(_localName);
	}

	public void characters(char[] _chars, int _start, int _length) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].characters(_chars, _start, _length);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.characters(_chars, _start, _length);
		}
	}

	public void setDocumentLocator(Locator _locator) {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].setDocumentLocator(_locator);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.setDocumentLocator(_locator);
		}
	}
	
	public void startPrefixMapping(String _prefix, String _uri) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].startPrefixMapping(_prefix, _uri);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.startPrefixMapping(_prefix, _uri);
		}
	}
	
	public void endPrefixMapping(String _prefix) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].endPrefixMapping(_prefix);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.endPrefixMapping(_prefix);
		}
	}
	
	public void ignorableWhitespace(char[] _chars, int _start, int _length) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].ignorableWhitespace(_chars, _start, _length);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.ignorableWhitespace(_chars, _start, _length);
		}
	}
	
	public void processingInstruction(String _target, String _data) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].processingInstruction(_target, _data);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.processingInstruction(_target, _data);
		}
	}
	
	public void skippedEntity(String _name) throws SAXException {
		// if any path was found, forward this event; otherwise, ignore it
		if (!this.inPath.empty()) {
			ContentHandler[] ch = (ContentHandler[]) this.configuredPaths.get((String) this.inPath.peek());
			for (int i=0; i < ch.length; i++) {
				ch[i].skippedEntity(_name);
			}
		} else if (this.defaultHandler != null) {
			this.defaultHandler.skippedEntity(_name);
		}
	}
}
