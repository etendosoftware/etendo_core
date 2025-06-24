package com.etendoerp.sequences.dimensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;

/**
 * Unit tests for the SequenceDimension class.
 * Tests the functionality related to sequence dimensions.
 */
@RunWith(MockitoJUnitRunner.class)
public class SequenceDimensionTest {

  private static final String TEST_COLUMN = "test_column";

  @Mock
  private DimensionsList mockDimensionsList;

  @Mock
  private Column mockColumn;

  @Mock
  private Entity mockEntity;

  @Mock
  private Property mockProperty;

  @Mock
  private ModelProvider mockModelProvider;

  /**
   * Sets up the test environment before each test.
   * Initializes Mockito annotations.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize Mockito annotations
    MockitoAnnotations.openMocks(this);
  }

  /**
   * Tests the constructor with a null DimensionsList.
   * Verifies that the properties are initialized to null.
   */
  @Test
  public void testConstructorWithNullDimensionsList() {
    // GIVEN

    // WHEN
    SequenceDimension sequenceDimension = new SequenceDimension(null);

    // THEN
    assertNull(sequenceDimension.getOriginalDimensionList());
    assertNull(sequenceDimension.getProperty());
  }

  /**
   * Tests the initialization of the property.
   * Verifies that the property is correctly set based on the column name.
   */
  @Test
  public void testInitializeProperty() {
    try (var mockedStatic = mockStatic(ModelProvider.class)) {
      // GIVEN
      final String columnName = TEST_COLUMN;

      // Mock ModelProvider behavior
      mockedStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(SequenceDimension.AD_SEQUENCE_ENTITY)).thenReturn(mockEntity);

      // Mock Column behavior
      when(mockDimensionsList.getColumn()).thenReturn(mockColumn);
      when(mockColumn.getDBColumnName()).thenReturn(columnName);

      // Mock Entity behavior for getPropertyByColumnName
      when(mockEntity.getPropertyByColumnName(columnName, false)).thenReturn(mockProperty);

      // WHEN
      SequenceDimension sequenceDimension = new SequenceDimension(mockDimensionsList);

      // THEN
      assertNotNull(sequenceDimension.getProperty());
      assertEquals(mockProperty, sequenceDimension.getProperty());
      assertEquals(mockDimensionsList, sequenceDimension.getOriginalDimensionList());

      // Verify interactions
      mockedStatic.verify(ModelProvider::getInstance);
      verify(mockModelProvider).getEntity(SequenceDimension.AD_SEQUENCE_ENTITY);
      verify(mockDimensionsList).getColumn();
      verify(mockColumn).getDBColumnName();
      verify(mockEntity).getPropertyByColumnName(columnName, false);
    }
  }

  /**
   * Tests the retrieval of the property name.
   * Verifies that the correct property name is returned.
   */
  @Test
  public void testGetPropertyName() {
    try (var mockedStatic = mockStatic(ModelProvider.class)) {
      // GIVEN
      final String columnName = TEST_COLUMN;
      final String propertyName = "testProperty";

      // Mock ModelProvider behavior
      mockedStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(SequenceDimension.AD_SEQUENCE_ENTITY)).thenReturn(mockEntity);

      // Mock Column behavior
      when(mockDimensionsList.getColumn()).thenReturn(mockColumn);
      when(mockColumn.getDBColumnName()).thenReturn(columnName);

      // Mock Entity behavior for getPropertyByColumnName
      when(mockEntity.getPropertyByColumnName(columnName, false)).thenReturn(mockProperty);
      when(mockProperty.getName()).thenReturn(propertyName);

      // WHEN
      SequenceDimension sequenceDimension = new SequenceDimension(mockDimensionsList);
      String resultPropertyName = sequenceDimension.getPropertyName();

      // THEN
      assertEquals(propertyName, resultPropertyName);

      // Verify interactions
      verify(mockProperty).getName();
    }
  }

  /**
   * Tests the handling of exceptions thrown by ModelProvider.
   * Verifies that the exception is correctly propagated.
   */
  @Test
  public void testModelProviderException() {
    try (var mockedStatic = mockStatic(ModelProvider.class)) {
      // GIVEN
      final String errorMessage = "Test ModelProvider exception";

      // Mock ModelProvider to throw exception
      mockedStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(SequenceDimension.AD_SEQUENCE_ENTITY)).thenThrow(
          new RuntimeException(errorMessage));

      // WHEN/THEN
      RuntimeException exception = assertThrows(RuntimeException.class, () -> new SequenceDimension(mockDimensionsList));

      // Verify exception message
      assertEquals(errorMessage, exception.getMessage());

      // Verify interactions
      mockedStatic.verify(ModelProvider::getInstance);
      verify(mockModelProvider).getEntity(SequenceDimension.AD_SEQUENCE_ENTITY);
    }
  }

  /**
   * Tests the behavior when the property is not found by column name.
   * Verifies that the property is set to null.
   */
  @Test
  public void testPropertyByColumnNameNotFound() {
    try (var mockedStatic = mockStatic(ModelProvider.class)) {
      // GIVEN
      final String columnName = "nonexistent_column";

      // Mock ModelProvider behavior
      mockedStatic.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntity(SequenceDimension.AD_SEQUENCE_ENTITY)).thenReturn(mockEntity);

      // Mock Column behavior
      when(mockDimensionsList.getColumn()).thenReturn(mockColumn);
      when(mockColumn.getDBColumnName()).thenReturn(columnName);

      // Mock Entity to return null for property
      when(mockEntity.getPropertyByColumnName(columnName, false)).thenReturn(null);

      // WHEN
      SequenceDimension sequenceDimension = new SequenceDimension(mockDimensionsList);

      // THEN
      assertNull(sequenceDimension.getProperty());
      assertEquals(mockDimensionsList, sequenceDimension.getOriginalDimensionList());

      // Verify interactions
      verify(mockEntity).getPropertyByColumnName(columnName, false);
    }
  }

  /**
   * Tests the retrieval of the property name when the property is null.
   * Verifies that a NullPointerException is thrown.
   *
   * @throws NullPointerException
   *     if the property is null
   */
  @Test(expected = NullPointerException.class)
  public void testGetPropertyNameWithNullProperty() {
    // GIVEN - Manually create a SequenceDimension with null property
    SequenceDimension sequenceDimension = new SequenceDimension(null);

    // WHEN - This should throw NullPointerException
    sequenceDimension.getPropertyName();

    // THEN - Expected exception handles verification
  }
}
