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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.taxes.data;

public class OrderLineTestData {

  private final String product;
  private final String tax;
  private final String quantity;
  private final String price;
  private final String expectedTax;
  private final String expectedNet;
  private final String expectedGross;
  private final String expectedTax2;
  private final String expectedNet2;
  private final String expectedGross2;

  public OrderLineTestData(final String product, final String tax, final String quantity,
      final String price, final String expectedTax, final String expectedNet,
      final String expectedGross, final String expectedTax2, final String expectedNet2,
      final String expectedGross2) {
    super();
    this.product = product;
    this.tax = tax;
    this.quantity = quantity;
    this.price = price;
    this.expectedTax = expectedTax;
    this.expectedNet = expectedNet;
    this.expectedGross = expectedGross;
    this.expectedTax2 = expectedTax2;
    this.expectedNet2 = expectedNet2;
    this.expectedGross2 = expectedGross2;
  }

  public String getProduct() {
    return product;
  }

  public String getTax() {
    return tax;
  }

  public String getQuantity() {
    return quantity;
  }

  public String getPrice() {
    return price;
  }

  public String getExpectedTax() {
    return expectedTax;
  }

  public String getExpectedNet() {
    return expectedNet;
  }

  public String getExpectedGross() {
    return expectedGross;
  }

  public String getExpectedTax2() {
    return expectedTax2;
  }

  public String getExpectedNet2() {
    return expectedNet2;
  }

  public String getExpectedGross2() {
    return expectedGross2;
  }
}
