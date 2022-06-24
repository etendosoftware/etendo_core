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
 * All portions are Copyright (C) 2001-2019 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.filter.IsPositiveIntFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.costing.CostingBackground;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.info.SelectorUtilityData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBCurrencyUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

public class ReportInvoiceCustomerDimensionalAnalysesJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT", "DEFAULT_COMPARATIVE")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo", "");
      String strDateFromRef = vars.getGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef", "");
      String strDateToRef = vars.getGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef", "");
      String strDateFromRef2 = vars.getGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2", "");
      String strDateToRef2 = vars.getGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2", "");
      String strDateFromRef3 = vars.getGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3", "");
      String strDateToRef3 = vars.getGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3", "");
      String strPartnerGroup = vars.getGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup", "");
      String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", "", IsIDFilter.instance);
      String strProductCategory = vars.getGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory", "");
      String strmProductId = vars.getInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", "", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInGlobalVariable("inpNotShown",
          "ReportInvoiceCustomerDimensionalAnalysesJR|notShown", "", IsPositiveIntFilter.instance);
      String strShown = vars.getInGlobalVariable("inpShown",
          "ReportInvoiceCustomerDimensionalAnalysesJR|shown", "", IsPositiveIntFilter.instance);
      String strOrg = vars.getGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org", "");
      String strsalesrepId = vars.getGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep", "");
      String strcProjectId = vars.getGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project", "");
      String strProducttype = vars.getGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype", "");
      String strcDocTypeId = vars.getInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", "", IsIDFilter.instance);
      String strOrder = vars.getGlobalVariable("inpOrder",
          "ReportInvoiceCustomerDimensionalAnalyze|order", "Normal");
      String strMayor = vars.getNumericGlobalVariable("inpMayor",
          "ReportInvoiceCustomerSalesDimensionalAnalyze|mayor", "");
      String strMenor = vars.getNumericGlobalVariable("inpMenor",
          "ReportInvoiceCustomerDimensionalAnalyze|menor", "");
      String strPartnerSalesRepId = vars.getGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep", "");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strComparative = "";
      String strVoid = "";
      if (vars.commandIn("DEFAULT_COMPARATIVE")) {
        strComparative = vars.getRequestGlobalVariable("inpComparative",
            "ReportInvoiceCustomerDimensionalAnalysesJR|comparative");
        strVoid = vars.getRequestGlobalVariable("inpVoid",
            "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      } else {
        strComparative = vars.getGlobalVariable("inpComparative",
            "ReportInvoiceCustomerDimensionalAnalysesJR|comparative", "N");
        strVoid = vars.getGlobalVariable("inpVoid",
            "ReportInvoiceCustomerDimensionalAnalysesJR|Void", "Y");
      }

      printPageDataSheet(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown, strShown,
          strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesRepId, strCurrencyId, strVoid);
    } else if (vars.commandIn("EDIT_HTML", "EDIT_HTML_COMPARATIVE")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportInvoiceCustomerDimensionalAnalysesJR|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown, strShown,
          strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "html");
    } else if (vars.commandIn("EDIT_PDF", "EDIT_PDF_COMPARATIVE")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportSalesDimensionalAnalyze|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown, strShown,
          strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "pdf");
    } else if (vars.commandIn("EXCEL")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateTo");
      String strDateFromRef = vars.getRequestGlobalVariable("inpDateFromRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef");
      String strDateToRef = vars.getRequestGlobalVariable("inpDateToRef",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef");
      String strDateFromRef2 = vars.getRequestGlobalVariable("inpDateFromRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef2");
      String strDateToRef2 = vars.getRequestGlobalVariable("inpDateToRef2",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef2");
      String strDateFromRef3 = vars.getRequestGlobalVariable("inpDateFromRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateFromRef3");
      String strDateToRef3 = vars.getRequestGlobalVariable("inpDateToRef3",
          "ReportInvoiceCustomerDimensionalAnalysesJR|dateToRef3");
      String strPartnerGroup = vars.getRequestGlobalVariable("inpPartnerGroup",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnerGroup");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partner", IsIDFilter.instance);
      String strProductCategory = vars.getRequestGlobalVariable("inpProductCategory",
          "ReportInvoiceCustomerDimensionalAnalysesJR|productCategory");
      String strmProductId = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|product", IsIDFilter.instance);
      // ad_ref_list.value for refercence_id 800087
      String strNotShown = vars.getInStringParameter("inpNotShown", IsPositiveIntFilter.instance);
      String strShown = vars.getInStringParameter("inpShown", IsPositiveIntFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg",
          "ReportInvoiceCustomerDimensionalAnalysesJR|org");
      String strsalesrepId = vars.getRequestGlobalVariable("inpSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|salesrep");
      String strcProjectId = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|project");
      String strProducttype = vars.getRequestGlobalVariable("inpProducttype",
          "ReportInvoiceCustomerDimensionalAnalysesJR|producttype");
      String strcDocTypeId = vars.getRequestInGlobalVariable("inpcDocTypeId_IN",
          "ReportInvoiceCustomerDimensionalAnalysesJR|doctype", IsIDFilter.instance);
      String strOrder = vars.getRequestGlobalVariable("inpOrder",
          "ReportSalesDimensionalAnalyze|order");
      String strMayor = vars.getNumericParameter("inpMayor", "");
      String strMenor = vars.getNumericParameter("inpMenor", "");
      String strComparative = vars.getStringParameter("inpComparative", "N");
      String strPartnerSalesrepId = vars.getRequestGlobalVariable("inpPartnerSalesrepId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|partnersalesrep");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportInvoiceCustomerDimensionalAnalysesJR|currency", strUserCurrencyId);
      String strVoid = vars.getRequestGlobalVariable("inpVoid",
          "ReportInvoiceCustomerDimensionalAnalysesJR|Void");
      printPageHtml(request, response, vars, strComparative, strDateFrom, strDateTo,
          strPartnerGroup, strcBpartnerId, strProductCategory, strmProductId, strNotShown, strShown,
          strDateFromRef, strDateToRef, strDateFromRef2, strDateToRef2, strDateFromRef3,
          strDateToRef3, strOrg, strsalesrepId, strcProjectId, strProducttype, strcDocTypeId,
          strOrder, strMayor, strMenor, strPartnerSalesrepId, strCurrencyId, strVoid, "xls");
    } else if (vars.commandIn("CUR")) {
      String orgId = vars.getStringParameter("inpOrg");
      String strOrgCurrencyId = OBCurrencyUtils.getOrgCurrency(orgId);
      if (StringUtils.isEmpty(strOrgCurrencyId)) {
        strOrgCurrencyId = strUserCurrencyId;
      }
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.print(StringEscapeUtils.escapeHtml(strOrgCurrencyId));
      out.close();
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strComparative, String strDateFrom, String strDateTo,
      String strPartnerGroup, String strcBpartnerId, String strProductCategory,
      String strmProductId, String strNotShown, String strShown, String strDateFromRef,
      String strDateToRef, String strDateFromRef2, String strDateToRef2, String strDateFromRef3,
      String strDateToRef3, String strOrg, String strsalesrepId, String strcProjectId,
      String strProducttype, String strcDocTypeId, String strOrder, String strMayor,
      String strMenor, String strPartnerSalesrepId, String strCurrencyId, String strVoid)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    String discard[] = { "selEliminarHeader1" };
    if (StringUtils.equals(strComparative, "Y")) {
      discard[0] = "selEliminarHeader2";
    }
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate(
            "org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesJRFilter",
            discard)
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(),
        "ReportInvoiceCustomerDimensionalAnalysesJRFilter", false, "", "", "", false, "ad_reports",
        strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());
    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportInvoiceCustomerDimensionalAnalysesJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportInvoiceCustomerDimensionalAnalysesJR.html", classInfo.id, classInfo.type,
          strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportInvoiceCustomerDimensionalAnalysesJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportInvoiceCustomerDimensionalAnalysesJR");
      vars.removeMessage("ReportInvoiceCustomerDimensionalAnalysesJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
      if (!transactionCostDateAcctInitialized()) {
        advise(request, response, "ERROR", Utility.messageBD(readOnlyCP,
            "TransactionCostDateAcctNotInitilized", vars.getLanguage()), "");
        return;
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef", strDateFromRef);
    xmlDocument.setParameter("dateFromRefdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRefsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef", strDateToRef);
    xmlDocument.setParameter("dateToRefdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRefsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef2", strDateFromRef2);
    xmlDocument.setParameter("dateFromRef2displayFormat",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef2saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef2", strDateToRef2);
    xmlDocument.setParameter("dateToRef2displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef2saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef3", strDateFromRef3);
    xmlDocument.setParameter("dateFromRef3displayFormat",
        vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromRef3saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef3", strDateToRef3);
    xmlDocument.setParameter("dateToRef3displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateToRef3saveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("cBpGroupId", strPartnerGroup);
    xmlDocument.setParameter("mProductCategoryId", strProductCategory);
    xmlDocument.setParameter("adOrgId", strOrg);
    xmlDocument.setParameter("salesRepId", strsalesrepId);
    xmlDocument.setParameter("normal", strOrder);
    xmlDocument.setParameter("amountasc", strOrder);
    xmlDocument.setParameter("amountdesc", strOrder);
    xmlDocument.setParameter("mayor", strMayor);
    xmlDocument.setParameter("menor", strMenor);
    xmlDocument.setParameter("comparative", strComparative);
    xmlDocument.setParameter("void", strVoid);
    xmlDocument.setParameter("cProjectId", strcProjectId);
    xmlDocument.setParameter("producttype", strProducttype);
    xmlDocument.setParameter("partnerSalesRepId", strPartnerSalesrepId);
    xmlDocument.setParameter("projectName",
        ReportInvoiceCustomerDimensionalAnalysesJRData.selectProject(readOnlyCP, strcProjectId));
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_BP_Group_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strPartnerGroup);
      xmlDocument.setData("reportC_BP_GROUPID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_Product_Category_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strProductCategory);
      xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID",
          "", "",
          Utility.getContext(readOnlyCP, vars, "#User_Org",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strOrg);
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
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
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strmProductId));

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "LIST", "",
          "M_Product_ProductType", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", "");
      xmlDocument.setData("reportProductType", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE", "SalesRep_ID",
          "AD_User SalesRep", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportSalesDimensionalAnalyzeJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportSalesDimensionalAnalyzeJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportSalesDimensionalAnalyzeJR", strsalesrepId);
      xmlDocument.setData("reportSalesRep_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE", "",
          "C_BPartner SalesRep", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strPartnerSalesrepId);
      xmlDocument.setData("reportPartnerSalesRep_ID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"),
          0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportInvoiceCustomerDimensionalAnalysesJR", strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setData("reportCDocTypeId_IN", "liststructure",
        SelectorUtilityData.selectCDocType(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strcDocTypeId));

    if (StringUtils.equals(vars.getLanguage(), "en_US")) {
      xmlDocument.setData("structure1",
          ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShown(readOnlyCP, strShown));
      xmlDocument.setData("structure2",
          StringUtils.isEmpty(strShown) ? new ReportInvoiceCustomerDimensionalAnalysesJRData[0]
              : ReportInvoiceCustomerDimensionalAnalysesJRData.selectShown(readOnlyCP, strShown));
    } else {
      xmlDocument.setData("structure1", ReportInvoiceCustomerDimensionalAnalysesJRData
          .selectNotShownTrl(readOnlyCP, vars.getLanguage(), strShown));
      xmlDocument.setData("structure2",
          StringUtils.isEmpty(strShown) ? new ReportInvoiceCustomerDimensionalAnalysesJRData[0]
              : ReportInvoiceCustomerDimensionalAnalysesJRData.selectShownTrl(readOnlyCP,
                  vars.getLanguage(), strShown));
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strComparative, String strDateFrom, String strDateTo,
      String strPartnerGroup, String strcBpartnerId, String strProductCategory,
      String strmProductId, String strNotShown, String strShown, String strDateFromRef,
      String strDateToRef, String strDateFromRef2, String strDateToRef2, String strDateFromRef3,
      String strDateToRef3, String strOrg, String strsalesrepId, String strcProjectId,
      String strProducttype, String strcDocTypeId, String strOrder, String strMayor,
      String strMenor, String strPartnerSalesrepId, String strCurrencyId, String strVoid,
      String strOutput) throws IOException, ServletException {
    String localStrShown = strShown;
    String localStrOrg = strOrg;
    String localStrDateToRef3 = strDateToRef3;
    String localStrDateToRef2 = strDateToRef2;
    String localStrDateFromRef2 = strDateFromRef2;
    String localStrDateFromRef3 = strDateFromRef3;
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: print html");
    }
    String strOrderby = "";
    String[] discard = { "", "", "", "", "", "", "", "", "", "" };
    String[] discard1 = { "selEliminarBody1", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard", "discard", "discard", "discard",
        "discard", "discard", "discard", "discard", "discard" };
    if (StringUtils.isEmpty(localStrOrg)) {
      localStrOrg = vars.getOrg();
    }
    boolean multiComparative2 = false;
    boolean multiComparative3 = false;
    if (StringUtils.equals(strComparative, "Y")) {
      discard1[0] = "selEliminarBody2";
      if (StringUtils.isNotBlank(localStrDateFromRef2)
          && StringUtils.isNotBlank(localStrDateToRef2)) {
        multiComparative2 = true;
        if (StringUtils.isNotBlank(localStrDateFromRef3)
            && StringUtils.isNotBlank(localStrDateToRef3)) {
          multiComparative3 = true;
        } else {
          localStrDateFromRef3 = "";
          localStrDateToRef3 = "";
        }
      } else {
        localStrDateFromRef2 = "";
        localStrDateToRef2 = "";
        localStrDateFromRef3 = "";
        localStrDateToRef3 = "";
      }
    }

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strTitle = Utility.messageBD(readOnlyCP, "From", vars.getLanguage()) + " " + strDateFrom
        + " " + Utility.messageBD(readOnlyCP, "to", vars.getLanguage()) + " " + strDateTo;
    String strSubTitle = strTitle;
    if (StringUtils.isNotEmpty(strPartnerGroup)) {
      strTitle = strTitle + ", "
          + Utility.messageBD(readOnlyCP, "ForBPartnerGroup", vars.getLanguage()) + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectBpgroup(readOnlyCP,
              strPartnerGroup);
    }
    String treeTable = Tree.getTreeTable(readOnlyCP, vars.getClient(), "209");
    String productCategoryGroup = Tree.getMembers(readOnlyCP, treeTable, strProductCategory);
    if (StringUtils.isNotEmpty(strProductCategory)) {
      strTitle = strTitle + ", "
          + Utility.messageBD(readOnlyCP, "ProductCategory", vars.getLanguage()) + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProductCategory(readOnlyCP,
              strProductCategory);
      productCategoryGroup = "(" + productCategoryGroup + ")";
    }
    if (StringUtils.isNotEmpty(strcProjectId)) {
      strTitle = strTitle + ", " + Utility.messageBD(readOnlyCP, "Project", vars.getLanguage())
          + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProject(readOnlyCP, strcProjectId);
    }
    if (StringUtils.isNotEmpty(strProducttype)) {
      strTitle = strTitle + ", " + Utility.messageBD(readOnlyCP, "PRODUCTTYPE", vars.getLanguage())
          + " " + ReportInvoiceCustomerDimensionalAnalysesJRData.selectProducttype(readOnlyCP,
              "270", vars.getLanguage(), strProducttype);
    }
    if (StringUtils.isNotEmpty(strsalesrepId)) {
      strTitle = strTitle + ", "
          + Utility.messageBD(readOnlyCP, "TheClientSalesRep", vars.getLanguage()) + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectSalesrep(readOnlyCP,
              strsalesrepId);
    }
    if (StringUtils.isNotEmpty(strPartnerSalesrepId)) {
      strTitle = strTitle + " " + Utility.messageBD(readOnlyCP, "And", vars.getLanguage()) + " "
          + Utility.messageBD(readOnlyCP, "TheClientSalesRep", vars.getLanguage()) + " "
          + ReportInvoiceCustomerDimensionalAnalysesJRData.selectSalesrep(readOnlyCP,
              strPartnerSalesrepId);
    }

    ReportInvoiceCustomerDimensionalAnalysesJRData[] data = null;
    ReportInvoiceCustomerDimensionalAnalysesJRData[] dataXLS = null;
    String[] strShownArray = { "", "", "", "", "", "", "", "", "", "" };
    if (localStrShown.startsWith("(")) {
      localStrShown = localStrShown.substring(1, localStrShown.length() - 1);
    }
    if (StringUtils.isNotEmpty(localStrShown)) {
      localStrShown = Replace.replace(localStrShown, "'", "");
      localStrShown = Replace.replace(localStrShown, " ", "");
      StringTokenizer st = new StringTokenizer(localStrShown, ",", false);
      int intContador = 0;
      while (st.hasMoreTokens()) {
        strShownArray[intContador] = st.nextToken();
        intContador++;
      }

    }

    ReportInvoiceCustomerDimensionalAnalysesJRData[] dimensionLabel = null;
    if (StringUtils.equals(vars.getLanguage(), "en_US")) {
      dimensionLabel = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShown(readOnlyCP,
          "");
    } else {
      dimensionLabel = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNotShownTrl(readOnlyCP,
          vars.getLanguage(), "");
    }

    String[] strLevelLabel = { "", "", "", "", "", "", "", "", "", "" };
    String[] strTextShow = { "", "", "", "", "", "", "", "", "", "" };
    int intOrder = 0;
    int intProductLevel = 11;
    int intAuxDiscard = -1;
    for (int i = 0; i < 10; i++) {
      if (StringUtils.equals(strShownArray[i], "1")) {
        strTextShow[i] = "C_BP_GROUP.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[0].name;
      } else if (StringUtils.equals(strShownArray[i], "2")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('C_Bpartner'), to_char( C_BPARTNER.C_BPARTNER_ID), to_char( '"
            + vars.getLanguage() + "'))";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[1].name;
      } else if (StringUtils.equals(strShownArray[i], "3")) {
        strTextShow[i] = "M_PRODUCT_CATEGORY.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[2].name;
      } else if (StringUtils.equals(strShownArray[i], "4")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('M_Product'), to_char( M_PRODUCT.M_PRODUCT_ID), to_char( '"
            + vars.getLanguage()
            + "'))|| CASE WHEN uomsymbol IS NULL THEN '' ELSE to_char(' ('||uomsymbol||')') END";
        intAuxDiscard = i;
        intOrder++;
        intProductLevel = i + 1;
        strLevelLabel[i] = dimensionLabel[3].name;
      } else if (StringUtils.equals(strShownArray[i], "5")) {
        strTextShow[i] = "C_INVOICE.DOCUMENTNO";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[4].name;
      } else if (StringUtils.equals(strShownArray[i], "6")) {
        strTextShow[i] = "AD_USER.FIRSTNAME||' '||' '||AD_USER.LASTNAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[5].name;
      } else if (StringUtils.equals(strShownArray[i], "8")) {
        strTextShow[i] = "AD_ORG.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[6].name;
      } else if (StringUtils.equals(strShownArray[i], "9")) {
        strTextShow[i] = "CASE WHEN AD_USER.AD_USER_ID IS NOT NULL THEN AD_COLUMN_IDENTIFIER(to_char('Ad_User'), to_char( AD_USER.AD_USER_ID), to_char( '"
            + vars.getLanguage() + "')) ELSE '' END";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[7].name;
      } else if (StringUtils.equals(strShownArray[i], "10")) {
        strTextShow[i] = "C_PROJECT.NAME";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[8].name;
      } else if (StringUtils.equals(strShownArray[i], "11")) {
        strTextShow[i] = "AD_COLUMN_IDENTIFIER(to_char('C_Bpartner_Location'), to_char( M_INOUT.C_BPARTNER_LOCATION_ID), to_char( '"
            + vars.getLanguage() + "'))";
        intOrder++;
        strLevelLabel[i] = dimensionLabel[9].name;
      } else {
        strTextShow[i] = "''";
        discard[i] = "display:none;";
      }
    }
    if (intOrder != 0 || intAuxDiscard != -1) {
      int k = 1;
      if (intOrder == 1) {
        strOrderby = " ORDER BY  NIVEL" + k + ",";
      } else {
        strOrderby = " ORDER BY ";
      }
      while (k < intOrder) {
        strOrderby = strOrderby + "NIVEL" + k + ",";
        k++;
      }
      if (k == 1) {
        if (StringUtils.equals(strOrder, "Normal")) {
          strOrderby = " ORDER BY NIVEL" + k;
        } else if (StringUtils.equals(strOrder, "Amountasc")) {
          strOrderby = " ORDER BY CONVAMOUNT ASC";
        } else if (StringUtils.equals(strOrder, "Amountdesc")) {
          strOrderby = " ORDER BY CONVAMOUNT DESC";
        } else {
          strOrderby = "1";
        }
      } else {
        if (StringUtils.equals(strOrder, "Normal")) {
          strOrderby += "NIVEL" + k;
        } else if (StringUtils.equals(strOrder, "Amountasc")) {
          strOrderby += "CONVAMOUNT ASC";
        } else if (StringUtils.equals(strOrder, "Amountdesc")) {
          strOrderby += "CONVAMOUNT DESC";
        } else {
          strOrderby = "1";
        }
      }

    } else {
      strOrderby = " ORDER BY 1";
    }
    String strHaving = "";
    if (StringUtils.isNotEmpty(strMayor) && StringUtils.isNotEmpty(strMenor)) {
      strHaving = " HAVING (SUM(CONVAMOUNT) >= " + strMayor + " AND SUM(CONVAMOUNT) <= " + strMenor
          + ")";
    } else if (StringUtils.isNotEmpty(strMayor) && StringUtils.isEmpty(strMenor)) {
      strHaving = " HAVING (SUM(CONVAMOUNT) >= " + strMayor + ")";
    } else if (StringUtils.isEmpty(strMayor) && StringUtils.isNotEmpty(strMenor)) {
      strHaving = " HAVING (SUM(CONVAMOUNT) <= " + strMenor + ")";
    }
    strOrderby = strHaving + strOrderby;

    int limit = Integer.parseInt(Utility.getPreference(vars, "ReportsLimit", ""));
    String pgLimit = null, oraLimit = null;
    if (StringUtils.equalsIgnoreCase(readOnlyCP.getRDBMS(), "ORACLE")) {
      oraLimit = String.valueOf(limit + 1);
    } else {
      pgLimit = String.valueOf(limit + 1);
    }

    // Checks if there is a conversion rate for each of the transactions of the report
    String strConvRateErrorMsg = "";
    OBError myMessage = null;
    myMessage = new OBError();
    if (StringUtils.equals(strOutput, "xls")) {
      try {
        dataXLS = ReportInvoiceCustomerDimensionalAnalysesJRData.selectXLS(readOnlyCP,
            strCurrencyId, vars.getLanguage(),
            Tree.getMembers(readOnlyCP, TreeData.getTreeOrg(readOnlyCP, vars.getClient()),
                localStrOrg),
            Utility.getContext(readOnlyCP, vars, "#User_Client",
                "ReportInvoiceCustomerDimensionalAnalysesJR"),
            strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strPartnerGroup,
            strcBpartnerId, productCategoryGroup, strmProductId, strsalesrepId,
            strPartnerSalesrepId, strcProjectId, strProducttype, strcDocTypeId,
            StringUtils.equals(strVoid, "Y") ? "" : "VO", pgLimit, oraLimit);

      } catch (ServletException ex) {
        myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
      }
    } else {
      if (StringUtils.equals(strComparative, "Y")) {
        try {
          if (multiComparative2) {
            if (multiComparative3) {
              // Multi-comparative B: 1 base date, 3 reference dates
              data = ReportInvoiceCustomerDimensionalAnalysesJRData.select3(readOnlyCP,
                  strCurrencyId, strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3],
                  strTextShow[4], strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8],
                  strTextShow[9],
                  Tree.getMembers(readOnlyCP, TreeData.getTreeOrg(readOnlyCP, vars.getClient()),
                      localStrOrg),
                  Utility.getContext(readOnlyCP, vars, "#User_Client",
                      "ReportInvoiceCustomerDimensionalAnalysesJR"),
                  strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strPartnerGroup,
                  strcBpartnerId, productCategoryGroup, strmProductId, strsalesrepId,
                  strPartnerSalesrepId, strcProjectId, strProducttype, strcDocTypeId,
                  StringUtils.equals(strVoid, "Y") ? "" : "VO", strDateFromRef,
                  DateTimeData.nDaysAfter(readOnlyCP, strDateToRef, "1"), localStrDateFromRef2,
                  DateTimeData.nDaysAfter(readOnlyCP, localStrDateToRef2, "1"),
                  localStrDateFromRef3,
                  DateTimeData.nDaysAfter(readOnlyCP, localStrDateToRef3, "1"), strOrderby, pgLimit,
                  oraLimit);
            } else {
              // Multi-comparative A: 1 base date, 2 reference dates
              data = ReportInvoiceCustomerDimensionalAnalysesJRData.select2(readOnlyCP,
                  strCurrencyId, strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3],
                  strTextShow[4], strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8],
                  strTextShow[9],
                  Tree.getMembers(readOnlyCP, TreeData.getTreeOrg(readOnlyCP, vars.getClient()),
                      localStrOrg),
                  Utility.getContext(readOnlyCP, vars, "#User_Client",
                      "ReportInvoiceCustomerDimensionalAnalysesJR"),
                  strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strPartnerGroup,
                  strcBpartnerId, productCategoryGroup, strmProductId, strsalesrepId,
                  strPartnerSalesrepId, strcProjectId, strProducttype, strcDocTypeId,
                  StringUtils.equals(strVoid, "Y") ? "" : "VO", strDateFromRef,
                  DateTimeData.nDaysAfter(readOnlyCP, strDateToRef, "1"), localStrDateFromRef2,
                  DateTimeData.nDaysAfter(readOnlyCP, localStrDateToRef2, "1"), strOrderby, pgLimit,
                  oraLimit);
            }
          } else {
            // Regular comparative: 1 base date, 1 reference date
            data = ReportInvoiceCustomerDimensionalAnalysesJRData.select(readOnlyCP, strCurrencyId,
                strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3], strTextShow[4],
                strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8], strTextShow[9],
                Tree.getMembers(readOnlyCP, TreeData.getTreeOrg(readOnlyCP, vars.getClient()),
                    localStrOrg),
                Utility.getContext(readOnlyCP, vars, "#User_Client",
                    "ReportInvoiceCustomerDimensionalAnalysesJR"),
                strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strPartnerGroup,
                strcBpartnerId, productCategoryGroup, strmProductId, strsalesrepId,
                strPartnerSalesrepId, strcProjectId, strProducttype, strcDocTypeId,
                StringUtils.equals(strVoid, "Y") ? "" : "VO", strDateFromRef,
                DateTimeData.nDaysAfter(readOnlyCP, strDateToRef, "1"), strOrderby, pgLimit,
                oraLimit);
          }
        } catch (ServletException ex) {
          myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
        }
      } else {
        try {
          data = ReportInvoiceCustomerDimensionalAnalysesJRData.selectNoComparative(readOnlyCP,
              strCurrencyId, strTextShow[0], strTextShow[1], strTextShow[2], strTextShow[3],
              strTextShow[4], strTextShow[5], strTextShow[6], strTextShow[7], strTextShow[8],
              strTextShow[9],
              Tree.getMembers(readOnlyCP, TreeData.getTreeOrg(readOnlyCP, vars.getClient()),
                  localStrOrg),
              Utility.getContext(readOnlyCP, vars, "#User_Client",
                  "ReportInvoiceCustomerDimensionalAnalysesJR"),
              strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strPartnerGroup,
              strcBpartnerId, productCategoryGroup, strmProductId, strsalesrepId,
              strPartnerSalesrepId, strcProjectId, strProducttype, strcDocTypeId,
              StringUtils.equals(strVoid, "Y") ? "" : "VO", strOrderby, pgLimit, oraLimit);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
        }
      }
    }

    strConvRateErrorMsg = myMessage.getMessage();
    // If a conversion rate is missing for a certain transaction, an error message window pops-up.
    if (StringUtils.isNotEmpty(strConvRateErrorMsg)) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()),
          strConvRateErrorMsg);
    } else {
      // Otherwise, the report is launched

      // Passing secondary filters Organization tree and businees partner group and product group
      Organization organization = OBDal.getReadOnlyInstance().get(Organization.class, strOrg);
      String strOrgName = organization.getName();
      String strPartnerGroupName = ReportInvoiceCustomerDimensionalAnalysesJRData
          .selectBpgroup(readOnlyCP, strPartnerGroup);
      String strProductGroupName = ReportInvoiceCustomerDimensionalAnalysesJRData
          .selectProductCategory(readOnlyCP, strProductCategory);

      if (StringUtils.equals(strOutput, "xls")) {
        if (limit > 0 && dataXLS.length > limit) {
          String msgbody = Utility.messageBD(readOnlyCP, "ReportsLimit", vars.getLanguage());
          msgbody = msgbody.replace("@limit@", String.valueOf(limit));
          advisePopUp(request, response, "WARNING",
              Utility.messageBD(this, "ReportsLimitHeader", vars.getLanguage()), msgbody);
          return;
        }
        if (dataXLS == null || dataXLS.length == 0) {
          advisePopUp(request, response, "WARNING",
              Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
              Utility.messageBD(readOnlyCP, "NoDataFound", vars.getLanguage()));
        } else {
          /*
           * int rowLimit = 65532; ScrollableFieldProvider limitedData = new
           * LimitRowsScrollableFieldProviderFilter( dataXLS, rowLimit);
           */
          String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesXLS.jrxml";

          HashMap<String, Object> parameters = new HashMap<String, Object>();

          String strLanguage = vars.getLanguage();
          String strBaseDesign = getBaseDesignPath(strLanguage);

          JasperReport jasperReportLines;
          try {
            jasperReportLines = ReportingUtils.compileReport(strBaseDesign
                + "/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalyses_srpt_doctypecount.jrxml");
          } catch (JRException e) {
            log4j.error("Error compiling report ", e);
            throw new ServletException(e.getMessage());
          }

          parameters.put("USER_CLIENT", Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"));
          String strDateFormat;
          strDateFormat = vars.getJavaDateFormat();
          parameters.put("strDateFormat", strDateFormat);
          parameters.put("BASE_DESIGN", strBaseDesign);
          parameters.put("ORGANIZATION", strOrgName);
          parameters.put("BPGROUP", strPartnerGroupName);
          parameters.put("PRODUCTGROUP", strProductGroupName);
          parameters.put("C_CURRENCY_ID", strCurrencyId);
          parameters.put("AD_ORG_ID", Tree.getMembers(readOnlyCP,
              TreeData.getTreeOrg(readOnlyCP, vars.getClient()), strOrg));
          parameters.put("DateFrom", strDateFrom);
          parameters.put("DateTo", DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"));
          parameters.put("C_BP_GROUP_ID", strPartnerGroup);
          parameters.put("C_BPARTNER_ID", strcBpartnerId);
          parameters.put("M_PRODUCT_CATEGORY_ID", productCategoryGroup);
          parameters.put("M_PRODUCT_ID", strmProductId);
          parameters.put("SALESREP_ID", strsalesrepId);
          parameters.put("PARTNER_SALESREP_ID", strPartnerSalesrepId);
          parameters.put("C_PROJECT_ID", strcProjectId);
          parameters.put("PRODUCTTYPE", strProducttype);
          parameters.put("C_DOCTYPE_ID", strcDocTypeId);
          parameters.put("DOCSTATUS", StringUtils.equals(strVoid, "Y") ? "" : "VO");
          parameters.put("LANGUAGE", vars.getLanguage());
          parameters.put("SR_LINES", jasperReportLines);

          renderJR(vars, response, strReportName, null, "xls", parameters, dataXLS, null);
        }
      } else {
        if (limit > 0 && data.length > limit) {
          String msgbody = Utility.messageBD(readOnlyCP, "ReportsLimit", vars.getLanguage());
          msgbody = msgbody.replace("@limit@", String.valueOf(limit));
          advisePopUp(request, response, "WARNING",
              Utility.messageBD(this, "ReportsLimitHeader", vars.getLanguage()), msgbody);
          return;
        }
        String strReportPath;
        if (StringUtils.equals(strComparative, "Y")) {
          strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesComparativeJR.jrxml";
          if (multiComparative2) {
            strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesMultiComparativeJR.jrxml";
          }
          if (multiComparative3) {
            strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesMultiComparativeExtendedJR.jrxml";
          }
        } else {
          strReportPath = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalysesNoComparativeJR.jrxml";
        }
        if (data == null || data.length == 0) {
          advisePopUp(request, response, "WARNING",
              Utility.messageBD(readOnlyCP, "ProcessStatus-W", vars.getLanguage()),
              Utility.messageBD(readOnlyCP, "NoDataFound", vars.getLanguage()));
        } else {
          String strLanguage = vars.getLanguage();
          String strBaseDesign = getBaseDesignPath(strLanguage);

          JasperReport jasperReportLines;
          try {
            jasperReportLines = ReportingUtils.compileReport(strBaseDesign
                + "/org/openbravo/erpCommon/ad_reports/ReportInvoiceCustomerDimensionalAnalyses_srpt_doctypecount.jrxml");
          } catch (JRException e) {
            log4j.error("Error compiling report ", e);
            throw new ServletException(e.getMessage());
          }

          HashMap<String, Object> parameters = new HashMap<String, Object>();

          parameters.put("BASE_DESIGN", strBaseDesign);
          parameters.put("USER_CLIENT", Utility.getContext(readOnlyCP, vars, "#User_Client",
              "ReportInvoiceCustomerDimensionalAnalysesJR"));
          parameters.put("ORGANIZATION", strOrgName);
          parameters.put("BPGROUP", strPartnerGroupName);
          parameters.put("PRODUCTGROUP", strProductGroupName);
          parameters.put("C_CURRENCY_ID", strCurrencyId);
          parameters.put("AD_ORG_ID", Tree.getMembers(readOnlyCP,
              TreeData.getTreeOrg(readOnlyCP, vars.getClient()), strOrg));
          parameters.put("DateFrom", strDateFrom);
          parameters.put("DateTo", DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"));
          parameters.put("C_BP_GROUP_ID", strPartnerGroup);
          parameters.put("C_BPARTNER_ID", strcBpartnerId);
          parameters.put("M_PRODUCT_CATEGORY_ID", productCategoryGroup);
          parameters.put("M_PRODUCT_ID", strmProductId);
          parameters.put("SALESREP_ID", strsalesrepId);
          parameters.put("PARTNER_SALESREP_ID", strPartnerSalesrepId);
          parameters.put("C_PROJECT_ID", strcProjectId);
          parameters.put("PRODUCTTYPE", strProducttype);
          parameters.put("C_DOCTYPE_ID", strcDocTypeId);
          parameters.put("DOCSTATUS", StringUtils.equals(strVoid, "Y") ? "" : "VO");
          parameters.put("LANGUAGE", vars.getLanguage());
          parameters.put("SR_LINES", jasperReportLines);

          parameters.put("LEVEL1_LABEL", strLevelLabel[0]);
          parameters.put("LEVEL2_LABEL", strLevelLabel[1]);
          parameters.put("LEVEL3_LABEL", strLevelLabel[2]);
          parameters.put("LEVEL4_LABEL", strLevelLabel[3]);
          parameters.put("LEVEL5_LABEL", strLevelLabel[4]);
          parameters.put("LEVEL6_LABEL", strLevelLabel[5]);
          parameters.put("LEVEL7_LABEL", strLevelLabel[6]);
          parameters.put("LEVEL8_LABEL", strLevelLabel[7]);
          parameters.put("LEVEL9_LABEL", strLevelLabel[8]);
          parameters.put("LEVEL10_LABEL", strLevelLabel[9]);
          parameters.put("DIMENSIONS", intOrder);
          parameters.put("REPORT_SUBTITLE", strSubTitle);
          parameters.put("PRODUCT_LEVEL", intProductLevel);
          renderJR(vars, response, strReportPath, strOutput, parameters, data, null);
        }
      }
    }
  }

  private boolean transactionCostDateAcctInitialized() {
    boolean transactionCostDateacctInitialized = false;
    Client client = OBDal.getReadOnlyInstance().get(Client.class, "0");
    Organization organization = OBDal.getReadOnlyInstance().get(Organization.class, "0");
    try {
      transactionCostDateacctInitialized = StringUtils.equals(
          Preferences.getPreferenceValue(CostingBackground.TRANSACTION_COST_DATEACCT_INITIALIZED,
              false, client, organization, null, null, null),
          Preferences.YES);
    } catch (PropertyException e1) {
      transactionCostDateacctInitialized = false;
    }
    return transactionCostDateacctInitialized;
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportInvoiceCustomerDimensionalAnalysesJR. This Servlet was made by Jon Alegría";
  } // end of getServletInfo() method
}
