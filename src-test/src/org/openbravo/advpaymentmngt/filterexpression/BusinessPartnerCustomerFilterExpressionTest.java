package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;

/**
 * Unit tests for the BusinessPartnerCustomerFilterExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerCustomerFilterExpressionTest {

    private BusinessPartnerCustomerFilterExpression filterExpression;

    /**
     * Sets up the test environment before each test.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filterExpression = new BusinessPartnerCustomerFilterExpression();
    }

    /**
     * Tests the getExpression method for a financial account window with a sales transaction.
     */
    @Test
    public void testGetExpressionFinancialAccountWindowSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, TestConstants.TRUE);

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }

    /**
     * Tests the getExpression method for a financial account window with a non-sales transaction.
     */
    @Test
    public void testGetExpressionFinancialAccountWindowNonSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, "false");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("", result);
    }

    /**
     * Tests the getExpression method for a financial account window with isSOTrx set to "Y".
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrxY() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("IsSOTrx", "Y");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }

    /**
     * Tests the getExpression method for a financial account window with isSOTrx set to "N".
     */
    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrxN() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, TestConstants.FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put(TestConstants.IS_SO_TRX, "N");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("", result);
    }

    /**
     * Tests the getExpression method with a null window ID and a sales transaction.
     */
    @Test
    public void testGetExpressionNullWindowIdSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(TestConstants.IS_SO_TRX, TestConstants.TRUE);

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }
}
