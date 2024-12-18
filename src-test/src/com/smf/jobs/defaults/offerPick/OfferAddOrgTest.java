package com.smf.jobs.defaults.offerPick;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Assertions;
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

import com.smf.jobs.defaults.Utility;

/**
 * Test class for the {@link OfferAddOrg} functionality.
 * This class contains unit tests to verify the behavior of the OfferAddOrg class,
 * which is responsible for adding organizations to a price adjustment.
 * The test cases cover various scenarios including:
 * <ul>
 *   <li>Adding a single organization to a price adjustment</li>
 *   <li>Adding multiple organizations to a price adjustment</li>
 *   <li>Handling invalid JSON input</li>
 *   <li>Verifying the JSON name retrieval</li>
 * </ul>
 *
 * The tests use Mockito for mocking dependencies and static method calls.
 */
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

  /**
   * Sets up the test environment before each test method.
   *
   * Initializes the OfferAddOrg instance to be tested.
   */
  @BeforeEach
  public void setup() {
    offerAddOrg = new OfferAddOrg();
  }

  /**
   * Tests the doPickAndExecute method with a single organization.
   * Verifies that:
   * <ul>
   *   <li>A single organization can be added to a price adjustment</li>
   *   <li>The organization filter is correctly configured</li>
   *   <li>The organization is saved to the database</li>
   * </ul>
   *
   * @throws JSONException if there's an error creating JSON objects
   */
  @Test
  public void testDoPickAndExecuteSingleOrganization() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);
      when(priceAdjustment.getClient()).thenReturn(client);

      JSONArray selectedLines = new JSONArray();
      JSONObject orgJson = new JSONObject();
      orgJson.put("id", Utility.TEST_ID);
      selectedLines.put(orgJson);

      when(obDal.getProxy(eq(Organization.ENTITY_NAME), eq(Utility.TEST_ID))).thenReturn(organization);

      OBProvider obProvider = mock(OBProvider.class);
      OrganizationFilter mockOrgFilter = mock(OrganizationFilter.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(OrganizationFilter.class)).thenReturn(mockOrgFilter);

      offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines);

      verify(mockOrgFilter).setActive(true);
      verify(mockOrgFilter).setClient(client);
      verify(mockOrgFilter).setOrganization(organization);
      verify(mockOrgFilter).setPriceAdjustment(priceAdjustment);
      verify(obDal).save(mockOrgFilter);
    }
  }

  /**
   * Tests the doPickAndExecute method with multiple organizations.
   * Verifies that:
   * <ul>
   *   <li>Multiple organizations can be added to a price adjustment</li>
   *   <li>The correct number of organizations are saved</li>
   *   <li>The database session is managed appropriately (flushed and cleared)</li>
   * </ul>
   *
   * @throws JSONException if there's an error creating JSON objects
   */
  @Test
  public void testDoPickAndExecuteMultipleOrganizations() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class);
         MockedStatic<OBProvider> mockedOBProvider = mockStatic(OBProvider.class)) {

      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);
      when(priceAdjustment.getClient()).thenReturn(client);

      JSONArray selectedLines = new JSONArray();
      for (int i = 0; i < 150; i++) {
        JSONObject orgJson = new JSONObject();
        orgJson.put("id", Utility.TEST_ID + i);
        selectedLines.put(orgJson);
      }

      when(obDal.getProxy(eq(Organization.ENTITY_NAME), anyString())).thenReturn(organization);

      OBProvider obProvider = mock(OBProvider.class);
      OrganizationFilter mockOrgFilter = mock(OrganizationFilter.class);

      mockedOBProvider.when(OBProvider::getInstance).thenReturn(obProvider);
      when(obProvider.get(OrganizationFilter.class)).thenReturn(mockOrgFilter);

      offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines);

      verify(obDal, times(150)).save(any());
      verify(obDal, times(2)).flush();
      verify(session, times(2)).clear();
    }
  }

  /**
   * Tests the doPickAndExecute method with invalid JSON input.
   * Verifies that:
   * <ul>
   *   <li>An invalid JSON input throws a JSONException</li>
   * </ul>
   *
   * @throws JSONException to be caught and verified by the test
   */
  @Test
  public void testDoPickAndExecuteInvalidJSON() throws JSONException {
    try (MockedStatic<OBDal> mockedOBDal = mockStatic(OBDal.class)) {
      mockedOBDal.when(OBDal::getInstance).thenReturn(obDal);

      JSONArray selectedLines = new JSONArray();
      JSONObject invalidJson = new JSONObject();
      invalidJson.put("invalid", "value");
      selectedLines.put(invalidJson);

      assertThrows(JSONException.class, () ->
          offerAddOrg.doPickAndExecute(priceAdjustment, selectedLines));
    }
  }

  /**
   * Tests the getJSONName method.
   * Verifies that:
   * <ul>
   *   <li>The method returns the correct JSON name "Conforgprocess"</li>
   * </ul>
   */
  @Test
  public void testGetJSONName() {
    Assertions.assertEquals("Conforgprocess", offerAddOrg.getJSONName());
  }
}