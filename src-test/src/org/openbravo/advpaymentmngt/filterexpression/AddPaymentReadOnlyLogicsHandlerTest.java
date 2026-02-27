package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyNotFoundException;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the concrete methods of AddPaymentReadOnlyLogicsHandler.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddPaymentReadOnlyLogicsHandlerTest {

  private static final String NOT_ALLOW_CHANGE_EXCHANGE = "NotAllowChangeExchange";
  private static final String INPWINDOW_ID = "inpwindowId";
  private static final String CONTEXT = "context";

  private static final String WINDOW_ID = "WIN001";

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<Preferences> preferencesStatic;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrg;

  @Mock
  private User mockUser;

  @Mock
  private Role mockRole;

  @Mock
  private Window mockWindow;

  private TestableHandler handler;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);

    preferencesStatic = mockStatic(Preferences.class);

    lenient().when(mockOBContext.getCurrentClient()).thenReturn(mockClient);
    lenient().when(mockOBContext.getCurrentOrganization()).thenReturn(mockOrg);
    lenient().when(mockOBContext.getUser()).thenReturn(mockUser);
    lenient().when(mockOBContext.getRole()).thenReturn(mockRole);
    lenient().when(mockOBDal.get(eq(Window.class), anyString())).thenReturn(mockWindow);

    handler = new TestableHandler();
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (preferencesStatic != null) {
      preferencesStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  // --- getConvertedAmountReadOnlyLogic ---
  /**
   * Get converted amount read only logic null context.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConvertedAmountReadOnlyLogicNullContext() throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    assertFalse(handler.getConvertedAmountReadOnlyLogic(requestMap));
  }
  /**
   * Get converted amount read only logic preference yes.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConvertedAmountReadOnlyLogicPreferenceYes() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenReturn("Y");

    JSONObject context = new JSONObject();
    context.put(INPWINDOW_ID, WINDOW_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertTrue(handler.getConvertedAmountReadOnlyLogic(requestMap));
  }
  /**
   * Get converted amount read only logic preference no.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConvertedAmountReadOnlyLogicPreferenceNo() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenReturn("N");

    JSONObject context = new JSONObject();
    context.put(INPWINDOW_ID, WINDOW_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertFalse(handler.getConvertedAmountReadOnlyLogic(requestMap));
  }
  /**
   * Get converted amount read only logic property not found.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConvertedAmountReadOnlyLogicPropertyNotFound() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenThrow(new PropertyNotFoundException());

    JSONObject context = new JSONObject();
    context.put(INPWINDOW_ID, WINDOW_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertFalse(handler.getConvertedAmountReadOnlyLogic(requestMap));
  }

  // --- getConversionRateReadOnlyLogic ---
  /**
   * Get conversion rate read only logic null context.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConversionRateReadOnlyLogicNullContext() throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    assertFalse(handler.getConversionRateReadOnlyLogic(requestMap));
  }
  /**
   * Get conversion rate read only logic preference yes.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConversionRateReadOnlyLogicPreferenceYes() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenReturn("Y");

    JSONObject context = new JSONObject();
    context.put(INPWINDOW_ID, WINDOW_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertTrue(handler.getConversionRateReadOnlyLogic(requestMap));
  }
  /**
   * Get conversion rate read only logic preference no.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConversionRateReadOnlyLogicPreferenceNo() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenReturn("N");

    JSONObject context = new JSONObject();
    context.put(INPWINDOW_ID, WINDOW_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertFalse(handler.getConversionRateReadOnlyLogic(requestMap));
  }
  /**
   * Get conversion rate read only logic no window id.
   * @throws JSONException if an error occurs
   */

  @Test
  public void testGetConversionRateReadOnlyLogicNoWindowId() throws JSONException {
    preferencesStatic.when(() -> Preferences.getPreferenceValue(
        eq(NOT_ALLOW_CHANGE_EXCHANGE), anyBoolean(), any(), any(), any(), any(), any(Window.class)))
        .thenThrow(new PropertyNotFoundException());

    JSONObject context = new JSONObject();

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put(CONTEXT, context.toString());

    assertFalse(handler.getConversionRateReadOnlyLogic(requestMap));
  }

  // --- Concrete test subclass ---

  private static class TestableHandler extends AddPaymentReadOnlyLogicsHandler {
    /** Get payment document no read only logic. */
    @Override
    public boolean getPaymentDocumentNoReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get received from read only logic. */

    @Override
    public boolean getReceivedFromReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get payment method read only logic. */

    @Override
    public boolean getPaymentMethodReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get actual payment read only logic. */

    @Override
    public boolean getActualPaymentReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get payment date read only logic. */

    @Override
    public boolean getPaymentDateReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get financial account read only logic. */

    @Override
    public boolean getFinancialAccountReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }
    /** Get currency read only logic. */

    @Override
    public boolean getCurrencyReadOnlyLogic(Map<String, String> requestMap) {
      return false;
    }

    @Override
    protected long getSeq() {
      return 100L;
    }
  }
}
