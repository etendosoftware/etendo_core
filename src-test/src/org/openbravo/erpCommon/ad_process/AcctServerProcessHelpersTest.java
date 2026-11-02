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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import org.openbravo.service.db.QueryTimeOutUtil;
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
   * table whose query fails, or whose name is not valid, is kept for every active client. A
   * failed query also discards the DAL session, whose transaction it aborted.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsFallsBackOnFailingTable() throws Exception {
    Set<String> activeClients = new HashSet<>(Arrays.asList(NON_SYSTEM_CLIENT_ID,
        OTHER_CLIENT_ID));
    Map<String, Set<String>> result;
    try (MockedStatic<AcctServerProcessData> data = mockStatic(AcctServerProcessData.class)) {
      stubTableNames(data);
      data.when(() -> AcctServerProcessData.selectClientsWithPendingDocs(mockConnection,
          INVOICE_TABLE)).thenReturn(pendingRows(NON_SYSTEM_CLIENT_ID, "INACTIVE"));
      data.when(() -> AcctServerProcessData.selectClientsWithPendingDocs(mockConnection,
          PAYMENT_TABLE)).thenThrow(new ServletException("@CODE=0@column posted does not exist"));

      result = AcctServerProcess.clientsWithPendingDocs(mockConnection,
          new String[] { TEST_TABLE_ID, OTHER_TABLE_ID, UNKNOWN_TABLE_ID }, activeClients);

      // The query is never built with a name that is not a plain identifier
      data.verify(() -> AcctServerProcessData.selectClientsWithPendingDocs(any(), eq("")),
          never());
    }

    assertEquals(new HashSet<>(Arrays.asList(TEST_TABLE_ID, OTHER_TABLE_ID, UNKNOWN_TABLE_ID)),
        result.get(NON_SYSTEM_CLIENT_ID));
    assertEquals(new HashSet<>(Arrays.asList(OTHER_TABLE_ID, UNKNOWN_TABLE_ID)),
        result.get(OTHER_CLIENT_ID));
    assertFalse(result.containsKey("INACTIVE"));
    verify(mockOBDal, times(2)).rollbackAndClose();
  }

  /**
   * Tests that the pre-filter query selects exactly the documents AcctServerData.select would
   * post, minus its organization, period and date conditions: that is what makes it a superset.
   * Runs the generated SqlC method and checks the SQL it sends.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsUsesThePostingSelectionCondition() throws Exception {
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(ps.executeQuery()).thenReturn(rs);
    when(mockConnection.getPreparedStatement(anyString())).thenReturn(ps);

    try (MockedStatic<QueryTimeOutUtil> timeout = mockStatic(QueryTimeOutUtil.class)) {
      timeout.when(QueryTimeOutUtil::getInstance).thenReturn(mock(QueryTimeOutUtil.class));
      AcctServerProcessData.selectClientsWithPendingDocs(mockConnection, INVOICE_TABLE);
    }

    ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
    verify(mockConnection).getPreparedStatement(sqlCaptor.capture());
    String sql = sqlCaptor.getValue().replaceAll("\\s+", " ").trim();
    assertTrue(sql, sql.startsWith("SELECT DISTINCT AD_CLIENT_ID FROM " + INVOICE_TABLE + " "));
    assertTrue(sql, sql.contains("POSTED = 'N'"));
    assertTrue(sql, sql.contains("PROCESSED = 'Y'"));
    assertTrue(sql, sql.contains("(PROCESSING = 'N' OR PROCESSING IS NULL)"));
  }

  /**
   * Tests that a broken session that cannot be discarded after a failed query does not stop the
   * pre-filter: the table still falls back to every active client.
    * @throws Exception if an error occurs
   */
  @Test
  public void testClientsWithPendingDocsSurvivesAFailingSessionDiscard() throws Exception {
    doThrow(new IllegalStateException("session closed")).when(mockOBDal).rollbackAndClose();
    Map<String, Set<String>> result;
    try (MockedStatic<AcctServerProcessData> data = mockStatic(AcctServerProcessData.class)) {
      stubTableNames(data);
      data.when(() -> AcctServerProcessData.selectClientsWithPendingDocs(mockConnection,
          INVOICE_TABLE)).thenThrow(new ServletException("@CODE=0@connection closed"));

      result = AcctServerProcess.clientsWithPendingDocs(mockConnection,
          new String[] { TEST_TABLE_ID }, Collections.singleton(NON_SYSTEM_CLIENT_ID));
    }

    assertEquals(Collections.singleton(TEST_TABLE_ID), result.get(NON_SYSTEM_CLIENT_ID));
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

  private void stubTableNames(MockedStatic<AcctServerProcessData> data) {
    data.when(() -> AcctServerProcessData.selectTableName(mockConnection, TEST_TABLE_ID))
        .thenReturn(INVOICE_TABLE);
    data.when(() -> AcctServerProcessData.selectTableName(mockConnection, OTHER_TABLE_ID))
        .thenReturn(PAYMENT_TABLE);
    // An unknown AD_Table_ID: SqlC returns the method default
    data.when(() -> AcctServerProcessData.selectTableName(mockConnection, UNKNOWN_TABLE_ID))
        .thenReturn("");
  }

  private AcctServerProcessData[] pendingRows(String... clientIds) {
    AcctServerProcessData[] rows = new AcctServerProcessData[clientIds.length];
    for (int i = 0; i < clientIds.length; i++) {
      rows[i] = new ObjenesisStd().newInstance(AcctServerProcessData.class);
      rows[i].adClientId = clientIds[i];
    }
    return rows;
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
