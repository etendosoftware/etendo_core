package org.openbravo.service.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CallStoredProcedure}.
 * Since {@link CallStoredProcedure} delegates to {@link CallProcess#executeRaw},
 * we mock {@link CallProcess} to verify the delegation.
 */
public class CallStoredProcedureTest {

  private CallProcess mockCallProcess;
  private CallProcess originalCallProcess;

  @Before
  public void setUp() {
    mockCallProcess = mock(CallProcess.class);
    originalCallProcess = CallProcess.getInstance();
    CallProcess.setInstance(mockCallProcess);
  }

  @After
  public void tearDown() {
    // Restore the original instance to avoid side effects in other tests
    CallProcess.setInstance(originalCallProcess);
  }

  /**
   * Verifies the singleton behavior of {@link CallStoredProcedure#getInstance()}.
   */
  @Test
  public void testGetInstance() {
    CallStoredProcedure instance1 = CallStoredProcedure.getInstance();
    CallStoredProcedure instance2 = CallStoredProcedure.getInstance();
    assertSame("getInstance() should return the same singleton instance", instance1, instance2);
  }

  /**
   * Verifies that {@link CallStoredProcedure#call(String, List, List)} 
   * correctly delegates to {@link CallProcess#executeRaw} with default parameters.
   */
  @Test
  public void testCallDelegation() {
    String name = "test_procedure";
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = "result";

    when(mockCallProcess.executeRaw(name, parameters, types, true, true)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types);

    assertEquals("Result should be delegated from CallProcess", expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, true, true);
  }

  /**
   * Verifies that {@link CallStoredProcedure#call(String, List, List, boolean)} 
   * correctly delegates to {@link CallProcess#executeRaw} with custom doFlush.
   */
  @Test
  public void testCallWithFlushDelegation() {
    String name = "test_procedure";
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = 123;

    when(mockCallProcess.executeRaw(name, parameters, types, false, true)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types, false);

    assertEquals("Result should be delegated from CallProcess", expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, false, true);
  }

  /**
   * Verifies that {@link CallStoredProcedure#call(String, List, List, boolean, boolean)} 
   * correctly delegates to {@link CallProcess#executeRaw} with all custom parameters.
   */
  @Test
  public void testFullCallDelegation() {
    String name = "test_procedure";
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = null;

    when(mockCallProcess.executeRaw(name, parameters, types, false, false)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types, false, false);

    assertEquals("Result should be delegated from CallProcess", expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, false, false);
  }
}
