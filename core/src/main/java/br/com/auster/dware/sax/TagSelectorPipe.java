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
 * Created on Apr 11, 2005
 */
package br.com.auster.dware.sax;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import br.com.auster.common.xml.DOMUtils;

/**
 * TODO comments
 * 
 * @author Ricardo Barone
 * @version $Id: TagSelectorPipe.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class TagSelectorPipe extends ContentHandlerPipe {

  //##################################################
  // Class variables
  //##################################################
  protected static final String TAG_ELT = "tag";
  protected static final String NAME_ATT = "name";
  protected static final String ACTION_ATT = "action";
  
  protected static final int DEFAULT_ACTION = 0;
  protected static final int INCLUDE_ACTION = 1;
  protected static final int EXCLUDE_ACTION = 2;
  
  // Map<String(action value), Integer(action index)>
  protected static final Map actionValues;
  static {
    actionValues = new HashMap();
    actionValues.put("default", new Integer(DEFAULT_ACTION));
    actionValues.put("include", new Integer(INCLUDE_ACTION));
    actionValues.put("exclude", new Integer(EXCLUDE_ACTION));
  }
  
  private static final Logger log = Logger.getLogger(TagSelectorPipe.class);
  
  
  // ##################################################
  // Instance variables
  //##################################################

  // Output filter that will receive the resulting SAX events
  protected ContentHandler output;

  // Controles e Contadores
  protected int currentElementLevel = -1;

//  protected String currentPath = "";

  // 'true' = ignore; 'false' = include
  private LinkedList actionStack = new LinkedList();
  private boolean ignoreTag = false;
  
  private TagDefinition tagDefinition;
  private TagDefinition context = null;

  //##################################################
  // Constructors
  //##################################################

  public TagSelectorPipe(Element config) {
    this.tagDefinition = configureTag("0-root", DEFAULT_ACTION, config);
  }
  
  public TagDefinition configureTag(Element node) {
    String name = DOMUtils.getAttribute(node, NAME_ATT, true);
    
    // Configure action value
    String actionValue = DOMUtils.getAttribute(node, ACTION_ATT, false);
    int action = DEFAULT_ACTION;
    if (actionValue != null && actionValue.length() > 0) {
      Integer constant = (Integer) actionValues.get(actionValue);
      if (constant == null) {
        throw new IllegalArgumentException("Invalid action value: " + actionValue);
      }
      action = constant.intValue();
    }

    return configureTag(name, action, node);
  }
  
  public TagDefinition configureTag(String name, int action, Element node) {
    TagDefinition tag = new TagDefinition(name, action);
    NodeList tagList = DOMUtils.getElements(node, TAG_ELT);
    for (int i = 0; i < tagList.getLength(); i++) {
      Element elt = (Element) tagList.item(i);
      tag.addChild(configureTag(elt));
    }
    return tag;
  }
  

  //##################################################
  // ContentHandlerPipe Methods
  //##################################################

  public void setOutput(ContentHandler output) {
    this.output = output;
  }

  //##################################################
  // SAX event handling
  //##################################################

  public void startDocument() throws SAXException {
    this.context = this.tagDefinition;
    this.output.startDocument();
  }

  public void endDocument() throws SAXException {
    this.output.endDocument();
    this.context = null;
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
    this.currentElementLevel++;
//    this.currentPath += "/" + localName;
//    log.debug("SE=>" + this.currentPath);

    this.actionStack.add(new Boolean(this.ignoreTag));
    this.ignoreTag = this.context.isChildIgnored(localName);
    if (!this.ignoreTag) {
      this.output.startElement(namespaceURI, localName, qName, atts);
//      log.info("INCLUDED START ---> " + currentPath);
    } else {
//      log.info("IGNORED START ---> " + currentPath);
    }
    
    if (this.context.containsChild(localName)) {
      this.context = this.context.getChild(localName);
      this.currentElementLevel = this.context.getLevel(); 
//      if (!this.context.isBound()) {
//        this.context.bind(this.currentElementLevel);
//      }
    }
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    if (localName.equals(this.context.getName()) && this.currentElementLevel == this.context.getLevel()) {
      this.context = this.context.getParent();
    }

    if (!this.ignoreTag) {
      this.output.endElement(namespaceURI, localName, qName);
//      log.info("INCLUDED END ---> " + currentPath);
    } else {
//      log.info("IGNORED END ---> " + currentPath);
    }
    Boolean lastTagAction = (Boolean) this.actionStack.removeLast();
    this.ignoreTag = lastTagAction.booleanValue();
    
    this.currentElementLevel--;
//    this.currentPath = this.currentPath.substring(0, this.currentPath.lastIndexOf("/"));
//    log.debug("EE=>" + this.currentPath);
  }

  public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
    if (!this.ignoreTag) {
      this.output.characters(arg0, arg1, arg2);
    }
  }

  public void endPrefixMapping(String arg0) throws SAXException {
    if (!this.ignoreTag) {
      this.output.endPrefixMapping(arg0);
    }
  }

  public void startPrefixMapping(String arg0, String arg1) throws SAXException {
    if (!this.ignoreTag) {
      this.output.startPrefixMapping(arg0, arg1);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (!this.ignoreTag) {
      this.output.ignorableWhitespace(ch, start, length);
    }
  }
  
  /**
   * TODO class comments 
   *
   * @author Ricardo Barone
   * @version $Id: TagSelectorPipe.java 87 2005-08-04 21:21:25Z rbarone $
   */
  private class TagDefinition {
    
    // see *_ACTION fields in TagSelectorPipe class
    private int action = DEFAULT_ACTION;
    private int childAction = DEFAULT_ACTION;
    
    // Tag name
    private String name;
    
    // Hierarchy Level - relative to the parent (roots will be 0)
    private int level = -1;
    
    // Root tag has no parents (null)
    private WeakReference parent = null;
    
    // Map<String(name), TagDefinition>
    private Map childs = new HashMap();
    
    protected TagDefinition(String name) {
      if (name == null || name.length() == 0) {
        throw new IllegalArgumentException("Attribute 'name' is mandatory and must be a non-empty String.");
      }
      this.name = name;
    }
    
    protected TagDefinition(String name, int action) {
      this(name);
      this.action = action;
    }
    
    protected void addChild(TagDefinition child) {
      if (getAction() == EXCLUDE_ACTION) {
        throw new IllegalArgumentException("No tags are allowed inside an excluded tag: " + getName());
      }
      if (this.childAction != DEFAULT_ACTION) {
        if (child.getAction() != DEFAULT_ACTION && child.getAction() != this.childAction) {
          throw new IllegalArgumentException("Invalid action value for current context in tag: " + child.getName());
        }
      } else {
        this.childAction = child.getAction();
      }
      if (this.containsChild(child.getName())) {
        throw new IllegalArgumentException("Duplicate tag definition: " + child.getName());
      }
      child.parent = new WeakReference(this);
      child.level = this.level + 1;
      this.childs.put(child.getName(), child);
    }
    
    protected int getAction() {
      return this.action;
    }
    
    protected int getChildAction() {
      return this.childAction;
    }
    
    protected String getName() {
      return this.name;
    }
    
    protected Collection getChilds() {
      return this.childs.values();
    }
    
    protected boolean containsChild(String name) {
      return this.childs.containsKey(name);
    }
    
    protected TagDefinition getChild(String name) {
      return (TagDefinition) this.childs.get(name);
    }
    
    protected boolean isChildIgnored(String name) {
      boolean exclude = false;
      if (getAction() == EXCLUDE_ACTION) {
//        log.debug("Excluding '" + name  + "' because tag action is EXCLUDE.");
        exclude = true;
      } else if (!containsChild(name)) {
        if (getChildAction() == INCLUDE_ACTION) {
          exclude = true;
//          log.debug("Excluding '" + name + "' because it's not my child and childtag action is INCLUDE.");
        }
      } else if (getChild(name).getAction() == EXCLUDE_ACTION) {
        exclude = true;
//        log.debug("Excluding '" + name + "' because child action is EXCLUDE.");
      }
      return exclude;
    }
    
    protected TagDefinition getParent() {
      return (TagDefinition) this.parent.get();
    }
    
    protected boolean isRoot() {
      return (this.parent == null);
    }
    
    protected int getLevel() {
      return this.level;
    }
    
//    protected boolean isBound() {
//      return (this.level >= 0);
//    }
//    
//    protected void bind(int level) {
//      if (this.level == level) {
//        return;
//      }
//      this.level = level;
//      bindChilds();
//    }
//    
//    private void bindChilds() {
//      for (Iterator it = getChilds().iterator(); it.hasNext(); ) {
//        TagDefinition child = (TagDefinition) it.next();
//        child.level = this.level + 1;
//        child.bindChilds();
//      }
//    }
    
  }
  
  
}
