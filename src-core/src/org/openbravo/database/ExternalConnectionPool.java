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
 ************************************************************************
 */
package org.openbravo.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Abstract class that represents an external connection pool
 * 
 */
public abstract class ExternalConnectionPool {

  static Logger log = LogManager.getLogger();
  public static final String DEFAULT_POOL = "DEFAULT";
  public static final String READONLY_POOL = "RO";

  private static ExternalConnectionPool instance;

  private static final String PG_TOO_MANY_CONNECTIONS = "53300";
  private static final String ORA_CONNECTION_REFUSED = "66000";

  /**
   * 
   * @param externalConnectionPoolClassName
   *          The full class name of the external connection pool
   * @return An instance of the external connection pool
   * @throws ReflectiveOperationException
   */
  public static final synchronized ExternalConnectionPool getInstance(
      String externalConnectionPoolClassName) throws ReflectiveOperationException {
    if (instance == null) {
      instance = (ExternalConnectionPool) Class.forName(externalConnectionPoolClassName)
          .getDeclaredConstructor()
          .newInstance();
    }
    return instance;
  }

  /**
   * @return the singleton instance of the external connection pool
   */
  public final static ExternalConnectionPool getInstance() {
    return instance;
  }

  /**
   * If the external connection pool should be closed this method should be overwritten
   */
  public void closePool() {
    instance = null;
  }

  /**
   * If the external connection pool supports interceptors this method should be overwritten
   * 
   * @param interceptors
   *          List of PoolInterceptorProvider comprised of all the interceptors injected with Weld
   */
  public void loadInterceptors(List<PoolInterceptorProvider> interceptors) {
  }

  /**
   * @return A Connection from the external connection pool
   */
  public abstract Connection getConnection();

  /**
   * This method is intended to be overridden by external connection multi-pools. This kind of pools
   * can handle several datasources and this method allows to select from which of them the
   * connection should be taken.
   * 
   * @param poolName
   *          The name of the external connection pool
   * 
   * @return A Connection from the external connection pool whose name is passed as parameter
   * 
   */
  public Connection getConnection(String poolName) {
    return getConnection();
  }

  /** {@code Exception}s thrown when trying to create a new connection and pool is exhausted. */
  protected List<Class<? extends Exception>> getExhaustedExceptions() {
    return Collections.emptyList();
  }

  /** Checks if {@code Throwable} was caused by pool not having more connections. */
  public boolean hasNoConnections(Throwable t) {
    if (t == null) {
      return false;
    }

    boolean isOutOfPhysicalConns;
    if (t instanceof SQLException) {
      String state = ((SQLException) t).getSQLState();
      isOutOfPhysicalConns = PG_TOO_MANY_CONNECTIONS.equals(state)
          || ORA_CONNECTION_REFUSED.equals(state);
    } else {
      isOutOfPhysicalConns = false;
    }

    return isOutOfPhysicalConns
        || getExhaustedExceptions().stream().anyMatch(e -> e.isAssignableFrom(t.getClass()))
        || hasNoConnections(t.getCause());
  }
}
