package org.openbravo.materialmgmt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Characteristic;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.common.plm.ProductCharacteristicValue;

/**
 * Test class for CharacteristicsUtils.
 * <p>
 * This class uses Mockito to mock dependencies and verify interactions.
 * It includes unit tests for the methods in the CharacteristicsUtils class.
 * <p>
 * Annotations:
 * - @RunWith(MockitoJUnitRunner.class): This annotation is used to specify that the test runner should be MockitoJUnitRunner,
 * which initializes mocks and handles the lifecycle of the test class.
 */
@RunWith(MockitoJUnitRunner.class)
public class CharacteristicsUtilsTest {

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBProvider> mockedOBProvider;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<ProductCharacteristicValue> mockPCVCriteria;

  @Mock
  private OBCriteria<ProductCharacteristic> mockPCCriteria;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private Product mockProduct;

  @Mock
  private Characteristic mockCharacteristic;

  @Mock
  private CharacteristicValue mockCharacteristicValue;

  @Mock
  private ProductCharacteristicValue mockProductCharacteristicValue;

  @Mock
  private ProductCharacteristic mockProductCharacteristic;

  @Mock
  private Organization mockOrganization;

  /**
   * Sets up the mock objects and static methods before each test.
   */
  @Before
  public void setUp() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBProvider = mockStatic(OBProvider.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProvider);

    when(mockOBDal.createCriteria(ProductCharacteristicValue.class)).thenReturn(mockPCVCriteria);
    when(mockOBDal.createCriteria(ProductCharacteristic.class)).thenReturn(mockPCCriteria);

  when(mockPCVCriteria.addEqual(anyString(), any())).thenReturn(mockPCVCriteria);
  when(mockPCVCriteria.setMaxResults(anyInt())).thenReturn(mockPCVCriteria);

  when(mockPCCriteria.addEqual(anyString(), any())).thenReturn(mockPCCriteria);
  when(mockPCCriteria.setMaxResults(anyInt())).thenReturn(mockPCCriteria);

    when(mockProduct.getOrganization()).thenReturn(mockOrganization);
    List<ProductCharacteristic> pcList = new ArrayList<>();
    when(mockProduct.getProductCharacteristicList()).thenReturn(pcList);

    when(mockCharacteristicValue.getCharacteristic()).thenReturn(mockCharacteristic);

    when(mockOBProvider.get(ProductCharacteristicValue.class)).thenReturn(mockProductCharacteristicValue);
    when(mockOBProvider.get(ProductCharacteristic.class)).thenReturn(mockProductCharacteristic);
  }

  /**
   * Cleans up the static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
  }

  /**
   * Tests {@link CharacteristicsUtils#getCharacteristicValue(Product, Characteristic)}.
   * <p>
   * Ensures the correct characteristic value is retrieved when a match is found.
   */
  @Test
  public void testGetCharacteristicValueWhenFound() {
    // GIVEN
    when(mockPCVCriteria.uniqueResult()).thenReturn(mockProductCharacteristicValue);
    when(mockProductCharacteristicValue.getCharacteristicValue()).thenReturn(mockCharacteristicValue);

    // WHEN
    CharacteristicValue result = CharacteristicsUtils.getCharacteristicValue(mockProduct, mockCharacteristic);

    // THEN
    assertEquals(mockCharacteristicValue, result);
    verify(mockOBDal).createCriteria(ProductCharacteristicValue.class);
  verify(mockPCVCriteria, times(2)).addEqual(anyString(), any());
    verify(mockPCVCriteria).setMaxResults(1);
    verify(mockPCVCriteria).uniqueResult();
  }

  /**
   * Tests {@link CharacteristicsUtils#getCharacteristicValue(Product, Characteristic)}.
   * <p>
   * Ensures null is returned when no characteristic value is found.
   */
  @Test
  public void testGetCharacteristicValueWhenNotFound() {
    // GIVEN
    when(mockPCVCriteria.uniqueResult()).thenReturn(null);

    // WHEN
    CharacteristicValue result = CharacteristicsUtils.getCharacteristicValue(mockProduct, mockCharacteristic);

    // THEN
    assertNull(result);
    verify(mockOBDal).createCriteria(ProductCharacteristicValue.class);
  verify(mockPCVCriteria, times(2)).addEqual(anyString(), any());
    verify(mockPCVCriteria).setMaxResults(1);
    verify(mockPCVCriteria).uniqueResult();
  }

  /**
   * Tests {@link CharacteristicsUtils#setCharacteristicValue(Product, CharacteristicValue)}.
   * <p>
   * Ensures the characteristic value is updated when an existing product characteristic value is present.
   */
  @Test
  public void testSetCharacteristicValueWhenExistingProductCharacteristicValue() {
    // GIVEN
    when(mockPCVCriteria.uniqueResult()).thenReturn(mockProductCharacteristicValue);
    when(mockPCCriteria.count()).thenReturn(1);

    // WHEN
    ProductCharacteristicValue result = CharacteristicsUtils.setCharacteristicValue(mockProduct,
        mockCharacteristicValue);

    // THEN
    assertEquals(mockProductCharacteristicValue, result);
    verify(mockProductCharacteristicValue).setCharacteristicValue(mockCharacteristicValue);
    verify(mockOBDal).save(mockProductCharacteristicValue);
    verify(mockPCCriteria).count();
    verify(mockProductCharacteristic, never()).setProduct(any(Product.class));
  }

  /**
   * Tests {@link CharacteristicsUtils#setCharacteristicValue(Product, CharacteristicValue)}.
   * <p>
   * Ensures a new characteristic value is created when an existing product characteristic is present.
   */
  @Test
  public void testSetCharacteristicValueWithExistingProductCharacteristic() {
    // GIVEN
    when(mockPCVCriteria.uniqueResult()).thenReturn(null);
    when(mockPCCriteria.count()).thenReturn(1);

    // WHEN
    ProductCharacteristicValue result = CharacteristicsUtils.setCharacteristicValue(mockProduct,
        mockCharacteristicValue);

    // THEN
    assertEquals(mockProductCharacteristicValue, result);

    // Verify ProductCharacteristicValue creation
    verify(mockOBProvider).get(ProductCharacteristicValue.class);
    verify(mockProductCharacteristicValue).setCharacteristic(mockCharacteristic);
    verify(mockProductCharacteristicValue).setOrganization(mockOrganization);
    verify(mockProductCharacteristicValue).setProduct(mockProduct);
    verify(mockProductCharacteristicValue).setCharacteristicValue(mockCharacteristicValue);
    verify(mockOBDal).save(mockProductCharacteristicValue);

    // Verify ProductCharacteristic NOT created as it already exists
    verify(mockProductCharacteristic, never()).setOrganization(any(Organization.class));
    verify(mockProductCharacteristic, never()).setProduct(any(Product.class));
  }

  /**
   * Tests sequence number calculation in
   * {@link CharacteristicsUtils#setCharacteristicValue(Product, CharacteristicValue)}.
   * <p>
   * Ensures the sequence number is correctly calculated based on existing product characteristics.
   */
  @Test
  public void testSetCharacteristicValueSequenceNumberCalculation() {
    // GIVEN
    when(mockPCVCriteria.uniqueResult()).thenReturn(null); // No existing ProductCharacteristicValue
    when(mockPCCriteria.count()).thenReturn(0); // No existing ProductCharacteristic

    // Add some existing ProductCharacteristic objects to the list
    List<ProductCharacteristic> pcList = new ArrayList<>();
    pcList.add(mock(ProductCharacteristic.class));
    pcList.add(mock(ProductCharacteristic.class));
    when(mockProduct.getProductCharacteristicList()).thenReturn(pcList);

    // WHEN
    CharacteristicsUtils.setCharacteristicValue(mockProduct, mockCharacteristicValue);

    // THEN
    // The sequence number should be (2 + 1) * 10 = 30
    verify(mockProductCharacteristic).setSequenceNumber(30L);
  }
}
