package org.openbravo.cluster;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ExecutorService;

import javax.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.SequenceIdData;
import org.openbravo.jmx.MBeanRegistry;
import org.openbravo.model.ad.system.ADClusterService;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.hibernate.criterion.Criterion;

@ExtendWith(MockitoExtension.class)
public class ClusterServiceManagerTest {

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

  private static final String NODE_ID = "test-node-id";
  private static final String NODE_NAME = "test-node-name";

  @BeforeEach
  public void setUp() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, "isCluster", true);

    setPrivateField(serviceManager, "nodeId", NODE_ID);
    setPrivateField(serviceManager, "nodeName", NODE_NAME);
    setPrivateField(serviceManager, "isShutDown", false);
    setPrivateField(serviceManager, "executorService", mockExecutorService);
  }

  @Test
  public void testStartInNonClusterMode() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, "isCluster", false);

    serviceManager.start();

    verifyNoInteractions(mockExecutorService);
  }

  @Test
  public void testShutdownInClusterMode() {
    try (MockedStatic<OBContext> obContext = mockStatic(OBContext.class);
         MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

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

  @Test
  public void testShutdownInNonClusterMode() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, "isCluster", false);

    serviceManager.shutdown();

    verifyNoInteractions(mockExecutorService);
  }

  @Test
  public void testGetClusterServices() {
    when(mockClusterServices.iterator()).thenReturn(Collections.singletonList(mockClusterService).iterator());

    Iterable<ClusterService> result = serviceManager.getClusterServices();

    assertNotNull(result);
    assertTrue(result.iterator().hasNext());
    assertEquals(mockClusterService, result.iterator().next());
  }

  @Test
  public void testGetCurrentNodeId() {
    assertEquals(NODE_ID, serviceManager.getCurrentNodeId());
  }

  @Test
  public void testGetCurrentNodeName() {
    assertEquals(NODE_NAME, serviceManager.getCurrentNodeName());
  }

  @Test
  public void testGetLastPing() {
    Date lastPing = new Date();
    setPrivateField(serviceManager, "lastPing", lastPing);

    assertEquals(lastPing, serviceManager.getLastPing());
  }

  @Test
  public void testIsCluster() throws Exception {
    setPrivateStaticField(ClusterServiceManager.class, "isCluster", true);
    assertTrue(ClusterServiceManager.isCluster());

    setPrivateStaticField(ClusterServiceManager.class, "isCluster", false);
    assertFalse(ClusterServiceManager.isCluster());
  }

  private void setPrivateField(Object target, String fieldName, Object value) {
    try {
      Field field = ClusterServiceManager.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      fail("Error al establecer el campo privado: " + fieldName + " - " + e.getMessage());
    }
  }

  private Object getPrivateField(Object target, String fieldName) {
    try {
      Field field = ClusterServiceManager.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      return field.get(target);
    } catch (Exception e) {
      fail("Error al obtener el campo privado: " + fieldName + " - " + e.getMessage());
      return null;
    }
  }

  private static void setPrivateStaticField(Class<?> clazz, String fieldName, Object value)
      throws Exception {
    Field field = clazz.getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(null, value);
  }
}