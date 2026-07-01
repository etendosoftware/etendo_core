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

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.HashMap;
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
 * processed first by looping the same per-target recompute over every primary key of the column's
 * target table; once a sentinel succeeds, any sibling per-target rows claimed in the same batch for
 * that column are dropped because the rebuild already covered every row.</p>
 *
 * <p><b>Recompute runs in Java</b> (Phase 5, workstream C2). The per-row recompute is issued
 * directly on this processor's connection as two statements — a {@code FOR UPDATE} lock followed by
 * an {@code UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?} — instead of calling the PL/pgSQL
 * {@code ad_scd_recompute}. The stored-column metadata ({@code table}, {@code col}, {@code pk},
 * {@code fn}) is read once from {@code AD_COLUMN}/{@code AD_TABLE} and cached per column. This single
 * Java drain serves both PostgreSQL and Oracle, so Oracle needs zero engine PL/SQL functions;
 * PostgreSQL keeps PL/pgSQL {@code ad_scd_recompute} only for its in-transaction {@code 'S'} drain.
 * The write is unconditional (no {@code IS DISTINCT FROM} guard), matching the PL/pgSQL engine.</p>
 *
 * <p>Per-target rows are processed each inside its own JDBC savepoint. A failure rolls the row's
 * savepoint back and increments {@code RETRY_COUNT}, records {@code ERROR_MSG}, and dead-letters the
 * row ({@code IS_IGNORED='Y'}) once the retry threshold is reached — without aborting the rest of the
 * batch.</p>
 *
 * <p>The engine's own writes to watched target tables must not re-enqueue work. On PostgreSQL the
 * {@code my.scd_refreshing} GUC is set for the whole transaction; on Oracle the equivalent global
 * trigger-disable ({@code AD_Disable_Triggers}, checked by every enqueue trigger via
 * {@code AD_IsTriggerEnabled()}) is engaged instead (Phase 5, workstream C3).</p>
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

  /**
   * Resolves the physical target table, stored column, computation function and primary-key column
   * for a stored computed column. Logical AD identifiers are stored mixed-case while the physical
   * objects are lowercase, so every name is {@code lower()}-ed here. Read once per column and cached
   * in {@link #metaCache}. Mirrors the metadata lookup the PL/pgSQL {@code ad_scd_recompute} does.
   */
  private static final String META_SQL = "SELECT lower(t.tablename),"
      + " lower(c.columnname), lower(c.computation_function),"
      + " lower((SELECT k.columnname FROM ad_column k"
      + "         WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y'))"
      + " FROM ad_column c"
      + " JOIN ad_table t ON t.ad_table_id = c.ad_table_id"
      + " WHERE c.ad_column_id = ?";

  /** Per-column recompute metadata, populated lazily within one processor invocation. */
  private final Map<String, ColumnMeta> metaCache = new HashMap<>();

  /** True when the underlying RDBMS is Oracle; selects the dialect-specific recursion guard (C3). */
  private boolean oracle;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    ProcessLogger logger = bundle.getLogger();
    int maxRecords = intParam(bundle, "Max_Records", DEFAULT_MAX_RECORDS);
    int retryThreshold = intParam(bundle, "Retry_Threshold", DEFAULT_RETRY_THRESHOLD);

    ConnectionProvider conn = bundle.getConnection();
    oracle = "ORACLE".equals(conn.getRDBMS());
    metaCache.clear();
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

      clearRefreshingGuard(con);
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

  /**
   * Runs a full column rebuild for a sentinel row by recomputing every primary key of the column's
   * target table, all inside the sentinel's savepoint (replaces {@code ad_scd_rebuild} on the async
   * path). Returns true on success.
   */
  private boolean processSentinel(Connection con, DirtyRow row, int retryThreshold)
      throws SQLException {
    Savepoint sp = con.setSavepoint();
    try {
      ColumnMeta meta = metaFor(con, row.columnId);
      if (meta != null) {
        for (String pk : allTargetIds(con, meta)) {
          recompute(con, meta, pk);
        }
      }
      deleteRow(con, row.id);
      return true;
    } catch (SQLException e) {
      con.rollback(sp);
      recordFailure(con, row, retryThreshold, e);
      return false;
    }
  }

  /**
   * Recomputes a single target row inside its own savepoint (replaces {@code ad_scd_recompute} on the
   * async path — the recompute now runs in Java on this connection). Returns true on success.
   */
  private boolean processTarget(Connection con, DirtyRow row, int retryThreshold)
      throws SQLException {
    Savepoint sp = con.setSavepoint();
    try {
      ColumnMeta meta = metaFor(con, row.columnId);
      if (meta != null) {
        recompute(con, meta, row.targetId);
      }
      deleteRow(con, row.id);
      return true;
    } catch (SQLException e) {
      con.rollback(sp);
      recordFailure(con, row, retryThreshold, e);
      return false;
    }
  }

  /**
   * Recomputes and writes one target row: locks it {@code FOR UPDATE} to serialize concurrent
   * recomputations of the same aggregate, then writes {@code <col> = <fn>(<pk>)} unconditionally.
   * This is the Java equivalent of the PL/pgSQL {@code ad_scd_recompute} and must stay behaviorally
   * identical to it (verified by the parity tests). Table/column/function/pk names come from cached
   * {@code AD_COLUMN} metadata and are lowercased physical identifiers, so they are quoted here.
   */
  private void recompute(Connection con, ColumnMeta meta, String targetId) throws SQLException {
    String q = quoteChar();
    String lockSql = "SELECT 1 FROM " + q + meta.table + q
        + " WHERE " + q + meta.pk + q + " = ? FOR UPDATE";
    try (PreparedStatement ps = con.prepareStatement(lockSql)) {
      ps.setString(1, targetId);
      ps.execute();
    }
    String updateSql = "UPDATE " + q + meta.table + q
        + " SET " + q + meta.column + q + " = " + q + meta.fn + q + "(" + q + meta.pk + q + ")"
        + " WHERE " + q + meta.pk + q + " = ?";
    try (PreparedStatement ps = con.prepareStatement(updateSql)) {
      ps.setString(1, targetId);
      ps.executeUpdate();
    }
  }

  /** Reads (and caches) the recompute metadata for a column; null when the column is not resolvable. */
  private ColumnMeta metaFor(Connection con, String columnId) throws SQLException {
    if (metaCache.containsKey(columnId)) {
      return metaCache.get(columnId);
    }
    ColumnMeta meta = null;
    try (PreparedStatement ps = con.prepareStatement(META_SQL)) {
      ps.setString(1, columnId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          String table = rs.getString(1);
          String column = rs.getString(2);
          String fn = rs.getString(3);
          String pk = rs.getString(4);
          if (table != null && column != null && fn != null && pk != null) {
            meta = new ColumnMeta(table, column, fn, pk);
          }
        }
      }
    }
    if (meta == null) {
      log4j.warn("Stored column {} has incomplete recompute metadata — skipping", columnId);
    }
    metaCache.put(columnId, meta);
    return meta;
  }

  /** Returns every primary-key value of the target table, used to drive a sentinel full rebuild. */
  private List<String> allTargetIds(Connection con, ColumnMeta meta) throws SQLException {
    String q = quoteChar();
    List<String> ids = new ArrayList<>();
    String sql = "SELECT " + q + meta.pk + q + " FROM " + q + meta.table + q;
    try (PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        ids.add(rs.getString(1));
      }
    }
    return ids;
  }

  /** Identifier quote character for the active dialect (both PostgreSQL and Oracle use double quotes). */
  private String quoteChar() {
    return "\"";
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

  /**
   * Engages the dialect-specific recursion guard so the engine's own writes to watched target tables
   * do not re-enqueue dirty rows (Phase 5, workstream C3).
   *
   * <p>On PostgreSQL the transaction-local {@code my.scd_refreshing} GUC is set (SET LOCAL semantics),
   * checked by the {@code ad_scd_dirty_aiu} enqueue trigger. On Oracle there is no per-transaction
   * GUC, so the processor reuses Etendo's existing session-scoped global trigger disable
   * ({@code AD_Disable_Triggers}); every Oracle enqueue trigger short-circuits when
   * {@code AD_IsTriggerEnabled()} returns {@code 'N'}. Both guards are released implicitly when the
   * connection's transaction ends (PostgreSQL) or when the session ends / {@code AD_Enable_Triggers}
   * runs (Oracle); the processor uses its own dedicated connection, so the Oracle disable never leaks
   * into unrelated work.</p>
   */
  private void setRefreshingGuard(Connection con) throws SQLException {
    if (oracle) {
      try (CallableStatement cs = con.prepareCall("{call AD_Disable_Triggers()}")) {
        cs.execute();
      }
    } else {
      try (PreparedStatement ps = con
          .prepareStatement("SELECT set_config('my.scd_refreshing', 'true', true)")) {
        ps.execute();
      }
    }
  }

  /**
   * Releases the recursion guard before commit. PostgreSQL needs nothing (the {@code SET LOCAL} GUC
   * ends with the transaction), but Oracle's {@code AD_Disable_Triggers} inserted a session-status
   * row that would otherwise persist past commit and keep triggers disabled on the shared session, so
   * {@code AD_Enable_Triggers} is called to clear it.
   */
  private void clearRefreshingGuard(Connection con) throws SQLException {
    if (oracle) {
      try (CallableStatement cs = con.prepareCall("{call AD_Enable_Triggers()}")) {
        cs.execute();
      }
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

  /**
   * Cached recompute metadata for one stored computed column: the physical target table, the stored
   * column, its computation function and the primary-key column. All are lowercased physical
   * identifiers (see {@link #META_SQL}).
   */
  private static final class ColumnMeta {
    private final String table;
    private final String column;
    private final String fn;
    private final String pk;

    private ColumnMeta(String table, String column, String fn, String pk) {
      this.table = table;
      this.column = column;
      this.fn = fn;
      this.pk = pk;
    }
  }
}
