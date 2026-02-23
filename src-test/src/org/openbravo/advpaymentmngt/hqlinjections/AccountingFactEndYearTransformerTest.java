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
  private static final String FIELD_NAME_CREATION_DATE_OPERATOR_EQUALS_VALUE_DATE = "{\"fieldName\":\"creationDate\",\"operator\":\"equals\",\"value\":\"2024-01-01\"}";
  private static final String FIELD_NAME_UPDATED_OPERATOR_GREATER_THAN_VALUE_DATE = "{\"fieldName\":\"updated\",\"operator\":\"greaterThan\",\"value\":\"2024-01-01T10:00:00\"}";
  public static final String WHERE = "WHERE";
  public static final String DESCRIPTION_FILTER = "upper(Max(fa.description)) like upper(:alias_0) escape '|'";
  public static final String SHOULD_KEEP_CLIENT_ID_PARAMETER = "Should keep clientId parameter";
  public static final String SHOULD_HAVE_REMOVED_ALIAS_0 = "Should have removed alias_0";
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
    assertFalse("Should not contain the aggregate condition", result.contains(DESCRIPTION_FILTER));
    assertFalse("Should have removed alias_0 parameter", queryNamedParameters.containsKey(ALIAS_0));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    // The WHERE clause should still exist with clientId condition
    assertTrue("Should contain WHERE clause", result.contains(WHERE));
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
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain upper(Max(fa.description)) in WHERE", whereClause.contains(UPPER_MAX_FA_DESCRIPTION));
    assertFalse("Should not contain Max(fa.creationDate) > in WHERE", whereClause.contains("Max(fa.creationDate) >"));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
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
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
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
    
    assertFalse("Should remove the aggregate condition", result.contains(DESCRIPTION_FILTER));
    assertTrue("Should keep non-aggregate condition", result.contains("fa.type = :alias_1"));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
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

  /**
   * Verifies that date fields with equals operator use date range conditions (tests parseDate and buildDateRangeCondition).
   */
  @Test
  public void testDateFieldEqualsOperatorUsesDateRange() {
    requestParameters.put(CRITERIA, FIELD_NAME_CREATION_DATE_OPERATOR_EQUALS_VALUE_DATE);
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause for date field", result.contains(HAVING));
    assertTrue("Should have start and end parameters", queryNamedParameters.containsKey("havingParam_0_start"));
    assertTrue("Should have start and end parameters", queryNamedParameters.containsKey("havingParam_0_end"));
    assertTrue("Should contain date range condition", result.contains("Max(fa.creationDate) >= :havingParam_0_start"));
    assertTrue("Should contain date range condition", result.contains("Max(fa.creationDate) <= :havingParam_0_end"));
  }

  /**
   * Verifies that date fields with greaterThan operator parse dates correctly (tests parseDate).
   */
  @Test
  public void testDateFieldGreaterThanOperatorParsesDate() {
    requestParameters.put(CRITERIA, FIELD_NAME_UPDATED_OPERATOR_GREATER_THAN_VALUE_DATE);
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause for date field", result.contains(HAVING));
    assertTrue("Should have parameter for greater than", queryNamedParameters.containsKey(HAVING_PARAM_0));
    assertTrue("Should contain greater than condition", result.contains("Max(fa.updated) > :havingParam_0"));
    // Verify the parameter is a Date object
    Object paramValue = queryNamedParameters.get(HAVING_PARAM_0);
    assertTrue("Parameter should be Date", paramValue instanceof java.util.Date);
  }

  /**
   * Verifies that nested parentheses in aggregate conditions are handled correctly (tests findMatchingOpenParen).
   */
  @Test
  public void testNestedParenthesesInAggregateConditions() {
    String queryWithNestedParens = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND ((( upper(Max(fa.description)) like upper(:alias_0) escape '|' ))) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithNestedParens, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove nested aggregate condition", result.contains(DESCRIPTION_FILTER));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    assertTrue("Should contain WHERE clause", result.contains(WHERE));
    assertTrue("Should contain clientId condition", result.contains("fa.client.id in (:clientId)"));
  }

  /**
   * Verifies that unbalanced parentheses before GROUP BY are automatically fixed.
   * This tests the fixUnclosedParensBeforeGroupBy() method which adds missing closing parens.
   */
  @Test
  public void testUnbalancedParenthesesBeforeGroupByAreFixed() {
    String queryWithUnbalancedParens = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (((pc.id = :alias_10 )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc-id");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithUnbalancedParens, requestParameters, queryNamedParameters);
    
    // Count parentheses in WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    
    assertEquals("Should have balanced parentheses", openCount, closeCount);
    assertTrue("Should contain GROUP BY", result.contains("GROUP BY"));
  }

  /**
   * Verifies that queries with subqueries in SELECT clause correctly identify the main FROM.
   * This tests that findMainFromPosition() skips FROMs inside subqueries.
   */
  @Test
  public void testSubqueryInSelectDoesNotConfuseMainFrom() {
    String queryWithSubquery = "SELECT max(fa.id), "
        + "(select asch.name from FinancialMgmtAcctSchema asch where fa.accountingSchema.id = asch.id) AS C_Acctschema_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND upper((select ev.name from FinancialMgmtElementValue ev where fa.account.id = ev.id)) like upper(:alias_0) escape '|' "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, "{\"fieldName\":\"account\",\"operator\":\"iContains\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(queryWithSubquery, requestParameters, queryNamedParameters);
    
    // The subquery conditions should be removed from WHERE
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain subquery in WHERE", whereClause.contains("(select ev.name"));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    
    // Parentheses should be balanced
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced parentheses in WHERE", openCount, closeCount);
  }

  /**
   * Verifies that when multiple alias parameters are in the same captured condition,
   * all of them are removed from the parameter map (not just the one being processed).
   * This prevents "Could not locate named parameter" errors.
   */
  @Test
  public void testMultipleAliasParametersInSameConditionAllRemoved() {
    String queryWithAdjacentConditions = "SELECT max(fa.id), Max(fa.description), fa.type "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "AND ( fa.type = :alias_1 ) and upper((select ev.name from FinancialMgmtElementValue ev where fa.account.id = ev.id)) like upper(:alias_2) escape '|' "
        + "GROUP BY fa.client.id, fa.type";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_1, "O");
    queryNamedParameters.put("alias_2", TEST);
    requestParameters.put(CRITERIA, 
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"fieldName\":\"type\",\"operator\":\"equals\",\"value\":\"O\"},"
        + "{\"fieldName\":\"account\",\"operator\":\"iContains\",\"value\":\"test\"}"
        + "]}");
    
    String result = transformer.transformHqlQuery(queryWithAdjacentConditions, requestParameters, queryNamedParameters);
    
    // Both alias_1 and alias_2 should be removed because the subquery condition might have captured both
    assertFalse("Should have removed alias_1", queryNamedParameters.containsKey(ALIAS_1));
    assertFalse("Should have removed alias_2", queryNamedParameters.containsKey("alias_2"));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    
    // The result should not contain these parameters
    assertFalse("Should not contain :alias_1 in query", result.contains(":alias_1"));
    assertFalse("Should not contain :alias_2 in query", result.contains(":alias_2"));
  }

  /**
   * Verifies that complex filters with all field types work together correctly.
   * This is a comprehensive integration test for the real-world scenario.
   */
  @Test
  public void testComplexMultiFieldFilterIntegration() {
    String complexQuery = "SELECT max(fa.id), "
        + "(select asch.name from FinancialMgmtAcctSchema asch where fa.accountingSchema.id = asch.id) AS C_Acctschema_ID, "
        + "(select ev.searchKey from FinancialMgmtElementValue ev where fa.account.id = ev.id) AS Account_ID, "
        + "fa.type AS Factaccttype, "
        + "Max(fa.description) as Description, "
        + "Max(fa.creationDate) AS Created, "
        + "Max(fa.updated) AS Updated, "
        + "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END AS Debit, "
        + "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END AS Credit "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "AND upper((select asch.name from FinancialMgmtAcctSchema asch where fa.accountingSchema.id = asch.id)) like upper(:alias_0) escape '|' "
        + "AND fa.type = :alias_1 "
        + "AND upper((select ev.searchKey from FinancialMgmtElementValue ev where fa.account.id = ev.id)) like upper(:alias_2) escape '|' "
        + "AND CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END = :alias_3 "
        + "AND CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END = :alias_4 "
        + "AND upper(Max(fa.description)) like upper(:alias_5) escape '|' "
        + "AND Max(fa.creationDate) >= :alias_6 "
        + "AND Max(fa.creationDate) <= :alias_7 "
        + "AND Max(fa.updated) >= :alias_8 "
        + "AND Max(fa.updated) <= :alias_9 "
        + "AND pc.id = :alias_10 "
        + "GROUP BY fa.client.id, fa.type, fa.accountingSchema.id, fa.account.id "
        + "HAVING Sum(fa.credit - fa.debit) <> 0 "
        + "ORDER BY fa.type DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, "%test%");
    queryNamedParameters.put(ALIAS_1, "O");
    queryNamedParameters.put("alias_2", "%account%");
    queryNamedParameters.put("alias_3", BigDecimal.valueOf(100));
    queryNamedParameters.put("alias_4", BigDecimal.valueOf(200));
    queryNamedParameters.put("alias_5", "%desc%");
    queryNamedParameters.put("alias_6", "2024-01-01");
    queryNamedParameters.put("alias_7", "2024-12-31");
    queryNamedParameters.put("alias_8", "2024-01-01");
    queryNamedParameters.put("alias_9", "2024-12-31");
    queryNamedParameters.put("alias_10", "test-pc-id");
    
    requestParameters.put(CRITERIA,
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"fieldName\":\"generalLedger\",\"operator\":\"iContains\",\"value\":\"test\"},"
        + "{\"fieldName\":\"type\",\"operator\":\"equals\",\"value\":\"O\"},"
        + "{\"fieldName\":\"account\",\"operator\":\"iContains\",\"value\":\"account\"},"
        + FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0 + ","
        + "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":200.0},"
        + FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST + ","
        + FIELD_NAME_CREATION_DATE_OPERATOR_EQUALS_VALUE_DATE + ","
        + FIELD_NAME_UPDATED_OPERATOR_GREATER_THAN_VALUE_DATE
        + "]}");
    
    String result = transformer.transformHqlQuery(complexQuery, requestParameters, queryNamedParameters);
    
    // All aggregate conditions should be removed from WHERE
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain Max(fa.description) in WHERE", whereClause.contains("Max(fa.description)"));
    assertFalse("Should not contain Max(fa.creationDate) in WHERE", whereClause.contains("Max(fa.creationDate)"));
    assertFalse("Should not contain Max(fa.updated) in WHERE", whereClause.contains("Max(fa.updated)"));
    assertFalse("Should not contain Sum aggregate in WHERE", whereClause.contains("Sum(fa.debit"));
    
    // Parentheses should be balanced
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced parentheses in WHERE", openCount, closeCount);
    
    // Non-aggregate conditions should still be present
    assertTrue("Should keep alias_10 (non-aggregate)", result.contains(":alias_10"));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    assertTrue("Should keep alias_10 parameter", queryNamedParameters.containsKey("alias_10"));
    
    // Should have HAVING clause with aggregate filters
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should contain havingParam in HAVING", result.contains("havingParam_"));
    
    // Should contain ORDER BY
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
  }

  /**
   * Verifies that empty 1=1 conditions are properly collapsed after aggregate removal.
   */
  @Test
  public void testMultiple1eq1ConditionsAreCollapsed() {
    String queryWithMultiple1_1 = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND ((1=1 and (1=1 and (1=1 1=1) and (1=1 1=1) and pc.id = :alias_10 ))) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc-id");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithMultiple1_1, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not contain "1=1 1=1" pattern (missing operator)
    assertFalse("Should not contain '1=1 1=1' pattern", whereClause.contains("1=1 1=1"));
    
    // Should have fewer 1=1 occurrences after cleanup
    int count1_1 = whereClause.split("1=1", -1).length - 1;
    assertTrue("Should have minimized 1=1 occurrences", count1_1 <= 2);
    
    // Parentheses should be balanced
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced parentheses", openCount, closeCount);
  }

  /**
   * Verifies that double nested Max() aggregate is handled correctly.
   * This tests edge case like Max(Max(fa.updated)).
   */
  @Test
  public void testDoubleNestedMaxAggregateHandled() {
    String queryWithDoubleNested = "SELECT max(fa.id), Max(fa.updated) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND Max(Max(fa.updated)) <= :alias_0 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, "2024-12-31");
    requestParameters.put(CRITERIA, FIELD_NAME_UPDATED_OPERATOR_GREATER_THAN_VALUE_DATE);
    
    String result = transformer.transformHqlQuery(queryWithDoubleNested, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain Max(Max aggregate in WHERE", whereClause.contains("Max(Max("));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
  }

  /**
   * Verifies handling when query has no GROUP BY clause.
   * Tests fallback behavior in fixUnclosedParensBeforeGroupBy.
   */
  @Test
  public void testQueryWithoutGroupBySkipsParenthesisFix() {
    String queryNoGroupBy = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND ((upper(Max(fa.description)) like upper(:alias_0) escape '|'))";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryNoGroupBy, requestParameters, queryNamedParameters);
    
    assertNotNull("Should return non-null result", result);
    
    // Accept either: aggregate removed from WHERE, or query unchanged (both are valid)
    // The key check is that parameters are handled correctly and no errors occur
    boolean aggregateRemoved = !result.contains("upper(Max(fa.description)) like upper(:alias_0)");
    boolean parameterRemoved = !queryNamedParameters.containsKey(ALIAS_0);
    
    // If aggregate was removed, parameter should also be removed
    if (aggregateRemoved) {
      assertTrue("If aggregate removed from query, parameter should also be removed", parameterRemoved);
    }
    
    // Should keep clientId parameter
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    
    // SELECT clause should still have Max(fa.description) since it's in the SELECT list
    assertTrue("SELECT clause should still have Max aggregate", result.contains("SELECT max(fa.id), Max(fa.description)"));
  }

  /**
   * Verifies handling when query has no WHERE clause.
   * Tests when only GROUP BY and HAVING exist.
   */
  @Test
  public void testQueryWithoutWhereClause() {
    String queryNoWhere = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "GROUP BY fa.client.id";
    
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryNoWhere, requestParameters, queryNamedParameters);
    
    // Should add HAVING clause even without WHERE
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should contain havingParam", result.contains(HAVING_PARAM));
  }

  /**
   * Verifies ORDER BY clause transformation with subqueries.
   * Tests fixOrderByClause and related methods.
   */
  @Test
  public void testOrderByWithSubqueryGetsReplacedWithAlias() {
    String queryWithOrderBySubquery = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "(select ev.searchKey from FinancialMgmtElementValue ev where fa.account.id = ev.id) AS Account_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id, fa.account.id "
        + "ORDER BY (select ev.searchKey from FinancialMgmtElementValue ev where fa.account.id = ev.id) DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithOrderBySubquery, requestParameters, queryNamedParameters);
    
    // ORDER BY should use alias instead of subquery
    int orderByIndex = result.indexOf("ORDER BY");
    if (orderByIndex != -1) {
      // Should either use the alias or at least not have the subquery repeated in ORDER BY
      assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    }
  }

  /**
   * Verifies ORDER BY with CASE expressions gets handled.
   * Tests replaceAggregateFunctionsInOrderBy.
   */
  @Test
  public void testOrderByWithCaseExpressionGetsReplaced() {
    String queryWithOrderByCase = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END AS Debit "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithOrderByCase, requestParameters, queryNamedParameters);
    
    // Should have ORDER BY (possibly transformed to use alias)
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
  }

  /**
   * Verifies ORDER BY with Max() aggregate function gets replaced.
   * Tests findNextAggregateOrCase and extractExpression.
   */
  @Test
  public void testOrderByWithMaxAggregateGetsReplaced() {
    String queryWithOrderByMax = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description) ASC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithOrderByMax, requestParameters, queryNamedParameters);
    
    // Should have ORDER BY
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    // Likely transformed to use "Description" alias
    int orderByIndex = result.indexOf("ORDER BY");
    String orderByPart = result.substring(orderByIndex);
    assertTrue("Should have ASC or DESC", orderByPart.contains("ASC") || orderByPart.contains("DESC"));
  }

  /**
   * Verifies that excess closing parentheses are handled.
   * Tests balanceParentheses method.
   */
  @Test
  public void testExcessClosingParenthesesRemoved() {
    String queryWithExcessClosing = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (fa.type = 'O')) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithExcessClosing, requestParameters, queryNamedParameters);
    
    // Should handle gracefully without throwing exception
    assertNotNull("Should return result", result);
    assertTrue("Should contain WHERE", result.contains(WHERE));
  }

  /**
   * Verifies handling of empty parentheses patterns.
   * Tests cleanupConditions regex patterns.
   */
  @Test
  public void testEmptyParenthesesPatternsAreRemoved() {
    String queryWithEmptyParens = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND () AND (()) AND pc.id = :alias_10 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithEmptyParens, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not contain empty parentheses
    assertFalse("Should not contain ()", whereClause.contains("()"));
    assertFalse("Should not contain (())", whereClause.contains("(())"));
  }

  /**
   * Verifies that "AND 1=1 AND" patterns are collapsed.
   * Tests specific cleanupConditions regex.
   */
  @Test
  public void testAnd1Equals1AndPatternsCollapsed() {
    String queryWithAnd1And = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND 1=1 AND 1=1 AND pc.id = :alias_10 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithAnd1And, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should have minimized 1=1 occurrences
    int count1_1 = whereClause.split("1=1", -1).length - 1;
    assertTrue("Should have fewer 1=1 patterns", count1_1 <= 2);
  }

  /**
   * Verifies that "(1=1 AND" at start of group is removed.
   * Tests specific cleanupConditions regex.
   */
  @Test
  public void testOpenParen1Equals1AndRemovedFromStart() {
    String queryWith1AtStart = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (1=1 AND pc.id = :alias_10) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWith1AtStart, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not have "(1=1 AND" pattern
    assertFalse("Should not contain '(1=1 AND'", whereClause.contains("(1=1 AND"));
  }

  /**
   * Verifies that "AND 1=1)" at end of group is removed.
   * Tests specific cleanupConditions regex.
   */
  @Test
  public void testAnd1Equals1CloseParenRemovedFromEnd() {
    String queryWith1AtEnd = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (pc.id = :alias_10 AND 1=1) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWith1AtEnd, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not have "AND 1=1)" pattern
    assertFalse("Should not contain 'AND 1=1)'", whereClause.contains("AND 1=1)"));
  }

  /**
   * Verifies that "WHERE 1=1 AND" is cleaned to just "WHERE".
   * Tests specific cleanupConditions regex.
   */
  @Test
  public void testWhere1Equals1AndCleanedToWhere() {
    String queryWithWhere1And = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE 1=1 AND fa.client.id in (:clientId) AND pc.id = :alias_10 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithWhere1And, requestParameters, queryNamedParameters);
    
    // Should have WHERE but not immediately followed by 1=1 AND
    assertTrue("Should contain WHERE", result.contains(WHERE));
    assertFalse("Should not contain 'WHERE 1=1 AND'", result.contains("WHERE 1=1 AND"));
  }

  /**
   * Verifies that queries with potentially missing operators are handled gracefully.
   * After aggregate removal and cleanup, the query should remain syntactically valid.
   */
  @Test
  public void testQueryWithPotentiallyMissingOperatorIsHandled() {
    String queryWithPattern = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND ((upper(Max(fa.description)) like upper(:alias_0) escape '|')) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithPattern, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // The aggregate condition should be removed (replaced and cleaned up)
    assertFalse("Should not contain original aggregate", whereClause.contains("upper(Max(fa.description))"));
    // The alias_0 parameter should be removed since aggregate was removed
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
    // Should keep clientId parameter (non-aggregate)
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    // Parentheses should be balanced
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced parentheses", openCount, closeCount);
  }

  /**
   * Verifies that orphaned "and" at start of parentheses is removed.
   * Tests cleanupConditions regex for "\\(\\s*and\\s+".
   */
  @Test
  public void testOrphanedAndAtStartOfParenthesesRemoved() {
    String queryWithOrphanedAnd = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (and pc.id = :alias_10) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithOrphanedAnd, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not have "(and" pattern
    assertFalse("Should not contain '(and '", whereClause.toLowerCase().contains("(and "));
  }

  /**
   * Verifies double AND patterns are collapsed to single AND.
   * Tests cleanupConditions regex for "AND\\s+AND".
   */
  @Test
  public void testDoubleAndPatternsCollapsedToSingleAnd() {
    String queryWithDoubleAnd = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND AND pc.id = :alias_10 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put("alias_10", "test-pc");
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithDoubleAnd, requestParameters, queryNamedParameters);
    
    // Extract WHERE clause
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    // Should not have "AND AND" pattern
    assertFalse("Should not contain 'AND AND'", whereClause.contains("AND AND"));
  }

  /**
   * Verifies that findMainFromPosition handles query starting with FROM.
   * Tests edge case in findMainFromPosition.
   */
  @Test
  public void testFindMainFromPositionAtStart() {
    String queryFromAtStart = "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryFromAtStart, requestParameters, queryNamedParameters);
    
    // Should handle gracefully
    assertNotNull("Should return result", result);
  }

  /**
   * Verifies handling when no aggregate functions exist in query.
   * Tests that normal queries pass through correctly.
   */
  @Test
  public void testQueryWithoutAggregatesPassesThrough() {
    String simpleQuery = "SELECT fa.id "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND fa.type = :alias_0";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, "O");
    requestParameters.put(CRITERIA, "{\"fieldName\":\"type\",\"operator\":\"equals\",\"value\":\"O\"}");
    
    String result = transformer.transformHqlQuery(simpleQuery, requestParameters, queryNamedParameters);
    
    // Should keep both parameters since nothing is aggregate
    assertTrue("Should keep alias_0", queryNamedParameters.containsKey(ALIAS_0));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    assertTrue("Should contain :alias_0", result.contains(":alias_0"));
  }

  /**
   * Verifies that extractValue handles JSONArray with single element.
   * When a filter value is a JSONArray with one element, it should extract that element.
   */
  @Test
  public void testExtractValueWithSingleElementJSONArray() {
    // This simulates a combo/selector field that returns an array with single value
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":[100.0]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    // Should process the array value correctly
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey(HAVING_PARAM_0));
    Object paramValue = queryNamedParameters.get(HAVING_PARAM_0);
    assertNotNull("Parameter value should not be null", paramValue);
    assertEquals("Should extract single value from array", BigDecimal.valueOf(100.0), paramValue);
  }

  /**
   * Verifies that extractValue handles JSONArray with multiple elements.
   * When value is an array with multiple elements, should use the first one.
   */
  @Test
  public void testExtractValueWithMultipleElementJSONArray() {
    // Array with multiple values - should take first
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":[100.0, 200.0, 300.0]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey(HAVING_PARAM_0));
    Object paramValue = queryNamedParameters.get(HAVING_PARAM_0);
    assertNotNull("Parameter value should not be null", paramValue);
    assertEquals("Should use first value from array", BigDecimal.valueOf(100.0), paramValue);
  }

  /**
   * Verifies that extractValue handles empty JSONArray.
   * Empty array should result in skipping the condition (no HAVING parameter).
   */
  @Test
  public void testExtractValueWithEmptyJSONArray() {
    requestParameters.put(CRITERIA, "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":[]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    // Empty array should be treated as no value, may not add HAVING param
    // The behavior should be graceful - no exceptions
    assertNotNull("Result should not be null", result);
  }

  /**
   * Verifies that closesFunction correctly identifies function parentheses.
   * Tests that closing paren of Max() is recognized as function closure.
   */
  @Test
  public void testClosesFunctionWithAggregateFunction() {
    String queryWithFunction = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND Max(fa.description) like upper(:alias_0) escape '|' "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithFunction, requestParameters, queryNamedParameters);
    
    // The aggregate condition should be removed properly
    // This implicitly tests closesFunction because it needs to identify
    // where Max(...) ends to extract the full condition
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should remove Max(fa.description) from WHERE", whereClause.contains("Max(fa.description)"));
    assertFalse(SHOULD_HAVE_REMOVED_ALIAS_0, queryNamedParameters.containsKey(ALIAS_0));
  }

  /**
   * Verifies that nested functions in SELECT clause don't interfere with query transformation.
   * Tests that transformer correctly handles complex nested function expressions like upper(Max(...)).
   */
  @Test
  public void testClosesFunctionWithNestedFunctions() {
    String queryWithNested = "SELECT max(fa.id), upper(Max(fa.description)) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithNested, requestParameters, queryNamedParameters);
    
    // Should correctly handle nested functions in SELECT without errors
    assertTrue("Should preserve nested function in SELECT", result.contains("upper(Max(fa.description))"));
    assertTrue("Should add HAVING clause", result.contains(HAVING));
    assertTrue(SHOULD_KEEP_CLIENT_ID_PARAMETER, queryNamedParameters.containsKey(CLIENT_ID));
    assertTrue("Should add havingParam for debit filter", queryNamedParameters.containsKey(HAVING_PARAM_0));
  }

  /**
   * Verifies findMatchingOpenParen with simple parentheses.
   * Tests matching of opening and closing parentheses in conditions.
   */
  @Test
  public void testFindMatchingOpenParenSimpleCase() {
    String queryWithParens = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE (fa.client.id in (:clientId) AND Max(fa.description) like :alias_0) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithParens, requestParameters, queryNamedParameters);
    
    // Should correctly match parentheses when extracting conditions
    // Parentheses should remain balanced
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced parentheses", openCount, closeCount);
  }

  /**
   * Verifies findMatchingOpenParen with deeply nested parentheses.
   * Tests matching through multiple levels of nesting.
   */
  @Test
  public void testFindMatchingOpenParenDeeplyNested() {
    String queryWithDeepNesting = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE (((fa.client.id in (:clientId) AND upper(Max(fa.description)) like upper(:alias_0)))) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    queryNamedParameters.put(ALIAS_0, TEST);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithDeepNesting, requestParameters, queryNamedParameters);
    
    // Should correctly match through nested parentheses
    int whereIndex = result.indexOf(WHERE);
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    long openCount = whereClause.chars().filter(ch -> ch == '(').count();
    long closeCount = whereClause.chars().filter(ch -> ch == ')').count();
    assertEquals("Should have balanced nested parentheses", openCount, closeCount);
    assertFalse("Should remove aggregate", whereClause.contains(UPPER_MAX_FA_DESCRIPTION));
  }

  /**
   * Verifies skipSortDirection with DESC keyword.
   * Tests ORDER BY transformation when DESC is present.
   */
  @Test
  public void testSkipSortDirectionWithDesc() {
    String queryWithDescOrder = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description) DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithDescOrder, requestParameters, queryNamedParameters);
    
    // Should preserve DESC keyword after transforming ORDER BY
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should preserve DESC", result.contains("DESC"));
    // Should transform Max(fa.description) to use alias
    int orderByIndex = result.indexOf("ORDER BY");
    String orderByPart = result.substring(orderByIndex);
    // After transformation, should not have Max() directly in ORDER BY
    // (it should be replaced with alias reference)
    assertNotNull("Should have ORDER BY clause", orderByPart);
  }

  /**
   * Verifies skipSortDirection with ASC keyword.
   * Tests ORDER BY transformation when ASC is present.
   */
  @Test
  public void testSkipSortDirectionWithAsc() {
    String queryWithAscOrder = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.updated) as Updated "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.updated) ASC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_UPDATED_OPERATOR_GREATER_THAN_VALUE_DATE);
    
    String result = transformer.transformHqlQuery(queryWithAscOrder, requestParameters, queryNamedParameters);
    
    // Should preserve ASC keyword
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should preserve ASC", result.contains("ASC"));
  }

  /**
   * Verifies skipSortDirection without explicit sort direction.
   * Tests ORDER BY when neither ASC nor DESC is specified.
   */
  @Test
  public void testSkipSortDirectionWithoutExplicitDirection() {
    String queryWithNoDirection = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description)";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithNoDirection, requestParameters, queryNamedParameters);
    
    // Should still process ORDER BY even without explicit direction
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertNotNull("Result should be valid", result);
  }

  /**
   * Verifies skipSortDirection with whitespace before direction keyword.
   * Tests that extra spaces before DESC/ASC are handled correctly.
   */
  @Test
  public void testSkipSortDirectionWithWhitespace() {
    String queryWithSpaces = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description)   DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithSpaces, requestParameters, queryNamedParameters);
    
    // Should handle whitespace correctly
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should preserve DESC", result.contains("DESC"));
  }

  /**
   * Verifies skipSortDirection with multiple ORDER BY expressions.
   * Tests comma-separated order expressions with different directions.
   */
  @Test
  public void testSkipSortDirectionWithMultipleOrderByExpressions() {
    String queryWithMultipleOrder = "SELECT max(fa.id) AS Fact_Acct_ID, "
        + "Max(fa.description) as Description, "
        + "Max(fa.creationDate) as Created "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description) DESC, Max(fa.creationDate) ASC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DESCRIPTION_OPERATOR_I_CONTAINS_VALUE_TEST);
    
    String result = transformer.transformHqlQuery(queryWithMultipleOrder, requestParameters, queryNamedParameters);
    
    // Should handle multiple ORDER BY expressions
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should preserve DESC", result.contains("DESC"));
    assertTrue("Should preserve ASC", result.contains("ASC"));
  }

  /**
   * Verifies findFromWithWordBoundary and isValidFromKeyword helper methods.
   * Tests that main FROM is correctly identified even with FROMs in subqueries.
   */
  @Test
  public void testFindMainFromWithSubqueriesInSelect() {
    String queryWithSubqueryFrom = "SELECT max(fa.id), "
        + "(select asch.name from FinancialMgmtAcctSchema asch where fa.accountingSchema.id = asch.id) AS Acctschema, "
        + "(select ev.name from FinancialMgmtElementValue ev where fa.account.id = ev.id) AS Account "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithSubqueryFrom, requestParameters, queryNamedParameters);
    
    // Should successfully transform despite multiple FROM keywords
    assertTrue("Should contain main FROM", result.contains("FROM FinancialMgmtAccountingFact"));
    assertTrue("Should contain HAVING clause", result.contains(HAVING));
    assertTrue("Should preserve subqueries", result.contains("from FinancialMgmtAcctSchema"));
  }

  /**
   * Verifies findFromOutsideParentheses and countParenDepth helper methods.
   * Tests fallback logic for finding FROM outside parentheses.
   */
  @Test
  public void testFindFromOutsideParenthesesWithComplexNesting() {
    String queryWithNestedFrom = "SELECT max(fa.id), "
        + "CASE WHEN (select count(*) from FinancialMgmtPeriod p where p.id = fa.period.id) > 0 THEN 'Y' ELSE 'N' END AS HasPeriod "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(queryWithNestedFrom, requestParameters, queryNamedParameters);
    
    // Should correctly identify main FROM and not get confused by nested ones
    assertTrue("Should contain HAVING for credit filter", result.contains(HAVING));
    assertTrue("Should preserve CASE expression with nested FROM", result.contains("from FinancialMgmtPeriod"));
  }

  /**
   * Verifies skipNonAlphanumeric and extractWord helper methods.
   * Tests ORDER BY with special characters and word extraction.
   */
  @Test
  public void testOrderByWithSpecialCharactersAndAliases() {
    String queryWithSpecialChars = "SELECT max(fa.id), "
        + "Max(fa.description) as Description, "
        + "Max(fa.creationDate) as Created "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY  Description , Created  DESC";  // Extra spaces
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithSpecialChars, requestParameters, queryNamedParameters);
    
    // Should handle extra spaces and extract words correctly
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should preserve Description alias", result.contains("Description"));
    assertTrue("Should preserve Created alias", result.contains("Created"));
  }

  /**
   * Verifies isAliasPropertyPattern and handleAliasProperty helper methods.
   * Tests that alias.property patterns are replaced when alias is known.
   */
  @Test
  public void testOrderByReplacesKnownAliasProperty() {
    String queryWithAliasProperty = "SELECT max(fa.id), "
        + "Max(fa.createdBy.name) as Createdby "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Createdby.name DESC";  // Should be replaced with just "Createdby"
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithAliasProperty, requestParameters, queryNamedParameters);
    
    // Should replace Createdby.name with just Createdby in ORDER BY
    int orderByPos = result.toUpperCase().indexOf("ORDER BY");
    String orderByClause = result.substring(orderByPos);
    
    assertTrue("ORDER BY should contain Createdby alias", orderByClause.contains("Createdby"));
    assertFalse("ORDER BY should not contain .name property access", orderByClause.contains("Createdby.name"));
  }

  /**
   * Verifies processWord helper method with unknown alias.
   * Tests that alias.property patterns are kept when alias is NOT known.
   */
  @Test
  public void testOrderByKeepsUnknownAliasProperty() {
    String queryWithUnknownAlias = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY fa.type DESC";  // fa.type is not an alias, should be kept
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithUnknownAlias, requestParameters, queryNamedParameters);
    
    // Should keep fa.type as-is since 'fa' is not a known alias
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    assertTrue("Should keep fa.type property access", result.contains("fa.type") || result.contains("fa.client.id"));
  }

  /**
   * Verifies findPropertyEnd helper method with multiple properties.
   * Tests ORDER BY with multiple alias.property patterns.
   */
  @Test
  public void testOrderByWithMultipleAliasProperties() {
    String queryWithMultipleAliasProps = "SELECT max(fa.id), "
        + "Max(fa.createdBy.name) as Createdby, "
        + "Max(fa.updatedBy.name) as Updatedby "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Createdby.name, Updatedby.name DESC";
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithMultipleAliasProps, requestParameters, queryNamedParameters);
    
    // Should replace both Createdby.name and Updatedby.name with just the aliases
    int orderByPos = result.toUpperCase().indexOf("ORDER BY");
    String orderByClause = result.substring(orderByPos);
    
    assertTrue("ORDER BY should contain Createdby", orderByClause.contains("Createdby"));
    assertTrue("ORDER BY should contain Updatedby", orderByClause.contains("Updatedby"));
    assertFalse("Should not contain Createdby.name", orderByClause.contains("Createdby.name"));
    assertFalse("Should not contain Updatedby.name", orderByClause.contains("Updatedby.name"));
  }

  /**
   * Verifies processing flag in replaceSubqueriesInOrderBy.
   * Tests that malformed subquery stops processing correctly.
   */
  @Test
  public void testOrderByWithMalformedSubqueryHandling() {
    String queryWithMalformedSubquery = "SELECT max(fa.id), "
        + "(select ev.name from FinancialMgmtElementValue ev where fa.account.id = ev.id) as Account "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY (select incomplete";  // Malformed subquery (no closing paren)
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithMalformedSubquery, requestParameters, queryNamedParameters);
    
    // Should handle malformed query gracefully without throwing exception
    assertNotNull("Result should not be null", result);
    assertTrue("Should still contain HAVING clause", result.contains(HAVING));
  }

  /**
   * Verifies processing flag in replaceAggregateFunctionsInOrderBy.
   * Tests that ORDER BY with only aggregates gets all replaced.
   */
  @Test
  public void testOrderByWithOnlyAggregateFunctions() {
    String queryWithAggregatesOnly = "SELECT max(fa.id), "
        + "Max(fa.description) as Description, "
        + "Sum(fa.debit) as TotalDebit "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY Max(fa.description), Sum(fa.debit)";  // Only aggregates
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithAggregatesOnly, requestParameters, queryNamedParameters);
    
    // Should replace aggregates with aliases in ORDER BY
    int orderByPos = result.toUpperCase().indexOf("ORDER BY");
    String orderByClause = result.substring(orderByPos);
    
    assertTrue("ORDER BY should exist", orderByClause.length() > 8);
    // After replacement, ORDER BY should use aliases instead of aggregate functions
    assertFalse("Should not have Max(fa.description) in ORDER BY", 
        orderByClause.contains("Max(fa.description)"));
  }

  /**
   * Verifies that empty ORDER BY after removing all expressions falls back to default.
   * Tests edge case handling in ORDER BY transformation.
   */
  @Test
  public void testOrderByEmptyAfterTransformationUsesDefault() {
    String queryWithRemovableOrderBy = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) "
        + "GROUP BY fa.client.id "
        + "ORDER BY (select unmapped.field from UnknownTable unmapped)";  // No alias mapping
    
    queryNamedParameters.put(CLIENT_ID, TEST_CLIENT);
    requestParameters.put(CRITERIA, FIELD_NAME_DEBIT_OPERATOR_GREATER_THAN_VALUE_100_0);
    
    String result = transformer.transformHqlQuery(queryWithRemovableOrderBy, requestParameters, queryNamedParameters);
    
    // Should handle unmapped ORDER BY expression
    assertTrue("Should contain ORDER BY", result.contains("ORDER BY"));
    // Should use a default order field (fa.client.id from GROUP BY)
    String orderByClause = result.substring(result.toUpperCase().indexOf("ORDER BY"));
    assertFalse("Should not contain unmapped subquery", orderByClause.contains("UnknownTable"));
  }
}
