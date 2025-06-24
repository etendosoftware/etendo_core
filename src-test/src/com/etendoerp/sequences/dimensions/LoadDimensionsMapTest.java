package com.etendoerp.sequences.dimensions;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;
import org.openbravo.model.ad.domain.Reference;

import com.etendoerp.sequences.SequenceDatabaseUtils;

/**
 * Unit tests for the LoadDimensionsMap class.
 * Tests the functionality related to loading dimensions map.
 */
@RunWith(MockitoJUnitRunner.class)
public class LoadDimensionsMapTest {

  @Mock
  private Column mockColumn;
  @Mock
  private Reference mockReference;
  private TestSequenceDimensionList sequenceDimensionList;

  /**
   * Sets up the test environment before each test.
   * Initializes Mockito annotations and configures common behavior.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {

    MockitoAnnotations.openMocks(this);

    when(mockColumn.getReferenceSearchKey()).thenReturn(mockReference);
  }

  /**
   * Tests the loadDimensionsMap method with a null reference.
   * Verifies that no interactions occur with the reference and no dimensions are handled.
   */
  @Test
  public void testLoadDimensionsMapWithNullReference() {
    // GIVEN
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(null);

    sequenceDimensionList = new TestSequenceDimensionList(mockColumn);

    // WHEN
    sequenceDimensionList.loadDimensionsMap();

    // THEN
    assertEquals(0, sequenceDimensionList.getHandleDimensionListEntityCalls());

    verifyNoInteractions(mockReference);
  }

  /**
   * Tests the loadDimensionsMap method's exception handling.
   * Verifies that an exception is correctly propagated when thrown by SequenceDatabaseUtils.
   */
  @Test
  public void testLoadDimensionsMapException() {
    // GIVEN
    try (var mockedStatic = mockStatic(SequenceDatabaseUtils.class)) {
      mockedStatic.when(() -> SequenceDatabaseUtils.getDimensionLists(mockReference)).thenThrow(
          new RuntimeException("Test exception"));

      // WHEN/THEN
      RuntimeException exception = assertThrows(RuntimeException.class, () -> sequenceDimensionList = new TestSequenceDimensionList(mockColumn));

      // Verify exception message
      assertEquals("Test exception", exception.getMessage());

      // Verify interactions
      mockedStatic.verify(() -> SequenceDatabaseUtils.getDimensionLists(mockReference));
    }
  }

  /**
   * A test implementation of the ASequenceDimensionList class for unit testing.
   * This class provides specific behavior for handling dimension lists and transforming keys.
   */
  private static class TestSequenceDimensionList extends ASequenceDimensionList<String> {
    private int handleDimensionListEntityCalls = 0;

    /**
     * Constructs a new TestSequenceDimensionList with the specified column.
     *
     * @param column
     *     the column associated with the dimension list
     */
    public TestSequenceDimensionList(Column column) {
      super(column);
    }

    /**
     * Sets a dimension in the list, transforming the key to uppercase.
     *
     * @param key
     *     the key for the dimension
     * @param dimension
     *     the dimension to set
     */
    @Override
    public void setDimensionTransformingKey(String key, SequenceDimension dimension) {
      setDimension(key.toUpperCase(), dimension);
    }

    /**
     * Retrieves a dimension from the list, transforming the key to uppercase.
     *
     * @param key
     *     the key for the dimension
     * @return the dimension associated with the transformed key
     */
    @Override
    public SequenceDimension getDimensionTransformingKey(String key) {
      return getDimension(key.toUpperCase());
    }

    /**
     * Handles the specified dimension list entity.
     * Increments the call count each time this method is invoked.
     *
     * @param dimensionsList
     *     the dimensions list entity to handle
     */
    @Override
    public void handleDimensionListEntity(DimensionsList dimensionsList) {
      handleDimensionListEntityCalls++;
    }

    /**
     * Returns the number of times the handleDimensionListEntity method has been called.
     *
     * @return the call count for handleDimensionListEntity
     */
    public int getHandleDimensionListEntityCalls() {
      return handleDimensionListEntityCalls;
    }

  }
}
