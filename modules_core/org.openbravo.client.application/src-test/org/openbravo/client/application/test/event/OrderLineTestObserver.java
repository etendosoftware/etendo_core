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
 * All portions are Copyright (C) 2016-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test.event;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.Note;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.kernel.event.TransactionBeginEvent;
import org.openbravo.client.kernel.event.TransactionCompletedEvent;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

/**
 * Test persistence observer used by {@link DatasourceEventObserver} to ensure observer is correctly
 * invoked and works fine together with datasource update invocations.
 * 
 * @author alostale
 *
 */
public class OrderLineTestObserver extends EntityPersistenceEventObserver {
  static final String FORCED_DESCRIPTION = "test description";
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME) };
  private static int executionCount = 0;
  private static int beginTrx = 0;
  private static int endTrx = 0;

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    switch (ObserverBaseTest.observerExecutionType) {
      case UPDATE_DESCRIPTION:
        event.setCurrentState(entities[0].getProperty(OrderLine.PROPERTY_DESCRIPTION),
            FORCED_DESCRIPTION);
        break;
      case CREATE_NOTE:
        final OrderLine orderLine = (OrderLine) event.getTargetInstance();
        Note newNote = OBProvider.getInstance().get(Note.class);
        newNote.setTable(
            OBDal.getInstance().getProxy(Table.class, orderLine.getEntity().getTableId()));
        newNote.setRecord(orderLine.getId());
        newNote.setNote("test");
        OBDal.getInstance().save(newNote);
        break;
      case COUNT_LINES:
        int numOfLines = ((OrderLine) event.getTargetInstance()).getSalesOrder()
            .getOrderLineList()
            .size();
        event.setCurrentState(entities[0].getProperty(OrderLine.PROPERTY_DESCRIPTION),
            FORCED_DESCRIPTION + numOfLines);
        break;
      case UPDATE_PARENT:
        Order orderWithForcedDescription = ((OrderLine) event.getTargetInstance()).getSalesOrder();
        orderWithForcedDescription.setDescription(FORCED_DESCRIPTION);
        break;
      case UPDATE_PARENT_RANDOM:
        Order orderWithRandomDescription = ((OrderLine) event.getTargetInstance()).getSalesOrder();
        orderWithRandomDescription.setDescription(Long.toString(System.currentTimeMillis()));
        break;
      case ON_NOOP:
        break;
      default:
        return;
    }

    executionCount++;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    switch (ObserverBaseTest.observerExecutionType) {
      case ON_NOOP:
        break;
      default:
        return;
    }

    executionCount++;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    switch (ObserverBaseTest.observerExecutionType) {
      case ON_NOOP:
        break;
      default:
        return;
    }

    executionCount++;
  }

  public void onTransactionBegin(@Observes TransactionBeginEvent event) {
    beginTrx++;
  }

  public void onTransactionCompleted(@Observes TransactionCompletedEvent event) {
    endTrx++;
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  static void resetExecutionCount() {
    executionCount = 0;
    beginTrx = 0;
    endTrx = 0;
  }

  static int getNumberOfExecutions() {
    return executionCount;
  }

  public static int getNumberOfStartedTrxs() {
    return beginTrx;
  }

  public static int getNumberOfClosedTrxs() {
    return endTrx;
  }

  public static void refreshObservedEntities() {
    Entity currentEntity = ModelProvider.getInstance().getEntity(OrderLine.ENTITY_NAME);
    if (entities[0] != currentEntity) {
      entities[0] = currentEntity;
    }
  }
}
