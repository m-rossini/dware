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

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;

/**
 * This filter receives a Object Tree and can do any processing with it The
 * Object Tree will be passed to this filtro thru processObject(Element obj)
 * method call.
 * 
 * <p>
 * To get the input : ObjectProcessor getInput(null). Will give to previous 
 * filter this instance
 * 
 * @author Marcos Tengelmann
 * @version $Id: ObjectReceiverFilter.java 87 2005-08-04 21:21:25Z rbarone $
 * 
 */
public class ObjectReceiverFilter extends DefaultFilter implements ObjectProcessor {

  protected static final Logger log = Logger.getLogger(ObjectReceiverFilter.class);

  private static final String ROOT_CLASS_ATTR = "root-class";

  private String rootClass;

  public ObjectReceiverFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public void configure(Element config) throws FilterException {
    this.rootClass = DOMUtils.getAttribute(config, ROOT_CLASS_ATTR, false);
  }

  /**
   * Returns the name of the Root Class Name passed as Parameter in COnfig
   * 
   * @return The name of the root class (Which is Equals to the ROOT XML Element
   */
  public String getRootClassName() {
    return this.rootClass;
  }

  /**
   * Gets the input for this filter
   */
  public Object getInput(String sourceName) {
    return this;
  }

  // public void processElement(javax.xml.bind.Element object) throws
  // FilterException {
  public void processElement(Object object) throws FilterException {
    log.debug("Element Processor=>" + object);
    log.debug("Element Processor Class Name=>" + object.getClass().getName());
  }

}
