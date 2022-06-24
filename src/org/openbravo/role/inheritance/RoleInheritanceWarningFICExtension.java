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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.base.structure.InheritedAccessEnabled;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.application.window.FICExtension;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleInheritance;
import org.openbravo.model.ad.ui.Tab;

/**
 * This FICExtension is used to show a warning message to the user when editing an access which
 * belongs to a role marked as template. When this type of accesses are edited, the changes are
 * propagated to the roles which are using that template to inherit permissions. With this class,
 * the user will be warned before saving the changes.
 */
@ApplicationScoped
public class RoleInheritanceWarningFICExtension implements FICExtension {
  private static final Logger log = LogManager.getLogger();
  private final static String EDIT_MODE = "EDIT";
  private final static String NEW_MODE = "NEW";
  @Inject
  private RoleInheritanceManager manager;
  private ConcurrentMap<String, Boolean> validTabsCache = new ConcurrentHashMap<String, Boolean>();

  @Override
  public void execute(String mode, Tab tab, Map<String, JSONObject> columnValues, BaseOBObject row,
      List<String> changeEventCols, List<JSONObject> calloutMessages, List<JSONObject> attachments,
      List<String> jsExcuteCode, Map<String, Object> hiddenInputs, int noteCount,
      List<String> overwrittenAuxiliaryInputs) {
    long t = System.nanoTime();
    // check if an edit action has been done in a tab related with an access type
    if (!isValidEvent(mode, tab)) {
      log.debug("took {} ns", (System.nanoTime() - t));
      return;
    }
    Role role = getRoleOfAccess(columnValues, tab, row);
    String childRoleList = "";
    if (role != null && role.isTemplate()) {
      for (RoleInheritance inheritance : role.getADRoleInheritanceInheritFromList()) {
        if (inheritance.isActive()) {
          childRoleList += ", " + inheritance.getRole().getName();
        }
      }
      if (!StringUtils.isEmpty(childRoleList)) {
        String[] msgParam = { childRoleList.substring(1) };
        addWarningMessage(calloutMessages, "EditTemplateRoleAccess", msgParam);
      }
    }
    log.debug("took {} ns", (System.nanoTime() - t));
  }

  private boolean isValidEvent(String mode, Tab tab) {
    if (EDIT_MODE.equals(mode) || NEW_MODE.equals(mode)) {
      final String tabId = tab.getTable().getId();
      Boolean valid = validTabsCache.get(tabId);
      if (valid != null) {
        return valid;
      }

      if (!ApplicationConstants.TABLEBASEDTABLE.equals(tab.getTable().getDataOriginType())) {
        valid = Boolean.FALSE;
      } else {
        String entityClassName = ModelProvider.getInstance()
            .getEntityByTableId(tabId)
            .getClassName();
        valid = manager.existsInjector(entityClassName);
      }
      validTabsCache.put(tabId, valid);
      return valid;
    }
    return false;
  }

  private Role getRoleOfAccess(Map<String, JSONObject> columnValues, Tab tab, BaseOBObject row) {
    JSONObject roleColumn = columnValues.get("inpadRoleId");
    if (roleColumn != null && roleColumn.has("value")) {
      try {
        String roleId = (String) roleColumn.get("value");
        return OBDal.getInstance().get(Role.class, roleId);
      } catch (JSONException e) {
        log.error("Error retrieving role id in tab {}" + tab.getName(), e);
        return null;
      }
    } else {
      String entityClassName = ModelProvider.getInstance()
          .getEntityByTableId(tab.getTable().getId())
          .getClassName();
      InheritedAccessEnabled access = (InheritedAccessEnabled) row;
      return manager.getRole(access, entityClassName);
    }
  }

  private void addWarningMessage(List<JSONObject> calloutMessages, String message,
      String[] parameters) {
    try {
      JSONObject msg = new JSONObject();
      String text = OBMessageUtils.getI18NMessage(message, parameters);
      msg.put("text", text);
      msg.put("severity", "TYPE_WARNING");
      calloutMessages.add(msg);
    } catch (JSONException e) {
      log.error("Error parsing JSON Object.", e);
    }
  }
}
