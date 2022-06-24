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
package org.openbravo.scheduling;

import java.util.Optional;

/**
 * Represents that frequency of an scheduled process request.
 */
public enum Frequency {
  SECONDLY("1"), MINUTELY("2"), HOURLY("3"), DAILY("4"), WEEKLY("5"), MONTHLY("6"), CRON("7");

  private String label;

  private Frequency(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static Optional<Frequency> of(String label) {
    for (Frequency frequency : values()) {
      if (frequency.label.equals(label)) {
        return Optional.of(frequency);
      }
    }
    return Optional.empty();
  }
}
