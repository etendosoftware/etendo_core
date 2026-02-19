package com.smf.jobs.background;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;

import com.smf.jobs.ActionResult;
import com.smf.jobs.JobManager;
import com.smf.jobs.model.JobResult;

import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BackgroundRunnerTest {

  private static final String TEST_JOB_ID = "TEST_JOB_001";
  private static final String TEST_REQUEST_ID = "TEST_REQ_001";
  private static final String TEST_RESULT_MESSAGE = "Job completed successfully";

  private BackgroundRunner instance;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private ProcessLogger mockLogger;

  private MockedStatic<JobManager> jobManagerStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(BackgroundRunner.class);
    jobManagerStatic = mockStatic(JobManager.class);
  }

  @After
  public void tearDown() {
    if (jobManagerStatic != null) {
      jobManagerStatic.close();
    }
  }

  @Test
  public void testDoExecuteRunsJobAndSavesResults() throws Exception {
    // Arrange
    Map<String, Object> params = new HashMap<>();
    params.put("jobId", TEST_JOB_ID);
    params.put("requestId", TEST_REQUEST_ID);
    when(mockBundle.getParams()).thenReturn(params);
    when(mockBundle.getLogger()).thenReturn(mockLogger);

    List<ActionResult> results = new ArrayList<>();
    jobManagerStatic.when(() -> JobManager.runJobSynchronously(
        eq(TEST_JOB_ID), eq(TEST_REQUEST_ID), any(MutableBoolean.class), eq(false)))
        .thenReturn(results);

    JobResult jobResult = mock(JobResult.class);
    when(jobResult.getMessage()).thenReturn(TEST_RESULT_MESSAGE);
    jobManagerStatic.when(() -> JobManager.saveResults(any(List.class), eq(TEST_JOB_ID), eq(TEST_REQUEST_ID)))
        .thenReturn(jobResult);

    // Set the stopped field since Objenesis skips constructor
    Field stoppedField = BackgroundRunner.class.getDeclaredField("stopped");
    stoppedField.setAccessible(true);
    stoppedField.set(instance, new MutableBoolean(false));

    // Act
    java.lang.reflect.Method doExecute = BackgroundRunner.class.getDeclaredMethod("doExecute", ProcessBundle.class);
    doExecute.setAccessible(true);
    doExecute.invoke(instance, mockBundle);

    // Assert
    jobManagerStatic.verify(() -> JobManager.runJobSynchronously(
        eq(TEST_JOB_ID), eq(TEST_REQUEST_ID), any(MutableBoolean.class), eq(false)));
  }

  @Test
  public void testKillSetsStopped() throws Exception {
    // Arrange
    MutableBoolean stopped = new MutableBoolean(false);
    Field stoppedField = BackgroundRunner.class.getDeclaredField("stopped");
    stoppedField.setAccessible(true);
    stoppedField.set(instance, stopped);

    // Act
    instance.kill(mockBundle);

    // Assert
    assertTrue("stopped should be true after kill()", stopped.booleanValue());
  }
}
