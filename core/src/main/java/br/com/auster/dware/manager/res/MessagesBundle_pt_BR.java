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
        {"graphGroupReady", "Grupo de Processamento aguardando requisições!"},
        {"processingLastRequests", "Processando últimas requisições antes de finalizar..."},
        {"graphGroupDown", "Grupo de Processamento desativado."},
        {"graphGroupGoingDown", "O Grupo de Processamento \"{0}\" está sendo desativado."},
        {"graphMedException", "O mediator não pode ser null."},
        {"graphFailed", "Erro fatal no GraphGroup. O grafo não o conteve a exceção!"},

        // LocalGraphGroup.java
        {"maxGraphsNumber", "Ajustando o número máximo de grafos de processamento para o grupo \"{0}\" para {1}."},
        {"settingGraphs", "Configurando os grafos do Grupo de Processamento \"{0}\"."},
        {"settingFilters", "Configurando os filtros nomeados \"{0}\" para o Grupo de Processamento \"{1}\"."},
        {"graphDoesNotExist", "O grafo \"{0}\" não existe na lista de grafos do Grupo de Processamento \"{1}\"!"},
        {"listenerConfigured", "O Listener \"{1}\" foi configurado para o grupo \"{0}\"."},
        {"listenerConfigError", "Erro ao configurar o Listener \"{1}\" para o grupo \"{0}\"!"},
        {"listenerFinishError", "Erro ao alertar sobre o término de uma requisição para o Listener do grupo \"{0}\"!"},

        // GraphManager.java
        {"configuringManager", "Configurando o Gerente de Processamento..."},
        {"managerConfigured", "Gerente de Processamento configurado!"},
        {"creatingLocalGraphGroup", "Criando o Grupo de Processamento Local \"{0}\"."},
        {"groupAlreadyExists", "O grupo \"{0}\" já existe no Gerente de Processamento."},
        {"groupDoesNotExist", "O grupo \"{0}\" não existe no Gerente de Processamento."},
        {"configuringGroup", "Configurando o Grupo de Processamento \"{0}\"."},
        {"cantEnqueueManagerDown", "Não é possível adicionar a requisição {0} para processamento. O Gerente de Processamento está desativado."},
        {"managerGoingDown", "Desativando o Gerente de Processamento!"},
        {"gotInterruption", "Interrupção recebida."},
        {"managerDown", "Gerente de Processamento desativado."},
        {"queueListenersNotConfigured", "Erro ao configurar os listeners de queue-empty - ignorando todos os listeners."},
        {"queueListenerConfigured", "QueueEmptyListener \"{0}\" adicionado e configurado."},
        {"checkpointNotConfigured", "O checkpoint não foi configurado."},
       	{"checkpointCreationError", "O checkpoint não foi com sucesso."},

        // ReqForwarder
        {"reqAlreadyLoaded", "A requisição \"{0}\" já foi carregada."},

        // remote
        {"creatingRemoteGraphGroup", "Criando graph group remoto."},
        {"remoteConfigErr", "Erro durante a configuração de um graph group remoto."},
        {"remoteProcessErr", "Erro durante o processamento de uma requisição remota."},
        {"remoteShutdownErr", "Erro durante o encerramento de um graph group remoto."},
        {"remoteFilterCfgErr", "Erro durante a reconfiguração de um filtro de grafo."},
        {"remoteReplyCommitErr", "Erro durante a confirmação de um commit de uma requisição."},
        {"remoteManagerServiceErr", "Erro ao configurar o JNDI no GraphManager. Os graph groups remotos não serão capazes de se conectar."},        {"remoteXMLConfigErr", "Erro ao configurar um graph group remoto. O formato XML pode estar incorreto."},
        {"remoteReqUnexpected", "Uma messagem de alguma requisição que terminou e não foi processada por este graph group foi recebida."},
        {"remoteConnErr", "O graph group remoto não pôde se conectar pois o graph manager está finalizando."},

        {"remoteManagerConfigErr", "O Graph manager não foi configurado."},
        {"remoteManagerRegisterErr", "Erro ao registrar um graph group remoto."},
        {"remoteTerminateErr", "Erro ao encerrar um graph group remoto."},
        {"remoteFinalizeErr", "Erro ao finalizar um graph group remoto."},
        {"remoteKillErr", "Erro ao terminar um graph group remoto."},
        {"remoteSrvDownErr", "Terminando graph group remoto devido pois o servidor não é mais encontrado."}
    };
}
