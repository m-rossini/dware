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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import br.com.auster.common.stats.ProcessingStats;
import br.com.auster.common.stats.StatsMapping;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;



/**
 * This filter is used to copy SAX events and ByPass it to another
 * ContentHandler. The Another CH is this case is the JAXB UnmarshallerHandler
 * Class (Acctually it implements the ContentHandler Interface).
 * 
 * <p>
 * To get the input: ContentHandler getInput(null)
 * <p>
 * To set the output: setOutput(Object)
 * <p>
 * NOTE: This is the Next Filter, that must be an ObjectProcessor.
 * 
 * @author Marcos Tengelmann
 * @version $Id: ObjectManagerFilter.java 362 2008-07-12 19:42:00Z lmorozow $
 */
public class ObjectManagerFilter extends DefaultFilter implements ContentHandler {

  // #####################
  // Class Variables
  // #####################

  // A context-path can be a list of packages separeted by ":"
  protected static final String PACKAGE_ATTR = "context-path";

  // If this option is true, we will set validation on Unmarshalling.
  // Default is false
  protected static final String VALIDATING_ATTR = "validating";

  // #####################
  // Instance Variables
  // #####################
  protected static final Logger log = Logger.getLogger(ObjectManagerFilter.class);

  final I18n i18n = I18n.getInstance(this.getClass());

  protected String contextPath;

  private UnmarshallerHandler unmarshallerHandler;

  private ObjectProcessor objProcessor;

  private String currentElement;

  private boolean validating;

  public ObjectManagerFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public void configure(Element config) throws FilterException {
    this.contextPath = DOMUtils.getAttribute(config, PACKAGE_ATTR, true);
    this.validating = Boolean.valueOf(DOMUtils.getAttribute(config, VALIDATING_ATTR, false))
        .booleanValue();
    try {
      // Creates the Context, Unmarshaller and ContextHandler.
      JAXBContext context = JAXBContext.newInstance(this.contextPath);
      Unmarshaller unm = context.createUnmarshaller();
      unm.setValidating(this.validating);
      this.unmarshallerHandler = unm.getUnmarshallerHandler();
    } catch (JAXBException e) {
      throw new FilterException(e);
      // } catch(ParserConfigurationException e) {
      // throw new FilterException(e);
    }
  }

  /**
   * Gets the input for this filter, which is a Content Handler. And in this
   * case the CH is this class itself.
   */
  public Object getInput(String sourceName) {
    return this;
  }

  /**
   * Sets the Output for this filter.
   * 
   */
  public void setOutput(String sourceName, Object objProcessor) {
    this.objProcessor = (ObjectProcessor) objProcessor;
  }

  // ############################################
  // Start of Content Handler Implementation
  // Please note, that JAXB Content Handler can ONLY unmarshall a XML data
  // AFTER
  // receiving endDocument() event.
  // Once we are using the JAXB ContentHandler, we need to drive all events to
  // to JAXB ContentHandler.
  // ############################################
  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endDocument()
   */
  public void endDocument() throws SAXException {
    this.unmarshallerHandler.endDocument();
    // handler.endDocument();
    javax.xml.bind.Element obj = null;
    // UnmarshallerHandler uh = (UnmarshallerHandler) jaxbobj;
    StatsMapping stats = ProcessingStats.starting(this.objProcessor.getClass(), "processElement()");
    try {
      obj = (javax.xml.bind.Element) this.unmarshallerHandler.getResult();
      this.objProcessor.processElement(obj);
      // this.objProcessor.processElement(handler.getDOM());
    } catch (IllegalStateException e) {
      e.printStackTrace();
    } catch (JAXBException e) {
      e.printStackTrace();
    } catch (FilterException e) {
      e.printStackTrace();
    } catch (IllegalArgumentException e) {
      log.fatal(i18n.getString("JAXBIllegal", this.currentElement), e);
    } finally {
      stats.finished();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startDocument()
   */
  public void startDocument() throws SAXException {
    this.unmarshallerHandler.startDocument();
    // handler.startDocument();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#characters(char[], int, int)
   */
  public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
    this.unmarshallerHandler.characters(arg0, arg1, arg2);
    // handler.characters(arg0, arg1, arg2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
   */
  public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
    this.unmarshallerHandler.ignorableWhitespace(arg0, arg1, arg2);
    // handler.ignorableWhitespace(arg0, arg1, arg2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
   */
  public void endPrefixMapping(String arg0) throws SAXException {
    this.unmarshallerHandler.endPrefixMapping(arg0);
    // handler.endPrefixMapping(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
   */
  public void skippedEntity(String arg0) throws SAXException {
    this.unmarshallerHandler.skippedEntity(arg0);
    // handler.skippedEntity(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
   */
  public void setDocumentLocator(Locator arg0) {
    this.unmarshallerHandler.setDocumentLocator(arg0);
    // handler.setDocumentLocator(arg0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String,
   *      java.lang.String)
   */
  public void processingInstruction(String arg0, String arg1) throws SAXException {
    this.unmarshallerHandler.processingInstruction(arg0, arg1);
    // handler.processingInstruction(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String,
   *      java.lang.String)
   */
  public void startPrefixMapping(String arg0, String arg1) throws SAXException {
    this.unmarshallerHandler.startPrefixMapping(arg0, arg1);
    // handler.startPrefixMapping(arg0, arg1);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  public void endElement(String arg0, String arg1, String arg2) throws SAXException {
    this.unmarshallerHandler.endElement(arg0, arg1, arg2);
    // handler.endElement(arg0, arg1, arg2);
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
   *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
   */
  public void startElement(String arg0, String arg1, String arg2, Attributes arg3)
      throws SAXException {
    log.debug("Parameters = {0-" + arg0 + " 1-" + arg1 + " 2-" + arg2 + " 3-" + arg3 + "}");
    this.currentElement = arg1;
    try {
      this.unmarshallerHandler.startElement(arg0, arg1, arg2, arg3);
      // handler.startElement(arg0, arg1, arg2, arg3);
    } catch (Exception e) {
      log.fatal(i18n.getString("JAXBIllegal", this.currentElement), e);
    }
  }
}
