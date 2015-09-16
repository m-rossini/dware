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
import java.util.Map;

import org.w3c.dom.Element;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.request.RequestBuilderManager;
import junit.framework.TestCase;

/**
 * @author framos
 * @version $Id$
 */
public class ChainedRequestBuildersTest extends TestCase {

	
	public void testChainedBuilders() {
		try {
			RequestBuilderManager manager = new RequestBuilderManager(openConfiguration("chain-test.xml"));
			Collection c = manager.createRequests("default", getAttrs());
			assertEquals(3, c.size());
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}

	
	private Element openConfiguration(String _file) throws Exception {
		InputStream in = ChainedRequestBuildersTest.class.getResourceAsStream(_file);
		if (in == null) {
			throw new NullPointerException("Resource '" + _file + "' not found."); 
		}
		return DOMUtils.openDocument(in);
	}
	
	private Map getAttrs() {
		HashMap attrs = new HashMap();
		
		HashMap test1Attrs = new HashMap();
		URL url = ChainedRequestBuildersTest.class.getResource("statics/testsource.txt");
		if (url == null) {
			throw new NullPointerException("Resource 'statics/testsource.txt' not found."); 
		}
		if (url.getFile().charAt(2) == ':') {
			test1Attrs.put("filenames", url.getFile().substring(1));
		} else {
			test1Attrs.put("filenames", url.getFile());
		}
		attrs.put("test1", test1Attrs);
		
		HashMap test2Attrs = new HashMap();
		url = ChainedRequestBuildersTest.class.getResource("statics/test2filter.txt");
		if (url == null) {
			throw new NullPointerException("Resource 'statics/test2filter.txt' not found."); 
		}
		if (url.getFile().charAt(2) == ':') {
			test2Attrs.put("filenames", url.getFile().substring(1));
		} else {
			test2Attrs.put("filenames", url.getFile());
		}
		attrs.put("test2", test2Attrs);

		return attrs;
	}
	
	
}
