package org.openbravo.dal.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.Reference;

import jakarta.enterprise.context.Dependent;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.metamodel.Metamodel; // ← IMPORT JPA, no org.hibernate.Metamodel

import org.hibernate.Cache;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.StatelessSession;
import org.hibernate.StatelessSessionBuilder;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.FilterDefinition;
import org.hibernate.graph.*;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.relational.*;
import org.hibernate.stat.Statistics;
import org.hibernate.query.criteria.HibernateCriteriaBuilder; // ← si querés el builder específico
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.database.SessionInfo;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Dependent
public class DalSessionFactory implements SessionFactory {

  private static final long serialVersionUID = 1L;

  private SessionFactory delegateSessionFactory;
  private JdbcConnectionAccess jdbcConnectionAccess;

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

  // ───────────────────────────────────────────────────────────────────────────
  // Métodos ELIMINADOS en Hibernate 6: getAllClassMetadata / getAllCollectionMetadata /
  // getClassMetadata / getCollectionMetadata / getTypeHelper
  // Simplemente se quitan. Si tenías usos en otro lado, hay que migrarlos al Metamodel JPA.
  // ───────────────────────────────────────────────────────────────────────────

  @Override
  public Session getCurrentSession() throws HibernateException {
    return delegateSessionFactory.getCurrentSession();
  }

  @Override
  public Set<String> getDefinedFilterNames() {
    return delegateSessionFactory.getDefinedFilterNames();
  }

  @Override
  public FilterDefinition getFilterDefinition(String filterName) throws HibernateException {
    return delegateSessionFactory.getFilterDefinition(filterName);
  }

  // Nuevo en Hibernate 6: nombre del método cambió
  @Override
  public Set<String> getDefinedFetchProfileNames() {
    return delegateSessionFactory.getDefinedFetchProfileNames();
  }

  @Override
  public boolean containsFetchProfileDefinition(String name) {
    return delegateSessionFactory.containsFetchProfileDefinition(name);
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
  public SchemaManager getSchemaManager() {
    return delegateSessionFactory.getSchemaManager();
  }

  @Override
  public boolean isClosed() {
    return delegateSessionFactory.isClosed();
  }

  /**
   * Abre sesión y ejecuta la inicialización en la MISMA conexión de la sesión
   */
  @Override
  public Session openSession() throws HibernateException {
    final Session session = delegateSessionFactory.openSession();
    // En Hibernate 6 NO uses SessionImplementor#connection(); usá doWork:
    session.doWork(this::initConnection);
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

  @Override
  public StatelessSession openStatelessSession() {
    final StatelessSession session = delegateSessionFactory.openStatelessSession();
    // Igual que arriba, en la conexión de ESA sesión:
    session.doWork(this::initConnection);
    return session;
  }

  @Override
  public StatelessSession openStatelessSession(Connection connection) {
    final StatelessSession session = delegateSessionFactory.openStatelessSession(connection);
    // Y también inicializamos la sesión para esta conexión provista:
    session.doWork(this::initConnection);
    return session;
  }

  @Override
  public Cache getCache() {
    return delegateSessionFactory.getCache();
  }

  JdbcConnectionAccess getJdbcConnectionAccess() {
    if (jdbcConnectionAccess == null) {
      jdbcConnectionAccess = ((SessionFactoryImpl) delegateSessionFactory)
          .getJdbcServices()
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

  // ───────────────────────────────────────────────────────────────────────────
  // EntityManagerFactory (JPA)
  // ───────────────────────────────────────────────────────────────────────────

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

  // ⚠️ En Hibernate 6, el tipo de retorno “oficial” es HibernateCriteriaBuilder.
  // Si tu interfaz importada es org.hibernate.SessionFactory, su método es:
  //   HibernateCriteriaBuilder getCriteriaBuilder();
  // Podés devolverlo con ese tipo, y si en tu código llamas por JPA, seguís usando CriteriaBuilder.
  @Override
  public HibernateCriteriaBuilder getCriteriaBuilder() {
    return delegateSessionFactory.getCriteriaBuilder();
  }
  // Si preferís mantener la firma jakarta CriteriaBuilder en este wrapper,
  // cambiá la import del SessionFactory que implementás (no recomendado).
  // Dejarlo como arriba evita el error de incompatibilidad que viste.

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
  public RootGraph<?> findEntityGraphByName(String name) {
    return delegateSessionFactory.findEntityGraphByName(name);
  }

  // IMPORTANTE: usar el Metamodel de JPA (jakarta), no el de Hibernate viejo
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
