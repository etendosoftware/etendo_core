package org.openbravo.common.actionhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.ProcessRun;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.service.db.DbUtility;

/**
 * Unit tests for the {@link KillProcess} class.
 * Verifies the behavior of methods related to process termination and exception handling.
 */
@ExtendWith(MockitoExtension.class)
public class KillProcessTest {

  private static final String PROCESS_RUN_ID = "TEST_PROCESS_RUN_ID";
  private static final String PROCESS_NOT_FOUND_MSG = "Process Not Found";

  @Mock
  private OBScheduler mockScheduler;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private ProcessRun mockProcessRun;

  @InjectMocks
  private KillProcess killProcess;

  private MockedStatic<OBScheduler> mockedOBScheduler;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<DbUtility> mockedDbUtility;

  /**
   * Sets up the test environment before each test.
   * Mocks static methods and initializes dependencies.
   *
   * @throws Exception
   *     if an error occurs during setup
   */
  @BeforeEach
  public void setUp() throws Exception {
    mockedOBScheduler = mockStatic(OBScheduler.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedDbUtility = mockStatic(DbUtility.class);

    mockedOBScheduler.when(OBScheduler::getInstance).thenReturn(mockScheduler);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

  }

  /**
   * Cleans up the test environment after each test.
   * Closes mocked static methods to release resources.
   */
  @AfterEach
  public void tearDown() {
    if (mockedOBScheduler != null) {
      mockedOBScheduler.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    if (mockedOBContext != null) {
      mockedOBContext.close();
    }
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedDbUtility != null) {
      mockedDbUtility.close();
    }
  }

  /**
   * Tests the {@code markProcessShouldBeKilled} method when the process is found.
   * Verifies that the process is marked as "should be killed" and changes are persisted.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testMarkProcessShouldBeKilledProcessFound() throws Exception {
    when(mockOBDal.get(ProcessRun.class, PROCESS_RUN_ID)).thenReturn(mockProcessRun);

    java.lang.reflect.Method markProcessShouldBeKilledMethod = KillProcess.class.getDeclaredMethod(
        "markProcessShouldBeKilled", String.class);
    markProcessShouldBeKilledMethod.setAccessible(true);

    markProcessShouldBeKilledMethod.invoke(killProcess, PROCESS_RUN_ID);

    verify(mockProcessRun).setShouldBeKilled(true);
    verify(mockOBDal).flush();
    mockedOBContext.verify(() -> OBContext.setAdminMode(false), times(1));
    mockedOBContext.verify(OBContext::restorePreviousMode, times(1));
  }

  /**
   * Tests the {@code markProcessShouldBeKilled} method when the process is not found.
   * Verifies that an exception is thrown with the appropriate error message.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testMarkProcessShouldBeKilledProcessNotFound() throws Exception {
    when(mockOBDal.get(ProcessRun.class, PROCESS_RUN_ID)).thenReturn(null);
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("ProcessNotFound", null)).thenReturn(
        PROCESS_NOT_FOUND_MSG);

    java.lang.reflect.Method markProcessShouldBeKilledMethod = KillProcess.class.getDeclaredMethod(
        "markProcessShouldBeKilled", String.class);
    markProcessShouldBeKilledMethod.setAccessible(true);

    Exception exception = org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> markProcessShouldBeKilledMethod.invoke(killProcess, PROCESS_RUN_ID));
    assertNotNull(exception.getCause());
    assertEquals(PROCESS_NOT_FOUND_MSG, exception.getCause().getMessage());
  }

  /**
   * Tests the {@code getTranslatedExceptionMessage} method when a translation exists.
   * Verifies that the translated message is returned.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testGetTranslatedExceptionMessageExistingTranslation() throws Exception {
    Throwable mockThrowable = mock(Throwable.class);
    when(mockThrowable.getMessage()).thenReturn(ActionHandlerTestConstants.TEST_ERROR_CODE);

    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(ActionHandlerTestConstants.TEST_ERROR_CODE, null)).thenReturn(
        "Translated error message");

    java.lang.reflect.Method getTranslatedExceptionMessageMethod = KillProcess.class.getDeclaredMethod(
        "getTranslatedExceptionMessage", Throwable.class);
    getTranslatedExceptionMessageMethod.setAccessible(true);

    String result = (String) getTranslatedExceptionMessageMethod.invoke(killProcess, mockThrowable);

    assertEquals("Translated error message", result);
  }

  /**
   * Tests the {@code getTranslatedExceptionMessage} method when no translation exists.
   * Verifies that the original message is returned.
   *
   * @throws Exception
   *     if reflection access or method invocation fails
   */
  @Test
  public void testGetTranslatedExceptionMessageNoTranslation() throws Exception {
    Throwable mockThrowable = mock(Throwable.class);
    when(mockThrowable.getMessage()).thenReturn(ActionHandlerTestConstants.TEST_ERROR_CODE);

    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(ActionHandlerTestConstants.TEST_ERROR_CODE, null)).thenReturn(null);

    java.lang.reflect.Method getTranslatedExceptionMessageMethod = KillProcess.class.getDeclaredMethod(
        "getTranslatedExceptionMessage", Throwable.class);
    getTranslatedExceptionMessageMethod.setAccessible(true);

    String result = (String) getTranslatedExceptionMessageMethod.invoke(killProcess, mockThrowable);

    assertEquals(ActionHandlerTestConstants.TEST_ERROR_CODE, result);
  }
}
