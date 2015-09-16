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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.Request;

/**
 * This filter has as objective to be just a pipe of SAX Events from before to
 * after filters. In other words, just a bridge to implement a very simple
 * functionality.
 *
 * This filter will read configuration and find a max size a request must have.
 * If not informed this filter is void and does nothing (Thus, should not be in
 * the graph-design). If equals to -1 is the same as not informed. The value of
 * the attribute MUST be translatable to a Long.
 *
 * The users of this class must be aware of some potential undesired
 * side-effects: 1. When a request larger than the configuration allows is found
 * and FilterException is thrown. 2. When the exception is thrown the request is
 * rolledback, and ALL filters undo their processing. 3. Any FinishListener
 * configured at the end of the processing will capture this exception. 4. Any
 * checkpoint configured will capture this exception and potentially will retry
 * the execution.
 *
 * This class is thread safe due to ligh Lock implementation during
 * configuration handling. No addiitonal syncronization is done.
 *
 *
 * @version $Id: ContentHandlerPipeFilter.java 87 2005-08-04 21:21:25Z mtengelm $
 */
public final class CHLimiterPipeFilter extends DefaultFilter implements
		ContentHandler {

	private final static Logger log = Logger
			.getLogger(CHLimiterPipeFilter.class);
	private static final I18n i18n = I18n
			.getInstance(CHLimiterPipeFilter.class);

	/**
	 * The Attribute name that must be configured as max size of a request to be
	 * processed.
	 */
	public static final String MAX_SIZE_LIMIT = "max-size";

	/***************************************************************************
	 * This attribute is used in order to avoid situations of having a max-size
	 * defined, just for a few bytes a request not be processed.
	 *
	 * So, as deviation we expect a percentage that will HARDCODED limited to
	 * 9%. The value we expect here is in the form: N - Where it represents the
	 * precentual of upscale of max limit parameter. If N=9, then the follwoing
	 * will be done maxLimit = maxLimit + ((maxLimit * N) /100) )
	 *
	 * Any configuration higher than 9999 will be disallowed and will be
	 * reverted to 0500 (5%).
	 *
	 * This parameter will be handled only and if only maxLimit be a valid
	 * parameter.
	 *
	 * This parameter must be translated to an Integer.
	 */
	public static final String DEVIATION = "pct-deviation";

	/**
	 * If this filter, in order to suspend processing the current request should
	 * raise an exception (default), or just not "return" without forwarding the SAX
	 * events to the next filter.
	 */
	public static final String IGNORE_EXCEPTION = "ignore-exception";

	private ContentHandler handler;
	private long maxLimit = -1;
	private boolean useExceptions;
	private boolean limitExceeded;
	private Lock lock = new ReentrantLock();

	public CHLimiterPipeFilter(String name) throws Exception {
		super(name);
	}

	/**
	 * Configures this filter. This filter expects to find an attribute named as (
	 *
	 * @see MAX_SIZE_LIMIT field). The value MUST be in bytes. And it is not
	 *      mandatory. If not existent nothing will happen and any size of
	 *      request is acceptable.
	 *
	 * This method uses light Lock as of JAVA5.0 so it is thread safe.
	 */
	public final void configure(Element config) throws FilterException {
		try {
			lock.lock();
			String max = DOMUtils.getAttribute(config, MAX_SIZE_LIMIT, false);
			if ((max != null) && (!max.equals(""))) {
				maxLimit = Long.parseLong(max);
				int dev = DOMUtils.getIntAttribute(config, DEVIATION, false);
				if (dev > 0) {
					dev = (dev > 9) ? 5 : dev;
					maxLimit += (maxLimit * dev) / 100;
					log.info("MaxLimit overriden by percent deviation. Final max limit:" + maxLimit);
				}
			}
			this.useExceptions = ( ! DOMUtils.getBooleanAttribute(config, IGNORE_EXCEPTION, false) );
		} finally {
			lock.unlock();
		}
	}

	/**
	 * @see br.com.auster.dware.graph.DefaultFilter#prepare(br.com.auster.dware.graph.Request)
	 */
	public void prepare(Request req) throws FilterException {
		this.limitExceeded = false;
		if ((maxLimit != -1) && (req.getWeight() > maxLimit)) {
			this.limitExceeded = true;
			// OK. Request is larger than configured. Let´s throw an exception.
			if (this.useExceptions) {
				// This is because we want the finish listener to capture this and
				// compute this request as a failed one.
				throw new FilterException(i18n.getString("maxSizeExceeded", new Long(req.getWeight()), new Long(maxLimit)));
			} else {
				// In this case the request will not be set with error, and the file will be processed but not
				// forwarded. This option will take more time and CPU then using exceptions
				log.info(i18n.getString("maxSizeExceeded", new Long(req.getWeight()), new Long(maxLimit)));
			}
		}

	}

	/***************************************************************************
	 * Gets the Input for this filter. The input is this instance itself, that
	 * acts as a ContentHandler. Standard DefaultFilter Method.
	 *
	 * @see DefaultFilter for more info.
	 */
	public Object getInput(String filterName) {
		return this;
	}

	/**
	 * Sets the output for this filter. It expects to receive a ContentHandler
	 * where all incoming SAX Events reveived by this instance will be written
	 * to.
	 */
	public void setOutput(String sinkName, Object oCH) throws ConnectException {
		handler = (ContentHandler) oCH;
	}

	// START OF CONTENTHANDLER IMPLEMENTATION
	// JUST BYPASS FROM INPUT TO OUTPUT
	public void endDocument() throws SAXException {
		if (!this.limitExceeded) { handler.endDocument(); }
	}

	public void startDocument() throws SAXException {
		if (!this.limitExceeded) { handler.startDocument(); }
	}

	public void characters(char[] ch, int start, int length) throws SAXException {
		if (!this.limitExceeded) { handler.characters(ch, start, length); }
	}

	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (!this.limitExceeded) { handler.ignorableWhitespace(ch, start, length); }
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		if (!this.limitExceeded) { handler.endPrefixMapping(prefix); }
	}

	public void skippedEntity(String name) throws SAXException {
		if (!this.limitExceeded) { handler.skippedEntity(name); }
	}

	public void setDocumentLocator(Locator locator) {
		if (!this.limitExceeded) { handler.setDocumentLocator(locator); }
	}

	public void processingInstruction(String target, String data) throws SAXException {
		if (!this.limitExceeded) { handler.processingInstruction(target, data); }
	}

	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (!this.limitExceeded) { handler.startPrefixMapping(prefix, uri); }
	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (!this.limitExceeded) { handler.endElement(namespaceURI, localName, qName); }
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
		if (!this.limitExceeded) { handler.startElement(namespaceURI, localName, qName, atts); }
	}

}
