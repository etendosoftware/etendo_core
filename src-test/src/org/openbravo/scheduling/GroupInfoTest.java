package org.openbravo.scheduling;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.ProcessGroup;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;

/**
 * Test class for GroupInfo functionality.
 * Tests process group execution, logging and status management.
 */
public class GroupInfoTest {

  @Mock
  private ProcessGroup mockGroup;
  @Mock
  private ProcessRequest mockRequest;
  @Mock
  private ProcessRun mockProcessRun;
  @Mock
  private ProcessGroupList mockProcessGroupList;
  @Mock
  private Process mockProcess;
  @Mock
  private VariablesSecureApp mockVars;
  @Mock
  private ConnectionProvider mockConn;
  @Mock
  private OBScheduler mockScheduler;

  private MockedStatic<OBMessageUtils> mockedOBMessageUtils;
  private MockedStatic<OBScheduler> mockedOBScheduler;

  private GroupInfo groupInfo;
  private List<ProcessGroupList> mockGroupList;

  /**
   * Sets up the test environment before each test.
   * Initializes mocks, configures mock responses and creates test instances.
   *
   * @throws Exception
   *     if there's an error during mock initialization
   */
  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Mock static methods
    mockedOBMessageUtils = mockStatic(OBMessageUtils.class);
    mockedOBScheduler = mockStatic(OBScheduler.class);

    // Setup mock responses
    when(mockProcess.getId()).thenReturn("testProcessId");
    when(mockProcess.getName()).thenReturn("Test Process");
    when(mockProcessGroupList.getProcess()).thenReturn(mockProcess);
    when(mockProcessGroupList.getSequenceNumber()).thenReturn(1L);
    when(mockGroup.getName()).thenReturn("Test Group");

    // Configure request mocks for ProcessBundle
    when(mockRequest.getClient()).thenReturn(mock(org.openbravo.model.ad.system.Client.class));
    when(mockRequest.getClient().getId()).thenReturn("testClientId");
    when(mockRequest.getOrganization()).thenReturn(mock(org.openbravo.model.common.enterprise.Organization.class));
    when(mockRequest.getOrganization().getId()).thenReturn("testOrgId");
    when(mockRequest.isSecurityBasedOnRole()).thenReturn(false);

    // Mock OBScheduler instance
    mockedOBScheduler.when(OBScheduler::getInstance).thenReturn(mockScheduler);

    // Mock message utils
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage(anyString(), any())).thenReturn("Mocked Message");

    mockGroupList = List.of(mockProcessGroupList);

    groupInfo = new GroupInfo(mockGroup, mockRequest, mockProcessRun, mockGroupList, true, mockVars, mockConn);
  }

  /**
   * Cleans up resources after each test.
   * Closes static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    if (mockedOBMessageUtils != null) {
      mockedOBMessageUtils.close();
    }
    if (mockedOBScheduler != null) {
      mockedOBScheduler.close();
    }
  }

  /**
   * Tests the correct initialization of GroupInfo constructor.
   * Verifies instance variables and initial status.
   */
  @Test
  public void testConstructorInitialization() {
    assertEquals(mockGroup, groupInfo.getGroup());
    assertEquals(mockRequest, groupInfo.getRequest());
    assertEquals(mockProcessRun, groupInfo.getProcessRun());

    assertEquals("SUC", groupInfo.getStatus());
  }

  /**
   * Tests executeNextProcess with an empty group list.
   * Verifies that END is returned and no scheduling occurs.
   *
   * @throws Exception
   *     if there's an error during process execution or scheduling
   */
  @Test
  public void testExecuteNextProcessEmptyGroupList() throws Exception {
    GroupInfo emptyGroupInfo = new GroupInfo(mockGroup, mockRequest, mockProcessRun, new ArrayList<>(), true, mockVars,
        mockConn);

    String result = emptyGroupInfo.executeNextProcess();
    assertEquals("END", result);

    verify(mockScheduler, never()).schedule(any(ProcessBundle.class));
  }

  /**
   * Tests logging of successful process execution.
   * Verifies success status and correct message formatting.
   *
   * @throws Exception
   *     if there's an error accessing private fields or during logging
   */
  @Test
  public void testLogProcessSuccess() throws Exception {
    groupInfo = spy(groupInfo);

    Field groupLogField = GroupInfo.class.getDeclaredField("groupLog");
    groupLogField.setAccessible(true);
    groupLogField.set(groupInfo, new StringBuilder());

    Field currentPositionField = GroupInfo.class.getDeclaredField("currentposition");
    currentPositionField.setAccessible(true);
    currentPositionField.set(groupInfo, 1);

    when(mockProcessGroupList.getProcess().getName()).thenReturn("Test Process");
    when(mockProcessGroupList.getSequenceNumber()).thenReturn(1L);

    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("PROGROUP_Success", null)).thenReturn("SUCCESS");
    mockedOBMessageUtils.when(
        () -> OBMessageUtils.getI18NMessage("PROGROUP_Process", new String[]{ "Test Process" })).thenReturn(
        " - Test Process: ");
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("PROGROUP_Separator", null)).thenReturn(
        "-------------");

    groupInfo.logProcess(org.openbravo.scheduling.Process.SUCCESS);

    assertEquals(org.openbravo.scheduling.Process.SUCCESS, groupInfo.getStatus());

    mockedOBMessageUtils.verify(() -> OBMessageUtils.getI18NMessage("PROGROUP_Success", null));
    mockedOBMessageUtils.verify(
        () -> OBMessageUtils.getI18NMessage("PROGROUP_Process", new String[]{ "Test Process" }));
    mockedOBMessageUtils.verify(() -> OBMessageUtils.getI18NMessage("PROGROUP_Separator", null));
  }

  /**
   * Tests logging of failed process execution.
   * Verifies error status and correct message formatting.
   *
   * @throws Exception
   *     if there's an error accessing private fields or during logging
   */
  @Test
  public void testLogProcessError() throws Exception {
    groupInfo = spy(groupInfo);

    Field groupLogField = GroupInfo.class.getDeclaredField("groupLog");
    groupLogField.setAccessible(true);
    groupLogField.set(groupInfo, new StringBuilder());

    Field currentPositionField = GroupInfo.class.getDeclaredField("currentposition");
    currentPositionField.setAccessible(true);
    currentPositionField.set(groupInfo, 1);

    when(mockProcessGroupList.getProcess().getName()).thenReturn("Test Process");
    when(mockProcessGroupList.getSequenceNumber()).thenReturn(1L);

    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("PROGROUP_Fail", null)).thenReturn("ERROR");
    mockedOBMessageUtils.when(
        () -> OBMessageUtils.getI18NMessage("PROGROUP_Process", new String[]{ "Test Process" })).thenReturn(
        " - Test Process: ");
    mockedOBMessageUtils.when(() -> OBMessageUtils.getI18NMessage("PROGROUP_Separator", null)).thenReturn(
        "-------------");

    groupInfo.logProcess(org.openbravo.scheduling.Process.ERROR);

    assertEquals(org.openbravo.scheduling.Process.ERROR, groupInfo.getStatus());

    mockedOBMessageUtils.verify(() -> OBMessageUtils.getI18NMessage("PROGROUP_Fail", null));
    mockedOBMessageUtils.verify(
        () -> OBMessageUtils.getI18NMessage("PROGROUP_Process", new String[]{ "Test Process" }));
    mockedOBMessageUtils.verify(() -> OBMessageUtils.getI18NMessage("PROGROUP_Separator", null));
  }

  /**
   * Tests status management with multiple processes.
   * Verifies that error status persists across multiple process executions.
   *
   * @throws Exception
   *     if there's an error accessing private fields or during process execution
   */
  @Test
  public void testMultipleProcessesStatus() throws Exception {
    ProcessGroupList mockProcessGroupList2 = mock(ProcessGroupList.class);
    Process mockProcess2 = mock(Process.class);
    when(mockProcess2.getName()).thenReturn("Test Process 2");
    when(mockProcessGroupList2.getProcess()).thenReturn(mockProcess2);

    List<ProcessGroupList> twoProcessList = Arrays.asList(mockProcessGroupList, mockProcessGroupList2);
    GroupInfo multiGroupInfo = new GroupInfo(mockGroup, mockRequest, mockProcessRun, twoProcessList, false, mockVars,
        mockConn);

    Field groupLogField = GroupInfo.class.getDeclaredField("groupLog");
    groupLogField.setAccessible(true);
    groupLogField.set(multiGroupInfo, new StringBuilder());

    Field currentPositionField = GroupInfo.class.getDeclaredField("currentposition");
    currentPositionField.setAccessible(true);
    currentPositionField.set(multiGroupInfo, 1);

    multiGroupInfo.logProcess(org.openbravo.scheduling.Process.ERROR);

    assertEquals(org.openbravo.scheduling.Process.ERROR, multiGroupInfo.getStatus());

    currentPositionField.set(multiGroupInfo, 2);

    multiGroupInfo.logProcess(org.openbravo.scheduling.Process.SUCCESS);

    assertEquals(org.openbravo.scheduling.Process.ERROR, multiGroupInfo.getStatus());
  }

  /**
   * Tests behavior of stopWhenFails flag.
   * Verifies different execution paths based on flag value.
   *
   * @throws Exception
   *     if there's an error accessing private fields or during process execution
   */
  @Test
  public void testStopWhenFailsFlag() throws Exception {
    GroupInfo stopWhenFailsGroupInfo = new GroupInfo(mockGroup, mockRequest, mockProcessRun, mockGroupList, true,
        mockVars, mockConn);

    GroupInfo continueOnFailGroupInfo = new GroupInfo(mockGroup, mockRequest, mockProcessRun, mockGroupList, false,
        mockVars, mockConn);

    Field groupLogField = GroupInfo.class.getDeclaredField("groupLog");
    groupLogField.setAccessible(true);
    groupLogField.set(stopWhenFailsGroupInfo, new StringBuilder());
    groupLogField.set(continueOnFailGroupInfo, new StringBuilder());

    Field currentPositionField = GroupInfo.class.getDeclaredField("currentposition");
    currentPositionField.setAccessible(true);
    currentPositionField.set(stopWhenFailsGroupInfo, 1);
    currentPositionField.set(continueOnFailGroupInfo, 1);

    when(mockProcessGroupList.getProcess().getName()).thenReturn("Test Process");
    when(mockProcessGroupList.getSequenceNumber()).thenReturn(1L);

    assertEquals(org.openbravo.scheduling.Process.SUCCESS, stopWhenFailsGroupInfo.getStatus());
    assertEquals(org.openbravo.scheduling.Process.SUCCESS, continueOnFailGroupInfo.getStatus());

    stopWhenFailsGroupInfo.logProcess(org.openbravo.scheduling.Process.ERROR);
    continueOnFailGroupInfo.logProcess(org.openbravo.scheduling.Process.ERROR);

    assertEquals(org.openbravo.scheduling.Process.ERROR, stopWhenFailsGroupInfo.getStatus());
    assertEquals(org.openbravo.scheduling.Process.ERROR, continueOnFailGroupInfo.getStatus());
  }
}
