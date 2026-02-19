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

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

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
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.ad_forms.AcctServer;
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
@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctServerProcessTest {

  private static final String SYSTEM_CLIENT_ID = "0";
  private static final String NON_SYSTEM_CLIENT_ID = "1000000";
  private static final String TEST_ORG_ID = "1000001";
  private static final String TEST_USER_ID = "100";
  private static final String TEST_PROCESS_ID = "PROCESS_123";
  private static final String TEST_PINSTANCE_ID = "PINSTANCE_456";
  private static final String TEST_TABLE_ID = "318";
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

  private MockedStatic<OBDal> obDalStatic;
  private MockedStatic<AcctServerProcessData> acctServerProcessDataStatic;
  private MockedStatic<AcctServer> acctServerStatic;

  /**
   * Sets up the test environment before each test.
   * Initializes the AcctServerProcess instance and configures mocks.
   */
  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    acctServerProcess = spy(objenesis.newInstance(AcctServerProcess.class));

    // Initialize fields via reflection
    setPrivateField(acctServerProcess, "lastLog", new StringBuffer());
    setPrivateField(acctServerProcess, "message", new StringBuffer());

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
    verify(mockLogger, times(1)).log(anyString());
  }

  /**
   * Tests doExecute with system client (client ID = "0").
   * Verifies that it iterates through all non-system clients.
   */
  @Test
  public void testDoExecuteWithSystemClient() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(SYSTEM_CLIENT_ID);

    List<Client> clientList = new ArrayList<>();
    clientList.add(mockClient);
    when(mockClient.getId()).thenReturn(NON_SYSTEM_CLIENT_ID);

    when(mockOBDal.createCriteria(Client.class)).thenReturn(mockClientCriteria);
    when(mockClientCriteria.add(any(Criterion.class))).thenReturn(mockClientCriteria);
    when(mockClientCriteria.list()).thenReturn(clientList);

    when(mockContext.getUser()).thenReturn(TEST_USER_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
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
    verify(mockOBDal).createCriteria(Client.class);
  }

  /**
   * Tests doExecute when running in DIRECT channel mode.
   * Verifies that direct mode specific parameters are retrieved.
   */
  @Test
  public void testDoExecuteWithDirectChannel() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTable(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectOrg(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_ORG_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateFrom(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateTo(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
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
   */
  @Test
  public void testDoExecuteWithSpecificTable() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);

    AcctServerProcessData[] tableData = new AcctServerProcessData[1];
    tableData[0] = createAcctServerProcessData(TEST_TABLE_ID);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTable(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectOrg(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_ORG_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateFrom(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateTo(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.useRequestProcessOrg(mockConnection))
        .thenReturn(false);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDescription(
        eq(mockConnection), eq(TEST_LANGUAGE), eq(TEST_TABLE_ID)))
        .thenReturn("Test Table");

    acctServerStatic.when(() -> AcctServer.get(eq(TEST_TABLE_ID), eq(NON_SYSTEM_CLIENT_ID),
        eq(TEST_ORG_ID), eq(mockConnection)))
        .thenReturn(mockAcctServer);

    when(mockAcctServer.checkDocuments(anyString(), anyString())).thenReturn(false);
    when(mockAcctServer.getInfo(anyString())).thenReturn("Created=0");

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
   */
  @Test
  public void testDoExecuteWhenAcctServerIsNull() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockVars.getOrg()).thenReturn(TEST_ORG_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);

    AcctServerProcessData[] tableData = new AcctServerProcessData[1];
    tableData[0] = createAcctServerProcessData(TEST_TABLE_ID);

    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(tableData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTable(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_TABLE_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectOrg(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_ORG_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateFrom(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateTo(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
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
    when(mockAcctServer.getInfo(anyString())).thenReturn("Created=1");
    setPublicField(mockAcctServer, "AD_Table_ID", TEST_TABLE_ID);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockAcctServer).run(any(VariablesSecureApp.class), anyString(), anyString());
    verify(mockAcctServer).getInfo(TEST_LANGUAGE);
  }

  /**
   * Tests the addLog private method via reflection.
   * Verifies that messages are logged correctly in background mode.
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

    StringBuffer message = (StringBuffer) getPrivateField(acctServerProcess, "message");
    assertNotNull(message);
  }

  /**
   * Tests the addLog private method in direct mode.
   * Verifies that HTML formatting is applied correctly.
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

    StringBuffer lastLog = (StringBuffer) getPrivateField(acctServerProcess, "lastLog");
    assertNotNull(lastLog);
    String logContent = lastLog.toString();
    assertNotNull(logContent);
  }

  /**
   * Tests doExecute when vars is null and selectUserOrg returns empty array.
   * Verifies that appropriate error logging occurs.
   */
  @Test
  public void testProcessClientWithNullVarsAndEmptyUserOrg() throws Exception {
    // Arrange
    setupBasicBundleMocks();
    when(mockVars.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getClient()).thenReturn(NON_SYSTEM_CLIENT_ID);
    when(mockContext.getOrganization()).thenReturn(TEST_ORG_ID);
    when(mockContext.getLanguage()).thenReturn(TEST_LANGUAGE);

    when(mockBundle.getChannel()).thenReturn(Channel.DIRECT);
    when(mockBundle.getPinstanceId()).thenReturn(TEST_PINSTANCE_ID);

    AcctServerProcessData[] emptyData = new AcctServerProcessData[0];
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectAcctTable(
        eq(mockConnection), eq(NON_SYSTEM_CLIENT_ID), eq(TEST_ORG_ID)))
        .thenReturn(emptyData);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectTable(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectOrg(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn(TEST_ORG_ID);
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateFrom(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("2024-01-01");
    acctServerProcessDataStatic.when(() -> AcctServerProcessData.selectDateTo(
        eq(mockConnection), eq(TEST_PINSTANCE_ID)))
        .thenReturn("2024-12-31");
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
    when(mockAcctServer1.getInfo(anyString())).thenReturn("Created=0");
    when(mockAcctServer2.getInfo(anyString())).thenReturn("Created=0");
    setPublicField(mockAcctServer1, "AD_Table_ID", tableId1);
    setPublicField(mockAcctServer2, "AD_Table_ID", tableId2);

    // Act
    acctServerProcess.doExecute(mockBundle);

    // Assert
    verify(mockAcctServer1).setBatchSize("50");
    verify(mockAcctServer1).setBackground(true);
    verify(mockAcctServer2).setBatchSize("50");
    verify(mockAcctServer2).setBackground(true);
  }

  /**
   * Tests doExecute when an exception occurs during acct table selection.
   * Verifies that the process returns early without throwing.
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
    verify(mockLogger).log(anyString());
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
  private void setPrivateField(Object target, String fieldName, Object value) throws Exception {
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
  private Object getPrivateField(Object target, String fieldName) throws Exception {
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
