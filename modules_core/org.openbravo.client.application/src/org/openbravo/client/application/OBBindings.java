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
 * All portions are Copyright (C) 2010-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.util.Check;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.reference.DateTimeUIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.json.JsonUtils;

/**
 * JS - Java binding to use in JavaScript expressions.
 * 
 * @author iperdomo
 */
public class OBBindings {

  private static final Logger log = LogManager.getLogger();

  private OBContext context;
  private Map<String, String> requestMap;
  private HttpSession httpSession;
  private SimpleDateFormat jsDateTimeFormat = null;

  public OBBindings(OBContext obContext) {
    Check.isNotNull(obContext, "The OBContext parameter cannot be null");
    context = obContext;
  }

  public OBBindings(OBContext obContext, Map<String, String> parameters) {
    Check.isNotNull(obContext, "The OBContext parameter cannot be null");
    context = obContext;
    requestMap = parameters;
  }

  public OBBindings(OBContext obContext, Map<String, String> parameters, HttpSession session) {
    Check.isNotNull(obContext, "The OBContext parameter cannot be null");
    context = obContext;

    Check.isNotNull(session, "The HttpSession parameter cannot be null");
    httpSession = session;

    requestMap = parameters;

    jsDateTimeFormat = JsonUtils.createJSTimeFormat();
  }

  public OBContext getContext() {
    return context;
  }

  public String getIdentifier(String entityName, String id) {
    return OBDal.getInstance().get(entityName, id).getIdentifier();
  }

  public Map<String, String> getParameters() {
    return requestMap;
  }

  public HttpSession getSession() {
    return httpSession;
  }

  private boolean checkRequestMap() {
    if (requestMap == null) {
      log.warn("Accessing request parameters map without initializing it");
      return false;
    }
    return true;
  }

  /**
   * Checks if is a Sales Order transaction, based on the parameters of the HTTP request
   * 
   * @return null if there is no request parameters, or if both the inpissotrx and inpwindowId
   *         request parameters are not available
   */
  public Boolean isSalesTransaction() {
    if (requestMap == null) {
      log.warn("Requesting isSOTrx check without request parameters");
      return null;
    }

    String value = requestMap.get(OBBindingsConstants.SO_TRX_PARAM);
    if (value != null) {
      return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }
    if (httpSession == null) {
      log.warn("Requesting isSOTrx check without request parameters and session");
      return null;
    }

    value = (String) httpSession.getAttribute("inpisSOTrxTab");
    if (value != null) {
      return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    String windowId = getWindowId();
    if (windowId == null) {
      return null;
    }

    value = (String) httpSession.getAttribute(windowId + "|ISSOTRX");
    if (value != null) {
      return "Y".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value);
    }

    ApplicationDictionaryCachedStructures adcs = WeldUtils
        .getInstanceFromStaticBeanManager(ApplicationDictionaryCachedStructures.class);

    Window w = adcs.getWindow(windowId);
    if (w != null) {
      return w.isSalesTransaction();
    }
    return null;
  }

  public String getWindowId() {
    if (!checkRequestMap()) {
      return null;
    }
    return requestMap.get(OBBindingsConstants.WINDOW_ID_PARAM);
  }

  public String getTabId() {
    if (!checkRequestMap()) {
      return null;
    }
    return requestMap.get(OBBindingsConstants.TAB_ID_PARAM);
  }

  public String getCommandType() {
    if (!checkRequestMap()) {
      return null;
    }
    return requestMap.get(OBBindingsConstants.COMMAND_TYPE_PARAM);
  }

  public Boolean isPosted() {
    if (!checkRequestMap()) {
      return null;
    }
    return "Y".equalsIgnoreCase(requestMap.get(OBBindingsConstants.POSTED_PARAM));
  }

  public Boolean isProcessed() {
    if (!checkRequestMap()) {
      return null;
    }
    return "Y".equalsIgnoreCase(requestMap.get(OBBindingsConstants.PROCESSED_PARAM));
  }

  public String formatDate(Date d) {
    return UIDefinitionController.DATE_UI_DEFINITION.convertToClassicString(d);
  }

  public String formatDate(Object d) {
    return formatDate(getTimeFromJavaScriptDate(d));
  }

  private Date getTimeFromJavaScriptDate(Object d) {
    // Rhino engine is used to generate a Date object from provided JS expression
    Context rhinoContext = Context.enter();
    try {
      Scriptable scope = rhinoContext.initStandardObjects();
      Function f = (Function) scope.get("getTime", scope);
      Object result = f.call(rhinoContext, scope, scope, new Object[] { d });
      long localTime = ((Double) result).longValue();
      return new Date(localTime);
    } catch (Exception ex) {
      log.error("Error getting javascript date from object {} of class {}", d,
          d.getClass().getName());
      throw new OBException(ex.getMessage(), ex);
    } finally {
      Context.exit();
    }
  }

  public String formatDateTime(Date d) {
    return ((DateTimeUIDefinition) UIDefinitionController.DATETIME_UI_DEFINITION)
        .convertToClassicStringInLocalTime(d);
  }

  public String formatDateTime(Object d) {
    return formatDateTime(getTimeFromJavaScriptDate(d));
  }

  public Date parseDate(String date) {
    try {
      return UIDefinitionController.DATE_UI_DEFINITION.parse(date);
    } catch (Exception e) {
      return null;
    }
  }

  public Date parseDateTime(String dateTime) {
    try {
      return convertToLocalTime(jsDateTimeFormat.parse(dateTime));
    } catch (Exception e) {
      try {
        return convertToLocalTime(UIDefinitionController.DATETIME_UI_DEFINITION.parse(dateTime));
      } catch (Exception ex) {
        return null;
      }
    }
  }

  private static Date convertToLocalTime(Date UTCTime) {
    Calendar localTime = Calendar.getInstance();
    localTime.setTime(UTCTime);

    int gmtMillisecondOffset = (localTime.get(Calendar.ZONE_OFFSET)
        + localTime.get(Calendar.DST_OFFSET));
    localTime.add(Calendar.MILLISECOND, gmtMillisecondOffset);

    return localTime.getTime();
  }

  public String formatDate(Date d, String format) {
    Check.isNotNull(format, "Format is a required parameter");
    SimpleDateFormat df = new SimpleDateFormat(format);
    return df.format(d);
  }

  public String formatDate(Object d, String format) {
    return formatDate(getTimeFromJavaScriptDate(d), format);
  }

  public Date parseDate(String date, String format) {
    Check.isNotNull(format, "Format is a required parameter");
    try {
      SimpleDateFormat df = new SimpleDateFormat(format);
      return df.parse(date);
    } catch (Exception e) {
      log.error("Error parsing string " + date + " with format: " + format, e);
    }
    return null;
  }

  public String getFilterExpression(String className) {
    Check.isNotNull(className, "The class name must not be null");
    FilterExpression expr;
    try {
      try {
        expr = (FilterExpression) WeldUtils
            .getInstanceFromStaticBeanManager(Class.forName(className));
      } catch (IllegalArgumentException e) {
        // try with OBClassLoader in case package is excluded by Weld
        expr = (FilterExpression) OBClassLoader.getInstance()
            .loadClass(className)
            .getDeclaredConstructor()
            .newInstance();
      }
      return expr.getExpression(requestMap);
    } catch (Exception e) {
      log.error("Error trying to get filter expression from class: " + className, e);
    }
    return "";
  }
}
