/*
 ************************************************************************************
 * Copyright (C) 2001-2020 Openbravo S.L.U.
 * Licensed under the Apache Software License version 2.0
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to  in writing,  software  distributed
 * under the License is distributed  on  an  "AS IS"  BASIS,  WITHOUT  WARRANTIES  OR
 * CONDITIONS OF ANY KIND, either  express  or  implied.  See  the  License  for  the
 * specific language governing permissions and limitations under the License.
 ************************************************************************************
 */
package org.openbravo.base.secureApp;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.authentication.AuthenticationException;
import org.openbravo.authentication.AuthenticationExpirationPasswordException;
import org.openbravo.authentication.AuthenticationManager;
import org.openbravo.authentication.ChangePasswordException;
import org.openbravo.authentication.hashing.PasswordHash;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.HttpBaseUtils;
import org.openbravo.base.secureApp.LoginUtils.RoleDefaults;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.application.CachedPreference;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.businessUtility.Preferences.QueryFilter;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.obps.ActivationKey.LicenseRestriction;
import org.openbravo.erpCommon.security.Login;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyConflictException;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Session;
import org.openbravo.model.ad.access.TokenUser;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserRoles;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.server.ServerControllerHandler;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.password.PasswordStrengthChecker;

/**
 * 
 * {@link LoginHandler} is called from {@link Login} Servlet after the user has entered user and
 * password. It checks user/ password validity as well as license settings and decides whether the
 * user can log in the application or not.
 * <p>
 * Depending if the instance is 2.50 or 3.0 the result of this Servlet differs. 2.50 instances show
 * the messages in a new window served by this Servlet and do the actual redirection in case of
 * success. 3.0 instance Login Servlet call LoginHandler as an ajax request and they expect to
 * obtain a json object with information about success or message and in this case the message to
 * show.
 * 
 */
public class LoginHandler extends HttpBaseServlet {
  private static final long serialVersionUID = 1L;
  public static final String SUCCESS_SESSION_STANDARD = "S";
// "dev-fut-test.us.auth0.com";
// "zxo9HykojJHT1HXg18KwUjCNlLPs3tZU";
// "EAlNG8TK063hfFWmQHRdn94F7qzle04GQ7q3O067_lTMzcKAG4tPQ6P476hxdRAV";
  private static final String ERROR = "Error";

  @Inject
  private ServerControllerHandler serverController;

  @Inject
  private CachedPreference cachedPreference;

  @Inject
  private PasswordStrengthChecker passwordStrengthChecker;

  @Inject
  @Any
  private Instance<LoginHandlerHook> hooks;

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res)
      throws IOException, ServletException {

    log4j.debug("start doPost");

    boolean isPasswordResetFlow = Boolean.parseBoolean(req.getParameter("resetPassword"));
    if (!isPasswordResetFlow) {
      // Cookie id will be reset every time a user logs in, to prevent a malicious user from
      // stealing a cookie which later on will correspond with a valid session
      // If we are in password reset flow, there is no need to reset the cookie as it was reset in
      // the previous attempt to login
      resetCookieId(req);
    }

    doOptions(req, res);
    final VariablesSecureApp vars = new VariablesSecureApp(req);

    // Empty session
    req.getSession().removeAttribute("#Authenticated_user");
    vars.removeSessionValue("#AD_Role_ID");
    vars.setSessionObject("#loggingIn", "Y");

    final String user;
    final String password;

    if (isPasswordResetFlow) {
      user = vars.getSessionValue("#AD_User_ID");
    } else if (!StringUtils.isBlank(req.getParameter("code")) && !StringUtils.isBlank(req.getParameter("state"))) {
      String token = getAuthToken(req);
      HashMap<String, String> tokenValues = decodeToken(token);
      User adUser = matchUser(token, tokenValues.get("sub"));
      if (adUser == null) {
        final Properties openbravoProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();
        String ssoDomain = ((String) openbravoProperties.get("sso.domain.url")).trim();
        String clientId = ((String) openbravoProperties.get("sso.client.id")).trim();
        String logoutRedirectUri = StringUtils.remove(req.getRequestURL().toString(), req.getServletPath()).trim();
        String contextName = ((String) openbravoProperties.get("context.name")).trim();
        log4j.info("context.name: " + contextName);
        log4j.info("sso.domain.url: " + ssoDomain);
        log4j.info("sso.client.id: " + clientId);
        log4j.info("logoutRedirectUri: " + logoutRedirectUri);
        log4j.info("Encoded logoutRedirectUri: " + URLEncoder.encode(logoutRedirectUri, StandardCharsets.UTF_8));
        String ssoNoUserLinkURL = String.format("/%s/secureApp/Auth0ErrorPage.html?ssoDomain=%s&clientId=%s&logoutRedirectUri=%s",
            contextName,
            URLEncoder.encode(ssoDomain, StandardCharsets.UTF_8),
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(logoutRedirectUri, StandardCharsets.UTF_8));
        try {
          log4j.info("User not found in the system, redirecting to: " + ssoNoUserLinkURL);
          res.sendRedirect(ssoNoUserLinkURL);
        } catch (Exception e) {
          log4j.error("Error redirecting to: " + ssoNoUserLinkURL + " --- " + e.getMessage(), e);
        }
        return;
      }
      req.setAttribute("user-token-sub", tokenValues.get("sub"));
      user = adUser.getUsername();
    } else {
      user = vars.getStringParameter("user");
    }

    OBContext.setAdminMode();
    try {
      Client systemClient = OBDal.getInstance().get(Client.class, "0");

      String language = systemClient.getLanguage().getLanguage();
      vars.setSessionValue("#AD_Language", language);
      ConnectionProvider cp = new DalConnectionProvider(false);
      if ("".equals(user)) {
        goToRetry(res, vars, Utility.messageBD(cp, "IDENTIFICATION_FAILURE_TITLE", language),
            Utility.messageBD(cp, "IDENTIFICATION_FAILURE_MSG", language), ERROR,
            "../security/Login");
      } else {
        try {
          if (isPasswordResetFlow && StringUtils.isNotBlank(vars.getSessionValue("#AD_User_ID"))) {
            password = vars.getStringParameter("password");
            updatePassword(user, password, language);
          }

          AuthenticationManager authManager = AuthenticationManager.getAuthenticationManager(this);

          final String strUserAuth = authManager.authenticate(req, res);
          final String sessionId = vars.getSessionValue("#AD_Session_ID");

          if (StringUtils.isEmpty(strUserAuth)) {
            throw new AuthenticationException("Message");// FIXME
          }

          checkLicenseAndGo(res, vars, strUserAuth, user, sessionId);

        } catch (AuthenticationExpirationPasswordException | ChangePasswordException exception) {
          vars.removeSessionValue("#LoginErrorMsg");
          goToUpdatePassword(res, vars, exception, "../security/Login_FS.html");
        } catch (AuthenticationException e) {

          final OBError errorMsg = e.getOBError();

          if (errorMsg != null) {
            vars.removeSessionValue("#LoginErrorMsg");

            final String failureTitle = Utility.messageBD(cp, errorMsg.getTitle(), language);
            final String failureMessage = Utility.messageBD(cp, errorMsg.getMessage(), language);

            goToRetry(res, vars, failureMessage, failureTitle, ERROR,
                "../security/Login_FS.html");

          } else {
            throw new ServletException(ERROR); // FIXME
          }
        }
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Matches the user based on the provided token and subject.
   *
   * @param token the authentication token
   * @param sub   the subject identifier from the token
   * @return the matched User object, or null if no match is found
   */
  private User matchUser(String token, String sub) {
    OBContext.setAdminMode(true);
    TokenUser tokenUser = (TokenUser) OBDal.getInstance().createCriteria(TokenUser.class)
        .add(Restrictions.eq(TokenUser.PROPERTY_SUB, sub))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setMaxResults(1).uniqueResult();
    if (tokenUser != null) {
      tokenUser.setToken(token);
    } else {
      return null;
    }
    return tokenUser.getUser();
  }

  /**
   * Decodes the provided token and extracts its claims.
   *
   * @param token the authentication token
   * @return a HashMap containing the token claims
   */
  private HashMap<String, String> decodeToken(String token) {

    HashMap<String, String> tokenValues = new HashMap<>();
    DecodedJWT decodedJWT = JWT.decode(token);

    tokenValues.put("given_name", decodedJWT.getClaim("given_name").asString());
    tokenValues.put("family_name", decodedJWT.getClaim("family_name").asString());
    tokenValues.put("email", decodedJWT.getClaim("email").asString());
    tokenValues.put("sid", decodedJWT.getClaim("sid").asString());
    tokenValues.put("sub", decodedJWT.getClaim("sub").asString());
    return tokenValues;
  }

  /**
   * Retrieves the authentication token from the request.
   *
   * @param request the HttpServletRequest object
   * @return the authentication token, or null if the token could not be retrieved
   */
  private String getAuthToken(HttpServletRequest request) {
    String code = request.getParameter("code");
    String token = "";
    String domain = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.domain.url");
    String tokenEndpoint = "https://" + domain + "/oauth/token";
    try {
      URL url = new URL(tokenEndpoint);
      HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      con.setDoOutput(true);

      String clientId = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.id");
      String clientSecret = OBPropertiesProvider.getInstance().getOpenbravoProperties().getProperty("sso.client.secret");

      String codeVerifier = (String) request.getSession().getAttribute("code_verifier");
      boolean isPKCE = (codeVerifier != null && !codeVerifier.isEmpty());
      String strDirection = request.getScheme() + "://" + request.getServerName() + request.getContextPath() + "/secureApp/LoginHandler.html";
      String params;
      if (isPKCE) {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&code=%s&redirect_uri=%s&code_verifier=%s",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(strDirection, StandardCharsets.UTF_8),
            URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
        );
      } else {
        params = String.format(
            "grant_type=authorization_code&client_id=%s&client_secret=%s&code=%s&redirect_uri=%s",
            URLEncoder.encode(clientId, StandardCharsets.UTF_8),
            URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
            URLEncoder.encode(code, StandardCharsets.UTF_8),
            URLEncoder.encode(strDirection, StandardCharsets.UTF_8)
        );
      }

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = params.getBytes(StandardCharsets.UTF_8);
        os.write(input, 0, input.length);
      } catch (Exception e) {
        log4j.error(e.getMessage(), e);
      }

      int status = con.getResponseCode();
      if (status == 200) {
        try (InputStream in = con.getInputStream()) {
          String responseBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          JSONObject jsonResponse = new JSONObject(responseBody);
          token = jsonResponse.getString("id_token");
        }
      } else {
        log4j.error(con.getResponseMessage());
        token = null;
      }
    } catch (JSONException | IOException e) {
      log4j.error(e);
    }
    return token;
  }

  /**
   * This method invalidates the current session and generates a new one on the fly, thus generating
   * a new JSSESSIONID cookie. It is called every time the user logs in to prevent some malicious
   * user from stealing a cookie which later on will correspond with a valid session
   */
  private void resetCookieId(HttpServletRequest req) {
    HttpSession httpSession = req.getSession(false);
    if (httpSession != null && !httpSession.isNew()) {
      httpSession.invalidate();
    }
    httpSession = req.getSession(true);

  }

  @Override
  public void doOptions(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    setCORSHeaders(request, response);
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
      doPost(request, response);
  }

  protected void setCORSHeaders(HttpServletRequest request, HttpServletResponse response) {

    String origin = request.getHeader("Origin");

    if (origin != null && !origin.equals("")) {
      response.setHeader("Access-Control-Allow-Origin", origin);
      response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
      response.setHeader("Access-Control-Allow-Headers",
          "Content-Type, origin, accept, X-Requested-With");
      response.setHeader("Access-Control-Max-Age", "1000");
    }
  }

  @Deprecated
  protected final void checkLicenseAndGo(HttpServletResponse res, VariablesSecureApp vars,
      String strUserAuth, String username, String sessionId, boolean doRedirect)
      throws IOException, ServletException {
    checkLicenseAndGo(res, vars, strUserAuth, username, sessionId);
  }

  protected final void checkLicenseAndGo(HttpServletResponse res, VariablesSecureApp vars,
      String strUserAuth, String username, String sessionId) throws IOException, ServletException {
    OBContext.setAdminMode();
    ConnectionProvider cp = new DalConnectionProvider(false);
    try {
      ActivationKey ak = ActivationKey.getInstance(true);
      boolean hasSystem = false;

      try {
        hasSystem = SeguridadData.hasSystemRole(cp, strUserAuth);
      } catch (Exception ignore) {
        log4j.error(ignore);
      }
      String msgType, action;
      if (hasSystem) {
        msgType = "Warning";
        action = "../security/Menu.html";
      } else {
        msgType = ERROR;
        action = "../security/Login_FS.html";
      }

      LicenseRestriction limitation = ak.checkOPSLimitations(sessionId, getSessionType());

      SystemInformation sysInfo = OBDal.getInstance().get(SystemInformation.class, "0");

      // We check if there is a Openbravo Professional Subscription restriction in the license,
      // or if the last rebuild didn't go well. If any of these are true, then the user is
      // allowed to login only as system administrator
      switch (limitation) {
        case NUMBER_OF_CONCURRENT_USERS_REACHED:
          String msg = Utility.messageBD(cp, "NUMBER_OF_CONCURRENT_USERS_REACHED",
              vars.getLanguage());
          String title = Utility.messageBD(cp, "NUMBER_OF_CONCURRENT_USERS_REACHED_TITLE",
              vars.getLanguage());
          log4j.warn("Concurrent Users Reached - Session: " + sessionId);
          updateDBSession(sessionId, msgType.equals("Warning"), "CUR");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case NUMBER_OF_SOFT_USERS_REACHED:
          msg = Utility.messageBD(cp, "NUMBER_OF_SOFT_USERS_REACHED", vars.getLanguage());
          title = Utility.messageBD(cp, "NUMBER_OF_SOFT_USERS_REACHED_TITLE", vars.getLanguage());
          action = "../security/Menu.html";
          msgType = "Warning";
          log4j.warn("Soft Users Reached - Session: " + sessionId);
          updateDBSession(sessionId, true, "SUR");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case OPS_INSTANCE_NOT_ACTIVE:
          msg = Utility.messageBD(cp, "OPS_INSTANCE_NOT_ACTIVE", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_INSTANCE_NOT_ACTIVE_TITLE", vars.getLanguage());
          log4j.warn("Innactive OBPS instance - Session: " + sessionId);
          updateDBSession(sessionId, msgType.equals("Warning"), "IOBPS");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case MODULE_EXPIRED:
          msg = Utility.messageBD(cp, "OPS_MODULE_EXPIRED", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_MODULE_EXPIRED_TITLE", vars.getLanguage());
          StringBuffer expiredMoudules = new StringBuffer();
          log4j.warn("Expired modules - Session: " + sessionId);
          for (Module module : ak.getExpiredInstalledModules()) {
            expiredMoudules.append("<br/>").append(module.getName());
            log4j.warn("  module:" + module.getName());
          }
          msg += expiredMoudules.toString();
          updateDBSession(sessionId, msgType.equals("Warning"), "ME");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case NOT_MATCHED_INSTANCE:
          msg = Utility.messageBD(cp, "OPS_NOT_MATCHED_INSTANCE", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_NOT_MATCHED_INSTANCE_TITLE", vars.getLanguage());
          log4j.warn("No matched instance - Session: " + sessionId);
          updateDBSession(sessionId, msgType.equals("Warning"), "IOBPS");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case HB_NOT_ACTIVE:
          msg = Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_NOT_HB_ACTIVE_TITLE", vars.getLanguage());
          log4j.warn("HB not active - Session: " + sessionId);
          updateDBSession(sessionId, msgType.equals("Warning"), "IOBPS");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case EXPIRED_GOLDEN:
          msg = Utility.messageBD(cp, "OPS_EXPIRED_GOLDEN", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_EXPIRED_GOLDEN_TITLE", vars.getLanguage());
          updateDBSession(sessionId, false, "IOBPS");
          goToRetry(res, vars, msg, title, ERROR, "../security/Login_FS.html");
          return;
        case POS_TERMINALS_EXCEEDED:
          msg = Utility.messageBD(cp, "OPS_POS_TERMINALS_EXCEEDED", vars.getLanguage());
          title = Utility.messageBD(cp, "OPS_POS_TERMINALS_EXCEEDED_TITLE", vars.getLanguage());
          log4j.warn("Exceeded maximum number of pos terminals");
          goToRetry(res, vars, msg, title, msgType, action);
          return;
        case NO_RESTRICTION:
          break;
      }

      boolean hasNonRestrictedRole = false;
      User user = OBDal.getInstance().get(User.class, strUserAuth);
      for (UserRoles userrole : user.getADUserRolesList()) {
        if (!userrole.getRole().isRestrictbackend()) {
          hasNonRestrictedRole = true;
          break;
        }
      }
      if (!hasNonRestrictedRole) {
        String msg = Utility.messageBD(cp, "NON_RESTRICTED_ROLE", vars.getLanguage());
        String title = Utility.messageBD(cp, "NON_RESTRICTED_ROLE_TITLE", vars.getLanguage());
        updateDBSession(sessionId, false, "RESTR");
        goToRetry(res, vars, msg, title, ERROR, action);
        return;
      }
      // Build checks

      if (sysInfo.getSystemStatus() == null || sysInfo.getSystemStatus().equals("RB70")
          || this.globalParameters.getOBProperty("safe.mode", "false").equalsIgnoreCase("false")) {
        // Last build went fine and tomcat was restarted. We should continue with the rest of checks
      } else if (sysInfo.getSystemStatus().equals("RB60")
          || sysInfo.getSystemStatus().equals("RB51")) {
        String msg = Utility.messageBD(cp, "TOMCAT_NOT_RESTARTED", vars.getLanguage());
        String title = Utility.messageBD(cp, "TOMCAT_NOT_RESTARTED_TITLE", vars.getLanguage());
        log4j.warn("Tomcat not restarted");
        updateDBSession(sessionId, true, "RT");
        goToRetry(res, vars, msg, title, "Warning", "../security/Menu.html");
        return;
      } else {
        String msg = Utility.messageBD(cp, "LAST_BUILD_FAILED", vars.getLanguage());
        String title = Utility.messageBD(cp, "LAST_BUILD_FAILED_TITLE", vars.getLanguage());
        updateDBSession(sessionId, msgType.equals("Warning"), "LBF");
        goToRetry(res, vars, msg, title, msgType, action);
        return;
      }

      // WS calls restrictions
      if (hasSystem && ak.getWsCallsExceededDays() > 0) {
        String msg;
        String title = Utility.messageBD(cp, "OPS_MAX_WS_CALLS_TITLE", vars.getLanguage());

        switch (ak.checkNewWSCall(false)) {
          case NO_RESTRICTION:
          case EXPIRED:
          case EXPIRED_MODULES:
            break;
          case EXCEEDED_WARN_WS_CALLS:
            msg = Utility.messageBD(cp, "OPS_MAX_WS_CALLS_SOFT_MSG", vars.getLanguage(), false)
                .replace("@daysExceeding@", Integer.toString(ak.getWsCallsExceededDays()))
                .replace("@extraDays@", Integer.toString(ak.getExtraWsExceededDaysAllowed()))
                .replace("@numberOfDays@", Integer.toString(ak.getNumberOfDaysLeftInPeriod()));
            goToRetry(res, vars, msg, title, msgType, action);
            return;
          case EXCEEDED_MAX_WS_CALLS:
            msg = Utility.messageBD(cp, "OPS_MAX_WS_CALLS_MSG", vars.getLanguage(), false)
                .replace("@daysExceeding@", Integer.toString(ak.getWsCallsExceededDays()));
            goToRetry(res, vars, msg, title, msgType, action);
            return;
        }
      }

      // checks if the current server is a store server, and in that case, if the access is
      // restricted
      vars.removeSessionValue("OnlySystemAdminAccess");
      if (serverController.isThisAStoreServer() && isLoginAccessRestrictedInStoreServer(vars)) {
        // sets the onlySystemAdminRoleShouldBeAvailableInErp flag to "Y" to:
        // * use it in UserInfoWidgetActionHandler.getRoles to make System Admin the only role
        // available
        // * use it in index.jsp to make sure the ERP is only available to users with the System
        // Admin role
        vars.setSessionValue("onlySystemAdminRoleShouldBeAvailableInErp", "Y");
        String msg = Utility.messageBD(cp, "BACKEND_LOGIN_RESTRICTED", vars.getLanguage());
        String title = Utility.messageBD(cp, "BACKEND_LOGIN_RESTRICTED_TITLE", vars.getLanguage());
        goToRetry(res, vars, msg, title, msgType, action);
        return;
      }

      for (LoginHandlerHook hook : hooks) {
        OBError error = hook.process(username, action);
        if (error != null) {
          String msg = Utility.messageBD(cp, error.getMessage(), vars.getLanguage());
          String title = Utility.messageBD(cp, error.getTitle(), vars.getLanguage());
          final String urlToRedirect = StringUtils.equals(ERROR,
              error.getType()) ? "../security/Login_FS.htm" : "../security/Menu.html";
          goToRetry(res, vars, msg, title, error.getType(), urlToRedirect);
          return;
        }
      }

      RoleDefaults userLoginDefaults;
      try {
        userLoginDefaults = LoginUtils.getLoginDefaults(strUserAuth, "", cp);
      } catch (DefaultValidationException e) {
        updateDBSession(sessionId, false, "F");
        String title = Utility.messageBD(cp, "InvalidDefaultLoginTitle", vars.getLanguage())
            .replace("%0", e.getDefaultField());
        String msg = Utility.messageBD(cp, "InvalidDefaultLoginMsg", vars.getLanguage())
            .replace("%0", e.getDefaultField());
        goToRetry(res, vars, msg, title, ERROR, "../security/Menu.html");
        return;
      }

      String target = getUserStartPage(strUserAuth, userLoginDefaults,
          vars.getSessionValue("target"), vars.getStringParameter("targetQueryString"));
      vars.removeSessionValue("target");

      goToTarget(res, target);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  /** Is current login for a back-office session */
  protected boolean isBackOfficeLogin() {
    return true;
  }

  private String getUserStartPage(String userId, RoleDefaults rd, String target,
      String targetQueryString) {
    String startPage = null;
    try {
      Map<QueryFilter, Boolean> queryFilters = new HashMap<>();
      queryFilters.put(QueryFilter.ACTIVE, true);
      queryFilters.put(QueryFilter.CLIENT, false);
      queryFilters.put(QueryFilter.ORGANIZATION, false);
      startPage = Preferences.getPreferenceValue("StartPage", true, rd.client, rd.org, userId,
          rd.role, null, queryFilters);
    } catch (PropertyConflictException e) {
      // ignore show normal page
      log4j.warn("Conflict getting StartPage preference. Showing normal page.");
    } catch (PropertyException e) {
      log4j.debug("Could not retrieve StartPage preference. Showing normal page.");
    }
    if (startPage != null) {
      if (targetQueryString != null && targetQueryString.length() > 0) {
        final String separator = startPage.contains("?") ? "&" : "?";
        return strDireccion + startPage + separator + targetQueryString;
      } else {
        return strDireccion + startPage;
      }
    } else if ("".equals(target)) {
      return strDireccion + "/";
    } else {
      return target;
    }
  }

  /** Returns how the successful session will be marked in ad_session. It can be app specific. */
  protected String getSessionType() {
    return SUCCESS_SESSION_STANDARD;
  }

  /**
   * Returns true if the access to the current login handler should be restricted in the store
   * servers
   */
  protected boolean isLoginAccessRestrictedInStoreServer(VariablesSecureApp vars) {
    return isErpAccessRestrictedInStoreServer();
  }

  /**
   * Checks if the RestrictErpAccessInStoreServer preference has been set to "Y". In that case, the
   * access to the ERP will be restricted in store servers and only the System Admin role will be
   * available
   */
  protected boolean isErpAccessRestrictedInStoreServer() {
    String restrictErpAccessInStoreServer = cachedPreference
        .getPreferenceValue(CachedPreference.RESTRICT_ERP_ACCESS_IN_STORE_SERVER);
    return Preferences.YES.equals(restrictErpAccessInStoreServer);
  }

  private void updateDBSession(String sessionId, boolean sessionActive, String status) {
    try {
      OBContext.setAdminMode();
      Session session = OBDal.getInstance().get(Session.class, sessionId);
      session.setSessionActive(sessionActive);
      session.setLoginStatus(status);
      OBDal.getInstance().flush();
    } catch (Exception e) {
      log4j.error("Error updating session in DB", e);
    } finally {
      OBContext.restorePreviousMode();
    }

  }

  private void goToTarget(HttpServletResponse response, String target)
      throws IOException, ServletException {

    // Return a JSON object with the target to redirect to
    try {
      JSONObject jsonResult = new JSONObject();
      jsonResult.put("showMessage", false);
      jsonResult.put("target", target);

      response.setContentType("application/json;charset=UTF-8");
      final PrintWriter out = response.getWriter();
      out.print(jsonResult.toString());
      out.close();
    } catch (JSONException e) {
      log4j.error("Error setting login msg", e);
      throw new ServletException(e);
    }
  }

  @Deprecated
  protected final void goToRetry(HttpServletResponse response, VariablesSecureApp vars,
      String message, String title, String msgType, String action, boolean doRedirect)
      throws IOException, ServletException {
    goToRetry(response, vars, message, title, msgType, action);
  }

  protected final void goToRetry(HttpServletResponse response, VariablesSecureApp vars,
      String message, String title, String msgType, String action)
      throws IOException, ServletException {
    String msg = (message != null && !message.equals("")) ? message
        : Utility.messageBD(myPool, "CPEmptyUserPassword", vars.getLanguage());
    String targetQueryString = vars.getStringParameter("targetQueryString");
    String target = StringUtils.isBlank(targetQueryString) ? action
        : action + "?" + targetQueryString;

    // Show the message in the login window, return a JSON object with the info to print the message
    try {
      boolean loginHasError = ERROR.equals(msgType);
      JSONObject jsonMsg = new JSONObject();
      jsonMsg.put("showMessage", true);
      jsonMsg.put("target", loginHasError ? null : target);
      jsonMsg.put("messageType", msgType);
      jsonMsg.put("messageTitle", title);
      jsonMsg.put("messageText", msg);

      if (loginHasError && isBackOfficeLogin()) {
        // mobile apps expect session to be populated in case of backoffice restricted roles
        vars.clearSession(false);
      }

      response.setContentType("application/json;charset=UTF-8");
      final PrintWriter out = response.getWriter();
      out.print(jsonMsg.toString());
      out.close();
    } catch (JSONException e) {
      log4j.error("Error setting login msg", e);
      throw new ServletException(e);
    }
  }

  private void goToUpdatePassword(HttpServletResponse response, VariablesSecureApp vars,
      AuthenticationException authenticationException, String action)
      throws IOException, ServletException {
    String title = authenticationException.getOBError().getTitle();
    String message = authenticationException.getOBError().getMessage();

    String msg = (message != null && !message.equals("")) ? message
        : Utility.messageBD(myPool, "CPEmptyUserPassword", vars.getLanguage());

    try {
      JSONObject jsonMsg = new JSONObject();
      jsonMsg.put("showMessage", true);
      jsonMsg.put("target", action);
      jsonMsg.put("messageType", ERROR);
      jsonMsg.put("messageTitle", title);
      jsonMsg.put("messageText", msg);
      jsonMsg.put("resetPassword", true);
      if (authenticationException instanceof ChangePasswordException) {
        jsonMsg.put("attemptedChange", true);
      }
      jsonMsg.put("loggedUser", vars.getStringParameter("user"));
      response.setContentType("application/json;charset=UTF-8");
      final PrintWriter out = response.getWriter();
      out.print(jsonMsg.toString());
      out.close();
    } catch (JSONException e) {
      log4j.error("Error setting login msg", e);
      throw new ServletException(e);
    }
  }

  @Override
  public String getServletInfo() {
    return "User-login control Servlet";
  } // end of getServletInfo() method

  /**
   * Update user password for userId with unHashedPassword provided, throws
   * AuthenticationExpirationPasswordException when the provided password is the same as the old one
   * or when the password is not strong enough.
   * 
   * @param userId
   *          the userId
   * @param newPassword
   *          the password, the plain text password as it is entered by the user.
   * @param language
   *          Default language for the user
   * @throws ServletException
   *           ServletException is thrown in case that password could not be hashed
   * 
   */
  private void updatePassword(String userId, String newPassword, String language) {
    try {
      OBContext.setAdminMode();
      User user = (User) OBDal.getInstance()
          .createCriteria(User.class)
          .add(Restrictions.eq(User.PROPERTY_ID, userId))
          .setFilterOnReadableClients(false)
          .setFilterOnReadableOrganization(false)
          .uniqueResult();

      if (PasswordHash.matches(newPassword, user.getPassword())) {
        throwChangePasswordException("CPSamePasswordThanOld", "CPDifferentPassword", language);
      } else if (!passwordStrengthChecker.isStrongPassword(newPassword)) {
        throwChangePasswordException("CPWeakPasswordTitle", "CPPasswordNotStrongEnough", language);
      } else {
        user.setPassword(PasswordHash.generateHash(newPassword));
        OBDal.getInstance().commitAndClose();
      }
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void throwChangePasswordException(String titleKey, String messageKey, String language)
      throws ChangePasswordException {
    OBError errorMsg = new OBError();
    errorMsg.setType(ERROR);
    errorMsg.setTitle(Utility.messageBD(myPool, titleKey, language));
    errorMsg.setMessage(Utility.messageBD(myPool, messageKey, language));
    throw new ChangePasswordException(errorMsg.getMessage(), errorMsg);
  }
}
