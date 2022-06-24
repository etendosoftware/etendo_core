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
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.test.costing.utils.TestCostingConstants;

public class ProductCostingAssert {

  final private MaterialTransaction transaction;
  final private Warehouse warehouse;
  final private BigDecimal price;
  final private BigDecimal originalCost;
  final private BigDecimal finalCost;
  final private BigDecimal quantity;
  final private String type;
  final private int year;
  final private boolean manual;

  public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
      BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity) {
    this(transaction, TestCostingConstants.SPAIN_WAREHOUSE_ID, price, originalCost, finalCost,
        quantity);
  }

  public ProductCostingAssert(MaterialTransaction transaction, String warehouseId, BigDecimal price,
      BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity) {
    this(transaction, warehouseId, price, originalCost, finalCost, quantity, "AVA", 0, false);
  }

  public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
      BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity, String type) {
    this(transaction, price, originalCost, finalCost, quantity, type, 0);
  }

  public ProductCostingAssert(MaterialTransaction transaction, BigDecimal price,
      BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity, String type, int year) {
    this(transaction, TestCostingConstants.SPAIN_WAREHOUSE_ID, price, originalCost, finalCost,
        quantity, type, year, true);
  }

  public ProductCostingAssert(MaterialTransaction transaction, String warehouseId, BigDecimal price,
      BigDecimal originalCost, BigDecimal finalCost, BigDecimal quantity, String type, int year,
      boolean manual) {
    this.transaction = transaction;
    this.warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
    this.price = price;
    this.originalCost = originalCost;
    this.finalCost = finalCost;
    this.quantity = quantity;
    this.type = type;
    this.year = year;
    this.manual = manual;
  }

  public MaterialTransaction getTransaction() {
    return transaction;
  }

  public Warehouse getWarehouse() {
    return warehouse;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getOriginalCost() {
    return originalCost;
  }

  public BigDecimal getFinalCost() {
    return finalCost;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public String getType() {
    return type;
  }

  public int getYear() {
    return year;
  }

  public boolean isManual() {
    return manual;
  }

}
