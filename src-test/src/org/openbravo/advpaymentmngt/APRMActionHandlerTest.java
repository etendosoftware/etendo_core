package org.openbravo.advpaymentmngt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

/**
 * Test class for the APRMActionHandler.
 * This class contains unit tests for the APRMActionHandler class.
 */
@RunWith(MockitoJUnitRunner.class)
public class APRMActionHandlerTest {

  private static final String FINANCIAL_ACCOUNT_ID = "test-fin-account-id";
  private MockedStatic<OBDal> mockedOBDal;

  @Mock
  private OBDal mockDal;

  @Mock
  private FIN_FinancialAccount mockFinAccount;

  @Mock
  private FinAccPaymentMethod mockPaymentMethod1;

  @Mock
  private FinAccPaymentMethod mockPaymentMethod2;

  @Captor
  private ArgumentCaptor<String> clearingUseCaptor;

  private APRMActionHandler handler;
  private Map<String, Object> parameters;

  /**
   * Sets up the test environment before each test.
   * Initializes mocks and configures default mock behavior.
   */
  @Before
  public void setUp() {
    // Initialize mocked static OBDal
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

    // Set up financial account mock
    when(mockDal.get(FIN_FinancialAccount.class, FINANCIAL_ACCOUNT_ID)).thenReturn(mockFinAccount);

    // Create list of payment methods
    List<FinAccPaymentMethod> paymentMethods = new ArrayList<>();
    paymentMethods.add(mockPaymentMethod1);
    paymentMethods.add(mockPaymentMethod2);

    // Set up financial account to return payment methods
    when(mockFinAccount.getFinancialMgmtFinAccPaymentMethodList()).thenReturn(paymentMethods);

    handler = new APRMActionHandler();
    parameters = new HashMap<>();
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mock resources.
   */
  @After
  public void tearDown() {
    mockedOBDal.close();
  }

  /**
   * Tests the updatePaymentMethodConfiguration method.
   * Verifies that payment methods are updated correctly.
   */
  @Test
  public void testUpdatePaymentMethodConfiguration() {
    // Execute method under test
    handler.updatePaymentMethodConfiguration(FINANCIAL_ACCOUNT_ID);

    // Verify payment methods were updated correctly
    verify(mockPaymentMethod1).setOUTUponClearingUse(clearingUseCaptor.capture());
    assertEquals("CLE", clearingUseCaptor.getValue());

    verify(mockPaymentMethod1).setINUponClearingUse(clearingUseCaptor.capture());
    assertEquals("CLE", clearingUseCaptor.getValue());

    verify(mockPaymentMethod2).setOUTUponClearingUse("CLE");
    verify(mockPaymentMethod2).setINUponClearingUse("CLE");

    // Verify save order
    InOrder inOrder = inOrder(mockDal, mockPaymentMethod1, mockPaymentMethod2);
    inOrder.verify(mockDal).save(mockPaymentMethod1);
    inOrder.verify(mockDal).save(mockPaymentMethod2);
    inOrder.verify(mockDal).flush();
  }

  /**
   * Tests the updatePaymentMethodConfiguration method with an empty list of payment methods.
   * Verifies that no payment methods are saved.
   */
  @Test
  public void testUpdatePaymentMethodConfigurationEmptyPaymentMethods() {
    when(mockFinAccount.getFinancialMgmtFinAccPaymentMethodList()).thenReturn(new ArrayList<>());

    handler.updatePaymentMethodConfiguration(FINANCIAL_ACCOUNT_ID);

    verify(mockDal, never()).save(any(FinAccPaymentMethod.class));
    verify(mockDal).flush();
  }

  /**
   * Tests the execute method for the bankTransitoryCalloutResponse event.
   * Verifies that payment methods are updated correctly.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testExecuteBankTransitoryCallout() throws Exception {
    String content = new JSONObject().put("eventType", "bankTransitoryCalloutResponse").put("financialAccountId",
        FINANCIAL_ACCOUNT_ID).toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockPaymentMethod1).setOUTUponClearingUse("CLE");
    verify(mockPaymentMethod1).setINUponClearingUse("CLE");
    verify(mockDal).flush();
  }

  /**
   * Tests the execute method with an unsupported event type.
   * Verifies that no payment methods are updated.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testExecuteUnsupportedEventType() throws Exception {
    String content = new JSONObject().put("eventType", "unsupportedEvent").put("financialAccountId",
        FINANCIAL_ACCOUNT_ID).toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

  /**
   * Tests the execute method with invalid JSON content.
   * Verifies that no payment methods are updated.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testExecuteInvalidJson() throws Exception {
    String invalidJson = "{ invalid json content }";

    JSONObject result = handler.execute(parameters, invalidJson);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

  /**
   * Tests the execute method with missing financial account ID.
   * Verifies that no payment methods are updated.
   *
   * @throws Exception
   *     if an error occurs during test execution
   */
  @Test
  public void testExecuteMissingFinancialAccountId() throws Exception {
    String content = new JSONObject().put("eventType", "bankTransitoryCalloutResponse").toString();

    JSONObject result = handler.execute(parameters, content);

    assertNotNull(result);
    verify(mockDal, never()).save(any());
    verify(mockDal, never()).flush();
  }

}
