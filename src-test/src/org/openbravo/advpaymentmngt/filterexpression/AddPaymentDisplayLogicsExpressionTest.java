package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.erpCommon.utility.OBMessageUtils;

/**
 * Test class for the AddPaymentDisplayLogicsExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddPaymentDisplayLogicsExpressionTest {

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private Instance<AddPaymentDisplayLogicsHandler> addPaymentFilterExpressionHandlers;

  @Mock
  private AddPaymentDisplayLogicsHandler mockHandler;

  @InjectMocks
  private AddPaymentDisplayLogicsExpression expressionUnderTest;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // No need to do anything with setAdminMode since we are mocking it
    mockedOBContext.when(() -> OBContext.setAdminMode(true)).then(invocation -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).then(invocation -> null);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is Organization.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionOrganizationTrue() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap(TestConstants.AD_ORG_ID_DISPLAY_LOGIC, TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return true for organization display logic
    when(mockHandler.getOrganizationDisplayLogic(requestMap)).thenReturn(true);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("Y", result);
    verify(mockHandler).getOrganizationDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is Organization but the logic returns false.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionOrganizationFalse() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap(TestConstants.AD_ORG_ID_DISPLAY_LOGIC, TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return false for organization display logic
    when(mockHandler.getOrganizationDisplayLogic(requestMap)).thenReturn(false);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("N", result);
    verify(mockHandler).getOrganizationDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is Document.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionDocumentTrue() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap("trxtype_display_logic", TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return true for document display logic
    when(mockHandler.getDocumentDisplayLogic(requestMap)).thenReturn(true);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("Y", result);
    verify(mockHandler).getDocumentDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is CreditToUse.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionCreditToUseTrue() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap("credit_to_use_display_logic", TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return true for credit to use display logic
    when(mockHandler.getCreditToUseDisplayLogic(requestMap)).thenReturn(true);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("Y", result);
    verify(mockHandler).getCreditToUseDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is OverpaymentAction.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionOverpaymentActionTrue() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap("overpayment_action_display_logic", TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return true for overpayment action display logic
    when(mockHandler.getOverpaymentActionDisplayLogic(requestMap)).thenReturn(true);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("Y", result);
    verify(mockHandler).getOverpaymentActionDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the window ID has a matching handler
   * and the parameter is BankStatementLine.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionBankStatementLineTrue() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap("bslamount_display_logic", TestConstants.TEST_WINDOW);

    // Set up the mock to return a handler for the window ID
    mockHandlerSelection();

    // Prepare the handler to return true for bank statement line display logic
    when(mockHandler.getBankStatementLineDisplayLogic(requestMap)).thenReturn(true);

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertEquals("Y", result);
    verify(mockHandler).getBankStatementLineDisplayLogic(requestMap);
  }

  /**
   * Test for getExpression method when the handler throws an exception.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetExpressionHandlerException() throws Exception {
    // GIVEN
    Map<String, String> requestMap = createRequestMap(TestConstants.AD_ORG_ID_DISPLAY_LOGIC, TestConstants.TEST_WINDOW);

    mockHandlerSelection();

    when(mockHandler.getOrganizationDisplayLogic(requestMap)).thenThrow(new RuntimeException("Test exception"));

    // WHEN
    String result = expressionUnderTest.getExpression(requestMap);

    // THEN
    assertNull(result);
  }

  /**
   * Creates a request map with a current parameter and window ID in context.
   *
   * @param currentParam
   *     the current parameter
   * @param windowId
   *     the window ID
   * @return the request map
   * @throws Exception
   *     if an error occurs during the creation of the request map
   */
  private Map<String, String> createRequestMap(String currentParam, String windowId) throws Exception {
    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("currentParam", currentParam);

    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, windowId);
    requestMap.put("context", context.toString());

    return requestMap;
  }

  /**
   * Sets up the mock to return a handler for the given window ID.
   */
  @SuppressWarnings("unchecked")
  private void mockHandlerSelection() {
    // Create instance with our mockHandler
    Instance<AddPaymentDisplayLogicsHandler> mockInstance = mock(Instance.class);
    when(mockInstance.iterator()).thenReturn(java.util.Collections.singletonList(mockHandler).iterator());

    // Return our mockInstance when select is called with the right selector
    when(addPaymentFilterExpressionHandlers.select(any(ComponentProvider.Selector.class))).thenReturn(mockInstance);

  }
}
