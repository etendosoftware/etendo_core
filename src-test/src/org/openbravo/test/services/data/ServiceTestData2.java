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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.services.data;

import java.math.BigDecimal;

public class ServiceTestData2 extends ServiceTestData {

  @Override
  public void initialize() {
    setTestNumber("BACK-202");
    setTestDescription("Service with three related products and regular pricelist");
    setBpartnerId(BP_CUSTOMER_A);
    setServiceId(SERVICE_WARRANTY);
    setProducts(new String[][] { //
        // ProductId, quantity, price, amount
        { PRODUCT_DISTRIBUTION_GOOD_A, "1", "10", "10" },
        { PRODUCT_DISTRIBUTION_GOOD_B, "1", "5", "5" },
        { PRODUCT_DISTRIBUTION_GOOD_C, "1", "7", "7" } //
    });
    setQuantity(BigDecimal.ONE);
    setPrice(new BigDecimal("200.00"));
    setServicePriceResult(new BigDecimal("204.40"));
    setServiceAmountResult(new BigDecimal("613.20"));
    setServiceQtyResult(new BigDecimal("3"));
    setPricelistId(PRICELIST_SALES);
  }
}
