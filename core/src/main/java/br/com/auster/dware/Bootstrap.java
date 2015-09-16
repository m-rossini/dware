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
package br.com.auster.dware;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import br.com.auster.common.cache.ETManagerArgs;
import br.com.auster.common.cache.ExternalTableManager;
import br.com.auster.common.cli.CLOption;
import br.com.auster.common.cli.OptionsParser;
import br.com.auster.common.jmx.AusterManagementServices;
import br.com.auster.common.log.LogFactory;
import br.com.auster.common.sql.SQLConnectionManager;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestBuilderManager;
import br.com.auster.dware.request.RequestErrorHandler;
import br.com.auster.dware.request.comparators.RequestWeightComparator;
import com.thoughtworks.xstream.XStream;

/**
 * Class used to execute Data-A-Ware using a <code>RequestManager</code>.
 * 
 * <p>
 * Extensions of this class must only implement the {@link #check()} method.
 * 
 * <p>
 * Before using the {@link #process(String, Map, List) process},
 * {@link #shutdown(boolean) shutdown} or
 * {@link #enqueueRequests(Collection) enqueueRequests} methods, you need to
 * {@link #init(Element) initialize} the instance.
 * 
 * <p>
 * For a single (batch) execution, this class provides the method
 * {@link #execute(String[])} that will take care of the initialization (unless
 * the instance is already initialized).
 * 
 * @author Ricardo Barone
 * @version $Id: Bootstrap.java 368 2008-08-29 00:41:19Z lmorozow $
 */
public abstract class Bootstrap extends UnicastRemoteObject implements RemoteBootstrap {

	private final I18n									i18n												= I18n
																																			.getInstance(Bootstrap.class);

	// name of the command line parameters
	/*****************************************************************************
	 * 
	 */
	protected static List								options											= new ArrayList();
	/**
	 * {@value}
	 */
	public static final String					CONF_PARAM									= "xml-conf";
	/**
	 * {@value}
	 */
	public static final String					NO_ENCRYPT_PARAM						= "secret";
	/**
	 * {@value}
	 */
	public static final String					CHAIN_NAME_PARAM						= "builder-chain";
	/**
	 * {@value}
	 */
	public static final String					CHAIN_ARGS_PARAM						= "chain-args";
	/**
	 * {@value}
	 */
	public static final String					DESIRED_IDS_PARAM						= "desired-id";
	/**
	 * {@value}
	 */
	public static final String					PROVIDER_PARAM							= "provider-url";
	/**
	 * {@value}
	 */
	public static final String					ETM_PARAM										= "etm-args";

	/*****************************************************************************
	 * Save requests before enqueing them option.
	 */
	public static final String					SAVE_REQUEST_PARAM					= "save-request-file";

	/*****************************************************************************
	 * Instead of calling the request builder, de-serializes the previously
	 * serialized requests to enqueue them
	 */
	public static final String					RESTORE_REQUEST_PARAM				= "restore-request-file";

	/*****************************************************************************
	 * Just run process, ie, request builders.
	 */
	public static final String					INDEX_ONLY_PARAM						= "only-index-requests";

	/*****************************************************************************
	 * Put all requests above a up-bound value at the end of the request to
	 * enqueue list.
	 */
	public static final String					UPBOUND_OPT_PARAM						= "up-bound";

	/*****************************************************************************
	 * If up-bound values are informed, then sort them in the sort order value
	 */
	public static final String					UPBOUNDSORTED_OPT_PARAM			= "sort-order";
	/**
	 * {@value}
	 */
	public static final String					ARG_HASH_START							= "{{";
	/**
	 * {@value}
	 */
	public static final String					ARG_HASH_END								= "}}";

	// builds the regex pattern that will be used to parse hash arguments
	protected static final Pattern			argHashPattern							= Pattern
																																			.compile("^\\Q"
																																					+ ARG_HASH_START
																																					+ "\\E(.*)\\Q"
																																					+ ARG_HASH_END
																																					+ "\\E$");

	/**
	 * {@value}
	 */
	public static final String					ARG_LIST_START							= "[[";
	/**
	 * {@value}
	 */
	public static final String					ARG_LIST_END								= "]]";

	// builds the regex pattern that will be used to parse list arguments
	protected static final Pattern			argListPattern							= Pattern
																																			.compile("^\\Q"
																																					+ ARG_LIST_START
																																					+ "\\E(.*)\\Q"
																																					+ ARG_LIST_END
																																					+ "\\E$");

	/**
	 * {@value}
	 */
	public static final String					ETM_NAMESPACE_URI						= "http://www.auster.com.br/dware/etm/";
	/**
	 * {@value}
	 */
	public static final String					ETM_CONFIGURATION_ELEMENT		= "tables";
	/**
	 * {@value}
	 */
	public static final String					MANAGER_ELEMENT							= "manager";
	/**
	 * {@value}
	 */
	public static final String					CLASS_NAME_ATTR							= "class-name";

	/**
	 * {@value} - used for the SQLConnectionManager initialization.
	 */
	public static final String					FILENAME_ATTR								= "filename";

	/**
	 * {@value} - used for the SQLConnectionManager initialization.
	 */
	private static final String					NAME_ATTR										= "name";

	/**
	 * {@value} - Enables configuring a bootstrap listener
	 */
	public static final String					BOOTSTRAP_LISTENER_LIST	= "bootstrap-listener-list";
	public static final String					BOOTSTRAP_LISTENER_ELEMENT	= "bootstrap-listener";

	// you KNOW what this is, right?!
	protected static final Logger				log													= Logger
																																			.getLogger(Bootstrap.class);

	/**
	 * DataAware instance that will be used to process all requests.
	 */
	protected DataAware									dataAware;

	/**
	 * The requets manager that will be used to create requests.
	 */
	protected RequestBuilderManager			requestManager;

	/*****************************************************************************
	 * Holds the reference to the Management Services. An instance of this class
	 * will always be created, even in the absence of configuration.
	 */
	protected AusterManagementServices	dwms;

	/**
	 * The bootstrap listeners configured in the XML configuration file
	 */
	protected List<BootstrapListener>					bootstrapListenerList;

	/**
	 * Default Constructor - does nothing
	 */
	public Bootstrap() throws RemoteException {
		super();
	}

	/**
	 * Initializes Data-A-Ware to work with a request manager.
	 * 
	 * @param configRoot
	 *          the DOM tree root that contains all the configuration for
	 *          data-a-ware. It also configures the external table management.
	 *          ETM, is indicated by element "tables" and is a place holder (A
	 *          Java Class) that will be instantiated here in order to process and
	 *          keep in-memory all external flat files with data needed for
	 *          processing.
	 * @throws Exception
	 *           if the configuration has failed.
	 */
	public void init(Element configRoot) throws Exception {
		preInit(configRoot);

		// Creates the DataAware instance
		Element config = DOMUtils.getElement(configRoot, DataAware.DWARE_NAMESPACE_URI,
				DataAware.CONFIGURATION_ELEMENT, true);

		// Starts the Management Services, if configured.
		Element management = DOMUtils.getElement(configRoot,
				AusterManagementServices.MANAGEMENT_NAMESPACE_URI,
				AusterManagementServices.MANAGEMENT_CONFIGURATION_ELEMENT, false);
		this.dwms = new AusterManagementServices();
		if (management != null) {
			this.dwms.setConfig(management);
		}

		this.dataAware = new DataAware(config);

		// Now, we are gonna check the Product ID
		if (check() < 0) {
			throw new SecurityException("Product ID does not match");
		}

		// Starts the ETM.
		Element tables = DOMUtils.getElement(configRoot, ETM_NAMESPACE_URI,
				ETM_CONFIGURATION_ELEMENT, false);
		String etmArgs = OptionsParser.getOptionValue(ETM_PARAM);
		Map etmMap = null;
		if (etmArgs != null && etmArgs.length() > 0) {
			etmMap = parseMapArgs(etmArgs);
		}
		if (tables != null) {
			NodeList managers = DOMUtils.getElements(tables, MANAGER_ELEMENT);
			int qtd = managers.getLength();
			for (int i = 0; i < qtd; i++) {
				Element manager = (Element) managers.item(i);
				String etmName = DOMUtils.getAttribute(manager, CLASS_NAME_ATTR, true);
				Class[] c = { Element.class };
				Object[] o = { manager };
				ExternalTableManager etm = (ExternalTableManager) Class.forName(etmName)
						.getConstructor(c).newInstance(o);
				if (etm instanceof ETManagerArgs) {
					ETManagerArgs etma = (ETManagerArgs) etm;
					String name = DOMUtils.getAttribute(manager, NAME_ATTR, false);
					name = (name.length() > 0) ? name : etma.getClass().getName();
					etma.setParms(name, (Map) etmMap.get(name));
					etma.process();
				}
			}
		}

		Element bootListenerList = DOMUtils.getElement(configRoot, BOOTSTRAP_LISTENER_LIST, false);
		if (bootListenerList != null) { // tries to configure multiple bootstrap listeners
			NodeList bootListenerNodeList = DOMUtils.getElements(bootListenerList, BOOTSTRAP_LISTENER_ELEMENT);
			int size = bootListenerNodeList.getLength();
			for (int i = 0; i < size; i++) {
				Element listener = (Element) bootListenerNodeList.item(i);
				if (listener != null) {
					configureBootstrapListener(listener);
				}
			}
		} else { // configures a single bootstrap listener, if set
			Element bootListener = DOMUtils.getElement(configRoot, BOOTSTRAP_LISTENER_ELEMENT, false);
			if (bootListener != null) {
				configureBootstrapListener(bootListener);
			}
		}

		this.requestManager = new RequestBuilderManager(configRoot);
	}

	/**
	 * This method is called before any Data-Aware initialization and gives
	 * implementation classes a chance to manipulate/use the Data-Aware config
	 * element.
	 * 
	 * This implementation initializes the SQLConnectionManager if the
	 * sql:configuration element is found.
	 * 
	 * @param config
	 *          the Data-Aware config element.
	 */
	protected void preInit(Element config) throws Exception {
		Element sql = DOMUtils.getElement(config, SQLConnectionManager.SQL_NAMESPACE_URI,
				SQLConnectionManager.CONFIG_ELEMENT, false);
		if (sql != null) {
			SQLConnectionManager.init(config);
		}
	}

	/**
	 * Starts the 'server' mode using RMI.
	 * 
	 */
	public void initServer(String providerURL) {
		// Create and install a security manager
		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new RMISecurityManager());
		}

		// calling bootstrap listener
		if (this.bootstrapListenerList != null && this.bootstrapListenerList.size() > 0) {
			for (BootstrapListener b: this.bootstrapListenerList) { 
				b.onInitServer(this, providerURL);	
			}
		}

		// Bind this server instance
		try {
			Naming.rebind(providerURL, this);
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log.info("InvoiceServer bound in registry");

		/*
		 * // initialize the server (prepare for something...) if
		 * (this.requestResumerClassName != null) { RemoteRequestResumer
		 * requestResumer; try { Class resumerClass =
		 * Class.forName(this.requestResumerClassName); requestResumer =
		 * (RemoteRequestResumer) resumerClass.newInstance(); } catch (Exception e) {
		 * if (this.invoice != null) { this.invoice.shutdown(false); } throw new
		 * RemoteException(e.getLocalizedMessage(), e); }
		 * 
		 * try { this.resumePendingRequests(requestResumer); } catch (Exception e) {
		 * log.error("Could not resume all pending requests!", e); } }
		 */
	}

	/**
	 * Creates and enqueues all requests using the specified chain name, arguments
	 * and desired request list.
	 * 
	 * @param chainName
	 *          the name of the chain that will be used to create requests.
	 * @param args
	 *          the arguments for the chain.
	 * @param desiredRequests
	 *          the (optional) list of request user-ids to consider.
	 */
	public void process(String chainName, Map args, List desiredRequests) {
		if (this.dataAware == null || this.requestManager == null) {
			throw new IllegalStateException("Bootstrap have not been initialized.");
		}
		this.dataAware.checkMe();
		Collection<Request> requests;
		// We have to restore requests?
		String fileName = OptionsParser.getOptionValue(RESTORE_REQUEST_PARAM);
		if (fileName != null && !"".equals(fileName)) {
			// We have to restore. Is index only active? If so, ERROR.
			boolean isIndexOnlySet = findOption(OptionsParser.getOptions(), INDEX_ONLY_PARAM);
			if (isIndexOnlySet) {
				String msg = i18n.getString("incompatible.options", INDEX_ONLY_PARAM,
						RESTORE_REQUEST_PARAM);
				log.fatal(msg);
				throw new RuntimeException(msg);
			} else {
				requests = restoreRequets(fileName);
			}
		} else {
			requests = this.requestManager.createRequests(chainName, args, desiredRequests);
		}

		// calling bootstrap listener
		if (this.bootstrapListenerList != null && this.bootstrapListenerList.size() > 0) {
			for (BootstrapListener b: this.bootstrapListenerList) { 
				b.onProcess(this, chainName, args, desiredRequests, requests);
			}
		}

		enqueueRequests(requests);
	}

	/**
	 * TODO what this method is responsible for
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 *    Create a use example.
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param fileName
	 * @return
	 */
	protected Collection<Request> restoreRequets(String fileName) {
		XStream xs = new XStream();
		xs.addImplicitCollection(Request.class, "entries");
		xs.useAttributeFor(String.class);
		FileInputStream saveStream;
		List<Request> requests = new ArrayList<Request>();
		try {
			saveStream = new FileInputStream(fileName);
			Object[] objs = (Object[]) xs.fromXML(new GZIPInputStream(saveStream));
			for (int i = 0; i < objs.length; i++) {
				requests.add((Request) objs[i]);
			}
		} catch (FileNotFoundException e) {
			log.fatal(e);
			throw new RuntimeException(i18n.getString("file.restore.error", fileName), e);
		} catch (IOException e) {
			log.fatal(e);
			throw new RuntimeException(i18n.getString("file.restore.error", fileName), e);
		}

		return requests;
	}

	/**
	 * Same as <code>process(chainName, args, null)</code>.
	 * 
	 * @param chainName
	 *          the name of the chain that will be used to create requests.
	 * @param args
	 *          the arguments for the chain.
	 * @see #process(String, Map, List)
	 */
	public void process(String chainName, Map args) {
		this.process(chainName, args, null);
	}

	/**
	 * Shuts down the data-a-ware instance.
	 * 
	 * @param wait
	 *          if true, this method will block until all queued requests have
	 *          been processed.
	 */
	public void shutdown(boolean wait) {
		if (this.dataAware == null || this.requestManager == null) {
			throw new IllegalStateException("Bootstrap have not been initialized.");
		}
		this.dataAware.shutdown(wait);
	}

	/**
	 * Sends the specified list of requests to Data-A-Ware for processing.
	 * 
	 * @param requests
	 *          Collection<br.com.auster.dware.graph.Request> to be processed.
	 */
	public void enqueueRequests(Collection<Request> requests) {
		if (this.dataAware == null || this.requestManager == null) {
			throw new IllegalStateException(i18n.getString("init.error"));
		}

		requests = handleSortingOption(requests);
		saveRequests(requests);

		boolean isIndexOnlySet = findOption(OptionsParser.getOptions(), INDEX_ONLY_PARAM);
		if (isIndexOnlySet) {
			log.warn(i18n.getString("index.only", requests.size()));
			return;
		}

		log.warn(i18n.getString("sendingRequests", requests.size()));
		try {
			this.dataAware.enqueue(requests);
		} catch (Throwable t) {
			log.fatal("Could not enqueue requests for processing.", t);
			RequestErrorHandler.handleErrors(requests, t);
		}
	}

	protected void saveRequests(Collection<Request> requests) {
		String fileName = OptionsParser.getOptionValue(SAVE_REQUEST_PARAM);
		if (fileName != null && !"".equals(fileName)) {
			XStream xs = new XStream();
			xs.addImplicitCollection(Request.class, "entries");
			xs.useAttributeFor(String.class);
			FileOutputStream saveStream;
			try {
				saveStream = new FileOutputStream(fileName);
				GZIPOutputStream gos = new GZIPOutputStream(saveStream);
				xs.toXML(requests.toArray(), gos);
				gos.flush();
				gos.close();
				saveStream.flush();
				saveStream.close();
			} catch (FileNotFoundException e) {
				log.fatal(e);
				throw new RuntimeException(i18n.getString("file.save.error", fileName), e);
			} catch (IOException e) {
				log.fatal(e);
				throw new RuntimeException(i18n.getString("file.save.error", fileName), e);
			}
		}
	}

	protected List<Request> handleSortingOption(Collection<Request> requests) {
		List<Request> toPassAhead = new ArrayList<Request>();
		String lval = OptionsParser.getOptionValue(UPBOUND_OPT_PARAM);
		if (lval != null) {
			String order = OptionsParser.getOptionValue(UPBOUNDSORTED_OPT_PARAM);
			// we are assuming an ascending order to to upper bounded requests
			int ordered = 1;
			if (order != null) {
				if (order.equalsIgnoreCase("DSC")) {
					ordered = -1;
				} else if (order.equalsIgnoreCase("NONE")) {
					ordered = 0;
				}
			}
			log.info("Ordering is:" + ordered);
			long limit = Long.parseLong(lval);
			log.info("Limiting requests on queue to " + limit);

			List<Request> toBeLast = new ArrayList<Request>();

			// Decides if goes immediattly to the list or to the last positions
			for (Iterator<Request> itr = requests.iterator(); itr.hasNext();) {
				Request req = (Request) itr.next();
				if (req.getWeight() > limit) {
					toBeLast.add(req);
				} else {
					toPassAhead.add(req);
				}
			}
			// Should we order the list with Weighests requests
			if (ordered != 0) {
				boolean ascending = (ordered > 0) ? true : false;
				RequestWeightComparator comparator = new RequestWeightComparator(ascending);
				Collections.sort(toBeLast, comparator);
			}

			toPassAhead.addAll(toBeLast);
		} else {
			log.info("No up bound limit. Passing the requests as it comes to enqueue");
			toPassAhead.addAll(requests);
		}
		return toPassAhead;
	}

	private boolean findOption(String[] options, String optionToFind) {
		if (null == optionToFind || "".equals(optionToFind)) {
			return false;
		}

		for (int i = 0; i < options.length; i++) {
			if (optionToFind.equalsIgnoreCase(options[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Helper method that creates the DOM tree root from the given configuration
	 * filename.
	 * 
	 * @param configFilename
	 *          the configuration path and filename.
	 * @param decryptFileFlag
	 *          <code>'true'</code> if configuration file must be decrypted,
	 *          <code>'false'</code> otherwise.
	 */
	public static final Element getXMLConfig(String configFilename,
			String isEncryptedFileFlag) throws Exception {
		boolean isEncrypted = Boolean.valueOf(isEncryptedFileFlag).booleanValue();
		return DOMUtils.openDocument(configFilename, isEncrypted);
	}

	/**
	 * Parses the given String into a Map, using the hash delimiter defined for
	 * this class ({@link #ARG_HASH_START} and {@link #ARG_HASH_END}).
	 * 
	 * <p>
	 * Additionally, this method will tranform values delimited by
	 * {@link #ARG_LIST_START} and {@link #ARG_LIST_END} to a
	 * <code>java.util.List&gt;String&lt;</code>.
	 * 
	 * <p>
	 * Hashes must have a list of 'name=value' pairs, delimited by ';'. Anything
	 * else will be treated as a single text.
	 * 
	 * <p>
	 * IMPORTANT: if you want to use '=' or ';' as part of one single text (and
	 * not as delimiters), don't do it! This method doesn't support escaping and
	 * will think they are delimiters anyway!!
	 * 
	 * @param args
	 *          the string containing the arguments to be parsed.
	 * @return a (possibly deeply nested) Map with all arguments found.
	 */
	public static final Map parseMapArgs(String args) {
		Map result = new HashMap();
		args = argHashPattern.matcher(args).replaceAll("$1");
		String[] entries = splitArg(args, ";");
		for (int i = 0; i < entries.length; i++) {
			String[] entry = splitArg(entries[i], "=");
			if (entry.length != 2) {
				throw new IllegalArgumentException(
						"Map arguments must always be in the form 'name=value'.");
			}
			String name = entry[0].trim();
			String value = entry[1];
			if (argListPattern.matcher(value).matches()) {
				value = value.replaceAll("\\Q" + ARG_LIST_START + "\\E", "");
				value = value.replaceAll("\\Q" + ARG_LIST_END + "\\E", "");
				result.put(name, Arrays.asList(value.split(",")));
			} else if (argHashPattern.matcher(value).matches()) {
				result.put(name, parseMapArgs(value));
			} else {
				result.put(name, value);
			}
		}
		return result;
	}

	/**
	 * Splits the arguments from the string in <code>arg</code> using the
	 * specified delimiter.
	 * 
	 * <p>
	 * This method correctly handles the hash delimiters, so that delimiters found
	 * inside hashes will be ignored.
	 * 
	 * @param arg
	 *          the argument String to be parsed.
	 * @param delim
	 *          the delimiter used to split the specified argument.
	 * @return an array of all splitted arguments.
	 */
	protected static final String[] splitArg(String arg, String delim) {
		List result = new ArrayList();
		int listCounter = 0;
		String buffer = "";
		for (int i = 0; i < arg.length(); i++) {
			buffer += arg.charAt(i);
			if (buffer.endsWith(ARG_HASH_START)) {
				listCounter++;
			} else if (listCounter > 0) {
				if (buffer.endsWith(ARG_HASH_END)) {
					listCounter--;
				}
			} else if (buffer.endsWith(delim)) {
				result.add(buffer.substring(0, buffer.length() - delim.length()));
				buffer = "";
			}
		}
		result.add(buffer);
		return (String[]) result.toArray(new String[0]);
	}

	/**
	 * Starts the execution of Data-A-Ware, given a list of command-line arguments
	 * and should only be used for a single run in stateless mode (kind of a batch
	 * mode).
	 * 
	 * <p>
	 * Each call of this method will parse the arguments, initialize the Bootstrap
	 * instance, process the requests and shutdown Data-A-Ware.
	 * 
	 * <p>
	 * The following arguments are supported:
	 * 
	 * <pre>
	 *     -x,--xml-conf       the XML configuration filename
	 *     -s,--secret         secret password ;)
	 *     -b,--builder-chain  the name of the request builder chain to execute
	 *     -a,--args           the request builder chain arguments (see details bellow)
	 *     -d,--desired-id     the list of request user-id to process
	 * </pre>
	 * 
	 * <p>
	 * The builder chain arguments is a hash (list of 'name=value' pairs) containg
	 * all init parameters of each builder in the chain. The supported format is:
	 * 
	 * <pre>
	 *    {{builder-name={{name=value;name=value;...}};builder-name={{name=value;...}}}}
	 * </pre>
	 * 
	 * <p>
	 * See the Builder documentation for supported parameters.
	 * 
	 * @param args
	 *          the command-line arguments necessary to execute.
	 * @throws RuntimeException
	 *           if any error occurs during the request's processing.
	 */
	protected void execute(String[] args) throws Exception {
		Element configRoot = configure(args);
		init(configRoot);

		// Start server mode
		String providerURL = OptionsParser.getOptionValue(PROVIDER_PARAM);
		if (providerURL != null && providerURL.length() > 0) {
			initServer(providerURL);
			// Since we are running in server-mode, the chain builder will be started
			// as requests are remotely sent.
			// Also, no shutdown is needed.
			return;
		}

		// Configure the specified RequestBuilderChain
		String chain = OptionsParser.getOptionValue(CHAIN_NAME_PARAM);
		String chainValue = OptionsParser.getOptionValue(CHAIN_ARGS_PARAM);
		if (!argHashPattern.matcher(chainValue).matches()) {
			throw new IllegalArgumentException("Chain arguments must be in the form '"
					+ ARG_HASH_START + "name=value;name=value;..." + ARG_HASH_END + "'.");
		}
		Map managerArgs = parseMapArgs(chainValue);

		try {
			String desiredList = OptionsParser.getOptionValue(DESIRED_IDS_PARAM);
			if (desiredList != null) {
				List idList = Arrays.asList(desiredList.split(","));
				process(chain, managerArgs, idList);
			} else {
				process(chain, managerArgs);
			}
		} catch (Throwable e) {
			String msg = "Unexpected error in Data-A-Ware - Processing Cancelled!!";
			log.fatal(msg, e);
			throw new RuntimeException(msg, e);
		} finally {
			// Telling data-a-ware that we are done.
			shutdown(true);
		}
		unexportObject(this, true);
	}

	protected static void fillOptions(String[] args) {
		// Getting the command line arguments
		options.add(new CLOption(CONF_PARAM, 'x', true, true, "file",
				"the XML configuration file"));
		options.add(new CLOption(NO_ENCRYPT_PARAM, 's', false, true, "secret",
				"secret password"));
		options.add(new CLOption(CHAIN_NAME_PARAM, 'b', true, true, "chain",
				"request builder chain name"));
		options.add(new CLOption(CHAIN_ARGS_PARAM, 'a', true, true, "args",
				"request builder chain arguments"));
		options.add(new CLOption(DESIRED_IDS_PARAM, 'd', false, true, "ids",
				"list of request user-id to process."));
		options.add(new CLOption(PROVIDER_PARAM, 'p', false, true, "URL",
				"activate server mode with the supplied provider URL."));
		options.add(new CLOption(ETM_PARAM, 'e', false, true, "etm",
				"external table manager arguments."));
		options.add(new CLOption(SAVE_REQUEST_PARAM, 'v', false, true, "file name",
				"File name used to save the requests."));
		options.add(new CLOption(RESTORE_REQUEST_PARAM, 'r', false, true, "file name",
				"File name used to restore requests from."));
		options.add(new CLOption(INDEX_ONLY_PARAM, 'i', false, false, "",
				"If only request builder runs to generate indexes."));
		options.add(new CLOption(UPBOUND_OPT_PARAM, 'u', false, true, "up",
				"upper bound request size"));
		options.add(new CLOption(UPBOUNDSORTED_OPT_PARAM, 'o', false, true, "sortorder",
				"the order for sorting upper bounded requests (ASC,DSC,NONE). Default is ASC."));
	}

	protected void addOption(CLOption option) {
		options.add(option);
	}

	protected void addOption(String lName, char sName, boolean req, boolean argreq,
			String argName, String argDesc) {
		options.add(new CLOption(lName, sName, req, argreq, argName, argDesc));
	}

	protected static OptionsParser getOptionParser() {
		return new OptionsParser((CLOption[]) options.toArray(new CLOption[0]),
				Bootstrap.class, null, true);
	}

	protected static Element configure(String[] args) throws Exception {
		fillOptions(args);
		OptionsParser parser = getOptionParser();
		parser.parse(args);
		return finishConfig();
	}

	protected static Element finishConfig() throws Exception {
		// Get the configuration DOM tree root
		String encrypt = OptionsParser.getOptionValue(NO_ENCRYPT_PARAM);
		String isEncryptedConfigFlag = "true";
		if (encrypt != null && encrypt.equalsIgnoreCase("false")) {
			isEncryptedConfigFlag = "false";
		}
		Element configRoot = getXMLConfig(OptionsParser.getOptionValue(CONF_PARAM),
				isEncryptedConfigFlag);

		// Configure the Log4J library
		LogFactory.configureLogSystem(configRoot);

		return configRoot;
	}

	public static Element getRootConfig(String filePath, String isEncrypted)
			throws Exception {
		return getXMLConfig(filePath, isEncrypted);
	}

	public static Element getDWareConfig(String filePath, String isEncrypted)
			throws Exception {
		return DOMUtils.getElement(getXMLConfig(filePath, isEncrypted),
				DataAware.DWARE_NAMESPACE_URI, DataAware.CONFIGURATION_ELEMENT, true);
	}

	public static Element getMonitorConfig(String filePath, String isEncrypted)
			throws Exception {
		return DOMUtils.getElement(getXMLConfig(filePath, isEncrypted),
				AusterManagementServices.MANAGEMENT_NAMESPACE_URI,
				AusterManagementServices.MANAGEMENT_CONFIGURATION_ELEMENT, false);
	}

	public static Element getETMConfig(String filePath, String isEncrypted)
			throws Exception {
		return DOMUtils.getElement(getXMLConfig(filePath, isEncrypted), ETM_NAMESPACE_URI,
				ETM_CONFIGURATION_ELEMENT, false);
	}

	protected void configureBootstrapListener(Element _configuration) {

		try {
			String klass = DOMUtils.getAttribute(_configuration, CLASS_NAME_ATTR, true);
			if (this.bootstrapListenerList == null) { 
				this.bootstrapListenerList = new ArrayList<BootstrapListener>();
			}
			BootstrapListener bootListener = (BootstrapListener) Class.forName(klass).newInstance();
			this.bootstrapListenerList.add(bootListener);
			bootListener.configure(_configuration);
		} catch (Exception e) {
			log.error("could not configure bootstrap listeners", e);
			this.bootstrapListenerList = null;
		}
	}

	/**
	 * This method is called in order to check if the resource manage by this run
	 * of data-aware is for the same product as this main class
	 * 
	 * @return Returns 0 if the productID is the same or -1 otherwise
	 */
	protected abstract int check();

}
