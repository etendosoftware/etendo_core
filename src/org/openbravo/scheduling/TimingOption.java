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
 * Represents that available timing options for a process request.
 */
public enum TimingOption {
  IMMEDIATE("I"), LATER("L"), SCHEDULED("S");

  private String label;

  private TimingOption(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static Optional<TimingOption> of(String label) {
    for (TimingOption timingOption : values()) {
      if (timingOption.label.equals(label)) {
        return Optional.of(timingOption);
      }
    }
    return Optional.empty();
  }
}
