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
 * All portions are Copyright (C) 2001-2020 Openbravo SLU 
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_reports;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.application.report.ReportingUtils;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.JRFieldProviderDataSource;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBCurrencyUtils;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostingAlgorithm;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperReport;

public class ReportValuationStock extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final String COST_TEMPLATE = "org/openbravo/erpCommon/ad_reports/ReportValuationStock";
  private static final String NO_COST_TEMPLATE = "org/openbravo/erpCommon/ad_reports/ReportValuationStock2";

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    // Get user Client's base currency
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT", "RELATION")) {
      String strDate = vars.getGlobalVariable("inpDate", "ReportValuationStock|Date",
          DateTimeData.today(readOnlyCP));
      String strOrganization = vars.getGlobalVariable("inpOrg", "ReportValuationStock|Organization",
          "", IsIDFilter.instance);
      String strWarehouse = vars.getGlobalVariable("inpmWarehouseId",
          "ReportValuationStock|Warehouse", "", IsIDFilter.instance);
      String strCategoryProduct = vars.getGlobalVariable("inpCategoryProduct",
          "ReportValuationStock|CategoryProduct", "", IsIDFilter.instance);
      String strCurrencyId = OBCurrencyUtils.getOrgCurrency(vars.getOrg());
      if (StringUtils.isEmpty(strCurrencyId)) {
        strCurrencyId = strUserCurrencyId;
      }
      String strWarehouseConsolidation = vars.getGlobalVariable("inpWarehouseConsolidation",
          "ReportValuationStock|warehouseConsolidation", "");
      boolean isWarehouseConsolidation = StringUtils.equals(strWarehouseConsolidation, "on");

      printPageDataSheet(response, vars, strDate, strOrganization, strWarehouse, strCategoryProduct,
          strCurrencyId, isWarehouseConsolidation, null, null, null, null);
    } else if (vars.commandIn("FIND", "PDF", "XLS")) {
      String strDate = vars.getGlobalVariable("inpDate", "ReportValuationStock|Date",
          DateTimeData.today(readOnlyCP));
      String strOrganization = vars.getRequestGlobalVariable("inpOrg",
          "ReportValuationStock|Organization", IsIDFilter.instance);
      String strWarehouse = vars.getRequestGlobalVariable("inpmWarehouseId",
          "ReportValuationStock|Warehouse", IsIDFilter.instance);
      String strCategoryProduct = vars.getRequestGlobalVariable("inpCategoryProduct",
          "ReportValuationStock|CategoryProduct", IsIDFilter.instance);
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId",
          "ReportValuationStock|currency", strUserCurrencyId, IsIDFilter.instance);
      String strWarehouseConsolidation = vars.getRequestGlobalVariable("inpWarehouseConsolidation",
          "ReportValuationStock|warehouseConsolidation");
      boolean isWarehouseConsolidation = StringUtils.equals(strWarehouseConsolidation, "on");

      buildData(response, vars, strDate, strOrganization, strWarehouse, strCategoryProduct,
          strCurrencyId, isWarehouseConsolidation);
    } else if (vars.commandIn("CURRENCY")) {
      String strOrg = vars.getRequestGlobalVariable("inpOrg", "ReportValuationStock|Organization",
          IsIDFilter.instance);
      if (StringUtils.isEmpty(strOrg)) {
        strOrg = vars.getOrg();
      }
      String strCurrencyId = OBCurrencyUtils.getOrgCurrency(strOrg);
      response.setContentType("text/html; charset=UTF-8");
      PrintWriter out = response.getWriter();
      out.print(strCurrencyId);
      out.close();
    } else {
      pageError(response);
    }
  }

  private void buildData(HttpServletResponse response, VariablesSecureApp vars, String strDate,
      String strOrganization, String strWarehouse, String strCategoryProduct, String strCurrencyId,
      boolean isWarehouseConsolidation) throws IOException, ServletException {
    ReportValuationStockData[] data = null;

    CostingAlgorithm ca = null;
    String processTime = null;
    // By default, Warehouse Dimension is never enabled
    String warehouseDimension = "N";
    String strCostType = null;
    Set<String> orgs = null;
    Set<String> warehouseIds = null;
    String strWarehouseIncluded = null;
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    try {
      Organization filterOrg = OBDal.getReadOnlyInstance().get(Organization.class, strOrganization);
      OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId());
      Organization legalEntity = osp.getLegalEntity(filterOrg);
      String strLegalEntity = null;
      if (legalEntity == null) {
        throw new OBException(OBMessageUtils.messageBD("WarehouseNotInLE"));
      } else {
        strLegalEntity = legalEntity.getId();
      }
      orgs = osp.getNaturalTree(strLegalEntity);
      String orgIds = Utility.getInStrSet(orgs);

      CostingRule costRule = getLEsCostingAlgortithm(legalEntity);
      if (costRule != null) {
        ca = costRule.getCostingAlgorithm();
        processTime = OBDateUtils.formatDate(CostingUtils.getCostingRuleStartingDate(costRule),
            "dd-MM-yyyy HH:mm:ss");
        warehouseDimension = costRule.isWarehouseDimension() ? "Y" : "N";
      }
      String compare = DateTimeData.compare(readOnlyCP, strDate, ReportValuationStockData
          .getCostingMigrationDate(readOnlyCP, legalEntity.getClient().getId()));
      String strCostOrg = "";
      String strCostClientId = "";
      if (StringUtils.equals(compare, "-1")) {
        // Date is before migration, available types are ST and AV. These costs are created at
        // client level and organization can be any that belong to the client.
        strCostType = "'AV', 'ST'";
        strCostClientId = OBContext.getOBContext().getCurrentClient().getId();
        orgs = osp.getChildTree("0", true);
      } else if (ca != null) {
        strCostType = getCostType(ca);
        strCostOrg = strLegalEntity;
      }
      String strDateNext = DateTimeData.nDaysAfter(readOnlyCP, strDate, "1");
      String strMaxAggDate = ReportValuationStockData.selectMaxAggregatedDate(readOnlyCP,
          OBDal.getReadOnlyInstance().get(Organization.class, strLegalEntity).getClient().getId(),
          strDateNext, orgIds);
      if (StringUtils.isEmpty(strMaxAggDate)) {
        DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        Date maxAggDate = formatter.parse("01-01-0000");
        strMaxAggDate = OBDateUtils.formatDate(maxAggDate);
      }
      String dateFormat = "DD-MM-YYYY HH24:MI:SS";
      warehouseIds = new HashSet<String>();
      if (StringUtils.isEmpty(strWarehouse)) {
        warehouseIds.addAll(getWarehouses(vars.getClient(), strOrganization));
      } else {
        warehouseIds.add(strWarehouse);
      }
      if (!warehouseIds.isEmpty()) {
        strWarehouseIncluded = "(" + Utility.getInStrSet(warehouseIds) + ")";
      }
      if (strCostType != null && !isWarehouseConsolidation) {
        data = ReportValuationStockData.select(readOnlyCP, vars.getLanguage(), strCurrencyId,
            strLegalEntity, strDate, strDateNext, strMaxAggDate, processTime, dateFormat, orgIds,
            strWarehouseIncluded, strCostOrg, strCostClientId, strCostType, warehouseDimension,
            strCategoryProduct);
      } else if (strCostType == null && !isWarehouseConsolidation) {
        data = ReportValuationStockData.selectWithoutCost(readOnlyCP, vars.getLanguage(),
            strCurrencyId, strLegalEntity, strDateNext, strMaxAggDate, processTime, dateFormat,
            orgIds, strWarehouseIncluded, strCategoryProduct);
      } else if (strCostType != null && isWarehouseConsolidation) {
        data = ReportValuationStockData.selectClusteredByWarehouse(readOnlyCP, vars.getLanguage(),
            strDate, strLegalEntity, strCurrencyId, strDateNext, strMaxAggDate, processTime,
            dateFormat, orgIds, strWarehouseIncluded, strCategoryProduct, strCostOrg,
            strCostClientId, strCostType, warehouseDimension, filterOrg.getId());
      } else {
        data = ReportValuationStockData.selectClusteredByWarehouseWithoutCost(readOnlyCP,
            vars.getLanguage(), strCurrencyId, strLegalEntity, strDateNext, strMaxAggDate,
            processTime, dateFormat, orgIds, strWarehouseIncluded, strCategoryProduct);
      }

    } catch (Exception ex) {
      OBError myMessage = OBMessageUtils.translateError(ex.getMessage());
      vars.setMessage("ReportValuationStock", myMessage);
      if (vars.commandIn("FIND")) {
        data = null;
      } else {
        printPageClosePopUpAndRefreshParent(response, vars);
        return;
      }
    }

    if (vars.commandIn("FIND")) {
      printPageDataSheet(response, vars, strDate, strOrganization, strWarehouse, strCategoryProduct,
          strCurrencyId, isWarehouseConsolidation, data, strCostType, ca, orgs);
    } else if (vars.commandIn("PDF")) {
      printPageDataPDF(response, vars, strDate, data, strCostType, ca);
    } else {
      printPageDataXLS(response, vars, data, strCostType, ca);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strDate, String strOrganization, String strWarehouse, String strCategoryProduct,
      String strCurrencyId, boolean isWarehouseConsolidation, ReportValuationStockData[] data,
      String strCostType, CostingAlgorithm ca, Set<String> orgs)
      throws IOException, ServletException {
    log4j.debug("Output: dataSheet");

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String discard[] = { "discard", "discard1" };
    XmlDocument xmlDocument;
    OBError myMessage = new OBError();
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();

    // Otherwise, the report is launched
    if (vars.commandIn("DEFAULT")) {
      discard[0] = "sectionCategoryProduct";
      discard[1] = "sectionWarehouse";
      xmlDocument = xmlEngine.readXmlTemplate(COST_TEMPLATE, discard).createXmlDocument();
    } else {
      if (data == null || data.length == 0) {
        discard[0] = "sectionCategoryProduct";
        discard[1] = "sectionWarehouse";
      } else {
        boolean hasTrxWithNoCost = hasTrxWithNoCost(strDate, orgs, strWarehouse,
            strCategoryProduct);
        if (hasTrxWithNoCost) {
          OBError warning = new OBError();
          warning.setType("Warning");
          warning.setTitle(OBMessageUtils.messageBD("Warning"));
          warning.setMessage(OBMessageUtils.messageBD("TrxWithNoCost"));
          vars.setMessage("ReportValuationStock", warning);
        }
      }
      if (strCostType != null) {
        xmlDocument = xmlEngine.readXmlTemplate(COST_TEMPLATE, discard).createXmlDocument();
      } else {
        // If there is no cost type it is not using a Costing Algorithm that makes use of M_Costing
        // table to calculate the current Average or Standard cost. It is used the html template
        // that does not show these fields.
        xmlDocument = xmlEngine.readXmlTemplate(NO_COST_TEMPLATE, discard).createXmlDocument();
      }
    }

    ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportValuationStock", true, "",
        "", "printReport('PDF');return false;", false, "ad_reports", strReplaceWith, false, true);
    toolbar.prepareRelationBarTemplate(false, false, "printReport('XLS');return false;");
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
          "org.openbravo.erpCommon.ad_reports.ReportValuationStock");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
          "ReportValuationStock.html", classInfo.id, classInfo.type, strReplaceWith,
          tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
          "ReportValuationStock.html", strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    myMessage = vars.getMessage("ReportValuationStock");
    vars.removeMessage("ReportValuationStock");
    if (myMessage != null) {
      xmlDocument.setParameter("messageType", myMessage.getType());
      xmlDocument.setParameter("messageTitle", myMessage.getTitle());
      xmlDocument.setParameter("messageMessage", myMessage.getMessage());
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("date", strDate);
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    xmlDocument.setParameter("mWarehouseId", strWarehouse);
    xmlDocument.setParameter("categoryProduct", strCategoryProduct);
    xmlDocument.setParameter("adOrgId", strOrganization);
    xmlDocument.setParameter("warehouseConsolidation", isWarehouseConsolidation ? "" : "on");

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR", "AD_Org_ID",
          "", "D4DF252DEC3B44858454EE5292A8B836",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportValuationStock"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportValuationStock"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportValuationStock",
          strOrganization);
      xmlDocument.setData("reportAD_ORGID", "liststructure", comboTableData.select(false));
      comboTableData = null;

    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLE",
          "M_Warehouse_ID", "M_Warehouse of Client", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
          Utility.getContext(readOnlyCP, vars, "#User_Client", ""), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "", "");
      xmlDocument.setData("reportM_WAREHOUSEID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    xmlDocument.setParameter("warehouses", Utility.arrayDobleEntrada("arrWh",
        ReportValuationStockData.selectWhsDouble(readOnlyCP, vars.getClient())));
    xmlDocument.setParameter("warehouseID", strWarehouse);
    xmlDocument.setParameter("whwh", strWarehouse);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "M_Product_Category_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", ""),
          Utility.getContext(readOnlyCP, vars, "#User_Client", ""), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "", strCategoryProduct);
      xmlDocument.setData("reportM_PRODUCT_CATEGORYID", "liststructure",
          comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("ccurrencyid", strCurrencyId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
          "C_Currency_ID", "", "",
          Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportValuationStock"),
          Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportValuationStock"), 0);
      Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportValuationStock",
          strCurrencyId);
      xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    if (data != null && data.length > 0) {
      if (strCostType != null && ca != null) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("algorithm", ca.getName());
        String strCostHeader = OBMessageUtils
            .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_CostHeader"), params);
        String strValuationHeader = OBMessageUtils.parseTranslation(
            OBMessageUtils.messageBD("ValuedStockReport_ValuationHeader"), params);
        xmlDocument.setParameter("costTypeCol", strCostHeader);
        xmlDocument.setParameter("valuationTypeCol", strValuationHeader);
      }
      xmlDocument.setData("structure1", data);
    }
    out.println(xmlDocument.print());
    out.close();
  }

  private void printPageDataXLS(HttpServletResponse response, VariablesSecureApp vars,
      ReportValuationStockData[] data, String strCostType, CostingAlgorithm ca)
      throws IOException, ServletException {
    log4j.debug("Output: XLS");
    response.setContentType("text/html; charset=UTF-8");

    if (data == null || data.length == 0) {
      OBError myMessage = new OBError();
      myMessage.setTitle(OBMessageUtils.messageBD("ProcessStatus-W"));
      myMessage.setMessage(OBMessageUtils.messageBD("NoDataFound"));
      myMessage.setType("WARNING");
      vars.setMessage("ReportValuationStock", myMessage);
      printPageClosePopUpAndRefreshParent(response, vars);
      return;
    }

    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportValuationStockExcel.jrxml";

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    String strCostHeader = "";
    String strValuationHeader = "";
    if (ca != null && strCostType != null) {
      Map<String, String> params = new HashMap<String, String>();
      params.put("algorithm", ca.getName());
      strCostHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_CostHeader"), params);
      strValuationHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_ValuationHeader"), params);
    }
    parameters.put("ALG_COST", strCostHeader);
    parameters.put("SUM_ALG_COST", strValuationHeader);
    parameters.put("COSTFORMAT", Utility.getFormat(vars, "generalQtyExcel"));

    renderJR(vars, response, strReportName, "xls", parameters, data, null);
  }

  private void printPageDataPDF(HttpServletResponse response, VariablesSecureApp vars,
      String strDate, ReportValuationStockData[] data, String strCostType, CostingAlgorithm ca)
      throws IOException, ServletException {
    log4j.debug("Output: PDF");
    response.setContentType("text/html; charset=UTF-8");

    if (data == null || data.length == 0) {
      OBError myMessage = new OBError();
      myMessage.setTitle(OBMessageUtils.messageBD("ProcessStatus-W"));
      myMessage.setMessage(OBMessageUtils.messageBD("NoDataFound"));
      myMessage.setType("WARNING");
      vars.setMessage("ReportValuationStock", myMessage);
      printPageClosePopUpAndRefreshParent(response, vars);
      return;
    }

    List<SummaryProductCategory> spcs = new ArrayList<SummaryProductCategory>();
    HashMap<String, SummaryProductCategory> totalByCategory = new HashMap<String, SummaryProductCategory>();
    for (int i = 0; i < data.length; i++) {
      ReportValuationStockData row = data[i];
      SummaryProductCategory spc = totalByCategory.get(row.categoryName);
      if (spc == null) {
        spc = new SummaryProductCategory(row.categoryName, BigDecimal.ZERO);
        totalByCategory.put(row.categoryName, spc);
        spcs.add(spc);
      }
      if (StringUtils.isNotBlank(row.totalCost)) {
        spc.addCost(new BigDecimal(row.totalCost));
      }

    }
    SummaryProductCategory[] datos = spcs.toArray(new SummaryProductCategory[0]);
    String strReportName = "@basedesign@/org/openbravo/erpCommon/ad_reports/ReportValuationStockPDF.jrxml";
    String strSubReportName = "/org/openbravo/erpCommon/ad_reports/SumaryProductCategory.jrxml";

    HashMap<String, Object> parameters = new HashMap<String, Object>();
    String strBaseDesign = getBaseDesignPath(vars.getLanguage());
    JasperReport summaryReport;
    try {
      summaryReport = ReportingUtils.compileReport(strBaseDesign + strSubReportName);
    } catch (JRException e) {
      throw new ServletException(e.getMessage(), e);
    }
    parameters.put("SUMMARY_REPORT", summaryReport);
    parameters.put("SUMMARY_DATASET",
        new JRFieldProviderDataSource(datos, vars.getJavaDateFormat()));
    String strCostHeader = "";
    String strValuationHeader = "";
    if (ca != null && strCostType != null) {
      Map<String, String> params = new HashMap<String, String>();
      params.put("algorithm", ca.getName());
      strCostHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_CostHeader"), params);
      strValuationHeader = OBMessageUtils
          .parseTranslation(OBMessageUtils.messageBD("ValuedStockReport_ValuationHeader"), params);
    }
    parameters.put("TITLE", classInfo.name);
    parameters.put("DATE", strDate);
    parameters.put("ALG_COST", strCostHeader);
    parameters.put("SUM_ALG_COST", strValuationHeader);
    parameters.put("COSTFORMAT", Utility.getFormat(vars, "generalQtyEdition"));

    renderJR(vars, response, strReportName, "pdf", parameters, data, null);
  }

  public static CostingRule getLEsCostingAlgortithm(Organization legalEntity) {
    try {
      OBContext.setAdminMode(true);

      //@formatter:off
      String hql =
              " as cosrule" +
              " where cosrule.organization.id = :orgId" +
              " and cosrule.validated = true" +
              " order by startingDate desc";
      //@formatter:on

      CostingRule cr = OBDal.getReadOnlyInstance()
          .createQuery(CostingRule.class, hql)
          .setNamedParameter("orgId", legalEntity.getId())
          .setMaxResult(1)
          .uniqueResult();
      if (cr == null) {
        return null;
      }
      return cr;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private String getCostType(CostingAlgorithm ca) throws ServletException {
    Class<?> algorithm;
    String strCostType = null;
    try {
      OBContext.setAdminMode(true);
      algorithm = Class.forName(ca.getJavaClassName());
      if (org.openbravo.costing.AverageAlgorithm.class.isAssignableFrom(algorithm)) {
        strCostType = "'AVA'";
      } else if (org.openbravo.costing.StandardAlgorithm.class.isAssignableFrom(algorithm)) {
        strCostType = "'STA'";
      }
    } catch (ClassNotFoundException e) {
      // The class name defined in the costing rule is not found.
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
    return strCostType;
  }

  private boolean hasTrxWithNoCost(String strDate, Set<String> orgs, String strWarehouse,
      String strCategoryProduct) {
    try {
      OBContext.setAdminMode(true);

      //@formatter:off
      String hql =
              " as trx" +
              " join trx.storageBin as loc" +
              " join trx.product as p" +
              " where trx.movementDate < :maxDate" +
              "   and trx.isCostCalculated = false" +
              "   and trx.organization.id in (:orgIds)";
      //@formatter:on
      if (StringUtils.isNotBlank(strWarehouse)) {
        //@formatter:off
        hql +=
              "   and loc.warehouse.id = :warehouseId";
        //@formatter:on
      }
      //@formatter:off
      hql +=
              "   and p.stocked = true";
      //@formatter:on
      if (StringUtils.isNotBlank(strCategoryProduct)) {
        //@formatter:off
        hql +=
              "   and p.productCategory.id = :prodCategoryId";
        //@formatter:on
      }
      //@formatter:on
      final OBQuery<MaterialTransaction> whereQry = OBDal.getReadOnlyInstance()
          .createQuery(MaterialTransaction.class, hql)
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false);
      try {
        ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
        whereQry.setNamedParameter("maxDate",
            OBDateUtils.getDate(DateTimeData.nDaysAfter(readOnlyCP, strDate, "1")));
      } catch (Exception e) {
        // DoNothing parse exception not expected.
        log4j.error("error parsing date: " + strDate, e);
      }
      whereQry.setNamedParameter("orgIds", orgs);
      if (StringUtils.isNotBlank(strWarehouse)) {
        whereQry.setNamedParameter("warehouseId", strWarehouse);
      }

      if (StringUtils.isNotBlank(strCategoryProduct)) {
        whereQry.setNamedParameter("prodCategoryId", strCategoryProduct);
      }
      whereQry.setMaxResult(1);
      return whereQry.uniqueResult() != null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<String> getWarehouses(String clientId, String orgId) {
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(clientId);
    //@formatter:off
    final String hql =
                  "select e.id " +
                  " from Warehouse as e" +
                  " where e.organization.id in (:orgIds)" +
                  " and e.client.id = :clientId";
    //@formatter:on

    return OBDal.getReadOnlyInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameterList("orgIds", osp.getNaturalTree(orgId))
        .setParameter("clientId", clientId)
        .list();
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportValuationStock. This Servlet was made by Pablo Sarobe";
  } // end of getServletInfo() method
}
