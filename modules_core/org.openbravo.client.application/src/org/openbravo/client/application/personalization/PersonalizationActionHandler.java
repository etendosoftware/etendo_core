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
package org.openbravo.client.application.personalization;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.UIPersonalization;
import org.openbravo.client.application.window.OBViewFieldHandler;
import org.openbravo.client.application.window.OBViewFormComponent;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Tab;

/**
 * Is the entry point for the UI to store and retrieve personalization settings and form definition.
 * 
 * @author mtaal
 */
@RequestScoped
public class PersonalizationActionHandler extends BaseActionHandler {

  private static final String TEMPLATE_ID = "FF808181317B723B01317B7453E90006";

  private static final String ACTION = "action";
  private static final String ACTION_STORE = "store";
  private static final String ACTION_DELETE = "delete";
  private static final String ACTION_FORM = "getFormDefinition";
  private static final String PERSONALIZATIONID = "personalizationId";
  private static final String TARGET = "target";
  private static final String CLIENTID = "clientId";
  private static final String ORGID = "orgId";
  private static final String ROLEID = "roleId";
  private static final String USERID = "userId";
  private static final String TABID = "tabId";
  private static final String WINDOWID = "windowId";
  private static final String APPLYLEVELINFORMATION = "applyLevelInformation";

  @Inject
  private PersonalizationHandler personalizationHandler;

  @Inject
  private OBViewFormComponent viewFormComponent;

  @Inject
  private OBViewFieldHandler fieldHandler;

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {

    try {
      OBContext.setAdminMode(false);
      if (!parameters.containsKey(ACTION)) {
        throw new IllegalStateException("Mandatory parameter " + ACTION + " not present");
      }
      if (!parameters.containsKey(PERSONALIZATIONID) && !parameters.containsKey(TABID)
          && !parameters.containsKey(WINDOWID)) {
        throw new IllegalStateException(
            "Mandatory parameter " + TABID + "/" + WINDOWID + " not present");
      }
      final String action = (String) parameters.get(ACTION);
      final String tabId = (String) parameters.get(TABID);
      final String windowId = (String) parameters.get(WINDOWID);
      final String applyLevelInformation = (String) parameters.get(APPLYLEVELINFORMATION);
      Boolean saveAsNewPreference = false;
      String personalizationID = (String) parameters.get(PERSONALIZATIONID);
      if (action.equals(ACTION_DELETE)) {
        final UIPersonalization uiPersonalization = OBDal.getInstance()
            .get(UIPersonalization.class, personalizationID);
        if (uiPersonalization != null) {
          // is null if already removed
          OBDal.getInstance().remove(uiPersonalization);
          // Delete also all the preferences that has this uiPersonalization as the 'Default View'
          //@formatter:off
          String hql = 
                  " as p " +
                  "  where p.searchKey = :uiPersonalization " +
                  "    and p.property = :property ";
          //@formatter:on
          OBQuery<Preference> qPref = OBDal.getInstance()
              .createQuery(Preference.class, hql)
              .setNamedParameter("uiPersonalization", uiPersonalization.getId())
              .setNamedParameter("property", "OBUIAPP_DefaultSavedView");
          List<Preference> preferences = qPref.list();
          for (Preference preference : preferences) {
            OBDal.getInstance().remove(preference);
          }
        }

        return new JSONObject().put("result", "success");
      } else if (action.equals(ACTION_STORE)) {

        String clientID = (String) parameters.get(CLIENTID);
        String orgID = (String) parameters.get(ORGID);
        String roleID = (String) parameters.get(ROLEID);
        String userID = (String) parameters.get(USERID);

        if ("false".equals(applyLevelInformation) && personalizationID != null) {
          // If we don't have to apply the level information and the personalization is being
          // updated, use the original level information
          final UIPersonalization uiPersonalization = OBDal.getInstance()
              .get(UIPersonalization.class, personalizationID);
          clientID = uiPersonalization.getVisibleAtClient() != null
              ? uiPersonalization.getVisibleAtClient().getId()
              : null;
          orgID = uiPersonalization.getVisibleAtOrganization() != null
              ? uiPersonalization.getVisibleAtOrganization().getId()
              : null;
          roleID = uiPersonalization.getVisibleAtRole() != null
              ? uiPersonalization.getVisibleAtRole().getId()
              : null;
          userID = uiPersonalization.getUser() != null ? uiPersonalization.getUser().getId() : null;
        }

        if (clientID == null || orgID == null || roleID == null || userID == null) {
          // the personalization is a global personalization. do not update it. create a new
          // personalization for the user
          saveAsNewPreference = true;
        }

        final UIPersonalization uiPersonalization = personalizationHandler.storePersonalization(
            personalizationID, clientID, orgID, roleID, userID, tabId, windowId,
            (String) parameters.get(TARGET), data, saveAsNewPreference);
        final JSONObject result = new JSONObject();
        result.put(PERSONALIZATIONID, uiPersonalization.getId());
        return result;
      } else if (action.equals(ACTION_FORM)) {
        viewFormComponent.setParameters(parameters);
        viewFormComponent.setTemplateId(TEMPLATE_ID);
        fieldHandler.setTab(OBDal.getInstance().get(Tab.class, tabId));
        viewFormComponent.setFieldHandler(fieldHandler);
        final String formJS = viewFormComponent.generate();
        final JSONObject formProps = new JSONObject(formJS);
        return formProps;
      } else {
        throw new IllegalStateException("Action " + parameters.get(ACTION) + " not supported");
      }
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      throw new OBException(e);
    } finally {
      OBDal.getInstance().flush();
      OBContext.restorePreviousMode();
    }
  }
}
