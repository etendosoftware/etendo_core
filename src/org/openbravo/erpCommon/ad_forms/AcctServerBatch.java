/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_forms;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;

/**
 * Posts the pending documents of one {@link AcctServer} table, one batch per {@link #run} call, and
 * keeps what the batches did: the documents that failed to post and the one that ended a batch
 * early. The posted and failed counters of the {@link AcctServer} are updated as before.
 */
public class AcctServerBatch {
  // Same category as the log lines written by AcctServer.run before they moved here
  private static final Logger log4j = LogManager.getLogger(AcctServer.class);

  private final AcctServer acct;
  // Record id -> {document no, posted status} of the documents that failed to post
  private final Map<String, String[]> failedDocs = new LinkedHashMap<>();
  // {record id, document no, posted status, error} of the document that ended the last run early
  private String[] blockedDoc;
  private Connection con;
  private String currentId;

  /**
   * @param acct
   *     the table to post, as returned by {@link AcctServer#get}
   */
  public AcctServerBatch(AcctServer acct) {
    this.acct = acct;
  }

  /**
   * Posts one batch of documents, committing each one. Stops at the first document that post()
   * cannot handle (e.g. locked by a concurrent posting) and at an unexpected error, which is only
   * logged.
   */
  public void run(VariablesSecureApp vars, String strDateFrom, String strDateTo)
      throws ServletException {
    if (acct.AD_Client_ID.equals("")) {
      acct.AD_Client_ID = vars.getClient();
    }
    con = null;
    currentId = null;
    blockedDoc = null;
    try {
      postDocuments(vars, selectDocuments(strDateFrom, strDateTo));
    } catch (NoConnectionAvailableException ex) {
      throw new ServletException("@CODE=NoConnectionAvailable", ex);
    } catch (SQLException ex2) {
      releaseRollbackConnection();
      throw new ServletException(
          "@CODE=" + Integer.toString(ex2.getErrorCode()) + "@" + ex2.getMessage(), ex2);
    } catch (Exception ex3) {
      log4j.error("Exception in AcctServer.run", ex3);
      if (currentId != null) {
        blockedDoc = new String[] { currentId, "", null, ex3.getMessage() };
      }
      releaseRollbackConnection();
    }
  }

  private AcctServerData[] selectDocuments(String strDateFrom, String strDateTo)
      throws ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - Run - TableName = " + acct.tableName + strDateFrom + strDateTo);
    }

    log4j.debug("AcctServer.run - AD_Client_ID: " + acct.AD_Client_ID);

    final Set<String> orgSet = OBContext.getOBContext()
        .getOrganizationStructureProvider(acct.AD_Client_ID)
        .getChildTree(acct.AD_Org_ID, true);
    String strOrgs = Utility.getInStrSet(orgSet);
    // Send limit manually to SQL because auto-generated query doesn't limit properly
    String limit = StringUtils.equals(acct.connectionProvider.getRDBMS(), "ORACLE")
        ? " AND ROWNUM < " + acct.batchSize
        : " LIMIT " + acct.batchSize;
    AcctServerData[] data = AcctServerData.select(acct.connectionProvider, acct.tableName,
        acct.strDateColumn, acct.AD_Client_ID, strOrgs, strDateFrom, strDateTo, limit);
    if (data != null && data.length > 0 && log4j.isDebugEnabled()) {
      log4j.debug(
          "AcctServer - Run -Select inicial realizada N = " + data.length + " - Key: " + data[0].id);
    }
    return data;
  }

  private void postDocuments(VariablesSecureApp vars, AcctServerData[] data)
      throws NoConnectionAvailableException, SQLException, ServletException {
    if (data == null) {
      return;
    }
    final StringBuilder strIDs = new StringBuilder();
    for (AcctServerData document : data) {
      con = acct.connectionProvider.getTransactionConnection();
      currentId = document.getField("ID");
      strIDs.append(currentId).append(", ");
      if (!postDocument(vars)) {
        return;
      }
    }
    if (log4j.isDebugEnabled()) {
      log4j.debug("AcctServer - Run -" + data.length + " IDs [" + strIDs + "]");
    }
  }

  /** Posts {@link #currentId} on {@link #con}. Returns false when the batch must stop there. */
  private boolean postDocument(VariablesSecureApp vars) throws SQLException, ServletException {
    acct.setMessageResult(null);
    AcctServer tempServer = AcctServer.get(acct.AD_Table_ID, acct.AD_Client_ID, acct.AD_Org_ID,
        acct.connectionProvider);
    boolean postSuccess = tempServer.post(currentId, false, vars, acct.connectionProvider, con);
    acct.errors = acct.errors + tempServer.errors;
    acct.success = acct.success + tempServer.success;
    // post() also returns true when the document fails to post: the error counter is the signal
    if (tempServer.errors > 0) {
      failedDocs.put(currentId, new String[] { tempServer.DocumentNo, tempServer.getStatus() });
    }
    if (!postSuccess) {
      blockedDoc = new String[] { currentId, tempServer.DocumentNo, tempServer.getStatus(), null };
      acct.connectionProvider.releaseRollbackConnection(con);
      return false;
    }
    acct.connectionProvider.releaseCommitConnection(con);
    OBDal.getInstance().commitAndClose();
    return true;
  }

  private void releaseRollbackConnection() {
    try {
      acct.connectionProvider.releaseRollbackConnection(con);
    } catch (SQLException se) {
      log4j.error("Failed to close connection after an error", se);
    }
  }

  /**
   * @return the number of documents posted by the {@link AcctServer} of this batch
   */
  public int getSuccess() {
    return acct.success;
  }

  /**
   * @return record id to {document no, posted status} of the documents that failed to post in
   *         {@link #run}
   */
  public Map<String, String[]> getFailedDocs() {
    return Collections.unmodifiableMap(failedDocs);
  }

  /**
   * @return {record id, document no, posted status, error message} of the document that ended the
   *         last {@link #run} before the end of its batch (not posted, e.g. locked by a concurrent
   *         posting, or an unexpected error), or null if the batch was walked to the end
   */
  public String[] getBlockedDoc() {
    return blockedDoc == null ? null : blockedDoc.clone();
  }
}
