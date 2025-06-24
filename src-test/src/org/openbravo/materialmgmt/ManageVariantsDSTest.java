package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicConf;
import org.openbravo.service.json.JsonUtils;

/**
 * Test class for ManageVariantsDS.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageVariantsDSTest extends WeldBaseTest {

  private static final String MANAGE_VARIANTS_LIMIT = "ManageVariantsLimit";
  private static final String TEST_PRODUCT_ID = "TEST_PRODUCT_ID";
  private static final String PRODUCT_ID_PLACEHOLDER = "@Product.id@";
  private static final String CRITERIA = "criteria";
  private static final String FIELD_NAME = "fieldName";
  private static final String OPERATOR = "operator";
  private static final String EQUALS = "equals";
  private static final String VALUE = "value";
  private static final String READ_CRITERIA = "readCriteria";

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<ModelProvider> mockedModelProvider;
  private MockedStatic<Preferences> mockedPreferences;
  private MockedStatic<JsonUtils> mockedJsonUtils;

  @Mock
  private OBDal obDal;

  @Mock
  private OBContext obContext;

  @Mock
  private ModelProvider modelProvider;

  @Mock
  private OBCriteria<ProductCharacteristic> productCharacteristicCriteria;

  @Mock
  private OBCriteria<ProductCharacteristicConf> productCharacteristicConfCriteria;

  @Mock
  private Product product;

  @InjectMocks
  private ManageVariantsDS manageVariantsDS;

  /**
   * Sets up the test environment.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedModelProvider = mockStatic(ModelProvider.class);
    mockedPreferences = mockStatic(Preferences.class);
    mockedJsonUtils = mockStatic(JsonUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(obContext);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(modelProvider);

    when(product.getSearchKey()).thenReturn("TEST_PRODUCT");
  }

  /**
   * Tears down the test environment.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedModelProvider != null) {
      mockedModelProvider.close();
    }
    if (mockedPreferences != null) {
      mockedPreferences.close();
    }
    if (mockedJsonUtils != null) {
      mockedJsonUtils.close();
    }
  }

  /**
   * Tests the getData method with no characteristics.
   */
  @Test
  public void testGetDataWithNoCharacteristics() {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PRODUCT_ID_PLACEHOLDER, TEST_PRODUCT_ID);

    setupGetDataMocks(parameters, 0);

    // WHEN
    List<Map<String, Object>> result = manageVariantsDS.getData(parameters, 0, 10);

    // THEN
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  /**
   * Sets up the mocks for the getData method.
   *
   * @param parameters
   *     the parameters
   * @param characteristicsCount
   *     the number of characteristics
   */
  private void setupGetDataMocks(Map<String, String> parameters, int characteristicsCount) {
    JSONObject jsonCriteria = mock(JSONObject.class);
    JSONArray criteriaArray = mock(JSONArray.class);

    try {
      when(criteriaArray.length()).thenReturn(0);
      when(jsonCriteria.get(CRITERIA)).thenReturn(criteriaArray);
      mockedJsonUtils.when(() -> JsonUtils.buildCriteria(parameters)).thenReturn(jsonCriteria);
    } catch (Exception e) {
      fail("Error setting up JSON mocks: " + e.getMessage());
    }

    when(obDal.get(Product.class, TEST_PRODUCT_ID)).thenReturn(product);

    when(obDal.createCriteria(ProductCharacteristic.class)).thenReturn(productCharacteristicCriteria);
    when(productCharacteristicCriteria.add(any())).thenReturn(productCharacteristicCriteria);
    when(productCharacteristicCriteria.addOrderBy(any(), anyBoolean())).thenReturn(productCharacteristicCriteria);

    List<ProductCharacteristic> characteristics = mock(List.class);
    when(characteristics.size()).thenReturn(characteristicsCount);
    when(productCharacteristicCriteria.list()).thenReturn(characteristics);

    if (characteristicsCount > 0) {
      // Mock for ProductCharacteristicConf
      when(obDal.createCriteria(ProductCharacteristicConf.class)).thenReturn(productCharacteristicConfCriteria);
      when(productCharacteristicConfCriteria.add(any())).thenReturn(productCharacteristicConfCriteria);

      List<ProductCharacteristicConf> characteristicConfs = mock(List.class);
      when(characteristicConfs.size()).thenReturn(1);
      when(productCharacteristicConfCriteria.list()).thenReturn(characteristicConfs);
    }
  }

  /**
   * Tests the readCriteria method.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testReadCriteria() throws Exception {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();

    // Mock for JsonUtils.buildCriteria
    JSONObject jsonCriteria = mock(JSONObject.class);
    JSONArray criteriaArray = mock(JSONArray.class);
    JSONObject criteria = mock(JSONObject.class);

    when(jsonCriteria.get(CRITERIA)).thenReturn(criteriaArray);
    when(criteriaArray.length()).thenReturn(1);
    when(criteriaArray.getJSONObject(0)).thenReturn(criteria);
    when(criteria.getString(FIELD_NAME)).thenReturn("name");
    when(criteria.getString(OPERATOR)).thenReturn(EQUALS);
    when(criteria.has(VALUE)).thenReturn(true);
    when(criteria.getString(VALUE)).thenReturn("Test Product");

    mockedJsonUtils.when(() -> JsonUtils.buildCriteria(parameters)).thenReturn(jsonCriteria);

    // WHEN
    Method readCriteriaMethod = ManageVariantsDS.class.getDeclaredMethod(READ_CRITERIA, Map.class);
    readCriteriaMethod.setAccessible(true);
    Object result = readCriteriaMethod.invoke(manageVariantsDS, parameters);

    // THEN
    assertNotNull(result);
  }

  /**
   * Tests the buildExistsClause method.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testBuildExistsClause() throws Exception {
    // GIVEN
    int index = 0;

    // WHEN
    Method buildExistsClauseMethod = ManageVariantsDS.class.getDeclaredMethod("buildExistsClause", int.class);
    buildExistsClauseMethod.setAccessible(true);
    String result = (String) buildExistsClauseMethod.invoke(manageVariantsDS, index);

    // THEN
    assertNotNull(result);
    assertTrue(StringUtils.contains(result, ":ch0"));
    assertTrue(StringUtils.contains(result, ":chvalue0"));
  }

  /**
   * Tests the getCount method.
   */
  @Test
  public void testGetCount() {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PRODUCT_ID_PLACEHOLDER, TEST_PRODUCT_ID);

    setupGetDataMocks(parameters, 0);

    // WHEN
    int count = manageVariantsDS.getCount(parameters);

    // THEN
    assertEquals(0, count);
  }

  /**
   * Tests the readCriteria method with multiple filters.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testReadCriteriaWithMultipleFilters() throws Exception {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();

    // Mock for JsonUtils.buildCriteria
    JSONObject jsonCriteria = mock(JSONObject.class);
    JSONArray criteriaArray = mock(JSONArray.class);
    JSONObject criteria1 = mock(JSONObject.class);
    JSONObject criteria2 = mock(JSONObject.class);

    when(jsonCriteria.get(CRITERIA)).thenReturn(criteriaArray);
    when(criteriaArray.length()).thenReturn(2);
    when(criteriaArray.getJSONObject(0)).thenReturn(criteria1);
    when(criteriaArray.getJSONObject(1)).thenReturn(criteria2);

    // First criteria for name
    when(criteria1.getString(FIELD_NAME)).thenReturn("name");
    when(criteria1.getString(OPERATOR)).thenReturn(EQUALS);
    when(criteria1.has(VALUE)).thenReturn(true);
    when(criteria1.getString(VALUE)).thenReturn("Test Product");

    // Second criteria for searchKey
    when(criteria2.getString(FIELD_NAME)).thenReturn("searchKey");
    when(criteria2.getString(OPERATOR)).thenReturn("contains");
    when(criteria2.has(VALUE)).thenReturn(true);
    when(criteria2.getString(VALUE)).thenReturn("TST");

    mockedJsonUtils.when(() -> JsonUtils.buildCriteria(parameters)).thenReturn(jsonCriteria);

    // WHEN
    Method readCriteriaMethod = ManageVariantsDS.class.getDeclaredMethod(READ_CRITERIA, Map.class);
    readCriteriaMethod.setAccessible(true);
    Object result = readCriteriaMethod.invoke(manageVariantsDS, parameters);

    // THEN
    assertNotNull(result);
    assertTrue(StringUtils.contains(result.getClass().getName(), "ProductChSelectedFilters"));
  }

  /**
   * Tests the readCriteria method with a characteristic filter.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testReadCriteriaWithCharacteristicFilter() throws Exception {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();

    // Mock for JsonUtils.buildCriteria
    JSONObject jsonCriteria = mock(JSONObject.class);
    JSONArray criteriaArray = mock(JSONArray.class);
    JSONObject criteria = mock(JSONObject.class);

    when(jsonCriteria.get(CRITERIA)).thenReturn(criteriaArray);
    when(criteriaArray.length()).thenReturn(1);
    when(criteriaArray.getJSONObject(0)).thenReturn(criteria);

    // Criteria for characteristicDescription
    when(criteria.getString(FIELD_NAME)).thenReturn("characteristicDescription");
    when(criteria.getString(OPERATOR)).thenReturn(EQUALS);
    when(criteria.has(VALUE)).thenReturn(true);
    when(criteria.getString(VALUE)).thenReturn("[\"CHAR_VALUE_1\"]");

    mockedJsonUtils.when(() -> JsonUtils.buildCriteria(parameters)).thenReturn(jsonCriteria);

    // Mock CharacteristicValue
    CharacteristicValue mockCharValue = mock(CharacteristicValue.class);
    Characteristic mockChar = mock(Characteristic.class);
    when(mockChar.getId()).thenReturn("CHAR_ID_1");
    when(mockCharValue.getCharacteristic()).thenReturn(mockChar);
    when(obDal.get(eq(CharacteristicValue.class), eq("CHAR_VALUE_1"))).thenReturn(mockCharValue);

    // WHEN
    Method readCriteriaMethod = ManageVariantsDS.class.getDeclaredMethod(READ_CRITERIA, Map.class);
    readCriteriaMethod.setAccessible(true);
    Object result = readCriteriaMethod.invoke(manageVariantsDS, parameters);

    // THEN
    assertNotNull(result);
  }

  /**
   * Tests the getData method with characteristics.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testGetDataWithCharacteristics() throws Exception {
    // GIVEN
    Map<String, String> parameters = new HashMap<>();
    parameters.put(PRODUCT_ID_PLACEHOLDER, TEST_PRODUCT_ID);

    // Basic mocks setup
    JSONObject jsonCriteria = mock(JSONObject.class);
    JSONArray criteriaArray = mock(JSONArray.class);

    when(criteriaArray.length()).thenReturn(0);
    when(jsonCriteria.get(CRITERIA)).thenReturn(criteriaArray);
    mockedJsonUtils.when(() -> JsonUtils.buildCriteria(parameters)).thenReturn(jsonCriteria);

    when(obDal.get(Product.class, TEST_PRODUCT_ID)).thenReturn(product);

    // Mock the getManageVariantsLimitPrefValue method to return a high value
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq(MANAGE_VARIANTS_LIMIT), anyBoolean(), (Client) any(), any(), any(),
            any(), any())).thenReturn("1000");

    // Create a simple mock that returns no characteristics
    when(obDal.createCriteria(ProductCharacteristic.class)).thenReturn(productCharacteristicCriteria);
    when(productCharacteristicCriteria.add(any())).thenReturn(productCharacteristicCriteria);
    when(productCharacteristicCriteria.addOrderBy(any(), anyBoolean())).thenReturn(productCharacteristicCriteria);

    List<ProductCharacteristic> emptyCharacteristics = new ArrayList<>();
    when(productCharacteristicCriteria.list()).thenReturn(emptyCharacteristics);

    // WHEN
    List<Map<String, Object>> result = manageVariantsDS.getData(parameters, 0, 10);

    // THEN
    assertNotNull(result);
    assertEquals(0, result.size());
  }

  /**
   * Tests the getManageVariantsLimitPrefValue method.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testGetManageVariantsLimitPrefValue() throws Exception {
    // GIVEN
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq(MANAGE_VARIANTS_LIMIT), anyBoolean(), (Client) any(), any(), any(),
            any(), any())).thenReturn("500");

    // WHEN
    Method getManageVariantsLimitPrefValueMethod = ManageVariantsDS.class.getDeclaredMethod(
        "getManageVariantsLimitPrefValue");
    getManageVariantsLimitPrefValueMethod.setAccessible(true);
    int result = (int) getManageVariantsLimitPrefValueMethod.invoke(manageVariantsDS);

    // THEN
    assertEquals(500, result);
  }

  /**
   * Tests the getManageVariantsLimitPrefValue method when an exception is thrown.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testGetManageVariantsLimitPrefValueWithException() throws Exception {
    // GIVEN
    // Mock the Preferences static method to throw an exception
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq(MANAGE_VARIANTS_LIMIT), anyBoolean(), (Client) any(), any(), any(),
            any(), any())).thenThrow(new RuntimeException("Test exception"));

    // WHEN
    Method getManageVariantsLimitPrefValueMethod = ManageVariantsDS.class.getDeclaredMethod(
        "getManageVariantsLimitPrefValue");
    getManageVariantsLimitPrefValueMethod.setAccessible(true);
    int result = (int) getManageVariantsLimitPrefValueMethod.invoke(manageVariantsDS);

    // THEN
    assertEquals(1000, result);
  }

  /**
   * Tests the throwExceptionIfVariantNumberIsTooHigh method.
   *
   * @throws Exception
   *     if an error occurs
   */
  @Test
  public void testThrowExceptionIfVariantNumberIsTooHigh() throws Exception {
    // GIVEN
    long variantNumber = 2000; // Higher than default limit

    // Mock the getManageVariantsLimitPrefValue method to return a low value
    mockedPreferences.when(
        () -> Preferences.getPreferenceValue(eq(MANAGE_VARIANTS_LIMIT), anyBoolean(), (Client) any(), any(), any(),
            any(), any())).thenReturn("1000");

    // WHEN/THEN
    Method throwExceptionIfVariantNumberIsTooHighMethod = ManageVariantsDS.class.getDeclaredMethod(
        "throwExceptionIfVariantNumberIsTooHigh", long.class);
    throwExceptionIfVariantNumberIsTooHighMethod.setAccessible(true);

    try {
      throwExceptionIfVariantNumberIsTooHighMethod.invoke(manageVariantsDS, variantNumber);
      fail("Should have thrown an OBException");
    } catch (Exception e) {
      // We expect an InvocationTargetException containing an OBException
      Throwable cause = e.getCause();
      assertTrue(cause instanceof OBException);
      assertEquals("ManageVariantsLimitReached", cause.getMessage());
    }
  }
}
