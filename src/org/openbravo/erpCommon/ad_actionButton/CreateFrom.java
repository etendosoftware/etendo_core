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
 * Contributor(s):  Cheli Pineda__________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_actionButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.xmlEngine.XmlDocument;

public class CreateFrom extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final BigDecimal ZERO = BigDecimal.ZERO;

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    final VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      final String strKey = vars.getGlobalVariable("inpKey", "CreateFrom|key");
      final String strTableId = vars.getGlobalVariable("inpTableId", "CreateFrom|tableId");
      final String strProcessId = vars.getGlobalVariable("inpProcessId", "CreateFrom|processId",
          "");
      final String strPath = vars.getGlobalVariable("inpPath", "CreateFrom|path",
          strDireccion + request.getServletPath());
      final String strWindowId = vars.getGlobalVariable("inpWindowId", "CreateFrom|windowId", "");
      final String strTabName = vars.getGlobalVariable("inpTabName", "CreateFrom|tabName", "");
      final String strDateInvoiced = vars.getGlobalVariable("inpDateInvoiced",
          "CreateFrom|dateInvoiced", "");
      final String strBPartnerLocation = vars.getGlobalVariable("inpcBpartnerLocationId",
          "CreateFrom|bpartnerLocation", "");
      final String strMPriceList = vars.getGlobalVariable("inpMPricelist", "CreateFrom|pricelist",
          "");
      final String strBPartner = vars.getGlobalVariable("inpcBpartnerId", "CreateFrom|bpartner",
          "");
      final String strStatementDate = vars.getGlobalVariable("inpstatementdate",
          "CreateFrom|statementDate", "");
      final String strBankAccount = vars.getGlobalVariable("inpcBankaccountId",
          "CreateFrom|bankAccount", "");
      final String strOrg = vars.getGlobalVariable("inpadOrgId", "CreateFrom|adOrgId", "");
      final String strIsreceipt = vars.getGlobalVariable("inpisreceipt", "CreateFrom|isreceipt",
          "");

      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpadOrgId = " + strOrg);
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpisreceipt = " + strIsreceipt);
      }

      // 26-06-07
      vars.setSessionValue("CreateFrom|default", "1");

      printPage_FS(response, vars, strPath, strKey, strTableId, strProcessId, strWindowId,
          strTabName, strDateInvoiced, strBPartnerLocation, strMPriceList, strBPartner,
          strStatementDate, strBankAccount, strOrg, strIsreceipt);
    } else if (vars.commandIn("FRAME1")) {
      final String strTableId = vars.getGlobalVariable("inpTableId", "CreateFrom|tableId");
      final String strType = pageType(strTableId);
      final String strKey = vars.getGlobalVariable("inpKey", "CreateFrom" + strType + "|key");
      final String strProcessId = vars.getGlobalVariable("inpProcessId",
          "CreateFrom" + strType + "|processId", "");
      final String strPath = vars.getGlobalVariable("inpPath", "CreateFrom" + strType + "|path",
          strDireccion + request.getServletPath());
      final String strWindowId = vars.getGlobalVariable("inpWindowId",
          "CreateFrom" + strType + "|windowId");
      final String strTabName = vars.getGlobalVariable("inpTabName",
          "CreateFrom" + strType + "|tabName");
      final String strDateInvoiced = vars.getGlobalVariable("inpDateInvoiced",
          "CreateFrom" + strType + "|dateInvoiced", "");
      final String strBPartnerLocation = vars.getGlobalVariable("inpcBpartnerLocationId",
          "CreateFrom" + strType + "|bpartnerLocation", "");
      final String strPriceList = vars.getGlobalVariable("inpMPricelist",
          "CreateFrom" + strType + "|pricelist", "");
      final String strBPartner = vars.getGlobalVariable("inpcBpartnerId",
          "CreateFrom" + strType + "|bpartner", "");
      final String strStatementDate = vars.getGlobalVariable("inpstatementdate",
          "CreateFrom" + strType + "|statementDate", "");
      final String strBankAccount = vars.getGlobalVariable("inpcBankaccountId",
          "CreateFrom" + strType + "|bankAccount", "");
      final String strOrg = vars.getGlobalVariable("inpadOrgId",
          "CreateFrom" + strType + "|adOrgId", "");
      final String strIsreceipt = vars.getGlobalVariable("inpisreceipt",
          "CreateFrom" + strType + "|isreceipt", "");

      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpadOrgId = " + strOrg);
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpisreceipt = " + strIsreceipt);
      }

      vars.removeSessionValue("CreateFrom" + strType + "|key");
      vars.removeSessionValue("CreateFrom" + strType + "|processId");
      vars.removeSessionValue("CreateFrom" + strType + "|path");
      vars.removeSessionValue("CreateFrom" + strType + "|windowId");
      vars.removeSessionValue("CreateFrom" + strType + "|tabName");
      vars.removeSessionValue("CreateFrom" + strType + "|dateInvoiced");
      vars.removeSessionValue("CreateFrom" + strType + "|bpartnerLocation");
      vars.removeSessionValue("CreateFrom" + strType + "|pricelist");
      vars.removeSessionValue("CreateFrom" + strType + "|bpartner");
      vars.removeSessionValue("CreateFrom" + strType + "|statementDate");
      vars.removeSessionValue("CreateFrom" + strType + "|bankAccount");
      vars.removeSessionValue("CreateFrom" + strType + "|adOrgId");
      vars.removeSessionValue("CreateFrom" + strType + "|isreceipt");

      callPrintPage(response, vars, strPath, strKey, strTableId, strProcessId, strWindowId,
          strTabName, strDateInvoiced, strBPartnerLocation, strPriceList, strBPartner,
          strStatementDate, strBankAccount, strOrg, strIsreceipt);
    } else if (vars.commandIn("FIND_PO", "FIND_INVOICE")) {
      final String strKey = vars.getRequiredStringParameter("inpKey");
      final String strTableId = vars.getStringParameter("inpTableId");
      final String strProcessId = vars.getStringParameter("inpProcessId");
      final String strPath = vars.getStringParameter("inpPath",
          strDireccion + request.getServletPath());
      final String strWindowId = vars.getStringParameter("inpWindowId");
      final String strTabName = vars.getStringParameter("inpTabName");
      final String strDateInvoiced = vars.getStringParameter("inpDateInvoiced");
      final String strBPartnerLocation = vars.getStringParameter("inpcBpartnerLocationId");
      final String strPriceList = vars.getStringParameter("inpMPricelist");
      final String strBPartner = vars.getStringParameter("inpcBpartnerId");
      final String strStatementDate = vars.getStringParameter("inpstatementdate");
      final String strBankAccount = vars.getStringParameter("inpcBankaccountId");
      final String strOrg = vars.getStringParameter("inpadOrgId");
      final String strIsreceipt = vars.getStringParameter("inpisreceipt");
      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpadOrgId = " + strOrg);
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("doPost - inpisreceipt = " + strIsreceipt);
      }

      callPrintPage(response, vars, strPath, strKey, strTableId, strProcessId, strWindowId,
          strTabName, strDateInvoiced, strBPartnerLocation, strPriceList, strBPartner,
          strStatementDate, strBankAccount, strOrg, strIsreceipt);
    } else if (vars.commandIn("REFRESH_INVOICES")) {
      final String strBPartner = vars.getStringParameter("inpcBpartnerId");
      final String strWindowId = vars.getStringParameter("inpWindowId");
      final String strKey = vars.getRequiredStringParameter("inpKey");
      printPageInvoiceCombo(response, vars, strBPartner, strWindowId, strKey);
    } else if (vars.commandIn("SAVE")) {
      final String strProcessId = vars.getStringParameter("inpProcessId");
      final String strKey = vars.getRequiredStringParameter("inpKey");
      final String strTableId = vars.getStringParameter("inpTableId");
      final String strWindowId = vars.getStringParameter("inpWindowId");

      // Set this special case for auditing
      SessionInfo.setProcessType("CF");
      SessionInfo.setProcessId(strTableId);

      final OBError myMessage = saveMethod(vars, strKey, strTableId, strProcessId, strWindowId);
      final String strTabId = vars.getGlobalVariable("inpTabId", "CreateFrom|tabId");
      vars.setMessage(strTabId, myMessage);
      printPageClosePopUp(response, vars);
      vars.removeSessionValue("CreateFrom|key");
      vars.removeSessionValue("CreateFrom|processId");
      vars.removeSessionValue("CreateFrom|path");
      vars.removeSessionValue("CreateFrom|windowId");
      vars.removeSessionValue("CreateFrom|tabName");
      vars.removeSessionValue("CreateFrom|dateInvoiced");
      vars.removeSessionValue("CreateFrom|bpartnerLocation");
      vars.removeSessionValue("CreateFrom|pricelist");
      vars.removeSessionValue("CreateFrom|bpartner");
      vars.removeSessionValue("CreateFrom|statementDate");
      vars.removeSessionValue("CreateFrom|bankAccount");
      vars.removeSessionValue("CreateFrom|adOrgId");
      vars.removeSessionValue("CreateFrom|isreceipt");
      // response.sendRedirect(strPath);
    } else {
      pageErrorPopUp(response);
    }
  }

  private void printPage_FS(HttpServletResponse response, VariablesSecureApp vars, String strPath,
      String strKey, String strTableId, String strProcessId, String strWindowId, String strTabName,
      String strDateInvoiced, String strBPartnerLocation, String strPriceList, String strBPartner,
      String strStatementDate, String strBankAccount, String strOrg, String strIsreceipt)
      throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: FrameSet");
    }
    final XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_FS")
        .createXmlDocument();
    final String strType = pageType(strTableId);
    vars.setSessionValue("CreateFrom" + strType + "|path", strPath);
    vars.setSessionValue("CreateFrom" + strType + "|key", strKey);
    vars.setSessionValue("CreateFrom" + strType + "|processId", strProcessId);
    vars.setSessionValue("CreateFrom" + strType + "|windowId", strWindowId);
    vars.setSessionValue("CreateFrom" + strType + "|tabName", strTabName);
    vars.setSessionValue("CreateFrom" + strType + "|dateInvoiced", strDateInvoiced);
    vars.setSessionValue("CreateFrom" + strType + "|bpartnerLocation", strBPartnerLocation);
    vars.setSessionValue("CreateFrom" + strType + "|pricelist", strPriceList);
    vars.setSessionValue("CreateFrom" + strType + "|bpartner", strBPartner);
    vars.setSessionValue("CreateFrom" + strType + "|statementDate", strStatementDate);
    vars.setSessionValue("CreateFrom" + strType + "|bankAccount", strBankAccount);
    vars.setSessionValue("CreateFrom" + strType + "|adOrgId", strOrg);
    vars.setSessionValue("CreateFrom" + strType + "|isreceipt", strIsreceipt);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private String pageType(String strTableId) {
    if (strTableId.equals("319")) {
      return "Shipment";
    } else {
      return "";
    }
  }

  void callPrintPage(HttpServletResponse response, VariablesSecureApp vars, String strPath,
      String strKey, String strTableId, String strProcessId, String strWindowId, String strTabName,
      String strDateInvoiced, String strBPartnerLocation, String strPriceList, String strBPartner,
      String strStatementDate, String strBankAccount, String strOrg, String strIsreceipt)
      throws IOException, ServletException {
    OBContext.setAdminMode();
    try {
      if (strTableId.equals("319")) { // M_InOut
        printPageShipment(response, vars, strPath, strKey, strTableId, strProcessId, strWindowId,
            strTabName, strBPartner);
      } else {
        pageError(response);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected void printPageShipment(HttpServletResponse response, VariablesSecureApp vars,
      String strPath, String strKey, String strTableId, String strProcessId, String strWindowId,
      String strTabName, String strBPartner) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: Shipment");
    }
    CreateFromShipmentData[] data = null;
    XmlDocument xmlDocument;
    String strPO = vars.getStringParameter("inpPurchaseOrder");
    String strInvoice = vars.getStringParameter("inpInvoice");
    final String strLocator = vars.getStringParameter("inpmLocatorId");
    final String isSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
    if (vars.commandIn("FIND_PO")) {
      strInvoice = "";
    } else if (vars.commandIn("FIND_INVOICE")) {
      strPO = "";
    }
    if (strPO.equals("") && strInvoice.equals("")) {
      final String[] discard = { "sectionDetail" };
      if (isSOTrx.equals("Y")) {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_Shipment", discard)
            .createXmlDocument();
      } else {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_ShipmentPO",
                discard)
            .createXmlDocument();
      }
      data = CreateFromShipmentData.set();
    } else {
      if (isSOTrx.equals("Y")) {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_Shipment")
            .createXmlDocument();
      } else {
        xmlDocument = xmlEngine
            .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_ShipmentPO")
            .createXmlDocument();
      }
      if (strInvoice.equals("")) {
        if (vars.getLanguage().equals("en_US")) {
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromPOSOTrx(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strPO);
          } else {
            data = CreateFromShipmentData.selectFromPO(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strPO);
          }
        } else {
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromPOTrlSOTrx(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strPO);
          } else {
            data = CreateFromShipmentData.selectFromPOTrl(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strPO);
          }
        }
      } else {
        if (vars.getLanguage().equals("en_US")) {
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromInvoiceTrx(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strInvoice);
          } else {
            data = CreateFromShipmentData.selectFromInvoice(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strInvoice);
          }
        } else {
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromInvoiceTrx(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strInvoice);
          } else {
            data = CreateFromShipmentData.selectFromInvoiceTrl(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strInvoice);
          }
        }
      }
    }

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("path", strPath);
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("tableId", strTableId);
    xmlDocument.setParameter("processId", strProcessId);
    xmlDocument.setParameter("cBpartnerId", strBPartner);
    xmlDocument.setParameter("BPartnerDescription",
        CreateFromShipmentData.selectBPartner(this, strBPartner));
    xmlDocument.setParameter("PurchaseOrder", strPO);
    xmlDocument.setParameter("M_Locator_ID", strLocator);
    xmlDocument.setParameter("M_Locator_ID_DES",
        CreateFromShipmentData.selectLocator(this, strLocator));
    xmlDocument.setParameter("Invoice", strInvoice);
    xmlDocument.setParameter("pType",
        (!strInvoice.equals("") ? "INVOICE" : (!strPO.equals("")) ? "PO" : ""));
    xmlDocument.setParameter("windowId", strWindowId);
    xmlDocument.setParameter("tabName", strTabName);

    if (strBPartner.equals("")) {
      xmlDocument.setData("reportInvoice", "liststructure", new CreateFromShipmentData[0]);
      xmlDocument.setData("reportPurchaseOrder", "liststructure", new CreateFromShipmentData[0]);
    } else {
      if (isSOTrx.equals("Y")) {
        xmlDocument.setData("reportInvoice", "liststructure",
            CreateFromShipmentData.selectFromInvoiceTrxCombo(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strBPartner));
        xmlDocument.setData("reportPurchaseOrder", "liststructure",
            CreateFromShipmentData.selectFromPOSOTrxCombo(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strBPartner));
      } else {
        xmlDocument.setData("reportInvoice", "liststructure",
            CreateFromShipmentData.selectFromInvoiceCombo(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strBPartner));
        xmlDocument.setData("reportPurchaseOrder", "liststructure",
            CreateFromShipmentData.selectFromPOCombo(this, vars.getLanguage(),
                Utility.getContext(this, vars, "#User_Client", strWindowId),
                Utility.getContext(this, vars, "#User_Org", strWindowId), strBPartner));
      }
    }

    {
      final OBError myMessage = vars.getMessage("CreateFrom");
      vars.removeMessage("CreateFrom");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }

    final boolean strUomPreference = UOMUtil.isUomManagementEnabled();
    if (isSOTrx.equals("Y")) {
      if (strUomPreference) {
        xmlDocument.setParameter("uompreference", "");
        for (int i = 0; i < data.length; i++) {
          // Obtain the specific units for each product
          data[i].haveuompreference = "";
          if (data[i].cAum.isEmpty() && data[i].aumqty.isEmpty()) {
            FieldProvider[] defaultAumData = UOMUtil.selectDefaultAUM(data[i].mProductId,
                data[i].cDoctypeId);
            String defaultAum = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_ID)
                : data[i].cUomId;
            data[i].cAum = defaultAum;
            data[i].aumname = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_NAME)
                : data[i].uomsymbol;
            data[i].mProductUomId = null;
            if (!defaultAum.equals(data[i].cUomId)) {
              data[i].aumqty = UOMUtil
                  .getConvertedAumQty(data[i].mProductId, new BigDecimal(data[i].qty), defaultAum)
                  .toString();
            } else {
              data[i].aumqty = data[i].qty;
            }
          }
        }
      } else {
        xmlDocument.setParameter("uompreference", "display:none;");
        for (int i = 0; i < data.length; i++) {
          data[i].haveuompreference = "display:none;";
        }
      }
    } else {
      final FieldProvider[][] dataUOM = new FieldProvider[data.length][];
      boolean strHaveSecUom = false;
      boolean strHaveAum = false;
      for (int i = 0; i < data.length; i++) {
        try {
          if (!data[i].havesec.equals("0")) {
            strHaveSecUom = true;
          }
          if (strUomPreference && data[i].havesec.equals("0")) {
            strHaveAum = true;
          }
          if (strHaveSecUom && strHaveAum) {
            break;
          }
        } catch (NullPointerException ignore) {

        }
      }

      for (int i = 0; i < data.length; i++) {
        // Obtain the specific units for each product

        dataUOM[i] = UOMUtil.selectUOM(data[i].mProductId);

        // Check the hidden fields

        final String strhavesec = data[i].havesec;

        if (strHaveSecUom && strHaveAum) {
          // UOM preference is Y and at least one line has secondary UOM
          if (!data[i].havesec.equals("0")) {
            data[i].havesec = "text";
            data[i].haveuompreference = "hidden";
          }
          if (strUomPreference && data[i].havesec.equals("0")) {
            data[i].haveuompreference = "text";
            data[i].havesec = "hidden";
          }
          xmlDocument.setParameter("uompreference", "");
          xmlDocument.setParameter("havesecuom", "");
          if (data[i].cAum.isEmpty() && data[i].aumqty.isEmpty()
              && data[i].secProductUomId.isEmpty() && data[i].secqty.isEmpty()) {
            FieldProvider[] defaultAumData = UOMUtil.selectDefaultAUM(data[i].mProductId,
                data[i].cDoctypeId);
            String defaultAum = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_ID)
                : data[i].cUomId;
            data[i].cAum = defaultAum;
            data[i].aumname = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_NAME)
                : data[i].uomsymbol;
            data[i].mProductUomId = null;
            if (!defaultAum.equals(data[i].cUomId)) {
              data[i].aumqty = UOMUtil
                  .getConvertedAumQty(data[i].mProductId, new BigDecimal(data[i].qty), defaultAum)
                  .toString();
            } else {
              data[i].aumqty = data[i].qty;
            }
          }
        } else if (strUomPreference && "0".equals(strhavesec)) {
          // UOM preference is Y and no line has secondary UOM
          data[i].havesec = "hidden";
          data[i].havesecuom = "none";
          data[i].haveuompreference = "text";
          xmlDocument.setParameter("uompreference", "");
          xmlDocument.setParameter("havesecuom", "display:none;");
          if (data[i].cAum.isEmpty() && data[i].aumqty.isEmpty()) {
            FieldProvider[] defaultAumData = UOMUtil.selectDefaultAUM(data[i].mProductId,
                data[i].cDoctypeId);
            String defaultAum = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_ID)
                : data[i].cUomId;
            data[i].cAum = defaultAum;
            data[i].aumname = (defaultAumData.length > 0)
                ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_NAME)
                : data[i].uomsymbol;
            data[i].mProductUomId = null;
            if (!defaultAum.equals(data[i].cUomId)) {
              data[i].aumqty = UOMUtil
                  .getConvertedAumQty(data[i].mProductId, new BigDecimal(data[i].qty), defaultAum)
                  .toString();
            } else {
              data[i].aumqty = data[i].qty;
            }
          }
        } else {
          // UOM preference is N
          data[i].havesec = "text";
          data[i].uompreference = "none";
          data[i].haveuompreference = "hidden";
          xmlDocument.setParameter("uompreference", "display:none;");
          xmlDocument.setParameter("havesecuom", "");
        }
      }
      xmlDocument.setDataArray("reportM_Product_Uom_To_ID", "liststructure", dataUOM);
    }
    xmlDocument.setData("structure1", data);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  void printPageInvoiceCombo(HttpServletResponse response, VariablesSecureApp vars,
      String strBPartner, String strWindowId, String strKey) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Output: Refresh Invoices");
    }
    final XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CreateFrom_F0")
        .createXmlDocument();
    String strArray = "";
    String strArray2 = "";
    final String isSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);

    if (strBPartner.equals("")) {
      strArray = Utility.arrayEntradaSimple("arrDatos", new CreateFromShipmentData[0]);
      strArray2 = Utility.arrayEntradaSimple("arrDatos2", new CreateFromShipmentData[0]);
    } else {
      if (vars.commandIn("REFRESH_INVOICES")) { // Loading the combos in
        // the delivery note's
        // CreateFrom
        if (isSOTrx.equals("Y")) {
          strArray = Utility.arrayEntradaSimple("arrDatos", new CreateFromShipmentData[0]);
          strArray2 = Utility.arrayEntradaSimple("arrDatos2",
              CreateFromShipmentData.selectFromPOSOTrxCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner));
        } else {
          strArray = Utility.arrayEntradaSimple("arrDatos",
              CreateFromShipmentData.selectFromInvoiceCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner));
          strArray2 = Utility.arrayEntradaSimple("arrDatos2",
              CreateFromShipmentData.selectFromPOCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner));
        }
      } else { // Loading the Combos in the Invoice's CreateFrom
        Invoice invoice = OBDal.getInstance().get(Invoice.class, strKey);
        String strIsTaxIncluded = invoice.getPriceList().isPriceIncludesTax() ? "Y" : "N";
        String invoiceCurrencyId = invoice.getCurrency().getId();

        if (isSOTrx.equals("Y")) {
          strArray = Utility.arrayEntradaSimple("arrDatos",
              CreateFromInvoiceData.selectFromShipmentSOTrxCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner,
                  strIsTaxIncluded, invoiceCurrencyId));
          strArray2 = Utility.arrayEntradaSimple("arrDatos2",
              CreateFromInvoiceData.selectFromPOSOTrxCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner,
                  strIsTaxIncluded, invoiceCurrencyId));
        } else {
          strArray = Utility.arrayEntradaSimple("arrDatos",
              CreateFromInvoiceData.selectFromShipmentCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner,
                  strIsTaxIncluded, invoiceCurrencyId));
          strArray2 = Utility.arrayEntradaSimple("arrDatos2",
              CreateFromInvoiceData.selectFromPOCombo(this, vars.getLanguage(),
                  Utility.getContext(this, vars, "#User_Client", strWindowId),
                  Utility.getContext(this, vars, "#AccessibleOrgTree", strWindowId), strBPartner,
                  strIsTaxIncluded, invoiceCurrencyId));
        }
      }
    }

    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("array", strArray + "\n" + strArray2);
    response.setContentType("text/html; charset=UTF-8");
    final PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  OBError saveMethod(VariablesSecureApp vars, String strKey, String strTableId, String strProcessId,
      String strWindowId) throws IOException, ServletException {
    OBContext.setAdminMode();
    try {
      if (strTableId.equals("319")) {
        return saveShipment(vars, strKey, strTableId, strProcessId, strWindowId);
      } else {
        return null;
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  protected OBError saveShipment(VariablesSecureApp vars, String strKey, String strTableId,
      String strProcessId, String strWindowId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Save: Shipment");
    }
    final String isSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
    if (isSOTrx.equals("Y")) {
      return saveShipmentSO(vars, strKey, strTableId, strProcessId, strWindowId);
    } else {
      return saveShipmentPO(vars, strKey, strTableId, strProcessId, strWindowId);
    }
  }

  protected OBError saveShipmentPO(VariablesSecureApp vars, String strKey, String strTableId,
      String strProcessId, String strWindowId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Save: Shipment");
    }
    final String strLocatorCommon = vars.getStringParameter("inpmLocatorId");
    final String strType = vars.getRequiredStringParameter("inpType");
    final String strClaves = Utility
        .stringList(vars.getRequiredInParameter("inpId", IsIDFilter.instance));
    final String isSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
    String strInvoice = "", strPO = "";
    CreateFromShipmentData[] data = null;
    OBError myMessage = null;
    Connection conn = null;
    String[] ids = restrictParameter(strClaves);
    try {
      conn = this.getTransactionConnection();
      for (int k = 0; k < ids.length; k++) {
        if (strType.equals("INVOICE")) {
          strInvoice = vars.getStringParameter("inpInvoice");
          if (!isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromInvoiceUpdate(conn, this, vars.getLanguage(),
                ids[k]);
          }
        } else {
          strPO = vars.getStringParameter("inpPurchaseOrder");
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromPOUpdateSOTrx(conn, this, vars.getLanguage(),
                ids[k]);
          } else {
            data = CreateFromShipmentData.selectFromPOUpdate(conn, this, vars.getLanguage(),
                ids[k]);
          }
        }
        if (data != null) {
          for (int i = 0; i < data.length; i++) {

            // Obtain the values from the window

            String strLineId = "";

            if (strType.equals("INVOICE")) {
              strLineId = data[i].cInvoicelineId;
            } else {
              strLineId = data[i].cOrderlineId;
            }

            String strMovementqty = "";
            String strAumQty = "";
            if (UOMUtil.isUomManagementEnabled() && data[i].mProductUomId.isEmpty()) {
              try {
                if (data[i].cAum.isEmpty()) {
                  data[i].cAum = getDefaultAUMForData(data[i]);
                }
                strAumQty = vars.getNumericParameter("inpaumqty" + strLineId);
                if (StringUtils.isNotEmpty(strAumQty)) {
                  data[i].aumqty = strAumQty;
                } else if (data[i].aumqty.isEmpty()) {
                  data[i].aumqty = getConvertedAUMQtyForData(data[i]);
                }
                BigDecimal qtyAum = new BigDecimal(data[i].aumqty);
                strAumQty = qtyAum.toString();
                strMovementqty = qtyAum.toString();
                if (!data[i].cUomId.equals(data[i].cAum)) {
                  strMovementqty = UOMUtil.getConvertedQty(data[i].mProductId, qtyAum, data[i].cAum)
                      .toString();
                }
              } catch (NumberFormatException e) {
                log4j.debug(e.getMessage());
              }
            } else {
              strMovementqty = vars.getRequiredNumericParameter("inpmovementqty" + strLineId);
            }
            String strQuantityorder = "";
            String strProductUomId = "";
            String strLocator = vars.getStringParameter("inpmLocatorId" + strLineId);
            final String strmAttributesetinstanceId = vars
                .getStringParameter("inpmAttributesetinstanceId" + strLineId);
            String strbreakdown = "";
            CreateFromShipmentData[] dataUomIdConversion = null;

            if ("".equals(strLocator)) {
              strLocator = strLocatorCommon;
            }

            if ("".equals(data[i].mProductUomId)) {
              strQuantityorder = "";
              strProductUomId = "";
            } else {
              strQuantityorder = vars.getRequiredStringParameter("inpquantityorder" + strLineId);
              strProductUomId = vars.getRequiredStringParameter("inpmProductUomId" + strLineId);
              dataUomIdConversion = CreateFromShipmentData.selectcUomIdConversion(this,
                  strProductUomId);

              if (dataUomIdConversion == null || dataUomIdConversion.length == 0) {
                dataUomIdConversion = CreateFromShipmentData.set();
                strbreakdown = "N";
              } else {
                strbreakdown = dataUomIdConversion[0].breakdown;
              }
            }

            //

            String strMultiplyRate = "";
            int stdPrecision = 0;
            if ("Y".equals(strbreakdown)) {
              if (dataUomIdConversion[0].cUomIdConversion.equals("")) {
                releaseRollbackConnection(conn);
                myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                    "ProcessRunError");
                return myMessage;
              }
              final String strInitUOM = dataUomIdConversion[0].cUomIdConversion;
              final String strUOM = data[i].cUomId;
              if (strInitUOM.equals(strUOM)) {
                strMultiplyRate = "1";
              } else {
                strMultiplyRate = CreateFromShipmentData.multiplyRate(this, strInitUOM, strUOM);
              }
              if (strMultiplyRate.equals("")) {
                strMultiplyRate = CreateFromShipmentData.divideRate(this, strUOM, strInitUOM);
              }
              if (strMultiplyRate.equals("")) {
                strMultiplyRate = "1";
                releaseRollbackConnection(conn);
                myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                    "ProcessRunError");
                return myMessage;
              }
              stdPrecision = Integer.valueOf(dataUomIdConversion[0].stdprecision).intValue();
              BigDecimal quantity, qty, multiplyRate;

              multiplyRate = new BigDecimal(strMultiplyRate);
              qty = new BigDecimal(strMovementqty);
              boolean qtyIsNegative = false;
              if (qty.compareTo(ZERO) < 0) {
                qtyIsNegative = true;
                qty = qty.negate();
              }
              quantity = qty.multiply(multiplyRate);
              if (quantity.scale() > stdPrecision) {
                quantity = quantity.setScale(stdPrecision, RoundingMode.HALF_UP);
              }
              while (qty.compareTo(ZERO) > 0) {
                String total = "1";
                BigDecimal conversion;
                if (quantity.compareTo(BigDecimal.ONE) < 0) {
                  total = quantity.toString();
                  conversion = qty;
                  quantity = ZERO;
                  qty = ZERO;
                } else {
                  conversion = multiplyRate;
                  if (conversion.compareTo(qty) > 0) {
                    conversion = qty;
                    qty = ZERO;
                  } else {
                    qty = qty.subtract(conversion);
                  }
                  quantity = quantity.subtract(BigDecimal.ONE);
                }
                final String strConversion = conversion.toString();
                final String strSequence = SequenceIdData.getUUID();
                try {
                  CreateFromShipmentData.insert(conn, this, strSequence, strKey, vars.getClient(),
                      data[i].adOrgId, vars.getUser(), data[i].description, data[i].mProductId,
                      data[i].cUomId, (qtyIsNegative ? "-" + strConversion : strConversion),
                      strAumQty, data[i].cAum, data[i].cOrderlineId, strLocator,
                      CreateFromShipmentData.isInvoiced(conn, this, data[i].cInvoicelineId),
                      (qtyIsNegative ? "-" + total : total), data[i].mProductUomId,
                      strmAttributesetinstanceId, data[i].aAssetId, data[i].cProjectId,
                      data[i].cCostcenterId, data[i].user1Id, data[i].user2Id, data[i].cBpartnerId,
                      data[i].explode, data[i].isorder);

                  if (strType.equals("INVOICE") && !data[i].cInvoicelineId.isEmpty()) {
                    CreateFromShipmentData.insertInvoiceAcctDimension(conn, this, strSequence,
                        vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cInvoicelineId);
                  } else if (!data[i].cOrderlineId.isEmpty()) {
                    CreateFromShipmentData.insertAcctDimension(conn, this, strSequence,
                        vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cOrderlineId);
                  }
                  if (!strInvoice.equals("")) {
                    CreateFromShipmentData.updateInvoice(conn, this, strSequence,
                        data[i].cInvoicelineId);
                  } else {
                    CreateFromShipmentData.updateInvoiceOrder(conn, this, strSequence,
                        data[i].cOrderlineId);
                  }
                } catch (final ServletException ex) {
                  myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                      ex.getMessage());
                  releaseRollbackConnection(conn);
                  return myMessage;
                }
              }
            } else {
              final String strSequence = SequenceIdData.getUUID();
              try {
                CreateFromShipmentData.insert(conn, this, strSequence, strKey, vars.getClient(),
                    data[i].adOrgId, vars.getUser(), data[i].description, data[i].mProductId,
                    data[i].cUomId, strMovementqty, strAumQty, data[i].cAum, data[i].cOrderlineId,
                    strLocator,
                    CreateFromShipmentData.isInvoiced(conn, this, data[i].cInvoicelineId),
                    strQuantityorder, strProductUomId, strmAttributesetinstanceId, data[i].aAssetId,
                    data[i].cProjectId, data[i].cCostcenterId, data[i].user1Id, data[i].user2Id,
                    data[i].cBpartnerId, data[i].explode, data[i].isorder);

                if (strType.equals("INVOICE") && !data[i].cInvoicelineId.isEmpty()) {
                  CreateFromShipmentData.insertInvoiceAcctDimension(conn, this, strSequence,
                      vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cInvoicelineId);
                } else if (!data[i].cOrderlineId.isEmpty()) {
                  CreateFromShipmentData.insertAcctDimension(conn, this, strSequence,
                      vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cOrderlineId);
                }
                if (!strInvoice.equals("")) {
                  String strInOutLineId = CreateFromShipmentData.selectInvoiceInOut(conn, this,
                      data[i].cInvoicelineId);
                  if (strInOutLineId.isEmpty()) {
                    CreateFromShipmentData.updateInvoice(conn, this, strSequence,
                        data[i].cInvoicelineId);
                    CreateFromShipmentData.updateBOMStructure(conn, this, strKey, strSequence);
                  } else {
                    CreateFromShipmentData.insertMatchInv(conn, this, vars.getUser(),
                        data[i].cInvoicelineId, strSequence, data[i].cInvoiceId);
                  }
                } else {
                  CreateFromShipmentData.updateInvoiceOrder(conn, this, strSequence,
                      data[i].cOrderlineId);
                }
              } catch (final ServletException ex) {
                myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
                releaseRollbackConnection(conn);
                return myMessage;
              }
            }
          }
        }

        if (!strPO.equals("")) {
          try {
            final int total = CreateFromShipmentData.deleteC_Order_ID(conn, this, strKey, strPO);
            if (total == 0) {
              int noOfOrders = Integer
                  .valueOf(CreateFromShipmentData.countOrders(conn, this, strKey));
              if (noOfOrders == 1) {
                CreateFromShipmentData.updateC_Order_ID(conn, this, strPO, strKey);
              }
            }
          } catch (final ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }
        if (!strInvoice.equals("")) {
          try {
            final int total = CreateFromShipmentData.deleteC_Invoice_ID(conn, this, strKey,
                strInvoice);
            if (total == 0) {
              CreateFromShipmentData.updateC_Invoice_ID(conn, this, strInvoice, strKey);
            }
          } catch (final ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }
      }
      releaseCommitConnection(conn);
      if (log4j.isDebugEnabled()) {
        log4j.debug("Save commit");
      }
      myMessage = new OBError();
      myMessage.setType("Success");
      myMessage.setTitle("");
      myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    } catch (final Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (final Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }
    return myMessage;
  }

  private String getConvertedAUMQtyForData(CreateFromShipmentData data) {
    return UOMUtil.getConvertedAumQty(data.mProductId, new BigDecimal(data.id), data.cAum)
        .toString();
  }

  private String getDefaultAUMForData(CreateFromShipmentData data) {
    FieldProvider[] defaultAumData = UOMUtil.selectDefaultAUM(data.mProductId, data.cDoctypeId);
    return (defaultAumData.length > 0) ? defaultAumData[0].getField(UOMUtil.FIELD_PROVIDER_ID)
        : data.cUomId;
  }

  protected OBError saveShipmentSO(VariablesSecureApp vars, String strKey, String strTableId,
      String strProcessId, String strWindowId) throws IOException, ServletException {
    if (log4j.isDebugEnabled()) {
      log4j.debug("Save: Shipment");
    }
    final String strLocator = vars.getRequiredStringParameter("inpmLocatorId");
    final String strType = vars.getRequiredStringParameter("inpType");
    final String strClaves = Utility
        .stringList(vars.getRequiredInParameter("inpId", IsIDFilter.instance));
    final String isSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
    String strInvoice = "", strPO = "";
    CreateFromShipmentData[] data = null;
    OBError myMessage = null;
    Connection conn = null;
    String[] ids = restrictParameter(strClaves);
    try {
      conn = this.getTransactionConnection();
      for (int k = 0; k < ids.length; k++) {
        if (strType.equals("INVOICE")) {
          strInvoice = vars.getStringParameter("inpInvoice");
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromInvoiceTrxUpdate(conn, this, vars.getLanguage(),
                ids[k]);
          } else {
            data = CreateFromShipmentData.selectFromInvoiceUpdate(conn, this, vars.getLanguage(),
                ids[k]);
          }
        } else {
          strPO = vars.getStringParameter("inpPurchaseOrder");
          if (isSOTrx.equals("Y")) {
            data = CreateFromShipmentData.selectFromPOUpdateSOTrx(conn, this, vars.getLanguage(),
                ids[k]);
          } else {
            data = CreateFromShipmentData.selectFromPOUpdate(conn, this, vars.getLanguage(),
                ids[k]);
          }
        }
        if (data != null) {
          for (int i = 0; i < data.length; i++) {
            String strMultiplyRate = "";
            int stdPrecision = 0;
            if (data[i].breakdown.equals("Y")) {
              if (data[i].cUomIdConversion.equals("")) {
                releaseRollbackConnection(conn);
                myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                    "ProcessRunError");
                return myMessage;
              }
              final String strInitUOM = data[i].cUomIdConversion;
              final String strUOM = data[i].cUomId;
              if (strInitUOM.equals(strUOM)) {
                strMultiplyRate = "1";
              } else {
                strMultiplyRate = CreateFromShipmentData.multiplyRate(this, strInitUOM, strUOM);
              }
              if (strMultiplyRate.equals("")) {
                strMultiplyRate = CreateFromShipmentData.divideRate(this, strUOM, strInitUOM);
              }
              if (strMultiplyRate.equals("")) {
                strMultiplyRate = "1";
                releaseRollbackConnection(conn);
                myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                    "ProcessRunError");
                return myMessage;
              }
              stdPrecision = Integer.valueOf(data[i].stdprecision).intValue();
              BigDecimal quantity, qty, multiplyRate;

              multiplyRate = new BigDecimal(strMultiplyRate);
              qty = new BigDecimal(data[i].id);
              boolean qtyIsNegative = false;
              if (qty.compareTo(ZERO) < 0) {
                qtyIsNegative = true;
                qty = qty.negate();
              }
              quantity = qty.multiply(multiplyRate);
              if (quantity.scale() > stdPrecision) {
                quantity = quantity.setScale(stdPrecision, RoundingMode.HALF_UP);
              }
              while (qty.compareTo(ZERO) > 0) {
                String total = "1";
                BigDecimal conversion;
                if (quantity.compareTo(BigDecimal.ONE) < 0) {
                  total = quantity.toString();
                  conversion = qty;
                  quantity = ZERO;
                  qty = ZERO;
                } else {
                  conversion = multiplyRate;
                  if (conversion.compareTo(qty) > 0) {
                    conversion = qty;
                    qty = ZERO;
                  } else {
                    qty = qty.subtract(conversion);
                  }
                  quantity = quantity.subtract(BigDecimal.ONE);
                }
                final String strConversion = conversion.toString();
                final String strSequence = SequenceIdData.getUUID();
                try {
                  CreateFromShipmentData.insert(conn, this, strSequence, strKey, vars.getClient(),
                      data[i].adOrgId, vars.getUser(), data[i].description, data[i].mProductId,
                      data[i].cUomId, (qtyIsNegative ? "-" + strConversion : strConversion),
                      data[i].aumqty, data[i].cAum, data[i].cOrderlineId, strLocator,
                      CreateFromShipmentData.isInvoiced(conn, this, data[i].cInvoicelineId),
                      (qtyIsNegative ? "-" + total : total), data[i].mProductUomId,
                      data[i].mAttributesetinstanceId, data[i].aAssetId, data[i].cProjectId,
                      data[i].cCostcenterId, data[i].user1Id, data[i].user2Id, data[i].cBpartnerId,
                      data[i].explode, data[i].isorder);

                  if (strType.equals("INVOICE") && !data[i].cInvoicelineId.isEmpty()) {
                    CreateFromShipmentData.insertInvoiceAcctDimension(conn, this, strSequence,
                        vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cInvoicelineId);
                  } else if (!data[i].cOrderlineId.isEmpty()) {
                    CreateFromShipmentData.insertAcctDimension(conn, this, strSequence,
                        vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cOrderlineId);
                  }

                  if (!strInvoice.equals("")) {
                    CreateFromShipmentData.updateInvoice(conn, this, strSequence,
                        data[i].cInvoicelineId);
                  } else {
                    CreateFromShipmentData.updateInvoiceOrder(conn, this, strSequence,
                        data[i].cOrderlineId);
                  }
                } catch (final ServletException ex) {
                  myMessage = Utility.translateError(this, vars, vars.getLanguage(),
                      ex.getMessage());
                  releaseRollbackConnection(conn);
                  return myMessage;
                }
              }
            } else {
              final String strSequence = SequenceIdData.getUUID();
              try {
                CreateFromShipmentData.insert(conn, this, strSequence, strKey, vars.getClient(),
                    data[i].adOrgId, vars.getUser(), data[i].description, data[i].mProductId,
                    data[i].cUomId, data[i].id, data[i].aumqty, data[i].cAum, data[i].cOrderlineId,
                    strLocator,
                    CreateFromShipmentData.isInvoiced(conn, this, data[i].cInvoicelineId),
                    data[i].quantityorder, data[i].mProductUomId, data[i].mAttributesetinstanceId,
                    data[i].aAssetId, data[i].cProjectId, data[i].cCostcenterId, data[i].user1Id,
                    data[i].user2Id, data[i].cBpartnerId, data[i].explode, data[i].isorder);

                if (strType.equals("INVOICE") && !data[i].cInvoicelineId.isEmpty()) {
                  CreateFromShipmentData.insertInvoiceAcctDimension(conn, this, strSequence,
                      vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cInvoicelineId);
                } else if (!data[i].cOrderlineId.isEmpty()) {
                  CreateFromShipmentData.insertAcctDimension(conn, this, strSequence,
                      vars.getClient(), data[i].adOrgId, vars.getUser(), data[i].cOrderlineId);
                }

                if (!strInvoice.equals("")) {
                  CreateFromShipmentData.updateInvoice(conn, this, strSequence,
                      data[i].cInvoicelineId);
                  CreateFromShipmentData.updateBOMStructure(conn, this, strKey, strSequence);
                } else {
                  CreateFromShipmentData.updateInvoiceOrder(conn, this, strSequence,
                      data[i].cOrderlineId);
                }
              } catch (final ServletException ex) {
                myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
                releaseRollbackConnection(conn);
                return myMessage;
              }
            }
          }
        }

        if (!strPO.equals("")) {
          try {
            final int total = CreateFromShipmentData.deleteC_Order_ID(conn, this, strKey, strPO);
            if (total == 0) {
              CreateFromShipmentData.updateC_Order_ID(conn, this, strPO, strKey);
            }
          } catch (final ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }
        if (!strInvoice.equals("")) {
          try {
            final int total = CreateFromShipmentData.deleteC_Invoice_ID(conn, this, strKey,
                strInvoice);
            if (total == 0) {
              CreateFromShipmentData.updateC_Invoice_ID(conn, this, strInvoice, strKey);
            }
          } catch (final ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            releaseRollbackConnection(conn);
            return myMessage;
          }
        }
      }
      releaseCommitConnection(conn);
      if (log4j.isDebugEnabled()) {
        log4j.debug("Save commit");
      }
      myMessage = new OBError();
      myMessage.setType("Success");
      myMessage.setTitle("");
      myMessage.setMessage(Utility.messageBD(this, "Success", vars.getLanguage()));
    } catch (final Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (final Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myMessage = Utility.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }
    return myMessage;
  }

  private String[] restrictParameter(String strIds) {
    String localStrIds = strIds;
    String[] ids = null;
    if (localStrIds == null || ("").equals(localStrIds)) {
      return new String[0];
    }
    localStrIds = localStrIds.substring(1, localStrIds.length() - 1);
    StringTokenizer st = new StringTokenizer(localStrIds, ",");
    int noOfRecords = 1;
    int tokenCount = st.countTokens();
    final double totalRecords = 900.0;
    int strArrayCount = tokenCount <= totalRecords ? 0 : (int) Math.ceil(tokenCount / totalRecords);
    if (strArrayCount != 0) {
      ids = new String[strArrayCount];
    } else {
      ids = new String[1];
      ids[0] = "(" + localStrIds + ")";
    }

    int count = 1;
    String tempIds = "";
    if (strArrayCount != 0) {
      while (st.hasMoreTokens()) {
        tempIds = tempIds + st.nextToken();
        if ((noOfRecords % totalRecords) != 0 && st.hasMoreTokens()) {
          tempIds = tempIds + ",";
        }
        if ((noOfRecords % totalRecords) == 0 || (strArrayCount == count && !st.hasMoreTokens())) {
          ids[count - 1] = "(" + tempIds + ")";
          tempIds = "";
          count++;
        }
        noOfRecords++;
      }

    }
    return ids;

  }

  @Override
  public String getServletInfo() {
    return "Servlet that presents the button of CreateFrom";
  } // end of getServletInfo() method
}
