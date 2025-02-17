package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

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
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;

public class ReceivedFromPaymentMethodActionHandlerTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ReceivedFromPaymentMethodActionHandler actionHandler;
    private MockedStatic<OBDal> mockedOBDal;
    private AutoCloseable mocks;

    @Mock
    private OBDal mockOBDal;
    @Mock
    private FIN_FinancialAccount mockFinancialAccount;
    @Mock
    private BusinessPartner mockBusinessPartner;
    @Mock
    private FIN_PaymentMethod mockPaymentMethod;
    @Mock
    private OBCriteria<FinAccPaymentMethod> mockCriteria;
    @Mock
    private FinAccPaymentMethod mockFinAccPaymentMethod;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        actionHandler = new ReceivedFromPaymentMethodActionHandler();
        
        // Setup static mock for OBDal
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
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
    public void testExecute_WithValidSOTrxData_ReturnsPaymentMethod() throws Exception {
        // Given
        String financialAccountId = "TEST_FIN_ACC";
        String receivedFromId = "TEST_BP";
        String paymentMethodId = "TEST_PM";
        String paymentMethodName = "Test Payment Method";

        Map<String, Object> parameters = new HashMap<>();
        String jsonData = String.format(
            "{\"financialAccount\":\"%s\",\"receivedFrom\":\"%s\",\"isSOTrx\":\"true\"}",
            financialAccountId, receivedFromId);

        // Mock OBDal get calls
        when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
        when(mockOBDal.get(BusinessPartner.class, receivedFromId)).thenReturn(mockBusinessPartner);
        
        // Mock BusinessPartner payment method
        when(mockBusinessPartner.getPaymentMethod()).thenReturn(mockPaymentMethod);
        
        // Mock criteria chain
        when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
        when(mockCriteria.add(any())).thenReturn(mockCriteria);
        when(mockCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockCriteria);
        when(mockCriteria.setFilterOnActive(true)).thenReturn(mockCriteria);
        when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
        when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
        
        // Mock FinAccPaymentMethod
        when(mockFinAccPaymentMethod.getPaymentMethod()).thenReturn(mockPaymentMethod);
        when(mockPaymentMethod.getId()).thenReturn(paymentMethodId);
        when(mockPaymentMethod.getName()).thenReturn(paymentMethodName);

        // When
        JSONObject result = actionHandler.execute(parameters, jsonData);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Payment method ID should match", paymentMethodId, result.getString("paymentMethodId"));
        assertEquals("Payment method name should match", paymentMethodName, result.getString("paymentMethodName"));
    }

    @Test
    public void testExecute_WithValidPOTrxData_ReturnsPaymentMethod() throws Exception {
        // Given
        String financialAccountId = "TEST_FIN_ACC";
        String receivedFromId = "TEST_BP";
        String paymentMethodId = "TEST_PM";
        String paymentMethodName = "Test Payment Method";

        Map<String, Object> parameters = new HashMap<>();
        String jsonData = String.format(
            "{\"financialAccount\":\"%s\",\"receivedFrom\":\"%s\",\"isSOTrx\":\"false\"}",
            financialAccountId, receivedFromId);

        // Mock OBDal get calls
        when(mockOBDal.get(FIN_FinancialAccount.class, financialAccountId)).thenReturn(mockFinancialAccount);
        when(mockOBDal.get(BusinessPartner.class, receivedFromId)).thenReturn(mockBusinessPartner);
        
        // Mock BusinessPartner payment method
        when(mockBusinessPartner.getPOPaymentMethod()).thenReturn(mockPaymentMethod);
        
        // Mock criteria chain
        when(mockOBDal.createCriteria(FinAccPaymentMethod.class)).thenReturn(mockCriteria);
        when(mockCriteria.add(any())).thenReturn(mockCriteria);
        when(mockCriteria.setFilterOnReadableOrganization(false)).thenReturn(mockCriteria);
        when(mockCriteria.setFilterOnActive(true)).thenReturn(mockCriteria);
        when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
        when(mockCriteria.uniqueResult()).thenReturn(mockFinAccPaymentMethod);
        
        // Mock FinAccPaymentMethod
        when(mockFinAccPaymentMethod.getPaymentMethod()).thenReturn(mockPaymentMethod);
        when(mockPaymentMethod.getId()).thenReturn(paymentMethodId);
        when(mockPaymentMethod.getName()).thenReturn(paymentMethodName);

        // When
        JSONObject result = actionHandler.execute(parameters, jsonData);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Payment method ID should match", paymentMethodId, result.getString("paymentMethodId"));
        assertEquals("Payment method name should match", paymentMethodName, result.getString("paymentMethodName"));
    }

    @Test
    public void testExecute_WithNullFinancialAccount_ReturnsEmptyPaymentMethod() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String jsonData = "{\"financialAccount\":\"INVALID_ID\",\"receivedFrom\":\"TEST_BP\",\"isSOTrx\":\"true\"}";

        when(mockOBDal.get(FIN_FinancialAccount.class, "INVALID_ID")).thenReturn(null);

        // When
        JSONObject result = actionHandler.execute(parameters, jsonData);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Payment method ID should be empty", "", result.getString("paymentMethodId"));
        assertEquals("Payment method name should be empty", "", result.getString("paymentMethodName"));
    }

    @Test
    public void testExecute_WithInvalidJson_ThrowsOBException() throws Exception {
        // Given
        expectedException.expect(OBException.class);
        Map<String, Object> parameters = new HashMap<>();
        String invalidJson = "invalid json";

        // When
        actionHandler.execute(parameters, invalidJson);
    }
}