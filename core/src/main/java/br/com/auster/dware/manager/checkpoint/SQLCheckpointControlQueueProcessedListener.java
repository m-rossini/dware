/*
 * Copyright (c) 2004-2007 Auster Solutions. All Rights Reserved.
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
 * Created on 28/11/2007
 */
package br.com.auster.dware.manager.checkpoint;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import br.com.auster.common.sql.SQLConnectionManager;
import br.com.auster.common.sql.SQLStatement;
import br.com.auster.common.util.I18n;
import br.com.auster.common.xml.DOMUtils;
import br.com.auster.dware.manager.QueueProcessedListener;

/**
 * This class will help {@link SQLCheckpoint} removing from the checkpoint table all requests
 *    for a specified transaction id when the transaction has finished. For requests that have
 *    no transaction id, then all requests for transaction id <code>null</code> will be removed.
 * <p>
 * By defining the SQL statement that will be executed for deleting rows, one can determine if
 * 	  only finished requests (defined by the status <code>{@link SQLCheckpoint#PROCESSED_STATUS}
 *    will be removed or if all including queued and on retry. This last two status normally should
 *    not appear, but since this operation happends in a transaction finished basis, and transaction
 *    ids are not mandatory for requests, <code>null</code> transaction id can <i>finish</i> more than
 *    once.
 * <p>
 *
 * @author framos
 * @version $Id$
 *
 */
public class SQLCheckpointControlQueueProcessedListener implements QueueProcessedListener {



	private static final Logger log = Logger.getLogger(SQLCheckpointControlQueueProcessedListener.class);
	private static final I18n i18n = I18n.getInstance(SQLCheckpointControlQueueProcessedListener.class);

	public static final String DB_NAME_ATT = "database-name";
	public static final String DELETE_QUERY_ELT = "delete-query";


	protected String databaseName;
	protected String deleteQuery;




	/**
	 * @see br.com.auster.dware.manager.QueueProcessedListener#init(org.w3c.dom.Element)
	 */
	public void init(Element _config) {
		this.databaseName = DOMUtils.getAttribute(_config, DB_NAME_ATT, true);
		log.info(i18n.getString("sqlckptcontrol.database", this.databaseName));
		this.deleteQuery =  DOMUtils.getAttribute(_config, DELETE_QUERY_ELT, true);
		log.info(i18n.getString("sqlckptcontrol.query", this.deleteQuery));
	}

	/**
	 * @see br.com.auster.dware.manager.QueueProcessedListener#onQueueProcessed(java.lang.String, int)
	 */
	public void onQueueProcessed(String _transactionId, int _size) {
		log.info(i18n.getString("sqlckptcontrol.removingTransaction", _transactionId, String.valueOf(_size)));
		Connection conn = null;
		PreparedStatement stmt = null;
		try {
			conn = SQLConnectionManager.getInstance(this.databaseName).getConnection();
			SQLStatement sqlStmt = SQLConnectionManager.getInstance(this.databaseName).getStatement(this.deleteQuery);

			stmt = conn.prepareStatement(sqlStmt.getStatementText());
			log.debug(i18n.getString("sqlckptcontrol.runningQuery", sqlStmt.getStatementText()));
			stmt.setString(1, _transactionId);
			int rows = stmt.executeUpdate();
			log.debug(i18n.getString("sqlckptcontrol.removedRows", rows, _transactionId));
			if (!conn.getAutoCommit()) { conn.commit(); }
		} catch (SQLException sqle) {
			log.error(i18n.getString("sqlckptcontrol.error", _transactionId), sqle);
		} catch (NamingException ne) {
			log.error(i18n.getString("sqlckptcontrol.error", _transactionId), ne);
		} finally {
			try {
				if (stmt != null) { stmt.close(); }
				if (conn != null) { conn.close(); }
			} catch (SQLException sqle) {
				log.error(i18n.getString("sqlckptcontrol.error", _transactionId), sqle);
			}
		}
	}

}
