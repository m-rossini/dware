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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.jxpath.ClassFunctions;
import org.apache.commons.jxpath.FunctionLibrary;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.Pointer;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.filter.template.FieldTemplate;
import br.com.auster.dware.filter.template.ObjectTemplate;
import br.com.auster.dware.filter.template.RecurringObjectTemplate;
import br.com.auster.dware.filter.template.TemplateSerializer;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;

/**
 * Configuration template:
 * 
 * <pre>
 *         &lt;dw:config file=&quot;...&quot;/&gt;
 * </pre>
 * 
 * <p>
 * And, template configuration file will look like : (order of elements AND
 * attributes ENFORCED!!!!)
 * 
 * <pre>
 *          &lt;dw:templates&gt;
 *            (&lt;dw:template name=&quot;...&quot; (recurr-by=&quot;xpath&quot;)?&gt;
 *              (&lt;dw:field name=&quot;...&quot; (value=&quot;...&quot; | xpath=&quot;...&quot; | plugin=&quot;...&quot;)&gt;)?
 *              (&lt;dw:sub-templates&gt;
 *                (&lt;dw:template .../&gt;)+
 *               &lt;/dw:sub-templates&gt;)?
 *             &lt;dw:template&gt;)+
 *          &lt;dw:templates&gt;
 * </pre>
 * 
 * @author Frederico A Ramos
 * @version $Id: ObjectTransformerFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class ObjectTransformerFilter extends DefaultFilter implements ObjectProcessor {

  // #####################
  // Class Variables
  // #####################
  public static final String CONFIGURATION_ROOT_ATTRIBUTE = "file";

  public static final String CONFIGURATION_TEMPLATE_ELEMENT = "template";

  public static final String CONFIGURATION_TEMPLATE_NAME_ATTRIBUTE = "name";

  public static final String CONFIGURATION_TEMPLATE_POINTER_ATTRIBUTE = "pointer";

  public static final String CONFIGURATION_TEMPLATE_RECURRING_ATTRIBUTE = "recurr-by";

  public static final String CONFIGURATION_FIELD_ELEMENT = "field";

  public static final String CONFIGURATION_FIELD_NAME_ATTRIBUTE = "name";

  public static final String CONFIGURATION_FIELD_TYPE_ATTRIBUTE = "type";

  public static final String CONFIGURATION_FIELD_VALUE_CONSTANT_ATTRIBUTE = "value";

  public static final String CONFIGURATION_FIELD_VALUE_XPATH_ATTRIBUTE = "xpath";

  public static final String CONFIGURATION_FIELD_VALUE_PLUGIN_ATTRIBUTE = "plugin";

  public static final String CONFIGURATION_SUBTEMPLATE_ELEMENT = "sub-templates";

  public static final String CONFIGURATION_CUSTOM_FUNCTION_ELEMENT = "custom-function";

  public static final String CONFIGURATION_CUSTOM_FUNCTION_PREFIX_ATTRIBUTE = "prefix";

  public static final String CONFIGURATION_SUBTEMPLATE_CLASS_ATTRIBUTE = "class";

  // #####################
  // Instance Variables
  // #####################

  protected TemplateSerializer serializer;

  protected Element configuration;

  protected FunctionLibrary functionLib;

  protected static final Logger log = Logger.getLogger(ObjectTransformerFilter.class);

  final I18n i18n = I18n.getInstance(this.getClass());

  // #####################
  // Constructors
  // #####################

  public ObjectTransformerFilter(String name) {
    super(name);
  }

  // #####################
  // Instance Methods
  // #####################

  public void configure(Element _configuration) throws FilterException {
    try {

      // get configuration filename
      File file = new File(DOMUtils
          .getAttribute(_configuration, CONFIGURATION_ROOT_ATTRIBUTE, true));
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      this.configuration = dbf.newDocumentBuilder().parse(file).getDocumentElement();

      NodeList functionsXML = DOMUtils.getElements(_configuration,
                                                   CONFIGURATION_CUSTOM_FUNCTION_ELEMENT);
      this.functionLib = new FunctionLibrary();
      log.debug("+++++++++++ encontrado '" + functionsXML.getLength() + "' elementos");
      // get list of extension functions
      for (int i = 0; i < functionsXML.getLength(); i++) {
        Element customFunction = (Element) functionsXML.item(i);
        String prefix = DOMUtils.getAttribute(customFunction,
                                              CONFIGURATION_CUSTOM_FUNCTION_PREFIX_ATTRIBUTE, true);
        String klass = DOMUtils.getAttribute(customFunction,
                                             CONFIGURATION_SUBTEMPLATE_CLASS_ATTRIBUTE, true);
        log.debug("+++++++++++++ registrando class '" + klass + "' com prefixo '" + prefix + "'");
        this.functionLib.addFunctions(new ClassFunctions(Class.forName(klass), prefix));
      }
    } catch (ClassNotFoundException cnfe) {
      cnfe.printStackTrace();
      throw new FilterException(cnfe);
    } catch (ParserConfigurationException pce) {
      throw new FilterException(pce);
    } catch (IOException ioe) {
      throw new FilterException(ioe);
    } catch (SAXException saxe) {
      throw new FilterException(saxe);
    }
  }

  public Object getInput(String sourceName) {
    return this;
  }

  public void setOutput(String _filterName, Object _output) throws ConnectException,
      UnsupportedOperationException {
    serializer = (TemplateSerializer) _output;
  }

  // public void processElement(javax.xml.bind.Element _sourceObject) throws
  // FilterException {
  public void processElement(Object _sourceObject) throws FilterException {
    try {
      JXPathContext context = JXPathContext.newContext(_sourceObject);
      context.setLenient(true);
      // FunctionLibrary lib = new FunctionLibrary();
      // lib.addFunctions(new ClassFunctions(br.com.auster.common.xsl.Sequencer.class,
      // "seq"));
      context.setFunctions(this.functionLib);
      this._processElement(null, context, this.configuration);
    } catch (IOException ioe) {
      throw new FilterException(ioe);
    }
  }

  protected void _processElement(ObjectTemplate _rootTemplate,
                                 JXPathContext _context,
                                 Element _configuration) throws IOException {
    if (_configuration == null) {
      throw new IllegalStateException("builder not configured");
    }
    List resultList = new ArrayList();
    NodeList listOfTemplates = DOMUtils.getElements(_configuration, CONFIGURATION_TEMPLATE_ELEMENT);
    for (int i = 0; i < listOfTemplates.getLength(); i++) {
      Element templateElement = (Element) listOfTemplates.item(i);
      // defining the type of template and creating the ObjectTemplate class
      ObjectTemplate currentTemplate = createObjectTemplate(templateElement);
      if (currentTemplate == null) {
        continue;
      }

      if (currentTemplate instanceof RecurringObjectTemplate) {
        populateRecurringObjectTemplate(_rootTemplate, (RecurringObjectTemplate) currentTemplate,
                                        _context, templateElement);
        // REMEMBER !!! Recurring templates as split to be treated as single
        // templates, so the current template created
        // will only be used to spawn clones when populating each single
        // instance of the recurring template. Therefore,
        // there is no use to continue processing after instances where handled.
        continue;
      } else {
        // configuring the template
        populateSingleObjectTemplate(_rootTemplate, currentTemplate, _context, templateElement);
      }

      // this will make sure the parents are printed BEFORE their childs
      if (_rootTemplate == null) {
        if ((this.serializer == null) || (!this.serializer.isReady())) {
          continue;
          // TODO throw exception!!!
        }
        this.serializer.serializeTemplate(currentTemplate);
      }
    }
  }

  private void populateRecurringObjectTemplate(ObjectTemplate _root,
                                               RecurringObjectTemplate _template,
                                               JXPathContext _context,
                                               Element _xmlElement) throws IOException {

    try {
      String recurrBy = _template.getRecurringPath();
      Iterator nodeIterator = _context.iteratePointers(recurrBy);
      while (nodeIterator.hasNext()) {
        Pointer currentPointer = (Pointer) nodeIterator.next();
        JXPathContext relativeContext = _context.getRelativeContext(currentPointer);
        ObjectTemplate singleTemplate = (ObjectTemplate) _template.clone();
        // this will populate all the instances of this recurring template
        populateSingleObjectTemplate(_root, singleTemplate, relativeContext, _xmlElement);
      }
    } catch (CloneNotSupportedException cnsp) {
      // TODO throw exception
      cnsp.printStackTrace();
    }
  }

  // handles a single ObjectTemplate
  private void populateSingleObjectTemplate(ObjectTemplate _root,
                                            ObjectTemplate _template,
                                            JXPathContext _context,
                                            Element _xmlElement) throws IOException {

    String pointerXPath = DOMUtils.getAttribute(_xmlElement,
                                                CONFIGURATION_TEMPLATE_POINTER_ATTRIBUTE, false);
    JXPathContext relativeContext = null;
    if ((pointerXPath != null) && (pointerXPath.trim().length() > 0)) {
      Pointer pointer = _context.getPointer(pointerXPath);
      relativeContext = _context.getRelativeContext(pointer);
    } else {
      relativeContext = _context;
    }
    log.debug("running @path =' " + relativeContext.getContextPointer().asPath() + "'");
    // configuring its fields
    createTemplateFields(relativeContext, _template, DOMUtils
        .getElements(_xmlElement, CONFIGURATION_FIELD_ELEMENT));
    // recursively gets sub-templates
    Element subTemplates = DOMUtils.getElement(_xmlElement, CONFIGURATION_SUBTEMPLATE_ELEMENT,
                                               false);
    if (subTemplates != null) {
      _processElement(_template, relativeContext, subTemplates);
    }
    // only save if there is a ROOT template
    if (_root != null) {
      _root.getSubTemplates().add(_root.getSubTemplates().size(), _template);
    }
  }

  private ObjectTemplate createObjectTemplate(Element _templateXML) {
    String name = DOMUtils.getAttribute(_templateXML, CONFIGURATION_TEMPLATE_NAME_ATTRIBUTE, true);
    String recPath = DOMUtils.getAttribute(_templateXML,
                                           CONFIGURATION_TEMPLATE_RECURRING_ATTRIBUTE, false);
    if (recPath.trim().length() > 0) {
      return (new RecurringObjectTemplate(name, recPath));
    } else {
      return (new ObjectTemplate(name));
    }
  }

  private void createTemplateFields(JXPathContext _context,
                                    ObjectTemplate _template,
                                    NodeList _fieldList) {
    for (int i = 0; i < _fieldList.getLength(); i++) {
      Element fieldElement = (Element) _fieldList.item(i);
      FieldTemplate currentField = new FieldTemplate(DOMUtils
          .getAttribute(fieldElement, CONFIGURATION_FIELD_NAME_ATTRIBUTE, true));
      if (DOMUtils.getAttribute(fieldElement, CONFIGURATION_FIELD_VALUE_CONSTANT_ATTRIBUTE, false)
          .trim().length() > 0) {
        // if field is defined as CONSTANT
        currentField.setValue(DOMUtils.getAttribute(fieldElement,
                                                    CONFIGURATION_FIELD_VALUE_CONSTANT_ATTRIBUTE,
                                                    false));
        // currentField.setType(FieldTemplate.TEMPLATEFIELD_CONSTANT_TYPE);
      } else if (DOMUtils.getAttribute(fieldElement, CONFIGURATION_FIELD_VALUE_XPATH_ATTRIBUTE,
                                       false).trim().length() > 0) {
        // if field is defined as XPATH
        String xpath = DOMUtils.getAttribute(fieldElement,
                                             CONFIGURATION_FIELD_VALUE_XPATH_ATTRIBUTE, true);
        currentField.setValue(_context.getValue(xpath));
      } else if (DOMUtils.getAttribute(fieldElement, CONFIGURATION_FIELD_VALUE_PLUGIN_ATTRIBUTE,
                                       false).trim().length() > 0) {
        // if field is defined as PLUGIN
        // TODO need to implement this functionality
        throw new UnsupportedOperationException();
      } else {
        throw new UnsupportedOperationException();
      }
      // adding field to template´s list
      _template.getFields().add(_template.getFields().size(), currentField);
    }
  }

}
