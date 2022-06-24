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
 * All portions are Copyright (C) 2010-2012 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.advpaymentmngt.algorithm;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletException;

import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.utility.APRM_MatchingUtility;
import org.openbravo.advpaymentmngt.utility.FIN_MatchedTransaction;
import org.openbravo.advpaymentmngt.utility.FIN_MatchingAlgorithm;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.MatchingAlgorithm;
import org.openbravo.service.db.DalConnectionProvider;

public class StandardMatchingAlgorithm implements FIN_MatchingAlgorithm {

  @Override
  public FIN_MatchedTransaction match(FIN_BankStatementLine line,
      List<FIN_FinaccTransaction> excluded) throws ServletException {

    MatchingAlgorithm algorithm = line.getBankStatement().getAccount().getMatchingAlgorithm();

    Date transactionDate = (algorithm.isMatchtransactiondate()) ? line.getTransactionDate() : null;
    String reference = (algorithm.isMatchreference()) ? line.getReferenceNo() : "";

    List<FIN_FinaccTransaction> transactions = new ArrayList<FIN_FinaccTransaction>();
    if (line.getGLItem() != null) {
      transactions = MatchTransactionDao.getMatchingGLItemTransaction(
          line.getBankStatement().getAccount().getId(), line.getGLItem(), line.getTransactionDate(),
          (line.getCramount().subtract(line.getDramount())), excluded);
      if (transactions.isEmpty()) {
        transactions = MatchTransactionDao.getMatchingGLItemTransaction(
            line.getBankStatement().getAccount().getId(), line.getGLItem(), null,
            (line.getCramount().subtract(line.getDramount())), excluded);
        if (!transactions.isEmpty()) {
          return new FIN_MatchedTransaction(transactions.get(0), FIN_MatchedTransaction.WEAK);
        }
      } else {
        return new FIN_MatchedTransaction(transactions.get(0), FIN_MatchedTransaction.STRONG);
      }
    }
    if (algorithm.isMatchbpname()) {
      transactions = MatchTransactionDao.getMatchingFinancialTransaction(
          line.getBankStatement().getAccount().getId(), transactionDate, reference,
          (line.getCramount().subtract(line.getDramount())), line.getBpartnername(), excluded);
    } else {
      transactions = MatchTransactionDao.getMatchingFinancialTransaction(
          line.getBankStatement().getAccount().getId(), transactionDate, reference,
          (line.getCramount().subtract(line.getDramount())), excluded);
    }

    if (!transactions.isEmpty()) {
      return new FIN_MatchedTransaction(transactions.get(0), FIN_MatchedTransaction.STRONG);
    }
    if (algorithm.isMatchtransactiondate()) {
      transactions = MatchTransactionDao.getMatchingFinancialTransaction(
          line.getBankStatement().getAccount().getId(), line.getTransactionDate(),
          line.getCramount().subtract(line.getDramount()), excluded);
    } else {
      transactions = MatchTransactionDao.getMatchingFinancialTransaction(
          line.getBankStatement().getAccount().getId(),
          line.getCramount().subtract(line.getDramount()), excluded);
    }
    if (!transactions.isEmpty()) {
      return new FIN_MatchedTransaction(transactions.get(0), FIN_MatchedTransaction.WEAK);
    }

    return new FIN_MatchedTransaction(null, FIN_MatchedTransaction.NOMATCH);
  }

  @Override
  public void unmatch(FIN_FinaccTransaction _transaction) throws ServletException {
    if (_transaction == null) {
      return;
    }
    FIN_Payment payment = _transaction.getFinPayment();
    VariablesSecureApp vars = new VariablesSecureApp(OBContext.getOBContext().getUser().getId(),
        OBContext.getOBContext().getCurrentClient().getId(),
        OBContext.getOBContext().getCurrentOrganization().getId(),
        OBContext.getOBContext().getRole().getId());
    // flush is set to true because pl is called in removeTransaction and removePayment
    ConnectionProvider conn = new DalConnectionProvider(true);
    if (_transaction.isCreatedByAlgorithm()) {
      APRM_MatchingUtility.removeTransaction(vars, conn, _transaction);
    } else {
      return;
    }
    if (payment.isCreatedByAlgorithm()) {
      APRM_MatchingUtility.removePayment(vars, conn, payment);
    }
    return;
  }
}
