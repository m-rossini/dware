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
package br.com.auster.dware.manager.remote;

import java.io.File;
import java.rmi.MarshalException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.cli.CLOption;
import br.com.auster.common.cli.OptionsParser;
import br.com.auster.common.util.ConfigUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.util.SyncStack;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.FinishListener;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.GraphManager;
import br.com.auster.dware.manager.GraphManagerRemoteInterface;
import br.com.auster.dware.manager.ManagerException;
import br.com.auster.dware.manager.RemoteGraphGroupInterface;

/**
 * Facade for remote graph groups.
 * 
 * @version $Id: ClientRemoteGraphFacade.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class ClientRemoteGraphFacade extends PortableRemoteObject implements
    ClientRemoteGraphInterface {

  public static final String SQL_CONF_PARAM = "sql-conf";

  public static final String CONF_PARAM = "xml-conf";

  public static final String REMOTE_GROUP_ELEMENT = "remote-graph-group";

  public static final String REMOTE_GRAPH_MANAGER_ELEMENT = "remote-graph-manager";

  protected static final CLOption[] options = { new CLOption(CONF_PARAM, 'x', true, true, "file",
      "the XML configuration file") };

  private final I18n i18n = I18n.getInstance(GraphManager.class);

  private static final Logger log = Logger.getLogger(ClientRemoteGraphFacade.class);

  private Hashtable graphSetHash; // graph set hashmap.

  private GraphManagerRemoteInterface remoteGraphMan; // GraphManager stub
                                                      // pointer.

  /**
   * Constructor
   * 
   * @param jndiName
   *          jndi name.
   * @param config
   *          graph group configuration
   */
  public ClientRemoteGraphFacade(String jndiName, String dwareName, Element config)
      throws RemoteException {

    // init hash of graph set
    graphSetHash = new Hashtable();

    // Graph Manager configuration
    Element conf = configureRemoteFacade(config, dwareName);
    if (conf != null)
      registerGraphManager(jndiName, conf);
    else
      log.warn(i18n.getString("remoteManagerConfigErr"));
  }

  /*
   * Return graph manager configuration on a DOM object.
   */
  private final Element configureRemoteFacade(Element config, String dwareName) {
    Element conf;
    try {
      conf = DOMUtils.getElement(config, DataAware.DWARE_NAMESPACE_URI, dwareName, true);
      conf = DOMUtils.getElement(conf, DataAware.DWARE_NAMESPACE_URI,
                                 REMOTE_GRAPH_MANAGER_ELEMENT, true);
    } catch (IllegalArgumentException e) {
      log.warn(i18n.getString("remoteXMLConfigErr"));
      conf = null;
    }
    return conf;
  }

  /*
   * Register this client remote graph facade to the graph manager. @param
   * jndiName jndi name. @param config DOM object with graph group configuration
   */
  private void registerGraphManager(String jndiName, Element config) throws RemoteException {

    try {
      // get remote reference
      InitialContext jndiContext = new InitialContext();
      remoteGraphMan = (GraphManagerRemoteInterface) jndiContext.lookup(jndiName);
      // GraphManagerRemoteInterface remoteGraphMan =
      // (GraphManagerRemoteInterface)Naming.lookup(jndiName);

      // register all remote graph groups
      NodeList list = DOMUtils.getElements(config, DataAware.DWARE_NAMESPACE_URI,
                                           REMOTE_GROUP_ELEMENT);
      for (int i = 0; i < list.getLength(); i++) {
        remoteGraphMan.registerClientRemoteGraph(this, (Element) list.item(i));
      }
    } catch (Exception e) {
      log.warn(i18n.getString("remoteManagerRegisterErr") + " : " + e.getMessage());
    }
  }

  /**
   * This method is called by server to configure the graph group.
   * 
   * @param name
   *          graph set name.
   * @param config
   *          DOM object with graph group configuration
   * @param max
   *          maximum number of graph this set can have.
   * @param callbackObj
   *          callback object.
   */
  public void configureGraph(String name,
                             Element config,
                             int max,
                             RemoteGraphGroupInterface callbackObj) throws RemoteException {

    log.info(i18n.getString("settingGraphs", name));

    // if the graph group was already configured throw an exception
    if (graphSetHash.get(name) != null)
      throw new RemoteException("Graph set has already been created.");

    // create graph set and put in hash
    GraphSetObj gobj = new GraphSetObj(name, config, max, callbackObj, log);
    graphSetHash.put(name, gobj);
  }

  /**
   * This method is called by server to REconfigures the filters inside the
   * graphs of this group.
   * 
   * @param name
   *          graph set name.
   * @param filterName
   *          the name of the filter that will be reconfigured.
   * @param config
   *          the DOM tree that has the filter configuration to be applied.
   */
  public void configureFilter(String name, String filterName, Element config)
      throws GraphException, FilterException, RemoteException {

    log.info(i18n.getString("settingFilters", filterName, name));

    if (graphSetHash.get(name) == null)
      throw new RemoteException("This graph set was not configured.");

    ((GraphSetObj) graphSetHash.get(name)).reconfigGraphFilter(filterName, config);
  }

  /**
   * Shutdown graphs from the set given by the name passed.
   * 
   * @param name
   *          graph set name.
   */
  public void shutdown(String name) throws RemoteException {

    if (graphSetHash.get(name) == null)
      throw new RemoteException("This graph set was not configured.");

    ((GraphSetObj) graphSetHash.get(name)).shutdownGraphs();
  }

  /**
   * This method is called by server when some graph group will be down.
   */
  public void notifyServerDown(String name) throws RemoteException {
    shutdown(name);
  }

  /**
   * Forward a request to be processed by a graph set.
   * 
   * @param name
   *          graph set name.
   * @param request
   *          request to be processed.
   */
  public void process(String name, Request request) throws ManagerException, RemoteException {

    if (log.isDebugEnabled())
      log.debug(name + " : start process request " + request.getId() + " (weight="
                + request.getWeight() + ")");

    if (graphSetHash.get(name) == null)
      throw new ManagerException("This graph set was not configured.");

    ((GraphSetObj) graphSetHash.get(name)).process(request);
  }

  /**
   * Method used to check client connection by the server.
   */
  public void ping() throws RemoteException {

    log.debug("Server checks if i'm up.");

  }

  /**
   * Stop to process requests. Warn the server to avoid him to forward him
   * requests.
   * 
   * @param kill
   *          if true, the graph group will stop after finishing to process the
   *          request of that moment.
   * @param wait
   *          if true, this method will block until the graph group have
   *          finished.
   */
  public void terminate(boolean kill, boolean wait) {
    // terminate all graph sets.
    for (Iterator it = graphSetHash.values().iterator(); it.hasNext();) {
      try {
        GraphSetObj gobj = (GraphSetObj) it.next();
        // warn server that it is finishing
        remoteGraphMan.unregisterClientRemoteGraph(gobj.getName(), kill, wait);
      } catch (RemoteException re) {
        log.warn(i18n.getString("remoteTerminateErr") + " : " + re.getMessage());
      }
    }
  }

  /*
   * Called when connection error occurs
   */
  private void handleConnectionError(String errorMsg) {
    log.fatal(i18n.getString("remoteSrvDownErr") + " : " + errorMsg);
    for (Iterator it = graphSetHash.values().iterator(); it.hasNext();) {
      GraphSetObj gobj = (GraphSetObj) it.next();
      gobj.shutdownGraphs();
    }
  }

  /**
   * Starts a facade object.
   */
  public static void main(String[] args) {

    try {
      OptionsParser parser = new OptionsParser(options, ClientRemoteGraphFacade.class,
          "ClientRemoteGraphFacade Test", true);
      parser.parse(args);

      // Parses the XML config file
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      File configFile = new File(OptionsParser.getOptionValue(CONF_PARAM));

      Element baseElem = dbf.newDocumentBuilder().parse(configFile).getDocumentElement();

      ConfigUtils.configureLog4J(baseElem);

      ClientRemoteGraphFacade rgf = new ClientRemoteGraphFacade(GraphManager.JNDI_NAME, "teste",
          baseElem);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /*
   * Inner class for GraphSet objects.
   */
  class GraphSetObj implements FinishListener {

    private final SyncStack freeGraphStack = new SyncStack();

    private final Set graphSet = new HashSet();

    private volatile int max;

    private volatile int count = 0;

    private String name; // graph set name.

    private Element graphConfig; // graph configuration.

    private Logger log;

    private RemoteGraphGroupInterface callbackObj; // callback object.

    /**
     * Constructor.
     * 
     * @param _name
     *          name of graph set.
     * @param _graphConfig
     *          DOM object with graph configuration.
     * @param _max
     *          maximum number of graph this set can have.
     * @param _callbackObj
     *          callback object.
     * @param _log
     *          Log4j object.
     */
    public GraphSetObj(String _name, Element _graphConfig, int _max,
                       RemoteGraphGroupInterface _callbackObj, Logger _log) {
      name = _name;
      max = _max;
      log = _log;
      graphConfig = _graphConfig;
      callbackObj = _callbackObj;
    }

    /**
     * Return object name.
     */
    public String getName() {
      return name;
    }

    /**
     * Check if the set can be expanded, in this case it create a new graph and
     * add it to the set.
     */
    private final Graph checkExpandSet() throws ManagerException {
      if (graphSet.size() < max) {
        // Creates a new graph to process this request
        try {
          Graph graph;
          synchronized (graphSet) {
            String graphName = "(" + name + ") #" + (++count);
            graph = new Graph(graphName, graphConfig);
            graphSet.add(graph);
          }
          return graph;
        } catch (GraphException e1) {
          throw new ManagerException(e1);
        }
      }
      throw new ManagerException("There is no graph avaiable and all of them were created.");
    }

    /**
     * Reconfigures graphs filters.
     * 
     * @param filterName
     *          the name of the filter that will be reconfigured.
     * @param config
     *          the DOM tree that has the filter configuration to be applied.
     */
    public void reconfigGraphFilter(String filterName, Element config) throws GraphException,
        FilterException {

      synchronized (this.graphSet) {
        for (Iterator it = this.graphSet.iterator(); it.hasNext();)
          ((Graph) it.next()).configureFilter(filterName, config);
        graphConfig = config;
      }
    }

    /**
     * Shutdown graphs from the set.
     */
    public void shutdownGraphs() {
      synchronized (this.graphSet) {
        // Waits for the thread's graphs.
        for (Iterator it = this.graphSet.iterator(); it.hasNext();) {
          Graph g = (Graph) it.next();
          if (log.isDebugEnabled())
            log.debug("Shutdown graph : " + g);
          g.shutdown(true);
        }

        // Clear the free graph stack, because we don't have any free
        // graph now.
        this.freeGraphStack.clear();
        this.graphSet.clear();
      }
    }

    /**
     * Process a request. When the request arrives here I know that there is
     * some graph avaiable or the graph hasn't been created yet, and can be
     * created now that it's needed.
     * 
     * @param request
     *          request to be processed.
     */
    public final void process(Request request) throws ManagerException {

      if (log.isDebugEnabled())
        log.debug("graph group process -> request " + request.getId());

      Graph graph = null;
      do {
        try {
          synchronized (graphSet) {
            graph = (Graph) this.freeGraphStack.get();
          }
        } catch (NoSuchElementException e) {
          graph = checkExpandSet();
        }
      } while (graph == null);

      if (log.isDebugEnabled())
        log.debug("there is a free graph, process request wei=" + request.getWeight());

      graph.process(request, this); // this object is the listener
    }

    /**
     * When the graph finishes to process a request, it calls this method after.
     * Method defined by FinishListener interface.
     * 
     * @param graph
     *          the graph that is going to be available.
     * @param request
     *          the request that was processed.
     * @param error
     *          if null, the request was processed successfully. If not, it
     *          represents the problem that ocurred.
     */
    public void graphFinished(Graph graph, Request request, Throwable error, Date time)
        throws Exception {

      if (log.isDebugEnabled())
        log.debug("graph group finish -> request " + request.getId());
      // forward to server
      try {
        callbackObj.remoteGraphFinished(request, graph.toString(), error, time);
      } catch (MarshalException me) {
        handleConnectionError(me.getMessage());
      }

      // synchronized (graphSet) {
      // manage set of graphs
      if (graphSet.contains(graph)) {
        freeGraphStack.put(graph);
        if (log.isDebugEnabled())
          log
              .debug("Putting the graph '" + graph
                     + "' in the free graph stack. Number of free graphs = "
                     + freeGraphStack.size());
      } else
        throw new IllegalArgumentException(i18n.getString("graphDoesNotExist", graph, name));
      // }
    }

    /**
     * When the graph will commit a request processed, it calls this method
     * before.
     */
    public void graphCommiting(Graph graph, Request request) throws Exception {

      try {
        callbackObj.remoteGraphCommiting(request, graph.toString());
      } catch (MarshalException me) {
        handleConnectionError(me.getMessage());
      }
    }

    /**
     * When the graph will rollback a request processed, it calls this method
     * before.
     */
    public void graphRollingBack(Graph graph, Request request, Throwable error) throws Exception {

      try {
        callbackObj.remoteGraphRollingBack(request, graph.toString(), error);
      } catch (MarshalException me) {
        handleConnectionError(me.getMessage());
      }
    }
  }
}
