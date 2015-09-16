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

import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;

/**
 * This filter just sleeps the milliseconds set in the WaitRequest.
 * 
 * @version $Id: WaitFilter.java 356 2008-02-15 13:12:36Z lmorozow $
 */
public class WaitFilter extends ThreadedFilter {

  protected WaitRequest request;

  public WaitFilter(String name) {
    super(name);
  }

  public void prepare(Request request) {
    this.request = (WaitRequest) request;
  }

  public void process() throws FilterException {
    try {
      this.request.waitTime();
    } catch (InterruptedException e) {
      throw new FilterException(e);
    }
  }
}
