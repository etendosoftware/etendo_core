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
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.erpCommon.businessUtility;

import java.math.BigDecimal;
import java.util.Optional;

import javax.enterprise.context.Dependent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.service.db.DbUtility;

/**
 * Process that cancels an existing order and creates another one inverse of the original.
 * 
 * This process will create a netting payment to leave the original order and the inverse order
 * completely paid, and if anything was paid in the original order it will be paid so in the new
 * one.
 */
@Dependent
class CancelOrderExecutor extends CancelAndReplaceUtils {
  private Logger log4j = LogManager.getLogger();
  private String oldOrderId;
  private String paymentOrganizationId;
  private JSONObject jsonOrder;
  private boolean useOrderDocumentNoForRelatedDocs;

  @SuppressWarnings("hiding")
  void init(String oldOrderId, String paymentOrganizationId, JSONObject jsonOrder,
      boolean useOrderDocumentNoForRelatedDocs) {
    this.oldOrderId = oldOrderId;
    this.paymentOrganizationId = paymentOrganizationId;
    this.jsonOrder = jsonOrder;
    this.useOrderDocumentNoForRelatedDocs = useOrderDocumentNoForRelatedDocs;
  }

  void run() {
    cancelOrder();
  }

  private void cancelOrder() {
    OBContext.setAdminMode(false);
    try {
      Order oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
      oldOrder = lockOrder(oldOrder);

      // Added check in case Cancel and Replace button is hit more than once
      throwExceptionIfOrderIsCanceled(oldOrder);

      // Close old reservations
      closeOldReservations(oldOrder);
      oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);

      Order inverseOrder = OBDal.getInstance().get(Order.class, jsonOrder.getString("id"));
      closeOrder(inverseOrder);
      inverseOrder = OBDal.getInstance().get(Order.class, inverseOrder.getId());
      closeOrder(oldOrder);
      oldOrder = OBDal.getInstance().get(Order.class, oldOrderId);
      OBDal.getInstance().flush();

      // Payment Creation only to orders with grand total different than ZERO
      // Get the payment schedule detail of the oldOrder
      final Organization paymentOrganization = OBDal.getInstance()
          .get(Organization.class, paymentOrganizationId);
      createNettingPayment(oldOrder, inverseOrder, paymentOrganization);

      runCancelAndReplaceOrderHooks(oldOrder, inverseOrder, Optional.empty(), jsonOrder);
    } catch (Exception e1) {
      Throwable e2 = DbUtility.getUnderlyingSQLException(e1);
      log4j.error("Error executing Cancel and Replace", e1);
      throw new OBException(e2.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private void createNettingPayment(Order oldOrder, Order inverseOrder,
      Organization paymentOrganization) {
    try {
      if (oldOrder.getGrandTotalAmount().compareTo(BigDecimal.ZERO) == 0) {
        return;
      }

      final FIN_PaymentSchedule paymentSchedule = CancelAndReplaceUtils
          .getPaymentScheduleOfOrder(oldOrder);
      if (paymentSchedule == null) {
        throw new OBException("There is no payment plan for the order: " + oldOrder.getId());
      }

      // To only cancel a layaway two payments must be added to fully pay the old order and add
      // the same quantity in negative to the inverse order
      if (jsonOrder.getJSONArray("payments").length() > 0) {
        WeldUtils.getInstanceFromStaticBeanManager(CancelLayawayPaymentsHookCaller.class)
            .executeHook(jsonOrder, inverseOrder);
      }

      final BigDecimal outstandingAmount = CancelAndReplaceUtils
          .getPaymentScheduleOutstandingAmount(paymentSchedule);
      final BigDecimal negativeAmount = outstandingAmount.negate();
      final FIN_Payment nettingPayment = outstandingAmount.compareTo(BigDecimal.ZERO) != 0
          ? payOriginalAndInverseOrder(jsonOrder, oldOrder, inverseOrder, paymentOrganization,
              outstandingAmount, negativeAmount, useOrderDocumentNoForRelatedDocs)
          : null;
      if (nettingPayment != null) {
        processPayment(nettingPayment, jsonOrder);
      }

    } catch (Exception e1) {
      log4j.error("Error in CancelAndReplaceUtils.createPayments", e1);
      try {
        OBDal.getInstance().getConnection().rollback();
      } catch (Exception e2) {
        throw new OBException(e2);
      }
      Throwable e3 = DbUtility.getUnderlyingSQLException(e1);
      throw new OBException(e3);
    }
  }

}
