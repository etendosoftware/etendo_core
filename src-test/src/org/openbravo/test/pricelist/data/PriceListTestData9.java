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
 * Data used for Test: Price List Schema with Fixed Price or Cost plus Margin Based, an entire
 * Product Category and base Price List.
 * 
 * @author Mark
 *
 */
public class PriceListTestData9 extends PriceListTestData {

  @Override
  public void initialize() {

    // Define the rule to be applied when the price list be generated
    PriceListSchemaLineTestData ruleLine = new PriceListSchemaLineTestData();
    ruleLine.setProductCategoryId(PriceListTestConstants.DISTRIBUTION_GOODS_PRODUCT_CATEGORY);
    ruleLine.setBaseListPriceValue(
        PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN);
    ruleLine.setFixedListPrice(new BigDecimal("11.73"));
    ruleLine.setListPriceMargin(new BigDecimal("10"));
    ruleLine.setBaseStandardPriceValue(
        PriceListTestConstants.REFLIST_VALUE_FIXED_PRICE_OR_COST_PLUS_MARGIN);
    ruleLine.setFixedStandardPrice(new BigDecimal("10.65"));
    ruleLine.setUnitPriceMargin(new BigDecimal("10"));

    // Add lines
    setTestPriceListRulesData(new PriceListSchemaLineTestData[] { ruleLine });

    /**
     * This Map will be used to verify Product Prices values after test is executed. Map has the
     * following structure: <Product name, [Unit Price Expected, List Price Expected]>
     */
    HashMap<String, String[]> productPriceLines = new HashMap<String, String[]>();
    productPriceLines.put(PriceListTestConstants.LAPTOP_PRODUCT_NAME,
        new String[] { "11.00", "11.73" });
    productPriceLines.put(PriceListTestConstants.SOCCER_BALL_PRODUCT_NAME,
        new String[] { "11.00", "11.73" });
    productPriceLines.put(PriceListTestConstants.T_SHIRTS_PRODUCT_NAME,
        new String[] { "11.00", "11.73" });
    productPriceLines.put(PriceListTestConstants.TENNIS_BALL_PRODUCT_NAME,
        new String[] { "11.00", "11.73" });
    setExpectedProductPrices(productPriceLines);

    // Price List Header
    setOrganizationId(PriceListTestConstants.SPAIN_ORGANIZATION_ID);
    setPriceListName(PriceListTestConstants.PRICE_LIST_NAME);
    setCurrencyId(PriceListTestConstants.EUR_CURRENCY_ID);
    setSalesPrice(true);
    // This will be a Price list Based on Cost
    setBasedOnCost(true);
    setPriceIncludesTax(false);

    // Price List Version
    setBasePriceListVersionId(PriceListTestConstants.CUSTOMER_B_PRICE_LIST_VERSION_ID);
  }

}
