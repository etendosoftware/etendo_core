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

  /** Relative path, inside core or inside a module, of the compiled-classes folder to scan. */
  private static final String CLASSES_SUBDIR = "build/classes";

  private File basedir;
  private String propertiesFile;

  @Override
  public void execute() {
    Map<String, OpenbravoVersion> modulesVersionMap = readModulesVersionMap();
    for (String className : collectCandidateClassNames()) {
      runScript(className, modulesVersionMap);
    }
  }

  /**
   * Collects the fully qualified names of every compiled class found in the scanned folders:
   * the core's {@code src-util/modulescript/build/classes} plus each module's
   * {@code build/classes}. Filtering by type happens later, in {@link #runScript}.
   */
  private List<String> collectCandidateClassNames() {
    ModulesUtil.checkCoreInSources(ModulesUtil.coreInSources());
    // basedir points to where build.xml lives; when the core ships as a jar the modules hang from
    // the project root instead.
    File projectRoot = ModulesUtil.coreInSources ? basedir
        : new File(ModulesUtil.getProjectRootDir());

    List<String> classNames = new ArrayList<>();
    BuildValidationHandler.readClassFiles(classNames,
        new File(basedir, "src-util/modulescript/" + CLASSES_SUBDIR));
    for (File moduleClassesDir : listModuleClassesDirs(projectRoot)) {
      BuildValidationHandler.readClassFiles(classNames, moduleClassesDir);
    }
    return classNames;
  }

  /**
   * Lists, in deterministic (sorted) order, the {@code build/classes} folder of every module
   * found under each of the {@link ModulesUtil#moduleDirs} roots.
   */
  private List<File> listModuleClassesDirs(File projectRoot) {
    List<File> moduleFolders = new ArrayList<>();
    for (String moduleDirName : ModulesUtil.moduleDirs) {
      File moduleDir = new File(projectRoot, moduleDirName);
      File[] entries = moduleDir.isDirectory() ? moduleDir.listFiles() : null;
      if (entries == null) {
        continue;
      }
      log4j.info("PostUpdate Module Script Handler - Adding modules directories from: "
          + moduleDir.getAbsolutePath());
      moduleFolders.addAll(Arrays.asList(entries));
    }
    Collections.sort(moduleFolders);

    List<File> classesDirs = new ArrayList<>();
    for (File moduleFolder : moduleFolders) {
      File classesDir = new File(moduleFolder, CLASSES_SUBDIR);
      if (moduleFolder.isDirectory() && classesDir.exists()) {
        classesDirs.add(classesDir);
      }
    }
    return classesDirs;
  }

  /**
   * Instantiates and executes {@code className} if (and only if) it is a concrete
   * {@link PostUpdateModuleScript}; anything else found in the scanned folders is skipped. A
   * {@link BuildException} thrown by the script itself (e.g. a validator carrying its own
   * detailed report) is rethrown untouched; any other failure is wrapped into a generic one.
   */
  private void runScript(String className, Map<String, OpenbravoVersion> modulesVersionMap) {
    try {
      Class<?> clazz = Class.forName(className);
      if (!PostUpdateModuleScript.class.isAssignableFrom(clazz)
          || Modifier.isAbstract(clazz.getModifiers())) {
        return;
      }
      PostUpdateModuleScript instance = (PostUpdateModuleScript) clazz.getDeclaredConstructor()
          .newInstance();
      instance.setBaseDir(basedir);
      instance.preExecute(modulesVersionMap);
    } catch (BuildException e) {
      throw e;
    } catch (Exception e) {
      log4j.error("Error executing postUpdateModuleScript: " + className, e);
      throw new BuildException("Execution of postUpdateModuleScript " + className + " failed.");
    }
  }

  /**
   * Reads the current (POST-update) module versions from {@code AD_MODULE} through a standalone
   * connection built from {@link #propertiesFile}. On any failure an empty map is returned, in
   * which case the scripts fall back to their {@code executeOnInstall()} behavior (same
   * semantics as {@code org.openbravo.buildvalidation.BuildValidationHandler}).
   */
  private Map<String, OpenbravoVersion> readModulesVersionMap() {
    Map<String, OpenbravoVersion> modulesVersion = new HashMap<>();
    try (PreparedStatement ps = new CPStandAlone(propertiesFile)
        .getPreparedStatement("SELECT ad_module_id, version FROM ad_module");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        modulesVersion.put(rs.getString(1), new OpenbravoVersion(rs.getString(2)));
      }
    } catch (Exception e) {
      log4j.error("Not possible to recover the current version of modules", e);
    }
    return modulesVersion;
  }

  /**
   * Ant property: base directory, i.e. where {@code src-db/database/build.xml} lives.
   */
  public void setBasedir(File basedir) {
    this.basedir = basedir;
  }

  /**
   * Ant property: path of the {@code Openbravo.properties} file used to open the standalone
   * database connection that recovers the current module versions.
   */
  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }
}
