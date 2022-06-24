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
import org.openbravo.model.common.plm.Product;

public class PhysicalInventoryAssert {

  final private Product product;
  final private BigDecimal price;
  final private BigDecimal quantity;
  final private int day;

  public PhysicalInventoryAssert(Product product, BigDecimal price, BigDecimal quantity, int day) {
    this.product = OBDal.getInstance().get(Product.class, product.getId());
    this.price = price;
    this.quantity = quantity;
    this.day = day;
  }

  public Product getProduct() {
    return product;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public int getDay() {
    return day;
  }

}
