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
 * Data used for Test: Price List Schema with more than one discount line, each associated to
 * different Product Categories and rules. Price List Based on Cost
 * 
 * @author Mark
 *
 */
public class PriceListTestData15 extends PriceListTestData {

  @Override
  public void initialize() {

    // Define rule to be applied for an entire Product Category
    PriceListSchemaLineTestData ruleLine1 = new PriceListSchemaLineTestData();
    ruleLine1.setProductCategoryId(PriceListTestConstants.DISTRIBUTION_GOODS_PRODUCT_CATEGORY);
    ruleLine1.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_COST);
    ruleLine1.setListPriceMargin(new BigDecimal("12.35"));
    ruleLine1.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_COST);
    ruleLine1.setUnitPriceMargin(new BigDecimal("11.35"));

    // Define a rule to be applied only for one product
    PriceListSchemaLineTestData ruleLine2 = new PriceListSchemaLineTestData();
    ruleLine2.setProductCategoryId(PriceListTestConstants.FINISHED_GOODS_PRODUCT_CATEGORY);
    ruleLine2.setBaseListPriceValue(PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_BASED);
    ruleLine2.setFixedListPrice(new BigDecimal("12.00"));
    ruleLine2.setListPriceMargin(new BigDecimal("10.00"));
    ruleLine2.setBaseStandardPriceValue(PriceListTestConstants.REFLIST_VALUE_NET_UNIT_PRICE);
    ruleLine2.setSurchargeStandardPriceAmount(BigDecimal.ZERO);
    ruleLine2.setStandardPriceDiscount(new BigDecimal("20.00"));

    // Add lines
    setTestPriceListRulesData(new PriceListSchemaLineTestData[] { ruleLine1, ruleLine2 });

    /**
     * This Map will be used to verify Product Prices values after test is executed. Map has the
     * following structure: <Product name, [Unit Price Expected, List Price Expected]>
     */
    HashMap<String, String[]> productPriceLines = new HashMap<String, String[]>();
    productPriceLines.put(PriceListTestConstants.DISTRIB_GOOD_A_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.DISTRIB_GOOD_B_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.DISTRIB_GOOD_C_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_A_PRODUCT_NAME,
        new String[] { "8.00", "12.00" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_B_PRODUCT_NAME,
        new String[] { "8.00", "12.00" });
    productPriceLines.put(PriceListTestConstants.FINAL_GOOD_C_PRODUCT_NAME,
        new String[] { "8.00", "12.00" });
    productPriceLines.put(PriceListTestConstants.LAPTOP_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.SOCCER_BALL_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.SPAIN_GOOD_A_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.SPAIN_GOOD_B_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.SPAIN_GOOD_C_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.T_SHIRTS_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.TENNIS_BALL_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.USA_GOOD_A_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.USA_GOOD_B_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    productPriceLines.put(PriceListTestConstants.USA_GOOD_C_PRODUCT_NAME,
        new String[] { "11.14", "11.24" });
    setExpectedProductPrices(productPriceLines);

    // Price List Header
    setOrganizationId(PriceListTestConstants.SPAIN_ORGANIZATION_ID);
    setPriceListName(PriceListTestConstants.PRICE_LIST_NAME);
    setCurrencyId(PriceListTestConstants.EUR_CURRENCY_ID);
    setSalesPrice(true);
    setBasedOnCost(true);
    setPriceIncludesTax(false);
  }

}
