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
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * @author framos
 *
 */
public class CounterContentHandler implements MultiHandlerReceiver {


	private static Map elements = new HashMap();
	private static boolean startedDoc = false;
	private static boolean endedDoc = false;
	private MultiHandlerContext context;
	
//	private String path;
	
	
	
	public CounterContentHandler() {
//		this.path = MultiHandlerForwarder.XML_PATH_SEPARATOR;
	}


	public static  boolean receivedStartDocument() {
		return startedDoc;
	}

	public static boolean receivedEndDocument() {
		return endedDoc;
	}
	
	public static void reset() {
		endedDoc = false;
		startedDoc = false;
		elements = new HashMap();
	}
	
	public static int getStartElementFor(String _path) {
		Integer i = (Integer) elements.get(_path);
		if (i == null) {
			return 0;
		}
		return i.intValue();
	}
	
	public static Iterator getStartElementPaths() {
		return elements.keySet().iterator();
	}
	
	//
	// ContentHandler API methods
	//
	public void startDocument() throws SAXException {
		startedDoc = true;
	}

	public void endDocument() throws SAXException {
		endedDoc = true;
	}


	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		int len = 1;
		if (elements.containsKey(this.context.getCurrentPath())) {
			len += ((Integer)elements.get(this.context.getCurrentPath())).intValue();
		}
		elements.put(this.context.getCurrentPath(), new Integer(len));
	}

	public void endElement(String uri, String localName, String qName) throws SAXException {
	}

	public void characters(char[] ch, int start, int length)throws SAXException {
		int len = 1;
		if (elements.containsKey(this.context.getCurrentPath() + "chars()")) {
			len += ((Integer)elements.get(this.context.getCurrentPath() + "chars()")).intValue();
		}
		elements.put(this.context.getCurrentPath() + "chars()", new Integer(len));
	}
	
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}
	public void processingInstruction(String target, String data) throws SAXException {}
	public void setDocumentLocator(Locator locator) {}
	public void skippedEntity(String name) throws SAXException {}
	public void startPrefixMapping(String prefix, String uri) throws SAXException {}
	public void endPrefixMapping(String prefix) throws SAXException {}


	public void setContext(MultiHandlerContext _context) {
		this.context = _context;
	}

}
