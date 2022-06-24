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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.TreeData;
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

public class ReportAgingBalance extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    // Get user Client's base currency
    final String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());

    if (vars.commandIn("DEFAULT")) {
      String strisReceipt = vars.getGlobalVariable("inpReceipt", "ReportAgingBalance|IsReceipt",
          "N");
      String strcolumn1 = vars.getNumericGlobalVariable("inpColumn1", "ReportAgingBalance|Column1",
          "10");
      String strcolumn2 = vars.getNumericGlobalVariable("inpColumn2", "ReportAgingBalance|Column2",
          "20");
      String strcolumn3 = vars.getNumericGlobalVariable("inpColumn3", "ReportAgingBalance|Column3",
          "30");
      String strcolumn4 = vars.getNumericGlobalVariable("inpColumn4", "ReportAgingBalance|Column4",
          "60");
      String strcBpartnerId = vars.getInGlobalVariable("inpcBPartnerId_IN",
          "ReportAgingBalance|cBpartnerId", "", IsIDFilter.instance);
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportAgingBalance|Org", "");
      final String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportAgingBalance|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strisReceipt, strcolumn1, strcolumn2, strcolumn3,
          strcolumn4, strcBpartnerId, strOrg, "Y", strCurrencyId);
    } else if (vars.commandIn("FIND")) {
      String strisReceipt = vars.getRequestGlobalVariable("inpReceipt",
          "ReportAgingBalance|IsReceipt");
      String strcolumn1 = vars.getNumericRequestGlobalVariable("inpColumn1",
          "ReportAgingBalance|Column1");
      String strcolumn2 = vars.getNumericRequestGlobalVariable("inpColumn2",
          "ReportAgingBalance|Column2");
      String strcolumn3 = vars.getNumericRequestGlobalVariable("inpColumn3",
          "ReportAgingBalance|Column3");
      String strcolumn4 = vars.getNumericRequestGlobalVariable("inpColumn4",
          "ReportAgingBalance|Column4");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportAgingBalance|cBpartnerId", IsIDFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportAgingBalance|Org");
      final String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportAgingBalance|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strisReceipt, strcolumn1, strcolumn2, strcolumn3,
          strcolumn4, strcBpartnerId, strOrg, "N", strCurrencyId);
    } else if (vars.commandIn("PRINT_PDF")) {
      String strisReceipt = vars.getRequestGlobalVariable("inpReceipt",
          "ReportAgingBalance|IsReceipt");
      String strcolumn1 = vars.getNumericRequestGlobalVariable("inpColumn1",
          "ReportAgingBalance|Column1");
      String strcolumn2 = vars.getNumericRequestGlobalVariable("inpColumn2",
          "ReportAgingBalance|Column2");
      String strcolumn3 = vars.getNumericRequestGlobalVariable("inpColumn3",
          "ReportAgingBalance|Column3");
      String strcolumn4 = vars.getNumericRequestGlobalVariable("inpColumn4",
          "ReportAgingBalance|Column4");
      String strcBpartnerId = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportAgingBalance|cBpartnerId", IsIDFilter.instance);
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportAgingBalance|Org");
      final String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportAgingBalance|currency", strUserCurrencyId);
      printPageDataPdf(request, response, vars, strisReceipt, strcolumn1, strcolumn2, strcolumn3,
          strcolumn4, strcBpartnerId, strOrg, "N", strCurrencyId);
    } else {
      pageError(response);
    }
  }

  private void printPageDataPdf(final HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strisReceipt, String strcolumn1, String strcolumn2,
      String strcolumn3, String strcolumn4, String strcBpartnerId, String strOrgTrx,
      String strfirstPrint, final String strCurrencyId) throws IOException, ServletException {
    String localStrisReceipt = strisReceipt;
    ReportAgingBalanceData[] data = null;

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strTreeOrg = TreeData.getTreeOrg(readOnlyCP, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrgTrx);

    if (StringUtils.isEmpty(localStrisReceipt)) {
      localStrisReceipt = "N";
    }

    try {
      data = ReportAgingBalanceData.select(readOnlyCP, vars.getLanguage(), strOrgTrx, strCurrencyId,
          strcolumn1, strcolumn2, strcolumn3, strcolumn4, localStrisReceipt, strcBpartnerId,
          strOrgFamily, Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportAgingBalance"),
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportAgingBalance"));
    } catch (ServletException ex) {
      advisePopUp(request, response,
          Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()),
          Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage())
              .getMessage());
    }
    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportAgingBalance.jrxml";
    HashMap<String, Object> parameters = new HashMap<String, Object>();
    parameters.put("col1", "0 - " + strcolumn1);
    parameters.put("col2", String.valueOf((Integer.valueOf(strcolumn1) + 1)) + " - " + strcolumn2);
    parameters.put("col3", String.valueOf((Integer.valueOf(strcolumn2) + 1)) + " - " + strcolumn3);
    parameters.put("col4", String.valueOf((Integer.valueOf(strcolumn3) + 1)) + " - " + strcolumn4);
    parameters.put("col5", ">" + strcolumn4);
    renderJR(vars, response, strReportName, "pdf", parameters, data, null);

  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strisReceipt, String strcolumn1, String strcolumn2, String strcolumn3,
      String strcolumn4, String strcBpartnerId, String strOrgTrx, String strfirstPrint,
      final String strCurrencyId) throws IOException, ServletException {
    String localStrisReceipt = strisReceipt;
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String discard[] = { "sectionDocType" };
    XmlDocument xmlDocument = null;
    ReportAgingBalanceData[] data = null;

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strTreeOrg = TreeData.getTreeOrg(readOnlyCP, vars.getClient());
    String strOrgFamily = getFamily(strTreeOrg, strOrgTrx);

    if (StringUtils.isEmpty(localStrisReceipt)) {
      localStrisReceipt = "N";
    }

    if (vars.commandIn("FIND")) {
      try {
        data = ReportAgingBalanceData.select(readOnlyCP, vars.getLanguage(), strOrgTrx,
            strCurrencyId, strcolumn1, strcolumn2, strcolumn3, strcolumn4, localStrisReceipt,
            strcBpartnerId, strOrgFamily,
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportAgingBalance"),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportAgingBalance"));
      } catch (final ServletException ex) {
        final OBError message = new OBError();
        message.setType("Error");
        message
            .setTitle(Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()));
        message.setMessage(
            Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage())
                .getMessage());
        vars.setMessage("ReportAgingBalance", message);
      }
    }
    if (strfirstPrint == "Y" || data == null || data.length == 0) {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportAgingBalance", discard)
          .createXmlDocument();
      data = ReportAgingBalanceData.set();
    } else {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportAgingBalance")
          .createXmlDocument();
    }

    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportAgingBalance", false, "",
        "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();

    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportAgingBalance");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportAgingBalance.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(), "ReportAgingBalance.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportAgingBalance");
      vars.removeMessage("ReportAgingBalance");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("receipt", localStrisReceipt);
    xmlDocument.setParameter("payable", localStrisReceipt);
    xmlDocument.setParameter("column1", strcolumn1);
    xmlDocument.setParameter("column2", strcolumn2);
    xmlDocument.setParameter("column3", strcolumn3);
    xmlDocument.setParameter("column4", strcolumn4);
    xmlDocument.setParameter("paramAD_ORG_Id", strOrgTrx);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_ORG_ID",
          "", "", Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportAgingBalanceData"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportAgingBalanceData"), '*');
      comboTableData.fillParameters(null, "ReportAgingBalanceData", strOrgTrx);
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportProductionCost"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProductionCost"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportProductionCost",
          strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("titleColumn1", "0 - " + strcolumn1);
    Integer iAux = Integer.valueOf(strcolumn1).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("titleColumn2", iAux.toString() + " - " + strcolumn2);
    iAux = Integer.valueOf(strcolumn2).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("titleColumn3", iAux.toString() + " - " + strcolumn3);
    iAux = Integer.valueOf(strcolumn3).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("titleColumn4", iAux.toString() + " - " + strcolumn4);
    xmlDocument.setParameter("titleColumn5", "&gt;" + strcolumn4);

    xmlDocument.setParameter("dateFromPrevious",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), "-1"));
    xmlDocument.setParameter("dateFromCol1", DateTimeData.today(readOnlyCP));
    xmlDocument.setParameter("dateToCol1",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), strcolumn1));
    iAux = Integer.valueOf(strcolumn1).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("dateFromCol2",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), iAux.toString()));
    xmlDocument.setParameter("dateToCol2",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), strcolumn2));
    iAux = Integer.valueOf(strcolumn2).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("dateFromCol3",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), iAux.toString()));
    xmlDocument.setParameter("dateToCol3",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), strcolumn3));
    iAux = Integer.valueOf(strcolumn3).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("dateFromCol4",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), iAux.toString()));
    xmlDocument.setParameter("dateToCol4",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), strcolumn4));
    iAux = Integer.valueOf(strcolumn4).intValue() + Integer.valueOf("1").intValue();
    xmlDocument.setParameter("dateFromCol5",
        DateTimeData.nDaysAfter(readOnlyCP, DateTimeData.today(readOnlyCP), iAux.toString()));
    xmlDocument.setParameter("dateToCol5", "");

    xmlDocument.setData("reportCBPartnerId_IN", "liststructure",
        SelectorUtilityData.selectBpartner(readOnlyCP,
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
            Utility.getContext(readOnlyCP, vars, "#User_Client", ""), strcBpartnerId));
    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }

  private String getFamily(String strTree, String strChild) throws IOException, ServletException {
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    return Tree.getMembers(readOnlyCP, strTree, strChild);
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportAgingBalance. This Servlet was made by David Alsasua";
  }
}
