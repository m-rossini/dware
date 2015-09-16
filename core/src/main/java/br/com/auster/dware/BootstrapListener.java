/*
 * Copyright (c) 2004-2006 Auster Solutions. All Rights Reserved.
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
 * Created on 14/09/2006
 */
package br.com.auster.dware;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

/**
 * This listener interface enables interactions with the DataAware boostrap at init(), initServer() and
 *   process() calls. 
 *    
 * @author framos
 * @version $Id$
 *
 */
public interface BootstrapListener {


	/**
	 * Executed right before the initialization of this bootstrap in server mode. 
	 * 
	 * @param _instance the bootstrap instance
	 * @param _url the url for listening, when in server mode
	 */
	public void onInitServer(Bootstrap _instance, String _url);
	
	/**
	 * Executed right before enqueuing the created requests into the DataAware queue. This 
	 *   implies that the license was verified and that requests were created.
	 * <p>
	 * The last parameter, <code>_desiredRequests</code>, is set to <code>null</code> when 
	 * 	called {@link br.com.auster.dware.Bootstrap#process(String, Map)}.
	 * 
	 * @param _instance the bootstrap instance
	 * @param _chainName the chosen chain name
	 * @param _args the chain arguments
	 * @param _desiredRequests the list of desired requests (may be <code>null</code>)
	 * @param _requests the list of requests created by the chosen chain, using the specified argumentos
	 */
	public void onProcess(Bootstrap _instance, String _chainName, Map _args, List _desiredRequests, Collection _requests);

	/**
	 * Executes startup configurations for this listener instance. The <code>_configuration</code>
	 * 	parameter is the {@link link br.com.auster.dware.Bootstrap#BOOTSTRAP_LISTENER_ELEMENT} element, 
	 *  from the XML configuration file.
	 *  
	 * @param _configuration
	 */
	public void configure(Element _configuration);
}
