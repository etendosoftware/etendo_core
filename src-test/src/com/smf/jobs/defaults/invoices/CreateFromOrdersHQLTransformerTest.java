package com.smf.jobs.defaults.invoices;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.smf.jobs.defaults.Utility;

/**
 * Unit test class for {@link CreateFromOrdersHQLTransformer}.
 * This class contains test cases to verify the behavior of the HQL query transformer
 * related to tax inclusion in orders.
 * The tests cover various scenarios, including:
 * <ul>
 *   <li>Query transformation when taxes are included</li>
 *   <li>Query transformation when taxes are not included</li>
 *   <li>Handling of missing parameters</li>
 *   <li>Multiple replacements in the query</li>
 *   <li>Handling of invalid boolean values</li>
 *   <li>Management of empty or null queries</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
public class CreateFromOrdersHQLTransformerTest {

  private CreateFromOrdersHQLTransformer transformer;
  private String baseHqlQuery;

  /**
   * Sets up the test environment before each test method.
   * Initializes the query transformer and establishes a base query
   * to be used in tests.
   */
  @BeforeEach
  void setUp() {
    transformer = new CreateFromOrdersHQLTransformer();
    baseHqlQuery = "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@";
  }

  /**
   * Tests HQL query transformation when lines include taxes.
   * Verifies that when the 'linesIncludeTaxes' parameter is true,
   * the query is correctly transformed by replacing '@linesIncludeTaxes@' with 'Y'.
   */
  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesIsTrue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    Assertions.assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y'", result);
  }

  /**
   * Tests HQL query transformation when lines do not include taxes.
   * Verifies that when the 'linesIncludeTaxes' parameter is false,
   * the query is correctly transformed by replacing '@linesIncludeTaxes@' with 'N'.
   */
  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesIsFalse() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, "false");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    Assertions.assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation when the 'linesIncludeTaxes' parameter is missing.
   * Ensures that when no parameter is provided, the query defaults to 'N'
   * for tax inclusion.
   */
  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesParameterIsMissing() {
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    Assertions.assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation with multiple replacements.
   * Verifies that when multiple occurrences of '@linesIncludeTaxes@'
   * are present in the query, they are all correctly replaced.
   */
  @Test
  void testTransformHqlQueryWithMultipleReplacements() {
    String queryWithMultipleReplacements =
        "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@ AND otherField = @linesIncludeTaxes@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(queryWithMultipleReplacements, requestParameters, queryNamedParameters);

    Assertions.assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y' AND otherField = 'Y'", result);
  }

  /**
   * Tests HQL query transformation with an invalid boolean value.
   * Checks that when an invalid boolean value is provided,
   * the query defaults to 'N' for tax inclusion.
   */
  @Test
  void testTransformHqlQueryWithInvalidBooleanValue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, "invalid_boolean");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    Assertions.assertEquals(Utility.QUERY_ORDERS_WITHOUT_TAX, result);
  }

  /**
   * Tests HQL query transformation with an empty query.
   * Ensures that an empty query remains unchanged during transformation.
   */
  @Test
  void testTransformHqlQueryWithEmptyQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery("", requestParameters, queryNamedParameters);

    Assertions.assertEquals("", result);
  }

  /**
   * Tests HQL query transformation with a null query.
   * Verifies that attempting to transform a null query
   * throws a NullPointerException.
   */
  @Test
  void testTransformHqlQueryWithNullQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put(Utility.LINES_INCLUDE_TAXES, Utility.TRUE);
    Map<String, Object> queryNamedParameters = new HashMap<>();

    assertThrows(NullPointerException.class, () ->
      transformer.transformHqlQuery(null, requestParameters, queryNamedParameters)
    );
  }
}