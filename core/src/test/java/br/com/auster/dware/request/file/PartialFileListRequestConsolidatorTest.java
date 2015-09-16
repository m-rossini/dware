package br.com.auster.dware.request.file;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.request.RequestFilter;

/**
 * <p><b>Title:</b> PartialFileListRequestConsolidatorTest</p>
 * <p><b>Description:</b> Unit test to PartialFileListRequestConsolidatorTest</p>
 * <p><b>Copyright:</b> Copyright (c) 2005</p>
 * <p><b>Company:</b> Auster Solutions</p>
 *
 * @author etirelli
 * @version $Id: PartialFileListRequestConsolidatorTest.java 336 2007-12-26 13:14:17Z mtengelm $
 */
public class PartialFileListRequestConsolidatorTest extends TestCase {
  private PartialFileListRequestConsolidator builder;
  private final int NUMBER_OF_KEYS = 10;
  private final int PARTS_PER_KEY  = 3;
  

  protected void setUp() throws Exception {
    super.setUp();
  }

  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testCreateRequestWithNull() {
    try {
      this.builder = new PartialFileListRequestConsolidator("TestBuilder", null);
      builder.createRequests(null);
      Assert.fail("The builder is supposed to throw an exception when createRequest(Map) is called");
    } catch(UnsupportedOperationException e) {
      // working fine
    }
  }
  
  public void testCreateRequestNoConfig() {
    try {
      this.builder = new PartialFileListRequestConsolidator("TestBuilder", null);
      List requests = new ArrayList();
      int total = NUMBER_OF_KEYS * PARTS_PER_KEY;
      for(int i = 0; i<total; i++) {
        PartialFileRequest request = new PartialFileRequest(
            "key"+(i%NUMBER_OF_KEYS), 0, 100, new File("file"+i));
        requests.add(request);
      }
      RequestFilter filter = new DummyFilter(requests);
      
      RequestFilter newFilter = builder.createRequests(filter, null);
      
      Assert.assertSame("builder is not supposed to instantiate a new filter", filter, newFilter);
      Assert.assertEquals("Wrong number of consolidated requests", 
          NUMBER_OF_KEYS, filter.getAcceptedRequests().size());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Builder was not supposed to throw any exception");
    }
  }

  public void testCreateRequestAllAttributesEqual() {
    String configStr = 
     "<partial-file-list-config default-compare-method='equals' > " +
     " <use-request-attribute name='att1' compare-method='identity'/> " +
     " <use-request-attribute name='att2' compare-method='equals'/> " +
     " <use-request-attribute name='att3' compare-method='string'/> " +
     " <use-request-attribute name='att4' /> " +
     "</partial-file-list-config>";

    Element config = this.parseXML(configStr);
    
    try {
      this.builder = new PartialFileListRequestConsolidator("TestBuilder", config);
      List requests = new ArrayList();
      int total = NUMBER_OF_KEYS * PARTS_PER_KEY;
      final Integer att1 = new Integer(1);
      final Integer att3 = new Integer(3);
      for(int i = 0; i<total; i++) {
        PartialFileRequest request = new PartialFileRequest(
            "key"+(i%NUMBER_OF_KEYS), 0, 100, new File("file"+i));
        Map attributes = new HashMap();
        attributes.put("att1", att1);
        attributes.put("att2", new Integer(2));
        attributes.put("att3", att3);
        attributes.put("att4", new Integer(4));
        request.setAttributes(attributes);
        requests.add(request);
      }
      RequestFilter filter = new DummyFilter(requests);
      
      RequestFilter newFilter = builder.createRequests(filter, null);
      
      Assert.assertSame("builder is not supposed to instantiate a new filter", filter, newFilter);
      Assert.assertEquals("Wrong number of consolidated requests", 
          NUMBER_OF_KEYS, filter.getAcceptedRequests().size());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Builder was not supposed to throw any exception");
    }
  }

  public void testCreateRequestDifferentAttributes() {
    String configStr = 
     "<partial-file-list-config default-compare-method='equals' > " +
     " <use-request-attribute name='att1' compare-method='identity'/> " +
     " <use-request-attribute name='att2' compare-method='equals'/> " +
     " <use-request-attribute name='att3' compare-method='string'/> " +
     " <use-request-attribute name='att4' /> " +
     "</partial-file-list-config>";

    Element config = this.parseXML(configStr);
    
    try {
      this.builder = new PartialFileListRequestConsolidator("TestBuilder", config);
      List requests = new ArrayList();
      int total = NUMBER_OF_KEYS * PARTS_PER_KEY;
      final Integer att3 = new Integer(3);
      for(int i = 0; i<total; i++) {
        PartialFileRequest request = new PartialFileRequest(
            "key"+(i%NUMBER_OF_KEYS), 0, 100, new File("file"+i));
        Map attributes = new HashMap();
        attributes.put("att1", new Integer(i));
        attributes.put("att2", new Integer(2));
        attributes.put("att3", att3);
        attributes.put("att4", new Integer(4));
        request.setAttributes(attributes);
        requests.add(request);
      }
      RequestFilter filter = new DummyFilter(requests);
      
      RequestFilter newFilter = builder.createRequests(filter, null);
      
      Assert.assertSame("builder is not supposed to instantiate a new filter", filter, newFilter);
      Assert.assertEquals("Wrong number of consolidated requests", 
          total, filter.getAcceptedRequests().size());
    } catch (Exception e) {
      e.printStackTrace();
      Assert.fail("Builder was not supposed to throw any exception");
    }
  }

  private final Element parseXML(String xml) {
    StringReader reader = new StringReader(xml);  
    try {     
      DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
      docBuilderFactory.setNamespaceAware(true);
      DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder(); 
      Document doc = docBuilder.parse(new InputSource(reader));
      return doc.getDocumentElement();
    } catch (SAXException saxe) {
      Assert.fail("Error parsing XML document");
      saxe.printStackTrace();
    } catch (ParserConfigurationException pce) {
      Assert.fail("Error parsing XML document");
      pce.printStackTrace();
    } catch (IOException ioe) {
      Assert.fail("Error parsing XML document");
      ioe.printStackTrace();
    }
    return null;
  }
  
  private static class DummyFilter implements RequestFilter {
    private Collection requests;
    
    public DummyFilter(Collection requests) {
      this.requests = requests;
    }

    public String getFilterName() {
      return "DummyFilter";
    }

    public void configure(Element config) throws FilterException {
    }

    public void prepare(Request request) throws FilterException {
    }

    public void commit() {
    }

    public void rollback() {
    }

    public boolean accept(Request request) {
      return true;
    }

    public boolean accept(Request request, boolean ignore) {
      return true;
    }

    public boolean willAccept(Request request) {
      return true;
    }

    public boolean canAccept() {
      return true;
    }

    public void acceptAll() {
    }

    public Collection getAcceptedRequests() {
      return Collections.unmodifiableCollection(this.requests);
    }

    public Collection getRemainingRequests() {
      return Collections.unmodifiableCollection(this.requests);
    }

    public void setPreviousRequests(Collection requests) {
      this.requests = requests;
    }

    public RequestFilter reset() {
      return this;
    }

		/**
		 * TODO why this methods was overriden, and what's the new expected behavior.
		 * 
		 * @return
		 * @see br.com.auster.dware.request.RequestFilter#getAllowedRequests()
		 */
		public Map<String, List<Request>> getAllowedRequests() {
			return Collections.emptyMap();
		}
    
  }
  
}
