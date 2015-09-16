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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.remote.ClientRemoteGraphInterface;

/**
 * This graph groups ask the priority queue to the request with the weight
 * closest to the weight it has avaiable. This group also asks to the priority
 * queue the weight it prefer to deliver. Using the value returned the group
 * tries to free space to satisfy the desire of the priority queue.
 * 
 * @version $Id: PriorityQueueWishRemoteGraphGroup.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class PriorityQueueWishRemoteGraphGroup extends RemoteGraphGroup {

  /**
   * This attribute of graph group configuration tells the maximum weight this
   * group can treat.
   */
  public static final String MAX_WEIGHT = "max-weight";

  private long maxWeight; // maximum weight this group can process.

  private long avaiableWeight; // weight avaiable to process a request.

  private static final Logger log = Logger.getLogger(PriorityQueueWishRemoteGraphGroup.class);

  private long wishWeight; // this value is given by the PQ to balance queues.

  /**
   * Constructor
   * 
   * @param name
   *          Thread name.
   * @param dwareMediator
   *          Mediator of manager objects.
   * @param config
   *          graph group configuration.
   * @param graphConfig
   *          graphs configuration.
   */
  public PriorityQueueWishRemoteGraphGroup(String name, DataAwareManagerMediator dwareMediator,
                                           Element config, Element graphConfig,
                                           ClientRemoteGraphInterface _clientRemoteGraphGrp)
      throws GraphException, ManagerException {

    super(name, dwareMediator, config, graphConfig, _clientRemoteGraphGrp);
    maxWeight = DOMUtils.getIntAttribute(config, MAX_WEIGHT, true);
    avaiableWeight = maxWeight;
    if (log.isDebugEnabled()) { // initial status log
      log.debug("PQ Wish Graph Group created: maxWeight=" + maxWeight + "  avaiableWeight="
                + avaiableWeight);
    }
    wishWeight = -1;
  }

  /**
   * Return the next request.
   */
  protected Request getNextRequest() throws NoSuchElementException {
    Request retReq;

    // log.info(i18n.getString("graphGroupReady"));

    if (log.isDebugEnabled()) // print forced list
      log.debug("searching for request...  total weight avaiable = " + avaiableWeight);

    // if there isn't a wishWeight set ask to PriorityQueueReqForwarder...
    if (wishWeight == -1)
      wishWeight = ((DataAwareManagerWishMediator) fmediator).getWishWeight();
    // ((PriorityQueueReqForwarder)fmediator.getRegisteredReqForwarder()).getWishWeight();

    if (wishWeight < avaiableWeight) { // including if wishWeight == -1
      retReq = ((DataAwareManagerWishMediator) fmediator).getNextRequest(avaiableWeight);// requestList.chooseNextRequest(avaiableWeight);
      wishWeight = -1; // if the last wish was satisfied ask for a new one next
                        // time
    } else { // wishWeight can't be satisfied yet, ask for a small request to
              // spend resources
      if (log.isDebugEnabled())
        log.debug("there isn't enough resources to satisfy the wish (" + wishWeight
                  + "), ask for a small request to spend resources!");
      retReq = ((DataAwareManagerWishMediator) fmediator).getNextRequest(0);// requestList.chooseNextRequest(0);
    }

    if (retReq != null) {
      // update avaiable space
      avaiableWeight -= retReq.getWeight();
  
      if (log.isDebugEnabled())
        log.debug("got the request " + retReq.getId() + ": weight=" + retReq.getWeight()
                  + "   weight avaiable=" + avaiableWeight);
    }

    return retReq;
  }

  /**
   * Override of method. We need to know when some request ends to compute force
   * request algorithm.
   * 
   * @param graph
   *          the graph that is going to be available.
   * @param request
   *          the request that was processed.
   * @param error
   *          if null, the request was processed successfully. If not, it
   *          represents the problem that ocurred.
   */
  public void serverGraphFinishedAction(Request request, String graphName, Throwable error) {
    avaiableWeight += request.getWeight(); // update avaiable space
    if (log.isDebugEnabled())
      log.debug("finishes request " + request.getId() + ": weight=" + request.getWeight()
                + "   weight avaiable=" + avaiableWeight);
    
    // it is mandatory to notify the GraphGroup class 
    // that a Graph has finished processing a request.
    // (since Data-Aware 1.5)
    super.serverGraphFinishedAction(request, graphName, error);
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
    super.serverGraphCommitingAction(request, graphName);
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
  public void serverGraphRollingBackAction(Request request, String graphName, Throwable error) {
    super.serverGraphRollingBackAction(request, graphName, error);
  }

}
