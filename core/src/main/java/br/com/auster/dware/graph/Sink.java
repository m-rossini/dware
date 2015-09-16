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
 * Defines the classes that can accept or give objects for input data.
 * 
 * @version $Id: Sink.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface Sink {

  /**
   * Returns the name of this Sink object.
   */
  public String getFilterName();

  /**
   * Returns an input object, that will be used by the Source object that got it
   * to push data to this Sink object.
   * 
   * @param sourceName
   *          the Source object's name that is requesting an input object.
   * @throws ConnectException
   *           if some error ocurred while getting the input.
   * @throws UnsupportedOperationException
   *           if this method is not supported.
   */
  public Object getInput(String sourceName) throws ConnectException, UnsupportedOperationException;

  /**
   * Receives an input object, that will be used by this Sink object to pull
   * data from the Source object that gave it.
   * 
   * @param sourceName
   *          the Source object's name that is giving the input object.
   * @throws ConnectException
   *           if some error ocurred while setting the input.
   * @throws UnsupportedOperationException
   *           if this method is not supported.
   */
  public void setInput(String sourceName, Object input) throws ConnectException,
      UnsupportedOperationException;
}
