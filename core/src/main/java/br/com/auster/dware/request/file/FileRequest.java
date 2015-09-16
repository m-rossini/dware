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

import br.com.auster.dware.graph.Request;

/**
 * This class represents a file request to be processed by the graph.
 * 
 * @version $Id: FileRequest.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public class FileRequest extends Request {

  protected File file;
  
  protected FileRequest() {
    // do nothing - used only be child classes
  }

  public FileRequest(File file) {
    this(null, file);
  }

  public FileRequest(String userKey, File file) {
    this.file = file;
    if (userKey == null || userKey.length() == 0) {
      setUserKey(file.getPath());
    } else {
      setUserKey(userKey);
    }
  }

  public long getWeight() {
    return this.file.length();
  }

  public String toString() {
    String key = getUserKey().length() > 0 ? getUserKey() + ":" : "";
    return "[" + key + getFile() + ":" + getWeight() + "]";
  }

  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof FileRequest)) {
      return false;
    }
    FileRequest other = (FileRequest) obj;
    if (this.getId() == null) {
      return this.file.equals(other.getFile());
    }
    return this.getId().equals(other.getId());
  }

  public int hashCode() {
    return getId().hashCode();
  }

  public File getFile() {
    return this.file;
  }

}
