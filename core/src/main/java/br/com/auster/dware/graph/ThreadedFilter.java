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

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.stats.ProcessingStats;
import br.com.auster.common.stats.StatsMapping;
import br.com.auster.common.util.I18n;

/**
 * This is a default implementation of a filter that runs as a thread. Unlike
 * the implementation of DefaultFilter, it permits to run an algorithm on every
 * request that the graph receives to process.
 * 
 * It implements all Filter methods with empty algorithms. The Sink and Source
 * methods throw UnsupportedOperationException by default.
 * 
 * @version $Id: ThreadedFilter.java 362 2008-07-12 19:42:00Z lmorozow $
 */
public abstract class ThreadedFilter extends Thread implements Filter, Sink, Source, FilterMonitor {

  private static final Logger log = Logger.getLogger(ThreadedFilter.class);

  private final I18n i18n = I18n.getInstance(ThreadedFilter.class);

  private final String filterName;

  // Attributes for multi-threaded environment control
  private final Object syncGo = new Object();

  private final Object syncFinish = new Object();

  private volatile boolean closing = false;

  private volatile boolean processing = false;

  private Throwable problem = null;

  private Graph graph = null;

  public ThreadedFilter(String name) {
    this.filterName = name;
  }

  /**
   * Sets the graph that takes care of this filter.
   */
  protected final void setGraph(Graph graph) {
    this.graph = graph;
  }

  /**
   * Tells this thread that it may wake and start to process the request given
   * to it by the method <code>prepare</code>.
   * 
   * @param graph
   *          if not null, this object will have the method
   *          <code>threadedFilterFinished()</code> called back when it
   *          finishes to process the request.
   */
  public final void go() {
    synchronized (this.syncGo) {
      this.problem = null;
      synchronized (this.syncFinish) {
        while (this.processing)
          try {
            this.waitProcessing(-1);
          } catch (InterruptedException e) {
            log.debug("Got interruption.", e);
          }
        this.processing = true;
      }
      this.syncGo.notify();
    }
  }

  /**
   * Waits until the filter has finished processing the last request, if it's
   * processing some one.
   * 
   * @param milli
   *          the maximum time out, in milliseconds, that this method will wait
   *          for this processing to finish. If this value is negative, then it
   *          will wait with no time out.
   */
  public final void waitProcessing(int milli) throws InterruptedException {
    synchronized (this.syncFinish) {
      if (this.processing)
        if (milli < 0)
          this.syncFinish.wait();
        else
          this.syncFinish.wait(milli);
    }
  }

  /**
   * This method is executed in a separated thread, in assynchronous mode.
   */
  public final void run() {
    log.info(i18n.getString("filterThreadReady"));
    ProcessingStats.dontDumpMyStats();
    while (!this.closing) {
      try {
        // Waits the main thread to prepare the job
        synchronized (this.syncGo) {
          // Verifies it may go before waiting a signal
          if (!this.processing)
            this.syncGo.wait();
        }
      } catch (InterruptedException e) {
        // Ops, someone interrupted us. Let's finish.
        this.closing = true;
        log.warn(i18n.getString("gotInterruption"), e);
      }
      if (this.closing)
        log.debug("Filter Shutting Down");
      else {
        log.debug("Filter Started");
        StatsMapping stats = ProcessingStats.starting(getClass(), "process()");
        try {
          this.process();
        } catch (Throwable e) {
          log.debug("Problems inside filter " + this.getName(), e);
          this.problem = e;
        } finally {
          stats.finished();
          synchronized (this.syncFinish) {
            this.processing = false;
            this.syncFinish.notifyAll();
          }
          // Notifies that this filter finished its job
          if (this.graph != null)
            this.graph.threadedFilterFinished(this, this.problem);
          log.debug("Filter Finished");
        }
      }
    }
    log.warn(i18n.getString("filterStopped"));

    ProcessingStats.dumpMyStats("Dumping stats (ThreadedFilter stopped)");
    // Just in case this thread starts again.
    this.closing = false;
  }

  /**
   * Checks if this filter is still processing the request.
   * 
   * @return true if and only if this filter is still processing the request. In
   *         case of error, a call to this method will return false.
   */
  public final boolean isProcessing() {
    return this.processing;
  }

  /**
   * If this filter throw an exception, only this method will show the problem.
   * After a call to the <code>go()</code> method, any exception caused by the
   * last call to <code>go()</code> will be cleaned.
   * 
   * @return the exception that stopped the execution of the
   *         <code>process</code> method.
   */
  public final Throwable getProblem() {
    return this.problem;
  }

  /**
   * Shuts down this instance of FilterThread. This method is used to stop this
   * thread.
   * 
   * @param wait
   *          if true, this method will wait until this thread stops before
   *          returning.
   */
  public final void shutdown(boolean wait) {
    this.closing = true;
    this.go();
    if (wait) {
      try {
        log.info(i18n.getString("waitingThread", this.getName()));
        this.join();
      } catch (InterruptedException e) {
        log.warn(i18n.getString("gotInterruption"), e);
      }
    }
  }

  /**
   * This method is used to start the processing of the request.
   */
  public abstract void process() throws FilterException;

  /** ***************************************** */
  /* START OF FILTER INTERFACE IMPLEMENTATION */
  /** ***************************************** */

  /**
   * Do nothing.
   */
  public void configure(Element config) throws FilterException {
  }

  /**
   * Prints the filter name.
   */
  public String toString() {
    return this.filterName;
  }

  /**
   * Gets this filter name.
   */
  public String getFilterName() {
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
