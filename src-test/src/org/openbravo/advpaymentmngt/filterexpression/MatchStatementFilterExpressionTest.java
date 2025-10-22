package org.openbravo.advpaymentmngt.filterexpression;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.inject.Instance;

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
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.client.application.OBBindingsConstants;

/**
 * Unit tests for the MatchStatementFilterExpression class.
 */
@RunWith(MockitoJUnitRunner.class)
public class MatchStatementFilterExpressionTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Instance<MatchStatementFilterExpressionHandler> matchStatementFilterExpressionHandlers;

  @Mock
  private MatchStatementFilterExpressionHandler mockHandler;

  @InjectMocks
  private MatchStatementFilterExpression filterExpression;

  private Map<String, String> requestMap;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Initialize request map
    requestMap = new HashMap<>();
    JSONObject context = new JSONObject();
    context.put(OBBindingsConstants.WINDOW_ID_PARAM, "TEST_WINDOW_ID");
    requestMap.put(TestConstants.CONTEXT, context.toString());
    requestMap.put("filterExpressionColumnName", "cleared");

    // Setup mock handler
    when(matchStatementFilterExpressionHandlers.select(any())).thenReturn(matchStatementFilterExpressionHandlers);
    when(matchStatementFilterExpressionHandlers.iterator()).thenReturn(
        java.util.Collections.singletonList(mockHandler).iterator());
  }

  /**
   * Tests the getExpression method with a valid handler that returns a handler expression.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithValidHandlerReturnsHandlerExpression() throws JSONException {
    // Given
    String expectedExpression = "TestExpression";
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(expectedExpression);

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return handler's expression", expectedExpression, result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  /**
   * Tests the getExpression method with a null handler expression, expecting "No".
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithNullHandlerExpressionReturnsNo() throws JSONException {
    // Given
    when(mockHandler.getFilterExpression(requestMap)).thenReturn(null);

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return 'No' when handler returns null", "No", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  /**
   * Tests the getExpression method with an empty handler expression, expecting "No".
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithEmptyHandlerExpressionReturnsNo() throws JSONException {
    // Given
    when(mockHandler.getFilterExpression(requestMap)).thenReturn("");

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return 'No' when handler returns empty string", "No", result);
    verify(mockHandler).getFilterExpression(requestMap);
  }

  /**
   * Tests the getExpression method with an invalid column, expecting an empty string.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithInvalidColumnReturnsEmptyString() throws JSONException {
    // Given
    requestMap.put("filterExpressionColumnName", "invalidColumn");

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return empty string for invalid column", "", result);
    verify(mockHandler, never()).getFilterExpression(any());
  }

  /**
   * Tests the getExpression method with invalid JSON in the context, expecting an empty string.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithInvalidContextReturnsEmptyString() throws JSONException {
    // Given
    requestMap.put(TestConstants.CONTEXT, "invalid json");

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return empty string for invalid context", "", result);
    verify(mockHandler, never()).getFilterExpression(any());
  }

  /**
   * Tests the getExpression method with a missing context, expecting an empty string.
   *
   * @throws JSONException
   *     if a JSON error occurs
   */
  @Test
  public void testGetExpressionWithMissingContextReturnsEmptyString() throws JSONException {
    // Given
    requestMap.remove(TestConstants.CONTEXT);

    // When
    String result = filterExpression.getExpression(requestMap);

    // Then
    assertEquals("Should return empty string for missing context", "", result);
    verify(mockHandler, never()).getFilterExpression(any());
  }
}
