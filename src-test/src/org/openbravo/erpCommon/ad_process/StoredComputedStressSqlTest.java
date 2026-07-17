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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * JUnit wrapper for {@code stored_computed_stress.sql} — Mode 1 (single-transaction batch) stress
 * of the synchronous ({@code 'S'}) engine (EPL-1807, Phase 3b Topic 3). F1 fan-in (many lines →
 * one header → exactly one dedup dirty row → one recompute) and F2 fan-out (bulk update across many
 * headers plus an untouched control header whose value and {@code xmin} stay stable). The script
 * self-heals pre-existing rows via {@code ad_scd_rebuild}, self-asserts, and self-restores; a clean
 * run prints {@code ALL STRESS SCENARIOS PASSED}.
 *
 * <p>Runs with the script's CI-fast defaults. The scale knobs can be overridden with the system
 * properties {@code scd.n_lines}, {@code scd.n_orders}, {@code scd.lines_per_order} (each mapped to
 * the corresponding {@code psql -v} variable). PostgreSQL only; skips when {@code bbdd.rdbms} is not
 * PostgreSQL or {@code psql} is absent.</p>
 */
public class StoredComputedStressSqlTest extends StoredComputedSqlScriptTestBase {

  private static final String SCRIPT = "stored_computed_stress.sql";
  private static final String SENTINEL = "ALL STRESS SCENARIOS PASSED";

  @Test
  public void stressScenariosPass() throws Exception {
    assumeTrue("Stress harness exercises the synchronous engine (PostgreSQL) — skipping on "
        + rdbms(), isPostgres());
    assumeBinaryOnPath("psql");
    assertScriptPassed(runPsql(SCRIPT, scaleArgs()), SENTINEL);
  }

  /** Optional {@code -v key=value} overrides for the three psql scale variables. */
  private static String[] scaleArgs() {
    List<String> args = new ArrayList<>();
    addKnob(args, "n_lines", "scd.n_lines");
    addKnob(args, "n_orders", "scd.n_orders");
    addKnob(args, "lines_per_order", "scd.lines_per_order");
    return args.toArray(new String[0]);
  }

  private static void addKnob(List<String> args, String psqlVar, String sysProp) {
    String value = System.getProperty(sysProp);
    if (value != null && !value.isEmpty()) {
      args.add("-v");
      args.add(psqlVar + "=" + value);
    }
  }
}
