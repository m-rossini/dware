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
 * Created on Apr 8, 2005
 */
package br.com.auster.dware.test;

import java.io.File;
import java.io.FileWriter;
import java.util.Random;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.cli.CLOption;
import br.com.auster.common.cli.OptionsParser;
import br.com.auster.common.util.ConfigUtils;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;

/**
 * @version $Id: DataAwareTest.java 356 2008-02-15 13:12:36Z lmorozow $
 */
public class DataAwareTest {

  public static final String SQL_CONF_PARAM = "sql-conf";

  public static final String CONF_PARAM = "xml-conf";

  protected static final CLOption[] options = {
      new CLOption(SQL_CONF_PARAM, 's', false, true, "file", "the SQL configuration file"),
      new CLOption(CONF_PARAM, 'x', true, true, "file", "the XML configuration file") };

  /**
   * Gets the XML configuration given by the argument "xml-conf".
   * 
   * @param args
   *          the command line arguments.
   */
  public static Element getXMLConfig(String[] args) throws Exception {
    OptionsParser parser = new OptionsParser(options, DataAwareTest.class, "DataAware Test", true);
    parser.parse(args);

    // Parses the XML config file
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    File configFile = new File(OptionsParser.getOptionValue(CONF_PARAM));
    return dbf.newDocumentBuilder().parse(configFile).getDocumentElement();
  }

  /**
   * Creates requests that do nothing more than stop the graphs for a while.
   * 
   * @param args
   *          the command line arguments.
   */
  public static void createRequests(DataAware dware, Element config, int randSeed) {
    config = DOMUtils.getElement(config, "wait-request", true);
    int quant = DOMUtils.getIntAttribute(config, "quantity", true);
    int maxWait = DOMUtils.getIntAttribute(config, "max-wait", true);

    Random rand = new Random(randSeed);

    int initQuant = quant;// (int)Math.round(0.3*quant);

    WaitRequest[] requests = new WaitRequest[initQuant];
    for (int i = 0; i < initQuant; i++) {
      long waitTime = Math.round(rand.nextFloat() * maxWait);
      int hcode = i;// Math.round(rand.nextFloat() * quant);
      // if (i % 10 == 0)
      // waitTime += 400;
      // if (i % 5 == 0)
      // waitTime += 200;
      requests[i] = new WaitRequest(waitTime, hcode);
    }

    for (int i = 0; i < initQuant; i++) {
      dware.enqueue(requests[i]);
    }

    /*
     * initQuant = quant - initQuant; // create requests sleeping time for (int
     * i = 0; i < initQuant; i++) { try { // sleeps 2 seconds max
     * Thread.sleep(Math.round(rand.nextFloat() * 2000)); } catch
     * (InterruptedException ie) { } long waitTime = Math.round(rand.nextFloat() *
     * maxWait); if (i % 10 == 0) waitTime += 50000; requests = new
     * WaitRequest(waitTime); dware.enqueue(requests); }
     */

  }

  private static FileWriter logFile = null;

  public static void fileLog(String msg) {
    try {
      if (logFile == null) {
        File faux = new File("C:/logTest.txt");
        faux.createNewFile();
        logFile = new FileWriter(faux);
      }
      logFile.write(msg);
      logFile.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static FileWriter logFile2 = null;

  public static void fileLog2(String msg) {
    try {
      if (logFile2 == null) {
        File faux = new File("C:/meanTest.txt");
        faux.createNewFile();
        logFile2 = new FileWriter(faux);
      }
      logFile2.write(msg);
      logFile2.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static FileWriter logFile3 = null;

  public static void fileLog3(String msg) {
    try {
      if (logFile3 == null) {
        File faux = new File("C:/failTestLog.txt");
        faux.createNewFile();
        logFile3 = new FileWriter(faux);
      }
      logFile3.write(msg);
      logFile3.flush();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) throws Exception {
    Element config = getXMLConfig(args);

    // Configures the Log4J library
    ConfigUtils.configureLog4J(config);

    // Creates the DataAware instance
    config = DOMUtils.getElement(config, DataAware.DWARE_NAMESPACE_URI,
                                 DataAware.CONFIGURATION_ELEMENT, true);
    DataAware dware = new DataAware(config);

    long initTest = System.currentTimeMillis();

    createRequests(dware, config, 10);

    long diff = System.currentTimeMillis() - initTest;

    Logger.getLogger(dware.getClass()).info("*** Test time = " + diff);

    // Gets the requests to be processed
    // Request[] requests = getRequests(config);
    // for (int i = 0; i < requests.length; i++)
    // dware.enqueue(requests[i]);

    Logger.getLogger(dware.getClass()).info("*** sleep starts....");
    // Thread.sleep(300000);

    Logger.getLogger(dware.getClass()).info("*** shutdown....");
    dware.shutdown(true);
    // System.exit(ok?0:1);

    Logger.getLogger(dware.getClass()).info("*** Test time = " + diff);
  }
}
