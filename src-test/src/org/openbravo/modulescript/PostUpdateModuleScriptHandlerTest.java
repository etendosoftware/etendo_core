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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.tools.ant.BuildException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openbravo.ddlutils.util.ModulesUtil;

/**
 * Pure-logic unit tests for {@link PostUpdateModuleScriptHandler} (EPL-1810) — no database, no
 * live Ant build. They cover the two package-private surfaces the {@code execute()} entry point
 * is composed of:
 * <ul>
 *   <li>{@code runScript(String, Map)} — the concrete-{@link PostUpdateModuleScript} filter
 *       (abstract classes and unrelated classes found in the scanned folders must be skipped,
 *       never instantiated), and the failure contract: a {@link BuildException} thrown by the
 *       script itself (e.g. a validator carrying its own detailed report) is rethrown untouched,
 *       while any other failure is wrapped into a generic {@code BuildException};</li>
 *   <li>{@code listModuleClassesDirs(File)} — discovery of every module's {@code build/classes}
 *       folder under the {@link ModulesUtil#moduleDirs} roots, in deterministic sorted order,
 *       skipping modules without compiled classes and tolerating missing roots.</li>
 * </ul>
 *
 * <p>The tests live in the handler's own package so the package-private members are reachable
 * without widening any production visibility. Script fixtures are static nested classes,
 * addressed with their {@code Outer$Nested} binary names exactly as the classpath scan would
 * report them.</p>
 */
public class PostUpdateModuleScriptHandlerTest {

  private static final Map<String, OpenbravoVersion> EMPTY_VERSIONS = new HashMap<>();

  private PostUpdateModuleScriptHandler handler;

  @BeforeEach
  void setUp() {
    handler = new PostUpdateModuleScriptHandler();
    RecordingScript.EXECUTIONS.set(0);
  }

  @Test
  void runScriptExecutesAConcreteScript() {
    handler.runScript(RecordingScript.class.getName(), EMPTY_VERSIONS);
    assertEquals(1, RecordingScript.EXECUTIONS.get());
  }

  /**
   * The scanned folder contains the compiled {@code PostUpdateModuleScript} base class itself;
   * instantiating it would fail, so abstract classes must be filtered out.
   */
  @Test
  void runScriptSkipsAbstractClasses() {
    assertDoesNotThrow(
        () -> handler.runScript(PostUpdateModuleScript.class.getName(), EMPTY_VERSIONS));
    assertDoesNotThrow(() -> handler.runScript(AbstractScript.class.getName(), EMPTY_VERSIONS));
    assertEquals(0, RecordingScript.EXECUTIONS.get());
  }

  @Test
  void runScriptSkipsClassesThatAreNotPostUpdateModuleScripts() {
    assertDoesNotThrow(() -> handler.runScript("java.lang.String", EMPTY_VERSIONS));
    // A plain ModuleScript found in the same folders belongs to ModuleScriptHandler, not here
    assertDoesNotThrow(() -> handler.runScript(ModuleScript.class.getName(), EMPTY_VERSIONS));
  }

  /**
   * A {@code BuildException} raised by the script itself (e.g. {@code StoredComputedValidator}'s
   * aggregated report) must reach Ant untouched — wrapping it would replace the detailed report
   * with the generic failure message.
   */
  @Test
  void runScriptRethrowsBuildExceptionsUntouched() {
    BuildException thrown = assertThrows(BuildException.class,
        () -> handler.runScript(ReportingFailingScript.class.getName(), EMPTY_VERSIONS));
    assertEquals("detailed validator report", thrown.getMessage());
  }

  @Test
  void runScriptWrapsAnyOtherFailureIntoABuildException() {
    BuildException thrown = assertThrows(BuildException.class,
        () -> handler.runScript(CrashingScript.class.getName(), EMPTY_VERSIONS));
    assertTrue(thrown.getMessage().contains("Execution of postUpdateModuleScript"));
  }

  @Test
  void runScriptWrapsUnresolvableClassNamesIntoABuildException() {
    assertThrows(BuildException.class,
        () -> handler.runScript("org.openbravo.modulescript.DoesNotExist", EMPTY_VERSIONS));
  }

  @Test
  void listModuleClassesDirsFindsSortedModuleClassesFolders(@TempDir Path projectRoot)
      throws IOException {
    // Build, under every configured modules root, two modules with compiled classes (named so
    // that creation order differs from the expected sorted order), one module without them, and
    // a stray plain file.
    String modulesRoot = ModulesUtil.moduleDirs[0];
    Path root = projectRoot.resolve(modulesRoot);
    Files.createDirectories(root.resolve("zz.last.module/build/classes"));
    Files.createDirectories(root.resolve("aa.first.module/build/classes"));
    Files.createDirectories(root.resolve("no.compiled.classes.module/src"));
    Files.createFile(root.resolve("stray-file.txt"));

    List<File> dirs = handler.listModuleClassesDirs(projectRoot.toFile());

    List<File> underThisRoot = dirs.stream()
        .filter(d -> d.getAbsolutePath().startsWith(root.toFile().getAbsolutePath()))
        .toList();
    assertEquals(2, underThisRoot.size(), "only modules WITH build/classes are listed");
    assertTrue(underThisRoot.get(0).getAbsolutePath().contains("aa.first.module"),
        "modules are visited in sorted order");
    assertTrue(underThisRoot.get(1).getAbsolutePath().contains("zz.last.module"));
    assertTrue(dirs.stream().allMatch(d -> d.getName().equals("classes")),
        "every listed folder is a build/classes folder");
  }

  @Test
  void listModuleClassesDirsToleratesMissingModuleRoots(@TempDir Path emptyRoot) {
    List<File> dirs = handler.listModuleClassesDirs(emptyRoot.toFile());
    assertTrue(dirs.isEmpty());
  }

  @Test
  void listModuleClassesDirsIgnoresAFileWhereAModulesRootIsExpected(
      @TempDir Path projectRoot) throws IOException {
    Files.createFile(projectRoot.resolve(ModulesUtil.moduleDirs[0]));
    assertDoesNotThrow(() -> {
      List<File> dirs = handler.listModuleClassesDirs(projectRoot.toFile());
      assertFalse(dirs.stream().anyMatch(
          d -> d.getAbsolutePath().startsWith(projectRoot.toFile().getAbsolutePath())));
    });
  }

  /** Concrete script fixture; counts executions through a static counter (the handler builds
   * its own instance, so an instance field would be unreachable from the test). */
  public static class RecordingScript extends PostUpdateModuleScript {
    static final AtomicInteger EXECUTIONS = new AtomicInteger();

    @Override
    public void execute() {
      EXECUTIONS.incrementAndGet();
    }
  }

  /** Abstract script fixture: must be skipped by the concrete-class filter. */
  public abstract static class AbstractScript extends PostUpdateModuleScript {
  }

  /** Script fixture that fails with its own, report-carrying {@code BuildException}. */
  public static class ReportingFailingScript extends PostUpdateModuleScript {
    @Override
    public void execute() {
      throw new BuildException("detailed validator report");
    }
  }

  /** Script fixture that fails with a non-Ant exception: must be wrapped. */
  public static class CrashingScript extends PostUpdateModuleScript {
    @Override
    public void execute() {
      throw new IllegalStateException("unexpected crash");
    }
  }
}
