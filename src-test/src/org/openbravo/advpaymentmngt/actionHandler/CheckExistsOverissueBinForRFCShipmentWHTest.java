package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
import static org.openbravo.test.base.mock.MockitoStaticMockUtils.mockStaticSafely;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openbravo.advpaymentmngt.TestConstants;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;

/**
 * Test class for CheckExistsOverissueBinForRFCShipmentWH action handler.
 * Tests the functionality of checking overissue bins in warehouses for RFC shipments.
 */
public class CheckExistsOverissueBinForRFCShipmentWHTest {

  /**
   * Rule for handling expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MockedStatic<OBDal> mockedOBDal;
  private CheckExistsOverissueBinForRFCShipmentWH handler;
  private AutoCloseable mocks;

  @Mock
  private OBDal mockDal;
  @Mock
  private OBQuery<Locator> mockQuery;

  /**
   * Sets up the test environment before each test.
   * Initializes mocks and configures default behavior.
   *
   * @throws Exception
   *     if there's an error during setup
   */
  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    handler = new CheckExistsOverissueBinForRFCShipmentWH();

    // Initialize static mock for OBDal
    mockedOBDal = mockStaticSafely(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

    // Setup default query mock behavior
    when(mockDal.createQuery(eq(Locator.class), anyString())).thenReturn(mockQuery);
    when(mockQuery.setNamedParameter(anyString(), any())).thenReturn(mockQuery);
    when(mockQuery.setMaxResult(1)).thenReturn(mockQuery);
  }

  /**
   * Cleans up the test environment after each test.
   *
   * @throws Exception
   *     if there's an error during cleanup
   */
  @After
  public void tearDown() throws Exception {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Tests the execute method when an overissue bin exists.
   * Verifies that the correct locator information is returned.
   *
   * @throws Exception
   *     if there's an error during test execution
   */
  @Test
  public void testExecuteWithExistingOverissueBin() throws Exception {
    // Given
    String warehouseId = "TEST_WAREHOUSE";
    String locatorId = "TEST_LOCATOR";
    String locatorIdentifier = "Test Locator";

    Locator mockLocator = createMockLocator(locatorId, locatorIdentifier);
    when(mockQuery.uniqueResult()).thenReturn(mockLocator);

    String jsonInput = String.format("{\"warehouseId\": \"%s\"}", warehouseId);

    // When
    JSONObject result = handler.execute(null, jsonInput);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("Locator ID should match", locatorId, result.getString(TestConstants.OVERISSUE_BIN));
    assertEquals("Locator identifier should match", locatorIdentifier,
        result.getString(TestConstants.STORAGE_BIN_IDENTIFIER));
  }

  /**
   * Tests the execute method when no overissue bin exists.
   * Verifies that empty strings are returned for bin information.
   *
   * @throws Exception
   *     if there's an error during test execution
   */
  @Test
  public void testExecuteWithNoOverissueBin() throws Exception {
    // Given
    String warehouseId = "TEST_WAREHOUSE";
    when(mockQuery.uniqueResult()).thenReturn(null);

    String jsonInput = String.format("{\"warehouseId\": \"%s\"}", warehouseId);

    // When
    JSONObject result = handler.execute(null, jsonInput);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("Overissue bin should be empty", "", result.getString(TestConstants.OVERISSUE_BIN));
    assertEquals("Storage bin identifier should be empty", "", result.getString(TestConstants.STORAGE_BIN_IDENTIFIER));
  }

  /**
   * Tests the execute method with invalid JSON input.
   * Verifies that appropriate error information is returned.
   *
   * @throws Exception
   *     if there's an error during test execution
   */
  @Test
  public void testExecuteWithInvalidJSON() throws Exception {
    // Given
    String invalidJson = "invalid json";

    // When
    JSONObject result = handler.execute(null, invalidJson);

    // Then
    assertNotNull(TestConstants.RESULT_NOT_NULL_MESSAGE, result);
    assertEquals("Overissue bin should be empty", "", result.getString(TestConstants.OVERISSUE_BIN));
    assertEquals("Storage bin identifier should be empty", "", result.getString(TestConstants.STORAGE_BIN_IDENTIFIER));
    assertNotNull("Error message should exist", result.getJSONObject("message"));
    assertEquals("Error severity should be error", "error", result.getJSONObject("message").getString("severity"));
  }

  /**
   * Creates a mock Locator with the specified ID and identifier.
   * Sets up the locator with a warehouse and inventory status.
   *
   * @param locatorId
   *     the ID for the mock locator
   * @param identifier
   *     the identifier for the mock locator
   * @return the configured mock Locator
   */
  private Locator createMockLocator(String locatorId, String identifier) {
    Locator mockLocator = mock(Locator.class);
    when(mockLocator.getId()).thenReturn(locatorId);
    when(mockLocator.getIdentifier()).thenReturn(identifier);

    Warehouse mockWarehouse = mock(Warehouse.class);
    InventoryStatus mockStatus = mock(InventoryStatus.class);
    when(mockStatus.isOverissue()).thenReturn(true);

    when(mockLocator.getWarehouse()).thenReturn(mockWarehouse);
    when(mockLocator.getInventoryStatus()).thenReturn(mockStatus);

    return mockLocator;
  }
}
