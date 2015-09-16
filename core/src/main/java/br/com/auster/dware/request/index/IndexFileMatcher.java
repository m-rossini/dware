/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 23/11/2007
 */
package br.com.auster.dware.request.index;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import org.apache.log4j.Logger;
import br.com.auster.common.io.NIOBufferUtils;
import br.com.auster.dware.request.file.PartialFileRequest;
import br.com.auster.dware.request.utils.RequestUtils;

/**
 * TODO What this class is responsible for
 *
 * @author mtengelm
 * @version $Id$
 * @since JDK1.4
 */
public class IndexFileMatcher implements Callable<Set<PartialFileRequest>> {

	private static final Logger log = Logger.getLogger(IndexFileMatcher.class);
	private Set<String>	list;
	private Set<PartialFileRequest> toReturnSet = new HashSet<PartialFileRequest>();
	private File	file;
	private byte[]	sep;
	private ByteBuffer	indexBB;
	private Charset							charset;
	private CharsetDecoder			decoder;
	private List<IndexMatcherDef>	indexPieces;

	/**
	 * Creates a new instance of the class <code>IndexFileMatcher</code>.
	 */
	public IndexFileMatcher(Set<String> list, List<IndexMatcherDef> indexPieces, File file, byte[] sep, ByteBuffer indexBB) {
		this.list = list;
		this.file = file;
		this.sep = sep;
		this.indexBB = indexBB;
		this.indexPieces = indexPieces;
		
		configureBuffers();
		log.trace("Starting to process file named:"  + file.getAbsolutePath());
	}

	/*****************************************************************************
	 * Once this method configures the Buffers for indexes reading it can be the
	 * default buffer configuration
	 * 
	 */
	protected void configureBuffers() {
		this.charset = Charset.defaultCharset();
		this.decoder = this.charset.newDecoder();
	}
	/**
	 * TODO why this methods was overriden, and what's the new expected behavior.
	 * 
	 * @return
	 * @throws Exception
	 * @see java.util.concurrent.Callable#call()
	 */
	public Set<PartialFileRequest> call() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		FileChannel channel = fis.getChannel();
		boolean hasRecords = true;
		boolean notEOF = true;

		log.trace("Before while for file.");
		while (hasRecords && notEOF) {
			int read = channel.read(indexBB);
			if (read <= 0) {
				notEOF = false;
				continue;
			}
			if (!findRecords(this.indexBB, sep)) {
				break;
			}
		}		
		channel.close();
		fis.close();
		return this.toReturnSet;
	}

	protected boolean findRecords(ByteBuffer bb, byte[] sep) {
		log.trace("Entering findRecords");
		int lastPos = 0;
		bb.flip();
		byte[] record = null;
		while (true) {
			int pos = NIOBufferUtils.findToken(bb, sep); // Find a new record
																														// end position
			if (pos == -1) {
				log.debug("Buffer Exausthed. Proceeding for a new READ.");
				bb.compact();
				break;
			}
			
			if (pos != lastPos) {
				//This is to avoid creating new objects all the time.
				record = new byte[pos];
			}
			
			bb.get(record);
			
			//Skip the separator in the buffer.
			bb.position(bb.position() + sep.length);
			
			decodeAndHanle(record);
			if (this.list.isEmpty()) {
				log.warn("There is no more elements on desired list for me.Returning");
				return false;
			}
			
			bb.compact();
			bb.flip();
			lastPos=pos;
		}
		
		return true;
	}
		
	/**
	 * TODO what this method is responsible for
	 * <p>
	 * Example:
	 * <pre>
	 *    Create a use example.
	 * </pre>
	 * </p>
	 * 
	 * @param record
	 */
	protected void decodeAndHanle(byte[] record) {
		log.trace("Entering decode");
		//We need just one decode operation once we areallocating the whole char buffer for every request.
		CharBuffer cc = CharBuffer.allocate((int) (this.decoder.averageCharsPerByte() * record.length));
		this.decoder.reset();
		this.decoder.decode(ByteBuffer.wrap(record), cc, true);
		this.decoder.flush(cc);
		cc.flip();	
		
		//Now lets handle it.
		//We do Support the Index File Version ID?
		if (cc.charAt(0) != '0' ||
				cc.charAt(1) != '0' ||
				cc.charAt(2) != '1') {
			String message = "This builder does not support the Index File Version. Supported Version is 001 file version is " + cc.subSequence(0, 3).toString();
			IllegalArgumentException ex = new IllegalArgumentException(message);
			log.fatal(message, ex);
			throw ex;
		}
		
		//TODO Separator MUST be configurable
		String[] fields = cc.toString().split(";");

		//Version 1 Support. Second Field (Index 1) Is the User Key.
		String combined = RequestUtils.assemblePieces(indexPieces, fields[1]);
		if (this.list.contains(combined)) {
			log.debug("Found a match:" + combined); 
			PartialFileRequest partialFileRequest = new PartialFileRequest(combined,Long.parseLong(fields[3]),Long.parseLong(fields[4]), new File(fields[5]));
			partialFileRequest.setTransactionId(fields[2]);
			for (int i=6; i < fields.length; i+=2) {
				partialFileRequest.getAttributes().put(fields[i], fields[i+1]);
			}
			log.trace("Request:" + partialFileRequest);
			this.toReturnSet.add(partialFileRequest);
		}
	}
	
}
