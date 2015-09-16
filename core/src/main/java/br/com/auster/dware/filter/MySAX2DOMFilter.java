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
package br.com.auster.dware.filter;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.util.I18n;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;

/**
 * @author framos
 * @version $Id: MySAX2DOMFilter.java 87 2005-08-04 21:21:25Z rbarone $
 */
public class MySAX2DOMFilter extends SAX2DOMFilter {

  private ObjectProcessor objProcessor;

  protected final static Logger log = Logger.getLogger(MySAX2DOMFilter.class);

  final I18n i18n = I18n.getInstance(this.getClass());

  public MySAX2DOMFilter(String name) {
    super(name);
  }

  public void configure(Element config) throws FilterException {
    super.configure(config);
  }

  public void process(Element root) {
    try {
      this.objProcessor.processElement(root);
    } catch (FilterException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void setOutput(String sourceName, Object objProcessor) {
    this.objProcessor = (ObjectProcessor) objProcessor;
  }
}
