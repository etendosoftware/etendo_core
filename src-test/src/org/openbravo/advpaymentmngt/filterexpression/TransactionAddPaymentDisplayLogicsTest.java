package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link TransactionAddPaymentDisplayLogics}
 */
public class TransactionAddPaymentDisplayLogicsTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OBDal mockOBDal;

    private TransactionAddPaymentDisplayLogics classUnderTest;

    @Mock
    private BusinessPartner mockBusinessPartner;

    @Mock
    private Organization mockOrganization;

    @Mock
    private Currency mockCurrency;

    private MockedStatic<OBDal> mockedOBDal;

    private MockedConstruction<AdvPaymentMngtDao> mockedAdvPaymentMngtDao;

    private AutoCloseable mocks;

    @Before
    public void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        classUnderTest = new TransactionAddPaymentDisplayLogics();

        mockedOBDal = mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mockedAdvPaymentMngtDao != null) {
            mockedAdvPaymentMngtDao.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testGetSeq_returnsExpectedValue() {
        // When
        long result = classUnderTest.getSeq();

        // Then
        assertEquals(100L, result);
    }

    @Test
    public void testGetDocumentDisplayLogic_alwaysReturnsTrue() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        boolean result = classUnderTest.getDocumentDisplayLogic(requestMap);

        // Then
        assertTrue("Document display logic should always return true", result);
    }

    @Test
    public void testGetOrganizationDisplayLogic_alwaysReturnsFalse() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        boolean result = classUnderTest.getOrganizationDisplayLogic(requestMap);

        // Then
        assertFalse("Organization display logic should always return false", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogic_withNoBusinessPartner_returnsFalse() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        requestMap.put("context", context.toString());

        // When
        boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertFalse("Credit to use display logic should return false when no business partner is provided", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogic_withEmptyBusinessPartner_returnsFalse() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("received_from", "");
        requestMap.put("context", context.toString());

        // When
        boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertFalse("Credit to use display logic should return false when business partner is empty", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogic_withBusinessPartnerAndZeroCredit_returnsFalse() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("received_from", "BP123");
        context.put("trxtype", "PAYMENT");
        context.put("ad_org_id", "ORG123");
        context.put("c_currency_id", "CURR123");
        requestMap.put("context", context.toString());

        // Mock OBDal.get calls
        when(mockOBDal.get(BusinessPartner.class, "BP123")).thenReturn(mockBusinessPartner);
        when(mockOBDal.get(Organization.class, "ORG123")).thenReturn(mockOrganization);
        when(mockOBDal.get(Currency.class, "CURR123")).thenReturn(mockCurrency);

        mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
            (mock, ctx) -> {
                when(mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency))
                    .thenReturn(BigDecimal.ZERO);
            });

        TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }
        };

        // When
        boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertFalse("Credit to use display logic should return false when customer credit is zero", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogicWithBusinessPartnerAndPositiveCreditReturnsTrue() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("received_from", "BP123");
        context.put("trxtype", "PAYMENT");
        context.put("ad_org_id", "ORG123");
        context.put("c_currency_id", "CURR123");
        requestMap.put("context", context.toString());

        // Mock OBDal.get calls
        when(mockOBDal.get(BusinessPartner.class, "BP123")).thenReturn(mockBusinessPartner);
        when(mockOBDal.get(Organization.class, "ORG123")).thenReturn(mockOrganization);
        when(mockOBDal.get(Currency.class, "CURR123")).thenReturn(mockCurrency);

        mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
            (mock, ctx) -> {
                when(mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency))
                    .thenReturn(new BigDecimal("100.00"));
            });

        TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }
        };

        // When
        boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertTrue("Credit to use display logic should return true when customer credit is positive", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogicWithRCINDocumentTypeChecksCredit() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("received_from", "BP123");
        context.put("trxtype", "RCIN");
        context.put("ad_org_id", "ORG123");
        context.put("c_currency_id", "CURR123");
        requestMap.put("context", context.toString());

        // Mock OBDal.get calls
        when(mockOBDal.get(BusinessPartner.class, "BP123")).thenReturn(mockBusinessPartner);
        when(mockOBDal.get(Organization.class, "ORG123")).thenReturn(mockOrganization);
        when(mockOBDal.get(Currency.class, "CURR123")).thenReturn(mockCurrency);

        AdvPaymentMngtDao mockDao = mock(AdvPaymentMngtDao.class);
        when(mockDao.getCustomerCredit(mockBusinessPartner, true, mockOrganization, mockCurrency))
            .thenReturn(new BigDecimal("100.00"));

        mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
            (mock, ctx) -> {
                when(mock.getCustomerCredit(mockBusinessPartner, true, mockOrganization, mockCurrency))
                    .thenReturn(new BigDecimal("100.00"));
            });

        // When
        boolean result = classUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertTrue("Credit to use display logic should return true for RCIN document with positive credit", result);
    }

    @Test
    public void testGetCreditToUseDisplayLogicWithInpreceivedFromUsesAlternativeParameter() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("inpreceivedFrom", "BP123");
        context.put("inptrxtype", "PAYMENT");
        context.put("ad_org_id", "ORG123");
        context.put("c_currency_id", "CURR123");
        requestMap.put("context", context.toString());

        // Mock OBDal.get calls
        when(mockOBDal.get(BusinessPartner.class, "BP123")).thenReturn(mockBusinessPartner);
        when(mockOBDal.get(Organization.class, "ORG123")).thenReturn(mockOrganization);
        when(mockOBDal.get(Currency.class, "CURR123")).thenReturn(mockCurrency);

        mockedAdvPaymentMngtDao = mockConstruction(AdvPaymentMngtDao.class,
            (mock, ctx) -> {
                when(mock.getCustomerCredit(mockBusinessPartner, false, mockOrganization, mockCurrency))
                    .thenReturn(new BigDecimal("100.00"));
            });

        TransactionAddPaymentDisplayLogics spyClassUnderTest = new TransactionAddPaymentDisplayLogics() {
            @Override
            BigDecimal getDefaultGeneratedCredit(Map<String, String> requestMap) {
                return BigDecimal.ZERO;
            }
        };

        // When
        boolean result = spyClassUnderTest.getCreditToUseDisplayLogic(requestMap);

        // Then
        assertTrue("Credit to use display logic should return true when using inpreceivedFrom parameter", result);
    }

    @Test
    public void testGetDefaultGeneratedCreditReturnsZero() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();

        // When
        BigDecimal result = classUnderTest.getDefaultGeneratedCredit(requestMap);

        // Then
        assertEquals("Default generated credit should be zero", BigDecimal.ZERO, result);
    }

    @Test
    public void testGetBankStatementLineDisplayLogicWithTrxtypeReturnsTrue() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put("trxtype", "PAYMENT");
        requestMap.put("context", context.toString());

        // When
        boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);

        // Then
        assertTrue("Bank statement line display logic should return true when trxtype is present", result);
    }

    @Test
    public void testGetBankStatementLineDisplayLogicwithoutTrxtypeReturnsFalse() throws JSONException {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        requestMap.put("context", context.toString());

        // When
        boolean result = classUnderTest.getBankStatementLineDisplayLogic(requestMap);

        // Then
        assertFalse("Bank statement line display logic should return false when trxtype is not present", result);
    }
}