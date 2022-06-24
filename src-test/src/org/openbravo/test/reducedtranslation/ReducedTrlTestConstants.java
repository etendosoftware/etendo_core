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
package org.openbravo.test.reducedtranslation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Some constants to be used in tests */

class ReducedTrlTestConstants {
  private static final Logger log = LogManager.getLogger();

  private static Path TRLFOLDER;
  static {
    try {
      TRLFOLDER = Files.createTempDirectory("trl");
    } catch (IOException e) {
      log.error("Error while creating temporary directory.", e);
    }
  }

  public static final Path FULL_TRL_DIR = TRLFOLDER.resolve("full");
  public static final Path REDUCED_TRL_DIR = TRLFOLDER.resolve("reduced");
  public static final String APPLICATION_DICTIONARY_MENU_ID = "153";
  public static final String EXCLUDE_FROM_REDUCED_TRANSLATION = "EXCLUDE_FROM_REDUCED_TRL";
  public static final String ELEMENT_MENU_ID = "138";

  private ReducedTrlTestConstants() {
  }

}
