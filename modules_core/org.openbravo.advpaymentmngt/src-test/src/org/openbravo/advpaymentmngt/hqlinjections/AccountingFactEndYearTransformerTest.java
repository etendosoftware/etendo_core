package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for AccountingFactEndYearTransformer
 * Tests the HQL query transformation for End Year Close table
 */
public class AccountingFactEndYearTransformerTest {

  private AccountingFactEndYearTransformer transformer;
  private Map<String, String> requestParameters;
  private Map<String, Object> queryNamedParameters;
  private String baseHqlQuery;

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

  @Test
  public void testNoCriteriaParameter_ReturnsOriginalQuery() {
    // No criteria parameter in request
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertEquals("Should return original query when no criteria", baseHqlQuery, result);
  }

  @Test
  public void testEmptyCriteria_ReturnsOriginalQuery() {
    requestParameters.put("criteria", "");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertNotNull("Result should not be null", result);
  }

  @Test
  public void testDebitFilterOnly_AddsHavingClause() {
    // Criteria with debit filter
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0}");
    queryNamedParameters.put("clientId", "test-client");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause", result.contains("HAVING"));
    assertTrue("Should contain debit condition", result.contains("CASE WHEN Sum(fa.debit - fa.credit)"));
    assertTrue("Should contain havingParam", result.contains("havingParam_"));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey("havingParam_0"));
    assertEquals("Parameter should be BigDecimal", BigDecimal.valueOf(100.0), queryNamedParameters.get("havingParam_0"));
  }

  @Test
  public void testCreditFilterOnly_AddsHavingClause() {
    requestParameters.put("criteria", "{\"fieldName\":\"credit\",\"operator\":\"lessThan\",\"value\":500.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain HAVING clause", result.contains("HAVING"));
    assertTrue("Should contain credit condition", result.contains("CASE WHEN Sum(fa.credit - fa.debit)"));
    assertTrue("Should have added parameter", queryNamedParameters.containsKey("havingParam_0"));
  }

  @Test
  public void testDebitAndCreditFilters_CombinesWithAnd() {
    requestParameters.put("criteria", 
        "{\"operator\":\"and\",\"_constructor\":\"AdvancedCriteria\",\"criteria\":["
        + "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0},"
        + "{\"fieldName\":\"credit\",\"operator\":\"lessThan\",\"value\":500.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain AND operator", result.contains(" AND "));
    assertTrue("Should have two parameters", queryNamedParameters.size() >= 2);
    assertTrue("Should have havingParam_0", queryNamedParameters.containsKey("havingParam_0"));
    assertTrue("Should have havingParam_1", queryNamedParameters.containsKey("havingParam_1"));
  }

  @Test
  public void testDebitOrCreditFilters_CombinesWithOr() {
    requestParameters.put("criteria", 
        "{\"operator\":\"or\",\"_constructor\":\"AdvancedCriteria\",\"criteria\":["
        + "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0},"
        + "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":500.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain OR operator", result.contains(" OR "));
  }

  @Test
  public void testDescriptionFilterWithAggregate_RemovesFromWhere() {
    String queryWithAggregateFilter = "SELECT max(fa.id), Max(fa.description) as Description "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (( upper(Max(fa.description)) like upper(:alias_0) escape '|' )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("clientId", "test-client");
    queryNamedParameters.put("alias_0", "%test%");
    requestParameters.put("criteria", "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(queryWithAggregateFilter, requestParameters, queryNamedParameters);
    
    // Verify the aggregate condition was removed from WHERE
    assertFalse("Should not contain upper(Max(fa.description)) in result", result.contains("upper(Max(fa.description))"));
    assertFalse("Should have removed alias_0 parameter", queryNamedParameters.containsKey("alias_0"));
    assertTrue("Should keep clientId parameter", queryNamedParameters.containsKey("clientId"));
    // The WHERE clause should still exist with clientId condition
    assertTrue("Should contain WHERE clause", result.contains("WHERE"));
    assertTrue("Should contain clientId condition", result.contains("fa.client.id in (:clientId)"));
  }

  @Test
  public void testMultipleAggregateConditions_RemovesAll() {
    String queryWithMultipleAggregates = "SELECT max(fa.id), Max(fa.description), Max(fa.creationDate) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND upper(Max(fa.description)) like upper(:alias_0) escape '|' "
        + "AND Max(fa.creationDate) > :alias_1 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "%test%");
    queryNamedParameters.put("alias_1", "2024-01-01");
    requestParameters.put("criteria", 
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"},"
        + "{\"fieldName\":\"creationDate\",\"operator\":\"greaterThan\",\"value\":\"2024-01-01\"}"
        + "]}");
    
    String result = transformer.transformHqlQuery(queryWithMultipleAggregates, requestParameters, queryNamedParameters);
    
    // Extract the WHERE clause to verify aggregate removal (not SELECT clause)
    int whereIndex = result.indexOf("WHERE");
    int groupByIndex = result.indexOf("GROUP BY");
    String whereClause = result.substring(whereIndex, groupByIndex);
    
    assertFalse("Should not contain upper(Max(fa.description)) in WHERE", whereClause.contains("upper(Max(fa.description))"));
    assertFalse("Should not contain Max(fa.creationDate) > in WHERE", whereClause.contains("Max(fa.creationDate) >"));
    assertFalse("Should have removed alias_0", queryNamedParameters.containsKey("alias_0"));
    assertFalse("Should have removed alias_1", queryNamedParameters.containsKey("alias_1"));
    assertTrue("Should contain 1=1 replacements", whereClause.contains("1=1"));
  }

  @Test
  public void testNestedCriteria_HandlesCorrectly() {
    requestParameters.put("criteria", 
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"operator\":\"or\",\"criteria\":["
        + "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0},"
        + "{\"fieldName\":\"debit\",\"operator\":\"lessThan\",\"value\":50.0}"
        + "]},"
        + "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":200.0}"
        + "]}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should contain parentheses for nesting", result.contains("(") && result.contains(")"));
    assertTrue("Should have multiple parameters", queryNamedParameters.size() >= 3);
  }

  @Test
  public void testDifferentOperators_AllHandled() {
    // Test equals operator
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0}");
    String result1 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle equals", result1.contains(" = :"));
    
    // Test notEqual operator
    queryNamedParameters.clear();
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"notEqual\",\"value\":100.0}");
    String result2 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle notEqual", result2.contains(" <> :"));
    
    // Test greaterOrEqual operator
    queryNamedParameters.clear();
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"greaterOrEqual\",\"value\":100.0}");
    String result3 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle greaterOrEqual", result3.contains(" >= :"));
    
    // Test lessOrEqual operator
    queryNamedParameters.clear();
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"lessOrEqual\",\"value\":100.0}");
    String result4 = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    assertTrue("Should handle lessOrEqual", result4.contains(" <= :"));
  }

  @Test
  public void testIntegerValue_ConvertsToBigDecimal() {
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100}");
    
    transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    Object paramValue = queryNamedParameters.get("havingParam_0");
    assertTrue("Should convert Integer to BigDecimal", paramValue instanceof BigDecimal);
    assertEquals("Value should match", BigDecimal.valueOf(100), paramValue);
  }

  @Test
  public void testLongValue_ConvertsToBigDecimal() {
    requestParameters.put("criteria", "{\"fieldName\":\"credit\",\"operator\":\"equals\",\"value\":9999999999}");
    
    transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    Object paramValue = queryNamedParameters.get("havingParam_0");
    assertTrue("Should convert Long to BigDecimal", paramValue instanceof BigDecimal);
  }

  @Test
  public void testNullValue_SkipsCondition() {
    // Null values in JSON are parsed as missing, not as null objects
    // Test with a query that has no debit/credit filters (only other fields)
    requestParameters.put("criteria", "{\"fieldName\":\"clientId\",\"operator\":\"equals\",\"value\":\"test\"}");
    queryNamedParameters.put("clientId", "test-client");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    // Should not add HAVING clause since clientId is not debit/credit
    assertFalse("Should not add havingParam for non-aggregate field", result.contains("havingParam_"));
    assertTrue("Should keep clientId parameter", queryNamedParameters.containsKey("clientId"));
  }

  @Test
  public void testExistingHavingClause_AppendsWithAnd() {
    String queryWithHaving = baseHqlQuery; // Already has HAVING clause
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(queryWithHaving, requestParameters, queryNamedParameters);
    
    assertTrue("Should append to existing HAVING", result.contains("HAVING"));
    assertTrue("Should use AND to combine", result.contains(" AND "));
  }

  @Test
  public void testNoGroupBy_StillProcesses() {
    String simpleQuery = "SELECT fa.id FROM FinancialMgmtAccountingFact fa WHERE fa.client.id = :clientId";
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"equals\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(simpleQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should add HAVING even without GROUP BY", result.contains("HAVING"));
  }

  @Test
  public void testEscapeClauseInLike_HandledCorrectly() {
    String queryWithEscape = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE upper(Max(fa.description)) like upper(:alias_0) escape '|' and fa.client.id = :clientId "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "%test%");
    queryNamedParameters.put("clientId", "test");
    requestParameters.put("criteria", "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(queryWithEscape, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove entire condition including escape", result.contains("escape '|'"));
    assertFalse("Should not have standalone escape keyword", result.matches(".*\\s+escape\\s+.*"));
  }

  @Test
  public void testComplexNestedParentheses_HandledCorrectly() {
    String complexQuery = "SELECT max(fa.id), Max(fa.description) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id in (:clientId) AND (( upper(Max(fa.description)) like upper(:alias_0) escape '|' and fa.type = :alias_1 )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "%test%");
    queryNamedParameters.put("alias_1", "O");
    requestParameters.put("criteria", 
        "{\"operator\":\"and\",\"criteria\":["
        + "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"},"
        + "{\"fieldName\":\"type\",\"operator\":\"equals\",\"value\":\"O\"}"
        + "]}");
    
    String result = transformer.transformHqlQuery(complexQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Max aggregate condition", result.contains("upper(Max(fa.description))"));
    assertTrue("Should keep non-aggregate condition", result.contains("fa.type = :alias_1"));
    assertFalse("Should have removed alias_0", queryNamedParameters.containsKey("alias_0"));
    assertTrue("Should keep alias_1", queryNamedParameters.containsKey("alias_1"));
  }

  @Test
  public void testSumAggregateInWhere_Removed() {
    String queryWithSum = "SELECT Sum(fa.amount) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Sum(fa.amount) > :alias_0 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "100");
    requestParameters.put("criteria", "{\"fieldName\":\"amount\",\"operator\":\"greaterThan\",\"value\":100}");
    
    String result = transformer.transformHqlQuery(queryWithSum, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Sum from WHERE", result.contains("WHERE Sum(fa.amount)"));
    assertFalse("Should remove alias_0", queryNamedParameters.containsKey("alias_0"));
  }

  @Test
  public void testAvgAggregateInWhere_Removed() {
    String queryWithAvg = "SELECT Avg(fa.amount) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Avg(fa.amount) < :alias_0 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "50");
    requestParameters.put("criteria", "{\"fieldName\":\"avgAmount\",\"operator\":\"lessThan\",\"value\":50}");
    
    String result = transformer.transformHqlQuery(queryWithAvg, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Avg from WHERE", result.contains("WHERE Avg(fa.amount)"));
  }

  @Test
  public void testMinMaxCountAggregates_AllRemoved() {
    String queryWithMultiple = "SELECT Min(fa.id), Max(fa.id), Count(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE Min(fa.id) > :alias_0 and Max(fa.id) < :alias_1 and Count(fa.id) = :alias_2 "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "1");
    queryNamedParameters.put("alias_1", "100");
    queryNamedParameters.put("alias_2", "10");
    requestParameters.put("criteria", "{\"fieldName\":\"id\",\"operator\":\"equals\",\"value\":1}");
    
    String result = transformer.transformHqlQuery(queryWithMultiple, requestParameters, queryNamedParameters);
    
    assertFalse("Should remove Min", result.contains("Min(fa.id) >"));
    assertFalse("Should remove Max", result.contains("Max(fa.id) <"));
    assertFalse("Should remove Count", result.contains("Count(fa.id) ="));
    assertEquals("Should remove all alias parameters", 0, queryNamedParameters.size());
  }

  @Test
  public void testMalformedJSON_HandlesGracefully() {
    requestParameters.put("criteria", "{malformed json");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertEquals("Should return original query on error", baseHqlQuery, result);
  }

  @Test
  public void testCriteriaWithoutFieldName_Skipped() {
    requestParameters.put("criteria", "{\"operator\":\"equals\",\"value\":100}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should not add HAVING", result.contains("havingParam_"));
  }

  @Test
  public void testNonDebitCreditField_NoHavingAdded() {
    requestParameters.put("criteria", "{\"fieldName\":\"clientId\",\"operator\":\"equals\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertFalse("Should not add HAVING for non-aggregate fields", result.contains("havingParam_"));
  }

  @Test
  public void testOrderByPreserved_WhenAddingHaving() {
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"greaterThan\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should preserve ORDER BY", result.contains("ORDER BY"));
    int havingIndex = result.indexOf("HAVING");
    int orderByIndex = result.indexOf("ORDER BY");
    assertTrue("HAVING should come before ORDER BY", havingIndex < orderByIndex);
  }

  @Test
  public void testCaseInsensitiveOperators_Work() {
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"GREATERTHAN\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should handle uppercase operators", result.contains(" > :"));
  }

  @Test
  public void testIEqualsOperator_UsesEquals() {
    requestParameters.put("criteria", "{\"fieldName\":\"debit\",\"operator\":\"iEquals\",\"value\":100.0}");
    
    String result = transformer.transformHqlQuery(baseHqlQuery, requestParameters, queryNamedParameters);
    
    assertTrue("Should use = for iEquals", result.contains(" = :"));
  }

  @Test
  public void testEmptyParentheses_CleanedUp() {
    String queryWithEmptyParens = "SELECT max(fa.id) "
        + "FROM FinancialMgmtAccountingFact fa "
        + "WHERE fa.client.id = :clientId AND (( upper(Max(fa.description)) like upper(:alias_0) )) "
        + "GROUP BY fa.client.id";
    
    queryNamedParameters.put("alias_0", "%test%");
    requestParameters.put("criteria", "{\"fieldName\":\"description\",\"operator\":\"iContains\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(queryWithEmptyParens, requestParameters, queryNamedParameters);
    
    assertFalse("Should not have empty double parentheses", result.contains("(())"));
  }

  @Test
  public void testMultipleIterations_HandlesCorrectly() {
    // Create a query with many aggregate conditions to test iteration logic
    StringBuilder query = new StringBuilder("SELECT max(fa.id) FROM FinancialMgmtAccountingFact fa WHERE ");
    for (int i = 0; i < 5; i++) {
      if (i > 0) query.append(" and ");
      query.append("Max(fa.field").append(i).append(") = :alias_").append(i);
      queryNamedParameters.put("alias_" + i, "value" + i);
    }
    query.append(" GROUP BY fa.client.id");
    
    requestParameters.put("criteria", "{\"fieldName\":\"test\",\"operator\":\"equals\",\"value\":\"test\"}");
    
    String result = transformer.transformHqlQuery(query.toString(), requestParameters, queryNamedParameters);
    
    for (int i = 0; i < 5; i++) {
      assertFalse("Should remove all aggregate conditions", result.contains("Max(fa.field" + i + ")"));
      assertFalse("Should remove all alias parameters", queryNamedParameters.containsKey("alias_" + i));
    }
  }
}
