
<%@ page import="org.openbravo.dal.core.OBContext"%>
<%@ page import="org.openbravo.base.weld.WeldUtils"%>
<%@ page import="org.openbravo.authentication.AuthenticationManager" %>
<%@ page import="org.openbravo.client.kernel.KernelUtils" %>
<%@ page import="org.openbravo.client.kernel.KernelConstants" %>
<%@ page import="org.openbravo.client.kernel.StaticResourceProvider" %>
<%@ page import="org.apache.log4j.Logger" %>
<%@ page import="org.openbravo.model.ad.access.Role" %>
<%@ page import="org.openbravo.model.ad.access.User" %>
<%@ page import="org.openbravo.dal.service.OBDal" %>
<%@ page import="org.openbravo.base.secureApp.VariablesSecureApp" %>
<%@ page import="org.openbravo.erpCommon.obps.ActivationKey" %>
<%@ page import="org.openbravo.base.secureApp.LoginHandler" %>
<%@ page import="org.openbravo.erpCommon.utility.OBMessageUtils" %>
<%@ page import="org.openbravo.erpCommon.utility.OBError" %>
<%@ page import="org.openbravo.erpCommon.obps.ActivationKey.LicenseRestriction" %>
<%@ page import="org.openbravo.client.application.window.ApplicationDictionaryCachedStructures"%>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page session="false" %>
<%
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
 * All portions are Copyright (C) 2011-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

Logger log = Logger.getLogger(org.openbravo.authentication.AuthenticationManager.class);

HttpSession currentSession = request.getSession(false);
boolean adSessionPresent = currentSession != null && currentSession.getAttribute("#AD_SESSION_ID") != null;

AuthenticationManager authManager = AuthenticationManager.getAuthenticationManager(this);
if (!adSessionPresent && !authManager.useExternalLoginPage()) {
  if (request.getQueryString() != null) {
    response.sendRedirect(authManager.getLoginURL(request) + "?" + request.getQueryString());
  } else {
    response.sendRedirect(authManager.getLoginURL(request));
  }
  return;
}

String userId = authManager.authenticate(request, response);

if (userId == null) {
  return;
}

OBContext.setAdminMode(false);
String sessionId = null;
try {
  sessionId = currentSession != null ? (String) currentSession.getAttribute("#AD_SESSION_ID") : null;
  if (sessionId != null && !"".equals(sessionId) && !"Y".equals(currentSession.getAttribute("forceLogin"))) {
    org.openbravo.model.ad.access.Session dbSession = OBDal.getInstance().get(org.openbravo.model.ad.access.Session.class, sessionId);
    String currentSessionType = dbSession.getLoginStatus();

    if (!ActivationKey.consumesConcurrentUser(currentSessionType) && !"CUR".equals(currentSessionType)) {
      // session was created not counting concurrent users, now switching to backend so they
      // should be counted
      dbSession.setLoginStatus(LoginHandler.SUCCESS_SESSION_STANDARD);
      OBDal.getInstance().flush();

      if (ActivationKey.getInstance().checkOPSLimitations(sessionId) == LicenseRestriction.NUMBER_OF_CONCURRENT_USERS_REACHED) {
        dbSession.setSessionActive(false);
        OBDal.getInstance().flush();

        OBError errMsg = new OBError();
        errMsg.setTitle(OBMessageUtils.messageBD("NUMBER_OF_CONCURRENT_USERS_REACHED_TITLE", false, true));
        errMsg.setMessage(OBMessageUtils.messageBD("NUMBER_OF_CONCURRENT_USERS_REACHED", false, true));
        currentSession.setAttribute("LOGINERRORMSG", errMsg);

        response.sendRedirect(authManager.getLoginURL(request));
        return;
      }
    }
  }
} catch (Exception e) {
  log.error("Error resetting login status for session "  + sessionId,e);
} finally {
  OBContext.restorePreviousMode();
}

String legacySkin = "N";
try {
  legacySkin = org.openbravo.erpCommon.businessUtility.Preferences.getPreferenceValue(
      "SKINLEG_LegacySkin", true, OBContext.getOBContext().getCurrentClient(),
      OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
      OBContext.getOBContext().getRole(), null);
} catch (Exception ignored) { }
String skinVersion = legacySkin.equals("N") ? "Default" : "Legacy";

boolean uncompSC = false;
String scDevModulePackage = "org.openbravo.userinterface.smartclient.dev";
OBContext.setAdminMode();
try {
  if (KernelUtils.getInstance().isModulePresent(scDevModulePackage)) {
    uncompSC = KernelUtils.getInstance().getModule(scDevModulePackage).isInDevelopment();
  }
} catch (Exception e) {
  log.error("Error trying to acquire module \"" + scDevModulePackage + "\": " + e.getMessage(), e);
} finally {
  OBContext.restorePreviousMode();
}

String ua = request.getHeader( "User-Agent" );
boolean isMSIE = ( ua != null && ua.indexOf( "MSIE" ) != -1 );
int verMSIE = 0;
String verMSIEtmp = "";
if (isMSIE) {
  verMSIEtmp = ua.substring(ua.indexOf("MSIE") + 5);
  verMSIEtmp = verMSIEtmp.substring(0, verMSIEtmp.indexOf("."));
  if (ua.indexOf("MSIE 7.0") != -1 && ua.indexOf("Trident/4") != -1) {
    //In case IE8 runs in "IE8 Compatibility mode, look for Trident/4.0 to know that is IE8 although MSIE string is MSIE 7.0
    verMSIEtmp = "8";
  } else if (ua.indexOf("MSIE 7.0") != -1 && ua.indexOf("Trident/5") != -1) {
    // In case IE9 runs in "IE8 Compatibility mode, look for Trident/5.0 to know that is IE9 although MSIE string is MSIE 7.0
    verMSIEtmp = "9";
  } else if (ua.indexOf("MSIE 7.0") != -1 && ua.indexOf("Trident/") != -1) {
    // For hypothetic future IE versions in case IEX runs in "IEX Compatibility mode, look for Trident/ to know that is IEX although MSIE string is MSIE 7.0
    verMSIEtmp = "10"; //If this 'if' statement is not updated, could be 10 or 11 or anything... but set 10 just to ensure it is not in IE7
  }
  verMSIE = Integer.parseInt(verMSIEtmp);
}
response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
response.addHeader("Pragma", "no-cache");
response.addHeader("Expires", "0");
%>
<!DOCTYPE html>

<html dir="<%=(OBContext.isRightToLeft() ? "RTL" : "LTR")%>">
<head>
<meta http-equiv="Expires" content="Tue, 24 Apr 1979 00:00:01 GMT"/>
<meta http-equiv="Content-type" content="text/html;charset=utf-8"/>
<meta http-equiv="Cache-Control" content="no-cache no-store must-revalidate" >
<meta http-equiv="Pragma" content="no-cache" >
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="author" content="Openbravo S.L.U.">
<meta name="keywords" content="openbravo">
<meta name="description" content="Openbravo S.L.U.">
<link rel="shortcut icon" href="./web/images/favicon.ico" />
<link rel="stylesheet" type="text/css" href="./org.openbravo.client.kernel/OBCLKER_Kernel/StyleSheetResources?_appName=OB3&_skinVersion=<%= skinVersion %>&_cssDataUri=<%=(!isMSIE || (isMSIE && verMSIE >=8))%>"/>

<title>Etendo</title>
<%
  ApplicationDictionaryCachedStructures adcs = WeldUtils.getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);
  if (!adcs.useCache()) {
%>
<script src="./web/org.openbravo.client.kernel/js/scopeleaks.min.js"></script>
<%
  }
%>
<script src="./web/org.openbravo.client.kernel/js/LAB.min.js"></script>
<script src="./web/org.openbravo.client.kernel/js/BigDecimal-all-1.0.3.min.js"></script>

<script>
  (function () {
    var b = document.documentElement;
    b.setAttribute('data-useragent',  navigator.userAgent);
    b.setAttribute('data-platform', navigator.platform );
    b.className += ((!!('ontouchstart' in window) || !!('onmsgesturechange' in window))?' touch':'');
  }());
</script>

<!-- styles used during loading -->
<style>
  html, body {
      height: 100%;
      width: 100%;
  }

  .OBCenteredBox {
      position: fixed;
      z-index: 1000000;
      top: 50%;
      left: 50%;
      margin: -25px 0 0 -150px;
      width: 300px;
      height: 50px;
  }

  .OBLoadingPromptLabel {
      font-family: 'Arial';
      font-size: 12px;
      color: #202452;
  }

  .OBLoadingPromptModalMask {
      left: 0;
      top: 0;
      width: 100%;
      height: 100%;
      background-color: #fff;
  }
</style>

</head>
<body>

<!-- shows the loading div -->
<div class="OBLoadingPromptModalMask" id="OBLoadingDiv">
    <div class="OBCenteredBox">
        <table>
            <tr>
                <td>
                    <span class="OBLoadingPromptLabel">LOADING...</span>
                </td>
                <td>
                    <img width="220" height="16" src="./web/org.openbravo.userinterface.smartclient/openbravo/skins/Default/org.openbravo.client.application/images/system/windowLoading.gif"/>
                </td>
            </tr>
        </table>
    </div>
</div>
<!-- load the rest -->
<script>
// Bootstrap error handling: Shows an alert to the user when an error occurs loading static resources
// This error handler gets removed at the end of static resources
window.onerror = function <%=KernelConstants.BOOTSTRAP_ERROR_HANDLER_NAME%> (errorMsg, url, lineNumber) {
  var msg = errorMsg + ' - ' + url + ':' + lineNumber;
  document.body.removeChild(document.getElementById('OBLoadingDiv'));
  alert(msg);
};

$LAB.setGlobalDefaults({AppendTo: 'body'});

var isomorphicDir='./web/org.openbravo.userinterface.smartclient/isomorphic/';
var isc = window.isc ? window.isc : {};

// starts the application is called as the last statement in the StaticResources part
function OBStartApplication() {
<%
//If the role has its access to the backend restricted, an error message will be shown
final VariablesSecureApp vars1 = new VariablesSecureApp(request, false);
OBContext.setAdminMode();
try{
  String roleId=vars1.getRole();
  Role role;
  if(roleId==null || roleId.equals("")){
    role=OBDal.getInstance().get(User.class, userId).getDefaultRole();
  }else{
    role = OBDal.getInstance().get(Role.class, vars1.getRole());
  }
if(role !=null && role.isRestrictbackend()){
%>
  document.body.removeChild(document.getElementById('OBLoadingDiv'));
  isc.Dialog.create({message: OB.I18N.getLabel('OBUIAPP_RestrictedUser'), title: OB.I18N.getLabel('OBUIAPP_RestrictedUserTitle'), showCloseButton: false}).show();
  return;
<%
}
boolean onlySystemAdminAccess = "Y".equals(vars1.getSessionValue("onlySystemAdminRoleShouldBeAvailableInErp"));
if (onlySystemAdminAccess && role != null && !"0".equals(role.getId())) {
  %>
  document.body.removeChild(document.getElementById('OBLoadingDiv'));
  isc.Dialog.create({message: OB.I18N.getLabel('BACKEND_LOGIN_RESTRICTED'), title: OB.I18N.getLabel('BACKEND_LOGIN_RESTRICTED_TITLE'), showCloseButton: false}).show();
  return;
<%  
}
 }finally{
  OBContext.restorePreviousMode();
}
%>
  OB.Layout.initialize();
  OB.Layout.draw();
  OB.Layout.ViewManager.createAddStartTab();
  // get rid of the loading stuff
  document.body.removeChild(document.getElementById('OBLoadingDiv'));
  OB.GlobalHiddenForm = document.forms.OBGlobalHiddenForm;
<%
  if (currentSession != null && currentSession.getAttribute("STARTUP-MESSAGE") != null) {
    String text = (String) currentSession.getAttribute("STARTUP-MESSAGE");
    String title = (String) currentSession.getAttribute("STARTUP-MESSAGE-TITLE");
    currentSession.removeAttribute("STARTUP-MESSAGE");
    currentSession.removeAttribute("STARTUP-MESSAGE-TITLE");
%>
  isc.say('<%=text%>', null, {title: '<%=title%>'});
<%
  }
%>
}
</script>
<script src="./web/org.openbravo.userinterface.smartclient/isomorphic/ISC_Combined<%=(uncompSC ? ".uncompressed" : "")%>.js"></script>
<script src="./web/org.openbravo.userinterface.smartclient/isomorphic/ISC_History<%=(uncompSC ? ".uncompressed" : "")%>.js"></script>
<%
  StaticResourceProvider resourceProvider = WeldUtils.getInstanceFromStaticBeanManager(StaticResourceProvider.class);
  final String staticResourceFileName = resourceProvider.getStaticResourceCachedFileName("OB3");
  if (staticResourceFileName != null) {
%>
<script src="./web/js/gen/<%=staticResourceFileName%>.js"></script>
<%
  } else {
%>
<script src="./org.openbravo.client.kernel/OBCLKER_Kernel/StaticResources?_appName=OB3&_skinVersion=<%= skinVersion %>"></script>
<%
  }
%>
<iframe name="background_target" id="background_target" height="0" width="0"></iframe>
<form name="OBGlobalHiddenForm" method="post" action="blank.html" target="background_target">
</form>
</body>
</html>
