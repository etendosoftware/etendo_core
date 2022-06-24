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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_callouts;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.application.window.servlet.CalloutServletConfig;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * A connection provider which is used on current SimpleCallout infrastructure (see
 * {@link SimpleCallout}).
 *
 * This implementation is required to maintain backwards compatibility in SimpleCallouts
 * implementations.
 *
 * @author inigo.sanchez
 */
public class DelegateConnectionProvider implements ConnectionProvider {
  protected ConnectionProvider myPool;
  protected Logger log4j = LogManager.getLogger(this.getClass());

  public void init(CalloutServletConfig config) {
    myPool = getPool();
  }

  private ConnectionProvider getPool() {
    if (myPool == null) {
      myPool = new DalConnectionProvider(false);
    }
    return myPool;
  }

  @Override
  public Connection getConnection() throws NoConnectionAvailableException {
    return getPool().getConnection();
  }

  @Override
  public String getRDBMS() {
    return getPool().getRDBMS();
  }

  @Override
  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    return getPool().getTransactionConnection();
  }

  @Override
  public void releaseCommitConnection(Connection conn) throws SQLException {
    getPool().releaseCommitConnection(conn);
  }

  @Override
  public void releaseRollbackConnection(Connection conn) throws SQLException {
    getPool().releaseRollbackConnection(conn);
  }

  @Override
  public PreparedStatement getPreparedStatement(String poolName, String strSql) throws Exception {
    return getPool().getPreparedStatement(poolName, strSql);
  }

  @Override
  public PreparedStatement getPreparedStatement(String strSql) throws Exception {
    return getPool().getPreparedStatement(strSql);
  }

  @Override
  public PreparedStatement getPreparedStatement(Connection conn, String strSql)
      throws SQLException {
    return getPool().getPreparedStatement(conn, strSql);
  }

  @Override
  public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    getPool().releasePreparedStatement(preparedStatement);
  }

  @Override
  public Statement getStatement(String poolName) throws Exception {
    return getPool().getStatement(poolName);
  }

  @Override
  public Statement getStatement() throws Exception {
    return getPool().getStatement();
  }

  @Override
  public Statement getStatement(Connection conn) throws SQLException {
    return getPool().getStatement(conn);
  }

  @Override
  public void releaseStatement(Statement statement) throws SQLException {
    getPool().releaseStatement(statement);
  }

  @Override
  public void releaseTransactionalStatement(Statement statement) throws SQLException {
    getPool().releaseTransactionalStatement(statement);
  }

  @Override
  public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
      throws SQLException {
    getPool().releaseTransactionalPreparedStatement(preparedStatement);
  }

  @Override
  public CallableStatement getCallableStatement(String poolName, String strSql) throws Exception {
    return getPool().getCallableStatement(poolName, strSql);
  }

  @Override
  public CallableStatement getCallableStatement(String strSql) throws Exception {
    return getPool().getCallableStatement(strSql);
  }

  @Override
  public CallableStatement getCallableStatement(Connection conn, String strSql)
      throws SQLException {
    return getPool().getCallableStatement(conn, strSql);
  }

  @Override
  public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
    getPool().releaseCallableStatement(callableStatement);
  }

  @Override
  public void destroy() throws Exception {
    getPool().destroy();
  }

  @Override
  public String getStatus() {
    return getPool().getStatus();
  }
}
