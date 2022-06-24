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
 * All portions are Copyright (C) 2009-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.security;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.SessionInfo;

/**
 * Inserts usage auditory in database.
 * 
 * @author alostale
 * 
 */
public class UsageAudit {
  private static final Logger log4j = LogManager.getLogger();

  private static final String SESSION_ID_ATTR = "#AD_SESSION_ID";

  /**
   * Inserts a new record in usage audit in case auditory is enabled. Information is obtained from
   * vars parameter and SessionInfo.
   * 
   */
  public static void auditActionNoDal(ConnectionProvider conn, VariablesSecureApp vars,
      String javaClassName, long duration) {
    String sessionId = vars.getSessionValue(SESSION_ID_ATTR);
    String action = SessionInfo.getCommand();
    String objectType = SessionInfo.getProcessType();
    String moduleId = SessionInfo.getModuleId();

    final boolean auditAction = SessionInfo.isUsageAuditActive() && sessionId != null
        && !sessionId.isEmpty() && objectType != null && !objectType.isEmpty() && moduleId != null
        && !moduleId.isEmpty() && action != null && !action.isEmpty();
    if (!auditAction) {
      return;
    }

    String objectId = SessionInfo.getProcessId();
    try {
      if (log4j.isDebugEnabled()) {
        log4j.debug("Auditing sessionId: " + sessionId + " -  action:" + action + " - objectType:"
            + objectType + " - moduleId:" + moduleId + " - objectId:" + objectId
            + " - javaClassName:" + javaClassName);
      }
      SessionLoginData.insertUsageAudit(conn, SessionInfo.getUserId(), sessionId, objectId,
          moduleId, action, javaClassName, objectType, Long.toString(duration));
    } catch (ServletException se) {
      log4j.error("Error inserting usage audit", se);
    }
  }
}
