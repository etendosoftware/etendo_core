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

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import org.openbravo.base.session.OBPropertiesProvider;
import org.quartz.Calendar;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.jdbcjobstore.FiredTriggerRecord;
import org.quartz.impl.jdbcjobstore.NoSuchDelegateException;
import org.quartz.impl.jdbcjobstore.SchedulerStateRecord;
import org.quartz.impl.jdbcjobstore.TriggerStatus;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.OperableTrigger;
import org.quartz.utils.Key;
import org.slf4j.Logger;

/**
 * Adds a wrapper DriverDelegate, that will wrap OpenbravoPostgreJDBCDelegate or
 * OpenbravoOracleJDBCDelegate depending on the value of bbdd.rdbms in Openbravo.properties
 */
public class OpenbravoDriverDelegate implements DriverDelegate {

  private DriverDelegate wrappedDriverDelegate;

  public OpenbravoDriverDelegate() {
    if ("POSTGRE"
        .equals(OBPropertiesProvider.getInstance().getOpenbravoProperties().get("bbdd.rdbms"))) {
      wrappedDriverDelegate = new OpenbravoPostgreJDBCDelegate();
    } else {
      wrappedDriverDelegate = new OpenbravoOracleJDBCDelegate();
    }

  }

  @Override
  public void initialize(Logger logger, String tablePrefix, String schedName, String instanceId,
      ClassLoadHelper classLoadHelper, boolean useProperties, String initString)
      throws NoSuchDelegateException {
    wrappedDriverDelegate.initialize(logger, tablePrefix, schedName, instanceId, classLoadHelper,
        useProperties, initString);
  }

  @Override
  public int updateTriggerStatesFromOtherStates(Connection conn, String newState, String oldState1,
      String oldState2) throws SQLException {
    return wrappedDriverDelegate.updateTriggerStatesFromOtherStates(conn, newState, oldState1,
        oldState2);
  }

  @Override
  public List<TriggerKey> selectMisfiredTriggers(Connection conn, long ts) throws SQLException {
    return wrappedDriverDelegate.selectMisfiredTriggers(conn, ts);
  }

  @Override
  public List<TriggerKey> selectMisfiredTriggersInState(Connection conn, String state, long ts)
      throws SQLException {
    return wrappedDriverDelegate.selectMisfiredTriggersInState(conn, state, ts);
  }

  @Override
  public boolean hasMisfiredTriggersInState(Connection conn, String state1, long ts, int count,
      List<TriggerKey> resultList) throws SQLException {
    return wrappedDriverDelegate.hasMisfiredTriggersInState(conn, state1, ts, count, resultList);
  }

  @Override
  public int countMisfiredTriggersInState(Connection conn, String state1, long ts)
      throws SQLException {
    return wrappedDriverDelegate.countMisfiredTriggersInState(conn, state1, ts);
  }

  @Override
  public List<TriggerKey> selectMisfiredTriggersInGroupInState(Connection conn, String groupName,
      String state, long ts) throws SQLException {
    return wrappedDriverDelegate.selectMisfiredTriggersInGroupInState(conn, groupName, state, ts);
  }

  @Override
  public List<OperableTrigger> selectTriggersForRecoveringJobs(Connection conn)
      throws SQLException, IOException, ClassNotFoundException {
    return wrappedDriverDelegate.selectTriggersForRecoveringJobs(conn);
  }

  @Override
  public int deleteFiredTriggers(Connection conn) throws SQLException {
    return wrappedDriverDelegate.deleteFiredTriggers(conn);
  }

  @Override
  public int deleteFiredTriggers(Connection conn, String instanceId) throws SQLException {
    return wrappedDriverDelegate.deleteFiredTriggers(conn, instanceId);
  }

  @Override
  public int insertJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
    return wrappedDriverDelegate.insertJobDetail(conn, job);
  }

  @Override
  public int updateJobDetail(Connection conn, JobDetail job) throws IOException, SQLException {
    return wrappedDriverDelegate.updateJobDetail(conn, job);
  }

  @Override
  public List<TriggerKey> selectTriggerKeysForJob(Connection conn, JobKey jobKey)
      throws SQLException {
    return wrappedDriverDelegate.selectTriggerKeysForJob(conn, jobKey);
  }

  @Override
  public int deleteJobDetail(Connection conn, JobKey jobKey) throws SQLException {
    return wrappedDriverDelegate.deleteJobDetail(conn, jobKey);
  }

  @Override
  public boolean isJobNonConcurrent(Connection conn, JobKey jobKey) throws SQLException {
    return wrappedDriverDelegate.isJobNonConcurrent(conn, jobKey);
  }

  @Override
  public boolean jobExists(Connection conn, JobKey jobKey) throws SQLException {
    return wrappedDriverDelegate.jobExists(conn, jobKey);
  }

  @Override
  public int updateJobData(Connection conn, JobDetail job) throws IOException, SQLException {
    return wrappedDriverDelegate.updateJobData(conn, job);
  }

  @Override
  public JobDetail selectJobDetail(Connection conn, JobKey jobKey, ClassLoadHelper loadHelper)
      throws ClassNotFoundException, IOException, SQLException {
    return wrappedDriverDelegate.selectJobDetail(conn, jobKey, loadHelper);
  }

  @Override
  public int selectNumJobs(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectNumJobs(conn);
  }

  @Override
  public List<String> selectJobGroups(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectJobGroups(conn);
  }

  @Override
  public Set<JobKey> selectJobsInGroup(Connection conn, GroupMatcher<JobKey> matcher)
      throws SQLException {
    return wrappedDriverDelegate.selectJobsInGroup(conn, matcher);
  }

  @Override
  public int insertTrigger(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException, IOException {
    return wrappedDriverDelegate.insertTrigger(conn, trigger, state, jobDetail);
  }

  @Override
  public int updateTrigger(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException, IOException {
    return wrappedDriverDelegate.updateTrigger(conn, trigger, state, jobDetail);
  }

  @Override
  public boolean triggerExists(Connection conn, TriggerKey triggerKey) throws SQLException {
    return wrappedDriverDelegate.triggerExists(conn, triggerKey);
  }

  @Override
  public int updateTriggerState(Connection conn, TriggerKey triggerKey, String state)
      throws SQLException {
    return wrappedDriverDelegate.updateTriggerState(conn, triggerKey, state);
  }

  @Override
  public int updateTriggerStateFromOtherState(Connection conn, TriggerKey triggerKey,
      String newState, String oldState) throws SQLException {
    return wrappedDriverDelegate.updateTriggerStateFromOtherState(conn, triggerKey, newState,
        oldState);
  }

  @Override
  public int updateTriggerStateFromOtherStates(Connection conn, TriggerKey triggerKey,
      String newState, String oldState1, String oldState2, String oldState3) throws SQLException {
    return wrappedDriverDelegate.updateTriggerStateFromOtherStates(conn, triggerKey, newState,
        oldState1, oldState2, oldState3);
  }

  @Override
  public int updateTriggerGroupStateFromOtherStates(Connection conn,
      GroupMatcher<TriggerKey> matcher, String newState, String oldState1, String oldState2,
      String oldState3) throws SQLException {
    return wrappedDriverDelegate.updateTriggerGroupStateFromOtherStates(conn, matcher, newState,
        oldState1, oldState2, oldState3);
  }

  @Override
  public int updateTriggerGroupStateFromOtherState(Connection conn,
      GroupMatcher<TriggerKey> matcher, String newState, String oldState) throws SQLException {
    return wrappedDriverDelegate.updateTriggerGroupStateFromOtherState(conn, matcher, newState,
        oldState);
  }

  @Override
  public int updateTriggerStatesForJob(Connection conn, JobKey jobKey, String state)
      throws SQLException {
    return wrappedDriverDelegate.updateTriggerStatesForJob(conn, jobKey, state);
  }

  @Override
  public int updateTriggerStatesForJobFromOtherState(Connection conn, JobKey jobKey, String state,
      String oldState) throws SQLException {
    return wrappedDriverDelegate.updateTriggerStatesForJobFromOtherState(conn, jobKey, state,
        oldState);
  }

  @Override
  public int deleteTrigger(Connection conn, TriggerKey triggerKey) throws SQLException {
    return wrappedDriverDelegate.deleteTrigger(conn, triggerKey);
  }

  @Override
  public int selectNumTriggersForJob(Connection conn, JobKey jobKey) throws SQLException {
    return wrappedDriverDelegate.selectNumTriggersForJob(conn, jobKey);
  }

  @Override
  public JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,
      TriggerKey triggerKey) throws ClassNotFoundException, SQLException {
    return wrappedDriverDelegate.selectJobForTrigger(conn, loadHelper, triggerKey);
  }

  @Override
  public JobDetail selectJobForTrigger(Connection conn, ClassLoadHelper loadHelper,
      TriggerKey triggerKey, boolean loadJobClass) throws ClassNotFoundException, SQLException {
    return wrappedDriverDelegate.selectJobForTrigger(conn, loadHelper, triggerKey, loadJobClass);
  }

  @Override
  public List<OperableTrigger> selectTriggersForJob(Connection conn, JobKey jobKey)
      throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
    return wrappedDriverDelegate.selectTriggersForJob(conn, jobKey);
  }

  @Override
  public List<OperableTrigger> selectTriggersForCalendar(Connection conn, String calName)
      throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
    return wrappedDriverDelegate.selectTriggersForCalendar(conn, calName);
  }

  @Override
  public OperableTrigger selectTrigger(Connection conn, TriggerKey triggerKey)
      throws SQLException, ClassNotFoundException, IOException, JobPersistenceException {
    return wrappedDriverDelegate.selectTrigger(conn, triggerKey);
  }

  @Override
  public JobDataMap selectTriggerJobDataMap(Connection conn, String triggerName, String groupName)
      throws SQLException, ClassNotFoundException, IOException {
    return wrappedDriverDelegate.selectTriggerJobDataMap(conn, triggerName, groupName);
  }

  @Override
  public String selectTriggerState(Connection conn, TriggerKey triggerKey) throws SQLException {
    return wrappedDriverDelegate.selectTriggerState(conn, triggerKey);
  }

  @Override
  public TriggerStatus selectTriggerStatus(Connection conn, TriggerKey triggerKey)
      throws SQLException {
    return wrappedDriverDelegate.selectTriggerStatus(conn, triggerKey);
  }

  @Override
  public int selectNumTriggers(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectNumTriggers(conn);
  }

  @Override
  public List<String> selectTriggerGroups(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectTriggerGroups(conn);
  }

  @Override
  public List<String> selectTriggerGroups(Connection conn, GroupMatcher<TriggerKey> matcher)
      throws SQLException {
    return wrappedDriverDelegate.selectTriggerGroups(conn, matcher);
  }

  @Override
  public Set<TriggerKey> selectTriggersInGroup(Connection conn, GroupMatcher<TriggerKey> matcher)
      throws SQLException {
    return wrappedDriverDelegate.selectTriggersInGroup(conn, matcher);
  }

  @Override
  public List<TriggerKey> selectTriggersInState(Connection conn, String state) throws SQLException {
    return wrappedDriverDelegate.selectTriggersInState(conn, state);
  }

  @Override
  public int insertPausedTriggerGroup(Connection conn, String groupName) throws SQLException {
    return wrappedDriverDelegate.insertPausedTriggerGroup(conn, groupName);
  }

  @Override
  public int deletePausedTriggerGroup(Connection conn, String groupName) throws SQLException {
    return wrappedDriverDelegate.deletePausedTriggerGroup(conn, groupName);
  }

  @Override
  public int deletePausedTriggerGroup(Connection conn, GroupMatcher<TriggerKey> matcher)
      throws SQLException {
    return wrappedDriverDelegate.deletePausedTriggerGroup(conn, matcher);
  }

  @Override
  public int deleteAllPausedTriggerGroups(Connection conn) throws SQLException {
    return wrappedDriverDelegate.deleteAllPausedTriggerGroups(conn);
  }

  @Override
  public boolean isTriggerGroupPaused(Connection conn, String groupName) throws SQLException {
    return wrappedDriverDelegate.isTriggerGroupPaused(conn, groupName);
  }

  @Override
  public Set<String> selectPausedTriggerGroups(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectPausedTriggerGroups(conn);
  }

  @Override
  public boolean isExistingTriggerGroup(Connection conn, String groupName) throws SQLException {
    return wrappedDriverDelegate.isExistingTriggerGroup(conn, groupName);
  }

  @Override
  public int insertCalendar(Connection conn, String calendarName, Calendar calendar)
      throws IOException, SQLException {
    return wrappedDriverDelegate.insertCalendar(conn, calendarName, calendar);
  }

  @Override
  public int updateCalendar(Connection conn, String calendarName, Calendar calendar)
      throws IOException, SQLException {
    return wrappedDriverDelegate.updateCalendar(conn, calendarName, calendar);
  }

  @Override
  public boolean calendarExists(Connection conn, String calendarName) throws SQLException {
    return wrappedDriverDelegate.calendarExists(conn, calendarName);
  }

  @Override
  public Calendar selectCalendar(Connection conn, String calendarName)
      throws ClassNotFoundException, IOException, SQLException {
    return wrappedDriverDelegate.selectCalendar(conn, calendarName);
  }

  @Override
  public boolean calendarIsReferenced(Connection conn, String calendarName) throws SQLException {
    return wrappedDriverDelegate.calendarIsReferenced(conn, calendarName);
  }

  @Override
  public int deleteCalendar(Connection conn, String calendarName) throws SQLException {
    return wrappedDriverDelegate.deleteCalendar(conn, calendarName);
  }

  @Override
  public int selectNumCalendars(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectNumCalendars(conn);
  }

  @Override
  public List<String> selectCalendars(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectCalendars(conn);
  }

  @Deprecated
  @Override
  public long selectNextFireTime(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectNextFireTime(conn);
  }

  @Override
  public Key<?> selectTriggerForFireTime(Connection conn, long fireTime) throws SQLException {
    return wrappedDriverDelegate.selectTriggerForFireTime(conn, fireTime);
  }

  @Deprecated
  @Override
  public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan,
      long noEarlierThan) throws SQLException {
    return wrappedDriverDelegate.selectTriggerToAcquire(conn, noLaterThan, noEarlierThan);
  }

  @Override
  public List<TriggerKey> selectTriggerToAcquire(Connection conn, long noLaterThan,
      long noEarlierThan, int maxCount) throws SQLException {
    return wrappedDriverDelegate.selectTriggerToAcquire(conn, noLaterThan, noEarlierThan, maxCount);
  }

  @Override
  public int insertFiredTrigger(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException {
    return wrappedDriverDelegate.insertFiredTrigger(conn, trigger, state, jobDetail);
  }

  @Override
  public int updateFiredTrigger(Connection conn, OperableTrigger trigger, String state,
      JobDetail jobDetail) throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<FiredTriggerRecord> selectFiredTriggerRecords(Connection conn, String triggerName,
      String groupName) throws SQLException {
    return wrappedDriverDelegate.selectFiredTriggerRecords(conn, triggerName, groupName);
  }

  @Override
  public List<FiredTriggerRecord> selectFiredTriggerRecordsByJob(Connection conn, String jobName,
      String groupName) throws SQLException {
    return wrappedDriverDelegate.selectFiredTriggerRecordsByJob(conn, jobName, groupName);
  }

  @Override
  public List<FiredTriggerRecord> selectInstancesFiredTriggerRecords(Connection conn,
      String instanceName) throws SQLException {
    return wrappedDriverDelegate.selectInstancesFiredTriggerRecords(conn, instanceName);
  }

  @Override
  public Set<String> selectFiredTriggerInstanceNames(Connection conn) throws SQLException {
    return wrappedDriverDelegate.selectFiredTriggerInstanceNames(conn);
  }

  @Override
  public int deleteFiredTrigger(Connection conn, String entryId) throws SQLException {
    return wrappedDriverDelegate.deleteFiredTrigger(conn, entryId);
  }

  @Override
  public int selectJobExecutionCount(Connection conn, JobKey jobKey) throws SQLException {
    return wrappedDriverDelegate.selectJobExecutionCount(conn, jobKey);
  }

  @Override
  public int insertSchedulerState(Connection conn, String instanceId, long checkInTime,
      long interval) throws SQLException {
    return wrappedDriverDelegate.insertSchedulerState(conn, instanceId, checkInTime, interval);
  }

  @Override
  public int deleteSchedulerState(Connection conn, String instanceId) throws SQLException {
    return wrappedDriverDelegate.deleteSchedulerState(conn, instanceId);
  }

  @Override
  public int updateSchedulerState(Connection conn, String instanceId, long checkInTime)
      throws SQLException {
    return wrappedDriverDelegate.updateSchedulerState(conn, instanceId, checkInTime);
  }

  @Override
  public List<SchedulerStateRecord> selectSchedulerStateRecords(Connection conn, String instanceId)
      throws SQLException {
    return wrappedDriverDelegate.selectSchedulerStateRecords(conn, instanceId);
  }

  @Override
  public void clearData(Connection conn) throws SQLException {
    wrappedDriverDelegate.clearData(conn);
  }
}
