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
 * JUnit wrapper for {@code stored_computed_queue_scenarios.sql} — the asynchronous ({@code 'Q'})
 * queue-drain contract of the stored computed engine (EPL-1807), mirroring the exact SQL
 * {@code StoredColumnQueueProcessor} issues. Scenarios Q1–Q7: accumulate without sync, batch drain,
 * sentinel rebuild + sibling, CLAIM ordering, {@code ad_scd_check} staleness, poison /
 * dead-letter / reset, and single seq-ordered drain across columns. The script self-asserts and
 * self-restores; a clean run prints {@code ALL QUEUE SCENARIOS PASSED}.
 *
 * <p>Runs on the local PostgreSQL DB (it is the PG parity partner of the Oracle script). Skips when
 * {@code bbdd.rdbms} is not PostgreSQL or {@code psql} is not on the PATH. Requires the engine +
 * pilot trigger deployed via {@code ./gradlew update.database} and a draft sales order with
 * &gt;= 2 lines.</p>
 */
public class StoredComputedQueueScenariosSqlTest extends StoredComputedSqlScriptTestBase {

  private static final String SCRIPT = "stored_computed_queue_scenarios.sql";
  private static final String SENTINEL = "ALL QUEUE SCENARIOS PASSED";

  @Test
  public void queueAsyncScenariosPass() throws Exception {
    assumeTrue("Queue scenarios run against PostgreSQL — skipping on " + rdbms(), isPostgres());
    assumeBinaryOnPath("psql");
    assertScriptPassed(runPsql(SCRIPT), SENTINEL);
  }
}
