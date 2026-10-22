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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
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
import org.openbravo.test.base.TestConstants.Clients;
import org.openbravo.test.base.TestConstants.Orgs;
import org.openbravo.test.base.TestConstants.Roles;
import org.openbravo.test.base.TestConstants.Users;

/**
 * Regression tests for ETP-5694 (GitHub etendo_core#1172): when an order is invoiced from
 * different organizations, APRM_GEN_PAYMENTSCHEDULE_INV must leave every payment schedule detail
 * in an organization visible from the document it belongs to, so Add Payment lists the full
 * outstanding amount of the invoice.
 *
 * Every document is created and completed inside the test transaction, which is rolled back at
 * the end of each test.
 */
public class PaymentScheduleDetailOrgTest extends WeldBaseTest {

  private static final String ORDER_POST = "c_order_post1";
  private static final String INVOICE_POST = "c_invoice_post";
  private static final String COMPLETED = "CO";
  private static final String DRAFT = "DR";

  private static final String CURRENCY_EUR = "102";
  private static final String UOM_UNIT = "100";
  private static final String PAYMENT_TERM_30_DAYS = "66BA1164A7394344BB9CD1A6ECEED05D";
  private static final String PAYMENT_METHOD_TRANSFER = "A97CFD2AFC234B59BB0A72189BD8FC2A";
  private static final String FIN_ACCOUNT_BANK = "DEDDE613C5314ACD8DCC60C474D1A107";
  private static final String WAREHOUSE_NORTE = "B2D40D8A5D644DD89E329DC297309055";
  private static final String WAREHOUSE_SUR = "5848641D712545C7AE0FE9634A163648";

  private static final String PRODUCT_BEER = "DA7FC1BB3BA44EC48EC1AB9C74168CED";
  private static final String PRODUCT_WHITE_WINE = "20EA4222B741434A9203471A3B29C343";
  private static final String PRODUCT_RED_WINE = "FDDBB89508D24D2C8D97A2A57BEA8788";

  private static final DocData PURCHASE = new DocData(false, "858B90C7AF0A4533863EEC65437382BF",
      "1F38687D74244B369F0B16B3858CD256", "91AE1E96A30844209CD996917E193BE1",
      "18A499249FF74A68B79510F0AD341141", "AB22CE8FFA5E4AF29F2AC90FCDD400D8",
      "F2EB2EAD2612449A83C28DA84689D78B");
  private static final DocData SALES = new DocData(true, "A6750F0D15334FB890C254369AC750A8",
      "6518D3040ED54008A1FC0C09ED140D66", "AEE66281A08F42B6BC509B8A80A33C29",
      "696801EA1AAF46A4AF56E367B40459AE", "466AF4B0136A4A3F9F84129711DA8BD3",
      "7FCD49652E104E6BB06C3A0D787412E3");

  private static final List<OrderLineData> ORDER_LINES = List.of(
      new OrderLineData(10L, PRODUCT_BEER, "100", "1.36"),
      new OrderLineData(20L, PRODUCT_WHITE_WINE, "50", "3.55"),
      new OrderLineData(30L, PRODUCT_RED_WINE, "40", "3.45"));

  private static final BigDecimal PREPAYMENT = new BigDecimal("100.00");

  private final AdvPaymentMngtDao dao = new AdvPaymentMngtDao();
  private VariablesSecureApp vars;

  /** Business partner and documents used for one side (sales or purchase). */
  private record DocData(boolean sales, String bpId, String bpLocationId, String priceListId,
      String taxId, String orderDocTypeId, String invoiceDocTypeId) {
  }

  /** Order line to create: line number, product, ordered quantity and net unit price. */
  private record OrderLineData(long lineNo, String productId, String qty, String price) {
  }

  /** Order line to invoice in full, optionally at a different net unit price. */
  private record InvoiceLineData(long orderLineNo, String price) {
  }

  /** Documents of the reported scenario. */
  private record ReportedScenario(Order order, Invoice firstInvoice, Invoice secondInvoice) {
  }

  @Before
  public void setUpContext() {
    OBContext.setOBContext(Users.ADMIN, Roles.FB_GRP_ADMIN, Clients.FB_GRP, Orgs.ESP);
    vars = new VariablesSecureApp(Users.ADMIN, Clients.FB_GRP, Orgs.ESP, Roles.FB_GRP_ADMIN);
    RequestContext.get().setVariableSecureApp(vars);
    vars.setSessionValue("#FormatOutput|generalQtyEdition", "#0.######");
    vars.setSessionValue("#GroupSeparator|generalQtyEdition", ",");
    vars.setSessionValue("#DecimalSeparator|generalQtyEdition", ".");
  }

  @After
  public void rollback() {
    OBDal.getInstance().rollbackAndClose();
  }

  /**
   * Order at the store, partial invoice at its legal entity, rest invoiced at the store at a
   * higher price (the reported case). The remainder of the split stays at the order org and the
   * store invoice shows its full outstanding amount.
   */
  @Test
  public void purchasePartialInvoiceInParentOrgKeepsRemainderInOrderOrg() {
    assertReportedScenario(PURCHASE);
  }

  /** Same as the reported case, on the sales side. */
  @Test
  public void salesPartialInvoiceInParentOrgKeepsRemainderInOrderOrg() {
    assertReportedScenario(SALES);
  }

  /**
   * Order at the legal entity, invoiced in two parts at the store. Both store invoices show their
   * full outstanding amount.
   */
  @Test
  public void purchasePartialInvoicesInChildOrgOfParentOrder() {
    Order order = bookOrder(PURCHASE, Orgs.ESP, WAREHOUSE_NORTE);
    Invoice firstInvoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, order, line(10L),
        line(20L));

    assertFullyVisibleFromInvoiceOrg(firstInvoice);
    assertOrgs(Orgs.ESP, getOrderOnlyPSDs(order));

    Invoice secondInvoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, order, line(30L));
    assertFullyVisibleFromInvoiceOrg(secondInvoice);
  }

  /**
   * Order at the legal entity fully invoiced at the store, and order at a sibling store partially
   * invoiced at the store.
   */
  @Test
  public void purchaseInvoiceInChildOrSiblingOrgOfOrder() {
    Order parentOrder = bookOrder(PURCHASE, Orgs.ESP, WAREHOUSE_NORTE);
    Invoice fullInvoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, parentOrder, line(10L),
        line(20L), line(30L));
    assertFullyVisibleFromInvoiceOrg(fullInvoice);

    Order siblingOrder = bookOrder(PURCHASE, Orgs.ESP_SUR, WAREHOUSE_SUR);
    Invoice partialInvoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, siblingOrder, line(10L),
        line(20L));
    assertFullyVisibleFromInvoiceOrg(partialInvoice);
    assertOrgs(Orgs.ESP_SUR, getOrderOnlyPSDs(siblingOrder));
  }

  /**
   * Guard: when the payment schedule details are already visible from the invoice org, their
   * organization does not change.
   */
  @Test
  public void sameOrgAndParentOrgInvoicesKeepOrderOrg() {
    Order sameOrgOrder = bookOrder(PURCHASE, Orgs.ESP_NORTE, WAREHOUSE_NORTE);
    Invoice sameOrgInvoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, sameOrgOrder, line(10L),
        line(20L));
    assertOrgs(Orgs.ESP_NORTE, getInvoicePSDs(sameOrgInvoice));
    assertOrgs(Orgs.ESP_NORTE, getOrderOnlyPSDs(sameOrgOrder));

    Order childOrder = bookOrder(PURCHASE, Orgs.ESP_NORTE, WAREHOUSE_NORTE);
    Invoice parentInvoice = completeInvoice(PURCHASE, Orgs.ESP, childOrder, line(10L), line(20L),
        line(30L));
    assertOrgs(Orgs.ESP_NORTE, getInvoicePSDsFromOrder(parentInvoice));
    assertFullyVisibleFromInvoiceOrg(parentInvoice);
  }

  /**
   * Reactivating the invoice merges the order payment schedule details back into one detail at
   * the order org; completing it again leaves it visible from the invoice org.
   */
  @Test
  public void reactivateInvoiceRestoresOrderOrg() {
    Order order = bookOrder(PURCHASE, Orgs.ESP, WAREHOUSE_NORTE);
    Invoice invoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, order, line(10L), line(20L));

    invoice = processInvoice(invoice, "RE", DRAFT);

    List<FIN_PaymentScheduleDetail> orderPSDs = getOrderOnlyPSDs(order);
    assertEquals("Order details must be merged back into one", 1, orderPSDs.size());
    assertOrgs(Orgs.ESP, orderPSDs);
    assertAmount("Merged detail must hold the whole order", reload(order).getGrandTotalAmount(),
        orderPSDs.get(0).getAmount());

    invoice = processInvoice(invoice, COMPLETED, COMPLETED);
    assertFullyVisibleFromInvoiceOrg(invoice);
  }

  /**
   * A detail already paid by a prepayment of the order keeps the order org; the unpaid rest
   * becomes visible from the invoice org.
   */
  @Test
  public void prepaidOrderDetailKeepsOrderOrg() {
    Order order = bookOrder(PURCHASE, Orgs.ESP, WAREHOUSE_NORTE);
    payOrder(order, PREPAYMENT);

    Invoice invoice = completeInvoice(PURCHASE, Orgs.ESP_NORTE, order, line(10L), line(20L),
        line(30L));

    List<FIN_PaymentScheduleDetail> paid = new ArrayList<>();
    for (FIN_PaymentScheduleDetail psd : getInvoicePSDs(invoice)) {
      if (psd.getPaymentDetails() != null) {
        paid.add(psd);
      }
    }
    assertEquals("The prepaid detail must be linked to the invoice", 1, paid.size());
    assertOrgs(Orgs.ESP, paid);
    assertAmount("Pending amount must exclude the prepayment",
        invoice.getGrandTotalAmount().subtract(PREPAYMENT), getVisiblePendingAmount(invoice));
    assertFullyVisibleFromInvoiceOrg(invoice);
  }

  /** Paying from the store everything Add Payment lists for the store invoice settles it. */
  @Test
  public void payFullInvoiceFromInvoiceOrgLeavesNothingOutstanding() {
    Invoice invoice = createReportedScenario(PURCHASE).secondInvoice();

    payVisibleDetails(invoice);

    invoice = reload(invoice);
    assertAmount("Invoice must be fully paid", BigDecimal.ZERO, invoice.getOutstandingAmount());
    assertTrue("Invoice must be marked as paid", invoice.isPaymentComplete());
  }

  private void assertReportedScenario(DocData data) {
    ReportedScenario scenario = createReportedScenario(data);
    Order order = reload(scenario.order());

    List<FIN_PaymentScheduleDetail> remainder = getOrderOnlyPSDs(order);
    assertEquals("The order must not keep any detail without invoice", 0, remainder.size());
    assertFullyVisibleFromInvoiceOrg(scenario.firstInvoice());
    assertFullyVisibleFromInvoiceOrg(scenario.secondInvoice());
  }

  private ReportedScenario createReportedScenario(DocData data) {
    Order order = bookOrder(data, Orgs.ESP_NORTE, WAREHOUSE_NORTE);
    Invoice firstInvoice = completeInvoice(data, Orgs.ESP, order, line(10L), line(20L));

    List<FIN_PaymentScheduleDetail> remainder = getOrderOnlyPSDs(order);
    assertEquals("The split must leave one order-only detail", 1, remainder.size());
    assertOrgs(Orgs.ESP_NORTE, remainder);
    assertAmount("The remainder must hold the not invoiced amount",
        reload(order).getGrandTotalAmount().subtract(firstInvoice.getGrandTotalAmount()),
        remainder.get(0).getAmount());

    Invoice secondInvoice = completeInvoice(data, Orgs.ESP_NORTE, order, line(30L, "3.50"));
    return new ReportedScenario(order, firstInvoice, secondInvoice);
  }

  // ---------------------------------------------------------------------------------------------
  // Assertions
  // ---------------------------------------------------------------------------------------------

  private void assertFullyVisibleFromInvoiceOrg(Invoice invoice) {
    Invoice current = reload(invoice);
    String invoiceOrgId = current.getOrganization().getId();
    Set<String> visibleOrgs = new OrganizationStructureProvider().getChildTree(invoiceOrgId,
        true);
    for (FIN_PaymentScheduleDetail psd : getInvoicePSDs(current)) {
      if (psd.getPaymentDetails() == null) {
        assertTrue("Detail of " + psd.getAmount() + " must be visible from the invoice org",
            visibleOrgs.contains(psd.getOrganization().getId()));
      }
    }
    assertAmount("Add Payment must list the whole outstanding amount",
        current.getOutstandingAmount(), getVisiblePendingAmount(current));
  }

  private void assertOrgs(String expectedOrgId, List<FIN_PaymentScheduleDetail> psds) {
    assertFalse("Expected payment schedule details to check", psds.isEmpty());
    for (FIN_PaymentScheduleDetail psd : psds) {
      assertEquals("Unexpected organization for the detail of " + psd.getAmount(), expectedOrgId,
          psd.getOrganization().getId());
    }
  }

  private void assertAmount(String message, BigDecimal expected, BigDecimal actual) {
    assertEquals(message + " (expected " + expected + ", actual " + actual + ")", 0,
        expected.compareTo(actual));
  }

  /** Same rule as AddPaymentOrderInvoicesTransformer: details in the child tree of the org. */
  private BigDecimal getVisiblePendingAmount(Invoice invoice) {
    Set<String> visibleOrgs = new OrganizationStructureProvider()
        .getChildTree(invoice.getOrganization().getId(), true);
    BigDecimal amount = BigDecimal.ZERO;
    for (FIN_PaymentScheduleDetail psd : dao.getInvoicePendingScheduledPaymentDetails(invoice)) {
      if (visibleOrgs.contains(psd.getOrganization().getId())) {
        amount = amount.add(psd.getAmount()).add(psd.getWriteoffAmount());
      }
    }
    return amount;
  }

  // ---------------------------------------------------------------------------------------------
  // Payment schedule detail queries
  // ---------------------------------------------------------------------------------------------

  private List<FIN_PaymentScheduleDetail> getOrderOnlyPSDs(Order order) {
    return queryPSDs("orderPaymentSchedule.order.id = :id and invoicePaymentSchedule is null",
        order.getId());
  }

  private List<FIN_PaymentScheduleDetail> getInvoicePSDs(Invoice invoice) {
    return queryPSDs("invoicePaymentSchedule.invoice.id = :id", invoice.getId());
  }

  private List<FIN_PaymentScheduleDetail> getInvoicePSDsFromOrder(Invoice invoice) {
    return queryPSDs(
        "invoicePaymentSchedule.invoice.id = :id and orderPaymentSchedule is not null",
        invoice.getId());
  }

  private List<FIN_PaymentScheduleDetail> queryPSDs(String whereClause, String id) {
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
  // Documents
  // ---------------------------------------------------------------------------------------------

  private Order bookOrder(DocData data, String orgId, String warehouseId) {
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

  private OrderLine createOrderLine(Order order, OrderLineData lineData, String taxId) {
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

  private Invoice completeInvoice(DocData data, String orgId, Order order,
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

  private InvoiceLine createInvoiceLine(Invoice invoice, OrderLine orderLine, String price) {
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

  private OrderLine getOrderLine(Order order, long lineNo) {
    for (OrderLine orderLine : order.getOrderLineList()) {
      if (orderLine.getLineNo() == lineNo) {
        return orderLine;
      }
    }
    throw new IllegalArgumentException("Order line " + lineNo + " not found");
  }

  private Order processOrder(Order order) {
    callProcedure(ORDER_POST, order.getId());
    Order processed = reload(order);
    assertEquals("Order must be booked", COMPLETED, processed.getDocumentStatus());
    return processed;
  }

  private Invoice processInvoice(Invoice invoice, String docAction, String expectedStatus) {
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

  private void callProcedure(String procedure, String recordId) {
    List<Object> params = new ArrayList<>();
    params.add(null);
    params.add(recordId);
    CallStoredProcedure.getInstance().call(procedure, params, null, true, false);
    OBDal.getInstance().getSession().clear();
  }

  private Order reload(Order order) {
    return OBDal.getInstance().get(Order.class, order.getId());
  }

  private Invoice reload(Invoice invoice) {
    return OBDal.getInstance().get(Invoice.class, invoice.getId());
  }

  private static InvoiceLineData line(long orderLineNo) {
    return new InvoiceLineData(orderLineNo, null);
  }

  private static InvoiceLineData line(long orderLineNo, String price) {
    return new InvoiceLineData(orderLineNo, price);
  }

  private static String getNewDocumentNo() {
    return "ETP5694-" + UUID.randomUUID().toString().substring(0, 8);
  }

  // ---------------------------------------------------------------------------------------------
  // Payments
  // ---------------------------------------------------------------------------------------------

  private void payOrder(Order order, BigDecimal amount) {
    Order current = reload(order);
    List<FIN_PaymentScheduleDetail> psds = dao.getOrderPendingScheduledPaymentDetails(current);
    Map<String, BigDecimal> amounts = new HashMap<>();
    amounts.put(psds.get(0).getId(), amount);
    pay(current.getOrganization(), current.getBusinessPartner(), current.isSalesTransaction(),
        new ArrayList<>(psds.subList(0, 1)), amounts, amount);
  }

  private void payVisibleDetails(Invoice invoice) {
    Invoice current = reload(invoice);
    Set<String> visibleOrgs = new OrganizationStructureProvider()
        .getChildTree(current.getOrganization().getId(), true);
    List<FIN_PaymentScheduleDetail> psds = new ArrayList<>();
    Map<String, BigDecimal> amounts = new HashMap<>();
    BigDecimal total = BigDecimal.ZERO;
    for (FIN_PaymentScheduleDetail psd : dao.getInvoicePendingScheduledPaymentDetails(current)) {
      if (visibleOrgs.contains(psd.getOrganization().getId())) {
        psds.add(psd);
        amounts.put(psd.getId(), psd.getAmount());
        total = total.add(psd.getAmount());
      }
    }
    pay(current.getOrganization(), current.getBusinessPartner(), current.isSalesTransaction(),
        psds, amounts, total);
  }

  private void pay(Organization org, BusinessPartner bp, boolean isReceipt,
      List<FIN_PaymentScheduleDetail> psds, Map<String, BigDecimal> amounts, BigDecimal total) {
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
