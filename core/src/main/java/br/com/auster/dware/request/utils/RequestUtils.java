/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 28/11/2007
 */
package br.com.auster.dware.request.utils;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
import br.com.auster.common.io.FileSet;
import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.file.PartialFileRequest;
import br.com.auster.dware.request.index.IndexMatcherDef;

/**
 * Classes utilitárias para tratamento de requisições
 * 
 * @author mtengelm
 * @version $Id$
 * @since JDK1.4
 */
public class RequestUtils {

	private static final I18n		i18n								= I18n.getInstance(RequestUtils.class);
	private static final Logger	log									= Logger.getLogger(RequestUtils.class);

	/**
	 * "{@value}": builder argument for a String representing the input filename
	 * list.
	 */
	public static final String	FILENAMES_ARG				= "filenames";
	/**
	 * "{@value}": builder argument for a File[] representing the input file
	 * list.
	 */
	public static final String	FILES_ARG						= "files";

	/**
	 * "{@value}": builder argument for a String representing the transaction ID
	 * of all requests.
	 */
	public static final String	TRANSACTION_ID_ARG	= "transaction-id";

	/**
	 * "{@value}": builder argument for a Map representing all attributes that
	 * must be stored in each request.
	 */
	public static final String	REQUEST_PARAMS_ARG	= "request-params";

	/**
	 * Parses all arguments. This is kind of the command-line arguments of the
	 * buider, except that is not necessarily coming from the command-line!
	 * 
	 * <p>
	 * This implementation accepts the following args:
	 * <ul>
	 * <li>{@link #FILENAMES_ARG}: (String) list of filenames to be parsed
	 * <li>{@link #FILES_ARG}: (File[]) files to be parsed
	 * <li>{@link br.com.auster.dware.request.BaseRequestBuilder#TRANSACTION_ID_ARG}:
	 * (String) will be stored in each request
	 * <li>{@link br.com.auster.dware.request.BaseRequestBuilder#REQUEST_PARAMS_ARG}:
	 * (Map<String, Object>) parameters that will be stored in each request
	 * </ul>
	 * 
	 * <p>
	 * At least one of {@link #FILENAMES_ARG} or {@link #FILES_ARG} must be
	 * provided, but you can specify both if you want.
	 * 
	 * <p>
	 * List arguments must be delimited by "{@link br.com.auster.dware.request.RequestBuilder#ARG_LIST_DELIMITER}".
	 * 
	 * @param args
	 *          Map<String, Object> with all arguments for this File Builder.
	 * @return an instance of {@link FileBuilderArgs} with all parsed arguments.
	 */
	public static FileBuilderArgs parseFileArgs(Map args) {
		if (args == null) {
			args = new HashMap();
		}

		final List files = new ArrayList();
		Object value;

		final String[] fileNames;
		value = args.get(FILENAMES_ARG);
		if (value != null) {
			checkArgType(FILENAMES_ARG, value, String.class);
			fileNames = ((String) value).split(RequestBuilder.ARG_LIST_DELIMITER);
			for (int i = 0; i < fileNames.length; i++) {
				files.addAll(Arrays.asList(FileSet.getFiles(fileNames[i])));
			}
		}

		value = args.get(FILES_ARG);
		if (value != null) {
			checkArgType(FILES_ARG, value, File[].class);
			files.addAll(Arrays.asList((File[]) value));
		}

		if (files.size() == 0) {
			throw new IllegalArgumentException("No files found to process!");
		}

		BuilderArgs baseArgs = parseArgs(args);
		return new FileBuilderArgs(files, baseArgs.getTransactionId(), baseArgs
				.getRequestParams());
	}

	/**
	 * Parses all arguments. This is kind of the command-line arguments of the
	 * builder, except that is not necessarily coming from the command-line!
	 * 
	 * <p>
	 * This implementation accepts the following args:
	 * <ul>
	 * <li>{@link #TRANSACTION_ID_ARG}: (String) will be stored in each request
	 * <li>{@link #REQUEST_PARAMS_ARG}: (Map<String, Object>) parameters that
	 * will be stored in each request
	 * </ul>
	 * 
	 * <p>
	 * List arguments must be delimited by "{@link #ARG_LIST_DELIMITER}".
	 * 
	 * @param args
	 *          Map<String, Object> with all arguments for this Request Builder.
	 * @return an instance of {@link BuilderArgs} with all parsed arguments.
	 */
	public static BuilderArgs parseArgs(Map args) {
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
	public static void checkArgType(String name, Object value, Class expectedType) {
		if (!expectedType.isInstance(value)) {
			throw new IllegalArgumentException(i18n.getString("invalidArgument", name, value
					.getClass().getName(), expectedType.getName()));
		}
	}

	public static String assemblePieces(List<IndexMatcherDef> pieces, String record) {
		StringBuilder sb = new StringBuilder();
		for (IndexMatcherDef pieceDef : pieces) {
			sb.append(IndexMatcherDef.handleSequence(record, pieceDef));
			int k = 0;
		}

		return sb.toString();
	}

	/*****************************************************************************
	 * 
	 * Dado um CharBuffer que supostamente tem uma entrada de indice de requisição, retorna
	 * um objeto Request.
	 * 
	 * This method can return NULL. If so, it means the request could not be
	 * created.
	 * 
	 * @param cc
	 * @return
	 * @throws IOException 
	 */
	public static Request createPartialFileRequestFromCharBuffer(CharBuffer cc) throws IOException {
		if (!isAcceptableVersion(cc.get(0), cc.get(1), cc.get(2))) {
			return null;
		}
		cc.mark();

		try {
			// This is format 001
			String[] pieces = cc.toString().split(";");

			long off = -1L;
			long wei = -1L;
			try {
				off = Long.parseLong(pieces[3]);
				wei = Long.parseLong(pieces[4]);
			} catch (java.lang.NumberFormatException e) {
				log.error(i18n.getString("wrong.request.position", pieces[3], pieces[4],
						pieces[1]), e);
				return null;
			}

			File file = null;
			String fileName = null;
			try {
				file = new File(pieces[5]);
				fileName = file.getCanonicalPath();
				if (!file.canRead()) {
					throw new IOException(i18n.getString("file.cannot.read", fileName));
				}
			} catch (IOException e) {
				log.error(
						i18n.getString("wrong.request.file", file.getAbsolutePath(), pieces[1]), e);
				throw e;
			}

			PartialFileRequest pfr = new PartialFileRequest(pieces[1], off, wei, file);
			if (!"".equals(pieces[2])) {
				pfr.setTransactionId(pieces[2]);
			}

			// Handle Request Attributes.
			for (int i = 6; i < pieces.length; i += 2) {
				pfr.getAttributes().put(pieces[i], pieces[i + 1]);
			}
			return pfr;
		} finally {
			cc.reset();
		}
	}

	/**
	 * Indica se uma versão de indice é aceitavel.
	 * Atualmente somente a versão 001 é suportada.
	 * 
	 * @param b Primeiro byte do formato
	 * @param c Segundo byte do formato
	 * @param d Terceiro byte do formato.
	 * 
	 * @return true se for suportado, e false se não for suportada.
	 */
	protected static boolean isAcceptableVersion(char b, char c, char d) {
		// INDEX FORMAT Version 001
		if ((b == '0') && (c == '0') && (d == '1')) {
			return true;
		}
		return false;
	}

	/***
	 * Formata os dados básicos da requisição.
	 * Entende-se por dados básicos, as informações que não são parte dos atributos da requisição.
	 * O Formato a ser gravado depende do VersionID informado. 
	 * 
	 * @param fileRequest Requisição.
	 * @param cc Charbuffer com resultado.
	 * @param VERSIONID Versão a ser utilizada.
	 */
	public static void formatHeaderRequest(PartialFileRequest fileRequest, CharBuffer cc,
			String VERSIONID) {
		cc.clear();
		//
		//
		//
		// PLEASE NOTE:
		// IF YOU CHANGE THE FORMAT OF INDEX OUTPUT FILE, IT IS MANDATORY TO CHANGE
		// VERSIONID
		//
		//
		//
		cc.append(VERSIONID);

		cc.append(';');
		cc.append(fileRequest.getUserKey());

		cc.append(';');
		if (fileRequest.getTransactionId() != null) {
			cc.append(fileRequest.getTransactionId());
		}

		cc.append(';');
		cc.append(Long.toString(fileRequest.getOffset()));

		cc.append(';');
		cc.append(Long.toString(fileRequest.getLength()));

		cc.append(';');
		try {
			cc.append(fileRequest.getFile().getCanonicalPath());
		} catch (IOException e) {
			cc.append(fileRequest.getFile().getAbsolutePath());
			log.trace("Error getting file data", e);
		}
	}

	/***
	 * Cria no charbuffer informado os atributos (request.getAttributes()) da requisição.
	 * 
	 * @param fileRequest Requisição
	 * @param cc Charbuffer a ser preenchido com os atributos da requisição;
	 */
	public static void formatAttributes(PartialFileRequest fileRequest, CharBuffer cc) {
		for (Iterator<Map.Entry> it = fileRequest.getAttributes().entrySet().iterator(); it
				.hasNext();) {
			Entry entry = it.next();

			if ((entry.getKey() instanceof String)) {
				String key = (String) entry.getKey();
				if (!key.startsWith("dware.")) {
					cc.append(';');
					cc.append(key);
					cc.append(';');
					cc.append(entry.getValue().toString());
				}
			}
		}
	}

	/***
	 * 
	 * Formato o trailer do request para o charbuffer.
	 * Este metodo cria apenas um newline (Formato UNIX)
	 *  
	 * @param fileRequest Não utilizado
	 * @param cc Charbuffer que recebrá o newLine.
	 */
	public static void formatTailRequest(PartialFileRequest fileRequest, CharBuffer cc) {
		cc.append('\n');
	}

	/***
	 * Formato no Charbuffer informado, a requisição informada,
	 * na versão infromada. 
	 * 
	 * Este metodo não faz flip() ou nenhuma operação de buffer antes do retorno.
	 * Durante a formatação o position() do buffer será alterado conforme o mesmo
	 * é preenchido.
	 * 
	 * @param request Request a ser formatado
	 * @param cc Charbuffer com resultado.
	 * @param versionid Versão a ser utilizada.
	 */
	public static void formatFullRecord(PartialFileRequest request, CharBuffer cc,
			String versionid) {
		RequestUtils.formatHeaderRequest(request, cc, versionid);
		RequestUtils.formatAttributes(request, cc);
		RequestUtils.formatTailRequest(request, cc);
	}

}
