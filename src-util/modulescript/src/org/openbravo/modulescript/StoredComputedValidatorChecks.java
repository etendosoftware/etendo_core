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
import java.util.List;
import java.util.Map;

import org.openbravo.database.ConnectionProvider;

/**
 * Per-rule helper checks factored out of {@link StoredComputedValidator#checkFunctions},
 * {@link StoredComputedValidator#checkCyclesAndSequenceOrder} and
 * {@link StoredComputedValidator#checkDeploymentDrift} to keep those methods' cognitive complexity down.
 * Kept as a separate top-level, package-private class (rather than a static method group inside
 * {@link StoredComputedValidator}) to keep that class's method count under the Sonar threshold. Pure
 * relocation of existing behavior — no logic change.
 */
class StoredComputedValidatorChecks {

  private StoredComputedValidatorChecks() {
  }

  /** V5 — signature: exactly one character-typed argument. HARD on wrong arg count, WARN on type. */
  static void checkFunctionSignature(StoredComputedValidator.ColInfo c, StoredComputedValidator.FnInfo info,
      List<StoredComputedValidator.Violation> violations) {
    if (info.argCount != null && info.argCount != 1) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_FUNCTION_SIGNATURE,
          StoredComputedValidator.COLUMN_PREFIX + c.qname() + StoredComputedValidator.COMPUTATION_FUNCTION_PREFIX
              + c.fn + "' takes " + info.argCount
              + " argument(s); the engine calls it with exactly one (the target row primary key)"));
    } else if (info.argFamily != null && !StoredComputedValidator.FAMILY_STRING.equals(info.argFamily)) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.WARN,
          StoredComputedValidator.ETGO_SCD_FUNCTION_SIGNATURE,
          StoredComputedValidator.COLUMN_PREFIX + c.qname() + StoredComputedValidator.COMPUTATION_FUNCTION_PREFIX
              + c.fn + "' single argument is " + info.argType + "; the engine passes a VARCHAR/UUID primary key"));
    }
  }

  /**
   * V6 — return type vs AD reference. WARN for a type-family mismatch; HARD when the function
   * returns nothing usable (void/trigger/record).
   */
  static void checkFunctionReturnType(StoredComputedValidator.ColInfo c, StoredComputedValidator.FnInfo info,
      List<StoredComputedValidator.Violation> violations) {
    if (info.returnType == null) {
      return;
    }
    String rt = info.returnType.toLowerCase();
    if ("void".equals(rt) || "trigger".equals(rt) || "record".equals(rt)) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_FUNCTION_RETURN_TYPE,
          StoredComputedValidator.COLUMN_PREFIX + c.qname() + StoredComputedValidator.COMPUTATION_FUNCTION_PREFIX
              + c.fn + "' returns '" + rt + "', which yields no usable column value"));
      return;
    }
    String expected = StoredComputedValidator.familyForReference(c.refId);
    String actual = StoredComputedValidator.familyForPgType(info.returnType);
    if (expected != null && actual != null && !expected.equals(actual)) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.WARN,
          StoredComputedValidator.ETGO_SCD_FUNCTION_RETURN_TYPE,
          StoredComputedValidator.COLUMN_PREFIX + c.qname() + StoredComputedValidator.COMPUTATION_FUNCTION_PREFIX
              + c.fn + "' returns " + info.returnType + " (" + actual + ") but the column reference expects "
              + expected));
    }
  }

  /** V7 — side-effect free (PG volatility only; Oracle has no reliable marker). */
  static void checkFunctionVolatility(StoredComputedValidator.ColInfo c, StoredComputedValidator.FnInfo info,
      List<StoredComputedValidator.Violation> violations) {
    if (info.volatileFlag) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.WARN,
          StoredComputedValidator.ETGO_SCD_FUNCTION_VOLATILE,
          StoredComputedValidator.COLUMN_PREFIX + c.qname() + StoredComputedValidator.COMPUTATION_FUNCTION_PREFIX
              + c.fn + "' is declared VOLATILE; a "
              + "recompute function should be IMMUTABLE or STABLE and free of side effects"));
    }
  }

  /**
   * Builds the V14/V17 dependency graph: node = active {@code 'S'} column, edge {@code A -> B} when
   * B has an active dependency whose source table is A's table and whose watched columns include A's
   * column (A's recompute write then enqueues B).
   */
  static Map<String, List<String>> buildDependencyGraph(ConnectionProvider cp,
      Map<String, StoredComputedValidator.ColInfo> nodeById) {
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
          StoredComputedValidator.ColInfo a = nodeById.get(watchedColId);
          if (a != null && srcTable != null && srcTable.equals(a.tableId)
              && !watchedColId.equals(bCol)) {
            adjacency.computeIfAbsent(watchedColId, k -> new ArrayList<>()).add(bCol);
          }
        }
      } finally {
        cp.releasePreparedStatement(ps);
      }
    } catch (Exception e) {
      throw StoredComputedValidator.wrap("V14 cycle edges", e);
    }
    return adjacency;
  }

  /** Formats and records one HARD violation ({@code ETGO_SCD_DEPENDENCY_CYCLE}) per V14 cycle found. */
  static void reportCycles(Map<String, StoredComputedValidator.ColInfo> nodeById,
      List<StoredComputedValidator.Cycle> cycles, List<StoredComputedValidator.Violation> violations) {
    for (StoredComputedValidator.Cycle cycle : cycles) {
      StringBuilder path = new StringBuilder();
      for (int i = 0; i < cycle.path.size(); i++) {
        StoredComputedValidator.ColInfo c = nodeById.get(cycle.path.get(i));
        path.append(c != null ? c.qname() : cycle.path.get(i)).append(" -> ");
      }
      // close the loop back to the first node
      StoredComputedValidator.ColInfo first = nodeById.get(cycle.path.get(0));
      path.append(first != null ? first.qname() : cycle.path.get(0));
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_DEPENDENCY_CYCLE,
          "cycle among stored computed columns (" + path + ")"));
    }
  }

  /** Formats and records one WARN violation ({@code ETGO_SCD_SEQUENCE_ORDER}) per V17 offending edge. */
  static void reportSequenceOrderViolations(Map<String, StoredComputedValidator.ColInfo> nodeById,
      Map<String, List<String>> adjacency, Map<String, Long> seqByNode,
      List<StoredComputedValidator.Cycle> cycles, List<StoredComputedValidator.Violation> violations) {
    for (StoredComputedValidator.Edge edge : StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seqByNode, cycles)) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.WARN,
          StoredComputedValidator.ETGO_SCD_SEQUENCE_ORDER,
          "refresh ordering: " + qnameOf(nodeById, edge.to)
              + " (Computation_Sequence_Number " + seqText(seqByNode.get(edge.to))
              + ") reads " + qnameOf(nodeById, edge.from)
              + " (Computation_Sequence_Number " + seqText(seqByNode.get(edge.from))
              + "), but the refresh drain processes dirty rows in Computation_Sequence_Number order, "
              + "so " + qnameOf(nodeById, edge.to) + " may be recomputed before "
              + qnameOf(nodeById, edge.from) + " and read a stale value — give "
              + qnameOf(nodeById, edge.from)
              + " a strictly lower Computation_Sequence_Number than "
              + qnameOf(nodeById, edge.to)
              + " (equal numbers do not order: ties break arbitrarily)"));
    }
  }

  /** Formats a column ID as its qualified {@code Table.Column} name, or the raw ID if unknown. */
  private static String qnameOf(Map<String, StoredComputedValidator.ColInfo> nodeById, String columnId) {
    StoredComputedValidator.ColInfo c = nodeById.get(columnId);
    return c != null ? c.qname() : columnId;
  }

  /** Formats a {@code Computation_Sequence_Number} for messages, or {@code "unset"} if null. */
  private static String seqText(Long seq) {
    return seq == null ? "unset" : seq.toString();
  }

  /** Oracle side of {@code checkDeploymentDrift}: presence-only, no reliable body-drift marker. */
  static void checkOracleDeployment(ConnectionProvider cp, StoredComputedValidator.DeployedDep dep,
      String triggerName, List<StoredComputedValidator.Violation> violations) {
    if (!oracleObjectExists(cp, "TRIGGER", triggerName)) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_TRIGGER_MISSING,
          StoredComputedValidator.DEPENDENCY_PREFIX + dep.depId + " — expected Oracle trigger " + triggerName
              + StoredComputedValidator.NOT_PRESENT_AFTER_GENERATION));
    }
    // Oracle body drift is best-effort and skipped to avoid false positives from formatting.
  }

  /** PostgreSQL side of {@code checkDeploymentDrift}: presence plus body-drift comparison. */
  static void checkPgDeployment(ConnectionProvider cp, StoredComputedValidator.DeployedDep dep, String funcName,
      String triggerName, List<StoredComputedValidator.Violation> violations) {
    boolean funcPresent =
        pgCount(cp, "SELECT count(*) FROM pg_proc WHERE proname = ?", funcName) > 0;
    boolean trigPresent =
        pgCount(cp, "SELECT count(*) FROM pg_trigger WHERE tgname = ?", triggerName) > 0;
    if (!funcPresent) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_TRIGGER_MISSING,
          StoredComputedValidator.DEPENDENCY_PREFIX + dep.depId + " — expected PG function " + funcName
              + StoredComputedValidator.NOT_PRESENT_AFTER_GENERATION));
    }
    if (!trigPresent) {
      violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.ERROR,
          StoredComputedValidator.ETGO_SCD_TRIGGER_MISSING,
          StoredComputedValidator.DEPENDENCY_PREFIX + dep.depId + " — expected PG trigger " + triggerName
              + StoredComputedValidator.NOT_PRESENT_AFTER_GENERATION));
    }
    if (funcPresent && dep.expectedPgFunctionDdl != null) {
      checkPgBodyDrift(cp, dep, funcName, violations);
    }
  }

  /** PG presence/drift helper used only by {@link #checkPgDeployment}: dependency existence check. */
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
      StoredComputedValidator.log.warn("SCD validation: could not check Oracle {} {} — assuming present: {}", type,
          name, e.getMessage());
      return true;
    }
  }

  /** Runs a {@code count(*)} query with one string parameter; defaults to "present" on failure. */
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
      StoredComputedValidator.log.warn("SCD validation: count query failed ({}) — assuming present: {}", param,
          e.getMessage());
      return 1L;
    }
  }

  /** PostgreSQL body-drift comparison for {@link #checkPgDeployment} (V15, WARN-only). */
  private static void checkPgBodyDrift(ConnectionProvider cp, StoredComputedValidator.DeployedDep dep,
      String funcName, List<StoredComputedValidator.Violation> violations) {
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
        violations.add(new StoredComputedValidator.Violation(StoredComputedValidator.Severity.WARN,
            StoredComputedValidator.ETGO_SCD_TRIGGER_DRIFT,
            StoredComputedValidator.DEPENDENCY_PREFIX + dep.depId + " — deployed function " + funcName
                + " body differs from the freshly generated definition (a re-run self-heals)"));
      }
    } catch (Exception e) {
      StoredComputedValidator.log.warn("SCD validation: could not compare drift for {} — skipping: {}", funcName,
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
}
