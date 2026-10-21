package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext.Usage;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for MatchStatementTransformer.
 */
public class MatchStatementTransformerTest extends OBBaseTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @InjectMocks
  private MatchStatementTransformer transformer;

  @Mock
  private OBDal obDal;

  @Mock
  private FIN_FinancialAccount mockFinAccount;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<TransactionsDao> mockedTransactionsDao;
  private MockedStatic<WeldUtils> mockedWeldUtils;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    transformer = new MatchStatementTransformer();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    mockedTransactionsDao = mockStatic(TransactionsDao.class);

    // No bank statement line filter hooks by default
    mockedWeldUtils = mockStatic(WeldUtils.class);
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenReturn(Collections.emptyList());

    // Mock basic behaviors
    when(obDal.get(eq(FIN_FinancialAccount.class), anyString())).thenReturn(mockFinAccount);
    when(mockFinAccount.getId()).thenReturn("TEST_FIN_ACCOUNT_ID");
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if an error occurs during teardown
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedTransactionsDao != null) {
      mockedTransactionsDao.close();
    }
    if (mockedWeldUtils != null) {
      mockedWeldUtils.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the transformHqlQuery method to ensure it preserves existing ORDER BY clauses.
   */
  @Test
  public void testTransformHqlQueryWithExistingOrderBy() {
    // Given
    String hqlQuery = "SELECT e FROM Entity e WHERE @whereClause@ ORDER BY e.id";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("@FIN_Financial_Account.id@", "TEST_FIN_ACCOUNT_ID");

    Map<String, Object> queryNamedParameters = new HashMap<>();

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Should preserve existing ORDER BY", result.contains("ORDER BY e.id"));
  }

  /**
   * Tests the transformHqlQuery method to ensure it handles queries without financial account parameters.
   */
  @Test
  public void testTransformHqlQueryWithoutFinancialAccount() {
    // Given
    String hqlQuery = "SELECT e FROM Entity e WHERE @whereClause@ @orderby@";
    Map<String, String> requestParameters = new HashMap<>();
    Map<String, Object> queryNamedParameters = new HashMap<>();

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Should contain empty where clause", result.contains("WHERE  order by banklineDate, lineNo"));
    assertTrue("Should not contain any parameters", queryNamedParameters.isEmpty());
  }

  /**
   * Tests the transformHqlQuery method with a financial account and without bank statement line
   * filter hooks.
   */
  @Test
  public void testTransformHqlQueryWithFinancialAccountWithoutHooks() {
    // Given
    String hqlQuery = "SELECT bsl FROM FIN_BankStatementLine bsl WHERE @whereClause@ @orderby@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("@FIN_Financial_Account.id@", "TEST_FIN_ACCOUNT_ID");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Should filter by financial account", result.contains("bs.account.id = :account"));
    assertFalse("Should not add any hook condition", result.contains("discarded"));
    assertEquals(Collections.singletonMap("account", "TEST_FIN_ACCOUNT_ID"), queryNamedParameters);
  }

  /**
   * Tests the transformHqlQuery method with a financial account and a bank statement line filter
   * hook. Verifies that the hook condition and its parameters are added to the grid query.
   */
  @Test
  public void testTransformHqlQueryWithFinancialAccountAndHook() {
    // Given
    BankStatementLineFilterHook hook = (alias, context, namedParameters) -> {
      if (context.getUsage() != Usage.MATCH_STATEMENT_GRID || context.getAccount() != mockFinAccount) {
        return null;
      }
      namedParameters.put("test_discarded", Boolean.FALSE);
      return alias + ".discarded = :test_discarded";
    };
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenReturn(Collections.singletonList(hook));
    String hqlQuery = "SELECT bsl FROM FIN_BankStatementLine bsl WHERE @whereClause@ @orderby@";
    Map<String, String> requestParameters = new HashMap<>();
    requestParameters.put("@FIN_Financial_Account.id@", "TEST_FIN_ACCOUNT_ID");
    Map<String, Object> queryNamedParameters = new HashMap<>();

    // When
    String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

    // Then
    assertTrue("Should add the hook condition",
        result.contains(" and (bsl.discarded = :test_discarded) order by banklineDate, lineNo"));
    assertEquals("TEST_FIN_ACCOUNT_ID", queryNamedParameters.get("account"));
    assertEquals(Boolean.FALSE, queryNamedParameters.get("test_discarded"));
  }

  /**
   * Tests that the bank statement line filter hooks are not evaluated when no financial account is
   * received.
   */
  @Test
  public void testTransformHqlQueryWithoutFinancialAccountDoesNotCallHooks() {
    // Given
    String hqlQuery = "SELECT e FROM Entity e WHERE @whereClause@ @orderby@";

    // When
    transformer.transformHqlQuery(hqlQuery, new HashMap<>(), new HashMap<>());

    // Then
    mockedWeldUtils.verify(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class), never());
  }
}
