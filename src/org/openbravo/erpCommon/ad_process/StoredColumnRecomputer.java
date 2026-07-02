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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Dialect-neutral Java recompute engine for stored computed columns (Phase 5, workstream C2).
 *
 * <p>Shared by both the asynchronous {@link StoredColumnQueueProcessor} and the manual
 * {@link StoredColumnRebuild} process so a single implementation drives every recompute path on both
 * PostgreSQL and Oracle. It issues the recompute as plain JDBC — a {@code FOR UPDATE} lock followed
 * by {@code UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?} — instead of calling the PL/pgSQL
 * {@code ad_scd_recompute}/{@code ad_scd_rebuild}. This is what lets Oracle run without any engine
 * PL/SQL functions (none are deployed there): the per-column computation function {@code <fn>} is the
 * only database object involved, and it exists on whichever RDBMS the column targets.</p>
 *
 * <p>Stored-column metadata ({@code table}, {@code col}, {@code pk}, {@code fn}) is read once from
 * {@code AD_COLUMN}/{@code AD_TABLE} and cached per column for the lifetime of the instance, so a
 * single processor pass or rebuild reads it at most once. Instances are therefore <b>not</b>
 * thread-safe and must not be shared across transactions.</p>
 *
 * <p>On PostgreSQL the engine recomputes synchronously ({@code 'S'} columns) inside the enqueuing
 * transaction, so its own writes to watched target tables could re-fire the enqueue triggers and loop
 * forever; callers guard the recompute with {@link #setRefreshingGuard(Connection)}, which sets the
 * transaction-local {@code my.scd_refreshing} GUC that the enqueue trigger checks (Phase 5, workstream
 * C3). Oracle forces every column to the async {@code 'Q'} drain, so the enqueue trigger only writes
 * the dirty queue and cannot recurse within a transaction — no guard is engaged there.</p>
 */
class StoredColumnRecomputer {

  private static final Logger log4j = LogManager.getLogger();

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

  /** True when the underlying RDBMS is Oracle; selects the dialect-specific recursion guard (C3). */
  private final boolean oracle;

  /** Per-column recompute metadata, populated lazily and cached for the instance lifetime. */
  private final Map<String, ColumnMeta> metaCache = new HashMap<>();

  StoredColumnRecomputer(boolean oracle) {
    this.oracle = oracle;
  }

  /**
   * Engages the PostgreSQL recursion guard so the engine's own synchronous writes to watched target
   * tables do not re-enqueue dirty rows and loop forever within the same transaction.
   *
   * <p>Sets the transaction-local {@code my.scd_refreshing} GUC (SET LOCAL semantics), checked by the
   * per-dependency {@code ad_scd_*_trf} enqueue functions; it is released implicitly when the
   * transaction ends. No-op on Oracle: every column is drained asynchronously ({@code 'Q'} mode) so
   * the enqueue trigger only writes the dirty queue and cannot recurse inside a transaction, and the
   * enqueue trigger deliberately ignores the global trigger-disable so imports and record cloning stay
   * consistent.</p>
   */
  void setRefreshingGuard(Connection con) throws SQLException {
    if (oracle) {
      return;
    }
    try (PreparedStatement ps = con
        .prepareStatement("SELECT set_config('my.scd_refreshing', 'true', true)")) {
      ps.execute();
    }
  }

  /**
   * Recomputes a single target row: resolves (and caches) the column metadata, then writes
   * {@code <col> = <fn>(<pk>)}. No-op when the column has incomplete metadata. Returns true when a
   * recompute was issued.
   */
  boolean recomputeOne(Connection con, String columnId, String targetId) throws SQLException {
    ColumnMeta meta = metaFor(con, columnId);
    if (meta == null) {
      return false;
    }
    recompute(con, meta, targetId);
    return true;
  }

  /**
   * Full idempotent rebuild of one stored computed column: recomputes every primary key of the
   * column's target table. This is the Java equivalent of the PL/pgSQL {@code ad_scd_rebuild} (a
   * per-row recompute loop) and serves both the async sentinel path and the manual rebuild process.
   * Returns the number of target rows recomputed (0 when the column has incomplete metadata).
   */
  int rebuild(Connection con, String columnId) throws SQLException {
    ColumnMeta meta = metaFor(con, columnId);
    if (meta == null) {
      return 0;
    }
    int count = 0;
    for (String pk : allTargetIds(con, meta)) {
      recompute(con, meta, pk);
      count++;
    }
    return count;
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

  /** Returns every primary-key value of the target table, used to drive a full rebuild. */
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
