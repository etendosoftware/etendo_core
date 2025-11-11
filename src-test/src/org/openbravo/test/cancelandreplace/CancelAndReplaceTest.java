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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.cancelandreplace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.order.OrderReplacement;
import org.openbravo.test.base.TestConstants.Roles;
import org.openbravo.test.base.TestConstants.Users;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceOrderTestData;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceOrderTestData.Line;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData1;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData10;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData11;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData12;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData13;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData14;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData15;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData16;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData17;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData18;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData19;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData2;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData20;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData21;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData22;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData23;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData3;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData4;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData5;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData6;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData7;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData8;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData9;

/**
 * Tests cases to check Cancel and Replace development
 * 
 */
public class CancelAndReplaceTest extends WeldBaseTest {

  private Logger log = LogManager.getLogger();

  // Organization Spain
  private static final String ORGANIZATION_ID = "357947E87C284935AD1D783CF6F099A1";
  // Goods Shipment: 500014
  private static final String M_INOUT_ID = "09658144E1AF40AC81A3E5F5C3D0F132";

  static final List<CancelAndReplaceTestData> PARAMS = Arrays.asList(
      new CancelAndReplaceTestData1(), new CancelAndReplaceTestData2(),
      new CancelAndReplaceTestData3(), new CancelAndReplaceTestData4(),
      new CancelAndReplaceTestData5(), new CancelAndReplaceTestData6(),
      new CancelAndReplaceTestData7(), new CancelAndReplaceTestData8(),
      new CancelAndReplaceTestData9(), new CancelAndReplaceTestData10(),
      new CancelAndReplaceTestData11(), new CancelAndReplaceTestData12(),
      new CancelAndReplaceTestData13(), new CancelAndReplaceTestData14(),
      new CancelAndReplaceTestData15(), new CancelAndReplaceTestData16(),
      new CancelAndReplaceTestData17(), new CancelAndReplaceTestData18(),
      new CancelAndReplaceTestData19(), new CancelAndReplaceTestData20(),
      new CancelAndReplaceTestData21(), new CancelAndReplaceTestData22(),
      new CancelAndReplaceTestData23());

  public static List<CancelAndReplaceTestData> params() {
    return PARAMS;
  }

  /**
   * Verifies Cancel and Replace functionality API with one or more than one replacement. Clone and
   * existing Order. Click on Cancel and Replace process that will create two new Orders in
   * temporary status. Update the Orders depending on the test executed and finally confirm those
   * orders. Different check points have been added in each stage to verify the results of the
   * processes.
   */
  @ParameterizedTest
  @MethodSource("params")
  public void cancelAndReplaceTest(CancelAndReplaceTestData testData) {
    // Set QA context
    OBContext.setAdminMode();
    try {
      OBContext.setOBContext(Users.ADMIN, Roles.QA_ADMIN_ROLE, QA_TEST_CLIENT_ID,
          ORGANIZATION_ID);

      VariablesSecureApp vars = new VariablesSecureApp(Users.ADMIN, QA_TEST_CLIENT_ID,
          ORGANIZATION_ID, Roles.QA_ADMIN_ROLE,
          OBContext.getOBContext().getLanguage().getLanguage());
      RequestContext.get().setVariableSecureApp(vars);

      Order oldOrder = CancelAndReplaceTestUtils.cloneAndCompleteOrder(testData);

      // Deliver old order if the test is for fully or partially delivered orders
      if (testData.getOldOrder().isDelivered()) {
        CancelAndReplaceTestUtils.createShipmentFromOrder(oldOrder, M_INOUT_ID, testData);
        OBDal.getInstance().refresh(oldOrder);
      }

      // Pay old order if the test is for fully or partially paid orders
      if (testData.isOrderPaid()) {
        CancelAndReplaceTestUtils.createOrderPayment(oldOrder, testData);
        OBDal.getInstance().refresh(oldOrder);
      }

      // Activate "Create netting shipment on Cancel and Replace" and
      // "Cancel and Replace - Associate shipment lines to new ticket" depending on the test
      boolean createNettingGoodsShipment = CancelAndReplaceTestUtils
          .isPreferenceEnabled(CancelAndReplaceUtils.CREATE_NETTING_SHIPMENT);

      if (testData.isActivateNettingGoodsShipmentPref() && !createNettingGoodsShipment) {
        Preferences.setPreferenceValue(CancelAndReplaceUtils.CREATE_NETTING_SHIPMENT, "Y", true,
            OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            null, null, null);
      } else if (!testData.isActivateNettingGoodsShipmentPref() && createNettingGoodsShipment) {
        Preferences.setPreferenceValue(CancelAndReplaceUtils.CREATE_NETTING_SHIPMENT, "N", true,
            OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            null, null, null);
      }

      boolean associateShipmentToNewReceipt = CancelAndReplaceTestUtils
          .isPreferenceEnabled(CancelAndReplaceUtils.ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET);

      if (testData.isActivateAssociateNettingGoodsShipmentPref()) {
        if (!associateShipmentToNewReceipt) {
          Preferences.setPreferenceValue(CancelAndReplaceUtils.ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET,
              "Y", true, OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              null, null, null);
        }
      } else if (!testData.isActivateAssociateNettingGoodsShipmentPref()
          && associateShipmentToNewReceipt) {
        Preferences.setPreferenceValue(CancelAndReplaceUtils.ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET,
            "N", true, OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            null, null, null);
      }

      // Create the new replacement order
      List<Order> newOrders = CancelAndReplaceUtils.createReplacementOrder(oldOrder,
          Collections.singletonMap(oldOrder.getWarehouse(), testData.getNewOrders().size()));
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
      newOrders = refreshOrders(newOrders);

      log.debug("New orders Created:{}",
          newOrders.stream().map(Order::getDocumentNo).collect(Collectors.toList()));
      log.debug(testData.getTestDescription());

      updateNewOrders(testData, newOrders);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      // Cancel and Replace Sales Order
      Set<String> newOrderIdSet = new LinkedHashSet<>();
      newOrders.forEach(order -> newOrderIdSet.add(order.getId()));

      CancelAndReplaceUtils.cancelAndReplaceOrder(oldOrder.getId(), newOrderIdSet,
          oldOrder.getOrganization().getId(), null, false);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();

      oldOrder = getOrder(oldOrder.getId());
      Order inverseOrder = oldOrder.getOrderCancelledorderList().get(0);
      newOrders = refreshOrders(newOrders);

      // Sales Orders Grand Total Amounts
      CancelAndReplaceTestUtils.assertOrderHeader(oldOrder, testData.getOldOrder(),
          CancelAndReplaceTestUtils.DOCUMENT_NO_PREFIX + testData.getTestNumber());
      CancelAndReplaceTestUtils.assertOrderHeader(inverseOrder, testData.getInverseOrder(),
          CancelAndReplaceTestUtils.DOCUMENT_NO_PREFIX + testData.getTestNumber() + "*R*");
      for (int i = 0; i < newOrders.size(); i++) {
        CancelAndReplaceTestUtils.assertOrderHeader(newOrders.get(i),
            testData.getNewOrders().get(i), CancelAndReplaceTestUtils.DOCUMENT_NO_PREFIX
                + testData.getTestNumber() + "-" + (i + 1));
      }

      // Relations between orders
      Optional<String> replacementOrderId = newOrderIdSet.stream()
          .skip(newOrders.size() - 1L)
          .findFirst();

      assertOrderRelations(oldOrder, newOrders, inverseOrder, replacementOrderId);

      // Sales Orders Received and Outstanding payments
      CancelAndReplaceTestUtils.assertOrderPayment(oldOrder, testData.getOldOrder());
      CancelAndReplaceTestUtils.assertOrderPayment(inverseOrder, testData.getInverseOrder());
      for (int i = 0; i < newOrders.size(); i++) {
        CancelAndReplaceTestUtils.assertOrderPayment(newOrders.get(i),
            testData.getNewOrders().get(i));
      }

      // Assert Lines
      assertOrderLines(oldOrder.getOrderLineList(), testData.getOldOrder());
      assertOrderLines(inverseOrder.getOrderLineList(), testData.getInverseOrder());
      for (int i = 0; i < newOrders.size(); i++) {
        assertOrderLines(newOrders.get(i).getOrderLineList(), testData.getNewOrders().get(i));
      }
    } catch (Exception e) {
      log.error("Error when executing: " + testData.getTestDescription(), e);
      assertFalse(true);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void assertOrderRelations(Order oldOrder, List<Order> newOrders, Order inverseOrder,
      Optional<String> replacementOrderId) {
    final String oldOrderId = oldOrder.getId();
    newOrders.forEach(newOrder -> assertThat("Wrong Cancelled Order id",
        newOrder.getReplacedorder().getId(), equalTo(oldOrderId)));
    assertThat("Wrong Cancelled Order id", inverseOrder.getCancelledorder().getId(),
        equalTo(oldOrderId));
    assertThat("Wrong Replacement Order id", oldOrder.getReplacementorder().getId(),
        equalTo(replacementOrderId.isPresent() ? replacementOrderId.get() : ""));

    List<OrderReplacement> orderReplacements = getOrderReplacement(oldOrder);
    assertThat("Order replacement should be " + newOrders.size(), orderReplacements.size(),
        equalTo(newOrders.size()));
    newOrders.forEach(newOrder -> assertTrue(orderReplacements.stream()
                .anyMatch(orderReplacement -> StringUtils
                    .equals(orderReplacement.getReplacement().getId(), newOrder.getId())),
        "Order " + newOrder.getDocumentNo() + " should be in replacement list"));
  }

  private List<Order> refreshOrders(List<Order> newOrders) {
    List<Order> orders = new ArrayList<>(2);
    newOrders.forEach(order -> orders.add(OBDal.getInstance().get(Order.class, order.getId())));
    return orders;
  }

  private void assertOrderLines(List<OrderLine> lines,
      CancelAndReplaceOrderTestData orderTestData) {
    for (int i = 0; i < lines.size(); i++) {
      CancelAndReplaceTestUtils.assertOrderLine(lines.get(i), orderTestData.getLines()[i]);
    }
  }

  private void updateNewOrders(CancelAndReplaceTestData testData, List<Order> newOrders) {
    for (int i = 0; i < newOrders.size(); i++) {
      updateOrder(i, newOrders.get(i), testData.getNewOrders().get(i).getLines()[0]);
    }
  }

  private void updateOrder(int index, Order order, Line lineData) {
    OrderLine orderLine = null;
    for (int i = 0; i < order.getOrderLineList().size(); i++) {
      OrderLine line = order.getOrderLineList().get(i);
      if (line.getLineNo() == (index + 1) * 10) {
        orderLine = line;
        orderLine.setOrderedQuantity(lineData.getOrderedQuantity());
        OBDal.getInstance().save(orderLine);
      } else {
        OBDal.getInstance().remove(line);
      }
    }
    order.getOrderLineList().clear();
    order.getOrderLineList().add(orderLine);
    OBDal.getInstance().save(order);
  }

  private Order getOrder(String orderId) {
    return OBDal.getInstance().get(Order.class, orderId);
  }

  private List<OrderReplacement> getOrderReplacement(Order order) {
    //@formatter:off
    String hql = " as r"
        + " where r.salesOrder.id = :orderId"
        + " order by r.replacement.organization.id";
    //@formatter:on

    OBQuery<OrderReplacement> query = OBDal.getInstance().createQuery(OrderReplacement.class, hql);
    query.setNamedParameter("orderId", order.getId());
    return query.list();
  }
}
