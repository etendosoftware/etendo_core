/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2009-2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_process;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

public class AcctServerProcess extends DalBaseProcess {

  private final static String BATCH_SIZE = "50";
  private final static String SYSTEM_CLIENT_ID = "0";

  private boolean isDirect;

  private StringBuffer lastLog = new StringBuffer();
  private StringBuffer message = new StringBuffer();

  private String[] TableIds = null;

  private static final Pattern TABLE_NAME = Pattern.compile("\\w+");
  private static final int MAX_FAILED_LINES = 100;

  // Run summary and error context (DefaultJob creates a new instance per run)
  private int activeClients = 0;
  private int clientsWithPending = 0;
  private boolean preFiltered = false;
  private int totalPosted = 0;
  private final List<String> failedDocs = new ArrayList<>();
  private final Map<String, String> statusNames = new HashMap<>();
  private boolean hadErrors = false;
  private boolean clientHeaderLogged = false;
  private final List<String> notPostableClients = new ArrayList<>();
  private String currentClient = "";
  private String currentTable = "";

  private ProcessLogger logger;
  private ConnectionProvider connection;

  static Logger log4j = LogManager.getLogger();

  @Override
  public void doExecute(ProcessBundle bundle) throws Exception {

    logger = bundle.getLogger();
    connection = bundle.getConnection();
    isDirect = bundle.getChannel() == Channel.DIRECT;
    final long start = System.currentTimeMillis();

    VariablesSecureApp vars = bundle.getContext().toVars();
    try {
      if (vars.getClient().equals(SYSTEM_CLIENT_ID)) {
        processAllClients(bundle);
      } else {
        activeClients = 1;
        processClient(vars, bundle, null,
            clientLabel(OBDal.getInstance().get(Client.class, vars.getClient())));
      }
    } catch (Exception ex) {
      // DefaultJob only sends the exception to log4j: write it where the Process Monitor shows it
      addLog("ERROR: " + currentClient + " / " + currentTable + ": " + ex.getMessage());
      throw ex;
    } finally {
      if (!isDirect) {
        addSummary(System.currentTimeMillis() - start);
      }
    }
    // "Posting by DB tables" shows the log only when the process ends normally
    if (hadErrors && !isDirect) {
      throw new OBException("Accounting finished with errors, see the process log");
    }
  }

  private void processAllClients(ProcessBundle bundle) throws Exception {
    currentClient = "System";
    currentTable = "active clients";
    final OBCriteria<Client> obc = OBDal.getInstance().createCriteria(Client.class);
    obc.add(Restrictions.not(Restrictions.eq(Client.PROPERTY_ID, SYSTEM_CLIENT_ID)));
    final List<Client> clients = obc.list();
    activeClients = clients.size();
    // The direct channel ("Posting by DB tables") keeps walking every client
    Map<String, Set<String>> pendingTables = null;
    if (!isDirect) {
      final Set<String> clientIds = new HashSet<>();
      for (Client c : clients) {
        clientIds.add(c.getId());
      }
      currentTable = "pending documents check";
      pendingTables = clientsWithPendingDocs(connection, selectAcctTableIds(), clientIds);
      preFiltered = true;
    }
    for (Client c : clients) {
      final Set<String> clientTables = pendingTables == null ? null : pendingTables.get(c.getId());
      if (pendingTables == null || clientTables != null) {
        if (clientTables != null) {
          clientsWithPending++;
        }
        final VariablesSecureApp clientVars = new VariablesSecureApp(
            bundle.getContext().getUser(), c.getId(), bundle.getContext().getOrganization());
        processClient(clientVars, bundle, clientTables, clientLabel(c));
      }
    }
  }

  private String[] selectAcctTableIds() throws ServletException {
    final AcctServerProcessData[] data = AcctServerProcessData.selectAcctTable(connection);
    final String[] ids = new String[data.length];
    for (int i = 0; i < data.length; i++) {
      ids[i] = data[i].adTableId;
    }
    return ids;
  }

  /**
   * Returns, for each of the given clients, the accounting tables that hold at least one document
   * pending to be posted. It is a superset of what {@link AcctServer#checkDocuments} finds (no
   * organization, period or date filter), so skipping the other (client, table) pairs never skips
   * a document.
   *
   * A table whose query fails is kept for every client, as it was before this pre-filter existed.
   */
  static Map<String, Set<String>> clientsWithPendingDocs(ConnectionProvider cp,
      String[] tableIds, Set<String> clientIds) throws NoConnectionAvailableException,
      SQLException {
    final Map<String, Set<String>> result = new HashMap<>();
    final Connection conn = cp.getTransactionConnection();
    try {
      for (String tableId : tableIds) {
        Set<String> pendingClients;
        try {
          pendingClients = selectClientsWithPendingDocs(conn, tableId);
        } catch (SQLException ex) {
          if (isConnectionFailure(ex)) {
            throw ex;
          }
          log4j.warn(
              "Pending documents pre-filter failed for table {}, walking it for every client",
              tableId, ex);
          conn.rollback();
          pendingClients = clientIds;
        }
        for (String clientId : pendingClients) {
          if (clientIds.contains(clientId)) {
            result.computeIfAbsent(clientId, k -> new LinkedHashSet<>()).add(tableId);
          }
        }
      }
    } finally {
      try {
        cp.releaseRollbackConnection(conn);
      } catch (SQLException ex) {
        // Do not hide the exception that ended the loop, if any
        log4j.warn("Could not release the pre-filter connection", ex);
      }
    }
    return result;
  }

  private static Set<String> selectClientsWithPendingDocs(Connection conn, String tableId)
      throws SQLException {
    String tableName = null;
    try (PreparedStatement ps = conn
        .prepareStatement("SELECT tablename FROM ad_table WHERE ad_table_id = ?")) {
      ps.setString(1, tableId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          tableName = rs.getString(1);
        }
      }
    }
    if (tableName == null || !TABLE_NAME.matcher(tableName).matches()) {
      // Handled by the caller's fallback: the table is walked for every client
      throw new SQLException("No valid table name for AD_Table_ID " + tableId);
    }
    final Set<String> clients = new HashSet<>();
    // Same pending condition as AcctServerData.select. Concatenating is safe: the table name comes
    // from AD_Table and was checked against TABLE_NAME above
    try (PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT ad_client_id FROM "
        + tableName
        + " WHERE posted = 'N' AND processed = 'Y' AND (processing = 'N' OR processing IS NULL)");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        clients.add(rs.getString(1));
      }
    }
    return clients;
  }

  /**
   * A broken connection or pool, or a database shutting down (SQLState 57P*), fails every following
   * table the same way, so the run aborts.
   */
  static boolean isConnectionFailure(Throwable ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (t instanceof NoConnectionAvailableException) {
        return true;
      }
      final String sqlState = t instanceof SQLException ? ((SQLException) t).getSQLState() : null;
      if (sqlState != null && (sqlState.startsWith("08") || sqlState.startsWith("57P"))) {
        return true;
      }
      if (t.getMessage() != null && t.getMessage().contains("@CODE=NoConnectionAvailable")) {
        return true;
      }
    }
    return false;
  }

  private void addSummary(long elapsedMs) {
    addLog("Finished in " + elapsedMs + " ms - active clients: " + activeClients
        + (preFiltered ? " / with pending: " + clientsWithPending : "") + " - posted: "
        + totalPosted + " - failed: " + failedDocs.size());
    if (!notPostableClients.isEmpty()) {
      addLog("Pending but not postable (closed period or organization not ready): "
          + String.join(", ", notPostableClients));
    }
    // shortcut: only the first MAX_FAILED_LINES failed documents are listed, raise it if needed
    for (int i = 0; i < failedDocs.size() && i < MAX_FAILED_LINES; i++) {
      addLog("Failed: " + failedDocs.get(i));
    }
    if (failedDocs.size() > MAX_FAILED_LINES) {
      addLog("Failed: ... and " + (failedDocs.size() - MAX_FAILED_LINES) + " more");
    }
  }

  private static String clientLabel(Client client) {
    return client == null ? "" : client.getId() + " (" + client.getName() + ")";
  }

  /**
   * Posts the pending documents of one client, table by table. A failing table is logged and the
   * remaining tables are still posted; only a lost database connection aborts the run.
   *
   * @param onlyTables
   *          tables to walk, or null to walk every accounting table
   */
  private void processClient(VariablesSecureApp vars, ProcessBundle bundle,
      Set<String> onlyTables, String clientLabel) throws ServletException, IOException {
    final String pinstanceId = bundle.getPinstanceId();
    final ProcessContext ctx = bundle.getContext();
    currentClient = clientLabel.isEmpty() ? vars.getClient() : clientLabel;
    currentTable = "";

    if (log4j.isDebugEnabled()) {
      log4j.debug("Processing client: " + vars.getClient());
    }

    if (isDirect) {
      addLog("@DL_STARTING@", false);
    } else {
      // Written with the first table line, so clients with nothing done stay out of the log
      clientHeaderLogged = false;
    }
    final VariablesSecureApp localVars = vars != null ? vars
        : loadUserOrgVars(bundle.getProcessId(), ctx);
    if (localVars == null || !loadTableIds(ctx)) {
      return;
    }
    String strTable = "";
    // If it is the background process, we use 0
    String strOrg = "0";
    String strDateFrom = "";
    String strDateTo = "";
    // if called by 'Posting by DB tables' get params from ad_pinstance
    if (isDirect) {
      strTable = AcctServerProcessData.selectTable(connection, pinstanceId);
      strOrg = AcctServerProcessData.selectOrg(connection, pinstanceId);
      strDateFrom = AcctServerProcessData.selectDateFrom(connection, pinstanceId);
      strDateTo = AcctServerProcessData.selectDateTo(connection, pinstanceId);
    }
    // If UseRequestOrganizationExecutingRequestProcess preference exists
    // use process Organization
    if (AcctServerProcessData.useRequestProcessOrg(connection)) {
      strOrg = localVars.getOrg();
    }
    for (String table : tablesToWalk(strTable, onlyTables)) {
      currentTable = table;
      try {
        processTable(table, localVars, strOrg, strDateFrom, strDateTo, ctx);
      } catch (Exception ex) {
        if (isConnectionFailure(ex)) {
          throw ex;
        }
        logFailedTable(ex);
      }
    }
    // The pre-filter found pending documents, but none could be selected for posting
    if (onlyTables != null && !isDirect && !clientHeaderLogged) {
      notPostableClients.add(currentClient);
    }
  }

  /** Builds the variables of the process user and organization, or returns null if missing. */
  private VariablesSecureApp loadUserOrgVars(String processId, ProcessContext ctx) {
    try {
      final AcctServerProcessData[] dataOrg = AcctServerProcessData.selectUserOrg(connection,
          processId);
      if (dataOrg == null || dataOrg.length == 0) {
        if (isDirect) {
          addLog("@DL_LOAD_FAILED@");
        } else {
          addLog("User and Organization loading failed.");
        }
        return null;
      }
      return new VariablesSecureApp(dataOrg[0].adUserId, ctx.getClient(), dataOrg[0].adOrgId);
    } catch (final ServletException ex) {
      log4j.error(ex.getMessage());
      return null;
    }
  }

  /** Loads the accounting tables of the context into TableIds; false if they cannot be read. */
  private boolean loadTableIds(ProcessContext ctx) {
    try {
      final AcctServerProcessData[] data;
      if ("0".equals(ctx.getClient())) {
        data = AcctServerProcessData.selectAcctTable(connection);
      } else if ("0".equals(ctx.getOrganization())) {
        data = AcctServerProcessData.selectAcctTable(connection, ctx.getClient());
      } else {
        data = AcctServerProcessData.selectAcctTable(connection, ctx.getClient(),
            ctx.getOrganization());
      }
      TableIds = new String[data.length];
      for (int i = 0; i < data.length; i++) {
        TableIds[i] = data[i].adTableId;
      }
      return true;
    } catch (final ServletException ex) {
      log4j.error(ex.getMessage());
      return false;
    }
  }

  private String[] tablesToWalk(String strTable, Set<String> onlyTables) {
    if (!strTable.equals("")) {
      return new String[] { strTable };
    }
    return onlyTables != null ? onlyTables.toArray(new String[0]) : TableIds;
  }

  /** Logs a table that threw; the remaining tables are still posted and the run ends as ERROR. */
  private void logFailedTable(Exception ex) {
    log4j.error(ex.getMessage(), ex);
    hadErrors = true;
    logClientHeader();
    addLog("ERROR: " + currentClient + " / " + currentTable + ": " + ex.getMessage());
    // Drop what the failed table left in the DAL session, so the next posted document's commit
    // does not persist it. Documents posted so far are already committed one by one
    try {
      OBDal.getInstance().rollbackAndClose();
    } catch (Exception rollbackEx) {
      // A broken session must not stop the remaining tables: the next one opens a new one
      log4j.error("Could not discard the DAL session after a failed table", rollbackEx);
    }
  }

  private void logClientHeader() {
    if (!isDirect && !clientHeaderLogged) {
      addLog("Client " + currentClient);
      clientHeaderLogged = true;
    }
  }

  private void processTable(String tableId, VariablesSecureApp localVars, String strOrg,
      String strDateFrom, String strDateTo, ProcessContext ctx)
      throws ServletException, IOException {
    final AcctServer acct = AcctServer.get(tableId, localVars.getClient(), strOrg, connection);
    if (acct == null) {
      return;
    }
    acct.setBatchSize(BATCH_SIZE);
    // Sets execution as background
    acct.setBackground(true);
    final String strTableDesc = AcctServerProcessData.selectDescription(connection,
        ctx.getLanguage(), acct.AD_Table_ID);
    currentTable = strTableDesc;
    try {
      int total = 0;
      int idleBatches = 0;
      while (acct.checkDocuments(strDateFrom, strDateTo)) {

        // The background log only gets the per-table result below
        if (isDirect) {
          if (total == 0) {
            addLog("@DL_ACCOUNTING@ - " + strTableDesc, false);
          } else {
            addLog("@DL_COUNTED@ " + total + " - " + strTableDesc, false);
          }
        }

        final int successBefore = acct.getSuccess();
        final int failedBefore = acct.getFailedDocs().size();
        acct.run(localVars, strDateFrom, strDateTo);
        if (acct.getSuccess() != successBefore || acct.getFailedDocs().size() != failedBefore) {
          idleBatches = 0;
        } else if (++idleBatches > 1) {
          // One idle batch can be a lock lost to a concurrent posting, so it is retried. Two in a
          // row mean documents left at posted='N' that would be selected again forever: leave them
          log4j.warn("No progress posting {} for client {}", strTableDesc, currentClient);
          logClientHeader();
          addLog("  " + strTableDesc + ": no progress" + blockedDocText(acct, ctx.getLanguage())
              + ", remaining documents left for the next run");
          break;
        }

        total += Integer.valueOf(BATCH_SIZE).intValue();
      }
    } finally {
      // Documents of earlier batches are already committed: count and list them even if a later
      // batch threw
      logTableResult(acct, strTableDesc, ctx.getLanguage());
    }
  }

  private void logTableResult(AcctServer acct, String strTableDesc, String language) {
    totalPosted += acct.getSuccess();
    final int failedCount = acct.getFailedDocs().size();
    if (isDirect) {
      addLog("@DL_TABLE@ = " + strTableDesc + " - " + acct.getInfo(language), false);
    } else if (acct.getSuccess() > 0 || failedCount > 0) {
      logClientHeader();
      addLog("  " + strTableDesc + ": posted " + acct.getSuccess()
          + (failedCount > 0 ? ", failed " + failedCount : ""));
      collectFailedDocs(acct, strTableDesc, language);
    }
  }

  /** Names the document that ended the last batch early, when posting reported one. */
  private String blockedDocText(AcctServer acct, String language) {
    final String[] blocked = acct.getBlockedDoc();
    if (blocked == null) {
      return "";
    }
    final String status = blocked[2];
    final String detail;
    if (blocked[3] != null) {
      detail = ": " + blocked[3];
    } else if (status != null && !status.isEmpty()) {
      detail = " / " + status + postedStatusName(status, language);
    } else {
      detail = "";
    }
    return ", stopped at " + documentLabel(acct.AD_Table_ID, blocked[0], blocked[1]) + detail;
  }

  private void collectFailedDocs(AcctServer acct, String strTableDesc, String language) {
    for (Map.Entry<String, String[]> failed : acct.getFailedDocs().entrySet()) {
      final String status = failed.getValue()[1];
      // Lines past the cap are only counted, so skip the document lookup for them
      final String document = failedDocs.size() < MAX_FAILED_LINES
          ? documentLabel(acct.AD_Table_ID, failed.getKey(), failed.getValue()[0])
          : failed.getKey();
      failedDocs.add(currentClient + " / " + strTableDesc + " / " + document + " / " + status
          + postedStatusName(status, language));
    }
  }

  /**
   * Returns "document (record id)". Not every document type loads its number while posting (e.g.
   * goods shipments), so it falls back to the record identifier, or to the bare id.
   */
  String documentLabel(String tableId, String recordId, String documentNo) {
    if (documentNo != null && !documentNo.isEmpty()) {
      return documentNo + " (" + recordId + ")";
    }
    OBContext.setAdminMode(true);
    try {
      final Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
      final BaseOBObject document = entity == null ? null
          : OBDal.getInstance().get(entity.getName(), recordId);
      return document == null ? recordId : document.getIdentifier() + " (" + recordId + ")";
    } catch (Exception ex) {
      log4j.debug("Could not read the identifier of {}", recordId, ex);
      return recordId;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Returns " - " plus the name of a posted status (e.g. "Cost Not Calculated" for NC), or an
   * empty string when it cannot be resolved. Names are cached for the run.
   */
  String postedStatusName(String status, String language) {
    return statusNames.computeIfAbsent(String.valueOf(status), s -> {
      try {
        // "All_Posted Status" holds the Post button labels, e.g. "Post: Cost Not Calculated"
        final String name = Utility.getListValueName("All_Posted Status", s, language);
        if (name == null || name.equals(s)) {
          return "";
        }
        final int sep = name.indexOf(": ");
        return " - " + (sep >= 0 ? name.substring(sep + 2) : name);
      } catch (Exception ex) {
        log4j.debug("Could not resolve the name of posted status {}", s, ex);
        return "";
      }
    });
  }

  /**
   * Adds a message to the log.
   * 
   * @param msg
   *          to add to the log
   */
  private void addLog(String msg) {
    addLog(msg, true);
  }

  /**
   * Add a message to the log.
   * 
   * @param msg
   * @param generalLog
   */
  private void addLog(String msg, boolean generalLog) {
    logger.log(msg + "\n");
    final Timestamp tmp = new Timestamp(System.currentTimeMillis());
    if (isDirect) {
      lastLog.append("<span>").append(msg).append("</span><br>");
    } else {
      if (generalLog) {
        this.message.append(tmp.toString()).append(" - ").append(msg).append("<br>");
      }
      lastLog.append("<span>")
          .append(tmp.toString())
          .append(" - ")
          .append(msg)
          .append("</span><br>");
    }
  }
}
