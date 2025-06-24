package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.common.plm.ProductServiceLinked;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link RelateProductCatTaxToServiceProduct} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including valid selections, no selections, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class RelateProductCatTaxToServiceProductTest {

  private RelateProductCatTaxToServiceProduct handler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private Product mockProduct;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrg;

  @Mock
  private ProductCategory mockProductCategory;

  @Mock
  private TaxCategory mockTaxCategory;

  @Mock
  private ProductServiceLinked mockProductServiceLinked;

  @Mock
  private OBProvider mockOBProvider;

  private MockedStatic<OBDal> staticOBDal;
  private MockedStatic<OBContext> staticOBContext;
  private MockedStatic<OBMessageUtils> staticOBMessageUtils;
  private MockedStatic<OBProvider> staticOBProvider;
  private MockedStatic<DbUtility> staticDbUtility;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes required dependencies.
   */
  @BeforeEach
  public void setUp() {
    handler = new RelateProductCatTaxToServiceProduct();

    staticOBDal = mockStatic(OBDal.class);
    staticOBContext = mockStatic(OBContext.class);
    staticOBMessageUtils = mockStatic(OBMessageUtils.class);
    staticOBProvider = mockStatic(OBProvider.class);
    staticDbUtility = mockStatic(DbUtility.class);

    staticOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    staticOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProvider);

  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (staticOBDal != null) staticOBDal.close();
    if (staticOBContext != null) staticOBContext.close();
    if (staticOBMessageUtils != null) staticOBMessageUtils.close();
    if (staticOBProvider != null) staticOBProvider.close();
    if (staticDbUtility != null) staticDbUtility.close();
  }

  /**
   * Tests the `doExecute` method with valid selections.
   * Verifies that the method processes the input correctly and creates the expected entities.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithValidSelections() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createValidJsonContent();

    when(mockOBDal.getProxy(eq(Product.class), anyString())).thenReturn(mockProduct);
    when(mockOBDal.getProxy(eq(Client.class), anyString())).thenReturn(mockClient);
    when(mockOBDal.getProxy(eq(Organization.class), anyString())).thenReturn(mockOrg);
    when(mockOBDal.getProxy(eq(ProductCategory.class), anyString())).thenReturn(mockProductCategory);
    when(mockOBDal.getProxy(eq(TaxCategory.class), anyString())).thenReturn(mockTaxCategory);
    when(mockOBProvider.get(ProductServiceLinked.class)).thenReturn(mockProductServiceLinked);
    when(mockOBDal.getSession()).thenReturn(mockSession);

    staticOBMessageUtils.when(() -> OBMessageUtils.messageBD("Success")).thenReturn("Successful operation");

    doNothing().when(mockOBDal).flush();
    doNothing().when(mockSession).clear();
    doNothing().when(mockOBDal).save(any(ProductServiceLinked.class));

    JSONObject result = handler.doExecute(parameters, content);

    assertAll("Verify operation result", () -> assertNotNull(result, ActionHandlerTestConstants.RESULT_NOT_NULL),
        () -> assertTrue(result.has(ActionHandlerTestConstants.MESSAGE), ActionHandlerTestConstants.RESULT_MESSAGE_FIELD),
        () -> assertEquals("success", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY),
            "The severity should be 'success'"),
        () -> assertEquals("Successful operation", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.TITLE),
            "The title should be 'Successful operation'"));

    staticOBContext.verify(() -> OBContext.setAdminMode(true));
    staticOBContext.verify(OBContext::restorePreviousMode);
    verify(mockOBDal, times(1)).flush();

    verify(mockOBDal, times(2)).save(any(ProductServiceLinked.class));
  }

  /**
   * Tests the `doExecute` method when no selections are provided.
   * Verifies that the method returns an error message indicating no selection.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteNoSelections() throws Exception {
    staticOBMessageUtils.when(() -> OBMessageUtils.messageBD("NotSelected")).thenReturn(ActionHandlerTestConstants.NO_SELECTION);

    JSONObject expectedResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(ActionHandlerTestConstants.SEVERITY, ActionHandlerTestConstants.ERROR);
    message.put(ActionHandlerTestConstants.TITLE, ActionHandlerTestConstants.NO_SELECTION);
    expectedResult.put(ActionHandlerTestConstants.MESSAGE, message);

    assertAll("Verify operation result with no selections",
        () -> assertNotNull(expectedResult, ActionHandlerTestConstants.RESULT_NOT_NULL),
        () -> assertTrue(expectedResult.has(ActionHandlerTestConstants.MESSAGE), ActionHandlerTestConstants.RESULT_MESSAGE_FIELD),
        () -> assertEquals(ActionHandlerTestConstants.ERROR, expectedResult.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY),
            "The severity should be 'error'"),
        () -> assertEquals(ActionHandlerTestConstants.NO_SELECTION, expectedResult.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.TITLE),
            "The title should be 'No selection'"));
  }

  /**
   * Tests the `doExecute` method when an exception occurs during execution.
   * Verifies that the method handles the exception and returns an appropriate error message.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithException() throws Exception {
    RuntimeException mockRuntimeException = new RuntimeException("Simulated error");

    OBError mockError = new OBError();
    mockError.setType(ActionHandlerTestConstants.ERROR);
    mockError.setMessage(ActionHandlerTestConstants.TRANSLATED_ERROR);

    staticDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(mockRuntimeException);

    staticOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockError);

    JSONObject expectedResult = new JSONObject();
    JSONObject message = new JSONObject();
    message.put(ActionHandlerTestConstants.SEVERITY, ActionHandlerTestConstants.ERROR);
    message.put("text", ActionHandlerTestConstants.TRANSLATED_ERROR);
    expectedResult.put(ActionHandlerTestConstants.MESSAGE, message);

    assertAll("Verify exception handling", () -> assertNotNull(expectedResult, ActionHandlerTestConstants.RESULT_NOT_NULL),
        () -> assertTrue(expectedResult.has(ActionHandlerTestConstants.MESSAGE), ActionHandlerTestConstants.RESULT_MESSAGE_FIELD),
        () -> assertEquals(ActionHandlerTestConstants.ERROR, expectedResult.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY),
            "The severity should be 'error'"),
        () -> assertEquals(ActionHandlerTestConstants.TRANSLATED_ERROR, expectedResult.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString("text"),
            "The text should be 'Translated error'"));
  }

  /**
   * Tests the `doExecute` method when a null exception message is encountered.
   * Verifies that the method handles the null message gracefully and rolls back the transaction.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithNullExceptionMessage() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createValidJsonContent();

    RuntimeException mockException = new RuntimeException("null");
    when(mockOBDal.getProxy(eq(Product.class), anyString())).thenThrow(mockException);

    Throwable underlyingException = new SQLException("null");
    staticDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(underlyingException);

    staticOBMessageUtils.when(() -> OBMessageUtils.translateError(null)).thenThrow(new NullPointerException());

    JSONObject result = handler.doExecute(parameters, content);

    assertNotNull(result, ActionHandlerTestConstants.RESULT_NOT_NULL);

    staticOBContext.verify(() -> OBContext.setAdminMode(true));
    staticOBContext.verify(OBContext::restorePreviousMode);
    verify(mockOBDal, times(1)).rollbackAndClose();
  }

  /**
   * Tests the `doExecute` method when an exception occurs in the catch block.
   * Verifies that the method handles the secondary exception and rolls back the transaction.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithExceptionInCatchBlock() throws Exception {
    Map<String, Object> parameters = new HashMap<>();
    String content = createValidJsonContent();

    RuntimeException mockException = new RuntimeException("Simulated error");
    when(mockOBDal.getProxy(eq(Product.class), anyString())).thenThrow(mockException);

    staticDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenThrow(
        new NullPointerException("Error in catch"));

    JSONObject result = handler.doExecute(parameters, content);

    assertNotNull(result, ActionHandlerTestConstants.RESULT_NOT_NULL);

    staticOBContext.verify(() -> OBContext.setAdminMode(true));
    staticOBContext.verify(OBContext::restorePreviousMode);
    verify(mockOBDal, times(1)).rollbackAndClose();

  }

  /**
   * Creates a valid JSON content string for testing.
   * The JSON includes product, client, organization, and selected lines.
   *
   * @return a JSON string containing valid test data
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createValidJsonContent() throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inpmProductId", "TEST_PRODUCT_ID");
    jsonRequest.put("inpadClientId", "TEST_CLIENT_ID");
    jsonRequest.put("inpadOrgId", "TEST_ORG_ID");

    JSONObject params = new JSONObject();
    JSONObject relateProdCatNewTax = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject line1 = new JSONObject();
    line1.put(ProductCategory.PROPERTY_ID, "CATEGORY_1_ID");
    line1.put("taxCategory", "TAX_CATEGORY_1_ID");
    selection.put(line1);

    JSONObject line2 = new JSONObject();
    line2.put(ProductCategory.PROPERTY_ID, "CATEGORY_2_ID");
    line2.put("taxCategory", "TAX_CATEGORY_2_ID");
    selection.put(line2);

    relateProdCatNewTax.put("_selection", selection);
    params.put("relateProdCatNewTax", relateProdCatNewTax);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }
}
