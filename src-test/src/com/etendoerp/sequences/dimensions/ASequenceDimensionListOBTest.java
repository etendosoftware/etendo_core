package com.etendoerp.sequences.dimensions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.exception.OBException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.datamodel.Column;
import org.openbravo.model.ad.domain.DimensionsList;
import org.openbravo.model.ad.domain.Reference;
import org.openbravo.test.base.OBBaseTest;

import com.etendoerp.sequences.parameters.SequenceParameter;
import com.etendoerp.sequences.parameters.SequenceParameterList;

/**
 * Test class for ASequenceDimensionList that extends OBBaseTest to properly
 * handle Openbravo context in tests.
 */
@RunWith(MockitoJUnitRunner.class)
public class ASequenceDimensionListOBTest extends OBBaseTest {

  private static final String TEST_KEY = "TEST_KEY";

  @Mock
  private Column mockColumn;
  @Mock
  private Reference mockReference;
  @Mock
  private SequenceDimension mockDimension;
  private ASequenceDimensionList<String> sequenceDimensionList;

  /**
   * Sets up the test environment before each test.
   * Initializes OBContext and Mockito annotations.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Override
  @Before
  public void setUp() throws Exception {
    // Initialize OBContext with OBBaseTest's setUp
    super.setUp();

    // Use MockitoAnnotations to initialize mocks
    MockitoAnnotations.openMocks(this);

    // Set up the mock column and reference
    when(mockColumn.getReferenceSearchKey()).thenReturn(mockReference);

    // Create the sequence dimension list with mocked components
    sequenceDimensionList = spy(new ConcreteSequenceDimensionList(mockColumn));

  }

  /**
   * Tests setting and getting a dimension.
   * Verifies that the dimension is correctly set and retrieved.
   */
  @Test
  public void testSetAndGetDimension() {
    // GIVEN
    String key = TEST_KEY;

    // WHEN
    sequenceDimensionList.setDimension(key, mockDimension);
    SequenceDimension result = sequenceDimensionList.getDimension(key);

    // THEN
    assertNotNull(result);
    assertEquals(mockDimension, result);
  }

  /**
   * Tests the isEmpty method.
   * Verifies that the list is initially empty and not empty after adding a dimension.
   */
  @Test
  public void testIsEmpty() {
    // GIVEN - A new list

    // WHEN
    boolean isEmpty = sequenceDimensionList.isEmpty();

    // THEN
    assertTrue(isEmpty);

    // Add a dimension
    sequenceDimensionList.setDimension(TEST_KEY, mockDimension);
    assertFalse(sequenceDimensionList.isEmpty());
  }

  /**
   * Tests the validateParametersList method.
   * Verifies that an OBException is thrown when a required parameter is missing.
   *
   * @throws OBException
   *     if a required parameter is missing
   */
  @Test(expected = OBException.class)
  public void testValidateParametersListThrowsExceptionForMissingParameter() {
    // We need to be in admin mode to use OBMessageUtils.getI18NMessage
    OBContext.setAdminMode(true);
    try {
      // GIVEN
      SequenceParameterList parameterList = mock(SequenceParameterList.class);
      Map<String, SequenceParameter> parameterMap = new HashMap<>();

      when(parameterList.getParameterMap()).thenReturn(parameterMap);

      sequenceDimensionList.setDimension(TEST_KEY, mockDimension);
      when(mockDimension.getPropertyName()).thenReturn("MISSING_PROPERTY");

      // WHEN - This should throw an OBException because the parameter is missing
      sequenceDimensionList.validateParametersList(parameterList);

      // THEN - The expected=OBException.class annotation handles the verification
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Tests the loadReference method.
   * Verifies that the reference is correctly loaded from the column.
   */
  @Test
  public void testLoadReference() {
    // GIVEN
    when(mockColumn.getReferenceSearchKey()).thenReturn(null);
    when(mockColumn.getReference()).thenReturn(mockReference);

    // WHEN
    sequenceDimensionList.loadReference();

    // THEN
    verify(mockColumn, atLeastOnce()).getReference();
  }

  /**
   * A concrete implementation of ASequenceDimensionList for testing purposes
   */
  private static class ConcreteSequenceDimensionList extends ASequenceDimensionList<String> {
    public ConcreteSequenceDimensionList(Column column) {
      super(column);
    }

    @Override
    public void setDimensionTransformingKey(String key, SequenceDimension dimension) {
      setDimension(key.toUpperCase(), dimension);
    }

    @Override
    public SequenceDimension getDimensionTransformingKey(String key) {
      return getDimension(key.toUpperCase());
    }

    @Override
    public void handleDimensionListEntity(DimensionsList dimensionsList) {
      // Simulate handling the entity
    }

    // Override to avoid database call during tests
    @Override
    public void loadDimensionsMap() {
      // Do nothing in tests - dimensions will be added manually
    }
  }
}
