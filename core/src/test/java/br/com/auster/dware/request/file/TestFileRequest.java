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
 * Created on 22/12/2006
 */
package br.com.auster.dware.request.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

import br.com.auster.dware.graph.Request;

import junit.framework.TestCase;

/**
 * @author mtengelm
 *
 */
public class TestFileRequest extends TestCase {

	private BufferedWriter br;
	private File file;
	private String fname= null;

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		fname = "dware-test.txt";
		file = new File(fname);
		br = new BufferedWriter(new FileWriter(file));
		assertNotNull(file);
		assertNotNull("Buffered Writter SHOULD NOT be null" , br);
		br.write("Linha01");
		br.write("\n");
		br.write("Linha02");
		br.write("\n");
		br.flush();
	}

	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		file = null;
		br.close();
		br = null;
	}

	/**
	 * Test method for {@link br.com.auster.dware.request.file.FileRequest#getWeight()}.
	 */
	public void testGetWeight() {
		FileRequest request = new FileRequest(file);
		assertEquals(file.length(), request.getWeight());
	}

	/**
	 * Test method for {@link br.com.auster.dware.request.file.FileRequest#FileRequest(java.io.File)}.
	 */
	public void testFileRequestFile() {
		FileRequest request = new FileRequest(file);
		assertNotNull(request);
	}

	/**
	 * Test method for {@link br.com.auster.dware.request.file.FileRequest#FileRequest(java.lang.String, java.io.File)}.
	 */
	public void testFileRequestStringFile() {
		FileRequest request = new FileRequest("UserKeyTestString", file);
		assertNotNull(request);
		assertEquals(request.getUserKey(), "UserKeyTestString");
		assertEquals(request.getUserKey(), request.getAttributes().get(Request.KEY4_USERKEY));
	}

	/**
	 * Test method for {@link br.com.auster.dware.request.file.FileRequest#getFile()}.
	 */
	public void testGetFile() {
		FileRequest request = new FileRequest(file);
		assertEquals(request.getFile(), file);
		assertSame(request.getFile(), file);
	}

	/**
	 * Test method for {@link br.com.auster.dware.graph.Request#getId()}.
	 */
	public void testGetId() {
		FileRequest request = new FileRequest(file);
		assertEquals(request.getId(), fname);
		assertEquals(request.getId(), request.getAttributes().get(Request.KEY4_ID));
	}

	/**
	 * Test method for {@link br.com.auster.dware.graph.Request#getUserKey()}.
	 */
	public void testGetUserKey() {
		FileRequest request = new FileRequest(file);
		assertEquals(request.getUserKey(), fname);
		assertEquals(request.getUserKey(), request.getAttributes().get(Request.KEY4_USERKEY));
		
	}

	/**
	 * Test method for {@link br.com.auster.dware.graph.Request#getTransactionId()}.
	 */
	public void testGetTransactionId() {
		FileRequest request = new FileRequest(file);
		assertNull(request.getTransactionId());
		assertEquals(request.getTransactionId(), request.getAttributes().get(Request.KEY4_TRANSACTION_ID));
		
		request.setTransactionId("Transaction Setted.");
		assertEquals(request.getTransactionId(), request.getAttributes().get(Request.KEY4_TRANSACTION_ID));
		assertEquals("Transaction Setted.", request.getTransactionId());
	}

	/**
	 * Test method for {@link br.com.auster.dware.graph.Request#getAttributes()}.
	 */
	public void testGetAttributes() {
		FileRequest request = new FileRequest(file);
		assertEquals(2, request.getAttributes().size());
		
		request.setTransactionId("Transaction Setted.");
		assertEquals(3, request.getAttributes().size());
	}
}
