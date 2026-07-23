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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessContext;

/**
 * Unit tests for the client-scope selection of {@link StoredColumnQueueProcessor} (EPL-1807): the drain
 * partitions by client, with a System ({@code AD_CLIENT_ID='0'}) catch-all that drains every client's
 * partition — mirroring the System branch of {@link StoredColumnRebuild}.
 *
 * <p>The processor is driven with a mocked {@link ProcessBundle}/{@link ConnectionProvider}/
 * {@link Connection} that records every {@code prepareStatement} SQL, so the tests can prove the exact
 * fetch shape and the parameter bindings without a database. Three behaviours are pinned:</p>
 * <ul>
 *   <li>a per-client run appends {@code AND ad_client_id = ?} and binds the running client;</li>
 *   <li>a System run omits the client filter and binds no client on the fetch (drains all clients);</li>
 *   <li>a sentinel row's rebuild is scoped to the <b>row's own</b> {@code AD_CLIENT_ID}, not the
 *       running client — the correctness fix that lets a System drain repair every client.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoredColumnQueueProcessorClientScopeTest {

  private static final String SYSTEM_CLIENT = "0";
  private static final String CLIENT_A = "CLIENT-A";
  private static final String CLIENT_B = "CLIENT-B";

  private static final String COLUMN_ID = "COL-1807";
  private static final String DIRTY_ID = "DIRTY-1";

  // RAW metadata for the sentinel-rebuild path (recompute engine reads these once).
  private static final String RAW_TABLE = "M_Product";
  private static final String RAW_COLUMN = "EM_ETGO_Stock";
  private static final String RAW_FN = "ETGO_Product_Stock";
  private static final String RAW_PK = "M_Product_ID";

  /** Records the mocked collaborators and every SQL passed to {@code prepareStatement}. */
  private static final class Harness {
    private final ProcessBundle bundle;
    private final Connection con;
    private final PreparedStatement fetchPs;
    private final PreparedStatement genericPs;
    private final List<String> sql;

    private Harness(ProcessBundle bundle, Connection con, PreparedStatement fetchPs,
        PreparedStatement genericPs, List<String> sql) {
      this.bundle = bundle;
      this.con = con;
      this.fetchPs = fetchPs;
      this.genericPs = genericPs;
      this.sql = sql;
    }

    /** The single prepared SQL that fetches the dirty queue (identified by {@code refresh_mode}). */
    private String fetchSql() {
      String found = null;
      for (String s : sql) {
        if (s.contains("refresh_mode")) {
          assertTrue(found == null, "expected exactly one fetch SQL, got >1: " + sql);
          found = s;
        }
      }
      assertTrue(found != null, "no fetch SQL was prepared: " + sql);
      return found;
    }

    /** The single keyset-pagination chunk SELECT issued by a sentinel rebuild. */
    private String chunkSql() {
      String found = null;
      for (String s : sql) {
        if (s.contains("ORDER BY") && s.contains(RAW_PK.toLowerCase())) {
          assertTrue(found == null, "expected exactly one chunk SELECT, got >1: " + sql);
          found = s;
        }
      }
      assertTrue(found != null, "no rebuild chunk SELECT was prepared: " + sql);
      return found;
    }
  }

  /**
   * Builds a processor harness running as {@code runningClient}. When {@code sentinelOwnerClient} is
   * non-null the fetch returns a single sentinel dirty row (null target) owned by that client;
   * otherwise the queue is empty. PostgreSQL dialect (non-Oracle).
   */
  private Harness newHarness(String runningClient, String sentinelOwnerClient) throws Exception {
    Connection con = mock(Connection.class);

    // Fetch statement: returns the dirty batch (one sentinel row, or empty).
    ResultSet fetchRs = mock(ResultSet.class);
    if (sentinelOwnerClient != null) {
      when(fetchRs.next()).thenReturn(true, false);
      when(fetchRs.getString(1)).thenReturn(DIRTY_ID);
      when(fetchRs.getString(2)).thenReturn(COLUMN_ID);
      when(fetchRs.getString(3)).thenReturn(null); // sentinel: null target -> full rebuild
      when(fetchRs.getString(4)).thenReturn(sentinelOwnerClient);
    } else {
      when(fetchRs.next()).thenReturn(false);
    }
    PreparedStatement fetchPs = mock(PreparedStatement.class);
    when(fetchPs.executeQuery()).thenReturn(fetchRs);

    // Metadata statement for the recompute engine (only reached by the sentinel path).
    ResultSet metaRs = mock(ResultSet.class);
    when(metaRs.next()).thenReturn(true, false);
    when(metaRs.getString(1)).thenReturn(RAW_TABLE);
    when(metaRs.getString(2)).thenReturn(RAW_COLUMN);
    when(metaRs.getString(3)).thenReturn(RAW_FN);
    when(metaRs.getString(4)).thenReturn(RAW_PK);
    PreparedStatement metaPs = mock(PreparedStatement.class);
    when(metaPs.executeQuery()).thenReturn(metaRs);

    // Everything else (set_config guard, chunk SELECT, DELETE) is a generic no-op with empty results.
    ResultSet emptyRs = mock(ResultSet.class);
    when(emptyRs.next()).thenReturn(false);
    PreparedStatement genericPs = mock(PreparedStatement.class);
    when(genericPs.executeQuery()).thenReturn(emptyRs);
    when(genericPs.execute()).thenReturn(true);
    when(genericPs.executeUpdate()).thenReturn(1);

    List<String> capturedSql = new ArrayList<>();
    when(con.prepareStatement(anyString())).thenAnswer(inv -> {
      String s = inv.getArgument(0);
      capturedSql.add(s);
      if (s.contains("refresh_mode")) {
        return fetchPs; // the queue fetch
      }
      if (s.contains("computation_function")) {
        return metaPs; // recompute-engine metadata lookup
      }
      return genericPs;
    });

    ConnectionProvider conn = mock(ConnectionProvider.class);
    when(conn.getRDBMS()).thenReturn("POSTGRE");
    when(conn.getTransactionConnection()).thenReturn(con);

    ProcessContext ctx = mock(ProcessContext.class);
    when(ctx.getClient()).thenReturn(runningClient);

    ProcessBundle bundle = mock(ProcessBundle.class);
    when(bundle.getLogger()).thenReturn(null);
    when(bundle.getParams()).thenReturn(null);
    when(bundle.getConnection()).thenReturn(conn);
    when(bundle.getContext()).thenReturn(ctx);

    return new Harness(bundle, con, fetchPs, genericPs, capturedSql);
  }

  @Test
  void perClientRunAppendsClientFilterAndBindsRunningClient() throws Exception {
    Harness h = newHarness(CLIENT_A, null);

    new StoredColumnQueueProcessor().execute(h.bundle);

    String fetch = h.fetchSql();
    assertTrue(fetch.contains("AND ad_client_id = ?"),
        "per-client fetch must filter by client: " + fetch);
    assertEquals(
        "SELECT ad_storedcolumn_dirty_id, ad_column_id, target_record_id, ad_client_id"
            + " FROM ad_storedcolumn_dirty WHERE refresh_mode = 'Q' AND is_ignored = 'N'"
            + " AND ad_client_id = ? ORDER BY computation_sequence_number ASC, created ASC",
        fetch);
    verify(h.fetchPs).setString(1, CLIENT_A);
  }

  @Test
  void systemRunOmitsClientFilterAndBindsNoClientOnFetch() throws Exception {
    Harness h = newHarness(SYSTEM_CLIENT, null);

    new StoredColumnQueueProcessor().execute(h.bundle);

    String fetch = h.fetchSql();
    assertFalse(fetch.contains("ad_client_id = ?"),
        "System fetch must not filter by client (drains all clients): " + fetch);
    assertEquals(
        "SELECT ad_storedcolumn_dirty_id, ad_column_id, target_record_id, ad_client_id"
            + " FROM ad_storedcolumn_dirty WHERE refresh_mode = 'Q' AND is_ignored = 'N'"
            + " ORDER BY computation_sequence_number ASC, created ASC",
        fetch);
    // No client id is ever bound on the System fetch statement.
    verify(h.fetchPs, never()).setString(anyInt(), anyString());
  }

  @Test
  void systemRunRebuildsSentinelScopedToRowsOwnClientNotRunningClient() throws Exception {
    // System ('0') drain picks up a sentinel owned by CLIENT-B: its rebuild must scope to CLIENT-B, so
    // the keyset chunk SELECT filters by CLIENT-B — never the running System client.
    Harness h = newHarness(SYSTEM_CLIENT, CLIENT_B);

    new StoredColumnQueueProcessor().execute(h.bundle);

    String chunk = h.chunkSql();
    assertTrue(chunk.contains("ad_client_id"),
        "sentinel rebuild must scope the chunk SELECT by client: " + chunk);
    verify(h.genericPs).setString(1, CLIENT_B);
    verify(h.genericPs, never()).setString(1, SYSTEM_CLIENT);
  }
}
