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
 * All portions are Copyright (C) 2015 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.role.inheritance;

import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.access.Role;
import org.openbravo.role.inheritance.RoleInheritanceManager.CalculationResult;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

/**
 * Handler for the Recalculate Permissions process which is intended to launch that process to one
 * or multiple roles
 */
public class RecalculatePermissionsHandler extends BaseActionHandler {
  final static private Logger log = LogManager.getLogger();
  @Inject
  private RoleInheritanceManager manager;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String content) {
    JSONObject response = new JSONObject();
    try {
      final JSONObject request = new JSONObject(content);
      final JSONArray roleIds = request.getJSONArray("roles");
      JSONObject message;

      // Recalculate permissions for the selected role, if the role is a template the
      // changes will be propagated
      if (roleIds.length() == 1) {
        message = recalculateForSingleRole(roleIds.getString(0));
      } else if (roleIds.length() > 1) {
        message = recalculateForMultipleRoles(roleIds);
      } else {
        message = new JSONObject();
        message.put("severity", "error");
        message.put("title", "Error");
        message.put("text", "NoRoleSelected");
      }

      // Create success message
      response.put("message", message);

    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Error recalculating permissions", e);
      Throwable ex = DbUtility.getUnderlyingSQLException(e);
      String textMessage = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      try {
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", "Error");
        errorMessage.put("text", textMessage);
        response.put("message", errorMessage);
      } catch (JSONException ignore) {
      }
    }
    return response;
  }

  private JSONObject recalculateForSingleRole(String roleId) throws JSONException {
    final Role role = OBDal.getInstance().get(Role.class, roleId);
    Map<String, CalculationResult> accessCount = manager.recalculateAllAccessesForRole(role, true);
    String text = composeAccessMessageText(accessCount);
    if (StringUtils.isEmpty(text)) {
      text = Utility.messageBD(new DalConnectionProvider(false), "PermissionsNotModified",
          OBContext.getOBContext().getLanguage().getLanguage());
    }
    String msgParam[] = { role.getName() };
    String title = OBMessageUtils.getI18NMessage("RecalculatePermissionsSuccess", msgParam);
    JSONObject message = new JSONObject();
    message.put("severity", "success");
    message.put("title", title);
    message.put("text", text);
    return message;
  }

  private JSONObject recalculateForMultipleRoles(JSONArray roleIds) throws JSONException {
    String roleNames = "";
    for (int i = 0; i < roleIds.length(); i++) {
      final Role role = OBDal.getInstance().get(Role.class, roleIds.getString(i));
      manager.recalculateAllAccessesForRole(role, true);
      OBDal.getInstance().commitAndClose();
      roleNames += ", " + role.getName();
    }
    String msgParam[] = { roleNames.substring(1) };
    String title = Utility.messageBD(new DalConnectionProvider(false),
        "RecalculatePermissionsMultipleSuccess",
        OBContext.getOBContext().getLanguage().getLanguage());
    String text = OBMessageUtils.getI18NMessage("RecalculatePermissionsMultipleRoles", msgParam);
    JSONObject message = new JSONObject();
    message.put("severity", "success");
    message.put("title", title);
    message.put("text", text);
    return message;
  }

  private String composeAccessMessageText(Map<String, CalculationResult> map) {
    String text = "";
    try {
      for (String className : map.keySet()) {
        CalculationResult counters = map.get(className);
        if (counters.getUpdated() > 0 || counters.getCreated() > 0) {
          Class<?> myClass = Class.forName(className);
          Entity entity = ModelProvider.getInstance().getEntity(myClass);
          String[] params = { counters.getCreated() + "" };
          text += OBMessageUtils.getI18NMessage(entity.getName() + "_PermissionsCount", params)
              + " ";
        }
      }
    } catch (Exception ex) {
      log.error("Error creating the text for the returned message", ex);
    }
    return text;
  }
}
