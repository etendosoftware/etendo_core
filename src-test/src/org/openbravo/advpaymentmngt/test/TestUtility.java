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
 * All portions are Copyright (C) 2010-2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.PersistenceException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.criterion.Restrictions;
import org.openbravo.advpaymentmngt.dao.AdvPaymentMngtDao;
import org.openbravo.advpaymentmngt.process.FIN_AddPayment;
import org.openbravo.advpaymentmngt.process.FIN_PaymentProcess;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBConfigFileProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.database.ConnectionProviderImpl;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.exception.PoolNotFoundException;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.geography.Location;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.MatchingAlgorithm;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.model.financialmgmt.payment.PaymentTerm;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.CallProcess;
import org.openbravo.test.base.OBBaseTest;

public class TestUtility extends OBBaseTest {

  private static final Logger log = LogManager.getLogger();

  public static FIN_FinancialAccount insertFinancialAccount(String name, String description,
      Currency currency, String type, boolean isDefault, Location location,
      BusinessPartner businessPartner, String bankCode, String branchCode, String bankControlDigit,
      String iNeNo, String swiftCode, String accountControlDigit, String partialAccNo,
      String accountNo, String iban, BigDecimal currentBalance, BigDecimal creditLimit,
      MatchingAlgorithm matchingAlgorithm, boolean isValid, boolean checkIfExist) {

    if (checkIfExist) {
      final OBCriteria<FIN_FinancialAccount> obc = OBDal.getInstance()
          .createCriteria(FIN_FinancialAccount.class);
      obc.add(Restrictions.eq(FIN_FinancialAccount.PROPERTY_NAME, name));
      if (obc.list() != null && obc.list().size() > 0) {
        return obc.list().get(0);
      }
    }

    FIN_FinancialAccount finAcc = OBProvider.getInstance().get(FIN_FinancialAccount.class);

    finAcc.setName(name);
    finAcc.setDescription(description);
    finAcc.setCurrency(currency);
    finAcc.setType(type);
    finAcc.setDefault(isDefault);
    finAcc.setLocationAddress(location);
    finAcc.setBusinessPartner(businessPartner);

    // Bank
    finAcc.setBankCode(bankCode);
    finAcc.setBranchCode(branchCode);
    finAcc.setBankDigitcontrol(bankControlDigit);
    finAcc.setINENo(iNeNo);
    finAcc.setSwiftCode(swiftCode);

    // Bank Account
    finAcc.setAccountDigitcontrol(accountControlDigit);
    finAcc.setPartialAccountNo(partialAccNo);
    finAcc.setAccountNo(accountNo);
    finAcc.setIBAN(iban);

    // Amounts
    finAcc.setCurrentBalance(currentBalance);
    finAcc.setCreditLimit(creditLimit);
    finAcc.setMatchingAlgorithm(matchingAlgorithm);

    boolean exception = false;
    try {
      // force dal commit to throw exception
      OBDal.getInstance().save(finAcc);
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log.error(e);
      exception = true;
      OBDal.getInstance().rollbackAndClose();
    }

    if (isValid) {
      assertFalse("Not inserted a valid financial account:" + name, exception);
    } else {
      assertTrue("Inserted a non-valid financial account:" + name, exception);
    }

    if (exception) {
      return null;
    } else {
      return finAcc;
    }
  }

  public static FIN_PaymentMethod insertPaymentMethod(String name, String description,
      boolean isPayInAllowed, boolean isAutomaticReceipt, boolean isAutomaticDeposit,
      String payInExecutionType, PaymentExecutionProcess payInExecutionProcess,
      boolean isPayinDeferred, String uponReceiptUse, String uponDepositUse,
      String inUponClearingUse, boolean isPayOutAllowed, boolean isAutomaticPayment,
      boolean isAutomaticWithdrawn, String payOutExecutionType,
      PaymentExecutionProcess payOutExecutionProcess, boolean isPayoutDeferred,
      String uponPaymentUse, String uponWithdrawalUse, String outUponClearingUse, boolean isValid,
      boolean checkIfExist) {

    if (checkIfExist) {
      final OBCriteria<FIN_PaymentMethod> obc = OBDal.getInstance()
          .createCriteria(FIN_PaymentMethod.class);
      obc.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_NAME, name));
      if (obc.list() != null && obc.list().size() > 0) {
        return obc.list().get(0);
      }
    }

    FIN_PaymentMethod paymentMethod = OBProvider.getInstance().get(FIN_PaymentMethod.class);

    paymentMethod.setName(name);
    paymentMethod.setDescription(description);

    // Payment IN
    paymentMethod.setPayinAllow(isPayInAllowed);
    paymentMethod.setAutomaticReceipt(isAutomaticReceipt);
    paymentMethod.setAutomaticDeposit(isAutomaticDeposit);

    paymentMethod.setPayinExecutionType(payInExecutionType);
    paymentMethod.setPayinExecutionProcess(payInExecutionProcess);
    paymentMethod.setPayinDeferred(isPayinDeferred);
    paymentMethod.setUponReceiptUse(uponReceiptUse);
    paymentMethod.setUponDepositUse(uponDepositUse);
    paymentMethod.setINUponClearingUse(inUponClearingUse);

    // Payment OUT
    paymentMethod.setPayoutAllow(isPayOutAllowed);
    paymentMethod.setAutomaticPayment(isAutomaticPayment);
    paymentMethod.setAutomaticWithdrawn(isAutomaticWithdrawn);

    paymentMethod.setPayoutExecutionType(payOutExecutionType);
    paymentMethod.setPayoutExecutionProcess(payOutExecutionProcess);
    paymentMethod.setPayoutDeferred(isPayoutDeferred);

    paymentMethod.setUponPaymentUse(uponPaymentUse);
    paymentMethod.setUponWithdrawalUse(uponWithdrawalUse);
    paymentMethod.setOUTUponClearingUse(outUponClearingUse);

    OBDal.getInstance().save(paymentMethod);

    boolean exception = false;
    try {
      // force dal commit to throw exception
      OBDal.getInstance().flush();
      OBDal.getInstance().commitAndClose();
    } catch (Exception e) {
      log.error(e);
      exception = true;
      OBDal.getInstance().rollbackAndClose();
    }

    if (isValid) {
      assertFalse("Not inserted a valid payment method:" + name, exception);
    } else {
      assertTrue("Inserted a non-valid payment method:" + name, exception);
    }

    if (exception) {
      return null;
    } else {
      return paymentMethod;
    }
  }

  public static FinAccPaymentMethod associatePaymentMethod(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod) {
    FinAccPaymentMethod accountPay = OBProvider.getInstance().get(FinAccPaymentMethod.class);
    accountPay.setAccount(account);
    accountPay.setPaymentMethod(paymentMethod);

    accountPay.setPayoutAllow(paymentMethod.isPayoutAllow());
    accountPay.setPayoutDeferred(paymentMethod.isPayoutDeferred());
    accountPay.setPayoutExecutionProcess(paymentMethod.getPayoutExecutionProcess());
    accountPay.setPayoutExecutionType(paymentMethod.getPayoutExecutionType());
    accountPay.setAutomaticReceipt(paymentMethod.isAutomaticReceipt());
    accountPay.setAutomaticDeposit(paymentMethod.isAutomaticDeposit());
    accountPay.setUponReceiptUse(paymentMethod.getUponReceiptUse());
    accountPay.setUponDepositUse(paymentMethod.getUponDepositUse());
    accountPay.setOUTUponClearingUse(paymentMethod.getOUTUponClearingUse());

    accountPay.setPayinAllow(paymentMethod.isPayinAllow());
    accountPay.setPayinDeferred(paymentMethod.isPayinDeferred());
    accountPay.setPayinExecutionProcess(paymentMethod.getPayinExecutionProcess());
    accountPay.setPayinExecutionType(paymentMethod.getPayinExecutionType());
    accountPay.setAutomaticPayment(paymentMethod.isAutomaticPayment());
    accountPay.setAutomaticWithdrawn(paymentMethod.isAutomaticWithdrawn());
    accountPay.setUponPaymentUse(paymentMethod.getUponPaymentUse());
    accountPay.setUponWithdrawalUse(paymentMethod.getUponWithdrawalUse());
    accountPay.setINUponClearingUse(paymentMethod.getINUponClearingUse());

    boolean exception = false;
    try {
      // force dal commit to throw exception
      OBDal.getInstance().save(accountPay);
      OBDal.getInstance().flush();
    } catch (PersistenceException e) {
      log.error(e);
      exception = true;
      OBDal.getInstance().rollbackAndClose();
    }

    assertFalse("Not associated a valid payment method to the financial account", exception);

    if (exception) {
      return null;
    } else {
      return accountPay;
    }

  }

  public static Order createNewOrder(Client client, Organization org, Date orderDate,
      Date accountingDate, Date taxDate, DocumentType documentType, BusinessPartner bp,
      org.openbravo.model.common.businesspartner.Location loc, Warehouse warehouse,
      String invoiceTerm, PriceList priceList, Currency currency, FIN_PaymentMethod paymentMethod,
      PaymentTerm paymentTerm, Product product, UOM uom, BigDecimal orderedQuantity,
      BigDecimal netUnitPrice, BigDecimal netListPrice, BigDecimal priceLimit, TaxRate taxRate,
      BigDecimal lineNetAmount, boolean isReceipt) throws Exception {

    // Create header
    Order order = OBProvider.getInstance().get(Order.class);
    order.setOrganization(org);
    order.setClient(client);
    order.setDocumentType(documentType);
    order.setTransactionDocument(documentType);
    order.setDocumentNo(FIN_Utility.getDocumentNo(org, "SOO", "C_Order"));
    order.setAccountingDate(accountingDate);
    order.setOrderDate(orderDate);
    order.setWarehouse(warehouse);
    if (!"".equals(invoiceTerm)) {
      order.setInvoiceTerms(invoiceTerm);
    }
    // order.setTaxDate(taxDate);
    order.setBusinessPartner(bp);
    order.setPartnerAddress(loc);
    order.setPriceList(priceList);
    order.setCurrency(currency);
    order.setSummedLineAmount(BigDecimal.ZERO);
    order.setGrandTotalAmount(BigDecimal.ZERO);
    // order.setWithholdingamount(BigDecimal.ZERO);
    order.setSalesTransaction(isReceipt);
    order.setPaymentMethod(paymentMethod);
    order.setPaymentTerms(paymentTerm);

    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();

    // Create one line
    OrderLine orderLine = OBProvider.getInstance().get(OrderLine.class);
    orderLine.setOrganization(org);
    orderLine.setClient(client);
    orderLine.setSalesOrder(order);
    orderLine.setOrderDate(orderDate);
    orderLine.setWarehouse(warehouse);
    orderLine.setCurrency(currency);
    orderLine.setLineNo(10L);
    orderLine.setProduct(product);
    orderLine.setUOM(uom);
    orderLine.setInvoicedQuantity(BigDecimal.ZERO);
    orderLine.setOrderedQuantity(orderedQuantity);
    orderLine.setUnitPrice(netUnitPrice);
    orderLine.setListPrice(netListPrice);
    orderLine.setPriceLimit(priceLimit);
    orderLine.setTax(taxRate);
    orderLine.setLineNetAmount(lineNetAmount);

    OBDal.getInstance().save(orderLine);
    OBDal.getInstance().flush();

    return order;
  }

  public static boolean processOrder(Order order) throws Exception {
    OBContext.setAdminMode();
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "104");
    } finally {
      OBContext.restorePreviousMode();
    }
    final ProcessInstance pinstance = CallProcess.getInstance().call(process, order.getId(), null);
    OBDal.getInstance().save(order);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    return (pinstance.getResult() == 0L);
  }

  public static Invoice createNewInvoice(Client client, Organization org, Date invoiceDate,
      Date accountingDate, Date taxDate, DocumentType documentType, BusinessPartner bp,
      org.openbravo.model.common.businesspartner.Location loc, PriceList priceList,
      Currency currency, FIN_PaymentMethod paymentMethod, PaymentTerm paymentTerm, Product product,
      UOM uom, BigDecimal invoicedQuantity, BigDecimal netUnitPrice, BigDecimal netListPrice,
      BigDecimal priceLimit, TaxRate taxRate, BigDecimal lineNetAmount, boolean isReceipt)
      throws Exception {

    // Create header
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setOrganization(org);
    invoice.setClient(client);
    invoice.setDocumentType(documentType);
    invoice.setTransactionDocument(documentType);
    invoice.setDocumentNo(FIN_Utility.getDocumentNo(org, "ARI", "C_Invoice"));
    invoice.setAccountingDate(accountingDate);
    invoice.setInvoiceDate(invoiceDate);
    invoice.setTaxDate(taxDate);
    invoice.setBusinessPartner(bp);
    invoice.setPartnerAddress(loc);
    invoice.setPriceList(priceList);
    invoice.setCurrency(currency);
    invoice.setSummedLineAmount(BigDecimal.ZERO);
    invoice.setGrandTotalAmount(BigDecimal.ZERO);
    invoice.setWithholdingamount(BigDecimal.ZERO);
    invoice.setSalesTransaction(isReceipt);
    invoice.setPaymentMethod(paymentMethod);
    invoice.setPaymentTerms(paymentTerm);

    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();

    // Create one line
    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);
    invoiceLine.setOrganization(org);
    invoiceLine.setClient(client);
    invoiceLine.setInvoice(invoice);
    invoiceLine.setLineNo(10L);
    invoiceLine.setProduct(product);
    invoiceLine.setUOM(uom);
    invoiceLine.setInvoicedQuantity(invoicedQuantity);
    invoiceLine.setUnitPrice(netUnitPrice);
    invoiceLine.setListPrice(netListPrice);
    invoiceLine.setPriceLimit(priceLimit);
    invoiceLine.setTax(taxRate);
    invoiceLine.setLineNetAmount(lineNetAmount);

    OBDal.getInstance().save(invoiceLine);
    OBDal.getInstance().flush();

    return invoice;
  }

  /**
   * This method is used to create a new Invoice in the system.
   *
   * @param client The Client object representing the client for which the invoice is being created.
   * @param organization The Organization object representing the organization for which the invoice is being created.
   * @param invoiceDate The Date object representing the date of the invoice.
   * @param accountingDate The Date object representing the accounting date of the invoice.
   * @param taxDate The Date object representing the tax date of the invoice.
   * @param documentType The DocumentType object representing the type of the document for the invoice.
   * @param businessPartner The BusinessPartner object representing the business partner for the invoice.
   * @param location The Location object representing the location for the invoice.
   * @param priceList The PriceList object representing the price list for the invoice.
   * @param currency The Currency object representing the currency for the invoice.
   * @param paymentMethod The FIN_PaymentMethod object representing the payment method for the invoice.
   * @param paymentTerm The PaymentTerm object representing the payment term for the invoice.
   * @param product The Product object representing the product for the invoice.
   * @param uom The UOM object representing the unit of measure for the invoice.
   * @param invoicedQuantity The BigDecimal object representing the invoiced quantity for the invoice.
   * @param netUnitPrice The BigDecimal object representing the net unit price for the invoice.
   * @param netListPrice The BigDecimal object representing the net list price for the invoice.
   * @param priceLimit The BigDecimal object representing the price limit for the invoice.
   * @param taxRate The TaxRate object representing the tax rate for the invoice.
   * @param lineNetAmount The BigDecimal object representing the line net amount for the invoice.
   * @param isSalesInvoice The boolean value representing whether the invoice is a sales invoice or not.
   * @return Invoice The created Invoice object.
   */
  public static Invoice createInvoice(Client client, Organization organization, Date invoiceDate,
      Date accountingDate, Date taxDate, DocumentType documentType, BusinessPartner businessPartner,
      org.openbravo.model.common.businesspartner.Location location, PriceList priceList,
      Currency currency, FIN_PaymentMethod paymentMethod, PaymentTerm paymentTerm, Product product,
      UOM uom, BigDecimal invoicedQuantity, BigDecimal netUnitPrice, BigDecimal netListPrice,
      BigDecimal priceLimit, TaxRate taxRate, BigDecimal lineNetAmount, boolean isSalesInvoice) {
    // Create header
    Invoice invoice = OBProvider.getInstance().get(Invoice.class);
    invoice.setOrganization(organization);
    invoice.setTransactionDocument(documentType);
    invoice.setBusinessPartner(businessPartner);
    invoice.setPaymentTerms(paymentTerm);
    invoice.setSalesTransaction(isSalesInvoice);
    invoice.setPartnerAddress(location);
    invoice.setClient(client);
    invoice.setDocumentType(documentType);
    invoice.setInvoiceDate(invoiceDate);
    invoice.setAccountingDate(accountingDate);
    invoice.setTaxDate(taxDate);
    invoice.setCurrency(currency);
    invoice.setPriceList(priceList);
    invoice.setPaymentMethod(paymentMethod);

    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoice);

    // Create one line
    InvoiceLine invoiceLine = OBProvider.getInstance().get(InvoiceLine.class);

    invoiceLine.setOrganization(organization);
    invoiceLine.setProduct(product);
    invoiceLine.setLineNo(10L);
    invoiceLine.setUOM(uom);
    invoiceLine.setTax(taxRate);
    invoiceLine.setGrossUnitPrice(netListPrice);
    invoiceLine.setGrossListPrice(netListPrice);
    invoiceLine.setStandardPrice(netListPrice);
    invoiceLine.setBaseGrossUnitPrice(netListPrice);
    invoiceLine.setGrossAmount(netListPrice);

    invoiceLine.setInvoicedQuantity(invoicedQuantity);
    invoiceLine.setUnitPrice(netUnitPrice);
    invoiceLine.setListPrice(netListPrice);
    invoiceLine.setPriceLimit(priceLimit);
    invoiceLine.setLineNetAmount(lineNetAmount);

    invoiceLine.setInvoice(invoice);

    OBDal.getInstance().save(invoiceLine);
    OBDal.getInstance().flush();
    OBDal.getInstance().refresh(invoiceLine);
    return invoice;
  }

  public static boolean processInvoice(Invoice invoice) throws Exception {
    OBContext.setAdminMode();
    Process process = null;
    try {
      process = OBDal.getInstance().get(Process.class, "111");
    } finally {
      OBContext.restorePreviousMode();
    }
    final ProcessInstance pinstance = CallProcess.getInstance()
        .call(process, invoice.getId(), null);
    OBDal.getInstance().save(invoice);
    OBDal.getInstance().flush();
    OBDal.getInstance().commitAndClose();
    return (pinstance.getResult() == 0L);
  }

  public static FIN_Payment addPaymentFromInvoice(Invoice invoice, FIN_FinancialAccount account,
      BigDecimal amount, boolean isWriteOff) throws Exception {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();

    OBDal.getInstance().getSession().refresh(invoice);
    List<FIN_PaymentScheduleDetail> scheduleDetails = dao
        .getInvoicePendingScheduledPaymentDetails(invoice);

    HashMap<String, BigDecimal> paidAmount = new HashMap<String, BigDecimal>();
    paidAmount.put(scheduleDetails.get(0).getId(), amount);

    FIN_Payment payment = FIN_AddPayment.savePayment(null, invoice.isSalesTransaction(),
        FIN_Utility.getDocumentType(invoice.getOrganization(), "ARR"),
        FIN_Utility.getDocumentNo(invoice.getOrganization(), "APP", "FIN_Payment"),
        invoice.getBusinessPartner(), invoice.getPaymentMethod(), account, amount.toString(),
        new Date(), invoice.getOrganization(), null, scheduleDetails, paidAmount, isWriteOff,
        false);

    return payment;
  }

  public static FIN_Payment addPaymentFromOrder(Order order, FIN_FinancialAccount account,
      BigDecimal amount, boolean isWriteOff) throws Exception {
    AdvPaymentMngtDao dao = new AdvPaymentMngtDao();

    OBDal.getInstance().getSession().refresh(order);
    List<FIN_PaymentScheduleDetail> scheduleDetails = dao
        .getOrderPendingScheduledPaymentDetails(order);

    HashMap<String, BigDecimal> paidAmount = new HashMap<String, BigDecimal>();
    paidAmount.put(scheduleDetails.get(0).getId(), amount);

    FIN_Payment payment = FIN_AddPayment.savePayment(null, order.isSalesTransaction(),
        FIN_Utility.getDocumentType(order.getOrganization(), "ARR"),
        FIN_Utility.getDocumentNo(order.getOrganization(), "APP", "FIN_Payment"),
        order.getBusinessPartner(), order.getPaymentMethod(), account, amount.toString(),
        new Date(), order.getOrganization(), null, scheduleDetails, paidAmount, isWriteOff, false);

    return payment;
  }

  public static void processPayment(FIN_Payment payment, String strDocAction) throws Exception {
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId());

    FIN_AddPayment.processPayment(vars, getConnectionProviderMy(), strDocAction, payment);
  }

  /**
   * Method used to generating payment document with refund amount.
   * 
   * @param payment
   *          Created Payment.
   * @param refundAmount
   *          Refund Amount need to transfer to the customer.
   * @return Created FIN_Payment object.
   * @throws Exception
   *           In case of executing invalid data.
   */
  public static FIN_Payment createRefundPayment(FIN_Payment payment, BigDecimal refundAmount)
      throws Exception {
    OBContext.setAdminMode();
    FIN_Payment refundPayment = null;
    try {
      VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId());
      refundPayment = FIN_AddPayment.createRefundPayment(getConnectionProviderMy(), vars, payment,
          refundAmount.negate());
    } finally {
      OBContext.restorePreviousMode();
    }
    return refundPayment;
  }

  /**
   * This method is used to create a reverse payment in the system.
   *
   * @param payment The FIN_Payment object representing the payment to be reversed.
   * @return FIN_Payment The created reverse payment object.
   * @throws Exception If there is an error during the creation of the reverse payment.
   */
  public static FIN_Payment createReversePayment(FIN_Payment payment)
      throws Exception {
    FIN_Payment reversePayment = null;
    try {
      OBContext.setAdminMode();
      VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
          OBContext.getOBContext().getCurrentClient().getId(),
          OBContext.getOBContext().getCurrentOrganization().getId(),
          OBContext.getOBContext().getRole().getId());

      ProcessBundle pb = new ProcessBundle("29D17F515727436DBCE32BC6CA28382B", vars).init(getConnectionProviderMy());
      HashMap<String, Object> parameters = new HashMap<>();
      parameters.put("action", "RV");
      parameters.put("paymentdate", OBDateUtils.formatDate(payment.getPaymentDate()));
      parameters.put("Fin_Payment_ID", payment.getId());
      parameters.put("comingFrom", null);
      parameters.put("selectedCreditLineIds", null);
      pb.setParams(parameters);
      new FIN_PaymentProcess().execute(pb);
      OBError myMessage = (OBError) pb.getResult();
      if (myMessage != null) {
        OBDal.getInstance().refresh(payment);
        reversePayment = payment.getReversedPayment();
        // Log success message
        log.info("Reverse payment created successfully for payment ID: " + payment.getId());
      } else {
        // Log error message
        log.error("Failed to create reverse payment for payment ID: " + payment.getId());
      }
    } catch (Exception e) {
      // Log exception
      log.error("Exception occurred while creating reverse payment for payment ID: " + payment.getId(), e);
      throw e;
    } finally {
      OBContext.restorePreviousMode();
    }
    return reversePayment;
  }

  public static void processPaymentProposal(FIN_PaymentProposal paymentProposal,
      String strDocAction) throws Exception {
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId());

    FIN_AddPayment.processPaymentProposal(vars, getConnectionProviderMy(), strDocAction,
        paymentProposal.getId());
  }

  public static FIN_PaymentProposal createNewPaymentProposal(Client client, Organization org,
      FIN_FinancialAccount account, Currency currency, FIN_PaymentMethod paymentMethod) {
    FIN_PaymentProposal pp = OBProvider.getInstance().get(FIN_PaymentProposal.class);
    pp.setOrganization(org);
    pp.setClient(client);
    pp.setAccount(account);
    pp.setAmount(BigDecimal.ZERO);
    pp.setBusinessPartner(null);
    pp.setCurrency(currency);
    pp.setDocumentNo(FIN_Utility.getDocumentNo(org, "APPP", "FIN_PaymentProposal"));
    pp.setDocumentType(FIN_Utility.getDocumentType(org, "APPP"));
    pp.setPaymentMethod(paymentMethod);
    pp.setDuedate(null);
    pp.setPaymentDate(new Date());
    pp.setReceipt(false);
    pp.setWriteoffAmount(BigDecimal.ZERO);
    OBDal.getInstance().save(pp);
    OBDal.getInstance().flush();

    return pp;
  }

  public static <T extends BaseOBObject> T getOneInstance(Class<T> clazz, Value... values) {

    // create an OBCriteria object and add a filter
    final OBCriteria<T> obCriteria = OBDal.getInstance().createCriteria(clazz);

    for (Value value : values) {
      if (value.getValue() == null) {
        obCriteria.add(Restrictions.isNull(value.getField()));
      } else {
        obCriteria.add(Restrictions.eq(value.getField(), value.getValue()));
      }
    }

    final List<T> listt = obCriteria.list();
    if (listt != null && listt.size() > 0) {
      return listt.get(0);
    } else {
      return null;
    }
  }

  public static void setTestContext() {
    // User = Openbravo
    // Role = F&B International Group Admin
    // Client = F&B International Group
    // Org = F&B US, Inc.
    OBContext.setOBContext("100", "42D0EEB1C66F497A90DD526DC597E6F0",
        "23C59575B9CF467C9620760EB255B389", "2E60544D37534C0B89E765FE29BC0B43");
  }

  public static void setTestContextSpain() {
    // User = Openbravo
    // Role = F&B International Group Admin
    // Client = F&B International Group
    // Org = F&B España, S.A
    OBContext.setOBContext("100", "42D0EEB1C66F497A90DD526DC597E6F0",
        "23C59575B9CF467C9620760EB255B389", "B843C30461EA4501935CB1D125C9C25A");
  }

  private static ConnectionProvider getConnectionProviderMy() {
    try {
      final String propFile = OBConfigFileProvider.getInstance().getFileLocation();
      final ConnectionProvider conn = new ConnectionProviderImpl(
          propFile + "/Openbravo.properties");
      return conn;
    } catch (PoolNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

}
