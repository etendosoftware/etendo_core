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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.advpaymentmngt.actionHandler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.service.json.JsonUtils;

/**
 * This class implements the ability to transfer funds among financial accounts in a simple and
 * quick way. The idea is to have a button in the Financial Account window to transfer money.
 * 
 * @author Daniel Martins
 */
public class FundsTransferActionHandler extends BaseProcessActionHandler {
  private static final String ERROR_IN_PROCESS = "Error in process";
  private static final Logger log = LogManager.getLogger();
  private static final String BP_DEPOSIT = "BPD";
  private static final String BP_WITHDRAWAL = "BPW";
  private static final String BANK_FEE = "BF";
  private static final String PROCESS_ACTION = "P";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {

    try {
      JSONObject request = new JSONObject(content);
      JSONObject jsonParams = request.getJSONObject("_params");

      // Format Date
      String strTrxDate = jsonParams.getString("trxdate");
      Date trxDate = JsonUtils.createDateFormat().parse(strTrxDate);

      // Account from
      String strAccountFrom = request.getString("inpfinFinancialAccountId");
      FIN_FinancialAccount accountFrom = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strAccountFrom);

      // Account to
      String strAccountTo = jsonParams.getString("fin_financial_account_id");
      FIN_FinancialAccount accountTo = OBDal.getInstance()
          .get(FIN_FinancialAccount.class, strAccountTo);

      // GL item
      String strGLItem = jsonParams.getString("glitem");
      GLItem glitem = OBDal.getInstance().get(GLItem.class, strGLItem);

      // Amount
      BigDecimal amount = new BigDecimal(jsonParams.getString("deposit_amount"));

      String description = jsonParams.getString("description");

      // Conversion Rate
      BigDecimal manualConversionRate = null;
      if (accountFrom.getCurrency().getId().equalsIgnoreCase(accountTo.getCurrency().getId())) {
        manualConversionRate = BigDecimal.ONE;
      } else if (!jsonParams.isNull("multiply_rate")) {
        manualConversionRate = new BigDecimal(jsonParams.getString("multiply_rate"));
      }

      // Fees
      BigDecimal bankFeeFrom = BigDecimal.ZERO;
      BigDecimal bankFeeTo = BigDecimal.ZERO;
      if (jsonParams.getBoolean("bank_fee")) {
        if (!jsonParams.isNull("bank_fee_from")) {
          bankFeeFrom = new BigDecimal(jsonParams.getString("bank_fee_from"));
        }
        if (!jsonParams.isNull("bank_fee_to")) {
          bankFeeTo = new BigDecimal(jsonParams.getString("bank_fee_to"));
        }
      }

      createTransfer(trxDate, accountFrom, accountTo, glitem, amount, manualConversionRate,
          bankFeeFrom, bankFeeTo, description);

    } catch (OBException e) {
      log.error(ERROR_IN_PROCESS, e);

      return getResponseBuilder()
          .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.messageBD("error"),
              e.getMessage(), true)
          .retryExecution()
          .build();
    } catch (Exception e) {
      log.error(ERROR_IN_PROCESS, e);
      return getResponseBuilder()
          .showMsgInProcessView(MessageType.ERROR, OBMessageUtils.messageBD("error"),
              OBMessageUtils.messageBD("APRM_UnknownError"))
          .build();
    }
    return getResponseBuilder()
        .showMsgInProcessView(MessageType.SUCCESS, OBMessageUtils.messageBD("success"),
            OBMessageUtils.messageBD("APRM_TransferFundsSuccess"))
        .refreshGrid()
        .build();
  }

  /**
   * Create all the transactions for a funds transfer between two accounts
   * 
   * @param date
   *          used for the transactions, current date is used if this is null.
   * @param accountFrom
   *          source account.
   * @param accountTo
   *          target account.
   * @param glitem
   *          gl item used in transactions.
   * @param amount
   *          the transfer amount.
   * @param manualConversionRate
   *          conversion rate to override the system one.
   * @param bankFeeFrom
   *          fee on the source bank.
   * @param bankFeeTo
   *          fee on the target bank.
   */
  public static void createTransfer(Date date, FIN_FinancialAccount accountFrom,
      FIN_FinancialAccount accountTo, GLItem glitem, BigDecimal amount,
      BigDecimal manualConversionRate, BigDecimal bankFeeFrom, BigDecimal bankFeeTo) {
    createTransfer(date, accountFrom, accountTo, glitem, amount, manualConversionRate, bankFeeFrom,
        bankFeeTo);
  }

  /**
   * Create all the transactions for a funds transfer between two accounts
   * 
   * @param date
   *          used for the transactions, current date is used if this is null.
   * @param accountFrom
   *          source account.
   * @param accountTo
   *          target account.
   * @param glitem
   *          gl item used in transactions.
   * @param amount
   *          the transfer amount.
   * @param manualConversionRate
   *          conversion rate to override the system one.
   * @param bankFeeFrom
   *          fee on the source bank.
   * @param bankFeeTo
   *          fee on the target bank.
   * @param description
   *          description set by the user in the Funds Transfer Process.
   */
  public static void createTransfer(Date date, FIN_FinancialAccount accountFrom,
      FIN_FinancialAccount accountTo, GLItem glitem, BigDecimal amount,
      BigDecimal manualConversionRate, BigDecimal bankFeeFrom, BigDecimal bankFeeTo,
      String description) {
    List<FIN_FinaccTransaction> transactions = new ArrayList<FIN_FinaccTransaction>();
    FIN_FinaccTransaction newTrx;
    Date trxDate = date;

    if (trxDate == null) {
      trxDate = new Date();
    }

    try {
      LineNumberUtil lineNoUtil = new LineNumberUtil();
      BigDecimal targetAmount = convertAmount(amount, accountFrom, accountTo, trxDate,
          manualConversionRate);

      // Source Account
      FIN_FinaccTransaction sourceTrx = createTransaction(accountFrom, BP_WITHDRAWAL, trxDate,
          glitem, amount, lineNoUtil, description);
      transactions.add(sourceTrx);
      if (bankFeeFrom != null && BigDecimal.ZERO.compareTo(bankFeeFrom) != 0) {
        newTrx = createTransaction(accountFrom, BANK_FEE, trxDate, glitem, bankFeeFrom, lineNoUtil,
            description);
        transactions.add(newTrx);
      }

      // Target Account
      newTrx = createTransaction(accountTo, BP_DEPOSIT, trxDate, glitem, targetAmount, lineNoUtil,
          sourceTrx, description);
      transactions.add(newTrx);

      if (bankFeeTo != null && BigDecimal.ZERO.compareTo(bankFeeTo) != 0) {
        newTrx = createTransaction(accountTo, BANK_FEE, trxDate, glitem, bankFeeTo, lineNoUtil,
            sourceTrx, description);
        transactions.add(newTrx);
      }

      OBDal.getInstance().flush();
      processTransactions(transactions);

      WeldUtils.getInstanceFromStaticBeanManager(FundsTransferHookCaller.class)
          .executeHook(transactions);

    } catch (Exception e) {
      String message = OBMessageUtils.parseTranslation(e.getMessage());
      throw new OBException(message, e);
    }
  }

  private static BigDecimal convertAmount(BigDecimal amount, FIN_FinancialAccount accountFrom,
      FIN_FinancialAccount accountTo, Date date, BigDecimal rate) {
    if (rate != null) {
      int precision = accountTo.getCurrency().getStandardPrecision().intValue();
      return amount.multiply(rate).setScale(precision, RoundingMode.HALF_UP);
    } else {
      return FinancialUtils.getConvertedAmount(amount, accountFrom.getCurrency(),
          accountTo.getCurrency(), date, accountFrom.getOrganization(), null);
    }
  }

  private static FIN_FinaccTransaction createTransaction(FIN_FinancialAccount account,
      String trxType, Date trxDate, GLItem glitem, BigDecimal amount, LineNumberUtil lineNoUtil,
      String description) {
    return createTransaction(account, trxType, trxDate, glitem, amount, lineNoUtil, null,
        description);
  }

  private static FIN_FinaccTransaction createTransaction(FIN_FinancialAccount account,
      String trxType, Date trxDate, GLItem glitem, BigDecimal amount, LineNumberUtil lineNoUtil,
      FIN_FinaccTransaction sourceTrx, String description) {
    FIN_FinaccTransaction trx = OBProvider.getInstance().get(FIN_FinaccTransaction.class);

    trx.setAccount(account);
    trx.setTransactionType(trxType);
    trx.setTransactionDate(trxDate);
    trx.setDateAcct(trxDate);
    trx.setGLItem(glitem);
    trx.setCurrency(account.getCurrency());
    if (BP_DEPOSIT.equalsIgnoreCase(trxType)) {
      trx.setDepositAmount(amount);
    } else {
      trx.setPaymentAmount(amount);
    }
    // If the user has access to the Organization of the Financial Account, the Transaction is
    // created for it. If not, the Organization of the context is used instead
    if (OBContext.getOBContext()
        .getWritableOrganizations()
        .contains(account.getOrganization().getId())) {
      trx.setOrganization(account.getOrganization());
    } else {
      trx.setOrganization(OBContext.getOBContext().getCurrentOrganization());
    }

    Long line = lineNoUtil.getNextLineNumber(account);
    trx.setLineNo(line);

    trx.setAprmFinaccTransOrigin(sourceTrx);
    if (StringUtils.isNotEmpty(description)) {
      trx.setDescription(description);
    } else {
      trx.setDescription(OBMessageUtils.messageBD("FundsTransfer"));
    }

    OBDal.getInstance().save(trx);

    return trx;
  }

  private static void processTransactions(List<FIN_FinaccTransaction> transactions) {
    for (FIN_FinaccTransaction trx : transactions) {
      FIN_TransactionProcess.doTransactionProcess(PROCESS_ACTION, trx);
    }
  }

  /**
   * This class exists because TransactionsDao.getTransactionMaxLineNo does not take into account
   * not flushed transactions
   * 
   */
  private static class LineNumberUtil {
    private HashMap<FIN_FinancialAccount, Long> lastLineNo = new HashMap<FIN_FinancialAccount, Long>();

    protected Long getNextLineNumber(FIN_FinancialAccount account) {
      Long lineNo = lastLineNo.get(account);

      if (lineNo == null) {
        lineNo = TransactionsDao.getTransactionMaxLineNo(account);
      }
      lineNo += 10;
      lastLineNo.put(account, lineNo);

      return lineNo;
    }
  }
}
