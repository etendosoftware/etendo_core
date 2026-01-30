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
  public static final String WHERE = "WHERE ";
  public static final String AND = " AND ";
  public static final String DEBIT = "debit";
  public static final String CREDIT = "credit";
  public static final String UPDATED = "updated";
  public static final String ICONTAINS = "icontains";
  public static final String ISTARTSWITH = "istartswith";
  public static final String STARTSWITH = "startswith";
  public static final String IENDSWITH = "iendswith";
  public static final String ENDSWITH = "endswith";
  public static final String NOTEQUAL = "notequal";
  public static final String GREATERTHAN = "greaterthan";
  public static final String LESSTHAN = "lessthan";
  public static final String GREATEROREQUAL = "greaterorequal";
  public static final String GREATER_EQUAL_QUERY_SYMBOL = " >= :";
  public static final String LESSOREQUAL = "lessorequal";

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

  private String getJoinOperator(JSONObject criteria) {
    String operator = criteria.optString("operator", "and");
    return StringUtils.equalsIgnoreCase("or", operator) ? " OR " : AND;
  }

  private boolean isAggregateField(String fieldName) {
    return StringUtils.equalsIgnoreCase(DEBIT, fieldName)
        || StringUtils.equalsIgnoreCase(CREDIT, fieldName)
        || StringUtils.equalsIgnoreCase("description", fieldName)
        || StringUtils.equalsIgnoreCase("created", fieldName)
        || StringUtils.equalsIgnoreCase(UPDATED, fieldName);
  }

  private String getAggregateExpression(String fieldName) {
    if (StringUtils.equalsIgnoreCase(DEBIT, fieldName)) {
      return "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END";
    } else if (StringUtils.equalsIgnoreCase(CREDIT, fieldName)) {
      return "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END";
    } else if (StringUtils.equalsIgnoreCase("description", fieldName)) {
      return "Max(fa.description)";
    } else if (StringUtils.equalsIgnoreCase("created", fieldName)) {
      return "Max(fa.creationDate)";
    } else if (StringUtils.equalsIgnoreCase(UPDATED, fieldName)) {
      return "Max(fa.updated)";
    }
    return null;
  }

  private String buildCondition(String expr, String operator, Object value,
      Map<String, Object> queryNamedParameters, int[] paramCounter) {
    
    if (value == null) {
      return null;
    }

    boolean isDateField = expr.contains("creationDate") || expr.contains(UPDATED);
    Object paramValue = convertValueToAppropriateType(expr, value, isDateField);

    if (paramValue == null) {
      return null; // Conversion failed
    }

    String paramName = "havingParam_" + paramCounter[0]++;
    return buildConditionByOperator(expr, operator, paramValue, isDateField, paramName, queryNamedParameters);
  }

  private Object convertValueToAppropriateType(String expr, Object value, boolean isDateField) {
    // For numeric fields (debit/credit), convert to BigDecimal
    if (expr.contains("Sum(") && (expr.contains(DEBIT) || expr.contains(CREDIT))) {
      return convertToBigDecimal(value);
    }
    // For date fields (created/updated), convert String to Date
    else if (isDateField && value instanceof String) {
      return parseDate((String) value);
    }

    return value;
  }

  private Object convertToBigDecimal(Object value) {
    if (value instanceof Double) {
      return BigDecimal.valueOf((Double) value);
    } else if (value instanceof Integer) {
      return BigDecimal.valueOf((Integer) value);
    } else if (value instanceof Long) {
      return BigDecimal.valueOf((Long) value);
    }
    return value;
  }

  private Object parseDate(String dateValue) {
    try {
      // Parse ISO date string to Date
      java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
      isoFormat.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
      return isoFormat.parse(dateValue);
    } catch (Exception e) {
      // Try alternative format without time
      try {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.parse(dateValue);
      } catch (Exception e2) {
        log.error("Failed to parse date value: " + dateValue, e2);
        return null;
      }
    }
  }

  private String buildConditionByOperator(String expr, String operator, Object paramValue,
      boolean isDateField, String paramName, Map<String, Object> queryNamedParameters) {

    switch (operator.toLowerCase()) {
      case ICONTAINS:
      case "contains":
        return buildLikeCondition(expr, paramValue, paramName, queryNamedParameters, "%", "%");
      case ISTARTSWITH:
      case STARTSWITH:
        return buildLikeCondition(expr, paramValue, paramName, queryNamedParameters, "", "%");
      case IENDSWITH:
      case ENDSWITH:
        return buildLikeCondition(expr, paramValue, paramName, queryNamedParameters, "%", "");
      case NOTEQUAL:
        queryNamedParameters.put(paramName, paramValue);
        return expr + " <> :" + paramName;
      case GREATERTHAN:
        queryNamedParameters.put(paramName, paramValue);
        return expr + " > :" + paramName;
      case LESSTHAN:
        queryNamedParameters.put(paramName, paramValue);
        return expr + " < :" + paramName;
      case GREATEROREQUAL:
      case "between":
        queryNamedParameters.put(paramName, paramValue);
        return expr + GREATER_EQUAL_QUERY_SYMBOL + paramName;
      case LESSOREQUAL:
        queryNamedParameters.put(paramName, paramValue);
        return expr + " <= :" + paramName;
      case "equals":
      default:
        return buildEqualsCondition(expr, paramValue, isDateField, paramName, queryNamedParameters);
    }
  }

  private String buildLikeCondition(String expr, Object paramValue, String paramName,
      Map<String, Object> queryNamedParameters, String prefix, String suffix) {

    String likeValue = paramValue.toString();
    if (!prefix.isEmpty() && !likeValue.startsWith(prefix)) {
      likeValue = prefix + likeValue;
    }
    if (!suffix.isEmpty() && !likeValue.endsWith(suffix)) {
      likeValue = likeValue + suffix;
    }

    queryNamedParameters.put(paramName, likeValue);
    return "upper(" + expr + ") like upper(:" + paramName + ")";
  }

  private String buildEqualsCondition(String expr, Object paramValue, boolean isDateField,
      String paramName, Map<String, Object> queryNamedParameters) {

    // CRITICAL FIX: For date fields with "equals" operator, use a range
    // because the original query uses >= and <= for date comparisons
    if (isDateField && paramValue instanceof java.util.Date) {
      return buildDateRangeCondition(expr, (java.util.Date) paramValue, paramName, queryNamedParameters);
    } else {
      queryNamedParameters.put(paramName, paramValue);
      return expr + " = :" + paramName;
    }
  }

  private String buildDateRangeCondition(String expr, java.util.Date dateValue, String paramName,
      Map<String, Object> queryNamedParameters) {

    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime(dateValue);

    // Start of day (00:00:00)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
    cal.set(java.util.Calendar.MINUTE, 0);
    cal.set(java.util.Calendar.SECOND, 0);
    cal.set(java.util.Calendar.MILLISECOND, 0);
    java.util.Date startDate = cal.getTime();

    // End of day (23:59:59.999)
    cal.set(java.util.Calendar.HOUR_OF_DAY, 23);
    cal.set(java.util.Calendar.MINUTE, 59);
    cal.set(java.util.Calendar.SECOND, 59);
    cal.set(java.util.Calendar.MILLISECOND, 999);
    java.util.Date endDate = cal.getTime();

    String startParamName = paramName + "_start";
    String endParamName = paramName + "_end";

    queryNamedParameters.put(startParamName, startDate);
    queryNamedParameters.put(endParamName, endDate);

    return "(" + expr + GREATER_EQUAL_QUERY_SYMBOL + startParamName + AND + expr + " <= :" + endParamName + ")";
  }

  private String removeAggregateConditionsFromWhere(String hqlQuery, Map<String, Object> queryNamedParameters) {
    Set<String> paramsToRemove = new HashSet<>();
    String cleaned = hqlQuery;
    
    // Repeatedly remove any conditions containing aggregate functions
    boolean foundMatch = true;
    int iterations = 0;

    while (foundMatch && iterations < 20) {
      iterations++;
      foundMatch = false;

      Pattern paramPattern = Pattern.compile(":(alias_\\d+)");
      Matcher paramMatcher = paramPattern.matcher(cleaned);

      while (paramMatcher.find()) {
        String paramName = paramMatcher.group(1);
        int paramPos = paramMatcher.start();

        // Extract the condition containing this parameter
        int conditionStart = findConditionStart(cleaned, paramPos);
        int conditionEnd = findConditionEnd(cleaned, paramMatcher.end());
        String condition = cleaned.substring(conditionStart, conditionEnd);

        if (containsAggregateFunction(condition)) {
          paramsToRemove.add(paramName);
          
          // Replace the entire condition with 1=1
          cleaned = cleaned.substring(0, conditionStart) + "1=1" + cleaned.substring(conditionEnd);
          foundMatch = true;
          break;
        }
      }
    }
    
    if (iterations >= 20) {
      log.warn("Reached maximum iterations while removing aggregate conditions");
    }

    // Clean up conditions and parentheses
    cleaned = cleanupConditions(cleaned);

    for (String paramName : paramsToRemove) {
      queryNamedParameters.remove(paramName);
    }

    return cleaned;
  }

  private int findConditionStart(String query, int fromPos) {
    // Look backwards to find where this condition starts
    int pos = fromPos - 1;
    int parenDepth = 0;

    while (pos >= 0) {
      char c = query.charAt(pos);

      if (c == ')') {
        parenDepth++;
      } else if (c == '(') {
        parenDepth--;

        // Check if this is a function opening paren (has word characters immediately before)
        boolean isFunctionParen = false;
        if (pos > 0) {
          int checkPos = pos - 1;
          // Skip backwards over word characters to see if this is a function
          while (checkPos >= 0 && (Character.isLetterOrDigit(query.charAt(checkPos)) || query.charAt(checkPos) == '_')) {
            checkPos--;
          }
          // If we found word chars, this is a function paren like "upper(" or "Max("
          isFunctionParen = (checkPos < pos - 1);
        }

        // If it's NOT a function paren and we're going negative depth, stop here
        if (!isFunctionParen && parenDepth < 0) {
          return pos + 1; // Start after the opening paren
        }
      }

      // Only check for keywords when at depth 0 and not inside function parens
      if (parenDepth == 0) {
        // Check for " AND " (with spaces)
        if (pos >= 5) {
          String substr = query.substring(pos - 4, pos + 1);
          if (substr.equalsIgnoreCase(AND)) {
            return pos + 1;
          }
        }

        // Check for " OR " (with spaces)
        if (pos >= 4) {
          String substr = query.substring(pos - 3, pos + 1);
          if (substr.equalsIgnoreCase(" OR ")) {
            return pos + 1;
          }
        }

        // Check for "WHERE "
        if (pos >= 6) {
          String substr = query.substring(pos - 5, pos + 1);
          if (substr.equalsIgnoreCase(WHERE)) {
            return pos + 1;
          }
        }
      }

      pos--;
    }

    return 0;
  }

  private int findConditionEnd(String query, int fromPos) {
    int pos = fromPos;
    int parenDepth = 0;
    
    while (pos < query.length()) {
      char c = query.charAt(pos);
      
      if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        // Check for escape clause before considering this a real closing paren
        int escapeEndPos = checkForEscapeClause(query, pos);
        if (escapeEndPos != -1) {
          pos = escapeEndPos;
          continue;
        }

        parenDepth--;

        // Check if this closes a function (has word chars before the matching open paren)
        boolean closesFunctionParen = false;
        if (parenDepth >= 0) {
          // Find the matching opening paren
          int matchPos = findMatchingOpenParen(query, pos);
          if (matchPos > 0) {
            // Check if there's a function name before it
            int checkPos = matchPos - 1;
            while (checkPos >= 0 && (Character.isLetterOrDigit(query.charAt(checkPos)) || query.charAt(checkPos) == '_')) {
              checkPos--;
            }
            closesFunctionParen = (checkPos < matchPos - 1);
          }
        }

        // If we're at depth 0 or below, and this is NOT a function paren, we've found the end
        if (parenDepth < 0 || (parenDepth == 0 && !closesFunctionParen)) {
          return pos;
        }
      } else if (parenDepth == 0) {
        // Check for terminating keywords only at depth 0
        int keywordPos = checkForTerminatingKeyword(query, pos);
        if (keywordPos != -1) {
          return keywordPos;
        }
      }
      pos++;
    }

    return query.length();
  }

  private int findMatchingOpenParen(String query, int closingPos) {
    int depth = 1;
    for (int i = closingPos - 1; i >= 0; i--) {
      if (query.charAt(i) == ')') {
        depth++;
      } else if (query.charAt(i) == '(') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }
    return -1;
  }

  private String cleanupConditions(String query) {
    String cleaned = query;

    // Multiple passes to handle nested cleanup
    for (int pass = 0; pass < 5; pass++) {

      // Handle spacing issues with 1=1
      cleaned = cleaned.replace("and1=1", "and 1=1");
      cleaned = cleaned.replace("1=1and", "1=1 and");
      cleaned = cleaned.replace("AND1=1", "AND 1=1");
      cleaned = cleaned.replace("1=1AND", "1=1 AND");

      // Remove duplicate 1=1 conditions
      cleaned = cleaned.replaceAll("1=1\\s+and\\s+1=1", "1=1");
      cleaned = cleaned.replaceAll("1=1\\s+AND\\s+1=1", "1=1");

      // Remove "AND 1=1 AND" patterns
      cleaned = cleaned.replaceAll("\\s+AND\\s+1=1\\s+AND\\s+", AND);
      cleaned = cleaned.replaceAll("\\s+and\\s+1=1\\s+and\\s+", " and ");

      // Remove "1=1 AND" at start of parenthetical group
      cleaned = cleaned.replaceAll("\\(\\s*1=1\\s+AND\\s+", "(");
      cleaned = cleaned.replaceAll("\\(\\s*1=1\\s+and\\s+", "(");

      // Remove "AND 1=1" at end of parenthetical group
      cleaned = cleaned.replaceAll("\\s+AND\\s+1=1\\s*\\)", ")");
      cleaned = cleaned.replaceAll("\\s+and\\s+1=1\\s*\\)", ")");

      // Remove standalone "1=1" in parentheses
      cleaned = cleaned.replaceAll("\\(\\s*1=1\\s*\\)", "");

      // Remove empty parentheses
      cleaned = cleaned.replaceAll("\\(\\s*\\)", "");

      // NEW: Remove orphaned "and" or "AND" at the start of parentheses
      cleaned = cleaned.replaceAll("\\(\\s*and\\s+", "(");
      cleaned = cleaned.replaceAll("\\(\\s*AND\\s+", "(");

      // NEW: Remove orphaned "and" or "AND" after opening double parens
      cleaned = cleaned.replaceAll("\\(\\(\\s*and\\s+", "((");
      cleaned = cleaned.replaceAll("\\(\\(\\s*AND\\s+", "((");

      // Clean up "AND ()" or "() AND" patterns
      cleaned = cleaned.replaceAll("\\s+AND\\s+\\(\\s*\\)\\s+", AND);
      cleaned = cleaned.replaceAll("\\s+and\\s+\\(\\s*\\)\\s+", " and ");
      cleaned = cleaned.replaceAll("\\(\\s*\\)\\s+AND\\s+", "");
      cleaned = cleaned.replaceAll("\\(\\s*\\)\\s+and\\s+", "");

      // Fix "AND  AND" or "and  and" (double operators)
      cleaned = cleaned.replaceAll("AND\\s+AND", "AND");
      cleaned = cleaned.replaceAll("and\\s+and", "and");
      cleaned = cleaned.replaceAll("AND\\s+and", "AND");
      cleaned = cleaned.replaceAll("and\\s+AND", "and");

      // Fix "WHERE 1=1 AND" -> "WHERE"
      cleaned = cleaned.replaceAll("WHERE\\s+1=1\\s+AND\\s+", WHERE);
      cleaned = cleaned.replaceAll("WHERE\\s+1=1\\s+and\\s+", WHERE);

      // NEW: Remove patterns like "1=1 and 1=1 and" -> "1=1 and"
      cleaned = cleaned.replaceAll("1=1\\s+and\\s+1=1\\s+and\\s+", "");
      cleaned = cleaned.replaceAll("1=1\\s+AND\\s+1=1\\s+AND\\s+", "");

      // CRITICAL FIX: Remove double parentheses patterns like ((condition))
      // This fixes the issue where AND ((pc.id = :alias_7 )) becomes AND (pc.id = :alias_7 )
      cleaned = cleaned.replaceAll("\\(\\s*\\(\\s*([^()]+)\\s*\\)\\s*\\)", "($1)");

      // Handle triple or more nested empty parentheses
      cleaned = cleaned.replace("\\(\\(\\(", "((");
      cleaned = cleaned.replace("\\)\\)\\)", "))");
    }

    // Final cleanup: balance parentheses
    cleaned = balanceParentheses(cleaned);

    return cleaned;
  }

  private String balanceParentheses(String query) {
    int openCount = 0;
    int closeCount = 0;

    for (char c : query.toCharArray()) {
      if (c == '(') openCount++;
      else if (c == ')') closeCount++;
    }

    if (openCount == closeCount) {
      return query;
    }

    if (closeCount > openCount) {
      int toRemove = closeCount - openCount;
      StringBuilder result = new StringBuilder(query);

      // Find the WHERE clause to focus cleanup there
      String upperQuery = result.toString().toUpperCase();
      int wherePos = upperQuery.indexOf(" WHERE ");
      int groupPos = upperQuery.indexOf(" GROUP BY");

      if (wherePos == -1 || groupPos == -1) {
        return query; // Can't safely clean up
      }

      // Remove extra closing parens in the WHERE clause area, working backwards
      for (int i = groupPos - 1; i > wherePos && toRemove > 0; i--) {
        if (result.charAt(i) == ')') {
          // Count balance from WHERE to this position
          int balance = 0;
          for (int j = wherePos; j < i; j++) {
            if (result.charAt(j) == '(') balance++;
            else if (result.charAt(j) == ')') balance--;
          }

          // If balance is negative, we have too many closing parens
          if (balance < 0) {
            result.deleteCharAt(i);
            toRemove--;
          }
        }
      }

      return result.toString();
    }

    return query;
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
