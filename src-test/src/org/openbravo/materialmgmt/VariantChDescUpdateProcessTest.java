package org.openbravo.materialmgmt;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicValue;
import org.openbravo.scheduling.ProcessBundle;

/**
 * Unit tests for the {@link VariantChDescUpdateProcess} class.
 * <p>
 * This class tests the functionality of updating product characteristic descriptions
 * in Openbravo ERP.
 * </p>
 */
@RunWith(MockitoJUnitRunner.class)
public class VariantChDescUpdateProcessTest {

  private static final String TEST_PRODUCT_ID = "TEST_PRODUCT_ID";
  private static final String UPDATE_PRODUCT = "updateProduct";


  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private ProcessBundle mockProcessBundle;

  @Mock
  private OBQuery<ProductCharacteristic> mockPchQuery;

  @Mock
  private OBQuery<ProductCharacteristicValue> mockPchvQuery;

  @Mock
  private Product mockProduct;

  @Mock
  private ProductCharacteristic mockProductCharacteristic;

  @Mock
  private ProductCharacteristicValue mockProductCharacteristicValue;

  @Mock
  private Characteristic mockCharacteristic;

  @Mock
  private CharacteristicValue mockCharacteristicValue;

  @InjectMocks
  private VariantChDescUpdateProcess process;

  /**
   * Sets up the mocks and static context before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    Map<String, Object> params = new HashMap<>();
    when(mockProcessBundle.getParams()).thenReturn(params);

    when(mockProduct.getId()).thenReturn(TEST_PRODUCT_ID);

    when(mockCharacteristic.getId()).thenReturn("TEST_CHARACTERISTIC_ID");
    when(mockCharacteristic.getName()).thenReturn("Test Characteristic");
    when(mockProductCharacteristic.getCharacteristic()).thenReturn(mockCharacteristic);

    when(mockCharacteristicValue.getName()).thenReturn("Test Value");
    when(mockProductCharacteristicValue.getCharacteristicValue()).thenReturn(mockCharacteristicValue);
  }

  /**
   * Cleans up static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
  }

  /**
   * Tests the {@link VariantChDescUpdateProcess#doExecute(ProcessBundle)} method
   * under a normal, successful execution scenario.
   *
   * @throws Exception
   *     if an unexpected error occurs
   */
  @Test
  public void testDoExecuteHappyPath() throws Exception {
    // GIVEN
    Map<String, Object> params = new HashMap<>();
    params.put("mProductId", TEST_PRODUCT_ID);
    when(mockProcessBundle.getParams()).thenReturn(params);

    when(mockOBDal.get(Product.class, TEST_PRODUCT_ID)).thenReturn(mockProduct);

    when(mockOBDal.createQuery(eq(ProductCharacteristic.class), anyString())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchQuery);

    List<ProductCharacteristic> pchList = new ArrayList<>();
    pchList.add(mockProductCharacteristic);
    when(mockPchQuery.list()).thenReturn(pchList);

    when(mockOBDal.createQuery(eq(ProductCharacteristicValue.class), anyString())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchvQuery);

    List<ProductCharacteristicValue> pchvList = new ArrayList<>();
    pchvList.add(mockProductCharacteristicValue);
    when(mockPchvQuery.list()).thenReturn(pchvList);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Success");

    // WHEN
    process.doExecute(mockProcessBundle);

    // THEN
    verify(mockProduct).setCharacteristicDescription("Test Characteristic: Test Value");

    verify(mockProcessBundle).setResult(any(OBError.class));
  }

  /**
   * Test to verify that the `update` method correctly updates the characteristic description
   * of a product when given a product ID.
   *
   * <p>Scenario:
   * - A product with a specified ID is retrieved.
   * - Associated characteristics and characteristic values are fetched and linked.
   * - The characteristic description is updated for the product.</p>
   */
  @Test
  public void testUpdateWithProductId() {
    // GIVEN
    String productId = TEST_PRODUCT_ID;
    when(mockOBDal.get(Product.class, productId)).thenReturn(mockProduct);

    when(mockOBDal.createQuery(eq(ProductCharacteristic.class), anyString())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchQuery);

    List<ProductCharacteristic> pchList = new ArrayList<>();
    pchList.add(mockProductCharacteristic);
    when(mockPchQuery.list()).thenReturn(pchList);

    when(mockOBDal.createQuery(eq(ProductCharacteristicValue.class), anyString())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchvQuery);

    List<ProductCharacteristicValue> pchvList = new ArrayList<>();
    pchvList.add(mockProductCharacteristicValue);
    when(mockPchvQuery.list()).thenReturn(pchvList);

    // WHEN
    process.update(productId, null);

    // THEN
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockProduct).setCharacteristicDescription("Test Characteristic: Test Value");
  }

  /**
   * Test to verify that the `updateProduct` method correctly updates the characteristic
   * description of a product when the product has multiple characteristics.
   *
   * <p>Scenario:
   * - The product has characteristics "Color" and "Size".
   * - Each characteristic has a single associated value (e.g., "Red" and "Large").
   * - The description is correctly formatted and set as "Color: Red, Size: Large".</p>
   *
   * @throws NoSuchMethodException
   *     if the `updateProduct` method is not found.
   * @throws InvocationTargetException
   *     if the method invocation fails.
   * @throws IllegalAccessException
   *     if access to the method is denied.
   */
  @Test
  public void testUpdateProductWithMultipleCharacteristics() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // GIVEN
    when(mockOBDal.createQuery(eq(ProductCharacteristic.class), anyString())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchQuery);

    ProductCharacteristic pch1 = mock(ProductCharacteristic.class);
    Characteristic ch1 = mock(Characteristic.class);
    when(ch1.getName()).thenReturn("Color");
    when(pch1.getCharacteristic()).thenReturn(ch1);

    ProductCharacteristic pch2 = mock(ProductCharacteristic.class);
    Characteristic ch2 = mock(Characteristic.class);
    when(ch2.getName()).thenReturn("Size");
    when(pch2.getCharacteristic()).thenReturn(ch2);

    List<ProductCharacteristic> pchList = new ArrayList<>();
    pchList.add(pch1);
    pchList.add(pch2);
    when(mockPchQuery.list()).thenReturn(pchList);

    when(mockOBDal.createQuery(eq(ProductCharacteristicValue.class), anyString())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchvQuery);

    ProductCharacteristicValue pchv1 = mock(ProductCharacteristicValue.class);
    CharacteristicValue chv1 = mock(CharacteristicValue.class);
    when(chv1.getName()).thenReturn("Red");
    when(pchv1.getCharacteristicValue()).thenReturn(chv1);

    ProductCharacteristicValue pchv2 = mock(ProductCharacteristicValue.class);
    CharacteristicValue chv2 = mock(CharacteristicValue.class);
    when(chv2.getName()).thenReturn("Large");
    when(pchv2.getCharacteristicValue()).thenReturn(chv2);

    List<ProductCharacteristicValue> colorValues = new ArrayList<>();
    colorValues.add(pchv1);

    List<ProductCharacteristicValue> sizeValues = new ArrayList<>();
    sizeValues.add(pchv2);

    when(mockPchvQuery.list()).thenReturn(colorValues, sizeValues);

    // WHEN
    Method updateProductMethod = VariantChDescUpdateProcess.class.getDeclaredMethod(UPDATE_PRODUCT, Product.class);
    updateProductMethod.setAccessible(true);
    updateProductMethod.invoke(process, mockProduct);

    // THEN
    verify(mockProduct).setCharacteristicDescription("Color: Red, Size: Large");
  }

  /**
   * Test to verify that the `updateProduct` method correctly handles characteristics
   * with multiple associated values.
   *
   * <p>Scenario:
   * - The product has a single characteristic "Feature".
   * - The characteristic has multiple values ("Value1" and "Value2").
   * - The description is correctly formatted as "Feature: Value1 Value2".</p>
   *
   * @throws NoSuchMethodException
   *     if the `updateProduct` method is not found.
   * @throws InvocationTargetException
   *     if the method invocation fails.
   * @throws IllegalAccessException
   *     if access to the method is denied.
   */
  @Test
  public void testUpdateProductWithMultipleValuesPerCharacteristic() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // GIVEN
    when(mockOBDal.createQuery(eq(ProductCharacteristic.class), anyString())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchQuery);

    ProductCharacteristic pch = mock(ProductCharacteristic.class);
    Characteristic ch = mock(Characteristic.class);
    when(ch.getName()).thenReturn("Feature");
    when(pch.getCharacteristic()).thenReturn(ch);

    List<ProductCharacteristic> pchList = new ArrayList<>();
    pchList.add(pch);
    when(mockPchQuery.list()).thenReturn(pchList);

    when(mockOBDal.createQuery(eq(ProductCharacteristicValue.class), anyString())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchvQuery);
    when(mockPchvQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchvQuery);

    ProductCharacteristicValue pchv1 = mock(ProductCharacteristicValue.class);
    CharacteristicValue chv1 = mock(CharacteristicValue.class);
    when(chv1.getName()).thenReturn("Value1");
    when(pchv1.getCharacteristicValue()).thenReturn(chv1);

    ProductCharacteristicValue pchv2 = mock(ProductCharacteristicValue.class);
    CharacteristicValue chv2 = mock(CharacteristicValue.class);
    when(chv2.getName()).thenReturn("Value2");
    when(pchv2.getCharacteristicValue()).thenReturn(chv2);

    List<ProductCharacteristicValue> featureValues = new ArrayList<>();
    featureValues.add(pchv1);
    featureValues.add(pchv2);
    when(mockPchvQuery.list()).thenReturn(featureValues);

    // WHEN
    Method updateProductMethod = VariantChDescUpdateProcess.class.getDeclaredMethod(UPDATE_PRODUCT, Product.class);
    updateProductMethod.setAccessible(true);
    updateProductMethod.invoke(process, mockProduct);

    // THEN
    verify(mockProduct).setCharacteristicDescription("Feature: Value1 Value2");
  }

  /**
   * Test to verify that the `updateProduct` method correctly handles products
   * with no characteristics.
   *
   * <p>Scenario:
   * - The product has no associated characteristics or values.
   * - The description should be set to an empty string.</p>
   *
   * @throws NoSuchMethodException
   *     if the `updateProduct` method is not found.
   * @throws InvocationTargetException
   *     if the method invocation fails.
   * @throws IllegalAccessException
   *     if access to the method is denied.
   */
  @Test
  public void testUpdateProductWithNoCharacteristics() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // GIVEN
    when(mockOBDal.createQuery(eq(ProductCharacteristic.class), anyString())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnActive(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setFilterOnReadableOrganization(anyBoolean())).thenReturn(mockPchQuery);
    when(mockPchQuery.setNamedParameter(anyString(), any())).thenReturn(mockPchQuery);

    List<ProductCharacteristic> emptyList = new ArrayList<>();
    when(mockPchQuery.list()).thenReturn(emptyList);

    // WHEN
    Method updateProductMethod = VariantChDescUpdateProcess.class.getDeclaredMethod(UPDATE_PRODUCT, Product.class);
    updateProductMethod.setAccessible(true);
    updateProductMethod.invoke(process, mockProduct);

    // THEN
    verify(mockProduct).setCharacteristicDescription("");
  }

}
