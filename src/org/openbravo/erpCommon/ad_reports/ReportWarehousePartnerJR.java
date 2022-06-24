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
 * All portions are Copyright (C) 2007-2017 Openbravo SLU 
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

import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportWarehousePartnerJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
      String strDate = vars.getGlobalVariable("inpDateFrom", "ReportWarehousePartnerJR|Date",
          DateTimeData.today(readOnlyCP));
      String strProductCategory = vars.getGlobalVariable("inpProductCategory",
          "ReportWarehousePartnerJR|productCategory", "");
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportWarehousePartnerJR|mProductId", "", IsIDFilter.instance);
      String strmLocatorId = vars.getInGlobalVariable("inpmLocatorId_IN",
          "ReportWarehousePartnerJR|mLocatorId", "", IsIDFilter.instance);
      String strX = vars.getGlobalVariable("inpX", "ReportWarehousePartnerJR|X", "");
      String strY = vars.getGlobalVariable("inpY", "ReportWarehousePartnerJR|Y", "");
      String strZ = vars.getGlobalVariable("inpZ", "ReportWarehousePartnerJR|Z", "");
      printPageDataSheet(response, vars, strDate, strProductCategory, strmProductId, strmLocatorId,
          strX, strY, strZ);
    } else if (vars.commandIn("PRINT_HTML")) {
      String strDate = vars.getGlobalVariable("inpDateFrom", "ReportWarehousePartner|Date");
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportWarehousePartnerJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportWarehousePartnerJR|mProductId", IsIDFilter.instance);
      String strmLocatorId = vars.getRequestInGlobalVariable("inpmLocatorId_IN",
          "ReportWarehousePartnerJR|mLocatorId", IsIDFilter.instance);
      String strX = vars.getRequestGlobalVariable("inpX", "ReportWarehousePartnerJR|X");
      String strY = vars.getRequestGlobalVariable("inpY", "ReportWarehousePartnerJR|Y");
      String strZ = vars.getRequestGlobalVariable("inpZ", "ReportWarehousePartnerJR|Z");
      setHistoryCommand(request, "FIND");
      printPageDataHtml(response, vars, strDate, strProductCategory, strmProductId, strmLocatorId,
          strX, strY, strZ, "html");
    } else if (vars.commandIn("PRINT_PDF")) {
      String strDate = vars.getGlobalVariable("inpDateFrom", "ReportWarehousePartner|Date");
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportWarehousePartnerJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportWarehousePartnerJR|mProductId", IsIDFilter.instance);
      String strmLocatorId = vars.getRequestInGlobalVariable("inpmLocatorId_IN",
          "ReportWarehousePartnerJR|mLocatorId", IsIDFilter.instance);
      String strX = vars.getRequestGlobalVariable("inpX", "ReportWarehousePartnerJR|X");
      String strY = vars.getRequestGlobalVariable("inpY", "ReportWarehousePartnerJR|Y");
      String strZ = vars.getRequestGlobalVariable("inpZ", "ReportWarehousePartnerJR|Z");
      setHistoryCommand(request, "FIND");
      printPageDataHtml(response, vars, strDate, strProductCategory, strmProductId, strmLocatorId,
          strX, strY, strZ, "pdf");
    } else if (vars.commandIn("PRINT_XLS")) {
      String strDate = vars.getGlobalVariable("inpDateFrom", "ReportWarehousePartner|Date");
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportWarehousePartnerJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportWarehousePartnerJR|mProductId", IsIDFilter.instance);
      String strmLocatorId = vars.getRequestInGlobalVariable("inpmLocatorId_IN",
          "ReportWarehousePartnerJR|mLocatorId", IsIDFilter.instance);
      String strX = vars.getRequestGlobalVariable("inpX", "ReportWarehousePartnerJR|X");
      String strY = vars.getRequestGlobalVariable("inpY", "ReportWarehousePartnerJR|Y");
      String strZ = vars.getRequestGlobalVariable("inpZ", "ReportWarehousePartnerJR|Z");
      setHistoryCommand(request, "FIND");
      printPageDataHtml(response, vars, strDate, strProductCategory, strmProductId, strmLocatorId,
          strX, strY, strZ, "xls");
    } else {
      pageError(response);
    }
  }

  private void printPageDataHtml(HttpServletResponse response, VariablesSecureApp vars,
      String strDate, String strProductCategory, String strmProductId, String strmLocatorId,
      String strX, String strY, String strZ, String strOutput)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ReportWarehousePartnerData[] data = ReportWarehousePartnerData.select(readOnlyCP,
        vars.getLanguage(),
        Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportWarehouseControl"),
        Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportWarehouseControl"),
        DateTimeData.nDaysAfter(readOnlyCP, strDate, "1"), strmProductId, strmLocatorId,
        strProductCategory, strX, strY, strZ);

    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportWarehousePartnerJR.jrxml";

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    // parameters.put("Subtitle",strSubtitle);
    renderJR(vars, response, strReportName, strOutput, parameters, data, null);

  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDate, String strProductCategory, String strmProductId, String strmLocatorId,
      String strX, String strY, String strZ) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportWarehousePartnerJR")
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportWarehousePartnerJR", false,
        "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportWarehousePartnerJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportWarehousePartnerJR.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportWarehousePartnerJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportWarehousePartnerJR");
      vars.removeMessage("ReportWarehousePartnerJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("date", strDate);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("parameterX", strX);
    xmlDocument.setParameter("parameterY", strY);
    xmlDocument.setParameter("parameterZ", strZ);
    xmlDocument.setParameter("mProductCategoryId", strProductCategory);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_Product_Category_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportPricelist"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportPricelist"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportPricelist",
          strProductCategory);
      xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setData("reportMProductId_IN", "liststructure",
        SelectorUtilityData.selectMproduct(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strmProductId));
    xmlDocument.setData("reportMLocatorId_IN", "liststructure",
        SelectorUtilityData.selectMlocator(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strmLocatorId));

    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportWarehousePartner. This Servlet was made by Jon Alegria";
  } // end of getServletInfo() method
}
