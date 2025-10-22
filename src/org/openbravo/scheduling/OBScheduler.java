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
package org.openbravo.scheduling;

import static org.openbravo.scheduling.Process.SCHEDULED;
import static org.openbravo.scheduling.Process.UNSCHEDULED;
import static org.quartz.JobKey.jobKey;
import static org.quartz.TriggerKey.triggerKey;

import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.jmx.MBeanRegistry;
import org.openbravo.scheduling.quartz.JobInitializationListener;
import org.openbravo.scheduling.quartz.OpenbravoPersistentJobStore;
import org.openbravo.scheduling.trigger.TriggerProvider;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Provides the ability of schedule and unschedule background processes.
 *
 * @author awolski
 */
public class OBScheduler implements OBSchedulerMBean {
  private static final OBScheduler INSTANCE = new OBScheduler();

  private static final Logger log = LogManager.getLogger();

  public static final String OB_GROUP = "OB_QUARTZ_GROUP";

  public static final String KEY = "org.openbravo.scheduling.OBSchedulingContext.KEY";

  private Scheduler sched;

  private SchedulerContext ctx;

  private String sqlDateTimeFormat;

  private boolean initializing = true;

  private static final String BACKGROUND_POLICY = "background.policy";
  private static final String NO_EXECUTE_POLICY = "no-execute";

  private OBScheduler() {
    MBeanRegistry.registerMBean("OBScheduler", this);
  }

  /**
   * @return the singleton instance of this class
   */
  public static final OBScheduler getInstance() {
    return INSTANCE;
  }

  /**
   * @return The Quartz Scheduler instance used by OBScheduler.
   */
  public Scheduler getScheduler() {
    return sched;
  }

  /**
   * Retrieves the Openbravo ConnectionProvider from the Scheduler Context.
   *
   * @return A ConnectionProvider
   */
  public ConnectionProvider getConnection() {
    return (ConnectionProvider) ctx.get(ConnectionProviderContextListener.POOL_ATTRIBUTE);
  }

  /**
   * Retrieves the Openbravo ConfigParameters from the Scheduler context.
   *
   * @return Openbravo ConfigParameters
   */
  public ConfigParameters getConfigParameters() {
    return (ConfigParameters) ctx.get(ConfigParameters.CONFIG_ATTRIBUTE);
  }

  /**
   * @return The sqlDateTimeFormat of the OBScheduler.
   */
  String getSqlDateTimeFormat() {
    return sqlDateTimeFormat;
  }

  /**
   * Schedule a new process (bundle) to run immediately in the background, using a random name for
   * the Quartz's JobDetail.
   *
   * This will create a new record in AD_PROCESS_REQUEST. This method throws a
   * {@link ServletException} if there is an error creating the AD_PROCESS_REQUEST information.
   *
   * @see #schedule(String, ProcessBundle)
   */
  public void schedule(ProcessBundle bundle) throws SchedulerException, ServletException {
    if (bundle == null) {
      throw new SchedulerException("Process bundle cannot be null.");
    }
    final String requestId = SequenceIdData.getUUID();

    final String processId = bundle.getProcessId();
    final String channel = bundle.getChannel().toString();
    final ProcessContext context = bundle.getContext();

    ProcessRequestData.insert(getConnection(), context.getOrganization(), context.getClient(),
        context.getUser(), context.getUser(), requestId, processId, context.getUser(), SCHEDULED,
        channel, context.toString(), bundle.getParamsDeflated(), null, null, null, null);

    if (bundle.getGroupInfo() != null) {
      // Is Part of a Group, update the info
      ProcessRequestData.updateGroup(getConnection(), bundle.getGroupInfo().getRequest().getId(),
          requestId);
    }
    schedule(requestId, bundle);
  }

  /**
   * Schedule a process (bundle) with the specified request id. The request id is used in Quartz as
   * the JobDetail's name. The details must be saved to AD_PROCESS_REQUEST before reaching this
   * method.
   *
   * @param requestId
   *          the id of the process request used as the Quartz jobDetail name
   * @param bundle
   *          The bundle with all of the process' details
   *
   * @throws SchedulerException
   *           If something goes wrong with the trigger creation or with the process scheduling.
   */
  public void schedule(String requestId, ProcessBundle bundle) throws SchedulerException {
    if (!initializing && !isSchedulingAllowed()) {
      log.info("Not scheduling process because no scheduler instances are active");
      return;
    }
    if (requestId == null) {
      throw new SchedulerException("Request Id cannot be null.");
    }
    if (bundle == null) {
      throw new SchedulerException("Process bundle cannot be null.");
    }
    JobDetail jobDetail = JobDetailProvider.getInstance().createJobDetail(getConnection(), requestId, bundle);

    Trigger trigger = TriggerProvider.getInstance()
        .createTrigger(requestId, bundle, getConnection());

    if(!sched.checkExists(jobDetail.getKey())) {
      sched.scheduleJob(jobDetail, trigger);
    }
  }

  /**
   * Schedule a process (bundle) with the specified request id. The request id is used in Quartz as
   * the JobDetail's name. The details are saved to AD_PROCESS_REQUEST using this id.
   *
   * @param requestId
   *          the id of the process request used as the Quartz jobDetail name
   * @param bundle
   *          The bundle with all of the process' details
   *
   * @throws SchedulerException
   *           If something goes wrong with the trigger creation or with the process scheduling.
   */
  public void scheduleImmediately(ProcessBundle bundle, String requestId) throws SchedulerException, ServletException {
    if (bundle == null) {
      throw new SchedulerException("Process bundle cannot be null.");
    }

    final String processId = bundle.getProcessId();
    final String channel = bundle.getChannel().toString();
    final ProcessContext context = bundle.getContext();

    ProcessRequestData.insert(getConnection(), context.getOrganization(), context.getClient(),
            context.getUser(), context.getUser(), requestId, processId, context.getUser(), SCHEDULED,
            channel, context.toString(), bundle.getParamsDeflated(), null, null, null, null);

    if (bundle.getGroupInfo() != null) {
      // Is Part of a Group, update the info
      ProcessRequestData.updateGroup(getConnection(), bundle.getGroupInfo().getRequest().getId(),
              requestId);
    }
    schedule(requestId, bundle);
  }

  /**
   * Returns if scheduling is allowed in the current instance It is allowed when there's any
   * scheduler active in one of the instances of the cluster. An active scheduler means that it's
   * policy is not no-execute and it is allowed to schedule new jobs.
   *
   * @return true if allowed to schedule, false otherwise
   * @throws SchedulerException
   */
  public boolean isSchedulingAllowed() throws SchedulerException {
    return !sched.isInStandbyMode() || (sched.getMetaData().isJobStoreClustered()
        && OpenbravoPersistentJobStore.isSchedulingAllowedInCluster(sched.getSchedulerName()));
  }

  /**
   * Returns whether current node is set with no-execute background policy, which should prevent the
   * scheduler instance from being started. If this instance is not started later and no other
   * scheduler instances are active, then it should also prevent scheduling processes.
   */
  public static boolean isNoExecuteBackgroundPolicy() {
    String policy = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty(BACKGROUND_POLICY, "default");
    return NO_EXECUTE_POLICY.equals(policy);
  }

  /**
   * @param requestId
   * @param bundle
   * @throws SchedulerException
   */
  public void reschedule(String requestId, ProcessBundle bundle) throws SchedulerException {
    try {
      sched.unscheduleJob(triggerKey(requestId, OB_GROUP));
      sched.deleteJob(jobKey(requestId, OB_GROUP));

    } catch (final SchedulerException e) {
      log.error("An error occurred rescheduling process {}.", bundle, e);
    }
    schedule(requestId, bundle);
  }

  public void unschedule(String requestId, ProcessContext context) {
    try {
      sched.unscheduleJob(triggerKey(requestId, OB_GROUP));
      sched.deleteJob(jobKey(requestId, OB_GROUP));
      ProcessRequestData.update(getConnection(), UNSCHEDULED, null, sqlDateTimeFormat,
          getCurrentDate(), context.getUser(), requestId);
    } catch (final Exception e) {
      log.error("An error occurred unscheduling process {}", requestId, e);
    }
  }

  private String getCurrentDate() {
    try {
      return SchedulerTimeUtils.currentDate(getConfigParameters().getJavaDateTimeFormat());
    } catch (Exception ex) {
      log.error("Could not format current date", ex);
      return null;
    }
  }

  /**
   * @param schdlr
   * @throws SchedulerException
   */
  public void initialize(Scheduler schdlr) throws SchedulerException {
    this.ctx = schdlr.getContext();
    this.sched = schdlr;

    final ProcessMonitor monitor = new ProcessMonitor("Monitor." + OB_GROUP, this.ctx);
    schdlr.getListenerManager().addSchedulerListener(monitor);
    schdlr.getListenerManager().addJobListener(monitor);
    schdlr.getListenerManager().addTriggerListener(monitor);

    // Add the listener in charge of initializing transient fields after deserialization
    schdlr.getListenerManager().addJobListener(new JobInitializationListener());

    sqlDateTimeFormat = getConfigParameters().getSqlDateTimeFormat();

    try {
      for (ProcessRequestData request : ProcessRequestData.selectByStatus(getConnection(),
          SCHEDULED)) {
        String requestId = request.id;
        VariablesSecureApp vars = ProcessContext.newInstance(request.obContext).toVars();
        boolean isImmediate = TimingOption.of(request.timingOption)
            .map(timingOption -> timingOption == TimingOption.IMMEDIATE)
            .orElse(false);

        if ("Direct".equals(request.channel) || isImmediate) {
          // do not re-schedule immediate and direct requests that were in execution last time
          // Tomcat stopped
          ProcessRequestData.update(getConnection(), Process.SYSTEM_RESTART, vars.getUser(),
              requestId);
          log.debug("{} run of process id {} was scheduled, marked as 'System Restart'",
              request.channel, request.processId);
          continue;
        }

        // processes will be scheduled on initialization only when the Quartz JobStore
        // in usage does not support persistence and when, even with persistence, the
        // job is not in Quartz structures. This can happen if the database recreated
        // or the JobStore has been changed from a non-persistent one.
        if (!schdlr.getMetaData().isJobStoreSupportsPersistence()
            || !sched.checkExists(jobKey(requestId, OB_GROUP))) {
          scheduleProcess(requestId, vars);
        }
      }
      initializing = false;
    } catch (final ServletException e) {
      log.error("An error occurred retrieving scheduled process data: {}", e.getMessage(), e);
    }
  }

  private void scheduleProcess(String requestId, VariablesSecureApp vars)
      throws SchedulerException {
    try {
      final ProcessBundle bundle = ProcessBundle.request(requestId, vars, getConnection());
      schedule(requestId, bundle);
    } catch (final ServletException | ParameterSerializationException e) {
      log.error("Error scheduling process request: {}", requestId, e);
    }
  }

  /*
   * Implementation of the MBean interface
   */

  @Override
  public boolean isStarted() {
    boolean started;
    try {
      started = sched != null && sched.isStarted() && !sched.isInStandbyMode();
    } catch (SchedulerException ex) {
      throw new OBException(ex);
    }
    return started;
  }

  @Override
  public void start() {
    try {
      if (sched == null) {
        throw new OBException(
            "The scheduler was incorrectly initialized, the internal Quartz Scheduler is null");
      }
      if (sched.isStarted() && !sched.isInStandbyMode()) {
        throw new OBException("The scheduler is already started");
      }
      sched.start();
    } catch (SchedulerException ex) {
      throw new OBException(ex);
    }
  }

  @Override
  public void standby() {
    try {
      if (sched == null) {
        throw new OBException(
            "The scheduler was incorrectly initialized, the internal Quartz Scheduler is null");
      }
      if (sched.isInStandbyMode()) {
        throw new OBException("The scheduler is already in standby mode");
      }
      sched.standby();
    } catch (SchedulerException ex) {
      throw new OBException(ex);
    }
  }

}
