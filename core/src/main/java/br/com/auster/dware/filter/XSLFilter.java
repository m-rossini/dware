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
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;

import br.com.auster.common.cli.OptionsParser;
import br.com.auster.common.io.IOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * Receives the data from a ContentHandler, process it using a sequence of XSL
 * files, and writes the output to another ContentHandler.
 * 
 * <p>
 * To get the input:
 * <pre>
 *    ContentHandler getInput(null)
 * </pre>
 * 
 * <p>
 * To set the output:
 * <pre>
 *    setOutput(ContentHandler)
 *    setOutput(WritableByteChannel)
 *    setOutput(OutputStream)
 *    setOutput(Writer)
 *    setOutput(File)
 *    setOutput(Node)
 * </pre>
 * 
 * @version $Id: XSLFilter.java 283 2007-03-08 23:44:43Z rbarone $
 */
public final class XSLFilter extends DefaultFilter {

  /**
   * {@value}
   */
  public static final String XSL_FILE_ELEMENT = "xsl-file";

  /**
   * {@value}
   */
  public static final String PATH_ATTR = "path";

  /**
   * {@value}
   */
  public static final String ENCRYPTED_ATTR = "encrypted";

  /**
   * {@value}
   */
  public static final String INCREMENTAL_ATTR = "incremental";
  
  /**
   * {@value}
   */
  public static final String PARAM_ELEMENT = "param";

  /**
   * {@value}
   */
  public static final String PARAMETERS_ELEMENT = "params";  

  /**
   * {@value}
   */
  public static final String NAME_ATTR = "name";

  /**
   * {@value}
   */
  public static final String VALUE_ATTR = "value";
  
  /**
   * {@value}
   */
  public static final String REQUEST_NAMESPACE = "xalan://" + Request.class.getName();
  
  /**
   * {@value}
   */
  public static final String REQUEST_QUALIFIED_PARAM = "{" + REQUEST_NAMESPACE + "}request";

  protected static final Map templates = new HashMap();

  protected TransformerHandler firstTHandler = null, lastTHandler = null;

  protected final SAXTransformerFactory saxTFactory = 
    (SAXTransformerFactory) TransformerFactory.newInstance();

  protected Templates[] templateArray;

  protected static final Logger log = Logger.getLogger(XSLFilter.class);

  private final I18n i18n = I18n.getInstance(XSLFilter.class);

  private Map parmListMap;

  public XSLFilter(String name) {
    super(name);
  } 

  /**
   * Configures this filter.
   */
  public final synchronized void configure(Element config) throws FilterException {   
    // Checks if it's wanted to use incremental feature
    if (DOMUtils.getBooleanAttribute(config, INCREMENTAL_ATTR)) {
      log.info(i18n.getString("usingIncremental"));
      saxTFactory.setAttribute("http://xml.apache.org/xalan/features/incremental", Boolean.TRUE);
    }

    //Handle Filter Level Parameters.
    Element parms = DOMUtils.getElement(config, PARAMETERS_ELEMENT, false);
    if (parms != null) {
       NodeList parmListNodes = DOMUtils.getElements(parms, PARAM_ELEMENT);
       this.parmListMap = new HashMap();
       int qtd = parmListNodes.getLength();
       for (int i=0; i < qtd; i++) {
          Element parm = (Element) parmListNodes.item(i);
          String key = DOMUtils.getAttribute(parm, NAME_ATTR, true);
          String value = DOMUtils.getAttribute(parm, VALUE_ATTR, true);
          this.parmListMap.put(key, value);
       }
    }
    
    // Creates the XSL templates
    final NodeList xslList = DOMUtils.getElements(config, XSL_FILE_ELEMENT);
    if (xslList.getLength() == 0)
      throw new FilterException(i18n.getString("noXSLFiles", filterName));

    try {
      this.templateArray = new Templates[xslList.getLength()];
      for (int i = 0; i < this.templateArray.length; i++) {
        final Element xslElement = (Element) xslList.item(i);
        final String xslFile = DOMUtils.getAttribute(xslElement, PATH_ATTR, true);
        final boolean isEncrypted = 
          DOMUtils.getBooleanAttribute(xslElement, ENCRYPTED_ATTR, true);
        this.templateArray[i] = this.getTemplate(xslFile, isEncrypted, saxTFactory);
      }
    } catch (TransformerConfigurationException e) {
      throw new FilterException(e.getMessageAndLocation(), e);
    }
  }

  /**
   * Gets a XSL template based on the XSL path given.
   * 
   * @param xslPath
   *          the path to a XSL file that will be used to generate the template.
   * @param factory
   *          the factory used to create templates from XSL files.
   * @return a template based on the xsl path given.
   */
  private final Templates getTemplate(String xslPath,
                                      boolean isEncrypted,
                                      TransformerFactory factory)
      throws TransformerConfigurationException {
    // Instantiates the template based on the XSL file
    synchronized (templates) {
      Templates template = (Templates) templates.get(xslPath);
      if (template == null) {
        log.debug("Creating a XSL template from the file: " + xslPath);
        InputStream xslInputStream;
        try {
          xslInputStream = IOUtils.openFileForRead(xslPath, isEncrypted);
        } catch (Exception ie) {
          throw new RuntimeException(ie);
        }
        template = factory.newTemplates(new StreamSource(xslInputStream, xslPath));
        templates.put(xslPath, template);
      }
      return template;
    }
  }

  public synchronized final void prepare(Request request) throws FilterException {
    // Creates the TransformerHandler pipe.
    try {
      this.lastTHandler = null;

      // Links the last transformer to the new one
      for (int i = 0; i < this.templateArray.length; i++) {
        TransformerHandler tHandler = saxTFactory.newTransformerHandler(templateArray[i]);
        if (this.lastTHandler == null)
          this.firstTHandler = tHandler;
        else
          this.lastTHandler.setResult(new SAXResult(tHandler));

        this.lastTHandler = tHandler;
        this.setXSLParameters(tHandler, request);
      }
    } catch (TransformerException e) {
      throw new FilterException(e.getMessageAndLocation(), e);
    }
  }

  /*****************************************************************************
   * Sets all the command line parameters in the XSL used for the
   * transformation. The XSL will be able to read these parameters directly,
   * with no need to create and call specific methods to fetch these data.
   * 
   * This method will create also, parameters defined on config element of the filter definition.
   * If there is a name conflict between the cmd line args and the config args, the config will take precedence
   * 
   * @param tHandler
   *          transformer handler used in the XSL Filter
   */
  private final void setXSLParameters(TransformerHandler tHandler, Request request) {
    String[] params = OptionsParser.getOptions();

    // set request parameter
    tHandler.getTransformer().setParameter(REQUEST_QUALIFIED_PARAM, request);
    
    for (int i = 0; i < params.length; i++) {
      try {
        tHandler.getTransformer().setParameter(params[i], OptionsParser.getOptionValue(params[i]));
      } catch (IllegalArgumentException e) {
      }
    }
    
    if (this.parmListMap != null) {
       for (Iterator itr=this.parmListMap.keySet().iterator();itr.hasNext();) {
          String key = (String) itr.next();
          String value = (String)this.parmListMap.get(key);
          tHandler.getTransformer().setParameter(key, value);
       }
    }
  }

  /**
   * Cleans this filter.
   */
  public final void commit() {
    this.firstTHandler = null;
    this.lastTHandler = null;
  }

  /**
   * Just calls commit.
   */
  public final void rollback() {
    this.commit();
  }

  /**
   * Returns the input for this filter. It will be a ContentHandler.
   */
  public final Object getInput(String sourceName) throws ConnectException,
      UnsupportedOperationException {
    return this.firstTHandler;
  }

  /**
   * Sets the output for this filter.
   */
  public final void setOutput(String sinkName, Object output) throws ConnectException,
      UnsupportedOperationException {
    if (output instanceof ContentHandler) {
      this.lastTHandler.setResult(new SAXResult((ContentHandler) output));
    } else if (output instanceof WritableByteChannel) {
      this.lastTHandler.setResult(new StreamResult(Channels
          .newOutputStream((WritableByteChannel) output)));
    } else if (output instanceof OutputStream) {
      this.lastTHandler.setResult(new StreamResult((OutputStream) output));
    } else if (output instanceof Writer) {
      this.lastTHandler.setResult(new StreamResult((Writer) output));
    } else if (output instanceof File) {
      this.lastTHandler.setResult(new StreamResult((File) output));
    } else if (output instanceof Node) {
      this.lastTHandler.setResult(new DOMResult((Node) output));
    } else {
      throw new ConnectException(i18n.getString("unsupportedOutputType", output.getClass(),
                                                XSLFilter.class.getName()));
    }
  }
}
