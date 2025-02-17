package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

@RunWith(MockitoJUnitRunner.class)
public class PaymentInAddPaymentDefaultValuesTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private PaymentInAddPaymentDefaultValues classUnderTest;

    @Mock
    private OBDal mockOBDal;

    @Mock
    private FIN_Payment mockPayment;

    @Mock
    private Currency mockCurrency;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Organization mockOrganization;

    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBDateUtils> mockedOBDateUtils;
    private AutoCloseable mocks;

    private Map<String, String> requestMap;
    private JSONObject context;
    private static final String PAYMENT_ID = "TEST_PAYMENT_ID";
    private static final String CURRENCY_ID = "TEST_CURRENCY_ID";
    private static final String BP_ID = "TEST_BP_ID";
    private static final String ORG_ID = "TEST_ORG_ID";
    private static final String DOCUMENT_NO = "P-0001";
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal CONVERSION_RATE = new BigDecimal("1.25");
    private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("125.00");
    private static final Integer STANDARD_PRECISION = 2;
    private static final Date PAYMENT_DATE = new Date();
    private static final String FORMATTED_DATE = "2023-01-01";

    private Method getPaymentMethod;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);

        getPaymentMethod = PaymentInAddPaymentDefaultValues.class.getDeclaredMethod("getPayment", Map.class);
        getPaymentMethod.setAccessible(true);

        // Setup static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        mockedOBDateUtils = mockStatic(OBDateUtils.class);
        mockedOBDateUtils.when(() -> OBDateUtils.formatDate(PAYMENT_DATE)).thenReturn(FORMATTED_DATE);

        // Setup request map with context
        requestMap = new HashMap<>();
        context = new JSONObject();
        context.put("inpfinPaymentId", PAYMENT_ID);
        requestMap.put("context", context.toString());

        // Setup mock payment
        when(mockOBDal.get(FIN_Payment.class, PAYMENT_ID)).thenReturn(mockPayment);
        when(mockPayment.getAmount()).thenReturn(PAYMENT_AMOUNT);
        when(mockPayment.getDocumentNo()).thenReturn(DOCUMENT_NO);
        when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(CONVERSION_RATE);
        when(mockPayment.getFinancialTransactionAmount()).thenReturn(CONVERTED_AMOUNT);
        when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
        when(mockPayment.getCurrency()).thenReturn(mockCurrency);
        when(mockPayment.getOrganization()).thenReturn(mockOrganization);
        when(mockPayment.getPaymentDate()).thenReturn(PAYMENT_DATE);

        // Setup mock currency
        when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
        when(mockCurrency.getStandardPrecision()).thenReturn(Long.valueOf(STANDARD_PRECISION));

        // Setup mock business partner
        when(mockBusinessPartner.getId()).thenReturn(BP_ID);

        // Setup mock organization
        when(mockOrganization.getId()).thenReturn(ORG_ID);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBDateUtils != null) {
            mockedOBDateUtils.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testGetDefaultExpectedAmountReturnsPaymentAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultExpectedAmount(requestMap);

        // Then
        assertEquals("Expected amount should match payment amount", PAYMENT_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultActualAmountReturnsPaymentAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultActualAmount(requestMap);

        // Then
        assertEquals("Actual amount should match payment amount", PAYMENT_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultIsSOTrxReturnsY() {
        // When
        String result = classUnderTest.getDefaultIsSOTrx(requestMap);

        // Then
        assertEquals("IsSOTrx should be Y", "Y", result);
    }

    @Test
    public void testGetDefaultTransactionTypeReturnsI() {
        // When
        String result = classUnderTest.getDefaultTransactionType(requestMap);

        // Then
        assertEquals("Transaction type should be I", "I", result);
    }

    @Test
    public void testGetDefaultPaymentTypeReturnsPaymentId() throws JSONException {
        // When
        String result = classUnderTest.getDefaultPaymentType(requestMap);

        // Then
        assertEquals("Payment type should match payment ID", PAYMENT_ID, result);
    }

    @Test
    public void testGetDefaultOrderTypeReturnsEmptyString() throws JSONException {
        // When
        String result = classUnderTest.getDefaultOrderType(requestMap);

        // Then
        assertEquals("Order type should be empty", "", result);
    }

    @Test
    public void testGetDefaultInvoiceTypeReturnsEmptyString() throws JSONException {
        // When
        String result = classUnderTest.getDefaultInvoiceType(requestMap);

        // Then
        assertEquals("Invoice type should be empty", "", result);
    }

    @Test
    public void testGetDefaultDocumentNoReturnsPaymentDocumentNo() throws JSONException {
        // When
        String result = classUnderTest.getDefaultDocumentNo(requestMap);

        // Then
        assertEquals("Document number should match payment document number", DOCUMENT_NO, result);
    }

    @Test
    public void testGetPaymentWithFinPaymentId() throws Exception {
        // When
        FIN_Payment result = (FIN_Payment) getPaymentMethod.invoke(classUnderTest, requestMap);

        // Then
        assertEquals("Should return the mocked payment", mockPayment, result);
    }

    @Test
    public void testGetPaymentWithFinPaymentIdInDifferentFormat() throws Exception {
        // Given
        JSONObject altContext = new JSONObject();
        altContext.put("Fin_Payment_ID", PAYMENT_ID);
        Map<String, String> altRequestMap = new HashMap<>();
        altRequestMap.put("context", altContext.toString());

        // When
        FIN_Payment result = (FIN_Payment) getPaymentMethod.invoke(classUnderTest, altRequestMap);

        // Then
        assertEquals("Should return the mocked payment", mockPayment, result);
    }

    @Test
    public void testGetDefaultConversionRateReturnsPaymentConversionRate() throws JSONException {
        // When
        String result = classUnderTest.getDefaultConversionRate(requestMap);

        // Then
        assertEquals("Conversion rate should match payment conversion rate", CONVERSION_RATE.toPlainString(), result);
    }

    @Test
    public void testGetDefaultConvertedAmountReturnsPaymentConvertedAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultConvertedAmount(requestMap);

        // Then
        assertEquals("Converted amount should match payment converted amount", CONVERTED_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultReceivedFromReturnsBusinessPartnerId() throws JSONException {
        // When
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);

        // Then
        assertEquals("Received from should match business partner ID", BP_ID, result);
    }

    @Test
    public void testGetDefaultReceivedFromWithNullBusinessPartner() throws JSONException {
        // Given
        when(mockPayment.getBusinessPartner()).thenReturn(null);

        // When
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);

        // Then
        assertEquals("Received from should be empty when business partner is null", "", result);
    }

    @Test
    public void testGetDefaultStandardPrecisionReturnsCurrencyPrecision() throws JSONException {
        // When
        String result = classUnderTest.getDefaultStandardPrecision(requestMap);

        // Then
        assertEquals("Standard precision should match currency standard precision", STANDARD_PRECISION.toString(), result);
    }

    @Test
    public void testGetDefaultCurrencyReturnsCurrencyId() throws JSONException {
        // When
        String result = classUnderTest.getDefaultCurrency(requestMap);

        // Then
        assertEquals("Currency should match payment currency ID", CURRENCY_ID, result);
    }

    @Test
    public void testGetOrganizationReturnsOrganizationId() throws JSONException {
        // When
        String result = classUnderTest.getOrganization(requestMap);

        // Then
        assertEquals("Organization should match payment organization ID", ORG_ID, result);
    }

    @Test
    public void testGetDefaultPaymentDateReturnsFormattedPaymentDate() throws JSONException {
        // When
        String result = classUnderTest.getDefaultPaymentDate(requestMap);

        // Then
        assertEquals("Payment date should match formatted payment date", FORMATTED_DATE, result);
    }

    @Test
    public void testGetDefaultDocumentReturnsEmptyString() throws JSONException {
        // When
        String result = classUnderTest.getDefaultDocument(requestMap);

        // Then
        assertEquals("Document should be empty", "", result);
    }

    @Test
    public void testGetBankStatementLineAmountReturnsEmptyString() throws JSONException {
        // When
        String result = classUnderTest.getBankStatementLineAmount(requestMap);

        // Then
        assertEquals("Bank statement line amount should be empty", "", result);
    }

    @Test
    public void testGetSeqReturns100() {
        // When
        long result = classUnderTest.getSeq();

        // Then
        assertEquals("Sequence should be 100", 100L, result);
    }
}
