package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.lang.reflect.InvocationTargetException;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.common.uom.UOMConversion;

/**
 * Tests for AUM_ConversionRate callout.
 */
@SuppressWarnings({"java:S101", "java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.class)
public class AUM_ConversionRateTest {

  private static final String UOM_001 = "UOM_001";
  private static final String PROD_001 = "PROD_001";
  private static final String UOM_BASE = "UOM_BASE";
  private static final String INPC_UOM_ID = "inpcUomId";
  private static final String INPM_PRODUCT_ID = "inpmProductId";
  private static final String INPCONVERSIONRATE = "inpconversionrate";

  private AUM_ConversionRate callout;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private Product mockProduct;

  @Mock
  private UOM mockUOM;

  @Mock
  private OBCriteria<UOMConversion> mockCriteria;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    callout = new AUM_ConversionRate();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (obContextStatic != null) {
      obContextStatic.close();
    }
  }
  /**
   * Execute with direct conversion found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithDirectConversionFound() throws Exception {
    // Arrange
    String uomId = UOM_001;
    String productId = PROD_001;
    String productUomId = UOM_BASE;

    when(mockInfo.getStringParameter(INPC_UOM_ID, null)).thenReturn(uomId);
    when(mockInfo.getStringParameter(INPM_PRODUCT_ID, null)).thenReturn(productId);
    when(mockOBDal.get(Product.class, productId)).thenReturn(mockProduct);
    when(mockProduct.getUOM()).thenReturn(mockUOM);
    when(mockUOM.getId()).thenReturn(productUomId);

    UOMConversion mockConversion = mock(UOMConversion.class);
    when(mockConversion.getMultipleRateBy()).thenReturn(new BigDecimal("2.5"));

    List<UOMConversion> conversionList = new ArrayList<>();
    conversionList.add(mockConversion);

    lenient().when(mockOBDal.createCriteria(UOMConversion.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(conversionList);

    // Act
    invokeExecute(callout, mockInfo);

    // Assert
    verify(mockInfo).addResult(INPCONVERSIONRATE, new BigDecimal("2.5"));
  }
  /**
   * Execute with reverse conversion found.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithReverseConversionFound() throws Exception {
    // Arrange
    String uomId = UOM_001;
    String productId = PROD_001;
    String productUomId = UOM_BASE;

    when(mockInfo.getStringParameter(INPC_UOM_ID, null)).thenReturn(uomId);
    when(mockInfo.getStringParameter(INPM_PRODUCT_ID, null)).thenReturn(productId);
    when(mockOBDal.get(Product.class, productId)).thenReturn(mockProduct);
    when(mockProduct.getUOM()).thenReturn(mockUOM);
    when(mockUOM.getId()).thenReturn(productUomId);

    UOMConversion mockReverseConversion = mock(UOMConversion.class);
    when(mockReverseConversion.getDivideRateBy()).thenReturn(new BigDecimal("0.4"));

    List<UOMConversion> emptyList = new ArrayList<>();
    List<UOMConversion> reverseList = new ArrayList<>();
    reverseList.add(mockReverseConversion);

    OBCriteria<UOMConversion> mockCriteria2 = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(UOMConversion.class)).thenReturn(mockCriteria,
        mockCriteria2);
    when(mockCriteria.list()).thenReturn(emptyList);
    when(mockCriteria2.list()).thenReturn(reverseList);

    // Act
    invokeExecute(callout, mockInfo);

    // Assert
    verify(mockInfo).addResult(INPCONVERSIONRATE, new BigDecimal("0.4"));
  }
  /**
   * Execute with no conversion found returns zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNoConversionFoundReturnsZero() throws Exception {
    // Arrange
    String uomId = UOM_001;
    String productId = PROD_001;
    String productUomId = UOM_BASE;

    when(mockInfo.getStringParameter(INPC_UOM_ID, null)).thenReturn(uomId);
    when(mockInfo.getStringParameter(INPM_PRODUCT_ID, null)).thenReturn(productId);
    when(mockOBDal.get(Product.class, productId)).thenReturn(mockProduct);
    when(mockProduct.getUOM()).thenReturn(mockUOM);
    when(mockUOM.getId()).thenReturn(productUomId);

    List<UOMConversion> emptyList = new ArrayList<>();
    OBCriteria<UOMConversion> mockCriteria2 = mock(OBCriteria.class);
    lenient().when(mockOBDal.createCriteria(UOMConversion.class)).thenReturn(mockCriteria,
        mockCriteria2);
    when(mockCriteria.list()).thenReturn(emptyList);
    when(mockCriteria2.list()).thenReturn(emptyList);

    // Act
    invokeExecute(callout, mockInfo);

    // Assert
    verify(mockInfo).addResult(INPCONVERSIONRATE, BigDecimal.ZERO);
  }

  private void invokeExecute(AUM_ConversionRate instance, SimpleCallout.CalloutInfo info)
      throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
    Method method = AUM_ConversionRate.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, info);
  }
}
