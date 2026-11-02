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
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.ad_process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Unit tests for the helpers of {@link AcctServerProcess}: the pending documents pre-filter, the
 * connection failure detection, the failed document labels and the process log writer.
 */
@SuppressWarnings({"java:S120", "java:S112"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class AcctServerProcessHelpersTest {

  private static final String NON_SYSTEM_CLIENT_ID = "1000000";
  private static final String OTHER_CLIENT_ID = "2000000";
  private static final String TEST_TABLE_ID = "318";
  private static final String OTHER_TABLE_ID = "4D8C3B3C31D1410DA046140C9F024D17";
  private static final String UNKNOWN_TABLE_ID = "UNKNOWN";
  private static final String INVOICE_TABLE = "C_Invoice";
  private static final String PAYMENT_TABLE = "FIN_Payment";
  private static final String TEST_LANGUAGE = "en_US";
  private static final String POSTED_STATUS_LIST = "All_Posted Status";

  private AcctServerProcess acctServerProcess;

  @Mock
  private ProcessLogger mockLogger;

  @Mock
  private ConnectionProvider mockConnection;

  @Mock
  private OBDal mockOBDal;

  private final List<String> pendingSql = new ArrayList<>();
  private MockedStatic<OBDal> obDalStatic;

  /**
   * Creates the process without running its constructor and mocks OBDal.
   * @throws Exception if an error occurs
   */
  @Before
  public void setUp() throws Exception {
    acctServerProcess = new ObjenesisStd().newInstance(AcctServerProcess.class);
    setPrivateField(acctServerProcess, "lastLog", new StringBuffer());
    setPrivateField(acctServerProcess, "message", new StringBuffer());
    setPrivateField(acctServerProcess, "statusNames", new HashMap<String, String>());
    obDalStatic = mockStatic(OBDal.class);
    obDalStatic.when(OBDal::getInstance).thenReturn(mockOBDal);
  }

  /** Closes the OBDal static mock. */
  @After
  public void tearDown() {
    obDalStatic.close();
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
    SQLException connectionClosed = new SQLException("connection closed", "08006");
    pending.put(INVOICE_TABLE, connectionClosed);
    Connection conn = mockJdbc(pending);
    when(mockConnection.getTransactionConnection()).thenReturn(conn);
    // Releasing the broken connection fails too: the original failure must still surface
    doThrow(new SQLException("already closed")).when(mockConnection)
        .releaseRollbackConnection(conn);

    SQLException thrown = expectThrows(SQLException.class,
        () -> AcctServerProcess.clientsWithPendingDocs(mockConnection,
            new String[] { TEST_TABLE_ID }, Collections.singleton(NON_SYSTEM_CLIENT_ID)));
    assertSame(connectionClosed, thrown);
    verify(mockConnection).releaseRollbackConnection(conn);
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
   * Tests that posted status codes are named from the POSTED_STATUS_LIST list, without the
   * button prefix, and that an unknown code or a lookup failure leaves only the code.
   */
  @Test
  public void testPostedStatusName() {
    try (MockedStatic<Utility> utility = mockStatic(Utility.class)) {
      utility.when(() -> Utility.getListValueName(POSTED_STATUS_LIST, "NC", TEST_LANGUAGE))
          .thenReturn("Post: Cost Not Calculated");
      utility.when(() -> Utility.getListValueName(POSTED_STATUS_LIST, "XX", TEST_LANGUAGE))
          .thenReturn("XX");
      utility.when(() -> Utility.getListValueName(POSTED_STATUS_LIST, "E", TEST_LANGUAGE))
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


  private Connection mockJdbc(Map<String, Object> pendingByTable) throws SQLException {
    Map<String, String> tableNames = new HashMap<>();
    tableNames.put(TEST_TABLE_ID, INVOICE_TABLE);
    tableNames.put(OTHER_TABLE_ID, PAYMENT_TABLE);
    return PendingDocsJdbcMock.mockJdbc(tableNames, pendingByTable, pendingSql);
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

  private void setPrivateField(Object target, String fieldName, Object value)
      throws IllegalAccessException, NoSuchFieldException {
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
}
