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

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.filter.template.TemplateSerializer;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * This filter receives a Object Tree and can do any processing with it The
 * Object Tree will be passed to this filtro thru processObject(Element obj)
 * method call.
 * 
 * <p>
 * To get the input : ObjectProcessor getInput(null). Will give to previous
 * filter this instance.
 * 
 * <p>
 * A object serializer filter should be configured like :
 * 
 * <pre>
 *     &lt;dw:obj-writer class=&quot;...&quot;/&gt;
 * </pre>
 * 
 * @author Marcos Tengelmann
 * @version $Id: ObjectSerializerFilter.java 87 2005-08-04 21:21:25Z rbarone $
 * 
 */
public class ObjectSerializerFilter extends DefaultFilter {

  protected static final Logger log = Logger.getLogger(ObjectSerializerFilter.class);

  private String serializerClass;

  private TemplateSerializer serializer;

  public static final String CONFIGURATION_OUTPUT_CLASS = "class";

  public ObjectSerializerFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter.
   */
  public void configure(Element _configuration) throws FilterException {
    this.serializerClass = DOMUtils.getAttribute(_configuration, CONFIGURATION_OUTPUT_CLASS, true);
  }

  public void prepare(Request request) throws FilterException {
    try {
      Class klass = Class.forName(this.serializerClass);
      this.serializer = (TemplateSerializer) klass.newInstance();
      this.serializer.setOutputStream(System.out);
    } catch (ClassNotFoundException cnfe) {
      throw new FilterException(cnfe);
    } catch (InstantiationException ie) {
      throw new FilterException(ie);
    } catch (IllegalAccessException iae) {
      throw new FilterException(iae);
    }
  }

  public Object getInput(String sourceName) {
    return this.serializer;
  }

  public void setOutput(String sinkName, Object output) {
    if (output instanceof WritableByteChannel) {
      output = Channels.newOutputStream((WritableByteChannel) output);
    }
    this.serializer.setOutputStream((OutputStream) output);
  }
}
