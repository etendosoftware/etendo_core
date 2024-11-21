package org.openbravo.test.stockReservation;

import static org.openbravo.test.stockReservation.StockReservationTestUtils.createInventoryCount;
import static org.openbravo.test.stockReservation.StockReservationTestUtils.createOrder;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.criterion.Restrictions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DbUtility;
import org.openbravo.test.base.TestConstants;

/**
 * Test class for automated warehouse reservation.
 * This class validates the creation and processing of inventory counts,
 * sales orders, and automatic reservations across single or multiple warehouses.
 */
public class StockReservationTest extends WeldBaseTest {

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
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN, TestConstants.Clients.FB_GRP,
        TestConstants.Orgs.ESP);
    OBContext currentContext = OBContext.getOBContext();
    VariablesSecureApp vsa = new VariablesSecureApp(currentContext.getUser().getId(),
        currentContext.getCurrentClient().getId(), currentContext.getCurrentOrganization().getId(),
        currentContext.getRole().getId());
    RequestContext.get().setVariableSecureApp(vsa);
  }

  /**
   * Processes a given sales order by calling the corresponding Openbravo process.
   *
   * @param salesOrder
   *     the sales order to be processed
   * @throws OBException
   *     if the process fails
   */
  private void processOrder(final Order salesOrder) {
    org.openbravo.model.ad.ui.Process process = null;
    try {
      OBContext.setAdminMode(true);
      process = OBDal.getInstance().get(Process.class, "104");
    } finally {
      OBContext.restorePreviousMode();
    }
    try {
      final ProcessInstance pinstance = CallProcess.getInstance().call(process, salesOrder.getId(), null);
      OBDal.getInstance().refresh(salesOrder);

      if (pinstance.getResult() == 0L) {
        OBError oberror = OBMessageUtils.getProcessInstanceMessage(pinstance);
        throw new OBException(oberror.getMessage());
      }
    } catch (Exception e) {
      final Throwable t = DbUtility.getUnderlyingSQLException(e);
      throw new OBException(OBMessageUtils.parseTranslation(t.getMessage()), t);
    }
  }

  /**
   * Test for automatic reservation when inventory is available in a single warehouse.
   */
  @Test
  public void AutomaticReservationOneWarehouse() {
    InventoryCount WarehouseRn = createInventoryCountRN();
    new InventoryCountProcess().processInventory(WarehouseRn, false, true);

    Order salesOrder = createOrderOneWarehouse();
    processOrder(salesOrder);

    verifyReservationDetails(salesOrder, "Rn-0-0-0", BigDecimal.valueOf(1000));
  }

  /**
   * Test for automatic reservation when inventory is distributed across multiple warehouses.
   */
  @Test
  public void AutomaticReservationMoreThanOneWarehouse() {
    InventoryCount WarehouseRn = createInventoryCountRN();
    new InventoryCountProcess().processInventory(WarehouseRn, false, true);

    InventoryCount WarehouseRs = createInventoryCountRS();
    new InventoryCountProcess().processInventory(WarehouseRs, false, true);

    Order salesOrder = createOrderMoreThanOneWarehouse();
    processOrder(salesOrder);

    verifyReservationDetails(salesOrder, "Rn-0-0-0", BigDecimal.valueOf(1000));
    verifyReservationDetails(salesOrder, "RS-0-0-0", BigDecimal.valueOf(1000));
  }

  /**
   * Verifies reservation details for a specific storage bin and expected quantity.
   *
   * @param salesOrder
   *     the sales order linked to the reservation
   * @param binSearchKey
   *     the search key of the storage bin
   * @param expectedQuantity
   *     the expected reserved quantity
   */
  private void verifyReservationDetails(Order salesOrder, String binSearchKey, BigDecimal expectedQuantity) {
    Reservation reservation = findReservationForOrder(salesOrder);
    List<ReservationStock> reservationStocks = findReservationStocksForReservation(reservation);

    boolean foundMatchingBin = reservationStocks.stream().anyMatch(
        stock -> binSearchKey.equals(stock.getStorageBin().getSearchKey()) && stock.getQuantity().compareTo(
            expectedQuantity) == 0);

    assertTrue(String.format("A StorageBin with searchKey '%s' and quantity '%s' was not found.", binSearchKey,
        expectedQuantity), foundMatchingBin);
  }

  /**
   * Cleans up the test environment by rolling back the transaction and closing the session.
   */
  @After
  public void cleanUp() {
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Creates a sales order for a single warehouse scenario.
   *
   * @return the created sales order
   */
  private Order createOrderOneWarehouse() {
    return createOrder("One Warehouse", "DR", "CO", new BigDecimal(1000), StockReservationTestUtils.TAX_ID, "CRP");
  }

  /**
   * Finds the reservation associated with a specific sales order.
   *
   * @param salesOrder
   *     the sales order for which the reservation is to be found
   * @return the reservation associated with the sales order, or {@code null} if none exists
   */
  private Reservation findReservationForOrder(Order salesOrder) {
    OBCriteria<Reservation> reservationCriteria = OBDal.getInstance().createCriteria(Reservation.class);
    reservationCriteria.add(
        Restrictions.eq(Reservation.PROPERTY_SALESORDERLINE + ".id", salesOrder.getOrderLineList().get(0).getId()));
    return (Reservation) reservationCriteria.setMaxResults(1).uniqueResult();
  }

  /**
   * Retrieves the list of reservation stock entries for a given reservation.
   *
   * @param reservation
   *     the reservation for which stock entries are to be retrieved
   * @return a list of {@link ReservationStock} entries associated with the reservation
   */
  private List<ReservationStock> findReservationStocksForReservation(Reservation reservation) {
    OBCriteria<ReservationStock> reservationStockCriteria = OBDal.getInstance().createCriteria(ReservationStock.class);
    reservationStockCriteria.add(Restrictions.eq(ReservationStock.PROPERTY_RESERVATION + ".id", reservation.getId()));
    return reservationStockCriteria.list();
  }

  /**
   * Creates a sales order for a scenario where inventory is distributed across multiple warehouses.
   *
   * @return the created sales order
   */
  private Order createOrderMoreThanOneWarehouse() {
    return createOrder("More than One Warehouse", "DR", "CO", new BigDecimal(2000), StockReservationTestUtils.TAX_ID,
        "CRP");
  }

  /**
   * Creates an inventory count for the "RN" warehouse.
   *
   * @return the created inventory count
   */
  private InventoryCount createInventoryCountRN() {
    Locator storageBin = OBDal.getInstance().get(Locator.class, StockReservationTestUtils.LOCATOR_RN_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, StockReservationTestUtils.PRODUCT_PRICE);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, StockReservationTestUtils.WAREHOUSE_RN_ID);

    return createInventoryCount("Warehouse Rn-0-0-0", productPrice, storageBin, warehouse);
  }

  /**
   * Creates an inventory count for the "RS" warehouse.
   *
   * @return the created inventory count
   */
  private InventoryCount createInventoryCountRS() {
    Locator storageBin = OBDal.getInstance().get(Locator.class, StockReservationTestUtils.LOCATOR_RS_ID);
    ProductPrice productPrice = OBDal.getInstance().get(ProductPrice.class, StockReservationTestUtils.PRODUCT_PRICE);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, StockReservationTestUtils.WAREHOUSE_RS_ID);

    return createInventoryCount("Warehouse Rs-0-0-0", productPrice, storageBin, warehouse);
  }
}
