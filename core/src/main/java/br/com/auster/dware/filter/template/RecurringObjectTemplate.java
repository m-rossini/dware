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
 * @version $Id: RecurringObjectTemplate.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class RecurringObjectTemplate extends ObjectTemplate implements Serializable, Cloneable,
    Comparable {

  private String recurringPath;

  public RecurringObjectTemplate(String _name, String _reccuringPath) {
    super(_name);
    this.recurringPath = _reccuringPath;
  }

  public final String getRecurringPath() {
    return this.recurringPath;
  }

  public int hashCode() {
    int result = 17;
    result = 37 * result + super.hashCode();
    result = 37 * result + ((this.recurringPath != null) ? this.recurringPath.hashCode() : 0);
    return result;
  }

  public int compareTo(Object _other) {
    int comparison = super.compareTo(_other);
    if (comparison != 0)
      return comparison;

    RecurringObjectTemplate other = (RecurringObjectTemplate) _other;
    comparison = this.recurringPath.compareTo(other.recurringPath);
    if (comparison != 0)
      return comparison;
    return 0;
  }

  public Object clone() throws CloneNotSupportedException {
    ObjectTemplate superClone = (ObjectTemplate) super.clone();
    RecurringObjectTemplate clone = new RecurringObjectTemplate(this.getTemplateName(), this
        .getRecurringPath());
    clone.setFields(superClone.getFields());
    clone.setSubTemplates(superClone.getSubTemplates());
    return clone;
  }

  public String toString() {
    return "{ Template >> " + super.toString() + " recurring-path = '" + this.recurringPath + "'"
           + " }";
  }

}
