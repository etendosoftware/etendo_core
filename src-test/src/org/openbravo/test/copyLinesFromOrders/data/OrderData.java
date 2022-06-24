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

package org.openbravo.test.copyLinesFromOrders.data;

public class OrderData {
  private boolean isSales;
  private boolean isPriceIncludingTaxes;
  private String documentNo;
  private String documentTypeId;
  private String businessPartnerId;
  private String businessPartnerLocationId;
  private String priceListId;
  private String organizationId;
  private String paymentMethodId;
  private String paymentTermsId;
  private String warehouseId;
  private int deliveryDaysCountFromNow;
  private String description;

  public OrderData() {

  }

  public boolean isSales() {
    return isSales;
  }

  public void setSales(boolean isSales) {
    this.isSales = isSales;
  }

  public boolean isPriceIncludingTaxes() {
    return isPriceIncludingTaxes;
  }

  public void setPriceIncludingTaxes(boolean isPriceIncludingTaxes) {
    this.isPriceIncludingTaxes = isPriceIncludingTaxes;
  }

  public String getDocumentNo() {
    return documentNo;
  }

  public void setDocumentNo(String documentNo) {
    this.documentNo = documentNo;
  }

  public String getDocumentTypeId() {
    return documentTypeId;
  }

  public void setDocumentTypeId(String documentTypeId) {
    this.documentTypeId = documentTypeId;
  }

  public String getBusinessPartnerId() {
    return businessPartnerId;
  }

  public void setBusinessPartnerId(String businessPartnerId) {
    this.businessPartnerId = businessPartnerId;
  }

  public String getBusinessPartnerLocationId() {
    return businessPartnerLocationId;
  }

  public void setBusinessPartnerLocationId(String businessPartnerLocationId) {
    this.businessPartnerLocationId = businessPartnerLocationId;
  }

  public String getPriceListId() {
    return priceListId;
  }

  public void setPriceListId(String priceListId) {
    this.priceListId = priceListId;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getPaymentMethodId() {
    return paymentMethodId;
  }

  public void setPaymentMethodId(String paymentMethodId) {
    this.paymentMethodId = paymentMethodId;
  }

  public String getPaymentTermsId() {
    return paymentTermsId;
  }

  public void setPaymentTermsId(String paymentTermsId) {
    this.paymentTermsId = paymentTermsId;
  }

  public String getWarehouseId() {
    return warehouseId;
  }

  public void setWarehouseId(String warehouseId) {
    this.warehouseId = warehouseId;
  }

  public int getDeliveryDaysCountFromNow() {
    return deliveryDaysCountFromNow;
  }

  public void setDeliveryDaysCountFromNow(int deliveryDaysCountFromNow) {
    this.deliveryDaysCountFromNow = deliveryDaysCountFromNow;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

}
