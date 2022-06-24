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

import java.math.BigDecimal;

public class OrderLineData {
  private long lineNo;
  private String businessPartnerId;
  private String organizationId;
  private String productId;
  private String attributeSetInstanceId;
  private String uomId;
  private BigDecimal orderedQuantity;
  private BigDecimal price;
  private String taxId;
  private String warehouseId;
  private String referencePOSODocumentNo;
  private String description;
  private String businessPartnerLocationId;
  private String operativeUOMId;
  private BigDecimal operativeQuantity;

  public OrderLineData() {
  }

  public OrderLineData(long lineNo, String businessPartnerId, String organizationId,
      String productId, BigDecimal orderedQuantity, BigDecimal price, String taxId,
      String warehouseId, String attributeSetInstanceId, String operativeUOMId,
      BigDecimal operativeQuantity) {
    super();
    this.lineNo = lineNo;
    this.businessPartnerId = businessPartnerId;
    this.organizationId = organizationId;
    this.productId = productId;
    this.orderedQuantity = orderedQuantity;
    this.price = price;
    this.taxId = taxId;
    this.warehouseId = warehouseId;
    this.attributeSetInstanceId = attributeSetInstanceId;
  }

  public Long getLineNo() {
    return lineNo;
  }

  public void setLineNo(Long lineNo) {
    this.lineNo = lineNo;
  }

  public String getOrganizationId() {
    return organizationId;
  }

  public void setOrganizationId(String organizationId) {
    this.organizationId = organizationId;
  }

  public String getUomId() {
    return uomId;
  }

  public void setUomId(String uomId) {
    this.uomId = uomId;
  }

  public void setLineNo(long lineNo) {
    this.lineNo = lineNo;
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

  public String getProductId() {
    return productId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public BigDecimal getOrderedQuantity() {
    return orderedQuantity;
  }

  public void setOrderedQuantity(BigDecimal orderedQuantity) {
    this.orderedQuantity = orderedQuantity;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public String getTaxId() {
    return taxId;
  }

  public void setTaxId(String taxId) {
    this.taxId = taxId;
  }

  public String getWarehouseId() {
    return warehouseId;
  }

  public void setWarehouseId(String warehouseId) {
    this.warehouseId = warehouseId;
  }

  public String getReferencePOSODocumentNo() {
    return referencePOSODocumentNo;
  }

  public void setReferencePOSODocumentNo(String referencePOSODocumentNo) {
    this.referencePOSODocumentNo = referencePOSODocumentNo;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAttributeSetInstanceId() {
    return attributeSetInstanceId;
  }

  public void setAttributeSetInstanceId(String attributeSetInstanceId) {
    this.attributeSetInstanceId = attributeSetInstanceId;
  }

  public String getOperativeUOMId() {
    return operativeUOMId;
  }

  public void setOperativeUOMId(String operativeUOMId) {
    this.operativeUOMId = operativeUOMId;
  }

  public BigDecimal getOperativeQuantity() {
    return operativeQuantity;
  }

  public void setOperativeQuantity(BigDecimal operativeQuantity) {
    this.operativeQuantity = operativeQuantity;
  }

}
