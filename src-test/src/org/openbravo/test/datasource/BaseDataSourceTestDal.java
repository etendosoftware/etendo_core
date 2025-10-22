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
 * All portions are Copyright (C) 2014-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.datasource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.test.base.OBBaseTest;

/**
 * Base for tests performing requests to a live Openbravo instance. Allows to work with DAL, in case
 * it is not needed {@link BaseDataSourceTestNoDal} can be used instead.
 *
 * NOTE FOR DEVELOPERS: {@link BaseDataSourceTestNoDal} class should be maintained in parallel to
 * this one
 *
 * @author alostale
 *
 */
public class BaseDataSourceTestDal extends OBBaseTest {
  private static Logger log = LogManager.getLogger();
  private static String OB_URL = null;
  protected static final String LOGIN = "admin";
  protected static final String PWD = "admin";
  private static boolean authenticated = false;
  private static String cookie;
  private static String csrfToken;

  protected static final String POST_METHOD = "POST";

  /**
   * @see BaseDataSourceTestDal#doRequest(String, String, int, String, String)
   */
  protected String doRequest(String wsPart, String content, int expectedResponse, String method)
      throws Exception {
    return doRequest(wsPart, content, 200, method, null);
  }

  /**
   * @see BaseDataSourceTestDal#doRequest(String, String, int, String, String)
   */
  protected String doRequest(String wsPart, Map<String, String> params, int expectedResponse,
      String method) throws Exception {
    return doRequest(wsPart, DatasourceTestUtil.getParamsContent(params), expectedResponse, method,
        null);
  }

  /**
   * Performs a request to Openbravo returning its response and asserting the response code matches
   * expectedResponse. Before performing the requests, this method tries to authenticate with
   * current settings if already not authenticated.
   *
   * @return a {@code String} containing the response for the request
   */
  protected String doRequest(String wsPart, String content, int expectedResponse, String method,
      String contentType) throws Exception {
    authenticate();

    return DatasourceTestUtil.request(getOpenbravoURL(), wsPart, method, content, cookie, 200,
        contentType);
  }

  /**
   * Performs a request to authenticate with current settings if already not authenticated.
   *
   * @return a {@code String} representing the cookie with authenticated session id
   */
  protected String authenticate() throws Exception {
    if (!authenticated) {
      cookie = DatasourceTestUtil.authenticate(getOpenbravoURL(), getLogin(), getPassword());
      authenticated = true;
    }
    return cookie;
  }

  protected String getSessionCsrfToken() {
    if (csrfToken == null) {
      csrfToken = getTokenFromSessionDynamic();
    }
    return csrfToken;
  }

  private String getTokenFromSessionDynamic() {
    Map<String, String> params = new HashMap<>();
    try {
      String response = doRequest("/org.openbravo.client.kernel/OBCLKER_Kernel/SessionDynamic",
          params, HttpServletResponse.SC_OK, POST_METHOD);
      return findCsrfTokenInResponse(response);
    } catch (Exception e) {
      log.error("Cannot retrieve CSRF Token", e);
      return "";
    }
  }

  private String findCsrfTokenInResponse(String response) throws Exception {
    Pattern pattern = Pattern.compile("csrfToken:\\s?\'([A-Z0-9]+)\'");
    Matcher matcher = pattern.matcher(response);
    if (matcher.find()) {
      return matcher.group(1);
    }

    throw new Exception("Cannot find CSRF Token in SessionDynamic response");
  }

  /**
   * Obtains URL of Openbravo instance, by default taken from Openbravo.poperties context.url
   * property
   *
   * @return a {@code String} with the URL of Openbravo instance
   */
  protected String getOpenbravoURL() {
    if (OB_URL != null) {
      return OB_URL;
    }
    OB_URL = DatasourceTestUtil.getOpenbravoURL();
    return OB_URL;
  }

  /**
   * Returns the login used to login for the requests. The default value is {@link #LOGIN}
   *
   * @return the login name used to login for the requests
   */
  protected String getLogin() {
    return LOGIN;
  }

  /**
   * Returns the password used to login for the requests. The default value is {@link #PWD}
   *
   * @return the password used to login for the requests
   */
  protected String getPassword() {
    return PWD;
  }

  /**
   * Changes current session's profile
   */
  protected void changeProfile(String roleId, String langId, String orgId, String warehouseId)
      throws Exception {
    if (!authenticated) {
      cookie = DatasourceTestUtil.authenticate(getOpenbravoURL(), getLogin(), getPassword());
      authenticated = true;
    }

    DatasourceTestUtil.changeProfile(getOpenbravoURL(), cookie, roleId, langId, orgId, warehouseId);
    csrfToken = getTokenFromSessionDynamic();
  }

  /** Logs out current session */
  protected void logout() throws Exception {
    Map<String, String> params = new HashMap<>();
    params.put("_action", "org.openbravo.client.application.LogOutActionHandler");
    doRequest("/org.openbravo.client.kernel", params, HttpServletResponse.SC_OK, POST_METHOD);
    authenticated = false;
    cookie = null;
    csrfToken = null;
  }
}
