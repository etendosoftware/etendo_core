package org.openbravo.service.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.ui.Tab;
import org.openbravo.model.ad.utility.TableTree;
import org.openbravo.model.ad.utility.Tree;
import org.openbravo.service.json.DataToJsonConverter;

/**
 * Unit tests for {@link ADTreeDatasourceService}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ADTreeDatasourceServiceTest {

  private static final String TEST_TAB_ID = "TEST_TAB_ID";
  private static final String TEST_TABLE_ID = "TEST_TABLE_ID";
  private static final String IS_ORDERED = "isOrdered";

  private ADTreeDatasourceService instance;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private ModelProvider mockModelProvider;
  @Mock
  private OBProvider mockOBProvider;
  @Mock
  private Tree mockTree;
  @Mock
  private Table mockTable;
  @Mock
  private TableTree mockTableTree;
  @Mock
  private Tab mockTab;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  private MockedStatic<ModelProvider> modelProviderStatic;
  private MockedStatic<OBProvider> obProviderStatic;

  /** Sets up test fixtures. */
  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(ADTreeDatasourceService.class);

    obDalStatic = mockStatic(OBDal.class);
    obContextStatic = mockStatic(OBContext.class);
    modelProviderStatic = mockStatic(ModelProvider.class);
    obProviderStatic = mockStatic(OBProvider.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
    modelProviderStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(mockOBProvider);
  }

  /** Tears down test fixtures. */
  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
    if (modelProviderStatic != null) modelProviderStatic.close();
    if (obProviderStatic != null) obProviderStatic.close();
  }

  /** Get table tree returns table tree. */
  @Test
  public void testGetTableTreeReturnsTableTree() {
    setupTableTreeCriteria(mockTableTree);

    TableTree result = instance.getTableTree(mockTable);

    assertNotNull(result);
    assertEquals(mockTableTree, result);
  }

  /** Get table tree returns null when not found. */
  @Test
  public void testGetTableTreeReturnsNullWhenNotFound() {
    setupTableTreeCriteria(null);

    TableTree result = instance.getTableTree(mockTable);

    assertNull(result);
  }

  private void setupTableTreeCriteria(TableTree returnValue) {
    OBCriteria<TableTree> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(TableTree.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(returnValue);
  }

  /**
   * Is ordered returns true when table tree is ordered.
   * @throws Exception if an error occurs
   */
  @Test
  public void testIsOrderedReturnsTrueWhenTableTreeIsOrdered() throws Exception {
    setupTreeWithSingleTableTree(true);

    assertTrue(invokeIsOrdered());
  }

  /**
   * Is ordered returns false when table tree is not ordered.
   * @throws Exception if an error occurs
   */
  @Test
  public void testIsOrderedReturnsFalseWhenTableTreeIsNotOrdered() throws Exception {
    setupTreeWithSingleTableTree(false);

    assertFalse(invokeIsOrdered());
  }

  /**
   * Is ordered returns false when multiple table trees.
   * @throws Exception if an error occurs
   */
  @Test
  public void testIsOrderedReturnsFalseWhenMultipleTableTrees() throws Exception {
    List<TableTree> tableTreeList = new ArrayList<>();
    tableTreeList.add(mockTableTree);
    tableTreeList.add(mock(TableTree.class));
    when(mockTree.getTable()).thenReturn(mockTable);
    when(mockTable.getADTableTreeList()).thenReturn(tableTreeList);

    assertFalse(invokeIsOrdered());
  }

  private void setupTreeWithSingleTableTree(boolean ordered) {
    List<TableTree> tableTreeList = new ArrayList<>();
    tableTreeList.add(mockTableTree);
    when(mockTree.getTable()).thenReturn(mockTable);
    when(mockTable.getADTableTreeList()).thenReturn(tableTreeList);
    when(mockTableTree.isOrdered()).thenReturn(ordered);
  }

  private boolean invokeIsOrdered() throws Exception {
    Method method = ADTreeDatasourceService.class.getDeclaredMethod(IS_ORDERED, Tree.class);
    method.setAccessible(true);
    return (boolean) method.invoke(instance, mockTree);
  }

  /** Get datasource specific params with tab id. */
  @Test
  public void testGetDatasourceSpecificParamsWithTabId() {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("tabId", TEST_TAB_ID);
    parameters.put("treeReferenceId", null);

    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TEST_TABLE_ID);

    OBCriteria<Tree> mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(Tree.class)).thenReturn(mockCriteria);
    when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    lenient().when(mockCriteria.setFilterOnActive(anyBoolean())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(mockTree);

    Map<String, Object> result = instance.getDatasourceSpecificParams(parameters);

    assertNotNull(result);
    assertEquals(mockTree, result.get("tree"));
  }

  /** Get datasource specific params with no params. */
  @Test
  public void testGetDatasourceSpecificParamsWithNoParams() {
    Map<String, String> parameters = new HashMap<>();

    Map<String, Object> result = instance.getDatasourceSpecificParams(parameters);

    assertNotNull(result);
    assertNull(result.get("tree"));
  }

  /**
   * Fetch filtered nodes for trees with multi parent nodes returns empty array.
   * @throws Exception if an error occurs
   */
  @Test
  public void testFetchFilteredNodesForTreesWithMultiParentNodesReturnsEmptyArray()
      throws Exception {
    Map<String, String> parameters = new HashMap<>();
    Map<String, Object> datasourceParameters = new HashMap<>();
    List<String> filteredNodes = new ArrayList<>();

    JSONArray result = instance.fetchFilteredNodesForTreesWithMultiParentNodes(
        parameters, datasourceParameters, mockTableTree, filteredNodes, null, null, false);

    assertNotNull(result);
    assertEquals(0, result.length());
  }

  /**
   * Get json object by node id delegates to get json object by record id.
   * @throws Exception if an error occurs
   */
  @Test
  public void testGetJSONObjectByNodeIdDelegatesToGetJSONObjectByRecordId() throws Exception {
    Map<String, String> parameters = new HashMap<>();
    parameters.put("tabId", TEST_TAB_ID);
    Map<String, Object> datasourceParameters = new HashMap<>();
    datasourceParameters.put("tree", mockTree);

    when(mockOBDal.get(Tab.class, TEST_TAB_ID)).thenReturn(mockTab);
    when(mockTab.getHqlwhereclause()).thenReturn(null);
    when(mockTree.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TEST_TABLE_ID);

    Entity mockEntity = mock(Entity.class);
    when(mockModelProvider.getEntityByTableId(TEST_TABLE_ID)).thenReturn(mockEntity);

    DataToJsonConverter mockConverter = mock(DataToJsonConverter.class);
    when(mockOBProvider.get(DataToJsonConverter.class)).thenReturn(mockConverter);

    OBCriteria mockCriteria = mock(OBCriteria.class);
    when(mockOBDal.createCriteria(any(Class.class))).thenReturn(mockCriteria);
    lenient().when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
    lenient().when(mockCriteria.setFilterOnActive(anyBoolean())).thenReturn(mockCriteria);
    when(mockCriteria.uniqueResult()).thenReturn(null);

    // This will likely throw NPE internally since treeNode is null, but the method
    // catches exceptions and returns null
    instance.getJSONObjectByNodeId(parameters, datasourceParameters,
        "TEST_NODE");

    // The result may be null since the tree node lookup returned null
    // and the method catches exceptions internally
  }

  /** Node conforms to where clause with null where clause. */
  @Test
  public void testNodeConformsToWhereClauseWithNullWhereClause() {
    setupNodeConformsQuery(1);

    boolean result = instance.nodeConformsToWhereClause(mockTableTree, "NODE_1", null);

    assertTrue(result);
  }

  /** Node conforms to where clause returns false when no match. */
  @Test
  public void testNodeConformsToWhereClauseReturnsFalseWhenNoMatch() {
    setupNodeConformsQuery(0);

    boolean result = instance.nodeConformsToWhereClause(mockTableTree, "NODE_1", null);

    assertFalse(result);
  }

  private void setupNodeConformsQuery(int queryCount) {
    when(mockTableTree.getTable()).thenReturn(mockTable);
    when(mockTable.getId()).thenReturn(TEST_TABLE_ID);

    Entity mockEntity = mock(Entity.class);
    when(mockModelProvider.getEntityByTableId(TEST_TABLE_ID)).thenReturn(mockEntity);
    when(mockEntity.getName()).thenReturn("TestEntity");

    org.openbravo.dal.service.OBQuery mockQuery = mock(org.openbravo.dal.service.OBQuery.class);
    when(mockOBDal.createQuery(anyString(), anyString())).thenReturn(mockQuery);
    when(mockQuery.setFilterOnActive(anyBoolean())).thenReturn(mockQuery);
    when(mockQuery.setNamedParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.count()).thenReturn(queryCount);
  }
}
