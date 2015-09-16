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
package br.com.auster.dware.filter.res;

import java.util.ListResourceBundle;

/**
 * This class is used by the I18n class for internationalization purposes.
 * 
 * @version $Id: MessagesBundle_pt_BR.java 282 2007-03-01 17:49:44Z framos $
 */
public final class MessagesBundle_pt_BR extends ListResourceBundle {
    public final Object[][] getContents() {
        return contents;
    }

    static final Object[][] contents = {
        // ObjectManagerFilter
        {"JAXBIllegal", "Parametros Inv�lidos. Elemento Corrente �: {0}"},
			
        // InputFromFile.java
        {"setInputRequest", "Usando o arquivo {1} como entrada para a requisi��o {0}."},
        {"problemOpenFile", "Problema abrindo o arquivo de entrada para a requisi��o {0}."},
        {"fileDeleted", "Arquivo {0} apagado."},
        {"problemDeleting", "Problema apagando arquivo {0}."},
        {"problemClosingFile", "Problema ao fechar arquivo."},

        // OutputToFile.java
        {"setOutputRequest", "Usando o arquivo {1} como sa�da para a requisi��o {0}."},
        {"problemCreatingFile", "Problema abrindo o arquivo de sa�da para a requisi��o {0}."},
        {"couldNotMoveFile", "N�o foi poss�vel mover o arquivo {0} para o novo arquivo {1}."},

        // DefaultFilter.java
        {"methodNotSupported", "O m�todo \"{0}\" n�o � suportado por esta classe."},

        // ThreadedFilter.java
        {"gotInterruption", "Interrup��o recebida."},
        {"filterThreadReady", "Thread do filtro pronta."},
        {"filterStopped", "Thread do filtro parada."},

        // PipeConnector.java
        {"problemsPipe", "Problema criando o 'pipe'."},
        {"problemClosingWriter", "Problema ao fechar o escritor."},
        {"problemClosingReader", "Problema ao fechar o leitor."},

        // NIOFilter.java
        {"inputNotSet", "A entrada deve ser definida antes do uso do filtro (configura��o inv�lida?)."},
        {"outputNotSet", "A sa�da deve ser definida antes do uso do filtro (configura��o inv�lida?)."},

        // XMLReaderFilter.java
        {"defaultXMLReader", "Usando o XMLReader padr�o para leitura da entrada de dados."},
        {"unsupportedOutputType", "Sa�da do tipo {0} n�o � suportada por {1}."},
        {"unsupportedInputType", "Entrada do tipo {0} n�o � suportada por {1}."},

        // XSLFilter.java
        {"noXSLFiles", "N�o h� arquivos XSL definidos na configura��o para o filtro {0}."},
        {"usingIncremental", "Usando processamento incremental no XSL."},
        {"usingXSLTC", "Ativando o uso do compilador XSLT no XSL."},

        // ContentHandlerFilter.java
        {"usingContentHandler", "Usando a classe {0} como ContentHandler para o filtro {1}."},

        // OffsetDataFromFile.java
        {"problemWriteOutput", "Problemas ao escrever para a sa�da."},
        {"readDataNotEnough", "Dados lidos n�o s�o suficientes para serem escritos."},

        // PartialFileRequestFromFileList.java
        {"invalidRequestType", "Tipo de requisi��o inv�lido: {0}"},

        // XMLSplitterFilter.java
        {"chNotNull", "O 'content handler' n�o pode ser nulo."},
        {"chNotFound", "O 'content handler' nomeado {0} n�o foi encontrado ou definido."},
        
        //CHLimiterPipeFilter
        {"maxSizeExceeded", "O tamanho, {0} , da requisi��o � maior que o limite de {1} bytes."},
        
        //DataSaverFilter
        {"allFilters.startProcessing", "Iniciando o processamento do filtro ''{0}'' de nome ''{1}''."},
        {"allFilters.endProcessing",  "Finalizado o processamento do filtro ''{0}'' de nome ''{1}'' em {2}ms."},
        {"allFilters.hasNextFilter", "Outro filtro de processamento de objetos configurado...iniciando-o."},
        {"allFilters.noNextFilter", "Nenhum filtro de processamento de objetos configurado a seguir. Finalizando!"}        
        
    };
}
