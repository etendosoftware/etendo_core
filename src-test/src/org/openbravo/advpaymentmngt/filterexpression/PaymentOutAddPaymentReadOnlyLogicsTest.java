package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Window;

/**
 * Test cases for the PaymentOutAddPaymentReadOnlyLogics class.
 */
public class PaymentOutAddPaymentReadOnlyLogicsTest extends WeldBaseTest {

  private PaymentOutAddPaymentReadOnlyLogics logics;
  private Map<String, String> requestMap;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private AutoCloseable mocks;

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    logics = new PaymentOutAddPaymentReadOnlyLogics();
    requestMap = new HashMap<>();
  }

  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Test that the sequence value is correctly set to 100
   */
  @Test
  public void testGetSeq() {
    assertEquals("Sequence value should be 100", 100L, logics.getSeq());
  }

  /**
   * Test that the component provider qualifier is correctly set to the payment out window ID
   */
  @Test
  public void testComponentProviderQualifier() throws Exception {
    // Get the annotation value using reflection
    String qualifierValue = PaymentOutAddPaymentReadOnlyLogics.class
        .getAnnotation(org.openbravo.client.kernel.ComponentProvider.Qualifier.class)
        .value();

    assertEquals("Component provider qualifier should match payment out window ID",
        APRMConstants.PAYMENT_OUT_WINDOW_ID, qualifierValue);
  }

  /**
   * Test that all read-only logic methods return true
   */
  @Test
  public void testAllReadOnlyLogicsReturnTrue() throws JSONException {
    assertTrue("Payment document number should be read-only",
        logics.getPaymentDocumentNoReadOnlyLogic(requestMap));

    assertTrue("Received from should be read-only",
        logics.getReceivedFromReadOnlyLogic(requestMap));

    assertTrue("Payment method should be read-only",
        logics.getPaymentMethodReadOnlyLogic(requestMap));

    assertTrue("Actual payment should be read-only",
        logics.getActualPaymentReadOnlyLogic(requestMap));

    assertTrue("Payment date should be read-only",
        logics.getPaymentDateReadOnlyLogic(requestMap));

    assertTrue("Financial account should be read-only",
        logics.getFinancialAccountReadOnlyLogic(requestMap));

    assertTrue("Currency should be read-only",
        logics.getCurrencyReadOnlyLogic(requestMap));
  }

  /**
   * Test the conversion rate read-only logic with NotAllowChangeExchange preference set to Y
   */
  @Test
  public void testConversionRateReadOnlyLogicWithPreferenceY() throws Exception {
    // Setup
    setupMocksForPreferenceTest("Y");

    // Execute
    boolean result = logics.getConversionRateReadOnlyLogic(requestMap);

    // Verify
    assertTrue("Conversion rate should be read-only when NotAllowChangeExchange is Y", result);
  }


  /**
   * Helper method to set up mocks for preference tests
   */
  private void setupMocksForPreferenceTest(String preferenceValue) throws Exception {
    // Create context with window ID
    JSONObject context = new JSONObject();
    context.put("inpwindowId", APRMConstants.PAYMENT_OUT_WINDOW_ID);
    requestMap.put("context", context.toString());

    // Mock OBContext
    mockedOBContext = mockStatic(OBContext.class);
    OBContext mockContext = mock(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);

    // Mock OBDal
    mockedOBDal = mockStatic(OBDal.class);
    OBDal mockOBDal = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Mock Window
    Window mockWindow = mock(Window.class);
    when(mockOBDal.get(Window.class, APRMConstants.PAYMENT_OUT_WINDOW_ID)).thenReturn(mockWindow);

    // Mock Preferences
    MockedStatic<org.openbravo.erpCommon.businessUtility.Preferences> mockedPreferences =
        mockStatic(org.openbravo.erpCommon.businessUtility.Preferences.class);

    mockedPreferences.when(() -> org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
        "NotAllowChangeExchange", true, mockContext.getCurrentClient(),
        mockContext.getCurrentOrganization(), mockContext.getUser(),
        mockContext.getRole(), mockWindow)).thenReturn(preferenceValue);
  }
}