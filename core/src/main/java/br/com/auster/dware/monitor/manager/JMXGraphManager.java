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
* Created on 16/06/2005
*/
package br.com.auster.dware.monitor.manager;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import br.com.auster.common.jmx.AusterMBean;
import br.com.auster.dware.manager.GraphManager;

/**
 * <p><b>Title:</b> JMXmanagerMBean</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXGraphManager implements AusterMBean, JMXGraphManagerMBean {

   private static final Logger log = Logger.getLogger(JMXGraphManager.class);
   private static final String MBEAN_NAME = "type=DataAware,name=GraphManager";
   private GraphManager graphManger;
   private String name = MBEAN_NAME;


   /**
    * 
    */
   public JMXGraphManager(GraphManager gm) {
      super();
      this.graphManger = gm;
   }


   /* (non-Javadoc)
    * @see br.com.auster.dware.management.DWareMBean#getMBeanName()
    */
   public String getMBeanName() {
      return name;
   }
   
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.DWareMBean#setMBeanName(java.lang.String)
    */
   public void setMBeanName(String _name) {
	   name = MessageFormat.format(AusterMBean.MBEAN_NAME_FORMAT, new Object[] {_name, _name } );
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.DWareMBean#setMBeanName(java.lang.String, java.lang.String)
    */
   public void setMBeanName(String _type, String _name) {
	   name = MessageFormat.format(AusterMBean.MBEAN_NAME_FORMAT, new Object[] {_type, _name } );
   }  
   
   /* (non-Javadoc)
    * @see br.com.auster.dware.manager.jmx.JMXmanager#getGrouphNames()
    */
   public List getGroupNames() {
      return this.graphManger.getGroupNames();      
   }

   public void log() {
      log.info("-----------------" + this.getMBeanName() + "----------------------------");
      log.info("Group Names:");
      for (Iterator itr=this.getGroupNames().iterator();itr.hasNext();) {
         log.info("Group name is " + (String) itr.next());
      }
      log.info("---------------------------------------------");
   }
  
}
 