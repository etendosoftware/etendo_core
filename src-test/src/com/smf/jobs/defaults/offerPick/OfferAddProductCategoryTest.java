package com.smf.jobs.defaults.offerPick;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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

@ExtendWith(MockitoExtension.class)
public class OfferAddProductCategoryTest {

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

  @BeforeEach
  public void setup() {
    offerAddProductCategory = new OfferAddProductCategory();
    when(priceAdjustment.getClient()).thenReturn(client);
    when(priceAdjustment.getOrganization()).thenReturn(organization);
  }

  @Test
  public void testDoPickAndExecuteSingleProductCategory() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // Arrange
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);

      JSONArray selectedLines = new JSONArray();
      JSONObject productCatJson = new JSONObject();
      productCatJson.put("id", "testId");
      selectedLines.put(productCatJson);

      ProductCategory productCategory = mock(ProductCategory.class);
      when(obDal.getProxy(eq(ProductCategory.ENTITY_NAME), eq("testId"))).thenReturn(productCategory);

      OBProvider obProvider = mock(OBProvider.class);
      org.openbravo.model.pricing.priceadjustment.ProductCategory mockProductCategory =
          mock(org.openbravo.model.pricing.priceadjustment.ProductCategory.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(org.openbravo.model.pricing.priceadjustment.ProductCategory.class))
          .thenReturn(mockProductCategory);

      // Act
      offerAddProductCategory.doPickAndExecute(priceAdjustment, selectedLines);

      // Assert
      verify(mockProductCategory).setActive(true);
      verify(mockProductCategory).setClient(client);
      verify(mockProductCategory).setOrganization(organization);
      verify(mockProductCategory).setPriceAdjustment(priceAdjustment);
      verify(mockProductCategory).setProductCategory(productCategory);
      verify(obDal).save(mockProductCategory);
    }
  }

  @Test
  public void testDoPickAndExecuteMultipleProductCategories() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // Arrange
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);

      JSONArray selectedLines = new JSONArray();
      for (int i = 0; i < 150; i++) {
        JSONObject productCatJson = new JSONObject();
        productCatJson.put("id", "testId" + i);
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

      // Act
      offerAddProductCategory.doPickAndExecute(priceAdjustment, selectedLines);

      // Assert
      verify(obDal, times(150)).save(any());
      verify(obDal, times(2)).flush();
      verify(session, times(2)).clear();
    }
  }
}