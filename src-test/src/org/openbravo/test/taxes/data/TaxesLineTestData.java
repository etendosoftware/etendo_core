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
 * All portions are Copyright (C) 2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.taxes.data;

import java.math.BigDecimal;
import java.util.HashMap;

public class TaxesLineTestData {

  private String productId;
  private BigDecimal quantity;
  private BigDecimal price;
  private BigDecimal quantityUpdated;
  private BigDecimal priceUpdated;
  private String taxid;
  private HashMap<String, String[]> lineTaxes;
  private String[] lineAmounts;

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public BigDecimal getQuantityUpdated() {
    return quantityUpdated;
  }

  public void setQuantityUpdated(BigDecimal quantityUpdated) {
    this.quantityUpdated = quantityUpdated;
  }

  public BigDecimal getPriceUpdated() {
    return priceUpdated;
  }

  public void setPriceUpdated(BigDecimal priceUpdated) {
    this.priceUpdated = priceUpdated;
  }

  public String getTaxid() {
    return taxid;
  }

  public void setTaxid(String taxid) {
    this.taxid = taxid;
  }

  public HashMap<String, String[]> getLineTaxes() {
    return lineTaxes;
  }

  public void setLinetaxes(HashMap<String, String[]> lineTaxes) {
    this.lineTaxes = lineTaxes;
  }

  public String[] getLineAmounts() {
    return lineAmounts;
  }

  public void setLineAmounts(String[] lineAmounts) {
    this.lineAmounts = lineAmounts;
  }
}
