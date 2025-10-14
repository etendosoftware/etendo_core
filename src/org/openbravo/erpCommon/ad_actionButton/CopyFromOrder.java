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
package org.openbravo.erpCommon.ad_actionButton;

import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.util.StringTokenizer;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.filter.IsPositiveIntFilter;
import org.openbravo.base.secureApp.HttpSecureAppServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.utility.DateTimeData;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.materialmgmt.UOMUtil;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.utils.Replace;
import org.openbravo.xmlEngine.XmlDocument;

public class CopyFromOrder extends HttpSecureAppServlet {
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
    VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("DEFAULT")) {
      String strWindowId = vars.getStringParameter("inpwindowId");
      String strSOTrx = Utility.getContext(this, vars, "isSOTrx", strWindowId);
      String strKey = vars.getGlobalVariable("inpcOrderId", strWindowId + "|C_Order_ID");
      String strTabId = vars.getStringParameter("inpTabId");
      String strBpartner = vars.getStringParameter("inpcBpartnerId");
      String strmPricelistId = vars.getStringParameter("inpmPricelistId");
      OBContext.setAdminMode();
      try {
        boolean hasErrors = false;
        if (StringUtils.equals(strKey, "undefined")) {
          hasErrors = true;
          advisePopUpRefresh(request, response, "Error", "Error", OBMessageUtils.messageBD(this, "OrderNotDefined", vars.getLanguage()));
        }
        if (!hasErrors)
          printPageDataSheet(response, vars, strKey, strWindowId, strTabId, strSOTrx, strBpartner,
              strmPricelistId);
      } finally {
        OBContext.restorePreviousMode();
      }
    } else if (vars.commandIn("SAVE")) {
      String strRownum = null;
      try {
        strRownum = vars.getRequiredInStringParameter("inpRownumId", IsPositiveIntFilter.instance);
      } catch (ServletException e) {
        log4j.error("Error captured: ", e);
        throw new ServletException(OBMessageUtils.messageBD("@JS1@"));
      }
      String strKey = vars.getRequiredStringParameter("inpcOrderId");
      String strTabId = vars.getStringParameter("inpTabId");
      if (strRownum.startsWith("(")) {
        strRownum = strRownum.substring(1, strRownum.length() - 1);
      }
      strRownum = Replace.replace(strRownum, "'", "");
      OBError myError = new OBError();
      OBContext.setAdminMode();
      try {
        myError = copyLines(vars, strRownum, strKey);
      } finally {
        OBContext.restorePreviousMode();
      }

      String strWindowPath = Utility.getTabURL(strTabId, "R", true);
      if (strWindowPath.equals("")) {
        strWindowPath = strDefaultServlet;
      }

      vars.setMessage(strTabId, myError);
      printPageClosePopUp(response, vars, strWindowPath);
    } else {
      pageErrorPopUp(response);
    }
  }

  private OBError copyLines(VariablesSecureApp vars, String strRownums, String strKey)
      throws IOException, ServletException {

    OBError myError = null;
    int count = 0;

    if (strRownums.equals("")) {
      // return "";
      myError = OBMessageUtils.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
      return myError;
    }
    Connection conn = null;
    try {
      conn = getTransactionConnection();
      StringTokenizer st = new StringTokenizer(strRownums, ",", false);
      CopyFromOrderRecordData[] orderData = CopyFromOrderRecordData.select(this, strKey);
      Order order = OBDal.getInstance().get(Order.class, strKey);

      BigDecimal discount, priceActual, priceList, netPriceList, grossPriceList, priceStd,
          priceLimit, priceGross, amtGross, pricestdgross;
      boolean isUomManagementEnabled = UOMUtil.isUomManagementEnabled();
      while (st.hasMoreTokens()) {
        String strRownum = st.nextToken().trim();
        String strmProductId = vars.getStringParameter("inpmProductId" + strRownum);
        String strmAttributesetinstanceId = vars
            .getStringParameter("inpmAttributesetinstanceId" + strRownum);
        String strLastpriceso = vars.getNumericParameter("inpLastpriceso" + strRownum);
        String strQty = vars.getNumericParameter("inpquantity" + strRownum);
        String strcTaxId = vars.getStringParameter("inpcTaxId" + strRownum);
        String strcUOMId = vars.getStringParameter("inpcUOMId" + strRownum);
        String strCOrderlineID = SequenceIdData.getUUID();

        String strAumQty = null;
        String strcAumId = null;
        if (isUomManagementEnabled) {
          strAumQty = vars.getNumericParameter("inpaumquantity" + strRownum);
          strcAumId = vars.getStringParameter("inpcAUMId" + strRownum);
          strQty = strAumQty;
          if (!strcAumId.equals(strcUOMId)) {
            strQty = UOMUtil.getConvertedQty(strmProductId, new BigDecimal(strAumQty), strcAumId)
                .toString();
          }
        }

        priceStd = new BigDecimal(CopyFromOrderData.getOffersStdPrice(this, strLastpriceso, strKey));
        ProductPrice prices = FinancialUtils.getProductPrice(
            OBDal.getInstance().get(Product.class, strmProductId), order.getOrderDate(),
            order.isSalesTransaction(), order.getPriceList(), false);
        if (prices != null) {
          priceLimit = prices.getPriceLimit();
          priceList = prices.getListPrice();
          pricestdgross = prices.getStandardPrice();
        } else {
          priceLimit = BigDecimal.ZERO;
          priceList = BigDecimal.ZERO;
          pricestdgross = BigDecimal.ZERO;
        }

        int stdPrecision = 2;
        int pricePrecision = 2;
        OBContext.setAdminMode(true);
        try {
          stdPrecision = order.getCurrency().getStandardPrecision().intValue();
          pricePrecision = order.getCurrency().getPricePrecision().intValue();
        } finally {
          OBContext.restorePreviousMode();
        }

        if (order.getPriceList().isPriceIncludesTax()) {
          BigDecimal qty = new BigDecimal(strQty);
          priceGross = (strLastpriceso.equals("") ? ZERO : new BigDecimal(strLastpriceso));
          amtGross = priceGross.multiply(qty).setScale(stdPrecision, RoundingMode.HALF_UP);
          priceActual = BigDecimal.ZERO;
          priceLimit = BigDecimal.ZERO;
          netPriceList = BigDecimal.ZERO;
          grossPriceList = priceList;
        } else {
          priceActual = (strLastpriceso.equals("") ? ZERO : new BigDecimal(strLastpriceso));
          netPriceList = priceList;
          priceGross = BigDecimal.ZERO;
          amtGross = BigDecimal.ZERO;
          grossPriceList = BigDecimal.ZERO;
        }

        if (priceList.compareTo(BigDecimal.ZERO) == 0) {
          discount = ZERO;
        } else {
          log4j.debug("pricelist:" + priceList.toString());
          log4j.debug("priceActual:" + priceActual.toString());
          BigDecimal unitPrice;
          if (order.getPriceList().isPriceIncludesTax()) {
            unitPrice = pricestdgross;
          } else {
            unitPrice = priceActual;
          }
          // (PL-UP)/PL * 100
          discount = ((priceList.subtract(unitPrice)).multiply(new BigDecimal("100"))
              .divide(priceList, stdPrecision, RoundingMode.HALF_UP));
        }
        log4j.debug("Discount: " + discount.toString());
        if (priceStd.scale() > pricePrecision) {
          priceStd = priceStd.setScale(pricePrecision, RoundingMode.HALF_UP);
        }

        try {
          CopyFromOrderData.insertCOrderline(conn, this, strCOrderlineID, orderData[0].adClientId,
              orderData[0].adOrgId, vars.getUser(), strKey, orderData[0].cBpartnerId,
              orderData[0].cBpartnerLocationId, orderData[0].dateordered, orderData[0].dateordered,
              strmProductId,
              orderData[0].mWarehouseId.equals("") ? vars.getWarehouse()
                  : orderData[0].mWarehouseId,
              strcUOMId, strQty, orderData[0].cCurrencyId, netPriceList.toString(),
              priceActual.toString(), priceLimit.toString(), priceStd.toString(),
              discount.toString(), strcTaxId, strmAttributesetinstanceId, grossPriceList.toString(),
              priceGross.toString(), amtGross.toString(), pricestdgross.toString(), strcAumId,
              strAumQty);
        } catch (ServletException ex) {
          myError = OBMessageUtils.translateError(this, vars, vars.getLanguage(), ex.getMessage());
          releaseRollbackConnection(conn);
          return myError;
        }
        count++;
      }

      releaseCommitConnection(conn);
      myError = new OBError();
      myError.setType("Success");
      myError.setTitle(OBMessageUtils.messageBD("Success"));
      myError.setMessage(OBMessageUtils.messageBD("RecordsCopied") + " " + count);
    } catch (Exception e) {
      try {
        releaseRollbackConnection(conn);
      } catch (Exception ignored) {
      }
      e.printStackTrace();
      log4j.warn("Rollback in transaction");
      myError = OBMessageUtils.translateError(this, vars, vars.getLanguage(), "ProcessRunError");
    }
    return myError;
  }

  private void printPageDataSheet(HttpServletResponse response, VariablesSecureApp vars,
      String strKey, String strWindowId, String strTabId, String strSOTrx, String strBpartner,
      String strmPricelistId) throws IOException, ServletException {
    log4j.debug("Output: Shipment");

    XmlDocument xmlDocument = xmlEngine
        .readXmlTemplate("org/openbravo/erpCommon/ad_actionButton/CopyFromOrder")
        .createXmlDocument();
    CopyFromOrderRecordData[] dataOrder = CopyFromOrderRecordData.select(this, strKey);
    Order order = OBDal.getInstance().get(Order.class, strKey);
    CopyFromOrderData[] data = CopyFromOrderData.select(this, strBpartner, strmPricelistId,
        dataOrder[0].dateordered, order.getPriceList().isPriceIncludesTax() ? "Y" : "N", strSOTrx,
        dataOrder[0].lastDays.equals("") ? "0" : dataOrder[0].lastDays);
    FieldProvider[][] dataAUM = new FieldProvider[data.length][];
    for (int i = 0; i < data.length; i++) {
      Product product = OBDal.getInstance().get(Product.class, data[i].mProductId);
      data[i].lastpriceso = (PriceAdjustment.calculatePriceActual(order, product,
          new BigDecimal(data[i].qty), new BigDecimal(data[i].lastpriceso))).toString();

      dataAUM[i] = UOMUtil.selectAUM(data[i].mProductId, data[i].cDoctypeId);
      if (dataAUM[i].length == 0) {
        dataAUM[i] = UOMUtil.selectDefaultAUM(data[i].mProductId, data[i].cDoctypeId);
      }
    }
    xmlDocument.setParameter("language", "defaultLang=\"" + vars.getLanguage() + "\";");
    xmlDocument.setParameter("directory", "var baseDirectory = \"" + strReplaceWith + "/\";\n");
    xmlDocument.setParameter("theme", vars.getTheme());
    xmlDocument.setParameter("key", strKey);
    xmlDocument.setParameter("windowId", strWindowId);
    xmlDocument.setParameter("tabId", strTabId);
    xmlDocument.setParameter("sotrx", strSOTrx);
    xmlDocument.setParameter("yearactual", DateTimeData.sysdateYear(this));
    xmlDocument.setParameter("lastmonth",
        dataOrder[0].lastDays.equals("") ? "0" : dataOrder[0].lastDays);
    xmlDocument.setParameter("pendingdelivery",
        strSOTrx.equals("Y")
            ? CopyFromOrderRecordData.pendingDeliverySales(this, strBpartner, dataOrder[0].adOrgId,
                dataOrder[0].adClientId)
            : CopyFromOrderRecordData.materialReceiptPending(this, strBpartner,
                dataOrder[0].adOrgId, dataOrder[0].adClientId));
    xmlDocument.setParameter("pendingInvoice",
        strSOTrx.equals("Y")
            ? CopyFromOrderRecordData.pendingInvoiceSales(this, strBpartner, dataOrder[0].adOrgId,
                dataOrder[0].adClientId)
            : CopyFromOrderRecordData.purchasePendingInvoice(this, strBpartner,
                dataOrder[0].adOrgId, dataOrder[0].adClientId));
    xmlDocument.setParameter("debtpending", CopyFromOrderRecordData.debtPending(this, strBpartner,
        dataOrder[0].adOrgId, dataOrder[0].adClientId, strSOTrx));
    xmlDocument.setParameter("contact",
        CopyFromOrderRecordData.contact(this, dataOrder[0].adUserId));
    xmlDocument.setParameter("lastOrder",
        CopyFromOrderRecordData.maxDateordered(this, vars.getSqlDateFormat(), strBpartner, strSOTrx,
            dataOrder[0].adOrgId, dataOrder[0].adClientId));
    xmlDocument.setParameter("orgname", dataOrder[0].orgname);
    String strInvoicing = CopyFromOrderRecordData.invoicing(this, strSOTrx, strBpartner,
        dataOrder[0].adOrgId, dataOrder[0].adClientId);
    String strTotal = CopyFromOrderRecordData.invoicingTotal(this, strSOTrx, dataOrder[0].adOrgId,
        dataOrder[0].adClientId);
    xmlDocument.setParameter("invoicing", strInvoicing);
    xmlDocument.setParameter("bpartnername", dataOrder[0].bpartnername);

    if (UOMUtil.isUomManagementEnabled()) {
      xmlDocument.setParameter("aumQtyVisible", "table-cell");
      xmlDocument.setParameter("aumVisible", "table-cell");
      xmlDocument.setParameter("uomEnabled", "Y");
    } else {
      xmlDocument.setParameter("aumQtyVisible", "none");
      xmlDocument.setParameter("aumVisible", "none");
      xmlDocument.setParameter("uomEnabled", "N");
    }

    BigDecimal invoicing, total, totalAverage;

    invoicing = (strInvoicing.equals("") ? ZERO : (new BigDecimal(strInvoicing)));
    total = (strTotal.equals("") ? ZERO : new BigDecimal(strTotal));
    String strTotalAverage = "";
    if (total == ZERO) {
      totalAverage = (invoicing.divide(total, 12, RoundingMode.HALF_EVEN))
          .multiply(new BigDecimal("100"));
      totalAverage = totalAverage.setScale(2, RoundingMode.HALF_UP);
      strTotalAverage = totalAverage.toPlainString();
      // int intscale = totalAverage.scale();
    }

    xmlDocument.setParameter("totalAverage", strTotalAverage);

    xmlDocument.setData("structure1", data);
    xmlDocument.setData("structure2", dataOrder);
    xmlDocument.setDataArray("reportAUM_ID", "liststructure", dataAUM);
    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();

  }

  @Override
  public String getServletInfo() {
    return "Servlet Copy from order";
  } // end of getServletInfo() method
}
