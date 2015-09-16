package br.com.auster.dware.request;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;

public abstract class BaseRequestBuilder implements RequestBuilder {

	public class BuilderArgs {

		private String transactionId;

		private Map requestParams;

		protected BuilderArgs(String transactionId, Map requestParams) {
			this.transactionId = transactionId;
			this.requestParams = requestParams;
		}

		public String getTransactionId() {
			return this.transactionId;
		}

		public Map getRequestParams() {
			return this.requestParams;
		}

	}

	public class StaticAttributes {

		protected static final String STATIC_ATTRS_ELEMENT = "static-attributes";

		protected static final String STATIC_ELEMENT = "static";

		protected static final String STATIC_NAME_ATT = "name";

		protected static final String STATIC_TYPE_ATT = "type";

		protected static final String STATIC_LIST_ELEMENT = "static-list";

		protected static final String STATIC_LIST_ENTRYTYPE_ATT = "element-type";

		protected static final String STATIC_LIST_ENTRYVALUE_ELEMENT = "value";

		//		  protected static final Logger log = LogFactory.getLogger(StaticAttributes.class);

		protected Map attrs;

		public StaticAttributes() {
			attrs = new HashMap();
		}

		public void configure(Element _configuration) {
			
			this.attrs = new HashMap();
			Element staticsRoot = DOMUtils.getElement(_configuration,
					STATIC_ATTRS_ELEMENT, false);
			try {
				if (staticsRoot != null) {
					NodeList statics = DOMUtils.getElements(staticsRoot,
							                                STATIC_ELEMENT);
					for (int i = 0; i < statics.getLength(); i++) {
						Element staticElement = (Element) statics.item(i);
						String name = DOMUtils.getAttribute(staticElement,
								                            STATIC_NAME_ATT, 
								                            true);
						String type = DOMUtils.getAttribute(staticElement, 
												            STATIC_TYPE_ATT, 
												  			true);
						this.attrs.put(name, 
								       handleStatic(staticElement, type));
					}
					statics = DOMUtils.getElements(staticsRoot,
												   STATIC_LIST_ELEMENT);
					for (int i = 0; i < statics.getLength(); i++) {
						Element staticElement = (Element) statics.item(i);
						String name = DOMUtils.getAttribute(staticElement,
	                                                        STATIC_NAME_ATT, 
	                                                        true);
						String type = DOMUtils.getAttribute(staticElement, 
				  				                            STATIC_TYPE_ATT, 
				  				                            true);
						Collection collection = handleCollection(type);
						this.attrs.put(name, collection);
						NodeList collValues = DOMUtils.getElements(
								staticElement, STATIC_LIST_ENTRYVALUE_ELEMENT);
						for (int j = 0; j < collValues.getLength(); j++) {
							Element staticValue = (Element) collValues.item(j);
							String entryType = 
								DOMUtils.getAttribute(staticElement,
									                  STATIC_LIST_ENTRYTYPE_ATT, 
									                  true);
							collection.add(handleStatic(staticValue, entryType));
						}
					}
				}
			} catch (Exception e) {
				log.error(i18n.getString("errorHandlingStatics", e.getMessage()));
				log.debug("", e);
			}
		}

		private Object handleStatic(Element _entry, String _type)
				throws Exception {
			String value = DOMUtils.getText(_entry).toString();
			Class klass = Class.forName(_type);
			Constructor constructor = klass
					.getConstructor(new Class[] { java.lang.String.class });
			return constructor.newInstance(new Object[] { value });
		}

		private Collection handleCollection(String _type) throws Exception {
			return (Collection) Class.forName(_type).newInstance();
		}

		/**
		 * Duplicates all static constants. This will avoid that the requests created have pointers to the 
		 * 	lists contained in the static map. 
		 * 
		 * NOTE: IF SOMEDAY WE MADE THIS STATIC-ATTRIBUTES FEATURE MORE POWERFUL (Maps of Maps, for example) THIS
		 *  METHOD MUST BE REVIEWED AND MODIFIED SO INFORMATION IS NOT SHARED BETWEEN REQUESTS AND THE BUILDER 
		 *  CONFIGURATION
		 */
		public void insertStatics(Map _destination) {
			for (Iterator it = attrs.entrySet().iterator(); it.hasNext();) {
				Map.Entry entry = (Map.Entry) it.next();
				Object inDestination = entry.getValue();
				if (entry.getValue() instanceof Collection) {
					inDestination = new ArrayList();
					((Collection) inDestination).addAll((Collection) entry.getValue());
				}
				_destination.put(entry.getKey(), inDestination);
			}
		}

	}


    /**
	 * "{@value}": builder argument for a String representing the
	 * transaction ID of all requests.
	 */
	public static final String TRANSACTION_ID_ARG = "transaction-id";

	/**
	 * "{@value}": builder argument for a Map representing all attributes that
	 * must be stored in each request.
	 */
	public static final String REQUEST_PARAMS_ARG = "request-params";
	
	private static final Logger log = LogFactory.getLogger(BaseRequestBuilder.class);
	
	private final I18n i18n = I18n.getInstance(BaseRequestBuilder.class);
	
    // name of this builder - provided in constructor
	protected final String name;
	
	// configured static values for requests
	// Map<String(attr. name), Object>
	protected final StaticAttributes staticAttributes;

	/*****************************************************************************
	 * The constants below are used in configuration file to configure what
	 * request logs are to be processed
	 */
	public static final Logger	LOG_ACCEPTED							= Logger.getLogger("request.accepted");
	public static final Logger	LOG_REJECTED							= Logger.getLogger("request.rejected");
	public static final Logger	LOG_ALL										= Logger.getLogger("request.all");
//	/*****************************************************************************
//	 * Will write a message to writer stating a request was accepted for
//	 * processing
//	 */
//	private Writer							logAcceptedWriter					= null;
//
//	/*****************************************************************************
//	 * Will write a message to writer stating a request was rejected for
//	 * processing
//	 */
//	private Writer							logRejectedWriter					= null;
//
//	/*****************************************************************************
//	 * Will write a message to writer stating the state (Rejected OR Accepted) of
//	 * a request
//	 */
//	private Writer							logAllWriter							= null;

	protected BaseRequestBuilder(String name, Element config) {
		this.name = name;
		
		this.staticAttributes = new StaticAttributes();
		this.staticAttributes.configure(config);

	}

	public String getName() {
		return this.name;
	}

	/**
	 * Parses all arguments. This is kind of the command-line arguments of the
	 * builder, except that is not necessarily coming from the command-line!
	 * 
	 * <p>
	 * This implementation accepts the following args:
	 * <ul>
	 * <li>{@link #TRANSACTION_ID_ARG}: (String) will be stored in each
	 * request
	 * <li>{@link #REQUEST_PARAMS_ARG}: (Map<String, Object>) parameters
	 * that will be stored in each request
	 * </ul>
	 * 
	 * <p>
	 * List arguments must be delimited by "{@link #ARG_LIST_DELIMITER}".
	 * 
	 * @param args
	 *          Map<String, Object> with all arguments for this Request Builder.
	 * @return an instance of {@link BuilderArgs} with all parsed arguments.
	 */
	protected BuilderArgs parseArgs(Map args) {
	    if (args == null) {
	        args = new HashMap();
	      }
	    
	      Object value;
	      
	      String transactionId = null;
	      value = args.get(TRANSACTION_ID_ARG);
	      if (value != null) {
	        checkArgType(TRANSACTION_ID_ARG, value, String.class);
	        transactionId = (String) value;
	      }
	      
	      Map requestParams = null;
	      value = args.get(REQUEST_PARAMS_ARG);
	      if (value != null) {
	        checkArgType(REQUEST_PARAMS_ARG, value, Map.class);
	        requestParams = (Map) value;      
	      }
	      
	      return new BuilderArgs(transactionId, requestParams);
	}

	  /**
	   * Helper method that checks if the given <code>value</code> is of the
	   * <code>expectedType</code> class type.
	   * 
	   * <p>
	   * The <code>name</code> is used to build the exception message.
	   * 
	   * @param name
	   * @param value
	   * @param expectedType
	   *          class that <code>value</code> should be.
	   * @throws IllegalArgumentException
	   *           if the type is not the excepted.
	   */
	  protected void checkArgType(String name, Object value, Class expectedType) {
	    if (!expectedType.isInstance(value)) {
	      throw new IllegalArgumentException(i18n.getString("invalidArgument", 
	                                                        name, 
	                                                        value.getClass().getName(), 
	                                                        expectedType.getName()));
	    }
	  }
	  
	public abstract RequestFilter createRequests(Map args);

	public abstract RequestFilter createRequests(RequestFilter filter, Map args);

}
