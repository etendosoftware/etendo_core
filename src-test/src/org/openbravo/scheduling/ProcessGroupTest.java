package org.openbravo.scheduling;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.model.ad.ui.Process;
import org.openbravo.model.ad.ui.ProcessGroupList;
import org.openbravo.model.ad.ui.ProcessRequest;
import org.openbravo.model.ad.ui.ProcessRun;

/**
 * Unit tests for the ProcessGroup class.
 * Tests the execution of process groups and their error handling.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessGroupTest {

  @InjectMocks
  private ProcessGroup processGroup;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private ConnectionProvider mockConnectionProvider;

  @Mock
  private ProcessRequest mockProcessRequest;

  @Mock
  private ProcessRun mockProcessRun;

  @Mock
  private org.openbravo.model.ad.ui.ProcessGroup mockGroup;

  @Mock
  private OBCriteria<ProcessGroupList> mockCriteria;

  @Mock
  private ProcessContext mockContext;

  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<Hibernate> mockedHibernate;

  /**
   * Sets up the test environment before each test.
   * Initializes mocks and configures common behavior for the ProcessGroup tests.
   */
  @Before
  public void setUp() {
    try {
      mockedOBDal = mockStatic(OBDal.class);
      mockedHibernate = mockStatic(Hibernate.class);

      when(mockBundle.getConnection()).thenReturn(mockConnectionProvider);
      when(mockBundle.getProcessRequestId()).thenReturn("requestId");
      when(mockBundle.getProcessRunId()).thenReturn("runId");

      when(mockBundle.getContext()).thenReturn(mockContext);
      VariablesSecureApp mockVars = mock(VariablesSecureApp.class);
      when(mockContext.toVars()).thenReturn(mockVars);

      OBDal mockOBDal = mock(OBDal.class);
      mockedOBDal.when(OBDal::getInstance).thenReturn(mockOBDal);

      when(mockOBDal.get(ProcessRequest.class, "requestId")).thenReturn(mockProcessRequest);
      when(mockOBDal.get(ProcessRun.class, "runId")).thenReturn(mockProcessRun);
      when(mockProcessRequest.getProcessGroup()).thenReturn(mockGroup);

      when(mockOBDal.createCriteria(ProcessGroupList.class)).thenReturn(mockCriteria);

      when(mockCriteria.add(any())).thenReturn(mockCriteria);
      when(mockCriteria.addOrderBy(any(), anyBoolean())).thenReturn(mockCriteria);
    } catch (Exception e) {
      closeStaticMocks();
      throw e;
    }
  }

  /**
   * Cleans up resources after each test.
   * Closes static mocks to prevent memory leaks.
   */
  @After
  public void tearDown() {
    closeStaticMocks();
  }

  /**
   * Helper method to close static mocks.
   * Ensures proper cleanup of mocked static classes.
   */
  private void closeStaticMocks() {
    if (mockedOBDal != null) {
      mockedOBDal.close();
      mockedOBDal = null;
    }
    if (mockedHibernate != null) {
      mockedHibernate.close();
      mockedHibernate = null;
    }
  }

  /**
   * Tests the doExecute method when Hibernate initialization fails.
   * Verifies that a RuntimeException is thrown when process list initialization fails.
   *
   * @throws Exception
   *     if an unexpected error occurs
   */
  @Test(expected = RuntimeException.class)
  public void testDoExecuteHibernateInitializationError() throws Exception {
    // GIVEN
    List<ProcessGroupList> mockProcessList = new ArrayList<>();
    ProcessGroupList mockProcessGroupList = mock(ProcessGroupList.class);
    mockProcessList.add(mockProcessGroupList);

    when(mockCriteria.list()).thenReturn(mockProcessList);
    mockedHibernate.when(() -> Hibernate.initialize(mockProcessGroupList)).thenThrow(
        new RuntimeException("Initialization error"));

    // WHEN
    processGroup.doExecute(mockBundle);

    // THEN
    // Exception is expected
  }

  /**
   * Tests the doExecute method when group execution fails.
   * Verifies that a RuntimeException is thrown when the process group execution fails.
   *
   * @throws Exception
   *     if an unexpected error occurs
   */
  @Test(expected = RuntimeException.class)
  public void testDoExecuteGroupExecutionError() throws Exception {
    // GIVEN
    List<ProcessGroupList> mockProcessList = new ArrayList<>();
    ProcessGroupList mockProcessGroupList = mock(ProcessGroupList.class);
    Process mockProcess = mock(Process.class);
    mockProcessList.add(mockProcessGroupList);


    GroupInfo mockGroupInfo = mock(GroupInfo.class);

    try (MockedStatic<GroupInfo> mockedGroupInfo = mockStatic(GroupInfo.class)) {
      mockedGroupInfo.when(
          () -> new GroupInfo(any(org.openbravo.model.ad.ui.ProcessGroup.class), any(ProcessRequest.class),
              any(ProcessRun.class), anyList(), anyBoolean(), any(VariablesSecureApp.class),
              any(ConnectionProvider.class))).thenReturn(mockGroupInfo);

      // WHEN
      processGroup.doExecute(mockBundle);

      // THEN
      // Exception is expected
    }
  }
}
