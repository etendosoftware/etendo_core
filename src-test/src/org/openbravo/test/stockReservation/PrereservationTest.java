package org.openbravo.test.stockReservation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.common.actionhandler.ManagePrereservationActionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.materialmgmt.onhandquantity.ReservationStock;

/**
 * Unit test for the ManagePrereservationActionHandler class.
 * This test verifies the behavior of managing prereserved stock lines.
 */
@RunWith(MockitoJUnitRunner.class)
public class PrereservationTest {

  @InjectMocks
  private ManagePrereservationActionHandler process;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private ReservationStock mockReservationStock;

  @Mock
  private Reservation mockReservation;

  @Mock
  private OrderLine mockOrderLine;

  private List<String> idList;

  /**
   * Sets up the test environment by initializing the mocks and other necessary variables.
   */
  @Before
  public void setup() {
    MockitoAnnotations.openMocks(this);
    process = Mockito.spy(new ManagePrereservationActionHandler());

    idList = new ArrayList<>();

    when(mockOrderLine.getMaterialMgmtReservationStockList()).thenReturn(new ArrayList<>());
  }

  /**
   * Verifies that the ManagePrereservationActionHandler properly creates and saves a new
   * ReservationStock instance when passed a request with a single order line selected.
   *
   * @throws JSONException
   *     if parsing the JSON requests fails
   */
  @Test
  public void testManagePrereservedStockLines_NewReservationStock() throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedLine = new JSONObject();
    selectedLine.put("reservationStock", JSONObject.NULL);
    selectedLine.put("salesOrderLine", "mockOrderLineId");
    selectedLine.put("reservedQty", "5");
    selectedLine.put("allocated", true);

    selection.put(selectedLine);
    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonRequest.put("_params", params);

    try (MockedStatic<OBDal> mockedOBDal = mockStatic(
        OBDal.class); MockedStatic<OBProvider> mockedOBProvider = mockStatic(
        OBProvider.class); MockedStatic<ReservationUtils> mockedReservationUtils = mockStatic(ReservationUtils.class)) {

      OBDal mockOBDalInstance = mock(OBDal.class);
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDalInstance);
      when(mockOBDalInstance.get(OrderLine.class, "mockOrderLineId")).thenReturn(mockOrderLine);
      when(mockOrderLine.getMaterialMgmtReservationStockList()).thenReturn(new ArrayList<>());

      OBProvider mockOBProviderInstance = mock(OBProvider.class);
      mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProviderInstance);
      when(mockOBProviderInstance.get(ReservationStock.class)).thenReturn(mockReservationStock);

      mockedReservationUtils.when(() -> ReservationUtils.getReservationFromOrder(mockOrderLine)).thenReturn(
          mockReservation);

      process.managePrereservedStockLines(jsonRequest, mockOrderLine, idList);

      verify(mockReservationStock, times(1)).setReservation(mockReservation);
      verify(mockReservationStock, times(1)).setOrganization(mockReservation.getOrganization());
      verify(mockReservationStock, times(1)).setSalesOrderLine(mockOrderLine);
      verify(mockReservationStock, times(1)).setReleased(BigDecimal.ZERO);
      verify(mockReservation, times(1)).setRESStatus("CO");
      verify(mockOBDalInstance, times(1)).save(mockOrderLine);
    }
  }
}
