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
package org.openbravo.event;

import javax.enterprise.event.Observes;

import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.ApplicationConstants;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Event Handler for AD_Table
 * 
 * @author shankar
 * 
 */
class ADTableEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Table.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkClassNameForDuplicates(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    checkClassNameForDuplicates(event);
  }

  private void checkClassNameForDuplicates(EntityPersistenceEvent event) {
    ConnectionProvider conn = new DalConnectionProvider(false);
    String language = OBContext.getOBContext().getLanguage().getLanguage();
    String tableId = (String) event.getTargetInstance().getId();
    final Entity tableEntity = ModelProvider.getInstance().getEntity(Table.ENTITY_NAME);
    final Property javaClassNameProperty = tableEntity.getProperty(Table.PROPERTY_JAVACLASSNAME);
    Object javaClassName = event.getCurrentState(javaClassNameProperty);
    final Property packageNameProperty = tableEntity.getProperty(Table.PROPERTY_DATAPACKAGE);
    Object packageName = event.getCurrentState(packageNameProperty);
    final Property dataOriginTypeProperty = tableEntity.getProperty(Table.PROPERTY_DATAORIGINTYPE);
    Object dataOriginType = event.getCurrentState(dataOriginTypeProperty);
    if (ApplicationConstants.TABLEBASEDTABLE.equals(dataOriginType) && javaClassName != null) {
      OBCriteria<Table> tableCriteria = OBDal.getInstance().createCriteria(Table.class);
      tableCriteria.add(Restrictions.eq(Table.PROPERTY_JAVACLASSNAME, javaClassName));
      tableCriteria.add(Restrictions.eq(Table.PROPERTY_DATAPACKAGE, packageName));
      tableCriteria.add(Restrictions.not(Restrictions.eq(Table.PROPERTY_ID, tableId)));
      if (tableCriteria.count() != 0) {
        throw new OBException(Utility.messageBD(conn, "DuplicateJavaClassName", language));
      }
    }
  }
}
