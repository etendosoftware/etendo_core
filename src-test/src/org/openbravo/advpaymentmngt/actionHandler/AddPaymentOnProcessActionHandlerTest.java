package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

public class AddPaymentOnProcessActionHandlerTest extends WeldBaseTest {

  private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
  private static final String FINANCIAL_ACCOUNT_ID = "TEST_FIN_ACC_ID";
  private static final String CURRENCY_ID = "TEST_CURR_ID";
  private static final String BP_NAME = "Test Business Partner";
  private static final String ISO_CODE = "USD";
  private static final BigDecimal WRITEOFF_LIMIT = new BigDecimal("1000");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AddPaymentOnProcessActionHandler actionHandler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<FIN_Utility> mockedFINUtility;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private BusinessPartner mockBusinessPartner;
  @Mock
  private FIN_FinancialAccount mockFinancialAccount;
  @Mock
  private Currency mockCurrency;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);
    actionHandler = new AddPaymentOnProcessActionHandler();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedFINUtility = mockStatic(FIN_Utility.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    // Configure OBDal mock
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    when(mockOBDal.get(eq(FIN_FinancialAccount.class), anyString())).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(eq(BusinessPartner.class), anyString())).thenReturn(mockBusinessPartner);

    // Configure BusinessPartner mock with base data
    setupBusinessPartnerMock();

    // Configure Financial Account mock with base data
    setupFinancialAccountMock();

    // Configure Currency mock with base data
    setupCurrencyMock();

    // Configure default FIN_Utility behavior
    mockedFINUtility.when(() -> FIN_Utility.isBlockedBusinessPartner(anyString(), anyBoolean(), anyInt()))
        .thenReturn(false);
  }

  private void setupBusinessPartnerMock() {
    when(mockBusinessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
    when(mockBusinessPartner.getName()).thenReturn(BP_NAME);
    when(mockBusinessPartner.getCurrency()).thenReturn(mockCurrency);
  }

  private void setupFinancialAccountMock() {
    when(mockFinancialAccount.getId()).thenReturn(FINANCIAL_ACCOUNT_ID);
    when(mockFinancialAccount.getWriteofflimit()).thenReturn(WRITEOFF_LIMIT);
  }

  private void setupCurrencyMock() {
    when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
    when(mockCurrency.getISOCode()).thenReturn(ISO_CODE);
  }

  @After
  public void tearDown() {
    mockedOBDal.close();
    mockedOBContext.close();
    mockedFINUtility.close();
    mockedOBMessageUtils.close();
  }

  @Test
  public void testExecute_ValidPayment_Success() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("success", message.getString("severity"));
    assertEquals("Ok", message.getString("text"));
  }

  @Test
  public void testExecute_BlockedBusinessPartner_ReturnsError() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Configure mocks for blocked BP scenario
    mockedFINUtility.when(() -> FIN_Utility.isBlockedBusinessPartner(eq(BUSINESS_PARTNER_ID), eq(true), eq(4)))
        .thenReturn(true);
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@ThebusinessPartner@"))
        .thenReturn("The Business Partner");
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@BusinessPartnerBlocked@"))
        .thenReturn("is blocked");

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertTrue(message.getString("text").contains(BP_NAME));
    assertTrue(message.getString("text").contains("is blocked"));
  }


  @Test
  public void testExecute_BusinessPartnerWithoutCurrency_ReturnsError() throws Exception {
    // Prepare test data
    Map<String, Object> parameters = new HashMap<>();
    String jsonData = createJsonData(false, false);

    // Configure BP without currency
    when(mockBusinessPartner.getCurrency()).thenReturn(null);

    // Configure message mock
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("InitBPCurrencyLnk", false))
        .thenReturn("Business Partner %s (%s) does not have a currency");

    // Execute
    JSONObject result = actionHandler.execute(parameters, jsonData);

    // Verify
    assertTrue(result.has("message"));
    JSONObject message = result.getJSONObject("message");
    assertEquals("error", message.getString("severity"));
    assertTrue(message.getString("text").contains("Business Partner"));
  }

  private String createJsonData(boolean usesCredit, boolean generatesCredit) {
    return createJsonData(usesCredit, generatesCredit, CURRENCY_ID);
  }

  private String createJsonData(boolean usesCredit, boolean generatesCredit, String currencyId) {
    return "{" +
        "\"issotrx\": \"true\"," +
        "\"finFinancialAccount\": \"" + FINANCIAL_ACCOUNT_ID + "\"," +
        "\"receivedFrom\": \"" + BUSINESS_PARTNER_ID + "\"," +
        "\"currencyId\": \"" + currencyId + "\"," +
        "\"usesCredit\": " + usesCredit + "," +
        "\"generatesCredit\": " + generatesCredit +
        "}";
  }
}