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
package br.com.auster.dware.test.web;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.cli.CLOption;
import br.com.auster.common.cli.OptionsParser;
import br.com.auster.common.util.ConfigUtils;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.request.net.SocketRequest;

/**
 * TODO class comments
 * 
 * @version $Id: DataAwareWebServer.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class DataAwareWebServer {

  public static final String CONF_PARAM = "xml-conf";

  protected static final String ROOT_PATH_ATTR = "root-dir";

  protected static final CLOption[] options = { new CLOption(CONF_PARAM, 'x', true, true, "file",
      "the XML configuration file") };

  /**
   * Gets the XML configuration given by the argument "xml-conf".
   * 
   * @param args
   *          the command line arguments.
   */
  public static Element getXMLConfig(String[] args) throws Exception {
    OptionsParser parser = new OptionsParser(options, DataAwareWebServer.class,
        "DataAware Web Server (" + DataAware.getVersion() + ")", true);
    parser.parse(args);

    // Parses the XML config file
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    File configFile = new File(OptionsParser.getOptionValue(CONF_PARAM));
    return dbf.newDocumentBuilder().parse(configFile).getDocumentElement();
  }

  /**
   * Starts to listen a net port, waiting for HTTP requests.
   * 
   * @param args
   *          the command line arguments.
   */
  public static void startWebServer(DataAware dware, Element config) {
    Logger log = Logger.getLogger(DataAwareWebServer.class);
    config = DOMUtils.getElement(config, "server", true);
    int port = DOMUtils.getIntAttribute(config, "port", true);
    String rootPath = DOMUtils.getAttribute(config, ROOT_PATH_ATTR, true);

    try {
      ServerSocketChannel server = ServerSocketChannel.open();
      // server.configureBlocking(false);
      server.socket().bind(new InetSocketAddress(port));
      log.info("Accepting connections on port " + port);

      int reqNum = 0;
      FileWeightHash fwei = new FileWeightHash(rootPath);
      while (true) {
        SocketChannel socket = server.accept();
        SocketRequest request = new SocketRequest(socket);
        reqNum++;
        fwei.addReqWei(request, "");
        // System.out.println("lye: "+reqNum);
        dware.enqueue(request);
      }
    } catch (IOException e) {
      log.error("An error ocurred in the server socket.", e);
    }
  }

  public static void main(String[] args) throws Exception {
    Element config = getXMLConfig(args);

    // Configures the Log4J library
    ConfigUtils.configureLog4J(config);

    config = DOMUtils.getElement(config, DataAware.DWARE_NAMESPACE_URI,
                                 DataAware.CONFIGURATION_ELEMENT, true);
    DataAware dware = new DataAware(config);
    try {
      // Creates the DataAware instance
      startWebServer(dware, config);
    } finally {
      dware.shutdown(true);
    }
  }
}
