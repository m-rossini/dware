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
package br.com.auster.dware.graph;

import java.util.Date;

/**
 * This class is used by the graph as a listener object. It is called when a
 * graph finishes to process a request.
 * 
 * @version $Id: FinishListener.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface FinishListener {

  /**
   * When the graph finishes to process a request, it calls this method after.
   */
  public void graphFinished(Graph graph, Request request, Throwable error, Date time)
      throws Exception;

  /**
   * When the graph will commit a request processed, it calls this method
   * before.
   */
  public void graphCommiting(Graph graph, Request request) throws Exception;

  /**
   * When the graph will rollback a request processed, it calls this method
   * before.
   */
  public void graphRollingBack(Graph graph, Request request, Throwable error) throws Exception;

}
