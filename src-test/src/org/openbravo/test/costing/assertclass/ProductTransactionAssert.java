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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.costing.assertclass;

import java.math.BigDecimal;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.materialmgmt.cost.InventoryAmountUpdateLine;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.model.materialmgmt.transaction.InternalMovementLine;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.test.costing.utils.TestCostingConstants;

public class ProductTransactionAssert {

  final private ShipmentInOutLine shipmentReceiptLine;
  final private InventoryAmountUpdateLine inventoryLine;
  final private InternalMovementLine movementLine;
  final private InternalConsumptionLine consumptionLine;
  final private ProductionLine productionLine;
  final private Currency currency;
  final private BigDecimal originalPrice;
  final private BigDecimal totalPrice;
  final private BigDecimal unitPrice;
  final private boolean priceDifference;
  final private boolean permanent;

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, BigDecimal originalPrice,
      BigDecimal finalPrice, String costingStatus) {
    this(shipmentReceiptLine, null, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, BigDecimal originalPrice,
      BigDecimal finalPrice) {
    this(shipmentReceiptLine, null, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, String currencyId,
      BigDecimal originalPrice, BigDecimal finalPrice) {
    this(shipmentReceiptLine, null, null, null, null, currencyId, originalPrice, finalPrice,
        finalPrice, false, false);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, BigDecimal originalPrice,
      BigDecimal finalPrice, boolean permanent) {
    this(shipmentReceiptLine, null, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, permanent);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, BigDecimal originalPrice,
      BigDecimal finalPrice, boolean priceDifference, boolean permanent) {
    this(shipmentReceiptLine, null, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, priceDifference, permanent);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine, BigDecimal originalPrice,
      BigDecimal totalPrice, BigDecimal unitPrice) {
    this(shipmentReceiptLine, null, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        totalPrice, unitPrice, false, false);
  }

  public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine, BigDecimal originalPrice,
      BigDecimal finalPrice) {
    this(null, inventoryLine, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine, BigDecimal originalPrice,
      BigDecimal totalPrice, BigDecimal unitPrice) {
    this(null, inventoryLine, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        totalPrice, unitPrice, false, false);
  }

  public ProductTransactionAssert(InventoryAmountUpdateLine inventoryLine, BigDecimal originalPrice,
      BigDecimal finalPrice, boolean permanent) {
    this(null, inventoryLine, null, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, permanent);
  }

  public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
      BigDecimal finalPrice) {
    this(null, null, movementLine, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
      BigDecimal totalPrice, BigDecimal unitPrice) {
    this(null, null, movementLine, null, null, TestCostingConstants.EURO_ID, originalPrice,
        totalPrice, unitPrice, false, false);
  }

  public ProductTransactionAssert(InternalMovementLine movementLine, BigDecimal originalPrice,
      BigDecimal finalPrice, boolean permanent) {
    this(null, null, movementLine, null, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, permanent);
  }

  public ProductTransactionAssert(InternalConsumptionLine consumptionLine, BigDecimal originalPrice,
      BigDecimal finalPrice) {
    this(null, null, null, consumptionLine, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(InternalConsumptionLine consumptionLine, BigDecimal originalPrice,
      BigDecimal finalPrice, boolean permanent) {
    this(null, null, null, consumptionLine, null, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, permanent);
  }

  public ProductTransactionAssert(ProductionLine productionLine, BigDecimal originalPrice,
      BigDecimal finalPrice) {
    this(null, null, null, null, productionLine, TestCostingConstants.EURO_ID, originalPrice,
        finalPrice, finalPrice, false, false);
  }

  public ProductTransactionAssert(ProductionLine productionLine, BigDecimal originalPrice,
      BigDecimal totalCost, BigDecimal finalPrice) {
    this(null, null, null, null, productionLine, TestCostingConstants.EURO_ID, originalPrice,
        totalCost, finalPrice, false, false);
  }

  public ProductTransactionAssert(ShipmentInOutLine shipmentReceiptLine,
      InventoryAmountUpdateLine inventoryLine, InternalMovementLine movementLine,
      InternalConsumptionLine consumptionLine, ProductionLine productionLine, String currencyId,
      BigDecimal originalPrice, BigDecimal totalPrice, BigDecimal unitPrice,
      boolean priceDifference, boolean permanent) {
    this.shipmentReceiptLine = shipmentReceiptLine;
    this.inventoryLine = inventoryLine;
    this.movementLine = movementLine;
    this.consumptionLine = consumptionLine;
    this.productionLine = productionLine;
    this.currency = OBDal.getInstance().get(Currency.class, currencyId);
    this.originalPrice = originalPrice;
    this.totalPrice = totalPrice;
    this.unitPrice = unitPrice;
    this.priceDifference = priceDifference;
    this.permanent = permanent;
  }

  public ShipmentInOutLine getShipmentReceiptLine() {
    return shipmentReceiptLine;
  }

  public InventoryAmountUpdateLine getInventoryLine() {
    return inventoryLine;
  }

  public InternalMovementLine getMovementLine() {
    return movementLine;
  }

  public InternalConsumptionLine getConsumptionLine() {
    return consumptionLine;
  }

  public ProductionLine getProductionLine() {
    return productionLine;
  }

  public Currency getCurrency() {
    return currency;
  }

  public BigDecimal getOriginalPrice() {
    return originalPrice;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public boolean isPriceDifference() {
    return priceDifference;
  }

  public boolean isPermanent() {
    return permanent;
  }
}
