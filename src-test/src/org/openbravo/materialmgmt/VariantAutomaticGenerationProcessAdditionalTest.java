package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;

/**
 * Test class for the VariantAutomaticGenerationProcess.
 */
public class VariantAutomaticGenerationProcessAdditionalTest {

  private static final String RUN_CHECKS_METHOD_NAME = "runChecks";
  private static final String VALUE_1 = "VALUE1";


  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBDao> mockedOBDao;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<FIN_Utility> mockedFINUtility;
  private MockedStatic<DalUtil> mockedDalUtil;
  private AutoCloseable mocks;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Product mockProduct;

  @InjectMocks
  private VariantAutomaticGenerationProcess processUnderTest;

  /**
   * Sets up the test environment before each test.
   */
  @Before
  public void setUp() {
    Mockito.framework().clearInlineMocks();
    mocks = MockitoAnnotations.openMocks(this);
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBDao = mockStatic(OBDao.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedFINUtility = mockStatic(FIN_Utility.class);
    mockedDalUtil = mockStatic(DalUtil.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

    // Prepare process bundle
    Map<String, Object> params = new HashMap<>();
    params.put("M_Product_ID", "TEST_PRODUCT_ID");
  }

  /**
   * Cleans up the test environment after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedOBDao != null) {
      mockedOBDao.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mockedFINUtility != null) {
      mockedFINUtility.close();
    }
    if (mockedDalUtil != null) {
      mockedDalUtil.close();
    }
    if (mocks != null) {
      try {
        mocks.close();
      } catch (Exception ignored) {
        // no-op
      }
    }
  }

  /**
   * Tests the runChecks method with a valid generic product.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testRunChecksValidGenericProduct() throws Exception {
    // GIVEN
    // Configure a valid generic product with no variants and with a variant characteristic
    when(mockProduct.isGeneric()).thenReturn(true);
    when(mockProduct.getProductGenericProductList()).thenReturn(Collections.emptyList());

    List<ProductCharacteristic> characteristics = new ArrayList<>();
    ProductCharacteristic variantCh = mock(ProductCharacteristic.class);
    when(variantCh.isVariant()).thenReturn(true);
    characteristics.add(variantCh);

    when(mockProduct.getProductCharacteristicList()).thenReturn(characteristics);

    // Access private runChecks method via reflection
    Method runChecksMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod(RUN_CHECKS_METHOD_NAME,
        Product.class);
    runChecksMethod.setAccessible(true);

    // WHEN & THEN
    // Should not throw any exception
    runChecksMethod.invoke(processUnderTest, mockProduct);
  }

  /**
   * Tests the runChecks method with a product that has variants.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test(expected = InvocationTargetException.class)
  public void testRunChecksProductWithVariants() throws Exception {
    // GIVEN
    // Configure product as generic but with existing variants

    List<Product> variants = new ArrayList<>();
    variants.add(mock(Product.class));
    when(mockProduct.getProductGenericProductList()).thenReturn(variants);

    // Configure error message
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@ProductWithVariantsError@")).thenReturn(
        "The product already has variants");

    // Access private runChecks method via reflection
    Method runChecksMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod(RUN_CHECKS_METHOD_NAME,
        Product.class);
    runChecksMethod.setAccessible(true);

    // WHEN & THEN
    // Should throw an OBException
    runChecksMethod.invoke(processUnderTest, mockProduct);
  }

  /**
   * Tests the runChecks method with a non-generic product.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test(expected = InvocationTargetException.class)
  public void testRunChecksNonGenericProduct() throws Exception {
    // GIVEN
    // Configure product as non-generic
    when(mockProduct.isGeneric()).thenReturn(false);
    when(mockProduct.getProductGenericProductList()).thenReturn(Collections.emptyList());

    // Configure error message
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@ProductIsNotGenericError@")).thenReturn(
        "The product is not generic");

    // Access private runChecks method via reflection
    Method runChecksMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod(RUN_CHECKS_METHOD_NAME,
        Product.class);
    runChecksMethod.setAccessible(true);

    // WHEN & THEN
    // Should throw an OBException
    runChecksMethod.invoke(processUnderTest, mockProduct);
  }

  /**
   * Tests the runChecks method with a generic product that has no variant characteristics.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test(expected = InvocationTargetException.class)
  public void testRunChecksNoVariantCharacteristics() throws Exception {
    // GIVEN
    // Configure product as generic with no variants
    when(mockProduct.isGeneric()).thenReturn(true);
    when(mockProduct.getProductGenericProductList()).thenReturn(Collections.emptyList());

    // But no variant characteristics
    List<ProductCharacteristic> characteristics = new ArrayList<>();
    ProductCharacteristic nonVariantCh = mock(ProductCharacteristic.class);
    when(nonVariantCh.isVariant()).thenReturn(false);
    characteristics.add(nonVariantCh);

    when(mockProduct.getProductCharacteristicList()).thenReturn(characteristics);

    // Configure error message
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation("@GenericWithNoVariantChError@")).thenReturn(
        "The generic product has no variant characteristics");

    // Access private runChecks method via reflection
    Method runChecksMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod(RUN_CHECKS_METHOD_NAME,
        Product.class);
    runChecksMethod.setAccessible(true);

    // WHEN & THEN
    // Should throw an OBException
    runChecksMethod.invoke(processUnderTest, mockProduct);
  }

  /**
   * Tests the behavior of the ProductCharacteristicAux inner class.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testProductCharacteristicAuxBehavior() throws Exception {
    // GIVEN
    // Access inner class ProductCharacteristicAux via reflection
    Class<?> innerClass = Class.forName(
        "org.openbravo.materialmgmt.VariantAutomaticGenerationProcess$ProductCharacteristicAux");

    // Create constructor parameters
    boolean useCode = true;
    List<String> values = new ArrayList<>();
    values.add(VALUE_1);
    values.add("VALUE2");
    values.add("VALUE3");

    // Get constructor and create instance
    Object innerInstance = innerClass.getDeclaredConstructor(boolean.class, List.class).newInstance(useCode, values);

    // Get methods via reflection
    Method isUseCodeMethod = innerClass.getDeclaredMethod("isUseCode");
    Method isIteratorResetMethod = innerClass.getDeclaredMethod("isIteratorReset");
    Method getNextValueMethod = innerClass.getDeclaredMethod("getNextValue");

    // WHEN & THEN
    // Test isUseCode()
    boolean actualUseCode = (boolean) isUseCodeMethod.invoke(innerInstance);
    assertEquals(useCode, actualUseCode);

    // First call to getNextValue() should return first value and isIteratorReset should be true
    String value1 = (String) getNextValueMethod.invoke(innerInstance);
    boolean isReset1 = (boolean) isIteratorResetMethod.invoke(innerInstance);

    assertEquals(VALUE_1, value1);
    assertTrue(isReset1);

    // Second call should return second value and isIteratorReset should be false
    String value2 = (String) getNextValueMethod.invoke(innerInstance);
    boolean isReset2 = (boolean) isIteratorResetMethod.invoke(innerInstance);

    assertEquals("VALUE2", value2);
    assertFalse(isReset2);

    // Third call should return third value and isIteratorReset should be false
    String value3 = (String) getNextValueMethod.invoke(innerInstance);
    boolean isReset3 = (boolean) isIteratorResetMethod.invoke(innerInstance);

    assertEquals("VALUE3", value3);
    assertFalse(isReset3);

    // Fourth call should wrap around to first value and isIteratorReset should be true
    String value4 = (String) getNextValueMethod.invoke(innerInstance);
    boolean isReset4 = (boolean) isIteratorResetMethod.invoke(innerInstance);

    assertEquals(VALUE_1, value4);
    assertTrue(isReset4);
  }

  /**
   * Tests the setPrice method with multiple sales price lists.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testSetPriceMultipleSalesPriceLists() throws Exception {
    // GIVEN
    // Access private setPrice method via reflection
    Method setPriceMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod("setPrice", Product.class,
        BigDecimal.class, String.class);
    setPriceMethod.setAccessible(true);

    BigDecimal price = new BigDecimal("10.50");
    String priceListType = "SALES";

    // Create multiple mocked prices
    List<ProductPrice> productPrices = new ArrayList<>();

    // First price (sales)
    ProductPrice mockProdPrice1 = mock(ProductPrice.class);
    PriceListVersion mockPLV1 = mock(PriceListVersion.class);
    PriceList mockPL1 = mock(PriceList.class);
    when(mockProdPrice1.getPriceListVersion()).thenReturn(mockPLV1);
    when(mockPLV1.getPriceList()).thenReturn(mockPL1);
    when(mockPL1.isSalesPriceList()).thenReturn(true);
    productPrices.add(mockProdPrice1);

    // Second price (also sales)
    ProductPrice mockProdPrice2 = mock(ProductPrice.class);
    PriceListVersion mockPLV2 = mock(PriceListVersion.class);
    PriceList mockPL2 = mock(PriceList.class);
    when(mockProdPrice2.getPriceListVersion()).thenReturn(mockPLV2);
    when(mockPLV2.getPriceList()).thenReturn(mockPL2);
    when(mockPL2.isSalesPriceList()).thenReturn(true);
    productPrices.add(mockProdPrice2);

    // Third price (purchase)
    ProductPrice mockProdPrice3 = mock(ProductPrice.class);
    PriceListVersion mockPLV3 = mock(PriceListVersion.class);
    PriceList mockPL3 = mock(PriceList.class);
    when(mockProdPrice3.getPriceListVersion()).thenReturn(mockPLV3);
    when(mockPLV3.getPriceList()).thenReturn(mockPL3);
    when(mockPL3.isSalesPriceList()).thenReturn(false);
    productPrices.add(mockProdPrice3);

    // Mock to get price list
    mockedOBDao.when(
        () -> OBDao.getActiveOBObjectList(mockProduct, Product.PROPERTY_PRICINGPRODUCTPRICELIST)).thenReturn(
        productPrices);

    // WHEN
    setPriceMethod.invoke(processUnderTest, mockProduct, price, priceListType);

    // THEN
    // Both sales price lists should be updated
    verify(mockProdPrice1).setStandardPrice(price);
    verify(mockProdPrice1).setListPrice(price);
    verify(mockProdPrice1).setPriceLimit(price);
    verify(mockOBDal).save(mockProdPrice1);

    verify(mockProdPrice2).setStandardPrice(price);
    verify(mockProdPrice2).setListPrice(price);
    verify(mockProdPrice2).setPriceLimit(price);
    verify(mockOBDal).save(mockProdPrice2);

    // The purchase price list should not be updated
    verify(mockProdPrice3, never()).setStandardPrice(any(BigDecimal.class));
    verify(mockProdPrice3, never()).setListPrice(any(BigDecimal.class));
    verify(mockProdPrice3, never()).setPriceLimit(any(BigDecimal.class));
    verify(mockOBDal, never()).save(mockProdPrice3);
  }

  /**
   * Tests the setPrice method with an empty price list.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testSetPriceEmptyPriceList() throws Exception {
    // GIVEN
    // Access private setPrice method via reflection
    Method setPriceMethod = VariantAutomaticGenerationProcess.class.getDeclaredMethod("setPrice", Product.class,
        BigDecimal.class, String.class);
    setPriceMethod.setAccessible(true);

    BigDecimal price = new BigDecimal("10.50");
    String priceListType = "SALES";

    // Return empty price list
    List<ProductPrice> emptyList = Collections.emptyList();
    mockedOBDao.when(
        () -> OBDao.getActiveOBObjectList(mockProduct, Product.PROPERTY_PRICINGPRODUCTPRICELIST)).thenReturn(emptyList);

    // WHEN
    setPriceMethod.invoke(processUnderTest, mockProduct, price, priceListType);

    // THEN
    // No exceptions should be thrown, and no saves should happen
    verify(mockOBDal, never()).save(any(ProductPrice.class));
  }
}
