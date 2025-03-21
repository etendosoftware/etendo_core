package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Test cases for {@link CreditToUseTransformer}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CreditToUseTransformerTest {

  @InjectMocks
  private CreditToUseTransformer transformer;

  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();
  }

  /**
   * Tests the transformHqlQuery method to ensure it correctly replaces placeholders in the HQL query.
   */
  @Test
  public void testTransformHqlQueryReplacesPlaceholders() {
    // Given
    String hqlQuery = "SELECT @selectClause@ FROM table @joinClause@ WHERE 1=1 @whereClause@";
    String currencyId = "102";
    requestParameters.put("c_currency_id", currencyId);

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertEquals("SELECT   FROM table   WHERE 1=1  and f.currency.id = :currencyId ", result);
    assertEquals(currencyId, queryNamedParameters.get("currencyId"));
  }

  /**
   * Tests the transformHqlQuery method to ensure it sets the query parameters correctly.
   */
  @Test
  public void testTransformHqlQuerySetsQueryParameters() {
    // Given
    String hqlQuery = "SELECT * FROM table WHERE 1=1 @whereClause@";
    String currencyId = "100";
    requestParameters.put("c_currency.id", currencyId);

    // When
    transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Named parameters should contain currencyId", queryNamedParameters.containsKey("currencyId"));
  }

  /**
   * Tests the transformHqlQuery method with empty parameters to ensure it handles them correctly.
   */
  @Test
  public void testTransformHqlQueryWithEmptyParameters() {
    // Given
    String hqlQuery = "SELECT @selectClause@ FROM table @joinClause@ WHERE 1=1 @whereClause@";

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertEquals("SELECT   FROM table   WHERE 1=1  and f.currency.id = :currencyId ", result);
  }
}
