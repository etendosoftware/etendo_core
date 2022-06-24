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
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.procurement.Requisition;

public class SL_RequisitionLine_Amt extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strRequisitionId = info.getStringParameter("inpmRequisitionId", IsIDFilter.instance);
    String strMPricelistId = info.getStringParameter("inpmPricelistId", IsIDFilter.instance);
    BigDecimal grossPrice = info.getBigDecimalParameter("inpgrossUnitPrice");
    BigDecimal priceActual = info.getBigDecimalParameter("inppriceactual");
    BigDecimal discount = info.getBigDecimalParameter("inpdiscount");
    BigDecimal qty = info.getBigDecimalParameter("inpqty");
    BigDecimal priceList = info.getBigDecimalParameter("inppricelist");

    // Standard Precision, Price Precision, Price List, Requisition Object
    Integer stdPrecision = null;
    Integer pricePrecision = null;
    PriceList priceListObj = null;
    Requisition requisitionObj = OBDal.getInstance().get(Requisition.class, strRequisitionId);

    SLRequisitionLineAmtData[] data = null;
    if (StringUtils.isEmpty(strMPricelistId)) {
      data = SLRequisitionLineAmtData.select(this, strRequisitionId);
    } else {
      data = SLRequisitionLineAmtData.selectPriceListLine(this, strMPricelistId);
      priceListObj = OBDal.getInstance().get(PriceList.class, strMPricelistId);
    }
    if (data != null && data.length > 0) {
      stdPrecision = Integer.valueOf(data[0].stdprecision);
      pricePrecision = Integer.valueOf(data[0].priceprecision);
    }

    // Set true if selected Price List or Requisition's Price List Includes Tax in Price
    boolean isPriceIncludesTax = (priceListObj != null && priceListObj.isPriceIncludesTax())
        || (priceListObj == null && requisitionObj.getPriceList() != null
            && requisitionObj.getPriceList().isPriceIncludesTax());

    // Apply precision to Price Actual
    priceActual = internalRound(priceActual, pricePrecision);

    // Calculate discount
    if (StringUtils.equals(strChanged, "inppricelist")
        || StringUtils.equals(strChanged, "inppriceactual")) {
      if (priceList.compareTo(BigDecimal.ZERO) == 0) {
        discount = BigDecimal.ZERO;
      } else {
        if (log4j.isDebugEnabled()) {
          log4j.debug("pricelist: " + priceList.toString());
          log4j.debug("priceActual: " + priceActual.toString());
        }
        discount = ((priceList.subtract(priceActual)).divide(priceList, 12, RoundingMode.HALF_EVEN))
            .multiply(new BigDecimal("100"));
      }
      if (log4j.isDebugEnabled()) {
        log4j.debug("Discount: " + discount.toString());
      }
      discount = internalRound(discount, stdPrecision);
      if (log4j.isDebugEnabled()) {
        log4j.debug("Discount rounded: " + discount.toString());
      }
      info.addResult("inpdiscount", discount);

      if (isPriceIncludesTax) {
        info.addResult("inppriceactual", priceActual);
      }

    } else if (StringUtils.equals(strChanged, "inpdiscount")) {
      // Calculate Price Std and Price Actual
      BigDecimal discount1 = BigDecimal.ZERO;
      if (priceList.compareTo(BigDecimal.ZERO) != 0) {
        discount1 = internalRound(
            ((priceList.subtract(priceActual)).divide(priceList, 12, RoundingMode.HALF_EVEN))
                .multiply(new BigDecimal("100")),
            stdPrecision);
      }
      BigDecimal discount2 = internalRound(discount, stdPrecision);

      // checks if rounded discount has changed
      if (discount1.compareTo(discount2) != 0) {
        priceActual = priceList.subtract(
            priceList.multiply(discount).divide(new BigDecimal("100"), 12, RoundingMode.HALF_EVEN));
        priceActual = internalRound(priceActual, pricePrecision);
        info.addResult("inppriceactual", priceActual);
      }
    }

    // Set Gross Unit Price
    if (isPriceIncludesTax) {
      priceActual = internalRound(grossPrice, pricePrecision);
      info.addResult("inpgrossUnitPrice", priceActual);
    }

    // Calculate & Set LineNetAmount or GrossAmount
    BigDecimal lineNetAmt = qty.multiply(priceActual);
    lineNetAmt = internalRound(lineNetAmt, stdPrecision);

    if (isPriceIncludesTax) {
      info.addResult("inpgrossAmt", lineNetAmt);
    } else {
      info.addResult("inplinenetamt", lineNetAmt);
    }
  }

  private BigDecimal internalRound(BigDecimal value, Integer precision) {
    return (precision == null || value.scale() <= precision) ? value
        : value.setScale(precision, RoundingMode.HALF_UP);
  }
}
