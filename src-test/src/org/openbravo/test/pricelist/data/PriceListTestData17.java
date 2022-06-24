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

package org.openbravo.test.pricelist.data;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Data used for Test: Price List Schema with more than one rule applied. One rule with 50% unit
 * price for one product, and second one with one product of of same Category and an unit price of
 * 50%
 * 
 * @author Andy Armaignac
 *
 */
public class PriceListTestData17 extends PriceListTestData {

  @Override
  public void initialize() {

    // Define rule to be applied for a product
    PriceListSchemaLineTestData ruleLine1 = new PriceListSchemaLineTestData();
    ruleLine1.setProductId(PriceListTestConstants.FINAL_GOOD_A_PRODUCT_ID);
    ruleLine1.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine1.setSurchargeListPriceAmount(BigDecimal.ZERO);
    ruleLine1.setListPriceDiscount(BigDecimal.ZERO);
    ruleLine1.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine1.setSurchargeStandardPriceAmount(BigDecimal.ZERO);
    ruleLine1.setStandardPriceDiscount(BigDecimal.valueOf(50L));

    // Define a rule to be applied for the second product
    PriceListSchemaLineTestData ruleLine2 = new PriceListSchemaLineTestData();
    ruleLine2.setProductId(PriceListTestConstants.FINAL_GOOD_B_PRODUCT_ID);
    ruleLine2.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine2.setSurchargeListPriceAmount(BigDecimal.ZERO);
    ruleLine2.setListPriceDiscount(BigDecimal.ZERO);
    ruleLine2.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine2.setSurchargeStandardPriceAmount(BigDecimal.ZERO);
    ruleLine2.setStandardPriceDiscount(BigDecimal.valueOf(50L));

    // Add lines
    setTestPriceListRulesData(new PriceListSchemaLineTestData[] { ruleLine1, ruleLine2 });

    /**
     * This Map will be used to verify Product Prices values after test is executed. Map has the
     * following structure: <Product name, [Unit Price Expected, List Price Expected]>
     */
    HashMap<String, String[]> productPriceLines = new HashMap<String, String[]>();
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_A_PRODUCT_NAME,
        new String[] { "1.00", "2.00" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_B_PRODUCT_NAME,
        new String[] { "1.00", "2.00" });
    setExpectedProductPrices(productPriceLines);

    // Price List Header
    setOrganizationId(PriceListTestConstants.SPAIN_ORGANIZATION_ID);
    setPriceListName(PriceListTestConstants.PRICE_LIST_NAME);
    setCurrencyId(PriceListTestConstants.EUR_CURRENCY_ID);
    setSalesPrice(true);
    setBasedOnCost(false);
    setPriceIncludesTax(false);
    setDefault(true);

    // Price List Version
    setBasePriceListVersionId(PriceListTestConstants.CUSTOMER_A_PRICE_LIST_VERSION_ID);

  }
}
