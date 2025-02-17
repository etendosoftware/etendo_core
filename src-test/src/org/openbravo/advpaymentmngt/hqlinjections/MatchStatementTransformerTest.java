package org.openbravo.advpaymentmngt.hqlinjections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

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
import org.openbravo.advpaymentmngt.hqlinjections.MatchStatementTransformer;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;
import org.openbravo.model.financialmgmt.payment.FIN_Reconciliation;

/**
 * Test class for MatchStatementTransformer
 */
public class MatchStatementTransformerTest extends WeldBaseTest {

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
    private AutoCloseable mocks;

    @Before
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

    @After
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

    @Test
    public void testTransformHqlQuery_WithExistingOrderBy() {
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

    @Test
    public void testTransformHqlQuery_WithoutFinancialAccount() {
        // Given
        String hqlQuery = "SELECT e FROM Entity e WHERE @whereClause@ @orderby@";
        Map<String, String> requestParameters = new HashMap<>();
        Map<String, Object> queryNamedParameters = new HashMap<>();

        // When
        String result = transformer.transformHqlQuery(hqlQuery, requestParameters, queryNamedParameters);

        // Then
        assertTrue("Should contain empty where clause", 
                  result.contains("WHERE  order by banklineDate, lineNo"));
        assertTrue("Should not contain any parameters", 
                  queryNamedParameters.isEmpty());
    }
}
