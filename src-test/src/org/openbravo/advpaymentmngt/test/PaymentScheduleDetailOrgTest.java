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
import static org.junit.Assert.assertTrue;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.COMPLETED;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.DRAFT;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.PURCHASE;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.SALES;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.WAREHOUSE_NORTE;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.WAREHOUSE_SUR;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.bookOrder;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.completeInvoice;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.getInvoicePSDs;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.getInvoicePSDsFromOrder;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.getOrderOnlyPSDs;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.getVisibleOrgs;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.getVisiblePendingAmount;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.line;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.payOrder;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.payVisibleDetails;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.processInvoice;
import static org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.reload;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.test.PaymentScheduleDetailOrgTestUtils.DocData;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
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

  private static final BigDecimal PREPAYMENT = new BigDecimal("100.00");

  private VariablesSecureApp vars;

  /** Documents of the reported scenario. */
  private static final class ReportedScenario {
    private final Order order;
    private final Invoice firstInvoice;
    private final Invoice secondInvoice;

    ReportedScenario(Order order, Invoice firstInvoice, Invoice secondInvoice) {
      this.order = order;
      this.firstInvoice = firstInvoice;
      this.secondInvoice = secondInvoice;
    }

    Order order() {
      return order;
    }

    Invoice firstInvoice() {
      return firstInvoice;
    }

    Invoice secondInvoice() {
      return secondInvoice;
    }
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
    payOrder(order, PREPAYMENT, vars);

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

    payVisibleDetails(invoice, vars);

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
    Set<String> visibleOrgs = getVisibleOrgs(current.getOrganization());
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
}
