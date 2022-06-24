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

package org.openbravo.test.services;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.services.data.ServiceTestData;
import org.openbravo.test.services.data.ServiceTestData7;
import org.openbravo.test.services.data.ServiceTestData8;

/**
 * Tests cases to check modifications on quantities of a sales order lines with related service
 * lines
 * 
 */
public class ServicesTest2 extends WeldBaseTest {
  final static private Logger log = LogManager.getLogger();
  // User Openbravo
  private final String USER_ID = "100";
  // Client QA Testing
  private final String CLIENT_ID = "4028E6C72959682B01295A070852010D";
  // Organization Spain
  private final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Role QA Testing Admin
  private final String ROLE_ID = "4028E6C72959682B01295A071429011E";
  // Sales order: 50012
  private final String SALESORDER_ID = "3C7982B8D15D4650B6BFC32A5200DBB4";
  // Tax Exempt
  private static final String TAX_EXEMPT = "BA7059430C0A43A9B86A21C4EECF3A21";

  private boolean isPriceIncludingTaxes;

  public ServicesTest2() {
  }

  public static final List<ServiceTestData> PARAMS = Arrays.asList(new ServiceTestData7(),
      new ServiceTestData8());

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<ServiceTestData> parameterValuesRule = new ParameterCdiTestRule<ServiceTestData>(
      PARAMS);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest ServiceTestData parameter;

  /**
   * Tests cases to check modifications on quantities of a sales order lines with related service
   * lines. Creates and order, it adds a product line and one or several service lines. It all
   * service lines to the product line. After it, it changes the ordered quantity of the product
   * line. All related service lines have to be updated.
   */
  @Test
  public void ServiceTest() {
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    String testOrderId = null;
    try {
      Order order;
      order = OBDal.getInstance().get(Order.class, SALESORDER_ID);
      Order testOrder = (Order) DalUtil.copy(order, false);
      testOrderId = testOrder.getId();
      testOrder.setDocumentNo("Service Test " + parameter.getTestNumber());
      testOrder.setBusinessPartner(
          OBDal.getInstance().get(BusinessPartner.class, parameter.getBpartnerId()));
      PriceList priceList = OBDal.getInstance().get(PriceList.class, parameter.getPricelistId());
      testOrder.setPriceList(priceList);
      isPriceIncludingTaxes = priceList.isPriceIncludesTax();
      testOrder.setPriceIncludesTax(isPriceIncludingTaxes);
      testOrder.setSummedLineAmount(BigDecimal.ZERO);
      testOrder.setGrandTotalAmount(BigDecimal.ZERO);
      testOrder.setId(SequenceIdData.getUUID());
      testOrder.setNewOBObject(true);
      OBDal.getInstance().save(testOrder);
      OBDal.getInstance().flush();
      log.debug("Order Created:" + testOrder.getDocumentNo());
      log.debug(parameter.getTestDescription());
      OBDal.getInstance().refresh(testOrder);
      testOrderId = testOrder.getId();
      final List<String> serviceLines = new ArrayList<String>();
      // Insert Product Line
      OrderLine productOrderLine = insertLine(order, testOrder, parameter.getProductId(),
          parameter.getQuantity(), parameter.getPrice());
      for (String[] service : parameter.getServices()) {
        OrderLine serviceOrderLine = insertLine(order, testOrder, service[0],
            new BigDecimal(service[1]), new BigDecimal(service[2]));
        insertRelation(serviceOrderLine, productOrderLine, productOrderLine.getOrderedQuantity(),
            productOrderLine.getLineNetAmount());
        OBDal.getInstance().flush();
        OBDal.getInstance().refresh(serviceOrderLine);
      }

      productOrderLine.setOrderedQuantity(parameter.getProductChangedQty());

      OBDal.getInstance().save(productOrderLine);
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);

      for (String serviceLineId : serviceLines) {
        OrderLine serviceLine = OBDal.getInstance().get(OrderLine.class, serviceLineId);
        for (String[] service : parameter.getServicesResults()) {
          if (serviceLine.getProduct().getId().equals(service[0])) {
            if (isPriceIncludingTaxes) {
              assertThat("Wrong Service Gross Price", serviceLine.getGrossUnitPrice(),
                  closeTo(new BigDecimal(service[2]), BigDecimal.ZERO));
              assertThat("Wrong Line Gross amount for service", serviceLine.getLineGrossAmount(),
                  closeTo(new BigDecimal(service[3]), BigDecimal.ZERO));
            } else {
              assertThat("Wrong Service Price", serviceLine.getUnitPrice(),
                  closeTo(new BigDecimal(service[2]), BigDecimal.ZERO));
              assertThat("Wrong Line Net amount for service", serviceLine.getLineNetAmount(),
                  closeTo(new BigDecimal(service[3]), BigDecimal.ZERO));
            }
            assertThat("Wrong Quantity for Service", serviceLine.getOrderedQuantity(),
                closeTo(new BigDecimal(service[1]), BigDecimal.ZERO));
            break;
          }
        }
      }

    } catch (Exception e) {
      log.error("Error when executing: " + parameter.getTestDescription(), e);
      assertFalse(true);
    } finally {
      if (testOrderId != null) {
        System.out.println(testOrderId);
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
        // OBDal.getInstance().remove(OBDal.getInstance().get(Order.class, testOrderId));
        OBDal.getInstance().flush();
      }
    }
  }

  private OrderLine insertLine(Order sampleOrder, Order testOrder, String productId,
      BigDecimal _quantity, BigDecimal _price) {
    OrderLine orderLine = sampleOrder.getOrderLineList().get(0);
    OrderLine testOrderLine = (OrderLine) DalUtil.copy(orderLine, false);
    Product product = OBDal.getInstance().get(Product.class, productId);
    testOrderLine.setProduct(product);
    testOrderLine.setUOM(product.getUOM());
    testOrderLine.setOrderedQuantity(_quantity);
    testOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
    if (isPriceIncludingTaxes) {
      testOrderLine.setUnitPrice(BigDecimal.ZERO);
      testOrderLine.setListPrice(BigDecimal.ZERO);
      testOrderLine.setStandardPrice(BigDecimal.ZERO);
      testOrderLine.setGrossListPrice(_price);
      testOrderLine.setGrossUnitPrice(_price);
      testOrderLine.setLineNetAmount(BigDecimal.ZERO);
      testOrderLine.setLineGrossAmount(_price.multiply(_quantity));
      testOrderLine.setTaxableAmount(_price.multiply(_quantity));
    } else {
      testOrderLine.setUnitPrice(_price);
      testOrderLine.setListPrice(_price);
      testOrderLine.setGrossListPrice(BigDecimal.ZERO);
      testOrderLine.setGrossUnitPrice(BigDecimal.ZERO);
      testOrderLine.setStandardPrice(_price);
      testOrderLine.setLineNetAmount(_price.multiply(_quantity));
      testOrderLine.setLineGrossAmount(BigDecimal.ZERO);
      testOrderLine.setTaxableAmount(_price.multiply(_quantity));
    }
    testOrderLine.setTax(OBDal.getInstance().get(TaxRate.class, TAX_EXEMPT));
    if (parameter.getBpartnerId() != null) {
      testOrderLine.setBusinessPartner(
          OBDal.getInstance().get(BusinessPartner.class, parameter.getBpartnerId()));
    }
    testOrderLine.setSalesOrder(testOrder);
    testOrder.getOrderLineList().add(testOrderLine);
    testOrderLine.setId(SequenceIdData.getUUID());
    testOrderLine.setNewOBObject(true);
    OBDal.getInstance().save(testOrderLine);
    OBDal.getInstance().save(testOrder);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(testOrder);
    OBDal.getInstance().refresh(testOrderLine);
    return testOrderLine;
  }

  private void insertRelation(OrderLine serviceOrderLine, OrderLine orderLine, BigDecimal quantity,
      BigDecimal amount) {
    OrderlineServiceRelation osr = OBProvider.getInstance().get(OrderlineServiceRelation.class);
    osr.setAmount(amount);
    osr.setQuantity(quantity);
    osr.setOrderlineRelated(orderLine);
    osr.setSalesOrderLine(serviceOrderLine);
    OBDal.getInstance().save(osr);
  }
}
