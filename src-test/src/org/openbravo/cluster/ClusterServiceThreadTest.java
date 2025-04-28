package org.openbravo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.system.ADClusterService;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for the `ClusterServiceThread` class, a private inner class of {@link ClusterServiceManager}.
 * Verifies the behavior of methods related to cluster service management, including service registration,
 * ping rounds, and node updates.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ClusterServiceThreadTest {

  private static final String NODE_ID = "test-node-id";
  private static final String NODE_NAME = "test-node-name";
  private static final String SERVICE_NAME = "test-service";
  public static final String NODE_ID_PARAM = "nodeId";

  @Mock
  private ClusterServiceManager mockManager;
  @Mock
  private ClusterService mockClusterService1;
  @Mock
  private OBDal mockOBDal;
  @Mock
  private Organization mockOrganization;
  @Mock
  private OBProvider mockOBProvider;
  @Mock
  private OBCriteria<ADClusterService> mockCriteria;
  @Mock
  private ADClusterService mockADClusterService;
  @Mock
  private Client mockClient;
  @Mock
  private Session mockSession;
  @Mock
  private Query mockQuery;
  @Mock
  private OBContext mockOBContext;
  @Mock
  private User mockUser;
  private Object clusterServiceThread;

  /**
   * Sets a private static field to a specified value using reflection.
   *
   * @param clazz
   *     the class containing the field
   * @param fieldName
   *     the name of the field
   * @param value
   *     the value to set
   * @throws Exception
   *     if an error occurs during reflection
   */
  private static void setPrivateStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  /**
   * Sets up the test environment before each test.
   * Initializes the `ClusterServiceThread` instance and mocks required dependencies.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, "isCluster", true);

    Class<?> threadClass = Class.forName("org.openbravo.cluster.ClusterServiceManager$ClusterServiceThread");
    Constructor<?> constructor = threadClass.getDeclaredConstructor(ClusterServiceManager.class);
    constructor.setAccessible(true);
    clusterServiceThread = constructor.newInstance(mockManager);

    setPrivateField(mockManager, NODE_ID_PARAM, NODE_ID);
    setPrivateField(mockManager, "nodeName", NODE_NAME);
  }

  /**
   * Sets up the test environment before each test.
   * Initializes the `ClusterServiceThread` instance and mocks required dependencies.
   *
   */
  @Test
  public void testRegisterService() {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class); MockedStatic<OBProvider> obProviderMock = mockStatic(
        OBProvider.class); MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.isInAdministratorMode()).thenReturn(false);

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      obProviderMock.when(OBProvider::getInstance).thenReturn(mockOBProvider);

      when(mockOBProvider.get(ADClusterService.class)).thenReturn(mockADClusterService);
      when(mockOBDal.getProxy(Organization.class, "0")).thenReturn(mockOrganization);
      when(mockOBDal.getProxy(Client.class, "0")).thenReturn(mockClient);

      doNothing().when(mockOBDal).commitAndClose();

      try {
        Method registerServiceMethod = clusterServiceThread.getClass().getDeclaredMethod("registerService",
            String.class);
        registerServiceMethod.setAccessible(true);

        ADClusterService result = (ADClusterService) registerServiceMethod.invoke(clusterServiceThread, SERVICE_NAME);

        verify(mockADClusterService).setOrganization(any());
        verify(mockADClusterService).setClient(any());
        verify(mockADClusterService).setService(anyString());

        verify(mockADClusterService).setNodeID(anyString());
        verify(mockADClusterService).setNodeName(anyString());

        verify(mockOBDal).save(any());
        assertEquals(mockADClusterService, result);

      } catch (Exception e) {
        fail("Error using reflection: " + e.getMessage());
      }
    }
  }

  /**
   * Tests the `doPingRound` method.
   * Verifies that the method calculates the correct sleep time for the next ping round.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testDoPingRound() throws Exception {
    try (MockedStatic<OBDal> ignored = mockStatic(OBDal.class); MockedStatic<OBContext> obContextMock = mockStatic(
        OBContext.class)) {

      obContextMock.when(OBContext::getOBContext).thenReturn(mockOBContext);
      when(mockOBContext.isInAdministratorMode()).thenReturn(false);
      when(mockOBContext.getUser()).thenReturn(mockUser);
      when(mockUser.getId()).thenReturn("0");

      when(mockClusterService1.getServiceName()).thenReturn(SERVICE_NAME);
      when(mockClusterService1.isAlive()).thenReturn(true);
      when(mockClusterService1.isInitialized()).thenReturn(true);
      when(mockClusterService1.isDisabled()).thenReturn(false);

      long currentTime = System.currentTimeMillis();
      when(mockClusterService1.getNextPing()).thenReturn(currentTime + 5000L);
      when(mockClusterService1.getTimeout()).thenReturn(5000L);

      when(mockManager.getClusterServices()).thenReturn(List.of(mockClusterService1));

      Method doPingRoundMethod = clusterServiceThread.getClass().getDeclaredMethod("doPingRound");
      doPingRoundMethod.setAccessible(true);

      Long result = (Long) doPingRoundMethod.invoke(clusterServiceThread);

      assertTrue(result >= 4900 && result <= 5100, "Expected value around 5000, but it was: " + result);

      verify(mockClusterService1, never()).setUseCache(anyBoolean());
    }
  }

  /**
   * Tests the `getService` method.
   * Verifies that the method retrieves the correct service from the database.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testGetService() throws Exception {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.createCriteria(ADClusterService.class)).thenReturn(mockCriteria);
      when(mockCriteria.add(any())).thenReturn(mockCriteria);
      when(mockCriteria.uniqueResult()).thenReturn(mockADClusterService);

      Method method = clusterServiceThread.getClass().getDeclaredMethod("getService", String.class);
      method.setAccessible(true);
      ADClusterService result = (ADClusterService) method.invoke(clusterServiceThread, SERVICE_NAME);

      assertEquals(mockADClusterService, result);
      verify(mockOBDal).createCriteria(ADClusterService.class);
      verify(mockCriteria).add(any());
      verify(mockCriteria).uniqueResult();
    }
  }

  /**
   * Tests the `shouldReplaceNodeOfService` method.
   * Verifies that the method correctly determines whether a node should be replaced.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testShouldReplaceNodeOfService() throws Exception {
    Date oldDate = new Date(System.currentTimeMillis() - 10000);
    when(mockADClusterService.getUpdated()).thenReturn(oldDate);

    Method method = clusterServiceThread.getClass().getDeclaredMethod("shouldReplaceNodeOfService",
        ADClusterService.class, Long.class);
    method.setAccessible(true);
    boolean shouldReplace = (boolean) method.invoke(clusterServiceThread, mockADClusterService, 5000L);

    assertTrue(shouldReplace);

    boolean shouldNotReplace = (boolean) method.invoke(clusterServiceThread, mockADClusterService, 15000L);

    assertFalse(shouldNotReplace);
  }

  /**
   * Tests the `updateNodeOfService` method.
   * Verifies that the method updates the node responsible for a service correctly.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUpdateNodeOfService() throws Exception {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      Date now = new Date();
      Date formerUpdate = new Date(now.getTime() - 10000);
      String formerNodeId = "old-node-id";

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockSession);
      when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
      when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

      when(mockQuery.executeUpdate()).thenReturn(1);

      when(mockADClusterService.getNodeID()).thenReturn(formerNodeId);
      when(mockADClusterService.getUpdated()).thenReturn(formerUpdate);

      setPrivateField(mockManager, NODE_ID_PARAM, NODE_ID);
      setPrivateField(mockManager, "nodeName", NODE_NAME);

      Method updateNodeOfServiceMethod = clusterServiceThread.getClass().getDeclaredMethod("updateNodeOfService",
          ADClusterService.class, String.class, Date.class);
      updateNodeOfServiceMethod.setAccessible(true);

      updateNodeOfServiceMethod.invoke(clusterServiceThread, mockADClusterService, SERVICE_NAME, now);

      verify(mockADClusterService).getNodeID();
      verify(mockADClusterService).getUpdated();
      verify(mockOBDal).getSession();
      verify(mockSession).createQuery(anyString());

      verify(mockQuery).setParameter("newNodeId", NODE_ID);
      verify(mockQuery).setParameter("newNodeName", NODE_NAME);
      verify(mockQuery).setParameter("updated", now);
      verify(mockQuery).setParameter("service", SERVICE_NAME);
      verify(mockQuery).setParameter("formerNodeId", formerNodeId);
      verify(mockQuery).setParameter("formerUpdate", formerUpdate);
      verify(mockQuery).executeUpdate();

      when(mockQuery.executeUpdate()).thenReturn(0);

      reset(mockQuery);
      when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);

      updateNodeOfServiceMethod.invoke(clusterServiceThread, mockADClusterService, SERVICE_NAME, now);

      verify(mockQuery).executeUpdate();
    }
  }

  /**
   * Tests the `updateLastPing` method.
   * Verifies that the method updates the last ping timestamp for a service correctly.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testUpdateLastPing() throws Exception {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      Date now = new Date();

      obDalMock.when(OBDal::getInstance).thenReturn(mockOBDal);
      when(mockOBDal.getSession()).thenReturn(mockSession);
      when(mockSession.createQuery(anyString())).thenReturn(mockQuery);
      when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
      when(mockQuery.executeUpdate()).thenReturn(1);

      setPrivateField(mockManager, NODE_ID_PARAM, NODE_ID);

      Method updateLastPingMethod = clusterServiceThread.getClass().getDeclaredMethod("updateLastPing", String.class,
          Date.class);
      updateLastPingMethod.setAccessible(true);

      updateLastPingMethod.invoke(clusterServiceThread, SERVICE_NAME, now);

      verify(mockOBDal).getSession();
      verify(mockSession).createQuery(anyString());

      verify(mockQuery).setParameter("updated", now);
      verify(mockQuery).setParameter("service", SERVICE_NAME);
      verify(mockQuery).setParameter("currentNodeId", NODE_ID);
      verify(mockQuery).executeUpdate();
    }
  }

  /**
   * Sets a private field to a specified value using reflection.
   *
   * @param object
   *     the object containing the field
   * @param fieldName
   *     the name of the field
   * @param value
   *     the value to set
   * @throws Exception
   *     if an error occurs during reflection
   */
  private void setPrivateField(Object object, String fieldName, Object value) throws Exception {
    Field field = object.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(object, value);
  }
}
