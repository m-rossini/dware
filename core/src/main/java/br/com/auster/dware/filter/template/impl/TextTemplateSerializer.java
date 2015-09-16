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
package br.com.auster.dware.filter.template.impl;

import java.io.IOException;
import java.io.OutputStream;

import br.com.auster.dware.filter.template.TemplateElement;
import br.com.auster.dware.filter.template.TemplateSerializer;

/**
 * TODO class comments
 * 
 * @author Frederico A Ramos
 * @version $Id: TextTemplateSerializer.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class TextTemplateSerializer implements TemplateSerializer {

  private OutputStream output;

  public static final String NEWLINE = System.getProperty("line.separator");

  public TextTemplateSerializer() {
    super();
    output = null;
  }

  public void serializeTemplate(TemplateElement _template) throws IOException {
    if (isReady()) {
      if (_template != null)
        output.write(_template.toString().getBytes());
      output.write(NEWLINE.getBytes());
    }
  }

  public void setOutputStream(OutputStream _output) {
    this.output = _output;
  }

  public final boolean isReady() {
    return (output != null);
  }
}
