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
package br.com.auster.dware.filter;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.file.FileRequest;

/**
 * This filter is used to read a request input from a file.
 * 
 * @version $Id: InputFromFile.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class InputFromFile extends DefaultFilter {

  protected static final String REMOVE_ATTR = "remove-after";

  // Instance variables
  protected volatile boolean remove;

  protected File file;

  protected ReadableByteChannel reader;

  protected final static Logger log = Logger.getLogger(InputFromFile.class);

  private final I18n i18n = I18n.getInstance(InputFromFile.class);

  public InputFromFile(String name) {
    super(name);
  }

  /**
   * Configure this filter.
   */
  public synchronized void configure(Element config) throws FilterException {
    // Specifies if the input file must be removed after processing (on commit)
    this.remove = DOMUtils.getBooleanAttribute(config, REMOVE_ATTR);
  }

  /**
   * Opens the file for reading as the input for the request to be processed.
   * 
   * @param request
   *          the request to be processed.
   */
  public void prepare(Request request) throws FilterException {
    try {
      this.file = this.getFile(request);
      log.info(i18n.getString("setInputRequest", request, this.file));
      this.reader = NIOUtils.openFileForRead(this.file);
    } catch (IOException e) {
      throw new FilterException(i18n.getString("problemOpenFile", request), e);
    }
  }

  /**
   * Creates the file that will be opened for reading.
   * 
   * @param request
   *          the file name will be defined using some request's properties.
   */
  protected File getFile(Request request) throws FilterException {
    return ((FileRequest) request).getFile();
  }

  /**
   * Closes the input.
   */
  public void commit() {
    this.rollback();

    // Decides if the file must be deleted or not.
    if (this.remove)
      if (this.file.delete())
        log.warn(i18n.getString("fileDeleted", this.file));
      else
        log.error(i18n.getString("problemDeleting", this.file));
  }

  /**
   * Closes the input.
   */
  public void rollback() {
    try {
      if (this.reader != null && this.reader.isOpen())
        this.reader.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingFile"), e);
    }
  }

  public Object getOutput(String sinkName) throws ConnectException, UnsupportedOperationException {
    return this.reader;
  }
}
