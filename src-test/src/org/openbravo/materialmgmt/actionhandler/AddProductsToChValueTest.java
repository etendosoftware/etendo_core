package org.openbravo.materialmgmt.actionhandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.CharacteristicsUtils;
import org.openbravo.model.common.plm.CharacteristicValue;
import org.openbravo.model.common.plm.Product;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for {@link AddProductsToChValue}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AddProductsToChValueTest {

  private static final String TEST_CH_VALUE_ID = "TEST_CH_VALUE_ID";
  private static final String MESSAGE = "message";
  private static final String ERROR_MESSAGE = "The answer must not be null";
  private static final String SEVERITY = "severity";
  /**
   * Rule to handle expected exceptions in tests.
   */
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<CharacteristicsUtils> mockedCharacteristicsUtils;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;

  @Mock
  private OBDal obDal;

  @Mock
  private CharacteristicValue mockCharacteristicValue;

  @Mock
  private Product mockProduct;

  @Mock
  private Product mockVariantProduct;

  @InjectMocks
  private AddProductsToChValue addProductsToChValue;

  /**
   * Sets up the necessary mock behaviors before each test.
   *
   * @throws Exception
   *     if setup fails
   */
  @Before
  public void setUp() throws Exception {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedCharacteristicsUtils = mockStatic(CharacteristicsUtils.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBContext.when(() -> OBContext.setAdminMode(anyBoolean())).thenAnswer(i -> null);
    mockedOBContext.when(OBContext::restorePreviousMode).thenAnswer(i -> null);

    when(mockCharacteristicValue.getName()).thenReturn("TestCharacteristicValue");
  }

  /**
   * Cleans up static mocks after each test.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedCharacteristicsUtils != null) {
      mockedCharacteristicsUtils.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
    }
  }

  /**
   * Tests the successful execution of {@code doExecute} when products are added correctly.
   *
   * @throws Exception
   *     if the test setup or execution fails
   */
  @Test
  public void testDoExecuteHappyPath() throws Exception {
    // GIVEN
    final String productId = "TEST_PRODUCT_ID";
    final String variantProductId = "TEST_VARIANT_PRODUCT_ID";

    Map<String, Object> parameters = new HashMap<>();
    String jsonContent = createRequestJsonContent(TEST_CH_VALUE_ID, productId);

    when(obDal.get(CharacteristicValue.class, TEST_CH_VALUE_ID)).thenReturn(mockCharacteristicValue);
    when(obDal.get(Product.class, productId)).thenReturn(mockProduct);

    List<Product> variantList = new ArrayList<>();
    variantList.add(mockVariantProduct);
    when(mockProduct.getProductGenericProductList()).thenReturn(variantList);
    when(mockVariantProduct.getId()).thenReturn(variantProductId);

    mockedCharacteristicsUtils.when(() -> CharacteristicsUtils.setCharacteristicValue(any(Product.class),
        any(CharacteristicValue.class))).thenReturn(null);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("AddProductsResult")).thenReturn(
        "@productNumer@ products added to characteristic value @chValueName@");

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(
        "2 products added to characteristic value TestCharacteristicValue");

    // WHEN
    JSONObject response = addProductsToChValue.doExecute(parameters, jsonContent);

    // THEN
    assertNotNull(ERROR_MESSAGE, response);
    JSONObject message = response.getJSONObject(MESSAGE);
    assertEquals("The message must be of type success", "success", message.getString(SEVERITY));
    assertEquals("The message must indicate the correct number of products",
        "2 products added to characteristic value TestCharacteristicValue", message.getString("text"));

    verify(obDal).get(CharacteristicValue.class, TEST_CH_VALUE_ID);
    verify(obDal).get(Product.class, productId);
    verify(mockProduct).getProductGenericProductList();
    verify(mockVariantProduct).getId();

    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));

    mockedCharacteristicsUtils.verify(
        () -> CharacteristicsUtils.setCharacteristicValue(any(Product.class), any(CharacteristicValue.class)),
        times(2));
  }

  /**
   * Tests the execution of {@code doExecute} when no products are selected.
   *
   * @throws Exception
   *     if the test setup or execution fails
   */
  @Test
  public void testDoExecuteNoProductsSelected() throws Exception {
    // GIVEN

    Map<String, Object> parameters = new HashMap<>();

    String jsonContent = createRequestJsonContentNoProducts(TEST_CH_VALUE_ID);

    when(obDal.get(CharacteristicValue.class, TEST_CH_VALUE_ID)).thenReturn(mockCharacteristicValue);

    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD("AddProductsResult")).thenReturn(
        "@productNumer@ products added to characteristic value @chValueName@");

    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(
        "0 products added to characteristic value TestCharacteristicValue");

    // WHEN
    JSONObject response = addProductsToChValue.doExecute(parameters, jsonContent);

    // THEN
    assertNotNull(ERROR_MESSAGE, response);
    JSONObject message = response.getJSONObject(MESSAGE);
    assertEquals("The message must be of type success", "success", message.getString(SEVERITY));
    assertEquals("The message must indicate the correct number of products",
        "0 products added to characteristic value TestCharacteristicValue", message.getString("text"));

    verify(obDal).get(CharacteristicValue.class, TEST_CH_VALUE_ID);

    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the execution of {@code doExecute} when an exception occurs.
   *
   * @throws Exception
   *     if the test setup or execution fails
   */
  @Test
  public void testDoExecuteException() throws Exception {
    // GIVEN
    final String productId = "TEST_PRODUCT_ID";

    Map<String, Object> parameters = new HashMap<>();
    String jsonContent = createRequestJsonContent(TEST_CH_VALUE_ID, productId);

    when(obDal.get(CharacteristicValue.class, TEST_CH_VALUE_ID)).thenReturn(mockCharacteristicValue);
    when(obDal.get(Product.class, productId)).thenThrow(new RuntimeException("Test exception"));

    RuntimeException testException = new RuntimeException("Test exception");
    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any(Exception.class))).thenReturn(testException);

    OBError mockOBError = mock(OBError.class);
    when(mockOBError.getMessage()).thenReturn("Test error message");
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockOBError);

    // WHEN
    JSONObject response = addProductsToChValue.doExecute(parameters, jsonContent);

    // THEN
    assertNotNull(ERROR_MESSAGE, response);
    JSONObject message = response.getJSONObject(MESSAGE);
    assertEquals("The message must be of type error", "error", message.getString(SEVERITY));
    assertEquals("The message must contain the error text", "Test error message", message.getString("text"));

    verify(obDal).get(CharacteristicValue.class, TEST_CH_VALUE_ID);
    verify(obDal).get(Product.class, productId);
    verify(mockOBError).getMessage();

    mockedOBContext.verify(() -> OBContext.setAdminMode(true), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
    mockedDbUtility.verify(() -> DbUtility.getUnderlyingSQLException(any(Exception.class)), times(1));
    mockedOBMessageUtils.verify(() -> OBMessageUtils.translateError(anyString()), times(1));
  }

  /**
   * Creates a JSON request containing a characteristic value ID and a product ID.
   *
   * @param chValueId
   *     the characteristic value ID
   * @param productId
   *     the product ID
   * @return a JSON string representing the request
   * @throws JSONException
   *     if JSON creation fails
   */
  private String createRequestJsonContent(String chValueId, String productId) throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inpmChValueId", chValueId);

    JSONObject params = new JSONObject();
    JSONObject view = new JSONObject();
    JSONArray selection = new JSONArray();

    if (productId != null) {
      JSONObject product = new JSONObject();
      product.put("product", productId);
      selection.put(product);
    }

    view.put("_selection", selection);
    params.put("m_prodchview_v", view);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }

  /**
   * Creates a JSON request containing only a characteristic value ID, with no products selected.
   *
   * @param chValueId
   *     the characteristic value ID
   * @return a JSON string representing the request
   * @throws JSONException
   *     if JSON creation fails
   */
  private String createRequestJsonContentNoProducts(String chValueId) throws JSONException {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("inpmChValueId", chValueId);

    JSONObject params = new JSONObject();
    JSONObject view = new JSONObject();
    JSONArray selection = new JSONArray();

    view.put("_selection", selection);
    params.put("m_prodchview_v", view);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }
}
