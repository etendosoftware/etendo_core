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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * This event handler listen to events that are fired in the TableTree table. This table is used to
 * define trees for the Tables.
 * 
 * This event handler is in charge of ensuring that each table define at most one ADTree table
 * 
 */
class TableTreeEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(TableTree.ENTITY_NAME) };
  private static final String ADTREE_STRUCTURE = "ADTree";

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Property treeStructureProperty = entities[0].getProperty(TableTree.PROPERTY_TREESTRUCTURE);
    Property tableProperty = entities[0].getProperty(TableTree.PROPERTY_TABLE);
    String treeStructureValue = (String) event.getCurrentState(treeStructureProperty);
    Table tableValue = (Table) event.getCurrentState(tableProperty);
    checkTreeStructure(tableValue, treeStructureValue, null);
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    Property treeStructureProperty = entities[0].getProperty(TableTree.PROPERTY_TREESTRUCTURE);
    Property tableProperty = entities[0].getProperty(TableTree.PROPERTY_TABLE);
    String treeStructureValue = (String) event.getCurrentState(treeStructureProperty);
    String recordId = event.getId();
    Table tableValue = (Table) event.getCurrentState(tableProperty);
    checkTreeStructure(tableValue, treeStructureValue, recordId);
  }

  /**
   * Checks that no other ADTree structured tree exists for this table, throws an exception if this
   * occurs
   * 
   * @param table
   *          table being checked
   * @param treeStructure
   *          treestructure of the added/updated tree
   * @param recordId
   *          null if a new record is being created or id of the record being modified
   */
  private void checkTreeStructure(Table table, String treeStructure, String recordId) {
    if (ADTREE_STRUCTURE.equals(treeStructure)) {
      // Check that there is no other ADTree Defined for this table
      OBCriteria<TableTree> obq = OBDal.getInstance().createCriteria(TableTree.class);
      obq.add(Restrictions.eq(TableTree.PROPERTY_TABLE, table));
      obq.add(Restrictions.eq(TableTree.PROPERTY_TREESTRUCTURE, treeStructure));
      if (recordId != null) {
        obq.add(Restrictions.ne(TableTree.PROPERTY_ID, recordId));
      }
      if (obq.count() > 0) {
        String language = OBContext.getOBContext().getLanguage().getLanguage();
        ConnectionProvider conn = new DalConnectionProvider(false);
        throw new OBException(Utility.messageBD(conn, "OBUIAPP_OneADTreePerTable", language));
      }
    }
  }
}
