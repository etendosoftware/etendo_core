package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.dao.MatchTransactionDao;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext.Usage;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Tests for APRM_MatchingUtility.
 */
@SuppressWarnings({"java:S101", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class APRM_MatchingUtilityTest {

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBContext mockOBContext;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private static final String ACCOUNT_ID = "TEST_ACCOUNT_ID";
  private static final String RECONCILIATION_ID = "TEST_RECONCILIATION_ID";
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }
  /** Fix mixed line receipt sets deposit not cleared. */

  @Test
  public void testFixMixedLineReceiptSetsDepositNotCleared() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    when(mixedLine.getDepositAmount()).thenReturn(new BigDecimal("100.00"));
    when(mixedLine.getFinPayment()).thenReturn(null);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(mixedLine).setReconciliation(null);
    verify(mockOBDal).save(mixedLine);
  }
  /** Fix mixed line withdrawal sets withdrawal not cleared. */

  @Test
  public void testFixMixedLineWithdrawalSetsWithdrawalNotCleared() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    when(mixedLine.getDepositAmount()).thenReturn(BigDecimal.ZERO);
    when(mixedLine.getFinPayment()).thenReturn(null);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    verify(mixedLine).setReconciliation(null);
    verify(mockOBDal).save(mixedLine);
  }
  /** Fix mixed line receipt with payment updates payment status. */

  @Test
  public void testFixMixedLineReceiptWithPaymentUpdatesPaymentStatus() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(mixedLine.getDepositAmount()).thenReturn(new BigDecimal("50.00"));
    when(mixedLine.getFinPayment()).thenReturn(payment);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(payment).setStatus(APRMConstants.PAYMENT_STATUS_DEPOSIT_NOT_CLEARED);
    verify(mockOBDal).save(mixedLine);
    verify(mockOBDal).save(payment);
  }
  /** Fix mixed line withdrawal with payment updates payment status. */

  @Test
  public void testFixMixedLineWithdrawalWithPaymentUpdatesPaymentStatus() {
    // Arrange
    FIN_FinaccTransaction mixedLine = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(mixedLine.getDepositAmount()).thenReturn(BigDecimal.ZERO);
    when(mixedLine.getFinPayment()).thenReturn(payment);

    // Act
    APRM_MatchingUtility.fixMixedLine(mixedLine);

    // Assert
    verify(mixedLine).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
    verify(payment).setStatus(APRMConstants.PAYMENT_STATUS_WITHDRAWAL_NOT_CLEARED);
  }
  /**
   * Get matched document returns transaction when not created by algorithm.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsTransactionWhenNotCreatedByAlgorithm() throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(false);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("T", result);
  }
  /**
   * Get matched document returns transaction when no payment.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsTransactionWhenNoPayment() throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(true);
    when(transaction.getFinPayment()).thenReturn(null);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("T", result);
  }
  /**
   * Get matched document returns payment when payment not created by algorithm.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetMatchedDocumentReturnsPaymentWhenPaymentNotCreatedByAlgorithm()
      throws Exception {
    // Arrange
    FIN_FinaccTransaction transaction = mock(FIN_FinaccTransaction.class);
    FIN_Payment payment = mock(FIN_Payment.class);
    when(transaction.isCreatedByAlgorithm()).thenReturn(true);
    when(transaction.getFinPayment()).thenReturn(payment);
    when(payment.isCreatedByAlgorithm()).thenReturn(false);

    // Act
    String result = invokeGetMatchedDocument(transaction);

    // Assert
    assertEquals("P", result);
  }

  /**
   * Get pending to be matched bank statement lines of the last reconciliation applies the bank
   * statement line filter hooks before the order by clause.
   */
  @Test
  public void testGetPendingToBeMatchedBankStatementLinesWithHook() {
    // Arrange
    FIN_Reconciliation reconciliation = mock(FIN_Reconciliation.class);
    FIN_FinancialAccount account = mock(FIN_FinancialAccount.class);
    when(mockOBDal.get(FIN_Reconciliation.class, RECONCILIATION_ID)).thenReturn(reconciliation);
    when(mockOBDal.get(FIN_FinancialAccount.class, ACCOUNT_ID)).thenReturn(account);
    OBQuery<FIN_BankStatementLine> query = givenBankStatementLineQuery();
    ScrollableResults scroll = mock(ScrollableResults.class);
    when(query.scroll(ScrollMode.FORWARD_ONLY)).thenReturn(scroll);
    BankStatementLineFilterHook hook = (alias, context, namedParameters) -> {
      if (context.getUsage() != Usage.AUTO_MATCH_CANDIDATES || context.getAccount() != account
          || context.getReconciliation() != reconciliation) {
        return null;
      }
      namedParameters.put("test_discarded", Boolean.FALSE);
      return alias + ".discarded = :test_discarded";
    };

    try (MockedStatic<MatchTransactionDao> dao = mockStatic(MatchTransactionDao.class);
        MockedStatic<WeldUtils> weld = mockStatic(WeldUtils.class)) {
      dao.when(() -> MatchTransactionDao.islastreconciliation(reconciliation)).thenReturn(true);
      weld.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
          .thenReturn(Collections.singletonList(hook));

      // Act
      ScrollableResults result = APRM_MatchingUtility
          .getPendingToBeMatchedBankStatementLines(ACCOUNT_ID, RECONCILIATION_ID);

      // Assert
      assertSame(scroll, result);
    }
    ArgumentCaptor<String> hql = ArgumentCaptor.forClass(String.class);
    verify(mockOBDal).createQuery(eq(FIN_BankStatementLine.class), hql.capture());
    assertTrue(hql.getValue()
        .contains(" and (bsl.discarded = :test_discarded) order by bsl.transactionDate,"));
    assertFalse(hql.getValue().contains(":endingdate"));
    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("account", ACCOUNT_ID);
    expectedParameters.put("test_discarded", Boolean.FALSE);
    verify(query).setNamedParameters(expectedParameters);
  }

  /**
   * Get pending to be matched bank statement lines of a reconciliation that is not the last one
   * filters by its ending date, and adds no condition when there are no hooks.
   */
  @Test
  public void testGetPendingToBeMatchedBankStatementLinesNotLastReconciliationWithoutHooks() {
    // Arrange
    FIN_Reconciliation reconciliation = mock(FIN_Reconciliation.class);
    Date endingDate = new Date();
    when(reconciliation.getEndingDate()).thenReturn(endingDate);
    when(mockOBDal.get(FIN_Reconciliation.class, RECONCILIATION_ID)).thenReturn(reconciliation);
    when(mockOBDal.get(FIN_FinancialAccount.class, ACCOUNT_ID))
        .thenReturn(mock(FIN_FinancialAccount.class));
    OBQuery<FIN_BankStatementLine> query = givenBankStatementLineQuery();

    try (MockedStatic<MatchTransactionDao> dao = mockStatic(MatchTransactionDao.class);
        MockedStatic<WeldUtils> weld = mockStatic(WeldUtils.class)) {
      dao.when(() -> MatchTransactionDao.islastreconciliation(reconciliation)).thenReturn(false);
      weld.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
          .thenReturn(Collections.emptyList());

      // Act
      APRM_MatchingUtility.getPendingToBeMatchedBankStatementLines(ACCOUNT_ID, RECONCILIATION_ID);
    }

    // Assert
    verify(mockOBDal).createQuery(FIN_BankStatementLine.class, "as bsl "
        + " where bsl.bankStatement.account.id = :account"
        + "   and bsl.bankStatement.processed = 'Y'"
        + "   and  bsl.transactionDate <= :endingdate"
        + "   and bsl.financialAccountTransaction is null"
        + " order by bsl.transactionDate,"
        + "   bsl.lineNo,"
        + "   bsl.bpartnername");
    Map<String, Object> expectedParameters = new HashMap<>();
    expectedParameters.put("account", ACCOUNT_ID);
    expectedParameters.put("endingdate", endingDate);
    verify(query).setNamedParameters(expectedParameters);
  }

  /**
   * Mocks the query over bank statement lines.
   *
   * @return the mocked query
   */
  @SuppressWarnings("unchecked")
  private OBQuery<FIN_BankStatementLine> givenBankStatementLineQuery() {
    OBQuery<FIN_BankStatementLine> query = mock(OBQuery.class);
    when(mockOBDal.createQuery(eq(FIN_BankStatementLine.class), anyString())).thenReturn(query);
    when(query.setNamedParameters(any())).thenReturn(query);
    return query;
  }

  private String invokeGetMatchedDocument(FIN_FinaccTransaction transaction) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = APRM_MatchingUtility.class.getDeclaredMethod("getMatchedDocument",
        FIN_FinaccTransaction.class);
    method.setAccessible(true);
    return (String) method.invoke(null, transaction);
  }
}
