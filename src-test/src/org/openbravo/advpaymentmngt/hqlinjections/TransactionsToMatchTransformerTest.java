package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_BankStatementLine;

/**
 * Test class for TransactionsToMatchTransformer.
 */
@RunWith(MockitoJUnitRunner.class)
public class TransactionsToMatchTransformerTest {

  /**
   * Rule for handling expected exceptions in tests.
   * This rule allows specifying the type of exception expected to be thrown
   * and verifying that the exception is thrown during the test execution.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private TransactionsToMatchTransformer transformer;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private FIN_BankStatementLine mockBankStatementLine;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    transformer = new TransactionsToMatchTransformer();

    // Setup static mocks
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
  }

  /**
   * Tests the transformHqlQuery method with valid parameters.
   */
  @Test
  public void testTransformHqlQueryValidParameters() {
    // Given
    String hqlQuery = TestConstants.SELECT_ENTITY_QUERY;
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String accountId = TestConstants.TEST_ACCOUNT_ID;
    String bankStatementLineId = "TEST_STATEMENT_LINE_ID";

    requestParameters.put("@FIN_Financial_Account.id@", accountId);
    requestParameters.put("bankStatementLineId", bankStatementLineId);

    Date mockDate = new Date();
    when(mockOBDal.get(FIN_BankStatementLine.class, bankStatementLineId)).thenReturn(mockBankStatementLine);
    when(mockBankStatementLine.getTransactionDate()).thenReturn(mockDate);

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Result should contain the account parameter", result.contains(TestConstants.ACCOUNT_CONDITION));
    assertEquals("Account ID should be set in named parameters", accountId, queryNamedParameters.get("account"));
    assertEquals("Date should be set in named parameters", mockDate, queryNamedParameters.get("dateTo"));
  }

  /**
   * Tests the transformHqlQuery method when the bank statement line is null.
   */
  @Test
  public void testTransformHqlQueryNullBankStatementLine() {
    // Given
    String hqlQuery = TestConstants.SELECT_ENTITY_QUERY;
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    String accountId = TestConstants.TEST_ACCOUNT_ID;
    String bankStatementLineId = "INVALID_ID";

    requestParameters.put("@FIN_Financial_Account.id@", accountId);
    requestParameters.put("bankStatementLineId", bankStatementLineId);

    when(mockOBDal.get(FIN_BankStatementLine.class, bankStatementLineId)).thenReturn(null);

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Result should contain the account parameter", result.contains(TestConstants.ACCOUNT_CONDITION));
    assertEquals("Account ID should be set in named parameters", accountId, queryNamedParameters.get("account"));
    assertTrue("Date parameter should be set even with null bank statement line",
        queryNamedParameters.get("dateTo") instanceof Date);
  }

  /**
   * Tests the transformHqlQuery method with empty parameters.
   */
  @Test
  public void testTransformHqlQueryEmptyParameters() {
    // Given
    String hqlQuery = TestConstants.SELECT_ENTITY_QUERY;
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Result should contain basic where clause", result.contains("e.reconciliation is null"));
    assertTrue("Result should contain account parameter reference", result.contains(TestConstants.ACCOUNT_CONDITION));
  }
}
