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
 * Test cases for {@link CreditToUseTransformer}
 */
@RunWith(MockitoJUnitRunner.class)
public class CreditToUseTransformerTest {

    @InjectMocks
    private CreditToUseTransformer transformer;

    private Map<String, String> requestParameters;
    private Map<String, Object> queryNamedParameters;

    @Before
    public void setUp() {
        requestParameters = new HashMap<>();
        queryNamedParameters = new HashMap<>();
    }

    @Test
    public void testTransformHqlQuery_replacesPlaceholders() {
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

    @Test
    public void testTransformHqlQuery_setsQueryParameters() {
        // Given
        String hqlQuery = "SELECT * FROM table WHERE 1=1 @whereClause@";
        String currencyId = "100";
        requestParameters.put("c_currency.id", currencyId);

        // When
        transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

        // Then
        assertTrue("Named parameters should contain currencyId",
                queryNamedParameters.containsKey("currencyId"));
    }

    @Test
    public void testTransformHqlQuery_withEmptyParameters() {
        // Given
        String hqlQuery = "SELECT @selectClause@ FROM table @joinClause@ WHERE 1=1 @whereClause@";

        // When
        String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

        // Then
        assertEquals("SELECT   FROM table   WHERE 1=1  and f.currency.id = :currencyId ", result);
    }
}