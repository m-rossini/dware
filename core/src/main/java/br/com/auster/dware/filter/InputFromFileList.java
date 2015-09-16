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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;
import br.com.auster.dware.request.file.FileListRequest;

/**
 * This filter is used to read a list of files, defined in the request, and to
 * write them (appended) to the output stream.
 * 
 * @version $Id: InputFromFileList.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class InputFromFileList extends ThreadedFilter {

  protected static final String BUFFER_SIZE_ATTR = "buffer-size";

  // Instance variables
  protected WritableByteChannel writer;

  protected FileListRequest request;

  protected ByteBuffer buffer;

  protected final static Logger log = Logger.getLogger(InputFromFileList.class);

  private final I18n i18n = I18n.getInstance(InputFromFileList.class);

  public InputFromFileList(String name) {
    super(name);
  }

  /**
   * Configures this filter
   */
  public synchronized void configure(Element config) throws FilterException {
    this.buffer = ByteBuffer.allocateDirect(DOMUtils
        .getIntAttribute(config, BUFFER_SIZE_ATTR, true));
  }

  /**
   * Opens the file for reading as the input for the request to be processed.
   * 
   * @param request
   *          the request to be processed.
   */
  public void prepare(Request request) throws FilterException {
    this.request = (FileListRequest) request;
  }

  public void process() throws FilterException {
    ReadableByteChannel input = null;
    try {
      File[] files = this.request.getFiles();
      for (int i = 0; i < files.length; i++) {
        File file = files[i];
        input = this.getInputStream(file);
        // Prints the header for this file, if any.
        this.printHeader(file);
        // Copies the input to the writer
        this.copyStream(input);
        input.close();
        // Prints the footer for this file, if any.
        this.printFooter(file);
      }
    } catch (IOException e) {
      if (input != null) {
        try {
          input.close();
        } catch (IOException e1) {
          log.error(i18n.getString("problemClosingReader"), e1);
        }
      }
      throw new FilterException(e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        throw new FilterException(i18n.getString("problemClosingWriter"), e);
      }
      this.writer = null;
    }
  }

  /**
   * Prints a header based on this file to the output buffer. This
   * implementation do nothing.
   */
  protected void printHeader(File file) throws IOException {
  }

  /**
   * Prints a footer based on this file to the output buffer. This
   * implementation do nothing.
   */
  protected void printFooter(File file) throws IOException {
  }

  /**
   * Gets an input stream from the file to print it to the output.
   */
  protected ReadableByteChannel getInputStream(File file) throws IOException {
    log.info(i18n.getString("setInputRequest", request, file.getAbsolutePath()));
    return NIOUtils.openFileForRead(file);
  }

  /**
   * Reads all the input data to the writer.
   */
  protected void copyStream(ReadableByteChannel input) throws IOException {
    NIOUtils.copyStream(input, this.writer, this.buffer);
  }

  /**
   * Closes the output.
   */
  public void rollback() {
    try {
      if (writer != null)
        writer.close();
    } catch (IOException e) {
      log.fatal(e.getMessage(), e);
    }
  }

  /**
   * Sets the writer for this filter, for not using a pipe.
   */
  public void setOutput(String sinkName, Object output) {
    this.writer = (WritableByteChannel) output;
  }
}
