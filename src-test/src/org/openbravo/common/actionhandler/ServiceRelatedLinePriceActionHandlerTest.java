package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ServicePriceUtils;
import org.openbravo.model.common.order.OrderLine;

/**
 * Unit tests for the {@link ServiceRelatedLinePriceActionHandler} class.
 * Verifies the behavior of the `execute` method under various scenarios,
 * including successful execution, error handling, and deferred sale processing.
 */
public class ServiceRelatedLinePriceActionHandlerTest {

  private static final String RFC_ORDERLINE_TAB_ID = "AF4090093D471431E040007F010048A5";

  private ServiceRelatedLinePriceActionHandler actionHandler;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<ServicePriceUtils> mockedServicePriceUtils;
  private MockedStatic<RequestContext> mockedRequestContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private OrderLine mockServiceOrderLine;

  @Mock
  private OrderLine mockOrderLineToRelate;

  @Mock
  private RequestContext mockRequestContext;

  @Mock
  private OBContext mockOBContext;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes required dependencies.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    actionHandler = new ServiceRelatedLinePriceActionHandler();

    setupMocks();
  }

  /**
   * Sets up the mocked static methods and dependencies required for testing.
   * Initialize mocks for OBDal, OBContext, ServicePriceUtils, RequestContext, and OBMessageUtils.
   * Configures behavior for mocked instances to simulate the expected interactions.
   */
  private void setupMocks() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedServicePriceUtils = mockStatic(ServicePriceUtils.class);
    mockedRequestContext = mockStatic(RequestContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    OBDal mockOBDalInstance = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDalInstance);
    when(mockOBDalInstance.get(OrderLine.class, "testOrderLineId")).thenReturn(mockServiceOrderLine);
    when(mockOBDalInstance.get(OrderLine.class, ActionHandlerTestConstants.ORDER_LINE_TO_RELATE_ID)).thenReturn(mockOrderLineToRelate);

    mockedRequestContext.when(RequestContext::get).thenReturn(mockRequestContext);

    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    org.openbravo.model.ad.system.Language mockLanguage = mock(org.openbravo.model.ad.system.Language.class);
    when(mockLanguage.getLanguage()).thenReturn("en_US");
    when(mockOBContext.getLanguage()).thenReturn(mockLanguage);

    VariablesSecureApp mockVars = mock(VariablesSecureApp.class);
    when(mockRequestContext.getVariablesSecureApp()).thenReturn(mockVars);

    mockedServicePriceUtils.when(
        () -> ServicePriceUtils.getServiceAmount(any(), any(), any(), any(), any(), any(), any())).thenReturn(
        new BigDecimal(ActionHandlerTestConstants.DEFAULT_AMOUNT));

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(any(), any(), any(), any())).thenReturn(
        "Error translation");
  }

  /**
   * Creates a valid JSON object for testing the `execute` method.
   * Includes required fields such as order line ID, amount, and related information.
   *
   * @param amount
   *     the amount to include in the JSON object
   * @param includeState
   *     whether to include the state parameter
   * @param tabId
   *     the tab ID to include in the JSON object
   * @return a JSON object containing valid test data
   * @throws Exception
   *     if an error occurs while creating the JSON object
   */
  private JSONObject createParameters(String amount, boolean includeState, String tabId) throws Exception {
    JSONObject parameters = new JSONObject();
    parameters.put("orderlineId", "testOrderLineId");
    parameters.put(ActionHandlerTestConstants.AMOUNT, amount);
    parameters.put("discounts", "10.00");
    parameters.put("priceamount", "90.00");
    parameters.put("relatedqty", "2.00");
    parameters.put("unitdiscountsamt", "5.00");
    parameters.put(ActionHandlerTestConstants.ORDER_LINE_TO_RELATE_ID, ActionHandlerTestConstants.ORDER_LINE_TO_RELATE_ID);
    parameters.put("relatedLinesInfo", new JSONObject());
    if (includeState) {
      parameters.put("state", true);
    }
    parameters.put("tabId", tabId);
    return parameters;
  }

  /**
   * Tests the `execute` method for a successful scenario.
   * Verifies that the method processes the input parameters correctly
   * and returns the expected result.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteHappyPath() throws Exception {
    JSONObject parameters = createParameters(ActionHandlerTestConstants.PRODUCT_PRICE, false, ActionHandlerTestConstants.NON_RFC_ID);

    JSONObject result = actionHandler.execute(Collections.emptyMap(), parameters.toString());

    assertNotNull(result);
    assertEquals(new BigDecimal(ActionHandlerTestConstants.DEFAULT_AMOUNT), result.get(ActionHandlerTestConstants.AMOUNT));

    mockedServicePriceUtils.verify(
        () -> ServicePriceUtils.getServiceAmount(eq(mockServiceOrderLine), eq(new BigDecimal(ActionHandlerTestConstants.PRODUCT_PRICE)),
            eq(new BigDecimal("10.00")), eq(new BigDecimal("90.00")), eq(new BigDecimal("2.00")),
            eq(new BigDecimal("5.00")), any(JSONObject.class)));
  }

  /**
   * Tests the `execute` method when an exception occurs.
   * Verifies that the method handles the exception and returns an appropriate error response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithException() throws Exception {
    JSONObject parameters = createParameters("invalidAmount", false, ActionHandlerTestConstants.NON_RFC_ID);

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(any(), any(), any(), any())).thenReturn(
        "Error parsing number");

    JSONObject result = actionHandler.execute(Collections.emptyMap(), parameters.toString());

    assertNotNull(result);
    JSONObject message = result.getJSONObject(ActionHandlerTestConstants.MESSAGE);
    assertEquals("error", message.getString("severity"));
    assertEquals("Error", message.getString("title"));
    assertTrue(message.getString("text").contains("Error parsing number"));
  }

  /**
   * Tests the `execute` method with deferred sale processing.
   * Verifies that the method includes deferred sale information in the response.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithDeferredSale() throws Exception {
    JSONObject parameters = createParameters(ActionHandlerTestConstants.PRODUCT_PRICE, true, ActionHandlerTestConstants.NON_RFC_ID);

    JSONObject deferredSale = new JSONObject();
    deferredSale.put("deferred", true);
    mockedServicePriceUtils.when(
        () -> ServicePriceUtils.deferredSaleAllowed(eq(mockServiceOrderLine), eq(mockOrderLineToRelate))).thenReturn(
        deferredSale);

    JSONObject result = actionHandler.execute(Collections.emptyMap(), parameters.toString());

    assertNotNull(result);
    assertEquals(new BigDecimal(ActionHandlerTestConstants.DEFAULT_AMOUNT), result.get(ActionHandlerTestConstants.AMOUNT));
    assertTrue(result.getJSONObject(ActionHandlerTestConstants.MESSAGE).has("deferred"));

    mockedServicePriceUtils.verify(
        () -> ServicePriceUtils.deferredSaleAllowed(eq(mockServiceOrderLine), eq(mockOrderLineToRelate)));
  }

  /**
   * Tests the `execute` method with the RFC tab ID.
   * Verifies that the method skips deferred sale processing and returns the expected result.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithRFCTabId() throws Exception {
    JSONObject parameters = createParameters(ActionHandlerTestConstants.PRODUCT_PRICE, true, RFC_ORDERLINE_TAB_ID);

    JSONObject result = actionHandler.execute(Collections.emptyMap(), parameters.toString());

    assertNotNull(result);
    assertEquals(new BigDecimal(ActionHandlerTestConstants.DEFAULT_AMOUNT), result.get(ActionHandlerTestConstants.AMOUNT));
    assertFalse(result.has(ActionHandlerTestConstants.MESSAGE) && result.get(ActionHandlerTestConstants.MESSAGE) != JSONObject.NULL);

    mockedServicePriceUtils.verify(() -> ServicePriceUtils.deferredSaleAllowed(any(), any()), never());
  }

  /**
   * Tests the `execute` method with a null state parameter.
   * Verifies that the method processes the input correctly and excludes the message field.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteWithNullState() throws Exception {
    JSONObject parameters = createParameters(ActionHandlerTestConstants.PRODUCT_PRICE, false, ActionHandlerTestConstants.NON_RFC_ID);
    parameters.put("state", JSONObject.NULL);

    JSONObject result = actionHandler.execute(Collections.emptyMap(), parameters.toString());

    assertNotNull(result);
    assertEquals(new BigDecimal(ActionHandlerTestConstants.DEFAULT_AMOUNT), result.get(ActionHandlerTestConstants.AMOUNT));
    assertFalse(result.has(ActionHandlerTestConstants.MESSAGE) && result.get(ActionHandlerTestConstants.MESSAGE) != JSONObject.NULL);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedServicePriceUtils != null) mockedServicePriceUtils.close();
    if (mockedRequestContext != null) mockedRequestContext.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
  }
}
