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

import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.jmx.AusterManagementServices;
import br.com.auster.common.util.I18n;
import br.com.auster.common.util.SyncStack;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.FinishListener;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;

/**
 * This class represents a local graph group. It contains local graphs, that are
 * running in the same virtual machine as this thread.
 * 
 * @version $Id: LocalGraphGroup.java 246 2006-09-18 19:53:34Z framos $
 */
public abstract class LocalGraphGroup extends GraphGroup implements FinishListener {

   public static final String MAX_GRAPH_ATTR = "max-graphs";

   public static final String GRAPHS_PER_PROCESSOR_ATTR = "graphs-per-processor";

   // protected static final String MIN_GRAPH_ATTR = "min-graphs";
   // protected static final String TIMEOUT_ATTR = "timeout";

   private static final Logger log = Logger.getLogger(LocalGraphGroup.class);

   // Instance attributes
   protected final I18n i18n = I18n.getInstance(LocalGraphGroup.class);

   protected final SyncStack freeGraphStack = new SyncStack();

   protected final Set graphSet = new HashSet();

   // protected volatile int min = 0;
   protected volatile int max = 0;

   protected volatile int count = 0;

   // protected volatile long timeout;
   protected Element graphConfig;

   public LocalGraphGroup(String name, DataAwareManagerMediator dwareMediator,
         Element config, Element graphConfig) throws GraphException, ManagerException {
      super(name, dwareMediator);
      this.configure(config);
      this.configureGraph(graphConfig);
      // this.graphConfig = graphConfig;
      // this.configure(config);
      // for (int i = 0; i < this.min; i++) {
      // Graph graph = this.createGraph();
      // this.freeGraphStack.put(graph);
      // }
   }

   /**
    * Configures this graph group.
    * 
    * @param config
    *           the DOM tree that has the configuration to be applied.
    */
   public synchronized void configure(Element config) {
  	 super.configure(config);
      try {
         this.max = DOMUtils.getIntAttribute(config, GRAPHS_PER_PROCESSOR_ATTR, true)
               * Runtime.getRuntime().availableProcessors();
      } catch (IllegalArgumentException e) {
         this.max = DOMUtils.getIntAttribute(config, MAX_GRAPH_ATTR, true);
      }
      log.info(i18n.getString("maxGraphsNumber", this.getName(),
                              Integer.toString(this.max)));
      
      AusterManagementServices.registerMBean(true, config, this.getClass(), this);
      // this.setMinGraphs(XMLUtils.getIntAttribute(config, MIN_GRAPH_ATTR));
      // this.setMaxGraphs(XMLUtils.getIntAttribute(config, MAX_GRAPH_ATTR));
      // this.setTimeout(XMLUtils.getIntAttribute(config, TIMEOUT_ATTR));
   }

   /**
    * Reconfigures the graph inside this group.
    * 
    * @param config
    *           the DOM tree that has the configuration to be applied.
    */
   public void configureGraph(Element config) throws GraphException {
      log.info(i18n.getString("settingGraphs", this.getName()));

      synchronized (this.graphSet) {
         for (Iterator it = this.graphSet.iterator(); it.hasNext();) {
            ((Graph) it.next()).configureGraph(config);
         }
         this.graphConfig = config;
      }
   }

   /**
    * Reconfigures the filters inside the graphs of this group.
    * 
    * @param filterName
    *           the name of the filter that will be reconfigured.
    * @param config
    *           the DOM tree that has the filter configuration to be applied.
    */
   public void configureFilter(String filterName, Element config) throws GraphException,
         FilterException {
      log.info(i18n.getString("settingFilters", filterName, this.getName()));
      synchronized (this.graphSet) {
         for (Iterator it = this.graphSet.iterator(); it.hasNext();) {
            ((Graph) it.next()).configureFilter(filterName, config);
         }
         this.graphConfig = config;
      }
   }

   /**
    * Sets the timeout of the graph threads that this manager has.
    * 
    * @param timeout
    *           the timeout value for the graphs.
    */
   // public synchronized void setTimeout(long timeout) {
   // this.timeout = timeout;
   // // ??????????? Precisa acertar os timeout das threads já ativas.
   // }
   /**
    * Sets the minimum amount of graph threads that this manager will have.
    * 
    * @param quant
    *           the minimum quantity.
    */
   // public synchronized void setMinGraphs(int quant) {
   // if (quant < 0)
   // this.min = 0;
   // else
   // this.min = quant;
   // if (this.min > this.max)
   // this.max = this.min;
   // }
   /**
    * Sets the maximum amount of graph threads that this manager will have.
    * 
    * @param quant
    *           the minimum quantity.
    */
   // public synchronized void setMaxGraphs(int quant) {
   // if (quant < 0)
   // this.max = Integer.MAX_VALUE;
   // else
   // this.max = quant;
   // if (this.max < this.min)
   // this.min = this.max;
   // }
   /**
    * This method is only called when there is at least one graph not working at
    * the moment. This method must pass this request to such graph to process
    * it.
    * 
    * @throws RuntimeException
    *            if some exception was not handled, or if it is wanted to finish
    *            this graph group with an error.
    */
   protected void process(Request request) throws RuntimeException {
      Graph graph = null;
      log.debug("process request wei=" + request.getWeight());
      do {
         try {
            graph = (Graph) this.freeGraphStack.get();
         } catch (NoSuchElementException e) {
            if (this.graphSet.size() < this.max) {
               // Creates a new graph to process this request
               try {
                  graph = createGraph();
               } catch (GraphException e1) {
                  throw new RuntimeException(e1);
               }
            } else {
               // This code must never be reached. Just in case, we take some
               // precaution.
               log.debug("waiting for graph available...");
               this.waitForGraphAvailable(1);
            }
         }
      } while (graph == null); // || !graph.isAlive());

      log.debug("there is a free graph, process request wei=" + request.getWeight());
      graph.process(request, this);
   }

   /**
    * Creates a graph.
    */
   protected Graph createGraph() throws GraphException {
      synchronized (this.graphSet) {
         String graphName = "(" + this.getName() + ") #" + (++count);
         Graph graph = new Graph(graphName, this.graphConfig);
		 // setting the JMX shared counter object 
		 graph.setJMXCounters(this.fmediator.getJMXCounters());
         this.graphSet.add(graph);
         return graph;
      }
   }

   /**
    * When called, this method will tell to all the graphs that they must stop
    * when they finish their jobs.
    */
   protected void shutdownGraphs() {
      synchronized (this.graphSet) {
         // Waits for the thread's graphs.
         for (Iterator it = this.graphSet.iterator(); it.hasNext();) {
            ((Graph) it.next()).shutdown(true);
         }

         // Clear the free graph stack, because we don't have any free
         // graph now.
         this.freeGraphStack.clear();
         this.graphSet.clear();
      }
   }

   /**
    * This method blocks until there is some graph idle.
    * 
    * @param minVal
    *           the minimum number of graphs available that this method will
    *           wait for.
    */
   protected void waitForGraphAvailable(int minVal) {
      final Object graphSync = this.freeGraphStack.getSyncObject();
      boolean done = false;
      while (!done) {
         synchronized (graphSync) {
            // The amount of free graphs are the ones in the free list plus the
            // ones
            // not created yet.
            int freeGraphs = this.freeGraphStack.size()
                  + (this.max - this.graphSet.size());

            // If we have no graphs available, wait for one.
            if (freeGraphs < minVal) {
               log.debug("No graphs available (only " + freeGraphs + "). Waiting...");
               try {
                  graphSync.wait();
               } catch (InterruptedException e) {
               }
            } else {
               done = true;
            }
         }
      }
   }

   /**
    * Called before graph group thread dies.
    */
   protected void finalizeGraphGroup() {
   }

   /**
    * This method is called by the graphs to check if they may finish their
    * operations or if they need to stay waiting for new requests.
    * 
    * @param graph
    *           the graph that is going to be available.
    */
   // public boolean graphTimedOut(ThreadedGraph graph) {
   // if (this.graphSet.contains(graph) && this.graphSet.size() > this.min) {
   // this.graphSet.remove(graph);
   // Object graphSync = this.freeGraphStack.getSyncObject();
   // synchronized(graphSync) {
   // if (this.freeGraphStack.contains(graph))
   // this.freeGraphStack.remove(graph);
   // }
   // return true;
   // } else {
   // return false;
   // }
   // }
   /**
    * This method is called by the graphs to indicate that it is free to process
    * other request, and shows a report about the last request processed. It
    * will also alert the external FinishListener, if it exists.
    * 
    * @param graph
    *           the graph that is going to be available.
    * @param request
    *           the request that was processed.
    * @param error
    *           if null, the request was processed successfully. If not, it
    *           represents the problem that ocurred.
    */
   public void graphFinished(Graph graph, Request request, Throwable error, Date time) {
      if (this.graphSet.contains(graph)) {
         this.freeGraphStack.put(graph);
         log.debug("Putting the graph '" + graph
                  + "' in the free graph stack. Number of free graphs = "
                  + freeGraphStack.size());
      } else {
         throw new IllegalArgumentException(i18n.getString("graphDoesNotExist", graph,
               this.getName()));
      }
      
      // (since Data-Aware 1.5) mandatory call as specified in 
      // GraphGroup.graphFinished(String, Request, Object)
      super.graphFinished(graph.toString(), request, error);
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.manager.GraphGroup#setMaxThread(int)
    */
   public void setMaxThreads(int maxThreads) {
      this.max = maxThreads;      
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.manager.GraphGroup#getMaxThreads()
    */
   public int getMaxThreads() {
      return this.max;
   }

   /**
    * Do Nothing.
    */
   public void graphCommiting(Graph graph, Request request) {
   }

   /**
    * Do Nothing.
    */
   public void graphRollingBack(Graph graph, Request request, Throwable error) {
   }


}
