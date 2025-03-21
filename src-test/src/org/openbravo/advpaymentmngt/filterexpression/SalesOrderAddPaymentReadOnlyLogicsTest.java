package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.ui.Window;

/**
 * Test class for SalesOrderAddPaymentReadOnlyLogics.
 */
@RunWith(MockitoJUnitRunner.class)
public class SalesOrderAddPaymentReadOnlyLogicsTest {

  @InjectMocks
  private SalesOrderAddPaymentReadOnlyLogics logics;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<Preferences> mockedPreferences;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private Window mockWindow;

  private AutoCloseable mocks;
  private Map<String, String> requestMap;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    // Initialize request map
    requestMap = new HashMap<>();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBContext = mockStatic(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    mockedPreferences = mockStatic(Preferences.class);
  }

  /**
   * Cleans up the test environment after each test.
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
    if (mockedPreferences != null) {
      mockedPreferences.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the getSeq method.
   */
  @Test
  public void testGetSeq() {
    // WHEN
    long sequence = logics.getSeq();

    // THEN
    assertEquals("Sequence should be 100", 100L, sequence);
  }

  /**
   * Tests the getPaymentDocumentNoReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentDocumentNoReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Payment document number should not be read-only", result);
  }

  /**
   * Tests the getReceivedFromReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetReceivedFromReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getReceivedFromReadOnlyLogic(requestMap);

    // THEN
    assertTrue("Received from field should be read-only", result);
  }

  /**
   * Tests the getPaymentMethodReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentMethodReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentMethodReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Payment method should not be read-only", result);
  }

  /**
   * Tests the getActualPaymentReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetActualPaymentReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getActualPaymentReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Actual payment should not be read-only", result);
  }

  /**
   * Tests the getPaymentDateReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDateReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getPaymentDateReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Payment date should not be read-only", result);
  }

  /**
   * Tests the getFinancialAccountReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetFinancialAccountReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getFinancialAccountReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Financial account should not be read-only", result);
  }

  /**
   * Tests the getCurrencyReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetCurrencyReadOnlyLogic() throws JSONException {
    // WHEN
    boolean result = logics.getCurrencyReadOnlyLogic(requestMap);

    // THEN
    assertTrue("Currency should be read-only", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method with no context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicNoContext() throws JSONException {
    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Converted amount should not be read-only when no context is provided", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method with context and preference set to YES.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicWithContextPreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.YES);

    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);

    // THEN
    assertTrue("Converted amount should be read-only when preference is YES", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method with context and preference set to NO.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicWithContextPreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.NO);

    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Converted amount should not be read-only when preference is NO", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method with context and PropertyNotFoundException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicWithContextPropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyNotFoundException());

    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Converted amount should not be read-only when property is not found", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method with context and PropertyException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicWithContextPropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyException());

    // WHEN
    boolean result = logics.getConvertedAmountReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Converted amount should not be read-only when property exception occurs", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method with no context.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicNoContext() throws JSONException {
    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Conversion rate should not be read-only when no context is provided", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method with context and preference set to YES.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicWithContextPreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.YES);

    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // THEN
    assertTrue("Conversion rate should be read-only when preference is YES", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method with context and preference set to NO.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicWithContextPreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.NO);

    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Conversion rate should not be read-only when preference is NO", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method with context and PropertyNotFoundException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicWithContextPropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyNotFoundException());

    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Conversion rate should not be read-only when property is not found", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method with context and PropertyException.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicWithContextPropertyException() throws JSONException {
    // GIVEN
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.SALES_ORDER_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.SALES_ORDER_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyException());

    // WHEN
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // THEN
    assertFalse("Conversion rate should not be read-only when property exception occurs", result);
  }
}
