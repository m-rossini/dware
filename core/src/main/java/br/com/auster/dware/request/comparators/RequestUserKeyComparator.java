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
 * Created on 02/11/2006
 */
package br.com.auster.dware.request.comparators;

import java.util.Comparator;
import br.com.auster.dware.graph.Request;

/**
 * @author mtengelm
 * This class is a simple comparator of request user keys.
 * It receives a boolean in the constructor indicating if it should behave 
 * ascending or descending.
 * It is used to sort requests by user_key.
 */
public class RequestUserKeyComparator implements Comparator<Request> {
 
	private boolean	ascending	= true;

	/**
	 * If parameter is true, the ordering will be aascending, otherwise will be descending
	 * @param ascending
	 */
	public RequestUserKeyComparator(boolean ascending) {
		this.ascending = ascending;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	public int compare(Request objReq1, Request objReq2) {
		Request req1 = (Request) objReq1;
		Request req2 = (Request) objReq2;
		int compareTo = req1.getUserKey().compareTo(req2.getUserKey());
		return (ascending) ? compareTo : -1 * compareTo; 
	}

}
