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

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.StoredComputedValidator.Severity;
import org.openbravo.modulescript.StoredComputedValidator.Violation;

/**
 * Rule-matrix unit tests for {@link StoredComputedValidator#collectDefinitionViolations} — one test
 * per definition-time rule group (EPL-1807, Phase 5b). These are deterministic and DB-free: a mocked
 * {@link ConnectionProvider} routes each SQL query the validator issues to a canned
 * {@link ResultSet} keyed by a unique substring of that query, so exactly one rule's input is
 * "broken" per test and every other query returns an empty result set. Each test then asserts the
 * specific violation {@code code} and {@link Severity} that broken input must produce.
 *
 * <p>Coverage: <b>V4</b> function-missing, <b>V5</b> signature (HARD wrong arg count / WARN non-text
 * arg), <b>V6</b> return type (HARD void / WARN family mismatch), <b>V7</b> volatility, <b>V8</b>
 * no-dependencies, <b>V9</b> update-without-watched, <b>V10</b> watched-column-table, <b>V11</b>
 * target XOR (both set / neither set), <b>V14</b> dependency cycle (full SQL &rarr; graph &rarr;
 * {@code findCycles} wiring), <b>V16</b> FK-index advisory, plus one end-to-end enforce-mode smoke
 * that asserts a {@link BuildException}.</p>
 *
 * <p><b>Not fixtured here:</b> <b>V15</b> deployment drift ({@code checkDeploymentDrift}) is a
 * post-deploy check that compares against objects a live {@code update.database} run just created; it
 * cannot be exercised meaningfully against a mock or a clean DB without a real module deploy, so it is
 * intentionally omitted rather than asserted in a way that could false-pass. Live-DB engine behaviour
 * is covered separately by the self-asserting script wrappers under
 * {@code modules/com.etendoerp.storedcomputedcolumn/src-test/} (e.g.
 * {@code StoredComputedEngineScenariosSqlTest}).</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoredComputedValidatorRulesTest {

  // Unique SQL substrings that identify each query the validator issues (verified against the
  // StoredComputedValidator source). Routing on these keeps every test to a single broken rule.
  private static final String Q_LOADER = "c.sqllogic";
  private static final String Q_PG_FN = "pronargs";
  private static final String Q_V8_NO_DEP = "NOT EXISTS (SELECT 1 FROM ad_column_comp_dependency d";
  private static final String Q_V9_UPDATE = "d.update_event = 'Y'";
  private static final String Q_V10_WATCHED_TABLE = "wc.ad_table_id <> d.source_table_id";
  private static final String Q_V11_XOR = "AS linkcol";
  private static final String Q_V14_EDGES = "AS watched_col_id";
  private static final String Q_V16_FK = "AS fk_col";

  private String savedToggle;

  @BeforeEach
  void saveToggle() {
    savedToggle = System.getProperty(StoredComputedValidator.TOGGLE);
  }

  @AfterEach
  void restoreToggle() {
    if (savedToggle == null) {
      System.clearProperty(StoredComputedValidator.TOGGLE);
    } else {
      System.setProperty(StoredComputedValidator.TOGGLE, savedToggle);
    }
  }

  // ================================================================================================
  // V4–V7 — computation-function correctness (checkFunctions + introspectFunctionPg).
  // ================================================================================================

  @Test
  void v4MissingFunctionIsHard() throws Exception {
    // Loader yields a stored computed column whose function 'myfn' is never found: the pg_proc
    // introspection query returns no row -> exists=false -> ETGO_ScdFunctionMissing (HARD).
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    // Deliberately NO Q_PG_FN route -> introspection sees an empty result set.
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionMissing, Severity.ERROR);
  }

  @Test
  void v5WrongArgumentCountIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    routes.put(Q_PG_FN, singletonList(pgFn(2, "s", "numeric", "text"))); // 2 args, engine passes 1
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionSignature, Severity.ERROR);
  }

  @Test
  void v5NonTextArgumentIsWarn() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    // Exactly one argument (count OK) but it is int4 (NUMERIC family), not the VARCHAR/UUID PK.
    routes.put(Q_PG_FN, singletonList(pgFn(1, "s", "numeric", "int4")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionSignature, Severity.WARN);
  }

  @Test
  void v6VoidReturnTypeIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    routes.put(Q_PG_FN, singletonList(pgFn(1, "s", "void", "text"))); // returns nothing usable
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionReturnType, Severity.ERROR);
  }

  @Test
  void v6ReturnFamilyMismatchIsWarn() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    // Column is an Amount (ref 12 -> NUMERIC) but the function returns text (STRING) -> mismatch WARN.
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    routes.put(Q_PG_FN, singletonList(pgFn(1, "s", "text", "text")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionReturnType, Severity.WARN);
  }

  @Test
  void v7VolatileFunctionIsWarn() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, singletonList(fnColumn("12")));
    // provolatile 'v' = VOLATILE; arg/return kept clean so only V7 fires.
    routes.put(Q_PG_FN, singletonList(pgFn(1, "v", "numeric", "text")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdFunctionVolatile, Severity.WARN);
  }

  // ================================================================================================
  // V8–V11 — dependency correctness (all HARD).
  // ================================================================================================

  @Test
  void v8ColumnWithoutDependenciesIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_V8_NO_DEP, singletonList(row("tablename", "c_order", "columnname", "totalamt")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdNoDependencies, Severity.ERROR);
  }

  @Test
  void v9UpdateEventWithoutWatchedColumnIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_V9_UPDATE, singletonList(row("depid", "dep-1", "source_table", "c_orderline")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdUpdateNoWatched, Severity.ERROR);
  }

  @Test
  void v10WatchedColumnOnForeignTableIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_V10_WATCHED_TABLE, singletonList(row(
        "depid", "dep-1", "watched_col", "qtyordered",
        "source_table", "c_orderline", "watched_table", "m_product")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdWatchedColumnTable, Severity.ERROR);
  }

  @Test
  void v11BothTargetResolversSetIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    // Both Target_ID_Resolver_SQL (non-empty) and Target_Link_Column (non-null) set -> XOR violated.
    routes.put(Q_V11_XOR,
        singletonList(row("depid", "dep-1", "resolver", "SELECT 1", "linkcol", "col-99")));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    Violation v = assertOneViolation(vs, StoredComputedValidator.ETGO_CompDepTargetXor, Severity.ERROR);
    assertTrue(v.detail.contains("both"), "detail must state that both resolvers are set");
  }

  @Test
  void v11NeitherTargetResolverSetIsHard() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    // resolver "" (COALESCE/TRIM empty) and linkcol null -> neither set -> XOR violated.
    routes.put(Q_V11_XOR, singletonList(row("depid", "dep-2", "resolver", "", "linkcol", null)));
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    Violation v = assertOneViolation(vs, StoredComputedValidator.ETGO_CompDepTargetXor, Severity.ERROR);
    assertTrue(v.detail.contains("neither"), "detail must state that neither resolver is set");
  }

  // ================================================================================================
  // V14 — dependency cycle: full loader + edges SQL wired into the pure findCycles detector.
  // ================================================================================================

  @Test
  void v14MutualDependencyCycleIsHard() throws Exception {
    // Two valid stored computed columns A (col-a on tab-x) and B (col-b on tab-y). A's recompute
    // enqueues B (dep on B: source tab-x, watches col-a) and B's enqueues A (dep on A: source tab-y,
    // watches col-b) -> a 2-node cycle with no strict ordering -> ETGO_ScdDependencyCycle (HARD).
    Map<String, Object> colA = fnColumnFull("col-a", "tab-x", 10L, "12");
    Map<String, Object> colB = fnColumnFull("col-b", "tab-y", 20L, "12");

    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_LOADER, Arrays.asList(colA, colB));
    routes.put(Q_PG_FN, singletonList(pgFn(1, "s", "numeric", "text"))); // valid fn -> no V4–V7 noise
    routes.put(Q_V14_EDGES, Arrays.asList(
        row("b_col", "col-b", "src_table", "tab-x", "watched_col_id", "col-a"),   // A -> B
        row("b_col", "col-a", "src_table", "tab-y", "watched_col_id", "col-b"))); // B -> A
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdDependencyCycle, Severity.ERROR);
  }

  // ================================================================================================
  // V16 — FK-index performance advisory (WARN).
  // ================================================================================================

  @Test
  void v16MissingLeadingFkIndexIsWarn() throws Exception {
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_V16_FK,
        singletonList(row("depid", "dep-1", "source_table", "c_orderline", "fk_col", "c_order_id")));
    // No pg_index route -> hasLeadingIndex() sees an empty result set -> no leading index -> advisory.
    List<Violation> vs = StoredComputedValidator.collectDefinitionViolations(router(routes));
    assertOneViolation(vs, StoredComputedValidator.ETGO_ScdMissingIndex, Severity.WARN);
  }

  // ================================================================================================
  // End-to-end smoke — enforce mode + a hard violation stops the build.
  // ================================================================================================

  @Test
  void enforceModeSmokeThrowsBuildExceptionOnHardViolation() throws Exception {
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    Map<String, List<Map<String, Object>>> routes = new LinkedHashMap<>();
    routes.put(Q_V8_NO_DEP, singletonList(row("tablename", "c_order", "columnname", "totalamt")));
    ConnectionProvider cp = router(routes);
    BuildException ex = assertThrows(BuildException.class,
        () -> StoredComputedValidator.assertDefinitionsValid(cp),
        "a hard violation in enforce mode must stop the build");
    assertTrue(ex.getMessage().contains(StoredComputedValidator.ETGO_ScdNoDependencies),
        "the aggregated report must carry the offending code");
  }

  @Test
  void enforceModeSmokePassesOnCleanDefinitions() throws Exception {
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    // Every query returns empty -> no violations -> no throw (the clean-DB baseline: never false-fail).
    ConnectionProvider cp = router(new LinkedHashMap<>());
    assertDoesNotThrow(() -> StoredComputedValidator.assertDefinitionsValid(cp),
        "a clean database must never fail the build");
  }

  // ------------------------------------------------------------------------------------------------
  // Mock ConnectionProvider harness — routes each query to canned rows by a unique SQL substring.
  // ------------------------------------------------------------------------------------------------

  /**
   * Builds a {@link ConnectionProvider} that, for every {@code getPreparedStatement(sql)}, returns a
   * statement whose {@code executeQuery()} yields the canned rows of the first route whose substring
   * {@code sql} contains, or an empty result set when none matches. A fresh {@link ResultSet} is built
   * per {@code executeQuery()} call because the column loader query is executed twice per run.
   */
  private static ConnectionProvider router(Map<String, List<Map<String, Object>>> routes)
      throws Exception {
    ConnectionProvider cp = mock(ConnectionProvider.class);
    when(cp.getRDBMS()).thenReturn("POSTGRESQL");

    Map<String, PreparedStatement> psByRoute = new LinkedHashMap<>();
    for (Map.Entry<String, List<Map<String, Object>>> e : routes.entrySet()) {
      PreparedStatement ps = mock(PreparedStatement.class);
      List<Map<String, Object>> rows = e.getValue();
      when(ps.executeQuery()).thenAnswer(inv -> cannedRs(rows));
      psByRoute.put(e.getKey(), ps);
    }
    PreparedStatement emptyPs = mock(PreparedStatement.class);
    when(emptyPs.executeQuery()).thenAnswer(inv -> cannedRs(Collections.emptyList()));

    when(cp.getPreparedStatement(anyString())).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      for (Map.Entry<String, PreparedStatement> e : psByRoute.entrySet()) {
        if (sql.contains(e.getKey())) {
          return e.getValue();
        }
      }
      return emptyPs;
    });
    return cp;
  }

  /** A stateful {@link ResultSet} mock over the given rows, keyed by column name (String/Int/Long). */
  private static ResultSet cannedRs(List<Map<String, Object>> rows) throws Exception {
    ResultSet rs = mock(ResultSet.class);
    AtomicInteger pos = new AtomicInteger(-1);
    AtomicBoolean lastWasNull = new AtomicBoolean(false);
    when(rs.next()).thenAnswer(inv -> pos.incrementAndGet() < rows.size());
    when(rs.getString(anyString())).thenAnswer(inv -> {
      Object v = rows.get(pos.get()).get((String) inv.getArgument(0));
      lastWasNull.set(v == null);
      return v == null ? null : v.toString();
    });
    when(rs.getInt(anyString())).thenAnswer(inv -> {
      Object v = rows.get(pos.get()).get((String) inv.getArgument(0));
      lastWasNull.set(v == null);
      return v == null ? 0 : ((Number) v).intValue();
    });
    when(rs.getLong(anyString())).thenAnswer(inv -> {
      Object v = rows.get(pos.get()).get((String) inv.getArgument(0));
      lastWasNull.set(v == null);
      return v == null ? 0L : ((Number) v).longValue();
    });
    when(rs.wasNull()).thenAnswer(inv -> lastWasNull.get());
    return rs;
  }

  // ------------------------------------------------------------------------------------------------
  // Row builders + assertions.
  // ------------------------------------------------------------------------------------------------

  /** Builds a column-loader row (a valid stored computed column shape) with the given AD reference. */
  private static Map<String, Object> fnColumn(String refId) {
    return fnColumnFull("col-a", "tab-x", 10L, refId);
  }

  /** Builds a column-loader row with explicit column id / table id / sequence, function 'myfn'. */
  private static Map<String, Object> fnColumnFull(String columnId, String tableId, Long seq,
      String refId) {
    return row(
        "ad_column_id", columnId,
        "ad_table_id", tableId,
        "tablename", "tab",
        "columnname", columnId,
        "computation_mode", "S",
        "sqllogic", null,
        "computation_function", "myfn",
        "computation_sequence_number", seq,
        "ad_reference_id", refId);
  }

  /** Builds a pg_proc introspection row: argument count, provolatile marker, return type, arg type. */
  private static Map<String, Object> pgFn(int pronargs, String provolatile, String rettype,
      String argtype) {
    return row("pronargs", pronargs, "provolatile", provolatile, "rettype", rettype,
        "argtype", argtype);
  }

  /** Builds a single result-set row from alternating (columnName, value) pairs. */
  private static Map<String, Object> row(Object... kv) {
    Map<String, Object> m = new LinkedHashMap<>();
    for (int i = 0; i + 1 < kv.length; i += 2) {
      m.put((String) kv[i], kv[i + 1]);
    }
    return m;
  }

  /**
   * Asserts that exactly one violation carries {@code code} with {@code severity}, and that no OTHER
   * violation code appears (so the single broken rule under test is the only thing that fired).
   */
  private static Violation assertOneViolation(List<Violation> all, String code, Severity severity) {
    List<Violation> matching = new java.util.ArrayList<>();
    List<String> otherCodes = new java.util.ArrayList<>();
    for (Violation v : all) {
      if (code.equals(v.code)) {
        matching.add(v);
      } else {
        otherCodes.add(v.code);
      }
    }
    assertEquals(1, matching.size(), "expected exactly one " + code + " violation, got: " + all);
    assertEquals(severity, matching.get(0).severity, code + " must have severity " + severity);
    assertTrue(otherCodes.isEmpty(),
        "no unrelated rule should fire; unexpected codes: " + otherCodes);
    return matching.get(0);
  }
}
