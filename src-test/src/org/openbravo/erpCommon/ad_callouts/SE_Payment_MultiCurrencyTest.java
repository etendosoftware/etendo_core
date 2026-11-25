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
package org.openbravo.erpCommon.ad_callouts; //NOSONAR

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Tests for {@link SE_Payment_MultiCurrency} callout.
 */
@SuppressWarnings("java:S101")
@RunWith(MockitoJUnitRunner.class)
public class SE_Payment_MultiCurrencyTest {

  private static final String FINACC_TXN_AMOUNT = "inpfinaccTxnAmount";
  private static final String FINACC_TXN_CONVERT_RATE = "inpfinaccTxnConvertRate";
  private static final String PAYMENT_DATE = "inppaymentdate";
  private static final String CURRENCY_ID = "inpcCurrencyId";
  private static final String AMOUNT = "inpamount";
  private static final String GENERATED_CREDIT = "inpgeneratedCredit";
  private static final String ORG_ID = "inpadOrgId";
  private static final String FINANCIAL_ACCOUNT_ID = "inpfinFinancialAccountId";
  private static final String EUR_ID = "EUR_ID";
  private static final String ORG_VALUE = "ORG_ID";
  private static final String FIN_ACCOUNT_VALUE = "FIN_ACCOUNT_ID";
  private static final String PAYMENT_AMOUNT = "154.10";

  private final Map<String, Object> results = new HashMap<>();
  private SE_Payment_MultiCurrency callout;
  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;

  @Mock
  private SimpleCallout.CalloutInfo info;
  @Mock
  private VariablesSecureApp vars;
  @Mock
  private OBDal obDal;
  @Mock
  private FIN_FinancialAccount financialAccount;
  @Mock
  private Currency paymentCurrency;
  @Mock
  private Currency otherCurrency;
  @Mock
  private Organization organization;
  @Mock
  private OBCriteria<ConversionRate> conversionRateCriteria;

  /**
   * Initializes the callout and captures every result written through any addResult overload.
   */
  @Before
  public void setUp() {
    callout = new SE_Payment_MultiCurrency();
    info.vars = vars;
    results.clear();
    lenient().doAnswer(invocation -> {
      results.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(info).addResult(anyString(), nullable(Object.class));
    lenient().doAnswer(invocation -> {
      results.put(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(info).addResult(anyString(), nullable(String.class));

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    obContextStatic = mockStatic(OBContext.class);
  }

  /** Closes the static mocks. */
  @After
  public void tearDown() {
    obDalStatic.close();
    obContextStatic.close();
  }

  /**
   * Changing the payment date with the same currency as the financial account must return the
   * transaction amount as a number, so the client does not parse it again with the user's number
   * format (e.g. 154.10 shown as 15.410,00 with European format).
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testPaymentDateChangeSameCurrencyReturnsAmountAsBigDecimal() throws ServletException {
    stubPayment(PAYMENT_DATE, PAYMENT_AMOUNT, paymentCurrency);

    callout.execute(info);

    assertBigDecimalResult(new BigDecimal(PAYMENT_AMOUNT), FINACC_TXN_AMOUNT);
    assertBigDecimalResult(BigDecimal.ONE, FINACC_TXN_CONVERT_RATE);
  }

  /**
   * Changing the currency with the same currency as the financial account must return the
   * transaction amount as a number.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testCurrencyChangeSameCurrencyReturnsAmountAsBigDecimal() throws ServletException {
    stubPayment(CURRENCY_ID, PAYMENT_AMOUNT, paymentCurrency);

    callout.execute(info);

    assertBigDecimalResult(new BigDecimal(PAYMENT_AMOUNT), FINACC_TXN_AMOUNT);
  }

  /**
   * Changing the payment date with a currency different from the financial account and no
   * conversion rate available must return the transaction amount as a number.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testPaymentDateChangeWithoutConversionRateReturnsAmountAsBigDecimal()
      throws ServletException {
    stubPayment(PAYMENT_DATE, PAYMENT_AMOUNT, otherCurrency);
    when(obDal.get(Organization.class, ORG_VALUE)).thenReturn(organization);
    when(obDal.createCriteria(ConversionRate.class)).thenReturn(conversionRateCriteria);
    when(conversionRateCriteria.list()).thenReturn(Collections.emptyList());
    when(organization.getId()).thenReturn("0");

    callout.execute(info);

    assertBigDecimalResult(new BigDecimal(PAYMENT_AMOUNT), FINACC_TXN_AMOUNT);
    assertBigDecimalResult(BigDecimal.ONE, FINACC_TXN_CONVERT_RATE);
  }

  /**
   * Changing the payment date with an empty amount must not set the transaction amount.
   *
   * @throws ServletException
   *     if the callout execution fails
   */
  @Test
  public void testPaymentDateChangeWithEmptyAmountDoesNotSetTransactionAmount()
      throws ServletException {
    stubPayment(PAYMENT_DATE, "", paymentCurrency);

    callout.execute(info);

    assertFalse(results.containsKey(FINACC_TXN_AMOUNT));
    assertBigDecimalResult(BigDecimal.ONE, FINACC_TXN_CONVERT_RATE);
  }

  private void stubPayment(String lastFieldChanged, String amount,
      Currency financialAccountCurrency) throws ServletException {
    when(info.getLastFieldChanged()).thenReturn(lastFieldChanged);
    when(vars.getNumericParameter(FINACC_TXN_AMOUNT)).thenReturn("");
    when(vars.getNumericParameter(FINACC_TXN_CONVERT_RATE)).thenReturn("");
    when(vars.getStringParameter(PAYMENT_DATE)).thenReturn("");
    when(vars.getNumericParameter(GENERATED_CREDIT)).thenReturn("");
    when(vars.getNumericParameter(AMOUNT)).thenReturn(amount);
    when(vars.getStringParameter(CURRENCY_ID)).thenReturn(EUR_ID);
    when(vars.getStringParameter(ORG_ID)).thenReturn(ORG_VALUE);
    when(vars.getStringParameter(FINANCIAL_ACCOUNT_ID)).thenReturn(FIN_ACCOUNT_VALUE);
    when(obDal.get(FIN_FinancialAccount.class, FIN_ACCOUNT_VALUE)).thenReturn(financialAccount);
    when(obDal.get(Currency.class, EUR_ID)).thenReturn(paymentCurrency);
    when(financialAccount.getCurrency()).thenReturn(financialAccountCurrency);
  }

  private void assertBigDecimalResult(BigDecimal expected, String param) {
    Object value = results.get(param);
    assertTrue(param + " must be returned as BigDecimal but was "
        + (value == null ? "null" : value.getClass().getSimpleName()),
        value instanceof BigDecimal);
    assertEquals(0, expected.compareTo((BigDecimal) value));
  }
}
