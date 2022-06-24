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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.querylist;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.LongDomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.reference.EnumUIDefinition;
import org.openbravo.client.kernel.reference.ForeignKeyUIDefinition;
import org.openbravo.client.kernel.reference.NumberUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.client.kernel.reference.YesNoUIDefinition;
import org.openbravo.client.myob.WidgetClass;
import org.openbravo.client.myob.WidgetInstance;
import org.openbravo.client.myob.WidgetReference;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.WindowAccess;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.datasource.DataSourceProperty;
import org.openbravo.service.datasource.ReadOnlyDataSourceService;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

/**
 * Reads the tabs which the user is allowed to see.
 * 
 * @author gorkaion
 */
public class QueryListDataSource extends ReadOnlyDataSourceService implements PortalAccessible {
  private static final String OPTIONAL_FILTERS = "@optional_filters@";
  private static final Logger log = LogManager.getLogger();
  private static final String OPERATOR = "$OPERATOR";

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameters) {
    // Check security: continue only if the widget instance is visible for current user/role
    OBContext.setAdminMode(true);
    try {
      WidgetClass widgetClass = OBDal.getInstance()
          .get(WidgetClass.class, parameters.get("widgetId"));
      WidgetInstance wi = OBDal.getInstance()
          .get(WidgetInstance.class, parameters.get("widgetInstanceId"));

      boolean accessibleWidgetInForm = false;
      if (wi == null) {
        accessibleWidgetInForm = isAccessibleWidgetInForm(widgetClass);
      }

      User ctxUser = OBContext.getOBContext().getUser();
      Role ctxRole = OBContext.getOBContext().getRole();

      if (!accessibleWidgetInForm
          && (wi == null || !wi.getWidgetClass().getId().equals(widgetClass.getId()))) {
        // weird stuff: widget class doesn't match widget instance's class, most probably URL is
        // not generated by UI, but user is typing it
        log.error("User " + ctxUser + " with role " + ctxRole + " is trying to access widget '"
            + widgetClass.getWidgetTitle() + "' but widget instance doesn't match with class");
        throw new OBSecurityException(OBMessageUtils.getI18NMessage("OBCQL_NoAccessToWidget",
            new String[] { widgetClass.getWidgetTitle() }));
      }

      if (!accessibleWidgetInForm && !isAccessibleWidget(wi, ctxUser.getId(), ctxRole.getId())) {
        log.error("User " + ctxUser + " with role " + ctxRole + " is trying to access widget '"
            + widgetClass.getWidgetTitle() + "' which is not granted");
        throw new OBSecurityException(OBMessageUtils.getI18NMessage("OBCQL_NoAccessToWidget",
            new String[] { widgetClass.getWidgetTitle() }));
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private boolean isAccessibleWidget(WidgetInstance wi, String userId, String roleId) {
    boolean visibleAtUser = wi.getVisibleAtUser() != null
        ? wi.getVisibleAtUser().getId().equals(userId)
        : true;
    boolean visibleAtRole = wi.getVisibleAtRole() != null
        ? wi.getVisibleAtRole().getId().equals(roleId)
        : true;
    return visibleAtUser || visibleAtRole;
  }

  /**
   * Returns the count of objects based on the passed parameters.
   * 
   * @param parameters
   *          the parameters passed in from the request
   * @return the total number of objects
   */
  @Override
  protected int getCount(Map<String, String> parameters) {
    return getData(parameters, 0, -1).size();
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {
    // creation of formats is done here because they are not thread safe
    final SimpleDateFormat xmlDateFormat = JsonUtils.createDateFormat();
    final SimpleDateFormat xmlDateTimeFormat = JsonUtils.createDateTimeFormat();

    OBContext.setAdminMode();
    try {
      WidgetClass widgetClass = OBDal.getInstance()
          .get(WidgetClass.class, parameters.get("widgetId"));

      boolean isExport = "true".equals(parameters.get("exportToFile"));
      boolean showAll = "true".equals(parameters.get("showAll"));
      String viewMode = parameters.get("viewMode");
      List<OBCQL_QueryColumn> columns = QueryListUtils
          .getColumns(widgetClass.getOBCQLWidgetQueryList().get(0));

      // handle complex criteria
      try {
        JSONArray criterias = (JSONArray) JsonUtils.buildCriteria(parameters).get("criteria");
        for (int i = 0; i < criterias.length(); i++) {
          final JSONObject criteria = criterias.getJSONObject(i);
          parameters.put(criteria.getString("fieldName"), criteria.getString("value"));
          parameters.put(criteria.getString("fieldName") + OPERATOR,
              criteria.getString("operator"));
        }
      } catch (JSONException e) {
        // Ignore exception.
      }

      OBCQL_WidgetQuery widgetQueryInstance = widgetClass.getOBCQLWidgetQueryList().get(0);
      String HQL = widgetQueryInstance.getHQL();
      // Parse the HQL in case that optional filters are required
      HQL = parseOptionalFilters(HQL, viewMode, parameters, columns, xmlDateFormat);
      boolean fetchingSummaryFields = parameters.containsKey(JsonConstants.SUMMARY_PARAMETER);
      if (fetchingSummaryFields) {
        // if the request comes from the summary row, update the select clause so that it obtains
        // the values for the summary fields
        HQL = updateHQLWithSummaryFields(HQL, parameters.get(JsonConstants.SUMMARY_PARAMETER),
            widgetQueryInstance);
      }

      if (parameters.containsKey(JsonConstants.SORTBY_PARAMETER)) {
        HQL = updateSortByFields(HQL, parameters.get(JsonConstants.SORTBY_PARAMETER));
      }

      Query<Tuple> widgetQuery = null;
      try {
        widgetQuery = OBDal.getInstance().getSession().createQuery(HQL, Tuple.class);
      } catch (Exception e) {
        if (fetchingSummaryFields) {
          log.error("Exception while fetching the summary columns of the widget "
              + widgetClass.getWidgetTitle()
              + ". It is not supported using as summaries columns that are defined using a subquery, or that are defined using a summary function. \n Query = "
              + HQL);
        } else {
          log.error("Exception while executing the HQL query to fetch the data of the widget "
              + widgetClass.getWidgetTitle() + ". \n Query = " + HQL);
        }
        final List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        return result;
      }

      if (!isExport && "widget".equals(viewMode) && !showAll) {
        int rowsNumber = Integer.parseInt(
            (parameters.get("rowsNumber") != null && !parameters.get("rowsNumber").equals("null"))
                ? parameters.get("rowsNumber")
                : "10");
        widgetQuery.setMaxResults(rowsNumber);
      } else if (!isExport) {
        if (startRow > 0) {
          widgetQuery.setFirstResult(startRow);
        }
        if (endRow > startRow) {
          widgetQuery.setMaxResults(endRow - startRow + 1);
        }
      }

      Set<String> params = widgetQuery.getParameterMetadata().getNamedParameterNames();
      if (!params.isEmpty()) {
        HashMap<String, Object> parameterValues = getParameterValues(parameters, widgetClass);

        for (String namedParam : params) {
          boolean isParamSet = false;
          if (parameterValues.containsKey(namedParam)) {
            Object value = parameterValues.get(namedParam);
            if (value instanceof Collection<?>) {
              widgetQuery.setParameterList(namedParam, (Collection<?>) value);
            } else if (value instanceof Object[]) {
              widgetQuery.setParameterList(namedParam, (Object[]) value);
            } else if (value instanceof String
                && isDate(namedParam, widgetClass.getOBUIAPPParameterEMObkmoWidgetClassIDList())) {
              widgetQuery.setParameter(namedParam, convertToDate((String) value));
            } else {
              widgetQuery.setParameter(namedParam, value);
            }
            isParamSet = true;
          }
          if (!isParamSet) {
            // TODO: throw an exception
          }
        }
      }

      final List<Map<String, Object>> result = new ArrayList<>();

      if (fetchingSummaryFields) {
        // process the response for the summary row
        Map<String, Object> summaryData = new LinkedHashMap<>();
        try {
          JSONObject summaryFieldsObject = new JSONObject(
              parameters.get(JsonConstants.SUMMARY_PARAMETER));
          Tuple uniqueResult = widgetQuery.uniqueResult();
          Iterator<?> summaryFieldNameIterator = summaryFieldsObject.keys();
          int i = 0;
          while (summaryFieldNameIterator.hasNext()) {
            String summaryFieldName = (String) summaryFieldNameIterator.next();
            summaryData.put(summaryFieldName, uniqueResult.get(i++));
          }
          summaryData.put("isGridSummary", true);
        } catch (Exception e) {
          log.error("Exception fetching summary columns of the widget "
              + widgetClass.getWidgetTitle() + ". \n Query = " + HQL, e);
        }
        result.add(summaryData);

      } else {
        // process the response for the grid
        for (Tuple tuple : widgetQuery.list()) {
          final Map<String, Object> data = new LinkedHashMap<>();

          for (OBCQL_QueryColumn column : columns) {
            UIDefinition uiDefinition = UIDefinitionController.getInstance()
                .getUIDefinition(column.getReference());
            DomainType domainType = uiDefinition.getDomainType();
            for (TupleElement<?> tupleElement : tuple.getElements()) {
              String alias = tupleElement.getAlias();
              if (alias == null) {
                continue;
              } else if (alias.equals(column.getDisplayExpression())
                  || (!isExport && alias.equals(column.getLinkExpression()))) {
                Object value = tuple.get(tupleElement.getAlias());
                if (domainType instanceof DateDomainType && value != null) {
                  value = xmlDateFormat.format(value);
                } else if (value instanceof Timestamp && value != null) {
                  value = xmlDateTimeFormat.format(value);
                  value = JsonUtils.convertToCorrectXSDFormat((String) value);
                }

                if (domainType instanceof BooleanDomainType && value instanceof String) {
                  value = ((PrimitiveDomainType) domainType).createFromString((String) value);
                }

                if (!isExport) {
                  data.put(tupleElement.getAlias(), value);
                } else {
                  data.put(QueryListUtils.getColumnLabel(column), value);
                }
              }
            }
          }
          result.add(data);
        }
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Updates the order by clause of the HQL query so that it obtains the values for the summary
   * fields. If the HQL query already contains order by fields, the new fields are appended for the
   * existing fields.
   * 
   * @param hql
   *          original HQL query
   * @param sortBy
   *          parameter that contains sortBy field values
   * @return an updated HQL query that will set the order by fields
   */
  private String updateSortByFields(String hql, String sortBy) {
    String[] fieldList = null;
    String sortByClause = "";
    String hqlString = hql;
    if (sortBy.contains(",")) {
      fieldList = sortBy.split(",");
    }
    if (hqlString.toLowerCase().contains("order by")) {
      if (fieldList == null) {
        sortByClause = sortBy.startsWith("-") ? sortBy.substring(1, sortBy.length()) + " desc "
            : sortBy;
      } else {
        // sort by multiple columns
        for (String field : fieldList) {
          sortByClause = field.startsWith("-")
              ? sortByClause.concat(field.substring(1, field.length())) + " desc "
              : sortByClause.concat(field);
        }
      }
      int sortByIndex = hqlString.toLowerCase().indexOf("order by");
      hqlString = hqlString.substring(0, sortByIndex + "order by".length() + 1) + sortByClause + ","
          + hqlString.substring(sortByIndex + "order by".length() + 1);
    } else {
      hqlString = hqlString.concat(" order by " + sortByClause);
    }
    return hqlString;
  }

  /**
   * Updates the select clause of the HQL query so that it obtains the values for the summary fields
   * 
   * @param hQL
   *          original HQL query
   * @param summaryParametersString
   *          parameter that contains pairs of summaryField - summaryFunction values
   * @param widgetQuery
   *          the instance of the widget, used to obtain the whereCLauseLeftPart of the summary
   *          parameter
   * @return an updated HQL query that will obtain the values for the summary fields
   */
  private String updateHQLWithSummaryFields(String hQL, String summaryParametersString,
      OBCQL_WidgetQuery widgetQuery) {
    // get rid of the original select clause, a new one is going to be built
    String updatedHQL = removeSelectClause(hQL);
    // the order and group by clauses are not needed when obtaining the values for the summary
    // fields
    updatedHQL = removeOrderByClause(updatedHQL);
    updatedHQL = removeGroupByClause(updatedHQL);
    try {
      JSONObject summaryFieldsObject = new JSONObject(summaryParametersString);
      Iterator<?> summaryFieldNameIterator = summaryFieldsObject.keys();
      StringBuilder selectClause = new StringBuilder("select ");
      boolean first = true;
      while (summaryFieldNameIterator.hasNext()) {
        String summaryFieldName = (String) summaryFieldNameIterator.next();
        String whereClauseLeftPart = getWhereClauseLeftPart(widgetQuery, summaryFieldName);
        // if the column has whereClauseLeftPart, use it to support using columns with aliases
        // see issue https://issues.openbravo.com/view.php?id=29174
        String summaryColumnInnerClause = (whereClauseLeftPart.isEmpty() ? summaryFieldName
            : whereClauseLeftPart);
        String summaryFunction = summaryFieldsObject.getString(summaryFieldName);
        if (!first) {
          selectClause.append(", ");
        } else {
          first = false;
        }
        // only three summary functions are available for the columns of Query/List widgets: count,
        // sum and avg
        if ("count".equals(summaryFunction)) {
          selectClause.append("count(*)");
        } else if ("sum".equals(summaryFunction)) {
          selectClause.append("sum(" + summaryColumnInnerClause + ")");
        } else if ("avg".equals(summaryFunction)) {
          selectClause.append("sum(" + summaryColumnInnerClause + ")/count(*)");
        }
      }
      updatedHQL = selectClause.toString() + " " + updatedHQL;
    } catch (JSONException e) {
      log.error("Error obtaining the values of the summary fields", e);
    }
    return updatedHQL;
  }

  private String getWhereClauseLeftPart(OBCQL_WidgetQuery widgetQuery, String summaryFieldName) {
    OBCriteria<OBCQL_QueryColumn> columnCriteria = OBDal.getInstance()
        .createCriteria(OBCQL_QueryColumn.class);
    columnCriteria.add(Restrictions.eq(OBCQL_QueryColumn.PROPERTY_WIDGETQUERY, widgetQuery));
    columnCriteria
        .add(Restrictions.eq(OBCQL_QueryColumn.PROPERTY_DISPLAYEXPRESSION, summaryFieldName));
    OBCQL_QueryColumn queryColumn = (OBCQL_QueryColumn) columnCriteria.uniqueResult();
    return (queryColumn != null && queryColumn.getWhereClauseLeftPart() != null
        ? queryColumn.getWhereClauseLeftPart()
        : "");
  }

  /**
   * Removes the select clause of a hql query
   * 
   * @param hql
   *          the original hql query
   * @return the original hql query without its select clause
   */
  private String removeSelectClause(String hql) {
    String hqlWithoutSelectClause = hql;
    String hqlLowerCase = hql.toLowerCase();
    if (hqlLowerCase.indexOf(" from ") != -1) {
      hqlWithoutSelectClause = hqlWithoutSelectClause.substring(hqlLowerCase.indexOf(" from "));
    } else if (hqlWithoutSelectClause.toLowerCase().indexOf("\nfrom ") != -1) {
      hqlWithoutSelectClause = hqlWithoutSelectClause.substring(hqlLowerCase.indexOf("\nfrom "));
    }
    return hqlWithoutSelectClause;
  }

  private String removeOrderByClause(String hql) {
    return removeClause(hql, "order by");
  }

  private String removeGroupByClause(String hql) {
    return removeClause(hql, "group by");
  }

  private String removeClause(String hql, String clause) {
    String hqlWithoutOrderByClause = hql;
    String hqlLowerCase = hql.toLowerCase();
    String clauseBetweenSpaces = " " + clause + " ";
    String clauseNewLine = "\n" + clause + " ";
    if (hqlLowerCase.indexOf(clauseBetweenSpaces) != -1) {
      hqlWithoutOrderByClause = hqlWithoutOrderByClause.substring(0,
          hqlLowerCase.indexOf(clauseBetweenSpaces));
    } else if (hqlLowerCase.indexOf(clauseNewLine) != -1) {
      hqlWithoutOrderByClause = hqlWithoutOrderByClause.substring(0,
          hqlLowerCase.indexOf(clauseNewLine));
    }
    return hqlWithoutOrderByClause;
  }

  private boolean isAccessibleWidgetInForm(WidgetClass widgetClass) {
    OBCriteria<WidgetReference> widgetInFormCriteria = OBDal.getInstance()
        .createCriteria(WidgetReference.class);
    widgetInFormCriteria.add(Restrictions.eq(WidgetReference.PROPERTY_WIDGETCLASS, widgetClass));
    List<Window> windowList = new ArrayList<>();
    List<WidgetReference> widgetInFormList = widgetInFormCriteria.list();
    for (WidgetReference widgetInForm : widgetInFormList) {
      List<Column> columnList = widgetInForm.getReference().getADColumnReferenceSearchKeyList();
      for (Column column : columnList) {
        List<Field> fieldList = column.getADFieldList();
        for (Field field : fieldList) {
          windowList.add(field.getTab().getWindow());
        }
      }
    }

    if (windowList.isEmpty()) {
      // The widget is not embedded in any window
      return false;
    } else {
      OBCriteria<WindowAccess> accessibleWindowCriteria = OBDal.getInstance()
          .createCriteria(WindowAccess.class);
      accessibleWindowCriteria
          .add(Restrictions.eq(WindowAccess.PROPERTY_ROLE, OBContext.getOBContext().getRole()));
      accessibleWindowCriteria.add(Restrictions.in(WindowAccess.PROPERTY_WINDOW, windowList));
      int count = accessibleWindowCriteria.count();
      // If the widget is embedded in at least one window accessible by the user, return true
      return (count > 0);
    }
  }

  // Converts and object from String to Date
  private Date convertToDate(String value) {
    DateDomainType domainType = new DateDomainType();
    return (Date) domainType.createFromString(value);
  }

  // Check if the reference of a parameter is a Date
  private boolean isDate(String paramName, List<Parameter> parameterList) {
    Parameter parameterToCheck = null;
    for (Parameter p : parameterList) {
      if (p.getDBColumnName().equals(paramName)) {
        parameterToCheck = p;
        break;
      }
    }
    if (parameterToCheck == null) {
      return false;
    } else {
      DomainType domainType = ModelProvider.getInstance()
          .getReference(parameterToCheck.getReference().getId())
          .getDomainType();
      return domainType.getClass().equals(DateDomainType.class);
    }
  }

  /**
   * Returns a HashMap with the values of the parameters included on the given widget instance.
   * 
   * @param parameters
   *          the parameters passed in from the request
   * @param widgetClass
   *          the widget class to which the parameters belong to
   * @return a HashMap<String, Object> with the value of each parameter mapped by the DBColumnName
   *         of the parameter.
   */
  private HashMap<String, Object> getParameterValues(Map<String, String> parameters,
      WidgetClass widgetClass) {
    HashMap<String, Object> parameterValues = new HashMap<>();

    // get serializedValues from request (if present)
    String serializedParams = parameters.get("serializedParameters");
    if (serializedParams != null) {
      try {
        JSONObject json = new JSONObject(serializedParams);
        for (Parameter parameter : widgetClass.getOBUIAPPParameterEMObkmoWidgetClassIDList()) {
          if (parameter.isFixed()) {
            parameterValues.put(parameter.getDBColumnName(),
                ParameterUtils.getParameterFixedValue(parameters, parameter));
          } else {
            if (json.has(parameter.getDBColumnName())) {
              parameterValues.put(parameter.getDBColumnName(),
                  json.get(parameter.getDBColumnName()));
            } else {
              // TODO: not fixed & value missing -> error (prepared to be handled in caller, but not
              // yet implemented)
            }
          }
        }
      } catch (JSONException e) {
        log.error("Error processing client parameters", e);
      }
    } else {
      // data send without serializedParams (should not happen)
      throw new OBException("Missing serializedParameters value in request");
    }

    return parameterValues;
  }

  private String parseOptionalFilters(String _HQL, String viewMode, Map<String, String> parameters,
      List<OBCQL_QueryColumn> columns, SimpleDateFormat xmlDateFormat) {
    StringBuilder optionalFilter = new StringBuilder(" 1=1 ");
    String HQL = _HQL;

    // Parse for columns filtered by grid's filter row on maximized view. If we are not on maximized
    // view return the HQL without parsing.
    if ("maximized".equals(viewMode)) {
      for (OBCQL_QueryColumn column : columns) {
        if (column.isCanBeFiltered()) {
          String value = parameters.get(column.getDisplayExpression());
          String operator = parameters.get(column.getDisplayExpression() + OPERATOR);
          if (column.getReference().getName().equals("YesNo") && value != null) {
            if (value.equals("true")) {
              value = "Y";
            } else {
              value = "N";
            }
          }
          String whereClause = " 1=1 ";
          if (value != null) {
            whereClause = getWhereClause(value, column, xmlDateFormat, operator);
          }

          if (HQL.contains("@" + column.getDisplayExpression() + "@")) {
            HQL = HQL.replace("@" + column.getDisplayExpression() + "@", whereClause);
          } else {
            optionalFilter.append(" and " + whereClause);
          }
        }
      }
    }
    HQL = HQL.replace(OPTIONAL_FILTERS, optionalFilter.toString());
    return HQL;
  }

  private String getWhereClause(String value, OBCQL_QueryColumn column,
      SimpleDateFormat xmlDateFormat, String operator) {
    String whereClause = "";
    DomainType domainType = ModelProvider.getInstance()
        .getReference(column.getReference().getId())
        .getDomainType();
    if (domainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)
        || domainType.getClass().equals(LongDomainType.class)) {
      if (StringUtils.isNotEmpty(value)) {
        whereClause = column.getWhereClauseLeftPart() + " "
            + AdvancedQueryBuilder.getHqlOperator(operator) + " " + value;
      } else {
        whereClause = " 1=1 ";
      }
    } else if (domainType.getClass().equals(DateDomainType.class)) {
      try {
        final Calendar cal = Calendar.getInstance();
        cal.setTime(xmlDateFormat.parse(value));
        whereClause = " (day(" + column.getWhereClauseLeftPart() + ") = " + cal.get(Calendar.DATE);
        whereClause += "\n and month(" + column.getWhereClauseLeftPart() + ") = "
            + (cal.get(Calendar.MONTH) + 1);
        whereClause += "\n and year(" + column.getWhereClauseLeftPart() + ") = "
            + cal.get(Calendar.YEAR) + ") ";
      } catch (Exception e) {
        // ignore these errors, just don't filter then
        // add a dummy whereclause to make the query format correct
        whereClause = " 1=1 ";
      }
    } else {
      whereClause = "upper(" + column.getWhereClauseLeftPart() + ")";
      whereClause += " LIKE ";
      whereClause += "'%" + value.toUpperCase().replaceAll(" ", "%") + "%'";
    }
    return whereClause;
  }

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    // note datasource properties are not cached as the component is
    // re-used within one request thread
    final List<DataSourceProperty> dsProperties = new ArrayList<>();
    OBContext.setAdminMode();
    try {
      WidgetClass widgetClass = (WidgetClass) parameters
          .get(QueryListWidgetProvider.WIDGETCLASS_PARAMETER);

      if (!widgetClass.getOBCQLWidgetQueryList().isEmpty()) {
        for (OBCQL_QueryColumn column : QueryListUtils
            .getColumns(widgetClass.getOBCQLWidgetQueryList().get(0))) {
          Reference reference = column.getReference();
          if (column.getReferenceSearchKey() != null) {
            reference = column.getReferenceSearchKey();
          }

          final DataSourceProperty dsProperty = new DataSourceProperty();
          dsProperty.setName(column.getDisplayExpression());
          dsProperty.setId(false);
          dsProperty.setMandatory(false);
          dsProperty.setAuditInfo(false);
          dsProperty.setUpdatable(false);
          final UIDefinition uiDefinition = UIDefinitionController.getInstance()
              .getUIDefinition(reference);
          dsProperty.setBoolean(uiDefinition instanceof YesNoUIDefinition);
          dsProperty.setPrimitive(!(uiDefinition instanceof ForeignKeyUIDefinition));
          dsProperty.setUIDefinition(uiDefinition);
          if (dsProperty.isPrimitive()) {
            dsProperty.setPrimitiveObjectType(
                ((PrimitiveDomainType) uiDefinition.getDomainType()).getPrimitiveType());
            dsProperty.setNumericType(uiDefinition instanceof NumberUIDefinition);

            if (uiDefinition instanceof EnumUIDefinition) {
              if (column.getReferenceSearchKey() == null) {
                log.warn("In widget " + column.getWidgetQuery().getWidgetClass().getWidgetTitle()
                    + " column " + column.getDisplayExpression()
                    + " is of enum type but does not define sub reference.");
              } else {
                Set<String> allowedValues = DataSourceProperty
                    .getAllowedValues(column.getReferenceSearchKey());
                dsProperty.setAllowedValues(allowedValues);
                dsProperty.setValueMap(DataSourceProperty.createValueMap(allowedValues,
                    column.getReferenceSearchKey().getId()));
              }
            }
          }
          dsProperties.add(dsProperty);
        }
      }
      return dsProperties;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @Override
  protected void sort(String sortBy, List<Map<String, Object>> data) {
    Collections.sort(data, new DataComparator(sortBy));
  }

  // can only be used if the comparedBy is a string
  private static class DataComparator implements Comparator<Map<String, Object>> {
    private ArrayList<String> compareByArray;

    public DataComparator(String compareBy) {
      this.compareByArray = new ArrayList<>();
      if (compareBy.contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
        final String[] separatedValues = compareBy.split(JsonConstants.IN_PARAMETER_SEPARATOR);
        for (String separatedValue : separatedValues) {
          this.compareByArray.add(separatedValue);
        }
      } else {
        this.compareByArray.add(compareBy);
      }
    }

    @Override
    public int compare(Map<String, Object> o1, Map<String, Object> o2) {
      for (String compareBy : compareByArray) {
        int ascending = 1;
        if (compareBy.startsWith("-")) {
          ascending = -1;
          compareBy = compareBy.substring(1);
        }
        final Object v1 = o1.get(compareBy);
        final Object v2 = o2.get(compareBy);
        if (v1 == null) {
          return -1 * ascending;
        } else if (v2 == null) {
          return 1 * ascending;
        }
        int returnValue = 0;
        if (v1 instanceof Date && v2 instanceof Date) {
          returnValue = ((Date) v1).compareTo((Date) v2) * ascending;
        } else if (v1 instanceof Timestamp && v2 instanceof Timestamp) {
          returnValue = ((Timestamp) v1).compareTo((Timestamp) v2) * ascending;
        } else if (v1 instanceof Long && v2 instanceof Long) {
          returnValue = ((Long) v1).compareTo((Long) v2) * ascending;
        } else if (v1 instanceof BigDecimal && v2 instanceof BigDecimal) {
          returnValue = ((BigDecimal) v1).compareTo((BigDecimal) v2) * ascending;
        } else if (v1 instanceof String && v2 instanceof String) {
          returnValue = ((String) v1).compareTo((String) v2) * ascending;
        } else {
          log.warn("Comparing on property " + compareBy + " for objects " + v1 + "/" + v2 + ". "
              + "But value is are of different classes or an instance of a not supported class. "
              + "Returning default compare value.");
          returnValue = 0;
        }
        if (returnValue != 0) {
          return returnValue;
        }
      }
      return 0;
    }
  }

}
