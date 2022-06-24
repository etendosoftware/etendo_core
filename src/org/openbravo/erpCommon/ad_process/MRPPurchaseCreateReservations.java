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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.SessionHandler;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.model.mrp.PurchasingRun;
import org.openbravo.model.mrp.PurchasingRunLine;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.service.db.DalBaseProcess;
import org.openbravo.service.db.DbUtility;

public class MRPPurchaseCreateReservations extends DalBaseProcess {

  // private ProcessLogger logger;

  @Override
  public void doExecute(final ProcessBundle bundle) throws Exception {
    // logger = bundle.getLogger();
    final Map<String, Object> params = bundle.getParams();

    final String strMRPRunId = (String) params.get("MRP_Run_Purchase_ID");
    final PurchasingRun mrpPurchaseRun = OBDal.getInstance().get(PurchasingRun.class, strMRPRunId);

    final String strMWarehosueID = (String) params.get("mWarehouseId");

    // Execute Create Orders process.
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "800163");
    } finally {
      OBContext.restorePreviousMode();
    }
    final Map<String, String> createOrderParams = new HashMap<>();
    createOrderParams.put("M_Warehouse_ID", strMWarehosueID);
    try {
      final ProcessInstance pinstance = CallProcess.getInstance()
          .call(process, strMRPRunId, createOrderParams);

      if (pinstance.getResult() == 0L) {
        OBDal.getInstance().rollbackAndClose();
        final OBError oberror = OBMessageUtils.getProcessInstanceMessage(pinstance);
        bundle.setResult(oberror);
        return;
      }
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      final OBError messsage = OBMessageUtils
          .translateError(DbUtility.getUnderlyingSQLException(e).getMessage());
      bundle.setResult(messsage);
      return;
    }

    // Create reservations
    final ScrollableResults outgoingRLs = getPRLinesOutgoing(mrpPurchaseRun);
    final ScrollableResults incomingRLs = getPRLinesIncoming(mrpPurchaseRun);
    int i = 1;
    BigDecimal currentStock = BigDecimal.ZERO;

    PurchasingRunLine incomingLine = null;
    String productID = "";
    try {
      while (outgoingRLs.next()) {
        final PurchasingRunLine outgoingLine = (PurchasingRunLine) outgoingRLs.get(0);
        if (!productID.equals(outgoingLine.getProduct().getId())) {
          productID = outgoingLine.getProduct().getId();
          currentStock = BigDecimal.ZERO;
        }
        BigDecimal quantity = outgoingLine.getQuantity().negate();
        final boolean isSalesOrderLine = outgoingLine.getSalesOrderLine() != null
            && outgoingLine.getSalesOrderLine().getSalesOrder().isSalesTransaction();
        while (quantity.signum() == 1) {
          if (currentStock.signum() < 1 && incomingRLs.next()) {
            incomingLine = (PurchasingRunLine) incomingRLs.get(0);
            if (!productID.equals(outgoingLine.getProduct().getId()) && incomingRLs.next()) {
              incomingLine = (PurchasingRunLine) incomingRLs.get(0);
            }
            currentStock = currentStock.add(incomingLine.getQuantity());
            if (incomingLine.getTransactionType().equals("PP")
                && incomingLine.getSalesOrderLine() != null) {
              OBDal.getInstance().refresh(incomingLine.getSalesOrderLine().getSalesOrder());
              if (!incomingLine.getSalesOrderLine().getSalesOrder().isProcessed()) {
                try {
                  processOrder(incomingLine.getSalesOrderLine().getSalesOrder());
                } catch (OBException e) {
                  OBDal.getInstance().rollbackAndClose();
                  final OBError error = OBMessageUtils.translateError(e.getMessage());
                  bundle.setResult(error);
                  return;
                }
              }
            }
          }
          final BigDecimal consumedQuantity = currentStock.min(quantity);
          currentStock = currentStock.subtract(consumedQuantity);
          quantity = quantity.subtract(consumedQuantity);
          if (isSalesOrderLine) {
            final Reservation reservation = ReservationUtils
                .getReservationFromOrder(outgoingLine.getSalesOrderLine());
            if (reservation.getReservedQty().compareTo(reservation.getQuantity()) < 0) {
              if (incomingLine.getTransactionType().equals("PP")
                  && incomingLine.getSalesOrderLine() != null) {
                ReservationUtils.reserveStockManual(reservation, incomingLine.getSalesOrderLine(),
                    consumedQuantity,
                    incomingLine.getSalesOrderLine().getSalesOrder().getWarehouse().isAllocated()
                        ? "Y"
                        : "N");
              }

              if (quantity.signum() < 1 && reservation.getRESStatus().equals("DR")) {
                ReservationUtils.processReserve(reservation, "PR");
              }
            }
            OBDal.getInstance().save(reservation);
            OBDal.getInstance().flush();
          }
        }
        if ((i % 100) == 0) {
          SessionHandler.getInstance().commitAndStart();
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      incomingRLs.close();
      outgoingRLs.close();
    }
    final OBError message = new OBError();
    message.setType("Success");
    message.setTitle(OBMessageUtils.messageBD("Success"));
    bundle.setResult(message);
  }

  private ScrollableResults getPRLinesIncoming(final PurchasingRun mrpPurchaseRun) {
    //@formatter:off
    final String hql =
                  " where purchasingPlan.id = :purchaserunId" +
                  "   and quantity > 0" +
                  " order by product" +
                  "   ,plannedDate " +
                  "   , case transactionType " +
                  "       when 'ST' then 0 " +
                  "       when 'MS' then 2 " +
                  "       else 1 " +
                  "     end";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(PurchasingRunLine.class, hql)
        .setNamedParameter("purchaserunId", mrpPurchaseRun.getId())
        .setFetchSize(1000)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private ScrollableResults getPRLinesOutgoing(final PurchasingRun mrpPurchaseRun) {
    //@formatter:off
    final String hql =
                  " where purchasingPlan.id = :purchaserunId" +
                  "   and quantity < 0" +
                  " order by product" +
                  "   , plannedDate" +
                  "   , case transactionType " +
                  "       when 'ST' then 0 " +
                  "       when 'MS' then 2 " +
                  "       else 3 " +
                  "     end";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(PurchasingRunLine.class, hql)
        .setNamedParameter("purchaserunId", mrpPurchaseRun.getId())
        .setFetchSize(1000)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private void processOrder(final Order salesOrder) {
    OBContext.setAdminMode(true);
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "104");
    } finally {
      OBContext.restorePreviousMode();
    }
    try {
      final ProcessInstance pinstance = CallProcess.getInstance()
          .call(process, salesOrder.getId(), null);

      if (pinstance.getResult() == 0L) {
        OBError oberror = OBMessageUtils.getProcessInstanceMessage(pinstance);
        throw new OBException(oberror.getMessage());
      }
    } catch (Exception e) {
      final Throwable t = DbUtility.getUnderlyingSQLException(e);
      throw new OBException(OBMessageUtils.parseTranslation(t.getMessage()), t);
    }
  }
}
