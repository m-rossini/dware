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
 * Created on Jul 13, 2005
 */
package br.com.auster.dware.filter;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.io.BaseFilenameBuilder;
import br.com.auster.common.lang.StringUtils;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * <p><b>Title:</b> FilenameBuilder</p>
 * <p><b>Description:</b> Utility class used to create output 
 * filenames from Requests</p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 * 
 * <b>NOTE:</b> XML Namespace is ignored.
 * 
 * 
 * @author rbarone
 * @version $Id: FilenameBuilder.java 335 2007-12-06 13:44:30Z mtengelm $
 */
public class FilenameBuilder extends BaseFilenameBuilder{

  /**
   * Returns a new FilenameBuilder configured from the specified
   * <code>Element</code>.
   * 
   * @param config
   *          The configuration <code>Element</code> that contains a
   *          {@link #FILENAME_ELEMENT} and childs.
   */
  public FilenameBuilder(final Element config) {
  	super(config);
  }

  /**
   * Creates the file that will be opened for writing as the output.
   * 
   * @param request
   *           the file name will be defined using some request's properties.
   * @throws FilterException 
   */
  public String getFilename(Request request) throws FilterException {
    try {
			return this.getFilename(request.getAttributes());
		} catch (Exception e) {
			throw new FilterException(e);
		}
  }

}
