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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.HashMap;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.Tree;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.businessUtility.WindowTabsData;
import org.openbravo.erpCommon.reference.PInstanceProcessData;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.procurement.RequisitionLine;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class RequisitionToOrder extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strProductId = vars.getGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID", "");
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "RequisitionToOrder|DateFrom", "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo", "");
      String strRequesterId = vars.getGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID", "");
      String strVendorId = vars.getGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID", "");
      String strIncludeVendor = vars.getGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor", "Y");
      String strOrgId = vars.getGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID",
          vars.getOrg());
      vars.setSessionValue("RequisitionToOrder|isSOTrx", "N");
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId);
    } else if (vars.commandIn("FIND")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      updateLockedLines(vars, strOrgId);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId);
    } else if (vars.commandIn("ADD")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strRequisitionLines = vars.getRequiredInStringParameter("inpRequisitionLine",
          IsIDFilter.instance);
      updateLockedLines(vars, strOrgId);
      lockRequisitionLines(vars, strRequisitionLines);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId);
    } else if (vars.commandIn("REMOVE")) {
      String strProductId = vars.getRequestGlobalVariable("inpmProductId",
          "RequisitionToOrder|M_Product_ID");
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "RequisitionToOrder|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo", "RequisitionToOrder|DateTo");
      String strRequesterId = vars.getRequestGlobalVariable("inpRequesterId",
          "RequisitionToOrder|Requester_ID");
      String strVendorId = vars.getRequestGlobalVariable("inpcBpartnerId",
          "RequisitionToOrder|C_BPartner_ID");
      String strIncludeVendor = vars.getRequestGlobalVariable("inpShowNullVendor",
          "RequisitionToOrder|ShowNullVendor");
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      unlockRequisitionLines(vars, strSelectedLines);
      updateLockedLines(vars, strOrgId);
      printPageDataSheet(response, vars, strProductId, strDateFrom, strDateTo, strRequesterId,
          strVendorId, strIncludeVendor, strOrgId);
    } else if (vars.commandIn("OPEN_CREATE")) {
      String strSelectedLines = vars.getRequiredInStringParameter("inpSelectedReq",
          IsIDFilter.instance);
      String strOrgId = vars.getRequestGlobalVariable("inpadOrgId", "RequisitionToOrder|AD_Org_ID");
      updateLockedLines(vars, strOrgId);
      checkSelectedRequisitionLines(response, vars, strSelectedLines);
    } else if (vars.commandIn("GENERATE")) {
      String strSelectedLines = vars.getRequiredGlobalVariable("inpSelected",
          "RequisitionToOrderCreate|SelectedLines");
      String strOrderDate = vars.getRequiredGlobalVariable("inpOrderDate",
          "RequisitionToOrderCreate|OrderDate");
      String strVendor = vars.getRequiredGlobalVariable("inpOrderVendorId",
          "RequisitionToOrderCreate|OrderVendor");
      String strPriceListId = vars.getRequiredGlobalVariable("inpPriceListId",
          "RequisitionToOrderCreate|PriceListId");
      String strOrg = vars.getRequiredGlobalVariable("inpOrderOrg", "RequisitionToOrderCreate|Org");
      String strWarehouse = vars.getRequiredGlobalVariable("inpWarehouse",
          "RequisitionToOrderCreate|Warehouse");
      OBError myMessage = processPurchaseOrder(vars, strSelectedLines, strOrderDate, strVendor,
          strPriceListId, strOrg, strWarehouse);
      vars.setMessage("RequisitionToOrderCreate", myMessage);
      printPageCreate(response, vars, "", "", "", "", "");
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strProductId, String strDateFrom, String strDateTo, String strRequesterId,
      String strVendorId, String strIncludeVendor, String strOrgId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    XmlDocument xmlDocument = null;

    String strTreeOrg = RequisitionToOrderData.treeOrg(this, vars.getClient());
    RequisitionToOrderData[] datalines = RequisitionToOrderData.selectLines(this,
        Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"),
        Tree.getMembers(this, strTreeOrg, strOrgId), strDateFrom,
        DateTimeData.nDaysAfter(this, strDateTo, "1"), strProductId, strRequesterId,
        (strIncludeVendor.equals("Y") ? strVendorId : null),
        (strIncludeVendor.equals("Y") ? null : strVendorId));

    RequisitionToOrderData[] dataselected = RequisitionToOrderData.selectSelected(this,
        vars.getLanguage(), vars.getUser(),
        Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"),
        Tree.getMembers(this, strTreeOrg, strOrgId));
    String discard[] = { "" };
    if (dataselected == null || dataselected.length == 0) {
      dataselected = RequisitionToOrderData.set();
      discard[0] = "funcSelectedEvenOddRow";
    }
    xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/RequisitionToOrder", discard)
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "RequisitionToOrder", false, "", "", "",
        false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_forms.RequisitionToOrder");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "RequisitionToOrder.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "RequisitionToOrder.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("RequisitionToOrder");
      vars.removeMessage("RequisitionToOrder");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("paramProductId", strProductId);
    xmlDocument.setParameter("paramProductDescription", strProductId.equals("") ? ""
        : RequisitionToOrderData.mProductDescription(this, strProductId, vars.getLanguage()));
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("paramRequester", strRequesterId);
    xmlDocument.setParameter("paramBPartnerId", strVendorId);
    xmlDocument.setParameter("paramBPartnerDescription", strVendorId.equals("") ? ""
        : RequisitionToOrderData.bPartnerDescription(this, strVendorId, vars.getLanguage()));
    xmlDocument.setParameter("paramShowNullVendor", strIncludeVendor);
    xmlDocument.setParameter("paramAdOrgId", strOrgId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_User_ID", "",
          "UsersWithRequisition",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "RequisitionToOrder"),
          Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder",
          strRequesterId);
      xmlDocument.setData("reportRequester_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation",
          Utility.getContext(this, vars, "#User_Org", "RequisitionToOrder"),
          Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder", strOrgId);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    boolean uomManagementPreference = UOMUtil.isUomManagementEnabled();

    boolean strHaveSecDataLines = false;
    boolean strHaveAumDataLines = false;
    for (int i = 0; i < datalines.length; i++) {
      try {
        if (!datalines[i].quantityorder.isEmpty()
            && !StringUtils.equals(datalines[i].secuomname, "**")) {
          strHaveSecDataLines = true;
        }
        if (uomManagementPreference && !datalines[i].cAum.isEmpty()
            && !datalines[i].aumqty.equals("**")) {
          strHaveAumDataLines = true;
        }
        if (strHaveSecDataLines && strHaveAumDataLines) {
          break;
        }
      } catch (NullPointerException e) {

      }
    }

    for (int i = 0; i < datalines.length; i++) {

      if (strHaveSecDataLines && strHaveAumDataLines) {
        // UOM preference has value "Y" and at least one line has secondary UOM
        xmlDocument.setParameter("uompreference", "");
        xmlDocument.setParameter("havesecuom", "");
        if (datalines[i].cAum.isEmpty() && datalines[i].aumqty.isEmpty()
            && datalines[i].quantityorder.isEmpty()
            && StringUtils.equals(datalines[i].secuomname, "**")) {
          RequisitionToOrderData[] defaultAumData = RequisitionToOrderData.selectAUMDefault(this,
              datalines[i].mProductId, vars.getClient(), strOrgId);
          String defaultAum = (defaultAumData.length > 0) ? defaultAumData[0].cAum
              : datalines[i].cUomId;
          datalines[i].aumqty = datalines[i].qtytoorder;
          datalines[i].cAum = defaultAum;
          datalines[i].aumname = (defaultAumData.length > 0) ? defaultAumData[0].aumname
              : datalines[i].uomname;
          if (!defaultAum.equals(datalines[i].cUomId)) {
            datalines[i].qtytoorder = UOMUtil
                .getConvertedQty(datalines[i].mProductId, new BigDecimal(datalines[i].aumqty),
                    datalines[i].cAum)
                .toString();
          }
        }
      } else if (uomManagementPreference && datalines[i].quantityorder.isEmpty()
          && StringUtils.equals(datalines[i].secuomname, "**")) {
        // UOM preference is Y and no line has secondary UOM
        datalines[i].havesecuom = "none";
        xmlDocument.setParameter("uompreference", "");
        xmlDocument.setParameter("havesecuom", "display:none;");
        if (datalines[i].cAum.isEmpty() && datalines[i].aumqty.isEmpty()) {
          RequisitionToOrderData[] defaultAumData = RequisitionToOrderData.selectAUMDefault(this,
              datalines[i].mProductId, vars.getClient(), strOrgId);
          String defaultAum = (defaultAumData.length > 0) ? defaultAumData[0].cAum
              : datalines[i].cUomId;
          datalines[i].aumqty = datalines[i].qtytoorder;
          datalines[i].cAum = defaultAum;
          datalines[i].aumname = (defaultAumData.length > 0) ? defaultAumData[0].aumname
              : datalines[i].uomname;
          if (!defaultAum.equals(datalines[i].cUomId)) {
            datalines[i].qtytoorder = UOMUtil
                .getConvertedQty(datalines[i].mProductId, new BigDecimal(datalines[i].aumqty),
                    datalines[i].cAum)
                .toString();
          }
        }
      } else {
        // UOM preference is N
        datalines[i].uompreference = "none";
        xmlDocument.setParameter("uompreference", "display:none;");
        xmlDocument.setParameter("havesecuom", "");
      }
    }

    boolean strHaveSecDataSelected = false;
    boolean strHaveAumDataSelected = false;
    for (int i = 0; i < dataselected.length; i++) {
      try {
        if (!dataselected[i].quantityorder.isEmpty()
            && !StringUtils.equals(dataselected[i].secuomname, "**")) {
          strHaveSecDataSelected = true;
        }
        if (uomManagementPreference && !dataselected[i].cAum.isEmpty()
            && !dataselected[i].aumqty.equals("**")) {
          strHaveAumDataSelected = true;
        }
        if (strHaveSecDataSelected && strHaveAumDataSelected) {
          break;
        }
      } catch (NullPointerException e) {

      }
    }

    for (int i = 0; i < dataselected.length; i++) {

      if (strHaveSecDataSelected && strHaveAumDataSelected) {
        // UOM preference is Y and at least one line has secondary UOM
        xmlDocument.setParameter("uompreferencesel", "");
        xmlDocument.setParameter("havesecuomsel", "");
        if (dataselected[i].cAum.isEmpty() && dataselected[i].aumqty.isEmpty()
            && dataselected[i].quantityorder.isEmpty()
            && StringUtils.equals(dataselected[i].secuomname, "**")) {
          RequisitionToOrderData[] defaultAumData = RequisitionToOrderData.selectAUMDefault(this,
              dataselected[i].mProductId, vars.getClient(), strOrgId);
          String defaultAum = (defaultAumData.length > 0) ? defaultAumData[0].cAum
              : dataselected[i].cUomId;
          dataselected[i].aumqty = dataselected[i].lockqty;
          dataselected[i].cAum = defaultAum;
          dataselected[i].aumname = (defaultAumData.length > 0) ? defaultAumData[0].aumname
              : dataselected[i].uomname;
          if (!defaultAum.equals(dataselected[i].cUomId)) {
            dataselected[i].lockqty = UOMUtil
                .getConvertedQty(dataselected[i].mProductId, new BigDecimal(dataselected[i].aumqty),
                    dataselected[i].cAum)
                .toString();
          }
        }
      } else if (uomManagementPreference && dataselected[i].quantityorder.isEmpty()
          && StringUtils.equals(dataselected[i].secuomname, "**")) {
        // UOM preference is Y and no line has secondary UOM
        dataselected[i].havesecuom = "none";
        xmlDocument.setParameter("uompreferencesel", "");
        xmlDocument.setParameter("havesecuomsel", "display:none;");
        if (dataselected[i].cAum.isEmpty() && dataselected[i].aumqty.isEmpty()) {
          RequisitionToOrderData[] defaultAumData = RequisitionToOrderData.selectAUMDefault(this,
              dataselected[i].mProductId, vars.getClient(), strOrgId);
          String defaultAum = (defaultAumData.length > 0) ? defaultAumData[0].cAum
              : dataselected[i].cUomId;
          dataselected[i].aumqty = dataselected[i].lockqty;
          dataselected[i].cAum = defaultAum;
          dataselected[i].aumname = (defaultAumData.length > 0) ? defaultAumData[0].aumname
              : dataselected[i].uomname;
          if (!defaultAum.equals(dataselected[i].cUomId)) {
            dataselected[i].lockqty = UOMUtil
                .getConvertedQty(dataselected[i].mProductId, new BigDecimal(dataselected[i].aumqty),
                    dataselected[i].cAum)
                .toString();
          }
        }
      } else {
        // UOM preference is N
        dataselected[i].uompreference = "none";
        xmlDocument.setParameter("uompreferencesel", "display:none;");
        xmlDocument.setParameter("havesecuomsel", "");
      }
    }
    xmlDocument.setData("structureSearch", datalines);
    xmlDocument.setData("structureSelected", dataselected);
    out.println(xmlDocument.print());
    out.close();
  }

  private void lockRequisitionLines(VariablesSecureApp vars, String strRequisitionLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Locking requisition lines: " + strRequisitionLines);
    }
    RequisitionToOrderData.lock(this, vars.getUser(), strRequisitionLines);
  }

  private void unlockRequisitionLines(VariablesSecureApp vars, String strRequisitionLines)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Unlocking requisition lines: " + strRequisitionLines);
    }
    RequisitionToOrderData.unlock(this, strRequisitionLines);
  }

  private void updateLockedLines(VariablesSecureApp vars, String strOrgId)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Update locked lines");
    }
    String strTreeOrg = RequisitionToOrderData.treeOrg(this, vars.getClient());
    RequisitionToOrderData[] dataselected = RequisitionToOrderData.selectSelected(this,
        vars.getLanguage(), vars.getUser(),
        Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"),
        Tree.getMembers(this, strTreeOrg, strOrgId));

    boolean uomManagementPreference = UOMUtil.isUomManagementEnabled();

    for (int i = 0; dataselected != null && i < dataselected.length; i++) {
      String strLockQty = "";
      String strLockAqumQty = null;
      if (uomManagementPreference && dataselected[i].quantityorder.isEmpty()
          && StringUtils.equals(dataselected[i].secuomname, "**")) {
        strLockQty = vars.getNumericParameter("inpQty" + dataselected[i].mRequisitionlineId);
        strLockAqumQty = vars.getNumericParameter("inpAumQty" + dataselected[i].mRequisitionlineId);
      } else {
        strLockQty = vars.getNumericParameter("inpQty" + dataselected[i].mRequisitionlineId);
      }

      String strLockPrice = vars
          .getNumericParameter("inpPrice" + dataselected[i].mRequisitionlineId);
      RequisitionToOrderData.updateLock(this, strLockQty, strLockPrice, strLockAqumQty,
          dataselected[i].mRequisitionlineId);
    }
  }

  private void checkSelectedRequisitionLines(HttpServletResponse response, VariablesSecureApp vars,
      String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Check selected requisition lines");
    }

    // Check unique partner
    String strVendorId = "";
    String strOrderDate = DateTimeData.today(this);
    String strPriceListId = "";
    String strOrgId = "";
    String strMessage = "";
    if (!strSelected.equals("")) {
      RequisitionToOrderData[] vendor = RequisitionToOrderData.selectVendor(this, strSelected);
      if (vendor != null && vendor.length == 1) {
        strVendorId = vendor[0].vendorId;
        strMessage = Utility.messageBD(this, "AllLinesSameVendor", vars.getLanguage()) + ": "
            + RequisitionToOrderData.bPartnerDescription(this, vendor[0].vendorId,
                vars.getLanguage());
      } else if (vendor != null && vendor.length > 1) {
        // Error, the selected lines are of different vendors, it is
        // necessary to set one.
        strMessage = Utility.messageBD(this, "MoreThanOneVendor", vars.getLanguage());
      } else {
        // Error, it is necessary to select a vendor.
        strMessage = Utility.messageBD(this, "AllLinesNullVendor", vars.getLanguage());
      }
      // Check unique pricelist
      RequisitionToOrderData[] pricelist = RequisitionToOrderData.selectPriceList(this,
          vars.getLanguage(), strSelected);
      if (pricelist != null && pricelist.length == 1) {
        strPriceListId = pricelist[0].mPricelistId;
        strMessage += "<br>" + Utility.messageBD(this, "AllLinesSamePricelist", vars.getLanguage())
            + ": " + pricelist[0].pricelistid;
      } else if (pricelist != null && pricelist.length > 1) {
        // Error, the selected lines are of different pricelists, it is
        // necessary to set one.
        strMessage += "<br>" + Utility.messageBD(this, "MoreThanOnePricelist", vars.getLanguage());
      } else {
        // Error, it is necessary to select a pricelist.
        strMessage += "<br>" + Utility.messageBD(this, "AllLinesNullVendor", vars.getLanguage());
      }

      // Check unique org
      RequisitionToOrderData[] org = RequisitionToOrderData.selectOrg(this, vars.getLanguage(),
          strSelected);
      if (org != null && org.length == 1) {
        strOrgId = org[0].adOrgId;
        strMessage += "<br>" + Utility.messageBD(this, "AllLinesSameOrg", vars.getLanguage()) + ": "
            + org[0].org;
      } else {
        // Error, the selected lines are of different orgs, it is
        // necessary to set one.
        strMessage += "<br>" + Utility.messageBD(this, "MoreThanOneOrg", vars.getLanguage());
      }
      OBError myMessage = new OBError();
      myMessage.setTitle("");
      myMessage.setType("Info");
      myMessage.setMessage(strMessage);
      vars.setMessage("RequisitionToOrderCreate", myMessage);
    } else {
      OBError myMessage = new OBError();
      myMessage.setTitle("");
      myMessage.setType("Info");
      myMessage.setMessage(Utility.messageBD(this, "MustSelectLines", vars.getLanguage()));
      vars.setMessage("RequisitionToOrderCreate", myMessage);
    }

    printPageCreate(response, vars, strOrderDate, strVendorId, strPriceListId, strOrgId,
        strSelected);
  }

  private void printPageCreate(HttpServletResponse response, VariablesSecureApp vars,
      String strOrderDate, String strVendorId, String strPriceListId, String strOrgId,
      String strSelected) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Print Create Purchase order");
    }
    String strDescription = Utility.messageBD(this, "RequisitionToOrderCreate", vars.getLanguage());
    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_forms/RequisitionToOrderCreate")
        .createXmlDocument();
    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\r\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("help", Replace.replace(strDescription, "\\n", "\n"));
    xmlDocument.setParameter("paramLoginWarehouseId", vars.getSessionValue("#M_WAREHOUSE_ID"));
    {
      OBError myMessage = vars.getMessage("RequisitionToOrderCreate");
      vars.removeMessage("RequisitionToOrderCreate");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    xmlDocument.setParameter("paramSelected", strSelected);
    xmlDocument.setParameter("paramOrderVendorId", strVendorId);
    xmlDocument.setParameter("paramOrderVendorDescription", strVendorId.equals("") ? ""
        : RequisitionToOrderData.bPartnerDescription(this, strVendorId, vars.getLanguage()));
    xmlDocument.setParameter("orderDate", strOrderDate);
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("paramOrderOrgId", strOrgId);
    xmlDocument.setParameter("arrayWarehouse",
        Utility.arrayDobleEntrada("arrWarehouse",
            RequisitionToOrderData.selectWarehouseDouble(this, vars.getClient(),
                Utility.getContext(this, vars, "#AccessibleOrgTree", "RequisitionToOrder"),
                Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"))));
    xmlDocument.setParameter("paramPriceListId", strPriceListId);
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org is transactions allowed",
          Utility.getContext(this, vars, "#User_Org", "RequisitionToOrder"),
          Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder", strOrgId);
      xmlDocument.setData("reportOrderOrg_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "M_Pricelist_ID",
          "", "Purchase Pricelist",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "RequisitionToOrder"),
          Utility.getContext(this, vars, "#User_Client", "RequisitionToOrder"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "RequisitionToOrder",
          strPriceListId);
      xmlDocument.setData("reportPriceList_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processPurchaseOrder(VariablesSecureApp vars, String strSelected,
      String strOrderDate, String strVendor, String strPriceListId, String strOrg,
      String strWarehouse) throws IOException, ServletException {
    StringBuffer textMessage = new StringBuffer();
    Connection conn = null;

    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");

    String strPriceListVersionId = RequisitionToOrderData.getPricelistVersion(this, strPriceListId,
        strOrderDate);
    RequisitionToOrderData[] noprice = RequisitionToOrderData.selectNoPrice(this,
        vars.getLanguage(), strPriceListVersionId, strSelected);
    if (noprice != null && noprice.length > 0) {
      textMessage.append(Utility.messageBD(this, "LinesWithNoPrice", vars.getLanguage()))
          .append("<br><ul>");
      for (int i = 0; i < noprice.length; i++) {
        textMessage.append("<li>").append(noprice[i].product);
      }
      textMessage.append("</ul>");
      myMessage.setType("Error");
      myMessage.setMessage(textMessage.toString());
      return myMessage;
    }

    RequisitionToOrderData[] data1 = RequisitionToOrderData.selectVendorData(this, strVendor);
    if (data1[0].poPaymenttermId == null || data1[0].poPaymenttermId.equals("")) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "VendorWithNoPaymentTerm", vars.getLanguage()));
      return myMessage;
    }
    if ("".equals(RequisitionToOrderData.cBPartnerLocationId(this, strVendor))) {
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "NoBPLocation", vars.getLanguage()));
      return myMessage;
    }

    try {
      conn = getTransactionConnection();
      String strCOrderId = SequenceIdData.getUUID();
      String docTargetType = RequisitionToOrderData.cDoctypeTarget(conn, this, vars.getClient(),
          strOrg);
      String strDocumentNo = Utility.getDocumentNo(this, vars, "", "C_Order", docTargetType,
          docTargetType, false, true);
      String cCurrencyId = RequisitionToOrderData.selectCurrency(this, strPriceListId);

      try {
        RequisitionToOrderData.insertCOrder(conn, this, strCOrderId, vars.getClient(), strOrg,
            vars.getUser(), strDocumentNo, "DR", "CO", "0", docTargetType, strOrderDate,
            strOrderDate, strOrderDate, strVendor,
            RequisitionToOrderData.cBPartnerLocationId(this, strVendor),
            RequisitionToOrderData.billto(this, strVendor).equals("")
                ? RequisitionToOrderData.cBPartnerLocationId(this, strVendor)
                : RequisitionToOrderData.billto(this, strVendor),
            cCurrencyId, isAlternativeFinancialFlow() ? "P" : data1[0].paymentrulepo,
            data1[0].poPaymenttermId, data1[0].invoicerule.equals("") ? "I" : data1[0].invoicerule,
            data1[0].deliveryrule.equals("") ? "A" : data1[0].deliveryrule, "I",
            data1[0].deliveryviarule.equals("") ? "D" : data1[0].deliveryviarule, strWarehouse,
            strPriceListId, "", "", "", data1[0].poPaymentmethodId);
      } catch (ServletException ex) {
        myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
        releaseRollbackConnection(conn);
        return myMessage;
      }

      RequisitionToOrderData[] lines = RequisitionToOrderData.linesToOrder(this, cCurrencyId,
          strOrderDate, strOrg, strWarehouse,
          RequisitionToOrderData.billto(this, strVendor).equals("")
              ? RequisitionToOrderData.cBPartnerLocationId(this, strVendor)
              : RequisitionToOrderData.billto(this, strVendor),
          RequisitionToOrderData.cBPartnerLocationId(this, strVendor), strPriceListVersionId,
          strSelected);

      boolean uomManagementPreference = UOMUtil.isUomManagementEnabled();
      HashMap<String, String[]> hashLines = new HashMap<String, String[]>();
      int line = 0;
      for (int i = 0; lines != null && i < lines.length; i++) {
        if ("".equals(lines[i].tax)) {
          RequisitionLine rl = OBDal.getInstance()
              .get(RequisitionLine.class, lines[i].mRequisitionlineId);
          myMessage.setType("Error");
          myMessage.setMessage(String.format(OBMessageUtils.messageBD("NoTaxRequisition"),
              rl.getLineNo(), rl.getRequisition().getDocumentNo()));
          releaseRollbackConnection(conn);
          return myMessage;
        }

        // Group entries with same attributes set into a hash map
        String strGroupId = String.format("%s-%s-%s-%s-%s", lines[i].mProductId,
            lines[i].mAttributesetinstanceId, lines[i].description, lines[i].priceactual,
            lines[i].mProductUomId);
        String[] hashLine = hashLines.get(strGroupId);

        lines[i].aumqty = lines[i].lockaumqty;
        if (uomManagementPreference && lines[i].quantityorder.isEmpty()
            && StringUtils.equals(lines[i].secuomname, "**")) {
          try {
            if (lines[i].cAum.isEmpty()) {
              RequisitionToOrderData[] defaultAumData = RequisitionToOrderData
                  .selectAUMDefault(this, lines[i].mProductId, vars.getClient(), strOrg);
              lines[i].cAum = (defaultAumData.length > 0) ? defaultAumData[0].cAum
                  : lines[i].cUomId;
            }
            if (!lines[i].cUomId.equals(lines[i].cAum)) {
              lines[i].aumqty = new BigDecimal(
                  UOMUtil
                      .getConvertedQty(lines[i].mProductId, new BigDecimal(lines[i].lockaumqty),
                          lines[i].cAum)
                      .toString()).toPlainString();
            }
          } catch (NumberFormatException e) {
          }
        }
        // Insert new line
        if (hashLine == null) {
          lines[i].cOrderlineId = SequenceIdData.getUUID();
          line += 10;
          hashLines.put(strGroupId, new String[] { lines[i].cOrderlineId, lines[i].lockqty,
              lines[i].quantityorder, lines[i].aumqty });
          try {
            RequisitionToOrderData.insertCOrderline(conn, this, lines[i].cOrderlineId,
                vars.getClient(), strOrg, vars.getUser(), strCOrderId, Integer.toString(line),
                strVendor, RequisitionToOrderData.cBPartnerLocationId(this, strVendor),
                strOrderDate, lines[i].needbydate, lines[i].description, lines[i].mProductId,
                lines[i].mAttributesetinstanceId, strWarehouse, lines[i].mProductUomId,
                lines[i].cUomId, lines[i].cAum, lines[i].quantityorder, lines[i].lockqty,
                lines[i].aumqty, cCurrencyId, lines[i].pricelist, lines[i].priceactual,
                lines[i].pricelimit, lines[i].tax, "", lines[i].discount,
                lines[i].grossUnit, lines[i].grossAmt);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }

        // Update existing line
        else {
          lines[i].cOrderlineId = hashLine[0];
          BigDecimal qtyOrder = new BigDecimal(hashLine[1]).add(new BigDecimal(lines[i].lockqty));
          BigDecimal quantityOrder = (StringUtils.isEmpty(hashLine[2]) ? BigDecimal.ZERO
              : new BigDecimal(hashLine[2]))
                  .add(StringUtils.isEmpty(lines[i].quantityorder) ? BigDecimal.ZERO
                      : new BigDecimal(lines[i].quantityorder));
          BigDecimal qtyAumOrder = (StringUtils.isEmpty(hashLine[3]) ? BigDecimal.ZERO
              : new BigDecimal(hashLine[3]))
                  .add(StringUtils.isEmpty(lines[i].aumqty) ? BigDecimal.ZERO
                      : new BigDecimal(lines[i].aumqty));
          hashLine[1] = qtyOrder.toPlainString();
          hashLine[2] = BigDecimal.ZERO.compareTo(quantityOrder) == 0 ? ""
              : quantityOrder.toPlainString();
          hashLine[3] = qtyAumOrder.toPlainString();
          try {
            RequisitionToOrderData.updateCOrderline(conn, this, hashLine[1], hashLine[2],
                hashLine[3], hashLine[0]);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }

        if (log4j.isDebugEnabled()) {
          log4j.debug(
              "Lockqty: " + lines[i].lockqty + " - Quantityorder: " + lines[i].quantityorder);
        }
      }

      unlockRequisitionLines(vars, strSelected);
      for (int i = 0; lines != null && i < lines.length; i++) {
        String strRequisitionOrderId = SequenceIdData.getUUID();
        try {
          RequisitionToOrderData.insertRequisitionOrder(conn, this, strRequisitionOrderId,
              vars.getClient(), strOrg, vars.getUser(), lines[i].mRequisitionlineId,
              lines[i].cOrderlineId, lines[i].lockqty);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myMessage;
        }
        if (lines[i].toClose.equals("Y")) {
          RequisitionToOrderData.requisitionStatus(conn, this, lines[i].mRequisitionlineId,
              vars.getUser());
        }
      }

      OBError myMessageAux = cOrderPost(conn, vars, strCOrderId);
      releaseCommitConnection(conn);
      String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "181");
      textMessage.append(strWindowName).append(" ").append(strDocumentNo).append(": ");
      if (myMessageAux.getMessage().equals("")) {
        textMessage.append(Utility.messageBD(this, "Success", vars.getLanguage()));
      } else {
        textMessage.append(myMessageAux.getMessage());
      }

      myMessage.setType(myMessageAux.getType());
      myMessage.setMessage(textMessage.toString());
      return myMessage;
    } catch (Exception e) {
      try {
        if (conn != null) {
          releaseRollbackConnection(conn);
        }
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      return myMessage;
    }
  }

  private OBError cOrderPost(Connection conn, VariablesSecureApp vars, String strcOrderId)
      throws IOException, ServletException {
    String pinstance = SequenceIdData.getUUID();

    PInstanceProcessData.insertPInstance(conn, this, pinstance, "104", strcOrderId, "N",
        vars.getUser(), vars.getClient(), vars.getOrg());
    RequisitionToOrderData.cOrderPost0(conn, this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, this,
        pinstance);
    OBError myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  /**
   * Checks if the any module implements and alternative Financial Management preference. It should
   * be the Advanced Payables and Receivables module.
   * 
   * @return true if any module implements and alternative Financial Management preference.
   */
  private boolean isAlternativeFinancialFlow() {
    try {
      try {
        Preferences.getPreferenceValue("FinancialManagement", true, null, null,
            OBContext.getOBContext().getUser(), null, null);
      } catch (PropertyNotFoundException e) {
        return false;
      }
    } catch (PropertyException e) {
      return false;
    }
    return true;
  }

  @Override
  public String getServletInfo() {
    return "Servlet RequisitionToOrder.";
  } // end of getServletInfo() method
}
