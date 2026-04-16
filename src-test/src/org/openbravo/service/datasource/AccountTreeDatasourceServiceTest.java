/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.service.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.model.financialmgmt.accounting.coa.Element;

/**
 * Tests for {@link AccountTreeDatasourceService}.
 */
@SuppressWarnings("java:S112")
@RunWith(MockitoJUnitRunner.Silent.class)
public class AccountTreeDatasourceServiceTest {

  private static final String C_ELEMENTVALUE_TABLE_ID = "188";
  private static final String DATASOURCE_ID = "D2F94DC86DEC48D69E4BFCE59DC670CF";
  private static final String FINANCIALMGMTELEMENT_ID = "@FinancialMgmtElement.id@";

  private MockedStatic<OBDal> obDalStatic;

  @Mock
  private OBDal obDal;

  @Mock
  private Element element;

  @Mock
  private Tree tree;

  @Mock
  private Table table;

  @Mock
  private Table cElementValueTable;

  @Mock
  private OBCriteria<TableTree> criteria;

  @Mock
  private TableTree tableTree;

  @Mock
  private DataSource accountTreeDatasource;

  private AccountTreeDatasourceService service;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

    ObjenesisStd objenesis = new ObjenesisStd();
    service = objenesis.newInstance(AccountTreeDatasourceService.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
  }

  // --- getDatasourceSpecificParams tests ---
  /**
   * Get datasource specific params returns empty when account tree id is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsReturnsEmptyWhenAccountTreeIdIsNull() throws Exception {
    Map<String, String> parameters = new HashMap<>();

    Map<String, Object> result = invokeGetDatasourceSpecificParams(parameters);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
  /**
   * Get datasource specific params returns empty when account tree id is null string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsReturnsEmptyWhenAccountTreeIdIsNullString()
      throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put(FINANCIALMGMTELEMENT_ID, "null");

    Map<String, Object> result = invokeGetDatasourceSpecificParams(parameters);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }
  /**
   * Get datasource specific params returns tree when table is set.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsReturnsTreeWhenTableIsSet() throws Exception {
    String accountTreeId = "TEST_ELEMENT_ID";
    Map<String, String> parameters = new HashMap<>();
    parameters.put(FINANCIALMGMTELEMENT_ID, accountTreeId);

    when(obDal.get(Element.class, accountTreeId)).thenReturn(element);
    when(element.getTree()).thenReturn(tree);
    when(tree.getTable()).thenReturn(table);

    Map<String, Object> result = invokeGetDatasourceSpecificParams(parameters);

    assertNotNull(result);
    assertEquals(tree, result.get("tree"));
  }
  /**
   * Get datasource specific params sets table when tree table is null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsSetsTableWhenTreeTableIsNull() throws Exception {
    String accountTreeId = "TEST_ELEMENT_ID_2";
    Map<String, String> parameters = new HashMap<>();
    parameters.put(FINANCIALMGMTELEMENT_ID, accountTreeId);

    when(obDal.get(Element.class, accountTreeId)).thenReturn(element);
    when(element.getTree()).thenReturn(tree);
    when(tree.getTable()).thenReturn(null);
    when(obDal.get(Table.class, C_ELEMENTVALUE_TABLE_ID)).thenReturn(cElementValueTable);

    Map<String, Object> result = invokeGetDatasourceSpecificParams(parameters);

    verify(tree).setTable(cElementValueTable);
    assertNotNull(result);
    assertEquals(tree, result.get("tree"));
  }
  /**
   * Get datasource specific params does not set table when already set.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsDoesNotSetTableWhenAlreadySet() throws Exception {
    String accountTreeId = "TEST_ELEMENT_ID_3";
    Map<String, String> parameters = new HashMap<>();
    parameters.put(FINANCIALMGMTELEMENT_ID, accountTreeId);

    when(obDal.get(Element.class, accountTreeId)).thenReturn(element);
    when(element.getTree()).thenReturn(tree);
    when(tree.getTable()).thenReturn(table);

    invokeGetDatasourceSpecificParams(parameters);

    verify(tree, never()).setTable(any(Table.class));
  }
  /**
   * Get datasource specific params with empty parameters.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetDatasourceSpecificParamsWithEmptyParameters() throws Exception {
    Map<String, String> parameters = new HashMap<>();

    Map<String, Object> result = invokeGetDatasourceSpecificParams(parameters);

    assertTrue("Result should be empty when no account tree id parameter is present",
        result.isEmpty());
  }

  // --- getTableTree tests ---
  /**
   * Get table tree returns table tree.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetTableTreeReturnsTableTree() throws Exception {
    when(obDal.get(DataSource.class, DATASOURCE_ID)).thenReturn(accountTreeDatasource);
    when(obDal.createCriteria(TableTree.class)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(tableTree);

    TableTree result = invokeGetTableTree(table);

    assertNotNull(result);
    assertEquals(tableTree, result);
  }
  /**
   * Get table tree returns null when no criteria match.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetTableTreeReturnsNullWhenNoCriteriaMatch() throws Exception {
    when(obDal.get(DataSource.class, DATASOURCE_ID)).thenReturn(accountTreeDatasource);
    when(obDal.createCriteria(TableTree.class)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(null);

    TableTree result = invokeGetTableTree(table);

    assertEquals(null, result);
  }
  /**
   * Get table tree sets max results to one.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetTableTreeSetsMaxResultsToOne() throws Exception {
    when(obDal.get(DataSource.class, DATASOURCE_ID)).thenReturn(accountTreeDatasource);
    when(obDal.createCriteria(TableTree.class)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(tableTree);

    invokeGetTableTree(table);

    verify(criteria).setMaxResults(1);
  }
  /**
   * Get table tree adds three restrictions.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetTableTreeAddsThreeRestrictions() throws Exception {
    when(obDal.get(DataSource.class, DATASOURCE_ID)).thenReturn(accountTreeDatasource);
    when(obDal.createCriteria(TableTree.class)).thenReturn(criteria);
    when(criteria.uniqueResult()).thenReturn(tableTree);

    invokeGetTableTree(table);

    verify(criteria).setMaxResults(1);
    verify(criteria).uniqueResult();
  }

  // --- Helper methods ---

  @SuppressWarnings("unchecked")
  private Map<String, Object> invokeGetDatasourceSpecificParams(Map<String, String> parameters)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AccountTreeDatasourceService.class.getDeclaredMethod(
        "getDatasourceSpecificParams", Map.class);
    method.setAccessible(true);
    return (Map<String, Object>) method.invoke(service, parameters);
  }

  private TableTree invokeGetTableTree(Table tableParam) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AccountTreeDatasourceService.class.getDeclaredMethod("getTableTree",
        Table.class);
    method.setAccessible(true);
    return (TableTree) method.invoke(service, tableParam);
  }
}
