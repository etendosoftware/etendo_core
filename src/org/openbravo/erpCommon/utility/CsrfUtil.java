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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.client.kernel.OBUserException;

public class CsrfUtil {
  private static final Logger log = LogManager.getLogger();

  private static Pattern csrfTokenPattern = Pattern
      .compile("\"csrfToken\":\"(?<token>[A-Z0-9]+)\"");

  public static void checkCsrfToken(String requestToken, HttpServletRequest request) {
    String sessionToken = getSessionCsrfToken(request);
    if (!hasValidCsrfToken(requestToken, sessionToken)) {
      log.error("CSRF token check failed. Request=" + request.getRequestURI() + ", SessionID="
          + request.getSession(false).getId() + ", SessionToken=" + sessionToken + ", RequestToken="
          + requestToken);
      throw new OBUserException("InvalidCSRFToken");
    }
  }

  public static boolean hasValidCsrfToken(String requestToken, String sessionToken) {
    return StringUtils.isNotEmpty(requestToken) && StringUtils.isNotEmpty(sessionToken)
        && requestToken.equals(sessionToken);
  }

  public static String getCsrfTokenFromRequestContent(String requestContent) {
    Matcher matcher = csrfTokenPattern.matcher(requestContent);
    if (matcher.find()) {
      return matcher.group("token");
    }

    return "";
  }

  private static String getSessionCsrfToken(HttpServletRequest request) {
    String token = (String) request.getSession(false).getAttribute("#CSRF_TOKEN");
    return token != null ? token : "";
  }
}
