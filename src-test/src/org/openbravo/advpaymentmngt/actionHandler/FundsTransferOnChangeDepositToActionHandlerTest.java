package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

import java.util.HashMap;
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
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

public class FundsTransferOnChangeDepositToActionHandlerTest extends WeldBaseTest {
    
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    
    @Mock
    private OBDal obDal;
    
    @Mock
    private FIN_FinancialAccount mockFinancialAccount;
    
    @Mock
    private Currency mockCurrency;
    
    @InjectMocks
    private FundsTransferOnChangeDepositToActionHandler handler;
    
    private MockedStatic<OBDal> mockedOBDal;
    private AutoCloseable mocks;
    
    private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";
    private static final String TEST_CURRENCY_ID = "TEST_CURRENCY_ID";
    private static final String TEST_CURRENCY_ISO = "USD";
    
    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Setup static mock for OBDal
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
        
        // Setup Currency mock
        when(mockCurrency.getId()).thenReturn(TEST_CURRENCY_ID);
        when(mockCurrency.getISOCode()).thenReturn(TEST_CURRENCY_ISO);
        
        // Setup Financial Account mock
        when(mockFinancialAccount.getCurrency()).thenReturn(mockCurrency);
        
        // Setup OBDal mock
        when(obDal.get(FIN_FinancialAccount.class, TEST_ACCOUNT_ID)).thenReturn(mockFinancialAccount);
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
    public void testExecute_WithValidAccountReturnsCorrectCurrencyInfo() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String jsonData = String.format("{\"accountID\":\"%s\"}", TEST_ACCOUNT_ID);
        
        // When
        JSONObject result = handler.execute(parameters, jsonData);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Currency ID should match", TEST_CURRENCY_ID, result.getString("currencyID"));
        assertEquals("Currency ISO should match", TEST_CURRENCY_ISO, result.getString("currencyISO"));
    }
    
    @Test
    public void testExecuteWithNullAccountReturnsEmptyResult() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String jsonData = "{\"accountID\":null}";
        
        // When
        JSONObject result = handler.execute(parameters, jsonData);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty", 0, result.length());
    }
    
    @Test
    public void testExecuteWithInvalidAccountReturnsEmptyResult() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String jsonData = "{\"accountID\":\"INVALID_ID\"}";
        when(obDal.get(FIN_FinancialAccount.class, "INVALID_ID")).thenReturn(null);
        
        // When
        JSONObject result = handler.execute(parameters, jsonData);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty", 0, result.length());
    }
    
    @Test
    public void testExecuteWithInvalidJsonReturnsEmptyResult() throws Exception {
        // Given
        Map<String, Object> parameters = new HashMap<>();
        String invalidJson = "invalid json";
        
        // When
        JSONObject result = handler.execute(parameters, invalidJson);
        
        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Result should be empty", 0, result.length());
    }
}
