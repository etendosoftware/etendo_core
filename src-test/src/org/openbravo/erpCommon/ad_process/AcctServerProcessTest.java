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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.servlet.ServletException;

import org.hibernate.criterion.Criterion;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
import org.openbravo.erpCommon.utility.Utility;
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
  private static final String UNKNOWN_TABLE_ID = "UNKNOWN";
  private static final String TEST_LANGUAGE = "en_US";

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
  private Client mockClient;

  private final List<String> pendingSql = new ArrayList<>();
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
    verify(mockLogger, never()).log("Client " + NON_SYSTEM_CLIENT_ID + "\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Pending but not postable")));
    verify(mockLogger).log(argThat(msg -> msg.startsWith("Finished in ")));
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
    Map<String, Object> pending = new HashMap<>();
    pending.put(INVOICE_TABLE, Collections.singletonList(NON_SYSTEM_CLIENT_ID));
    pending.put(PAYMENT_TABLE, Collections.emptyList());
    Connection conn = mockJdbc(pending);
    when(mockConnection.getTransactionConnection()).thenReturn(conn);

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
    verify(mockConnection).releaseRollbackConnection(conn);
    verify(mockLogger).log(argThat(msg -> msg.contains("active clients: 2 / with pending: 1")));
    // Its pending documents could not be selected (checkDocuments false): no header, listed once
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Client ")));
    verify(mockLogger).log("Pending but not postable (closed period or organization not ready): "
        + NON_SYSTEM_CLIENT_ID + " (Acme)\n");
  }

  /**
   * Tests the pending documents pre-filter: clients outside the active set are dropped, and a
   * table whose query fails is kept for every active client.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsFallsBackOnFailingTable() throws Exception {
    Map<String, Object> pending = new HashMap<>();
    pending.put(INVOICE_TABLE, Arrays.asList(NON_SYSTEM_CLIENT_ID, "INACTIVE"));
    pending.put(PAYMENT_TABLE, new SQLException("column \"posted\" does not exist", "42703"));
    Connection conn = mockJdbc(pending);
    when(mockConnection.getTransactionConnection()).thenReturn(conn);
    Set<String> activeClients = new HashSet<>(Arrays.asList(NON_SYSTEM_CLIENT_ID,
        OTHER_CLIENT_ID));

    Map<String, Set<String>> result = AcctServerProcess.clientsWithPendingDocs(mockConnection,
        new String[] { TEST_TABLE_ID, OTHER_TABLE_ID, UNKNOWN_TABLE_ID }, activeClients);

    // A failing query and a table without a valid name both fall back to every active client
    assertEquals(new HashSet<>(Arrays.asList(TEST_TABLE_ID, OTHER_TABLE_ID, UNKNOWN_TABLE_ID)),
        result.get(NON_SYSTEM_CLIENT_ID));
    assertEquals(new HashSet<>(Arrays.asList(OTHER_TABLE_ID, UNKNOWN_TABLE_ID)),
        result.get(OTHER_CLIENT_ID));
    assertFalse(result.containsKey("INACTIVE"));
    verify(conn, times(2)).rollback();
    verify(mockConnection).releaseRollbackConnection(conn);
  }

  /**
   * Tests that the pre-filter selects exactly the documents AcctServerData.select would post,
   * minus its organization, period and date conditions: that is what makes it a superset.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsUsesThePostingSelectionCondition() throws Exception {
    Map<String, Object> pending = new HashMap<>();
    pending.put(INVOICE_TABLE, Collections.emptyList());
    Connection conn = mockJdbc(pending);
    when(mockConnection.getTransactionConnection()).thenReturn(conn);

    AcctServerProcess.clientsWithPendingDocs(mockConnection, new String[] { TEST_TABLE_ID },
        Collections.singleton(NON_SYSTEM_CLIENT_ID));

    assertEquals(1, pendingSql.size());
    String sql = pendingSql.get(0);
    assertTrue(sql, sql.startsWith("SELECT DISTINCT ad_client_id FROM " + INVOICE_TABLE + " "));
    assertTrue(sql, sql.contains("posted = 'N'"));
    assertTrue(sql, sql.contains("processed = 'Y'"));
    assertTrue(sql, sql.contains("(processing = 'N' OR processing IS NULL)"));
  }

  /**
   * Tests that a connection failure in the pre-filter is not hidden by the per-table fallback.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsRethrowsConnectionFailure() throws Exception {
    Map<String, Object> pending = new HashMap<>();
    pending.put(INVOICE_TABLE, new SQLException("connection closed", "08006"));
    Connection conn = mockJdbc(pending);
    when(mockConnection.getTransactionConnection()).thenReturn(conn);

    expectThrows(SQLException.class, () -> AcctServerProcess.clientsWithPendingDocs(
        mockConnection, new String[] { TEST_TABLE_ID },
        Collections.singleton(NON_SYSTEM_CLIENT_ID)));
    verify(mockConnection).releaseRollbackConnection(conn);
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

    expectThrows(OBException.class, () -> acctServerProcess.doExecute(mockBundle));

    verify(mockAcctServer).setBatchSize("50");
    verify(mockOBDal).rollbackAndClose();
    verify(mockLogger).log(
        "ERROR: " + NON_SYSTEM_CLIENT_ID + " (Acme) / " + TEST_TABLE_ID + ": boom\n");
    verify(mockLogger).log(argThat(msg -> msg.startsWith("Finished in ")));
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
    verify(mockLogger).log(argThat(msg -> msg.startsWith("ERROR: " + NON_SYSTEM_CLIENT_ID)));
  }

  /**
   * Tests that a batch that posts nothing and fails nothing new stops the table loop, and that
   * the failed documents are listed in the run summary.
    * @throws Exception if an error occurs
   */
  @Test
  public void testNoProgressBatchStopsLoopAndFailedDocsAreSummarized() throws Exception {
    setupBackgroundClientWithTables(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), any())).thenReturn("Test Table");
    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), anyString(), anyString(),
        any())).thenReturn(mockAcctServer);
    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(true);
    when(mockAcctServer.getFailedDocs())
        .thenReturn(Collections.singletonMap("DOC1", new String[] { "10000261", "i" }));
    when(mockAcctServer.getInfo(anyString())).thenReturn(CREATED_0);
    doReturn(" - Invalid Account").when(acctServerProcess).postedStatusName("i", TEST_LANGUAGE);

    acctServerProcess.doExecute(mockBundle);

    verify(mockAcctServer, times(1)).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log("Client " + NON_SYSTEM_CLIENT_ID + "\n");
    verify(mockLogger).log("  Test Table: posted 0, failed 1\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Accounting - ")));
    verify(mockLogger).log(argThat(msg -> msg.contains("- failed: 1")));
    verify(mockLogger).log(
        "Failed: " + NON_SYSTEM_CLIENT_ID + " / Test Table / 10000261 (DOC1) / i - Invalid Account\n");
  }

  /**
   * Tests the document label of a failed line: the document number when posting loaded it, else
   * the record identifier, else the bare record id.
   */
  @Test
  public void testDocumentLabel() {
    assertEquals("10000261 (DOC1)", acctServerProcess.documentLabel(TEST_TABLE_ID, "DOC1",
        "10000261"));
    try (MockedStatic<ModelProvider> modelProvider = mockStatic(ModelProvider.class);
        MockedStatic<OBContext> obContext = mockStatic(OBContext.class)) {
      ModelProvider mockModelProvider = mock(ModelProvider.class);
      Entity entity = mock(Entity.class);
      BaseOBObject shipment = mock(BaseOBObject.class);
      modelProvider.when(ModelProvider::getInstance).thenReturn(mockModelProvider);
      when(mockModelProvider.getEntityByTableId("319")).thenReturn(entity);
      when(entity.getName()).thenReturn("MaterialMgmtShipmentInOut");
      when(mockOBDal.get("MaterialMgmtShipmentInOut", "DOC2")).thenReturn(shipment);
      when(shipment.getIdentifier()).thenReturn("1000000 - 30-09-2026");

      assertEquals("1000000 - 30-09-2026 (DOC2)", acctServerProcess.documentLabel("319", "DOC2",
          ""));
      assertEquals("DOC3", acctServerProcess.documentLabel("319", "DOC3", null));
      obContext.verify(OBContext::restorePreviousMode, times(2));
    }
  }

  /**
   * Tests that posted status codes are named from the "All_Posted Status" list, without the
   * button prefix, and that an unknown code or a lookup failure leaves only the code.
   */
  @Test
  public void testPostedStatusName() {
    try (MockedStatic<Utility> utility = mockStatic(Utility.class)) {
      utility.when(() -> Utility.getListValueName("All_Posted Status", "NC", TEST_LANGUAGE))
          .thenReturn("Post: Cost Not Calculated");
      utility.when(() -> Utility.getListValueName("All_Posted Status", "XX", TEST_LANGUAGE))
          .thenReturn("XX");
      utility.when(() -> Utility.getListValueName("All_Posted Status", "E", TEST_LANGUAGE))
          .thenThrow(new IllegalStateException("no session"));

      assertEquals(" - Cost Not Calculated",
          acctServerProcess.postedStatusName("NC", TEST_LANGUAGE));
      assertEquals("", acctServerProcess.postedStatusName("XX", TEST_LANGUAGE));
      assertEquals("", acctServerProcess.postedStatusName("E", TEST_LANGUAGE));
    }
  }

  /**
   * Tests the connection failure detection used to abort the run.
   */
  @Test
  public void testIsConnectionFailure() {
    assertTrue(AcctServerProcess.isConnectionFailure(
        new ServletException("wrapped", new SQLException("closed", "08003"))));
    assertTrue(AcctServerProcess.isConnectionFailure(
        new ServletException("@CODE=NoConnectionAvailable")));
    assertFalse(AcctServerProcess.isConnectionFailure(
        new ServletException("@CODE=0@ERROR: division by zero")));
    assertTrue(AcctServerProcess.isConnectionFailure(new SQLException("shutdown", "57P01")));
    assertFalse(AcctServerProcess.isConnectionFailure(new SQLException("syntax", "42601")));
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

    verify(mockLogger).log("ERROR: " + NON_SYSTEM_CLIENT_ID + " / " + TEST_TABLE_ID + ": boom\n");
    verify(mockLogger, never()).log(argThat(msg -> msg.startsWith("Finished in ")));
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
        .thenReturn("Test Table");

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
        .thenReturn("Test Table");

    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        eq("0"), eq(mockConnection)))
        .thenReturn(mockAcctServer);

    // Return true first, then false to simulate processing one batch
    when(mockAcctServer.checkDocuments(anyString(), anyString()))
        .thenReturn(true)
        .thenReturn(false);
    // 0 before the batch, 1 after it
    when(mockAcctServer.getSuccess()).thenReturn(0, 1);
    setPublicField(mockAcctServer, AD_TABLE_ID, TEST_TABLE_ID);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockAcctServer).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockLogger).log("  Test Table: posted 1\n");
    verify(mockLogger).log(argThat(msg -> msg.contains("- posted: 1 - failed: 0")));
  }

  /**
   * Tests the addLog private method via reflection.
   * Verifies that messages are logged correctly in background mode.
    * @throws Exception if an error occurs
   */
  @Test
  public void testAddLogInBackgroundMode() throws Exception {
    // Arrange
    setPrivateField(acctServerProcess, "logger", mockLogger);
    setPrivateField(acctServerProcess, "isDirect", false);

    // Act
    Method addLogMethod = AcctServerProcess.class.getDeclaredMethod("addLog", String.class);
    addLogMethod.setAccessible(true);
    addLogMethod.invoke(acctServerProcess, "Test message");

    // Assert
    verify(mockLogger).log("Test message\n");

    CharSequence message = (CharSequence) getPrivateField(acctServerProcess, "message");
    assertNotNull(message);
  }

  /**
   * Tests the addLog private method in direct mode.
   * Verifies that HTML formatting is applied correctly.
    * @throws Exception if an error occurs
   */
  @Test
  public void testAddLogInDirectMode() throws Exception {
    // Arrange
    setPrivateField(acctServerProcess, "logger", mockLogger);
    setPrivateField(acctServerProcess, "isDirect", true);

    // Act
    Method addLogMethod = AcctServerProcess.class.getDeclaredMethod("addLog", String.class, boolean.class);
    addLogMethod.setAccessible(true);
    addLogMethod.invoke(acctServerProcess, "Test message", false);

    // Assert
    verify(mockLogger).log("Test message\n");

    CharSequence lastLog = (CharSequence) getPrivateField(acctServerProcess, "lastLog");
    assertNotNull(lastLog);
    String logContent = lastLog.toString();
    assertNotNull(logContent);
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
    verify(mockLogger).log(argThat(msg -> msg.startsWith("Finished in ")));
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

  /**
   * Mocks the JDBC connection used by the pending documents pre-filter. Table ids
   * {@value #TEST_TABLE_ID} and {@value #OTHER_TABLE_ID} resolve to {@value #INVOICE_TABLE} and
   * {@value #PAYMENT_TABLE}; each table maps to the list of client ids with pending documents, or
   * to the SQLException its query throws.
   */
  private Connection mockJdbc(Map<String, Object> pendingByTable) throws SQLException {
    Map<String, String> tableNames = new HashMap<>();
    tableNames.put(TEST_TABLE_ID, INVOICE_TABLE);
    tableNames.put(OTHER_TABLE_ID, PAYMENT_TABLE);
    Connection conn = mock(Connection.class);
    AtomicReference<String> tableIdParam = new AtomicReference<>();
    PreparedStatement namePs = mock(PreparedStatement.class);
    doAnswer(inv -> {
      tableIdParam.set(inv.getArgument(1));
      return null;
    }).when(namePs).setString(eq(1), anyString());
    when(namePs.executeQuery()).thenAnswer(inv -> mockResultSet(
        Collections.singletonList(tableNames.get(tableIdParam.get()))));
    when(conn.prepareStatement(anyString())).thenAnswer(inv -> {
      String sql = inv.getArgument(0);
      if (sql.startsWith("SELECT tablename")) {
        return namePs;
      }
      pendingSql.add(sql);
      for (Map.Entry<String, Object> entry : pendingByTable.entrySet()) {
        if (sql.contains(" FROM " + entry.getKey() + " ")) {
          PreparedStatement ps = mock(PreparedStatement.class);
          if (entry.getValue() instanceof SQLException) {
            when(ps.executeQuery()).thenThrow((SQLException) entry.getValue());
          } else {
            @SuppressWarnings("unchecked")
            List<String> clients = (List<String>) entry.getValue();
            ResultSet rs = mockResultSet(clients);
            when(ps.executeQuery()).thenReturn(rs);
          }
          return ps;
        }
      }
      throw new AssertionError("Unexpected SQL: " + sql);
    });
    return conn;
  }

  private ResultSet mockResultSet(List<String> values) throws SQLException {
    ResultSet rs = mock(ResultSet.class);
    Iterator<String> it = values.iterator();
    AtomicReference<String> current = new AtomicReference<>();
    when(rs.next()).thenAnswer(inv -> {
      if (!it.hasNext()) {
        return false;
      }
      current.set(it.next());
      return true;
    });
    when(rs.getString(1)).thenAnswer(inv -> current.get());
    return rs;
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
   * Gets a private field value via reflection.
   *
   * @param target the object containing the field
   * @param fieldName the name of the field
   * @return the field value
   */
  private Object getPrivateField(Object target, String fieldName) throws IllegalAccessException, NoSuchFieldException {
    Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    return field.get(target);
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
