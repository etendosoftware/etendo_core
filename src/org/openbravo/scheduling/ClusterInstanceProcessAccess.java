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

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ClusterInstance;
import org.openbravo.model.ad.ui.Process;
import org.quartz.JobDataMap;
import org.quartz.TriggerKey;
import org.quartz.impl.jdbcjobstore.DriverDelegate;
import org.quartz.impl.triggers.AbstractTrigger;

/**
 * Utility class that helps to control what cluster instance are allowed to execute each process
 * request
 */
public class ClusterInstanceProcessAccess {

  /**
   * Given a list of triggerKeys, returns only those that can be executed on the current instance
   * 
   */
  public List<TriggerKey> removeProcessesBannedFromCurrentClusterInstance(DriverDelegate delegate,
      Connection conn, List<TriggerKey> triggerKeys) {
    return triggerKeys.stream()
        .filter(triggerKey -> allowedInCurrentClusterInstance(delegate, conn, triggerKey))
        .collect(Collectors.toList());
  }

  private boolean allowedInCurrentClusterInstance(DriverDelegate delegate, Connection conn,
      TriggerKey triggerKey) {
    try {
      AbstractTrigger<?> trigger = (AbstractTrigger<?>) delegate.selectTrigger(conn, triggerKey);
      JobDataMap jobDataMap = trigger.getJobDataMap();
      ProcessBundle processBundle = (ProcessBundle) jobDataMap.get(ProcessBundle.KEY);
      List<String> bannedClusterInstanceNames = processBundle.getBannedClusterInstanceNames();
      return hasAccess(bannedClusterInstanceNames);
    } catch (Exception e) {
      return true;
    }
  }

  /**
   * Given a processId, returns the list of cluster instances where the process is not allowed to be
   * executed
   */
  public List<String> getBannedClusterInstanceNames(String processId) {
    try {
      OBContext.setAdminMode(true);
      Process process = OBDal.getInstance().get(Process.class, processId);
      if (process == null) {
        // if there is no process, there is no need to check banned cluster instances, because they
        // cannot be configured
        return Collections.emptyList();
      }
      List<String> definedClusterInstancesForProcess = getDefinedClusterInstancesForProcess(
          process);
      if (definedClusterInstancesForProcess.isEmpty()) {
        // All cluster instances can run these process
        return definedClusterInstancesForProcess;
      } else { // ONLY_THOSE_DEFINED
        // All the rest of the instances are banned from executing these process
        List<String> allClusterInstances = getAllClusterInstances();
        return allClusterInstances.stream()
            .filter(clusterInstance -> !definedClusterInstancesForProcess.contains(clusterInstance))
            .collect(Collectors.toList());
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private List<String> getAllClusterInstances() {
    OBCriteria<ClusterInstance> criteria = OBDal.getInstance()
        .createCriteria(ClusterInstance.class);
    criteria.setFilterOnReadableClients(false);
    return criteria.list().stream().map(ClusterInstance::getName).collect(Collectors.toList());
  }

  private List<String> getDefinedClusterInstancesForProcess(
      org.openbravo.model.ad.ui.Process process) {
    return process.getProcessClusterInstList()
        .stream()
        .map(e -> e.getClusterInstance().getName())
        .collect(Collectors.toList());
  }

  private boolean hasAccess(List<String> bannedClusterInstanceNames) {
    String clusterInstanceName = System.getProperty("machine.name");
    return !bannedClusterInstanceNames.contains(clusterInstanceName);
  }

}
