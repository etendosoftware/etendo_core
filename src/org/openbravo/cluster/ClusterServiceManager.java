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
 * All portions are Copyright (C) 2017-2021 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.cluster;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.jmx.MBeanRegistry;
import org.openbravo.model.ad.system.ADClusterService;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Class in charge of registering the node that should handle a particular service when working in a
 * clustered environment.
 */
@ApplicationScoped
public class ClusterServiceManager {
  private static final Logger log = LogManager.getLogger();
  private static final String UNKNOWN = "Unknown";
  private static boolean isCluster = OBPropertiesProvider.getInstance()
      .getBooleanProperty("cluster");

  private boolean isShutDown;
  private String nodeId;
  private String nodeName;
  private Date lastPing;
  private ExecutorService executorService;

  @Inject
  @Any
  private Instance<ClusterService> clusterServices;

  /**
   * Initializes the ClusterServiceManager and starts the thread in charge of registering the node
   * in charge of a particular service. This method has no effect if the application is not running
   * in a clustered environment.
   */
  public void start() {
    if (!isCluster()) {
      return;
    }
    nodeId = SequenceIdData.getUUID();
    nodeName = getNodeName();
    isShutDown = false;
    log.info("Starting Cluster Service Manager - Node ID: {}, Node Name: {}", nodeId, nodeName);
    // register the JMX MBean
    MBeanRegistry.registerMBean("ClusterServices",
        WeldUtils.getInstanceFromStaticBeanManager(JmxClusterServiceManager.class));
    // start the ping thread
    executorService = createExecutorService();
    ClusterServiceThread thread = new ClusterServiceThread(this);
    executorService.execute(thread);
  }

  /**
   * @return a {@code String} with the name that identifies the current cluster node.
   */
  private String getNodeName() {
    String name = ConfigParameters.getMachineName();
    if (StringUtils.isEmpty(name)) {
      name = UNKNOWN;
    }
    return name;
  }

  /**
   * @return a single-threaded {@code ExecutorService} that creates threads which have daemon set to
   *         true.
   */
  private ExecutorService createExecutorService() {
    return Executors.newSingleThreadExecutor(new ThreadFactory() {
      @Override
      public Thread newThread(Runnable runnable) {
        ThreadGroup group = Thread.currentThread().getThreadGroup();
        final Thread thread = new Thread(group, runnable, "Cluster Service Leader Registrator");
        if (thread.getPriority() != Thread.NORM_PRIORITY) {
          thread.setPriority(Thread.NORM_PRIORITY);
        }
        thread.setDaemon(true);
        return thread;
      }
    });
  }

  /**
   * Stops the thread in charge of registering the node in charge of a particular service. This
   * method has no effect if the application is not running in a clustered environment.
   */
  public void shutdown() {
    if (!isCluster() || executorService == null) {
      return;
    }
    deregisterServicesForCurrentNode();
    isShutDown = true;
    log.info("Shutting down Cluster Service Manager");
    executorService.shutdownNow();
    executorService = null;
  }

  private void deregisterServicesForCurrentNode() {
    try {
      OBContext.setAdminMode(false); // allow to delete, the current context does not matter
      OBCriteria<ADClusterService> criteria = OBDal.getInstance()
          .createCriteria(ADClusterService.class);
      criteria.add(Restrictions.eq(ADClusterService.PROPERTY_NODEID, nodeId));
      for (ADClusterService service : criteria.list()) {
        log.info("Deregistering node {} in charge of service {}", nodeId, service.getService());
        OBDal.getInstance().remove(service);
      }
      OBDal.getInstance().commitAndClose();
    } catch (Exception ex) {
      log.error("Could not deregister node {}", nodeId);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * @return {@code true} if the application is running in clustered environment, {@code false}
   *         otherwise.
   */
  protected static boolean isCluster() {
    return isCluster;
  }

  /**
   * @return the available cluster services.
   */
  Iterable<ClusterService> getClusterServices() {
    return clusterServices;
  }

  /**
   * @return the unique identifier of the current cluster node.
   */
  String getCurrentNodeId() {
    return nodeId;
  }

  /**
   * @return the name of the current cluster node.
   */
  String getCurrentNodeName() {
    return nodeName;
  }

  /**
   * @return the Date of the last ping done (for any service) by the current node.
   */
  Date getLastPing() {
    return lastPing;
  }

  private static class ClusterServiceThread implements Runnable {
    private final ClusterServiceManager manager;

    public ClusterServiceThread(ClusterServiceManager manager) {
      this.manager = manager;
    }

    @Override
    public void run() {

      if (!isCluster()) {
        // don't even start, we are not in cluster
        return;
      }

      if (!registerAvailableClusterServices()) {
        log.warn("Could not find any available cluster service");
        return;
      }

      while (true) {
        try {
          if (manager.isShutDown) {
            return;
          }

          // obcontext cleared or wrong obcontext, repair
          if (OBContext.getOBContext() == null
              || !"0".equals(OBContext.getOBContext().getUser().getId())) {
            // make ourselves an admin
            OBContext.setOBContext("0", "0", "0", "0");
          }

          Long nextSleep = doPingRound();
          // wait for the next ping round
          try {
            log.debug("Going to sleep {} milliseconds until the next ping", nextSleep);
            Thread.sleep(nextSleep);
          } catch (Exception ignored) {
          }
        } catch (Throwable t) {
          log.error(t.getMessage(), t);
        }
      }
    }

    private boolean registerAvailableClusterServices() {
      if (manager.getClusterServices() == null) {
        return false;
      }
      try {
        OBContext.setAdminMode(false);
        long current = System.currentTimeMillis();
        boolean anyServiceRegistered = false;
        for (ClusterService service : manager.getClusterServices()) {
          if (service.init(manager.nodeId, manager.nodeName)) {
            // service initialized properly, register it
            registerOrUpdateService(service);
            service.setNextPing(current + service.getTimeout());
            anyServiceRegistered = true;
            if (service.isHandledInCurrentNode()) {
              log.info("Service {} handled by current node {}", service.getServiceName(),
                  getNodeIdentifier());
            } else {
              log.info("Service {} handled by other node ({})", service.getServiceName(),
                  service.getIdentifierOfNodeHandlingService());
            }
          }
        }
        return anyServiceRegistered;
      } finally {
        OBContext.restorePreviousMode();
      }
    }

    private Long doPingRound() {
      long nextSleep = 0L;
      long startTime = System.currentTimeMillis();
      for (ClusterService service : manager.getClusterServices()) {
        if (!service.isAlive() || !service.isInitialized() || service.isDisabled()) {
          // Do not update the last ping: the service is not working
          log.debug("Service {} is not working in node {}", service.getServiceName(),
              getNodeIdentifier());
          continue;
        }
        long current = System.currentTimeMillis();
        long sleep;
        Long serviceNextPing = service.getNextPing();
        if (serviceNextPing <= current) {
          registerOrUpdateService(service);
          service.setNextPing(current + service.getTimeout());
          sleep = service.getTimeout();
        } else {
          sleep = serviceNextPing - current;
        }
        if (sleep < nextSleep || nextSleep == 0) {
          nextSleep = sleep;
        }
      }
      log.debug("Ping round completed in {} milliseconds",
          (System.currentTimeMillis() - startTime));
      if (nextSleep == 0L) {
        // No service available to update its last ping, wait 30 seconds for the next round
        nextSleep = 30_000L;
      }
      return nextSleep;
    }

    private void registerOrUpdateService(ClusterService clusterService) {
      String serviceName = clusterService.getServiceName();
      try {
        ADClusterService service = getService(serviceName);
        Long interval = clusterService.getTimeout() + clusterService.getThreshold();
        Date now = new Date();
        if (service == null) {
          // register the service for the first time
          log.info("Registering current node {} in charge of service {}", getNodeIdentifier(),
              serviceName);
          registerService(serviceName);
        } else if (manager.nodeId.equals(service.getNodeID())) {
          // current node is charge of handling the service, just update the last ping
          log.debug("Current node {} still in charge of service {}", getNodeIdentifier(),
              serviceName);
          updateLastPing(serviceName, now);
        } else if (shouldReplaceNodeOfService(service, interval)) {
          // try to register the current node as the one in charge of handling the service
          log.info("Node {} in charge of service {} should be replaced", getNodeIdentifier(service),
              serviceName);
          clusterService.prepareForNewNodeInCharge();
          updateNodeOfService(service, serviceName, now);
        } else {
          log.debug("Node {} still in charge of service {}", getNodeIdentifier(service),
              serviceName);
        }
        manager.lastPing = now;
      } catch (Exception ex) {
        log.warn("Node {} could not complete register/update task of service {}", manager.nodeId,
            serviceName, ex);
      } finally {
        OBDal.getInstance().commitAndClose();
        // force the service to go to the database to see the changes (if any)
        clusterService.setUseCache(false);
      }
    }

    private ADClusterService getService(String serviceName) {
      OBCriteria<ADClusterService> criteria = OBDal.getInstance()
          .createCriteria(ADClusterService.class);
      criteria.add(Restrictions.eq(ADClusterService.PROPERTY_SERVICE, serviceName));
      return (ADClusterService) criteria.uniqueResult();
    }

    private ADClusterService registerService(String serviceName) {
      ADClusterService service = OBProvider.getInstance().get(ADClusterService.class);
      service.setOrganization(OBDal.getInstance().getProxy(Organization.class, "0"));
      service.setClient(OBDal.getInstance().getProxy(Client.class, "0"));
      service.setService(serviceName);
      service.setNodeID(manager.nodeId);
      service.setNodeName(manager.nodeName);
      OBDal.getInstance().save(service);
      return service;
    }

    private boolean shouldReplaceNodeOfService(ADClusterService service, Long intervalAmount) {
      long leaderLostTime = service.getUpdated().getTime() + intervalAmount;
      long now = new Date().getTime();
      return leaderLostTime < now;
    }

    private void updateNodeOfService(ADClusterService service, String serviceName, Date now) {
      String formerNodeId = service.getNodeID();
      Date formerUpdated = service.getUpdated();
      //@formatter:off
      String hql = 
              "update ADClusterService " +
              "  set nodeID = :newNodeId, " +
              "      nodeName = :newNodeName," +
              "      updated = :updated " +
              " where service = :service " +
              "   and nodeID = :formerNodeId"+
              "   and updated = :formerUpdate";
      //@formatter:on
      int rowCount = OBDal.getInstance()
          .getSession()
          .createQuery(hql)
          .setParameter("newNodeId", manager.nodeId)
          .setParameter("newNodeName", manager.nodeName)
          .setParameter("updated", now)
          .setParameter("service", serviceName)
          .setParameter("formerNodeId", formerNodeId)
          .setParameter("formerUpdate", formerUpdated)
          .executeUpdate();
      if (rowCount == 1) {
        log.info("Replaced node {} with node {} in charge of service {}", formerNodeId,
            getNodeIdentifier(), serviceName);
      }
    }

    private void updateLastPing(String serviceName, Date now) {
      //@formatter:off
      String hql = 
              "update ADClusterService " +
              "  set updated = :updated " +
              " where service = :service" +
              "   and nodeID = :currentNodeId";
      //@formatter:on
      OBDal.getInstance()
          .getSession()
          .createQuery(hql) //
          .setParameter("updated", now) //
          .setParameter("service", serviceName) //
          .setParameter("currentNodeId", manager.nodeId) //
          .executeUpdate();
    }

    private String getNodeIdentifier() {
      return manager.nodeName + " - " + manager.nodeId;
    }

    private String getNodeIdentifier(ADClusterService service) {
      return service.getNodeName() + " - " + service.getNodeID();
    }
  }
}
