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
package br.com.auster.dware.request.net;

import java.net.Socket;
import java.nio.channels.SocketChannel;

import br.com.auster.dware.graph.Request;

/**
 * This class represents a socket request to be processed by the graph.
 * 
 * @version $Id: SocketRequest.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public class SocketRequest extends Request {

  protected final SocketChannel socket;

  private long wei;

  public SocketRequest(SocketChannel socket) {
    this.socket = socket;
    wei = 1;

    Socket s = socket.socket();
    setUserKey(s.getInetAddress().getHostAddress() + ":" + s.getPort());
  }

  public long getWeight() {
    return wei;
  }

  public void setWeight(long _wei) {
    wei = _wei;
  }

  public String toString() {
    return "[SocketRequest: " + this.socket + "]";
  }

  public int hashCode() {
    return this.socket.hashCode();
  }

  public SocketChannel getSocket() {
    return this.socket;
  }
}
