package com.smf.securewebservices.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import com.smf.securewebservices.service.ContextInfoServlet;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;

/**
 * Unit tests for the ContextInfoServlet class.
 * Tests the retrieval of context information including client, role, organization, and warehouse data.
 */
public class ContextInfoServletTest {

  private ContextInfoServlet contextInfoServlet;

  @Mock
  private Client mockClient;
  @Mock
  private Role mockRole;
  @Mock
  private Organization mockOrganization;
  @Mock
  private Warehouse mockWarehouse;

  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<SecureWebServicesUtils> mockedSecureWebServicesUtils;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks, creates test instances and configures mock behavior.
   */
  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    contextInfoServlet = new ContextInfoServlet();

    // Mock OBContext
    mockedOBContext = mockStatic(OBContext.class);
    OBContext mockContext = mock(OBContext.class);
    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);

    when(mockContext.getCurrentClient()).thenReturn(mockClient);
    when(mockContext.getRole()).thenReturn(mockRole);
    when(mockContext.getCurrentOrganization()).thenReturn(mockOrganization);
    when(mockContext.getWarehouse()).thenReturn(mockWarehouse);
    when(mockContext.getReadableClients()).thenReturn(new String[] { "client1", "client2" });
    when(mockContext.getReadableOrganizations()).thenReturn(new String[] { "org1", "org2" });
    when(mockContext.getWritableOrganizations()).thenReturn(Set.of("org3", "org4"));

    // Mock SecureWebServicesUtils
    mockedSecureWebServicesUtils = mockStatic(SecureWebServicesUtils.class);
    when(SecureWebServicesUtils.getChildrenOrganizations(mockOrganization))
        .thenReturn(Collections.emptyList());

    // Mock entity IDs
    when(mockClient.getId()).thenReturn("clientId");
    when(mockRole.getId()).thenReturn("roleId");
    when(mockOrganization.getId()).thenReturn("orgId");
    when(mockWarehouse.getId()).thenReturn("warehouseId");
  }

  /**
   * Cleans up resources after each test.
   * Closes the static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedSecureWebServicesUtils != null) {
      mockedSecureWebServicesUtils.close();
    }
  }

  /**
   * Tests the successful retrieval of context information.
   * Verifies that the get method returns the expected status and result type.
   *
   * @throws Exception if an error occurs during test execution
   */
  @Test
  public void testGetHappyPath() throws Exception {
    // Given
    String path = "/context";
    Map<String, String> parameters = Collections.emptyMap();

    // When
    WSResult result = contextInfoServlet.get(path, parameters);

    // Then
    assertEquals(WSResult.Status.OK, result.getStatus());
    assertEquals(WSResult.ResultType.SINGLE, result.getResultType());
  }
}
