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
 * Created on Apr 28, 2005
 */
package br.com.auster.dware.sax;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections.ArrayStack;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import br.com.auster.common.xml.DOMUtils;


/**
 * TODO class comments 
 *
 * @author Ricardo Barone
 * @version $Id: TagDropperPipe.java 189 2006-05-25 20:43:38Z framos $
 */
public class TagDropperPipe extends ContentHandlerPipe {

  protected static final String TAG_ELT = "tag";
  protected static final String NAME_ATT = "name";
  
  protected final HashSet tags = new HashSet();
  
  private static final Logger log = Logger.getLogger(TagDropperPipe.class);

  // Output filter that will receive the resulting SAX events
  protected ContentHandler output;
  
  private boolean outputCurrentTag = true;
  private String lastDropElement;
  
  /**
   * 
   */
  public TagDropperPipe(Element config) {
    NodeList tagList = DOMUtils.getElements(config, TAG_ELT);
    for (int i = 0; i < tagList.getLength(); i++) {
      Element tag = (Element) tagList.item(i);
      String name = DOMUtils.getAttribute(tag, NAME_ATT, true);
      this.tags.add(name);
    }
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
    this.output.startDocument();
  }

  public void endDocument() throws SAXException {
    this.output.endDocument();
  }

  public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
      throws SAXException {
	if (this.outputCurrentTag) {
	    if (this.tags.contains(localName)) {
	    	this.outputCurrentTag = false;
	    	// stacking the tag name
	    	this.lastDropElement= localName;
	    } else {
	    	this.output.startElement(namespaceURI, localName, qName, atts);
	    }
	}
  }

  public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
    if (this.outputCurrentTag) {
    	this.output.endElement(namespaceURI, localName, qName);
    } else {
    	// stacking the tag name
    	if (localName.equals(this.lastDropElement)) {
    		this.outputCurrentTag = true;
    		this.lastDropElement = null;
    	}
    }
  }

  public void characters(char[] arg0, int arg1, int arg2) throws SAXException {
    if (this.outputCurrentTag) {
      this.output.characters(arg0, arg1, arg2);
    }
  }

  public void endPrefixMapping(String arg0) throws SAXException {
    if (this.outputCurrentTag) {
      this.output.endPrefixMapping(arg0);
    }
  }

  public void startPrefixMapping(String arg0, String arg1) throws SAXException {
    if (this.outputCurrentTag) {
      this.output.startPrefixMapping(arg0, arg1);
    }
  }

  public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
    if (this.outputCurrentTag) {
      this.output.ignorableWhitespace(ch, start, length);
    }
  }
  
}
