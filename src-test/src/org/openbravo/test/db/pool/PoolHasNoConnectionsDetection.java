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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 ************************************************************************
 */

package org.openbravo.test.db.pool;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.sql.SQLException;

import org.apache.tomcat.jdbc.pool.PoolExhaustedException;
import org.junit.Test;
import org.openbravo.apachejdbcconnectionpool.JdbcExternalConnectionPool;
import org.openbravo.base.exception.OBException;

/** Test cases covering checking of Exceptions thrown when DB pool gets out of connections */
public class PoolHasNoConnectionsDetection {
  private JdbcExternalConnectionPool pool = new JdbcExternalConnectionPool();

  @Test
  public void tomcatPoolExhaustedShouldBeDetected() {
    assertThat("PoolExhaustedException should be detected as no connections exception",
        pool.hasNoConnections(new PoolExhaustedException()), is(true));
  }

  @Test
  public void tomcatPoolNestedExhaustedShouldBeDetected() {
    Exception nestedException = new OBException(new PoolExhaustedException());
    assertThat("Nested PoolExhaustedException should be detected as no connections exception",
        pool.hasNoConnections(nestedException), is(true));

    Exception nested2LevelException = new Exception(nestedException);
    assertThat(
        "Nested 2 levels PoolExhaustedException should be detected as no connections exception",
        pool.hasNoConnections(nested2LevelException), is(true));
  }

  @Test
  public void pgOutOfConnectionsShouldBeDetected() {
    SQLException se = new SQLException("PG cannot create DB connection", "53300");
    assertThat("Cannot create DB connections should be detected as no connections exception",
        pool.hasNoConnections(se), is(true));
  }

  @Test
  public void oraOutOfConnectionsShouldBeDetected() {
    SQLException se = new SQLException("ORA cannot create DB connection", "66000");
    assertThat("Cannot create DB connections should be detected as no connections exception",
        pool.hasNoConnections(se), is(true));
  }

  @Test
  public void otherExceptionsShoulNotBeDetected() {
    assertThat("Other Exception detected as no connections",
        pool.hasNoConnections(new SQLException()), is(false));
  }

}
