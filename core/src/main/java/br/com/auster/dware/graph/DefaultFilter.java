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

import java.util.concurrent.atomic.AtomicLongArray;

import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;

/**
 * This is a default implementation of a filter. It implements all methods with
 * empty algorithms. The Sink and Source methods throw
 * UnsupportedOperationException by default.
 * 
 * @version $Id: DefaultFilter.java 80 2005-07-28 19:02:26Z mtengelm $
 */
public abstract class DefaultFilter implements Filter, Source, Sink, FilterMonitor {

  protected final String filterName;

  private final I18n i18n = I18n.getInstance(DefaultFilter.class);

  public DefaultFilter(String name) {
    this.filterName = name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see br.com.auster.dware.graph.FilterMonitor#getCounters()
   */
  public AtomicLongArray getCounters() {
     return null;
  }
  /* (non-Javadoc)
   * @see br.com.auster.dware.graph.FilterMonitor#resetCounters()
   */
  public void resetCounters() {      
  }
/**
   * Do nothing.
   */
  public void configure(Element config) throws FilterException {
  }

  /**
   * Gets this filter name.
   */
  public String getFilterName() {
    return this.filterName;
  }

  /**
   * Prints the filter name.
   */
  public String toString() {
    return this.filterName;
  }

  /**
   * Do nothing.
   */
  public void prepare(Request request) throws FilterException {
  }

  /**
   * Do nothing.
   */
  public void commit() {
  }

  /**
   * Do nothing.
   */
  public void rollback() {
  }

  /**
   * Default implementation. UnsupportedOperationException is thrown.
   */
  public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
    throw new UnsupportedOperationException(i18n.getString("methodNotSupported", "getInput"));
  }

  /**
   * Default implementation. UnsupportedOperationException is thrown.
   */
  public void setInput(String filterName, Object input) throws ConnectException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException(i18n.getString("methodNotSupported", "setInput"));
  }

  /**
   * Default implementation. UnsupportedOperationException is thrown.
   */
  public Object getOutput(String filterName) throws ConnectException, UnsupportedOperationException {
    throw new UnsupportedOperationException(i18n.getString("methodNotSupported", "getOutput"));
  }

  /**
   * Default implementation. UnsupportedOperationException is thrown.
   */
  public void setOutput(String filterName, Object output) throws ConnectException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException(i18n.getString("methodNotSupported", "setOutput"));
  }
}
