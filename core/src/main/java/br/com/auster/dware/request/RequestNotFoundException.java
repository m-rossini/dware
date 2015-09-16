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

/**
 * Thrown when a request from a {@link RequestFilter} was not found or created.
 *
 * @author Ricardo Barone
 * @version $Id: RequestNotFoundException.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class RequestNotFoundException extends Exception {

  /**
   * 
   */
  public RequestNotFoundException() {
    super();
  }

  /**
   * @param message
   */
  public RequestNotFoundException(String message) {
    super(message);
  }

  /**
   * @param message
   * @param cause
   */
  public RequestNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * @param cause
   */
  public RequestNotFoundException(Throwable cause) {
    super(cause);
  }

}
