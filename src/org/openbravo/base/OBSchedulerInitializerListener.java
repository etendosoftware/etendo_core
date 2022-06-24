/*
 *************************************************************************
 * This file is a copy of org.quartz.ee.servlet.QuartzInitializerListener
 * with some minor modifications to accommodate the Openbravo ERP environment.
 ************************************************************************
 */
package org.openbravo.base;

import static org.openbravo.base.ConfigParameters.CONFIG_ATTRIBUTE;
import static org.openbravo.base.ConnectionProviderContextListener.POOL_ATTRIBUTE;
import static org.quartz.ee.servlet.QuartzInitializerListener.QUARTZ_FACTORY_KEY;

import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.service.OBDal;
import org.openbravo.scheduling.OBScheduler;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

/**
 * A copy of the QuartzInializerListener with some modifications to initialize the Openbravo
 * scheduling items.
 * 
 * Places Openbravo ERP application resources such as ConnectionProvider and ConfigParameters in the
 * scheduling context so that OBScheduler singleton instance has access to them.
 * 
 * @author awolski
 * 
 */
public class OBSchedulerInitializerListener implements ServletContextListener {

  private boolean performShutdown = true;

  private Scheduler scheduler = null;

  static Logger log = LogManager.getLogger();

  @Override
  public void contextInitialized(ServletContextEvent sce) {

    log.info("Quartz Initializer Servlet loaded, initializing " + "Scheduler...");

    final ServletContext servletContext = sce.getServletContext();
    StdSchedulerFactory factory;
    try {

      final String configFile = servletContext.getInitParameter("config-file");
      final String shutdownPref = servletContext.getInitParameter("shutdown-on-unload");

      if (shutdownPref != null) {
        performShutdown = Boolean.valueOf(shutdownPref).booleanValue();
      }

      // get Properties
      if (configFile != null) {
        factory = new StdSchedulerFactory(configFile);
      } else {
        factory = new StdSchedulerFactory();
      }

      // Always want to get the scheduler, even if it isn't starting,
      // to make sure it is both initialized and registered.
      scheduler = factory.getScheduler();

      // Should the Scheduler being started now or later
      final String startOnLoad = servletContext.getInitParameter("start-scheduler-on-load");

      int startDelay = 0;
      final String startDelayS = servletContext.getInitParameter("start-delay-seconds");
      try {
        if (startDelayS != null && startDelayS.trim().length() > 0) {
          startDelay = Integer.parseInt(startDelayS);
        }
      } catch (final Exception e) {
        log.error("Cannot parse value of 'start-delay-seconds' to an integer: " + startDelayS
            + ", defaulting to 5 seconds.", e);
        startDelay = 5;
      }

      /*
       * If the "start-scheduler-on-load" init-parameter is not specified, the scheduler will be
       * started. This is to maintain backwards compatibility.
       * start-scheduler-on-load controls the starting of the scheduler across all the nodes in
       * the cluster. The configuration parameter background.policy controls the starting of the
       * scheduler in a specific instance of the cluster.
       * Even if the instance is not started on application startup, it can be started later by
       * using the JMX OBScheduler MBean.
       */
      if ((startOnLoad == null || (Boolean.parseBoolean(startOnLoad)))
          && !OBScheduler.isNoExecuteBackgroundPolicy()) {
        if (startDelay <= 0) {
          // Start now
          scheduler.start();
          log.info("Scheduler has been started...");
        } else {
          // Start delayed
          scheduler.startDelayed(startDelay);
          log.info("Scheduler will start in " + startDelay + " seconds.");
        }
      } else {
        log.info("Scheduler has not been started. Start the scheduler calling start() on the OBScheduler MBean");
      }

      String factoryKey = servletContext.getInitParameter("servlet-context-factory-key");
      if (factoryKey == null) {
        factoryKey = QUARTZ_FACTORY_KEY;
      }

      /**
       * Openbravo scheduling stuff.
       */
      log.info("Storing the Quartz Scheduler Factory in the servlet context at key: " + factoryKey);
      servletContext.setAttribute(factoryKey, factory);

      log.info("Storing ConfigParameters and ConnectionProvider in " + "Scheduler Context.");
      scheduler.getContext().put(POOL_ATTRIBUTE, servletContext.getAttribute(POOL_ATTRIBUTE));
      scheduler.getContext().put(CONFIG_ATTRIBUTE, servletContext.getAttribute(CONFIG_ATTRIBUTE));
      log.info("Initalizing singleton instance of " + OBScheduler.class.getName());
      OBScheduler.getInstance().initialize(scheduler);

      // Update Interrupted Process Instance's End time with current time.
      try (Connection connection = OBDal.getInstance().getConnection()) {
        if (connection != null) {
          PreparedStatement ps = null;
          try {
            final String schedulerInstanceId = scheduler.getSchedulerInstanceId();
            //@formatter:off
            String query = ""
              + "update ad_process_run"
              + "  set end_time=NOW(), status='SYR'"
              + " where status='PRC'"
              + "   and end_time is null"
              + "   and scheduler_instance=?";
            //@formatter:on
            ps = connection.prepareStatement(query);
            ps.setString(1, schedulerInstanceId);
            int n = ps.executeUpdate();
            if (n > 0) {
              log.info(n
                  + " background processes were in execution before Tomcat start, they have been marked as 'System Restarted' ");
            }
          } finally {
            if (ps != null && !ps.isClosed()) {
              ps.close();
            }
            OBDal.getInstance().flush();
            OBDal.getInstance().commitAndClose();
          }
        } else {
          System.out.println("Connection Failed!");
        }

      } catch (Exception e) {
        log.error("Error updating Process Instance " + e.toString(), e);
      }
    } catch (final Exception e) {
      log.error("Quartz Scheduler failed to initialize: " + e.toString(), e);
    }
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {

    if (!performShutdown) {
      return;
    }

    try {
      if (scheduler != null) {
        scheduler.shutdown();
      }
    } catch (final Exception e) {
      log.error("Quartz Scheduler failed to shutdown cleanly: " + e.toString(), e);
    }

    log.info("Quartz Scheduler successful shutdown.");
  }

}
