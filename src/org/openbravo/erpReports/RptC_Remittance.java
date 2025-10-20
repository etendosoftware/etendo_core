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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpReports;

import java.io.IOException;
import java.util.HashMap;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.erpCommon.utility.Utility;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

public class RptC_Remittance extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = createVars(request);

    if (vars.commandIn("DEFAULT")) {
      String strcRemittanceId = vars.getSessionValue("RptC_Remittance.inpcRemittanceId_R");
      if (strcRemittanceId.equals("")) {
        strcRemittanceId = vars.getSessionValue("RptC_Remittance.inpcRemittanceId");
      }
      printPagePDF(response, vars, strcRemittanceId, vars.getLanguage());
    } else {
      pageError(response);
    }
  }

  protected void printPagePDF(HttpServletResponse response, VariablesSecureApp vars,
      String strcRemittanceId, String language) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: pdf");
    }

    String strBaseDesign = getBaseDesignPath(language);

    String strOutput = new String("pdf");

    String strReportName = "@basedesign@/org/openbravo/erpReports/RptC_Remittance.jrxml";

    if (strOutput.equals("pdf")) {
      response.setHeader("Content-disposition", "inline; filename=RptC_Remittance.pdf");
    }

    RptCRemittanceData[] data = RptCRemittanceData.select(this,
        Utility.getContext(this, vars, "#User_Client", "RptC_RemittanceJR"),
        Utility.getContext(this, vars, "#AccessibleOrgTree", "RptC_RemittanceJR"),
        strcRemittanceId);

    JasperReport jasperReportLines;
    try {
      jasperReportLines = ReportingUtils.getTranslatedJasperReport(this,
          strBaseDesign + "/org/openbravo/erpReports/RptC_Remittance_Lines.jrxml",
          vars.getLanguage());
    } catch (JRException e) {
      e.printStackTrace();
      throw new ServletException(e.getMessage());
    }

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("LANGUAGE", language);
    parameters.put("SR_LINES", jasperReportLines);

    renderJR(vars, response, strReportName, strOutput, parameters, data, null);
  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the RptCOrders seeker";
  } // End of getServletInfo() method

  /**
   * Creates a {@link VariablesSecureApp} from the given {@link HttpServletRequest}.
   *
   * @param request the HTTP request
   * @return the initialized secure variables
   */
  protected VariablesSecureApp createVars(HttpServletRequest request) {
    return new VariablesSecureApp(request);
  }
}
