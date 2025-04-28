package org.openbravo.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.ADClusterService;

/**
 * Unit tests for JmxClusterServiceManager.
 */
@ExtendWith(MockitoExtension.class)
public class JmxClusterServiceManagerTest {
  public static final String NODE_1 = "Node1";
  public static final String TEST_SERVICE = "TestService";
  public static final String SERVICE_1 = "Service1";


  @Mock
  private ClusterServiceManager mockClusterServiceManager;

  @Mock
  private ClusterService mockClusterService;

  private MockedStatic<OBContext> obContextMock;
  private MockedStatic<OBDal> obDalMock;

  @InjectMocks
  private JmxClusterServiceManager manager;


  /**
   * Sets up the test environment before each test.
   * Initialize mocks and static contexts.
   */
  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    obContextMock = mockStatic(OBContext.class);
    obDalMock = mockStatic(OBDal.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static contexts to release resources.
   */
  @AfterEach
  public void tearDown() {
    obContextMock.close();
    obDalMock.close();
  }

  /**
   * Tests the {@code getCurrentNodeId} method.
   * Verifies that the correct node ID is returned.
   */
  @Test
  public void testGetCurrentNodeId() {
    when(mockClusterServiceManager.getCurrentNodeId()).thenReturn(NODE_1);

    String nodeId = manager.getCurrentNodeId();

    assertEquals(NODE_1, nodeId);
  }

  /**
   * Tests the {@code getClusterServiceLeaders} method.
   * Verifies that the correct cluster service leaders are retrieved.
   */
  @Test
  public void testGetClusterServiceLeaders() {
    OBDal mockedOBDal = mock(OBDal.class);
    OBCriteria<ADClusterService> mockCriteria = mock(OBCriteria.class);
    ADClusterService mockService = mock(ADClusterService.class);
    Map<String, String> expectedLeaders = new HashMap<>();
    expectedLeaders.put(SERVICE_1, "leader ID: Node1, leader name: Leader1, last ping: null");

    obDalMock.when(OBDal::getInstance).thenReturn(mockedOBDal);
    when(mockedOBDal.createCriteria(ADClusterService.class)).thenReturn(mockCriteria);
    when(mockCriteria.list()).thenReturn(Collections.singletonList(mockService));
    when(mockService.getNodeID()).thenReturn(NODE_1);
    when(mockService.getNodeName()).thenReturn("Leader1");
    when(mockService.getService()).thenReturn(SERVICE_1);

    Map<String, String> leaders = manager.getClusterServiceLeaders();

    assertEquals(expectedLeaders, leaders);
    verify(mockedOBDal).commitAndClose();
  }

  /**
   * Tests the {@code enablePingForService} method when the service exists.
   * Verifies that the ping is enabled for the service.
   */
  @Test
  public void testEnablePingForServiceServiceExists() {
    when(mockClusterServiceManager.getClusterServices()).thenReturn(Collections.singletonList(mockClusterService));
    when(mockClusterService.getServiceName()).thenReturn(TEST_SERVICE);
    when(mockClusterService.isDisabled()).thenReturn(true);

    manager.enablePingForService(TEST_SERVICE);

    verify(mockClusterService).setDisabled(false);
  }

  /**
   * Tests the {@code enablePingForService} method when the service does not exist.
   * Verifies that no action is taken.
   */
  @Test
  public void testEnablePingForServiceServiceNotExists() {
    when(mockClusterServiceManager.getClusterServices()).thenReturn(Collections.emptyList());

    manager.enablePingForService("NonExistentService");

    verify(mockClusterService, never()).setDisabled(false);
  }

  /**
   * Tests the {@code disablePingForService} method when the service exists.
   * Verifies that the ping is disabled for the service.
   */
  @Test
  public void testDisablePingForServiceServiceExists() {
    when(mockClusterServiceManager.getClusterServices()).thenReturn(Collections.singletonList(mockClusterService));
    when(mockClusterService.getServiceName()).thenReturn(TEST_SERVICE);
    when(mockClusterService.isDisabled()).thenReturn(false);

    manager.disablePingForService(TEST_SERVICE);

    verify(mockClusterService).deregister();
  }

  /**
   * Tests the {@code disablePingForService} method when the service does not exist.
   * Verifies that no action is taken.
   */
  @Test
  public void testDisablePingForServiceServiceNotExists() {
    when(mockClusterServiceManager.getClusterServices()).thenReturn(Collections.emptyList());

    manager.disablePingForService("NonExistentService");

    verify(mockClusterService, never()).setDisabled(true);
  }

  /**
   * Tests the {@code getClusterServiceSettings} method when services are available.
   * Verifies that the correct settings are retrieved for each service.
   */
  @Test
  public void testGetClusterServiceSettingsWithServices() {
    ClusterService service1 = mock(ClusterService.class);
    ClusterService service2 = mock(ClusterService.class);

    when(service1.getServiceName()).thenReturn(SERVICE_1);
    when(service1.getTimeout()).thenReturn(5000L);
    when(service2.getServiceName()).thenReturn("Service2");
    when(service2.getTimeout()).thenReturn(3000L);

    when(mockClusterServiceManager.getClusterServices()).thenReturn(Arrays.asList(service1, service2));

    Map<String, String> expectedSettings = new HashMap<>();
    expectedSettings.put(SERVICE_1, "timeout: 5000 milliseconds");
    expectedSettings.put("Service2", "timeout: 3000 milliseconds");

    Map<String, String> result = manager.getClusterServiceSettings();

    assertEquals(expectedSettings, result);

    verify(mockClusterServiceManager).getClusterServices();
    verify(service1).getServiceName();
    verify(service1).getTimeout();
    verify(service2).getServiceName();
    verify(service2).getTimeout();
  }

  /**
   * Tests the {@code getClusterServiceSettings} method when no services are available.
   * Verifies that an empty map is returned.
   */
  @Test
  public void testGetClusterServiceSettingsNoServices() {
    when(mockClusterServiceManager.getClusterServices()).thenReturn(Collections.emptyList());

    Map<String, String> result = manager.getClusterServiceSettings();

    assertTrue(result.isEmpty());
    verify(mockClusterServiceManager).getClusterServices();
  }
}
