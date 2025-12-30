package com.etendoerp.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.process.ProcessInstance;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.test.base.OBBaseTest;

/**
 * Tests for {@link CallAsyncProcess}.
 */
public class CallAsyncProcessTest extends OBBaseTest {

  private ExecutorService mockExecutorService;
  private ExecutorService originalExecutorService;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    mockExecutorService = mock(ExecutorService.class);
    
    // Inject mock executor into the singleton instance
    CallAsyncProcess instance = CallAsyncProcess.getInstance();
    Field field = CallAsyncProcess.class.getDeclaredField("executorService");
    field.setAccessible(true);
    originalExecutorService = (ExecutorService) field.get(instance);
    field.set(instance, mockExecutorService);
  }

  @After
  public void tearDown() throws Exception {
    // Restore original executor to avoid side effects in other tests
    CallAsyncProcess instance = CallAsyncProcess.getInstance();
    Field field = CallAsyncProcess.class.getDeclaredField("executorService");
    field.setAccessible(true);
    field.set(instance, originalExecutorService);
  }

  /**
   * Verifies that callProcess returns immediately with the correct status
   * and submits the task to the executor.
   */
  @Test
  public void testCallProcessAsync() {
    setSystemAdministratorContext();

    // Use a standard process ID that usually exists in test environments
    // 114 is "Copy Test Line"
    Process process = OBDal.getInstance().get(Process.class, "114");
    assertNotNull("Process 114 should exist in test environment", process);

    Map<String, String> parameters = new HashMap<>();
    
    // Execute the process asynchronously
    ProcessInstance pInstance = CallAsyncProcess.getInstance().callProcess(process, "0", parameters, false);

    // 1. Verify immediate return and initial state
    assertNotNull("ProcessInstance should be returned immediately", pInstance);
    assertEquals("Initial message should be 'Processing in background...'", 
        "Processing in background...", pInstance.getErrorMsg());
    assertEquals("Initial result should be 0 (Processing)", Long.valueOf(0), pInstance.getResult());

    // 2. Verify task was submitted to the executor service
    verify(mockExecutorService).submit(any(Runnable.class));
    
    // Rollback to keep the database clean
    OBDal.getInstance().rollbackAndClose();
  }
}
