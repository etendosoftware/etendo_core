/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2011-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.userinterface.selector;

import static org.openbravo.userinterface.selector.SelectorConstants.includeOrgFilter;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Hibernate;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.LongDomainType;
import org.openbravo.base.model.domaintype.StringEnumerateDomainType;
import org.openbravo.base.model.domaintype.UniqueIdDomainType;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.service.datasource.DataSourceUtils;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

public class CustomQuerySelectorDatasource extends ReadOnlyDataSourceService {

  private static Logger log = LogManager.getLogger();
  private static final String ADDITIONAL_FILTERS = "@additional_filters@";
  public static final String ALIAS_PREFIX = "alias_";

  @Override
  protected int getCount(Map<String, String> parameters) {
    // we return -1, so that the super class calculates a valid count
    return -1;
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    // creation of formats is done here because they are not thread safe
    final SimpleDateFormat xmlDateFormat = JsonUtils.createDateFormat();
    final SimpleDateFormat xmlDateTimeFormat = JsonUtils.createDateTimeFormat();
    final List<Map<String, Object>> result = new ArrayList<>();
    final List<Object> typedParameters = new ArrayList<>();
    final Map<String, Object> namedParameters = new HashMap<>();
    // Defaulted to endRow + 2 to check for more records while scrolling.
    int totalRows = endRow + 2;
    int rowCount = 0;

    String selectorId = parameters.get(SelectorConstants.DS_REQUEST_SELECTOR_ID_PARAMETER);

    if (StringUtils.isEmpty(selectorId)) {
      return result;
    }

    OBContext.setAdminMode();
    try {

      Selector sel = OBDal.getInstance().get(Selector.class, selectorId);
      List<SelectorField> fields = OBDao.getActiveOBObjectList(sel,
          Selector.PROPERTY_OBUISELSELECTORFIELDLIST);

      // Forcing object initialization to prevent LazyInitializationException in case session is
      // cleared when number of records is big enough
      Hibernate.initialize(fields);

      // Parse the hql in case that optional filters are required
      String hql = parseOptionalFilters(parameters, sel, xmlDateFormat, typedParameters,
          namedParameters);

      String sortBy = parameters.get("_sortBy");
      hql += getSortClause(sortBy, sel);

      Query<Tuple> selQuery = OBDal.getInstance()
          .getSession()
          .createQuery(hql, Tuple.class);

      selQuery.setParameterList("clients", (String[]) namedParameters.get("clients"));
      if (namedParameters.containsKey("orgs")) {
        selQuery.setParameterList("orgs", (String[]) namedParameters.get("orgs"));
      }

      for (int i = 0; i < typedParameters.size(); i++) {
        selQuery.setParameter(ALIAS_PREFIX + Integer.toString(i), typedParameters.get(i));
      }

      if (startRow > 0) {
        selQuery.setFirstResult(startRow);
      }
      if (endRow > startRow) {
        selQuery.setMaxResults(endRow - startRow + 1);
      }

      for (Tuple tuple : selQuery.list()) {
        rowCount++;
        final Map<String, Object> data = new LinkedHashMap<>();
        for (SelectorField field : fields) {
          // TODO: throw an exception if the display expression doesn't match any returned alias.
          for (TupleElement<?> tupleElement : tuple.getElements()) {
            String alias = tupleElement.getAlias();
            if (alias != null && alias.equals(field.getDisplayColumnAlias())) {
              Object value = tuple.get(alias);
              if (value instanceof Date) {
                value = xmlDateFormat.format(value);
              }
              if (value instanceof Timestamp) {
                value = xmlDateTimeFormat.format(value);
                value = JsonUtils.convertToCorrectXSDFormat((String) value);
              }
              data.put(alias, value);
            }
          }
        }
        result.add(data);
      }
      if ("true".equals(parameters.get(JsonConstants.NOCOUNT_PARAMETER)) && startRow < endRow) {
        if (rowCount < endRow) {
          totalRows = rowCount;
        }
        parameters.put(JsonConstants.RESPONSE_TOTALROWS, String.valueOf(totalRows));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /**
   * Returns the selectors HQL query. In case that it contains the '@additional_filters@' String it
   * is replaced by a set of filter clauses.
   * 
   * These include a filter clause:
   * <ul>
   * <li>for the main entity's client by the context's client.</li>
   * <li>for the main entity's organization by an organization list see
   * {@link DataSourceUtils#getOrgs(String)}</li>
   * <li>with Selector's default filter expression.</li>
   * <li>for each default expression defined on the selector fields.</li>
   * <li>for each selector field in case exists a value for it on the parameters param.</li>
   * </ul>
   * 
   * @param parameters
   *          Map of String values with the request parameters.
   * @param sel
   *          the selector that it is being retrieved the data.
   * @param xmlDateFormat
   *          SimpleDataFormat to be used to parse date Strings.
   * @return a String with the HQL to be executed.
   */

  public String parseOptionalFilters(Map<String, String> parameters, Selector sel,
      SimpleDateFormat xmlDateFormat, List<Object> typedParameters) {
    return parseOptionalFilters(parameters, sel, xmlDateFormat, typedParameters, new HashMap<>());
  }

  /**
   * Returns the selectors HQL query. In case that it contains the '@additional_filters@' String it
   * is replaced by a set of filter clauses.
   *
   * These include a filter clause:
   * <ul>
   * <li>for the main entity's client by the context's client.</li>
   * <li>for the main entity's organization by an organization list see
   * {@link DataSourceUtils#getOrgs(String)}</li>
   * <li>with Selector's default filter expression.</li>
   * <li>for each default expression defined on the selector fields.</li>
   * <li>for each selector field in case exists a value for it on the parameters param.</li>
   * </ul>
   *
   * @param parameters
   *          Map of String values with the request parameters.
   * @param sel
   *          the selector that it is being retrieved the data.
   * @param xmlDateFormat
   *          SimpleDataFormat to be used to parse date Strings.
   * @param typedParameters
   *          Typed parameters to be used in the query
   * @param namedParameters
   *          Named parameters to be used in the query
   * @return a String with the HQL to be executed.
   */

  public String parseOptionalFilters(Map<String, String> parameters, Selector sel,
      SimpleDateFormat xmlDateFormat, List<Object> typedParameters,
      Map<String, Object> namedParameters) {
    String hql = sel.getHQL();
    if (!hql.contains(ADDITIONAL_FILTERS)) {
      return hql;
    }
    final String requestType = parameters.get(SelectorConstants.DS_REQUEST_TYPE_PARAMETER);
    final String entityAlias = sel.getEntityAlias();
    // Client filter
    String additionalFilter = entityAlias + ".client.id in :clients";
    final String[] clients = { "0", OBContext.getOBContext().getCurrentClient().getId() };
    namedParameters.put("clients", clients);
    if (includeOrgFilter(parameters)) {
      // Organization filter
      boolean isOrgSelector = sel.getTable().getName().equals("Organization");
      String orgs;
      if (isOrgSelector) {
        // Just retrieve the list of readable organizations in the current context
        orgs = DataSourceUtils.getOrgs(parameters.get(""));
      } else {
        orgs = DataSourceUtils.getOrgs(parameters.get(JsonConstants.ORG_PARAMETER));
      }
      if (StringUtils.isNotEmpty(orgs)) {
        additionalFilter += " and " + entityAlias;
        if (isOrgSelector) {
          additionalFilter += ".id in :orgs";
        } else {
          additionalFilter += ".organization.id in :orgs";
        }
        namedParameters.put("orgs", orgs.replaceAll("'", "").split(","));
      }
    }
    additionalFilter += getDefaultFilterExpression(sel, parameters);

    String defaultExpressionsFilter = "";
    boolean hasFilter = false;
    List<SelectorField> fields = OBDao.getActiveOBObjectList(sel,
        Selector.PROPERTY_OBUISELSELECTORFIELDLIST);
    HashMap<String, String[]> criteria = getCriteria(parameters);
    for (SelectorField field : fields) {
      if (StringUtils.isEmpty(field.getClauseLeftPart())) {
        continue;
      }
      String operator = null;
      String value = null;
      String[] operatorvalue = null;
      if (criteria != null) {
        operatorvalue = criteria.get(field.getDisplayColumnAlias());
        if (operatorvalue != null) {
          operator = operatorvalue[0];
          value = operatorvalue[1];
        }
      }
      if (StringUtils.isEmpty(value)) {
        value = parameters.get(field.getDisplayColumnAlias());
      }
      // Add field default expression on picklist if it is not already filtered. Default expressions
      // on selector popup are already evaluated and their values came in the parameters object.
      if (field.getDefaultExpression() != null && !"Window".equals(requestType)
          && StringUtils.isEmpty(value)) {
        try {
          String defaultValue = "";
          Object defaultValueObject = ParameterUtils.getJSExpressionResult(parameters,
              RequestContext.get().getSession(), field.getDefaultExpression());
          if (defaultValueObject != null) {
            defaultValue = defaultValueObject.toString();
          }
          if (StringUtils.isNotEmpty(defaultValue)) {
            defaultExpressionsFilter += " and " + getWhereClause(operator, defaultValue, field,
                xmlDateFormat, operatorvalue, typedParameters);
          }
        } catch (Exception e) {
          log.error("Error evaluating filter expression: " + e.getMessage(), e);
        }
      }
      if (field.isFilterable() && StringUtils.isNotEmpty(value)) {
        String whereClause = getWhereClause(operator, value, field, xmlDateFormat, operatorvalue,
            typedParameters);
        if (!hasFilter) {
          additionalFilter += " and (";
          hasFilter = true;
        } else {
          if ("Window".equals(requestType)) {
            additionalFilter += " and ";
          } else {
            additionalFilter += " or ";
          }
        }
        additionalFilter += whereClause;
      }
    }
    if (hasFilter) {
      additionalFilter += ")";
    }
    if (defaultExpressionsFilter.length() > 0) {
      additionalFilter += defaultExpressionsFilter;
    }
    hql = hql.replace(ADDITIONAL_FILTERS, additionalFilter);
    return hql;
  }

  /**
   * Returns the where clause of a selector's field based on the given value.
   * 
   * This method based on the DomainType of the selector field returns the filter clause using the
   * clause left part defined on the selector field.
   * <ul>
   * <li>Numeric Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>Date Domain Type: Returns a multiple clause comparing separately value's day, month and
   * year.</li>
   * <li>Boolean Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>Foreign Key Domain Type: Returns an equals clause <i>field.clauseLeftPart.id =
   * value</i></li>
   * <li>Unique Id Domain Type: Returns an equals clause <i>field.clauseLeftPart = value</i></li>
   * <li>String Domain Type: Compares the clause left part with the value using the upper database
   * function which to make comparison case insensitive.
   * </ul>
   * 
   * @param operator
   *          String with the operator of the filter expression to be applied on the selector
   *          field's column.
   * @param value
   *          String with the value that the selector field's column is filtered by.
   * @param field
   *          The SelectorField that is filtered.
   * @param xmlDateFormat
   *          SimpleDateFormat to parse the value in case the field is a Date field.
   * @param operatorvalue
   *          a String array containing the filtering criteria.
   * @param typedParameters
   *          a list of Strings that will be populated with the parameters of the query.
   * @return a String with the HQL where clause to filter the field by the given value.
   */
  private String getWhereClause(String operator, String value, SelectorField field,
      SimpleDateFormat xmlDateFormat, String[] operatorvalue, List<Object> typedParameters) {
    String whereClause = "";

    if (operator != null && operator.equals(AdvancedQueryBuilder.EXISTS_QUERY_KEY)) {
      String val = "";
      for (int i = 1; i < operatorvalue.length; i++) {
        val += i > 1 ? " and " : "";
        val += operatorvalue[i];
      }
      return val;
    }

    DomainType domainType = ModelProvider.getInstance()
        .getReference(field.getReference().getId())
        .getDomainType();
    if (domainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)
        || domainType.getClass().equals(LongDomainType.class)) {
      whereClause = field.getClauseLeftPart() + AdvancedQueryBuilder.getHqlOperator(operator)
          + getTypedParameterAlias(typedParameters, new BigDecimal(value));
    } else if (domainType.getClass().equals(DateDomainType.class)) {
      try {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(xmlDateFormat.parse(value));
        whereClause = " (day(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.DATE));
        whereClause += "\n and month(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.MONTH) + 1);
        whereClause += "\n and year(" + field.getClauseLeftPart() + ") = "
            + getTypedParameterAlias(typedParameters, cal.get(Calendar.YEAR)) + ") ";
      } catch (Exception e) {
        // ignore these errors, just don't filter then
        // add a dummy whereclause to make the query format correct
        whereClause = "1 = 1";
      }
    } else if (domainType instanceof BooleanDomainType) {
      whereClause = field.getClauseLeftPart() + " = "
          + getTypedParameterAlias(typedParameters, Boolean.valueOf(value));
    } else if (domainType instanceof UniqueIdDomainType) {
      whereClause = field.getClauseLeftPart() + " = "
          + getTypedParameterAlias(typedParameters, value);
    } else if (domainType instanceof ForeignKeyDomainType) {
      // Assume left part definition is full object reference from HQL select
      whereClause = field.getClauseLeftPart() + ".id = "
          + getTypedParameterAlias(typedParameters, value);
    } else if (domainType instanceof StringEnumerateDomainType) {
      // For enumerations value can be in two formats:
      // 1- VAL: in this case the expression should be property='VAL'
      // 2- ["VAL1", "VAL2"] (JSONArray): the expression should be property in ('VAL1', 'VAL2')
      JSONArray values = null;
      if (value.startsWith("[")) {
        try {
          values = new JSONArray(value);
        } catch (JSONException ignore) {
          // It is not a JSONArray: assuming format 1
        }
      }

      if (values == null) {
        // format 1
        whereClause = field.getClauseLeftPart() + " = "
            + getTypedParameterAlias(typedParameters, value);
      } else {
        // format 2
        whereClause = field.getClauseLeftPart() + " IN (";
        for (int i = 0; i < values.length(); i++) {
          if (i > 0) {
            whereClause += ", ";
          }
          try {
            whereClause += getTypedParameterAlias(typedParameters, values.getString(i));
          } catch (JSONException e) {
            log.error("Error parsing values as JSONArray:" + value, e);
          }
        }
        whereClause += ")";
      }
    } else {
      if ("iStartsWith".equals(operator)) {
        whereClause = "upper(" + field.getClauseLeftPart() + ") LIKE upper("
            + getTypedParameterAlias(typedParameters, value.replaceAll(" ", "%") + "%") + ")";
      } else if ("iEquals".equals(operator)) {
        whereClause = "upper(" + field.getClauseLeftPart() + ") = upper("
            + getTypedParameterAlias(typedParameters, value) + ")";
      } else {
        whereClause = "upper(" + field.getClauseLeftPart() + ") LIKE upper("
            + getTypedParameterAlias(typedParameters, "%" + value.replaceAll(" ", "%") + "%") + ")";
      }
    }
    return whereClause;
  }

  /**
   * Generates the HQL Sort By Clause to append to the query being executed. If no sort options is
   * set on the sortBy parameter the result is ordered by the first shown grid's column.
   * 
   * @param sortBy
   *          String of grid's field names concatenated by JsonConstants.IN_PARAMETER_SEPARATOR.
   * @param sel
   *          the selector that it is being displayed.
   * @return a String with the HQL Sort By clause.
   */
  private String getSortClause(String sortBy, Selector sel) {
    StringBuffer sortByClause = new StringBuffer();
    boolean sortByDesc = false;
    if (sortBy != null && sortBy.startsWith("-")) {
      sortByDesc = true;
    }
    // If grid is manually filtered sortBy is not empty
    if (StringUtils.isNotEmpty(sortBy)) {
      if (sortBy.contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
        final String[] fieldNames = sortBy.split(JsonConstants.IN_PARAMETER_SEPARATOR);
        for (String fieldName : fieldNames) {
          if (sortByDesc) {
            fieldName = fieldName.substring(1, fieldName.length());
          }
          int fieldSortIndex = getFieldSortIndex(fieldName, sel);
          if (fieldSortIndex > 0) {
            if (sortByClause.length() > 0) {
              sortByClause.append(", ");
            }
            if (sortByDesc) {
              sortByClause.append(fieldSortIndex + " desc");
            } else {
              sortByClause.append(fieldSortIndex);
            }
          }
        }
      } else {
        String fieldName = null;
        if (sortByDesc) {
          fieldName = sortBy.substring(1, sortBy.length());
        } else {
          fieldName = sortBy;
        }
        int fieldSortIndex = getFieldSortIndex(fieldName, sel);
        if (fieldSortIndex > 0) {
          if (sortByDesc) {
            sortByClause.append(fieldSortIndex + " desc");
          } else {
            sortByClause.append(fieldSortIndex);
          }
        }
      }
    }

    // If sortByClause is empty set default sort options.
    if (sortByClause.length() == 0) {
      OBCriteria<SelectorField> selFieldsCrit = OBDao.getFilteredCriteria(SelectorField.class,
          Restrictions.eq(SelectorField.PROPERTY_OBUISELSELECTOR, sel),
          Restrictions.eq(SelectorField.PROPERTY_SHOWINGRID, true));
      selFieldsCrit.addOrderBy(SelectorField.PROPERTY_SORTNO, true);
      for (SelectorField selField : selFieldsCrit.list()) {
        int fieldSortIndex = getFieldSortIndex(selField.getDisplayColumnAlias(), sel);
        if (fieldSortIndex > 0) {
          sortByClause.append(fieldSortIndex + ", ");
        }
      }
      // Delete last 2 characters: ", "
      if (sortByClause.length() > 0) {
        sortByClause.delete(sortByClause.length() - 2, sortByClause.length() - 1);
      }
    }
    String result = "";
    if (sortByClause.length() > 0) {
      result = "\n ORDER BY " + sortByClause.toString();
    }

    return result;
  }

  /**
   * Given a Selector object and the request parameters it evaluates the Filter Expression in case
   * that it is defined and returns the result.
   * 
   * @param sel
   *          The Selector that it is being used.
   * @param parameters
   *          parameters used for this request.
   * @return a String with the evaluated JavaScript filter expression in case it is defined.
   */
  private String getDefaultFilterExpression(Selector sel, Map<String, String> parameters) {
    if ((sel.getFilterExpression() == null || sel.getFilterExpression().equals(""))) {
      // Nothing to filter
      return "";
    }

    Object result = null;
    try {
      result = ParameterUtils.getJSExpressionResult(parameters, RequestContext.get().getSession(),
          sel.getFilterExpression());
    } catch (Exception e) {
      log.error("Error evaluating filter expression: " + e.getMessage(), e);
    }
    if (result != null && !result.toString().equals("")) {
      return " and " + "(" + result.toString() + ")";
    }

    return "";
  }

  /**
   * Based on the given field name it gets the HQL query column related to it and returns its index.
   * 
   * @param fieldName
   *          Grid's field name or display alias of the related selector field it is desired to
   *          order by.
   * @param sel
   *          The Selector that it is being used.
   * @return The index of the query column related to the field. Note that 0 will be returned if
   *         there is no query column with an alias equal to the provided field name.
   */
  private int getFieldSortIndex(String fieldName, Selector sel) {
    @SuppressWarnings("deprecation")
    final String[] queryAliases = OBDal.getInstance()
        .getSession()
        .createQuery(sel.getHQL().replace(ADDITIONAL_FILTERS, "1=1"))
        .getReturnAliases();

    for (int i = 0; i < queryAliases.length; i++) {
      if (queryAliases[i] != null && queryAliases[i].equals(fieldName)) {
        return i + 1;
      }
    }
    return 0;
  }

  private HashMap<String, String[]> getCriteria(JSONArray criterias) {
    HashMap<String, String[]> criteriaValues = new HashMap<String, String[]>();
    try {

      for (int i = 0; i < criterias.length(); i++) {
        JSONObject criteria = criterias.getJSONObject(i);
        if (!criteria.has("fieldName") && criteria.has("criteria")
            && criteria.has("_constructor")) {
          // nested criteria, eval it recursively
          JSONArray cs = criteria.getJSONArray("criteria");
          HashMap<String, String[]> c = getCriteria(cs);
          for (String k : c.keySet()) {
            criteriaValues.put(k, c.get(k));
          }
          continue;
        }
        final String operator = criteria.getString("operator");
        final String fieldName = criteria.getString("fieldName");
        String[] criterion;
        if (operator.equals(AdvancedQueryBuilder.OPERATOR_EXISTS)
            && criteria.has(AdvancedQueryBuilder.EXISTS_QUERY_KEY)) {
          String value = "";
          JSONArray values = criteria.getJSONArray("value");
          for (int v = 0; v < values.length(); v++) {
            value += value.length() > 0 ? ", " : "";
            value += "'" + values.getString(v) + "'";
          }
          String qry = criteria.getString(AdvancedQueryBuilder.EXISTS_QUERY_KEY)
              .replace(AdvancedQueryBuilder.EXISTS_VALUE_HOLDER, value);

          if (criteriaValues.containsKey(fieldName)) {
            // assuming it is possible to have more than one query for exists in same field, storing
            // them as array
            String[] originalCriteria = criteriaValues.get(fieldName);
            List<String> newCriteria = new ArrayList<String>(Arrays.asList(originalCriteria));
            newCriteria.add(qry);
            criteriaValues.put(fieldName, newCriteria.toArray(new String[newCriteria.size()]));
          } else {
            criteriaValues.put(fieldName,
                new String[] { AdvancedQueryBuilder.EXISTS_QUERY_KEY, qry });
          }
        } else {
          criterion = new String[] { operator, criteria.getString("value") };
          criteriaValues.put(fieldName, criterion);
        }
      }
    } catch (JSONException e) {
      log.error("Error getting criteria for custom query selector", e);
    }
    if (criteriaValues.isEmpty()) {
      return null;
    }
    return criteriaValues;

  }

  private HashMap<String, String[]> getCriteria(Map<String, String> parameters) {
    if (!"AdvancedCriteria".equals(parameters.get("_constructor"))) {
      return null;
    }
    try {
      JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
      return getCriteria(criterias);
    } catch (JSONException e) {
      return null;
    }
  }

  private String getTypedParameterAlias(List<Object> typedParameters, Object value) {
    String alias = ":" + ALIAS_PREFIX + (typedParameters.size());
    typedParameters.add(value);
    return alias;
  }
}
