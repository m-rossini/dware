/*
 * Copyright (c) 2004-2006 Auster Solutions do Brasil. All Rights Reserved.
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
package br.com.auster.dware.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the superclass for every requests that wants to be
 * processed by the filter graphs.
 *
 * @version $Id: Request.java 281 2006-12-22 14:45:54Z mtengelm $
 */
public abstract class Request implements java.io.Serializable {

	public static final String KEY4_ID = "dware.id";
	public static final String KEY4_TRANSACTION_ID = "dware.transactionid";
	public static final String KEY4_USERKEY = "dware.userkey";
	
  /**
   * {@value}
   */
  public static final String COMPOSITE_KEY_DELIMITER = "_";
  
  private String transactionId = null;
  private String id = Integer.toString(super.hashCode());
  private String userKey = "";
  
  private Map attributes = Collections.synchronizedMap(new HashMap());

  /**
   * Gets how difficulty is to process this request. The bigger the value the
   * more difficulty is to process it.
   */
  public abstract long getWeight();
  
  /**
   * Gets the unique identifier of this request.
   * 
   * This is used by request builders, logs, etc to represent a request. Also
   * used by checkpoint/restart algorithms, to check if this request was already
   * processed.
   * 
   * @see #toString() for a more verbose alternative to identify a request.
   */
  public final String getId() {
    return this.id;
  }
  
  /***
   * Sets the Request ID. 
   * It Also puts the ID into the properties MAP.
   * @param id
   */
  private void setId(String id) {
    if (id == null) {
      throw new IllegalArgumentException("Request ID cannot be null");
    }
    this.id = id;
    this.attributes.put(KEY4_ID, id);
  }
  
  public int hashCode() {
    return getId().hashCode();
  }
  
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Request)) {
      return false;
    }
    Request other = (Request) obj;
    if (this.getId() == null) {
      return other.getId() == null;
    }
    return this.getId().equals(other.getId());
  }
  
  /**
   * Returns a string representation of this object. This is very used in log
   * and exception messages.
   */
  public String toString() {
    return super.toString();
  }

  /**
   * Gets the user key that identifies this request. This is a more
   * user-friendly identifier that should be populated with the business concept
   * of a request key, without having to worry about uniqueness.
   * 
   * @return the user key defined for this request.
   */
  public final String getUserKey() {
    return this.userKey;
  }
  
  public final void setUserKey(String key) {
    if (userKey != null && userKey.length() > 0) {
      throw new IllegalStateException("User Key was already set - modification not allowed");
    }
    this.userKey = (key == null ? "" : key);
    setId( buildId(getTransactionId(), this.userKey) );
    this.attributes.put(KEY4_USERKEY, userKey);    
  }

  /**
   * Returns the transaction ID to which this request belongs.
   * 
   * @return the transaction ID of the request or <code>null</code> if there
   *         is no transaction ID configured for this request.
   */
  public final String getTransactionId() {
    return this.transactionId;
  }
  
  public final void setTransactionId(String id) {
    if (transactionId != null && transactionId.length() > 0) {
      throw new IllegalStateException("Transaction ID was already set - modification not allowed");
    } else if (id == null) {
      throw new IllegalArgumentException("Transaction ID cannot be null");
    }
    this.transactionId = id;
    setId( buildId(this.transactionId, getUserKey()) );
    this.attributes.put(KEY4_TRANSACTION_ID, transactionId);    
  }

  /**
   * Returns the attributes configured for the request.
   * 
   * @return the Map<String(name), Object(value)> of all attributes configured
   *         for the request.
   */
  public final Map getAttributes() {
    return this.attributes;
  }
  
  public final void setAttributes(Map attributes) {
    if (attributes == null) {
      throw new IllegalArgumentException("Attributes Map cannot be null");
    }    
    this.attributes.putAll(attributes);
  }
  
  public static final String buildId(String transactionId, String userKey) {
    String id = "";
    if (transactionId != null && transactionId.length() > 0) {
      id += transactionId + COMPOSITE_KEY_DELIMITER;
    }
    id += userKey == null ? "" : userKey;
    return id;
  }

}
