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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Frederico A Ramos
 * @version $Id: ObjectTemplate.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class ObjectTemplate implements Serializable, Cloneable, Comparable, TemplateElement {

  private String name;

  private List fields;

  private List subTemplates;

  public ObjectTemplate(String _name) {
    this.name = _name;
    fields = new ArrayList();
    subTemplates = new ArrayList();
  }

  public final String getTemplateName() {
    return this.name;
  }

  public final List getFields() {
    return this.fields;
  }

  public final void setFields(List _newFields) {
    if (_newFields != null) {
      this.fields = _newFields;
    }
  }

  public final List getSubTemplates() {
    return this.subTemplates;
  }

  public final void setSubTemplates(List _newTemplates) {
    if (_newTemplates != null) {
      this.subTemplates = _newTemplates;
    }
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
    result = 37 * result + ((this.fields != null) ? this.fields.hashCode() : 0);
    result = 37 * result + ((this.subTemplates != null) ? this.subTemplates.hashCode() : 0);
    return result;
  }

  public int compareTo(Object _other) {
    ObjectTemplate other = (ObjectTemplate) _other;
    int comparison = this.name.compareTo(other.name);
    if (comparison != 0)
      return comparison;
    if (!this.fields.equals(other.fields)) {
      return this.fields.size() - other.fields.size();
    }
    if (!this.subTemplates.equals(other.subTemplates)) {
      return this.subTemplates.size() - other.subTemplates.size();
    }
    return 0;
  }

  public Object clone() throws CloneNotSupportedException {
    ObjectTemplate clone = new ObjectTemplate(this.name);
    Iterator iterator = this.fields.iterator();
    while (iterator.hasNext()) {
      clone.getFields().add(((FieldTemplate) iterator.next()).clone());
    }
    iterator = this.subTemplates.iterator();
    while (iterator.hasNext()) {
      clone.getSubTemplates().add(((ObjectTemplate) iterator.next()).clone());
    }
    return clone;
  }

  public String toString() {
    return "{  Template >> " + "name=" + this.name + " : fields=" + this.fields
           + " : subTemplates=" + this.subTemplates + " }";
  }
}
