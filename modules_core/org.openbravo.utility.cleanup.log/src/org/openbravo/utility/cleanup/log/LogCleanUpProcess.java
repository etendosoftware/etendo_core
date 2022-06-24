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
 * All portions are Copyright (C) 2013-2016 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.utility.cleanup.log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DalBaseProcess;

/**
 * This is the process that is invoked to perform the clean up of configured entities.
 * 
 * @author alostale
 * 
 */
public class LogCleanUpProcess extends DalBaseProcess {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected void doExecute(ProcessBundle bundle) throws Exception {
    ProcessLogger bgLogger = bundle.getLogger();

    BeanManager bm = WeldUtils.getStaticInstanceBeanManager();
    final Set<Bean<?>> beans = bm.getBeans(CleanEntity.class,
        new ComponentProvider.Selector("Default"));
    Bean<?> bean = beans.iterator().next();
    CleanEntity defaultCleaner = (CleanEntity) bm.getReference(bean, CleanEntity.class,
        bm.createCreationalContext(bean));

    OBContext.setAdminMode(false);

    try {
      VariablesSecureApp vars = bundle.getContext().toVars();

      Client client = OBDal.getInstance().get(Client.class, vars.getClient());
      Organization org = OBDal.getInstance().get(Organization.class, vars.getOrg());

      String logMsg = "Starting log clean up process for Client:" + client.getName()
          + " - Organization:" + org.getName();
      bgLogger.log(logMsg + "\n\n");
      log.debug(logMsg);

      OBCriteria<LogCleanUpConfig> qConfig = OBDal.getInstance()
          .createCriteria(LogCleanUpConfig.class);
      qConfig.add(Restrictions.eq(LogCleanUpConfig.PROPERTY_ACTIVE, true));

      long totalDeletedRows = 0L;
      Set<String> tablesWithDeletions = new LinkedHashSet<String>();
      Set<String> tablesToTruncate = new LinkedHashSet<String>();
      for (LogCleanUpConfig config : qConfig.list()) {
        long t = System.currentTimeMillis();
        // force reload of log clean up configuration to prevent LazyInitializationException
        config = OBDal.getInstance().get(LogCleanUpConfig.class, config.getId());
        Entity entity = ModelProvider.getInstance().getEntityByTableId(config.getTable().getId());

        if (config.isTruncateTable()) {
          // tables to be truncated don't use CleanEntity
          tablesToTruncate.add(entity.getTableName());
          continue;
        }

        logMsg = "Cleaning up entity " + entity.getName();
        bgLogger.log(logMsg + "\n");
        log.debug(logMsg);

        boolean useDefault = true;
        int deletedRowsInEntity = 0;

        for (Bean<?> beanCleaner : bm.getBeans(CleanEntity.class,
            new ComponentProvider.Selector(entity.getName()))) {
          // in case there are specific handlers for this entity, use them
          useDefault = false;
          CleanEntity cleaner = (CleanEntity) bm.getReference(beanCleaner, CleanEntity.class,
              bm.createCreationalContext(beanCleaner));
          log.debug("Using {} to clean up entity", cleaner, entity);
          deletedRowsInEntity += cleaner.clean(config, client, org, bgLogger);
        }

        if (useDefault) {
          // if there was no specific handler for this entity, use default one
          log.debug("Using default cleaner for entity", entity);
          deletedRowsInEntity += defaultCleaner.clean(config, client, org, bgLogger);
        }

        if (deletedRowsInEntity > 0) {
          // keep tables where actual deletions occurred, to vacuum them in PG later
          tablesWithDeletions.add(entity.getTableName());
        }
        logMsg = "Entity " + entity.getName() + " cleaned up in " + (System.currentTimeMillis() - t)
            + "ms";
        bgLogger.log(logMsg + "\n\n");
        log.debug(logMsg);
        totalDeletedRows += deletedRowsInEntity;

        OBDal.getInstance().commitAndClose();
      }

      logMsg = "Total rows deleted: " + totalDeletedRows;
      bgLogger.log(logMsg + "\n\n");
      log.debug(logMsg);

      truncateTables(tablesToTruncate, bgLogger);
      vacuumTables(tablesWithDeletions, bgLogger);

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /** Truncates tables removing all data */
  private void truncateTables(Set<String> tablesToTruncate, ProcessLogger bgLogger) {
    if (tablesToTruncate.isEmpty()) {
      // nothing to do
      return;
    }

    Connection con = OBDal.getInstance().getConnection(false);
    PreparedStatement ps = null;
    String logMsg;
    for (String tableName : tablesToTruncate) {
      try {
        long truncateTime = System.currentTimeMillis();
        ps = con.prepareStatement("truncate table " + tableName);
        ps.execute();
        logMsg = "Truncated table " + tableName + " in "
            + (System.currentTimeMillis() - truncateTime) + " ms.";
        bgLogger.log(logMsg + "\n");
        log.debug(logMsg);
      } catch (SQLException e) {
        log.error("Error truncating table {} ", tableName, e);
        bgLogger.log("Error truncating table " + tableName + " " + e.getMessage() + "\n");
      } finally {
        try {
          if (ps != null && !ps.isClosed()) {
            ps.close();
          }
        } catch (SQLException e) {
          log.error("Coulnd't close prepared statement to truncate table {}" + tableName);
        }
      }
    }
  }

  /** vacuums tables if in PostgreSQL */
  private void vacuumTables(Set<String> tablesWithDeletions, ProcessLogger bgLogger) {
    if (!"POSTGRE"
        .equals(OBPropertiesProvider.getInstance().getOpenbravoProperties().get("bbdd.rdbms"))) {
      // Executing vacuum only in PG
      return;
    }
    String logMsg;
    Connection con = OBDal.getInstance().getConnection(false);
    try {
      // auto commit is required in order to be able to execute vacuum
      con.setAutoCommit(true);

      for (String tableName : tablesWithDeletions) {
        PreparedStatement ps = null;
        try {
          logMsg = "About to execute vacuum for " + tableName;
          log.debug(logMsg);
          bgLogger.log(logMsg + "\n");
          long vacuumTime = System.currentTimeMillis();
          ps = con.prepareStatement("vacuum analyze " + tableName);
          ps.execute();
          logMsg = "Vacuum for " + tableName + " executed in "
              + (System.currentTimeMillis() - vacuumTime) + " ms";
          log.debug(logMsg);
          bgLogger.log(logMsg + "\n\n");
        } catch (SQLException e) {
          log.error("Error executing vacuum for table {}", tableName, e);
          bgLogger.log("Error executing vacuum " + e.getMessage() + "\n\n");
        } finally {
          try {
            if (ps != null && !ps.isClosed()) {
              ps.close();
            }
          } catch (SQLException e) {
            log.error("Error closing call statement for vacuum in table {}", tableName, e);
          }
        }
      }
    } catch (SQLException e1) {
      log.error("Error executing vacuum", e1);
      bgLogger.log("Error executing vacuum: " + e1.getMessage());
    } finally {
      // resetting auto commit to properly release DAL trx
      try {
        con.setAutoCommit(false);
      } catch (SQLException e) {
        log.error("Error executing vacuum", e);
      }
    }
  }
}
