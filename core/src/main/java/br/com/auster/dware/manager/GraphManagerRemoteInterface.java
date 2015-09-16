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

import org.w3c.dom.Element;

import br.com.auster.dware.manager.remote.ClientRemoteGraphInterface;

/**
 * TODO class comments
 * 
 * @version $Id: GraphManagerRemoteInterface.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface GraphManagerRemoteInterface extends Remote {

  /**
   * This method should instanciate a new RemoteGraphGroup to process requests.
   * 
   * @param remoteGraph
   *          instance of a client remote graph group.
   * @param config
   *          the configuration, as a DOM tree, of this remote group to be
   *          created.
   */
  public void registerClientRemoteGraph(ClientRemoteGraphInterface remoteGraph, Element config)
      throws RemoteException;

  /**
   * This method should be called when some remote graph group stop to act. This
   * graph group must be take out of the avaiable graph groups.
   * 
   * @param groupName
   *          the graph group to be stopped.
   * @param kill
   *          if true, the graph group will stop after finishing to process the
   *          request of that moment.
   * @param wait
   *          if true, this method will block until the graph group have
   *          finished.
   */
  public void unregisterClientRemoteGraph(String groupName, boolean kill, boolean wait)
      throws RemoteException;
}
