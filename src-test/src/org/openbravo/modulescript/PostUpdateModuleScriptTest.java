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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Pure-logic unit tests for the {@link PostUpdateModuleScript} base type (EPL-1810) — no
 * database, no Ant. They pin down the contract that distinguishes the new script type from a
 * plain {@link ModuleScript}:
 * <ul>
 *   <li>the <b>design invariant</b> that {@code PostUpdateModuleScript} is NOT assignable to
 *       {@code ModuleScript} — otherwise {@code ModuleScriptHandler} would also pick these
 *       scripts up and run them mid-update, which is exactly what the type exists to avoid;</li>
 *   <li>the execution-limit semantics inherited from
 *       {@code org.openbravo.base.ExecutionLimitBaseProcess}, exercised through
 *       {@link PostUpdateModuleScript#getPostUpdateModuleScriptExecutionLimits()} (the
 *       exhaustive boundary matrix for the shared base lives in
 *       {@code org.openbravo.test.modularity.ExecutionLimitsTest}; here we verify the new
 *       type is wired into it correctly);</li>
 *   <li>the empty-version-map fallback ({@code executeOnInstall()} semantics) that
 *       {@link PostUpdateModuleScriptHandler} relies on when {@code AD_MODULE} cannot be
 *       read;</li>
 *   <li>{@code handleError(Throwable)} wrapping into {@code IllegalStateException} with the
 *       original cause preserved.</li>
 * </ul>
 *
 * <p>The tests live in the scripts' own package so protected members are reachable through the
 * fixture subclass without widening any production visibility.</p>
 */
public class PostUpdateModuleScriptTest {

  private static final String MODULE_ID = "0";
  /** The version the dependent module is at when {@code preExecute} runs. */
  private static final String CURRENT_VERSION = "3.0.10000";
  /** A version below {@link #CURRENT_VERSION}, used as a limit boundary. */
  private static final String OLDER_VERSION = "3.0.9000";

  /** Non-empty module-versions map: core at a fixed, known version. */
  private static Map<String, OpenbravoVersion> versionMap(String coreVersion) {
    Map<String, OpenbravoVersion> map = new HashMap<>();
    map.put(MODULE_ID, new OpenbravoVersion(coreVersion));
    return map;
  }

  @Test
  void isNotAModuleScriptSoItNeverRunsMidUpdate() {
    assertFalse(ModuleScript.class.isAssignableFrom(PostUpdateModuleScript.class),
        "PostUpdateModuleScript must NOT extend ModuleScript: ModuleScriptHandler executes "
            + "anything assignable to ModuleScript, so extending it would also run these "
            + "scripts mid-update — the exact skew EPL-1810 removes");
  }

  @Test
  void typeNameIdentifiesTheNewScriptType() {
    assertEquals("PostUpdateModuleScript", new FakeScript().typeName());
  }

  @Test
  void executionLimitsDefaultToNull() {
    assertNull(new FakeScript().getPostUpdateModuleScriptExecutionLimits());
  }

  @Test
  void executesWhenNoLimitsAreDefined() {
    FakeScript script = new FakeScript();
    script.preExecute(versionMap(CURRENT_VERSION));
    assertTrue(script.wasExecuted);
  }

  @Test
  void executesWhenCurrentVersionIsInsideTheLimits() {
    FakeScript script = new FakeScript();
    script.limits = new ModuleScriptExecutionLimits(MODULE_ID, new OpenbravoVersion(OLDER_VERSION),
        new OpenbravoVersion("3.0.11000"));
    script.preExecute(versionMap(CURRENT_VERSION));
    assertTrue(script.wasExecuted);
  }

  @Test
  void skipsWhenCurrentVersionIsAboveTheLastLimit() {
    FakeScript script = new FakeScript();
    script.limits = new ModuleScriptExecutionLimits(MODULE_ID, new OpenbravoVersion("3.0.8000"),
        new OpenbravoVersion(OLDER_VERSION));
    script.preExecute(versionMap(CURRENT_VERSION));
    assertFalse(script.wasExecuted);
  }

  @Test
  void skipsWhenCurrentVersionIsBelowTheFirstLimit() {
    FakeScript script = new FakeScript();
    script.limits = new ModuleScriptExecutionLimits(MODULE_ID, new OpenbravoVersion("3.0.11000"),
        new OpenbravoVersion("3.0.12000"));
    script.preExecute(versionMap(CURRENT_VERSION));
    assertFalse(script.wasExecuted);
  }

  @Test
  void skipsWhenLimitsAreIncorrectlyDefined() {
    FakeScript script = new FakeScript();
    script.limits = new ModuleScriptExecutionLimits(MODULE_ID, new OpenbravoVersion("3.0.12000"),
        new OpenbravoVersion(OLDER_VERSION));
    script.preExecute(versionMap(CURRENT_VERSION));
    assertFalse(script.wasExecuted);
  }

  /**
   * When the dependent module is missing from the versions map (module being installed),
   * {@code executeOnInstall()} decides.
   */
  @Test
  void moduleNotInstalledYetHonorsExecuteOnInstall() {
    FakeScript executed = new FakeScript();
    executed.limits = new ModuleScriptExecutionLimits("NOT-IN-MAP", null, null);
    executed.onInstall = true;
    executed.preExecute(versionMap(CURRENT_VERSION));
    assertTrue(executed.wasExecuted);

    FakeScript skipped = new FakeScript();
    skipped.limits = new ModuleScriptExecutionLimits("NOT-IN-MAP", null, null);
    skipped.onInstall = false;
    skipped.preExecute(versionMap(CURRENT_VERSION));
    assertFalse(skipped.wasExecuted);
  }

  /**
   * {@link PostUpdateModuleScriptHandler} passes an EMPTY map when the module versions cannot be
   * recovered from {@code AD_MODULE}; the base process must then fall back to
   * {@code executeOnInstall()} semantics.
   */
  @Test
  void emptyVersionMapFallsBackToExecuteOnInstall() {
    FakeScript executed = new FakeScript();
    executed.onInstall = true;
    executed.preExecute(new HashMap<>());
    assertTrue(executed.wasExecuted);

    FakeScript skipped = new FakeScript();
    skipped.onInstall = false;
    skipped.preExecute(new HashMap<>());
    assertFalse(skipped.wasExecuted);
  }

  @Test
  void handleErrorWrapsIntoIllegalStateExceptionPreservingTheCause() {
    FakeScript script = new FakeScript();
    RuntimeException cause = new RuntimeException("boom");
    IllegalStateException thrown = assertThrows(IllegalStateException.class,
        () -> script.handleError(cause));
    assertSame(cause, thrown.getCause());
  }

  /** Fake script with configurable limits/onInstall; flags when it is executed. */
  private static class FakeScript extends PostUpdateModuleScript {
    boolean wasExecuted = false;
    ModuleScriptExecutionLimits limits = null;
    boolean onInstall = true;

    @Override
    public void execute() {
      wasExecuted = true;
    }

    @Override
    protected ModuleScriptExecutionLimits getPostUpdateModuleScriptExecutionLimits() {
      return limits;
    }

    @Override
    protected boolean executeOnInstall() {
      return onInstall;
    }

    /** Exposes the protected type name to the test. */
    String typeName() {
      return getTypeName();
    }
  }
}
