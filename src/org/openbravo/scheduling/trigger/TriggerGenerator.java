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

import static org.openbravo.scheduling.OBScheduler.OB_GROUP;
import static org.openbravo.scheduling.Process.PREVENT_CONCURRENT_EXECUTIONS;
import static org.openbravo.scheduling.Process.PROCESS_ID;
import static org.openbravo.scheduling.Process.PROCESS_NAME;

import java.text.ParseException;

import org.openbravo.scheduling.OBScheduler;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

/**
 * A generator of Quartz's Triggers. Each class extending this one should provide a
 * {@code TriggerBuilder} with the settings used to generate the Trigger.
 */
abstract class TriggerGenerator {

  /**
   * Provides the TriggerBuilder used by the {@link #generate(String, TriggerData)} method to create
   * the Trigger.
   * 
   * @param data
   *          the trigger details loaded from an AD_PROCESS_REQUEST.
   * 
   * @return the {@link TriggerBuilder} instance that will be used to create the Trigger.
   * 
   * @throws ParseException
   *           if there is an error creating the {@link TriggerBuilder} instance.
   */
  protected abstract TriggerBuilder<?> getBuilder(TriggerData data) throws ParseException;

  /**
   * Generates a Trigger with the provided name and details. Note that this method uses as group
   * element for the Trigger's TriggerKey the value of {@link OBScheduler#OB_GROUP}.
   * 
   * @param name
   *          the name element for the Trigger's TriggerKey. In general this will be the ID of the
   *          AD_PROCESS_REQUEST.
   * @param data
   *          the trigger details loaded from the corresponding AD_PROCESS_REQUEST.
   * 
   * @return a {@link Trigger} instance configured according to the provided information.
   * 
   * @throws ParseException
   *           if there is an error creating the {@link Trigger} instance.
   */
  Trigger generate(String name, TriggerData data) throws ParseException {
    TriggerBuilder<?> builder = getBuilder(data).withIdentity(name, OB_GROUP);
    if (data != null) {
      builder.usingJobData(PREVENT_CONCURRENT_EXECUTIONS, "Y".equals(data.preventconcurrent))
          .usingJobData(PROCESS_NAME, data.processName + " " + data.processGroupName)
          .usingJobData(PROCESS_ID, data.adProcessId);
    }
    return builder.build();
  }
}
