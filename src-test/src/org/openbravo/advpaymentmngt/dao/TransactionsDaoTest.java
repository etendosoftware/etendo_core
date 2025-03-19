package org.openbravo.advpaymentmngt.dao;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.data.FieldProvider;
import org.openbravo.erpCommon.utility.AccDefUtility;
import org.openbravo.erpCommon.utility.FieldProviderFactory;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Payment;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;
import org.openbravo.model.project.Project;

/**
 * Test class for the TransactionsDao class.
 * This class contains unit tests for the TransactionsDao class.
 */
public class TransactionsDaoTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<FIN_Utility> mockedFINUtility;
  private MockedStatic<AccDefUtility> mockedAccDefUtility;
  private MockedStatic<OBContext> mockedOBContext;
  private AutoCloseable mocks;

  @Mock
  private OBDal obDal;

  @Mock
  private OBProvider obProvider;

  @Mock
  private FIN_Payment mockPayment;

  @Mock
  private FIN_FinancialAccount mockFinancialAccount;

  @Mock
  private FIN_FinaccTransaction mockTransaction;

  @Mock
  private DocumentType mockDocumentType;

  @Mock
  private Organization mockOrganization;

  @Mock
  private Currency mockCurrency;

  @Mock
  private Currency mockAccountCurrency;

  @Mock
  private Project mockProject;

  @Mock
  private OBCriteria<FIN_Reconciliation> mockCriteria;

  @Mock
  private OBCriteria<AccountingFact> mockAccountingFactCriteria;

  @Mock
  private AccountingFact mockAccountingFact;

  @Mock
  private Table mockTable;

  @Mock
  private OBQuery<FIN_FinaccTransaction> mockQuery;

  @Mock
  private FIN_Reconciliation mockReconciliation;

  @Mock
  private OBContext mockOBContext;

  /**
   * Sets up the test environment before each test.
   * Initializes mocks and configures default mock behavior.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize mocks
    mocks = MockitoAnnotations.openMocks(this);

    mockedOBDal = mockStatic(OBDal.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedFINUtility = mockStatic(FIN_Utility.class);
    mockedAccDefUtility = mockStatic(AccDefUtility.class);
    mockedOBContext = mockStatic(OBContext.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);

    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);

    when(mockOBContext.getUser()).thenReturn(mock(org.openbravo.model.ad.access.User.class));
    when(mockOBContext.getCurrentClient()).thenReturn(mock(Client.class));
    when(mockOBContext.getCurrentOrganization()).thenReturn(mock(Organization.class));
    when(mockOBContext.getRole()).thenReturn(mock(org.openbravo.model.ad.access.Role.class));

    when(mockOBContext.getUser().getId()).thenReturn(org.openbravo.test.base.TestConstants.Users.ADMIN);
    when(mockOBContext.getCurrentClient().getId()).thenReturn(org.openbravo.test.base.TestConstants.Clients.FB_GRP);
    when(mockOBContext.getCurrentOrganization().getId()).thenReturn(
        org.openbravo.test.base.TestConstants.Orgs.ESP_NORTE);
    when(mockOBContext.getRole().getId()).thenReturn(org.openbravo.test.base.TestConstants.Roles.FB_GRP_ADMIN);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static resources and mocks.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedAccDefUtility != null) {
      mockedAccDefUtility.close();
    }
    if (mockedFINUtility != null) {
      mockedFINUtility.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the createFinAccTransaction method.
   * Verifies that a financial account transaction is created successfully.
   */
  @Test
  public void testCreateFinAccTransactionSuccess() {
    // GIVEN
    FIN_FinaccTransaction newTransaction = mock(FIN_FinaccTransaction.class);
    when(obProvider.get(FIN_FinaccTransaction.class)).thenReturn(newTransaction);

    String paymentId = "TEST_PAYMENT_ID";
    when(mockPayment.getId()).thenReturn(paymentId);
    when(mockPayment.getOrganization()).thenReturn(mockOrganization);
    when(mockPayment.getAccount()).thenReturn(mockFinancialAccount);
    Date paymentDate = new Date();
    when(mockPayment.getPaymentDate()).thenReturn(paymentDate);
    when(mockPayment.getActivity()).thenReturn(null);
    when(mockPayment.getProject()).thenReturn(mockProject);
    when(mockPayment.getCostCenter()).thenReturn(null);
    when(mockPayment.getStDimension()).thenReturn(null);
    when(mockPayment.getNdDimension()).thenReturn(null);
    when(mockFinancialAccount.getCurrency()).thenReturn(mockAccountCurrency);
    when(mockPayment.getDescription()).thenReturn("Test payment description");

    Client mockClient = mock(Client.class);
    when(mockClient.getId()).thenReturn("TEST_CLIENT_ID");
    when(mockPayment.getClient()).thenReturn(mockClient);

    when(mockDocumentType.getDocumentCategory()).thenReturn("ARR");
    when(mockPayment.getDocumentType()).thenReturn(mockDocumentType);
    when(mockPayment.getFinancialTransactionAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));
    when(mockPayment.getCurrency()).thenReturn(mockCurrency);

    when(mockPayment.getFinancialTransactionConvertRate()).thenReturn(new BigDecimal("1.0"));
    when(mockPayment.getAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));

    List<FIN_FinaccTransaction> transactionList = new ArrayList<>();
    when(mockPayment.getFINFinaccTransactionList()).thenReturn(transactionList);

    // Mock getTransactionMaxLineNo
    when(obDal.getSession()).thenReturn(mock(org.hibernate.Session.class));
    org.hibernate.query.Query mockHibernateQuery = mock(org.hibernate.query.Query.class);
    when(obDal.getSession().createQuery(anyString(), eq(Long.class))).thenReturn(mockHibernateQuery);
    when(mockHibernateQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
        mockHibernateQuery);
    when(mockHibernateQuery.uniqueResult()).thenReturn(10L);

    // Mock FIN_Utility methods
    mockedFINUtility.when(() -> FIN_Utility.getDepositAmount(true, new BigDecimal(TestConstants.AMOUNT))).thenReturn(
        new BigDecimal(TestConstants.AMOUNT));
    mockedFINUtility.when(() -> FIN_Utility.getPaymentAmount(true, new BigDecimal(TestConstants.AMOUNT))).thenReturn(
        BigDecimal.ZERO);

    when(newTransaction.getDepositAmount()).thenReturn(new BigDecimal(TestConstants.AMOUNT));
    when(newTransaction.getPaymentAmount()).thenReturn(BigDecimal.ZERO);

    when(newTransaction.getCurrency()).thenReturn(mockAccountCurrency);
    when(newTransaction.getStatus()).thenReturn("RPR");

    // WHEN
    FIN_FinaccTransaction result = TransactionsDao.createFinAccTransaction(mockPayment);

    // THEN
    assertNotNull(result);
    assertEquals(newTransaction, result);
    assertEquals(1, transactionList.size());
    assertEquals(newTransaction, transactionList.get(0));
  }

  /**
   * Tests the getTransactionMaxLineNo method.
   * Verifies that the maximum line number is returned correctly.
   */
  @Test
  public void testGetTransactionMaxLineNoReturnsMaxLineNo() {
    // GIVEN
    when(obDal.getSession()).thenReturn(mock(org.hibernate.Session.class));
    org.hibernate.query.Query mockHibernateQuery = mock(org.hibernate.query.Query.class);
    when(obDal.getSession().createQuery(anyString(), eq(Long.class))).thenReturn(mockHibernateQuery);
    when(mockHibernateQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
        mockHibernateQuery);
    when(mockHibernateQuery.uniqueResult()).thenReturn(100L);

    // WHEN
    Long result = TransactionsDao.getTransactionMaxLineNo(mockFinancialAccount);

    // THEN
    assertEquals(Long.valueOf(100), result);
  }

  /**
   * Tests the getTransactionMaxLineNo method when no transactions exist.
   * Verifies that zero is returned.
   */
  @Test
  public void testGetTransactionMaxLineNoReturnsZeroWhenNoTransactions() {
    // GIVEN
    when(obDal.getSession()).thenReturn(mock(org.hibernate.Session.class));
    org.hibernate.query.Query mockHibernateQuery = mock(org.hibernate.query.Query.class);
    when(obDal.getSession().createQuery(anyString(), eq(Long.class))).thenReturn(mockHibernateQuery);
    when(mockHibernateQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
        mockHibernateQuery);
    when(mockHibernateQuery.uniqueResult()).thenReturn(null);

    // WHEN
    Long result = TransactionsDao.getTransactionMaxLineNo(mockFinancialAccount);

    // THEN
    assertEquals(Long.valueOf(0), result);
  }

  /**
   * Tests the getLastReconciliation method.
   * Verifies that the last reconciliation is returned correctly.
   */
  @Test
  public void testGetLastReconciliationProcessed() {
    // GIVEN
    when(obDal.createCriteria(FIN_Reconciliation.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(org.mockito.ArgumentMatchers.any(org.hibernate.criterion.Criterion.class))).thenReturn(
        mockCriteria);
    when(mockCriteria.addOrderBy(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn(mockCriteria);
    when(mockCriteria.setMaxResults(1)).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(mockReconciliation);

    // WHEN
    FIN_Reconciliation result = TransactionsDao.getLastReconciliation(mockFinancialAccount, "Y");

    // THEN
    assertEquals(mockReconciliation, result);
  }

  /**
   * Tests the updateAccountingDate method.
   * Verifies that the accounting date is updated successfully.
   */
  @Test
  public void testUpdateAccountingDateUpdatesDateSuccessfully() {
    // GIVEN
    final String FIN_FINACC_TRANSACTION_TABLE = "4D8C3B3C31D1410DA046140C9F024D17";
    Date transactionDate = new Date();
    when(mockTransaction.getTransactionDate()).thenReturn(transactionDate);
    when(mockTransaction.getOrganization()).thenReturn(mockOrganization);
    when(mockTransaction.getId()).thenReturn("TEST_TRANSACTION_ID");

    when(obDal.createCriteria(AccountingFact.class)).thenReturn(mockAccountingFactCriteria);
    when(mockAccountingFactCriteria.add(
        org.mockito.ArgumentMatchers.any(org.hibernate.criterion.Criterion.class))).thenReturn(
        mockAccountingFactCriteria);

    when(obDal.get(Table.class, FIN_FINACC_TRANSACTION_TABLE)).thenReturn(mockTable);

    List<AccountingFact> accountingFacts = new ArrayList<>();
    accountingFacts.add(mockAccountingFact);
    when(mockAccountingFactCriteria.list()).thenReturn(accountingFacts);

    org.openbravo.model.financialmgmt.calendar.Period mockPeriod = mock(
        org.openbravo.model.financialmgmt.calendar.Period.class);
    when(mockPeriod.getId()).thenReturn("202301");

    mockedAccDefUtility.when(() -> AccDefUtility.getCurrentPeriod(org.mockito.ArgumentMatchers.any(Date.class),
        org.mockito.ArgumentMatchers.any())).thenReturn(mockPeriod);

    // WHEN
    TransactionsDao.updateAccountingDate(mockTransaction);

    // THEN
    // Verify that the accounting fact was updated with the transaction date
    mockedAccDefUtility.verify(
        () -> AccDefUtility.getCurrentPeriod(eq(transactionDate), org.mockito.ArgumentMatchers.any()));
  }

  /**
   * Tests the getCurrentlyClearedAmt method.
   * Verifies that the currently cleared amount is returned correctly.
   */
  @Test
  public void testGetCurrentlyClearedAmtReturnsCorrectAmount() {
    // GIVEN
    String accountId = TestConstants.TEST_ACCOUNT_ID;
    when(obDal.getSession()).thenReturn(mock(org.hibernate.Session.class));
    org.hibernate.query.Query mockHibernateQuery = mock(org.hibernate.query.Query.class);
    when(obDal.getSession().createQuery(anyString(), eq(BigDecimal.class))).thenReturn(mockHibernateQuery);
    when(mockHibernateQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
        mockHibernateQuery);
    when(mockHibernateQuery.uniqueResult()).thenReturn(new BigDecimal("150.00"));

    // WHEN
    BigDecimal result = TransactionsDao.getCurrentlyClearedAmt(accountId);

    // THEN
    assertMonetaryEquals("The amount should match", new BigDecimal("150.00"), result);
  }

  /**
   * Tests the getCurrentlyClearedAmt method.
   * Verifies that the currently cleared amount is returned correctly.
   */
  @Test
  public void testGetCurrentlyClearedAmtReturnsZeroWhenNoResults() {
    // GIVEN
    String accountId = TestConstants.TEST_ACCOUNT_ID;
    when(obDal.getSession()).thenReturn(mock(org.hibernate.Session.class));
    org.hibernate.query.Query mockHibernateQuery = mock(org.hibernate.query.Query.class);
    when(obDal.getSession().createQuery(anyString(), eq(BigDecimal.class))).thenReturn(mockHibernateQuery);
    when(mockHibernateQuery.setParameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(
        mockHibernateQuery);
    when(mockHibernateQuery.uniqueResult()).thenReturn(null);

    // WHEN
    BigDecimal result = TransactionsDao.getCurrentlyClearedAmt(accountId);

    // THEN
    assertMonetaryEquals("The amount should be zero", BigDecimal.ZERO, result);
  }

  /**
   * Tests the getTransactionsFiltered method with hideAfterDate set to true.
   * Verifies that the transactions are filtered correctly.
   */
  @Test
  public void testGetTransactionsFilteredWithHideAfterDate() {
    // GIVEN
    String accountId = TestConstants.TEST_ACCOUNT_ID;
    when(mockFinancialAccount.getId()).thenReturn(accountId);
    Date statementDate = new Date();

    // Mock para OBQuery
    when(obDal.createQuery(eq(FIN_FinaccTransaction.class), anyString(),
        org.mockito.ArgumentMatchers.any(Map.class))).thenReturn(mockQuery);

    FIN_FinaccTransaction mockTransaction1 = createMockTransaction("TRANS_1", new Date(), TestConstants.AMOUNT, "0.00",
        "Description 1", "Y");

    List<FIN_FinaccTransaction> transactionsList = List.of(mockTransaction1);
    when(mockQuery.list()).thenReturn(transactionsList);

    // Mock para FieldProviderFactory
    FieldProvider[] mockFieldProviders = new FieldProvider[transactionsList.size()];
    for (int i = 0; i < transactionsList.size(); i++) {
      mockFieldProviders[i] = mock(FieldProvider.class);
    }

    org.mockito.MockedStatic<FieldProviderFactory> mockedFieldProviderFactory = mockStatic(FieldProviderFactory.class);
    mockedFieldProviderFactory.when(() -> FieldProviderFactory.getFieldProviderArray(eq(transactionsList))).thenReturn(
        mockFieldProviders);


    // WHEN
    FieldProvider[] result = TransactionsDao.getTransactionsFiltered(mockFinancialAccount, statementDate, true);

    // THEN
    assertNotNull(result);
    assertEquals(1, result.length);

    verify(mockQuery).list();

    ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
    verify(obDal).createQuery(eq(FIN_FinaccTransaction.class), anyString(), paramsCaptor.capture());

    Map<String, Object> capturedParams = paramsCaptor.getValue();
    assertEquals(accountId, capturedParams.get("accountId"));
    assertEquals(statementDate, capturedParams.get("statementDate"));

    mockedFieldProviderFactory.close();
  }

  /**
   * Tests the getTransactionsFiltered method when an exception is thrown.
   * Verifies that an OBException is thrown.
   */
  @Test
  public void testGetTransactionsFilteredWithException() {
    // GIVEN
    String accountId = TestConstants.TEST_ACCOUNT_ID;
    when(mockFinancialAccount.getId()).thenReturn(accountId);

    // Mock para OBQuery
    when(obDal.createQuery(eq(FIN_FinaccTransaction.class), anyString(),
        org.mockito.ArgumentMatchers.any(Map.class))).thenThrow(new RuntimeException("Test database error"));

    // WHEN & THEN
    expectedException.expect(OBException.class);
    TransactionsDao.getTransactionsFiltered(mockFinancialAccount, new Date(), false);
  }

  /**
   * Utility method for creating a mock FIN_FinaccTransaction.
   *
   * @param id
   *     the transaction ID
   * @param transactionDate
   *     the transaction date
   * @param depositAmount
   *     the deposit amount
   * @param paymentAmount
   *     the payment amount
   * @param description
   *     the transaction description
   * @param status
   *     the transaction status
   * @return the mock FIN_FinaccTransaction
   */
  private FIN_FinaccTransaction createMockTransaction(String id, Date transactionDate, String depositAmount,
      String paymentAmount, String description, String status) {

    FIN_FinaccTransaction mockTrans = mock(FIN_FinaccTransaction.class);
    when(mockTrans.getId()).thenReturn(id);
    when(mockTrans.getTransactionDate()).thenReturn(transactionDate);
    when(mockTrans.getDepositAmount()).thenReturn(new BigDecimal(depositAmount));
    when(mockTrans.getPaymentAmount()).thenReturn(new BigDecimal(paymentAmount));
    when(mockTrans.getDescription()).thenReturn(description);
    when(mockTrans.getStatus()).thenReturn(status);

    return mockTrans;
  }

  /**
   * Utility method for comparing monetary values.
   *
   * @param message
   *     the assertion message
   * @param expected
   *     the expected value
   * @param actual
   *     the actual value
   */
  protected void assertMonetaryEquals(String message, BigDecimal expected, BigDecimal actual) {
    assertEquals(message, expected.setScale(2, RoundingMode.HALF_UP), actual.setScale(2, RoundingMode.HALF_UP));
  }
}
