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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import br.com.auster.dware.manager.PriorityQueueWishGraphGroup;

/**
 * <p><b>Title:</b> JMXGraphGroup</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXPriorityWishGraphGroup extends JMXLocalGraphGroup implements JMXPriorityWishGraphGroupMBean {

   private PriorityQueueWishGraphGroup pgg;
   private static final Logger log = Logger.getLogger(JMXPriorityWishGraphGroup.class);
   private static final String MBEAN_NAME = "type=DataAware,name=PriorityWishGraphGroup";
   private static AtomicInteger index = new AtomicInteger(0);
   /**
    * @param gg
    */
   public JMXPriorityWishGraphGroup(PriorityQueueWishGraphGroup gg) {
      super(gg);
      pgg = gg;
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
      log.info("Max Number of Threads is " + this.getMaxThreads());
      log.info("Available Weight for this group is " + this.getAvailableWeight());
      log.info("Max Weight for this group is " + this.getMaxWeight());
      log.info("---------------------------------------------");
   }
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXGraphGroupMBean#getMaxThreads()
    */
   public long getMaxWeight() {
      return this.pgg.getMaxWeight();
   }
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXLocalGraphGroupMBean#setMaxThreads(int)
    */
   public void changeMaxWeight(long maxWeight) {
      log.info("Changing MAX WEIGHT to " + maxWeight);
      this.pgg.setMaxWeight(maxWeight);      
   }
   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.manager.JMXPriorirityWishGraphGroupMBean#getAvailableWeight()
    */
   public long getAvailableWeight() {
      return this.pgg.getAvailableWeight();
   }

	public String getMBeanName() {		
		return MBEAN_NAME + index.incrementAndGet();
	}

   
}
 