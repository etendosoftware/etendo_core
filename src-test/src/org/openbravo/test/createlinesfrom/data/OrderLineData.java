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

package org.openbravo.test.createlinesfrom.data;

import java.math.BigDecimal;

/**
 * Line data to check Create Line From process
 * 
 * @author Andy Armaignac
 *
 */
public class OrderLineData {

  private String productId;
  private BigDecimal orderedQuantity;
  private BigDecimal lineNetAmount;

  private OrderLineData() {
  }

  public static class Builder {
    private OrderLineData lineData = new OrderLineData();

    public Builder productId(String productId) {
      lineData.productId = productId;
      return this;
    }

    public Builder orderedQuantity(BigDecimal orderedQuantity) {
      lineData.orderedQuantity = orderedQuantity;
      return this;
    }

    public Builder lineNetAmount(BigDecimal lineNetAmount) {
      lineData.lineNetAmount = lineNetAmount;
      return this;
    }

    public OrderLineData build() {
      return lineData;
    }
  }

  public String getProductId() {
    return productId;
  }

  public BigDecimal getOrderedQuantity() {
    return orderedQuantity;
  }

  public BigDecimal getLineNetAmount() {
    return lineNetAmount;
  }
}
