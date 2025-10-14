package org.openbravo.test.stockReservation;

import static org.openbravo.materialmgmt.ReservationUtils.processReserve;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.createInventoryCount;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.stockReservationPreference;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.createOrder;

import java.math.BigDecimal;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.ProcessOrderUtil;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.utils.Utils;

/**
 * Test class for automated warehouse reservation.
 * This class validates the creation and processing of inventory counts,
 * sales orders, and automatic reservations across single or multiple warehouses.
 */
public class StockReservationTest extends WeldBaseTest {

  @Inject
  private WeldUtils weldUtils;

  /**
   * Sets up the test environment, initializing the OBContext and VariablesSecureApp.
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
   * Tests the automatic reservation functionality for a single warehouse.
   *
   * This test ensures that:
   * - An inventory count is created and processed for a specific locator and warehouse.
   * - A sales order is completed, triggering an automatic reservation.
   * - The reservation details are validated to match the expected quantity in the single warehouse.
   *
   * Cleanup operations reprocess inventory levels and remove test data to reset the system state.
   *
   * @throws Exception
   *     if any error occurs during the test execution.
   */
  @Test
  public void AutomaticReservationOneWarehouse() {
    Reservation reservation = null;
    Order salesOrder = null;

    InventoryCount warehouseRn = null;

    try {
      warehouseRn = createInventoryCount(StockReservationTestUtils.LOCATOR_RN, StockReservationTestUtils._1000,
          StockReservationTestUtils.LOCATOR_RN_ID, StockReservationTestUtils.WAREHOUSE_RN_ID);
      new InventoryCountProcess().processInventory(warehouseRn, false, true);

      salesOrder = createOrder("OW - 001", StockReservationTestUtils.DRAFT, StockReservationTestUtils.COMPLETED,
          StockReservationTestUtils._1000, StockReservationTestUtils.TAX_ID,
          StockReservationTestUtils.AUTOMATIC_RESERVATION);

      processOrder(salesOrder, StockReservationTestUtils.COMPLETED);
      reservation = StockReservationTestUtils.findReservationForOrder(salesOrder);

      StockReservationTestUtils.verifyAutomaticReservationDetails(reservation, StockReservationTestUtils.LOCATOR_RN,
          BigDecimal.valueOf(1000));
    } catch (Exception e) {
      Assert.fail(StockReservationTestUtils.ERROR + e.getMessage());
    } finally {
      cleanUpData(reservation, salesOrder, null, null);
    }
  }

  /**
   * Tests the automatic reservation functionality across multiple warehouses.
   *
   * This test ensures that:
   * - Inventory counts are created and processed for two different locators and warehouses.
   * - A sales order is completed, triggering an automatic reservation.
   * - The reservation details are validated to match the expected distribution across the warehouses.
   *
   * Cleanup operations reprocess inventory levels and remove test data to reset the system state.
   *
   * @throws Exception
   *     if any error occurs during the test execution.
   */
  @Test
  public void AutomaticReservationMoreThanOneWarehouse() {
    Reservation reservation = null;
    Order salesOrder = null;

    InventoryCount warehouseRn = null;
    InventoryCount warehouseRs = null;

    try {
      warehouseRn = createInventoryCount(StockReservationTestUtils.LOCATOR_RN, StockReservationTestUtils._1000,
          StockReservationTestUtils.LOCATOR_RN_ID, StockReservationTestUtils.WAREHOUSE_RN_ID);
      new InventoryCountProcess().processInventory(warehouseRn, false, true);

      warehouseRs = createInventoryCount(StockReservationTestUtils.LOCATOR_RS, StockReservationTestUtils._1000,
          StockReservationTestUtils.LOCATOR_RS_ID, StockReservationTestUtils.WAREHOUSE_RS_ID);
      new InventoryCountProcess().processInventory(warehouseRs, false, true);

      salesOrder = createOrder("MOW - 001", StockReservationTestUtils.DRAFT, StockReservationTestUtils.COMPLETED,
          new BigDecimal(2000), StockReservationTestUtils.TAX_ID, StockReservationTestUtils.AUTOMATIC_RESERVATION);

      processOrder(salesOrder, StockReservationTestUtils.COMPLETED);
      reservation = StockReservationTestUtils.findReservationForOrder(salesOrder);

      StockReservationTestUtils.verifyAutomaticReservationDetails(reservation, StockReservationTestUtils.LOCATOR_RN,
          BigDecimal.valueOf(1000));
      StockReservationTestUtils.verifyAutomaticReservationDetails(reservation, StockReservationTestUtils.LOCATOR_RS,
          BigDecimal.valueOf(1000));
    } catch (Exception e) {
      Assert.fail(StockReservationTestUtils.ERROR + e.getMessage());
    } finally {
      cleanUpData(reservation, salesOrder, warehouseRn, warehouseRs);
    }
  }

  /**
   * Verifies the manual reservation functionality in the stock reservation system.
   *
   * This test ensures that:
   * - An inventory count is created and processed for a specified locator and warehouse.
   * - A sales order with a manual reservation type is completed.
   * - The reservation details are validated against expected values.
   *
   * Cleanup operations remove the created reservation and sales order, and reset inventory levels.
   *
   * @throws Exception
   *     if any error occurs during the test execution.
   */
  @Test
  public void ManualReservationTest() {
    Reservation reservation = null;
    Order salesOrder = null;

    InventoryCount warehouseRn = null;

    try {
      warehouseRn = createInventoryCount(StockReservationTestUtils.LOCATOR_RN, StockReservationTestUtils._1000,
          StockReservationTestUtils.LOCATOR_RN_ID, StockReservationTestUtils.WAREHOUSE_RN_ID);
      new InventoryCountProcess().processInventory(warehouseRn, false, true);

      salesOrder = createOrder("MR - 001", StockReservationTestUtils.DRAFT, StockReservationTestUtils.COMPLETED,
          StockReservationTestUtils._1000, StockReservationTestUtils.TAX_ID,
          StockReservationTestUtils.MANUAL_RESERVATION);

      processOrder(salesOrder, StockReservationTestUtils.COMPLETED);
      reservation = StockReservationTestUtils.findReservationForOrder(salesOrder);

      StockReservationTestUtils.verifyManualReservationDetails(reservation, StockReservationTestUtils.LOCATOR_RN,
          BigDecimal.valueOf(1000));
    } catch (Exception e) {
      Assert.fail(StockReservationTestUtils.ERROR + e.getMessage());
    } finally {
      cleanUpData(reservation, salesOrder, null, null);
    }
  }

  /**
   * Cleans up the test environment by rolling back the transaction and closing the session.
   */
  @After
  public void cleanUp() {
    Preference pref = (Preference) OBDal.getInstance().createCriteria(Preference.class).add(
        Restrictions.eq(Preference.PROPERTY_PROPERTY, StockReservationTestUtils.PREFERENCE_PROPERTY)).add(
        Restrictions.eq(Preference.PROPERTY_SELECTED, true)).uniqueResult();
    OBDal.getInstance().remove(pref);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Processes a sales order by invoking the order processing utility.
   * This method executes a specific document action on the provided order
   * using the {@link ProcessOrderUtil} class. It ensures that the action is
   * processed within the context of the current session.
   *
   * @param order
   *     the sales order to be processed
   * @param docAction
   *     the document action to be performed on the order
   */
  private void processOrder(Order order, String docAction) {
    var processor = weldUtils.getInstance(ProcessOrderUtil.class);

    processor.process(order.getId(), docAction, RequestContext.get().getVariablesSecureApp(),
        new DalConnectionProvider(false));
  }

  /**
   * Cleans up data related to reservations, sales orders, and inventory counts by deactivating or removing
   * them from the system.
   *
   * @param reservation
   *     the Reservation object to clean up; if its status is "COMPLETED", it will be reactivated before removal
   * @param salesOrder
   *     the Order object to clean up; it will be reactivated before removal
   * @param inventoryCountRn
   *     the InventoryCount object for warehouse "RN" to clean up; it will be reset to default stock levels if not null
   * @param inventoryCountRs
   *     the InventoryCount object for warehouse "RS" to clean up; it will be reset to default stock levels if not null
   */
  private void cleanUpData(Reservation reservation, Order salesOrder, InventoryCount inventoryCountRn,
      InventoryCount inventoryCountRs) {

    if (reservation != null) {
      if (StringUtils.equals(reservation.getRESStatus(), StockReservationTestUtils.COMPLETED)) {
        processReserve(reservation, StockReservationTestUtils.REACTIVATE);
      }
      OBDal.getInstance().remove(reservation);
    }

    if (salesOrder != null) {
      processOrder(salesOrder, StockReservationTestUtils.REACTIVATE);
      OBDal.getInstance().remove(salesOrder);
    }

    if (inventoryCountRn != null) {
      InventoryCount warehouseRnDefault = createInventoryCount(StockReservationTestUtils.LOCATOR_RN,
          StockReservationTestUtils.STOCK_DEFAULT, StockReservationTestUtils.LOCATOR_RN_ID,
          StockReservationTestUtils.WAREHOUSE_RN_ID);
      new InventoryCountProcess().processInventory(warehouseRnDefault, false, true);
    }

    if (inventoryCountRs != null) {
      InventoryCount warehouseRsDefault = createInventoryCount(StockReservationTestUtils.LOCATOR_RS,
          StockReservationTestUtils.ZERO, StockReservationTestUtils.LOCATOR_RS_ID,
          StockReservationTestUtils.WAREHOUSE_RS_ID);
      new InventoryCountProcess().processInventory(warehouseRsDefault, false, true);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
  }
}
