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
* Created on 24/06/2005
*/
package br.com.auster.dware.filter;

import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.OutputKeys;

import org.apache.log4j.Logger;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.serializer.Serializer;
import org.apache.xml.serializer.SerializerFactory;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import br.com.auster.common.io.IOUtils;
import br.com.auster.common.io.NIOUtils;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * <p><b>Title:</b> TagSlicer</p>
 * <p><b>Description:</b> 
 * 
 * This class will receive SAX events and will find a configured Slice Tag.
 * The configured Tag when found in the startElement will signal de begin of a redirect of SAX Input/Output
 * , when the matching endElement for Slice tag is found, all data between the start and end, will then
 * be redirected to another output AND stripped off from the original stream
 * 
 * </p>
 * <p><b>Copyright:</b> Copyright (c) 2004-2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author mtengelm
 * @version $Id$
 */
public class XMLSlicer extends DefaultFilter implements ContentHandler {

   protected class SliceContext {
      private Serializer serial;
      private File file;
      private ContentHandler ch;
      private WritableByteChannel output;
      protected SliceContext() {         
      }
      protected Serializer getSerializer() {
         return this.serial;         
      }
      protected ContentHandler getContentHandler() {
         return this.ch;
      }
      protected void setSerializer(Serializer serial) {
         this.serial = serial;
         try {
            this.ch = serial.asContentHandler();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      public WritableByteChannel getOutput() {
        return this.output;
      }
      public void setOutput(WritableByteChannel output) {
        this.output = output;
      }
      public File getFile() {
         return file;
      }
      public void setFile(File file) {
         this.file = file;
      }
      public String getFileName() {
         return this.file.getName();
      }
      public String getPath() {
         return this.file.getAbsolutePath();
      }
   }
   
   private static final Logger log = Logger.getLogger(XMLSlicer.class);
   private LinkedList sliceContexts = new LinkedList();
   private String slicerTag = "";
   
   protected static final String SLICER_TAG = "slicer-tag";
   protected static final String DEFAULT_SLICER_TAG = "Slice";      
   protected static final String METHOD_ATTR = "method";
   protected static final String INDENT_ATTR = "indent";
   protected static final String INDENT_AMOUNT_ATTR = "indent-amount";
   protected static final String ENCODING_ATTR = "encoding";
   protected static final String FILENAME_ATTR = "fileName";
   protected static final String USE_DEFAULT_ATTR = "use-default";   
   protected static final String FORMAT_ATTR = "format";
   protected static final String GENERATE_MAP_ATTR = "generate-map";
   protected static final String GENERATE_MAP_KEY = "noFormat";   
   protected static final String MULTI_FILE = "multiFile";
   protected static final String BUNDLE_FILE = "bundleFile";
   protected static final String SLICE_SEQUENCE = "sliceSequence";
   protected static final String PREFIX_ELT = "filename-prefix";
   protected static final String SUFFIX_ELT = "filename-suffix";
   
   private Properties properties;
   private String fileNameAttribute;
   private boolean useDefault;
   private boolean slicing;
   private String formatName;
   private boolean isFormatActive;
   private boolean handleRequest;
   private Map generatedFiles;
   private Request request;
   private int sliceSequence;
   private boolean generateAlways;
   
   private FilenameBuilder prefixBuilder;
   private FilenameBuilder suffixBuilder;
   private String prefixName;
   private String suffixName;
        
   /**
    * 
    */
   public XMLSlicer(String filterName) {
      super(filterName);
   }


   /* (non-Javadoc)
    * @see br.com.auster.dware.graph.DefaultFilter#configure(org.w3c.dom.Element)
    */
   public synchronized void configure(Element config) throws FilterException {
      
      //Handle Slice Tag
      slicerTag = DOMUtils.getAttribute(config, SLICER_TAG, false);
      if ((slicerTag == null) || (slicerTag.equals(""))) {
         slicerTag = DEFAULT_SLICER_TAG;
      }
     
      //Handle UnSliced Data           
      this.useDefault = DOMUtils.getBooleanAttribute(config, USE_DEFAULT_ATTR);

      //Handle File Name Attribute
      this.fileNameAttribute="";
      this.fileNameAttribute = DOMUtils.getAttribute(config, FILENAME_ATTR, true);      
      
      //Handle format parameter
      // If the parameter is not present or if it is empty, we assume there is no format active for
      // the current filter processing. It means the filter will handle the input normally for all events.
      // On the other side, if the format is specified the format is active and the filter will check
      // if the request needs to be processed for the informed format.
      this.isFormatActive = true;
      this.formatName = DOMUtils.getAttribute(config, FORMAT_ATTR, false);
      if (this.formatName.equals("")) {
         this.isFormatActive = false;
      }
      
      //Handle generate-map parameter.
      //The Map will generated if FORMAT parameter is specified and the current request asks for this FORMAT.
      //Otherwise the Map generation will obey generate-map parameter specs, below.
      //This parameters tells the filter to generate the Map of Generated Files if the
      //parameter format was not specified.
      //Note that if the format parameter has being specified, but the request does not requests this
      // format to be generated, then the filter will not generate files, thus this parameters does not apply
      this.generateAlways = DOMUtils.getBooleanAttribute(config, GENERATE_MAP_ATTR);
      
      //Handle Output Filename Prefix and Suffix.
      this.prefixBuilder = new FilenameBuilder(DOMUtils.getElement(config, PREFIX_ELT, false));
      this.suffixBuilder = new FilenameBuilder(DOMUtils.getElement(config, SUFFIX_ELT, false));
      
      //Handle Serializer output properties
      this.properties = OutputPropertiesFactory.getDefaultMethodProperties(DOMUtils.getAttribute(
            config, METHOD_ATTR, true));
      this.properties.setProperty(OutputKeys.INDENT, DOMUtils.getBooleanAttribute(config,
            INDENT_ATTR) ? "yes" : "false");
      this.properties.setProperty(OutputKeys.ENCODING, DOMUtils.getAttribute(config,
            ENCODING_ATTR, true));
      try {
         this.properties.setProperty(
               OutputPropertiesFactory.S_KEY_INDENT_AMOUNT,
               Integer.toString(DOMUtils.getIntAttribute(config, INDENT_AMOUNT_ATTR, true)));
      } catch (IllegalArgumentException e) {
      }
      
   }
   /*
    * (non-Javadoc)
    * 
    * @see br.com.auster.dware.graph.DefaultFilter#prepare(br.com.auster.dware.graph.Request)
    */
   public void prepare(Request request) throws FilterException {
      this.handleRequest=true;
      this.request = request;  
      this.sliceSequence = 0;
      
      if (this.isFormatActive) {
         Map formatsMap = request.getAttributes();
         if (formatsMap == null) {
            this.handleRequest=false;
         } else {
            List formatsList = (List) request.getAttributes().get(FORMAT_ATTR);
            if (formatsList == null) {
               this.handleRequest=false;  
            } else {
               if (!formatsList.contains(this.formatName)) {
                  this.handleRequest=false;
               }
            }
         }
         if (!this.handleRequest) {
            log.warn("Format is Active, howhever the request does not support the current format. Current Format is " + this.formatName);
         }
      }
      
      this.prefixName = this.prefixBuilder.getFilename(request);
      this.suffixName = this.suffixBuilder.getFilename(request);
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.graph.DefaultFilter#rollback()
    */
   public void rollback() {   
      this.commit();
   }

   /* (non-Javadoc)
    * @see br.com.auster.dware.graph.DefaultFilter#commit()
    */
   public synchronized void commit() {
     this.prefixName = null;
     this.suffixName = null;
     
      if (!this.handleRequest) {
         return;
      }
      if (this.sliceContexts.size() > 0) {
         log.warn("Potential problem on XML file. On commit the context queue should have only the default context and has "
               + this.sliceContexts.size() + " elements.");
      }
      this.sliceContexts.clear();

      if (this.generateAlways || this.isFormatActive) {
         Map gf;
         if (request.getAttributes().containsKey(OutputToFile.GENERATED_FILES_KEY)) {
            gf = (Map) request.getAttributes().get(OutputToFile.GENERATED_FILES_KEY);
         } else {
            gf = new HashMap();            
         }
         String name = (this.formatName.length() == 0) ? GENERATE_MAP_KEY : this.formatName;
         gf.put(name, this.generatedFiles);
         request.getAttributes().put(OutputToFile.GENERATED_FILES_KEY, gf);         
      }
   }
   
   public final Object getInput(String sourceName) {
     return this;
   }

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
    */
   public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if (!this.handleRequest) {
         return;
      }
      if (!this.slicing) {
         if (this.useDefault) {
            this.slicing = true;
            this.sliceContexts.addLast(buildContext(atts.getValue(this.fileNameAttribute)));
            SliceContext sc = (SliceContext) this.sliceContexts.getLast();
            sc.getContentHandler().startDocument();                     
         }
      }
      SliceContext sc;
      if (localName.equals(this.slicerTag)) {
         this.sliceSequence++;
         this.slicing=true;     
         this.sliceContexts.addLast(buildContext(atts.getValue(this.fileNameAttribute)));
      }

      if (this.sliceContexts.size() == 0) {
         return;
      }
      
      sc = (SliceContext) this.sliceContexts.getLast();
      if (localName.equals(this.slicerTag)) {
         sc.getContentHandler().startDocument();
      }
      sc.getContentHandler().startElement(uri, localName, qName, atts);
   }

   protected SliceContext buildContext(String fileName) { 
      File fileObj = new File(this.prefixName + fileName + this.suffixName);
      
      WritableByteChannel out = null;
      try {
        IOUtils.createParentDirs(fileObj);
        out = NIOUtils.openFileForWrite(fileObj, false);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      
      //Stores the generated file
      Map attrMap = new HashMap();
      attrMap.put(MULTI_FILE, Boolean.TRUE);
      attrMap.put(BUNDLE_FILE, Boolean.FALSE);
      attrMap.put(SLICE_SEQUENCE, new Integer(sliceSequence));     
      
      this.generatedFiles.put(fileObj.getAbsolutePath(), attrMap);
      
      //Creates the Context
      Serializer serial = SerializerFactory.getSerializer(this.properties);
      serial.setOutputStream(Channels.newOutputStream(out));
      SliceContext sc = new SliceContext();
      sc.setSerializer(serial);
      sc.setFile(fileObj);
      sc.setOutput(out);
      
      return sc;
   }
   
   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
    */
   public void endElement(String uri, String localName, String qName) throws SAXException {
      if (!this.handleRequest) {
         return;
      }
      if (this.sliceContexts.size() == 0) {
         return;
      }
      
      SliceContext sc = (SliceContext) this.sliceContexts.getLast();      
      sc.getContentHandler().endElement(uri, localName, qName); 
      
      if (localName.equals(this.slicerTag)) {
         sc.getContentHandler().endDocument();
         this.sliceContexts.removeLast();
         
         // close last stream (essential since output can be buffered)
         if (sc.getOutput() != null && sc.getOutput().isOpen()) {
           try {
             sc.getOutput().close();
           } catch (IOException e) {
             throw new RuntimeException("Problems while trying to close output file " + 
                                         sc.getFile().getPath(), e);
           }
         }
      }
   }
   
   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#characters(char[], int, int)
    */
   public void characters(char[] ch, int start, int length) throws SAXException {
      if (!this.handleRequest) {
         return;
      }      
      if (this.sliceContexts.size() == 0) {
         return;
      }      
      SliceContext sc = (SliceContext) this.sliceContexts.getLast();      
      sc.getContentHandler().characters(ch, start, length);
   }   
   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
    */
   public void setDocumentLocator(Locator locator) {   }

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#startDocument()
    */
   public void startDocument() throws SAXException {
      if (!this.handleRequest) {
         return;
      }      
      this.slicing=false;   
      this.generatedFiles = new LinkedHashMap();
   }

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#endDocument()
    */
   public void endDocument() throws SAXException {
      if (!this.handleRequest) {
         return;
      }      
      if (this.sliceContexts.size() == 0) {
         return;
      }      
      SliceContext sc = (SliceContext) this.sliceContexts.getLast(); 
      sc.getContentHandler().endDocument();
   }

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
    */
   public void startPrefixMapping(String prefix, String uri) throws SAXException {   }

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
    */
   public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {}

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
    */
   public void processingInstruction(String target, String data) throws SAXException {}

   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
    */
   public void skippedEntity(String name) throws SAXException {}
   
   /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
    */
   public void endPrefixMapping(String prefix) throws SAXException {   }
   
}
 