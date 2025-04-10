package org.openbravo.erpCommon.ad_process;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Add this to avoid strict stubbing issues
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

  private static class TestableScheduleProcess extends ScheduleProcess {
    public boolean advisePopUpCalled = false;
    public boolean advisePopUpRefreshCalled = false;
    public String lastPopUpType;

    @Override
    protected void advisePopUp(HttpServletRequest request, HttpServletResponse response,
        String type, String title, String text) {
      advisePopUpCalled = true;
      lastPopUpType = type;
    }

    @Override
    protected void advisePopUpRefresh(HttpServletRequest request, HttpServletResponse response,
        String type, String title, String text) {
      advisePopUpRefreshCalled = true;
      lastPopUpType = type;
    }

    // Make protected method public for testing
    @Override
    public String getRequestId(VariablesSecureApp vars) {
      return super.getRequestId(vars);
    }
  }

  @BeforeEach
  void setUp() {
    mocks = MockitoAnnotations.openMocks(this);

    // Use our testable subclass that overrides the protected methods
    scheduleProcess = spy(new TestableScheduleProcess());

    mockedScheduler = mockStatic(OBScheduler.class);
    mockedOBDal = mockStatic(OBDal.class);
  }

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


  @Test
  void testGetRequestId() {
    // GIVEN
    when(vars.getStringParameter("inpwindowId")).thenReturn("window123");
    when(vars.getSessionValue("window123|AD_Process_Request_ID")).thenReturn("");
    when(vars.getStringParameter("AD_Process_Request_ID")).thenReturn("request123");

    // WHEN
    String requestId = scheduleProcess.getRequestId(vars);

    // THEN
    assertEquals("request123", requestId);
  }

  @Test
  void testIsEmptyProcessGroup() {
    // GIVEN
    String requestId = "test-request-id";
    OBDal obdalMock = mock(OBDal.class);
    mockedOBDal.when(OBDal::getInstance).thenReturn(obdalMock);

    // Mock ProcessRequest and ProcessGroup
    when(obdalMock.get(ProcessRequest.class, requestId)).thenReturn(mockProcessRequest);
    when(mockProcessRequest.getProcessGroup()).thenReturn(mockProcessGroup);

    OBCriteria<ProcessGroupList> criteriaMock = mock(OBCriteria.class);
    when(obdalMock.createCriteria(ProcessGroupList.class)).thenReturn(criteriaMock);
    when(criteriaMock.setMaxResults(anyInt())).thenReturn(criteriaMock);
    when(criteriaMock.add(any())).thenReturn(criteriaMock);
    when(criteriaMock.uniqueResult()).thenReturn(null);

    // WHEN
    boolean result = scheduleProcess.isEmptyProcessGroup(requestId);

    // THEN
    assertTrue(result);
  }
}
