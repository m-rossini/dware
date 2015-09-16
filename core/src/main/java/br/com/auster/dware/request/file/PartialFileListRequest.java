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

import br.com.auster.dware.graph.Request;

/**
 * This class represents a request to process a list of partial file requests.
 * 
 * @version $Id: PartialFileListRequest.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public class PartialFileListRequest extends Request {

  protected final PartialFileRequest[] fileRequests;

  protected long weight = 0;

  protected String string;

  /**
   * @param fileList
   *          a list of <code>PartialFileRequest</code> to process.
   */
  public PartialFileListRequest(PartialFileRequest[] fileRequests) {
    this(null, fileRequests);
  }

  public PartialFileListRequest(String userKey, PartialFileRequest[] fileRequests) {
    this.fileRequests = fileRequests;

    // Get the weight of this request and create a string
    // representation of this request
    this.string = "[";
    for (int i = 0; i < fileRequests.length; i++) {
      PartialFileRequest req = fileRequests[i];
      this.weight += req.getLength();
      this.string += req + (i < fileRequests.length - 1 ? ", " : "");
    }
    this.string += "]";
    
    if (userKey == null || userKey.length() == 0) {
      setUserKey(this.string);
    } else {
      setUserKey(userKey);
    }
  }

  public PartialFileRequest[] getFiles() {
    return this.fileRequests;
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof PartialFileListRequest)) {
      return false;
    }
    return ((PartialFileListRequest) obj).getId().equals(this.getId());
  }

  public int hashCode() {
    return getId().hashCode();
  }

  public long getWeight() {
    return this.weight;
  }

  public String toString() {
    return this.string;
  }

}
