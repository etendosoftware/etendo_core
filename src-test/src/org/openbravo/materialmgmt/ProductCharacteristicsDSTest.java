package org.openbravo.materialmgmt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.EntityAccessChecker;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.access.RoleOrganization;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.datasource.DataSourceServiceProvider;
import org.openbravo.service.json.JsonConstants;

/**
 * Test cases for the ProductCharacteristicsDS class
 */
public class ProductCharacteristicsDSTest extends WeldBaseTest {

  private static final String ORG_ID = "1000000";

  @InjectMocks
  private ProductCharacteristicsDS productCharacteristicsDS;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<ModelProvider> mockedModelProvider;

  private OBContext mockOBContext;
  private OBDal mockOBDal;
  private ModelProvider mockModelProvider;
  private EntityAccessChecker mockEntityAccessChecker;
  private OrganizationStructureProvider mockOrgStructureProvider;
  private AutoCloseable mocks;

  /**
   * Sets up the test environment by initializing mocks and configuring static mocks.
   *
   * @throws Exception if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    // Initialize mocks
    mocks = MockitoAnnotations.openMocks(this);

    // Set up static mocks
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedModelProvider = mockStatic(ModelProvider.class);

    // Create mock instances
    mockOBContext = mock(OBContext.class);
    mockOBDal = mock(OBDal.class);
    mockModelProvider = mock(ModelProvider.class);
    mockEntityAccessChecker = mock(EntityAccessChecker.class);
    mockOrgStructureProvider = mock(OrganizationStructureProvider.class);

    // Configure static mocks
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockOBContext);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);
    mockedModelProvider.when(ModelProvider::getInstance).thenReturn(mockModelProvider);

    // Configure mock behavior
    when(mockOBContext.getEntityAccessChecker()).thenReturn(mockEntityAccessChecker);
    when(mockOBContext.getOrganizationStructureProvider()).thenReturn(mockOrgStructureProvider);
  }

  /**
   * Cleans up the test environment by closing static mocks and other resources.
   *
   * @throws Exception if an error occurs during teardown
   */
  @AfterEach
  void tearDown() throws Exception {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedModelProvider != null) {
      mockedModelProvider.close();
    }
    if (mocks != null) {
      mocks.close();
    }
  }

  /**
   * Test the checkFetchDatasourceAccess method when access is granted
   */
  @Test
  void testCheckFetchDatasourceAccessAccessGranted() {
    // Given
    Map<String, String> parameters = new HashMap<>();
    Entity mockEntity = mock(Entity.class);

    when(mockModelProvider.getEntityByTableId(ProductCharacteristicsDS.PRODUCT_CHARACTERISTICS_TABLE_ID))
        .thenReturn(mockEntity);

    // When
    productCharacteristicsDS.checkFetchDatasourceAccess(parameters);

    // Then
    // No exception should be thrown
  }

  /**
   * Tests the checkFetchDatasourceAccess method when access is denied.
   */
  @Test
  void testCheckFetchDatasourceAccessAccessDenied() {
    // Given
    Map<String, String> parameters = new HashMap<>();
    Entity mockEntity = mock(Entity.class);

    when(mockModelProvider.getEntityByTableId(ProductCharacteristicsDS.PRODUCT_CHARACTERISTICS_TABLE_ID))
        .thenReturn(mockEntity);

    OBSecurityException securityException = new OBSecurityException("Access denied");
    doThrow(securityException)
        .when(mockEntityAccessChecker).checkReadableAccess(mockEntity);

    ProductCharacteristicsDS testDS = new ProductCharacteristicsDS() {
      @Override
      protected void handleExceptionUnsecuredDSAccess(OBSecurityException e) {
        throw e;
      }
    };

    // When & Then
    assertThrows(OBSecurityException.class, () -> {
      testDS.checkFetchDatasourceAccess(parameters);
    });
  }

  /**
   * Test the getOrgFilter method
   */
  @Test
  void testGetOrgFilter() {
    // Given
    Role mockRole = mock(Role.class);
    Organization mockOrg = mock(Organization.class);
    RoleOrganization mockRoleOrg = mock(RoleOrganization.class);
    List<RoleOrganization> roleOrgList = new ArrayList<>();
    roleOrgList.add(mockRoleOrg);
    Set<String> orgTree = new HashSet<>();
    orgTree.add(ORG_ID);
    orgTree.add("1000001");

    when(mockOBContext.getRole()).thenReturn(mockRole);
    when(mockOBDal.get(Role.class, mockRole.getId())).thenReturn(mockRole);
    when(mockRole.getADRoleOrganizationList()).thenReturn(roleOrgList);
    when(mockRoleOrg.getOrganization()).thenReturn(mockOrg);
    when(mockOrg.getId()).thenReturn(ORG_ID);
    when(mockOrgStructureProvider.getNaturalTree(ORG_ID)).thenReturn(orgTree);

    // When
    Set<String> result = productCharacteristicsDS.getOrgFilter();

    // Then
    assertEquals(2, result.size());
    assertTrue(result.contains(ORG_ID));
    assertTrue(result.contains("1000001"));
  }

  /**
   * Tests the fetch method with basic parameters.
   *
   * @throws Exception if an error occurs during the test
   */
  @Test
  void testFetchBasicParameters() throws Exception {
    // Given
    Map<String, String> parameters = new HashMap<>();
    parameters.put("_parentDSIdentifier", "Product");
    parameters.put("_propertyPath", "e.product");

    ProductCharacteristicsDS customDS = new ProductCharacteristicsDS() {
      @Override
      protected JSONArray getAllNodes(Map<String, String> params, String dsIdentifier,
          String productPath, List<String> allNodes, Set<String> missingNodes, boolean addMissingNodes)
          throws JSONException {
        return new JSONArray();
      }

      @Override
      protected Set<String> getOrgFilter() {
        Set<String> result = new HashSet<>();
        result.add("0");
        return result;
      }
    };

    Field customQueriesField = ProductCharacteristicsDS.class.getDeclaredField("productCharacteristicCustomQueries");
    customQueriesField.setAccessible(true);

    @SuppressWarnings("unchecked")
    Instance<ProductCharacteristicCustomWhereClause> mockCustomQueries = mock(Instance.class);

    customQueriesField.set(customDS, mockCustomQueries);

    Field dataSourceServiceProviderField = ProductCharacteristicsDS.class.getDeclaredField("dataSourceServiceProvider");
    dataSourceServiceProviderField.setAccessible(true);

    DataSourceServiceProvider mockDataSourceServiceProvider = mock(DataSourceServiceProvider.class);

    dataSourceServiceProviderField.set(customDS, mockDataSourceServiceProvider);

    // When
    String result = customDS.fetch(parameters);

    // Then
    assertNotNull(result);
    JSONObject jsonResult = new JSONObject(result);
    assertTrue(jsonResult.has(JsonConstants.RESPONSE_RESPONSE));

    JSONObject response = jsonResult.getJSONObject(JsonConstants.RESPONSE_RESPONSE);
    assertTrue(response.has(JsonConstants.RESPONSE_STATUS));

    int startRow = Integer.parseInt(response.get(JsonConstants.RESPONSE_STARTROW).toString());
    assertEquals(0, startRow);

    int endRow = Integer.parseInt(response.get(JsonConstants.RESPONSE_ENDROW).toString());
    assertEquals(-1, endRow);

    int totalRows = Integer.parseInt(response.get(JsonConstants.RESPONSE_TOTALROWS).toString());
    assertEquals(0, totalRows);
  }
}
