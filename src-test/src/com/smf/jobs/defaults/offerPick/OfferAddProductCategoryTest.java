package com.smf.jobs.defaults.offerPick;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.ProductCategory;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.hibernate.Session;

/**
 * Unit tests for the {@link OfferAddProductCategory} class.
 *
 * <p>These tests validate the behavior of the {@code doPickAndExecute} method,
 * ensuring that product categories are correctly handled and associated with a
 * {@link PriceAdjustment} instance in both single and multiple category scenarios.
 */
@ExtendWith(MockitoExtension.class)
public class OfferAddProductCategoryTest {

  private static final String TEST_ID = "testId" ;


  @Mock
  private OBDal obDal;

  @Mock
  private PriceAdjustment priceAdjustment;

  @Mock
  private Client client;

  @Mock
  private Organization organization;

  @Mock
  private Session session;

  private OfferAddProductCategory offerAddProductCategory;

  /**
   * Initializes the test environment before each test.
   *
   * <p>Configures the {@code OfferAddProductCategory} instance and defines default behavior for
   * the mocked objects, such as {@code PriceAdjustment}.
   */
  @BeforeEach
  public void setup() {
    offerAddProductCategory = new OfferAddProductCategory();
    when(priceAdjustment.getClient()).thenReturn(client);
    when(priceAdjustment.getOrganization()).thenReturn(organization);
  }

  /**
   * Tests the {@code doPickAndExecute} method with a single product category.
   *
   * <p>Validates that the method retrieves, sets, and saves a single product category
   * and its associated {@link PriceAdjustment}, ensuring the proper calls to mock objects.
   *
   * @throws JSONException if there is an error creating the JSON input.
   */
  @Test
  public void testDoPickAndExecuteSingleProductCategory() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);

      JSONArray selectedLines = new JSONArray();
      JSONObject productCatJson = new JSONObject();
      productCatJson.put("id", TEST_ID);
      selectedLines.put(productCatJson);

      ProductCategory productCategory = mock(ProductCategory.class);
      when(obDal.getProxy(eq(ProductCategory.ENTITY_NAME), eq(TEST_ID))).thenReturn(productCategory);

      OBProvider obProvider = mock(OBProvider.class);
      org.openbravo.model.pricing.priceadjustment.ProductCategory mockProductCategory =
          mock(org.openbravo.model.pricing.priceadjustment.ProductCategory.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(org.openbravo.model.pricing.priceadjustment.ProductCategory.class))
          .thenReturn(mockProductCategory);

      offerAddProductCategory.doPickAndExecute(priceAdjustment, selectedLines);

      verify(mockProductCategory).setActive(true);
      verify(mockProductCategory).setClient(client);
      verify(mockProductCategory).setOrganization(organization);
      verify(mockProductCategory).setPriceAdjustment(priceAdjustment);
      verify(mockProductCategory).setProductCategory(productCategory);
      verify(obDal).save(mockProductCategory);
    }
  }

  /**
   * Tests the {@code doPickAndExecute} method with multiple product categories.
   *
   * <p>Validates that the method efficiently processes and saves multiple product categories,
   * flushes the session at regular intervals, and clears it to optimize performance.
   *
   * @throws JSONException if there is an error creating the JSON input.
   */
  @Test
  public void testDoPickAndExecuteMultipleProductCategories() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);

      JSONArray selectedLines = new JSONArray();
      for (int i = 0; i < 150; i++) {
        JSONObject productCatJson = new JSONObject();
        productCatJson.put("id", TEST_ID + i);
        selectedLines.put(productCatJson);
      }

      ProductCategory productCategory = mock(ProductCategory.class);
      when(obDal.getProxy(eq(ProductCategory.ENTITY_NAME), anyString())).thenReturn(productCategory);

      OBProvider obProvider = mock(OBProvider.class);
      org.openbravo.model.pricing.priceadjustment.ProductCategory mockProductCategory =
          mock(org.openbravo.model.pricing.priceadjustment.ProductCategory.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(org.openbravo.model.pricing.priceadjustment.ProductCategory.class))
          .thenReturn(mockProductCategory);

      offerAddProductCategory.doPickAndExecute(priceAdjustment, selectedLines);

      verify(obDal, times(150)).save(any());
      verify(obDal, times(2)).flush();
      verify(session, times(2)).clear();
    }
  }
}