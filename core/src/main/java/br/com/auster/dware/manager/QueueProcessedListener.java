/*
 * Copyright (c) 2006 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on Nov 29, 2006
 */
package br.com.auster.dware.manager;

import org.w3c.dom.Element;

/**
 * Implementations of this interface that are configured in the Graph Manager
 * will be notified when a queue is empty (by it's transaction ID).
 * 
 * <p>
 * This will be done by a specific Thread responsible to communicate with
 * all listeners.
 * 
 * @author rbarone
 * @version $Id$
 */
public interface QueueProcessedListener {
  
  /**
   * This method will be called only once after instantiation to pass the
   * configuration root of the {@link GraphManager#LISTENER_ELEMENT} for this
   * listener.
   * 
   * @param config
   *          The configuration root element for this listener.
   */
  public void init(Element config);
  
  /**
   * This method will be called each time a queue's request count has reached
   * zero, that is, it's empty.
   * 
   * @param transactionId
   *          The transaction ID of the queue - it can be <code>null</code>,
   *          meaning that no transaction ID was configured for one or more
   *          requests.
   * @param size
   *          The size (request count) of the processed queue.
   */
  public void onQueueProcessed(String transactionId, int size);

}
