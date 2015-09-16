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
 * Created on 13/12/2007
 */
package br.com.auster.dware.request.utils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Arrays;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Esta classe representa Strings em bytes.
 * A Representação é feita sempre como um byte[] e é ncessário um 
 * Charset que mapeia esta representação de volta para String.
 * 
 * @author mtengelm
 * @version $Id$
 * @since 13/12/2007
 */
public class EncodedString {

	private final byte[]	data;
	private Charset	charset;
	private CharsetDecoder	decoder;

	public EncodedString(byte[] field, String charsetName) {
		this.data = field;
		this.charset = Charset.forName(charsetName);
		this.decoder = this.charset.newDecoder();
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof EncodedString)) {
			return false;
		}
		return Arrays.equals(this.data, ((EncodedString) obj).data);
	}

	public int hashCode() {
		return new HashCodeBuilder().append(this.data).toHashCode();
	}

	public byte[] getData() {
		return data;
	}

	public String toString() {
		if (this.data == null) {
			return null;
		}
		String result;
		try {
			ByteBuffer buffer = ByteBuffer.wrap(this.data);
			result = this.decoder.decode(buffer).toString();
		} catch (Exception e) {
			result = data.toString();
		}
		return result;
	}
}
