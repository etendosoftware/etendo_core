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

import static org.quartz.CalendarIntervalScheduleBuilder.calendarIntervalSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

import java.text.ParseException;

import org.apache.commons.lang.StringUtils;
import org.quartz.DateBuilder.IntervalUnit;
import org.quartz.TriggerBuilder;

/**
 * A generator of Quartz's Triggers with daily frequency.
 */
class DailyTriggerGenerator extends ScheduledTriggerGenerator {

  private enum DailyOption {
    D, // WEEKDAYS
    E, // WEEKENDS
    N // EVERY_N_DAYS
  }

  @Override
  TriggerBuilder<?> getScheduledBuilder(TriggerData data) throws ParseException {
    if (StringUtils.isEmpty(data.dailyOption)) {
      return cronScheduledTriggerBuilder(getCronTime(data) + " ? * *");
    }

    switch (DailyOption.valueOf(data.dailyOption)) {
      case N:
        try {
          return newTrigger().withSchedule(calendarIntervalSchedule()
              .withInterval(Integer.parseInt(data.dailyInterval), IntervalUnit.DAY)
              .withMisfireHandlingInstructionDoNothing());

        } catch (NumberFormatException e) {
          throw new ParseException("Invalid daily interval specified.", -1);
        }
      case D:
        return cronScheduledTriggerBuilder(getCronTime(data) + " ? * MON-FRI");
      case E:
        return cronScheduledTriggerBuilder(getCronTime(data) + " ? * SAT,SUN");
      default:
        throw new ParseException("Invalid daily option: " + data.dailyOption, -1);
    }
  }

}
