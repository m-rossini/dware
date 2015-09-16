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

import junit.framework.TestCase;

import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.common.xml.sax.NIOInputSource;

/**
 * @author framos
 *
 */
public class MultiHandlerForwarderTest extends TestCase {

	
	public static final String SOURCE_FILE = "example1.xml";
	public static final String CONF_COUNT_CONTRATS = "count.xml";
	public static final String CONF_IGNORE_CONTRATS = "ignore.xml";
	public static final String CONF_DUPLICATE_CONTRATS = "duplicate.xml";
	
	public static final String NAMESPACE_PROPERTY = "http://xml.org/sax/features/namespaces";
	
	private MultiHandlerForwarder ch;
	
	
	protected void setUp() throws Exception {
		ch = new MultiHandlerForwarder();
	}
	
	/**
	 * No configuration meains : execute and leave without forwaring events  
	 *
	 */
	public void testNoConfiguration() {
		try {
			XMLReader reader = XMLReaderFactory.createXMLReader(); //parser.getXMLReader();
			reader.setFeature(NAMESPACE_PROPERTY, true);
			reader.setContentHandler(this.ch);
			NIOInputSource insrc = new NIOInputSource(MultiHandlerForwarderTest.class.getResourceAsStream(SOURCE_FILE));
			reader.parse(insrc);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
	public void testCountContacts() {
		try {
			CounterContentHandler.reset();

			this.ch.init(DOMUtils.openDocument(MultiHandlerForwarderTest.class.getResourceAsStream(CONF_COUNT_CONTRATS)));
			XMLReader reader = XMLReaderFactory.createXMLReader(); //parser.getXMLReader();
			reader.setFeature(NAMESPACE_PROPERTY, true);
			reader.setContentHandler(this.ch);
			NIOInputSource insrc = new NIOInputSource(MultiHandlerForwarderTest.class.getResourceAsStream(SOURCE_FILE));
			reader.parse(insrc);
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/name/"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/home/"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/home/chars()"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/mobile/"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/mobile/chars()"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/work/"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/work/chars()"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/email/"));
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testCountAndIgnoreContacts() {
		try {
			CounterContentHandler.reset();
			IgnoreEventsContentHandler.reset();
			
			this.ch.init(DOMUtils.openDocument(MultiHandlerForwarderTest.class.getResourceAsStream(CONF_IGNORE_CONTRATS)));
			XMLReader reader = XMLReaderFactory.createXMLReader(); 
			reader.setFeature(NAMESPACE_PROPERTY, true);
			reader.setContentHandler(this.ch);
			NIOInputSource insrc = new NIOInputSource(MultiHandlerForwarderTest.class.getResourceAsStream(SOURCE_FILE));
			reader.parse(insrc);
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/"));
			assertEquals(0, CounterContentHandler.getStartElementFor("/contacts/contact/name/"));
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/"));
			assertEquals(0, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/home/"));
			assertEquals(0, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/home/chars()"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/mobile/"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/mobile/chars()"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/work/"));
			assertEquals(4, CounterContentHandler.getStartElementFor("/contacts/contact/telephones/work/chars()"));
			assertEquals(0, CounterContentHandler.getStartElementFor("/contacts/contact/email/"));
			
			assertEquals(33, IgnoreEventsContentHandler.getIngoreCount());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	public void testDuplicateContacts() {
		try {
			CounterContentHandler.reset();
			IgnoreEventsContentHandler.reset();
			
			this.ch.init(DOMUtils.openDocument(MultiHandlerForwarderTest.class.getResourceAsStream(CONF_DUPLICATE_CONTRATS)));
			XMLReader reader = XMLReaderFactory.createXMLReader(); 
			reader.setFeature(NAMESPACE_PROPERTY, true);
			reader.setContentHandler(this.ch);
			NIOInputSource insrc = new NIOInputSource(MultiHandlerForwarderTest.class.getResourceAsStream(SOURCE_FILE));
			reader.parse(insrc);
			assertEquals(6, CounterContentHandler.getStartElementFor("/contacts/contact/"));
			assertEquals(53, IgnoreEventsContentHandler.getIngoreCount());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
	
}
