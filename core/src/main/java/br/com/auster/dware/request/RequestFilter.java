package br.com.auster.dware.request;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import br.com.auster.dware.graph.Request;

/**
 * Implementations of this interface are able to filter which
 * <code>Request</code> objects can be processed.
 * 
 * Additionaly, the filter will hold the list of accepted requests.
 *
 * @author Ricardo Barone
 * @version $Id: RequestFilter.java 336 2007-12-26 13:14:17Z mtengelm $
 */
public interface RequestFilter {

  /**
   * Tests if a specified request should be included in a
   * request processing list. Same as <code>accept(request, false)</code>.
   * 
   * If the request was accepted, it will be added to the internal
   * list of accepted requests. If you don't want it to be added, see
   * {@link #accept(Request,boolean)}.
   *
   * @param request the request to be tested.
   * @return <code>true</code> if and only if the request should be included in the processing list.
   */
  public boolean accept(Request request);

  /**
   * Tests if a specified request should be included in a
   * request processing list.
   * 
   * If the request was accepted, it will be added to the internal
   * list of accepted requests, according to the <code>ignore</arg>
   * parameter.
   *
   * @param request the request to be tested.
   * @param ignore if the request should be ignored (true) or stored (false), after being accepted. 
   * @return <code>true</code> if and only if the request should be included in the processing list.
   */
  public boolean accept(Request request, boolean ignore);
  
  /**
   * Tests if a specified request should be included in a
   * request processing list without affecting the internal
   * list of accepted requests.
   *
   * @param request the request to be tested. 
   * @return <code>true</code> if and only if the request should be included in the processing list.
   */
  public boolean willAccept(Request request);

  /**
   * Informs if the filter can accept further Requests.
   * 
   * A return value of <code>false</false> means that all subsequent 
   * calls to <code>accept</code> will <b>always</b> return <code>false</code>.
   * 
   * @return <code>true</code> if the filter still can accept Requests.
   */
  public boolean canAccept();
  
  /**
   * Forces all remaining requests to be accepted no matter if they should be or
   * not (by calling the <code>accept()</code> method, for example), and will
   * store all those requests in the accepted request list. If the filter
   * doesn't maintain an internal list of remaining requests, this method will
   * do nothing.
   * 
   * <p>
   * Be aware that this method unconditionally accepts all remaining requests!
   */
  public void acceptAll();

  /**
   * Returns the list of all accepted requests.
   * 
   * @return an unmodifiable list of all accepted requests.
   */
  public Collection getAcceptedRequests();

  /**
   * Returns a list of all remaining requests to be accepted.
   * 
   * This method must always return an empty list if the filter
   * does not maintain a list of requests to be accepted.
   * 
   * @return the list of remaining requests.
   */
  public Collection getRemainingRequests();
  
  /**
   * Passes a list of requests that were accepted by a previous filter in a
   * chain. The filter implementation can than decide if it will take this list
   * into consideration when accepting requests.
   * 
   * NOTE: it makes sense to call this method before using the accept method for
   * the first time.
   * 
   * @param requests
   *          the list of requests accepted by the previous filter.
   */
  public void setPreviousRequests(Collection requests);

  /**
   * Resets the filter for a new filtering round.
   * All internal state should be reset also (optional).
   * 
   * <p>
   * The details of the reset behaviour is implementation-dependent.
   * 
   * @return a reset (and possibly new) <code>RequestFilter</code> instance.
   */
  public RequestFilter reset();
  
  /***
   * Returns a typed Map of Allowed request for this filter.
   * The K&lt;String&gt;> is the request user_key
   * 
   * @return
   */
  public Map<String, List<Request>> getAllowedRequests();
}
