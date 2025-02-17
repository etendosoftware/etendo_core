package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;

@RunWith(MockitoJUnitRunner.class)
public class PurchaseOrderAddPaymentDefaultValuesTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // Static mocks
    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBDateUtils> mockedOBDateUtils;

    // Mocks
    @Mock
    private OBDal mockOBDal;

    @Mock
    private Order mockOrder;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Currency mockCurrency;

    @Mock
    private Organization mockOrganization;

    @InjectMocks
    private PurchaseOrderAddPaymentDefaultValues classUnderTest;

    private Map<String, String> requestMap;
    private JSONObject mockContext;
    private List<FIN_PaymentSchedule> paymentScheduleList;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Initialize static mocks
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        mockedOBDateUtils = mockStatic(OBDateUtils.class);

        // Setup mock context
        mockContext = new JSONObject();
        mockContext.put("inpcOrderId", "TEST_ORDER_ID");

        // Setup request map
        requestMap = new HashMap<>();
        requestMap.put("context", mockContext.toString());

        // Setup order mock
        when(mockOBDal.get(Order.class, "TEST_ORDER_ID")).thenReturn(mockOrder);

        // Setup business partner mock
        when(mockBusinessPartner.getId()).thenReturn("TEST_BP_ID");
        when(mockOrder.getBusinessPartner()).thenReturn(mockBusinessPartner);

        // Setup currency mock
        when(mockCurrency.getId()).thenReturn("TEST_CURRENCY_ID");
        when(mockCurrency.getStandardPrecision()).thenReturn(2L);
        when(mockOrder.getCurrency()).thenReturn(mockCurrency);

        // Setup organization mock
        when(mockOrganization.getId()).thenReturn("TEST_ORG_ID");
        when(mockOrder.getOrganization()).thenReturn(mockOrganization);

        // Setup payment schedule list
        paymentScheduleList = new ArrayList<>();
        when(mockOrder.getFINPaymentScheduleList()).thenReturn(paymentScheduleList);

        // Setup date formatting
        Date testDate = new Date();
        mockedOBDateUtils.when(() -> OBDateUtils.formatDate(any(Date.class)))
            .thenReturn("2023-01-01");
    }

    @After
    public void tearDown() {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedOBDateUtils != null) {
            mockedOBDateUtils.close();
        }
    }

    @Test
    public void testGetDefaultExpectedAmount_ReturnsOrderPendingAmount() throws JSONException {
        // GIVEN
        // Create mock payment schedule
        FIN_PaymentSchedule mockSchedule = mock(FIN_PaymentSchedule.class);

        // Mock the invoice as null to force using order payment schedule list
        when(mockSchedule.getInvoice()).thenReturn(null);

        // Create mock payment schedule details
        List<FIN_PaymentScheduleDetail> mockScheduleDetails = new ArrayList<>();
        FIN_PaymentScheduleDetail mockDetail = mock(FIN_PaymentScheduleDetail.class);

        // Set amount and null payment details to match expected behavior
        when(mockDetail.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(mockDetail.getPaymentDetails()).thenReturn(null);

        mockScheduleDetails.add(mockDetail);

        // Link details to schedule
        when(mockSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(mockScheduleDetails);

        // Add to payment schedule list
        paymentScheduleList.add(mockSchedule);

        // WHEN
        String result = classUnderTest.getDefaultExpectedAmount(requestMap);

        // THEN
        assertEquals("100.00", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getFINPaymentScheduleList();
        verify(mockSchedule).getInvoice();
        verify(mockSchedule).getFINPaymentScheduleDetailOrderPaymentScheduleList();
        verify(mockDetail).getAmount();
        verify(mockDetail).getPaymentDetails();
    }

    @Test
    public void testGetDefaultActualAmount_ReturnsOrderPendingAmount() throws JSONException {
        // GIVEN
        // Create mock payment schedule
        FIN_PaymentSchedule mockSchedule = mock(FIN_PaymentSchedule.class);

        // Mock the invoice as null to force using order payment schedule list
        when(mockSchedule.getInvoice()).thenReturn(null);

        // Create mock payment schedule details
        List<FIN_PaymentScheduleDetail> mockScheduleDetails = new ArrayList<>();
        FIN_PaymentScheduleDetail mockDetail = mock(FIN_PaymentScheduleDetail.class);

        // Set amount and null payment details to match expected behavior
        when(mockDetail.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(mockDetail.getPaymentDetails()).thenReturn(null);

        mockScheduleDetails.add(mockDetail);

        // Link details to schedule
        when(mockSchedule.getFINPaymentScheduleDetailOrderPaymentScheduleList()).thenReturn(mockScheduleDetails);

        // Add to payment schedule list
        paymentScheduleList.add(mockSchedule);

        // WHEN
        String result = classUnderTest.getDefaultActualAmount(requestMap);

        // THEN
        assertEquals("100.00", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getFINPaymentScheduleList();
        verify(mockSchedule).getInvoice();
        verify(mockSchedule).getFINPaymentScheduleDetailOrderPaymentScheduleList();
        verify(mockDetail).getAmount();
        verify(mockDetail).getPaymentDetails();
    }

    @Test
    public void testGetDefaultIsSOTrx_ReturnsN() {
        // WHEN
        String result = classUnderTest.getDefaultIsSOTrx(requestMap);

        // THEN
        assertEquals("N", result);
    }

    @Test
    public void testGetDefaultTransactionType_ReturnsO() {
        // WHEN
        String result = classUnderTest.getDefaultTransactionType(requestMap);

        // THEN
        assertEquals("O", result);
    }

    @Test
    public void testGetDefaultPaymentType_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultPaymentType(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultOrderType_ReturnsOrderId() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultOrderType(requestMap);

        // THEN
        assertEquals("TEST_ORDER_ID", result);
    }

    @Test
    public void testGetDefaultInvoiceType_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultInvoiceType(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultConversionRate_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultConversionRate(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultConvertedAmount_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultConvertedAmount(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetDefaultReceivedFrom_ReturnsBusinessPartnerId() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);

        // THEN
        assertEquals("TEST_BP_ID", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getBusinessPartner();
        verify(mockBusinessPartner).getId();
    }

    @Test
    public void testGetDefaultStandardPrecision_ReturnsCurrencyPrecision() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultStandardPrecision(requestMap);

        // THEN
        assertEquals("2", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getCurrency();
        verify(mockCurrency).getStandardPrecision();
    }

    @Test
    public void testGetDefaultCurrency_ReturnsCurrencyId() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultCurrency(requestMap);

        // THEN
        assertEquals("TEST_CURRENCY_ID", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getCurrency();
        verify(mockCurrency).getId();
    }

    @Test
    public void testGetOrganization_ReturnsOrganizationId() throws JSONException {
        // WHEN
        String result = classUnderTest.getOrganization(requestMap);

        // THEN
        assertEquals("TEST_ORG_ID", result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
        verify(mockOrder).getOrganization();
        verify(mockOrganization).getId();
    }

    @Test
    public void testGetDefaultDocument_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultDocument(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetOrder_ReturnsOrderFromContext() throws JSONException {
        // WHEN
        Order result = classUnderTest.getOrder(mockContext);

        // THEN
        assertEquals(mockOrder, result);
        verify(mockOBDal).get(Order.class, "TEST_ORDER_ID");
    }

    @Test
    public void testGetDefaultPaymentDate_ReturnsFormattedCurrentDate() throws JSONException {
        // WHEN
        String result = classUnderTest.getDefaultPaymentDate(requestMap);

        // THEN
        assertEquals("2023-01-01", result);
        mockedOBDateUtils.verify(() -> OBDateUtils.formatDate(any(Date.class)));
    }

    @Test
    public void testGetBankStatementLineAmount_ReturnsEmptyString() throws JSONException {
        // WHEN
        String result = classUnderTest.getBankStatementLineAmount(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetSeq_Returns100() {
        // WHEN
        long result = classUnderTest.getSeq();

        // THEN
        assertEquals(100L, result);
    }
}