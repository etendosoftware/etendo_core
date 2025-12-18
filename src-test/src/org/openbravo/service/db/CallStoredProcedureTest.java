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
 * Unit tests for the {@link CallStoredProcedure} class.
 * <p>
 * This test suite ensures that {@link CallStoredProcedure} correctly maintains its singleton
 * behavior and properly delegates calls to the underlying {@link CallProcess} engine.
 * Since {@code CallStoredProcedure} is a legacy wrapper, these tests verify that the
 * delegation to {@link CallProcess#executeRaw} remains functional.
 * </p>
 *
 * @author etendo
 * @see CallStoredProcedure
 * @see CallProcess
 */
public class CallStoredProcedureTest {

  public static final String TEST_PROCEDURE = "test_procedure";
  public static final String RESULT_SHOULD_BE_DELEGATED_FROM_CALL_PROCESS = "Result should be delegated from CallProcess";
  private CallProcess mockCallProcess;
  private CallProcess originalCallProcess;

  /**
   * Sets up the test environment by mocking the {@link CallProcess} singleton.
   * The original instance is stored to be restored after each test.
   */
  @Before
  public void setUp() {
    mockCallProcess = mock(CallProcess.class);
    originalCallProcess = CallProcess.getInstance();
    CallProcess.setInstance(mockCallProcess);
  }

  /**
   * Restores the original {@link CallProcess} instance to prevent side effects
   * on other tests in the suite.
   */
  @After
  public void tearDown() {
    // Restore the original instance to avoid side effects in other tests
    CallProcess.setInstance(originalCallProcess);
  }

  /**
   * Verifies the singleton behavior of {@link CallStoredProcedure#getInstance()}.
   * Ensures that multiple calls return the exact same object instance.
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
   * <p>
   * Default parameters for this legacy method are:
   * <ul>
   *   <li>doFlush: true</li>
   *   <li>returnResults: true</li>
   * </ul>
   * </p>
   */
  @Test
  public void testCallDelegation() {
    String name = TEST_PROCEDURE;
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = "result";

    when(mockCallProcess.executeRaw(name, parameters, types, true, true)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types);

    assertEquals(RESULT_SHOULD_BE_DELEGATED_FROM_CALL_PROCESS, expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, true, true);
  }

  /**
   * Verifies that {@link CallStoredProcedure#call(String, List, List, boolean)}
   * correctly delegates to {@link CallProcess#executeRaw} with a custom flush flag.
   * <p>
   * The returnResults flag is expected to be true by default in this overload.
   * </p>
   */
  @Test
  public void testCallWithFlushDelegation() {
    String name = TEST_PROCEDURE;
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = 123;

    when(mockCallProcess.executeRaw(name, parameters, types, false, true)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types, false);

    assertEquals(RESULT_SHOULD_BE_DELEGATED_FROM_CALL_PROCESS, expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, false, true);
  }

  /**
   * Verifies that {@link CallStoredProcedure#call(String, List, List, boolean, boolean)}
   * correctly delegates to {@link CallProcess#executeRaw} with all custom parameters.
   */
  @Test
  public void testFullCallDelegation() {
    String name = TEST_PROCEDURE;
    List<Object> parameters = new ArrayList<>();
    List<Class<?>> types = new ArrayList<>();
    Object expectedResult = null;

    when(mockCallProcess.executeRaw(name, parameters, types, false, false)).thenReturn(expectedResult);

    Object result = CallStoredProcedure.getInstance().call(name, parameters, types, false, false);

    assertEquals(RESULT_SHOULD_BE_DELEGATED_FROM_CALL_PROCESS, expectedResult, result);
    verify(mockCallProcess).executeRaw(name, parameters, types, false, false);
  }
}
