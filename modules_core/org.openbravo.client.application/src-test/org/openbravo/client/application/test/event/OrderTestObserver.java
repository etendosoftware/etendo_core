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
 * All portions are Copyright (C) 2017-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.test.event;

import java.util.List;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.application.test.event.ObserverBaseTest.ObserverExecutionType;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;

class OrderTestObserver extends EntityPersistenceEventObserver {
  private static Entity[] entities = { ModelProvider.getInstance().getEntity(Order.ENTITY_NAME) };
  private static int executionCount = 0;

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    if (ObserverBaseTest.observerExecutionType == ObserverExecutionType.ON_NOOP) {
      final Order order = (Order) event.getTargetInstance();
      final List<OrderLine> orderLineList = order.getOrderLineList();
      if (!orderLineList.isEmpty()) {
        orderLineList.get(0).setDescription(Long.toString(System.currentTimeMillis()));
        event.setCurrentState(getOrderLineListProperty(), orderLineList);
      }
    }
    executionCount++;
  }

  public void onNew(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    executionCount++;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    executionCount++;
  }

  static int getNumberOfExecutions() {
    return executionCount;
  }

  static void resetExecutionCount() {
    executionCount = 0;
  }

  private Property getOrderLineListProperty() {
    final Entity orderEntity = ModelProvider.getInstance().getEntity(Order.ENTITY_NAME);
    return orderEntity.getProperty(Order.PROPERTY_ORDERLINELIST);
  }

  public static void refreshObservedEntities() {
    Entity currentEntity = ModelProvider.getInstance().getEntity(Order.ENTITY_NAME);
    if (entities[0] != currentEntity) {
      entities[0] = currentEntity;
    }
  }
}
