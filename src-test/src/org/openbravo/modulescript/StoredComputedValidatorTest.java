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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.StoredComputedValidator.Severity;
import org.openbravo.modulescript.StoredComputedValidator.Violation;

/**
 * Unit tests for {@link StoredComputedValidator} covering the composite-primary-key target guard
 * (EPL-1807, hardening item ENG-W3) and the shared {@code checkShape} shape invariants.
 *
 * <p>The recompute engine resolves a target row through a single scalar primary key
 * ({@code UPDATE <table> SET <col> = <fn>(<pk>) WHERE <pk> = ?}), so a target table with a composite
 * (more than one {@code IsKey='Y'}) primary key would fail at runtime. {@code collectDefinitionViolations}
 * must reject such a column at build time with {@code ETGO_ScdCompositePkTarget}. These tests drive that
 * path through a mocked {@link ConnectionProvider}: only the {@code keycount} query returns rows; every
 * other loader query returns an empty {@link ResultSet}, so the sole variable is the target's key count.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoredComputedValidatorTest {

  // -------------------------------------------------------------------------------------------------
  // ENG-W3 — composite primary-key target guard (checkCompositePkTarget via collectDefinitionViolations)
  // -------------------------------------------------------------------------------------------------

  /**
   * Builds a {@link ConnectionProvider} whose only non-empty result set is the composite-PK
   * {@code keycount} query; it yields one row per supplied key count. Every other query the validator
   * runs (column loader, functions, dependencies, cycles, indexes, ...) returns an empty result set,
   * so the only violations produced come from the key counts under test.
   */
  private ConnectionProvider providerWithKeycounts(int... keycounts) throws Exception {
    ConnectionProvider cp = mock(ConnectionProvider.class);
    when(cp.getRDBMS()).thenReturn("POSTGRESQL");

    PreparedStatement keyPs = mock(PreparedStatement.class);
    // Materialize the keycount ResultSet BEFORE opening the outer stub: keycountResultSet() itself
    // opens when(...) stubs, and doing so inside an unfinished when(keyPs.executeQuery()) call would
    // trip Mockito's UnfinishedStubbingException.
    ResultSet keyRs = keycountResultSet(keycounts);
    when(keyPs.executeQuery()).thenReturn(keyRs);

    ResultSet emptyRs = mock(ResultSet.class);
    when(emptyRs.next()).thenReturn(false);
    PreparedStatement emptyPs = mock(PreparedStatement.class);
    when(emptyPs.executeQuery()).thenReturn(emptyRs);

    when(cp.getPreparedStatement(anyString())).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      // The composite-PK guard is the only query that selects a 'keycount' column.
      return sql.contains("keycount") ? keyPs : emptyPs;
    });
    return cp;
  }

  /** A stateful {@link ResultSet} mock over the given key counts (one row each). */
  private ResultSet keycountResultSet(int... keycounts) throws Exception {
    ResultSet rs = mock(ResultSet.class);
    AtomicInteger pos = new AtomicInteger(-1);
    when(rs.next()).thenAnswer(inv -> pos.incrementAndGet() < keycounts.length);
    when(rs.getInt("keycount")).thenAnswer(inv -> keycounts[pos.get()]);
    when(rs.getString("tablename")).thenAnswer(inv -> "t_" + pos.get());
    when(rs.getString("columnname")).thenAnswer(inv -> "col_" + pos.get());
    return rs;
  }

  private static List<Violation> compositePkViolations(List<Violation> all) {
    List<Violation> out = new ArrayList<>();
    for (Violation v : all) {
      if (StoredComputedValidator.ETGO_ScdCompositePkTarget.equals(v.code)) {
        out.add(v);
      }
    }
    return out;
  }

  @Test
  void compositePkTargetIsRejected() throws Exception {
    ConnectionProvider cp = providerWithKeycounts(2);
    List<Violation> composite = compositePkViolations(
        StoredComputedValidator.collectDefinitionViolations(cp));
    assertEquals(1, composite.size(), "a target with 2 key columns must be rejected");
    assertEquals(Severity.ERROR, composite.get(0).severity);
    assertEquals(StoredComputedValidator.ETGO_ScdCompositePkTarget, composite.get(0).code);
  }

  @Test
  void singlePkTargetIsAccepted() throws Exception {
    ConnectionProvider cp = providerWithKeycounts(1);
    assertEquals(0, compositePkViolations(
        StoredComputedValidator.collectDefinitionViolations(cp)).size(),
        "a single-column primary key must not raise ETGO_ScdCompositePkTarget");
  }

  @Test
  void onlyCompositeTargetsAmongManyAreRejected() throws Exception {
    // Two stored columns scanned: one composite (keycount 2), one single (keycount 1).
    ConnectionProvider cp = providerWithKeycounts(2, 1);
    assertEquals(1, compositePkViolations(
        StoredComputedValidator.collectDefinitionViolations(cp)).size(),
        "exactly the composite target of the two must be rejected");
  }

  @Test
  void higherOrderCompositeKeyIsRejected() throws Exception {
    ConnectionProvider cp = providerWithKeycounts(3);
    assertEquals(1, compositePkViolations(
        StoredComputedValidator.collectDefinitionViolations(cp)).size(),
        "a 3-column primary key is still composite and must be rejected");
  }

  // -------------------------------------------------------------------------------------------------
  // Shared shape invariants (checkShape) — pure, no DB.
  // -------------------------------------------------------------------------------------------------

  @Test
  void validStoredComputedShapeReturnsNull() {
    assertNull(StoredComputedValidator.checkShape("S", "", "sum_fn", 10L));
    assertNull(StoredComputedValidator.checkShape("S", null, "sum_fn", 10L));
  }

  @Test
  void nonStoredComputedColumnsAreAlwaysValid() {
    // Mode != 'S' short-circuits before any invariant — sqlLogic/fn/seq are irrelevant.
    assertNull(StoredComputedValidator.checkShape("D", "SELECT 1", null, null));
    assertNull(StoredComputedValidator.checkShape(null, "SELECT 1", null, null));
    assertNull(StoredComputedValidator.checkShape("", "SELECT 1", null, null));
  }

  @Test
  void storedComputedWithSqlLogicIsRejected() {
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", "SELECT 1", "sum_fn", 10L));
  }

  @Test
  void storedComputedWithoutFunctionIsRejected() {
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", null, null, 10L));
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", null, "   ", 10L));
  }

  @Test
  void storedComputedWithNonPositiveOrMissingSequenceIsRejected() {
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", null, "sum_fn", null));
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", null, "sum_fn", 0L));
    assertEquals(StoredComputedValidator.ETGO_StoredComputedColDef,
        StoredComputedValidator.checkShape("S", null, "sum_fn", -1L));
  }
}
