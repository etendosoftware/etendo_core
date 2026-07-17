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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * JUnit wrapper for {@code stored_computed_oracle_scenarios.sql} — the Oracle ({@code sqlplus})
 * counterpart of the PostgreSQL async-queue script, proving PG ↔ Oracle parity for stored computed
 * columns (EPL-1807, Phase 5). Scenarios O0–O7: on Oracle every column is queued ({@code 'S'} is
 * silently forced to {@code 'Q'}, no sync constraint trigger, no engine PL/SQL), the Oracle-dialect
 * enqueue trigger dedups via {@code MERGE}, the C2 raw two-statement Java recompute is issued
 * directly, and the C3 recursion guard reuses {@code AD_Disable_Triggers}. The script self-asserts
 * ({@code WHENEVER SQLERROR EXIT ROLLBACK}) and self-restores; a clean run ends with
 * {@code ALL ORACLE SCENARIOS PASSED}.
 *
 * <p><b>Skipped unless the active database is Oracle</b> and {@code sqlplus} is on the PATH — on a
 * PostgreSQL dev DB this test reports "assumption failed" and does not run (the local environment in
 * this repo is PostgreSQL only). Requires the module deployed on an Oracle instance via
 * {@code ./gradlew update.database} and a draft sales order with &gt;= 2 lines.</p>
 */
public class StoredComputedOracleScenariosSqlTest extends StoredComputedSqlScriptTestBase {

  private static final String SCRIPT = "stored_computed_oracle_scenarios.sql";
  private static final String SENTINEL = "ALL ORACLE SCENARIOS PASSED";

  @Test
  public void oracleParityScenariosPass() throws Exception {
    assumeTrue("Oracle scenarios require bbdd.rdbms=ORACLE — skipping on " + rdbms(), isOracle());
    assumeBinaryOnPath("sqlplus");

    String sqlplus = System.getProperty("scd.sqlplus.bin", "sqlplus");
    // sqlplus @<script> under a connect string built from Openbravo.properties (no hardcoding).
    // "-L" fails fast instead of re-prompting on a bad login; "-S" silences the banner.
    List<String> cmd = List.of(sqlplus, "-S", "-L", oracleConnectString(),
        "@" + script(SCRIPT).getAbsolutePath());
    Map<String, String> env = new HashMap<>();
    assertScriptPassed(run(cmd, env), SENTINEL);
  }
}
