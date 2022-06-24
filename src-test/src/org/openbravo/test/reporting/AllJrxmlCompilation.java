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

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;

import net.sf.jasperreports.engine.JRException;

/**
 * Compiles all jrxml templates present in the sources directory ensuring they can be compiled with
 * the current combination of jdk + ejc.
 * 
 * @author alostale
 *
 */
public class AllJrxmlCompilation extends WeldBaseTest {

  private static final List<Path> REPORTS = parameters();

  @Rule
  public ParameterCdiTestRule<Path> reportRule = new ParameterCdiTestRule<Path>(REPORTS);

  private @ParameterCdiTest Path report;

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @Test
  public void jrxmlShouldCompile() throws JRException {
    ReportingUtils.compileReport(report.toString());
  }

  private static List<Path> parameters() {
    final List<Path> allJasperFiles = new ArrayList<>();
    try {
      allJasperFiles.addAll(getJrxmlTemplates("src"));
      allJasperFiles.addAll(getJrxmlTemplates("modules"));
    } catch (IOException ioex) {

    }
    return allJasperFiles;
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
