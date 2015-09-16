/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 03/09/2007
 */
package br.com.auster.dware.interrupt;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;

/**
 * @author framos
 * @version $Id$
 *
 */
public class ThreadSleepFilter extends ThreadedFilter {

	

	private static final Logger log = Logger.getLogger(ThreadSleepFilter.class);
	
	private int sleepTime;
	private ReadableByteChannel reader;
	

	
	public ThreadSleepFilter(String name) {
		super(name);
	}

	public void configure(Element config) throws FilterException {
		int time = DOMUtils.getIntAttribute(config, "sleep", false);
		if (time < 1) {
			time = 1;
		}
		sleepTime = time;
		log.info("Sleep time is " + sleepTime);
	}
	
	public void prepare(Request request) throws FilterException {
		log.info("Preparing for request " + request.getUserKey());
	}

	
	
	
	public void setInput(String filterName, Object input) throws ConnectException, UnsupportedOperationException {
		this.reader = (ReadableByteChannel) input;
	}

	public void process() throws FilterException {
		log.info("Will sleep by the time the first read operation returns");
		
		ByteBuffer dst = ByteBuffer.allocate(1024);
		int size = 0;
		try {
			int counter=1;
			while ((size = this.reader.read(dst)) > 0) {
				dst.rewind();
				log.info("Going to sleep after reading '" + dst.asCharBuffer().toString() + "'");
				Thread.sleep(this.sleepTime*1000);
				log.info("Woke up from sleep #" + counter++ );
			}
		} catch (InterruptedException ie) {
			log.error("Thread interrupted", ie);
			throw new FilterException(ie);
		} catch (IOException ioe) {
			log.error("IOException while reading", ioe);
			throw new FilterException(ioe);
		}
	}
}
