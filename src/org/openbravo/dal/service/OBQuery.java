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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.database.SessionInfo;
import org.openbravo.service.db.QueryTimeOutUtil;

/**
 * The OBQuery supports querying in the Data Access Layer with free-format (HQL) where and order by
 * clauses. The OBQuery automatically adds applicable client and organization filters and handles
 * joining of entities for orderby clauses.
 * 
 * @see OBCriteria
 * @see OBDal
 * @author mtaal
 */

public class OBQuery<E extends BaseOBObject> {
  private static final Logger log = LogManager.getLogger();

  private static final String FROM_SPACED = " from ";
  private static final String FROM_BRACKET = "(from ";
  private static final String AS = "as ";
  private static final String WHERE = "where";
  private static final String ORDERBY = "order by";
  private static final String DAL_CLIENT_FILTER = "_dal_readableClients_dal_";
  private static final String DAL_ORG_FILTER = "_dal_readableOrganizations_dal_";

  // computed in createQueryString
  private String usedAlias = "";
  private String whereAndOrderBy;
  private Entity entity;
  private Map<String, Object> namedParameters;
  private boolean filterOnReadableOrganizations = true;
  private boolean filterOnReadableClients = true;
  private boolean filterOnActive = true;
  private int firstResult = -1;
  private int maxResult = -1;
  private int fetchSize = -1;
  private String queryType = null;

  private String selectClause;

  private String poolName;

  // package visible
  OBQuery() {
  }

  /**
   * Queries the database using the where clauses and additional active, client and organization
   * filters.
   * 
   * @return single result or null
   * @throws HibernateException
   *           if the query returns more than one result
   * @see OBQuery#uniqueResultObject() uniqueResultObject for a version returning an Object
   */
  public E uniqueResult() {
    return createQuery().uniqueResult();
  }

  /**
   * Queries the database using the where clauses and additional active, client and organization
   * filters.
   * 
   * @return single result of type Object or null
   * @throws HibernateException
   *           if the query returns more than one result
   * @see OBQuery#uniqueResult() uniqueResult for a type-safe version
   */
  public Object uniqueResultObject() {
    return createQuery(Object.class).uniqueResult();
  }

  /**
   * Queries the database using the where clauses and additional active, client and organization
   * filters. The order in the list is determined by order by clause.
   * 
   * @return list of objects retrieved from the database
   */
  public List<E> list() {
    return createQuery().list();
  }

  /**
   * Makes it possible to get a {@link Stream} over the underlying Query object. Note that the
   * {@link java.util.stream.Stream#close()} method should be invoked after processing the stream so
   * that the underlying resources are deallocated right away.
   * 
   * @return a {@link Stream} over the underlying Query object.
   */
  public Stream<E> stream() {
    return createQuery().stream();
  }

  /**
   * Queries the database using the where clauses and addition active, client and organization
   * filters. The order in the list is determined by order by clause. Returns an iterator over the
   * data.
   * 
   * @return iterator which walks over the list of objects in the db
   * @deprecated
   */
  @Deprecated
  public Iterator<E> iterate() {
    return createQuery().iterate();
  }

  /**
   * Makes it possible to get a {@link ScrollableResults} from the underlying Query object.
   * 
   * @param scrollMode
   *          the scroll mode to be used
   * @return the scrollable results which can be scrolled in the direction supported by the
   *         scrollMode
   */
  public ScrollableResults scroll(ScrollMode scrollMode) {
    return createQuery().scroll(scrollMode);
  }

  /**
   * Counts the number of objects in the database on the basis of the whereclause of the query.
   * 
   * @return the number of objects in the database taking into account the where and orderby clause
   */
  public int count() {
    // add a space because the FROM constant also starts with a space
    String qryStr = " " + stripOrderBy(createQueryString());
    if (qryStr.toLowerCase().contains(FROM_SPACED)) {
      final int index = qryStr.indexOf(FROM_SPACED) + FROM_SPACED.length();
      qryStr = qryStr.substring(index);
    }
    final Query<Number> qry = getSession().createQuery("select count(*) " + FROM_SPACED + qryStr,
        Number.class);
    setParameters(qry);
    return qry.uniqueResult().intValue();
  }

  /**
   * Computes the row number of a record which has the id which is passed in as a parameter. The
   * rownumber computation takes into account the filter and sorting settings of the the OBQuery
   * object.
   * 
   * @param targetId
   *          the record id
   * @return the row number or -1 if not found
   */
  public int getRowNumber(String targetId) {
    String sql = createQueryString();
    if (sql.toLowerCase().contains(FROM_SPACED)) {
      final int index = sql.indexOf(FROM_SPACED) + FROM_SPACED.length();
      sql = sql.substring(index);
    }
    sql = "select " + usedAlias + "id " + FROM_SPACED + sql;
    final Query<String> qry = getSession().createQuery(sql, String.class);
    setParameters(qry);

    try (ScrollableResults results = qry.scroll(ScrollMode.FORWARD_ONLY)) {
      while (results.next()) {
        final String id = results.getString(0);
        if (id.equals(targetId)) {
          return results.getRowNumber();
        }
      }
    }
    return -1;
  }

  private String stripOrderBy(String qryStr) {
    if (qryStr.toLowerCase().indexOf(ORDERBY) != -1) {
      return qryStr.substring(0, qryStr.toLowerCase().indexOf(ORDERBY));
    }
    return qryStr;
  }

  /**
   * Creates a Hibernate Query object intended to delete records of the Entity associated to the
   * OBQuery instance. To generate the criteria of the deletion, it makes use of the whereclause and
   * extra filters (for readable organizations etc.).
   * 
   * @return a new Hibernate Query object. Note that it does not have an specific type because
   *         delete queries can not be typed.
   */
  @SuppressWarnings("rawtypes")
  public Query deleteQuery() {
    final String qryStr = createQueryString();
    String whereClause;
    final int whereIndex = qryStr.toLowerCase().indexOf(WHERE);

    if (whereIndex != -1) {
      whereClause = qryStr.substring(whereIndex);
    } else {
      throw new OBException("Exception when creating delete query " + qryStr);
    }

    StringBuilder deleteClause = new StringBuilder();
    try {
      deleteClause.append("DELETE FROM ");
      deleteClause.append(getEntity().getName() + " ");
      deleteClause.append(whereClause);
      final Query qry = getSession().createQuery(deleteClause.toString());
      setParameters(qry);
      return qry;
    } catch (final Exception e) {
      throw new OBException("Exception when creating delete query " + deleteClause.toString(), e);
    }
  }

  /**
   * Creates a Hibernate Query object using the whereclause and extra filters (for readable
   * organizations etc.).
   * 
   * @return a new Hibernate Query object
   */
  @SuppressWarnings("unchecked")
  public Query<E> createQuery() {
    return (Query<E>) createQuery(BaseOBObject.class);
  }

  /**
   * Creates a Hibernate Query object using the whereclause and extra filters (for readable
   * organizations etc.). The Query will return objects with the type specified as parameter (unless
   * a specific select clause is provided using the {@link #setSelectClause(String)} method).
   *
   * @param clz
   *          the class of the query's resulting objects
   * 
   * @return a new Hibernate Query object
   */
  public <T extends Object> Query<T> createQuery(Class<T> clz) {
    final String qryStr = createQueryString();
    try {
      final Query<T> qry = getSession().createQuery(qryStr, clz);
      setParameters(qry);
      if (fetchSize > -1) {
        qry.setFetchSize(fetchSize);
      }
      if (firstResult > -1) {
        qry.setFirstResult(firstResult);
      }
      if (maxResult > -1) {
        qry.setMaxResults(maxResult);
      }
      String queryProfile = null;
      if (this.getQueryType() != null) {
        queryProfile = this.getQueryType();
      } else if (SessionInfo.getQueryProfile() != null) {
        queryProfile = SessionInfo.getQueryProfile();
      }
      if (queryProfile != null) {
        QueryTimeOutUtil.getInstance().setQueryTimeOut(qry, queryProfile);
      }
      return qry;
    } catch (final Exception e) {
      throw new OBException("Exception when creating query " + qryStr, e);
    }
  }

  String createQueryString() {
    // split the orderby and where
    final String qryStr = getWhereAndOrderBy();
    final String orderByClause;
    String whereClause;
    final int orderByIndex = qryStr.toLowerCase().indexOf(ORDERBY);
    if (orderByIndex != -1) {
      whereClause = qryStr.substring(0, orderByIndex);
      orderByClause = qryStr.substring(orderByIndex);
    } else {
      whereClause = qryStr;
      orderByClause = "";
    }

    // strip the where, is added later
    if (whereClause.trim().toLowerCase().startsWith(WHERE)) {
      final int whereIndex = whereClause.toLowerCase().indexOf(WHERE);
      if (whereIndex != -1) {
        whereClause = whereClause.substring(1 + whereIndex + WHERE.length());
      }
    }

    // the query can start with an alias to support joins
    //
    String alias = null;
    // this is a space on purpose
    String prefix = " ";
    if (whereClause.toLowerCase().trim().startsWith(AS)) {
      // strip the as
      final String strippedWhereClause = whereClause.toLowerCase().trim().substring(2).trim();
      // get the next space
      final int index = strippedWhereClause.trim().indexOf(' ');
      if (index == -1) {
        alias = strippedWhereClause;
      } else {
        alias = strippedWhereClause.substring(0, index);
      }
      if (alias.endsWith(",")) {
        alias = alias.substring(0, alias.length() - 1);
      }
      prefix = alias + ".";
    }

    usedAlias = prefix;

    // detect a special case, no where but an alias or join
    String aliasJoinClause = "";
    if (alias != null && !whereClause.contains(WHERE)) {
      aliasJoinClause = whereClause;
      whereClause = "";
    }

    // The following if is there because the clauses which are added should
    // all be and-ed. Special cases which need to be handled:
    // left join a left join b where a.id is not null or b.id is not null
    // id='0' and exists (from ADModelObject as mo where mo.id=id)
    // id='0'
    if (whereClause.trim().length() > 0) {
      if (!whereClause.toLowerCase().contains(WHERE)) {
        // simple case: id='0's
        whereClause = " where (" + whereClause + ")";
      } else {
        // check if the where is before
        int fromIndex = whereClause.toLowerCase().indexOf(FROM_SPACED);
        // check another case
        if (fromIndex == -1) {
          fromIndex = whereClause.toLowerCase().indexOf(FROM_BRACKET);
        }
        int whereIndex = -1;
        if (fromIndex == -1) {
          // already there and no from
          // now find the place where to put the brackets
          // case: left join a left join b where a.id is not null or
          // b.id is not null

          whereIndex = whereClause.toLowerCase().indexOf(WHERE);
          Check.isTrue(whereIndex != -1, "Where not found in string: " + whereClause);
        } else {
          // example: id='0' and exists (from ADModelObject as mo
          // where mo.id=id)
          // example: left join x where id='0' and x.id=id and exists
          // (from ADModelObject as mo where mo.id=id)

          // check if the whereClause is before the first from
          whereIndex = whereClause.toLowerCase().substring(0, fromIndex).indexOf(WHERE);
        }

        if (whereIndex != -1) {
          // example: left join x where id='0' and x.id=id and exists
          // (from ADModelObject as mo where mo.id=id)
          // now put the ( at the correct place
          final int endOfWhere = whereIndex + WHERE.length();
          whereClause = whereClause.substring(0, endOfWhere) + " ("
              + whereClause.substring(endOfWhere) + ")";
        } else { // no whereclause before the from
          // example: id='0' and exists (from ADModelObject as mo
          // where mo.id=id)
          whereClause = " where (" + whereClause + ")";
        }
      }
    }

    if (!OBContext.getOBContext().isInAdministratorMode()) {
      OBContext.getOBContext().getEntityAccessChecker().checkReadable(getEntity());
    }

    whereClause = addOrgClientActiveFilter(whereClause, prefix);

    final String result;
    if (alias != null) {
      result = "select " + (selectClause == null ? alias : selectClause) + " from "
          + getEntity().getName() + " " + aliasJoinClause + " " + whereClause + orderByClause;
    } else {
      result = (selectClause == null ? "" : "select " + selectClause + " ") + "from "
          + getEntity().getName() + " " + aliasJoinClause + " " + whereClause + orderByClause;
    }
    log.debug("Created query string " + result);
    return result;
  }

  private String addOrgClientActiveFilter(String paramWhereClause, String prefix) {
    String whereClause = paramWhereClause;
    final OBContext obContext = OBContext.getOBContext();
    boolean addWhereClause = !whereClause.toLowerCase().contains(" where ");
    if (isFilterOnReadableOrganization() && entity.isOrganizationPartOfKey()) {
      whereClause = (addWhereClause ? " where " : "") + addAnd(whereClause) + prefix
          + "id.organization.id in (:" + DAL_ORG_FILTER + ")";
      setNamedParameter(DAL_ORG_FILTER, obContext.getReadableOrganizations());
      if (addWhereClause) {
        addWhereClause = false;
      }
    } else if (isFilterOnReadableOrganization() && entity.isOrganizationEnabled()) {
      whereClause = (addWhereClause ? " where " : "") + addAnd(whereClause) + prefix
          + "organization.id in (:" + DAL_ORG_FILTER + ")";
      setNamedParameter(DAL_ORG_FILTER, obContext.getReadableOrganizations());
      if (addWhereClause) {
        addWhereClause = false;
      }
    }

    if (isFilterOnReadableClients() && getEntity().isClientEnabled()) {
      whereClause = (addWhereClause ? " where " : "") + addAnd(whereClause) + prefix
          + "client.id in (:" + DAL_CLIENT_FILTER + ")";
      setNamedParameter(DAL_CLIENT_FILTER, obContext.getReadableClients());
      if (addWhereClause) {
        addWhereClause = false;
      }
    }

    if (isFilterOnActive() && entity.isActiveEnabled()) {
      whereClause = (addWhereClause ? " where " : "") + addAnd(whereClause) + prefix
          + "active='Y' ";
    }
    return whereClause;
  }

  private String addAnd(String whereClause) {
    if (whereClause.trim().length() > 0) {
      return whereClause + " and ";
    }
    return whereClause;
  }

  /**
   * @return the Entity queried by the Query object
   */
  public Entity getEntity() {
    return entity;
  }

  void setEntity(Entity entity) {
    this.entity = entity;
  }

  private void setParameters(Query<?> qry) {
    final Map<String, Object> localNamedParameters = getNamedParameters();
    if (localNamedParameters == null) {
      return;
    }
    for (Entry<String, Object> entry : localNamedParameters.entrySet()) {
      final String name = entry.getKey();
      final Object value = entry.getValue();
      if (value instanceof Collection<?>) {
        qry.setParameterList(name, (Collection<?>) value);
      } else if (value instanceof String[]) {
        qry.setParameterList(name, (String[]) value);
      } else {
        qry.setParameter(name, value);
      }
    }
  }

  /**
   * Controls if the readable organizations should be used as a filter in the query. The default is
   * true.
   * 
   * @return if false then readable organizations are not added as a filter to the query
   */
  public boolean isFilterOnReadableOrganization() {
    return filterOnReadableOrganizations;
  }

  /**
   * Controls if the readable organizations should be used as a filter in the query. The default is
   * true.
   * 
   * @param filterOnReadableOrganizations
   *          if set to false then readable organizations are not added as a filter to the query
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setFilterOnReadableOrganization(boolean filterOnReadableOrganizations) {
    this.filterOnReadableOrganizations = filterOnReadableOrganizations;
    return this;
  }

  /**
   * Controls if the isActive column is used as a filter (isActive == 'Y'). The default is true.
   * 
   * @return if false then isActive is not used as a filter for the query
   */
  public boolean isFilterOnActive() {
    return filterOnActive;
  }

  /**
   * Controls if the isActive column is used as a filter (isActive == 'Y'). The default is true.
   * 
   * @param filterOnActive
   *          if false then isActive is not used as a filter for the query, if true (the default)
   *          then isActive='Y' is added as a filter to the query
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setFilterOnActive(boolean filterOnActive) {
    this.filterOnActive = filterOnActive;
    return this;
  }

  /**
   * @return the where and order by clause used in the query
   */
  public String getWhereAndOrderBy() {
    // replace WHERE keyword to lowercase as hql exception is generated in org.hibernate.hql.PARSER
    whereAndOrderBy = whereAndOrderBy.replaceAll(" WHERE ", " where ");
    return whereAndOrderBy;
  }

  /**
   * Sets the where and order by clause in the query.
   * 
   * @param queryString
   *          the where and order by parts of the query
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setWhereAndOrderBy(String queryString) {
    if (queryString == null) {
      this.whereAndOrderBy = "";
    } else {
      this.whereAndOrderBy = queryString;
    }
    return this;
  }

  private Session getSession() {
    return SessionHandler.getInstance().getSession(poolName);
  }

  /**
   * Set the non-named parameters ('?') in the query by converting them to named parameters. This
   * conversion is done because legacy-style query parameters are no longer supported in Hibernate.
   * 
   * Note that this method also parses the where and order by clauses of the query to make use of
   * the newly generated named parameters.
   * 
   * @param parameters
   *          the parameters which are set in the query without a name (e.g. as :?)
   * 
   * @deprecated use {@link #setNamedParameters(Map)} instead.
   */
  @Deprecated
  public void setParameters(List<Object> parameters) {
    converToNamedParameterQuery(parameters);
  }

  private void converToNamedParameterQuery(List<Object> parameters) {
    if (parameters == null || parameters.isEmpty()) {
      return;
    }
    Pattern pattern = Pattern.compile("\\?");
    Matcher matcher = pattern.matcher(whereAndOrderBy);
    StringBuffer parsedHql = new StringBuffer();
    int parameterCount = 0;
    while (matcher.find()) {
      String parameterName = "__p" + parameterCount;
      matcher.appendReplacement(parsedHql, ":" + parameterName + " ");
      setNamedParameter(parameterName, parameters.get(parameterCount));
      parameterCount++;
    }
    matcher.appendTail(parsedHql);
    Check.isTrue(parameterCount == parameters.size(),
        "Could not convert legacy-style query parameters in: " + whereAndOrderBy);
    whereAndOrderBy = parsedHql.toString();
  }

  /**
   * Filter the results on readable clients (@see OBContext#getReadableClients()). The default is
   * true.
   * 
   * @return if true then only objects from readable clients are returned, if false then objects
   *         from all clients are returned
   */
  public boolean isFilterOnReadableClients() {
    return filterOnReadableClients;
  }

  /**
   * Filter the results on readable clients (@see OBContext#getReadableClients()). The default is
   * true.
   * 
   * @param filterOnReadableClients
   *          if true then only objects from readable clients are returned by this Query, if false
   *          then objects from all clients are returned
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setFilterOnReadableClients(boolean filterOnReadableClients) {
    this.filterOnReadableClients = filterOnReadableClients;
    return this;
  }

  /**
   * The named parameters used in the query.
   * 
   * @return the map of named parameters which are being used in the query
   */
  public Map<String, Object> getNamedParameters() {
    return namedParameters;
  }

  /**
   * Set the named parameters used in the query.
   * 
   * @param namedParameters
   *          the list of named parameters (string, value pair)
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setNamedParameters(Map<String, Object> namedParameters) {
    this.namedParameters = namedParameters;
    return this;
  }

  /**
   * Sets one named parameter used in the query.
   * 
   * @param paramName
   *          name of the parameter
   * @param value
   *          value which should be used for this parameter
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setNamedParameter(String paramName, Object value) {
    if (this.namedParameters == null) {
      this.namedParameters = new HashMap<>();
    }
    this.namedParameters.put(paramName, value);
    return this;
  }

  /**
   * Returns the position of the first row to be retrieved by the underlying query.
   * 
   * @return the position of the first row to be retrieved
   */
  public int getFirstResult() {
    return firstResult;
  }

  /**
   * Sets the position of the first row to retrieve.
   * 
   * @param firstResult
   *          the position of the first row to retrieve
   *
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setFirstResult(int firstResult) {
    this.firstResult = firstResult;
    return this;
  }

  /**
   * Returns the maximum number of rows to be retrieved by the underlying query.
   * 
   * @return the maximum number of rows to be retrieved
   */
  public int getMaxResult() {
    return maxResult;
  }

  /**
   * Sets the maximum number of rows to retrieve.
   * 
   * @param maxResult
   *          the maximum number of rows to retrieve
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setMaxResult(int maxResult) {
    this.maxResult = maxResult;
    return this;
  }

  /**
   * Returns the fetch size of the underlying query.
   * 
   * @return the fetch size of the underlying query
   */
  public int getFetchSize() {
    return fetchSize;
  }

  /**
   * Sets a fetch size for the underlying query.
   * 
   * @param fetchSize
   *          the fetch size for the underlying query
   *
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setFetchSize(int fetchSize) {
    this.fetchSize = fetchSize;
    return this;
  }

  /**
   * Returns the select clause defined for the underlying query.
   * 
   * @return the select clause defined for the underlying query
   */
  public String getSelectClause() {
    return selectClause;
  }

  /**
   * Defines a select clause for the underlying query. <b>Important Note</b>: this method can change
   * the type of the object returned by the query, which is previously defined when instantiating
   * the OBQuery object.
   * 
   * @param selectClause
   *          the select clause to be used by the underlying query.
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setSelectClause(String selectClause) {
    this.selectClause = selectClause;
    return this;
  }

  /**
   * Sets the type of the underlying query.
   * 
   * @param queryType
   *          the type of the underlying query
   * 
   * @return this OBQuery instance, for method chaining.
   */
  public OBQuery<E> setQueryType(String queryType) {
    this.queryType = queryType;
    return this;
  }

  /**
   * Returns the type of the underlying query.
   * 
   * @return a String with the type of the underlying query
   */
  public String getQueryType() {
    return this.queryType;
  }

  void setPoolName(String poolName) {
    this.poolName = poolName;
  }
}
