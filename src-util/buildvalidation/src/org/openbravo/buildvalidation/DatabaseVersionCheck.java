/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  
 *************************************************************************
 */
package org.openbravo.buildvalidation;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.ExecutionLimits;
import org.openbravo.buildvalidation.BuildValidation;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.modulescript.OpenbravoVersion;

/**
 * This validation prevents system to be built in unsupported database versions.
 */
public class DatabaseVersionCheck extends BuildValidation {
  private static Logger log4j = LogManager.getLogger();
  private final static String POSTGRES = "PostgreSQL";
  private final static String ORACLE = "Oracle";
  private final static String MIN_PG_VERSION = "10";
  private final static String MIN_ORA_VERSION = "19";
  private final static String[] WARNING_PG_VERSIONS = {};
  private final static String[] WARNING_ORA_VERSIONS = {};

  @Override
  public List<String> execute() {
    ConnectionProvider cp = getConnectionProvider();
    try {
      if (cp.getRDBMS().equalsIgnoreCase("POSTGRE")) {
        String postgresVersion = getVersion(DatabaseVersionCheckData.getPostgresVersion(cp));
        return checkDatabaseVersion(postgresVersion, MIN_PG_VERSION, WARNING_PG_VERSIONS, POSTGRES);
      } else if (cp.getRDBMS().equalsIgnoreCase("ORACLE")) {
        String oracleVersion = getVersion(DatabaseVersionCheckData.getOracleVersion(cp));
        return checkDatabaseVersion(oracleVersion, MIN_ORA_VERSION, WARNING_ORA_VERSIONS, ORACLE);
      } else {
        return new ArrayList<String>();
      }
    } catch (Exception e) {
      return handleError(e);
    }
  }

  private ArrayList<String> checkDatabaseVersion(String databaseVersion, String minVersion,
      String[] warningVersions, String databaseType) {
    ArrayList<String> errors = new ArrayList<String>();
    if (databaseVersion != null && !StringUtils.isEmpty(databaseVersion)) {
      if (compareVersion(databaseVersion, minVersion) < 0) {
        String msg1 = "The current " + databaseType + " database version (" + databaseVersion
            + ") is not supported. Minimum supported version is " + minVersion;
        String msg2 = "Please, visit the following link: http://wiki.openbravo.com/wiki/System_Requirements "
            + "to check the list of supported versions.";
        errors.add(msg1);
        errors.add(msg2);
      } else {
        for (String version : warningVersions) {
          if (compareVersion(databaseVersion, version) == 0) {
            log4j
                .warn("The current "
                    + databaseType
                    + " database version ("
                    + databaseVersion
                    + ") is not the recommended one. Please, visit the following link: http://wiki.openbravo.com/wiki/System_Requirements "
                    + "to check the current recommended version.");
            break;
          }
        }
      }
    }
    return errors;
  }

  private int compareVersion(String v1, String v2) {
    if (v1.equals(v2))
      return 0;
    final String[] version1 = v1.split("\\.");
    final String[] version2 = v2.split("\\.");
    final int minorVers = version1.length > version2.length ? version2.length : version1.length;
    for (int i = 0; i < minorVers; i++) {
      if (version1[i].equals(version2[i]))
        continue;
      try {
        return Integer.valueOf(version1[i]).compareTo(Integer.valueOf(version2[i]));
      } catch (NumberFormatException e) {
        // Not possible to compare
        return -1;
      }
    }
    return 0;
  }

  private String getVersion(String str) {
    String version = "";
    if (str == null)
      return "";
    final Pattern pattern = Pattern.compile("((\\d+\\.)+)\\d+");
    final Matcher matcher = pattern.matcher(str);
    if (matcher.find()) {
      version = matcher.group();
    }
    return version;
  }
}
