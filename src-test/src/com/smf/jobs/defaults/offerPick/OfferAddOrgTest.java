package com.smf.jobs.defaults.offerPick;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

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
import org.openbravo.model.pricing.priceadjustment.OrganizationFilter;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.hibernate.Session;

@ExtendWith(MockitoExtension.class)
public class OfferAddOrgTest {

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

  private OfferAddOrg offerAddOrg;

  @BeforeEach
  public void setup() {
    offerAddOrg = new OfferAddOrg();
  }

  @Test
  public void testDoPickAndExecuteSingleOrganization() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // Arrange
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);
      when(priceAdjustment.getClient()).thenReturn(client);

      JSONArray selectedLines = new JSONArray();
      JSONObject orgJson = new JSONObject();
      orgJson.put("id", "testId");
      selectedLines.put(orgJson);

      when(obDal.getProxy(eq(Organization.ENTITY_NAME), eq("testId"))).thenReturn(organization);

      OBProvider obProvider = mock(OBProvider.class);
      OrganizationFilter mockOrgFilter = mock(OrganizationFilter.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(OrganizationFilter.class)).thenReturn(mockOrgFilter);

      // Act
      offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines);

      // Assert
      verify(mockOrgFilter).setActive(true);
      verify(mockOrgFilter).setClient(client);
      verify(mockOrgFilter).setOrganization(organization);
      verify(mockOrgFilter).setPriceAdjustment(priceAdjustment);
      verify(obDal).save(mockOrgFilter);
    }
  }

  @Test
  public void testDoPickAndExecuteMultipleOrganizations() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      // Arrange
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);
      when(priceAdjustment.getClient()).thenReturn(client);

      JSONArray selectedLines = new JSONArray();
      for (int i = 0; i < 150; i++) {
        JSONObject orgJson = new JSONObject();
        orgJson.put("id", "testId" + i);
        selectedLines.put(orgJson);
      }

      when(obDal.getProxy(eq(Organization.ENTITY_NAME), anyString())).thenReturn(organization);

      OBProvider obProvider = mock(OBProvider.class);
      OrganizationFilter mockOrgFilter = mock(OrganizationFilter.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(OrganizationFilter.class)).thenReturn(mockOrgFilter);

      // Act
      offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines);

      // Assert
      verify(obDal, times(150)).save(any());
      verify(obDal, times(2)).flush();
      verify(session, times(2)).clear();
    }
  }

  @Test
  public void testDoPickAndExecuteInvalidJSON() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      // Arrange
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      JSONArray selectedLines = new JSONArray();
      JSONObject invalidJson = new JSONObject();
      invalidJson.put("invalid", "value");
      selectedLines.put(invalidJson);


      // Act & Assert
      assertThrows(JSONException.class, () ->
          offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines));
    }
  }

  @Test
  public void testGetJSONName() {
    assertEquals("Conforgprocess", offerAddOrg.getJSONName());
  }
}