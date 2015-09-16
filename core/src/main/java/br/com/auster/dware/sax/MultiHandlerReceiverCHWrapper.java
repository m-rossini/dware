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
 * Created on 06/06/2006
 */
package br.com.auster.dware.sax;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author framos
 * @version $Id: MultiHandlerReceiverCHWrapper.java 201 2006-06-11 17:50:17Z framos $
 */
public class MultiHandlerReceiverCHWrapper extends DefaultHandler implements MultiHandlerReceiver {

	
	protected ContentHandler wrapped;
	
	
	public void setContentHandler(ContentHandler _wrapped) {
		this.wrapped = _wrapped;
	}

	
	

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.startPrefixMapping(prefix, uri);
	}
	
	public void startDocument() throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.startDocument();
	}
	
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.startElement(uri, localName, qName, attributes);
	}

	public void skippedEntity(String name) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.skippedEntity(name);
	}
	
	public void setDocumentLocator(Locator locator) {
		if (this.wrapped != null) 
			this.wrapped.setDocumentLocator(locator);
	}
	
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.ignorableWhitespace(ch, start, length);
	}
	
	public void processingInstruction(String target, String data) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.processingInstruction(target, data);
	}
	
	public void endDocument() throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.endDocument();
	}
	
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.endElement(uri, localName, qName);
	}
	
	public void endPrefixMapping(String prefix) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.endPrefixMapping(prefix);
	}
	
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (this.wrapped != null) 
			this.wrapped.characters(ch, start, length);
	}

	
	/**
	 * @see br.com.auster.dware.sax.MultiHandlerReceiver#setContext(br.com.auster.dware.sax.MultiHandlerContext)
	 */
	public void setContext(MultiHandlerContext _context) {
		// do nothing
	}

}
