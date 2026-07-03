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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeNoException;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.modulescript.StoredComputedValidator.Severity;
import org.openbravo.modulescript.StoredComputedValidator.Violation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Live-database integration matrix for {@link StoredComputedValidator}. Each test builds a minimal
 * broken (or clean) stored-computed definition directly in the real Application Dictionary tables,
 * inside a single transaction that is <b>always rolled back</b> — nothing is ever committed.
 *
 * <h2>Technique — shared DAL session connection</h2>
 * The validator reads its input through {@code cp.getPreparedStatement(sql)}, which
 * {@link DalConnectionProvider} routes to {@code getPreparedStatement(getConnection(), sql)} — the
 * cached, thread-bound DAL session connection ({@code autoCommit = false}). This test grabs that very
 * connection once via {@code cp.getConnection()} and builds the fixture on it without committing, so
 * the validator's subsequent reads land on the same physical connection and observe the uncommitted
 * rows (read-your-writes). {@link #tearDown()} calls {@code con.rollback()} to discard every fixture —
 * including the {@code CREATE FUNCTION} DDL (PostgreSQL DDL is transactional) and the
 * {@code isindevelopment} toggle. It deliberately does <em>not</em> call {@code
 * releaseRollbackConnection}, which would close the shared DAL session connection.
 *
 * <h2>Robustness</h2>
 * <ul>
 *   <li>The whole suite is skipped (JUnit {@code Assume}) when the EPL-1807 schema is absent or the
 *       backend is not PostgreSQL, so it never fails on an environment that lacks the feature.</li>
 *   <li>Fixture construction that the environment rejects (e.g. dictionary triggers refusing an
 *       in-transaction mutation) downgrades to a skip via {@link org.junit.Assume#assumeNoException},
 *       never a false failure. The deterministic rule logic is already covered by the mock matrix in
 *       {@code StoredComputedValidatorRulesTest}; this suite is a best-effort real-DB confirmation.</li>
 *   <li>Assertions are scoped to a unique fixture marker (a fresh dependency UUID, or the reused
 *       column's qualified name), so any pre-existing real stored-computed columns on the database
 *       cannot influence the result.</li>
 * </ul>
 *
 * <h2>NOTE — build classpath prerequisite</h2>
 * The class under test, {@link StoredComputedValidator}, is a <b>modulescript</b>
 * ({@code src-util/modulescript/src/...}). Modulescript sources are compiled by the ant target
 * {@code compile.modulescript} into {@code src-util/modulescript/build/classes/}, <em>not</em> by
 * Gradle's {@code compileJava} into {@code build/classes/}. The Gradle {@code test} task's classpath
 * includes {@code build/classes/} but not {@code src-util/modulescript/build/classes/}, so a plain
 * {@code ./gradlew test} cannot see the validator and this test fails to compile out of the box.
 * To run it, the modulescript output directory must be added to the test classpath, e.g. in
 * {@code build.gradle}:
 * <pre>{@code
 * dependencies {
 *   testImplementation files("src-util/modulescript/build/classes")
 * }
 * }</pre>
 * (which still requires {@code compile.modulescript} to have run first). Until that wiring lands, the
 * suite is only runnable after manually placing the compiled {@code StoredComputedValidator*.class}
 * on the test classpath.
 */
public class StoredComputedValidatorLiveDbTest extends OBBaseTest {

  private DalConnectionProvider cp;
  private Connection con;
  /** Function names created in-transaction; dropped implicitly by the rollback. */
  private String validFn;

  @Before
  public void setUp() throws Exception {
    cp = new DalConnectionProvider();
    // PostgreSQL-only: the fixtures use PG-specific DDL (CREATE FUNCTION) and catalog lookups.
    assumeTrue("Live SCD fixtures are PostgreSQL-only", "POSTGRE".equals(cp.getRDBMS()));
    // The cached DAL session connection: fixtures are built on it and the validator reads on it.
    con = cp.getConnection();
    assumeTrue("EPL-1807 stored-computed schema is not present", schemaPresent());
    // Dictionary tables are guarded by module-in-development triggers; put core in development so the
    // in-transaction UPDATE/INSERT on AD_COLUMN and friends is allowed. Rolled back in tearDown.
    exec("UPDATE ad_module SET isindevelopment = 'Y' WHERE ad_module_id = '0'");
    // One valid IMMUTABLE, single-text-argument, text-returning function reused as the "good"
    // Computation_Function so unrelated columns never raise a spurious V4/V5/V6/V7.
    validFn = "scdfn_valid_" + suffix();
    exec("CREATE FUNCTION " + validFn + "(p_id text) RETURNS text AS $$ SELECT p_id $$ "
        + "LANGUAGE sql IMMUTABLE");
  }

  @After
  public void tearDown() throws Exception {
    if (con != null) {
      // Roll back on the shared DAL session connection to discard the fixture (rows, CREATE FUNCTION
      // DDL and the isindevelopment toggle). NOT releaseRollbackConnection — that closes the session.
      con.rollback();
      con = null;
    }
  }

  // ================================================================================================
  // Tests — one deliberate defect per fixture; every other aspect is kept valid so exactly one rule
  // fires for the marker.
  // ================================================================================================

  /** Composite-PK target — a stored computed column on a table with a multi-column primary key. */
  @Test
  public void compositePkTargetIsHard() throws Exception {
    String marker;
    try {
      Victim v = pickCompositeKeyVictim();
      assumeTrue("no core table with a composite primary key available", v != null);
      makeStoredColumn(v.colId, validFn);
      // Give it an otherwise-valid dependency so V8 does not also fire for this column.
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdCompositePkTarget, Severity.ERROR);
  }

  /** V8 — an active stored computed column with no active dependency. */
  @Test
  public void noDependenciesIsHard() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, validFn); // valid function + shape, but NO dependency row
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdNoDependencies, Severity.ERROR);
  }

  /** V9 — a dependency declaring an UPDATE event but with no active watched columns. */
  @Test
  public void updateEventWithoutWatchedIsHard() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, validFn);
      // update_event = 'Y', resolver set (XOR ok), no watched columns -> V9.
      marker = insertDependency(v.tableId, v.colId, v.tableId, "Y", "SELECT 1", null);
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdUpdateNoWatched, Severity.ERROR);
  }

  /** V10 — a watched column that belongs to a table other than the dependency source table. */
  @Test
  public void watchedColumnOnForeignTableIsHard() throws Exception {
    String marker;
    try {
      List<Victim> vs = distinctTableVictims(2);
      assumeTrue("need two core single-key tables", vs.size() >= 2);
      Victim c = vs.get(0); // stored computed column, source table = c.tableId
      Victim other = vs.get(1); // watched column lives on a different table
      makeStoredColumn(c.colId, validFn);
      marker = insertDependency(c.tableId, c.colId, c.tableId, "N", "SELECT 1", null);
      insertWatchedCol(marker, other.colId); // wc.ad_table_id <> source_table_id -> V10
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdWatchedColumnTable, Severity.ERROR);
  }

  /** V11 — both Target_ID_Resolver_SQL and Target_Link_Column are set. */
  @Test
  public void bothTargetResolversIsHard() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, validFn);
      // Link column = the source table's indexed primary key, so V16 (missing FK index) is silent
      // and only V11 fires for this dependency.
      marker = insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", v.keyColId);
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_CompDepTargetXor, Severity.ERROR);
  }

  /** V11 — neither Target_ID_Resolver_SQL nor Target_Link_Column is set. */
  @Test
  public void neitherTargetResolverIsHard() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, validFn);
      marker = insertDependency(v.tableId, v.colId, v.tableId, "N", null, null);
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_CompDepTargetXor, Severity.ERROR);
  }

  /** V14 — a genuine mutual dependency cycle among two stored computed columns. */
  @Test
  public void mutualDependencyCycleIsHard() throws Exception {
    String marker;
    try {
      List<Victim> vs = distinctTableVictims(2);
      assumeTrue("need two core single-key tables", vs.size() >= 2);
      Victim a = vs.get(0);
      Victim b = vs.get(1);
      makeStoredColumn(a.colId, validFn);
      makeStoredColumn(b.colId, validFn);
      // Edge a -> b: dependency OF b, source table = a's table, watched column = a.
      String depB = insertDependency(a.tableId, b.colId, a.tableId, "N", "SELECT 1", null);
      insertWatchedCol(depB, a.colId);
      // Edge b -> a: dependency OF a, source table = b's table, watched column = b.
      String depA = insertDependency(b.tableId, a.colId, b.tableId, "N", "SELECT 1", null);
      insertWatchedCol(depA, b.colId);
      marker = a.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdDependencyCycle, Severity.ERROR);
  }

  /** V16 — a Target_Link_Column with no leading index on the source table. */
  @Test
  public void missingLeadingFkIndexIsWarn() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      // The link column is v's own (non-key) column on v's own physical table; skip if it happens
      // to already lead an index (then the advisory correctly would not fire).
      assumeTrue("chosen FK column already leads an index", !columnLeadsIndex(v.tableName, v.columnName));
      makeStoredColumn(v.colId, validFn);
      // Link column set, resolver empty -> XOR satisfied (no V11); V16 evaluates the index.
      marker = insertDependency(v.tableId, v.colId, v.tableId, "N", null, v.colId);
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdMissingIndex, Severity.WARN);
  }

  /** V4 — Computation_Function does not exist in the database. */
  @Test
  public void functionMissingIsHard() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, "scdfn_absent_" + suffix()); // never created
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdFunctionMissing, Severity.ERROR);
  }

  /** V5 — Computation_Function takes a number of arguments other than one. */
  @Test
  public void wrongArgumentCountIsHard() throws Exception {
    String marker;
    try {
      String fn = "scdfn_twoargs_" + suffix();
      exec("CREATE FUNCTION " + fn + "(a text, b text) RETURNS text AS $$ SELECT a || b $$ "
          + "LANGUAGE sql IMMUTABLE");
      Victim v = singleVictim();
      makeStoredColumn(v.colId, fn);
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdFunctionSignature, Severity.ERROR);
  }

  /** V6 — return type family differs from the AD reference family (numeric vs string). */
  @Test
  public void returnFamilyMismatchIsWarn() throws Exception {
    String marker;
    try {
      String fn = "scdfn_retint_" + suffix();
      exec("CREATE FUNCTION " + fn + "(p_id text) RETURNS integer AS $$ SELECT 1 $$ "
          + "LANGUAGE sql IMMUTABLE");
      Victim v = singleVictim();
      // Reference 10 = String -> STRING family; function returns integer -> NUMERIC family -> warn.
      makeStoredColumn(v.colId, fn, "10");
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdFunctionReturnType, Severity.WARN);
  }

  /** V7 — Computation_Function is declared VOLATILE. */
  @Test
  public void volatileFunctionIsWarn() throws Exception {
    String marker;
    try {
      String fn = "scdfn_volatile_" + suffix();
      exec("CREATE FUNCTION " + fn + "(p_id text) RETURNS text AS $$ SELECT p_id $$ "
          + "LANGUAGE sql VOLATILE");
      Victim v = singleVictim();
      makeStoredColumn(v.colId, fn);
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    assertMarkerViolation(collect(), marker,
        StoredComputedValidator.ETGO_ScdFunctionVolatile, Severity.WARN);
  }

  /** A fully valid definition must produce no violation referencing its column. */
  @Test
  public void cleanDefinitionProducesNoViolation() throws Exception {
    String marker;
    try {
      Victim v = singleVictim();
      makeStoredColumn(v.colId, validFn, "10"); // valid immutable text fn, String reference
      // Exactly one target resolver, no watched columns, update_event = 'N' -> nothing to flag.
      insertDependency(v.tableId, v.colId, v.tableId, "N", "SELECT 1", null);
      marker = v.qname();
    } catch (SQLException e) {
      assumeNoException("live fixture not supported by this environment", e);
      return;
    }
    for (Violation viol : collect()) {
      assertFalse("clean fixture unexpectedly produced: " + viol, viol.detail.contains(marker));
    }
  }

  // ================================================================================================
  // Fixture helpers (all operate directly on the dedicated transaction connection).
  // ================================================================================================

  private List<Violation> collect() {
    return StoredComputedValidator.collectDefinitionViolations(cp);
  }

  private void assertMarkerViolation(List<Violation> all, String marker, String code, Severity sev) {
    List<Violation> matched = new ArrayList<>();
    for (Violation v : all) {
      if (v.detail.contains(marker)) {
        matched.add(v);
      }
    }
    assertEquals("expected exactly one violation mentioning '" + marker + "' but got " + matched, 1,
        matched.size());
    assertEquals("violation code for '" + marker + "'", code, matched.get(0).code);
    assertEquals("violation severity for '" + marker + "'", sev, matched.get(0).severity);
  }

  private boolean schemaPresent() {
    return probe("SELECT computation_mode, computation_function, computation_sequence_number, "
            + "ad_reference_id FROM ad_column WHERE 1 = 0")
        && probe("SELECT ad_column_comp_dependency_id, ad_column_id, source_table_id, update_event, "
            + "target_id_resolver_sql, target_link_column_id FROM ad_column_comp_dependency "
            + "WHERE 1 = 0")
        && probe("SELECT ad_compdep_watched_col_id, ad_column_comp_dependency_id, ad_column_id "
            + "FROM ad_compdep_watched_col WHERE 1 = 0");
  }

  private boolean probe(String sql) {
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.executeQuery().close();
      return true;
    } catch (SQLException e) {
      return false;
    }
  }

  private void exec(String sql) throws SQLException {
    try (Statement st = con.createStatement()) {
      st.execute(sql);
    }
  }

  private void makeStoredColumn(String colId, String fn) throws SQLException {
    makeStoredColumn(colId, fn, null);
  }

  /**
   * Repurposes an existing, already-valid column into a stored computed column: mode 'S', the given
   * function, a positive sequence number and blank SQLLogic. When {@code referenceId} is non-null the
   * column reference is set too (used by the return-type-family tests).
   */
  private void makeStoredColumn(String colId, String fn, String referenceId) throws SQLException {
    StringBuilder sql = new StringBuilder(
        "UPDATE ad_column SET computation_mode = 'S', computation_function = ?, "
            + "computation_sequence_number = 10, sqllogic = NULL");
    if (referenceId != null) {
      sql.append(", ad_reference_id = ?");
    }
    sql.append(", updated = now(), updatedby = '0' WHERE ad_column_id = ?");
    try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
      int i = 1;
      ps.setString(i++, fn);
      if (referenceId != null) {
        ps.setString(i++, referenceId);
      }
      ps.setString(i, colId);
      ps.executeUpdate();
    }
  }

  /**
   * Inserts one active dependency for {@code columnId}. Returns the fresh dependency id, used as the
   * unique violation marker for the dependency-scoped rules (V9–V11, V16).
   */
  private String insertDependency(String sourceTableId, String columnId, String depSourceTableId,
      String updateEvent, String resolverSql, String targetLinkColumnId) throws SQLException {
    String depId = SequenceIdData.getUUID();
    String sql = "INSERT INTO ad_column_comp_dependency (ad_column_comp_dependency_id, ad_client_id, "
        + "ad_org_id, isactive, created, createdby, updated, updatedby, ad_column_id, ad_module_id, "
        + "seqno, source_table_id, insert_event, update_event, delete_event, target_id_resolver_sql, "
        + "target_link_column_id) VALUES (?, '0', '0', 'Y', now(), '0', now(), '0', ?, '0', 10, ?, "
        + "'N', ?, 'N', ?, ?)";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, depId);
      ps.setString(2, columnId);
      ps.setString(3, depSourceTableId);
      ps.setString(4, updateEvent);
      ps.setString(5, resolverSql); // null -> no resolver
      ps.setString(6, targetLinkColumnId); // null -> no link column
      ps.executeUpdate();
    }
    return depId;
  }

  private void insertWatchedCol(String depId, String columnId) throws SQLException {
    String sql = "INSERT INTO ad_compdep_watched_col (ad_compdep_watched_col_id, ad_client_id, "
        + "ad_org_id, isactive, created, createdby, updated, updatedby, ad_column_comp_dependency_id, "
        + "ad_column_id, seqno) VALUES (?, '0', '0', 'Y', now(), '0', now(), '0', ?, ?, 10)";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, SequenceIdData.getUUID());
      ps.setString(2, depId);
      ps.setString(3, columnId);
      ps.executeUpdate();
    }
  }

  // ---- victim selection (existing rows reused so we never build a dictionary row from scratch) ----

  private Victim singleVictim() throws SQLException {
    List<Victim> vs = distinctTableVictims(1);
    assumeTrue("no reusable core single-key column available", !vs.isEmpty());
    return vs.get(0);
  }

  /** Picks up to {@code n} reusable columns, each on a distinct core table with a single-key PK. */
  private List<Victim> distinctTableVictims(int n) throws SQLException {
    String sql = "SELECT c.ad_column_id, c.ad_table_id, t.tablename, c.columnname, "
        + "  (SELECT k.ad_column_id FROM ad_column k WHERE k.ad_table_id = c.ad_table_id "
        + "     AND k.iskey = 'Y' AND k.isactive = 'Y' ORDER BY k.ad_column_id LIMIT 1) AS keycol "
        + "FROM ad_column c "
        + "JOIN ad_table t ON t.ad_table_id = c.ad_table_id "
        + "JOIN ad_package p ON p.ad_package_id = t.ad_package_id AND p.ad_module_id = '0' "
        + "WHERE c.isactive = 'Y' AND c.iskey = 'N' "
        + "  AND (c.computation_mode IS NULL OR c.computation_mode <> 'S') "
        + "  AND (SELECT count(*) FROM ad_column k2 WHERE k2.ad_table_id = c.ad_table_id "
        + "         AND k2.iskey = 'Y') = 1 "
        + "ORDER BY t.tablename, c.columnname";
    List<Victim> out = new ArrayList<>();
    List<String> usedTables = new ArrayList<>();
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ResultSet rs = ps.executeQuery();
      while (rs.next() && out.size() < n) {
        String tableId = rs.getString("ad_table_id");
        if (usedTables.contains(tableId)) {
          continue; // one column per table so distinct-table callers get distinct tables
        }
        usedTables.add(tableId);
        out.add(new Victim(rs.getString("ad_column_id"), tableId, rs.getString("tablename"),
            rs.getString("columnname"), rs.getString("keycol")));
      }
      rs.close();
    }
    return out;
  }

  private Victim pickCompositeKeyVictim() throws SQLException {
    String sql = "SELECT c.ad_column_id, c.ad_table_id, t.tablename, c.columnname "
        + "FROM ad_column c "
        + "JOIN ad_table t ON t.ad_table_id = c.ad_table_id "
        + "JOIN ad_package p ON p.ad_package_id = t.ad_package_id AND p.ad_module_id = '0' "
        + "WHERE c.isactive = 'Y' AND c.iskey = 'N' "
        + "  AND (c.computation_mode IS NULL OR c.computation_mode <> 'S') "
        + "  AND (SELECT count(*) FROM ad_column k WHERE k.ad_table_id = c.ad_table_id "
        + "         AND k.iskey = 'Y') > 1 "
        + "ORDER BY t.tablename, c.columnname";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ResultSet rs = ps.executeQuery();
      Victim v = null;
      if (rs.next()) {
        v = new Victim(rs.getString("ad_column_id"), rs.getString("ad_table_id"),
            rs.getString("tablename"), rs.getString("columnname"), null);
      }
      rs.close();
      return v;
    }
  }

  private boolean columnLeadsIndex(String table, String column) throws SQLException {
    String sql = "SELECT 1 FROM pg_index i "
        + "JOIN pg_class tc ON tc.oid = i.indrelid "
        + "JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = i.indkey[0] "
        + "WHERE lower(tc.relname) = lower(?) AND lower(a.attname) = lower(?)";
    try (PreparedStatement ps = con.prepareStatement(sql)) {
      ps.setString(1, table);
      ps.setString(2, column);
      ResultSet rs = ps.executeQuery();
      boolean present = rs.next();
      rs.close();
      return present;
    }
  }

  private static String suffix() {
    return SequenceIdData.getUUID().toLowerCase();
  }

  /** A reusable existing column plus the (single) key column id of its table. */
  private static final class Victim {
    final String colId;
    final String tableId;
    final String tableName;
    final String columnName;
    final String keyColId;

    Victim(String colId, String tableId, String tableName, String columnName, String keyColId) {
      this.colId = colId;
      this.tableId = tableId;
      this.tableName = tableName;
      this.columnName = columnName;
      this.keyColId = keyColId;
    }

    String qname() {
      return tableName + "." + columnName;
    }
  }
}
