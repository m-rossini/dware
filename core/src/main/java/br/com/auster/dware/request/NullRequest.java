/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on Apr 4, 2005
 */
package br.com.auster.dware.request;

import br.com.auster.dware.graph.Request;

/**
 * The NullRequest is a request that carries no information besides it's ID.
 * 
 * The weight will always be 0.
 *
 * @author Ricardo Barone
 * @version $Id: NullRequest.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public class NullRequest extends Request {

  /**
   * Sole constructor. UserKey is mandatory for Null Requests.
   * 
   * The ID will be the same as the user key.
   * 
   * @param userKey the user key of the request
   */
  public NullRequest(String userKey) {
    super();
    setUserKey(userKey);
  }

  /**
   * @inheritDoc
   */
  public long getWeight() {
    return 0;
  }
  
  public boolean equals(Object obj) {
    if ( obj == null  || !(obj instanceof NullRequest) ) {
      return false;
    }
    return ((NullRequest) obj).getId().equals(this.getId());
  }

  public int hashCode() {
    return getId().hashCode();
  }
  
  public String toString() {
    return "[userKey = " + getId() + "]";
  }
  
}
