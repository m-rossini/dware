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
 * Created on 06/06/2006
 */
package br.com.auster.dware.sax;

import java.util.HashMap;
import java.util.Map;

/**
 * @author framos
 * @version $Id$
 */
public class MultiHandlerContext {

	
	private StringBuffer currentPath; 
	private Map context;
	
	
	public MultiHandlerContext() {
		this.currentPath = new StringBuffer();
		this.context = new HashMap();
	}
	
	
	public final String getCurrentPath() {
		return this.currentPath.toString();
	}
	
	public String startedPath(String _path) {
		if (_path != null) { 
			this.currentPath.append(_path);
			this.currentPath.append(MultiHandlerForwarder.XML_PATH_SEPARATOR);
		}
		return this.getCurrentPath();
	}
	
	public String endedPath(String _path) {
		if (_path != null) { 
			this.currentPath.delete(this.currentPath.length()-(_path.length()+1), this.currentPath.length());
		}
		return this.getCurrentPath();
	}
	
	public Object getAttribute(String _key) {
		return this.context.get(_key);
	}
	
	public void setAttribute(String _key, Object _value) {
		this.context.put(_key, _value);
	}
	
	public boolean hasAttribute(String _key) {
		return this.context.containsKey(_key);
	}
	
	public Object removeAttribute(String _key) {
		return this.context.remove(_key);
	}
	
	public void removeAllAttributes() {
		this.context.clear();
	}
	
	public void reset() {
		removeAllAttributes();
		this.currentPath.delete(0, this.currentPath.length());
		this.currentPath.append(MultiHandlerForwarder.XML_PATH_SEPARATOR);
	}
}
