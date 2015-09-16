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
package br.com.auster.dware.graph;

import org.w3c.dom.Element;

/**
 * Classes that implement this interface are used by DataAware to process a
 * request, doing a commit or rollback at the end.
 * 
 * @version $Id: Filter.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface Filter {

  /**
   * Returns the name of this filter.
   */
  public String getFilterName();

  /**
   * This method is used to configure the filter.
   * 
   * @param config
   *          the root of the DOM tree that contains this filter configuration.
   */
  public void configure(Element config) throws FilterException;

  /**
   * Tells the filter to prepare to process this request.
   * 
   * @param request
   *          the request to be processed.
   */
  public void prepare(Request request) throws FilterException;

  /**
   * Commits the last request processed. This method is only called if all other
   * filters has finished nicely for this request.
   */
  public void commit();

  /**
   * Rollback the last request processed. This method is called if some other
   * filter in the same graph has failed to process this request. IT MAY BE
   * CALLED AT ANY MOMENT, indicating that this or some other filter failed to
   * execute and must be stopped now.
   */
  public void rollback();
}
