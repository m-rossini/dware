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
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.request;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;


/**
 *
 * @author Ricardo Barone
 * @version $Id: RequestBuilderChain.java 361 2008-03-17 16:46:25Z mtengelm $
 */
public class RequestBuilderChain implements RequestBuilder {
  
  protected static final String FILTER_CLASS_ATT = "filter-class";
  
  protected static final String ACCEPT_ONCE_ATT = "ignore-duplicated-requests";
  protected static final String ROLLOVER_ATT = "rollover-attributes";
  
  protected static final String COMPARE_ELEMENT = "compare-results";
  protected static final String COMPARE_TO_ATT = "to-builder";
  protected static final String COMPARE_DIRECTION_ATT = "direction";
  
  protected static final String LEFT_DIRECTION_VALUE = "left";
  protected static final String RIGHT_DIRECTION_VALUE = "right";
  protected static final String ANY_DIRECTION_VALUE = "any";
  
  protected static final String	NOT_FOUND_NAME	= "notFoundFile";
  
  private static final Logger log = LogFactory.getLogger(RequestBuilderChain.class);	
	
  private final I18n i18n = I18n.getInstance(RequestBuilderChain.class);
  
  private final String name;
  private final Class filterClass;
  private final List builders = new ArrayList();
  private final Map filtersByBuilder = new HashMap();
  
  // Map<String(builder name), ComparationDefinition>
  private Map comparationDefs = new HashMap();
  
  // Map<String(builder name), Boolean(RequestFilter.acceptOnlyOnce)>
  private Map filterAcceptAtts = new HashMap();
  
  private Set rollOverFlags = new HashSet();
  
  /**
   * 
   */
  public RequestBuilderChain(String name, Element config) {
    this.name = name;
    
    String filterName = DOMUtils.getAttribute(config, FILTER_CLASS_ATT, false);
    if (filterName == null || filterName.length() == 0) {
      this.filterClass = HashRequestFilter.class;
    } else {
      try {
        this.filterClass = Class.forName(filterName);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException("RequestFilter '" + filterName + "' not found.", e);
      }
      if (!RequestFilter.class.isAssignableFrom(this.filterClass)) {
        throw new IllegalArgumentException("'" + filterName + "' is not a RequestFilter.");
      }
    }
    
    NodeList nodes = DOMUtils.getElements(config, RequestBuilderManager.BUILDER_ELEMENT); 
    for (int i = 0; i < nodes.getLength(); i++) {
      Element builder = (Element) nodes.item(i);
      String bName = DOMUtils.getAttribute(builder, RequestBuilderManager.NAME_ATT, true);
      final boolean acceptOnlyOnce = DOMUtils.getBooleanAttribute(builder, ACCEPT_ONCE_ATT, true);
      final boolean rollOver = DOMUtils.getBooleanAttribute(builder, ROLLOVER_ATT, false);
      final String builderFilter = DOMUtils.getAttribute(builder, FILTER_CLASS_ATT, false);
      if ((builderFilter != null) && (builderFilter.trim().length() > 0)) {
    	  this.filtersByBuilder.put(bName, builderFilter);
      }
      this.filterAcceptAtts.put(bName, Boolean.valueOf(acceptOnlyOnce));
      if (rollOver) {
    	  this.rollOverFlags.add(bName);
      }
      Element comp = DOMUtils.getElement(builder, COMPARE_ELEMENT, false);
      if (comp != null) {
        String toBuilder = DOMUtils.getAttribute(comp, COMPARE_TO_ATT, true);
        String d = DOMUtils.getAttribute(comp, COMPARE_DIRECTION_ATT, false);
        int direction = ComparationDefinition.ANY_DIRECTION;
        if (d != null && d.length() > 0) {
          if (d.equals(LEFT_DIRECTION_VALUE)) {
            direction = ComparationDefinition.LEFT_DIRECTION;
          } else if (d.equals(RIGHT_DIRECTION_VALUE)) {
            direction = ComparationDefinition.RIGHT_DIRECTION;
          } else if (!d.equals(ANY_DIRECTION_VALUE)) {
          	log.fatal("Acceptable Options for 'direction' are:" + LEFT_DIRECTION_VALUE +
          			" or " + RIGHT_DIRECTION_VALUE + " or " + ANY_DIRECTION_VALUE);
            throw new IllegalArgumentException("Attribute 'direction' is invalid: " + d);
          }
        }
        
        ComparationDefinition compDef = new ComparationDefinition(toBuilder, direction, bName);        
        this.comparationDefs.put(bName, compDef);
      }
    }
  }
  
  public void addBuilder(RequestBuilder builder) {
    this.builders.add(builder);
  }
  
  public List getBuilders() {
    return Collections.unmodifiableList(this.builders);
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
    return this.createRequests(null, args);
  }

  /**
   * @inheritDoc
   */
  public RequestFilter createRequests(RequestFilter filter, Map args) {
    // Map<String(builder name), List(accepted requests)>
    final Map builderResults = new HashMap();
   
    for (int i = 0; i < this.builders.size(); i++) {
      final RequestBuilder builder = (RequestBuilder) this.builders.get(i);
      final Map builderArgs = (Map) args.get(builder.getName());
      
      // this will be used as input for the next filter
      Collection previousRequests = null;
      if (filter != null) {
        //previousRequests = filter.getAcceptedRequests();
    	  previousRequests = new ArrayList();
    	  previousRequests.addAll(filter.getAcceptedRequests());
      }
      
      //if (filter == null || i == 0) {
        try {
          if (this.filtersByBuilder.containsKey(builder.getName())) {
        	  Class klass = Class.forName((String)this.filtersByBuilder.get(builder.getName()));
        	  filter = (RequestFilter) klass.newInstance();
          } else { 
        	  filter = (RequestFilter) this.filterClass.newInstance();
          }
        } catch (InstantiationException e) {
          throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
        	throw new RuntimeException(e);
		}
        
      //}

      if (filter instanceof HashRequestFilter) {
        Boolean acceptOnlyOnce = (Boolean) this.filterAcceptAtts.get(builder.getName());
        filter = ((HashRequestFilter) filter).reset(acceptOnlyOnce.booleanValue());
        ((HashRequestFilter) filter).setRollOver(this.rollOverFlags.contains(builder.getName()));
      } else {
        filter = filter.reset();
      }
      if (previousRequests != null) {
        // we have a list of accepted requests from a previous filter
        filter.setPreviousRequests(previousRequests);
      }

      filter = builder.createRequests(filter, builderArgs);
      final List results = new ArrayList();
      results.addAll(filter.getAcceptedRequests());
      builderResults.put(builder.getName(), results); 
      
      compareBuilderResults(builder.getName(), builderResults, args);
    }
    
    return filter;
  }
  
  public void compareBuilderResults(String builderName, Map resultMap, Map args) {
    if (!this.comparationDefs.containsKey(builderName)) { 
      return;
    }
    ComparationDefinition def = (ComparationDefinition) this.comparationDefs.get(builderName);
    //Lets see if we have to dump the not found requests.
    Map builderArgs = (HashMap) args.get(builderName);
    if (builderArgs != null) {
    	String notFoundName = (String) builderArgs.get(NOT_FOUND_NAME);
    	if (notFoundName != null && !"".equals(notFoundName)) {
    		BufferedWriter writer;
				try {
					writer = new BufferedWriter(new FileWriter(notFoundName));
	    		def.setNotFoundWriter(writer);
				} catch (IOException e) {
					//We can live we this exception.
					//The consequence is to not have the not found file generated
					log.error(i18n.getString("notFoundFileError"), e);
				}
    	}
    }
    
    List rightResults = (List) resultMap.get(builderName);
    List leftResults = (List) resultMap.get(def.getToBuilder());
    if (leftResults == null) {
      log.error("Could not compare results of builder '" + builderName + "' to builder '"
                + def.getToBuilder() + "' : to builder results is not available.");
      return;
    }
    if (def.getDirection() == ComparationDefinition.LEFT_DIRECTION || 
        def.getDirection() == ComparationDefinition.ANY_DIRECTION) {
      handleResultsDifference(builderName,
                              rightResults,
                              leftResults,
                              ComparationDefinition.LEFT_DIRECTION);
    }
    if (def.getDirection() == ComparationDefinition.RIGHT_DIRECTION || 
        def.getDirection() == ComparationDefinition.ANY_DIRECTION) {
      handleResultsDifference(def.getToBuilder(),
                              leftResults, 
                              rightResults,
                              ComparationDefinition.RIGHT_DIRECTION);
    }
  }
  
  private void handleResultsDifference(String builderName, List from, List to, int direction) {
  	ComparationDefinition cd = (ComparationDefinition) this.comparationDefs.get(builderName);
  	
    HashRequestFilter f = new HashRequestFilter(from, false);
    for (Iterator it = to.iterator(); it.hasNext();) {
      Request request = (Request) it.next();
      if (!f.accept(request, true)) {
        String message;
        if (direction == ComparationDefinition.LEFT_DIRECTION) {
          message = i18n.getString("leftRequestNotFound", request.toString(), builderName);
        } else {
          message = i18n.getString("rightRequestNotFound", request.toString(), builderName);
        }
        try {
        	if (cd != null) {
        		cd.writeNotFoundRecord(buildRecord(request));
        	}
				} catch (IOException e) {
					log.error("writeError", e);
				}
        log.warn(message);
      }
    }
    
    if (cd != null) {
    	cd.closeNotFoundWriter();
    }
  }
    
  /**
	 * Builds a record to be written based on the request.
	 * This method creates a record only with a user_key.
	 * If more advanced funcionality is needed, one has to overwrite this method.
	 * 
	 * @param request
	 * @return
	 */
	public String buildRecord(Request request) {		
		return request.getUserKey();
	}

	private class ComparationDefinition {

    public static final int ANY_DIRECTION = 0;
    public static final int LEFT_DIRECTION = 1;
    public static final int RIGHT_DIRECTION = 2;

    private String toBuilder;
    
    private int direction = ANY_DIRECTION;    
    
    private Writer notFoundOutput=null;
    
    public ComparationDefinition(String to, String from) {
      this.toBuilder = to;
    }
    
    public ComparationDefinition(String to, int direction, String fromBuilder) {
      this(to,fromBuilder);
      if (direction < ANY_DIRECTION || direction > RIGHT_DIRECTION) {
        throw new IllegalArgumentException("Invalid direction '" + direction + "'.");
      }
      this.direction = direction;
    }

    public String getToBuilder() {
      return this.toBuilder;
    }

    public int getDirection() {
      return this.direction;
    }		   
    
    public void setNotFoundWriter(Writer writer) {
    	this.notFoundOutput = writer;
    }
    
    /***
     * Gets the writer (Possibly a FileWriter) where a not found request will be written,
     * or null if not configured.
     * 
     * @see {{@link #getFileNames()}
     * 
     * @return A Writer or null if no notFound file name was not specified by set not fund method.
     */
    public Writer getNotFoundWriter() {
    	return this.notFoundOutput;
    }
    
    /***
     * Closes the not found file.
     */
    public void closeNotFoundWriter() {
    	if (notFoundOutput != null) {
    		try {
    			notFoundOutput.flush();
					notFoundOutput.close();
				} catch (IOException e) {
					log.error(i18n.getString("closeError", "Not Found Files"), e);
				}
    	}
    }
    
    public void writeNotFoundRecord(String record) throws IOException {
    	if (this.notFoundOutput != null) {
    		this.notFoundOutput.write(record);
    		this.notFoundOutput.write('\n');
    	}
    }
  }

}
