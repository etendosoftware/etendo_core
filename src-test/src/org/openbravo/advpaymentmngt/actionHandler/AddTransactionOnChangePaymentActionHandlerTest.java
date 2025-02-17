package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

public class AddTransactionOnChangePaymentActionHandlerTest extends WeldBaseTest {
    private static final Logger log = LogManager.getLogger();

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

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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

    @After
    public void tearDown() {
        if (obDalMock != null) obDalMock.close();
        if (obContextMock != null) obContextMock.close();
        if (finUtilityMock != null) finUtilityMock.close();
    }

    @Test
    public void testExecute_NullPaymentId() throws Exception {
        // GIVEN
        Map<String, Object> parameters = new HashMap<>();
        String data = "{\"strDescription\":\"Test Description\", \"strPaymentId\":null}";

        // WHEN
        finUtilityMock.when(() -> FIN_Utility.getFinAccTransactionDescription("Test Description", "", ""))
                .thenReturn("Processed Description");

        JSONObject result = actionHandler.execute(parameters, data);

        // THEN
        assertEquals("Processed Description", result.getString("description"));
        assertEquals(BigDecimal.ZERO, result.get("depositamt"));
        assertEquals(BigDecimal.ZERO, result.get("paymentamt"));
    }

    @Test
    public void testExecute_ReceiptPayment() throws Exception {
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
        finUtilityMock.when(() -> FIN_Utility.getFinAccTransactionDescription(
                "Test Description", "", "Payment Description"))
                .thenReturn("Processed Description");

        // WHEN
        JSONObject result = actionHandler.execute(parameters, data);

        // THEN
        assertEquals(BigDecimal.TEN, result.get("depositamt"));
        assertEquals(BigDecimal.ZERO, result.get("paymentamt"));
        assertEquals("testBPartnerId", result.getString("cBpartnerId"));
        assertEquals("Processed Description", result.getString("description"));
    }



    @Test
    public void testExecute_ExceptionHandling() throws Exception {
        // GIVEN
        Map<String, Object> parameters = new HashMap<>();
        String data = "invalid json";

        // WHEN
        JSONObject result = actionHandler.execute(parameters, data);

        // THEN
        // Verify that no exception is thrown and an empty result is returned
        assertTrue(result.length() == 0);
    }
}