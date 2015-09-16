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
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;

/**
 * This graph groups ask the priority queue to the request with the weight
 * closest to the weight it has avaiable. This group also asks to the priority
 * queue the weight it prefer to deliver. Using the value returned the group
 * tries to free space to satisfy the desire of the priority queue.
 * 
 * @version $Id: PriorityQueueWishGraphGroup.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class PriorityQueueWishGraphGroup extends LocalGraphGroup {

  /**
   * This attribute of graph group configuration tells the maximum weight this
   * group can treat.
   */
  public static final String MAX_WEIGHT = "max-weight";

  private long maxWeight; // maximum weight this group can process.

  private long avaiableWeight; // weight avaiable to process a request.

  private static final Logger log = Logger.getLogger(PriorityQueueWishGraphGroup.class);

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
  public PriorityQueueWishGraphGroup(String name, DataAwareManagerMediator dwareMediator,
                                     Element config, Element graphConfig) throws GraphException,
      ManagerException {

    super(name, dwareMediator, config, graphConfig);
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

    if (log.isDebugEnabled()) // print forced list
      log.debug("searching for request...  total weight avaiable = " + avaiableWeight);

    DataAwareManagerWishMediator fwmed = (DataAwareManagerWishMediator) fmediator; // cast

    // if there isn't a wishWeight set ask to PriorityQueueReqForwarder...
    if (wishWeight == -1)
      wishWeight = fwmed.getWishWeight();

    if (maxWeight == -1) { // this group can handle any request
      retReq = fwmed.getNextRequest(wishWeight);
      wishWeight = -1; // if the last wish was satisfied ask for a new one next
                        // time
    } else { // check avaiable space
      if (wishWeight < avaiableWeight) { // including if wishWeight == -1
        if (avaiableWeight < 0)
          retReq = fwmed.getNextRequest(0);
        else
          retReq = fwmed.getNextRequest(avaiableWeight);
        wishWeight = -1; // if the last wish was satisfied ask for a new one
                          // next time
      } else { // wishWeight can't be satisfied yet, ask for a small request to
                // spend resources
        if (log.isDebugEnabled())
          log.debug("there isn't enough resources to satisfy the wish (" + wishWeight
                    + "), ask for a small request to spend resources!");
        retReq = fwmed.getNextRequest(0);// requestList.chooseNextRequest(0);
      }
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
  public void graphFinished(Graph graph, Request request, Throwable error, Date time) {
    avaiableWeight += request.getWeight(); // update avaiable space
    if (log.isDebugEnabled())
      log.debug("finishes  request " + request.getId() + ": weight=" + request.getWeight()
                + "   weight avaiable=" + avaiableWeight);
    super.graphFinished(graph, request, error, time);
  }
//JMX Methods
  public long getMaxWeight() {
     return this.maxWeight;
  }
  public void setMaxWeight(long max) {
     this.maxWeight = max;
  }
  public long getAvailableWeight() {
     return this.avaiableWeight;
  }
}
