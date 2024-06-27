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

package org.openbravo.erpCommon.ad_process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.Alert;
import org.openbravo.erpCommon.utility.HttpsUtils;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.HeartbeatLog;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;

public class HeartbeatProcess implements Process {

  private static final Logger log = LogManager.getLogger();

  private static final String HEARTBEAT_URL = "https://activation.futit.cloud:443/license-instance/heartbeat";

  private static final String ENABLING_BEAT = "E";
  private static final String SCHEDULED_BEAT = "S";
  private static final String DISABLING_BEAT = "D";
  private static final String DECLINING_BEAT = "DEC";
  private static final String DEFERRING_BEAT = "DEF";

  public static final String HB_PROCESS_ID = "1005800000";

  private ProcessContext ctx;

  private ConnectionProvider connection;
  private ProcessLogger logger;
  private Channel channel;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {
    initializeResources(bundle);
    if (!checkHeartbeatAndInternetAvailability()) {
      return;
    }

    logger.logln("Hearbeat process starting...");
    String beatType = determineBeatType(channel, bundle, connection);

    try {
      processHeartbeat(beatType);
    } catch (Exception e) {
      handleException(e);
    } finally {
      finalizeProcess(beatType);
    }
  }

  /**
   * Initializes necessary resources from the provided bundle, including database connection,
   * logger, and execution context.
   *
   * @param bundle
   *     The process bundle containing all necessary context and parameters.
   */
  private void initializeResources(ProcessBundle bundle) throws ServletException {
    connection = bundle.getConnection();
    logger = bundle.getLogger();
    channel = bundle.getChannel();
    this.ctx = bundle.getContext();
    SystemInfo.loadId(connection);
  }

  /**
   * Checks the heartbeat activity status and internet availability before proceeding with the heartbeat process.
   * If the heartbeat is inactive or the internet is not available, logs the appropriate message and terminates execution.
   *
   * @return true if the heartbeat is active and the internet is available, false otherwise.
   * @throws Exception
   *     if internet is not available.
   */
  private boolean checkHeartbeatAndInternetAvailability() {
    if (this.channel == Channel.SCHEDULED && !isHeartbeatActive()) {
      String msg = Utility.messageBD(connection, "HB_INACTIVE", ctx.getLanguage());
      logger.logln(msg);
      OBDal.getInstance().commitAndClose();
      return false;
    }

    if (!HttpsUtils.isInternetAvailable()) {
      String msg = Utility.messageBD(connection, "HB_INTERNET_UNAVAILABLE", ctx.getLanguage());
      logger.logln(msg);
      OBDal.getInstance().commitAndClose();
      throw new OBException(msg);
    }
    return true;
  }

  /**
   * Determines the type of heartbeat to process based on the execution channel and possibly user actions.
   * This can return types such as scheduled, enabling, disabling, deferring, or declining beats.
   *
   * @param bundle
   *     The process bundle containing all necessary context and parameters.
   * @return A string representing the determined heartbeat type.
   */
  public String determineBeatType(Channel channel, ProcessBundle bundle,
      ConnectionProvider connection) throws ServletException {
    String beatType;
    if (channel == Channel.SCHEDULED) {
      beatType = SCHEDULED_BEAT;
    } else {
      final String active = SystemInfoData.isHeartbeatActive(connection);
      if (StringUtils.isEmpty(active) || StringUtils.equals("N", active)) {
        String action = Optional.ofNullable(bundle.getParams().get("action")).orElse("").toString();
        switch (action) {
          case "DECLINE":
            beatType = DECLINING_BEAT;
            break;
          case "DEFER":
            beatType = DEFERRING_BEAT;
            break;
          default:
            beatType = ENABLING_BEAT;
        }
      } else {
        beatType = DISABLING_BEAT;
      }
    }
    return beatType;
  }

  /**
   * Processes the heartbeat by loading system information, sending it to the appropriate service,
   * and handling the response based on the beat type.
   *
   * @param beatType
   *     The type of heartbeat being processed.
   * @throws Exception
   *     if an error occurs during heartbeat processing.
   */
  private void processHeartbeat(String beatType) throws ServletException, IOException, JSONException {
    SystemInfo.load(connection);
    String jsonInputString = setInfo().toString();
    String response = sendInfo(jsonInputString);
    logSystemInfo(beatType);
    updateHeartbeatStatus(beatType);
    if (!(StringUtils.equals(DEFERRING_BEAT, beatType) || StringUtils.equals(DECLINING_BEAT, beatType))) {
      parseResponse(response);
    }
  }

  /**
   * Finalizes the process by committing any pending transactions and closing resources,
   * if necessary, based on the type of beat.
   *
   * @param beatType
   *     The type of heartbeat that was processed.
   */
  private void finalizeProcess(String beatType) {
    if (StringUtils.equals(SCHEDULED_BEAT, beatType)) {
      try {
        OBContext.setAdminMode();
        OBDal.getInstance().commitAndClose();
      } catch (Exception e) {
        handleException(e);
      } finally {
        OBContext.restorePreviousMode();
      }
    }
  }

  /**
   * Handles exceptions that occur during the heartbeat process.
   * This method logs and processes exceptions to ensure proper error handling.
   *
   * @param e
   *     The exception that occurred.
   */
  private void handleException(Exception e) {
    logger.logln(e.getMessage());
    log.error(e.getMessage(), e);
    throw new OBException(e.getMessage());
  }

  private void updateHeartbeatStatus(String beatType) throws ServletException {
    if (this.channel == Channel.SCHEDULED || DEFERRING_BEAT.equals(beatType)) {
      return;
    }

    String active = "";

    if (ENABLING_BEAT.equals(beatType)) {
      active = "Y";
    } else {
      active = "N";
    }
    SystemInfoData.updateHeartbeatActive(connection, active);
  }

  /**
   * @return true if heart beat is enabled, false otherwise
   */
  private boolean isHeartbeatActive() {
    String isheartbeatactive = SystemInfo.get(SystemInfo.Item.ISHEARTBEATACTIVE);
    return (StringUtils.isNotBlank(isheartbeatactive) && !StringUtils.equals(isheartbeatactive, "N"));
  }

  /**
   * Sends a request to the butler. Returns the https response as a string.
   *
   * @param request
   * @return the result of sending the info
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private String sendInfo(String request) throws IOException {
    logger.logln(logger.messageDb("HB_SEND", ctx.getLanguage()));
    URL url = null;
    HttpsURLConnection httpsConnection = null;

    try {
      url = new URL(HEARTBEAT_URL);
      httpsConnection = (HttpsURLConnection) url.openConnection();
    } catch (MalformedURLException e) {
      log.error("Malformed URL: " + HEARTBEAT_URL, e);
      throw new IOException("Malformed URL: " + HEARTBEAT_URL, e);
    }

    if (httpsConnection != null) {
      log.debug("Heartbeat sending: ");
      return sendSecure(httpsConnection, request);
    } else {
      log.error("Failed to open HTTPS connection.");
      throw new IOException("Failed to open HTTPS connection.");
    }
  }


  private void logSystemInfo(String beatType) {
    logger.logln(logger.messageDb("HB_LOG", ctx.getLanguage()));

    try {
      Properties systemInfo = SystemInfo.getSystemInfo();
      OBContext.setAdminMode();
      HeartbeatLog hbLog = OBProvider.getInstance().get(HeartbeatLog.class);
      hbLog.setSystemIdentifier(systemInfo.getProperty("systemIdentifier"));
      hbLog.setDatabaseIdentifier(systemInfo.getProperty(SystemInfo.Item.DB_IDENTIFIER.getLabel()));
      hbLog.setMacIdentifier(systemInfo.getProperty(SystemInfo.Item.MAC_IDENTIFIER.getLabel()));
      hbLog.setBeatType(beatType);

      if (!(DECLINING_BEAT.equals(beatType) || DEFERRING_BEAT.equals(beatType))) {
        hbLog.setServletContainer(systemInfo.getProperty("servletContainer"));
        hbLog.setServletContainerVersion(systemInfo.getProperty("servletContainerVersion"));
        hbLog.setOpenbravoVersion(systemInfo.getProperty("etendoVersion"));
        hbLog.setOpenbravoInstallMode(systemInfo.getProperty("obInstallMode"));
        hbLog.setWebServer(systemInfo.getProperty("webserver"));
        hbLog.setWebServerVersion(systemInfo.getProperty("webserverVersion"));
        hbLog.setOperatingSystem(systemInfo.getProperty("os"));
        hbLog.setOperatingSystemVersion(systemInfo.getProperty("osVersion"));
        hbLog.setDatabase(systemInfo.getProperty("db"));
        hbLog.setDatabaseVersion(systemInfo.getProperty("dbVersion"));
        hbLog.setJavaVersion(systemInfo.getProperty("javaVersion"));
        hbLog.setProxyRequired("Y".equals(systemInfo.getProperty("isproxyrequired")));
        hbLog.setProxyServer(systemInfo.getProperty("proxyServer"));
        hbLog.setUsageAuditEnabled(
            StringUtils.equals("true", systemInfo.getProperty(SystemInfo.Item.USAGE_AUDIT.getLabel())));
        hbLog.setInstancePurpose(systemInfo.getProperty("instancePurpose"));
        if (StringUtils.isNotBlank(systemInfo.getProperty("proxyPort"))) {
          hbLog.setProxyPort(parseLongSafely(systemInfo, SystemInfo.Item.PROXY_PORT));
        }
        hbLog.setNumberOfRegisteredUsers(parseLongSafely(systemInfo, SystemInfo.Item.NUM_REGISTERED_USERS));
        hbLog.setInstalledModules(systemInfo.getProperty(SystemInfo.Item.MODULES.getLabel()));
        hbLog.setActivationKeyIdentifier(systemInfo.getProperty(SystemInfo.Item.OBPS_INSTANCE.getLabel()));
        if (ActivationKey.getInstance().isOPSInstance()) {
          hbLog.setInstanceNumber(parseLongSafely(systemInfo, SystemInfo.Item.INSTANCE_NUMBER));
        }
        hbLog.setFirstLogin(parseDateSafely(systemInfo, SystemInfo.Item.FIRST_LOGIN));
        hbLog.setLastLogin(parseDateSafely(systemInfo, SystemInfo.Item.LAST_LOGIN));
        hbLog.setTotalLogins(parseLongSafely(systemInfo, SystemInfo.Item.TOTAL_LOGINS));
        hbLog.setTotalLoginsLastMonth(parseLongSafely(systemInfo, SystemInfo.Item.TOTAL_LOGINS_LAST_MOTH));
        hbLog.setConcurrentUsersAverage(parseBigDecimalSafely(systemInfo, SystemInfo.Item.AVG_CONCURRENT_USERS));
        hbLog.setUsagePercentage(parseBigDecimalSafely(systemInfo, SystemInfo.Item.PERC_TIME_USAGE));
        hbLog.setMaximumConcurrentUsers(parseLongSafely(systemInfo, SystemInfo.Item.MAX_CONCURRENT_USERS));
        hbLog.setWSCallsMaximum(parseLongSafely(systemInfo, SystemInfo.Item.WS_CALLS_MAX));
        hbLog.setWSCallsAverage(parseBigDecimalSafely(systemInfo, SystemInfo.Item.WS_CALLS_AVG));
        hbLog.setConnectorCallsMax(parseLongSafely(systemInfo, SystemInfo.Item.WSC_CALLS_MAX));
        hbLog.setConnectorCallsAverage(parseBigDecimalSafely(systemInfo, SystemInfo.Item.WSC_CALLS_AVG));
        hbLog.setWSRejectedMaximum(parseLongSafely(systemInfo, SystemInfo.Item.WSR_CALLS_MAX));
        hbLog.setWSRejectedAverage(parseBigDecimalSafely(systemInfo, SystemInfo.Item.WSR_CALLS_AVG));
        hbLog.setNumberOfClients(parseLongSafely(systemInfo, SystemInfo.Item.NUMBER_OF_CLIENTS));
        hbLog.setNumberOfOrganizations(parseLongSafely(systemInfo, SystemInfo.Item.NUMBER_OF_ORGS));
        hbLog.setRejectedLoginsDueConcUsers(
            parseLongSafely(systemInfo, SystemInfo.Item.REJECTED_LOGINS_DUE_CONC_USERS));
      }
      OBDal.getInstance().save(hbLog);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * @param response
   */
  private void parseResponse(String response) {
    logger.logln(logger.messageDb("HB_UPDATES", ctx.getLanguage()));
    if (response == null) {
      return;
    }

    OBContext.setAdminMode();
    try {
      JSONObject json = new JSONObject(response);
      String alertsResponse = (String) json.get("alerts");
      parseAlerts(alertsResponse);
    } catch (JSONException e) {
      log.error(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void parseAlerts(String alertsResponse) {
    String[] updates = alertsResponse.split("::");
    List<Alert> alerts = new ArrayList<Alert>();
    Pattern pattern = Pattern.compile("\\[recordId=\\d+\\]");
    for (String update : updates) {
      String recordId = null;
      String description = update;
      Matcher matcher = pattern.matcher(update);
      if (matcher.find()) {
        String s = matcher.group();
        recordId = s.substring(s.indexOf('=') + 1, s.indexOf(']'));
        description = update.substring(update.indexOf(']') + 1);
      }
      Alert alert = new Alert(1005400000, recordId);
      alert.setDescription(description);
      alerts.add(alert);
    }
    saveUpdateAlerts(alerts);

  }

  /**
   * @param updates
   */
  private void saveUpdateAlerts(List<Alert> updates) {
    if (updates == null) {
      logger.logln("No Updates found...");
      return;
    }
    for (Alert update : updates) {
      update.save(connection);
    }
  }

  public enum HeartBeatOrRegistration {
    HeartBeat, None, InstancePurpose;
  }

  /**
   * @see HeartbeatProcess#isLoginPopupRequired(String, String, ConnectionProvider)
   */
  public static HeartBeatOrRegistration isLoginPopupRequired(VariablesSecureApp vars,
      ConnectionProvider connectionProvider) throws ServletException {
    return isLoginPopupRequired(vars.getRole(), vars.getJavaDateFormat(), connectionProvider);
  }

  /**
   * Check if a popup is needed to be shown when a user logins.
   *
   * @return the type of popup that is needed.
   */
  public static HeartBeatOrRegistration isLoginPopupRequired(String roleId, String javaDateFormat,
      ConnectionProvider connectionProvider) throws ServletException {
    if (roleId != null && "0".equals(roleId)) {
      // Check if the instance purpose is set.
      if (isShowInstancePurposeRequired()) {
        return HeartBeatOrRegistration.InstancePurpose;
      }
      if (isClonedInstance()) {
        return HeartBeatOrRegistration.InstancePurpose;
      }
      if (isShowHeartbeatRequired(javaDateFormat, connectionProvider)) {
        return HeartBeatOrRegistration.HeartBeat;
      }
    }
    return HeartBeatOrRegistration.None;
  }

  public static boolean isShowInstancePurposeRequired() {
    final SystemInformation systemInformation = OBDal.getInstance()
        .get(SystemInformation.class, "0");
    if (systemInformation.getInstancePurpose() == null
        || systemInformation.getInstancePurpose().isEmpty()) {
      if (ActivationKey.isActiveInstance()) {
        systemInformation.setInstancePurpose(ActivationKey.getInstance().getProperty("purpose"));
        OBDal.getInstance().save(systemInformation);
        OBDal.getInstance().flush();
        try {
          OBDal.getInstance().getConnection().commit();
        } catch (SQLException e) {
          // ignore exception on commit
          log.error("Error on commit", e);
        }
        return false;
      }
      return true;
    }
    return false;
  }

  public static boolean isClonedInstance() throws ServletException {
    OBContext.setAdminMode();
    try {
      HeartbeatLog lastBeat = getLastHBLog();
      return (lastBeat != null && lastBeat.getDatabaseIdentifier() != null
          && lastBeat.getMacIdentifier() != null)
          && (!lastBeat.getSystemIdentifier().equals(SystemInfo.getSystemIdentifier())
          || !lastBeat.getDatabaseIdentifier().equals(SystemInfo.getDBIdentifier())
          || !lastBeat.getMacIdentifier().equals(SystemInfo.getMacAddress()));
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static HeartbeatLog getLastHBLog() {
    OBCriteria<HeartbeatLog> obc = OBDal.getInstance().createCriteria(HeartbeatLog.class);
    obc.addOrderBy(HeartbeatLog.PROPERTY_CREATIONDATE, false);
    obc.setMaxResults(1);
    List<HeartbeatLog> hbLogs = obc.list();
    if (hbLogs.isEmpty()) {
      return null;
    }
    return hbLogs.get(0);
  }

  /**
   * @see HeartbeatProcess#isShowHeartbeatRequired(String, ConnectionProvider)
   */
  public static boolean isShowHeartbeatRequired(VariablesSecureApp vars,
      ConnectionProvider connectionProvider) throws ServletException {
    return isShowHeartbeatRequired(vars.getJavaDateFormat(), connectionProvider);
  }

  public static boolean isShowHeartbeatRequired(String javaDateFormat,
      ConnectionProvider connectionProvider) throws ServletException {
    final SystemInfoData[] hbData = SystemInfoData.selectSystemProperties(connectionProvider);
    if (hbData.length == 0) {
      return false;
    }
    if (isHeartbeatInactive(hbData[0])) {
      return isPostponeDatePast(javaDateFormat, hbData[0].postponeDate);
    }
    return false;
  }

  private static boolean isHeartbeatInactive(SystemInfoData data) {
    return StringUtils.isBlank(data.isheartbeatactive);
  }

  private static boolean isPostponeDatePast(String javaDateFormat, String postponeDate) {
    if (StringUtils.isBlank(postponeDate)) {
      return true;
    }
    try {
      Date date = new SimpleDateFormat(javaDateFormat).parse(postponeDate);
      return date.before(new Date());
    } catch (ParseException e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }

  public static boolean isHeartbeatEnabled() {
    SystemInformation sys = OBDal.getInstance().get(SystemInformation.class, "0");

    final org.openbravo.model.ad.ui.Process HBProcess = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, HB_PROCESS_ID);

    final OBCriteria<ProcessRequest> prCriteria = OBDal.getInstance()
        .createCriteria(ProcessRequest.class);
    prCriteria.add(Restrictions.and(Restrictions.eq(ProcessRequest.PROPERTY_PROCESS, HBProcess),
        Restrictions.or(Restrictions.eq(ProcessRequest.PROPERTY_STATUS, Process.SCHEDULED),
            Restrictions.eq(ProcessRequest.PROPERTY_STATUS, Process.MISFIRED))));
    final List<ProcessRequest> prRequestList = prCriteria.list();

    if (prRequestList.isEmpty()) { // Resetting state to disabled
      sys.setEnableHeartbeat(false);
      OBDal.getInstance().save(sys);
      OBDal.getInstance().flush();
    }

    // Must exist a scheduled process request for HB and must be enable at SystemInfo level
    return !prRequestList.isEmpty() && sys.isEnableHeartbeat() != null && sys.isEnableHeartbeat();
  }

  private static String sendSecure(HttpsURLConnection conn, String jsonData) throws IOException {
    StringBuilder response = new StringBuilder();

    conn.setRequestMethod("POST");
    conn.setDoOutput(true);
    conn.setRequestProperty("Content-Type", "application/json");

    try (OutputStream os = conn.getOutputStream()) {
      byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
      os.write(input, 0, input.length);
    }

    int responseCode = conn.getResponseCode();

    if (responseCode >= 400) {
      InputStream errorStream = conn.getErrorStream();
      if (errorStream != null) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
          String responseLine;
          while ((responseLine = br.readLine()) != null) {
            response.append(StringUtils.trim(responseLine));
          }
        }
      }
      log.error("HTTP error code: " + responseCode + " Response: " + conn.getResponseMessage());
      throw new OBException(response.toString());
    }

    try (InputStream stream = conn.getInputStream();
         BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String responseLine;
      while ((responseLine = br.readLine()) != null) {
        response.append(StringUtils.trim(responseLine));
      }
    }
    return response.toString();
  }


  /**
   * Compiles system and application information into a JSONArray containing a JSONObject.
   * This object includes details such as system and database identifiers, network information,
   * operating system version, server configurations, Java version, user statistics, among others,
   * gathered through {@code SystemInfo.getSystemInfo()}.
   * <p>
   * Dates and long numbers are processed safely to prevent format errors.
   * Any exceptions caught during the process are logged, and the generated JSONArray is returned,
   * which may be empty or contain partial data if an error occurs.
   *
   * @return JSONArray with system and application information.
   * @throws JSONException
   *     if there is an error creating the JSON objects.
   */
  private static JSONArray setInfo() throws JSONException {
    JSONArray heartbeatArray = new JSONArray();
    JSONObject heartbeatObj = new JSONObject();
    Properties systemInfo = SystemInfo.getSystemInfo();
    String dateFormatStr = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("dateFormat.java");
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatStr);
    try {
      heartbeatObj.put("active", true);
      heartbeatObj.put("systemID", systemInfo.getProperty(SystemInfo.Item.SYSTEM_IDENTIFIER.getLabel()));
      heartbeatObj.put("databaseID", systemInfo.getProperty(SystemInfo.Item.DB_IDENTIFIER.getLabel()));
      heartbeatObj.put("mACID", systemInfo.getProperty(SystemInfo.Item.MAC_IDENTIFIER.getLabel()));
      heartbeatObj.put("purpose", systemInfo.getProperty(SystemInfo.Item.INSTANCE_PURPOSE.getLabel()));
      heartbeatObj.put("operationSystem", systemInfo.getProperty(SystemInfo.Item.OPERATING_SYSTEM.getLabel()));
      heartbeatObj.put("operationSystemVersion",
          systemInfo.getProperty(SystemInfo.Item.OPERATING_SYSTEM_VERSION.getLabel()));
      heartbeatObj.put("dbName", systemInfo.getProperty(SystemInfo.Item.DATABASE.getLabel()));
      heartbeatObj.put("dbVersion", systemInfo.getProperty(SystemInfo.Item.DATABASE_VERSION.getLabel()));
      heartbeatObj.put("servletContainer", systemInfo.getProperty(SystemInfo.Item.SERVLET_CONTAINER.getLabel()));
      heartbeatObj.put("servletContainerVersion",
          systemInfo.getProperty(SystemInfo.Item.SERVLET_CONTAINER_VERSION.getLabel()));
      heartbeatObj.put("webServer", systemInfo.getProperty(SystemInfo.Item.WEBSERVER.getLabel()));
      heartbeatObj.put("webServerVersion", systemInfo.getProperty(SystemInfo.Item.WEBSERVER_VERSION.getLabel()));
      heartbeatObj.put("javaVersion", systemInfo.getProperty(SystemInfo.Item.JAVA_VERSION.getLabel()));
      heartbeatObj.put("etendoVersion", systemInfo.getProperty(SystemInfo.Item.ETENDO_VERSION.getLabel()));
      heartbeatObj.put("rejectedLoginsDueConcUsers",
          systemInfo.getProperty(SystemInfo.Item.REJECTED_LOGINS_DUE_CONC_USERS.getLabel()));
      heartbeatObj.put("modules", systemInfo.getProperty(SystemInfo.Item.MODULES.getLabel()));
      heartbeatObj.put("numRegisteredUsers", systemInfo.getProperty(SystemInfo.Item.NUM_REGISTERED_USERS.getLabel()));
      heartbeatObj.put("firstLogin", dateFormat.format(parseDateSafely(systemInfo, SystemInfo.Item.FIRST_LOGIN)));
      heartbeatObj.put("lastLogin", dateFormat.format(parseDateSafely(systemInfo, SystemInfo.Item.LAST_LOGIN)));
      heartbeatObj.put("totalLogins", parseLongSafely(systemInfo, SystemInfo.Item.TOTAL_LOGINS));
      heartbeatObj.put("maxUsers", parseLongSafely(systemInfo, SystemInfo.Item.MAX_CONCURRENT_USERS));
      heartbeatObj.put("avgUsers", systemInfo.getProperty(SystemInfo.Item.AVG_CONCURRENT_USERS.getLabel()));
      heartbeatObj.put("obpsId", systemInfo.getProperty(SystemInfo.Item.OBPS_INSTANCE.getLabel()));
      heartbeatObj.put("totalLoginsLastMonth",
          systemInfo.getProperty(SystemInfo.Item.TOTAL_LOGINS_LAST_MOTH.getLabel()));
      heartbeatObj.put("status", ActivationKey.getInstance().getSubscriptionStatus().getStatusCode());
      heartbeatArray.put(heartbeatObj);
    } catch (Exception e) {
      log.error(e.getMessage());
    }
    return heartbeatArray;
  }

  private static Date parseDateSafely(Properties systemInfo, SystemInfo.Item item) {
    try {
      return SystemInfo.parseDate(systemInfo.getProperty(item.getLabel()));
    } catch (ParseException e) {
      logFormatError(item.getLabel(), systemInfo.getProperty(item.getLabel()));
      return null;
    }
  }

  private static Long parseLongSafely(Properties systemInfo, SystemInfo.Item item) {
    try {
      return Long.parseLong(systemInfo.getProperty(item.getLabel()));
    } catch (NumberFormatException e) {
      logFormatError(item.getLabel(), systemInfo.getProperty(item.getLabel()));
      return null;
    }
  }

  private static BigDecimal parseBigDecimalSafely(Properties systemInfo, SystemInfo.Item item) {
    try {
      return new BigDecimal(systemInfo.getProperty(item.getLabel()));
    } catch (NumberFormatException e) {
      logFormatError(item.getLabel(), systemInfo.getProperty(item.getLabel()));
      return null;
    }
  }

  private static void logFormatError(String itemLabel, String systemItemLabel) {
    log.warn("Incorrect format for {} : {}", itemLabel, systemItemLabel);
  }
}
