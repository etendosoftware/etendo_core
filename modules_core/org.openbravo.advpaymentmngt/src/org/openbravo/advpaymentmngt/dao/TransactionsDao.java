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
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.dao;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

public class TransactionsDao {

  public static FIN_FinaccTransaction createFinAccTransaction(FIN_Payment payment) {
    final FIN_FinaccTransaction newTransaction = OBProvider.getInstance()
        .get(FIN_FinaccTransaction.class);
    OBContext.setAdminMode();
    try {
      newTransaction.setId(payment.getId());
      newTransaction.setNewOBObject(true);
      newTransaction.setFinPayment(payment);
      newTransaction.setOrganization(payment.getOrganization());
      newTransaction.setAccount(payment.getAccount());
      newTransaction.setDateAcct(payment.getPaymentDate());
      newTransaction.setTransactionDate(payment.getPaymentDate());
      newTransaction.setActivity(payment.getActivity());
      newTransaction.setProject(payment.getProject());
      newTransaction.setCostCenter(payment.getCostCenter());
      newTransaction.setStDimension(payment.getStDimension());
      newTransaction.setNdDimension(payment.getNdDimension());
      newTransaction.setCurrency(payment.getAccount().getCurrency());
      String desc = "";
      if (payment.getDescription() != null && !payment.getDescription().isEmpty()) {
        desc = payment.getDescription()
            .replace("\n", ". ")
            .substring(0,
                payment.getDescription().length() > 254 ? 254 : payment.getDescription().length());
      }
      newTransaction.setDescription(desc);
      newTransaction.setClient(payment.getClient());
      newTransaction.setLineNo(getTransactionMaxLineNo(payment.getAccount()) + 10);

      BigDecimal depositAmt = FIN_Utility.getDepositAmount(
          payment.getDocumentType().getDocumentCategory().equals("ARR"),
          payment.getFinancialTransactionAmount());
      BigDecimal paymentAmt = FIN_Utility.getPaymentAmount(
          payment.getDocumentType().getDocumentCategory().equals("ARR"),
          payment.getFinancialTransactionAmount());

      newTransaction.setDepositAmount(depositAmt);
      newTransaction.setPaymentAmount(paymentAmt);
      newTransaction.setStatus(
          newTransaction.getDepositAmount().compareTo(newTransaction.getPaymentAmount()) > 0 ? "RPR"
              : "PPM");
      if (!newTransaction.getCurrency().equals(payment.getCurrency())) {
        newTransaction.setForeignCurrency(payment.getCurrency());
        newTransaction.setForeignConversionRate(payment.getFinancialTransactionConvertRate());
        newTransaction.setForeignAmount(payment.getAmount());
      }
      payment.getFINFinaccTransactionList().add(newTransaction);
      OBDal.getInstance().save(newTransaction);
      OBDal.getInstance().flush();
    } finally {
      OBContext.restorePreviousMode();
    }
    return newTransaction;
  }

  public static Long getTransactionMaxLineNo(FIN_FinancialAccount financialAccount) {
    //@formatter:off
    String hql = "select max(f.lineNo) as maxLineno "
        + "from FIN_Finacc_Transaction as f "
        + "where account.id = :accountId";
    //@formatter:on
    Query<Long> query = OBDal.getInstance().getSession().createQuery(hql, Long.class);
    query.setParameter("accountId", financialAccount.getId());
    Long maxLineNo = query.uniqueResult();
    if (maxLineNo != null) {
      return maxLineNo;
    }
    return 0l;
  }

  public static FIN_Reconciliation getLastReconciliation(FIN_FinancialAccount account,
      String isProcessed) {
    OBContext.setAdminMode();
    try {
      final OBCriteria<FIN_Reconciliation> obc = OBDal.getInstance()
          .createCriteria(FIN_Reconciliation.class);
      obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_ACCOUNT, account));
      if ("Y".equals(isProcessed)) {
        obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_PROCESSED, true));
      } else if ("N".equals(isProcessed)) {
        obc.add(Restrictions.eq(FIN_Reconciliation.PROPERTY_PROCESSED, false));
      }
      obc.addOrderBy(FIN_Reconciliation.PROPERTY_ENDINGDATE, false);
      obc.addOrderBy(FIN_Reconciliation.PROPERTY_CREATIONDATE, false);
      obc.setMaxResults(1);
      return (FIN_Reconciliation) obc.uniqueResult();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static void updateAccountingDate(FIN_FinaccTransaction transaction) {
    final String FIN_FINACC_TRANSACTION_TABLE = "4D8C3B3C31D1410DA046140C9F024D17";
    OBCriteria<AccountingFact> obcAF = OBDal.getInstance().createCriteria(AccountingFact.class);
    obcAF.add(Restrictions.eq(AccountingFact.PROPERTY_TABLE,
        OBDal.getInstance().get(Table.class, FIN_FINACC_TRANSACTION_TABLE)));
    obcAF.add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, transaction.getId()));
    for (AccountingFact aFact : obcAF.list()) {
      aFact.setAccountingDate(transaction.getTransactionDate());
      aFact.setTransactionDate(transaction.getTransactionDate());
      aFact.setPeriod((AccDefUtility.getCurrentPeriod(transaction.getTransactionDate(),
          AccDefUtility.getCalendar(transaction.getOrganization()))));
    }
    return;
  }

  public static FieldProvider[] getTransactionsFiltered(FIN_FinancialAccount account,
      Date statementDate, boolean hideAfterDate) {

    String dateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    SimpleDateFormat dateFormater = new SimpleDateFormat(dateFormat);
    OBContext.setAdminMode();
    try {

      final Map<String, Object> parameters = new HashMap<>();
      //@formatter:off
      String whereClause = " as ft"
          + " left outer join ft.reconciliation as rec"
          + " where ft.account.id = :accountId"
          + "   and (rec is null or rec.processed = 'N')"
          + "   and ft.processed = 'Y'";
      parameters.put("accountId", account.getId());
      if (hideAfterDate) {
        whereClause += " and ft.transactionDate < :statementDate";
        parameters.put("statementDate", statementDate);
      }
      whereClause += " order by ft.transactionDate, ft.lineNo";

      //@formatter:on
      final OBQuery<FIN_FinaccTransaction> obQuery = OBDal.getInstance()
          .createQuery(FIN_FinaccTransaction.class, whereClause, parameters);

      List<FIN_FinaccTransaction> transactionOBList = obQuery.list();

      FIN_FinaccTransaction[] FIN_Transactions = new FIN_FinaccTransaction[0];
      FIN_Transactions = transactionOBList.toArray(FIN_Transactions);
      FieldProvider[] data = FieldProviderFactory.getFieldProviderArray(transactionOBList);

      for (int i = 0; i < data.length; i++) {
        String strPaymentDocNo = "";
        String strBusinessPartner = "";
        FieldProviderFactory.setField(data[i], "transactionId", FIN_Transactions[i].getId());
        FieldProviderFactory.setField(data[i], "transactionDate",
            dateFormater.format(FIN_Transactions[i].getTransactionDate()));
        if (FIN_Transactions[i].getFinPayment() != null) {
          if (FIN_Transactions[i].getFinPayment().getBusinessPartner() != null) {
            strBusinessPartner = FIN_Transactions[i].getFinPayment().getBusinessPartner().getName();
          }
          strPaymentDocNo = FIN_Transactions[i].getFinPayment().getDocumentNo();
        }

        // Truncate business partner name
        String truncateBPname = (strBusinessPartner.length() > 30)
            ? strBusinessPartner.substring(0, 27).concat("...").toString()
            : strBusinessPartner;
        FieldProviderFactory.setField(data[i], "businessPartner",
            (strBusinessPartner.length() > 30) ? strBusinessPartner : "");
        FieldProviderFactory.setField(data[i], "businessPartnerTrunc", truncateBPname);

        FieldProviderFactory.setField(data[i], "paymentDocument", strPaymentDocNo);

        // Truncate description
        String description = FIN_Transactions[i].getDescription();
        String truncateDescription = "";
        if (description != null) {
          truncateDescription = (description.length() > 42)
              ? description.substring(0, 39).concat("...").toString()
              : description;
        }
        FieldProviderFactory.setField(data[i], "description",
            (description != null && description.length() > 42) ? description : "");
        FieldProviderFactory.setField(data[i], "descriptionTrunc", truncateDescription);

        FieldProviderFactory.setField(data[i], "paymentAmount",
            FIN_Transactions[i].getPaymentAmount().toString());
        FieldProviderFactory.setField(data[i], "depositAmount",
            FIN_Transactions[i].getDepositAmount().toString());
        FieldProviderFactory.setField(data[i], "rownum", "" + (i + 1));
        FieldProviderFactory.setField(data[i], "markSelectedId",
            (FIN_Transactions[i].getStatus().equals("RPPC")) ? FIN_Transactions[i].getId() : "");
      }

      return data;

    } catch (Exception e) {
      throw new OBException(e);

    } finally {
      OBContext.restorePreviousMode();
    }

  }

  public static BigDecimal getCurrentlyClearedAmt(String strAccountId) {
    //@formatter:off
    final String hqlString = "select sum(ft.depositAmount) - sum(ft.paymentAmount)"
        + " from FIN_Finacc_Transaction as ft"
        + " left outer join ft.reconciliation as rec"
        + " where ft.account.id = :accountId"
        + "   and rec.processed = 'N'"
        + "   and ft.processed = 'Y'";

    //@formatter:on
    final Session session = OBDal.getInstance().getSession();
    final Query<BigDecimal> query = session.createQuery(hqlString, BigDecimal.class);
    query.setParameter("accountId", strAccountId);
    BigDecimal resultObject = query.uniqueResult();
    if (resultObject != null) {
      return resultObject;
    }
    return BigDecimal.ZERO;
  }
}
