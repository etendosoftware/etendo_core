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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.cancelandreplace.data;

import java.math.BigDecimal;
import java.util.function.Consumer;

public class CancelAndReplaceOrderTestData {

  BigDecimal quantity;
  BigDecimal paidAmount;
  boolean delivered;
  BigDecimal totalAmount;
  String status;
  BigDecimal receivedPayment;
  BigDecimal outstandingAmount;
  Line[] lines;

  CancelAndReplaceOrderTestData with(Consumer<CancelAndReplaceOrderTestData> builderFunction) {
    builderFunction.accept(this);
    return this;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  public boolean isDelivered() {
    return delivered;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public BigDecimal getReceivedPayment() {
    return receivedPayment;
  }

  public BigDecimal getOutstandingAmount() {
    return outstandingAmount;
  }

  public Line[] getLines() {
    return lines;
  }

  public class Line {
    BigDecimal deliveredQty;
    BigDecimal movementQty;
    BigDecimal shipmentLines;
    BigDecimal orderedQuantity;

    Line with(Consumer<Line> builderFunction) {
      builderFunction.accept(this);
      return this;
    }

    public BigDecimal getDeliveredQty() {
      return deliveredQty;
    }

    public BigDecimal getMovementQty() {
      return movementQty;
    }

    public BigDecimal getShipmentLines() {
      return shipmentLines;
    }

    public BigDecimal getOrderedQuantity() {
      return orderedQuantity;
    }
  }
}
