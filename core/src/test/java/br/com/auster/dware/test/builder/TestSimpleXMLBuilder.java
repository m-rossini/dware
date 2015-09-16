/*
 * Copyright (c) 2004 TTI Tecnologia. All Rights Reserved.
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
 * Created on Jul 4, 2005
 */
package br.com.auster.dware.test.builder;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.request.RequestFilter;
import br.com.auster.dware.request.file.SingleXMLFileRequestBuilder;

/**
 * @author framos
 * @version $Id: TestSimpleXMLBuilder.java 42 2005-07-04 21:43:35Z framos $
 */
public class TestSimpleXMLBuilder {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: java TestSimpleXMLBuilder <source-xml>");
			System.exit(1);
		}
		
		try {
			Element config = DOMUtils.openDocument(TestSimpleXMLBuilder.class.getResourceAsStream("config.xml"));
			long start = Calendar.getInstance().getTimeInMillis();
			SingleXMLFileRequestBuilder builder = new SingleXMLFileRequestBuilder("test", config);
			
			Map argsMap = new HashMap();
			argsMap.put(SingleXMLFileRequestBuilder.REQUEST_INPUT_FILE_ARG, args[0]);
			argsMap.put(SingleXMLFileRequestBuilder.REQUEST_TRANSACTION_ID_ARG, "10010010");
			
			RequestFilter filter = builder.createRequests(argsMap);
			Collection requests = filter.getAcceptedRequests();
			long end = Calendar.getInstance().getTimeInMillis();
			System.out.println(requests);
			System.out.println("time=" + (end-start) + "ms");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			e.printStackTrace();
		}
	}

}
