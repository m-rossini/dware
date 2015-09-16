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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;

/**
 * This filter reads the input and writes the output doing no transformations.
 * The objective here is to replicate flat streams.
 * Once we have one input, we can define many outputs as desired.
 * The outputs are taken from connect-to element in the Graph Design Defintion.
 * As parameter and option, this filter accepts only buffer-size, whcih is self explanatory
 * 
 * How it works:
 * 
 * It is a Data-Aware {@link ThreadedFilter}, so it implements the process method.
 * 
 * In configure method the Buffer is allocated.
 * 
 * The interface with other filters are setInput and setOutput.
 * The setInput method defines the input and assumes the input is a {@link ReadableByteChannel},
 * 	so it needs improvement to accept streams also.
 * 
 * The setOutput adds all outputs to a Map (It implies the set behaves lika an add).
 *  Each output is where the input will be replicated to.
 * 	it assumes outputs are {@link WritableByteChannel}, and can be improved to accept streams also.
 * 
 * In the prepare method the buffer is cleaned
 * 
 * In process method data is read from input and replicated to each output
 * 
 * In commit the data is flushed and outputs, as well as, input are closed
 * 
 * In rollback it calls commit.
 * <p>
 * To set the input: setInput (ReadableByteChannel)
 * <p>
 * To set the output: setOutput(WritableByteChannel)
 * 
 */
public class FlatStreamReplicatorFilter extends ThreadedFilter {

  protected static final String BUFFER_SIZE_ATTR = "buffer-size";

  protected Map outputMap;

  protected ReadableByteChannel reader;

  protected ByteBuffer inBuffer;

  protected final static Logger log = Logger.getLogger(FlatStreamReplicatorFilter.class);

  private final I18n i18n = I18n.getInstance(FlatStreamReplicatorFilter.class);

	public FlatStreamReplicatorFilter(String name) {
    super(name);
  }

  /**
   * Configures this filter
   */
  public synchronized void configure(Element config) throws FilterException {
    this.inBuffer = ByteBuffer.allocateDirect(DOMUtils.getIntAttribute(config, BUFFER_SIZE_ATTR,
                                                                       true));
  }

  private void debugNIO(ByteBuffer bb, String msg) {
    log.info("***" + msg + "***");
    log.info("capacity=>" + bb.capacity());
    log.info("position=>" + bb.position());
    log.info("limit=>" + bb.limit());
    log.info("remaining=>" + bb.remaining());
  }

  public void commit() {    
    dataFlush();
  }

  /**
   * Closes the opened connections.
   */
  public void rollback() {
    dataFlush();
  }

  private void dataFlush() {
    Iterator itr = this.outputMap.values().iterator();
    while (itr.hasNext()) {
      WritableByteChannel writer = (WritableByteChannel) itr.next();
      try {
        if (writer != null && writer.isOpen())
          writer.close();
      } catch (IOException e) {
        log.error(i18n.getString("problemClosingWriter"), e);
      }
    }

    try {
      if (this.reader != null && this.reader.isOpen())
        this.reader.close();
    } catch (IOException e) {
      log.error(i18n.getString("problemClosingReader"), e);
    }
  }

  public synchronized void setInput(String sourceName, Object input) throws ConnectException {
    this.reader = (ReadableByteChannel) input;
  }

  public synchronized void setOutput(String sinkName, Object output) throws ConnectException {
    if (this.outputMap == null) {
      this.outputMap = new HashMap();
    }
    this.outputMap.put(sinkName, output);
  }

	/* (non-Javadoc)
   * @see br.com.auster.dware.graph.ThreadedFilter#process()
   */
  public void process() throws FilterException {
	  try {
	  	int read=0;
	  	while (NIOUtils.read(reader, inBuffer) != 0) {	  	
	    	for (Iterator itr = this.outputMap.values().iterator();itr.hasNext();) {
	    		WritableByteChannel writer = (WritableByteChannel) itr.next();
	    		writer.write(inBuffer);
	    		inBuffer.flip();
	    	}
	    }
    } catch (IOException e) {
	    e.printStackTrace();
    }
  }

	/* (non-Javadoc)
   * @see br.com.auster.dware.graph.ThreadedFilter#prepare(br.com.auster.dware.graph.Request)
   */
  public void prepare(Request request) throws FilterException {
  	this.inBuffer.clear();
  }
}