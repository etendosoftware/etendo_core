package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

/**
 * Unit tests for the AddTransactionOnChangePaymentActionHandler class.
 */
public class AddTransactionOnChangePaymentActionHandlerTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private AddTransactionOnChangePaymentActionHandler actionHandler;
  @Mock
  private FIN_Payment mockPayment;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private OBDal mockOBDal;
  private MockedStatic<OBDal> obDalMock;
  private MockedStatic<OBContext> obContextMock;
  private MockedStatic<FIN_Utility> finUtilityMock;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddTransactionOnChangePaymentActionHandler();

    // Setup static mocks
    obDalMock = mockStatic(OBDal.class);
    obContextMock = mockStatic(OBContext.class);
    finUtilityMock = mockStatic(FIN_Utility.class);

    // Configure OBDal mock
    obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (obDalMock != null) obDalMock.close();
    if (obContextMock != null) obContextMock.close();
    if (finUtilityMock != null) finUtilityMock.close();
  }

  /**
   * Tests the execute method when the payment ID is null.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteNullPaymentId() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String data = "{\"strDescription\":\"Test Description\", \"strPaymentId\":null}";

    // WHEN
    finUtilityMock.when(() -> FIN_Utility.getFinAccTransactionDescription("Test Description", "", "")).thenReturn(
        "Processed Description");

    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertEquals("Processed Description", result.getString("description"));
    assertEquals(BigDecimal.ZERO, result.get("depositamt"));
    assertEquals(BigDecimal.ZERO, result.get("paymentamt"));
  }

  /**
   * Tests the execute method when the payment is a receipt.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testExecuteReceiptPayment() throws Exception {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String paymentId = "testPaymentId";
    String data = "{\"strDescription\":\"Test Description\", \"strPaymentId\":\"" + paymentId + "\"}";

    // Configure mock payment
    when(mockOBDal.get(FIN_Payment.class, paymentId)).thenReturn(mockPayment);
    when(mockPayment.isReceipt()).thenReturn(true);
    when(mockPayment.getAmount()).thenReturn(BigDecimal.TEN);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(BigDecimal.TEN);
    when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
    when(mockBusinessPartner.getId()).thenReturn("testBPartnerId");
    when(mockPayment.getDescription()).thenReturn("Payment Description");

    // Configure utility mock
    finUtilityMock.when(
        () -> FIN_Utility.getFinAccTransactionDescription("Test Description", "", "Payment Description")).thenReturn(
        "Processed Description");

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    assertEquals(BigDecimal.TEN, result.get("depositamt"));
    assertEquals(BigDecimal.ZERO, result.get("paymentamt"));
    assertEquals("testBPartnerId", result.getString("cBpartnerId"));
    assertEquals("Processed Description", result.getString("description"));
  }

  /**
   * Tests the execute method to ensure proper handling of invalid JSON input.
   * Verifies that no exception is thrown and an empty result is returned.
   */
  @Test
  public void testExecuteExceptionHandling() {
    // GIVEN
    Map<String, Object> parameters = new HashMap<>();
    String data = "invalid json";

    // WHEN
    JSONObject result = actionHandler.execute(parameters, data);

    // THEN
    // Verify that no exception is thrown and an empty result is returned
    assertEquals(0, result.length());
  }

}
