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
 * Created on Jul 7, 2005
 */
package br.com.auster.dware.filter;

import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.Request;

import com.sun.xml.fastinfoset.sax.SAXDocumentSerializer;


/**
 * <p>
 * <b>Title:</b> XMLInfosetSerializerFilter
 * </p>
 * <p>
 * <b>Description:</b>
 * XML Serializer Filter that uses the Fast Infoset binary representation.
 * This filter has no configuration parameters.
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2004-2005
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 * 
 * @author rbarone
 * @version $Id: XMLInfosetSerializerFilter.java 44 2005-07-07 18:02:19Z rbarone $
 */
public class XMLInfosetSerializerFilter extends DefaultFilter {

  SAXDocumentSerializer serializer;
  
  public XMLInfosetSerializerFilter(String name) {
    super(name);
  }
  
  /**
   * Creates the serializer for the SAX events.
   */
  public final void prepare(Request request) {
    this.serializer = new SAXDocumentSerializer();
  }

  /**
   * Gets the serializer, which is a ContentHandler.
   */
  public final Object getInput(String sourceName) throws ConnectException {
    return this.serializer;
  }

  /**
   * Sets the WritableByteChannel output for this filter.
   */
  public final void setOutput(String sinkName, Object output) {
    if (output instanceof WritableByteChannel) {
      output = Channels.newOutputStream((WritableByteChannel) output);
    }
    this.serializer.setOutputStream((OutputStream) output);
  }

}
