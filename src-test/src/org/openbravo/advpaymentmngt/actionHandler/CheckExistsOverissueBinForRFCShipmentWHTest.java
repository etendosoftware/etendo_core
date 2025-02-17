package org.openbravo.advpaymentmngt.actionHandler;

import static org.junit.Assert.assertEquals;
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
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.onhandquantity.InventoryStatus;

public class CheckExistsOverissueBinForRFCShipmentWHTest extends WeldBaseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private MockedStatic<OBDal> mockedOBDal;
    private CheckExistsOverissueBinForRFCShipmentWH handler;
    private AutoCloseable mocks;

    @Mock
    private OBDal mockDal;
    @Mock
    private OBQuery<Locator> mockQuery;

    @Before
    public void setUp() throws Exception {
        mocks = MockitoAnnotations.openMocks(this);
        handler = new CheckExistsOverissueBinForRFCShipmentWH();

        // Initialize static mock for OBDal
        mockedOBDal = Mockito.mockStatic(OBDal.class);
        mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

        // Setup default query mock behavior
        when(mockDal.createQuery(eq(Locator.class), anyString())).thenReturn(mockQuery);
        when(mockQuery.setNamedParameter(anyString(), any())).thenReturn(mockQuery);
        when(mockQuery.setMaxResult(1)).thenReturn(mockQuery);
    }

    @After
    public void tearDown() throws Exception {
        if (mockedOBDal != null) {
            mockedOBDal.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    public void testExecute_WithExistingOverissueBin() throws Exception {
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
        assertNotNull("Result should not be null", result);
        assertEquals("Locator ID should match", locatorId, result.getString("overissueBin"));
        assertEquals("Locator identifier should match", locatorIdentifier,
            result.getString("storageBin$_identifier"));
    }

    @Test
    public void testExecute_WithNoOverissueBin() throws Exception {
        // Given
        String warehouseId = "TEST_WAREHOUSE";
        when(mockQuery.uniqueResult()).thenReturn(null);

        String jsonInput = String.format("{\"warehouseId\": \"%s\"}", warehouseId);

        // When
        JSONObject result = handler.execute(null, jsonInput);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Overissue bin should be empty", "", result.getString("overissueBin"));
        assertEquals("Storage bin identifier should be empty", "",
            result.getString("storageBin$_identifier"));
    }

    @Test
    public void testExecute_WithInvalidJSON() throws Exception {
        // Given
        String invalidJson = "invalid json";

        // When
        JSONObject result = handler.execute(null, invalidJson);

        // Then
        assertNotNull("Result should not be null", result);
        assertEquals("Overissue bin should be empty", "", result.getString("overissueBin"));
        assertEquals("Storage bin identifier should be empty", "",
            result.getString("storageBin$_identifier"));
        assertNotNull("Error message should exist", result.getJSONObject("message"));
        assertEquals("Error severity should be error", "error",
            result.getJSONObject("message").getString("severity"));
    }

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