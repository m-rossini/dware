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
package br.com.auster.dware.filter.template;

import java.io.Serializable;

/**
 * @author Frederico A Ramos
 * @version $Id: FieldTemplate.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class FieldTemplate implements Serializable, Cloneable, Comparable, TemplateElement {

  private String name;

  private Object value;

  public static final String TEMPLATEFIELD_CONSTANT_TYPE = "default";

  public static final String TEMPLATEFIELD_XPATH_TYPE = "xpath";

  public static final String TEMPLATEFIELD_PLUGIN_TYPE = "plugin-class";

  public FieldTemplate(String _name) {
    this.name = _name;
  }

  protected FieldTemplate(String _name, Object _value) {
    this(_name);
    this.value = _value;
  }

  public final String getTemplateName() {
    return this.name;
  }

  public final void setValue(Object _newValue) {
    this.value = _newValue;
  }

  public final Object getValue() {
    return this.value;
  }

  public boolean equals(Object _other) {
    try {
      return (this.compareTo(_other) == 0);
    } catch (ClassCastException cce) {
      return false;
    }
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + ((this.name != null) ? this.name.hashCode() : 0);
    result = 37 * result + ((this.value != null) ? this.value.hashCode() : 0);
    return result;
  }

  public int compareTo(Object _other) {
    FieldTemplate other = (FieldTemplate) _other;
    return this.name.compareTo(other.name);
  }

  public Object clone() throws CloneNotSupportedException {
    return new FieldTemplate(this.name, this.value);
  }

  public String toString() {
    return "{ " + this.getClass().getName() + " >> " + "name=" + this.name + " : value="
           + this.value + " }";
  }

}
