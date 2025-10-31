package org.openbravo.advpaymentmngt.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.financialmgmt.gl.GLItem;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatement;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Test class for the MatchTransactionDao class.
 */
@RunWith(MockitoJUnitRunner.class)
public class MatchTransactionDaoTest {

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private AutoCloseable mocks;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBQuery<FIN_BankStatementLine> mockBankStatementLineQuery;

  @Mock
  private OBQuery<FIN_FinaccTransaction> mockFinaccTransactionQuery;

  @Mock
  private OBQuery<FIN_Reconciliation> mockReconciliationQuery;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private FIN_Reconciliation mockReconciliation;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    mockedOBContext = mockStatic(OBContext.class);

    // Setup common mock behaviors
    when(mockOBDal.get(FIN_FinancialAccount.class, TestConstants.FINANCIAL_ACCOUNT_ID)).thenReturn(
        mockFinancialAccount);
    when(mockOBDal.get(FIN_Reconciliation.class, TestConstants.RECONCILIATION_ID)).thenReturn(mockReconciliation);

    when(mockFinancialAccount.getId()).thenReturn(TestConstants.FINANCIAL_ACCOUNT_ID);
    when(mockFinancialAccount.getInitialBalance()).thenReturn(TestConstants.INITIAL_BALANCE);
    when(mockReconciliation.getAccount()).thenReturn(mockFinancialAccount);

    // Setup query mocks
    when(mockOBDal.createQuery(eq(FIN_BankStatementLine.class), anyString())).thenReturn(mockBankStatementLineQuery);
    when(mockOBDal.createQuery(eq(FIN_FinaccTransaction.class), anyString())).thenReturn(mockFinaccTransactionQuery);
    when(mockOBDal.createQuery(eq(FIN_Reconciliation.class), anyString())).thenReturn(mockReconciliationQuery);

    when(mockBankStatementLineQuery.setNamedParameter(anyString(), any())).thenReturn(mockBankStatementLineQuery);
    when(mockFinaccTransactionQuery.setNamedParameters(any())).thenReturn(mockFinaccTransactionQuery);

    when(mockReconciliationQuery.setNamedParameter(anyString(), any())).thenReturn(mockReconciliationQuery);
    when(mockReconciliationQuery.setNamedParameters(any())).thenReturn(mockReconciliationQuery);

  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception if an error occurs during tear down
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the getObject method.
   * Verifies that the correct object is returned.
   */
  @Test
  public void testGetObject() {
    // Given
    String testId = "TEST_ID";
    FIN_FinancialAccount expectedAccount = mock(FIN_FinancialAccount.class);
    when(mockOBDal.get(FIN_FinancialAccount.class, testId)).thenReturn(expectedAccount);

    // When
    FIN_FinancialAccount result = MatchTransactionDao.getObject(FIN_FinancialAccount.class, testId);

    // Then
    assertEquals("Should return the expected object", expectedAccount, result);
    verify(mockOBDal).get(FIN_FinancialAccount.class, testId);
  }

  /**
   * Tests the getClearedLinesAmount method with empty lines.
   * Verifies that the amount returned is zero.
   */
  @Test
  public void testGetClearedLinesAmountWithEmptyLines() {
    // Given
    when(mockFinaccTransactionQuery.list()).thenReturn(Collections.emptyList());

    // When
    BigDecimal result = MatchTransactionDao.getClearedLinesAmount(TestConstants.RECONCILIATION_ID);

    // Then
    assertEquals("Should return zero for empty lines", BigDecimal.ZERO.setScale(0), result.setScale(0));

    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getClearedLinesAmount method with lines.
   * Verifies that the sum of deposit and payment amounts is returned.
   */
  @Test
  public void testGetClearedLinesAmountWithLines() {
    // Given
    FIN_FinaccTransaction transaction1 = mock(FIN_FinaccTransaction.class);
    FIN_FinaccTransaction transaction2 = mock(FIN_FinaccTransaction.class);

    when(transaction1.getDepositAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));
    when(transaction1.getPaymentAmount()).thenReturn(new BigDecimal("0.00"));
    when(transaction2.getDepositAmount()).thenReturn(new BigDecimal("50.00"));
    when(transaction2.getPaymentAmount()).thenReturn(new BigDecimal("20.00"));

    List<FIN_FinaccTransaction> transactions = Arrays.asList(transaction1, transaction2);

    when(mockFinaccTransactionQuery.list()).thenReturn(transactions);
    // When
    BigDecimal result = MatchTransactionDao.getClearedLinesAmount(TestConstants.RECONCILIATION_ID);

    // Then
    assertEquals("Should return sum of deposit - payment amounts", new BigDecimal("130.00"), result);
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the checkAllLinesCleared method when all lines are cleared.
   * Verifies that the method returns true.
   */
  @Test
  public void testCheckAllLinesClearedAllCleared() {
    // Given
    List<FIN_BankStatement> bankStatements = new ArrayList<>();
    when(mockFinancialAccount.getFINBankStatementList()).thenReturn(bankStatements);

    // REEMPLAZADO: Se mockea el OBQuery
    when(mockBankStatementLineQuery.setMaxResult(1)).thenReturn(mockBankStatementLineQuery);
    when(mockBankStatementLineQuery.list()).thenReturn(Collections.emptyList());

    // When
    boolean result = MatchTransactionDao.checkAllLinesCleared(TestConstants.FINANCIAL_ACCOUNT_ID);

    // Then
    assertTrue("Should return true when all lines are cleared", result);
    verify(mockBankStatementLineQuery).setMaxResult(1);
    verify(mockBankStatementLineQuery).list();
  }

  /**
   * Tests the checkAllLinesCleared method when not all lines are cleared.
   * Verifies that the method returns false.
   */
  @Test
  public void testCheckAllLinesClearedNotAllCleared() {
    // Given
    List<FIN_BankStatement> bankStatements = new ArrayList<>();
    FIN_BankStatementLine unmatched = mock(FIN_BankStatementLine.class);
    List<FIN_BankStatementLine> unmatchedLines = Collections.singletonList(unmatched);

    when(mockFinancialAccount.getFINBankStatementList()).thenReturn(bankStatements);

    // REEMPLAZADO: Se mockea el OBQuery
    when(mockBankStatementLineQuery.setMaxResult(1)).thenReturn(mockBankStatementLineQuery);
    when(mockBankStatementLineQuery.list()).thenReturn(unmatchedLines);

    // When
    boolean result = MatchTransactionDao.checkAllLinesCleared(TestConstants.FINANCIAL_ACCOUNT_ID);

    // Then
    assertFalse("Should return false when not all lines are cleared", result);
    verify(mockBankStatementLineQuery).setMaxResult(1);
    verify(mockBankStatementLineQuery).list();
  }

  /**
   * Tests the isLastReconciliation method when no later reconciliations exist.
   * Verifies that the method returns true.
   */
  @Test
  public void testIsLastReconciliationTrue() {
    // Given
    when(mockReconciliationQuery.list()).thenReturn(Collections.emptyList());

    // When
    boolean result = MatchTransactionDao.islastreconciliation(mockReconciliation);

    // Then
    assertTrue("Should return true when no later reconciliations exist", result);
  }

  /**
   * Tests the isLastReconciliation method when later reconciliations exist.
   * Verifies that the method returns false.
   */
  @Test
  public void testIsLastReconciliationFalse() {
    // Given
    FIN_Reconciliation laterReconciliation = mock(FIN_Reconciliation.class);
    List<FIN_Reconciliation> laterReconciliations = Collections.singletonList(laterReconciliation);
    when(mockReconciliationQuery.list()).thenReturn(laterReconciliations);

    // When
    boolean result = MatchTransactionDao.islastreconciliation(mockReconciliation);

    // Then
    assertFalse("Should return false when later reconciliations exist", result);
  }

  /**
   * Tests the getUnMatchedBankStatementLines method.
   * Verifies that the list of unmatched bank statement lines is returned.
   */
  @Test
  public void testGetUnMatchedBankStatementLines() {
    // Given
    FIN_BankStatementLine unmatchedLine = mock(FIN_BankStatementLine.class);
    List<FIN_BankStatementLine> unmatchedLines = Collections.singletonList(unmatchedLine);

    when(mockBankStatementLineQuery.list()).thenReturn(unmatchedLines);

    // When
    List<FIN_BankStatementLine> result = MatchTransactionDao.getUnMatchedBankStatementLines(mockFinancialAccount);

    // Then
    assertEquals("Should return the list of unmatched bank statement lines", unmatchedLines, result);
    verify(mockBankStatementLineQuery).setNamedParameter("accountId", TestConstants.FINANCIAL_ACCOUNT_ID);
    verify(mockBankStatementLineQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with reference and business partner.
   * Verifies that the list of matching transactions is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithReferenceAndBPartner() {
    // Given
    String reference = TestConstants.REFERENCE;
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    String bpartnerName = "Test Partner";
    Date transactionDate = new Date();
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, transactionDate, reference, amount, bpartnerName, excluded);

    // Then
    assertEquals("Should return the list of matching transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingGLItemTransaction method.
   * Verifies that the list of matching GL item transactions is returned.
   */
  @Test
  public void testGetMatchingGLItemTransaction() {
    // Given
    GLItem glItem = mock(GLItem.class);
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    Date transactionDate = new Date();
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingGLItemTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, glItem, transactionDate, amount, excluded);

    // Then
    assertEquals("Should return the list of matching GL item transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getBankStatementLineMaxDate method with results.
   * Verifies that the maximum transaction date is returned.
   */
  @Test
  public void testGetBankStatementLineMaxDateWithResults() {
    // Given
    Date maxDate = new Date();
    FIN_BankStatementLine latestLine = mock(FIN_BankStatementLine.class);
    List<FIN_BankStatementLine> lines = Collections.singletonList(latestLine);

    when(mockBankStatementLineQuery.setWhereAndOrderBy(anyString())).thenReturn(
        mockBankStatementLineQuery);
    when(mockBankStatementLineQuery.setMaxResult(1)).thenReturn(mockBankStatementLineQuery);
    when(mockBankStatementLineQuery.list()).thenReturn(lines);
    when(latestLine.getTransactionDate()).thenReturn(maxDate);
    when(latestLine.getTransactionDate()).thenReturn(maxDate);

    // When
    Date result = MatchTransactionDao.getBankStatementLineMaxDate(mockFinancialAccount);

    // Then
    assertEquals("Should return the max transaction date", maxDate, result);
    verify(mockBankStatementLineQuery).setMaxResult(1);
    verify(mockBankStatementLineQuery).list();
  }

  /**
   * Tests the getReconciliationLastAmount method with results.
   * Verifies that the last reconciliation amount is returned.
   */
  @Test
  public void testGetReconciliationLastAmountWithResults() {
    // Given
    BigDecimal lastAmount = new BigDecimal("2000.00");
    FIN_Reconciliation lastReconciliation = mock(FIN_Reconciliation.class);
    List<FIN_Reconciliation> reconciliations = Collections.singletonList(lastReconciliation);

    when(mockReconciliationQuery.setWhereAndOrderBy(anyString())).thenReturn(
        mockReconciliationQuery);
    when(mockReconciliationQuery.setMaxResult(1)).thenReturn(mockReconciliationQuery);
    when(mockReconciliationQuery.list()).thenReturn(reconciliations);
    when(lastReconciliation.getEndingBalance()).thenReturn(lastAmount);

    // When
    BigDecimal result = MatchTransactionDao.getReconciliationLastAmount(mockFinancialAccount);

    // Then
    assertEquals("Should return the last reconciliation amount", lastAmount, result);
    // REEMPLAZADO: Verificando el mock de Query
    verify(mockReconciliationQuery).setMaxResult(1);
    verify(mockReconciliationQuery).list();
  }

  /**
   * Tests the getStartingBalance method with a previous reconciliation.
   * Verifies that the previous reconciliation ending balance is returned.
   */
  @Test
  public void testGetStartingBalanceWithPreviousReconciliation() {
    // Given
    BigDecimal previousEndingBalance = new BigDecimal("1500.00");
    FIN_Reconciliation previousReconciliation = mock(FIN_Reconciliation.class);

    when(mockReconciliationQuery.setWhereAndOrderBy(anyString())).thenReturn(
        mockReconciliationQuery);
    when(mockReconciliationQuery.setMaxResult(1)).thenReturn(mockReconciliationQuery);
    when(mockReconciliationQuery.uniqueResult()).thenReturn(previousReconciliation);
    when(previousReconciliation.getEndingBalance()).thenReturn(previousEndingBalance);

    // When
    BigDecimal result = MatchTransactionDao.getStartingBalance(mockReconciliation);

    // Then
    assertEquals("Should return the previous reconciliation ending balance", previousEndingBalance,
        result);
  }

  /**
   * Tests the getStartingBalance method without a previous reconciliation.
   * Verifies that the financial account initial balance is returned.
   */
  @Test
  public void testGetStartingBalanceWithoutPreviousReconciliation() {
    // Given
    when(mockReconciliationQuery.setWhereAndOrderBy(anyString())).thenReturn(
        mockReconciliationQuery);
    when(mockReconciliationQuery.setMaxResult(1)).thenReturn(mockReconciliationQuery);
    when(mockReconciliationQuery.uniqueResult()).thenReturn(null);

    // When
    BigDecimal result = MatchTransactionDao.getStartingBalance(mockReconciliation);

    // Then
    assertEquals("Should return the financial account initial balance",
        TestConstants.INITIAL_BALANCE, result);
  }

  /**
   * Tests the getMatchingFinancialTransaction method with date and reference.
   * Verifies that the list of matching transactions is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithDateAndReference() {
    // Given
    Date transactionDate = new Date();
    String reference = TestConstants.REFERENCE;
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, transactionDate, reference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with an empty reference.
   * Verifies that the list of matching transactions with an empty reference is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithEmptyReference() {
    // Given
    Date transactionDate = new Date();
    String emptyReference = "";
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, transactionDate, emptyReference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions with empty reference", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with a wildcard reference.
   * Verifies that the list of matching transactions with a wildcard reference is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithWildcardReference() {
    // Given
    Date transactionDate = new Date();
    String wildcardReference = "**";
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, transactionDate, wildcardReference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions with wildcard reference", matchingTransactions,
        result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with reference and business partner but no date.
   * Verifies that the method delegates to the method with a null date parameter.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithReferenceAndBPartnerNoDate() {
    // Given
    String reference = TestConstants.REFERENCE;
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    String bpartnerName = "Test Partner";
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, reference, amount, bpartnerName, excluded);

    // Then
    assertEquals("Should delegate to method with date parameter as null", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with only date and amount.
   * Verifies that the list of matching transactions is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithDateAndAmountOnly() {
    // Given
    Date transactionDate = new Date();
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, transactionDate, amount, excluded);

    // Then
    assertEquals("Should return matching transactions with only date and amount", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  /**
   * Tests the getMatchingFinancialTransaction method with a null date.
   * Verifies that the list of matching transactions with a null date is returned.
   */
  @Test
  public void testGetMatchingFinancialTransactionWithNullDate() {
    // Given
    Date nullDate = null;
    BigDecimal amount = new BigDecimal(TestConstants.AMOUNT);
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        TestConstants.FINANCIAL_ACCOUNT_ID, nullDate, amount, excluded);

    // Then
    assertEquals("Should return matching transactions with null date", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

}
