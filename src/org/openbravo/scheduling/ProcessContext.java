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
 * All portions are Copyright (C) 2008-2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.scheduling;

import java.io.Serializable;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;

import java.io.Serializable;

/**
 * @author awolski
 * 
 */
public class ProcessContext implements Serializable {

  static final long serialVersionUID = 1L;

  private static final Logger log = LogManager.getLogger();
  public static final String KEY = "org.openbravo.base.secureApp.ObContext";

  private String user;
  private String role;
  private String language;
  private String theme;
  private String client;
  private String organization;
  private String warehouse;
  private String command;
  private String userClient;
  private String userOrganization;
  private String dbSessionID;
  private String javaDateFormat;
  private String javaDateTimeFormat;
  private String jsDateFormat;
  private String sqlDateFormat;
  private String accessLevel;

  private boolean roleSecurity;

  /**
   * Creates an empty ProcessContext.
   */
  public ProcessContext() {

  }

  /**
   * Creates a ProcessContext using the information of VariablesSecureApp object.
   * 
   * @param vars
   *          a VariablesSecureApp object with the session information used to initialize the
   *          ProcessContext properties.
   */
  public ProcessContext(VariablesSecureApp vars) {
    user = vars.getUser();
    role = vars.getRole();
    language = vars.getLanguage();
    theme = vars.getTheme();
    client = vars.getClient();
    organization = vars.getOrg();
    warehouse = vars.getWarehouse();
    command = vars.getCommand();
    userClient = vars.getUserClient();
    userOrganization = vars.getUserOrg();
    dbSessionID = vars.getDBSession();
    javaDateFormat = vars.getJavaDateFormat();
    javaDateTimeFormat = vars.getJavaDataTimeFormat();
    jsDateFormat = vars.getJsDateFormat();
    sqlDateFormat = vars.getSqlDateFormat();
    accessLevel = vars.getAccessLevel();
    roleSecurity = true;
  }

  private ProcessContext(JSONObject json) throws JSONException {
    user = getStringProperty(json, "user");
    role = getStringProperty(json, "role");
    language = getStringProperty(json, "language");
    theme = getStringProperty(json, "theme");
    client = getStringProperty(json, "client");
    organization = getStringProperty(json, "organization");
    warehouse = getStringProperty(json, "warehouse");
    command = getStringProperty(json, "command");
    userClient = getStringProperty(json, "userClient");
    userOrganization = getStringProperty(json, "userOrganization");
    dbSessionID = getStringProperty(json, "dbSessionID");
    javaDateFormat = getStringProperty(json, "javaDateFormat");
    javaDateTimeFormat = getStringProperty(json, "javaDateTimeFormat");
    jsDateFormat = getStringProperty(json, "jsDateFormat");
    sqlDateFormat = getStringProperty(json, "sqlDateFormat");
    accessLevel = getStringProperty(json, "accessLevel");
    roleSecurity = getBooleanProperty(json, "roleSecurity");
  }

  private String getStringProperty(JSONObject json, String property) throws JSONException {
    return json.has(property) ? json.getString(property) : null;
  }

  private boolean getBooleanProperty(JSONObject json, String property) throws JSONException {
    return json.has(property) ? json.getBoolean(property) : false;
  }

  /**
   * Create a request with the selected client and organization.
   * 
   * @param vars
   * @param client
   * @param org
   */
  public ProcessContext(VariablesSecureApp vars, String client, String org, boolean roleSecurity) {
    this(vars);
    this.client = client;
    this.organization = org;
    this.roleSecurity = roleSecurity;
  }

  /**
   * @return a new instance of {@link VariablesSecureApp} created from the client, organization and
   *         user.
   * @see #getClient()
   * @see #getOrganization()
   * @see #getUser()
   */
  public VariablesSecureApp toVars() {
    return new VariablesSecureApp(user, client, organization, role, language);
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * @return the language
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @return the theme
   */
  public String getTheme() {
    return theme;
  }

  /**
   * @return the client
   */
  public String getClient() {
    return client;
  }

  /**
   * @return the organization
   */
  public String getOrganization() {
    return organization;
  }

  /**
   * @return the warehouse
   */
  public String getWarehouse() {
    return warehouse;
  }

  /**
   * @return the command
   */
  public String getCommand() {
    return command;
  }

  /**
   * @return the userClient
   */
  public String getUserClient() {
    return userClient;
  }

  /**
   * @return the userOrganization
   */
  public String getUserOrganization() {
    return userOrganization;
  }

  /**
   * @return the dbSessionID
   */
  public String getDbSessionID() {
    return dbSessionID;
  }

  /**
   * @return the javaDateFormat
   */
  public String getJavaDateFormat() {
    return javaDateFormat;
  }

  /**
   * @return the javaDateTimeFormat
   */
  public String getJavaDateTimeFormat() {
    return javaDateTimeFormat;
  }

  /**
   * @return the jsDateFormat
   */
  public String getJsDateFormat() {
    return jsDateFormat;
  }

  /**
   * @return the sqlDateFormat
   */
  public String getSqlDateFormat() {
    return sqlDateFormat;
  }

  /**
   * @return the accessLevel
   */
  public String getAccessLevel() {
    return accessLevel;
  }

  /**
   * @return whether the security for the process is based on role.
   */
  public boolean isRoleSecurity() {
    return roleSecurity;
  }

  /**
   * @return a JSON string representation of this ProcessContext
   */
  @Override
  public String toString() {
    return toJSON().toString();
  }

  private JSONObject toJSON() {
    JSONObject jsonObject = new JSONObject();
    try {
      JSONObject properties = new JSONObject();
      properties.put("user", user);
      properties.put("role", role);
      properties.put("language", language);
      properties.put("theme", theme);
      properties.put("client", client);
      properties.put("organization", organization);
      properties.put("warehouse", warehouse);
      properties.put("command", command);
      properties.put("userClient", userClient);
      properties.put("userOrganization", userOrganization);
      properties.put("dbSessionID", dbSessionID);
      properties.put("javaDateFormat", javaDateFormat);
      properties.put("javaDateTimeFormat", javaDateTimeFormat);
      properties.put("jsDateFormat", jsDateFormat);
      properties.put("sqlDateFormat", sqlDateFormat);
      properties.put("accessLevel", accessLevel);
      properties.put("roleSecurity", roleSecurity);
      jsonObject.put(ProcessContext.class.getName(), properties);
    } catch (JSONException ignore) {
    }
    return jsonObject;
  }

  /**
   * @param processContext
   *          a String with the JSON representation of a ProcessContext
   * @return a new instance created from the provided JSON representation of a ProcessContext. This
   *         method returns null if the provided String is null, empty or if it is an invalid JSON
   *         definition of a ProcessContext.
   */
  public static ProcessContext newInstance(String processContext) {
    if (StringUtils.isBlank(processContext)) {
      return null;
    }
    try {
      JSONObject json = new JSONObject(processContext);
      return new ProcessContext(json.getJSONObject(ProcessContext.class.getName()));
    } catch (JSONException ex) {
      log.error("Error creating ProcessContext from String representation: {}", processContext, ex);
      return null;
    }
  }

}
