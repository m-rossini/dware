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
 * Created on 29/11/2007
 */
package br.com.auster.dware.listeners.resume;

import java.io.File;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.StartupListener;
import br.com.auster.dware.graph.Request;

import com.sun.xml.fastinfoset.tools.XML_SAX_FI;
import com.thoughtworks.xstream.XStream;

/**
 * This <code>StartupListener</code> implementation persists all requests enqueued, before really enqueuing them,
 *   so that they can be restarted in case of a server abnormal or forced shutdown. It works in conjunction with
 *   {@link ResumeRequestsBootstrapListener} which is the component responsible for resuming the unfinished
 *   transaction ids.
 * <p>
 * By default, files are generated with the max of {@value #MAX_REQUESTS} requests in each. To override this value,
 *   use the {@value #PERSIST_REQUESTS_MAX} attribute in the configuration xml document.
 * <p>
 * To enable "sharing" the requests persisted these two listeners use the same -D option named {@link #PERSIST_DIR},
 * 	where each transaction id is persisted in separate files. The name of these files are the transaction id number,
 *  appended by <code>tid</code> and the <code>.dat</code> extension, and a sequencial counter. If such property is
 *  not set, then the current directory is used.
 * <p>
 * <strong>IMPORTANT:</strong> This two filters do not handle status for requests. So <strong>all</strong> requests
 *   previously persisted will be restarted. For performance reasons, it is also advisable that a <code>SQLCheckpoint</code>
 *   be configured to control request status.
 * <p>
 * The last component required for this feature to work is to use the {@link ResumeControlQueueProcessedListener} which handles
 * 	removing the transaction id files so that new restarts will not resume completed transaction ids.
 *
 *
 * @author framos
 * @version $Id$
 *
 */
public class EnqueuePersistenceStartupListener implements StartupListener {

	public static final Logger log = Logger.getLogger(EnqueuePersistenceStartupListener.class);
	public static final I18n i18n = I18n.getInstance(EnqueuePersistenceStartupListener.class);

	public static final String PERSIST_DIR = "persist.basedir";
	public static final String PERSIST_FILE_APPEND = "tid-";
	public static final String PERSIST_FILE_EXTENSION = ".dat.gz";
	public static final String PERSIST_REQUESTS_MAX = "max-requests";

	public static final int MAX_REQUESTS = 50000;

	protected int requestsPerFile = MAX_REQUESTS;



	public EnqueuePersistenceStartupListener(Element _config) {
		if (_config == null) { return; }
		if (DOMUtils.getIntAttribute(_config, PERSIST_REQUESTS_MAX, false) > 0) {
			this.requestsPerFile = DOMUtils.getIntAttribute(_config, PERSIST_REQUESTS_MAX, false);
		}
	}

	/**
	 * @see br.com.auster.dware.StartupListener#afterConfig(br.com.auster.dware.DataAware, org.w3c.dom.Element)
	 */
	public void afterConfig(DataAware _dware, Element _config) {
		// nothing to do
	}

	/**
	 * @see br.com.auster.dware.StartupListener#afterEnqueue(br.com.auster.dware.DataAware, br.com.auster.dware.graph.Request)
	 */
	public void afterEnqueue(DataAware _dware, Request _request) {
		// nothing to do
	}

	/**
	 * @see br.com.auster.dware.StartupListener#afterEnqueue(br.com.auster.dware.DataAware, java.util.Collection)
	 */
	public void afterEnqueue(DataAware _dware, Collection<Request> _requests) {
		// nothing to do
	}

	/**
	 * @see br.com.auster.dware.StartupListener#beforeConfig(br.com.auster.dware.DataAware, org.w3c.dom.Element)
	 */
	public void beforeConfig(DataAware _dware, Element _config) {
		// nothing to do
	}

	/**
	 * @see br.com.auster.dware.StartupListener#beforeEnqueue(br.com.auster.dware.DataAware, br.com.auster.dware.graph.Request)
	 */
	public boolean beforeEnqueue(DataAware _dware, Request _request) {
		if (_request.getAttributes().containsKey(ResumeRequestsBootstrapListener.RESUMED_FLAG)) {
			log.warn(i18n.getString("persist.ignoringResumed"));
			return false;
		}
		ArrayList<Request> list = new ArrayList<Request>();
		list.add(_request);
		return beforeEnqueue(_dware, list);
	}

	/**
	 * @see br.com.auster.dware.StartupListener#beforeEnqueue(br.com.auster.dware.DataAware, java.util.Collection)
	 */
	public boolean beforeEnqueue(DataAware _dware, Collection<Request> _request) {
		Request firstRequest = _request.iterator().next();
		if (firstRequest.getAttributes().containsKey(ResumeRequestsBootstrapListener.RESUMED_FLAG)) {
			log.warn(i18n.getString("persist.ignoringResumed"));
			return false;
		}
		String dir = System.getProperty(EnqueuePersistenceStartupListener.PERSIST_DIR);
		if (dir == null) {
			log.info(i18n.getString("resumedir.notset"));
			dir = ".";
		}
		File f = new File (dir);
		if (!f.exists()) {
			f.mkdirs();
			log.warn(i18n.getString("resumedir.built", dir));
		}
		try {
			log.info(i18n.getString("persist.starting", _request.size()));
			presistRequests(dir, _request);
			log.info(i18n.getString("persist.finished"));
		} catch (Exception e) {
			log.info(i18n.getString("persist.finishedWithException"), e);
			return false;
		}
		return true;
	}




	protected void presistRequests(String _outputDir, Collection<Request> _request) throws Exception {
		int split = (_request.size() / this.requestsPerFile) +
		            ((_request.size() % this.requestsPerFile) > 0 ? 1 : 0 );
		Request[] copy = _request.toArray( new Request[] {} );
		for (int i=0; i < split; i++) {
			int srcStart = i*this.requestsPerFile;
			int len = Math.min(this.requestsPerFile, (_request.size()-srcStart));
			Request[] temp = new Request[len];
			System.arraycopy(copy, srcStart, temp, 0, len);
			presistRequestsToFile(_outputDir, temp, i);
		}
	}

	protected void presistRequestsToFile(String _outputDir, Request[] _request, int _split) throws Exception {
		Request firstRequest = _request[0];
		String transactionId = firstRequest.getTransactionId();
		log.debug(i18n.getString("persist.transaction", transactionId));
		File outputFile = buildFilename(_outputDir, transactionId, _split);
		log.debug(i18n.getString("persist.finalname", outputFile.getAbsolutePath()));
		WritableByteChannel out = NIOUtils.openFileForWrite(outputFile, false);
		// piping streams so we can send the output of FI into XStream
		Pipe pipe = Pipe.open();
		WritableByteChannel pipeOut = pipe.sink();
		ReadableByteChannel pipeIn = pipe.source();
		// starting persister thread
		FIPersisterThread thread = new FIPersisterThread(pipeIn, out);
		thread.start();
		// saving the request list
		XStream xstream = new XStream();
		xstream.useAttributeFor(String.class);
		xstream.toXML(_request, Channels.newOutputStream(pipeOut));
		log.debug(i18n.getString("persist.numberOfRequests", _request.length));
		pipeOut.close();
	}

	protected static File buildFilename(String _outputDir, String _transactionId, int _split) {
		String finalFilename = _outputDir + System.getProperty("file.separator") +
							   PERSIST_FILE_APPEND + _transactionId;
		if (_split >= 0) {
			finalFilename += "." + _split + PERSIST_FILE_EXTENSION;
		}
		return new File(finalFilename);
	}

	/**
	 * This thread will read the piped stream and write the FastInfoset document to the
	 * 	designated output.
	 */
	static class FIPersisterThread extends Thread {

		private WritableByteChannel out;
		private ReadableByteChannel in;

		public FIPersisterThread(ReadableByteChannel _in, WritableByteChannel _out) {
			this.in = _in;
			this.out = _out;
		}

		public void run() {
			try {
				XML_SAX_FI converter = new XML_SAX_FI();
				converter.parse(Channels.newInputStream(in), Channels.newOutputStream(out));
				in.close();
				out.close();
			} catch (Exception e) {
				log.info(i18n.getString("persist.finishedWithException"), e);
			}
		}
	}
}



