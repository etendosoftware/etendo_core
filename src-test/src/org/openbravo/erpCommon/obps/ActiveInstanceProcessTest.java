/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2024 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.erpCommon.obps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.net.URL;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.session.SessionFactoryController;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.HttpsUtils;
import org.openbravo.erpCommon.utility.SystemInfo;
import org.openbravo.model.ad.domain.Preference;
import org.openbravo.model.ad.module.Module;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.ad.system.HeartbeatLog;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link ActiveInstanceProcess}.
 *
 * Note: Tests are limited in scope because ActivationKey class has static
 * initialization that calls SessionFactoryController.getInstance() which cannot
 * be fully mocked before class loading. Tests focus on static methods and the
 * private send() method via reflection.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class ActiveInstanceProcessTest {

  private static final String VALUE = "value";
  private static final String SYS123 = "SYS123";
  private static final String DB456 = "DB456";
  private static final String VAL_00_11_22_33_44_55 = "00:11:22:33:44:55";
  private static final String VAL_1_0_0 = "1.0.0";
  private static final String PUBLIC_KEY123 = "publicKey123";
  private static final String SUCCESS = "@Success@";
  private static final String INST123 = "inst123";

  @Mock
  private OBDal obDal;

  @Mock
  private Session session;

  @Mock
  private OBProvider obProvider;

  @Mock
  private Module coreModule;

  @Mock
  private Client client;

  @Mock
  private Organization organization;
  /** Update show production fields with value y creates new preference. */

  @Test
  public void testUpdateShowProductionFieldsWithValueYCreatesNewPreference() {
    // Arrange
    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);
    Preference pref = mock(Preference.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);

      when(obDal.getSession()).thenReturn(session);
      when(session.createQuery(anyString())).thenReturn(updateQuery);
      when(updateQuery.setParameter(eq(VALUE), eq("Y"))).thenReturn(updateQuery);
      when(updateQuery.executeUpdate()).thenReturn(0); // No rows updated
      when(obProvider.get(Preference.class)).thenReturn(pref);

      // Act
      ActiveInstanceProcess.updateShowProductionFields("Y");

      // Assert
      verify(updateQuery).executeUpdate();
      verify(obProvider).get(Preference.class);
      verify(pref).setProperty("showMRPandProductionFields");
      verify(pref).setSearchKey("Y");
      verify(obDal).save(pref);
    }
  }
  /** Update show production fields with value y and rows updated. */

  @Test
  public void testUpdateShowProductionFieldsWithValueYAndRowsUpdated() {
    // Arrange
    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {

      setupOBDalAndOBProvider(obDalStatic, obProviderStatic);
      setupUpdateQuery(updateQuery, "Y", 1);

      // Act
      ActiveInstanceProcess.updateShowProductionFields("Y");

      // Assert
      verify(updateQuery).executeUpdate();
      verify(obProvider, never()).get(Preference.class);
    }
  }
  /** Update show production fields with value n. */

  @Test
  public void testUpdateShowProductionFieldsWithValueN() {
    // Arrange
    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class)) {

      setupOBDalAndOBProvider(obDalStatic, obProviderStatic);
      setupUpdateQuery(updateQuery, "N", 0);

      // Act
      ActiveInstanceProcess.updateShowProductionFields("N");

      // Assert
      verify(updateQuery).executeUpdate();
      // Should not create new preference when value is N
      verify(obProvider, never()).get(Preference.class);
    }
  }
  /** Update show production fields with value n and rows updated. */

  @Test
  public void testUpdateShowProductionFieldsWithValueNAndRowsUpdated() {
    // Arrange
    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.getSession()).thenReturn(session);
      when(session.createQuery(anyString())).thenReturn(updateQuery);
      when(updateQuery.setParameter(eq(VALUE), eq("N"))).thenReturn(updateQuery);
      when(updateQuery.executeUpdate()).thenReturn(5); // Multiple rows updated

      // Act
      ActiveInstanceProcess.updateShowProductionFields("N");

      // Assert
      verify(updateQuery).executeUpdate();
    }
  }
  /**
   * Insert dummy hb log.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDummyHBLog() throws Exception {
    // Arrange
    HeartbeatLog hbLog = mock(HeartbeatLog.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class);
         MockedStatic<SystemInfo> systemInfoStatic = mockStatic(SystemInfo.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
      systemInfoStatic.when(SystemInfo::getSystemIdentifier).thenReturn(SYS123);
      systemInfoStatic.when(SystemInfo::getDBIdentifier).thenReturn(DB456);
      systemInfoStatic.when(SystemInfo::getMacAddress).thenReturn(VAL_00_11_22_33_44_55);

      when(obProvider.get(HeartbeatLog.class)).thenReturn(hbLog);
      when(obDal.get(Client.class, "0")).thenReturn(client);
      when(obDal.get(Organization.class, "0")).thenReturn(organization);

      // Act
      ActiveInstanceProcess.insertDummyHBLog();

      // Assert
      verify(obProvider).get(HeartbeatLog.class);
      verify(hbLog).setClient(client);
      verify(hbLog).setOrganization(organization);
      verify(hbLog).setSystemIdentifier(SYS123);
      verify(hbLog).setDatabaseIdentifier(DB456);
      verify(hbLog).setMacIdentifier(VAL_00_11_22_33_44_55);
      verify(obDal).save(hbLog);
    }
  }
  /**
   * Insert dummy hb log with empty identifiers.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInsertDummyHBLogWithEmptyIdentifiers() throws Exception {
    // Arrange
    HeartbeatLog hbLog = mock(HeartbeatLog.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<OBProvider> obProviderStatic = mockStatic(OBProvider.class);
         MockedStatic<SystemInfo> systemInfoStatic = mockStatic(SystemInfo.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
      systemInfoStatic.when(SystemInfo::getSystemIdentifier).thenReturn("");
      systemInfoStatic.when(SystemInfo::getDBIdentifier).thenReturn("");
      systemInfoStatic.when(SystemInfo::getMacAddress).thenReturn("");

      when(obProvider.get(HeartbeatLog.class)).thenReturn(hbLog);
      when(obDal.get(Client.class, "0")).thenReturn(client);
      when(obDal.get(Organization.class, "0")).thenReturn(organization);

      // Act
      ActiveInstanceProcess.insertDummyHBLog();

      // Assert
      verify(hbLog).setSystemIdentifier("");
      verify(hbLog).setDatabaseIdentifier("");
      verify(hbLog).setMacIdentifier("");
      verify(obDal).save(hbLog);
    }
  }
  /**
   * Send method successful response.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodSuccessfulResponse() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Success@\nactivationKeyValue",
        PUBLIC_KEY123, "P", "instanceNo456", true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
    assertEquals("activationKeyValue", result[1]);
  }
  /**
   * Send method with cancellation.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithCancellation() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Success@\ncancelledKey",
        PUBLIC_KEY123, "E", null, false, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
  }
  /**
   * Send method with connection exception.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithConnectionException() throws Exception {
    String[] result = invokeSendWithMocksThrowingOnSend(VAL_1_0_0, SYS123, DB456,
        new RuntimeException("Connection failed"),
        PUBLIC_KEY123, "P", "instanceNo456", true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("@HB_SECURE_CONNECTION_ERROR@", result[0]);
    assertEquals("", result[1]);
  }
  /**
   * Send method with updated parameter.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithUpdatedParameter() throws Exception {
    String[] result = invokeSendWithMocks("2.0.0", SYS123, DB456,
        "@NoChange@\n",
        PUBLIC_KEY123, "P", "inst789", true, "2024-01-01T00:00:00");

    assertNotNull(result);
    assertEquals("@NoChange@", result[0]);
  }
  /**
   * Send method with empty instance no.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithEmptyInstanceNo() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Success@\nnewActivationKey",
        PUBLIC_KEY123, "E", "", true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
  }
  /**
   * Send method with null instance no.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithNullInstanceNo() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Success@\nnewActivationKey",
        PUBLIC_KEY123, "P", null, true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
    assertEquals("newActivationKey", result[1]);
  }
  /**
   * Send method with evaluation purpose.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithEvaluationPurpose() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Success@\nevaluationKey",
        PUBLIC_KEY123, "E", INST123, true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
    assertEquals("evaluationKey", result[1]);
  }
  /**
   * Send method with error response.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithErrorResponse() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@Error@\nInvalid public key",
        "invalidKey", "P", INST123, true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals("@Error@", result[0]);
    assertEquals("Invalid public key", result[1]);
  }
  /**
   * Set modules as not in development not in web container.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSetModulesAsNotInDevelopmentNotInWebContainer() throws Exception {
    // Arrange
    ActiveInstanceProcess activeInstanceProcess = new ActiveInstanceProcess();

    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class);
         MockedStatic<SessionFactoryController> sessionFactoryStatic = mockStatic(SessionFactoryController.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.getSession()).thenReturn(session);
      when(session.createQuery(anyString())).thenReturn(updateQuery);
      when(updateQuery.executeUpdate()).thenReturn(5);

      sessionFactoryStatic.when(SessionFactoryController::isRunningInWebContainer)
          .thenReturn(false);

      Method setModulesMethod = ActiveInstanceProcess.class.getDeclaredMethod("setModulesAsNotInDevelopment");
      setModulesMethod.setAccessible(true);

      // Act
      setModulesMethod.invoke(activeInstanceProcess);

      // Assert
      verify(session).createQuery(anyString());
      verify(updateQuery).executeUpdate();
    }
  }
  /** Update show production fields query structure. */

  @Test
  public void testUpdateShowProductionFieldsQueryStructure() {
    // Arrange
    @SuppressWarnings("unchecked")
    Query<Object> updateQuery = mock(Query.class);

    try (MockedStatic<OBDal> obDalStatic = mockStatic(OBDal.class)) {

      obDalStatic.when(OBDal::getInstance).thenReturn(obDal);

      when(obDal.getSession()).thenReturn(session);
      when(session.createQuery(anyString())).thenReturn(updateQuery);
      when(updateQuery.setParameter(eq(VALUE), anyString())).thenReturn(updateQuery);
      when(updateQuery.executeUpdate()).thenReturn(1);

      // Act
      ActiveInstanceProcess.updateShowProductionFields("Y");

      // Assert - verify HQL query is executed
      verify(session).createQuery(
          "update ADPreference set searchKey = :value where property = 'showMRPandProductionFields' and module.id is null");
    }
  }
  /**
   * Send method with special characters in public key.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithSpecialCharactersInPublicKey() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, "SYS+123=", "DB/456",
        "@Success@\nencodedKey",
        "publicKey+with/special=chars", "P", INST123, true, null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(SUCCESS, result[0]);
  }
  /**
   * Send method with single line response.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSendMethodWithSingleLineResponse() throws Exception {
    String[] result = invokeSendWithMocks(VAL_1_0_0, SYS123, DB456,
        "@SingleLineError@",
        PUBLIC_KEY123, "P", INST123, true, null);

    assertNotNull(result);
    assertEquals(1, result.length);
    assertEquals("@SingleLineError@", result[0]);
  }

  // ==================== Helper Methods ====================

  private void setupOBDalAndOBProvider(MockedStatic<OBDal> obDalStatic,
      MockedStatic<OBProvider> obProviderStatic) {
    obDalStatic.when(OBDal::getInstance).thenReturn(obDal);
    obProviderStatic.when(OBProvider::getInstance).thenReturn(obProvider);
  }

  private void setupUpdateQuery(Query<Object> updateQuery, String paramValue, int rowsUpdated) {
    when(obDal.getSession()).thenReturn(session);
    when(session.createQuery(anyString())).thenReturn(updateQuery);
    when(updateQuery.setParameter(eq(VALUE), eq(paramValue))).thenReturn(updateQuery);
    when(updateQuery.executeUpdate()).thenReturn(rowsUpdated);
  }

  private void setupSendMethodMocks(MockedStatic<OBDal> obDalMock,
      MockedStatic<SystemInfo> systemInfoMock,
      String coreVersion, String sysId, String dbId) {
    obDalMock.when(OBDal::getInstance).thenReturn(obDal);
    when(obDal.get(Module.class, "0")).thenReturn(coreModule);
    when(coreModule.getVersion()).thenReturn(coreVersion);

    systemInfoMock.when(SystemInfo::getSystemIdentifier).thenReturn(sysId);
    systemInfoMock.when(SystemInfo::getDBIdentifier).thenReturn(dbId);
    systemInfoMock.when(SystemInfo::getMacAddress).thenReturn(VAL_00_11_22_33_44_55);
  }

  private Method getSendMethod() throws NoSuchMethodException {
    Method sendMethod = ActiveInstanceProcess.class.getDeclaredMethod(
        "send", String.class, String.class, String.class, boolean.class, String.class);
    sendMethod.setAccessible(true);
    return sendMethod;
  }

  private String[] invokeSendWithMocks(String coreVersion, String sysId, String dbId,
      String httpsResponse, String publicKey, String purpose, String instanceNo,
      boolean activate, String lastUpdate) throws Exception {
    ActiveInstanceProcess activeInstanceProcess = new ActiveInstanceProcess();

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<SystemInfo> systemInfoMock = mockStatic(SystemInfo.class);
         MockedStatic<HttpsUtils> httpsUtilsMock = mockStatic(HttpsUtils.class)) {

      setupSendMethodMocks(obDalMock, systemInfoMock, coreVersion, sysId, dbId);
      httpsUtilsMock.when(() -> HttpsUtils.sendSecure(any(URL.class), anyString()))
          .thenReturn(httpsResponse);

      return (String[]) getSendMethod().invoke(
          activeInstanceProcess, publicKey, purpose, instanceNo, activate, lastUpdate);
    }
  }

  private String[] invokeSendWithMocksThrowingOnSend(String coreVersion, String sysId, String dbId,
      Throwable exception, String publicKey, String purpose, String instanceNo,
      boolean activate, String lastUpdate) throws Exception {
    ActiveInstanceProcess activeInstanceProcess = new ActiveInstanceProcess();

    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<OBContext> obContextMock = mockStatic(OBContext.class);
         MockedStatic<SystemInfo> systemInfoMock = mockStatic(SystemInfo.class);
         MockedStatic<HttpsUtils> httpsUtilsMock = mockStatic(HttpsUtils.class)) {

      setupSendMethodMocks(obDalMock, systemInfoMock, coreVersion, sysId, dbId);
      httpsUtilsMock.when(() -> HttpsUtils.sendSecure(any(URL.class), anyString()))
          .thenThrow(exception);

      return (String[]) getSendMethod().invoke(
          activeInstanceProcess, publicKey, purpose, instanceNo, activate, lastUpdate);
    }
  }
}
