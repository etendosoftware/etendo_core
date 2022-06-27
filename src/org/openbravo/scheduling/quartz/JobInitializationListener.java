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
package org.openbravo.scheduling.quartz;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.listeners.JobListenerSupport;

import java.util.Map;

/**
 * Makes sure ProcessBundles have a connection, as it can be lost if the bundle has been serialized
 */
public class JobInitializationListener extends JobListenerSupport {

  private static final String LISTENER_NAME = "OBScheduler.JobInitializationListener";

  private static Logger logger = LogManager.getLogger(JobInitializationListener.class);

  @Override
  public String getName() {
    return LISTENER_NAME;
  }

  @Override
  public void jobToBeExecuted(JobExecutionContext ctx) {
    super.jobToBeExecuted(ctx);
    JobDataMap dataMap = ctx.getJobDetail().getJobDataMap();
    if (dataMap == null) {
      logger.warn("Executing job {} without a ConnectionProvider because the DataMap was null.",
          ctx.getJobDetail().getKey().getName());
      return;
    }
    ProcessBundle bundle = ProcessBundle.mapToObject((Map<String, Object>) dataMap.get(ProcessBundle.KEY));
    if (bundle == null) {
      logger.warn(
          "Executing job {} without a ConnectionProvider because the ProcessBundle was null.",
          ctx.getJobDetail().getKey().getName());
      return;
    }
    if (bundle.getConnection() == null) {
      // Set the ConnectionProvider if it was lost during serialization/deserialization
      ConnectionProvider conn = (ConnectionProvider) ctx.get(ConnectionProviderContextListener.POOL_ATTRIBUTE);
      if(conn == null)
        conn = OBScheduler.getInstance().getConnection();
      bundle.setConnection(conn);
    }
    bundle.setLog(new ProcessLogger(bundle.getConnection()));
  }

}
