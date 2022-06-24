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
 * All portions are Copyright (C) 2014-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.apachejdbcconnectionpool;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolExhaustedException;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.database.ExternalConnectionPool;
import org.openbravo.database.PoolInterceptorProvider;
import org.openbravo.jmx.MBeanRegistry;

/**
 * JdbcExternalConnectionPool manages all the functionality of the Apache JDBC Connection Pool. This
 * class can handle different pools (data sources) at the same time. All these pools are eventually
 * making use of the Apache JDBC Connection Pool. This class provides convenience methods to get a
 * connection from a pool, close the different pools and other actions.
 */
public class JdbcExternalConnectionPool extends ExternalConnectionPool {
  final static private Logger log = LogManager.getLogger();

  private Map<String, DataSource> availableDataSources = null;
  private DataSource defaultDataSource = null;

  private static final List<Class<? extends Exception>> EXHAUSTED_EXCEPTION = Arrays
      .asList(PoolExhaustedException.class);

  /**
   * This method loads all the interceptors of Apache JDBC Connection Pool injected with weld.
   */
  @Override
  public void loadInterceptors(List<PoolInterceptorProvider> interceptors) {
    String currentInterceptors = this.getDataSource().getJdbcInterceptors();
    for (PoolInterceptorProvider interceptor : interceptors) {
      currentInterceptors += interceptor.getPoolInterceptorsClassNames();
    }
    for (DataSource ds : availableDataSources.values()) {
      ds.setJdbcInterceptors(currentInterceptors);
    }
  }

  /**
   * Gets the default data source, which was obtained from the Apache JDBC Connection Pool.
   * 
   * @return the {@code DataSource} of the default pool
   */
  public DataSource getDataSource() {
    if (defaultDataSource == null) {
      defaultDataSource = availableDataSources.get(DEFAULT_POOL);
    }
    return defaultDataSource;
  }

  /**
   * This method provides a connection from the default pool. The connection pool is initialized in
   * the first call to this method.
   * 
   * a {@code Connection} retrieved from the default pool
   */
  @Override
  public Connection getConnection() {
    if (availableDataSources == null) {
      initPool();
    }
    return getConnectionFromDS(getDataSource());
  }

  /**
   * This method provides a connection from the pool whose name is specified as parameter. The
   * connection pool is initialized in the first call to this method.
   * 
   * @param poolName
   *          the name of the pool used to retrieve the connection
   * @return a {@code Connection} retrieved from the pool passed as parameter
   */
  @Override
  public Connection getConnection(String poolName) {
    if (availableDataSources == null) {
      initPool();
    }
    DataSource ds = getDataSourceByName(poolName);
    return getConnectionFromDS(ds);
  }

  private DataSource getDataSourceByName(String poolName) {
    if (DEFAULT_POOL.equals(poolName)) {
      return getDataSource();
    }
    DataSource dataSource = availableDataSources.get(poolName);
    if (dataSource == null) {
      log.debug(
          "Connection pool with name {} is not available, using default connection pool to retrieve the connection",
          poolName);
      dataSource = getDataSource();
    }
    return dataSource;
  }

  private Connection getConnectionFromDS(DataSource datasource) {
    Connection connection = null;
    try {
      connection = datasource.getConnection();

      // All connections are setting autoCommit to true. DAL is taking into account his logical and
      // DAL is setting autoCommit to false to maintain transactional way of working.
      connection.setAutoCommit(true);
    } catch (Exception e) {
      log.error("Error while retrieving connection: ", e);
      throw new OBException(e);
    }
    return connection;
  }

  private void initPool() {
    availableDataSources = new HashMap<>(1);
    DataSource defaultDS = new DataSource();
    defaultDS.setPoolProperties(getPoolProperties(""));
    availableDataSources.put(DEFAULT_POOL, defaultDS);
    if (isReadOnlyPoolDefined()) {
      PoolProperties p = getPoolProperties("readonly");
      p.setDefaultReadOnly(true);

      log.info("Read only pool: " + p.getUrl());

      DataSource ro = new DataSource();
      ro.setPoolProperties(p);
      availableDataSources.put(READONLY_POOL, ro);
    }

    for (Entry<String, DataSource> dse : availableDataSources.entrySet()) {
      DataSource ds = dse.getValue();
      try {
        if (ds.isJmxEnabled()) {
          // pool needs to be created before it's registered
          ds.createPool();
          MBeanRegistry.registerMBean("Pool-" + dse.getKey(), ds.getPool().getJmxPool());
        }
      } catch (Exception e) {
        log.error("Error creating pool {}", dse.getKey(), e);
      }
    }
  }

  private PoolProperties getPoolProperties(String poolName) {
    Properties props = OBPropertiesProvider.getInstance().getOpenbravoProperties();

    PoolProperties poolProperties = new PoolProperties();

    poolProperties.setJdbcInterceptors("org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"
        + "org.openbravo.apachejdbcconnectionpool.ConnectionInitializerInterceptor;");

    if (SessionFactoryController.isJNDIModeOn(props)) {
      try {
        Context initctx = new InitialContext();
        Context ctx = (Context) initctx.lookup("java:/comp/env");
        javax.sql.DataSource ds = (javax.sql.DataSource) ctx
            .lookup(props.getProperty("JNDI.resourceName"));
        poolProperties.setDataSource(ds);
        return poolProperties;
      } catch (Exception e) {
        log.error("Error trying to get JNDI datasource, trying to get direct DB connection", e);
        poolProperties = new PoolProperties();
      }
    }

    String obUrl = getStringProperty(props, "bbdd.url", poolName);
    String sid = getStringProperty(props, "bbdd.sid", poolName);
    String driver = getStringProperty(props, "bbdd.driver", poolName);
    String username = getStringProperty(props, "bbdd.user", poolName);
    String password = getStringProperty(props, "bbdd.password", poolName);
    String rbdms = getStringProperty(props, "bbdd.rdbms", poolName);

    if ("POSTGRE".equals(rbdms)) {
      String connectionURL = obUrl + "/" + sid;
      connectionURL += (connectionURL.contains("?") ? "&" : "?") + "ApplicationName=openbravo";
      poolProperties.setUrl(connectionURL);
    } else {
      poolProperties.setUrl(obUrl);
    }
    poolProperties.setDriverClassName(driver);
    poolProperties.setUsername(username);
    poolProperties.setPassword(password);

    if (getStringProperty(props, "db.pool.initialSize", poolName) != null) {
      poolProperties.setInitialSize(getIntProperty(props, "db.pool.initialSize", poolName));
    }
    if (getStringProperty(props, "db.pool.maxActive", poolName) != null) {
      poolProperties.setMaxActive(getIntProperty(props, "db.pool.maxActive", poolName));
    }
    if (getStringProperty(props, "db.pool.minIdle", poolName) != null) {
      poolProperties.setMinIdle(getIntProperty(props, "db.pool.minIdle", poolName));
    }
    if (getStringProperty(props, "db.pool.timeBetweenEvictionRunsMillis", poolName) != null) {
      poolProperties.setTimeBetweenEvictionRunsMillis(
          getIntProperty(props, "db.pool.timeBetweenEvictionRunsMillis", poolName));
    }
    if (getStringProperty(props, "db.pool.minEvictableIdleTimeMillis", poolName) != null) {
      poolProperties.setMinEvictableIdleTimeMillis(
          getIntProperty(props, "db.pool.minEvictableIdleTimeMillis", poolName));
    }
    if (getStringProperty(props, "db.pool.removeAbandoned", poolName) != null) {
      poolProperties
          .setRemoveAbandoned(getBooleanProperty(props, "db.pool.removeAbandoned", poolName));
    }
    if (getStringProperty(props, "db.pool.testWhileIdle", poolName) != null) {
      poolProperties.setTestWhileIdle(getBooleanProperty(props, "db.pool.testWhileIdle", poolName));
    }
    if (getStringProperty(props, "db.pool.testOnBorrow", poolName) != null) {
      poolProperties.setTestOnBorrow(getBooleanProperty(props, "db.pool.testOnBorrow", poolName));
    }
    if (getStringProperty(props, "db.pool.testOnReturn", poolName) != null) {
      poolProperties.setTestOnReturn(getBooleanProperty(props, "db.pool.testOnReturn", poolName));
    }
    if (getStringProperty(props, "db.pool.validationInterval", poolName) != null) {
      poolProperties
          .setValidationInterval(getIntProperty(props, "db.pool.validationInterval", poolName));
    }
    if (getStringProperty(props, "db.pool.validationQuery", poolName) != null) {
      poolProperties
          .setValidationQuery(getStringProperty(props, "db.pool.validationQuery", poolName));
    }
    if (getStringProperty(props, "db.pool.defaultTransactionIsolation", poolName) != null) {
      poolProperties.setDefaultTransactionIsolation(
          getIntProperty(props, "db.pool.defaultTransactionIsolation", poolName));
    }
    if (getStringProperty(props, "db.pool.maxIdle", poolName) != null) {
      poolProperties.setMaxIdle(getIntProperty(props, "db.pool.maxIdle", poolName));
    }
    if (getStringProperty(props, "db.pool.maxWait", poolName) != null) {
      poolProperties.setMaxWait(getIntProperty(props, "db.pool.maxWait", poolName));
    }
    if (getStringProperty(props, "db.pool.numTestsPerEvictionRun", poolName) != null) {
      poolProperties.setNumTestsPerEvictionRun(
          getIntProperty(props, "db.pool.numTestsPerEvictionRun", poolName));
    }
    if (getStringProperty(props, "db.pool.removeAbandonedTimeout", poolName) != null) {
      poolProperties.setRemoveAbandonedTimeout(
          getIntProperty(props, "db.pool.removeAbandonedTimeout", poolName));
    }
    if (getStringProperty(props, "db.pool.accessToUnderlyingConnectionAllowed", poolName) != null) {
      poolProperties.setAccessToUnderlyingConnectionAllowed(
          getBooleanProperty(props, "db.pool.accessToUnderlyingConnectionAllowed", poolName));
    }
    if (getStringProperty(props, "db.pool.defaultAutoCommit", poolName) != null) {
      poolProperties
          .setDefaultAutoCommit(getBooleanProperty(props, "db.pool.defaultAutoCommit", poolName));
    }
    if (getStringProperty(props, "db.pool.defaultReadOnly", poolName) != null) {
      poolProperties
          .setDefaultReadOnly(getBooleanProperty(props, "db.pool.defaultReadOnly", poolName));
    }
    if (getStringProperty(props, "db.pool.logAbandoned", poolName) != null) {
      poolProperties.setLogAbandoned(getBooleanProperty(props, "db.pool.logAbandoned", poolName));
    }
    if (getStringProperty(props, "db.pool.suspectTimeout", poolName) != null) {
      poolProperties.setSuspectTimeout(getIntProperty(props, "db.pool.suspectTimeout", poolName));
    }
    if (getStringProperty(props, "db.pool.testOnConnect", poolName) != null) {
      poolProperties.setTestOnConnect(getBooleanProperty(props, "db.pool.testOnConnect", poolName));
    }
    if (getStringProperty(props, "db.pool.connectionProperties", poolName) != null) {
      poolProperties.setConnectionProperties(
          getStringProperty(props, "db.pool.connectionProperties", poolName));
    }
    if (getStringProperty(props, "db.pool.defaultCatalog", poolName) != null) {
      poolProperties
          .setDefaultCatalog(getStringProperty(props, "db.pool.defaultCatalog", poolName));
    }
    if (getStringProperty(props, "db.pool.validatorClassName", poolName) != null) {
      poolProperties
          .setValidatorClassName(getStringProperty(props, "db.pool.validatorClassName", poolName));
    }
    if (getStringProperty(props, "db.pool.initSQL", poolName) != null) {
      poolProperties.setInitSQL(getStringProperty(props, "db.pool.initSQL", poolName));
    }
    if (getStringProperty(props, "db.pool.name", poolName) != null) {
      poolProperties.setName(getStringProperty(props, "db.pool.name", poolName));
    }
    if (getStringProperty(props, "db.pool.jmxEnabled", poolName) != null) {
      poolProperties.setJmxEnabled(getBooleanProperty(props, "db.pool.jmxEnabled", poolName));
    }
    return poolProperties;
  }

  private boolean isReadOnlyPoolDefined() {
    // to define readonly pool, its url is enough
    return OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .containsKey("bbdd.readonly.url");
  }

  private boolean getBooleanProperty(Properties properties, String propertyName, String poolName) {
    return "true".equals(getStringProperty(properties, propertyName, poolName));
  }

  private int getIntProperty(Properties properties, String propertyName, String poolName) {
    return Integer.parseInt(getStringProperty(properties, propertyName, poolName).trim());
  }

  /** gets specific property for pool if present, default property if not */
  private String getStringProperty(Properties props, String key, String poolName) {
    if (StringUtils.isEmpty(poolName)) {
      // return default property if the pool name is not provided
      return props.getProperty(key);
    }
    String poolSpecificKey = key.replaceFirst("\\.", "." + poolName + ".");
    return props.containsKey(poolSpecificKey) ? props.getProperty(poolSpecificKey)
        : props.getProperty(key);
  }

  /**
   * This method closes all the data sources retrieved from the Apache JDBC Connection Pool.
   */
  @Override
  public void closePool() {
    for (DataSource ds : availableDataSources.values()) {
      if (ds != null) {
        // Closes the pool and all idle connections. true parameter is for close the active
        // connections too.
        ds.close(true);
      }
    }
    super.closePool();
  }

  @Override
  protected List<Class<? extends Exception>> getExhaustedExceptions() {
    return EXHAUSTED_EXCEPTION;
  }

}
