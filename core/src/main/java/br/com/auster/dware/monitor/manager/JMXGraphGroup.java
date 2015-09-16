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
* Created on 18/06/2005
*/
package br.com.auster.dware.monitor.manager;

import java.text.MessageFormat;

import org.apache.log4j.Logger;

import br.com.auster.common.jmx.AusterMBean;
import br.com.auster.dware.manager.GraphGroup;

/**
 * <p><b>Title:</b> JMXGraphGroup</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXGraphGroup implements AusterMBean, JMXGraphGroupMBean {

   protected GraphGroup gg;
   private static final Logger log = Logger.getLogger(JMXGraphGroup.class);
   private static final String MBEAN_NAME = "type=DataAware,name=GraphGroup";
   private String name = MBEAN_NAME;

   /**
    * 
    */
   public JMXGraphGroup(GraphGroup gg) {
      super();
      this.gg = gg;
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#kill()
    */
   public void kill() {
      this.gg.killGroup();
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
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#getGraphGroupName()
    */
   public String getGraphGroupName() {
      return this.gg.getGraphGroupName();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#isCosing()
    */
   public boolean isClosing() {
      return this.gg.isClosing();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#hasToProcessLastObjects()
    */
   public boolean isToProcessLastObjects() {
      return this.gg.hasToProcessLastObjects();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#log()
    */
   public void log() {
      log.info("-----------------" + this.getMBeanName() + "----------------------------");
      log.info("Is Closing?          " + this.isClosing());
      log.info("Process Last Object? " + this.isToProcessLastObjects());
      log.info("Graph Group Name " + this.getGraphGroupName());
      log.info("Thread State is " + this.getState());
      log.info("---------------------------------------------");
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#getState()
    */
   public String getState() {
      String state;
      if (this.gg.isAlive()) {
        switch (this.gg.getStatus()) {
          case GraphGroup.STATUS_INITIALIZING:
            state = "initializing";
            break;
          case GraphGroup.STATUS_CONSUMING:
            state = "consuming";
            break;
          case GraphGroup.STATUS_PROCESSING:
            state = "processing";
            break;
          case GraphGroup.STATUS_WAITING_GRAPH:
            state = "waiting (graph availability)";
            break;
          case GraphGroup.STATUS_WAITING_EMPTY:
            state = "waiting (empty queue)";
            break;
          case GraphGroup.STATUS_WAITING_FULL:
            state = "waiting (full capacity)";
            break;
          case GraphGroup.STATUS_SHUTDOWN:
            state = "shutdown";
            break;
          case GraphGroup.STATUS_DEAD:
            state = "finished";
            break;
          default:
            state = "unknown [" + this.gg.getStatus() + "]";
        }
      } else if (this.gg.isInterrupted()) {
        state = "interrupted";
      } else {
        state = "dead";
      }
      return state;
   }

}
 