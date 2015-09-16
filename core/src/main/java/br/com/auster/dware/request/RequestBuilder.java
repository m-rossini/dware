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
 * Created on Mar 31, 2005
 */
package br.com.auster.dware.request;

import java.util.Map;

/**
 * A request builder is responsible to create and return a list of requests.
 *
 * @author Ricardo Barone
 * @version $Id: RequestBuilder.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface RequestBuilder {

  public static final String ARG_LIST_DELIMITER = ","; 
  
  /**
   * Returns the builder's name according to the specified configuration.
   * This value is populated at creation time (constructors).
   * 
   * @return the name of this builder.
   */
  public String getName();
  
  /**
   * Creates and returns the list of requests as a RequestFilter.
   * Same as <code>getRequests(null, args)</code>.
   *
   * @param args a Map<String> with all arguments required by the builder.
   * @return A RequestFilter with the list of the created <code>Request</code> objects.
   */
  public RequestFilter createRequests(Map args);

  /**
   * Creates and returns the list of requests as a RequestFilter that
   * satisfy the specified filter.
   * 
   * <p>
   * If the specified filter is <code>null</null> all requests found
   * by the builder will be returned.
   *
   * @param filter a request filter.
   * @param args a Map<String> with all arguments required by the builder.
   * @return A RequestFilter with the list of the created <code>Request</code> objects.
   */
  public RequestFilter createRequests(RequestFilter filter, Map args);

}
