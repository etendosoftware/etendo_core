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

public abstract class ServiceTestData {

  /*
   * CONSTANTS:
   */
  public final String BP_CUSTOMER_A = "4028E6C72959682B01295F40C3CB02EC";
  public final String SERVICE_WARRANTY = "D67EF9E66FF447E88176DF0C054A9D3F";
  public final String SERVICE_INSURANCE = "DE0F34D6A6F64E23BB03144CC5E0A4C0";
  public final String SERVICE_TRANSPORTATION = "73AADD53EBA94C1EAC8B472A261F02AA";
  public final String PRODUCT_DISTRIBUTION_GOOD_A = "4028E6C72959682B01295ADC211E0237";
  public final String PRODUCT_DISTRIBUTION_GOOD_B = "4028E6C72959682B01295ADC21C90239";
  public final String PRODUCT_DISTRIBUTION_GOOD_C = "4028E6C72959682B01295ADC2285023B";
  public final String PRICELIST_SALES = "4028E6C72959682B01295ADC1D55022B";
  public final String PRICELIST_CUSTOMER_A = "4028E6C72959682B01295B03CE480243";
  public final String PRICELIST_CUSTOMER_A_INCL_TAX = "6C69F63AE6C34DD48329368AFE29C91D";

  private String testNumber;
  private String testDescription;
  private String serviceId;
  private String productId;
  private String[][] services;
  private String[][] products;
  private BigDecimal quantity;
  private BigDecimal price;
  private String bpartnerId;
  private String pricelistId;
  private BigDecimal servicePriceResult;
  private BigDecimal serviceAmountResult;
  private BigDecimal serviceQtyResult;
  private String[][] servicesResults;
  private BigDecimal productChangedQty;
  private String orderDate;
  private String errorMessage;

  public BigDecimal getServiceAmountResult() {
    return serviceAmountResult;
  }

  public void setServiceAmountResult(BigDecimal serviceNetAmountResult) {
    this.serviceAmountResult = serviceNetAmountResult;
  }

  public BigDecimal getServicePriceResult() {
    return servicePriceResult;
  }

  public void setServicePriceResult(BigDecimal servicePriceResult) {
    this.servicePriceResult = servicePriceResult;
  }

  public String getPricelistId() {
    return pricelistId;
  }

  public void setPricelistId(String pricelistId) {
    this.pricelistId = pricelistId;
  }

  public String getBpartnerId() {
    return bpartnerId;
  }

  public void setBpartnerId(String bpartnerId) {
    this.bpartnerId = bpartnerId;
  }

  public ServiceTestData() {
    initialize();
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public String getServiceId() {
    return this.serviceId;
  }

  public String getProductId() {
    return this.productId;
  }

  public String[][] getServices() {
    return this.services;
  }

  public String[][] getProducts() {
    return this.products;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public void setServiceId(String serviceId) {
    this.serviceId = serviceId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public void setServices(String[][] services) {
    this.services = services;
  }

  public void setProducts(String[][] products) {
    this.products = products;
  }

  public String getTestDescription() {
    return testDescription;
  }

  public void setTestDescription(String testDescription) {
    this.testDescription = testDescription;
  }

  public String getTestNumber() {
    return testNumber;
  }

  public void setTestNumber(String testNumber) {
    this.testNumber = testNumber;
  }

  public BigDecimal getServiceQtyResult() {
    return serviceQtyResult;
  }

  public void setServiceQtyResult(BigDecimal serviceQtyResult) {
    this.serviceQtyResult = serviceQtyResult;
  }

  public String[][] getServicesResults() {
    return this.servicesResults;
  }

  public void setServicesResults(String[][] servicesResults) {
    this.servicesResults = servicesResults;
  }

  public BigDecimal getProductChangedQty() {
    return productChangedQty;
  }

  public void setProductChangedQty(BigDecimal productChangedQty) {
    this.productChangedQty = productChangedQty;
  }

  public String getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(String orderDate) {
    this.orderDate = orderDate;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public abstract void initialize();

}
