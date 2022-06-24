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
 * All portions are Copyright (C) 2018-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.common.actionhandler.createlinesfromprocess;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.Dependent;

import org.apache.commons.lang.ArrayUtils;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.model.pricing.pricelist.PriceList;

@Dependent
@Qualifier(CreateLinesFromProcessHook.CREATE_LINES_FROM_PROCESS_HOOK_QUALIFIER)
class UpdatePricesAndAmounts extends CreateLinesFromProcessHook {

  @Override
  public int getOrder() {
    return -30;
  }

  /**
   * Updates prices and amounts. If the copied line is related with an order line then copy exactly
   * the same prices from the original line. If the copied line is unrelated with an order line then
   * if the product has a product price in the invoice price list then all prices and amounts will
   * be recalculated using currency precisions and taking into account if the price list includes
   * taxes or not.
   * 
   */
  @Override
  public void exec() {
    if (isCopiedFromOrderLine()
        || CreateLinesFromUtil.hasRelatedOrderLine((ShipmentInOutLine) getCopiedFromLine())) {
      setPricesBasedOnOrderLineValues(isCopiedFromOrderLine() ? (OrderLine) getCopiedFromLine()
          : ((ShipmentInOutLine) getCopiedFromLine()).getSalesOrderLine());
    } else {
      Product product = (Product) getCopiedFromLine().get("product");
      setPricesBasedOnBOM(product, getInvoice().getPriceList());
    }
  }

  private void setPricesBasedOnOrderLineValues(OrderLine orderLine) {
    PriceInformation priceInformation = new PriceInformation();
    BigDecimal qtyOrdered = CreateLinesFromUtil.getOrderedQuantity(getPickExecJSONObject());

    // Standard and Price precision
    Currency invoiceCurrency = getInvoice().getCurrency();
    int stdPrecision = invoiceCurrency.getStandardPrecision().intValue();

    // Price List, Price Standard and discount
    BigDecimal priceActual = orderLine.getUnitPrice();
    BigDecimal priceStd = orderLine.getStandardPrice();
    if (orderLine.isCancelPriceAdjustment() && orderLine.getSalesOrder().isSalesTransaction()) {
      priceActual = priceStd;
    }
    BigDecimal priceList = orderLine.getListPrice();
    BigDecimal priceLimit = orderLine.getPriceLimit();

    BigDecimal lineNetAmount = qtyOrdered.multiply(priceActual)
        .setScale(stdPrecision, RoundingMode.HALF_UP);

    // Processing for Prices Including Taxes
    if (getInvoice().getPriceList().isPriceIncludesTax()) {
      BigDecimal grossUnitPrice = orderLine.getGrossUnitPrice();
      BigDecimal grossBaseUnitPrice = orderLine.getBaseGrossUnitPrice();
      BigDecimal grossListPrice = orderLine.getGrossListPrice();
      BigDecimal grossAmount = qtyOrdered.multiply(grossUnitPrice)
          .setScale(stdPrecision, RoundingMode.HALF_UP);

      // Set gross price information
      priceInformation.setGrossUnitPrice(grossUnitPrice);
      priceInformation.setGrossBaseUnitPrice(grossBaseUnitPrice);
      priceInformation.setGrossListPrice(grossListPrice);
      priceInformation.setLineGrossAmount(grossAmount);

      // Update Net Prices to 0
      priceActual = BigDecimal.ZERO;
      priceList = BigDecimal.ZERO;
      priceLimit = BigDecimal.ZERO;
      priceStd = BigDecimal.ZERO;
    }

    priceInformation.setUnitPrice(priceActual);
    priceInformation.setStandardPrice(priceStd);
    priceInformation.setListPrice(priceList);
    priceInformation.setLineNetAmount(lineNetAmount);
    priceInformation.setPriceLimit(priceLimit);

    setPrices(priceInformation);
  }

  private void setPrices(final PriceInformation priceInformation) {
    // Net Prices
    getInvoiceLine().setUnitPrice(priceInformation.getUnitPrice());
    getInvoiceLine().setListPrice(priceInformation.getListPrice());
    getInvoiceLine().setStandardPrice(priceInformation.getStandardPrice());
    // Gross Prices
    getInvoiceLine().setGrossUnitPrice(priceInformation.getGrossUnitPrice());
    getInvoiceLine().setGrossListPrice(priceInformation.getGrossListPrice());
    getInvoiceLine().setBaseGrossUnitPrice(priceInformation.getGrossBaseUnitPrice());
    getInvoiceLine().setGrossAmount(priceInformation.getLineGrossAmount());
    // Price Limit and Line Net Amount
    getInvoiceLine().setPriceLimit(priceInformation.getPriceLimit());
    getInvoiceLine().setLineNetAmount(priceInformation.getLineNetAmount());
  }

  private void setPricesBasedOnBOM(Product product, PriceList invoicePriceList) {
    PriceInformation priceInformation = getBOMPrices(product, invoicePriceList);
    if (priceInformation == null) {
      setPricesToZero();
    } else {
      BigDecimal qtyOrdered = CreateLinesFromUtil.getOrderedQuantity(getPickExecJSONObject());

      // Standard and Price precision
      Currency invoiceCurrency = getInvoice().getCurrency();
      int stdPrecision = invoiceCurrency.getStandardPrecision().intValue();
      int pricePrecision = invoiceCurrency.getPricePrecision().intValue();

      // Price List, Price Standard and Price Limit
      BigDecimal priceActual = priceInformation.getOffersPriceInvoice()
          .setScale(pricePrecision, RoundingMode.HALF_UP);
      BigDecimal priceStd = priceInformation.getStandardPrice()
          .setScale(pricePrecision, RoundingMode.HALF_UP);
      BigDecimal priceList = priceInformation.getListPrice()
          .setScale(pricePrecision, RoundingMode.HALF_UP);
      BigDecimal priceLimit = priceInformation.getPriceLimit()
          .setScale(pricePrecision, RoundingMode.HALF_UP);

      BigDecimal lineNetAmount = qtyOrdered.multiply(priceActual)
          .setScale(stdPrecision, RoundingMode.HALF_UP);

      // Processing for Prices Including Taxes
      if (getInvoice().getPriceList().isPriceIncludesTax()) {
        BigDecimal grossUnitPrice = priceStd;
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
        priceStd = BigDecimal.ZERO;
      }

      priceInformation.setUnitPrice(priceActual);
      priceInformation.setStandardPrice(priceStd);
      priceInformation.setListPrice(priceList);
      priceInformation.setLineNetAmount(lineNetAmount);
      priceInformation.setPriceLimit(priceLimit);

      setPrices(priceInformation);
    }
  }

  /**
   * Returns the defined product price in a selected pricelist taking into account BOM prices and
   * offers or null if the product doesn't has any price defined on the price list
   * 
   * @param product
   *          The product where the price is searched.
   * @param priceList
   *          The price list where the product price is searched.
   * @return The product price defined for the product in the price list or NULL if any.
   */
  private PriceInformation getBOMPrices(Product product, PriceList priceList) {
    Object[] bomPrices = selectBOMPrices(product, priceList);
    if (bomPrices.length == 0) {
      return null;
    }
    PriceInformation priceInformation = new PriceInformation();
    priceInformation.setStandardPrice((BigDecimal) bomPrices[0]);
    priceInformation.setListPrice((BigDecimal) bomPrices[1]);
    priceInformation.setPriceLimit((BigDecimal) bomPrices[2]);
    priceInformation.setOffersPriceInvoice((BigDecimal) bomPrices[3]);
    return priceInformation;
  }

  @SuppressWarnings("unchecked")
  private Object[] selectBOMPrices(Product product, PriceList priceList) {
    //@formatter:off
    String hql =
            " select " +
            "   TO_NUMBER(M_BOM_PriceStd(:productID, plv.id)), " +
            "   TO_NUMBER(M_BOM_PriceList(:productID, plv.id)), " +
            "   TO_NUMBER(M_BOM_PriceLimit(:productID, plv.id)), " +
            "   TO_NUMBER(ROUND(TO_NUMBER(M_BOM_PriceStd(:productID, plv.id)), :pricePrecision)) " +
            " from PricingProductPrice pp " +
            "   join pp.priceListVersion plv " +
            " where pp.product.id = :productID" +
            "   and plv.priceList.id = :priceListID" +
            "   and plv.active = true" +
            "   and (plv.validFromDate is null or plv.validFromDate <= :validFromDate)" +
            " order by plv.validFromDate desc";
    //@formatter:on

    List<Object[]> prices = OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("productID", product.getId())
        .setParameter("priceListID", priceList.getId())
        .setParameter("validFromDate", new Date())
        .setParameter("pricePrecision", getInvoice().getCurrency().getPricePrecision())
        .setMaxResults(1)
        .getResultList();

    if (prices.isEmpty()) {
      return ArrayUtils.EMPTY_OBJECT_ARRAY;
    } else {
      return prices.get(0);
    }
  }

  private void setPricesToZero() {
    PriceInformation zeroPrices = new PriceInformation();
    setPrices(zeroPrices);
  }

  private class PriceInformation {
    // Net Prices
    private BigDecimal unitPrice;
    private BigDecimal standardPrice;
    private BigDecimal listPrice;
    // Gross Prices
    private BigDecimal grossUnitPrice;
    private BigDecimal grossBaseUnitPrice;
    private BigDecimal grossListPrice;
    private BigDecimal lineGrossAmount;
    private BigDecimal priceLimit;
    // Amounts
    private BigDecimal lineNetAmount;
    // Offer
    private BigDecimal offersPriceInvoice;

    private PriceInformation() {
      this.priceLimit = BigDecimal.ZERO;
      this.unitPrice = BigDecimal.ZERO;
      this.standardPrice = BigDecimal.ZERO;
      this.listPrice = BigDecimal.ZERO;
      this.setLineNetAmount(BigDecimal.ZERO);
      this.grossUnitPrice = BigDecimal.ZERO;
      this.grossBaseUnitPrice = BigDecimal.ZERO;
      this.grossListPrice = BigDecimal.ZERO;
      this.lineGrossAmount = BigDecimal.ZERO;
      this.offersPriceInvoice = BigDecimal.ZERO;
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

    private BigDecimal getLineNetAmount() {
      return lineNetAmount;
    }

    private void setLineNetAmount(BigDecimal lineNetAmount) {
      this.lineNetAmount = lineNetAmount;
    }

    private BigDecimal getOffersPriceInvoice() {
      return offersPriceInvoice;
    }

    private void setOffersPriceInvoice(BigDecimal offersPrice) {
      this.offersPriceInvoice = offersPrice;
    }
  }
}
