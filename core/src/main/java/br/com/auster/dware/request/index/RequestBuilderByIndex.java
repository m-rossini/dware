package br.com.auster.dware.request.index;


import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import br.com.auster.common.io.FileSet;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.filter.FilenameBuilder;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.HashRequestFilter;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

/**
 * Esta classe irá de acordo com sua configuração buscar nos indices de requisições as requisições desejadas.
 * As requisições desejadas devem ser criadas por outro builder da builder chain. Portanto este builder
 * deverá ser utlizado somente em builder chains com mais de um filtro, e não poderá nunca se o primeiro. 
 * Caso isso não ocorra (Ou seja, este é o primeiro builder ou o único), nenhuma requisição será gerada.
 * 
 * Os indices a serem pesquisados são definidos em configuração, veja em {@link #RequestBuilderByIndex(String, Element)}
 * 
 * @author mtengelm
 * @version $Id$
 * @since 20/12/2007
 */
public class RequestBuilderByIndex implements RequestBuilder {

	private static final Logger	log										= Logger
																												.getLogger(RequestBuilderByIndex.class);
	private static final I18n		i18n									= I18n
																												.getInstance(OptimizeIndexFile.class);

	/*** Nome deste builder ***/
	private final String				name;

	/*** Nome do atributo de buffer size ***/
	public static final String	BUFFER_SIZE_ATTR			= "bufferSize";
	
	/**** Nome do atributo de encoding ***/
	public static final String	ENCODING_ATTR					= "encoding";
	
	/***Nome do atributo para ignorar erros de IO de indices ***/
	public static final String	IGNORE_IO_ERRORS_ATTR	= "ignore-io-errors";
	
	/*** Nome do atributo da quantidade de threads ****/
	public static final String	THREADS_ATTR					= "threads";
	
	/***Nome do elemento da configuração que define os arquivos de indices a serem pesquisados ***/
	public static final String	FILENAME_ELEMENT			= "filename";
	
	/***Nome do elemnto da configuração que define os indices***/
	public static final String	INDEX_ELEMENT					= "index";
	
	/**
	 * "{@value}": builder argument for a String representing the input filename
	 * list.
	 */
	public static final String	FILENAMES_ARG					= "filenames";

	/*****************************************************************************
	 * Default threads. If not informed or mis-informed.
	 * 
	 */
	/*** Quantidade default de threads, caso não configurado ou configurado erroneamente. Valor = Qtde de Processadores * 2 ***/
	public final int						defaultThreads				= Runtime.getRuntime()
																												.availableProcessors() * 2;

	/*** Tamanho do buffer de leitura em caso de erro de configuração ou não configuração. Valor = (Memoria da VM/2) / Numero de Threads ***/
	public final int						defaultBufferSize			= ((int) (Runtime.getRuntime()
																												.maxMemory() / 2) / defaultThreads);
	/*****************************************************************************
	 * Parameters
	 */
	private int									bufferSize;
	private int									threads;
	private final boolean				ignoreIOErrors;
	private Charset							charset;
	private FilenameBuilder			indexFileNamePattern;

	/*****************************************************************************
	 * Cria uma nova instância desta classe.
	 * 
	 * <code>RequestBuilderByIndex</code>.
	 * 
	 * As configurações suportadas são:
	 * Atributo {@link #BUFFER_SIZE_ATTR} Que é o tamanho do buffer de leitura de cada arquivo de indice.
	 * Atributo {@link #THREADS_ATTR} Que á a quantidade de threads simultaneas (Um arquivo de indice por thread)
	 * Atributo {@link #ignoreIOErrors} Se deverão ser ignorados erros de IO na leitura dos indices.
	 * Atributo {@link #ENCODING_ATTR} Qual o encoding utlizado para decodificar os buffers de leitura de indices.
	 * 
	 * Dentro deste elemento um outro elemento poderá ser definido:
	 * Elemento {@link #INDEX_ELEMENT} que contém as configurações dos indices que são:
	 * 	Elemento {@link #FILENAME_ELEMENT} que define o nome dos arquivos de indices que serão procurados
	 * 					Esses nomes devem ser configurados conforme a classe {@linkplain FilenameBuilder}
	 * 
	 * 
	 * @param name Nome pelo qual o Builder será conhecido.
	 * @param config Elemento XML com as configurações deste builder.
	 */
	public RequestBuilderByIndex(final String name, final Element config) {
		this.name = name;

		// Configure Buffers
		String bs = DOMUtils.getAttribute(config, BUFFER_SIZE_ATTR, false);
		if (bs == null || "".equals(bs)) {
			bufferSize = defaultBufferSize;
		} else {
			try {
				bufferSize = Integer.parseInt(bs);
			} catch (NumberFormatException e) {
				bufferSize = defaultBufferSize;
				log.error(i18n.getString("error.buffersize", new Integer(bufferSize)));
			}
		}

		// Configure Threads
		String th = DOMUtils.getAttribute(config, THREADS_ATTR, false);
		if (th == null || "".equals(th)) {
			threads = defaultThreads;
		} else {
			try {
				threads = Integer.parseInt(th);
			} catch (NumberFormatException e) {
				threads = defaultThreads;
				log.error(i18n.getString("error.threadssize", new Integer(threads)));
			}
		}

		// Configure IOErrors
		ignoreIOErrors = DOMUtils.getBooleanAttribute(config, IGNORE_IO_ERRORS_ATTR, false);

		// Configure Charset Encode/Decode
		String enc = DOMUtils.getAttribute(config, ENCODING_ATTR, false);
		this.charset = Charset.forName(enc);

		// Configure Index File Name Pattern
		Element indexElmt = DOMUtils.getElement(config, INDEX_ELEMENT, false);
		if (indexElmt != null) {
			Element filenameElmt = DOMUtils.getElement(indexElmt, FILENAME_ELEMENT, false);
			if (null == filenameElmt) {
				return;
			}

			indexFileNamePattern = new FilenameBuilder(filenameElmt);
		}

	}

	/**
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(java.util.Map)
	 */
	public RequestFilter createRequests(Map args) {
		return createRequests(null, args);
	}

	/**
	 * Cria os requests a partir das entradas encontradas nos indices.
	 * 
	 * @param filter RequestFilter onde serão armazenadas as requisições a processar.
	 * @param args mapa de argumentos.
	 * @return RequestFilter com as requisições que serão processadas.
	 * 
	 * @see br.com.auster.dware.request.RequestBuilder#createRequests(br.com.auster.dware.request.RequestFilter,
	 *      java.util.Map)
	 */
	public RequestFilter createRequests(RequestFilter filter, Map args) {
		final RequestFilter requestFilter;
		if (filter == null) {
			requestFilter = new HashRequestFilter();
		} else {
			requestFilter = filter;
		}

		Map<File, List<Request>> toProcessFiles = determineFilesToSearch(filter, args);

		List<Request> acceptedByIndex = null;
		acceptedByIndex = buildRequests(toProcessFiles);

		if (acceptedByIndex != null) {
			for (Request request : acceptedByIndex) {
				requestFilter.accept(request);
			}
		}
		return requestFilter;
	}

	/**
	 * 
	 * Este método, é aquele que efetivamente dispara as threads de busca nos indices.
	 * 
	 * para cada arquivo na lista de arquivos informada como parametro, uma thread será criada e disparada.
	 * 
	 * Somente o número máximo de threads configuradas ver ( {@link #threads} } será disparada simultaneamente e
	 * será controlada pelo pool de threads criado.
	 * 
	 * Uma vez que todas foram criadas e disparadas, o metodo entrará em um loop de espera.
	 * 
	 * Esse espera terá duração de 10 segundos (Melhoria Futura: Configurar o tempo de espera).
	 * 
	 * Entre cada iteração de espera diversas threads podem ter acabado e portanto uma mensagem informado quantas
	 * threads já acabram será mostrada. NOTA: A Quantidade de threads encerradas mostradas nessa mensagem poderá ser
	 * menor que a quantidade efetivamente encerrada, em particular na ultima mensagem. Isso pode ocorrer emfunção
	 * de que na ultima espera todas as threads tenham acabado e não seja mais necessário aguardar.
	 * 
	 * @param toProcessFiles Lista com os arquivos a serem processados (Busca de indices)
	 * @return Lista com requisições encontradas nos indices.
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
	private List<Request> buildRequests(Map<File, List<Request>> toProcessFiles) {
		// TODO DO NOT Forget to Use MatchIndex Structure (To allow partial match
		// for requests

		List<Request> results = new ArrayList<Request>();

		ThreadPoolExecutor myPool = new ThreadPoolExecutor(this.threads, this.threads, 10,
				TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>());

		for (Map.Entry<File, List<Request>> entry : toProcessFiles.entrySet()) {
			File file = entry.getKey();
			List<Request> list = entry.getValue();

			FindRequestsOnList frol = new FindRequestsOnList(results);
			frol.setFile(file);
			frol.setRequestList(list);
			frol.setIgnoreIOErrors(this.ignoreIOErrors);
			frol.setBufferSize(this.bufferSize);
			frol.setCharset(charset);
			myPool.execute( (Runnable) frol);
		}

		myPool.shutdown();
		try {
			int duration = 10;
			while (!myPool.isTerminated()) {
				StringBuilder sb = new StringBuilder();
				sb.append(myPool.getActiveCount());
				sb.append('/');
				sb.append(myPool.getCompletedTaskCount());
				sb.append('/');
				sb.append(myPool.getCorePoolSize());
				sb.append('/');
				sb.append(myPool.getPoolSize());
				sb.append('/');
				sb.append(myPool.getMaximumPoolSize());
				sb.append('/');
				sb.append(myPool.getLargestPoolSize());
				sb.append('/');
				sb.append(results.size());
				log.info(i18n.getString("thread.wait", new Integer(duration), sb.toString()));

				myPool.awaitTermination(duration, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			RuntimeException rte = new RuntimeException(e);
			log.fatal(i18n.getString("thread.interrupted"), e);
			throw rte;
		}

		return results;
	}

	/**
	 * Determina, em função das requisições já obtidas no filtro (Algum outro filtro do Builder Chain já deve ter sido executado)
	 * quais os arquivos de indice que serão pesquisados.
	 * 
	 * Cada requisição deverá derivar um nome de arquivo de indices, através da configuração desta classe.
	 * Essa confgiuração é feita através da classe {@linkplain FilenameBuilder} com dados da propria requisição.
	 * 
	 * É importante portanto que o Builder que criou os requests ({@link RequestFilter}, e no caso,
	 * os requestes retornados pelo método getAllowdRequests(), tenha criado os atributos da reuiqsição configurados
	 * para este builder poder gerar os nomes corretamente. Também importante, a configuração de nomes de arquivos
	 * de indices utilizados na sua criação ({@linkplain MultiThreadFileRequestBuilder} seja a mesma utilizada por
	 * esta classe.
	 * 
	 * @param filter Com as requisições já encontradas
	 * @param args Mapa de argumentos da linhas de comandos.
	 * @return Uma mapa cuja chave é o arquivo a ser pesquisado e o valor uma lista de reuisições a procurar no arquivo.
	 */
	protected Map<File, List<Request>> determineFilesToSearch(RequestFilter filter, Map args) {
		Map<File, List<Request>> toProcessFiles = new HashMap<File, List<Request>>();
		log.info(i18n.getString("index.filename"));
		Map<String, List<Request>> acceptedFileNames = new HashMap<String, List<Request>>();
		for (Iterator<List<Request>> it = filter.getAllowedRequests().values().iterator(); it
				.hasNext();) {
			List<Request> list = it.next();
			for (Iterator<Request> itReq = list.iterator(); itReq.hasNext();) {
				Request request = itReq.next();
				try {
					String filename = new File(indexFileNamePattern.getFilename(request)).getName();
					List<Request> toProcessList = acceptedFileNames.get(filename);
					if (toProcessList == null) {
						toProcessList = new ArrayList<Request>();
					}
					toProcessList.add(request);
					acceptedFileNames.put(filename, toProcessList);
				} catch (FilterException e) {
					log.error(i18n.getString("error.filename", request),e);
					continue;
				}
			}
		}

		String fileMask = (String) args.get(FILENAMES_ARG);
		File[] files = FileSet.getFiles(fileMask);
		log.info(i18n.getString("index.count.total", new Integer(files.length)));
		log.info(i18n.getString("index.count.search", new Integer( acceptedFileNames.size())));
		for (int i = 0; (i < files.length) && (acceptedFileNames.size() != 0); i++) {
			List<Request> requestList = acceptedFileNames.get(files[i].getName());
			if (requestList != null) {
				toProcessFiles.put(files[i], acceptedFileNames.get(files[i].getName()));
			}
			acceptedFileNames.remove(files[i].getName());
		}

		return toProcessFiles;
	}

	/**
	 * @see br.com.auster.dware.request.RequestBuilder#getName()
	 */
	public String getName() {
		return this.name;
	}

}
