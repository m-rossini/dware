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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.dware.manager.QueueProcessedListener;

/**
 * This class implements <code>QueueProcessedListener</code> adding to it the ability to remove files
 *    where requests for a specific transaction were previously persisted. To understand how these files
 *    work, see {@link EnqueuePersistenceStartupListener} and {@link ResumeRequestsBootstrapListener} documentations
 *    since they are the listeners responsible for operating over persist and resume of enqueued requests.
 * <p>
 *
 * @author framos
 * @version $Id$
 */
public class ResumeControlQueueProcessedListener implements QueueProcessedListener {


	public static final Logger log = Logger.getLogger(ResumeControlQueueProcessedListener.class);
	public static final I18n i18n = I18n.getInstance(ResumeControlQueueProcessedListener.class);


	/**
	 * Will handle which files should be read when resuming requests.
	 */
	static class QueueProcessedFilenameFilter implements FilenameFilter {

		public String filterBy;

		public boolean accept(File _dir, String _filename) {
			log.debug(i18n.getString("persist.checkFileInDir", _filename, _dir.getAbsolutePath()));
			return ( _filename.indexOf(this.filterBy) == 0 );
		}

	};



	/**
	 * @see br.com.auster.dware.manager.QueueProcessedListener#onQueueProcessed(java.lang.String, int)
	 */
	public void onQueueProcessed(String _transactionId, int _size) {

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
		QueueProcessedFilenameFilter filter = new QueueProcessedFilenameFilter();
		filter.filterBy = EnqueuePersistenceStartupListener.buildFilename(".", _transactionId, -1).getName();

		File[] files = f.listFiles(filter);
		int counter=0;
		for (int i=0; i < files.length; i++) {
			if (files[i].exists() && files[i].isFile()) {
				files[i].delete();
				log.info(i18n.getString("persist.removed", files[i].getName()));
				counter++;
			}
		}
		if (counter <= 0) {
			log.warn(i18n.getString("persist.fileNotFound", filter.filterBy));
		}
	}

	/**
	 * @see br.com.auster.dware.manager.QueueProcessedListener#onQueueProcessed(java.lang.String, int)
	 */
	public void init(Element config) {
		// do nothing
	}
}
