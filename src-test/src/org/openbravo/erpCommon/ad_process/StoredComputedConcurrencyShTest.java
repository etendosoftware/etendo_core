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
 * JUnit wrapper for {@code stored_computed_concurrency.sh} — Mode 2 (concurrent contention) stress
 * of the synchronous ({@code 'S'}) engine (EPL-1807, Phase 3b Topic 3). A single psql session cannot
 * open two real connections, so this is a <b>bash</b> harness that spawns {@code SCD_WRITERS} (&gt;= 2)
 * parallel psql writer sessions released together by a PostgreSQL advisory-lock start barrier, then
 * a verifier asserts no lost updates ({@code stored == SUM(linenetamt)} per header),
 * {@code ad_scd_check = 0}, and a fully drained queue. This exercises the engine's cross-transaction
 * {@code FOR UPDATE} target lock under genuine contention — which no single-connection SQL script
 * can reproduce. The harness self-restores; a clean run prints
 * {@code RESULT: ALL CONCURRENCY ASSERTIONS PASSED}.
 *
 * <p>The harness reads the standard {@code PG*} environment variables, which this test populates
 * from {@code Openbravo.properties} (no hardcoded credentials). Scale knobs can be overridden with
 * the system properties {@code scd.shared_headers}, {@code scd.private_headers},
 * {@code scd.lines_per_header}, {@code scd.rounds}, {@code scd.writers}, {@code scd.max_retries}
 * (mapped to the harness's {@code SCD_*} variables). PostgreSQL only; skips when {@code bbdd.rdbms}
 * is not PostgreSQL or {@code bash}/{@code psql} is absent.</p>
 */
public class StoredComputedConcurrencyShTest extends StoredComputedSqlScriptTestBase {

  private static final String SCRIPT = "stored_computed_concurrency.sh";
  private static final String SENTINEL = "RESULT: ALL CONCURRENCY ASSERTIONS PASSED";

  @Test
  public void concurrencyHarnessPasses() throws Exception {
    assumeTrue("Concurrency harness exercises the synchronous engine (PostgreSQL) — skipping on "
        + rdbms(), isPostgres());
    assumeBinaryOnPath("bash");
    assumeBinaryOnPath("psql");

    String bash = System.getProperty("scd.bash.bin", "bash");
    List<String> cmd = List.of(bash, script(SCRIPT).getAbsolutePath());

    Map<String, String> env = new HashMap<>();
    applyPostgresEnv(env);
    // Let the harness pick up the psql binary override too, if one was set for the SQL tests.
    String psql = System.getProperty("scd.psql.bin");
    if (psql != null && !psql.isEmpty()) {
      env.put("PSQL_BIN", psql);
    }
    mapKnob(env, "SCD_SHARED_HEADERS", "scd.shared_headers");
    mapKnob(env, "SCD_PRIVATE_HEADERS", "scd.private_headers");
    mapKnob(env, "SCD_LINES_PER_HEADER", "scd.lines_per_header");
    mapKnob(env, "SCD_ROUNDS", "scd.rounds");
    mapKnob(env, "SCD_WRITERS", "scd.writers");
    mapKnob(env, "SCD_MAX_RETRIES", "scd.max_retries");

    assertScriptPassed(run(cmd, env), SENTINEL);
  }

  private static void mapKnob(Map<String, String> env, String envVar, String sysProp) {
    String value = System.getProperty(sysProp);
    if (value != null && !value.isEmpty()) {
      env.put(envVar, value);
    }
  }
}
