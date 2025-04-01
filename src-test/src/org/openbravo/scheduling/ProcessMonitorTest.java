package org.openbravo.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.Trigger;

/**
 * Test class for ProcessMonitor functionality.
 * Tests job execution veto logic and duration formatting.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessMonitorTest {

  private ProcessMonitor processMonitor;

  @Mock
  private SchedulerContext schedulerContext;

  @Mock
  private Trigger trigger;

  @Mock
  private JobDataMap jobDataMap;

  @Mock
  private JobExecutionContext jobExecutionContext;

  @Mock
  private Scheduler scheduler;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and configures common trigger setup.
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Setup the ProcessMonitor
    processMonitor = new ProcessMonitor("TestMonitor", schedulerContext);

    // Common trigger setup
    when(trigger.getJobDataMap()).thenReturn(jobDataMap);
  }

  /**
   * Tests that job execution is not vetoed when concurrent executions are allowed.
   */
  @Test
  public void testVetoJobExecutionWithNoPreventConcurrentExecutions() {
    // Arrange
    when(jobDataMap.get(Process.PREVENT_CONCURRENT_EXECUTIONS)).thenReturn(false);

    // Act
    boolean result = processMonitor.vetoJobExecution(trigger, jobExecutionContext);

    // Assert
    assertFalse(result);
  }

  /**
   * Tests that job execution is not vetoed when there are no concurrent jobs running.
   */
  @Test
  public void testVetoJobExecutionWithPreventConcurrentExecutionsNoConcurrentJobs() throws SchedulerException {
    // Arrange
    when(jobDataMap.get(Process.PREVENT_CONCURRENT_EXECUTIONS)).thenReturn(true);
    when(jobDataMap.getString(Process.PROCESS_NAME)).thenReturn("TestProcess");
    when(jobDataMap.get(Process.PROCESS_ID)).thenReturn("TestProcessId");

    List<JobExecutionContext> emptyJobsList = new ArrayList<>();
    when(jobExecutionContext.getScheduler()).thenReturn(scheduler);
    when(scheduler.getCurrentlyExecutingJobs()).thenReturn(emptyJobsList);

    // Act
    boolean result = processMonitor.vetoJobExecution(trigger, jobExecutionContext);

    // Assert
    assertFalse(result);
  }

  /**
   * Tests that the monitor name is correctly returned.
   */
  @Test
  public void testGetName() {
    // Act
    String name = processMonitor.getName();

    // Assert
    assertEquals("TestMonitor", name);
  }

  /**
   * Tests the duration formatting for a typical duration value.
   */
  @Test
  public void testGetDuration() {
    // Arrange
    long duration = 3723456;

    // Act
    String result = ProcessMonitor.getDuration(duration);

    // Assert
    assertEquals("01:02:03.456", result);
  }

  /**
   * Tests the duration formatting for small duration values.
   * Verifies correct padding of zeros for milliseconds, seconds, and minutes.
   */
  @Test
  public void testGetDurationSmallValues() {
    // Arrange & Act & Assert
    assertEquals("00:00:01.005", ProcessMonitor.getDuration(1005));
    assertEquals("00:00:01.050", ProcessMonitor.getDuration(1050));
    assertEquals("00:00:01.005", ProcessMonitor.getDuration(1005));
    assertEquals("00:00:00.005", ProcessMonitor.getDuration(5));
  }
}
