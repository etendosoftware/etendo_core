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
 * All portions are Copyright (C) 2009-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.json;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.SearchDomainType;
import org.openbravo.base.model.domaintype.TableDomainType;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.IdentifierProvider;
import org.openbravo.base.util.Check;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.domain.ReferencedTable;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Translates an advanced criteria/filter object into a HQL query. Also takes into account session
 * and other parameters.
 * 
 * @author mtaal
 */
public class AdvancedQueryBuilder {

  private static final String CRITERIA_KEY = "criteria";
  private static final String VALUE_KEY = "value";
  private static final String FIELD_NAME_KEY = "fieldName";
  public static final String EXISTS_QUERY_KEY = "existsQuery";
  private static final String OPERATOR_KEY = "operator";
  private static final String ALIAS_PREFIX = "alias_";
  private static final String JOIN_ALIAS_PREFIX = "join_";
  private static final char ESCAPE_CHAR = '|';
  public static final String EXISTS_VALUE_HOLDER = "$value";

  private static final String OPERATOR_AND = "and";
  static final String OPERATOR_OR = "or";

  private static final String OPERATOR_EQUALS = "equals";
  private static final String OPERATOR_NOTEQUAL = "notEqual";
  private static final String OPERATOR_IEQUALS = "iEquals";
  private static final String OPERATOR_INOTEQUAL = "iNotEqual";
  private static final String OPERATOR_GREATERTHAN = "greaterThan";
  private static final String OPERATOR_LESSTHAN = "lessThan";
  private static final String OPERATOR_GREATEROREQUAL = "greaterOrEqual";
  private static final String OPERATOR_LESSOREQUAL = "lessOrEqual";
  private static final String OPERATOR_IGREATERTHAN = "iGreaterThan";
  private static final String OPERATOR_ILESSTHAN = "iLessThan";
  private static final String OPERATOR_IGREATEROREQUAL = "iGreaterOrEqual";
  private static final String OPERATOR_ILESSOREQUAL = "iLessOrEqual";
  private static final String OPERATOR_CONTAINS = "contains";
  private static final String OPERATOR_STARTSWITH = "startsWith";
  private static final String OPERATOR_ENDSWITH = "endsWith";
  private static final String OPERATOR_ICONTAINS = "iContains";
  private static final String OPERATOR_ISTARTSWITH = "iStartsWith";
  private static final String OPERATOR_IENDSWITH = "iEndsWith";
  private static final String OPERATOR_NOTCONTAINS = "notContains";
  private static final String OPERATOR_NOTSTARTSWITH = "notStartsWith";
  private static final String OPERATOR_NOTENDSWITH = "notEndsWith";
  private static final String OPERATOR_INOTCONTAINS = "iNotContains";
  private static final String OPERATOR_INOTSTARTSWITH = "iNotStartsWith";
  private static final String OPERATOR_INOTENDSWITH = "iNotEndsWith";
  // private static final String OPERATOR_REGEXP = "regexp";
  // private static final String OPERATOR_IREGEXP = "iregexp";
  private static final String OPERATOR_ISNULL = "isNull";
  private static final String OPERATOR_NOTNULL = "notNull";
  private static final String OPERATOR_INSET = "inSet";
  private static final String OPERATOR_NOTINSET = "notInSet";
  private static final String OPERATOR_EQUALSFIELD = "equalsField";
  private static final String OPERATOR_NOTEQUALFIELD = "notEqualField";
  private static final String OPERATOR_GREATERTHANFIElD = "greaterThanField";
  private static final String OPERATOR_LESSTHANFIELD = "lessThanField";
  private static final String OPERATOR_GREATEROREQUALFIELD = "greaterOrEqualField";
  private static final String OPERATOR_LESSOREQUALFIElD = "lessOrEqualField";
  private static final String OPERATOR_CONTAINSFIELD = "containsField";
  private static final String OPERATOR_STARTSWITHFIELD = "startsWithField";
  private static final String OPERATOR_ENDSWITHFIELD = "endsWithField";
  private static final String OPERATOR_NOT = "not";
  private static final String OPERATOR_BETWEEN = "between";
  private static final String OPERATOR_BETWEENINCLUSIVE = "betweenInclusive";
  private static final String OPERATOR_IBETWEEN = "iBetween";
  private static final String OPERATOR_IBETWEENINCLUSIVE = "iBetweenInclusive";
  public static final String OPERATOR_EXISTS = "exists";
  private static final String ACCESIBLE_ORG_TREE_ARG = "#AccessibleOrgTree";

  private JSONObject criteria = null;

  private Map<String, String> filterParameters = new HashMap<String, String>();
  private List<Object> typedParameters = new ArrayList<Object>();
  private Entity entity;
  private String mainAlias = null;
  private int aliasIndex = 0;
  private List<JoinDefinition> joinDefinitions = new ArrayList<JoinDefinition>();
  private String orderBy;

  private List<String> selectClauseParts = new ArrayList<String>();

  private String orderByClause = null;
  private String whereClause = null;
  private String joinClause = null;

  // keeps track if during parsing the criteria one or more or's are encountered.
  private int orNesting = 0;

  private int UTCServerMinutesTimeZoneDiff = 0;
  private int clientUTCMinutesTimeZoneDiff = 0;

  private SimpleDateFormat simpleDateFormat = JsonUtils.createDateFormat();
  private SimpleDateFormat simpleDateTimeFormat = JsonUtils.createJSTimeFormat();

  // join associated entities
  private boolean joinAssociatedEntities = false;

  private List<String> additionalProperties = new ArrayList<String>();
  private Entity subEntity;
  private Property distinctProperty;
  private String distinctPropertyPath;
  private DataEntityQueryService subDataEntityQueryService;

  // map that indicates, for a property, if it should be joined using an inner join or a left join.
  // An inner join will be used if one of the following cases is fulfilled:
  // 1- the join definition of the property is used only in joins used for filtering the grid.
  // 2- the join definition is included for sorting, and the property used to make the join is
  // mandatory, i.e., the inner join can be used safely.
  private Map<String, Boolean> useInnerJoinMap = new HashMap<String, Boolean>();

  private int aliasOffset = 0;

  // Sometimes (i.e. in HQLDataSourceServe) the AdvancedQueryBuilder is used only to retrieve the
  // WHERE clause, and the FROM clause is discarted
  // In this cases, prevent adding new join aliases in the WHERE clause, as they will not be defined
  // in the FROM clause
  private boolean creatingJoinsInWhereClauseIsPrevented = false;

  public static enum TextMatching {
    startsWith, exact, substring
  }

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(String entityName) {
    this.entity = ModelProvider.getInstance().getEntity(entityName);
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  /**
   * Translates the filter criteria ({@link #addFilterParameter(String, String)}) to a valid HQL
   * where clause (without the 'where' keyword). After calling this method the method
   * {@link #getNamedParameters()} can be called. Note that currently only filtering on string and
   * boolean properties is supported. Also filtering on the identifier of a referenced business
   * object is supported.
   * 
   * @return a valid where clause or an empty string if not set.
   */
  public String getWhereClause() {

    if (whereClause != null) {
      return whereClause;
    }

    Check.isNotNull(entity, "Entity must be set");

    // parse the criteria themselves
    if (criteria.has(OPERATOR_KEY)) {
      try {
        whereClause = parseCriteria(criteria);
      } catch (JSONException e) {
        throw new OBException(e);
      }
    }
    if (whereClause == null) {
      whereClause = "";
    }
    whereClause = addWhereOrgParameters(whereClause);
    whereClause = substituteParameters(whereClause);

    if (whereClause.trim().length() > 0) {
      whereClause = " where " + whereClause;
    }

    whereClause += " ";

    if (subEntity != null) {
      // if there's subentity, process it as a subquery with "exists"

      String orgPath;
      if (subEntity.getMappingClass().isAssignableFrom(Organization.class)) {
        // special case subentity is Organization, so no extra path required to get it
        orgPath = "e";
      } else {
        orgPath = "e.organization";
      }

      String subEntityClientOrg = " and " + orgPath + ".id "
          + createInClause(OBContext.getOBContext().getReadableOrganizations());

      String clientPath;
      if (subEntity.getMappingClass().isAssignableFrom(Client.class)) {
        // special case subentity is Client, so no extra path required to get it
        clientPath = "e";
      } else {
        clientPath = "e.client";
      }

      subEntityClientOrg += " and " + clientPath + ".id "
          + createInClause(OBContext.getOBContext().getReadableClients());

      AdvancedQueryBuilder subEntityQueryBuilder = subDataEntityQueryService.getQueryBuilder();
      subEntityQueryBuilder.aliasOffset = typedParameters.size();

      String subentityWhere = subEntityQueryBuilder.getWhereClause();
      if (StringUtils.isEmpty(subentityWhere.trim())) {
        subentityWhere += " where ";
      } else {
        subentityWhere += " and ";
      }

      String distinctPropName = distinctProperty.getName();
      if (distinctProperty.isComputedColumn()) {
        distinctPropName = Entity.COMPUTED_COLUMNS_PROXY_PROPERTY + DalUtil.DOT + distinctPropName;
      }
      whereClause += StringUtils.isEmpty(whereClause.trim()) ? "where" : "and";

      // if the property allows null values, use a left join instead an inner join
      if (!distinctPropertyPath.contains(DalUtil.FIELDSEPARATOR)
          && subEntity.getProperty(distinctPropertyPath).allowNullValues()) {
        String joinType = null;
        // if all the identifier properties of the subentity are mandatory, an inner join can be
        // used
        if (KernelUtils.hasNullableIdentifierProperties(subEntity)) {
          joinType = " left join ";
        } else {
          joinType = " inner join ";
        }
        whereClause += " exists (select 1 from " + subEntity.getName() + " "
            + subEntityQueryBuilder.getJoinClause() + joinType
            + subEntityQueryBuilder.getMainAlias() + DalUtil.DOT + distinctPropertyPath + " as i "
            + subentityWhere + " i = " + mainAlias + subEntityClientOrg + ") ";
      } else {
        whereClause += " exists (select 1 from " + subEntity.getName() + " "
            + subEntityQueryBuilder.getJoinClause() + subentityWhere + "e."
            + distinctPropertyPath.replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT) + " = " + mainAlias
            + subEntityClientOrg + ") ";
      }

      typedParameters.addAll(subEntityQueryBuilder.typedParameters);
    } else if (filterParameters.containsKey(JsonConstants.SHOW_FK_DROPDOWN_UNFILTERED_PARAMETER)) {
      // If the entity is Client or Organization, we need to add the readable Client or
      // Organization filter manually, because the OBQuery will not be able to do it
      // See issue https://issues.openbravo.com/view.php?id=29846
      String subEntityClientOrg = "";
      String whereClauseFirstWord = StringUtils.isEmpty(whereClause.trim()) ? "where" : "and";
      if (entity.getMappingClass().isAssignableFrom(Organization.class)) {
        subEntityClientOrg = " " + whereClauseFirstWord + " e.id "
            + createInClause(OBContext.getOBContext().getReadableOrganizations());
      } else if (entity.getMappingClass().isAssignableFrom(Client.class)) {
        subEntityClientOrg = " " + whereClauseFirstWord + " e.id "
            + createInClause(OBContext.getOBContext().getReadableClients());
      }
      if (!subEntityClientOrg.isEmpty()) {
        whereClause += subEntityClientOrg;
      }
    }

    return whereClause;
  }

  private String createInClause(String[] values) {
    if (values.length == 0) {
      return " in ('') ";
    }
    final StringBuilder sb = new StringBuilder();
    for (final String v : values) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append("'" + v + "'");
    }
    return " in (" + sb.toString() + ")";
  }

  private String addWhereOrgParameters(String where) {
    String localWhereClause = where;

    // add the organization parameter
    StringBuilder orgPart = new StringBuilder();
    if (filterParameters.containsKey(JsonConstants.ORG_PARAMETER)) {
      final String value = filterParameters.get(JsonConstants.ORG_PARAMETER);
      if (entity.isOrganizationEnabled() && value != null && value.length() > 0) {
        orgPart = buildOrgPartWhereClause(getReadableOrgsInOrgNaturalTree(value));
      } else if (Organization.TABLE_NAME.equals(entity.getTableName())) {
        orgPart = buildOrgPartWhereClause(getReadableOrgs());
      }
    } else if (filterParameters.containsKey(JsonConstants.CALCULATE_ORGS)) {
      orgPart = buildOrgPartWhereClause(getReadableOrgs());
    }
    localWhereClause = buildLocalWhereClause(localWhereClause, orgPart);

    // add the special whereParameter
    final String whereParameter = filterParameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE);
    if (whereParameter != null && !whereParameter.equals("null") && whereParameter.length() > 0) {
      if (localWhereClause.length() > 0) {
        localWhereClause = " (" + localWhereClause + ") and (" + whereParameter + ") ";
      } else {
        localWhereClause = " " + whereParameter;
      }
    }
    return localWhereClause;
  }

  private Set<String> getReadableOrgsInOrgNaturalTree(String adOrgId) {
    Set<String> orgs = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getNaturalTree(adOrgId);
    String[] readableOrgs = OBContext.getOBContext().getReadableOrganizations();
    Set<String> readableOrgsSet = new HashSet<>(Arrays.asList(readableOrgs));
    orgs.retainAll(readableOrgsSet);
    return orgs;
  }

  private Set<String> getReadableOrgs() {
    return new HashSet<>(Arrays.asList(OBContext.getOBContext().getReadableOrganizations()));
  }

  private String buildLocalWhereClause(String localWhere, StringBuilder orgPart) {
    if (localWhere == null || localWhere.length() == 0) {
      return (orgPart.length() > 0 ? orgPart.toString() : "");
    } else {
      return ("(" + localWhere + ")" + (orgPart.length() > 0 ? " and " + orgPart.toString() : ""));
    }
  }

  private StringBuilder buildOrgPartWhereClause(Set<String> organizations) {
    StringBuilder buildOrgPart = new StringBuilder();
    if (organizations.size() > 0) {
      if (getMainAlias() != null) {
        String organizationEntity = Organization.ENTITY_NAME.equals(entity.toString()) ? ".id"
            : ".organization";
        buildOrgPart.append(" " + getMainAlias() + organizationEntity + " in (");
      } else {
        buildOrgPart.append(" organization in (");
      }
      boolean addComma = false;
      for (String org : organizations) {
        if (addComma) {
          buildOrgPart.append(",");
        }
        buildOrgPart.append("'" + org + "'");
        addComma = true;
      }
      buildOrgPart.append(") ");
    }
    return buildOrgPart;
  }

  private String substituteParameters(String where) {

    // add some default filter parameters which are substituted
    filterParameters.put(JsonConstants.QUERY_PARAM_USER,
        OBContext.getOBContext().getUser().getId());
    if (!filterParameters.containsKey(JsonConstants.QUERY_PARAM_CLIENT)) {
      filterParameters.put(JsonConstants.QUERY_PARAM_CLIENT,
          OBContext.getOBContext().getCurrentClient().getId());
    }

    String localWhereClause = where;
    // handle special transactional range parameter
    if (localWhereClause.contains(JsonConstants.QUERY_PARAM_TRANSACTIONAL_RANGE)) {
      final String alias = getTypedParameterAlias();
      String windowId = RequestContext.get().getRequestParameter("windowId");
      if (windowId == null) {
        windowId = "";
      }
      final String range = Utility.getTransactionalDate(new DalConnectionProvider(false),
          RequestContext.get().getVariablesSecureApp(), windowId);
      final int rangeNum = Integer.parseInt(range);
      final Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, -1 * rangeNum);
      localWhereClause = localWhereClause.replace(JsonConstants.QUERY_PARAM_TRANSACTIONAL_RANGE,
          alias);
      typedParameters.add(cal.getTime());
    }

    if (localWhereClause.contains(JsonConstants.QUERY_PARAM_CLIENT)) {
      final String alias = getTypedParameterAlias();
      String clientId = OBContext.getOBContext().getCurrentClient().getId();
      localWhereClause = localWhereClause.replace(JsonConstants.QUERY_PARAM_CLIENT, alias);
      typedParameters.add(clientId);
    }
    localWhereClause = setRequestParameters(localWhereClause);
    return substituteContextParameters(localWhereClause);
  }

  private String parseCriteria(JSONObject jsonCriteria) throws JSONException {
    // a constructor so the content is an advanced criteria
    if (jsonCriteria.has("_constructor") || hasOrAndOperator(jsonCriteria)) {
      return parseAdvancedCriteria(jsonCriteria);
    }
    return parseSingleClause(jsonCriteria);
  }

  private boolean hasOrAndOperator(JSONObject jsonCriteria) throws JSONException {
    if (!jsonCriteria.has(OPERATOR_KEY)) {
      return false;
    }
    return OPERATOR_OR.equals(jsonCriteria.get(OPERATOR_KEY))
        || OPERATOR_AND.equals(jsonCriteria.get(OPERATOR_KEY));
  }

  private String parseSingleClause(JSONObject jsonCriteria) throws JSONException {
    String operator = jsonCriteria.getString(OPERATOR_KEY);

    if (operator.equals(OPERATOR_BETWEEN) || operator.equals(OPERATOR_BETWEENINCLUSIVE)
        || operator.equals(OPERATOR_IBETWEEN) || operator.equals(OPERATOR_IBETWEENINCLUSIVE)) {
      return parseBetween(jsonCriteria, operator, true);
    }

    Object value = jsonCriteria.has(VALUE_KEY) ? jsonCriteria.get(VALUE_KEY) : null;

    if (operator.equals(OPERATOR_EXISTS)) {
      String query = jsonCriteria.getString(EXISTS_QUERY_KEY);
      String alias = getTypedParameterAlias();
      query = query.replace(EXISTS_VALUE_HOLDER, alias);

      final List<Object> typedValues = new ArrayList<Object>();
      final JSONArray values = (JSONArray) value;
      for (int i = 0; i < values.length(); i++) {
        typedValues.add(values.getString(i));
      }

      typedParameters.add(typedValues);
      return query;
    }

    String fieldName = jsonCriteria.getString(FIELD_NAME_KEY);

    // translate to a OR for each value
    // inSet or notInSet operators should not be translated to an OR for each value
    if (value instanceof JSONArray && !(OPERATOR_INSET.equals(jsonCriteria.getString(OPERATOR_KEY)) || OPERATOR_NOTINSET.equals(jsonCriteria.getString(OPERATOR_KEY)))) {
      final JSONArray jsonArray = (JSONArray) value;
      final JSONObject advancedCriteria = new JSONObject();
      advancedCriteria.put(OPERATOR_KEY, OPERATOR_OR);
      final JSONArray subCriteria = new JSONArray();
      for (int i = 0; i < jsonArray.length(); i++) {
        final JSONObject subCriterion = new JSONObject();
        subCriterion.put(OPERATOR_KEY, operator);
        subCriterion.put(FIELD_NAME_KEY, fieldName);
        subCriterion.put(VALUE_KEY, jsonArray.get(i));
        subCriteria.put(i, subCriterion);
      }
      advancedCriteria.put(CRITERIA_KEY, subCriteria);
      return parseAdvancedCriteria(advancedCriteria);
    }

    // Retrieves the UTC time zone offset of the client
    if (jsonCriteria.has("minutesTimezoneOffset")) {
      int clientMinutesTimezoneOffset = Integer
          .parseInt(jsonCriteria.get("minutesTimezoneOffset").toString());
      Calendar now = Calendar.getInstance();
      // Obtains the UTC time zone offset of the server
      int serverMinutesTimezoneOffset = (now.get(Calendar.ZONE_OFFSET)
          + now.get(Calendar.DST_OFFSET)) / (1000 * 60);
      // Obtains the time zone offset between the server and the client
      clientUTCMinutesTimeZoneDiff = clientMinutesTimezoneOffset;
      UTCServerMinutesTimeZoneDiff = serverMinutesTimezoneOffset;
    }

    if (operator.equals(OPERATOR_ISNULL) || operator.equals(OPERATOR_NOTNULL)) {
      value = null;
    }

    // if a comparison is done on an equal date then replace
    // with a between start time and end time on that date
    if (operator.equals(OPERATOR_EQUALS) || operator.equals(OPERATOR_EQUALSFIELD)) {
      final List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), fieldName);
      if (properties.isEmpty()) {
        return null;
      }
      final Property property = properties.get(properties.size() - 1);
      if (property == null) {
        return null;
      }
      // create the clauses, re-uses the code in parseSimpleClause
      // which translates a lesserthan/greater than to the end/start
      // time of a date
      if (property.isDate() || property.isDatetime() || property.isAbsoluteDateTime()) {
        if (operator.equals(OPERATOR_EQUALS)) {
          return "(" + parseSimpleClause(fieldName, OPERATOR_GREATEROREQUAL, value) + " and "
              + parseSimpleClause(fieldName, OPERATOR_LESSOREQUAL, value) + ")";

        } else {
          return "(" + parseSimpleClause(fieldName, OPERATOR_GREATEROREQUALFIELD, value) + " and "
              + parseSimpleClause(fieldName, OPERATOR_LESSOREQUALFIElD, value) + ")";
        }
      }
    }

    return parseSimpleClause(fieldName, operator, value);
  }

  private String parseBetween(JSONObject jsonCriteria, String operator, boolean inclusive)
      throws JSONException {
    final String fieldName = jsonCriteria.getString(FIELD_NAME_KEY);
    final Object start = jsonCriteria.get("start");
    final Object end = jsonCriteria.get("end");
    final String leftClause = parseSimpleClause(fieldName, getBetweenOperator(operator, false),
        start);
    final String rightClause = parseSimpleClause(fieldName, getBetweenOperator(operator, true),
        end);
    if (leftClause != null && rightClause != null) {
      return "(" + leftClause + " and " + rightClause + ")";
    }
    return null;
  }

  private String parseSimpleClause(String fieldName, String operator, Object value)
      throws JSONException {

    // note: code duplicated in parseSingleClause
    final List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), fieldName);
    if (properties.isEmpty()) {
      return null;
    }
    final Property property = properties.get(properties.size() - 1);

    if (property == null) {
      return null;
    }

    String leftClause = buildFieldClause(properties, property, fieldName, operator);
    String hqlOperator = getHqlOperator(operator);
    // special case
    if (value != null && value.toString().contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
      hqlOperator = "in";
    }

    String rightClause = buildRightClause(property, operator, value);

    if (hqlOperator.equals("in")) {
      rightClause = "(" + rightClause + ")";
    }

    if (isNot(operator)) {
      return "not(" + leftClause + " " + hqlOperator + " " + rightClause + ")";
    } else {
      return leftClause + " " + hqlOperator + " " + rightClause;
    }
  }

  private String buildRightClause(Property property, String operator, Object value)
      throws JSONException {
    if (value == null) {
      return null;
    }
    // the right side can be a field
    if (operator.equals(OPERATOR_EQUALSFIELD) || operator.equals(OPERATOR_NOTEQUALFIELD)
        || operator.equals(OPERATOR_GREATERTHANFIElD) || operator.equals(OPERATOR_LESSTHANFIELD)
        || operator.equals(OPERATOR_GREATEROREQUALFIELD)
        || operator.equals(OPERATOR_LESSOREQUALFIElD) || operator.equals(OPERATOR_CONTAINSFIELD)
        || operator.equals(OPERATOR_STARTSWITHFIELD) || operator.equals(OPERATOR_ENDSWITHFIELD)) {
      List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), value.toString());
      properties = getPropertyForTableReference(properties);
      if (properties.isEmpty()) {
        // invalid property, report it with a listing of allowed names
        final StringBuilder sb = new StringBuilder();
        for (Property prop : getEntity().getProperties()) {
          if (prop.isId() || prop.isOneToMany() || prop.isBoolean() || prop.isDate()
              || prop.isDatetime() || prop.isAbsoluteDateTime()
              || prop.getAllowedValues().size() > 0 || prop.isInactive() || prop.isEncrypted()
              || prop.isOneToOne()) {
            continue;
          }
          if (!prop.isPrimitive()) {
            continue;
          }
          if (sb.length() > 0) {
            sb.append(", ");
          }
          sb.append(prop.getName());
        }
        throw new OBException(OBMessageUtils.getI18NMessage("OBJSON_InvalidProperty",
            new String[] { value.toString(), sb.toString() }));
      }
      final Property fieldProperty = properties.get(properties.size() - 1);
      if (property == null) {
        return null;
      }
      return buildFieldClause(properties, fieldProperty, value.toString(), operator);
    } else {
      return buildValueClause(property, operator, value);
    }
  }

  private String buildFieldClause(List<Property> properties, Property property, String fieldName,
      String operator) {

    // special cases:
    // TableDomainType
    // TableDirDomainType

    // handle a special case the table reference which shows a tablename in a combo
    // or uses the display column to display that in the grid
    Property useProperty = property;
    String useFieldName = fieldName.replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT);

    if (useProperty.isComputedColumn()) {
      // Computed columns are not directly accessed but through _computedColumns proxy
      useFieldName = Entity.COMPUTED_COLUMNS_PROXY_PROPERTY + DalUtil.DOT + useFieldName;
    }

    boolean tableReference = false;
    if (properties.size() >= 2) {
      final Property refProperty = properties.get(properties.size() - 2);
      tableReference = refProperty.getDomainType() instanceof TableDomainType;
      if (tableReference) {
        // special case table reference itself
        final boolean isTable = property.getEntity() == ModelProvider.getInstance()
            .getEntity(Table.ENTITY_NAME);
        if (isTable) {
          useProperty = property.getEntity().getProperty(Table.PROPERTY_NAME);
          final int index = useFieldName.indexOf(DalUtil.DOT);
          useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName();
        } else if (fieldName.contains(JsonConstants.IDENTIFIER)) {
          // After solving issue https://issues.openbravo.com/view.php?id=30800, the displayed
          // property of table references is sent from the client side when filtering.
          // So, the property used to filter the table reference (displayed property) is replaced
          // just in case the _identifier value is sent in some case as part of the fieldName.

          // read the reference to get the table reference
          final Reference reference = OBDal.getInstance()
              .get(Reference.class, refProperty.getDomainType().getReference().getId());
          for (ReferencedTable referencedTable : reference.getADReferencedTableList()) {
            if (referencedTable.isActive() && referencedTable.getDisplayedColumn() != null
                && referencedTable.getDisplayedColumn().isActive()) {
              useProperty = property.getEntity()
                  .getPropertyByColumnName(referencedTable.getDisplayedColumn().getDBColumnName());
              final int index = useFieldName.lastIndexOf(DalUtil.DOT);
              if (useProperty.isPrimitive()) {
                useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName();
              } else {
                // adding _identifier so that the identifier properties will be formed properly in
                // computeLeftWhereClauseForIdentifier.
                useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName()
                    + DalUtil.DOT + JsonConstants.IDENTIFIER;
              }
              break;
            }
          }
        }
      }
    }

    String clause = null;
    if (!creatingJoinsInWhereClauseIsPrevented && orNesting > 0) {
      boolean fromCriteria = true;
      clause = resolveJoins(properties, useFieldName, fromCriteria);
    } else if (getMainAlias() != null) {
      clause = getMainAlias() + DalUtil.DOT + useFieldName.trim();
    } else {
      clause = useFieldName;
    }

    // get rid of the identifier and replace it with the real property name
    // or with the concatenation if there are multiple parts
    // NOTE: the if and else check against the key variable and not the leftwherepart
    // because the key contains the original string (with the _identifier part).
    // Within the if the leftWherePart is used because it contains the join aliases
    if (useFieldName.equals(JsonConstants.IDENTIFIER)
        || useFieldName.endsWith(DalUtil.DOT + JsonConstants.IDENTIFIER)) {
      if (useFieldName.endsWith(DalUtil.DOT + JsonConstants.IDENTIFIER)
          && (operator.equals(OPERATOR_ISNULL) || operator.equals(OPERATOR_NOTNULL))) {
        clause = getMainAlias() + DalUtil.DOT
            + useFieldName.replace(DalUtil.DOT + JsonConstants.IDENTIFIER, "");
      } else {
        final Property refProperty = this.distinctProperty;
        if (refProperty != null) {
          tableReference = refProperty.getDomainType() instanceof TableDomainType;
        }
        if (subEntity != null && tableReference) {
          final boolean isTable = property.getEntity() == ModelProvider.getInstance()
              .getEntity(Table.ENTITY_NAME);
          if (isTable) {
            useProperty = property.getEntity().getProperty(Table.PROPERTY_NAME);
            final int index = useFieldName.indexOf(DalUtil.DOT);
            useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName();
          } else {
            // read the reference to get the table reference
            final Reference reference = OBDal.getInstance()
                .get(Reference.class, refProperty.getDomainType().getReference().getId());
            for (ReferencedTable referencedTable : reference.getADReferencedTableList()) {
              if (referencedTable.isActive() && referencedTable.getDisplayedColumn() != null
                  && referencedTable.getDisplayedColumn().isActive()) {
                useProperty = property.getEntity()
                    .getPropertyByColumnName(
                        referencedTable.getDisplayedColumn().getDBColumnName());
                final int index = useFieldName.lastIndexOf(DalUtil.DOT);
                if (useProperty.isPrimitive()) {
                  useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName();
                } else {
                  // adding _identifier so that the identifier properties will be formed properly in
                  // computeLeftWhereClauseForIdentifier.
                  useFieldName = useFieldName.substring(0, index + 1) + useProperty.getName()
                      + DalUtil.DOT + JsonConstants.IDENTIFIER;
                }
                break;
              }
            }
          }
          clause = getEntity() + DalUtil.DOT + useFieldName;
          if (!useProperty.isPrimitive()) {
            clause = computeLeftWhereClauseForIdentifier(useProperty, useFieldName, clause,
                tableReference);
          } else {
            // passing true for last argument to apply filterCriteria
            clause = getMainAlias() + DalUtil.DOT + useFieldName;
          }
        } else {
          clause = computeLeftWhereClauseForIdentifier(useProperty, useFieldName, clause,
              tableReference);
        }
      }
    } else if (!useProperty.isPrimitive()) {
      clause = clause + "." + getReferencedPropertyName(useProperty);
    } else if (tableReference && useProperty.isTranslatable()
        && OBContext.hasTranslationInstalled()) {
      // filtering by table reference translatable field: use translation table
      clause = computeLeftWhereClauseForIdentifier(useProperty, useFieldName, clause,
          tableReference);
    }
    if (ignoreCase(properties, operator)) {
      clause = "upper(" + clause + ")";
    }

    // if the operator is isNull or notNull comparison should be done at the object level and not at
    // the field value level. Refer issue https://issues.openbravo.com/view.php?id=25447
    if (tableReference && (operator.equals(OPERATOR_ISNULL) || operator.equals(OPERATOR_NOTNULL))) {
      clause = clause.substring(0, clause.lastIndexOf(DalUtil.DOT));
    }
    return clause;
  }

  private String getReferencedPropertyName(Property property) {
    return property.getReferencedProperty().getName();
  }

  private String buildValueClause(Property property, String operator, Object value)
      throws JSONException {
    String alias = getTypedParameterAlias();
    if (ignoreCase(property, operator)) {
      alias = "upper(" + alias + ")";
    }

    String clause = alias;
    if (isLike(operator)) {
      clause += " escape '" + ESCAPE_CHAR + "' ";
    }

    Object localValue = unEscapeOperator(value);
    if (!property.isPrimitive() && localValue != null
        && localValue.toString().contains(JsonConstants.IN_PARAMETER_SEPARATOR)) {
      clause = "(" + clause + ")";
      localValue = Arrays.asList(localValue.toString().split(JsonConstants.IN_PARAMETER_SEPARATOR));
    }

    try {
      localValue = getTypeSafeValue(operator, property, localValue);
    } catch (IllegalArgumentException e) {
      throw new OBException(OBMessageUtils.getI18NMessage("OBJSON_InvalidFilterValue",
          new String[] { Objects.toString(value, "") }));
    }
    typedParameters.add(localValue);
    return clause;
  }

  private Object getTypeSafeValue(String operator, Property property, Object value)
      throws JSONException {
    if (value == null) {
      return value;
    }

    if (isLike(operator)) {
      if (operator.equals(OPERATOR_INOTCONTAINS) || operator.equals(OPERATOR_ICONTAINS)
          || operator.equals(OPERATOR_CONTAINSFIELD)) {
        return "%" + escapeLike(value.toString()).replaceAll(" ", "%") + "%";
      } else if (operator.equals(OPERATOR_NOTCONTAINS) || operator.equals(OPERATOR_CONTAINS)) {
        return "%" + escapeLike(value.toString()).replaceAll(" ", "%") + "%";
      } else if (operator.equals(OPERATOR_INOTSTARTSWITH) || operator.equals(OPERATOR_ISTARTSWITH)
          || operator.equals(OPERATOR_STARTSWITHFIELD)) {
        return escapeLike(value.toString()).replaceAll(" ", "%") + "%";
      } else if (operator.equals(OPERATOR_NOTSTARTSWITH) || operator.equals(OPERATOR_STARTSWITH)) {
        return escapeLike(value.toString()).replaceAll(" ", "%") + "%";
      } else {
        return "%" + escapeLike(value.toString());
      }
    }

    if (operator.equals(OPERATOR_INSET) || operator.equals(OPERATOR_NOTINSET)) {
      final List<Object> typedValues = new ArrayList<Object>();
      final JSONArray values = (JSONArray) value;
      for (int i = 0; i < values.length(); i++) {
        typedValues.add(getTypeSafeValue(OPERATOR_EQUALS, property, values.get(i)));
      }
      return typedValues;
    }

    if (property.getDomainType() instanceof SearchDomainType) {
      return value;
    }

    // a FK. Old selectors is an special key, though they are not primitive they should be treated
    // as text
    if (!property.isPrimitive() && !(property.getDomainType() instanceof SearchDomainType)) {
      return value;
    }

    if (Boolean.class == property.getPrimitiveObjectType()) {
      return Boolean.valueOf(value.toString());
    } else if (property.isNumericType()) {
      try {
        final BigDecimal bdValue = new BigDecimal(value.toString());
        if (Long.class == property.getPrimitiveObjectType()) {
          return bdValue.longValue();
        } else if (Integer.class == property.getPrimitiveObjectType()) {
          return bdValue.intValue();
        } else {
          return bdValue;
        }
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException(e);
      }
    } else if (Date.class.isAssignableFrom(property.getPrimitiveObjectType())) {
      try {
        Date date = null;
        boolean hasComeADateTime = true;
        if (property.isDatetime() || property.isAbsoluteDateTime()) {
          try {
            date = simpleDateTimeFormat.parse(value.toString());
          } catch (ParseException e) {
            // When a DateTime column is filtered, plain Date values are used
            // See issue https://issues.openbravo.com/view.php?id=23203
            hasComeADateTime = false;
            date = simpleDateFormat.parse(value.toString());
          }
        }
        if (property.isDate()) {
          hasComeADateTime = false;
          date = simpleDateFormat.parse(value.toString());
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);

        if (!hasComeADateTime) {
          // move the date to the beginning of the day
          if (isGreaterOperator(operator)) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
          } else if (isLesserOperator(operator)) {
            // move the data to the end of the day
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
          }
        }

        if (hasComeADateTime || property.isDatetime() || property.isDate()) {

          // Applies the time zone offset difference of the client
          // Just in case the date needs to be changed
          calendar.add(Calendar.MINUTE, -clientUTCMinutesTimeZoneDiff);

          // Applies the time zone offset difference of the server
          calendar.add(Calendar.MINUTE, UTCServerMinutesTimeZoneDiff);
        }

        return calendar.getTime();
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
    }
    return value;
  }

  @SuppressWarnings("unchecked")
  private <T> T unEscapeOperator(T val) {
    if (val == null || !(val instanceof String)) {
      return val;
    }
    String localVal = (String) val;
    localVal = localVal.replace("\\and", "and");
    localVal = localVal.replace("\\or", "or");
    localVal = localVal.replace("\\AND", "AND");
    localVal = localVal.replace("\\OR", "OR");
    return (T) localVal;
  }

  private boolean isGreaterOperator(String operator) {
    return operator != null && (operator.equals(OPERATOR_GREATERTHAN)
        || operator.equals(OPERATOR_GREATEROREQUAL) || operator.equals(OPERATOR_IGREATERTHAN)
        || operator.equals(OPERATOR_IGREATEROREQUAL) || operator.equals(OPERATOR_GREATERTHANFIElD)
        || operator.equals(OPERATOR_GREATEROREQUALFIELD));
  }

  private boolean isLesserOperator(String operator) {
    return operator != null && (operator.equals(OPERATOR_LESSTHAN)
        || operator.equals(OPERATOR_LESSOREQUAL) || operator.equals(OPERATOR_ILESSTHAN)
        || operator.equals(OPERATOR_ILESSOREQUAL) || operator.equals(OPERATOR_LESSTHANFIELD)
        || operator.equals(OPERATOR_LESSOREQUALFIElD));
  }

  private String computeLeftWhereClauseForIdentifier(Property property, String key,
      String leftWherePart, boolean isTableReference) {

    // the identifierProperties are read from the owning entity of the
    // property, that should work fine, as this last property is always part of the
    // identifier
    List<Property> identifierProperties = null;
    identifierProperties = property.getEntity().getIdentifierProperties();
    if (!isTableReference) {
      Check.isTrue(identifierProperties.contains(property),
          "Property " + property + " not part of identifier of " + property.getEntity());
    } else {
      // for table references, the display column identifier properties should be used in the joins
      if (property.getTargetEntity() != null) {
        identifierProperties = property.getTargetEntity().getIdentifierProperties();
      }
    }
    String prefix = "";
    final int index = leftWherePart.lastIndexOf(DalUtil.DOT);
    if (key.equals(JsonConstants.IDENTIFIER)) {
      prefix = getMainAlias() + DalUtil.DOT;
    } else if (key.endsWith(JsonConstants.IDENTIFIER) && !creatingJoinsInWhereClauseIsPrevented) {
      final String propPath = key.substring(0, key.indexOf(JsonConstants.IDENTIFIER) - 1);
      boolean fromCriteria = true;
      final String join = resolveJoins(
          getPropertyForTableReference(JsonUtils.getPropertiesOnPath(getEntity(), propPath)),
          propPath, fromCriteria);
      prefix = join + DalUtil.DOT;
    } else if (index == -1) {
      prefix = "";
    } else {
      // the + 1 makes sure that the dot is included
      if (index != -1) {
        prefix = leftWherePart.substring(0, index + 1);
      }
    }
    return createIdentifierLeftClause(identifierProperties, prefix);
  }

  private String parseAdvancedCriteria(JSONObject advancedCriteria) throws JSONException {
    final String operator = advancedCriteria.getString(OPERATOR_KEY);
    if (operator.equals(OPERATOR_NOT)) {
      final String clause = parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY),
          "or");
      if (clause != null) {
        return " not(" + clause + ")";
      }
      return null;
    }
    if (operator.equals(OPERATOR_AND)) {
      return parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY), "and");
    }
    if (operator.equals(OPERATOR_OR)) {
      orNesting++;
      final String value = parseStructuredClause(advancedCriteria.getJSONArray(CRITERIA_KEY), "or");
      orNesting--;
      return value;
    }
    return parseSingleClause(advancedCriteria);
  }

  private String parseStructuredClause(JSONArray clauses, String hqlOperator) throws JSONException {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < clauses.length(); i++) {
      final JSONObject clause = clauses.getJSONObject(i);
      if (clause.has(VALUE_KEY) && clause.get(VALUE_KEY) != null
          && clause.getString(VALUE_KEY).equals("")) {
        continue;
      }
      final String clauseString = parseCriteria(clause);
      if (clauseString != null) {
        if (sb.length() > 0) {
          sb.append(" " + hqlOperator + " ");
        }
        sb.append(" " + clauseString + " ");
      }
    }
    if (sb.length() > 0) {
      return "(" + sb.toString() + ")";
    }
    return null;
  }

  private boolean isLike(String operator) {
    return operator.equals(OPERATOR_ICONTAINS) || operator.equals(OPERATOR_IENDSWITH)
        || operator.equals(OPERATOR_ISTARTSWITH) || operator.equals(OPERATOR_CONTAINS)
        || operator.equals(OPERATOR_ENDSWITH) || operator.equals(OPERATOR_STARTSWITH)
        || operator.equals(OPERATOR_NOTCONTAINS) || operator.equals(OPERATOR_INOTCONTAINS)
        || operator.equals(OPERATOR_NOTENDSWITH) || operator.equals(OPERATOR_NOTSTARTSWITH)
        || operator.equals(OPERATOR_INOTENDSWITH) || operator.equals(OPERATOR_INOTSTARTSWITH)
        || operator.equals(OPERATOR_CONTAINSFIELD) || operator.equals(OPERATOR_ENDSWITHFIELD)
        || operator.equals(OPERATOR_STARTSWITHFIELD);
  }

  private String getBetweenOperator(String operator, boolean rightClause) {
    if (operator.equals(OPERATOR_IBETWEEN)) {
      if (rightClause) {
        return OPERATOR_ILESSTHAN;
      } else {
        return OPERATOR_IGREATERTHAN;
      }
    }
    if (operator.equals(OPERATOR_BETWEEN)) {
      if (rightClause) {
        return OPERATOR_LESSTHAN;
      } else {
        return OPERATOR_GREATERTHAN;
      }
    }
    if (operator.equals(OPERATOR_IBETWEENINCLUSIVE)) {
      if (rightClause) {
        return OPERATOR_ILESSOREQUAL;
      } else {
        return OPERATOR_IGREATEROREQUAL;
      }
    }
    if (operator.equals(OPERATOR_BETWEENINCLUSIVE)) {
      if (rightClause) {
        return OPERATOR_LESSOREQUAL;
      } else {
        return OPERATOR_GREATEROREQUAL;
      }
    }
    throw new IllegalArgumentException("Operator not supported " + operator);
  }

  private boolean ignoreCase(List<Property> properties, String operator) {
    boolean operatorCase = operator.equals(OPERATOR_IEQUALS) || operator.equals(OPERATOR_INOTEQUAL)
        || operator.equals(OPERATOR_ICONTAINS) || operator.equals(OPERATOR_INOTSTARTSWITH)
        || operator.equals(OPERATOR_INOTENDSWITH) || operator.equals(OPERATOR_NOTSTARTSWITH)
        || operator.equals(OPERATOR_NOTCONTAINS) || operator.equals(OPERATOR_INOTCONTAINS)
        || operator.equals(OPERATOR_NOTENDSWITH) || operator.equals(OPERATOR_IENDSWITH)
        || operator.equals(OPERATOR_ISTARTSWITH) || operator.equals(OPERATOR_IBETWEEN)
        || operator.equals(OPERATOR_IGREATEROREQUAL) || operator.equals(OPERATOR_ILESSOREQUAL)
        || operator.equals(OPERATOR_IGREATERTHAN) || operator.equals(OPERATOR_ILESSTHAN)
        || operator.equals(OPERATOR_IBETWEENINCLUSIVE);

    for (Property property : properties) {
      if (!property.isPrimitive() || (!property.isNumericType() && !property.isDate()
          && !property.isDatetime() && !property.isAbsoluteDateTime())) {
        return operatorCase;
      }
    }
    return false;
  }

  private boolean ignoreCase(Property property, String operator) {
    if (property.isPrimitive() && (property.isNumericType() || property.isDate()
        || property.isDatetime() || property.isAbsoluteDateTime())) {
      return false;
    }
    return operator.equals(OPERATOR_IEQUALS) || operator.equals(OPERATOR_INOTEQUAL)
        || operator.equals(OPERATOR_ICONTAINS) || operator.equals(OPERATOR_INOTSTARTSWITH)
        || operator.equals(OPERATOR_INOTENDSWITH) || operator.equals(OPERATOR_INOTCONTAINS)
        || operator.equals(OPERATOR_IENDSWITH) || operator.equals(OPERATOR_ISTARTSWITH)
        || operator.equals(OPERATOR_IBETWEEN) || operator.equals(OPERATOR_IGREATEROREQUAL)
        || operator.equals(OPERATOR_ILESSOREQUAL) || operator.equals(OPERATOR_IGREATERTHAN)
        || operator.equals(OPERATOR_ILESSTHAN) || operator.equals(OPERATOR_IBETWEENINCLUSIVE);
  }

  private boolean isNot(String operator) {
    return operator.equals(OPERATOR_NOTCONTAINS) || operator.equals(OPERATOR_NOTENDSWITH)
        || operator.equals(OPERATOR_NOTSTARTSWITH) || operator.equals(OPERATOR_INOTCONTAINS)
        || operator.equals(OPERATOR_INOTENDSWITH) || operator.equals(OPERATOR_INOTSTARTSWITH)
        || operator.equals(OPERATOR_NOT) || operator.equals(OPERATOR_NOTINSET);
  }

  public static String getHqlOperator(String operator) {
    if (operator.equals(OPERATOR_EQUALS)) {
      return "=";
    } else if (operator.equals(OPERATOR_INSET)) {
      return "in";
    } else if (operator.equals(OPERATOR_NOTINSET)) {
      return "in";
    } else if (operator.equals(OPERATOR_NOTEQUAL)) {
      return "!=";
    } else if (operator.equals(OPERATOR_IEQUALS)) {
      return "=";
    } else if (operator.equals(OPERATOR_INOTEQUAL)) {
      return "!=";
    } else if (operator.equals(OPERATOR_GREATERTHAN)) {
      return ">";
    } else if (operator.equals(OPERATOR_LESSTHAN)) {
      return "<";
    } else if (operator.equals(OPERATOR_GREATEROREQUAL)) {
      return ">=";
    } else if (operator.equals(OPERATOR_LESSOREQUAL)) {
      return "<=";
    } else if (operator.equals(OPERATOR_IGREATERTHAN)) {
      return ">";
    } else if (operator.equals(OPERATOR_ILESSTHAN)) {
      return "<";
    } else if (operator.equals(OPERATOR_IGREATEROREQUAL)) {
      return ">=";
    } else if (operator.equals(OPERATOR_ILESSOREQUAL)) {
      return "<=";
    } else if (operator.equals(OPERATOR_CONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_STARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_ENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_ICONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_ISTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_IENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTCONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTSTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_NOTENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTCONTAINS)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTSTARTSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_INOTENDSWITH)) {
      return "like";
    } else if (operator.equals(OPERATOR_EQUALSFIELD)) {
      return "=";
    } else if (operator.equals(OPERATOR_NOTEQUALFIELD)) {
      return "!=";
    } else if (operator.equals(OPERATOR_GREATERTHANFIElD)) {
      return ">";
    } else if (operator.equals(OPERATOR_LESSTHANFIELD)) {
      return "<";
    } else if (operator.equals(OPERATOR_GREATEROREQUALFIELD)) {
      return ">=";
    } else if (operator.equals(OPERATOR_LESSOREQUALFIElD)) {
      return "<=";
    } else if (operator.equals(OPERATOR_CONTAINSFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_STARTSWITHFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_ENDSWITHFIELD)) {
      return "like";
    } else if (operator.equals(OPERATOR_ISNULL)) {
      return "is";
    } else if (operator.equals(OPERATOR_NOTNULL)) {
      return "is not";
    } else if (operator.equals(OPERATOR_EXISTS)) {
      return "exists";
    }
    // todo throw exception
    return null;
  }

  private String substituteContextParameters(String currentWhereClause) {
    // This method will check for any remaining @params@
    // If there are still some in the whereclause, they will be resolved by calling the getContext()
    // method
    if (!currentWhereClause.contains("@")) {
      return currentWhereClause;
    }
    String localWhereClause = currentWhereClause;
    String tabId = RequestContext.get().getRequestParameter("tabId");
    Tab tab = null;
    while (localWhereClause.contains("@")) {
      int firstAtIndex = localWhereClause.indexOf("@");
      String prefix = localWhereClause.substring(0, firstAtIndex);
      String restOfClause = localWhereClause.substring(firstAtIndex + 1);
      int secondAtIndex = restOfClause.indexOf("@");
      if (secondAtIndex == -1) {
        // No second @. We return the clause as it is
        return localWhereClause;
      }
      String suffix = restOfClause.substring(secondAtIndex + 1);
      String param = restOfClause.substring(0, secondAtIndex);
      String paramValue = "";

      // Try to select the value from the request instead of picking it from the context
      // Look if param is an ID
      if (param.substring(param.length() - 3).toUpperCase().equals("_ID")
          && !StringUtils.isEmpty(tabId)) {
        VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
        Entity paramEntity = ModelProvider.getInstance()
            .getEntityByTableName(param.substring(0, param.length() - 3));

        if (tab == null) {
          tab = OBDal.getInstance().get(Tab.class, tabId);
        }
        Tab ancestorTab = KernelUtils.getInstance().getParentTab(tab);
        boolean checkIsNotNull = false;

        while (ancestorTab != null && paramValue.equals("")) {

          Entity tabEntity = ModelProvider.getInstance()
              .getEntityByTableName(ancestorTab.getTable().getDBTableName());

          if (tabEntity.equals(paramEntity)) {
            paramValue = vars.getStringParameter("@" + paramEntity.getName() + ".id@");
          } else {
            try {
              Property prop = tabEntity.getPropertyByColumnName(param, checkIsNotNull);
              if (prop == null) {
                paramValue = "";
              } else {
                paramValue = vars.getStringParameter("@" + tabEntity + "." + prop.getName() + "@");
              }
            } catch (Exception ignore) {
              // ignoring exception as the property might be found from context.
              // for eg., refer issue https://issues.openbravo.com/view.php?id=26871
            }
          }
          ancestorTab = KernelUtils.getInstance().getParentTab(ancestorTab);
        }
      }

      // If paramValue has not been brought form the request, select it from context
      if (paramValue.equals("")) {
        paramValue = Utility.getContext(new DalConnectionProvider(false),
            RequestContext.get().getVariablesSecureApp(), param,
            RequestContext.get().getRequestParameter("windowId") != null
                ? RequestContext.get().getRequestParameter("windowId")
                : "");
      }

      // not found, try to get the parameter directly from the request object
      if (paramValue.equals("") && RequestContext.get().getRequestParameter(param) != null) {
        paramValue = RequestContext.get().getRequestParameter(param);
      }

      localWhereClause = prefix + getTypedParameterAlias() + suffix;

      if (ACCESIBLE_ORG_TREE_ARG.equals(param)) {
        typedParameters.add(OBContext.getOBContext().getReadableOrganizations());
      } else {
        typedParameters.add(paramValue);
      }
    }
    return localWhereClause;
  }

  private String setRequestParameters(String currentWhereClause) {
    // no parameters
    if (!currentWhereClause.contains(DataEntityQueryService.PARAM_DELIMITER)) {
      return currentWhereClause;
    }
    String localWhereClause = currentWhereClause;
    for (String key : filterParameters.keySet()) {
      if (!key.startsWith(DataEntityQueryService.PARAM_DELIMITER)
          || !key.endsWith(DataEntityQueryService.PARAM_DELIMITER)) {
        continue;
      }
      int index = localWhereClause.toLowerCase().indexOf(key.toLowerCase());
      if (index != -1) {
        while (index != -1) {
          final Object value = filterParameters.get(key);
          // substitute all occurrences of paramater
          localWhereClause = localWhereClause.substring(0, index) + getTypedParameterAlias() + " "
              + localWhereClause.substring(index + key.length());
          typedParameters.add("null".equals(value) ? null : value);
          index = localWhereClause.toLowerCase().indexOf(key.toLowerCase());
        }
      }
    }

    return localWhereClause;
  }

  private String getTypedParameterAlias() {
    return ":" + ALIAS_PREFIX + (typedParameters.size() + aliasOffset);
  }

  /**
   * @return an empty String if there is no join clause, in other cases a String like the following
   *         is returned " as e left join e.bank as alias_1"
   */
  public String getJoinClause() {
    if (joinClause != null) {
      return joinClause;
    }

    // create join definitions for all many-to-ones
    if (joinAssociatedEntities) {
      for (Property property : entity.getProperties()) {
        if (!property.isPrimitive() && !property.isOneToMany() && !property.isOneToOne()) {
          final JoinDefinition joinDefinition = new JoinDefinition();
          joinDefinition.setOwnerAlias(getMainAlias());
          joinDefinition.setFetchJoin(true);
          joinDefinition.setProperty(property);
          joinDefinitions.add(joinDefinition);
        }
      }
    }

    // make sure that the join clauses are computed
    getOrderByClause();
    getWhereClause();

    final StringBuilder sb = new StringBuilder();
    if (getMainAlias() != null) {
      sb.append(" as " + getMainAlias() + " ");
    }
    for (JoinDefinition joinDefinition : joinDefinitions) {
      sb.append(joinDefinition.getJoinStatement());
    }
    sb.append(" ");
    joinClause = sb.toString();
    return joinClause;
  }

  /**
   * Converts the value of the sortBy member into a valid order by clause in a HQL query. The method
   * handles special cases as sorting by the identifier properties and descending which is
   * controlled with a minus sign before the property name.
   * 
   * @return a valid order by clause (or an empty string if no sorting)
   */
  public String getOrderByClause() {
    if (orderByClause != null) {
      return orderByClause;
    }
    if (orderBy == null || orderBy.trim().length() == 0) {
      orderByClause = "";
      return orderByClause;
    }
    final StringBuilder sb = new StringBuilder();
    boolean firstElement = true;
    int columnsInDescending = getNumberOfDescendingColumns();

    int totalColumnSeperators = StringUtils.countMatches(orderBy, ",");
    boolean orderPrimaryKeyInDesc = false;
    if (columnsInDescending == totalColumnSeperators) {
      orderPrimaryKeyInDesc = true;
    }

    for (String localOrderBy : orderBy.split(",")) {
      if (orderPrimaryKeyInDesc && localOrderBy.equals("id")) {
        localOrderBy = "-".concat(localOrderBy);
      }
      if (!firstElement) {
        sb.append(",");
      }
      sb.append(
          getOrderByClausePart(localOrderBy.trim().replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT)));
      firstElement = false;
    }

    // no order by elements, just use empty string
    if (sb.toString().trim().length() == 0) {
      orderByClause = "";
    } else {
      orderByClause = " order by " + sb.toString();
    }
    return orderByClause;
  }

  private int getNumberOfDescendingColumns() {
    // supports both -columnName...
    int columnsInDescending = StringUtils.countMatches(orderBy, "-");

    // ... and columnName desc/DESC
    Pattern p = Pattern.compile(" (desc|DESC)([^a-zA-Z0-9]|$)");
    Matcher m = p.matcher(orderBy);
    while (m.find()) {
      columnsInDescending++;
    }
    return columnsInDescending;
  }

  protected String getOrderByClausePart(String orderByParam) {
    // Support for one argument functions
    // Supports descending order, both -functionName(param), functionName(param) desc and
    // functionName(param) DESC
    String functionPattern = "(?<leftSign>-)?(?<functionName>.*)\\((?<paramName>.*)\\)( (?<rightSign>desc|DESC))?";
    Pattern p = Pattern.compile(functionPattern);
    Matcher m = p.matcher(orderByParam);

    String localOrderBy = null;
    String functionName = null;
    boolean descOrderedFunction = false;
    if (m.find()) {
      // If it is a function, retrieve the function name and the localOrderBy
      functionName = m.group("functionName");
      localOrderBy = m.group("paramName");
      // Check if the property is to be ordered in descending order
      if (m.group("leftSign") != null || m.group("rightSign") != null) {
        descOrderedFunction = true;
      }
    } else {
      localOrderBy = orderByParam;
    }

    final boolean asc = !localOrderBy.startsWith("-");
    String direction = "";
    if (!asc) {
      localOrderBy = localOrderBy.substring(1);
      direction = " desc ";
    }

    final List<String> paths = new ArrayList<String>();

    // handle the following case:
    // table.window.identifier as the sort string
    boolean isIdentifier = localOrderBy.equals(JsonConstants.IDENTIFIER)
        || localOrderBy.endsWith(DalUtil.DOT + JsonConstants.IDENTIFIER);
    Property originalProp = null;
    if (isIdentifier) {
      Entity searchEntity = getEntity();
      // a path to an entity, find the last entity
      String prefix;
      if (!localOrderBy.equals(JsonConstants.IDENTIFIER)) {
        // be lazy get the last property, it belongs to the last entity
        final Property prop = DalUtil.getPropertyFromPath(searchEntity, localOrderBy);
        Check.isNotNull(prop,
            "Property path " + localOrderBy + " is not valid for entity " + searchEntity);
        searchEntity = prop.getEntity();
        prefix = localOrderBy.substring(0, localOrderBy.lastIndexOf(DalUtil.DOT) + 1);

        String originalPropName = localOrderBy.replace(DalUtil.DOT + JsonConstants.IDENTIFIER, "");
        originalProp = DalUtil.getPropertyFromPath(getEntity(), originalPropName);
        if (originalProp.isComputedColumn()) {
          prefix += Entity.COMPUTED_COLUMNS_PROXY_PROPERTY + DalUtil.DOT + prefix;
        }

      } else {
        prefix = "";
      }
      boolean tableReference = false;
      if (originalProp == null) {
        if (distinctProperty != null) {
          tableReference = distinctProperty.getDomainType() instanceof TableDomainType;
          if (tableReference) {
            originalProp = distinctProperty;
          }
        }
      } else {
        tableReference = originalProp.getDomainType() instanceof TableDomainType;
      }

      if (tableReference) {
        // special case table reference itself
        final boolean isTable = originalProp.getEntity() == ModelProvider.getInstance()
            .getEntity(Table.ENTITY_NAME);
        Property useProperty = null;
        if (isTable) {
          useProperty = originalProp.getEntity().getProperty(Table.PROPERTY_NAME);
        } else {
          // read the reference to get the table reference
          final Reference reference = OBDal.getInstance()
              .get(Reference.class, originalProp.getDomainType().getReference().getId());
          for (ReferencedTable referencedTable : reference.getADReferencedTableList()) {
            if (referencedTable.isActive() && referencedTable.getDisplayedColumn() != null
                && referencedTable.getDisplayedColumn().isActive()) {
              useProperty = originalProp.getTargetEntity()
                  .getPropertyByColumnName(referencedTable.getDisplayedColumn().getDBColumnName());
              break;
            }
          }
        }
        if (!useProperty.isPrimitive()) {
          final Entity targetEntity = useProperty.getTargetEntity();
          for (Property targetEntityProperty : targetEntity.getIdentifierProperties()) {
            paths
                .add(prefix + useProperty.getName() + DalUtil.DOT + targetEntityProperty.getName());
          }
        } else {
          paths.add(prefix + useProperty.getName());
        }
      } else {
        for (Property prop : searchEntity.getIdentifierProperties()) {
          if (prop.isOneToMany()) {
            // not supported ignoring it
            continue;
          }
          if (!prop.isPrimitive()) {
            // get identifier properties from target entity
            // TODO: currently only supports one level, recursive
            // calls have the danger of infinite loops in case of
            // wrong identifier definitions in the AD
            final Entity targetEntity = prop.getTargetEntity();
            for (Property targetEntityProperty : targetEntity.getIdentifierProperties()) {
              paths.add(prefix + prop.getName() + DalUtil.DOT + targetEntityProperty.getName());
            }
          } else {
            paths.add(prefix + prop.getName());
          }
        }
      }
    } else {
      paths.add(localOrderBy);
    }

    final StringBuilder sb = new StringBuilder();
    boolean addComma = false;
    for (String path : paths) {
      if (addComma) {
        sb.append(", ");
      }
      addComma = true;

      final String[] orderByExpression = path.split(" "); // e.property DESC
      if (orderByExpression.length > 1) {
        path = orderByExpression[0];
        direction = " " + orderByExpression[1] + " ";
      }
      List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), path);
      // decide whether the entity joined for sorting have to use a left join or an inner join
      final String resolvedPath = resolveJoins(properties, path, canUseInnerJoin(properties));
      sb.append(resolvedPath);
      sb.append(direction);
    }

    String orderByClausePart = sb.toString();
    if (functionName != null) {
      orderByClausePart = functionName + "(" + orderByClausePart + ")";
      if (descOrderedFunction) {
        orderByClausePart = orderByClausePart + " desc";
      }
    }
    return orderByClausePart;
  }

  // When joining DB tables, this method can be used to check if the joining property of the
  // leftmost table is mandatory. In that case, an inner join would not discard any record of that
  // table. This means that the left join can be safely replaced with inner join.
  private boolean canUseInnerJoin(List<Property> properties) {
    if (properties.size() == 0) {
      return false;
    }
    for (Property property : properties) {
      if (!property.isPrimitive() && !property.isMandatory()) {
        return false;
      }
    }
    return true;
  }

  // Creates a Hibernate concatenation if there are multiple identifierproperties
  // note prefix includes the dot at the end
  private String createIdentifierLeftClause(List<Property> identifierProperties, String prefix) {
    final StringBuilder sb = new StringBuilder();
    for (Property prop : identifierProperties) {
      if (sb.length() > 0) {
        sb.append(" || '" + IdentifierProvider.SEPARATOR + "' || ");
      }
      // note to_char is added to handle null values correctly
      if (prop.getReferencedProperty() == null) {
        if (prop.isTranslatable() && OBContext.hasTranslationInstalled()) {
          // HQL for trl properties. Doing it as a select because it cannot be done as left join.
          // Example:
          //
          // select coalesce(w.name, t.name)
          // from ADWindow w left join w.aDWindowTrlList as t with t.language = :lang
          // where w.id=:window
          //
          // raises: with clause can only reference columns in the driving table

          sb.append("COALESCE(to_char((select " + prop.getTranslationProperty().getName() + " from "
              + prop.getTranslationProperty().getEntity().getName() + " as t where t."
              + prop.getTrlParentProperty().getName() + " = "
              + prefix.substring(0, prefix.lastIndexOf('.')) + " and t.language.language='"
              + OBContext.getOBContext().getLanguage().getLanguage() + "')), to_char("
              + replaceValueWithJoins(prefix + prop.getName()) + "), '')");
        } else if (prop.isMandatory() && prop.isTextualType()) {
          // if the property is mandatory there is no need to use coalesce to replace a possible
          // null value with an empty string
          // getting rid of the coalesce and to_char functions allow under certain circumstances
          // to use indexes defined on that property
          sb.append(replaceValueWithJoins(prefix + prop.getName()));
        } else {
          sb.append("COALESCE(to_char(" + replaceValueWithJoins(prefix + prop.getName()) + "),'')");

        }

      } else {
        final List<Property> newIdentifierProperties = prop.getReferencedProperty()
            .getEntity()
            .getIdentifierProperties();

        String newPrefix = prefix + prop.getName();

        if (prop.allowNullValues()) {

          boolean addJoin = true;

          // Look if the property has been joined
          for (JoinDefinition joinableDefinition : joinDefinitions) {
            if (joinableDefinition.property == prop) {
              addJoin = false;

              // Update newPrefix with the alias of the joinDefinition
              newPrefix = joinableDefinition.joinAlias;
              break;
            }
          }

          if (addJoin) {
            // Add join if this property allows null values
            final JoinDefinition joinDefinition = new JoinDefinition();
            joinDefinition.setOwnerAlias(prefix.substring(0, prefix.length() - 1));
            joinDefinition.setProperty(prop);
            joinDefinitions.add(joinDefinition);
          }
        }
        if (prop.isMandatory()) {
          sb.append(createIdentifierLeftClause(newIdentifierProperties, newPrefix + DalUtil.DOT));
        } else {
          sb.append("COALESCE(to_char("
              + createIdentifierLeftClause(newIdentifierProperties, newPrefix + DalUtil.DOT)
              + "),'')");
        }
      }
    }

    return "(" + sb.toString() + ")";
  }

  /*
   * To handle cases where joins are formed but the property path is used to compare with values.
   * For eg., instead of join_4.description, e.product.attributeSetValue.description is used in
   * where clause which results in exception when null objects are sub referenced.Refer issue
   * https://issues.openbravo.com/view.php?id=22007
   */
  private String replaceValueWithJoins(String value) {
    String query = value;
    String compare = value.substring(0, value.lastIndexOf(DalUtil.DOT));
    String properties[] = value.split("\\.");
    if (properties.length > 2) {
      for (JoinDefinition join : joinDefinitions) {
        if (compare.startsWith(getMainAlias())) {
          if (compare.equalsIgnoreCase(getMainAlias() + DalUtil.DOT + join.property.toString())) {
            query = join.joinAlias + DalUtil.DOT + properties[properties.length - 1];
          }

        } else {
          String joinStatement = join.getJoinStatement();
          String[] joinElement = joinStatement.split("as");
          if (joinElement[0] != null) {
            String entities[] = joinElement[0].split(" ");
            if (entities[entities.length - 1] != null) {
              String entityToCompare = entities[entities.length - 1];
              if (compare.equalsIgnoreCase(entityToCompare)) {
                query = join.joinAlias + DalUtil.DOT + properties[properties.length - 1];
              }
            }
          }
        }
      }
    }
    return query;
  }

  /*
   * To handle cases where if the select part contains a summary function, the query builder fails
   * as the join values are not properly replaced. Refer
   * https://issues.openbravo.com/view.php?id=25008
   */
  private String replaceJoinsWithValue(String value) {
    String query = value, joinValue = null;
    if (value.contains("join")) {
      joinValue = value.substring(0, value.indexOf("."));
      for (JoinDefinition joinDefinition : joinDefinitions) {
        if (joinDefinition.joinAlias.equals(joinValue)) {
          String string = joinDefinition.property.toString();
          string = string.substring(string.indexOf(".") + 1, string.length());
          query = getMainAlias() + DalUtil.DOT + string + DalUtil.DOT
              + value.substring(value.indexOf(".") + 1, value.length());
          break;
        }
      }
    }
    return query;
  }

  /**
   * @return true if one of the filter parameters is the {@link JsonConstants#ORG_PARAMETER}.
   */
  public boolean hasOrganizationParameter() {
    final String value = filterParameters.get(JsonConstants.ORG_PARAMETER);
    return value != null && value.trim().length() > 0;
  }

  /**
   * Add a filter parameter, the method {@link #getWhereClause()} will try to convert the String
   * value to a typed parameter.
   * 
   * @param key
   *          the filter key, can be direct property or a referenced property.
   * @param value
   *          the value as a String
   */
  public void addFilterParameter(String key, String value) {
    // ignore these
    if (value == null) {
      return;
    }
    whereClause = null;
    typedParameters.clear();
    filterParameters.put(key, value);
  }

  public Map<String, Object> getNamedParameters() {
    final Map<String, Object> parameters = new HashMap<String, Object>();
    for (int i = 0; i < typedParameters.size(); i++) {
      parameters.put(ALIAS_PREFIX + Integer.toString(i), typedParameters.get(i));
    }
    return parameters;
  }

  public void setDoOr(boolean doOr) {
    if (doOr) {
      orNesting++;
    }
    // in case of join always do outer joining
    setMainAlias(JsonConstants.MAIN_ALIAS);
  }

  public String resolveJoins(List<Property> props, String originalPath) {
    // by default use fromCriteria = false. that way if resolveJoins is called without the
    // fromCriteria parameter, a 'left join' will be used, which is the previous standard behavior
    return resolveJoins(props, originalPath, false);
  }

  // Resolves the list of properties against existing join definitions
  // creates new join definitions when necessary
  public String resolveJoins(List<Property> props, String originalPath, boolean fromCriteria) {
    String alias = getMainAlias();
    if (alias == null) {
      return originalPath;
    }
    int index = 0;
    int joinedPropertyIndex = -1;
    for (Property prop : props) {
      boolean found = false;
      for (JoinDefinition joinDefinition : joinDefinitions) {
        if (joinDefinition.appliesTo(alias, prop)) {
          if (!fromCriteria) {
            useInnerJoinMap.put(prop.getName(), Boolean.FALSE);
          }
          alias = joinDefinition.getJoinAlias();
          joinedPropertyIndex = index;
          found = true;
          break;
        }
      }
      if (!found) {
        // no more joins, leave
        break;
      }
      index++;
    }
    // check if any new JoinDefinitions should be created
    for (int i = (joinedPropertyIndex + 1); i < props.size(); i++) {
      final Property prop = props.get(i);
      if (prop.isPrimitive()) {
        break;
      }
      // a joinable property
      final JoinDefinition joinDefinition = new JoinDefinition();
      joinDefinition.setOwnerAlias(alias);
      joinDefinition.setProperty(prop);
      if (fromCriteria) {
        useInnerJoinMap.put(prop.getName(), Boolean.TRUE);
      } else {
        useInnerJoinMap.put(prop.getName(), Boolean.FALSE);
      }
      joinDefinitions.add(joinDefinition);

      // move the result up to use the new JoinDefinition
      alias = joinDefinition.getJoinAlias();
      joinedPropertyIndex = i;
    }
    if (joinedPropertyIndex == (props.size() - 1)) {
      return alias;
    }
    Property prop = props.get(props.size() - 1);
    String propName = null;
    if (props.get(0).isComputedColumn()) {
      propName = Entity.COMPUTED_COLUMNS_PROXY_PROPERTY;
      for (Property p : props) {
        propName += DalUtil.DOT + p.getName();
      }
    } else {
      propName = prop.getName();
    }

    return alias + DalUtil.DOT + propName;
  }

  private String getNewUniqueJoinAlias() {
    return JOIN_ALIAS_PREFIX + (aliasIndex++);
  }

  private class JoinDefinition {
    private Property property;
    private String joinAlias = getNewUniqueJoinAlias();
    private String ownerAlias;
    private boolean fetchJoin = AdvancedQueryBuilder.this.isJoinAssociatedEntities();

    public boolean appliesTo(String checkAlias, Property checkProperty) {
      return checkAlias.equals(ownerAlias) && checkProperty == property;
    }

    public String getJoinStatement() {
      String propName;
      if (property.isComputedColumn()) {
        propName = Entity.COMPUTED_COLUMNS_PROXY_PROPERTY + DalUtil.DOT + property.getName();
      } else {
        propName = property.getName();
      }
      if (orNesting > 0) {
        return " left outer join " + (fetchJoin ? "fetch " : "")
            + (ownerAlias != null ? ownerAlias + DalUtil.DOT : "") + propName + " as " + joinAlias;
      } else {
        String joinType = null;
        // if all the identifier properties of the target entity are mandatory, and if the joined
        // entity is used only in where clauses resulting from filtering the grid, an inner join can
        // be used
        if (Entity.COMPUTED_COLUMNS_PROXY_PROPERTY.equals(property.getName())
            || KernelUtils.hasNullableIdentifierProperties(property.getTargetEntity())
            || !(Boolean.TRUE.equals(useInnerJoinMap.get(property.getName())))) {
          joinType = " left join ";
        } else {
          joinType = " inner join ";
        }
        return joinType + (fetchJoin ? "fetch " : "")
            + (ownerAlias != null ? ownerAlias + DalUtil.DOT : "") + propName + " as " + joinAlias;
      }
    }

    public void setProperty(Property property) {
      this.property = property;
    }

    public String getJoinAlias() {
      return joinAlias;
    }

    public void setOwnerAlias(String ownerAlias) {
      this.ownerAlias = ownerAlias;
    }

    public void setFetchJoin(boolean fetchJoin) {
      this.fetchJoin = fetchJoin;
    }
  }

  public String getMainAlias() {
    return mainAlias;
  }

  public void setMainAlias(String mainAlias) {
    this.mainAlias = mainAlias;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
    // do outer joining if the order by has more than 1 dot
    if (orderBy.indexOf(DalUtil.DOT) != -1
        && orderBy.indexOf(DalUtil.DOT) != orderBy.lastIndexOf(DalUtil.DOT)) {
      setMainAlias(JsonConstants.MAIN_ALIAS);
    }
  }

  private String escapeLike(String value) {
    if (value == null || value.trim().length() == 0) {
      return value;
    }
    String escapeChar = "|";
    String localValue = value.replace(escapeChar + "", escapeChar + escapeChar + "");
    localValue = localValue.replace("_", ESCAPE_CHAR + "_");
    localValue = localValue.replace("%", ESCAPE_CHAR + "%");
    return localValue;
  }

  public JSONObject getCriteria() {
    return criteria;
  }

  public void setCriteria(JSONObject criteria) {
    this.criteria = criteria;
    whereClause = null;
    aliasIndex = 0;
    orNesting = 0;
    typedParameters = new ArrayList<>();
  }

  public boolean isJoinAssociatedEntities() {
    return joinAssociatedEntities;
  }

  public void setJoinAssociatedEntities(boolean joinAssociatedEntities) {
    this.joinAssociatedEntities = joinAssociatedEntities;
    if (joinAssociatedEntities) {
      // force an alias then
      setMainAlias(JsonConstants.MAIN_ALIAS);
    }
  }

  public List<String> getAdditionalProperties() {
    return additionalProperties;
  }

  public void clearCachedValues() {
    joinClause = null;
    whereClause = null;
    orderByClause = null;
    joinDefinitions.clear();
    typedParameters.clear();
  }

  public void addSelectFunctionPart(String function, String field) {
    if ("count".equals(function)) {
      selectClauseParts.add(function + "(*)");
    } else {
      String localField = field;
      List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), localField);
      properties = getPropertyForTableReference(properties);
      boolean fromCriteria = false;
      localField = resolveJoins(properties, localField, fromCriteria);
      if (properties.size() > 0) {
        final Property lastProperty = properties.get(properties.size() - 1);
        if (lastProperty.getTargetEntity() != null) {
          final StringBuilder sb = new StringBuilder();
          for (Property identifierProperty : lastProperty.getTargetEntity()
              .getIdentifierProperties()) {
            if (sb.length() > 0) {
              sb.append(" + ");
            }
            sb.append(localField + "." + identifierProperty.getName());
          }
          localField = sb.toString();
        }
      }
      // for select clause with functions replace the joins before so that the join values are not
      // lost later. Refer issue https://issues.openbravo.com/view.php?id=25008
      localField = replaceJoinsWithValue(localField);
      selectClauseParts.add(function + "(" + localField + ")");
    }
  }

  public void addSelectClausePart(String selectClausePart) {
    String localSelectClausePart = selectClausePart;
    List<Property> properties = JsonUtils.getPropertiesOnPath(getEntity(), localSelectClausePart);
    properties = getPropertyForTableReference(properties);
    boolean fromCriteria = false;
    localSelectClausePart = resolveJoins(properties, localSelectClausePart, fromCriteria);
    selectClauseParts.add(localSelectClausePart);
  }

  public String getSelectClause() {
    final StringBuilder sb = new StringBuilder();
    for (String selectClausePart : selectClauseParts) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(selectClausePart);
    }
    return sb.toString();
  }

  public void setAdditionalProperties(List<String> additionalProperties) {
    this.additionalProperties = additionalProperties;
  }

  public void setSubEntityName(String subEntityName) {
    this.subEntity = ModelProvider.getInstance().getEntity(subEntityName);
  }

  public void setSubDataEntityQueryService(DataEntityQueryService dataEntityQueryService) {
    this.subDataEntityQueryService = dataEntityQueryService;

  }

  public void setDistinctProperty(Property distinctProperty) {
    this.distinctProperty = distinctProperty;
  }

  void setDistinctPropertyPath(String distinctPropertyPath) {
    this.distinctPropertyPath = distinctPropertyPath;
  }

  /**
   * Returns the appropriate display column property for table references instead of the identifier
   * properties. Used in cases when filtering data in grid, based on the data of table reference.
   * 
   * @param properties
   * @return properties with the proper display column property
   */
  private List<Property> getPropertyForTableReference(List<Property> properties) {
    if (properties.isEmpty()) {
      return properties;
    }
    Property property = properties.get(properties.size() - 1);
    boolean tableReference = false;
    if (properties.size() >= 2) {
      final Property refProperty = properties.get(properties.size() - 2);
      tableReference = refProperty.getDomainType() instanceof TableDomainType;
      if (tableReference) {
        // special case table reference itself
        final boolean isTable = property.getEntity() == ModelProvider.getInstance()
            .getEntity(Table.ENTITY_NAME);
        if (isTable) {
          property = property.getEntity().getProperty(Table.PROPERTY_NAME);
        } else {
          // read the reference to get the table reference
          final Reference reference = OBDal.getInstance()
              .get(Reference.class, refProperty.getDomainType().getReference().getId());
          for (ReferencedTable referencedTable : reference.getADReferencedTableList()) {
            if (referencedTable.isActive() && referencedTable.getDisplayedColumn() != null
                && referencedTable.getDisplayedColumn().isActive()) {
              property = property.getEntity()
                  .getPropertyByColumnName(referencedTable.getDisplayedColumn().getDBColumnName());
              break;
            }
          }
        }
      }
    }
    properties.set(properties.size() - 1, property);
    return properties;
  }

  /**
   * Allows preventing the creation of new join alias when the where clause is built. This is useful
   * when the AdvancedQueryBuilder is used to obtain the WHERE clause, but when the FROM clause
   * built is not used (i.e. en HQLDataSourceService)
   * 
   * @param preventedCreatingJoinsInWhereClause
   *          If true, the creation of new join alias is prevented when the where clause is built
   * 
   */
  public void preventCreatingJoinsInWhereClause(boolean preventedCreatingJoinsInWhereClause) {
    this.creatingJoinsInWhereClauseIsPrevented = preventedCreatingJoinsInWhereClause;
  }
}
