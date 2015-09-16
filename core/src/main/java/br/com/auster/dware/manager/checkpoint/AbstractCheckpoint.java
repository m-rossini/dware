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
package br.com.auster.dware.manager.checkpoint;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.DataAwareManagerMediator;

/**
 * Abstract class for checkpoint objects. Classes that extends this shoud
 * implement how to store and retrieve the checkpoint information.
 *
 * @version $Id: AbstractCheckpoint.java 326 2007-11-28 13:56:35Z framos $
 */
public abstract class AbstractCheckpoint {

  public static final String MAX_REQ_FAILS = "max-req-fails";
  public static final String REQUEUE_SLEEP_TIME = "requeue-sleep-milis";

  protected HashSet reqProcessedHash; // hashset of id of requests already
                                      // processed.

  protected HashSet reqQueuedHash; // hashset of id of requests queued.

  protected HashMap reqFailedMap; // hashmap of id/number of fails of requests.

  protected DataAwareManagerMediator dwareManMed; // mediator

  protected int maxNumReqFails; // maximum number of times a request can fail.

  protected long requeueSleepTime;

  private static final Logger log = Logger.getLogger(AbstractCheckpoint.class);

  protected final Object syncObj = new Object(); // synchronization object.

  protected final I18n i18n = I18n.getInstance(AbstractCheckpoint.class);

  /**
   * Constructor.
   *
   * @param config
   *          configuration in a Element object.
   * @param _dwareManMed
   *          manager mediator.
   */
  public AbstractCheckpoint(Element config, DataAwareManagerMediator _dwareManMed) {
    dwareManMed = _dwareManMed;
    maxNumReqFails = DOMUtils.getIntAttribute(config, MAX_REQ_FAILS, false);

    String sleepTime = DOMUtils.getAttribute(config, REQUEUE_SLEEP_TIME, false);
    if (sleepTime == null || sleepTime.length() == 0) {
      this.requeueSleepTime = -1;
    } else {
      this.requeueSleepTime = Long.parseLong(sleepTime);
    }

    // if mediator is null some integration problems WILL happen in this
    // package.
    // you can use null for unit tests or tring to use this in other packages.
    if (dwareManMed != null)
      dwareManMed.registerCheckpoint(this);
  }

  public long getRequeueSleepTime() {
    return this.requeueSleepTime;
  }

  private final void fillReqProcessedHash(NullPointerException e) {
    if (reqProcessedHash == null)
      reqProcessedHash = initReqProcessedHash();
    else
      throw e;
  }

  private final void fillReqQueuedHash(NullPointerException e) {
    if (reqQueuedHash == null)
      reqQueuedHash = initReqQueuedHash();
    else
      throw e;
  }

  private final void fillReqFailedMap(NullPointerException e) {
    if (reqFailedMap == null)
      reqFailedMap = initReqFailedMap();
    else
      throw e;
  }

  /**
   * Verifies if the request has already been processed or the checkpoint knows
   * it is in the list for processing.
   *
   * @param req
   *          Request object that will be check.
   * @return if the request has already been queued for processing.
   */
  public boolean contains(Request req) {
    String reqId = req.getId();

    synchronized (syncObj) {
      try {
        return reqProcessedHash.contains(reqId) || reqQueuedHash.contains(reqId);
      } catch (NullPointerException e) { // some set was not initialized
        fillReqProcessedHash(e);
        fillReqQueuedHash(e);
        return contains(req);
      }
    }
  }

  /**
   * Warn this class that some request was processed.
   *
   * @param req
   *          Request object that was processed.
   */
  public void checkReqProcessed(Request req) {
    String reqId = req.getId();

    synchronized (syncObj) {

      // call for custom action
      loadReqProcessed(req);

      try {
        // add to set
        reqProcessedHash.add(reqId);

        // it is not queue anymore
        reqQueuedHash.remove(reqId);
      } catch (NullPointerException e) { // some set was not initialized
        fillReqProcessedHash(e);
        fillReqQueuedHash(e);
        checkReqProcessed(req);
      }
    }
  }

  /**
   * Warn this class that the request has failed to process.
   *
   * @param req
   *          Request object that failed.
   * @param graphName
   *          graph name.
   * @param error
   *          if null, the request was processed
   * @return if the request fails less than the maximum number of fails allowed.
   */
  public boolean checkReqFailed(Request req, String graphName, Throwable error) {

    String reqId = req.getId();

    synchronized (syncObj) {
      try {
        int numFail = 1;
        if (reqFailedMap.containsKey(reqId)) {
          int valFail = ((Integer) reqFailedMap.get(reqId)).intValue();
          numFail = valFail + 1;
        }

        boolean isRetry;

        if (maxNumReqFails <= numFail) {
          isRetry = false;
          log.fatal(i18n.getString("processFailed", req, graphName), error);
        } else {
          isRetry = true;
          reqFailedMap.put(reqId, new Integer(numFail));
          log.warn(i18n.getString("processFailedTryAgain", req, graphName, Integer.toString(numFail)));
        }

        loadReqFailed(req, isRetry, numFail); // call for custom action
        // we need to remove from queued hash so that when retrying, this request will not be checked as "already loaded"
        reqQueuedHash.remove(reqId);
        return isRetry;
      } catch (NullPointerException e) { // some set was not initialized
        fillReqFailedMap(e);
        return checkReqFailed(req, graphName, error);
      }
    }
  }

  /**
   * Warn this class that the request is on list of requests that will be
   * processed.
   *
   * @param req
   *          Request object queued.
   */
  public void checkReqWillBeProcessed(Request req) {

    synchronized (syncObj) {
      // call for custom action
      loadReqWillBeProcessed(req);

      try {
        // add to set
        reqQueuedHash.add(req.getId());
      } catch (NullPointerException e) { // some set was not initialized
        fillReqQueuedHash(e);
        if (req != null)
          checkReqWillBeProcessed(req);
        else
          throw e;
      }
    }
  }

  /**
   * Must load a set of processed requests from the data source and return them
   * on a HashSet object.
   */
  protected abstract HashSet initReqProcessedHash();

  /**
   * Must load a set of queued requests from the data source and return them on
   * a HashSet object.
   */
  protected abstract HashSet initReqQueuedHash();

  /**
   * Must load a set of failed requests from the data source and return them on
   * a HashMap object.
   */
  protected abstract HashMap initReqFailedMap();

  /**
   * Handles shutdown of request processing. It must call methods for data
   * storage in some data source.
   */
  public abstract void shutdown();

  /**
   * Store in some data storage the information of the request that was
   * processed, if it is required.
   *
   * @param req
   *          Request object that was processed.
   */
  protected abstract void loadReqProcessed(Request req);

  /**
   * Store in some data storage the information of the request that has failed
   * to process, if it is required.
   *
   * @param req
   *          Request object that failed.
   * @param isRetry
   *          <code>true</code> if Data-Aware will retry to process the request,
   *          <code>false</code> otherwise.
   * @param failCount
   *          Number of times this request has failed in this execution.
   */
  protected abstract void loadReqFailed(Request req, boolean isRetry, int failCount);

  /**
   * Store in some data storage the information of the request that will be
   * processed, if it is required.
   *
   * @param req
   *          Request object queued.
   */
  protected abstract void loadReqWillBeProcessed(Request req);

}
