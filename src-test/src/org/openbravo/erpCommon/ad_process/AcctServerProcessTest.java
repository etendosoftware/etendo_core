/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.ad_forms.AcctServerBatch;
import org.openbravo.model.ad.system.Client;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessBundle.Channel;
import org.openbravo.scheduling.ProcessContext;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Unit tests for {@link AcctServerProcess}.
 * Tests the accounting server process execution including handling of
 * system client vs regular client processing and different execution channels.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctServerProcessTest {

  private static final String CREATED_0 = "Created=0";
  private static final String AD_TABLE_ID = "AD_Table_ID";

  private static final String SYSTEM_CLIENT_ID = "0";
  private static final String NON_SYSTEM_CLIENT_ID = "1000000";
  private static final String TEST_ORG_ID = "1000001";
  private static final String TEST_USER_ID = "100";
  private static final String TEST_PROCESS_ID = "PROCESS_123";
  private static final String TEST_PINSTANCE_ID = "PINSTANCE_456";
  private static final String TEST_TABLE_ID = "318";
  private static final String OTHER_TABLE_ID = "4D8C3B3C31D1410DA046140C9F024D17";
  private static final String INVOICE_TABLE = "C_Invoice";
  private static final String PAYMENT_TABLE = "FIN_Payment";
  private static final String OTHER_CLIENT_ID = "2000000";
  private static final String TEST_LANGUAGE = "en_US";
  private static final String TEST_TABLE_DESC = "Test Table";
  private static final String POSTED_ONE_LINE = "  " + TEST_TABLE_DESC + ": posted 1\n";
  private static final String ERROR_PREFIX = "ERROR: ";
  private static final String FINISHED_PREFIX = "Finished in ";
  private static final String CLIENT_PREFIX = "Client ";
  private static final String FAILED_PREFIX = "Failed: ";

  private AcctServerProcess acctServerProcess;

  @Mock
  private ProcessBundle mockBundle;

  @Mock
  private ProcessLogger mockLogger;

  @Mock
  private ConnectionProvider mockConnection;

  @Mock
  private ProcessContext mockContext;

  @Mock
  private VariablesSecureApp mockVars;

  @Mock
  private OBDal mockOBDal;

  @Mock
  private OBCriteria<Client> mockClientCriteria;

  @Mock
  private AcctServer mockAcctServer;

  @Mock
  private AcctServerBatch mockBatch;

  @Mock
  private Client mockClient;

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<AcctServerProcessData> acctServerProcessDataStatic;
  private MockedStatic<AcctServer> acctServerStatic;

  /**
   * Sets up the test environment before each test.
   * Initializes the AcctServerProcess instance and configures mocks.
    * @throws Exception if an error occurs
   */
  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    acctServerProcess = spy(objenesis.newInstance(AcctServerProcess.class));

    // Initialize fields via reflection
    setPrivateField(acctServerProcess, "lastLog", new StringBuffer());
    setPrivateField(acctServerProcess, "message", new StringBuffer());
    setPrivateField(acctServerProcess, "failedDocs", new ArrayList<String>());
    setPrivateField(acctServerProcess, "statusNames", new HashMap<String, String>());
    setPrivateField(acctServerProcess, "notPostableClients", new ArrayList<String>());
    setPrivateField(acctServerProcess, "currentClient", "");
    setPrivateField(acctServerProcess, "currentTable", "");

    obDalStatic = mockStatic(OBDal.class);
    acctServerProcessDataStatic = mockStatic(AcctServerProcessData.class);
    acctServerStatic = mockStatic(AcctServer.class);

    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
    doReturn(mockBatch).when(acctServerProcess).newBatch(any());
  }

  /**
   * Cleans up the test environment after each test.
   * Closes all mocked static instances.
   */
  @After
  public void tearDown() {
    if (obDalStatic != null) {
      obDalStatic.close();
    }
    if (acctServerProcessDataStatic != null) {
      acctServerProcessDataStatic.close();
    }
    if (acctServerStatic != null) {
      acctServerStatic.close();
    }
  }

  /**
   * Tests doExecute with a non-system client.
   * Verifies that processClient is called once for a regular client.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithNonSystemClient() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    // Nothing posted: no client header, and no "not postable" line outside the System pre-filter
    verify(mockLogger, never()).log(CLIENT_PREFIX + NON_SYSTEM_CLIENT_ID + "\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Pending but not postable")));
    verify(mockLogger).log(argThat(msg -> msg.startsWith(FINISHED_PREFIX)
        && !msg.contains("with pending")));
  }

  /**
   * Tests doExecute with system client (client ID = "0").
   * Verifies that only the clients with pending documents are walked, and only for the tables
   * where they have them.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithSystemClientWalksOnlyClientsWithPendingDocs() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    Client idleClient = mock(Client.class);
    when(idleClient.getId()).thenReturn(OTHER_CLIENT_ID);
    when(mockClient.getId()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockClient.getName()).thenReturn("Acme");
    when(mockOBDal.createCriteria(Client.class)).thenReturn(mockClientCriteria);
    when(mockClientCriteria.add(any(Criterion.class))).thenReturn(mockClientCriteria);
    when(mockClientCriteria.list()).thenReturn(Arrays.asList(mockClient, idleClient));

    when(mockContext.getUser()).thenReturn(TEST_USER_ID);
    when(mockContext.getOrganization()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);
    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(mockConnection))
        .thenReturn(new AcctServerProcessData[] { createAcctServerProcessData(TEST_TABLE_ID),
            createAcctServerProcessData(OTHER_TABLE_ID) });
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    // Pending documents: only the first client, only in the first table
    AcctServerProcessData pendingRow = createAcctServerProcessData(null);
    pendingRow.adClientId = NON_SYSTEM_CLIENT_ID;
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTableName(mockConnection,
        TEST_TABLE_ID)).thenReturn(INVOICE_TABLE);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTableName(mockConnection,
        OTHER_TABLE_ID)).thenReturn(PAYMENT_TABLE);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectClientsWithPendingDocs(
        mockConnection, INVOICE_TABLE)).thenReturn(new AcctServerProcessData[] { pendingRow });
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectClientsWithPendingDocs(
        mockConnection, PAYMENT_TABLE)).thenReturn(new AcctServerProcessData[0]);

    acctServerStatic.when(() -> AcctServer.get(anyString(), anyString(), anyString(), any()))
        .thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(any(), any())).thenReturn(false);
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockOBDal).createCriteria(Client.class);
    acctServerStatic.verify(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        anyString(), any()));
    acctServerStatic.verify(() -> AcctServer.get(eq(OTHER_TABLE_ID), anyString(), anyString(),
        any()), never());
    acctServerStatic.verify(() -> AcctServer.get(anyString(), eq(OTHER_CLIENT_ID), anyString(),
        any()), never());
    // No query failed, so the DAL session is kept
    verify(mockOBDal, never()).rollbackAndClose();
    verify(mockLogger).log(argThat(msg -> msg.contains("active clients: 2 / with pending: 1")));
    // Its pending documents could not be selected (checkDocuments false): no header, listed once
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith(CLIENT_PREFIX)));
    verify(mockLogger).log("Pending but not postable (closed period or organization not ready): "
        + NON_SYSTEM_CLIENT_ID + " (Acme)\n");
  }

  /**
   * Tests that a failing table is written to the process log, the next table is still posted and
   * the run ends with an error.
    * @throws Exception if an error occurs
   */
  @Test
  public void testFailingTableIsLoggedAndRunContinuesThenFails() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID, OTHER_TABLE_ID);
    when(mockClient.getId()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockClient.getName()).thenReturn("Acme");
    when(mockOBDal.get(Client.class, NON_SYSTEM_CLIENT_ID)).thenReturn(mockClient);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenThrow(new ServletException("boom"));
    acctServerStatic.when(() -> AcctServer.get(eq(OTHER_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(false);
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);
    // Even a broken session that cannot be discarded must not stop the remaining tables
    doThrow(new IllegalStateException("session closed")).when(mockOBDal).rollbackAndClose();

    expectThrows(OBException.class, () -> acctServerProcess.doExecute(mockBundle));

    verify(mockAcctServer).setBatchSize("50");
    // The error is logged before the session is discarded, so a failing rollback cannot hide it
    InOrder order = inOrder(mockLogger, mockOBDal);
    order.verify(mockLogger).log(
        ERROR_PREFIX + NON_SYSTEM_CLIENT_ID + " (Acme) / " + TEST_TABLE_ID + ": boom\n");
    order.verify(mockOBDal).rollbackAndClose();
    verify(mockLogger).log(argThat(msg -> msg.startsWith(FINISHED_PREFIX)));
  }

  /**
   * Tests that a connection failure aborts the run right away with the original exception.
    * @throws Exception if an error occurs
   */
  @Test
  public void testConnectionFailureAbortsRun() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID, OTHER_TABLE_ID);
    ServletException noConnection = new ServletException("@CODE=NoConnectionAvailable");
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenThrow(noConnection);

    Exception thrown = expectThrows(ServletException.class,
        () -> acctServerProcess.doExecute(mockBundle));

    assertSame(noConnection, thrown);
    acctServerStatic.verify(() -> AcctServer.get(eq(OTHER_TABLE_ID), anyString(), anyString(),
        any()), never());
    verify(mockLogger).log(argThat(msg -> msg.startsWith(ERROR_PREFIX + NON_SYSTEM_CLIENT_ID)));
  }

  /**
   * Tests that two batches in a row that post nothing and fail nothing new stop the table loop,
   * and that the failed documents are listed in the run summary.
    * @throws Exception if an error occurs
   */
  @Test
  public void testNoProgressBatchStopsLoopAndFailedDocsAreSummarized() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    // Bounded, so a broken loop guard fails the run count instead of hanging the test
    when(mockAcctServer.checkDocuments(anyString(), anyString()))
        .thenReturn(true, true, true, false);
    when(mockBatch.getFailedDocs())
        .thenReturn(Collections.singletonMap("DOC1", new String[] { "10000261", "i" }));
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);
    when(mockBatch.getBlockedDoc()).thenReturn(new String[] { "DOC9", "1000123", "L", null });
    doReturn(" - Invalid Account").when(acctServerProcess).postedStatusName("i", TEST_LANGUAGE);
    doReturn(" - Document Locked").when(acctServerProcess).postedStatusName("L", TEST_LANGUAGE);

    acctServerProcess.doExecute(mockBundle);

    verify(mockBatch, times(2)).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log(CLIENT_PREFIX + NON_SYSTEM_CLIENT_ID + "\n");
    verify(mockLogger).log("  Test Table: no progress, stopped at 1000123 (DOC9) / L - Document"
        + " Locked, remaining documents left for the next run\n");
    verify(mockLogger).log("  Test Table: posted 0, failed 1\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Accounting - ")));
    verify(mockLogger).log(argThat(msg -> msg.contains("- failed: 1")));
    verify(mockLogger).log(
        FAILED_PREFIX + NON_SYSTEM_CLIENT_ID + " / Test Table / 10000261 (DOC1) / i - Invalid Account\n");
  }

  /**
   * Tests that the documents a table posted before a later batch threw are still counted and
   * listed: they are already committed.
    * @throws Exception if an error occurs
   */
  @Test
  public void testTableFailingAfterAPostedBatchKeepsItsCounts() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString()))
        .thenReturn(true, true, true, false);
    // First batch posts one document, the second one throws
    when(mockBatch.getSuccess()).thenReturn(0, 1);
    doAnswer(inv -> null).doThrow(new ServletException("deadlock detected"))
        .when(mockBatch).run(any(VariablesSecureApp.class), anyString(), anyString());

    expectThrows(OBException.class, () -> acctServerProcess.doExecute(mockBundle));

    InOrder order = inOrder(mockLogger);
    order.verify(mockLogger).log(POSTED_ONE_LINE);
    order.verify(mockLogger).log(argThat(msg -> msg.startsWith(ERROR_PREFIX)
        && msg.endsWith(": deadlock detected\n")));
    verify(mockLogger).log(argThat(msg -> msg.contains("- posted: 1 - failed: 0")));
  }

  /**
   * Tests that a table stopped for lack of progress names the document whose unexpected error
   * ended the last batch.
    * @throws Exception if an error occurs
   */
  @Test
  public void testNoProgressNamesTheDocumentThatFailedWithAnError() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString()))
        .thenReturn(true, true, true, false);
    when(mockBatch.getBlockedDoc()).thenReturn(new String[] { "DOC9", "", null, "boom" });
    doReturn("Shipment 1 (DOC9)").when(acctServerProcess).documentLabel(any(), eq("DOC9"), eq(""));

    acctServerProcess.doExecute(mockBundle);

    verify(mockBatch, times(2)).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log("  Test Table: no progress, stopped at Shipment 1 (DOC9): boom,"
        + " remaining documents left for the next run\n");
  }

  /**
   * Tests that a failure reading the client list is logged with the System label.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientListFailureIsLoggedWithSystemLabel() throws Exception {
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);
    when(mockOBDal.createCriteria(Client.class)).thenReturn(mockClientCriteria);
    when(mockClientCriteria.add(any(Criterion.class))).thenReturn(mockClientCriteria);
    when(mockClientCriteria.list()).thenThrow(new IllegalStateException("db down"));

    expectThrows(IllegalStateException.class, () -> acctServerProcess.doExecute(mockBundle));

    verify(mockLogger).log("ERROR: System / active clients: db down\n");
  }

  /**
   * Tests that a single idle batch (e.g. a document lock lost to a concurrent posting) is retried
   * instead of leaving the rest of the table for the next run.
    * @throws Exception if an error occurs
   */
  @Test
  public void testSingleIdleBatchIsRetried() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(true, true, false);
    // First batch: 0 -> 0 (idle), second batch: 0 -> 1
    when(mockBatch.getSuccess()).thenReturn(0, 0, 0, 1);

    acctServerProcess.doExecute(mockBundle);

    verify(mockBatch, times(2)).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log(POSTED_ONE_LINE);
    verify(mockLogger, never()).log(argThat(msg -> msg.contains("no progress")));
  }

  /**
   * Tests that a batch which only adds failed documents still counts as progress, so the table
   * loop keeps going until no documents are left.
    * @throws Exception if an error occurs
   */
  @Test
  public void testBatchWithOnlyFailuresKeepsLooping() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(true, true, false);
    Map<String, String[]> oneFailed = failedDocsMap(1);
    Map<String, String[]> twoFailed = failedDocsMap(2);
    // Before and after each batch: 0 -> 1, then 1 -> 2
    when(mockBatch.getFailedDocs()).thenReturn(Collections.emptyMap(), oneFailed, oneFailed,
        twoFailed);
    doReturn("").when(acctServerProcess).postedStatusName(anyString(), anyString());

    acctServerProcess.doExecute(mockBundle);

    verify(mockBatch, times(2)).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log("  Test Table: posted 0, failed 2\n");
  }

  /**
   * Tests that documents failing to post are only reported: the run does not end with an error
   * and the DAL session is not discarded. The "Failed:" lines are capped at MAX_FAILED_LINES.
    * @throws Exception if an error occurs
   */
  @Test
  public void testFailedDocsAreCappedAndDoNotFailTheRun() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn(TEST_TABLE_DESC);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(true, false);
    Map<String, String[]> failed = failedDocsMap(102);
    when(mockBatch.getFailedDocs()).thenReturn(Collections.emptyMap(), failed);
    doReturn(" - Invalid Account").when(acctServerProcess).postedStatusName("i", TEST_LANGUAGE);

    acctServerProcess.doExecute(mockBundle);

    verify(mockOBDal, never()).rollbackAndClose();
    verify(mockLogger).log(argThat(msg -> msg.contains("- failed: 102")));
    verify(mockLogger, times(100)).log(argThat(msg -> msg.startsWith(FAILED_PREFIX)
        && !msg.startsWith("Failed: ... ")));
    verify(mockLogger).log("Failed: ... and 2 more\n");
    verify(mockLogger).log(
        FAILED_PREFIX + NON_SYSTEM_CLIENT_ID + " / Test Table / DOCNO1 (DOC1) / i - Invalid Account\n");
  }

  /**
   * Tests that "Posting by DB tables" (DIRECT channel) run as System walks every client without
   * the pending documents pre-filter, and writes no client header or run summary.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDirectChannelSystemRunSkipsPreFilterAndSummary() throws Exception {
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockClient.getId()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockClient.getName()).thenReturn("Acme");
    when(mockOBDal.createCriteria(Client.class)).thenReturn(mockClientCriteria);
    when(mockClientCriteria.add(any(Criterion.class))).thenReturn(mockClientCriteria);
    when(mockClientCriteria.list()).thenReturn(Collections.singletonList(mockClient));
    when(mockContext.getUser()).thenReturn(TEST_USER_ID);
    when(mockContext.getOrganization()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);
    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);
    setupDirectChannelPInstanceData("", "", "");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(mockConnection))
        .thenReturn(new AcctServerProcessData[] { createAcctServerProcessData(TEST_TABLE_ID) });
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerStatic.when(() -> AcctServer.get(anyString(), anyString(), anyString(), any()))
        .thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(any(), any())).thenReturn(false);
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);

    acctServerProcess.doExecute(mockBundle);

    acctServerProcessDataStatic.verify(() -> AcctServerProcessData.selectClientsWithPendingDocs(
        any(), anyString()), never());
    acctServerStatic.verify(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        anyString(), any()));
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith(CLIENT_PREFIX)
        || msg.startsWith(FINISHED_PREFIX) || msg.startsWith("Pending but not postable")));
  }

  /**
   * Tests that "Posting by DB tables" (DIRECT channel) with a failing table writes the error to
   * the log and ends normally, because its form only shows the log of a run that did not throw.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDirectChannelFailingTableEndsNormallyWithErrorInLog() throws Exception {
    setupNonSystemClientDirectChannel();
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(new AcctServerProcessData[] { createAcctServerProcessData(TEST_TABLE_ID) });
    setupDirectChannelPInstanceData("", "", "");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenThrow(new ServletException("boom"));

    acctServerProcess.doExecute(mockBundle);

    verify(mockLogger).log(ERROR_PREFIX + NON_SYSTEM_CLIENT_ID + " / " + TEST_TABLE_ID + ": boom\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith(FINISHED_PREFIX)));
  }

  /**
   * Tests doExecute when running in DIRECT channel mode.
   * Verifies that direct mode specific parameters are retrieved.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithDirectChannel() throws Exception {
    // Arrange
    setupNonSystemClientDirectChannel();

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    setupDirectChannelPInstanceData("", "", "");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockBundle).getChannel();
    verify(mockBundle).getPinstanceId();
  }

  /**
   * Tests doExecute when a specific table is selected for DIRECT channel.
   * Verifies that only that table is processed.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithSpecificTable() throws Exception {
    // Arrange
    setupNonSystemClientDirectChannel();

    AcctServerProcessData[] tableData = new AcctServerProcessData[1];
    tableData[0] = createAcctServerProcessData(TEST_TABLE_ID);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    setupDirectChannelPInstanceData(TEST_TABLE_ID, "", "");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), eq(TEST_TABLE_ID)))
        .thenReturn(TEST_TABLE_DESC);

    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        eq(TEST_ORG_ID), eq(mockConnection)))
        .thenReturn(mockAcctServer);

    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(false);
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    acctServerStatic.verify(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        eq(TEST_ORG_ID), eq(mockConnection)));
    verify(mockAcctServer).setBatchSize("50");
    verify(mockAcctServer).setBackground(true);
  }

  /**
   * Tests doExecute when AcctServer.get returns null.
   * Verifies that processing continues without error.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWhenAcctServerIsNull() throws Exception {
    // Arrange
    setupNonSystemClientDirectChannel();

    AcctServerProcessData[] tableData = new AcctServerProcessData[1];
    tableData[0] = createAcctServerProcessData(TEST_TABLE_ID);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    setupDirectChannelPInstanceData(TEST_TABLE_ID, "", "");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    // AcctServer.get returns null
    acctServerStatic.when(() -> AcctServer.get(anyString(), anyString(), anyString(), any()))
        .thenReturn(null);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert - should complete without error
    verify(mockAcctServer, never()).setBatchSize(anyString());
  }

  /**
   * Tests doExecute when useRequestProcessOrg preference is enabled.
   * Verifies that the organization from vars is used.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithUseRequestProcessOrgPreference() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(true);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    acctServerProcessDataStatic.verify(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection));
    verify(mockVars).getOrg();
  }

  /**
   * Tests doExecute with client "0" (system) but organization non-zero.
   * Verifies correct table selection query is used.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithSystemClientContextButNonZeroOrg() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID)))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert - selectAcctTable should be called with only client parameter
    acctServerProcessDataStatic.verify(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID)));
  }

  /**
   * Tests doExecute with client "0" and organization "0".
   * Verifies that selectAcctTable without parameters is used.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithZeroClientAndOrg() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(SYSTEM_CLIENT_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(mockConnection))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    acctServerProcessDataStatic.verify(() -> AcctServerProcessData.selectAcctTable(mockConnection));
  }

  /**
   * Tests that checkDocuments loop processes documents correctly.
   * Verifies that run is called when documents are available.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithDocumentsToProcess() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    AcctServerProcessData[] tableData = new AcctServerProcessData[1];
    tableData[0] = createAcctServerProcessData(TEST_TABLE_ID);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), eq(TEST_TABLE_ID)))
        .thenReturn(TEST_TABLE_DESC);

    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        eq("0"), eq(mockConnection)))
        .thenReturn(mockAcctServer);

    // Return true first, then false to simulate processing one batch
    when(mockAcctServer.checkDocuments(anyString(), anyString()))
        .thenReturn(true)
        .thenReturn(false);
    // 0 before the batch, 1 after it
    when(mockBatch.getSuccess()).thenReturn(0, 1);
    setPublicField(mockAcctServer, AD_TABLE_ID, TEST_TABLE_ID);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockBatch).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log(POSTED_ONE_LINE);
    verify(mockLogger).log(argThat(msg -> msg.contains("- posted: 1 - failed: 0")));
  }

  /**
   * Tests doExecute when vars is null and selectUserOrg returns empty array.
   * Verifies that appropriate error logging occurs.
    * @throws Exception if an error occurs
   */
  @Test
  public void testProcessClientWithNullVarsAndEmptyUserOrg() throws Exception {
    // Arrange
    setupNonSystemClientDirectChannel();

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    setupDirectChannelPInstanceData("", "2024-01-01", "2024-12-31");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert - should complete without error
    verify(mockBundle).getPinstanceId();
  }

  /**
   * Tests doExecute when multiple tables need to be processed.
   * Verifies that each table is processed independently.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWithMultipleTables() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    String tableId1 = "318";
    String tableId2 = "319";

    AcctServerProcessData[] tableData = new AcctServerProcessData[2];
    tableData[0] = createAcctServerProcessData(tableId1);
    tableData[1] = createAcctServerProcessData(tableId2);

    AcctServer mockAcctServer1 = mock(AcctServer.class);
    AcctServer mockAcctServer2 = mock(AcctServer.class);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), eq(tableId1)))
        .thenReturn("Table 1");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), eq(tableId2)))
        .thenReturn("Table 2");

    acctServerStatic.when(() -> AcctServer.get(eq(tableId1), eq(NON_SYSTEM_CLIENT_ID),
        eq("0"), eq(mockConnection)))
        .thenReturn(mockAcctServer1);
    acctServerStatic.when(() -> AcctServer.get(eq(tableId2), eq(NON_SYSTEM_CLIENT_ID),
        eq("0"), eq(mockConnection)))
        .thenReturn(mockAcctServer2);

    when(mockAcctServer1.checkDocuments(anyString(), anyString())).thenReturn(false);
    when(mockAcctServer2.checkDocuments(anyString(), anyString())).thenReturn(false);
    when(mockAcctServer1.getInfo(anyString())).thenReturn(CREATED_0);
    when(mockAcctServer2.getInfo(anyString())).thenReturn(CREATED_0);
    setPublicField(mockAcctServer1, AD_TABLE_ID, tableId1);
    setPublicField(mockAcctServer2, AD_TABLE_ID, tableId2);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockAcctServer1).setBatchSize("50");
    verify(mockAcctServer1).setBackground(true);
    verify(mockAcctServer2).setBatchSize("50");
    verify(mockAcctServer2).setBackground(true);
    // Nothing posted or failed: the background log gets no line for these tables
    verify(mockLogger, never()).log(argThat(msg -> msg.contains("Table 1")
        || msg.contains("Table 2")));
  }

  /**
   * Tests doExecute when an exception occurs during acct table selection.
   * Verifies that the process returns early without throwing.
    * @throws Exception if an error occurs
   */
  @Test
  public void testDoExecuteWhenSelectAcctTableThrowsException() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenThrow(new ServletException("Database error"));

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert - process should complete without throwing
    verify(mockLogger).log(argThat(msg -> msg.startsWith(FINISHED_PREFIX)));
  }

  private interface ThrowingRunnable {
    void run() throws Exception;
  }

  private <T extends Exception> T expectThrows(Class<T> expected, ThrowingRunnable action) {
    try {
      action.run();
    } catch (Exception ex) {
      if (expected.isInstance(ex)) {
        return expected.cast(ex);
      }
      throw new AssertionError("Expected " + expected.getName() + " but got " + ex, ex);
    }
    fail("Expected " + expected.getName());
    return null;
  }

  /**
   * Sets up a non-system client in BACKGROUND channel whose accounting tables are the given ones.
   */
  private void setupBackgroundClientWithTables(String... tableIds) {
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);
    when(mockBundle.getChannel()).thenReturn(Channel.BACKGROUND);
    AcctServerProcessData[] tableData = new AcctServerProcessData[tableIds.length];
    for (int i = 0; i < tableIds.length; i++) {
      tableData[i] = createAcctServerProcessData(tableIds[i]);
    }
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
  }

  /** Failed documents DOC1..DOCn, numbered DOCNO1..DOCNOn, all with status "i". */
  private Map<String, String[]> failedDocsMap(int count) {
    Map<String, String[]> failed = new LinkedHashMap<>();
    for (int i = 1; i <= count; i++) {
      failed.put("DOC" + i, new String[] { "DOCNO" + i, "i" });
    }
    return failed;
  }

  /**
   * Sets up mocks for a non-system client with DIRECT channel mode.
   */
  private void setupNonSystemClientDirectChannel() {
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);
  }

  /**
   * Sets up DIRECT channel PInstance data lookup mocks.
   */
  private void setupDirectChannelPInstanceData(String tableId, String dateFrom, String dateTo) {
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTable(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(tableId);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectOrg(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_ORG_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateFrom(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(dateFrom);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateTo(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(dateTo);
  }

  /**
   * Helper method to set up basic bundle mocks.
   */
  private void setupBasicBundleMocks() {
    when(mockBundle.getLogger()).thenReturn(mockLogger);
    when(mockBundle.getConnection()).thenReturn(mockConnection);
    when(mockBundle.getContext()).thenReturn(mockContext);
    when(mockContext.toVars()).thenReturn(mockVars);
    when(mockBundle.getProcessId()).thenReturn(TEST_PROCESS_ID);
  }

  /**
   * Creates an AcctServerProcessData instance for testing.
   *
   * @param tableId the table ID to set
   * @return a new AcctServerProcessData instance
   */
  private AcctServerProcessData createAcctServerProcessData(String tableId) {
    ObjenesisStd objenesis = new ObjenesisStd();
    AcctServerProcessData data = objenesis.newInstance(AcctServerProcessData.class);
    data.adTableId = tableId;
    return data;
  }

  /**
   * Sets a private field value via reflection.
   *
   * @param target the object containing the field
   * @param fieldName the name of the field
   * @param value the value to set
   */
  private void setPrivateField(Object target, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  /**
   * Sets a public field value via reflection for mock objects.
   *
   * @param target the object containing the field
   * @param fieldName the name of the field
   * @param value the value to set
   */
  private void setPublicField(Object target, String fieldName, Object value) {
    try {
      Field field = AcctServer.class.getDeclaredField(fieldName);
      field.setAccessible(true);
      field.set(target, value);
    } catch (Exception e) {
      // Field may not exist on mock, ignore
    }
  }
}
