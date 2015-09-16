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
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.filter;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.Request;

/**
 * Receives SAX events and writes them to an output.
 * This Filter uses the pull parser API in order to serialize the XML.
 * It provides exactly the same functionality as XMLSerializer filter (This filter uses the XALAN Serializer).
 * 
 * This filter acts as a ContentHandler to receive Sax Events and for each call-back it calls
 * the appropriated method of the XMLStreamWriter(Pull-parser API).
 * 
 * It implements also the method option that can be "text" or "xml", and this implementation works
 * the same way as in normal serializers.
 * 
 * Additionally it supports the encoding attribute to provide encoding conversion seamless.
 * 
 * What is not supported:
 * 1.Indentation
 * 2.WhiteSpace
 * 
 * A Difference in XML output (method="xml") is:
 * 
 * In Xalan Serializer the following structure will be created:
 * 	<tag attr="x"/>
 * 
 * This serializer creates the the same xml as folowing:
 * 	<tag attr="x"><\tag>
 * 
 * <p>
 * To get the input: <code>ContentHandler getInput(null)</code>
 * <p>
 * To set the output: <code>getOutput(WritableByteChannel)</code>
 * 
 * @version $Id: STaXSerializerFilter.java 134 2006-02-10 12:51:56Z mtengelm $
 */
public final class STaXSerializerFilter extends DefaultFilter implements
    ContentHandler {

	protected static final String	METHOD_ATTR	     = "method";
	protected static final String	METHOD_ATTR_TEXT	= "text";
	protected static final String	METHOD_ATTR_XML	 = "xml";
	protected static final String	ENCODING_ATTR	   = "encoding";

	// protected static final String INDENT_ATTR = "indent";

	// protected static final String INDENT_AMOUNT_ATTR = "indent-amount";

	private String	              outMethod	       = null;
	private String	              encoding;
	private XMLOutputFactory	    factory	         = XMLOutputFactory
	                                                   .newInstance();
	private XMLStreamWriter	      writer;

	public STaXSerializerFilter(String name) {
		super(name);
	}

	/**
	 * Configures this filter.
	 */
	public final synchronized void configure(Element config) {
		this.outMethod = DOMUtils.getAttribute(config, METHOD_ATTR, false);
		if ((null == this.outMethod) || (this.outMethod.length() == 0)
		    || this.outMethod.equals("")) {
			this.outMethod = METHOD_ATTR_XML;
		} else if ((!this.outMethod.toLowerCase().equals(METHOD_ATTR_TEXT))
		    && (!this.outMethod.toLowerCase().equals(METHOD_ATTR_XML))) {
			this.outMethod = METHOD_ATTR_XML;
		}

		this.encoding = DOMUtils.getAttribute(config, ENCODING_ATTR, true);
		// this.properties =
		// OutputPropertiesFactory.getDefaultMethodProperties(DOMUtils.getAttribute(config,
		// METHOD_ATTR, true));
		// this.properties.setProperty(OutputKeys.INDENT,
		// DOMUtils.getBooleanAttribute(config, INDENT_ATTR) ? "yes" : "false");
		// this.properties.setProperty(OutputKeys.ENCODING,
		// DOMUtils.getAttribute(config, ENCODING_ATTR,true));
		/*
		 * try {
		 * this.properties.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,
		 * Integer .toString(DOMUtils.getIntAttribute(config, INDENT_AMOUNT_ATTR,
		 * true))); } catch (IllegalArgumentException e) { }
		 */
	}

	/**
	 * Creates the serializer for the SAX events.
	 */
	public final void prepare(Request request) {
	// this.serializer = SerializerFactory.getSerializer(this.properties);
	}

	/**
	 * Gets the serializer, which is a ContentHandler.
	 */
	public final Object getInput(String sourceName) throws ConnectException {
		return this;
	}

	/**
	 * Sets the WritableByteChannel output for this filter.
	 */
	public final void setOutput(String sinkName, Object output) {
		if (output instanceof WritableByteChannel) {
			output = Channels.newOutputStream((WritableByteChannel) output);
		}
		try {
			this.writer = factory.createXMLStreamWriter((OutputStream) output,
			    this.encoding);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator arg0) {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		if (this.outMethod.equals(METHOD_ATTR_TEXT)) {
			return;
		}
		try {
			writer.writeStartDocument();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		if (!this.outMethod.equals(METHOD_ATTR_TEXT)) {
			try {
				writer.writeEndDocument();
			} catch (XMLStreamException e) {
				e.printStackTrace();
			}
		}
		try {
			writer.flush();
			writer.close();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String,
	 *      java.lang.String)
	 */
	public void startPrefixMapping(String arg0, String arg1) throws SAXException {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
	 */
	public void endPrefixMapping(String arg0) throws SAXException {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String prefix,
	    Attributes atts) throws SAXException {
		if (this.outMethod.equals(METHOD_ATTR_TEXT)) {
			return;
		}
		try {
			if ("".equals(uri)) {
				writer.writeStartElement(localName);
			} else {
				writer.writeStartElement(prefix, localName, uri);
			}
			for (int i = 0; i < atts.getLength(); i++) {
				if ("".equals(atts.getURI(i))) {
					writer.writeAttribute(atts.getLocalName(i), atts.getValue(i));
				} else {
					writer.writeAttribute(atts.getQName(i), atts.getURI(i), atts
					    .getLocalName(i), atts.getValue(i));
				}
			}
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String arg0, String arg1, String arg2)
	    throws SAXException {
		if (this.outMethod.equals(METHOD_ATTR_TEXT)) {
			return;
		}
		try {
			writer.writeEndElement();
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
		if (!this.outMethod.equals(METHOD_ATTR_TEXT)) {
			return;
		}
		try {
			writer.writeCharacters(arg0, arg1, arg2);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] arg0, int arg1, int arg2)
	    throws SAXException {}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String,
	 *      java.lang.String)
	 */
	public void processingInstruction(String arg0, String arg1)
	    throws SAXException {
		if (this.outMethod.equals(METHOD_ATTR_TEXT)) {
			return;
		}
		try {
			writer.writeProcessingInstruction(arg0, arg1);
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
	 */
	public void skippedEntity(String arg0) throws SAXException {}
}
