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
 * All portions are Copyright (C) 2017-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.pricelist.data;

import java.util.HashMap;

public abstract class PriceListTestData {

  /**
   * This array will contain all the rules to be applied when the Product Price List be generated.
   */
  private PriceListSchemaLineTestData[] testPriceListRulesData;

  /**
   * This Map will be used to verify Product Prices values after test is executed. Map has the
   * following structure: <Product name, [Unit Price Expected, List Price Expected]>
   */
  private HashMap<String, String[]> expectedProductPrices;

  // Price List Header
  private String organizationId;
  private String priceListName;
  private String currencyId;
  private boolean isSalesPrice;
  private boolean isBasedOnCost;
  private boolean isPriceIncludesTax;
  private boolean isDefault;

  // Price List Version
  private String basePriceListVersionId;

  public PriceListTestData() {
    initialize();
  }

  public PriceListSchemaLineTestData[] getTestPriceListRulesData() {
    return testPriceListRulesData;
  }

  public void setTestPriceListRulesData(PriceListSchemaLineTestData[] testPriceListRulesData) {
    this.testPriceListRulesData = testPriceListRulesData;
  }

  public abstract void initialize();

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getPriceListName() {
    return priceListName;
  }

  public void setPriceListName(String priceListName) {
    this.priceListName = priceListName;
  }

  public String getCurrencyId() {
    return currencyId;
  }

  public void setCurrencyId(String currencyId) {
    this.currencyId = currencyId;
  }

  public boolean isSalesPrice() {
    return isSalesPrice;
  }

  public void setSalesPrice(boolean isSalesPrice) {
    this.isSalesPrice = isSalesPrice;
  }

  public boolean isBasedOnCost() {
    return isBasedOnCost;
  }

  public void setBasedOnCost(boolean isBasedOnCost) {
    this.isBasedOnCost = isBasedOnCost;
  }

  public boolean isPriceIncludesTax() {
    return isPriceIncludesTax;
  }

  public void setPriceIncludesTax(boolean isPriceIncludesTax) {
    this.isPriceIncludesTax = isPriceIncludesTax;
  }

  public String getBasePriceListVersionId() {
    return basePriceListVersionId;
  }

  public void setBasePriceListVersionId(String basePriceListVersionId) {
    this.basePriceListVersionId = basePriceListVersionId;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public void setDefault(boolean isDefault) {
    this.isDefault = isDefault;
  }

  /**
   * Returns Map should be used to verify Product Prices values after test is executed. Map has the
   * following structure: &lt;Product name, [Unit Price Expected, List Price Expected]&gt;
   */
  public HashMap<String, String[]> getExpectedProductPrices() {
    return expectedProductPrices;
  }

  public void setExpectedProductPrices(HashMap<String, String[]> expectedProductPrices) {
    this.expectedProductPrices = expectedProductPrices;
  }
}
