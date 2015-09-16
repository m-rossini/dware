/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 03/09/2007
 */
package br.com.auster.dware.interrupt;

import java.rmi.RemoteException;
import java.util.Map;

import org.apache.log4j.Logger;

import br.com.auster.dware.Bootstrap;

/**
 * 
 * FOR THIS MAIN() TO RUN, YOU NEED TO PUT A license.bin FILE IN THE ROOT DIR. OF SRC/TEST/RESOURCES
 * 
 * @author framos
 * @version $Id$
 *
 */
public class InterruptTest  {

	private static final Logger log = Logger.getLogger(InterruptTest.class);
	
	public static final String CONF_FILE = "src/test/resources/br/com/auster/dware/interrupt/dware.xml";
	public static final String INPUT_FILE = "src/test/resources/br/com/auster/dware/interrupt/ACCOUNTS.TXT";
	
	
	public void testInterrupt() {
		
		System.getProperties().put("jdbc.drivers", 
								   "org.apache.commons.dbcp.PoolingDriver:oracle.jdbc.driver.OracleDriver");
		System.getProperties().put("br.com.auster.license.path", "src/test/resources/license.bin");
		
        String args[] = { "-x", CONF_FILE, 
        		          "-b", "default", 
        		          "-a", "{{bgh={{filenames="+ INPUT_FILE + "}}}}", 
        		          "-s",  "false",
        		          "-u", "20000000",
        		          "-o", "ASC" };
        
        try {
        	Interrupter interruper = new Interrupter();
        	interruper.start();
        	MyBootstrap boot = new MyBootstrap();
	        boot.executeIt(args);
	        log.info("Test case has finished.");
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}
	
	public static void main(String args[]) {
		new InterruptTest().testInterrupt();
	}
}


class MyBootstrap extends Bootstrap  {
	
	public MyBootstrap() throws RemoteException {}
	
	protected int check() { return 0; }
	public void executeIt(String args[]) throws Exception { 
		super.execute(args); 
	}
};


class Interrupter extends Thread {
	
	
	private static final Logger log = Logger.getLogger(Interrupter.class);
	
	public void run() {
		log.info("Starting interrupter");
		try {
			Thread.sleep(3000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread[] list = new Thread[10];
		Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();
		for (Thread t : threads.keySet()) {
			log.info("Looking @ thread " + t.getName());
			if (t.getName().indexOf("(local-1) #1") >= 0) {
				log.info("Interrupting thread...");
				t.interrupt();
				log.info("...done");
			}
		}
	}
}