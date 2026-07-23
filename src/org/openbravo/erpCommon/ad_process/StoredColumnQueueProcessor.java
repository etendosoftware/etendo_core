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
 * database transaction, committing after every row.</p>
 *
 * <p><b>The queue is partitioned by client, with a System catch-all.</b> A run drains the rows owned
 * by the client it runs in ({@code AD_CLIENT_ID = bundle.getContext().getClient()}) — the enqueue
 * triggers stamp that client from the source record and initial population writes one full-rebuild
 * sentinel per owning client. The one exception is a run as System ({@code AD_CLIENT_ID='0'}): it
 * drains <b>every</b> client's partition in one pass, mirroring the deliberate cross-client repair of
 * the manual {@link StoredColumnRebuild} (which likewise treats a System caller as all-clients). So
 * the processor may be scheduled either <b>per active client</b> (disjoint partitions whose drains may
 * run <b>concurrently</b> with no interference) or as a <b>single System request</b> that drains them
 * all. Either way each sentinel rebuilds only its own owning client's rows (never the running
 * client's), so a System drain still repairs each client correctly.</p>
 *
 * <p><b>Concurrent drainers over the same rows are NOT supported.</b> Schedule <i>either</i> per-client
 * <i>or</i> a single System request, never both at once (they would overlap the same partitions), and
 * within a partition this process must run serially — a single Process Request, and (in a cluster) a
 * single node. Correctness
 * depends on the {@code computation_sequence_number} ordering of the fetch: chained stored columns are
 * allowed to read one another's stored values, and a column is only guaranteed to see a fresh upstream
 * value because the lower-sequence column is recomputed first. A second drainer on the same client
 * orders its own half of the partition independently, so a downstream column can be recomputed
 * before — or concurrently with — the upstream column it reads, leaving it stale. The fetch takes no
 * row lock: a lock would not help here anyway, since {@link #execute} commits the fetch transaction
 * before the per-row work begins, releasing any lock immediately.</p>
 *
 * <p>Sentinel rows ({@code TARGET_RECORD_ID IS NULL}) request a full rebuild of the column for the
 * sentinel's own client. The batch is processed in a single pass honouring the
 * {@code computation_sequence_number} order of the fetch: a sentinel rebuilds its column for the
 * running client (looping the same per-target recompute over every primary key of the column's target
 * table that belongs to that client), and any sibling per-target rows for that column later in the
 * batch are dropped because the rebuild already covered every row.</p>
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
   * Fetches the oldest non-dead-lettered queued rows, lowest {@code computation_sequence_number} first,
   * carrying each row's {@code AD_CLIENT_ID} so a sentinel rebuild can be scoped to its owning client.
   *
   * <p>Two shapes, chosen in {@link #fetchBatch} by whether the running client is System: a per-client
   * drain appends {@code AND ad_client_id = ?} ({@link #FETCH_SQL_CLIENT}) and takes only that client's
   * partition, while a System ({@code '0'}) drain omits the filter ({@link #FETCH_SQL_ALL}) and takes
   * every client's rows in one pass. Both per-target dirty rows (stamped with the source record's
   * {@code AD_CLIENT_ID} by the enqueue triggers) and full-rebuild null-sentinels (one per owning
   * client, written by initial population) live under a real client id, so the System drain simply sees
   * all of them. Per-client partitions are disjoint, so per-client drains may run concurrently; a
   * System drain and per-client drains must not run at once (see the class javadoc).</p>
   *
   * <p>The {@code ORDER BY} is a correctness requirement, not a nicety: it is what makes a chained
   * stored column read an already-refreshed upstream value. Chains live within a single target row's
   * dependencies, so ordering globally across clients is safe — different clients' rows are independent.
   * It composes with the JDBC {@code setMaxRows} bound — the batch takes the <i>lowest</i> sequence
   * numbers, so a chain split across successive batches still drains upstream-first. Only a concurrent
   * drainer over the same rows breaks the ordering, which is why overlapping drainers are unsupported
   * (see the class javadoc).</p>
   *
   * <p>The fetch takes no row lock. This process assumes a single drainer over any given partition —
   * concurrent overlapping drainers are not supported, since a second drainer would fetch an
   * overlapping, independently-ordered batch and could recompute a chained reader before the upstream
   * value it reads, breaking the sequence ordering above. A lock here would not help anyway:
   * {@link #execute} commits the fetch transaction before the per-row work begins, releasing any lock
   * immediately. A plain {@code SELECT ... ORDER BY} is dialect-neutral, valid on both PostgreSQL and
   * Oracle.</p>
   */
  private static final String FETCH_BASE = "SELECT ad_storedcolumn_dirty_id, ad_column_id,"
      + " target_record_id, ad_client_id"
      + " FROM ad_storedcolumn_dirty"
      + " WHERE refresh_mode = 'Q' AND is_ignored = 'N'";

  private static final String FETCH_ORDER = " ORDER BY computation_sequence_number ASC, created ASC";

  /** Per-client drain: restrict to one client's partition (bound parameter is the running client). */
  private static final String FETCH_SQL_CLIENT = FETCH_BASE + " AND ad_client_id = ?" + FETCH_ORDER;

  /**
   * System ({@code AD_CLIENT_ID='0'}) drain: no client filter, so every client's partition is drained
   * in one pass — the deliberate cross-client path, mirroring the System branch of
   * {@link StoredColumnRebuild}. Each sentinel still rebuilds only its own owning client's rows.
   */
  private static final String FETCH_SQL_ALL = FETCH_BASE + FETCH_ORDER;

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
    // A System ('0') run drains every client's partition; any other client drains only its own. The
    // client is read from the process bundle context, the same accessor StoredColumnRebuild uses to
    // decide its System-vs-client rebuild scope.
    String clientId = bundle.getContext().getClient();
    boolean system = "0".equals(clientId);
    Connection con = conn.getTransactionConnection();
    int processed = 0;
    int rebuilt = 0;
    int failed = 0;
    try {
      List<DirtyRow> batch = fetchBatch(con, maxRecords, clientId, system);
      // Close the read transaction opened by the fetch before the per-row transactions begin.
      con.commit();

      // Single pass in the batch's computation_sequence_number order, so a low-seq column is always
      // recomputed before a higher-seq column that depends on it. A sentinel (targetId == null)
      // rebuilds its whole column for its OWN client; any per-target row for that same (column, client)
      // already rebuilt by an earlier sentinel in this batch is dropped because the rebuild covered
      // every row. The dedup key is (column, client) so a System drain that spans clients does not let
      // one client's rebuild mask another client's per-target rows for the same column.
      Set<String> rebuiltColumns = new HashSet<>();
      for (DirtyRow row : batch) {
        String rebuiltKey = row.columnId + ' ' + row.clientId;
        if (row.targetId == null) {
          if (processRow(con, row, retryThreshold, true)) {
            rebuiltColumns.add(rebuiltKey);
            rebuilt++;
          } else {
            failed++;
          }
        } else if (rebuiltColumns.contains(rebuiltKey)) {
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
   * Fetches the oldest queued rows in {@code computation_sequence_number} order, bounding the batch via
   * JDBC {@code setMaxRows}. When {@code system} the whole queue is drained across every client
   * ({@link #FETCH_SQL_ALL}, no bound parameter); otherwise only {@code clientId}'s partition is taken
   * ({@link #FETCH_SQL_CLIENT}, binding the client id). Each row carries its own {@code AD_CLIENT_ID} so
   * a sentinel rebuild can be scoped to its owning client. Takes no row lock; see {@link #FETCH_SQL_ALL}
   * / {@link #FETCH_SQL_CLIENT} and the class javadoc for the per-client partitioning, the System
   * catch-all, and why overlapping drainers are unsupported.
   */
  private List<DirtyRow> fetchBatch(Connection con, int maxRecords, String clientId, boolean system)
      throws SQLException {
    List<DirtyRow> rows = new ArrayList<>();
    // System ('0') drains every client's partition; any other client drains only its own. The enqueue
    // triggers stamp AD_CLIENT_ID from the source record and initial population writes one full-rebuild
    // null-sentinel per owning client, so both shapes recompute exactly the rows they claim.
    try (PreparedStatement ps = con.prepareStatement(system ? FETCH_SQL_ALL : FETCH_SQL_CLIENT)) {
      ps.setMaxRows(maxRecords);
      if (!system) {
        ps.setString(1, clientId);
      }
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          rows.add(new DirtyRow(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        }
      }
    }
    return rows;
  }

  /**
   * Processes one dirty row in its own transaction: engages the recursion guard, recomputes the row
   * (a full column rebuild scoped to the row's own {@code AD_CLIENT_ID} when {@code sentinel}, otherwise
   * a single-target recompute), deletes the dirty row, and commits. On <b>any</b> failure — including
   * non-{@code SQLException}s from metadata lookup — the row's transaction is rolled back and the
   * failure is recorded and dead-lettered in a separate committed transaction, so one poison row
   * neither aborts the batch nor blocks its siblings. The cause is logged at {@code ERROR} with its
   * stack trace. Returns true on success.
   */
  private boolean processRow(Connection con, DirtyRow row, int retryThreshold, boolean sentinel) {
    try {
      // Suppress re-entrant enqueues caused by the engine's own synchronous writes within this row's
      // transaction (PostgreSQL only; no-op on Oracle, which drains asynchronously).
      recomputer.setRefreshingGuard(con);
      if (sentinel) {
        // A sentinel rebuilds only its OWN client's rows of the target table — never the running
        // client's. This is what lets a System ('0') drain repair every client correctly: each client's
        // sentinel rebuilds that client's partition, so the union covers all clients.
        recomputer.rebuild(con, row.columnId, row.clientId);
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

  /**
   * Immutable view of a claimed dirty row. {@code targetId} is null for rebuild sentinels;
   * {@code clientId} is the row's own {@code AD_CLIENT_ID}, used to scope a sentinel rebuild to its
   * owning client (never the running client) so a System drain repairs every client correctly.
   */
  private static final class DirtyRow {
    private final String id;
    private final String columnId;
    private final String targetId;
    private final String clientId;

    private DirtyRow(String id, String columnId, String targetId, String clientId) {
      this.id = id;
      this.columnId = columnId;
      this.targetId = targetId;
      this.clientId = clientId;
    }
  }
}
