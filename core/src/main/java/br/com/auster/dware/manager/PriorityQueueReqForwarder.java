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

import gnu.trove.TLongArrayList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.jmx.AusterManagementServices;
import br.com.auster.common.util.I18n;
import br.com.auster.common.util.SyncQueue;
import br.com.auster.dware.graph.Request;

/**
 * <p>
 * Creates a priority queue to forward requests. These queues are created to
 * store requests separated by their weights. This object tries to manage the
 * queues to have similar weights. It has a function that returns which weight
 * it would be perfect if someone requests.
 * </p>
 * <p>
 * This class also have a function that returns a request with the weight
 * closest to the value asked as the maximum weight. Furthermore, in order to
 * manage the balance of the weight of the queues it may deliver a request with
 * the weight smaller than it was asked.
 * </p>
 * 
 * @version $Id: PriorityQueueReqForwarder.java 272 2006-11-30 20:08:39Z rbarone $
 */
public class PriorityQueueReqForwarder implements ReqForwarderInterface {

   /* when the number of new req is 10% of total manage queues. */
   private final float PERCENT_FOR_MANAGE = 0.10f;

   /* rate for decide when to split queues. */
   private final double PERCENT_CHANGE_OFFSET = 0.025;

   /*
    * the difference between weight of two queues must be greater than a
    * percentage of queues thresholds, to avoid split when there is similar
    * queues.
    */
   private final double PERCENT_DIFF_WEIGHT_QUEUES = 0.1;

   /* number of queues should be 1% of the number of requests... */
   private final double PERCENT_NUM_QUEUES = 0.001;

   /*
    * Initial % of difference between queue size and mean between all queues, to
    * define if some queue will be split.
    */
   private final double CHANGE_PERCENT_INI_VAL = 1.5;

   // synchronized list.
   private final List pqueue;

   // threshold array.
   private final TLongArrayList threshold;

   private final I18n i18n = I18n.getInstance(PriorityQueueReqForwarder.class);

   private static final Logger log = Logger.getLogger(PriorityQueueReqForwarder.class);

   // lock for all queue operations
   protected ReentrantLock queueLock = new ReentrantLock(); 
   
   // condition used to signal when the queue has a new request
   protected Condition queueNotEmptyCondition = this.queueLock.newCondition();

   // number of new requests.
   private int numNewReq;

   // number of requests in all queues.
   private int numReq;

   // most priority queue.
   private ReqQueue mostPriorQueue;

   // least priority queue.
   private ReqQueue lastPriorQueue;

   // number that describes the % of change accepted for management.
   private double changePercentage; 
  
   // % of number of req that defines the # of queues.
   private double percentNumQueues;

   // % of new requests for manager queues.
   private float percent4Manage;

   // if shutdown has already been called
   private boolean shutdownFlag;

   // mediator instace
   protected DataAwareManagerMediator fmediator;

   /**
     * Contructor. Initialize priority queues.
     */
   public PriorityQueueReqForwarder(DataAwareManagerMediator dwareManMed) {
      pqueue = Collections.synchronizedList(new LinkedList());

      // register to mediator
      if (dwareManMed != null) {
         dwareManMed.registerReqForwarder(this);
         fmediator = dwareManMed;
      }

      // % approach -> when the number of new elements are greater than some
      // value
      // manages
      numNewReq = 0;
      numReq = 0;
      shutdownFlag = false;

      mostPriorQueue = new ReqQueue();
      pqueue.add(mostPriorQueue); // initial queue

      threshold = new TLongArrayList();

      // magic numbers!!!
      // default value, if the number of queues increase descrease this value...
      changePercentage = CHANGE_PERCENT_INI_VAL;
      percentNumQueues = PERCENT_NUM_QUEUES;
      percent4Manage = PERCENT_FOR_MANAGE;
   }

   /*
     * (non-Javadoc)
     * 
     * @see br.com.auster.dware.manager.ReqForwarderInterface#configure(org.w3c.dom.Element)
     */
   public void configure(Element config) {
      if (config != null) {
         AusterManagementServices.registerMBean(true, config, this.getClass(), this);
      }      
   }

   /*
     * Return queue pointer given a weight. This is NOT synchronized, take care.
     * @param wei Maximum request weight.
     */
   private final int getQueueIndex(long wei) {
      final int tsize = threshold.size();
      if (tsize == 0)
         return 0;
      // linear search, if there is many queues this may interfere in algorithm
      // performance
      // and some other searching algorithm, like binary search, must be
      // implemented.
      int i = tsize - 1;
      while (i >= 0 && wei < threshold.getQuick(i))
         i--;
      return i + 1;
   }

   /**
     * This method must implement how to store new requests.
     * 
     * @param newReq
     *          new request.
     */
  public void addNewReq(Request newReq) {
    this.queueLock.lock();
    try {
      if ( checkIfNotLoaded(newReq) && addReq(newReq) ) {
        this.fmediator.reqQueued(newReq); // let mediator knows
        this.queueNotEmptyCondition.signal();
      }
    } finally {
      this.queueLock.unlock();
    }
  }
  
  /**
   * 
   */
  public void addRetryReq(Request req) {
    this.queueLock.lock();
    try {
      if ( checkIfNotLoaded(req) && addReq(req) ) {
        this.fmediator.reqRequeued(req); // let mediator knows
        this.queueNotEmptyCondition.signal();
      }
    } finally {
      this.queueLock.unlock();
    }
  }
   
   /**
     * {@inheritDoc}
     */
  public void addNewReqs(Collection<Request> newReqs) {
    this.queueLock.lock();
    try {
      for (Request req : newReqs) {
        addNewReq(req);
      }
    } finally {
      this.queueLock.unlock();
    }
  }
  
  /**
   * Checks if a request was not already loaded to this queue. This
   * method will delegate the decision to the mediator.
   * 
   * @param req the request to check.
   * @return <code>true</code> if the request wasn't already loaded,
   *          <code>false</code> otherwise.
   */
  private boolean checkIfNotLoaded(Request req) {
    if (fmediator != null && fmediator.checkIfReqLoaded(req)) {
      log.warn(i18n.getString("reqAlreadyLoaded", req.getId()));
      return false;
    }
    return true;
  }

   /**
     * This method must implement how to store new requests.
     * 
     * @param newReq
     *          new request.
     * @return <code>true</code> if the request was added to the queue,
     *         <code>false</code> otherwise.
     */
   protected boolean addReq(Request newReq) {
     this.queueLock.lock();
     try {
         ((ReqQueue) this.pqueue.get(getQueueIndex(newReq.getWeight()))).put(newReq);
         this.numReq++;
         this.numNewReq++;
         if (numNewReq > Math.round(this.percent4Manage * (float) this.numReq)) {
            manageList();
         }
         return true;
     } finally {
       this.queueLock.unlock();
     }
   }

   /**
    * This method chooses some request that is closest to the maximum value
    * asked and the priority queue wish to send to balance their queues. If the
    * maximum weight requested is too small this method send the smallest
    * request it can send. Therefore, it will throw <code>
    * NoSuchElementException</code>
    * only when there isn't any element avaiable.
    * 
    * @param maxWeight
    *           maximum weight the request can have.
    * @exception NoSuchElementException
    *               when the priority queue is empty.
    */
   public Request chooseNextRequest(long maxWeight) throws NoSuchElementException {

      Request reqAux = null; // request that will be returned.
      boolean foundReq = false; // true when some fitable request is found.

      this.queueLock.lock();
      try {
         final int tsize = threshold.size();

         if (tsize == 0) {
            // this warrants that if there is only one queue no there is no
            // overhead.
            reqAux = ((ReqQueue) pqueue.get(0)).get();
            foundReq = true;
         } else { // there is more than one queue so I must search in correct
                  // one.
            ReqQueue qaux = mostPriorQueue; // current queue
            int qindex; // queue current index
            int avaiableIndex = tsize + 1; // last index that will be used if
                                             // no
            // queue fit wish
            int tindex; // threshold for current queue

            // while an request that fit wish is not found or all the queues
            // were
            // checked
            while (qaux != null && !foundReq) {

               // update index
               qindex = qaux.getMyIndex();

               if (log.isDebugEnabled())
                  log.debug(" Searching request in queue " + qindex);

               // check if current queue is empty
               if (qaux.size() == 0) {
                  qaux = qaux.getPrevPrior();
                  continue;
               }

               // if is the last queue set threshold index for last too
               if (qindex == tsize)
                  tindex = qindex - 1;
               else
                  tindex = qindex;

               if (log.isDebugEnabled())
                  log.debug(" compare: max wei (" + maxWeight + ") > threshold ("
                        + threshold.getQuick(tindex) + ")? queue size=" + qaux.size());

               // check if the weight if the requests of this queue satisfy the
               // weight expected
               if (maxWeight > threshold.getQuick(tindex)) {
                  reqAux = qaux.get();
                  foundReq = true;
               } else if (qindex < avaiableIndex) { // used to store the best
                                                      // queue
                  // is no good one is found
                  avaiableIndex = qindex;
                  if (log.isDebugEnabled())
                     log.debug(" set queue avaiable but not perfect = " + avaiableIndex);
               }

               // go to next queue in priority
               qaux = qaux.getPrevPrior();
            }

            // if no good queue is found use the "least worse"
            if (!foundReq && avaiableIndex != tsize + 1) {
               if (log.isDebugEnabled())
                  log.debug(" no good queue not found... set " + avaiableIndex);
               reqAux = ((ReqQueue) pqueue.get(avaiableIndex)).get();
               foundReq = true;
            }
         }
      } finally {
        this.queueLock.unlock();
      }

      // return request or throw exception...
      if (foundReq) {
         numReq--; // this is important for management algorithm
         return reqAux;
      } else {
         throw new NoSuchElementException();
      }
   }

   /**
    * Return the weight that the priority queue wish that someone ask him.
    * 
    * @return the weight that the priority queue wish that someone ask him.
    */
   public long getWishWeight() {
      int auxIndex = -1;
      this.queueLock.lock();
      try {
         if ((threshold.size() == 0) && ((ReqQueue) pqueue.get(0)).size() != 0)
            auxIndex = 0;
         else {
            ReqQueue qaux = mostPriorQueue;
            while (qaux != null) {
               if (qaux.size() != 0) {
                  auxIndex = qaux.getMyIndex();
                  break;
               }
               qaux = qaux.getPrevPrior();
            }
         }
      } finally {
        this.queueLock.unlock();
      }
      
      if (auxIndex == -1) {
        // no element
        return 0;
      }
      
      return Math.round(((ReqQueue) pqueue.get(auxIndex)).getAvgWeight());
   }

   /**
    * {@inheritDoc}
    */
   public Lock getSyncObj() {
      return this.queueLock;
   }
   
   /**
    * {@inheritDoc}
    */
   public Condition getQueueNotEmptyCondition() {
     return this.queueNotEmptyCondition;
   }

   /*
    * Tries to balance the queues.
    */
   private final void manageList() {

      int qsize = pqueue.size();
      if (qsize == 0)
         return;

      log.debug("Priority queue management started.");

      // calculate total elements
      float totalWei = 0;
      for (int i = 0; i < qsize; i++)
         totalWei += ((ReqQueue) pqueue.get(i)).getTotalWeight();

      int qexpected = (int) Math.ceil((double) numReq * percentNumQueues);

      // avg number of elements
      float avgWei = totalWei / (float) qexpected;// (int)Math.ceil((float)totalWei/(float)qsize);

      if (qexpected != 1)
         changePercentage += (float) (qsize - qexpected) * PERCENT_CHANGE_OFFSET;

      float avgWeiMax = avgWei * (float) changePercentage;

      // print priority stack
      /*
       * if (log.isDebugEnabled()) {
       * log.debug("----------------------------------------");
       * log.debug("Priority queue before management:"); for (int i = 0; i <
       * pqueue.size(); i++) { // for all queues
       * ((ReqQueue)pqueue.get(i)).printElements(); } log.debug("Thresholds:");
       * if (threshold != null) for (int i = 0; i < threshold.size(); i++)
       * log.debug("t["+i+"] = "+threshold.getQuick(i)); log.debug(" qsize =
       * "+qsize+" qexpected = "+qexpected); log.debug(" avgWeight = "+avgWei+"
       * weight4Management = "+avgWeiMax+ "
       * changePercentage="+changePercentage);
       * log.debug("----------------------------------------"); }
       */

      long weiAux;
      ReqQueue currReq;

      threshold.clear();

      // balance queues
      for (int i = 0; i < pqueue.size(); i++) {

         // update queue info
         currReq = (ReqQueue) pqueue.get(i);
         weiAux = currReq.getTotalWeight();

         // if some queue is bigger than 2x avg num elem separe
         if (weiAux >= avgWeiMax || (qsize == 1 && totalWei > 1)) { // or it
                                                                     // only
            // have one
            // queue with
            // more than 1
            // element...
            if (trySplitQueue(threshold, currReq.getAvgWeight(), i))
               i++; // a new queue was added
            else
               threshold.add(Math.round(currReq.getAvgWeight()));
         }

         // if some queue and it's next are smaller than half of avg num elem
         // concat queues
         else if (weiAux <= avgWei && i < qsize - 1) {
            long totAux = weiAux;
            int numCat = i + 1;
            ReqQueue rqaux;
            // sum number of elements and concat while the sum is bellow avg
            while (numCat < pqueue.size() && totAux < avgWei) {
               rqaux = (ReqQueue) pqueue.get(numCat);
               totAux += rqaux.getTotalWeight();// size();
               if (totAux < avgWei) {
                  rqaux.prepareRemove(); // manage links before remove
                  currReq.append(((ReqQueue) pqueue.remove(numCat)));
                  // log.debug("appended next queue...");
                  // numCat++;
               }
            }
            threshold.add(Math.round(currReq.getAvgWeight()));
            // log.debug("queue avg wei="+currReq.getAvgWeight());
         }

         else { // queue doesn't change
            threshold.add(Math.round(currReq.getAvgWeight()));
            // log.debug("queue avg wei="+currReq.getAvgWeight());
         }
      }

      updateThresholds(threshold); // update threshold
      numNewReq = 0; // because I'd already managed the new ones.
   }

   /*
    * Tries to split a big queue.
    */
   private final boolean trySplitQueue(TLongArrayList thresholdList, float reqAvgWei,
         int i) {
      float lowerAvgWei, upperAvgWei, diffWei;

      // lower threshold
      int tcounter = i - 1;
      // try to find the last threshold that have value defined
      while (tcounter >= 0 && ((ReqQueue) pqueue.get(tcounter)).getAvgWeight() == 0)
         tcounter--;
      if (tcounter == -1) // if all before this are 0
         diffWei = (reqAvgWei / (i + 1)) * 0.35f; // use the avg value
      else
         // found one not 0 use it as base, use this value to calculate avg
         diffWei = ((reqAvgWei - ((ReqQueue) pqueue.get(tcounter)).getAvgWeight()) / (i - tcounter)) * 0.35f;
      lowerAvgWei = reqAvgWei - diffWei; // define lower threshold

      // upper threshold
      tcounter = i + 1;
      // same idea, try to find some upper threshold defined
      while (tcounter < pqueue.size()
            && ((ReqQueue) pqueue.get(tcounter)).getAvgWeight() == 0)
         tcounter++;
      if (tcounter == pqueue.size()) // there is none threshold defined, use
                                       // avg
         diffWei = (reqAvgWei / (tcounter - i)) * 0.35f;
      else
         // use upper threshold
         diffWei = ((((ReqQueue) pqueue.get(tcounter)).getAvgWeight() - reqAvgWei) / (tcounter - i)) * 0.35f;
      upperAvgWei = reqAvgWei + diffWei; // define upper threshold

      // compare thresholds
      // log.debug("New COMPARE: %="+(avgWei*PERCENT_DIFF_WEIGHT_QUEUES)+" <->
      // diff="+
      // Math.abs((upperAvgWei-lowerAvgWei)));
      if (reqAvgWei * PERCENT_DIFF_WEIGHT_QUEUES < Math.abs((upperAvgWei - lowerAvgWei))) {

         // set thresholds
         thresholdList.add(Math.round(lowerAvgWei));
         thresholdList.add(Math.round(upperAvgWei));
         /*
          * if (log.isDebugEnabled()) { log.debug("found a big queue... split");
          * log.debug("lower queue avg wei="+lowerAvgWei); log.debug("upper
          * queue avg wei="+upperAvgWei); }
          */

         // add new queue
         pqueue.add(i, new ReqQueue());
         return true;
      }
      return false;
   }

   /*
    * Updates the threshold given average weight of queues.
    */
   private final void updateThresholds(TLongArrayList thresholdList) {
      // updates threshold array
      int zeroCount;
      long diffThres, iniVal;

      // calculate thresholds
      int tsize = thresholdList.size() - 1;
      for (int i = 0; i < tsize; i++) {
         iniVal = thresholdList.getQuick(i);
         zeroCount = 1;
         while ((i + zeroCount < tsize) && thresholdList.getQuick(i + zeroCount) == 0) { // check
            // is
            // there
            // is
            // empty
            // queues
            zeroCount++;
         }
         // threshold = avg value between the avg of weights of two consecutive
         // queues
         diffThres = (thresholdList.getQuick(i + zeroCount) - iniVal) / (zeroCount + 1);
         for (int k = 0; k < zeroCount; k++) {
            // threshold[i] = iniVal + (k+1)*diffThres;
            thresholdList.setQuick(i, iniVal + (k + 1) * diffThres);
            i++;
         }
      }
      thresholdList.remove(tsize);

      tsize--;
      // if there is threshold that is bigger than its next concat them
      for (int i = 0; i < tsize; i++) {
         if (thresholdList.getQuick(i) > thresholdList.getQuick(i + 1)) {
            ((ReqQueue) pqueue.get(i)).append(((ReqQueue) pqueue.remove(i + 1)));
            thresholdList.setQuick(i,
                  Math.round(((ReqQueue) pqueue.get(i)).getAvgWeight()));
            thresholdList.remove(i + 1);
            tsize--;
            i--;
         }
      }

      // manages index
      final int qsize = pqueue.size();
      for (int i = 0; i < qsize; i++)
         ((ReqQueue) pqueue.get(i)).setMyIndex(i);

      // print priority stack
      if (log.isDebugEnabled()) {
         log.debug("----------------------------------------");
         log.debug("Priority queue after management:");
         for (int i = 0; i < qsize; i++) { // for all queues
            ((ReqQueue) pqueue.get(i)).printElements();
         }
         log.debug("Thresholds:");
         for (int i = 0; i < threshold.size(); i++)
            log.debug("t[" + i + "] = " + threshold.getQuick(i));
         log.debug("----------------------------------------");
      }
      // }
   }

   /*
    * Tries to balance the queues.
    */
   private final void manageShutdownList() {

      int qsize = pqueue.size();
      if (qsize == 0)
         return;

      log.debug("Priority queue management started.");

      // calculate total elements
      float totalWei = 0;
      for (int i = 0; i < qsize; i++)
         totalWei += ((ReqQueue) pqueue.get(i)).getTotalWeight();

      int qexpected = (int) Math.ceil((double) numReq * percentNumQueues);

      // avg number of elements
      float avgWei = totalWei / (float) qexpected;// (int)Math.ceil((float)totalWei/(float)qsize);

      if (qexpected != 1)
         changePercentage += (float) (qsize - qexpected) * PERCENT_CHANGE_OFFSET;

      float avgWeiMax = avgWei * (float) changePercentage;

      // balance queues
      long weiAux;
      ReqQueue currReq;

      threshold.clear();
      for (int i = 0; i < pqueue.size(); i++) {
         currReq = (ReqQueue) pqueue.get(i);
         weiAux = currReq.getTotalWeight();
         // if some queue and it's next are smaller than half of avg num elem
         // concat queues
         if (weiAux <= avgWei && i < qsize - 1) {
            long totAux = weiAux;
            int numCat = 1;
            ReqQueue rqaux;
            // sum number of elements and concat while the sum is bellow avg
            while (i + numCat < pqueue.size() && totAux < avgWei) {
               rqaux = (ReqQueue) pqueue.get(i + numCat);
               totAux += rqaux.getTotalWeight();// size();
               if (totAux < avgWei) {
                  rqaux.prepareRemove(); // manage links before remove
                  currReq.append(((ReqQueue) pqueue.remove(i + numCat)));
                  // log.debug("appended next queue...");
                  // numCat++;
               }
            }
            threshold.add(Math.round(currReq.getAvgWeight()));
            // log.debug("queue avg wei="+currReq.getAvgWeight());
         } else { // queue dont change
            threshold.add(Math.round(currReq.getAvgWeight()));
            // log.debug("queue avg wei="+currReq.getAvgWeight());
         }
      }

      updateThresholds(threshold);
   }

   /**
    * Stop permanently management of priority list.
    */
   public void shutdown() {
      shutdownFlag = true;
   }

   public List getQueueAsList() {
      List list = new ArrayList();
      List tempList = pqueue.subList(0, pqueue.size() );
      System.out.println("SubList was built");
      for (Iterator itr=tempList.iterator();itr.hasNext();) {
         System.out.println("About to request REQUEUE");
         ReqQueue fila = (ReqQueue) itr.next();
         System.out.println("About to ADD ALL");
         list.addAll((Collection) fila.getReqQueue());
         System.out.println("Added ALL");
      }
      return list;
   }

   /*
    * Defines a request queue with total weight information. It have pointers to
    * previous and next elements that are ordered by the queue total weight.
    * When put and get methods are called this object treat the priority list
    * order. This is used to the Priority queue class decide which queue he want
    * to use to balance them.
    */
   class ReqQueue {

      private final SyncQueue reqQueue = new SyncQueue();

      private long totalWeight;

      private ReqQueue prevPrior, nextPrior;

      private int myIndex;

      /**
       * Constructor.
       */
      public ReqQueue() {
         totalWeight = 0;
         addLastPriority();
      }

      /**
       * Return total weight of this queue.
       * 
       * @return total weight of this queue.
       */
      public long getTotalWeight() {
         return totalWeight;
      }

      /**
       * Return the number of requests in the queue.
       * 
       * @return the number of requests in the queue.
       */
      public int size() {
         return reqQueue.size();
      }

      /**
       * Return the <code>Queue</code> object.
       * 
       * @return the <code>Queue</code> object.
       */
      public SyncQueue getReqQueue() {
         return reqQueue;
      }

      /**
       * Return the average weight of this queue.
       * 
       * @return the average weight of this queue.
       */
      public float getAvgWeight() {
         if (reqQueue.size() == 0)
            return 0;
         return (float) totalWeight / (float) reqQueue.size();
      }

      /**
       * Put an item in the queue. A notify is sent to the sync object.
       * 
       * @param item
       *           an item
       */
      public final void put(Request item) {
         totalWeight += item.getWeight();
         reqQueue.put(item);
         manageAdd();
      }

      /**
       * Verifies is total weight of this queue is still smaller than his next.
       * If it doesn't change position and call this method recursivaly.
       */
      private final void manageAdd() {
         if (this.nextPrior != null
               && this.nextPrior.getTotalWeight() < this.getTotalWeight()) {
            // pointer changes this became greater than next
            nextPrior.setPrevPrior(this.prevPrior);
            this.setPrevPrior(nextPrior);
            ReqQueue next = nextPrior.getNextPrior();
            nextPrior.setNextPrior(this);
            this.setNextPrior(next);
            // next and prev must know that the order has changed
            if (next != null)
               next.setPrevPrior(this);
            if (prevPrior.getPrevPrior() != null)
               prevPrior.getPrevPrior().setNextPrior(prevPrior);
            manageAdd();
         }
      }

      /**
       * Remove and return the first item of the queue.
       * 
       * @return The first item of the queue.
       * @throws NoSuchElementException
       *            if the queue is empty.
       */
      public final Request get() throws NoSuchElementException {

         Request raux = (Request) reqQueue.get();
         totalWeight -= raux.getWeight();
         manageRem();
         // manages list to decrease only
         if (shutdownFlag && totalWeight == 0) {
            manageShutdownList();
         }
         return raux;
      }

      /**
       * Verifies is total weight of this queue is still greater than his
       * previous. If it doesn't change position and call this method
       * recursivaly.
       */
      private final void manageRem() {
         if (this.prevPrior != null
               && this.prevPrior.getTotalWeight() > this.getTotalWeight()) {
            // pointer changes this became smaller than prev
            this.prevPrior.setNextPrior(this.nextPrior);
            ReqQueue prev = this.prevPrior.getPrevPrior();
            this.prevPrior.setPrevPrior(this);
            this.setNextPrior(this.prevPrior);
            this.setPrevPrior(prev);
            // next and prev must know that the order has changed
            if (nextPrior.getNextPrior() != null)
               nextPrior.getNextPrior().setPrevPrior(nextPrior);
            if (prevPrior != null)
               prevPrior.setNextPrior(this);
            manageRem();
         }
      }

      /**
       * Appends a request queue to this. Weight is automatically updated.
       * 
       * @param q
       *           queue to be appended with this.
       */
      public final boolean append(ReqQueue q) {
         boolean appOk = true;
         if (q.size() != 0) {
            // sum total weight
            totalWeight += q.getTotalWeight();// (totalWeight*size()+q.getTotalWeight()*q.size())/(size()+q.size());
            // append
            appOk = reqQueue.append(q.getReqQueue());
            // mantain links
            q.prepareRemove();
            this.manageAdd();
         }
         return true;
      }

      /**
       * Print elements in log if debug is enabled.
       */
      public void printElements() {
         try {
            if (log.isDebugEnabled()) {
               StringBuffer sb = new StringBuffer();
               sb.append("queue ");
               sb.append(myIndex);
               sb.append(" (");
               if (prevPrior != null)
                  sb.append(prevPrior.getMyIndex());
               sb.append(",");
               if (nextPrior != null)
                  sb.append(nextPrior.getMyIndex());
               sb.append(") W:");
               sb.append(totalWeight);
               sb.append(" size:");
               sb.append(size());
               sb.append(" -> || ");
               for (int j = 0; j < reqQueue.size(); j++) {
                  Request req = (Request) reqQueue.elementAt(j);
                  sb.append(" ");
                  sb.append(req.getId());
                  sb.append(":");
                  sb.append(req); // nao tem get...
               }
               sb.append(" ||");
               log.debug(sb.toString());
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      public ReqQueue getNextPrior() {
         return nextPrior;
      }

      public ReqQueue getPrevPrior() {
         return prevPrior;
      }

      public void setNextPrior(ReqQueue q) {
         if (q == null)
            mostPriorQueue = this;
         nextPrior = q;
      }

      public void setPrevPrior(ReqQueue q) {
         if (q == null)
            lastPriorQueue = this;
         prevPrior = q;
      }

      public void prepareRemove() {
         if (nextPrior != null)
            nextPrior.setPrevPrior(prevPrior);
         if (prevPrior != null)
            prevPrior.setNextPrior(nextPrior);
      }

      public void addLastPriority() {
         if (lastPriorQueue != null)
            lastPriorQueue.setPrevPrior(this);
         this.nextPrior = lastPriorQueue;
         lastPriorQueue = this;
      }

      public int getMyIndex() {
         return myIndex;
      }

      public void setMyIndex(int index) {
         myIndex = index;
      }
   }

}
