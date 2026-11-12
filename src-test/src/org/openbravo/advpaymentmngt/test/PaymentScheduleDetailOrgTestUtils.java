/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.businesspartner.Location;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Helpers for {@link PaymentScheduleDetailOrgTest}: they create, complete and pay orders and
 * invoices of F&amp;B España inside the current transaction, and read their payment schedule
 * details.
 */
final class PaymentScheduleDetailOrgTestUtils {

  static final String COMPLETED = "CO";
  static final String DRAFT = "DR";
  static final String WAREHOUSE_NORTE = "B2D40D8A5D644DD89E329DC297309055";
  static final String WAREHOUSE_SUR = "5848641D712545C7AE0FE9634A163648";

  private static final String ORDER_POST = "c_order_post1";
  private static final String INVOICE_POST = "c_invoice_post";
  private static final String CURRENCY_EUR = "102";
  private static final String UOM_UNIT = "100";
  private static final String PAYMENT_TERM_30_DAYS = "66BA1164A7394344BB9CD1A6ECEED05D";
  private static final String PAYMENT_METHOD_TRANSFER = "A97CFD2AFC234B59BB0A72189BD8FC2A";
  private static final String FIN_ACCOUNT_BANK = "DEDDE613C5314ACD8DCC60C474D1A107";
  private static final String PRODUCT_BEER = "DA7FC1BB3BA44EC48EC1AB9C74168CED";
  private static final String PRODUCT_WHITE_WINE = "20EA4222B741434A9203471A3B29C343";
  private static final String PRODUCT_RED_WINE = "FDDBB89508D24D2C8D97A2A57BEA8788";

  static final DocData PURCHASE = new DocData(false, "858B90C7AF0A4533863EEC65437382BF",
      "1F38687D74244B369F0B16B3858CD256", "91AE1E96A30844209CD996917E193BE1",
      "18A499249FF74A68B79510F0AD341141", "AB22CE8FFA5E4AF29F2AC90FCDD400D8",
      "F2EB2EAD2612449A83C28DA84689D78B");
  static final DocData SALES = new DocData(true, "A6750F0D15334FB890C254369AC750A8",
      "6518D3040ED54008A1FC0C09ED140D66", "AEE66281A08F42B6BC509B8A80A33C29",
      "696801EA1AAF46A4AF56E367B40459AE", "466AF4B0136A4A3F9F84129711DA8BD3",
      "7FCD49652E104E6BB06C3A0D787412E3");

  private static final List<OrderLineData> ORDER_LINES = List.of(
      new OrderLineData(10L, PRODUCT_BEER, "100", "1.36"),
      new OrderLineData(20L, PRODUCT_WHITE_WINE, "50", "3.55"),
      new OrderLineData(30L, PRODUCT_RED_WINE, "40", "3.45"));

  private static final AdvPaymentMngtDao DAO = new AdvPaymentMngtDao();

  /** Business partner and documents used for one side (sales or purchase). */
  record DocData(boolean sales, String bpId, String bpLocationId, String priceListId,
      String taxId, String orderDocTypeId, String invoiceDocTypeId) {
  }

  /** Order line to invoice in full, optionally at a different net unit price. */
  record InvoiceLineData(long orderLineNo, String price) {
  }

  /** Order line to create: line number, product, ordered quantity and net unit price. */
  private record OrderLineData(long lineNo, String productId, String qty, String price) {
  }

  private PaymentScheduleDetailOrgTestUtils() {
  }

  // ---------------------------------------------------------------------------------------------
  // Documents
  // ---------------------------------------------------------------------------------------------

  /** Creates the three-line order of the reproduction in the given org and books it. */
  static Order bookOrder(DocData data, String orgId, String warehouseId) {
    Organization org = OBDal.getInstance().get(Organization.class, orgId);
    Warehouse warehouse = OBDal.getInstance().get(Warehouse.class, warehouseId);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, data.orderDocTypeId());
    Date today = new Date();

    Order order = OBProvider.getInstance().get(Order.class);
    order.setClient(OBContext.getOBContext().getCurrentClient());
    order.setOrganization(org);
    order.setDocumentType(docType);
    order.setTransactionDocument(docType);
    order.setDocumentNo(getNewDocumentNo());
    order.setOrderDate(today);
    order.setAccountingDate(today);
    order.setScheduledDeliveryDate(today);
    order.setWarehouse(warehouse);
    order.setBusinessPartner(OBDal.getInstance().get(BusinessPartner.class, data.bpId()));
    order.setPartnerAddress(OBDal.getInstance().get(Location.class, data.bpLocationId()));
    order.setPriceList(OBDal.getInstance().get(PriceList.class, data.priceListId()));
    order.setCurrency(OBDal.getInstance().get(Currency.class, CURRENCY_EUR));
    order.setPaymentMethod(OBDal.getInstance().get(FIN_PaymentMethod.class,
        PAYMENT_METHOD_TRANSFER));
    order.setPaymentTerms(OBDal.getInstance().get(PaymentTerm.class, PAYMENT_TERM_30_DAYS));
    order.setSalesTransaction(data.sales());
    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);
    OBDal.getInstance().save(order);

    for (OrderLineData lineData : ORDER_LINES) {
      OBDal.getInstance().save(createOrderLine(order, lineData, data.taxId()));
    }
    OBDal.getInstance().flush();

    order.setDocumentAction(COMPLETED);
    return processOrder(order);
  }

  /** Creates an invoice in the given org with the given order lines and completes it. */
  static Invoice completeInvoice(DocData data, String orgId, Order order,
      InvoiceLineData... lines) {
    Order currentOrder = reload(order);
    Organization org = OBDal.getInstance().get(Organization.class, orgId);
    DocumentType docType = OBDal.getInstance().get(DocumentType.class, data.invoiceDocTypeId());
    Date today = new Date();

    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setClient(currentOrder.getClient());
    invoice.setOrganization(org);
    invoice.setDocumentType(docType);
    invoice.setTransactionDocument(docType);
    invoice.setDocumentNo(getNewDocumentNo());
    invoice.setInvoiceDate(today);
    invoice.setAccountingDate(today);
    invoice.setTaxDate(today);
    invoice.setBusinessPartner(currentOrder.getBusinessPartner());
    invoice.setPartnerAddress(currentOrder.getPartnerAddress());
    invoice.setPriceList(currentOrder.getPriceList());
    invoice.setCurrency(currentOrder.getCurrency());
    invoice.setPaymentMethod(currentOrder.getPaymentMethod());
    invoice.setPaymentTerms(currentOrder.getPaymentTerms());
    invoice.setSalesTransaction(data.sales());
    invoice.setSalesOrder(currentOrder);
    invoice.setSummedLineAmount(BigDecimal.ZERO);
    invoice.setGrandTotalAmount(BigDecimal.ZERO);
    invoice.setWithholdingamount(BigDecimal.ZERO);
    OBDal.getInstance().save(invoice);

    for (InvoiceLineData lineData : lines) {
      OBDal.getInstance()
          .save(createInvoiceLine(invoice, getOrderLine(currentOrder, lineData.orderLineNo()),
              lineData.price()));
    }
    OBDal.getInstance().flush();

    return processInvoice(invoice, COMPLETED, COMPLETED);
  }

  /** Runs C_Invoice_Post with the given action and checks the resulting status. */
  static Invoice processInvoice(Invoice invoice, String docAction, String expectedStatus) {
    Invoice current = reload(invoice);
    current.setDocumentAction(docAction);
    OBDal.getInstance().save(current);
    OBDal.getInstance().flush();
    callProcedure(INVOICE_POST, current.getId());
    Invoice processed = reload(current);
    assertEquals("Unexpected invoice status after " + docAction, expectedStatus,
        processed.getDocumentStatus());
    return processed;
  }

  static Order reload(Order order) {
    return OBDal.getInstance().get(Order.class, order.getId());
  }

  static Invoice reload(Invoice invoice) {
    return OBDal.getInstance().get(Invoice.class, invoice.getId());
  }

  static InvoiceLineData line(long orderLineNo) {
    return new InvoiceLineData(orderLineNo, null);
  }

  static InvoiceLineData line(long orderLineNo, String price) {
    return new InvoiceLineData(orderLineNo, price);
  }

  private static OrderLine createOrderLine(Order order, OrderLineData lineData, String taxId) {
    BigDecimal qty = new BigDecimal(lineData.qty());
    BigDecimal price = new BigDecimal(lineData.price());
    OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
    orderLine.setClient(order.getClient());
    orderLine.setOrganization(order.getOrganization());
    orderLine.setSalesOrder(order);
    orderLine.setLineNo(lineData.lineNo());
    orderLine.setOrderDate(order.getOrderDate());
    orderLine.setWarehouse(order.getWarehouse());
    orderLine.setCurrency(order.getCurrency());
    orderLine.setProduct(OBDal.getInstance().get(Product.class, lineData.productId()));
    orderLine.setUOM(OBDal.getInstance().get(UOM.class, UOM_UNIT));
    orderLine.setOrderedQuantity(qty);
    orderLine.setInvoicedQuantity(BigDecimal.ZERO);
    orderLine.setUnitPrice(price);
    orderLine.setListPrice(price);
    orderLine.setPriceLimit(BigDecimal.ZERO);
    orderLine.setLineNetAmount(qty.multiply(price));
    orderLine.setTax(OBDal.getInstance().get(TaxRate.class, taxId));
    return orderLine;
  }

  private static InvoiceLine createInvoiceLine(Invoice invoice, OrderLine orderLine,
      String price) {
    BigDecimal qty = orderLine.getOrderedQuantity();
    BigDecimal unitPrice = price == null ? orderLine.getUnitPrice() : new BigDecimal(price);
    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
    invoiceLine.setClient(invoice.getClient());
    invoiceLine.setOrganization(invoice.getOrganization());
    invoiceLine.setInvoice(invoice);
    invoiceLine.setLineNo(orderLine.getLineNo());
    invoiceLine.setSalesOrderLine(orderLine);
    invoiceLine.setProduct(orderLine.getProduct());
    invoiceLine.setUOM(orderLine.getUOM());
    invoiceLine.setInvoicedQuantity(qty);
    invoiceLine.setUnitPrice(unitPrice);
    invoiceLine.setListPrice(unitPrice);
    invoiceLine.setPriceLimit(BigDecimal.ZERO);
    invoiceLine.setLineNetAmount(qty.multiply(unitPrice));
    invoiceLine.setTax(orderLine.getTax());
    return invoiceLine;
  }

  private static OrderLine getOrderLine(Order order, long lineNo) {
    for (OrderLine orderLine : order.getOrderLineList()) {
      if (orderLine.getLineNo() == lineNo) {
        return orderLine;
      }
    }
    throw new IllegalArgumentException("Order line " + lineNo + " not found");
  }

  private static Order processOrder(Order order) {
    callProcedure(ORDER_POST, order.getId());
    Order processed = reload(order);
    assertEquals("Order must be booked", COMPLETED, processed.getDocumentStatus());
    return processed;
  }

  private static void callProcedure(String procedure, String recordId) {
    List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(recordId);
    CallStoredProcedure.getInstance().call(procedure, params, null, true, false);
    OBDal.getInstance().getSession().clear();
  }

  private static String getNewDocumentNo() {
    return "ETP5694-" + UUID.randomUUID().toString().substring(0, 8);
  }

  // ---------------------------------------------------------------------------------------------
  // Payment schedule details
  // ---------------------------------------------------------------------------------------------

  /** Same rule as AddPaymentOrderInvoicesTransformer: the org and its child tree. */
  static Set<String> getVisibleOrgs(Organization org) {
    return new OrganizationStructureProvider().getChildTree(org.getId(), true);
  }

  /** Pending amount of the invoice that Add Payment lists when launched from its org. */
  static BigDecimal getVisiblePendingAmount(Invoice invoice) {
    Set<String> visibleOrgs = getVisibleOrgs(invoice.getOrganization());
    BigDecimal amount = BigDecimal.ZERO;
    for (FIN_PaymentScheduleDetail psd : DAO.getInvoicePendingScheduledPaymentDetails(invoice)) {
      if (visibleOrgs.contains(psd.getOrganization().getId())) {
        amount = amount.add(psd.getAmount()).add(psd.getWriteoffAmount());
      }
    }
    return amount;
  }

  static List<FIN_PaymentScheduleDetail> getOrderOnlyPSDs(Order order) {
    return queryPSDs("orderPaymentSchedule.order.id = :id and invoicePaymentSchedule is null",
        order.getId());
  }

  static List<FIN_PaymentScheduleDetail> getInvoicePSDs(Invoice invoice) {
    return queryPSDs("invoicePaymentSchedule.invoice.id = :id", invoice.getId());
  }

  static List<FIN_PaymentScheduleDetail> getInvoicePSDsFromOrder(Invoice invoice) {
    return queryPSDs(
        "invoicePaymentSchedule.invoice.id = :id and orderPaymentSchedule is not null",
        invoice.getId());
  }

  private static List<FIN_PaymentScheduleDetail> queryPSDs(String whereClause, String id) {
    OBContext.setAdminMode(true);
    try {
      OBQuery<FIN_PaymentScheduleDetail> query = OBDal.getInstance()
          .createQuery(FIN_PaymentScheduleDetail.class,
              "where " + whereClause + " and canceled = false order by amount");
      query.setNamedParameter("id", id);
      return query.list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Payments
  // ---------------------------------------------------------------------------------------------

  /** Pays the given amount of the first pending detail of the order (a prepayment). */
  static void payOrder(Order order, BigDecimal amount, VariablesSecureApp vars) {
    Order current = reload(order);
    List<FIN_PaymentScheduleDetail> psds = DAO.getOrderPendingScheduledPaymentDetails(current);
    Map<String, BigDecimal> amounts = new HashMap<>();
    amounts.put(psds.get(0).getId(), amount);
    pay(current.getOrganization(), current.getBusinessPartner(), current.isSalesTransaction(),
        new ArrayList<>(psds.subList(0, 1)), amounts, amount, vars);
  }

  /** Pays, from the invoice org, every pending detail Add Payment lists for the invoice. */
  static void payVisibleDetails(Invoice invoice, VariablesSecureApp vars) {
    Invoice current = reload(invoice);
    Set<String> visibleOrgs = getVisibleOrgs(current.getOrganization());
    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();
    Map<String, BigDecimal> amounts = new HashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    for (FIN_PaymentScheduleDetail psd : DAO.getInvoicePendingScheduledPaymentDetails(current)) {
      if (visibleOrgs.contains(psd.getOrganization().getId())) {
        psds.add(psd);
        amounts.put(psd.getId(), psd.getAmount());
        total = total.add(psd.getAmount());
      }
    }
    pay(current.getOrganization(), current.getBusinessPartner(), current.isSalesTransaction(),
        psds, amounts, total, vars);
  }

  private static void pay(Organization org, BusinessPartner bp, boolean isReceipt,
      List<FIN_PaymentScheduleDetail> psds, Map<String, BigDecimal> amounts, BigDecimal total,
      VariablesSecureApp vars) {
    try {
      FIN_Payment payment = FIN_AddPayment.savePayment(null, isReceipt,
          FIN_Utility.getDocumentType(org, isReceipt ? "ARR" : "APP"), getNewDocumentNo(), bp,
          OBDal.getInstance().get(FIN_PaymentMethod.class, PAYMENT_METHOD_TRANSFER),
          OBDal.getInstance().get(FIN_FinancialAccount.class, FIN_ACCOUNT_BANK),
          total.toPlainString(), new Date(), org, null, psds, new HashMap<>(amounts), false,
          false);
      OBDal.getInstance().flush();
      OBError result = FIN_AddPayment.processPayment(vars, new DalConnectionProvider(false), "P",
          payment);
      assertNotEquals("Payment must be processed: " + result.getMessage(), "Error",
          result.getType());
      OBDal.getInstance().flush();
      OBDal.getInstance().getSession().clear();
    } catch (Exception e) {
      throw new IllegalStateException("Error paying " + total, e);
    }
  }
}
