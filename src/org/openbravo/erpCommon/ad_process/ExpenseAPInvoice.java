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
package org.openbravo.erpCommon.ad_process;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_actionButton.ActionButtonDefaultData;
import org.openbravo.erpCommon.businessUtility.Tax;
import org.openbravo.erpCommon.businessUtility.WindowTabs;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.ComboTableData;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.LeftTabsBar;
import org.openbravo.erpCommon.utility.NavigationBar;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.ToolBar;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.xmlEngine.XmlDocument;

public class ExpenseAPInvoice extends HttpSecureAppServlet {
  private static final long serialVersionUID = 1L;
  private static final BigDecimal ZERO = BigDecimal.ZERO;
  private static final Logger expenseAPInvoiceLog4j = LogManager.getLogger();

  @Override
  public void init(ServletConfig config) {
    super.init(config);
    boolHist = false;
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strcBpartnerId = vars.getStringParameter("inpcBpartnerId");
      String strDatereportFrom = vars.getGlobalVariable("inpReportDateFrom",
          "ExpenseAPInvoice.reportdatefrom", "");
      String strDatereportTo = vars.getGlobalVariable("inpReportDateTo",
          "ExpenseAPInvoice.reportdateto", "");
      String strDateInvoiced = vars.getGlobalVariable("inpDateinvoiced",
          "ExpenseAPInvoice.dateinvoiced", "");
      printPage(response, vars, strcBpartnerId, strDatereportFrom, strDatereportTo,
          strDateInvoiced);
    } else if (vars.commandIn("SAVE")) {
      String strcBpartnerId = vars.getStringParameter("inpcBpartnerId", "");
      String strDatereportFrom = vars.getRequestGlobalVariable("inpReportDateFrom",
          "ExpenseAPInvoice.reportdatefrom");
      String strDatereportTo = vars.getRequestGlobalVariable("inpReportDateTo",
          "ExpenseAPInvoice.reportdateto");
      String strDateInvoiced = vars.getRequestGlobalVariable("inpDateinvoiced",
          "ExpenseAPInvocie.dateinvoiced");
      OBError myMessage = processExpense(vars, strcBpartnerId, strDatereportFrom, strDatereportTo,
          strDateInvoiced);
      vars.setMessage("ExpenseAPInvoice", myMessage);
      printPage(response, vars, strcBpartnerId, strDatereportFrom, strDatereportTo,
          strDateInvoiced);
    } else {
      pageErrorPopUp(response);
    }
  }

  private OBError processExpense(VariablesSecureApp vars, String strcBpartnerId,
      String strDatereportFrom, String strDatereportTo, String strDateInvoiced) {
    if (expenseAPInvoiceLog4j.isDebugEnabled()) {
      expenseAPInvoiceLog4j.debug("Save: Expense AP Invoice");
    }

    String strcInvoiceId = "";
    String strCCurrencyId = "";
    String strPricelistId = "";
    String strBPCCurrencyId = "";
    String strDateexpense = "";
    String strcTaxID = "";
    String strcBpartnerLocationId = "";
    String strcInvoiceLineId = "";
    String strPricestd = "";
    String strPricelimit = "";
    String strPricelist = "";
    String strSalesrepId = "";
    String strPaymentRule = "";
    String strPaymentMethodId = "";
    String strPaymentterm = "";
    BigDecimal qty = BigDecimal.ZERO;
    BigDecimal amount = BigDecimal.ZERO;
    int total = 0;

    StringBuffer textoMensaje = new StringBuffer();
    OBError myMessage = null;
    myMessage = new OBError();
    myMessage.setTitle("");
    String strProd = "";
    String strEmpl = "";

    try {

      ExpenseAPInvoiceData[] data = ExpenseAPInvoiceData.select(this,
          Utility.getContext(this, vars, "#User_Client", "ExpenseAPInvoice"),
          Utility.getContext(this, vars, "#User_Org", "ExpenseAPInvoice"), strDatereportFrom,
          DateTimeData.nDaysAfter(this, strDatereportTo, "1"), strcBpartnerId);
      for (int i = 0; i < data.length; i++) {
        String docTargetType = ExpenseAPInvoiceData.cDoctypeTarget(this, data[i].adClientId,
            data[i].adOrgId);

        // Checks some employee data
        strEmpl = data[i].bpname;
        strProd = data[i].prodname;

        strPricelistId = ExpenseAPInvoiceData.pricelistId(this, data[i].cBpartnerId);
        if (strPricelistId.equals("")) {
          throw new Exception("PricelistNotdefined");
        } else {
          // Selects the currency of the employee price list, that
          // can be different from the currency of the expense
          strBPCCurrencyId = ExpenseAPInvoiceData.selectCurrency(this, strPricelistId);
        }

        strcBpartnerLocationId = ExpenseAPInvoiceData.bPartnerLocation(this, data[i].cBpartnerId);
        if (strcBpartnerLocationId.equals("")) {
          throw new Exception("ShiptoNotdefined");
        }

        strPaymentRule = ExpenseAPInvoiceData.paymentrule(this, data[i].cBpartnerId);
        if (strPaymentRule == null || "".equals(strPaymentRule)) {
          strPaymentRule = "P";
        }
        if (strPaymentRule.equals("")) {
          throw new Exception("FormofPaymentNotdefined");
        }

        strPaymentMethodId = ExpenseAPInvoiceData.paymentmethodId(this, data[i].cBpartnerId);
        if (strPaymentMethodId.equals("")) {
          throw new Exception("PayementMethodNotdefined");
        }
        strPaymentRule = ExpenseAPInvoiceData.paymentrule(this, data[i].cBpartnerId);
        if (strPaymentRule.equals("")) {
          strPaymentRule = "P";
        }
        strPaymentterm = ExpenseAPInvoiceData.paymentterm(this, data[i].cBpartnerId);
        if (strPaymentterm.equals("")) {
          throw new Exception("PaymenttermNotdefined");
        }

        // Checks if there are invoices not processed that full filled
        // the requirements
        String strcInvoiceIdOld = "";
        // In order to make different purchase invoices for expense
        // lines assigned to different projects
        if (data[i].cProjectId.equals("")) {
          strcInvoiceIdOld = ExpenseAPInvoiceData.selectInvoiceHeaderNoProject(
              OBDal.getInstance().getConnection(), this, data[i].adClientId, data[i].adOrgId,
              strDateInvoiced, data[i].cBpartnerId, strBPCCurrencyId, data[i].cActivityId,
              data[i].cCampaignId, strcBpartnerLocationId, strPaymentMethodId, strPaymentterm,
              data[i].cCostcenterId, data[i].aAssetId, data[i].user1Id, data[i].user2Id);

        } else {
          strcInvoiceIdOld = ExpenseAPInvoiceData.selectInvoiceHeader(
              OBDal.getInstance().getConnection(), this, data[i].adClientId, data[i].adOrgId,
              strDateInvoiced, data[i].cBpartnerId, strBPCCurrencyId, data[i].cProjectId,
              data[i].cActivityId, data[i].cCampaignId, strcBpartnerLocationId, strPaymentMethodId,
              strPaymentterm, data[i].cCostcenterId, data[i].aAssetId, data[i].user1Id,
              data[i].user2Id);
        }

        if (strcInvoiceIdOld.equals("")) {

          // Creates a new purchase invoice header
          strcInvoiceId = SequenceIdData.getUUID();
          String strDocumentno = Utility.getDocumentNo(this, vars, "", "C_Invoice", docTargetType,
              docTargetType, false, true);
          String strDocType = ExpenseAPInvoiceData.cDoctypeTarget(this, data[i].adClientId,
              data[i].adOrgId);

          // Catch database error message
          try {
            ExpenseAPInvoiceData.insert(OBDal.getInstance().getConnection(), this, strcInvoiceId,
                "N", "", "N", "N", "N", "N", "N", data[i].adClientId, data[i].adOrgId, "", "",
                strDocumentno, "", "", "Y", docTargetType, strDateInvoiced, strDateInvoiced,
                data[i].cBpartnerId, strcBpartnerLocationId, "", strPricelistId, strBPCCurrencyId,
                strSalesrepId, "N", "", "", strPaymentMethodId, strPaymentterm, "N", "N",
                data[i].cProjectId, data[i].cActivityId, data[i].cCampaignId, vars.getOrg(),
                data[i].user1Id, data[i].user2Id, "0", "0", "DR", strDocType, "N", "CO", "N",
                vars.getUser(), vars.getUser(), data[i].cCostcenterId, data[i].aAssetId);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            OBDal.getInstance().rollbackAndClose();
            return myMessage;
          }

          textoMensaje
              .append(Utility.messageBD(this, "PurchaseInvoiceDocumentno", vars.getLanguage()))
              .append(" ")
              .append(strDocumentno)
              .append(" ")
              .append(Utility.messageBD(this, "beenCreated", vars.getLanguage()))
              .append("<br>");
          total++;
        } else {
          strcInvoiceId = strcInvoiceIdOld;
        }

        // Determines the date of the expense (strDateExpense)
        strDateexpense = data[i].dateexpense.equals("") ? data[i].datereport : data[i].dateexpense;
        strDateexpense = strDateexpense.equals("") ? DateTimeData.today(this) : strDateexpense;

        String bpartnerLocationShip = ExpenseAPInvoiceData.shipto(this, data[i].cBpartnerId);
        if (bpartnerLocationShip.equals("")) {
          throw new Exception("ShiptoNotdefined");
        }

        String strmProductUomId = ExpenseAPInvoiceData.mProductUomId(this, data[i].mProductId);

        // Gets the tax for the purchase invoice line
        strcTaxID = Tax.get(this, data[i].mProductId, strDateInvoiced, data[i].adOrgId,
            vars.getWarehouse(), strcBpartnerLocationId, bpartnerLocationShip, data[i].cProjectId,
            false);
        if (strcTaxID.equals("")) {
          throw new Exception("TaxNotFound");
        }

        // Currency of the expense line
        strCCurrencyId = data[i].cCurrencyId;

        String strPricePrecision = "0";
        String strStdPrecision = "0";

        BigDecimal priceActual, priceList, priceLimit;

        ExpenseAPInvoiceData[] dataPrice = ExpenseAPInvoiceData.selectPrice(this,
            data[i].mProductId, strPricelistId, strDateInvoiced);

        strPricestd = data[i].invoiceprice.equals("") ? dataPrice[0].pricestd
            : data[i].invoiceprice;
        strPricelist = data[i].invoiceprice.equals("") ? dataPrice[0].pricelist
            : data[i].invoiceprice;
        strPricelimit = data[i].invoiceprice.equals("") ? dataPrice[0].pricelimit
            : data[i].invoiceprice;

        // If there was an invoice price, makes currency conversions, if
        // necessary
        if (data[i].invoiceprice != null && !data[i].invoiceprice.equals("")) {
          // If the currency of the expense line is not the same as
          // the currency of the employee pricelist, make the
          // corresponding conversions and set the correct precisions
          if (!strCCurrencyId.equals(strBPCCurrencyId)) {
            // Converts priceactual, pricelist and pricelimit to the
            // currency of the employee pricelist
            strPricestd = ExpenseAPInvoiceData.selectConvertedAmt(this, strPricestd, strCCurrencyId,
                strBPCCurrencyId, strDateexpense, vars.getClient(), vars.getOrg());
            strPricelist = ExpenseAPInvoiceData.selectConvertedAmt(this, strPricelist,
                strCCurrencyId, strBPCCurrencyId, strDateexpense, vars.getClient(), vars.getOrg());
            strPricelimit = ExpenseAPInvoiceData.selectConvertedAmt(this, strPricelimit,
                strCCurrencyId, strBPCCurrencyId, strDateexpense, vars.getClient(), vars.getOrg());
          }
        }

        // Recalculates precisions for the employee pricelist currency
        ExpenseAPInvoiceData[] data4 = ExpenseAPInvoiceData.selectPrecisions(this,
            strBPCCurrencyId);
        if (data4 != null && data4.length > 0) {
          strPricePrecision = data4[0].priceprecision.equals("") ? "0" : data4[0].priceprecision;
          strStdPrecision = data4[0].stdprecision.equals("") ? "0" : data4[0].stdprecision;
        }
        int PricePrecision = Integer.valueOf(strPricePrecision).intValue();
        priceActual = (strPricestd.equals("") ? ZERO : (new BigDecimal(strPricestd)));
        priceActual = priceActual.setScale(PricePrecision, RoundingMode.HALF_UP);
        priceList = (strPricelist.equals("") ? ZERO : (new BigDecimal(strPricelist)));
        priceList = priceList.setScale(PricePrecision, RoundingMode.HALF_UP);
        priceLimit = (strPricelimit.equals("") ? ZERO : (new BigDecimal(strPricelimit)));
        priceLimit = priceLimit.setScale(PricePrecision, RoundingMode.HALF_UP);

        strPricestd = priceActual.toString();
        strPricelist = priceList.toString();
        strPricelimit = priceLimit.toString();

        if (strPricestd.equals("")) {
          strPricestd = "0";
        }
        if (strPricelist.equals("")) {
          strPricelist = "0";
        }
        if (strPricelimit.equals("")) {
          strPricelimit = "0";
        }

        // Checks if there are lines with the same conditions in the
        // current invoice
        ExpenseAPInvoiceData[] dataInvoiceline = ExpenseAPInvoiceData.selectInvoiceLine(
            OBDal.getInstance().getConnection(), this, strcInvoiceId, data[i].adClientId,
            data[i].adOrgId, data[i].mProductId, data[i].cUomId, strPricestd, strPricelist,
            strPricelimit, data[i].description, strcTaxID);

        if (expenseAPInvoiceLog4j.isDebugEnabled()) {
          expenseAPInvoiceLog4j.debug("dataInvoiceline: " + dataInvoiceline.length);
        }
        if (dataInvoiceline == null || dataInvoiceline.length == 0) {
          // If it is a new line, calculates c_invoiceline_id and qty
          strcInvoiceLineId = SequenceIdData.getUUID();
          qty = new BigDecimal(data[i].qty);
          String strLine = ExpenseAPInvoiceData.selectLine(OBDal.getInstance().getConnection(),
              this, strcInvoiceId);

          if (expenseAPInvoiceLog4j.isDebugEnabled()) {
            expenseAPInvoiceLog4j.debug("*****************+client: "
                + (data[i].invoiceprice.equals("") ? dataPrice[0].pricestd : data[i].invoiceprice));
          }
          // Calculate Acc and Def Plan from Product
          String isDeferred = "N";
          HashMap<String, String> accDefPlanData = AccDefUtility
              .getDeferredPlanForInvoiceProduct(strcInvoiceId, data[i].mProductId);
          String planType = accDefPlanData.get("planType");
          String periodNumber = accDefPlanData.get("periodNumber");
          String startingPeriodId = accDefPlanData.get("startingPeriodId");
          // As it is a Expense invoice:
          if (!"".equals(planType) && !"".equals(periodNumber) && !"".equals(startingPeriodId)) {
            isDeferred = "Y";
          } else {
            planType = "";
            periodNumber = "";
            startingPeriodId = "";
          }
          // Catch database error message
          try {
            int stdPrecision = Integer.parseInt(strStdPrecision);
            ExpenseAPInvoiceData.insertLine(OBDal.getInstance().getConnection(), this,
                data[i].adClientId, data[i].adOrgId, strcInvoiceId, "", strLine, "",
                data[i].mProductId, "", data[i].description, "", strmProductUomId,
                String.valueOf(qty), data[i].cUomId, strPricestd, strPricelist, strcTaxID,
                ((new BigDecimal(strPricestd).setScale(stdPrecision, RoundingMode.HALF_UP))
                    .multiply(qty)).toPlainString(),
                "", strPricestd, strPricelimit, "", "", "Y", "0", "", "", strcInvoiceLineId,
                "N", vars.getUser(), vars.getUser(), isDeferred, planType, periodNumber,
                startingPeriodId, data[i].cProjectId, data[i].cCostcenterId, data[i].aAssetId,
                data[i].user1Id, data[i].user2Id);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            OBDal.getInstance().rollbackAndClose();
            return myMessage;
          }
        } else {
          // If there are more lines that full filled the
          // requirements, adds the new amount to the old
          strcInvoiceLineId = dataInvoiceline[0].cInvoicelineId;
          qty = new BigDecimal(dataInvoiceline[0].qtyinvoiced).add(new BigDecimal(data[i].qty));
          // Catch database error message
          try {
            ExpenseAPInvoiceData.updateInvoiceline(OBDal.getInstance().getConnection(), this,
                String.valueOf(qty), (new BigDecimal(strPricestd).multiply(qty)).toPlainString(),
                strcInvoiceLineId);
          } catch (ServletException ex) {
            myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
            OBDal.getInstance().rollbackAndClose();
            return myMessage;
          }
        }

        if (!data[i].cProjectId.equals("")) {
          // If there are acctdimensions that full filled the
          // requirements
          ExpenseAPInvoiceData[] dataAcctdimension = ExpenseAPInvoiceData.selectAcctdimension(
              OBDal.getInstance().getConnection(), this, data[i].adClientId, data[i].adOrgId,
              strcInvoiceLineId, data[i].cProjectId, data[i].cCampaignId, data[i].cCostcenterId,
              data[i].aAssetId, data[i].user1Id, data[i].user2Id);
          int StdPrecision = Integer.valueOf(strStdPrecision).intValue();
          if (dataAcctdimension == null || dataAcctdimension.length == 0) {
            String strcInvoicelineAcctdimension = SequenceIdData.getUUID();
            // Catch database error message
            try {
              BigDecimal roundPriceStd = qty.multiply(new BigDecimal(strPricestd));
              roundPriceStd = roundPriceStd.setScale(StdPrecision, RoundingMode.HALF_UP);
              ExpenseAPInvoiceData.insertInvoicelineAcctdimension(
                  OBDal.getInstance().getConnection(), this, strcInvoicelineAcctdimension,
                  data[i].adClientId, data[i].adOrgId, "Y", vars.getUser(), vars.getUser(),
                  strcInvoiceLineId, roundPriceStd.toPlainString(), data[i].cProjectId,
                  data[i].cCampaignId, data[i].user1Id, data[i].user2Id, data[i].cCostcenterId,
                  data[i].aAssetId);
            } catch (ServletException ex) {
              myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
              OBDal.getInstance().rollbackAndClose();
              return myMessage;
            }
          } else {
            // If there are more lines that full filled the
            // requirements, adds the new amount to the old
            amount = new BigDecimal(dataAcctdimension[0].amt)
                .add(new BigDecimal(data[i].qty).multiply(new BigDecimal(strPricestd)));
            amount = amount.setScale(StdPrecision, RoundingMode.HALF_UP);
            // Catch database error message
            try {
              ExpenseAPInvoiceData.updateAcctdimension(OBDal.getInstance().getConnection(), this,
                  String.valueOf(amount), dataAcctdimension[0].cInvoicelineAcctdimensionId);
            } catch (ServletException ex) {
              myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
              OBDal.getInstance().rollbackAndClose();
              return myMessage;
            }
          }
        }
        // Catch database error message
        try {
          // Updates expense line with the invoice line ID
          ExpenseAPInvoiceData.updateExpense(OBDal.getInstance().getConnection(), this,
              strcInvoiceLineId, data[i].sTimeexpenselineId);
        } catch (ServletException ex) {
          myMessage = Utility.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          OBDal.getInstance().rollbackAndClose();
          return myMessage;
        }
      }
      OBDal.getInstance().commitAndClose();

      myMessage.setType("Success");
      myMessage.setTitle(Utility.messageBD(this, "Success", vars.getLanguage()));
      myMessage.setMessage(
          textoMensaje.toString() + Utility.messageBD(this, "Created", vars.getLanguage()) + ": "
              + Integer.toString(total));
      return myMessage;
    } catch (ArrayIndexOutOfBoundsException f) {
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Exception ignored) {
      }
      f.printStackTrace();
      expenseAPInvoiceLog4j.warn("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      myMessage.setMessage(Utility.messageBD(this, "PriceListVersionNotFound", vars.getLanguage())
          + ' ' + Utility.messageBD(this, "ForProduct", vars.getLanguage()) + ' ' + strProd + ' '
          + Utility.messageBD(this, "And", vars.getLanguage()) + ' '
          + Utility.messageBD(this, "Employee", vars.getLanguage()) + ' ' + strEmpl + ". "
          + Utility.messageBD(this, "PleaseAddTheProduct", vars.getLanguage()) + ' '
          + Utility.messageBD(this, strProd, vars.getLanguage()) + ' '
          + Utility.messageBD(this, "ToPurchasePriceList", vars.getLanguage()) + ".");
      return myMessage;
    } catch (Exception e) {
      try {
        OBDal.getInstance().rollbackAndClose();
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      expenseAPInvoiceLog4j.warn("Rollback in transaction");
      myMessage.setType("Error");
      myMessage.setTitle(Utility.messageBD(this, "Error", vars.getLanguage()));
      if (e.getMessage().equals("PricelistNotdefined")) {
        myMessage.setMessage(Utility.messageBD(this, "TheEmployee", vars.getLanguage()) + ' '
            + strEmpl + ' ' + Utility.messageBD(this, "PricelistNotdefined", vars.getLanguage()));
      } else if (e.getMessage().equals("FormofPaymentNotdefined")) {
        myMessage
            .setMessage(Utility.messageBD(this, "TheEmployee", vars.getLanguage()) + ' ' + strEmpl
                + ' ' + Utility.messageBD(this, "FormofPaymentNotdefined", vars.getLanguage()));
      } else if (e.getMessage().equals("PayementMethodNotdefined")) {
        myMessage
            .setMessage(Utility.messageBD(this, "TheEmployee", vars.getLanguage()) + ' ' + strEmpl
                + ' ' + Utility.messageBD(this, "PayementMethodNotdefined", vars.getLanguage()));
      } else if (e.getMessage().equals("PaymenttermNotdefined")) {
        myMessage.setMessage(Utility.messageBD(this, "TheEmployee", vars.getLanguage()) + ' '
            + strEmpl + ' ' + Utility.messageBD(this, "PaymenttermNotdefined", vars.getLanguage()));
      } else if (e.getMessage().equals("ShiptoNotdefined")) {
        myMessage.setMessage(Utility.messageBD(this, "TheEmployee", vars.getLanguage()) + ' '
            + strEmpl + ' ' + Utility.messageBD(this, "ShiptoNotdefined", vars.getLanguage()));
      } else if (e.getMessage().equals("TaxNotFound")) {
        myMessage.setMessage(Utility.messageBD(this, "TaxNotFound", vars.getLanguage()));
      } else {
        myMessage.setMessage(Utility.messageBD(this, "ProcessRunError", vars.getLanguage()));
      }
      return myMessage;
    }

  }

  private void printPage(HttpServletResponse response, VariablesSecureApp vars,
      String strcBpartnerId, String strDatereportFrom, String strDatereportTo,
      String strDateInvoiced) throws IOException, ServletException {
    if (expenseAPInvoiceLog4j.isDebugEnabled()) {
      expenseAPInvoiceLog4j.debug("Output: process ExpenseAPInvoice");
    }

    ActionButtonDefaultData[] data = null;
    String strHelp = "", strDescription = "", strProcessId = "187";
    String[] discard = { "" };
    if (vars.getLanguage().equals("en_US")) {
      data = ActionButtonDefaultData.select(this, strProcessId);
    } else {
      data = ActionButtonDefaultData.selectLanguage(this, vars.getLanguage(), strProcessId);
    }
    if (data != null && data.length != 0) {
      strDescription = data[0].description;
      strHelp = data[0].help;
    }
    if (strHelp.equals("")) {
      discard[0] = new String("helpDiscard");
    }

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_process/ExpenseAPInvoice")
        .createXmlDocument();

    ToolBar toolbar = new ToolBar(this, vars.getLanguage(), "ExpenseAPInvoice", false, "", "", "",
        false, "ad_process", strReplaceWith, false, true);
    toolbar.prepareSimpleToolBarTemplate();
    xmlDocument.setParameter("toolbar", toolbar.toString());

    xmlDocument.setParameter("calendar", vars.getLanguage().substring(0, 2));
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("description", strDescription);
    xmlDocument.setParameter("help", strHelp);
    xmlDocument.setParameter("dateFrom", strDatereportFrom);
    xmlDocument.setParameter("dateTo", strDatereportTo);
    xmlDocument.setParameter("dateInvoiced", strDateInvoiced);

    try {
      ComboTableData comboTableData = new ComboTableData(vars, this, "TABLE", "C_BPartner_ID",
          "C_BPartner Employee w Address", "",
          Utility.getContext(this, vars, "#AccessibleOrgTree", "ExpenseAPInvoice"),
          Utility.getContext(this, vars, "#User_Client", "ExpenseAPInvoice"), 0);
      Utility.fillSQLParameters(this, vars, null, comboTableData, "ExpenseAPInvoice", "");
      xmlDocument.setData("reportC_BPARTNERID", "liststructure", comboTableData.select(false));
      comboTableData = null;
    } catch (Exception ex) {
      throw new ServletException(ex);
    }

    xmlDocument.setParameter("dateFromdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateFromsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTodisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateTosaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateInvdisplayFormat", vars.getSessionValue("#AD_SqlDateFormat"));
    xmlDocument.setParameter("dateInvsaveFormat", vars.getSessionValue("#AD_SqlDateFormat"));

    // New interface parameters
    try {
      WindowTabs tabs = new WindowTabs(this, vars,
          "org.openbravo.erpCommon.ad_process.ExpenseAPInvoice");

      xmlDocument.setParameter("parentTabContainer", tabs.parentTabs());
      xmlDocument.setParameter("mainTabContainer", tabs.mainTabs());
      xmlDocument.setParameter("childTabContainer", tabs.childTabs());
      xmlDocument.setParameter("theme", vars.getTheme());
      NavigationBar nav = new NavigationBar(this, vars.getLanguage(), "ExpenseAPInvoice.html",
          classInfo.id, classInfo.type, strReplaceWith, tabs.breadcrumb());
      xmlDocument.setParameter("navigationBar", nav.toString());
      LeftTabsBar lBar = new LeftTabsBar(this, vars.getLanguage(), "ExpenseAPInvoice.html",
          strReplaceWith);
      xmlDocument.setParameter("leftTabs", lBar.manualTemplate());
    } catch (Exception ex) {
      throw new ServletException(ex);
    }
    {
      OBError myMessage = vars.getMessage("ExpenseAPInvoice");
      vars.removeMessage("ExpenseAPInvoice");
      if (myMessage != null) {
        xmlDocument.setParameter("messageType", myMessage.getType());
        xmlDocument.setParameter("messageTitle", myMessage.getTitle());
        xmlDocument.setParameter("messageMessage", myMessage.getMessage());
      }
    }
    // //----

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  @Override
  public String getServletInfo() {
    return "Servlet ExpenseAPInvoice";
  } // end of getServletInfo() method
}
