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
 * All portions are Copyright (C) 2010-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
import org.openbravo.erpCommon.utility.Utility;

/**
 * An abstract class that can be extended for those callouts that need to evaluate a Javascript
 * expression.
 * 
 * @author gorkaion
 */
public abstract class JSExpressionCallout extends SimpleCallout {
  private static final String JS_ERROR_MSG_PREFIX = "sun.org.mozilla.javascript.internal.EcmaError:";
  private static final String JS_ERROR_EVAL = "in <eval>";

  protected abstract String getExpression(VariablesSecureApp vars);

  @Override
  protected void execute(CalloutInfo info) throws ServletException {
    info.addResult("MESSAGE", "");
    String expression = getExpression(info.vars);
    if (expression.equals("")) {
      return;
    }
    try {
      HttpServletRequest request = RequestContext.get().getRequest();
      Object result = ParameterUtils.getJSExpressionResult(getParameterMap(request),
          request.getSession(false), expression);
      info.addResult("INFO", getInfoMessage(info.vars, result));
    } catch (Exception e) {
      info.addResult("WARNING", getErrorMessage(info.vars, e.getMessage()));
    }
  }

  private Map<String, String> getParameterMap(HttpServletRequest request) {
    final Map<String, String> parameterMap = new HashMap<>();
    for (Enumeration<?> keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      parameterMap.put(key, request.getParameter(key));
    }
    return parameterMap;
  }

  private String getInfoMessage(VariablesSecureApp vars, Object result) {
    return Utility.messageBD(this, "OBUIAPP_CALLOUT_JS_EXPR_RESULT", vars.getLanguage()) + " "
        + result;
  }

  private String getErrorMessage(VariablesSecureApp vars, String exceptionMessage) {
    String errorMessage;
    if (exceptionMessage.contains(JS_ERROR_MSG_PREFIX)) {
      errorMessage = exceptionMessage.substring(JS_ERROR_MSG_PREFIX.length());
    } else if (exceptionMessage.contains(JS_ERROR_EVAL)) {
      errorMessage = exceptionMessage.substring(0, exceptionMessage.indexOf(JS_ERROR_EVAL));
    } else {
      errorMessage = exceptionMessage;
    }
    return Utility.messageBD(this, "OBUIAPP_CALLOUT_JS_EXPR_ERROR", vars.getLanguage()) + " "
        + errorMessage;
  }
}
