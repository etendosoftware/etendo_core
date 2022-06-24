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
 * All portions are Copyright (C) 2011-2017 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.personalization.PersonalizationHandler;
import org.openbravo.client.application.window.ApplicationDictionaryCachedStructures;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.KernelUtils;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.ad.access.FieldAccess;
import org.openbravo.model.ad.access.TabAccess;
import org.openbravo.model.ad.ui.Field;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Computes different settings which may be user/role specific for a certain window.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class WindowSettingsActionHandler extends BaseActionHandler {
  private static final Logger log = LogManager.getLogger();
  public static final String EXTRA_CALLBACKS = "extraCallbacks";

  @Inject
  private PersonalizationHandler personalizationHandler;

  @Inject
  private ApplicationDictionaryCachedStructures adcs;

  @Inject
  @Any
  private Instance<ExtraWindowSettingsInjector> extraSettings;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    long t = System.currentTimeMillis();
    final String windowId = (String) parameters.get("windowId");
    try {
      OBContext.setAdminMode();
      final Window window = adcs.getWindow(windowId);

      final JSONObject json = new JSONObject();
      json.put("uiPattern", getUIPattern(window));
      json.put("autoSave", getBooleanPreference(window, "Autosave", true));

      try {
        json.put("personalization", personalizationHandler.getPersonalizationForWindow(window));
      } catch (Throwable e) {
        // be robust about errors in the personalization settings
        log.error("Error for window: " + window + " - role: " + OBContext.getOBContext().getRole(),
            e);
      }

      json.put("showAutoSaveConfirmation",
          getBooleanPreference(window, "ShowConfirmationDefault", false));
      json.put("tabs", getFieldLevelRoles(window));
      json.put("notAccessibleProcesses", getNotAccessibleProcesses(window));
      setExtraSettings(parameters, json);

      return json;
    } catch (Exception e) {
      log.error("Error for window: " + windowId + " - role: " + OBContext.getOBContext().getRole(),
          e);
      throw new OBException(e);
    } finally {
      OBContext.restorePreviousMode();
      // clear anything we have in session as there's no change to make faster flush
      OBDal.getInstance().getSession().clear();
      log.debug("window: {} - role: {} - took: {}ms", new Object[] { windowId,
          OBContext.getOBContext().getRole(), System.currentTimeMillis() - t });
    }
  }

  private JSONObject getUIPattern(Window window) throws ServletException, JSONException {
    final String roleId = OBContext.getOBContext().getRole().getId();
    final JSONObject jsonUIPattern = new JSONObject();
    final String windowType = window.getWindowType();
    DalConnectionProvider cp = new DalConnectionProvider(false);
    for (Tab tab : window.getADTabList()) {
      final boolean readOnlyAccess = org.openbravo.erpCommon.utility.WindowAccessData
          .hasReadOnlyAccess(cp, roleId, tab.getId());
      String uiPattern = readOnlyAccess ? "RO" : tab.getUIPattern();
      // window should be read only when is assigned with a table defined as a view
      if (!"RO".equals(uiPattern) && ("T".equals(windowType) || "M".equals(windowType))
          && tab.getTable().isView()) {
        log.warn("Tab {} is set to read only because is assigned with a table defined as a view.",
            tab);
        uiPattern = "RO";
      }
      jsonUIPattern.put(tab.getId(), uiPattern);
    }
    return jsonUIPattern;
  }

  private boolean getBooleanPreference(Window window, String preference, boolean defaultValue) {
    try {
      String prefValue = Preferences.getPreferenceValue(preference, false,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window);
      return Preferences.YES.equals(prefValue);
    } catch (PropertyException ignore) {
      return defaultValue;
    }
  }

  private JSONArray getFieldLevelRoles(Window window) throws JSONException {
    final String roleId = OBContext.getOBContext().getRole().getId();
    final JSONArray tabs = new JSONArray();

    OBQuery<TabAccess> qTabAccess = OBDal.getInstance()
        .createQuery(TabAccess.class,
            "as ta where ta.windowAccess.role.id= :roleId\n"
                + "and ta.windowAccess.window.id = :windowId\n" //
                + "and ta.windowAccess.active = true\n" //
                + "and ta.active=true");
    qTabAccess.setNamedParameter("roleId", roleId);
    qTabAccess.setNamedParameter("windowId", window.getId());

    for (TabAccess tabAccess : qTabAccess.list()) {
      Tab tab = adcs.getTab(tabAccess.getTab().getId());
      boolean tabEditable = tabAccess.isEditableField();
      final Entity entity = ModelProvider.getInstance().getEntityByTableId(tab.getTable().getId());
      final JSONObject jTab = new JSONObject();
      tabs.put(jTab);
      jTab.put("tabId", tab.getId());
      jTab.put("updatable", tabEditable);
      final JSONObject jFields = new JSONObject();
      jTab.put("fields", jFields);
      final Set<String> fields = new TreeSet<String>();
      List<Field> tabFields = tab.getADFieldList();
      for (Field field : tabFields) {
        if (!field.isReadOnly() && !field.isShownInStatusBar() && field.getColumn().isUpdatable()) {
          final Property property = KernelUtils.getProperty(entity, field);
          if (property != null) {
            fields.add(property.getName());
          }
        }
      }
      for (FieldAccess fieldAccess : tabAccess.getADFieldAccessList()) {
        if (!fieldAccess.isActive()) {
          continue;
        }
        Field field = getField(tabFields, fieldAccess.getField());
        final Property property = field != null ? KernelUtils.getProperty(entity, field) : null;
        if (property == null) {
          continue;
        }
        final String name = property.getName();
        if (fields.contains(name)) {
          jFields.put(name, fieldAccess.isEditableField());
          fields.remove(name);
        }
      }
      for (String name : fields) {
        jFields.put(name, tabEditable);
      }
    }

    return tabs;
  }

  private Field getField(List<Field> tabFields, Field field) {
    for (Field f : tabFields) {
      if (f.getId().equals(field.getId())) {
        return f;
      }
    }
    return null;
  }

  private void setExtraSettings(Map<String, Object> parameters, JSONObject json)
      throws JSONException {
    JSONObject extraSettingsJson = new JSONObject();
    JSONArray extraCallbacks = new JSONArray();

    // Add the extraSettings injected
    for (ExtraWindowSettingsInjector nextSetting : extraSettings) {
      Map<String, Object> settingsToAdd = nextSetting.doAddSetting(parameters, json);
      for (Entry<String, Object> setting : settingsToAdd.entrySet()) {
        String settingKey = setting.getKey();
        Object settingValue = setting.getValue();
        if (settingKey.equals(EXTRA_CALLBACKS)) {
          if (settingValue instanceof List) {
            for (Object callbackExtra : (List<?>) settingValue) {
              if (callbackExtra instanceof String) {
                extraCallbacks.put(callbackExtra);
              } else {
                log.warn("You are trying to set a wrong instance of extraCallbacks");
              }
            }
          } else if (settingValue instanceof String) {
            extraCallbacks.put(settingValue);
          } else {
            log.warn("You are trying to set a wrong instance of extraCallbacks");
          }
        } else {
          extraSettingsJson.put(settingKey, settingValue);
        }
      }
    }
    json.put("extraSettings", extraSettingsJson);
    json.put("extraCallbacks", extraCallbacks);
  }

  private JSONArray getNotAccessibleProcesses(Window window) throws JSONException {
    // processes can be secured in 3 ways:
    // - Secured preference is set: explicit grant is required
    // - Process is marked as requiresExplicitAccessPermission: explicit grant is required
    // - None of the above: permission is inherited from window
    boolean securedProcess = false;
    try {
      securedProcess = Preferences.YES.equals(Preferences.getPreferenceValue("SecuredProcess", true,
          OBContext.getOBContext().getCurrentClient(),
          OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
          OBContext.getOBContext().getRole(), window));
    } catch (PropertyException e) {
      // do nothing, property is not set so securedProcess is false
    }

    String restrictedProcessesQry = " as f where  tab.window.id = :window and tab.active = true and (";

    restrictedProcessesQry += "(column.oBUIAPPProcess is not null";
    if (!securedProcess) {
      // not secured, restrict only those that require explicit permission
      // subquery required to prevent inner join due to compound path check
      // (process.requiresExplicitAccessPermission)
      restrictedProcessesQry += " and exists (select 1 from OBUIAPP_Process p where p = f.column.oBUIAPPProcess and requiresExplicitAccessPermission = true) ";
    }
    restrictedProcessesQry += " and not exists (select 1 from " //
        + " OBUIAPP_Process_Access a" + " where a.obuiappProcess = f.column.oBUIAPPProcess"
        + " and a.role.id = :role and a.active=true))"//

        + " or (column.process is not null ";

    if (!securedProcess) {
      // not secured, restrict only those that require explicit permission
      // subquery required to prevent inner join due to compound path check
      // (process.requiresExplicitAccessPermission)
      restrictedProcessesQry += " and exists (select 1 from ADProcess p where p = f.column.process and requiresExplicitAccessPermission = true) ";
    }

    restrictedProcessesQry += " and not exists (select 1 from ADProcessAccess a where a.process = f.column.process and "
        + " a.role.id = :role and a.active=true))";

    restrictedProcessesQry += ")  order by f.tab";

    OBQuery<Field> q = OBDal.getInstance().createQuery(Field.class, restrictedProcessesQry);
    q.setNamedParameter("window", window.getId());
    q.setNamedParameter("role", OBContext.getOBContext().getRole().getId());

    final JSONArray processes = new JSONArray();

    Tab tab = null;
    JSONObject t;
    JSONArray ps = null;
    for (Field f : q.list()) {
      if (tab == null || !tab.getId().equals(f.getTab().getId())) {
        t = new JSONObject();
        tab = f.getTab();
        ps = new JSONArray();
        t.put("tabId", tab.getId());
        t.put("processes", ps);
        processes.put(t);
      }
      ps.put(KernelUtils.getProperty(f).getName());
    }
    return processes;
  }

}
