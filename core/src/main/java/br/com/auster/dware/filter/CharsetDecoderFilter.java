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
 * Created on Aug 12, 2005
 */
package br.com.auster.dware.filter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import org.apache.commons.io.CopyUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ThreadedFilter;


/**
 * <p><b>Title:</b> CharsetDecoderFilter</p>
 * <p><b>Description:</b> TODO class description</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id: CharsetDecoderFilter.java 353 2008-01-17 14:38:35Z mtengelm $
 */
public class CharsetDecoderFilter extends ThreadedFilter {

  private static final Logger log = Logger.getLogger(CharsetDecoderFilter.class);
  
  /**
   * "{@value}" - default charset used when no enconding has been configured.
   */
  public static final String DEFAULT_CHARSET = "US-ASCII";
  
  /**
   * "{@value}" - attribute name for the charset configuration.
   */
  protected static final String ENCODING_ATTR = "encoding";
  
  private Reader reader = null;
  private PipedReader output = null;
  
  private CharsetDecoder decoder;
  
  /**
   * @param name
   */
  public CharsetDecoderFilter(String name) {
    super(name);
  }
  
  /**
   * Configures this filter from the specified node.
   * 
   * The only optional supported attribute name is "{@value #ENCODING_ATTR}",
   * whose value is the charset name that must be used for decoding.
   * 
   * @param config @inheritDoc
   */
  public synchronized void configure(Element config) {
    String dec = DOMUtils.getAttribute(config, ENCODING_ATTR, false);
    if (dec == null || dec.length() == 0) {
      dec = DEFAULT_CHARSET; 
    }
    this.decoder = Charset.forName(dec).newDecoder();
  }

  /**
   * @inheritDoc
   */
  public void process() throws FilterException {
    this.decoder.reset();
    
    PipedWriter writer = null;
    try {
      writer = new PipedWriter(this.output);
      CopyUtils.copy(this.reader, writer);
    } catch(IOException e) {
      throw new FilterException("Unexpected exception while copying text.", e);
    } finally {
      if (writer != null) {
        try {
          writer.close();
        } catch (IOException e) {
          log.error("Error while closing the writer.", e);
        }
      }
    }
    
    this.reader = null;
    this.output = null;
  }
  
  /**
   * @inheritDoc
   */
  public void commit() {
    if (this.output != null) {
      try {
        this.output.close();
      } catch (IOException e) {
        log.error("Error while closing the output.", e);
      }
    }
  }
  
  /**
   * @inheritDoc
   */
  public void rollback() {
    this.commit();
  }
  
  
  /**
   * Sets the input for decoding.
   * 
   * <p>
   * The accepted types are:
   * <ul>
   * <li><code>java.nio.ReadableByteChannel</code></li>
   * <li><code>java.io.InputStream</code></li>
   * </ul>
   * </p>
   * 
   * @param filterName @inheritDoc
   * @param input
   *          the input from which bytes will be decoded - see method
   *          description for allowed types.
   */
  public void setInput(String filterName, Object input) throws ConnectException, UnsupportedOperationException {
    if (input instanceof ReadableByteChannel) {
      this.reader = Channels.newReader((ReadableByteChannel) input, this.decoder, -1);
    } else if (input instanceof InputStream) {
      this.reader = new BufferedReader(new InputStreamReader((InputStream) input, this.decoder));
    } else {
      throw new UnsupportedOperationException("CharsetDecoderFilter does not accept input of type "
                                              + input.getClass().getName());
    }
  }
  

  /**
   * Returns a <code>java.io.Reader</code> that can be used to read the
   * decoded chars.
   * 
   * @param filterName @inheritDoc
   * @return a <code>java.io.Reader</code> as the output.
   */
  public Object getOutput(String filterName) throws ConnectException, UnsupportedOperationException {
    this.output = new PipedReader();
    return this.output;
  }

}
