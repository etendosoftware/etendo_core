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
 * All portions are Copyright (C) 2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.event;

import java.util.Arrays;

import javax.enterprise.event.Observes;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.TreeData;
import org.openbravo.model.ad.ui.Menu;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Event Handler for AD_MENU.
 * 
 * It updates the "Included In Reduced Translation" flag for any child menu entry.
 */
class ADMenuEventHandler extends EntityPersistenceEventObserver {
  private static final Entity[] ENTITIES = {
      ModelProvider.getInstance().getEntity(Menu.ENTITY_NAME) };
  private static final String MENU_TREE_ID = "10";

  @Override
  protected Entity[] getObservedEntities() {
    return ENTITIES;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    updateTranslationStrategyForChildEntries(event);
  }

  private void updateTranslationStrategyForChildEntries(EntityUpdateEvent event) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    final String menuId = (String) event.getTargetInstance().getId();
    final Entity menuEntity = ModelProvider.getInstance().getEntity(Menu.ENTITY_NAME);
    final Property translationStrategyProperty = menuEntity
        .getProperty(Menu.PROPERTY_TRANSLATIONSTRATEGY);
    final String currentValueTranslationStrategy = (String) event
        .getCurrentState(translationStrategyProperty);
    final String previousValueTranslationStrategy = (String) event
        .getPreviousState(translationStrategyProperty);
    if (!StringUtils.equals(previousValueTranslationStrategy, currentValueTranslationStrategy)) {
      try {
        Arrays.stream(TreeData.select(conn, MENU_TREE_ID, menuId))
            .filter(node -> !menuId.equals(node.id))
            .map(node -> OBDal.getInstance().get(Menu.class, node.id))
            .filter(menuEntry -> menuEntry.getModule().isInDevelopment() != null
                && menuEntry.getModule().isInDevelopment())
            .forEach(
                menuEntry -> menuEntry.setTranslationStrategy(currentValueTranslationStrategy));
      } catch (ServletException e) {
        Menu menu = OBDal.getInstance().get(Menu.class, menuId);
        throw new OBException(
            "Error while updating Translation Strategy for Menu: " + menu.getName(), e);
      }
    }
  }
}
