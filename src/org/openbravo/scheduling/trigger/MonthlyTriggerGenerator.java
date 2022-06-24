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

import java.text.ParseException;
import java.util.Optional;

import org.quartz.CronTrigger;
import org.quartz.TriggerBuilder;

/**
 * A generator of Quartz's Triggers with monthly frequency.
 */
class MonthlyTriggerGenerator extends ScheduledTriggerGenerator {

  private enum MonthlyOption {
    FIRST("1"), SECOND("2"), THIRD("3"), FOURTH("4"), LAST("L"), SPECIFIC("S");

    private String label;

    private MonthlyOption(String label) {
      this.label = label;
    }

    private static Optional<MonthlyOption> of(String label) {
      for (MonthlyOption monthOption : values()) {
        if (monthOption.label.equals(label)) {
          return Optional.of(monthOption);
        }
      }
      return Optional.empty();
    }
  }

  @Override
  TriggerBuilder<CronTrigger> getScheduledBuilder(TriggerData data) throws ParseException {
    MonthlyOption monthOption = MonthlyOption.of(data.monthlyOption)
        .orElseThrow(() -> new ParseException("Unknown monthly option: " + data.monthlyOption, -1));

    StringBuilder sb = new StringBuilder();
    sb.append(getCronTime(data) + " ");

    switch (monthOption) {
      case FIRST:
      case SECOND:
      case THIRD:
      case FOURTH:
        int day = Integer.parseInt(data.monthlyDayOfWeek) + 1;
        sb.append("? * " + (day > 7 ? 1 : day) + "#" + data.monthlyOption);
        break;
      case LAST:
        sb.append("L * ?");
        break;
      case SPECIFIC:
        sb.append(Integer.parseInt(data.monthlySpecificDay) + " * ?");
        break;
      default:
        throw new ParseException("At least one month option be selected.", -1);
    }

    return cronScheduledTriggerBuilder(sb.toString());
  }

}
