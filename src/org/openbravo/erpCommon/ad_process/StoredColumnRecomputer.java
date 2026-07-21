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
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
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

  /** Keyset-pagination page size for a full rebuild — bounds the PKs held in memory at once. */
  private static final int REBUILD_CHUNK_SIZE = 1000;

  /**
   * Resolves the physical target table, stored column, computation function and primary-key column
   * for a stored computed column. Logical AD identifiers are stored mixed-case while the physical
   * objects are lowercase on PostgreSQL but UPPERCASE on Oracle, so the RAW identifiers are read here
   * and case-folded per dialect in {@link #metaFor(Connection, String)} before quoting. Read once per
   * column and cached in {@link #metaCache}. Mirrors the metadata lookup the PL/pgSQL
   * {@code ad_scd_recompute} does.
   */
  private static final String META_SQL = "SELECT t.tablename,"
      + " c.columnname, c.computation_function,"
      + " (SELECT k.columnname FROM ad_column k"
      + "         WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y')"
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
   * Full idempotent rebuild of one stored computed column across <b>all</b> clients: recomputes every
   * primary key of the column's target table regardless of {@code AD_CLIENT_ID}. This is the Java
   * equivalent of the PL/pgSQL {@code ad_scd_rebuild} (a per-row recompute loop). It serves the manual
   * rebuild ({@link StoredColumnRebuild}) only when the caller runs as System ({@code AD_CLIENT_ID='0'})
   * — a deliberate cross-client repair. The async sentinel path and a non-System manual rebuild use the
   * client-scoped {@link #rebuild(Connection, String, String)} overload instead. Target PKs are paged
   * through in bounded keyset chunks (see {@link #nextTargetIdChunk}) so memory stays constant
   * regardless of table size. Returns the number of target rows recomputed (0 when the column has
   * incomplete metadata).
   */
  int rebuild(Connection con, String columnId) throws SQLException {
    return rebuildInternal(con, columnId, null);
  }

  /**
   * Full idempotent rebuild of one stored computed column scoped to a single client: recomputes only
   * the target-table rows whose {@code AD_CLIENT_ID} equals {@code clientId}. This is the per-client
   * variant used by the async sentinel path ({@link StoredColumnQueueProcessor}, which partitions the
   * queue by client) and by a non-System manual rebuild ({@link StoredColumnRebuild}). It mirrors
   * {@link #rebuild(Connection, String)} but appends a client filter to the keyset paging. Returns the
   * number of target rows recomputed (0 when the column has incomplete metadata).
   */
  int rebuild(Connection con, String columnId, String clientId) throws SQLException {
    return rebuildInternal(con, columnId, clientId);
  }

  /**
   * Shared rebuild loop for both the all-client and client-scoped entry points. When {@code clientId}
   * is null every target PK is recomputed; otherwise only PKs of rows owned by {@code clientId} are
   * paged and recomputed (see {@link #nextTargetIdChunk}).
   */
  private int rebuildInternal(Connection con, String columnId, String clientId) throws SQLException {
    ColumnMeta meta = metaFor(con, columnId);
    if (meta == null) {
      return 0;
    }
    int count = 0;
    String afterPk = null;
    while (true) {
      // Fetch one bounded chunk of PKs and fully read it into memory before recomputing, so no
      // ResultSet stays open on this connection while the per-row recompute UPDATE runs.
      List<String> chunk = nextTargetIdChunk(con, meta, afterPk, clientId);
      for (String pk : chunk) {
        recompute(con, meta, pk);
        count++;
      }
      if (chunk.size() < REBUILD_CHUNK_SIZE) {
        break; // last (partial) chunk drained — no more rows to page through
      }
      afterPk = chunk.get(chunk.size() - 1);
    }
    return count;
  }

  /**
   * Recomputes and writes one target row: locks it {@code FOR UPDATE} to serialize concurrent
   * recomputations of the same aggregate, then writes {@code <col> = <fn>(<pk>)} unconditionally.
   * This is the Java equivalent of the PL/pgSQL {@code ad_scd_recompute} and must stay behaviorally
   * identical to it (verified by the parity tests). Table/column/function/pk names come from cached
   * {@code AD_COLUMN} metadata and are case-folded physical identifiers (lowercase on PostgreSQL,
   * UPPERCASE on Oracle), so they are quoted here.
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
          String table = fold(rs.getString(1));
          String column = fold(rs.getString(2));
          String fn = fold(rs.getString(3));
          String pk = fold(rs.getString(4));
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

  /**
   * Fetches the next bounded chunk of primary-key values of the target table for a keyset-paginated
   * rebuild: at most {@link #REBUILD_CHUNK_SIZE} PKs ordered ascending, starting strictly after
   * {@code afterPk} (or from the beginning when it is null). When {@code clientId} is non-null the
   * chunk is restricted to rows owned by that client (an {@code AD_CLIENT_ID = ?} filter, folded to the
   * dialect's stored case); when null every row is paged. Keyset pagination keeps memory bounded
   * regardless of table size — {@link #rebuildInternal} pages through the table one chunk at a time
   * instead of buffering every PK. The chunk is fully read and its {@code ResultSet} closed before the
   * caller issues any recompute UPDATE on the same connection.
   *
   * <p>Note: the whole rebuild still runs inside the caller's single transaction, so the per-row
   * {@code FOR UPDATE} locks accumulate for its duration — the memory footprint is bounded here, but
   * the transaction's lock footprint is not.</p>
   */
  private List<String> nextTargetIdChunk(Connection con, ColumnMeta meta, String afterPk,
      String clientId) throws SQLException {
    String q = quoteChar();
    List<String> ids = new ArrayList<>();
    StringBuilder sql = new StringBuilder("SELECT ").append(q).append(meta.pk).append(q)
        .append(" FROM ").append(q).append(meta.table).append(q);
    boolean hasWhere = false;
    if (afterPk != null) {
      sql.append(" WHERE ").append(q).append(meta.pk).append(q).append(" > ?");
      hasWhere = true;
    }
    if (clientId != null) {
      // Restrict the rebuild to one client's rows. AD_CLIENT_ID is folded to the physical case the
      // active dialect stores it in (lowercase on PostgreSQL, UPPERCASE on Oracle), matching how the
      // metadata identifiers are folded, then quoted.
      sql.append(hasWhere ? " AND " : " WHERE ")
          .append(q).append(fold("ad_client_id")).append(q).append(" = ?");
    }
    sql.append(" ORDER BY ").append(q).append(meta.pk).append(q);
    try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
      ps.setMaxRows(REBUILD_CHUNK_SIZE);
      int idx = 1;
      if (afterPk != null) {
        ps.setString(idx++, afterPk);
      }
      if (clientId != null) {
        ps.setString(idx, clientId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          ids.add(rs.getString(1));
        }
      }
    }
    return ids;
  }

  /** Identifier quote character for the active dialect (both PostgreSQL and Oracle use double quotes). */
  private String quoteChar() {
    return "\"";
  }

  /**
   * Folds a raw physical identifier to the case the active dialect stores it in: UPPERCASE on Oracle,
   * lowercase on PostgreSQL. Once folded the identifier can be quoted safely because it matches the
   * physical object's stored name. Null-safe (returns null for a null input).
   */
  private String fold(String value) {
    if (value == null) {
      return null;
    }
    return oracle ? value.toUpperCase() : value.toLowerCase();
  }

  /**
   * Cached recompute metadata for one stored computed column: the physical target table, the stored
   * column, its computation function and the primary-key column. All are case-folded physical
   * identifiers (lowercase on PostgreSQL, UPPERCASE on Oracle — see {@link #META_SQL} and
   * {@link #fold(String)}).
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
