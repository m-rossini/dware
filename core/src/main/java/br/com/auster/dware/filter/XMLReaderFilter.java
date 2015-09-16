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

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.common.xml.sax.NIOInputSource;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ThreadedFilter;

/**
 * This filter is used to read data from the input stream using a XMLReader. If
 * no XMLReader were especified, a default XMLReader will be created. 
 * 
 * <p>
 * To set the input: 
 * <pre>
 *  setInput(ReadableByteChannel)
 *  setInput(InputStream)
 *  setInput(Reader)
 *  setInput(Node)
 * </pre>
 * 
 * <p>
 * To set the output:
 * <pre>
 *  setOutput(ContentHandler)
 *  setOutput(WritableByteChannel)
 *  setOutput(OutputStream)
 *  setOutput(Writer)
 *  setOutput(File)
 *  setOutput(Node)
 * </pre>
 * 
 * @version $Id: XMLReaderFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public final class XMLReaderFilter extends ThreadedFilter {

  protected static final String XML_READER_ELEMENT = "xml-reader";

  protected static final String CLASS_NAME_ATTR = "class-name";

  protected XMLReader xmlReader = null;

  protected javax.xml.transform.Result result = null;

  protected javax.xml.transform.Source source = null;

  protected Transformer transformer;

  protected static final Logger log = Logger.getLogger(XMLReaderFilter.class);

  private final I18n i18n = I18n.getInstance(XMLReaderFilter.class);

  public XMLReaderFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public final synchronized void configure(Element config) throws FilterException {
    try {
      // Tries to find a XMLReader other than the default, to parse the input
      this.xmlReader = getXMLReaderInstance(config);
      if (this.xmlReader != null) {
        log.debug("Using the following class as the XMLReader for input: "
                  + xmlReader.getClass().getName());
      } else {
        // If could not find the XMLReader, use the default
        log.warn(i18n.getString("defaultXMLReader"));
      }

      this.transformer = SAXTransformerFactory.newInstance().newTransformer();
    } catch (Exception e) {
      throw new FilterException(e);
    }
  }

  /**
   * Given a XMLReader configuration, creates an instance of it and returns it.
   * 
   * @param config
   *          the DOM tree corresponding to the XMLReader configuration to be
   *          passed to its constructor.
   * @return a XMLReader instance based on the information of the given config
   *         element.
   */
  private static final XMLReader getXMLReaderInstance(Element config)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
      IllegalAccessException, java.lang.reflect.InvocationTargetException {
    String className = null;
    try {
      className = DOMUtils.getAttribute(config, CLASS_NAME_ATTR, true);
      config = DOMUtils.getElement(config, XML_READER_ELEMENT, true);
    } catch (IllegalArgumentException e) {
      return null;
    }

    Class[] c = { Element.class };
    Object[] o = { config };
    return (XMLReader) Class.forName(className).getConstructor(c).newInstance(o);
  }

  /**
   * Starts the XMLReader, reading from the input and sending events to the
   * output.
   */
  public synchronized final void process() throws FilterException {
    if (this.result == null)
      throw new FilterException(i18n.getString("outputNotSet"));
    if (this.source == null)
      throw new FilterException(i18n.getString("inputNotSet"));

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
  public synchronized final void setInput(String sourceName, Object input) throws ConnectException,
      UnsupportedOperationException {
    if (input instanceof Node) {
      this.source = new DOMSource((Node) input);
    } else {
      final InputSource inputSource;
      if (input instanceof ReadableByteChannel) {
        inputSource = new NIOInputSource((ReadableByteChannel) input);
      } else if (input instanceof InputStream) {
        inputSource = new InputSource((InputStream) input);
      } else if (input instanceof Reader) {
        inputSource = new InputSource((Reader) input);
      } else {
        throw new ConnectException(i18n.getString("unsupportedInputType", input.getClass(),
                                                  XMLReaderFilter.class.getName()));
      }

      if (this.xmlReader == null)
        this.source = new SAXSource(inputSource);
      else
        this.source = new SAXSource(this.xmlReader, inputSource);
    }
  }

  /**
   * Sets the output for this filter.
   */
  public final void setOutput(String sinkName, Object output) throws ConnectException,
      UnsupportedOperationException {
    if (output instanceof ContentHandler) {
      this.result = new SAXResult((ContentHandler) output);
    } else if (output instanceof WritableByteChannel) {
      this.result = new StreamResult(Channels.newOutputStream((WritableByteChannel) output));
    } else if (output instanceof OutputStream) {
      this.result = new StreamResult((OutputStream) output);
    } else if (output instanceof Writer) {
      this.result = new StreamResult((Writer) output);
    } else if (output instanceof File) {
      this.result = new StreamResult((File) output);
    } else if (output instanceof Node) {
      this.result = new DOMResult((Node) output);
    } else {
      throw new ConnectException(i18n.getString("unsupportedOutputType", output.getClass(),
                                                XMLReaderFilter.class.getName()));
    }
  }
}
