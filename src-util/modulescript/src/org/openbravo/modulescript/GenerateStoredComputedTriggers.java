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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.modulescript;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.ConnectionProvider;

/**
 * Deploys the stored-computed-column recalculation engine and the per-dependency enqueue triggers.
 *
 * <p>Two halves run on every {@code update.database} (no execution limits), both idempotent:</p>
 *
 * <ol>
 *   <li><b>Static engine</b> ({@link #deployEngine}) — a fixed set of {@code ad_scd_*} functions and
 *       the deferred constraint trigger {@code ad_scd_dirty_aiu} on {@code AD_STOREDCOLUMN_DIRTY}.
 *       These are raw PostgreSQL objects (using {@code format()}, {@code pg_current_xact_id()},
 *       {@code current_setting}, {@code FOR UPDATE}, {@code CREATE CONSTRAINT TRIGGER ... DEFERRABLE})
 *       that cannot be expressed in dbsm's abstracted Oracle-style XML dialect, so they are deployed
 *       here via JDBC instead of as model XML.</li>
 *   <li><b>Per-dependency enqueue triggers</b> ({@link #execute}) — one PL/pgSQL function + AFTER
 *       trigger per active {@code AD_COLUMN_COMP_DEPENDENCY} row, which writes a dirty-row record into
 *       {@code AD_STOREDCOLUMN_DIRTY} so the engine recomputes the affected target rows at commit.</li>
 * </ol>
 *
 * <p>All deployed objects live under the {@code ad_scd_} namespace and are excluded from
 * {@code export.database} via {@code src-db/database/model/excludeFilter.xml} so they never drift into
 * the model. Orphaned per-dependency functions (whose dependency row was removed) are dropped
 * automatically.</p>
 */
public class GenerateStoredComputedTriggers extends ModuleScript {

  private static final Logger log = LogManager.getLogger();

  private static final String QUERY_DEPS =
      "SELECT d.ad_column_comp_dependency_id, d.ad_column_id, d.insert_event, "
    + "       d.update_event, d.delete_event, d.watched_columns, d.target_id_resolver_sql, "
    + "       t.tablename AS source_table, c.refresh_mode, c.computation_sequence_number "
    + "FROM   ad_column_comp_dependency d "
    + "JOIN   ad_table  t ON t.ad_table_id  = d.source_table_id "
    + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
    + "WHERE  d.isactive = 'Y' AND c.isactive = 'Y' AND c.computation_mode = 'S' "
    + "ORDER  BY c.computation_sequence_number, d.ad_column_comp_dependency_id";

  private static final String QUERY_DEPLOYED =
      "SELECT proname FROM pg_proc WHERE proname LIKE 'ad_scd_%_trf'";

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();

      // Half 1: static recalculation engine (functions + deferred constraint trigger).
      deployEngine(cp);

      // Half 2: per-dependency enqueue triggers.
      Set<String> activeDepIds = new HashSet<>();

      PreparedStatement psDeps = cp.getPreparedStatement(QUERY_DEPS);
      ResultSet rs = psDeps.executeQuery();
      while (rs.next()) {
        String depId       = rs.getString("ad_column_comp_dependency_id").toLowerCase();
        String columnId    = rs.getString("ad_column_id");
        String insertEvent = rs.getString("insert_event");
        String updateEvent = rs.getString("update_event");
        String deleteEvent = rs.getString("delete_event");
        String watchedCols = rs.getString("watched_columns");
        String resolverSql = rs.getString("target_id_resolver_sql");
        String sourceTable = rs.getString("source_table").toLowerCase();
        String refreshMode = rs.getString("refresh_mode");
        int    seqNo       = rs.getInt("computation_sequence_number");

        if (refreshMode == null) {
          log.warn("Skipping SCD dependency {} — target column {} has no REFRESH_MODE set",
              depId, columnId);
          continue;
        }

        activeDepIds.add(depId);
        String funcName    = "ad_scd_" + depId + "_trf";
        String triggerName = "ad_scd_" + depId + "_trg";

        List<String> watchedColNames = parseWatchedColumns(watchedCols);

        cp.getPreparedStatement(
            buildFunctionDdl(funcName, resolverSql, columnId, refreshMode, seqNo, watchedColNames))
            .execute();

        String eventClause = buildEventClause(insertEvent, updateEvent, deleteEvent,
            watchedColNames);

        cp.getPreparedStatement(
            "DROP TRIGGER IF EXISTS " + triggerName + " ON " + sourceTable).execute();
        cp.getPreparedStatement(
            "CREATE TRIGGER " + triggerName
            + " AFTER " + eventClause + " ON " + sourceTable
            + " FOR EACH ROW EXECUTE FUNCTION " + funcName + "()").execute();

        log.info("Deployed SCD trigger {} on table {}", triggerName, sourceTable);
      }
      cp.releasePreparedStatement(psDeps);

      dropOrphanedFunctions(cp, activeDepIds);

    } catch (Exception e) {
      handleError(e);
    }
  }

  /**
   * Deploys the static recalculation engine — idempotent on every run.
   *
   * <ul>
   *   <li>{@code ad_scd_recompute(column_id, target_id)} — recompute one target row.</li>
   *   <li>{@code ad_scd_process_dirty()} — deferred-trigger drain orchestrator.</li>
   *   <li>{@code ad_scd_rebuild(column_id)} — full idempotent rebuild of a column.</li>
   *   <li>{@code ad_scd_check(column_id)} — count of stale rows.</li>
   *   <li>{@code ad_scd_dirty_aiu} — DEFERRABLE INITIALLY DEFERRED constraint trigger that runs the
   *       drain at commit.</li>
   * </ul>
   */
  private void deployEngine(ConnectionProvider cp) throws Exception {
    cp.getPreparedStatement(RECOMPUTE_FN).execute();
    cp.getPreparedStatement(PROCESS_DIRTY_FN).execute();
    cp.getPreparedStatement(REBUILD_FN).execute();
    cp.getPreparedStatement(CHECK_FN).execute();

    cp.getPreparedStatement(
        "DROP TRIGGER IF EXISTS ad_scd_dirty_aiu ON ad_storedcolumn_dirty").execute();
    cp.getPreparedStatement(
        "CREATE CONSTRAINT TRIGGER ad_scd_dirty_aiu\n"
      + "  AFTER INSERT ON ad_storedcolumn_dirty\n"
      + "  DEFERRABLE INITIALLY DEFERRED\n"
      + "  FOR EACH ROW\n"
      + "  WHEN (NEW.refresh_mode = 'S')\n"
      + "  EXECUTE FUNCTION ad_scd_process_dirty()").execute();

    log.info("Deployed SCD recalculation engine (ad_scd_* functions + ad_scd_dirty_aiu)");
  }

  /**
   * Recompute a single target row. Looks up the physical target table, stored column, computation
   * function and primary key from {@code AD_COLUMN} metadata. Logical AD identifiers are stored
   * mixed-case ({@code C_Order}, {@code EM_Ettst_Linetotal}, {@code ETTST_SUMLINEAMOUNTS}) while the
   * physical PostgreSQL objects are lowercase, so every name is {@code lower()}-ed and quoted via
   * {@code format(%I)}.
   *
   * <p>Concurrency: takes a {@code FOR UPDATE} row lock on the target before recomputing so two
   * transactions touching different child rows of the same aggregate cannot lose an update under
   * READ COMMITTED. The {@code IS DISTINCT FROM} guard avoids a no-op write when the value is
   * unchanged.</p>
   */
  private static final String RECOMPUTE_FN =
      "CREATE OR REPLACE FUNCTION ad_scd_recompute(p_column_id varchar, p_target_id varchar)\n"
    + "RETURNS void AS $$\n"
    + "DECLARE\n"
    + "  v_table  varchar;\n"
    + "  v_column varchar;\n"
    + "  v_fn     varchar;\n"
    + "  v_pk     varchar;\n"
    + "BEGIN\n"
    + "  SELECT lower(t.tablename), lower(c.columnname), lower(c.computation_function),\n"
    + "         lower((SELECT k.columnname FROM ad_column k\n"
    + "                 WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y'))\n"
    + "    INTO v_table, v_column, v_fn, v_pk\n"
    + "    FROM ad_column c\n"
    + "    JOIN ad_table  t ON t.ad_table_id = c.ad_table_id\n"
    + "   WHERE c.ad_column_id = p_column_id;\n"
    + "  IF v_fn IS NULL OR v_table IS NULL OR v_pk IS NULL THEN\n"
    + "    RETURN;\n"
    + "  END IF;\n"
    + "  -- Serialize concurrent recomputations of the same aggregate row.\n"
    + "  EXECUTE format('SELECT 1 FROM %I WHERE %I = $1 FOR UPDATE', v_table, v_pk)\n"
    + "    USING p_target_id;\n"
    + "  EXECUTE format(\n"
    + "    'UPDATE %I SET %I = %I(%I) WHERE %I = $1 AND %I IS DISTINCT FROM %I(%I)',\n"
    + "    v_table, v_column, v_fn, v_pk, v_pk, v_column, v_fn, v_pk)\n"
    + "    USING p_target_id;\n"
    + "END;\n"
    + "$$ LANGUAGE plpgsql";

  /**
   * Deferred constraint-trigger body. Fires once per inserted dirty row at commit, sequentially
   * within the single backend process. The first firing claims the drain via the
   * {@code my.scd_refreshing} GUC and processes the whole sync queue for the transaction; firings
   * 2..N see the flag set and return immediately. The flag also suppresses re-entrant enqueues
   * caused by the engine's own writes to target tables.
   */
  private static final String PROCESS_DIRTY_FN =
      "CREATE OR REPLACE FUNCTION ad_scd_process_dirty()\n"
    + "RETURNS TRIGGER AS $$\n"
    + "DECLARE\n"
    + "  v_txn bigint;\n"
    + "  v_rec record;\n"
    + "BEGIN\n"
    + "  IF current_setting('my.scd_refreshing', true) = 'true' THEN\n"
    + "    RETURN NULL;\n"
    + "  END IF;\n"
    + "  PERFORM set_config('my.scd_refreshing', 'true', true);\n"
    + "  v_txn := pg_current_xact_id()::text::bigint;\n"
    + "  FOR v_rec IN\n"
    + "    SELECT ad_column_id, target_record_id\n"
    + "      FROM ad_storedcolumn_dirty\n"
    + "     WHERE transaction_id = v_txn\n"
    + "       AND refresh_mode = 'S'\n"
    + "       AND target_record_id IS NOT NULL\n"
    + "     ORDER BY computation_sequence_number, target_record_id\n"
    + "  LOOP\n"
    + "    PERFORM ad_scd_recompute(v_rec.ad_column_id, v_rec.target_record_id);\n"
    + "  END LOOP;\n"
    + "  DELETE FROM ad_storedcolumn_dirty\n"
    + "   WHERE transaction_id = v_txn AND refresh_mode = 'S';\n"
    + "  RETURN NULL;\n"
    + "END;\n"
    + "$$ LANGUAGE plpgsql";

  /** Full idempotent rebuild — recompute every target row of a stored computed column. */
  private static final String REBUILD_FN =
      "CREATE OR REPLACE FUNCTION ad_scd_rebuild(p_column_id varchar)\n"
    + "RETURNS integer AS $$\n"
    + "DECLARE\n"
    + "  v_table varchar;\n"
    + "  v_pk    varchar;\n"
    + "  v_id    varchar;\n"
    + "  v_cnt   integer := 0;\n"
    + "BEGIN\n"
    + "  SELECT lower(t.tablename),\n"
    + "         lower((SELECT k.columnname FROM ad_column k\n"
    + "                 WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y'))\n"
    + "    INTO v_table, v_pk\n"
    + "    FROM ad_column c\n"
    + "    JOIN ad_table  t ON t.ad_table_id = c.ad_table_id\n"
    + "   WHERE c.ad_column_id = p_column_id;\n"
    + "  IF v_table IS NULL OR v_pk IS NULL THEN\n"
    + "    RETURN 0;\n"
    + "  END IF;\n"
    + "  FOR v_id IN EXECUTE format('SELECT %I::varchar AS id FROM %I', v_pk, v_table)\n"
    + "  LOOP\n"
    + "    PERFORM ad_scd_recompute(p_column_id, v_id);\n"
    + "    v_cnt := v_cnt + 1;\n"
    + "  END LOOP;\n"
    + "  RETURN v_cnt;\n"
    + "END;\n"
    + "$$ LANGUAGE plpgsql";

  /** Returns the count of target rows whose stored value differs from the recomputed value. */
  private static final String CHECK_FN =
      "CREATE OR REPLACE FUNCTION ad_scd_check(p_column_id varchar)\n"
    + "RETURNS integer AS $$\n"
    + "DECLARE\n"
    + "  v_table  varchar;\n"
    + "  v_column varchar;\n"
    + "  v_fn     varchar;\n"
    + "  v_pk     varchar;\n"
    + "  v_cnt    integer;\n"
    + "BEGIN\n"
    + "  SELECT lower(t.tablename), lower(c.columnname), lower(c.computation_function),\n"
    + "         lower((SELECT k.columnname FROM ad_column k\n"
    + "                 WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y'))\n"
    + "    INTO v_table, v_column, v_fn, v_pk\n"
    + "    FROM ad_column c\n"
    + "    JOIN ad_table  t ON t.ad_table_id = c.ad_table_id\n"
    + "   WHERE c.ad_column_id = p_column_id;\n"
    + "  IF v_fn IS NULL THEN\n"
    + "    RETURN 0;\n"
    + "  END IF;\n"
    + "  EXECUTE format(\n"
    + "    'SELECT count(*) FROM %I WHERE %I IS DISTINCT FROM %I(%I)',\n"
    + "    v_table, v_column, v_fn, v_pk)\n"
    + "    INTO v_cnt;\n"
    + "  RETURN v_cnt;\n"
    + "END;\n"
    + "$$ LANGUAGE plpgsql";

  private void dropOrphanedFunctions(ConnectionProvider cp, Set<String> activeDepIds)
      throws Exception {
    PreparedStatement psDeployed = cp.getPreparedStatement(QUERY_DEPLOYED);
    ResultSet rsDep = psDeployed.executeQuery();
    while (rsDep.next()) {
      String proname = rsDep.getString(1);
      // strip "ad_scd_" prefix (7 chars) and "_trf" suffix (4 chars)
      String depId = proname.substring(7, proname.length() - 4);
      if (!activeDepIds.contains(depId)) {
        cp.getPreparedStatement("DROP FUNCTION IF EXISTS " + proname + "() CASCADE").execute();
        log.info("Dropped orphaned SCD function {}", proname);
      }
    }
    cp.releasePreparedStatement(psDeployed);
  }

  /**
   * Builds the per-dependency enqueue function.
   *
   * <p>Three guards precede the enqueue:</p>
   * <ol>
   *   <li>{@code my.triggers_disabled} — the global Etendo trigger-disable flag.</li>
   *   <li>{@code my.scd_refreshing} — set while the engine drains the queue, so the engine's own
   *       writes to target tables do not re-enqueue (recursive-loop guard).</li>
   *   <li>Watched-column value guard — on {@code UPDATE}, skip enqueueing unless at least one watched
   *       column actually changed value ({@code IS DISTINCT FROM}). {@code INSERT}/{@code DELETE}
   *       always enqueue.</li>
   * </ol>
   */
  private String buildFunctionDdl(String funcName, String resolverSql,
      String columnId, String refreshMode, int seqNo, List<String> watchedColNames) {
    StringBuilder watchedGuard = new StringBuilder();
    if (!watchedColNames.isEmpty()) {
      List<String> changed = new ArrayList<>();
      for (String col : watchedColNames) {
        changed.add("NEW." + col + " IS DISTINCT FROM OLD." + col);
      }
      watchedGuard
          .append("  IF TG_OP = 'UPDATE' AND NOT (\n")
          .append("    ").append(String.join("\n    OR ", changed)).append("\n")
          .append("  ) THEN\n")
          .append("    RETURN NEW;\n")
          .append("  END IF;\n");
    }
    return "CREATE OR REPLACE FUNCTION " + funcName + "()\n"
        + "RETURNS TRIGGER AS $$\n"
        + "DECLARE\n"
        + "  v_target_id VARCHAR(32);\n"
        + "BEGIN\n"
        + "  IF current_setting('my.triggers_disabled', true) = 'Y' THEN\n"
        + "    RETURN COALESCE(NEW, OLD);\n"
        + "  END IF;\n"
        + "  IF current_setting('my.scd_refreshing', true) = 'true' THEN\n"
        + "    RETURN COALESCE(NEW, OLD);\n"
        + "  END IF;\n"
        + watchedGuard
        + "  FOR v_target_id IN (\n"
        + "    " + resolverSql + "\n"
        + "  ) LOOP\n"
        + "    INSERT INTO AD_STOREDCOLUMN_DIRTY (\n"
        + "      AD_STOREDCOLUMN_DIRTY_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE,\n"
        + "      CREATED, CREATEDBY, UPDATED, UPDATEDBY,\n"
        + "      AD_COLUMN_ID, TARGET_RECORD_ID, TRANSACTION_ID,\n"
        + "      REFRESH_MODE, COMPUTATION_SEQUENCE_NUMBER\n"
        + "    ) VALUES (\n"
        + "      get_uuid(), '0', '0', 'Y', NOW(), '0', NOW(), '0',\n"
        + "      '" + columnId + "', v_target_id, pg_current_xact_id()::text::bigint,\n"
        + "      '" + refreshMode + "', " + seqNo + "\n"
        + "    ) ON CONFLICT DO NOTHING;\n"
        + "  END LOOP;\n"
        + "  RETURN COALESCE(NEW, OLD);\n"
        + "END;\n"
        + "$$ LANGUAGE plpgsql";
  }

  /**
   * Parses {@code WATCHED_COLUMNS} — a case-insensitive, comma-delimited list of source-table column
   * <i>names</i> — into a list of lowercased names matching PostgreSQL's default identifier folding.
   */
  private List<String> parseWatchedColumns(String watchedCols) {
    List<String> names = new ArrayList<>();
    if (watchedCols != null && !watchedCols.isBlank()) {
      for (String colName : watchedCols.split(",")) {
        String trimmed = colName.trim();
        if (!trimmed.isEmpty()) {
          names.add(trimmed.toLowerCase());
        }
      }
    }
    return names;
  }

  /**
   * Builds the {@code AFTER ...} event clause. When watched columns are present alongside an UPDATE
   * event, the trigger is narrowed to {@code AFTER UPDATE OF col1, col2} so PostgreSQL only fires it
   * when one of those columns is targeted by the statement; the value-level
   * {@code IS DISTINCT FROM} guard in the function body then rejects no-op updates.
   */
  private String buildEventClause(String insertEvent, String updateEvent, String deleteEvent,
      List<String> watchedColNames) {
    List<String> parts = new ArrayList<>();
    if ("Y".equals(insertEvent)) {
      parts.add("INSERT");
    }
    if ("Y".equals(updateEvent)) {
      if (!watchedColNames.isEmpty()) {
        parts.add("UPDATE OF " + String.join(", ", watchedColNames));
      } else {
        parts.add("UPDATE");
      }
    }
    if ("Y".equals(deleteEvent)) {
      parts.add("DELETE");
    }
    return String.join(" OR ", parts);
  }
}
