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
package br.com.auster.dware.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.common.xml.sax.ContentHandlerManager;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * This filter is used to copy SAX events from several inputs to lot
 * of outputs that are configured in the graph.
 *
 * <p>
 * To get the input: <code>ContentHandler getInput(null)</code>
 * <p>
 * To set the output: <code>setOutput(ContentHandler)</code>
 * 
 * @version $Id: XMLManagerFilter.java 144 2006-02-16 17:43:20Z rbarone $
 */
public class XMLManagerFilter extends DefaultFilter {
  
  private class EmptyContentHandler extends DefaultHandler {
    
    private ContentHandler output;
    
    private EmptyContentHandler(ContentHandler output) {
      this.output = output;
    }

    /**
     * @inheritDoc
     */
    public void startDocument() throws SAXException {
      this.output.startDocument();
    }
    
    /**
     * @inheritDoc
     */
    public void endDocument() throws SAXException {
      this.output.endDocument();
    }
    
  }
  
    protected static final String FIRST_TAG_ATTR  = "initial-tag";
    protected static final String KEEP_ORDER_ATTR = "keep-order";
    protected static final String FAST_MODE_ATTR  = "cached-mode";
    protected static final String OUTPUT_ELEMENT = "output";
    protected static final String NAME_ATTR = "name";
    protected static final String FORMAT_ATTR = "format";
    
    // Map<String(sink),String(format)> of declared outputs in each OUTPUT_ELEMENT
    private Map declaredOutputs = new HashMap();
    
    // list of desired output according to: 
    // List<String> request.getAttributes.get("format")
    private Set desiredOutputs = new HashSet();
    
    protected static final Logger log = Logger.getLogger(XMLManagerFilter.class);
    protected ContentHandlerManager manager;
 
    public XMLManagerFilter(String name) {
        super(name);        
    }

    /**
     * Configures this filter.
     */
    public void configure(Element config) 
        throws FilterException
    {
        String firstTag = config.getAttribute(FIRST_TAG_ATTR);
        boolean keepOrder = DOMUtils.getBooleanAttribute(config, KEEP_ORDER_ATTR);
        boolean fastMode = DOMUtils.getBooleanAttribute(config, FAST_MODE_ATTR);

        this.manager = new ContentHandlerManager(firstTag, keepOrder, fastMode);    
        
        NodeList outputList = DOMUtils.getElements(config, OUTPUT_ELEMENT);
        for (int i = 0; i < outputList.getLength(); i++) {
            Element output = (Element) outputList.item(i);
            String name = DOMUtils.getAttribute(output, NAME_ATTR, true);
            String format = DOMUtils.getAttribute(output, FORMAT_ATTR, true);
            this.declaredOutputs.put(name, format);
        }
    }

    /** 
     * Clear the content handler output list.
     */
    public void prepare(Request request) {
        this.manager.reset();
        this.desiredOutputs.clear();
        
        if (request.getAttributes().containsKey(FORMAT_ATTR)) {
          List fmts = (List) request.getAttributes().get(FORMAT_ATTR);
          this.desiredOutputs.addAll(fmts);
        }
    }

    public void rollback() {
        this.manager.releaseLocks();        
    }

    /** 
     * Gets the input for this filter
     */
    public Object getInput(String sourceName) {    	
        return this.manager.getContentHandler();
    }
    
    /**
     * Sets the content handler for output of this filter.
     */
  public void setOutput(String sinkName, Object output) {
    Object declaredOutput = this.declaredOutputs.get(sinkName);
    boolean isDesired = declaredOutput == null || this.desiredOutputs.isEmpty()
                        || this.desiredOutputs.contains(declaredOutput);
    
    ContentHandler ch = (ContentHandler) output;
    if (isDesired) {
      this.manager.addOutput(sinkName, ch);
    } else {
      this.manager.addOutput(sinkName, new EmptyContentHandler(ch));
    }
  }
}
