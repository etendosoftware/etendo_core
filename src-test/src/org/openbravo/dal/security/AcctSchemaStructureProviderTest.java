package org.openbravo.dal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;

@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctSchemaStructureProviderTest {

  private static final String TEST_ORG_ID = "testOrg001";
  private static final String TEST_CLIENT_ID = "testClient001";
  private static final String SCHEMA_ID_1 = "schema001";
  private static final String SCHEMA_ID_2 = "schema002";

  private AcctSchemaStructureProvider provider;

  @Mock
  private OBDal mockOBDal;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private Session mockSession;
  @Mock
  private Query<String> mockQuery;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<OBContext> obContextStatic;

  @Before
  public void setUp() {
    provider = new AcctSchemaStructureProvider();

    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);

    obContextStatic = mockStatic(OBContext.class);
    obContextStatic.when(OBContext::getOBContext).thenReturn(mockOBContext);
  }

  @After
  public void tearDown() {
    if (obDalStatic != null) obDalStatic.close();
    if (obContextStatic != null) obContextStatic.close();
  }

  @Test
  public void testSetAndGetClientId() {
    assertNull(provider.getClientId());
    provider.setClientId(TEST_CLIENT_ID);
    assertEquals(TEST_CLIENT_ID, provider.getClientId());
  }

  @Test
  public void testGetAcctSchemasCachesResult() throws Exception {
    // Arrange - pre-populate the cache via reflection
    List<String> schemas = Arrays.asList(SCHEMA_ID_1, SCHEMA_ID_2);
    Map<String, List<String>> cache = new HashMap<>();
    cache.put(TEST_ORG_ID, schemas);

    Field cacheField = AcctSchemaStructureProvider.class.getDeclaredField("acctSchemaByOrg");
    cacheField.setAccessible(true);
    cacheField.set(provider, cache);

    // Act
    List<String> result = provider.getAcctSchemas(TEST_ORG_ID, TEST_CLIENT_ID);

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(SCHEMA_ID_1, result.get(0));
    assertEquals(SCHEMA_ID_2, result.get(1));
    assertSame(schemas, result);
  }

  @Test
  public void testGetAcctSchemasInitializesFromDB() throws Exception {
    // Arrange
    List<String> expectedSchemas = Arrays.asList(SCHEMA_ID_1);

    // Mock OBContext for clientId initialization
    org.openbravo.model.ad.system.Client mockClientObj = mock(org.openbravo.model.ad.system.Client.class);
    when(mockOBContext.getCurrentClient()).thenReturn(mockClientObj);
    when(mockClientObj.getId()).thenReturn(TEST_CLIENT_ID);

    // Mock OBDal session and query
    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(any(String.class), eq(String.class))).thenReturn(mockQuery);
    when(mockQuery.list()).thenReturn(expectedSchemas);

    // Act
    List<String> result = provider.getAcctSchemas(TEST_ORG_ID, TEST_CLIENT_ID);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(SCHEMA_ID_1, result.get(0));
    assertEquals(TEST_CLIENT_ID, provider.getClientId());
  }

  @Test
  public void testGetAcctSchemasUsesProvidedClientIdWhenAlreadySet() throws Exception {
    // Arrange
    provider.setClientId("existingClient");
    List<String> expectedSchemas = Arrays.asList(SCHEMA_ID_2);

    when(mockOBDal.getSession()).thenReturn(mockSession);
    when(mockSession.createQuery(any(String.class), eq(String.class))).thenReturn(mockQuery);
    when(mockQuery.list()).thenReturn(expectedSchemas);

    // Act
    List<String> result = provider.getAcctSchemas(TEST_ORG_ID, TEST_CLIENT_ID);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("existingClient", provider.getClientId());
  }
}
