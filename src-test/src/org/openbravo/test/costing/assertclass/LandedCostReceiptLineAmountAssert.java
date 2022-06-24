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

import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class LandedCostReceiptLineAmountAssert {

  final private LandedCostCost landedCostCost;
  final private ShipmentInOutLine receiptLine;
  final private BigDecimal amount;

  public LandedCostReceiptLineAmountAssert(LandedCostCost landedCostCost,
      ShipmentInOutLine receiptLine, BigDecimal amount) {
    this.landedCostCost = landedCostCost;
    this.receiptLine = receiptLine;
    this.amount = amount;
  }

  public LandedCostCost getLandedCostCost() {
    return landedCostCost;
  }

  public ShipmentInOutLine getReceiptLine() {
    return receiptLine;
  }

  public BigDecimal getAmount() {
    return amount;
  }

}
