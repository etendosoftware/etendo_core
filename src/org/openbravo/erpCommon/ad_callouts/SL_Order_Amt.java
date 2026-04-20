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

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.filter.IsIDFilter;
import org.openbravo.base.weld.WeldUtils;

import com.etendoerp.common.hooks.HookManager;
import com.etendoerp.common.hooks.OrderLineQtyChangedHookObject;

import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.PriceAdjustment;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.utils.FormatUtilities;

public class SL_Order_Amt extends SimpleCallout {

  public static final String INPQTYORDERED = "inpqtyordered";
  public static final String INPPRICELIST = "inppricelist";
  public static final String INPGROSSPRICESTD = "inpgrosspricestd";
  public static final String INPDISCOUNT = "inpdiscount";

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
    BigDecimal qtyOrdered = info.getBigDecimalParameter(INPQTYORDERED);
    BigDecimal priceActual = info.getBigDecimalParameter("inppriceactual");
    BigDecimal priceLimit = info.getBigDecimalParameter("inppricelimit");
    BigDecimal netPriceList = info.getBigDecimalParameter(INPPRICELIST);
    BigDecimal priceStd = info.getBigDecimalParameter("inppricestd");
    BigDecimal lineNetAmt = info.getBigDecimalParameter("inplinenetamt");
    BigDecimal taxBaseAmt = info.getBigDecimalParameter("inptaxbaseamt");
    BigDecimal grossUnitPrice = info.getBigDecimalParameter("inpgrossUnitPrice");
    BigDecimal grossPriceList = info.getBigDecimalParameter("inpgrosspricelist");
    BigDecimal grossBaseUnitPrice = info.getBigDecimalParameter(INPGROSSPRICESTD);
    BigDecimal newDiscount = info.getBigDecimalParameter(INPDISCOUNT);
    /*
      Always cancel price adjustment recalculation in this callout
      to avoid errors when applying custom discounts using the
      discount field. Check EPL-512 for more information.
     */
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
    if (StringUtils.equals(strChanged, INPQTYORDERED) && StringUtils.isNotEmpty(strProduct)) {
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
        WeldUtils.getInstanceFromStaticBeanManager(HookManager.class)
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
    if (StringUtils.equals(strChanged, INPQTYORDERED) && !cancelPriceAd) {
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
      log4j.debug("priceActual:" + priceActual);
      if (!cancelPriceAd) {
        // Keep the current base price when the current final price is already fully explained by
        // the existing price adjustments. Reversing from the rounded final price can drift it.
        if (!isPriceAdjustmentAlreadyApplied(order, product, qtyOrdered, priceStd, priceActual)) {
          priceStd = PriceAdjustment.calculatePriceStd(order, product, qtyOrdered, priceActual);
        }
        // Check whether price adjustment sets priceStd as Zero
        calcDiscount = priceStd.compareTo(priceActual) == 0 || BigDecimal.ZERO.compareTo(priceStd) != 0;
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
        if (!isPriceAdjustmentAlreadyApplied(order, product, qtyOrdered, grossBaseUnitPrice,
            grossUnitPrice)) {
          grossBaseUnitPrice = PriceAdjustment.calculatePriceStd(order, product, qtyOrdered,
              grossUnitPrice);
        }
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

      info.addResult(INPGROSSPRICESTD, grossBaseUnitPrice);
      info.addResult("inppriceactual", priceActual);
      info.addResult(INPPRICELIST, netUnitPrice);
      info.addResult("inppricelimit", netUnitPrice);
      info.addResult("inppricestd", priceStd);
    }

    // Calculate Discount
    if (StringUtils.equals(strChanged, INPPRICELIST)
        || StringUtils.equals(strChanged, "inppriceactual")
        || StringUtils.equals(strChanged, "inplinenetamt")
        || StringUtils.equals(strChanged, "inpgrosspricelist")
        || StringUtils.equals(strChanged, "inpgrossUnitPrice")
        || StringUtils.equals(strChanged, INPQTYORDERED)) {
      BigDecimal priceList = isTaxIncludedPriceList ? grossPriceList : netPriceList;
      BigDecimal unitPrice = isTaxIncludedPriceList ? grossBaseUnitPrice : priceStd;
      BigDecimal finalPrice = isTaxIncludedPriceList ? grossUnitPrice : priceActual;
      BigDecimal discount = BigDecimal.ZERO;
      if (priceList.compareTo(BigDecimal.ZERO) != 0 && calcDiscount) {
        BigDecimal basePrice = calculateNewUnitPrice(newDiscount, priceList, pricePrecision);
        if (isDiscountAlreadyApplied(order, product, qtyOrdered, basePrice, finalPrice, cancelPriceAd)) {
          discount = newDiscount;
        } else {
          discount = calculateDiscountFromPrices(priceList, unitPrice, stdPrecision);
        }
      }
      log4j.debug("Discount rounded: " + discount.toString());
      info.addResult(INPDISCOUNT, discount);

    } else if (StringUtils.equals(strChanged, INPDISCOUNT)) {

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
        BigDecimal baseUnitPrice = calculateNewUnitPrice(newDiscount, priceList, pricePrecision);
        if (isTaxIncludedPriceList) {
          grossUnitPrice = PriceAdjustment.calculatePriceActual(order, product, qtyOrdered,
              baseUnitPrice);
          info.addResult(INPGROSSPRICESTD, baseUnitPrice);
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
    if (!StringUtils.equals(strChanged, INPQTYORDERED)
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

  /**
   * Calculates the base unit price that corresponds to the manual discount entered by the user.
   *
   * @param newDiscount
   *     manual discount percentage entered in the line
   * @param priceList
   *     list price used as the reference for the discount calculation
   * @param pricePrecision
   *     currency price precision applied to the resulting base price
   * @return the base unit price before applying any price adjustment
   */
  private BigDecimal calculateNewUnitPrice(BigDecimal newDiscount, BigDecimal priceList, int pricePrecision) {
    return priceList.subtract(newDiscount.multiply(priceList).divide(BigDecimal.valueOf(100))).setScale(pricePrecision,
        RoundingMode.HALF_UP);
  }

  /**
   * Calculates the manual discount percentage implied by a list price and a base unit price.
   *
   * @param priceList
   *     list price used as the discount reference
   * @param unitPrice
   *     base unit price after applying the manual discount only
   * @param stdPrecision
   *     standard precision used to round the resulting discount percentage
   * @return the manual discount percentage represented by the provided prices
   */
  private BigDecimal calculateDiscountFromPrices(BigDecimal priceList, BigDecimal unitPrice, int stdPrecision) {
    return priceList.subtract(unitPrice).multiply(new BigDecimal("100")).divide(priceList, stdPrecision,
        RoundingMode.HALF_UP);
  }

  /**
   * Checks whether the current final price already matches the result of applying active price
   * adjustments to the provided base price.
   *
   * @param order
   *     order used to resolve the applicable price adjustments
   * @param product
   *     product whose price adjustments are evaluated
   * @param qtyOrdered
   *     quantity used during price adjustment evaluation
   * @param basePrice
   *     base price before applying price adjustments
   * @param finalPrice
   *     current final price present in the line
   * @return {@code true} when the final price is already explained by the active price adjustments
   */
  private boolean isPriceAdjustmentAlreadyApplied(Order order, Product product, BigDecimal qtyOrdered,
      BigDecimal basePrice, BigDecimal finalPrice) {
    return order != null && product != null && qtyOrdered != null && basePrice != null && finalPrice != null && PriceAdjustment.calculatePriceActual(
        order, product, qtyOrdered, basePrice).compareTo(finalPrice) == 0;
  }

  /**
   * Checks whether the current final price is already explained by the current manual discount and
   * the active price adjustments.
   *
   * @param order
   *     order used to resolve the applicable price adjustments
   * @param product
   *     product whose price adjustments are evaluated
   * @param qtyOrdered
   *     quantity used during price adjustment evaluation
   * @param basePrice
   *     base price resulting from the current manual discount
   * @param finalPrice
   *     current final price present in the line
   * @param cancelPriceAd
   *     flag indicating whether price adjustments are disabled for the line
   * @return {@code true} when the current final price is already explained by the current manual
   *     discount and the active price adjustments
   */
  private boolean isDiscountAlreadyApplied(Order order, Product product, BigDecimal qtyOrdered, BigDecimal basePrice,
      BigDecimal finalPrice, boolean cancelPriceAd) {
    if (basePrice == null || finalPrice == null || (!cancelPriceAd && (order == null || product == null || qtyOrdered == null))) {
      return false;
    }

    BigDecimal expectedFinalPrice = cancelPriceAd ? basePrice : PriceAdjustment.calculatePriceActual(order, product,
        qtyOrdered, basePrice);
    return expectedFinalPrice.compareTo(finalPrice) == 0;
  }
}
