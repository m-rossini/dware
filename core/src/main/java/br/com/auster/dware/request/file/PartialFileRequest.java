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
package br.com.auster.dware.request.file;

import java.io.File;

/**
 * This class represents a piece of data from a input file. It has a offset, a
 * length and a file, so it can be find in the input stream.
 * 
 * @version $Id: PartialFileRequest.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public class PartialFileRequest extends FileRequest implements PartialDataRequest {

  protected final long offset, length;

  public PartialFileRequest(long offset, long length, File file) {
    this(null, offset, length, file);
  }

  public PartialFileRequest(String userKey, long offset, long length, File file) {
    this.offset = offset;
    this.length = length;
    this.file = file;
    if (userKey == null || userKey.length() == 0) {
      setUserKey(file.getPath() + COMPOSITE_KEY_DELIMITER + offset + 
                 COMPOSITE_KEY_DELIMITER + length);
    } else {
      setUserKey(userKey);
    }
  }

  public long getOffset() {
    return this.offset;
  }

  public long getLength() {
    return this.length;
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof PartialFileRequest)) {
      return false;
    }
    PartialFileRequest other = (PartialFileRequest) obj;
    return this.getId().equals(other.getId());
  }

  public int hashCode() {
    return getId().hashCode();
  }

  public long getWeight() {
    return this.length;
  }

  public String toString() {
    return "[" + this.getUserKey() + ": " + this.file + ": " + this.offset + "(+" + this.length
           + ")]";
  }

}
