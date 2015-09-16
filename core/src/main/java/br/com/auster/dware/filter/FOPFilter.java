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
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.avalon.framework.logger.Log4JLogger;
import org.apache.fop.apps.Driver;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Options;
import org.apache.fop.messaging.MessageHandler;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.XMLFilter;
import org.xml.sax.helpers.XMLFilterImpl;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;



/**
 * Reads events from a ContentHandler that describes a FO formatted XML, pass
 * them to the FOP engine (a <code>Driver</code> instance) and writes the
 * output to an output stream.
 * 
 * <p>
 * To get the input: ContentHandler getInput(null)
 * <p>
 * To set the output: setOutput(OutputStream|WritableByteChannel)
 * 
 * @version $Id
 */
public final class FOPFilter extends DefaultFilter {

  protected static final String FOP_CONF_ATTR = "fop-config-path";
  
  protected static final Log4JLogger fopLogger = new Log4JLogger(Logger.getLogger("org.apache.fop"));
  
  static {
    MessageHandler.setScreenLogger(fopLogger);
  }

  // Instance attributes
  protected final Driver driver = new Driver();
  protected XMLFilter xmlFilter = new XMLFilterImpl();

  public FOPFilter(String name) {
    super(name);
    this.driver.setLogger(fopLogger);
  }

  public final synchronized void configure(Element config) throws FilterException {
    // Create a FOP driver and configures it.
    try {
      new Options(new File(DOMUtils.getAttribute(config, FOP_CONF_ATTR, true)));
    } catch (IllegalArgumentException e) {
    } catch (FOPException e) {
      throw new FilterException(e);
    }
    this.driver.setRenderer(Driver.RENDER_PDF);
  }

  /**
   * Resets the FOP engine for next work.
   */
  public final void commit() {
    this.driver.reset();
    this.xmlFilter.setContentHandler(null);
  }

  /**
   * Just calls commit.
   */
  public final void rollback() {
    this.commit();
  }
  
  /**
   * Gets the FOP input ContentHandler. The previous filter must send SAX events
   * to this filter in order to FOP process the them.
   */
  public final Object getInput(String sourceName) {
    // see comment bellow, inside setOuput() method, in order
    // to understand why we need to use a XMLFilter as the input
    return this.xmlFilter;
  }

  /**
   * Sets the FOP output. The output may be an <code>OutputStream</code>
   * instance or a <code>WritableByteChannel</code> instance.
   */
  public final void setOutput(String sinkName, Object output) throws ConnectException,
      UnsupportedOperationException {
    if (output instanceof java.io.OutputStream) {
      this.driver.setOutputStream((OutputStream) output);
    } else if (output instanceof WritableByteChannel) {
      this.driver.setOutputStream(Channels.newOutputStream((WritableByteChannel) output));
    } else {
      throw new ConnectException(I18n.getInstance(FOPFilter.class)
          .getString("unsupportedOutputType", output.getClass(), FOPFilter.class.getName()));
    }
    // it is mandatory that the OutputStream is configured before 
    // passing the ContentHandler, otherwise FOP will throw a
    // NullPointerException - that's why we need to use a 
    // XMLFilter between the input and output
    this.xmlFilter.setContentHandler(this.driver.getContentHandler());
  }
}
