/*
 ************************************************************************************
 * Copyright (C) 2001-2018 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.VariablesBase;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.OBScheduler;
import org.quartz.SchedulerException;

/**
 * This class is used to provide the coder with friendly methods to retrieve certain environment,
 * session and servlet call variables.
 * 
 * @author Openbravo
 * 
 */
public class VariablesSecureApp extends VariablesBase {
  private static final Logger log4j = LogManager.getLogger();
  private String user;
  private String role;
  private String language;
  private String theme;
  private String client;
  private String organization;
  private String warehouse;
  private String command;
  private String userClient;
  private String dbSessionID;
  private String javaDateFormat;
  private String javaDataTimeFormat;
  private String jsDateFormat;
  private String sqlDateFormat;
  private String accessLevel;

  /**
   * Constructor used to make an empty/manual instance of this class.
   * 
   * @param strUser
   *          ID of the user as specified by the AD_USER_ID column within the AD_USER database
   *          table.
   * @param strClient
   *          ID of the client as specified by the AD_CLIENT_ID column within the AD_CLIENT database
   *          table.
   * @param strOrganization
   *          ID of the organization as specified by the AD_ORG_ID column within the AD_ORG database
   *          table.
   */
  public VariablesSecureApp(String strUser, String strClient, String strOrganization) {
    this.user = strUser;
    this.role = "";
    this.language = System.getProperty("user.language") + "_" + System.getProperty("user.country");
    this.theme = "";
    this.client = strClient;
    this.organization = strOrganization;
    this.userClient = "";
    this.warehouse = "";
    this.dbSessionID = "";
    this.command = "DEFAULT";
    this.javaDateFormat = "";
    this.jsDateFormat = "";
    this.sqlDateFormat = "";
    this.accessLevel = "";
  }

  /**
   * Constructor used to make an empty/manual instance of this class.
   * 
   * @param strUser
   *          ID of the user as specified by the AD_USER_ID column within the AD_USER database
   *          table.
   * @param strClient
   *          ID of the client as specified by the AD_CLIENT_ID column within the AD_CLIENT database
   *          table.
   * @param strOrganization
   *          ID of the organization as specified by the AD_ORG_ID column within the AD_ORG database
   *          table.
   * @param strRole
   *          ID of the role as specified by the AD_ROLE_ID column within the AD_ROLE database
   *          table.
   */
  public VariablesSecureApp(String strUser, String strClient, String strOrganization,
      String strRole) {
    this(strUser, strClient, strOrganization, strRole, null);
  }

  /**
   * Constructor used to make an empty/manual instance of this class.
   * 
   * @param strUser
   *          ID of the user as specified by the AD_USER_ID column within the AD_USER database
   *          table.
   * @param strClient
   *          ID of the client as specified by the AD_CLIENT_ID column within the AD_CLIENT database
   *          table.
   * @param strOrganization
   *          ID of the organization as specified by the AD_ORG_ID column within the AD_ORG database
   *          table.
   * @param strLanguage
   *          Language
   */
  public VariablesSecureApp(String strUser, String strClient, String strOrganization,
      String strRole, String strLanguage) {
    this.user = strUser;
    this.role = strRole;
    if (StringUtils.isEmpty(strLanguage)) {
      this.language = "en_US";
    } else {
      this.language = strLanguage;
    }
    this.theme = "";
    this.client = strClient;
    this.organization = strOrganization;
    this.userClient = "";
    this.warehouse = "";
    this.dbSessionID = "";
    this.command = "DEFAULT";
    this.javaDateFormat = "";
    this.jsDateFormat = "";
    this.sqlDateFormat = "";
    this.accessLevel = "";
  }

  /**
   * Constructor that parses the session variables to initialize the class' internal variables.
   * 
   * @param request
   *          HttpServletRequest object originating from the user request.
   */
  public VariablesSecureApp(HttpServletRequest request) {
    super(request);
    setValues();
  }

  /**
   * Constructor that parses the session variables to initialize the class' internal variables for
   * multipart requests.
   * 
   * @param request
   *          HttpServletRequest object originating from the user request.
   */
  public VariablesSecureApp(HttpServletRequest request, boolean f) {
    super(request, f);
    setValues();
  }

  /**
   * Internal method used to parse the session variables and store them into private variables of
   * this class. These variables can them be retrieved through various get methods (e.g. getUser(),
   * getRole(), etc.).
   */
  private void setValues() {
    this.user = getSessionValue("#AD_User_ID");
    this.role = getSessionValue("#AD_Role_ID");
    this.language = getSessionValue("#AD_Language");
    this.theme = getSessionValue("#Theme");
    this.client = getSessionValue("#AD_Client_ID");
    this.organization = getSessionValue("#AD_Org_ID");
    this.userClient = getSessionValue("#User_Client");
    this.warehouse = getSessionValue("#M_Warehouse_ID");
    this.dbSessionID = getSessionValue("#AD_Session_ID");
    this.command = getStringParameter("Command", "DEFAULT");
    this.javaDateFormat = getSessionValue("#AD_JavaDateFormat");
    this.javaDataTimeFormat = getSessionValue("#AD_JavaDateTimeFormat");
    this.jsDateFormat = getSessionValue("#AD_JsDateFormat");
    this.sqlDateFormat = getSessionValue("#AD_SqlDateFormat");
    this.accessLevel = getSessionValue("#CurrentAccessLevel");
  }

  /**
   * Returns the primary key (AD_USER_ID) of the authenticated user deriving from the AD_USER table.
   * 
   * @return AD_USER_ID primary key number formatted as string
   */
  public String getUser() {
    return user;
  }

  /**
   * Returns the primary key (AD_ROLE_ID) of the role of the authenticated user deriving as entered
   * in the AD_ROLE table.
   * 
   * @return AD_ROLE_ID primary key number formatted as string
   */
  public String getRole() {
    return role;
  }

  /**
   * Returns the code of the language currently selected by the authenticated user according to the
   * RFC 4646 format LANG_REGION, e.g. es_ES for Spanish language from Spain or en_GB for English
   * language from Great Britain.
   * 
   * @return The language code formatted as a string according to RFC 4646.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Returns the unique name of the theme currently selected for the session. This usually
   * corresponds to the theme's folder name in the web/skins. Default theme's value is 'Default'.
   * 
   * @return String with the unique name of the theme.
   */
  public String getTheme() {
    if (!theme.equals("")) {
      return theme;
    } else {
      String strTheme = "";
      try {
        if (OBScheduler.getInstance().getScheduler() != null
            && OBScheduler.getInstance().getScheduler().isStarted()) {
          Client systemClient = OBDal.getInstance().get(Client.class, "0");

          // Get theme (skin)
          OBContext.setAdminMode();
          try {
            org.openbravo.model.ad.system.System sys = OBDal.getInstance()
                .get(org.openbravo.model.ad.system.System.class, "0");
            if (sys != null && !sys.getTADTheme().isEmpty()) {
              strTheme = (systemClient.getLanguage().isRTLLanguage() ? "rtl/" : "ltr/")
                  + sys.getTADTheme();
            }
          } catch (Exception e) {
            log4j.error("Error getting theme", e);
            // set default theme and ignore exception
            strTheme = "";
          } finally {
            OBContext.restorePreviousMode();
          }
        }
      } catch (SchedulerException e) {
        log4j.error("Error while checking if the scheduler is started.", e);
      } finally {
        if (strTheme.isEmpty()) {
          strTheme = "ltr/org.openbravo.userinterface.skin.250to300Comp/250to300Comp";
        }
      }
      return strTheme;
    }
  }

  /**
   * Returns the ID of the client (AD_CLIENT_ID) as defined by the role of the user's current
   * session.
   * 
   * @return String with the AD_CLIENT_ID primary key value.
   */
  public String getClient() {
    return client;
  }

  /**
   * Returns the ID of the organization (AD_ORG_ID) selected by the user among the ones available
   * within the role of the current session.
   * 
   * @return String with the AD_ORG_ID primary key value.
   */
  public String getOrg() {
    return organization;
  }

  public String getUserClient() {
    return userClient;
  }

  public String getUserOrg() {
    return getSessionValue("#AccessibleOrgTree");
  }

  /**
   * Returns the ID of the current default warehouse that will be used with transactions that
   * require a warehouse selected. This is selected using the Role change window and the user can
   * only select warehouses he or she has access to.
   * 
   * @return String with the M_WAREHOUSE primary key value.
   */
  public String getWarehouse() {
    return warehouse;
  }

  /**
   * Returns the ID of the session stored within the AD_SESSION database table.
   * 
   * @return String with the AD_SESSION primary key value.
   */
  public String getDBSession() {
    return dbSessionID;
  }

  /**
   * Returns the command that was passed to the servlet through the Command parameter of the HTTP
   * POST/GET. Normally used by the java controllers so that one controller can support various
   * actions/functions.
   * 
   * @return String containing the value of the Command parameter.
   */
  public String getCommand() {
    return command;
  }

  public String getAccessLevel() {
    return accessLevel;
  }

  /**
   * Returns true if the Command parameter of the HTTP POST/GET request to the servlet is one of the
   * specified values, false if not.
   * 
   * @param commands
   *          The list of strings to compare Command parameter to.
   * @return {@code true} in case current command is any of the ones passed as parameter
   * @see #getCommand()
   */
  public boolean commandIn(String... commands) {
    return command != null && Arrays.asList(commands).contains(command);
  }

  /**
   * Returns the date format used in Java formatting as defined by the dateFormat.java variable
   * within the config/Openbravo.properties configuration file.
   * 
   * @return Formatting string, for example 'dd-MM-yyyy'.
   */
  public String getJavaDateFormat() {
    return javaDateFormat;
  }

  /**
   * @return the javaDataTimeFormat
   */
  public String getJavaDataTimeFormat() {
    return javaDataTimeFormat;
  }

  /**
   * Returns the date format used in Javascript formatting as defined by the dateFormat.java
   * variable within the config/Openbravo.properties configuration file.
   * 
   * @return Formatting string, for example '%d-%m-%Y'.
   */
  public String getJsDateFormat() {
    return jsDateFormat;
  }

  /**
   * Returns the date format used in SQL formatting as defined by the dateFormat.sql variable within
   * the config/Openbravo.properties configuration file.
   * 
   * @return Formatting string, for example 'DD-MM-YYYY'.
   */
  public String getSqlDateFormat() {
    return sqlDateFormat;
  }

  /**
   * Returns a deserialized OBError object retrieved from the session data that might contain the
   * error information for the specified tab. This error would normally be generated by the
   * controller servlet of that tab.
   * 
   * @param AD_Tab_ID
   *          String with the primary key (ID) of the tab as entered within the AD_TAB database
   *          table.
   * @return Deserialized OBError object retrieved from the session. Null if no error message exists
   *         for this tab.
   * @see #setMessage(String, OBError)
   * @see #removeMessage(String)
   */
  public OBError getMessage(String AD_Tab_ID) {
    return ((OBError) getSessionObject(AD_Tab_ID + "|message"));
  }

  /**
   * Serializes and saves the error object to a session variable, specific to the tab which ID is
   * being passed.
   * 
   * @param AD_Tab_ID
   *          String with the primary key (ID) of the tab as entered within the AD_TAB database
   *          table.
   * @param error
   *          The OBError object that needs to be set.
   * @see #setMessage(String, OBError)
   * @see #removeMessage(String)
   */
  public void setMessage(String AD_Tab_ID, OBError error) {
    setSessionObject(AD_Tab_ID + "|message", error);
  }

  /**
   * Removes the error object for the specified tab from the session data. This needs to be done in
   * order for the message not to appear every time the tab is reloaded.
   * 
   * @param AD_Tab_ID
   *          String with the primary key (ID) of the tab as entered within the AD_TAB database
   *          table.
   * @see #setMessage(String, OBError)
   * @see #getMessage(String)
   */
  public void removeMessage(String AD_Tab_ID) {
    removeSessionValue(AD_Tab_ID + "|message");
  }

  public FieldProvider getEditionData(String AD_Tab_ID) {
    return ((FieldProvider) getSessionObject(AD_Tab_ID + "|editionData"));
  }

  public void setEditionData(String AD_Tab_ID, FieldProvider data) {
    setSessionObject(AD_Tab_ID + "|editionData", data);
  }

  public void removeEditionData(String AD_Tab_ID) {
    removeSessionValue(AD_Tab_ID + "|editionData");
  }
}
