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
package br.com.auster.dware.test;

import br.com.auster.dware.graph.Request;

/**
 * This request is used to test the DataAware engine. It sleeps a little
 * milliseconds before finishing.
 * 
 * @version $Id: WaitRequest.java 356 2008-02-15 13:12:36Z lmorozow $
 */
public class WaitRequest extends Request {

  protected long waitTime;

  private int hashcode;

  public WaitRequest(long waitTime, int hashcode) {
    this.waitTime = waitTime;
    this.hashcode = hashcode;
    setUserKey(String.valueOf(hashcode));
  }

  public void waitTime() throws InterruptedException {
    Thread.sleep(this.waitTime);
  }

  public long getWeight() {
    return this.waitTime;
  }

  public String toString() {
    return "[WaitTime=" + this.waitTime + "ms]";
  }

  public int hashCode() {
    return this.hashcode;
  }
}
