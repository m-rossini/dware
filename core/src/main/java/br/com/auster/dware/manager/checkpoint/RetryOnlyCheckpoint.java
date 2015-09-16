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
 * Created on Aug 9, 2005
 */
package br.com.auster.dware.manager.checkpoint;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.DataAwareManagerMediator;


/**
 * <p><b>Title:</b> RetryOnlyCheckpoint</p>
 * <p><b>Description:</b> TODO class description</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id: RetryOnlyCheckpoint.java 236 2006-08-29 19:17:32Z rbarone $
 */
public class RetryOnlyCheckpoint extends AbstractCheckpoint {
  
  private static final Logger log = Logger.getLogger(RetryOnlyCheckpoint.class);

  /**
   * @param config
   * @param _dwareManMed
   */
  public RetryOnlyCheckpoint(Element config, DataAwareManagerMediator _dwareManMed) {
    super(config, _dwareManMed);
    this.reqFailedMap = initReqFailedMap();
    this.reqProcessedHash = initReqProcessedHash();
    this.reqQueuedHash = initReqQueuedHash();
  }

  public boolean contains(Request req) {
    return false;
  }
  
  public void checkReqProcessed(Request req) {
    synchronized (this.syncObj) {
      // remove from failed list
      this.reqFailedMap.remove(new Integer(System.identityHashCode(req)));
    }
  }
  
  public void checkReqWillBeProcessed(Request req) {
    // do nothing
  }
  
  /**
   * @inheritDoc
   */
  protected HashSet initReqProcessedHash() {
    return new HashSet();
  }

  /**
   * @inheritDoc
   */
  protected HashSet initReqQueuedHash() {
    return new HashSet();
  }

  /**
   * @inheritDoc
   */
  protected HashMap initReqFailedMap() {
    return new HashMap();
  }

  /**
   * @inheritDoc
   */
  public void shutdown() {
    // do nothing
  }

  /**
   * @inheritDoc
   */
  protected void loadReqProcessed(Request req) {
    // do nothing
  }

  /**
   * @inheritDoc
   */
  protected void loadReqFailed(Request req, boolean isRetry, int failCount) {
    // do nothing
  }

  /**
   * @inheritDoc
   */
  protected void loadReqWillBeProcessed(Request req) {
    // do nothing
  }

}
