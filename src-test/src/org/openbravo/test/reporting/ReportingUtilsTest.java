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
 * All portions are Copyright (C) 2022 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.client.application.report.ReportingUtils.ExportType;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.Issue;

/**
 * Test cases covering report generation using {@link ReportingUtils}
 */
public class ReportingUtilsTest extends WeldBaseTest {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @After
  public void cleanUp() {
    for (ExportType exportType : new ExportType[] { ExportType.HTML, ExportType.XLS,
        ExportType.XLSX }) {
      File report = getTmpFile(exportType);
      if (report.exists()) {
        report.delete();
      }
    }
  }

  /**
   * Generates a report that uses {@link Utility#showImageLogo(String, String)} and a subreport that
   * executes a query
   */
  @Test
  @Issue("48465")
  public void generateReport() {
    File report = getTmpFile();
    generateReport(report, null);
    assertThat("report generated correctly", report.exists(), equalTo(true));
  }

  /**
   * Generates a report that uses {@link Utility#showImageLogo(String, String)} and a subreport that
   * executes a query. It provides an specific connection provider to generate the report.
   */
  @Test
  @Issue("48465")
  public void generateReportWithConnectionProvider() {
    File report = getTmpFile();
    generateReport(report, DalConnectionProvider.getReadOnlyConnectionProvider());
    assertThat("report generated correctly", report.exists(), equalTo(true));
  }

  private void generateReport(File report, ConnectionProvider connectionProvider) {
    try {
      exportReport(report, connectionProvider, ExportType.HTML);
    } catch (Exception ex) {
      log.error("Could not generate test report", ex);
    }
  }

  private void exportReport(File report, ConnectionProvider connectionProvider,
      ExportType exportType) throws Exception {
    ReportingUtils.exportJR(getReportPath().toString(), exportType, new HashMap<>(), report, true,
        connectionProvider, null, new HashMap<>());
  }

  private File getTmpFile() {
    return getTmpFile(ExportType.HTML);
  }

  private File getTmpFile(ExportType exportType) {
    return new File(ReportingUtils.getTempFolder(), "tmp." + exportType.getExtension());
  }

  /**
   * Exports a report to the legacy Excel format, which relies on the Apache POI HSSF
   * implementation. It ensures that the JasperReports and POI libraries shipped with the platform
   * are compatible and that all their runtime dependencies are available.
   * 
   * @throws Exception
   *           if the report cannot be exported or the generated file cannot be read
   */
  @Test
  @Issue("#1146")
  public void generateLegacyExcelReport() throws Exception {
    File report = getTmpFile(ExportType.XLS);
    exportReport(report, null, ExportType.XLS);
    assertThat("xls report generated correctly", report.exists(), equalTo(true));
    try (HSSFWorkbook workbook = new HSSFWorkbook(new FileInputStream(report))) {
      assertThat("xls report contains a sheet", workbook.getNumberOfSheets() > 0, equalTo(true));
    }
  }

  /**
   * Exports a report to the OOXML Excel format, which is the default one.
   * 
   * @throws Exception
   *           if the report cannot be exported
   */
  @Test
  @Issue("#1146")
  public void generateExcelReport() throws Exception {
    File report = getTmpFile(ExportType.XLSX);
    exportReport(report, null, ExportType.XLSX);
    assertThat("xlsx report generated correctly", report.exists(), equalTo(true));
  }

  private Path getReportPath() throws URISyntaxException {
    return Paths.get(System.getProperty("user.dir"), "src-test/resources/reports/Main.jrxml").normalize();
  }
}
