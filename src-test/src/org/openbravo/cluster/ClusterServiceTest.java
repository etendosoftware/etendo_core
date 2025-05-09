package org.openbravo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for the {@link ClusterService} class.
 * Verifies the behavior of various methods under different scenarios, including initialization,
 * handling in the current node, processing state, and deregistration.
 */
@ExtendWith(MockitoExtension.class)
class ClusterServiceTest {

  private static final String NODE_ID = "node123";
  private static final String NODE_NAME = "testNode";
  private static final String SERVICE_NAME = "TestService";

  private TestClusterService clusterService;

  /**
   * Sets up the test environment before each test.
   * Initializes the test implementation of {@link ClusterService}.
   */
  @BeforeEach
  void setUp() {
    clusterService = new TestClusterService();

  }

  /**
   * Tests the initialization of the service when it is enabled.
   * Verifies that the service is initialized successfully.
   */
  @Test
  void testInitServiceEnabled() {
    clusterService.setEnabled(true);

    boolean result = clusterService.init(NODE_ID, NODE_NAME);

    assertTrue(result);
    assertTrue(clusterService.isInitialized());
  }

  /**
   * Tests the initialization of the service when it is disabled.
   * Verifies that the service is not initialized.
   */
  @Test
  void testInitServiceDisabled() {
    clusterService.setEnabled(false);

    boolean result = clusterService.init(NODE_ID, NODE_NAME);

    assertFalse(result);
    assertFalse(clusterService.isInitialized());
  }

  /**
   * Tests the behavior of `isHandledInCurrentNode` when not in a clustered environment.
   * Verifies that the method always returns {@code true}.
   */
  @Test
  void testIsHandledInCurrentNodeNotInCluster() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(false);

      assertTrue(clusterService.isHandledInCurrentNode());
    }
  }

  /**
   * Tests the behavior of `isHandledInCurrentNode` when the service is not initialized.
   * Verifies that the method returns {@code false}.
   */
  @Test
  void testIsHandledInCurrentNodeNotInitialized() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);

      assertFalse(clusterService.isHandledInCurrentNode());
    }
  }

  /**
   * Tests the behavior of `isHandledInCurrentNode` when the cache is expired.
   * Verifies that the method retrieves the node handling the service from the database.
   */
  @Test
  void testIsHandledInCurrentNodeCacheExpired() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(
        ClusterServiceManager.class); MockedStatic<ClusterServiceData> mockedData = mockStatic(
        ClusterServiceData.class)) {

      String nodeId = NODE_ID;
      ClusterServiceData[] nodeInfo = new ClusterServiceData[1];
      nodeInfo[0] = new ClusterServiceData();
      nodeInfo[0].nodeId = nodeId;
      nodeInfo[0].nodeName = NODE_NAME;

      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);
      mockedData.when(() -> ClusterServiceData.getNodeHandlingService(any(), any(), eq(SERVICE_NAME))).thenReturn(
          nodeInfo);

      clusterService.init(nodeId, NODE_NAME);
      clusterService.setNextPing(new Date().getTime() - 20000L);

      boolean result = clusterService.isHandledInCurrentNode();

      assertTrue(result);
    }
  }

  /**
   * Tests the behavior of `isHandledInCurrentNode` when the service is handled by a different node.
   * Verifies that the method returns {@code false}.
   */
  @Test
  void testIsHandledInCurrentNodeDifferentNodeHandling() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(
        ClusterServiceManager.class); MockedStatic<ClusterServiceData> mockedData = mockStatic(
        ClusterServiceData.class)) {

      String nodeId = NODE_ID;
      ClusterServiceData[] nodeInfo = new ClusterServiceData[1];
      nodeInfo[0] = new ClusterServiceData();
      nodeInfo[0].nodeId = "differentNode";
      nodeInfo[0].nodeName = "otherNode";

      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);
      mockedData.when(() -> ClusterServiceData.getNodeHandlingService(any(), any(), eq(SERVICE_NAME))).thenReturn(
          nodeInfo);

      clusterService.init(nodeId, NODE_NAME);
      clusterService.setNextPing(new Date().getTime() - 20000L);

      boolean result = clusterService.isHandledInCurrentNode();

      assertFalse(result);
    }
  }

  /**
   * Tests the behavior of `startProcessing` and `endProcessing` methods.
   * Verifies that the processing count is updated correctly.
   */
  @Test
  void testStartAndEndProcessing() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);

      clusterService.startProcessing();
      clusterService.startProcessing();

      int processingCount = getProcessingCount(clusterService);

      assertEquals(2, processingCount);

      clusterService.endProcessing();
      processingCount = getProcessingCount(clusterService);

      assertEquals(1, processingCount);

      clusterService.endProcessing();
      processingCount = getProcessingCount(clusterService);

      assertEquals(0, processingCount);
    }
  }

  /**
   * Tests the behavior of `startProcessing` when the service is not in a clustered environment.
   * Verifies that the processing count remains zero.
   */
  @Test
  void testStartProcessingNotInCluster() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(false);

      clusterService.startProcessing();

      int processingCount = getProcessingCount(clusterService);
      assertEquals(0, processingCount);
    }
  }

  /**
   * Tests the behavior of `deregister` when the service is processing.
   * Verifies that the deregistration is postponed until processing finishes.
   */
  @Test
  void testDeregisterWhileProcessing() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(
        ClusterServiceManager.class); MockedStatic<ClusterServiceData> mockedData = mockStatic(
        ClusterServiceData.class)) {

      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);

      clusterService.init(NODE_ID, NODE_NAME);
      clusterService.startProcessing(); // Set processing state

      clusterService.deregister();

      boolean disableAfterProcess = getDisableAfterProcess(clusterService);
      assertTrue(disableAfterProcess);

      mockedData.verify(() -> ClusterServiceData.deregisterService(any(), any(), any(), any()), never());
    }
  }

  /**
   * Tests the behavior of `deregister` when the service is not processing.
   * Verifies that the deregistration is performed immediately.
   */
  @Test
  void testDeregisterNotProcessing() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(
        ClusterServiceManager.class); MockedStatic<ClusterServiceData> mockedData = mockStatic(
        ClusterServiceData.class)) {

      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);
      mockedData.when(
          () -> ClusterServiceData.deregisterService(any(), any(), eq(SERVICE_NAME), eq(NODE_ID))).thenReturn(1);

      clusterService.init(NODE_ID, NODE_NAME);

      clusterService.deregister();

      assertTrue(clusterService.isDisabled());
      mockedData.verify(() -> ClusterServiceData.deregisterService(any(), any(), eq(SERVICE_NAME), eq(NODE_ID)),
          times(1));
    }
  }

  /**
   * Tests the behavior of `endProcessing` when the deregister flag is set.
   */
  @Test
  void testEndProcessingWithDeregisterFlag() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(
        ClusterServiceManager.class); MockedStatic<ClusterServiceData> mockedData = mockStatic(
        ClusterServiceData.class)) {

      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);
      mockedData.when(
          () -> ClusterServiceData.deregisterService(any(), any(), eq(SERVICE_NAME), eq(NODE_ID))).thenReturn(1);

      clusterService.init(NODE_ID, NODE_NAME);
      clusterService.startProcessing();

      setDisableAfterProcess(clusterService, true);

      clusterService.endProcessing();

      assertTrue(clusterService.isDisabled());
      mockedData.verify(() -> ClusterServiceData.deregisterService(any(), any(), eq(SERVICE_NAME), eq(NODE_ID)),
          times(1));
    }
  }

  /**
   * Tests the `toString` method.
   * Verifies that the string representation contains the service name, node name, and node ID.
   */
  @Test
  void testToString() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);
      clusterService.init(NODE_ID, NODE_NAME);

      String result = clusterService.toString();

      assertTrue(result.contains(SERVICE_NAME));
      assertTrue(result.contains(NODE_NAME));
      assertTrue(result.contains(NODE_ID));
    }
  }

  /**
   * Tests the behavior of `prepareForNewNodeInCharge` when the service is processing.
   * Verifies that the processing count is reset and the deregister flag is cleared.
   */
  @Test
  void testPrepareForNewNodeInCharge() {
    try (MockedStatic<ClusterServiceManager> mockedManager = mockStatic(ClusterServiceManager.class)) {
      mockedManager.when(ClusterServiceManager::isCluster).thenReturn(true);

      clusterService.startProcessing();
      setDisableAfterProcess(clusterService, true);

      clusterService.prepareForNewNodeInCharge();

      int processingCount = getProcessingCount(clusterService);
      boolean disableAfterProcess = getDisableAfterProcess(clusterService);

      assertEquals(0, processingCount);
      assertFalse(disableAfterProcess);
    }
  }

  /**
   * Tests the behavior of `isAlive` method.
   * Verifies that the method returns {@code true} when the service is alive.
   */
  private int getProcessingCount(ClusterService service) {
    try {
      var field = ClusterService.class.getDeclaredField("processing");
      field.setAccessible(true);
      return (int) field.get(service);
    } catch (Exception e) {
      fail("Failed to access processing field: " + e.getMessage());
      return -1;
    }
  }

  /**
   * Tests the behavior of `isAlive` method.
   * Verifies that the method returns {@code true} when the service is alive.
   */
  private boolean getDisableAfterProcess(ClusterService service) {
    try {
      var field = ClusterService.class.getDeclaredField("disableAfterProcess");
      field.setAccessible(true);
      return (boolean) field.get(service);
    } catch (Exception e) {
      fail("Failed to access disableAfterProcess field: " + e.getMessage());
      return false;
    }
  }

  /**
   * Sets the `disableAfterProcess` field to the specified value.
   * Used for testing purposes to control the state of the service.
   */
  private void setDisableAfterProcess(ClusterService service, boolean value) {
    try {
      var field = ClusterService.class.getDeclaredField("disableAfterProcess");
      field.setAccessible(true);
      field.set(service, value);
    } catch (Exception e) {
      fail("Failed to set disableAfterProcess field: " + e.getMessage());
    }
  }

  /**
   * Test implementation of the {@link ClusterService} class for testing purposes.
   */
  private class TestClusterService extends ClusterService {
    private boolean enabled = true;

    @Override
    protected String getServiceName() {
      return SERVICE_NAME;
    }

    @Override
    protected boolean isAlive() {
      return true;
    }

    @Override
    protected boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
