package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.Parameter;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Language;
import org.openbravo.model.ad.ui.Element;
import org.openbravo.model.ad.ui.ElementTrl;

/**
 * Unit tests for the AddPaymentReloadLabelsActionHandler class.
 */
public class AddPaymentReloadLabelsActionHandlerTest {

  /**
   * Rule to define and verify expected exceptions in test cases.
   * Ensures that tests can specify the type and message of expected exceptions.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddPaymentReloadLabelsActionHandler actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private Language mockLanguage;
  @Mock
  private Parameter mockBusinessPartnerParam;
  @Mock
  private Parameter mockFinancialAccountParam;
  @Mock
  private Element mockBusinessPartnerElement;
  @Mock
  private Element mockFinancialAccountElement;
  @Mock
  private OBCriteria<ElementTrl> mockElementTrlCriteria;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddPaymentReloadLabelsActionHandler();

    // Setup static mocks
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);

    // Configure OBDal mock
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Configure OBContext mock
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    when(mockOBContext.getLanguage()).thenReturn(mockLanguage);
    when(mockLanguage.getLanguage()).thenReturn("en_US");
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
  }

  /**
   * Tests the execute method with default language.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteDefaultLanguageSuccess() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TestConstants.BUSINESS_PARTNER, TestConstants.BP_ID);
    parameters.put(TestConstants.FINANCIAL_ACCOUNT, TestConstants.FA_ID);
    parameters.put("issotrx", "true");

    // Mock Parameter retrieval
    when(mockOBDal.get(eq(Parameter.class), eq(TestConstants.BP_ID))).thenReturn(mockBusinessPartnerParam);
    when(mockOBDal.get(eq(Parameter.class), eq(TestConstants.FA_ID))).thenReturn(mockFinancialAccountParam);

    // Mock Element retrieval
    when(mockBusinessPartnerParam.getApplicationElement()).thenReturn(mockBusinessPartnerElement);
    when(mockFinancialAccountParam.getApplicationElement()).thenReturn(mockFinancialAccountElement);

    // Set label properties
    when(mockBusinessPartnerElement.get(Element.PROPERTY_NAME)).thenReturn(TestConstants.BUSINESS_PARTNER_LABEL);
    when(mockFinancialAccountElement.get(Element.PROPERTY_NAME)).thenReturn(TestConstants.FINANCIAL_ACCOUNT_LABEL);

    // When
    JSONObject result = actionHandler.execute(parameters, null);

    // Then
    assertNotNull(result);
    assertTrue(result.has(TestConstants.VALUES));
    JSONObject values = result.getJSONObject(TestConstants.VALUES);
    assertEquals(TestConstants.BUSINESS_PARTNER_LABEL, values.getString(TestConstants.BUSINESS_PARTNER));
    assertEquals(TestConstants.FINANCIAL_ACCOUNT_LABEL, values.getString(TestConstants.FINANCIAL_ACCOUNT));
  }

  /**
   * Tests the execute method with non-English language.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteNonEnglishLanguageSuccess() throws Exception {
    // Given
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(TestConstants.BUSINESS_PARTNER, TestConstants.BP_ID);
    parameters.put(TestConstants.FINANCIAL_ACCOUNT, TestConstants.FA_ID);
    parameters.put("issotrx", "false");

    // Mock Language
    when(mockLanguage.getLanguage()).thenReturn("es_ES");

    // Mock Parameter retrieval
    when(mockOBDal.get(eq(Parameter.class), eq(TestConstants.BP_ID))).thenReturn(mockBusinessPartnerParam);
    when(mockOBDal.get(eq(Parameter.class), eq(TestConstants.FA_ID))).thenReturn(mockFinancialAccountParam);

    // Mock Element retrieval
    when(mockBusinessPartnerParam.getApplicationElement()).thenReturn(mockBusinessPartnerElement);
    when(mockFinancialAccountParam.getApplicationElement()).thenReturn(mockFinancialAccountElement);

    // Set label properties
    when(mockBusinessPartnerElement.get(Element.PROPERTY_PURCHASEORDERNAME)).thenReturn(
        TestConstants.BUSINESS_PARTNER_LABEL);
    when(mockFinancialAccountElement.get(Element.PROPERTY_PURCHASEORDERNAME)).thenReturn(
        TestConstants.FINANCIAL_ACCOUNT_LABEL);

    // Mock ElementTrl criteria
    when(mockOBDal.createCriteria(ElementTrl.class)).thenReturn(mockElementTrlCriteria);
    when(mockElementTrlCriteria.uniqueResult()).thenReturn(null);

    // When
    JSONObject result = actionHandler.execute(parameters, null);

    // Then
    assertNotNull(result);
    assertTrue(result.has(TestConstants.VALUES));
    JSONObject values = result.getJSONObject(TestConstants.VALUES);
    assertEquals(TestConstants.BUSINESS_PARTNER_LABEL, values.getString(TestConstants.BUSINESS_PARTNER));
    assertEquals(TestConstants.FINANCIAL_ACCOUNT_LABEL, values.getString(TestConstants.FINANCIAL_ACCOUNT));
  }
}
