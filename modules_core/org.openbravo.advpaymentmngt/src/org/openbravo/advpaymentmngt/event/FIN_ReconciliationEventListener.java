/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2016-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.event;

import java.math.BigDecimal;

import javax.enterprise.event.Observes;

import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

class FIN_ReconciliationEventListener extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_Reconciliation.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    FIN_Reconciliation rec = OBDal.getInstance()
        .get(FIN_Reconciliation.class, event.getTargetInstance().getId());
    if (!rec.isProcessed()) {
      updateNextReconciliationsBalance(rec);
    }
  }

  /**
   * Update starting balance and ending balance of subsequent reconciliations when one
   * reconciliation is deleted
   * 
   * @param rec
   *          Reconciliation being deleted
   */
  private void updateNextReconciliationsBalance(final FIN_Reconciliation rec) {
    BigDecimal balance = rec.getEndingBalance().subtract(rec.getStartingbalance());
    //@formatter:off
    String hql = 
            "update FIN_Reconciliation" +
            " set startingbalance = startingbalance - :balance , " + 
            "   endingBalance = endingBalance - :balance" +
            " where account.id = :accountId" +
            "   and transactionDate > :date";
    //@formatter:on

    OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("balance", balance)
        .setParameter("accountId", rec.getAccount().getId())
        .setParameter("date", rec.getTransactionDate())
        .executeUpdate();
  }
}
