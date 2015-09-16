/*
* Copyright (c) 2004-2005 Auster Solutions. All Rights Reserved.
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
* Created on 19/05/2005
*/
package br.com.auster.dware.request.file;

import java.util.Map;

import org.w3c.dom.Element;

import br.com.auster.common.io.FileSet;
import br.com.auster.dware.request.RequestBuilder;
import br.com.auster.dware.request.RequestFilter;

/**
 * <p><b>Title:</b> SimpleFileRequestBuilder</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class SimpleFileListRequestBuilder implements RequestBuilder {

   private String name;
   static protected final String FILE_NAMES_PARM = "filenames";

   /**
    * @param name
    * @param config
    */
   public SimpleFileListRequestBuilder(String name, Element config) {
      this.name = name;
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.request.RequestBuilder#getName()
    */
   public String getName() {
      return this.name;
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.request.RequestBuilder#createRequests(java.util.Map)
    */
   public RequestFilter createRequests(Map args) {
      return createRequests(null, args);
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.request.RequestBuilder#createRequests(br.com.auster.dware.request.RequestFilter, java.util.Map)
    */
   public RequestFilter createRequests(RequestFilter filter, Map args) {
      //System.out.println("Filtered." + filter + ".Map=" + args);
      filter.accept(new FileListRequest(FileSet.getFiles((String) args.get(FILE_NAMES_PARM))));
      return filter;
   }


}
 