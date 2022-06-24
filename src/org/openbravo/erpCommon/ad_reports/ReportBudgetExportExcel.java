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

package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.UpdateActuals;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.financialmgmt.accounting.Budget;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportBudgetExportExcel extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strKey = vars.getRequiredGlobalVariable("inpcBudgetId",
          "ReportBudgetGenerateExcel|inpcBudgetId");
      printPageDataExportExcel(response, vars, strKey);
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataExportExcel(HttpServletResponse response, VariablesSecureApp vars,
      String strBudgetId) throws IOException, ServletException {

    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: EXCEL");
    }

    vars.removeSessionValue("ReportBudgetGenerateExcel|inpTabId");

    response.setContentType("application/xls; charset=UTF-8");
    PrintWriter out = response.getWriter();

    XmlDocument xmlDocument = null;
    ReportBudgetGenerateExcelData[] data = null;
    Budget budget = OBDal.getInstance().get(Budget.class, strBudgetId);
    boolean exportActualData = budget.isExportActualData();
    if (exportActualData) {
      try {
        ConnectionProvider conn = new DalConnectionProvider(false);
        ProcessBundle pb = new ProcessBundle("ABDFC8131D964936AD2EF7E0CED97FD9", vars).init(conn);
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("C_Budget_ID", strBudgetId);
        pb.setParams(parameters);
        OBError myMessage = null;
        new UpdateActuals().execute(pb);
        myMessage = (OBError) pb.getResult();
        if (myMessage != null && StringUtils.equals("Error", myMessage.getType())) {
          log4j.error(myMessage.getMessage());
        }
      } catch (Exception e) {
        log4j.error("Error in printPageDataExportExcel of ReportBudgetExportExcel", e);
      }
    }
    data = ReportBudgetGenerateExcelData.selectLines(this, vars.getLanguage(), strBudgetId);

    if (data.length != 0 && StringUtils.equals(data[0].exportactual, "Y")) {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportBudgetGenerateExcelXLS")
          .createXmlDocument();
    } else {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportBudgetGenerateExcelExportXLS")
          .createXmlDocument();
    }

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");

    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();

  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportBudgetGenerateExcel.";
  } // end of getServletInfo() method
}
