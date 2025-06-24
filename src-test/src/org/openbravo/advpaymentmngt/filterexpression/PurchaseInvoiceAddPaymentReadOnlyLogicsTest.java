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
 * Unit tests for the PurchaseInvoiceAddPaymentReadOnlyLogics class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseInvoiceAddPaymentReadOnlyLogicsTest {

  @InjectMocks
  private PurchaseInvoiceAddPaymentReadOnlyLogics classUnderTest;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<Preferences> mockedPreferences;

  private AutoCloseable mocks;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Window mockWindow;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

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
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
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
    // When
    long result = classUnderTest.getSeq();

    // Then
    assertEquals("Sequence should be 100", 100L, result);
  }

  /**
   * Tests the getPaymentDocumentNoReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDocumentNoReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getPaymentDocumentNoReadOnlyLogic(requestMap);

    // Then
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
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getReceivedFromReadOnlyLogic(requestMap);

    // Then
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
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getPaymentMethodReadOnlyLogic(requestMap);

    // Then
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
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getActualPaymentReadOnlyLogic(requestMap);

    // Then
    assertTrue("Actual payment should be read-only", result);
  }

  /**
   * Tests the getPaymentDateReadOnlyLogic method.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetPaymentDateReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getPaymentDateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Payment date should not be read-only", result);
  }

  /**
   * Tests the getFinancialAccountReadOnlyLogic method.
   * <p>
   * Tests the getConvertedAmountReadOnlyLogic method when preference is YES.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicPreferenceYes() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.YES);

    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);

    // Then
    assertTrue("Converted amount should be read-only when preference is YES", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method when preference is NO.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicPreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.NO);

    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);

    // Then
    assertFalse("Converted amount should not be read-only when preference is NO", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method when property is not found.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicPropertyNotFoundException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyNotFoundException());

    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);

    // Then
    assertFalse("Converted amount should not be read-only when property is not found", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method when a property exception occurs.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicPropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyException());

    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);

    // Then
    assertFalse("Converted amount should not be read-only when property exception occurs", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method when context is null.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicNullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Conversion rate should not be read-only when context is null", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method when preference is YES.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicPreferenceYes() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.YES);

    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);

    // Then
    assertTrue("Conversion rate should be read-only when preference is YES", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method when preference is NO.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicPreferenceNo() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenReturn(Preferences.NO);

    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Conversion rate should not be read-only when preference is NO", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method when property is not found.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetFinancialAccountReadOnlyLogic() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getFinancialAccountReadOnlyLogic(requestMap);

    // Then
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
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getCurrencyReadOnlyLogic(requestMap);

    // Then
    assertTrue("Currency should be read-only", result);
  }

  /**
   * Tests the getConvertedAmountReadOnlyLogic method when context is null.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConvertedAmountReadOnlyLogicNullContext() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();

    // When
    boolean result = classUnderTest.getConvertedAmountReadOnlyLogic(requestMap);

    // Then
    assertFalse("Converted amount should not be read-only when context is null", result);
  }

  /**
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicPropertyNotFoundException() throws JSONException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyNotFoundException());

    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Conversion rate should not be read-only when property is not found", result);
  }

  /**
   * Tests the getConversionRateReadOnlyLogic method when a property exception occurs.
   *
   * @throws JSONException
   *     if a JSON error occurs
   * @throws PropertyNotFoundException
   *     if the property is not found
   * @throws PropertyException
   *     if a property exception occurs
   */
  @Test
  public void testGetConversionRateReadOnlyLogicPropertyException() throws JSONException, PropertyNotFoundException, PropertyException {
    // Given
    Map<String, String> requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(TestConstants.INPWINDOW_ID, APRMConstants.PURCHASE_INVOICE_WINDOW_ID);
    requestMap.put(TestConstants.CONTEXT, context.toString());

    when(mockOBDal.get(Window.class, APRMConstants.PURCHASE_INVOICE_WINDOW_ID)).thenReturn(mockWindow);

    mockedPreferences.when(() -> Preferences.getPreferenceValue(TestConstants.NOT_ALLOW_CHANGE_EXCHANGE, true,
        mockOBContext.getCurrentClient(), mockOBContext.getCurrentOrganization(), mockOBContext.getUser(),
        mockOBContext.getRole(), mockWindow)).thenThrow(new PropertyException());

    // When
    boolean result = classUnderTest.getConversionRateReadOnlyLogic(requestMap);

    // Then
    assertFalse("Conversion rate should not be read-only when property exception occurs", result);
  }
}
