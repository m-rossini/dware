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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

import br.com.auster.dware.graph.Request;

/**
 * Interface for remote clients callback
 * 
 * @version $Id: RemoteGraphGroupInterface.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface RemoteGraphGroupInterface extends Remote {

  /**
   * Warn server that the request process has finished.
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
      throws RemoteException;

  /**
   * When the graph will commit a request processed, it calls this method
   * before.
   * 
   * @param request
   *          request processed.
   * @param graphName
   *          graph that will commit.
   */
  public void remoteGraphCommiting(Request request, String graphName) throws RemoteException;

  /**
   * When the graph will rollback a request processed, it calls this method
   * before.
   * 
   * @param request
   *          request processed.
   * @param graphName
   *          graph that will commit.
   * @param error
   *          exception throwed.
   */
  public void remoteGraphRollingBack(Request request, String graphName, Throwable error)
      throws RemoteException;

}
