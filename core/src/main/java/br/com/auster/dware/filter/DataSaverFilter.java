/*
 * Copyright (c) 2004-2006 Auster Solutions do Brasil. All Rights Reserved.
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
 * Created on 27/01/2006
 */

package br.com.auster.dware.filter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import br.com.auster.common.data.DataSaver;
import br.com.auster.common.data.DataSaverException;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.ConnectException;
import br.com.auster.dware.graph.DefaultFilter;
import br.com.auster.dware.graph.FilterException;
import br.com.auster.dware.graph.ObjectProcessor;
import br.com.auster.dware.graph.Request;

/**
 * <p>
 * <b>Title:</b> DataSaverFilter
 * </p>
 * <p>
 * <b>Description:</b> A data saver filter
 * </p>
 * <p>
 * <b>Copyright:</b> Copyright (c) 2005
 * </p>
 * <p>
 * <b>Company:</b> Auster Solutions
 * </p>
 * 
 * Sample configuration is as follows:
 * 
 * <dw:config encoding="ISO-8859-1"> <dw:database driver="org.postgresql.Driver"
 * url="jdbc:postgresql://localhost:5432/billcheckout" username="billchk"
 * password="billchk"/> <dw:data-saver-list> <dw:data-saver
 * class-name="br.com.auster.common.data.SQLDataSaver"
 * config-file="conf/data-saver.conf.xml" encrypted="true"/> ...
 * </dw:data-saver-list> </dw:config>
 * 
 * 
 * @author etirelli
 * @version $Id: DataSaverFilter.java 282 2007-03-01 17:49:44Z framos $
 */
public class DataSaverFilter extends DefaultFilter implements ObjectProcessor {

    private static final Logger log = Logger.getLogger(DataSaverFilter.class);
    private static final I18n i18n = I18n.getInstance(DataSaverFilter.class);

    private static final String CONFIG_SAVER_LIST    = "data-saver-list";
    private static final String CONFIG_DATA_SAVER    = "data-saver";
    private static final String CONFIG_CLASS_NAME    = "class-name";
    private static final String CONFIG_CONF_FILE     = "config-file";
    private static final String CONFIG_ENCRYPTED     = "encrypted";

    private static final String CONFIG_JDBC          = "database";
    /**
     * @deprecated no longer to be used; set JDBC driver using -D option
     */
    private static final String CONFIG_JDBC_DRIVER   = "driver";
    private static final String CONFIG_JDBC_URL      = "url";
    private static final String CONFIG_JDBC_USERNAME = "username";
    private static final String CONFIG_JDBC_PASSWORD = "password";

    private List saverList;
    private ObjectProcessor objProcessor;

    
    public DataSaverFilter(String name) {
        super(name);
        saverList = new ArrayList();
    }

    /**
     * @inheritDoc
     */
    public void prepare(Request request) throws FilterException {}

    /**
     * @inheritDoc
     */
    public void configure(Element config) throws FilterException {
        Element jdbc = DOMUtils.getElement(config, CONFIG_JDBC, true);
//        String jdbcDriver = DOMUtils.getAttribute(jdbc, CONFIG_JDBC_DRIVER, true);
        String jdbcURL = DOMUtils.getAttribute(jdbc, CONFIG_JDBC_URL, true);
        String jdbcUsername = DOMUtils.getAttribute(jdbc, CONFIG_JDBC_USERNAME, false);
        String jdbcPassword = DOMUtils.getAttribute(jdbc, CONFIG_JDBC_PASSWORD, false);
        Connection connection = null;
        try {
//            Class.forName(jdbcDriver);
        	if ((jdbcUsername != null) && (jdbcUsername.trim().length() > 0)) {
        		connection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword);
        	} else {
        		connection = DriverManager.getConnection(jdbcURL);
        	}
        } catch (Exception e) {
            throw new FilterException("Error stablishing database connection", e);
        }
        Element list = DOMUtils.getElement(config, CONFIG_SAVER_LIST, true);
        NodeList dslist = DOMUtils.getElements(list, CONFIG_DATA_SAVER);
        int length = dslist.getLength();
        for (int i = 0; i < length; i++) {
            Element dataSaver = (Element) dslist.item(i);
            String dsClass = DOMUtils.getAttribute(dataSaver, CONFIG_CLASS_NAME, true);
            String dsConfig = DOMUtils.getAttribute(dataSaver, CONFIG_CONF_FILE, true);
            String dsEncrypt = DOMUtils.getAttribute(dataSaver, CONFIG_ENCRYPTED, true);
            try {
                DataSaver saver = (DataSaver) Class.forName(dsClass).newInstance();
                Element conf = DOMUtils.openDocument(dsConfig, Boolean.valueOf(dsEncrypt).booleanValue());
                saver.configure(conf, connection);
                saverList.add(saver);
            } catch (Exception e) {
                log.error("Error instantiating DataSaver class [" + dsClass + "]", e);
            }
        }
        if (saverList.size() == 0) {
            throw new FilterException("Error configuring DataSaverFilter. No saver plugin found.");
        }
    }

    /**
     * @inheritDoc
     */
    public Object getInput(String filterName) throws ConnectException, UnsupportedOperationException {
        return this;
    }

    /**
     * @inheritDoc
     * 
     * @param _filterName
     * @param _output
     * @return
     * @throws ConnectException
     * @throws UnsupportedOperationException
     */
    public Object getOutput(String _filterName, Object _output) throws ConnectException, UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
    
	/**
	 * Sets the Output for this filter.
	 * 
	 */
	public void setOutput(String sourceName, Object objProcessor) {
		this.objProcessor = (ObjectProcessor) objProcessor;
	}    

    /**
     * @inheritDoc
     */
    public void processElement(Object map) throws FilterException {
        long start = System.currentTimeMillis();
        log.info(i18n.getString("allFilters.startProcessing", this.getClass().getSimpleName(), this.filterName));
        Map dataMap = (Map) map;
        for (Iterator i = saverList.iterator(); i.hasNext();) {
            DataSaver saver = (DataSaver) i.next();
            try {
                saver.save(dataMap);
            } catch (DataSaverException e) {
                log.error("Error saving data", e);
            }
        }
        long time = System.currentTimeMillis() - start;
        log.info(i18n.getString("allFilters.endProcessing", this.getClass().getSimpleName(), this.filterName, String.valueOf(time)));
        if (this.objProcessor != null) {
        	log.debug(i18n.getString("allFilters.hasNextFilter"));
        	this.objProcessor.processElement(map);
        } else {
        	log.debug(i18n.getString("allFilters.noNextFilter"));
        }
    }
}
