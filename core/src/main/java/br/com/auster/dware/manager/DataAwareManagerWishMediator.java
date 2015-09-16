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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import br.com.auster.common.log.LogFactory;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.checkpoint.AbstractCheckpoint;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * This class handles communication between manager components. Implements
 * mediator pattern.
 * 
 * @version $Id: DataAwareManagerWishMediator.java 272 2006-11-30 20:08:39Z rbarone $
 */
public class DataAwareManagerWishMediator implements DataAwareManagerMediator {
  
  private static final Logger log = LogFactory.getLogger(DataAwareManagerWishMediator.class);
  
  private ReqForwarderInterface reqFwd;

  private AbstractCheckpoint chkPt;

  private GraphManager gmanager;

  private JMXGraphGroupCounter counters;
  
  private ThreadGroup requeueWorkersGroup = new ThreadGroup("RequeueWorkers");
  
  // number of requests by transaction
  private final Map<String,RequestCounter> reqCounterByTransaction = new HashMap<String,RequestCounter>();
  
  // used to synchronize access to numReqByTransaction
  protected final Lock reqCounterLock = new ReentrantLock();
  
  
  /**
   * Constructor.
   */
  public DataAwareManagerWishMediator() {
    counters = new JMXGraphGroupCounter();
  }

  /**
   * Register a <code>GraphManager</code> object to this mediator.
   * 
   * @param gm
   *          <code>GraphManager</code> object.
   */
  public void registerGraphManager(GraphManager gm) {
    gmanager = gm;
  }

  /**
   * Register a <code>ReqForwarderInterface</code> object to this mediator.
   * 
   * @param _reqFwd
   *          <code>ReqForwarderInterface</code> object.
   */
  public void registerReqForwarder(ReqForwarderInterface _reqFwd) {
    reqFwd = _reqFwd;
  }

  /**
   * Register a <code>AbstractCheckpoint</code> object to this mediator.
   * 
   * @param _chkPt
   *          <code>AbstractCheckpoint</code> object.
   */
  public void registerCheckpoint(AbstractCheckpoint _chkPt) {
    chkPt = _chkPt;
  }

  /**
   * Register a <code>GraphGroup</code> object to this mediator.
   * 
   * @param _chkPt
   *          <code>GraphGroup</code> object.
   */
  public void registerGraphGroup(GraphGroup graphGp) {
    graphGp.setRequestSyncObj(this.reqFwd.getSyncObj(), 
                              this.reqFwd.getQueueNotEmptyCondition());
  }

  /**
   * Tells graph manager to unload this graph group.
   * 
   * @param graphGpName
   *          nome of the graph group that will be unloaded.
   */
  public void unregisterGraphGroup(String graphGpName) {
    gmanager.unloadGraphGroup(graphGpName);
  }

  /**
   * Returns some request given the maximum weight allowed.
   * 
   * @param maximum
   *          weight allowed.
   */
  public Request getNextRequest(long maxWeight) throws NoSuchElementException {

    return reqFwd.chooseNextRequest(maxWeight);
  }

  /**
   * Return the weight that the request forwarder wish to send.
   */
  public long getWishWeight() {
    if (reqFwd instanceof PriorityQueueReqForwarder) {
      return ((PriorityQueueReqForwarder) reqFwd).getWishWeight();
    }
    return -1;
  }

  /**
   * This must be called when some request finish to process.
   * 
   * @param req
   *          request object.
   */
  public void reqProcessed(Request req) {
    if (chkPt != null) {
      chkPt.checkReqProcessed(req);
    }
    incrementReqFinishedCounter(req);
  }

  /**
   * This must be called when some request fails to process.
   * 
   * @param req
   *          request object.
   * @param graphName
   *          graph name.
   * @param error
   *          exception thrown.
   */
  public void reqFailed(Request req, String graph, Throwable error) {
    if (chkPt != null && chkPt.checkReqFailed(req, graph, error)) {
      synchronized (this.requeueWorkersGroup) {
        if (this.gmanager.isShuttingDown()) {
          // must ensure that at least one Graph will process the requeued
          // request before dying.
          new RequeueWorker(req, this.chkPt.getRequeueSleepTime()).requeue();
        } else {
          // no shutdown called, we can delegate de requeue to a separate Thread.
          Runnable worker = new RequeueWorker(req, this.chkPt.getRequeueSleepTime());
          new Thread(this.requeueWorkersGroup, worker).start();
        }
      }
    } else {
      incrementReqFinishedCounter(req);
    }
  }

  /**
   * This must be called when some request was enqueued.
   * 
   * @param req
   *          request object.
   */
  public void reqQueued(Request req) {
    if (chkPt != null) {
      chkPt.checkReqWillBeProcessed(req);
    }
    this.reqCounterLock.lock();
    try {
      RequestCounter currentCount = this.reqCounterByTransaction.get(req.getTransactionId());
      if (currentCount == null) {
        currentCount = new RequestCounter();
        currentCount.queueCount++;
        this.reqCounterByTransaction.put(req.getTransactionId(), currentCount);
      } else {
        currentCount.queueCount++;
      }
    } finally {
      this.reqCounterLock.unlock();
    }
  }
  
  public void reqRequeued(Request req) {
    if (chkPt != null) {
      chkPt.checkReqWillBeProcessed(req);
    }
  }
  
  private void incrementReqFinishedCounter(Request req) {
    this.reqCounterLock.lock();
    try {
      RequestCounter currentCount = this.reqCounterByTransaction.get(req.getTransactionId());
      if (currentCount == null || currentCount.queueCount == 0) {
        log.error("Request was processed but queue was supposed to be empty: " + req);
      } else if (++currentCount.finishedCount == currentCount.queueCount) {
        // finished processing all requests for this queue
        this.reqCounterByTransaction.remove(req.getTransactionId());
        this.gmanager.queueProcessed(req.getTransactionId(), currentCount.finishedCount);
      }
    } finally {
      this.reqCounterLock.unlock();
    }
  }

  /**
   * Check if the request has been already loaded.
   * 
   * @param req
   *          request object.
   */
  public boolean checkIfReqLoaded(Request req) {
    if (chkPt == null) {
      return false;
    }
    return chkPt.contains(req);
  }
  
  public JMXGraphGroupCounter getJMXCounters() {
	  return counters;
  }
  
  public void shutdown() {
    synchronized (this.requeueWorkersGroup) {
      this.requeueWorkersGroup.interrupt();
    }
  }
  
  
  private class RequestCounter {
    private int queueCount = 0;
    private int finishedCount = 0;
  }
  
  
  private class RequeueWorker implements Runnable {
    private Request request;
    private long sleepTime;
    
    RequeueWorker(Request request, long sleepTime) {
      this.request = request;
      this.sleepTime = sleepTime;;
    }
    
    public void run() {
      requeue();
    }
    
    public void requeue() {
      if (this.sleepTime > 0) {
        try {
          Thread.sleep(this.sleepTime);
        } catch (InterruptedException e) {}
      }
      gmanager.requeue(this.request);
    }
  }
  
}
