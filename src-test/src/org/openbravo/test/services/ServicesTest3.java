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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.ParameterCdiTest;
import org.openbravo.base.weld.test.ParameterCdiTestRule;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ServicePriceRuleRange;
import org.openbravo.test.services.data.ServiceTestData;
import org.openbravo.test.services.data.ServiceTestData10;
import org.openbravo.test.services.data.ServiceTestData11;
import org.openbravo.test.services.data.ServiceTestData9;

/**
 * Tests cases to check ServicePriceUtils.getServiceAmount method. All possible errors are properly
 * handled.
 * 
 */
public class ServicesTest3 extends WeldBaseTest {
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
  // Amount Up To Blank Range of "Range" Service Price Rule
  private static final String SERVICEPRICERULE_RANGE_UP_TO_BLANK = "6B485CFB092A48919C6B4459F8ED45BA";

  private boolean isPriceIncludingTaxes;

  public ServicesTest3() {
  }

  public static final List<ServiceTestData> PARAMS = Arrays.asList(new ServiceTestData9(),
      new ServiceTestData10(), new ServiceTestData11());

  /** defines the values the parameter will take. */
  @Rule
  public ParameterCdiTestRule<ServiceTestData> parameterValuesRule = new ParameterCdiTestRule<ServiceTestData>(
      PARAMS);

  /** this field will take the values defined by parameterValuesRule field. */
  private @ParameterCdiTest ServiceTestData parameter;

  /**
   * Tests cases to check ServicePriceUtils.getServiceAmount method. All possible errors are
   * properly handled. Creates and order, it adds a product line and one service line. Then, it
   * executes ServicePriceUtils .getServiceAmount method preparing scenarios on which certain type
   * of error is obtained.
   */
  @Test
  public void ServiceTest3() {
    // Set QA context
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);
    String testOrderId = null;
    try {
      if (parameter.getTestNumber().equals("BACK-501")) {
        ServicePriceRuleRange range = OBDal.getInstance()
            .get(ServicePriceRuleRange.class, SERVICEPRICERULE_RANGE_UP_TO_BLANK);
        range.setActive(false);
      }
      Order order;
      order = OBDal.getInstance().get(Order.class, SALESORDER_ID);
      Order testOrder = (Order) DalUtil.copy(order, false);
      testOrderId = testOrder.getId();
      testOrder.setDocumentNo("Service Test " + parameter.getTestNumber());
      testOrder.setBusinessPartner(
          OBDal.getInstance().get(BusinessPartner.class, parameter.getBpartnerId()));
      testOrder.setOrderDate(OBDateUtils.getDate(parameter.getOrderDate()));
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

      boolean error = false;

      // Insert Service Line
      OrderLine serviceOrderLine = insertLine(order, testOrder, parameter.getServiceId(),
          parameter.getQuantity(), parameter.getPrice());
      for (String[] product : parameter.getProducts()) {
        OrderLine orderLine = insertLine(order, testOrder, product[0], new BigDecimal(product[1]),
            new BigDecimal(product[2]));

        try {
          ServicePriceUtils.getServiceAmount(serviceOrderLine,
              isPriceIncludingTaxes
                  ? orderLine.getLineGrossAmount()
                      .setScale(orderLine.getCurrency().getStandardPrecision().intValue(),
                          RoundingMode.HALF_UP)
                  : orderLine.getLineNetAmount()
                      .setScale(orderLine.getCurrency().getStandardPrecision().intValue(),
                          RoundingMode.HALF_UP),
              null, null, null, null);
        } catch (OBException e) {
          assertEquals("ServicePriceUtils.getServiceAmount not properly handled error",
              e.getMessage(), parameter.getErrorMessage());
          error = true;
        }
      }

      assertTrue("Not properly handled error obtained", error);
    } catch (Exception e) {
      log.error("Error when executing: " + parameter.getTestDescription(), e);
      assertFalse(true);
    } finally {
      if (testOrderId != null) {
        System.out.println(testOrderId);
        if (parameter.getTestNumber().equals("BACK-501")) {
          ServicePriceRuleRange range = OBDal.getInstance()
              .get(ServicePriceRuleRange.class, SERVICEPRICERULE_RANGE_UP_TO_BLANK);
          range.setActive(true);
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
        OBDal.getInstance().remove(OBDal.getInstance().get(Order.class, testOrderId));
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
    testOrderLine.setOrderDate(testOrder.getOrderDate());
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
}
