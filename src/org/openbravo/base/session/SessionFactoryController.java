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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base.session;

import java.sql.Connection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.internal.DriverManagerConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.Service;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalSessionFactory;

/**
 * Initializes and provides the session factory to the rest of the application. There are subclasses
 * for for the bootstrap process and the runtime process.
 * 
 * @author mtaal
 */

public abstract class SessionFactoryController {
  private static final Logger log = LogManager.getLogger();

  // note the order by is really important otherwise the build of
  // uniqueconstraints (ModelProvider) can fail with strange errors.
  private static final String UNIQUE_CONSTRAINT_QUERY_POSTGRES = "SELECT pg_class.relname, pg_attribute.attname, pg_constraint.conname FROM pg_constraint JOIN pg_class ON pg_class.oid = pg_constraint.conrelid JOIN pg_attribute ON pg_attribute.attrelid=pg_constraint.conrelid WHERE pg_constraint.contype = 'u' AND (pg_attribute.attnum = ANY (pg_constraint.conkey)) order by pg_constraint.conname";
  private static final String UNIQUE_CONSTRAINT_QUERY_ORACLE = "SELECT UCC.TABLE_NAME,UCC.COLUMN_NAME,UCC.CONSTRAINT_NAME FROM USER_CONS_COLUMNS UCC JOIN USER_CONSTRAINTS UC ON UC.CONSTRAINT_NAME=UCC.CONSTRAINT_NAME WHERE UC.CONSTRAINT_TYPE = 'U' ORDER BY UCC.CONSTRAINT_NAME";

  private static final String COLUMN_QUERY_POSTGRES = "SELECT t.tablename, a.attname, a.attnotnull FROM pg_tables t, pg_class c, pg_attribute a "
      + "WHERE c.relname = t.tablename AND a.attnum > 0 AND a.attrelid = c.oid";

  private static final String COLUMN_QUERY_ORACLE = "SELECT C.TABLE_NAME, C.COLUMN_NAME, C.NULLABLE "
      + "FROM USER_TAB_COLUMNS C  ORDER BY C.TABLE_NAME";

  private static SessionFactoryController instance = null;

  private static boolean runningInWebContainer = false;

  /**
   * Keeps track if the Dal layer runs within Tomcat or within for example an Ant task.
   * 
   * @return true if the current Dal layer runs within Tomcat, false otherwise
   */
  public static boolean isRunningInWebContainer() {
    return runningInWebContainer;
  }

  /**
   * Is set from the {@link org.openbravo.dal.core.DalContextListener DalContextListener}, keeps
   * track if the Dal layer runs within Tomcat or within for example an Ant task.
   * 
   * @param runningInWebContainer
   *          true if running in a webcontainer such as Tomcat
   */
  public static void setRunningInWebContainer(boolean runningInWebContainer) {
    SessionFactoryController.runningInWebContainer = runningInWebContainer;
  }

  public static synchronized SessionFactoryController getInstance() {
    return instance;
  }

  public static synchronized void setInstance(SessionFactoryController sfc) {
    if (sfc != null) {
      log.debug("Setting instance of {} as session factory controller", sfc.getClass().getName());
    } else {
      log.debug("Nullifying session factory controller");
    }
    if (instance != null && instance.isInitialized()) {
      // TODO: we should ensure that there are no open sessions....
      instance.getSessionFactory().close();
    }
    instance = sfc;
  }

  private SessionFactory sessionFactory = null;
  private Configuration configuration = null;
  private boolean isPostgresDatabase = false;
  private String bbddUser;

  public SessionFactory getSessionFactory() {
    initialize();
    return sessionFactory;
  }

  public Configuration getConfiguration() {
    initialize();
    return configuration;
  }

  public boolean isInitialized() {
    return sessionFactory != null;
  }

  /**
   * Resets and initializes the SessionFactory. If there is a SessionFactory then this one is first
   * closed before a new one is created.
   */
  public void reInitialize() {
    if (sessionFactory != null) {
      sessionFactory.close();
      sessionFactory = null;
    }
    initialize();
  }

  /**
   * Creates a new Hibernate Configuration, generates a mapping and initializes the SessionFactory.
   */
  public void initialize() {
    if (sessionFactory != null) {
      return;
    }

    log.debug("Initializing session factory");

    // TODO: mapping is automatically generated and should depend on the
    // modules/tables
    // which are actually used
    // NOTE: reads the hibernate.properties in the root of the classpath
    try {
      configuration = buildConfiguration();
      mapModel(configuration);
      setInterceptor(configuration);

      final Properties properties = getOpenbravoProperties();
      bbddUser = properties.getProperty(AvailableSettings.USER);
      configuration.addProperties(properties);

      // second-level caching is disabled for now because not all data
      // access and updates go through hibernate.
      // TODO: move to configuration file
      configuration.getProperties().setProperty(AvailableSettings.USE_SECOND_LEVEL_CACHE, "false");
      configuration.getProperties().setProperty(AvailableSettings.USE_QUERY_CACHE, "false");
      configuration.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, "LEGACY");
      configuration.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE, "50");
      configuration.getProperties().setProperty(AvailableSettings.STATEMENT_BATCH_SIZE, "10");
      configuration.getProperties().setProperty(AvailableSettings.STATEMENT_FETCH_SIZE, "50");
      configuration.getProperties().setProperty(AvailableSettings.JPA_VALIDATION_MODE, "NONE");
      configuration.getProperties().setProperty(AvailableSettings.CHECK_NULLABILITY, "false");
      // TODO: consider setting isolation level explicitly
      configuration.getProperties().setProperty(Environment.ISOLATION,
      "" + Connection.TRANSACTION_READ_COMMITTED);

      //registerSqlFunctions();

      final DalSessionFactory dalSessionFactory = OBProvider.getInstance()
          .get(DalSessionFactory.class);

      configuration.getStandardServiceRegistryBuilder().applySettings(configuration.getProperties());
      StandardServiceRegistry serviceRegistry = configuration.getStandardServiceRegistryBuilder().build();
      initializeServices(serviceRegistry);

      configuration.registerTypeOverride(org.openbravo.base.session.OBYesNoType.INSTANCE);
      SessionFactory delegateSessionFactory = configuration.buildSessionFactory(serviceRegistry);
      dalSessionFactory.setDelegateSessionFactory(delegateSessionFactory);

      sessionFactory = dalSessionFactory;

      log.debug("Session Factory initialized");
    } catch (final OBException obex) {
      throw obex;
    } catch (final Exception t) {
      throw new OBException(t);
    }
  }

  /**
   * Creates the Configuration instance used to build the SessionFactory. It can be extended by the
   * subclasses for example if they need to register a Service Provider Interface to customize the
   * Hibernate internals.
   * 
   * @return a {@code Configuration} instance used to build the {@code SessionFactory}
   */
  protected Configuration buildConfiguration() {
    return new Configuration();
  }

  protected void initializeServices(StandardServiceRegistry serviceRegistry) {
    for (Class<? extends Service> service : getServices()) {
      serviceRegistry.getService(service);
    }
  }

  protected List<Class<? extends Service>> getServices() {
    return Collections.emptyList();
  }

//Ya en Hibernate 6 las funciones SQL se registran en el dialecto, no en Configuration.
//Y como tú ya tienes OBOracle10gDialect, ahí mismo puedes registrarlas (por ejemplo, to_number, etc.).
//  private void registerSqlFunctions() {
//    Map<String, SQLFunction> sqlFunctions = getSQLFunctions();
//    if (sqlFunctions == null || sqlFunctions.isEmpty()) {
//      return;
//    }
//    for (Entry<String, SQLFunction> entry : sqlFunctions.entrySet()) {
//      log.debug("Registering SQL function: {}", entry.getKey());
//      configuration.addSqlFunction(entry.getKey(), entry.getValue());
//    }
//  }

//  protected Map<String, SQLFunction> getSQLFunctions() {
//    return Collections.emptyMap();
//  }

  public void closeHibernatePool() {
    ConnectionProvider hibernatePool = sessionFactory.getSessionFactoryOptions()
        .getServiceRegistry()
        .getService(ConnectionProvider.class);
    if (hibernatePool != null && hibernatePool instanceof DriverManagerConnectionProviderImpl) {
      ((DriverManagerConnectionProviderImpl) hibernatePool).stop();
    }
  }

  protected abstract void mapModel(Configuration theConfiguration);

  protected Properties getOpenbravoProperties() {
    final Properties obProps = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    if (obProps == null) {
      return new Properties();
    }

    Properties props = new Properties();
    if (obProps.getProperty("bbdd.rdbms") != null) {
      if (obProps.getProperty("bbdd.rdbms").equals("POSTGRE")) {
        props = getPostgresHbProps(obProps);
      } else {
        props = getOracleHbProps(obProps);
      }
    }
    addCommonHibernateProperties(props, obProps);

    return props;
  }

  @SuppressWarnings("unchecked")
  private void addCommonHibernateProperties(Properties properties, Properties allProperties) {
    Enumeration<String> allPropertyNames = (Enumeration<String>) allProperties.propertyNames();
    while (allPropertyNames.hasMoreElements()) {
      String propertyName = allPropertyNames.nextElement();
      if (propertyName.startsWith("hibernate.")) {
        properties.put(propertyName, allProperties.getProperty(propertyName));
      }
    }
  }

  private Properties getPostgresHbProps(Properties obProps) {
    isPostgresDatabase = true;
    final Properties props = new Properties();
    props.setProperty(AvailableSettings.DIALECT, PostgreSQLDialect.class.getName());
    if (isJNDIModeOn(obProps)) {
      setJNDI(obProps, props);
    } else {
      props.setProperty(AvailableSettings.DRIVER, "org.postgresql.Driver");
      props.setProperty(AvailableSettings.URL,
          obProps.getProperty("bbdd.url") + "/" + obProps.getProperty("bbdd.sid"));

      props.setProperty(AvailableSettings.USER, obProps.getProperty("bbdd.user"));
      props.setProperty(AvailableSettings.PASS, obProps.getProperty("bbdd.password"));
    }
    return props;
  }

  private Properties getOracleHbProps(Properties obProps) {
    isPostgresDatabase = false;
    final Properties props = new Properties();
    props.setProperty(AvailableSettings.DIALECT, OBOracle10gDialect.class.getName());
    if (isJNDIModeOn(obProps)) {
      setJNDI(obProps, props);
    } else {
      props.setProperty(AvailableSettings.DRIVER, "oracle.jdbc.driver.OracleDriver");
      props.setProperty(AvailableSettings.URL, obProps.getProperty("bbdd.url"));
      props.setProperty(AvailableSettings.USER, obProps.getProperty("bbdd.user"));
      props.setProperty(AvailableSettings.PASS, obProps.getProperty("bbdd.password"));
    }
    return props;
  }

  private void setJNDI(Properties obProps, Properties hbProps) {
    log.info("Using JNDI with resource name-> {}", obProps.getProperty("JNDI.resourceName"));
    hbProps.setProperty(AvailableSettings.DATASOURCE,
        "java:/comp/env/" + obProps.getProperty("JNDI.resourceName"));
  }

  /**
   * jndi should only be used if the application is running in a webcontainer in all other cases
   * (ant etc.) jndi should not be used, but the direct openbravo.properties should be used.
   */
  public static boolean isJNDIModeOn(Properties obProps) {
    if (!isRunningInWebContainer()) {
      return false;
    }
    return ("yes".equals(obProps.getProperty("JNDI.usage")) ? true : false);
  }

  protected void setInterceptor(Configuration configuration) {
  }

  /**
   * Returns a query which is specific for Postgres or Oracle.
   * 
   * @return depending on the Openbravo.properties a Postgres or Oracle specific query is returned
   */
  public String getUniqueConstraintQuery() {
    if (isPostgresDatabase) {
      return UNIQUE_CONSTRAINT_QUERY_POSTGRES;
    }
    return UNIQUE_CONSTRAINT_QUERY_ORACLE.replace("${bbdd.user}", bbddUser);
  }

  /**
   * Returns a query for column metadata for not-null/required which is specific for Postgres or
   * Oracle.
   * 
   * @return depending on the Openbravo.properties a Postgres or Oracle specific query is returned
   */
  public String getColumnMetadataQuery() {
    if (isPostgresDatabase) {
      return COLUMN_QUERY_POSTGRES;
    }

    return COLUMN_QUERY_ORACLE.replace("${bbdd.user}", bbddUser);
  }
}
