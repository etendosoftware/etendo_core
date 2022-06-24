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
 * All portions are Copyright (C) 2008-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.modules;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.database.CPStandAlone;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.utility.DataSet;
import org.openbravo.service.dataset.DataSetService;
import org.openbravo.service.db.DataExportService;

/**
 * ExtractModule is able to extract an obx file for a module
 * 
 * 
 */
public class ExtractModule {
  private static ConnectionProvider pool;
  static Logger log4j = LogManager.getLogger();
  private String relativeDir;
  private String modulesBaseDir;
  private String destDir;
  private boolean exportReferenceData;
  private List<String> processedModules;
  private boolean addAllDependencies = false;

  /**
   * Initializes a ExtractModule instance for Stand Alone conection
   * 
   * @param xmlPoolFile
   *          Openbravo.properties file with pool information
   * @param modulesDir
   *          Base directory for modules
   */
  public ExtractModule(String xmlPoolFile, String modulesDir) {
    pool = new CPStandAlone(xmlPoolFile);
    modulesBaseDir = modulesDir;
    processedModules = new ArrayList<String>();
  }

  /**
   * Generates the obx file for the module/template/package
   * 
   * @param moduleID
   *          Identifier for the module
   */
  public void extract(String moduleID) {
    try {
      final ExtractModuleData module = ExtractModuleData.selectDirectory(pool, moduleID);

      log4j.info("Extracting module: " + module.javapackage);
      final String moduleDirectory = modulesBaseDir + "/modules/" + module.javapackage;

      if (!(new File(moduleDirectory).exists())) {

        throw new OBException("Directory " + moduleDirectory
            + " for module does not exist.\nYou may have not exported database before packaging this module");
      }

      final FileOutputStream file = new FileOutputStream(
          destDir + "/" + module.javapackage + "-" + module.version + ".obx");
      final ZipOutputStream obx = new ZipOutputStream(file);
      extractModule(moduleID, moduleDirectory, obx);
      log4j.info("addAllDependencies: " + addAllDependencies);
      if (addAllDependencies || module.type.equals("P") || module.type.equals("T")) {
        log4j.info(module.javapackage + " looking for inner modules...");
        extractPackage(moduleID, obx);
      }
      obx.close();
      log4j.info(
          "Completed file: " + destDir + "/" + module.javapackage + "-" + module.version + ".obx");
    } catch (final Exception e) {
      log4j.error("Error packaging module", e);
    }

  }

  /**
   * Extracts the reference data for the module
   * 
   * @param moduleId
   * @param moduleDirectory
   */
  private void extractReferenceData(String moduleId, String moduleDirectory) {
    final List<DataSet> dss = DataSetService.getInstance().getDataSetsByModuleID(moduleId);
    final File modDir = new File(moduleDirectory);
    if (!modDir.exists()) {
      modDir.mkdir();
    }
    final File refDir = new File(modDir, "referencedata");
    if (!refDir.exists()) {
      refDir.mkdir();
    }
    final File stdDir = new File(refDir, "standard");
    if (!stdDir.exists()) {
      stdDir.mkdir();
    }

    for (final DataSet ds : dss) {
      try {
        final String xml = DataExportService.getInstance()
            .exportDataSetToXML(ds, moduleId, new HashMap<String, Object>());
        if (xml != null) {
          final String fileName = ds.getName() + ".xml";
          final OutputStreamWriter osWriter = new OutputStreamWriter(
              new FileOutputStream(new File(stdDir, fileName)), "UTF-8");
          osWriter.write(xml);
          osWriter.close();
        }
      } catch (final Exception e) {
        throw new OBException("Exception while extracting referencedata for module: " + moduleId
            + " and dataset " + ds.getName(), e);
      }
    }
  }

  public void extractName(String name) throws Exception {
    log4j.info("Looking for ID for module with package: " + name);
    log4j.info("Extract reference data: " + exportReferenceData);
    final String id = ExtractModuleData.selectID(pool, name);
    if (id == null || id.equals("")) {
      throw new Exception("Module ID not fond");
    }
    extract(id);
  }

  /**
   * Extracts a single module
   * 
   * @param moduleDirectory
   * @param obx
   * @throws Exception
   */
  private void extractModule(String moduleID, String moduleDirectory, ZipOutputStream obx)
      throws Exception {
    if (exportReferenceData && ExtractModuleData.hasReferenceData(pool, moduleID)) {
      extractReferenceData(moduleID, moduleDirectory);
    }
    if ("0".equals(moduleID)) {
      log4j.info("obx for core...");
      relativeDir = modulesBaseDir + File.separator;
      createOBX(ModuleUtiltiy.getCore(modulesBaseDir), obx);
    } else {
      relativeDir = modulesBaseDir + File.separator + "modules" + File.separator;
      createOBX(new File(moduleDirectory), obx);
    }
  }

  /**
   * Extracts a package
   * 
   * @param moduleID
   * @param obx
   * @throws Exception
   */
  private void extractPackage(String moduleID, ZipOutputStream obx) throws Exception {
    final ExtractModuleData modules[] = ExtractModuleData.selectContainedModules(pool, moduleID,
        addAllDependencies ? "Y" : "N");
    for (int i = 0; i < modules.length; i++) {
      if (processedModules.contains(modules[i].adModuleId)) {
        log4j.info("Skipping already processed module:" + modules[i].javapackage);
        continue;
      }
      processedModules.add(modules[i].adModuleId);
      if (!addAllDependencies && modules[i].adModuleId.equals("0")) {
        log4j.warn("Core is included! It is not going to be packaged...");
      } else {
        obx.putNextEntry(new ZipEntry(modules[i].javapackage + "-" + modules[i].version + ".obx"));
        final ByteArrayOutputStream ba = new ByteArrayOutputStream();
        final ZipOutputStream moduleObx = new ZipOutputStream(ba);
        final String moduleDirectory = modulesBaseDir + "/modules/" + modules[i].javapackage;
        log4j.info("Extracting module: " + modules[i].javapackage);
        extractModule(modules[i].adModuleId, moduleDirectory, moduleObx);
        if (addAllDependencies || modules[i].type.equals("P") || modules[i].type.equals("T")) {
          // If it is Package or Template it can contain other modules
          log4j.info(modules[i].javapackage + " looking for inner modules...");
          extractPackage(modules[i].adModuleId, moduleObx);
        }
        moduleObx.close();
        ba.flush();
        obx.write(ba.toByteArray());

        obx.closeEntry();
      }
    }
  }

  /**
   * Crates a zip file (obx) with the module contents
   * 
   * @param file
   *          directory to zip
   * @param obx
   * @throws Exception
   */
  private void createOBX(File file, ZipOutputStream obx) throws Exception {
    File[] list;
    if (file.isDirectory()) {
      list = file.listFiles(
          (f, name) -> !(name.equals(".svn") || name.equals(".hg") || name.equals(".git")));
    } else {
      list = new File[] { file };
    }
    String fileSeparator = File.separator;
    for (int i = 0; list != null && i < list.length; i++) {
      if (list[i].isDirectory()) {
        // add entry for directory
        obx.putNextEntry(new ZipEntry(
            new ZipEntry(list[i].toString().replace(relativeDir, "").replace(fileSeparator, "/"))
                + "/"));
        obx.closeEntry();
        createOBX(list[i], obx);
      } else {
        // add entry for file (and compress it)
        final byte[] buf = new byte[1024];
        obx.putNextEntry(
            new ZipEntry(list[i].toString().replace(relativeDir, "").replace(fileSeparator, "/")));
        final FileInputStream in = new FileInputStream(list[i].toString());
        int len;
        while ((len = in.read(buf)) > 0) {
          obx.write(buf, 0, len);
        }
        obx.closeEntry();
        in.close();
      }
    }
  }

  private void createOBX(List<File> files, ZipOutputStream obx) throws Exception {
    for (File f : files) {
      createOBX(f, obx);
    }
  }

  public void setDestDir(String d) {
    destDir = d;
  }

  public void setExportReferenceData(boolean export) {
    exportReferenceData = export;
  }

  public void setAddAllDependencies(boolean addAllDependencies) {
    this.addAllDependencies = addAllDependencies;
  }
}
