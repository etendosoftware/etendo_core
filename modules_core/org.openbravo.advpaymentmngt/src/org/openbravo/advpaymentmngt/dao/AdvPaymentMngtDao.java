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
 * All portions are Copyright (C) 2010-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  Enterprise Intelligence Systems (http://www.eintel.com.au).
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.dao;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.APRMPendingPaymentFromInvoice;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.advpaymentmngt.utility.Value;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.invoice.Invoice;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.accounting.Costcenter;
import org.openbravo.model.financialmgmt.accounting.UserDimension1;
import org.openbravo.model.financialmgmt.accounting.UserDimension2;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentMethod;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentPropDetail;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentProposal;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentSchedule;
import org.openbravo.model.financialmgmt.payment.FIN_PaymentScheduleDetail;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.financialmgmt.payment.FinAccPaymentMethod;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcess;
import org.openbravo.model.financialmgmt.payment.PaymentExecutionProcessParameter;
import org.openbravo.model.financialmgmt.payment.PaymentRun;
import org.openbravo.model.financialmgmt.payment.PaymentRunParameter;
import org.openbravo.model.financialmgmt.payment.PaymentRunPayment;
import org.openbravo.model.marketing.Campaign;
import org.openbravo.model.materialmgmt.cost.ABCActivity;
import org.openbravo.model.project.Project;
import org.openbravo.model.sales.SalesRegion;

public class AdvPaymentMngtDao {

  public enum PaymentDirection {
    IN, OUT, EITHER
  }

  public final String PAYMENT_STATUS_AWAITING_EXECUTION = "RPAE";
  public final String PAYMENT_STATUS_CANCELED = "RPVOID";
  public final String PAYMENT_STATUS_PAYMENT_CLEARED = "RPPC";
  public final String PAYMENT_STATUS_DEPOSIT_NOT_CLEARED = "RDNC";
  public final String PAYMENT_STATUS_PAYMENT_MADE = "PPM";
  public final String PAYMENT_STATUS_AWAITING_PAYMENT = "RPAP";
  public final String PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED = "PWNC";
  public final String PAYMENT_STATUS_PAYMENT_RECEIVED = "RPR";

  public AdvPaymentMngtDao() {
  }

  public <T extends BaseOBObject> T getObject(Class<T> t, String strId) {
    return OBDal.getInstance().get(t, strId);
  }

  public List<FIN_PaymentScheduleDetail> getInvoicePendingScheduledPaymentDetails(Invoice invoice) {

    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {

      //@formatter:off
      final String whereClause = " as psd "
           + " where psd.paymentDetails is null"
           + "   and psd.invoicePaymentSchedule.invoice.id = :invoiceId"
           + " order by psd.invoicePaymentSchedule.expectedDate, psd.amount";
      
      //@formatter:on
      final OBQuery<FIN_PaymentScheduleDetail> obqPSD = OBDal.getInstance()
          .createQuery(FIN_PaymentScheduleDetail.class, whereClause);
      obqPSD.setNamedParameter("invoiceId", invoice.getId());
      return obqPSD.list();

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public List<FIN_PaymentScheduleDetail> getOrderPendingScheduledPaymentDetails(Order order) {

    // FIXME: added to access the FIN_PaymentSchedule and FIN_PaymentScheduleDetail tables to be
    // removed when new security implementation is done
    OBContext.setAdminMode();
    try {
      //@formatter:off
      final String whereClause = " as psd "
           + " where psd.paymentDetails is null"
           + "   and psd.orderPaymentSchedule.order.id = :orderId"
           + " order by psd.orderPaymentSchedule.expectedDate, psd.amount";

      //@formatter:on
      final OBQuery<FIN_PaymentScheduleDetail> obqPSD = OBDal.getInstance()
          .createQuery(FIN_PaymentScheduleDetail.class, whereClause);
      obqPSD.setNamedParameter("orderId", order.getId());
      return obqPSD.list();

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public FIN_Payment getNewPayment(boolean isReceipt, Organization organization,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, String referenceNo) {
    return getNewPayment(isReceipt, organization, docType, strPaymentDocumentNo, businessPartner,
        paymentMethod, finAccount, strPaymentAmount, paymentDate, referenceNo, null, null, null);
  }

  public FIN_Payment getNewPayment(boolean isReceipt, Organization organization,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, String referenceNo, Currency paymentCurrency, BigDecimal finTxnConvertRate,
      BigDecimal finTxnAmount) {
    return getNewPayment(isReceipt, organization, docType, strPaymentDocumentNo, businessPartner,
        paymentMethod, finAccount, strPaymentAmount, paymentDate, referenceNo, paymentCurrency,
        finTxnConvertRate, finTxnAmount, null);
  }

  public FIN_Payment getNewPayment(boolean isReceipt, Organization organization,
      DocumentType docType, String strPaymentDocumentNo, BusinessPartner businessPartner,
      FIN_PaymentMethod paymentMethod, FIN_FinancialAccount finAccount, String strPaymentAmount,
      Date paymentDate, String referenceNo, Currency paymentCurrency, BigDecimal finTxnConvertRate,
      BigDecimal finTxnAmount, String paymentId) {
    BigDecimal localFinTxnConvertRate = finTxnConvertRate;
    BigDecimal localFinTxnAmount = finTxnAmount;
    final FIN_Payment newPayment = OBProvider.getInstance().get(FIN_Payment.class);
    if (paymentId != null) {
      newPayment.setId(paymentId);
      newPayment.setNewOBObject(true);
    }
    newPayment.setReceipt(isReceipt);
    newPayment.setDocumentType(docType);
    newPayment.setDocumentNo(strPaymentDocumentNo);
    newPayment.setOrganization(organization);
    newPayment.setClient(organization.getClient());
    newPayment.setStatus("RPAP");
    newPayment.setBusinessPartner(businessPartner);
    newPayment.setPaymentMethod(paymentMethod);
    newPayment.setAccount(finAccount);
    final BigDecimal paymentAmount = new BigDecimal(strPaymentAmount);
    newPayment.setAmount(paymentAmount);
    newPayment.setPaymentDate(paymentDate);
    if (paymentCurrency != null) {
      newPayment.setCurrency(paymentCurrency);
    } else {
      newPayment.setCurrency(finAccount.getCurrency());
    }
    newPayment.setReferenceNo(referenceNo);
    if (localFinTxnConvertRate == null || localFinTxnConvertRate.compareTo(BigDecimal.ZERO) <= 0) {
      localFinTxnConvertRate = BigDecimal.ONE;
    }
    if (localFinTxnAmount == null || localFinTxnAmount.compareTo(BigDecimal.ZERO) == 0) {
      localFinTxnAmount = paymentAmount.multiply(localFinTxnConvertRate);
    }
    // This code commented due to fix in bug 17829
    // else if (paymentAmount != null && paymentAmount.compareTo(BigDecimal.ZERO) != 0) {
    // // Correct exchange rate for rounding that occurs in UI
    // finTxnConvertRate = finTxnAmount.divide(paymentAmount, MathContext.DECIMAL64);
    // }

    newPayment.setFinancialTransactionConvertRate(localFinTxnConvertRate);
    newPayment.setFinancialTransactionAmount(localFinTxnAmount);

    OBDal.getInstance().save(newPayment);

    return newPayment;
  }

  public FIN_PaymentDetail getNewPaymentDetail(FIN_Payment payment,
      FIN_PaymentScheduleDetail paymentScheduleDetail, BigDecimal paymentDetailAmount,
      BigDecimal writeoffAmount, boolean isRefund, GLItem glitem) {
    return getNewPaymentDetail(payment, paymentScheduleDetail, paymentDetailAmount, writeoffAmount,
        isRefund, glitem, true, null);
  }

  public FIN_PaymentDetail getNewPaymentDetail(FIN_Payment payment,
      FIN_PaymentScheduleDetail paymentScheduleDetail, BigDecimal paymentDetailAmount,
      BigDecimal writeoffAmount, boolean isRefund, GLItem glitem, boolean doFlush,
      String paymentId) {
    try {
      // Don't check organization access to allow Cross Store payments
      OBContext.setAdminMode(false);
      final FIN_PaymentDetail newPaymentDetail = OBProvider.getInstance()
          .get(FIN_PaymentDetail.class);
      if (paymentId != null) {
        newPaymentDetail.setId(paymentId);
        newPaymentDetail.setNewOBObject(true);
      }
      List<FIN_PaymentDetail> paymentDetails = payment.getFINPaymentDetailList();
      newPaymentDetail.setFinPayment(payment);
      newPaymentDetail.setOrganization(payment.getOrganization());
      newPaymentDetail.setClient(payment.getClient());
      newPaymentDetail.setAmount(paymentDetailAmount);
      newPaymentDetail.setWriteoffAmount(writeoffAmount);
      newPaymentDetail.setRefund(isRefund);
      newPaymentDetail.setGLItem(glitem);
      newPaymentDetail.setPrepayment(
          glitem == null && paymentScheduleDetail.getInvoicePaymentSchedule() == null);

      paymentDetails.add(newPaymentDetail);
      payment.setFINPaymentDetailList(paymentDetails);
      payment.setWriteoffAmount(payment.getWriteoffAmount().add(writeoffAmount));

      List<FIN_PaymentScheduleDetail> paymentScheduleDetails = newPaymentDetail
          .getFINPaymentScheduleDetailList();
      paymentScheduleDetail.setPaymentDetails(newPaymentDetail);
      paymentScheduleDetails.add(paymentScheduleDetail);
      newPaymentDetail.setFINPaymentScheduleDetailList(paymentScheduleDetails);

      OBDal.getInstance().save(payment);
      OBDal.getInstance().save(newPaymentDetail);
      OBDal.getInstance().save(paymentScheduleDetail);
      if (doFlush) {
        OBDal.getInstance().flush();
      }

      return newPaymentDetail;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public FIN_PaymentScheduleDetail getNewPaymentScheduleDetail(Organization organization,
      BigDecimal amount, String paymentId) {
    final FIN_PaymentScheduleDetail newPaymentScheduleDetail = OBProvider.getInstance()
        .get(FIN_PaymentScheduleDetail.class);
    if (paymentId != null) {
      newPaymentScheduleDetail.setId(paymentId);
      newPaymentScheduleDetail.setNewOBObject(true);
    }
    newPaymentScheduleDetail.setOrganization(organization);
    // As '0' is not a valid organization for transactions we can assume that organization client is
    // transaction client
    newPaymentScheduleDetail.setClient(organization.getClient());
    newPaymentScheduleDetail.setAmount(amount);

    OBDal.getInstance().save(newPaymentScheduleDetail);
    // OBDal.getInstance().flush();

    return newPaymentScheduleDetail;
  }

  public FIN_PaymentScheduleDetail getNewPaymentScheduleDetail(Organization organization,
      BigDecimal amount) {

    return getNewPaymentScheduleDetail(organization, amount, null);
  }

  /**
   * Returns a new FIN_PaymentScheduleDetail for the given accounting dimensions
   * 
   * @param organization
   * @param amount
   * @param businessPartner
   *          accounting dimension
   * @param product
   *          accounting dimension
   * @param project
   *          accounting dimension
   * @param campaign
   *          accounting dimension
   * @param activity
   *          accounting dimension
   * @param salesRegion
   *          accounting dimension
   * @param costCenter
   *          accounting dimension
   * @param user1
   *          accounting dimension
   * @param user2
   *          accounting dimension
   */
  public FIN_PaymentScheduleDetail getNewPaymentScheduleDetail(Organization organization,
      BigDecimal amount, BusinessPartner businessPartner, Product product, Project project,
      Campaign campaign, ABCActivity activity, SalesRegion salesRegion, Costcenter costCenter,
      UserDimension1 user1, UserDimension2 user2) {
    final FIN_PaymentScheduleDetail psd = getNewPaymentScheduleDetail(organization, amount);
    psd.setBusinessPartner(businessPartner);
    psd.setProduct(product);
    psd.setProject(project);
    psd.setSalesCampaign(campaign);
    psd.setActivity(activity);
    psd.setSalesRegion(salesRegion);
    psd.setCostCenter(costCenter);
    psd.setStDimension(user1);
    psd.setNdDimension(user2);
    return psd;
  }

  /**
   * Returns a new FIN_PaymentScheduleDetail for the given accounting dimensions
   * 
   * @param organization
   * @param amount
   * @param businessPartner
   *          accounting dimension
   * @param product
   *          accounting dimension
   * @param project
   *          accounting dimension
   * @param campaign
   *          accounting dimension
   * @param activity
   *          accounting dimension
   * @param salesRegion
   *          accounting dimension
   */
  public FIN_PaymentScheduleDetail getNewPaymentScheduleDetail(Organization organization,
      BigDecimal amount, BusinessPartner businessPartner, Product product, Project project,
      Campaign campaign, ABCActivity activity, SalesRegion salesRegion) {
    return getNewPaymentScheduleDetail(organization, amount, businessPartner, product, project,
        campaign, activity, salesRegion, null, null, null);
  }

  /**
   * Creates a new payment schedule
   * 
   */
  public FIN_PaymentSchedule getNewPaymentSchedule(Client client, Organization organization,
      Invoice invoice, Order order, Currency currency, Date dueDate,
      FIN_PaymentMethod paymentMethod, BigDecimal amount) {
    FIN_PaymentSchedule ps = OBProvider.getInstance().get(FIN_PaymentSchedule.class);
    ps.setClient(invoice.getClient());
    ps.setOrganization(invoice.getOrganization());
    ps.setInvoice(invoice);
    ps.setOrder(order);
    ps.setCurrency(invoice.getCurrency());
    ps.setDueDate(dueDate);
    ps.setExpectedDate(dueDate);
    ps.setFinPaymentmethod(paymentMethod);
    ps.setOutstandingAmount(amount);
    ps.setPaidAmount(BigDecimal.ZERO);
    ps.setAmount(amount);
    OBDal.getInstance().save(ps);
    return ps;
  }

  /**
   * Creates a new payment schedule detail taking info from provided payment schedule
   * 
   */
  public FIN_PaymentScheduleDetail getNewPaymentScheduleDetail(FIN_PaymentSchedule invoicePS,
      FIN_PaymentSchedule orderPS, BigDecimal amount, BigDecimal writeOff,
      FIN_PaymentDetail paymentDetail) {

    Client client = null;
    Organization org = null;
    BusinessPartner bp = null;
    Project proj = null;
    Campaign sc = null;
    ABCActivity act = null;

    if (orderPS == null && invoicePS == null) {
      return null;
    }

    if (orderPS != null && orderPS.getOrder() != null) {
      Order order = orderPS.getOrder();
      client = order.getClient();
      org = order.getOrganization();
      bp = order.getBusinessPartner();
      proj = order.getProject();
      sc = order.getSalesCampaign();
      act = order.getActivity();
    }

    if (invoicePS != null && invoicePS.getInvoice() != null) {
      Invoice invoice = invoicePS.getInvoice();
      client = invoice.getClient();
      org = invoice.getOrganization();
      bp = invoice.getBusinessPartner();
      proj = invoice.getProject();
      sc = invoice.getSalesCampaign();
      act = invoice.getActivity();
    }

    FIN_PaymentScheduleDetail psd = OBProvider.getInstance().get(FIN_PaymentScheduleDetail.class);
    psd.setClient(client);
    psd.setOrganization(org);
    psd.setInvoicePaymentSchedule(invoicePS);
    psd.setOrderPaymentSchedule(orderPS);
    psd.setAmount(amount);
    psd.setWriteoffAmount((writeOff == null) ? BigDecimal.ZERO : writeOff);
    psd.setBusinessPartner(bp);
    psd.setProject(proj);
    psd.setSalesCampaign(sc);
    psd.setActivity(act);
    if (paymentDetail != null) {
      psd.setPaymentDetails(paymentDetail);
    }
    OBDal.getInstance().save(psd);

    return psd;
  }

  public FIN_PaymentPropDetail getNewPaymentProposalDetail(Organization organization,
      FIN_PaymentProposal paymentProposal, FIN_PaymentScheduleDetail paymentScheduleDetail,
      BigDecimal amount, BigDecimal writeoffamount, GLItem glitem) {
    final FIN_PaymentPropDetail newPaymentProposalDetail = OBProvider.getInstance()
        .get(FIN_PaymentPropDetail.class);
    newPaymentProposalDetail.setOrganization(organization);
    newPaymentProposalDetail.setAmount(amount);
    if (writeoffamount != null) {
      newPaymentProposalDetail.setWriteoffAmount(writeoffamount);
    }
    if (glitem != null) {
      newPaymentProposalDetail.setGLItem(glitem);
    }
    newPaymentProposalDetail.setFINPaymentScheduledetail(paymentScheduleDetail);

    List<FIN_PaymentPropDetail> paymentProposalDetails = paymentProposal
        .getFINPaymentPropDetailList();
    paymentProposalDetails.add(newPaymentProposalDetail);
    paymentProposal.setFINPaymentPropDetailList(paymentProposalDetails);
    newPaymentProposalDetail.setFinPaymentProposal(paymentProposal);

    OBDal.getInstance().save(newPaymentProposalDetail);
    OBDal.getInstance().save(paymentProposal);
    OBDal.getInstance().flush();

    return newPaymentProposalDetail;
  }

  public FIN_FinaccTransaction getFinancialTransaction(FIN_Payment payment) {
    FIN_FinaccTransaction transaction = FIN_Utility.getOneInstance(FIN_FinaccTransaction.class,
        new Value(FIN_FinaccTransaction.PROPERTY_FINPAYMENT, payment));
    if (transaction == null) {
      transaction = getNewFinancialTransaction(payment.getOrganization(), payment.getAccount(),
          TransactionsDao.getTransactionMaxLineNo(payment.getAccount()) + 10, payment,
          payment.getDescription(), payment.getPaymentDate(), null, "RPPC",
          FIN_Utility.getDepositAmount(payment.isReceipt(),
              payment.getFinancialTransactionAmount()),
          FIN_Utility.getPaymentAmount(payment.isReceipt(),
              payment.getFinancialTransactionAmount()),
          payment.getProject(), payment.getSalesCampaign(), payment.getActivity(),
          payment.isReceipt() ? "BPD" : "BPW", payment.getPaymentDate(), payment.getCurrency(),
          payment.getFinancialTransactionConvertRate(), payment.getAmount());
    }
    return transaction;
  }

  public FIN_FinaccTransaction getNewFinancialTransaction(Organization organization,
      FIN_FinancialAccount account, Long line, FIN_Payment payment, String description,
      Date accountingDate, GLItem glItem, String status, BigDecimal depositAmount,
      BigDecimal paymentAmount, Project project, Campaign campaing, ABCActivity activity,
      String transactionType, Date statementDate, Currency paymentCurrency, BigDecimal convertRate,
      BigDecimal sourceAmount) {
    FIN_FinaccTransaction finTrans = OBProvider.getInstance().get(FIN_FinaccTransaction.class);
    finTrans.setActive(true);
    finTrans.setOrganization(organization);
    finTrans.setCurrency(account.getCurrency());
    finTrans.setAccount(account);
    finTrans.setLineNo(line);
    if (payment != null) {
      OBDal.getInstance().refresh(payment);
    }
    finTrans.setFinPayment(payment);
    String truncateDescription = null;
    if (description != null) {
      truncateDescription = (description.length() > 255)
          ? description.substring(0, 252).concat("...")
          : description;
    }
    finTrans.setDescription(truncateDescription);
    finTrans.setDateAcct(accountingDate);
    finTrans.setGLItem(glItem);
    finTrans.setStatus(status);
    finTrans.setDepositAmount(depositAmount);
    finTrans.setPaymentAmount(paymentAmount);
    finTrans.setProject(project);
    finTrans.setSalesCampaign(campaing);
    finTrans.setActivity(activity);
    finTrans.setTransactionType(transactionType);
    finTrans.setTransactionDate(statementDate);

    if (paymentCurrency != null && !paymentCurrency.equals(finTrans.getCurrency())) {
      finTrans.setForeignCurrency(paymentCurrency);
      finTrans.setForeignConversionRate(convertRate);
      finTrans.setForeignAmount(sourceAmount);
    }
    OBDal.getInstance().save(finTrans);
    OBDal.getInstance().flush();

    return finTrans;
  }

  public FIN_FinaccTransaction getNewFinancialTransaction(Organization organization,
      FIN_FinancialAccount account, Long line, FIN_Payment payment, String description,
      Date accountingDate, GLItem glItem, String status, BigDecimal depositAmount,
      BigDecimal paymentAmount, Project project, Campaign campaing, ABCActivity activity,
      String transactionType, Date statementDate, Currency paymentCurrency, BigDecimal convertRate,
      BigDecimal sourceAmount, BusinessPartner businessPartner, Product product,
      SalesRegion salesRegion) {

    final FIN_FinaccTransaction finTrans = getNewFinancialTransaction(organization, account, line,
        payment, description, accountingDate, glItem, status, depositAmount, paymentAmount, project,
        campaing, activity, transactionType, statementDate, paymentCurrency, convertRate,
        sourceAmount, businessPartner, product, salesRegion, null, null, null);

    return finTrans;
  }

  public FIN_FinaccTransaction getNewFinancialTransaction(Organization organization,
      FIN_FinancialAccount account, Long line, FIN_Payment payment, String description,
      Date accountingDate, GLItem glItem, String status, BigDecimal depositAmount,
      BigDecimal paymentAmount, Project project, Campaign campaing, ABCActivity activity,
      String transactionType, Date statementDate, Currency paymentCurrency, BigDecimal convertRate,
      BigDecimal sourceAmount, BusinessPartner businessPartner, Product product,
      SalesRegion salesRegion, UserDimension1 user1, UserDimension2 user2, Costcenter costcenter) {
    final FIN_FinaccTransaction finTrans = getNewFinancialTransaction(organization, account, line,
        payment, description, accountingDate, glItem, status, depositAmount, paymentAmount, project,
        campaing, activity, transactionType, statementDate, paymentCurrency, convertRate,
        sourceAmount);
    finTrans.setBusinessPartner(businessPartner);
    finTrans.setProduct(product);
    finTrans.setSalesRegion(salesRegion);
    finTrans.setCostCenter(costcenter);
    finTrans.setStDimension(user1);
    finTrans.setNdDimension(user2);

    OBDal.getInstance().save(finTrans);
    OBDal.getInstance().flush();

    return finTrans;
  }

  public FIN_Reconciliation getNewReconciliation(Organization org, FIN_FinancialAccount account,
      String documentNo, DocumentType docType, Date dateTo, Date statementDate,
      BigDecimal startingBalance, BigDecimal endingBalance, String docStatus) {
    FIN_Reconciliation finRecon = OBProvider.getInstance().get(FIN_Reconciliation.class);
    finRecon.setOrganization(org);
    finRecon.setAccount(account);
    finRecon.setDocumentNo(documentNo);
    finRecon.setDocumentType(docType);
    finRecon.setEndingDate(dateTo);
    finRecon.setTransactionDate(statementDate);
    finRecon.setDocumentStatus(docStatus);
    finRecon.setStartingbalance(startingBalance);
    finRecon.setEndingBalance(endingBalance);

    OBDal.getInstance().save(finRecon);
    OBDal.getInstance().flush();

    return finRecon;

  }

  public PaymentRun getNewPaymentRun(String sourceType, PaymentExecutionProcess executionProcess,
      Organization organization) {
    PaymentRun paymentRun = OBProvider.getInstance().get(PaymentRun.class);
    paymentRun.setStatus("P");
    paymentRun.setOrganization(organization);
    paymentRun.setPaymentExecutionProcess(executionProcess);
    paymentRun.setSourceOfTheExecution(sourceType);

    OBDal.getInstance().save(paymentRun);
    OBDal.getInstance().flush();
    return paymentRun;
  }

  public PaymentRunPayment getNewPaymentRunPayment(PaymentRun paymentRun, FIN_Payment payment) {
    PaymentRunPayment paymentRunPayment = OBProvider.getInstance().get(PaymentRunPayment.class);
    paymentRunPayment.setPaymentRun(paymentRun);
    paymentRunPayment.setPayment(payment);
    paymentRunPayment.setOrganization(paymentRun.getOrganization());
    paymentRunPayment.setResult("P");

    List<PaymentRunPayment> paymentRunPayments = paymentRun.getFinancialMgmtPaymentRunPaymentList();
    paymentRunPayments.add(paymentRunPayment);
    paymentRun.setFinancialMgmtPaymentRunPaymentList(paymentRunPayments);

    OBDal.getInstance().save(paymentRunPayment);
    OBDal.getInstance().save(paymentRun);
    OBDal.getInstance().flush();
    return paymentRunPayment;
  }

  public PaymentRunParameter getNewPaymentRunParameter(PaymentRun paymentRun,
      PaymentExecutionProcessParameter parameter, String value) {
    PaymentRunParameter paymentRunParameter = OBProvider.getInstance()
        .get(PaymentRunParameter.class);
    paymentRunParameter.setPaymentRun(paymentRun);
    paymentRunParameter.setOrganization(paymentRun.getOrganization());
    paymentRunParameter.setPaymentExecutionProcessParameter(parameter);
    if ("CHECK".equals(parameter.getInputType())) {
      paymentRunParameter.setValueOfTheCheck("Y".equals(value));
    } else if ("TEXT".equals(parameter.getInputType())) {
      paymentRunParameter.setValueOfTheTextParameter(value);
    }

    List<PaymentRunParameter> paymentRunParameters = paymentRun
        .getFinancialMgmtPaymentRunParameterList();
    paymentRunParameters.add(paymentRunParameter);
    paymentRun.setFinancialMgmtPaymentRunParameterList(paymentRunParameters);

    OBDal.getInstance().save(paymentRunParameter);
    OBDal.getInstance().save(paymentRun);
    OBDal.getInstance().flush();
    return paymentRunParameter;

  }

  public void duplicateScheduleDetail(FIN_PaymentScheduleDetail paymentScheduleDetail,
      BigDecimal writeoffAmount) {
    duplicateScheduleDetail(paymentScheduleDetail, writeoffAmount, null);
  }

  public void duplicateScheduleDetail(FIN_PaymentScheduleDetail paymentScheduleDetail,
      BigDecimal writeoffAmount, BigDecimal debtAmount) {
    final FIN_PaymentScheduleDetail newPaymentScheduleDetail = (FIN_PaymentScheduleDetail) DalUtil
        .copy(paymentScheduleDetail);
    newPaymentScheduleDetail.setCreationDate(new Date());
    newPaymentScheduleDetail.setAmount(writeoffAmount);
    newPaymentScheduleDetail.setDoubtfulDebtAmount(debtAmount);
    OBDal.getInstance().save(newPaymentScheduleDetail);
    OBDal.getInstance().flush();
  }

  /**
   * Deletes from database a given fin_payment_schedule row
   * 
   */
  public void removePaymentSchedule(FIN_PaymentSchedule fin_PaymentSchedule) {

    OBCriteria<FIN_PaymentScheduleDetail> obcPSD = OBDal.getInstance()
        .createCriteria(FIN_PaymentScheduleDetail.class);
    obcPSD.add(Restrictions.eq(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE,
        fin_PaymentSchedule));
    List<FIN_PaymentScheduleDetail> lPSD = obcPSD.list();
    Iterator<FIN_PaymentScheduleDetail> itPSD = lPSD.iterator();

    while (itPSD.hasNext()) {
      FIN_PaymentScheduleDetail psdToRemove = itPSD.next();
      fin_PaymentSchedule.getFINPaymentScheduleDetailInvoicePaymentScheduleList()
          .remove(psdToRemove);
      removePaymentScheduleDetail(psdToRemove);
    }
    OBDal.getInstance().remove(fin_PaymentSchedule);
    OBDal.getInstance().flush();
  }

  /**
   * Removes a payment schedule detail row from database
   * 
   */
  public void removePaymentScheduleDetail(FIN_PaymentScheduleDetail fin_PaymentScheduleDetail) {
    OBDal.getInstance().remove(fin_PaymentScheduleDetail);
    OBDal.getInstance().flush();
  }

  public List<FIN_PaymentPropDetail> getOrderedPaymentProposalDetails(
      FIN_PaymentProposal paymentProposal) {

    OBContext.setAdminMode();
    try {
      //@formatter:off
      final String whereClause = " as ppd "
           + " left outer join ppd.fINPaymentScheduledetail as psd"
           + " left outer join psd.invoicePaymentSchedule as ips"
           + " left outer join ips.invoice as inv"
           + " left outer join psd.orderPaymentSchedule as ops"
           + " left outer join ops.order as ord"
           + " where ppd.finPaymentProposal.id= :paymentProposalId"
           + " order by COALESCE (inv.businessPartner, ord.businessPartner)";

      //@formatter:on
      final OBQuery<FIN_PaymentPropDetail> obqPSD = OBDal.getInstance()
          .createQuery(FIN_PaymentPropDetail.class, whereClause);
      obqPSD.setNamedParameter("paymentProposalId", paymentProposal.getId());
      return obqPSD.list();

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public List<FIN_PaymentMethod> getFilteredPaymentMethods(String strFinancialAccountId,
      String strOrgId, boolean excludePaymentMethodWithoutAccount,
      PaymentDirection paymentDirection) {
    final OBCriteria<FIN_PaymentMethod> obc = OBDal.getInstance()
        .createCriteria(FIN_PaymentMethod.class);
    obc.add(Restrictions.in("organization.id",
        OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(strOrgId)));
    obc.setFilterOnReadableOrganization(false);

    Set<String> payMethods = new HashSet<>();
    if (strFinancialAccountId != null && !strFinancialAccountId.isEmpty()) {
      for (FinAccPaymentMethod finAccPayMethod : getObject(FIN_FinancialAccount.class,
          strFinancialAccountId).getFinancialMgmtFinAccPaymentMethodList()) {
        if (paymentDirection == PaymentDirection.EITHER) {
          payMethods.add(finAccPayMethod.getPaymentMethod().getId());
        } else if (paymentDirection == PaymentDirection.IN && finAccPayMethod.isPayinAllow()) {
          payMethods.add(finAccPayMethod.getPaymentMethod().getId());
        } else if (paymentDirection == PaymentDirection.OUT && finAccPayMethod.isPayoutAllow()) {
          payMethods.add(finAccPayMethod.getPaymentMethod().getId());
        }
        // else not valid for this type of payment
      }
      if (payMethods.isEmpty()) {
        return (new ArrayList<FIN_PaymentMethod>());
      }
      addPaymentMethodList(obc, new ArrayList<String>(payMethods));
    } else {
      if (excludePaymentMethodWithoutAccount) {

        final OBCriteria<FinAccPaymentMethod> obcExc = OBDal.getInstance()
            .createCriteria(FinAccPaymentMethod.class);
        obcExc.createAlias(FinAccPaymentMethod.PROPERTY_ACCOUNT, "acc");
        obcExc.add(Restrictions.in("acc.organization.id",
            OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(strOrgId)));
        obcExc.setFilterOnReadableOrganization(false);
        for (FinAccPaymentMethod fapm : obcExc.list()) {
          payMethods.add(fapm.getPaymentMethod().getId());
        }
        if (payMethods.isEmpty()) {
          return (new ArrayList<FIN_PaymentMethod>());
        }
        addPaymentMethodList(obc, new ArrayList<String>(payMethods));
      }
      if (paymentDirection == PaymentDirection.IN) {
        obc.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_PAYINALLOW, true));
      } else if (paymentDirection == PaymentDirection.OUT) {
        obc.add(Restrictions.eq(FIN_PaymentMethod.PROPERTY_PAYOUTALLOW, true));
      }
    }
    obc.addOrderBy(FIN_PaymentMethod.PROPERTY_NAME, true);
    return obc.list();
  }

  public String getDefaultPaymentMethodId(FIN_FinancialAccount account, boolean paymentIn) {
    final OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
        .createCriteria(FinAccPaymentMethod.class);
    obc.createAlias(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, "pm");
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, account));
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_DEFAULT, true));
    if (paymentIn) {
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYINALLOW, true));
    } else {
      obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYOUTALLOW, true));
    }
    obc.addOrder(org.hibernate.criterion.Order.asc("pm." + FIN_PaymentMethod.PROPERTY_NAME));
    obc.setFilterOnReadableOrganization(false);
    obc.setMaxResults(1);
    FinAccPaymentMethod paymentMethod = (FinAccPaymentMethod) obc.uniqueResult();
    if (paymentMethod != null) {
      return paymentMethod.getPaymentMethod().getId();
    } else {
      return "";
    }
  }

  /**
   * This method adds a filter to the Query to filter by Payment Methods Id's. However, since the
   * list can be bigger than 1000 records, it is necessary to split it to create several
   * "paymentMethod.id in ('')" clauses that have less than 1000 records.
   */
  private void addPaymentMethodList(OBCriteria<FIN_PaymentMethod> obc,
      List<String> paymentMethods) {
    Criterion compoundExp = null;
    int paymentMethodsSize = paymentMethods.size();
    int batchIni = 0;
    int batchSize = 1000;
    while (paymentMethodsSize > batchIni) {
      List<String> paymentMethodsToRemove = paymentMethods.subList(batchIni,
          Math.min(batchIni + batchSize, paymentMethodsSize));
      if (compoundExp == null) {
        compoundExp = Restrictions.in("id", paymentMethodsToRemove);
      } else {
        compoundExp = Restrictions.or(compoundExp, Restrictions.in("id", paymentMethodsToRemove));
      }
      batchIni += batchSize;
    }
    if (compoundExp != null) {
      obc.add(compoundExp);
    }
  }

  public List<FIN_FinancialAccount> getFilteredFinancialAccounts(String strPaymentMethodId,
      String strOrgId, String strCurrencyId, PaymentDirection paymentDirection) {
    final OBCriteria<FIN_FinancialAccount> obc = OBDal.getInstance()
        .createCriteria(FIN_FinancialAccount.class, "acc");
    obc.add(Restrictions.in("organization.id",
        OBContext.getOBContext().getOrganizationStructureProvider().getNaturalTree(strOrgId)));
    obc.setFilterOnReadableOrganization(false);

    Currency requiredCurrency = null;
    if (strCurrencyId != null && !strCurrencyId.isEmpty()) {
      DetachedCriteria multiCurrAllowed = DetachedCriteria
          .forEntityName(FinAccPaymentMethod.ENTITY_NAME, "fapm")
          .add(Restrictions.eqProperty(FinAccPaymentMethod.PROPERTY_ACCOUNT + ".id", "acc.id"));
      if (paymentDirection == PaymentDirection.IN || paymentDirection == PaymentDirection.EITHER) {
        multiCurrAllowed
            .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYINISMULTICURRENCY, true));
      }
      if (paymentDirection == PaymentDirection.OUT || paymentDirection == PaymentDirection.EITHER) {
        multiCurrAllowed
            .add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYOUTISMULTICURRENCY, true));
      }
      requiredCurrency = OBDal.getInstance().get(Currency.class, strCurrencyId);
      obc.add(
          Restrictions.or(Restrictions.eq(FIN_FinancialAccount.PROPERTY_CURRENCY, requiredCurrency),
              Subqueries.exists(multiCurrAllowed.setProjection(Projections.id()))));
    }

    if (strPaymentMethodId != null && !strPaymentMethodId.isEmpty()) {
      List<FinAccPaymentMethod> finAccsMethods = getObject(FIN_PaymentMethod.class,
          strPaymentMethodId).getFinancialMgmtFinAccPaymentMethodList();

      if (finAccsMethods.isEmpty()) {
        return (new ArrayList<FIN_FinancialAccount>());
      }
      ExpressionForFinAccPayMethod exp = new ExpressionForFinAccPayMethod();

      for (FinAccPaymentMethod finAccPayMethod : finAccsMethods) {
        boolean validPaymentDirection = true;
        if (paymentDirection == PaymentDirection.IN) {
          validPaymentDirection = finAccPayMethod.isPayinAllow();
        } else if (paymentDirection == PaymentDirection.OUT) {
          validPaymentDirection = finAccPayMethod.isPayoutAllow();
        }

        boolean validCurrency = true;
        if (requiredCurrency != null) {
          boolean multiCurrencyAllowed = false;
          if (paymentDirection == PaymentDirection.IN) {
            multiCurrencyAllowed = finAccPayMethod.isPayinIsMulticurrency();
          } else if (paymentDirection == PaymentDirection.OUT) {
            multiCurrencyAllowed = finAccPayMethod.isPayoutIsMulticurrency();
          } else if (paymentDirection == PaymentDirection.EITHER) {
            multiCurrencyAllowed = finAccPayMethod.isPayinIsMulticurrency()
                || finAccPayMethod.isPayoutIsMulticurrency();
          }

          validCurrency = multiCurrencyAllowed
              || requiredCurrency.equals(finAccPayMethod.getAccount().getCurrency());
        }

        if (validPaymentDirection && validCurrency) {
          exp.addFinAccPaymentMethod(finAccPayMethod);
        }

      }

      Criterion crit = exp.getCriterion();
      if (crit != null) {
        obc.add(crit);
      } else {
        return new ArrayList<FIN_FinancialAccount>();
      }
    }
    return obc.list();
  }

  private static class ExpressionForFinAccPayMethod {

    private int MAX = 999;

    private Criterion compoundexp = null;
    List<String> finAccs = new ArrayList<>();

    public void addFinAccPaymentMethod(FinAccPaymentMethod finAccPayMethod) {
      finAccs.add(finAccPayMethod.getAccount().getId());
      if (finAccs.size() >= MAX) {
        refresh();
      }
    }

    public Criterion getCriterion() {
      if (!finAccs.isEmpty()) {
        refresh();
      }
      return compoundexp;
    }

    private void refresh() {
      // finAccs size must be > 0
      if (compoundexp == null) {
        compoundexp = Restrictions.in("id", finAccs);
      } else {
        compoundexp = Restrictions.or(compoundexp, Restrictions.in("id", finAccs));
      }
      finAccs = new ArrayList<>();
    }
  }

  public FinAccPaymentMethod getFinancialAccountPaymentMethod(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod) {
    final OBCriteria<FinAccPaymentMethod> obc = OBDal.getInstance()
        .createCriteria(FinAccPaymentMethod.class);
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACCOUNT, account));
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_PAYMENTMETHOD, paymentMethod));
    obc.add(Restrictions.eq(FinAccPaymentMethod.PROPERTY_ACTIVE, true));
    obc.setFilterOnReadableClients(false);
    obc.setFilterOnReadableOrganization(false);
    try {
      return obc.list().get(0);
    } catch (IndexOutOfBoundsException e) {
      throw new OBException(FIN_Utility.messageBD("APRM_PaymentMethod"));
    }
  }

  public boolean isAutomatedExecutionPayment(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod, boolean isReceipt) {
    FinAccPaymentMethod finAccPaymentMethod = getFinancialAccountPaymentMethod(account,
        paymentMethod);
    return "A".equals(isReceipt ? finAccPaymentMethod.getPayinExecutionType()
        : finAccPaymentMethod.getPayoutExecutionType());
  }

  public boolean hasNotDeferredExecutionProcess(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod, Boolean isReceipt) {
    FinAccPaymentMethod finAccPaymentMethod = getFinancialAccountPaymentMethod(account,
        paymentMethod);
    return (isReceipt
        ? (finAccPaymentMethod.getPayinExecutionProcess() != null
            && !finAccPaymentMethod.isPayinDeferred())
        : (finAccPaymentMethod.getPayoutExecutionProcess() != null
            && !finAccPaymentMethod.isPayoutDeferred()));
  }

  public PaymentExecutionProcess getExecutionProcess(FIN_Payment payment) {
    return getExecutionProcess(payment.getAccount(), payment.getPaymentMethod(),
        payment.isReceipt());
  }

  public PaymentExecutionProcess getExecutionProcess(FIN_FinancialAccount account,
      FIN_PaymentMethod paymentMethod, Boolean receipt) {
    FinAccPaymentMethod finAccPaymentMethod = getFinancialAccountPaymentMethod(account,
        paymentMethod);
    return receipt ? finAccPaymentMethod.getPayinExecutionProcess()
        : finAccPaymentMethod.getPayoutExecutionProcess();
  }

  public boolean isAutomaticExecutionProcess(PaymentExecutionProcess executionProcess) {
    List<PaymentExecutionProcessParameter> parameters = executionProcess
        .getFinancialMgmtPaymentExecutionProcessParameterList();
    for (PaymentExecutionProcessParameter parameter : parameters) {
      if ("CONSTANT".equals(parameter.getParameterType())
          && StringUtils.isBlank(parameter.getDefaultTextValue())) {
        return false;
      } else if ("IN".equals(parameter.getParameterType())) {
        if ("CHECK".equals(parameter.getInputType()) && (parameter.getDefaultValueForFlag() == null
            || "".equals(parameter.getDefaultValueForFlag()))) {
          return false;
        } else if ("TEXT".equals(parameter.getInputType())
            && (parameter.getDefaultTextValue() == null
                || "".equals(parameter.getDefaultTextValue()))) {
          return false;
        }
      }
    }
    return true;
  }

  public List<PaymentExecutionProcessParameter> getInPaymentExecutionParameters(
      PaymentExecutionProcess executionProcess) {
    OBCriteria<PaymentExecutionProcessParameter> obc = OBDal.getInstance()
        .createCriteria(PaymentExecutionProcessParameter.class);
    obc.add(Restrictions.eq(PaymentExecutionProcessParameter.PROPERTY_PAYMENTEXECUTIONPROCESS,
        executionProcess));
    obc.add(Restrictions.eq(PaymentExecutionProcessParameter.PROPERTY_PARAMETERTYPE, "IN"));
    return obc.list();
  }

  public List<FIN_Payment> getPaymentProposalPayments(FIN_PaymentProposal paymentProposal) {
    List<FIN_Payment> paymentsInProposal = new ArrayList<>();
    for (FIN_PaymentPropDetail proposalDetail : paymentProposal.getFINPaymentPropDetailList()) {
      if ("RPAE".equals(proposalDetail.getFINPaymentScheduledetail()
          .getPaymentDetails()
          .getFinPayment()
          .getStatus())) {
        paymentsInProposal
            .add(proposalDetail.getFINPaymentScheduledetail().getPaymentDetails().getFinPayment());
      }
    }

    return paymentsInProposal;
  }

  /**
   * This method returns list of Payments that are in Awaiting Execution status and filtered by the
   * following parameters.
   * 
   * @param organizationId
   *          Organization
   * @param paymentMethodId
   *          Payment Method used for the payment.
   * @param financialAccountId
   *          Financial Account used for the payment.
   * @param dateFrom
   *          Optional. Filters payments made after the specified date.
   * @param dateTo
   *          Optional. Filters payments made before the specified date.
   * @param offset
   *          Starting register number.
   * @param pageSize
   *          Limited the max number of results.
   * @param strOrderByProperty
   *          Property used for ordering the results.
   * @param strAscDesc
   *          if true order by asc, if false order by desc
   * @param isReceipt
   *          if true sales, if false purchase
   * @return Filtered Payment list.
   */
  public List<FIN_Payment> getPayExecRowCount(String organizationId, String paymentMethodId,
      String financialAccountId, Date dateFrom, Date dateTo, int offset, int pageSize,
      String strOrderByProperty, String strAscDesc, boolean isReceipt) {

    List<FIN_Payment> emptyList = new ArrayList<>();
    if (organizationId == null || organizationId.isEmpty()) {
      return emptyList;
    }

    OBContext.setAdminMode();
    try {

      FIN_PaymentMethod obPayMethod = OBDal.getInstance()
          .get(FIN_PaymentMethod.class, paymentMethodId);
      FIN_FinancialAccount obFinAccount = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, financialAccountId);

      OBCriteria<FIN_Payment> obcPayment = OBDal.getInstance().createCriteria(FIN_Payment.class);
      obcPayment.add(Restrictions.in("organization.id",
          OBContext.getOBContext()
              .getOrganizationStructureProvider()
              .getParentTree(organizationId, true)));
      obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_STATUS, "RPAE"));
      obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_PAYMENTMETHOD, obPayMethod));
      obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_ACCOUNT, obFinAccount));
      obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, isReceipt));
      if (dateFrom != null) {
        obcPayment.add(Restrictions.ge(FIN_Payment.PROPERTY_PAYMENTDATE, dateFrom));
      }
      if (dateTo != null) {
        obcPayment.add(Restrictions.lt(FIN_Payment.PROPERTY_PAYMENTDATE, dateTo));
      }

      boolean ascDesc = true;
      if (strAscDesc != null && !strAscDesc.isEmpty()) {
        ascDesc = "asc".equalsIgnoreCase(strAscDesc);
      }
      if (strOrderByProperty != null && !strOrderByProperty.isEmpty()) {
        obcPayment.addOrderBy(strOrderByProperty, ascDesc);
      }
      obcPayment.setFirstResult(offset);
      obcPayment.setMaxResults(pageSize);

      return obcPayment.list();

    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public List<FIN_Payment> getPendingExecutionPayments(String strInvoiceId) {
    List<FIN_Payment> payments = new ArrayList<>();
    List<FIN_PaymentSchedule> paySchedList = new AdvPaymentMngtDao()
        .getObject(Invoice.class, strInvoiceId)
        .getFINPaymentScheduleList();
    OBCriteria<FIN_PaymentScheduleDetail> psdCriteria = OBDal.getInstance()
        .createCriteria(FIN_PaymentScheduleDetail.class);
    if (!paySchedList.isEmpty()) {
      psdCriteria.add(
          Restrictions.in(FIN_PaymentScheduleDetail.PROPERTY_INVOICEPAYMENTSCHEDULE, paySchedList));
      for (FIN_PaymentScheduleDetail psd : psdCriteria.list()) {
        if (psd.getPaymentDetails() != null) {
          FIN_Payment payment = psd.getPaymentDetails().getFinPayment();
          if ("RPAE".equals(payment.getStatus()) && hasNotDeferredExecutionProcess(
              payment.getAccount(), payment.getPaymentMethod(), payment.isReceipt())) {
            payments.add(payment);
          }
        }
      }
    }

    return payments;
  }

  public void setPaymentExecuting(FIN_Payment payment, boolean executing) {
    OBCriteria<APRMPendingPaymentFromInvoice> ppfiCriteria = OBDal.getInstance()
        .createCriteria(APRMPendingPaymentFromInvoice.class);
    ppfiCriteria.add(Restrictions.eq(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENT, payment));
    ppfiCriteria.setMaxResults(1);
    APRMPendingPaymentFromInvoice pendingPayment = (APRMPendingPaymentFromInvoice) ppfiCriteria
        .uniqueResult();
    if (pendingPayment != null) {
      pendingPayment.setProcessNow(executing);
      OBDal.getInstance().flush();
      OBDal.getInstance().save(pendingPayment);
    }
  }

  public boolean isPaymentBeingExecuted(FIN_Payment payment) {
    OBCriteria<APRMPendingPaymentFromInvoice> ppfiCriteria = OBDal.getInstance()
        .createCriteria(APRMPendingPaymentFromInvoice.class);
    ppfiCriteria.add(Restrictions.eq(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENT, payment));
    ppfiCriteria.setMaxResults(1);
    APRMPendingPaymentFromInvoice pendingPayment = (APRMPendingPaymentFromInvoice) ppfiCriteria
        .uniqueResult();
    if (pendingPayment != null) {
      return pendingPayment.isProcessNow();
    } else {
      return false;
    }

  }

  public void removeFromExecutionPending(FIN_Payment payment) {
    OBCriteria<APRMPendingPaymentFromInvoice> ppfiCriteria = OBDal.getInstance()
        .createCriteria(APRMPendingPaymentFromInvoice.class);
    ppfiCriteria.add(Restrictions.eq(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENT, payment));
    List<APRMPendingPaymentFromInvoice> pendingPayments = ppfiCriteria.list();
    OBDal.getInstance().remove(pendingPayments.get(0));
    OBDal.getInstance().flush();
  }

  public List<APRMPendingPaymentFromInvoice> getPendingPayments() {
    OBCriteria<APRMPendingPaymentFromInvoice> ppfiCriteria = OBDal.getInstance()
        .createCriteria(APRMPendingPaymentFromInvoice.class);
    ppfiCriteria.add(Restrictions.eq(APRMPendingPaymentFromInvoice.PROPERTY_PROCESSNOW, false));
    ppfiCriteria.addOrderBy(APRMPendingPaymentFromInvoice.PROPERTY_PAYMENTEXECUTIONPROCESS, false);
    ppfiCriteria.addOrderBy(APRMPendingPaymentFromInvoice.PROPERTY_ORGANIZATION, false);
    return ppfiCriteria.list();
  }

  public BigDecimal getCustomerCredit(BusinessPartner bp, boolean isReceipt, Organization Org) {
    return getCustomerCredit(bp, isReceipt, Org, null);
  }

  public BigDecimal getCustomerCredit(BusinessPartner bp, boolean isReceipt, Organization Org,
      Currency currency) {
    BigDecimal creditAmount = BigDecimal.ZERO;
    List<FIN_Payment> paymentList = getCustomerPaymentsWithCredit(Org, bp, isReceipt, currency);
    for (FIN_Payment payment : paymentList) {
      creditAmount = creditAmount.add(payment.getGeneratedCredit())
          .subtract(payment.getUsedCredit());
    }
    return creditAmount;
  }

  /**
   * Returns the list of credit payments for the selected business partner that belongs to the legal
   * entity's natural tree of the given organization
   */
  public List<FIN_Payment> getCustomerPaymentsWithCredit(Organization org, BusinessPartner bp,
      boolean isReceipt) {
    return getCustomerPaymentsWithCredit(org, bp, isReceipt, null);
  }

  /**
   * Returns the list of credit payments for the selected business partner that belongs to the legal
   * entity's natural tree of the given organization
   */
  public List<FIN_Payment> getCustomerPaymentsWithCredit(Organization org, BusinessPartner bp,
      boolean isReceipt, Currency currency) {

    try {
      OBContext.setAdminMode(false);

      final Organization legalEntity = FIN_Utility.getLegalEntityOrg(org);
      Set<String> orgIds = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getChildTree(legalEntity.getId(), true);

      final Map<String, Object> params = new HashMap<String, Object>();

      //@formatter:off
      String hql = "select p "
           + " from FIN_Payment as p "
           + " where p.businessPartner.id = :bpartnerId "
           + "  and p.receipt = :isReceipt "
           + "  and p.organization.id in (:orgIds) "
           + "  and obequals(p.generatedCredit, p.usedCredit) = 'N' "
           + "  and p.generatedCredit <> 0 ";
      if (currency != null) {
        hql += " and p.currency.id = :currencyId";
        params.put("currencyId", currency.getId());
      }

      hql += " order by p.paymentDate asc, p.documentNo asc ";

      params.put("bpartnerId", bp.getId());
      params.put("isReceipt", isReceipt);
      params.put("orgIds", orgIds);

      //@formatter:on
      final Session session = OBDal.getInstance().getSession();
      final Query<FIN_Payment> query = session.createQuery(hql, FIN_Payment.class);
      query.setProperties(params);

      final List<FIN_Payment> queryList = query.list();
      final List<FIN_Payment> paymentList = new ArrayList<>();
      for (FIN_Payment fp : queryList) {
        if ((FIN_Utility.seqnumberpaymentstatus(fp.getStatus())) >= (FIN_Utility
            .seqnumberpaymentstatus(FIN_Utility.invoicePaymentStatus(fp)))) {
          paymentList.add(fp);
        }

      }
      return paymentList;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public List<FIN_Payment> getCustomerPaymentsWithUsedCredit(BusinessPartner bp,
      Boolean isReceipt) {
    OBCriteria<FIN_Payment> obcPayment = OBDal.getInstance().createCriteria(FIN_Payment.class);
    obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_BUSINESSPARTNER, bp));
    obcPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, isReceipt));
    obcPayment.add(Restrictions.ne(FIN_Payment.PROPERTY_GENERATEDCREDIT, BigDecimal.ZERO));
    obcPayment.add(Restrictions.ne(FIN_Payment.PROPERTY_USEDCREDIT, BigDecimal.ZERO));
    obcPayment.addOrderBy(FIN_Payment.PROPERTY_PAYMENTDATE, false);
    obcPayment.addOrderBy(FIN_Payment.PROPERTY_DOCUMENTNO, false);
    return obcPayment.list();
  }

  public boolean existsAPRMReadyPreference() {
    OBCriteria<Preference> obcPreference = OBDal.getInstance().createCriteria(Preference.class);
    obcPreference.setFilterOnReadableClients(false);
    obcPreference.setFilterOnReadableOrganization(false);
    obcPreference.add(Restrictions.eq(Preference.PROPERTY_ATTRIBUTE, "APRM_Ready"));

    return obcPreference.count() > 0;
  }

  /**
   * Create a preference to be able to determine that the instance is ready to use APRM.
   */
  public void createAPRMReadyPreference() {
    Organization org0 = OBDal.getInstance().get(Organization.class, "0");
    Client client0 = OBDal.getInstance().get(Client.class, "0");

    Preference newPref = OBProvider.getInstance().get(Preference.class);
    newPref.setClient(client0);
    newPref.setOrganization(org0);
    newPref.setPropertyList(false);
    newPref.setAttribute("APRM_Ready");

    OBDal.getInstance().save(newPref);
  }

  /**
   * Gets the oldest credit payment for the given parameters
   * 
   * @param organization
   * @param businessPartner
   * @param amount
   * @param currency
   * @param isReceipt
   * @param toDate
   * @return if exists, returns the credit payment, else returns null
   */
  public FIN_Payment getCreditPayment(final Organization organization,
      final BusinessPartner businessPartner, final BigDecimal amount, final Currency currency,
      final boolean isReceipt, final Date toDate) {
    try {
      OBContext.setAdminMode(false);
      OBCriteria<FIN_Payment> obcFinPayment = OBDal.getInstance().createCriteria(FIN_Payment.class);
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_PROCESSED, true));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_BUSINESSPARTNER, businessPartner));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_GENERATEDCREDIT, amount));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_USEDCREDIT, BigDecimal.ZERO));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_CURRENCY, currency));
      obcFinPayment.add(Restrictions.eq(FIN_Payment.PROPERTY_RECEIPT, isReceipt));
      obcFinPayment.add(Restrictions.le(FIN_Payment.PROPERTY_PAYMENTDATE, toDate));
      obcFinPayment.add(Restrictions.in("organization.id",
          OBContext.getOBContext()
              .getOrganizationStructureProvider()
              .getNaturalTree(organization.getId())));
      obcFinPayment.addOrderBy(FIN_Payment.PROPERTY_PAYMENTDATE, true);
      obcFinPayment.setMaxResults(1);
      return (FIN_Payment) obcFinPayment.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Gets the oldest credit payment for the given invoice
   * 
   * @param invoice
   * @return if exists, returns the credit payment, else returns null
   */
  public FIN_Payment getCreditPayment(final Invoice invoice) {
    if (invoice == null) {
      return null;
    } else {
      return getCreditPayment(invoice.getOrganization(), invoice.getBusinessPartner(),
          invoice.getGrandTotalAmount(), invoice.getCurrency(), invoice.isSalesTransaction(),
          invoice.getInvoiceDate());
    }
  }

  /**
   * Returns true in case the provided status of the payment has already recognized the amount as
   * paid
   * 
   */
  public boolean isPaymentMadeStatus(String paymentStatus) {
    ArrayList<String> paidStatusList = new ArrayList<>();
    paidStatusList.add(PAYMENT_STATUS_PAYMENT_CLEARED);
    paidStatusList.add(PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    paidStatusList.add(PAYMENT_STATUS_PAYMENT_MADE);
    paidStatusList.add(PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    paidStatusList.add(PAYMENT_STATUS_PAYMENT_RECEIVED);

    return paidStatusList.contains(paymentStatus);
  }
}
