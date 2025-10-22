package org.openbravo.erpCommon.ad_process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.scheduling.OBScheduler;
import org.openbravo.scheduling.ProcessBundle;

/**
 * Unit tests for the {@link ScheduleProcess} class.
 * Verifies the behavior of methods related to scheduling processes,
 * including request ID retrieval and process group validation.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ScheduleProcessTest {

  private AutoCloseable mocks;

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private VariablesSecureApp vars;
  @Mock
  private ProcessBundle bundle;
  @Mock
  private ProcessGroup mockProcessGroup;
  @Mock
  private ProcessRequest mockProcessRequest;

  private MockedStatic<OBScheduler> mockedScheduler;
  private MockedStatic<OBDal> mockedOBDal;

  private ScheduleProcess scheduleProcess;

  /**
   * Sets up the test environment before each test.
   * Initialize mocks and the instance of the class under test.
   */
  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    scheduleProcess = spy(new TestableScheduleProcess());

    mockedScheduler = mockStatic(OBScheduler.class);
    mockedOBDal = mockStatic(OBDal.class);
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances and releases resources.
   */
  @AfterEach
  void tearDown() throws Exception {
    if (mockedScheduler != null) {
      mockedScheduler.close();
    }
    if (mockedOBDal != null) {
      mockedOBDal.close();
    }
    mocks.close();
  }

  /**
   * Tests the `getRequestId` method.
   * Verifies that the correct request ID is retrieved based on the input parameters.
   */
  @Test
  void testGetRequestId() {
    when(vars.getStringParameter("inpwindowId")).thenReturn("window123");
    when(vars.getSessionValue("window123|AD_Process_Request_ID")).thenReturn("");
    when(vars.getStringParameter("AD_Process_Request_ID")).thenReturn("request123");

    String requestId = scheduleProcess.getRequestId(vars);

    assertEquals("request123", requestId);
  }

  /**
   * Tests the `isEmptyProcessGroup` method.
   * Verifies that the method correctly identifies an empty process group.
   */
  @Test
  void testIsEmptyProcessGroup() {
    String requestId = "test-request-id";
    OBDal obdalMock = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obdalMock);

    when(obdalMock.get(ProcessRequest.class, requestId)).thenReturn(mockProcessRequest);
    when(mockProcessRequest.getProcessGroup()).thenReturn(mockProcessGroup);

    OBCriteria<ProcessGroupList> criteriaMock = mock(OBCriteria.class);
    when(obdalMock.createCriteria(ProcessGroupList.class)).thenReturn(criteriaMock);
    when(criteriaMock.setMaxResults(anyInt())).thenReturn(criteriaMock);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.uniqueResult()).thenReturn(null);

    boolean result = scheduleProcess.isEmptyProcessGroup(requestId);

    assertTrue(result);
  }

  /**
   * Testable subclass of {@link ScheduleProcess} to expose protected methods for testing.
   */
  private static class TestableScheduleProcess extends ScheduleProcess {
    /**
     * Flag to indicate if the advisePopUp method was called.
     * This is used to verify that the method was called with the expected parameters.
     */
    public boolean advisePopUpCalled = false;
    /**
     * The last pop-up type used in the advisePopUp method.
     * This is used to verify that the method was called with the expected parameters.
     */
    public boolean advisePopUpRefreshCalled = false;
    /**
     * The last pop-up type used in the advisePopUp method.
     * This is used to verify that the method was called with the expected parameters.
     */
    public String lastPopUpType;

    /**
     * Mock implementation of the advisePopUp method to set a flag for testing.
     * This is used to verify that the method was called with the expected parameters.
     */
    @Override
    protected void advisePopUp(HttpServletRequest request, HttpServletResponse response, String type, String title,
        String text) {
      advisePopUpCalled = true;
      lastPopUpType = type;
    }

    /**
     * Mock implementation of the advisePopUpRefresh method to set a flag for testing.
     * This is used to verify that the method was called with the expected parameters.
     */
    @Override
    protected void advisePopUpRefresh(HttpServletRequest request, HttpServletResponse response, String type,
        String title, String text) {
      advisePopUpRefreshCalled = true;
      lastPopUpType = type;
    }

    /**
     * Mock implementation of the getRequestId method to return a fixed request ID.
     * This is used for testing purposes to avoid dependency on external systems.
     */
    @Override
    public String getRequestId(VariablesSecureApp vars) {
      return super.getRequestId(vars);
    }
  }
}
