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
package br.com.auster.dware;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.io.IOUtils;
import br.com.auster.common.jmx.AusterManagementServices;
import br.com.auster.common.security.ResourceReady;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FinishListener;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.GraphManager;

/**
 * This is the main class to use and configure DataAware. It encapsulates all
 * the code used to initialize the engine (log4j configuration, Graph
 * configuration and Graph Manager initialization).
 * 
 * This class is intended to be the most basic way to use DataAware. Subclasses
 * may specialize it for other ends, like command line reports, web reports,
 * directory listener for file requests on demand, EJBs, etc.
 * 
 * @version $Id: DataAware.java 272 2006-11-30 20:08:39Z rbarone $
 */
public class DataAware {

  protected static final String VERSION = "DataAware v1.8.0";

  // The XML config constants
  public static final String LOG4J_NAMESPACE_URI = "http://jakarta.apache.org/log4j/";

  public static final String DWARE_NAMESPACE_URI = "http://www.auster.com.br/dware/";

  public static final String CONFIGURATION_ELEMENT = "configuration";

  public static final String NAME_ATTR = "name";

  public static final String CLASS_NAME_ATTR = "class-name";

  public static final String GRAPH_MANAGER_ELEMENT = "graph-manager";

  public static final String GRAPH_ELEMENT = "graph-design";
  
  public static final String STARTUP_LISTENER_ELEMENT = "startup-listener";

  // Instance attributes
  protected static final Logger log = Logger.getLogger(DataAware.class);

  private final I18n i18n = I18n.getInstance(DataAware.class);

  protected final GraphManager graphManager = new GraphManager();

  protected Element graphConf;

  protected String name;

  private final ResourceReady license;
  
  private ArrayList listeners;
  
  //JMX Variables
  private AtomicLong jEnqueuedRequests = new AtomicLong(0);
  private final long startupTime;

  /**
   * Creates an instance of DataAware, for high performance processing of
   * serveral requests in a well defined graph of filters.
   * 
   * @param configRoot
   *          the root of DOM tree that contains the configuration for
   *          DataAware. This configuration must be inside the namespace
   *          "http://www.auster.com.br/dware/".
   * @throws Exception
   *           if some fatal error is found while reading the configuration.
   */
  public DataAware(Element configRoot) throws Exception {
  	this.license = createLicense();
  	
    this.startupTime = System.currentTimeMillis();
    // Initializes StartuptListeners
    NodeList listenerNodes = DOMUtils.getElements(configRoot, STARTUP_LISTENER_ELEMENT);
    this.listeners = new ArrayList(listenerNodes.getLength());
    for (int i = 0; i < listenerNodes.getLength(); i++) {
      Element listenerNode = (Element) listenerNodes.item(i);
      if (listenerNode != null) {
        String className = DOMUtils.getAttribute(listenerNode, CLASS_NAME_ATTR, true);
        log.debug("Instantiating the startup listener '" + className + "'.");
        Class[] c = { Element.class };
        Object[] o = { DOMUtils.getElement(listenerNode, Graph.CONFIG_ELEMENT, false) };
        StartupListener listener = (StartupListener) 
          Class.forName(className).getConstructor(c).newInstance(o);
        this.listeners.add(listener);
        log.info(i18n.getString("listenerReady", className));
      }
    }
    
    // Notifying Listeners that we are going to configure Data-Aware
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      ((StartupListener) it.next()).beforeConfig(this, configRoot);
    }
    
    // Configure Data-Aware
    this.configure(configRoot);
    
    // Notifying Listeners that we have finished configuring Data-Aware
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      ((StartupListener) it.next()).afterConfig(this, this.graphConf);
    }
    
    //Configure Management
    AusterManagementServices.registerMBean(true, configRoot, this.getClass(), this);
  }

  /**
   * Configures DataAware using the given DOM tree. This method may be called
   * while the DataAware engine is running. If so, some requests will be
   * processed using the old configuration, some others with the new one. Also,
   * the graph manager will be reconfigured.
   * 
   * @param configRoot
   *          the root of the DataAware XML configuration as a DOM tree.
   */
  public synchronized void configure(Element configRoot) throws Exception {
  	checkMe();
  	
    // DataAware name
    this.name = DOMUtils.getAttribute(configRoot, NAME_ATTR, true);

    // Graph configuration
    Element conf = DOMUtils.getElement(configRoot, DWARE_NAMESPACE_URI, GRAPH_ELEMENT, true);
    this.configureGraph(conf);

    // Graph Manager configuration
    try {
      conf = DOMUtils.getElement(configRoot, DWARE_NAMESPACE_URI, GRAPH_MANAGER_ELEMENT, true);
    } catch (IllegalArgumentException e) {
      conf = null;
    }
    if (conf != null)
      this.configureGraphManager(conf);
    else
      log.warn(i18n.getString("managerNotConfigured"));
  }
  
  private final ResourceReady createLicense() {
    ObjectInputStream ois = null;
    try {
      InputStream fis = IOUtils.openFileForRead(ResourceReady.LICENSE_FILE);
      ois = new ObjectInputStream(new GZIPInputStream(fis));
      return (ResourceReady) ois.readObject();
    } catch (FileNotFoundException e) {
      throw new IllegalAccessError(i18n.getString("resourceMissing", 
                                                  ResourceReady.LICENSE_FILE.getPath()));
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalAccessError("See previous exception.");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new IllegalAccessError("See previous exception.");
    } finally {
      try { 
        if (ois != null) {
          ois.close();
        }
      } catch (IOException e) {}
    }
  }

  protected final synchronized void checkMe() {

    ObjectInputStream ois = null;
    try {
      InputStream fis = IOUtils.openFileForRead(ResourceReady.LICENSE_FILE);
      GZIPInputStream gis = new GZIPInputStream(fis);
      ois = new ObjectInputStream(gis);
      Object myObj = ois.readObject();
      ResourceReady rr = (ResourceReady) myObj;
      int rrReturnCode = rr.canRun();
      if (rrReturnCode > 0) {
        throw new IllegalAccessError(
            "You are not Allowed to Run this Application - Security Check Violation #"
                + rrReturnCode);
      }
    } catch (FileNotFoundException e) {
      throw new IllegalAccessError(i18n.getString("resourceMissing", 
                                                  ResourceReady.LICENSE_FILE.getPath()));
    } catch (IOException e) {
      e.printStackTrace();
      throw new IllegalAccessError("See previous exception.");
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      throw new IllegalAccessError("See previous exception.");
    } finally {
      try { 
        if (ois != null) {
          ois.close();
        }
      } catch (IOException e) {}
    }

  }

  public final ResourceReady getLicense() {
    return this.license;
  }

  /**
   * Configures the graphs of this DataAware instance using the given DOM tree.
   * This method may be called while the DataAware engine is running. If so,
   * some requests will be processed using the old configuration, some others
   * with the new one.
   * 
   * @param config
   *          the root of the graph configuration as a DOM tree.
   */
  public void configureGraph(Element config) throws GraphException {
    this.graphConf = config;
    this.graphManager.configureGraph(config);
  }

  /**
   * Configures the graph manager of this DataAware instance using the given DOM
   * tree. This method may be called while the DataAware engine is running.
   * 
   * @param config
   *          the root of the graph manager configuration as a DOM tree.
   * @throws GraphException
   *           if the graph configuration is detected to be incorrect while the
   *           graph manager is instantiating the initial graphs.
   */
  public void configureGraphManager(Element config) throws GraphException {
    this.graphManager.configure(config);
  }

  /**
   * Adds a request to be processed assynchronously by this DataAware instance.
   * 
   * @param request
   *          the request to be processed.
   * @throws IllegalStateException
   *           if the graph manager is down, not configured or does not have any
   *           graphs or groups configured to process a request.
   */
  public void enqueue(Request request) throws IllegalStateException {
    if (graphManager.isCompletelyConfigured()) {
      for (Iterator it = this.listeners.iterator(); it.hasNext();) {
        ((StartupListener) it.next()).beforeEnqueue(this, request);
      }
      
      this.graphManager.enqueue(request);
      this.jEnqueuedRequests.incrementAndGet();
      
      for (Iterator it = this.listeners.iterator(); it.hasNext();) {
        try {
          ((StartupListener) it.next()).afterEnqueue(this, request);
        } catch (Throwable e) {
          log.warn("Error in StartupListener.", e);
        }
      }
    } else {
      throw new IllegalStateException(i18n.getString("managerNotConfigured"));
    }
  }
  
  /**
   * Adds a list of requests to be processed assynchronously by this DataAware instance.
   * 
   * @param requests
   *          the requests to be processed.
   * @throws IllegalStateException
   *           if the graph manager is down, not configured or does not have any
   *           graphs or groups configured to process a request.
   */
  public void enqueue(Collection<Request> requests) throws IllegalStateException {
    if (graphManager.isCompletelyConfigured()) {
      for (Iterator it = this.listeners.iterator(); it.hasNext();) {
        ((StartupListener) it.next()).beforeEnqueue(this, requests);
      }
      
      this.graphManager.enqueue(requests);
      this.jEnqueuedRequests.addAndGet(requests.size());
      
      for (Iterator it = this.listeners.iterator(); it.hasNext();) {
        try {
          ((StartupListener) it.next()).afterEnqueue(this, requests);
        } catch (Throwable e) {
          log.warn("Error in StartupListener.", e);
        }
      }
    } else {
      throw new IllegalStateException(i18n.getString("managerNotConfigured"));
    }
  }

  /**
   * Configures the Log4J. This method is no longer used and is provided here
   * just for backward compatibility.
   * 
   * @param root
   *          the DOM tree root that contains all the configuration, including
   *          the Log4J configuration. Starting at this node, the element
   *          "log4j:configuration" will be searched as the root of the Log4J
   *          configuration.
   * @throws IllegalArgumentException
   *           if could not find a configuration for the Log4J.
   * @deprecated use
   *             {@link br.com.auster.common.util.ConfigUtils#configureLog4J(Element)}
   *             instead.
   * @see br.com.auster.common.util.ConfigUtils ConfigUtils
   */
  public static void configureLog4J(Element root) throws IllegalArgumentException {
    br.com.auster.common.util.ConfigUtils.configureLog4J(root);
  }

  /**
   * Starts to process the request bypassing the queue. It will create a Graph
   * instance, using the XML config specified at instantiation.
   * 
   * @param request
   *          the request to be processed.
   * @throws Throwable
   *           if the request processing was not successfull.
   */
  public void process(Request request) throws Throwable {
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      ((StartupListener) it.next()).beforeEnqueue(this, request);
    }

    final Graph graph = new Graph(this.name, this.graphConf);
    try {
      graph.process(request);
    } finally {
      graph.shutdown(false);
    }
    
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      try {
        ((StartupListener) it.next()).afterEnqueue(this, request);
      } catch (Throwable e) {
        log.warn("Error in StartupListener.", e);
      }
    }
  }

  /**
   * Starts to process the request bypassing the queue. It will create a Graph
   * instance, using the XML config specified at instantiation. This method will
   * not block.
   * 
   * @param request
   *          the request to be processed.
   * @param listener
   *          an object that will be called when the graph finishes to process
   *          the request.
   * @throws GraphException
   *           if the graph configuration is invalid.
   */
  public void process(Request request, FinishListener listener) throws GraphException {
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      ((StartupListener) it.next()).beforeEnqueue(this, request);
    }
    
    final Graph graph = new Graph(this.name, this.graphConf);
    try {
      graph.process(request, listener);
    } finally {
      graph.shutdown(false);
    }
    
    for (Iterator it = this.listeners.iterator(); it.hasNext();) {
      try {
        ((StartupListener) it.next()).afterEnqueue(this, request);
      } catch (Throwable e) {
        log.warn("Error in StartupListener.", e);
      }
    }
  }

  /**
   * Shuts down all the graph processors that were instantiated.
   * 
   * @param wait
   *          if true, this method will block until all the graphs have
   *          processed all the requests queued and stopped.
   */
  public void shutdown(boolean wait) {
    this.graphManager.shutdown(false, wait);
  }

  public GraphManager getGraphManager() {
     return this.graphManager;
  }
  //JMX Methods
  public long getEnqueuedRequests() {
     return this.jEnqueuedRequests.get();
  }
  /**
   * Gets the version of the DataAware engine.
   */
  public static String getVersion() {
    return VERSION;
  }
  
  public long getStartupTime() {
     return this.startupTime;
  }
}
