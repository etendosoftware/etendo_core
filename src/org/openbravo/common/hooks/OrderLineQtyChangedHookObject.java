package org.openbravo.common.hooks;

import java.math.BigDecimal;

import org.openbravo.model.pricing.pricelist.PriceList;

public class OrderLineQtyChangedHookObject {

  private String productId;
  private String orderId;
  private BigDecimal qty;
  private BigDecimal price;
  private String changed;
  private int pricePrecision;
  private PriceList priceList;
  private BigDecimal listPrice;

  public int getPricePrecision() {
    return pricePrecision;
  }

  public void setPricePrecision(int pricePrecision) {
    this.pricePrecision = pricePrecision;
  }

  public String getChanged() {
    return changed;
  }

  public void setChanged(String changed) {
    this.changed = changed;
  }

  public String getOrderId() {
    return orderId;
  }

  public void setOrderId(String orderId) {
    this.orderId = orderId;
  }

  public void setProductId(String productId) {
    this.productId = productId;
  }

  public String getProductId() {
    return productId;
  }

  public void setProduct(String productid) {
    this.productId = productid;
  }

  public BigDecimal getQty() {
    return qty;
  }

  public void setQty(BigDecimal qty) {
    this.qty = qty;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public void setPrice(BigDecimal price) {
    this.price = price;
  }

  public PriceList getPriceList() {
    return priceList;
  }

  public void setPriceList(PriceList priceList) {
    this.priceList = priceList;
  }

  public BigDecimal getListPrice() {
    return listPrice;
  }

  public void setListPrice(BigDecimal listPrice) {
    this.listPrice = listPrice;
  }

}
