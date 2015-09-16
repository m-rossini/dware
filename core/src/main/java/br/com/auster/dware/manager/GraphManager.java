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
package br.com.auster.dware.manager;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;
import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.jmx.AusterManagementServices;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.checkpoint.AbstractCheckpoint;
import br.com.auster.dware.manager.remote.ClientRemoteGraphInterface;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * This is the main processing manager. It manages all the graph groups,
 * controls the checkpoint/restart implementation and contains all the requests
 * that were not hold by any graph group yet, in a common queue.
 * 
 * @version $Id: GraphManager.java 357 2008-03-07 19:19:15Z lmorozow $
 */
public class GraphManager {

  /**
   * {@value}
   */
  public static final String LOCAL_GROUP_ELEMENT = "local-graph-group";
  /**
   * {@value}
   */
  public static final String CHECKPOINT_ELEMENT = "checkpoint";
  /**
   * {@value}
   */
  public static final String NAME_ATTR = "name";
  /**
   * {@value}
   */
  public static final String CLASS_NAME_ATTR = "class-name";
  /**
   * {@value}
   */
  private static final String REQUEST_FORWARDER = "request-forwarder";
  /**
   * {@value}
   */
  public static final String JNDI_NAME = "GraphManagerService";
  /**
   * {@value}
   */
  public static final String QUEUE_PROCESSED_LISTENERS_ELEMENT = "queue-processed-listeners";
  /**
   * {@value}
   */
  public static final String LISTENER_ELEMENT = "listener";

  private static final Logger log = Logger.getLogger(GraphManager.class);

  //#####################
  // Instance attributes
  //#####################
  private final I18n i18n = I18n.getInstance(GraphManager.class);

  private final DataAwareManagerMediator managerMediator = new DataAwareManagerWishMediator();

  private ReqForwarderInterface requestPriorQueue = null;

  private AbstractCheckpoint chkPt;

  private final Map groupMap = new HashMap();

  private Element graphConfig = null;

  // object for remote connection.
  private GraphManagerConnectionObj remConnObj;

  // if the graph manager is shutting down.
  private boolean shuttingDown;

  // if this object and graph were completely configured
  private boolean isConfigured, isGraphConfigured;
  
  // queue-processed listener
  private final List<QueueProcessedListener> queueProcessedListeners = new ArrayList<QueueProcessedListener>();
  
  // thread-pool executor for queue-processed listeners
  private final ExecutorService queueProcessedExecutor = Executors.newCachedThreadPool();

  
  /**
   * Simple constructor for RMI stub creation.
   */
  public GraphManager() {
    super();
    shuttingDown = false;
    isConfigured = false;
    isGraphConfigured = false;
    managerMediator.registerGraphManager(this);
  }

  /**
   * Reconfigures this graph manager. This may shutdown all the graph groups and
   * start them again.
   * 
   * @throws IllegalArgumentException
   *           if the configuration given is not acceptable.
   * @throws GraphException
   *           if the graph configuration passed to the graph group is
   *           incorrect.
   */
  public void configure(Element config) throws GraphException {
    log.info(i18n.getString("configuringManager"));
    
    //Configure Management
    AusterManagementServices.registerMBean(true, config, this.getClass(), this);

    // Configures RequestForwarder
    Element configRF = DOMUtils.getElement(config, REQUEST_FORWARDER, false);
    if (configRF == null) {
      this.requestPriorQueue = new PriorityQueueReqForwarder(this.managerMediator);
    } else {
      try {
        String className = DOMUtils.getAttribute(configRF, CLASS_NAME_ATTR, true);
        Class[] c = { DataAwareManagerMediator.class };
        Object[] o = { this.managerMediator };
        this.requestPriorQueue = (ReqForwarderInterface) 
            Class.forName(className).getConstructor(c).newInstance(o);
        log.info("Request Forwarder configured as " + className);
      } catch (Exception e) {
        String message = "Request Forwarder could not be created.";
        log.fatal(message, e);
        throw new GraphException(message, e);
      }
    }
    this.requestPriorQueue.configure(configRF);
    
    // Configures the local graph groups.
    NodeList list = DOMUtils.getElements(config, 
                                         DataAware.DWARE_NAMESPACE_URI,
                                         LOCAL_GROUP_ELEMENT);
    for (int i = 0; i < list.getLength(); i++) {
      this.addLocalGroup((Element) list.item(i));
    }

    // configure checkpoint
    try {
      Element checkPointConf = DOMUtils.getElement(config, CHECKPOINT_ELEMENT, true);
      createCheckpointInstance(checkPointConf);
    } catch (IllegalArgumentException e) {
      log.warn(i18n.getString("checkpointNotConfigured"));
    }
    
    // configure queue-processed listeners
    try {
      Element listenersElt = DOMUtils.getElement(config, QUEUE_PROCESSED_LISTENERS_ELEMENT, false);
      if (listenersElt != null) {
        list = DOMUtils.getElements(listenersElt, LISTENER_ELEMENT);
        for (int i = 0; i < list.getLength(); i++) {
          Element listenerElt = (Element) list.item(i);
          String className = DOMUtils.getAttribute(listenerElt, CLASS_NAME_ATTR, true);
          QueueProcessedListener listener = (QueueProcessedListener) Class.forName(className).newInstance();
          listener.init(listenerElt);
          this.queueProcessedListeners.add(listener);
          log.info(i18n.getString("queueListenerConfigured", className));
        }
      }
    } catch (Exception e) {
      log.error(i18n.getString("queueListenersNotConfigured"), e);
      this.queueProcessedListeners.clear();
    }
    
    log.info(i18n.getString("managerConfigured"));

    isConfigured = true;
  }

  /**
   * Tells if this object was completely configured.
   */
  public boolean isCompletelyConfigured() {
    return isConfigured && isGraphConfigured;
  }

  private void createCheckpointInstance(Element config) {
    try {
      String className = config.getAttribute(CLASS_NAME_ATTR);

      Class[] c = { Element.class, DataAwareManagerMediator.class };
      Object[] o = { config, managerMediator };
      chkPt = (AbstractCheckpoint) Class.forName(className).getConstructor(c).newInstance(o);
    } catch (Exception e) {
      log.error(i18n.getString("checkpointCreationError"), e);
    }
  }

  /**
   * Adds a local graph group to the graph manager.
   * 
   * @param config
   *          the configuration, as a DOM tree, of this group to be created.
   * @throws IllegalArgumentException
   *           if the configuration given is not acceptable.
   * @throws GraphException
   *           if the graph configuration passed to the graph group is
   *           incorrect.
   */
  public void addLocalGroup(Element config) throws GraphException {
    String name = DOMUtils.getAttribute(config, NAME_ATTR, true);
    log.info(i18n.getString("creatingLocalGraphGroup", name));
    GraphGroup group = null;
    synchronized (this.groupMap) {
      if (this.groupMap.containsKey(name))
        throw new IllegalArgumentException(i18n.getString("groupAlreadyExists", name));

      try {
        Element graphConfigClone = (Element) this.graphConfig.cloneNode(true);
		group = new PriorityQueueWishGraphGroup(name, managerMediator, config, graphConfigClone);
      } catch (ManagerException me) {
        throw new GraphException(me);
      }
      this.groupMap.put(name, group);
    }
    group.start();
  }

  /**
   * Given a configuration, creates an instance of the GraphGroup subclass
   * specified in the "class-name" attribute of it. Its constructor must accept
   * some parameters.
   * 
   * @param config
   *          the DOM tree corresponding to the configuration of the GraphGroup
   *          subclass to be instantiated.
   * @return a instance based on the information of the given config element.
   * @throws Exception
   *           if some error occurs while instantiating the class.
   */
  // protected final GraphGroup getGraphGroupInstance(Element config)
  // throws Exception
  // {
  // String className = getAttribute(config, CLASS_NAME_ATTR);
  // Class[] c = {String.class, Element.class};
  // Object[] o = {groupName, config};
  // return (GraphGroup)
  // Class.forName(className).getConstructor(c).newInstance(o);
  // }
  /**
   * Reconfigures the graph groups.
   * 
   * @throws NoSuchElementException
   *           if the group name specified does not exist.
   */
  public void configureGroup(String groupName, Element config) throws NoSuchElementException {
    synchronized (this.groupMap) {
      if (!this.groupMap.containsKey(groupName))
        throw new NoSuchElementException(i18n.getString("groupDoesNotExist", groupName));

      GraphGroup group = (GraphGroup) this.groupMap.get(groupName);
      log.info(i18n.getString("configuringGroup", groupName));
      group.configure(config);
    }
  }

  /**
   * Reconfigures the graphs inside their groups. This may shutdown these
   * graphs, but not the groups.
   * 
   * @throws GraphException
   *           if some error ocurrs while reconfiguring the graphs.
   */
  public void configureGraph(Element config) throws GraphException {

    synchronized (this.groupMap) {
      Iterator it = this.groupMap.values().iterator();
      while (it.hasNext()) {
        GraphGroup group = (GraphGroup) it.next();
        group.configureGraph(config);
      }
      this.graphConfig = config;
    }

    // initialize listener for remote graph groups.
    try {
      remConnObj = new GraphManagerConnectionObj(null, null);
    } catch (RemoteException re) {
      log.warn(i18n.getString("remoteManagerServiceErr"));
      log.debug(re.getMessage(), re);
    }

    isGraphConfigured = true;
  }

  /**
   * Reconfigures the filters inside the graphs in their groups.
   * 
   * @throws GraphException
   *           if the graph configuration does not have a filter with the give
   *           name.
   * @throws FilterException
   *           if some error ocurrs while reconfiguring the filters.
   */
  public void configureFilter(String filterName, Element config) throws GraphException,
      FilterException {
    synchronized (this.groupMap) {
      Iterator it = this.groupMap.values().iterator();
      while (it.hasNext()) {
        GraphGroup group = (GraphGroup) it.next();
        group.configureFilter(filterName, config);
      }
    }
  }

  /**
   * Put the request in the queue to be processed.
   * 
   * @throws IllegalStateException
   *           if the manager is down, not configured or does not have any
   *           graphs or groups configured to process a request.
   */
  public void enqueue(Request request) {
    // This graph manager will not accept requests if it does not
    // have graph groups to process them.
    if (this.groupMap.isEmpty()) {
      throw new IllegalStateException(i18n.getString("cantEnqueueManagerDown", request));
    }
    requestPriorQueue.addNewReq(request);
  }
  
  /**
   * Put the request in the queue to be processed. This method differs from enqueue
   * because it signals that the request is being requeued, that is, it was
   * previously enqueued and now (probably because of an error) it will be
   * requeued for a retry. 
   * 
   * @throws IllegalStateException
   *           if the manager is down, not configured or does not have any
   *           graphs or groups configured to process a request.
   */
  public void requeue(Request request) {
    // This graph manager will not accept requests if it does not
    // have graph groups to process them.
    if (this.groupMap.isEmpty()) {
      throw new IllegalStateException(i18n.getString("cantEnqueueManagerDown", request));
    }
    requestPriorQueue.addRetryReq(request);
  }
  
  /**
   * Put a collection of requests in the queue to be processed. This method will
   * block queue consuming until all the requests have been enqueued, which is
   * important to guarantee that graphs will start processing these requests
   * only after the queue is filled.
   * 
   * <p>This method is faster that a series of calls to {@link #enqueue(Request)}
   * for enqueueing a list of requests.
   * 
   * @throws IllegalStateException
   *           if the manager is down, not configured or does not have any
   *           graphs or groups configured to process a request.
   */
  public void enqueue(Collection<Request> requests) {
    // This graph manager will not accept requests if it does not
    // have graph groups to process them.
    if (this.groupMap.isEmpty()) {
      throw new IllegalStateException(i18n.getString("cantEnqueueManagerDown", requests));
    }
    requestPriorQueue.addNewReqs(requests);
  }
  
  /**
   * Notifies all configured listeners that the queue for a particular
   * transactionId was fully processed.
   * 
   * @param transactionId
   *          The transactionId of the queue that is processed.
   */
  public void queueProcessed(String transactionId, int size) {
    for (QueueProcessedListener listener : this.queueProcessedListeners) {
      this.queueProcessedExecutor.execute(new QueueProcessedNotifier(listener, 
                                                                     transactionId,
                                                                     size));
    }
  }

  /**
   * Shutdown the graph groups. These groups will stop after the request queue
   * is empty.
   * 
   * @param kill
   *          if true, the graph groups will stop after finishing to process the
   *          requests of that moment. They will not process all the requests
   *          enqueued.
   * @param wait
   *          if true, this method will block until all the graph groups have
   *          finished.
   */
  public void shutdown(boolean kill, boolean wait) {
    shuttingDown = true;

    log.warn(i18n.getString("managerGoingDown"));
    synchronized (this.groupMap) {
      // warn mediator
      this.managerMediator.shutdown();
      
      // warn request forwarder
      this.requestPriorQueue.shutdown();

      // Stops all the graph groups.
      Iterator it = this.groupMap.values().iterator();
      while (it.hasNext()) {
        GraphGroup group = (GraphGroup) it.next();
        group.shutdown(false);
      }

      if (wait) {
        it = this.groupMap.values().iterator();
        while (it.hasNext()) {
          GraphGroup group = (GraphGroup) it.next();
          try {
            group.join();
          } catch (InterruptedException e) {
            log.warn(i18n.getString("gotInterruption"), e);
          }
        }
        log.warn(i18n.getString("managerDown"));
      }

      this.groupMap.clear();
      
      if (remConnObj != null)
        remConnObj.shutdown();
    }
    
    if (this.chkPt != null) {
      this.chkPt.shutdown();
    }
    
    if (wait) {
      this.queueProcessedExecutor.shutdown();
      while (!this.queueProcessedExecutor.isTerminated()) {
        try {
          this.queueProcessedExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
          this.queueProcessedExecutor.shutdownNow();
        }
      }
    } else {
      this.queueProcessedExecutor.shutdownNow();
    }
    
  }

  /**
   * Shutdown a graph group. This group will stop after processing its last
   * request.
   * 
   * @param groupName
   *          the graph group to be stopped.
   * @param kill
   *          if true, the graph group will stop after finishing to process the
   *          request of that moment.
   * @param wait
   *          if true, this method will block until the graph group have
   *          finished.
   * @throws NoSuchElementException
   *           if the given group name does not exist in the group list.
   */
  public void shutdownGroup(String groupName, boolean kill, boolean wait) {
    GraphGroup group = unloadGraphGroup(groupName);
    if (kill) {
      group.kill(wait);
    } else {
      group.shutdown(wait);
    }
  }
  
  /**
   * Returns if this graph manager's shutdown method has been called.
   * 
   * @return true if this graph manager's shutdown method has been called, false
   *         otherwise;
   */
  public boolean isShuttingDown() {
    return this.shuttingDown;
  }

  /**
   * Unloads some graph group from graph manager.
   * 
   * @param groupName
   *          the graph group to be stopped.
   */
  public GraphGroup unloadGraphGroup(String groupName) {
    GraphGroup group;
    synchronized (this.groupMap) {
      if (!this.groupMap.containsKey(groupName))
        throw new NoSuchElementException(i18n.getString("groupDoesNotExist", groupName));
      group = (GraphGroup) this.groupMap.remove(groupName);
    }
    return group;
  }
  //JMX Methods
  public Collection getGroups() {
     return this.groupMap.values();
  }
  public List getGroupNames() {     
     List retValue = new ArrayList();
     for (Iterator itr = this.groupMap.keySet().iterator(); itr.hasNext();) {
        //retValue += (String) itr.next() + "|";
        retValue.add((String) itr.next());
     }
     return retValue;
  }
  
  public JMXGraphGroupCounter getJMXCounter() {
    return this.managerMediator.getJMXCounters();
  }
  

  /*
   * Object that manages remote graph group connections.
   */
  class GraphManagerConnectionObj extends PortableRemoteObject implements
      GraphManagerRemoteInterface {

    /**
     * Constructor.
     * 
     * @param jNamingFactIni
     *          <code>java.naming.factory.initial</code> property
     * @param jNamingProvURL
     *          <code>java.naming.provider.url</code> property
     */
    public GraphManagerConnectionObj(String jNamingFactIni, String jNamingProvURL)
        throws RemoteException {

      if (jNamingFactIni != null)
        System.setProperty("java.naming.factory.initial", jNamingFactIni);
      if (jNamingProvURL != null)
        System.setProperty("java.naming.provider.url", jNamingProvURL);

      // initialize listener for remote graph groups.
      try {
        InitialContext jndiContext = new InitialContext();
        jndiContext.rebind(JNDI_NAME, this);
      } catch (Exception e) {
        log.info(i18n.getString("remoteManagerServiceErr"));
      }
    }

    /**
     * This method instanciates a new RemoteGraphGroup to process requests. It
     * must be called by a remote ClientRemoteGraphFacade object that will
     * process the requests remotely.
     * 
     * @param remoteGraph
     *          instance of a client remote graph group.
     * @param config
     *          the configuration, as a DOM tree, of this remote group to be
     *          created.
     */
    public void registerClientRemoteGraph(ClientRemoteGraphInterface remoteGraph, Element config)
        throws RemoteException {

      String name = DOMUtils.getAttribute(config, NAME_ATTR, true);
      log.info(i18n.getString("creatingRemoteGraphGroup", name));

      if (shuttingDown)
        throw new RemoteException(i18n.getString("remoteConnErr", name));

      GraphGroup group = null;
      synchronized (groupMap) {

        if (groupMap.containsKey(name))
          throw new RemoteException(i18n.getString("groupAlreadyExists", name));

        try {
          group = new PriorityQueueWishRemoteGraphGroup(name, managerMediator, config, graphConfig,
              remoteGraph);
          groupMap.put(name, group);
        } catch (Exception me) {
          throw new RemoteException(me.getMessage());
        }
      }
      group.start();
    }

    /**
     * Some remote graph group stop to act. This graph group must be take out of
     * the avaiable graph groups.
     * 
     * @param groupName
     *          the graph group to be stopped.
     * @param kill
     *          if true, the graph group will stop after finishing to process
     *          the request of that moment.
     * @param wait
     *          if true, this method will block until the graph group have
     *          finished.
     */
    public void unregisterClientRemoteGraph(String groupName, boolean kill, boolean wait)
        throws RemoteException {
      shutdownGroup(groupName, kill, wait);
    }

    public void shutdown() {
      // Deregisters this server object
      try {
        PortableRemoteObject.unexportObject(this);
      } catch (NoSuchObjectException e) {
      }
    }
  } 

  private class QueueProcessedNotifier implements Runnable {
    private final QueueProcessedListener listener;
    private final String transactionId;
    private final int size;
    public QueueProcessedNotifier(QueueProcessedListener listener, 
                                  String transactionId,
                                  int size) {
      this.listener = listener;
      this.transactionId = transactionId;
      this.size = size;
    }
    public void run() {
      listener.onQueueProcessed(transactionId, size);
    }
  }
  
}
