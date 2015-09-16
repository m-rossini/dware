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
package br.com.auster.dware.graph.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 * 
 * @version $Id: MessagesBundle_pt_BR.java 306 2007-09-05 00:13:01Z rbarone $
 */
public class MessagesBundle_pt_BR extends ListResourceBundle {
    public Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // Graph.java
        {"filterListEmpty", "A configura��o do grafo deve ter elementos 'filter'."},
        {"addFilter", "Adicionando o filtro {0}..."},
        {"noConnectionFilter", "N�o h� conex�o sainte do filtro {0}."},
        {"filterAlreadyAdded", "Tentativa de adicionar o filtro \"{0}\" que j� havia sido adicionado (filtro com nome repetido na configura��o?)."},
        {"makingConnection", "Criando a aresta [ {0} -> {1} ]..."},
        {"connectionFilterFailed", "Um ou ambos dos filtros \"{0}\" ou \"{1}\" n�o foi adicionado ao grafo. Verifique sua configura��o."},
        {"invalidConnectorClassName", "N�o foi poss�vel instanciar o conector \"{0}\". Conectores padr�es ser�o usados no lugar desse."},
        {"processingRequest", "Iniciando processamento da requisi��o {0} no grafo \"{1}\"."},
        {"waitingRequest", "Aguardando finaliza��o do processamento da requisi��o {0} no grafo \"{1}\"."},
        {"rollingbackRequest", "Desfazendo processamento para requisi��o {0} no grafo \"{1}\"."},
        {"commitingRequest", "Persistindo processamento para requisi��o {0} no grafo \"{1}\"."},
        {"finishListenerProblem", "Exce��o recebida ao notificar a finaliza��o para a requisi��o {0} no grafo \"{1}\"."},
        {"totalProcTime", "Tempo total de processamento para {0}: {1}"},
        {"noConfig", "Este grafo n�o est� configurado. Ele pode j� ter sido desligado. Configure-o antes de tentar processar alguma requisi��o."},
        {"threadAlive", "A thread \"{0}\" j� est� ativa. O grafo n�o ir� ativ�-la novamente."},
        {"noSuchFilter", "O filtro \"{0}\" n�o existe no grafo \"{1}\"."},
        {"configuringGraph", "Configurando o grafo \"{0}\"..."},
        {"graphConfigured", "Configura��o do grafo \"{0}\" terminada."},
        {"gotInterruption", "Interrup��o recebida."},
        {"graphGoingDown", "Iniciado o processo de desativa��o do grafo \"{0}\"."},
        {"graphDown", "Processo de desativa��o do grafo \"{0}\" terminado."},
        {"listenerConfigured", "O Listener \"{1}\" foi configurado para o grafo \"{0}\"."},
        {"listenerConfigError", "Erro ao configurar o Listener \"{1}\" para o grafo \"{0}\"!"},
        {"listenerFinishError", "Erro ao alertar sobre o t�rmino de uma requisi��o para o Listener do grafo \"{0}\"!"},
        {"commitError", "Erro ao persistir requisi��o {0} no grafo \"{1}\"."},
        {"rollbackError", "Erro ao desfazer requisi��o {0} no grafo \"{1}\"."},
        {"counterUpdateError", "Erro ao atualizar contadores no grafo \"{1}\"."},
        {"shouldNotBeDead", "Um filtro 'threaded' foi encontrado em estado desativado quando o grafo \"{0}\" recebeu o sinal para iniciar o processamento."},
        {"threadedFilterDown", "O filtro 'threaded' \"{1}\" do grafo \"{0}\" est� desativado - for�ando reconfigura��o de todos os filtros."},
        {"invalidTimeout", "Configura��o de timeout inv�lida \"{0}\": deve ser um n�mero inteiro e maior do que zero."},
        {"idleTimeout", "O tempo de timeout do Grafo \"{0}\" foi alcan�ado - desligando o grafo..."},

        // DefaultFilter.java
        {"methodNotSupported", "O m�todo \"{0}\" n�o � suportado por esta classe."},

        // ThreadedFilter.java
        {"gotInterruption", "Interrup��o recebida."},
        {"filterThreadReady", "Thread do filtro pronta."},
        {"filterStopped", "Thread do filtro parada."},
        {"waitingThread", "Aguardando pela finaliza��o da thread do filtro {0}."},

        // Edge.java
        {"noConnectorsAvailable", "N�o foi poss�vel encontrar nenhum conector dispon�vel."},
        {"noConnectorAcceptable", "N�o foi poss�vel encontrar nenhum conector compat�vel com a liga��o [ {0} -> {1} ]"}
    };
}
