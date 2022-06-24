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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.report.JmxReportCache;
import org.openbravo.client.application.report.ReportingUtils;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

/**
 * Test to ensure the correct behavior of the compiled reports cache.
 */
public class CompiledReportsCacheTest extends WeldBaseTest {

  @Rule
  public ParameterCdiTestRule<Boolean> reportRule = new ParameterCdiTestRule<Boolean>(
      Arrays.asList(Boolean.TRUE, Boolean.FALSE));

  private @ParameterCdiTest Boolean isCacheEnabled;

  private JmxReportCache jmxReportCache;

  @Override
  protected boolean shouldMockServletContext() {
    return true;
  }

  @Test
  public void jrxmlCompilationShouldNotBeRetrievedFromCache() throws JRException {
    getJmxReportCache().clearCache();
    getJmxReportCache().setEnabled(isCacheEnabled);
    String reportPath = getReportPath();
    // compile the report for the first time
    JasperReport firstCompilation = ReportingUtils.compileReport(reportPath);
    // launch the compilation again: report should be retrieved from cache if it is enabled
    JasperReport secondCompilation = ReportingUtils.compileReport(reportPath);
    assertThat("Compilation for report " + reportPath + " retrieved from cache",
        expectedCompilationResult(firstCompilation, secondCompilation), equalTo(true));
  }

  private boolean expectedCompilationResult(JasperReport first, JasperReport second) {
    if (getJmxReportCache().isEnabled()) {
      return first == second;
    } else {
      return first != second;
    }
  }

  private JmxReportCache getJmxReportCache() {
    if (jmxReportCache == null) {
      jmxReportCache = WeldUtils.getInstanceFromStaticBeanManager(JmxReportCache.class);
    }
    return jmxReportCache;
  }

  private String getReportPath() {
    Path basePath = Paths.get(
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("source.path"),
        "src/org/openbravo/erpReports/C_OrderJR_new.jrxml");
    return basePath.toString();
  }
}
