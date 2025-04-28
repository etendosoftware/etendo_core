package org.openbravo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.Instance;

import org.hibernate.criterion.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.ADClusterService;

/**
 * Unit tests for the {@link ClusterServiceManager} class.
 * Verifies the behavior of cluster service management methods under various scenarios,
 * including cluster and non-cluster modes, service retrieval, and shutdown operations.
 */
@ExtendWith(MockitoExtension.class)
public class ClusterServiceManagerTest {

  private static final String NODE_ID = "test-node-id";
  private static final String NODE_NAME = "test-node-name";
  public static final String IS_CLUSTER = "isCluster";

  @Mock
  private Instance<ClusterService> mockClusterServices;
  @Mock
  private ClusterService mockClusterService;
  @Mock
  private OBPropertiesProvider propertiesProvider;
  @Mock
  private OBProvider obProvider;
  @Mock
  private OBDal obDal;
  @Mock
  private OBCriteria<ADClusterService> mockCriteria;
  @Mock
  private ADClusterService mockADClusterService;
  @Mock
  private ExecutorService mockExecutorService;
  @InjectMocks
  private ClusterServiceManager serviceManager;

  /**
   * Sets a private static field in the target class.
   *
   * @param clazz
   *     the target class
   * @param fieldName
   *     the name of the field
   * @param value
   *     the value to set
   * @throws Exception
   *     if an error occurs during the operation
   */
  private static void setPrivateStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }

  /**
   * Sets up the test environment before each test.
   * Initializes private fields and mocks.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, IS_CLUSTER, true);

    setPrivateField(serviceManager, "nodeId", NODE_ID);
    setPrivateField(serviceManager, "nodeName", NODE_NAME);
    setPrivateField(serviceManager, "isShutDown", false);
    setPrivateField(serviceManager, "executorService", mockExecutorService);
  }

  /**
   * Tests the {@code start} method in non-cluster mode.
   * Verifies that no interactions occur with the executor service.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testStartInNonClusterMode() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, IS_CLUSTER, false);

    serviceManager.start();

    verifyNoInteractions(mockExecutorService);
  }

  /**
   * Tests the {@code shutdown} method in cluster mode.
   * Verifies that the executor service is shut down and cluster services are removed.
   */
  @Test
  public void testShutdownInClusterMode() {
    try (MockedStatic<OBContext> obContext = mockStatic(OBContext.class); MockedStatic<OBDal> obDalStatic = mockStatic(
        OBDal.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.createCriteria(ADClusterService.class)).thenReturn(mockCriteria);
      when(mockCriteria.add(any(Criterion.class))).thenReturn(mockCriteria);
      when(mockCriteria.list()).thenReturn(Collections.singletonList(mockADClusterService));

      serviceManager.shutdown();

      verify(mockExecutorService).shutdownNow();
      verify(obDal).remove(mockADClusterService);
      verify(obDal).commitAndClose();
      obContext.verify(() -> OBContext.setAdminMode(false));
      obContext.verify(OBContext::restorePreviousMode);

      assertTrue((boolean) getPrivateField(serviceManager, "isShutDown"));
    }
  }

  /**
   * Tests the {@code shutdown} method in non-cluster mode.
   * Verifies that no interactions occur with the executor service.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testShutdownInNonClusterMode() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, IS_CLUSTER, false);

    serviceManager.shutdown();

    verifyNoInteractions(mockExecutorService);
  }

  /**
   * Tests the {@code getClusterServices} method.
   * Verifies that the correct cluster services are retrieved.
   */
  @Test
  public void testGetClusterServices() {
    when(mockClusterServices.iterator()).thenReturn(Collections.singletonList(mockClusterService).iterator());

    Iterable<ClusterService> result = serviceManager.getClusterServices();

    assertNotNull(result);
    assertTrue(result.iterator().hasNext());
    assertEquals(mockClusterService, result.iterator().next());
  }

  /**
   * Tests the {@code getCurrentNodeId} method.
   * Verifies that the correct node ID is returned.
   */
  @Test
  public void testGetCurrentNodeId() {
    assertEquals(NODE_ID, serviceManager.getCurrentNodeId());
  }

  /**
   * Tests the {@code getCurrentNodeName} method.
   * Verifies that the correct node name is returned.
   */
  @Test
  public void testGetCurrentNodeName() {
    assertEquals(NODE_NAME, serviceManager.getCurrentNodeName());
  }

  /**
   * Tests the {@code getLastPing} method.
   * Verifies that the correct last ping date is returned.
   */
  @Test
  public void testGetLastPing() {
    Date lastPing = new Date();
    setPrivateField(serviceManager, "lastPing", lastPing);

    assertEquals(lastPing, serviceManager.getLastPing());
  }

  /**
   * Tests the {@code isCluster} method.
   * Verifies that the cluster mode is correctly determined.
   *
   * @throws Exception
   *     if an error occurs during the test
   */
  @Test
  public void testIsCluster() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, IS_CLUSTER, true);
    assertTrue(ClusterServiceManager.isCluster());

    setPrivateStaticField(ClusterServiceManager.class, IS_CLUSTER, false);
    assertFalse(ClusterServiceManager.isCluster());
  }

  /**
   * Sets a private field in the target object.
   *
   * @param target
   *     the target object
   * @param fieldName
   *     the name of the field
   * @param value
   *     the value to set
   */
  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = ClusterServiceManager.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      fail("Error setting the private field: " + fieldName + " - " + e.getMessage());
    }
  }

  /**
   * Gets the value of a private field in the target object.
   *
   * @param target
   *     the target object
   * @param fieldName
   *     the name of the field
   * @return the value of the field
   */
  private Object getPrivateField(Object target, String fieldName) {
    try {
      Field field = ClusterServiceManager.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (Exception e) {
      fail("Error setting the private field: " + fieldName + " - " + e.getMessage());
      return null;
    }
  }
}
