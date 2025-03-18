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

import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
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

@RunWith(MockitoJUnitRunner.class)
public class MatchTransactionDaoTest {

  private static final String FINANCIAL_ACCOUNT_ID = "TEST_FINANCIAL_ACCOUNT_ID";
  private static final String RECONCILIATION_ID = "TEST_RECONCILIATION_ID";
  private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
  
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private AutoCloseable mocks;
  
  @Mock
  private OBDal mockOBDal;
  
  @Mock
  private OBCriteria<FIN_FinaccTransaction> mockFinaccTransactionCriteria;
  
  @Mock
  private OBCriteria<FIN_BankStatementLine> mockBankStatementLineCriteria;
  
  @Mock
  private OBCriteria<FIN_Reconciliation> mockReconciliationCriteria;
  
  @Mock
  private OBQuery<FIN_BankStatementLine> mockBankStatementLineQuery;
  
  @Mock
  private OBQuery<FIN_FinaccTransaction> mockFinaccTransactionQuery;
  
  @Mock
  private FIN_FinancialAccount mockFinancialAccount;
  
  @Mock
  private FIN_Reconciliation mockReconciliation;


  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    
    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    
    mockedOBContext = mockStatic(OBContext.class);
    
    // Setup common mock behaviors
    when(mockOBDal.get(FIN_FinancialAccount.class, FINANCIAL_ACCOUNT_ID)).thenReturn(mockFinancialAccount);
    when(mockOBDal.get(FIN_Reconciliation.class, RECONCILIATION_ID)).thenReturn(mockReconciliation);
    when(mockFinancialAccount.getId()).thenReturn(FINANCIAL_ACCOUNT_ID);
    when(mockFinancialAccount.getInitialBalance()).thenReturn(INITIAL_BALANCE);
    when(mockReconciliation.getAccount()).thenReturn(mockFinancialAccount);
    
    // Setup criteria mocks
    when(mockOBDal.createCriteria(FIN_FinaccTransaction.class)).thenReturn(mockFinaccTransactionCriteria);
    when(mockOBDal.createCriteria(FIN_BankStatementLine.class)).thenReturn(mockBankStatementLineCriteria);
    when(mockOBDal.createCriteria(FIN_Reconciliation.class)).thenReturn(mockReconciliationCriteria);
    
    // Setup query mocks
    when(mockOBDal.createQuery(eq(FIN_BankStatementLine.class), anyString())).thenReturn(mockBankStatementLineQuery);
    when(mockOBDal.createQuery(eq(FIN_FinaccTransaction.class), anyString())).thenReturn(mockFinaccTransactionQuery);
    when(mockBankStatementLineQuery.setNamedParameter(anyString(), any())).thenReturn(mockBankStatementLineQuery);
    when(mockFinaccTransactionQuery.setNamedParameters(any())).thenReturn(mockFinaccTransactionQuery);

  }

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

  @Test
  public void testGetClearedLinesAmountWithEmptyLines() {
    // Given
    when(mockFinaccTransactionCriteria.add(any(Criterion.class))).thenReturn(mockFinaccTransactionCriteria);
    when(mockFinaccTransactionCriteria.list()).thenReturn(Collections.emptyList());
    
    // When
    BigDecimal result = MatchTransactionDao.getClearedLinesAmount(RECONCILIATION_ID);
    
    // Then
    assertEquals("Should return zero for empty lines", BigDecimal.ZERO.setScale(0), result.setScale(0));
    verify(mockFinaccTransactionCriteria, times(2)).add(any(Criterion.class));
    verify(mockFinaccTransactionCriteria).list();
  }

  @Test
  public void testGetClearedLinesAmountWithLines() {
    // Given
    FIN_FinaccTransaction transaction1 = mock(FIN_FinaccTransaction.class);
    FIN_FinaccTransaction transaction2 = mock(FIN_FinaccTransaction.class);
    
    when(transaction1.getDepositAmount()).thenReturn(new BigDecimal("100.00"));
    when(transaction1.getPaymentAmount()).thenReturn(new BigDecimal("0.00"));
    when(transaction2.getDepositAmount()).thenReturn(new BigDecimal("50.00"));
    when(transaction2.getPaymentAmount()).thenReturn(new BigDecimal("20.00"));
    
    List<FIN_FinaccTransaction> transactions = Arrays.asList(transaction1, transaction2);
    
    when(mockFinaccTransactionCriteria.add(any(Criterion.class))).thenReturn(mockFinaccTransactionCriteria);
    when(mockFinaccTransactionCriteria.list()).thenReturn(transactions);
    
    // When
    BigDecimal result = MatchTransactionDao.getClearedLinesAmount(RECONCILIATION_ID);
    
    // Then
    assertEquals("Should return sum of deposit - payment amounts", new BigDecimal("130.00"), result);
    verify(mockFinaccTransactionCriteria, times(2)).add(any(Criterion.class));
    verify(mockFinaccTransactionCriteria).list();
  }

  @Test
  public void testCheckAllLinesClearedAllCleared() {
    // Given
    List<FIN_BankStatement> bankStatements = new ArrayList<>();
    when(mockFinancialAccount.getFINBankStatementList()).thenReturn(bankStatements);
    when(mockBankStatementLineCriteria.add(any(Criterion.class))).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.setMaxResults(1)).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.list()).thenReturn(Collections.emptyList());
    
    // When
    boolean result = MatchTransactionDao.checkAllLinesCleared(FINANCIAL_ACCOUNT_ID);
    
    // Then
    assertTrue("Should return true when all lines are cleared", result);
    verify(mockBankStatementLineCriteria).setMaxResults(1);
    verify(mockBankStatementLineCriteria).list();
  }

  @Test
  public void testCheckAllLinesClearedNotAllCleared() {
    // Given
    List<FIN_BankStatement> bankStatements = new ArrayList<>();
    FIN_BankStatementLine unmatched = mock(FIN_BankStatementLine.class);
    List<FIN_BankStatementLine> unmatchedLines = Collections.singletonList(unmatched);
    
    when(mockFinancialAccount.getFINBankStatementList()).thenReturn(bankStatements);
    when(mockBankStatementLineCriteria.add(any(Criterion.class))).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.setMaxResults(1)).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.list()).thenReturn(unmatchedLines);
    
    // When
    boolean result = MatchTransactionDao.checkAllLinesCleared(FINANCIAL_ACCOUNT_ID);
    
    // Then
    assertFalse("Should return false when not all lines are cleared", result);
    verify(mockBankStatementLineCriteria).setMaxResults(1);
    verify(mockBankStatementLineCriteria).list();
  }

  @Test
  public void testIsLastReconciliationTrue() {
    // Given
    when(mockReconciliationCriteria.add(any(Criterion.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.list()).thenReturn(Collections.emptyList());
    
    // When
    boolean result = MatchTransactionDao.islastreconciliation(mockReconciliation);
    
    // Then
    assertTrue("Should return true when no later reconciliations exist", result);
  }

  @Test
  public void testIsLastReconciliationFalse() {
    // Given
    FIN_Reconciliation laterReconciliation = mock(FIN_Reconciliation.class);
    List<FIN_Reconciliation> laterReconciliations = Collections.singletonList(laterReconciliation);
    
    when(mockReconciliationCriteria.add(any(Criterion.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.list()).thenReturn(laterReconciliations);
    
    // When
    boolean result = MatchTransactionDao.islastreconciliation(mockReconciliation);
    
    // Then
    assertFalse("Should return false when later reconciliations exist", result);
  }

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
    verify(mockBankStatementLineQuery).setNamedParameter("accountId", FINANCIAL_ACCOUNT_ID);
    verify(mockBankStatementLineQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransaction_WithReferenceAndBPartner() {
    // Given
    String reference = "REF123";
    BigDecimal amount = new BigDecimal("100.00");
    String bpartnerName = "Test Partner";
    Date transactionDate = new Date();
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);
    
    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);
    
    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, transactionDate, reference, amount, bpartnerName, excluded);
    
    // Then
    assertEquals("Should return the list of matching transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingGLItemTransaction() {
    // Given
    GLItem glItem = mock(GLItem.class);
    BigDecimal amount = new BigDecimal("100.00");
    Date transactionDate = new Date();
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);
    
    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);
    
    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingGLItemTransaction(
        FINANCIAL_ACCOUNT_ID, glItem, transactionDate, amount, excluded);
    
    // Then
    assertEquals("Should return the list of matching GL item transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetBankStatementLineMaxDateWithResults() {
    // Given
    Date maxDate = new Date();
    FIN_BankStatementLine latestLine = mock(FIN_BankStatementLine.class);
    List<FIN_BankStatementLine> lines = Collections.singletonList(latestLine);
    
    when(mockBankStatementLineCriteria.createAlias(anyString(), anyString())).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.add(any(Criterion.class))).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.setMaxResults(1)).thenReturn(mockBankStatementLineCriteria);
    when(mockBankStatementLineCriteria.list()).thenReturn(lines);
    when(latestLine.getTransactionDate()).thenReturn(maxDate);
    
    // When
    Date result = MatchTransactionDao.getBankStatementLineMaxDate(mockFinancialAccount);
    
    // Then
    assertEquals("Should return the max transaction date", maxDate, result);
    verify(mockBankStatementLineCriteria).setMaxResults(1);
    verify(mockBankStatementLineCriteria).list();
  }

  @Test
  public void testGetReconciliationLastAmountWithResults() {
    // Given
    BigDecimal lastAmount = new BigDecimal("2000.00");
    FIN_Reconciliation lastReconciliation = mock(FIN_Reconciliation.class);
    List<FIN_Reconciliation> reconciliations = Collections.singletonList(lastReconciliation);
    
    when(mockReconciliationCriteria.add(any(Criterion.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.addOrderBy(anyString(), anyBoolean())).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.setMaxResults(1)).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.list()).thenReturn(reconciliations);
    when(lastReconciliation.getEndingBalance()).thenReturn(lastAmount);
    
    // When
    BigDecimal result = MatchTransactionDao.getReconciliationLastAmount(mockFinancialAccount);
    
    // Then
    assertEquals("Should return the last reconciliation amount", lastAmount, result);
    verify(mockReconciliationCriteria).setMaxResults(1);
    verify(mockReconciliationCriteria).list();
  }


  @Test
  public void testGetStartingBalanceWithPreviousReconciliation() {
    // Given
    BigDecimal previousEndingBalance = new BigDecimal("1500.00");
    FIN_Reconciliation previousReconciliation = mock(FIN_Reconciliation.class);
    
    when(mockReconciliationCriteria.add(any(Criterion.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.addOrder(any(Order.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.setMaxResults(1)).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.uniqueResult()).thenReturn(previousReconciliation);
    when(previousReconciliation.getEndingBalance()).thenReturn(previousEndingBalance);
    
    // When
    BigDecimal result = MatchTransactionDao.getStartingBalance(mockReconciliation);
    
    // Then
    assertEquals("Should return the previous reconciliation ending balance", previousEndingBalance, result);
  }

  @Test
  public void testGetStartingBalanceWithoutPreviousReconciliation() {
    // Given
    when(mockReconciliationCriteria.add(any(Criterion.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.addOrder(any(Order.class))).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.setMaxResults(1)).thenReturn(mockReconciliationCriteria);
    when(mockReconciliationCriteria.uniqueResult()).thenReturn(null);
    
    // When
    BigDecimal result = MatchTransactionDao.getStartingBalance(mockReconciliation);
    
    // Then
    assertEquals("Should return the financial account initial balance", INITIAL_BALANCE, result);
  }

  //nuevas
  @Test
  public void testGetMatchingFinancialTransactionWithDateAndReference() {
    // Given
    Date transactionDate = new Date();
    String reference = "REF123";
    BigDecimal amount = new BigDecimal("100.00");
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, transactionDate, reference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransactionWithEmptyReference() {
    // Given
    Date transactionDate = new Date();
    String emptyReference = "";
    BigDecimal amount = new BigDecimal("100.00");
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, transactionDate, emptyReference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions with empty reference", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransactionWithWildcardReference() {
    // Given
    Date transactionDate = new Date();
    String wildcardReference = "**";
    BigDecimal amount = new BigDecimal("100.00");
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, transactionDate, wildcardReference, amount, excluded);

    // Then
    assertEquals("Should return the list of matching transactions with wildcard reference", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransactionWithReferenceAndBPartnerNoDate() {
    // Given
    String reference = "REF123";
    BigDecimal amount = new BigDecimal("100.00");
    String bpartnerName = "Test Partner";
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, reference, amount, bpartnerName, excluded);

    // Then
    assertEquals("Should delegate to method with date parameter as null", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransactionWithDateAndAmountOnly() {
    // Given
    Date transactionDate = new Date();
    BigDecimal amount = new BigDecimal("100.00");
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, transactionDate, amount, excluded);

    // Then
    assertEquals("Should return matching transactions with only date and amount", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

  @Test
  public void testGetMatchingFinancialTransactionWithNullDate() {
    // Given
    Date nullDate = null;
    BigDecimal amount = new BigDecimal("100.00");
    List<FIN_FinaccTransaction> excluded = new ArrayList<>();
    FIN_FinaccTransaction matchingTransaction = mock(FIN_FinaccTransaction.class);
    List<FIN_FinaccTransaction> matchingTransactions = Collections.singletonList(matchingTransaction);

    when(mockFinaccTransactionQuery.list()).thenReturn(matchingTransactions);

    // When
    List<FIN_FinaccTransaction> result = MatchTransactionDao.getMatchingFinancialTransaction(
        FINANCIAL_ACCOUNT_ID, nullDate, amount, excluded);

    // Then
    assertEquals("Should return matching transactions with null date", matchingTransactions, result);
    verify(mockFinaccTransactionQuery).setNamedParameters(any());
    verify(mockFinaccTransactionQuery).list();
  }

}
