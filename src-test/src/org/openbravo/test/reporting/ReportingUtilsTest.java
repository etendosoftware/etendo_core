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
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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
    File report = getTmpFile();
    if (report.exists()) {
      report.delete();
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
      ReportingUtils.exportJR(getReportPath().toString(), ExportType.HTML, new HashMap<>(), report,
          true, connectionProvider, null, new HashMap<>());
    } catch (Exception ex) {
      log.error("Could not generate test report", ex);
    }
  }

  private File getTmpFile() {
    return new File(ReportingUtils.getTempFolder(), "tmp.html");
  }

  private Path getReportPath() throws URISyntaxException {
    return Paths.get(getClass().getResource("reports/Main.jrxml").toURI());
  }
}
