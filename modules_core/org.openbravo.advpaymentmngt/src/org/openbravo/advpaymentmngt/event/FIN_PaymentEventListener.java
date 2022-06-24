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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.event;

import java.math.BigDecimal;
import java.util.List;

import javax.enterprise.event.Observes;

import org.apache.commons.lang.StringUtils;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.client.kernel.event.EntityDeleteEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEvent;
import org.openbravo.client.kernel.event.EntityPersistenceEventObserver;
import org.openbravo.client.kernel.event.EntityUpdateEvent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.businessUtility.CancelAndReplaceUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.service.db.DalConnectionProvider;

class FIN_PaymentEventListener extends EntityPersistenceEventObserver {
  private static Entity[] entities = {
      ModelProvider.getInstance().getEntity(FIN_Payment.ENTITY_NAME) };

  @Override
  protected Entity[] getObservedEntities() {
    return entities;
  }

  public void onUpdate(@Observes EntityUpdateEvent event) {
    if (!isValidEvent(event)) {
      return;
    }

    final FIN_Payment payment = (FIN_Payment) event.getTargetInstance();
    final Entity paymentEntity = ModelProvider.getInstance().getEntity(FIN_Payment.ENTITY_NAME);
    final Property paymentProcessedProperty = paymentEntity
        .getProperty(FIN_Payment.PROPERTY_PROCESSED);
    final Boolean currentPaymentProcessed = (Boolean) event
        .getCurrentState(paymentProcessedProperty);
    final Boolean oldPaymentProcessed = (Boolean) event.getPreviousState(paymentProcessedProperty);
    final Property paymentAmountProperty = paymentEntity.getProperty(FIN_Payment.PROPERTY_AMOUNT);
    final BigDecimal currentPaymentAmount = (BigDecimal) event
        .getCurrentState(paymentAmountProperty);
    final BigDecimal oldPaymentAmount = (BigDecimal) event.getPreviousState(paymentAmountProperty);
    final Property paymentStatusProperty = paymentEntity.getProperty(FIN_Payment.PROPERTY_STATUS);
    final String currentPaymentStatus = (String) event.getCurrentState(paymentStatusProperty);
    final String oldPaymentStatus = (String) event.getPreviousState(paymentStatusProperty);

    final String documentNo = payment.getDocumentNo();
    final int documentNoLength = documentNo.length();

    if (!oldPaymentProcessed && currentPaymentProcessed
        && currentPaymentAmount.compareTo(BigDecimal.ZERO) == 0
        && (documentNoLength < CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length()
            || !CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.equals(documentNo.substring(
                documentNoLength - CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length())))) {
      // Processing a zero payment: add sufix
      final int documentNoLimit = CancelAndReplaceUtils.PAYMENT_DOCNO_LENGTH
          - CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length();
      String newDocumentNo = (documentNoLength > documentNoLimit
          ? documentNo.substring(0, documentNoLimit)
          : documentNo) + CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX;
      setDocumentNoToPayment(event, newDocumentNo);
    }

    else if (oldPaymentProcessed && !currentPaymentProcessed
        && oldPaymentAmount.compareTo(BigDecimal.ZERO) == 0
        && documentNoLength > CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length()
        && CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.equals(documentNo
            .substring(documentNoLength - CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length()))) {
      // Reactivating a zero payment: remove sufix
      String newDocumentNo = documentNo.substring(0,
          documentNoLength - CancelAndReplaceUtils.ZERO_PAYMENT_SUFIX.length());
      setDocumentNoToPayment(event, newDocumentNo);
    }

    manageAPRMPendingPaymentFromInvoiceRecord(payment, currentPaymentStatus, oldPaymentStatus);
  }

  public void onDelete(@Observes EntityDeleteEvent event) {
    if (!isValidEvent(event)) {
      return;
    }
    FIN_Payment pay = OBDal.getInstance().get(FIN_Payment.class, event.getTargetInstance().getId());
    List<FIN_PaymentDetail> pdList = pay.getFINPaymentDetailList();
    if (!pdList.isEmpty()) {
      String language = OBContext.getOBContext().getLanguage().getLanguage();
      ConnectionProvider conn = new DalConnectionProvider(false);
      throw new OBException(Utility.messageBD(conn, "ForeignKeyViolation", language));
    }
  }

  /**
   * Manages the APRMPendingPaymentFromInvoice record linked to the payment:
   * 
   * If the current payment status is Awaiting Execution, it updates the Payment Execution Process
   * Id of the APRMPendingPaymentFromInvoice record to point to the current Payment Execution
   * Process Id. If the current Payment Execution Process Id is null (because, for example, the
   * payment is associated to another payment method without payment process), then we delete the
   * APRMPendingPaymentFromInvoice record.
   * 
   * The APRMPendingPaymentFromInvoice record is also deleted when the payment status has evolved
   * from Awaiting Execution to Awaiting Payment or Voided (i.e. a reactivation has taken place).
   * This way the behavior is exactly the same as when creating a manual payment in awaiting
   * execution.
   * 
   * Returns the number of records updated or deleted (0 or 1)
   * 
   */
  private int manageAPRMPendingPaymentFromInvoiceRecord(FIN_Payment payment,
      String currentPaymentStatus, String oldPaymentStatus) {
    try {
      OBContext.setAdminMode(true);
      int rowCount = 0;

      if (StringUtils.equals("RPAE", currentPaymentStatus)) {
        final PaymentExecutionProcess executionProcess = new AdvPaymentMngtDao()
            .getExecutionProcess(payment);
        if (executionProcess == null) {
          rowCount = deleteAPRMPendingPaymentFromInvoiceRecord(payment);
        } else {
          rowCount = updateAPRMPendingPaymentFromInvoiceRecord(payment, executionProcess);
        }
      } else if (StringUtils.equals("RPAE", oldPaymentStatus)
          && (StringUtils.equals(currentPaymentStatus, "RPAP")
              || StringUtils.equals(currentPaymentStatus, "RPVOID"))) {
        rowCount = deleteAPRMPendingPaymentFromInvoiceRecord(payment);
      }

      return rowCount;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Updates the APRMPendingPaymentFromInvoice record setting the given Payment Execution Process Id
   * 
   * Returns the number of records updated (0 or 1)
   */
  private int updateAPRMPendingPaymentFromInvoiceRecord(final FIN_Payment payment,
      final PaymentExecutionProcess executionProcess) {
    if (executionProcess == null || payment == null) {
      return 0;
    }

    //@formatter:off
    String hql =
            "update APRM_PendingPaymentInvoice " +
            "set paymentExecutionProcess.id = :paymentExecutionProcessId " +
            " where paymentExecutionProcess.id <> :paymentExecutionProcessId " +
            "   and payment.id = :paymentId ";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("paymentExecutionProcessId", executionProcess.getId())
        .setParameter("paymentId", payment.getId())
        .executeUpdate();
  }

  /**
   * Updates the APRMPendingPaymentFromInvoice record linked to the given payment
   * 
   * Returns the number of records deleted (0 or 1)
   */
  private int deleteAPRMPendingPaymentFromInvoiceRecord(final FIN_Payment payment) {
    if (payment == null) {
      return 0;
    }

    //@formatter:off
    String hql =
            "delete from APRM_PendingPaymentInvoice " +
            " where payment.id = :paymentId ";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("paymentId", payment.getId())
        .executeUpdate();
  }

  private void setDocumentNoToPayment(EntityPersistenceEvent event, String newDocumentNo) {
    final Entity paymentEntity = ModelProvider.getInstance().getEntity(FIN_Payment.ENTITY_NAME);
    final Property paymentDocumentNoProperty = paymentEntity
        .getProperty(FIN_Payment.PROPERTY_DOCUMENTNO);
    event.setCurrentState(paymentDocumentNoProperty, newDocumentNo);
  }
}
