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

package org.openbravo.dal.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.persistence.EntityGraph;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.Query;
import javax.persistence.SynchronizationType;
import javax.persistence.criteria.CriteriaBuilder;

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.Metamodel;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.TypeHelper;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.StatelessSessionImpl;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.metadata.CollectionMetadata;
import org.hibernate.stat.Statistics;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.database.SessionInfo;

/**
 * The DalSessionFactory directly delegates all calls to a real SessionFactory except for the calls
 * to open a session in that case an extra action is done to set session information in the database
 * (and then the call is forwarded to the 'real' SessionFactory).
 * 
 * @author mtaal
 * @see SessionFactoryController
 */
@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class DalSessionFactory implements SessionFactory {

  private static final long serialVersionUID = 1L;

  private SessionFactory delegateSessionFactory;
  private JdbcConnectionAccess jdbcConnectionAccess;

  /**
   * NOTE: Openbravo requires normal application code to use the DalSessionFactory and not the real
   * underlying Hibernate SessionFactory.
   * 
   * @return the underlying real sessionfactory
   */
  public SessionFactory getDelegateSessionFactory() {
    return delegateSessionFactory;
  }

  public void setDelegateSessionFactory(SessionFactory delegateSessionFactory) {
    this.delegateSessionFactory = delegateSessionFactory;
  }

  @Override
  public void close() throws HibernateException {
    delegateSessionFactory.close();
  }

  @Override
  public Map getAllClassMetadata() throws HibernateException {
    return delegateSessionFactory.getAllClassMetadata();
  }

  @Override
  public Map getAllCollectionMetadata() throws HibernateException {
    return delegateSessionFactory.getAllCollectionMetadata();
  }

  @Override
  public ClassMetadata getClassMetadata(Class persistentClass) throws HibernateException {
    return delegateSessionFactory.getClassMetadata(persistentClass);
  }

  @Override
  public ClassMetadata getClassMetadata(String entityName) throws HibernateException {
    return delegateSessionFactory.getClassMetadata(entityName);
  }

  @Override
  public CollectionMetadata getCollectionMetadata(String roleName) throws HibernateException {
    return delegateSessionFactory.getCollectionMetadata(roleName);
  }

  @Override
  public Session getCurrentSession() throws HibernateException {
    return delegateSessionFactory.getCurrentSession();
  }

  @Override
  public Set getDefinedFilterNames() {
    return delegateSessionFactory.getDefinedFilterNames();
  }

  @Override
  public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
    return delegateSessionFactory.getFilterDefinition(filterName);
  }

  @Override
  public Reference getReference() throws NamingException {
    return delegateSessionFactory.getReference();
  }

  @Override
  public Statistics getStatistics() {
    return delegateSessionFactory.getStatistics();
  }

  @Override
  public boolean isClosed() {
    return delegateSessionFactory.isClosed();
  }

  /**
   * Note method sets user session information in the database and opens a connection for this.
   */
  @Override
  public Session openSession() throws HibernateException {
    final Session session = delegateSessionFactory.openSession();
    Connection conn = ((SessionImplementor) session).connection();
    initConnection(conn);
    return session;
  }

  void initConnection(Connection conn) {
    final Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    SessionInfo.initDB(conn, props.getProperty("bbdd.rdbms"));
    final String dbSessionConfig = props.getProperty("bbdd.sessionConfig");
    try (PreparedStatement pstmt = conn.prepareStatement(dbSessionConfig)) {
      pstmt.executeQuery();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Note method sets user session information in the database and opens a connection for this.
   */
  @Override
  public StatelessSession openStatelessSession() {
    final StatelessSession session = delegateSessionFactory.openStatelessSession();
    initializeDBSessionInfo((StatelessSessionImpl) session);
    return session;
  }

  /**
   * Note method sets user session information in the database and opens a connection for this.
   */
  @Override
  public StatelessSession openStatelessSession(Connection connection) {
    final StatelessSession session = delegateSessionFactory.openStatelessSession(connection);
    initializeDBSessionInfo((StatelessSessionImpl) session);
    return session;
  }

  private void initializeDBSessionInfo(StatelessSessionImpl session) {
    Connection conn = session.connection();
    SessionInfo.initDB(conn,
        OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("bbdd.rdbms"));
  }

  @Override
  public Cache getCache() {
    return delegateSessionFactory.getCache();
  }

  @Override
  public boolean containsFetchProfileDefinition(String name) {
    return delegateSessionFactory.containsFetchProfileDefinition(name);
  }

  @Override
  public TypeHelper getTypeHelper() {
    return delegateSessionFactory.getTypeHelper();
  }

  JdbcConnectionAccess getJdbcConnectionAccess() {
    if (jdbcConnectionAccess == null) {
      jdbcConnectionAccess = ((SessionFactoryImpl) delegateSessionFactory).getJdbcServices()
          .getBootstrapJdbcConnectionAccess();
    }
    return jdbcConnectionAccess;
  }

  @Override
  public <T> void addNamedEntityGraph(String graphName, EntityGraph<T> entityGraph) {
    delegateSessionFactory.addNamedEntityGraph(graphName, entityGraph);
  }

  @Override
  public void addNamedQuery(String name, Query query) {
    delegateSessionFactory.addNamedQuery(name, query);
  }

  @Override
  public EntityManager createEntityManager() {
    return delegateSessionFactory.createEntityManager();
  }

  @Override
  public EntityManager createEntityManager(Map map) {
    return delegateSessionFactory.createEntityManager(map);
  }

  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType) {
    return delegateSessionFactory.createEntityManager(synchronizationType);
  }

  @Override
  public EntityManager createEntityManager(SynchronizationType synchronizationType, Map map) {
    return delegateSessionFactory.createEntityManager(synchronizationType, map);
  }

  @Override
  public CriteriaBuilder getCriteriaBuilder() {
    return delegateSessionFactory.getCriteriaBuilder();
  }

  @Override
  public PersistenceUnitUtil getPersistenceUnitUtil() {
    return delegateSessionFactory.getPersistenceUnitUtil();
  }

  @Override
  public Map<String, Object> getProperties() {
    return delegateSessionFactory.getProperties();
  }

  @Override
  public boolean isOpen() {
    return delegateSessionFactory.isOpen();
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    return delegateSessionFactory.unwrap(clazz);
  }

  @Override
  public <T> List<EntityGraph<? super T>> findEntityGraphsByType(Class<T> entityClass) {
    return delegateSessionFactory.findEntityGraphsByType(entityClass);
  }

  @Override
  public Metamodel getMetamodel() {
    return delegateSessionFactory.getMetamodel();
  }

  @Override
  public SessionFactoryOptions getSessionFactoryOptions() {
    return delegateSessionFactory.getSessionFactoryOptions();
  }

  @Override
  public SessionBuilder withOptions() {
    return delegateSessionFactory.withOptions();
  }

  @Override
  public StatelessSessionBuilder withStatelessOptions() {
    return delegateSessionFactory.withStatelessOptions();
  }

}
