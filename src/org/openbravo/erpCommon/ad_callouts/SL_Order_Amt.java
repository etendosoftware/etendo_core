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
package org.openbravo.erpCommon.ad_callouts;

import java.math.BigDecimal;
import java.math.RoundingMode;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.common.hooks.OrderLineQtyChangedHookManager;
import org.openbravo.common.hooks.OrderLineQtyChangedHookObject;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.utils.FormatUtilities;

public class SL_Order_Amt extends SimpleCallout {

  @Override
  protected void execute(CalloutInfo info) throws ServletException {

    String strChanged = info.getLastFieldChanged();
    if (log4j.isDebugEnabled()) {
      log4j.debug("CHANGED: " + strChanged);
    }

    // Parameters
    String strCOrderId = info.getStringParameter("inpcOrderId", IsIDFilter.instance);
    String strProduct = info.getStringParameter("inpmProductId", IsIDFilter.instance);
    String strUOM = info.getStringParameter("inpcUomId", IsIDFilter.instance);
    String strAttribute = info.getStringParameter("inpmAttributesetinstanceId",
        IsIDFilter.instance);
    String strTaxId = info.getStringParameter("inpcTaxId", IsIDFilter.instance);
    BigDecimal qtyOrdered = info.getBigDecimalParameter("inpqtyordered");
    BigDecimal priceActual = info.getBigDecimalParameter("inppriceactual");
    BigDecimal priceLimit = info.getBigDecimalParameter("inppricelimit");
    BigDecimal netPriceList = info.getBigDecimalParameter("inppricelist");
    BigDecimal priceStd = info.getBigDecimalParameter("inppricestd");
    BigDecimal lineNetAmt = info.getBigDecimalParameter("inplinenetamt");
    BigDecimal taxBaseAmt = info.getBigDecimalParameter("inptaxbaseamt");
    BigDecimal grossUnitPrice = info.getBigDecimalParameter("inpgrossUnitPrice");
    BigDecimal grossPriceList = info.getBigDecimalParameter("inpgrosspricelist");
    BigDecimal grossBaseUnitPrice = info.getBigDecimalParameter("inpgrosspricestd");
    BigDecimal newDiscount = info.getBigDecimalParameter("inpdiscount");
    boolean cancelPriceAd = StringUtils.equals(info.getStringParameter("inpcancelpricead"), "Y");

    Order order = OBDal.getInstance().get(Order.class, strCOrderId);
    Product product = OBDal.getInstance().get(Product.class, strProduct);

    // Standard Precision, Price Precision, IsTaxIncluded for Price List
    SLOrderAmtData[] data = SLOrderAmtData.select(this, strCOrderId);
    int stdPrecision = Integer.valueOf(data[0].stdprecision);
    int pricePrecision = Integer.valueOf(data[0].priceprecision);
    PriceList currentPriceList = OBDal.getInstance().get(PriceList.class, data[0].mPricelistId);
    boolean isTaxIncludedPriceList = currentPriceList.isPriceIncludesTax();

    // Apply Price Precision and Standard Precision
    priceActual = priceActual.setScale(pricePrecision, RoundingMode.HALF_UP);
    priceLimit = priceLimit.setScale(pricePrecision, RoundingMode.HALF_UP);
    netPriceList = netPriceList.setScale(pricePrecision, RoundingMode.HALF_UP);
    priceStd = priceStd.setScale(pricePrecision, RoundingMode.HALF_UP);
    lineNetAmt = lineNetAmt.setScale(pricePrecision, RoundingMode.HALF_UP);
    taxBaseAmt = taxBaseAmt.setScale(pricePrecision, RoundingMode.HALF_UP);
    grossUnitPrice = grossUnitPrice.setScale(pricePrecision, RoundingMode.HALF_UP);
    grossPriceList = grossPriceList.setScale(pricePrecision, RoundingMode.HALF_UP);
    grossBaseUnitPrice = grossBaseUnitPrice.setScale(pricePrecision, RoundingMode.HALF_UP);
    newDiscount = newDiscount.setScale(stdPrecision, RoundingMode.HALF_UP);

    // A hook has been created. This hook will be raised when the quantity is changed having
    // selected a product
    boolean forceSetPriceStd = false;
    boolean isGrossUnitPriceChanged = StringUtils.equals(strChanged, "inpgrossUnitPrice");
    if (StringUtils.equals(strChanged, "inpqtyordered") && StringUtils.isNotEmpty(strProduct)) {
      try {
        OrderLineQtyChangedHookObject hookObject = new OrderLineQtyChangedHookObject();
        hookObject.setProductId(strProduct);
        hookObject.setQty(qtyOrdered);
        hookObject.setOrderId(strCOrderId);
        hookObject.setPricePrecision(pricePrecision);
        hookObject.setPriceList(currentPriceList);
        if (isTaxIncludedPriceList) {
          hookObject.setListPrice(grossPriceList);
          hookObject.setPrice(grossBaseUnitPrice);
        } else {
          hookObject.setListPrice(netPriceList);
          hookObject.setPrice(priceStd);
        }

        hookObject.setChanged(strChanged);
        WeldUtils.getInstanceFromStaticBeanManager(OrderLineQtyChangedHookManager.class)
            .executeHooks(hookObject);
        if (isTaxIncludedPriceList) {
          if (grossBaseUnitPrice.compareTo(hookObject.getPrice()) != 0) {
            grossBaseUnitPrice = hookObject.getPrice();
            isGrossUnitPriceChanged = true;
          }
        } else {
          if (priceStd.compareTo(hookObject.getPrice()) != 0) {
            priceStd = hookObject.getPrice();
            forceSetPriceStd = true;
          }
        }
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    // Edit Line Net Amount
    if (StringUtils.equals(strChanged, "inplinenetamt")) {
      priceActual = lineNetAmt.divide(qtyOrdered, pricePrecision, RoundingMode.HALF_UP);
      if (priceActual.compareTo(BigDecimal.ZERO) == 0) {
        lineNetAmt = BigDecimal.ZERO;
      }
    }

    // Price Actual, Gross Unit Price
    if (StringUtils.equals(strChanged, "inpqtyordered") && !cancelPriceAd) {
      if (isTaxIncludedPriceList) {
        grossUnitPrice = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered,
            grossBaseUnitPrice);
        BigDecimal grossAmount = grossUnitPrice.multiply(qtyOrdered)
            .setScale(stdPrecision, RoundingMode.HALF_UP);
        BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount,
            stdPrecision, taxBaseAmt);
        priceActual = BigDecimal.ZERO;
        if (qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
          priceActual = netAmount.divide(qtyOrdered, pricePrecision, RoundingMode.HALF_UP);
        }
        info.addResult("inpgrossUnitPrice", grossUnitPrice);
      } else {
        priceActual = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered, priceStd);
      }
      info.addResult("inppriceactual", priceActual);
    }

    // Calculate PriceStd for offers
    boolean calcDiscount = true;
    if (StringUtils.equals(strChanged, "inppriceactual")
        || StringUtils.equals(strChanged, "inplinenetamt") || forceSetPriceStd) {
      log4j.debug("priceActual:" + priceActual.toString());
      if (!cancelPriceAd) {
        priceStd = PriceAdjustment.calculatePriceStd(order, product, qtyOrdered, priceActual);
        if (priceStd.compareTo(priceActual) != 0 && priceStd.compareTo(BigDecimal.ZERO) == 0) {
          // Check whether price adjustment sets priceStd as Zero
          calcDiscount = false;
        } else {
          calcDiscount = true;
        }
      } else {
        priceStd = priceActual;
      }
      info.addResult("inppricestd", priceStd);
    }

    // Price Actual
    if (StringUtils.equals(strChanged, "inpcancelpricead") && cancelPriceAd) {
      info.addResult("inppriceactual", priceStd);
    }

    // If taxinclusive field is changed then modify net unit price and gross price
    if (isGrossUnitPriceChanged
        || (StringUtils.equals(strChanged, "inpcTaxId") && isTaxIncludedPriceList)) {
      BigDecimal grossAmount = grossUnitPrice.multiply(qtyOrdered)
          .setScale(stdPrecision, RoundingMode.HALF_UP);
      BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount,
          stdPrecision, taxBaseAmt);
      BigDecimal netUnitPrice = BigDecimal.ZERO;
      if (qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
        netUnitPrice = netAmount.divide(qtyOrdered, pricePrecision, RoundingMode.HALF_UP);
      }

      priceActual = netUnitPrice;
      if (cancelPriceAd) {
        grossBaseUnitPrice = grossUnitPrice;
        priceStd = netUnitPrice;
      } else {
        grossBaseUnitPrice = PriceAdjustment.calculatePriceStd(order, product, qtyOrdered,
            grossUnitPrice);
        BigDecimal baseGrossAmount = grossBaseUnitPrice.multiply(qtyOrdered)
            .setScale(stdPrecision, RoundingMode.HALF_UP);
        BigDecimal baseAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, baseGrossAmount,
            stdPrecision, taxBaseAmt);
        priceStd = BigDecimal.ZERO;
        if (qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
          priceStd = baseAmount.divide(qtyOrdered, pricePrecision, RoundingMode.HALF_UP);
        }
        // Check whether price adjustment sets grossBaseUnitPrice as Zero
        calcDiscount = (grossBaseUnitPrice.compareTo(grossUnitPrice) == 0
            || grossBaseUnitPrice.compareTo(BigDecimal.ZERO) != 0);
      }

      info.addResult("inpgrosspricestd", grossBaseUnitPrice);
      info.addResult("inppriceactual", priceActual);
      info.addResult("inppricelist", netUnitPrice);
      info.addResult("inppricelimit", netUnitPrice);
      info.addResult("inppricestd", priceStd);
    }

    // Calculate Discount
    if (StringUtils.equals(strChanged, "inppricelist")
        || StringUtils.equals(strChanged, "inppriceactual")
        || StringUtils.equals(strChanged, "inplinenetamt")
        || StringUtils.equals(strChanged, "inpgrosspricelist")
        || StringUtils.equals(strChanged, "inpgrossUnitPrice")
        || StringUtils.equals(strChanged, "inpqtyordered")) {
      BigDecimal priceList = isTaxIncludedPriceList ? grossPriceList : netPriceList;
      BigDecimal unitPrice = isTaxIncludedPriceList ? grossBaseUnitPrice : priceStd;
      BigDecimal discount = priceList.compareTo(BigDecimal.ZERO) == 0 || !calcDiscount
          ? BigDecimal.ZERO
          : priceList.subtract(unitPrice)
              .multiply(new BigDecimal("100"))
              .divide(priceList, stdPrecision, RoundingMode.HALF_UP);
      log4j.debug("Discount rounded: " + discount.toString());
      info.addResult("inpdiscount", discount);

    } else if (StringUtils.equals(strChanged, "inpdiscount")) {

      // Calculate PriceStd, PriceActual, GrossPriceStd, GrossUnitPrice
      BigDecimal origDiscount = BigDecimal.ZERO;
      BigDecimal priceList = isTaxIncludedPriceList ? grossPriceList : netPriceList;
      if (priceList.compareTo(BigDecimal.ZERO) != 0) {
        BigDecimal baseUnitPrice = isTaxIncludedPriceList ? grossBaseUnitPrice : priceStd;
        origDiscount = priceList.subtract(baseUnitPrice)
            .multiply(new BigDecimal("100"))
            .divide(priceList, stdPrecision, RoundingMode.HALF_UP);
      }

      if (origDiscount.compareTo(newDiscount) != 0) {
        BigDecimal baseUnitPrice = priceList
            .subtract(priceList.multiply(newDiscount).divide(new BigDecimal("100")))
            .setScale(pricePrecision, RoundingMode.HALF_UP);
        if (isTaxIncludedPriceList) {
          grossUnitPrice = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered,
              baseUnitPrice);
          info.addResult("inpgrosspricestd", baseUnitPrice);
          info.addResult("inpgrossUnitPrice", grossUnitPrice);
          BigDecimal grossAmount = grossUnitPrice.multiply(qtyOrdered)
              .setScale(stdPrecision, RoundingMode.HALF_UP);
          BigDecimal netAmount = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount,
              stdPrecision, taxBaseAmt);
          BigDecimal netUnitPrice = BigDecimal.ZERO;
          if (qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
            netUnitPrice = netAmount.divide(qtyOrdered, pricePrecision, RoundingMode.HALF_UP);
          }
          priceStd = netUnitPrice;
        } else {
          priceStd = baseUnitPrice;
        }

        if (!cancelPriceAd) {
          priceActual = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered, priceStd);
        } else {
          priceActual = priceStd;
        }
        info.addResult("inppriceactual", priceActual);
        info.addResult("inppricestd", priceStd);
      }
    }

    // Check Stock Limit
    BigDecimal stockSecurity = BigDecimal.ZERO;
    String strEnforceAttribute = "N";
    SLOrderStockData[] dataStock = SLOrderStockData.select(this, strProduct);
    if (dataStock != null && dataStock.length > 0) {
      stockSecurity = new BigDecimal(dataStock[0].stock);
      strEnforceAttribute = dataStock[0].enforceAttribute;
    }
    if (order.isSalesTransaction() && stockSecurity.compareTo(BigDecimal.ZERO) != 0
        && qtyOrdered.compareTo(BigDecimal.ZERO) != 0) {
      BigDecimal stockNoAttribute, stockAttribute, resultStock;
      if (StringUtils.equals(strEnforceAttribute, "N")) {
        stockNoAttribute = new BigDecimal(
            SLOrderStockData.totalStockNoAttribute(this, strProduct, strUOM));
        resultStock = stockNoAttribute.subtract(qtyOrdered);
        if (stockSecurity.compareTo(resultStock) > 0) {
          info.showMessage(FormatUtilities
              .replaceJS(Utility.messageBD(this, "StockLimit", info.vars.getLanguage())));
        }
      } else if (StringUtils.isNotEmpty(strAttribute)) {
        stockAttribute = new BigDecimal(
            SLOrderStockData.totalStockAttribute(this, strProduct, strUOM, strAttribute));
        resultStock = stockAttribute.subtract(qtyOrdered);
        if (stockSecurity.compareTo(resultStock) > 0) {
          info.showMessage(FormatUtilities
              .replaceJS(Utility.messageBD(this, "StockLimit", info.vars.getLanguage())));
        }
      }
    }

    // Check Price Limit
    if (!StringUtils.equals(strChanged, "inpqtyordered")
        || StringUtils.equals(strChanged, "inplinenetamt")) {
      boolean enforced = SLOrderAmtData.listPriceType(this, currentPriceList.getId());
      if (enforced && priceLimit.compareTo(BigDecimal.ZERO) != 0
          && priceActual.compareTo(priceLimit) < 0) {
        info.showMessage(FormatUtilities
            .replaceJS(Utility.messageBD(this, "UnderLimitPrice", info.vars.getLanguage())));
      }
    }

    // Calculate Line Net Amount
    if (cancelPriceAd) {
      lineNetAmt = qtyOrdered.multiply(priceStd);
    } else {
      if (!StringUtils.equals(strChanged, "inplinenetamt")) {
        if (isTaxIncludedPriceList) {
          BigDecimal grossAmount = grossUnitPrice.multiply(qtyOrdered)
              .setScale(stdPrecision, RoundingMode.HALF_UP);
          lineNetAmt = FinancialUtils.calculateNetAmtFromGross(strTaxId, grossAmount, stdPrecision,
              taxBaseAmt);
        } else {
          lineNetAmt = qtyOrdered.multiply(priceActual);
        }
        if (lineNetAmt.scale() > stdPrecision) {
          lineNetAmt = lineNetAmt.setScale(stdPrecision, RoundingMode.HALF_UP);
        }
      }
    }

    // Price Actual
    if (StringUtils.equals(strChanged, "inplinenetamt")) {
      info.addResult("inppriceactual", priceActual);
    }

    // Line Net Amount
    if (!StringUtils.equals(strChanged, "inplinenetamt")
        || priceActual.compareTo(BigDecimal.ZERO) == 0) {
      info.addResult("inplinenetamt", lineNetAmt);
    }

    // Line Gross Amount
    if (!StringUtils.equals(strChanged, "inplineGrossAmount")) {
      BigDecimal grossLineAmt = grossUnitPrice.multiply(qtyOrdered)
          .setScale(stdPrecision, RoundingMode.HALF_UP);
      info.addResult("inplineGrossAmount", grossLineAmt);
    }

    // Tax Base Amount
    info.addResult("inptaxbaseamt", lineNetAmt);

    // Dummy
    info.addResult("dummy", null);
  }
}
