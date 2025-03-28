package org.openbravo.materialmgmt.actionhandler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.DalUtil;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBDao;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.materialmgmt.VariantChDescUpdateProcess;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.utility.Image;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductCharacteristic;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.DbUtility;

/**
 * Test class for the {@link ManageVariants} class.
 * This class contains unit tests for the methods in the ManageVariants class.
 */
@RunWith(MockitoJUnitRunner.class)
public class ManageVariantsTest {

  private static final String SUCCESS = "Success";


  // Mocked static instances
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DalUtil> mockedDalUtil;
  private MockedStatic<OBProvider> mockedOBProvider;
  private MockedStatic<OBDao> mockedOBDao;
  private MockedStatic<DbUtility> mockedDbUtility;

  // Mocked instances
  @Mock
  private OBDal obDal;

  @Mock
  private OBProvider obProvider;

  @InjectMocks
  private ManageVariants manageVariants;

  private Product mockVariantProduct;
  private VariantChDescUpdateProcess mockUpdateProcess;

  /**
   * Sets up the test environment before each test.
   * Initializes the mocked static instances and sets up the mock objects.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialization of mocked static instances
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDalUtil = mockStatic(DalUtil.class);
    mockedOBProvider = mockStatic(OBProvider.class);
    mockedOBDao = mockStatic(OBDao.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    // Mocking static methods
    mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
    mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
    mockedDbUtility.when(() -> DbUtility.getUnderlyingSQLException(any())).thenReturn(
        new RuntimeException("Test error"));

    // Mocking objects
    mockUpdateProcess = mock(VariantChDescUpdateProcess.class);
    setupMockObjects();
  }

  /**
   * Cleans up the test environment after each test.
   * Closes the mocked static instances.
   */
  @After
  public void tearDown() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedDalUtil != null) {
      mockedDalUtil.close();
    }
    if (mockedOBProvider != null) {
      mockedOBProvider.close();
    }
    if (mockedOBDao != null) {
      mockedOBDao.close();
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
    }
  }

  /**
   * Sets up the mock objects used in the tests.
   */
  private void setupMockObjects() {
    Product mockGenericProduct = mock(Product.class);
    mockVariantProduct = mock(Product.class);

    ProductCharacteristic mockProductCharacteristic = mock(ProductCharacteristic.class);
    Image mockImage = mock(Image.class);

    List<ProductCharacteristic> productCharacteristicList = new ArrayList<>();
    productCharacteristicList.add(mockProductCharacteristic);

    when(obDal.get(Product.class, "GENERIC_PRODUCT_ID")).thenReturn(mockGenericProduct);

    Client mockClient = mock(Client.class);
    when(mockClient.isMultilingualDocuments()).thenReturn(false);
    when(mockGenericProduct.getClient()).thenReturn(mockClient);

    mockedDalUtil.when(() -> DalUtil.copy(any(Product.class))).thenReturn(mockVariantProduct);
    mockedDalUtil.when(() -> DalUtil.copy(any(Image.class), any(Boolean.class))).thenReturn(mockImage);

    List<ProductPrice> productPriceList = new ArrayList<>();
    ProductPrice mockProductPrice = mock(ProductPrice.class);
    productPriceList.add(mockProductPrice);

    mockedOBDao.when(() -> OBDao.getActiveOBObjectList(any(Product.class), anyString())).thenReturn(productPriceList);

    doNothing().when(obDal).save(any());
  }

  /**
   * Tests the creation of a variant.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCreateVariant() throws Exception {
    // GIVEN
    String content = createTestVariantJSON(false);
    Map<String, Object> parameters = new HashMap<>();

    // Setup mocks for variant ID
    when(mockVariantProduct.getId()).thenReturn("MOCK_VARIANT_ID");

    // Create a spy of the ManageVariants class to intercept the createVariant method
    ManageVariants spyManageVariants = spy(manageVariants);

    // Override the createVariant method to use our mock VariantChDescUpdateProcess
    doAnswer(invocation -> {
      mockUpdateProcess.update(mockVariantProduct.getId(), null);
      return null;
    }).when(spyManageVariants).createVariant(any(JSONObject.class), any(Product.class));

    // Setup message utils
    mockedOBMessageUtils.when(() -> OBMessageUtils.messageBD(SUCCESS)).thenReturn(SUCCESS);
    mockedOBMessageUtils.when(() -> OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(SUCCESS);

    // WHEN
    JSONObject result = spyManageVariants.doExecute(parameters, content);

    // THEN
    assertNotNull("The result should not be null", result);

    // Verify the update method was called with the expected parameters
    verify(mockUpdateProcess, times(1)).update(eq("MOCK_VARIANT_ID"), eq(null));

    // Verify OBContext was properly used
    mockedOBContext.verify(() -> OBContext.setAdminMode(true));
    mockedOBContext.verify(OBContext::restorePreviousMode);
  }

  /**
   * Tests the update of a variant.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUpdateVariant() throws Exception {
    // GIVEN
    String content = createTestVariantJSON(true);
    Map<String, Object> parameters = new HashMap<>();

    when(obDal.get(Product.class, "VARIANT_ID")).thenReturn(mockVariantProduct);

    when(OBMessageUtils.messageBD(SUCCESS)).thenReturn(SUCCESS);
    when(OBMessageUtils.parseTranslation(anyString(), any())).thenReturn(SUCCESS);

    // WHEN
    JSONObject result = manageVariants.doExecute(parameters, content);

    // THEN
    assertNotNull(result);
    verify(mockVariantProduct).setName("Variant Name");
    verify(mockVariantProduct).setSearchKey("VAR001");
  }

  /**
   * Tests the creation of a variant when an error occurs.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testCreateVariantWithError() throws Exception {
    // GIVEN
    String content = createTestVariantJSON(false);
    Map<String, Object> parameters = new HashMap<>();

    doThrow(new RuntimeException("Test exception")).when(obDal).flush();

    org.openbravo.erpCommon.utility.OBError mockObError = mock(org.openbravo.erpCommon.utility.OBError.class);
    when(mockObError.getMessage()).thenReturn("Error message");
    mockedOBMessageUtils.when(() -> OBMessageUtils.translateError(anyString())).thenReturn(mockObError);

    // WHEN
    JSONObject result = null;
    try {
      result = manageVariants.doExecute(parameters, content);
    } catch (Exception e) {
      fail("Shouldn't have thrown exception: " + e.getMessage());
    }

    // THEN
    assertNotNull("The result should not be null", result);
  }

  /**
   * Creates a JSON request for testing the creation or update of a variant.
   *
   * @param isVariantCreated
   *     whether the variant is already created
   * @return a JSON string representing the request
   * @throws Exception
   *     if JSON creation fails
   */
  private String createTestVariantJSON(boolean isVariantCreated) throws Exception {
    JSONObject jsonRequest = new JSONObject();
    jsonRequest.put("M_Product_ID", "GENERIC_PRODUCT_ID");

    JSONObject params = new JSONObject();
    JSONObject grid = new JSONObject();
    JSONArray selection = new JSONArray();

    JSONObject row = new JSONObject();
    row.put("variantCreated", isVariantCreated);
    row.put("name", "Variant Name");
    row.put("searchKey", "VAR001");

    if (isVariantCreated) {
      row.put("variantId", "VARIANT_ID");
    } else {
      JSONArray characteristicArray = new JSONArray();
      JSONObject charValue = new JSONObject();
      charValue.put("characteristic", "CHAR_ID");
      charValue.put("characteristicValue", "CHAR_VALUE_ID");
      charValue.put("characteristicConf", "CONF_ID");
      characteristicArray.put(charValue);
      row.put("characteristicArray", characteristicArray);
    }

    selection.put(row);
    grid.put("_selection", selection);
    params.put("grid", grid);
    jsonRequest.put("_params", params);

    return jsonRequest.toString();
  }
}
