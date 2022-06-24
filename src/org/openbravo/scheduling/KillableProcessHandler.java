/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo Public License
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
package org.openbravo.scheduling;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.ThreadExecutor;

/**
 * Checks and handles KillableProcess regularly, killing them if are marked as should_be_killed
 * 
 * It only runs if the scheduler is configured to use the OpenbravoPersistentJobStore
 * 
 */
public class KillableProcessHandler extends Thread {
  private static final Logger logger = LogManager.getLogger();

  private static final long DEFAULT_THRESHOLD = 1000;

  private boolean shutdown = false;

  public KillableProcessHandler() {
    this.setName("KillableProcessHandler");
    this.setDaemon(true);
  }

  public void initialize(ThreadExecutor executor) {
    executor.execute(KillableProcessHandler.this);
  }

  public void shutdown() {
    shutdown = true;
    this.interrupt();
  }

  /**
   * Returns max time threshold to check for killable processes It checks if preference
   * OBSCHD_KillableCheckThreshold is set and uses this if not, it will use
   * {@link #DEFAULT_THRESHOLD} constant.
   *
   * @return
   */
  private long getCheckThreshold() {
    long checkThreshold;
    try {
      String checkThresholdPreference = Preferences.getPreferenceValue(
          "OBSCHD_KillableCheckThreshold", false, (String) null, null, null, null, null);
      checkThreshold = Long.parseLong(checkThresholdPreference);
    } catch (PropertyException e) {
      // Property could not be retrieved, use default threshold
      checkThreshold = DEFAULT_THRESHOLD;
    } catch (NumberFormatException e) {
      logger.debug("Preference OBSCHD_KillableCheckThreshold is not a number, using default.");
      checkThreshold = DEFAULT_THRESHOLD;
    }
    return checkThreshold;
  }

  @Override
  public void run() {
    // Only run if OBScheduler has been initialized
    if (OBScheduler.getInstance().getScheduler() == null) {
      return;
    }
    while (!shutdown) {
      long startTime = System.currentTimeMillis();
      try {
        // Only check if there's some KillableProcess on this instance
        List<JobExecutionContext> killableJobsInInstance = getKillableProcessJobs();
        if (!killableJobsInInstance.isEmpty()) {
          // Check in database if any of those should be killed
          // Filter processRunIds and compare both lists
          List<String> processRunIds = getProcessRunIdsFromDB();
          if (!processRunIds.isEmpty()) {
            for (JobExecutionContext jobContext : killableJobsInInstance) {
              String processRunId = (String) jobContext.get(Process.EXECUTION_ID);
              DefaultJob jobToKill = ((DefaultJob) jobContext.getJobInstance());
              if (!jobToKill.isKilled() && processRunIds.contains(processRunId)) {
                // Kill this process
                ((KillableProcess) jobToKill.getProcessInstance()).kill(jobToKill.getBundle());
                jobToKill.setKilled(true);
                logger.info("Process instance {} has been killed successfully.", processRunId);
              }
            }
          }
        }
      } catch (SchedulerException e) {
        logger.error("Couldn't check for currently executing jobs in KillableProcessHandler", e);
      } catch (Exception e) {
        logger.error("Could not kill process.", e);
      }

      long timeToSleep = getCheckThreshold() - (System.currentTimeMillis() - startTime);
      // Make sure timeToSleep is not negative
      if (timeToSleep <= 0) {
        timeToSleep = 1000;
      }
      try {
        // because this runs in a separate thread, we need to make sure transaction is not left open
        OBDal.getInstance().commitAndClose();
        Thread.sleep(timeToSleep);
      } catch (Exception e) {
        // Exception is ignored
      }
    }
  }

  /**
   * Retrieves all jobs currently executing in this instance that implement KillableProcess
   * interface
   * 
   * @return List of KillableProcess JobExecutionContexts
   * @throws SchedulerException
   *           If scheduler is not initialized
   */
  private List<JobExecutionContext> getKillableProcessJobs() throws SchedulerException {
    Scheduler scheduler = OBScheduler.getInstance().getScheduler();
    List<JobExecutionContext> jobs = scheduler.getCurrentlyExecutingJobs();
    return jobs.stream()
        .filter(jobContext -> ((DefaultJob) jobContext.getJobInstance())
            .getProcessInstance() instanceof KillableProcess)
        .collect(Collectors.toList());
  }

  /**
   * Retrieves all ProcessRun ids that should be killed and were scheduled by this instance
   * 
   * @return List of ProcessRunIds
   */
  private List<String> getProcessRunIdsFromDB() {
    Scheduler scheduler = OBScheduler.getInstance().getScheduler();

    //@formatter:off
    String sql = "" +
      "select ad_process_run_id" +
      "  from ad_process_run" +
      " where status='PRC'" +
      "   and should_be_killed='Y' " +
      "   and scheduler_instance=?";
    //@formatter:on

    List<String> processRunIds = new ArrayList<>();
    Connection connection = OBDal.getInstance().getConnection(true);
    try (PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, scheduler.getSchedulerInstanceId());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          processRunIds.add(rs.getString("ad_process_run_id"));
        }
      }
    } catch (SQLException | SchedulerException e) {
      logger.error("Couldn't retrieve processRunIds from Database", e);
    }
    return processRunIds;
  }

}
