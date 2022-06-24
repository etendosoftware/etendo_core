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

package org.openbravo.erpCommon.ad_forms;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
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
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.OrgWarehouse;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMConversion;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class MaterialReceiptPending extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strDateFrom = vars.getGlobalVariable("inpDateFrom", "MaterialReceiptPending|DateFrom",
          "");
      String strDateTo = vars.getGlobalVariable("inpDateTo", "MaterialReceiptPending|DateTo", "");
      String strDocumentNo = vars.getGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo", "");
      String strC_BPartner_ID = vars.getGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID", "");
      String strAD_Org_ID = vars.getGlobalVariable("inpadOrgId", "MaterialReceiptPending|AD_Org_ID",
          vars.getOrg());
      vars.setSessionValue("MaterialReceiptPending|isSOTrx", "N");
      if (strDocumentNo.equals("")) {
        strDocumentNo += "%";
      }
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo,
          strDocumentNo, "DEFAULT");
    } else if (vars.commandIn("FIND")) {
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MaterialReceiptPending|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "MaterialReceiptPending|DateTo");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo");
      String strC_BPartner_ID = vars.getRequestGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID");
      String strAD_Org_ID = vars.getGlobalVariable("inpadOrgId",
          "MaterialReceiptPending|AD_Org_ID");
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo,
          strDocumentNo, "FIND");
    } else if (vars.commandIn("GENERATE")) {
      String strcOrderLineId = vars.getRequiredInStringParameter("inpOrder", IsIDFilter.instance);
      String strDateFrom = vars.getRequestGlobalVariable("inpDateFrom",
          "MaterialReceiptPending|DateFrom");
      String strDateTo = vars.getRequestGlobalVariable("inpDateTo",
          "MaterialReceiptPending|DateTo");
      String strDocumentNo = vars.getRequestGlobalVariable("inpDocumentNo",
          "MaterialReceiptPending|DocumentNo");
      String strC_BPartner_ID = vars.getRequestGlobalVariable("inpcBpartnerId",
          "MaterialReceiptPending|C_BPartner_ID");
      String strAD_Org_ID = vars.getGlobalVariable("inpadOrgId",
          "MaterialReceiptPending|AD_Org_ID");
      OBError myMessage = processPurchaseOrder(vars, strcOrderLineId);
      vars.setMessage("MaterialReceiptPending", myMessage);
      printPageDataSheet(response, vars, strC_BPartner_ID, strAD_Org_ID, strDateFrom, strDateTo,
          strDocumentNo, "GENERATE");
    } else {
      pageError(response);
    }
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strC_BPartner_ID, String strAD_Org_ID, String strDateFrom, String strDateTo,
      String strDocumentNo, String commandIn) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: dataSheet");
    }
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    String discard[] = { "sectionDetail" };
    XmlDocument xmlDocument = null;

    int limit = 0;
    MaterialReceiptPendingData[] data = null;
    String strTreeOrg = MaterialReceiptPendingData.treeOrg(this, vars.getClient());
    if (strC_BPartner_ID.equals("") && strAD_Org_ID.equals("")) {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_forms/MaterialReceiptPending", discard)
          .createXmlDocument();
      data = MaterialReceiptPendingData.set();
    } else {
      xmlDocument = xmlEngine
          .readXmlTemplate("org/openbravo/erpCommon/ad_forms/MaterialReceiptPending")
          .createXmlDocument();

      limit = Integer.parseInt(Utility.getPreference(vars, "FormsLimit", ""));
      String pgLimit = null, oraLimit = null;
      if (this.myPool.getRDBMS().equalsIgnoreCase("ORACLE")) {
        oraLimit = String.valueOf(limit + 1);
      } else {
        pgLimit = String.valueOf(limit + 1);
      }
      String strDateFormat = vars.getSessionValue("#AD_SqlDateFormat");
      data = MaterialReceiptPendingData.selectLines(this, strDateFormat, vars.getLanguage(),
          Utility.getContext(this, vars, "#User_Client", "MaterialReceiptPending"),
          Tree.getMembers(this, strTreeOrg, strAD_Org_ID),
          Utility.getContext(this, vars, "#User_Org", "MaterialReceiptPending"), strDateFrom,
          DateTimeData.nDaysAfter(this, strDateTo, "1"), strC_BPartner_ID, strDocumentNo, pgLimit,
          oraLimit);
    }

    boolean preference = UOMUtil.isUomManagementEnabled();
    xmlDocument.setParameter("paramIsUomEnabled", preference ? "Y" : "N");
    if (preference) {
      xmlDocument.setParameter("aumVisible", "table-cell");
      xmlDocument.setParameter("paramColSpanDocument", "5");
      xmlDocument.setParameter("paramColSpanDate", "3");
      xmlDocument.setParameter("paramColSpanBPartner", "4");
      xmlDocument.setParameter("paramColSpanCalendar", "2");
    } else {
      xmlDocument.setParameter("aumVisible", "none");
      xmlDocument.setParameter("paramColSpanDocument", "4");
      xmlDocument.setParameter("paramColSpanDate", "2");
      xmlDocument.setParameter("paramColSpanBPartner", "3");
      xmlDocument.setParameter("paramColSpanCalendar", "1");
    }

    FieldProvider[][] dataAum = new FieldProvider[data.length][];
    for (int i = 0; i < data.length; i++) {
      dataAum[i] = UOMUtil.selectAUM(data[i].mProductId, data[i].cDoctypeId);
      if (dataAum[i].length == 0) {
        dataAum[i] = UOMUtil.selectDefaultAUM(data[i].mProductId, data[i].cDoctypeId);
      }
    }

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "MaterialReceiptPending", false, "", "",
        "", false, "ad_forms", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    OBError myMessage = null;
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_forms.MaterialReceiptPending");
      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "MaterialReceiptPending.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "MaterialReceiptPending.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      myMessage = vars.getMessage("MaterialReceiptPending");
      if (limit > 0 && data.length > limit) {
        myMessage = new OBError();
        myMessage.setType("Warning");
        myMessage.setTitle("");
        String msgbody = Utility.messageBD(this, "OldFormsLimit", vars.getLanguage());
        msgbody = msgbody.replace("@limit@", Integer.toString(limit + 1));
        myMessage.setMessage(msgbody);
      }
      vars.removeMessage("MaterialReceiptPending");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("paramLanguage", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("paramBPartnerId", strC_BPartner_ID);
    xmlDocument.setParameter("paramAdOrgId", strAD_Org_ID);
    xmlDocument.setParameter("dateFrom", strDateFrom);
    xmlDocument.setParameter("dateTo", strDateTo);
    xmlDocument.setParameter("paramDocumentNo", strDocumentNo);
    xmlDocument.setParameter("paramBPartnerDescription",
        MaterialReceiptPendingData.bPartnerDescription(this, strC_BPartner_ID));
    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLEDIR", "AD_Org_ID", "",
          "AD_Org Security validation",
          Utility.getContext(this, vars, "#User_Org", "MaterialReceiptPending"),
          Utility.getContext(this, vars, "#User_Client", "MaterialReceiptPending"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "MaterialReceiptPending",
          strAD_Org_ID);
      xmlDocument.setData("reportAD_Org_ID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    // xmlDocument.setParameter("paramMessage",
    // (strMessage.equals("")?"":"alert('" + strMessage + "');"));
    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("displayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    if (commandIn.equals("GENERATE") && myMessage != null
        && !myMessage.getType().equals("Success")) {

      String strcOrderLineId = vars.getRequiredInStringParameter("inpOrder", IsIDFilter.instance);
      StringBuffer html = new StringBuffer();
      if (strcOrderLineId.startsWith("(")) {
        strcOrderLineId = strcOrderLineId.substring(1, strcOrderLineId.length() - 1);
      }
      if (!strcOrderLineId.equals("")) {
        strcOrderLineId = Replace.replace(strcOrderLineId, "'", "");
        StringTokenizer st = new StringTokenizer(strcOrderLineId, ",", false);
        html.append("\nfunction insertData() {\n");
        while (st.hasMoreTokens()) {
          String strOrderlineId = st.nextToken().trim();
          int i = 0;
          for (i = 0; i < data.length; i++) {
            if (data[i].id.equals(strOrderlineId)) {
              String strLocator = vars.getStringParameter("inpmLocatorId" + strOrderlineId);
              String strDateReceipt = vars
                  .getStringParameter("inpDateReceipt" + data[0].cBpartnerId);
              html.append("document.getElementsByName(\"" + "inpQtyordered" + strOrderlineId + "\""
                  + ")[0].value = " + "'"
                  + vars.getStringParameter("inpQtyordered" + strOrderlineId) + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId" + strOrderlineId + "\""
                  + ")[0].value = " + "'" + strLocator + "';\n");
              html.append("document.getElementsByName(\"" + "inpmLocatorId_D" + strOrderlineId
                  + "\"" + ")[0].value = '"
                  + MaterialReceiptPendingData.selectLocator(this, strLocator) + "';\n");
              html.append("document.getElementsByName(\"" + "inpDateReceipt" + data[0].cBpartnerId
                  + "\"" + ")[0].value = '" + strDateReceipt + "';\n");
              html.append(
                  "setCheckedValue(document.frmMain.inpOrder, '" + strOrderlineId + "');\n");
              break;
            }
          }
        }
        html.append("}\n");
      }
      xmlDocument.setParameter("script", html.toString());
    }

    xmlDocument.setData("structure1", data);
    xmlDocument.setDataArray("reportAUM_ID", "liststructure", dataAum);
    out.println(xmlDocument.print());
    out.close();
  }

  private OBError processPurchaseOrder(VariablesSecureApp vars, String strcOrderLineIdParam)
      throws IOException, ServletException {
    String localStrcOrderLineId = strcOrderLineIdParam;
    String strMessageResult = "";
    String strMessageType = "Success";
    String strWindowName = WindowTabsData.selectWindowInfo(this, vars.getLanguage(), "184");
    OBError myMessage = null;
    OBError myMessageAux = new OBError();
    myMessage = new OBError();
    myMessage.setTitle("");
    if (localStrcOrderLineId.equals("")) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
      return myMessage;
    }

    Connection conn = null;
    OBContext.setAdminMode();
    try {
      conn = this.getTransactionConnection();
      if (localStrcOrderLineId.startsWith("(")) {
        localStrcOrderLineId = localStrcOrderLineId.substring(1, localStrcOrderLineId.length() - 1);
      }
      if (!localStrcOrderLineId.equals("")) {
        localStrcOrderLineId = Replace.replace(localStrcOrderLineId, "'", "");
        StringTokenizer st = new StringTokenizer(localStrcOrderLineId, ",", false);
        String strmInoutId = "";
        String docTargetType = "";
        String docType = "";
        String strDocumentno = "";
        String strDateReceipt = "";
        String strLastBpartnerId = "";
        String strLastOrgId = "";
        int line = 0;
        while (st.hasMoreTokens()) {
          String strOrderlineId = st.nextToken().trim();
          MaterialReceiptPendingData[] data = MaterialReceiptPendingData.select(this,
              strOrderlineId);

          String strLocator = vars.getStringParameter("inpmLocatorId" + strOrderlineId);
          Locator locator = OBDal.getInstance().get(Locator.class, strLocator);
          Warehouse locatorWH = locator.getWarehouse();

          Organization orderOrganization = OBDal.getInstance()
              .get(Organization.class, data[0].adOrgId);

          boolean warehouseBelongsToOrg = false;
          List<OrgWarehouse> orderOrgWHList = orderOrganization.getOrganizationWarehouseList();
          for (OrgWarehouse orderOrgWH : orderOrgWHList) {
            if (orderOrgWH.getWarehouse().equals(locatorWH)) {
              warehouseBelongsToOrg = true;
              continue;
            }
          }
          String strIsSoTrx = vars.getSessionValue("isSOTrx");
          if (!warehouseBelongsToOrg && ("Y").equals(strIsSoTrx)) {
            OrderLine ol = OBDal.getInstance().get(OrderLine.class, strOrderlineId);
            myMessage.setType("Error");
            myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
            myMessage.setMessage(
                Utility.messageBD(this, "WarehouseNotAccessibleByOrg", vars.getLanguage()) + " "
                    + ol.getSalesOrder().getDocumentNo() + " - "
                    + MaterialReceiptPendingData.bPartnerDescription(this, data[0].cBpartnerId));
            return myMessage;
          }

          if (!strLastBpartnerId.equals(data[0].cBpartnerId)
              || !strLastOrgId.equals(data[0].adOrgId)) {
            if (!strmInoutId.equals("")) {
              myMessageAux = mInoutPost(conn, vars, strmInoutId);
              strMessageResult += strWindowName + " " + strDocumentno + ": "
                  + myMessageAux.getMessage() + "<br>";
              if (strMessageType.equals("Success")) {
                strMessageType = myMessageAux.getType();
              } else if (strMessageType.equals("Warning")
                  && myMessageAux.getType().equals("Error")) {
                strMessageType = "Error";
              }
            }
            line = 10;
            strmInoutId = SequenceIdData.getUUID();
            docTargetType = MaterialReceiptPendingData.cDoctypeTarget(this, vars.getClient(),
                data[0].adOrgId);
            docType = MaterialReceiptPendingData.cDoctypeId(this, docTargetType);
            strDocumentno = Utility.getDocumentNo(this, vars, "", "M_InOut", docTargetType, docType,
                false, true);
            strDateReceipt = vars.getStringParameter("inpDateReceipt" + data[0].cBpartnerId);

            if (strDateReceipt.equals("")) {
              myMessage.setType("Error");
              myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
              myMessage.setMessage(Utility.messageBD(this, "DateReceipt", vars.getLanguage()) + " "
                  + MaterialReceiptPendingData.bPartnerDescription(this, data[0].cBpartnerId));
              return myMessage;
            }

            OrganizationStructureProvider osp = OBContext.getOBContext()
                .getOrganizationStructureProvider(vars.getClient());
            boolean orgLegalWithAccounting = osp
                .getLegalEntityOrBusinessUnit(
                    OBDal.getInstance().get(Organization.class, data[0].adOrgId))
                .getOrganizationType()
                .isLegalEntityWithAccounting();
            org.openbravo.model.common.enterprise.DocumentType dt = OBDal.getInstance()
                .get(org.openbravo.model.common.enterprise.DocumentType.class, docTargetType);
            if (!FIN_Utility.isPeriodOpen(vars.getClient(), dt.getDocumentCategory(),
                data[0].adOrgId, strDateReceipt) && orgLegalWithAccounting) {
              myMessage.setType("Error");
              myMessage.setTitle(OBMessageUtils.messageBD("Error"));
              myMessage.setMessage(OBMessageUtils.messageBD("PeriodNotAvailable"));
              return myMessage;
            }

            try {
              MaterialReceiptPendingData.insert(conn, this, strmInoutId, vars.getClient(),
                  data[0].adOrgId, "Y", vars.getUser(), vars.getUser(), "N", strDocumentno, "CO",
                  "DR", "N", "N", "N", docTargetType, data[0].description, data[0].cOrderId,
                  data[0].dateordered, "N", "V+", strDateReceipt, strDateReceipt,
                  data[0].cBpartnerId, data[0].cBpartnerLocationId, data[0].mWarehouseId,
                  data[0].poreference, data[0].deliveryrule, data[0].freightcostrule,
                  data[0].freightamt, data[0].deliveryviarule, data[0].mShipperId,
                  data[0].cChargeId, data[0].chargeamt, data[0].priorityrule, "N", "N",
                  data[0].adUserId, data[0].salesrepId, data[0].adOrgtrxId, data[0].cProjectId,
                  data[0].cCampaignId, data[0].cActivityId, data[0].user1Id, data[0].user2Id, "N",
                  "N", data[0].cCostcenterId, data[0].aAssetId);
            } catch (ServletException ex) {
              myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
              releaseRollbackConnection(conn);
              return myMessage;
            }
          }
          strLastBpartnerId = data[0].cBpartnerId;
          strLastOrgId = data[0].adOrgId;
          String strQtyordered = vars.getNumericParameter("inpQtyordered" + strOrderlineId);
          String strSequenceLine = SequenceIdData.getUUID();
          String qtyorder = null;
          MaterialReceiptPendingLinesData[] dataLine = MaterialReceiptPendingLinesData.select(this,
              strOrderlineId);
          // Recalculate quantityorder only if qtyordered has been updated
          if (dataLine[0].quantityorder != "") {
            if (new BigDecimal(dataLine[0].qtyordered)
                .compareTo(new BigDecimal(strQtyordered)) == 0) {
              qtyorder = dataLine[0].quantityorder;
            } else {
              UOM uom = OBDal.getInstance().get(UOM.class, dataLine[0].cUomId);
              UOM orderUom = OBDal.getInstance()
                  .get(ProductUOM.class, dataLine[0].mProductUomId)
                  .getUOM();
              int stdPrecision = orderUom.getStandardPrecision().intValue();

              OBCriteria<UOMConversion> conversion = OBDal.getInstance()
                  .createCriteria(UOMConversion.class);
              conversion.add(Restrictions.eq(UOMConversion.PROPERTY_UOM, uom));
              conversion.add(Restrictions.eq(UOMConversion.PROPERTY_TOUOM, orderUom));
              List<UOMConversion> conversionList = conversion.list();
              Boolean useDivideRateBy = false;

              // Inverting search of UOM conversion if conversion list is empty
              if (conversionList.isEmpty()) {
                conversion = OBDal.getInstance().createCriteria(UOMConversion.class);
                conversion.add(Restrictions.eq(UOMConversion.PROPERTY_UOM, orderUom));
                conversion.add(Restrictions.eq(UOMConversion.PROPERTY_TOUOM, uom));
                conversionList = conversion.list();
                useDivideRateBy = true;
              }

              for (UOMConversion conv : conversionList) {
                if (!useDivideRateBy) {
                  qtyorder = new BigDecimal(strQtyordered).multiply(conv.getMultipleRateBy())
                      .setScale(stdPrecision, RoundingMode.HALF_UP)
                      .toString();
                } else {
                  qtyorder = new BigDecimal(strQtyordered).multiply(conv.getDivideRateBy())
                      .setScale(stdPrecision, RoundingMode.HALF_UP)
                      .toString();
                }
              }
            }
          }
          if (UOMUtil.isUomManagementEnabled() && dataLine[0].mProductUomId.isEmpty()) {
            dataLine[0].aumqty = vars.getStringParameter("inpAumQty" + strOrderlineId);
            strQtyordered = dataLine[0].aumqty;
            String defaultAum = vars.getStringParameter("inpcAUMId" + strOrderlineId);
            dataLine[0].cAum = defaultAum;
            if (!defaultAum.equals(dataLine[0].cUomId)) {
              strQtyordered = UOMUtil
                  .getConvertedQty(dataLine[0].mProductId, new BigDecimal(dataLine[0].aumqty),
                      defaultAum)
                  .toString();
            }
          }
          try {
            MaterialReceiptPendingLinesData.insert(conn, this, strSequenceLine, vars.getClient(),
                dataLine[0].adOrgId, "Y", vars.getUser(), vars.getUser(), String.valueOf(line),
                dataLine[0].description, strmInoutId, strOrderlineId, strLocator,
                dataLine[0].mProductId, dataLine[0].cUomId,
                new BigDecimal(strQtyordered).toString(), "N", dataLine[0].mAttributesetinstanceId,
                "N", qtyorder, dataLine[0].mProductUomId, dataLine[0].cProjectId,
                dataLine[0].user1Id, dataLine[0].user2Id, dataLine[0].cCostcenterId,
                dataLine[0].aAssetId, dataLine[0].cAum, dataLine[0].aumqty);
            MaterialReceiptPendingLinesData.updateInvoiceOrder(conn, this, strSequenceLine,
                strOrderlineId);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
          line += 10;
        }
        myMessageAux = mInoutPost(conn, vars, strmInoutId);
        strMessageResult += strWindowName + " " + strDocumentno + ": " + myMessageAux.getMessage();
        if (strMessageType.equals("Success")) {
          strMessageType = myMessageAux.getType();
        } else if (strMessageType.equals("Warning") && myMessageAux.getType().equals("Error")) {
          strMessageType = "Error";
        }
      }

      releaseCommitConnection(conn);
      myMessage.setType(strMessageType);
      myMessage.setTitle(myMessageAux.getTitle());
      myMessage.setMessage(strMessageResult);
    } catch (Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    } finally {
      OBContext.restorePreviousMode();
    }
    return myMessage;
  }

  private OBError mInoutPost(Connection conn, VariablesSecureApp vars, String strmInoutId)
      throws IOException, ServletException, SQLException {
    String pinstance = SequenceIdData.getUUID();

    OBError myMessage = null;
    myMessage = new OBError();
    try {
      PInstanceProcessData.insertPInstance(this, pinstance, "109", strmInoutId, "N", vars.getUser(),
          vars.getClient(), vars.getOrg());
    } catch (ServletException ex) {
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
      releaseRollbackConnection(conn);
      return myMessage;
    }
    MaterialReceiptPendingData.mInoutPost0(conn, this, pinstance);

    PInstanceProcessData[] pinstanceData = PInstanceProcessData.selectConnection(conn, this,
        pinstance);
    Pattern patern = Pattern.compile("^@ERROR=@Inline@\\s+[0-9]+\\s+@productWithoutAttributeSet@");
    for (PInstanceProcessData pinstanceD : pinstanceData) {
      if (patern.matcher(pinstanceD.errormsg).matches()) {
        pinstanceD.errormsg = "@productWithoutSomeAttributeSet@";
        pinstanceD.result = "2";
      }
    }
    myMessage = Utility.getProcessInstanceMessage(this, vars, pinstanceData);
    return myMessage;
  }

  @Override
  public String getServletInfo() {
    return "Servlet MaterialReceiptPending. This Servlet was made by Jon Alegría";
  } // end of getServletInfo() method
}
