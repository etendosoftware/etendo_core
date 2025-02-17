package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.application.OBBindingsConstants;

@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerVendorFilterExpressionTest {

    private BusinessPartnerVendorFilterExpression filterExpression;
    private static final String FINANCIAL_ACCOUNT_WINDOW_ID = "94EAA455D2644E04AB25D93BE5157B6D";

    @Before
    public void setUp() {
        filterExpression = new BusinessPartnerVendorFilterExpression();
    }

    @Test
    public void testGetExpression_FinancialAccountWindow_IsNotSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("issotrx", "false");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("true", result);
    }

    @Test
    public void testGetExpression_FinancialAccountWindow_IsSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("issotrx", "true");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetExpression_FinancialAccountWindow_IsSOTrxN() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("IsSOTrx", "N");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("true", result);
    }

    @Test
    public void testGetExpression_FinancialAccountWindow_IsSOTrxY() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, FINANCIAL_ACCOUNT_WINDOW_ID);
        requestMap.put("IsSOTrx", "Y");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("", result);
    }

    @Test
    public void testGetExpression_NullWindowId_NotSalesTransaction() {
        // Given
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("issotrx", "false");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("true", result);
    }


}
