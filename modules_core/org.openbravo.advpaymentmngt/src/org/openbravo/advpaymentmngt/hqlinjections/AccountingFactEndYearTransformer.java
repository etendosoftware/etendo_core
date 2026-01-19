/*
 *************************************************************************
 * Transformer that moves grid filters over aggregated columns (Debit/Credit)
 * from WHERE to HAVING for the FinancialMgmtAccountingFactEndYear HQL table.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.hqlinjections;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.json.AdvancedQueryBuilder;

@ComponentProvider.Qualifier("A45FDE07216C40FBA472E7B91F7273E4")
public class AccountingFactEndYearTransformer extends HqlQueryTransformer {

  private static final Logger log = LogManager.getLogger();
  private static final String TABLE_ID = "A45FDE07216C40FBA472E7B91F7273E4";

  @Override
  public String transformHqlQuery(String _hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    if (!requestParameters.containsKey("criteria")) {
      return _hqlQuery;
    }

    try {
      String criteriaStr = requestParameters.get("criteria");
      JSONObject criteria = org.openbravo.service.json.JsonUtils.buildCriteria(requestParameters);

      // Build HAVING clause directly from JSON criteria for debit/credit only
      StringBuilder havingClause = new StringBuilder();
      int[] paramCounter = {0};
      boolean hasAggregateFilters = processJSONCriteria(criteria, havingClause, queryNamedParameters, paramCounter);
      
      // ALWAYS remove aggregate conditions from WHERE (even if no debit/credit filters)
      // because other fields like description use Max() and DAL puts them in WHERE
      String cleanedQuery = removeAggregateConditionsFromWhere(_hqlQuery, queryNamedParameters);
      
      if (hasAggregateFilters) {
        String modifiedQuery = insertHavingClause(cleanedQuery, havingClause.toString());
        return modifiedQuery;
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
    
    if (!criteria.has("criteria")) {
      return false;
    }

    JSONArray criteriaArray = criteria.getJSONArray("criteria");
    boolean hasAggregate = false;
    java.util.List<String> conditions = new java.util.ArrayList<>();
    
    for (int i = 0; i < criteriaArray.length(); i++) {
      JSONObject criterion = criteriaArray.getJSONObject(i);
      
      // Handle nested criteria with recursion
      if (criterion.has("criteria")) {
        StringBuilder nestedHaving = new StringBuilder();
        if (processJSONCriteria(criterion, nestedHaving, queryNamedParameters, paramCounter)) {
          conditions.add("(" + nestedHaving.toString() + ")");
          hasAggregate = true;
        }
        continue;
      }
      
      if (!criterion.has("fieldName")) {
        continue;
      }
      
      String fieldName = criterion.getString("fieldName");
      
      // Only process debit and credit fields
      if ("debit".equalsIgnoreCase(fieldName) || "credit".equalsIgnoreCase(fieldName)) {
        String operator = criterion.optString("operator", "equals");
        Object value = criterion.opt("value");
        
        // Build the aggregate expression
        String aggregateExpr;
        if ("debit".equalsIgnoreCase(fieldName)) {
          aggregateExpr = "CASE WHEN Sum(fa.debit - fa.credit) > 0 THEN Sum(fa.debit - fa.credit) ELSE 0 END";
        } else {
          aggregateExpr = "CASE WHEN Sum(fa.credit - fa.debit) > 0 THEN Sum(fa.credit - fa.debit) ELSE 0 END";
        }
        
        // Build the condition
        String condition = buildCondition(aggregateExpr, operator, value, queryNamedParameters, paramCounter);
        if (condition != null) {
          conditions.add(condition);
          hasAggregate = true;
        }
      }
    }
    
    if (!conditions.isEmpty()) {
      String operator = criteria.optString("operator", "and");
      String join = " AND ";
      if ("or".equalsIgnoreCase(operator)) {
        join = " OR ";
      }
      havingClause.append(String.join(join, conditions));
    }
    
    return hasAggregate;
  }

  private String buildCondition(String expr, String operator, Object value,
      Map<String, Object> queryNamedParameters, int[] paramCounter) {
    
    if (value == null) {
      return null;
    }
    
    // Convert Double to BigDecimal if needed (debit/credit are BigDecimal fields)
    Object paramValue = value;
    if (value instanceof Double) {
      paramValue = java.math.BigDecimal.valueOf((Double) value);
    } else if (value instanceof Integer) {
      paramValue = java.math.BigDecimal.valueOf((Integer) value);
    } else if (value instanceof Long) {
      paramValue = java.math.BigDecimal.valueOf((Long) value);
    }
    
    String paramName = "havingParam_" + paramCounter[0]++;
    queryNamedParameters.put(paramName, paramValue);
    
    switch (operator.toLowerCase()) {
      case "equals":
      case "iequals":
        return expr + " = :" + paramName;
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
    java.util.Set<String> paramsToRemove = new java.util.HashSet<>();
    String cleaned = hqlQuery;
    
    // Repeatedly remove any conditions containing aggregate functions
    boolean foundMatch = true;
    int iterations = 0;
    int totalMatches = 0;
    
    while (foundMatch && iterations < 20) { // increased safety limit
      iterations++;
      foundMatch = false;
      
      // Find any parameter :alias_N in the query, then check if its condition contains aggregates
      // This handles cases like: upper(Max(fa.description)) like upper(:alias_0)
      java.util.regex.Pattern paramPattern = java.util.regex.Pattern.compile(
        ":(alias_\\d+)"
      );
      java.util.regex.Matcher paramMatcher = paramPattern.matcher(cleaned);

      while (paramMatcher.find()) {
        String paramName = paramMatcher.group(1);
        int paramPos = paramMatcher.start();
        totalMatches++;

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
    cleaned = cleaned.replaceAll("and1=1", "and 1=1");
    cleaned = cleaned.replaceAll("1=1and", "1=1 and");
    
    // Handle multiple 1=1 with AND between them
    cleaned = cleaned.replaceAll("1=1\\s+and\\s+1=1", "1=1");
    cleaned = cleaned.replaceAll("AND\\s+1=1\\s+AND", "AND");
    cleaned = cleaned.replaceAll("\\s+AND\\s+1=1\\s*\\)", ")");
    cleaned = cleaned.replaceAll("\\(\\s*1=1\\s+AND\\s+", "(");
    cleaned = cleaned.replaceAll("\\s+AND\\s+1=1\\s+GROUP", " GROUP");
    cleaned = cleaned.replaceAll("WHERE\\s+1=1\\s+AND\\s+", "WHERE ");
    cleaned = cleaned.replaceAll("\\(\\(\\s*1=1\\s+and\\s+", "((");
    cleaned = cleaned.replaceAll("\\s+and\\s+1=1\\s+and\\s+", " and ");
    cleaned = cleaned.replaceAll("\\(\\s*1=1\\s+and\\s+", "(");
    
    // Final cleanup: remove standalone 1=1 in parentheses
    cleaned = cleaned.replaceAll("\\(\\s*1=1\\s*\\)", "");
    cleaned = cleaned.replaceAll("\\(\\(\\s*\\)", "((");
    cleaned = cleaned.replaceAll("\\(\\s*and\\s+", "(");
    
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
      if (pos >= 4 && query.substring(pos - 4, pos).equalsIgnoreCase(" AND")) {
        return pos;
      }
      if (pos >= 3 && query.substring(pos - 3, pos).equalsIgnoreCase(" OR")) {
        return pos;
      }
      if (pos >= 6 && query.substring(pos - 6, pos).equalsIgnoreCase("WHERE ")) {
        return pos;
      }
      // Check for opening double parenthesis which marks start of filter conditions: ((
      if (pos >= 2 && query.substring(pos - 2, pos).equals("((")) {
        return pos;
      }
      pos--;
    }
    
    return 0; // Start of string
  }
  
  private int findConditionEnd(String query, int fromPos) {
    // Look forwards to find where this condition ends
    // Condition ends before: AND, OR, closing parenthesis at depth 0, or end of string
    // Special case: include "escape 'x'" clause if it follows a closing paren (part of LIKE syntax)
    int pos = fromPos;
    int parenDepth = 0;
    
    while (pos < query.length()) {
      char c = query.charAt(pos);
      
      if (c == '(') {
        parenDepth++;
      } else if (c == ')') {
        if (parenDepth == 0) {
          // Found closing paren at same level
          // Check if followed by "escape" keyword (part of LIKE clause)
          int checkPos = pos + 1;
          while (checkPos < query.length() && Character.isWhitespace(query.charAt(checkPos))) {
            checkPos++;
          }
          if (checkPos + 6 <= query.length() && query.substring(checkPos, checkPos + 6).equalsIgnoreCase("escape")) {
            // Include the escape clause - find the end of the escape value
            checkPos += 6; // Skip "escape"
            while (checkPos < query.length() && Character.isWhitespace(query.charAt(checkPos))) {
              checkPos++;
            }
            // Skip the escape character (usually '|' with quotes)
            if (checkPos < query.length() && query.charAt(checkPos) == '\'') {
              checkPos++; // Skip opening quote
              while (checkPos < query.length() && query.charAt(checkPos) != '\'') {
                checkPos++;
              }
              if (checkPos < query.length()) {
                checkPos++; // Skip closing quote
              }
            }
            // Continue searching from after the escape clause
            pos = checkPos;
            continue;
          }
          // No escape clause, condition ends here
          return pos;
        }
        parenDepth--;
      } else if (parenDepth == 0) {
        // Check for AND, OR, GROUP, HAVING, ORDER at same depth
        if (pos + 4 <= query.length() && query.substring(pos, pos + 4).equalsIgnoreCase(" AND")) {
          return pos;
        }
        if (pos + 3 <= query.length() && query.substring(pos, pos + 3).equalsIgnoreCase(" OR")) {
          return pos;
        }
        if (pos + 6 <= query.length() && query.substring(pos, pos + 6).equalsIgnoreCase(" GROUP")) {
          return pos;
        }
        if (pos + 7 <= query.length() && query.substring(pos, pos + 7).equalsIgnoreCase(" HAVING")) {
          return pos;
        }
        if (pos + 6 <= query.length() && query.substring(pos, pos + 6).equalsIgnoreCase(" ORDER")) {
          return pos;
        }
      }
      pos++;
    }
    
    return query.length(); // End of string
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
