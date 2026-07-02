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

/**
 * Build-time (single-pass, whole-DB) validator for stored computed column definitions (EPL-1807,
 * Phase 5b). Run under Ant during {@code update.database} through a raw JDBC
 * {@link ConnectionProvider} — there is <b>no</b> {@code OBContext}/{@code OBMessageUtils} here, so
 * every message is an English build-log string and the violation codes below are plain
 * {@code static final String} constants, <b>not</b> {@code AD_MESSAGE} rows. The only two codes that
 * remain {@code AD_MESSAGE} entries are the ones the runtime DAL handlers render in the UI
 * ({@link #ETGO_StoredComputedColDef} for the shape rules V1–V3 and {@link #ETGO_CompDepTargetXor}
 * for the target XOR V11); this validator reuses those same two strings as labels for those rules.
 *
 * <p>This class is the single source of truth for the rule logic:</p>
 * <ul>
 *   <li>{@link #checkShape(String, String, String, Long)} — the pure shape predicate shared with the
 *       runtime DAL guard {@code ColumnStoredComputedHandler} (no DB, no DAL types).</li>
 *   <li>{@link #findCycles(Map, Map)} — the pure three-color DFS cycle detector.</li>
 *   <li>{@link #assertDefinitionsValid(ConnectionProvider)} — the JDBC entry point that runs
 *       V1–V11 + V14 + V16 over every {@code Computation_Mode='S' AND IsActive='Y'} column and, when
 *       any hard rule is violated, throws its own aggregated {@link BuildException}.</li>
 *   <li>{@link #checkDeploymentDrift(ConnectionProvider, boolean, List)} — V15, invoked at the end
 *       of {@code GenerateStoredComputedTriggers.execute()} (post-deploy).</li>
 * </ul>
 *
 * <p><b>Rollout toggle.</b> {@code ETGO_SCD_VALIDATION} (JVM system property first, then environment
 * variable; default {@code enforce}). In {@code warn} mode every hard violation is downgraded to a
 * warning and the build is never stopped — an escape hatch for a grace period. The heuristic rules
 * (V6 type mismatch, V7 volatility, V15 drift, V16 index) are warn-only regardless of the toggle.</p>
 */
public final class StoredComputedValidator {

  private static final Logger log = LogManager.getLogger();

  private StoredComputedValidator() {
  }

  // --- Reused AD_MESSAGE keys (rendered by the runtime DAL handlers; reused here as labels) ------
  /** V1–V3 shape rule — shared with {@code ColumnStoredComputedHandler} (an actual AD_MESSAGE). */
  public static final String ETGO_StoredComputedColDef = "ETGO_StoredComputedColDef";
  /** V11 target XOR — shared with {@code ColumnCompDependencyTargetHandler} (an actual AD_MESSAGE). */
  public static final String ETGO_CompDepTargetXor = "ETGO_CompDepTargetXor";

  // --- Build-only codes (English label strings only — NOT AD_MESSAGE rows) ----------------------
  static final String ETGO_ScdValidationFailed = "ETGO_ScdValidationFailed";
  static final String ETGO_ScdFunctionMissing = "ETGO_ScdFunctionMissing";
  static final String ETGO_ScdFunctionSignature = "ETGO_ScdFunctionSignature";
  static final String ETGO_ScdFunctionReturnType = "ETGO_ScdFunctionReturnType";
  static final String ETGO_ScdFunctionVolatile = "ETGO_ScdFunctionVolatile";
  static final String ETGO_ScdNoDependencies = "ETGO_ScdNoDependencies";
  static final String ETGO_ScdUpdateNoWatched = "ETGO_ScdUpdateNoWatched";
  static final String ETGO_ScdWatchedColumnTable = "ETGO_ScdWatchedColumnTable";
  static final String ETGO_ScdDependencyCycle = "ETGO_ScdDependencyCycle";
  static final String ETGO_ScdTriggerMissing = "ETGO_ScdTriggerMissing";
  static final String ETGO_ScdTriggerDrift = "ETGO_ScdTriggerDrift";
  static final String ETGO_ScdMissingIndex = "ETGO_ScdMissingIndex";

  private static final String STORED_COMPUTED = "S";

  /** Rollout toggle name (JVM system property or environment variable). */
  static final String TOGGLE = "ETGO_SCD_VALIDATION";

  // ================================================================================================
  // Pure predicates (no DB, no DAL types) — the single source of truth shared with the DAL handler.
  // ================================================================================================

  /**
   * Shape rule V1–V3, shared verbatim with the runtime DAL guard {@code ColumnStoredComputedHandler}.
   * Pure: only String/Long arguments, so it is callable from both the {@code src/} observer (DAL) and
   * this build-time validator (JDBC).
   *
   * <p>When {@code computationMode = 'S'} the column is recomputed by a database function, so
   * {@code sqlLogic} MUST be blank, {@code fn} MUST be set, and {@code seq} MUST be a positive number.
   * Returns {@link #ETGO_StoredComputedColDef} when any of the three is violated, otherwise
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
    if (!STORED_COMPUTED.equals(computationMode)) {
      return null;
    }
    boolean hasSqlLogic = isNotBlank(sqlLogic);
    boolean hasFunction = isNotBlank(fn);
    boolean hasSequence = seq != null && seq > 0;
    if (hasSqlLogic || !hasFunction || !hasSequence) {
      return ETGO_StoredComputedColDef;
    }
    return null;
  }

  /**
   * Pure three-color (white/gray/black) DFS cycle detector over a directed dependency graph. A
   * back-edge to a gray node closes a cycle; each distinct cycle (by its node set) is reported once.
   *
   * <p>Each returned {@link Cycle} also carries whether it is broken by a strict refresh ordering:
   * {@link Cycle#seqOrdered} is evaluated over the CLOSED walk (every forward edge plus the closing
   * {@code last -> first} edge) so the classification is rotation-independent — see
   * {@link #isStrictlyOrdered(List, Map)}. A genuine cycle can never be strictly increasing all the
   * way around, so a real cycle is deterministically HARD.</p>
   *
   * @param adjacency
   *          directed edges: {@code node -> list of successors}
   * @param seqByNode
   *          {@code Computation_Sequence_Number} per node (used only for the ordered/unordered split)
   * @return one {@link Cycle} per distinct cycle found (empty when the graph is acyclic)
   */
  public static List<Cycle> findCycles(Map<String, List<String>> adjacency,
      Map<String, Long> seqByNode) {
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
        dfs(node, adjacency, color, path, cycles, seen, seqByNode);
      }
    }
    return cycles;
  }

  private static void dfs(String u, Map<String, List<String>> adjacency, Map<String, Integer> color,
      Deque<String> path, List<Cycle> cycles, Set<String> seen, Map<String, Long> seqByNode) {
    color.put(u, 1);
    path.addLast(u);
    for (String v : adjacency.getOrDefault(u, Collections.<String> emptyList())) {
      int c = color.getOrDefault(v, 0);
      if (c == 0) {
        dfs(v, adjacency, color, path, cycles, seen, seqByNode);
      } else if (c == 1) {
        recordCycle(v, path, cycles, seen, seqByNode);
      }
    }
    color.put(u, 2);
    path.removeLast();
  }

  private static void recordCycle(String backTo, Deque<String> path, List<Cycle> cycles,
      Set<String> seen, Map<String, Long> seqByNode) {
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
    cycles.add(new Cycle(cyc, isStrictlyOrdered(cyc, seqByNode)));
  }

  /**
   * True when {@code seqByNode} imposes a strict order along <b>every</b> edge of the CLOSED walk of
   * the cycle — every forward edge <i>and</i> the closing {@code last -> first} edge. Evaluating the
   * closed walk (rather than only the forward slice DFS happened to capture) makes the result
   * <b>rotation-independent</b>: the same cycle classifies identically no matter which node DFS
   * entered it from. Because a genuine cycle can never be strictly increasing all the way around the
   * loop (that would require {@code seq[first] < ... < seq[last] < seq[first]}), a real cycle in this
   * enqueue graph deterministically classifies as unordered (HARD) here.
   */
  static boolean isStrictlyOrdered(List<String> cyclePath, Map<String, Long> seqByNode) {
    if (cyclePath.size() < 2) {
      return false;
    }
    int n = cyclePath.size();
    for (int i = 0; i < n; i++) {
      Long a = seqByNode.get(cyclePath.get(i));
      Long b = seqByNode.get(cyclePath.get((i + 1) % n)); // wrap: closing last -> first edge
      if (a == null || b == null || a >= b) {
        return false;
      }
    }
    return true;
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
        return "STRING";
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
        return "STRING";
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
   * Runs V1–V11, V14 and V16 and returns every violation found (hard and warn), without applying the
   * toggle or throwing. Separated from {@link #assertDefinitionsValid} so tests can inspect the raw
   * result.
   */
  public static List<Violation> collectDefinitionViolations(ConnectionProvider cp) {
    boolean oracle = isOracle(cp);
    List<Violation> violations = new ArrayList<>();
    List<ColInfo> columns = loadStoredComputedColumns(cp);

    checkShapeRules(columns, violations);
    checkFunctions(cp, oracle, columns, violations);
    checkDependencyExistence(cp, violations);
    checkUpdateWatched(cp, violations);
    checkWatchedColumnTable(cp, violations);
    checkTargetXor(cp, violations);
    checkCycles(cp, violations);
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
          "column " + c.qname() + " — " + String.join("; ", issues)));
    }
  }

  // --- Group B — computation function correctness (V4–V7) ----------------------------------------

  private static void checkFunctions(ConnectionProvider cp, boolean oracle, List<ColInfo> columns,
      List<Violation> violations) {
    // Introspect each distinct function once; report per affected column for a clear message.
    Map<String, FnInfo> byFn = new HashMap<>();
    for (ColInfo c : columns) {
      if (!isNotBlank(c.fn)) {
        continue; // V2 already flags a missing function
      }
      String key = c.fn.toLowerCase();
      FnInfo info = byFn.get(key);
      if (info == null) {
        info = oracle ? introspectFunctionOracle(cp, c.fn) : introspectFunctionPg(cp, c.fn);
        byFn.put(key, info);
      }
      if (info == null) {
        continue; // introspection failed (best-effort) — skip, no violation
      }

      // V4 — function exists.
      if (!info.exists) {
        violations.add(new Violation(Severity.ERROR, ETGO_ScdFunctionMissing,
            "column " + c.qname() + " — Computation_Function '" + c.fn
                + "' does not exist in the database"));
        continue; // nothing else to check for a missing function
      }

      // V5 — signature: exactly one character-typed argument.
      if (info.argCount != null && info.argCount != 1) {
        violations.add(new Violation(Severity.ERROR, ETGO_ScdFunctionSignature,
            "column " + c.qname() + " — Computation_Function '" + c.fn + "' takes " + info.argCount
                + " argument(s); the engine calls it with exactly one (the target row primary key)"));
      } else if (info.argFamily != null && !"STRING".equals(info.argFamily)) {
        violations.add(new Violation(Severity.WARN, ETGO_ScdFunctionSignature,
            "column " + c.qname() + " — Computation_Function '" + c.fn + "' single argument is "
                + info.argType + "; the engine passes a VARCHAR/UUID primary key"));
      }

      // V6 — return type vs AD reference. WARN for a type-family mismatch; HARD when the function
      // returns nothing usable (void/trigger/record).
      if (info.returnType != null) {
        String rt = info.returnType.toLowerCase();
        if ("void".equals(rt) || "trigger".equals(rt) || "record".equals(rt)) {
          violations.add(new Violation(Severity.ERROR, ETGO_ScdFunctionReturnType,
              "column " + c.qname() + " — Computation_Function '" + c.fn + "' returns '" + rt
                  + "', which yields no usable column value"));
        } else {
          String expected = familyForReference(c.refId);
          String actual = familyForPgType(info.returnType);
          if (expected != null && actual != null && !expected.equals(actual)) {
            violations.add(new Violation(Severity.WARN, ETGO_ScdFunctionReturnType,
                "column " + c.qname() + " — Computation_Function '" + c.fn + "' returns "
                    + info.returnType + " (" + actual + ") but the column reference expects "
                    + expected));
          }
        }
      }

      // V7 — side-effect free (PG volatility only; Oracle has no reliable marker).
      if (info.volatile_) {
        violations.add(new Violation(Severity.WARN, ETGO_ScdFunctionVolatile,
            "column " + c.qname() + " — Computation_Function '" + c.fn + "' is declared VOLATILE; a "
                + "recompute function should be IMMUTABLE or STABLE and free of side effects"));
      }
    }
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
          info.volatile_ = "v".equals(prov);
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
          violations.add(new Violation(Severity.ERROR, ETGO_ScdNoDependencies,
              "column " + rs.getString("tablename") + "." + rs.getString("columnname")
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
          violations.add(new Violation(Severity.ERROR, ETGO_ScdUpdateNoWatched,
              "dependency " + rs.getString("depid") + " on source table "
                  + rs.getString("source_table")
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
          violations.add(new Violation(Severity.ERROR, ETGO_ScdWatchedColumnTable,
              "dependency " + rs.getString("depid") + " — watched column "
                  + rs.getString("watched_col") + " belongs to table "
                  + rs.getString("watched_table")
                  + ", not the dependency source table " + rs.getString("source_table")));
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
            violations.add(new Violation(Severity.ERROR, ETGO_CompDepTargetXor,
                "dependency " + rs.getString("depid") + " — "
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

  // --- Group E — cycle detection (V14) -----------------------------------------------------------

  private static void checkCycles(ConnectionProvider cp, List<Violation> violations) {
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

    Map<String, List<String>> adjacency = new HashMap<>();
    // For each active dependency of a stored computed column B: source_table_id + watched column ids.
    String sql =
        "SELECT d.ad_column_id AS b_col, d.source_table_id AS src_table, "
      + "       w.ad_column_id AS watched_col_id "
      + "FROM   ad_column_comp_dependency d "
      + "JOIN   ad_column c ON c.ad_column_id = d.ad_column_id "
      + "         AND c.computation_mode = 'S' AND c.isactive = 'Y' "
      + "JOIN   ad_compdep_watched_col w "
      + "         ON w.ad_column_comp_dependency_id = d.ad_column_comp_dependency_id "
      + "         AND w.isactive = 'Y' "
      + "WHERE  d.isactive = 'Y'";
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          String bCol = rs.getString("b_col");
          String srcTable = rs.getString("src_table");
          String watchedColId = rs.getString("watched_col_id");
          // A is a stored computed node whose column IS the watched column AND lives on the source table.
          ColInfo a = nodeById.get(watchedColId);
          if (a != null && srcTable != null && srcTable.equals(a.tableId)
              && !watchedColId.equals(bCol)) {
            adjacency.computeIfAbsent(watchedColId, k -> new ArrayList<>()).add(bCol);
          }
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw wrap("V14 cycle edges", e);
    }

    for (Cycle cycle : findCycles(adjacency, seqByNode)) {
      StringBuilder path = new StringBuilder();
      for (int i = 0; i < cycle.path.size(); i++) {
        ColInfo c = nodeById.get(cycle.path.get(i));
        path.append(c != null ? c.qname() : cycle.path.get(i)).append(" -> ");
      }
      // close the loop back to the first node
      ColInfo first = nodeById.get(cycle.path.get(0));
      path.append(first != null ? first.qname() : cycle.path.get(0));
      if (cycle.seqOrdered) {
        violations.add(new Violation(Severity.WARN, ETGO_ScdDependencyCycle,
            "cycle among stored computed columns (" + path
                + ") — broken by strictly increasing Computation_Sequence_Number, so it is ordered"));
      } else {
        violations.add(new Violation(Severity.ERROR, ETGO_ScdDependencyCycle,
            "cycle among stored computed columns (" + path
                + ") with no strict refresh ordering (Computation_Sequence_Number)"));
      }
    }
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
          fks.add(new String[] { rs.getString("depid"), rs.getString("source_table"),
              rs.getString("fk_col") });
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
      for (String[] fk : fks) {
        if (!hasLeadingIndex(cp, oracle, fk[1], fk[2])) {
          violations.add(new Violation(Severity.WARN, ETGO_ScdMissingIndex,
              "dependency " + fk[0] + " — no index leads with FK column " + fk[2] + " on source table "
                  + fk[1] + "; target lookups may scan"));
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
        if (!oracleObjectExists(cp, "TRIGGER", triggerName)) {
          violations.add(new Violation(Severity.ERROR, ETGO_ScdTriggerMissing,
              "dependency " + dep.depId + " — expected Oracle trigger " + triggerName
                  + " is not present after generation"));
        }
        // Oracle body drift is best-effort and skipped to avoid false positives from formatting.
      } else {
        boolean funcPresent = pgCount(cp, "SELECT count(*) FROM pg_proc WHERE proname = ?", funcName) > 0;
        boolean trigPresent =
            pgCount(cp, "SELECT count(*) FROM pg_trigger WHERE tgname = ?", triggerName) > 0;
        if (!funcPresent) {
          violations.add(new Violation(Severity.ERROR, ETGO_ScdTriggerMissing,
              "dependency " + dep.depId + " — expected PG function " + funcName
                  + " is not present after generation"));
        }
        if (!trigPresent) {
          violations.add(new Violation(Severity.ERROR, ETGO_ScdTriggerMissing,
              "dependency " + dep.depId + " — expected PG trigger " + triggerName
                  + " is not present after generation"));
        }
        if (funcPresent && dep.expectedPgFunctionDdl != null) {
          checkPgBodyDrift(cp, dep, funcName, violations);
        }
      }
    }
    return violations;
  }

  private static void checkPgBodyDrift(ConnectionProvider cp, DeployedDep dep, String funcName,
      List<Violation> violations) {
    try {
      String deployedBody = null;
      PreparedStatement ps = cp.getPreparedStatement(
          "SELECT pg_get_functiondef(oid) AS def FROM pg_proc WHERE proname = ?");
      try {
        ps.setString(1, funcName);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
          deployedBody = extractPlpgsqlBody(rs.getString("def"));
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
      String expectedBody = extractPlpgsqlBody(dep.expectedPgFunctionDdl);
      if (deployedBody != null && expectedBody != null
          && !normalizeWs(deployedBody).equals(normalizeWs(expectedBody))) {
        violations.add(new Violation(Severity.WARN, ETGO_ScdTriggerDrift,
            "dependency " + dep.depId + " — deployed function " + funcName
                + " body differs from the freshly generated definition (a re-run self-heals)"));
      }
    } catch (Exception e) {
      log.warn("SCD validation: could not compare drift for {} — skipping: {}", funcName,
          e.getMessage());
    }
  }

  /** Extracts the body between the first and last dollar-quote delimiters, else returns input. */
  private static String extractPlpgsqlBody(String def) {
    if (def == null) {
      return null;
    }
    int first = def.indexOf('$');
    if (first < 0) {
      return def;
    }
    int open = def.indexOf('$', first + 1);
    if (open < 0) {
      return def;
    }
    int last = def.lastIndexOf('$');
    int beforeLast = def.lastIndexOf('$', last - 1);
    if (beforeLast <= open) {
      return def;
    }
    return def.substring(open + 1, beforeLast);
  }

  private static String normalizeWs(String s) {
    return s.replaceAll("\\s+", " ").trim();
  }

  private static boolean oracleObjectExists(ConnectionProvider cp, String type, String name) {
    try {
      PreparedStatement ps = cp.getPreparedStatement(
          "SELECT count(*) AS n FROM user_objects WHERE object_type = ? AND lower(object_name) = lower(?)");
      try {
        ps.setString(1, type);
        ps.setString(2, name);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt("n") > 0;
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      log.warn("SCD validation: could not check Oracle {} {} — assuming present: {}", type, name,
          e.getMessage());
      return true;
    }
  }

  private static long pgCount(ConnectionProvider cp, String sql, String param) {
    try {
      PreparedStatement ps = cp.getPreparedStatement(sql);
      try {
        ps.setString(1, param);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getLong(1) : 0L;
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      log.warn("SCD validation: count query failed ({}) — assuming present: {}", param,
          e.getMessage());
      return 1L;
    }
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
          c.tableName = rs.getString("tablename");
          c.columnName = rs.getString("columnname");
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

  private static BuildException wrap(String phase, Exception e) {
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
    public final Severity severity;
    public final String code;
    public final String detail;

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

  /** A detected cycle: the node path (not repeating the closing node) and whether seq-ordered. */
  public static final class Cycle {
    public final List<String> path;
    public final boolean seqOrdered;

    public Cycle(List<String> path, boolean seqOrdered) {
      this.path = path;
      this.seqOrdered = seqOrdered;
    }
  }

  /**
   * The objects the generator (re)deployed for one dependency this run, passed to
   * {@link #checkDeploymentDrift}. {@code expectedPgFunctionDdl} is the freshly rendered PG function
   * DDL for drift comparison (null on Oracle, where only presence is checked).
   */
  public static final class DeployedDep {
    public final String depId;
    public final String sourceTable;
    public final String expectedPgFunctionDdl;

    public DeployedDep(String depId, String sourceTable, String expectedPgFunctionDdl) {
      this.depId = depId;
      this.sourceTable = sourceTable;
      this.expectedPgFunctionDdl = expectedPgFunctionDdl;
    }
  }

  /** One stored computed column row plus the metadata the rules need. */
  private static final class ColInfo {
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
  private static final class FnInfo {
    boolean exists;
    Integer argCount;
    String argType;
    String argFamily;
    String returnType;
    boolean volatile_;
  }
}
