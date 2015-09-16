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
 * Created on 04/09/2007
 */
package br.com.auster.dware.interrupt;

import java.util.Date;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.dware.graph.FinishListener;
import br.com.auster.dware.graph.Graph;
import br.com.auster.dware.graph.Request;

/**
 * @author framos
 * @version $Id$
 *
 */
public class DummyFinishListener implements FinishListener {

	
	private static final Logger log = Logger.getLogger(DummyFinishListener.class);
	
	
	
	public DummyFinishListener(Element _config) {
	}
	
	
	/**
	 * @see br.com.auster.dware.graph.FinishListener#graphCommiting(br.com.auster.dware.graph.Graph, br.com.auster.dware.graph.Request)
	 */
	public void graphCommiting(Graph graph, Request request) throws Exception {
		log.info("We got to 'graphCommiting' for request " + request.getUserKey());
	}

	/**
	 * @see br.com.auster.dware.graph.FinishListener#graphFinished(br.com.auster.dware.graph.Graph, br.com.auster.dware.graph.Request, java.lang.Throwable, java.util.Date)
	 */
	public void graphFinished(Graph graph, Request request, Throwable error, Date time) throws Exception {
		log.info("We got to 'graphFinished' for request " + request.getUserKey());
	}

	/**
	 * @see br.com.auster.dware.graph.FinishListener#graphRollingBack(br.com.auster.dware.graph.Graph, br.com.auster.dware.graph.Request, java.lang.Throwable)
	 */
	public void graphRollingBack(Graph graph, Request request, Throwable error) throws Exception {
		log.info("We got to 'graphRollingBack' for request " + request.getUserKey());
	}

}
