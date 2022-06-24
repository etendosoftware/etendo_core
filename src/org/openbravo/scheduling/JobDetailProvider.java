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

import static org.openbravo.scheduling.OBScheduler.OB_GROUP;
import static org.quartz.JobBuilder.newJob;

import org.openbravo.database.ConnectionProvider;
import org.openbravo.service.db.DalConnectionProvider;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

/**
 * Allows to build the detail properties of a Quartz's Job instance.
 */
public class JobDetailProvider {

  private static final JobDetailProvider INSTANCE = new JobDetailProvider();

  /**
   * @return the JobDetailProvider singleton instance
   */
  public static JobDetailProvider getInstance() {
    return INSTANCE;
  }

  /**
   * Creates a new JobDetail with the specified name, using {@link DefaultJob} as the class of the
   * JobDetail. Inserts the process bundle into the JobDetail's jobDataMap for retrieval when the
   * job is executed.
   *
   * @param name
   *          The name of the JobDetail. In general this will be the the ID of the
   *          AD_PROCESS_REQUEST.
   * @param bundle
   *          The Openbravo process bundle.
   *
   * @return a JobDetail instance for the specified name.
   *
   * @throws SchedulerException
   *           if there is an error building the JobDetail
   */
  public JobDetail createJobDetail(String name, ProcessBundle bundle) throws SchedulerException {
    return createJobDetail(null, name, bundle);
  }

    /**
     * Creates a new JobDetail with the specified name, using {@link DefaultJob} as the class of the
     * JobDetail. Inserts the process bundle into the JobDetail's jobDataMap for retrieval when the
     * job is executed.
     *
     *
     * @param connection
     * @param name
     *          The name of the JobDetail. In general this will be the the ID of the
     *          AD_PROCESS_REQUEST.
     * @param bundle
     *          The Openbravo process bundle.
     *
     * @return a JobDetail instance for the specified name.
     *
     * @throws SchedulerException
     *           if there is an error building the JobDetail
     */
  public JobDetail createJobDetail(ConnectionProvider connection, String name, ProcessBundle bundle) throws SchedulerException {
    if (bundle == null) {
      throw new SchedulerException("Process bundle cannot be null.");
    }
    JobDetail jobDetail = newJob(DefaultJob.class).withIdentity(name, OB_GROUP).build();
    if(connection == null) {
      connection = new DalConnectionProvider();
    }
    bundle.setConnection(connection);
    jobDetail.getJobDataMap().put(ProcessBundle.KEY, bundle.getMap());

    return jobDetail;
  }
}
