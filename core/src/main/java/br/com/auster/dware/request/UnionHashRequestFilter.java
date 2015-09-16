/*
 * Copyright (c) 2004 Auster Solutions. All Rights Reserved.
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
 * Created on 15/02/2006
 */
package br.com.auster.dware.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import br.com.auster.dware.graph.Request;

/**
 * This class is a customization of {@link br.com.auster.dware.request.HashRequestFilter}, where:
 * <li>
 *   <ul><code>willAccept()</code> and <code>canAccept()</code> will always return <code>true</code>, <strong>unless</strong>
 *       the allowed list was set and a request with such key was previously accepted.</ul>
 *   <ul><code>accept()</code> will always accept the request unless the same condition defined above happens. Also, when
 *       the request is accepted, all the attributes from the request in the allowed list are copied into this new request.
 *       In other words, this implementaion <strong>forces</strong> the <code>rollOver</code> flag to <code>true</code>.</ul>
 *   <ul><code>getAcceptedRequests()</code> will return the accept list <strong>plus</strong> all requests in the allowed list 
 *   	 that were never accepted. 
 * </li>
 * 
 * To make sure a request is not duplicated, in this last behaviour, a list of found keys is kept between executions. Those 
 * 	requests in the allowed list without corresponding key in the found list, are the ones copied when calling <code>getAcceptedRequests()</code>. 
 *           
 * 
 * @author framos
 * @version $Id$
 */
public class UnionHashRequestFilter extends HashRequestFilter {

	
	
	protected Set foundKeys;
	
	
	public UnionHashRequestFilter() {
		super();
		this.foundKeys = new HashSet();
	}
	
	public UnionHashRequestFilter(Collection _allowedRequests, boolean _acceptOnlyOnce) {
		super(_allowedRequests, _acceptOnlyOnce);
		this.foundKeys = new HashSet();
	}
		
	public boolean accept(Request _request, boolean _ignore) {		
		if (this.allowedRequests != null) {
			Request original = null;
			List requests = null;
			if (this.acceptOnlyOnce) {
				requests = (ArrayList) this.allowedRequests.remove(_request.getUserKey());
			} else {
				requests = (ArrayList) this.allowedRequests.get(_request.getUserKey());
			}
			if (requests != null) {
				original = (Request) requests.get(requests.size() - 1);
				mergeAttributes(_request, original);				
			} else if (this.acceptOnlyOnce && this.foundKeys.contains(_request.getUserKey())) {
				// when original is NULL, the acceptOnlyOnce flag is set and the foundKeys set contains the current request key,
				//   then this request was already accepted and 
				return false;
			}
		}
		if (!_ignore) {
			this.acceptedRequests.add(_request);
			this.foundKeys.add(_request.getUserKey());
		}
		return true;
	}

	public boolean willAccept(Request _request) {
		if ((this.allowedRequests != null) && this.acceptOnlyOnce && this.foundKeys.contains(_request.getUserKey())) {
			return false;
		}
		return true;
	}
	
	public boolean canAccept() {
		if ((this.allowedRequests != null) && this.allowedRequests.isEmpty() && this.acceptOnlyOnce) {
			return false;
		}
		return true;
	}
	
	public Collection getAcceptedRequests() {
		Collection acptedReqs = new ArrayList();
		acptedReqs.addAll(this.acceptedRequests);
		if (this.allowedRequests != null) {
			for (Iterator it=this.allowedRequests.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				if (!this.foundKeys.contains(key)) {
					acptedReqs.addAll( (Collection) this.allowedRequests.get(key) );
				}
			}
		}
		return Collections.unmodifiableCollection(acptedReqs);
	}
	
	public RequestFilter reset() {
		this.foundKeys.clear();
		return super.reset();
	}

	public RequestFilter reset(boolean acceptOnlyOnce) {
		this.foundKeys.clear();
		return super.reset(acceptOnlyOnce);
	}
}
