/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.test.stockReservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.createOrder;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.stockReservationPreference;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.materialMgmt.invoiceFromShipment.TestUtils;
import org.openbravo.test.utils.Utils;

/**
 * Tests the validations added to the Re-activate action of C_ORDER_POST1: a Sales Order cannot be
 * reactivated if any of its lines has deliveries, or if any of its stock reservations is closed.
 * <p>
 * Completing an order commits it (and its reservation), so each created order is tracked and
 * removed in {@link #cleanUp()}. Shipments and reactivation attempts are rolled back.
 */
public class ReactivateOrderWithReservationTest extends WeldBaseTest {

  private static final String ORDER_POST_PROCEDURE = "c_order_post1";
  private static final String COMPLETE_SHIPMENT_ACTION = "CO";
  private static final String CLOSE_RESERVATION = "CL";
  private static final String SHIPMENT_MOVEMENT_TYPE = "C-";
  private static final String MSG_DELIVERIES = "ReactivateOrderWithDeliveries";
  private static final String MSG_CLOSED_RESERVATIONS = "ReactivateOrderWithClosedReservations";
  private static final BigDecimal ORDERED_QTY = BigDecimal.TEN;
  private final List<String> createdOrderIds = new ArrayList<>();

  @Inject
  private WeldUtils weldUtils;

  private static boolean messageChainContains(Throwable error, String text) {
    Throwable current = error;
    while (current != null) {
      if (current.getMessage() != null && current.getMessage().contains(text)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  /**
   * Sets up the test environment, initializing the OBContext and the stock reservation preference.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    Utils.initializeTestContext();
    stockReservationPreference();
  }

  /**
   * Discards any pending change, removes the orders committed by the test and the preference
   * created in the setup.
   */
  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
    for (String orderId : createdOrderIds) {
      cleanUpOrder(orderId);
    }
    createdOrderIds.clear();
    Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class).add(
        Restrictions.eq(Preference.PROPERTY_PROPERTY, StockReservationTestUtils.PREFERENCE_PROPERTY)).add(
        Restrictions.eq(Preference.PROPERTY_SELECTED, true)).setMaxResults(1).uniqueResult();
    if (pref != null) {
      OBDal.getInstance().remove(pref);
      OBDal.getInstance().flush();
    }
    OBDal.getInstance().commitAndClose();
  }

  /**
   * A completed Sales Order with an active reservation and no deliveries can still be reactivated.
   * C_ORDER_POST1 leaves a reactivated order in Draft status.
   */
  @Test
  public void reactivateOrderWithReservationAndNoDeliveries() {
    Order order = createCompletedOrder("RAR - 001");
    assertNotNull("The order should have a reservation", StockReservationTestUtils.findReservationForOrder(order));

    reactivate(order);

    assertEquals(StockReservationTestUtils.DRAFT, order.getDocumentStatus());
  }

  /**
   * A completed Sales Order with a completed Goods Shipment cannot be reactivated.
   */
  @Test
  public void reactivateOrderWithDeliveriesFails() {
    Order order = createCompletedOrder("RAR - 002");
    createAndProcessShipment(order);

    assertReactivationFails(order, MSG_DELIVERIES);
  }

  /**
   * A completed Sales Order whose reservation has been closed cannot be reactivated.
   */
  @Test
  public void reactivateOrderWithClosedReservationFails() {
    Order order = createCompletedOrder("RAR - 004");
    Reservation reservation = StockReservationTestUtils.findReservationForOrder(order);
    assertNotNull("The order should have a reservation", reservation);

    ReservationUtils.processReserve(reservation, CLOSE_RESERVATION);
    OBDal.getInstance().refresh(reservation);
    assertEquals(CLOSE_RESERVATION, reservation.getRESStatus());

    assertReactivationFails(order, MSG_CLOSED_RESERVATIONS);
  }

  /**
   * Creates a Sales Order with automatic reservation and completes it.
   */
  private Order createCompletedOrder(String docNo) {
    Order order = createOrder(docNo, StockReservationTestUtils.DRAFT, StockReservationTestUtils.COMPLETED, ORDERED_QTY,
        StockReservationTestUtils.TAX_ID, StockReservationTestUtils.AUTOMATIC_RESERVATION);
    createdOrderIds.add(order.getId());
    processOrder(order.getId(), StockReservationTestUtils.COMPLETED);
    order = OBDal.getInstance().get(Order.class, order.getId());
    assertEquals(StockReservationTestUtils.COMPLETED, order.getDocumentStatus());
    return order;
  }

  /**
   * Executes a document action on the order through {@link ProcessOrderUtil}, which commits the
   * transaction.
   */
  private void processOrder(String orderId, String docAction) {
    var processor = weldUtils.getInstance(ProcessOrderUtil.class);
    processor.process(orderId, docAction, RequestContext.get().getVariablesSecureApp(),
        new DalConnectionProvider(false));
  }

  /**
   * Removes an order committed by a test, following the same steps as
   * StockReservationTest#cleanUpData: the reservation is reactivated (only Draft reservations can
   * be deleted) and removed, then the order is reactivated and removed.
   */
  private void cleanUpOrder(String orderId) {
    Order order = OBDal.getInstance().get(Order.class, orderId);
    if (order == null) {
      return;
    }
    Reservation reservation = StockReservationTestUtils.findReservationForOrder(order);
    if (reservation != null) {
      if (!StringUtils.equals(reservation.getRESStatus(), StockReservationTestUtils.DRAFT)) {
        ReservationUtils.processReserve(reservation, StockReservationTestUtils.REACTIVATE);
        OBDal.getInstance().refresh(reservation);
      }
      OBDal.getInstance().remove(reservation);
      OBDal.getInstance().flush();
    }
    if (StringUtils.equals(order.getDocumentStatus(), StockReservationTestUtils.COMPLETED)) {
      processOrder(orderId, StockReservationTestUtils.REACTIVATE);
      order = OBDal.getInstance().get(Order.class, orderId);
    }
    OBDal.getInstance().remove(order);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }

  /**
   * Creates a Goods Shipment linked to the order lines and completes it.
   */
  private ShipmentInOut createAndProcessShipment(Order order) {
    Locator locator = OBDal.getInstance().get(Locator.class, StockReservationTestUtils.LOCATOR_RN_ID);
    Date now = new Date();

    ShipmentInOut shipment = OBProvider.getInstance().get(ShipmentInOut.class);
    shipment.setClient(order.getClient());
    shipment.setOrganization(order.getOrganization());
    shipment.setDocumentNo(order.getDocumentNo() + "-SHP");
    shipment.setDocumentStatus(StockReservationTestUtils.DRAFT);
    shipment.setDocumentAction(COMPLETE_SHIPMENT_ACTION);
    shipment.setDocumentType(order.getTransactionDocument().getDocumentTypeForShipment());
    shipment.setMovementType(SHIPMENT_MOVEMENT_TYPE);
    shipment.setMovementDate(now);
    shipment.setAccountingDate(now);
    shipment.setBusinessPartner(order.getBusinessPartner());
    shipment.setPartnerAddress(order.getPartnerAddress());
    shipment.setWarehouse(order.getWarehouse());
    shipment.setSalesTransaction(true);
    shipment.setSalesOrder(order);
    shipment.setProcessed(false);
    shipment.setPosted("N");
    OBDal.getInstance().save(shipment);

    List<ShipmentInOutLine> shipmentLines = new ArrayList<>();
    for (OrderLine orderLine : order.getOrderLineList()) {
      ShipmentInOutLine line = OBProvider.getInstance().get(ShipmentInOutLine.class);
      line.setClient(shipment.getClient());
      line.setOrganization(shipment.getOrganization());
      line.setShipmentReceipt(shipment);
      line.setLineNo(orderLine.getLineNo());
      line.setProduct(orderLine.getProduct());
      line.setUOM(orderLine.getUOM());
      line.setMovementQuantity(orderLine.getOrderedQuantity());
      line.setStorageBin(locator);
      line.setSalesOrderLine(orderLine);
      OBDal.getInstance().save(line);
      shipmentLines.add(line);
    }
    shipment.setMaterialMgmtShipmentInOutLineList(shipmentLines);
    OBDal.getInstance().flush();

    TestUtils.processShipmentReceipt(shipment);
    OBDal.getInstance().refresh(shipment);
    assertEquals(StockReservationTestUtils.COMPLETED, shipment.getDocumentStatus());
    OBDal.getInstance().refresh(order);
    return shipment;
  }

  /**
   * Executes the Re-activate action of the order through C_ORDER_POST1.
   */
  private void reactivate(Order order) {
    order.setDocumentAction(StockReservationTestUtils.REACTIVATE);
    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

    final List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(order.getId());
    CallStoredProcedure.getInstance().call(ORDER_POST_PROCEDURE, params, null, true, false);
    OBDal.getInstance().refresh(order);
  }

  /**
   * Asserts that the reactivation of the order fails with the given message key.
   */
  private void assertReactivationFails(Order order, String expectedMessageKey) {
    try {
      reactivate(order);
    } catch (Exception e) {
      assertTrue("Unexpected error: " + e.getMessage() + " (expected " + expectedMessageKey + ")",
          messageChainContains(e, expectedMessageKey));
      return;
    }
    fail("The reactivation should have failed with " + expectedMessageKey);
  }
}
