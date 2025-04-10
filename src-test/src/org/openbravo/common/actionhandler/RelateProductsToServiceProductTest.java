package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.ServiceProduct;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link RelateProductsToServiceProduct} class.
 * Verifies the behavior of the `doExecute` method under various scenarios,
 * including empty selections, valid selections, and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class RelateProductsToServiceProductTest {

  @InjectMocks
  private RelateProductsToServiceProduct actionHandler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private Session mockSession;

  @Mock
  private Product mockServiceProduct;

  @Mock
  private Product mockRelatedProduct;

  @Mock
  private Client mockClient;

  @Mock
  private Organization mockOrg;

  @Mock
  private ServiceProduct mockServiceProductRelation;

  @Mock
  private OBProvider mockOBProvider;

  @Mock
  private OBError mockOBError;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<DbUtility> mockedDbUtility;

  /**
   * Sets up the test environment before each test.
   * Initializes mocked static methods and dependencies.
   */
  @BeforeEach
  public void setup() {
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(mockOBProvider);

  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBDal != null) mockedOBDal.close();
    if (mockedOBContext != null) mockedOBContext.close();
    if (mockedOBMessageUtils != null) mockedOBMessageUtils.close();
    if (mockedOBProvider != null) mockedOBProvider.close();
    if (mockedDbUtility != null) mockedDbUtility.close();
  }

  /**
   * Tests the `doExecute` method with an empty selection.
   * Verifies that the method returns an error message indicating no lines were selected.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithEmptySelection() throws Exception {
    String content = createEmptySelectionContent();
    Map<String, Object> parameters = new HashMap<>();

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("NotSelected")).thenReturn("No lines selected");

    JSONObject result = actionHandler.doExecute(parameters, content);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockOBProvider, never()).get(ServiceProduct.class);
    verify(mockOBDal, never()).save(any(ServiceProduct.class));

    verify(mockOBDal, never()).rollbackAndClose();

    assertEquals("error", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals("No lines selected", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString("title"));
  }

  /**
   * Tests the `doExecute` method with a valid selection.
   * Verifies that the method processes the selected lines and creates the expected relationships.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoExecuteWithValidSelection() throws Exception {
    String content = createValidContent();
    Map<String, Object> parameters = new HashMap<>();

    when(mockOBDal.getProxy(Product.ENTITY_NAME, ActionHandlerTestConstants.TEST_SERVICE_PRODUCT)).thenReturn(mockServiceProduct);
    when(mockOBDal.getProxy(Client.ENTITY_NAME, ActionHandlerTestConstants.TEST_CLIENT)).thenReturn(mockClient);
    when(mockOBDal.getProxy(Organization.ENTITY_NAME, ActionHandlerTestConstants.TEST_ORG)).thenReturn(mockOrg);
    when(mockOBDal.getProxy(Product.ENTITY_NAME, "TEST_RELATED_PRODUCT")).thenReturn(mockRelatedProduct);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    doNothing().when(mockSession).clear();

    when(mockOBProvider.get(ServiceProduct.class)).thenReturn(mockServiceProductRelation);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(ActionHandlerTestConstants.SUCCESS)).thenReturn(ActionHandlerTestConstants.SUCCESS);

    JSONObject result = actionHandler.doExecute(parameters, content);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockServiceProductRelation).setClient(mockClient);
    verify(mockServiceProductRelation).setOrganization(mockOrg);
    verify(mockServiceProductRelation).setProduct(mockServiceProduct);
    verify(mockServiceProductRelation).setRelatedProduct(mockRelatedProduct);
    verify(mockOBDal).save(mockServiceProductRelation);
    verify(mockOBDal).flush();

    verify(mockSession).clear();

    verify(mockOBDal, never()).rollbackAndClose();

    assertEquals("success", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals(ActionHandlerTestConstants.SUCCESS, result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString("title"));
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
    String content = createValidContent();
    Map<String, Object> parameters = new HashMap<>();

    RuntimeException testException = new RuntimeException("Test exception");
    when(mockOBDal.getProxy(eq(Product.ENTITY_NAME), eq(ActionHandlerTestConstants.TEST_SERVICE_PRODUCT))).thenThrow(testException);

    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(testException);

    when(mockOBError.getMessage()).thenReturn("Translated test exception");
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockOBError);

    JSONObject result = actionHandler.doExecute(parameters, content);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);

    verify(mockOBDal).rollbackAndClose();

    assertEquals("error", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString(ActionHandlerTestConstants.SEVERITY));
    assertEquals("Translated test exception", result.getJSONObject(ActionHandlerTestConstants.MESSAGE).getString("text"));
  }

  /**
   * Creates a valid JSON content string for testing.
   * The JSON includes product, client, organization, and selected lines.
   *
   * @return a JSON string containing valid test data
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createValidContent() throws JSONException {
    JSONObject content = new JSONObject();
    content.put("inpmProductId", ActionHandlerTestConstants.TEST_SERVICE_PRODUCT);
    content.put("inpadClientId", ActionHandlerTestConstants.TEST_CLIENT);
    content.put("inpadOrgId", ActionHandlerTestConstants.TEST_ORG);

    JSONObject params = new JSONObject();
    JSONObject servicesRelatedProducts = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject selectedProduct = new JSONObject();
    selectedProduct.put(Product.PROPERTY_ID, "TEST_RELATED_PRODUCT");
    selection.put(selectedProduct);

    servicesRelatedProducts.put("_selection", selection);
    params.put("servicesRelatedProducts", servicesRelatedProducts);
    content.put("_params", params);

    return content.toString();
  }

  /**
   * Creates a JSON content string with an empty selection for testing.
   * The JSON includes product, client, and organization, but no selected lines.
   *
   * @return a JSON string containing test data with an empty selection
   * @throws JSONException
   *     if an error occurs while creating the JSON content
   */
  private String createEmptySelectionContent() throws JSONException {
    JSONObject content = new JSONObject();
    content.put("inpmProductId", ActionHandlerTestConstants.TEST_SERVICE_PRODUCT);
    content.put("inpadClientId", ActionHandlerTestConstants.TEST_CLIENT);
    content.put("inpadOrgId", ActionHandlerTestConstants.TEST_ORG);

    JSONObject params = new JSONObject();
    JSONObject servicesRelatedProducts = new JSONObject();
    JSONArray selection = new JSONArray();

    servicesRelatedProducts.put("_selection", selection);
    params.put("servicesRelatedProducts", servicesRelatedProducts);
    content.put("_params", params);

    return content.toString();
  }
}
