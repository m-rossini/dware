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
import java.io.FilenameFilter;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.Bootstrap;
import br.com.auster.dware.BootstrapListener;
import br.com.auster.dware.graph.Request;

import com.sun.xml.fastinfoset.tools.FI_SAX_XML;
import com.thoughtworks.xstream.XStream;

/**
 * This <code>BootstrapListener</code> implementation handles the restart (aka resume) of unfinished
 *    requests, after a server abnormal or forced shutdown. It works in conjunction with {@link EnqueuePersistenceStartupListener}
 *    which is the component responsible for serializing all requests information.
 * <p>
 * To enable "sharing" the requests persisted these two listeners use the same -D option named {@link #PERSIST_DIR},
 * 	where each transaction id is persisted in separate files. The name of these files are the transaction id number,
 *  appended by <code>tid</code> and the <code>.dat</code> extension, and a sequencial counter. If such property is
 *  not set, then the current directory is used.
 * <p>
 * <strong>IMPORTANT:</strong> This two filters do not handle status for requests. So <strong>all</strong> requests previously persisted
 *   will be restarted. For performance reasons, it is also advisable that a <code>SQLCheckpoint</code> be configured to control request
 *   status.
 * <p>
 * The last component required for this feature to work is to use the {@link ResumeControlQueueProcessedListener} which handles
 * 	removing the transaction id files so that new restarts will not resume completed transaction ids.
 *
 * @author framos
 * @version $Id$
 *
 */
public class ResumeRequestsBootstrapListener implements BootstrapListener {



	public static final Logger log = Logger.getLogger(ResumeRequestsBootstrapListener.class);
	public static final I18n i18n = I18n.getInstance(ResumeRequestsBootstrapListener.class);

	public static final String RESUMED_FLAG = "__resumed__";


	/**
	 * Will handle which files should be read when resuming requests.
	 */
	public static final FilenameFilter filter = new FilenameFilter() {
		public boolean accept(File _dir, String _filename) {
			// filename must start with PERSIST_FILE_APPEND, end with PERSIST_FILE_EXTENSION
			//   and have more than 7 chars
			return _filename.startsWith(EnqueuePersistenceStartupListener.PERSIST_FILE_APPEND) &&
				   _filename.endsWith(EnqueuePersistenceStartupListener.PERSIST_FILE_EXTENSION) &&
				   _filename.length() > (EnqueuePersistenceStartupListener.PERSIST_FILE_APPEND.length() + EnqueuePersistenceStartupListener.PERSIST_FILE_EXTENSION.length());
		}

	};




	/**
	 * This implementation of </code>BootstrapListener</code> does not require any special configuration.
	 *
	 * @see br.com.auster.dware.BootstrapListener#configure(org.w3c.dom.Element)
	 */
	public void configure(Element _configuration) {
	}

	/**
	 * @see br.com.auster.dware.BootstrapListener#onInitServer(br.com.auster.dware.Bootstrap, java.lang.String)
	 */
	public void onInitServer(Bootstrap _instance, String _url) {
		String dir = System.getProperty(EnqueuePersistenceStartupListener.PERSIST_DIR);
		if (dir == null) {
			log.info(i18n.getString("resumedir.notset"));
			dir = ".";
		}
		File f = new File (dir);
		if ((!f.exists()) || (!f.isDirectory())) {
			log.warn(i18n.getString("resumedir.notfound", dir));
			return;
		}
		log.info(i18n.getString("resumedir.starting", dir));
		resumeRequests(f, _instance);
	}

	/**
	 * @see br.com.auster.dware.BootstrapListener#onProcess(br.com.auster.dware.Bootstrap, java.lang.String, java.util.Map, java.util.List, java.util.Collection)
	 */
	public void onProcess(Bootstrap _bootstrap, String _chainName, Map _args, List _desiredRequests, Collection _requests) {
		// do nothing
	}



	/**
	 * Protected methods
	 */

	protected void resumeRequests(File _resumeDir, Bootstrap _bootstrap) {
		String[] files = _resumeDir.list(filter);
		for (String filename : files) {
			try {
				log.info(i18n.getString("resuming.started", filename));
				resumeRequestsForFile(_resumeDir +  System.getProperty("file.separator") + filename, _bootstrap);
				log.info(i18n.getString("resuming.finished", filename));
			} catch (Exception e) {
				log.warn(i18n.getString("resuming.finishedWithException", filename), e);
			}
		}
	}

	protected void resumeRequestsForFile(String _filename, Bootstrap _bootstrap) throws Exception {
		// piping streams so we can send the output of FI into XStream
		Pipe pipe = Pipe.open();
		WritableByteChannel pipeOut = pipe.sink();
		ReadableByteChannel pipeIn = pipe.source();
		// reading persisted file
		ReadableByteChannel inChannel = NIOUtils.openFileForRead(_filename);
		Future<?> thread = Executors.newSingleThreadExecutor().submit(new FIReaderThread(inChannel, pipeOut));
		// now the file read in inChannel was piped into pipeIn where we can read into XStream class
		XStream xstream = new XStream();
		xstream.useAttributeFor(String.class);
		Request[] requests =  (Request[]) xstream.fromXML(Channels.newInputStream(pipeIn));
		pipeOut.close();
		thread.get();
		log.debug(i18n.getString("resumed.numberOfRequests", _filename, requests.length));
		// marking the requests so that they wont be persisted on this new enqueue
		requests[0].getAttributes().put(RESUMED_FLAG, "true");
		// enqueueing a copy of the loaded request list
		_bootstrap.enqueueRequests(Arrays.asList(requests));
	}


	/**
	 * This thread will read the piped stream and write the FastInfoset document to the
	 * 	designated output.
	 */
	static class FIReaderThread implements Runnable {

		private WritableByteChannel out;
		private ReadableByteChannel in;

		public FIReaderThread(ReadableByteChannel _in, WritableByteChannel _out) {
			this.in = _in;
			this.out = _out;
		}

		public void run() {
			try {
				FI_SAX_XML converter = new FI_SAX_XML();
				converter.parse(Channels.newInputStream(in), Channels.newOutputStream(out));
				in.close();
				out.close();
			} catch (Exception e) {
				log.info(i18n.getString("persist.finishedWithException"), e);
			}
		}
	}

}
