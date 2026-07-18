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
package org.openbravo.modulescript;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
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
 *
 * <p><b>Oracle support</b> (Phase 5). Oracle has no deferred constraint-trigger firing and no
 * {@code pg_current_xact_id()}, so the synchronous ({@code 'S'}) drain cannot exist. Instead of
 * rejecting {@code 'S'} columns on Oracle they are <b>silently downgraded to {@code 'Q'}</b>: the
 * per-dependency enqueue triggers are still created (in Oracle PL/SQL) and always write
 * {@code Refresh_Mode='Q'}, and the async Java {@code StoredColumnQueueProcessor} is the only drain.
 * On Oracle the engine PL/pgSQL functions and the {@code ad_scd_dirty_aiu} constraint trigger are
 * <b>not</b> deployed at all — the recompute runs entirely in Java (workstream C2). Dialect is
 * detected via {@link ConnectionProvider#getRDBMS()} ({@code "POSTGRE"} / {@code "ORACLE"}).</p>
 */
public class GenerateStoredComputedTriggers extends ModuleScript {

  private static final Logger log = LogManager.getLogger();

  /** True when deploying against Oracle; selects the Oracle-dialect enqueue DDL and skips the PG engine. */
  private boolean oracle;

  private static final String QUERY_DEPS =
      "SELECT d.ad_column_comp_dependency_id, d.ad_column_id, d.insert_event, "
    + "       d.update_event, d.delete_event, d.target_id_resolver_sql, "
    + "       lc.columnname AS target_link_column, lc.isparent AS target_link_isparent, "
    + "       t.tablename AS source_table, ct.tablename AS target_table, "
    + "       c.refresh_mode, c.computation_sequence_number "
    + "FROM   ad_column_comp_dependency d "
    + "JOIN   ad_table  t ON t.ad_table_id  = d.source_table_id "
    + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
    + "JOIN   ad_table  ct ON ct.ad_table_id = c.ad_table_id "
    + "LEFT   JOIN ad_column lc ON lc.ad_column_id = d.target_link_column_id "
    + "WHERE  d.isactive = 'Y' AND c.isactive = 'Y' AND c.computation_mode = 'S' "
    + "ORDER  BY c.computation_sequence_number, d.ad_column_comp_dependency_id";

  /**
   * Loads watched-column names from the {@code AD_COMPDEP_WATCHED_COL} child table, joined to
   * {@code AD_COLUMN} for the physical column name. Grouped by dependency id in
   * {@link #loadWatchedColumns}. This child table is the sole source of watched columns.
   */
  private static final String QUERY_WATCHED =
      "SELECT w.ad_column_comp_dependency_id, c.columnname "
    + "FROM   ad_compdep_watched_col w "
    + "JOIN   ad_column c ON c.ad_column_id = w.ad_column_id "
    + "WHERE  w.isactive = 'Y' AND c.isactive = 'Y' "
    + "ORDER  BY w.ad_column_comp_dependency_id, w.seqno, c.columnname";

  private static final String QUERY_DEPLOYED =
      "SELECT proname FROM pg_proc WHERE proname LIKE 'ad_scd_%_trf'";

  /**
   * Oracle equivalent of {@link #QUERY_DEPLOYED}: the per-dependency enqueue trigger names already
   * present. Oracle emits an inline-body trigger (no standalone function), so the deployed unit is the
   * {@code ad_scd_<depId>_trg} trigger itself rather than a {@code _trf} function. Names come back
   * upper-cased from {@code user_triggers}; the caller lower-cases and strips prefix/suffix.
   */
  private static final String QUERY_DEPLOYED_ORACLE =
      "SELECT lower(trigger_name) FROM user_triggers WHERE lower(trigger_name) LIKE 'ad_scd_%_trg'";

  /**
   * Row-count ceiling for an inline synchronous initial population of a {@code REFRESH_MODE='S'}
   * column. At or below this many rows in the target table, the column is rebuilt inline during
   * {@code update.database}; above it, population is deferred to the async queue so a long rebuild
   * cannot stall the build. 100k is a deliberately conservative default — a full-table aggregate over
   * 100k rows is seconds, not minutes, on Etendo's typical document tables.
   */
  private static final long LARGE_TABLE_THRESHOLD = 100_000L;

  @Override
  public void execute() {
    try {
      ConnectionProvider cp = getConnectionProvider();
      oracle = "ORACLE".equals(cp.getRDBMS());

      // Gate 0: validate every stored computed definition before deploying anything. In enforce mode
      // this throws its own aggregated BuildException (preserved by the catch below) and no objects
      // are deployed against an invalid definition set; in warn mode it only logs.
      StoredComputedValidator.assertDefinitionsValid(cp);

      // Half 1: static recalculation engine (functions + deferred constraint trigger).
      // On Oracle there is no synchronous drain, so no engine PL/SQL is deployed at all.
      deployEngine(cp);

      // Objects (re)deployed this run, for the post-deploy drift check (V15).
      List<StoredComputedValidator.DeployedDep> deployed = new ArrayList<>();

      // Half 2: per-dependency enqueue triggers.
      // Drain the dependency rows into memory and release the cursor BEFORE issuing any DDL.
      // Each DDL statement below commits on this connection; with the default
      // CLOSE_CURSORS_AT_COMMIT holdability, deploying inside the open ResultSet would close the
      // cursor after the first row and silently skip the rest. Materialising first avoids that.
      List<DepRow> deps = new ArrayList<>();
      PreparedStatement psDeps = cp.getPreparedStatement(QUERY_DEPS);
      ResultSet rs = psDeps.executeQuery();
      while (rs.next()) {
        DepRow row = new DepRow();
        row.depId          = rs.getString("ad_column_comp_dependency_id").toLowerCase();
        row.columnId       = rs.getString("ad_column_id");
        row.insertEvent    = rs.getString("insert_event");
        row.updateEvent    = rs.getString("update_event");
        row.deleteEvent    = rs.getString("delete_event");
        row.resolverSql    = rs.getString("target_id_resolver_sql");
        row.linkColumn     = rs.getString("target_link_column");
        row.linkIsParent   = rs.getString("target_link_isparent");
        row.sourceTable    = rs.getString("source_table").toLowerCase();
        row.targetTable    = rs.getString("target_table").toLowerCase();
        row.refreshMode    = rs.getString("refresh_mode");
        row.seqNo          = rs.getInt("computation_sequence_number");
        deps.add(row);
      }
      cp.releasePreparedStatement(psDeps);

      // Materialise watched columns (child table) before any DDL, same cursor discipline as deps.
      Map<String, List<String>> watchedByDep = loadWatchedColumns(cp);

      // Snapshot which dependency functions already exist BEFORE we (re)create any, so a column
      // whose objects are all brand-new this run is recognised as a first activation (B2).
      Set<String> preexistingDepIds = loadDeployedDepIds(cp);

      Set<String> activeDepIds = new HashSet<>();
      // Per-column initial-population bookkeeping, insertion-ordered for stable logging.
      Map<String, ColumnPopulation> populations = new LinkedHashMap<>();
      for (DepRow row : deps) {
        if (row.refreshMode == null) {
          log.warn("Skipping SCD dependency {} — target column {} has no REFRESH_MODE set",
              row.depId, row.columnId);
          continue;
        }

        String resolverSql = resolveResolverSql(row);
        if (resolverSql == null) {
          log.warn("Skipping SCD dependency {} — neither TARGET_ID_RESOLVER_SQL nor "
              + "TARGET_LINK_COLUMN_ID is set", row.depId);
          continue;
        }

        String portabilityIssue = validateResolverPortability(resolverSql);
        if (portabilityIssue != null) {
          log.warn("Skipping SCD dependency {} — non-portable resolver SQL ({}): {}",
              row.depId, portabilityIssue, resolverSql);
          continue;
        }

        List<String> watchedColNames =
            watchedByDep.getOrDefault(row.depId, new ArrayList<>());

        // An UPDATE event needs watched columns to be safe: without them the trigger fires on EVERY
        // update to a source row — including the engine's own recompute write — with no value guard to
        // stop it. On Oracle the enqueue no longer honours the global trigger-disable (see
        // deployOracleTrigger), so an unguarded UPDATE would re-enqueue the row the processor just
        // recomputed, forever; on PostgreSQL it is at best wasteful. Drop the UPDATE event in that
        // case and warn. INSERT/DELETE carry no such risk — the engine recompute writes an UPDATE,
        // never an INSERT or DELETE — so they are kept.
        String updateEvent = row.updateEvent;
        if ("Y".equals(updateEvent) && watchedColNames.isEmpty()) {
          log.warn("SCD dependency {} on table {} declares an UPDATE event but has no watched columns"
              + " — dropping the UPDATE event (it would fire on every update with no value guard,"
              + " risking a re-enqueue loop). INSERT/DELETE events, if any, are still deployed.",
              row.depId, row.sourceTable);
          updateEvent = "N";
        }

        // If nothing is left to fire on, generate no trigger at all — and leave this dep OUT of
        // activeDepIds so dropOrphanedFunctions removes any objects a previous run deployed for it.
        if (!"Y".equals(row.insertEvent) && !"Y".equals(updateEvent) && !"Y".equals(row.deleteEvent)) {
          log.warn("SCD dependency {} on table {} has no watched columns and no INSERT/DELETE events"
              + " — no trigger generated.", row.depId, row.sourceTable);
          continue;
        }

        activeDepIds.add(row.depId);

        // Track this column for initial population. A column is a "first activation" only if NONE of
        // its dependency functions existed before this run (B2): if any did, the column was already
        // deployed and previously populated, so re-running would needlessly re-aggregate every row.
        ColumnPopulation pop = populations.computeIfAbsent(row.columnId,
            k -> new ColumnPopulation(row.columnId, row.refreshMode, row.targetTable, row.seqNo));
        if (preexistingDepIds.contains(row.depId)) {
          pop.hadPreexistingDep = true;
        }

        String funcName    = "ad_scd_" + row.depId + "_trf";
        String triggerName = "ad_scd_" + row.depId + "_trg";

        String eventClause = buildEventClause(row.insertEvent, updateEvent, row.deleteEvent,
            watchedColNames);

        if (oracle) {
          deployOracleTrigger(cp, triggerName, row, resolverSql, eventClause, watchedColNames);
          // Oracle deploys only the inline trigger; drift check degrades to presence (no DDL).
          deployed.add(new StoredComputedValidator.DeployedDep(row.depId, row.sourceTable, null));
        } else {
          String functionDdl = buildFunctionDdl(funcName, resolverSql, row.columnId, row.refreshMode,
              row.seqNo, watchedColNames);
          cp.getPreparedStatement(functionDdl).execute();

          cp.getPreparedStatement(
              "DROP TRIGGER IF EXISTS " + triggerName + " ON " + row.sourceTable).execute();
          cp.getPreparedStatement(
              "CREATE TRIGGER " + triggerName
              + " AFTER " + eventClause + " ON " + row.sourceTable
              + " FOR EACH ROW EXECUTE FUNCTION " + funcName + "()").execute();
          // Capture the freshly rendered function DDL for the post-deploy body-drift comparison.
          deployed.add(
              new StoredComputedValidator.DeployedDep(row.depId, row.sourceTable, functionDdl));
        }

        log.info("Deployed SCD trigger {} on table {}", triggerName, row.sourceTable);
      }

      dropOrphanedFunctions(cp, activeDepIds);

      // Initial population for columns activated for the first time this run.
      populateNewColumns(cp, populations);

      // Gate 1: verify what we just deployed is actually present (and, on PG, not drifted). Missing
      // objects are hard; body drift is a warning. Applies the ETGO_SCD_VALIDATION toggle.
      StoredComputedValidator.finishOrThrow(
          StoredComputedValidator.checkDeploymentDrift(cp, oracle, deployed));

    } catch (BuildException be) {
      // Preserve the validator's aggregated report — handleError would replace it with a generic one.
      throw be;
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
    if (oracle) {
      // Oracle: no synchronous drain exists (no deferred constraint triggers, no
      // pg_current_xact_id()), and the recompute runs in Java (workstream C2), so the engine deploys
      // ZERO PL/SQL functions and no ad_scd_dirty_aiu / ad_scd_process_dirty. Every column drains
      // through the async Java StoredColumnQueueProcessor.
      log.info("Oracle detected — skipping PL/pgSQL SCD engine; async Java drain is the only path");
      return;
    }

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
   * mixed-case ({@code C_Order}, {@code EM_Etscc_Linetotal}, {@code ETSCC_SUMLINEAMOUNTS}) while the
   * physical PostgreSQL objects are lowercase, so every name is {@code lower()}-ed and quoted via
   * {@code format(%I)}.
   *
   * <p>Concurrency: takes a {@code FOR UPDATE} row lock on the target before recomputing so two
   * transactions touching different child rows of the same aggregate cannot lose an update under
   * READ COMMITTED.</p>
   *
   * <p>The write is unconditional — no {@code IS DISTINCT FROM} guard. Guarding the {@code UPDATE}
   * with {@code … AND <col> IS DISTINCT FROM <fn>(<pk>)} would force the aggregate function to run
   * twice per row (once for the {@code SET}, once for the {@code WHERE}); paying a second full
   * compute to skip a no-op write is a bad trade. The enqueue trigger's cheap watched-column guard
   * already filters no-op source changes upstream, and {@code my.scd_refreshing} prevents the write
   * from re-enqueueing, so an unchanged-value write is rare and harmless.</p>
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
    + "    'UPDATE %I SET %I = %I(%I) WHERE %I = $1',\n"
    + "    v_table, v_column, v_fn, v_pk, v_pk)\n"
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
    if (oracle) {
      dropOrphanedOracleTriggers(cp, activeDepIds);
      return;
    }
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
   * Oracle counterpart of {@link #dropOrphanedFunctions}. Oracle has no standalone enqueue function,
   * so the deployed unit is the {@code ad_scd_<depId>_trg} trigger; orphans are found via
   * {@code user_triggers} and dropped by name (swallowing ORA-04080 in case of a concurrent drop).
   */
  private void dropOrphanedOracleTriggers(ConnectionProvider cp, Set<String> activeDepIds)
      throws Exception {
    List<String> orphanTriggers = new ArrayList<>();
    PreparedStatement psDeployed = cp.getPreparedStatement(QUERY_DEPLOYED_ORACLE);
    ResultSet rsDep = psDeployed.executeQuery();
    while (rsDep.next()) {
      String trigName = rsDep.getString(1);
      // strip "ad_scd_" prefix (7 chars) and "_trg" suffix (4 chars)
      String depId = trigName.substring(7, trigName.length() - 4);
      if (!activeDepIds.contains(depId)) {
        orphanTriggers.add(trigName);
      }
    }
    cp.releasePreparedStatement(psDeployed);
    for (String trigName : orphanTriggers) {
      try {
        cp.getPreparedStatement("DROP TRIGGER " + trigName).execute();
        log.info("Dropped orphaned SCD trigger {}", trigName);
      } catch (Exception e) {
        if (!isOracleTriggerMissing(e)) {
          throw e;
        }
      }
    }
  }

  /**
   * Returns the set of dependency ids whose enqueue function {@code ad_scd_<depId>_trf} already exists
   * in the database, captured before any (re)deploy this run. Used to distinguish a first activation
   * (no function existed) from a routine re-run (function already present). The prefix/suffix stripping
   * mirrors {@link #dropOrphanedFunctions}.
   */
  private Set<String> loadDeployedDepIds(ConnectionProvider cp) throws Exception {
    Set<String> deployed = new HashSet<>();
    PreparedStatement ps =
        cp.getPreparedStatement(oracle ? QUERY_DEPLOYED_ORACLE : QUERY_DEPLOYED);
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      String name = rs.getString(1);
      // PG:     strip "ad_scd_" prefix (7 chars) and "_trf" suffix (4 chars)
      // Oracle: strip "ad_scd_" prefix (7 chars) and "_trg" suffix (4 chars)
      deployed.add(name.substring(7, name.length() - 4));
    }
    cp.releasePreparedStatement(ps);
    return deployed;
  }

  /**
   * Populates the stored value of every column that was activated for the first time this run (B1).
   * A column qualifies only when none of its dependency functions pre-existed
   * ({@link ColumnPopulation#hadPreexistingDep} is false) — a routine re-run skips population so it
   * never re-aggregates already-correct data.
   *
   * <p>The action depends on the column's {@code REFRESH_MODE}:</p>
   * <ul>
   *   <li><b>{@code 'S'} (sync)</b> — populate <i>inline</i> at deploy time via
   *       {@code ad_scd_rebuild()}, but only when the target table holds at most
   *       {@link #LARGE_TABLE_THRESHOLD} rows. Above that, a synchronous rebuild during
   *       {@code update.database} could run for minutes and stall the build, so we fall back to the
   *       queue (insert a null-sentinel {@code 'Q'} row) and log a warning for the operator to run the
   *       background processor.</li>
   *   <li><b>{@code 'Q'} (queued)</b> — insert exactly one null-sentinel dirty row; the background
   *       {@code StoredColumnQueueProcessor} performs the rebuild asynchronously.</li>
   *   <li><b>{@code 'M'} (manual)</b> — do nothing; the operator triggers
   *       {@code StoredColumnRebuild} when ready.</li>
   * </ul>
   *
   * <p>The inline {@code 'S'} rebuild runs inside a single {@code DO} block that first sets the
   * {@code my.scd_refreshing} guard {@code LOCAL} to that block, so the engine's own writes do not
   * re-enqueue. It must be one statement because the ModuleScript connection commits per
   * {@code execute()}, which would otherwise reset a {@code SET LOCAL} guard between calls.</p>
   */
  private void populateNewColumns(ConnectionProvider cp, Map<String, ColumnPopulation> populations)
      throws Exception {
    for (ColumnPopulation pop : populations.values()) {
      if (pop.hadPreexistingDep) {
        continue; // already deployed in a prior run — leave existing values untouched
      }
      String mode = pop.refreshMode;
      if ("M".equals(mode)) {
        log.info("SCD column {} first-activated with REFRESH_MODE='M' — manual rebuild required "
            + "(run StoredColumnRebuild)", pop.columnId);
        continue;
      }
      if ("S".equals(mode)) {
        if (oracle) {
          // Oracle deploys no PL/pgSQL engine (no ad_scd_rebuild) and silently downgrades 'S' to the
          // async path: enqueue a sentinel and let StoredColumnQueueProcessor recompute in Java.
          insertSentinel(cp, pop);
          log.info("SCD column {} first-activated (REFRESH_MODE='S', Oracle) — deferred to the async "
              + "queue (per-client null-sentinels); run StoredColumnQueueProcessor to complete "
              + "population", pop.columnId);
          continue;
        }
        long rowCount = countRows(cp, pop.targetTable);
        if (rowCount <= LARGE_TABLE_THRESHOLD) {
          // Inline synchronous rebuild — single DO block so the LOCAL guard survives the call.
          cp.getPreparedStatement(
              "DO $$ BEGIN\n"
            + "  PERFORM set_config('my.scd_refreshing', 'true', true);\n"
            + "  PERFORM ad_scd_rebuild('" + pop.columnId + "');\n"
            + "END $$").execute();
          log.info("SCD column {} first-activated (REFRESH_MODE='S') — rebuilt {} row(s) inline",
              pop.columnId, rowCount);
        } else {
          insertSentinel(cp, pop);
          log.warn("SCD column {} first-activated (REFRESH_MODE='S') but target table {} has {} rows "
              + "(> {} threshold) — deferred to the async queue; run StoredColumnQueueProcessor to "
              + "complete population", pop.columnId, pop.targetTable, rowCount, LARGE_TABLE_THRESHOLD);
        }
      } else if ("Q".equals(mode)) {
        insertSentinel(cp, pop);
        log.info("SCD column {} first-activated (REFRESH_MODE='Q') — queued full rebuild "
            + "(per-client null-sentinels)", pop.columnId);
      } else {
        log.warn("SCD column {} has unknown REFRESH_MODE '{}' — skipping initial population",
            pop.columnId, mode);
      }
    }
  }

  /** Counts rows in a physical target table; returns {@link Long#MAX_VALUE} if the count fails. */
  private long countRows(ConnectionProvider cp, String targetTable) {
    try {
      PreparedStatement ps =
          cp.getPreparedStatement("SELECT count(*) FROM " + targetTable);
      ResultSet rs = ps.executeQuery();
      long count = rs.next() ? rs.getLong(1) : 0L;
      cp.releasePreparedStatement(ps);
      return count;
    } catch (Exception e) {
      // Conservative fallback: treat as large so we defer to the queue rather than risk a long
      // synchronous rebuild stalling update.database.
      log.warn("Could not count rows of {} — deferring SCD population to the queue: {}",
          targetTable, e.getMessage());
      return Long.MAX_VALUE;
    }
  }

  /**
   * Inserts one per-client null-sentinel ({@code TARGET_RECORD_ID IS NULL}) queued dirty row per
   * distinct client that owns rows in the target table, each requesting a full rebuild of the column
   * for that client. The queue is partitioned by client, so every client gets its own sentinel and
   * drains it under its own {@link StoredColumnQueueProcessor} run. {@code ON CONFLICT DO NOTHING}
   * (PostgreSQL) / the {@code MERGE} match (Oracle) on the {@code (AD_COLUMN_ID, AD_CLIENT_ID,
   * TRANSACTION_ID)} sentinel unique index makes this idempotent across re-runs within the same
   * transaction. {@code get_uuid()} is VOLATILE, so each selected client row gets its own primary key.
   */
  private void insertSentinel(ConnectionProvider cp, ColumnPopulation pop) throws Exception {
    if (oracle) {
      // Oracle: SYSDATE for timestamps, NULL TRANSACTION_ID (no pg_current_xact_id()), and a MERGE
      // against the sentinel key (column + client + NULL target, not ignored) for the idempotency that
      // ON CONFLICT DO NOTHING provides on PostgreSQL. The source is one row per distinct owning
      // client, so a per-client sentinel is inserted for every client that has rows in the target.
      // The SELECT DISTINCT ad_client_id full-scans the target table, which is acceptable because it
      // runs only once at column first-activation (not per-row, not per-request).
      cp.getPreparedStatement(
          "MERGE INTO AD_STOREDCOLUMN_DIRTY d\n"
        + "USING (SELECT DISTINCT ad_client_id FROM " + pop.targetTable + ") s\n"
        + "ON (d.AD_COLUMN_ID = '" + pop.columnId + "'\n"
        + "    AND d.AD_CLIENT_ID = s.ad_client_id\n"
        + "    AND d.TARGET_RECORD_ID IS NULL AND d.IS_IGNORED = 'N')\n"
        + "WHEN NOT MATCHED THEN INSERT (\n"
        + "  AD_STOREDCOLUMN_DIRTY_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE,\n"
        + "  CREATED, CREATEDBY, UPDATED, UPDATEDBY,\n"
        + "  AD_COLUMN_ID, TARGET_RECORD_ID, TRANSACTION_ID,\n"
        + "  REFRESH_MODE, COMPUTATION_SEQUENCE_NUMBER\n"
        + ") VALUES (\n"
        + "  get_uuid(), s.ad_client_id, '0', 'Y', SYSDATE, '0', SYSDATE, '0',\n"
        + "  '" + pop.columnId + "', NULL, NULL,\n"
        + "  'Q', " + pop.seqNo + "\n"
        + ")").execute();
      return;
    }
    // PostgreSQL: one sentinel row per distinct owning client via a set-based INSERT ... SELECT.
    // get_uuid() is VOLATILE so each selected client row receives its own primary key.
    // The SELECT DISTINCT ad_client_id full-scans the target table, which is acceptable because it
    // runs only once at column first-activation (not per-row, not per-request).
    cp.getPreparedStatement(
        "INSERT INTO AD_STOREDCOLUMN_DIRTY (\n"
      + "  AD_STOREDCOLUMN_DIRTY_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE,\n"
      + "  CREATED, CREATEDBY, UPDATED, UPDATEDBY,\n"
      + "  AD_COLUMN_ID, TARGET_RECORD_ID, TRANSACTION_ID,\n"
      + "  REFRESH_MODE, COMPUTATION_SEQUENCE_NUMBER\n"
      + ")\n"
      + "SELECT get_uuid(), c.ad_client_id, '0', 'Y', NOW(), '0', NOW(), '0',\n"
      + "       '" + pop.columnId + "', NULL, pg_current_xact_id()::text::bigint,\n"
      + "       'Q', " + pop.seqNo + "\n"
      + "  FROM (SELECT DISTINCT ad_client_id FROM " + pop.targetTable + ") c\n"
      + "ON CONFLICT DO NOTHING").execute();
  }

  /**
   * Builds the per-dependency enqueue function.
   *
   * <p>Two guards precede the enqueue:</p>
   * <ol>
   *   <li>{@code my.scd_refreshing} — set while the engine drains the queue, so the engine's own
   *       writes to target tables do not re-enqueue (recursive-loop guard).</li>
   *   <li>Watched-column value guard — on {@code UPDATE}, skip enqueueing unless at least one watched
   *       column actually changed value ({@code IS DISTINCT FROM}). {@code INSERT}/{@code DELETE}
   *       always enqueue.</li>
   * </ol>
   *
   * <p><b>Deliberately ignores {@code my.triggers_disabled}.</b> Operations that disable Etendo
   * triggers for bulk work — imports, record cloning ({@code CloneRecords} / {@code CloneOrderHook}),
   * and other {@code TriggerHandler.disable()} callers — still mutate watched source rows, and the
   * stored computed columns must stay consistent with those mutations. If the enqueue honored the
   * global disable flag the dirty row would never be written and the computed value would silently go
   * stale (this is the sales-order clone symptom). The enqueue therefore runs regardless of
   * {@code my.triggers_disabled}; recursion from the engine's own recompute writes is still guarded by
   * {@code my.scd_refreshing} above, which is an independent flag.</p>
   *
   * <p>Before each enqueue the function deletes any prior dead-lettered ({@code IS_IGNORED='Y'}) row
   * for the same {@code (column, target)} so a genuine new source change resets retry bookkeeping
   * (A4.4). This is a no-op for {@code 'S'} columns, which never dead-letter.</p>
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
        // NOTE: this enqueue trigger intentionally does NOT check my.triggers_disabled. Operations
        // that disable Etendo triggers for bulk work (imports, record cloning, ...) still change
        // watched source rows, so the dirty row MUST be enqueued regardless to keep the stored
        // computed column consistent. Recursion from the engine's own recompute writes is guarded
        // separately by my.scd_refreshing below (an independent flag), so dropping the disable check
        // does not reopen the recursive-loop it never protected against.
        + "  IF current_setting('my.scd_refreshing', true) = 'true' THEN\n"
        + "    RETURN COALESCE(NEW, OLD);\n"
        + "  END IF;\n"
        + watchedGuard
        + "  FOR v_target_id IN (\n"
        + "    " + resolverSql + "\n"
        + "  ) LOOP\n"
        // Skip dirty-record generation when the resolver yields a NULL target id: there is no row to
        // recompute, and a NULL TARGET_RECORD_ID would pollute the queue (and collide with the
        // null-sentinel full-rebuild row). Mirrors the Oracle path's CONTINUE WHEN v_target_id IS NULL.
        + "    CONTINUE WHEN v_target_id IS NULL;\n"
        // Reset on re-trigger: a genuine new source change must give a previously dead-lettered
        // (column, target) a clean retry. The dedup key includes TRANSACTION_ID, so a new
        // transaction inserts a fresh row instead of conflicting with the stale is_ignored='Y'
        // row — without this DELETE that dead row would linger forever. No-op for 'S' columns
        // (they never set is_ignored='Y').
        + "    DELETE FROM ad_storedcolumn_dirty\n"
        + "     WHERE ad_column_id = '" + columnId + "' AND target_record_id = v_target_id\n"
        + "       AND is_ignored = 'Y';\n"
        + "    INSERT INTO AD_STOREDCOLUMN_DIRTY (\n"
        + "      AD_STOREDCOLUMN_DIRTY_ID, AD_CLIENT_ID, AD_ORG_ID, ISACTIVE,\n"
        + "      CREATED, CREATEDBY, UPDATED, UPDATEDBY,\n"
        + "      AD_COLUMN_ID, TARGET_RECORD_ID, TRANSACTION_ID,\n"
        + "      REFRESH_MODE, COMPUTATION_SEQUENCE_NUMBER\n"
        + "    ) VALUES (\n"
        // AD_CLIENT_ID is taken from the source row that fired the trigger (COALESCE covers DELETE,
        // where NEW is null, and INSERT, where OLD is null) so the dirty row is owned by the same
        // client as the record whose change enqueued it.
        + "      get_uuid(), COALESCE(NEW.ad_client_id, OLD.ad_client_id), '0', 'Y',\n"
        + "      NOW(), '0', NOW(), '0',\n"
        + "      '" + columnId + "', v_target_id, pg_current_xact_id()::text::bigint,\n"
        + "      '" + refreshMode + "', " + seqNo + "\n"
        + "    ) ON CONFLICT DO NOTHING;\n"
        + "  END LOOP;\n"
        + "  RETURN COALESCE(NEW, OLD);\n"
        + "END;\n"
        + "$$ LANGUAGE plpgsql";
  }

  /**
   * Deploys one Oracle-dialect per-dependency enqueue trigger (Phase 5, workstream C1).
   *
   * <p>Oracle has no standalone-function-plus-{@code EXECUTE FUNCTION} split, so the whole enqueue
   * body is inlined in a {@code CREATE OR REPLACE TRIGGER … BEGIN … END;}. The differences from the
   * PostgreSQL form ({@link #buildFunctionDdl}) are:</p>
   * <ul>
   *   <li>{@code NEW.}/{@code OLD.} correlation → {@code :NEW.}/{@code :OLD.} (the only resolver
   *       rewrite — see {@link #toOracleResolver}); {@code TG_OP} → {@code INSERTING} /
   *       {@code UPDATING} / {@code DELETING}.</li>
   *   <li>{@code NOW()} → {@code SYSDATE}; {@code get_uuid()} is portable.</li>
   *   <li><b>No recursion guard.</b> Unlike PostgreSQL there is no {@code my.scd_refreshing} check and
   *       — deliberately — no {@code AD_IsTriggerEnabled()} check either. Oracle drains every column
   *       through the async Java processor ({@code Refresh_Mode} forced to {@code 'Q'}), so the enqueue
   *       cannot recurse inside a transaction (it only writes the dirty queue). Honouring the global
   *       trigger-disable would suppress the enqueue during imports and record cloning and silently
   *       stale the computed column (the sales-order clone symptom). Cross-cycle re-enqueue from the
   *       processor's own recompute write is prevented structurally: the caller never deploys an UPDATE
   *       event without watched columns, and the watched-column value guard rejects no-op writes.</li>
   *   <li>Dedup is a {@code MERGE} on {@code (AD_COLUMN_ID, TARGET_RECORD_ID)} among non-dead-lettered
   *       rows ({@code IS_IGNORED='N'}) instead of {@code INSERT … ON CONFLICT}; {@code TRANSACTION_ID}
   *       stays {@code NULL} (only the PG {@code 'S'} drain reads it) so there is no per-transaction
   *       dedup.</li>
   *   <li>{@code Refresh_Mode} is a <b>forced literal {@code 'Q'}</b> — a column configured {@code 'S'}
   *       silently behaves as queued on Oracle.</li>
   *   <li>Drop-by-name catching {@code ORA-04080} (trigger does not exist) replaces
   *       {@code DROP TRIGGER IF EXISTS}.</li>
   * </ul>
   */
  private void deployOracleTrigger(ConnectionProvider cp, String triggerName, DepRow row,
      String resolverSql, String eventClause, List<String> watchedColNames) throws Exception {
    // Drop-by-name; Oracle has no IF EXISTS for triggers, so swallow ORA-04080 (does not exist).
    try {
      cp.getPreparedStatement("DROP TRIGGER " + triggerName).execute();
    } catch (Exception e) {
      if (!isOracleTriggerMissing(e)) {
        throw e;
      }
    }

    String oracleResolver = toOracleResolver(resolverSql);

    StringBuilder watchedGuard = new StringBuilder();
    if (!watchedColNames.isEmpty()) {
      List<String> changed = new ArrayList<>();
      for (String col : watchedColNames) {
        // DECODE-based NULL-safe inequality: 1 when the two values differ (either side NULL counts).
        changed.add("DECODE(:NEW." + col + ", :OLD." + col + ", 1, 0) = 0");
      }
      watchedGuard
          .append("  IF UPDATING AND NOT (\n")
          .append("    ").append(String.join("\n    OR ", changed)).append("\n")
          .append("  ) THEN\n")
          .append("    RETURN;\n")
          .append("  END IF;\n");
    }

    String ddl =
        "CREATE OR REPLACE TRIGGER " + triggerName + "\n"
      + "AFTER " + eventClause + " ON " + row.sourceTable + "\n"
      + "FOR EACH ROW\n"
      + "DECLARE\n"
      + "  v_target_id VARCHAR2(32);\n"
      + "  CURSOR c_targets IS\n"
      + "    " + oracleResolver + ";\n"
      + "BEGIN\n"
      // NOTE: this enqueue trigger intentionally does NOT check AD_IsTriggerEnabled()/AD_Disable_Triggers.
      // Operations that disable Etendo triggers for bulk work (imports, record cloning, ...) still
      // mutate watched source rows, so the dirty row MUST be enqueued regardless to keep the stored
      // computed column consistent (this is the sales-order clone symptom). Oracle drains everything
      // through the async Java processor (REFRESH_MODE forced to 'Q'), so no enqueue can recurse within
      // a transaction; the trigger only writes to the AD_STOREDCOLUMN_DIRTY queue. Cross-cycle
      // re-enqueue from the processor's own recompute write is prevented structurally instead: an
      // UPDATE event is never deployed without watched columns (see the caller), and the watched-column
      // value guard below rejects no-op writes. INSERT/DELETE never fire on an UPDATE-only recompute.
      + watchedGuard
      + "  OPEN c_targets;\n"
      + "  LOOP\n"
      + "    FETCH c_targets INTO v_target_id;\n"
      + "    EXIT WHEN c_targets%NOTFOUND;\n"
      + "    CONTINUE WHEN v_target_id IS NULL;\n"
      // Reset dead-letter bookkeeping so a genuine new source change gets a clean retry.
      + "    DELETE FROM ad_storedcolumn_dirty\n"
      + "     WHERE ad_column_id = '" + row.columnId + "' AND target_record_id = v_target_id\n"
      + "       AND is_ignored = 'Y';\n"
      // Dedup on (column, target) among live rows; TRANSACTION_ID stays NULL on Oracle. Forced 'Q'.
      + "    MERGE INTO ad_storedcolumn_dirty d\n"
      + "    USING (SELECT '" + row.columnId + "' AS ad_column_id, v_target_id AS target_record_id,\n"
      // AD_CLIENT_ID taken from the source row that fired the trigger (COALESCE covers DELETE, where
      // :NEW is null, and INSERT, where :OLD is null).
      + "                  COALESCE(:NEW.ad_client_id, :OLD.ad_client_id) AS ad_client_id\n"
      + "             FROM dual) s\n"
      + "    ON (d.ad_column_id = s.ad_column_id\n"
      + "        AND d.target_record_id = s.target_record_id\n"
      + "        AND d.is_ignored = 'N')\n"
      + "    WHEN NOT MATCHED THEN INSERT (\n"
      + "      ad_storedcolumn_dirty_id, ad_client_id, ad_org_id, isactive,\n"
      + "      created, createdby, updated, updatedby,\n"
      + "      ad_column_id, target_record_id, transaction_id,\n"
      + "      refresh_mode, computation_sequence_number\n"
      + "    ) VALUES (\n"
      + "      get_uuid(), s.ad_client_id, '0', 'Y', SYSDATE, '0', SYSDATE, '0',\n"
      + "      s.ad_column_id, s.target_record_id, NULL,\n"
      + "      'Q', " + row.seqNo + "\n"
      + "    );\n"
      + "  END LOOP;\n"
      + "  CLOSE c_targets;\n"
      + "END;";

    cp.getPreparedStatement(ddl).execute();
  }

  /**
   * Applies the single sanctioned Oracle dialect rewrite to a portable resolver: the correlation
   * prefixes {@code NEW.}/{@code OLD.} become {@code :NEW.}/{@code :OLD.} (case-insensitive,
   * dot-qualified). Nothing else is rewritten — {@code FROM dual} and the rest of the body are
   * already portable. The rewritten SQL is used verbatim as the trigger's cursor query; the target id
   * is read positionally (first column) via {@code FETCH … INTO}, so the resolver's output column name
   * is irrelevant and need not be aliased.
   */
  private String toOracleResolver(String resolverSql) {
    return resolverSql
        .replaceAll("(?i)\\bNEW\\.", ":NEW.")
        .replaceAll("(?i)\\bOLD\\.", ":OLD.");
  }

  /** True when the exception chain indicates ORA-04080 (trigger to drop does not exist). */
  private boolean isOracleTriggerMissing(Exception e) {
    Throwable t = e;
    while (t != null) {
      String m = t.getMessage();
      if (m != null && m.contains("ORA-04080")) {
        return true;
      }
      t = t.getCause();
    }
    return false;
  }

  /**
   * Materialises every active {@code AD_COMPDEP_WATCHED_COL} row into a map keyed by lowercased
   * dependency id, with the watched-column <i>names</i> lowercased to match PostgreSQL's default
   * identifier folding and ordered by {@code SEQNO}. Like {@link #QUERY_DEPS}, the cursor is fully
   * drained and released before the caller issues any DDL, because every DDL {@code execute()}
   * commits on the shared connection and would otherwise close an open ResultSet.
   */
  private Map<String, List<String>> loadWatchedColumns(ConnectionProvider cp) throws Exception {
    Map<String, List<String>> byDep = new HashMap<>();
    PreparedStatement ps = cp.getPreparedStatement(QUERY_WATCHED);
    ResultSet rs = ps.executeQuery();
    while (rs.next()) {
      String depId = rs.getString("ad_column_comp_dependency_id").toLowerCase();
      String colName = rs.getString("columnname").toLowerCase();
      byDep.computeIfAbsent(depId, k -> new ArrayList<>()).add(colName);
    }
    cp.releasePreparedStatement(ps);
    return byDep;
  }

  /**
   * Resolves the SQL whose rows are the target record ids to enqueue. Exactly one of two declarative
   * sources is expected per dependency:
   * <ul>
   *   <li>{@code TARGET_ID_RESOLVER_SQL} — used verbatim when set (escape hatch for complex links).</li>
   *   <li>{@code TARGET_LINK_COLUMN_ID} — a source→target FK; the resolver is <i>rendered</i> here
   *       from the FK column name. An immutable parent FK ({@code IsParent='Y'}) renders the compact
   *       {@code COALESCE(NEW.fk, OLD.fk)} form; a mutable FK renders the {@code UNION} form so a
   *       re-pointed row refreshes both the old and the new target.</li>
   * </ul>
   * Returns {@code null} when neither source is set, signalling the caller to skip the dependency.
   */
  private String resolveResolverSql(DepRow row) {
    if (row.resolverSql != null && !row.resolverSql.isBlank()) {
      return row.resolverSql;
    }
    if (row.linkColumn != null && !row.linkColumn.isBlank()) {
      String fk = row.linkColumn.toLowerCase();
      if ("Y".equals(row.linkIsParent)) {
        return "SELECT COALESCE(NEW." + fk + ", OLD." + fk + ") FROM dual";
      }
      return "SELECT NEW." + fk + " FROM dual WHERE NEW." + fk + " IS NOT NULL\n"
          + "  UNION\n"
          + "  SELECT OLD." + fk + " FROM dual WHERE OLD." + fk + " IS NOT NULL";
    }
    return null;
  }

  /**
   * Lightweight portability gate over a resolver before it is deployed. {@code TARGET_ID_RESOLVER_SQL}
   * is authored once in <b>portable</b> SQL so the same string can feed both the PostgreSQL trigger
   * (Phase 4) and the Oracle trigger (Phase 5). Enforcing portability <i>now</i> keeps resolvers
   * Oracle-ready before the Oracle generator exists. The only sanctioned dialect-specific element is
   * the {@code NEW.}/{@code OLD.} correlation prefix, which Phase 5 rewrites to {@code :NEW.}/{@code
   * :OLD.}; everything else must already be portable.
   *
   * <p>Returns a human-readable reason when the resolver contains a known non-portable token, or
   * {@code null} when it passes. A non-null result makes {@link #execute} skip the dependency.</p>
   */
  private String validateResolverPortability(String resolverSql) {
    String upper = resolverSql.toUpperCase();
    if (resolverSql.contains("::")) {
      return "PostgreSQL-only cast '::' — use CAST(... AS ...)";
    }
    if (upper.matches("(?s).*\\bPG_\\w+.*")) {
      return "PostgreSQL-only catalog/function reference 'pg_*'";
    }
    if (upper.matches("(?s).*\\bON\\s+CONFLICT\\b.*")) {
      return "PostgreSQL-only 'ON CONFLICT' clause";
    }
    // Every SELECT must read FROM something (FROM dual for value-only selects) — a bare
    // SELECT ... WHERE with no FROM is valid in PostgreSQL but not in Oracle.
    if (upper.matches("(?s).*\\bSELECT\\b.*") && !upper.matches("(?s).*\\bFROM\\b.*")) {
      return "missing FROM clause — use 'FROM dual' (portable; Etendo ships public.dual on Postgres)";
    }
    return null;
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

  /**
   * Holds one {@code AD_COLUMN_COMP_DEPENDENCY} row, materialised from {@link #QUERY_DEPS} before any
   * DDL is issued. The dependency cursor must be fully drained and released first: every DDL
   * {@code execute()} commits on the shared connection, and under the default
   * {@code CLOSE_CURSORS_AT_COMMIT} holdability that commit would close an open ResultSet, dropping
   * every dependency after the first.
   */
  private static final class DepRow {
    String depId;
    String columnId;
    String insertEvent;
    String updateEvent;
    String deleteEvent;
    String resolverSql;
    String linkColumn;
    String linkIsParent;
    String sourceTable;
    String targetTable;
    String refreshMode;
    int seqNo;
  }

  /**
   * Per-column bookkeeping for the initial-population pass, accumulated across all the column's
   * dependency rows. {@link #hadPreexistingDep} flips to true the moment any one of those dependency
   * functions is found to have existed before this run, which demotes the column from "first
   * activation" (populate now) to "routine re-run" (leave existing values untouched).
   */
  private static final class ColumnPopulation {
    final String columnId;
    final String refreshMode;
    final String targetTable;
    final int seqNo;
    boolean hadPreexistingDep;

    ColumnPopulation(String columnId, String refreshMode, String targetTable, int seqNo) {
      this.columnId = columnId;
      this.refreshMode = refreshMode;
      this.targetTable = targetTable;
      this.seqNo = seqNo;
    }
  }
}
