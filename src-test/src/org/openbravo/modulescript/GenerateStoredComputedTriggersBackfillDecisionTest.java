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

import org.junit.jupiter.api.Test;

/**
 * Pure-logic truth-table tests for the first-activation backfill dispatch of
 * {@link GenerateStoredComputedTriggers#populateNewColumns} (EPL-1807).
 *
 * <p>The production method computes its {@code rowCount} lazily (only on the {@code 'S'} /
 * non-Oracle branch) and emits a distinct log line per branch, so it is deliberately <b>not</b>
 * refactored into a shared decision method: extracting it would either add a {@code SELECT count(*)}
 * for the {@code 'Q'} / {@code 'M'} columns (a real behavior change) or lose the per-case logging.
 * Instead, {@link #decideBackfillAction} below is a <b>hand-copied mirror</b> of that dispatch,
 * pinned here as a pure function so the branch decision — which the SQL simulation harness
 * ({@code stored_computed_install_scenarios.sql}) never exercises — is covered as a unit.</p>
 *
 * <p>This test is fully self-contained: it does not import the production class, so it can run
 * without any of {@code GenerateStoredComputedTriggers}'s DB / ModuleScript machinery. The mirror
 * and the production dispatch MUST be kept identical — see the reciprocal sync note above
 * {@code populateNewColumns} in {@link GenerateStoredComputedTriggers}.</p>
 */
public class GenerateStoredComputedTriggersBackfillDecisionTest {

  /**
   * Row-count ceiling for an inline synchronous initial population of a {@code REFRESH_MODE='S'}
   * column. Mirrors {@code GenerateStoredComputedTriggers.LARGE_TABLE_THRESHOLD} (100k).
   */
  private static final long LARGE_TABLE_THRESHOLD = 100_000L;

  /** The three possible outcomes of the first-activation dispatch. */
  enum BackfillAction {
    /** Run {@code ad_scd_rebuild} synchronously inline during {@code update.database}. */
    INLINE,
    /** Enqueue per-client null-sentinel dirty rows for the async queue processor. */
    SENTINEL,
    /** Do nothing at install (manual, already-deployed, or unknown mode). */
    SKIP
  }

  // ================================================================================================
  // MIRROR — hand-copied from GenerateStoredComputedTriggers.populateNewColumns.
  // This is a verbatim transcription of that method's branch decision, with the side effects
  // (insertSentinel / inline DO block / logging) replaced by the BackfillAction they correspond to
  // and the lazy countRows(...) result passed in as `rowCount`. Any change to the production
  // dispatch MUST be reflected here, and vice versa. Do NOT "clean up" or reorder the branches:
  // they are kept in the exact order and shape of the original so the two stay trivially diffable.
  // ================================================================================================
  static BackfillAction decideBackfillAction(boolean hadPreexistingDep, String refreshMode,
      boolean oracle, long rowCount) {
    if (hadPreexistingDep) {
      return BackfillAction.SKIP; // already deployed in a prior run — leave existing values untouched
    }
    String mode = refreshMode;
    if ("M".equals(mode)) {
      return BackfillAction.SKIP; // manual rebuild required (run StoredColumnRebuild)
    }
    if ("S".equals(mode)) {
      if (oracle) {
        // Oracle deploys no PL/pgSQL engine (no ad_scd_rebuild) and silently downgrades 'S' to the
        // async path: enqueue a sentinel and let StoredColumnQueueProcessor recompute in Java.
        return BackfillAction.SENTINEL;
      }
      if (rowCount <= LARGE_TABLE_THRESHOLD) {
        // Inline synchronous rebuild — single DO block so the LOCAL guard survives the call.
        return BackfillAction.INLINE;
      } else {
        return BackfillAction.SENTINEL;
      }
    } else if ("Q".equals(mode)) {
      return BackfillAction.SENTINEL;
    } else {
      return BackfillAction.SKIP; // unknown REFRESH_MODE — skip initial population
    }
  }

  // ================================================================================================
  // hadPreexistingDep — a routine re-run never re-aggregates already-correct data (SKIP wins first).
  // ================================================================================================

  @Test
  void preexistingDependencySkipsRegardlessOfMode() {
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(true, "Q", false, 1L),
        "the preexisting-dependency guard short-circuits and SKIPs before the mode ('Q') is ever considered");
  }

  @Test
  void preexistingDependencyOverridesAnOtherwiseInlineCase() {
    // 'S' + non-Oracle + tiny table would normally be INLINE; the preexisting-dep guard runs first
    // and must still SKIP so a re-run never re-aggregates already-correct values.
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(true, "S", false, 42L),
        "the preexisting-dependency guard overrides an otherwise-INLINE (S/!oracle/small) case");
  }

  // ================================================================================================
  // REFRESH_MODE='M' — manual: do nothing at install.
  // ================================================================================================

  @Test
  void manualModeSkips() {
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(false, "M", false, 1L),
        "REFRESH_MODE='M' defers population entirely to the operator (StoredColumnRebuild)");
  }

  // ================================================================================================
  // REFRESH_MODE='S' — sync, with the Oracle downgrade and the large-table fallback.
  // ================================================================================================

  @Test
  void syncModeOnOracleFallsBackToSentinel() {
    assertEquals(BackfillAction.SENTINEL,
        decideBackfillAction(false, "S", true, 1L),
        "Oracle has no ad_scd_rebuild, so 'S' silently downgrades to the async sentinel path");
  }

  @Test
  void syncModeOnPostgresWithSmallTableRunsInline() {
    assertEquals(BackfillAction.INLINE,
        decideBackfillAction(false, "S", false, 500L),
        "'S' on Postgres under the threshold rebuilds inline during update.database");
  }

  @Test
  void syncModeOnPostgresWithLargeTableFallsBackToSentinel() {
    assertEquals(BackfillAction.SENTINEL,
        decideBackfillAction(false, "S", false, 250_000L),
        "'S' on Postgres over the threshold defers to the async queue to avoid stalling the build");
  }

  // ================================================================================================
  // REFRESH_MODE='Q' — queued: always enqueue a sentinel.
  // ================================================================================================

  @Test
  void queuedModeEnqueuesSentinel() {
    assertEquals(BackfillAction.SENTINEL,
        decideBackfillAction(false, "Q", false, 1L),
        "REFRESH_MODE='Q' always enqueues a null-sentinel for the async processor");
  }

  // ================================================================================================
  // Unknown / null / empty mode — skip with a warning (represented here as SKIP).
  // ================================================================================================

  @Test
  void unknownModeSkips() {
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(false, "X", false, 1L),
        "an unrecognized REFRESH_MODE is skipped rather than guessed");
  }

  @Test
  void nullModeSkips() {
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(false, null, false, 1L),
        "a null REFRESH_MODE matches none of M/S/Q and is skipped");
  }

  @Test
  void emptyModeSkips() {
    assertEquals(BackfillAction.SKIP,
        decideBackfillAction(false, "", false, 1L),
        "an empty REFRESH_MODE matches none of M/S/Q and is skipped");
  }

  // ================================================================================================
  // Boundary — the LARGE_TABLE_THRESHOLD (100000) is inclusive of INLINE (<=), exclusive of SENTINEL.
  // ================================================================================================

  @Test
  void rowCountExactlyAtThresholdRunsInline() {
    assertEquals(BackfillAction.INLINE,
        decideBackfillAction(false, "S", false, 100_000L),
        "rowCount == 100000 is <= threshold, so the boundary row still rebuilds inline");
  }

  @Test
  void rowCountOneOverThresholdFallsBackToSentinel() {
    assertEquals(BackfillAction.SENTINEL,
        decideBackfillAction(false, "S", false, 100_001L),
        "rowCount == 100001 crosses the threshold and defers to the async queue");
  }

  // ================================================================================================
  // countRows failure — the production countRows(...) returns Long.MAX_VALUE on error, which on the
  // 'S' / non-Oracle path must be treated as a huge table and deferred to the queue.
  // ================================================================================================

  @Test
  void countRowsFailureFallbackDefersToSentinel() {
    assertEquals(BackfillAction.SENTINEL,
        decideBackfillAction(false, "S", false, Long.MAX_VALUE),
        "a failed row count (Long.MAX_VALUE) is treated as a large table -> async queue");
  }
}
