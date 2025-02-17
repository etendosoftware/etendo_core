package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.client.application.OBBindingsConstants;
import org.openbravo.client.kernel.RequestContext;

@RunWith(MockitoJUnitRunner.class)
public class BusinessPartnerCustomerFilterExpressionTest {

    private BusinessPartnerCustomerFilterExpression filterExpression;

    @Mock
    private RequestContext mockRequestContext;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        filterExpression = new BusinessPartnerCustomerFilterExpression();
    }

    @Test
    public void testGetExpressionFinancialAccountWindowSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, "94EAA455D2644E04AB25D93BE5157B6D");
        requestMap.put("issotrx", "true");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }

    @Test
    public void testGetExpressionFinancialAccountWindowNonSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, "94EAA455D2644E04AB25D93BE5157B6D");
        requestMap.put("issotrx", "false");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrx_Y() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, "94EAA455D2644E04AB25D93BE5157B6D");
        requestMap.put("IsSOTrx", "Y");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }

    @Test
    public void testGetExpressionFinancialAccountWindowIsSOTrx_N() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put(OBBindingsConstants.WINDOW_ID_PARAM, "94EAA455D2644E04AB25D93BE5157B6D");
        requestMap.put("IsSOTrx", "N");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("", result);
    }

    @Test
    public void testGetExpressionNullWindowIdSalesTransaction() {
        // GIVEN
        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("issotrx", "true");

        // WHEN
        String result = filterExpression.getExpression(requestMap);

        // THEN
        assertEquals("true", result);
    }

}
