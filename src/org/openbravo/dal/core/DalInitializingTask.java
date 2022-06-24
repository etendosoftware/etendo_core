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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.Task;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.dal.service.OBDal;

/**
 * This class can be sub-classed by java ant tasks which need to make use of the Data Access Layer.
 * This class reads the Openbravo.properties and initializes the Dal. The user context is set using
 * the userid. Then the doExecute method is called. This method should be implemented by the
 * subclass.
 * 
 * @author mtaal
 */
public class DalInitializingTask extends Task {
  private static final Logger log = LogManager.getLogger();

  protected String propertiesFile;
  protected String userId;
  private String providerConfigDirectory;
  private boolean reInitializeModel;
  private boolean adminMode = false;

  public String getPropertiesFile() {
    return propertiesFile;
  }

  /**
   * Sets the path to the Openbravo.properties file.
   * 
   * @param propertiesFile
   *          the full filesystem path to the Openbravo.properties file
   */
  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  /**
   * The user used to run the task.
   * 
   * @return the id of the user
   */
  public String getUserId() {
    return userId;
  }

  /**
   * Sets the user which will be used to run the task.
   * 
   * @param userId
   *          the id of the user which will be used to run the task
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * Performs Dal layer initialization and then calls the doExecute method.
   */
  @Override
  public void execute() {

    // initAntConsoleLogging();
    OBProvider.getInstance()
        .register(OBClassLoader.class, OBClassLoader.ClassOBClassLoader.class, false);

    final boolean rereadConfigs = !DalLayerInitializer.getInstance().isInitialized();

    // after install modules the model should be forcefully reinitialized
    if (reInitializeModel) {
      DalLayerInitializer.getInstance().setInitialized(false);
    }

    if (!DalLayerInitializer.getInstance().isInitialized()) {

      log.debug("initializating dal layer, getting properties from " + getPropertiesFile());
      OBPropertiesProvider.getInstance().setProperties(getPropertiesFile());

      if (getProviderConfigDirectory() != null) {
        OBConfigFileProvider.getInstance().setFileLocation(getProviderConfigDirectory());
      }

      // Set the current class class loader as the context class loader to initialize the DAL layer.
      // Otherwise, when executing this kind of tasks in JDK11 using ant, the selected context class
      // loader is not able to find the required JAXBContext instance implementation required to
      // build the Hibernate mapping.
      ClassLoader ctxClassLoader = Thread.currentThread().getContextClassLoader();
      ClassLoader classClassLoader = getClass().getClassLoader();
      Thread.currentThread().setContextClassLoader(classClassLoader);
      try {
        DalLayerInitializer.getInstance().initialize(rereadConfigs);
      } finally {
        Thread.currentThread().setContextClassLoader(ctxClassLoader);
      }
    } else {
      log.debug("Dal Layer already initialized");
    }
    boolean errorOccured = true;
    try {
      log.debug("Setting user context to user " + getUserId());
      OBContext.setOBContext(getUserId());
      if (isAdminMode()) {
        OBContext.setAdminMode();
      }
      doExecute();
      errorOccured = false;
    } finally {
      if (errorOccured) {
        OBDal.getInstance().rollbackAndClose();
      } else {
        OBDal.getInstance().commitAndClose();
      }
      if (isAdminMode()) {
        OBContext.restorePreviousMode();
      }
    }
  }

  /** The method which should be implemented by the subclass */
  protected void doExecute() {
  }

  public String getProviderConfigDirectory() {
    return providerConfigDirectory;
  }

  public void setProviderConfigDirectory(String providerConfigDirectory) {
    this.providerConfigDirectory = providerConfigDirectory;
  }

  public boolean isReInitializeModel() {
    return reInitializeModel;
  }

  public void setReInitializeModel(boolean reInitializeModel) {
    this.reInitializeModel = reInitializeModel;
  }

  public boolean isAdminMode() {
    return adminMode;
  }

  public void setAdminMode(boolean adminMode) {
    this.adminMode = adminMode;
  }

}
