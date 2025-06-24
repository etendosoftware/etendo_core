package com.smf.jobs.defaults.offerPick;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;

/**
 * Unit tests for the {@link OfferAddProduct} class.
 * This test class validates the functionality of the OfferAddProduct class
 * by testing its behavior in scenarios such as adding a product to an offer,
 * processing multiple products, handling invalid JSON, and retrieving its JSON name.
 * It uses Mockito for mocking dependencies and ensures the methods work as intended.
 */
@RunWith(MockitoJUnitRunner.class)
public class OfferAddProductTest {

  private OfferAddProduct offerAddProduct;

  @Mock
  private OBDal obDal;

  @Mock
  private Product product;

  @Mock
  private PriceAdjustment priceAdjustment;

  @Mock
  private Client client;

  @Mock
  private Organization organization;

  @Mock
  private org.openbravo.model.pricing.priceadjustment.Product priceAdjustmentProduct;

  @Mock
  private Session session;

  /**
   * Sets up the required mock objects and default behavior
   * before executing each test.
   */
  @Before
  public void setUp() {
    offerAddProduct = new OfferAddProduct();

    when(priceAdjustment.getClient()).thenReturn(client);
    when(priceAdjustment.getOrganization()).thenReturn(organization);
    when(obDal.getSession()).thenReturn(session);
  }

  /**
   * Tests the doPickAndExecute method for the basic scenario where a single product
   * is added to an offer. It validates that all operations are called as expected.
   *
   * @throws Exception if there is any issue during JSON processing or mock interaction
   */
  @Test
  public void testDoPickAndExecute() throws Exception {
    JSONArray selectedLines = new JSONArray();
    JSONObject productJson = new JSONObject();
    productJson.put("id", "test-product-id");
    selectedLines.put(productJson);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getProxy(eq(Product.ENTITY_NAME), anyString())).thenReturn(product);

      OBProvider obProvider = mock(OBProvider.class);
      obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(org.openbravo.model.pricing.priceadjustment.Product.class))
          .thenReturn(priceAdjustmentProduct);

      doNothing().when(priceAdjustmentProduct).setActive(anyBoolean());
      doNothing().when(priceAdjustmentProduct).setClient(any(Client.class));
      doNothing().when(priceAdjustmentProduct).setOrganization(any(Organization.class));
      doNothing().when(priceAdjustmentProduct).setPriceAdjustment(any(PriceAdjustment.class));
      doNothing().when(priceAdjustmentProduct).setProduct(any(Product.class));
      doNothing().when(obDal).save(any());
      doNothing().when(obDal).flush();
      doNothing().when(session).clear();

      offerAddProduct.doPickAndExecute(priceAdjustment, selectedLines);

      verify(priceAdjustmentProduct).setActive(true);
      verify(priceAdjustmentProduct).setClient(client);
      verify(priceAdjustmentProduct).setOrganization(organization);
      verify(priceAdjustmentProduct).setPriceAdjustment(priceAdjustment);
      verify(priceAdjustmentProduct).setProduct(product);
      verify(obDal).save(priceAdjustmentProduct);
    }
  }

  /**
   * Tests the getJSONName method to ensure it returns the correct JSON name.
   */
  @Test
  public void testGetJSONName() {
    Assert.assertEquals("Confprodprocess", StringUtils.defaultString(offerAddProduct.getJSONName()));
  }

  /**
   * Tests the doPickAndExecute method when processing multiple products.
   * Ensures that the appropriate operations are executed multiple times.
   *
   * @throws Exception if there is any issue during JSON processing or mock interaction
   */
  @Test
  public void testDoPickAndExecuteWithMultipleProducts() throws Exception {
    JSONArray selectedLines = new JSONArray();
    for (int i = 0; i < 150; i++) {
      JSONObject productJson = new JSONObject();
      productJson.put("id", "test-product-id-" + i);
      selectedLines.put(productJson);
    }

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getProxy(eq(Product.ENTITY_NAME), anyString())).thenReturn(product);

      OBProvider obProvider = mock(OBProvider.class);
      obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(org.openbravo.model.pricing.priceadjustment.Product.class))
          .thenReturn(priceAdjustmentProduct);

      doNothing().when(priceAdjustmentProduct).setActive(anyBoolean());
      doNothing().when(priceAdjustmentProduct).setClient(any(Client.class));
      doNothing().when(priceAdjustmentProduct).setOrganization(any(Organization.class));
      doNothing().when(priceAdjustmentProduct).setPriceAdjustment(any(PriceAdjustment.class));
      doNothing().when(priceAdjustmentProduct).setProduct(any(Product.class));
      doNothing().when(obDal).save(any());
      doNothing().when(obDal).flush();
      doNothing().when(session).clear();

      offerAddProduct.doPickAndExecute(priceAdjustment, selectedLines);

      verify(obDal, times(150)).save(any());
      verify(obDal, atLeast(1)).flush();
      verify(session, atLeast(1)).clear();
    }
  }

  /**
   * Tests the doPickAndExecute method when the input JSON is invalid.
   * Expects a {@link JSONException} to be thrown.
   *
   * @throws Exception if the test encounters any unexpected errors
   */
  @Test(expected = JSONException.class)
  public void testDoPickAndExecuteWithInvalidJSON() throws Exception {
    JSONArray selectedLines = new JSONArray();
    JSONObject invalidJson = new JSONObject();
    selectedLines.put(invalidJson);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {
      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      offerAddProduct.doPickAndExecute(priceAdjustment, selectedLines);
    }
  }
}