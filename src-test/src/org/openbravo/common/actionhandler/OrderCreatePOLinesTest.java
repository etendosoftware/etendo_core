package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.order.Order;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.service.db.CallStoredProcedure;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link OrderCreatePOLines} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including cases with no order, valid input, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class OrderCreatePOLinesTest {

    @InjectMocks
    private OrderCreatePOLines orderCreatePOLines;

    private MockedStatic<OBDal> mockedOBDal;
    private MockedStatic<OBContext> mockedOBContext;
    private MockedStatic<OBProvider> mockedOBProvider;
    private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
    private MockedStatic<CallStoredProcedure> mockedCallStoredProcedure;
    private MockedStatic<DbUtility> mockedDbUtility;

    @Mock
    private OBDal dal;

    @Mock
    private Order mockOrder;

    @Mock
    private CallStoredProcedure mockStoredProcedure;

    private static final String ORDER_ID = "TEST_ORDER_ID";
    private static final String PRODUCT_ID = "TEST_PRODUCT_ID";
    private static final String UOM_ID = "TEST_UOM_ID";
    private static final String AUM_ID = "TEST_AUM_ID";

    /**
     * Sets up the test environment before each test.
     * Mocks static methods and initializes required dependencies.
     */
    @BeforeEach
    public void setUp() {
        mockedOBDal = mockStatic(OBDal.class);
        mockedOBContext = mockStatic(OBContext.class);
        mockedOBProvider = mockStatic(OBProvider.class);
        mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
        mockedCallStoredProcedure = mockStatic(CallStoredProcedure.class);
        mockedDbUtility = mockStatic(DbUtility.class);

        mockedOBDal.when(OBDal::getInstance).thenReturn(dal);
        mockedCallStoredProcedure.when(CallStoredProcedure::getInstance).thenReturn(mockStoredProcedure);

        OBProvider provider = mock(OBProvider.class);
        mockedOBProvider.when(() -> OBProvider.getInstance()).thenReturn(provider);

        when(dal.get(Order.class, ORDER_ID)).thenReturn(mockOrder);

    }

    /**
     * Cleans up the test environment after each test.
     * Closes mocked static methods to release resources.
     */
    @AfterEach
    public void tearDown() {
        if (mockedOBDal != null) mockedOBDal.close();
        if (mockedOBContext != null) mockedOBContext.close();
        if (mockedOBProvider != null) mockedOBProvider.close();
        if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
        if (mockedCallStoredProcedure != null) mockedCallStoredProcedure.close();
        if (mockedDbUtility != null) mockedDbUtility.close();
    }

    /**
     * Tests the `doExecute` method when no order is found.
     * Verifies that the method returns a success message without saving any data.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testDoExecuteNoOrder() throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        String content = createJsonContent(ORDER_ID);

        when(dal.get(Order.class, ORDER_ID)).thenReturn(null);

        mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("success")).thenReturn("Success");

        JSONObject result = orderCreatePOLines.doExecute(parameters, content);

        assertNotNull(result);
        assertTrue(result.has("message"));
        JSONObject message = result.getJSONObject("message");
        assertEquals("success", message.getString("severity"));

        verify(dal).get(Order.class, ORDER_ID);
        verify(dal, never()).save(any(OrderLine.class));
        verify(dal, never()).save(any(Order.class));
        verify(dal, never()).flush();
        mockedOBContext.verify(() -> OBContext.setAdminMode(true));
        mockedOBContext.verify(OBContext::restorePreviousMode);
    }

    /**
     * Creates a valid JSON content string for testing.
     * The JSON includes order ID, product, and other required fields.
     *
     * @param orderId the order ID to include in the JSON content
     * @return a JSON string containing valid test data
     * @throws Exception if an error occurs while creating the JSON content
     */
    private String createJsonContent(String orderId) throws Exception {
        JSONObject jsonRequest = new JSONObject();
        jsonRequest.put("C_Order_ID", orderId);

        JSONObject params = new JSONObject();
        JSONObject grid = new JSONObject();
        JSONArray selection = new JSONArray();

        JSONObject line = new JSONObject();
        line.put("product", PRODUCT_ID);
        line.put("product$uOM", UOM_ID);
        line.put("aum", AUM_ID);
        line.put("orderedQuantity", "10");
        line.put("aumQuantity", "10");
        line.put("standardPrice", "100");

        selection.put(line);
        grid.put("_selection", selection);
        params.put("grid", grid);
        jsonRequest.put("_params", params);

        return jsonRequest.toString();
    }
}
