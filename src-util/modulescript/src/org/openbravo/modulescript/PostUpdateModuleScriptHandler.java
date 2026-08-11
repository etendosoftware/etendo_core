/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.modulescript;

import java.io.File;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.openbravo.buildvalidation.BuildValidationHandler;
import org.openbravo.database.CPStandAlone;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.ddlutils.util.ModulesUtil;

/**
 * Ant task that discovers and executes every {@link PostUpdateModuleScript} once the database
 * update has fully completed. It is invoked by the {@code postupdate.modulescripts} target in
 * {@code src-db/database/build.xml}, after the {@code database.postupdate.*} step of
 * {@code update.database}, {@code update.database.java} and {@code import.sample.data}, and
 * before the successful-update timestamp is stamped.
 *
 * <p>Discovery mirrors {@link ModuleScriptHandler}: the core's
 * {@code src-util/modulescript/build/classes} folder plus every module's {@code build/classes}
 * folder are scanned, and any concrete class assignable to {@link PostUpdateModuleScript} is
 * instantiated and executed (in deterministic, alphabetical order per folder).</p>
 *
 * <p>Unlike {@code ModuleScriptHandler} — which dbsm invokes internally, injecting the module
 * versions captured before the update — this task runs standalone from Ant, so it recovers the
 * module versions itself from {@code AD_MODULE} (same approach as
 * {@code org.openbravo.buildvalidation.BuildValidationHandler}). Note the versions read here are
 * the POST-update ones; version-limited scripts should take this into account when defining
 * their execution limits.</p>
 */
public class PostUpdateModuleScriptHandler extends Task {
  private static final Logger log4j = LogManager.getLogger();

  private File basedir;
  private String moduleJavaPackage;
  private String propertiesFile;

  @Override
  public void execute() {
    List<String> classes = new ArrayList<String>();

    // Update the modules dir to search
    ModulesUtil.checkCoreInSources(ModulesUtil.coreInSources());

    File auxBasedir = basedir;

    // The core is in Jar
    if (!ModulesUtil.coreInSources) {
      auxBasedir = new File(ModulesUtil.getProjectRootDir());
    }

    if (moduleJavaPackage != null) {
      // We will only be executing the PostUpdateModuleScripts of a specific module
      for (String module : ModulesUtil.moduleDirs) {
        File moduleDir = new File(auxBasedir,
            module + File.separator + moduleJavaPackage + "/build/classes");
        if (moduleDir.exists()) {
          log4j.info("PostUpdate Module Script Handler - Reading class files from: "
              + moduleDir.getAbsolutePath());
          BuildValidationHandler.readClassFiles(classes, moduleDir);
          break;
        }
      }

    } else {

      /**
       * basedir should point to the base dir where the build.xml file is found.
       */
      File coreBuildFolder = new File(basedir, "src-util/modulescript/build/classes");
      BuildValidationHandler.readClassFiles(classes, coreBuildFolder);

      ArrayList<File> modFolders = new ArrayList<File>();

      /**
       * auxBaseDir is the root dir of the project
       */
      for (String module : ModulesUtil.moduleDirs) {
        File auxModule = new File(auxBasedir, module);
        if (auxModule.exists() && auxModule.isDirectory()) {
          log4j.info("PostUpdate Module Script Handler - Adding modules directories from: "
              + auxModule.getAbsolutePath());
          modFolders.addAll(Arrays.asList(auxModule.listFiles()));
        }
      }

      Collections.sort(modFolders);
      for (File modFolder : modFolders) {
        if (modFolder.isDirectory()) {
          File classesFolder = new File(modFolder, "build/classes");
          if (classesFolder.exists()) {
            BuildValidationHandler.readClassFiles(classes, classesFolder);
          }
        }
      }

    }
    Map<String, OpenbravoVersion> modulesVersionMap = getModulesVersionMap();
    for (String s : classes) {
      try {
        Class<?> myClass = Class.forName(s);
        if (PostUpdateModuleScript.class.isAssignableFrom(myClass)
            && !Modifier.isAbstract(myClass.getModifiers())) {
          PostUpdateModuleScript instance = (PostUpdateModuleScript) myClass
              .getDeclaredConstructor()
              .newInstance();
          instance.setBaseDir(basedir);
          instance.preExecute(modulesVersionMap);
        }
      } catch (Exception e) {
        log4j.error("Error executing postUpdateModuleScript: " + s, e);
        throw new BuildException("Execution of postUpdateModuleScript " + s + " failed.");
      }
    }
  }

  /**
   * Returns a File with the base directory
   *
   * @return a File with the base directory
   */
  public File getBasedir() {
    return basedir;
  }

  /**
   * Sets the base directory
   *
   * @param basedir
   *          File used to set the base directory
   */
  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  /**
   * Returns the java package
   *
   * @return a String with the module java package
   */
  public String getModuleJavaPackage() {
    return moduleJavaPackage;
  }

  /**
   * Sets the java package
   *
   * @param moduleJavaPackage
   *          String to set the java package
   */
  public void setModuleJavaPackage(String moduleJavaPackage) {
    this.moduleJavaPackage = moduleJavaPackage;
  }

  /**
   * Returns the path of the Openbravo.properties file
   *
   * @return a String with the path of the Openbravo.properties file
   */
  public String getPropertiesFile() {
    return propertiesFile;
  }

  /**
   * Sets the path of the Openbravo.properties file, used to open the standalone database
   * connection that recovers the current module versions
   *
   * @param propertiesFile
   *          String with the path of the Openbravo.properties file
   */
  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  /**
   * Returns a map with the current module versions, read from AD_MODULE. If the versions cannot
   * be recovered an empty map is returned, in which case the scripts fall back to their
   * {@code executeOnInstall()} behavior (same semantics as
   * {@code org.openbravo.buildvalidation.BuildValidationHandler}).
   *
   * @return A data structure that contains module versions mapped by module id
   */
  private Map<String, OpenbravoVersion> getModulesVersionMap() {
    Map<String, OpenbravoVersion> modulesVersion = new HashMap<String, OpenbravoVersion>();
    String strSql = "SELECT ad_module_id AS moduleid, version AS version FROM ad_module";
    try {
      ConnectionProvider cp = new CPStandAlone(propertiesFile);
      PreparedStatement ps = cp.getPreparedStatement(strSql);
      try {
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
          modulesVersion.put(rs.getString("moduleid"), new OpenbravoVersion(rs.getString("version")));
        }
      } finally {
        ps.close();
      }
    } catch (Exception e) {
      log4j.error("Not possible to recover the current version of modules", e);
    }
    return modulesVersion;
  }
}
