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
package org.openbravo.advpaymentmngt.test;

import static org.hamcrest.Matchers.closeTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.jboss.arquillian.junit.InSequence;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.advpaymentmngt.actionHandler.FundsTransferActionHandler;
import org.openbravo.advpaymentmngt.process.FIN_TransactionProcess;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

public class FundsTransferTest extends WeldBaseTest {
  final private static String BP_DEPOSIT = "BPD";
  final private static String BP_WITHDRAWAL = "BPW";
  final private static String BANK_FEE = "BF";
  private static final String REACTIVATE = "R";
  // User Openbravo
  private final String USER_ID = "100";
  // Client F&B International Group
  private final String CLIENT_ID = "23C59575B9CF467C9620760EB255B389";
  // Organization F&B España, S.A
  private final String ORGANIZATION_ID = "B843C30461EA4501935CB1D125C9C25A";
  // Role F&B International Group Admin
  private final String ROLE_ID = "42D0EEB1C66F497A90DD526DC597E6F0";

  // Today
  private final Date TODAY = new Date();

  private final BigDecimal TRANSACTION_AMT = new BigDecimal(100);
  private final BigDecimal FEE_AMT_FROM = BigDecimal.TEN;
  private final BigDecimal FEE_AMT_TO = new BigDecimal(9);
  private final BigDecimal CONVERSION_RATE = new BigDecimal(0.77);

  private FIN_FinancialAccount caja_clone;
  private FIN_FinancialAccount banco_clone;
  private FIN_FinancialAccount banco_gbp;
  private GLItem glitem;
  private ConversionRate conversion_rate;

  @Before
  public void setUpEnvironment() throws Exception {
    OBContext.setOBContext(USER_ID, ROLE_ID, CLIENT_ID, ORGANIZATION_ID);

    // Caja Account
    FIN_FinancialAccount caja = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, "C2AA9C0AFB434FD4B827BE58DC52C1E2");
    caja_clone = (FIN_FinancialAccount) DalUtil.copy(caja, false);
    caja_clone.setName(caja_clone.getName() + " - clone");
    OBDal.getInstance().save(caja_clone);

    // Banco Account
    FIN_FinancialAccount banco = OBDal.getInstance()
        .get(FIN_FinancialAccount.class, "DEDDE613C5314ACD8DCC60C474D1A107");
    banco_clone = (FIN_FinancialAccount) DalUtil.copy(banco, false);
    banco_clone.setName(banco_clone.getName() + " - clone");
    OBDal.getInstance().save(banco_clone);

    // Banco UK
    banco_gbp = (FIN_FinancialAccount) DalUtil.copy(banco, false);
    banco_gbp.setName(banco_gbp.getName() + " - clone UK");
    Currency gbp_currency = OBDal.getInstance().get(Currency.class, "114");
    banco_gbp.setCurrency(gbp_currency);
    OBDal.getInstance().save(banco_gbp);

    // Random GL Item
    glitem = OBDal.getInstance().createCriteria(GLItem.class).list().get(0);

    // Hack to be able to delete all Accounts after testFundsTransferConversionRateError
    OBDal.getInstance().flush();
  }

  private void createConversionRate(Currency from, Currency to) {
    Organization starOrg = OBDal.getInstance().get(Organization.class, "0");
    Date yesterday = DateUtils.addDays(TODAY, -1);

    conversion_rate = OBProvider.getInstance().get(ConversionRate.class);
    conversion_rate.setOrganization(starOrg);

    conversion_rate.setCurrency(from);
    conversion_rate.setToCurrency(to);
    conversion_rate.setMultipleRateBy(CONVERSION_RATE);
    conversion_rate.setValidFromDate(yesterday);
    OBDal.getInstance().save(conversion_rate);
    OBDal.getInstance().flush();
  }

  @Test
  @InSequence(1)
  public void testFundsTransferSameCurrency() {
    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_clone, glitem,
        TRANSACTION_AMT, null, null, null);

    testTransactions(caja_clone, banco_clone, TRANSACTION_AMT, null, TRANSACTION_AMT, null);
  }

  @Test
  @InSequence(2)
  public void testFundsTransferBankFee() {
    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_clone, glitem,
        TRANSACTION_AMT, null, FEE_AMT_FROM, null);

    testTransactions(caja_clone, banco_clone, TRANSACTION_AMT, FEE_AMT_FROM, TRANSACTION_AMT, null);
  }

  @Test(expected = OBException.class)
  @InSequence(4)
  public void testFundsTransferConversionRateError() {
    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_gbp, glitem, TRANSACTION_AMT,
        null, null, null);
  }

  @Test
  @InSequence(5)
  public void testFundsTransferManualConversionRate() {
    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_gbp, glitem, TRANSACTION_AMT,
        CONVERSION_RATE, null, null);

    BigDecimal convertedAmount = TRANSACTION_AMT.multiply(CONVERSION_RATE)
        .setScale(2, RoundingMode.HALF_UP);

    testTransactions(caja_clone, banco_gbp, TRANSACTION_AMT, null, convertedAmount, null);
  }

  @Test
  @InSequence(7)
  public void testFundsTransferSystemConversionRate() {
    createConversionRate(caja_clone.getCurrency(), banco_gbp.getCurrency());

    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_gbp, glitem, TRANSACTION_AMT,
        null, null, null);

    BigDecimal convertedAmount = TRANSACTION_AMT.multiply(CONVERSION_RATE)
        .setScale(2, RoundingMode.HALF_UP);

    testTransactions(caja_clone, banco_gbp, TRANSACTION_AMT, null, convertedAmount, null);
  }

  @Test
  @InSequence(8)
  public void testFundsTransferTargetFee() {
    FundsTransferActionHandler.createTransfer(TODAY, caja_clone, banco_gbp, glitem, TRANSACTION_AMT,
        CONVERSION_RATE, null, FEE_AMT_TO);

    BigDecimal convertedAmount = TRANSACTION_AMT.multiply(CONVERSION_RATE)
        .setScale(2, RoundingMode.HALF_UP);

    testTransactions(caja_clone, banco_gbp, TRANSACTION_AMT, null, convertedAmount, FEE_AMT_TO);
  }

  private void testTransactions(FIN_FinancialAccount origin_acct, FIN_FinancialAccount target_acct,
      BigDecimal debit_amt, BigDecimal origin_fee, BigDecimal deposit_amt, BigDecimal target_fee) {
    boolean debit_amt_checked = false;
    boolean origin_fee_checked = origin_fee == null;
    boolean deposit_amt_checked = false;
    boolean target_fee_checked = target_fee == null;

    OBDal.getInstance().refresh(origin_acct);
    for (FIN_FinaccTransaction trans : origin_acct.getFINFinaccTransactionList()) {
      if (BP_WITHDRAWAL.equalsIgnoreCase(trans.getTransactionType())) {
        assertThat("Wrong debit", trans.getPaymentAmount(), closeTo(debit_amt, BigDecimal.ZERO));
        debit_amt_checked = true;
      } else if (BANK_FEE.equalsIgnoreCase(trans.getTransactionType())) {
        assertThat("Wrong origin fee", trans.getPaymentAmount(),
            closeTo(origin_fee, BigDecimal.ZERO));
        origin_fee_checked = true;
      } else {
        fail();
      }
    }

    OBDal.getInstance().refresh(target_acct);
    for (FIN_FinaccTransaction trans : target_acct.getFINFinaccTransactionList()) {
      if (BP_DEPOSIT.equalsIgnoreCase(trans.getTransactionType())) {
        assertThat("Wrong deposit", trans.getDepositAmount(),
            closeTo(deposit_amt, BigDecimal.ZERO));
        deposit_amt_checked = true;
      } else if (BANK_FEE.equalsIgnoreCase(trans.getTransactionType())) {
        assertThat("Wrong target fee", trans.getPaymentAmount(),
            closeTo(target_fee, BigDecimal.ZERO));
        target_fee_checked = true;
      } else {
        fail();
      }
    }

    // check that everything was checked
    assertTrue("Not all the transactions were found",
        debit_amt_checked && origin_fee_checked && deposit_amt_checked && target_fee_checked);
  }

  @After
  public void cleanEnvironment() {
    deleteAccount(banco_clone);
    deleteAccount(banco_gbp);
    deleteAccount(caja_clone);
    deleteConversionRate(conversion_rate);
  }

  private void deleteAccount(FIN_FinancialAccount account) {
    if (account == null) {
      return;
    }
    OBDal.getInstance().refresh(account);

    reactivateTransactions(account);
    OBDal.getInstance().remove(account);
  }

  private void reactivateTransactions(FIN_FinancialAccount account) {
    for (FIN_FinaccTransaction trans : account.getFINFinaccTransactionList()) {
      FIN_TransactionProcess.doTransactionProcess(REACTIVATE, trans);
    }
  }

  private void deleteConversionRate(ConversionRate conv_rate) {
    if (conv_rate != null) {
      OBDal.getInstance().remove(conv_rate);
    }
  }
}
