package org.openbravo.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openbravo.base.util.Check.fail;

import java.lang.reflect.Field;
import java.util.Properties;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.base.ConfigParameters;
import org.openbravo.base.ConnectionProviderContextListener;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.quartz.OpenbravoPersistentJobStore;
import org.openbravo.scheduling.trigger.TriggerProvider;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.spi.JobStore;

/**
 * Unit tests for the OBScheduler class, focusing on MBean interface functionality.
 * Tests the start, standby and status check operations of the scheduler.
 */
public class OBSchedulerTest {

  private static final String TEST_USER = "testUser";

  // Objects we need to mock
  private Scheduler mockScheduler;
  private ConnectionProvider mockConnectionProvider;
  private JobDetail mockJobDetail;
  private Trigger mockTrigger;
  private SchedulerMetaData mockMetaData;
  private Properties mockProperties;

  // Static mocks
  private MockedStatic<OBPropertiesProvider> staticOBPropertiesProvider;
  private MockedStatic<ProcessRequestData> staticProcessRequestData;
  private MockedStatic<JobDetailProvider> staticJobDetailProvider;
  private MockedStatic<TriggerProvider> staticTriggerProvider;
  private MockedStatic<OpenbravoPersistentJobStore> staticOpenbravoPersistentJobStore;
  private MockedStatic<SchedulerTimeUtils> staticSchedulerTimeUtils;

  // Instance to test - note that OBScheduler is a singleton
  private OBScheduler obScheduler;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks, configures mock behavior, and sets up the OBScheduler singleton instance.
   *
   * @throws Exception
   *     if any error occurs during setup
   */
  @Before
  public void setUp() throws Exception {
    // Initialize normal mocks
    mockScheduler = mock(Scheduler.class);
    SchedulerContext mockContext = mock(SchedulerContext.class);
    mockConnectionProvider = mock(ConnectionProvider.class);
    ConfigParameters mockConfigParameters = mock(ConfigParameters.class);
    mockJobDetail = mock(JobDetail.class);
    mockTrigger = mock(Trigger.class);
    ListenerManager mockListenerManager = mock(ListenerManager.class);
    mockMetaData = mock(SchedulerMetaData.class);
    OBPropertiesProvider mockPropertiesProvider = mock(OBPropertiesProvider.class);
    mock(JobStore.class);
    mockProperties = mock(Properties.class);

    // Configure static mocks
    staticOBPropertiesProvider = mockStatic(OBPropertiesProvider.class);
    staticProcessRequestData = mockStatic(ProcessRequestData.class);
    staticJobDetailProvider = mockStatic(JobDetailProvider.class);
    staticTriggerProvider = mockStatic(TriggerProvider.class);
    staticOpenbravoPersistentJobStore = mockStatic(OpenbravoPersistentJobStore.class);
    staticSchedulerTimeUtils = mockStatic(SchedulerTimeUtils.class);

    // Configure mock behavior
    staticOBPropertiesProvider.when(OBPropertiesProvider::getInstance).thenReturn(mockPropertiesProvider);
    when(mockPropertiesProvider.getOpenbravoProperties()).thenReturn(mockProperties);
    when(mockProperties.getProperty(eq("background.policy"), anyString())).thenReturn("default");

    when(mockScheduler.getContext()).thenReturn(mockContext);
    when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);
    doNothing().when(mockListenerManager).addSchedulerListener(any());
    doNothing().when(mockListenerManager).addJobListener(any());
    doNothing().when(mockListenerManager).addTriggerListener(any());

    when(mockScheduler.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.isJobStoreSupportsPersistence()).thenReturn(true);

    when(mockContext.get(ConnectionProviderContextListener.POOL_ATTRIBUTE)).thenReturn(mockConnectionProvider);
    when(mockContext.get(ConfigParameters.CONFIG_ATTRIBUTE)).thenReturn(mockConfigParameters);

    when(mockConfigParameters.getSqlDateTimeFormat()).thenReturn("DD-MM-YYYY HH24:MI:SS");
    when(mockConfigParameters.getJavaDateTimeFormat()).thenReturn("dd-MM-yyyy HH:mm:ss");

    JobDetailProvider mockJobDetailProviderInstance = mock(JobDetailProvider.class);
    staticJobDetailProvider.when(JobDetailProvider::getInstance).thenReturn(mockJobDetailProviderInstance);
    when(mockJobDetailProviderInstance.createJobDetail(any(), anyString(), any())).thenReturn(mockJobDetail);

    TriggerProvider mockTriggerProviderInstance = mock(TriggerProvider.class);
    staticTriggerProvider.when(TriggerProvider::getInstance).thenReturn(mockTriggerProviderInstance);
    when(mockTriggerProviderInstance.createTrigger(anyString(), any(), any())).thenReturn(mockTrigger);

    staticSchedulerTimeUtils.when(() -> SchedulerTimeUtils.currentDate(anyString())).thenReturn("01-01-2023 12:00:00");

    // Get the OBScheduler instance (it's a singleton)
    obScheduler = OBScheduler.getInstance();

    Field schedField = OBScheduler.class.getDeclaredField("sched");
    schedField.setAccessible(true);
    schedField.set(obScheduler, mockScheduler);

    Field ctxField = OBScheduler.class.getDeclaredField("ctx");
    ctxField.setAccessible(true);
    ctxField.set(obScheduler, mockContext);

    Field sqlDateTimeFormatField = OBScheduler.class.getDeclaredField("sqlDateTimeFormat");
    sqlDateTimeFormatField.setAccessible(true);
    sqlDateTimeFormatField.set(obScheduler, mockConfigParameters.getSqlDateTimeFormat());

    Field initializingField = OBScheduler.class.getDeclaredField("initializing");
    initializingField.setAccessible(true);
    initializingField.set(obScheduler, false);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all static mocks to prevent memory leaks.
   * <p>
   * Static mocks need to be explicitly closed since they're not automatically closed by Mockito.
   */
  @After
  public void tearDown() {
    // Close static mocks to prevent memory leaks
    if (staticOBPropertiesProvider != null) staticOBPropertiesProvider.close();
    if (staticProcessRequestData != null) staticProcessRequestData.close();
    if (staticJobDetailProvider != null) staticJobDetailProvider.close();
    if (staticTriggerProvider != null) staticTriggerProvider.close();
    if (staticOpenbravoPersistentJobStore != null) staticOpenbravoPersistentJobStore.close();
    if (staticSchedulerTimeUtils != null) staticSchedulerTimeUtils.close();
  }

  /**
   * Tests the singleton pattern implementation of OBScheduler.
   * Verifies that multiple calls to getInstance return the same instance.
   */
  @Test
  public void testGetInstance() {
    // Verify that getInstance always returns the same instance
    OBScheduler instance1 = OBScheduler.getInstance();
    OBScheduler instance2 = OBScheduler.getInstance();
    assertEquals("getInstance should always return the same instance", instance1, instance2);
  }

  /**
   * Tests the unschedule operation.
   * Verifies that a job can be properly unscheduled and marked as such in the database.
   */
  @Test
  public void testUnschedule() {
    // Prepare test data
    String requestId = "testRequestId";
    ProcessContext mockContext = mock(ProcessContext.class);

    when(mockContext.getUser()).thenReturn(TEST_USER);

    // Mock for ProcessRequestData.update
    staticProcessRequestData.when(
        () -> ProcessRequestData.update(any(), anyString(), any(), anyString(), anyString(), anyString(),
            anyString())).thenReturn(0); // Return 0 affected rows

    // Execute method under test
    obScheduler.unschedule(requestId, mockContext);

    // Verify interactions
    try {
      verify(mockScheduler).unscheduleJob(any(TriggerKey.class));
      verify(mockScheduler).deleteJob(any(JobKey.class));
    } catch (SchedulerException e) {
      fail("Should not throw an exception: " + e.getMessage());
    }

    staticProcessRequestData.verify(
        () -> ProcessRequestData.update(eq(mockConnectionProvider), eq("UNS"), eq(null), eq("DD-MM-YYYY HH24:MI:SS"),
            eq("01-01-2023 12:00:00"), eq(TEST_USER), eq(requestId)));
  }

  /**
   * Tests scheduling allowance when the scheduler is active.
   * Verifies that scheduling is allowed when the scheduler is not in standby mode.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   */
  @Test
  public void testIsSchedulingAllowedWhenActive() throws SchedulerException {
    // Configure Scheduler to be active
    when(mockScheduler.isInStandbyMode()).thenReturn(false);

    // Execute method under test
    boolean result = obScheduler.isSchedulingAllowed();

    // Verify result
    assertTrue("isSchedulingAllowed should return true when the scheduler is active", result);
  }

  /**
   * Tests scheduling allowance in a clustered environment.
   * Verifies that scheduling is allowed when in standby mode but clustered with scheduling permitted.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   */
  @Test
  public void testIsSchedulingAllowedWhenInStandbyButClusteredAndAllowed() throws SchedulerException {
    // Configure Scheduler to be in standby but in a cluster with scheduling allowed
    when(mockScheduler.isInStandbyMode()).thenReturn(true);
    when(mockMetaData.isJobStoreClustered()).thenReturn(true);
    when(mockScheduler.getSchedulerName()).thenReturn("testScheduler");

    staticOpenbravoPersistentJobStore.when(
        () -> OpenbravoPersistentJobStore.isSchedulingAllowedInCluster("testScheduler")).thenReturn(true);

    // Execute method under test
    boolean result = obScheduler.isSchedulingAllowed();

    // Verify result
    assertTrue("isSchedulingAllowed should return true when in a cluster with scheduling allowed", result);
  }

  /**
   * Tests the no-execute background policy check.
   * Verifies that the policy is correctly identified when set to "no-execute".
   */
  @Test
  public void testIsNoExecuteBackgroundPolicy() {
    // Configure the case for "no-execute"
    when(mockProperties.getProperty(eq("background.policy"), anyString())).thenReturn("no-execute");

    // Execute method under test
    boolean result = OBScheduler.isNoExecuteBackgroundPolicy();

    // Verify result
    assertTrue("isNoExecuteBackgroundPolicy should return true when the policy is 'no-execute'", result);
  }

  /**
   * Tests the scheduler start operation through MBean interface.
   * Verifies that the start method is called on the underlying Quartz scheduler.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   */
  @Test
  public void testMBeanInterfaceStartMethod() throws SchedulerException {
    // Configure Scheduler to not be started
    when(mockScheduler.isStarted()).thenReturn(false);

    // Execute method under test
    obScheduler.start();

    // Verify interaction
    verify(mockScheduler).start();
  }

  /**
   * Tests the scheduler start operation through MBean interface.
   * Verifies that the start method is called on the underlying Quartz scheduler.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   */
  @Test
  public void testMBeanInterfaceStandbyMethod() throws SchedulerException {
    // Configure Scheduler to not be in standby
    when(mockScheduler.isInStandbyMode()).thenReturn(false);

    // Execute method under test
    obScheduler.standby();

    // Verify interaction
    verify(mockScheduler).standby();
  }

  /**
   * Tests the rescheduling functionality.
   * Verifies that a job can be unscheduled and rescheduled properly.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   */
  @Test
  public void testReschedule() throws SchedulerException {
    // Prepare test data
    String requestId = "testRequestId";
    ProcessBundle mockBundle = mock(ProcessBundle.class);

    // Mock for unscheduleJob - returns boolean, don't use doNothing()
    when(mockScheduler.unscheduleJob(any(TriggerKey.class))).thenReturn(true);

    // Mock for deleteJob - returns boolean, don't use doNothing()
    when(mockScheduler.deleteJob(any(JobKey.class))).thenReturn(true);

    // Mock for checkExists
    when(mockScheduler.checkExists(any(JobKey.class))).thenReturn(false);

    // Execute method under test
    obScheduler.reschedule(requestId, mockBundle);

    // Verify interactions
    verify(mockScheduler).unscheduleJob(any(TriggerKey.class));
    verify(mockScheduler).deleteJob(any(JobKey.class));
    verify(mockScheduler).scheduleJob(mockJobDetail, mockTrigger);
  }

  /**
   * Tests scheduling a process bundle.
   * Verifies that a process bundle can be properly scheduled with all its parameters.
   *
   * @throws SchedulerException
   *     if the scheduler operation fails
   * @throws ServletException
   *     if there's an error with the servlet context
   */
  @Test
  public void testScheduleWithBundle() throws SchedulerException, ServletException {
    // Prepare test data
    ProcessBundle mockBundle = mock(ProcessBundle.class);
    ProcessContext mockContext = mock(ProcessContext.class);
    ProcessBundle.Channel mockChannel = mock(ProcessBundle.Channel.class);

    when(mockBundle.getProcessId()).thenReturn("testProcessId");
    when(mockBundle.getContext()).thenReturn(mockContext);
    when(mockBundle.getParamsDeflated()).thenReturn("deflatedParams");
    when(mockBundle.getChannel()).thenReturn(mockChannel);
    when(mockChannel.toString()).thenReturn("DEFAULT");

    when(mockContext.getOrganization()).thenReturn("testOrg");
    when(mockContext.getClient()).thenReturn("testClient");
    when(mockContext.getUser()).thenReturn(TEST_USER);
    when(mockContext.toString()).thenReturn("contextString");

    // Mock for ProcessRequestData.insert - return an integer (1), not null
    staticProcessRequestData.when(
        () -> ProcessRequestData.insert(any(), anyString(), anyString(), anyString(), anyString(), anyString(),
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(), any(), any(),
            any())).thenReturn(1); // Return 1 instead of null

    // Mock for checkExists
    when(mockScheduler.checkExists(any(JobKey.class))).thenReturn(false);

    // Execute method under test
    obScheduler.schedule(mockBundle);

    // Verify interactions
    staticProcessRequestData.verify(
        () -> ProcessRequestData.insert(eq(mockConnectionProvider), eq("testOrg"), eq("testClient"), eq(TEST_USER),
            eq(TEST_USER), any(String.class), eq("testProcessId"), eq(TEST_USER), eq("SCH"), eq("DEFAULT"),
            eq("contextString"), eq("deflatedParams"), eq(null), eq(null), eq(null), eq(null)));

    // Verify that the job was scheduled
    verify(mockScheduler).scheduleJob(mockJobDetail, mockTrigger);
  }

}
