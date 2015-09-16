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

import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.rmi.PortableRemoteObject;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.remote.ClientRemoteGraphInterface;

/**
 * This class represents a remote graph group. It communicates with a remote
 * class that manages the graphs, forwarding to it the requests to process.
 * 
 * @version $Id: RemoteGraphGroup.java 210 2006-06-26 23:17:25Z framos $
 */
public abstract class RemoteGraphGroup extends GraphGroup {

  public static final String MAX_GRAPH_ATTR = "max-graphs";

  public static final String GRAPHS_PER_PROCESSOR_ATTR = "graphs-per-processor";

  private static final int INI_TIME_TASK = 10000; // default initial value

  private static final Logger log = Logger.getLogger(RemoteGraphGroup.class);

  // Instance attributes
  protected final I18n i18n = I18n.getInstance(RemoteGraphGroup.class);

  protected volatile int max = 0; // maximum number of graphs

  protected volatile int gavaiable = 0; // graphs avaiable

  private HashMap reqHash; // requests that each graph is processing

  protected ClientRemoteGraphInterface clientRemoteGraphGrp; // remote object

  private final Object gSyncObj = new Object(); // synchronization object

  private CheckConnectionTask checkConnTimer; // object responsible to check
                                              // connection with remote client

  /**
   * Constructor of a new remote graph group.
   * 
   * @param name
   *          name of this graph group.
   * @param dwareMediator
   *          manager mediator.
   * @param config
   *          the DOM tree that has the configuration to be applied.
   * @param graphConfig
   *          DOM object of graph configuration.
   * @param _clientRemoteGraphGrp
   *          remote object stub pointer.
   */
  public RemoteGraphGroup(String name, DataAwareManagerMediator dwareMediator, Element config,
                          Element graphConfig, ClientRemoteGraphInterface _clientRemoteGraphGrp)
      throws GraphException, ManagerException {

    super(name, dwareMediator);
    reqHash = new HashMap();
    clientRemoteGraphGrp = _clientRemoteGraphGrp;
    this.configure(config);
    this.configureGraph(graphConfig);
    checkConnTimer = new CheckConnectionTask(INI_TIME_TASK);
    checkConnTimer.start();
  }

  /**
   * Configures this graph group.
   * 
   * @param config
   *          the DOM tree that has the configuration to be applied.
   */
  public final void configure(Element config) {
    try {
      this.max = DOMUtils.getIntAttribute(config, GRAPHS_PER_PROCESSOR_ATTR, true)
                 * Runtime.getRuntime().availableProcessors();
    } catch (IllegalArgumentException e) {
      this.max = DOMUtils.getIntAttribute(config, MAX_GRAPH_ATTR, true);
    }
    gavaiable = max;
    log.info(i18n.getString("maxGraphsNumber", this.getName(), Integer.toString(this.max)));
  }

  /**
   * Reconfigures the graph inside this group.
   * 
   * @param config
   *          the DOM tree that has the configuration to be applied.
   */
  public void configureGraph(Element config) throws GraphException {

    log.info(i18n.getString("settingGraphs", this.getName()));

    try {
      RemoteGraphGroupCallback callBackObj = new RemoteGraphGroupCallback();
      clientRemoteGraphGrp.configureGraph(this.getName(), config, max, callBackObj);
    } catch (RemoteException re) {
      throw new GraphException(i18n.getString("remoteConfigErr") + " : " + re.getMessage());
    }
  }

  /**
   * This method is only called when there is at least one graph not working at
   * the moment. This method must pass this request to such graph to process it.
   */
  protected void process(Request request) {
    if (log.isDebugEnabled())
      log.debug("Remote process request. weight=" + request.getWeight());
    gavaiable--;
    reqHash.put(request.getId(), request);
    try {
      clientRemoteGraphGrp.process(this.getName(), request);
    } catch (RemoteException re) { // something that I don't know happened with
                                    // remote graph group
      killRemoteGraphGroup(re);
    } catch (ManagerException me) {
      log.warn(i18n.getString("remoteProcessErr") + " : " + me.getMessage());
      // put the request back to the queue.
      synchronized (reqHash) {
        reqHash.remove(request.getId());
        fmediator.reqFailed(request, "processError", me);
      }
    }
  }

  /*
   * Kill this graph group, putting all request not finished back to request
   * forwarder.
   */
  private void killRemoteGraphGroup(RemoteException re) {

    synchronized (reqHash) {
      if (processLastObjects) {

        log.warn(i18n.getString("remoteProcessErr") + " : " + re.getMessage());

        /*
         * System.out.println("LYE RemoteException! "+this.getName()); String
         * hashcodesMissed = ""; for (int i = reqHash.size(); i-- > 0;) {
         * iterator.advance(); hashcodesMissed += iterator.key()+","; }
         * System.out.println("LYE KILL GRP GP! hash = "+hashcodesMissed);
         * 
         * iterator = reqHash.iterator();
         */
        for (Iterator it = reqHash.values().iterator(); it.hasNext();) {
          Request req = (Request) it.next();
          fmediator.reqFailed(req, "processError", re);
        }
        reqHash.clear();

        this.kill(false); // kill the graph group
      }
    }
  }

  /**
   * When called, this method will tell to all the graphs that they must stop
   * when they finish their jobs.
   */
  protected void shutdownGraphs() {
    try {
      clientRemoteGraphGrp.shutdown(this.getName());
    } catch (RemoteException re) {
      log.warn(i18n.getString("remoteShutdownErr") + " : " + re.getMessage());
    }
  }

  /**
   * Reconfigures the filters inside the graphs of this group.
   * 
   * @param filterName
   *          the name of the filter that will be reconfigured.
   * @param config
   *          the DOM tree that has the filter configuration to be applied.
   */
  public void configureFilter(String filterName, Element config) throws GraphException,
      FilterException {

    log.info(i18n.getString("settingFilters", filterName, this.getName()));

    try {
      clientRemoteGraphGrp.configureFilter(this.getName(), filterName, config);
    } catch (RemoteException re) {
      throw new GraphException(i18n.getString("remoteFilterCfgErr") + " : " + re.getMessage());
    }
  }

  /**
   * This method blocks until there is some graph idle.
   * 
   * @param minVal
   *          the minimum number of graphs available that this method will wait
   *          for.
   */
  protected void waitForGraphAvailable(int minVal) {
    boolean gotGraphAvaiable = false;
    while (!gotGraphAvaiable) {
      synchronized (gSyncObj) {
        if (gavaiable == 0) {
          try {
            gSyncObj.wait();
          } catch (InterruptedException ie) {
          }
          continue;
        } else {
          gotGraphAvaiable = true;
        }
      }
    }
    if (log.isDebugEnabled())
      log.debug("number of graphs avaiable: " + gavaiable);
  }

  /**
   * This method will be called by the graphs to indicate that it is free to
   * process other request, and shows a report about the last request processed.
   * 
   * @param request
   *          the request that was processed.
   * @param graphName
   *          name of the graph that is going to be available.
   * @param error
   *          if null, the request was processed successfully. If not, it
   *          represents the problem that ocurred.
   */
  public void serverGraphFinishedAction(Request request, String graphName, Throwable error) {
    // mandatory call as defined in GraphGroup.graphFinished method
    // (since Data-Aware 1.5)
    super.graphFinished(graphName, request, error);
  }

  /**
   * This method will be called by the graphs to let the server graph group know
   * that the remote graph group will commit it work.
   * 
   * @param request
   *          request processed.
   * @param graphName
   *          graph that will commit.
   */
  public void serverGraphCommitingAction(Request request, String graphName) {
    // no action
  }

  /**
   * This method will be called by the graphs to let the server graph group know
   * that there was an error during commit.
   * 
   * @param request
   *          request processed.
   * @param graphName
   *          graph that will commit.
   * @param error
   *          error that ocurred.
   */
  public void serverGraphRollingBackAction(Request request,
                                           String graphName,
                                           Throwable error) {
    // no action
  }

  /**
   * Called before graph group thread dies.
   */
  protected void finalizeGraphGroup() {
    checkConnTimer.shutdown();
    try {
      clientRemoteGraphGrp.notifyServerDown(this.getName());
    } catch (RemoteException re) {
      log.warn(i18n.getString("remoteFinalizeErr"));
    }
  }

  /*
   * Remote callback object.
   */
  class RemoteGraphGroupCallback extends PortableRemoteObject implements RemoteGraphGroupInterface {

    public RemoteGraphGroupCallback() throws RemoteException {
    }

    /**
     * This method is called by the graphs to indicate that it is free to
     * process other request, and shows a report about the last request
     * processed.
     * 
     * @param request
     *          the request that was processed.
     * @param graphName
     *          name of the graph that is going to be available.
     * @param error
     *          if null, the request was processed successfully. If not, it
     *          represents the problem that ocurred.
     * @param time
     *          execution time.
     */
    public void remoteGraphFinished(Request request, String graphName, Throwable error, Date time)
        throws RemoteException {

      // log.debug("RemoteGraphGroup: remoteGraphFinished called for "+
      // graphName+": request "+request.getId());

      // call custom server action
      serverGraphFinishedAction(request, graphName, error);

      synchronized (reqHash) {
        if (reqHash.remove(request.getId()) == null)
          log.warn(i18n.getString("remoteReqUnexpected"));
        else if (log.isDebugEnabled())
          log.debug("request " + request.getId() + " finished and out of graph group cache.");

        // default server action
        if (error == null) {
          fmediator.reqProcessed(request);
          if (log.isDebugEnabled())
            log.debug("The request " + request + " was processed successfully by graph '"
                      + graphName + "'.");
        } else {
          fmediator.reqFailed(request, graphName, error);
        }
      }

      // update timer object
      checkConnTimer.updateTimeTask(time.getTime());

      // update number of graphs avaiable and notify if someone is waiting for a
      // graph
      synchronized (gSyncObj) {
        gavaiable++;
        if (gavaiable == 1)
          gSyncObj.notify();
      }
    }

    /**
     * Do nothing.
     * 
     * @param request
     *          request processed.
     * @param graphName
     *          graph that will commit.
     */
    public void remoteGraphCommiting(Request request, String graphName) throws RemoteException {

      if (log.isDebugEnabled())
        log.debug("RemoteGraphGroup: remoteGraphCommiting called for " + graphName);

      // call custom server action
      serverGraphCommitingAction(request, graphName);
    }

    /**
     * Put the request back in the queue.
     * 
     * @param request
     *          request processed.
     * @param graphName
     *          graph that will commit.
     * @param error
     *          exception throwed.
     */
    public void remoteGraphRollingBack(Request request, String graphName, Throwable error)
        throws RemoteException {

      if (log.isDebugEnabled())
        log.debug("RemoteGraphGroup: remoteGraphRollingBack called for " + graphName);

      synchronized (reqHash) {
        reqHash.remove(request.getId());
        fmediator.reqFailed(request, "rollback called", error);
      }

      // call custom server action
      serverGraphRollingBackAction(request, graphName, error);
    }
  }

  /*
   * Check if remote client's connection is still up.
   */
  class CheckConnectionTask extends Thread {

    private long avgTaskDelay;

    private int numCalc;

    private Object tSyncObj;

    private boolean finish;

    public CheckConnectionTask(long initTaskTime) {
      numCalc = 0;
      avgTaskDelay = initTaskTime;
      tSyncObj = new Object();
      finish = false;
    }

    public void updateTimeTask(long timeActivation) {
      numCalc++;
      if (numCalc != 0)
        avgTaskDelay = (avgTaskDelay * (numCalc - 1) + timeActivation) / numCalc;
      synchronized (tSyncObj) {
        tSyncObj.notify();
      }
    }

    public void shutdown() {
      finish = true;
    }

    public void run() {
      long delayAux;

      // check whe the average time of a request has passed.
      while (!finish) {

        // wait average time.
        synchronized (tSyncObj) {
          delayAux = avgTaskDelay;
          try {
            tSyncObj.wait(avgTaskDelay);
          } catch (InterruptedException ie) {
          }
          if (avgTaskDelay != delayAux) // time has changed
            continue;
        }

        // time is up, check if client is alive.
        if (log.isDebugEnabled())
          log.debug("Checking if connection with client is up. ");
        try {
          clientRemoteGraphGrp.ping();
        } catch (RemoteException re) { // client is down stop graph group.
          log.debug("Client cannot be reached.");
          killRemoteGraphGroup(re);
        }
      }
    }
  }

}
