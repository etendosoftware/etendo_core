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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * This script will be executed only when migrating from a version which still supports log4j 1.x
 * and copies all new configuration files from the template
 */
public class CopyLog4jConfigurationFromTemplates extends BuildValidation {

  private static final String CONFIG_DIR = "/config/";
  private static final String TEST_SRC_DIR = "/src-test/src/";
  private static final String LOG4J_CONF_FILE = "log4j2.xml";
  private static final String LOG4J_WEB_CONF_FILE = "log4j2-web.xml";
  private static final String LOG4J_TEST_CONF_FILE = "log4j2-test.xml";

  private static final String DEFECTIVE_CONFIG_MD5_HASH_AS_BASE_64 = "6iGQxrhHHGR7JVS7PKS0mw==";

  @Override
  public List<String> execute() {
    try {
      String sourcePath = getSourcePath();
      copyFromTemplateFile(sourcePath + CONFIG_DIR + LOG4J_CONF_FILE);
      copyFromTemplateFile(sourcePath + CONFIG_DIR + LOG4J_WEB_CONF_FILE);
      copyFromTemplateFile(sourcePath + TEST_SRC_DIR + LOG4J_TEST_CONF_FILE);
      replaceDefectiveLog4jWebConfig(sourcePath + CONFIG_DIR + LOG4J_WEB_CONF_FILE);
    } catch (Exception e) {
      System.out.println(
          "Copy log4j config from templates failed: Log4j may not be properly configured. Please check your configuration files manually.");
    }

    return new ArrayList<>();
  }

  /**
   * Replace existing log4j-web config with the current template only if it is an exact copy of an
   * older version that has a bug that lets log archives grow indefinitely.
   *
   * See issue https://issues.openbravo.com/view.php?id=42556 for more info.
   */
  private void replaceDefectiveLog4jWebConfig(String targetPath) throws Exception {
    Path target = Paths.get(targetPath);

    if (Files.exists(target) && fileMatchesMd5(target, DEFECTIVE_CONFIG_MD5_HASH_AS_BASE_64)) {
      Path source = Paths.get(targetPath + ".template");
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
      System.out.println(targetPath
          + " is overriden with template file content. Please check this configuration is correct.");
    }
  }

  private boolean fileMatchesMd5(Path file, String md5sumBase64) throws Exception {
    byte[] fileHash = MessageDigest.getInstance("MD5").digest(Files.readAllBytes(file));
    String fileMd5 = Base64.getEncoder().encodeToString(fileHash);
    return fileMd5.equals(md5sumBase64);
  }

  private void copyFromTemplateFile(String targetPath) throws Exception {
    Path source = Paths.get(targetPath + ".template");
    Path target = Paths.get(targetPath);

    if (Files.notExists(target)) {
      Files.copy(source, target);
      System.out.println(targetPath
          + " is copied from template file. Please check this configuration is correct.");
    }
  } 
}

