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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.graph.Request;

/**
 * TODO class comments 
 * 
 *
 * @author Ricardo Barone
 * @version $Id: RequestBuilderManager.java 157 2006-02-24 19:37:41Z rbarone $
 */
public class RequestBuilderManager {

  public static final String NAMESPACE_URI = "http://www.auster.com.br/dware/requests/"; 
  public static final String CHAIN_ELEMENT = "builder-chain"; 
  public static final String BUILDER_ELEMENT = "builder";
  public static final String CONFIG_ELEMENT = "config";
  public static final String NAME_ATT = "name";
  public static final String CLASS_ATT = "class-name";

  private static final Logger log = LogFactory.getLogger(RequestBuilderManager.class);

  // key = String(builder name) 
  // value = RequestBuilderManager.BuilderDefinition
  private final Map builders = new HashMap();
  
  // key = String(chain name)
  // value = br.com.auster.dware.request.RequestBuilderChain
  private final Map chains = new HashMap();
  
  private final I18n i18n = I18n.getInstance(RequestBuilderManager.class);

  protected class BuilderDefinition {
    
    private String name;
    private Class type;
    private Element args;
    
    public BuilderDefinition(String name, Class type, Element args) {
      this.name = name;
      this.type = type;
      this.args = args;
    }
    
    public String getName() {
      return this.name;
    }
    
    public Class getType() {
      return this.type;
    }
    
    public Element getArgs() {
      return this.args;
    }
    
  }
  
  
  /**
   * TODO comments
   * 
   * @param config
   */
  public RequestBuilderManager(Element config) {
    log.info(i18n.getString("startConfig", RequestBuilderManager.class.getName()));

    // search for main configuration node
    if (config.getNamespaceURI() == null || !config.getNamespaceURI().equals(NAMESPACE_URI)) {
      config = DOMUtils.getElement(config, NAMESPACE_URI, DataAware.CONFIGURATION_ELEMENT, true);
    }
    
    // configure all declared builders
    NodeList nodes = DOMUtils.getElements(config, BUILDER_ELEMENT); 
    for (int i = 0; i < nodes.getLength(); i++) {
      BuilderDefinition def = configBuilder( (Element) nodes.item(i) );
      this.builders.put(def.getName(), def);
    }    
    
    // configure all declared chains
    final NodeList chainEtls = DOMUtils.getElements(config, CHAIN_ELEMENT);
    for (int i = 0; i < chainEtls.getLength(); i++) {
      final Element node = (Element) chainEtls.item(i);
      String name = DOMUtils.getAttribute(node, NAME_ATT, true);
      this.chains.put( name, configChain(name, node) );      
    }
    
    // sanity check
    if (this.chains.size() == 0) {
      throw new IllegalArgumentException(i18n.getString("managerInvalidConfig"));
    }
    
    // Initialize the error handler
    RequestErrorHandler.init(config);
    
    log.info(i18n.getString("endConfig", RequestBuilderManager.class.getName()));
  }
  
  /**
   * Same as
   * {@link #createRequests(String, Map, List) getRequests(chainName, args, null)}.
   * 
   * @param chainName
   *          the name of the request builder chain that will be used to create
   *          new requests.
   * @param args
   *          Map<String(chain name), Map<String(arg name), Object(arg
   *          value)>> containing all arguments for each builder.
   * @return List<br.com.auster.dware.graph.Request> the created requests.
   * @see #createRequests(String, Map, List)
   */
  public Collection createRequests(String chainName, Map args) {
    return createRequests(chainName, args, null);
  }
  
  /**
   * Create a list of requests using the given chain name.
   * 
   * @param chainName
   *          the name of the request builder chain that will be used to create
   *          new requests.
   * @param args
   *          Map<String(chain name), Map<String(arg name), Object(arg
   *          value)>> containing all arguments for each builder.
   * @param desiredIds
   *          List<String(request.getId())> containing the desired request userIDs
   *          you want to process.
   * @return List<br.com.auster.dware.graph.Request> the created requests.
   */
  public Collection createRequests(String chainName, Map args, List desiredIds) {
    final RequestBuilderChain chain = (RequestBuilderChain) this.chains.get(chainName);
    if (chain == null) {
      throw new IllegalArgumentException(i18n.getString("invalidChainName", chainName));
    }
    
    if (args == null) {
      args = new HashMap();
    }
    
    if (desiredIds == null) {
      return chain.createRequests(args).getAcceptedRequests();
    }
    
    // build list of desired requests
    List desiredRequests = new ArrayList(desiredIds.size());
    for (Iterator it = desiredIds.iterator(); it.hasNext();) {
      desiredRequests.add(new NullRequest((String) it.next()));
    }
    
    // create requests
    RequestFilter filter = new HashRequestFilter(desiredRequests, true);
    filter.acceptAll();
    try {
      filter = chain.createRequests(filter, args);
    } catch (Exception e) {
      RequestErrorHandler.handleError(null, e);
    }

    // Handle missing requests
    HashRequestFilter f = new HashRequestFilter(filter.getAcceptedRequests(), false);
    for (Iterator it = desiredRequests.iterator(); it.hasNext();) {
      Request request = (Request) it.next();
      if (!f.accept(request, true)) {
        String message = i18n.getString("requestNotFound", request.toString());
        RequestErrorHandler.handleError(request, new RequestNotFoundException(message));
      }
    }
    
    return filter.getAcceptedRequests();
  }
  
  /**
   * TODO comments
   * 
   * @param config
   * @return
   */
  private BuilderDefinition configBuilder(final Element config) {
    // get the configuration name
    String name = DOMUtils.getAttribute(config, NAME_ATT, true);
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException(i18n.getString("missingBuilderName"));
    }
    
    final Element args = DOMUtils.getElement(config, CONFIG_ELEMENT, false);
    BuilderDefinition def;
    
    // check if we already have the definition for the configured class-name
    String className = DOMUtils.getAttribute(config, CLASS_ATT, false);
    if (className == null || className.length() == 0) {
      // no class-name specified, so it must be a reference to a
      // previously declared builder (otherwise is an error...)
      if (!this.builders.containsKey(name)) {
        throw new IllegalArgumentException(i18n.getString("builderNotDeclared", name));
      }
      def = (BuilderDefinition) this.builders.get(name);
      if (args != null) {
        // new config element used inside chain, let's add
        // all news nodes to the beginning of the builder
        final Element builderArgs = (Element) def.getArgs().cloneNode(true);
        final NamedNodeMap atts = args.getAttributes();
        for (int i = 0; i < atts.getLength(); i++) {
          builderArgs.setAttributeNode((Attr)atts.item(i));
        }
        Element previous = DOMUtils.getFirstChild(builderArgs);
        Element next = DOMUtils.getLastChild(args);
        while (next != null) {
          if (previous == null) {
            builderArgs.appendChild(next);
          } else {
            builderArgs.insertBefore(next, previous);
          }
          previous = next;
          next = DOMUtils.getPreviousSibling(previous);
        }
        def = new BuilderDefinition(def.getName(), def.getType(), builderArgs);
      }
    } else {
      // first time we've declared this builder, create the definition
      final Class clazz;
      try {
        clazz = Class.forName(className);
      } catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(i18n.getString("classNotFound", className));
      }
      if ( !(RequestBuilder.class.isAssignableFrom(clazz)) ) {
        throw new IllegalArgumentException(i18n.getString("invalidBuilderType", className));
      }
      def = new BuilderDefinition(name, clazz, args);
    }
    
    return def;
  }
  
  /**
   * TODO comments
   * 
   * @param config
   * @return a List<br.com.auster.dware.request.RequestBuilder> of all builder
   *         instances created from the configuration.
   */
  private RequestBuilderChain configChain(String name, final Element config) {
    final RequestBuilderChain chain = new RequestBuilderChain(name, config);
    NodeList nodes = DOMUtils.getElements(config, BUILDER_ELEMENT); 
    for (int j = 0; j < nodes.getLength(); j++) {
      final BuilderDefinition def = configBuilder( (Element) nodes.item(j) );
      
      final Constructor c;
      try {
        c = def.getType().getDeclaredConstructor(new Class[] { String.class, Element.class });
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException(e.getMessage());
      }
      
      final RequestBuilder builder;
      try {
        builder = (RequestBuilder) c.newInstance(new Object[] { def.getName(), def.getArgs() });
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalArgumentException(e.getMessage());
      }
      
      chain.addBuilder(builder);
    }
    // sanity check
    if (chain.getBuilders().size() == 0) {
      throw new IllegalArgumentException(i18n.getString("managerInvalidConfig"));
    }
    return chain;
  }
  
}
