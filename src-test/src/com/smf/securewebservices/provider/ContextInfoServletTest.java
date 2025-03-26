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
import org.openbravo.base.weld.test.WeldBaseTest;
import org.openbravo.dal.core.OBContext;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;

import com.smf.securewebservices.service.ContextInfoServlet;
import com.smf.securewebservices.utils.SecureWebServicesUtils;
import com.smf.securewebservices.utils.WSResult;

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

  @After
  public void tearDown() {
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedSecureWebServicesUtils != null) {
      mockedSecureWebServicesUtils.close();
    }
  }

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
