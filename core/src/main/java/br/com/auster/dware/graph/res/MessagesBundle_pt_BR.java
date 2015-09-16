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
        {"filterListEmpty", "A configuração do grafo deve ter elementos 'filter'."},
        {"addFilter", "Adicionando o filtro {0}..."},
        {"noConnectionFilter", "Não há conexão sainte do filtro {0}."},
        {"filterAlreadyAdded", "Tentativa de adicionar o filtro \"{0}\" que já havia sido adicionado (filtro com nome repetido na configuração?)."},
        {"makingConnection", "Criando a aresta [ {0} -> {1} ]..."},
        {"connectionFilterFailed", "Um ou ambos dos filtros \"{0}\" ou \"{1}\" não foi adicionado ao grafo. Verifique sua configuração."},
        {"invalidConnectorClassName", "Não foi possível instanciar o conector \"{0}\". Conectores padrões serão usados no lugar desse."},
        {"processingRequest", "Iniciando processamento da requisição {0} no grafo \"{1}\"."},
        {"waitingRequest", "Aguardando finalização do processamento da requisição {0} no grafo \"{1}\"."},
        {"rollingbackRequest", "Desfazendo processamento para requisição {0} no grafo \"{1}\"."},
        {"commitingRequest", "Persistindo processamento para requisição {0} no grafo \"{1}\"."},
        {"finishListenerProblem", "Exceção recebida ao notificar a finalização para a requisição {0} no grafo \"{1}\"."},
        {"totalProcTime", "Tempo total de processamento para {0}: {1}"},
        {"noConfig", "Este grafo não está configurado. Ele pode já ter sido desligado. Configure-o antes de tentar processar alguma requisição."},
        {"threadAlive", "A thread \"{0}\" já está ativa. O grafo não irá ativá-la novamente."},
        {"noSuchFilter", "O filtro \"{0}\" não existe no grafo \"{1}\"."},
        {"configuringGraph", "Configurando o grafo \"{0}\"..."},
        {"graphConfigured", "Configuração do grafo \"{0}\" terminada."},
        {"gotInterruption", "Interrupção recebida."},
        {"graphGoingDown", "Iniciado o processo de desativação do grafo \"{0}\"."},
        {"graphDown", "Processo de desativação do grafo \"{0}\" terminado."},
        {"listenerConfigured", "O Listener \"{1}\" foi configurado para o grafo \"{0}\"."},
        {"listenerConfigError", "Erro ao configurar o Listener \"{1}\" para o grafo \"{0}\"!"},
        {"listenerFinishError", "Erro ao alertar sobre o término de uma requisição para o Listener do grafo \"{0}\"!"},
        {"commitError", "Erro ao persistir requisição {0} no grafo \"{1}\"."},
        {"rollbackError", "Erro ao desfazer requisição {0} no grafo \"{1}\"."},
        {"counterUpdateError", "Erro ao atualizar contadores no grafo \"{1}\"."},
        {"shouldNotBeDead", "Um filtro 'threaded' foi encontrado em estado desativado quando o grafo \"{0}\" recebeu o sinal para iniciar o processamento."},
        {"threadedFilterDown", "O filtro 'threaded' \"{1}\" do grafo \"{0}\" está desativado - forçando reconfiguração de todos os filtros."},
        {"invalidTimeout", "Configuração de timeout inválida \"{0}\": deve ser um número inteiro e maior do que zero."},
        {"idleTimeout", "O tempo de timeout do Grafo \"{0}\" foi alcançado - desligando o grafo..."},

        // DefaultFilter.java
        {"methodNotSupported", "O método \"{0}\" não é suportado por esta classe."},

        // ThreadedFilter.java
        {"gotInterruption", "Interrupção recebida."},
        {"filterThreadReady", "Thread do filtro pronta."},
        {"filterStopped", "Thread do filtro parada."},
        {"waitingThread", "Aguardando pela finalização da thread do filtro {0}."},

        // Edge.java
        {"noConnectorsAvailable", "Não foi possível encontrar nenhum conector disponível."},
        {"noConnectorAcceptable", "Não foi possível encontrar nenhum conector compatível com a ligação [ {0} -> {1} ]"}
    };
}
