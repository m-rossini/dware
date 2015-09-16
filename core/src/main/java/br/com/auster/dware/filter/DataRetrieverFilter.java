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
 * Created on 27/01/2006
 */

package br.com.auster.dware.filter;

import java.util.Map;

import org.w3c.dom.Element;

import br.com.auster.common.data.DataRetriever;
import br.com.auster.common.stats.ProcessingStats;
import br.com.auster.common.stats.StatsMapping;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.dware.graph.Request;

/**
 * <p><b>Title:</b> DataRetrieverFilter</p>
 * <p><b>Description:</b> A Filter that allows to retrieve data from the received
 * map.</p>
 * <p><b>Copyright:</b> Copyright (c) 2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author etirelli
 * @version $Id: DataRetrieverFilter.java 365 2008-07-20 23:51:40Z lmorozow $
 */

public class DataRetrieverFilter extends DefaultFilter implements ObjectProcessor {

  private static final String CONFIG_DATA_RETRIEVER       = "data-retriever";
  private static final String CONFIG_CLASS_NAME           = "class-name";
  private static final String CONFIG_CONF_FILE            = "config-file";
  private static final String CONFIG_ENCRYPTED            = "encrypted";
  
  private static final String CONFIG_REQUEST              = "request-tag";
  private static final String CONFIG_NAME_ATTR            = "name";
  
  private DataRetriever retriever = null;
  private Request       request   = null;

  private ObjectProcessor objProcessor;
  
  private String requestTag   = null;
  
  public DataRetrieverFilter(String name) {
    super(name);
  }

  /**
   * @inheritDoc
   */
  public void prepare(Request request) throws FilterException {
    this.request = request;
  }
  
  /**
   * @inheritDoc
   */
  public void configure(Element config) throws FilterException {

    Element dataRetriever = DOMUtils.getElement(config, CONFIG_DATA_RETRIEVER, true);
    String  drClass   = DOMUtils.getAttribute(dataRetriever, CONFIG_CLASS_NAME, true);
    String  drConfig  = DOMUtils.getAttribute(dataRetriever, CONFIG_CONF_FILE, true);
    String  drEncrypt = DOMUtils.getAttribute(dataRetriever, CONFIG_ENCRYPTED, true);
    
    try {
      retriever = (DataRetriever) Class.forName(drClass).newInstance();
      Element conf = DOMUtils.openDocument(drConfig, Boolean.valueOf(drEncrypt).booleanValue());
      retriever.configure(conf);
    } catch (Exception e) {
      throw new FilterException("Error instantiating DataRetriever class ["+drClass+"]", e);
    }
    
    Element requestElem = DOMUtils.getElement(config, CONFIG_REQUEST, true);
    requestTag          = DOMUtils.getAttribute(requestElem, CONFIG_NAME_ATTR, true);
  }
  
  /**
   * @inheritDoc
   */
  public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
    return this;
  }

  /**
   * @inheritDoc
   * 
   * @param _filterName
   * @param _output
   * @return
   * @throws ConnectException
   * @throws UnsupportedOperationException
   */
  public Object getOutput(String _filterName, Object _output) throws ConnectException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets the Output for this filter.
   */
  public void setOutput(String sourceName, Object objProcessor) {
    this.objProcessor = (ObjectProcessor) objProcessor;
  }
  
  /**
   * @inheritDoc
   */
  public void processElement(Object data) throws FilterException {
    try {
      Map dataMap = (Map) data;
      dataMap.put(requestTag, request);
      Map results = retriever.retrieve(dataMap);
      dataMap.put(requestTag, null);
      dataMap.putAll(results);

      StatsMapping stats = ProcessingStats.starting(this.objProcessor.getClass(), "processElement()");
      try {
    	  this.objProcessor.processElement(dataMap);
      } finally {
    	  stats.finished();
      }
    } catch (Exception e) {
      throw new FilterException("Error retrieving data", e);
    }
  }
}
