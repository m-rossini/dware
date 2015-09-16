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
* Created on 17/06/2005
*/
package br.com.auster.dware.monitor.graph;

import java.text.MessageFormat;

import org.apache.log4j.Logger;

import br.com.auster.common.jmx.AusterMBean;
import br.com.auster.dware.manager.GraphManager;
import br.com.auster.dware.monitor.manager.JMXGraphGroupCounter;

/**
 * <p><b>Title:</b> JMXGraphMBean</p>
 * <p><b>Description:</b> </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class JMXGraph implements AusterMBean, JMXGraphMBean {

   private String name = MBEAN_NAME;	
   private static final Logger log = Logger.getLogger(JMXGraph.class);
   private static final String MBEAN_NAME = "type=DataAware,name=Graph";
   private GraphManager graphManger;
   
   

   /**
    * 
    */
   public JMXGraph(GraphManager _gm) {
      super();
	  this.graphManger = _gm;
   }

   public long getFinishedRequestsCounter() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.FINISHED_REQUEST_COUNT);
//      return Graph.getFinishedRequestsCounter();
   }

   public long getFinishedRequestsTimer() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.TOTAL_PROCESSING_TIME);
//      return Graph.getFinishedRequestsTimer();
   }

   public long getAverageRequestsPerSecond() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   try {
		   return (long) (getFinishedRequestsCounter() / 
		                  (getFinishedRequestsTimer() / 1000));
	   } catch (ArithmeticException ae) {
		   return 0;
	   }
//      return Graph.getFinishedRequestsCounter() / (Graph.getFinishedRequestsTimer() / 1000);
   }

   public long getAverageMilliSecondsPerRequest() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   try {
		   return (long) (getFinishedRequestsTimer() / 
		                  getFinishedRequestsCounter());
	   } catch (ArithmeticException ae) {
		   return 0;
	   }
//      return Graph.getFinishedRequestsTimer() / Graph.getFinishedRequestsCounter();
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
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getRolledBackRequestsCounter()
    */
   public long getRolledBackRequestsCounter() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.ROLLEDBACK_REQUEST_COUNT);
//      return Graph.getRolledBackRequestsCounter();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getAverageWeight()
    */
   public long getAverageWeight() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   try {
		   return (long) (getTotalWeight() / 
		                  getFinishedRequestsCounter());
	   } catch (ArithmeticException ae) {
		   return 0;
	   }
//      return Graph.getTotalWeight() / Graph.getFinishedRequestsCounter();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getTotalWeight()
    */
   public long getTotalWeight() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.TOTAL_WEIGHT);
//      return Graph.getTotalWeight();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getLargestWeight()
    */
   public long getLargestWeight() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_WEIGHT);
//      return Graph.getLargestWeight();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getSmallestWeight()
    */
   public long getSmallestWeight() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_WEIGHT);
//      return Graph.getSmallestWeigth();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getLargestTimer()
    */
   public long getLargestTimer() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.LARGEST_PROCESSED_TIME);	   
//      return Graph.getLargestTimer();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#getSmallestTimer()
    */
   public long getSmallestTimer() {
	   if (graphManger.getJMXCounter() == null) { return -1; }
	   return graphManger.getJMXCounter().getCounter(JMXGraphGroupCounter.SMALLEST_PROCESSED_TIME);
//      return Graph.getSmallestTimer();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.monitor.graph.JMXGraphMBean#log()
    */
   public void log() {
      log.info("-----------------" + this.getMBeanName() + "----------------------------");
      log.info("|Finished requests counter=" + this.getFinishedRequestsCounter());
      log.info("|RolledBack requests counter=" + this.getRolledBackRequestsCounter());
      log.info("|Total Graph Processing Time=" + this.getFinishedRequestsTimer());
      log.info("|Average MilliSeconds per Request=" + this.getAverageMilliSecondsPerRequest());
      log.info("|Average Requests per Second=" + this.getAverageRequestsPerSecond());
      log.info("|Total Weight of Finished Requests=" + this.getTotalWeight());
      log.info("|Average Weight of Finished Requests=" + this.getAverageWeight());
      log.info("|Largest Timer of Finished Requests=" + this.getLargestTimer());
      log.info("|Smallest Timer of Finished Requests=" + this.getSmallestTimer());      
      log.info("|Largest Weight of Finished Requests=" + this.getLargestWeight());
      log.info("|Smallest Weight of Finished Requests=" + this.getSmallestWeight());
      log.info("---------------------------------------------");
   }

}
 