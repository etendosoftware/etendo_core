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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.UIPersonalization;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.ui.Window;
import org.openbravo.service.db.DalConnectionProvider;

class WindowPersonalizationEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(UIPersonalization.ENTITY_NAME) };
  private static Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final UIPersonalization uiPersonalization = (UIPersonalization) event.getTargetInstance();
    String personalizationType = uiPersonalization.getType();
    Tab personalizationTab = uiPersonalization.getTab();
    Window personalizationWindow = uiPersonalization.getWindow();
    if ("Window".equals(personalizationType) && (personalizationWindow == null)) {
      String language = OBContext.getOBContext().getLanguage().getLanguage();
      ConnectionProvider conn = new DalConnectionProvider(false);
      throw new OBException(Utility.messageBD(conn, "OBUIAPP_WindowFieldMandatory", language));
    }
    if ("Form".equals(personalizationType) && (personalizationTab == null)) {
      String language = OBContext.getOBContext().getLanguage().getLanguage();
      ConnectionProvider conn = new DalConnectionProvider(false);
      throw new OBException(Utility.messageBD(conn, "OBUIAPP_TabFieldMandatory", language));
    }
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final UIPersonalization uiPersonalization = (UIPersonalization) event.getTargetInstance();
    deleteDefaultViewPreferences(uiPersonalization);
  }

  /**
   * Given an UIPersonalization, deletes the OBUIAPP_DefaultSavedView preferences that reference it
   */
  private void deleteDefaultViewPreferences(UIPersonalization uiPersonalization) {
    try {
      List<Preference> preferenceList = this
          .getDefaultViewPreferencesForUiPersonalization(uiPersonalization);
      // don't do a client access check, the preference to delete might belong to another client
      // (i.e. System)
      OBContext.setAdminMode(false);
      for (Preference preference : preferenceList) {
        OBDal.getInstance().remove(preference);
      }
    } catch (Exception e) {
      logger.error("Error while deleting a default view preference", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Given an UIPersonalization, returns the list of OBUIAPP_DefaultSavedView preferences that
   * reference it
   */
  private List<Preference> getDefaultViewPreferencesForUiPersonalization(
      UIPersonalization uiPersonalization) {
    OBCriteria<Preference> preferenceCriteria = OBDal.getInstance()
        .createCriteria(Preference.class);
    // filter out the preferences that do not store the default view
    preferenceCriteria
        .add(Restrictions.eq(Preference.PROPERTY_PROPERTY, "OBUIAPP_DefaultSavedView"));
    // filter out the preferences whose default view is not the one being deleted
    preferenceCriteria
        .add(Restrictions.eq(Preference.PROPERTY_SEARCHKEY, uiPersonalization.getId()));
    return preferenceCriteria.list();
  }
}
