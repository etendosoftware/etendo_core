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

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.openbravo.scheduling.SchedulerTimeUtils;
import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;

/**
 * A generator of Quartz's Triggers with an scheduled frequency. Classes extending this one should
 * implement the scheduled frequency in particular for the generated Triggers.
 */
abstract class ScheduledTriggerGenerator extends TriggerGenerator {

  private static final String FINISHES = "Y";

  abstract TriggerBuilder<?> getScheduledBuilder(TriggerData data) throws ParseException;

  @Override
  public TriggerBuilder<?> getBuilder(TriggerData data) throws ParseException {
    TriggerBuilder<?> triggerBuilder = getScheduledBuilder(data);
    if (StringUtils.isEmpty(data.nextFireTime)) {
      triggerBuilder.startAt(getStartDate(data));
    } else {
      triggerBuilder.startAt(getNextFireDate(data));
    }

    if (FINISHES.equals(data.finishes)) {
      triggerBuilder.endAt(getFinishDate(data));
    }

    return triggerBuilder;
  }

  private Date getStartDate(TriggerData data) throws ParseException {
    String dateTime = SchedulerTimeUtils.getCurrentDateTime(data.startDate, data.startTime);
    return SchedulerTimeUtils.timestamp(dateTime);
  }

  private Date getFinishDate(TriggerData data) throws ParseException {
    String dateTime = SchedulerTimeUtils.getCurrentDateTime(data.finishesDate, data.finishesTime);
    return SchedulerTimeUtils.timestamp(dateTime);
  }

  private Date getNextFireDate(TriggerData data) throws ParseException {
    return SchedulerTimeUtils.timestamp(data.nextFireTime);
  }

  protected String getCronTime(TriggerData data) throws ParseException {
    String dateTime = data.startDate + " " + data.startTime;
    LocalDateTime localDateTime = SchedulerTimeUtils.parse(dateTime);

    int second = localDateTime.getSecond();
    int minute = localDateTime.getMinute();
    int hour = localDateTime.getHour();

    return second + " " + minute + " " + hour;
  }

  protected TriggerBuilder<CronTrigger> cronScheduledTriggerBuilder(String cron) {
    return newTrigger().withSchedule(cronSchedule(cron).withMisfireHandlingInstructionDoNothing());
  }
}
