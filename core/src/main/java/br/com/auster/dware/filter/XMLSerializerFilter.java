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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.Request;

/**
 * Receives SAX events and writes them to an output.
 * 
 * <p>
 * To get the input: <code>ContentHandler getInput(null)</code>
 * <p>
 * To set the output: <code>getOutput(WritableByteChannel)</code>
 * 
 * @version $Id: XMLSerializerFilter.java 2 2005-04-20 21:22:27Z rbarone $
 */
public final class XMLSerializerFilter extends DefaultFilter {

  protected static final String METHOD_ATTR = "method";

  protected static final String INDENT_ATTR = "indent";

  protected static final String INDENT_AMOUNT_ATTR = "indent-amount";

  protected static final String ENCODING_ATTR = "encoding";

  protected Serializer serializer;

  protected Properties properties;

  public XMLSerializerFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public final synchronized void configure(Element config) {
    this.properties = OutputPropertiesFactory.getDefaultMethodProperties(DOMUtils
        .getAttribute(config, METHOD_ATTR, true));
    this.properties.setProperty(OutputKeys.INDENT, DOMUtils
        .getBooleanAttribute(config, INDENT_ATTR) ? "yes" : "false");
    this.properties.setProperty(OutputKeys.ENCODING, DOMUtils.getAttribute(config, ENCODING_ATTR,
                                                                           true));
    try {
      this.properties.setProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, Integer
          .toString(DOMUtils.getIntAttribute(config, INDENT_AMOUNT_ATTR, true)));
    } catch (IllegalArgumentException e) {
    }
  }

  /**
   * Creates the serializer for the SAX events.
   */
  public final void prepare(Request request) {
    this.serializer = SerializerFactory.getSerializer(this.properties);
  }

  /**
   * Gets the serializer, which is a ContentHandler.
   */
  public final Object getInput(String sourceName) throws ConnectException {
    try {
      return this.serializer.asContentHandler();
    } catch (IOException e) {
      throw new ConnectException(e);
    }
  }

  /**
   * Sets the WritableByteChannel output for this filter.
   */
  public final void setOutput(String sinkName, Object output) {
    if (output instanceof WritableByteChannel) {
      output = Channels.newOutputStream((WritableByteChannel) output);
    }
    this.serializer.setOutputStream((OutputStream) output);
  }
}
