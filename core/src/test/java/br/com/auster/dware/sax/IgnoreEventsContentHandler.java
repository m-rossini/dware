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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * @author framos
 *
 */
public class IgnoreEventsContentHandler implements MultiHandlerReceiver {


	public static int ignoredElements;

	
	public IgnoreEventsContentHandler() {
	}


	public static int getIngoreCount() {
		return ignoredElements;
	}

	public static void reset() {
		ignoredElements = 0;
	}
	
	//
	// ContentHandler API methods
	//
	public void startDocument() throws SAXException {
		ignoredElements = 0;
	}

	public void endDocument() throws SAXException {
	}


	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		ignoredElements++;	
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	public void characters(char[] ch, int start, int length)throws SAXException {
	}
	
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}
	public void processingInstruction(String target, String data) throws SAXException {}
	public void setDocumentLocator(Locator locator) {}
	public void skippedEntity(String name) throws SAXException {}
	public void startPrefixMapping(String prefix, String uri) throws SAXException {}
	public void endPrefixMapping(String prefix) throws SAXException {}


	public void setContext(MultiHandlerContext _context) {
		// TODO Auto-generated method stub
		
	}

}
