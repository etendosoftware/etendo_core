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
 * All portions are Copyright (C) 2014 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.advpaymentmngt.actionHandler;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.Parameter;
import org.openbravo.client.application.ParameterUtils;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.service.db.DbUtility;

public class AddPaymentDisplayLogicActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected final JSONObject execute(Map<String, Object> parameters, String data) {
    JSONObject result = new JSONObject();
    JSONObject values = new JSONObject();
    JSONObject errorMessage = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      final JSONObject jsonData = new JSONObject(data);
      final JSONArray affectedParams = jsonData.getJSONArray("affectedParams");
      final JSONObject params = jsonData.getJSONObject("params");

      @SuppressWarnings("unchecked")
      Iterator<String> keys = params.keys();
      while (keys.hasNext()) {
        String key = keys.next();
        Object value = params.get(key);
        parameters.put(key, value);
      }

      for (int i = 0; i < affectedParams.length(); i++) {
        Parameter param = OBDal.getInstance().get(Parameter.class, affectedParams.getString(i));
        Object defaultValue = null;
        Map<String, String> requestMap = fixRequestMap(parameters);
        requestMap.put("currentParam", param.getDBColumnName());
        defaultValue = ParameterUtils.getJSExpressionResult(requestMap,
            (HttpSession) parameters.get(KernelConstants.HTTP_SESSION), param.getDefaultValue());
        values.put(param.getDBColumnName(), defaultValue);
      }
      result.put("values", values);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error executing AddPaymentDisplayLogicActionHandler", e);
      try {
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", message);
        result.put("message", errorMessage);
      } catch (Exception e2) {
        log.error(
            "Error happend when building error messsage for AddPaymentDisplayLogicActionHandler",
            e2);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  /*
   * The request map is <String, Object> because includes the HTTP request and HTTP session, is not
   * required to handle process parameters
   */
  private Map<String, String> fixRequestMap(Map<String, Object> parameters) {
    final Map<String, String> retval = new HashMap<String, String>();
    for (Entry<String, Object> entries : parameters.entrySet()) {
      if (entries.getKey().equals(KernelConstants.HTTP_REQUEST)
          || entries.getKey().equals(KernelConstants.HTTP_SESSION)) {
        continue;
      }
      retval.put(entries.getKey(), entries.getValue().toString());
    }
    return retval;
  }
}
