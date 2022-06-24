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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling.quartz;

import static org.openbravo.scheduling.quartz.OpenbravoJDBCPersistenceSupport.getBooleanValue;
import static org.openbravo.scheduling.quartz.OpenbravoJDBCPersistenceSupport.setBooleanValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.openbravo.scheduling.ClusterInstanceProcessAccess;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.CronTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.SimpleTriggerPersistenceDelegate;
import org.quartz.impl.jdbcjobstore.oracle.OracleDelegate;

/**
 * Quartz Openbravo JDBC delegate that handles Oracle database connections
 */
public class OpenbravoOracleJDBCDelegate extends OracleDelegate implements OpenbravoJDBCDelegate {

  @Override
  protected void addDefaultTriggerPersistenceDelegates() {
    addTriggerPersistenceDelegate(new SimpleTriggerPersistenceDelegate());
    addTriggerPersistenceDelegate(new CronTriggerPersistenceDelegate());
    // Handling of Bool fields is not extensible in TriggerPersistenceDelegates that use
    // extended properties so those classes are replaced with Openbravo specific ones
    addTriggerPersistenceDelegate(new OpenbravoDailyTimeIntervalTriggerPersistenceDelegate());
    addTriggerPersistenceDelegate(new OpenbravoCalendarIntervalTriggerPersistenceDelegate());
  }

  /**
   * Use 'Y' and 'N' Varchar fields instead of Bool to be consistent with Openbravo standards and to
   * allow DBSourceManager to manage the data structures
   */
  @Override
  protected void setBoolean(PreparedStatement ps, int index, boolean val) throws SQLException {
    setBooleanValue(ps, index, val);
  }

  /**
   * Use 'Y' and 'N' Varchar fields instead of Bool to be consistent with Openbravo standards and to
   * allow DBSourceManager to manage the data structures
   */
  @Override
  protected boolean getBoolean(ResultSet rs, String columnName) throws SQLException {
    return getBooleanValue(rs, columnName);
  }

  /**
   * Use 'Y' and 'N' Varchar fields instead of Bool to be consistent with Openbravo standards and to
   * allow DBSourceManager to manage the data structures
   */
  @Override
  protected boolean getBoolean(ResultSet rs, int columnIndex) throws SQLException {
    return getBooleanValue(rs, columnIndex);
  }

  @Override
  public int updateSchedulerStatus(Connection conn, String theInstanceId, long checkInTime,
      String status) throws SQLException {
    try (PreparedStatement ps = conn.prepareStatement(rtp(UPDATE_SCHEDULER_STATE_EXTENDED))) {
      ps.setLong(1, checkInTime);
      ps.setString(2, status);
      ps.setString(3, theInstanceId);

      return ps.executeUpdate();
    }
  }

  @Override
  public boolean schedulersStarted(Connection conn) throws SQLException {
    ResultSet rs = null;

    try (PreparedStatement ps = conn.prepareStatement(rtp(COUNT_STARTED_SCHEDULER_INSTANCES))) {

      ps.setString(1, SCHEDULER_STATUS_STARTED);
      rs = ps.executeQuery();

      if (rs.next()) {
        return rs.getInt(1) > 0;
      }

      throw new SQLException("No started instances count returned.");
    } finally {
      closeResultSet(rs);
    }
  }

  @Override
  public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan,
      long noEarlierThan, int maxCount) throws SQLException {
    List<TriggerKey> triggerKeys = super.selectTriggerToAcquire(conn, noLaterThan, noEarlierThan,
        maxCount);
    ClusterInstanceProcessAccess instanceProcessAccess = new ClusterInstanceProcessAccess();
    return instanceProcessAccess.removeProcessesBannedFromCurrentClusterInstance(this, conn,
        triggerKeys);
  }
}
