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
 * Created on Jul 7, 2005
 */
package br.com.auster.dware.filter;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.channels.ReadableByteChannel;

import org.apache.log4j.Logger;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.sax.NIOInputSource;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ThreadedFilter;

import com.sun.xml.fastinfoset.sax.SAXDocumentParser;


/**
 * <p><b>Title:</b> XMLInfosetReaderFilter</p>
 * <p><b>Description:</b> TODO class description</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id: XMLInfosetReaderFilter.java 75 2005-07-25 23:13:38Z rbarone $
 */
public class XMLInfosetReaderFilter extends ThreadedFilter {

  protected XMLReader parser = new SAXDocumentParser();
  
  protected InputSource source = null;
  
  protected static final Logger log = Logger.getLogger(XMLInfosetReaderFilter.class);

  private static final I18n i18n = I18n.getInstance(XMLReaderFilter.class);
  
  public XMLInfosetReaderFilter(String name) {
    super(name);
  }
  
  /**
   * @inheritDoc
   */
  public void process() throws FilterException {
    if (this.source == null) {
      throw new FilterException(i18n.getString("inputNotSet"));
    }
    if (this.parser.getContentHandler() == null) {
      throw new FilterException(i18n.getString("outputNotSet"));
    }
    try {
      this.parser.parse(source);
    } catch (IOException e) {
      throw new FilterException(e);
    } catch (SAXException e) {
      throw new FilterException(e);
    }
    this.source = null;
  }
  
  /**
   * Sets the input for this filter.
   */
  public synchronized final void setInput(String sourceName, Object input) throws ConnectException {
    if (input instanceof ReadableByteChannel) {
      source = new NIOInputSource((ReadableByteChannel) input);
    } else if (input instanceof InputStream) {
      source = new InputSource((InputStream) input);
    } else if (input instanceof Reader) {
      source = new InputSource((Reader) input);
    } else {
      throw new ConnectException(i18n.getString("unsupportedInputType", input.getClass(),
                                                XMLInfosetReaderFilter.class.getName()));
    }
  }
  
  /**
   * Sets the output for this filter.
   */
  public final void setOutput(String sinkName, Object output) throws ConnectException,
      UnsupportedOperationException {
    if (output instanceof ContentHandler) {
      this.parser.setContentHandler((ContentHandler) output);
    } else {
      throw new ConnectException(i18n.getString("unsupportedOutputType", output.getClass(),
                                                XMLReaderFilter.class.getName()));
    }
  }

}
