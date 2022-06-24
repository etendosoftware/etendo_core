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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling.trigger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.scheduling.Frequency;
import org.openbravo.scheduling.JobDetailProvider;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.TimingOption;
import org.openbravo.test.base.Issue;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Test cases to cover the Quartz's Trigger generation used for scheduling background processes.
 */
public class TriggerProviderTest {
  private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter
      .ofPattern("dd-MM-yyyy HH:mm:ss");
  private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
  private static final TimeZone EUROPE_MADRID = TimeZone.getTimeZone("Europe/Madrid");
  private static final TimeZone DEFAULT = TimeZone.getDefault();
  private static StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();

  @After
  public void cleanUp() throws SchedulerException {
    // restore default time zone
    TimeZone.setDefault(DEFAULT);
    // delete scheduled data
    schedulerFactory.getScheduler().clear();
  }

  @Test
  public void immediateExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.IMMEDIATE.getLabel();

    String name = SequenceIdData.getUUID();
    Date before = new Date();
    Trigger trigger = TriggerProvider.getInstance().createTrigger(name, null, data);
    Date after = new Date();

    scheduleJob(name, trigger);

    Date nextFire = trigger.getNextFireTime();

    assertThat("Scheduled now", nextFire.compareTo(before) >= 0 && nextFire.compareTo(after) <= 0,
        equalTo(true));
    assertThat("Execution finished", trigger.getFireTimeAfter(nextFire) == null, equalTo(true));
  }

  @Test
  public void laterExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.LATER.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "12:30:22";

    List<String> executions = Arrays.asList("23-09-2019 12:30:22", null);

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void secondlyExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.SECONDLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "15:10:00";
    data.secondlyInterval = "30";
    data.secondlyRepetitions = "3";

    List<String> executions = Arrays.asList( //
        "23-09-2019 15:10:00", //
        "23-09-2019 15:10:30", //
        "23-09-2019 15:11:00", //
        null);

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void secondlyForeverExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.SECONDLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "15:10:00";
    data.secondlyInterval = "30";

    List<String> executions = Arrays.asList( //
        "23-09-2019 15:10:00", //
        "23-09-2019 15:10:30", //
        "23-09-2019 15:11:00");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void minutelyExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MINUTELY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "09:10:15";
    data.minutelyInterval = "1";
    data.minutelyRepetitions = "3";

    List<String> executions = Arrays.asList( //
        "23-09-2019 09:10:15", //
        "23-09-2019 09:11:15", //
        "23-09-2019 09:12:15", //
        null);

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void minutelyForeverExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MINUTELY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "09:10:15";
    data.minutelyInterval = "1";

    List<String> executions = Arrays.asList( //
        "23-09-2019 09:10:15", //
        "23-09-2019 09:11:15", //
        "23-09-2019 09:12:15");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void hourlyExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.HOURLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "22:15:22";
    data.hourlyInterval = "1";
    data.hourlyRepetitions = "3";

    List<String> executions = Arrays.asList( //
        "23-09-2019 22:15:22", //
        "23-09-2019 23:15:22", //
        "24-09-2019 00:15:22", //
        null);

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void hourlyForeverExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.HOURLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "22:15:22";
    data.hourlyInterval = "1";

    List<String> executions = Arrays.asList( //
        "23-09-2019 22:15:22", //
        "23-09-2019 23:15:22", //
        "24-09-2019 00:15:22");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void dailyDefaultExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "27-09-2019";
    data.startTime = "17:45:29";

    List<String> executions = Arrays.asList( //
        "27-09-2019 17:45:29", //
        "28-09-2019 17:45:29", //
        "29-09-2019 17:45:29");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void dailyEveryNDaysExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "27-09-2019";
    data.startTime = "17:45:29";
    data.dailyInterval = "4";
    data.dailyOption = "N";

    List<String> executions = Arrays.asList( //
        "27-09-2019 17:45:29", //
        "01-10-2019 17:45:29", //
        "05-10-2019 17:45:29");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void dailyWeekDaysExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "27-09-2019";
    data.startTime = "17:45:29";
    data.dailyOption = "D";

    List<String> executions = Arrays.asList( //
        "27-09-2019 17:45:29", //
        "30-09-2019 17:45:29", //
        "01-10-2019 17:45:29");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void dailyWeekEndsExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "27-09-2019";
    data.startTime = "17:45:29";
    data.dailyOption = "E";

    List<String> executions = Arrays.asList( //
        "28-09-2019 17:45:29", //
        "29-09-2019 17:45:29", //
        "05-10-2019 17:45:29", //
        "06-10-2019 17:45:29");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void weeklyExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.WEEKLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "01:11:23";
    data.dayMon = "Y";
    data.dayTue = "N";
    data.dayWed = "N";
    data.dayThu = "N";
    data.dayFri = "Y";
    data.daySat = "Y";
    data.daySun = "N";

    List<String> executions = Arrays.asList( //
        "23-09-2019 01:11:23", //
        "27-09-2019 01:11:23", //
        "28-09-2019 01:11:23", //
        "30-09-2019 01:11:23");

    assertExecutions(data, executions, UTC);
  }

  @Test(expected = TriggerGenerationException.class)
  public void invalidWeeklyDefinition() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.WEEKLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "01:11:23";
    data.dayMon = "N";
    data.dayTue = "N";
    data.dayWed = "N";
    data.dayThu = "N";
    data.dayFri = "N";
    data.daySat = "N";
    data.daySun = "N";

    assertExecutions(data, Collections.emptyList(), UTC);
  }

  @Test
  public void monthlyFirstDayOfWeekExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "1";
    data.monthlyDayOfWeek = "1";

    List<String> executions = Arrays.asList( //
        "07-10-2019 19:18:21", //
        "04-11-2019 19:18:21", //
        "02-12-2019 19:18:21", //
        "06-01-2020 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void monthlySecondDayOfWeekExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "2";
    data.monthlyDayOfWeek = "7";

    List<String> executions = Arrays.asList( //
        "13-10-2019 19:18:21", //
        "10-11-2019 19:18:21", //
        "08-12-2019 19:18:21", //
        "12-01-2020 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void monthlyThirdDayOfWeekExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "3";
    data.monthlyDayOfWeek = "2";

    List<String> executions = Arrays.asList( //
        "15-10-2019 19:18:21", //
        "19-11-2019 19:18:21", //
        "17-12-2019 19:18:21", //
        "21-01-2020 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void monthlyFourthDayOfWeekExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "4";
    data.monthlyDayOfWeek = "1";

    List<String> executions = Arrays.asList( //
        "23-09-2019 19:18:21", //
        "28-10-2019 19:18:21", //
        "25-11-2019 19:18:21", //
        "23-12-2019 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void monthlyLastDayOfMonthExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "L";

    List<String> executions = Arrays.asList( //
        "30-09-2019 19:18:21", //
        "31-10-2019 19:18:21", //
        "30-11-2019 19:18:21", //
        "31-12-2019 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void monthlySpecificDateExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "S";
    data.monthlySpecificDay = "23";

    List<String> executions = Arrays.asList( //
        "23-09-2019 19:18:21", //
        "23-10-2019 19:18:21", //
        "23-11-2019 19:18:21", //
        "23-12-2019 19:18:21");

    assertExecutions(data, executions, UTC);
  }

  @Test(expected = TriggerGenerationException.class)
  public void invalidMonthlyDefinition() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.MONTHLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "19:18:21";
    data.monthlyOption = "unknown";

    assertExecutions(data, Collections.emptyList(), UTC);
  }

  @Test
  public void cronBasedExecution() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.CRON.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "16:00:00";
    data.cron = "0 0 16 ? * 7L *"; // On the last Saturday of the month at 16:00:00

    List<String> executions = Arrays.asList( //
        "28-09-2019 16:00:00", //
        "26-10-2019 16:00:00", //
        "30-11-2019 16:00:00", //
        "28-12-2019 16:00:00");

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void executionFinishesAtExpectedDate() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.SECONDLY.getLabel();
    data.startDate = "23-09-2019";
    data.startTime = "15:10:00";
    data.secondlyInterval = "30";
    data.finishesDate = "23-09-2019";
    data.finishesTime = "15:10:31";
    data.finishes = "Y";

    List<String> executions = Arrays.asList( //
        "23-09-2019 15:10:00", //
        "23-09-2019 15:10:30", //
        null // execution finishes
    );

    assertExecutions(data, executions, UTC);
  }

  @Test
  public void every24HoursChangesOnDST() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.HOURLY.getLabel();
    data.startDate = "26-10-2019";
    data.startTime = "13:10:00";
    data.hourlyInterval = "24";

    List<String> executions = Arrays.asList( //
        "26-10-2019 13:10:00", // CEST
        "27-10-2019 12:10:00" // CET
    );

    assertExecutions(data, executions, EUROPE_MADRID);

  }

  @Test
  public void every24HoursDoesNotChangeNoDST() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.HOURLY.getLabel();
    data.startDate = "26-10-2019";
    data.startTime = "13:10:00";
    data.hourlyInterval = "24";

    List<String> executions = Arrays.asList( //
        "26-10-2019 13:10:00", //
        "27-10-2019 13:10:00");

    assertExecutions(data, executions, UTC);

  }

  @Test
  @Issue("39564")
  public void dailyEvery1DayDoesNotChangeOnDST() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "26-10-2019";
    data.startTime = "13:10:00";
    data.dailyOption = "N";
    data.dailyInterval = "1";

    List<String> executions = Arrays.asList( //
        "26-10-2019 13:10:00", // CEST
        "27-10-2019 13:10:00" // CET
    );

    assertExecutions(data, executions, EUROPE_MADRID);
  }

  @Test
  @Issue("39564")
  public void dailyDefaultDoesNotChangeOnDST() throws SchedulerException {
    TriggerData data = new TriggerData();
    data.timingOption = TimingOption.SCHEDULED.getLabel();
    data.frequency = Frequency.DAILY.getLabel();
    data.startDate = "26-10-2019";
    data.startTime = "13:10:00";

    List<String> executions = Arrays.asList( //
        "26-10-2019 13:10:00", // CEST
        "27-10-2019 13:10:00" // CET
    );

    assertExecutions(data, executions, EUROPE_MADRID);
  }

  private void assertExecutions(TriggerData data, List<String> executions, TimeZone tz)
      throws SchedulerException {
    TimeZone.setDefault(tz);

    String name = SequenceIdData.getUUID();
    Trigger trigger = TriggerProvider.getInstance().createTrigger(name, null, data);
    scheduleJob(name, trigger);

    Date nextFire = trigger.getNextFireTime();
    assertThat("1st execution", nextFire, is(dateOf(executions.get(0))));

    for (int i = 1; i < executions.size(); i++) {
      nextFire = trigger.getFireTimeAfter(nextFire);
      String nextExpectedExecution = executions.get(i);
      if (nextExpectedExecution != null) {
        assertThat("execution #" + (i + 1), nextFire, is(dateOf(executions.get(i))));
      } else {
        assertThat("Execution finished", trigger.getFireTimeAfter(nextFire), is(nullValue()));
      }
    }
  }

  private void scheduleJob(String name, Trigger trigger) throws SchedulerException {
    JobDetail jd = JobDetailProvider.getInstance()
        .createJobDetail(name, new ProcessBundle(null, new VariablesSecureApp("0", "0", "0")));
    schedulerFactory.getScheduler().scheduleJob(jd, trigger);
  }

  private Date dateOf(String executionDate) {
    return Date.from(LocalDateTime.parse(executionDate, DEFAULT_FORMATTER)
        .atZone(ZoneId.systemDefault())
        .toInstant());
  }
}
