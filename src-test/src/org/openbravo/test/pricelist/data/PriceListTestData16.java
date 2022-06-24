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

package org.openbravo.test.pricelist.data;

import java.math.BigDecimal;
import java.util.HashMap;

/**
 * Data used for Test: Price List Schema with more than one rule applied. First rule with different
 * unit price and list price discounts and all products of all Product Categories, a second one only
 * affecting a selected product of a defined Category. Third rule affecting products specific
 * Product Category, and last one for another category.
 * 
 * @author Mark
 *
 */
public class PriceListTestData16 extends PriceListTestData {

  @Override
  public void initialize() {

    // Define the rule to be applied when the price list be generated
    PriceListSchemaLineTestData ruleLine1 = new PriceListSchemaLineTestData();
    ruleLine1.setProductCategoryId(PriceListTestConstants.FINISHED_GOODS_PRODUCT_CATEGORY);
    ruleLine1.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine1.setListPriceDiscount(new BigDecimal("11.32"));
    ruleLine1.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine1.setStandardPriceDiscount(new BigDecimal("7.63"));

    PriceListSchemaLineTestData ruleLine2 = new PriceListSchemaLineTestData();
    ruleLine2.setProductCategoryId(PriceListTestConstants.FINISHED_GOODS_PRODUCT_CATEGORY);
    ruleLine2.setProductId(PriceListTestConstants.FINAL_GOOD_A_PRODUCT_ID);
    ruleLine2.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine2.setListPriceDiscount(new BigDecimal("10.00"));
    ruleLine2.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine2.setStandardPriceDiscount(new BigDecimal("20.00"));

    PriceListSchemaLineTestData ruleLine3 = new PriceListSchemaLineTestData();
    ruleLine3.setProductCategoryId(PriceListTestConstants.FINISHED_GOODS_PRODUCT_CATEGORY);
    ruleLine3.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine3.setListPriceDiscount(new BigDecimal("21.34"));
    ruleLine3.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine3.setStandardPriceDiscount(new BigDecimal("13.73"));

    PriceListSchemaLineTestData ruleLine4 = new PriceListSchemaLineTestData();
    ruleLine4.setProductCategoryId(PriceListTestConstants.DISTRIBUTION_GOODS_PRODUCT_CATEGORY);
    ruleLine4.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_LIST_PRICE);
    ruleLine4.setListPriceDiscount(new BigDecimal("15.17"));
    ruleLine4.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine4.setStandardPriceDiscount(new BigDecimal("17.13"));

    // Add lines
    setTestPriceListRulesData(
        new PriceListSchemaLineTestData[] { ruleLine1, ruleLine2, ruleLine3, ruleLine4 });

    /**
     * This Map will be used to verify Product Prices values after test is executed. Map has the
     * following structure: <Product name, [Unit Price Expected, List Price Expected]>
     */
    HashMap<String, String[]> productPriceLines = new HashMap<String, String[]>();
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_A_PRODUCT_NAME,
        new String[] { "1.28", "1.25" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_B_PRODUCT_NAME,
        new String[] { "1.60", "1.39" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_C_PRODUCT_NAME,
        new String[] { "1.60", "1.39" });
    productPriceLines.put(PriceListTestConstants.LAPTOP_PRODUCT_NAME,
        new String[] { "1035.88", "1060.38" });
    productPriceLines.put(PriceListTestConstants.SOCCER_BALL_PRODUCT_NAME,
        new String[] { "30.66", "31.39" });
    productPriceLines.put(PriceListTestConstants.T_SHIRTS_PRODUCT_NAME,
        new String[] { "38.95", "39.87" });
    productPriceLines.put(PriceListTestConstants.TENNIS_BALL_PRODUCT_NAME,
        new String[] { "4.14", "4.24" });
    setExpectedProductPrices(productPriceLines);

    // Price List Header
    setOrganizationId(PriceListTestConstants.SPAIN_ORGANIZATION_ID);
    setPriceListName(PriceListTestConstants.PRICE_LIST_NAME);
    setCurrencyId(PriceListTestConstants.EUR_CURRENCY_ID);
    setSalesPrice(true);
    setBasedOnCost(false);
    setPriceIncludesTax(false);

    // Set Base Price List Version
    setBasePriceListVersionId(PriceListTestConstants.CUSTOMER_B_PRICE_LIST_VERSION_ID);
  }

}
