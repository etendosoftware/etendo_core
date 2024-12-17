package com.smf.jobs.defaults.invoices;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CreateFromOrdersHQLTransformerTest {

  private CreateFromOrdersHQLTransformer transformer;
  private String baseHqlQuery;

  @BeforeEach
  void setUp() {
    transformer = new CreateFromOrdersHQLTransformer();
    baseHqlQuery = "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@";
  }

  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesIsTrue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "true");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y'", result);
  }

  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesIsFalse() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "false");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT * FROM Orders WHERE includeTax = 'N'", result);
  }

  @Test
  void testTransformHqlQueryWhenLinesIncludeTaxesParameterIsMissing() {
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT * FROM Orders WHERE includeTax = 'N'", result);
  }

  @Test
  void testTransformHqlQueryWithMultipleReplacements() {
    String queryWithMultipleReplacements =
        "SELECT * FROM Orders WHERE includeTax = @linesIncludeTaxes@ AND otherField = @linesIncludeTaxes@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "true");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(queryWithMultipleReplacements, requestParameters, queryNamedParameters);

    assertEquals("SELECT * FROM Orders WHERE includeTax = 'Y' AND otherField = 'Y'", result);
  }

  @Test
  void testTransformHqlQueryWithInvalidBooleanValue() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "invalid_boolean");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);

    assertEquals("SELECT * FROM Orders WHERE includeTax = 'N'", result);
  }

  @Test
  void testTransformHqlQueryWithEmptyQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "true");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String result = transformer.transformHqlQuery("", requestParameters, queryNamedParameters);

    assertEquals("", result);
  }

  @Test
  void testTransformHqlQueryWithNullQuery() {
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("linesIncludeTaxes", "true");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    assertThrows(NullPointerException.class, () -> {
      transformer.transformHqlQuery(null, requestParameters, queryNamedParameters);
    });
  }
}