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

import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.w3c.dom.Element;

import br.com.auster.dware.graph.Request;

/**
 * Interface that defines how objects that manage requests should store or
 * return a request.
 * 
 * 
 * @version $Id: ReqForwarderInterface.java 272 2006-11-30 20:08:39Z rbarone $
 */
public interface ReqForwarderInterface {

   public void configure(Element config);
  
   /**
   * This method must implement how to store new requests.
   * 
   * @param newReq
   *          new request.
   */
  public void addNewReq(Request newReq);

  /**
   * This method must implement how to store requests that
   * are being reinserted in the queue.
   * 
   * @param req
   *          the request being requeued.
   */
  public void addRetryReq(Request req);

  
  /**
   * This method must implement how to store new requests.
   * 
   * @param newReq
   *          a collection containing all new requests.
   */
  public void addNewReqs(Collection<Request> newReqs);
  
  /**
   * This method must return some request stored by the request forwarder given
   * the maximum weight it can have.
   * 
   * @param maxWeight
   *          maximum weight the request can have.
   */
  public Request chooseNextRequest(long maxWeight) throws NoSuchElementException;

  /**
   * Must return a pointer to a lock that must be used to synchronize all access
   * to the queue.
   * 
   * @return A lock that can be used to synchronize access to the queue and also
   *         use the {@link #getQueueNotEmptyCondition()} condition.
   */
  public Lock getSyncObj();
  
  /**
   * Must return a pointer to a condition, that must signal someone when some
   * request is added.
   * 
   * <p>
   * The lock for this condition can be retrieved by {@link #getSyncObj()}.
   * 
   * @return the condition that will be used to signal that the queue has
   *         received a new request.
   */
  public Condition getQueueNotEmptyCondition();

  /**
   * May implement some action when it knows that there isn't more requests to
   * add on queue.
   */
  public void shutdown();
}
