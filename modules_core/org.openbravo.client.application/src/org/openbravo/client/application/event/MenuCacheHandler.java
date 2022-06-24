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
import javax.inject.Inject;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.GlobalMenu;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;

/**
 * EntityPersistenceEventObserver to listen to modifications in entities that are used to compose
 * the menu. In case they are modified cached global menu is invalidated to generate it again.
 * 
 * @see GlobalMenu
 * 
 * @author alostale
 * 
 */
class MenuCacheHandler extends EntityPersistenceEventObserver {
  @Inject
  private GlobalMenu menu;

  private static final String MENU_TABLE_ID = "116";
  private static final String TREENODE_TABLE_ID = "289";

  private static final String WINDOW_TABLE_ID = "105";
  private static final String VIEWDEFINITION_TABLE_ID = "79127717F4514B459D9014C91E793CE9";
  private static final String FORM_TABLE_ID = "376";
  private static final String PROCESS_TABLE_ID = "284";
  private static final String PROCESSDEFINITION_TABLE_ID = "FF80818132D7FB620132D8129D1A0028";

  private static final String WINDOW_ACCESS_TABLE_ID = "201";
  private static final String VIEWDEFINITION_ACCESS_TABLE_ID = "E6F29F8A30BC4603B1D1195051C4F3A6";
  private static final String FORM_ACCESS_TABLE_ID = "378";
  private static final String PROCESS_ACCESS_TABLE_ID = "197";
  private static final String PROCESSDEFINITION_ACCESS_TABLE_ID = "FF80818132D85DB50132D860924E0004";

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntityByTableId(MENU_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(TREENODE_TABLE_ID),

      ModelProvider.getInstance().getEntityByTableId(WINDOW_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(VIEWDEFINITION_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(FORM_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(PROCESS_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(PROCESSDEFINITION_TABLE_ID),

      ModelProvider.getInstance().getEntityByTableId(WINDOW_ACCESS_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(VIEWDEFINITION_ACCESS_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(FORM_ACCESS_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(PROCESS_ACCESS_TABLE_ID),
      ModelProvider.getInstance().getEntityByTableId(PROCESSDEFINITION_ACCESS_TABLE_ID) };

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    menu.invalidateCache();
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    menu.invalidateCache();
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    menu.invalidateCache();
  }

  @Override
  protected Entity[] getObservedEntities() {

    return entities;
  }

}
