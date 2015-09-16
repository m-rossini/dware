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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ProtocolException;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.DataAware;
import br.com.auster.dware.filter.NIOFilter;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.net.SocketRequest;

/**
 * This filter is used to read a HTTP request and generate output to it.
 * 
 * @version $Id: HTTPRequest.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class HTTPRequest extends NIOFilter {

  protected static final String ROOT_PATH_ATTR = "root-dir";

  protected static final String NOT_FOUND_FILE_ATTR = "404-file";

  protected static final Map contentTypeMap = new HashMap();

  static {
    contentTypeMap.put("txt", "text/plain");
    contentTypeMap.put("java", "text/plain");
    contentTypeMap.put("htm", "text/html");
    contentTypeMap.put("html", "text/html");
    contentTypeMap.put("xml", "text/xml");
    contentTypeMap.put("zip", "application/x-zip");
    contentTypeMap.put("gz", "application/x-zip");
    contentTypeMap.put("jar", "application/x-zip");
    contentTypeMap.put("jpg", "image/jpeg");
    contentTypeMap.put("jpeg", "image/jpeg");
    contentTypeMap.put("gif", "image/gif");
    contentTypeMap.put("bmp", "image/bmp");
  }

  // Instance variables
  protected String rootPath;

  protected File file404;

  protected SimpleDateFormat dateFormat;

  protected SocketChannel channel;

  protected final Logger log = Logger.getLogger(this.getClass());

  private FileRequestHash freqhash;

  private final I18n i18n = I18n.getInstance(NIOFilter.class);

  public HTTPRequest(String name) {
    super(name);
    this.dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
  }

  /**
   * Configure this filter.
   */
  public synchronized void configure(Element config) throws FilterException {
    super.configure(config);
    // Specifies if the input file must be removed after processing (on commit)
    this.rootPath = DOMUtils.getAttribute(config, ROOT_PATH_ATTR, true);
    try {
      this.file404 = new File(DOMUtils.getAttribute(config, NOT_FOUND_FILE_ATTR, true));
    } catch (IllegalArgumentException e) {
      log.warn("No default file for 404 error defined.");
    }
    freqhash = new FileRequestHash();
  }

  int rcount;

  /**
   * Opens the file for reading as the input for the request to be processed.
   * 
   * @param request
   *          the request to be processed.
   */
  public void prepare(Request request) throws FilterException {
    File file = null;
    this.channel = ((SocketRequest) request).getSocket();
    Socket socket = channel.socket();
    this.writer = channel;

    rcount++;

    try {
      try {
        file = this.getFileFromHTTPRequest(socket, (SocketRequest) request);
        this.reader = NIOUtils.openFileForRead(file);

        /*
         * // delay for test int j = 0; int fileLen = (int)file.length()/1000;
         * for (int i = 0; i < fileLen; i++) j += i; try {
         * this.sleep((int)file.length()/10000); } catch (InterruptedException
         * ie) { }
         */

        log.info("The request " + rcount + " : " + request + " asked for the file "
                 + file.getAbsolutePath());
        this.sendHTTPHeader(socket, "200 OK", file);
      } catch (FileNotFoundException e) {
        log.error("The file " + file + " was not found for request " + request);
        if (this.file404 != null)
          this.reader = NIOUtils.openFileForRead(this.file404);
        else
          this.reader = null;
        this.sendHTTPHeader(socket, "404 Not Found", null);
      } catch (ProtocolException e) {
        log.error("The request " + request + " used a not implemented operation.", e);
        this.sendHTTPHeader(socket, "501 Not Implemented", null);
      }
    } catch (IOException e) {
      throw new FilterException(i18n.getString("problemOpenFile", request), e);
    }
  }

  protected File getFileFromHTTPRequest(Socket socket, SocketRequest sr) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

    for (String line; (line = reader.readLine()) != null;) {
      log.debug("Line read from socket: " + line);
      if (line.startsWith("GET ") || line.startsWith("PUT ")) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        tokenizer.nextToken(); // "GET"
        String path = tokenizer.nextToken();
        if (path.endsWith("/"))
          path += "index.html";

        // creates file of given request
        return freqhash.updateRequestWei(sr, path);
      }
    }
    throw new ProtocolException();
  }

  protected void sendHTTPHeader(Socket socket, String status, File file) throws IOException {
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

    writer.write("HTTP/1.0 " + status);
    writer.newLine();
    writer.write("Date: " + this.dateFormat.format(new Date()));
    writer.newLine();
    writer.write("Server: DataAware WebServer (" + DataAware.getVersion() + ")");
    writer.newLine();
    if (file != null) {
      String fileName = file.getName();
      writer.write("Content-length: " + file.length());
      writer.newLine();
      String fileExt = fileName.substring(fileName.lastIndexOf('.'), fileName.length());
      String contentType = (String) contentTypeMap.get(fileExt);
      if (contentType == null)
        contentType = "application/octet-stream";

      writer.write("Content-Type: " + contentType);
      writer.newLine();
    }
    writer.newLine();
    writer.flush();
  }

  /**
   * Closes the input.
   */
  public void commit() {
    this.rollback();
  }

  public void rollback() {
    super.rollback();
    try {
      this.channel.close();
    } catch (IOException e) {

    }
  }

  /**
   * Sets the input.
   */
  public final void setInput(String sourceName, Object input) throws ConnectException {
    throw new UnsupportedOperationException();
  }

  /**
   * Sets an output with name outputName.
   */
  public final void setOutput(String sinkName, Object output) throws ConnectException {
    throw new UnsupportedOperationException();
  }

  /*
   * Class for weight cache
   */
  class FileRequestHash {

    public HashMap fweiHash;

    private Random rand;

    private final int NUMFILES = 10;

    public FileRequestHash() {
      fweiHash = new HashMap();

      try {
        for (int i = 0; i < NUMFILES; i++) {
          File newfile = new File(rootPath + "/dawaretest" + i + ".html"); // the
                                                                            // file
                                                                            // name
          fweiHash.put(Long.toString(newfile.length()), newfile);
        }
      } catch (Exception e) {
        log.debug("Error creating test files...");
        log.debug(e);
      }
    }

    /**
     * Updates request weight given the query string and file cache information.
     */
    public File updateRequestWei(SocketRequest sr, String qstr) {
      // qstr.substring(qstr.lastIndexOf("wei=")+4,qstr.length());
      File retF = (File) fweiHash.get(Long.toString(sr.getWeight()));
      if (retF != null) {
        // sr.setWeight(retF.length());
        return retF;
      }
      // counter[Integer.parseInt(weiAux)]++;
      return new File(rootPath + qstr); // the file name
    }
  }
}
