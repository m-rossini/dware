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
package br.com.auster.dware.manager.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.w3c.dom.Element;

import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.GraphException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.ManagerException;
import br.com.auster.dware.manager.RemoteGraphGroupInterface;

/**
 * Interface for objects that are on clients and agregate all graphs.
 * 
 * @version $Id: ClientRemoteGraphInterface.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface ClientRemoteGraphInterface extends Remote {

  /**
   * This method is called by server to configure the graph group.
   * 
   * @param name
   *          graph set name.
   * @param config
   *          DOM object with graph group configuration
   * @param max
   *          maximum number of graph this set can have.
   * @param callbackObj
   *          callback object.
   */
  public void configureGraph(String name,
                             Element config,
                             int max,
                             RemoteGraphGroupInterface callbackObj) throws RemoteException;

  /**
   * This method is called by server to REconfigures the filters inside the
   * graphs of this group.
   * 
   * @param name
   *          graph set name.
   * @param filterName
   *          the name of the filter that will be reconfigured.
   * @param config
   *          the DOM tree that has the filter configuration to be applied.
   */
  public void configureFilter(String name, String filterName, Element config)
      throws GraphException, FilterException, RemoteException;

  /**
   * Shutdown graphs from the set given by the name passed.
   * 
   * @param name
   *          graph set name.
   */
  public void shutdown(String name) throws RemoteException;

  /**
   * This method is called by server when some graph group will be down.
   */
  public void notifyServerDown(String name) throws RemoteException;

  /**
   * Forward a request to be processed by a graph set.
   * 
   * @param name
   *          graph set name.
   * @param request
   *          request to be processed.
   */
  public void process(String name, Request request) throws ManagerException, RemoteException;

  /**
   * Method used to check client connection by the server.
   */
  public void ping() throws RemoteException;

}
