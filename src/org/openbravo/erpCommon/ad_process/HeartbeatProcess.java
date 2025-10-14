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

/**
 * MIGRATED TO HIBERNATE 6
 * - Replaced org.hibernate.criterion.* with jakarta.persistence.criteria.*
 * - This file was automatically migrated from Criteria API to JPA Criteria API
 * - Review and test thoroughly before committing
 */


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
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

  private static Logger log = LogManager.getLogger();

  private static final String HEARTBEAT_URL = "https://activation.futit.cloud:443/license-server/heartbeat";

  private static final String ENABLING_BEAT = "E";
  private static final String SCHEDULED_BEAT = "S";
  private static final String DISABLING_BEAT = "D";
  private static final String DECLINING_BEAT = "DEC";
  private static final String DEFERRING_BEAT = "DEF";

  private static final String UNKNOWN_BEAT = "U";
  public static final String HB_PROCESS_ID = "1005800000";
  public static final String STATUS_SCHEDULED = "SCH";
  public static final String STATUS_UNSCHEDULED = "UNS";

  private ProcessContext ctx;

  private ConnectionProvider connection;
  private ProcessLogger logger;
  private Channel channel;
  private HeartbeatLog hbLog;

  @Override
  public void execute(ProcessBundle bundle) throws Exception {

    connection = bundle.getConnection();
    logger = bundle.getLogger();
    channel = bundle.getChannel();

    this.ctx = bundle.getContext();

    SystemInfo.loadId(connection);

    String msg = null;
    if (this.channel == Channel.SCHEDULED && !isHeartbeatActive()) {
      msg = Utility.messageBD(connection, "HB_INACTIVE", ctx.getLanguage());
      logger.logln(msg);
      OBDal.getInstance().commitAndClose();
      return;
    }

    if (!HttpsUtils.isInternetAvailable()) {
      msg = Utility.messageBD(connection, "HB_INTERNET_UNAVAILABLE", ctx.getLanguage());
      logger.logln(msg);
      OBDal.getInstance().commitAndClose();
      throw new Exception(msg);
    }

    logger.logln("Hearbeat process starting...");
    String beatType = UNKNOWN_BEAT;
    try {

      if (this.channel == Channel.SCHEDULED) {
        beatType = SCHEDULED_BEAT;
      } else {
        final String active = SystemInfoData.isHeartbeatActive(connection);
        if (active.equals("") || active.equals("N")) {
          String action = bundle.getParams().get("action") == null ? ""
              : ((String) bundle.getParams().get("action"));
          if ("DECLINE".equals(action)) {
            beatType = DECLINING_BEAT;
          } else if ("DEFER".equals(action)) {
            beatType = DEFERRING_BEAT;
          } else {
            beatType = ENABLING_BEAT;
          }
        } else {
          beatType = DISABLING_BEAT;
        }
      }

      String queryStr = createQueryStr(beatType);
      String response = sendInfo(queryStr);
      logSystemInfo(beatType);
      updateHeartbeatStatus(beatType);

      if (!(DEFERRING_BEAT.equals(beatType) || DECLINING_BEAT.equals(beatType))) {
        // Parse response for all standard beats, but not for non-skippable ones
        parseResponse(response);
      }

    } catch (Exception e) {
      handleException(e);
    } finally {
      if ("S".equals(beatType)) {
        // Background scheduled beats require explicit commit
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
  }

  private void handleException(Exception e) throws Exception {
    logger.logln(e.getMessage());
    log.error(e.getMessage(), e);
    throw new Exception(e.getMessage());
  }

  private void updateHeartbeatStatus(String beatType) throws Exception {

    if (this.channel == Channel.SCHEDULED || DEFERRING_BEAT.equals(beatType)) {
      // Don't update status when is a scheduled beat or deferring beat
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
    return (isheartbeatactive != null && !isheartbeatactive.equals("")
        && !isheartbeatactive.equals("N"));
  }

  /**
   * @return the system info as properties
   */
  private Properties getSystemInfo() {
    logger.logln(logger.messageDb("HB_GATHER", ctx.getLanguage()));
    return SystemInfo.getSystemInfo();
  }

  /**
   * Converts properties into a UTF-8 encoded query string.
   * 
   * @return the UTF-8 encoded query string
   */
  private String createQueryStr(String beatType) {
    logger.logln(logger.messageDb("HB_QUERY", ctx.getLanguage()));

    if (!(DECLINING_BEAT.equals(beatType) || DEFERRING_BEAT.equals(beatType))) {
      // Complete beat with all available instance info
      try {
        SystemInfo.load(connection);
      } catch (ServletException e1) {
        log.error("Error reading system info", e1);
      }
    }

    Properties props = null;
    props = getSystemInfo();
    if (props == null) {
      return null;
    }

    Enumeration<?> e = props.propertyNames();
    String sb = "";
    while (e.hasMoreElements()) {
      String elem = (String) e.nextElement();
      String value = props.getProperty(elem);
      try {
        if (value != null) {
          value = URLEncoder.encode(value, "UTF-8");
        } else {
          value = "";
        }
        sb += elem + "=" + value + "&";
      } catch (UnsupportedEncodingException e1) {
        log.error("Error encoding", e1);
      }
    }
    sb += "beatType=" + beatType;

    return sb;
  }

  /**
   * Sends a query string to the heartbeat server. Returns the https response as a string.
   * 
   * @param queryStr
   * @return the result of sending the info
   * @throws IOException
   * @throws GeneralSecurityException
   */
  private String sendInfo(String queryStr) throws GeneralSecurityException, IOException {
    logger.logln(logger.messageDb("HB_SEND", ctx.getLanguage()));
    URL url = null;
    try {
      url = new URL(HEARTBEAT_URL);
    } catch (MalformedURLException e) { // Won't happen
      log.error(e.getMessage(), e);
    }
    log.info("Heartbeat sending: '" + queryStr + "'");
    logger.logln(queryStr);
    return HttpsUtils.sendSecure(url, queryStr);
  }

  private void logSystemInfo(String beatType) {
    logger.logln(logger.messageDb("HB_LOG", ctx.getLanguage()));

    try {
      Properties systemInfo = SystemInfo.getSystemInfo();
      OBContext.setAdminMode();
      hbLog = OBProvider.getInstance().get(HeartbeatLog.class);
      hbLog.setSystemIdentifier(systemInfo.getProperty("systemIdentifier"));
      hbLog.setDatabaseIdentifier(systemInfo.getProperty(SystemInfo.Item.DB_IDENTIFIER.getLabel()));
      hbLog.setMacIdentifier(systemInfo.getProperty(SystemInfo.Item.MAC_IDENTIFIER.getLabel()));
      hbLog.setBeatType(beatType);

      if (!(DECLINING_BEAT.equals(beatType) || DEFERRING_BEAT.equals(beatType))) {
        hbLog.setServletContainer(systemInfo.getProperty("servletContainer"));
        hbLog.setServletContainerVersion(systemInfo.getProperty("servletContainerVersion"));
        hbLog.setAntVersion(systemInfo.getProperty("antVersion"));
        hbLog.setOpenbravoVersion(systemInfo.getProperty("obVersion"));
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
            "true".equals(systemInfo.getProperty(SystemInfo.Item.USAGE_AUDIT.getLabel())));
        hbLog.setInstancePurpose(systemInfo.getProperty("instancePurpose"));
        if (systemInfo.getProperty("proxyPort") != null
            && !systemInfo.getProperty("proxyPort").isEmpty()) {
          try {
            hbLog.setProxyPort(Long.parseLong(systemInfo.getProperty("proxyPort")));
          } catch (NumberFormatException e) {
            log.warn("Incorrect port: " + systemInfo.getProperty("proxyPort"));
          }
        }
        try {
          hbLog.setNumberOfRegisteredUsers(
              Long.parseLong(systemInfo.getProperty("numRegisteredUsers")));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of registered users: "
              + systemInfo.getProperty("numRegisteredUsers"));
        }
        hbLog.setInstalledModules(systemInfo.getProperty(SystemInfo.Item.MODULES.getLabel()));
        hbLog.setActivationKeyIdentifier(
            systemInfo.getProperty(SystemInfo.Item.OBPS_INSTANCE.getLabel()));
        if (ActivationKey.getInstance().isOPSInstance()) {
          try {
            hbLog.setInstanceNumber(
                Long.parseLong(systemInfo.getProperty(SystemInfo.Item.INSTANCE_NUMBER.getLabel())));
          } catch (NumberFormatException e) {
            log.warn("Incorrect instance number: "
                + systemInfo.getProperty(SystemInfo.Item.INSTANCE_NUMBER.getLabel()));
          }
        }

        try {
          hbLog.setFirstLogin(
              SystemInfo.parseDate(systemInfo.getProperty(SystemInfo.Item.FIRST_LOGIN.getLabel())));
        } catch (ParseException e) {
          log.warn("Incorrect date of first login: "
              + systemInfo.getProperty(SystemInfo.Item.FIRST_LOGIN.getLabel()));
        }
        try {
          hbLog.setLastLogin(
              SystemInfo.parseDate(systemInfo.getProperty(SystemInfo.Item.LAST_LOGIN.getLabel())));
        } catch (ParseException e) {
          log.warn("Incorrect date of last login: "
              + systemInfo.getProperty(SystemInfo.Item.LAST_LOGIN.getLabel()));
        }
        try {
          hbLog.setTotalLogins(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.TOTAL_LOGINS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of total logins: "
              + systemInfo.getProperty(SystemInfo.Item.TOTAL_LOGINS.getLabel()));
        }
        try {
          hbLog.setTotalLoginsLastMonth(Long.parseLong(
              systemInfo.getProperty(SystemInfo.Item.TOTAL_LOGINS_LAST_MOTH.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of total logins of last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.TOTAL_LOGINS_LAST_MOTH.getLabel()));
        }
        try {
          hbLog.setConcurrentUsersAverage(new BigDecimal(
              systemInfo.getProperty(SystemInfo.Item.AVG_CONCURRENT_USERS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect avg users last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.AVG_CONCURRENT_USERS.getLabel()));
        }
        try {
          hbLog.setUsagePercentage(
              new BigDecimal(systemInfo.getProperty(SystemInfo.Item.PERC_TIME_USAGE.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect usage percentage last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.PERC_TIME_USAGE.getLabel()));
        }
        try {
          hbLog.setMaximumConcurrentUsers(Long
              .parseLong(systemInfo.getProperty(SystemInfo.Item.MAX_CONCURRENT_USERS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect maximum number of concurrent users during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.MAX_CONCURRENT_USERS.getLabel()));
        }

        try {
          hbLog.setWSCallsMaximum(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.WS_CALLS_MAX.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect max number of ws calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WS_CALLS_MAX.getLabel()));
        }
        try {
          hbLog.setWSCallsAverage(
              new BigDecimal(systemInfo.getProperty(SystemInfo.Item.WS_CALLS_AVG.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect avg number of ws calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WS_CALLS_AVG.getLabel()));
        }

        try {
          hbLog.setConnectorCallsMax(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.WSC_CALLS_MAX.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect max number of wsc calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WSC_CALLS_MAX.getLabel()));
        }
        try {
          hbLog.setConnectorCallsAverage(
              new BigDecimal(systemInfo.getProperty(SystemInfo.Item.WSC_CALLS_AVG.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect avg number of wsc calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WSC_CALLS_AVG.getLabel()));
        }

        try {
          hbLog.setWSRejectedMaximum(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.WSR_CALLS_MAX.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect max number of ws rejected calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WSR_CALLS_MAX.getLabel()));
        }
        try {
          hbLog.setWSRejectedAverage(
              new BigDecimal(systemInfo.getProperty(SystemInfo.Item.WSR_CALLS_AVG.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect avg number of ws rejected calls during last 30 days: "
              + systemInfo.getProperty(SystemInfo.Item.WSR_CALLS_AVG.getLabel()));
        }

        try {
          hbLog.setNumberOfClients(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.NUMBER_OF_CLIENTS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of clients: "
              + systemInfo.getProperty(SystemInfo.Item.NUMBER_OF_CLIENTS.getLabel()));
        }
        try {
          hbLog.setNumberOfOrganizations(
              Long.parseLong(systemInfo.getProperty(SystemInfo.Item.NUMBER_OF_ORGS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of orgs: "
              + systemInfo.getProperty(SystemInfo.Item.NUMBER_OF_ORGS.getLabel()));
        }
        try {
          hbLog.setRejectedLoginsDueConcUsers(Long.parseLong(
              systemInfo.getProperty(SystemInfo.Item.REJECTED_LOGINS_DUE_CONC_USERS.getLabel())));
        } catch (NumberFormatException e) {
          log.warn("Incorrect number of rejected logins: "
              + SystemInfo.Item.REJECTED_LOGINS_DUE_CONC_USERS.getLabel());
        }
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
      if ((lastBeat != null && lastBeat.getDatabaseIdentifier() != null
          && lastBeat.getMacIdentifier() != null)
          && (!lastBeat.getSystemIdentifier().equals(SystemInfo.getSystemIdentifier())
              || !lastBeat.getDatabaseIdentifier().equals(SystemInfo.getDBIdentifier())
              || !lastBeat.getMacIdentifier().equals(SystemInfo.getMacAddress()))) {
        return true;
      } else {
        return false;
      }
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

  /**
   * Check if the Heartbeat popup must be displayed.
   * 
   * @return {@code true} if the Heartbeat popup must be displayed, {@code false} otherwise.
   */
  public static boolean isShowHeartbeatRequired(String javaDateFormat,
      ConnectionProvider connectionProvider) throws ServletException {
    final SystemInfoData[] hbData = SystemInfoData.selectSystemProperties(connectionProvider);
    if (hbData.length > 0) {
      final String isheartbeatactive = hbData[0].isheartbeatactive;
      final String postponeDate = hbData[0].postponeDate;
      if (isheartbeatactive == null || isheartbeatactive.equals("")) {
        if (postponeDate == null || postponeDate.equals("")) {
          return true;
        } else {
          Date date = null;
          try {
            date = new SimpleDateFormat(javaDateFormat).parse(postponeDate);
            if (date.before(new Date())) {
              return true;
            }
          } catch (final ParseException e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    }
    return false;
  }

  public static boolean isHeartbeatEnabled() {
    SystemInformation sys = OBDal.getInstance().get(SystemInformation.class, "0");

    final org.openbravo.model.ad.ui.Process HBProcess = OBDal.getInstance()
        .get(org.openbravo.model.ad.ui.Process.class, HB_PROCESS_ID);

    final OBCriteria<ProcessRequest> prCriteria = OBDal.getInstance()
        .createCriteria(ProcessRequest.class);
    // TODO: Migrar 
 Restrictions.and() a CriteriaBuilder.and() manualmente
    prCriteria.add(Restrictions.and(Restrictions.eq(ProcessRequest.PROPERTY_PROCESS, HBProcess),
        // TODO: Migrar Restrictions.or() a CriteriaBuilder.or() manualmente
        Restrictions.or(
            Restrictions.eq(ProcessRequest.PROPERTY_STATUS,
                org.openbravo.scheduling.Process.SCHEDULED),
            Restrictions.eq(ProcessRequest.PROPERTY_STATUS,
                org.openbravo.scheduling.Process.MISFIRED))));
    final List<ProcessRequest> prRequestList = prCriteria.list();

    if (prRequestList.size() == 0) { // Resetting state to disabled
      sys.setEnableHeartbeat(false);
      OBDal.getInstance().save(sys);
      OBDal.getInstance().flush();
    }

    // Must exist a scheduled process request for HB and must be enable at SystemInfo level
    return prRequestList.size() > 0 && sys.isEnableHeartbeat() != null && sys.isEnableHeartbeat();
  }

}
