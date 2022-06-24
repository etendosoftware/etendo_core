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

package org.openbravo.test.materialMgmt.iscompletelyinvoicedshipment;

import java.math.BigDecimal;
import java.util.Date;

import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.tax.TaxRate;

public class ICIOrderLineParameters {

  private Order order;
  private Client client;
  private Organization organization;
  private Date orderDate;
  private Currency currency;
  private Warehouse warehouse;
  private Product product;
  private UOM uom;
  private BigDecimal orderedQuantity;
  private BigDecimal netUnitPrice;
  private BigDecimal netListPrice;
  private BigDecimal priceLimit;
  private BigDecimal lineNetAmount;
  private TaxRate taxRate;
  private Long lineNo;

  public ICIOrderLineParameters(Order orderParam) {
    this.order = orderParam;
    this.client = orderParam.getClient();
    this.organization = orderParam.getOrganization();
    this.orderDate = orderParam.getOrderDate();
    this.currency = orderParam.getCurrency();
    this.warehouse = orderParam.getWarehouse();
    Product productParam = OBDal.getInstance().get(Product.class, ICIConstants.PRODUCT_ID);
    this.product = productParam;
    this.uom = productParam.getUOM();
    this.orderedQuantity = new BigDecimal("10");
    this.netUnitPrice = new BigDecimal("10");
    this.netListPrice = new BigDecimal("10");
    this.lineNetAmount = new BigDecimal("100");
    this.taxRate = OBDal.getInstance().get(TaxRate.class, ICIConstants.TAX_ID);
    this.lineNo = 10L;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Client getClient() {
    return client;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public Organization getOrganization() {
    return organization;
  }

  public void setOrganization(Organization organization) {
    this.organization = organization;
  }

  public Date getOrderDate() {
    return orderDate;
  }

  public void setOrderDate(Date orderDate) {
    this.orderDate = orderDate;
  }

  public Currency getCurrency() {
    return currency;
  }

  public void setCurrency(Currency currency) {
    this.currency = currency;
  }

  public Warehouse getWarehouse() {
    return warehouse;
  }

  public void setWarehouse(Warehouse warehouse) {
    this.warehouse = warehouse;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public UOM getUom() {
    return uom;
  }

  public void setUom(UOM uom) {
    this.uom = uom;
  }

  public BigDecimal getOrderedQuantity() {
    return orderedQuantity;
  }

  public void setOrderedQuantity(BigDecimal orderedQuantity) {
    this.orderedQuantity = orderedQuantity;
  }

  public BigDecimal getNetUnitPrice() {
    return netUnitPrice;
  }

  public void setNetUnitPrice(BigDecimal netUnitPrice) {
    this.netUnitPrice = netUnitPrice;
  }

  public BigDecimal getNetListPrice() {
    return netListPrice;
  }

  public void setNetListPrice(BigDecimal netListPrice) {
    this.netListPrice = netListPrice;
  }

  public BigDecimal getPriceLimit() {
    return priceLimit;
  }

  public void setPriceLimit(BigDecimal priceLimit) {
    this.priceLimit = priceLimit;
  }

  public BigDecimal getLineNetAmount() {
    return lineNetAmount;
  }

  public void setLineNetAmount(BigDecimal lineNetAmount) {
    this.lineNetAmount = lineNetAmount;
  }

  public TaxRate getTaxRate() {
    return taxRate;
  }

  public void setTaxRate(TaxRate taxRate) {
    this.taxRate = taxRate;
  }

  public Long getLineNo() {
    return lineNo;
  }

  public void setLineNo(Long lineNo) {
    this.lineNo = lineNo;
  }

}
