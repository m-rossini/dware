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
package br.com.auster.dware.filter;

import gnu.trove.TObjectLongHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.graph.ThreadedFilter;
import br.com.auster.dware.request.file.PartialFileListRequest;
import br.com.auster.dware.request.file.PartialFileRequest;

/**
 * This filter is used to get pieces of data from some files and write them to
 * the output.
 * 
 * @version $Id: PartialInputFromFileList.java 232 2006-08-18 15:04:12Z rbarone $
 */
public class PartialInputFromFileList extends ThreadedFilter {

  protected static final String BUFFER_SIZE_ATTR = "buffer-size";
  protected static final String MAX_CACHE_SIZE_ATTR = "file-cache-size";
  
  /**
   * LRU Cache for <code>ReadableByteChannel</code> objects that will close
   * the channel when an entry is removed or the map is cleared.
   * 
   * <p>
   * This implementation does NOT check the type of each inserted value.
   * Therefore, you must guarantee that each value is a
   * <code>ReadableByteChannel</code> before adding it to this cache.
   * </p>
   * 
   * <p><b>Title:</b> ChannelLRUMap </p>
   * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
   * <p><b>Company:</b> Auster Solutions</p>
   * 
   * @author rbarone
   * @version $Id: PartialInputFromFileList.java 232 2006-08-18 15:04:12Z rbarone $
   */
  private class ChannelLRUMap extends LRUMap {
    public ChannelLRUMap() {
      super();
    }
    public ChannelLRUMap(int maxSize, boolean scanUntilRemovable) {
      super(maxSize, scanUntilRemovable);
    }
    public ChannelLRUMap(int maxSize, float loadFactor, boolean scanUntilRemovable) {
      super(maxSize, loadFactor, scanUntilRemovable);
    }
    public ChannelLRUMap(int maxSize, float loadFactor) {
      super(maxSize, loadFactor);
    }
    public ChannelLRUMap(int maxSize) {
      super(maxSize);
    }
    public ChannelLRUMap(Map map, boolean scanUntilRemovable) {
      super(map, scanUntilRemovable);
    }
    public ChannelLRUMap(Map map) {
      super(map);
    }
    protected void removeEntry(HashEntry entry, int hashIndex, HashEntry previous) {
      ReadableByteChannel reader = (ReadableByteChannel) entry.getValue();
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
          log.error(e.getMessage(), e);
        }
      }
      super.removeEntry(entry, hashIndex, previous);
    }
    public void clear() {
      HashEntry[] data = this.data;
      for (int i = data.length - 1; i >= 0; i--) {
        if (data[i] == null) {
          continue;
        }
        ReadableByteChannel reader = (ReadableByteChannel) data[i].getValue();
        if (reader != null) {
          try {
            reader.close();
          } catch (IOException e) {
            log.error(e.getMessage(), e);
          }
        }
      }
      super.clear();
    }
  }

  // The instance variables
  protected PartialFileRequest[] requestArray;

  protected WritableByteChannel writer;

  protected ByteBuffer bb;
  
  protected Map readerByFileName = null;

  protected final TObjectLongHashMap offsetByFileName = new TObjectLongHashMap();

  protected static final Logger log = Logger.getLogger(PartialInputFromFileList.class);

  private final I18n i18n = I18n.getInstance(PartialInputFromFileList.class);
  
  private boolean isCacheValid = false; 

  public PartialInputFromFileList(String name) {
    super(name);
  }

  /**
   * Configures this filter
   */
  public synchronized void configure(Element config) throws FilterException {
    this.bb = ByteBuffer.allocateDirect(DOMUtils.getIntAttribute(config, BUFFER_SIZE_ATTR, true));
    final int maxCacheSize = DOMUtils.getIntAttribute(config, MAX_CACHE_SIZE_ATTR, true);
    if (maxCacheSize < 1) {
      this.readerByFileName = new ChannelLRUMap(1);
    } else {
      this.readerByFileName = new ChannelLRUMap(maxCacheSize);
    }
  }

  public void prepare(Request request) throws FilterException {  
    if (request instanceof PartialFileRequest) {
      this.requestArray = new PartialFileRequest[] { (PartialFileRequest) request };
    } else if (request instanceof PartialFileListRequest) {
      this.requestArray = ((PartialFileListRequest) request).getFiles();
    } else {
      throw new IllegalArgumentException(i18n.getString("invalidRequestType", request.getClass()
          .getName()));
    }
    this.isCacheValid = true;
  }

  /**
   * Finds the offset data and writes to the output.
   */
  public final void process() throws FilterException {
    try {
      // Writes to the output only the piece of data that this request
      // represents
      for (int i = 0; i < this.requestArray.length; i++) {
        PartialFileRequest request = requestArray[i];
        log.debug("Processing input file: " + request.getFile().getAbsolutePath());
        this.printHeader(request, this.writer);
        this.printBody(request, this.writer);
        this.printFooter(request, this.writer);
      }
    } catch (IOException e) {
      // Clears the input file cache, just in case.
      this.clearFiles();
      throw new FilterException(i18n.getString("problemWriteOutput"), e);
    } finally {
      try {
        writer.close();
      } catch (IOException e) {
        throw new FilterException(i18n.getString("problemClosingWriter"), e);
      }
    }
  }

  /**
   * Prints a header to the output buffer based on this partial file request.
   * This implementation do nothing.
   */
  protected void printHeader(PartialFileRequest request, WritableByteChannel writer)
      throws IOException {
  }

  /**
   * Prints a footer to the output buffer based on this partial file request.
   * This implementation do nothing.
   */
  protected void printFooter(PartialFileRequest request, WritableByteChannel writer)
      throws IOException {
  }

  /**
   * Sends to the writer the specified piece of data from the reader.
   */
  private final void printBody(PartialFileRequest request, WritableByteChannel writer)
      throws IOException {
    final ReadableByteChannel reader = getReaderByFileName(request.getFile(), request.getOffset(),
                                                           request.getLength());
    long bytesWritten = 0, toWrite;
    final ByteBuffer bb = this.bb;

    while ((toWrite = request.getLength() - bytesWritten) > 0) {
      if (!this.isCacheValid) {
        throw new IllegalStateException("File cache is invalid - maybe this filter has just rolled back.");
      }
      
      if (toWrite < bb.capacity()) {
        bb.limit((int) toWrite);
      }
      
      if (reader.read(bb) >= 0) {
        bb.flip();
      } else if (toWrite > bb.remaining()) {
        throw new IOException(i18n.getString("readDataNotEnough"));
      }
      
      try { 
        bytesWritten += writer.write(bb);
        bb.compact();
      } catch (ClosedChannelException e) {
        log.error("Writer has been closed elsewhere - ignoring remaining content of [" 
                  + toWrite + "] bytes.", e);
        break;
      } catch (IOException e) {
        log.error("Error while accessing Writer - ignoring remaining content of [" 
                  + toWrite + "] bytes.", e);
        break;
      }
    }
    bb.clear();
  }

  /**
   * Returns a reader for a given file, starting at the offset defined.
   * 
   * @param file
   *          the file to be read.
   * @param offset
   *          the offset from the beginning of the file to discard before
   *          starting reading.
   * @param length
   *          the length to be read from the reader.
   */
  private final ReadableByteChannel getReaderByFileName(File file, long offset, long length)
      throws IOException {
    ReadableByteChannel reader = null;
    
    synchronized (this.readerByFileName) {
      if (!this.isCacheValid) {
        throw new IllegalStateException("File cache is invalid - maybe this filter has just rolled back.");
      }
      
      // Gets the offset of the reader offset to the request offset
      reader = (ReadableByteChannel) this.readerByFileName.get(file);
      long bytesIgnored = this.offsetByFileName.get(file);
      long toIgnore;
      final ByteBuffer bb = this.bb;
  
      // If the reader passed the offset, open it again. If the reader is not
      // opened for this file, open it.
      if (reader == null || offset < bytesIgnored || !reader.isOpen()) {
        if (reader != null) {
          reader.close();
        }
        reader = NIOUtils.openFileForRead(file);
        this.readerByFileName.put(file, reader);
        bytesIgnored = 0;
      }
  
      // Ignores the offset data
      if (bytesIgnored < offset && reader instanceof FileChannel) {
        ((FileChannel)reader).position(offset);
      } else {
        while (bytesIgnored < offset) {
          toIgnore = offset - bytesIgnored;
          if (toIgnore < bb.capacity()) {
            bb.limit((int) toIgnore);
          }

          if ((bytesIgnored += reader.read(bb)) >= 0) {
            bb.flip().clear();
          }
        }
      }
      this.offsetByFileName.put(file, offset + length);
    }

    return reader;
  }

  /**
   * Sets the writer for this filter, for not using a pipe.
   */
  public final void setOutput(String sinkName, Object output) {     
    this.writer = (WritableByteChannel) output;   
  }

  public final void commit() {
    try {
      if (this.writer != null && this.writer.isOpen()) {
        this.writer.close();
      }
    } catch (IOException e) {
      log.fatal(e.getMessage(), e);
    }
  }

  /**
   * Closes the output.
   */
  public final void rollback() {
    commit();
    clearFiles();
  }

  protected void clearFiles() {
    synchronized (this.readerByFileName) {
      this.readerByFileName.clear();
      this.offsetByFileName.clear();
      this.isCacheValid = false;
    }
  }
  
  protected void finalize() throws Throwable {
    this.clearFiles();
  }
 
}
