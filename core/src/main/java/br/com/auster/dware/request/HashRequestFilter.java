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
package br.com.auster.dware.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import br.com.auster.dware.graph.Request;

/**
 * A <code>RequestFilter</code> that uses a <code>java.util.HashSet</code>
 * to hold a list of allowed requests to be processed.
 * 
 * A call to <code>accept</code> will only verify if the Hash contains the
 * request.
 * 
 * When accepting a request, this filter can <strong>roll over</strong> the
 * attributes from the request in the list of allowed to the request being
 * accepted. Checkout the {@link #rollOver} flag for that.
 * 
 * @author Ricardo Barone
 * @version $Id: HashRequestFilter.java 336 2007-12-26 13:14:17Z mtengelm $
 */
public class HashRequestFilter implements RequestFilter {
	// Map<String(userKey),List(requests)>
	protected Map<String, List<Request>>			allowedRequests		= null;

	protected List		acceptedRequests	= new ArrayList();

	protected boolean	acceptOnlyOnce;
	protected boolean	rollOver;

	/**
	 * Same as <code>new HashRequestFilter(null)</code>
	 * 
	 * @see #HashRequestFilter(List)
	 */
	public HashRequestFilter() {
		this(null, false, false);
	}

	/**
	 * Same as {@link #HashRequestFilter(Collection, boolean, boolean), setting
	 * <code>rollOver</code> to false.
	 * 
	 * @param allowedRequests
	 *          list of <code>Request</code> objects.
	 * @param acceptOnlyOnce
	 *          if the request must be removed after being accepted (so that each
	 *          request will be accepted only once) or not.
	 */
	public HashRequestFilter(Collection allowedRequests, boolean acceptOnlyOnce) {
		this(allowedRequests, acceptOnlyOnce, false);
	}

	/**
	 * Creates a HashRequestFilter containing the given list of allowed requests.
	 * <p>
	 * <b>Please note that passing an empty list is NOT the same as an empty
	 * filter.</b> Instead, it will create a filter with no allowed requests and
	 * so will reject all requests.
	 * <p>
	 * If the {@value #rollOver} flag is set to <code>true</code>, then all
	 * attributes in the request removed from the allowed list will be copied to
	 * this newly accepted request. This copy will preserve the values from the
	 * current accepted request in case of duplicate keys.
	 * <p>
	 * This attribute copy works though composite types (like maps and
	 * collections) considering the original behaviour of the selected type: sets
	 * will have only one occurence of each value, lists will allow duplicates,
	 * etc. Again, the value from the accepted request will always be preserved in
	 * case of duplicates.
	 * 
	 * 
	 * @param allowedRequests
	 *          list of <code>Request</code> objects.
	 * @param acceptOnlyOnce
	 *          if the request must be removed after being accepted (so that each
	 *          request will be accepted only once) or not.
	 * @param rollOver
	 *          if attributes added to the request in the allowedRequests list
	 *          should be copied into the recently accepted request.
	 */
	public HashRequestFilter(Collection allowedRequests, boolean acceptOnlyOnce,
			boolean rollOver) {
		this.acceptOnlyOnce = acceptOnlyOnce;
		if (allowedRequests != null) {
			this.allowedRequests = new HashMap();
			for (Iterator it = allowedRequests.iterator(); it.hasNext();) {
				Request request = (Request) it.next();
				ArrayList requests = (ArrayList) this.allowedRequests.get(request.getUserKey());
				if (requests == null) {
					requests = new ArrayList();
					requests.add(request);
					this.allowedRequests.put(request.getUserKey(), requests);
				} else if (!acceptOnlyOnce || requests.size() == 0) {
					requests.add(request);
				}
			}
		}
		setRollOver(rollOver);
	}

	public void setRollOver(boolean _flag) {
		this.rollOver = _flag;
	}

	/**
	 * @inheritDoc
	 */
	public boolean accept(Request request) {
		return accept(request, false);
	}

	/**
	 * @inheritDoc
	 */
	public boolean accept(Request request, boolean ignore) {

		boolean isAccepted = true;
		Request original = null;

		if (this.allowedRequests != null) {
			List requests = null;
			if (!this.allowedRequests.containsKey(request.getUserKey())) {
				isAccepted = false;
			} else if (this.acceptOnlyOnce) {
				requests = (ArrayList) this.allowedRequests.remove(request.getUserKey());
			} else {
				requests = (ArrayList) this.allowedRequests.get(request.getUserKey());
			}
			if (requests != null) {
				original = (Request) requests.get(requests.size() - 1);
			}
		}
		if (isAccepted && !ignore) {
			this.acceptedRequests.add(request);
			if ((this.rollOver) && (original != null)) {
				mergeAttributes(request, original);
			}
		}

		return isAccepted;
	}

	/**
	 * @inheritDoc
	 */
	public boolean willAccept(Request request) {
		boolean isAccepted = true;
		if (this.allowedRequests != null
				&& !this.allowedRequests.containsKey(request.getUserKey())) {
			isAccepted = false;
		}
		return isAccepted;
	}

	/**
	 * @inheritDoc
	 */
	public boolean canAccept() {
		return (this.allowedRequests == null ? true : !this.allowedRequests.isEmpty());
	}

	/**
	 * @inheritDoc
	 */
	public void acceptAll() {

		if (this.allowedRequests != null) {
			Iterator it = this.allowedRequests.values().iterator();
			while (it.hasNext()) {
				this.acceptedRequests.addAll((Collection) it.next());
			}
			this.allowedRequests.clear();
		}
	}

	/**
	 * @inheritDoc
	 */
	public Collection getAcceptedRequests() {
		return Collections.unmodifiableCollection(this.acceptedRequests);
	}

	/**
	 * @inheritDoc
	 */
	public Collection getRemainingRequests() {
		if (this.allowedRequests == null) {
			return Collections.unmodifiableCollection(new ArrayList(0));
		}
		ArrayList results = new ArrayList();
		Iterator it = this.allowedRequests.values().iterator();
		while (it.hasNext()) {
			results.addAll((Collection) it.next());
		}
		return Collections.unmodifiableCollection(results);
	}

	/**
	 * @inheritDoc
	 */
	public void setPreviousRequests(Collection requests) {
		if (requests != null) {
			if (this.allowedRequests == null) {
				this.allowedRequests = new HashMap();
			}
			for (Iterator it = requests.iterator(); it.hasNext();) {
				Request request = (Request) it.next();
				ArrayList previous = (ArrayList) this.allowedRequests.get(request.getUserKey());
				if (previous == null) {
					previous = new ArrayList();
					previous.add(request);
					this.allowedRequests.put(request.getUserKey(), previous);
				} else if (!acceptOnlyOnce || previous.size() == 0) {
					previous.add(request);
				}
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	public RequestFilter reset() {
		return reset(this.acceptOnlyOnce);
	}

	public RequestFilter reset(boolean acceptOnlyOnce) {
		this.acceptedRequests.clear();
		if (this.allowedRequests != null) {
			this.allowedRequests.clear();
		}
		this.acceptOnlyOnce = acceptOnlyOnce;
		return this;
	}

	protected void mergeAttributes(Request _newRequest, Request _sourceRequest) {
		Map attributes = _sourceRequest.getAttributes();
		mergeAttributes(_newRequest.getAttributes(), attributes);
	}

	protected void mergeAttributes(Map _requestAttributes, Map _sourceAttributes) {
		for (Iterator it = _sourceAttributes.entrySet().iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			if (!_requestAttributes.containsKey(entry.getKey())) {
				_requestAttributes.put(entry.getKey(), entry.getValue());
			} else {
				if (entry.getValue() instanceof Map) {
					mergeAttributes((Map) _requestAttributes.get(entry.getKey()), (Map) entry
							.getValue());
				} else if (entry.getValue() instanceof Collection) {
					Collection c = (Collection) _requestAttributes.get(entry.getKey());
					c.addAll((Collection) entry.getValue());
				}
			}
		}
	}

	/**
	 * @see br.com.auster.dware.request.RequestFilter#getAllowedRequests()
	 */
	public Map<String, List<Request>> getAllowedRequests() {
		return this.allowedRequests;
	}

}
