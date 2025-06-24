package org.openbravo.common.actionhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;

/**
 * Unit tests for the {@link ManageReservationActionHandler} class.
 * Verifies the behavior of the `doExecute` and `manageReservedStockLines` methods,
 * ensuring proper handling of reservations and stock lines.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageReservationActionHandlerTest {
  private static final String RESERVATIONS_TABLE_ID = "77264B07BB0E4FA483A07FB40C2E0FE0";
  private static final String ORDER_LINE_TABLE_ID = "260";
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<ReservationUtils> mockedReservationUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  @Mock
  private OBDal obDal;
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
  private ManageReservationActionHandler handlerUnderTest;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes required dependencies.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedReservationUtils = mockStatic(ReservationUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
    when(obProvider.get(ReservationStock.class)).thenReturn(reservationStock);

    OBError mockError = mock(OBError.class);
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockError);

    handlerUnderTest = new ManageReservationActionHandler();
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
   * Tests the `doExecute` method with a reservation table ID.
   * Verifies that the method processes the reservation correctly.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteWithReservationsTable() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String reservationId = "test-reservation-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put(ActionHandlerTestConstants.INP_TABLE_ID, RESERVATIONS_TABLE_ID);
    jsonContent.put("inpmReservationId", reservationId);

    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selectionArray = new JSONArray();
    gridObj.put(ActionHandlerTestConstants.SELECTION, selectionArray);
    paramsObj.put("grid", gridObj);
    jsonContent.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    when(obDal.get(Reservation.class, reservationId)).thenReturn(reservation);
    when(reservation.getRESStatus()).thenReturn("CO");

    List<ReservationStock> emptyList = new ArrayList<>();
    when(reservation.getMaterialMgmtReservationStockList()).thenReturn(emptyList);

    JSONObject result = handlerUnderTest.doExecute(parameters, jsonContent.toString());

    assertNotNull(ActionHandlerTestConstants.RESULT_NOT_NULL, result);
    assertEquals("Result should match input", jsonContent.toString(), result.toString());

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(obDal).get(Reservation.class, reservationId);
  }

  /**
   * Tests the `doExecute` method with an order line table ID and a draft reservation.
   * Verifies that the reservation is processed and updated to completed status.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteWithOrderLineTableAndDraftReservation() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String orderLineId = "test-order-line-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put(ActionHandlerTestConstants.INP_TABLE_ID, ORDER_LINE_TABLE_ID);
    jsonContent.put("C_OrderLine_ID", orderLineId);

    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selectionArray = new JSONArray();
    gridObj.put(ActionHandlerTestConstants.SELECTION, selectionArray);
    paramsObj.put("grid", gridObj);
    jsonContent.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    when(obDal.get(OrderLine.class, orderLineId)).thenReturn(orderLine);
    mockedReservationUtils.when(() -> ReservationUtils.getReservationFromOrder(orderLine)).thenReturn(reservation);
    when(reservation.getRESStatus()).thenReturn("DR");

    OBError successResult = new OBError();
    successResult.setType(ActionHandlerTestConstants.SUCCESS);
    successResult.setMessage("Processed successfully");
    mockedReservationUtils.when(() -> ReservationUtils.processReserve(reservation, "PR")).thenReturn(successResult);

    List<ReservationStock> emptyList = new ArrayList<>();
    when(reservation.getMaterialMgmtReservationStockList()).thenReturn(emptyList);

    JSONObject result = handlerUnderTest.doExecute(parameters, jsonContent.toString());

    assertNotNull(ActionHandlerTestConstants.RESULT_NOT_NULL, result);

    mockedReservationUtils.verify(() -> ReservationUtils.processReserve(reservation, "PR"));
    verify(reservation).setRESStatus("CO");
    verify(reservation).setRESProcess("HO");

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }

  /**
   * Tests the `doExecute` method when a reservation processing error occurs.
   * Verifies that the error message is included in the response.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testDoExecuteProcessReservationError() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String orderLineId = "test-order-line-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put(ActionHandlerTestConstants.INP_TABLE_ID, ORDER_LINE_TABLE_ID);
    jsonContent.put("C_OrderLine_ID", orderLineId);

    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selectionArray = new JSONArray();
    gridObj.put(ActionHandlerTestConstants.SELECTION, selectionArray);
    paramsObj.put("grid", gridObj);
    jsonContent.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    when(obDal.get(OrderLine.class, orderLineId)).thenReturn(orderLine);
    mockedReservationUtils.when(() -> ReservationUtils.getReservationFromOrder(orderLine)).thenReturn(reservation);
    when(reservation.getRESStatus()).thenReturn("DR");

    OBError errorResult = new OBError();
    errorResult.setType("Error");
    errorResult.setMessage("Process error");
    mockedReservationUtils.when(() -> ReservationUtils.processReserve(reservation, "PR")).thenReturn(errorResult);

    JSONObject result = handlerUnderTest.doExecute(parameters, jsonContent.toString());

    assertNotNull(ActionHandlerTestConstants.RESULT_NOT_NULL, result);
    assertTrue("Result should contain message field", result.has("message"));
    JSONObject errorMessage = result.getJSONObject("message");
    assertEquals("Error type should match", "error", errorMessage.getString("severity"));
    assertEquals("Error message should match", "Process error", errorMessage.getString("text"));

    verify(reservation, times(0)).setRESStatus("CO");
    verify(reservation, times(0)).setRESProcess("HO");
  }

  /**
   * Tests the `manageReservedStockLines` method with a new reservation.
   * Verifies that the reservation stock lines are created and saved correctly.
   *
   * @throws Exception
   *     if an error occurs during execution
   */
  @Test
  public void testManageReservedStockLinesWithNewReservation() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String reservationId = "test-reservation-id";

    JSONObject jsonContent = new JSONObject();
    jsonContent.put(ActionHandlerTestConstants.INP_TABLE_ID, RESERVATIONS_TABLE_ID);
    jsonContent.put("inpmReservationId", reservationId);

    JSONObject paramsObj = new JSONObject();
    JSONObject gridObj = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedLine = new JSONObject();
    selectedLine.put("reservationStock", JSONObject.NULL);
    selectedLine.put("released", JSONObject.NULL);
    String quantity = "15.0";
    boolean allocated = false;
    selectedLine.put("quantity", quantity);
    selectedLine.put("allocated", allocated);

    String storageBinId = "storage-bin-id";
    String attributeSetValueId = "attribute-set-value-id";
    String purchaseOrderLineId = "purchase-order-line-id";
    selectedLine.put("storageBin", storageBinId);
    selectedLine.put("attributeSetValue", attributeSetValueId);
    selectedLine.put("purchaseOrderLine", purchaseOrderLineId);

    selection.put(selectedLine);

    gridObj.put(ActionHandlerTestConstants.SELECTION, selection);
    paramsObj.put("grid", gridObj);
    jsonContent.put(ActionHandlerTestConstants.PARAMS, paramsObj);

    when(obDal.get(Reservation.class, reservationId)).thenReturn(reservation);
    when(reservation.getRESStatus()).thenReturn("CO");
    when(reservation.getOrganization()).thenReturn(organization);

    Locator locator = mock(Locator.class);
    AttributeSetInstance attributeSetInstance = mock(AttributeSetInstance.class);
    OrderLine purchaseOrderLine = mock(OrderLine.class);

    when(obDal.getProxy(Locator.ENTITY_NAME, storageBinId)).thenReturn(locator);
    when(obDal.getProxy(AttributeSetInstance.ENTITY_NAME, attributeSetValueId)).thenReturn(attributeSetInstance);
    when(obDal.getProxy(OrderLine.ENTITY_NAME, purchaseOrderLineId)).thenReturn(purchaseOrderLine);

    List<ReservationStock> reservationStockList = new ArrayList<>();
    when(reservation.getMaterialMgmtReservationStockList()).thenReturn(reservationStockList);

    handlerUnderTest.doExecute(parameters, jsonContent.toString());

    verify(reservationStock).setReservation(reservation);
    verify(reservationStock).setOrganization(organization);
    verify(reservationStock).setStorageBin(locator);
    verify(reservationStock).setAttributeSetValue(attributeSetInstance);
    verify(reservationStock).setSalesOrderLine(purchaseOrderLine);
    verify(reservationStock).setReleased(BigDecimal.ZERO);
    verify(reservationStock).setAllocated(allocated);
    verify(reservationStock).setQuantity(new BigDecimal(quantity));

    assertTrue("Stock list should contain the new reservation stock", reservationStockList.contains(reservationStock));

    verify(obDal).save(reservationStock);
    verify(obDal).save(reservation);
    verify(obDal).flush();
  }
}
