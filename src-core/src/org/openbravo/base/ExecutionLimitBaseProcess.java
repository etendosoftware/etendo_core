/*
 ************************************************************************************
 * Copyright (C) 2016-2020 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.etendoerp.properties.EtendoPropertiesProvider;
import org.apache.log4j.Logger;
import org.openbravo.database.CPStandAlone;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

public abstract class ExecutionLimitBaseProcess {

  // Do not update to log4j2. It may cause problems when upgrading from a version already compiled
  // log4j 1.x Logger
  protected static final Logger log4j = Logger.getLogger(ExecutionLimitBaseProcess.class);

  private ConnectionProvider cp;
  private File baseDir;

  private static final String PATH_CONFIG = "config/Openbravo.properties";

  /**
   * Override this method to implement the actions to be executed by the process.
   */
  protected abstract void doExecute();

  /**
   * This method checks whether the doExecute() method should be executed or not according to the
   * execution limits and the executeOnInstall() method when it applies. If eventually the
   * doExecute() method should be executed, it is invoked in this very same method.
   * 
   * @param modulesVersionMap
   *          A data structure that contains module versions mapped by module id
   */
  public void preExecute(Map<String, OpenbravoVersion> modulesVersionMap) {
    if (modulesVersionMap == null || modulesVersionMap.size() == 0) {
      // if we do not have module versions to compare with (install.source) then execute depending
      // on the value of the executeOnInstall() method
      if (executeOnInstall()) {
        doExecute();
      }
      return;
    }

    ExecutionLimits executionLimits = getExecutionLimits();
    if (executionLimits == null || executionLimits.getModuleId() == null) {
      doExecute();
      return;
    }

    String type = getTypeName();
    if (!executionLimits.areCorrect()) {
      log4j.error(type + " " + this.getClass().getName()
          + " not executed because its execution limits are incorrect. "
          + "Last version should be greater than first version.");
      return;
    }
    OpenbravoVersion currentVersion = modulesVersionMap.get(executionLimits.getModuleId());
    OpenbravoVersion firstVersion = executionLimits.getFirstVersion();
    OpenbravoVersion lastVersion = executionLimits.getLastVersion();
    String additionalInfo = "";
    if (currentVersion == null) {
      // Dependent module is being installed
      if (executeOnInstall()) {
        doExecute();
        return;
      }
      additionalInfo = this.getClass().getName()
          + " is configured to not execute it during dependent module installation.";
    } else {
      // Dependent module is already installed
      if ((firstVersion == null || firstVersion.compareTo(currentVersion) < 0)
          && (lastVersion == null || lastVersion.compareTo(currentVersion) > 0)) {
        doExecute();
        return;
      }
      additionalInfo = "Dependent module current version (" + currentVersion + ") is not between "
          + type + " execution limits: first version = " + firstVersion + ", last version = "
          + lastVersion;

    }
    log4j.debug("Not necessary to execute " + type + ": " + this.getClass().getName());
    log4j.debug(additionalInfo);
  }

  /**
   * This method is overridden to retrieve the name of the process type which is extending this
   * class.
   * 
   * @return Name of the process type.
   */
  protected abstract String getTypeName();

  /**
   * This method can be overridden by the subclasses of this class, to specify the module and the
   * limit versions to define whether they should be executed or not.
   *
   * @return a ExecutionLimits object which contains the dependent module id and the first and last
   *         versions of the module that define the execution logic.
   */
  protected abstract ExecutionLimits getExecutionLimits();

  /**
   * This method can be overridden by the subclasses of this class, to specify if they should be
   * executed when installing the dependent module.
   *
   * @return a boolean that indicates if they should be executed when installing the dependent
   *         module.
   */
  protected boolean executeOnInstall() {
    return true;
  }

  /**
   * This method returns a connection provider, which can be used to execute statements in the
   * database
   * 
   * @return a ConnectionProvider
   */
  protected ConnectionProvider getConnectionProvider() {
    if (cp != null) {
      return cp;
    }
    File fProp = getPropertiesFile();
    cp = new CPStandAlone(fProp.getAbsolutePath());
    return cp;
  }

  protected File getPropertiesFile() {
    File fProp = null;
    fProp = EtendoPropertiesProvider.getInstance().getFileFromDevelopmentPath("Openbravo.properties");

    if (fProp.exists()) {
      log4j.info("Loading Etendo properties file from: " + fProp.getAbsolutePath());
      return fProp;
    }

    if (baseDir != null && baseDir.exists()) {
      log4j.debug("Base dir: " + baseDir.getAbsolutePath());
      fProp = new File(baseDir, PATH_CONFIG);
    } else if (new File(PATH_CONFIG).exists()) {
      fProp = new File(PATH_CONFIG);
    } else if (new File("../" + PATH_CONFIG).exists()) {
      fProp = new File("../" + PATH_CONFIG);
    } else if (new File("../../" + PATH_CONFIG).exists()) {
      fProp = new File("../../" + PATH_CONFIG);
    }
    if (fProp == null) {
      log4j.error("Could not find Openbravo.properties");
    }
    return fProp;
  }
  
  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }

  /**
   * Get the source path using the user.dir System Property. Navigates two folders backwards and
   * checks the config directory is available to ensure the directory is an Openbravo instance,
   * throwing an exception otherwise
   *
   * @return String the source path
   * @throws NoSuchFileException
   *           when the source path directory is not valid
   */
  protected String getSourcePath() throws NoSuchFileException {
    String userDir = System.getProperty("user.dir");
    Path sourcePath = Paths.get(userDir, "/../..").normalize();

    Path configDir = sourcePath.resolve("config");
    if (Files.exists(configDir)) {
      return sourcePath.toString();
    }

    // Using System.out as at this point log4j might not be configured yet
    System.out.println(String.format("Config folder not found: %s", configDir.toString()));
    throw new NoSuchFileException(configDir.toString());
  }

}
