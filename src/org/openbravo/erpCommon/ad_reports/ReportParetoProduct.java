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
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBCurrencyUtils;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class ReportParetoProduct extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);
    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();

    // Get user Client's base currency
    String strUserCurrencyId = Utility.stringBaseCurrencyId(readOnlyCP, vars.getClient());
    if (vars.commandIn("DEFAULT")) {
      String strWarehouse = vars.getGlobalVariable("inpmWarehouseId",
          "ReportParetoProduct|M_Warehouse_ID", "");
      String strClient = vars.getClient();
      String strAD_Org_ID = vars.getGlobalVariable("inpadOrgId", "ReportParetoProduct|AD_Org_ID",
          "");
      String strCurrencyId = OBCurrencyUtils.getOrgCurrency(strAD_Org_ID);
      if (StringUtils.isEmpty(strCurrencyId)) {
        strCurrencyId = strUserCurrencyId;
      }

      printPageDataSheet(request, response, vars, strWarehouse, strAD_Org_ID, strClient,
          strCurrencyId);
    } else if (vars.commandIn("FIND")) {
      String strWarehouse = vars.getRequestGlobalVariable("inpmWarehouseId",
          "ReportParetoProduct|M_Warehouse_ID");
      String strClient = vars.getClient();
      String strAD_Org_ID = vars.getRequestGlobalVariable("inpadOrgId",
          "ReportParetoProduct|AD_Org_ID");
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId", "ReportParetoProduct|currency",
          strUserCurrencyId);
      printPageDataSheet(request, response, vars, strWarehouse, strAD_Org_ID, strClient,
          strCurrencyId);
    } else if (vars.commandIn("GENERATE")) {
      String strClient = vars.getClient();
      String strWarehouse = vars.getRequestGlobalVariable("inpmWarehouseId",
          "ReportParetoProduct|M_Warehouse_ID");
      String strAD_Org_ID = vars.getRequestGlobalVariable("inpadOrgId",
          "ReportParetoProduct|AD_Org_ID");
      OBError myMessage = mUpdateParetoProduct(vars, strWarehouse, strAD_Org_ID, strClient);
      myMessage.setTitle("");
      myMessage.setType("Success");
      myMessage.setTitle(Utility.messageBD(readOnlyCP, "Success", vars.getLanguage()));
      vars.setMessage("ReportParetoProduct", myMessage);
      String strCurrencyId = vars.getGlobalVariable("inpCurrencyId", "ReportParetoProduct|currency",
          strUserCurrencyId);
      printPageDataSheet(request, response, vars, strWarehouse, strAD_Org_ID, strClient,
          strCurrencyId);
    } else if (vars.commandIn("CURRENCY")) {
      String strOrg = vars.getRequestGlobalVariable("inpadOrgId", "ReportParetoProduct|AD_Org_ID",
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

  private void printPageDataSheet(HttpServletRequest request, HttpServletResponse response,
      VariablesSecureApp vars, String strWarehouse, String strAD_Org_ID, String strClient,
      String strCurrencyId) throws IOException, ServletException {

    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();

    ReportParetoProductData[] data = null;
    String strConvRateErrorMsg = "";
    String discard[] = { "discard" };

    ConnectionProvider readOnlyCP = DalConnectionProvider.getReadOnlyConnectionProvider();
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_reports/ReportParetoProduct", discard)
        .createXmlDocument();

    if (vars.commandIn("FIND")) {
      // Checks if there is a conversion rate for each of the transactions
      // of the report
      OBError myMessage = new OBError();
      try {
        OBContext.setAdminMode(true);

        // Get legal entity (for aggregated table and currency conversion. The latter is only
        // possible because the report can be launched only for one legal entity at the same time)
        final Organization legalEntity = OBContext.getOBContext()
            .getOrganizationStructureProvider(strClient)
            .getLegalEntity(OBDal.getReadOnlyInstance().get(Organization.class, strAD_Org_ID));
        if (legalEntity == null) {
          throw new OBException(OBMessageUtils.messageBD("WarehouseNotInLE"));
        }

        // Calculate max aggregated date or set a default value if not aggregated data
        String strMaxAggDate = ReportParetoProductData.selectMaxAggregatedDate(readOnlyCP,
            legalEntity.getId());
        if (StringUtils.isBlank(strMaxAggDate)) {
          final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
          final Date maxAggDate = formatter.parse("01-01-0000");
          strMaxAggDate = OBDateUtils.formatDate(maxAggDate);
        }

        // Process Time (useful to avoid taking into account transactions for legacy costing rules)
        String processTime = "01-01-1970 00:00:00";
        final CostingRule costRule = ReportValuationStock.getLEsCostingAlgortithm(legalEntity);
        if (costRule != null) {
          processTime = OBDateUtils.formatDate(CostingUtils.getCostingRuleStartingDate(costRule),
              "dd-MM-yyyy HH:mm:ss");
        }
        final String processTimeDateFormat = "DD-MM-YYYY HH24:MI:SS";

        data = ReportParetoProductData.select(readOnlyCP, strCurrencyId, strClient,
            legalEntity.getId(), strMaxAggDate, processTime, processTimeDateFormat, strWarehouse,
            strAD_Org_ID, vars.getLanguage());
      } catch (ServletException ex) {
        myMessage = Utility.translateError(readOnlyCP, vars, vars.getLanguage(), ex.getMessage());
      } catch (ParseException ignore) {
      } finally {
        OBContext.restorePreviousMode();
      }
      strConvRateErrorMsg = myMessage.getMessage();
      // If a conversion rate is missing for a certain transaction, an
      // error message window pops-up.
      if (StringUtils.isNotEmpty(strConvRateErrorMsg)) {
        advise(request, response, "ERROR",
            Utility.messageBD(readOnlyCP, "NoConversionRateHeader", vars.getLanguage()),
            strConvRateErrorMsg);
      } else { // Otherwise, the report is launched
        if (data == null || data.length == 0) {
          discard[0] = "selEliminar";
          data = ReportParetoProductData.set();
        } else {
          // Apply differences in percentages applying difference to bigger percentage
          BigDecimal total = BigDecimal.ZERO;
          BigDecimal difference = BigDecimal.ZERO;
          int toAdjustPosition = 0;
          String currentOrganization = data[0].orgid;
          for (int i = 0; i < data.length; i++) {
            if (StringUtils.equals(data[i].orgid, currentOrganization)) {
              total = total.add(new BigDecimal(data[i].percentage))
                  .setScale(2, RoundingMode.HALF_UP);
            } else {
              difference = new BigDecimal("100.00").subtract(total);
              total = new BigDecimal(data[i].percentage).setScale(2, RoundingMode.HALF_UP);
              data[toAdjustPosition].percentage = new BigDecimal(data[toAdjustPosition].percentage)
                  .add(difference)
                  .setScale(2, RoundingMode.HALF_UP)
                  .toString();
              toAdjustPosition = i;
              currentOrganization = data[i].orgid;
            }
          }
          // Update last group
          difference = new BigDecimal("100.00").subtract(total);
          data[toAdjustPosition].percentage = new BigDecimal(data[toAdjustPosition].percentage)
              .add(difference)
              .setScale(2, RoundingMode.HALF_UP)
              .toString();

          xmlDocument.setData("structure1", data);
        }
      }
    }

    else {
      if (StringUtils.isEmpty(strConvRateErrorMsg)) {
        discard[0] = "selEliminar";
        data = ReportParetoProductData.set();
      }
    }

    if (StringUtils.isEmpty(strConvRateErrorMsg)) {
      // Load Toolbar
      ToolBar toolbar = new ToolBar(readOnlyCP, vars.getLanguage(), "ReportParetoProduct", false,
          "", "", "", false, "ad_reports", strReplaceWith, false, true);
      toolbar.prepareSimpleToolBarTemplate();
      xmlDocument.setParameter("toolbar", toolbar.toString());

      // Create WindowTabs
      try {
        WindowTabs tabs = new WindowTabs(readOnlyCP, vars,
            "org.openbravo.erpCommon.ad_reports.ReportParetoProduct");
        xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
        xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
        xmlDocument.setParameter("childTabContainer", tabs.childTabs());
        xmlDocument.setParameter("theme", vars.getTheme());
        NavigationBar nav = new NavigationBar(readOnlyCP, vars.getLanguage(),
            "ReportParetoProduct.html", classInfo.id, classInfo.type, strReplaceWith,
            tabs.breadcrumb());
        xmlDocument.setParameter("navigationBar", nav.toString());
        LeftTabsBar lBar = new LeftTabsBar(readOnlyCP, vars.getLanguage(),
            "ReportParetoProduct.html", strReplaceWith);
        xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      // Load Message Area
      {
        OBError myMessage = vars.getMessage("ReportParetoProduct");
        vars.removeMessage("ReportParetoProduct");
        if (myMessage != null) {
          xmlDocument.setParameter("messageType", myMessage.getType());
          xmlDocument.setParameter("messageTitle", myMessage.getTitle());
          xmlDocument.setParameter("messageMessage", myMessage.getMessage());
        }
      }

      // Pass parameters to the window
      xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
      xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
      xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");

      // Load Business Partner Group combo with data
      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "M_Warehouse_ID", "", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportParetoProduct"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportParetoProduct"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportParetoProduct",
            strWarehouse);
        xmlDocument.setData("reportM_Warehouse_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "AD_Org_ID", "", "D4DF252DEC3B44858454EE5292A8B836",
            Utility.getContext(readOnlyCP, vars, "#User_Org", "ReportParetoProduct"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportParetoProduct"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportParetoProduct",
            strAD_Org_ID);
        xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      xmlDocument.setParameter("ccurrencyid", strCurrencyId);
      try {
        ComboTableData comboTableData = new ComboTableData(vars, readOnlyCP, "TABLEDIR",
            "C_Currency_ID", "", "",
            Utility.getContext(readOnlyCP, vars, "#AccessibleOrgTree", "ReportParetoProduct"),
            Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportParetoProduct"), 0);
        Utility.fillSQLParameters(readOnlyCP, vars, null, comboTableData, "ReportParetoProduct",
            strCurrencyId);
        xmlDocument.setData("reportC_Currency_ID", "liststructure", comboTableData.select(false));
        comboTableData = null;
      } catch (Exception ex) {
        throw new ServletException(ex);
      }

      xmlDocument.setParameter("warehouseArray",
          Utility.arrayDobleEntrada("arrWarehouse",
              ReportParetoProductData.selectWarehouseDouble(readOnlyCP,
                  Utility.getContext(readOnlyCP, vars, "#User_Client", "ReportParetoProduct"))));

      xmlDocument.setParameter("mWarehouseId", strWarehouse);
      xmlDocument.setParameter("adOrg", strAD_Org_ID);

      // Print document in the output
      out.println(xmlDocument.print());
      out.close();
    }
  }

  private OBError mUpdateParetoProduct(VariablesSecureApp vars, String strWarehouse,
      String strAD_Org_ID, String strAD_Client_ID) throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();

    PInstanceProcessData.insertPInstance(this, pinstance, "9CD67D41E43242CDA034FB994B75812A", "0",
        "N", vars.getUser(), vars.getClient(), vars.getOrg());
    PInstanceProcessData.insertPInstanceParam(this, pinstance, "1", "m_warehouse_id", strWarehouse,
        vars.getClient(), vars.getOrg(), vars.getUser());
    PInstanceProcessData.insertPInstanceParam(this, pinstance, "2", "ad_org_id", strAD_Org_ID,
        vars.getClient(), vars.getOrg(), vars.getUser());
    PInstanceProcessData.insertPInstanceParam(this, pinstance, "3", "ad_client_id", strAD_Client_ID,
        vars.getClient(), vars.getOrg(), vars.getUser());
    ReportParetoProductData.mUpdateParetoProduct0(this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.select(this, pinstance);
    OBError myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  @Override
  public String getServletInfo() {
    return "Servlet ReportParetoProduct info. Insert here any relevant information";
  }

}
