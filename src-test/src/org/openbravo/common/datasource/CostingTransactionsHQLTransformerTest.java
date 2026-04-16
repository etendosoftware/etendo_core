package org.openbravo.common.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.materialmgmt.cost.Costing;
/** Tests for {@link CostingTransactionsHQLTransformer}. */

@RunWith(MockitoJUnitRunner.class)
public class CostingTransactionsHQLTransformerTest {

  private static final String MATERIAL_MGMT_COSTING_ID = "@MaterialMgmtCosting.id@";
  private static final String SELECT_WHERE_CLAUSE_PREVIOUS_COSTING_COS = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
  private static final String VAL_1_2 = " 1 = 2 ";
  private static final String VAL_10_00 = "10.00";

  private CostingTransactionsHQLTransformer instance;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CostingTransactionsHQLTransformer.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
  }
  /** Transform hql query with null costing id returns fallback. */

  @Test
  public void testTransformHqlQueryWithNullCostingIdReturnsFallback() {
    String result = transformWithCostingId(null);

    assertNotNull(result);
    assertTrue(result.contains(VAL_1_2));
    assertTrue(result.contains("0"));
  }
  /** Transform hql query with null string costing id returns fallback. */

  @Test
  public void testTransformHqlQueryWithNullStringCostingIdReturnsFallback() {
    String result = transformWithCostingId("null");

    assertNotNull(result);
    assertTrue(result.contains(VAL_1_2));
  }
  /** Transform hql query with missing costing param returns fallback. */

  @Test
  public void testTransformHqlQueryWithMissingCostingParamReturnsFallback() {
    Map<String, String> requestParams = new HashMap<>();
    String result = instance.transformHqlQuery(SELECT_WHERE_CLAUSE_PREVIOUS_COSTING_COS,
        requestParams, new HashMap<>());

    assertNotNull(result);
    assertTrue(result.contains(VAL_1_2));
  }
  /** Transform hql query with non ava cost type returns fallback. */

  @Test
  public void testTransformHqlQueryWithNonAvaCostTypeReturnsFallback() {
    setupCostingMock("COSTING_001", "STA", null);

    String result = transformWithCostingId("COSTING_001");

    assertNotNull(result);
    assertTrue(result.contains(VAL_1_2));
  }
  /** Transform hql query with ava but null transaction returns fallback. */

  @Test
  public void testTransformHqlQueryWithAvaButNullTransactionReturnsFallback() {
    setupCostingMock("COSTING_002", "AVA", null);

    String result = transformWithCostingId("COSTING_002");

    assertNotNull(result);
    assertTrue(result.contains(VAL_1_2));
  }
  /**
   * Add cost on query with null costing returns zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddCostOnQueryWithNullCostingReturnsZero() throws Exception {
    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCostOnQuery",
        Costing.class);
    method.setAccessible(true);

    String result = (String) method.invoke(instance, (Costing) null);
    assertEquals("0", result);
  }
  /**
   * Add cost on query with costing returns cost.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddCostOnQueryWithCostingReturnsCost() throws Exception {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCost()).thenReturn(new BigDecimal("15.75"));

    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCostOnQuery",
        Costing.class);
    method.setAccessible(true);

    String result = (String) method.invoke(instance, mockCosting);
    assertEquals("15.75", result);
  }
  /**
   * Add cum cost with null prev costing returns zero else branch.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddCumCostWithNullPrevCostingReturnsZeroElseBranch() throws Exception {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCost()).thenReturn(new BigDecimal(VAL_10_00));

    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCumCost",
        String.class, Costing.class, Costing.class);
    method.setAccessible(true);

    String cumQty = "(select sum(...))";
    String result = (String) method.invoke(instance, cumQty, mockCosting, null);

    assertNotNull(result);
    assertTrue(result.contains(VAL_10_00));
    assertTrue(result.contains("0"));
    assertTrue(result.contains("case when"));
  }
  /**
   * Add cum cost with prev costing includes prev cost.
   * @throws Exception if an error occurs
   */

  @Test
  public void testAddCumCostWithPrevCostingIncludesPrevCost() throws Exception {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCost()).thenReturn(new BigDecimal(VAL_10_00));

    Costing mockPrevCosting = mock(Costing.class);
    when(mockPrevCosting.getCost()).thenReturn(new BigDecimal("8.50"));

    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCumCost",
        String.class, Costing.class, Costing.class);
    method.setAccessible(true);

    String cumQty = "(select sum(...))";
    String result = (String) method.invoke(instance, cumQty, mockCosting, mockPrevCosting);

    assertNotNull(result);
    assertTrue(result.contains(VAL_10_00));
    assertTrue(result.contains("8.50"));
    assertTrue(result.contains("case when"));
  }

  // --- Helper methods ---

  private String transformWithCostingId(String costingId) {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put(MATERIAL_MGMT_COSTING_ID, costingId);
    return instance.transformHqlQuery(SELECT_WHERE_CLAUSE_PREVIOUS_COSTING_COS,
        requestParams, new HashMap<>());
  }

  private void setupCostingMock(String costingId, String costType,
      org.openbravo.model.materialmgmt.transaction.MaterialTransaction transaction) {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCostType()).thenReturn(costType);
    when(mockCosting.getInventoryTransaction()).thenReturn(transaction);
    when(mockOBDal.get(Costing.class, costingId)).thenReturn(mockCosting);
  }
}
