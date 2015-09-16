/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE 
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 * Created on Jul 29, 2005
 */
package br.com.auster.dware.manager;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.Request;


/**
 * <p><b>Title:</b> FIFOQueueReqForwarder</p>
 * <p><b>Description:</b> TODO class description</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id: FIFOQueueReqForwarder.java 272 2006-11-30 20:08:39Z rbarone $
 * @since Data-Aware 1.5
 */
public class FIFOQueueReqForwarder extends PriorityQueueReqForwarder {
  
  private class QueuedRequest {
    
    private final Request request;
    private long position;
    
    public QueuedRequest(Request req, long pos) {
      this.request = req;
      this.position = pos;
    }
    
  }
  
  private final static long INFINITE_WEIGHT = Long.MAX_VALUE;
  private final static Long INFINITE_WEIGHT_OBJ = new Long(INFINITE_WEIGHT);

  // using parent i18n resource
  private final I18n i18n = I18n.getInstance(PriorityQueueReqForwarder.class);
  
  // my logger
  private static final Logger log = Logger.getLogger(FIFOQueueReqForwarder.class);
  
  // Map<Long(Weight),LinkedList<Request>> - the main request queue
  private TreeMap queueByWeight;
  
  // Map<Thread,Long> - stores the max weight ever requested by a specific Thread
  private IdentityHashMap lastWeightsByThread = new IdentityHashMap();
  
  // maintain current queue size 
  private int requestCount = 0;
  
  // controls position sequence
  private long requestPositionSequence = 0;
  
  // flag to indicate if the infinite queue is fake or real
  // ("fake" means automatically created, but no Thread is consuming it)
  private boolean hasFakeInfiniteQueue = true;
  
  // used only in debug mode
  private final ThreadLocal lastPosition = new ThreadLocal() {
    protected Object initialValue() {
      return new Long(0);
    }
  };
  
  /**
   * @param dwareManMed
   */
  public FIFOQueueReqForwarder(DataAwareManagerMediator dwareManMed) {
    super(dwareManMed);
    this.queueByWeight = new TreeMap();
    this.queueByWeight.put(INFINITE_WEIGHT_OBJ, new LinkedList());
    
    // register to mediator, overriding parent's registration
    if (dwareManMed != null) {
       dwareManMed.registerReqForwarder(this);
    }
  }
  
  public void configure(Element config) {
    super.configure(config);
  }
  
  protected boolean addReq(Request newReq) {
    this.queueLock.lock();
    try {
      LinkedList queue;
      SortedMap weightMap = this.queueByWeight.tailMap(new Long(newReq.getWeight()));
      if (weightMap.isEmpty()) {
        // queueByWeight is initialized with infinite upper limit, 
        // so this should never happen, but we will try to recover!
        log.error("Weight map is upper bounded!! Recovering infinite upper limit.");
        queue = new LinkedList();
        this.queueByWeight.put(INFINITE_WEIGHT_OBJ, queue);
      } else {
        queue = (LinkedList) weightMap.get(weightMap.firstKey());
      }
      
      if (this.requestPositionSequence == INFINITE_WEIGHT) {
        reindexAllRequests();
      }
      
      // add to main queue
      queue.addLast(new QueuedRequest(newReq, ++this.requestPositionSequence));
      this.requestCount++;
      
    } finally {
      this.queueLock.unlock();
    }
 
    return true;
  }
  
  private void reindexAllRequests() {
    log.info("Queue sequence has reached limit value. Re-indexing all requests...");
    this.requestPositionSequence = 0;
    Iterator it = this.queueByWeight.values().iterator();
    while (it.hasNext()) {
      Iterator qit = ((LinkedList) it.next()).iterator();
      while (qit.hasNext()) {
        QueuedRequest req = (QueuedRequest) qit.next();
        req.position = ++this.requestPositionSequence;
      }
    }
    log.info("Finished re-indexing requests.");
  }
  
  public Request chooseNextRequest(long maxWeight) throws NoSuchElementException {
    if (maxWeight == 0) {
      log.debug("FIFOQueue does not support maxWeight = 0; returning null.");
      return null;
    }
    
    QueuedRequest next = null;
    this.queueLock.lock();
    try {
      if (this.requestCount == 0) {
        throw new NoSuchElementException("Data-Aware queue is empty.");
      }
      
      // verifies max weight and (eventually) stores this one as new max
      Long desiredWeight = (maxWeight < 0 ? INFINITE_WEIGHT_OBJ : new Long(maxWeight));
      Long maxRequestedWeight = setMaxRequestedWeight(desiredWeight);

      LinkedList myQueue = (LinkedList) this.queueByWeight.get(maxRequestedWeight);
      SortedMap previousQueues = this.queueByWeight.headMap(maxRequestedWeight);
      if (myQueue.isEmpty()) {
        boolean hasToReturnNull = false;
        if (!previousQueues.isEmpty()) {
          // request will only be in previous queues - since my queue
          // is already empty, let's begin to consume them.
          TreeMap reversedQueue = new TreeMap(Collections.reverseOrder());
          reversedQueue.putAll(previousQueues);
          Iterator it = reversedQueue.values().iterator();
          while (it.hasNext()) {
            LinkedList queue = (LinkedList) it.next();
            if (!queue.isEmpty()) {
              if ( ((QueuedRequest)queue.getFirst()).request.getWeight() <= desiredWeight.longValue() ) {
                next = (QueuedRequest) queue.removeFirst();
              } else {
                hasToReturnNull = true;
              }
              break;
            }
          }
        }
        // if we ever reach this point, there are no requests available in any real queue,
        // so let's check if we can consume from the fake infinite queue
        if (this.hasFakeInfiniteQueue && !hasToReturnNull && next == null) {
          SortedMap realQueues = this.queueByWeight.headMap(INFINITE_WEIGHT_OBJ);
          Long highestWeightBeforeFake = (Long) realQueues.lastKey();
          if (!realQueues.isEmpty() && maxRequestedWeight.equals(highestWeightBeforeFake)) {
            LinkedList fakeInfiniteQueue = (LinkedList) this.queueByWeight.get(INFINITE_WEIGHT_OBJ);
            if (!fakeInfiniteQueue.isEmpty()) {
              next = (QueuedRequest) fakeInfiniteQueue.removeFirst();
            }
          }
        }
      } else if ( ((QueuedRequest)myQueue.getFirst()).request.getWeight() <= desiredWeight.longValue() ) {
        // my queue is not empty yet, so request must be fetched from here
        // ...but only if it respects the desired weight limit
        next = (QueuedRequest) myQueue.removeFirst();
      }
      
      // log some useful debug info
      if (log.isDebugEnabled()) {
        if (next == null) {
          StringBuffer m = new StringBuffer("no request found for weight " + maxWeight
                                            + "; weight map is: [");
          for (Iterator it = this.queueByWeight.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            m.append(entry.getKey() + "=>" + ((LinkedList) entry.getValue()).size() + ",\n");
          }
          log.debug(m.append("]").toString());
        } else {
          long lastPos = ((Long) this.lastPosition.get()).longValue();
          if (lastPos > next.position) {
            log.debug("Changed sequence flow for " + Thread.currentThread() + 
                      ": last position was " + lastPos + ", new position is " + next.position);
          }
          this.lastPosition.set(new Long(next.position));
        }
      }
      
      if (next != null) {
        this.requestCount--;
      }
      
    } finally {
      this.queueLock.unlock();
    }
    
    return next == null ? null : next.request;
  }
  
  /**
   * 
   * @param maxWeight Top limit for returned weight.
   * @return the max requested weight by any thread that is not greated than maxWeight.
   */
  private Long getMaxRequestedWeight() {
    return (Long) this.lastWeightsByThread.get(Thread.currentThread());
  }
  
  private Long setMaxRequestedWeight(Long weight) {
    Long previous = getMaxRequestedWeight();
    if (previous == null || weight.longValue() > previous.longValue()) {
        this.lastWeightsByThread.put(Thread.currentThread(), weight);
        log.debug("splitting queue from " + previous + " to " + 
                  weight + " in " + Thread.currentThread());
        trySplitQueue(previous, weight);
        return weight;
    }
    return previous;
  }
  
  private void trySplitQueue(Long oldWeight, Long newWeight) {
    // see if newWeight already exists
    LinkedList newQueue = (LinkedList) this.queueByWeight.get(newWeight);
    if (newWeight.longValue() != INFINITE_WEIGHT && newQueue == null) {
      // it doesn't exist - so we need to create it and merge
      // everything from before and after.
      newQueue = new LinkedList();
      this.queueByWeight.put(newWeight, newQueue);
      SortedMap lowerQueues = this.queueByWeight.headMap(newWeight);
      if (!lowerQueues.isEmpty()) {
        LinkedList lowerQueue = (LinkedList) lowerQueues.get(lowerQueues.lastKey());
        mergeQueues(lowerQueue, true, newWeight.longValue(), newQueue);
      }
      SortedMap upperQueues = this.queueByWeight.tailMap(new Long(newWeight.longValue() + 1L));
      if (!upperQueues.isEmpty()) {
        LinkedList upperQueue = (LinkedList) upperQueues.get(upperQueues.firstKey());
        mergeQueues(upperQueue, false, newWeight.longValue(), newQueue);
      }
    }
    
    // check if newWeight is infinite - if so, there is no "fake" queue
    if (newWeight.longValue() == INFINITE_WEIGHT) {
      this.hasFakeInfiniteQueue = false;
    }
    
    // verify if any other Thread still uses oldWeight
    // (need to check if downgrading or upgrading weight)
    if (oldWeight == null || oldWeight.longValue() == INFINITE_WEIGHT) {
      return;
    }
    boolean hasOtherReference = false;
    Iterator it = this.lastWeightsByThread.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry entry = (Map.Entry) it.next();
      long weight = ((Long) entry.getValue()).longValue();
      if (weight == oldWeight.longValue() && entry.getKey() != Thread.currentThread()) {
        hasOtherReference = true;
        break;
      }
    }
    if (!hasOtherReference) {
      // no other references to oldWeight, so remove it!
      LinkedList oldQueue = (LinkedList) this.queueByWeight.get(oldWeight);
      if (oldQueue != null && !oldQueue.isEmpty()) {
        SortedMap lowerQueues = this.queueByWeight.headMap(oldWeight);
        if (!lowerQueues.isEmpty()) {
          LinkedList lowerQueue = (LinkedList) lowerQueues.get(lowerQueues.lastKey());
          mergeQueues(oldQueue, false, oldWeight.longValue(), lowerQueue);
        }
        SortedMap upperQueues = this.queueByWeight.tailMap(new Long(oldWeight.longValue() + 1L));
        if (!upperQueues.isEmpty()) {
          LinkedList upperQueue = (LinkedList) upperQueues.get(upperQueues.firstKey());
          mergeQueues(oldQueue, true, -1L, upperQueue);
        }
      }
    }
  }
  
  private void mergeQueues(LinkedList from, boolean greaterEquals, long weight, LinkedList to) {
    ListIterator itFrom = from.listIterator();
    ListIterator itTo = to.listIterator();
    while (itFrom.hasNext()) {
      QueuedRequest reqFrom = (QueuedRequest) itFrom.next();
      
      // first, discard request if it doesn't attend to specified weight
      long weightFrom = reqFrom.request.getWeight();
      if ( (greaterEquals && weightFrom < weight) ||
          (!greaterEquals && weightFrom > weight) ) {
        continue;
      }
      
      // search for position to insert into
      while (itTo.hasNext()) {
        QueuedRequest reqTo = (QueuedRequest) itTo.next();
        if (reqFrom.position < reqTo.position) {
          // found the correct position, rewind to previous cursor
          itTo.previous();
          // ...and insert here
          itTo.add(reqFrom);
          // finally, remove from old queue
          itFrom.remove();
          reqFrom = null;
          break;
        }
      }
      
      // did not find any suitable position, so insert at the end
      if (reqFrom != null) {
        itTo.add(reqFrom);
        itFrom.remove();
        reqFrom = null;
      }
    }
  }
  
  
  public long getWishWeight() {
    return -1L;  // let PriorityQueueWishGraphGroup decide what to do
  }
  
  public void shutdown() {
    // nothing to do
  }

}
