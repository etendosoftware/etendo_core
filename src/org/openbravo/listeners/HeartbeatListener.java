package org.openbravo.listeners;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_process.HeartbeatProcess;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * WebListener that schedules and manages the periodic execution of the HeartbeatProcess.
 * The HeartbeatListener is responsible for initializing a scheduled executor service
 * when the servlet context is initialized and shutting it down when the context is destroyed.
 *
 * <p>This listener ensures that the HeartbeatProcess is executed once per day, which is used
 * to send system usage data to Etendo for analysis and improvement purposes.</p>
 *
 * @see javax.servlet.ServletContextListener
 * @see javax.servlet.annotation.WebListener
 * @see org.openbravo.erpCommon.ad_process.HeartbeatProcess
 */
@WebListener
public class HeartbeatListener implements ServletContextListener {

  private static final Logger log = LogManager.getLogger(HeartbeatListener.class);
  private ScheduledExecutorService scheduler;
  private static final String DEFAULT_SECURE_APP_ID = "0";
  private static final long HEARTBEAT_PERIOD = 1;

  /**
   * Initializes the HeartbeatListener. This method sets up a single-threaded scheduled
   * executor service to execute the HeartbeatProcess once per day.
   *
   * @param event
   *     the ServletContextEvent containing the servlet context that is being initialized
   */
  @Override
  public void contextInitialized(ServletContextEvent event) {
    log.info("Initializing HeartbeatListener...");
    scheduler = Executors.newSingleThreadScheduledExecutor();

    // Schedule the HeartbeatProcess to run once per day
    scheduler.scheduleAtFixedRate(HeartbeatListener::runHeartbeat, 0, HEARTBEAT_PERIOD, TimeUnit.DAYS);
  }

  /**
   * Destroys the HeartbeatListener. This method shuts down the scheduled executor service
   * to stop the periodic execution of the HeartbeatProcess when the servlet context is destroyed.
   *
   * @param event
   *     the ServletContextEvent containing the servlet context that is being destroyed
   */
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if (scheduler != null) {
      log.info("Shutting down HeartbeatListener...");
      scheduler.shutdownNow();
    }
  }

  /**
   * Executes the HeartbeatProcess in Etendo.
   * <p>
   * This method sets up and runs the HeartbeatProcess, which sends system
   * information to a remote server. It uses a {@link DalConnectionProvider}
   * to obtain a database connection and creates a {@link ProcessBundle}
   * with the necessary secure context.
   * </p>
   * <p>
   * If an exception occurs during execution, it is logged as an error.
   * </p>
   */
  private static void runHeartbeat() {
    try {
      log.info("Running HeartbeatProcess...");

      ConnectionProvider connectionProvider = new DalConnectionProvider(false);
      VariablesSecureApp vars = new VariablesSecureApp(DEFAULT_SECURE_APP_ID, DEFAULT_SECURE_APP_ID,
          DEFAULT_SECURE_APP_ID);
      ProcessBundle bundle = new ProcessBundle(HeartbeatProcess.HB_PROCESS_ID, vars);
      bundle.setConnection(connectionProvider);

      HeartbeatProcess heartbeatProcess = new HeartbeatProcess();
      heartbeatProcess.execute(bundle);

    } catch (Exception e) {
      log.error("Error executing HeartbeatProcess: ", e);
    }
  }
}
