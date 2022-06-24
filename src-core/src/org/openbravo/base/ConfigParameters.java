/*
 ************************************************************************************
 * Copyright (C) 2001-2021 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */

package org.openbravo.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Application parameters stored in web.xml as init parameters.
 * 
 * @author Ben Sommerville
 */
public class ConfigParameters implements Serializable {

  static final long serialVersionUID = 1L;

  public final static String CONFIG_ATTRIBUTE = "openbravoConfig";

  private final String strBaseConfigPath;
  public final String strBaseDesignPath;
  private final boolean isFullPathBaseDesignPath;
  public final String strDefaultDesignPath;
  public final String strLocalReplaceWith;
  public final String strBBDD = null;
  public final String strVersion;
  public final String strParentVersion;
  public String prefix;
  public final String strContext;
  private final String strFileFormat;
  public final String strSystemLanguage;
  public final String strDefaultServlet;
  private final String stcFileProperties;
  public final String strReplaceWhat;
  private final String poolFileName;
  public final String strTextDividedByZero;

  private static final Logger log4j = LogManager.getLogger();

  public final String loginServlet;
  public final String strServletSinIdentificar;
  private final String strServletGoBack;
  public final String strFTPDirectory;
  public final Long periodicBackgroundTime;
  public final String strLogFileAcctServer;

  private final Properties propFileProperties;

  // Default fall-back formats, used when properties are not present
  private static final String DEFAULT_JAVA_DATETIME_FORMAT = "dd-MM-yyyy HH:mm:ss";
  private static final String DEFAULT_SQL_DATETIME_FORMAT = "DD-MM-YYYY HH24:MI:SS";

  public static ConfigParameters retrieveFrom(ServletContext context) {
    ConfigParameters params = (ConfigParameters) context.getAttribute(CONFIG_ATTRIBUTE);
    if (params == null) {
      params = new ConfigParameters(context);
      params.storeIn(context);
    }
    return params;
  }

  public ConfigParameters(ServletContext context) {
    prefix = context.getRealPath("/");
    if (!prefix.endsWith("/")) {
      prefix += "/";
    }
    strContext = extractContext(getActualPathContext());

    strBaseConfigPath = getResolvedParameter(context, "BaseConfigPath");

    log4j.debug("context: " + strContext);
    log4j.debug("************************prefix: " + prefix);

    stcFileProperties = prefix + "/" + strBaseConfigPath + "/" + "Openbravo.properties";
    propFileProperties = loadOBProperties();

    String s = "FormatFile";
    strFileFormat = getResolvedParameter(context, s);

    strBaseDesignPath = trimTrailing(getResolvedParameter(context, "BaseDesignPath"), "/");
    isFullPathBaseDesignPath = determineIsFullDesignPath();
    strDefaultDesignPath = getResolvedParameter(context, "DefaultDesignPath");
    strDefaultServlet = getResolvedParameter(context, "DefaultServlet");
    strReplaceWhat = getResolvedParameter(context, "ReplaceWhat");

    log4j.debug("BaseConfigPath: " + strBaseConfigPath);
    log4j.debug("BaseDesignPath: " + strBaseDesignPath);

    strVersion = getResolvedParameter(context, "Version");
    strParentVersion = getResolvedParameter(context, "Parent_Version");

    strSystemLanguage = getSystemLanguage();
    strLocalReplaceWith = getResolvedParameter(context, "ReplaceWith");
    strTextDividedByZero = getResolvedParameter(context, "TextDividedByZero");

    poolFileName = getResolvedParameter(context, "PoolFile");

    loginServlet = getResolvedParameter(context, "LoginServlet");
    strServletSinIdentificar = getResolvedParameter(context, "LoginServlet");
    strServletGoBack = getResolvedParameter(context, "ServletGoBack");
    log4j.debug("strServletGoBack: " + strServletGoBack);
    periodicBackgroundTime = asLong(getResolvedParameter(context, "PeriodicBackgroundTime"));
    strLogFileAcctServer = prefix + "/" + strBaseConfigPath + "/"
        + getResolvedParameter(context, "LogFileAcctServer");

    strFTPDirectory = getResolvedParameter(context, "AttachmentDirectory");
    try {
      File f = new File(strFTPDirectory);
      if (!f.exists()) {
        f.mkdir();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private String getResolvedParameter(ServletContext context, String name) {
    String value = context.getInitParameter(name);
    if (value != null) {
      return value.replace("@actual_path_context@", getActualPathContext())
          .replace("@application_context@", getApplicationContext());
    } else {
      return value;
    }

  }

  private String getApplicationContext() {
    return strContext;
  }

  private String getActualPathContext() {
    return prefix;
  }

  public void storeIn(ServletContext context) {
    context.setAttribute(CONFIG_ATTRIBUTE, this);
  }

  private String getSystemLanguage() {
    return System.getProperty("user.language") + "_" + System.getProperty("user.country");
  }

  private String trimTrailing(String str, String trim) {
    if (str.endsWith(trim)) {
      return str.substring(0, str.length() - trim.length());
    }
    return str;
  }

  private Long asLong(String str) {
    if (str == null || str.length() == 0) {
      return null;
    }
    try {
      return Long.parseLong(str);
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return null;
    }
  }

  private String extractContext(String _prefix) {
    String path = "/";
    int secondPath = -1;
    int firstPath = _prefix.lastIndexOf(path);
    if (firstPath == -1) {
      path = "\\";
      firstPath = _prefix.lastIndexOf(path);
    }
    if (firstPath != -1) {
      secondPath = _prefix.lastIndexOf(path, firstPath - 1);
      return _prefix.substring(secondPath + 1, firstPath);
    }
    return null;
  }

  private boolean determineIsFullDesignPath() {
    try {
      File testPrefix = new File(strBaseDesignPath);
      if (!testPrefix.exists()) {
        return false;
      } else {
        return true;
      }
    } catch (Exception e) {
      return false;
    }
  }

  public String getPoolFilePath() {
    return prefix + "/" + strBaseConfigPath + "/" + poolFileName;
  }

  public String getBaseDesignPath() {
    return isFullPathBaseDesignPath ? strBaseDesignPath : (prefix + "/" + strBaseDesignPath);
  }

  public String getXmlEngineFileFormatPath() {
    return prefix + "/" + strBaseConfigPath + "/" + strFileFormat;
  }

  public String getOpenbravoPropertiesPath() {
    return stcFileProperties;
  }

  public String getFormatPath() {
    return prefix + "/" + strBaseConfigPath + "/Format.xml";
  }

  public boolean havePeriodicBackgroundTime() {
    return periodicBackgroundTime != null;
  }

  public long getPeriodicBackgroundTime() {
    return havePeriodicBackgroundTime() ? periodicBackgroundTime : 0;
  }

  public boolean haveLogFileAcctServer() {
    return strLogFileAcctServer != null && !strLogFileAcctServer.equals("");
  }

  public String getOBProperty(String skey, String sdefault) {

    return propFileProperties.getProperty(skey, sdefault);
  }

  public String getOBProperty(String skey) {

    return propFileProperties.getProperty(skey);
  }

  public Properties getOBProperties() {
    return propFileProperties;
  }

  public Properties loadOBProperties() {
    Properties obProperties = new Properties();
    try {
      obProperties.load(new FileInputStream(stcFileProperties));
      log4j.info("Properties file: " + stcFileProperties);

      overrideProperties(obProperties, stcFileProperties);
    } catch (IOException e) {
      log4j.error("IO error reading properties", e);
    }
    return obProperties;
  }

  /**
   * <p>
   * Looks for file to override some of the properties in the Openbravo.properties. This is intended
   * to be used in Tomcat cluster environments sharing same context directory to allow to define
   * specific values for some properties in different machines.
   * </p>
   * <p>
   * The file with overridden properties is selected based on the first fulfilled rule of these:
   * <ul>
   * <li>If {@code properties.path} JVM property is defined, it indicates the absolute path to take
   * the file from.
   * <li>If {@code machine.name} JVM property is defined, this value will be used as {@code prefix}
   * to look for a file in {@code config} directory named {@code prefix.Openbravo.properties}.
   * <li>Other case, {@code prefix} is defined by {@code InetAddress.getLocalHost().getHostName()}.
   * The actual name for <code>hostName</code> can be determined by {@code ant host.name}.
   * </ul>
   */
  public static void overrideProperties(Properties obProperties, String path) {
    if (obProperties == null) {
      log4j.warn("Openbravo.properties was not set, not trying to override it");
      return;
    }

    String propFilePath = path.replace("Openbravo.properties", "");

    if (propFilePath == null || propFilePath.isEmpty()) {
      log4j.debug("Could not determine context path");
      return;
    }

    String absPath = System.getProperty("properties.path");
    File propertiesFile = null;

    if (absPath != null && !absPath.isEmpty()) {
      propertiesFile = new File(absPath);
      log4j.info("Looking for override properties file in " + absPath + ". Found: "
          + propertiesFile.exists());
    } else {
      String fileName = getMachineName();

      if (fileName == null || fileName.isEmpty()) {
        log4j.debug("Override fileName env variable is not defined.");
        return;
      }

      fileName += ".Openbravo.properties";
      propertiesFile = new File(propFilePath, fileName);
    }

    if (!propertiesFile.exists()) {
      log4j.debug("No override file can be found at " + propertiesFile.getAbsolutePath());
      return;
    }

    // load override property file
    log4j.info("Loading override properties file from " + propertiesFile.getAbsolutePath());
    Properties overrideProperties = new Properties();
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(propertiesFile.getAbsolutePath());
      overrideProperties.load(fis);

      // read system variable value for each key found
      Enumeration<Object> em = overrideProperties.keys();
      while (em.hasMoreElements()) {
        String obProperty = (String) em.nextElement();
        String overrideValue = overrideProperties.getProperty(obProperty);
        Object object = obProperties.setProperty(obProperty, overrideValue); // replace original

        log4j.info("Overriding property " + obProperty + ": " + object + "->"
            + obProperties.getProperty(obProperty));
      }
    } catch (final Exception e) {
      log4j.error("Error loading override Openbravo.properties from " + propertiesFile, e);
    } finally {
      if (fis != null) {
        try {
          fis.close();
        } catch (IOException e) {
          log4j.error("Error closing input stream for " + propertiesFile);
        }
      }
    }
  }

  public static String getMachineName() {
    String name = System.getProperty("machine.name");
    if (name == null || name.isEmpty()) {
      try {
        name = InetAddress.getLocalHost().getHostName();
        log4j.info("Checking override properties for " + name);
      } catch (UnknownHostException e) {
        log4j.error("Error when getting host name", e);
      }
    }
    return name;
  }

  public String getJavaDateTimeFormat() {
    return getOBProperty("dateTimeFormat.java", ConfigParameters.DEFAULT_JAVA_DATETIME_FORMAT);
  }

  public String getSqlDateTimeFormat() {
    return getOBProperty("dateTimeFormat.sql", ConfigParameters.DEFAULT_SQL_DATETIME_FORMAT);
  }
}
