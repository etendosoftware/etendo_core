/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2017-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.reporting;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.test.base.TestConstants;

/**
 * Compiles all jrxml templates present in the sources directory ensuring they can be compiled with
 * the current combination of jdk + ejc.
 *
 * @author alostale
 */
public class AllJrxmlCompilation extends WeldBaseTest {

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("parameters")
  public void jrxmlShouldCompile(Path report) throws Exception {
    // Initialize test context - @BeforeEach doesn't work reliably with @ParameterizedTest + Arquillian
    ensureTestContextInitialized();

    try {
      ReportingUtils.compileReport(report.toString());
    } catch (Throwable e) {
      // Print full stack trace with ALL causes to diagnose the real issue
      System.err.println("\n========================================");
      System.err.println("Failed to compile report: " + report);
      System.err.println("Error class: " + e.getClass().getName());
      System.err.println("Error message: " + e.getMessage());
      System.err.println("----------------------------------------");
      
      // Print all causes with suppressed exceptions
      Throwable cause = e.getCause();
      int depth = 1;
      while (cause != null) {
        System.err.println("Caused by [" + depth + "]: " + cause.getClass().getName());
        System.err.println("  Message: " + cause.getMessage());
        if (cause.getStackTrace() != null && cause.getStackTrace().length > 0) {
          System.err.println("  At: " + cause.getStackTrace()[0]);
        }
        
        // Check for suppressed exceptions
        Throwable[] suppressed = cause.getSuppressed();
        if (suppressed != null && suppressed.length > 0) {
          System.err.println("  Suppressed exceptions:");
          for (Throwable s : suppressed) {
            System.err.println("    - " + s.getClass().getName() + ": " + s.getMessage());
          }
        }
        
        cause = cause.getCause();
        depth++;
      }
      
      // Check for suppressed exceptions in the main exception
      Throwable[] suppressed = e.getSuppressed();
      if (suppressed != null && suppressed.length > 0) {
        System.err.println("Main exception suppressed:");
        for (Throwable s : suppressed) {
          System.err.println("  - " + s.getClass().getName() + ": " + s.getMessage());
          s.printStackTrace(System.err);
        }
      }
      
      System.err.println("----------------------------------------");
      System.err.println("Full stack trace:");
      e.printStackTrace(System.err);
      System.err.println("========================================\n");
      throw e;
    }
  }

  private void ensureTestContextInitialized() throws Exception {
    // Ensure Weld components are initialized
    initializeWeldComponents();

    // Manually call setUp to ensure ServletContext is mocked
    // This is necessary because @BeforeEach doesn't execute reliably with @ParameterizedTest + Arquillian
    setUp();

    // Set up test context for each test execution
    OBContext.setOBContext(TestConstants.Users.SYSTEM, TestConstants.Roles.SYS_ADMIN,
        TestConstants.Clients.SYSTEM, TestConstants.Orgs.MAIN);
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId());
    RequestContext.get().setVariableSecureApp(vars);
  }

  private static Stream<Path> parameters() {
    final List<Path> allJasperFiles = new ArrayList<>();
    try {
      allJasperFiles.addAll(getJrxmlTemplates("src"));
      allJasperFiles.addAll(getJrxmlTemplates("modules"));
    } catch (IOException ioex) {

    }
    return allJasperFiles.stream();
  }

  private static Collection<Path> getJrxmlTemplates(String dir) throws IOException {
    final Collection<Path> allJasperFiles = new ArrayList<>();

    final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:.*\\.jrxml");
    Path basePath = Paths.get(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path"),
        dir);
    Files.walkFileTree(basePath, new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        if (matcher.matches(file)) {
          allJasperFiles.add(file);
        }
        return FileVisitResult.CONTINUE;
      }
    });
    return allJasperFiles;
  }
}
