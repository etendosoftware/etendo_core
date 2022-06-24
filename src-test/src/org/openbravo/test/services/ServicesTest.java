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
 * All portions are Copyright (C) 2015-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.services;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderlineServiceRelation;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.test.services.data.ServiceTestData;
import org.openbravo.test.services.data.ServiceTestData1;
import org.openbravo.test.services.data.ServiceTestData2;
import org.openbravo.test.services.data.ServiceTestData3;
import org.openbravo.test.services.data.ServiceTestData4;
import org.openbravo.test.services.data.ServiceTestData5;
import org.openbravo.test.services.data.ServiceTestData6;

/**
 * Tests cases to check service Price computation
 * 
 * 
 */
public class ServicesTest extends WeldBaseTest {
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

  public ServicesTest() {
  }

  public static final List<ServiceTestData> PARAMS = Arrays.asList(new ServiceTestData1(),
      new ServiceTestData2(), new ServiceTestData3(), new ServiceTestData4(),
      new ServiceTestData5(), new ServiceTestData6());

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<ServiceTestData> parameterValuesRule = new ParameterCdiTestRule<ServiceTestData>(
      PARAMS);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest ServiceTestData parameter;

  /**
   * Verifies price computation for services. Add a relation line, update it and delete it. Review
   * price computation for the service is correct
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
      // Insert Service Line
      OrderLine serviceOrderLine = insertLine(order, testOrder, parameter.getServiceId(),
          parameter.getQuantity(), parameter.getPrice());
      for (String[] product : parameter.getProducts()) {
        OrderLine orderLine = insertLine(order, testOrder, product[0], new BigDecimal(product[1]),
            new BigDecimal(product[2]));
        insertRelation(serviceOrderLine, orderLine, new BigDecimal(product[1]),
            new BigDecimal(product[3]));
      }
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      OBDal.getInstance().refresh(serviceOrderLine);

      if (isPriceIncludingTaxes) {
        assertThat("Wrong Service Gross Price", serviceOrderLine.getGrossUnitPrice(),
            closeTo(parameter.getServicePriceResult(), BigDecimal.ZERO));
        assertThat("Wrong Line Gross amount for service", serviceOrderLine.getLineGrossAmount(),
            closeTo(parameter.getServiceAmountResult(), BigDecimal.ZERO));
      } else {
        assertThat("Wrong Service Price", serviceOrderLine.getUnitPrice(),
            closeTo(parameter.getServicePriceResult(), BigDecimal.ZERO));
        assertThat("Wrong Line Net amount for service", serviceOrderLine.getLineNetAmount(),
            closeTo(parameter.getServiceAmountResult(), BigDecimal.ZERO));
      }
      assertThat("Wrong Quantity for Service", serviceOrderLine.getOrderedQuantity(),
          closeTo(parameter.getServiceQtyResult(), BigDecimal.ZERO));
      updateServiceRelationAmounts(serviceOrderLine, BigDecimal.ZERO, BigDecimal.ZERO);
      if (isPriceIncludingTaxes) {
        assertThat("Wrong Service Gross Price", serviceOrderLine.getGrossUnitPrice(),
            closeTo(parameter.getPrice(), BigDecimal.ZERO));
        assertThat("Wrong Line Gross amount for service", serviceOrderLine.getLineGrossAmount(),
            closeTo(parameter.getPrice().multiply(parameter.getQuantity()), BigDecimal.ZERO));
      } else {
        assertThat("Wrong Service Price", serviceOrderLine.getUnitPrice(),
            closeTo(parameter.getPrice(), BigDecimal.ZERO));
        assertThat("Wrong Line Net amount for service", serviceOrderLine.getLineNetAmount(),
            closeTo(parameter.getPrice().multiply(parameter.getQuantity()), BigDecimal.ZERO));
      }
      assertThat("Wrong Quantity for Service", serviceOrderLine.getOrderedQuantity(),
          closeTo(parameter.getQuantity(), BigDecimal.ZERO));
      OBDal.getInstance().flush();
      OBDal.getInstance().refresh(testOrder);
      OBDal.getInstance().refresh(serviceOrderLine);
      updateServiceRelationAmounts(serviceOrderLine, BigDecimal.ONE, BigDecimal.ONE);
      removeServiceRelations(serviceOrderLine);
      serviceOrderLine = OBDal.getInstance().get(OrderLine.class, serviceOrderLine.getId());
      if (isPriceIncludingTaxes) {
        assertThat("Wrong Service Price", serviceOrderLine.getGrossUnitPrice(),
            closeTo(parameter.getPrice(), BigDecimal.ZERO));
        assertThat("Wrong Line Net amount for service", serviceOrderLine.getLineGrossAmount(),
            closeTo(parameter.getPrice().multiply(parameter.getQuantity()), BigDecimal.ZERO));
      } else {
        assertThat("Wrong Service Gross Price", serviceOrderLine.getUnitPrice(),
            closeTo(parameter.getPrice(), BigDecimal.ZERO));
        assertThat("Wrong Line Gross amount for service", serviceOrderLine.getLineNetAmount(),
            closeTo(parameter.getPrice().multiply(parameter.getQuantity()), BigDecimal.ZERO));
      }
      assertThat("Wrong Quantity for Service", serviceOrderLine.getOrderedQuantity(),
          closeTo(parameter.getQuantity(), BigDecimal.ZERO));
      assertThat("Wrong Service Relations",
          new BigDecimal(
              serviceOrderLine.getOrderlineServiceRelationCOrderlineRelatedIDList().size()),
          closeTo(BigDecimal.ZERO, BigDecimal.ZERO));

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

  private void removeServiceRelations(OrderLine serviceOrderLine) {
    StringBuffer where = new StringBuffer();
    where.append(" as olsr");
    where.append(
        " where olsr." + OrderlineServiceRelation.PROPERTY_SALESORDERLINE + " = :salesorderline");
    OBQuery<OrderlineServiceRelation> olsrQry = OBDal.getInstance()
        .createQuery(OrderlineServiceRelation.class, where.toString());
    olsrQry.setNamedParameter("salesorderline", serviceOrderLine);

    ScrollableResults olsrScroller = olsrQry.scroll(ScrollMode.FORWARD_ONLY);
    while (olsrScroller.next()) {
      OrderlineServiceRelation orls = (OrderlineServiceRelation) olsrScroller.get(0);
      serviceOrderLine.getOrderlineServiceRelationList().remove(orls);
      OBDal.getInstance().remove(orls);
      OBDal.getInstance().flush();
    }
    olsrScroller.close();
  }

  private void updateServiceRelationAmounts(OrderLine serviceOrderLine, BigDecimal amount,
      BigDecimal quantity) {
    StringBuffer where = new StringBuffer();
    where.append(" as olsr");
    where.append(
        " where olsr." + OrderlineServiceRelation.PROPERTY_SALESORDERLINE + " = :salesorderline");
    OBQuery<OrderlineServiceRelation> olsrQry = OBDal.getInstance()
        .createQuery(OrderlineServiceRelation.class, where.toString());
    olsrQry.setNamedParameter("salesorderline", serviceOrderLine);

    ScrollableResults olsrScroller = olsrQry.scroll(ScrollMode.FORWARD_ONLY);
    while (olsrScroller.next()) {
      OrderlineServiceRelation orls = (OrderlineServiceRelation) olsrScroller.get(0);
      orls.setAmount(amount);
      orls.setQuantity(quantity);
      OBDal.getInstance().save(orls);
      OBDal.getInstance().flush();
    }
    olsrScroller.close();
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
