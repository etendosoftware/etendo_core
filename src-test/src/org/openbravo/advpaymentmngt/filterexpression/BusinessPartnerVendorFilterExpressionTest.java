package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;

/**
 * Unit tests for the BusinessPartnerVendorFilterExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerVendorFilterExpressionTest {

    private BusinessPartnerVendorFilterExpression filterExpression;

    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        filterExpression = new BusinessPartnerVendorFilterExpression();
    }

    /**
     * Tests the getExpression method for a financial account window that is not a sales transaction.
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsNotSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, "false");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals(TestConstants.TRUE, result);
    }

    /**
     * Tests the getExpression method for a financial account window that is a sales transaction.
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, TestConstants.TRUE);

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("", result);
    }

    /**
     * Tests the getExpression method for a financial account window with IS_SO_TRX set to "N".
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrxN() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("IsSOTrx", "N");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals(TestConstants.TRUE, result);
    }

    /**
     * Tests the getExpression method for a financial account window with IS_SO_TRX set to "Y".
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrxY() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, "Y");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("", result);
    }

    /**
     * Tests the getExpression method with a null window ID and IS_SO_TRX set to "false".
     */
    @Test
    public void testGetExpressionNullWindowIdNotSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(TestConstants.IS_SO_TRX, "false");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals(TestConstants.TRUE, result);
    }
}
