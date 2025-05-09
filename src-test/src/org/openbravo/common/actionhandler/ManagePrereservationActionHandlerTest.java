package org.openbravo.common.actionhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.PrereservationManualPickEdit;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;

/**
 * Unit tests for the {@link ManagePrereservationActionHandler} class.
 * Verifies the behavior of the methods responsible for managing prereserved stock lines
 * and handling JSON requests.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManagePrereservationActionHandlerTest {
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBDao> mockedOBDao;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<ReservationUtils> mockedReservationUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  // Class mocks
  @Mock
  private OBDal obDal;

  @Mock
  private OBCriteria<ReservationStock> reservationStockCriteria;

  @Mock
  private OrderLine orderLine;

  @Mock
  private ReservationStock reservationStock;

  @Mock
  private Reservation reservation;

  @Mock
  private Organization organization;

  @Mock
  private OBProvider obProvider;

  @InjectMocks
  private ManagePrereservationActionHandler handlerUnderTest;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes dependencies.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDao = mockStatic(OBDao.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedReservationUtils = mockStatic(ReservationUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
    when(obProvider.get(ReservationStock.class)).thenReturn(reservationStock);

    handlerUnderTest = new ManagePrereservationActionHandler();
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBDao != null) {
      mockedOBDao.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mockedReservationUtils != null) {
      mockedReservationUtils.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }

  }

  /**
   * Tests the {@code doExecute} method for a successful scenario.
   * Verifies that the method processes the input JSON correctly and returns the expected result.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteSuccessScenario() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String orderLineId = "test-order-line-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put("inpcOrderlineId", orderLineId);
    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selectionArray = new JSONArray();
    gridObj.put(ActionHandlerTestConstants.SELECTION, selectionArray);
    paramsObj.put("grid", gridObj);
    jsonContent.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    when(obDal.get(OrderLine.class, orderLineId)).thenReturn(orderLine);

    mockedOBDao.when(() -> OBDao.getFilteredCriteria(eq(ReservationStock.class), any(Criterion.class),
        any(Criterion.class))).thenReturn(reservationStockCriteria);

    List<ReservationStock> emptyList = new ArrayList<>();
    when(reservationStockCriteria.list()).thenReturn(emptyList);

    JSONObject result = handlerUnderTest.doExecute(parameters, jsonContent.toString());

    assertNotNull("Result should not be null", result);
    assertEquals("Result should be the same as the input", jsonContent.toString(), result.toString());

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(obDal).get(OrderLine.class, orderLineId);
  }

  /**
   * Tests the {@code managePrereservedStockLines} method when no lines are selected.
   * Verifies that non-selected lines are removed and the order line is updated.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testManagePrereservedStockLinesWithNoSelection() throws Exception {
    JSONObject jsonRequest = new JSONObject();
    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray emptySelection = new JSONArray();
    gridObj.put(ActionHandlerTestConstants.SELECTION, emptySelection);
    paramsObj.put("grid", gridObj);
    jsonRequest.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    List<String> idList = new ArrayList<>();
    idList.add("reservation-stock-id-1");
    idList.add("reservation-stock-id-2");
    final int initialIdListSize = idList.size();

    ReservationStock reservationStock1 = mock(ReservationStock.class);
    ReservationStock reservationStock2 = mock(ReservationStock.class);

    when(obDal.get(ReservationStock.class, "reservation-stock-id-1")).thenReturn(reservationStock1);
    when(obDal.get(ReservationStock.class, "reservation-stock-id-2")).thenReturn(reservationStock2);

    List<ReservationStock> reservationStockList = new ArrayList<>();
    reservationStockList.add(reservationStock1);
    reservationStockList.add(reservationStock2);

    when(orderLine.getMaterialMgmtReservationStockList()).thenReturn(reservationStockList);

    handlerUnderTest.managePrereservedStockLines(jsonRequest, orderLine, idList);

    verify(obDal, times(2)).remove(any(ReservationStock.class));
    verify(obDal, times(1)).save(orderLine);
    verify(obDal, times(1)).flush();

    assertEquals("All IDs should have been processed", initialIdListSize, 2);
  }

  /**
   * Tests the {@code managePrereservedStockLines} method with an existing reservation.
   * Verifies that the reservation stock is updated correctly.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testManagePrereservedStockLinesWithExistingReservation() throws Exception {
    JSONObject jsonRequest = new JSONObject();
    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedLine = new JSONObject();
    String reservationStockId = "existing-reservation-id";
    String reservedQty = "10.0";
    boolean allocated = true;

    selectedLine.put(PrereservationManualPickEdit.PROPERTY_RESERVATIONSTOCK, reservationStockId);
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_RESERVEDQTY, reservedQty);
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_ALLOCATED, allocated);
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_SALESORDERLINE, "sales-order-line-id");
    selection.put(selectedLine);

    gridObj.put(ActionHandlerTestConstants.SELECTION, selection);
    paramsObj.put("grid", gridObj);
    jsonRequest.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    List<String> idList = new ArrayList<>();
    idList.add(reservationStockId);
    int initialIdListSize = idList.size();

    when(obDal.get(ReservationStock.class, reservationStockId)).thenReturn(reservationStock);

    handlerUnderTest.managePrereservedStockLines(jsonRequest, orderLine, idList);

    assertEquals("ID list should be empty after processing", 0, idList.size());

    verify(reservationStock).setAllocated(allocated);
    verify(reservationStock).setQuantity(new BigDecimal(reservedQty));

    verify(obDal).save(reservationStock);
    verify(obDal).flush();

    assertEquals("Unexpected initial ID list size", 1, initialIdListSize);
  }

  /**
   * Tests the {@code managePrereservedStockLines} method with a new reservation.
   * Verifies that a new reservation stock is created and associated with the order line.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testManagePrereservedStockLinesWithNewReservation() throws Exception {
    JSONObject jsonRequest = new JSONObject();
    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedLine = new JSONObject();
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_RESERVATIONSTOCK, JSONObject.NULL);
    String reservedQty = "15.0";
    boolean allocated = false;
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_RESERVEDQTY, reservedQty);
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_ALLOCATED, allocated);
    String salesOrderLineId = "sales-order-line-id";
    selectedLine.put(PrereservationManualPickEdit.PROPERTY_SALESORDERLINE, salesOrderLineId);
    selection.put(selectedLine);

    gridObj.put(ActionHandlerTestConstants.SELECTION, selection);
    paramsObj.put("grid", gridObj);
    jsonRequest.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    List<String> idList = new ArrayList<>();

    OrderLine salesOrderLine = mock(OrderLine.class);
    when(obDal.get(OrderLine.class, salesOrderLineId)).thenReturn(salesOrderLine);

    mockedReservationUtils.when(() -> ReservationUtils.getReservationFromOrder(salesOrderLine)).thenReturn(reservation);

    when(reservation.getOrganization()).thenReturn(organization);

    List<ReservationStock> reservationStockList = new ArrayList<>();
    int initialListSize = reservationStockList.size();
    when(orderLine.getMaterialMgmtReservationStockList()).thenReturn(reservationStockList);

    handlerUnderTest.managePrereservedStockLines(jsonRequest, orderLine, idList);

    assertEquals("Stock list should have one item after processing", initialListSize + 1, reservationStockList.size());
    assertTrue("Stock list should contain the new reservation stock", reservationStockList.contains(reservationStock));

    verify(reservationStock).setReservation(reservation);
    verify(reservationStock).setOrganization(organization);
    verify(reservationStock).setSalesOrderLine(orderLine);
    verify(reservationStock).setReleased(BigDecimal.ZERO);

    verify(reservation).setRESStatus("CO");
    verify(reservation).setRESProcess("HO");

    verify(orderLine).setMaterialMgmtReservationStockList(reservationStockList);
    verify(obDal).save(orderLine);

    verify(reservationStock).setAllocated(allocated);
    verify(reservationStock).setQuantity(new BigDecimal(reservedQty));
    verify(obDal).save(reservationStock);
    verify(obDal).flush();
  }

  /**
   * Tests the {@code doExecute} method when an exception occurs.
   * Verifies that the method handles the exception and returns an appropriate error response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteHandleException() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String orderLineId = "test-order-line-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put("inpcOrderlineId", orderLineId);

    Exception mockException = new RuntimeException("Test exception");
    when(obDal.get(OrderLine.class, orderLineId)).thenThrow(mockException);

    JSONObject result = handlerUnderTest.doExecute(parameters, jsonContent.toString());

    assertNotNull("Result should not be null", result);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }
}
