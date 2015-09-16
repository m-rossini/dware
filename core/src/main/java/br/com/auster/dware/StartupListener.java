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
 * Created on Jun 1, 2005
 */
package br.com.auster.dware;

import java.util.Collection;

import br.com.auster.dware.graph.Request;


/**
 * <p>
 * <b>Title:</b> StartupListener
 * </p>
 * <p>
 * <b>Description:</b> Data-Aware will notify all configured StartupListener
 * implementations when after initialization.
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2004-2005
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 * 
 * @author rbarone
 * @version $Id: StartupListener.java 272 2006-11-30 20:08:39Z rbarone $
 */
public interface StartupListener {

  /**
   * Called after Data-Aware boot, right before configuration phase.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will abort and exit.
   * 
   * @param instance
   *          The Data-Aware instance being initialized.
   * @param root
   *          DOM element that will be used to configure Data-Aware - changes
   *          made by the listener will affect Data-Aware.
   */
  public void beforeConfig(DataAware instance, org.w3c.dom.Element root);
  
  /**
   * Called after Data-Aware configuration phase.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will abort and exit.
   * 
   * @param instance
   *          The Data-Aware instance being initialized.
   * @param root
   *          DOM element that was used to configure all Graph instances -
   *          modifications will reflect on Data-Aware only if graphs are
   *          reconfigured by Data-Aware's GraphManager.
   */
  public void afterConfig(DataAware instance, org.w3c.dom.Element graphConfig);
  
  /**
   * Called before Data-Aware enqueues (or processes) a request.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will abort the
   * request with an error.
   * 
   * @param instance
   *          The Data-Aware instance initialized.
   * @param request
   *          The Request that will be enqueued or processed bypassing the Queue
   *          (modifications will reflect on Data-Aware).
   * @returns <code>true</code> if the request should be enqueued/processed,
   *          <code>false</code> otherwise.
   */
  public boolean beforeEnqueue(DataAware instance, Request request);
  
  /**
   * Called before Data-Aware enqueues (or processes) a list of requests.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will abort the
   * requests with an error.
   * 
   * @param instance
   *          The Data-Aware instance initialized.
   * @param requests
   *          The Requests that will be enqueued or processed bypassing the Queue
   *          (modifications will reflect on Data-Aware).
   * @returns <code>true</code> if the requests should be enqueued/processed,
   *          <code>false</code> otherwise.
   */
  public boolean beforeEnqueue(DataAware instance, Collection<Request> requests);
  
  /**
   * Called after Data-Aware enqueues (or processes) a request.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will just log the error and continue.
   * 
   * @param instance
   *          The Data-Aware instance initialized.
   * @param request
   *          The Request that was enqueued or processed bypassing the Queue.
   */
  public void afterEnqueue(DataAware instance, Request request);
  
  /**
   * Called after Data-Aware enqueues (or processes) a list of requests.
   * 
   * <p>
   * If any Exception is thrown by the listener, Data-Aware will just log the error and continue.
   * 
   * @param instance
   *          The Data-Aware instance initialized.
   * @param requests
   *          The Requests that were enqueued or processed bypassing the Queue.
   */
  public void afterEnqueue(DataAware instance, Collection<Request> requests);
  
}
