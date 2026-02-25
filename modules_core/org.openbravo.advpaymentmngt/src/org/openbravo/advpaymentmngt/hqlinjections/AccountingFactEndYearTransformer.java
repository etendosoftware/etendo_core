package org.openbravo.advpaymentmngt.hqlinjections;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
  /**
   * Maximum number of unclosed parentheses that this transformer is willing to auto-close.
   * If the count exceeds this value it almost certainly means the query structure is unexpected
   * or corrupted, so we refuse to patch it rather than blindly appending closing parens that
   * could alter query semantics.
   */
  private static final int MAX_UNCLOSED_PARENS = 10;
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
  public static final String CREATION_DATE = "creationDate";
  public static final String ACCOUNT = "account";
  public static final String GENERAL_LEDGER = "generalLedger";
  public static final String GL_ITEM = "glItem";
  public static final String TYPE = "type";
  public static final String CREATED_BY = "createdBy";
  public static final String UPDATED_BY = "updatedBy";
  public static final String FROM = "FROM ";

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

      // Fix ORDER BY to use aliases instead of subqueries/aggregates
      cleanedQuery = fixOrderByClause(cleanedQuery);

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
    Object value = extractValue(criterion);
    String aggregateExpr = getAggregateExpression(fieldName);

    String condition = buildCondition(aggregateExpr, operator, value, queryNamedParameters, paramCounter);
    if (condition != null) {
      conditions.add(condition);
      return true;
    }

    return false;
  }

  /**
   * Extracts the value from a JSON criterion, handling both simple values and JSONArray.
   * When the value is a JSONArray with a single element, extracts that element.
   */
  private Object extractValue(JSONObject criterion) throws JSONException {
    Object value = criterion.opt("value");

    // Handle JSONArray case (common for combo/selector fields)
    if (value instanceof JSONArray) {
      JSONArray arr = (JSONArray) value;
      if (arr.length() > 1) {
        log.warn("Multiple values in filter for field, using first value: {}", arr);
      }
      // opt(0) returns null safely if the array is empty, avoiding a potential JSONException
      return arr.length() > 0 ? arr.opt(0) : null;
    }

    return value;
  }

  private String getJoinOperator(JSONObject criteria) {
    String operator = criteria.optString("operator", "and");
    return StringUtils.equalsIgnoreCase("or", operator) ? " OR " : AND;
  }

  private boolean isAggregateField(String fieldName) {
    return StringUtils.equalsIgnoreCase(DEBIT, fieldName)
        || StringUtils.equalsIgnoreCase(CREDIT, fieldName)
        || StringUtils.equalsIgnoreCase("description", fieldName)
        || StringUtils.equalsIgnoreCase(CREATION_DATE, fieldName)
        || StringUtils.equalsIgnoreCase(UPDATED, fieldName)
        || StringUtils.equalsIgnoreCase(ACCOUNT, fieldName)
        || StringUtils.equalsIgnoreCase(GENERAL_LEDGER, fieldName)
        || StringUtils.equalsIgnoreCase(GL_ITEM, fieldName)
        || StringUtils.equalsIgnoreCase(TYPE, fieldName)
        || StringUtils.equalsIgnoreCase(CREATED_BY, fieldName)
        || StringUtils.equalsIgnoreCase(UPDATED_BY, fieldName);
  }

  private String getAggregateExpression(String fieldName) {
    if (StringUtils.equalsIgnoreCase(DEBIT, fieldName)) {
      return "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END";
    } else if (StringUtils.equalsIgnoreCase(CREDIT, fieldName)) {
      return "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END";
    } else if (StringUtils.equalsIgnoreCase("description", fieldName)) {
      return "Max(fa.description)";
    } else if (StringUtils.equalsIgnoreCase(CREATION_DATE, fieldName)) {
      return "Max(fa.creationDate)";
    } else if (StringUtils.equalsIgnoreCase(UPDATED, fieldName)) {
      return "Max(fa.updated)";
    } else if (StringUtils.equalsIgnoreCase(ACCOUNT, fieldName)) {
      return "Max(fa.account.searchKey)";
    } else if (StringUtils.equalsIgnoreCase(GENERAL_LEDGER, fieldName) || StringUtils.equalsIgnoreCase(GL_ITEM, fieldName)) {
      return "Max(fa.accountingSchema.name)";
    } else if (StringUtils.equalsIgnoreCase(TYPE, fieldName)) {
      return "Max(fa.type)";
    } else if (StringUtils.equalsIgnoreCase(CREATED_BY, fieldName)) {
      return "Max(fa.createdBy.name)";
    } else if (StringUtils.equalsIgnoreCase(UPDATED_BY, fieldName)) {
      return "Max(fa.updatedBy.name)";
    }
    return null;
  }

  private String buildCondition(String expr, String operator, Object value,
      Map<String, Object> queryNamedParameters, int[] paramCounter) {

    if (value == null) {
      return null;
    }

    boolean isDateField = expr.contains(CREATION_DATE) || expr.contains(UPDATED);
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
        log.error("Failed to parse date value: {}", dateValue, e2);
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
        int paramPos = paramMatcher.start();

        // Extract the condition containing this parameter
        int conditionStart = findConditionStart(cleaned, paramPos);
        int conditionEnd = findConditionEnd(cleaned, paramMatcher.end());
        String condition = cleaned.substring(conditionStart, conditionEnd);

        if (containsAggregateFunction(condition)) {

          // CRITICAL FIX: Find ALL alias_X parameters in this condition and mark them for removal
          // This handles cases where findConditionStart() captured adjacent conditions
          // For example: "fa.type = :alias_1) and upper((select ...)) like :alias_2"
          // We need to remove BOTH alias_1 and alias_2 from the map
          Matcher allParamsMatcher = paramPattern.matcher(condition);
          while (allParamsMatcher.find()) {
            String foundParam = allParamsMatcher.group(1);
            paramsToRemove.add(foundParam);

          }

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
    int pos = fromPos - 1;
    int parenDepth = 0;

    while (pos >= 0) {
      char currentChar = query.charAt(pos);
      parenDepth = updateDepthForChar(currentChar, parenDepth);

      if (shouldStopAtOpenParen(query, pos, currentChar, parenDepth)) {
        return pos + 1;
      }

      if (parenDepth == 0) {
        int keywordPos = findStartKeywordAtPosition(query, pos);
        if (keywordPos != -1) {
          return keywordPos;
        }
      }

      pos--;
    }

    return 0;
  }

  private int updateDepthForChar(char c, int currentDepth) {
    if (c == ')') {
      return currentDepth + 1;
    } else if (c == '(') {
      return currentDepth - 1;
    }
    return currentDepth;
  }

  private boolean shouldStopAtOpenParen(String query, int pos, char c, int parenDepth) {
    if (c != '(' || parenDepth >= 0) {
      return false;
    }
    return !isFunctionOpenParen(query, pos);
  }

  private boolean isFunctionOpenParen(String query, int pos) {
    if (pos == 0) {
      return false;
    }

    int checkPos = pos - 1;
    while (checkPos >= 0 && isWordCharacter(query.charAt(checkPos))) {
      checkPos--;
    }
    return checkPos < pos - 1;
  }

  private boolean isWordCharacter(char c) {
    return Character.isLetterOrDigit(c) || c == '_';
  }

  private int findStartKeywordAtPosition(String query, int pos) {
    if (matchesKeywordAtPosition(query, pos, AND, 5)) {
      return pos + 1;
    }
    if (matchesKeywordAtPosition(query, pos, " OR ", 4)) {
      return pos + 1;
    }
    if (matchesKeywordAtPosition(query, pos, WHERE, 6)) {
      return pos + 1;
    }
    return -1;
  }

  private int findConditionEnd(String query, int fromPos) {
    int pos = fromPos;
    int parenDepth = 0;

    while (pos < query.length()) {
      char currentChar = query.charAt(pos);

      if (currentChar == '(') {
        parenDepth++;
      } else if (currentChar == ')') {
        ClosingParenResult result = handleClosingParen(query, pos, parenDepth);
        if (result.shouldReturn) {
          return result.position;
        }
        if (result.shouldSkip) {
          pos = result.position;
          continue;
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

  private ClosingParenResult handleClosingParen(String query, int pos, int parenDepth) {
    int escapeEndPos = checkForEscapeClause(query, pos);
    if (escapeEndPos != -1) {
      return ClosingParenResult.skip(escapeEndPos);
    }

    parenDepth--;
    if (shouldStopAtCloseParen(query, pos, parenDepth)) {
      return ClosingParenResult.returnPosition(pos);
    }

    return ClosingParenResult.continueProcessing();
  }

  private static class ClosingParenResult {
    final int position;
    final boolean shouldReturn;
    final boolean shouldSkip;

    private ClosingParenResult(int position, boolean shouldReturn, boolean shouldSkip) {
      this.position = position;
      this.shouldReturn = shouldReturn;
      this.shouldSkip = shouldSkip;
    }

    static ClosingParenResult returnPosition(int position) {
      return new ClosingParenResult(position, true, false);
    }

    static ClosingParenResult skip(int position) {
      return new ClosingParenResult(position, false, true);
    }

    static ClosingParenResult continueProcessing() {
      return new ClosingParenResult(0, false, false);
    }
  }

  private boolean shouldStopAtCloseParen(String query, int pos, int parenDepth) {
    if (parenDepth < 0) {
      return true;
    }
    if (parenDepth > 0) {
      return false;
    }
    return !closesFunction(query, pos);
  }

  private boolean closesFunction(String query, int pos) {
    int matchPos = findMatchingOpenParen(query, pos);
    if (matchPos <= 0) {
      return false;
    }
    return isFunctionOpenParen(query, matchPos);
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

      // CRITICAL: Fix missing AND before 1=1 after closing parenthesis
      // Pattern: ) 1=1 -> ) AND 1=1
      cleaned = cleaned.replaceAll("\\)\\s+1=1(?!\\s)", ") AND 1=1");
      cleaned = cleaned.replaceAll("\\)\\s+1=1\\s+(?i)and\\s+", ") AND 1=1 and ");
      cleaned = cleaned.replaceAll("\\)\\s+1=1\\s+(?i)or\\s+", ") AND 1=1 or ");

      // CRITICAL: Remove "1=1 1=1" patterns (no operator between them)
      // This happens when multiple aggregate conditions are replaced
      cleaned = cleaned.replaceAll("1=1\\s+1=1", "1=1 AND 1=1");

      // After inserting ANDs, collapse multiple 1=1 with operators
      cleaned = cleaned.replaceAll("1=1\\s+(?i)and\\s+1=1", "1=1");

      // Remove duplicate 1=1 conditions with operators
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

      // Handle triple or more nested empty parentheses
      cleaned = cleaned.replace("\\(\\(\\(", "((");
      cleaned = cleaned.replace("\\)\\)\\)", "))");
    }

    cleaned = fixUnclosedParensBeforeGroupBy(cleaned);
    return cleaned;
  }

  /**
   * Finds the position of the main FROM keyword (not ones inside subqueries)
   */
  private int findMainFromPosition(String upperQuery) {
    int result = findFromWithWordBoundary(upperQuery);
    return result != -1 ? result : findFromOutsideParentheses(upperQuery);
  }

  private int findFromWithWordBoundary(String upperQuery) {
    int parenDepth = 0;
    int pos = 0;

    while (pos < upperQuery.length() - 5) {
      char c = upperQuery.charAt(pos);

      if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        parenDepth--;
      } else if (isValidFromKeyword(upperQuery, pos, parenDepth)) {
        return pos;
      }
      pos++;
    }

    return -1;
  }

  private boolean isValidFromKeyword(String upperQuery, int pos, int parenDepth) {
    return parenDepth == 0 && pos + 5 <= upperQuery.length() &&
           upperQuery.startsWith(FROM, pos) &&
           (pos == 0 || !Character.isLetterOrDigit(upperQuery.charAt(pos - 1)));
  }

  private int findFromOutsideParentheses(String upperQuery) {
    int fromPos = upperQuery.indexOf(FROM);
    while (fromPos != -1) {
      if (countParenDepth(upperQuery, fromPos) == 0) {
        return fromPos;
      }
      fromPos = upperQuery.indexOf(FROM, fromPos + 5);
    }
    return 0;
  }

  private int countParenDepth(String upperQuery, int upToPos) {
    int depth = 0;
    for (int i = 0; i < upToPos; i++) {
      if (upperQuery.charAt(i) == '(') {
        depth++;
      } else if (upperQuery.charAt(i) == ')') {
        depth--;
      }
    }
    return depth;
  }

  /**
   * Fixes unclosed parentheses before GROUP BY keyword.
   * Counts parentheses in WHERE clause and adds missing closing ones.
   */
  private String fixUnclosedParensBeforeGroupBy(String query) {
    String upperQuery = query.toUpperCase();
    int fromPos = findMainFromPosition(upperQuery);
    int wherePos = upperQuery.indexOf(" WHERE ", fromPos);
    int groupPos = upperQuery.indexOf(" GROUP BY", wherePos);

    if (wherePos == -1 || groupPos == -1) {

      return query;
    }

    // Count parentheses in WHERE clause
    int openCount = 0;
    int closeCount = 0;
    for (int i = wherePos; i < groupPos; i++) {
      char c = query.charAt(i);
      if (c == '(') openCount++;
      else if (c == ')') closeCount++;
    }

    // If there are more opening than closing, add the missing ones before GROUP BY
    if (openCount > closeCount) {
      int missing = openCount - closeCount;

      if (missing > MAX_UNCLOSED_PARENS) {
        log.warn(
            "fixUnclosedParensBeforeGroupBy - suspiciously large parenthesis imbalance (open={}, close={}). "
                + "Refusing to auto-close {} parens to avoid altering query structure.",
            openCount, closeCount, missing);
        return query;
      }

      // Build the closing string with an explicit, bounded loop instead of String.repeat()
      // to make the intent clear and prevent any tooling from flagging string repetition.
      StringBuilder closingParens = new StringBuilder(missing);
      for (int i = 0; i < missing; i++) {
        closingParens.append(')');
      }

      // Insert before GROUP BY
      return query.substring(0, groupPos) + closingParens + " " + query.substring(groupPos);
    } else if (openCount < closeCount) {
      log.warn("fixUnclosedParensBeforeGroupBy - MORE closing than opening parens! open={}, close={}",
          openCount, closeCount);
    }
    return query;
  }

  private boolean containsAggregateFunction(String expression) {
    // Check if expression contains common aggregate functions or subqueries
    String upper = expression.toUpperCase();

    // Check for aggregate functions
    if (upper.contains("SUM(") ||
        upper.contains("MAX(") ||
        upper.contains("MIN(") ||
        upper.contains("AVG(") ||
        upper.contains("COUNT(")) {
      return true;
    }

    // Check for subqueries (select statements within parentheses)
    // This is needed for columns like General Ledger that use subqueries instead of aggregates
    return upper.contains("(SELECT ") || upper.contains("( SELECT ");
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

  /**
   * Fixes ORDER BY clause to use aliases instead of subqueries or complex expressions.
   * When a query has GROUP BY and ORDER BY contains subqueries or aggregate functions,
   * Hibernate/HQL cannot parse them properly. This method replaces those expressions
   * with their corresponding aliases from the SELECT clause.
   */
  private String fixOrderByClause(String hqlQuery) {
    String upperHql = hqlQuery.toUpperCase();
    int orderByPos = upperHql.indexOf("ORDER BY");
    int groupByPos = upperHql.indexOf("GROUP BY");

    // Only fix if both GROUP BY and ORDER BY exist
    if (groupByPos == -1 || orderByPos == -1 || orderByPos < groupByPos) {
      return hqlQuery;
    }

    // Extract the ORDER BY clause
    String beforeOrderBy = hqlQuery.substring(0, orderByPos);
    String orderByClause = hqlQuery.substring(orderByPos + 8).trim(); // +8 for "ORDER BY"

    // Build a map of expressions to aliases from the SELECT clause
    Map<String, String> expressionToAlias = buildSelectExpressionMap(hqlQuery);

    // Replace expressions in ORDER BY with their aliases
    String fixedOrderBy = replaceOrderByExpressions(orderByClause, expressionToAlias);

    return beforeOrderBy + "ORDER BY " + fixedOrderBy;
  }

  /**
   * Builds a map of SELECT expressions to their aliases.
   * For example: "(select ev.searchKey ... ) AS Account_ID" → map entry: expression → "Account_ID"
   */
  private Map<String, String> buildSelectExpressionMap(String hqlQuery) {
    Map<String, String> map = new HashMap<>();

    int selectPos = hqlQuery.toUpperCase().indexOf("SELECT");
    int fromMainPos = findMainFromClause(hqlQuery, selectPos);

    if (selectPos == -1 || fromMainPos == -1) {
      return map;
    }

    String selectClause = hqlQuery.substring(selectPos + 6, fromMainPos).trim();

    // Parse the SELECT clause to extract all expressions with their aliases
    // Split by commas, but respect parentheses
    List<String> selectItems = splitSelectClause(selectClause);

    for (String item : selectItems) {
      // Look for "AS alias_name" pattern
      Pattern asPattern = Pattern.compile("\\s+AS\\s+(\\w+)\\s*$", Pattern.CASE_INSENSITIVE);
      Matcher asMatcher = asPattern.matcher(item);

      if (asMatcher.find()) {
        String alias = asMatcher.group(1);
        String expression = item.substring(0, asMatcher.start()).trim();
        map.put(normalizeExpression(expression), alias);
      }
    }

    return map;
  }

  /**
   * Finds the main FROM clause position, ignoring FROMs inside subqueries.
   */
  private int findMainFromClause(String query, int afterPos) {
    int pos = afterPos;
    int parenDepth = 0;
    String upperQuery = query.toUpperCase();

    while (pos < query.length()) {
      char c = query.charAt(pos);

      if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        parenDepth--;
      } else if (parenDepth == 0 && pos + 4 <= query.length()) {
        // Check for FROM keyword at parenthesis depth 0
        String potentialFrom = upperQuery.substring(pos, Math.min(pos + 5, query.length()));
        if (potentialFrom.startsWith(FROM) || potentialFrom.equals("FROM")) {
          return pos;
        }
      }

      pos++;
    }

    return -1;
  }

  /**
   * Splits SELECT clause items by commas, respecting parentheses.
   */
  private List<String> splitSelectClause(String selectClause) {
    List<String> items = new ArrayList<>();
    StringBuilder currentItem = new StringBuilder();
    int parenDepth = 0;

    for (int i = 0; i < selectClause.length(); i++) {
      char c = selectClause.charAt(i);

      if (c == '(') {
        parenDepth++;
        currentItem.append(c);
      } else if (c == ')') {
        parenDepth--;
        currentItem.append(c);
      } else if (c == ',' && parenDepth == 0) {
        // This comma is at the top level, it's a separator
        if (currentItem.length() > 0) {
          items.add(currentItem.toString().trim());
          currentItem.setLength(0);
        }
      } else {
        currentItem.append(c);
      }
    }

    // Add the last item
    if (currentItem.length() > 0) {
      items.add(currentItem.toString().trim());
    }

    return items;
  }

  /**
   * Normalizes an expression by removing extra whitespace and standardizing format.
   */
  private String normalizeExpression(String expression) {
    return expression.replaceAll("\\s+", " ").trim();
  }

  /**
   * Replaces complex expressions in ORDER BY with their corresponding aliases.
   */
  private String replaceOrderByExpressions(String orderByClause, Map<String, String> expressionToAlias) {
    String result = orderByClause;

    // FIRST PASS: Replace subqueries starting with (select
    result = replaceSubqueriesInOrderBy(result, expressionToAlias);

    // SECOND PASS: Replace aggregate functions and CASE expressions
    result = replaceAggregateFunctionsInOrderBy(result, expressionToAlias);

    // THIRD PASS: Replace alias.property patterns (e.g., Createdby.name -> Createdby)
    result = replaceAliasPropertyReferences(result, expressionToAlias);

    // Clean up any leftover issues like double commas or leading/trailing commas
    result = result.replaceAll(",\\s*,", ",");
    result = result.replaceAll("^\\s*,\\s*", "");
    result = result.replaceAll(",\\s*$", "");
    result = result.trim();

    // If result is empty or just whitespace, use a field from GROUP BY
    if (result.isEmpty()) {
      // Use a simple field that's in the GROUP BY clause (fa.client.id is always there)
      return "fa.client.id";
    }

    return result;
  }

  /**
   * Replaces subqueries (select...) in ORDER BY clause with aliases.
   */
  private String replaceSubqueriesInOrderBy(String orderByClause, Map<String, String> expressionToAlias) {
    int startPos = 0;
    StringBuilder sb = new StringBuilder();
    boolean processing = true;

    while (processing && startPos < orderByClause.length()) {
      int selectPos = orderByClause.toLowerCase().indexOf("(select", startPos);

      if (selectPos == -1) {
        // No more subqueries, append the rest
        sb.append(orderByClause.substring(startPos));
        processing = false;
      } else {
        // Append everything before the subquery
        sb.append(orderByClause, startPos, selectPos);

        // Find the matching closing parenthesis
        int closingPos = findMatchingClosingParen(orderByClause, selectPos);

        if (closingPos == -1) {
          // Malformed query, append the rest
          sb.append(orderByClause.substring(selectPos));
          processing = false;
        } else {
          String subquery = orderByClause.substring(selectPos, closingPos + 1);
          String normalized = normalizeExpression(subquery);

          // Look for this expression in our map
          String alias = expressionToAlias.get(normalized);

          if (alias != null) {
            sb.append(alias);
            startPos = closingPos + 1;
          } else {
            // If no alias found, skip the subquery entirely
            log.warn("No alias found for ORDER BY subquery, removing: {}", subquery);
            // Skip the sort direction as well (ASC/DESC)
            startPos = skipSortDirection(orderByClause, closingPos + 1);
          }
        }
      }
    }

    return sb.toString();
  }

  /**
   * Replaces aggregate functions (Max, Sum, etc.) and CASE expressions in ORDER BY with aliases.
   */
  private String replaceAggregateFunctionsInOrderBy(String orderByClause, Map<String, String> expressionToAlias) {
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    boolean processing = true;

    while (processing && pos < orderByClause.length()) {
      // Look for aggregate functions or CASE expressions
      int nextExprPos = findNextAggregateOrCase(orderByClause, pos);

      if (nextExprPos == -1) {
        // No more expressions, append the rest
        sb.append(orderByClause.substring(pos));
        processing = false;
      } else {
        // Append everything before the expression
        sb.append(orderByClause, pos, nextExprPos);

        // Extract the full expression
        ExpressionMatch match = extractExpression(orderByClause, nextExprPos);

        if (match == null) {
          // Failed to extract, append and move forward
          sb.append(orderByClause.charAt(nextExprPos));
          pos = nextExprPos + 1;
        } else {
          String normalized = normalizeExpression(match.expression);
          String alias = expressionToAlias.get(normalized);

          if (alias != null) {
            sb.append(alias);
            pos = match.endPos;
          } else {
            // No alias found, remove the expression
            log.warn("No alias found for ORDER BY expression, removing: {}", match.expression);
            // Skip the sort direction as well (ASC/DESC)
            pos = skipSortDirection(orderByClause, match.endPos);
          }
        }
      }
    }

    return sb.toString();
  }

  /**
   * Finds the next aggregate function (Max, Sum, Min, Avg, Count) or CASE expression start position.
   */
  private int findNextAggregateOrCase(String orderByClause, int startPos) {
    String upperClause = orderByClause.toUpperCase();
    int minPos = orderByClause.length();

    // Check for aggregate functions
    String[] aggregates = {"MAX(", "MIN(", "SUM(", "AVG(", "COUNT("};
    for (String agg : aggregates) {
      int pos = upperClause.indexOf(agg, startPos);
      if (pos != -1 && pos < minPos) {
        minPos = pos;
      }
    }

    // Check for CASE expressions
    int casePos = upperClause.indexOf("CASE ", startPos);
    if (casePos != -1 && casePos < minPos) {
      minPos = casePos;
    }

    return minPos < orderByClause.length() ? minPos : -1;
  }

  /**
   * Extracts a complete expression starting at the given position.
   * Handles aggregate functions and CASE expressions.
   */
  private ExpressionMatch extractExpression(String orderByClause, int startPos) {
    String upperClause = orderByClause.toUpperCase();

    // Check if it's a CASE expression
    if (upperClause.startsWith("CASE ", startPos)) {
      return extractCaseExpression(orderByClause, startPos);
    }

    // Otherwise, it's an aggregate function - find matching closing paren
    int openParenPos = orderByClause.indexOf('(', startPos);
    if (openParenPos == -1) {
      return null;
    }

    int closingPos = findMatchingClosingParen(orderByClause, openParenPos);
    if (closingPos == -1) {
      return null;
    }

    String expression = orderByClause.substring(startPos, closingPos + 1);
    return new ExpressionMatch(expression, closingPos + 1);
  }

  /**
   * Extracts a CASE expression (CASE WHEN ... END).
   */
  private ExpressionMatch extractCaseExpression(String orderByClause, int startPos) {
    String upperClause = orderByClause.toUpperCase();
    int endPos = upperClause.indexOf("END", startPos);

    if (endPos == -1) {
      return null;
    }

    // Move past "END"
    endPos += 3;

    String expression = orderByClause.substring(startPos, endPos);
    return new ExpressionMatch(expression, endPos);
  }

  /**
   * Skips past ASC or DESC keywords after an expression.
   */
  private int skipSortDirection(String orderByClause, int startPos) {
    int pos = startPos;

    // Skip whitespace
    while (pos < orderByClause.length() && Character.isWhitespace(orderByClause.charAt(pos))) {
      pos++;
    }

    // Check for ASC or DESC
    String remaining = orderByClause.substring(pos).toUpperCase();
    if (remaining.startsWith("ASC")) {
      return pos + 3;
    } else if (remaining.startsWith("DESC")) {
      return pos + 4;
    }

    return pos;
  }

  /**
   * Replaces alias.property patterns in ORDER BY with just the alias.
   * For example: "Createdby.name" -> "Createdby" if Createdby is a known alias.
   * This is needed because when an alias represents an aggregate function,
   * you cannot access properties on it.
   */
  private String replaceAliasPropertyReferences(String orderByClause, Map<String, String> expressionToAlias) {
    Set<String> knownAliases = new HashSet<>(expressionToAlias.values());

    if (knownAliases.isEmpty()) {
      return orderByClause;
    }

    StringBuilder result = new StringBuilder();
    int pos = 0;

    while (pos < orderByClause.length()) {
      pos = skipNonAlphanumeric(orderByClause, pos, result);

      if (pos >= orderByClause.length()) {
        break;
      }

      WordMatch wordMatch = extractWord(orderByClause, pos);
      pos = processWord(orderByClause, wordMatch, knownAliases, result);
    }

    return result.toString();
  }

  private int skipNonAlphanumeric(String text, int startPos, StringBuilder result) {
    int pos = startPos;
    while (pos < text.length() && !Character.isLetterOrDigit(text.charAt(pos))) {
      result.append(text.charAt(pos));
      pos++;
    }
    return pos;
  }

  private WordMatch extractWord(String text, int startPos) {
    int endPos = startPos;
    while (endPos < text.length() && isWordCharacter(text.charAt(endPos))) {
      endPos++;
    }
    String word = text.substring(startPos, endPos);
    return new WordMatch(word, startPos, endPos);
  }

  private int processWord(String orderByClause, WordMatch wordMatch, Set<String> knownAliases, StringBuilder result) {
    if (isAliasPropertyPattern(orderByClause, wordMatch.endPos)) {
      return handleAliasProperty(orderByClause, wordMatch, knownAliases, result);
    }
    result.append(wordMatch.word);
    return wordMatch.endPos;
  }

  private boolean isAliasPropertyPattern(String text, int pos) {
    return pos < text.length() && text.charAt(pos) == '.';
  }

  private int handleAliasProperty(String orderByClause, WordMatch wordMatch, Set<String> knownAliases, StringBuilder result) {
    int propertyEnd = findPropertyEnd(orderByClause, wordMatch.endPos + 1);

    if (knownAliases.contains(wordMatch.word)) {
      result.append(wordMatch.word);
      return propertyEnd;
    }
    result.append(wordMatch.word);
    return wordMatch.endPos;
  }

  private int findPropertyEnd(String text, int startPos) {
    int pos = startPos;
    while (pos < text.length() && isWordCharacter(text.charAt(pos))) {
      pos++;
    }
    return pos;
  }

  private static class WordMatch {
    final String word;
    final int startPos;
    final int endPos;

    WordMatch(String word, int startPos, int endPos) {
      this.word = word;
      this.startPos = startPos;
      this.endPos = endPos;
    }
  }

  /**
   * Holder for an extracted expression and its end position.
   */
  private static class ExpressionMatch {
    final String expression;
    final int endPos;

    ExpressionMatch(String expression, int endPos) {
      this.expression = expression;
      this.endPos = endPos;
    }
  }

  /**
   * Finds the matching closing parenthesis for an opening parenthesis at the given position.
   * Handles nested parentheses correctly.
   */
  private int findMatchingClosingParen(String str, int openPos) {
    if (openPos >= str.length() || str.charAt(openPos) != '(') {
      return -1;
    }

    int depth = 0;
    for (int i = openPos; i < str.length(); i++) {
      char c = str.charAt(i);
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return i;
        }
      }
    }

    return -1; // No matching closing paren found
  }
}
