package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;

/**
 * Test class for {@link PurchaseInvoiceAddPaymentDefaultValues}
 */
@RunWith(MockitoJUnitRunner.class)
public class PurchaseInvoiceAddPaymentDefaultValuesTest {

    private static final String INVOICE_ID = "TEST_INVOICE_ID";
    private static final String BUSINESS_PARTNER_ID = "TEST_BP_ID";
    private static final String CURRENCY_ID = "TEST_CURRENCY_ID";
    private static final String ORGANIZATION_ID = "TEST_ORG_ID";
    private static final String FORMATTED_DATE = "01-01-2023";

    @Mock
    private Invoice mockInvoice;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Currency mockCurrency;

    @Mock
    private Organization mockOrganization;

    @Spy
    @InjectMocks
    private PurchaseInvoiceAddPaymentDefaultValues defaultValues;

    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBDateUtils> mockedOBDateUtils;
    private OBDal mockOBDal;
    private Map<String, String> requestMap;
    private JSONObject mockContext;

    @Before
    public void setUp() throws Exception {
        // Initialize static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDateUtils = mockStatic(OBDateUtils.class);

        // Configure mocked objects
        mockOBDal = mock(OBDal.class);
        mockContext = new JSONObject();
        mockContext.put("inpcInvoiceId", INVOICE_ID);

        // Set up static mocks
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
        mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class))).thenReturn(FORMATTED_DATE);

        // Setup request map
        requestMap = new HashMap<>();
        requestMap.put("context", mockContext.toString());

        // Setup mock invoice
        when(mockOBDal.get(Invoice.class, INVOICE_ID)).thenReturn(mockInvoice);
        when(mockInvoice.getBusinessPartner()).thenReturn(mockBusinessPartner);
        when(mockInvoice.getCurrency()).thenReturn(mockCurrency);
        when(mockInvoice.getOrganization()).thenReturn(mockOrganization);
        when(mockBusinessPartner.getId()).thenReturn(BUSINESS_PARTNER_ID);
        when(mockCurrency.getId()).thenReturn(CURRENCY_ID);
        when(mockCurrency.getStandardPrecision()).thenReturn(2L);
        when(mockOrganization.getId()).thenReturn(ORGANIZATION_ID);

        // Setup payment schedule list
        List<FIN_PaymentSchedule> paymentScheduleList = new ArrayList<>();
        FIN_PaymentSchedule mockPaymentSchedule = mock(FIN_PaymentSchedule.class);
        paymentScheduleList.add(mockPaymentSchedule);

        doReturn(new BigDecimal("100.00")).when(defaultValues).getPendingAmount(requestMap);
    }

    @After
    public void tearDown() {
        // Close all static mocks
        if (mockedOBDal != null) mockedOBDal.close();
        if (mockedOBDateUtils != null) mockedOBDateUtils.close();
    }

    @Test
    public void testGetDefaultExpectedAmount() throws JSONException {
        // When
        String result = defaultValues.getDefaultExpectedAmount(requestMap);

        // Then
        assertEquals("100.00", result);
    }

    @Test
    public void testGetDefaultActualAmount() throws JSONException {
        // When
        String result = defaultValues.getDefaultActualAmount(requestMap);

        // Then
        assertEquals("100.00", result);
    }

    @Test
    public void testGetDefaultIsSOTrx() {
        // When
        String result = defaultValues.getDefaultIsSOTrx(requestMap);

        // Then
        assertEquals("N", result);
    }

    @Test
    public void testGetDefaultTransactionType() {
        // When
        String result = defaultValues.getDefaultTransactionType(requestMap);

        // Then
        assertEquals("I", result);
    }

    @Test
    public void testGetDefaultPaymentType() {
        // When
        String result = defaultValues.getDefaultPaymentType(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultOrderType() {
        // When
        String result = defaultValues.getDefaultOrderType(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultInvoiceType() throws JSONException {
        // When
        String result = defaultValues.getDefaultInvoiceType(requestMap);

        // Then
        assertEquals(INVOICE_ID, result);
    }

    @Test
    public void testGetDefaultConversionRate() throws JSONException {
        // When
        String result = defaultValues.getDefaultConversionRate(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultConvertedAmount() throws JSONException {
        // When
        String result = defaultValues.getDefaultConvertedAmount(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultReceivedFrom() throws JSONException {
        // When
        String result = defaultValues.getDefaultReceivedFrom(requestMap);

        // Then
        assertEquals(BUSINESS_PARTNER_ID, result);
    }

    @Test
    public void testGetDefaultStandardPrecision() throws JSONException {
        // When
        String result = defaultValues.getDefaultStandardPrecision(requestMap);

        // Then
        assertEquals("2", result);
    }

    @Test
    public void testGetDefaultCurrency() throws JSONException {
        // When
        String result = defaultValues.getDefaultCurrency(requestMap);

        // Then
        assertEquals(CURRENCY_ID, result);
    }

    @Test
    public void testGetOrganization() throws JSONException {
        // When
        String result = defaultValues.getOrganization(requestMap);

        // Then
        assertEquals(ORGANIZATION_ID, result);
    }

    @Test
    public void testGetDefaultDocument() throws JSONException {
        // When
        String result = defaultValues.getDefaultDocument(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultPaymentDate() throws JSONException {
        // When
        String result = defaultValues.getDefaultPaymentDate(requestMap);

        // Then
        assertEquals(FORMATTED_DATE, result);
    }

    @Test
    public void testGetBankStatementLineAmount() throws JSONException {
        // When
        String result = defaultValues.getBankStatementLineAmount(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetSeq() {
        // When
        long result = defaultValues.getSeq();

        // Then
        assertEquals(100L, result);
    }
}