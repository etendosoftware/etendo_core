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

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
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

public class ReportOrderNotInvoiceJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT")) {
      String strdateFrom = vars.getGlobalVariable("inpDateFrom", "ReportOrderNotInvoiceJR|dateFrom",
          "");
      String strdateTo = vars.getGlobalVariable("inpDateTo", "ReportOrderNotInvoiceJR|dateTo", "");
      String strcBpartnetId = vars.getGlobalVariable("inpcBPartnerId",
          "ReportOrderNotInvoiceJR|bpartner", "");
      String strCOrgId = vars.getGlobalVariable("inpOrg", "ReportOrderNotInvoiceJR|Org", "");
      String strInvoiceRule = vars.getGlobalVariable("inpInvoiceRule",
          "ReportOrderNotInvoiceJR|invoiceRule", "");
      String strDetail = vars.getStringParameter("inpDetail", "0");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportOrderNotInvoiceJR|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strdateFrom, strdateTo, strcBpartnetId, strCOrgId,
          strInvoiceRule, strDetail, strCurrencyId);
    } else if (vars.commandIn("FIND")) {
      String strdateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportOrderNotInvoiceJR|dateFrom");
      String strdateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportOrderNotInvoiceJR|dateTo");
      String strcBpartnetId = vars.getRequestGlobalVariable("inpcBPartnerId",
          "ReportOrderNotInvoiceJR|bpartner");
      String strCOrgId = vars.getRequestGlobalVariable("inpOrg", "ReportOrderNotInvoiceJR|Org");
      String strInvoiceRule = vars.getRequestGlobalVariable("inpInvoiceRule",
          "ReportOrderNotInvoiceJR|invoiceRule");
      String strDetail = vars.getStringParameter("inpDetail", "0");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportOrderNotInvoiceJR|currency", strUserCurrencyId);
      printPageHtml(request, response, vars, strdateFrom, strdateTo, strcBpartnetId, strCOrgId,
          strInvoiceRule, strDetail, strCurrencyId);
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strdateFrom, String strdateTo, String strcBpartnetId, String strCOrgId,
      String strInvoiceRule, String strDetail, String strCurrencyId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportOrderNotInvoiceFilterJR")
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportOrderNotInvoiceJR", false,
        "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportOrderNotInvoiceJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportOrderNotInvoiceJR.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportOrderNotInvoiceJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportOrderNotInvoiceJR");
      vars.removeMessage("ReportOrderNotInvoiceJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("dateFrom", strdateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strdateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("detail", strDetail);
    xmlDocument.setParameter("paramBPartnerId", strcBpartnetId);
    xmlDocument.setParameter("paramBPartnerDescription",
        ReportOrderNotInvoiceData.bPartnerDescription(readOnlyCP, strcBpartnetId));
    xmlDocument.setParameter("invoiceRule", strInvoiceRule);
    xmlDocument.setParameter("adOrgId", strCOrgId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST", "",
          "C_Order InvoiceRule", "Invoice Terms used in Orders Awaiting Invoice report",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportOrderNotInvoiceFilterJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportOrderNotInvoiceJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportOrderNotInvoiceJR",
          strInvoiceRule);
      xmlDocument.setData("reportInvoiceRule", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportOrderNotInvoiceJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportOrderNotInvoiceJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportOrderNotInvoiceJR",
          strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID",
          "", "", Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportOrderNotInvoiceJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportOrderNotInvoiceJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportOrderNotInvoiceJR",
          strCOrgId);
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
      comboTableData = null;

    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strdateFrom, String strdateTo, String strcBpartnetId,
      String strCOrgId, String strInvoiceRule, String strDetail, String strCurrencyId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: print html");
    }

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    ReportOrderNotInvoiceData[] data = null;
    String strConvRateErrorMsg = "";
    OBError myMessage = new OBError();
    String strTreeOrgId = "";
    if (!StringUtils.isEmpty(strCOrgId)) {
      strTreeOrgId = "(" + Utility.getInStrSet(
          OBContext.getOBContext().getOrganizationStructureProvider().getChildTree(strCOrgId, true))
          + ")";
    }
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      data = ReportOrderNotInvoiceData.select(readOnlyCP, strCurrencyId,
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportOrderNotInvoiceJR"),
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportOrderNotInvoiceJR"),
          strcBpartnetId, strTreeOrgId, strInvoiceRule, strdateFrom,
          DateTimeData.nDaysAfter(readOnlyCP, strdateTo, "1"), vars.getLanguage());
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
      String strOutput = "html";
      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportOrderNotInvoiceJR.jrxml";

      String strSubTitle = "";
      strSubTitle = Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " " + strdateFrom
          + " " + Utility.messageBD(readOnlyCP, "To", vars.getLanguage()) + " " + strdateTo;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("REPORT_SUBTITLE", strSubTitle);
      parameters.put("Detail", StringUtils.equals(strDetail, "-1"));
      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    }
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportOrderNotInvoiceFilter. This Servlet was made by Pablo Sarobe";
  } // end of getServletInfo() method
}
