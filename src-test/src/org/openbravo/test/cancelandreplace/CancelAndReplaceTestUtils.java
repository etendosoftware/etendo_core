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
 * All portions are Copyright (C) 2019 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.cancelandreplace;

import static org.hamcrest.Matchers.comparesEqualTo;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.codec.binary.StringUtils;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceOrderTestData;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceOrderTestData.Line;
import org.openbravo.test.cancelandreplace.data.CancelAndReplaceTestData;

class CancelAndReplaceTestUtils {

  static final String DOCUMENT_NO_PREFIX = "C&R Test ";

  private CancelAndReplaceTestUtils() {

  }

  /***
   * Clone and complete an existing order to use in the test
   * 
   * @return the cloned order
   */
  static Order cloneAndCompleteOrder(CancelAndReplaceTestData testData) {
    Order order = OBDal.getInstance().get(Order.class, testData.getCloneOrderId());
    Order newOrder = (Order) DalUtil.copy(order, false);
    newOrder.setDocumentNo(DOCUMENT_NO_PREFIX + testData.getTestNumber());
    newOrder.setBusinessPartner(
        OBDal.getInstance().get(BusinessPartner.class, testData.getBpartnerId()));
    newOrder.setSummedLineAmount(BigDecimal.ZERO);
    newOrder.setGrandTotalAmount(BigDecimal.ZERO);
    newOrder.setProcessed(false);
    newOrder.setDocumentStatus("DR");
    newOrder.setDocumentAction("CO");
    newOrder.setCancelled(false);
    newOrder.setId(SequenceIdData.getUUID());
    newOrder.setNewOBObject(true);
    OBDal.getInstance().save(newOrder);

    for (int i = 0; i < testData.getNewOrders().size(); i++) {
      OrderLine oldOrderLine = order.getOrderLineList().size() > i ? order.getOrderLineList().get(i)
          : order.getOrderLineList().get(0);
      createOrderLine(newOrder, oldOrderLine, (i + 1) * 10);
    }

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newOrder);

    // Book oldOrder
    callCOrderPost(newOrder);

    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newOrder);

    return newOrder;
  }

  static OrderLine createOrderLine(Order newOrder, OrderLine oldOrderLine, long lineNo) {
    OrderLine newOrderLine = (OrderLine) DalUtil.copy(oldOrderLine, false);
    newOrderLine.setDeliveredQuantity(BigDecimal.ZERO);
    newOrderLine.setInvoicedQuantity(BigDecimal.ZERO);
    newOrderLine.setSalesOrder(newOrder);
    newOrderLine.setReplacedorderline(null);
    newOrderLine.setLineNo(lineNo);
    OBDal.getInstance().save(newOrderLine);

    newOrder.getOrderLineList().add(newOrderLine);
    return newOrderLine;
  }

  static void callCOrderPost(Order order) {
    final List<Object> parameters = new ArrayList<>();
    parameters.add(null);
    parameters.add(order.getId());
    final String procedureName = "c_order_post1";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  static ShipmentInOut createShipmentFromOrder(Order order, String cloneShipmentId,
      CancelAndReplaceTestData testData) {

    // Clone existing Goods Shipment to use new one in the test
    ShipmentInOut cloneShipment = OBDal.getInstance().get(ShipmentInOut.class, cloneShipmentId);
    ShipmentInOut newShipment = (ShipmentInOut) DalUtil.copy(cloneShipment, false);
    newShipment.setDocumentNo(DOCUMENT_NO_PREFIX + testData.getTestNumber());
    newShipment.setBusinessPartner(
        OBDal.getInstance().get(BusinessPartner.class, testData.getBpartnerId()));
    newShipment.setId(SequenceIdData.getUUID());
    newShipment.setNewOBObject(true);
    newShipment.setMovementDate(new Date());
    newShipment.setAccountingDate(new Date());
    newShipment.setCreationDate(new Date());
    newShipment.setUpdated(new Date());
    OBDal.getInstance().save(newShipment);

    for (int i = 0; i < order.getOrderLineList().size(); i++) {
      createShipmentLine(newShipment, order.getOrderLineList().get(i),
          testData.getOldOrder().getLines()[i]);
    }

    OBDal.getInstance().flush();
    // Book shipment
    callMINoutPost(newShipment);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(newShipment);

    return newShipment;
  }

  static void createShipmentLine(ShipmentInOut shipment, OrderLine orderLine, Line lineTestData) {

    // Add a line to the shipment
    ShipmentInOutLine shipmentLine = OBProvider.getInstance().get(ShipmentInOutLine.class);
    Product prod = orderLine.getProduct();
    shipmentLine.setOrganization(orderLine.getOrganization());
    shipmentLine.setProduct(prod);
    shipmentLine.setUOM(orderLine.getUOM());
    // Get first storage bin
    Locator locator1 = shipment.getWarehouse().getLocatorList().get(0);
    shipmentLine.setStorageBin(locator1);
    shipmentLine.setLineNo(orderLine.getLineNo());
    shipmentLine.setSalesOrderLine(orderLine);
    shipmentLine.setShipmentReceipt(shipment);
    shipmentLine.setMovementQuantity(lineTestData.getMovementQty());

    if (prod.getAttributeSet() != null
        && (prod.getUseAttributeSetValueAs() == null
            || !"F".equals(prod.getUseAttributeSetValueAs()))
        && prod.getAttributeSet().isRequireAtLeastOneValue().booleanValue()) {
      // Set fake AttributeSetInstance to transaction line for netting shipment as otherwise it
      // will return an error when the product has an attribute set and
      // "Is Required at Least One Value" property of the attribute set is "Y"
      AttributeSetInstance attr = OBProvider.getInstance().get(AttributeSetInstance.class);
      attr.setAttributeSet(prod.getAttributeSet());
      attr.setDescription("1");
      OBDal.getInstance().save(attr);
      shipmentLine.setAttributeSetValue(attr);
    }

    shipment.getMaterialMgmtShipmentInOutLineList().add(shipmentLine);
    OBDal.getInstance().save(shipmentLine);
  }

  static void callMINoutPost(ShipmentInOut shipment) {
    final List<Object> parameters = new ArrayList<>();
    parameters.add(null);
    parameters.add(shipment.getId());
    final String procedureName = "m_inout_post";
    CallStoredProcedure.getInstance().call(procedureName, parameters, null, true, false);
  }

  static void createOrderPayment(Order oldOrder, CancelAndReplaceTestData testData) {
    DocumentType documentType = FIN_Utility.getDocumentType(oldOrder.getOrganization(), "ARR");
    String strPaymentDocumentNo = FIN_Utility.getDocumentNo(documentType, "FIN_Payment");

    // Get the payment schedule of the order
    FIN_PaymentSchedule paymentSchedule = null;
    OBCriteria<FIN_PaymentSchedule> paymentScheduleCriteria = OBDal.getInstance()
        .createCriteria(FIN_PaymentSchedule.class);
    paymentScheduleCriteria.add(Restrictions.eq(FIN_PaymentSchedule.PROPERTY_ORDER, oldOrder));
    paymentScheduleCriteria.setMaxResults(1);
    paymentSchedule = (FIN_PaymentSchedule) paymentScheduleCriteria.uniqueResult();

    // Get the payment schedule detail of the order
    OBCriteria<FIN_PaymentScheduleDetail> paymentScheduleDetailCriteria = OBDal.getInstance()
        .createCriteria(FIN_PaymentScheduleDetail.class);
    paymentScheduleDetailCriteria.add(
        Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_ORDERPAYMENTSCHEDULE, paymentSchedule));
    // There should be only one with null paymentDetails
    paymentScheduleDetailCriteria
        .add(Restrictions.isNull(FIN_PaymentScheduleDetail.PROPERTY_PAYMENTDETAILS));
    List<FIN_PaymentScheduleDetail> paymentScheduleDetailList = paymentScheduleDetailCriteria
        .list();

    HashMap<String, BigDecimal> paymentScheduleDetailAmount = new HashMap<>();
    String paymentScheduleDetailId = paymentScheduleDetailList.get(0).getId();
    paymentScheduleDetailAmount.put(paymentScheduleDetailId,
        testData.getOldOrder().getPaidAmount());

    FIN_FinancialAccount financialAccount = null;

    if (oldOrder.getBusinessPartner().getAccount() != null) {
      financialAccount = oldOrder.getBusinessPartner().getAccount();
    } else {
      financialAccount = FIN_Utility
          .getFinancialAccountPaymentMethod(oldOrder.getPaymentMethod().getId(), null, true,
              oldOrder.getCurrency().getId(), oldOrder.getOrganization().getId())
          .getAccount();
    }

    // Create the payment
    FIN_Payment newPayment = FIN_AddPayment.savePayment(null, true, documentType,
        strPaymentDocumentNo, oldOrder.getBusinessPartner(), oldOrder.getPaymentMethod(),
        financialAccount, testData.getOldOrder().getPaidAmount().toPlainString(),
        oldOrder.getOrderDate(), oldOrder.getOrganization(), null, paymentScheduleDetailList,
        paymentScheduleDetailAmount, false, false, oldOrder.getCurrency(), BigDecimal.ZERO,
        BigDecimal.ZERO);

    // Process the payment
    FIN_PaymentProcess.doProcessPayment(newPayment, "P", null, null);
  }

  static boolean isPreferenceEnabled(String property) {
    try {
      return StringUtils.equals("Y",
          Preferences.getPreferenceValue(property, true,
              OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              null, null));
    } catch (PropertyException e1) {
      return false;
    }
  }

  static void assertOrderHeader(Order order, CancelAndReplaceOrderTestData orderTestData,
      String expectedDocumentNo) {
    assertThat("Order Total amount should be " + orderTestData.getTotalAmount(),
        order.getGrandTotalAmount(), comparesEqualTo(orderTestData.getTotalAmount()));
    assertThat("Order should be " + orderTestData.getStatus(), order.getDocumentStatus(),
        comparesEqualTo(orderTestData.getStatus()));
    assertThat("Order document number should be " + expectedDocumentNo, order.getDocumentNo(),
        comparesEqualTo(expectedDocumentNo));
  }

  static void assertOrderPayment(Order order, CancelAndReplaceOrderTestData orderTestData) {
    FIN_PaymentSchedule paymentScheduleOrder = order.getFINPaymentScheduleList().get(0);
    assertThat("Wrong Order Paid Amount", paymentScheduleOrder.getPaidAmount(),
        comparesEqualTo(orderTestData.getPaidAmount()));
    assertThat("Wrong Order Outstanding Amount", paymentScheduleOrder.getOutstandingAmount(),
        comparesEqualTo(orderTestData.getOutstandingAmount()));
  }

  static void assertOrderLine(OrderLine orderLine, Line line) {
    assertThat("Wrong OrderLine delivered quantity", orderLine.getDeliveredQuantity(),
        comparesEqualTo(line.getDeliveredQty()));

    OBCriteria<ShipmentInOutLine> orderLineShipments = OBDal.getInstance()
        .createCriteria(ShipmentInOutLine.class);
    orderLineShipments.add(Restrictions.eq(ShipmentInOutLine.PROPERTY_SALESORDERLINE, orderLine));
    assertThat("Wrong Orderline Goods shipment lines",
        new BigDecimal(orderLineShipments.list().size()), comparesEqualTo(line.getShipmentLines()));
  }
}
