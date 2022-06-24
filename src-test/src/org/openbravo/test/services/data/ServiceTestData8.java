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

public class ServiceTestData8 extends ServiceTestData {

  @Override
  public void initialize() {
    setTestNumber("BACK-401");
    setTestDescription("Modify Ordered Quantity of a Sales Order Line related to three services");
    setBpartnerId(BP_CUSTOMER_A);
    setProductId(PRODUCT_DISTRIBUTION_GOOD_A);
    setQuantity(new BigDecimal("15.00"));
    setPrice(new BigDecimal("10.00"));
    setProductChangedQty(new BigDecimal("10.00"));
    setServices(new String[][] {
        // ProductId, quantity, price, amount
        { SERVICE_WARRANTY, "1", "200.00", "2000.00" },
        { SERVICE_INSURANCE, "1", "25.00", "250.00" },
        { SERVICE_TRANSPORTATION, "1", "250.00", "250.00" } });
    setServicesResults(new String[][] {
        // ProductId, quantity, price, amount
        { SERVICE_WARRANTY, "10", "202.00", "2020.00" },
        { SERVICE_INSURANCE, "10", "26.50", "265.00" },
        { SERVICE_TRANSPORTATION, "1", "250.00", "250.00" } });
    setPricelistId(PRICELIST_SALES);
  }
}
