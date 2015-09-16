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
 * Created on 26/12/2007
 */
package br.com.auster.dware.request;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import br.com.auster.dware.graph.Request;

/**
 * Esta classe � um {@linkplain RequestFilter}.
 * 
 * Como request filter, a mesma segue a interface definida, entretanto SEMPRE ir� rejeitar as requisi��es.
 * Projetada para garantir que nenhuma requisi��o nunca seja armazenada na lista de accepted NEM de allowed.
 * Idealmente para uso de builders que n�o precisam armazenar requests, e grafos que n�o ser�o executados.
 * Um exemplo desse cen�rio � a gera��o APENAS de indices de requisi��es.
 * 
 * @author mtengelm
 * @version $Id$
 * @since 26/12/2007
 */
public class AlwaysRejectFilter implements RequestFilter {

	private AtomicLong acceptCalledCount= new AtomicLong(0);
	/**
	 * Creates a new instance of the class <code>AlwaysRejectFilter</code>.
	 */
	public AlwaysRejectFilter() {
	}

	public long getAcceptedCalledCounter() {
		return acceptCalledCount.longValue();
	}
	
	/**
	 * 
	 * @param request
	 * @return false. Sempre retornar� false.
	 * 
	 * @see br.com.auster.dware.request.RequestFilter#accept(br.com.auster.dware.graph.Request)
	 */
	public boolean accept(Request request) {
		return accept(request, false);
	}

	/**
	 * 
	 * @param request
	 * @param ignore
	 * @return false. Sempre retornar� false.
	 * 
	 * @see br.com.auster.dware.request.RequestFilter#accept(br.com.auster.dware.graph.Request, boolean)
	 */
	public boolean accept(Request request, boolean ignore) {
		acceptCalledCount.incrementAndGet();
		return false;
	}

	/**
	 * Este m�todo n�o faz a��o alguma.
	 * 
	 * @see br.com.auster.dware.request.RequestFilter#acceptAll()
	 */
	public void acceptAll() {
	}

	/**
	 * 
	 * @return true. Sempre retornar� true.
	 * @see br.com.auster.dware.request.RequestFilter#canAccept()
	 */
	public boolean canAccept() {
		return true;
	}

	/**
	 * 
	 * @return Uma cole��o. Sempre retornar� uma cole��o (No caso um Set) vazia.
	 * @see br.com.auster.dware.request.RequestFilter#getAcceptedRequests()
	 */
	public Collection getAcceptedRequests() {
		return Collections.emptySet();
	}

	/**
	 * 
	 * @return Um Mapa. Nesta classe sempre um mapa vazio.
	 * 
	 * @see br.com.auster.dware.request.RequestFilter#getAllowedRequests()
	 */
	public Map<String, List<Request>> getAllowedRequests() {
		return null ;
	}

	/**
	 * 
	 * @return Uma cole��o (Neste caso um Set) vazia.
	 * @see br.com.auster.dware.request.RequestFilter#getRemainingRequests()
	 */
	public Collection getRemainingRequests() {
		return Collections.emptySet();
	}

	/**
	 * 
	 * @return Um request filter. Neste caso retorna ele mesmo.
	 * @see br.com.auster.dware.request.RequestFilter#reset()
	 */
	public RequestFilter reset() {
		return this;
	}

	/**
	 * Este m�todo n�o toma nenhuma a��o.
	 * 
	 * @param requests
	 * @see br.com.auster.dware.request.RequestFilter#setPreviousRequests(java.util.Collection)
	 */
	public void setPreviousRequests(Collection requests) {
	}

	/**
	 * 
	 * @param request
	 * @return false. Sempre retorna false
	 * @see br.com.auster.dware.request.RequestFilter#willAccept(br.com.auster.dware.graph.Request)
	 */
	public boolean willAccept(Request request) {
		return false;
	}

}
