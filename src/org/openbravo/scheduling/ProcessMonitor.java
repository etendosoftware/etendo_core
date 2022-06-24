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

import static org.openbravo.scheduling.Process.COMPLETE;
import static org.openbravo.scheduling.Process.ERROR;
import static org.openbravo.scheduling.Process.EXECUTION_ID;
import static org.openbravo.scheduling.Process.KILLED;
import static org.openbravo.scheduling.Process.PROCESSING;
import static org.openbravo.scheduling.Process.SCHEDULED;
import static org.openbravo.scheduling.Process.SUCCESS;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;

/**
 * Listens to Scheduler events and JobDetail and Trigger executions in order to set context and
 * process run information for the application. It also manages the execution of process groups.
 *
 * @author awolski
 */
class ProcessMonitor implements SchedulerListener, JobListener, TriggerListener {

  static final Logger log = LogManager.getLogger();

  public static final String KEY = "org.openbravo.scheduling.ProcessMonitor.KEY";

  private String name;

  private SchedulerContext context;

  public ProcessMonitor(String name, SchedulerContext context) {
    this.name = name;
    this.context = context;
  }

  @Override
  public void jobScheduled(Trigger trigger) {
    final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) trigger.getJobDataMap().get(ProcessBundle.KEY));
    final ProcessContext ctx = bundle.getContext();
    try {
      ProcessRequestData.setContext(getConnection(), ctx.getUser(), ctx.getUser(), SCHEDULED,
          bundle.getChannel().toString(), ctx.toString(), trigger.getKey().getName());

      try {
        log.debug("jobScheduled for process {}",
            trigger.getJobDataMap().getString(Process.PROCESS_NAME));
      } catch (Exception ignore) {
        // ignore: exception while trying to log
      }
    } catch (final ServletException e) {
      log.error(e.getMessage(), e);
    } finally {
      // return connection to pool and remove it from current thread
      SessionInfo.init();
    }
  }

  @Override
  public void triggerFired(Trigger trigger, JobExecutionContext jec) {
    final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) jec.getMergedJobDataMap().get(ProcessBundle.KEY));
    final ProcessContext ctx = bundle.getContext();
    try {
      try {
        log.debug("triggerFired for process {}. Next execution time: {}",
            jec.getTrigger().getJobDataMap().getString(Process.PROCESS_NAME),
            trigger.getNextFireTime());
      } catch (Exception ignore) {
        // ignore: exception while trying to log
      }
      ProcessRequestData.update(getConnection(), ctx.getUser(), ctx.getUser(), SCHEDULED,
          bundle.getChannel().toString(), format(trigger.getPreviousFireTime()),
          OBScheduler.getInstance().getSqlDateTimeFormat(), format(trigger.getNextFireTime()),
          format(trigger.getFinalFireTime()), ctx.toString(), trigger.getKey().getName());

    } catch (final ServletException e) {
      log.error(e.getMessage(), e);
    }
    // no need to return the connection because it will be done after the process is executed
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext jec) {
    final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) jec.getMergedJobDataMap().get(ProcessBundle.KEY));
    if (bundle == null) {
      return;
    }
    try {
      log.debug("jobToBeExecuted for process {}}",
          jec.getTrigger().getJobDataMap().getString(Process.PROCESS_NAME));
    } catch (Exception ignore) {
      // ignore: exception while trying to log
    }
    final ProcessContext ctx = bundle.getContext();
    final String executionId = SequenceIdData.getUUID();
    try {
      ProcessRunData.insert(getConnection(), ctx.getOrganization(), ctx.getClient(), ctx.getUser(),
          ctx.getUser(), executionId, PROCESSING, null, null, jec.getJobDetail().getKey().getName(),
          jec.getScheduler().getSchedulerInstanceId());

      bundle.setProcessRunId(executionId);
      jec.put(EXECUTION_ID, executionId);
      jec.put(ProcessBundle.CONNECTION, getConnection());
      jec.put(ProcessBundle.CONFIG_PARAMS, getConfigParameters());
      bundle.setConnection(getConnection());
      jec.getMergedJobDataMap().put(ProcessBundle.KEY, bundle.getMap());

    } catch (final ServletException | SchedulerException e) {
      log.error(e.getMessage(), e);
    }
    // no need to return the connection because it will be done after the process is executed
  }

  @Override
  public void jobWasExecuted(JobExecutionContext jec, JobExecutionException jee) {
    final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) jec.getMergedJobDataMap().get(ProcessBundle.KEY));
    if (bundle == null || bundle.isGroup()) {
      return;
    }
    try {
      log.debug("jobToBeExecuted for process {}}",
          jec.getTrigger().getJobDataMap().getString(Process.PROCESS_NAME));
    } catch (Exception ignore) {
      // ignore: exception while trying to log
    }
    final ProcessContext ctx = bundle.getContext();
    try {
      final String executionId = (String) jec.get(EXECUTION_ID);
      Job jobInstance = jec.getJobInstance();

      String executionStatus;

      if (jee != null) {
        executionStatus = ERROR;
      } else if (jobInstance instanceof DefaultJob && ((DefaultJob) jobInstance).isKilled()) {
        executionStatus = KILLED;
      } else {
        executionStatus = SUCCESS;
      }

      ProcessRunData.update(getConnection(), ctx.getUser(), executionStatus,
          getDuration(jec.getJobRunTime()), bundle.getLog(), executionId);

      if (bundle.getGroupInfo() != null) {
        // Manage Process Group
        manageGroup(bundle, (jee == null ? SUCCESS : ERROR), executionId);
      }

    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      // return connection to pool and remove it from current thread
      SessionInfo.init();
    }
  }

  @Override
  public void triggerFinalized(Trigger trigger) {
    final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) trigger.getJobDataMap().get(ProcessBundle.KEY));
    String updatedBy = bundle != null ? bundle.getContext().getUser() : "0";
    try {
      ProcessRequestData.update(getConnection(), COMPLETE, updatedBy, trigger.getKey().getName());

    } catch (final ServletException e) {
      log.error(e.getMessage(), e);
    } finally {
      // return connection to pool and remove it from current thread
      SessionInfo.init();
    }
  }

  @Override
  public void jobUnscheduled(TriggerKey triggerKey) {
    // Not implemented
  }

  @Override
  public void triggerMisfired(Trigger trigger) {
    try {
      log.debug("Misfired process {}, start time {}.",
          trigger.getJobDataMap().getString(Process.PROCESS_NAME), trigger.getStartTime());

      final String executionId = SequenceIdData.getUUID();
      ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) trigger.getJobDataMap().get(ProcessBundle.KEY));
      ProcessContext ctx = bundle.getContext();

      ProcessRunData.insert(getConnection(), ctx.getOrganization(), ctx.getClient(), ctx.getUser(),
          ctx.getUser(), executionId, Process.MISFIRED, null, null, bundle.getProcessRequestId(),
          null);
    } catch (Exception e) {
      // ignore: exception while trying to log
    }

    // Not implemented
  }

  @Override
  public boolean vetoJobExecution(Trigger trigger, JobExecutionContext jec) {
    JobDataMap jobData = trigger.getJobDataMap();
    Boolean preventConcurrentExecutions = (Boolean) jobData
        .get(Process.PREVENT_CONCURRENT_EXECUTIONS);
    if (preventConcurrentExecutions == null || !preventConcurrentExecutions) {
      return false;
    }

    List<JobExecutionContext> jobs;
    String processName = jobData.getString(Process.PROCESS_NAME);
    try {
      jobs = jec.getScheduler().getCurrentlyExecutingJobs();
    } catch (SchedulerException e) {
      log.error("Error trying to determine if there are concurrent processes in execution for "
          + processName + ", executing it anyway", e);
      return false;
    }

    // Checking if there is another instance in execution for this process

    if (!trigger.getJobDataMap().get(Process.PROCESS_ID).equals(GroupInfo.processGroupId)) {
      // The process is not a group
      for (JobExecutionContext job : jobs) {
        if (job.getTrigger()
            .getJobDataMap()
            .get(Process.PROCESS_ID)
            .equals(trigger.getJobDataMap().get(Process.PROCESS_ID))
            && !job.getJobInstance().equals(jec.getJobInstance())) {

          ProcessBundle jobAlreadyScheduled = ProcessBundle.mapToObject((Map<String, Object>) job.getTrigger()
              .getJobDataMap()
              .get(ProcessBundle.KEY));

          final ProcessBundle newJob = ProcessBundle.mapToObject((Map<String, Object>) trigger.getJobDataMap().get(ProcessBundle.KEY));

          boolean isSameClient = isSameParam(jobAlreadyScheduled, newJob, "Client");

          if (!isSameClient || !isSameParam(jobAlreadyScheduled, newJob, "Organization")) {
            continue;
          }

          log.info("There's another instance running, so leaving {}", processName);
          stopConcurrency(trigger, jec, processName);
          return true;
        }
      }
      log.debug("No other instance");
      return false;
    } else {
      // The process it's a group
      try {
        final ProcessBundle newJob = ProcessBundle.mapToObject((Map<String, Object>) trigger.getJobDataMap().get(ProcessBundle.KEY));
        String concurrent = ProcessRunData.selectConcurrent(getConnection(),
            newJob.getProcessRequestId());
        if (!concurrent.equals("0")) {
          log.info("There's another instance running, so leaving {}", processName);
          stopConcurrency(trigger, jec, processName);
          return true;
        }
      } catch (Exception ex) {
        log.error("Error handling concurrency in process groups", ex);
      }
      log.debug("No other instance");
      return false;
    }

  }

  @Override
  public void jobsPaused(String jobGroup) {
    // Not implemented
  }

  @Override
  public void jobsResumed(String jobGroup) {
    // Not implemented
  }

  @Override
  public void schedulerError(String msg, SchedulerException cause) {
    // Not implemented
  }

  @Override
  public void schedulerShutdown() {
    // Not implemented
  }

  @Override
  public void triggersPaused(String triggerGroup) {
    // Not implemented
  }

  @Override
  public void triggersResumed(String triggerGroup) {
    // Not implemented
  }

  @Override
  public void jobExecutionVetoed(JobExecutionContext jec) {
    // Not implemented
  }

  @Override
  public void triggerComplete(Trigger trigger, JobExecutionContext executionContext,
      CompletedExecutionInstruction triggerInstructionCode) {
    // Not implemented
  }

  @Override
  public void jobAdded(JobDetail jobDetail) {
    // Not implemented
  }

  @Override
  public void jobDeleted(JobKey jobKey) {
    // Not implemented
  }

  @Override
  public void jobPaused(JobKey jobKey) {
    // Not implemented
  }

  @Override
  public void jobResumed(JobKey jobKey) {
    // Not implemented
  }

  @Override
  public void schedulerInStandbyMode() {
    // Not implemented
  }

  @Override
  public void schedulerShuttingdown() {
    // Not implemented
  }

  @Override
  public void schedulerStarted() {
    // Not implemented
  }

  @Override
  public void schedulerStarting() {
    // Not implemented
  }

  @Override
  public void schedulingDataCleared() {
    // Not implemented
  }

  @Override
  public void triggerPaused(TriggerKey triggerKey) {
    // Not implemented
  }

  @Override
  public void triggerResumed(TriggerKey triggerKey) {
    // Not implemented
  }

  private void stopConcurrency(Trigger trigger, JobExecutionContext jec, String processName) {
    try {
      final ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) jec.getMergedJobDataMap().get(ProcessBundle.KEY));
      if (!trigger.mayFireAgain()) {
        String updatedBy = bundle != null ? bundle.getContext().getUser() : "0";
        // This is last execution of this trigger, so set it as complete
        ProcessRequestData.update(getConnection(), COMPLETE, updatedBy, trigger.getKey().getName());
      }

      // Create a process run as error
      if (bundle != null) {
        final ProcessContext ctx = bundle.getContext();
        final String executionId = SequenceIdData.getUUID();
        ProcessRunData.insert(getConnection(), ctx.getOrganization(), ctx.getClient(),
            ctx.getUser(), ctx.getUser(), executionId, PROCESSING, null, null,
            trigger.getKey().getName(), jec.getScheduler().getSchedulerInstanceId());
        ProcessRunData.update(getConnection(), ctx.getUser(), ERROR, getDuration(0),
            "Concurrent attempt to execute", executionId);

        if (bundle.getGroupInfo() != null) {
          // Manage Process Group
          manageGroup(bundle, Process.ERROR, executionId);
        }
      }

    } catch (Exception e) {
      log.error("Error updating context for non executed process due to concurrency {}",
          processName, e);
    } finally {
      // return connection to pool and remove it from current thread only in case the process is
      // not going to be executed because of concurrency, other case leave the connection to be
      // closed after the process finishes
      SessionInfo.init();
    }
  }

  private boolean isSameParam(ProcessBundle jobAlreadyScheduled, ProcessBundle newJob,
      String param) {
    ProcessContext jobAlreadyScheduledContext = null;
    String jobAlreadyScheduledParam = null;
    ProcessContext newJobContext = null;
    String newJobParam = null;

    if (jobAlreadyScheduled != null) {
      jobAlreadyScheduledContext = jobAlreadyScheduled.getContext();
      if (jobAlreadyScheduledContext != null) {
        if ("Client".equals(param)) {
          jobAlreadyScheduledParam = jobAlreadyScheduledContext.getClient();
        } else if ("Organization".equals(param)) {
          jobAlreadyScheduledParam = jobAlreadyScheduledContext.getOrganization();
        }
      }
    }

    if (newJob != null) {
      newJobContext = newJob.getContext();
      if (newJobContext != null) {
        if ("Client".equals(param)) {
          newJobParam = newJobContext.getClient();
        } else if ("Organization".equals(param)) {
          newJobParam = newJobContext.getOrganization();
        }
      }
    }

    return newJobParam != null && jobAlreadyScheduledParam != null
        && newJobParam.equals(jobAlreadyScheduledParam);
  }

  /**
   * @return the database Connection Provider
   */
  public ConnectionProvider getConnection() {
    return (ConnectionProvider) context.get(ConnectionProviderContextListener.POOL_ATTRIBUTE);
  }

  /**
   * @return the configuration parameters.
   */
  public ConfigParameters getConfigParameters() {
    return (ConfigParameters) context.get(ConfigParameters.CONFIG_ATTRIBUTE);
  }

  private String format(Date date) {
    try {
      return date == null ? null
          : SchedulerTimeUtils.format(date, getConfigParameters().getJavaDateTimeFormat());
    } catch (Exception ex) {
      log.error("Could not format date {}", date, ex);
      return null;
    }
  }

  /**
   * Converts a duration in millis to a String
   * 
   * @param duration
   *          the duration in millis
   * @return a String representation of the duration
   */
  public static String getDuration(long duration) {
    final int milliseconds = (int) (duration % 1000);
    final int seconds = (int) ((duration / 1000) % 60);
    final int minutes = (int) ((duration / 60000) % 60);
    final int hours = (int) (duration / 3600000);

    final String millis = milliseconds < 100 ? "0" : "";
    final String m = (milliseconds < 10 ? "00" : millis) + milliseconds;
    final String sec = (seconds < 10 ? "0" : "") + seconds;
    final String min = (minutes < 10 ? "0" : "") + minutes;
    final String hr = (hours < 10 ? "0" : "") + hours;

    return hr + ":" + min + ":" + sec + "." + m;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.quartz.JobListener#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Manage Group actions
   * 
   * @param bundle
   *          the Process Bundle part of the process
   * @param status
   *          the status of the execution Process.SUCCESS or Process.ERROR
   * @param executionId
   *          the AD_ProcessRun_ID if needed to update the Process Run info
   * @throws Exception
   */
  private void manageGroup(ProcessBundle bundle, String status, String executionId)
      throws Exception {
    GroupInfo groupInfo = bundle.getGroupInfo();
    groupInfo.logProcess(status);
    ProcessRunData.updateGroup(getConnection(), groupInfo.getProcessRun().getId(), executionId);

    // Execute next process
    String result = groupInfo.executeNextProcess();
    if (result.equals(GroupInfo.END)) {
      // End of process group execution
      ProcessRunData.update(getConnection(), bundle.getContext().getUser(), groupInfo.getStatus(),
          getDuration(groupInfo.getDuration()), groupInfo.getLog(),
          groupInfo.getProcessRun().getId());
    }

  }
}
