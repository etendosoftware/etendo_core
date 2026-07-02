/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF  ANY  KIND,  either  express  or  implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Background process that drains the asynchronous stored-computed-column queue.
 *
 * <p>Each invocation fetches up to {@code Max Records} of the oldest queued ({@code REFRESH_MODE='Q'})
 * dirty rows that are not dead-lettered ({@code IS_IGNORED='N'}) and processes each one in its own
 * database transaction, committing after every row. Concurrent drainers are supported (spec §5.5):
 * the fetch claims a disjoint batch with {@code FOR UPDATE SKIP LOCKED}, so two runs never contend on
 * the same rows; the recompute is idempotent and the dirty row is deleted by id, so even a rare
 * overlap is harmless.</p>
 *
 * <p>Sentinel rows ({@code TARGET_RECORD_ID IS NULL}) request a full column rebuild. The batch is
 * processed in a single pass honouring the {@code computation_sequence_number} order of the fetch: a
 * sentinel rebuilds its column (looping the same per-target recompute over every primary key of the
 * column's target table), and any sibling per-target rows for that column later in the batch are
 * dropped because the rebuild already covered every row.</p>
 *
 * <p><b>Recompute runs in Java</b> (Phase 5, workstream C2), delegated to the shared
 * {@link StoredColumnRecomputer}. The per-row recompute is issued directly on this processor's
 * connection as two statements — a {@code FOR UPDATE} lock followed by an
 * {@code UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?} — instead of calling the PL/pgSQL
 * {@code ad_scd_recompute}, and a sentinel rebuild loops that recompute over every target row (the
 * Java equivalent of {@code ad_scd_rebuild}). That single dialect-neutral engine serves both
 * PostgreSQL and Oracle, so Oracle needs zero engine PL/SQL functions; PostgreSQL keeps PL/pgSQL
 * {@code ad_scd_recompute} only for its in-transaction {@code 'S'} drain. The write is unconditional
 * (no {@code IS DISTINCT FROM} guard), matching the PL/pgSQL engine.</p>
 *
 * <p>Every row is processed in its own transaction. On success the recompute and the dirty-row delete
 * commit together. On <b>any</b> failure the row's transaction is rolled back and the failure is
 * recorded in a <b>separate</b> transaction — incrementing {@code RETRY_COUNT}, storing
 * {@code ERROR_MSG}, and dead-lettering the row ({@code IS_IGNORED='Y'}) once the retry threshold is
 * reached — so a poison row neither aborts the batch nor blocks its siblings. A rolled-back
 * transaction cannot itself persist the failure (PostgreSQL aborts it), which is why the retry/error
 * bookkeeping runs in a fresh transaction. The cause is logged at {@code ERROR} with its stack trace
 * so the failure is never silent.</p>
 *
 * <p>On PostgreSQL the engine recomputes synchronously inside the row's transaction, so its own
 * writes to watched target tables must not re-enqueue work; the {@code my.scd_refreshing} GUC is set
 * for the transaction to suppress that recursion (Phase 5, workstream C3). Oracle drains every column
 * asynchronously, so its enqueue trigger only writes the dirty queue and needs no such guard.</p>
 */
public class StoredColumnQueueProcessor implements Process {

  private static final Logger log4j = LogManager.getLogger();

  /** Fallbacks used when the process parameters are absent (e.g. invoked without a bundle). */
  private static final int DEFAULT_MAX_RECORDS = 100;
  private static final int DEFAULT_RETRY_THRESHOLD = 5;

  /** Maximum length persisted in AD_STOREDCOLUMN_DIRTY.ERROR_MSG. */
  private static final int ERROR_MSG_MAX = 4000;

  /**
   * Fetches the oldest non-dead-lettered queued rows for this client (plus the System-owned
   * full-rebuild sentinels). Concurrent drainers are supported: {@code FOR UPDATE SKIP LOCKED} makes
   * each drainer claim a disjoint batch by skipping rows another drainer's fetch has already locked
   * (spec §5.5). SKIP LOCKED is supported by both PostgreSQL and Oracle 12c+ and composes with the
   * {@code ORDER BY} and JDBC {@code setMaxRows} batch bound, so a single dialect-neutral statement
   * serves both. Processing stays idempotent and rows are deleted by id, so even a rare overlap is
   * harmless.
   */
  private static final String FETCH_SQL = "SELECT ad_storedcolumn_dirty_id, ad_column_id, target_record_id"
      + " FROM ad_storedcolumn_dirty"
      + " WHERE refresh_mode = 'Q' AND is_ignored = 'N'"
      // Drain this client's own dirty rows plus the System-owned ('0') full-rebuild sentinels, which
      // recompute the whole table across all clients and so must be picked up by any client's run.
      + " AND ad_client_id IN (?, '0')"
      + " ORDER BY computation_sequence_number ASC, created ASC"
      + " FOR UPDATE SKIP LOCKED";

  private static final String DELETE_SQL = "DELETE FROM ad_storedcolumn_dirty WHERE ad_storedcolumn_dirty_id = ?";

  /**
   * Dead-letter update, dialect-specific in two spots: the error-message truncation ({@code left} on
   * PostgreSQL, {@code SUBSTR} on Oracle) and the update timestamp ({@code now()} vs {@code SYSDATE}).
   * Both variants bind the same three parameters (message, retry threshold, dirty-row id).
   */
  private static final String FAIL_SQL_PG = "UPDATE ad_storedcolumn_dirty"
      + " SET retry_count = retry_count + 1,"
      + "     error_msg = left(?, " + ERROR_MSG_MAX + "),"
      + "     is_ignored = CASE WHEN retry_count + 1 >= ? THEN 'Y' ELSE 'N' END,"
      + "     updated = now()"
      + " WHERE ad_storedcolumn_dirty_id = ?";

  private static final String FAIL_SQL_ORACLE = "UPDATE ad_storedcolumn_dirty"
      + " SET retry_count = retry_count + 1,"
      + "     error_msg = SUBSTR(?, 1, " + ERROR_MSG_MAX + "),"
      + "     is_ignored = CASE WHEN retry_count + 1 >= ? THEN 'Y' ELSE 'N' END,"
      + "     updated = SYSDATE"
      + " WHERE ad_storedcolumn_dirty_id = ?";

  /** True when the underlying RDBMS is Oracle; selects the dialect-specific claim/dead-letter SQL. */
  private boolean oracle;

  /** Shared Java recompute engine (metadata lookup, per-row recompute, rebuild, recursion guard). */
  private StoredColumnRecomputer recomputer;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    int maxRecords = intParam(bundle, "Max_Records", DEFAULT_MAX_RECORDS);
    int retryThreshold = intParam(bundle, "Retry_Threshold", DEFAULT_RETRY_THRESHOLD);

    ConnectionProvider conn = bundle.getConnection();
    oracle = "ORACLE".equals(conn.getRDBMS());
    recomputer = new StoredColumnRecomputer(oracle);
    String clientId = bundle.getContext().getClient();
    Connection con = conn.getTransactionConnection();
    int processed = 0;
    int rebuilt = 0;
    int failed = 0;
    try {
      List<DirtyRow> batch = fetchBatch(con, maxRecords, clientId);
      // Close the read transaction opened by the fetch before the per-row transactions begin.
      con.commit();

      // Single pass in the batch's computation_sequence_number order, so a low-seq column is always
      // recomputed before a higher-seq column that depends on it. A sentinel (targetId == null)
      // rebuilds its whole column; any per-target row for a column already rebuilt by an earlier
      // sentinel in this batch is dropped because the rebuild covered every row.
      Set<String> rebuiltColumns = new HashSet<>();
      for (DirtyRow row : batch) {
        if (row.targetId == null) {
          if (processRow(con, row, retryThreshold, true)) {
            rebuiltColumns.add(row.columnId);
            rebuilt++;
          } else {
            failed++;
          }
        } else if (rebuiltColumns.contains(row.columnId)) {
          continue;
        } else if (processRow(con, row, retryThreshold, false)) {
          processed++;
        } else {
          failed++;
        }
      }

      // Every row already committed (or rolled back) on its own; nothing pending to commit here.
      conn.releaseCommitConnection(con);
    } catch (Exception e) {
      log4j.error("Stored computed column queue processing failed", e);
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception rollbackEx) {
        log4j.error("Could not roll back stored-column queue transaction", rollbackEx);
      }
      throw new OBException("Stored computed column queue processing failed", e);
    }

    String summary = "Stored column queue: " + processed + " recomputed, " + rebuilt
        + " rebuilt, " + failed + " failed";
    if (logger != null) {
      logger.logln(summary);
    }
    log4j.info(summary);
  }

  /**
   * Fetches the oldest queued rows for this client plus the System-owned rebuild sentinels. Uses
   * {@code FOR UPDATE SKIP LOCKED} so concurrent drainers claim disjoint batches; the batch size is
   * bounded via JDBC {@code setMaxRows}.
   */
  private List<DirtyRow> fetchBatch(Connection con, int maxRecords, String clientId)
      throws SQLException {
    List<DirtyRow> rows = new ArrayList<>();
    // Drain dirty rows owned by the client this process is running in (the enqueue triggers stamp
    // AD_CLIENT_ID from the source record, partitioning the queue per client) plus the System-owned
    // ('0') full-rebuild sentinels, which span all clients and must run under any client's drain.
    try (PreparedStatement ps = con.prepareStatement(FETCH_SQL)) {
      ps.setMaxRows(maxRecords);
      ps.setString(1, clientId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(new DirtyRow(rs.getString(1), rs.getString(2), rs.getString(3)));
        }
      }
    }
    return rows;
  }

  /**
   * Processes one dirty row in its own transaction: engages the recursion guard, recomputes the row
   * (a full column rebuild when {@code sentinel}, otherwise a single-target recompute), deletes the
   * dirty row, and commits. On <b>any</b> failure — including non-{@code SQLException}s from metadata
   * lookup — the row's transaction is rolled back and the failure is recorded and dead-lettered in a
   * separate committed transaction, so one poison row neither aborts the batch nor blocks its
   * siblings. The cause is logged at {@code ERROR} with its stack trace. Returns true on success.
   */
  private boolean processRow(Connection con, DirtyRow row, int retryThreshold, boolean sentinel) {
    try {
      // Suppress re-entrant enqueues caused by the engine's own synchronous writes within this row's
      // transaction (PostgreSQL only; no-op on Oracle, which drains asynchronously).
      recomputer.setRefreshingGuard(con);
      if (sentinel) {
        recomputer.rebuild(con, row.columnId);
      } else {
        recomputer.recomputeOne(con, row.columnId, row.targetId);
      }
      deleteRow(con, row.id);
      con.commit();
      return true;
    } catch (Exception e) {
      log4j.error("Stored column recompute failed (ad_column_id={}, target_record_id={})",
          row.columnId, row.targetId, e);
      recordFailure(con, row, retryThreshold, e);
      return false;
    }
  }

  private void deleteRow(Connection con, String dirtyId) throws SQLException {
    try (PreparedStatement ps = con.prepareStatement(DELETE_SQL)) {
      ps.setString(1, dirtyId);
      ps.executeUpdate();
    }
  }

  /**
   * Records a processing failure in a fresh transaction: rolls back the aborted recompute transaction,
   * then increments the retry counter, stores the error message, and dead-letters the row once the
   * threshold is reached. Runs on its own transaction because a failed (PostgreSQL-aborted) transaction
   * cannot persist the bookkeeping. Never throws: a failure to record is logged and swallowed so the
   * batch continues with the remaining rows.
   */
  private void recordFailure(Connection con, DirtyRow row, int retryThreshold, Exception cause) {
    try {
      con.rollback();
    } catch (SQLException e) {
      log4j.error("Could not roll back failed stored-column recompute (id={})", row.id, e);
    }
    try {
      // The PostgreSQL recompute guard is a transaction-local GUC released by the rollback above, so
      // no explicit clear is needed here before recording the failure.
      String message = messageOf(cause);
      try (PreparedStatement ps = con.prepareStatement(oracle ? FAIL_SQL_ORACLE : FAIL_SQL_PG)) {
        ps.setString(1, message);
        ps.setInt(2, retryThreshold);
        ps.setString(3, row.id);
        ps.executeUpdate();
      }
      con.commit();
    } catch (SQLException e) {
      log4j.error("Could not record stored-column failure (id={})", row.id, e);
      try {
        con.rollback();
      } catch (SQLException rollbackEx) {
        log4j.error("Could not roll back stored-column failure record (id={})", row.id, rollbackEx);
      }
    }
  }

  /** Non-null failure message for {@code ERROR_MSG}: the exception message, or its class name. */
  private static String messageOf(Exception cause) {
    String message = cause.getMessage();
    if (message == null || message.isEmpty()) {
      message = cause.getClass().getName();
    }
    return message;
  }

  /** Reads an integer process parameter, falling back when the bundle or value is absent. */
  private int intParam(ProcessBundle bundle, String name, int fallback) {
    if (bundle == null) {
      return fallback;
    }
    Map<String, Object> params = bundle.getParams();
    if (params == null) {
      return fallback;
    }
    Object raw = params.get(name);
    if (raw == null) {
      return fallback;
    }
    try {
      if (raw instanceof Number) {
        return ((Number) raw).intValue();
      }
      String text = raw.toString().trim();
      if (text.isEmpty()) {
        return fallback;
      }
      return (int) Double.parseDouble(text);
    } catch (NumberFormatException e) {
      log4j.warn("Invalid value '{}' for parameter {}, using default {}", raw, name, fallback);
      return fallback;
    }
  }

  /** Immutable view of a claimed dirty row. {@code targetId} is null for rebuild sentinels. */
  private static final class DirtyRow {
    private final String id;
    private final String columnId;
    private final String targetId;

    private DirtyRow(String id, String columnId, String targetId) {
      this.id = id;
      this.columnId = columnId;
      this.targetId = targetId;
    }
  }
}
