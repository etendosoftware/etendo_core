package com.smf.jobs.defaults.invoices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.smf.jobs.defaults.Utility;

@RunWith(MockitoJUnitRunner.class)
public class CreateFromOrdersHQLTransformerTest {

  private CreateFromOrdersHQLTransformer transformer;
  private String baseHqlQuery;

  /**
   * Sets up the test environment before each test method.
   * Initializes the query transformer and establishes a base query
   * to be used in tests.
   */
  @Before
  public  void setUp() {
    transformer = new CreateFromOrdersHQLTransformer();
    baseHqlQuery = "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@";
  }

  /**
   * Tests HQL query transformation when lines include taxes.
   * Verifies that when the 'linesIncludeTaxes' parameter is true,
   * the query is correctly transformed by replacing '@linesIncludeTaxes@' with 'Y'.
   */
  @Test
  public void testTransformHqlQueryWhenLinesIncludeTaxesIsTrue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y'", result);
  }

  /**
   * Tests HQL query transformation when lines do not include taxes.
   * Verifies that when the 'linesIncludeTaxes' parameter is false,
   * the query is correctly transformed by replacing '@linesIncludeTaxes@' with 'N'.
   */
  @Test
  public void testTransformHqlQueryWhenLinesIncludeTaxesIsFalse() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, "false");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation when the 'linesIncludeTaxes' parameter is missing.
   * Ensures that when no parameter is provided, the query defaults to 'N'
   * for tax inclusion.
   */
  @Test
  public void testTransformHqlQueryWhenLinesIncludeTaxesParameterIsMissing() {
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation with multiple replacements.
   * Verifies that when multiple occurrences of '@linesIncludeTaxes@'
   * are present in the query, they are all correctly replaced.
   */
  @Test
  public void testTransformHqlQueryWithMultipleReplacements() {
    String queryWithMultipleReplacements = "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@ AND otherField = @linesIncludeTaxes@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(queryWithMultipleReplacements, requestParameters, queryNamedParameters);
    assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y' AND otherField = 'Y'", result);
  }

  /**
   * Tests HQL query transformation with an invalid boolean value.
   * Checks that when an invalid boolean value is provided,
   * the query defaults to 'N' for tax inclusion.
   */
  @Test
  public void testTransformHqlQueryWithInvalidBooleanValue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, "invalid_boolean");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation with an empty query.
   * Ensures that an empty query remains unchanged during transformation.
   */
  @Test
  public void testTransformHqlQueryWithEmptyQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery("", requestParameters, queryNamedParameters);
    assertEquals("", result);
  }

  /**
   * Tests HQL query transformation with a null query.
   * Verifies that attempting to transform a null query
   * throws a NullPointerException.
   */
  @Test
  public void testTransformHqlQueryWithNullQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    try {
      transformer.transformHqlQuery(null, requestParameters, queryNamedParameters);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      // Expected exception
    }
  }
}
