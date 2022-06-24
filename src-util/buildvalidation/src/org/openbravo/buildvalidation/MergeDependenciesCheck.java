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
 * All portions are Copyright (C) 2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * Validation to check the instance does not have installed modules that depend on the merged
 * modules, like translation modules for example.
 */
public class MergeDependenciesCheck extends BuildValidation {

  private static final Logger log4j = LogManager.getLogger();

  private static final String ID_CORE = "0";

  // Paths
  private static final String AD_MODULE_DEPENDENCY_PATH = "src-db/database/sourcedata/AD_MODULE_DEPENDENCY.xml";
  private static final String MODULES_DIRNAME = "modules";
  private static final String MODULES_CORE_DIRNAME = "modules_core";

  // Aging Balance Report
  private static final String ID_AGING = "391979C3E1A44A9D814B3F9756FC57F4";
  private static final String JAVAPACKAGE_AGING = "org.openbravo.agingbalance";
  private static final String NAME_AGING = "Aging Balance Report";

  // Cashflow Forecast Report
  private static final String ID_CASHFLOWFORECAST = "FF80808131D1689F0131D170F19A0006";
  private static final String JAVAPACKAGE_CASHFLOWFORECAST = "org.openbravo.financial.cashflowforecast";
  private static final String NAME_CASHFLOWFORECAST = "Report: Cash Flow Forecast";

  // Multi Business Partner Selector
  private static final String ID_MULTIPLEBP = "334C2A06294447FAA7D1AF5D98E8F857";
  private static final String JAVAPACKAGE_MULTIPLEBP = "org.openbravo.utility.multiplebpselector";
  private static final String NAME_MULTIPLEBP = "Openbravo Multi Business Partner Selector";

  // List of modules included in Openbravo 3 in 17Q1
  // It is used to filter out these modules during the check so it's faster. Note there is no
  // problem if more modules are included into the distribution in the future, only the check would
  // be a bit slower
  private static final List<String> OPENBRAVO3MODULESLIST = Arrays.asList(
      "org.openbravo.advpaymentmngt", "org.openbravo.client.application",
      "org.openbravo.client.htmlwidget", "org.openbravo.client.kernel",
      "org.openbravo.client.myob", "org.openbravo.client.querylist",
      "org.openbravo.service.datasource", "org.openbravo.service.integration.google",
      "org.openbravo.service.integration.openid", "org.openbravo.userinterface.skin.250to300Comp",
      "org.openbravo.financial.paymentreport", "org.openbravo.userinterface.smartclient",
      "org.openbravo.utility.cleanup.log", "org.openbravo.v3.datasets",
      "org.openbravo.v3.framework", "org.openbravo.v3",
      "org.openbravo.reports.ordersawaitingdelivery", "org.openbravo.apachejdbcconnectionpool",
      "org.openbravo.client.widgets", "org.openbravo.base.weld", "org.openbravo.service.json",
      "org.openbravo.userinterface.selector");

  /**
   * Contains the modules that are merged and need to be checked. If more modules are merged in the
   * future, we just need to add them here
   */
  private List<MergedModule> getMergedModules() {
    final List<MergedModule> modulesToCheck = new ArrayList<MergedModule>();
    modulesToCheck.add(new MergedModule(ID_AGING, JAVAPACKAGE_AGING, NAME_AGING));
    modulesToCheck.add(new MergedModule(ID_CASHFLOWFORECAST, JAVAPACKAGE_CASHFLOWFORECAST,
        NAME_CASHFLOWFORECAST));
    return modulesToCheck;
  }

  @Override
  public List<String> execute() {
    final List<String> errors = new ArrayList<String>();
    try {
      errors.addAll(checkMergedModulesAreNotInModulesDir());
      errors.addAll(checkPossibleDependencies());
      checkRemovableModule(new MergedModule(ID_MULTIPLEBP, JAVAPACKAGE_MULTIPLEBP, NAME_MULTIPLEBP));
    } catch (Exception e) {
      return handleError(e);
    }
    return errors;
  }

  /**
   * Checks the merged modules are not inside the modules folder. This is useful when the upgrade
   * process has been manually done by the user through the command line instead of using the Module
   * Management window that automatically removes the merged modules.
   * 
   * @return error message with a description of the problem
   */
  private List<String> checkMergedModulesAreNotInModulesDir() {
    final List<String> errors = new ArrayList<String>();
    final List<String> installedModules = Arrays.asList(getInstalledModules());
    for (final MergedModule mergedModule : getMergedModules()) {
      if (installedModules.contains(mergedModule.getJavaPackage())) {
        errors
            .add(String
                .format(
                    "The %s (%s) module has been merged into the Openbravo 3 distribution. You must uninstall it manually before proceeding with the update process.",
                    mergedModule.getName(), mergedModule.getJavaPackage()));
      }
    }
    return errors;
  }

  /**
   * Checks the instance doesn't have any declared dependency to any of the merged modules
   * 
   * @return error message with a description of the problem
   */
  private List<String> checkPossibleDependencies() {
    final List<String> errors = new ArrayList<String>();
    final File modulesDir = getModulesDir();
    final File modulesCoreDir = getModulesCoreDir();
    final List<MergedModule> mergedModules = getMergedModules();

    for (final String module : getInstalledModules()) {
      final File moduleDir = new File(modulesDir, module);
      File adModuleDependencyFile = new File(moduleDir, AD_MODULE_DEPENDENCY_PATH);
      if(!adModuleDependencyFile.exists()) {
        adModuleDependencyFile = new File(moduleCoreDir, AD_MODULE_DEPENDENCY_PATH);
      }
      if (adModuleDependencyFile != null && adModuleDependencyFile.exists()
          && adModuleDependencyFile.isFile()) {
        for (final MergedModule mergedModule : mergedModules) {
          if (!mergedModule.getJavaPackage().equals(module)) {
            if (containsString(adModuleDependencyFile, mergedModule.getAdModuleId())) {
              errors
                  .add(String
                      .format(
                          "The %s (%s) module has been merged into the Openbravo 3 distribution. However your instance has the \"%s\" module installed which declares a dependency on the merged module. You must uninstall it manually before proceeding with the update process.",
                          mergedModule.getName(), mergedModule.getJavaPackage(), module));
            }
          }
        }
      }
    }

    return errors;
  }

  /**
   * Checks the given module is not referenced as a dependency for other modules installed in the
   * instance. If so, shows a message in the log to recommend an uninstallation of that module.
   */
  private void checkRemovableModule(final MergedModule removableModule) {
    final File modulesDir = getModulesDir();
    boolean detectedDependency = false;
    boolean isRemovableModuleInstalled = false;
    for (final String module : getInstalledModules()) {
      if (module.equals(removableModule.getJavaPackage())) {
        isRemovableModuleInstalled = true;
      } else {
        final File moduleDir = new File(modulesDir, module);
        final File adModuleDependencyFile = new File(moduleDir, AD_MODULE_DEPENDENCY_PATH);
        if (adModuleDependencyFile != null && adModuleDependencyFile.exists()
            && adModuleDependencyFile.isFile()) {
          if (containsString(adModuleDependencyFile, removableModule.getAdModuleId())) {
            detectedDependency = true;
            break;
          }
        }
      }
    }

    if (isRemovableModuleInstalled && !detectedDependency) {
      log4j.warn(String.format("Orphan module detected: %s (%s). You can safely uninstall it.",
          removableModule.getName(), removableModule.getJavaPackage()));
    }
  }

  /**
   * Returns true if any of the searchStrings is found inside the file
   */
  private boolean containsString(final File file, final String searchString) {
    try {
      final Scanner scanner = new Scanner(file);
      while (scanner.hasNext()) {
        final String line = scanner.nextLine().toString();
        if (line.contains(searchString)) {
          return true;
        }
      }
    } catch (FileNotFoundException ignore) {
    }
    return false;
  }

  /**
   * Return the javapackage of the modules installed by the user (excluding Openbravo 3 distribution
   * modules). It looks at the modules folder (not into the database)
   */
  private String[] getInstalledModules() {
    final File modulesDir = getModulesDir();

    if (modulesDir.exists()) {
      return modulesDir.list(new FilenameFilter() {
        @Override
        public boolean accept(File current, String name) {
          final File currentFile = new File(current, name);
          return currentFile.exists() && currentFile.isDirectory()
              && !OPENBRAVO3MODULESLIST.contains(currentFile.getName());
        }
      });
    }

    return null;
  }

  /**
   * Return the openbravo directory for this instance
   */
  private File getOpenbravoDir() {
    final File actualDir = new File(System.getProperty("user.dir"));
    return actualDir.getParentFile().getParentFile();
  }

  /**
   * Return the openbravo/modules directory for this instance
   */
  private File getModulesDir() {
    return new File(getOpenbravoDir(), MODULES_DIRNAME);
  }

  /**
   * Return the openbravo/modules directory for this instance
   */
  private File getModulesCoreDir() {
    return new File(getOpenbravoCoreDir(), MODULES_CORE_DIRNAME);
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits(ID_CORE, null, new OpenbravoVersion(3, 0, 31553));
  }

  /**
   * Stores information about the merged modules
   * 
   */
  private class MergedModule {
    private String adModuleId;
    private String javaPackage;
    private String name;

    private MergedModule(final String adModuleId, final String javaPackage, final String name) {
      this.adModuleId = adModuleId;
      this.javaPackage = javaPackage;
      this.name = name;
    }

    String getAdModuleId() {
      return adModuleId;
    }

    String getJavaPackage() {
      return javaPackage;
    }

    String getName() {
      return name;
    }
  }
}
