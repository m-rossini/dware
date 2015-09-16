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
package br.com.auster.dware.res;

import java.util.ListResourceBundle;

/**
 * @version $Id: MessagesBundle_pt_BR.java 336 2007-12-26 13:14:17Z mtengelm $
 */
public class MessagesBundle_pt_BR extends ListResourceBundle {
	public Object[][] getContents() {
		return contents;
	}

	static final Object[][]	contents	= {
			// Bootstrap.java
			{ "file.save.error", "Problemas ao salvar requisi��es no arquivo [ {0} ]." },
			{ "file.restore.error", "Problemas ao restaurar requisi��es no arquivo [ {0} ]." },
			{ "index.only", "A Op��o de indexa��o foi utilizada. Obtidas {0} requisi��es." },
			{ "init.error", "Bootstrap n�o foi inicializado." },
			{ "sendingRequests", "Enviando {0} requisi��es para processamento." },
			{ "incompatible.options", "Op��o {0} e op��o {1} s�o incompat�veis." },
			// DataAware.java
			{ "listenerReady", "StartupListener [ {0} ] configurado e pronto!" },
			{ "managerNotConfigured", "O gerente de processamento n�o foi configurado!" },
			{ "resourceMissing", "Um recurso cujo nome � {0} est� faltando." } };
}
