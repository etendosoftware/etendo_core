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

import static org.quartz.TriggerBuilder.newTrigger;

import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * A generator of Quartz's Triggers to immediately execute the job. Note that this is also the
 * default generator when no AD_PROCESS_REQUEST information is found by the {@link TriggerProvider}.
 */
class ImmediateTriggerGenerator extends TriggerGenerator {

  @Override
  public TriggerBuilder<Trigger> getBuilder(TriggerData data) {
    return newTrigger().startNow();
  }

}
