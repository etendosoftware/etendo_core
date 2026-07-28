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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.StoredComputedShapeValidator;

/**
 * Build-time (single-pass, whole-DB) validator for stored computed column definitions (EPL-1807,
 * Phase 5b). Run under Ant during {@code update.database} through a raw JDBC
 * {@link ConnectionProvider} — there is <b>no</b> {@code OBContext}/{@code OBMessageUtils} here, so
 * every message is an English build-log string and the violation codes below are plain
 * {@code static final String} constants, <b>not</b> {@code AD_MESSAGE} rows. The only two codes that
 * remain {@code AD_MESSAGE} entries are the ones the runtime DAL handlers render in the UI
 * ({@link #ETGO_STORED_COMPUTED_COL_DEF} for the shape rules V1–V3 and {@link #ETGO_COMP_DEP_TARGET_XOR}
 * for the target XOR V11); this validator reuses those same two strings as labels for those rules.
 *
 * <p>This class is the single source of truth for the rule logic:</p>
 * <ul>
 *   <li>{@link #checkShape(String, String, String, Long)} — the pure shape predicate shared with the
 *       runtime DAL guard {@code ColumnStoredComputedHandler} (no DB, no DAL types).</li>
 *   <li>{@link #findCycles(Map)} — the pure three-color DFS cycle detector.</li>
 *   <li>{@link #findSequenceOrderViolations(Map, Map, List)} — the pure per-edge refresh-ordering
 *       predicate (V17).</li>
 *   <li>{@link #assertDefinitionsValid(ConnectionProvider)} — the JDBC entry point that runs
 *       V1–V11 + V14 + V16 + V17 (all the definition-time shape, function, dependency, cycle,
 *       ordering and index rules catalogued in the Rule index below) over every
 *       {@code Computation_Mode='S' AND IsActive='Y'} column and, when any hard rule is violated,
 *       throws its own aggregated {@link BuildException}.</li>
 *   <li>{@link #checkDeploymentDrift(ConnectionProvider, boolean, List)} — V15 (post-deploy trigger
 *       drift), invoked at the end of {@code GenerateStoredComputedTriggers.execute()}.</li>
 * </ul>
 *
 * <p><b>Rule index.</b> The {@code Vn} codes below are opaque labels; here is what each one actually
 * enforces. Severity <b>(HARD)</b> is an ERROR that stops the build in {@code enforce} mode;
 * <b>(WARN)</b> is only ever logged. Codes <b>V12 and V13 are intentionally unassigned</b> (reserved
 * gaps — no rule was ever numbered with them), so their absence is deliberate, not an omission.</p>
 * <ul>
 *   <li><b>V1–V3</b> — shape of a {@code Computation_Mode='S'} column, all HARD under
 *       {@link #ETGO_STORED_COMPUTED_COL_DEF} (see {@link #checkShape(String, String, String, Long)}):
 *       <b>V1</b> SQLLogic must be blank, <b>V2</b> Computation_Function must be set, <b>V3</b>
 *       Computation_Sequence_Number must be &gt; 0.</li>
 *   <li><b>Composite-PK target</b> (unnumbered, HARD, {@link #ETGO_SCD_COMPOSITE_PK_TARGET}) — rejects a
 *       stored computed column whose target table has a composite (multi-column) primary key, which
 *       the single-PK recompute engine cannot resolve.</li>
 *   <li><b>V4–V7</b> — computation-function correctness: <b>V4</b> the function must exist (HARD,
 *       {@link #ETGO_SCD_FUNCTION_MISSING}); <b>V5</b> it must take exactly one argument, the
 *       target-row primary key — HARD when the argument count is wrong, WARN when that single
 *       argument is non-textual ({@link #ETGO_SCD_FUNCTION_SIGNATURE}); <b>V6</b> its return type must
 *       be usable — HARD for void/trigger/record, WARN on a type-family mismatch against the column's
 *       AD reference ({@link #ETGO_SCD_FUNCTION_RETURN_TYPE}); <b>V7</b> it should be side-effect free —
 *       WARN when the PG function is declared VOLATILE ({@link #ETGO_SCD_FUNCTION_VOLATILE}).</li>
 *   <li><b>V8–V11</b> — dependency correctness, all HARD: <b>V8</b> every stored computed column needs
 *       at least one active dependency ({@link #ETGO_SCD_NO_DEPENDENCIES}); <b>V9</b> a dependency
 *       declaring an UPDATE event needs at least one active watched column
 *       ({@link #ETGO_SCD_UPDATE_NO_WATCHED}); <b>V10</b> each watched column must live on the
 *       dependency's source table ({@link #ETGO_SCD_WATCHED_COLUMN_TABLE}); <b>V11</b> exactly one of
 *       Target_ID_Resolver_SQL / Target_Link_Column must be set ({@link #ETGO_COMP_DEP_TARGET_XOR}).</li>
 *   <li><b>V14</b> — dependency-cycle detection among stored computed columns (HARD,
 *       {@link #ETGO_SCD_DEPENDENCY_CYCLE}): <b>every</b> cycle is an error. A cycle is exactly a
 *       dirty set with no topological order, so no Computation_Sequence_Number assignment can
 *       rescue one — the severity does not depend on the sequence numbers along it.</li>
 *   <li><b>V15</b> — post-deploy trigger drift ({@link #checkDeploymentDrift(ConnectionProvider,
 *       boolean, List)}): a missing deployed trigger/function is HARD
 *       ({@link #ETGO_SCD_TRIGGER_MISSING}); a PG function body that differs from the freshly generated
 *       DDL is WARN ({@link #ETGO_SCD_TRIGGER_DRIFT}, a re-run self-heals).</li>
 *   <li><b>V16</b> — FK-index performance advisory (WARN, {@link #ETGO_SCD_MISSING_INDEX}): no index
 *       leads with a dependency's Target_Link_Column on its source table.</li>
 *   <li><b>V17</b> — per-edge refresh-ordering advisory (WARN, {@link #ETGO_SCD_SEQUENCE_ORDER}): on
 *       the acyclic part of the same graph V14 builds, an edge {@code A -> B} means B's computation
 *       function reads A's stored value, so the drain must recompute A <i>before</i> B. Both drains
 *       order by Computation_Sequence_Number ({@code GenerateStoredComputedTriggers.PROCESS_DIRTY_FN}
 *       for {@code 'S'}, {@code StoredColumnQueueProcessor.FETCH_SQL} for {@code 'Q'}), so a null
 *       sequence on either end, or {@code seq[A] >= seq[B]}, means the drain may visit B first and
 *       read a stale A. Equality trips this rule too: the tie-breaks (target_record_id / created)
 *       are arbitrary with respect to the dependency and guarantee nothing. Warn-only by design: an
 *       unset or non-positive sequence number is already a hard V3 error, so erroring here would only
 *       double-report it, and what is left (two configured columns merely ordered wrongly relative to
 *       each other) is an advisory in the same family as V15/V16 — a fix a developer should make, not
 *       a reason to fail an install. Edges inside a cycle V14 already
 *       reported are suppressed: the cycle is the finding, per-edge noise on top of it is not.</li>
 * </ul>
 *
 * <p><b>Rollout toggle.</b> {@code ETGO_SCD_VALIDATION} (JVM system property first, then environment
 * variable; default {@code enforce}). In {@code warn} mode every hard violation is downgraded to a
 * warning and the build is never stopped — an escape hatch for a grace period. The heuristic rules
 * (V6 type mismatch, V7 volatility, V15 drift, V16 index, V17 ordering) are warn-only regardless of
 * the toggle.</p>
 */
public final class StoredComputedValidator {

  // Package-private (not private): used by StoredComputedValidatorChecks (oracleObjectExists, pgCount,
  // checkPgBodyDrift) in addition to this class's own methods.
  static final Logger log = LogManager.getLogger();

  private StoredComputedValidator() {
  }

  // --- Reused AD_MESSAGE keys (rendered by the runtime DAL handlers; reused here as labels) ------
  /**
   * V1–V3 shape rule — shared with {@code ColumnStoredComputedHandler} (an actual AD_MESSAGE).
   * Re-exposed as a delegating constant to the core single source of truth
   * {@link StoredComputedShapeValidator#ETGO_STORED_COMPUTED_COL_DEF}.
   */
  public static final String ETGO_STORED_COMPUTED_COL_DEF =
      StoredComputedShapeValidator.ETGO_STORED_COMPUTED_COL_DEF;
  /** V11 target XOR — shared with {@code ColumnCompDependencyTargetHandler} (an actual AD_MESSAGE). */
  public static final String ETGO_COMP_DEP_TARGET_XOR = "ETGO_CompDepTargetXor";

  // --- Build-only codes (English label strings only — NOT AD_MESSAGE rows) ----------------------
  static final String ETGO_SCD_VALIDATION_FAILED = "ETGO_ScdValidationFailed";
  static final String ETGO_SCD_FUNCTION_MISSING = "ETGO_ScdFunctionMissing";
  static final String ETGO_SCD_FUNCTION_SIGNATURE = "ETGO_ScdFunctionSignature";
  static final String ETGO_SCD_FUNCTION_RETURN_TYPE = "ETGO_ScdFunctionReturnType";
  static final String ETGO_SCD_FUNCTION_VOLATILE = "ETGO_ScdFunctionVolatile";
  static final String ETGO_SCD_NO_DEPENDENCIES = "ETGO_ScdNoDependencies";
  static final String ETGO_SCD_UPDATE_NO_WATCHED = "ETGO_ScdUpdateNoWatched";
  static final String ETGO_SCD_WATCHED_COLUMN_TABLE = "ETGO_ScdWatchedColumnTable";
  static final String ETGO_SCD_DEPENDENCY_CYCLE = "ETGO_ScdDependencyCycle";
  static final String ETGO_SCD_SEQUENCE_ORDER = "ETGO_ScdSequenceOrder";
  static final String ETGO_SCD_TRIGGER_MISSING = "ETGO_ScdTriggerMissing";
  static final String ETGO_SCD_TRIGGER_DRIFT = "ETGO_ScdTriggerDrift";
  static final String ETGO_SCD_MISSING_INDEX = "ETGO_ScdMissingIndex";
  static final String ETGO_SCD_COMPOSITE_PK_TARGET = "ETGO_ScdCompositePkTarget";

  /** Rollout toggle name (JVM system property or environment variable). */
  static final String TOGGLE = "ETGO_SCD_VALIDATION";

  // --- Duplicated-literal constants (message building blocks and JDBC column labels) --------------
  // Package-private (not private): FAMILY_STRING, COLUMN_PREFIX, COMPUTATION_FUNCTION_PREFIX,
  // DEPENDENCY_PREFIX and NOT_PRESENT_AFTER_GENERATION are also used by StoredComputedValidatorChecks.
  static final String FAMILY_STRING = "STRING";
  static final String COLUMN_PREFIX = "column ";
  static final String COMPUTATION_FUNCTION_PREFIX = " — Computation_Function '";
  private static final String TABLENAME = "tablename";
  private static final String COLUMNNAME = "columnname";
  private static final String DEPID = "depid";
  static final String DEPENDENCY_PREFIX = "dependency ";
  private static final String SOURCE_TABLE = "source_table";
  static final String NOT_PRESENT_AFTER_GENERATION = " is not present after generation";

  // ================================================================================================
  // Pure predicates (no DB, no DAL types) — the single source of truth shared with the DAL handler.
  // ================================================================================================

  /**
   * Shape rule V1–V3, shared verbatim with the runtime DAL guard {@code ColumnStoredComputedHandler}.
   * Delegates to the core single source of truth
   * {@link StoredComputedShapeValidator#checkShape(String, String, String, Long)} so both this
   * build-time validator (JDBC) and the {@code src/} observer (DAL) evaluate the exact same predicate.
   *
   * <p>When {@code computationMode = 'S'} the column is recomputed by a database function, so
   * {@code sqlLogic} MUST be blank, {@code fn} MUST be set, and {@code seq} MUST be a positive number.
   * Returns {@link #ETGO_STORED_COMPUTED_COL_DEF} when any of the three is violated, otherwise
   * {@code null}. Columns that are not stored computed are always valid here.</p>
   *
   * @param computationMode
   *          {@code AD_Column.Computation_Mode}
   * @param sqlLogic
   *          {@code AD_Column.SQLLogic}
   * @param fn
   *          {@code AD_Column.Computation_Function}
   * @param seq
   *          {@code AD_Column.Computation_Sequence_Number}
   * @return the violation code, or {@code null} when the shape is valid
   */
  public static String checkShape(String computationMode, String sqlLogic, String fn, Long seq) {
    return StoredComputedShapeValidator.checkShape(computationMode, sqlLogic, fn, seq);
  }

  /**
   * Pure three-color (white/gray/black) DFS cycle detector over a directed dependency graph. A
   * back-edge to a gray node closes a cycle; each distinct cycle (by its node set) is reported once.
   *
   * <p>Every cycle found is a hard error (V14), unconditionally. A cycle is exactly a dirty set with
   * no topological order, so no {@code Computation_Sequence_Number} assignment can rescue one: the
   * detector therefore reports the path only and carries no severity hint.</p>
   *
   * @param adjacency
   *          directed edges: {@code node -> list of successors}
   * @return one {@link Cycle} per distinct cycle found (empty when the graph is acyclic)
   */
  public static List<Cycle> findCycles(Map<String, List<String>> adjacency) {
    Set<String> nodes = new HashSet<>(adjacency.keySet());
    for (List<String> succ : adjacency.values()) {
      nodes.addAll(succ);
    }
    Map<String, Integer> color = new HashMap<>(); // 0=white, 1=gray, 2=black
    List<Cycle> cycles = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    Deque<String> path = new ArrayDeque<>();
    for (String node : nodes) {
      if (color.getOrDefault(node, 0) == 0) {
        dfs(node, adjacency, color, path, cycles, seen);
      }
    }
    return cycles;
  }

  private static void dfs(String u, Map<String, List<String>> adjacency, Map<String, Integer> color,
      Deque<String> path, List<Cycle> cycles, Set<String> seen) {
    color.put(u, 1);
    path.addLast(u);
    for (String v : adjacency.getOrDefault(u, Collections.<String> emptyList())) {
      int c = color.getOrDefault(v, 0);
      if (c == 0) {
        dfs(v, adjacency, color, path, cycles, seen);
      } else if (c == 1) {
        recordCycle(v, path, cycles, seen);
      }
    }
    color.put(u, 2);
    path.removeLast();
  }

  private static void recordCycle(String backTo, Deque<String> path, List<Cycle> cycles,
      Set<String> seen) {
    List<String> full = new ArrayList<>(path);
    int idx = full.indexOf(backTo);
    if (idx < 0) {
      return;
    }
    List<String> cyc = new ArrayList<>(full.subList(idx, full.size())); // backTo ... u
    String key = new TreeSet<>(cyc).toString();
    if (!seen.add(key)) {
      return;
    }
    cycles.add(new Cycle(cyc));
  }

  /**
   * Pure per-edge refresh-ordering predicate (V17). For every edge {@code A -> B} of the same graph
   * {@link #findCycles(Map)} walks, B's computation function reads A's stored value, so the drain must
   * recompute A <b>before</b> B. Both drains visit the dirty set ordered by
   * {@code Computation_Sequence_Number} ({@code GenerateStoredComputedTriggers.PROCESS_DIRTY_FN} for
   * the {@code 'S'} engine, {@code StoredColumnQueueProcessor.FETCH_SQL} for the {@code 'Q'} engine),
   * so the edge is only honoured when {@code seq[A] < seq[B]}.
   *
   * <p>An edge is returned (i.e. violates the ordering) when either endpoint has no sequence number,
   * or when {@code seq[A] >= seq[B]}. <b>Equality counts:</b> the drains break ties on
   * {@code target_record_id} / {@code created}, both arbitrary with respect to the dependency, so
   * {@code seq[A] == seq[B]} guarantees nothing.</p>
   *
   * <p>Edges whose two endpoints both sit inside one of the {@code cycles} already reported by V14 are
   * skipped: the cycle is the real finding and no sequence assignment can order it, so per-edge noise
   * on top of it is unhelpful. This also naturally suppresses self-loops and chords of a cycle.</p>
   *
   * @param adjacency
   *          directed edges: {@code node -> list of successors}
   * @param seqByNode
   *          {@code Computation_Sequence_Number} per node; a missing/null entry is a violation
   * @param cycles
   *          cycles already reported by V14, whose edges are suppressed here
   * @return the offending edges, ordered by {@code (from, to)} so the report is deterministic
   */
  public static List<Edge> findSequenceOrderViolations(Map<String, List<String>> adjacency,
      Map<String, Long> seqByNode, List<Cycle> cycles) {
    List<Set<String>> cycleNodes = new ArrayList<>();
    for (Cycle c : cycles) {
      cycleNodes.add(new HashSet<>(c.path));
    }
    List<Edge> offending = new ArrayList<>();
    for (Map.Entry<String, List<String>> e : adjacency.entrySet()) {
      String a = e.getKey();
      for (String b : e.getValue()) {
        if (inSameCycle(a, b, cycleNodes)) {
          continue;
        }
        Long sa = seqByNode.get(a);
        Long sb = seqByNode.get(b);
        if (sa == null || sb == null || sa >= sb) {
          offending.add(new Edge(a, b));
        }
      }
    }
    Collections.sort(offending, (x, y) -> {
      int c = x.from.compareTo(y.from);
      return c != 0 ? c : x.to.compareTo(y.to);
    });
    return offending;
  }

  private static boolean inSameCycle(String a, String b, List<Set<String>> cycleNodes) {
    for (Set<String> nodes : cycleNodes) {
      if (nodes.contains(a) && nodes.contains(b)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Maps an {@code AD_Reference_ID} to the coarse SQL type family the column's computation function is
   * expected to return (V6). Deliberately coarse — unmapped references return {@code null} and are
   * skipped, keeping the check warn-only and false-positive-averse.
   */
  static String familyForReference(String adReferenceId) {
    if (adReferenceId == null) {
      return null;
    }
    switch (adReferenceId) {
      case "12":  // Amount
      case "22":  // Number
      case "11":  // Integer
      case "29":  // Quantity
      case "800008": // Amount
      case "800019": // General Quantity
        return "NUMERIC";
      case "10":  // String
      case "14":  // Text
      case "20":  // YesNo (char(1)) — grouped with strings for family purposes
        return FAMILY_STRING;
      case "15":  // Date
      case "16":  // DateTime / Timestamp
        return "DATE";
      default:
        return null;
    }
  }

  /** Maps a PostgreSQL {@code pg_type.typname} to the same coarse family, or {@code null}. */
  static String familyForPgType(String typname) {
    if (typname == null) {
      return null;
    }
    switch (typname.toLowerCase()) {
      case "numeric":
      case "decimal":
      case "int2":
      case "int4":
      case "int8":
      case "float4":
      case "float8":
      case "money":
        return "NUMERIC";
      case "varchar":
      case "bpchar":
      case "char":
      case "text":
      case "name":
        return FAMILY_STRING;
      case "date":
      case "timestamp":
      case "timestamptz":
      case "time":
      case "timetz":
        return "DATE";
      default:
        return null;
    }
  }

  // ================================================================================================
  // Build-time entry points (JDBC).
  // ================================================================================================

  /**
   * Validates <b>every</b> stored computed definition in the DB and, when any hard rule is violated
   * (and the toggle is {@code enforce}), throws a single aggregated {@link BuildException}. Warnings
   * are always logged and never block. This is the first statement of both the dedicated
   * {@code ValidateStoredComputedColumns} ModuleScript and {@code GenerateStoredComputedTriggers}.
   *
   * @param cp
   *          the build-time JDBC connection provider
   * @throws BuildException
   *           when {@code enforce} mode and at least one hard violation was found
   */
  public static void assertDefinitionsValid(ConnectionProvider cp) {
    List<Violation> violations = collectDefinitionViolations(cp);
    finishOrThrow(violations);
  }

  /**
   * Runs V1–V11, V14 and V16 (every definition-time rule in the class Rule index: shape, function,
   * dependency, cycle and FK-index checks) and returns every violation found (hard and warn), without
   * applying the toggle or throwing. Separated from {@link #assertDefinitionsValid} so tests can
   * inspect the raw result.
   *
   * @param cp
   *          the build-time JDBC connection provider used to load and check the definitions
   * @return every violation found (hard and warn), unfiltered by the {@code ETGO_SCD_VALIDATION}
   *         toggle
   */
  public static List<Violation> collectDefinitionViolations(ConnectionProvider cp) {
    boolean oracle = isOracle(cp);
    List<Violation> violations = new ArrayList<>();
    List<ColInfo> columns = loadStoredComputedColumns(cp);

    checkShapeRules(columns, violations);
    checkCompositePkTarget(cp, violations);
    checkFunctions(cp, oracle, columns, violations);
    checkDependencyExistence(cp, violations);
    checkUpdateWatched(cp, violations);
    checkWatchedColumnTable(cp, violations);
    checkTargetXor(cp, violations);
    checkCyclesAndSequenceOrder(cp, violations);
    checkFkIndexes(cp, oracle, violations);

    return violations;
  }

  // --- Group A — shape (V1–V3) -------------------------------------------------------------------

  private static void checkShapeRules(List<ColInfo> columns, List<Violation> violations) {
    for (ColInfo c : columns) {
      String code = checkShape(c.computationMode, c.sqlLogic, c.fn, c.seq);
      if (code == null) {
        continue;
      }
      List<String> issues = new ArrayList<>();
      if (isNotBlank(c.sqlLogic)) {
        issues.add("SQLLogic must be blank");
      }
      if (!isNotBlank(c.fn)) {
        issues.add("Computation_Function must be set");
      }
      if (c.seq == null || c.seq <= 0) {
        issues.add("Computation_Sequence_Number must be greater than 0");
      }
      violations.add(new Violation(Severity.ERROR, code,
          COLUMN_PREFIX + c.qname() + " — " + String.join("; ", issues)));
    }
  }

  // --- Group A2 — single-column target primary key -----------------------------------------------

  /**
   * Rejects any stored computed column whose target table has a composite (multi-column) primary key.
   * The recompute engine resolves the target row through a single primary-key column
   * ({@code UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?} and the metadata subquery
   * {@code SELECT k.columnname ... WHERE k.iskey='Y'} in {@code StoredColumnRecomputer.META_SQL}),
   * which is scalar and therefore fails at runtime ("more than one row") on a composite-key table.
   * Guarding it here at build time turns that latent runtime failure into a clear, actionable error.
   */
  private static void checkCompositePkTarget(ConnectionProvider cp, List<Violation> violations) {
    String sql =
        "SELECT t.tablename, c.columnname, "
      + "       (SELECT count(*) FROM ad_column k "
      + "        WHERE k.ad_table_id = c.ad_table_id AND k.iskey = 'Y') AS keycount "
      + "FROM   ad_column c JOIN ad_table t ON t.ad_table_id = c.ad_table_id "
      + "WHERE  c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "ORDER  BY t.tablename, c.columnname";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          if (rs.getInt("keycount") > 1) {
            violations.add(new Violation(Severity.ERROR, ETGO_SCD_COMPOSITE_PK_TARGET,
                COLUMN_PREFIX + rs.getString(TABLENAME) + "." + rs.getString(COLUMNNAME)
                    + " — target table " + rs.getString(TABLENAME) + " has a composite (multi-column)"
                    + " primary key; the recompute engine only supports single-column primary keys"));
          }
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("composite-PK target", e);
    }
  }

  // --- Group B — computation function correctness (V4–V7) ----------------------------------------

  private static void checkFunctions(ConnectionProvider cp, boolean oracle, List<ColInfo> columns,
      List<Violation> violations) {
    // Introspect each distinct function once; report per affected column for a clear message.
    Map<String, FnInfo> byFn = new HashMap<>();
    for (ColInfo c : columns) {
      processColumnForFunctions(c, byFn, cp, oracle, violations);
    }
  }

  /** Per-column body of {@link #checkFunctions}, extracted so the loop keeps a single exit path (S135). */
  private static void processColumnForFunctions(ColInfo c, Map<String, FnInfo> byFn, ConnectionProvider cp,
      boolean oracle, List<Violation> violations) {
    if (!isNotBlank(c.fn)) {
      return; // V2 (Computation_Function must be set) already flags a missing function
    }
    String key = c.fn.toLowerCase();
    FnInfo info = byFn.get(key);
    if (info == null) {
      info = oracle ? introspectFunctionOracle(cp, c.fn) : introspectFunctionPg(cp, c.fn);
      byFn.put(key, info);
    }
    if (info == null) {
      return; // introspection failed (best-effort) — skip, no violation
    }

    // V4 — function exists.
    if (!info.exists) {
      violations.add(new Violation(Severity.ERROR, ETGO_SCD_FUNCTION_MISSING,
          COLUMN_PREFIX + c.qname() + COMPUTATION_FUNCTION_PREFIX + c.fn
              + "' does not exist in the database"));
      return; // nothing else to check for a missing function
    }

    StoredComputedValidatorChecks.checkFunctionSignature(c, info, violations);
    StoredComputedValidatorChecks.checkFunctionReturnType(c, info, violations);
    StoredComputedValidatorChecks.checkFunctionVolatility(c, info, violations);
  }

  private static FnInfo introspectFunctionPg(ConnectionProvider cp, String fn) {
    String sql =
        "SELECT p.pronargs, p.provolatile, "
      + "       rt.typname AS rettype, "
      + "       (SELECT at.typname FROM pg_type at WHERE at.oid = p.proargtypes[0]) AS argtype "
      + "FROM   pg_proc p JOIN pg_type rt ON rt.oid = p.prorettype "
      + "WHERE  lower(p.proname) = lower(?) "
      + "ORDER  BY p.pronargs LIMIT 1";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ps.setString(1, fn);
        ResultSet rs = ps.executeQuery();
        FnInfo info = new FnInfo();
        if (rs.next()) {
          info.exists = true;
          info.argCount = Integer.valueOf(rs.getInt("pronargs"));
          String prov = rs.getString("provolatile");
          info.volatileFlag = "v".equals(prov);
          info.returnType = rs.getString("rettype");
          info.argType = rs.getString("argtype");
          info.argFamily = familyForPgType(info.argType);
        } else {
          info.exists = false;
        }
        return info;
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      log.warn("SCD validation: could not introspect PG function '{}' — skipping V4–V7 for it: {}",
          fn, e.getMessage());
      return null;
    }
  }

  private static FnInfo introspectFunctionOracle(ConnectionProvider cp, String fn) {
    // Oracle is best-effort: existence only (V4). Signature/return/volatility degrade gracefully.
    String sql =
        "SELECT count(*) AS n FROM all_objects "
      + "WHERE object_type = 'FUNCTION' AND lower(object_name) = lower(?)";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ps.setString(1, fn);
        ResultSet rs = ps.executeQuery();
        FnInfo info = new FnInfo();
        info.exists = rs.next() && rs.getInt("n") > 0;
        // argCount/returnType/volatility left null — not reliably introspectable across packages here.
        return info;
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      log.warn("SCD validation: could not introspect Oracle function '{}' — skipping V4 for it: {}",
          fn, e.getMessage());
      return null;
    }
  }

  // --- Group C — dependency correctness (V8–V11) -------------------------------------------------

  private static void checkDependencyExistence(ConnectionProvider cp, List<Violation> violations) {
    // V8 (revised) — every active stored computed column MUST have at least one active dependency.
    String sql =
        "SELECT t.tablename, c.columnname "
      + "FROM   ad_column c JOIN ad_table t ON t.ad_table_id = c.ad_table_id "
      + "WHERE  c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "AND    NOT EXISTS (SELECT 1 FROM ad_column_comp_dependency d "
      + "                   WHERE d.ad_column_id = c.ad_column_id AND d.isactive = 'Y') "
      + "ORDER  BY t.tablename, c.columnname";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          violations.add(new Violation(Severity.ERROR, ETGO_SCD_NO_DEPENDENCIES,
              COLUMN_PREFIX + rs.getString(TABLENAME) + "." + rs.getString(COLUMNNAME)
                  + " — a stored computed column must have at least one active dependency"));
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("V8 dependency-existence", e);
    }
  }

  private static void checkUpdateWatched(ConnectionProvider cp, List<Violation> violations) {
    // V9 — a dependency declaring an UPDATE event must have at least one active watched column.
    String sql =
        "SELECT d.ad_column_comp_dependency_id AS depid, t.tablename AS source_table "
      + "FROM   ad_column_comp_dependency d "
      + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
      + "         AND c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "JOIN   ad_table t ON t.ad_table_id = d.source_table_id "
      + "WHERE  d.isactive = 'Y' AND d.update_event = 'Y' "
      + "AND    NOT EXISTS (SELECT 1 FROM ad_compdep_watched_col w "
      + "                   WHERE w.ad_column_comp_dependency_id = d.ad_column_comp_dependency_id "
      + "                     AND w.isactive = 'Y')";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          violations.add(new Violation(Severity.ERROR, ETGO_SCD_UPDATE_NO_WATCHED,
              DEPENDENCY_PREFIX + rs.getString(DEPID) + " on source table "
                  + rs.getString(SOURCE_TABLE)
                  + " declares an UPDATE event but has no active watched columns"));
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("V9 update-event-watched", e);
    }
  }

  private static void checkWatchedColumnTable(ConnectionProvider cp, List<Violation> violations) {
    // V10 — each watched column must belong to the dependency's source table.
    String sql =
        "SELECT d.ad_column_comp_dependency_id AS depid, wc.columnname AS watched_col, "
      + "       st.tablename AS source_table, wt.tablename AS watched_table "
      + "FROM   ad_compdep_watched_col w "
      + "JOIN   ad_column_comp_dependency d "
      + "         ON d.ad_column_comp_dependency_id = w.ad_column_comp_dependency_id "
      + "         AND d.isactive = 'Y' "
      + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
      + "         AND c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "JOIN   ad_column wc ON wc.ad_column_id = w.ad_column_id "
      + "JOIN   ad_table wt ON wt.ad_table_id = wc.ad_table_id "
      + "JOIN   ad_table st ON st.ad_table_id = d.source_table_id "
      + "WHERE  w.isactive = 'Y' AND wc.ad_table_id <> d.source_table_id";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          violations.add(new Violation(Severity.ERROR, ETGO_SCD_WATCHED_COLUMN_TABLE,
              DEPENDENCY_PREFIX + rs.getString(DEPID) + " — watched column "
                  + rs.getString("watched_col") + " belongs to table "
                  + rs.getString("watched_table")
                  + ", not the dependency source table " + rs.getString(SOURCE_TABLE)));
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("V10 watched-column-table", e);
    }
  }

  private static void checkTargetXor(ConnectionProvider cp, List<Violation> violations) {
    // V11 — exactly one of Target_ID_Resolver_SQL / Target_Link_Column_ID must be set.
    String sql =
        "SELECT d.ad_column_comp_dependency_id AS depid, "
      + "       COALESCE(TRIM(d.target_id_resolver_sql), '') AS resolver, "
      + "       d.target_link_column_id AS linkcol "
      + "FROM   ad_column_comp_dependency d "
      + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
      + "         AND c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "WHERE  d.isactive = 'Y'";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          boolean hasResolver = !rs.getString("resolver").isEmpty();
          boolean hasLink = rs.getString("linkcol") != null;
          if (hasResolver == hasLink) {
            violations.add(new Violation(Severity.ERROR, ETGO_COMP_DEP_TARGET_XOR,
                DEPENDENCY_PREFIX + rs.getString(DEPID) + " — "
                    + (hasResolver ? "both Target_ID_Resolver_SQL and Target_Link_Column are set"
                        : "neither Target_ID_Resolver_SQL nor Target_Link_Column is set")
                    + " (exactly one is required)"));
          }
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("V11 target-xor", e);
    }
  }

  // --- Group E — cycle detection (V14) and refresh-ordering advisory (V17) -----------------------

  private static void checkCyclesAndSequenceOrder(ConnectionProvider cp, List<Violation> violations) {
    // Build the graph: node = 'S' active column. Edge A -> B when B has an active dependency whose
    // source table is A's table and whose watched columns include A's column (A's recompute write
    // then enqueues B).
    Map<String, ColInfo> nodeById = new LinkedHashMap<>();
    Map<String, Long> seqByNode = new HashMap<>();
    for (ColInfo c : loadStoredComputedColumns(cp)) {
      nodeById.put(c.columnId, c);
      seqByNode.put(c.columnId, c.seq);
    }
    if (nodeById.isEmpty()) {
      return;
    }

    Map<String, List<String>> adjacency = StoredComputedValidatorChecks.buildDependencyGraph(cp, nodeById);

    // V14 — every cycle is hard: a cycle is a dirty set with no topological order, so no
    // Computation_Sequence_Number assignment can make the drain refresh it correctly.
    List<Cycle> cycles = findCycles(adjacency);
    StoredComputedValidatorChecks.reportCycles(nodeById, cycles, violations);

    // V17 — on the acyclic part, each edge A -> B needs seq[A] < seq[B] or the drain refreshes the
    // reader before the value it reads.
    StoredComputedValidatorChecks.reportSequenceOrderViolations(nodeById, adjacency, seqByNode, cycles, violations);
  }


  // --- Group G — performance advisory (V16) ------------------------------------------------------

  private static void checkFkIndexes(ConnectionProvider cp, boolean oracle,
      List<Violation> violations) {
    // For each active dependency with a Target_Link_Column set, advise an index on that FK column of
    // the source table. Warn-only, best-effort.
    String sql =
        "SELECT d.ad_column_comp_dependency_id AS depid, st.tablename AS source_table, "
      + "       lc.columnname AS fk_col "
      + "FROM   ad_column_comp_dependency d "
      + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
      + "         AND c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "JOIN   ad_table st ON st.ad_table_id = d.source_table_id "
      + "JOIN   ad_column lc ON lc.ad_column_id = d.target_link_column_id "
      + "WHERE  d.isactive = 'Y' AND d.target_link_column_id IS NOT NULL";
    try {
      List<String[]> fks = new ArrayList<>();
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          fks.add(new String[] { rs.getString(DEPID), rs.getString(SOURCE_TABLE),
              rs.getString("fk_col") });
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
      for (String[] fk : fks) {
        if (!hasLeadingIndex(cp, oracle, fk[1], fk[2])) {
          violations.add(new Violation(Severity.WARN, ETGO_SCD_MISSING_INDEX,
              DEPENDENCY_PREFIX + fk[0] + " — no index leads with FK column " + fk[2]
                  + " on source table " + fk[1] + "; target lookups may scan"));
        }
      }
    } catch (Exception e) {
      log.warn("SCD validation: could not evaluate FK-index advisory (V16) — skipping: {}",
          e.getMessage());
    }
  }

  private static boolean hasLeadingIndex(ConnectionProvider cp, boolean oracle, String table,
      String column) {
    try {
      String sql = oracle
          ? "SELECT 1 FROM user_ind_columns "
              + "WHERE lower(table_name) = lower(?) AND lower(column_name) = lower(?) "
              + "AND column_position = 1"
          : "SELECT 1 FROM pg_index i "
              + "JOIN pg_class tc ON tc.oid = i.indrelid "
              + "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0] "
              + "WHERE lower(tc.relname) = lower(?) AND lower(a.attname) = lower(?)";
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ps.setString(1, table);
        ps.setString(2, column);
        ResultSet rs = ps.executeQuery();
        return rs.next();
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      // On introspection failure assume an index exists so we do not emit a false advisory.
      log.warn("SCD validation: could not check index on {}.{} — assuming present: {}", table,
          column, e.getMessage());
      return true;
    }
  }

  // ================================================================================================
  // V15 — deployment drift (runs at the END of the generator, post-deploy).
  // ================================================================================================

  /**
   * Verifies that every active dependency's deployed trigger objects exist and, on PostgreSQL,
   * detects body drift against the freshly rendered function DDL. Missing objects are HARD; body
   * drift is WARN (a re-run self-heals). Returns the violation list; the caller passes it to
   * {@link #finishOrThrow(List)} to apply the toggle and possibly stop the build.
   *
   * @param cp
   *          the build-time connection provider (same one that just deployed the objects)
   * @param oracle
   *          true on Oracle (only the inline trigger is deployed; drift check degrades to presence)
   * @param deployed
   *          the objects the generator just (re)deployed this run
   * @return violations found (missing = ERROR, drift = WARN)
   */
  public static List<Violation> checkDeploymentDrift(ConnectionProvider cp, boolean oracle,
      List<DeployedDep> deployed) {
    List<Violation> violations = new ArrayList<>();
    for (DeployedDep dep : deployed) {
      String funcName = "ad_scd_" + dep.depId + "_trf";
      String triggerName = "ad_scd_" + dep.depId + "_trg";
      if (oracle) {
        StoredComputedValidatorChecks.checkOracleDeployment(cp, dep, triggerName, violations);
      } else {
        StoredComputedValidatorChecks.checkPgDeployment(cp, dep, funcName, triggerName, violations);
      }
    }
    return violations;
  }

  // ================================================================================================
  // Toggle, aggregation, reporting.
  // ================================================================================================

  /**
   * Applies the {@code ETGO_SCD_VALIDATION} toggle and either logs or throws. In {@code enforce} mode
   * with at least one hard violation, logs the aggregated report at ERROR and throws a
   * {@link BuildException} carrying it. Otherwise (warn mode, or enforce with only warnings) every
   * violation is logged as a warning and the build proceeds.
   *
   * @param violations
   *          the collected violations (may be empty)
   * @throws BuildException
   *           in enforce mode when a hard violation exists
   */
  public static void finishOrThrow(List<Violation> violations) {
    if (violations.isEmpty()) {
      log.debug("SCD validation: no stored computed definition issues found");
      return;
    }
    boolean enforce = isEnforce();
    long errors = violations.stream().filter(v -> v.severity == Severity.ERROR).count();

    if (enforce && errors > 0) {
      String report = formatReport(violations);
      log.error(report);
      throw new BuildException(report);
    }
    // warn mode, or enforce with only warnings: log each and continue.
    if (!enforce && errors > 0) {
      log.warn("SCD validation running in warn mode (ETGO_SCD_VALIDATION=warn) — {} hard "
          + "violation(s) downgraded to warnings; build will not be stopped", errors);
    }
    for (Violation v : violations) {
      log.warn("[{}] {}: {}", v.severity == Severity.ERROR ? "ERROR" : "WARN ", v.code, v.detail);
    }
  }

  /** Renders the aggregated, human-readable report exactly per the Phase 5b message format. */
  static String formatReport(List<Violation> violations) {
    long errors = violations.stream().filter(v -> v.severity == Severity.ERROR).count();
    long warns = violations.size() - errors;
    StringBuilder sb = new StringBuilder();
    sb.append("Stored computed column validation failed (").append(errors).append(" error(s), ")
        .append(warns).append(" warning(s)):\n");
    for (Violation v : violations) {
      if (v.severity == Severity.ERROR) {
        sb.append("  [ERROR] ").append(v.code).append(": ").append(v.detail).append('\n');
      }
    }
    for (Violation v : violations) {
      if (v.severity == Severity.WARN) {
        sb.append("  [WARN ] ").append(v.code).append(": ").append(v.detail).append('\n');
      }
    }
    sb.append("Fix the definitions above and re-run update.database.");
    return sb.toString();
  }

  /** True when the toggle is in {@code enforce} mode (the default); false when {@code warn}. */
  static boolean isEnforce() {
    String v = System.getProperty(TOGGLE);
    if (v == null || v.isBlank()) {
      v = System.getenv(TOGGLE);
    }
    return v == null || v.isBlank() || !"warn".equalsIgnoreCase(v.trim());
  }

  // ================================================================================================
  // JDBC loaders + small helpers.
  // ================================================================================================

  private static List<ColInfo> loadStoredComputedColumns(ConnectionProvider cp) {
    String sql =
        "SELECT c.ad_column_id, c.ad_table_id, t.tablename, c.columnname, c.computation_mode, "
      + "       c.sqllogic, c.computation_function, c.computation_sequence_number, c.ad_reference_id "
      + "FROM   ad_column c JOIN ad_table t ON t.ad_table_id = c.ad_table_id "
      + "WHERE  c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "ORDER  BY t.tablename, c.columnname";
    List<ColInfo> columns = new ArrayList<>();
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          ColInfo c = new ColInfo();
          c.columnId = rs.getString("ad_column_id");
          c.tableId = rs.getString("ad_table_id");
          c.tableName = rs.getString(TABLENAME);
          c.columnName = rs.getString(COLUMNNAME);
          c.computationMode = rs.getString("computation_mode");
          c.sqlLogic = rs.getString("sqllogic");
          c.fn = rs.getString("computation_function");
          long seq = rs.getLong("computation_sequence_number");
          c.seq = rs.wasNull() ? null : seq;
          c.refId = rs.getString("ad_reference_id");
          columns.add(c);
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("loading stored computed columns", e);
    }
    return columns;
  }

  private static boolean isOracle(ConnectionProvider cp) {
    return "ORACLE".equals(cp.getRDBMS());
  }

  private static boolean isNotBlank(String s) {
    return s != null && !s.trim().isEmpty();
  }

  // Package-private (not private): used by StoredComputedValidatorChecks.buildDependencyGraph.
  static BuildException wrap(String phase, Exception e) {
    return new BuildException(
        "Stored computed column validation could not run (" + phase + "): " + e.getMessage(), e);
  }

  // ================================================================================================
  // Value types.
  // ================================================================================================

  /** Violation severity. */
  public enum Severity {
    ERROR, WARN
  }

  /** One validation finding: its severity, its label {@code code}, and a human-readable detail. */
  public static final class Violation {
    /** Whether this finding stops the build in {@code enforce} mode, or is only ever logged. */
    public final Severity severity;
    /** The rule label (an {@code ETGO_Scd*} build-only code, or an {@code AD_MESSAGE} key for V1–V3/V11). */
    public final String code;
    /** Human-readable, English build-log description of the offending column/dependency/edge. */
    public final String detail;

    /**
     * Creates one validation finding.
     *
     * @param severity
     *          whether this finding stops the build in {@code enforce} mode, or is warn-only
     * @param code
     *          the rule label for this finding
     * @param detail
     *          human-readable description of what was violated and where
     */
    public Violation(Severity severity, String code, String detail) {
      this.severity = severity;
      this.code = code;
      this.detail = detail;
    }

    @Override
    public String toString() {
      return "[" + severity + "] " + code + ": " + detail;
    }
  }

  /** A detected cycle: the node path, not repeating the closing node. Always a hard error (V14). */
  public static final class Cycle {
    /** The cycle's node path (column IDs), in traversal order, not repeating the closing node. */
    public final List<String> path;

    /**
     * Creates a detected cycle.
     *
     * @param path
     *          the cycle's node path (column IDs), in traversal order, not repeating the closing node
     */
    public Cycle(List<String> path) {
      this.path = path;
    }
  }

  /**
   * A directed edge {@code from -> to} of the dependency graph: recomputing {@code from} dirties
   * {@code to}, i.e. {@code to}'s computation function reads {@code from}'s stored value. Returned by
   * {@link #findSequenceOrderViolations(Map, Map, List)} for the edges the drain would visit backwards.
   */
  public static final class Edge {
    /** The node (column ID) whose recompute dirties {@link #to}. */
    public final String from;
    /** The node (column ID) whose computation function reads {@link #from}'s stored value. */
    public final String to;

    /**
     * Creates a directed dependency-graph edge.
     *
     * @param from
     *          the node (column ID) whose recompute dirties {@code to}
     * @param to
     *          the node (column ID) whose computation function reads {@code from}'s stored value
     */
    public Edge(String from, String to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public String toString() {
      return from + " -> " + to;
    }
  }

  /**
   * The objects the generator (re)deployed for one dependency this run, passed to
   * {@link #checkDeploymentDrift}. {@code expectedPgFunctionDdl} is the freshly rendered PG function
   * DDL for drift comparison (null on Oracle, where only presence is checked).
   */
  public static final class DeployedDep {
    /** {@code AD_Column_Comp_Dependency_ID} of the dependency this run (re)deployed objects for. */
    public final String depId;
    /** Table name of the dependency's source table. */
    public final String sourceTable;
    /** The freshly rendered PG function DDL for drift comparison (null on Oracle). */
    public final String expectedPgFunctionDdl;

    /**
     * Creates the record of what the generator (re)deployed for one dependency this run.
     *
     * @param depId
     *          {@code AD_Column_Comp_Dependency_ID} of the dependency
     * @param sourceTable
     *          table name of the dependency's source table
     * @param expectedPgFunctionDdl
     *          the freshly rendered PG function DDL for drift comparison, or {@code null} on Oracle
     */
    public DeployedDep(String depId, String sourceTable, String expectedPgFunctionDdl) {
      this.depId = depId;
      this.sourceTable = sourceTable;
      this.expectedPgFunctionDdl = expectedPgFunctionDdl;
    }
  }

  /** One stored computed column row plus the metadata the rules need. */
  static final class ColInfo {
    String columnId;
    String tableId;
    String tableName;
    String columnName;
    String computationMode;
    String sqlLogic;
    String fn;
    Long seq;
    String refId;

    /** Qualified {@code Table.Column} name for messages. */
    String qname() {
      return tableName + "." + columnName;
    }
  }

  /** Introspected computation-function facts (PG full; Oracle existence-only). */
  static final class FnInfo {
    boolean exists;
    Integer argCount;
    String argType;
    String argFamily;
    String returnType;
    boolean volatileFlag;
  }
}
