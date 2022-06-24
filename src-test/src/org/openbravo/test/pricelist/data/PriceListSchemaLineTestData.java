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

/**
 * Class to define Price List Schema Rules Data to be used in Tests
 * 
 * @author Mark
 *
 */
public class PriceListSchemaLineTestData {

  private String businessPartnerId;
  private String productCategoryId;
  private String productId;
  // Net List/Unit Price, Limit (PO) Price
  private String baseListPriceValue;
  private BigDecimal surchargeListPriceAmount;
  private BigDecimal listPriceDiscount;
  private String baseStandardPriceValue;
  private BigDecimal surchargeStandardPriceAmount;
  private BigDecimal standardPriceDiscount;
  // Cost Based
  private BigDecimal listPriceMargin;
  private BigDecimal unitPriceMargin;
  // Fixed Price Based
  private BigDecimal fixedListPrice;
  private BigDecimal fixedStandardPrice;

  public PriceListSchemaLineTestData() {

  }

  public String getBusinessPartnerId() {
    return businessPartnerId;
  }

  public void setBusinessPartnerId(String businessPartnerId) {
    this.businessPartnerId = businessPartnerId;
  }

  public String getProductCategoryId() {
    return productCategoryId;
  }

  public void setProductCategoryId(String productCategoryId) {
    this.productCategoryId = productCategoryId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getBaseListPriceValue() {
    return baseListPriceValue;
  }

  public void setBaseListPriceValue(String baseListPriceValue) {
    this.baseListPriceValue = baseListPriceValue;
  }

  public BigDecimal getSurchargeListPriceAmount() {
    return surchargeListPriceAmount;
  }

  public void setSurchargeListPriceAmount(BigDecimal surchargeListPriceAmount) {
    this.surchargeListPriceAmount = surchargeListPriceAmount;
  }

  public BigDecimal getListPriceDiscount() {
    return listPriceDiscount;
  }

  public void setListPriceDiscount(BigDecimal listPriceDiscount) {
    this.listPriceDiscount = listPriceDiscount;
  }

  public String getBaseStandardPriceValue() {
    return baseStandardPriceValue;
  }

  public void setBaseStandardPriceValue(String baseStandardPriceValue) {
    this.baseStandardPriceValue = baseStandardPriceValue;
  }

  public BigDecimal getSurchargeStandardPriceAmount() {
    return surchargeStandardPriceAmount;
  }

  public void setSurchargeStandardPriceAmount(BigDecimal surchargeStandardPriceAmount) {
    this.surchargeStandardPriceAmount = surchargeStandardPriceAmount;
  }

  public BigDecimal getStandardPriceDiscount() {
    return standardPriceDiscount;
  }

  public void setStandardPriceDiscount(BigDecimal standardPriceDiscount) {
    this.standardPriceDiscount = standardPriceDiscount;
  }

  public BigDecimal getListPriceMargin() {
    return listPriceMargin;
  }

  public void setListPriceMargin(BigDecimal listPriceMargin) {
    this.listPriceMargin = listPriceMargin;
  }

  public BigDecimal getUnitPriceMargin() {
    return unitPriceMargin;
  }

  public void setUnitPriceMargin(BigDecimal unitPriceMargin) {
    this.unitPriceMargin = unitPriceMargin;
  }

  public BigDecimal getFixedListPrice() {
    return fixedListPrice;
  }

  public void setFixedListPrice(BigDecimal fixedListPrice) {
    this.fixedListPrice = fixedListPrice;
  }

  public BigDecimal getFixedStandardPrice() {
    return fixedStandardPrice;
  }

  public void setFixedStandardPrice(BigDecimal fixedStandardPrice) {
    this.fixedStandardPrice = fixedStandardPrice;
  }

}
