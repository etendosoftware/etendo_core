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
 * All portions are Copyright (C) 2009-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class is used to maintain session information which will be used for audit purposes.
 * 
 */
public class SessionInfo {
  private static final String JDBC_CONNECTION_POOL_CLASS_NAME = "org.openbravo.apachejdbcconnectionpool.JdbcExternalConnectionPool";

  private static final Logger log4j = LogManager.getLogger();

  public static final String IMPORT_ENTRY_PROCESS = "IE";

  /**
   * updated on context start and via SL_AuditTable. used to switch on/off the audit trail system
   */
  private static boolean isAuditActive = false;
  private static boolean usageAuditActive = false;
  private static boolean initialized = false;

  /*
   * The following variables track per thread the information about the current 'user' of the thread
   * (this info is later at getConnection() time passed into a temporary AD_CONTEXT_INFO table to be
   * available to the generated audit triggers.
   */
  private static ThreadLocal<String> sessionId = new ThreadLocal<String>();
  private static ThreadLocal<String> userId = new ThreadLocal<String>();
  private static ThreadLocal<String> processType = new ThreadLocal<String>();
  private static ThreadLocal<String> processId = new ThreadLocal<String>();
  private static ThreadLocal<String> command = new ThreadLocal<String>();
  private static ThreadLocal<String> queryProfile = new ThreadLocal<String>();

  /*
   * To optimize updating of the AD_CONTEXT_INFO information, getConnection() is changed to return
   * the same connection on all getConnection() calls done inside the same request when possible.
   * Then the ad_context_info does not need to be updated so often (as the data doesn't change so
   * often for a specific connection).
   */
  private static ThreadLocal<Connection> sessionConnection = new ThreadLocal<Connection>();
  private static ThreadLocal<Boolean> changedInfo = new ThreadLocal<Boolean>();

  /*
   * Maintain artifact's module id. This element is not persisted in auxiliary session table, it is
   * intended to be used in the usage audit.
   */
  private static ThreadLocal<String> moduleId = new ThreadLocal<String>();

  private static ThreadLocal<Boolean> auditThisThread = new ThreadLocal<Boolean>();

  /**
   * Sets all session information to null. Called at the end of http-request handling, to reset the
   * audit information for that thread.
   */
  public static void init() {
    sessionId.set(null);
    userId.set(null);
    processType.set(null);
    processId.set(null);
    changedInfo.set(null);
    moduleId.set(null);
    command.set(null);
    queryProfile.set(null);
    auditThisThread.set(true);
    // if there is an open connection associated to get current request, close it
    Connection conn = sessionConnection.get();
    try {
      if (conn != null && !conn.isClosed()) {
        log4j.debug("Close session's connection");
        conn.setAutoCommit(true);
        conn.close();
      }
    } catch (SQLException e) {
      log4j.error("Error closing sessionConnection", e);
    }
    sessionConnection.set(null);
  }

  /**
   * Creates the needed infrastructure for audit. Which is temporary session table for PostgreSQL
   * connections.
   * 
   * Called whenever a new physical db-connection is created.
   * 
   * @param conn
   *          Connection to database
   * @param rdbms
   *          Database, only action is take for POSTGRESQL
   */
  public static void initDB(Connection conn, String rdbms) {
    if (adContextInfoShouldBeCreated(conn, rdbms)) {
      createAdContextInfoTable(conn);
    }
  }

  private static void createAdContextInfoTable(Connection conn) {
    String sql = "CREATE TEMPORARY TABLE AD_CONTEXT_INFO" + //
        "(AD_USER_ID VARCHAR(32), " + //
        "  AD_SESSION_ID VARCHAR(32)," + //
        "  PROCESSTYPE VARCHAR(60), " + //
        "  PROCESSID VARCHAR(32)) on commit preserve rows";

    try (PreparedStatement psCreate = getPreparedStatement(conn, sql)) {
      psCreate.execute();
    } catch (SQLException e) {
      log4j.error("Error initializating audit infrastructure", e);
      throw new IllegalStateException(e);
    }
  }

  private static boolean adContextInfoShouldBeCreated(Connection conn, String rdbms) {
    if (!isAuditActive || !isPosgreSQL(rdbms) || isReadOnly(conn)) {
      return false;
    }
    if (usingJdbcConnectionPool()) {
      // if this pool is used , the initDB method will only be called when creating a new
      // connection, there is need to check if the ad_context_info has already been created
      return true;
    } else {
      return !adContextInfoExists(conn);
    }
  }

  private static boolean isReadOnly(Connection conn) {
    boolean readOnly = false;
    try {
      readOnly = conn.isReadOnly();
    } catch (SQLException e) {
      log4j.error("Error checking if the connection is read only", e);
    }
    return readOnly;
  }

  private static boolean isPosgreSQL(String rdbms) {
    return rdbms != null && rdbms.equals("POSTGRE");
  }

  private static boolean usingJdbcConnectionPool() {
    if (ExternalConnectionPool.getInstance() == null) {
      return false;
    }
    return JDBC_CONNECTION_POOL_CLASS_NAME
        .equals(ExternalConnectionPool.getInstance().getClass().getName());
  }

  private static boolean adContextInfoExists(Connection conn) {
    boolean alreadyExists = false;
    try (PreparedStatement psQuery = getPreparedStatement(conn,
        "select count(*) from information_schema.tables where table_name='ad_context_info' and table_type = 'LOCAL TEMPORARY'");
        ResultSet rs = psQuery.executeQuery()) {
      alreadyExists = rs.next() && !rs.getString(1).equals("0");
    } catch (SQLException e) {
      log4j.error("Error checking if the ad_context_info table exists", e);
    }
    return alreadyExists;
  }

  /**
   * @deprecated In most of the cases, this method is no longer required to be invoked: it was used
   *             to manually set in database audit info. Now, this is in an smarter manner only if
   *             needed from DAL/SQLC. When this method is still in use, it should be reviewed
   *             whether it is no longer needed because of this automatic mechanism or if it is
   *             required because new mechanism doesn't detect it (ie. DB modifications directly
   *             with jdbc), in which case saveContextInfoIntoDB method is recommended to make
   *             explicit in the code this need
   * @see #saveContextInfoIntoDB(Connection)
   */
  @Deprecated
  static void setDBSessionInfo(Connection conn, boolean onlyIfChanged) {
    if (!isAuditActive || (onlyIfChanged && (changedInfo.get() == null || !changedInfo.get()))) {
      if (log4j.isDebugEnabled()) {
        log4j.debug("No session info set isAuditActive: " + isAuditActive + " - changes in info: "
            + changedInfo.get());
      }
      return;
    }
    saveContextInfoIntoDB(conn);
  }

  /**
   * @deprecated In most of the cases this method is no longer required to be invoked
   * @see #saveContextInfoIntoDB(Connection)
   */
  @Deprecated
  public static void setDBSessionInfo(Connection conn) {
    saveContextInfoIntoDB(conn);
  }

  /**
   * Saves currently stored context information into DB. Generally, this method shouldn't be
   * directly invoked, as the Platform already does it when flushing changes to DB. Only in case
   * Openbravo platform is bypassed (ie. DB operations performed on a manually obtained connection),
   * this method must be manually invoked.
   * 
   * @param conn
   *          The connection where the session information will be stored in, note it must be the
   *          same one performing DB modifications so audit trail triggers can retrieve the session
   *          information.
   */
  public static void saveContextInfoIntoDB(Connection conn) {
    if (!isAuditActive) {
      return;
    }

    PreparedStatement psCleanUp = null;
    PreparedStatement psInsert = null;
    try {
      // When working with DAL sessionConnection is not set. This allows to have in the same thread
      // a connection for DAL within its session with autocommit false and another one for sqlc with
      // autocommit true.
      boolean infoModified = Boolean.TRUE.equals(changedInfo.get())
          || sessionConnection.get() == null || !conn.equals(sessionConnection.get());
      if (!infoModified || Boolean.FALSE.equals(auditThisThread.get()) || isReadOnly(conn)) {
        return;
      }

      if (log4j.isDebugEnabled()) {
        log4j.debug(
            "saving DB context info " + SessionInfo.getUserId() + " - " + SessionInfo.getSessionId()
                + " - " + SessionInfo.getProcessType() + " - " + SessionInfo.getProcessId());
      }

      psCleanUp = getPreparedStatement(conn, "delete from ad_context_info");
      psCleanUp.executeUpdate();

      psInsert = getPreparedStatement(conn,
          "insert into ad_context_info (ad_user_id, ad_session_id, processType, processId) values (?, ?, ?, ?)");
      psInsert.setString(1, SessionInfo.getUserId());
      psInsert.setString(2, SessionInfo.getSessionId());
      psInsert.setString(3, SessionInfo.getProcessType());
      psInsert.setString(4, SessionInfo.getProcessId());
      psInsert.executeUpdate();

      if (conn == sessionConnection.get()) {
        // Handling only for the sqlc connection, as DAL should be automatically handled so that
        // this method is invoked only once.
        changedInfo.set(false);
      }
    } catch (Exception e) {
      log4j.error("Error setting audit info", e);
    } finally {
      releasePreparedStatement(psCleanUp);
      releasePreparedStatement(psInsert);
    }
  }

  /**
   * Initialized DB with temporary table and sets session information on it.
   * 
   * @param conn
   *          Connection where the session information will be stored in
   * @param rdbms
   *          Database type
   * @deprecated
   */
  @Deprecated
  public static void setDBSessionInfo(Connection conn, String rdbms) {
    if (!isAuditActive) {
      return;
    }
    initDB(conn, rdbms);
    setDBSessionInfo(conn);
  }

  /**
   * Return the connection associated with the current session, if there is one.
   */
  static Connection getSessionConnection() {
    Connection conn = sessionConnection.get();
    try {
      if (conn == null || conn.isClosed()) {
        return null;
      }
    } catch (SQLException e) {
      log4j.error("Error checking connection", e);
      return null;
    }
    log4j.debug("Reuse session's connection");
    return conn;
  }

  private static PreparedStatement getPreparedStatement(Connection conn, String sql)
      throws SQLException {
    if (conn == null || sql == null || sql.equals("")) {
      return null;
    }
    PreparedStatement ps = null;

    try {
      log4j.trace("preparedStatement requested");
      ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY);
    } catch (SQLException e) {
      log4j.error("getPreparedStatement: " + sql, e);
      try {
        conn.setAutoCommit(true);
        conn.close();
      } catch (Exception ex) {
        log4j.error("Could not close PreparedStatement for " + sql, ex);
      }
    }
    return ps;
  }

  private static void releasePreparedStatement(PreparedStatement ps) {
    if (ps != null) {
      try {
        ps.close();
      } catch (Exception e) {
        log4j.error("Error closing PreparedStatement", e);
      }
    }
  }

  public static void setUserId(String user) {
    if (user == null || !user.equals(getUserId())) {
      userId.set(user);
      changedInfo.set(true);
    }
  }

  public static String getUserId() {
    return userId.get();
  }

  public static void setProcessId(String processId) {
    if (processId == null || !processId.equals(getProcessId())) {
      SessionInfo.processId.set(processId);
      changedInfo.set(true);
    }
  }

  public static String getProcessId() {
    return processId.get();
  }

  public static void setProcessType(String processType) {
    if (processType == null || !processType.equals(getProcessType())) {
      SessionInfo.processType.set(processType);
      changedInfo.set(true);
    }
  }

  public static String getProcessType() {
    return processType.get();
  }

  public static void setSessionId(String session) {
    if (session == null || !session.equals(getSessionId())) {
      sessionId.set(session);
      changedInfo.set(true);
    }
  }

  /**
   * Forces changed info flag so next time it is checked it session info will be set in DB
   */
  public static void infoChanged() {
    changedInfo.set(true);
  }

  public static String getCommand() {
    return command.get();
  }

  public static void setCommand(String comm) {
    command.set(comm);
  }

  public static String getQueryProfile() {
    return queryProfile.get();
  }

  public static void setQueryProfile(String profile) {
    queryProfile.set(profile);
  }

  public static String getSessionId() {
    return sessionId.get();
  }

  public static void setAuditActive(boolean isAuditActive) {
    SessionInfo.isAuditActive = isAuditActive;
    SessionInfo.initialized = true;
  }

  public static boolean isInitialized() {
    return initialized;
  }

  static void setSessionConnection(Connection conn) {
    sessionConnection.set(conn);
  }

  public static String getModuleId() {
    return moduleId.get();
  }

  public static void setModuleId(String moduleId) {
    SessionInfo.moduleId.set(moduleId);
  }

  public static boolean isUsageAuditActive() {
    return usageAuditActive;
  }

  public static void setUsageAuditActive(boolean usageAuditActive) {
    SessionInfo.usageAuditActive = usageAuditActive;
  }

  /** Set this value to {@code false} to prevent context info to be set in DB */
  public static void auditThisThread(boolean shouldAudit) {
    auditThisThread.set(shouldAudit);
  }
}
