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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConfigParameters;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.Map;

/**
 * Openbravo implementation of the Quartz Job interface to enable Openbravo processes to be
 * scheduled through the Quartz Scheduler. DefaultJob simply removes the {@link Process} and
 * {@link ProcessBundle} objects from the JobExecutionContext and executes them.
 *
 * @author awolski
 *
 */
public class DefaultJob implements Job {

  static Logger log = LogManager.getLogger();

  private Process processInstance;
  private ProcessBundle bundle;
  private boolean killed = false;

  /**
   * See the execute method of the Quartz Job class.
   */
  @Override
  public void execute(JobExecutionContext jec) throws JobExecutionException {
    bundle = ProcessBundle.mapToObject((Map<String, Object>) jec.getMergedJobDataMap().get(ProcessBundle.KEY));
    try {
      processInstance = bundle.getProcessClass().getDeclaredConstructor().newInstance();
      bundle.setConnection((ConnectionProvider) jec.get(ProcessBundle.CONNECTION));
      bundle.setConfig((ConfigParameters) jec.get(ProcessBundle.CONFIG_PARAMS));
      bundle.setLog(new ProcessLogger(bundle.getConnection()));

      // Set audit info
      SessionInfo.setUserId(bundle.getContext().getUser());
      SessionInfo.setProcessType("P");
      SessionInfo.setProcessId(bundle.getProcessId());
      SessionInfo.saveContextInfoIntoDB(bundle.getConnection().getConnection());
      SessionInfo.setQueryProfile("scheduledProcess");

      processInstance.execute(bundle);

    } catch (final Exception e) {
      String processName = bundle != null && bundle.getProcessClass() != null
          ? bundle.getProcessClass().getName()
          : "";
      log.error("Error executing process " + processName, e);
      throw new JobExecutionException(e);
    } finally {
      if (bundle != null) {
        try {
          bundle.applyLog();
          jec.getMergedJobDataMap().put(ProcessBundle.KEY, bundle.getMap());
        } catch (Exception applyError) {
          log.error(applyError);
        }
      }
    }
  }

  /**
   * Returns the process instance
   *
   * @return process instance
   */
  public Process getProcessInstance() {
    return processInstance;
  }

  /** Returns the bundle associated to current execution */
  public ProcessBundle getBundle() {
    return bundle;
  }

  /** Returns whether kill signal has been sent to current execution */
  public boolean isKilled() {
    return killed;
  }

  /** Flags current execution as killed */
  public void setKilled(boolean killed) {
    this.killed = killed;
  }
}
