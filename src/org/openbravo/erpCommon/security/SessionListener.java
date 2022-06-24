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

package org.openbravo.erpCommon.security;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;

/**
 * Keeps track of active sessions in this context so they can be marked as no active in database
 * when they are destroyed. Also used to keep last activity info for CU management.
 */
public class SessionListener implements HttpSessionListener, ServletContextListener {

  private static final int PING_TIMEOUT_SECS = 120;

  private static final Logger log = LogManager.getLogger();

  private static Set<String> sessionsInContext = Collections
      .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  private static Set<HttpSession> activeHttpSessions = Collections
      .newSetFromMap(new ConcurrentHashMap<HttpSession, Boolean>());
  private static ServletContext context = null;

  /**
   * This method is called whenever the session is destroyed because of user action or time out.
   * 
   * It deactivates the session in db
   */
  @Override
  public void sessionDestroyed(HttpSessionEvent event) {
    HttpSession session = event.getSession();
    String sessionId = (String) session.getAttribute("#AD_SESSION_ID");
    if (sessionId != null) {
      deactivateSession(sessionId);
    }
    activeHttpSessions.remove(session);
    log.debug("Session destroyed. Active sessions count: " + activeHttpSessions.size());
  }

  /**
   * This method is invoked when the server is shot down, it deactivates all sessions in this
   * context.
   */
  @Override
  public void contextDestroyed(ServletContextEvent event) {
    if(OBPropertiesProvider.getInstance()
            .getBooleanProperty("cluster")) {
      log.info("Etendo is running as a cluster, skipping sessions deactivation");
      return;
    }
    log.info("Destroying context. " + sessionsInContext.size() + " sessions to deactivate in DB.");
    long t = System.currentTimeMillis();

    for (String sessionId : sessionsInContext) {
      try {
        // cannot use dal at this point, use sqlc
        SessionLoginData.deactivate(
            (ConnectionProvider) event.getServletContext().getAttribute("openbravoPool"),
            sessionId);
      } catch (ServletException e1) {
        log.error(e1.getMessage(), e1);
      }
    }
    SessionListener.context = null;
    // detaching db connection from thread so can it be returned to pool
    SessionInfo.init();
    log.info("Sessions deactivated in " + (System.currentTimeMillis() - t) + " ms");
  }

  /**
   * Add a session to session tracking. This will be used when shut dowing the server
   * 
   * @param sessionId
   *          db id for the session to keep track
   */
  public static void addSession(String sessionId) {
    sessionsInContext.add(sessionId);
  }

  /**
   * Sets the current context and deactivates orphan sessions.
   * 
   * Orphan sessions occur after a wrong context shutdown.
   */
  @Override
  public void contextInitialized(ServletContextEvent event) {
    SessionListener.context = event.getServletContext();

    ConnectionProvider cp = (ConnectionProvider) context.getAttribute("openbravoPool");
    try {
      try {
        // Mark as inactive those sessions that were active and didn't send any ping during last
        // 120secs. And those ones that didn't send any ping and were created at least 1 day ago.
        // This is similar to what is done in ActivationKey.deactivateTimeOutSessions but for all
        // types of sessions.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, (-1) * PING_TIMEOUT_SECS);

        String strDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(cal.getTime());
        long t = System.currentTimeMillis();
        int deactivatedSessions = SessionLoginData.deactivateExpiredSessions(cp, strDate);
        log.debug(
            "Deactivated " + deactivatedSessions + " old session(s) while starting server. Took: "
                + (System.currentTimeMillis() - t) + "ms.");
      } catch (Exception e) {
        log.error("Error deactivating expired sessions", e);
      }

      // Decide whether audit trail is active
      try {
        SessionInfo.setAuditActive(SessionLoginData.isAudited(cp));
      } catch (Exception e) {
        log.error("Error activating audit trail", e);
      }

      try {
        SessionInfo.setUsageAuditActive(SessionLoginData.isUsageAuditEnabled(cp));
      } catch (Exception e) {
        log.error("Error activating usage audit", e);
      }
    } finally {
      // detaching db connection from thread so can it be returned to pool
      SessionInfo.init();
    }
  }

  @Override
  public void sessionCreated(HttpSessionEvent event) {
    activeHttpSessions.add(event.getSession());

    if (RequestContext.get().getRequest() != null
        && AuthenticationManager.isStatelessRequest(RequestContext.get().getRequest())) {
      final String errorLog = RequestContext.get().getRequest().getRequestURL() + " "
          + RequestContext.get().getRequest().getQueryString();
      log.error("Request is stateless, still a session is created " + errorLog, new Exception());
    }

    log.debug("Session created. Active sessions count: " + activeHttpSessions.size());
  }

  /**
   * Returns the {@code HttpSession} identified by {@code sessionId} it is present in this context.
   * If not present {@code null} is returned.
   */
  public static HttpSession getActiveSession(String sessionId) {
    try {
      for (HttpSession session : activeHttpSessions) {
        if (sessionId.equals(session.getAttribute("#AD_SESSION_ID"))) {
          return session;
        }
      }
    } catch (Exception e) {
      log.error("Error getting active session from context", e);
      // give up and return null
    }
    return null;
  }

  private void deactivateSession(String sessionId) {
    try {
      sessionsInContext.remove(sessionId);

      // Do not use DAL here
      SessionLoginData.deactivate((ConnectionProvider) context.getAttribute("openbravoPool"),
          sessionId);
      log.debug("Closed session" + sessionId);
    } catch (Exception e) {
      log.error("Error closing session:" + sessionId, e);
    } finally {
      // detaching db connection from thread so can it be returned to pool
      SessionInfo.init();
    }
  }

  /**
   * Check whether a session is in the current context and it is active
   * 
   * @param sessionId
   *          session to check
   * @return true in case it is in the context and active
   */
  public static boolean isSessionActiveInContext(String sessionId) {
    boolean isInContext = sessionsInContext.contains(sessionId);

    if (!isInContext) {
      return false;
    }

    try {
      return SessionLoginData
          .isSessionActive((ConnectionProvider) context.getAttribute("openbravoPool"), sessionId);
    } catch (ServletException e) {
      log.error("Error checking active session " + sessionId, e);
      return false;
    }
  }

}
