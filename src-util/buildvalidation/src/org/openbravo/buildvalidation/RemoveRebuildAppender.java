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
 * All portions are Copyright (C) 2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.buildvalidation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import org.openbravo.base.ExecutionLimits;
import org.openbravo.modulescript.OpenbravoVersion;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

/**
 * OBRebuildAppender was removed in 21Q1. Log4j2 was configured to make use of it to log rebuilds,
 * this script removes it from the configuration (config/log4j2.xml).
 * 
 * It is implemented as a BuildValidation instead as a ModuleScript to be executed early in the build
 * process as it affects the logging used within it.
 */
public class RemoveRebuildAppender extends BuildValidation {

  @Override
  public List<String> execute() {
    try {
      Path log4jConf = Paths.get(getSourcePath(), "config/log4j2.xml");

      if (!Files.exists(log4jConf)) {
        return Collections.emptyList();
      }

      String conf = new String(Files.readAllBytes(log4jConf));

      if (conf.indexOf("<AppenderRef ref=\"OBRebuildAppender\"/>\n") == -1) {
        return Collections.emptyList();
      }

      String newConf = conf
          .replaceAll(
              "\\s*\\n\\s*<!-- OBRebuildAppender is required for rebuilding from the GUI -->\\n",
              "")
          .replaceAll("\\s*<AppenderRef ref=\"OBRebuildAppender\"/>", "")
          .replaceAll("\\s*\\n\\s*<OBRebuildAppender name=\"OBRebuildAppender\"/>", "");
      Files.write(log4jConf, newConf.getBytes());

      // Using System.out.println instead of log4j as we don't want to load an invalid config
      System.out.println("Removed OBRebuildAppender from log4j2.xml");
    } catch (Exception e) {
      System.out.println(
          "Copy log4j config from templates failed: Log4j may not be properly configured. Please check your configuration files manually.");
    }

    return Collections.emptyList();
  }

  protected ExecutionLimits getBuildValidationLimits() {
    return new ExecutionLimits("0",
               new OpenbravoVersion("3.0.35325"), // 19Q1 (log4j2 was included)
               new OpenbravoVersion("3.0.211000")); // 21Q1
  }
}
