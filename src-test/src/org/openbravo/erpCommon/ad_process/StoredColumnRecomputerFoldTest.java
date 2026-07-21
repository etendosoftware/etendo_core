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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for the dialect-aware identifier fold/quote seam of {@link StoredColumnRecomputer}
 * (EPL-1807, hardening item B1).
 *
 * <p>The engine reads RAW mixed-case AD identifiers ({@code AD_TABLE.tablename},
 * {@code AD_COLUMN.columnname}, {@code Computation_Function}, and the {@code IsKey='Y'} primary-key
 * column) and folds them to the physical case the active dialect stores them in — <b>lowercase on
 * PostgreSQL, UPPERCASE on Oracle</b> — before quoting with a double quote on both dialects. These
 * tests capture the exact SQL the recompute path issues on a mocked {@link Connection} and prove the
 * SAME raw metadata yields quoted-lowercase physical SQL on PostgreSQL and quoted-UPPERCASE physical
 * SQL on Oracle, in the {@code FOR UPDATE} lock, the {@code UPDATE ... = fn(pk)} write, and the
 * keyset-pagination chunk {@code SELECT}. A column with a missing primary key must issue no
 * lock/update at all.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoredColumnRecomputerFoldTest {

  // RAW AD identifiers are deliberately mixed-case so a wrong (or absent) fold is observable in BOTH
  // directions: lower-folding changes them on PostgreSQL, upper-folding changes them on Oracle.
  private static final String RAW_TABLE = "C_Order";
  private static final String RAW_COLUMN = "TotalLines";
  private static final String RAW_FN = "C_Order_Recompute_TotalLines";
  private static final String RAW_PK = "C_Order_ID";

  private static final String COLUMN_ID = "COL-1807";
  private static final String TARGET_ID = "ORD-1";
  private static final String CLIENT_ID = "CLIENT-A";

  /**
   * Holds a mocked Connection, the list of SQL strings passed to {@code prepareStatement}, and the
   * generic {@link PreparedStatement} every non-metadata query is answered with (so tests can verify
   * parameter bindings such as the client-id filter).
   */
  private static final class Capture {
    private final Connection con;
    private final List<String> sql = new ArrayList<>();
    private final PreparedStatement genericPs;

    private Capture(Connection con, PreparedStatement genericPs) {
      this.con = con;
      this.genericPs = genericPs;
    }
  }

  /**
   * Builds a mocked {@link Connection} that records every {@code prepareStatement} SQL and answers the
   * metadata query (identified by the literal {@code computation_function}) with the given RAW
   * identifiers; any other statement is a generic no-op whose {@code executeQuery} returns an empty
   * {@link ResultSet}. A null {@code rawPk} models a column whose target has no single primary key.
   */
  private Capture newConnection(String rawTable, String rawColumn, String rawFn, String rawPk)
      throws SQLException {
    Connection con = mock(Connection.class);

    ResultSet metaRs = mock(ResultSet.class);
    when(metaRs.next()).thenReturn(true, false);
    when(metaRs.getString(1)).thenReturn(rawTable);
    when(metaRs.getString(2)).thenReturn(rawColumn);
    when(metaRs.getString(3)).thenReturn(rawFn);
    when(metaRs.getString(4)).thenReturn(rawPk);
    PreparedStatement metaPs = mock(PreparedStatement.class);
    when(metaPs.executeQuery()).thenReturn(metaRs);

    ResultSet emptyRs = mock(ResultSet.class);
    when(emptyRs.next()).thenReturn(false);
    PreparedStatement genericPs = mock(PreparedStatement.class);
    when(genericPs.executeQuery()).thenReturn(emptyRs);
    when(genericPs.execute()).thenReturn(true);
    when(genericPs.executeUpdate()).thenReturn(1);

    Capture capture = new Capture(con, genericPs);
    when(con.prepareStatement(anyString())).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      capture.sql.add(sql);
      // META_SQL is the only statement selecting computation_function.
      return sql.contains("computation_function") ? metaPs : genericPs;
    });
    return capture;
  }

  private static String only(List<String> sqls, String needle) {
    String found = null;
    for (String s : sqls) {
      if (s.contains(needle)) {
        assertTrue(found == null, "expected exactly one SQL containing '" + needle + "', got >1");
        found = s;
      }
    }
    assertTrue(found != null, "no SQL containing '" + needle + "' was issued: " + sqls);
    return found;
  }

  @Test
  void postgresFoldsIdentifiersToQuotedLowercaseInLockAndUpdate() throws SQLException {
    Capture c = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    StoredColumnRecomputer pg = new StoredColumnRecomputer(false);

    assertTrue(pg.recomputeOne(c.con, COLUMN_ID, TARGET_ID));

    assertEquals("SELECT 1 FROM \"c_order\" WHERE \"c_order_id\" = ? FOR UPDATE",
        only(c.sql, "FOR UPDATE"));
    assertEquals(
        "UPDATE \"c_order\" SET \"totallines\" = \"c_order_recompute_totallines\"(\"c_order_id\")"
            + " WHERE \"c_order_id\" = ?",
        only(c.sql, "UPDATE "));
  }

  @Test
  void oracleFoldsIdentifiersToQuotedUppercaseInLockAndUpdate() throws SQLException {
    Capture c = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    StoredColumnRecomputer ora = new StoredColumnRecomputer(true);

    assertTrue(ora.recomputeOne(c.con, COLUMN_ID, TARGET_ID));

    assertEquals("SELECT 1 FROM \"C_ORDER\" WHERE \"C_ORDER_ID\" = ? FOR UPDATE",
        only(c.sql, "FOR UPDATE"));
    assertEquals(
        "UPDATE \"C_ORDER\" SET \"TOTALLINES\" = \"C_ORDER_RECOMPUTE_TOTALLINES\"(\"C_ORDER_ID\")"
            + " WHERE \"C_ORDER_ID\" = ?",
        only(c.sql, "UPDATE "));
  }

  @Test
  void quoteCharacterIsAlwaysDoubleQuoteOnBothDialects() throws SQLException {
    // Both dialects quote with '"'; the only difference is the folded CASE inside the quotes.
    Capture pg = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    new StoredColumnRecomputer(false).recomputeOne(pg.con, COLUMN_ID, TARGET_ID);
    Capture ora = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    new StoredColumnRecomputer(true).recomputeOne(ora.con, COLUMN_ID, TARGET_ID);

    for (String sql : pg.sql) {
      assertFalse(sql.contains("`") || sql.contains("["), "unexpected quote style: " + sql);
    }
    assertTrue(only(pg.sql, "FOR UPDATE").startsWith("SELECT 1 FROM \""));
    assertTrue(only(ora.sql, "FOR UPDATE").startsWith("SELECT 1 FROM \""));
  }

  @Test
  void chunkSelectFoldsIdentifiersPerDialectDuringRebuild() throws SQLException {
    Capture pg = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    // Empty target table -> one bounded chunk SELECT, no recompute, count 0.
    assertEquals(0, new StoredColumnRecomputer(false).rebuild(pg.con, COLUMN_ID));
    assertEquals("SELECT \"c_order_id\" FROM \"c_order\" ORDER BY \"c_order_id\"",
        only(pg.sql, "ORDER BY"));

    Capture ora = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    assertEquals(0, new StoredColumnRecomputer(true).rebuild(ora.con, COLUMN_ID));
    assertEquals("SELECT \"C_ORDER_ID\" FROM \"C_ORDER\" ORDER BY \"C_ORDER_ID\"",
        only(ora.sql, "ORDER BY"));
  }

  @Test
  void clientScopedRebuildAppendsFoldedClientFilterAndBindsClientIdOnPostgres()
      throws SQLException {
    // Per-client rebuild overload (EPL-1807 per-client enqueue): the keyset chunk SELECT must append
    // a "ad_client_id" = ? filter, folded to lowercase and double-quoted on PostgreSQL, and bind the
    // clientId. The target is empty, so afterPk is null and clientId binds at parameter index 1.
    Capture pg = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    assertEquals(0, new StoredColumnRecomputer(false).rebuild(pg.con, COLUMN_ID, CLIENT_ID));

    assertEquals(
        "SELECT \"c_order_id\" FROM \"c_order\" WHERE \"ad_client_id\" = ? ORDER BY \"c_order_id\"",
        only(pg.sql, "ORDER BY"));
    verify(pg.genericPs).setString(1, CLIENT_ID);
  }

  @Test
  void clientScopedRebuildAppendsFoldedClientFilterAndBindsClientIdOnOracle() throws SQLException {
    // Same client-scoped rebuild on Oracle: the filter column is folded to UPPERCASE and quoted, and
    // the clientId is bound. Proves the client filter shares the engine's dialect-aware fold seam.
    Capture ora = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    assertEquals(0, new StoredColumnRecomputer(true).rebuild(ora.con, COLUMN_ID, CLIENT_ID));

    assertEquals(
        "SELECT \"C_ORDER_ID\" FROM \"C_ORDER\" WHERE \"AD_CLIENT_ID\" = ? ORDER BY \"C_ORDER_ID\"",
        only(ora.sql, "ORDER BY"));
    verify(ora.genericPs).setString(1, CLIENT_ID);
  }

  @Test
  void allClientRebuildOverloadAppendsNoClientFilter() throws SQLException {
    // The all-client overload (null clientId) must NOT append any client filter and must never bind a
    // client id — it is the deliberate cross-client repair path (System manual rebuild).
    Capture pg = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, RAW_PK);
    assertEquals(0, new StoredColumnRecomputer(false).rebuild(pg.con, COLUMN_ID));

    String chunk = only(pg.sql, "ORDER BY");
    assertFalse(chunk.contains("ad_client_id"), "all-client rebuild must not filter by client: " + chunk);
    assertFalse(chunk.contains(" WHERE "), "empty-target all-client chunk must have no WHERE: " + chunk);
    verify(pg.genericPs, never()).setString(eq(1), eq(CLIENT_ID));
  }

  @Test
  void missingPrimaryKeyMetadataIssuesNoLockOrUpdate() throws SQLException {
    // A target table without a single IsKey='Y' column yields incomplete metadata: recomputeOne must
    // return false and issue ONLY the metadata query — never a lock or update on an unresolved pk.
    Capture c = newConnection(RAW_TABLE, RAW_COLUMN, RAW_FN, null);
    StoredColumnRecomputer pg = new StoredColumnRecomputer(false);

    assertFalse(pg.recomputeOne(c.con, COLUMN_ID, TARGET_ID));

    assertEquals(1, c.sql.size(), "only the metadata query should have been prepared: " + c.sql);
    assertTrue(c.sql.get(0).contains("computation_function"));
    for (String sql : c.sql) {
      assertFalse(sql.contains("FOR UPDATE"), "no lock must be issued for incomplete metadata");
      assertFalse(sql.startsWith("UPDATE "), "no update must be issued for incomplete metadata");
    }
  }
}
