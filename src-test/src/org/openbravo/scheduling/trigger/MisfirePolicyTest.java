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
 * All portions are Copyright (C) 2019-2020 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling.trigger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.scheduling.Frequency;
import org.openbravo.scheduling.JobDetailProvider;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.scheduling.TimingOption;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.Issue;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Roles;
import org.openbravo.test.base.TestConstants.Users;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;

import jakarta.enterprise.context.Dependent;

/**
 * Test cases to cover the expected behavior of the misfire policy applied to the background
 * processes.
 */
@RunWith(Parameterized.class)
public class MisfirePolicyTest extends OBBaseTest {
  private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter
      .ofPattern("dd-MM-yyyy HH:mm:ss");

  // Maximum wait time in milliseconds for a job to be scheduled and a trigger to be misfired
  private static final Integer MAX_WAIT_MS = 10000;
  private Scheduler scheduler;
  private TestProcessMonitor processMonitor;
  private TestTriggerMonitor triggerMonitor;

  private Properties properties;

  @Parameterized.Parameters(name = "{0}")
  public static List<String> configOptions() {
    return Arrays.asList("non-clustered", "clustered");
  }

  /**
   * Constructor that will set the necessary config for clustered or non-clustered execution
   * 
   * @param configOption
   *          clustered or non-clustered
   */
  public MisfirePolicyTest(String configOption) {
    // Initialize properties
    properties = new Properties();

    // Set common properties
    properties.setProperty("org.quartz.scheduler.instanceName", "DefaultQuartzScheduler");
    properties.setProperty("org.quartz.scheduler.instanceId", "AUTO");
    properties.setProperty("org.quartz.scheduler.instanceIdGenerator.class",
        "org.openbravo.scheduling.quartz.OpenbravoInstanceIdGenerator");
    properties.setProperty("org.quartz.scheduler.rmi.export", "false");
    properties.setProperty("org.quartz.scheduler.rmi.proxy", "false");
    properties.setProperty("org.quartz.scheduler.wrapJobExecutionInUserTransaction", "false");
    properties.setProperty("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
    properties.setProperty("org.quartz.threadPool.threadCount", "10");
    properties.setProperty("org.quartz.threadPool.threadPriority", "5");
    properties.setProperty(
        "org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
    properties.setProperty("org.quartz.jobStore.misfireThreshold", "500");

    // Set different properties for clustered and non-clustered execution
    if ("non-clustered".equals(configOption)) {
      properties.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
    } else if ("clustered".equals(configOption)) {
      properties.setProperty("org.quartz.jobStore.class",
          "org.openbravo.scheduling.quartz.OpenbravoPersistentJobStore");
      properties.setProperty("org.quartz.jobStore.driverDelegateClass",
          "org.openbravo.scheduling.quartz.OpenbravoDriverDelegate");
      properties.setProperty("org.quartz.jobStore.useProperties", "false");
      properties.setProperty("org.quartz.jobStore.dataSource", "quartzDS");
      properties.setProperty("org.quartz.jobStore.tablePrefix", "OBSCHED_");
      properties.setProperty("org.quartz.jobStore.isClustered", "true");
      properties.setProperty("org.quartz.jobStore.acquireTriggersWithinLock", "true");
      properties.setProperty("org.quartz.jobStore.clusterCheckinInterval", "10000");
      properties.setProperty("org.quartz.dataSource.quartzDS.connectionProvider.class",
          "org.openbravo.scheduling.quartz.QuartzConnectionProvider");
      properties.setProperty("org.quartz.plugin.shutdownhook.class",
          "org.quartz.plugins.management.ShutdownHookPlugin");
      properties.setProperty("org.quartz.plugin.shutdownhook.cleanShutdown", "false");
    }
  }

  @Before
  public void startScheduler() throws SchedulerException {
    scheduler = new StdSchedulerFactory(properties).getScheduler();
    processMonitor = new TestProcessMonitor();
    triggerMonitor = new TestTriggerMonitor();
    scheduler.getListenerManager().addJobListener(processMonitor);
    scheduler.getListenerManager().addTriggerListener(triggerMonitor);
    scheduler.start();
  }

  @After
  public void stopScheduler() throws SchedulerException {
    scheduler.clear();
    scheduler.shutdown();
  }

  /**
   * Check that the misfire policy is fulfilled: don't execute on misfire and wait for next regular
   * execution time.
   */
  @Test
  @Issue("23767")
  public void checkMisfirePolicy() throws SchedulerException, InterruptedException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.WEEKLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "00:00:00";
    data.dayMon = "Y";
    data.dayTue = "N";
    data.dayWed = "N";
    data.dayThu = "N";
    data.dayFri = "N";
    data.daySat = "N";
    data.daySun = "N";

    Date startDate = dateOf("23-09-2019 00:00:00");
    Date nextExecutionDate = dateOf("30-09-2019 00:00:00");

    String name = SequenceIdData.getUUID();
    ProcessBundle bundle = getProcessBundle();
    Trigger trigger = TriggerProvider.getInstance().createTrigger(name, bundle, data);
    scheduleJob(name, trigger, bundle);

    // wait for the Misfire handler to detect a misfire
    waitUntilMisfiredJob(name);
    assertTrue("Trigger should have been misfired", triggerMonitor.hasMisfiredJob(name));

    // Wait to make sure job is not executed
    Thread.sleep(500);
    assertThat("Job should have been executed on misfire", processMonitor.getJobExecutions(name),
        equalTo(0));
    assertThat("Next regular execution time", trigger.getFireTimeAfter(startDate),
        is(nextExecutionDate));
  }

  @Test
  public void checkMisfirePolicyWithSecondlySchedule()
      throws InterruptedException, SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.SECONDLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "00:00:00";
    data.secondlyInterval = "1";
    data.secondlyRepetitions = "2";

    String name = SequenceIdData.getUUID();
    scheduleJob(name, data);

    waitUntilMisfiredJob(name);
    assertTrue("Trigger should have been misfired", triggerMonitor.hasMisfiredJob(name));

    // wait for the 2 job executions (2.5 seconds for executions)
    Thread.sleep(2500);
    assertThat("Expected number of job executions", processMonitor.getJobExecutions(name),
        equalTo(2));
  }

  /**
   * Check that jobs are not executed on misfire in the "every n days" daily execution. We are
   * explicitly testing this kind of schedule because of its particular implementation.
   */
  @Test
  public void everyNDaysNotExecutedOnMisfire() throws SchedulerException, InterruptedException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "00:00:00";
    data.dailyInterval = "1";
    data.dailyOption = "N";

    String name = SequenceIdData.getUUID();
    scheduleJob(name, data);

    // wait until the job has misfired
    waitUntilMisfiredJob(name);
    assertTrue("Trigger should have been misfired", triggerMonitor.hasMisfiredJob(name));

    // Wait to make sure the job is not executed
    Thread.sleep(500);

    assertThat("Job should have been executed on misfire", processMonitor.getJobExecutions(name),
        equalTo(0));
  }

  private void scheduleJob(String name, TriggerData data)
      throws InterruptedException, SchedulerException {
    ProcessBundle bundle = getProcessBundle();
    Trigger trigger = TriggerProvider.getInstance().createTrigger(name, bundle, data);
    scheduleJob(name, trigger, bundle);
  }

  /**
   * Method that will schedule a Job, it will fail silently, retrying to schedule the job every
   * second until MAX_WAIT_MS is surpassed, this is done because of concurrency, as if several
   * clustered tests are run simultaneously, those will generate an exception on some trigger lock
   * that is avoided here.
   * 
   * @param name
   *          Name of the job to schedule (e.g. job UUID)
   * @param trigger
   *          Trigger that will be used to trigger the job
   * @param bundle
   *          ProcessBundle information containing job schedule
   * @throws InterruptedException
   *           When Thread.sleep is interrupted
   */
  private void scheduleJob(String name, Trigger trigger, ProcessBundle bundle)
      throws InterruptedException, SchedulerException {
    int timeElapsed = 0;
    boolean hasBeenScheduled = false;
    SchedulerException lastThrownException = null;
    while (timeElapsed < MAX_WAIT_MS && !hasBeenScheduled) {
      try {
        JobDetail jd = JobDetailProvider.getInstance().createJobDetail(name, bundle);
        scheduler.scheduleJob(jd, trigger);
        hasBeenScheduled = true;
      } catch (SchedulerException e) {
        lastThrownException = e;
        // Wait a second and try to schedule the job again
        Thread.sleep(1000);
        timeElapsed += 1000;
      }
    }
    if (!hasBeenScheduled) {
      // The job couldn't be scheduled successfully, as such, an exception is thrown
      throw new SchedulerException(lastThrownException);
    }
  }

  private ProcessBundle getProcessBundle() {
    DalConnectionProvider conn = new DalConnectionProvider();
    VariablesSecureApp vars = new VariablesSecureApp(Users.ADMIN, Clients.SYSTEM, Orgs.MAIN,
        Roles.SYS_ADMIN);
    ProcessBundle bundle = new ProcessBundle(null, vars);

    bundle.setProcessClass(EmptyProcess.class);
    bundle.setParams(Collections.emptyMap());
    bundle.setConnection(conn);
    bundle.setLog(new ProcessLogger(conn));

    return bundle;
  }

  /**
   * Utility method that will wait until the job provided has correctly misfired or a maximum of
   * MAX_WAIT_MS milliseconds
   * 
   * @param jobName
   *          Name of the job to be checked for misfiring
   */
  private void waitUntilMisfiredJob(String jobName) throws InterruptedException {
    int timeElapsed = 0;
    while (!triggerMonitor.hasMisfiredJob(jobName) && timeElapsed < MAX_WAIT_MS) {
      Thread.sleep(500);
      timeElapsed += 500;
    }
  }

  private Date dateOf(String executionDate) {
    return Date.from(LocalDateTime.parse(executionDate, DEFAULT_FORMATTER)
        .atZone(ZoneId.systemDefault())
        .toInstant());
  }

  private class TestProcessMonitor implements JobListener {

    private static final String NAME = "TestProcessMonitor";
    private Map<String, Integer> jobExecutions;

    public TestProcessMonitor() {
      jobExecutions = new HashMap<>();
    }

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
      context.put(ProcessBundle.CONNECTION, new DalConnectionProvider());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
      // NOOP
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
      String key = context.getJobDetail().getKey().getName();
      jobExecutions.putIfAbsent(key, 0);
      jobExecutions.compute(key, (k, v) -> v + 1);
    }

    public int getJobExecutions(String name) {
      return jobExecutions.getOrDefault(name, 0);
    }
  }

  private class TestTriggerMonitor implements TriggerListener {

    private static final String NAME = "TestProcessMonitor";
    private List<String> misfiredJobs;

    public TestTriggerMonitor() {
      misfiredJobs = new ArrayList<>();
    }

    @Override
    public String getName() {
      return NAME;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
      // NOOP
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
      return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
      misfiredJobs.add(trigger.getJobKey().getName());
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context,
        Trigger.CompletedExecutionInstruction triggerInstructionCode) {
      // NOOP
    }

    public boolean hasMisfiredJob(String jobName) {
      return misfiredJobs.contains(jobName);
    }
  }

  /** Empty process used to determine whether jobs are executed by the scheduler */
  @Dependent
  public static class EmptyProcess extends DalBaseProcess {
    @Override
    protected void doExecute(ProcessBundle bundle) throws Exception {
      // Do nothing
    }
  }
}
