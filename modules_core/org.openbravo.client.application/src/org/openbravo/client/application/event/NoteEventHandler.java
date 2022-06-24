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

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.Note;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.erpCommon.utility.WindowAccessData;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Checks if the Role has access to add notes for a particular tab
 * 
 * @author shankar balachandran
 */
class NoteEventHandler extends EntityPersistenceEventObserver {
  private static Logger log = LogManager.getLogger();
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Note.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    isReadOnly();
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    isReadOnly();
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    isReadOnly();
  }

  /**
   * Checks if the note is accessible by the role
   */
  private void isReadOnly() {
    boolean isReadOnly = false;
    String disableNotesForReadOnly = "Y";
    String tabId = null;
    String roleId = null;
    String language = null;
    ConnectionProvider connection = null;
    try {
      OBContext.setAdminMode();
      try {
        disableNotesForReadOnly = Preferences.getPreferenceValue("DisableNotesForReadOnlyTabs",
            true, OBContext.getOBContext().getCurrentClient(),
            OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
            OBContext.getOBContext().getRole(), null);
      } catch (PropertyException e) {
        // if property not found, do not disable notes
        disableNotesForReadOnly = "N";
      }

      if (Preferences.YES.equals(disableNotesForReadOnly)) {
        tabId = RequestContext.get().getRequestParameter("tabId");
        roleId = OBContext.getOBContext().getRole().getId();

        if (tabId != null) {
          Tab tab = OBDal.getInstance().get(Tab.class, tabId);

          if (tab.getUIPattern().equals("RO")) {
            isReadOnly = true;
          } else {
            if (roleId != null) {
              isReadOnly = WindowAccessData.hasReadOnlyAccess(new DalConnectionProvider(false),
                  roleId, tabId);
            }

          }
        }
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      OBContext.restorePreviousMode();
    }

    if (isReadOnly) {
      language = OBContext.getOBContext().getLanguage().getLanguage();
      connection = new DalConnectionProvider(false);
      throw new OBException(
          Utility.messageBD(connection, "NotesDisabledForReadOnlyTabs", language));
    }
  }
}
