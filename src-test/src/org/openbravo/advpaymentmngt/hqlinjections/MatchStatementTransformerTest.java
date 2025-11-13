package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.dao.TransactionsDao;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test class for MatchStatementTransformer.
 */
public class MatchStatementTransformerTest extends OBBaseTest {

  @InjectMocks
  private MatchStatementTransformer transformer;

  @Mock
  private OBDal obDal;

  @Mock
  private FIN_FinancialAccount mockFinAccount;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<TransactionsDao> mockedTransactionsDao;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment before each test.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    transformer = new MatchStatementTransformer();

    // Setup static mocks
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

    mockedTransactionsDao = mockStatic(TransactionsDao.class);

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
  @AfterEach
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedTransactionsDao != null) {
      mockedTransactionsDao.close();
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
    assertTrue(result.contains("ORDER BY e.id"), "Should preserve existing ORDER BY");
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
    assertTrue(result.contains("WHERE  order by banklineDate, lineNo"), "Should contain empty where clause");
    assertTrue(queryNamedParameters.isEmpty(), "Should not contain any parameters");
  }
}
