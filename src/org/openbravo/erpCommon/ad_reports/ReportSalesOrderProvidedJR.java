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
 * All portions are Copyright (C) 2001-2017 Openbravo SLU 
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
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportSalesOrderProvidedJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT")) {
      String strdateFrom = vars.getStringParameter("inpDateFrom", "");
      String strdateTo = vars.getStringParameter("inpDateTo", "");
      String strcBpartnerId = vars.getStringParameter("inpcBPartnerId", "");
      String strmWarehouseId = vars.getStringParameter("inpmWarehouseId", "");
      String strcProjectId = vars.getStringParameter("inpcProjectId", "");
      String strmCategoryId = vars.getStringParameter("inpProductCategory", "");
      String strProjectkind = vars.getStringParameter("inpProjectkind", "");
      String strcRegionId = vars.getStringParameter("inpcRegionId", "");
      String strProjectpublic = vars.getStringParameter("inpProjectpublic", "");
      String strProduct = vars.getStringParameter("inpProductId", "");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportSalesOrderProvidedJR|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strdateFrom, strdateTo, strcBpartnerId, strmWarehouseId,
          strcProjectId, strmCategoryId, strProjectkind, strcRegionId, strProjectpublic, strProduct,
          strCurrencyId);
    } else if (vars.commandIn("FIND")) {
      String strdateFrom = vars.getStringParameter("inpDateFrom");
      String strdateTo = vars.getStringParameter("inpDateTo");
      String strcBpartnerId = vars.getStringParameter("inpcBPartnerId");
      String strmWarehouseId = vars.getStringParameter("inpmWarehouseId");
      String strcProjectId = vars.getStringParameter("inpcProjectId");
      String strmCategoryId = vars.getStringParameter("inpProductCategory");
      String strProjectkind = vars.getStringParameter("inpProjectkind");
      String strcRegionId = vars.getStringParameter("inpcRegionId");
      String strProjectpublic = vars.getStringParameter("inpProjectpublic");
      String strProduct = vars.getStringParameter("inpmProductId");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportSalesOrderProvidedJR|currency", strUserCurrencyId);
      printPageDataSheetJasper(request, response, vars, strdateFrom, strdateTo, strcBpartnerId,
          strmWarehouseId, strcProjectId, strmCategoryId, strProjectkind, strcRegionId,
          strProjectpublic, strProduct, strCurrencyId);
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strdateFrom, String strdateTo, String strcBpartnerId, String strmWarehouseId,
      String strcProjectId, String strmCategoryId, String strProjectkind, String strcRegionId,
      String strProjectpublic, String strProduct, String strCurrencyId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    String discard[] = { "sectionPartner" };
    String strTitle = "";
    XmlDocument xmlDocument = null;
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    if (vars.commandIn("DEFAULT")) {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportSalesOrderProvidedJR")
          .createXmlDocument();

      ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportSalesOrderProvidedJR",
          false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
      toolbar.prepareSimpleToolBarTemplate();
      xmlDocument.setParameter("toolbar", toolbar.toString());

      try {
        WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
            "org.openbravo.erpCommon.ad_reports.ReportSalesOrderProvidedJR");
        xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
        xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
        xmlDocument.setParameter("childTabContainer", tabs.childTabs());
        xmlDocument.setParameter("theme", vars.getTheme());
        NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
            "ReportSalesOrderProvidedJR.html", classInfo.id, classInfo.type, strReplaceWith,
            tabs.breadcrumb());
        xmlDocument.setParameter("navigationBar", nav.toString());
        LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
            "ReportSalesOrderProvidedJR.html", strReplaceWith);
        xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
      } catch (Exception ex) {
        throw new ServletException(ex);
      }
      {
        OBError myMessage = vars.getMessage("ReportSalesOrderProvidedJR");
        vars.removeMessage("ReportSalesOrderProvidedJR");
        if (myMessage != null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
      xmlDocument.setParameter("dateFrom", strdateFrom);
      xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateTo", strdateTo);
      xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
      xmlDocument.setParameter("paramBPartnerId", strcBpartnerId);
      xmlDocument.setParameter("mWarehouseId", strmWarehouseId);
      xmlDocument.setParameter("cProjectId", strcProjectId);
      xmlDocument.setParameter("mProductCategoryId", strmCategoryId);
      xmlDocument.setParameter("cProjectKind", strProjectkind);
      xmlDocument.setParameter("cRegionId", strcRegionId);
      xmlDocument.setParameter("cProjectPublic", strProjectpublic);
      xmlDocument.setParameter("projectName",
          ReportSalesOrderProvidedData.selectProject(readOnlyCP, strcProjectId));
      xmlDocument.setParameter("paramBPartnerDescription",
          ReportSalesOrderProvidedData.bPartnerDescription(readOnlyCP, strcBpartnerId));
      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "M_Warehouse_ID", "", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strmWarehouseId);
        xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "M_Product_Category_ID", "", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strmCategoryId);
        xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
            comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST",
            "C_Projectkind_ID", "Projectkind", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strProjectkind);
        xmlDocument.setData("reportC_PROJECTKIND", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "C_Region_ID", "", "C_Region of Country",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strcRegionId);
        xmlDocument.setData("reportC_REGIONID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST",
            "C_Project_Public_ID", "PublicPrivate", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strProjectpublic);
        xmlDocument.setData("reportC_PROJECTPUBLIC", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      xmlDocument.setParameter("ccurrencyid", strCurrencyId);
      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "C_Currency_ID", "", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
                "ReportSalesOrderProvidedJR"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvidedJR"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
            "ReportSalesOrderProvidedJR", strCurrencyId);
        xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

    } else { // command != DEFAULT
      ReportSalesOrderProvidedData[] data = ReportSalesOrderProvidedData.select(readOnlyCP,
          strCurrencyId,
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvided"),
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportSalesOrderProvided"),
          strdateFrom, DateTimeData.nDaysAfter(readOnlyCP, strdateTo, "1"), strcBpartnerId,
          strmWarehouseId, strcProjectId, strmCategoryId, strProjectkind, strcRegionId,
          strProjectpublic, strProduct);
      if (data == null || data.length == 0) {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportSalesOrderProvidedPop",
                discard)
            .createXmlDocument();
        xmlDocument.setData("structure1", ReportSalesOrderProvidedData.set());
      } else {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportSalesOrderProvidedPop")
            .createXmlDocument();
        xmlDocument.setData("structure1", data);
      }
      if (StringUtils.isNotEmpty(strmWarehouseId)) {
        strTitle += " " + Utility.messageBD(readOnlyCP, "ForWarehouse", vars.getLanguage()) + " "
            + ReportSalesOrderProvidedData.selectWarehouse(readOnlyCP, strmWarehouseId);
      }
      if (StringUtils.isNotEmpty(strcRegionId)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "InRegion", vars.getLanguage()) + " "
            + ReportSalesOrderProvidedData.selectRegionId(readOnlyCP, strcRegionId);
      }
      if (StringUtils.isNotEmpty(strmCategoryId)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "ForProductCategory", vars.getLanguage())
            + " " + ReportSalesOrderProvidedData.selectCategoryId(readOnlyCP, strmCategoryId);
      }
      if (StringUtils.isNotEmpty(strProjectkind)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "ProjectType", vars.getLanguage()) + " "
            + ReportSalesOrderProvidedData.selectProjectkind(readOnlyCP, vars.getLanguage(),
                strProjectkind);
      }
      if (StringUtils.isNotEmpty(strProjectpublic)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "WithInitiativeType", vars.getLanguage())
            + " " + ReportSalesOrderProvidedData.selectProjectpublic(readOnlyCP, vars.getLanguage(),
                strProjectpublic);
      }
      if (StringUtils.isNotEmpty(strdateFrom)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " "
            + strdateFrom;
      }
      if (StringUtils.isNotEmpty(strdateTo)) {
        strTitle += " " + Utility.messageBD(readOnlyCP, "To", vars.getLanguage()) + " " + strdateTo;
      }
      if (StringUtils.isNotEmpty(strProduct)) {
        strTitle += ", " + Utility.messageBD(readOnlyCP, "ForProduct", vars.getLanguage()) + " "
            + ReportSalesOrderProvidedData.selectProduct(readOnlyCP, strProduct);
      }
      xmlDocument.setParameter("title", strTitle);
    }
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  // Jasper calling starts here
  private void printPageDataSheetJasper(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strdateFrom, String strdateTo, String strcBpartnerId,
      String strmWarehouseId, String strcProjectId, String strmCategoryId, String strProjectkind,
      String strcRegionId, String strProjectpublic, String strProduct, String strCurrencyId)
      throws IOException, ServletException {

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportSalesOrderProvidedData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = new OBError();
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      data = ReportSalesOrderProvidedData.select(readOnlyCP, strCurrencyId,
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesOrderProvided"),
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportSalesOrderProvided"),
          strdateFrom, DateTimeData.nDaysAfter(readOnlyCP, strdateTo, "1"), strcBpartnerId,
          strmWarehouseId, strcProjectId, strmCategoryId, strProjectkind, strcRegionId,
          strProjectpublic, strProduct);
    } catch (ServletException ex) {
      myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
    }
    strConvRateErrorMsg = myMessage.getMessage();
    // If a conversion rate is missing for a certain transaction, an error
    // message window pops-up.
    if (StringUtils.isNotEmpty(strConvRateErrorMsg)) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()),
          strConvRateErrorMsg);
    } else { // Launch the report as usual, calling the JRXML file
      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportSalesOrderProvidedJR.jrxml";
      String strOutput = "html";

      String strSubTitle = "";
      strSubTitle = Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " " + strdateFrom
          + " " + Utility.messageBD(readOnlyCP, "To", vars.getLanguage()) + " " + strdateTo;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strSubTitle);
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportSalesOrderProvidedJR. This Servlet was made by Jon Alegría";
  } // end of getServletInfo() method
}
