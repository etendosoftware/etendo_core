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
package org.openbravo.erpCommon.businessUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.LockOptions;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.materialmgmt.ReservationUtils;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.materialmgmt.onhandquantity.Reservation;
import org.openbravo.service.db.DalConnectionProvider;

public class CancelAndReplaceUtils {
  private static Logger log4j = LogManager.getLogger();
  private static final String HYPHENONE = "-1";
  private static final String HYPHEN = "-";
  public static final String CREATE_NETTING_SHIPMENT = "CancelAndReplaceCreateNetShipment";
  public static final String ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET = "CancelAndReplaceAssociateShipmentToNewTicket";
  public static final String ENABLE_STOCK_RESERVATIONS = "StockReservations";
  public static final String REVERSE_PREFIX = "*R*";
  public static final String ZERO_PAYMENT_SUFIX = "*Z*";
  public static final String DOCTYPE_MatShipment = "MMS";
  public static final int PAYMENT_DOCNO_LENGTH = 30;
  public static final BigDecimal NEGATIVE_ONE = BigDecimal.ONE.negate();

  /**
   * Process that creates a replacement order in temporary status in order to Cancel and Replace an
   * original order
   * 
   * @param oldOrder
   *          Order that will be cancelled and replaced
   */
  public static Order createReplacementOrder(final Order oldOrder) {
    return createReplacementOrder(oldOrder, Collections.singletonMap(oldOrder.getWarehouse(), 1))
        .get(0);
  }

  /**
   * Process that creates a replacement order in temporary status in order to Cancel and Replace an
   * original order
   * 
   * @param oldOrder
   *          Order that will be cancelled and replaced
   * @param warehouseMap
   *          Map with the list of Warehouses where the replacements will be created and the number
   *          of replacements to create in each one
   */
  public static List<Order> createReplacementOrder(final Order oldOrder,
      final Map<Warehouse, Integer> warehouseMap) {
    final CreateReplacementOrderExecutor createReplacementOrderExecutor = WeldUtils
        .getInstanceFromStaticBeanManager(CreateReplacementOrderExecutor.class);
    createReplacementOrderExecutor.init(oldOrder, warehouseMap);
    return createReplacementOrderExecutor.run();
  }

  /**
   * Method that given an Order Id it cancels it and creates another one equal but with negative
   * quantities.
   * 
   * @param oldOrderId
   *          Id of the Sales Order to be cancelled.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS.
   * @param useOrderDocumentNoForRelatedDocs
   *          flag coming from Web POS. If it is true, it will set the same document of the order to
   *          netting payment.
   */
  public static Order cancelOrder(final String oldOrderId, final JSONObject jsonOrder,
      final boolean useOrderDocumentNoForRelatedDocs) {
    final Order oldOrder = OBDal.getInstance().getProxy(Order.class, oldOrderId);
    return cancelOrder(oldOrderId, oldOrder.getOrganization().getId(), jsonOrder,
        useOrderDocumentNoForRelatedDocs);
  }

  /**
   * Method that given an Order Id it cancels it and creates another one equal but with negative
   * quantities.
   * 
   * @param oldOrderId
   *          Id of the Sales Order to be cancelled.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS.
   * @param useOrderDocumentNoForRelatedDocs
   *          flag coming from Web POS. If it is true, it will set the same document of the order to
   *          netting payment.
   */
  public static Order cancelOrder(final String oldOrderId, final String paymentOrganizationId,
      final JSONObject jsonOrder, final boolean useOrderDocumentNoForRelatedDocs) {
    final CancelOrderExecutor cancelOrderExecutor = WeldUtils
        .getInstanceFromStaticBeanManager(CancelOrderExecutor.class);
    cancelOrderExecutor.init(oldOrderId, paymentOrganizationId, jsonOrder,
        useOrderDocumentNoForRelatedDocs);
    cancelOrderExecutor.run();
    return null;
  }

  /**
   * Method that given an Order Id it cancels it and creates another one equal but with negative
   * quantities. It also creates a new order replacing the cancelled one.
   * 
   * @param newOrderId
   *          Id of the replacement Sales Order.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS
   * @param useOrderDocumentNoForRelatedDocs
   *          . flag coming from Web POS. If it is true, it will set the same document of the order
   *          to netting payment.
   */
  public static Order cancelAndReplaceOrder(final String newOrderId, final JSONObject jsonOrder,
      final boolean useOrderDocumentNoForRelatedDocs) {
    final Order newOrder = OBDal.getInstance().getProxy(Order.class, newOrderId);
    return cancelAndReplaceOrder(newOrderId, newOrder.getOrganization().getId(), jsonOrder,
        useOrderDocumentNoForRelatedDocs);
  }

  /**
   * Method that given an Order Id it cancels it and creates another one equal but with negative
   * quantities. It also creates a new order replacing the cancelled one.
   * 
   * @param newOrderId
   *          Id of the replacement Sales Order.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS
   * @param useOrderDocumentNoForRelatedDocs
   *          . flag coming from Web POS. If it is true, it will set the same document of the order
   *          to netting payment.
   */
  public static Order cancelAndReplaceOrder(final String newOrderId,
      final String paymentOrganizationId, final JSONObject jsonOrder,
      final boolean useOrderDocumentNoForRelatedDocs) {
    final Order newOrder = OBDal.getInstance().getProxy(Order.class, newOrderId);
    return cancelAndReplaceOrder(newOrder.getReplacedorder().getId(),
        Collections.singleton(newOrderId), paymentOrganizationId, jsonOrder,
        useOrderDocumentNoForRelatedDocs).get(0);
  }

  /**
   * Method that given an Order Id it cancels it and creates another one equal but with negative
   * quantities. It also creates a new order replacing the cancelled one.
   * 
   * @param oldOrderId
   *          Id of the Sales Order to be cancelled.
   * @param newOrderIds
   *          Set of IDs of the replacement Sales Orders.
   * @param jsonOrder
   *          Parameter with order information coming from Web POS
   * @param useOrderDocumentNoForRelatedDocs
   *          . flag coming from Web POS. If it is true, it will set the same document of the order
   *          to netting payment.
   */
  public static List<Order> cancelAndReplaceOrder(final String oldOrderId,
      final Set<String> newOrderIds, final String paymentOrganizationId, final JSONObject jsonOrder,
      final boolean useOrderDocumentNoForRelatedDocs) {
    final ReplaceOrderExecutor replaceOrderExecutor = WeldUtils
        .getInstanceFromStaticBeanManager(ReplaceOrderExecutor.class);
    replaceOrderExecutor.init(oldOrderId, newOrderIds, paymentOrganizationId, jsonOrder,
        useOrderDocumentNoForRelatedDocs);
    return replaceOrderExecutor.run();
  }

  /**
   * Process that generates a document number for an order which cancels another order.
   * 
   * @param documentNo
   *          Document number of the cancelled order.
   * @return The new document number for the order which cancels the old order.
   */
  public static String getNextCancelDocNo(final String documentNo) {
    final StringBuilder newDocNo = new StringBuilder();
    final String[] splittedDocNo = documentNo.split(HYPHEN);
    if (splittedDocNo.length > 1) {
      int nextNumber;
      try {
        nextNumber = Integer.parseInt(splittedDocNo[splittedDocNo.length - 1]) + 1;
        for (int i = 0; i < splittedDocNo.length; i++) {
          if (i == 0 || i < splittedDocNo.length - 1) {
            newDocNo.append(splittedDocNo[i] + HYPHEN);
          } else {
            newDocNo.append(nextNumber);
          }
        }
      } catch (NumberFormatException nfe) {
        newDocNo.append(documentNo + HYPHENONE);
      }
    } else {
      newDocNo.append(documentNo + HYPHENONE);
    }
    return newDocNo.toString();
  }

  /**
   * Method to check if a netting shipment must be generated during the C&amp;R and CL process.
   * 
   * @param order
   *          The order that is being canceled.
   * @return True if is necessary to create the netting shipment.
   */
  public static boolean getCreateNettingGoodsShipmentPreferenceValue(final Order order) {
    boolean createNettingGoodsShipment = false;
    try {
      createNettingGoodsShipment = Preferences
          .getPreferenceValue(CREATE_NETTING_SHIPMENT, true,
              OBContext.getOBContext().getCurrentClient(), order.getOrganization(),
              OBContext.getOBContext().getUser(), null, null)
          .equals("Y");
    } catch (PropertyException e1) {
      createNettingGoodsShipment = false;
    }
    return createNettingGoodsShipment;
  }

  /**
   * Method to check if during the C&amp;R process the shipment lines must be moved from the old
   * order to the new order.
   * 
   * @param order
   *          The order that is being canceled.
   * @return True if the shipment lines must be moved to the new order.
   */
  public static boolean getAssociateGoodsShipmentToNewSalesOrderPreferenceValue(final Order order) {
    boolean associateShipmentToNewReceipt = false;
    try {
      associateShipmentToNewReceipt = Preferences
          .getPreferenceValue(ASSOCIATE_SHIPMENT_TO_REPLACE_TICKET, true,
              OBContext.getOBContext().getCurrentClient(), order.getOrganization(),
              OBContext.getOBContext().getUser(), null, null)
          .equals("Y");
    } catch (PropertyException e1) {
      associateShipmentToNewReceipt = false;
    }
    return associateShipmentToNewReceipt;
  }

  static void throwExceptionIfOrderIsCanceled(final Order order) {
    if (order.isCancelled().booleanValue()) {
      throw new OBException(
          String.format(OBMessageUtils.messageBD("IsCancelled"), order.getDocumentNo()));
    }
  }

  static void closeOrder(final Order order) {
    order.setDelivered(true);
    order.setDocumentStatus("CL");
    order.setDocumentAction("--");
    order.setCancelled(true);
    order.setProcessed(true);
    order.setProcessNow(false);
    OBDal.getInstance().save(order);
  }

  static void closeOldReservations(final Order oldOrder) {
    if (getEnableStockReservationsPreferenceValue(oldOrder.getOrganization())) {
      ScrollableResults oldOrderLines = null;
      try {
        // Iterate old order lines
        oldOrderLines = getOrderLineList(oldOrder);
        int i = 0;
        while (oldOrderLines.next()) {
          final OrderLine oldOrderLine = (OrderLine) oldOrderLines.get(0);
          final Reservation reservation = getReservationForOrderLine(oldOrderLine);
          if (reservation != null) {
            ReservationUtils.processReserve(reservation, "CL");
          }
          if ((++i % 100) == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
          }
        }
      } catch (Exception e) {
        log4j.error("Error in CancelAndReplaceUtils.releaseOldReservations", e);
        throw new OBException(e.getMessage(), e);
      } finally {
        if (oldOrderLines != null) {
          oldOrderLines.close();
        }
      }
    }
  }

  static Reservation getReservationForOrderLine(final OrderLine line) {
    return (Reservation) OBDal.getInstance()
        .createCriteria(Reservation.class)
        .add(Restrictions.eq(Reservation.PROPERTY_SALESORDERLINE, line))
        .setMaxResults(1)
        .uniqueResult();
  }

  static ScrollableResults getOrderLineList(final Order order) {
    return OBDal.getInstance()
        .createCriteria(OrderLine.class)
        .add(Restrictions.eq(OrderLine.PROPERTY_SALESORDER, order))
        .setFilterOnReadableOrganization(false)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  static void processPayment(final FIN_Payment nettingPayment, final JSONObject jsonOrder) {
    if (areTriggersDisabled(jsonOrder)) {
      TriggerHandler.getInstance().enable();
    }
    try {
      FIN_PaymentProcess.doProcessPayment(nettingPayment, "P", null, null);
    } finally {
      if (areTriggersDisabled(jsonOrder)) {
        TriggerHandler.getInstance().disable();
      }
    }
  }

  static boolean areTriggersDisabled(final JSONObject jsonOrder) {
    return jsonOrder != null;
  }

  static BigDecimal getPaymentScheduleOutstandingAmount(final FIN_PaymentSchedule paymentSchedule) {
    // @formatter:off
    final String hql = 
                  "select coalesce(sum(psd.amount), 0) as amount" +
                  "  from FIN_Payment_ScheduleDetail as psd" +
                  " where psd.orderPaymentSchedule.id = :paymentScheduleId" +
                  "   and psd.paymentDetails is null";
    // @formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("paymentScheduleId", paymentSchedule.getId())
        .setMaxResults(1)
        .uniqueResult();
  }

  // Pay original order and inverse order.
  static FIN_Payment payOriginalAndInverseOrder(final JSONObject jsonOrder, final Order oldOrder,
      final Order inverseOrder, final Organization paymentOrganization,
      final BigDecimal outstandingAmount, final BigDecimal negativeAmount,
      final boolean useOrderDocumentNoForRelatedDocs) throws JSONException {
    FIN_Payment nettingPayment = null;
    String paymentDocumentNo = null;
    FIN_PaymentMethod paymentPaymentMethod = null;
    FIN_FinancialAccount financialAccount = null;
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(oldOrder.getOrganization().getClient().getId());
    if (jsonOrder != null) {
      paymentPaymentMethod = OBDal.getInstance()
          .get(FIN_PaymentMethod.class,
              (String) jsonOrder.getJSONObject("defaultPaymentType").get("paymentMethodId"));
      financialAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class,
              (String) jsonOrder.getJSONObject("defaultPaymentType").get("financialAccountId"));
    } else {
      paymentPaymentMethod = oldOrder.getPaymentMethod();
      // Find a financial account belong the organization tree
      if (oldOrder.getBusinessPartner().getAccount() != null
          && FIN_Utility.getFinancialAccountPaymentMethod(paymentPaymentMethod.getId(),
              oldOrder.getBusinessPartner().getAccount().getId(), true,
              oldOrder.getCurrency().getId()) != null
          && osp.isInNaturalTree(oldOrder.getBusinessPartner().getAccount().getOrganization(),
              OBDal.getInstance().get(Organization.class, oldOrder.getOrganization().getId()))) {
        financialAccount = oldOrder.getBusinessPartner().getAccount();
      } else {
        financialAccount = FIN_Utility
            .getFinancialAccountPaymentMethod(paymentPaymentMethod.getId(), null, true,
                oldOrder.getCurrency().getId(), oldOrder.getOrganization().getId())
            .getAccount();
      }
    }

    final DocumentType paymentDocumentType = FIN_Utility.getDocumentType(oldOrder.getOrganization(),
        AcctServer.DOCTYPE_ARReceipt);
    if (paymentDocumentType == null) {
      throw new OBException(OBMessageUtils.messageBD("NoDocTypeDefinedForPaymentIn"));
    }

    paymentDocumentNo = getPaymentDocumentNo(useOrderDocumentNoForRelatedDocs, oldOrder,
        paymentDocumentType);

    // Get Payment Description
    String description = getOrderDocumentNoLabel();
    description += ": " + inverseOrder.getDocumentNo();

    // Duplicate payment with negative amount
    nettingPayment = createOrUdpatePayment(nettingPayment, inverseOrder, paymentOrganization,
        paymentPaymentMethod, negativeAmount, paymentDocumentType, financialAccount,
        paymentDocumentNo);

    if (outstandingAmount.compareTo(BigDecimal.ZERO) > 0) {
      // Duplicate payment with positive amount
      nettingPayment = createOrUdpatePayment(nettingPayment, oldOrder, paymentOrganization,
          paymentPaymentMethod, outstandingAmount, paymentDocumentType, financialAccount,
          paymentDocumentNo);
      description += ": " + oldOrder.getDocumentNo() + "\n";
    }

    // Set amount and used credit to zero
    nettingPayment.setAmount(BigDecimal.ZERO);
    nettingPayment.setFinancialTransactionAmount(BigDecimal.ZERO);
    nettingPayment.setUsedCredit(BigDecimal.ZERO);
    final String truncatedDescription = (description.length() > 255)
        ? description.substring(0, 252).concat("...")
        : description;
    nettingPayment.setDescription(truncatedDescription);
    return nettingPayment;
  }

  /**
   * Method that given an amount, payment method, financial account, document type, and a document
   * number, it creates a payment for a given Order. Also a payment is passed as parameter, if that
   * payment is null a new payment is created, if not, a new detail is added to the payment.
   */
  static FIN_Payment createOrUdpatePayment(final FIN_Payment nettingPayment, final Order order,
      final Organization paymentOrganization, final FIN_PaymentMethod paymentPaymentMethod,
      final BigDecimal amount, final DocumentType paymentDocumentType,
      final FIN_FinancialAccount financialAccount, final String paymentDocumentNo) {
    FIN_Payment currentNettingPayment = nettingPayment;
    // Get the payment schedule of the order
    FIN_PaymentSchedule paymentSchedule = getPaymentScheduleOfOrder(order);
    if (paymentSchedule == null) {
      paymentSchedule = createPaymentSchedule(order, amount);
    }

    if (currentNettingPayment == null) {
      // This is the first call to modify the netting payment. It is called to create the inverse
      // order detail.
      final List<FIN_PaymentScheduleDetail> paymentScheduleDetailList = new ArrayList<>();
      final HashMap<String, BigDecimal> paymentScheduleDetailAmount = new HashMap<>();
      final FIN_PaymentScheduleDetail paymentScheduleDetail = OBProvider.getInstance()
          .get(FIN_PaymentScheduleDetail.class);
      paymentScheduleDetail.setOrganization(order.getOrganization());
      paymentScheduleDetail.setOrderPaymentSchedule(paymentSchedule);
      paymentScheduleDetail.setBusinessPartner(order.getBusinessPartner());
      paymentScheduleDetail.setAmount(amount);
      OBDal.getInstance().save(paymentScheduleDetail);
      paymentScheduleDetailList.add(paymentScheduleDetail);

      final String paymentScheduleDetailId = paymentScheduleDetail.getId();
      paymentScheduleDetailAmount.put(paymentScheduleDetailId, amount);

      // Call to savePayment in order to create a new payment in
      currentNettingPayment = FIN_AddPayment.savePayment(currentNettingPayment, true,
          paymentDocumentType, paymentDocumentNo, order.getBusinessPartner(), paymentPaymentMethod,
          financialAccount, amount.toPlainString(), order.getOrderDate(), paymentOrganization, null,
          paymentScheduleDetailList, paymentScheduleDetailAmount, false, false, order.getCurrency(),
          BigDecimal.ZERO, BigDecimal.ZERO);
    } else {
      // The netting payment detail is being created for the original or the inverse order. It is
      // necessary to search for the existing outstanding PSD and set them to the payment.
      final OBCriteria<FIN_PaymentScheduleDetail> paymentScheduleDetailCriteria = OBDal
          .getInstance()
          .createCriteria(FIN_PaymentScheduleDetail.class);
      paymentScheduleDetailCriteria.add(Restrictions
          .eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE, paymentSchedule));
      // There should be only one with null paymentDetails
      paymentScheduleDetailCriteria
          .add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
      paymentScheduleDetailCriteria.add(Restrictions
          .eq(FIN_PaymentScheduleDetail.PROPERTY_ORGANIZATION, paymentSchedule.getOrganization()));
      paymentScheduleDetailCriteria.setFilterOnReadableOrganization(false);
      final List<FIN_PaymentScheduleDetail> pendingPaymentScheduleDetailList = paymentScheduleDetailCriteria
          .list();
      BigDecimal remainingAmount = new BigDecimal(amount.toString());
      final boolean isRemainingNegative = remainingAmount.compareTo(BigDecimal.ZERO) < 0;
      for (final FIN_PaymentScheduleDetail remainingPSD : pendingPaymentScheduleDetailList) {
        if ((!isRemainingNegative && remainingAmount.compareTo(BigDecimal.ZERO) > 0)
            || (isRemainingNegative && remainingAmount.compareTo(BigDecimal.ZERO) < 0)) {
          final BigDecimal auxAmount = new BigDecimal(remainingPSD.getAmount().toString());
          if (remainingPSD.getAmount().compareTo(remainingAmount) > 0) {
            // The PSD with the remaining amount is bigger to the amount to create, so it must be
            // separated in two different details
            FIN_AddPayment.createPSD(remainingPSD.getAmount().subtract(remainingAmount),
                paymentSchedule, remainingPSD.getInvoicePaymentSchedule(), order.getOrganization(),
                order.getBusinessPartner());
            remainingPSD.setAmount(remainingAmount);
            OBDal.getInstance().save(remainingPSD);
          }
          remainingAmount = remainingAmount.subtract(auxAmount);
          FIN_AddPayment.updatePaymentDetail(remainingPSD, currentNettingPayment,
              remainingPSD.getAmount(), false);
        } else {
          break;
        }
      }
      if ((!isRemainingNegative && remainingAmount.compareTo(BigDecimal.ZERO) > 0)
          || (isRemainingNegative && remainingAmount.compareTo(BigDecimal.ZERO) < 0)) {
        // If the new order has a lower amount than the initially paid amount, the payment must have
        // a bigger amount than the order, and the outstanding amount must be negative
        final FIN_PaymentScheduleDetail lastRemainingPSD = pendingPaymentScheduleDetailList
            .get(pendingPaymentScheduleDetailList.size() - 1);
        lastRemainingPSD.setAmount(lastRemainingPSD.getAmount().add(remainingAmount));
        OBDal.getInstance().save(lastRemainingPSD);
        // And the remaining PSD must be created with the quantity in negative
        FIN_AddPayment.createPSD(remainingAmount.negate(), paymentSchedule,
            lastRemainingPSD.getInvoicePaymentSchedule(), order.getOrganization(),
            order.getBusinessPartner());
        FIN_AddPayment.updatePaymentDetail(lastRemainingPSD, currentNettingPayment,
            lastRemainingPSD.getAmount(), false);
      }
    }

    return currentNettingPayment;
  }

  private static FIN_PaymentSchedule createPaymentSchedule(final Order order,
      final BigDecimal amount) {
    final FIN_PaymentSchedule paymentSchedule;
    // Create a Payment Schedule if the order hasn't got
    paymentSchedule = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
    paymentSchedule.setClient(order.getClient());
    paymentSchedule.setOrganization(order.getOrganization());
    paymentSchedule.setCurrency(order.getCurrency());
    paymentSchedule.setOrder(order);
    paymentSchedule.setFinPaymentmethod(order.getPaymentMethod());
    paymentSchedule.setAmount(amount);
    paymentSchedule.setOutstandingAmount(amount);
    paymentSchedule.setDueDate(order.getOrderDate());
    paymentSchedule.setExpectedDate(order.getOrderDate());
    if (ModelProvider.getInstance()
        .getEntity(FIN_PaymentSchedule.class)
        .hasProperty("origDueDate")) {
      // This property is checked and set this way to force compatibility with both MP13, MP14
      // and
      // later releases of Openbravo. This property is mandatory and must be set. Check issue
      paymentSchedule.set("origDueDate", paymentSchedule.getDueDate());
    }
    paymentSchedule.setFINPaymentPriority(order.getFINPaymentPriority());
    OBDal.getInstance().save(paymentSchedule);
    return paymentSchedule;
  }

  private static String getOrderDocumentNoLabel() {
    final String language = OBContext.getOBContext().getLanguage().getLanguage();
    return Utility.messageBD(new DalConnectionProvider(false), "OrderDocumentno", language);
  }

  private static String getPaymentDocumentNo(final boolean useOrderDocumentNoForRelatedDocs,
      final Order order, final DocumentType paymentDocumentType) {
    String paymentDocumentNo = null;
    // Get Payment DocumentNo
    final Entity paymentEntity = ModelProvider.getInstance().getEntity(FIN_Payment.class);

    if (useOrderDocumentNoForRelatedDocs) {
      paymentDocumentNo = order.getDocumentNo();
    } else {
      paymentDocumentNo = Utility.getDocumentNo(OBDal.getInstance().getConnection(false),
          new DalConnectionProvider(false), RequestContext.get().getVariablesSecureApp(), "",
          paymentEntity.getTableName(), "",
          paymentDocumentType == null ? "" : paymentDocumentType.getId(), false, true);
    }
    return paymentDocumentNo;
  }

  static boolean getEnableStockReservationsPreferenceValue(final Organization organization) {
    boolean enableStockReservations = false;
    try {
      enableStockReservations = ("Y")
          .equals(Preferences.getPreferenceValue(CancelAndReplaceUtils.ENABLE_STOCK_RESERVATIONS,
              true, OBContext.getOBContext().getCurrentClient(), organization,
              OBContext.getOBContext().getUser(), null, null));
    } catch (PropertyException e1) {
      enableStockReservations = false;
    }
    return enableStockReservations;
  }

  static FIN_PaymentSchedule getPaymentScheduleOfOrder(final Order order) {
    final OBCriteria<FIN_PaymentSchedule> paymentScheduleCriteria = OBDal.getInstance()
        .createCriteria(FIN_PaymentSchedule.class);
    paymentScheduleCriteria.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORDER, order));
    paymentScheduleCriteria
        .add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORGANIZATION, order.getOrganization()));
    paymentScheduleCriteria.setFilterOnReadableOrganization(false);
    paymentScheduleCriteria.setMaxResults(1);
    return (FIN_PaymentSchedule) paymentScheduleCriteria.uniqueResult();
  }

  static Order lockOrder(final Order order) {
    // @formatter:off
    final String hql = 
                  "select c" +
                  "  from Order c" +
                  " where id = :orderId";
    // @formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, Order.class)
        .setParameter("orderId", order.getId())
        .setMaxResults(1)
        .setLockOptions(LockOptions.UPGRADE)
        .uniqueResult();
  }

  static void runCancelAndReplaceOrderHooks(final Order oldOrder, final Order inverseOrder,
      final Optional<List<Order>> newOrdersOptional, final JSONObject jsonOrder) {
    if (areTriggersDisabled(jsonOrder)) {
      TriggerHandler.getInstance().enable();
    }
    try {
      if (newOrdersOptional.isPresent() && newOrdersOptional.get().size() > 1) {
        runCancelAndReplaceOrderHook(oldOrder, inverseOrder, newOrdersOptional.get(), jsonOrder,
            newOrdersOptional.isPresent());
      } else {
        runCancelAndReplaceOrderHook(oldOrder, inverseOrder,
            newOrdersOptional.map(newOrders -> newOrders.get(0)).orElse(null), jsonOrder,
            newOrdersOptional.isPresent());
      }
    } finally {
      if (areTriggersDisabled(jsonOrder)) {
        TriggerHandler.getInstance().disable();
      }
    }
  }

  private static void runCancelAndReplaceOrderHook(final Order oldOrder, final Order inverseOrder,
      final Order newOrder, final JSONObject jsonOrder, final boolean replaceOrder) {
    try {
      WeldUtils.getInstanceFromStaticBeanManager(CancelAndReplaceOrderHookCaller.class)
          .executeHook(replaceOrder, areTriggersDisabled(jsonOrder), oldOrder, newOrder,
              inverseOrder, jsonOrder);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

  private static void runCancelAndReplaceOrderHook(final Order oldOrder, final Order inverseOrder,
      final List<Order> newOrders, final JSONObject jsonOrder, final boolean replaceOrder) {
    try {
      WeldUtils.getInstanceFromStaticBeanManager(CancelAndReplaceOrderHookCaller.class)
          .executeHook(replaceOrder, areTriggersDisabled(jsonOrder), oldOrder, newOrders,
              inverseOrder, jsonOrder);
    } catch (final Exception e) {
      throw new OBException(e);
    }
  }

}
