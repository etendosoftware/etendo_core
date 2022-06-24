/*
 ************************************************************************************
 * Copyright (C) 2001-2010 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.database;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.pool.ObjectPool;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.exception.PoolNotFoundException;

public class CPStandAlone implements ConnectionProvider {
  protected ConnectionProviderImpl myPool;

  public CPStandAlone(String xmlPoolFile) {
    if (myPool == null) {
      try {
        myPool = new ConnectionProviderImpl(xmlPoolFile);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /*
   * Database access utilities
   */
  @SuppressWarnings("unused")
  // to be used in pool status service
  private ObjectPool getPool(String poolName) throws PoolNotFoundException {
    if (myPool == null) {
      throw new PoolNotFoundException(poolName + " not found");
    } else {
      return myPool.getPool(poolName);
    }
  }

  @SuppressWarnings("unused")
  // to be used in pool status service
  private ObjectPool getPool() throws PoolNotFoundException {
    if (myPool == null) {
      throw new PoolNotFoundException("Default pool not found");
    } else {
      return myPool.getPool();
    }
  }

  @Override
  public Connection getConnection() throws NoConnectionAvailableException {
    return (myPool.getConnection());
  }

  @Override
  public String getRDBMS() {
    return (myPool.getRDBMS());
  }

  @Override
  public Connection getTransactionConnection() throws NoConnectionAvailableException, SQLException {
    return myPool.getTransactionConnection();
  }

  @Override
  public void releaseCommitConnection(Connection conn) throws SQLException {
    myPool.releaseCommitConnection(conn);
  }

  @Override
  public void releaseRollbackConnection(Connection conn) throws SQLException {
    myPool.releaseRollbackConnection(conn);
  }

  @Override
  public PreparedStatement getPreparedStatement(String poolName, String strSql) throws Exception {
    return myPool.getPreparedStatement(poolName, strSql);
  }

  @Override
  public PreparedStatement getPreparedStatement(String strSql) throws Exception {
    return myPool.getPreparedStatement(strSql);
  }

  @Override
  public PreparedStatement getPreparedStatement(Connection conn, String strSql)
      throws SQLException {
    return myPool.getPreparedStatement(conn, strSql);
  }

  @Override
  public void releasePreparedStatement(PreparedStatement preparedStatement) throws SQLException {
    myPool.releasePreparedStatement(preparedStatement);
  }

  @Override
  public Statement getStatement(String poolName) throws Exception {
    return myPool.getStatement(poolName);
  }

  @Override
  public Statement getStatement() throws Exception {
    return myPool.getStatement();
  }

  @Override
  public Statement getStatement(Connection conn) throws SQLException {
    return myPool.getStatement(conn);
  }

  @Override
  public void releaseStatement(Statement statement) throws SQLException {
    myPool.releaseStatement(statement);
  }

  @Override
  public void releaseTransactionalStatement(Statement statement) throws SQLException {
    myPool.releaseTransactionalStatement(statement);
  }

  @Override
  public void releaseTransactionalPreparedStatement(PreparedStatement preparedStatement)
      throws SQLException {
    myPool.releaseTransactionalPreparedStatement(preparedStatement);
  }

  @Override
  public CallableStatement getCallableStatement(String poolName, String strSql) throws Exception {
    return myPool.getCallableStatement(poolName, strSql);
  }

  @Override
  public CallableStatement getCallableStatement(String strSql) throws Exception {
    return myPool.getCallableStatement(strSql);
  }

  @Override
  public CallableStatement getCallableStatement(Connection conn, String strSql)
      throws SQLException {
    return myPool.getCallableStatement(conn, strSql);
  }

  @Override
  public void releaseCallableStatement(CallableStatement callableStatement) throws SQLException {
    myPool.releaseCallableStatement(callableStatement);
  }

  @Override
  public void destroy() {
    try {
      myPool.destroy();
      myPool = null;
    } catch (Exception ex) {
    }
  }

  @Override
  public String getStatus() {
    // TODO Auto-generated method stub
    return null;
  }
}
