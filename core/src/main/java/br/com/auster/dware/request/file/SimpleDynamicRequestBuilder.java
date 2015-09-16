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
 * Created on 01/07/2005
 */
package br.com.auster.dware.request.file;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.FileSet;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

/**
 * <p>
 * <b>Title:</b> SimpleDynamicRequestBuilder
 * </p>
 * <p>
 * <b>Description:</b>
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2004-2005
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 * 
 * @author mtengelm
 * @version $Id$
 */
public class SimpleDynamicRequestBuilder implements RequestBuilder {

	private static final Logger	  log	                      = Logger
	                                                            .getLogger(SimpleDynamicRequestBuilder.class);
	private String	              name;
	private String	              rqType;

	protected static final String	REQUEST_CREATE_ATTR	      = "request-create-type";
	protected static final String	DEFAULT_REQUEST_TYPE	    = "FileRequest";
	protected static final String	FILE_REQUEST	            = "FileRequest";
	protected static final String	FILE_LIST_REQUEST	        = "FileListRequest";
	protected static final String	PARTIAL_FILE_LIST_REQUEST	= "PartialFileListRequest";
	protected static final String	PARTIAL_FILE_REQUEST	    = "PartialFileRequest";

	protected static final String	FILE_NAMES_PARM	          = "filenames";
	protected static final String	FILE_NAME_PARM	          = "filename";
	protected static final String	TRANSACTION_ID_ARG	      = "transaction-id";
	protected static final String	REQUEST_PARAMS_ARG	      = "request-params";

	/**
	 * 
	 */
	public SimpleDynamicRequestBuilder(String name, Element config) {
		this.name = name;
		this.config(config);
	}

	/**
	 * @param config
	 */
	public void config(Element config) {
		this.rqType = DOMUtils.getAttribute(config, REQUEST_CREATE_ATTR, false);
		if (rqType.equals("")) {
			this.rqType = DEFAULT_REQUEST_TYPE;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see br.com.auster.dware.request.RequestBuilder#getName()
	 */
	public String getName() {
		return this.name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(java.util.Map)
	 */
	public RequestFilter createRequests(Map args) {
		return this.createRequests(null, args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(br.com.auster.dware.request.RequestFilter,
	 *      java.util.Map)
	 */
	public RequestFilter createRequests(RequestFilter filter, Map args) {
		if (this.rqType.equals(FILE_LIST_REQUEST)) {
			FileListRequest req = new FileListRequest(FileSet.getFiles((String) args
			    .get(FILE_NAMES_PARM)));
			setTransactionIdAndParameters(req, args);
			filter.accept(req);
			return filter;
		}

		File[] files = FileSet.getFiles((String) args.get(FILE_NAMES_PARM));
		if (this.rqType.equals(FILE_REQUEST)) {
			for (int i = 0; i < files.length; i++) {
				FileRequest req = new FileRequest(files[i]);
				setTransactionIdAndParameters(req, args);
				req.getAttributes().put(FILE_NAME_PARM, files[i].getName());
				filter.accept(req);
			}
			return filter;
		}

		if (this.rqType.equals(PARTIAL_FILE_LIST_REQUEST)) {
			PartialFileRequest[] requests = new PartialFileRequest[files.length];
			for (int i = 0; i < files.length; i++) {
				requests[i] = new PartialFileRequest(0, files[i].length(), files[i]);
				requests[i].getAttributes().put(FILE_NAME_PARM, files[i].getName());				
			}
			PartialFileListRequest req = new PartialFileListRequest(requests);
			setTransactionIdAndParameters(req, args);
			filter.accept(req);

			return filter;
		}

		if (this.rqType.equals(PARTIAL_FILE_REQUEST)) {
			for (int i = 0; i < files.length; i++) {
				PartialFileRequest req = new PartialFileRequest(0, files[i].length(),
				    files[i]);
				setTransactionIdAndParameters(req, args);
				req.getAttributes().put(FILE_NAME_PARM, files[i].getName());
				filter.accept(req);
			}
			return filter;
		}
		return filter;
	}

	/**
	 * Sets the transaction id and request parameter, for each created request.
	 * 
	 * @param _request
	 * @param _args
	 */
	private void setTransactionIdAndParameters(Request _request, Map _args) {
		String transactionId = (String) _args.get(TRANSACTION_ID_ARG);
		if (transactionId != null) {
			_request.setTransactionId(transactionId);
		}

		Map params = (Map) _args.get(REQUEST_PARAMS_ARG);
		Map attrs = new HashMap();
		if (params != null) {
			attrs.putAll(params);
		}
		_request.setAttributes(attrs);
	}
}
