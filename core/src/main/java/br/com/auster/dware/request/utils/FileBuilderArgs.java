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

import java.util.List;
import java.util.Map;

/**
 * Esta classe estende {@link #BuilderArgs}, e serve para construir argumentos que sejam arquivos.
 * Primariamente desenhada para suportar os argumentos de @see RequestBuilder.
 *
 * @author mtengelm
 * @version $Id$
 * @since JDK1.4
 */
public class FileBuilderArgs extends BuilderArgs {

	private List	files;

	public FileBuilderArgs(List files, String transactionId,
			Map requestParams) {
		super(transactionId, requestParams);
		this.files = files;
	}

	public List getFiles() {
		return this.files;
	}

}
