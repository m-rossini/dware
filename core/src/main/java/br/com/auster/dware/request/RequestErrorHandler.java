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
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.Request;

/**
 * Base class that handles Request errors.
 * 
 * Configuration:
 * 
 * <pre>
 * 
 *  
 *  &lt;error-listener class-name=&quot;&lt;T extends RequestErrorListener&gt;&quot;/&gt;
 *  &lt;error-listener class-name=&quot;&lt;T extends RequestErrorListener&gt;&quot;/&gt;
 *  ...
 *  &lt;error-listener class-name=&quot;&lt;T extends RequestErrorListener&gt;&quot;/&gt;
 *  
 *  
 * </pre>
 * 
 * @author Ricardo Barone
 * @version $Id: RequestErrorHandler.java 272 2006-11-30 20:08:39Z rbarone $
 */
public class RequestErrorHandler {

  private static final String LISTENER_ELEMENT = "error-listener";

  private static final String CLASS_ATTR = "class-name";

  private static final Logger log = Logger.getLogger(RequestErrorHandler.class);

  private static final I18n i18n = I18n.getInstance(RequestErrorHandler.class);

  private static final List<RequestErrorListener> listeners = new ArrayList<RequestErrorListener>();;
  
  private static final DefaultRequestErrorListener defaultListener = new DefaultRequestErrorListener();

  /**
   * Intializes all listeners.
   * 
   * @param config
   *          the configuration Element containing all listeners to be
   *          registered.
   */
  public static final void init(Element config) {
    listeners.clear();
    NodeList listenerNodes = DOMUtils.getElements(config, LISTENER_ELEMENT);
    for (int i = 0; i < listenerNodes.getLength(); i++) {
      Element listenerNode = (Element) listenerNodes.item(i);
      String className = DOMUtils.getAttribute(listenerNode, CLASS_ATTR, true);
      try {
        log.debug("Instantiating the request error listener '" + className + "'.");
        Class[] c = { Element.class };
        Object[] o = { DOMUtils.getElement(listenerNode, Graph.CONFIG_ELEMENT, false) };
        RequestErrorListener listener = (RequestErrorListener) 
          Class.forName(className).getConstructor(c).newInstance(o);
        listeners.add(listener);
        log.info(i18n.getString("listenerReady", className));
      } catch (ClassNotFoundException e) {
        log.error(i18n.getString("listenerNotFound", className), e);
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        log.error(i18n.getString("listenerNotPermitted", className), e);
        e.printStackTrace();
      } catch (ClassCastException e) {
        log.error(i18n.getString("listenerInvalidType", className), e);
        e.printStackTrace();
      } catch (Exception e) {
        log.error(i18n.getString("listenerInvalid", className), e);
        e.printStackTrace();
      }
    }
  }

  /**
   * Notifies that an error has occured.
   * 
   * @param request
   *          the request that failed.
   * @param error
   *          the error that occured.
   * @throws IllegalArgumentException
   *           if the error is <code>null</code>
   */
  public static final void handleError(Request request, Throwable error) {
    if (error == null) {
      throw new IllegalArgumentException(i18n.getString("nullError"));
    } else if (listeners.size() == 0) {
      defaultListener.errorOccured(request, error);
    }
    for (Iterator it = listeners.iterator(); it.hasNext();) {
      RequestErrorListener listener = (RequestErrorListener) it.next();
      listener.errorOccured(request, error);
    }
  }
  
  /**
   * Notifies that an error has occured for a list of requests.
   * 
   * @param requests
   *          the requests that failed.
   * @param error
   *          the error that occured.
   * @throws IllegalArgumentException
   *           if the error is <code>null</code>
   */
  public static final void handleErrors(Collection<Request> requests, Throwable error) {
    if (error == null) {
      throw new IllegalArgumentException(i18n.getString("nullError"));
    }
    for (Request request : requests) {
      handleError(request, error);
    }
  }

}
