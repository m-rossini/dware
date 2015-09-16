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
package br.com.auster.dware.graph;

/**
 * This exception is thrown when some error occurs while creating or
 * manipulating a filter graph.
 * 
 * @version $Id: GraphException.java 2 2005-04-20 21:22:27Z rbarone $
 */
public final class GraphException extends Exception {

  /**
   * Creates a new instance of <code>GraphException</code> with detail message
   * and the cause that caused this.
   */
  public GraphException(String msg, Throwable cause) {
    super(msg, cause);
  }

  /**
   * Constructs an instance of <code>GraphException</code> with the specified
   * detail message.
   * 
   * @param msg
   *          the detail message.
   */
  public GraphException(String msg) {
    super(msg);
  }

  /**
   * Constructs an instance of <code>GraphException</code> with the cause.
   * 
   * @param msg
   *          the detail message.
   */
  public GraphException(Throwable e) {
    super(e);
  }
}
