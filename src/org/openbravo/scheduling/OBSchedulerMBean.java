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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling;

public interface OBSchedulerMBean {

  /*
   * The OBScheduler is instantiated upon application startup. However, depending on the setting of
   * the background.policy parameter, the internal Quartz Scheduler can remain in 'standby' mode.
   * This allows jobs to be scheduled from other nodes if the scheduler is clustered. This method
   * determines if the scheduler is started and actually executing processes.
   */
  public boolean isStarted();

  /*
   * Change the status of the OBScheduler's internal Quartz Scheduler to 'started' to make it fire
   * the scheduled triggers.
   */
  public void start();

  /*
   * Change the status of the OBScheduler's internal Quartz Scheduler to 'standby' to prevent
   * triggers from being fired in this instance. Shutdown should not be called on the internal
   * scheduler except when the application is shutting down.
   */
  public void standby();

}
