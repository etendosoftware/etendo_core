package org.openbravo.scheduling.quartz;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

/**
 * Unit tests for the JobInitializationListener class.
 * Tests the initialization of jobs with different ProcessBundle configurations.
 */
@ExtendWith(MockitoExtension.class)
public class JobInitializationListenerTest {

  @InjectMocks
  private JobInitializationListener jobInitializationListener;

  @Mock
  private JobExecutionContext mockContext;

  @Mock
  private JobDetail mockJobDetail;

  @Mock
  private JobDataMap mockJobDataMap;

  @Mock
  private ProcessBundle mockProcessBundle;

  @Mock
  private ConnectionProvider mockConnectionProvider;

  private OBScheduler mockScheduler;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and configures common behavior.
   */
  @BeforeEach
  public void setUp() {
    mockScheduler = mock(OBScheduler.class);

    when(mockContext.getJobDetail()).thenReturn(mockJobDetail);
    when(mockJobDetail.getJobDataMap()).thenReturn(mockJobDataMap);
  }

  /**
   * Tests job execution when ProcessBundle has no connection.
   * Verifies that a new connection is set and a process logger is created.
   */
  @Test
  public void testJobToBeExecutedProcessBundleWithoutConnection() {
    // GIVEN
    Map<String, Object> bundleMap = new HashMap<>();
    when(mockJobDataMap.get(ProcessBundle.KEY)).thenReturn(bundleMap);

    try (MockedStatic<OBScheduler> mockedOBScheduler = mockStatic(OBScheduler.class);
         MockedStatic<ProcessBundle> mockedProcessBundle = mockStatic(ProcessBundle.class)) {
      mockedOBScheduler.when(OBScheduler::getInstance).thenReturn(mockScheduler);
      mockedProcessBundle.when(() -> ProcessBundle.mapToObject(bundleMap)).thenReturn(mockProcessBundle);
      when(mockScheduler.getConnection()).thenReturn(mockConnectionProvider);

      when(mockProcessBundle.getConnection()).thenReturn(null);

      // WHEN
      jobInitializationListener.jobToBeExecuted(mockContext);

      // THEN
      verify(mockProcessBundle).setConnection(mockConnectionProvider);
      verify(mockProcessBundle).setLog(any(ProcessLogger.class));
    }
  }

  /**
   * Tests job execution when ProcessBundle already has a connection.
   * Verifies that the existing connection is kept and only a process logger is created.
   */
  @Test
  public void testJobToBeExecutedProcessBundleWithConnection() {
    // GIVEN
    Map<String, Object> bundleMap = new HashMap<>();
    when(mockJobDataMap.get(ProcessBundle.KEY)).thenReturn(bundleMap);

    try (MockedStatic<OBScheduler> mockedOBScheduler = mockStatic(OBScheduler.class);
         MockedStatic<ProcessBundle> mockedProcessBundle = mockStatic(ProcessBundle.class)) {
      mockedOBScheduler.when(OBScheduler::getInstance).thenReturn(mockScheduler);
      mockedProcessBundle.when(() -> ProcessBundle.mapToObject(bundleMap)).thenReturn(mockProcessBundle);

      when(mockProcessBundle.getConnection()).thenReturn(mockConnectionProvider);

      // WHEN
      jobInitializationListener.jobToBeExecuted(mockContext);

      // THEN
      verify(mockProcessBundle, never()).setConnection(any());
      verify(mockProcessBundle).setLog(any(ProcessLogger.class));
    }
  }
}
