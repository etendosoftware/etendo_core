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

@RunWith(MockitoJUnitRunner.class)
public class CostingTransactionsHQLTransformerTest {

  private CostingTransactionsHQLTransformer instance;

  @Mock
  private OBDal mockOBDal;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(CostingTransactionsHQLTransformer.class);

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
  }

  @Test
  public void testTransformHqlQueryWithNullCostingIdReturnsFallback() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("@MaterialMgmtCosting.id@", null);
    Map<String, Object> namedParams = new HashMap<>();

    String hqlQuery = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
    String result = instance.transformHqlQuery(hqlQuery, requestParams, namedParams);

    assertNotNull(result);
    assertTrue(result.contains(" 1 = 2 "));
    assertTrue(result.contains("0"));
  }

  @Test
  public void testTransformHqlQueryWithNullStringCostingIdReturnsFallback() {
    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("@MaterialMgmtCosting.id@", "null");
    Map<String, Object> namedParams = new HashMap<>();

    String hqlQuery = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
    String result = instance.transformHqlQuery(hqlQuery, requestParams, namedParams);

    assertNotNull(result);
    assertTrue(result.contains(" 1 = 2 "));
  }

  @Test
  public void testTransformHqlQueryWithMissingCostingParamReturnsFallback() {
    Map<String, String> requestParams = new HashMap<>();
    Map<String, Object> namedParams = new HashMap<>();

    String hqlQuery = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
    String result = instance.transformHqlQuery(hqlQuery, requestParams, namedParams);

    assertNotNull(result);
    assertTrue(result.contains(" 1 = 2 "));
  }

  @Test
  public void testTransformHqlQueryWithNonAvaCostTypeReturnsFallback() {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCostType()).thenReturn("STA");
    when(mockCosting.getInventoryTransaction()).thenReturn(null);
    when(mockOBDal.get(Costing.class, "COSTING_001")).thenReturn(mockCosting);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("@MaterialMgmtCosting.id@", "COSTING_001");
    Map<String, Object> namedParams = new HashMap<>();

    String hqlQuery = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
    String result = instance.transformHqlQuery(hqlQuery, requestParams, namedParams);

    assertNotNull(result);
    assertTrue(result.contains(" 1 = 2 "));
  }

  @Test
  public void testTransformHqlQueryWithAvaButNullTransactionReturnsFallback() {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCostType()).thenReturn("AVA");
    when(mockCosting.getInventoryTransaction()).thenReturn(null);
    when(mockOBDal.get(Costing.class, "COSTING_002")).thenReturn(mockCosting);

    Map<String, String> requestParams = new HashMap<>();
    requestParams.put("@MaterialMgmtCosting.id@", "COSTING_002");
    Map<String, Object> namedParams = new HashMap<>();

    String hqlQuery = "SELECT @whereClause@ @previousCostingCost@ @cumQty@ @cumCost@";
    String result = instance.transformHqlQuery(hqlQuery, requestParams, namedParams);

    assertNotNull(result);
    assertTrue(result.contains(" 1 = 2 "));
  }

  @Test
  public void testAddCostOnQueryWithNullCostingReturnsZero() throws Exception {
    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCostOnQuery",
        Costing.class);
    method.setAccessible(true);

    String result = (String) method.invoke(instance, (Costing) null);
    assertEquals("0", result);
  }

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

  @Test
  public void testAddCumCostWithNullPrevCostingReturnsZeroElseBranch() throws Exception {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCost()).thenReturn(new BigDecimal("10.00"));

    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCumCost",
        String.class, Costing.class, Costing.class);
    method.setAccessible(true);

    String cumQty = "(select sum(...))";
    String result = (String) method.invoke(instance, cumQty, mockCosting, null);

    assertNotNull(result);
    assertTrue(result.contains("10.00"));
    assertTrue(result.contains("0"));
    assertTrue(result.contains("case when"));
  }

  @Test
  public void testAddCumCostWithPrevCostingIncludesPrevCost() throws Exception {
    Costing mockCosting = mock(Costing.class);
    when(mockCosting.getCost()).thenReturn(new BigDecimal("10.00"));

    Costing mockPrevCosting = mock(Costing.class);
    when(mockPrevCosting.getCost()).thenReturn(new BigDecimal("8.50"));

    Method method = CostingTransactionsHQLTransformer.class.getDeclaredMethod("addCumCost",
        String.class, Costing.class, Costing.class);
    method.setAccessible(true);

    String cumQty = "(select sum(...))";
    String result = (String) method.invoke(instance, cumQty, mockCosting, mockPrevCosting);

    assertNotNull(result);
    assertTrue(result.contains("10.00"));
    assertTrue(result.contains("8.50"));
    assertTrue(result.contains("case when"));
  }
}
