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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.datasource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.IdentifierProvider;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.client.kernel.reference.EnumUIDefinition;
import org.openbravo.client.kernel.reference.IDUIDefinition;
import org.openbravo.client.kernel.reference.NumberUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.client.kernel.reference.YesNoUIDefinition;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.datasource.hql.HQLInserterQualifier;
import org.openbravo.service.datasource.hql.HqlInserter;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;
import org.openbravo.service.json.AdvancedQueryBuilder;
import org.openbravo.service.json.JsonConstants;
import org.openbravo.service.json.JsonUtils;

public class HQLDataSourceService extends ReadOnlyDataSourceService {
  public static final String PROPERTY_FIELD_SEPARATOR = "___";

  private static final Logger log = LogManager.getLogger();
  private static final String AND = " AND ";
  private static final String WHERE = " WHERE ";
  private static final String ORDERBY = " ORDER BY ";
  private static final String GROUPBY = "GROUP BY";
  private static final String MAIN_FROM = "MAINFROM";
  private static final String FROM = "FROM ";
  private static final String ADDITIONAL_FILTERS = "@additional_filters@";
  private static final String INSERTION_POINT_GENERIC_ID = "@insertion_point_#@";
  private static final String INSERTION_POINT_INDEX_PLACEHOLDER = "#";
  private static final String DUMMY_INSERTION_POINT_REPLACEMENT = " 1 = 1 ";

  @Inject
  @Any
  private Instance<HqlInserter> hqlInserters;

  @Inject
  @Any
  private Instance<HqlQueryTransformer> hqlQueryTransformers;

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Override
  public List<DataSourceProperty> getDataSourceProperties(Map<String, Object> parameters) {
    // Returns the datasource properties, based on the columns of the table that is going to use the
    // datasource
    // This is needed to support client side filtering
    List<DataSourceProperty> dataSourceProperties = new ArrayList<>();
    String tableId = (String) parameters.get("tableId");
    if (tableId != null) {
      Table table = adcs.getTable(tableId);
      Entity entity = ModelProvider.getInstance().getEntityByTableId(tableId);
      for (Column column : table.getADColumnList()) {
        final DataSourceProperty dsProperty = new DataSourceProperty();
        Property property = entity.getPropertyByColumnName(column.getDBColumnName());
        dsProperty.setName(property.getName());
        dsProperty.setMandatory(column.isMandatory());
        dsProperty.setUpdatable(column.isUpdatable());
        Reference reference = column.getReference();
        final UIDefinition uiDefinition = UIDefinitionController.getInstance()
            .getUIDefinition(reference);
        if (uiDefinition instanceof IDUIDefinition) {
          dsProperty.setId(true);
          dsProperty.setName("id");
        } else {
          dsProperty.setId(false);
        }
        dsProperty.setBoolean(uiDefinition instanceof YesNoUIDefinition);
        dsProperty.setPrimitive(uiDefinition.getDomainType() instanceof PrimitiveDomainType);
        dsProperty.setUIDefinition(uiDefinition);
        if (dsProperty.isPrimitive()) {
          dsProperty.setPrimitiveObjectType(
              ((PrimitiveDomainType) uiDefinition.getDomainType()).getPrimitiveType());
          dsProperty.setNumericType(uiDefinition instanceof NumberUIDefinition);
          if (uiDefinition instanceof EnumUIDefinition) {
            Set<String> allowedValues = DataSourceProperty
                .getAllowedValues(column.getReferenceSearchKey());
            dsProperty.setAllowedValues(allowedValues);
            dsProperty.setValueMap(DataSourceProperty.createValueMap(allowedValues,
                column.getReferenceSearchKey().getId()));
          }
        }
        dataSourceProperties.add(dsProperty);
      }
    }
    return dataSourceProperties;
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    final OBContext obContext = OBContext.getOBContext();
    Table table = getTableFromParameters(parameter);
    try {
      Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
      if (entity != null) {
        obContext.getEntityAccessChecker().checkReadableAccess(entity);
      }
    } catch (OBSecurityException e) {
      handleExceptionUnsecuredDSAccess(e);
    }
  }

  @Override
  protected int getCount(Map<String, String> parameters) {
    Table table = getTableFromParameters(parameters);
    boolean justCount = true;
    Query<Tuple> countQuery = getQuery(table, parameters, justCount);
    String hqlQuery = countQuery.getQueryString();
    int nRows = -1;
    if (hqlQuery.toUpperCase().contains(GROUPBY)) {
      justCount = false;
      countQuery = getQuery(table, parameters, justCount);
      return getGroupedCount(countQuery);
    } else {
      nRows = ((Number) countQuery.uniqueResult().get(0)).intValue();
    }
    return nRows;
  }

  protected int getGroupedCount(Query<Tuple> countQuery) {
    int nRows = -1;
    ScrollableResults scrollableResults = countQuery.scroll();
    if (scrollableResults.last()) {
      nRows = scrollableResults.getRowNumber();
    }
    scrollableResults.close();
    return nRows + 1;
  }

  @Override
  protected List<Map<String, Object>> getData(Map<String, String> parameters, int startRow,
      int endRow) {

    Table table = getTableFromParameters(parameters);
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    OBContext.setAdminMode(true);
    boolean justCount = false;
    Query<Tuple> query = getQuery(table, parameters, justCount);

    if (startRow > 0) {
      query.setFirstResult(startRow);
    }
    if (endRow > startRow) {
      query.setMaxResults(endRow - startRow + 1);
    }

    String distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);
    List<Column> columns = table.getADColumnList();
    List<Map<String, Object>> data = new ArrayList<>();
    for (Tuple tuple : query.list()) {
      Map<String, Object> record = new HashMap<>();
      if (distinct != null) {
        // the whole referenced BaseOBObject is stored in the first position of the result
        BaseOBObject bob = (BaseOBObject) tuple.get(0);
        if (bob == null) {
          break;
        }
        record.put(JsonConstants.ID, bob.getId());
        record.put(JsonConstants.IDENTIFIER, IdentifierProvider.getInstance().getIdentifier(bob));
      } else {
        int i = 0;
        for (TupleElement<?> tupleElement : tuple.getElements()) {
          String alias = tupleElement.getAlias();

          // Aliases should be properly set in HQL, column name can be used only as fallback in case
          // alias is not set or it does not match any actual property. Even in this case it will
          // only work in case columns in HQL are in the same order as defined by AD_Column.seqNo.
          String columnName = columns.size() > i ? columns.get(i).getDBColumnName() : null;

          String propertyName = getPropertyName(entity, alias, columnName);
          record.put(propertyName, alias != null ? tuple.get(alias) : tuple.get(i));
          i++;
        }
      }
      data.add(record);
    }
    OBContext.restorePreviousMode();
    return data;
  }

  private String getPropertyName(Entity entity, String alias, String columnName) {
    if (alias == null) {
      return entity.getPropertyByColumnName(columnName.toLowerCase()).getName();
    }
    if (alias.contains(PROPERTY_FIELD_SEPARATOR)) {
      return alias.replace(PROPERTY_FIELD_SEPARATOR, JsonConstants.FIELD_SEPARATOR);
    }
    boolean checkIsNotNull = false;
    Property property = entity.getPropertyByColumnName(alias.toLowerCase(), checkIsNotNull);
    if (property == null) {
      property = entity.getPropertyByColumnName(columnName.toLowerCase());
    }
    return property.getName();
  }

  /**
   * Returns the HQL table whose data is being fetched. It will be obtained either using the table
   * id, or the tab id
   * 
   * @param parameters
   *          the parameters sent in the fetch request
   * @return the table whose data is being fetched
   */
  private Table getTableFromParameters(Map<String, String> parameters) {
    Table table = null;
    try {
      OBContext.setAdminMode(true);
      String tableId = parameters.get("tableId");
      String tabId = parameters.get("tabId");
      if (tableId != null) {
        table = OBDal.getInstance().get(Table.class, tableId);
      } else if (tabId != null) {
        Tab tab = null;
        tab = OBDal.getInstance().get(Tab.class, tabId);
        table = tab.getTable();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return table;
  }

  /**
   * Returns a hibernate query object based on the hql query, if the justCount parameter is true,
   * the query will just return the number of records that fulfill the criteria. If justCount is
   * false, the query will return all the actual records that fulfill the criteria
   */
  private Query<Tuple> getQuery(Table table, Map<String, String> parameters, boolean justCount) {
    OBContext.setAdminMode(true);
    String hqlQuery = table.getHqlQuery();
    // obtains the where clause from the criteria, using the AdvancedQueryBuilder
    JSONObject criteria = JsonUtils.buildCriteria(parameters);
    AdvancedQueryBuilder queryBuilder = new AdvancedQueryBuilder();
    queryBuilder.setEntity(ModelProvider.getInstance().getEntityByTableId(table.getId()));
    queryBuilder.setCriteria(criteria);
    // don't create new join aliases in the where clause, as they would not be present in the
    // defined HQL FROM clause
    queryBuilder.preventCreatingJoinsInWhereClause(true);
    if (table.getEntityAlias() != null) {
      queryBuilder.setMainAlias(table.getEntityAlias());
    }
    String whereClause = queryBuilder.getWhereClause();
    // replace the property names with the column alias
    whereClause = replaceParametersWithAlias(table, whereClause);

    String distinct = parameters.get(JsonConstants.DISTINCT_PARAMETER);
    if (distinct != null) {
      String formClause = null;
      if (hqlQuery.indexOf(MAIN_FROM) != -1) {
        formClause = hqlQuery.substring(hqlQuery.indexOf(MAIN_FROM));
      } else {
        formClause = hqlQuery.substring(hqlQuery.toUpperCase().lastIndexOf(FROM));
      }
      Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
      Property property = entity.getProperty(distinct);
      Column distinctColumn = OBDal.getInstance().get(Column.class, property.getColumnId());
      // TODO: Improve distinct query like this: https://issues.openbravo.com/view.php?id=25182
      if (justCount) {
        hqlQuery = "select count(distinct " + distinctColumn.getEntityAlias() + "."
            + getNameOfFirstIdentifierProperty(property.getTargetEntity()) + ") " + formClause;
      } else {
        // Retrieve:
        // - the whole referenced object, so that later it is easier to obtain its id and
        // its identifier (which can be a translation)
        // - the first property of the entity's identifier. This is needed because it is the column
        // that will be used to order the rows
        hqlQuery = "select distinct " + distinctColumn.getEntityAlias() + ","
            + distinctColumn.getEntityAlias() + "."
            + getNameOfFirstIdentifierProperty(property.getTargetEntity()) + " " + formClause;
      }
    }
    // adds the additional filters (client, organization and criteria) to the query
    hqlQuery = addAdditionalFilters(table, hqlQuery, whereClause, parameters);

    boolean includeMainEntityID = true;
    if (hqlQuery.toUpperCase().contains(GROUPBY)) {
      // If the HQL Query contains a GROUP BY clause, the ID of the main entity should not be
      // included in the order by clause
      includeMainEntityID = false;
    }

    if (!justCount) {
      // adds the order by clause unless only the number of rows is needed
      String orderByClause = getSortByClause(parameters, includeMainEntityID);
      if (!orderByClause.isEmpty()) {
        hqlQuery = hqlQuery + orderByClause;
      }
    }
    parameters.put("_justCount", String.valueOf(justCount));

    Map<String, Object> queryNamedParameters = queryBuilder.getNamedParameters();

    // if the is any HQL Query transformer defined for this table, use it to transform the query
    hqlQuery = transFormQuery(hqlQuery, queryNamedParameters, parameters);

    // replaces the insertion points with injected code or with dummy comparisons
    // if the injected code includes named parameters for the query, they are stored in the
    // queryNamedParameters parameter
    hqlQuery = fillInInsertionPoints(hqlQuery, queryNamedParameters, parameters);

    if (distinct == null && justCount) {
      String formClause = null;
      if (hqlQuery.indexOf(MAIN_FROM) != -1) {
        formClause = hqlQuery.substring(hqlQuery.indexOf(MAIN_FROM));
      } else {
        formClause = hqlQuery.substring(hqlQuery.toUpperCase().lastIndexOf(FROM));
      }
      hqlQuery = "select count(*) " + formClause;
    }

    if (hqlQuery.indexOf(MAIN_FROM) != -1) {
      hqlQuery = hqlQuery.replace(MAIN_FROM, FROM);
    }

    log.debug("HQL query: {}", hqlQuery);
    Query<Tuple> query = OBDal.getInstance().getSession().createQuery(hqlQuery, Tuple.class);

    String paramsLog = "";

    // sets the named parameters of the query
    for (String key : queryNamedParameters.keySet()) {
      // Injection and transforms might have modified the query removing named parameters. Check
      // that key is still in the query.
      if (hqlQuery.contains(key)) {
        Object parameter = queryNamedParameters.get(key);
        if (parameter instanceof Collection<?>) {
          query.setParameterList(key, (Collection<?>) parameter);
        } else {
          query.setParameter(key, parameter);
        }
        if (log.isDebugEnabled()) {
          paramsLog += "\n" + key + ": " + parameter;
        }
      }
    }

    // Set the hql clientId and organization parameters of the query
    Set<String> namedParameters = query.getParameterMetadata().getNamedParameterNames();
    if (namedParameters.contains("clientId")) {
      query.setParameter("clientId", parameters.get("clientId"));
      parameters.remove("clientId");
    }
    if (namedParameters.contains("organizations")) {
      query.setParameterList("organizations",
          parameters.get("organizations").replaceAll("'", "").split(","));
      parameters.remove("organizations");
    }

    log.debug("  parameters:{}", paramsLog);

    OBContext.restorePreviousMode();
    return query;
  }

  /**
   * If the hql query has insertion points, resolve them using dependency injection. If some
   * insertion points are defined in the query but its definition is not injected, replace them with
   * dummy comparisons
   * 
   * @param hqlQuery
   *          hql query that might contain insertion points
   * @param queryNamedParameters
   *          array with the named paremeters that will be set to the query. At this point it is
   *          empty, is can be filled in by the insertion point implementators
   * @param parameters
   *          parameters of this request
   * @return the updated hql query. Also, hqlParameters can contain the named parameters used in the
   *         insertion points
   */
  private String fillInInsertionPoints(String hqlQuery, Map<String, Object> queryNamedParameters,
      Map<String, String> parameters) {
    String updatedHqlQuery = hqlQuery;
    int index = 0;
    while (existsInsertionPoint(hqlQuery, index)) {
      HqlInserter inserter = getHqlInserter(index, parameters);
      String insertedCode = null;
      if (inserter != null) {
        insertedCode = inserter.insertHql(parameters, queryNamedParameters);
      }
      if (insertedCode == null) {
        insertedCode = DUMMY_INSERTION_POINT_REPLACEMENT;
      }
      String insertionPointId = INSERTION_POINT_GENERIC_ID
          .replace(INSERTION_POINT_INDEX_PLACEHOLDER, Integer.toString(index));
      updatedHqlQuery = updatedHqlQuery.replace(insertionPointId, insertedCode);
      index++;
    }
    return updatedHqlQuery;
  }

  /**
   * If there is any HQL Query Transformer defined, uses its transformHqlQuery to transform the
   * query
   * 
   * @param hqlQuery
   *          the original HQL query
   * @param queryNamedParameters
   *          the named parameters that will be used in the query
   * @param parameters
   *          the parameters of the request
   * @return the transformed query
   */
  private String transFormQuery(String hqlQuery, Map<String, Object> queryNamedParameters,
      Map<String, String> parameters) {
    String transformedHqlQuery = hqlQuery;
    HqlQueryTransformer hqlQueryTransformer = getTransformer(parameters);
    if (hqlQueryTransformer != null) {
      transformedHqlQuery = hqlQueryTransformer.transformHqlQuery(transformedHqlQuery, parameters,
          queryNamedParameters);
    }
    return transformedHqlQuery;
  }

  /**
   * Returns, if defined, an HQL Query Transformer for this table. If the are several transformers
   * defined, the one with the lowest priority will be chosen
   * 
   * @param parameters
   *          the parameters of the request
   * @return the HQL Query transformer that will be used to transform the query
   */
  private HqlQueryTransformer getTransformer(Map<String, String> parameters) {
    HqlQueryTransformer transformer = null;
    Table table = getTableFromParameters(parameters);
    for (HqlQueryTransformer nextTransformer : hqlQueryTransformers
        .select(new ComponentProvider.Selector(table.getId()))) {
      if (transformer == null) {
        transformer = nextTransformer;
      } else if (nextTransformer.getPriority(parameters) < transformer.getPriority(parameters)) {
        transformer = nextTransformer;
      } else if (nextTransformer.getPriority(parameters) == transformer.getPriority(parameters)) {
        log.warn(
            "Trying to get hql query transformer for the table with id {}, there are more than one instance with same priority",
            table.getId());
      }
    }
    return transformer;
  }

  /**
   * Returns, if defined, an HQL inserter for the insertion point with index id
   * 
   * @param index
   *          the index of the insertion point
   * @param parameters
   *          the parameters of the request
   * @return the HQL inserter with the lowest priority for the insertion point
   * @insertion_point_<index>@
   */
  private HqlInserter getHqlInserter(int index, Map<String, String> parameters) {
    HqlInserter inserter = null;
    Table table = getTableFromParameters(parameters);
    for (HqlInserter inj : hqlInserters
        .select(new HQLInserterQualifier.Selector(table.getId(), Integer.toString(index)))) {
      if (inserter == null) {
        inserter = inj;
      } else if (inj.getPriority(parameters) < inserter.getPriority(parameters)) {
        inserter = inj;
      } else if (inj.getPriority(parameters) == inserter.getPriority(parameters)) {
        log.warn(
            "Trying to get hql inserter for the insertion point {} of the table with id {}, there are more than one instance with same priority",
            INSERTION_POINT_GENERIC_ID.replace(INSERTION_POINT_INDEX_PLACEHOLDER,
                Integer.toString(index)),
            table.getId());
      }
    }
    return inserter;
  }

  /**
   * Checks if the insertion point with id index exists in the provided hql query
   * 
   * @param hqlQuery
   *          hql query that can contain insertion points
   * @param index
   *          index of the insertion point
   * @return true if the hql query contains an insertion point with the provided index, false
   *         otherwise
   */
  private boolean existsInsertionPoint(String hqlQuery, int index) {
    String insertionPointId = INSERTION_POINT_GENERIC_ID.replace(INSERTION_POINT_INDEX_PLACEHOLDER,
        Integer.toString(index));
    return hqlQuery.contains(insertionPointId);
  }

  /**
   * This method replaces the column names with their alias
   * 
   * @param table
   *          the table being filtered
   * @param whereClause
   *          the filter criteria
   * @return an updated filter criteria that uses the alias of the columns instead of their names
   */
  private String replaceParametersWithAlias(Table table, String whereClause) {
    if (whereClause.trim().isEmpty()) {
      return whereClause;
    }
    String updatedWhereClause = whereClause;
    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    for (Column column : table.getADColumnList()) {
      // look for the property name, replace it with the column alias
      Property property = entity.getPropertyByColumnName(column.getDBColumnName());
      // Map used to replace the property name used in the criteria with its alias
      Map<String, String> replacementMap = new HashMap<>();
      String propertyNameBefore = null;
      String propertyNameAfter = null;
      if (property.isPrimitive()) {
        // if the property is a primitive, just replace the property name with the column alias
        propertyNameBefore = property.getName();
        propertyNameAfter = column.getEntityAlias();
        addEntryToReplacementMap(replacementMap, propertyNameBefore, propertyNameAfter,
            table.getEntityAlias());
      } else {
        // the criteria can refer to the foreign key via its ID...
        propertyNameBefore = property.getName() + "." + JsonConstants.ID;
        propertyNameAfter = column.getEntityAlias() + "." + JsonConstants.ID;
        addEntryToReplacementMap(replacementMap, propertyNameBefore, propertyNameAfter,
            table.getEntityAlias());
        // ... or through its identifier...
        Entity refEntity = property.getReferencedProperty().getEntity();
        String identifierPropertyName = refEntity.getIdentifierProperties().get(0).getName();
        propertyNameBefore = property.getName() + "." + identifierPropertyName;
        propertyNameAfter = column.getEntityAlias() + "." + identifierPropertyName;
        addEntryToReplacementMap(replacementMap, propertyNameBefore, propertyNameAfter,
            table.getEntityAlias());

        // ... or through its entity name
        propertyNameBefore = property.getName();
        propertyNameAfter = column.getEntityAlias();
        addEntryToReplacementMap(replacementMap, propertyNameBefore, propertyNameAfter,
            table.getEntityAlias());
      }
      for (Entry<String, String> replacement : replacementMap.entrySet()) {
        updatedWhereClause = updatedWhereClause.replaceAll(replacement.getKey(),
            replacement.getValue());
      }
    }
    return updatedWhereClause;
  }

  /**
   * Adds a pair oldName-newName to the replacement map All possible parenthesis combinations are
   * added to the replacement map
   */
  private void addEntryToReplacementMap(Map<String, String> replacementMap, String oldName,
      String newName, String mainAlias) {
    replacementMap.put(" " + oldName + " ", " " + newName + " ");
    replacementMap.put("[(]" + oldName + "[)]", "(" + newName + ")");
    replacementMap.put("[(]" + oldName + " ", "(" + newName + " ");
    replacementMap.put(" " + oldName + "[)]", " " + newName + ")");

    if (StringUtils.isNotEmpty(mainAlias)) {
      // if table has alias, add also replacements taking it into account
      addEntryToReplacementMap(replacementMap, mainAlias + "." + oldName, newName, null);
    }
  }

  /**
   * Adds the additional filters to the hql query. The additional filters include the client filter,
   * the organization filter and the filter created from the grid criteria
   * 
   * @param table
   *          table being fetched
   * @param hqlQuery
   *          hql query without the additional filters
   * @param filterWhereClause
   *          filter created from the grid criteria
   * @param parameters
   *          parameters used for this request
   */
  private String addAdditionalFilters(Table table, String hqlQuery, String filterWhereClause,
      Map<String, String> parameters) {
    OBContext.setAdminMode(true);
    final String entityAlias = table.getEntityAlias();

    // replace the carriage returns and the tabulations with blanks
    String hqlQueryWithFilters = hqlQuery.replace("\n", " ").replace("\r", " ");

    String additionalFilter = entityAlias + ".client.id in ('0', :clientId)";
    // client filter
    parameters.put("clientId", OBContext.getOBContext().getCurrentClient().getId());

    // organization filter
    final String orgs = DataSourceUtils.getOrgs(parameters.get(JsonConstants.ORG_PARAMETER));
    if (StringUtils.isNotEmpty(orgs)) {
      additionalFilter += AND + entityAlias + ".organization.id in ( :organizations )";
      parameters.put("organizations", orgs);
    }

    additionalFilter = addFilterWhereClause(additionalFilter, filterWhereClause);

    // the _where parameter contains the filter clause and the where clause defined at tab level
    String whereClauseParameter = parameters.get(JsonConstants.WHERE_AND_FILTER_CLAUSE);
    if (whereClauseParameter != null && !whereClauseParameter.trim().isEmpty()
        && !"null".equals(whereClauseParameter)) {
      additionalFilter += AND + whereClauseParameter;
    }

    if (hqlQueryWithFilters.contains(ADDITIONAL_FILTERS)) {
      // replace @additional_filters@ with the actual hql filters
      hqlQueryWithFilters = hqlQueryWithFilters.replace(ADDITIONAL_FILTERS, additionalFilter);
    } else {
      // adds the hql filters in the proper place at the end of the query
      String separator = null;
      if (StringUtils.containsIgnoreCase(hqlQueryWithFilters, WHERE)) {
        // if there is already a where clause, append with 'AND'
        separator = AND;
      } else {
        // otherwise, append with 'where'
        separator = WHERE;
      }
      hqlQueryWithFilters = hqlQueryWithFilters + separator + additionalFilter;
    }
    OBContext.restorePreviousMode();
    return hqlQueryWithFilters;
  }

  private String addFilterWhereClause(String additionalFilter, String filterWhereClause) {
    String filterWithWhereClause = additionalFilter;
    if (!filterWhereClause.trim().isEmpty()) {
      filterWithWhereClause += AND + removeLeadingWhere(filterWhereClause);
    }
    return filterWithWhereClause;
  }

  private String removeLeadingWhere(String whereClause) {
    return whereClause.replaceAll("^(?i)" + WHERE, " ");
  }

  /**
   * Returns a HQL sort by clause based on the parameters sent to the datasource
   * 
   * @param parameters
   *          parameters sent in the request. They can contain useful info like the property being
   *          sorted, its table, etc
   * @param includeMainEntityID
   *          boolean that specifies if the id of the main entity should be included in the sort
   *          clause. This parameter will be false when the HQL Query includes a GROUP BY clause,
   *          for instance
   * @return an HQL sort by clause or an empty string if the grid is not being filtered
   */
  private String getSortByClause(Map<String, String> parameters, boolean includeMainEntityID) {
    String orderByClause = "";
    Table table = getTableFromParameters(parameters);

    boolean isDistinctQuery = false;
    final String sortBy = parameters.get(JsonConstants.SORTBY_PARAMETER);
    if (parameters.get(JsonConstants.DISTINCT_PARAMETER) != null) {
      orderByClause = parameters.get(JsonConstants.DISTINCT_PARAMETER);
      isDistinctQuery = true;
    } else if (sortBy != null) {
      orderByClause = sortBy;
    } else if (parameters.get(JsonConstants.ORDERBY_PARAMETER) != null) {
      orderByClause = parameters.get(JsonConstants.ORDERBY_PARAMETER);
    } else {
      return "";
    }

    final boolean asc = !orderByClause.startsWith("-");
    String direction = "";
    if (!asc) {
      orderByClause = orderByClause.substring(1);
      direction = " desc ";
    }
    String propertyName = null;
    if (orderByClause.endsWith("$_identifier")) {
      propertyName = orderByClause.substring(0,
          orderByClause.length() - ("$identifier".length() + 1));
    } else {
      propertyName = orderByClause;
    }

    Entity entity = ModelProvider.getInstance().getEntityByTableId(table.getId());
    boolean checkIsNotNull = false;
    Property property = entity.getProperty(propertyName, checkIsNotNull);
    if (property == null) {
      orderByClause = ORDERBY + propertyName;
    } else {
      Column column = OBDal.getInstance().get(Column.class, property.getColumnId());
      if (!orderByClause.isEmpty()) {
        orderByClause = ORDERBY + column.getEntityAlias();
        if (property.getTargetEntity() != null) {
          orderByClause = orderByClause + "."
              + getNameOfFirstIdentifierProperty(property.getTargetEntity());
        }
        orderByClause = orderByClause + direction;
        if (includeMainEntityID && !isDistinctQuery) {
          orderByClause = orderByClause + ", " + table.getEntityAlias() + ".id";
        }
      }
    }
    return orderByClause;
  }

  /**
   * @param entity
   *          entity whose first identifier property name will be returned
   * @return the name of the first identifier property of an entity
   */
  private String getNameOfFirstIdentifierProperty(Entity entity) {
    String propertyName = "";
    if (!entity.getIdentifierProperties().isEmpty()) {
      propertyName = entity.getIdentifierProperties().get(0).getName();
    }
    return propertyName;
  }
}
