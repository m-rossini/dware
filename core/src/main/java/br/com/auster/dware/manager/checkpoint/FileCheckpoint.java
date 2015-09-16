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
package br.com.auster.dware.manager.checkpoint;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.DataAwareManagerMediator;

/**
 * This class implements a file storage for checkpoint information.
 * 
 * @version $Id: FileCheckpoint.java 236 2006-08-29 19:17:32Z rbarone $
 */
public class FileCheckpoint extends AbstractCheckpoint {

  public static final String FILE_NAME = "file-name";

  private BufferedWriter writer; // writer.

  private static final Logger log = Logger.getLogger(FileCheckpoint.class);

  private File chkptFile; // checkpoint file object.

  // private final I18n i18n = I18n.getInstance(FileCheckpoint.class);

  /**
   * Creates a new instance of CheckpointFile
   * 
   * @param config
   *          configuration in a Element object.
   * @param _dwareManMed
   *          manager mediator.
   */
  public FileCheckpoint(Element config, DataAwareManagerMediator _dwareManMed) {
    super(config, _dwareManMed);

    chkptFile = new File(DOMUtils.getAttribute(config, FILE_NAME, true));
    try {
      // Opens the checkpoint/restart file for appending.
      this.writer = new BufferedWriter(new FileWriter(chkptFile, true));
      log.info(i18n.getString("checkPointEnabled", chkptFile.getCanonicalPath()));
    } catch (IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }
  }

  /**
   * Loads a set of processed requests from a file and return them on a
   * HashSet object.
   */
  protected HashSet initReqProcessedHash() {
    HashSet procHash = new HashSet();
    try {
      // Opens the checkpoint file and creates the set of already processed
      // requests.
      BufferedReader reader = new BufferedReader(new FileReader(chkptFile));
      String line = null;
      while ((line = reader.readLine()) != null) {
        procHash.add(line);
      }
      reader.close();
      log.info(i18n.getString("previousCheckPoint"));
    } catch (IOException e) {
      log.info(i18n.getString("noPreviousCheckPoint"));
    }
    return procHash;
  }

  /**
   * Loads a set of queued requests from a file and return them on a HashSet
   * object.
   */
  protected HashSet initReqQueuedHash() {
    return new HashSet();
  }

  /**
   * Loads a set of failed requests from a file and return them on a
   * HashMap object.
   */
  protected HashMap initReqFailedMap() {
    return new HashMap();
  }

  /**
   * Store in the checkpoint file the information of the request that was
   * processed.
   * 
   * @param req
   *          Request object that was processed.
   */
  protected void loadReqProcessed(Request req) {
    try {
      this.writer.write(req.getId());
      this.writer.newLine();
      this.writer.flush();
    } catch (IOException e) {
      log.warn(i18n.getString("problemWriteOutput", e), e);
    }
  }

  /**
   * No action implemented.
   * 
   * @param req
   *          Request object that failed.
   * @param isRetry
   *          <code>true</code> if Data-Aware will retry to process the request, 
   *          <code>false</code> otherwise.
   * @param failCount
   *          Number of times this request has failed in this execution.
   */
  protected void loadReqFailed(Request req, boolean isRetry, int failCount) {

  }

  /**
   * No action implemented.
   * 
   * @param req
   *          Request object queued.
   */
  protected void loadReqWillBeProcessed(Request req) {

  }

  /**
   * This method is called when this instance will be no more used.
   */
  public void shutdown() {
    try {
      this.writer.close();
    } catch (IOException e) {
      log.warn(i18n.getString("problemCloseOutput", e), e);
    }
  }
}
