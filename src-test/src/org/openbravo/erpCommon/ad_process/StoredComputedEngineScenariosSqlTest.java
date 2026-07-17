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

import static org.junit.Assume.assumeTrue;

import org.junit.Test;

/**
 * JUnit wrapper for {@code stored_computed_engine_scenarios.sql} — the synchronous ({@code 'S'})
 * deferred-recalculation engine (EPL-1807). Scenarios S0–S6: insert/update/delete child line
 * refreshes the stored value, non-watched update does not enqueue, bulk update drains once, the
 * value guard suppresses no-op writes ({@code xmin} stable), and the re-entry guard blocks
 * recursion. The script self-asserts and self-restores; a clean run prints
 * {@code ALL SCENARIOS PASSED}.
 *
 * <p>PostgreSQL only (synchronous mode does not exist on Oracle). Skips when {@code bbdd.rdbms} is
 * not PostgreSQL or {@code psql} is not on the PATH. Requires the engine + pilot trigger deployed
 * via {@code ./gradlew update.database} and at least one draft sales order with &gt;= 2 lines.</p>
 */
public class StoredComputedEngineScenariosSqlTest extends StoredComputedSqlScriptTestBase {

  private static final String SCRIPT = "stored_computed_engine_scenarios.sql";
  private static final String SENTINEL = "ALL SCENARIOS PASSED";

  @Test
  public void engineSyncScenariosPass() throws Exception {
    assumeTrue("Synchronous engine scenarios require PostgreSQL — skipping on " + rdbms(),
        isPostgres());
    assumeBinaryOnPath("psql");
    assertScriptPassed(runPsql(SCRIPT), SENTINEL);
  }
}
