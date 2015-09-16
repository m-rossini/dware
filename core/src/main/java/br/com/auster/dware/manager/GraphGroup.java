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

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;

/**
 * This class represents a group of graphs to process requests that comes from a
 * linked list. The subclasses must implement how a request is given to a graph
 * to process, how the graphs notify that they are free, how the graphs are
 * created and timed out, etc. <br/> The requests are required given a maximum
 * weight this group currently suports.
 * 
 * @version $Id: GraphGroup.java 272 2006-11-30 20:08:39Z rbarone $
 */
public abstract class GraphGroup extends Thread {

	/**
	 * {@value} - this configuration attribute defines how long
	 * (in milliseconds) the graph group will wait when no suitable 
	 * request is available in the queue.
	 * 
	 * The group never waits using this attribute when the queue is 
	 * empty - it will just enter in idle state until a new request 
	 * is added to the queue.
	 */
	public static final String WAIT_TIME_ATTR = "wait-time";
	
  public static final int STATUS_INITIALIZING  = 1;
  public static final int STATUS_CONSUMING     = 2;
  public static final int STATUS_PROCESSING    = 3;
  public static final int STATUS_WAITING_EMPTY = 4;
  public static final int STATUS_WAITING_FULL  = 5;
  public static final int STATUS_WAITING_GRAPH = 6;
  public static final int STATUS_SHUTDOWN      = 7;
  public static final int STATUS_DEAD          = 8;
  
  private static final Logger log = Logger.getLogger(GraphGroup.class);

  
  // Instance attributes
  private final I18n i18n = I18n.getInstance(GraphGroup.class);

  protected ReqForwarderInterface requestList;

  protected boolean closing = false;

  protected boolean processLastObjects = true;
  
  protected int status, waitTime; 

  protected Lock requestSync; // synchronization for request objects.
  
  protected Condition queueNotEmpty;

  protected DataAwareManagerMediator fmediator; // mediator objects.
  
  private String name;
  
  /**
   * Sync object used to notify idle Graphs that another one has finished
   * processing a request.
   * 
   * @since Data-Aware 1.5
   */
  protected Condition graphFinished;
  

  /**
   * Constructor.
   * 
   * @param name
   *          Thread name.
   * @param dwareMediator
   *          Mediator of manager objects.
   */
  public GraphGroup(String name, DataAwareManagerMediator dwareMediator) throws ManagerException {

    super(name);
    this.name = name;
    if (dwareMediator == null) {
      throw new ManagerException(i18n.getString("graphMedException"));
    }
    this.fmediator = dwareMediator;
    this.processLastObjects = true;
    this.status = STATUS_INITIALIZING;
  }

  /**
   * Configures this graph group.
   * 
   * @param config
   *          the DOM tree that has the configuration to be applied.
   */
  public synchronized void configure(Element config) {
  	this.waitTime = DOMUtils.getIntAttribute(config, WAIT_TIME_ATTR, false);
  	if (this.waitTime <= 0) {
  		this.waitTime = 60000; // default wait time is 60 seconds
  	}
  }
  
  /*
   * Called by mediator object.
   */
  protected void setRequestSyncObj(Lock _requestSync, Condition _queueNotEmpty) {
    this.requestSync = _requestSync;
    this.queueNotEmpty = _queueNotEmpty;
    this.graphFinished = this.requestSync.newCondition();
  }

  /**
   * Starts to wait for free graphs and requests to process.
   */
  public void run() {
    this.fmediator.registerGraphGroup(this);
    
    Request request = null;

    log.info(i18n.getString("graphGroupReady"));
    
    // group initialized, wait for a request in queue
    this.requestSync.lock();
    try {
      log.info("GraphGroup waiting queue to be ready for consumption.");
      this.status = STATUS_WAITING_EMPTY;
      this.queueNotEmpty.await();
    } catch (InterruptedException e1) {
    } finally {
      this.requestSync.unlock();
    }

    // do this while not going down
    while ( ! this.closing ) {
      log.debug("GraphGroup-running. Waiting for graph avaiable...");
      this.status = STATUS_WAITING_GRAPH;
      this.waitForGraphAvailable(1);
      this.status = STATUS_CONSUMING;
      log.debug("Got a graph avaiable");
      // Now we have certainly a graph available. But we
      // need a request to process.
      this.requestSync.lock();
      try {
        log.debug("Asking the queue for a request to process");
        request = getNextRequest();
      } catch (NoSuchElementException e) {
        log.debug("Queue is empty!");
        // queue is empty, so we are done - notify all waiting Threads
        this.graphFinished.signalAll();
        // No request to process, wait for one
        try {
          log.debug("No requests to process. Waiting...");
          this.status = STATUS_WAITING_EMPTY;
          this.queueNotEmpty.await();
        } catch (InterruptedException e1) {
        }
        continue;
      } finally {
        this.requestSync.unlock();
      }
      
      if (request == null) {
        // no suitable request was found, so wait until 
        // a Graph finishes processing any request.
      	log.debug("No requests available for this queue");
        waitForNextTry();
        continue;
      }
      
      if (log.isDebugEnabled())
        log.debug("Got a request to process: " + request);
      try {
        this.status = STATUS_PROCESSING;
        process(request);
        this.status = STATUS_CONSUMING;
      } catch (RuntimeException e) {
        log.fatal(i18n.getString("graphFailed"), e);
        throw e;
      }
      if (log.isDebugEnabled())
        log.debug("Request: " + request + " sent to graph processment");
    }

    log.warn(i18n.getString("processingLastRequests"));
    while (this.processLastObjects) {
    	log.debug("GraphGroup processing last requests before shutdown.");
      // Now the graph group is going down, so we have to empty
      // the list before ending this thread. At this point we
      // may assume that no more objects will be put in the list
      this.status = STATUS_WAITING_GRAPH;
      this.waitForGraphAvailable(1);
      // Now we have certainly a graph available. But we
      // need a request to process.
      this.status = STATUS_CONSUMING;
      try {
      	log.debug("Asking the queue for a request to process");
      	request = getNextRequest();
      	if (request == null) {
      		log.debug("No requests available for this queue");
      		waitForNextTry();
      		continue;
      	}
        log.debug("Got a request to process: " + request);
        try {
          this.status = STATUS_PROCESSING;
          process(request);
          this.status = STATUS_CONSUMING;
        } catch (RuntimeException e) {
          log.fatal(i18n.getString("graphFailed"), e);
          throw e;
        }
        log.debug("Request: " + request + " sent to graph processment");
      } catch (NoSuchElementException e) {
      	log.debug("Queue is empty!");
      	this.processLastObjects = false;
        this.requestSync.lock();
        try {
          // queue is empty - notify all waiting Threads
          this.graphFinished.signalAll();
        } finally {
          this.requestSync.unlock();
        }
      }
    }

    this.status = STATUS_SHUTDOWN;
    this.shutdownGraphs();
    log.warn(i18n.getString("graphGroupDown"));

    // Initialize these attributes in case this thread is called
    // to restart after this shutdown.
    this.closing = false;
    this.processLastObjects = true;

    finalizeGraphGroup();
    this.status = STATUS_DEAD;
  }
  
  // no suitable request was found, so wait until 
  // a Graph finishes processing any request.
  protected void waitForNextTry() {
    this.requestSync.lock();
    try {
      this.status = STATUS_WAITING_FULL;
      this.graphFinished.await(this.waitTime, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      this.requestSync.unlock();
    }
    log.debug("Woke-up and will try again to find a request");
  }
  
  /**
   * Use this method to shutdown this thread. It will process the last requests
   * in the request list before finishing.
   * 
   * @param wait
   *          if true, this method will block until this thread finishes.
   */
  public void shutdown(boolean wait) {
    this.closing = true;

    this.requestSync.lock();
    try {
      this.graphFinished.signalAll();
      this.queueNotEmpty.signalAll();
    } finally {
      this.requestSync.unlock();
    }
    log.warn(i18n.getString("graphGroupGoingDown", this.getName()));
    if (wait) {
      try {
        this.join();
      } catch (InterruptedException e) {
        log.debug("Got interruption.", e);
      }
    }
  }

  /**
   * Use this method to stop this thread after processing its last request. It
   * will NOT process the last requests in the request list before finishing.
   * 
   * @param wait
   *          it true, this method will block until this thread finishes.
   */
  public void kill(boolean wait) {
    this.processLastObjects = false;
    if ( ! this.closing ) {
      this.shutdown(wait);
    }
    this.fmediator.unregisterGraphGroup(this.getName());
  }
  
  /**
   * Used to notify all the Graph group that it should try to fetch a new
   * request if it is waiting after the last call to <code>getNextRequest</code>
   * returned <code>null</code>.
   * 
   * <strong>
   * ALL SUBCLASSES MUST ENSURE THAT THIS METHOD IS CALLED AFTER A GRAPH
   * HAS FINISHED PROCESSING A REQUEST. IF THE SUBCLASS OVERRIDES THIS 
   * METHOD, IT MUST ADD THE FOLLOWING LINE BEFORE EXITING THE NEW METHOD:
   *    <code>super.graphFinished(graphName, request, error);</code>
   * </strong>
   * 
   * @param graphName
   *          The name of the graph that has finished processing.
   * @param request
   *          The request that was processed.
   * @param error
   *          The exception (if any) that occurred during processing.
   * @since Data-Aware 1.5
   */
  protected void graphFinished(String graphName, Request request, Throwable error) {
    if (error == null) {
      this.fmediator.reqProcessed(request);
    } else {
      this.fmediator.reqFailed(request, graphName, error);
    }
    
    // the only way a grah group will be awaken while waiting
    // on processSync is notifying it after a graph has
    // finished, so DO NOT REMOVE THE CODE BELLOW.
    this.requestSync.lock();
    try {
      this.graphFinished.signalAll();
    } finally {
      this.requestSync.unlock();
    }
  }
  
  /**
   * Returns current group status value, according to <code>STATUS_*</code>
   * constants.
   * 
   * @return current group status.
   */
  public int getStatus() {
    return this.status;
  }

  /**
   * Reconfigures the graph inside this group.
   * 
   * @param config
   *          the DOM tree that has the configuration to be applied.
   * @throws GraphException
   *           if some error ocurrs while reconfiguring the graphs.
   */
  public abstract void configureGraph(Element config) throws GraphException;

  /**
   * Reconfigures the filters inside the graphs of this group.
   * 
   * @param filterName
   *          the name of the filter that will be reconfigured.
   * @param config
   *          the DOM tree that has the filter configuration to be applied.
   * @throws GraphException
   *           if the graph configuration does not have a filter with the give
   *           name.
   * @throws FilterException
   *           if some error ocurrs while reconfiguring the filters.
   */
  public abstract void configureFilter(String filterName, Element config) throws GraphException,
      FilterException;

  /**
   * This method is only called when there is at least one graph not working at
   * the moment. This method must pass this request to such graph to process it.
   * 
   * @throws RuntimeException
   *           if some exception was not handled, or if it is wanted to finish
   *           this graph group with an error.
   */
  protected abstract void process(Request request) throws RuntimeException;

  /**
   * When called, this method will tell to all the graphs that they must stop
   * when they finish their jobs.
   */
  protected abstract void shutdownGraphs();

  /**
   * This method blocks until there is some graph idle.
   * 
   * @param minVal
   *          the minimum number of graphs available that this method will wait
   *          for.
   */
  protected abstract void waitForGraphAvailable(int minVal);

  /**
   * Must return the next request.
   */
  protected abstract Request getNextRequest() throws NoSuchElementException;

  /**
   * Called before graph group thread dies.
   */
  protected abstract void finalizeGraphGroup();

  //JMX Methods
  public String getGraphGroupName() {
     return this.name;
  }
  public boolean isClosing() {
     return this.closing;
  }
  public boolean hasToProcessLastObjects() {
     return this.processLastObjects;
  }

  public void killGroup() {
     this.closing=true;
     this.processLastObjects=false;
  }  
}
