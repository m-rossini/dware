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
package br.com.auster.dware.manager.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 *
 * @version $Id: MessagesBundle_pt_BR.java 334 2007-12-04 16:27:08Z framos $
 */
public class MessagesBundle_pt_BR extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // GraphGroup.java
        {"graphGroupReady", "Grupo de Processamento aguardando requisi��es!"},
        {"processingLastRequests", "Processando �ltimas requisi��es antes de finalizar..."},
        {"graphGroupDown", "Grupo de Processamento desativado."},
        {"graphGroupGoingDown", "O Grupo de Processamento \"{0}\" est� sendo desativado."},
        {"graphMedException", "O mediator n�o pode ser null."},
        {"graphFailed", "Erro fatal no GraphGroup. O grafo n�o o conteve a exce��o!"},

        // LocalGraphGroup.java
        {"maxGraphsNumber", "Ajustando o n�mero m�ximo de grafos de processamento para o grupo \"{0}\" para {1}."},
        {"settingGraphs", "Configurando os grafos do Grupo de Processamento \"{0}\"."},
        {"settingFilters", "Configurando os filtros nomeados \"{0}\" para o Grupo de Processamento \"{1}\"."},
        {"graphDoesNotExist", "O grafo \"{0}\" n�o existe na lista de grafos do Grupo de Processamento \"{1}\"!"},
        {"listenerConfigured", "O Listener \"{1}\" foi configurado para o grupo \"{0}\"."},
        {"listenerConfigError", "Erro ao configurar o Listener \"{1}\" para o grupo \"{0}\"!"},
        {"listenerFinishError", "Erro ao alertar sobre o t�rmino de uma requisi��o para o Listener do grupo \"{0}\"!"},

        // GraphManager.java
        {"configuringManager", "Configurando o Gerente de Processamento..."},
        {"managerConfigured", "Gerente de Processamento configurado!"},
        {"creatingLocalGraphGroup", "Criando o Grupo de Processamento Local \"{0}\"."},
        {"groupAlreadyExists", "O grupo \"{0}\" j� existe no Gerente de Processamento."},
        {"groupDoesNotExist", "O grupo \"{0}\" n�o existe no Gerente de Processamento."},
        {"configuringGroup", "Configurando o Grupo de Processamento \"{0}\"."},
        {"cantEnqueueManagerDown", "N�o � poss�vel adicionar a requisi��o {0} para processamento. O Gerente de Processamento est� desativado."},
        {"managerGoingDown", "Desativando o Gerente de Processamento!"},
        {"gotInterruption", "Interrup��o recebida."},
        {"managerDown", "Gerente de Processamento desativado."},
        {"queueListenersNotConfigured", "Erro ao configurar os listeners de queue-empty - ignorando todos os listeners."},
        {"queueListenerConfigured", "QueueEmptyListener \"{0}\" adicionado e configurado."},
        {"checkpointNotConfigured", "O checkpoint n�o foi configurado."},
       	{"checkpointCreationError", "O checkpoint n�o foi com sucesso."},

        // ReqForwarder
        {"reqAlreadyLoaded", "A requisi��o \"{0}\" j� foi carregada."},

        // remote
        {"creatingRemoteGraphGroup", "Criando graph group remoto."},
        {"remoteConfigErr", "Erro durante a configura��o de um graph group remoto."},
        {"remoteProcessErr", "Erro durante o processamento de uma requisi��o remota."},
        {"remoteShutdownErr", "Erro durante o encerramento de um graph group remoto."},
        {"remoteFilterCfgErr", "Erro durante a reconfigura��o de um filtro de grafo."},
        {"remoteReplyCommitErr", "Erro durante a confirma��o de um commit de uma requisi��o."},
        {"remoteManagerServiceErr", "Erro ao configurar o JNDI no GraphManager. Os graph groups remotos n�o ser�o capazes de se conectar."},        {"remoteXMLConfigErr", "Erro ao configurar um graph group remoto. O formato XML pode estar incorreto."},
        {"remoteReqUnexpected", "Uma messagem de alguma requisi��o que terminou e n�o foi processada por este graph group foi recebida."},
        {"remoteConnErr", "O graph group remoto n�o p�de se conectar pois o graph manager est� finalizando."},

        {"remoteManagerConfigErr", "O Graph manager n�o foi configurado."},
        {"remoteManagerRegisterErr", "Erro ao registrar um graph group remoto."},
        {"remoteTerminateErr", "Erro ao encerrar um graph group remoto."},
        {"remoteFinalizeErr", "Erro ao finalizar um graph group remoto."},
        {"remoteKillErr", "Erro ao terminar um graph group remoto."},
        {"remoteSrvDownErr", "Terminando graph group remoto devido pois o servidor n�o � mais encontrado."}
    };
}
