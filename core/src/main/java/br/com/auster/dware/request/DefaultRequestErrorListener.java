/*
 * Copyright (c) 2004-2005 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.request;

import org.apache.log4j.Logger;

import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.Request;

/**
 * TODO class comments 
 *
 * @author Ricardo Barone
 * @version $Id: DefaultRequestErrorListener.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class DefaultRequestErrorListener implements RequestErrorListener {

  private static final Logger log = Logger.getLogger(DefaultRequestErrorListener.class);

  private I18n i18n = I18n.getInstance(DefaultRequestErrorListener.class);

  /**
   * Default constructor.
   */
  public DefaultRequestErrorListener() {
  }

  /**
   * This method will never throw an exception, but just log the error.
   * 
   * @param request
   *           the request that failed.
   * @param error
   *           the error that occurred.
   */
  public void errorOccured(Request request, Throwable error) {
    if (error instanceof RequestNotFoundException) {
      log.warn(error.getMessage());
    } else {
      log.error(i18n.getString("errorDetected", request, error.getMessage()), error);
    }
  }

}
