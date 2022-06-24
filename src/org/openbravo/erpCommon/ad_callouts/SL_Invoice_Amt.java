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
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.plm.Product;
import org.openbravo.utils.FormatUtilities;

public class SL_Invoice_Amt extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    BigDecimal qtyInvoice = info.getBigDecimalParameter("inpqtyinvoiced");
    BigDecimal priceActual = info.getBigDecimalParameter("inppriceactual");
    BigDecimal priceLimit = info.getBigDecimalParameter("inppricelimit");
    String strInvoiceId = info.getStringParameter("inpcInvoiceId", IsIDFilter.instance);
    String strProduct = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    BigDecimal priceList = info.getBigDecimalParameter("inppricelist");
    BigDecimal priceStd = info.getBigDecimalParameter("inppricestd");
    BigDecimal lineNetAmt = info.getBigDecimalParameter("inplinenetamt");
    String strTaxId = info.getStringParameter("inpcTaxId", IsIDFilter.instance);
    BigDecimal grossUnitPrice = info.getBigDecimalParameter("inpgrossUnitPrice");
    BigDecimal baseGrossUnitPrice = info.getBigDecimalParameter("inpgrosspricestd");
    BigDecimal taxBaseAmt = info.getBigDecimalParameter("inptaxbaseamt");
    String strInvoicelineId = info.getStringParameter("inpcInvoicelineId", IsIDFilter.instance);

    // Standard Precision and Price Precision
    SLInvoiceAmtData[] data = SLInvoiceAmtData.select(this, strInvoiceId);
    String strPrecision = "0", strPricePrecision = "0";
    boolean enforcedLimit = false;
    if (data != null && data.length > 0) {
      strPrecision = StringUtils.isEmpty(data[0].stdprecision) ? "0" : data[0].stdprecision;
      strPricePrecision = StringUtils.isEmpty(data[0].priceprecision) ? "0"
          : data[0].priceprecision;
      enforcedLimit = StringUtils.equals(data[0].enforcepricelimit, "Y");
    }
    int stdPrecision = Integer.valueOf(strPrecision).intValue();
    int pricePrecision = Integer.valueOf(strPricePrecision).intValue();

    // Tax Rate and Tax Precision
    SLInvoiceTaxAmtData[] dataTax = SLInvoiceTaxAmtData.select(this, strTaxId, strInvoiceId);
    BigDecimal taxRate = BigDecimal.ZERO;
    int taxScale = 0;
    if (dataTax != null && dataTax.length > 0) {
      taxRate = StringUtils.isEmpty(dataTax[0].rate) ? BigDecimal.ONE
          : new BigDecimal(dataTax[0].rate);
      taxScale = Integer.parseInt(dataTax[0].priceprecision);
    }

    if (log4j.isDebugEnabled()) {
      log4j.debug("PriceActual: " + priceActual);
      log4j.debug("PriceLimit: " + priceLimit);
      log4j.debug("LineNetAmt: " + lineNetAmt);
      log4j.debug("TaxRate: " + taxRate);
    }

    priceActual = priceActual.setScale(pricePrecision, RoundingMode.HALF_UP);
    priceLimit = priceLimit.setScale(pricePrecision, RoundingMode.HALF_UP);
    taxBaseAmt = taxBaseAmt.setScale(pricePrecision, RoundingMode.HALF_UP);

    // Show warning if Invoiced Qty is higher than Delivered Qty
    SLInvoiceAmtData[] qtydata = SLInvoiceAmtData.selectDeliverQty(this, strInvoicelineId);
    if (qtydata != null && qtydata.length > 0
        && qtyInvoice.compareTo(new BigDecimal(qtydata[0].deliverqty)) > 0
        && StringUtils.equals(qtydata[0].invoicerule, "D")) {
      String msg = Utility.messageBD(this, "QtyInvoicedHigherDelivered", info.vars.getLanguage());
      info.showWarning(msg);
    }

    // Calculate Price Actual if Line Net Amount is edited
    if (StringUtils.equals(strChanged, "inplinenetamt")) {
      if (qtyInvoice.compareTo(BigDecimal.ZERO) == 0) {
        priceActual = BigDecimal.ZERO;
      } else {
        priceActual = lineNetAmt.divide(qtyInvoice, pricePrecision, RoundingMode.HALF_UP);
      }
    }
    if (priceActual.compareTo(BigDecimal.ZERO) == 0) {
      lineNetAmt = BigDecimal.ZERO;
    }

    Invoice invoice = OBDal.getInstance().get(Invoice.class, strInvoiceId);
    Product product = OBDal.getInstance().get(Product.class, strProduct);
    boolean priceIncludeTaxes = invoice.getPriceList().isPriceIncludesTax();

    // If unit price (actual price) changes, recalculates standard price
    // (std price) applying price adjustments (offers) if any
    if (StringUtils.equals(strChanged, "inppriceactual")
        || StringUtils.equals(strChanged, "inplinenetamt")) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("priceActual:" + Double.toString(priceActual.doubleValue()));
      }
      priceStd = PriceAdjustment.calculatePriceStd(invoice, product, qtyInvoice, priceActual);
      info.addResult("inppricestd", priceStd);
      info.addResult("inptaxbaseamt", priceActual.multiply(qtyInvoice));
    }

    // If quantity changes, recalculates unit price (actual price) applying
    // price adjustments (offers) if any
    if (StringUtils.equals(strChanged, "inpqtyinvoiced")) {
      if (log4j.isDebugEnabled()) {
        log4j.debug(
            "PriceList: " + priceList + " product:" + strProduct + " qty:" + qtyInvoice.toString());
      }
      if (priceIncludeTaxes) {
        grossUnitPrice = PriceAdjustment.calculatePriceActual(invoice, product, qtyInvoice,
            baseGrossUnitPrice);
        BigDecimal grossAmount = grossUnitPrice.multiply(qtyInvoice);
        BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount,
            stdPrecision, taxBaseAmt);
        priceActual = BigDecimal.ZERO;
        if (qtyInvoice.compareTo(BigDecimal.ZERO) != 0) {
          priceActual = netAmount.divide(qtyInvoice, pricePrecision, RoundingMode.HALF_UP);
        }
        info.addResult("inpgrossUnitPrice", grossUnitPrice);
        info.addResult("inplineGrossAmount", grossAmount);
      } else {
        priceActual = PriceAdjustment.calculatePriceActual(invoice, product, qtyInvoice, priceStd);
      }
    }

    // If Gross Unit Price or Tax field is changed when price Includes Tax = Yes
    if (StringUtils.equals(strChanged, "inpgrossUnitPrice")
        || StringUtils.equals(strChanged, "inpcTaxId") && priceIncludeTaxes) {
      baseGrossUnitPrice = PriceAdjustment.calculatePriceStd(invoice, product, qtyInvoice,
          grossUnitPrice);
      BigDecimal grossAmount = grossUnitPrice.multiply(qtyInvoice);
      BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount,
          stdPrecision, taxBaseAmt);
      BigDecimal netUnitPrice = BigDecimal.ZERO;
      if (qtyInvoice.compareTo(BigDecimal.ZERO) != 0) {
        netUnitPrice = netAmount.divide(qtyInvoice, pricePrecision, RoundingMode.HALF_UP);
      }
      priceActual = netUnitPrice;
      priceStd = netUnitPrice;

      info.addResult("inpgrosspricestd", baseGrossUnitPrice);
      info.addResult("inppriceactual", netUnitPrice);
      info.addResult("inppricelimit", netUnitPrice);
      info.addResult("inppricestd", netUnitPrice);

      // If Gross Unit Price field is changed then modify Line Gross Amount
      if (StringUtils.equals(strChanged, "inpgrossUnitPrice")) {
        info.addResult("inplineGrossAmount", grossAmount);
      }
    }

    if (!StringUtils.equals(strChanged, "inplinenetamt")) {
      if (priceIncludeTaxes) {
        // In price including taxes we get the net amount from the gross amount
        grossUnitPrice = PriceAdjustment.calculatePriceActual(invoice, product, qtyInvoice,
            baseGrossUnitPrice);
        BigDecimal grossAmount = grossUnitPrice.multiply(qtyInvoice);
        lineNetAmt = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount, stdPrecision,
            taxBaseAmt);
      } else {
        // Net amount of a line equals quantity x unit price (actual price)
        lineNetAmt = qtyInvoice.multiply(priceActual);
      }
    }

    // If edited Line Net Amount is not equals calculated Line Net Amount show informative
    // message to consider setting calculated Line Net Amount
    if (StringUtils.equals(strChanged, "inplinenetamt")) {
      int priceEditionScale = Utility.getFormat(info.vars, "priceEdition")
          .getMaximumFractionDigits();
      int euroEditionScale = Utility.getFormat(info.vars, "euroEdition").getMaximumFractionDigits();

      BigDecimal calculatedLineNetAmt = qtyInvoice
          .multiply(priceActual.setScale(priceEditionScale, RoundingMode.HALF_UP))
          .setScale(euroEditionScale, RoundingMode.HALF_UP);
      if (!lineNetAmt.setScale(priceEditionScale, RoundingMode.HALF_UP)
          .equals(calculatedLineNetAmt)) {
        StringBuffer strMessage = new StringBuffer(
            Utility.messageBD(this, "NotCorrectAmountProvided", info.vars.getLanguage()));
        strMessage.append(": ");
        strMessage.append(lineNetAmt);
        strMessage.append(". ");
        strMessage.append(Utility.messageBD(this, "CosiderUsing", info.vars.getLanguage()));
        strMessage.append(" " + calculatedLineNetAmt);
        info.showMessage(strMessage.toString());
      }
    }

    // Apply Price Precision to Line Net Amount
    if (lineNetAmt.scale() > stdPrecision) {
      lineNetAmt = lineNetAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
    }

    // Check price limit
    if (enforcedLimit) {
      if (priceLimit.compareTo(BigDecimal.ZERO) != 0 && priceActual.compareTo(priceLimit) < 0) {
        info.showMessage(FormatUtilities
            .replaceJS(Utility.messageBD(this, "UnderLimitPrice", info.vars.getLanguage())));
      }
    }

    // Calculate Tax Amount
    BigDecimal taxAmt = ((lineNetAmt.multiply(taxRate)).divide(new BigDecimal("100"), 12,
        RoundingMode.HALF_EVEN)).setScale(taxScale, RoundingMode.HALF_UP);

    // Set Line Net Amount
    if (!StringUtils.equals(strChanged, "inplinenetamt")
        || lineNetAmt.compareTo(BigDecimal.ZERO) == 0) {
      info.addResult("inplinenetamt", lineNetAmt);
    }

    // Set TaxbaseAmt, Tax Amount, Price Actual
    info.addResult("inptaxbaseamt", lineNetAmt);
    info.addResult("inptaxamt", taxAmt);
    info.addResult("inppriceactual", priceActual);
  }
}
