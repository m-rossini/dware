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
 * Created on Jul 4, 2005
 */
package br.com.auster.dware.filter;

import java.io.File;
import java.io.IOException;
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
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.joost.trax.TransformerFactoryImpl;

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
 * Filter capable of processing STX stylesheets.
 * 
 * Configuration:
 * <pre>
 * <!-- mandatory elements -->
 * <stx-file path="path to STX file" encrypted="false|true">
 *   <!-- optional elements inside 'stx-file': -->
 *   <uri-resolver class-name="class used for URI resolutions in 'stx:process-document'">
 *      <!-- 
 *       ...additional config elements here...
 *       the class will receive the entire 'uri-resolver' element
 *      -->
 *   </uri-resolver>
 * </stx-file>
 * <!-- optional elements -->
 * <uri-resolver class-name="class used for URI resolutions in 'stx:import'">
 *    <!-- 
 *       ...additional config elements here...
 *       the class will receive the entire 'uri-resolver' element
 *    -->
 * </uri-resolver>
 * <params>
 *   <param name="my-paramenter 1" value="my-value 1"/>
 *   <param name="my-paramenter 2" value="my-value 2"/>
 *   ...
 *   <param name="my-paramenter n" value="my-value n"/>
 * </params>
 * </pre>
 * 
 * <p><b>Title:</b> STXFilter</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id: STXFilter.java 283 2007-03-08 23:44:43Z rbarone $
 */
public class STXFilter extends DefaultFilter {

  /**
   * {@value}
   */
  public static final String URI_RESOLVER_ELEMENT = "uri-resolver";
  
  /**
   * {@value}
   */
  public static final String CLASS_NAME_ATT = "class-name";
  
  /**
   * {@value}
   */
  public static final String STX_FILE_ELEMENT = "stx-file";

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

  protected static final Logger log = Logger.getLogger(STXFilter.class);
  
  protected static final Map templates = new HashMap();
  

  protected TransformerHandler firstTHandler = null, lastTHandler = null;

  protected final SAXTransformerFactory saxTFactory;

  protected Templates[] templateArray;
  
  protected URIResolver[] uriResolverArray;

  private final I18n i18n = I18n.getInstance(XSLFilter.class);

  private Map parmListMap;
  

  public STXFilter(String name) {
    super(name);
    /*
    System.setProperty("javax.xml.transform.TransformerFactory",
                       "net.sf.joost.trax.TransformerFactoryImpl");
    */
    try {
      this.saxTFactory = new TransformerFactoryImpl();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    /*
    saxTFactory.setAttribute(net.sf.joost.trax.TrAXConstants.KEY_XSLT_FACTORY,
                             "net.sf.saxon.TransformerFactoryImpl");
    */
  } 

  /**
   * Configures this filter.
   */
  public final synchronized void configure(Element config) throws FilterException {
    // Configure optional URIResolver for STX 'import' instruction
    try {
      URIResolver resolver = STXFilter.getURIResolverInstance(config);
      if (resolver != null) {
        this.saxTFactory.setURIResolver(resolver);
      }
    } catch (Exception e) {
      throw new FilterException(e);
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
    
    // Creates the STX templates
    final NodeList stxList = DOMUtils.getElements(config, STX_FILE_ELEMENT);
    if (stxList.getLength() == 0) {
      throw new FilterException(i18n.getString("noXSLFiles", filterName));
    }

    try {
      this.templateArray = new Templates[stxList.getLength()];
      this.uriResolverArray = new URIResolver[this.templateArray.length];
      for (int i = 0; i < this.templateArray.length; i++) {
        final Element stxElement = (Element) stxList.item(i);
        final String stxFile = DOMUtils.getAttribute(stxElement, PATH_ATTR, true);
        final boolean isEncrypted = 
          DOMUtils.getBooleanAttribute(stxElement, ENCRYPTED_ATTR, true);
        try {
          this.uriResolverArray[i] = STXFilter.getURIResolverInstance(stxElement);
        } catch (Exception e) {
          throw new FilterException(e);
        }
        this.templateArray[i] = this.getTemplate(stxFile, isEncrypted, saxTFactory);
      }
    } catch (TransformerConfigurationException e) {
      throw new FilterException(e.getMessageAndLocation(), e);
    }
  }

  /**
   * Gets a STX template based on the STX path given.
   * 
   * @param stxPath
   *          the path to a STX file that will be used to generate the template.
   * @param factory
   *          the factory used to create templates from STX files.
   * @return a template based on the stx path given.
   */
  private final Templates getTemplate(String stxPath,
                                      boolean isEncrypted,
                                      TransformerFactory factory)
      throws TransformerConfigurationException {
    // Instantiates the template based on the STX file
    synchronized (templates) {
      Templates template = (Templates) templates.get(stxPath);
      if (template == null) {
        log.debug("Creating a STX template from the file: " + stxPath);
        InputStream stxInputStream;
        try {
          stxInputStream = IOUtils.openFileForRead(stxPath, isEncrypted);
        } catch (Exception ie) {
          throw new RuntimeException(ie);
        }
        template = factory.newTemplates(new StreamSource(stxInputStream, stxPath));
        templates.put(stxPath, template);
      }
      return template;
    }
  }

  private static final URIResolver getURIResolverInstance(Element config)
      throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
      IllegalAccessException, java.lang.reflect.InvocationTargetException {
    URIResolver resolver = null;
    final Element resolverElement = DOMUtils.getElement(config, URI_RESOLVER_ELEMENT, false);
    if (resolverElement != null) {
      final String className = DOMUtils.getAttribute(resolverElement, CLASS_NAME_ATT, true);
      Class[] c = { Element.class };
      Object[] o = { config };
      resolver = (URIResolver) Class.forName(className).getConstructor(c).newInstance(o);
    }
    return resolver;
  }
  
  public synchronized final void prepare(Request request) throws FilterException {
    // Creates the TransformerHandler pipe.
    try {
      this.firstTHandler = null;
      this.lastTHandler = null;

      // Links the last transformer to the new one
      for (int i = 0; i < this.templateArray.length; i++) {
        TransformerHandler tHandler = saxTFactory.newTransformerHandler(templateArray[i]);
        if (this.lastTHandler == null) {
          this.firstTHandler = tHandler;
        } else {
          this.lastTHandler.setResult(new SAXResult(tHandler));
        }
        this.lastTHandler = tHandler;
        this.setSTXParameters(tHandler);
        if (this.uriResolverArray[i] != null) {
          tHandler.getTransformer().setURIResolver(this.uriResolverArray[i]);
        }
      }
    } catch (TransformerException e) {
      throw new FilterException(e.getMessageAndLocation(), e);
    }
  }

  /*****************************************************************************
   * Sets all the command line parameters in the STX used for the
   * transformation. The STX will be able to read these parameters directly,
   * with no need to create and call specific methods to fetch these data.
   * 
   * This method will create also, parameters defined on config element of the filter definition.
   * If there is a name conflict between the cmd line args and the config args, the config will take precedence
   * 
   * @param tHandler
   *          transformer handler used in the STX Filter
   */
  private final void setSTXParameters(TransformerHandler tHandler) {
    String[] params = OptionsParser.getOptions();

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
                                                STXFilter.class.getName()));
    }
  }

}
