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
 * Created on 08/12/2005
 */

package br.com.auster.dware.filter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;

/**
 * <p><b>Title:</b> ObjectProcessorManagerFilter</p>
 * <p><b>Description:</b> A Object Processor Filter that calls all outputs</p>
 * <p><b>Copyright:</b> Copyright (c) 2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author etirelli
 * @version $Id: ObjectProcessorManagerFilter.java 127 2005-12-28 14:03:43Z etirelli $
 */

public class ObjectProcessorManagerFilter extends DefaultFilter 
                                          implements ObjectProcessor {
  
  private static Logger log = Logger.getLogger(ObjectProcessorManagerFilter.class);
  private Map declaredOutputs = new HashMap();

  /**
   * Default constructor
   * @param name
   */
  public ObjectProcessorManagerFilter(String name) {
    super(name);
  }

  /**
   * @inheritDoc
   */
  public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
    return this;
  }
  
  /**
   * @inheritDoc
   */
  public void processElement(Object object) throws FilterException {
    for(Iterator i = declaredOutputs.values().iterator(); i.hasNext(); ) {
      ObjectProcessor processor = (ObjectProcessor) i.next();
      processor.processElement(object);
    }
  }

  /**
   * Sets the content handler for output of this filter.
   */
  public void setOutput(String sinkName, Object output) {
    Object declaredOutput = this.declaredOutputs.get(sinkName);
    if (declaredOutput == null) {
      this.declaredOutputs.put(sinkName, (ObjectProcessor) output);
      log.debug("["+sinkName+"] added to output list.");
    }
  }
}
