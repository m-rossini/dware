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

import org.apache.log4j.Logger;

import br.com.auster.common.jmx.AusterMBean;
import br.com.auster.dware.manager.LocalGraphGroup;

/**
 * <p><b>Title:</b> JMXGraphGroup</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXLocalGraphGroup extends JMXGraphGroup implements AusterMBean, JMXLocalGraphGroupMBean {

   private LocalGraphGroup lgg;
   private static final Logger log = Logger.getLogger(JMXLocalGraphGroup.class);
   private static final String MBEAN_NAME = "type=DataAware,name=LocalGraphGroup";

   /**
    * @param gg
    */
   public JMXLocalGraphGroup(LocalGraphGroup gg) {
      super(gg);
      lgg = gg;
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
      log.info("Max Threads for this group is " + this.getMaxThreads());
      log.info("---------------------------------------------");
   }
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#getMaxThreads()
    */
   public int getMaxThreads() {
      return this.lgg.getMaxThreads();
   }
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXLocalGraphGroupMBean#setMaxThreads(int)
    */
//   public void changeMaxThreads(int maxThreads) {
//      log.info("Changing MAX THREADS to " + maxThreads);
//      this.lgg.setMaxThreads(maxThreads);      
//   }

}
 