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
import java.sql.Savepoint;
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
 * <p>Each invocation runs as a single database transaction. It claims up to {@code Max Records} of
 * the oldest queued ({@code REFRESH_MODE='Q'}) dirty rows that are not dead-lettered
 * ({@code IS_IGNORED='N'}), recomputes them through the generic engine functions, and deletes the
 * rows it processes. Rows are claimed with {@code FOR UPDATE SKIP LOCKED} so several scheduler
 * threads can drain the queue concurrently without contending for the same rows.</p>
 *
 * <p>Sentinel rows ({@code TARGET_RECORD_ID IS NULL}) request a full column rebuild — they are
 * processed first via {@code ad_scd_rebuild(column_id)}; once a sentinel succeeds, any sibling
 * per-target rows claimed in the same batch for that column are dropped because the rebuild already
 * covered every row.</p>
 *
 * <p>Per-target rows are processed each inside its own JDBC savepoint by calling
 * {@code ad_scd_recompute(column_id, target_id)}. A failure rolls the row's savepoint back and
 * increments {@code RETRY_COUNT}, records {@code ERROR_MSG}, and dead-letters the row
 * ({@code IS_IGNORED='Y'}) once the retry threshold is reached — without aborting the rest of the
 * batch.</p>
 *
 * <p>The {@code my.scd_refreshing} GUC is set for the whole transaction so the engine's own writes
 * to watched target tables do not re-enqueue work.</p>
 */
public class StoredColumnQueueProcessor implements Process {

  private static final Logger log4j = LogManager.getLogger();

  /** Fallbacks used when the process parameters are absent (e.g. invoked without a bundle). */
  private static final int DEFAULT_MAX_RECORDS = 100;
  private static final int DEFAULT_RETRY_THRESHOLD = 5;

  /** Maximum length persisted in AD_STOREDCOLUMN_DIRTY.ERROR_MSG. */
  private static final int ERROR_MSG_MAX = 4000;

  private static final String CLAIM_SQL = "SELECT ad_storedcolumn_dirty_id, ad_column_id, target_record_id"
      + " FROM ad_storedcolumn_dirty"
      + " WHERE refresh_mode = 'Q' AND is_ignored = 'N'"
      + " ORDER BY computation_sequence_number ASC, created ASC"
      + " LIMIT ?"
      + " FOR UPDATE SKIP LOCKED";

  private static final String DELETE_SQL = "DELETE FROM ad_storedcolumn_dirty WHERE ad_storedcolumn_dirty_id = ?";

  private static final String FAIL_SQL = "UPDATE ad_storedcolumn_dirty"
      + " SET retry_count = retry_count + 1,"
      + "     error_msg = left(?, " + ERROR_MSG_MAX + "),"
      + "     is_ignored = CASE WHEN retry_count + 1 >= ? THEN 'Y' ELSE 'N' END,"
      + "     updated = now()"
      + " WHERE ad_storedcolumn_dirty_id = ?";

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    int maxRecords = intParam(bundle, "Max_Records", DEFAULT_MAX_RECORDS);
    int retryThreshold = intParam(bundle, "Retry_Threshold", DEFAULT_RETRY_THRESHOLD);

    ConnectionProvider conn = bundle.getConnection();
    Connection con = conn.getTransactionConnection();
    int processed = 0;
    int rebuilt = 0;
    int failed = 0;
    try {
      // Suppress re-entrant enqueues caused by the engine's own writes during this transaction.
      setRefreshingGuard(con);

      List<DirtyRow> batch = claim(con, maxRecords);

      // Sentinel-first: full rebuilds cover every target row of their column.
      Set<String> rebuiltColumns = new HashSet<>();
      for (DirtyRow row : batch) {
        if (row.targetId == null) {
          if (processSentinel(con, row, retryThreshold)) {
            rebuiltColumns.add(row.columnId);
            rebuilt++;
          } else {
            failed++;
          }
        }
      }

      // Per-target rows, each isolated in its own savepoint.
      for (DirtyRow row : batch) {
        if (row.targetId == null || rebuiltColumns.contains(row.columnId)) {
          continue;
        }
        if (processTarget(con, row, retryThreshold)) {
          processed++;
        } else {
          failed++;
        }
      }

      conn.releaseCommitConnection(con);
    } catch (Exception e) {
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
    log4j.debug(summary);
  }

  /** Claims the oldest queued rows for this transaction with SKIP LOCKED. */
  private List<DirtyRow> claim(Connection con, int maxRecords) throws SQLException {
    List<DirtyRow> rows = new ArrayList<>();
    try (PreparedStatement ps = con.prepareStatement(CLAIM_SQL)) {
      ps.setInt(1, maxRecords);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(new DirtyRow(rs.getString(1), rs.getString(2), rs.getString(3)));
        }
      }
    }
    return rows;
  }

  /** Runs a full column rebuild for a sentinel row; returns true on success. */
  private boolean processSentinel(Connection con, DirtyRow row, int retryThreshold)
      throws SQLException {
    Savepoint sp = con.setSavepoint();
    try {
      try (PreparedStatement ps = con.prepareStatement("SELECT ad_scd_rebuild(?)")) {
        ps.setString(1, row.columnId);
        ps.execute();
      }
      deleteRow(con, row.id);
      return true;
    } catch (SQLException e) {
      con.rollback(sp);
      recordFailure(con, row, retryThreshold, e);
      return false;
    }
  }

  /** Recomputes a single target row inside its own savepoint; returns true on success. */
  private boolean processTarget(Connection con, DirtyRow row, int retryThreshold)
      throws SQLException {
    Savepoint sp = con.setSavepoint();
    try {
      try (PreparedStatement ps = con.prepareStatement("SELECT ad_scd_recompute(?, ?)")) {
        ps.setString(1, row.columnId);
        ps.setString(2, row.targetId);
        ps.execute();
      }
      deleteRow(con, row.id);
      return true;
    } catch (SQLException e) {
      con.rollback(sp);
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
   * Records a processing failure on the dirty row: increments the retry counter, stores the error
   * message, and dead-letters the row once the threshold is reached. Logged at WARN when
   * dead-lettered so operators can investigate.
   */
  private void recordFailure(Connection con, DirtyRow row, int retryThreshold, SQLException cause)
      throws SQLException {
    String message = cause.getMessage();
    try (PreparedStatement ps = con.prepareStatement(FAIL_SQL)) {
      ps.setString(1, message);
      ps.setInt(2, retryThreshold);
      ps.setString(3, row.id);
      ps.executeUpdate();
    }
    log4j.warn("Stored column recompute failed (ad_column_id={}, target_record_id={}): {}",
        row.columnId, row.targetId, message);
  }

  /** Sets the recursion guard for the whole transaction (SET LOCAL semantics). */
  private void setRefreshingGuard(Connection con) throws SQLException {
    try (PreparedStatement ps = con.prepareStatement("SELECT set_config('my.scd_refreshing', 'true', true)")) {
      ps.execute();
    }
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
