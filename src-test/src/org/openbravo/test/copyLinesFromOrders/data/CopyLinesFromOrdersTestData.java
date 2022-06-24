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

package org.openbravo.test.copyLinesFromOrders.data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductAUM;
import org.openbravo.model.common.uom.UOM;

public abstract class CopyLinesFromOrdersTestData {
  public abstract String getTestNumber();

  public abstract String getTestDescription();

  public abstract boolean isExecuteAsQAAdmin();

  /**
   * This array will contain all the orders to be copied.
   */
  private OrderData[] ordersCopiedFrom;

  /**
   * This array will contain all the lines from each order to be copied.
   */
  private OrderLineData[][] orderLinesCopiedFrom;

  /**
   * This will contain the order data that will be processed to be copied.
   */
  private OrderData order;

  /**
   * This array will be used to verify the Order Header values after the process is executed. Has
   * the following structure: [Total Net Amount expected, Total Gross maount expected]
   */
  private String[] expectedOrderAmounts;

  /**
   * This Map will be used to verify the Order Lines values after the process is executed. Map has
   * the following structure: <Line No, [Product Name, Ordered Qty, UOM, Net Unit Price Expected,
   * List Price Expected, Gross Unit Price, Tax, Reference to PO/SO DocumentNo From it was created,
   * BP Address, Organization, Attribute Value]>
   */
  private HashMap<String, String[]> expectedOrderLines;

  public CopyLinesFromOrdersTestData() {
    initialize();
  }

  public abstract void initialize();

  public void applyTestSettings() {

  };

  public void applyTestSettingsBeforeExecuteProcess() {

  };

  public String[] getExpectedOrderHeader() {
    return expectedOrderAmounts;
  }

  public void setExpectedOrderHeader(String[] expectedOrderHeader) {
    this.expectedOrderAmounts = expectedOrderHeader;
  }

  public HashMap<String, String[]> getExpectedOrderLines() {
    return expectedOrderLines;
  }

  public void setExpectedOrderLines(HashMap<String, String[]> expectedOrderLines) {
    this.expectedOrderLines = expectedOrderLines;
  }

  public OrderData[] getOrdersCopiedFrom() {
    return ordersCopiedFrom;
  }

  public void setOrdersCopiedFrom(OrderData[] ordersCopiedFrom) {
    this.ordersCopiedFrom = ordersCopiedFrom;
  }

  public OrderLineData[][] getOrderLinesCopiedFrom() {
    return orderLinesCopiedFrom;
  }

  public void setOrderLinesCopiedFrom(OrderLineData[][] orderLinesCopiedFrom) {
    this.orderLinesCopiedFrom = orderLinesCopiedFrom;
  }

  public OrderData getOrder() {
    return order;
  }

  public void setOrder(OrderData order) {
    this.order = order;
  }

  public String[] getExpectedOrderAmounts() {
    return expectedOrderAmounts;
  }

  public void setExpectedOrderAmounts(String[] expectedOrderAmounts) {
    this.expectedOrderAmounts = expectedOrderAmounts;
  }

  // Generic methods to Performance Tests
  public void generateDataForPerformanceTest(int orderCount, int linesCountPerOrder) {
    setExpectedOrderLines(new HashMap<String, String[]>());
    createOrderHeaderOrderLinesAndExpectedCopiedLines(orderCount, linesCountPerOrder);

    // Information of the order that will be processed
    OrderData orderToBeProcessed = createOrderHeader(0);
    setOrder(orderToBeProcessed);
  }

  public void createOrderHeaderOrderLinesAndExpectedCopiedLines(int orderCount,
      int linesCountPerOrder) {
    OrderData[] ordersCopied = new OrderData[orderCount];
    OrderLineData[][] linesCopied = new OrderLineData[orderCount][linesCountPerOrder];
    int expectedLine = 0;
    for (int orderReference = 0; orderReference < orderCount; orderReference++) {
      ordersCopied[orderReference] = createOrderHeader(orderReference + 1);
      for (int orderLineReference = 0; orderLineReference < linesCountPerOrder; orderLineReference++) {
        linesCopied[orderReference][orderLineReference] = createOrderLine(
            Long.valueOf(orderLineReference));
        createExpectedLine(String.valueOf(orderReference + 1), String.valueOf(expectedLine += 10));
      }
    }
    // Set orders
    setOrdersCopiedFrom(ordersCopied);
    // Set lines to those orders
    setOrderLinesCopiedFrom(linesCopied);
    // Set expected lines
    setExpectedOrderLines(expectedOrderLines);
    // Set expected amounts
    calculateExpectedOrderAmounts(orderCount, linesCountPerOrder);
  }

  private void calculateExpectedOrderAmounts(int orderCount, int linesCountPerOrder) {
    int totalLines = orderCount * linesCountPerOrder;
    double qty = 10;
    double price = 2.00;
    // TAX: VAT3+CHARGE0.5
    double tax = 0.03;
    double charge = 0.005;
    double expectedTotalNetAmount = qty * price * totalLines;
    double expectedTotalGrossAmount = expectedTotalNetAmount + expectedTotalNetAmount * tax
        + expectedTotalNetAmount * charge;
    setExpectedOrderAmounts(new String[] { String.valueOf(expectedTotalNetAmount),
        String.valueOf(expectedTotalGrossAmount) });
  }

  public void createExpectedLine(String orderReference, String lineNo) {
    expectedOrderLines.put(lineNo,
        new String[] { CLFOTestConstants.FINAL_GOOD_C_PRODUCT_NAME, "10",
            CLFOTestConstants.BAG_UOM_NAME, "2.00", "2.00", "0",
            CLFOTestConstants.VAT3_CHARGE05_TAX_NAME, "Order_" + orderReference,
            BPartnerDataConstants.CUSTOMER_A_LOCATION, CLFOTestConstants.SPAIN_ORGANIZATION_NAME,
            "", null, null, CLFOTestConstants.LINE1_DESCRIPTION });
  }

  public OrderLineData createOrderLine(Long lineNo) {
    OrderLineData order1Line1 = new OrderLineData();
    order1Line1.setLineNo(lineNo);
    order1Line1.setBusinessPartnerId(BPartnerDataConstants.CUSTOMER_A);
    order1Line1.setBusinessPartnerLocationId(BPartnerDataConstants.CUSTOMER_A_LOCATION_ID);
    order1Line1.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    order1Line1.setProductId(CLFOTestConstants.FINAL_GOOD_C_PRODUCT_ID);
    order1Line1.setUomId(CLFOTestConstants.BAG_UOM_ID);
    order1Line1.setOrderedQuantity(new BigDecimal("10"));
    order1Line1.setPrice(new BigDecimal("2"));
    order1Line1.setTaxId(CLFOTestConstants.VAT3_CHARGE05_TAX_ID);
    order1Line1.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    order1Line1.setDescription(CLFOTestConstants.LINE1_DESCRIPTION);
    return order1Line1;
  }

  public OrderData createOrderHeader(int i) {
    OrderData orderFrom1 = new OrderData();
    orderFrom1.setSales(true);
    orderFrom1.setPriceIncludingTaxes(false);
    orderFrom1.setDocumentNo("Order_" + i);
    orderFrom1.setDocumentTypeId(CLFOTestConstants.DOCUMENT_TYPE_STANDARD);
    orderFrom1.setBusinessPartnerId(BPartnerDataConstants.CUSTOMER_A);
    orderFrom1.setBusinessPartnerLocationId(BPartnerDataConstants.CUSTOMER_A_LOCATION_ID);
    orderFrom1.setPriceListId(CLFOTestConstants.CUSTOMER_A_PRICE_LIST_ID);
    orderFrom1.setOrganizationId(CLFOTestConstants.SPAIN_ORGANIZATION_ID);
    orderFrom1.setPaymentMethodId(CLFOTestConstants.PAYMENT_METHOD_1_SPAIN);
    orderFrom1.setPaymentTermsId(CLFOTestConstants.PAYMENT_TERMS_30_5_DAYS);
    orderFrom1.setWarehouseId(CLFOTestConstants.SPAIN_EAST_WAREHOUSE);
    orderFrom1.setDeliveryDaysCountFromNow(0);
    orderFrom1.setDescription("");
    return orderFrom1;
  }

  public void createAUMForProduct(String productId, String aumId, String conversionRate,
      String sales, String purchase, String logistics) {
    Product product = OBDal.getInstance().get(Product.class, productId);
    if (!existsAUMDefinedForProduct(aumId, product)) {
      createProductAUM(aumId, conversionRate, sales, purchase, logistics, product);
    }
  }

  private ProductAUM createProductAUM(String aumId, String conversionRate, String sales,
      String purchase, String logistics, Product productFGA) {
    ProductAUM productAUM = OBProvider.getInstance().get(ProductAUM.class);
    productAUM.setConversionRate(new BigDecimal(conversionRate));
    productAUM.setLogistics(logistics);
    productAUM.setProduct(productFGA);
    productAUM.setPurchase(purchase);
    productAUM.setSales(sales);
    productAUM.setUOM(OBDal.getInstance().getProxy(UOM.class, aumId));
    productAUM.setOrganization(
        OBDal.getInstance().getProxy(Organization.class, CLFOTestConstants.SPAIN_ORGANIZATION_ID));
    productFGA.getProductAUMList().add(productAUM);
    OBDal.getInstance().save(productAUM);
    OBDal.getInstance().flush();
    return productAUM;
  }

  private boolean existsAUMDefinedForProduct(String aumId, Product productFGA) {
    List<ProductAUM> productAUMs = productFGA.getProductAUMList();
    for (ProductAUM productAUM : productAUMs) {
      if (StringUtils.equals(productAUM.getUOM().getId(), aumId)) {
        return true;
      }
    }
    return false;
  }
}
