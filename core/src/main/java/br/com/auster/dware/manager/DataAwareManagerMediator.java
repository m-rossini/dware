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

import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.checkpoint.AbstractCheckpoint;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * This class defines a Mediator for manager objects.
 * 
 * @version $Id: DataAwareManagerMediator.java 272 2006-11-30 20:08:39Z rbarone $
 */
public interface DataAwareManagerMediator {

  /**
   * Register a <code>GraphManager</code> object to this mediator.
   * 
   * @param gm
   *          <code>GraphManager</code> object.
   */
  public void registerGraphManager(GraphManager gm);

  /**
   * Register a <code>ReqForwarderInterface</code> object to this mediator.
   * 
   * @param _reqFwd
   *          <code>ReqForwarderInterface</code> object.
   */
  public void registerReqForwarder(ReqForwarderInterface _reqFwd);

  /**
   * Register a <code>AbstractCheckpoint</code> object to this mediator.
   * 
   * @param _chkPt
   *          <code>AbstractCheckpoint</code> object.
   */
  public void registerCheckpoint(AbstractCheckpoint _chkPt);

  /**
   * Register a <code>GraphGroup</code> object to this mediator.
   * 
   * @param graphGp
   *          <code>GraphGroup</code> object.
   */
  public void registerGraphGroup(GraphGroup graphGp);

  /**
   * This must be called when some request finish to process.
   * 
   * @param req
   *          request object.
   */
  public void reqProcessed(Request req);

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
  public void reqFailed(Request req, String graphName, Throwable error);

  /**
   * This must be called when some request was enqueued.
   * 
   * @param req
   *          request object.
   */
  public void reqQueued(Request req);
  
  /**
   * This must be called when some request was requeued.
   * 
   * @param req
   *          request object.
   */
  public void reqRequeued(Request req);

  /**
   * Check if the request has been already loaded.
   * 
   * @param req
   *          request object.
   */
  public boolean checkIfReqLoaded(Request req);

  /**
   * Must handle when some graph group stops to act for some reason.
   * 
   * @param graphGpName
   *          nome of the graph group that will be unloaded.
   */
  public void unregisterGraphGroup(String graphGpName);
  
  /**
   * Shutdown the mediator.
   */
  public void shutdown();
  
  public JMXGraphGroupCounter getJMXCounters();

}
