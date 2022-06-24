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
 * All portions are Copyright (C) 2014-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.event;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.utility.APRMConstants;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.event.EntityNewEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;

class FIN_FinaccTransactionEventListener extends EntityPersistenceEventObserver {

  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_FinaccTransaction.ENTITY_NAME) };
  private static Logger logger = LogManager.getLogger();

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateTransactionType(event);
  }

  public void onSave(@Observes EntityNewEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    validateTransactionType(event);
  }

  /**
   * Throws OBException is transaction type is either BP Deposit or BP Withdrawal and it has not
   * defined neither a payment nor a gl item
   */
  private void validateTransactionType(final EntityPersistenceEvent event) {
    final Entity transactionEntity = ModelProvider.getInstance()
        .getEntity(FIN_FinaccTransaction.ENTITY_NAME);

    final String transactionType = (String) event.getCurrentState(
        transactionEntity.getProperty(FIN_FinaccTransaction.PROPERTY_TRANSACTIONTYPE));
    final GLItem glItem = (GLItem) event
        .getCurrentState(transactionEntity.getProperty(FIN_FinaccTransaction.PROPERTY_GLITEM));
    final FIN_Payment payment = (FIN_Payment) event
        .getCurrentState(transactionEntity.getProperty(FIN_FinaccTransaction.PROPERTY_FINPAYMENT));

    if ((StringUtils.equals(transactionType, APRMConstants.TRXTYPE_BPDeposit)
        || StringUtils.equals(transactionType, APRMConstants.TRXTYPE_BPWithdrawal))
        && glItem == null && payment == null) {
      logger.debug("@APRM_INVALID_TRANSACTION@");
      throw new OBException("@APRM_INVALID_TRANSACTION@");
    }
  }
}
