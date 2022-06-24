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
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportMaterialTransactionEditionJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strdateFrom = vars.getStringParameter("inpDateFrom", "");
      String strdateTo = vars.getStringParameter("inpDateTo", "");
      printPageDataSheet(response, vars, strdateFrom, strdateTo);
    } else if (vars.commandIn("EDIT_HTML")) {
      String strdateFrom = vars.getStringParameter("inpDateFrom");
      String strdateTo = vars.getStringParameter("inpDateTo");
      String strcBpartnetId = vars.getStringParameter("inpcBPartnerId");
      String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
      String strcProjectId = vars.getStringParameter("inpcProjectId");
      printPageHtml(response, vars, strdateFrom, strdateTo, strcBpartnetId, strmWarehouseId,
          strcProjectId, "html", request);
    } else if (vars.commandIn("EDIT_PDF")) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("WE EDIT THE PDF");
      }
      String strdateFrom = vars.getStringParameter("inpDateFrom");
      String strdateTo = vars.getStringParameter("inpDateTo");
      String strcBpartnetId = vars.getStringParameter("inpcBPartnerId");
      String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
      String strcProjectId = vars.getStringParameter("inpcProjectId");
      printPageHtml(response, vars, strdateFrom, strdateTo, strcBpartnetId, strmWarehouseId,
          strcProjectId, "pdf", request);
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strdateFrom, String strdateTo) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportMaterialTransactionEditionJR")
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(),
        "ReportMaterialTransactionEditionJR", false, "", "", "", false, "ad_reports",
        strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportMaterialTransactionEditionJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportMaterialTransactionEditionJR.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportMaterialTransactionEditionJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportMaterialTransactionEditionJR");
      vars.removeMessage("ReportMaterialTransactionEditionJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strdateFrom);
    xmlDocument.setParameter("dateTo", strdateTo);
    String strDateFormat = vars.getSessionValue("#AD_SqlDateFormat");
    xmlDocument.setParameter("dateFromdisplayFormat", strDateFormat);
    xmlDocument.setParameter("dateFromsaveFormat", strDateFormat);
    xmlDocument.setParameter("dateTodisplayFormat", strDateFormat);
    xmlDocument.setParameter("dateTosaveFormat", strDateFormat);
    xmlDocument.setParameter("paramBPartnerId", "");
    xmlDocument.setParameter("mWarehouseId", "");
    xmlDocument.setParameter("cProjectId", "");
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_Warehouse_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportMaterialTransactionEditionJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportMaterialTransactionEditionJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportMaterialTransactionEditionJR", "");
      xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageHtml(HttpServletResponse response, VariablesSecureApp vars,
      String strdateFrom, String strdateTo, String strcBpartnetId, String strmWarehouseId,
      String strcProjectId, String strOutput, HttpServletRequest request)
      throws IOException, ServletException {

    InoutEditionData[] data = null;
    int limit = Integer.parseInt(Utility.getPreference(vars, "ReportsLimit", ""));
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();

    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }

    String pgLimit = null, oraLimit = null;
    if (StringUtils.equalsIgnoreCase(readOnlyCP.getRDBMS(), "ORACLE")) {
      oraLimit = String.valueOf(limit + 1);
    } else {
      pgLimit = String.valueOf(limit + 1);
    }
    String discard[] = { "discard" };
    data = InoutEditionData.select(readOnlyCP, vars.getLanguage(),
        Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
            "ReportMaterialTransactionEditionJR"),
        Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportMaterialTransactionEditionJR"),
        strdateFrom, strdateTo, strcBpartnetId, strmWarehouseId, strcProjectId, pgLimit, oraLimit);

    if (data == null || data.length == 0) {
      discard[0] = "selEliminar";
    }

    if (limit > 0 && data.length > limit) {
      String msgbody = Utility.messageBD(readOnlyCP, "ReportsLimit", vars.getLanguage());
      msgbody = msgbody.replace("@limit@", String.valueOf(limit));
      advisePopUp(request, response, "WARNING",
          Utility.messageBD(this, "ReportsLimitHeader", vars.getLanguage()), msgbody);
      return;
    }
    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportMaterialTransactionEditionJR.jrxml";

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    renderJR(vars, response, strReportName, strOutput, parameters, data, null);

  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportMaterialTransaction.";
  }
}
