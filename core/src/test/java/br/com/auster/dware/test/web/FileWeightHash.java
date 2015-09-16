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
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Random;

import br.com.auster.dware.request.net.SocketRequest;

/**
 * Class for weight cache.
 * 
 * @version $Id: FileWeightHash.java 2 2005-04-20 21:22:27Z rbarone $
 */
public class FileWeightHash {

  public HashMap fweiHash;

  private int[] counter;

  private Random rand;

  private final int NUMFILES = 10;

  public FileWeightHash(String rootPath) {
    fweiHash = new HashMap();
    counter = new int[NUMFILES];

    String textVal = "<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n<FONT SIZE=\"-2\"><FONT COLOR=#8080ff>Teste</FONT></FONT>\n";
    StringBuffer fcontent = new StringBuffer(textVal);
    String strAux;
    rand = new Random(NUMFILES);

    try {
      for (int i = 0; i < NUMFILES; i++) {
        File newfile = new File(rootPath + "/dwaretest" + i + ".html"); // the
                                                                          // file
                                                                          // name
        if (!newfile.exists()) {
          newfile.createNewFile();
          strAux = fcontent.toString();
          // if (i % 3 == 0)
          // fcontent.append(fcontent.toString());
          fcontent.append(fcontent.toString());
          String fval = "<HTML>\n" + strAux + "</HTML>\n";
          FileOutputStream fos = new FileOutputStream(newfile);
          fos.write(fval.getBytes());
          fos.close();
        }
        fweiHash.put(Integer.toString(i), new Long(newfile.length()));
        counter[i] = 0;
      }
    } catch (Exception e) {
      // log.debug("Error creating test files...");
      // log.debug(e);
      e.printStackTrace();
    }
  }

  /**
   * Updates request weight given the query string and file cache information.
   */
  public void addReqWei(SocketRequest sr, String qstr) {
    // System.out.println("qstr = "+qstr);

    /*
     * homogenea float reqIndexRand = rand.nextFloat(); float val = 0.9f; int i =
     * 0; for (i = 0; reqIndexRand < val && i < 9; i++) { val -= 0.1f; }
     */
    float reqIndexRand = rand.nextFloat();
    float val = 0.5f;
    int i = 0;
    for (i = 0; reqIndexRand < val && i < 9; i++) {
      val /= 1.5f;
    }

    String weiAux = Integer.toString(i);// (int)((NUMFILES-1)*rand.nextFloat()));

    long retF = ((Long) fweiHash.get(weiAux)).longValue();
    sr.setWeight(retF);
  }
}
