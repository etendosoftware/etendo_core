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
 * All portions are Copyright (C) 2015-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.buildvalidation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This build validation will be executed when updating from a version lower or equal to PR15Q3.
 *
 * This validation sets the value of the property db.externalPoolClassName in Openbravo.property if
 * it does not exist or its value is set to empty. The default value used will be the one defined
 * in Openbravo.properties.template
 * 
 */
public class CheckUpdateConnectionPoolMerge extends BuildValidation {

  private final static String PROPERTY_CONNECTION_POOL = "db.externalPoolClassName";
  private final static String PATH_OPENBRAVO_PROPERTIES = "/config/Openbravo.properties";
  private final static String PATH_OPENBRAVO_PROPERTIES_TEMPLATE = "/config/Openbravo.properties.template";
  private final static String SUFFIX_AUX = "_aux";

  private static Logger log = LogManager.getLogger();

  @Override
  public List<String> execute() {
    try {
      setDefaultConnectionPoolInOpenbravoPropertiesIfNotSetOrEmpty();
    } catch (Exception e) {
      return handleError(e);
    }
    return new ArrayList<>();
  }

  private void setDefaultConnectionPoolInOpenbravoPropertiesIfNotSetOrEmpty() throws Exception {
    String obDir = getSourcePathFromOBProperties();
    String openbravoPropertiesPath = obDir + PATH_OPENBRAVO_PROPERTIES;

    Properties openbravoProperties = openPropertiesFile(openbravoPropertiesPath);
    String connectionPoolValue = openbravoProperties.getProperty(PROPERTY_CONNECTION_POOL);

    if (connectionPoolValue == null || connectionPoolValue.isEmpty()) {
      String externalPoolClassName = getExternalPoolClassNameFromTemplate();
      replacePropertyValue(openbravoPropertiesPath, PROPERTY_CONNECTION_POOL, externalPoolClassName);
      log.info("External DB Pool class name property not found. Set to " + externalPoolClassName);
    }
    else {
      log.info("External DB Pool class name already defined: " + connectionPoolValue);
    }
  }

  private Properties openPropertiesFile(String path) throws IOException {
    Properties propertiesFile = new Properties();
    propertiesFile.load(new FileInputStream(path));
    return propertiesFile;
  }

  private String getExternalPoolClassNameFromTemplate() throws IOException {
    String obDir = getSourcePathFromOBProperties();
    String openbravoPropertiesTemplatePath = obDir + PATH_OPENBRAVO_PROPERTIES_TEMPLATE;

    Properties openbravoPropertiesTemplate = openPropertiesFile(openbravoPropertiesTemplatePath);
    return openbravoPropertiesTemplate.getProperty(PROPERTY_CONNECTION_POOL);
  }

  private void replacePropertyValue(String openbravoPropertiesPath, String propertyName,
      String newValue) throws Exception {
    try {
      File fileW = new File(openbravoPropertiesPath);

      replaceProperty(fileW, openbravoPropertiesPath + SUFFIX_AUX, propertyName, "=" + newValue);
      try {
        fileW.delete();
        File fileAux = new File(openbravoPropertiesPath + SUFFIX_AUX);
        fileAux.renameTo(new File(openbravoPropertiesPath));
      } catch (Exception ex) {
        log.error("Error renaming/deleting Openbravo.properties", ex);
        throw ex;
      }
    } catch (IOException e) {
      log.error("Error read/write Openbravo.properties", e);
      throw e;
    }
  }

  /**
   * Replaces a value changeOption in addressFilePath. FileR is used to check that exists
   * searchOption with different value.
   * 
   * Extract from original method in org.openbravo.configuration.ConfigurationApp.java. It is
   * necessary because build validations can not work with external methods.
   * 
   * @param fileR
   *          old file to read
   * @param addressFilePath
   *          file to write new property
   * @param searchOption
   *          Prefix to search
   * @param changeOption
   *          Value to write in addressFilePath
   */
  private void replaceProperty(File fileR, String addressFilePath, String searchOption,
      String changeOption) throws Exception {
    boolean isFound = false;
    // auxiliary file to rewrite
    File fileW = new File(addressFilePath);
    try (
      FileReader fr = new FileReader(fileR);
      BufferedReader br = new BufferedReader(fr);
      FileWriter fw = new FileWriter(fileW)
      ) {
      // data for restore
      String line;
      while ((line = br.readLine()) != null) {
        if (line.indexOf(searchOption) == 0) {
          // Replace new option
          line = line.replace(line, searchOption + changeOption);
          isFound = true;
        }
        fw.write(line + "\n");
      }
      if (!isFound) {
        fw.write(searchOption + changeOption);
      }
    }
  }

  /**
   * Searches an option in filePath file and returns the value of searchOption.
   * 
   * Extract from original method in org.openbravo.configuration.ConfigurationApp.java. It is
   * necessary because build validations can not work with external methods.
   * 
   * @param filePath
   *          Path of file
   * @param searchOption
   *          Prefix of property to search
   * @return valueFound Value found
   */
  private String searchProperty(File filePath, String searchOption) {
    String valueFound = "";
    try (
      FileReader fr = new FileReader(filePath);
      BufferedReader br = new BufferedReader(fr)
      ) {
      String line;
      while ((line = br.readLine()) != null) {
        if (line.indexOf(searchOption) == 0) {
          valueFound = line.substring(searchOption.length() + 1);
          break;
        }
      }
    } catch (Exception e) {
      log.error("Exception searching a property: ", e);
    }
    return valueFound;
  }

  /**
   * Gets source.path property from Openbravo.properties file
   * 
   */
  private String getSourcePathFromOBProperties() {
    // get the location of the current class file
    final URL url = this.getClass().getResource(getClass().getSimpleName() + ".class");
    File f = new File(url.getPath());
    File propertiesFile = null;
    while (f.getParentFile() != null && f.getParentFile().exists()) {
      f = f.getParentFile();
      final File configDirectory = new File(f, "config");
      if (configDirectory.exists()) {
        propertiesFile = new File(configDirectory, "Openbravo.properties");
        if (propertiesFile.exists()) {
          // found it and break
          break;
        }
      }
    }
    return searchProperty(propertiesFile, "source.path");
  }

  @Override
  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0", null, new OpenbravoVersion(3, 0, 27659));
  }
}
