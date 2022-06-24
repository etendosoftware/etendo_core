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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.expression.OBScriptEngine;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.domaintype.BasePrimitiveDomainType;
import org.openbravo.base.model.domaintype.BigDecimalDomainType;
import org.openbravo.base.model.domaintype.BooleanDomainType;
import org.openbravo.base.model.domaintype.DateDomainType;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.ForeignKeyDomainType;
import org.openbravo.base.model.domaintype.LongDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.util.Check;
import org.openbravo.client.kernel.reference.UIDefinition;
import org.openbravo.client.kernel.reference.UIDefinitionController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.data.Sqlc;
import org.openbravo.model.ad.domain.Reference;

/**
 * Utility class for Parameters handling
 * 
 * @author iperdomo
 */
public class ParameterUtils {

  private static Logger log = LogManager.getLogger();

  public static void setParameterValue(ParameterValue parameterValue, JSONObject requestValue) {
    try {
      String value = null;
      if (!requestValue.isNull("value")) {
        value = requestValue.getString("value");
      }
      setValue(parameterValue, value);
    } catch (Exception e) {
      log.error(
          "Error trying to set value for paramter: " + parameterValue.getParameter().getName(), e);
    }
  }

  public static void setDefaultParameterValue(ParameterValue value) {
    Check.isNotNull(value, "Default value is based on Parameter defintion");
    setValue(value, value.getParameter().getDefaultValue());
  }

  private static void setValue(ParameterValue parameterValue, String stringValue) {
    if (StringUtils.isEmpty(stringValue)) {
      parameterValue.setValueString(null);
      parameterValue.setValueDate(null);
      parameterValue.setValueNumber(null);
      return;
    }
    DomainType domainType = getParameterDomainType(parameterValue.getParameter());
    try {
      if (domainType.getClass().equals(StringDomainType.class)) {
        parameterValue.setValueString(stringValue);
      } else if (domainType.getClass().equals(DateDomainType.class)) {
        DateDomainType dateDomainType = (DateDomainType) domainType;
        Date date = (Date) dateDomainType.createFromString(stringValue);
        parameterValue.setValueDate(date);
      } else if (domainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)
          || domainType.getClass().equals(LongDomainType.class)) {
        parameterValue.setValueNumber(new BigDecimal(stringValue));
      } else { // default
        parameterValue.setValueString(stringValue);
      }
    } catch (Exception e) {
      log.error(
          "Error trying to set value for paramter: " + parameterValue.getParameter().getName(), e);
    }
  }

  public static DomainType getParameterDomainType(Parameter parameter) {
    String referenceId = parameter.getReference().getId();
    return ModelProvider.getInstance().getReference(referenceId).getDomainType();
  }

  /**
   * Returns an Object with the Value of the Parameter Value. This object can be a String, a
   * java.util.Date, long, boolean or a BigDecimal.
   * 
   * @param parameterValue
   *          the Parameter Value we want to get the Value from.
   * @return the Value of the Parameter Value.
   */
  public static Object getParameterValue(ParameterValue parameterValue) {
    DomainType domainType = getParameterDomainType(parameterValue.getParameter());
    if (domainType.getClass().equals(StringDomainType.class)) {
      return parameterValue.getValueString();
    } else if (domainType.getClass().equals(DateDomainType.class)) {
      return parameterValue.getValueDate();
    } else if (domainType.getClass().getSuperclass().equals(BigDecimalDomainType.class)) {
      return parameterValue.getValueNumber();
    } else if (domainType.getClass().equals(LongDomainType.class)) {
      return parameterValue.getValueNumber() != null ? parameterValue.getValueNumber().longValue()
          : null;
    } else if (domainType.getClass().equals(BooleanDomainType.class)) {
      return "true".equals(parameterValue.getValueString());
    } else { // default
      return parameterValue.getValueString();
    }
  }

  /**
   * Returns the String representation of the Value.
   * 
   * @param parameterValue
   *          the Parameter Value we want to get the Value from.
   * @return the Value of the Parameter Value.
   */
  public static String getParameterStringValue(ParameterValue parameterValue) {
    DomainType domainType = getParameterDomainType(parameterValue.getParameter());
    if (domainType instanceof BasePrimitiveDomainType) {
      BasePrimitiveDomainType basePrimitive = (BasePrimitiveDomainType) domainType;
      return basePrimitive.convertToString(getParameterValue(parameterValue));
    }

    // Non primitive domain types are Foreign Keys and Lists. Value returned as String
    return parameterValue.getValueString();
  }

  /**
   * Returns the Fixed value of the given parameter. If the value is a JS expression it returns the
   * result of the expression based on the parameters passed in from the request.
   * 
   * @param parameters
   *          the parameters passed in from the request
   * @param parameter
   *          the parameter we want to get the Fixed Value from
   * @return the Fixed Value of the parameter
   */
  public static Object getParameterFixedValue(Map<String, String> parameters, Parameter parameter) {
    if (parameter.isEvaluateFixedValue()) {
      try {
        return getJSExpressionResult(parameters, null, parameter.getFixedValue());
      } catch (Exception e) {
        // log.error(e.getMessage(), e);
        return null;
      }
    } else {
      return parameter.getFixedValue();
    }
  }

  /**
   * Returns the default value of the given parameter based on the request information.
   * 
   * @param parameters
   *          the parameters passed in from the request
   * @param parameter
   *          the parameter to get the Default Value from
   * @param session
   *          the HttpSession of the request
   * @param _context
   *          the JSONObject with the context information of the request.
   * @return the DefaultValue of the Parameter.
   * @throws ScriptException
   *           Error occurred executing the script to calculate the defaultValue of the parameter
   */
  public static Object getParameterDefaultValue(Map<String, String> parameters, Parameter parameter,
      HttpSession session, JSONObject _context) throws ScriptException {
    JSONObject context = _context != null ? _context : new JSONObject();
    Reference reference = parameter.getReferenceSearchKey();
    if (reference == null) {
      reference = parameter.getReference();
    }

    UIDefinition uiDefinition = UIDefinitionController.getInstance().getUIDefinition(reference);

    String rawDefaultValue = parameter.getDefaultValue();

    Object defaultValue = null;
    if (isSessionDefaultValue(rawDefaultValue)) {
      // Transforms the default value from @columnName@ to the column inp name
      String inpName = "inp"
          + Sqlc.TransformaNombreColumna(getDependentDefaultValue(rawDefaultValue));
      try {
        defaultValue = context.get(inpName);
      } catch (JSONException e) {
        log.error("The value \"" + inpName + "\" does not exist in context", e);
      }
    } else {
      parameters.put("currentParam", parameter.getDBColumnName());
      defaultValue = getJSExpressionResult(parameters, session, rawDefaultValue);
    }
    String inpName = "inp" + Sqlc.TransformaNombreColumna(parameter.getDBColumnName());
    if (!context.has(inpName)) {
      try {
        context.put(inpName, defaultValue);
      } catch (JSONException ignore) {
      }
    }

    DomainType domainType = uiDefinition.getDomainType();
    if (defaultValue != null && defaultValue instanceof String
        && domainType instanceof ForeignKeyDomainType) {
      // default value is ID of a FK, look for the identifier
      Entity referencedEntity = ((ForeignKeyDomainType) domainType)
          .getForeignKeyColumn(parameter.getDBColumnName())
          .getProperty()
          .getEntity();

      BaseOBObject record = OBDal.getInstance().get(referencedEntity.getName(), defaultValue);
      if (record != null) {
        String identifier = record.getIdentifier();
        JSONObject def = new JSONObject();
        try {
          def.put("value", defaultValue);
          def.put("identifier", identifier);
        } catch (JSONException ignore) {
        }
        return def;
      } else {
        return null;
      }
    } else if (defaultValue != null && domainType instanceof BooleanDomainType) {
      defaultValue = ((BooleanDomainType) domainType).createFromString((String) defaultValue);
    }
    return defaultValue;
  }

  /**
   * Returns the result of evaluating the given JavaScript expression.
   * 
   * @param parameters
   *          Map of Strings with the request map parameters.
   * @param session
   *          optional HttpSession object.
   * @param expression
   *          String with the JavaScript expression to be evaluated.
   * @return an Object with the result of the expression evaluation.
   * @throws ScriptException
   *           Error occurred in the script execution
   */
  @SuppressWarnings("rawtypes")
  public static Object getJSExpressionResult(Map<String, String> parameters, HttpSession session,
      String expression) throws ScriptException {

    Map<String, Object> bindings = new HashMap<>();
    if (session != null) {
      bindings.put("OB", new OBBindings(OBContext.getOBContext(), parameters, session));
    } else {
      bindings.put("OB", new OBBindings(OBContext.getOBContext(), parameters));
    }

    Object result = OBScriptEngine.getInstance().eval(expression, bindings);
    if (result instanceof Map) {
      // complex js object, convert it into a JSON
      result = new JSONObject((Map) result);
    }
    return result;
  }

  /**
   * Returns if a default value is a session value.
   * 
   * @param rawDefaultValue
   *          value to check if is session value.
   * @return Returns true if the value of the parameter default value matches "@*@"
   */
  private static boolean isSessionDefaultValue(String rawDefaultValue) {
    if ("@".equals(rawDefaultValue.substring(0, 1))
        && "@".equals(rawDefaultValue.substring(rawDefaultValue.length() - 1))
        && rawDefaultValue.length() > 2) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns a Map&lt;String, String&gt; with all parameters in the servlet request.
   * 
   * @param request
   *          request taken in the servlet.
   * @return a Map with all parameters in request.
   */
  public static Map<String, String> buildRequestMap(HttpServletRequest request) {
    final Map<String, String> parameterMap = new HashMap<String, String>();
    for (Enumeration<?> keys = request.getParameterNames(); keys.hasMoreElements();) {
      final String key = (String) keys.nextElement();
      if (request.getParameterValues(key) != null && request.getParameterValues(key).length > 1) {
        parameterMap.put(key, request.getParameterValues(key).toString());
      } else {
        parameterMap.put(key, request.getParameter(key).toString());
      }
    }
    return parameterMap;
  }

  /**
   * Removes the leading and preceding '@' from a default value
   * 
   * @param rawDefaultValue
   *          defaultValue surrounded by '@', i.e. '@AD_USER_ID@'
   * @return the rawDefaultValue, after removing the first and the last characters
   */
  private static String getDependentDefaultValue(String rawDefaultValue) {
    return rawDefaultValue.substring(1, rawDefaultValue.length() - 1);
  }
}
