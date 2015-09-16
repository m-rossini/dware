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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

public class NullWriterFilter extends DefaultFilter {

	protected WritableByteChannel	writer;

	protected static final Logger	log	= Logger.getLogger(NullWriterFilter.class);

	public NullWriterFilter(String name) {
		super(name);
	}

	/**
	 * Configures this filter.
	 */
	public synchronized void configure(Element config) throws FilterException {

	}

	/**
	 * Opens the file for writing the output processed for the request.
	 * 
	 * @param request
	 *          the request to be processed.
	 */
	public void prepare(Request request) throws FilterException {
		this.writer = Channels.newChannel(new NullOutputStream());
	}

	/**
	 * Closes the output.
	 */
	public void commit() {

		try {
			if (this.writer != null && this.writer.isOpen()) {
				this.writer.close();
			}
		} catch (IOException e) {
			log.error("Error CLosing File", e);
		}

	}

	/**
	 * Closes the output.
	 */
	public void rollback() {
		this.commit();
	}

	/**
	 * Gets the writer to the file.
	 */
	public Object getInput(String sourceName) throws ConnectException,
			UnsupportedOperationException {
		return this.writer;
	}
}
