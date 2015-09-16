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
package br.com.auster.dware.monitor.dware;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import br.com.auster.common.jmx.AusterMBean;
import br.com.auster.common.text.DurationFormat;
import br.com.auster.dware.DataAware;

/**
 * <p><b>Title:</b> JMXDWareMBean</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXDWare implements AusterMBean, JMXDWareMBean {

   private static final String MBEAN_NAME = "type=DataAware,name=DataAware";  
   private static final String DATE_PATTERN = "yyyy/MM/dd.HH:mm:ss.S-z";
   
   private String name = MBEAN_NAME;
   private DataAware dware;
   private static final Logger log = Logger.getLogger(JMXDWare.class);

   /**
    * 
    */
   public JMXDWare(DataAware dware) {
      super();
      this.dware = dware;
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
    * @see br.com.auster.dware.jmx.JMXDWare#getEnqueuedRequests()
    */
   public long getEnqueuedRequests() {
      return this.dware.getEnqueuedRequests();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.jmx.JMXDWare#getDWareVersion()
    */
   public String getDWareVersion() {
      return DataAware.getVersion();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.dware.JMXDWare#getStartupTime()
    */
   public String getStartupTime() {
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);     
      return sdf.format(new Date(this.dware.getStartupTime()));
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.dware.JMXDWare#getUpTime()
    */
   public String getUpTime() {
     long duration = System.currentTimeMillis() - this.dware.getStartupTime();
     return DurationFormat.formatFromSeconds(duration / 1000);
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.dware.JMXDWareMBean#log()
    */
   public void log() {
      log.info("-----------------" + this.getMBeanName() + "----------------------------");
      log.info("Enqueued Requests=" + this.getEnqueuedRequests());
      log.info("Start Up Time=" + this.getStartupTime());
      log.info("Running Time=" + this.getUpTime());
      log.info("---------------------------------------------");
      
   }

}
 