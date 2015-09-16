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

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.log4j.Logger;

import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * This filter is used to create a pipe and connect two filters whose Source
 * needs a writer and Sink needs a reader.
 * 
 * <p>
 * To get the input: WritableByteChannel getInput(null)
 * <p>
 * To get the output: ReadableByteChannel getOutput(null)
 * 
 * @version $Id: PipeFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public final class PipeFilter extends DefaultFilter {

  // Instance variables
  protected ReadableByteChannel reader;

  protected WritableByteChannel writer;

  protected static final Logger log = Logger.getLogger(PipeFilter.class);

  private final I18n i18n = I18n.getInstance(PipeFilter.class);

  public PipeFilter(String name) {
    super(name);
  }

  /**
   * Opens the file for reading as the input for the request to be processed.
   * 
   * @param request
   *          the request to be processed.
   */
  public final void prepare(Request request) throws FilterException {
    try {
      Pipe pipe = Pipe.open();
      this.writer = pipe.sink();
      this.reader = pipe.source();
    } catch (IOException e) {
      throw new FilterException(i18n.getString("problemsPipe"), e);
    }
  }

  /**
   * Closes the input and output stream.
   */
  public final void commit() {
    this.rollback();
  }

  /**
   * Closes the input and output stream.
   */
  public final void rollback() {
    try {
      if (this.writer != null && this.writer.isOpen())
        this.writer.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingWriter", e.getMessage()), e);
    }

    try {
      if (this.reader != null && this.reader.isOpen())
        this.reader.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingReader", e.getMessage()), e);
    }
  }

  /**
   * Gets the pipe's reader.
   */
  public final Object getOutput(String filterName) throws ConnectException,
      UnsupportedOperationException {
    return this.reader;
  }

  /**
   * Gets the pipe's writer.
   */
  public final Object getInput(String filterName) throws ConnectException,
      UnsupportedOperationException {
    return this.writer;
  }
}
