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

import static org.quartz.impl.jdbcjobstore.Constants.COL_INSTANCE_NAME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_LAST_CHECKIN_TIME;
import static org.quartz.impl.jdbcjobstore.Constants.COL_SCHEDULER_NAME;
import static org.quartz.impl.jdbcjobstore.Constants.TABLE_SCHEDULER_STATE;
import static org.quartz.impl.jdbcjobstore.StdJDBCConstants.SCHED_NAME_SUBST;
import static org.quartz.impl.jdbcjobstore.StdJDBCConstants.TABLE_PREFIX_SUBST;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface for Openbravo quartz JDBCDelegates
 */
public interface OpenbravoJDBCDelegate {

  String COL_SCHEDULER_STATUS = "STATUS";

  String SCHEDULER_STATUS_STANDBY = "STANDBY";
  String SCHEDULER_STATUS_STARTED = "STARTED";

  String UPDATE_SCHEDULER_STATE_EXTENDED = "UPDATE " + TABLE_PREFIX_SUBST + TABLE_SCHEDULER_STATE
      + " SET " + COL_LAST_CHECKIN_TIME + " = ?, " + COL_SCHEDULER_STATUS + " = ? " + " WHERE "
      + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND " + COL_INSTANCE_NAME + " = ?";

  String COUNT_STARTED_SCHEDULER_INSTANCES = "SELECT count(*) " + " FROM " + TABLE_PREFIX_SUBST
      + TABLE_SCHEDULER_STATE + " WHERE " + COL_SCHEDULER_NAME + " = " + SCHED_NAME_SUBST + " AND "
      + COL_SCHEDULER_STATUS + " = ?";

  /**
   * Updates scheduler status to provided status
   * 
   * @param conn
   *          Connection to DB
   * @param theInstanceId
   *          Instance id of the scheduler
   * @param checkInTime
   *          Time stamp when status has been updated
   * @param status
   *          Target status
   * @return Number of columns updated in DB (more than 0, scheduler status updated)
   * @throws SQLException
   *           In case of SQL error
   */
  int updateSchedulerStatus(Connection conn, String theInstanceId, long checkInTime, String status)
      throws SQLException;

  /**
   * Gets if scheduler has been started in the cluster
   * 
   * @param conn
   *          Connection to DB
   * @return true if scheduler has been started, false otherwise
   * @throws SQLException
   *           In case of SQL error
   */
  boolean schedulersStarted(Connection conn) throws SQLException;

}
