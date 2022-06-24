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

package org.openbravo.client.application.event;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.eventSequence.EventSequence;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.order.Order;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Listens to save events on purchase and sales orders and sets the document no.
 * 
 * @see Utility#getDocumentNo(java.sql.Connection, org.openbravo.database.ConnectionProvider,
 *      org.openbravo.base.secureApp.VariablesSecureApp, String, String, String, String, boolean,
 *      boolean)
 * 
 * @author mtaal
 */
class SetDocumentNoHandler extends EntityPersistenceEventObserver {

  public void onUpdate(@Observes EntityUpdateEvent event) {
    handleEvent(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    handleEvent(event);
  }

  private void handleEvent(EntityPersistenceEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    var sequenceHandler = EventSequence.getInstance().getSequenceActionHandler().get();
    if (sequenceHandler != null) {
      sequenceHandler.handleEvent(event);
    }
  }

  @Override
  protected synchronized Entity[] getObservedEntities() {
    var sequenceHandler = EventSequence.getInstance().getSequenceActionHandler().get();
    if (sequenceHandler != null) {
      return sequenceHandler.getObservedEntities();
    }
    return new Entity[0];
  }

}
