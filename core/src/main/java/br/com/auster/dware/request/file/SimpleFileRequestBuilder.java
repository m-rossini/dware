/*
 * Copyright (c) 2004-2005 Auster Solutions. All Rights Reserved.
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
 * Created on 27/06/2005
 */
package br.com.auster.dware.request.file;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestFilter;

/**
 * <p><b>Title:</b> SimpleFileRequestBuilder</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author rbarone
 * @version $Id$
 */
public class SimpleFileRequestBuilder extends FileRequestBuilder {

	private static final Logger log = LogFactory.getLogger(SimpleFileRequestBuilder.class);
	private final I18n i18n = I18n.getInstance(FileRequestBuilder.class);
	
	public SimpleFileRequestBuilder(String name, Element config) {
		super(config, name);
	}

	public String getName() {
		return this.name;
	}

	public RequestFilter createRequests(RequestFilter filter, Map args) {
		final RequestFilter requestFilter;
		if (filter == null) {
			requestFilter = new HashRequestFilter();
		} else {
			requestFilter = filter;
		}

		final FileBuilderArgs params = parseFileArgs(args);

		// request attributes
		Map atts = new HashMap();

		Iterator it = params.getFiles().iterator();
		while (it.hasNext() && requestFilter.canAccept()) {
			final File file = (File) it.next();

			FileRequest request = new FileRequest(file);
			this.staticAttributes.insertStatics(atts);

			if (params.getTransactionId() != null) {
				request.setTransactionId(params.getTransactionId());
			}
			if (params.getRequestParams() != null) {
				atts.putAll(params.getRequestParams());
			}
			request.setAttributes(atts);
			if (filter.accept(request)) {
				log.debug(i18n.getString("foundRequest", request));
			} else {
				log.debug(i18n.getString("discardedRequest", request));
			}
			// prepare the atts map for the next request
			atts.clear();
		}
		return requestFilter;
	}

}
