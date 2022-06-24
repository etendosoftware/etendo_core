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
 * All portions are Copyright (C) 2017-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.copyfromorderprocess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import javax.enterprise.context.Dependent;

import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;

@Dependent
@Qualifier(CopyFromOrdersProcessImplementationInterface.COPY_FROM_ORDER_PROCESS_HOOK_QUALIFIER)
class UpdatePricesAndAmounts implements CopyFromOrdersProcessImplementationInterface {
  private OrderLine newOrderLine;
  private Order processingOrder;

  @Override
  public int getOrder() {
    return -20;
  }

  /**
   * Updates prices and amounts. If the product has a product price in the order price list then all
   * prices and amounts will be recalculated using currency precisions and taking into account if
   * the price list includes taxes or not.
   * 
   */
  @Override
  public void exec(final Order processingOrderParam, final OrderLine orderLineParam,
      OrderLine newOrderLineParam) {
    this.newOrderLine = newOrderLineParam;
    this.processingOrder = processingOrderParam;

    ProductPrice productPrice = getProductPriceInPriceList(newOrderLine.getProduct(),
        processingOrder.getPriceList());
    if (productPrice != null) {
      setPricesBasedOnPriceList(productPrice);
    } else {
      setPricesToZero();
    }
  }

  private void setPricesBasedOnPriceList(final ProductPrice productPrice) {
    PriceInformation priceInformation = new PriceInformation();
    BigDecimal qtyOrdered = newOrderLine.getOrderedQuantity();

    // Standard and Price precision
    Currency orderCurrency = processingOrder.getCurrency();
    int stdPrecision = orderCurrency.getStandardPrecision().intValue();
    int pricePrecision = orderCurrency.getPricePrecision().intValue();

    // Price List, Price Standard and discount
    BigDecimal priceActual = productPrice.getStandardPrice()
        .setScale(pricePrecision, RoundingMode.HALF_UP);
    BigDecimal priceList = productPrice.getListPrice()
        .setScale(pricePrecision, RoundingMode.HALF_UP);
    BigDecimal priceLimit = productPrice.getPriceLimit()
        .setScale(pricePrecision, RoundingMode.HALF_UP);

    BigDecimal discount = BigDecimal.ZERO;
    if (productPrice.getListPrice().compareTo(BigDecimal.ZERO) != 0) {
      // Discount = ((PL-PA)/PL)*100
      discount = priceList.subtract(priceActual)
          .multiply(new BigDecimal("100"))
          .divide(priceList, stdPrecision, RoundingMode.HALF_UP);
    }

    // Processing for Prices Including Taxes
    if (processingOrder.getPriceList().isPriceIncludesTax()) {
      BigDecimal grossUnitPrice = priceActual;
      BigDecimal grossAmount = qtyOrdered.multiply(grossUnitPrice)
          .setScale(stdPrecision, RoundingMode.HALF_UP);

      // Set gross price information
      priceInformation.setGrossUnitPrice(grossUnitPrice);
      priceInformation.setGrossBaseUnitPrice(grossUnitPrice);
      priceInformation.setGrossListPrice(priceList);
      priceInformation.setLineGrossAmount(grossAmount);

      // Update Net Prices to 0
      priceActual = BigDecimal.ZERO;
      priceList = BigDecimal.ZERO;
      priceLimit = BigDecimal.ZERO;
    }

    priceInformation.setUnitPrice(priceActual);
    priceInformation.setStandardPrice(priceActual);
    priceInformation.setListPrice(priceList);

    priceInformation.setDiscount(discount);
    priceInformation.setPriceLimit(priceLimit);

    setPrices(priceInformation);

  }

  private void setPricesToZero() {
    PriceInformation zeroPrices = new PriceInformation();
    setPrices(zeroPrices);
  }

  private void setPrices(final PriceInformation priceInformation) {
    // Net Prices
    newOrderLine.setUnitPrice(priceInformation.getUnitPrice());
    newOrderLine.setListPrice(priceInformation.getListPrice());
    newOrderLine.setStandardPrice(priceInformation.getStandardPrice());
    // Gross Prices
    newOrderLine.setGrossUnitPrice(priceInformation.getGrossUnitPrice());
    newOrderLine.setGrossListPrice(priceInformation.getGrossListPrice());
    newOrderLine.setBaseGrossUnitPrice(priceInformation.getGrossBaseUnitPrice());
    newOrderLine.setLineGrossAmount(priceInformation.getLineGrossAmount());
    // Discount and Price Limit
    newOrderLine.setDiscount(priceInformation.getDiscount());
    newOrderLine.setPriceLimit(priceInformation.getPriceLimit());
  }

  /**
   * Return the defined product price in a selected pricelist or null if the product doesn't has any
   * price defined on the price list
   * 
   * @param product
   *          The product where the price is searched.
   * @param priceList
   *          The price list where the product price is searched.
   * @return The product price defined for the product in the price list or NULL if any.
   */
  private ProductPrice getProductPriceInPriceList(final Product product,
      final PriceList priceList) {
    //@formatter:off
    String hql =
            "as pp " +
            "  join pp.priceListVersion plv " +
            " where pp.product.id = :productID" +
            "   and plv.priceList.id = :priceListID" +
            "   and plv.active = true" +
            "   and (plv.validFromDate is null or plv.validFromDate <= :validFromDate)" +
            " order by plv.validFromDate desc";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(ProductPrice.class, hql)
        .setNamedParameter("productID", product.getId())
        .setNamedParameter("priceListID", priceList.getId())
        .setNamedParameter("validFromDate", new Date())
        .setMaxResult(1)
        .uniqueResult();
  }

  private static class PriceInformation {
    // Net Prices
    BigDecimal unitPrice;
    BigDecimal standardPrice;
    BigDecimal listPrice;
    // Gross Prices
    BigDecimal grossUnitPrice;
    BigDecimal grossBaseUnitPrice;
    BigDecimal grossListPrice;
    BigDecimal lineGrossAmount;
    // Discount and price Limit
    BigDecimal discount;
    BigDecimal priceLimit;

    private PriceInformation() {
      this.discount = BigDecimal.ZERO;
      this.priceLimit = BigDecimal.ZERO;
      this.unitPrice = BigDecimal.ZERO;
      this.standardPrice = BigDecimal.ZERO;
      this.listPrice = BigDecimal.ZERO;
      this.grossUnitPrice = BigDecimal.ZERO;
      this.grossBaseUnitPrice = BigDecimal.ZERO;
      this.grossListPrice = BigDecimal.ZERO;
      this.lineGrossAmount = BigDecimal.ZERO;
    }

    private BigDecimal getDiscount() {
      return discount;
    }

    private void setDiscount(final BigDecimal discount) {
      this.discount = discount;
    }

    private BigDecimal getPriceLimit() {
      return priceLimit;
    }

    private void setPriceLimit(final BigDecimal priceLimit) {
      this.priceLimit = priceLimit;
    }

    private BigDecimal getUnitPrice() {
      return unitPrice;
    }

    private void setUnitPrice(final BigDecimal unitPrice) {
      this.unitPrice = unitPrice;
    }

    private BigDecimal getStandardPrice() {
      return standardPrice;
    }

    private void setStandardPrice(final BigDecimal priceStandard) {
      this.standardPrice = priceStandard;
    }

    private BigDecimal getListPrice() {
      return listPrice;
    }

    private void setListPrice(final BigDecimal listPrice) {
      this.listPrice = listPrice;
    }

    private BigDecimal getGrossUnitPrice() {
      return grossUnitPrice;
    }

    private void setGrossUnitPrice(final BigDecimal grossUnitPrice) {
      this.grossUnitPrice = grossUnitPrice;
    }

    private BigDecimal getGrossBaseUnitPrice() {
      return grossBaseUnitPrice;
    }

    private void setGrossBaseUnitPrice(final BigDecimal grossBaseUnitPrice) {
      this.grossBaseUnitPrice = grossBaseUnitPrice;
    }

    private BigDecimal getGrossListPrice() {
      return grossListPrice;
    }

    private void setGrossListPrice(final BigDecimal grossListPrice) {
      this.grossListPrice = grossListPrice;
    }

    private BigDecimal getLineGrossAmount() {
      return lineGrossAmount;
    }

    private void setLineGrossAmount(final BigDecimal lineGrossAmount) {
      this.lineGrossAmount = lineGrossAmount;
    }
  }

}
