package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.application.OBBindingsConstants;

@RunWith(MockitoJUnitRunner.class)
public class MatchStatementFilterExpressionTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private Instance<MatchStatementFilterExpressionHandler> matchStatementFilterExpressionHandlers;

    @Mock
    private MatchStatementFilterExpressionHandler mockHandler;

    @InjectMocks
    private MatchStatementFilterExpression filterExpression;

    private Map<String, String> requestMap;
    private static final String TEST_WINDOW_ID = "TEST_WINDOW_ID";

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Initialize request map
        requestMap = new HashMap<>();
        JSONObject context = new JSONObject();
        context.put(OBBindingsConstants.WINDOW_ID_PARAM, TEST_WINDOW_ID);
        requestMap.put("context", context.toString());
        requestMap.put("filterExpressionColumnName", "cleared");

        // Setup mock handler
        when(matchStatementFilterExpressionHandlers.select(any())).thenReturn(matchStatementFilterExpressionHandlers);
        when(matchStatementFilterExpressionHandlers.iterator())
            .thenReturn(java.util.Collections.singletonList(mockHandler).iterator());
    }

    @Test
    public void testGetExpression_WithValidHandler_ReturnsHandlerExpression() throws JSONException {
        // Given
        String expectedExpression = "TestExpression";
        when(mockHandler.getFilterExpression(requestMap)).thenReturn(expectedExpression);

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return handler's expression", expectedExpression, result);
        verify(mockHandler).getFilterExpression(requestMap);
    }

    @Test
    public void testGetExpression_WithNullHandlerExpression_ReturnsNo() throws JSONException {
        // Given
        when(mockHandler.getFilterExpression(requestMap)).thenReturn(null);

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return 'No' when handler returns null", "No", result);
        verify(mockHandler).getFilterExpression(requestMap);
    }

    @Test
    public void testGetExpression_WithEmptyHandlerExpression_ReturnsNo() throws JSONException {
        // Given
        when(mockHandler.getFilterExpression(requestMap)).thenReturn("");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return 'No' when handler returns empty string", "No", result);
        verify(mockHandler).getFilterExpression(requestMap);
    }

    @Test
    public void testGetExpression_WithInvalidColumn_ReturnsEmptyString() throws JSONException {
        // Given
        requestMap.put("filterExpressionColumnName", "invalidColumn");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return empty string for invalid column", "", result);
        verify(mockHandler, never()).getFilterExpression(any());
    }

    @Test
    public void testGetExpression_WithInvalidContext_ReturnsEmptyString() throws JSONException {
        // Given
        requestMap.put("context", "invalid json");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return empty string for invalid context", "", result);
        verify(mockHandler, never()).getFilterExpression(any());
    }

    @Test
    public void testGetExpression_WithMissingContext_ReturnsEmptyString() throws JSONException {
        // Given
        requestMap.remove("context");

        // When
        String result = filterExpression.getExpression(requestMap);

        // Then
        assertEquals("Should return empty string for missing context", "", result);
        verify(mockHandler, never()).getFilterExpression(any());
    }
}