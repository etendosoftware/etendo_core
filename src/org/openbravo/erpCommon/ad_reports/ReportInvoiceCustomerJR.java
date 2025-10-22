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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportInvoiceCustomerJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT")) {
      String strdateFrom = vars.getGlobalVariable("inpDateFrom", "ReportInvoiceCustomerJR|DateFrom",
          "");
      String strdateTo = vars.getGlobalVariable("inpDateTo", "ReportInvoiceCustomerJR|DateTo", "");
      String strcProjectId = vars.getGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerJR|cProjectId", "");
      String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerJR|cBPartnerId_IN", "", IsIDFilter.instance);
      String strmCategoryId = vars.getInGlobalVariable("inpmProductCategoryId",
          "ReportInvoiceCustomerJR|mCategoryId", "", IsIDFilter.instance);
      String strProjectkind = vars.getInGlobalVariable("inpProjectkind",
          "ReportInvoiceCustomerJR|Projectkind", "", IsIDFilter.instance);
      String strProjectstatus = vars.getInGlobalVariable("inpProjectstatus",
          "ReportInvoiceCustomerJR|Projectstatus", "", IsIDFilter.instance);
      String strProjectphase = vars.getInGlobalVariable("inpProjectphase",
          "ReportInvoiceCustomerJR|Projectphase", "", IsIDFilter.instance);
      String strProduct = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerJR|mProductId_IN", "", IsIDFilter.instance);
      String strProjectpublic = vars.getGlobalVariable("inpProjectpublic",
          "ReportInvoiceCustomerJR|Projectpublic", "");
      String strSalesRep = vars.getGlobalVariable("inpSalesRepId",
          "ReportInvoiceCustomerJR|SalesRepId", "");
      String strcRegionId = vars.getInGlobalVariable("inpcRegionId",
          "ReportInvoiceCustomerJR|cRegionId", "", IsIDFilter.instance);
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerJR|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strdateFrom, strdateTo, strcProjectId, strcBpartnerId,
          strmCategoryId, strProjectkind, strProjectstatus, strProjectphase, strProduct,
          strProjectpublic, strSalesRep, strcRegionId, strCurrencyId);
    } else if (vars.commandIn("EDIT_HTML", "EDIT_PDF")) {
      String strdateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerJR|DateFrom");
      String strdateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerJR|DateTo");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerJR|cProjectId");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerJR|cBPartnerId_IN", IsIDFilter.instance);
      String strmCategoryId = vars.getRequestInGlobalVariable("inpmProductCategoryId",
          "ReportInvoiceCustomerJR|mCategoryId", IsIDFilter.instance);
      String strProjectkind = vars.getRequestInGlobalVariable("inpProjectkind",
          "ReportInvoiceCustomerJR|Projectkind", IsIDFilter.instance);
      String strProjectstatus = vars.getRequestInGlobalVariable("inpProjectstatus",
          "ReportInvoiceCustomerJR|Projectstatus", IsIDFilter.instance);
      String strProjectphase = vars.getRequestInGlobalVariable("inpProjectphase",
          "ReportInvoiceCustomerJR|Projectphase", IsIDFilter.instance);
      String strProduct = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerJR|mProductId_IN", IsIDFilter.instance);
      String strProjectpublic = vars.getRequestGlobalVariable("inpProjectpublic",
          "ReportInvoiceCustomerJR|Projectpublic");
      String strSalesRep = vars.getRequestGlobalVariable("inpSalesRepId",
          "ReportInvoiceCustomerJR|SalesRepId");
      String strcRegionId = vars.getRequestInGlobalVariable("inpcRegionId",
          "ReportInvoiceCustomerJR|cRegionId", IsIDFilter.instance);
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerJR|currency", strUserCurrencyId);
      printPageHtml(request, response, vars, strdateFrom, strdateTo, strcProjectId, strcBpartnerId,
          strmCategoryId, strProjectkind, strProjectstatus, strProjectphase, strProduct,
          strProjectpublic, strSalesRep, strcRegionId, strCurrencyId);
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strdateFrom, String strdateTo, String strcProjectId, String strcBpartnerId,
      String strmCategoryId, String strProjectkind, String strProjectstatus, String strProjectphase,
      String strProduct, String strProjectpublic, String strSalesRep, String strcRegionId,
      String strCurrencyId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }

    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerFilterJR")
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportInvoiceCustomerFilterJR",
        false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportInvoiceCustomerJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportInvoiceCustomerFilterJR.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportInvoiceCustomerFilterJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportInvoiceCustomerJR");
      vars.removeMessage("ReportInvoiceCustomerJR");
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
    xmlDocument.setParameter("paramBPartnerId", strcBpartnerId);
    xmlDocument.setParameter("cProjectId", strcProjectId);
    xmlDocument.setParameter("projectName",
        InvoiceCustomerEditionData.selectProject(readOnlyCP, strcProjectId));
    xmlDocument.setParameter("mProductCatId", strmCategoryId);
    xmlDocument.setParameter("cProjectKind", strProjectkind);
    xmlDocument.setParameter("cRegionId", strcRegionId);
    xmlDocument.setParameter("cProjectPhase", strProjectphase);
    xmlDocument.setParameter("cProjectStatus", strProjectstatus);
    xmlDocument.setParameter("cProjectPublic", strProjectpublic);
    xmlDocument.setParameter("salesRep", strSalesRep);

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "LIST", "", "Projectkind", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strProjectkind);
      xmlDocument.setData("reportC_PROJECTKIND", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "LIST", "", "Projectphase", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strProjectphase);
      xmlDocument.setData("reportC_PROJECTPHASE", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "LIST", "", "ProjectStatus",
          "", Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strProjectstatus);
      xmlDocument.setData("reportC_PROJECTSTATUS", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "LIST", "", "PublicPrivate",
          "", Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strProjectpublic);
      xmlDocument.setData("reportC_PROJECTPUBLIC", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_PRODUCT_CATEGORY_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      comboTableData.fillParameters(null, "ReportInvoiceCustomerJR", "");
      xmlDocument.setData("reportC_PRODUCTCATREGORY", "liststructure",
          comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "TABLEDIR", "C_REGION_ID", "",
          "C_Region of Country",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strcRegionId);
      xmlDocument.setData("reportC_REGIONID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(readOnlyCP, "TABLE", "", "190",
          "AD_User SalesRep",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strSalesRep);
      xmlDocument.setData("reportSALESREP", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportInvoiceCustomerJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportInvoiceCustomerJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportInvoiceCustomerJR",
          strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setData("reportCBPartnerId_IN", "liststructure",
        SelectorUtilityData.selectBpartner(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strcBpartnerId));
    xmlDocument.setData("reportMProductId_IN", "liststructure",
        SelectorUtilityData.selectMproduct(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strProduct));

    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strdateFrom, String strdateTo, String strcProjectId,
      String strcBpartnerId, String strmCategoryId, String strProjectkind, String strProjectstatus,
      String strProjectphase, String strProduct, String strProjectpublic, String strSalesRep,
      String strcRegionId, String strCurrencyId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: print html");
    }

    InvoiceCustomerEditionData[] data = null;

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    String strConvRateErrorMsg = "";
    OBError myMessage = new OBError();
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      data = InvoiceCustomerEditionData.select(readOnlyCP, strCurrencyId,
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "InvoiceCustomerFilter"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "InvoiceCustomerFilter"),
          strdateFrom, strdateTo, strcBpartnerId, strcProjectId, strmCategoryId, strProjectkind,
          strProjectphase, strProjectstatus, strProjectpublic, strcRegionId, strSalesRep,
          strProduct);
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
      String strOutput = vars.commandIn("EDIT_HTML") ? "html" : "pdf";
      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerJR.jrxml";
      String strSubTitle = "";
      strSubTitle = Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " " + strdateFrom
          + " " + Utility.messageBD(readOnlyCP, "To", vars.getLanguage()) + " " + strdateTo;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strSubTitle);
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    }
  }
}
