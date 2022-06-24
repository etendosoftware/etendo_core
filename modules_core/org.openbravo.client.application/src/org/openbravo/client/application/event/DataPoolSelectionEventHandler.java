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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.application.DataPoolSelection;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.client.kernel.event.TransactionBeginEvent;
import org.openbravo.client.kernel.event.TransactionCompletedEvent;
import org.openbravo.dal.service.DataPoolChecker;

/**
 * Refreshes DataPoolChecker report-pool mapping when DataPoolSelection changes.
 *
 * Note that in a cluster environment, it is required to restart Tomcat to refresh all changes in
 * every node.
 */
class DataPoolSelectionEventHandler extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(DataPoolSelection.class) };

  private static ThreadLocal<String> dataPoolSelectionValueUpdated = new ThreadLocal<>();
  private static DataPoolChecker dataPoolChecker;

  public void onTransactionBegin(@Observes TransactionBeginEvent event) {
    dataPoolSelectionValueUpdated.set(null);
  }

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onCreate(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DataPoolSelection dataPoolSelection = (DataPoolSelection) event.getTargetInstance();
    dataPoolSelectionValueUpdated.set(dataPoolSelection.getId());
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DataPoolSelection dataPoolSelection = (DataPoolSelection) event.getTargetInstance();
    dataPoolSelectionValueUpdated.set(dataPoolSelection.getId());
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    final DataPoolSelection dataPoolSelection = (DataPoolSelection) event.getTargetInstance();
    dataPoolSelectionValueUpdated.set(dataPoolSelection.getId());
  }

  public void onTransactionCompleted(@Observes TransactionCompletedEvent event) {
    String strValueId = dataPoolSelectionValueUpdated.get();
    dataPoolSelectionValueUpdated.set(null);
    if (StringUtils.isBlank(strValueId)
        || event.getTransaction().getStatus() == TransactionStatus.ROLLED_BACK) {
      return;
    }

    getDataPoolChecker().refreshDataPoolProcesses();
  }

  private static DataPoolChecker getDataPoolChecker() {
    if (dataPoolChecker == null) {
      dataPoolChecker = DataPoolChecker.getInstance();
    }

    return dataPoolChecker;
  }

}
