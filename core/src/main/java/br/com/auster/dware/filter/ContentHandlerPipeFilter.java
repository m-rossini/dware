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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.ContentHandler;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.sax.ContentHandlerPipe;

/**
 * Creates a ContentHandler to handle the SAX events from the previous filter
 * and sends SAX events as a result.
 * 
 * <p>
 * To get the input: ContentHandler getInput()
 * <p>
 * To set the output: setOutput(ContentHandler)
 * 
 * @version $Id: ContentHandlerPipeFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public final class ContentHandlerPipeFilter extends DefaultFilter {

  protected static final String CONTENT_HANDLER_ELEMENT = "content-handler";

  protected static final String CLASS_NAME_ATTR = "class-name";

  protected ContentHandlerPipe handler;

  private final static Logger log = Logger.getLogger(ContentHandlerPipeFilter.class);
  
  private final I18n i18n = I18n.getInstance(this.getClass());

  public ContentHandlerPipeFilter(String name) throws Exception {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public final synchronized void configure(Element config) throws FilterException {
    // Gets the content handler to understand the SAX events
    try {
      this.handler = ContentHandlerPipeFilter.getContentHandlerInstance(config);
    } catch (Throwable e) {
      throw new FilterException(e);
    }

    log.info(i18n.getString("usingContentHandler", handler.getClass().getName(), this
        .getFilterName()));
  }

  /**
   * Given a ContentHandler configuration, creates an instance of it and returns
   * it.
   * 
   * @param config
   *          the DOM tree corresponding to the ContentHandler configuration to
   *          be passed to its constructor.
   * @return a ContentHandler instance based on the information of the given
   *         config element.
   */
  private static final ContentHandlerPipe getContentHandlerInstance(Element config)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
      IllegalAccessException, java.lang.reflect.InvocationTargetException {
    String className = DOMUtils.getAttribute(config, CLASS_NAME_ATTR, true);
    config = DOMUtils.getElement(config, CONTENT_HANDLER_ELEMENT, true);

    Class[] c = { Element.class };
    Object[] o = { config };
    return (ContentHandlerPipe) Class.forName(className).getConstructor(c).newInstance(o);
  }

  /**
   * Gets the ContentHandler.
   */
  public final Object getInput(String sourceName) {
    return this.handler;
  }

  /**
   * Sets the output for this filter.
   */
  public void setOutput(String sinkName, Object output) throws ConnectException {
    if (output instanceof ContentHandler) {
      this.handler.setOutput((ContentHandler) output);
    } else {
      throw new ConnectException(i18n.getString("unsupportedOutputType", output.getClass(),
                                                ContentHandlerPipeFilter.class.getName()));
    }
  }
}
