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

public class DocumentPostAssert {

  final private String productId;
  final private String account;
  final private BigDecimal debit;
  final private BigDecimal credit;
  final private BigDecimal quantity;

  public DocumentPostAssert(String account, BigDecimal debit, BigDecimal credit,
      BigDecimal quantity) {
    this(null, account, debit, credit, quantity);
  }

  public DocumentPostAssert(String productId, String account, BigDecimal debit, BigDecimal credit,
      BigDecimal quantity) {
    this.productId = productId;
    this.account = account;
    this.debit = debit;
    this.credit = credit;
    this.quantity = quantity;
  }

  public String getProductId() {
    return productId;
  }

  public String getAccount() {
    return account;
  }

  public BigDecimal getDebit() {
    return debit;
  }

  public BigDecimal getCredit() {
    return credit;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

}
