package br.com.auster.dware.manager.checkpoint;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.sql.SQLConnectionManager;
import br.com.auster.common.sql.SQLStatement;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.graph.Request;
import br.com.auster.dware.manager.DataAwareManagerMediator;

/**
 * A Checkpoint implementation that uses a Database to
 * persist the requests.
 * <p>
 *
 * SQLCheckpoint has four possible status:
 * <p>'q' : queued
 * <p>'p' : processed (success)
 * <p>'r' : failed but will retry
 * <p>'f' : failed and won't retry
 * <p>
 *
 * To configure this class, you need to map the query
 * (statement) names for each situation:
 * <p> - query all requests in database
 * <p> - query all requests by transaction-id
 * <p> - query a single request by transaction-id and request-id
 * <p> - insert a new request by transaction-id
 * <p> - update an existing request by transaction-id and request-id
 * <p>
 *
 * Configuration example:
 *
 * Suppose that you have the following configured statements for
 * database named "db":
 * <pre>
 * &lt;statement name="loadAllRequests"&gt;
 *   &lt;query&gt;
 *   SELECT TRANSACTION_ID, REQUEST_ID, STATUS
 *   FROM DWARE_CHECKPOINT
 *   &lt;/query&gt;
 * &lt;/statement&gt;
 *
 * &lt;statement name="loadAllByTransaction"&gt;
 *   &lt;query&gt;
 *   SELECT REQUEST_ID, STATUS
 *   FROM DWARE_CHECKPOINT
 *   WHERE TRANSACTION_ID = ?
 *   &lt;/query&gt;
 *   &lt;param index="1" type="String"/&gt;
 * &lt;/statement&gt;
 *
 * &lt;statement name="loadRequest"&gt;
 *   &lt;query&gt;
 *   SELECT STATUS
 *   FROM DWARE_CHECKPOINT
 *   WHERE TRANSACTION_ID = ? AND REQUEST_ID = ?
 *   &lt;/query&gt;
 *   &lt;param index="1" type="String"/&gt;
 *   &lt;param index="2" type="String"/&gt;
 * &lt;/statement&gt;
 *
 * &lt;statement name="insertRequest"&gt;
 *   &lt;query&gt;
 *   INSERT INTO DWARE_CHECKPOINT (TRANSACTION_ID, REQUEST_ID, STATUS)
 *   VALUES (?, ?, ?)
 *   &lt;/query&gt;
 *   &lt;param index="1" type="String"/&gt;
 *   &lt;param index="2" type="String"/&gt;
 *   &lt;param index="3" type="String"/&gt;
 * &lt;/statement&gt;
 *
 * &lt;statement name="insertRequest"&gt;
 *   &lt;query&gt;
 *   UPDATE DWARE_CHECKPOINT
 *   SET STATUS = ?
 *   WHERE TRANSACTION_ID = ? AND REQUEST_ID = ?
 *   &lt;/query&gt;
 *   &lt;param index="1" type="String"/&gt;
 *   &lt;param index="2" type="String"/&gt;
 *   &lt;param index="3" type="String"/&gt;
 * &lt;/statement&gt;
 * </pre>
 *
 * The XML configuration for the SQLCheckpoint would be:
 *
 * <pre>
 * &lt;checkpoint class-name="br.com.auster.dware.manager.checkpoint.SQLCheckpoint"
 *             database-name="db"
 *             max-req-fails="3"
 *             requeue-sleep-milis="5000"&gt;
 *   &lt;query-all statement-name="loadAllRequests"&gt;
 *     &lt;columns transaction-id="1"
 *              request-id="2"
 *              status="3"/&gt;
 *   &lt;/query-all&gt;
 *   &lt;query-by-transaction statement-name="loadAllByTransaction"&gt;
 *     &lt;columns request-id="1"
 *              status="2"/&gt;
 *     &lt;params transaction-id="1"/&gt;
 *   &lt;/query-by-transaction&gt;
 *   &lt;insert statement-name="insertRequest"&gt;
 *     &lt;params transaction-id="1"
 *             request-id="2"
 *             status="3"/&gt;
 *   &lt;/insert&gt;
 *   &lt;update statement-name="updateRequest"&gt;
 *     &lt;params status="1"
 *             transaction-id="2"
 *             request-id="3"/&gt;
 *   &lt;/insert&gt;
 * &lt;/checkpoint&gt;
 * </pre>
 *
 * @author rbarone
 * @version $Id$
 */
public class SQLCheckpoint extends AbstractCheckpoint {

  public static final String INITIAL_TRANSACTION_ID_PROPERTY =
    "br.com.auster.dware.checkpoint.sql.transactionid.initial";
  private static final String INITIAL_TRANSACTION_ID =
    System.getProperty(INITIAL_TRANSACTION_ID_PROPERTY);

  public static final String DB_NAME_ATT = "database-name";

  public static final String QUERY_ALL_ELT = "query-all";
  public static final String QUERY_TRANSACTION_ELT = "query-by-transaction";
  public static final String QUERY_ONE_ELT = "query-one";
  public static final String INSERT_ELT = "insert";
  public static final String UPDATE_ELT = "update";

  public static final String STATEMENT_ATT = "statement-name";
  public static final String COLUMNS_ELT = "columns";
  public static final String PARAMS_ELT = "params";

  public static final String TRANSACTION_ID_ATT = "transaction-id";
  public static final String REQUEST_ID_ATT = "request-id";
  public static final String STATUS_ATT = "status";

  public static final char QUEUED_STATUS = 'q';
  public static final char PROCESSED_STATUS = 'p';
  public static final char RETRY_STATUS = 'r';
  public static final char FAILED_STATUS = 'f';

  private static final Logger log = Logger.getLogger(SQLCheckpoint.class);

  private final SQLConnectionManager sqlManager;

  private final Query queryAll;
  private final Query queryAllByTransaction;
  private final Query insert;
  private final Query update;

  // HashMap<CheckpointItem>
  private HashSet checkpoint;

  public SQLCheckpoint(Element config, DataAwareManagerMediator _dwareManMed) {
    super(config, _dwareManMed);

    // initialize database manager
    String dbName = DOMUtils.getAttribute(config, DB_NAME_ATT, true);
    try {
      this.sqlManager = SQLConnectionManager.getInstance(dbName);
    } catch (NamingException e) {
      log.error(i18n.getString("sqlNaming", dbName));
      throw new IllegalStateException(i18n.getString("dbError", dbName), e);
    }

    // configure query-all
    Element elt = DOMUtils.getElement(config, QUERY_ALL_ELT, true);
    this.queryAll = new Query(this.sqlManager, elt, false, false, false);

    // configure query-transaction
    elt = DOMUtils.getElement(config, QUERY_TRANSACTION_ELT, true);
    this.queryAllByTransaction = new Query(this.sqlManager, elt, true, false, false);

    // configure insert
    elt = DOMUtils.getElement(config, INSERT_ELT, true);
    this.insert = new Query(this.sqlManager, elt, true, true, true);

    // configure update
    elt = DOMUtils.getElement(config, UPDATE_ELT, true);
    this.update = new Query(this.sqlManager, elt, true, true, true);
  }

  protected HashMap initReqFailedMap() {
    return new HashMap();
  }

  protected HashSet initReqProcessedHash() {
    initCheckpoint();
    HashSet procHash = new HashSet();
    Iterator it = this.checkpoint.iterator();
    while (it.hasNext()) {
      CheckpointItem item = (CheckpointItem) it.next();
      if (item.getStatus() == PROCESSED_STATUS) {
        procHash.add( Request.buildId(item.getTransactionId(), item.getRequestId()) );
      }
    }
    log.info(i18n.getString("previousCheckPoint"));
    return procHash;
  }

  protected HashSet initReqQueuedHash() {
    return new HashSet();
  }

  protected void loadReqFailed(Request req, boolean isRetry, int numberFails) {
    String tid = req.getTransactionId();
    if (tid == null) {
      tid = INITIAL_TRANSACTION_ID;
    }
    char status = isRetry ? RETRY_STATUS : FAILED_STATUS;
    updateCheckpoint( new CheckpointItem(tid, req.getUserKey(), status) );
  }

  protected void loadReqProcessed(Request req) {
    String tid = req.getTransactionId();
    if (tid == null) {
      tid = INITIAL_TRANSACTION_ID;
    }
    updateCheckpoint( new CheckpointItem(tid, req.getUserKey(), PROCESSED_STATUS) );
  }

  protected void loadReqWillBeProcessed(Request req) {
    String tid = req.getTransactionId();
    if (tid == null) {
      tid = INITIAL_TRANSACTION_ID;
    }
    updateCheckpoint( new CheckpointItem(tid, req.getUserKey(), QUEUED_STATUS) );
  }

  public void shutdown() {
    // nothing to do
  }

  private void initCheckpoint() {
    this.checkpoint = new HashSet();
    Connection conn = null;
    ResultSet rs = null;
    try {
      conn = this.sqlManager.getConnection();

      if (INITIAL_TRANSACTION_ID == null) {
        rs = this.queryAll.getStatement().prepareStatement(conn, null).executeQuery();
      } else {
        Object[] params = new Object[] { INITIAL_TRANSACTION_ID };
        rs = this.queryAllByTransaction.getStatement().prepareStatement(conn, params).executeQuery();
      }

      while (rs.next()) {
        String tid;
        if (INITIAL_TRANSACTION_ID == null) {
          tid = rs.getString(this.queryAll.getColumns().getTransactionIdForJDBC());
        } else {
          tid = INITIAL_TRANSACTION_ID;
        }
        String requestId = rs.getString(this.queryAll.getColumns().getRequestIdForJDBC());
        String status = rs.getString(this.queryAll.getColumns().getStatusForJDBC());
        this.checkpoint.add(new CheckpointItem(tid, requestId, status));
      }

    } catch (SQLException e) {
      log.error(i18n.getString("dbError"), e);
    } finally {
      if (rs != null) {
        try { rs.close(); } catch (Exception e) {}
      }
      if (conn != null) {
        try { conn.close(); } catch (Exception e) {}
      }
    }
  }

  private void updateCheckpoint(CheckpointItem item) {
	// TODO instead of removing and adding the item (thus always updating the database),
	//      compare the status and update it (both the object and the database) only when necessary
    Query query = this.checkpoint.remove(item) ? this.update : this.insert;
    this.checkpoint.add(item);
    Connection conn = null;
    try {
      conn = this.sqlManager.getConnection();
      Object params[] = new Object[3];
      params[query.getParams().getTransactionId()] = item.getTransactionId();
      params[query.getParams().getRequestId()] = item.getRequestId();
      params[query.getParams().getStatus()] = String.valueOf(item.getStatus());
      int rowCount = query.getStatement().prepareStatement(conn, params).executeUpdate();
      if (rowCount <= 0) {
        log.error("No rows affected for checkpoint update: " + item);
      }
      if (!conn.getAutoCommit()) {
    	  conn.commit();
      }
    } catch (SQLException e) {
      log.error(i18n.getString("dbError"), e);
    } finally {
      if (conn != null) {
        try { conn.close(); } catch (Exception e) {}
      }
    }
  }


  //###################
  // INNER CLASSES
  //###################

  private static class QueryFields {
    private final int transactionId;
    private final int requestId;
    private final int status;
    public QueryFields(Element config,
                       boolean isTransactionMadatory,
                       boolean isRequestMandatory,
                       boolean isStatusMandatory) {
      this.transactionId = DOMUtils.getIntAttribute(config, TRANSACTION_ID_ATT, isTransactionMadatory) - 1;
      this.requestId = DOMUtils.getIntAttribute(config, REQUEST_ID_ATT, isRequestMandatory) - 1;
      this.status = DOMUtils.getIntAttribute(config, STATUS_ATT, isStatusMandatory) - 1;
    }
    public int getTransactionId() {
      return this.transactionId;
    }
    public int getRequestId() {
      return this.requestId;
    }
    public int getStatus() {
      return this.status;
    }

    public int getTransactionIdForJDBC() {
        return this.transactionId+1;
      }
      public int getRequestIdForJDBC() {
        return this.requestId+1;
      }
      public int getStatusForJDBC() {
        return this.status+1;
      }

  }

  private static class Query {

    private final SQLStatement stmt;
    private final QueryFields params;
    private final QueryFields columns;

    public Query (SQLConnectionManager sqlManager,
                  Element config,
                  boolean transactionIdAsParam,
                  boolean requestIdAsParam,
                  boolean statusAsParam) {

      String stmtName = DOMUtils.getAttribute(config, STATEMENT_ATT, true);
      this.stmt = sqlManager.getStatement(stmtName);

      if (transactionIdAsParam || requestIdAsParam || statusAsParam) {
        Element paramsElt = DOMUtils.getElement(config, PARAMS_ELT, true);
        this.params = new QueryFields(paramsElt,
                                      transactionIdAsParam,
                                      requestIdAsParam,
                                      statusAsParam);
      } else {
        this.params = null;
      }

      if (!transactionIdAsParam || !requestIdAsParam || !statusAsParam) {
        Element columnsElt = DOMUtils.getElement(config, COLUMNS_ELT, true);
        this.columns = new QueryFields(columnsElt,
                                       !transactionIdAsParam,
                                       !requestIdAsParam,
                                       !statusAsParam);
      } else {
        this.columns = null;
      }
    }

    public SQLStatement getStatement() {
      return this.stmt;
    }
    public QueryFields getParams() {
      return this.params;
    }
    public QueryFields getColumns() {
      return this.columns;
    }
  }

  private static class CheckpointItem {

    private String transactionId;
    private String requestId;
    private char status;

    public CheckpointItem(String transactionId,
                          String requestId,
                          char status) {
      this.transactionId = transactionId;
      this.requestId = requestId;
      this.status = status;
    }

    public CheckpointItem(String transactionId,
                          String requestId,
                          String status) {
      this(transactionId, requestId, status == null ? '\0' : status.charAt(0));
    }

    public CheckpointItem(String transactionId,
                          String requestId) {
      this(transactionId, requestId, '\0');
    }

    public String getTransactionId() {
      return this.transactionId;
    }
    public String getRequestId() {
      return this.requestId;
    }
    public char getStatus() {
      return this.status;
    }
    public void setStatus(char newStatus) {
      this.status = newStatus;
    }

    public boolean equals(Object obj) {
      if ( !(obj instanceof CheckpointItem) ) {
        return false;
      }

      CheckpointItem that = (CheckpointItem) obj;
      boolean isEqual = true;
      if (that.transactionId == null) {
        if (this.transactionId != null) {
          isEqual = false;
        }
      } else {
       isEqual &= that.transactionId.equals(this.transactionId);
      }
      if (that.requestId == null) {
        if (this.requestId != null) {
          isEqual = false;
        }
      } else {
        isEqual &= that.requestId.equals(this.requestId);
      }

      return isEqual;
    }

    public int hashCode() {
      HashCodeBuilder b = new HashCodeBuilder(23, 7);
      b.append(this.transactionId).append(this.requestId);
      return b.toHashCode();
    }

    public String toString() {
      return "[" + this.transactionId + ";" + this.requestId + ";" + this.status + "]";
    }
  }

}