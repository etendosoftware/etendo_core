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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openbravo.modulescript.StoredComputedValidator.Cycle;
import org.openbravo.modulescript.StoredComputedValidator.Edge;
import org.openbravo.modulescript.StoredComputedValidator.Severity;
import org.openbravo.modulescript.StoredComputedValidator.Violation;

/**
 * Pure-logic unit tests for {@link StoredComputedValidator} — no database, no Mockito. These cover
 * the four DB-free surfaces of the validator (EPL-1807, Phase 5b):
 * <ul>
 *   <li>the three-color DFS cycle detector {@link StoredComputedValidator#findCycles(Map)} (rule
 *       <b>V14</b>: every cycle is hard, unconditionally);</li>
 *   <li>the per-edge refresh-ordering predicate
 *       {@link StoredComputedValidator#findSequenceOrderViolations(Map, Map, List)} (rule
 *       <b>V17</b>);</li>
 *   <li>the coarse type-family mappers {@code familyForReference} / {@code familyForPgType} that
 *       drive the function-correctness rules (<b>V5</b>–<b>V6</b>);</li>
 *   <li>the toggle + aggregation surface {@code finishOrThrow} / {@code formatReport} /
 *       {@code isEnforce} ({@code ETGO_SCD_VALIDATION} rollout switch).</li>
 * </ul>
 *
 * <p>Every method name states the rule under test and the expected outcome. The tests live in the
 * validator's own package so the package-private members ({@code familyForReference},
 * {@code familyForPgType}, {@code formatReport}, {@code isEnforce}, {@code TOGGLE}) are reachable
 * without widening any production visibility.</p>
 */
public class StoredComputedValidatorPureTest {

  // Saved value of the rollout toggle system property, restored after each test so the toggle
  // tests never leak state into the rest of the suite.
  private String savedToggle;

  @BeforeEach
  void saveToggle() {
    savedToggle = System.getProperty(StoredComputedValidator.TOGGLE);
  }

  @AfterEach
  void restoreToggle() {
    if (savedToggle == null) {
      System.clearProperty(StoredComputedValidator.TOGGLE);
    } else {
      System.setProperty(StoredComputedValidator.TOGGLE, savedToggle);
    }
  }

  // ================================================================================================
  // V14 — findCycles (pure three-color DFS). Every cycle is hard, unconditionally: a cycle is
  // exactly a dirty set with no topological order, so no sequence assignment can rescue it.
  // ================================================================================================

  @Test
  void v14AcyclicChainReportsNoCycle() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("c"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertTrue(cycles.isEmpty(), "a simple acyclic chain a->b->c must yield no cycle");
  }

  @Test
  void v14DagWithSharedNodeIsNotReportedAsCycle() {
    // Diamond: a->b, a->c, b->d, c->d. 'd' is reached by two paths but there is NO back-edge, so it
    // must not be misreported as a cycle (guards against a shared-node false positive).
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b", "c"));
    adjacency.put("b", Arrays.asList("d"));
    adjacency.put("c", Arrays.asList("d"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertTrue(cycles.isEmpty(), "a diamond DAG with a shared sink is not a cycle");
  }

  @Test
  void v14PlainTwoNodeCycleIsReported() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("a"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertEquals(1, cycles.size(), "a<->b is a single distinct cycle");
  }

  @Test
  void v14SelfLoopIsReportedAsCycle() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("a"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertEquals(1, cycles.size(), "a self-loop a->a is a cycle");
    assertEquals(Collections.singletonList("a"), cycles.get(0).path);
  }

  @Test
  void v14MultiNodeCycleIsReportedOnce() {
    // a->b->c->a, plus a dangling edge c->d that is not part of the cycle.
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("c"));
    adjacency.put("c", Arrays.asList("a", "d"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertEquals(1, cycles.size(), "the single 3-node cycle must be reported exactly once");
    assertEquals(3, cycles.get(0).path.size(), "the cycle path is a,b,c (closing node not repeated)");
  }

  @Test
  void v14AscendingSequenceNumbersDoNotRescueACycle() {
    // Replaces the former "seq-ordered cycles are only WARN" behavior. Sequence numbers ascending
    // along the forward edges (a=1,b=2,c=3) change nothing: a cycle has no topological order at all,
    // so the drain cannot refresh it correctly whatever the numbers say. The detector reports the
    // cycle and the caller classifies it ERROR unconditionally.
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("c"));
    adjacency.put("c", Arrays.asList("a"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertEquals(1, cycles.size(),
        "an ascending-sequence cycle is still a cycle: severity does not depend on the seq numbers");
  }

  // ================================================================================================
  // V17 — findSequenceOrderViolations (per-edge refresh ordering, WARN).
  // Edge a->b means b's function READS a's stored value, so the drain (ORDER BY
  // computation_sequence_number) must visit a first: the edge needs seq[a] < seq[b].
  // ================================================================================================

  @Test
  void v17WellOrderedChainHasNoViolation() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 1L, "b", 2L), Collections.<Cycle> emptyList());
    assertTrue(bad.isEmpty(), "a(1) -> b(2): the drain refreshes a before b, which is correct");
  }

  @Test
  void v17EqualSequenceNumbersAreAViolation() {
    // Equality gives NO ordering guarantee: both drains break ties arbitrarily with respect to the
    // dependency (target_record_id for the 'S' engine, created for the 'Q' engine).
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 0L, "b", 0L), Collections.<Cycle> emptyList());
    assertEquals(1, bad.size(), "a(0) -> b(0): a tie orders nothing, so the edge is unsafe");
    assertEquals("a", bad.get(0).from);
    assertEquals("b", bad.get(0).to);
  }

  @Test
  void v17DecreasingSequenceNumbersAreAViolation() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 5L, "b", 2L), Collections.<Cycle> emptyList());
    assertEquals(1, bad.size(), "a(5) -> b(2): the drain visits b first and reads a stale a");
  }

  @Test
  void v17MissingSequenceNumberIsAViolation() {
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    Map<String, Long> s = new HashMap<>();
    s.put("a", 1L); // 'b' intentionally absent -> null seq
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency, s,
        Collections.<Cycle> emptyList());
    assertEquals(1, bad.size(),
        "a null Computation_Sequence_Number leaves the edge's ordering undefined -> unsafe");
  }

  @Test
  void v17FlagsOnlyTheMisorderedEdgeOfAPath() {
    // a(1) -> b(2) -> c(1). This is a PATH, not a cycle: c reads b but carries a LOWER sequence
    // number, so the drain visits c before b and c reads a stale b. Only the b->c edge is unsafe;
    // a->b is correctly ordered and must stay silent.
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("c"));
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 1L, "b", 2L, "c", 1L), Collections.<Cycle> emptyList());
    assertEquals(1, bad.size(), "only the b->c edge is misordered");
    assertAll(() -> assertEquals("b", bad.get(0).from), () -> assertEquals("c", bad.get(0).to));
  }

  @Test
  void v17IsSuppressedForEdgesInsideAReportedCycle() {
    // a <-> b is a cycle, already reported HARD by V14. Its edges necessarily fail the per-edge
    // ordering test in at least one direction, but the cycle is the real finding — per-edge noise on
    // top of it is unhelpful, so both edges are suppressed.
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("a"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    assertEquals(1, cycles.size(), "precondition: V14 reports the cycle");
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 1L, "b", 2L), cycles);
    assertTrue(bad.isEmpty(), "edges of an already-reported cycle must not also raise V17");
  }

  @Test
  void v17StillFlagsEdgesOutsideAReportedCycle() {
    // a <-> b cycle (suppressed), plus a separate misordered edge b -> c that is NOT in the cycle
    // and must still be reported.
    Map<String, List<String>> adjacency = new HashMap<>();
    adjacency.put("a", Arrays.asList("b"));
    adjacency.put("b", Arrays.asList("a", "c"));
    List<Cycle> cycles = StoredComputedValidator.findCycles(adjacency);
    List<Edge> bad = StoredComputedValidator.findSequenceOrderViolations(adjacency,
        seq("a", 1L, "b", 2L, "c", 2L), cycles);
    assertEquals(1, bad.size(), "the non-cycle edge b->c is still checked");
    assertAll(() -> assertEquals("b", bad.get(0).from), () -> assertEquals("c", bad.get(0).to));
  }

  // ================================================================================================
  // V5/V6 — familyForReference / familyForPgType coarse type families.
  // ================================================================================================

  @Test
  void familyForReferenceMapsNumericReferences() {
    assertAll("numeric AD references",
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("12")),  // Amount
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("22")),  // Number
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("11")),  // Integer
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("29")),  // Quantity
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("800008")),
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForReference("800019")));
  }

  @Test
  void familyForReferenceMapsStringAndDateReferences() {
    assertAll("string / date AD references",
        () -> assertEquals("STRING", StoredComputedValidator.familyForReference("10")), // String
        () -> assertEquals("STRING", StoredComputedValidator.familyForReference("14")), // Text
        () -> assertEquals("STRING", StoredComputedValidator.familyForReference("20")), // YesNo
        () -> assertEquals("DATE", StoredComputedValidator.familyForReference("15")),   // Date
        () -> assertEquals("DATE", StoredComputedValidator.familyForReference("16")));  // DateTime
  }

  @Test
  void familyForReferenceReturnsNullForUnknownOrNull() {
    assertAll("unmapped AD references are skipped (warn-only, false-positive averse)",
        () -> assertNull(StoredComputedValidator.familyForReference("999")),
        () -> assertNull(StoredComputedValidator.familyForReference("30")), // Search / FK
        () -> assertNull(StoredComputedValidator.familyForReference(null)));
  }

  @Test
  void familyForPgTypeMapsEachFamily() {
    assertAll("pg_type.typname families",
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("numeric")),
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("int4")),
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("int8")),
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("float8")),
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("money")),
        () -> assertEquals("STRING", StoredComputedValidator.familyForPgType("varchar")),
        () -> assertEquals("STRING", StoredComputedValidator.familyForPgType("bpchar")),
        () -> assertEquals("STRING", StoredComputedValidator.familyForPgType("text")),
        () -> assertEquals("STRING", StoredComputedValidator.familyForPgType("name")),
        () -> assertEquals("DATE", StoredComputedValidator.familyForPgType("date")),
        () -> assertEquals("DATE", StoredComputedValidator.familyForPgType("timestamptz")),
        () -> assertEquals("DATE", StoredComputedValidator.familyForPgType("timetz")));
  }

  @Test
  void familyForPgTypeIsCaseInsensitive() {
    assertAll("typname is lower-cased before mapping",
        () -> assertEquals("NUMERIC", StoredComputedValidator.familyForPgType("NUMERIC")),
        () -> assertEquals("STRING", StoredComputedValidator.familyForPgType("VarChar")),
        () -> assertEquals("DATE", StoredComputedValidator.familyForPgType("TIMESTAMP")));
  }

  @Test
  void familyForPgTypeReturnsNullForUnknownOrNull() {
    assertAll("unmapped pg types are skipped",
        () -> assertNull(StoredComputedValidator.familyForPgType("bytea")),
        () -> assertNull(StoredComputedValidator.familyForPgType("uuid")),
        () -> assertNull(StoredComputedValidator.familyForPgType(null)));
  }

  @Test
  void v6FamilyMismatchIsDetectableAndMatchIsNot() {
    // This is exactly the V6 WARN condition: expected (from AD reference) != actual (from pg return
    // type), both non-null. A numeric Amount column whose function returns text is a mismatch; the
    // same column whose function returns numeric is a clean match (no violation).
    String expectedNumeric = StoredComputedValidator.familyForReference("12"); // Amount -> NUMERIC
    assertEquals("NUMERIC", expectedNumeric);
    assertAll("V6 family comparison",
        () -> assertFalse(expectedNumeric.equals(StoredComputedValidator.familyForPgType("text")),
            "NUMERIC column returning text() is a family mismatch -> WARN"),
        () -> assertTrue(expectedNumeric.equals(StoredComputedValidator.familyForPgType("numeric")),
            "NUMERIC column returning numeric() matches -> no violation"));
  }

  // ================================================================================================
  // Toggle + aggregation — finishOrThrow / formatReport / isEnforce (ETGO_SCD_VALIDATION).
  // ================================================================================================

  @Test
  void enforceModeWithHardViolationThrowsBuildException() {
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    List<Violation> violations = new ArrayList<>();
    violations.add(new Violation(Severity.ERROR, StoredComputedValidator.ETGO_ScdNoDependencies,
        "column t.c — a stored computed column must have at least one active dependency"));
    BuildException ex = assertThrows(BuildException.class,
        () -> StoredComputedValidator.finishOrThrow(violations),
        "enforce mode + a hard violation must stop the build");
    assertTrue(ex.getMessage().contains(StoredComputedValidator.ETGO_ScdNoDependencies),
        "the thrown report must carry the offending code");
    assertTrue(ex.getMessage().contains("validation failed"),
        "the thrown report must be the aggregated Phase 5b report");
  }

  @Test
  void enforceModeWithOnlyWarningsDoesNotThrow() {
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    List<Violation> violations = new ArrayList<>();
    violations.add(new Violation(Severity.WARN, StoredComputedValidator.ETGO_ScdMissingIndex,
        "dependency d — no index leads with FK column"));
    assertDoesNotThrow(() -> StoredComputedValidator.finishOrThrow(violations),
        "warnings alone never stop the build, even in enforce mode");
  }

  @Test
  void warnModeWithHardViolationDoesNotThrow() {
    System.setProperty(StoredComputedValidator.TOGGLE, "warn");
    List<Violation> violations = new ArrayList<>();
    violations.add(new Violation(Severity.ERROR, StoredComputedValidator.ETGO_ScdNoDependencies,
        "column t.c — missing dependency"));
    assertDoesNotThrow(() -> StoredComputedValidator.finishOrThrow(violations),
        "warn mode downgrades every hard violation to a warning and never stops the build");
  }

  @Test
  void emptyViolationListNeverThrows() {
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    assertDoesNotThrow(() -> StoredComputedValidator.finishOrThrow(new ArrayList<>()),
        "no violations means no report and no throw");
  }

  @Test
  void isEnforceIsFalseOnlyForWarnValue() {
    System.setProperty(StoredComputedValidator.TOGGLE, "warn");
    assertFalse(StoredComputedValidator.isEnforce(), "'warn' disables enforcement");
    System.setProperty(StoredComputedValidator.TOGGLE, "WARN");
    assertFalse(StoredComputedValidator.isEnforce(), "the toggle value is compared case-insensitively");
    System.setProperty(StoredComputedValidator.TOGGLE, "enforce");
    assertTrue(StoredComputedValidator.isEnforce(), "'enforce' enables enforcement");
    System.setProperty(StoredComputedValidator.TOGGLE, "anything-else");
    assertTrue(StoredComputedValidator.isEnforce(),
        "any non-'warn' value enforces (enforce is the safe default)");
  }

  @Test
  void isEnforceDefaultsToEnforceWhenUnset() {
    // Only meaningful when the environment variable is also unset; otherwise the env var could set
    // the mode and this would not be a clean default. Skip rather than false-fail in that case.
    assumeTrue(System.getenv(StoredComputedValidator.TOGGLE) == null,
        "ETGO_SCD_VALIDATION is set in the environment — cannot assert the unset default");
    System.clearProperty(StoredComputedValidator.TOGGLE);
    assertTrue(StoredComputedValidator.isEnforce(), "the default (nothing set) is enforce");
  }

  @Test
  void formatReportListsErrorsBeforeWarningsWithCounts() {
    List<Violation> violations = new ArrayList<>();
    violations.add(new Violation(Severity.WARN, StoredComputedValidator.ETGO_ScdFunctionVolatile,
        "warn detail"));
    violations.add(new Violation(Severity.ERROR, StoredComputedValidator.ETGO_ScdFunctionMissing,
        "error detail"));
    String report = StoredComputedValidator.formatReport(violations);

    assertAll("aggregated report shape",
        () -> assertTrue(
            report.startsWith("Stored computed column validation failed (1 error(s), 1 warning(s)):"),
            "header counts errors and warnings separately"),
        () -> assertTrue(
            report.contains("  [ERROR] " + StoredComputedValidator.ETGO_ScdFunctionMissing
                + ": error detail"),
            "error lines are rendered with the [ERROR] prefix, code and detail"),
        () -> assertTrue(
            report.contains("  [WARN ] " + StoredComputedValidator.ETGO_ScdFunctionVolatile
                + ": warn detail"),
            "warn lines are rendered with the [WARN ] prefix, code and detail"),
        () -> assertTrue(
            report.indexOf("[ERROR]") < report.indexOf("[WARN ]"),
            "all errors are listed before any warning, regardless of insertion order"),
        () -> assertTrue(
            report.endsWith("Fix the definitions above and re-run update.database."),
            "the report ends with the actionable footer"));
  }

  // ------------------------------------------------------------------------------------------------
  // helpers
  // ------------------------------------------------------------------------------------------------

  /** Builds a {@code seqByNode} map from alternating (node, seq) pairs. */
  private static Map<String, Long> seq(Object... pairs) {
    Map<String, Long> m = new HashMap<>();
    for (int i = 0; i + 1 < pairs.length; i += 2) {
      m.put((String) pairs[i], (Long) pairs[i + 1]);
    }
    return m;
  }
}
