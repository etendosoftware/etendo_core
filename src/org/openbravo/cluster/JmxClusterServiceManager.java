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
 * All portions are Copyright (C) 2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.cluster;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.ADClusterService;

/**
 * An standard MBean that allows to display and manage some of the cluster services settings through
 * JMX.
 */
@ApplicationScoped
public class JmxClusterServiceManager implements JmxClusterServiceManagerMBean {
  private static final Logger log = LogManager.getLogger();

  @Inject
  private ClusterServiceManager clusterServiceManager;

  @Override
  public String getCurrentNodeId() {
    return clusterServiceManager.getCurrentNodeId();
  }

  @Override
  public String getCurrentNodeName() {
    return clusterServiceManager.getCurrentNodeName();
  }

  @Override
  public Date getLastPingOfCurrentNode() {
    return clusterServiceManager.getLastPing();
  }

  @Override
  public Map<String, String> getClusterServiceLeaders() {
    Map<String, String> leaders = new HashMap<>();
    try {
      OBContext.setAdminMode(true);
      OBCriteria<ADClusterService> criteria = OBDal.getInstance()
          .createCriteria(ADClusterService.class);
      for (ADClusterService service : criteria.list()) {
        StringBuilder serviceInfo = new StringBuilder();
        serviceInfo.append("leader ID: " + service.getNodeID());
        serviceInfo.append(", leader name: " + service.getNodeName());
        serviceInfo.append(", last ping: " + service.getUpdated());
        leaders.put(service.getService(), serviceInfo.toString());
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception ex) {
      log.error("Could not retrieve the cluster services information", ex);
    } finally {
      OBContext.restorePreviousMode();
    }
    return leaders;
  }

  @Override
  public Map<String, String> getClusterServiceSettings() {
    Map<String, String> leaders = new HashMap<>();
    for (ClusterService service : clusterServiceManager.getClusterServices()) {
      String serviceSettings = "timeout: " + service.getTimeout() + " milliseconds";
      leaders.put(service.getServiceName(), serviceSettings);
    }
    return leaders;
  }

  @Override
  public void enablePingForService(String serviceName) {
    ClusterService clusterService = getClusterService(serviceName);
    if (clusterService == null) {
      log.info("Can't enable ping for non-existent service {} in node {}", serviceName,
          getCurrentNodeId());
      return;
    }
    if (!clusterService.isDisabled()) {
      log.info("Ping for service {} in node {} is already enabled", serviceName,
          getCurrentNodeId());
      return;
    }
    // Enable the ping for the service
    clusterService.setDisabled(false);
    log.info("Enabled ping for service {} in node {}", serviceName, getCurrentNodeId());
  }

  @Override
  public void disablePingForService(String serviceName) {
    ClusterService clusterService = getClusterService(serviceName);
    if (clusterService == null) {
      log.info("Can't disable ping for non-existent service {} in node {}", serviceName,
          getCurrentNodeId());
      return;
    }
    if (clusterService.isDisabled()) {
      log.info("Ping for service {} in node {} is already disabled", serviceName,
          getCurrentNodeId());
      return;
    }
    // Disable the ping. In case the cluster service is currently processing its tasks then the ping
    // will be disabled afterwards.
    clusterService.deregister();
  }

  private ClusterService getClusterService(String serviceName) {
    for (ClusterService service : clusterServiceManager.getClusterServices()) {
      if (serviceName.equals(service.getServiceName())) {
        return service;
      }
    }
    return null;
  }
}
