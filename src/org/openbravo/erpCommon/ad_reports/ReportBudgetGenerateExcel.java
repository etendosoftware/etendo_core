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

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
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

public class ReportBudgetGenerateExcel extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      printPageDataSheet(response, vars);
    }

    else if (vars.commandIn("EXCEL")) {
      vars.removeSessionValue("ReportBudgetGenerateExcel|inpTabId");
      String strBPartner = vars.getRequestInGlobalVariable("inpcBPartnerId_IN",
          "ReportBudgetGenerateExcel|inpcBPartnerId_IN", IsIDFilter.instance);
      String strBPGroup = vars.getRequestInGlobalVariable("inpcBPGroupID",
          "ReportBudgetGenerateExcel|inpcBPGroupID", IsIDFilter.instance);
      String strProduct = vars.getRequestInGlobalVariable("inpmProductId_IN",
          "ReportBudgetGenerateExcel|inpmProductId_IN", IsIDFilter.instance);
      String strProdCategory = vars.getRequestInGlobalVariable("inpmProductCategoryId",
          "ReportBudgetGenerateExcel|inpmProductCategoryId", IsIDFilter.instance);
      String strUser1 = vars.getRequestInGlobalVariable("inpUser1Id",
          "ReportBudgetGenerateExcel|inpUser1Id", IsIDFilter.instance);
      String strUser2 = vars.getRequestInGlobalVariable("inpUser2Id",
          "ReportBudgetGenerateExcel|inpcSalesRegionId", IsIDFilter.instance);
      String strCostcenter = vars.getRequestInGlobalVariable("inpcCostcenterId",
          "ReportBudgetGenerateExcel|inpcCostcenterId", IsIDFilter.instance);
      String strSalesRegion = vars.getRequestInGlobalVariable("inpcSalesRegionId",
          "ReportBudgetGenerateExcel|inpcSalesRegionId", IsIDFilter.instance);
      String strCampaign = vars.getRequestInGlobalVariable("inpcCampaingId",
          "ReportBudgetGenerateExcel|inpcCampaingId", IsIDFilter.instance);
      String strActivity = vars.getRequestInGlobalVariable("inpcActivityId",
          "ReportBudgetGenerateExcel|inpcActivityId", IsIDFilter.instance);
      String strProject = vars.getRequestInGlobalVariable("inpcProjectId",
          "ReportBudgetGenerateExcel|inpcProjectId", IsIDFilter.instance);
      String strTrxOrg = vars.getRequestInGlobalVariable("inpTrxOrg",
          "ReportBudgetGenerateExcel|inpTrxOrg", IsIDFilter.instance);
      String strMonth = vars.getRequestInGlobalVariable("inpMonth",
          "ReportBudgetGenerateExcel|inpMonthId", IsIDFilter.instance);
      String strAccount = vars.getRequestGlobalVariable("paramAccountSelect",
          "ReportBudgetGenerateExcel|cAccountId", IsIDFilter.instance);
      String strcAcctSchemaId = vars.getStringParameter("inpcAcctSchemaId", "",
          IsIDFilter.instance);

      printPageDataExcel(response, vars, strBPartner, strBPGroup, strProduct, strProdCategory,
          strUser1, strUser2, strCostcenter, strSalesRegion, strCampaign, strActivity, strProject,
          strTrxOrg, strMonth, strcAcctSchemaId, strAccount);
    }

    else {
      pageErrorPopUp(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    XmlDocument xmlDocument = null;
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportBudgetGenerateExcel")
        .createXmlDocument();

    // Use ReadOnly Connection Provider
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportBudgetGenerateExcel",
        false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportBudgetGenerateExcel");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportBudgetGenerateExcel.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportBudgetGenerateExcel.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportBudgetGenerateExcel");
      vars.removeMessage("ReportBudgetGenerateExcel");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));

    // When showing window, field "accounts" is empty
    xmlDocument.setData("cAccount", "liststructure", ReportBudgetGenerateExcelData.set());
    xmlDocument.setParameter("accounts", Utility.arrayDobleEntrada("arrAccounts",
        ReportBudgetGenerateExcelData.selectAccounts(readOnlyCP, vars.getLanguage(),
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"))));
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_ACCTSCHEMA_ID", "", "C_AcctSchema validation",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportC_ACCTSCHEMA_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_BP_Group_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCBPGroupId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_Product_Category_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportM_PRODUCTCATEGORY", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "User1_ID",
          "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportUser1", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "User2_ID",
          "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportUser2", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Costcenter_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCCostcenterId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_SalesRegion_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCSalesRegionId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Campaign_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCCampaignId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Activity_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCActivityId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Project_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportCProjectId", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID",
          "", "", Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportBudgetGenerateExcel"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportBudgetGenerateExcel"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportBudgetGenerateExcel",
          "");
      xmlDocument.setData("reportTrxOrg", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setData("reportMonth", "liststructure",
        ReportBudgetGenerateExcelData.selectMonth(readOnlyCP));
    // added by gro 03/06/2007
    OBError myMessage = vars.getMessage("ReportBudgetGenerateExcel");
    vars.removeMessage("ReportBudgetGenerateExcel");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageDataExcel(HttpServletResponse response, VariablesSecureApp vars,
      String strBPartner, String strBPGroup, String strProduct, String strProdCategory,
      String strUser1, String strUser2, String strCostcenter, String strSalesRegion,
      String strCampaign, String strActivity, String strProject, String strTrxOrg, String strMonth,
      String strcAcctSchemaId, String strAccount) throws IOException, ServletException {

    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: EXCEL");
    }
    StringBuffer columns = new StringBuffer();
    StringBuffer tables = new StringBuffer();

    if (StringUtils.isNotEmpty(strBPartner)) {
      columns.append("PARTNER, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_BPARTNER', TO_CHAR(C_BPARTNER_ID), '")
          .append(vars.getLanguage())
          .append("') AS PARTNER, C_BPARTNER_ID FROM C_BPARTNER WHERE C_BPARTNER_ID IN ")
          .append(strBPartner)
          .append(") BP");
    } else {
      columns.append("' ' AS PARTNER, ");
    }
    if (StringUtils.isNotEmpty(strBPGroup)) {
      columns.append("PARTNERGROUP, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_BP_GROUP', TO_CHAR(C_BP_GROUP_ID), '")
          .append(vars.getLanguage())
          .append("') AS PARTNERGROUP FROM C_BP_GROUP WHERE C_BP_GROUP_ID IN ")
          .append(strBPGroup)
          .append(") PG");
    } else {
      columns.append("' ' AS PARTNERGROUP, ");
    }
    if (StringUtils.isNotEmpty(strProduct)) {
      columns.append("PRODUCT, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('M_PRODUCT', TO_CHAR(M_PRODUCT_ID), '")
          .append(vars.getLanguage())
          .append("') AS PRODUCT, M_PRODUCT_ID FROM M_PRODUCT WHERE M_PRODUCT_ID IN ")
          .append(strProduct)
          .append(") PROD");
    } else {
      columns.append("' ' AS PRODUCT, ");
    }
    if (StringUtils.isNotEmpty(strProdCategory)) {
      columns.append("PRODCATEGORY, ");
      tables.append(
          ", (SELECT AD_COLUMN_IDENTIFIER('M_PRODUCT_CATEGORY', TO_CHAR(M_PRODUCT_CATEGORY_ID), '")
          .append(vars.getLanguage())
          .append("') AS PRODCATEGORY FROM M_PRODUCT_CATEGORY WHERE M_PRODUCT_CATEGORY_ID IN ")
          .append(strProdCategory)
          .append(") PRODCAT");
    } else {
      columns.append("' ' AS PRODCATEGORY, ");
    }
    if (StringUtils.isNotEmpty(strUser1)) {
      columns.append("USER1, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('USER1', TO_CHAR(USER1_ID), '")
          .append(vars.getLanguage())
          .append("') AS USER1 FROM USER1 WHERE USER1_ID IN ")
          .append(strUser1)
          .append(") USER1");
    } else {
      columns.append("' ' AS USER1, ");
    }
    if (StringUtils.isNotEmpty(strUser2)) {
      columns.append("USER2, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('USER2', TO_CHAR(USER2_ID), '")
          .append(vars.getLanguage())
          .append("') AS USER2 FROM USER2 WHERE USER2_ID IN ")
          .append(strUser2)
          .append(") USER2");
    } else {
      columns.append("' ' AS USER2, ");
    }
    if (StringUtils.isNotEmpty(strCostcenter)) {
      columns.append("COSTCENTER, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_COSTCENTER', TO_CHAR(C_COSTCENTER_ID), '")
          .append(vars.getLanguage())
          .append("') AS COSTCENTER FROM C_COSTCENTER WHERE C_COSTCENTER_ID IN ")
          .append(strCostcenter)
          .append(") COSTCENTER");
    } else {
      columns.append("' ' AS COSTCENTER, ");
    }
    if (StringUtils.isNotEmpty(strSalesRegion)) {
      columns.append("SALESREGION, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_SALESREGION', TO_CHAR(C_SALESREGION_ID), '")
          .append(vars.getLanguage())
          .append("') AS SALESREGION FROM C_SALESREGION WHERE C_SALESREGION_ID IN ")
          .append(strSalesRegion)
          .append(") SALEREG");
    } else {
      columns.append("' ' AS SALESREGION, ");
    }
    if (StringUtils.isNotEmpty(strCampaign)) {
      columns.append("CAMPAIGN, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_CAMPAIGN', TO_CHAR(C_CAMPAIGN_ID), '")
          .append(vars.getLanguage())
          .append("') AS CAMPAIGN FROM C_CAMPAIGN WHERE C_CAMPAIGN_ID IN ")
          .append(strCampaign)
          .append(") CAMP");
    } else {
      columns.append("' ' AS CAMPAIGN, ");
    }
    if (StringUtils.isNotEmpty(strActivity)) {
      columns.append("ACTIVITY, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_ACTIVITY', TO_CHAR(C_ACTIVITY_ID), '")
          .append(vars.getLanguage())
          .append("') AS ACTIVITY FROM C_ACTIVITY WHERE C_ACTIVITY_ID IN ")
          .append(strActivity)
          .append(") ACT");
    } else {
      columns.append("' ' AS ACTIVITY, ");
    }
    if (StringUtils.isNotEmpty(strProject)) {
      columns.append("PROJECT, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_PROJECT', TO_CHAR(C_PROJECT_ID), '")
          .append(vars.getLanguage())
          .append("') AS PROJECT FROM C_PROJECT WHERE C_PROJECT_ID IN ")
          .append(strProject)
          .append(") PROJ");
    } else {
      columns.append("' ' AS PROJECT, ");
    }
    if (StringUtils.isNotEmpty(strTrxOrg)) {
      columns.append("TRXORG, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('AD_ORG', TO_CHAR(AD_ORG_ID), '")
          .append(vars.getLanguage())
          .append("') AS TRXORG FROM AD_ORG WHERE AD_ORG_ID IN ")
          .append(strTrxOrg)
          .append(") TORG");
    } else {
      columns.append("' ' AS TRXORG, ");
    }
    if (StringUtils.isNotEmpty(strMonth)) {
      columns.append("MONTH, ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('AD_MONTH', TO_CHAR(AD_MONTH_ID), '")
          .append(vars.getLanguage())
          .append("') AS MONTH FROM AD_MONTH WHERE  AD_MONTH_ID IN ")
          .append(strMonth)
          .append(") MTH");
    } else {
      columns.append("' ' AS MONTH, ");
    }
    // Although is called Valid Combination, it refers to accounts
    // (c_elementvalue)
    if (StringUtils.isNotEmpty(strAccount)) {
      columns.append("VALIDCOMBINATION, ");
      tables
          .append(", (SELECT AD_COLUMN_IDENTIFIER('C_ELEMENTVALUE', TO_CHAR(C_ELEMENTVALUE_ID), '")
          .append(vars.getLanguage())
          .append("' ) AS VALIDCOMBINATION FROM C_ELEMENTVALUE WHERE C_ELEMENTVALUE_ID = '")
          .append(strAccount)
          .append("') VCOMB");
    } else {
      columns.append("' ' AS VALIDCOMBINATION, ");
    }
    if (StringUtils.isNotEmpty(strcAcctSchemaId)) {
      columns.append("ACCOUNTSCHEMA, CURRENCY ");
      tables.append(", (SELECT AD_COLUMN_IDENTIFIER('C_ACCTSCHEMA', TO_CHAR(C_ACCTSCHEMA_ID), '")
          .append(vars.getLanguage())
          .append(
              "' ) AS ACCOUNTSCHEMA, ISO_CODE AS CURRENCY FROM C_ACCTSCHEMA, C_CURRENCY WHERE C_ACCTSCHEMA.C_CURRENCY_ID=C_CURRENCY.C_CURRENCY_ID AND C_ACCTSCHEMA_ID = '")
          .append(strcAcctSchemaId)
          .append("') ACSCH");
    } else {
      columns.append("' ' AS ACCOUNTSCHEMA, CURRENCY");
      tables.append(", (SELECT ISO_CODE AS CURRENCY FROM C_CURRENCY WHERE C_CURRENCY_ID = '")
          .append(vars.getSessionValue("$C_CURRENCY_ID"))
          .append("') CUR");
    }

    response.setContentType("application/xls; charset=UTF-8");
    PrintWriter out = response.getWriter();

    // Use ReadOnly Connection Provider
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ReportBudgetGenerateExcelData[] data = ReportBudgetGenerateExcelData.select(readOnlyCP,
        columns.toString(), tables.toString());

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportBudgetGenerateExcelExportXLS")
        .createXmlDocument();

    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");

    xmlDocument.setData("structure1", data);
    out.println(xmlDocument.print());
    out.close();
  }
}
