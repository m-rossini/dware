/*
 * Copyright (c) 2004-2006 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on 17/02/2006
 */

package br.com.auster.dware.request.file;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

/**
 * Configuration:
 * <pre>
 * <config>
 *   <partial-file-list-config default-compare-method="identity|equals|string"/>
 *     <use-request-attribute name="att 1" compare-method="identity|equals|string"/>
 *     <use-request-attribute name="att 2" compare-method="identity|equals|string"/>
 *     ...
 *     <use-request-attribute name="att n" compare-method="identity|equals|string"/>
 *   </partial-file-list-config>
 * </config>
 * </pre>
 * 
 * <p><b>Title:</b> PartialFileListRequestConsolidator</p>
 * <p><b>Description:</b> A builder that consolidades PartialFileRequests 
 * into PartialFileListRequests according to userkey</p>
 * <p><b>Copyright:</b> Copyright (c) 2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author etirelli
 * @version $Id: PartialFileListRequestConsolidator.java 153 2006-02-20 19:59:26Z etirelli $
 */

public class PartialFileListRequestConsolidator implements RequestBuilder {
  
  /**
   * {@value}
   */
  public static final String CONFIG_ROOT_ELEMENT = "partial-file-list-config";
  
  /**
   * {@value}
   */
  public static final String DEFAULT_COMPARE_ATTR = "default-compare-method";
  
  /**
   * {@value}
   */
  public static final String USE_ATTRIBUTE_ELEMENT = "use-request-attribute";
  
  /**
   * {@value}
   */
  public static final String NAME_ATT = "name";
  
  /**
   * {@value}
   */
  public static final String COMPARE_METHOD_ATT = "compare-method";
  
  /**
   * {@value}
   */
  public static final String IDENTITY_METHOD_NAME = "identity";
  private static final int IDENTITY_METHOD = 1;
  
  /**
   * {@value}
   */
  public static final String EQUALS_METHOD_NAME = "equals";
  private static final int EQUALS_METHOD = 2;
  
  /**
   * {@value}
   */
  public static final String STRING_METHOD_NAME = "string";
  private static final int STRING_METHOD = 3;
  
  
  final private String name;
  
  final private KeyWrapper key;
  
  public PartialFileListRequestConsolidator(final String name, final Element configElem) {
    this.name = name;

    if(configElem != null) {
      Element config = configElem;
      if(!CONFIG_ROOT_ELEMENT.equals(config.getLocalName())) {
        config = DOMUtils.getElement(config, CONFIG_ROOT_ELEMENT, true);
      }

      String defaultMethodName = DOMUtils.getAttribute(config, COMPARE_METHOD_ATT, false);
      if(defaultMethodName.length() == 0) {
        defaultMethodName = EQUALS_METHOD_NAME;
      }
      final int defaultMethod = this.getCompareMethod(defaultMethodName);
      
      final NodeList atts = DOMUtils.getElements(config, USE_ATTRIBUTE_ELEMENT);
      final int attrCount = atts.getLength();
      Constraint[] constraints = new Constraint[attrCount];
      for (int i = 0; i < attrCount; i++) {
        final Element att = (Element) atts.item(i); 
        final String attName = DOMUtils.getAttribute(att, NAME_ATT, true);
        final String attMethodName = DOMUtils.getAttribute(att, COMPARE_METHOD_ATT, false);

        final int attMethod; 
        if(attMethodName.length() > 0) {
          attMethod = getCompareMethod(attMethodName);
        } else {
          attMethod = defaultMethod;
        }
        constraints[i] = new Constraint(attName, attMethod);
      }
      this.key = new KeyWrapper(constraints);
    } else{
      this.key = new KeyWrapper();
    }
  }

  /**
   * @inheritDoc
   */
  public String getName() {
    return this.name;
  }

  /**
   * @inheritDoc
   */
  public RequestFilter createRequests(Map args) {
    throw new UnsupportedOperationException("Operation not supported by PartialFileListRequestConsolidator");
  }

  /**
   * @inheritDoc
   */
  public RequestFilter createRequests(RequestFilter filter, Map args) {
    Map requests = new HashMap();
    
    for(Iterator i = filter.getRemainingRequests().iterator(); i.hasNext(); ) {
      Request request = (Request) i.next();
      this.key.setRequest(request);
      List requestList = (List) requests.get(this.key);
      if(requestList == null) {
        requestList = new ArrayList();
        requests.put(this.key.clone(), requestList);
      }
      requestList.add(request);
    }
    
    PartialFileRequest[] arrayType = new PartialFileRequest[0];
    List newRequestList = new ArrayList();
    for(Iterator i = requests.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry) i.next();
      PartialFileRequest[] fileArray = 
        (PartialFileRequest[]) ((List)entry.getValue()).toArray(arrayType);
      PartialFileListRequest fileList = 
        new PartialFileListRequest(
            ((KeyWrapper)entry.getKey()).getRequest().getUserKey(), fileArray);
      
      // set Transaction ID from the first PartialFileRequest
      // (it's OK because everyone should have the same transactionId)
      String transactionId = fileArray[0].getTransactionId();
      if (transactionId != null) {
        fileList.setTransactionId(transactionId);
      }
      
      // preserve all attributes (actually each PartialFileRequest
      // will overwrite the previous one's attributes)
      for (int j = 0; j < fileArray.length; j++) {
        fileList.setAttributes(fileArray[j].getAttributes());
      }
      
      // add the new PartialFileListRequest
      newRequestList.add(fileList);
    }
    
    filter.reset();
    filter.setPreviousRequests(newRequestList);
    filter.acceptAll();
    
    return filter;
  }

  /**
   * @param attMethodName
   * @return
   */
  private int getCompareMethod(final String attMethodName) {
    final int attMethod;
    if (IDENTITY_METHOD_NAME.equals(attMethodName)) {
      attMethod = IDENTITY_METHOD;
    } else if (EQUALS_METHOD_NAME.equals(attMethodName)) {
      attMethod = EQUALS_METHOD;
    } else if (STRING_METHOD_NAME.equals(attMethodName)) {
      attMethod = STRING_METHOD;
    } else {
      throw new IllegalArgumentException("Compare method '" + attMethodName + "' not valid.");
    }
    return attMethod;
  }

  protected static class Constraint {
    private String attribute;
    private int    method;
    
    public Constraint(String attribute, int method) {
      this.attribute = attribute;
      this.method    = method;
    }

    /**
     * @return Returns the attribute.
     */
    public String getAttribute() {
      return attribute;
    }

    /**
     * @param attribute The attribute to set.
     */
    public void setAttribute(String attribute) {
      this.attribute = attribute;
    }

    /**
     * @return Returns the method.
     */
    public int getMethod() {
      return method;
    }

    /**
     * @param method The method to set.
     */
    public void setMethod(int method) {
      this.method = method;
    }
    
    public boolean isMatch(Request r1, Request r2) {
      boolean isMatch = false;
      switch(this.method) {
      case IDENTITY_METHOD:
        isMatch = r1.getAttributes().get(this.attribute) == 
                  r2.getAttributes().get(this.attribute);
        break;
      case EQUALS_METHOD:
        if((r1.getAttributes().get(this.attribute) != null) &&
           (r2.getAttributes().get(this.attribute) != null)) {
          isMatch = r1.getAttributes().get(this.attribute).equals( 
                    r2.getAttributes().get(this.attribute));
        }
        break;
      case STRING_METHOD:
        if((r1.getAttributes().get(this.attribute) != null) &&
           (r2.getAttributes().get(this.attribute) != null)) {
          isMatch = r1.getAttributes().get(this.attribute).toString().equals( 
                    r2.getAttributes().get(this.attribute).toString());
        }
        break;
      }
      return isMatch;
    }
    
    public String toString() {
      String methodName = "";
      switch (this.method) {
      case IDENTITY_METHOD: methodName = IDENTITY_METHOD_NAME; break;
      case EQUALS_METHOD: methodName = EQUALS_METHOD_NAME; break;
      case STRING_METHOD: methodName = STRING_METHOD_NAME; break;
      }
      return "Constraint( "+this.attribute+" "+methodName+" )";
    }
  }

  protected static class KeyWrapper implements Cloneable {
    private Request       request;
    private Constraint[]  constraints;
    
    public KeyWrapper() {
      this(null);
    }
    
    public KeyWrapper(Constraint[] constraints) {
      this.constraints = (constraints != null) ? constraints : new Constraint[0];
    }

    /**
     * @return Returns the constraints.
     */
    public Constraint[] getConstraints() {
      return constraints;
    }

    /**
     * @param constraints The constraints to set.
     */
    public void setConstraints(Constraint[] constraints) {
      this.constraints = constraints;
    }

    /**
     * @return Returns the request.
     */
    public Request getRequest() {
      return request;
    }

    /**
     * @param request The request to set.
     */
    public void setRequest(Request request) {
      this.request = request;
    }
    
    public boolean equals(Object key) {
      boolean equals = true;
      if((this.request != null) && (key instanceof KeyWrapper) && 
         (((KeyWrapper)key).request != null) && 
         (this.request.getUserKey().equals(((KeyWrapper)key).request.getUserKey()))) {
        Request other = ((KeyWrapper)key).request;
        for(int i = 0; i < constraints.length; i++) {
          if(!constraints[i].isMatch(this.request, other)) {
            equals = false;
            break;
          }
        }
      } else {
        equals = false;
      }
      return equals;
    }
    
    public int hashCode() {
      int hashCode = 0;
      if(this.request != null) {
        hashCode += this.request.getUserKey().hashCode();
        for( int i = 0 ; i < constraints.length ; i++ ) {
          Object attr = this.request.getAttributes().get(constraints[i].getAttribute()); 
          if(attr != null) {
            hashCode += ( i + 2 ) * attr.hashCode();
          }
        }
      }
      return hashCode;
    }
    
    public Object clone() {
      KeyWrapper key = new KeyWrapper(this.constraints);
      key.setRequest(this.request);
      return key;
    }
  }
  
  
}
