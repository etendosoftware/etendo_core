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
 * All portions are Copyright (C) 2001-2018 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.security;

import java.io.IOException;
import java.io.PrintWriter;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.hibernate.query.Query;
import org.openbravo.base.HttpBaseServlet;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.obps.ActivationKey;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.SystemInformation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.xmlEngine.XmlDocument;

public class Login extends HttpBaseServlet {
  private static final long serialVersionUID = 1L;

  private static final String GOOGLE_PREFERENCE_PROPERTY = "OBSEIG_ShowGIcon";

  @Inject
  @Any
  private Instance<SignInProvider> signInProvider;

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {

    final VariablesSecureApp vars = new VariablesSecureApp(request);

    if (vars.commandIn("CHECK")) {
      // Context reload check
      String checkString = "success";
      response.setContentType("text/plain; charset=UTF-8");
      response.setHeader("Cache-Control", "no-cache");
      PrintWriter out = response.getWriter();
      out.print(checkString);
      out.close();
    } else {
      // Look for forced login URL property and redirect in case it is set and the login is accessed
      // through a different URL
      try {
        String forcedLoginUrl = Preferences.getPreferenceValue("ForcedLoginURL", true,
            (Client) null, null, null, null, null);
        log4j.debug("Forced URL: " + forcedLoginUrl);
        if (forcedLoginUrl != null && !forcedLoginUrl.isEmpty()
            && !request.getRequestURL().toString().startsWith(forcedLoginUrl)) {
          log4j.info("Redirecting login from " + request.getRequestURL().toString()
              + " to forced login URL " + forcedLoginUrl);
          response.sendRedirect(forcedLoginUrl);
          return;
        }
      } catch (PropertyException e) {
        // Ignore and continue with the standard login. PropertyException is raised in case property
        // is not defined (standard case) or in case of conflict.
        log4j.debug("Exception getting ForcedLoginURL", e);
      }

      // Standard login
      String strTheme = vars.getTheme();
      OBContext.setAdminMode();
      try {
        ConnectionProvider cp = new DalConnectionProvider(false);
        Client systemClient = OBDal.getInstance().get(Client.class, "0");
        String systemLanguage = systemClient.getLanguage().getLanguage();
        final String cacheMsg = Utility.messageBD(cp, "OUTDATED_FILES_CACHED", systemLanguage);
        final String validBrowserMsg = Utility.messageBD(cp, "BROWSER_NOT_SUPPORTED",
            systemLanguage);
        final String orHigherMsg = Utility.messageBD(cp, "OR_HIGHER_TEXT", systemLanguage);
        final String recBrowserMsgTitle = Utility.messageBD(cp, "RECOMMENDED_BROWSER_TITLE",
            systemLanguage);
        final String recBrowserMsgText = Utility.messageBD(cp, "RECOMMENDED_BROWSER_TEXT",
            systemLanguage);
        final String identificationFailureTitle = Utility.messageBD(cp,
            "IDENTIFICATION_FAILURE_TITLE", systemLanguage);
        final String emptyUsernameOrPasswordText = Utility.messageBD(cp,
            "EMPTY_USERNAME_OR_PASSWORD_TEXT", systemLanguage);
        final String errorSamePassword = Utility.messageBD(cp, "CPSamePassword", systemLanguage);
        final String errorDifferentPasswordInFields = Utility.messageBD(cp,
            "CPDifferentPasswordInFields", systemLanguage);

        printPageLogin(vars, response, strTheme, cacheMsg, validBrowserMsg, orHigherMsg,
            recBrowserMsgTitle, recBrowserMsgText, identificationFailureTitle,
            emptyUsernameOrPasswordText, errorSamePassword, errorDifferentPasswordInFields);
      } finally {
        vars.clearSession(false);
        OBContext.restorePreviousMode();
      }
    }
  }

  private void printPageLogin(VariablesSecureApp vars, HttpServletResponse response,
      String strTheme, String cacheMsg, String validBrowserMsg, String orHigherMsg,
      String recBrowserMsgTitle, String recBrowserMsgText, String identificationFailureTitle,
      String emptyUsernameOrPasswordText, String errorSamePassword,
      String errorDifferentPasswordInFields) throws IOException {

    boolean showITLogo = false;
    boolean showCompanyLogo = false;
    boolean showGSignInButtonDemo = true;

    String itLink = "";
    String companyLink = "";
    SystemInformation sysInfo = OBDal.getInstance().get(SystemInformation.class, "0");

    ActivationKey ak = ActivationKey.getInstance(true);
    if (ak.isActive()) {
      String hql = "from ADPreference pref where searchKey like :value and property = :prop and (visibleAtClient is null or visibleAtClient.id = '0')";
      Query<Object> q = OBDal.getInstance().getSession().createQuery(hql, Object.class);
      q.setParameter("value", "N");
      q.setParameter("prop", GOOGLE_PREFERENCE_PROPERTY);

      // show by default - not show when there is a preference to disable it
      showGSignInButtonDemo = q.list().size() == 0;
    }

    if (sysInfo == null) {
      log4j.error("System information not found");
    } else {
      showITLogo = sysInfo.getYourItServiceLoginImage() != null;
      showCompanyLogo = sysInfo.getYourCompanyLoginImage() != null;

      itLink = sysInfo.getSupportContact() == null ? "" : sysInfo.getSupportContact();
      if (!itLink.isEmpty() && !(StringUtils.startsWithIgnoreCase(itLink, "http://")
          || StringUtils.startsWithIgnoreCase(itLink, "https://")
          || StringUtils.startsWithIgnoreCase(itLink, "ftp://"))) {
        itLink = "http://" + itLink;
      }
      companyLink = sysInfo.getYourCompanyURL() == null ? "" : sysInfo.getYourCompanyURL();
      if (!companyLink.isEmpty() && !(StringUtils.startsWithIgnoreCase(companyLink, "http://")
          || StringUtils.startsWithIgnoreCase(companyLink, "https://")
          || StringUtils.startsWithIgnoreCase(companyLink, "ftp://"))) {
        companyLink = "http://" + companyLink;
      }
    }

    Client systemClient = OBDal.getInstance().get(Client.class, "0");
    ConnectionProvider cp = new DalConnectionProvider(false);
    xmlEngine.sessionLanguage = systemClient.getLanguage().getLanguage();

    XmlDocument xmlDocument = xmlEngine.readXmlTemplate("org/openbravo/erpCommon/security/Login")
        .createXmlDocument();

    xmlDocument.setParameter("theme", strTheme);

    String visualPrefs = "var showCompanyLogo = " + showCompanyLogo + ", showSupportLogo = "
        + showITLogo + ", urlCompany = '" + companyLink + "', urlSupport = '" + itLink + "';";
    xmlDocument.setParameter("visualPrefs", visualPrefs);

    String expirationMessage = "var expirationMessage="
        + ak.getExpirationMessage(vars.getLanguage()).toString() + ";";
    xmlDocument.setParameter("expirationMessage", expirationMessage);

    insertMessageInPage(xmlDocument, "cacheMsg", cacheMsg);
    insertMessageInPage(xmlDocument, "identificationFailureTitle", identificationFailureTitle);

    insertMessageInPage(xmlDocument, "errorEmptyContent", emptyUsernameOrPasswordText);
    insertMessageInPage(xmlDocument, "errorSamePassword", errorSamePassword);
    insertMessageInPage(xmlDocument, "errorDifferentPasswordInFields",
        errorDifferentPasswordInFields);

    insertMessageInPage(xmlDocument, "validBrowserMsg", validBrowserMsg);
    insertMessageInPage(xmlDocument, "validBrowserMsgOrHigher", orHigherMsg);

    insertMessageInPage(xmlDocument, "recBrowserMsgTitle", recBrowserMsgTitle);
    insertMessageInPage(xmlDocument, "recBrowserMsgText", recBrowserMsgText);

    if (showGSignInButtonDemo || !signInProvider.isUnsatisfied()) {
      String link = "<span class=\"LabelText Login_LabelText\">"
          + Utility.messageBD(cp, "OBUIAPP_SignIn", vars.getLanguage()) + "</span>";
      if (signInProvider.isUnsatisfied()) {
        // if there is no external sign in provider, show Google Sign In icon with demo purposes
        String lang = OBDal.getInstance().get(Client.class, "0").getLanguage().getLanguage();
        String message = "";
        if (ak.isActive()) {
          message = Utility.messageBD(cp, "OBUIAPP_gSignInButtonDemoProfessional", lang);
        } else {
          message = Utility.messageBD(cp, "OBUIAPP_ActivateMessage", lang);
          message = message.replace("%0",
              Utility.messageBD(cp, "OBUIAPP_gSignInButtonDemoCommunity", lang));
        }
        message = message.replaceAll("&quot;", "\"")
            .replaceAll("\"", "\\\\\"")
            .replaceAll("'", "´");

        link += "<style type=\"text/css\">" //
            + "  .gSignInButtonDemo {" //
            + "    display: inline-block;" //
            + "    background-color: #dd4b39;" //
            + "    color: white;" //
            + "    width: 24px;" //
            + "    border-radius: 2px;" //
            + "    white-space: nowrap;" //
            + "    border: 1px solid #d9d9d9;" //
            + "  }" //
            + "  .gSignInButtonDemo:hover," //
            + "  .gSignInButtonDemo:active {" //
            + "    border-color: #c0c0c0;" //
            + "    box-shadow: 0 1px 0 rgba(0, 0, 0, 0.10);" //
            + "    cursor: hand;" //
            + "  }" //
            + "  .gSignInButtonDemo:hover {" //
            + "    background-color: #e74b37;" //
            + "  }" //
            + "  .gSignInButtonDemo:active {" //
            + "    background-color: #be3e2e;" //
            + "  }" //
            + "  .gSignInButtonDemo > span {" //
            + "    background: url('../web/images/gSignInButtonDemo.png') 2px 2px;" //
            + "    height: 24px;" //
            + "    width: 24px;" //
            + "    margin-top: -1px;" //
            + "    display: inline-block;" //
            + "    vertical-align: middle;" //
            + "  }" //
            + "</style>" //
            + "&nbsp;&nbsp;<div id=\"gSignInButtonDemo\" class=\"gSignInButtonDemo\" onclick='setLoginMessage(\"Error\", null, \""
            + message + "\")'>" //
            + "  <span title=\""
            + Utility.messageBD(cp, "OBUIAPP_gSignInButtonDemoAltMsg", vars.getLanguage()) //
            + "\"></span>" //
            + "</div>";
      } else {
        // a module is providing a different sign in: including its HTML code in Log In page
        for (SignInProvider cSignInProvider : signInProvider) {
          link += "&nbsp;";
          link += cSignInProvider.getLoginPageSignInHTMLCode();
        }
      }
      xmlDocument.setParameter("sign-in", link);
    }

    OBError error = (OBError) vars.getSessionObject("LoginErrorMsg");
    if (error != null) {
      vars.removeSessionValue("LoginErrorMsg");
      xmlDocument.setParameter("errorMsgStyle", ""); // clear style
      xmlDocument.setParameter("errorMsgTitle", error.getTitle());
      xmlDocument.setParameter("errorMsgContent", error.getMessage());
    }

    response.setContentType("text/html; charset=UTF-8");
    PrintWriter out = response.getWriter();
    out.println(xmlDocument.print());
    out.close();
  }

  private void insertMessageInPage(XmlDocument document, String parameterName, String message) {
    String messageFinal = String.format("var %s = \"%s\"", parameterName, message);
    document.setParameter(parameterName, messageFinal.replaceAll("\\n", "\n"));
  }
}
