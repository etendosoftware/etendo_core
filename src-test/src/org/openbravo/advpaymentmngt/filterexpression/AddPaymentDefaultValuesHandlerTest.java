package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;

/**
 * Unit tests for the concrete methods of AddPaymentDefaultValuesHandler.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class AddPaymentDefaultValuesHandlerTest {

  private static final String FINANCIAL_ACCOUNT_ID = "FA001";
  private static final String CURRENCY_ID = "CUR001";
  private static final String PAYMENT_ID = "PAY001";

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal mockOBDal;

  private TestableHandler handler;

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    handler = new TestableHandler();
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  // --- getDefaultCurrencyTo ---

  @Test
  public void testGetDefaultCurrencyToNullContext() throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    assertEquals("", handler.getDefaultCurrencyTo(requestMap));
  }

  @Test
  public void testGetDefaultCurrencyToWithFinancialAccount() throws JSONException {
    FIN_FinancialAccount mockAccount = mock(FIN_FinancialAccount.class);
    Currency mockCurrency = mock(Currency.class);
    when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
    when(mockAccount.getCurrency()).thenReturn(mockCurrency);
    when(mockOBDal.get(FIN_FinancialAccount.class, FINANCIAL_ACCOUNT_ID)).thenReturn(mockAccount);

    JSONObject context = new JSONObject();
    context.put("inpfinFinancialAccountId", FINANCIAL_ACCOUNT_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("context", context.toString());

    String result = handler.getDefaultCurrencyTo(requestMap);
    assertEquals(CURRENCY_ID, result);
  }

  // --- getDefaultGeneratedCredit ---

  @Test
  public void testGetDefaultGeneratedCredit() throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    assertEquals("0", handler.getDefaultGeneratedCredit(requestMap));
  }

  // --- getDefaultDocumentCategory ---

  @Test
  public void testGetDefaultDocumentCategorySOTrx() throws JSONException {
    handler.setIsSOTrx("Y");
    Map<String, String> requestMap = new HashMap<>();
    assertEquals("ARR", handler.getDefaultDocumentCategory(requestMap));
  }

  @Test
  public void testGetDefaultDocumentCategoryNotSOTrx() throws JSONException {
    handler.setIsSOTrx("N");
    Map<String, String> requestMap = new HashMap<>();
    assertEquals("APP", handler.getDefaultDocumentCategory(requestMap));
  }

  // --- getDefaultReferenceNo ---

  @Test
  public void testGetDefaultReferenceNoNullContext() throws JSONException {
    Map<String, String> requestMap = new HashMap<>();
    assertEquals("", handler.getDefaultReferenceNo(requestMap));
  }

  @Test
  public void testGetDefaultReferenceNoNoPaymentId() throws JSONException {
    JSONObject context = new JSONObject();

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("context", context.toString());

    assertNull(handler.getDefaultReferenceNo(requestMap));
  }

  @Test
  public void testGetDefaultReferenceNoNullPaymentId() throws JSONException {
    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", JSONObject.NULL);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("context", context.toString());

    assertNull(handler.getDefaultReferenceNo(requestMap));
  }

  @Test
  public void testGetDefaultReferenceNoEmptyPaymentId() throws JSONException {
    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", "");

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("context", context.toString());

    assertNull(handler.getDefaultReferenceNo(requestMap));
  }

  @Test
  public void testGetDefaultReferenceNoWithPayment() throws JSONException {
    FIN_Payment mockPayment = mock(FIN_Payment.class);
    when(mockPayment.getReferenceNo()).thenReturn("REF-123");
    when(mockOBDal.get(FIN_Payment.class, PAYMENT_ID)).thenReturn(mockPayment);

    JSONObject context = new JSONObject();
    context.put("inpfinPaymentId", PAYMENT_ID);

    Map<String, String> requestMap = new HashMap<>();
    requestMap.put("context", context.toString());

    assertEquals("REF-123", handler.getDefaultReferenceNo(requestMap));
  }

  // --- getPendingAmt ---

  @Test
  public void testGetPendingAmtEmptyList() {
    List<FIN_PaymentSchedule> psList = new ArrayList<>();
    assertEquals(BigDecimal.ZERO, handler.getPendingAmt(psList));
  }

  @Test
  public void testGetPendingAmtWithInvoiceSchedule() {
    FIN_PaymentSchedule ps = mock(FIN_PaymentSchedule.class);
    when(ps.getInvoice()).thenReturn(mock(org.openbravo.model.common.invoice.Invoice.class));

    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    when(psd.getPaymentDetails()).thenReturn(null);
    when(psd.getAmount()).thenReturn(new BigDecimal("100.00"));

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(ps.getFINPaymentScheduleDetailInvoicePaymentScheduleList()).thenReturn(psdList);

    List<FIN_PaymentSchedule> psList = new ArrayList<>();
    psList.add(ps);

    assertEquals(new BigDecimal("100.00"), handler.getPendingAmt(psList));
  }

  @Test
  public void testGetPendingAmtWithOrderSchedule() {
    FIN_PaymentSchedule ps = mock(FIN_PaymentSchedule.class);
    when(ps.getInvoice()).thenReturn(null);

    FIN_PaymentScheduleDetail psd = mock(FIN_PaymentScheduleDetail.class);
    when(psd.getPaymentDetails()).thenReturn(null);
    when(psd.getAmount()).thenReturn(new BigDecimal("250.50"));

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psd);
    when(ps.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(psdList);

    List<FIN_PaymentSchedule> psList = new ArrayList<>();
    psList.add(ps);

    assertEquals(new BigDecimal("250.50"), handler.getPendingAmt(psList));
  }

  @Test
  public void testGetPendingAmtSkipsPaidDetails() {
    FIN_PaymentSchedule ps = mock(FIN_PaymentSchedule.class);
    when(ps.getInvoice()).thenReturn(null);

    FIN_PaymentScheduleDetail psdPaid = mock(FIN_PaymentScheduleDetail.class);
    when(psdPaid.getPaymentDetails()).thenReturn(
        mock(org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail.class));

    FIN_PaymentScheduleDetail psdUnpaid = mock(FIN_PaymentScheduleDetail.class);
    when(psdUnpaid.getPaymentDetails()).thenReturn(null);
    when(psdUnpaid.getAmount()).thenReturn(new BigDecimal("75.00"));

    List<FIN_PaymentScheduleDetail> psdList = new ArrayList<>();
    psdList.add(psdPaid);
    psdList.add(psdUnpaid);
    when(ps.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(psdList);

    List<FIN_PaymentSchedule> psList = new ArrayList<>();
    psList.add(ps);

    assertEquals(new BigDecimal("75.00"), handler.getPendingAmt(psList));
  }

  // --- Concrete test subclass ---

  private static class TestableHandler extends AddPaymentDefaultValuesHandler {
    private String isSOTrx = "Y";

    void setIsSOTrx(String value) {
      this.isSOTrx = value;
    }

    @Override
    public String getDefaultExpectedAmount(Map<String, String> requestMap) {
      return "0";
    }

    @Override
    public String getDefaultActualAmount(Map<String, String> requestMap) {
      return "0";
    }

    @Override
    public String getDefaultIsSOTrx(Map<String, String> requestMap) {
      return isSOTrx;
    }

    @Override
    public String getDefaultTransactionType(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultPaymentType(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultOrderType(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultInvoiceType(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultConversionRate(Map<String, String> requestMap) {
      return "1";
    }

    @Override
    public String getDefaultConvertedAmount(Map<String, String> requestMap) {
      return "0";
    }

    @Override
    public String getDefaultReceivedFrom(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultStandardPrecision(Map<String, String> requestMap) {
      return "2";
    }

    @Override
    public String getDefaultCurrency(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getOrganization(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultDocument(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getDefaultPaymentDate(Map<String, String> requestMap) {
      return "";
    }

    @Override
    public String getBankStatementLineAmount(Map<String, String> requestMap) {
      return "0";
    }

    @Override
    protected long getSeq() {
      return 100L;
    }
  }
}
