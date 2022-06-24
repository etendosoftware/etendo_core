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
 * All portions are Copyright (C) 2009-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.service.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.platform.ExcludeFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.ddlutils.task.DatabaseUtils;
import org.openbravo.ddlutils.util.DBSMOBUtil;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.service.system.SystemValidationResult.SystemValidationType;
import org.quartz.SchedulerException;

/**
 * Provides utility like services.
 *
 * @author Martin Taal
 */
public class SystemService implements OBSingleton {
  private static SystemService instance;
  private static final Logger log4j = LogManager.getLogger();

  public static synchronized SystemService getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(SystemService.class);
    }
    return instance;
  }

  public static synchronized void setInstance(SystemService instance) {
    SystemService.instance = instance;
  }

  /**
   * Returns true if for a certain class there are objects which have changed.
   *
   * @param clzs
   *          the type of objects which are checked
   * @param afterDate
   *          the timestamp to check
   * @return true if there is an object in the database which changed since afterDate, false
   *         otherwise
   */
  public boolean hasChanged(Class<?>[] clzs, Date afterDate) {
    for (Class<?> clz : clzs) {
      @SuppressWarnings("unchecked")
      final OBCriteria<?> obc = OBDal.getInstance().createCriteria((Class<BaseOBObject>) clz);
      obc.add(Restrictions.gt(Organization.PROPERTY_UPDATED, afterDate));
      // todo: count is slower than exists, is exists possible?
      if (obc.count() > 0) {
        return true;
      }
    }
    return false;
  }

  /**
   * Validates a specific module, checks the javapackage, dependency on core etc. The database
   * changes of the module are not checked. This is a separate task.
   *
   * @param module
   *          the module to validate
   * @param database
   *          the database to read the dbschema from
   * @return the validation result
   */
  public SystemValidationResult validateModule(Module module, Database database) {
    final ModuleValidator moduleValidator = new ModuleValidator();
    moduleValidator.setValidateModule(module);
    return moduleValidator.validate();
  }

  /**
   * Validates the database for a specific module.
   *
   * @param module
   *          the module to validate
   * @param database
   *          the database to read the dbschema from
   * @return the validation result
   */
  public SystemValidationResult validateDatabase(Module module, Database database) {
    boolean validateAD = true;
    return validateDatabase(module, database, validateAD);
  }

  /**
   * Validates the database for a specific module.
   *
   * @param module
   *          the module to validate
   * @param database
   *          the database to read the dbschema from
   * @param validateAD
   *          a flag that determines if Application Dictionary checks should be done
   * @return the validation result
   */
  public SystemValidationResult validateDatabase(Module module, Database database,
      boolean validateAD) {
    final DatabaseValidator databaseValidator = new DatabaseValidator();
    databaseValidator.setValidateModule(module);
    databaseValidator.setDatabase(database);
    databaseValidator.setDbsmExecution(true);
    return databaseValidator.validate(validateAD);
  }

  /**
   * Prints the validation result grouped by validation type to the log.
   *
   * @param log
   *          the log to which the validation result is printed
   * @param result
   *          the validation result containing both errors and warning
   * @return the errors are returned as a string
   */
  public String logValidationResult(Logger log, SystemValidationResult result) {
    for (SystemValidationType validationType : result.getWarnings().keySet()) {
      log.warn("\n");
      log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++");
      log.warn("Warnings for Validation type: " + validationType);
      log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> warnings = result.getWarnings().get(validationType);
      for (String warning : warnings) {
        log.warn(warning);
      }
    }

    final StringBuilder sb = new StringBuilder();
    for (SystemValidationType validationType : result.getErrors().keySet()) {
      sb.append("\n");
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      sb.append("\nErrors for Validation type: " + validationType);
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> errors = result.getErrors().get(validationType);
      for (String err : errors) {
        sb.append("\n");
        sb.append(err);
      }
    }
    if (sb.length() > 0) {
      log.error(sb.toString());
    }
    return sb.toString();
  }

  /**
   * Prints the validation result grouped by validation type to the log.
   *
   * @param log
   *          the log to which the validation result is printed
   * @param result
   *          the validation result containing both errors and warning
   * @return the errors are returned as a string
   * @deprecated use {@link #logValidationResult(Logger, SystemValidationResult)}
   */
  @Deprecated
  public String logValidationResult(org.apache.log4j.Logger log, SystemValidationResult result) {
    for (SystemValidationType validationType : result.getWarnings().keySet()) {
      log.warn("\n");
      log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++");
      log.warn("Warnings for Validation type: " + validationType);
      log.warn("+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> warnings = result.getWarnings().get(validationType);
      for (String warning : warnings) {
        log.warn(warning);
      }
    }

    final StringBuilder sb = new StringBuilder();
    for (SystemValidationType validationType : result.getErrors().keySet()) {
      sb.append("\n");
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      sb.append("\nErrors for Validation type: " + validationType);
      sb.append("\n+++++++++++++++++++++++++++++++++++++++++++++++++++");
      final List<String> errors = result.getErrors().get(validationType);
      for (String err : errors) {
        sb.append("\n");
        sb.append(err);
      }
    }
    if (sb.length() > 0) {
      log.error(sb.toString());
    }
    return sb.toString();
  }

  /**
   * This process deletes a client from the database. During its execution, the Scheduler is
   * stopped, and all sessions active for other users are cancelled
   *
   * @param client
   *          The client to be deleted
   */
  public void deleteClient(Client client) {
    Platform platform = null;
    Connection con = null;
    String clientId = "";
    long t1 = System.currentTimeMillis();
    try {
      platform = getPlatform();
      con = OBDal.getInstance().getConnection();
      killConnectionsAndSafeMode(con);
      try {
        if (OBScheduler.getInstance() != null && OBScheduler.getInstance().getScheduler() != null
            && OBScheduler.getInstance().getScheduler().isStarted()) {
          OBScheduler.getInstance().getScheduler().standby();
        }
      } catch (Exception e) {
        throw new RuntimeException("Could not shutdown scheduler", e);
      }
      OBDal.getInstance().getConnection().commit();
      disableConstraints(platform);
      OBContext.setAdminMode(false);
      OBDal.getInstance().flush();
      OBDal.getInstance().getConnection().commit();
      clientId = client.getId();

      List<String> sqlCommands = new ArrayList<String>();

      List<Entity> entities = ModelProvider.getInstance().getModel();
      for (Entity entity : entities) {
        if ((entity.isClientEnabled() || entity.getName().equals("ADClient")) && !entity.isView()
            && !entity.isDataSourceBased() && !entity.isHQLBased() && !entity.isVirtualEntity()) {
          final String sql = "delete from " + entity.getTableName() + " where ad_client_id=?";
          sqlCommands.add(sql);
        }
      }

      sqlCommands.add("DELETE FROM ad_preference p where visibleat_client_id=?");
      sqlCommands.add("DELETE FROM obuiapp_uipersonalization p where visibleat_client_id=?");

      for (String command : sqlCommands) {
        try (PreparedStatement ps = con.prepareStatement(command)) {
          ps.setString(1, clientId);
          ps.executeUpdate();
        } catch (Exception e) {
          throw new RuntimeException("Exception when executing the following query: " + command, e);
        }
      }

      con.commit();
    } catch (Exception e) {
      log4j.error("Exception when deleting the client " + clientId, e);
      OBDal.getInstance().rollbackAndClose();
      throw new RuntimeException("@DeleteClient_ClientNotRemoved@", e);
    } finally {
      OBDal.getInstance().commitAndClose();
      enableConstraints(platform);
      Connection con2 = platform.borrowConnection();
      try {
        resetSafeMode(con2);
      } finally {
        platform.returnConnection(con2);
      }
      log4j.info("The delete client process for " + clientId + " took "
          + (System.currentTimeMillis() - t1) + " miliseconds");
      OBContext.restorePreviousMode();
      restartScheduler();
    }
  }

  private void restartScheduler() {
    try {
      if (OBScheduler.getInstance() != null && OBScheduler.getInstance().getScheduler() != null) {
        OBScheduler.getInstance().getScheduler().start();
      }
    } catch (SchedulerException e) {
      log4j.error("There was an error while restarting the scheduler", e);
    }
  }

  /**
   * Callend after killConnectionsAndSafeMode, it disables the restriction to log only with the
   * System Administrator role
   *
   * @param con
   *          Connection used to make the queries
   */
  public void resetSafeMode(Connection con) {

    try {
      PreparedStatement ps2 = null;
      try {
        ps2 = con.prepareStatement("UPDATE AD_SYSTEM_INFO SET SYSTEM_STATUS='RB70'");
        ps2.executeUpdate();
      } finally {
        if (ps2 != null && !ps2.isClosed()) {
          ps2.close();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Couldn't reset the safe mode", e);
    }
  }

  /**
   * Kills the active sessions for the current user and sets the System Admin role as the only one
   * available
   *
   * @param con
   *          the Connection used to execute the queries
   */
  public void killConnectionsAndSafeMode(Connection con) {
    try {
      PreparedStatement updateSession = null;
      try {
        updateSession = con
            .prepareStatement("UPDATE AD_SESSION SET SESSION_ACTIVE='N' WHERE CREATEDBY<>?");
        updateSession.setString(1, OBContext.getOBContext().getUser().getId());
        updateSession.executeUpdate();
      } finally {
        if (updateSession != null && !updateSession.isClosed()) {
          updateSession.close();
        }
      }
      PreparedStatement ps2 = null;
      try {
        ps2 = con.prepareStatement("UPDATE AD_SYSTEM_INFO SET SYSTEM_STATUS='RB80'");
        ps2.executeUpdate();
      } finally {
        if (ps2 != null && !ps2.isClosed()) {
          ps2.close();
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Couldn't destroy concurrent sessions", e);
    }
  }

  /**
   * Returns a dbsourcemanager Platform object
   *
   * @return A Platform object built following the configuration set in the Openbravo.properties
   *         file
   */
  public Platform getPlatform() {
    Properties obProp = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    // We disable check constraints before inserting reference data
    String driver = obProp.getProperty("bbdd.driver");
    String url = obProp.getProperty("bbdd.rdbms").equals("POSTGRE")
        ? obProp.getProperty("bbdd.url") + "/" + obProp.getProperty("bbdd.sid")
        : obProp.getProperty("bbdd.url");
    String user = obProp.getProperty("bbdd.user");
    String password = obProp.getProperty("bbdd.password");
    BasicDataSource datasource = DBSMOBUtil.getDataSource(driver, url, user, password);
    Platform platform = PlatformFactory.createNewPlatformInstance(datasource);
    return platform;
  }

  private void disableConstraints(Platform platform) throws FileNotFoundException, IOException {
    log4j.info("Disabling constraints...");
    Database xmlModel = getTablesFromDatabase(platform);
    Connection con = null;
    try {
      con = platform.borrowConnection();
      log4j.info("   Disabling foreign keys");
      platform.disableAllFK(con, xmlModel, false);
      log4j.info("   Disabling triggers");
      platform.disableAllTriggers(con, xmlModel, false);
      log4j.info("   Disabling check constraints");
      platform.disableCheckConstraints(con, xmlModel, null);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (con != null) {
        platform.returnConnection(con);
      }
    }
  }

  private Database getTablesFromDatabase(Platform platform) {
    ExcludeFilter excludeFilter = DBSMOBUtil.getInstance()
        .getExcludeFilter(new File(OBPropertiesProvider.getInstance()
            .getOpenbravoProperties()
            .getProperty("source.path")));
    return platform.loadTablesFromDatabase(excludeFilter);
  }

  /**
   * Given a org.apache.ddlutils.Platform, builds a org.apache.ddlutils.model.Database after
   * applying the exclude filters
   */
  public Database getModelFromDatabase(Platform platform) {
    boolean doPlSqlStandardization = true;
    return getModelFromDatabase(platform, doPlSqlStandardization);
  }

  /**
   * Given a org.apache.ddlutils.Platform, builds a org.apache.ddlutils.model.Database after
   * applying the exclude filters. It is possible to specify whether the PLSQL code standardization
   * should be done
   */
  public Database getModelFromDatabase(Platform platform, boolean doPlSqlStandardization) {
    ExcludeFilter excludeFilter = DBSMOBUtil.getInstance()
        .getExcludeFilter(new File(OBPropertiesProvider.getInstance()
            .getOpenbravoProperties()
            .getProperty("source.path")));
    return platform.loadModelFromDatabase(excludeFilter, doPlSqlStandardization);
  }

  private void enableConstraints(Platform platform) {
    Properties obProp = OBPropertiesProvider.getInstance().getOpenbravoProperties();
    String obDir = obProp.getProperty("source.path");

    Vector<File> dirs = new Vector<File>();
    dirs.add(new File(obDir, "/src-db/database/model/"));
    File modules = new File(obDir, "/modules");

    for (int j = 0; j < modules.listFiles().length; j++) {
      final File dirF = new File(modules.listFiles()[j], "/src-db/database/model/");
      if (dirF.exists()) {
        dirs.add(dirF);
      }
    }
    File[] fileArray = new File[dirs.size()];
    for (int i = 0; i < dirs.size(); i++) {
      fileArray[i] = dirs.get(i);
    }
    Database xmlModel = DatabaseUtils.readDatabase(fileArray);
    platform.deleteAllInvalidConstraintRows(xmlModel, false);
    log4j.info("Enabling constraints...");
    Connection con = null;
    try {
      con = platform.borrowConnection();
      log4j.info("   Enabling check constraints");
      platform.enableCheckConstraints(con, xmlModel, null);
      log4j.info("   Enabling triggers");
      platform.enableAllTriggers(con, xmlModel, false);
      log4j.info("   Enabling foreign keys");
      platform.enableAllFK(con, xmlModel, false);
    } finally {
      if (con != null) {
        platform.returnConnection(con);
      }
    }
  }
}
