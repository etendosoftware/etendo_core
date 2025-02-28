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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportProjectProfitabilityJR extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT")) {
      String strOrg = vars.getGlobalVariable("inpOrg", "ReportProjectProfitabilityJR|Org",
          vars.getOrg());
      String strProject = vars.getGlobalVariable("inpcProjectId",
          "ReportProjectProfitabilityJR|Project", "");
      String strProjectType = vars.getGlobalVariable("inpProjectType",
          "ReportProjectProfitabilityJR|ProjectType", "");
      String strResponsible = vars.getGlobalVariable("inpResponsible",
          "ReportProjectProfitabilityJR|Responsible", "");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom",
          "ReportProjectProfitabilityJR|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "ReportProjectProfitabilityJR|DateTo",
          "");
      String strDateFrom2 = vars.getGlobalVariable("inpDateFrom2",
          "ReportProjectProfitabilityJR|DateFrom2", "");
      String strDateTo2 = vars.getGlobalVariable("inpDateTo2",
          "ReportProjectProfitabilityJR|DateTo2", "");
      String strDateFrom3 = vars.getGlobalVariable("inpDateFrom3",
          "ReportProjectProfitabilityJR|DateFrom3", "");
      String strDateTo3 = vars.getGlobalVariable("inpDateTo3",
          "ReportProjectProfitabilityJR|DateTo3", "");
      String strExpand = vars.getGlobalVariable("inpExpand", "ReportProjectProfitabilityJR|Expand",
          "Y");
      String strPartner = vars.getGlobalVariable("inpcBPartnerId",
          "ReportProjectProfitabilityJR|Partner", "");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportProjectProfitabilityJR|currency", strUserCurrencyId);
      printPageDataSheet(response, vars, strOrg, strProject, strProjectType, strResponsible,
          strDateFrom, strDateTo, strExpand, strPartner, strDateFrom2, strDateTo2, strDateFrom3,
          strDateTo3, strCurrencyId);
    } else if (vars.commandIn("FIND") || vars.commandIn("PDF")) {
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportProjectProfitabilityJR|Org");
      String strProject = vars.getRequestGlobalVariable("inpcProjectId",
          "ReportProjectProfitabilityJR|Project");
      String strProjectType = vars.getRequestGlobalVariable("inpProjectType",
          "ReportProjectProfitabilityJR|ProjectType");
      String strResponsible = vars.getRequestGlobalVariable("inpResponsible",
          "ReportProjectProfitabilityJR|Responsible");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "ReportProjectProfitabilityJR|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "ReportProjectProfitabilityJR|DateTo");
      String strDateFrom2 = vars.getRequestGlobalVariable("inpDateFrom2",
          "ReportProjectProfitabilityJR|DateFrom2");
      String strDateTo2 = vars.getRequestGlobalVariable("inpDateTo2",
          "ReportProjectProfitabilityJR|DateTo2");
      String strDateFrom3 = vars.getRequestGlobalVariable("inpDateFrom3",
          "ReportProjectProfitabilityJR|DateFrom3");
      String strDateTo3 = vars.getRequestGlobalVariable("inpDateTo3",
          "ReportProjectProfitabilityJR|DateTo3");
      String strExpand = vars.getRequestGlobalVariable("inpExpand",
          "ReportProjectProfitabilityJR|Expand");
      String strPartner = vars.getRequestGlobalVariable("inpcBPartnerId",
          "ReportProjectProfitabilityJR|Partner");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportProjectProfitabilityJR|currency", strUserCurrencyId);
      String strOutput = "html";
      if (vars.commandIn("PDF")) {
        strOutput = "pdf";
      }
      printPageDataHtml(request, response, vars, strOrg, strProject, strProjectType, strResponsible,
          strDateFrom, strDateTo, strExpand, strPartner, strDateFrom2, strDateTo2, strDateFrom3,
          strDateTo3, strOutput, strCurrencyId);
    } else {
      pageError(response);
    }
  }

  private void printPageDataHtml(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strOrg, String strProject, String strProjectType,
      String strResponsible, String strDateFrom, String strDateTo, String strExpand,
      String strPartner, String strDateFrom2, String strDateTo2, String strDateFrom3,
      String strDateTo3, String strOutput, String strCurrencyId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }

    String discard[] = { "discard" };
    String strTreeOrg = "'" + strOrg + "'";
    if (StringUtils.equals(strExpand, "Y")) {
      strTreeOrg += treeOrg(vars, strOrg);
    }
    ReportProjectProfitabilityData[] data = null;

    // Checks if there is a conversion rate for each of the transactions of
    // the report
    OBError myMessage = new OBError();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strBaseCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    try {
      List<UOM> uomList = noConversionToHours(strProject, OBDateUtils.getDate(strDateFrom2),
          OBDateUtils.getDate(strDateTo2), strOrg, strProjectType, strResponsible,
          OBDateUtils.getDate(strDateFrom), OBDateUtils.getDate(strDateTo), strPartner,
          OBDateUtils.getDate(strDateFrom3), OBDateUtils.getDate(strDateTo3));
      if (!uomList.isEmpty()) {
        String uomsString = getUOMsString(uomList);
        advisePopUp(request, response, "ERROR",
            Utility.messageBD(readOnlyCP, "Error", vars.getLanguage()),
            Utility.messageBD(readOnlyCP, "NoConversionHourUOM", vars.getLanguage()) + uomsString);
      }
      data = ReportProjectProfitabilityData.select(readOnlyCP, strCurrencyId, strBaseCurrencyId,
          strDateFrom2, DateTimeData.nDaysAfter(readOnlyCP, strDateTo2, "1"), strTreeOrg,
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"),
          strDateFrom, DateTimeData.nDaysAfter(readOnlyCP, strDateTo, "1"), strDateFrom3,
          DateTimeData.nDaysAfter(readOnlyCP, strDateTo3, "1"), strProjectType, strProject,
          strResponsible, strPartner);
    } catch (ServletException ex) {
      myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
    } catch (Exception ex) {
    }
    String strConvRateErrorMsg = myMessage.getMessage();

    // If a conversion rate is missing for a certain transaction, an error
    // message window pops-up.
    if (StringUtils.isNotEmpty(strConvRateErrorMsg)) {
      advisePopUp(request, response, "ERROR",
          Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()),
          strConvRateErrorMsg);
    } else { // Otherwise, the report is launched
      if (data == null || data.length == 0) {
        data = ReportProjectProfitabilityData.set();
        discard[0] = "discardAll";
      }

      String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportProjectProfitabilityJR.jrxml";
      if (StringUtils.equals(strOutput, "pdf")) {
        response.setHeader("Content-disposition",
            "inline; filename=ReportProjectProfitabilityJR.pdf");
      }
      HashMap<String, Object> parameters = new HashMap<String, Object>();

      renderJR(vars, response, strReportName, strOutput, parameters, data, null);
    }
  }

  private String getUOMsString(List<UOM> uomList) {
    String uomsString = " ";
    for (UOM uom : uomList) {
      uomsString = uomsString + uom.getIdentifier() + ", ";
    }
    uomsString = uomsString.substring(0, uomsString.length() - 2);
    return uomsString;
  }

  private List<UOM> noConversionToHours(String strProject, Date dateFrom, Date dateTo,
      String strOrg, String strProjectType, String strResponsible, Date dateFromContract,
      Date dateToContract, String strPartner, Date dateFromStarting, Date dateToStarting) {
    String hsqlScript = "";
    final Map<String, Object> parameters = new HashMap<>();

    // @formatter:off
    hsqlScript += " as unitofmeasure";
    hsqlScript += " where exists (";
    hsqlScript += "   select 1 ";
    hsqlScript += "   from TimeAndExpenseSheetLine as tel ";
    hsqlScript += "        join tel.expenseSheet as es ";
    hsqlScript += "        left outer join tel.project as p";
    hsqlScript += "   where tel.timeSheet = true ";
    hsqlScript += "         and tel.uOM.id = unitofmeasure.id ";
    hsqlScript += "         and tel.uOM.id <> '101' ";
    hsqlScript += "         and es.processed = 'Y' ";
    if (StringUtils.isNotEmpty(strProject)) {
      hsqlScript += "    and p.id = :projectId";
      parameters.put("projectId", strProject);
    }
    if (StringUtils.isNotEmpty(strOrg)) {
      Set<String> orgIds = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getChildTree(strOrg, true);
      hsqlScript += "    and p.organization.id in (:orgIds)";
      parameters.put("orgIds", orgIds);
    }
    if (StringUtils.isNotEmpty(strProjectType)) {
      hsqlScript += "    and p.projectType.id = :projectTypeId";
      parameters.put("projectTypeId", strProjectType);
    }
    if (StringUtils.isNotEmpty(strResponsible)) {
      hsqlScript += "    and p.personInCharge.id = :responsible";
      parameters.put("responsible", strResponsible);
    }
    if (StringUtils.isNotEmpty(strPartner)) {
      hsqlScript += "    and p.businessPartner.id = :bpId";
      parameters.put("bpId", strPartner);
    }
    if (dateFrom != null) {
      hsqlScript += "    and es.reportDate >= :dateFrom";
      parameters.put("dateFrom", dateFrom);
    }
    if (dateTo != null) {
      hsqlScript += "    and es.reportDate <= :dateTo";
      parameters.put("dateTo", dateTo);
    }
    if (dateFromStarting != null) {
      hsqlScript += "    and p.startingDate >= :dateFromStarting";
      parameters.put("dateFromStarting", dateFromStarting);
    }
    if (dateToStarting != null) {
      hsqlScript += "    and p.startingDate < :dateToStarting";
      parameters.put("dateToStarting", dateToStarting);
    }
    if (dateFromContract != null) {
      hsqlScript += "    and p.contractDate >= :dateFromContract";
      parameters.put("dateFromContract", dateFromContract);
    }
    if (dateToContract != null) {
      hsqlScript += "    and p.contractDate < :dateToContract";
      parameters.put("dateToContract", dateToContract);
    }
    hsqlScript += " )";
    hsqlScript += " and not exists (";
    hsqlScript += "   select 1 ";
    hsqlScript += "   from UOMConversion as uomconv ";
    hsqlScript += "   where uomconv.uOM.id =  unitofmeasure.id ";
    hsqlScript += "         and uomconv.toUOM.id =  '101' ";
    hsqlScript += " )";
    // @formatter:on

    return OBDal.getReadOnlyInstance()
        .createQuery(UOM.class, hsqlScript)
        .setNamedParameters(parameters)
        .list();
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strOrg, String strProject, String strProjectType, String strResponsible,
      String strDateFrom, String strDateTo, String strExpand, String strPartner,
      String strDateFrom2, String strDateTo2, String strDateFrom3, String strDateTo3,
      String strCurrencyId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportProjectProfitabilityJR")
        .createXmlDocument();

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportProjectProfitabilityJR",
        false, "", "", "", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportProjectProfitabilityJR");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportProjectProfitabilityJR.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportProjectProfitabilityJR.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ReportProjectProfitabilityJR");
      vars.removeMessage("ReportProjectProfitabilityJR");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");

    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFrom2", strDateFrom2);
    xmlDocument.setParameter("dateFromdisplayFormat2", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat2", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo2", strDateTo2);
    xmlDocument.setParameter("dateTodisplayFormat2", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat2", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFrom3", strDateFrom3);
    xmlDocument.setParameter("dateFromdisplayFormat3", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat3", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTo3", strDateTo3);
    xmlDocument.setParameter("dateTodisplayFormat3", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat3", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("orgid", strOrg);
    xmlDocument.setParameter("project", strProject);
    xmlDocument.setParameter("projecttype", strProjectType);
    xmlDocument.setParameter("responsible", strResponsible);
    xmlDocument.setParameter("partnerid", strPartner);
    xmlDocument.setParameter("expand", strExpand);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE",
          "Responsible_ID", "Responsible employee", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportProjectProfitabilityJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportProjectProfitabilityJR", strResponsible);
      xmlDocument.setData("reportResponsible", "liststructure", comboTableData.select(false));

      comboTableData = null;
      comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportProjectProfitabilityJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportProjectProfitabilityJR", strOrg);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;

      comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "C_Project_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportProjectProfitabilityJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportProjectProfitabilityJR", strProject);
      xmlDocument.setData("reportC_Project_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;

      comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "C_ProjectType_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportProjectProfitabilityJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportProjectProfitabilityJR", strProjectType);
      xmlDocument.setData("reportC_ProjectType_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;

    } catch (Exception e) {
      throw new ServletException(e);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree",
              "ReportProjectProfitabilityJR"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportProjectProfitabilityJR"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData,
          "ReportProjectProfitabilityJR", strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportProjectProfitabilityJR. This Servlet was made by Pablo Sarobe";
  }

  private String treeOrg(VariablesSecureApp vars, String strOrg) throws ServletException {
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    ReportProjectProfitabilityData[] dataOrg = ReportProjectProfitabilityData.selectOrg(readOnlyCP,
        strOrg, vars.getClient());
    String strTreeOrg = "";
    for (int i = 0; i < dataOrg.length; i++) {
      strTreeOrg += "," + "'" + dataOrg[i].nodeId + "'";
      if (StringUtils.equals(dataOrg[i].issummary, "Y")) {
        strTreeOrg += treeOrg(vars, dataOrg[i].nodeId);
      }
    }
    return strTreeOrg;
  }
}
