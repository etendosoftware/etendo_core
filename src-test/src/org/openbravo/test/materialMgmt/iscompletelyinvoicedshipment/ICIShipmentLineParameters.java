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

package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import java.math.BigDecimal;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;

public class ICIShipmentLineParameters {

  private ShipmentInOut shipment;
  private Product product;
  private Locator locator;
  private BigDecimal quantity;

  public ICIShipmentLineParameters(ShipmentInOut shipmentParam) {
    this.shipment = shipmentParam;
    this.product = OBDal.getInstance().get(Product.class, ICIConstants.PRODUCT_ID);
    this.locator = OBDal.getInstance().get(Locator.class, ICIConstants.LOCATOR_RN_000_ID);
    this.quantity = new BigDecimal("10");
  }

  public ShipmentInOut getShipment() {
    return shipment;
  }

  public void setShipment(ShipmentInOut shipment) {
    this.shipment = shipment;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Locator getLocator() {
    return locator;
  }

  public void setLocator(Locator locator) {
    this.locator = locator;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

}
