package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.test.base.TestConstants;

public class SalesInvoiceAddPaymentDefaultValuesTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private SalesInvoiceAddPaymentDefaultValues classUnderTest;

    @Mock
    private Invoice mockInvoice;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Currency mockCurrency;

    @Mock
    private Organization mockOrganization;

    private MockedStatic<OBDal> mockedOBDal;
    private AutoCloseable mocks;

    private static final String TEST_INVOICE_ID = "TEST_INVOICE_ID";
    private static final String TEST_BP_ID = "TEST_BP_ID";
    private static final String TEST_CURRENCY_ID = "TEST_CURRENCY_ID";
    private static final String TEST_ORG_ID = "TEST_ORG_ID";

    @Before
    public void setUp() throws Exception {
        // Initialize mocks
        mocks = MockitoAnnotations.openMocks(this);

        // Set up OBContext
        OBContext.setOBContext(TestConstants.Users.ADMIN,
                TestConstants.Roles.FB_GRP_ADMIN,
                TestConstants.Clients.FB_GRP,
                TestConstants.Orgs.ESP_NORTE);

        // Set up static mocks
        mockedOBDal = mockStatic(OBDal.class);
        OBDal mockOBDal = mock(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

        // Configure mock invoice
        when(mockInvoice.getBusinessPartner()).thenReturn(mockBusinessPartner);
        when(mockInvoice.getCurrency()).thenReturn(mockCurrency);
        when(mockInvoice.getOrganization()).thenReturn(mockOrganization);
        when(mockBusinessPartner.getId()).thenReturn(TEST_BP_ID);
        when(mockCurrency.getId()).thenReturn(TEST_CURRENCY_ID);
        when(mockCurrency.getStandardPrecision()).thenReturn(2L);
        when(mockOrganization.getId()).thenReturn(TEST_ORG_ID);

        // Configure OBDal mock to return our mock invoice
        when(mockOBDal.get(Invoice.class, TEST_INVOICE_ID)).thenReturn(mockInvoice);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testGetDefaultReceivedFrom_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcInvoiceId", TEST_INVOICE_ID);
        requestMap.put("context", context.toString());

        // When
        String result = classUnderTest.getDefaultReceivedFrom(requestMap);

        // Then
        assertEquals("Should return the business partner ID", TEST_BP_ID, result);
    }

    @Test
    public void testGetDefaultCurrency_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcInvoiceId", TEST_INVOICE_ID);
        requestMap.put("context", context.toString());

        // When
        String result = classUnderTest.getDefaultCurrency(requestMap);

        // Then
        assertEquals("Should return the currency ID", TEST_CURRENCY_ID, result);
    }

    @Test
    public void testGetDefaultStandardPrecision_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcInvoiceId", TEST_INVOICE_ID);
        requestMap.put("context", context.toString());

        // When
        String result = classUnderTest.getDefaultStandardPrecision(requestMap);

        // Then
        assertEquals("Should return the standard precision", "2", result);
    }

    @Test
    public void testGetOrganization_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcInvoiceId", TEST_INVOICE_ID);
        requestMap.put("context", context.toString());

        // When
        String result = classUnderTest.getOrganization(requestMap);

        // Then
        assertEquals("Should return the organization ID", TEST_ORG_ID, result);
    }

    @Test
    public void testGetDefaultIsSOTrx_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        String result = classUnderTest.getDefaultIsSOTrx(requestMap);

        // Then
        assertEquals("Should return Y for sales transaction", "Y", result);
    }

    @Test
    public void testGetDefaultTransactionType_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        String result = classUnderTest.getDefaultTransactionType(requestMap);

        // Then
        assertEquals("Should return I for invoice transaction", "I", result);
    }

    @Test
    public void testGetPendingAmount_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpcInvoiceId", TEST_INVOICE_ID);
        requestMap.put("context", context.toString());

        List<FIN_PaymentSchedule> schedules = new ArrayList<>();
        when(mockInvoice.getFINPaymentScheduleList()).thenReturn(schedules);

        // When
        String result = classUnderTest.getDefaultExpectedAmount(requestMap);

        // Then
        assertNotNull("Should return a non-null amount", result);
        assertEquals("Should return 0 for empty schedule", "0", result);
    }

    @Test
    public void testGetDefaultPaymentDate_Success() throws Exception {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        String result = classUnderTest.getDefaultPaymentDate(requestMap);

        // Then
        assertNotNull("Should return a non-null date", result);
    }
}