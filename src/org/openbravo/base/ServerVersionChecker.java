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
 * All portions are Copyright (C) 2016-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.base;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks Etendo is deployed on a supported Server version, in case it is not, an error message
 * is logged and deployment is stopped. Currently, checks are performed only for Tomcat.
 * 
 * @author alostale
 *
 */
public class ServerVersionChecker implements ServletContextListener {
  private static final String MINIMUM_TOMCAT_VERSION = "8.5";

  private static final Logger log = LogManager.getLogger();

  private static String serverName;
  private static Version serverVersion;

  @Override
  public void contextInitialized(ServletContextEvent event) {
    String serverInfo = event.getServletContext().getServerInfo();
    log.debug("Server: {}", serverInfo);

    setServerInfo(serverInfo);

    if (!isRunningOnTomcat(serverInfo)) {
      log.info("Running on " + serverInfo);
      // we only check Tomcat
      return;
    }

    if (!serverVersion.isValid()) {
      log.info("Unknown Tomcat version {}", serverInfo);
      return;
    }

    if (serverVersion.compareTo(new Version(MINIMUM_TOMCAT_VERSION)) < 0) {
      log.error(
          "The minimum Tomcat version required deploy Etendo is {}. Trying to deploy it in {} is not allowed. Please, upgrade Tomcat.",
          MINIMUM_TOMCAT_VERSION, serverInfo);
      System.exit(1);
    }
  }

  private static void setServerInfo(String serverInfo) {
    try {
      Matcher versionMatcher = Pattern.compile("([^\\d/]*)[/]?([\\d\\.]*)").matcher(serverInfo);
      if (versionMatcher.find()) {
        serverName = versionMatcher.group(1);
        serverVersion = new Version(versionMatcher.group(2));
      }
    } catch (Exception ignore) {
    }
  }

  private boolean isRunningOnTomcat(String serverInfo) {
    return serverInfo.toLowerCase().contains("tomcat");
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
  }

  /** Returns current Servlet Container's name (ie. "Apache Tomcat") */
  public static String getServerName() {
    return serverName;
  }

  /** Returns current Servlet Container's full version (ie. "8.5.31") */
  public static String getServerVersion() {
    return serverVersion.getLiteral();
  }

  private static class Version implements Comparable<Version> {
    String literal;

    public Version(String versionLiteral) {
      literal = versionLiteral;
    }

    public String getLiteral() {
      return literal;
    }

    public boolean isValid() {
      try {
        Matcher versionMatcher = Pattern.compile("[^\\d]*([\\d]*)").matcher(literal);
        if (versionMatcher.find()) {
          return true;
        }
      } catch (Exception ignore) {
      }
      return false;
    }

    @Override
    public int compareTo(Version o) {
      if (this.literal.equals(o.literal)) {
        return 0;
      }
      final String[] version1 = this.literal.split("\\.");
      final String[] version2 = o.literal.split("\\.");
      final int minorVers = version1.length > version2.length ? version2.length : version1.length;
      for (int i = 0; i < minorVers; i++) {
        if (version1[i].equals(version2[i])) {
          continue;
        }
        try {
          return Integer.valueOf(version1[i]).compareTo(Integer.valueOf(version2[i]));
        } catch (NumberFormatException e) {
          // Not possible to compare
          return -1;
        }
      }
      return 0;
    }
  }

}
