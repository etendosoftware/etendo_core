/*
 ************************************************************************************
 * Copyright (C) 2010-2018 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.buildvalidation;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.database.CPStandAlone;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

public class BuildValidationHandler {
  private static final Logger log4j = LogManager.getLogger();

  /** Prerequisite validations are ensured to be executed before the rest of other ones */
  private static final List<String> prerequisiteValidations = Collections.emptyList();

  private static File basedir;
  private static String module;
  private static String propertiesFile;

  public static void main(String[] args) {
    basedir = new File(args[0]);
    module = null; // The module is not set so that all BuildValidations are always executed.

    propertiesFile = args[2];
    log4j.debug("basedir = " + basedir + ", propertiesFile = " + propertiesFile);
    List<String> classes = new ArrayList<String>();
    ArrayList<File> modFolders = new ArrayList<File>();
    if (module != null && !module.equals("%")) {
      String[] javapackages = module.split(",");
      for (String javapackage : javapackages) {
        File moduleFolder = new File(basedir, "modules/" + javapackage);
        modFolders.add(moduleFolder);
      }
      Collections.sort(modFolders);
    } else {
      File coreBuildFolder = new File(basedir, "src-util/buildvalidation/build/classes");
      readClassFiles(classes, coreBuildFolder);
      File moduleFolder = new File(basedir, "modules");
      for (File f : moduleFolder.listFiles()) {
        modFolders.add(f);
      }
      Collections.sort(modFolders);
    }

    for (File modFolder : modFolders) {
      if (modFolder.isDirectory()) {
        File validationFolder = new File(modFolder, "build/classes");
        if (validationFolder.exists()) {
          readClassFiles(classes, validationFolder);
        }
      }
    }

    Map<String, OpenbravoVersion> modulesVersionMap = getModulesVersionMap();
    sortPrerequisites(classes);

    for (String s : classes) {
      List<String> errors = new ArrayList<String>();
      try {
        Class<?> myClass = Class.forName(s);
        if (BuildValidation.class.isAssignableFrom(myClass)) {
          BuildValidation instance = (BuildValidation) myClass.getDeclaredConstructor()
              .newInstance();
          instance.preExecute(modulesVersionMap);
          errors = instance.getErrors();
        }
      } catch (Exception e) {
        log4j.info("Error executing build-validation: " + s, e);
        log4j.error("The build validation " + s + " couldn't be properly executed");
        System.exit(1);
      }
      if (errors.size() > 0) {
        log4j.error("The build validation failed.");
        printMessage(errors);
        System.exit(1);
      }
    }
  }

  /** Prerequisites are set at the beginning of the list */
  private static void sortPrerequisites(List<String> classes) {
    Collections.reverse(prerequisiteValidations);
    for (String prerequisite : prerequisiteValidations) {
      if (classes.remove(prerequisite)) {
        classes.add(0, prerequisite);
      }
    }
  }

  private static void printMessage(List<String> errors) {
    String errorMessage = "";
    for (String error : errors) {
      errorMessage += error + "\n";
    }
    log4j.error(errorMessage);
  }

  public static void readClassFiles(List<String> coreClasses, File file) {
    ArrayList<String> newClasses = new ArrayList<String>();
    readClassFilesExt(newClasses, file);
    Collections.sort(newClasses);
    coreClasses.addAll(newClasses);
  }

  private static void readClassFilesExt(List<String> coreClasses, File file) {
    if (!file.exists()) {
      return;
    }
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (File f : files) {
        readClassFiles(coreClasses, f);
      }
    } else {
      if (file.getAbsolutePath().endsWith(".class")) {
        String fileName = file.getAbsolutePath();
        // Replace Windows separator characters by / to make replacement simpler
        fileName = fileName.replace("\\", "/");
        fileName = fileName.split("build/classes/")[1];
        coreClasses.add(fileName.replace(".class", "").replace('/', '.'));
      }
    }
  }

  public File getBasedir() {
    return basedir;
  }

  public void setBasedir(File basedir) {
    BuildValidationHandler.basedir = basedir;
  }

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    BuildValidationHandler.module = module;
  }

  /**
   * Returns a map with the current module versions
   *
   * @return A data structure that contains module versions mapped by module id
   */
  private static Map<String, OpenbravoVersion> getModulesVersionMap() {
    Map<String, OpenbravoVersion> modulesVersion = new HashMap<String, OpenbravoVersion>();
    String strSql = "SELECT ad_module_id AS moduleid, version AS version FROM ad_module";
    PreparedStatement ps = null;
    try {
      ConnectionProvider cp = getConnectionProvider();
      ps = cp.getPreparedStatement(strSql);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        String moduleId = rs.getString("moduleid");
        String modVersion = rs.getString("version");
        modulesVersion.put(moduleId, new OpenbravoVersion(modVersion));
      }
    } catch (Exception e) {
      log4j.error("Not possible to recover the current version of modules", e);
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        // won't happen
      }
    }
    return modulesVersion;
  }

  private static ConnectionProvider getConnectionProvider() {
    ConnectionProvider cp = null;
    cp = new CPStandAlone(propertiesFile);
    return cp;
  }
}
