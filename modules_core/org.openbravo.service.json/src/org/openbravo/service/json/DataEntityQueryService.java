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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.service.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.domaintype.TableDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.datamodel.Column;

/**
 * Implements a service which can handle different types of query and paging options. This class
 * supports standard parameters for paging and flexible parameters for filtering queries. Sorting is
 * supported on one property.
 * 
 * This service class only supports querying for one {@link Entity}.
 * 
 * This service class can not be used as a singleton.
 * 
 * @author mtaal
 */
public class DataEntityQueryService {
  private static final Logger log = LogManager.getLogger();

  public static final String PARAM_DELIMITER = "@";

  private String entityName;
  private Integer firstResult = null;
  private Integer maxResults = null;

  private boolean filterOnActive = true;
  private AdvancedQueryBuilder queryBuilder = new AdvancedQueryBuilder();

  private boolean filterOnReadableOrganizations = true;
  private boolean filterOnReadableClients = true;

  private String distinct;

  private String summarySettings;
  private List<String> summaryFields = new ArrayList<String>();

  /**
   * Count the records which fit in the filter criteria.
   * 
   * @return the number of records in the filter.
   */
  public int count() {
    Check.isNotNull(entityName, "entityName must be set");
    final OBQuery<BaseOBObject> obq = OBDal.getInstance()
        .createQuery(entityName, queryBuilder.getJoinClause() + queryBuilder.getWhereClause());
    obq.setFilterOnReadableClients(isFilterOnReadableClients());
    obq.setFilterOnReadableOrganization(isFilterOnReadableOrganizations());

    if (queryBuilder.hasOrganizationParameter()) {
      obq.setFilterOnReadableOrganization(false);
    }
    if (!filterOnActive) {
      obq.setFilterOnActive(false);
    }
    obq.setNamedParameters(queryBuilder.getNamedParameters());

    return obq.count();
  }

  /**
   * Return the list of {@link BaseOBObject} objects retrieved by querying using the filter
   * criteria.
   * 
   * @return the list of retrieved objects from the db.
   */
  public List<BaseOBObject> list() {
    return buildOBQuery().list();
  }

  /**
   * @return a result which can be scrolled forward only and the results are not cached
   */
  public ScrollableResults scroll() {
    final Query<BaseOBObject> qry = buildOBQuery().createQuery();
    qry.setFetchSize(1000);
    qry.setCacheable(false);
    return qry.scroll(ScrollMode.FORWARD_ONLY);
  }

  public String getWhereClause() {
    return queryBuilder.getJoinClause() + queryBuilder.getWhereClause();
  }

  /**
   * Build an OBQuery object using the generated where, order by and select clauses.
   */
  public OBQuery<BaseOBObject> buildOBQuery() {
    final String whereOrderBy = getWhereClause()
        + (getSummarySettings() == null ? queryBuilder.getOrderByClause() : "");

    log.debug("Querying for " + entityName + " " + whereOrderBy);

    final OBQuery<BaseOBObject> obq = OBDal.getInstance().createQuery(entityName, whereOrderBy);
    obq.setFilterOnReadableClients(isFilterOnReadableClients());
    obq.setFilterOnReadableOrganization(isFilterOnReadableOrganizations());

    if (getSummarySettings() != null) {
      obq.setSelectClause(queryBuilder.getSelectClause());
    }

    if (getFirstResult() != null) {
      obq.setFirstResult(getFirstResult());
      log.debug("Firstresult " + getFirstResult());
    }
    if (getMaxResults() != null) {
      obq.setMaxResult(getMaxResults());
      log.debug("Maxresult " + getMaxResults());
    }

    if (queryBuilder.hasOrganizationParameter()) {
      obq.setFilterOnReadableOrganization(false);
    }
    obq.setFilterOnActive(isFilterOnActive());

    log.debug("Setting params:\n{}",
        () -> queryBuilder.getNamedParameters().entrySet().stream().map(e -> {
          Object v = e.getValue();
          if (v instanceof Object[]) {
            v = Arrays.asList((Object[]) v);
          }
          return "  -" + e.getKey() + ": " + v;
        }).collect(Collectors.joining("\n")));

    obq.setNamedParameters(queryBuilder.getNamedParameters());

    return obq;
  }

  public AdvancedQueryBuilder getQueryBuilder() {
    return queryBuilder;
  }

  // package private on purpose
  List<Property> getDistinctDisplayProperties() {
    final String localDistinct = getDistinct();
    final List<Property> properties = new ArrayList<Property>();
    final Property property = DalUtil
        .getPropertyFromPath(ModelProvider.getInstance().getEntity(getEntityName()), localDistinct);

    // now use the table reference definition or select on the identifier properties
    if (property.getDomainType() instanceof TableDomainType
        && ((TableDomainType) property.getDomainType()).getRefTable() != null) {
      final TableDomainType domainType = (TableDomainType) property.getDomainType();
      final Property displayProp = KernelUtils.getInstance()
          .getPropertyFromColumn(OBDal.getInstance()
              .get(Column.class, domainType.getRefTable().getDisplayColumn().getId()));
      if (displayProp != null) {
        properties.add(displayProp);
      }
    }

    if (properties.isEmpty()) {
      for (Property identifierProp : property.getTargetEntity().getIdentifierProperties()) {
        properties.add(identifierProp);
      }
    }
    return properties;
  }

  public int getRowNumber(String targetRecordId) {
    final String whereOrderBy = queryBuilder.getJoinClause() + queryBuilder.getWhereClause()
        + queryBuilder.getOrderByClause();

    log.debug("Querying for " + entityName + " " + whereOrderBy);
    final OBQuery<BaseOBObject> obq = OBDal.getInstance().createQuery(entityName, whereOrderBy);
    obq.setFilterOnReadableClients(isFilterOnReadableClients());
    obq.setFilterOnReadableOrganization(isFilterOnReadableOrganizations());

    if (queryBuilder.hasOrganizationParameter()) {
      obq.setFilterOnReadableOrganization(false);
    }
    obq.setFilterOnActive(isFilterOnActive());

    obq.setNamedParameters(queryBuilder.getNamedParameters());

    return obq.getRowNumber(targetRecordId);

  }

  public Integer getFirstResult() {
    return firstResult;
  }

  public void setFirstResult(Integer firstResult) {
    this.firstResult = firstResult;
  }

  public Integer getMaxResults() {
    return maxResults;
  }

  public void setMaxResults(Integer maxResults) {
    this.maxResults = maxResults;
  }

  public void setOrderBy(String orderBy) {
    queryBuilder.setOrderBy(orderBy);
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
    queryBuilder.setEntity(entityName);
  }

  /**
   * The text matching strategy used. See here for a description:
   * http://www.smartclient.com/docs/7.0rc2/a/b/c/go.html#attr..ComboBoxItem.textMatchStyle
   * 
   * @param textMatchingName
   *          the following values are allowed: startsWith, substring, exact
   */
  public void setTextMatching(String textMatchingName) {
  }

  public void setCriteria(JSONObject criteria) {
    try {
      queryBuilder.setCriteria(criteria);
      if (criteria.has("operator")
          && AdvancedQueryBuilder.OPERATOR_OR.equals(criteria.getString("operator"))) {
        setDoOrExpression();
      }
    } catch (JSONException e) {
      throw new OBException(e);
    }
  }

  public void addFilterParameter(String key, String value) {
    queryBuilder.addFilterParameter(key, value);
  }

  /**
   * If called then the where clause filters will be considered to be an or expression.
   */
  public void setDoOrExpression() {
    queryBuilder.setDoOr(true);
  }

  /**
   * Tells the query builder to use the {@link JsonConstants#MAIN_ALIAS} as the alias for prefixing
   * all properties in the where clause and order by.
   */
  public void setUseAlias() {
    queryBuilder.setMainAlias(JsonConstants.MAIN_ALIAS);
  }

  public boolean isFilterOnActive() {
    return filterOnActive;
  }

  public void setFilterOnActive(boolean filterOnActive) {
    this.filterOnActive = filterOnActive;
  }

  public void setJoinAssociatedEntities(boolean joinAssociatedEntities) {
    queryBuilder.setJoinAssociatedEntities(joinAssociatedEntities);
  }

  public void setAdditionalProperties(List<String> additionalProperties) {
    queryBuilder.setAdditionalProperties(additionalProperties);
  }

  public void clearCachedValues() {
    queryBuilder.clearCachedValues();
  }

  public String getDistinct() {
    return distinct;
  }

  public void setDistinct(String distinct) {
    this.distinct = distinct.replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT);
  }

  public String getSummarySettings() {
    return summarySettings;
  }

  public void setSummarySettings(String summarySettings) {
    this.summarySettings = summarySettings;
    if (getSummarySettings() != null) {
      try {
        summaryFields.clear();
        final JSONObject summarySetting = new JSONObject(getSummarySettings());
        final Iterator<?> it = summarySetting.keys();
        while (it.hasNext()) {
          final String key = (String) it.next();
          summaryFields.add(key);
          final String value = summarySetting.getString(key);
          queryBuilder.addSelectFunctionPart(value,
              key.replace(DalUtil.FIELDSEPARATOR, DalUtil.DOT));
        }
      } catch (JSONException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public List<String> getSummaryFields() {
    return summaryFields;
  }

  public boolean isFilterOnReadableOrganizations() {
    return filterOnReadableOrganizations;
  }

  public void setFilterOnReadableOrganizations(boolean filterOnReadableOrganizations) {
    this.filterOnReadableOrganizations = filterOnReadableOrganizations;
  }

  public boolean isFilterOnReadableClients() {
    return filterOnReadableClients;
  }

  public void setFilterOnReadableClients(boolean filterOnReadableClients) {
    this.filterOnReadableClients = filterOnReadableClients;
  }

  /**
   * In case of performing query for FK drop down list (ie. Organization link in Product window),
   * there are 2 entities to query:
   * <ul>
   * <li>main entity: in the example would be Organization which is the one we want to get records
   * from
   * <li>sub entity: in this case Product, it will be filtered in the same way it is in the grid so
   * only organizations with that criteria will be shown
   * </ul>
   */
  void setSubEntity(String subEntityName, DataEntityQueryService dataEntityQueryService,
      Property distinctProperty, String distinctPropertyPath) {
    queryBuilder.setSubEntityName(subEntityName);
    queryBuilder.setSubDataEntityQueryService(dataEntityQueryService);
    queryBuilder.setDistinctProperty(distinctProperty);
    queryBuilder.setDistinctPropertyPath(distinctPropertyPath);
  }

  @Deprecated
  public void setSubEntity(String subEntityName, DataEntityQueryService dataEntityQueryService,
      Property distinctProperty) {
    setSubEntity(subEntityName, dataEntityQueryService, distinctProperty,
        distinctProperty.getName());
  }

}
