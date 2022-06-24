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
 * All portions are Copyright (C) 2008-2018 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.dal.core;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.dialect.function.SQLFunction;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.database.ExternalConnectionPool;

/**
 * This class is responsible for initializing the DAL layer. It ensures that the model is read in
 * memory and that the mapping is generated in a two stage process.
 * 
 * @author mtaal
 */

public class DalLayerInitializer implements OBSingleton {
  private static final Logger log = LogManager.getLogger();

  private static DalLayerInitializer instance;

  private Map<String, SQLFunction> sqlFunctions;

  public static DalLayerInitializer getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(DalLayerInitializer.class);
    }
    return instance;
  }

  private boolean initialized = false;

  /**
   * Initializes the in-memory model, registers the entity classes with the {@link OBProvider
   * OBProvider}, initializes the SessionFactory and reads the service config files.
   * 
   * @param rereadConfigFiles
   *          there are cases where it does not make sense to reread the config files with services,
   *          for example after installing a module. The system needs to be restarted for those
   *          cases.
   */
  public void initialize(boolean rereadConfigFiles) {
    if (initialized) {
      return;
    }
    log.info("Initializing in-memory model...");
    ModelProvider.refresh();

    log.debug("Registering entity classes in the OBFactory");
    for (final Entity e : ModelProvider.getInstance().getModel()) {
      if (e.getMappingClass() != null) {
        OBProvider.getInstance().register(e.getMappingClass(), e.getMappingClass(), false);
        OBProvider.getInstance().register(e.getName(), e.getMappingClass(), false);
      }
    }

    log.info("Model read in-memory, generating mapping...");
    SessionFactoryController.setInstance(getDalSessionFactoryController());
    SessionFactoryController.getInstance().initialize();

    if (isUsingExternalConnectionPool()) {
      // when the session factory is created by the SessionFactoryController, a basic Hibernate pool
      // is also created, let's close it to prevent leaked connections
      SessionFactoryController.getInstance().closeHibernatePool();
    }

    // reset the session
    SessionHandler.deleteSessionHandler();

    // set the configs
    if (rereadConfigFiles) {
      OBConfigFileProvider.getInstance().setConfigInProvider();
    }

    log.info("Dal layer initialized");
    initialized = true;
  }

  private DalSessionFactoryController getDalSessionFactoryController() {
    DalSessionFactoryController dsfc;
    try {
      dsfc = WeldUtils.getInstanceFromStaticBeanManager(DalSessionFactoryController.class);
    } catch (Throwable t) {
      // retrieving the DalSessionFactoryController instance with weld can fail in some build tasks
      // or when executing the tests
      log.debug("Could not instantiate DalSessionFactoryController using weld", t);
      dsfc = OBProvider.getInstance().get(DalSessionFactoryController.class);
    }
    if (sqlFunctions != null && !sqlFunctions.isEmpty()) {
      dsfc.setSQLFunctions(sqlFunctions);
    }
    return dsfc;
  }

  /**
   * Can be used to manually provide to the {@link DalSessionFactoryController} the SQL functions to
   * be registered in Hibernate.
   * 
   * @param sqlFunctions
   *          a Map with the SQL functions to be registered in Hibernate.
   */
  public void setSQLFunctions(Map<String, SQLFunction> sqlFunctions) {
    this.sqlFunctions = sqlFunctions;
  }

  private boolean isUsingExternalConnectionPool() {
    String poolClassName = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("db.externalPoolClassName");
    if (poolClassName == null || "".equals(poolClassName)) {
      return false;
    }
    try {
      // check whether the external connection pool is present in the classpath
      ExternalConnectionPool.getInstance(poolClassName);
      return true;
    } catch (ReflectiveOperationException | NoClassDefFoundError e) {
      return false;
    }
  }

  public boolean isInitialized() {
    return initialized;
  }

  /**
   * Can be used to set the internal initialized member to false and then call initialize again to
   * re-initialize the DAL layer.
   * 
   * @param initialized
   *          the value of the initialized member
   */
  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

}
