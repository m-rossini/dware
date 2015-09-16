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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ThreadedFilter;

/**
 * This filter reads the input and writes the output doing no transformations.
 * 
 * <p>
 * To set the input: setInput (ReadableByteChannel)
 * <p> 
 * To set the output: setOutput(WritableByteChannel)
 * 
 * @version $Id: NIOFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class NIOFilter extends ThreadedFilter {

  protected static final String BUFFER_SIZE_ATTR = "buffer-size";

  protected ByteBuffer buffer;

  protected ReadableByteChannel reader;

  protected WritableByteChannel writer;

  protected static final Logger log = Logger.getLogger(NIOFilter.class);

  private final I18n i18n = I18n.getInstance(NIOFilter.class);

  public NIOFilter(String name) {
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
   * Reads the reader, process the data read and writes the result to the
   * writer.
   * 
   * @param reader
   *          where the data comes
   * @param writer
   *          where the data goes to
   */
  public void process() throws FilterException {
    if (this.reader == null) {
      // This is not a bug. We just don't have any data to send.
      log.warn(i18n.getString("inputNotSet"));
      return;
    }

    if (this.writer == null) {
      // We have data to send, but don't have an output. This is a bug.
      throw new FilterException(i18n.getString("outputNotSet"));
    }

    try {
      ByteBuffer buffer = this.buffer;
      buffer.clear();
      NIOUtils.copyStream(reader, writer, buffer);
      writer.close();
      reader.close();
    } catch (IOException e) {
      throw new FilterException(e);
    }
  }

  /**
   * Closes the opened connections.
   */
  public void rollback() {
    try {
      if (this.writer != null && this.writer.isOpen())
        this.writer.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingWriter"), e);
    }
    try {
      if (this.reader != null && this.reader.isOpen())
        this.reader.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingReader"), e);
    }
  }

  /**
   * Sets the input.
   */
  public void setInput(String sourceName, Object input) throws ConnectException {
    this.reader = (ReadableByteChannel) input;
  }

  /**
   * Sets an output with name outputName.
   */
  public void setOutput(String sinkName, Object output) throws ConnectException {
    this.writer = (WritableByteChannel) output;
  }
}
