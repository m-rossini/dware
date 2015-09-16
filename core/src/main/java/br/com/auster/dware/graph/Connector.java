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

import org.apache.log4j.Logger;

/**
 * Defines the classes that connects a Source to a Sink.
 * 
 * @version $Id: Connector.java 87 2005-08-04 21:21:25Z rbarone $
 */
public interface Connector {

  /**
   * This connector is used to set an input object for a Sink by getting an
   * output object from the Source.
   */
  public static final class GetSourceSetSinkConnector implements Connector {

    private static final Logger log = Logger.getLogger(GetSourceSetSinkConnector.class);

    public final void connect(Source source, Sink sink) throws ConnectException {
      sink.setInput(source.getFilterName(), source.getOutput(sink.getFilterName()));
    }
  }

  /**
   * This connector is used to set an output object for the Source by getting an
   * input object from the Sink.
   */
  public static final class GetSinkSetSourceConnector implements Connector {

    private static final Logger log = Logger.getLogger(GetSinkSetSourceConnector.class);

    public final void connect(Source source, Sink sink) throws ConnectException {
      source.setOutput(sink.getFilterName(), sink.getInput(sink.getFilterName()));
    }
  }

  /**
   * Connects a source to a sink.
   * 
   * @throws ConnectException
   *           if could not connect them.
   */
  public void connect(Source source, Sink sink) throws ConnectException;
}
