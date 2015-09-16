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
package br.com.auster.dware.graph;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.stats.ProcessingStats;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * This class represents an entire filter graph. The filters are the vertices
 * and the connection between each one are the edges.
 * 
 * Instances of this class are thread-safe.
 * 
 * @version $Id: Graph.java 362 2008-07-12 19:42:00Z lmorozow $
 */
public final class Graph {

  /**
   * This class represents a connection between two filters. It can be used to
   * connect them at any moment.
   */
  private static final class Edge {

    private static final Class[] connectors = Connector.class.getClasses();

    private Connector connector = null;

    private final Sink sink;

    private final Source source;

    private static final Logger log = Logger.getLogger(Edge.class);

    private final I18n i18n = I18n.getInstance(Edge.class);

    /**
     * Creates a edge between a source and a sink.
     * 
     * @param source
     *          the source to connect from.
     * @param sink
     *          the sink to connect to.
     * @param connector
     *          the connector object to be used to connect. If it is null, some
     *          default connectors will be tried before giving up.
     */
    public Edge(Source source, Sink sink, Connector connector) {
      this.source = source;
      this.sink = sink;
      this.connector = connector;
    }

    /**
     * Connects the source to the sink using the connector implementation.
     */
    public final void connect() throws ConnectException {
      if (log.isDebugEnabled())
        log.debug("Connecting '" + this.source + "' to '" + this.sink + "'...");

      if (this.connector == null)
        this.connector = connect(this.source, this.sink);
      else
        // We already have a connector that worked last time,
        // so let's try to use it instead of looking for another one.
        try {
          // Tries to connect
          this.connector.connect(this.source, this.sink);
          if (log.isDebugEnabled())
            log.debug("Connected [ " + source + " -> " + sink + " ] through " + connector);
        } catch (ConnectException e) {
          // Could not connect. Try to find another valid connector.
          if (log.isDebugEnabled())
            log.debug("Try failed for connection [ " + source + " -> " + sink + " ] through "
                      + connector, e);
          this.connector = connect(this.source, this.sink);
        }
    }

    /**
     * Tries to find an acceptable connector between a source and a sink.
     * 
     * @param source
     *          the source.
     * @param sink
     *          the sink.
     * @return a valid connection between the source and the sink.
     * @exception ConnectException
     *              if an acceptable connector could not be found.
     */
    private final Connector connect(Source source, Sink sink) throws ConnectException {
      Connector connector = null;
      if (connectors == null || connectors.length == 0) {
        throw new ConnectException(this.i18n.getString("noConnectorsAvailable"));
      }

      for (int i = 0; connector == null && i < connectors.length; i++) {
        Connector connectorTest = null;
        try {
          connectorTest = (Connector) connectors[i].newInstance();
          connectorTest.connect(source, sink);
          // The connection was done successfully.
          connector = connectorTest;
          if (log.isDebugEnabled())
            log.debug("Connected [ " + source + " -> " + sink + " ] through " + connector);
        } catch (Exception e) {
          // The last try failed. Log it.
          if (log.isDebugEnabled())
            log.debug("Try failed for connection [ " + source + " -> " + sink + " ] through "
                      + connectorTest, e);
        }
      }
      if (connector == null) {
        throw new ConnectException(this.i18n.getString("noConnectorAcceptable", source, sink));
      }

      return connector;
    }
  }

  public static final String TIME_FORMAT = "HH'h'mm'm'ss'.'SSS's'";

  // Configuration definitions
  public static final String LISTENER_ELEMENT = "finish-listener";

  public static final String FILTER_ELEMENT = "filter";

  public static final String CONFIG_ELEMENT = "config";

  public static final String CONNECT_ELEMENT = "connect-to";

  public static final String FILTER_NAME_ATTR = "name";

  public static final String CLASS_NAME_ATTR = "class-name";

  public static final String CACHE_FILTERS_ATTR = "use-filter-cache";
  
  public static final String IDLE_TIMEOUT_ATTR = "idle-timeout";

  public static Boolean syncGraphConfig = new Boolean(true);
  
  
  //
  // Instance attributes
  //
  
  // configuration node (XML)
  private Element config;
  
  // ThreadedFilter array
  private ThreadedFilter[] threadList;

  private Edge[] edgeList; // Edge array

  // Map<Filter> for searching filters
  private final Map filterMap = new LinkedHashMap();
  
  // filters array used to iterate all filters
  private Filter[] filterList;

  private static final Logger log = Logger.getLogger(Graph.class);

  private final I18n i18n = I18n.getInstance(Graph.class);

  private final SimpleDateFormat timeFormat = new SimpleDateFormat(TIME_FORMAT);

  private final Date time = new Date();

  private final String name;

  private FinishListener finishListener = null;

  protected FinishListener externalListener = null;

  // Attributes for multi-threaded environment control
  private final Object graphSync = new Object();

  private final Object threadedFilterSync = new Object();

  private volatile int threadsRunning = 0;

  private volatile boolean processing = false;

  private Throwable problem = null;

  private Request request = null;
  
  private JMXGraphGroupCounter counters;
  
  private TimeoutHandler idleTimeout = null;


  /**
   * Creates a filter graph, used to process requests.
   * 
   * @param graphName
   *          the name of this graph.
   * @param graphConf
   *          the configuration of this graph, in the form of a DOM tree.
   *          waiting for requests before automatically shutting down.
   * @throws GraphException
   *           if some error occurs while creating the graph.
   */
  public Graph(String graphName, Element graphConf) throws GraphException {
    this.name = graphName;
    this.timeFormat.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    synchronized (Graph.syncGraphConfig) {
      this.configureGraph(graphConf);
    }
  }

  public void setJMXCounters(JMXGraphGroupCounter _counters) {
	  this.counters = _counters;
  }
  
  /**
   * Configures the filter graph, used to process requests.
   * 
   * @param graphConf
   *          the configuration of this graph, in the form of a DOM tree.
   * @throws GraphException
   *           if some error occurs while creating the graph.
   */
  public final void configureGraph(Element graphConf) throws GraphException {
    this.config = graphConf;
    
    // configures the idle-timeout
    String timeoutAttr = DOMUtils.getAttribute(this.config, IDLE_TIMEOUT_ATTR, false);
    if (timeoutAttr != null) {
      try {
        long timeoutDuration = Long.valueOf(timeoutAttr).longValue();
        if (timeoutDuration <= 0L) {
          log.error(i18n.getString("invalidTimeout", timeoutAttr));
        }
        this.idleTimeout = new TimeoutHandler(timeoutDuration);
      } catch (NumberFormatException e) {
        log.error(i18n.getString("invalidTimeout", timeoutAttr));
      }
    }
    
    configListeners();
    configFilters();
  }
  
  private void configListeners() throws GraphException {
    // Configuring the external FinishListener (if it exists)
    Element listener = DOMUtils.getElement(this.config, LISTENER_ELEMENT, false);
    if (listener != null) {
      String className = DOMUtils.getAttribute(listener, CLASS_NAME_ATTR, true);
      try {
        log.debug("Instantiating the finish listener '" + className + "'.");
        Class[] c = { Element.class };
        Object[] o = { DOMUtils.getElement(listener, Graph.CONFIG_ELEMENT, false) };
        this.externalListener = (FinishListener) 
          Class.forName(className).getConstructor(c).newInstance(o);
        log.info(i18n.getString("listenerConfigured", this.name, className));
      } catch (Exception e) {
        log.error(i18n.getString("listenerConfigError", this.name, className), e);
        this.externalListener = null;
      }
    } else {
      this.externalListener = null;
    }    
  }
  
  private void configFilters() throws GraphException {    
    final NodeList filters = DOMUtils.getElements(this.config, FILTER_ELEMENT);
    if (this.config == null || filters.getLength() == 0) {
      throw new GraphException(i18n.getString("filterListEmpty"));
    }

    if (!this.filterMap.isEmpty()) {
      this.interruptThreadedFilters();
    }

    try {
      synchronized (this.graphSync) {
        log.info(i18n.getString("configuringGraph", this.name));

        final Element[] filtersElem = new Element[filters.getLength()];

        // Connects each filter to their adjacents
        for (int i = 0; i < filtersElem.length; i++) {
          filtersElem[i] = (Element) filters.item(i);
        }

        // Creates every filter found in the node list
        addFilters(filtersElem);

        // connects filters
        addEdges(filtersElem);

        log.info(i18n.getString("graphConfigured", this.name));
      }
    } catch (Throwable e) {
      // Stops all the started threads if this graph could not be created
      // correctly.
      this.shutdown(false);
      throw new GraphException(e);
    }
  }

  /**
   * Use this method to reconfigure any filter that is already in the graph.
   * 
   * To add or remove some filters, or to change the configuration about the
   * graph or connections between the filters, use the method
   * <code>configureGraph()</code>.
   * 
   * @param filterName
   *          the name of the filter that will be reconfigured.
   * @param config
   *          the new configuration for the filter.
   * @throws GraphException
   *           if the filter does not exist in the graph.
   * @throws FilterException
   *           if some error occurs while reconfiguring the filter. WARN: If
   *           this exception is thrown, the graph functionability may be
   *           compromised, because some changes may had been executed, some
   *           others may not.
   */
  public final void configureFilter(String filterName, Element config) throws GraphException,
      FilterException {
    if (!this.filterMap.containsKey(filterName)) {
      throw new GraphException(i18n.getString("noSuchFilter", filterName, this.name));
    }

    ((Filter) this.filterMap.get(filterName)).configure(config);
  }

  /**
   * Given a filter configuration, creates an instance of it and returns.
   * 
   * @param filterElement
   *          the DOM tree corresponding to the filter information and
   *          configuration.
   * @return a filter instance based on the information of the given element.
   */
  private final Filter getFilterInstance(Element filterElement) throws ClassNotFoundException,
      NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, FilterException {
    String filterName = DOMUtils.getAttribute(filterElement, FILTER_NAME_ATTR, true);
    String className = DOMUtils.getAttribute(filterElement, CLASS_NAME_ATTR, true);

    if (log.isDebugEnabled())
      log.debug("Instantiating the filter '" + filterName + "' using classname '" + className
                + "'.");
    Class[] c = { String.class };
    Object[] o = { filterName };
    Filter filter = (Filter) Class.forName(className).getConstructor(c).newInstance(o);

    // If this filter does not have a configuration defined,
    // creates an empty one just to be passed to the filter constructor
    Element filterConf = null;
    try {
      filterConf = DOMUtils.getElement(filterElement, CONFIG_ELEMENT, true);
    } catch (IllegalArgumentException e) {
      filterConf = filterElement.getOwnerDocument()
          .createElementNS(filterElement.getNamespaceURI(), CONFIG_ELEMENT);
    }

    filter.configure(filterConf);

    return filter;
  }

  /**
   * Adds a filter to this graph.
   * 
   * @param filters
   *          DOM filter elements
   * @throws GraphException
   *           if some error occurs while adding the filter to the list of
   *           filters for this graph.
   * @throws ClassNotFoundException
   *           if filter's class is not found.
   */
  private final void addFilters(Element[] filters) throws GraphException, ClassNotFoundException,
      NoSuchMethodException, InstantiationException, IllegalAccessException,
      InvocationTargetException, FilterException {

    // Creates every filter found in the node list
    final ArrayList tlistAux = new ArrayList();

    try {
      for (int i = 0; i < filters.length; i++) {
        Element filterNode = filters[i];
        String filterName = DOMUtils.getAttribute(filterNode, FILTER_NAME_ATTR, true);

        log.info(i18n.getString("addFilter", filterName));
        Filter filter = this.getFilterInstance(filterNode);

        if (this.filterMap.containsKey(filterName)) {
          throw new GraphException(i18n.getString("filterAlreadyAdded", filterName));
        }
        this.filterMap.put(filterName, filter);

        // threaded filters
        if (filter instanceof ThreadedFilter) {
          ThreadedFilter thread = (ThreadedFilter) filter;
          thread.setName(this.name + " - " + filterName);
          thread.setGraph(this);
          tlistAux.add(thread);
          if (!thread.isAlive()) {
            thread.start();
          } else {
            log.info(i18n.getString("threadAlive", thread.getName()));
          }
        }
        
      }
    } finally {
      filterList = (Filter[]) filterMap.values().toArray(new Filter[0]);
      threadList = (ThreadedFilter[]) tlistAux.toArray(new ThreadedFilter[0]);
    }
  }

  /**
   * Specifies how two filters must be connected to each other.
   * 
   * @param from
   *          the filter name that will be the source.
   * @param to
   *          the filter name that will be connected to.
   * @param connector
   *          the object that will do the connection.
   * @param connectionName
   *          the connection name.
   * @param connectorClassName
   *          the connector class name. It may be null or empty, if it is wanted
   *          that a suitable connector is searched between the defaults.
   * @throws GraphException
   *           if one or both filters do not exist.
   */
  private final void addEdges(Element[] filters)// String from, String to,
                                                // String connectorClassName)
      throws GraphException {

    String connectorClassName, from, to;
    final ArrayList edgeListAux = new ArrayList();

    // Connects each filter to their adjacents
    for (int i = 0; i < filters.length; i++) {
      Element filterNode = filters[i];
      from = DOMUtils.getAttribute(filterNode, FILTER_NAME_ATTR, true);

      // Gets the adjacent list
      NodeList adjacentList = DOMUtils.getElements(filterNode, CONNECT_ELEMENT);
      if (adjacentList == null || adjacentList.getLength() == 0) {
        log.warn(i18n.getString("noConnectionFilter", from));
      } else {
        for (int j = 0, adjSize = adjacentList.getLength(); j < adjSize; j++) {
          Element adjacentNode = (Element) adjacentList.item(j);

          // Gets the adjacent filter name
          to = DOMUtils.getText(adjacentNode).toString();
          if (to.length() == 0)
            to = DOMUtils.getAttribute(adjacentNode, FILTER_NAME_ATTR, true);

          // this.addEdge(filterName, adjacentFilterName,
          // adjacentNode.getAttribute(CLASS_NAME_ATTR));
          log.info(i18n.getString("makingConnection", from, to));

          final Source source = (Source) this.filterMap.get(from);
          final Sink sink = (Sink) this.filterMap.get(to);

          if (source == null || sink == null) {
            throw new GraphException(i18n.getString("connectionFilterFailed", from, to));
          }

          Connector connector = null;
          connectorClassName = adjacentNode.getAttribute(CLASS_NAME_ATTR);
          if (connectorClassName != null && connectorClassName.length() != 0)
            try {
              connector = (Connector) Class.forName(connectorClassName).newInstance();
            } catch (Exception e) {
              log.error(i18n.getString("invalidConnectorClassName", connectorClassName), e);
            }

          edgeListAux.add(new Edge(source, sink, connector));
        }
      }
    }
    edgeList = (Edge[]) edgeListAux.toArray(new Edge[0]);
  }

  /**
   * Starts this graph to process the given request. When the graph finishes to
   * process, it will call the <code>finishListner</code> object, if it is not
   * null, and will notify all the threads that are blocked in the method
   * <code>waitProcessing</code>.
   * 
   * @param request
   *          the request to be processed.
   * @param listener
   *          the object that will be called when this processing finishes.
   * @throws IllegalStateException
   *           if this graph is not properly configured.
   */
  public final void process(Request request, FinishListener listener) {
    // Waits untill this graph is ready to process this request
    synchronized (this.graphSync) {
      
      if (this.idleTimeout != null) {
        this.idleTimeout.stopClock();
      }

      while (this.processing) {
        try {
          this.waitProcessing(-1);
        } catch (InterruptedException e) {
          log.debug("Got interruption.", e);
        }
      }
      
      if (this.filterMap.isEmpty()) {
        try {
          configListeners();
          configFilters();
        } catch (GraphException e) {
          throw new RuntimeException(e);
        }
      }
      
      this.processing = true;
      this.problem = null;
      this.request = request;
      this.finishListener = listener;
    }

    log.info(i18n.getString("processingRequest", request, this.name));
    time.setTime(System.currentTimeMillis());

    try {
      // Making sure the threads are OK
      this.prepareThreadedFilters();
      // Call 'prepare' on all filters
      this.prepareFilters(request);
      // call get/set output on all filters
      this.connectGraph();
      // start processing
      this.startThreadedFilters();
      
    } catch (Throwable t) {
      this.problem = t;
      this.rollback(request, t);
      this.interruptThreadedFilters();
      this.finish(request);
    }
  }

  /**
   * Starts this graph to process the given request. This method will block the
   * calling thread until it finishes to process.
   * 
   * @param request
   *          the request to be processed.
   * @throws Throwable
   *           if some problem ocurred while processing.
   */
  public final void process(Request request) throws Throwable {
    this.process(request, null);
    log.info(i18n.getString("waitingRequest", request, this.name));
    this.waitProcessing(-1);
    if (this.problem != null) {
      throw problem;
    }
  }

  /**
   * Tells to each filter what request will be processed, before starting to
   * process.
   * 
   * @param request
   *          the request to be processed.
   * @throws FilterException
   *           if some error occurs while preparing the filters
   */
  private final void prepareFilters(Request request) throws FilterException {
    log.debug("Starting preparation of filters for graph '" + this.name + "'...");
    for (int i = 0; i < filterList.length; i++) {
      filterList[i].prepare(request);
    }
    log.debug("Finished preparing filters for graph '" + this.name + "'.");
  }

  /**
   * Connects all the filters using the configuration given at system startup,
   * using the edges between them.
   * 
   * @throws ConnectException
   *           if some error occurs while connecting the filters
   */
  private final void connectGraph() throws ConnectException {
    log.debug("Starting connecting graph '" + this.name + "'...");
    for (int i = 0; i < edgeList.length; i++) {
      edgeList[i].connect();
    }
    log.debug("Finished connecting graph '" + this.name + "'.");
  }

  /**
   * Makes sure all ThreadedFilters are ready and OK to be run.
   * 
   * @throws Throwable
   *           if some filter has thrown an exception.
   */
  private final void prepareThreadedFilters() throws GraphException {
    // This is used to control the amount of threads that need to
    // finish their job.
    try {
      boolean problemFound = false;
      for (int i = 0; i < this.threadList.length; i++) {
        if (!this.threadList[i].isAlive()) {
          log.warn(i18n.getString("threadedFilterDown",this.name,this.threadList[i].getFilterName()));
          problemFound = true;
          break;
        }
      }
      if (problemFound) {
        configFilters();
      }
    } catch (Throwable t) {
      throw new GraphException("Fatal error while reconfiguring filters.", t);
    }
  }
  
  /**
   * Starts the filters that must run as threads. This method will wait until
   * every filter has finished its job.
   * 
   * @throws Throwable
   *           if some filter has thrown an exception.
   */
  private final void startThreadedFilters() throws GraphException {
	    // This is used to control the amount of threads that need to
	    // finish their job.
	    try {
	      this.threadsRunning = threadList.length;
	      for (int i = 0; i < this.threadList.length; i++) {
	        ThreadedFilter t = this.threadList[i];
	        if (!t.isAlive()) {
	        	throw new FilterException(i18n.getString("shouldNotBeDead", this.name));
	        } else {
	          t.go();
          }
	      }
      } catch (Throwable t) {
        throw new GraphException("Fatal error while starting to process filters.", t);
      }
	  }
  

  /**
   * This method is called by each threaded filter that is running in this
   * graph. It is called when it finishes its work for a request. This is used
   * to control the moment to commit or to rollback a request processing.
   */
  protected final void threadedFilterFinished(ThreadedFilter filter, Throwable error) {
    synchronized (this.threadedFilterSync) {
      // A threaded filter finished.
      this.threadsRunning--;
      try {
        if (this.problem != null) {
          // This condition is used to avoid other later
          // threaded filters that received error while
          // processing
          return;
        } else if (error != null) {
          // We got an error. Let's rollback all the processing
          // (using the filter's rollback methods)
          this.rollback(this.request, error);
        } else if (this.threadsRunning == 0) {
          // No errors until now and all threaded filters
          // finished. Let's commit all the processing (using
          // the filter's commit methods)
          this.commit(this.request);
        }
      } catch (Throwable e) {
        // In case the rollback or the commit fails, we can
        // not trust in it any more. The only thing to do is
        // to finish this request processing.
        this.problem = e;
      } finally {
        if (this.threadsRunning == 0) {
          this.finish(this.request);
        }
      }
    }
  }

  /**
   * Notifies all the filters to commit the last processing. It must be called
   * only if it was successful.
   */
  private final void commit(Request request) {
    try {
      if (this.finishListener != null) {
        this.finishListener.graphCommiting(this, request);
      }
      if (this.externalListener != null) {
        this.externalListener.graphCommiting(this, request);
      }
    } catch (Throwable e) {
      log.error(i18n.getString("finishListenerProblem", request, this.name), e);
      this.rollback(request, e);
      return;
    }
    
    log.info(i18n.getString("commitingRequest", request, this.name));
    try {
      for (int i = 0; i < filterList.length; i++) {
        filterList[i].commit();
      }
    } catch (Throwable e) {
      log.error(i18n.getString("commitError", request, this.name), e);
      this.rollback(request, e);
      return;
    }
  }

  /**
   * Notifies all the filters to roll back the last processing. It must be
   * called only if something went wrong with some filter.
   */
  private final void rollback(Request request, Throwable error) {
    this.problem = error;
    
    if (this.finishListener != null) {
      try {
        this.finishListener.graphRollingBack(this, request, error);
      } catch (Throwable e) {
        log.error(i18n.getString("finishListenerProblem", request, this.name), e);
      }
    }
    if (this.externalListener != null) {
      try {
        this.externalListener.graphRollingBack(this, request, error);
      } catch (Throwable e) {
        log.error(i18n.getString("listenerFinishError", this.name), e);
      }
    }
    
    log.error(i18n.getString("rollingbackRequest", request, this.name), error);
    try {
      for (int i = 0; i < filterList.length; i++) {
        filterList[i].rollback();
      }
    } catch (Throwable e) {
      log.error(i18n.getString("rollbackError", request, this.name), e);
    }

	this.counters.addToCounter(JMXGraphGroupCounter.ROLLEDBACK_REQUEST_COUNT, 1);
  }

  /**
   * This method is called after a commit or rollback. It notifies the external
   * threads that are waiting for the end of the last processing, it does a call
   * to the finish listener object (if defined) and it prints the total
   * processing time.
   */
  private final void finish(Request request) {
    // Calculates the processing time
    this.time.setTime(System.currentTimeMillis() - this.time.getTime());
    
    try {
      // update request counter
      this.counters.addToCounter(JMXGraphGroupCounter.FINISHED_REQUEST_COUNT, 1);
      
      // update time counters
      this.counters.addToCounter(JMXGraphGroupCounter.TOTAL_PROCESSING_TIME, this.time.getTime());
      if (this.time.getTime() > this.counters.getCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_TIME)) {
        this.counters.setCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_TIME, this.time.getTime());
      }
      if (this.time.getTime() < this.counters.getCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_TIME)) {
        this.counters.setCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_TIME, this.time.getTime());
      }

      // update weight counters
      this.counters.addToCounter(JMXGraphGroupCounter.TOTAL_WEIGHT, request.getWeight());
      if (request.getWeight() > this.counters.getCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_WEIGHT)) {
        this.counters.setCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_WEIGHT, request.getWeight());
      }
      if (request.getWeight() < this.counters.getCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_WEIGHT)) {
        this.counters.setCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_WEIGHT, request.getWeight());
      }
    } catch (Throwable t) {
      log.error(i18n.getString("counterUpdateError", this.name), t);
    }
    
    log.info(i18n.getString("totalProcTime", request, this.timeFormat.format(this.time)));

    ProcessingStats.dumpMyStats("Dumping stats for request ID " + request.getId());

    if (this.finishListener != null) {
      try {
        this.finishListener.graphFinished(this, request, this.problem, this.time);
      } catch (Throwable e) {
        log.error(i18n.getString("finishListenerProblem", request, this.name), e);
      }
    }
    if (this.externalListener != null) {
      try {
        this.externalListener.graphFinished(this, request, this.problem, this.time);
      } catch (Throwable e) {
        log.error(i18n.getString("listenerFinishError", this.name), e);
      }
    }

    // If some thread is waiting for the end of this one,
    // notify it.
    synchronized (this.graphSync) {
      this.processing = false;
      this.request = null; // just to remove the reference
      this.finishListener = null; // just to remove the reference
      if (this.idleTimeout != null) {
        this.idleTimeout.startClock();
      }
      this.graphSync.notifyAll();
    }
  }
  
  /**
   * Waits until the filter has finished processing the last request, if it's
   * processing some one.
   * 
   * @param milli
   *          the maximum time out, in milliseconds, that this method will wait
   *          for this processing to finish. If this value is negative, then it
   *          will wait with no time out.
   * @throws InterruptedException
   *           if some interruption is received while waiting, or if the maximum
   *           time to wait has expired.
   */
  public final void waitProcessing(int milli) throws InterruptedException {
    synchronized (this.graphSync) {
      if (this.processing) {
        log.debug("The graph '" + this.name + "' is busy. Let's wait...");
        if (milli < 0)
          this.graphSync.wait();
        else
          this.graphSync.wait(milli);
      }
    }
  }

  /**
   * Returns true if and only if this graph is processing a request when called.
   */
  public final boolean isProcessing() {
    return this.processing;
  }

  /**
   * Returns the problem that ocurred while processing the last request. This is
   * usefull to detect if the last request was processed successfully or not.
   */
  public final Throwable getLastProblem() {
    return this.problem;
  }

  /**
   * Notifies each filter thread to terminate. This method will block until this
   * graph is down if <code>wait</code> is true. After called, no more
   * requests will be processed by this graph.
   * 
   * @param wait
   *          if true this method will block until this graph is down.
   */
  public final void shutdown(boolean wait) {
    log.warn(i18n.getString("graphGoingDown", this.name));
    
    synchronized (this.graphSync) {
      // If this graph is still running, wait until it finishes
      while (this.processing && wait)
        try {
          this.waitProcessing(-1);
        } catch (InterruptedException e) {
          log.debug("Got interruption.", e);
        }
      // Shutdown all the threaded filters
      if (this.threadList != null) {
        for (int i = 0; i < this.threadList.length; i++) {
          this.threadList[i].shutdown(wait);
        }
      }

      // Clear all the references to the filters and connectors
      this.edgeList = null;
      this.threadList = null;
      this.filterList = null;
      this.filterMap.clear();
      
      if (this.idleTimeout != null) {
        this.idleTimeout.stopClock();
      }

      log.warn(i18n.getString("graphDown", this.name));
    }
  }
  
  /**
   * Interrupts all Threads and cleans all references.
   */
  private void interruptThreadedFilters() {
    if (this.threadList != null) {
      for (int i = 0; i < this.threadList.length; i++) {
        this.threadList[i].interrupt();
        try {
          this.threadList[i].join();
        } catch (InterruptedException e) {
        }
      }
    }
    this.threadsRunning = 0;
    this.edgeList = null;
    this.threadList = null;
    this.filterList = null;
    this.filterMap.clear();
  }

  public String toString() {
    return this.name;
  }
  
  /**
   * Controls the idle ellapsed time of a Graph and clean
   * all filters if a timeout is detected.
   * 
   * To stop the clock, one must call the stopClock() method.
   *
   */
  private class TimeoutHandler extends Thread {
    
    private long timeout; 
    
    private Lock lock = new ReentrantLock();
    private Condition started = this.lock.newCondition();
    private Condition stopped = this.lock.newCondition();
    
    private boolean cleared = false;
      
    /**
     * 
     * @param limitMillis the timeout limit, in milliseconds
     */
    public TimeoutHandler(long limitMillis) {
      this.timeout = limitMillis;
      this.setDaemon(true);
      this.start();
    }
      
    public void run() {
      while(true) {
        this.lock.lock();
        try {
          try {
            this.started.await();
          } catch (InterruptedException e) {
            Graph.log.warn("Graph " + Graph.this.name + " - TimeoutHandler has been interrupted while "+
                "waiting to start clock - restarting...");
            continue;
          }
          try {
            this.cleared = false;
            this.stopped.await(this.timeout, TimeUnit.MILLISECONDS);
          } catch (InterruptedException e) {
            Graph.log.warn("Timeout clock has been interrupted anormally - graph WILL NOT be cleaned until startClock() is called again.");
            this.cleared = true;
          }
        } finally {
          this.lock.unlock();
        }
        synchronized (Graph.this.graphSync) {
          if (!this.cleared) {
            // timeout occurred!
            log.info(i18n.getString("idleTimeout", Graph.this.name));
            Graph.this.shutdown(true);
            Graph.this.problem = null;
            Graph.this.request = null;
            Graph.this.externalListener = null;
            Graph.this.finishListener = null;
          }
        }
      }
    }
    
    public void startClock() {
      this.lock.lock();
      try {
        this.started.signal();
      } finally {
        this.lock.unlock();
      }
    }
      
    public void stopClock() {
      this.lock.lock();
      try {
        this.cleared = true;
        this.stopped.signal();
      } finally {
        this.lock.unlock();
      }
    }
  }
  
}
