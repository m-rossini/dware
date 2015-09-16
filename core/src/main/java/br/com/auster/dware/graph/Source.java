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
 * Defines the classes that can accept or give objects for output data.
 * 
 * @version $Id: Source.java 2 2005-04-20 21:22:27Z rbarone $
 */
public interface Source {

  /**
   * Returns the name of this Source object.
   */
  public String getFilterName();

  /**
   * Returns an output object, that will be used by the Sink object that got it
   * to pull data from this Source object.
   * 
   * @param sinkName
   *          the Sink object's name that is requesting an output object.
   * @throws ConnectException
   *           if some error ocurred while getting the output.
   * @throws UnsupportedOperationException
   *           if this method is not supported.
   */
  public Object getOutput(String sinkName) throws ConnectException, UnsupportedOperationException;

  /**
   * Receives an output object, that will be used by this Source object to push
   * data to the Sink object that got it.
   * 
   * @param sinkName
   *          the Sink object's name that is giving an output object.
   * @throws ConnectException
   *           if some error ocurred while setting the output.
   * @throws UnsupportedOperationException
   *           if this method is not supported.
   */
  public void setOutput(String sinkName, Object output) throws ConnectException,
      UnsupportedOperationException;
}
