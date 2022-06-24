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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling.quartz;

import java.sql.Connection;
import java.sql.SQLException;

import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.exception.NoConnectionAvailableException;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.utils.ConnectionProvider;

/**
 * ConnectionProvider for quartz to allow Quartz to use Openbravo managed Database connections
 */
public class QuartzConnectionProvider implements ConnectionProvider {

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection;
    try {
      // By default use context connection provider
      org.openbravo.database.ConnectionProvider pool = ConnectionProviderContextListener.getPool();
      if (pool != null) {
        connection = pool.getTransactionConnection();
      } else {
        // In case Tomcat has not been initialized, use DalConnectionProvider, e.g. For unit-testing
        connection = new DalConnectionProvider().getTransactionConnection();
      }
      connection.setAutoCommit(false);
    } catch (NoConnectionAvailableException ex) {
      throw new SQLException(ex);
    }
    return connection;
  }

  @Override
  public void initialize() throws SQLException {
    // The Openbravo connection provider is initialized by the Servlet Context listener
  }

  @Override
  public void shutdown() throws SQLException {
    // The Openbravo connection provider is shutdown by the Servlet Context listener
  }

}
