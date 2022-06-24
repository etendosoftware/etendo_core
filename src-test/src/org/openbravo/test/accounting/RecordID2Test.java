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
 * All portions are Copyright (C) 2014 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.accounting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.financial.ResetAccounting;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests cases to check RecordID2-DateBalanced functionality (Open balances Project)
 * 
 * 
 */
public class RecordID2Test extends OBBaseTest {
  final static private Logger log = LogManager.getLogger();
  private static final String TABLE_INVOICE = "318";
  private static final String TABLE_TRANSACTION = "4D8C3B3C31D1410DA046140C9F024D17";
  private static final String TABLE_PAYMENT = "D1A97202E832470285C9B1EB026D54E2";
  private static final String TABLE_RECONCILIATION = "B1B7075C46934F0A9FD4C4D0F1457B42";

  /**
   * Verifies documents can be posted and unposted and that affects fact_acct (RecordID2 and
   * DateBalanced) Case 1: Invoice posted and just reconciliation posted. After both are posted
   * receivables entry should be balanced Invoice: I/1 Reconciliation: 1000032
   */
  @Test
  public void testRecordID2_case1() {
    // Invoice I/1 from QA dataset
    String strCInvoiceId = "FF808081328278B5013282A1A35800A0";
    String strCAcctSchemaId = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";

    OBContext.setAdminMode(true);
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strRecordID2 = invoice.getFINPaymentScheduleList().get(0).getId();
      assertEquals("Invoice Not Posted", "Y", invoice.getPosted());
      ResetAccounting.delete(invoice.getClient().getId(), invoice.getOrganization().getId(), "318",
          invoice.getId(), "", "");
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strReceivablesAccountId = getReceivablesAccount(invoice.getBusinessPartner(),
          strCAcctSchemaId);
      assertEquals("Invoice Could Not Be Unposted", "N", invoice.getPosted());
      assertTrue("RecordID2 not updated after unposting invoice", BigDecimal.ZERO.compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      postDocument(strCInvoiceId, "318", invoice.getClient().getId(),
          invoice.getOrganization().getId());
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      assertTrue("RecordID2 not updated after posting invoice", new BigDecimal("100").compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Invoice Could Not be Posted", "Y", invoice.getPosted());

      // Reconciliation: 1000032 - 02-07-2011 from QA Dataset. Paying previous invoice
      String strFINReconciliationId = "FF808081328278B5013282A3F67700C5";
      FIN_Reconciliation reconciliation = OBDal.getInstance()
          .get(FIN_Reconciliation.class, strFINReconciliationId);
      assertEquals("Reconciliation Not Posted", "Y", reconciliation.getPosted());
      ResetAccounting.delete(reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId(), TABLE_RECONCILIATION, reconciliation.getId(),
          "", "");
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, strFINReconciliationId);
      assertEquals("Reconciliation Could Not Be Unposted", "N", reconciliation.getPosted());
      assertTrue("RecordID2 not updated after unposting reconciliation",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2, TABLE_RECONCILIATION, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      postDocument(strFINReconciliationId, TABLE_RECONCILIATION, reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId());
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, strFINReconciliationId);
      assertTrue("RecordID2 not updated after posting reconciliation",
          new BigDecimal("-100").compareTo(getBalance(strRecordID2, TABLE_RECONCILIATION,
              strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Reconciliation Could Not be Posted", "Y", reconciliation.getPosted());
      assertEquals("Entry Not Balanced",
          DateUtils.truncate(
              reconciliation.getAPRMFinaccTransactionVList().get(0).getTransactionDate(),
              Calendar.DATE),
          getBalancedDate(strRecordID2, strReceivablesAccountId));
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Verifies documents can be posted and unposted and that affects fact_acct (RecordID2 and
   * DateBalanced) Case 2: Invoice posted and transaction posted. After both are posted receivables
   * entry should be balanced Invoice: I/10 Transaction: line 50 - 400012 - 02-04-2011 - Customer A
   * - 146.00
   */
  @Test
  public void testRecordID2_case2() {
    // Invoice I/10 from QA dataset
    String strCInvoiceId = "FF8080813285D49A0132874B35C7030C";
    String strCAcctSchemaId = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";

    OBContext.setAdminMode(true);
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strRecordID2 = invoice.getFINPaymentScheduleList().get(0).getId();
      assertEquals("Invoice Not Posted", "Y", invoice.getPosted());
      ResetAccounting.delete(invoice.getClient().getId(), invoice.getOrganization().getId(), "318",
          invoice.getId(), "", "");
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strReceivablesAccountId = getReceivablesAccount(invoice.getBusinessPartner(),
          strCAcctSchemaId);
      assertEquals("Invoice Could Not Be Unposted", "N", invoice.getPosted());
      assertTrue("RecordID2 not updated after unposting invoice", BigDecimal.ZERO.compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      postDocument(strCInvoiceId, "318", invoice.getClient().getId(),
          invoice.getOrganization().getId());
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      assertTrue("RecordID2 not updated after posting invoice", new BigDecimal("146").compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Invoice Could Not be Posted", "Y", invoice.getPosted());

      // Transaction: 1000032 - 02-07-2011 from QA Dataset. Paying previous invoice
      String strFINFinaccTransactionId = "FF8080813285D49A013287518E1C033E";
      FIN_FinaccTransaction transaction = OBDal.getInstance()
          .get(FIN_FinaccTransaction.class, strFINFinaccTransactionId);
      assertEquals("Transaction Not Posted", "Y", transaction.getPosted());
      ResetAccounting.delete(transaction.getClient().getId(), transaction.getOrganization().getId(),
          TABLE_TRANSACTION, transaction.getId(), "", "");
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, strFINFinaccTransactionId);
      assertEquals("Transaction Could Not Be Unposted", "N", transaction.getPosted());
      assertTrue("RecordID2 not updated after unposting transaction",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2, TABLE_TRANSACTION, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      postDocument(strFINFinaccTransactionId, TABLE_TRANSACTION, transaction.getClient().getId(),
          transaction.getOrganization().getId());
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, strFINFinaccTransactionId);
      assertTrue("RecordID2 not updated after posting transaction",
          new BigDecimal("-146").compareTo(getBalance(strRecordID2, TABLE_TRANSACTION,
              strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Transaction Could Not be Posted", "Y", transaction.getPosted());
      assertEquals("Entry Not Balanced",
          DateUtils.truncate(transaction.getTransactionDate(), Calendar.DATE),
          getBalancedDate(strRecordID2, strReceivablesAccountId));
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Verifies documents can be posted and unposted and that affects fact_acct (RecordID2 and
   * DateBalanced) Case 3: Invoice posted and payment posted. After both are posted receivables
   * entry should be balanced Invoice: I/11 Payment: 400013 - 02-04-2011 - Customer A - 30.00 Then
   * transaction is posted and in transit balance is checked
   */
  @Test
  public void testRecordID2_case3() {
    // Invoice I/11 from QA dataset
    String strCInvoiceId = "FF8080813285D49A0132875C1EF60374";
    String strCAcctSchemaId = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";

    OBContext.setAdminMode(true);
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strRecordID2 = invoice.getFINPaymentScheduleList().get(0).getId();
      assertEquals("Invoice Not Posted", "Y", invoice.getPosted());
      ResetAccounting.delete(invoice.getClient().getId(), invoice.getOrganization().getId(), "318",
          invoice.getId(), "", "");
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strReceivablesAccountId = getReceivablesAccount(invoice.getBusinessPartner(),
          strCAcctSchemaId);
      assertEquals("Invoice Could Not Be Unposted", "N", invoice.getPosted());
      assertTrue("RecordID2 not updated after unposting invoice", BigDecimal.ZERO.compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      postDocument(strCInvoiceId, "318", invoice.getClient().getId(),
          invoice.getOrganization().getId());
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);

      assertTrue("RecordID2 not updated after posting invoice", new BigDecimal("30").compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Invoice Could Not be Posted", "Y", invoice.getPosted());

      // Payment: 400013 - 02-04-2011 - Customer A - 30.00 from QA Dataset. Paying previous invoice
      String strFINPaymentId = "FF8080813285D49A0132875DB5780380";
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);
      String strRecordID2InTransit = payment.getId();
      String strInTransitAccountId = getInTransitAccount(payment.getAccount(), strCAcctSchemaId);
      assertEquals("Payment Not Posted", "Y", payment.getPosted());
      ResetAccounting.delete(payment.getClient().getId(), payment.getOrganization().getId(),
          TABLE_PAYMENT, payment.getId(), "", "");
      payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);

      assertEquals("Payment Could Not Be Unposted", "N", payment.getPosted());
      assertTrue("RecordID2 for receivables not updated after unposting Payment",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2, TABLE_PAYMENT, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for In Transit not updated after unposting Payment",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_PAYMENT,
              strCAcctSchemaId, strInTransitAccountId)) == 0);

      postDocument(strFINPaymentId, TABLE_PAYMENT, payment.getClient().getId(),
          payment.getOrganization().getId());
      payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);

      assertTrue("RecordID2 for receivables not updated after posting Payment",
          new BigDecimal("-30").compareTo(getBalance(strRecordID2, TABLE_PAYMENT, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for In Transit not updated after posting Payment",
          new BigDecimal("30").compareTo(getBalance(strRecordID2InTransit, TABLE_PAYMENT,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Payment Could Not be Posted", "Y", payment.getPosted());
      assertEquals("Entry Not Balanced for Receivables",
          DateUtils.truncate(payment.getPaymentDate(), Calendar.DATE),
          getBalancedDate(strRecordID2, strReceivablesAccountId));
      FIN_FinaccTransaction transaction = payment.getFINFinaccTransactionList().get(0);
      assertEquals("Transaction Not Posted", "Y", transaction.getPosted());
      ResetAccounting.delete(transaction.getClient().getId(), transaction.getOrganization().getId(),
          TABLE_TRANSACTION, transaction.getId(), "", "");
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, transaction.getId());
      assertEquals("Transaction Could Not Be Unposted", "N", transaction.getPosted());
      assertTrue("RecordID2 not updated after unposting transaction",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_TRANSACTION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      postDocument(transaction.getId(), TABLE_TRANSACTION, transaction.getClient().getId(),
          transaction.getOrganization().getId());
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, transaction.getId());
      assertTrue("RecordID2 not updated after posting transaction",
          new BigDecimal("-30").compareTo(getBalance(strRecordID2InTransit, TABLE_TRANSACTION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Transaction Could Not be Posted", "Y", transaction.getPosted());
      assertEquals("Entry Not Balanced for In Transit ",
          DateUtils.truncate(transaction.getTransactionDate(), Calendar.DATE),
          getBalancedDate(strRecordID2InTransit, strInTransitAccountId));
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Verifies documents can be posted and unposted and that affects fact_acct (RecordID2 and
   * DateBalanced) Case 4: Invoice posted , transaction posted and reconciliation posted. After both
   * are posted receivables entry should be balanced Invoice: I/14 Transaction: Accounting Documents
   * EURO - EUR - 400016 - 02-04-2011 - Customer A - 40.00 - 0 Reconciliation: 1000042 . When
   * Transaction and reconciliation are posted in transit account balance should be 0
   */
  @Test
  public void testRecordID2_case4() {
    // Invoice I/14 from QA dataset
    String strCInvoiceId = "FF808081328B2FE901328B5AE4030098";
    String strCAcctSchemaId = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";

    OBContext.setAdminMode(true);
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strRecordID2 = invoice.getFINPaymentScheduleList().get(0).getId();
      assertEquals("Invoice Not Posted", "Y", invoice.getPosted());
      ResetAccounting.delete(invoice.getClient().getId(), invoice.getOrganization().getId(), "318",
          invoice.getId(), "", "");
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strReceivablesAccountId = getReceivablesAccount(invoice.getBusinessPartner(),
          strCAcctSchemaId);
      assertEquals("Invoice Could Not Be Unposted", "N", invoice.getPosted());
      assertTrue("RecordID2 not updated after unposting invoice", BigDecimal.ZERO.compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      postDocument(strCInvoiceId, "318", invoice.getClient().getId(),
          invoice.getOrganization().getId());
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      assertTrue("RecordID2 not updated after posting invoice", new BigDecimal("50").compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Invoice Could Not be Posted", "Y", invoice.getPosted());

      // Payment: 400016 - 02-04-2011 - Customer A - 40.00 from QA Dataset. Paying previous invoice
      String strFINPaymentId = "FF808081328B2FE901328B5D698000B1";
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);
      String strInTransitAccountId = getDepositAccount(payment.getAccount(), strCAcctSchemaId);
      FIN_FinaccTransaction transaction = payment.getFINFinaccTransactionList().get(0);
      String strRecordID2InTransit = transaction.getId();
      assertEquals("Transaction Not Posted", "Y", transaction.getPosted());
      ResetAccounting.delete(transaction.getClient().getId(), transaction.getOrganization().getId(),
          TABLE_TRANSACTION, transaction.getId(), "", "");
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, transaction.getId());
      assertEquals("Transaction Could Not Be Unposted", "N", transaction.getPosted());
      assertTrue("RecordID2 for receivables not updated after unposting transaction",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2, TABLE_TRANSACTION, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for in transit not updated after unposting transaction",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_TRANSACTION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      postDocument(transaction.getId(), TABLE_TRANSACTION, transaction.getClient().getId(),
          transaction.getOrganization().getId());
      transaction = OBDal.getInstance().get(FIN_FinaccTransaction.class, transaction.getId());
      assertTrue("RecordID2 for receivables not updated after posting transaction",
          new BigDecimal("-50").compareTo(getBalance(strRecordID2, TABLE_TRANSACTION,
              strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for in transit not updated after posting transaction",
          new BigDecimal("40").compareTo(getBalance(strRecordID2InTransit, TABLE_TRANSACTION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Transaction Could Not be Posted", "Y", transaction.getPosted());
      assertEquals("Entry Not Balanced",
          DateUtils.truncate(transaction.getTransactionDate(), Calendar.DATE),
          getBalancedDate(strRecordID2, strReceivablesAccountId));
      FIN_Reconciliation reconciliation = transaction.getReconciliation();
      assertEquals("Reconciliation Not Posted", "Y", reconciliation.getPosted());
      ResetAccounting.delete(reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId(), TABLE_RECONCILIATION, reconciliation.getId(),
          "", "");
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, reconciliation.getId());
      assertEquals("Reconciliation Could Not Be Unposted", "N", reconciliation.getPosted());
      assertTrue("RecordID2 for in transit not updated after unposting reconciliation",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_RECONCILIATION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      postDocument(reconciliation.getId(), TABLE_RECONCILIATION, reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId());
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, reconciliation.getId());
      assertTrue("RecordID2 for in transit not updated after posting reconciliation",
          new BigDecimal("-40").compareTo(getBalance(strRecordID2InTransit, TABLE_RECONCILIATION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Reconciliation Could Not be Posted", "Y", reconciliation.getPosted());
      assertEquals("Entry Not Balanced",
          DateUtils.truncate(
              reconciliation.getAPRMFinaccTransactionVList().get(0).getTransactionDate(),
              Calendar.DATE),
          getBalancedDate(strRecordID2InTransit, strInTransitAccountId));

    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Verifies documents can be posted and unposted and that affects fact_acct (RecordID2 and
   * DateBalanced) Case 4: Invoice posted , payment posted and reconciliation posted. After both are
   * posted receivables entry should be balanced. Invoice: I/3 Payment: 400002 Reconciliation:
   * 1000034. When Payment and reconciliation are posted in transit account balance should be 0
   */
  @Test
  public void testRecordID2_case5() {
    // Invoice I/3 from QA dataset
    String strCInvoiceId = "FF8080813282F9FE0132830158E20024";
    String strCAcctSchemaId = "9A68A0F8D72D4580B3EC3CAA00A5E1F0";

    OBContext.setAdminMode(true);
    try {
      Invoice invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strRecordID2 = invoice.getFINPaymentScheduleList().get(0).getId();
      assertEquals("Invoice Not Posted", "Y", invoice.getPosted());
      ResetAccounting.delete(invoice.getClient().getId(), invoice.getOrganization().getId(), "318",
          invoice.getId(), "", "");
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      String strReceivablesAccountId = getReceivablesAccount(invoice.getBusinessPartner(),
          strCAcctSchemaId);
      assertEquals("Invoice Could Not Be Unposted", "N", invoice.getPosted());
      assertTrue("RecordID2 not updated after unposting invoice", BigDecimal.ZERO.compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      postDocument(strCInvoiceId, "318", invoice.getClient().getId(),
          invoice.getOrganization().getId());
      invoice = OBDal.getInstance().get(Invoice.class, strCInvoiceId);
      assertTrue("RecordID2 not updated after posting invoice", new BigDecimal("100").compareTo(
          getBalance(strRecordID2, TABLE_INVOICE, strCAcctSchemaId, strReceivablesAccountId)) == 0);
      assertEquals("Invoice Could Not be Posted", "Y", invoice.getPosted());

      // Payment: 400002 - 02-04-2011 - Customer A - 100.00 from QA Dataset. Paying previous invoice
      String strFINPaymentId = "FF8080813282F9FE013283022F3E0030";
      FIN_Payment payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);
      String strInTransitAccountId = getInTransitAccount(payment.getAccount(), strCAcctSchemaId);
      String strRecordID2InTransit = payment.getId();
      assertEquals("Payment Not Posted", "Y", payment.getPosted());
      ResetAccounting.delete(payment.getClient().getId(), payment.getOrganization().getId(),
          TABLE_PAYMENT, payment.getId(), "", "");
      payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);
      assertEquals("Payment Could Not Be Unposted", "N", payment.getPosted());
      assertTrue("RecordID2 for receivables not updated after unposting Payment",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2, TABLE_PAYMENT, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for In Transit not updated after unposting Payment",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_PAYMENT,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      postDocument(strFINPaymentId, TABLE_PAYMENT, payment.getClient().getId(),
          payment.getOrganization().getId());
      payment = OBDal.getInstance().get(FIN_Payment.class, strFINPaymentId);
      assertTrue("RecordID2 for receivables not updated after posting Payment",
          new BigDecimal("-100").compareTo(getBalance(strRecordID2, TABLE_PAYMENT, strCAcctSchemaId,
              strReceivablesAccountId)) == 0);
      assertTrue("RecordID2 for In Transit not updated after posting Payment",
          new BigDecimal("100").compareTo(getBalance(strRecordID2InTransit, TABLE_PAYMENT,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Payment Could Not be Posted", "Y", payment.getPosted());
      assertEquals("Entry Not Balanced for Receivables",
          DateUtils.truncate(payment.getPaymentDate(), Calendar.DATE),
          getBalancedDate(strRecordID2, strReceivablesAccountId));
      FIN_FinaccTransaction transaction = payment.getFINFinaccTransactionList().get(0);
      FIN_Reconciliation reconciliation = transaction.getReconciliation();
      assertEquals("Reconciliation Not Posted", "Y", reconciliation.getPosted());
      ResetAccounting.delete(reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId(), TABLE_RECONCILIATION, reconciliation.getId(),
          "", "");
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, reconciliation.getId());
      assertEquals("Reconciliation Could Not Be Unposted", "N", reconciliation.getPosted());
      assertTrue("RecordID2 for in transit not updated after unposting reconciliation",
          BigDecimal.ZERO.compareTo(getBalance(strRecordID2InTransit, TABLE_RECONCILIATION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      postDocument(reconciliation.getId(), TABLE_RECONCILIATION, reconciliation.getClient().getId(),
          reconciliation.getOrganization().getId());
      reconciliation = OBDal.getInstance().get(FIN_Reconciliation.class, reconciliation.getId());
      assertTrue("RecordID2 for in transit not updated after posting reconciliation",
          new BigDecimal("-100").compareTo(getBalance(strRecordID2InTransit, TABLE_RECONCILIATION,
              strCAcctSchemaId, strInTransitAccountId)) == 0);
      assertEquals("Reconciliation Could Not be Posted", "Y", reconciliation.getPosted());
      assertEquals("Entry Not Balanced",
          DateUtils.truncate(transaction.getTransactionDate(), Calendar.DATE),
          getBalancedDate(strRecordID2InTransit, strInTransitAccountId));

    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private Date getBalancedDate(String strRecordID2, String strAccountId) {
    String sql = "select max(datebalanced) " //
        + "from fact_Acct where record_ID2 = ?" + " and account_id = ?";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.setString(1, strRecordID2);
      sqlQuery.setString(2, strAccountId);
      sqlQuery.execute();
      sqlQuery.setMaxRows(1);
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        return rs.getDate(1);
      }
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
    return null;
  }

  private String getReceivablesAccount(BusinessPartner businessPartner, String strCAcctSchemaId) {
    String sql = "select  account_id " //
        + "from c_bp_customer_acct, c_validcombination where c_receivable_acct = c_validcombination_id and c_bp_customer_acct.c_bpartner_id = ? and c_bp_customer_acct.c_acctschema_id = ?";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.setString(1, businessPartner.getId());
      sqlQuery.setString(2, strCAcctSchemaId);
      sqlQuery.execute();
      sqlQuery.setMaxRows(1);
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        return rs.getString(1);
      }
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
    return null;
  }

  private String getInTransitAccount(FIN_FinancialAccount account, String strCAcctSchemaId) {
    String sql = "select  account_id " //
        + "from fin_financial_account_acct, c_validcombination where fin_in_intransit_acct = c_validcombination_id and fin_financial_account_acct.fin_financial_account_id = ? and fin_financial_account_acct.c_acctschema_id = ?";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.setString(1, account.getId());
      sqlQuery.setString(2, strCAcctSchemaId);
      sqlQuery.execute();
      sqlQuery.setMaxRows(1);
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        return rs.getString(1);
      }
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
    return null;
  }

  private String getDepositAccount(FIN_FinancialAccount account, String strCAcctSchemaId) {
    String sql = "select  account_id " //
        + "from fin_financial_account_acct, c_validcombination where fin_deposit_acct = c_validcombination_id and fin_financial_account_acct.fin_financial_account_id = ? and fin_financial_account_acct.c_acctschema_id = ?";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.setString(1, account.getId());
      sqlQuery.setString(2, strCAcctSchemaId);
      sqlQuery.execute();
      sqlQuery.setMaxRows(1);
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        return rs.getString(1);
      }
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
    return null;
  }

  private BigDecimal getBalance(String strRecordID2, String strTableId, String strCAcctSchemaId,
      String strAccountId) {
    String sql = "select coalesce(sum(amtacctdr-amtacctcr),0) " //
        + "from fact_Acct where record_ID2 = ?" + " and ad_table_id = ?"
        + " and c_acctschema_id = ? and account_id = ?";

    PreparedStatement sqlQuery = null;
    ResultSet rs = null;
    try {
      sqlQuery = new DalConnectionProvider(false).getPreparedStatement(sql);
      sqlQuery.setString(1, strRecordID2);
      sqlQuery.setString(2, strTableId);
      sqlQuery.setString(3, strCAcctSchemaId);
      sqlQuery.setString(4, strAccountId);
      sqlQuery.execute();
      sqlQuery.setMaxRows(1);
      rs = sqlQuery.getResultSet();
      while (rs.next()) {
        return rs.getBigDecimal(1);
      }
    } catch (Exception e) {
      assertFalse(true);
      log.error("Error when executing query", e);
    } finally {
      try {
        if (sqlQuery != null) {
          sqlQuery.close();
        }
        if (rs != null) {
          rs.close();
        }
      } catch (Exception e) {
        log.error("Error when closing statement", e);
      }
    }
    return BigDecimal.ZERO;
  }

  private void postDocument(String strKey, String strTableId, String strClient, String strOrg)
      throws ServletException {
    ConnectionProvider conn = getConnectionProvider();

    Connection con = null;
    try {
      con = conn.getTransactionConnection();
      AcctServer acct = AcctServer.get(strTableId, strClient, strOrg, conn);
      if (acct == null) {
        conn.releaseRollbackConnection(con);
        return;
      } else if (!acct.post(strKey, false, new VariablesSecureApp("100", strClient, strOrg), conn,
          con) || acct.errors != 0) {
        conn.releaseRollbackConnection(con);
        return;
      }
      conn.releaseCommitConnection(con);
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      try {
        conn.releaseRollbackConnection(con);
      } catch (Exception ignored) {
      }
    }
    return;
  }

}
