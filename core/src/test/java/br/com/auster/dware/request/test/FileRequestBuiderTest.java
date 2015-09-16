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

import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestFilter;
import br.com.auster.dware.request.file.FileRequestBuilder;
import junit.framework.TestCase;

/**
 * @author framos
 * @version $Id$
 */
public class FileRequestBuiderTest extends TestCase {

	
	
	
	
	public void testStaticNotDefined() {
		try {
			FileRequestBuilder builder = new FileRequestBuilder("test", openConfiguration("statics/statics-nostatic.xml"));
			RequestFilter filter = builder.createRequests(new HashRequestFilter(), getAttrs());
			assertEquals(2, filter.getAcceptedRequests().size());
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	public void testStaticWithSingleValuesOnly() {
		try {
			FileRequestBuilder builder = new FileRequestBuilder("test", openConfiguration("statics/statics-static1.xml"));
			RequestFilter filter = builder.createRequests(new HashRequestFilter(), getAttrs());
			assertEquals(2, filter.getAcceptedRequests().size());
			
			for (Iterator it=filter.getAcceptedRequests().iterator(); it.hasNext();) {
				Request req = (Request) it.next();
				assertTrue(req.getAttributes().containsKey("static1"));
				assertEquals("Static Info", req.getAttributes().get("static1"));
				assertTrue(req.getAttributes().containsKey("static2"));
				assertEquals(new Integer(10), req.getAttributes().get("static2"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	public void testStaticWithListOnly() {
		try {
			FileRequestBuilder builder = new FileRequestBuilder("test", openConfiguration("statics/statics-static2.xml"));
			RequestFilter filter = builder.createRequests(new HashRequestFilter(), getAttrs());
			assertEquals(2, filter.getAcceptedRequests().size());
			
			for (Iterator it=filter.getAcceptedRequests().iterator(); it.hasNext();) {
				Request req = (Request) it.next();
				assertTrue(req.getAttributes().containsKey("static1"));
				assertTrue(req.getAttributes().get("static1") instanceof Collection);
				assertEquals(3, ((Collection)req.getAttributes().get("static1")).size());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	public void testStaticWithAll() {
		try {
			FileRequestBuilder builder = new FileRequestBuilder("test", openConfiguration("statics/statics-static3.xml"));
			RequestFilter filter = builder.createRequests(new HashRequestFilter(), getAttrs());
			assertEquals(2, filter.getAcceptedRequests().size());
			
			for (Iterator it=filter.getAcceptedRequests().iterator(); it.hasNext();) {
				Request req = (Request) it.next();
				assertTrue(req.getAttributes().containsKey("static1"));
				assertTrue(req.getAttributes().get("static1") instanceof Collection);
				assertEquals(3, ((Collection)req.getAttributes().get("static1")).size());
				assertTrue(req.getAttributes().containsKey("static2"));
				assertEquals(new Integer(10), req.getAttributes().get("static2"));
				assertEquals(new Integer(10), req.getAttributes().get("static3"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	public void testStaticWithIncorrectStaticType() {
		try {
			FileRequestBuilder builder = new FileRequestBuilder("test", openConfiguration("statics/statics-static4.xml"));
			RequestFilter filter = builder.createRequests(new HashRequestFilter(), getAttrs());
			assertEquals(2, filter.getAcceptedRequests().size());
			
			for (Iterator it=filter.getAcceptedRequests().iterator(); it.hasNext();) {
				Request req = (Request) it.next();
				assertTrue(req.getAttributes().containsKey("static1"));
				assertEquals(new Integer(10), req.getAttributes().get("static1"));
				assertNull(req.getAttributes().get("static2"));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			fail();
		}
	}
	
	
	private Element openConfiguration(String _file) throws Exception {
		InputStream in = FileRequestBuiderTest.class.getResourceAsStream(_file);
		if (in == null) {
			throw new NullPointerException("Resource '" + _file + "' not found."); 
		}
		return DOMUtils.openDocument(in);
	}

	private Map getAttrs() {
		HashMap attrs = new HashMap();
		URL url = FileRequestBuiderTest.class.getResource("statics/testsource.txt");
		if (url == null) {
			throw new NullPointerException("Resource 'statics/testsource.txt' not found."); 
		}
		if (url.getFile().charAt(2) == ':') {
			attrs.put("filenames", url.getFile().substring(1));
		} else {
			attrs.put("filenames", url.getFile());
		}
		return attrs;
	}
	
}
