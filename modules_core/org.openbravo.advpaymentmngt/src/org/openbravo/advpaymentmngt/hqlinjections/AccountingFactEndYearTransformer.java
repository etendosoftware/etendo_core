package org.openbravo.advpaymentmngt.hqlinjections;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.json.JsonUtils;

/**
 * HQL Query Transformer for the Financial Management Accounting Fact End Year table.
 *
 * <p>
 * This transformer addresses a PostgreSQL limitation where aggregate functions (SUM, MAX, MIN, AVG,
 * COUNT) cannot be used in WHERE clauses. When users filter by aggregated columns like Debit,
 * Credit, or Description in the End Year Close grid, the Data Access Layer (DAL) generates HQL
 * queries that place these aggregate conditions in the WHERE clause, causing SQL errors.
 * </p>
 *
 * <p>
 * <b>Problem:</b> PostgreSQL rejects queries like:
 *
 * <pre>
 * WHERE Max(fa.description) LIKE :param
 * </pre>
 *
 * with error "aggregate functions are not allowed in WHERE"
 * </p>
 *
 * <p>
 * <b>Solution:</b> This transformer intercepts HQL queries before execution and performs two key
 * operations:
 * <ol>
 * <li>Removes all aggregate function conditions from the WHERE clause (replacing them with 1=1
 * placeholders)</li>
 * <li>For Debit and Credit filters, generates equivalent conditions in a HAVING clause using the
 * proper aggregate expressions</li>
 * </ol>
 * </p>
 *
 * <p>
 * The transformer handles complex scenarios including:
 * <ul>
 * <li>Nested aggregate functions: {@code upper(Max(fa.description))}</li>
 * <li>LIKE clauses with escape characters: {@code LIKE :param escape '|'}</li>
 * <li>Multiple filter conditions with AND/OR operators</li>
 * <li>Nested criteria groups</li>
 * <li>All comparison operators (equals, greater than, less than, etc.)</li>
 * </ul>
 * </p>
 *
 * <p>
 * <b>Example Transformation:</b>
 *
 * <pre>
 * Input HQL:
 * SELECT ... WHERE ... AND CASE WHEN Sum(...) > 0 THEN Sum(...) ELSE 0 END > :alias_0
 *
 * Output HQL:
 * SELECT ... WHERE ... AND 1=1 HAVING CASE WHEN Sum(...) > 0 THEN Sum(...) ELSE 0 END > :havingParam_0
 * </pre>
 * </p>
 *
 * @see HqlQueryTransformer
 * @see ComponentProvider.Qualifier
 */
@ComponentProvider.Qualifier("A45FDE07216C40FBA472E7B91F7273E4")
public class AccountingFactEndYearTransformer extends HqlQueryTransformer {

  private static final Logger log = LogManager.getLogger();
  private static final String CRITERIA = "criteria";

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    if (!requestParameters.containsKey(CRITERIA)) {
      return _hqlQuery;
    }

    try {
      JSONObject criteria = JsonUtils.buildCriteria(requestParameters);

      // Build HAVING clause directly from JSON criteria for debit/credit only
      StringBuilder havingClause = new StringBuilder();
      int[] paramCounter = {0};
      boolean hasAggregateFilters = processJSONCriteria(criteria, havingClause, queryNamedParameters, paramCounter);
      
      // ALWAYS remove aggregate conditions from WHERE (even if no debit/credit filters)
      // because other fields like description use Max() and DAL puts them in WHERE
      String cleanedQuery = removeAggregateConditionsFromWhere(_hqlQuery, queryNamedParameters);
      
      if (hasAggregateFilters) {
        return insertHavingClause(cleanedQuery, havingClause.toString());
      } else {
        return cleanedQuery;
      }
      
    } catch (Exception e) {
      log.error("Error transforming HQL query for End Year Close", e);
      return _hqlQuery;
    }
  }

  private boolean processJSONCriteria(JSONObject criteria, StringBuilder havingClause,
      Map<String, Object> queryNamedParameters, int[] paramCounter) throws Exception {
    
    if (!criteria.has(CRITERIA)) {
      return false;
    }

    JSONArray criteriaArray = criteria.getJSONArray(CRITERIA);
    List<String> conditions = new ArrayList<>();
    boolean hasAggregate = processCriteriaArray(criteriaArray, conditions, queryNamedParameters, paramCounter);
    
    if (!conditions.isEmpty()) {
      String join = getJoinOperator(criteria);
      havingClause.append(String.join(join, conditions));
    }
    
    return hasAggregate;
  }

  private boolean processCriteriaArray(JSONArray criteriaArray, List<String> conditions,
      Map<String, Object> queryNamedParameters, int[] paramCounter) throws Exception {
    boolean hasAggregate = false;
    
    for (int i = 0; i < criteriaArray.length(); i++) {
      JSONObject criterion = criteriaArray.getJSONObject(i);
      
      if (processNestedCriteria(criterion, conditions, queryNamedParameters, paramCounter)) {
        hasAggregate = true;
        continue;
      }
      
      if (processAggregateCriterion(criterion, conditions, queryNamedParameters, paramCounter)) {
        hasAggregate = true;
      }
    }
    
    return hasAggregate;
  }

  private boolean processNestedCriteria(JSONObject criterion, List<String> conditions,
      Map<String, Object> queryNamedParameters, int[] paramCounter) throws Exception {
    if (!criterion.has(CRITERIA)) {
      return false;
    }
    
    StringBuilder nestedHaving = new StringBuilder();
    if (processJSONCriteria(criterion, nestedHaving, queryNamedParameters, paramCounter)) {
      conditions.add("(" + nestedHaving.toString() + ")");
      return true;
    }
    
    return false;
  }

  private boolean processAggregateCriterion(JSONObject criterion, List<String> conditions,
      Map<String, Object> queryNamedParameters, int[] paramCounter) throws JSONException {
    if (!criterion.has("fieldName")) {
      return false;
    }
    
    String fieldName = criterion.getString("fieldName");
    if (!isAggregateField(fieldName)) {
      return false;
    }
    
    String operator = criterion.optString("operator", "equals");
    Object value = criterion.opt("value");
    String aggregateExpr = getAggregateExpression(fieldName);
    
    String condition = buildCondition(aggregateExpr, operator, value, queryNamedParameters, paramCounter);
    if (condition != null) {
      conditions.add(condition);
      return true;
    }
    
    return false;
  }

  private boolean isAggregateField(String fieldName) {
    return StringUtils.equalsIgnoreCase("debit", fieldName) 
        || StringUtils.equalsIgnoreCase("credit", fieldName);
  }

  private String getAggregateExpression(String fieldName) {
    if (StringUtils.equalsIgnoreCase("debit", fieldName)) {
      return "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END";
    } else {
      return "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END";
    }
  }

  private String getJoinOperator(JSONObject criteria) {
    String operator = criteria.optString("operator", "and");
    return StringUtils.equalsIgnoreCase("or", operator) ? " OR " : " AND ";
  }

  private String buildCondition(String expr, String operator, Object value,
      Map<String, Object> queryNamedParameters, int[] paramCounter) {
    
    if (value == null) {
      return null;
    }
    
    // Convert Double to BigDecimal if needed (debit/credit are BigDecimal fields)
    Object paramValue = value;
    if (value instanceof Double) {
      paramValue = BigDecimal.valueOf((Double) value);
    } else if (value instanceof Integer) {
      paramValue = BigDecimal.valueOf((Integer) value);
    } else if (value instanceof Long) {
      paramValue = BigDecimal.valueOf((Long) value);
    }
    
    String paramName = "havingParam_" + paramCounter[0]++;
    queryNamedParameters.put(paramName, paramValue);
    
    switch (operator.toLowerCase()) {
      case "notequal":
        return expr + " <> :" + paramName;
      case "greaterthan":
        return expr + " > :" + paramName;
      case "lessthan":
        return expr + " < :" + paramName;
      case "greaterorequal":
        return expr + " >= :" + paramName;
      case "lessorequal":
        return expr + " <= :" + paramName;
      default:
        return expr + " = :" + paramName;
    }
  }

  private String removeAggregateConditionsFromWhere(String hqlQuery, Map<String, Object> queryNamedParameters) {
    // Pattern to match any condition that contains aggregate functions (Sum, Max, etc.)
    // This handles nested cases where DAL generates complex nested expressions
    Set<String> paramsToRemove = new HashSet<>();
    String cleaned = hqlQuery;
    
    // Repeatedly remove any conditions containing aggregate functions
    boolean foundMatch = true;
    int iterations = 0;

    while (foundMatch && iterations < 20) { // increased safety limit
      iterations++;
      foundMatch = false;
      
      // Find any parameter :alias_N in the query, then check if its condition contains aggregates
      // This handles cases like: upper(Max(fa.description)) like upper(:alias_0)
      Pattern paramPattern = Pattern.compile(
        ":(alias_\\d+)"
      );
      Matcher paramMatcher = paramPattern.matcher(cleaned);

      while (paramMatcher.find()) {
        String paramName = paramMatcher.group(1);
        int paramPos = paramMatcher.start();

        // Look backwards from paramPos to find the start of this condition
        // We need to find the opening of the expression (after AND, OR, or opening paren)
        int conditionStart = findConditionStart(cleaned, paramPos);
        // Look forwards from paramPos to find the end of this condition
        int conditionEnd = findConditionEnd(cleaned, paramMatcher.end());
        String condition = cleaned.substring(conditionStart, conditionEnd);

        // Check if this condition contains any aggregate function
        if (containsAggregateFunction(condition)) {
          paramsToRemove.add(paramName);
          
          // Replace the entire condition with 1=1
          cleaned = cleaned.substring(0, conditionStart) + "1=1" + cleaned.substring(conditionEnd);
          foundMatch = true;
          break; // Restart the search after modification
        }
      }
    }
    
    if (iterations >= 20) {
      log.warn("Reached maximum iterations while removing aggregate conditions");
    }
    // Clean up dangling AND/OR operators and empty conditions
    // Handle "and1=1" or "1=1and" (missing space)
    cleaned = cleaned.replace("and1=1", "and 1=1");
    cleaned = cleaned.replace("1=1and", "1=1 and");
    
    // Handle multiple 1=1 with AND between them
    cleaned = cleaned.replace("1=1\\s+and\\s+1=1", "1=1");
    cleaned = cleaned.replace("AND\\s+1=1\\s+AND", "AND");
    cleaned = cleaned.replace("\\s+AND\\s+1=1\\s*\\)", ")");
    cleaned = cleaned.replace("\\(\\s*1=1\\s+AND\\s+", "(");
    cleaned = cleaned.replace("\\s+AND\\s+1=1\\s+GROUP", " GROUP");
    cleaned = cleaned.replace("WHERE\\s+1=1\\s+AND\\s+", "WHERE ");
    cleaned = cleaned.replace("\\(\\(\\s*1=1\\s+and\\s+", "((");
    cleaned = cleaned.replace("\\s+and\\s+1=1\\s+and\\s+", " and ");
    cleaned = cleaned.replace("\\(\\s*1=1\\s+and\\s+", "(");
    
    // Final cleanup: remove standalone 1=1 in parentheses
    cleaned = cleaned.replace("\\(\\s*1=1\\s*\\)", "");
    cleaned = cleaned.replace("\\(\\(\\s*\\)", "((");
    cleaned = cleaned.replace("\\(\\s*and\\s+", "(");
    
    // Remove only the parameters that were used in the removed aggregate conditions
    for (String paramName : paramsToRemove) {
      queryNamedParameters.remove(paramName);
    }
    
    return cleaned;
  }
  
  private boolean containsAggregateFunction(String expression) {
    // Check if expression contains common aggregate functions
    String upper = expression.toUpperCase();
    return upper.contains("SUM(") || 
           upper.contains("MAX(") || 
           upper.contains("MIN(") || 
           upper.contains("AVG(") || 
           upper.contains("COUNT(");
  }
  
  private int findConditionStart(String query, int fromPos) {
    // Look backwards to find where this condition starts
    // Condition starts after: AND, OR, WHERE
    // We DON'T stop at parentheses because they might be part of functions like upper(:param)
    int pos = fromPos - 1;
    
    while (pos >= 0) {
      // Check for AND, OR, WHERE at current position
      if (pos >= 4 && StringUtils.equalsIgnoreCase(" AND", query.substring(pos - 4, pos))) {
        return pos;
      }
      if (pos >= 3 && StringUtils.equalsIgnoreCase(" OR", query.substring(pos - 3, pos))) {
        return pos;
      }
      if (pos >= 6 && StringUtils.equalsIgnoreCase("WHERE ", query.substring(pos - 6, pos))) {
        return pos;
      }
      // Check for opening double parenthesis which marks start of filter conditions: ((
      if (pos >= 2 && StringUtils.equalsIgnoreCase("((", query.substring(pos - 2, pos))) {
        return pos;
      }
      pos--;
    }
    
    return 0; // Start of string
  }
  
  private int findConditionEnd(String query, int fromPos) {
    int pos = fromPos;
    int parenDepth = 0;
    
    while (pos < query.length()) {
      char c = query.charAt(pos);
      
      if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        int endPos = handleClosingParen(query, pos, parenDepth);
        if (endPos != -1) {
          return endPos;
        }
        parenDepth--;
      } else if (parenDepth == 0) {
        int keywordPos = checkForTerminatingKeyword(query, pos);
        if (keywordPos != -1) {
          return keywordPos;
        }
      }
      pos++;
    }
    
    return query.length();
  }

  private int handleClosingParen(String query, int pos, int parenDepth) {
    if (parenDepth != 0) {
      return -1;
    }
    
    int escapeEndPos = checkForEscapeClause(query, pos);
    if (escapeEndPos != -1) {
      return escapeEndPos;
    }
    
    return pos;
  }

  private int checkForEscapeClause(String query, int closingParenPos) {
    int checkPos = skipWhitespace(query, closingParenPos + 1);
    
    if (!hasEscapeKeyword(query, checkPos)) {
      return -1;
    }
    
    checkPos = skipWhitespace(query, checkPos + 6); // Skip "escape"
    return skipEscapeValue(query, checkPos);
  }

  private int skipWhitespace(String query, int startPos) {
    int pos = startPos;
    while (pos < query.length() && Character.isWhitespace(query.charAt(pos))) {
      pos++;
    }
    return pos;
  }

  private boolean hasEscapeKeyword(String query, int pos) {
    return pos + 6 <= query.length() 
        && StringUtils.equalsIgnoreCase("escape", query.substring(pos, pos + 6));
  }

  private int skipEscapeValue(String query, int startPos) {
    if (startPos >= query.length() || query.charAt(startPos) != '\'') {
      return startPos;
    }
    
    int pos = startPos + 1; // Skip opening quote
    while (pos < query.length() && query.charAt(pos) != '\'') {
      pos++;
    }
    
    return pos < query.length() ? pos + 1 : pos; // Skip closing quote if present
  }

  private int checkForTerminatingKeyword(String query, int pos) {
    if (matchesKeywordAtPosition(query, pos, " AND", 4)) {
      return pos;
    }
    if (matchesKeywordAtPosition(query, pos, " OR", 3)) {
      return pos;
    }
    if (matchesKeywordAtPosition(query, pos, " GROUP", 6)) {
      return pos;
    }
    if (matchesKeywordAtPosition(query, pos, " HAVING", 7)) {
      return pos;
    }
    if (matchesKeywordAtPosition(query, pos, " ORDER", 6)) {
      return pos;
    }
    return -1;
  }

  private boolean matchesKeywordAtPosition(String query, int pos, String keyword, int length) {
    return pos + length <= query.length() 
        && StringUtils.equalsIgnoreCase(keyword, query.substring(pos, pos + length));
  }

  private String insertHavingClause(String hqlQuery, String havingClause) {
    if (havingClause == null || havingClause.trim().isEmpty()) {
      return hqlQuery;
    }

    String upperHql = hqlQuery.toUpperCase();
    int havingPos = upperHql.indexOf("HAVING");
    int orderByPos = upperHql.indexOf("ORDER BY");

    if (havingPos != -1) {
      // There's already a HAVING clause, append with AND
      String beforeHaving = hqlQuery.substring(0, havingPos + 6); // +6 for "HAVING"
      String existingHaving;
      String afterHaving;
      
      if (orderByPos != -1 && orderByPos > havingPos) {
        existingHaving = hqlQuery.substring(havingPos + 6, orderByPos).trim();
        afterHaving = hqlQuery.substring(orderByPos);
      } else {
        existingHaving = hqlQuery.substring(havingPos + 6).trim();
        afterHaving = "";
      }
      
      return beforeHaving + " (" + existingHaving + ") AND (" + havingClause + ") " + afterHaving;
      
    } else if (orderByPos != -1) {
      // No HAVING, insert before ORDER BY
      String beforeOrder = hqlQuery.substring(0, orderByPos);
      String afterOrder = hqlQuery.substring(orderByPos);
      return beforeOrder + " HAVING " + havingClause + " " + afterOrder;
      
    } else {
      // No HAVING, no ORDER BY, append at end
      return hqlQuery + " HAVING " + havingClause;
    }
  }
}
