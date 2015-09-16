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
 * Created on Apr 5, 2005
 */
package br.com.auster.dware.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Element;

import br.com.auster.common.log.LogFactory;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestBuilderManager;

/**
 * TODO class comments
 * 
 * @author Ricardo Barone
 * @version $Id: RequestManagerTest.java 356 2008-02-15 13:12:36Z lmorozow $
 */
public class RequestManagerTest {

  /**
   * 
   */
  public RequestManagerTest() {
  }

  public static final void main(String[] args) {
    try {
      Element config = DOMUtils.openDocument("requests.xml", false);
      LogFactory.configureLogSystem(config);
      RequestBuilderManager manager = new RequestBuilderManager(config);

      HashMap requestArgs = new HashMap();
      requestArgs.put("formats", "astxt");
      HashMap builderArgs = new HashMap();
      builderArgs.put("filenames", "request-test.txt");
      builderArgs.put("request-params", requestArgs);
      HashMap managerArgs = new HashMap();
      managerArgs.put("teste-1", builderArgs);

      //List desiredRequests = new ArrayList();
      //desiredRequests.add("r1f3");

      //Collection requests = manager.createRequests("test-chain", managerArgs, desiredRequests);
      Collection requests = manager.createRequests("test-chain", managerArgs);
      for (Iterator it = requests.iterator(); it.hasNext();) {
        Request req = (Request) it.next();
        System.out.println("Request ==> " + req.toString() + "; Atts => " + req.getAttributes());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
