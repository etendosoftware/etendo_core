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

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.LockModeType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.stat.SessionStatistics;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.base.model.UniqueConstraint;
import org.openbravo.base.provider.OBNotSingleton;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.ClientEnabled;
import org.openbravo.base.structure.OrganizationEnabled;
import org.openbravo.base.util.Check;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.security.SecurityChecker;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.database.SessionInfo;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * The OBDal class offers the main external access to the Data Access Layer. The variety of data
 * access methods are provided such as save, get, query, remove, etc.
 * 
 * @see OBCriteria
 * @see OBQuery
 * 
 * @author mtaal
 */
// TODO: add methods to return a sorted list based on the identifier of an
// object
// TODO: re-check singleton pattern when a new factory/dependency injection
// approach is implemented.
public class OBDal implements OBNotSingleton {
  private static final Logger log = LogManager.getLogger();
  private static final String ACTIVE_FILTER = "activeFilter";

  private static OBDal instance;

  private static ConcurrentHashMap<String, OBDal> otherPoolInstances = new ConcurrentHashMap<>();
  private String poolName;
  private static DataPoolChecker dataPoolChecker;

  /**
   * @return the singleton instance of the OBDal service
   */
  public static OBDal getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(OBDal.class);
      instance.poolName = ExternalConnectionPool.DEFAULT_POOL;
    }
    return instance;
  }

  /**
   * This method tries to return a read-only instance if the read-only pool is enabled in the
   * configuration, the Preference "OBUIAPP_DefaultDBPoolForReports" is set to "RO" or there is a
   * DataPoolSelection entry for the current process set to "RO". Otherwise, the default pool is
   * returned.
   *
   * @return the singleton instance of the OBDal read-only service if possible. In any other case,
   *         the default instance will be returned.
   */
  public static OBDal getReadOnlyInstance() {
    return getInstance(ExternalConnectionPool.READONLY_POOL);
  }

  /**
   * @param pool
   *          the name of the pool used by the OBDal service that will be returned
   * 
   * @return the singleton instance related to the name passed as parameter
   */
  public static OBDal getInstance(String pool) {
    if (ExternalConnectionPool.DEFAULT_POOL.equals(pool)
        || getDataPoolChecker().shouldUseDefaultPool(SessionInfo.getProcessId())) {
      return getInstance();
    }

    return getOtherPoolInstance(pool);
  }

  private static DataPoolChecker getDataPoolChecker() {
    if (dataPoolChecker == null) {
      dataPoolChecker = DataPoolChecker.getInstance();
    }

    return dataPoolChecker;
  }

  private static OBDal getOtherPoolInstance(String pool) {
    if (!otherPoolInstances.containsKey(pool)) {
      OBDal dal = OBProvider.getInstance().get(OBDal.class);
      dal.poolName = pool;
      otherPoolInstances.putIfAbsent(pool, dal);
    }

    return otherPoolInstances.get(pool);
  }

  /**
   * After calling this method all collections and queries will only return objects which are
   * active. Note that this overrides the active filtering setting on
   * {@link OBQuery#setFilterOnActive(boolean)} and {@link OBCriteria#setFilterOnActive(boolean)}.
   * 
   * @see #disableActiveFilter()
   */
  public void enableActiveFilter() {
    SessionHandler.getInstance()
        .getSession(poolName)
        .enableFilter(ACTIVE_FILTER)
        .setParameter("activeParam", "Y");
  }

  /**
   * After calling this method the active filter is disabled. Note that then the settings in
   * {@link OBQuery#setFilterOnActive(boolean)} and {@link OBCriteria#setFilterOnActive(boolean)}
   * will apply.
   * 
   * @see #enableActiveFilter()
   */
  public void disableActiveFilter() {
    SessionHandler.getInstance().getSession(poolName).disableFilter(ACTIVE_FILTER);
  }

  /**
   * Returns the status of the active filter.
   * 
   * @return true if the active filter is enabled, false if it is disabled
   */
  public boolean isActiveFilterEnabled() {
    return SessionHandler.getInstance()
        .getSession(poolName)
        .getEnabledFilter(ACTIVE_FILTER) != null;
  }

  /**
   * Returns the connection used by the hibernate session.
   * 
   * Note: flushes the hibernate session before returning the connection.
   * 
   * @return the current database connection
   * @see #flush()
   */
  public Connection getConnection() {
    return getConnection(true);
  }

  /**
   * Returns the connection used by the hibernate session.
   * 
   * @param doFlush
   *          if true then the current actions are first flushed.
   * 
   * @return the current database connection
   * @see #flush()
   */
  public Connection getConnection(boolean doFlush) {
    if (doFlush) {
      // before returning a connection flush all other hibernate actions
      // to the database.
      flush();
    }

    return ((SessionImplementor) SessionHandler.getInstance().getSession(poolName)).connection();
  }

  /**
   * @return the current hibernate session
   */
  public Session getSession() {
    return SessionHandler.getInstance().getSession(poolName);
  }

  /**
   * Checks if the session associated with this DAL instance is dirty. Note isDirty method should
   * not be directly invoked on the Session instance.
   *
   * @see SessionHandler#isSessionDirty(String)
   */
  public boolean isSessionDirty() {
    return SessionHandler.getInstance().isSessionDirty(poolName);
  }

  /**
   * Commits the transaction and closes session.
   */
  public void commitAndClose() {
    if (SessionHandler.isSessionHandlerPresent(poolName)) {
      SessionHandler.getInstance().commitAndClose(poolName);
    }
  }

  /**
   * Rolls back the transaction and closes the session.
   */
  public void rollbackAndClose() {
    if (SessionHandler.isSessionHandlerPresent(poolName)) {
      SessionHandler.getInstance().rollback(poolName);
    }
  }

  /**
   * Utility method to log all entities loaded into the current hibernate session. Useful to debug
   * slow flush() calls.
   * 
   * @return the hibernate session statistics that are used to log the information
   */
  private SessionStatistics dumpSessionEntities() {
    SessionStatistics sessStat = SessionHandler.getInstance().getSession(poolName).getStatistics();
    log.debug("Dumping all entities in session");
    for (Object o : sessStat.getEntityKeys()) {
      log.debug(o);
    }
    return sessStat;
  }

  /**
   * Flushes the current state to the database.
   */
  public void flush() {
    if (SessionHandler.isSessionHandlerPresent(poolName)) {
      long s1 = System.currentTimeMillis();
      if (isSessionDirty()) {
        SessionInfo.saveContextInfoIntoDB(getConnection(false));
      }
      SessionHandler.getInstance().getSession(poolName).flush();
      if (log.isDebugEnabled()) {
        long s2 = System.currentTimeMillis();
        SessionStatistics sessStat = dumpSessionEntities();
        log.debug(
            "Flush of " + sessStat.getEntityCount() + " entities and "
                + sessStat.getCollectionCount() + " collections took: " + (s2 - s1),
            new Throwable());
      }
    }
  }

  /**
   * Sets the client and organization of the object (if not set) and persists the object in the
   * database.
   * 
   * @param obj
   *          the object to persist
   */
  public void save(Object obj) {

    // prevent saving of db view objects, this can happen for example if someone accidentally
    // exported views in xml and then imports this xml again
    if (obj instanceof BaseOBObject && ((BaseOBObject) obj).getEntity().isView()) {
      log.warn("Trying to save an object which is a db-view, ignoring save operation, entity: "
          + ((BaseOBObject) obj).getEntity().getName());
      return;
    }

    // set client organization has to be done here before checking write
    // access
    // not the most nice to do
    // TODO: add checking if setClientOrganization is really necessary
    // TODO: log using entityName
    log.debug("Saving object " + obj.getClass().getName());
    setClientOrganization(obj);
    if (!OBContext.getOBContext().isInAdministratorMode()) {
      if (obj instanceof BaseOBObject) {
        OBContext.getOBContext()
            .getEntityAccessChecker()
            .checkWritable(((BaseOBObject) obj).getEntity());
      }
      SecurityChecker.getInstance().checkWriteAccess(obj);
    }
    SessionHandler.getInstance().save(poolName, obj);
  }

  /**
   * Removes the object from the database.
   * 
   * @param obj
   *          the object to be removed
   */
  public void remove(Object obj) {

    // prevent removing of db view objects, this can happen for example if someone accidentally
    // exported views in xml and posts this xml using a webservice
    if (obj instanceof BaseOBObject && ((BaseOBObject) obj).getEntity().isView()) {
      log.warn("Trying to remove an object which is a db-view, ignoring remove operation, entity: "
          + ((BaseOBObject) obj).getEntity().getName());
      return;
    }

    // TODO: add checking if setClientOrganization is really necessary
    // TODO: log using entityName
    log.debug("Removing object " + obj.getClass().getName());
    SecurityChecker.getInstance().checkDeleteAllowed(obj);
    SessionHandler.getInstance().delete(poolName, obj);
  }

  /**
   * Refresh the given object from the database. Also initialized lists inside the object will be
   * refreshed.
   * 
   * @param obj
   *          the object to refresh
   * @see Session#refresh(Object)
   */
  public void refresh(Object obj) {
    SessionHandler.getInstance().getSession(poolName).refresh(obj);
  }

  /**
   * Retrieves an object from the database using the class and id.
   * 
   * @param clazz
   *          the type of object to search for
   * @param id
   *          the id of the object
   * @return the object, or null if none found
   */
  public <T extends Object> T get(Class<T> clazz, Object id) {
    checkReadAccess(clazz);
    try {
      return SessionHandler.getInstance().find(poolName, clazz, id);
    } catch (ObjectNotFoundException ignore) {
      // ObjectNotFoundException is thrown when there was a proxy in cache for this id but the
      // record does not exist in DB. As if there was no proxy, the same invocation would return
      // null, let's be consistent and return null also in this case.
      return null;
    }
  }

  /**
   * Returns true if an object (identified by the entityName and id) exists, false otherwise.
   * 
   * @param entityName
   *          the name of the entity
   * @param id
   *          the id used to find the instance
   * @return true if exists, false otherwise
   */
  public boolean exists(String entityName, Object id) {
    return null != SessionHandler.getInstance().find(poolName, entityName, id);
  }

  /**
   * Retrieves an object from the database using the entity name and id.
   * 
   * @param entityName
   *          the type of object to search for
   * @param id
   *          the id of the object
   * @return the object, or null if none found
   */
  public BaseOBObject get(String entityName, Object id) {
    checkReadAccess(entityName);
    try {
      return SessionHandler.getInstance().find(poolName, entityName, id);
    } catch (ObjectNotFoundException ignore) {
      // ObjectNotFoundException is thrown when there was a proxy in cache for this id but the
      // record does not exist in DB. As if there was no proxy, the same invocation would return
      // null, let's be consistent and return null also in this case.
      return null;
    }
  }

  /**
   * Will return a non-loaded hibernate proxy if the object was not already loaded by hibernate.
   * 
   * NOTE/BEWARE: this method will not check if the object actually exists in the database. This
   * will detected when persisting a referencing object or when this proxy gets initialized!
   * 
   * This method differs from other get methods in this class, these methods will always eagerly
   * load the object and thereby also immediately check the existence of these referenced objects.
   * 
   * @param entityName
   *          the type of object to search for
   * @param id
   *          the id of the object
   * @return the object, or null if none found
   */
  public BaseOBObject getProxy(String entityName, Object id) {
    return (BaseOBObject) ((SessionImplementor) getSession()).internalLoad(entityName,
        (Serializable) id, false, false);
  }

  /**
   * Typed version of {@link OBDal#getProxy(String, Object)} returning a proxied instance of
   * {@code entityClass}
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseOBObject> T getProxy(Class<T> entityClass, String id) {
    return (T) getProxy(DalUtil.getEntityName(entityClass), id);
  }

  /**
   * Creates an OBQuery object using a class and a specific where and order by clause.
   * 
   * @param fromClz
   *          the class to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @return the query object
   */
  public <T extends BaseOBObject> OBQuery<T> createQuery(Class<T> fromClz,
      String whereOrderByClause) {
    return createQuery(fromClz, whereOrderByClause, new HashMap<String, Object>());
  }

  /**
   * Creates an OBQuery object using a class and a specific where and order by clause and a set of
   * parameters which are used in the query.
   * 
   * @param fromClz
   *          the class to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @param parameters
   *          the parameters to use in the query
   * @return the query object
   * 
   * @deprecated use {@link #createQuery(Class, String, Map)} instead.
   */
  @Deprecated
  public <T extends BaseOBObject> OBQuery<T> createQuery(Class<T> fromClz,
      String whereOrderByClause, List<Object> parameters) {
    checkReadAccess(fromClz);
    final OBQuery<T> obQuery = new OBQuery<>();
    obQuery.setWhereAndOrderBy(whereOrderByClause);
    obQuery.setEntity(ModelProvider.getInstance().getEntity(fromClz));
    obQuery.setParameters(parameters);
    obQuery.setPoolName(poolName);
    return obQuery;
  }

  /**
   * Creates an OBQuery object using a class and a specific where and order by clause and a map of
   * named parameters which are used in the query.
   * 
   * @param fromClz
   *          the class to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @param parameters
   *          the named parameters to use in the query
   * @return the query object
   */
  public <T extends BaseOBObject> OBQuery<T> createQuery(Class<T> fromClz,
      String whereOrderByClause, Map<String, Object> parameters) {
    checkReadAccess(fromClz);
    final OBQuery<T> obQuery = new OBQuery<>();
    obQuery.setWhereAndOrderBy(whereOrderByClause);
    obQuery.setEntity(ModelProvider.getInstance().getEntity(fromClz));
    obQuery.setNamedParameters(parameters);
    obQuery.setPoolName(poolName);
    return obQuery;
  }

  /**
   * Creates an OBQuery object using an entity name and a specific where and order by clause.
   * 
   * @param entityName
   *          the type to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @return the new query object
   */
  public OBQuery<BaseOBObject> createQuery(String entityName, String whereOrderByClause) {
    return createQuery(entityName, whereOrderByClause, new HashMap<String, Object>());
  }

  /**
   * Creates an OBQuery object using an entity name and a specific where and order by clause and a
   * set of parameters which are used in the query.
   * 
   * @param entityName
   *          the type to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @param parameters
   *          the parameters to use in the query
   * @return a new instance of {@link OBQuery}.
   * 
   * @deprecated use {@link #createQuery(String, String, Map)} instead.
   */
  @Deprecated
  public OBQuery<BaseOBObject> createQuery(String entityName, String whereOrderByClause,
      List<Object> parameters) {
    checkReadAccess(entityName);
    final OBQuery<BaseOBObject> obQuery = new OBQuery<>();
    obQuery.setWhereAndOrderBy(whereOrderByClause);
    obQuery.setEntity(ModelProvider.getInstance().getEntity(entityName));
    obQuery.setParameters(parameters);
    obQuery.setPoolName(poolName);
    return obQuery;
  }

  /**
   * Creates an OBQuery object using an entity name and a specific where and order by clause and a
   * map of named parameters which are used in the query.
   * 
   * @param entityName
   *          the type to create the query for
   * @param whereOrderByClause
   *          the HQL where and orderby clause
   * @param parameters
   *          the named parameters to use in the query
   * @return a new instance of {@link OBQuery}.
   */
  public OBQuery<BaseOBObject> createQuery(String entityName, String whereOrderByClause,
      Map<String, Object> parameters) {
    checkReadAccess(entityName);
    final OBQuery<BaseOBObject> obQuery = new OBQuery<>();
    obQuery.setWhereAndOrderBy(whereOrderByClause);
    obQuery.setEntity(ModelProvider.getInstance().getEntity(entityName));
    obQuery.setNamedParameters(parameters);
    obQuery.setPoolName(poolName);
    return obQuery;
  }

  /**
   * Creates an OBCriteria object for the specified class.
   * 
   * @param clz
   *          the class used to create the OBCriteria
   * @return a new OBCriteria object
   */
  public <T extends BaseOBObject> OBCriteria<T> createCriteria(Class<T> clz) {
    checkReadAccess(clz);
    final Entity entity = ModelProvider.getInstance().getEntity(clz);
    final OBCriteria<T> obCriteria = new OBCriteria<>(clz.getName(),
        (SessionImplementor) SessionHandler.getInstance().getSession(poolName));
    obCriteria.setEntity(entity);
    return obCriteria;
  }

  /**
   * Creates an OBCriteria object for the specified class.
   * 
   * @param clz
   *          the class used to create the OBCriteria
   * @param alias
   *          an alias that can be used to refer to the specified object
   * @return a new OBCriteria object
   */
  public <T extends BaseOBObject> OBCriteria<T> createCriteria(Class<T> clz, String alias) {
    checkReadAccess(clz);
    final Entity entity = ModelProvider.getInstance().getEntity(clz);
    final OBCriteria<T> obCriteria = new OBCriteria<>(clz.getName(), alias,
        (SessionImplementor) SessionHandler.getInstance().getSession(poolName));
    obCriteria.setEntity(entity);
    return obCriteria;
  }

  /**
   * Creates an OBCriteria object for the specified entity.
   * 
   * @param entityName
   *          the type used to create the OBCriteria
   * @return a new OBCriteria object
   */
  public <T extends BaseOBObject> OBCriteria<T> createCriteria(String entityName) {
    checkReadAccess(entityName);
    Entity entity = ModelProvider.getInstance().getEntity(entityName);
    final OBCriteria<T> obCriteria = new OBCriteria<>(entity.getMappingClass().getName(),
        (SessionImplementor) SessionHandler.getInstance().getSession(poolName));
    obCriteria.setEntity(entity);
    return obCriteria;
  }

  /**
   * Creates an OBCriteria object for the specified entity.
   * 
   * @param entityName
   *          the type used to create the OBCriteria
   * @param alias
   *          an alias that can be used to refer to the specified object
   * @return a new OBCriteria object
   */
  public <T extends BaseOBObject> OBCriteria<T> createCriteria(String entityName, String alias) {
    checkReadAccess(entityName);
    Entity entity = ModelProvider.getInstance().getEntity(entityName);
    final OBCriteria<T> obCriteria = new OBCriteria<>(entity.getMappingClass().getName(), alias,
        (SessionImplementor) SessionHandler.getInstance().getSession(poolName));
    obCriteria.setEntity(entity);
    return obCriteria;
  }

  /**
   * Retrieves a list of baseOBObjects using the unique-constraints defined for the entity. The
   * passed BaseOBObject and the unique-constraints are used to construct a query searching for
   * matching objects in the database.
   * <p>
   * Note that multiple unique constraints are used, so therefore the result can be more than one
   * object.
   * 
   * @param obObject
   *          this property values of this obObject is used to find other objects in the database
   *          with the same property values for the unique constraint properties
   * @return a list of objects which match the passed obObject on the unique constraint properties
   * @see Entity#getUniqueConstraints()
   */
  public List<BaseOBObject> findUniqueConstrainedObjects(BaseOBObject obObject) {
    final Entity entity = obObject.getEntity();
    final List<BaseOBObject> result = new ArrayList<>();
    final Object id = obObject.getId();
    for (final UniqueConstraint uc : entity.getUniqueConstraints()) {
      final OBCriteria<BaseOBObject> criteria = createCriteria(entity.getName());
      if (id != null) {
        criteria.add(Restrictions.ne("id", id));
      }
      for (final Property p : uc.getProperties()) {
        final Object value = obObject.getValue(p.getName());
        criteria.add(Restrictions.eq(p.getName(), value));
      }
      final List<BaseOBObject> queryResult = criteria.list();
      // this is not fast, but the list should be small normally
      // if performance becomes a problem then a hashset should
      // be used.
      for (final BaseOBObject queriedObject : queryResult) {
        if (!result.contains(queriedObject)) {
          result.add(queriedObject);
        }
      }
    }

    return result;
  }

  protected void setClientOrganization(Object o) {
    final OBContext obContext = OBContext.getOBContext();
    // Client and organization in context could have been created in another session, use proxies
    // to set them. Note DalUtil.getId won't help here as objects are already loaded in memory

    if (o instanceof ClientEnabled) {
      final ClientEnabled ce = (ClientEnabled) o;
      if (ce.getClient() == null) {
        ce.setClient(getProxy(Client.class, obContext.getCurrentClient().getId()));
      }
    }
    if (o instanceof OrganizationEnabled) {
      final OrganizationEnabled oe = (OrganizationEnabled) o;
      if (oe.getOrganization() == null) {
        oe.setOrganization(
            getProxy(Organization.class, obContext.getCurrentOrganization().getId()));
      }
    }
  }

  /**
   * Returns an in-clause HQL clause denoting the organizations which are allowed to be read by the
   * current user. The in-clause can be directly used in a HQL. The return string will be for
   * example: in ('1000000', '1000001')
   * 
   * @return an in-clause which can be directly used inside of a HQL clause
   * @see OBContext#getReadableOrganizations()
   * @deprecated Use bind statement parameter instead of this method
   */
  @Deprecated
  public String getReadableOrganizationsInClause() {
    return createInClause(OBContext.getOBContext().getReadableOrganizations());
  }

  /**
   * Returns an in-clause HQL clause denoting the clients which are allowed to be read by the
   * current user. The in-clause can be directly used in a HQL. The return string will be for
   * example: in ('1000000', '1000001')
   * 
   * @return an in-clause which can be directly used inside of a HQL clause
   * @see OBContext#getReadableClients()
   * @deprecated Use bind statement parameter instead of this method
   */
  @Deprecated
  public String getReadableClientsInClause() {
    return createInClause(OBContext.getOBContext().getReadableClients());
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

  private void checkReadAccess(Class<?> clz) {
    checkReadAccess(DalUtil.getEntityName(clz));
  }

  private void checkReadAccess(String entityName) {
    // allow read access to those, otherwise it is really
    // difficult to use querying on these very generic values
    if (entityName.equals(Client.ENTITY_NAME) || entityName.equals(Organization.ENTITY_NAME)) {
      return;
    }
    if (OBContext.getOBContext().isInAdministratorMode()) {
      return;
    }
    final Entity e = ModelProvider.getInstance().getEntity(entityName);
    OBContext.getOBContext().getEntityAccessChecker().checkReadable(e);
  }

  /**
   * Creates a WRITE lock in database for the DAL persistence instance {@code object} parameter and
   * returns a new instance representing the same database object.
   * <p>
   * Note the original instance that is passed as parameter is evicted from Hibernate's 1st level.
   * Therefore, any state not persisted before invoking this method will be ignored, after invoking
   * this method the parameter instance shouldn't be used anymore using instead the returned one.
   * <p>
   * Whereas this is similar to JPA's {@link LockModeType#PESSIMISTIC_WRITE}, it decreases lock
   * level in PostgreSQL implemented by Hibernate from {@code FOR UPDATE} to
   * {@code FOR NO KEY UPDATE} allowing insertions of children records while a lock on its parent is
   * acquired by a different transaction. This is a workaround until Hibernate issue HHH-13135 is
   * fixed. Unlike locks acquired by Hibernate, the ones created by this method are only present in
   * Database and cannot be detected by Hibernate (eg. {@link Session#getCurrentLockMode(Object)}.
   * 
   * @param object
   *          DAL instance to acquire a database lock for.
   * @return A new DAL instance that represents the same database object than the parameter.
   */
  public <T extends BaseOBObject> T getObjectLockForNoKeyUpdate(T object) {
    Entity entity = object.getEntity();

    Check.isTrue(entity.getIdProperties().size() == 1,
        "Expected entity with a single ID. " + entity + " has " + entity.getIdProperties().size());

    String rdbms = new DalConnectionProvider(false).getRDBMS();
    String lockType = "ORACLE".equals(rdbms) ? "UPDATE" : "NO KEY UPDATE";

    String sql = "SELECT " + entity.getIdProperties().get(0).getColumnName() + " FROM "
        + entity.getTableName() + " WHERE " + entity.getIdProperties().get(0).getColumnName()
        + " = :id FOR " + lockType;

    Session session = getSession();
    session.evict(object);
    session.createNativeQuery(sql).setParameter(BaseOBObject.ID, object.getId()).uniqueResult();

    @SuppressWarnings("unchecked")
    T newInstance = OBDal.getInstance().get((Class<T>) entity.getMappingClass(), object.getId());

    return newInstance;
  }
}
