/*
 *************************************************************************
 * Test class for PaymentOutAddPaymentDefaultValues
 * All portions are Copyright (C) 2023
 * All Rights Reserved.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
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
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

@RunWith(MockitoJUnitRunner.class)
public class PaymentOutAddPaymentDefaultValuesTest {

    private static final String PAYMENT_ID = "TEST_PAYMENT_ID";
    private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
    private static final String CURRENCY_ID = "TEST_CURRENCY_ID";
    private static final String ORGANIZATION_ID = "TEST_ORG_ID";
    private static final String DOCUMENT_NO = "TEST_DOC_NO";
    private static final String FORMATTED_DATE = "2023-01-01";
    private static final BigDecimal PAYMENT_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal GENERATED_CREDIT = new BigDecimal("50.00");
    private static final BigDecimal CONVERSION_RATE = new BigDecimal("1.25");
    private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("125.00");
    private static final BigDecimal STANDARD_PRECISION = new BigDecimal("2");

    @InjectMocks
    private PaymentOutAddPaymentDefaultValues classUnderTest;

    @Mock
    private FIN_Payment mockPayment;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Currency mockCurrency;

    @Mock
    private Organization mockOrganization;

    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBDateUtils> mockedOBDateUtils;
    private OBDal mockOBDal;
    private Map<String, String> requestMap;
    private JSONObject context;
    private AutoCloseable mocks;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Setup static mocks
        mockOBDal = mock(OBDal.class);
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
        
        mockedOBDateUtils = mockStatic(OBDateUtils.class);
        
        // Setup mock payment
        when(mockPayment.getAmount()).thenReturn(PAYMENT_AMOUNT);
        when(mockPayment.getGeneratedCredit()).thenReturn(GENERATED_CREDIT);
        when(mockPayment.getDocumentNo()).thenReturn(DOCUMENT_NO);
        when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(CONVERSION_RATE);
        when(mockPayment.getFinancialTransactionAmount()).thenReturn(CONVERTED_AMOUNT);
        when(mockPayment.getBusinessPartner()).thenReturn(mockBusinessPartner);
        when(mockPayment.getCurrency()).thenReturn(mockCurrency);
        when(mockPayment.getOrganization()).thenReturn(mockOrganization);
        when(mockPayment.getPaymentDate()).thenReturn(new Date());
        
        // Setup mock business partner
        when(mockBusinessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
        
        // Setup mock currency
        when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
        when(mockCurrency.getStandardPrecision()).thenReturn(Long.valueOf(String.valueOf(STANDARD_PRECISION)));
        
        // Setup mock organization
        when(mockOrganization.getId()).thenReturn(ORGANIZATION_ID);
        
        // Setup request map with context
        requestMap = new HashMap<>();
        context = new JSONObject();
        context.put("inpfinPaymentId", PAYMENT_ID);
        requestMap.put("context", context.toString());
        
        // Setup OBDal to return mock payment
        when(mockOBDal.get(FIN_Payment.class, PAYMENT_ID)).thenReturn(mockPayment);
        
        // Setup date formatting
        mockedOBDateUtils.when(() -> OBDateUtils.formatDate(mockPayment.getPaymentDate()))
                .thenReturn(FORMATTED_DATE);
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
    public void testGetSeq() {
        // When
        long result = classUnderTest.getSeq();
        
        // Then
        assertEquals("Sequence should be 100", 100L, result);
    }

    @Test
    public void testGetDefaultExpectedAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultExpectedAmount(requestMap);
        
        // Then
        assertEquals("Expected amount should match payment amount", PAYMENT_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultActualAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultActualAmount(requestMap);
        
        // Then
        assertEquals("Actual amount should match payment amount", PAYMENT_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultGeneratedCredit() throws JSONException {
        // When
        String result = classUnderTest.getDefaultGeneratedCredit(requestMap);
        
        // Then
        assertEquals("Generated credit should match payment generated credit", 
                GENERATED_CREDIT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultGeneratedCreditNull() throws JSONException {
        // Given
        when(mockPayment.getGeneratedCredit()).thenReturn(null);
        
        // When
        String result = classUnderTest.getDefaultGeneratedCredit(requestMap);
        
        // Then
        assertEquals("Generated credit should be zero when null", 
                BigDecimal.ZERO.toPlainString(), result);
    }

    @Test
    public void testGetDefaultIsSOTrx() {
        // When
        String result = classUnderTest.getDefaultIsSOTrx(requestMap);
        
        // Then
        assertEquals("IsSOTrx should be N", "N", result);
    }

    @Test
    public void testGetDefaultTransactionType() {
        // When
        String result = classUnderTest.getDefaultTransactionType(requestMap);
        
        // Then
        assertEquals("Transaction type should be I", "I", result);
    }

    @Test
    public void testGetDefaultPaymentType() throws JSONException {
        // When
        String result = classUnderTest.getDefaultPaymentType(requestMap);
        
        // Then
        assertEquals("Payment type should match payment ID", PAYMENT_ID, result);
    }

    @Test
    public void testGetDefaultOrderType() throws JSONException {
        // When
        String result = classUnderTest.getDefaultOrderType(requestMap);
        
        // Then
        assertEquals("Order type should be empty", "", result);
    }

    @Test
    public void testGetDefaultInvoiceType() throws JSONException {
        // When
        String result = classUnderTest.getDefaultInvoiceType(requestMap);
        
        // Then
        assertEquals("Invoice type should be empty", "", result);
    }

    @Test
    public void testGetDefaultDocumentNo() throws JSONException {
        // When
        String result = classUnderTest.getDefaultDocumentNo(requestMap);
        
        // Then
        assertEquals("Document number should match payment document number", DOCUMENT_NO, result);
    }

    @Test
    public void testGetPaymentWithInpfinPaymentId() throws JSONException {
        // Given
        context = new JSONObject();
        context.put("inpfinPaymentId", PAYMENT_ID);
        requestMap.put("context", context.toString());
        
        // When
        String result = classUnderTest.getDefaultDocumentNo(requestMap);
        
        // Then
        assertEquals("Document number should match when using inpfinPaymentId", DOCUMENT_NO, result);
    }

    @Test
    public void testGetPayment_WithFinPaymentID() throws JSONException {
        // Given
        context = new JSONObject();
        context.put("Fin_Payment_ID", PAYMENT_ID);
        requestMap.put("context", context.toString());
        
        // When
        String result = classUnderTest.getDefaultDocumentNo(requestMap);
        
        // Then
        assertEquals("Document number should match when using Fin_Payment_ID", DOCUMENT_NO, result);
    }

    @Test
    public void testGetDefaultConversionRate() throws JSONException {
        // When
        String result = classUnderTest.getDefaultConversionRate(requestMap);
        
        // Then
        assertEquals("Conversion rate should match payment conversion rate", 
                CONVERSION_RATE.toPlainString(), result);
    }

    @Test
    public void testGetDefaultConvertedAmount() throws JSONException {
        // When
        String result = classUnderTest.getDefaultConvertedAmount(requestMap);
        
        // Then
        assertEquals("Converted amount should match payment converted amount", 
                CONVERTED_AMOUNT.toPlainString(), result);
    }

    @Test
    public void testGetDefaultReceivedFrom() throws JSONException {
        // When
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);
        
        // Then
        assertEquals("Received from should match business partner ID", 
                BUSINESS_PARTNER_ID, result);
    }

    @Test
    public void testGetDefaultReceivedFromNullBusinessPartner() throws JSONException {
        // Given
        when(mockPayment.getBusinessPartner()).thenReturn(null);
        
        // When
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);
        
        // Then
        assertEquals("Received from should be empty when business partner is null", 
                "", result);
    }

    @Test
    public void testGetDefaultStandardPrecision() throws JSONException {
        // When
        String result = classUnderTest.getDefaultStandardPrecision(requestMap);
        
        // Then
        assertEquals("Standard precision should match currency standard precision", 
                STANDARD_PRECISION.toString(), result);
    }

    @Test
    public void testGetDefaultCurrency() throws JSONException {
        // When
        String result = classUnderTest.getDefaultCurrency(requestMap);
        
        // Then
        assertEquals("Currency should match payment currency ID", 
                CURRENCY_ID, result);
    }

    @Test
    public void testGetOrganization() throws JSONException {
        // When
        String result = classUnderTest.getOrganization(requestMap);
        
        // Then
        assertEquals("Organization should match payment organization ID", 
                ORGANIZATION_ID, result);
    }

    @Test
    public void testGetDefaultPaymentDate() throws JSONException {
        // When
        String result = classUnderTest.getDefaultPaymentDate(requestMap);
        
        // Then
        assertEquals("Payment date should be formatted correctly", 
                FORMATTED_DATE, result);
    }

    @Test
    public void testGetDefaultDocument() throws JSONException {
        // When
        String result = classUnderTest.getDefaultDocument(requestMap);
        
        // Then
        assertEquals("Default document should be empty", "", result);
    }

    @Test
    public void testGetBankStatementLineAmount() throws JSONException {
        // When
        String result = classUnderTest.getBankStatementLineAmount(requestMap);
        
        // Then
        assertEquals("Bank statement line amount should be empty", "", result);
    }
}