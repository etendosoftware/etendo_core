package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for AccountingFactEndYearTransformer
 * Tests the HQL query transformation for End Year Close table
 */
@RunWith(MockitoJUnitRunner.class)
public class AccountingFactEndYearTransformerTest {

  private static final String CLIENT_ID = "clientId";
  private static final String UPPER_MAX_FA_DESCRIPTION = "upper(Max(fa.description))";
  private static final String HAVING_PARAM_0 = "havingParam_0";
  private static final String TEST = "%test%";
  private static final String TEST_CLIENT = "test-client";
  private static final String FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0 = "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0}";
  private static final String HAVING = "HAVING";
  private static final String CRITERIA = "criteria";
  private static final String ALIAS_0 = "alias_0";
  private static final String ALIAS_1 = "alias_1";
  private static final String FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST = "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"}";
  private static final String HAVING_PARAM = "havingParam_";
  private AccountingFactEndYearTransformer transformer;
  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

  /**
   * Set up test fixtures before each test.
   * Initializes transformer instance, parameter maps, and base HQL query.
   */
  @Before
  public void setUp() {
    transformer = new AccountingFactEndYearTransformer();
    requestParameters = new HashMap<>();
    queryNamedParameters = new HashMap<>();
    
    // Base HQL query similar to the real one
    baseHqlQuery = "SELECT max(fa.id) AS Fact_Acct_End_Year_V_ID, "
        + "fa.client.id AS AD_Client_ID, "
        + "pc.organization.id AS AD_Org_ID, "
        + "Max(fa.creationDate) AS Created, "
        + "Max(fa.description) as Description, "
        + "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END AS Debit, "
        + "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END AS Credit "
        + "FROM FinancialMgmtAccountingFact fa "
        + "JOIN fa.period p "
        + "WHERE fa.type IN ('O', 'C') "
        + "and fa.client.id in ('0', :clientId) "
        + "GROUP BY fa.client.id, pc.id "
        + "HAVING Sum(fa.credit - fa.debit) <> 0 "
        + "ORDER BY fa.type DESC";
  }

  /**
   * Verifies that query is returned unchanged when no criteria parameter is provided.
   */
  @Test
  public void testNoCriteriaParameterReturnsOriginalQuery() {
    // No criteria parameter in request
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertEquals("Should return original query when no criteria", baseHqlQuery, result);
  }

  /**
   * Verifies that empty criteria parameter returns a valid result.
   */
  @Test
  public void testEmptyCriteriaReturnsOriginalQuery() {
    requestParameters.put(CRITERIA, "");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertNotNull("Result should not be null", result);
  }

  /**
   * Verifies that filtering by debit adds a HAVING clause with the correct aggregate expression.
   */
  @Test
  public void testDebitFilterOnlyAddsHavingClause() {
    // Criteria with debit filter
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should contain debit condition", result.contains("CASE WHEN Sum(fa.debit - fa.credit)"));
    assertTrue("Should contain havingParam", result.contains(HAVING_PARAM));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey(HAVING_PARAM_0));
    assertEquals("Parameter should be BigDecimal", BigDecimal.valueOf(100.0), queryNamedParameters.get(HAVING_PARAM_0));
  }

  /**
   * Verifies that filtering by credit adds a HAVING clause with the correct aggregate expression.
   */
  @Test
  public void testCreditFilterOnlyAddsHavingClause() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"credit\",\"operator\":\"lessThan\",\"value\":500.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should contain credit condition", result.contains("CASE WHEN Sum(fa.credit - fa.debit)"));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey(HAVING_PARAM_0));
  }

  /**
   * Verifies that multiple debit and credit filters are combined with AND operator.
   */
  @Test
  public void testDebitAndCreditFiltersCombinesWithAnd() {
    requestParameters.put(CRITERIA,
        "{\"operator\":\"and\",\"_constructor\":\"AdvancedCriteria\",\"criteria\":["
        + FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0 + ","
        + "{\"fieldName\":\"credit\",\"operator\":\"lessThan\",\"value\":500.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain AND operator", result.contains(" AND "));
    assertTrue("Should have two parameters", queryNamedParameters.size() >= 2);
    assertTrue("Should have havingParam_0", queryNamedParameters.containsKey(HAVING_PARAM_0));
    assertTrue("Should have havingParam_1", queryNamedParameters.containsKey("havingParam_1"));
  }

  /**
   * Verifies that multiple debit and credit filters are combined with OR operator.
   */
  @Test
  public void testDebitOrCreditFiltersCombinesWithOr() {
    requestParameters.put(CRITERIA,
        "{\"operator\":\"or\",\"_constructor\":\"AdvancedCriteria\",\"criteria\":["
        + "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0},"
        + "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":500.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain OR operator", result.contains(" OR "));
  }

  /**
   * Verifies that aggregate conditions on description field are removed from WHERE clause.
   */
  @Test
  public void testDescriptionFilterWithAggregateRemovesFromWhere() {
    String queryWithAggregateFilter = "SELECT max(fa.id), Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (( upper(Max(fa.description)) like upper(:alias_0) escape '|' )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithAggregateFilter, requestParameters, queryNamedParameters);
    
    // Verify the aggregate condition was removed from WHERE
    assertFalse("Should not contain upper(Max(fa.description)) in result", result.contains(UPPER_MAX_FA_DESCRIPTION));
    assertFalse("Should have removed alias_0 parameter", queryNamedParameters.containsKey(ALIAS_0));
    assertTrue("Should keep clientId parameter", queryNamedParameters.containsKey(CLIENT_ID));
    // The WHERE clause should still exist with clientId condition
    assertTrue("Should contain WHERE clause", result.contains("WHERE"));
    assertTrue("Should contain clientId condition", result.contains("fa.client.id in (:clientId)"));
  }

  /**
   * Verifies that multiple aggregate conditions are all removed from WHERE clause.
   */
  @Test
  public void testMultipleAggregateConditionsRemovesAll() {
    String queryWithMultipleAggregates = "SELECT max(fa.id), Max(fa.description), Max(fa.creationDate) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND upper(Max(fa.description)) like upper(:alias_0) escape '|' "
        + "AND Max(fa.creationDate) > :alias_1 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, TEST);
    queryNamedParameters.put(ALIAS_1, "2024-01-01");
    requestParameters.put(CRITERIA,
        "{\"operator\":\"and\",\"criteria\":["
        + FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST + ","
        + "{\"fieldName\":\"creationDate\",\"operator\":\"greaterThan\",\"value\":\"2024-01-01\"}"
        + "]}");
    
    String result = transformer.transformHqlQuery(queryWithMultipleAggregates, requestParameters, queryNamedParameters);
    
    // Extract the WHERE clause to verify aggregate removal (not SELECT clause)
    int whereIndex = result.indexOf("WHERE");
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain upper(Max(fa.description)) in WHERE", whereClause.contains(UPPER_MAX_FA_DESCRIPTION));
    assertFalse("Should not contain Max(fa.creationDate) > in WHERE", whereClause.contains("Max(fa.creationDate) >"));
    assertFalse("Should have removed alias_0", queryNamedParameters.containsKey(ALIAS_0));
    assertFalse("Should have removed alias_1", queryNamedParameters.containsKey(ALIAS_1));
    assertTrue("Should contain 1=1 replacements", whereClause.contains("1=1"));
  }

  /**
   * Verifies that nested criteria groups are handled correctly with proper parentheses.
   */
  @Test
  public void testNestedCriteriaHandlesCorrectly() {
    requestParameters.put(CRITERIA,
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"operator\":\"or\",\"criteria\":["
        + FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0 + ","
        + "{\"fieldName\":\"debit\",\"operator\":\"lessThan\",\"value\":50.0}"
        + "]},"
        + "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":200.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain parentheses for nesting", result.contains("(") && result.contains(")"));
    assertTrue("Should have multiple parameters", queryNamedParameters.size() >= 3);
  }

  /**
   * Verifies that all comparison operators (equals, notEqual, greaterOrEqual, lessOrEqual) work correctly.
   */
  @Test
  public void testDifferentOperatorsAllHandled() {
    // Test equals operator
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0}");
    String result1 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle equals", result1.contains(" = :"));
    
    // Test notEqual operator
    queryNamedParameters.clear();
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"notEqual\",\"value\":100.0}");
    String result2 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle notEqual", result2.contains(" <> :"));
    
    // Test greaterOrEqual operator
    queryNamedParameters.clear();
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"greaterOrEqual\",\"value\":100.0}");
    String result3 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle greaterOrEqual", result3.contains(" >= :"));
    
    // Test lessOrEqual operator
    queryNamedParameters.clear();
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"lessOrEqual\",\"value\":100.0}");
    String result4 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle lessOrEqual", result4.contains(" <= :"));
  }

  /**
   * Verifies that Integer values are properly converted to BigDecimal for parameter binding.
   */
  @Test
  public void testIntegerValueConvertsToBigDecimal() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100}");
    
    transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    Object paramValue = queryNamedParameters.get(HAVING_PARAM_0);
    assertTrue("Should convert Integer to BigDecimal", paramValue instanceof BigDecimal);
    assertEquals("Value should match", BigDecimal.valueOf(100), paramValue);
  }

  /**
   * Verifies that Long values are properly converted to BigDecimal for parameter binding.
   */
  @Test
  public void testLongValueConvertsToBigDecimal() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":9999999999}");
    
    transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    Object paramValue = queryNamedParameters.get(HAVING_PARAM_0);
    assertTrue("Should convert Long to BigDecimal", paramValue instanceof BigDecimal);
  }

  /**
   * Verifies that non-aggregate fields do not add HAVING parameters.
   */
  @Test
  public void testNullValueSkipsCondition() {
    // Null values in JSON are parsed as missing, not as null objects
    // Test with a query that has no debit/credit filters (only other fields)
    requestParameters.put(CRITERIA, "{\"fieldName\":\"clientId\",\"operator\":\"equals\",\"value\":\"test\"}");
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    // Should not add HAVING clause since clientId is not debit/credit
    assertFalse("Should not add havingParam for non-aggregate field", result.contains(HAVING_PARAM));
    assertTrue("Should keep clientId parameter", queryNamedParameters.containsKey(CLIENT_ID));
  }

  /**
   * Verifies that new conditions are appended to existing HAVING clause with AND operator.
   */
  @Test
  public void testExistingHavingClauseAppendsWithAnd() {
    String queryWithHaving = baseHqlQuery; // Already has HAVING clause
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithHaving, requestParameters, queryNamedParameters);

    assertTrue("Should append to existing HAVING", result.contains(HAVING));
    assertTrue("Should use AND to combine", result.contains(" AND "));
  }

  /**
   * Verifies that HAVING clause is added even when query has no GROUP BY.
   */
  @Test
  public void testNoGroupByStillProcesses() {
    String simpleQuery = "SELECT fa.id FROM FinancialMgmtAccountingFact fa WHERE fa.client.id = :clientId";
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(simpleQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should add HAVING even without GROUP BY", result.contains(HAVING));
  }

  /**
   * Verifies that LIKE conditions with escape clauses are properly removed including the escape part.
   */
  @Test
  public void testEscapeClauseInLikeHandledCorrectly() {
    String queryWithEscape = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE upper(Max(fa.description)) like upper(:alias_0) escape '|' and fa.client.id = :clientId "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, TEST);
    queryNamedParameters.put(CLIENT_ID, "test");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithEscape, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove entire condition including escape", result.contains("escape '|'"));
    assertFalse("Should not have standalone escape keyword", result.matches(".*\\s+escape\\s+.*"));
  }

  /**
   * Verifies that complex queries with nested parentheses maintain correct structure after transformation.
   */
  @Test
  public void testComplexNestedParenthesesHandledCorrectly() {
    String complexQuery = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (( upper(Max(fa.description)) like upper(:alias_0) escape '|' and fa.type = :alias_1 )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, TEST);
    queryNamedParameters.put(ALIAS_1, "O");
    requestParameters.put(CRITERIA,
        "{\"operator\":\"and\",\"criteria\":["
        + FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST + ","
        + "{\"fieldName\":\"type\",\"operator\":\"equals\",\"value\":\"O\"}"
        + "]}");
    
    String result = transformer.transformHqlQuery(complexQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Max aggregate condition", result.contains(UPPER_MAX_FA_DESCRIPTION));
    assertTrue("Should keep non-aggregate condition", result.contains("fa.type = :alias_1"));
    assertFalse("Should have removed alias_0", queryNamedParameters.containsKey(ALIAS_0));
    assertTrue("Should keep alias_1", queryNamedParameters.containsKey(ALIAS_1));
  }

  /**
   * Verifies that SUM aggregate function is removed from WHERE clause.
   */
  @Test
  public void testSumAggregateInWhereRemoved() {
    String queryWithSum = "SELECT Sum(fa.amount) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Sum(fa.amount) > :alias_0 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, "100");
    requestParameters.put(CRITERIA, "{\"fieldName\":\"amount\",\"operator\":\"greaterThan\",\"value\":100}");
    
    String result = transformer.transformHqlQuery(queryWithSum, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Sum from WHERE", result.contains("WHERE Sum(fa.amount)"));
    assertFalse("Should remove alias_0", queryNamedParameters.containsKey(ALIAS_0));
  }

  /**
   * Verifies that AVG aggregate function is removed from WHERE clause.
   */
  @Test
  public void testAvgAggregateInWhereRemoved() {
    String queryWithAvg = "SELECT Avg(fa.amount) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Avg(fa.amount) < :alias_0 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, "50");
    requestParameters.put(CRITERIA, "{\"fieldName\":\"avgAmount\",\"operator\":\"lessThan\",\"value\":50}");
    
    String result = transformer.transformHqlQuery(queryWithAvg, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Avg from WHERE", result.contains("WHERE Avg(fa.amount)"));
  }

  /**
   * Verifies that MIN, MAX, and COUNT aggregate functions are all removed from WHERE clause.
   */
  @Test
  public void testMinMaxCountAggregatesAllRemoved() {
    String queryWithMultiple = "SELECT Min(fa.id), Max(fa.id), Count(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Min(fa.id) > :alias_0 and Max(fa.id) < :alias_1 and Count(fa.id) = :alias_2 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, "1");
    queryNamedParameters.put(ALIAS_1, "100");
    queryNamedParameters.put("alias_2", "10");
    requestParameters.put(CRITERIA, "{\"fieldName\":\"id\",\"operator\":\"equals\",\"value\":1}");
    
    String result = transformer.transformHqlQuery(queryWithMultiple, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Min", result.contains("Min(fa.id) >"));
    assertFalse("Should remove Max", result.contains("Max(fa.id) <"));
    assertFalse("Should remove Count", result.contains("Count(fa.id) ="));
    assertEquals("Should remove all alias parameters", 0, queryNamedParameters.size());
  }

  /**
   * Verifies that malformed JSON criteria returns the original query without throwing exceptions.
   */
  @Test
  public void testMalformedJSONHandlesGracefully() {
    requestParameters.put(CRITERIA, "{malformed json");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertEquals("Should return original query on error", baseHqlQuery, result);
  }

  /**
   * Verifies that criteria without fieldName property are skipped gracefully.
   */
  @Test
  public void testCriteriaWithoutFieldNameSkipped() {
    requestParameters.put(CRITERIA, "{\"operator\":\"equals\",\"value\":100}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should not add HAVING", result.contains(HAVING_PARAM));
  }

  /**
   * Verifies that non-aggregate fields do not trigger HAVING clause generation.
   */
  @Test
  public void testNonDebitCreditFieldNoHavingAdded() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"clientId\",\"operator\":\"equals\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should not add HAVING for non-aggregate fields", result.contains(HAVING_PARAM));
  }

  /**
   * Verifies that ORDER BY clause is preserved and placed after HAVING clause.
   */
  @Test
  public void testOrderByPreservedWhenAddingHaving() {
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should preserve ORDER BY", result.contains("ORDER BY"));
    int havingIndex = result.indexOf(HAVING);
    int orderByIndex = result.indexOf("ORDER BY");
    assertTrue("HAVING should come before ORDER BY", havingIndex < orderByIndex);
  }

  /**
   * Verifies that operators are handled case-insensitively.
   */
  @Test
  public void testCaseInsensitiveOperatorsWork() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"GREATERTHAN\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should handle uppercase operators", result.contains(" > :"));
  }

  /**
   * Verifies that iEquals operator is translated to equals (=) in SQL.
   */
  @Test
  public void testIEqualsOperatorUsesEquals() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"iEquals\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should use = for iEquals", result.contains(" = :"));
  }

  /**
   * Verifies that empty parentheses are removed during query cleanup.
   */
  @Test
  public void testEmptyParenthesesCleanedUp() {
    String queryWithEmptyParens = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id = :clientId AND (( upper(Max(fa.description)) like upper(:alias_0) )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithEmptyParens, requestParameters, queryNamedParameters);
    
    assertFalse("Should not have empty double parentheses", result.contains("(())"));
  }

  /**
   * Verifies that multiple aggregate conditions are handled correctly across iterations.
   */
  @Test
  public void testMultipleIterationsHandlesCorrectly() {
    // Create a query with many aggregate conditions to test iteration logic
    StringBuilder query = new StringBuilder("SELECT max(fa.id) FROM FinancialMgmtAccountingFact fa WHERE ");
    for (int i = 0; i < 5; i++) {
      if (i > 0) query.append(" and ");
      query.append("Max(fa.field").append(i).append(") = :alias_").append(i);
      queryNamedParameters.put("alias_" + i, "value" + i);
    }
    query.append(" GROUP BY fa.client.id");
    
    requestParameters.put(CRITERIA, "{\"fieldName\":\"test\",\"operator\":\"equals\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(query.toString(), requestParameters, queryNamedParameters);
    
    for (int i = 0; i < 5; i++) {
      assertFalse("Should remove all aggregate conditions", result.contains("Max(fa.field" + i + ")"));
      assertFalse("Should remove all alias parameters", queryNamedParameters.containsKey("alias_" + i));
    }
  }
}
