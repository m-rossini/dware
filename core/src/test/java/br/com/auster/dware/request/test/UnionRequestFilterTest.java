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
package br.com.auster.dware.request.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestFilter;
import br.com.auster.dware.request.UnionHashRequestFilter;
import br.com.auster.dware.request.file.FileRequest;
import br.com.auster.dware.request.file.PartialFileRequest;

import junit.framework.TestCase;

/**
 * @author framos
 * @version $Id$
 */
public class UnionRequestFilterTest extends TestCase {

	
	private Collection testRequests;
	
	protected void setUp() throws Exception {
		File f = new File("somefile.xml");
		if (testRequests != null) {
			testRequests.clear();
		} else {
			testRequests = new ArrayList();
		}
		ArrayList names = new ArrayList();
		names.add("Auster");
		names.add("Solutions");
		Request r = new PartialFileRequest("key1", 0, 0, f);
		r.getAttributes().put("userKey", "key1");		
		r.getAttributes().put("names", names);
		testRequests.add(r);
		r = new PartialFileRequest("key2", 0, 0, f);
		r.getAttributes().put("userKey", "key2");
		r.getAttributes().put("hasNames", "true");
		names = new ArrayList();
		names.add("Auster");
		names.add("Solutions");
		r.getAttributes().put("names", names);
		testRequests.add(r);
		r = new PartialFileRequest("key3", 0, 0, f);
		r.getAttributes().put("userKey", "key3");
		r.getAttributes().put("hasNames", "false");
		testRequests.add(r);
		r = new PartialFileRequest("key2", 0, 0, f);
		r.getAttributes().put("userKey", "key2.1");
		testRequests.add(r);
	}
	
	
	public void testWithNullAllowed() {
		UnionHashRequestFilter filter = new UnionHashRequestFilter();
		runOverTestRequests(filter);
		assertEquals(4, filter.getAcceptedRequests().size());
		for (Iterator i=filter.getAcceptedRequests().iterator(); i.hasNext();) {
			assertTrue(testRequests.contains(i.next()));
		}
	}
	
	
	public void testWithNotNullAllowed() {
		UnionHashRequestFilter filter = new UnionHashRequestFilter(createAllowedList(), false);
		runOverTestRequests(filter);
		assertEquals(4, filter.getAcceptedRequests().size());
		for (Iterator i=filter.getAcceptedRequests().iterator(); i.hasNext();) {
			assertTrue(testRequests.contains(i.next()));
		}
	}
	
	public void testAcceptOnlyOnce() {
		UnionHashRequestFilter filter = new UnionHashRequestFilter(createAllowedList(), true);
		runOverTestRequests(filter);
		assertEquals(3, filter.getAcceptedRequests().size());
		for (Iterator i=filter.getAcceptedRequests().iterator(); i.hasNext();) {
			assertTrue(testRequests.contains(i.next()));
		}
	}
	
	public void testEachRequestsAttribute() {
		UnionHashRequestFilter filter = new UnionHashRequestFilter(createAllowedList(), false);
		runOverTestRequests(filter);
		assertEquals(4, filter.getAcceptedRequests().size());
		Map atts = null;
		for (Iterator i=filter.getAcceptedRequests().iterator(); i.hasNext();) {
			FileRequest r = (FileRequest) i.next();
			assertEquals("somefile.xml", r.getFile().getName());
			atts = r.getAttributes();
			if (r.getUserKey().equals("key1")) {
				int currSize = atts.size();
				assertEquals(atts.get("userKey"), "key1");
				atts.remove("userKey");
				assertTrue(atts.get("names") instanceof List);
				assertEquals(4, ((List)atts.get("names")).size());
				atts.remove("names");
				assertEquals(currSize-2, atts.size());
			} else if (r.getUserKey().equals("key2")) {
				if (atts.get("userKey").equals("key2")) {
					int currSize = atts.size();
					atts.remove("userKey");
					assertEquals(atts.get("hasNames"), "true");
					atts.remove("hasNames");
					assertEquals(2, ((List)atts.get("names")).size());		
					atts.remove("names");
					assertEquals(currSize-3, atts.size());
				} else {
					int currSize = atts.size();
					assertEquals(atts.get("userKey"), "key2.1");
					atts.remove("userKey");
					assertEquals(currSize-1, atts.size());
				}
			} else if (r.getUserKey().equals("key3")) {		
				int currSize = atts.size();
				assertEquals(atts.get("userKey"), "key3");
				atts.remove("userKey");
				assertEquals(atts.get("hasNames"), "false");
				atts.remove("hasNames");
				assertTrue(atts.get("names") instanceof List);
				assertEquals(2, ((List)atts.get("names")).size());		
				atts.remove("names");
				assertEquals(currSize-3, atts.size());
			}
		}
	}
	
	private void runOverTestRequests(RequestFilter _filter) {
		for (Iterator it=testRequests.iterator(); it.hasNext();) {
			_filter.accept((Request)it.next());
		}
	}
	
	
	private Collection createAllowedList() {
		ArrayList allowed = new ArrayList();
		ArrayList names = new ArrayList();
		File f = new File("someotherfile.xml");
		names.add("do Brasil");
		names.add("Solutions");
		Request r = new PartialFileRequest("key1", 0, 0, f);
		r.getAttributes().put("userKey", "key4");
		r.getAttributes().put("names", names);
		allowed.add(r);
		r = new PartialFileRequest("key3", 0, 0, f);
		r.getAttributes().put("hasNames", "true");
		r.getAttributes().put("names", names);
		allowed.add(r);
		return allowed;
	}
	
	
}
