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
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.email.actionhandler;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.servlet.ServletException;

import org.codehaus.jettison.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.service.OBDal;
import org.openbravo.email.actionhandler.TestSmtpConnectionActionHandler.SmtpTestParams;
import org.openbravo.model.ad.access.UserEmailConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.FormatUtilities;

/**
 * Unit tests for {@link TestSmtpConnectionActionHandler}.
 * Validates routing logic, SMTP property building, connection security application,
 * port/timeout resolution, password decryption, transport lifecycle, and parameter
 * object construction.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class TestSmtpConnectionActionHandlerTest {

  private static final String USER_CONFIG_ID = "USER_CFG_001";
  private static final String POC_CONFIG_ID = "POC_CFG_001";
  private static final String SMTP_HOST = "smtp.example.com";
  private static final String SMTP_USERNAME = "user@example.com";
  private static final String SMTP_ENCRYPTED_CREDENTIAL = "encryptedPass123";
  private static final String SMTP_DECRYPTED_CREDENTIAL = "plainPass123";
  private static final int PORT_587 = 587;
  private static final int PORT_465 = 465;
  private static final int PORT_25 = 25;
  private static final int PORT_2525 = 2525;
  private static final long DEFAULT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
  private static final long CUSTOM_TIMEOUT_SECONDS = 60L;
  private static final long CUSTOM_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(CUSTOM_TIMEOUT_SECONDS);
  private static final String SECURITY_STARTTLS = "STARTTLS";
  private static final String SECURITY_SSL = "SSL";
  private static final String SECURITY_STARTTLS_SSL = "STARTTLS,SSL";
  private static final String SECURITY_STARTTLS_SSL_SPACED = "STARTTLS, SSL";
  private static final String SECURITY_NONE = "NONE";
  private static final String PROP_PROTOCOL = "mail.transport.protocol";
  private static final String PROP_HOST = "mail.smtp.host";
  private static final String PROP_PORT = "mail.smtp.port";
  private static final String PROP_AUTH = "mail.smtp.auth";
  private static final String PROP_TIMEOUT = "mail.smtp.timeout";
  private static final String PROP_CONNECTION_TIMEOUT = "mail.smtp.connectiontimeout";
  private static final String PROP_WRITE_TIMEOUT = "mail.smtp.writetimeout";
  private static final String PROP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";
  private static final String PROP_SSL_FACTORY_CLASS = "mail.smtp.socketFactory.class";
  private static final String PROP_SSL_FACTORY_FALLBACK = "mail.smtp.socketFactory.fallback";
  private static final String PROP_SSL_FACTORY_PORT = "mail.smtp.socketFactory.port";
  private static final String SSL_SOCKET_FACTORY_CLASS = "javax.net.ssl.SSLSocketFactory";
  private static final String MSG_NO_CONFIG_IDENTIFIED = "No SMTP configuration record identified in the request.";
  private static final String MSG_CONFIG_NOT_FOUND_PREFIX = "SMTP configuration record not found: ";
  private static final String MSG_AUTH_FAILED = "Authentication failed";
  private static final String MSG_CONNECTION_REFUSED = "Connection refused";
  private static final String MSG_UNEXPECTED_ERROR = "Unexpected runtime error";
  private static final String TRUE_STRING = "true";
  private static final String FALSE_STRING = "false";

  @Mock private OBDal obDal;
  @Mock private UserEmailConfig userEmailConfig;
  @Mock private EmailServerConfiguration pocConfig;
  @Mock private Transport transport;
  @Mock private Session mailSession;

  private TestSmtpConnectionActionHandler handler;

  /**
   * Initializes the handler spy and configures default mock behavior for each test.
   */
  @BeforeEach
  void setup() {
    handler = spy(new TestSmtpConnectionActionHandler());
  }

  /**
   * Verifies that routing dispatches to {@code testUserEmailConfig} when the request
   * contains an {@code AD_User_Email_Config_ID} key.
   * @throws Exception if JSON parsing or test execution fails
   */
  @Test
  void testRouteTestByConfigTypeWithUserConfigId() throws Exception {
    JSONObject request = new JSONObject();
    request.put(TestSmtpConnectionActionHandler.KEY_USER_CONFIG_ID, USER_CONFIG_ID);
    JSONObject mockResponse = new JSONObject();
    doReturn(mockResponse).when(handler).testUserEmailConfig(USER_CONFIG_ID);
    JSONObject result = handler.routeTestByConfigType(request);
    assertEquals(mockResponse, result);
    verify(handler).testUserEmailConfig(USER_CONFIG_ID);
    verify(handler, never()).testPocConfiguration(anyString());
  }

  /**
   * Verifies that routing dispatches to {@code testPocConfiguration} when the request
   * contains a {@code C_Poc_Configuration_ID} key.
   * @throws Exception if JSON parsing or test execution fails
   */
  @Test
  void testRouteTestByConfigTypeWithPocConfigId() throws Exception {
    JSONObject request = new JSONObject();
    request.put(TestSmtpConnectionActionHandler.KEY_POC_CONFIG_ID, POC_CONFIG_ID);
    JSONObject mockResponse = new JSONObject();
    doReturn(mockResponse).when(handler).testPocConfiguration(POC_CONFIG_ID);
    JSONObject result = handler.routeTestByConfigType(request);
    assertEquals(mockResponse, result);
    verify(handler).testPocConfiguration(POC_CONFIG_ID);
    verify(handler, never()).testUserEmailConfig(anyString());
  }

  /**
   * Verifies that routing prioritizes the user config key when both keys are present
   * in the request.
   * @throws Exception if JSON parsing or test execution fails
   */
  @Test
  void testRouteTestByConfigTypePrioritizesUserOverPoc() throws Exception {
    JSONObject request = new JSONObject();
    request.put(TestSmtpConnectionActionHandler.KEY_USER_CONFIG_ID, USER_CONFIG_ID);
    request.put(TestSmtpConnectionActionHandler.KEY_POC_CONFIG_ID, POC_CONFIG_ID);
    JSONObject mockResponse = new JSONObject();
    doReturn(mockResponse).when(handler).testUserEmailConfig(USER_CONFIG_ID);
    JSONObject result = handler.routeTestByConfigType(request);
    assertEquals(mockResponse, result);
    verify(handler).testUserEmailConfig(USER_CONFIG_ID);
    verify(handler, never()).testPocConfiguration(anyString());
  }

  /**
   * Verifies that routing returns an error response when neither configuration key
   * is present in the request.
   * @throws Exception if JSON parsing fails
   */
  @Test
  void testRouteTestByConfigTypeWithNoConfigId() throws Exception {
    JSONObject request = new JSONObject();
    JSONObject errorResponse = new JSONObject();
    doReturn(errorResponse).when(handler).buildErrorResponse(MSG_NO_CONFIG_IDENTIFIED);
    JSONObject result = handler.routeTestByConfigType(request);
    assertEquals(errorResponse, result);
    verify(handler).buildErrorResponse(MSG_NO_CONFIG_IDENTIFIED);
  }

  /**
   * Verifies that routing returns an error response when the config ID value is blank.
   * @throws Exception if JSON parsing fails
   */
  @Test
  void testRouteTestByConfigTypeWithBlankConfigId() throws Exception {
    JSONObject request = new JSONObject();
    request.put(TestSmtpConnectionActionHandler.KEY_USER_CONFIG_ID, "  ");
    JSONObject errorResponse = new JSONObject();
    doReturn(errorResponse).when(handler).buildErrorResponse(MSG_NO_CONFIG_IDENTIFIED);
    JSONObject result = handler.routeTestByConfigType(request);
    assertEquals(errorResponse, result);
  }

  /**
   * Verifies that testing a user email config returns an error when the record
   * is not found in the database.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testUserEmailConfigNotFound() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(UserEmailConfig.class, USER_CONFIG_ID)).thenReturn(null);
      JSONObject errorResponse = new JSONObject();
      doReturn(errorResponse).when(handler)
          .buildErrorResponse(MSG_CONFIG_NOT_FOUND_PREFIX + USER_CONFIG_ID);
      JSONObject result = handler.testUserEmailConfig(USER_CONFIG_ID);
      assertEquals(errorResponse, result);
    }
  }

  /**
   * Verifies that testing a user email config with a successful SMTP connection
   * persists the success flag and returns a success response.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testUserEmailConfigSuccess() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(UserEmailConfig.class, USER_CONFIG_ID)).thenReturn(userEmailConfig);
      setupUserEmailConfigMock(SMTP_HOST, (long) PORT_587, SECURITY_STARTTLS, true,
          SMTP_USERNAME, SMTP_ENCRYPTED_CREDENTIAL);
      formatMock.when(() -> FormatUtilities.encryptDecrypt(SMTP_ENCRYPTED_CREDENTIAL, false))
          .thenReturn(SMTP_DECRYPTED_CREDENTIAL);
      doReturn(null).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject successResponse = new JSONObject();
      doReturn(successResponse).when(handler).buildSuccessResponse(SMTP_HOST, PORT_587, true);
      JSONObject result = handler.testUserEmailConfig(USER_CONFIG_ID);
      assertEquals(successResponse, result);
      verify(userEmailConfig).setTestsuccessful(true);
      verify(obDal).save(userEmailConfig);
      verify(obDal).flush();
    }
  }

  /**
   * Verifies that testing a user email config with a failed SMTP connection
   * persists the failure flag and returns an error response.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testUserEmailConfigFailure() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(UserEmailConfig.class, USER_CONFIG_ID)).thenReturn(userEmailConfig);
      setupUserEmailConfigMock(SMTP_HOST, (long) PORT_587, SECURITY_STARTTLS, true,
          SMTP_USERNAME, SMTP_ENCRYPTED_CREDENTIAL);
      formatMock.when(() -> FormatUtilities.encryptDecrypt(SMTP_ENCRYPTED_CREDENTIAL, false))
          .thenReturn(SMTP_DECRYPTED_CREDENTIAL);
      MessagingException connectionError = new MessagingException(MSG_CONNECTION_REFUSED);
      doReturn(connectionError).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject errorResponse = new JSONObject();
      doReturn(errorResponse).when(handler).buildErrorResponse(MSG_CONNECTION_REFUSED);
      JSONObject result = handler.testUserEmailConfig(USER_CONFIG_ID);
      assertEquals(errorResponse, result);
      verify(userEmailConfig).setTestsuccessful(false);
      verify(obDal).save(userEmailConfig);
      verify(obDal).flush();
    }
  }

  /**
   * Verifies that the default user SMTP port (587) is used when the configured
   * port is {@code null}.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testUserEmailConfigDefaultPort() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(UserEmailConfig.class, USER_CONFIG_ID)).thenReturn(userEmailConfig);
      setupUserEmailConfigMock(SMTP_HOST, null, SECURITY_STARTTLS, false, null, null);
      doReturn(null).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject successResponse = new JSONObject();
      doReturn(successResponse).when(handler).buildSuccessResponse(SMTP_HOST, PORT_587, false);
      handler.testUserEmailConfig(USER_CONFIG_ID);
      verify(handler).buildSuccessResponse(SMTP_HOST, PORT_587, false);
    }
  }

  /**
   * Verifies that testing a POC configuration returns an error when the record
   * is not found in the database.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testPocConfigurationNotFound() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(EmailServerConfiguration.class, POC_CONFIG_ID)).thenReturn(null);
      JSONObject errorResponse = new JSONObject();
      doReturn(errorResponse).when(handler)
          .buildErrorResponse(MSG_CONFIG_NOT_FOUND_PREFIX + POC_CONFIG_ID);
      JSONObject result = handler.testPocConfiguration(POC_CONFIG_ID);
      assertEquals(errorResponse, result);
    }
  }

  /**
   * Verifies that testing a POC configuration with a successful SMTP connection
   * persists the success flag and returns a success response.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testPocConfigurationSuccess() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(EmailServerConfiguration.class, POC_CONFIG_ID)).thenReturn(pocConfig);
      setupPocConfigMock(SMTP_HOST, (long) PORT_465, SECURITY_SSL, true,
          SMTP_USERNAME, SMTP_ENCRYPTED_CREDENTIAL, CUSTOM_TIMEOUT_SECONDS);
      formatMock.when(() -> FormatUtilities.encryptDecrypt(SMTP_ENCRYPTED_CREDENTIAL, false))
          .thenReturn(SMTP_DECRYPTED_CREDENTIAL);
      doReturn(null).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject successResponse = new JSONObject();
      doReturn(successResponse).when(handler).buildSuccessResponse(SMTP_HOST, PORT_465, true);
      JSONObject result = handler.testPocConfiguration(POC_CONFIG_ID);
      assertEquals(successResponse, result);
      verify(pocConfig).setTestSuccessful(true);
      verify(obDal).save(pocConfig);
      verify(obDal).flush();
    }
  }

  /**
   * Verifies that the default POC SMTP port (25) is used when the configured
   * port is {@code null}.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testPocConfigurationDefaultPort() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(EmailServerConfiguration.class, POC_CONFIG_ID)).thenReturn(pocConfig);
      setupPocConfigMock(SMTP_HOST, null, null, false, null, null, null);
      doReturn(null).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject successResponse = new JSONObject();
      doReturn(successResponse).when(handler).buildSuccessResponse(SMTP_HOST, PORT_25, false);
      handler.testPocConfiguration(POC_CONFIG_ID);
      verify(handler).buildSuccessResponse(SMTP_HOST, PORT_25, false);
    }
  }

  /**
   * Verifies that the default timeout is used for POC configuration when the
   * configured timeout is {@code null}.
   * @throws ServletException if password decryption fails
   */
  @Test
  void testPocConfigurationDefaultTimeout() throws ServletException {
    try (MockedStatic<OBDal> obDalMock = mockStatic(OBDal.class);
         MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      obDalMock.when(OBDal::getInstance).thenReturn(obDal);
      when(obDal.get(EmailServerConfiguration.class, POC_CONFIG_ID)).thenReturn(pocConfig);
      setupPocConfigMock(SMTP_HOST, (long) PORT_25, null, false, null, null, null);
      doReturn(null).when(handler).attemptSmtpConnection(any(SmtpTestParams.class));
      JSONObject successResponse = new JSONObject();
      doReturn(successResponse).when(handler).buildSuccessResponse(SMTP_HOST, PORT_25, false);
      handler.testPocConfiguration(POC_CONFIG_ID);
      verify(handler).resolveTimeout(null);
    }
  }

  /**
   * Verifies that {@code resolvePort} returns the configured port when it is not {@code null}.
   */
  @Test
  void testResolvePortWithConfiguredValue() {
    assertEquals(PORT_465, handler.resolvePort(465L, PORT_587));
  }

  /**
   * Verifies that {@code resolvePort} returns the default port when the configured
   * value is {@code null}.
   */
  @Test
  void testResolvePortWithNullValue() {
    assertEquals(PORT_587, handler.resolvePort(null, PORT_587));
  }

  /**
   * Verifies that {@code resolvePort} correctly handles a non-standard port value.
   */
  @Test
  void testResolvePortWithNonStandardValue() {
    assertEquals(PORT_2525, handler.resolvePort(2525L, PORT_587));
  }

  /**
   * Verifies that {@code resolveTimeout} converts seconds to milliseconds when a
   * value is provided.
   */
  @Test
  void testResolveTimeoutWithConfiguredValue() {
    assertEquals(CUSTOM_TIMEOUT_MS, handler.resolveTimeout(CUSTOM_TIMEOUT_SECONDS));
  }

  /**
   * Verifies that {@code resolveTimeout} returns the default timeout when the
   * configured value is {@code null}.
   */
  @Test
  void testResolveTimeoutWithNullValue() {
    assertEquals(DEFAULT_TIMEOUT_MS, handler.resolveTimeout(null));
  }

  /**
   * Verifies that {@code resolveTimeout} correctly handles a zero-second timeout.
   */
  @Test
  void testResolveTimeoutWithZeroValue() {
    assertEquals(0L, handler.resolveTimeout(0L));
  }

  /**
   * Verifies that {@code decryptPassword} returns {@code null} for a {@code null} input.
   * @throws ServletException if decryption fails
   */
  @Test
  void testDecryptPasswordWithNull() throws ServletException {
    assertNull(handler.decryptPassword(null));
  }

  /**
   * Verifies that {@code decryptPassword} returns {@code null} for an empty string.
   * @throws ServletException if decryption fails
   */
  @Test
  void testDecryptPasswordWithEmpty() throws ServletException {
    assertNull(handler.decryptPassword(""));
  }

  /**
   * Verifies that {@code decryptPassword} returns {@code null} for a blank string.
   * @throws ServletException if decryption fails
   */
  @Test
  void testDecryptPasswordWithBlank() throws ServletException {
    assertNull(handler.decryptPassword("   "));
  }

  /**
   * Verifies that {@code decryptPassword} delegates to {@link FormatUtilities} and
   * returns the decrypted value.
   * @throws ServletException if decryption fails
   */
  @Test
  void testDecryptPasswordWithValidValue() throws ServletException {
    try (MockedStatic<FormatUtilities> formatMock = mockStatic(FormatUtilities.class)) {
      formatMock.when(() -> FormatUtilities.encryptDecrypt(SMTP_ENCRYPTED_CREDENTIAL, false))
          .thenReturn(SMTP_DECRYPTED_CREDENTIAL);
      assertEquals(SMTP_DECRYPTED_CREDENTIAL, handler.decryptPassword(SMTP_ENCRYPTED_CREDENTIAL));
    }
  }

  /**
   * Verifies that {@code buildSmtpProperties} sets all base SMTP properties correctly
   * for an authenticated connection with STARTTLS.
   */
  @Test
  void testBuildSmtpPropertiesWithAuth() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    Properties props = handler.buildSmtpProperties(params);
    assertEquals("smtp", props.get(PROP_PROTOCOL));
    assertEquals(SMTP_HOST, props.get(PROP_HOST));
    assertEquals(String.valueOf(PORT_587), props.get(PROP_PORT));
    assertEquals(TRUE_STRING, props.get(PROP_AUTH));
    assertEquals(String.valueOf(DEFAULT_TIMEOUT_MS), props.get(PROP_TIMEOUT));
    assertEquals(String.valueOf(DEFAULT_TIMEOUT_MS), props.get(PROP_CONNECTION_TIMEOUT));
    assertEquals(String.valueOf(DEFAULT_TIMEOUT_MS), props.get(PROP_WRITE_TIMEOUT));
    assertEquals(TRUE_STRING, props.get(PROP_STARTTLS_ENABLE));
  }

  /**
   * Verifies that {@code buildSmtpProperties} omits the auth property when
   * authentication is disabled.
   */
  @Test
  void testBuildSmtpPropertiesWithoutAuth() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_25, null, false,
        null, null, DEFAULT_TIMEOUT_MS);
    Properties props = handler.buildSmtpProperties(params);
    assertNull(props.get(PROP_AUTH));
    assertNull(props.get(PROP_STARTTLS_ENABLE));
    assertNull(props.get(PROP_SSL_FACTORY_CLASS));
  }

  /**
   * Verifies that {@code buildSmtpProperties} applies SSL socket factory properties
   * when the security mode is SSL.
   */
  @Test
  void testBuildSmtpPropertiesWithSsl() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_465, SECURITY_SSL, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    Properties props = handler.buildSmtpProperties(params);
    assertEquals(SSL_SOCKET_FACTORY_CLASS, props.get(PROP_SSL_FACTORY_CLASS));
    assertEquals(FALSE_STRING, props.get(PROP_SSL_FACTORY_FALLBACK));
    assertEquals(String.valueOf(PORT_465), props.get(PROP_SSL_FACTORY_PORT));
  }

  /**
   * Verifies that {@code applyConnectionSecurity} enables STARTTLS when the security
   * mode is "STARTTLS".
   */
  @Test
  void testApplyConnectionSecurityStarttls() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, SECURITY_STARTTLS, PORT_587);
    assertEquals(TRUE_STRING, props.get(PROP_STARTTLS_ENABLE));
    assertNull(props.get(PROP_SSL_FACTORY_CLASS));
  }

  /**
   * Verifies that {@code applyConnectionSecurity} configures the SSL socket factory
   * when the security mode is "SSL".
   */
  @Test
  void testApplyConnectionSecuritySsl() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, SECURITY_SSL, PORT_465);
    assertEquals(SSL_SOCKET_FACTORY_CLASS, props.get(PROP_SSL_FACTORY_CLASS));
    assertEquals(FALSE_STRING, props.get(PROP_SSL_FACTORY_FALLBACK));
    assertEquals(String.valueOf(PORT_465), props.get(PROP_SSL_FACTORY_PORT));
    assertNull(props.get(PROP_STARTTLS_ENABLE));
  }

  /**
   * Verifies that {@code applyConnectionSecurity} applies both STARTTLS and SSL
   * properties when both modes are specified as a comma-separated value.
   */
  @Test
  void testApplyConnectionSecurityCombined() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, SECURITY_STARTTLS_SSL, PORT_587);
    assertEquals(TRUE_STRING, props.get(PROP_STARTTLS_ENABLE));
    assertEquals(SSL_SOCKET_FACTORY_CLASS, props.get(PROP_SSL_FACTORY_CLASS));
  }

  /**
   * Verifies that {@code applyConnectionSecurity} handles whitespace in comma-separated
   * security values correctly.
   */
  @Test
  void testApplyConnectionSecurityWithSpaces() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, SECURITY_STARTTLS_SSL_SPACED, PORT_587);
    assertEquals(TRUE_STRING, props.get(PROP_STARTTLS_ENABLE));
    assertEquals(SSL_SOCKET_FACTORY_CLASS, props.get(PROP_SSL_FACTORY_CLASS));
  }

  /**
   * Verifies that {@code applyConnectionSecurity} does nothing when the security
   * string is {@code null}.
   */
  @Test
  void testApplyConnectionSecurityWithNull() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, null, PORT_587);
    assertTrue(props.isEmpty());
  }

  /**
   * Verifies that {@code applyConnectionSecurity} does nothing when the security
   * string is blank.
   */
  @Test
  void testApplyConnectionSecurityWithBlank() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, "   ", PORT_587);
    assertTrue(props.isEmpty());
  }

  /**
   * Verifies that {@code applyConnectionSecurity} ignores unrecognized security mode values.
   */
  @Test
  void testApplyConnectionSecurityUnknownMode() {
    Properties props = new Properties();
    handler.applyConnectionSecurity(props, SECURITY_NONE, PORT_587);
    assertNull(props.get(PROP_STARTTLS_ENABLE));
    assertNull(props.get(PROP_SSL_FACTORY_CLASS));
  }

  /**
   * Verifies that {@code createMailSession} creates a session with an authenticator
   * when authentication is enabled.
   */
  @Test
  void testCreateMailSessionWithAuth() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    Session session = handler.createMailSession(params);
    assertNotNull(session);
    assertEquals(TRUE_STRING, session.getProperty(PROP_AUTH));
  }
  
  /**
   * Verifies that {@code createMailSession} creates a session without an authenticator
   * when authentication is disabled.
   */
  @Test
  void testCreateMailSessionWithoutAuth() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_25, null, false,
        null, null, DEFAULT_TIMEOUT_MS);
    Session session = handler.createMailSession(params);
    assertNotNull(session);
    assertNull(session.getProperty(PROP_AUTH));
  }
  
  /**
   * Verifies that {@code connectTransport} passes credentials when authentication
   * is enabled.
   * @throws MessagingException if transport connection fails
   */
  @Test
  void testConnectTransportWithAuth() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    handler.connectTransport(transport, params);
    verify(transport).connect(SMTP_HOST, PORT_587, SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL);
  }

  /**
   * Verifies that {@code connectTransport} passes {@code null} credentials when
   * authentication is disabled.
   * @throws MessagingException if transport connection fails
   */
  @Test
  void testConnectTransportWithoutAuth() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_25, null, false,
        null, null, DEFAULT_TIMEOUT_MS);
    handler.connectTransport(transport, params);
    verify(transport).connect(SMTP_HOST, PORT_25, null, null);
  }

  /**
   * Verifies that {@code closeTransportQuietly} does not throw when given a {@code null}
   * transport.
   */
  @Test
  void testCloseTransportQuietlyWithNull() {
    assertDoesNotThrow(() -> handler.closeTransportQuietly(null));
  }

  /**
   * Verifies that {@code closeTransportQuietly} closes a connected transport.
   * @throws MessagingException if transport close fails
   */
  @Test
  void testCloseTransportQuietlyConnected() throws MessagingException {
    when(transport.isConnected()).thenReturn(true);
    handler.closeTransportQuietly(transport);
    verify(transport).close();
  }

  /**
   * Verifies that {@code closeTransportQuietly} skips closing a disconnected transport.
   * @throws MessagingException if transport close fails
   */
  @Test
  void testCloseTransportQuietlyNotConnected() throws MessagingException {
    when(transport.isConnected()).thenReturn(false);
    handler.closeTransportQuietly(transport);
    verify(transport, never()).close();
  }

  /**
   * Verifies that {@code closeTransportQuietly} suppresses exceptions thrown during close.
   * @throws MessagingException if transport close fails
   */
  @Test
  void testCloseTransportQuietlySuppressesException() throws MessagingException {
    when(transport.isConnected()).thenReturn(true);
    doThrow(new MessagingException("Close failed")).when(transport).close();
    assertDoesNotThrow(() -> handler.closeTransportQuietly(transport));
  }

  /**
   * Verifies that {@code attemptSmtpConnection} returns {@code null} on a successful
   * connection.
   * @throws MessagingException if session or transport operations fail
   */
  @Test
  void testAttemptSmtpConnectionSuccess() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    doReturn(mailSession).when(handler).createMailSession(params);
    when(mailSession.getTransport("smtp")).thenReturn(transport);
    Exception result = handler.attemptSmtpConnection(params);
    assertNull(result);
    verify(handler).connectTransport(transport, params);
    verify(handler).closeTransportQuietly(transport);
  }

  /**
   * Verifies that {@code attemptSmtpConnection} returns the
   * {@link AuthenticationFailedException} on authentication failure.
   * @throws MessagingException if session or transport operations fail
   */
  @Test
  void testAttemptSmtpConnectionAuthFailure() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    doReturn(mailSession).when(handler).createMailSession(params);
    when(mailSession.getTransport("smtp")).thenReturn(transport);
    doThrow(new AuthenticationFailedException(MSG_AUTH_FAILED))
        .when(handler).connectTransport(transport, params);
    Exception result = handler.attemptSmtpConnection(params);
    assertNotNull(result);
    assertTrue(result instanceof AuthenticationFailedException);
    assertEquals(MSG_AUTH_FAILED, result.getMessage());
  }

  /**
   * Verifies that {@code attemptSmtpConnection} returns the {@link MessagingException}
   * on a general messaging error.
   * @throws MessagingException if session or transport operations fail
   */
  @Test
  void testAttemptSmtpConnectionMessagingException() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    doReturn(mailSession).when(handler).createMailSession(params);
    when(mailSession.getTransport("smtp")).thenReturn(transport);
    doThrow(new MessagingException(MSG_CONNECTION_REFUSED))
        .when(handler).connectTransport(transport, params);
    Exception result = handler.attemptSmtpConnection(params);
    assertNotNull(result);
    assertTrue(result instanceof MessagingException);
    assertFalse(result instanceof AuthenticationFailedException);
    assertEquals(MSG_CONNECTION_REFUSED, result.getMessage());
  }

  /**
   * Verifies that {@code attemptSmtpConnection} returns the exception on an unexpected
   * runtime error.
   * @throws MessagingException if session or transport operations fail
   */
  @Test
  void testAttemptSmtpConnectionUnexpectedException() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, DEFAULT_TIMEOUT_MS);
    doReturn(mailSession).when(handler).createMailSession(params);
    when(mailSession.getTransport("smtp")).thenReturn(transport);
    doThrow(new RuntimeException(MSG_UNEXPECTED_ERROR))
        .when(handler).connectTransport(transport, params);
    Exception result = handler.attemptSmtpConnection(params);
    assertNotNull(result);
    assertTrue(result instanceof RuntimeException);
    assertEquals(MSG_UNEXPECTED_ERROR, result.getMessage());
  }

  /**
   * Verifies that {@code attemptSmtpConnection} always invokes
   * {@code closeTransportQuietly} even when an exception occurs.
   * @throws MessagingException if session or transport operations fail
   */
  @Test
  void testAttemptSmtpConnectionClosesTransportOnFailure() throws MessagingException {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, null, false,
        null, null, DEFAULT_TIMEOUT_MS);
    doReturn(mailSession).when(handler).createMailSession(params);
    when(mailSession.getTransport("smtp")).thenReturn(transport);
    doThrow(new MessagingException(MSG_CONNECTION_REFUSED))
        .when(handler).connectTransport(transport, params);
    handler.attemptSmtpConnection(params);
    verify(handler).closeTransportQuietly(transport);
  }

  /**
   * Verifies that {@link SmtpTestParams} correctly stores all provided values.
   */
  @Test
  void testSmtpTestParamsStoresValues() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_587, SECURITY_STARTTLS, true,
        SMTP_USERNAME, SMTP_DECRYPTED_CREDENTIAL, CUSTOM_TIMEOUT_MS);
    assertEquals(SMTP_HOST, params.host);
    assertEquals(PORT_587, params.port);
    assertEquals(SECURITY_STARTTLS, params.connectionSecurity);
    assertTrue(params.auth);
    assertEquals(SMTP_USERNAME, params.username);
    assertEquals(SMTP_DECRYPTED_CREDENTIAL, params.password);
    assertEquals(CUSTOM_TIMEOUT_MS, params.timeoutMs);
  }

  /**
   * Verifies that {@link SmtpTestParams} correctly stores {@code null} values for
   * optional fields.
   */
  @Test
  void testSmtpTestParamsWithNulls() {
    SmtpTestParams params = createParams(SMTP_HOST, PORT_25, null, false,
        null, null, DEFAULT_TIMEOUT_MS);
    assertEquals(SMTP_HOST, params.host);
    assertEquals(PORT_25, params.port);
    assertNull(params.connectionSecurity);
    assertFalse(params.auth);
    assertNull(params.username);
    assertNull(params.password);
    assertEquals(DEFAULT_TIMEOUT_MS, params.timeoutMs);
  }

  /**
   * Creates a {@link SmtpTestParams} instance with the given values.
   * @param host the SMTP host
   * @param port the SMTP port
   * @param connectionSecurity the connection security mode
   * @param auth whether authentication is required
   * @param username the SMTP username
   * @param password the SMTP password (decrypted)
   * @param timeoutMs the connection timeout in milliseconds
   * @return a new {@link SmtpTestParams} instance
   */
  private SmtpTestParams createParams(String host, int port, String connectionSecurity,
      boolean auth, String username, String password, long timeoutMs) {
    return new SmtpTestParams(host, port, connectionSecurity, auth, username, password, timeoutMs);
  }

  /**
   * Configures the {@link UserEmailConfig} mock with the given SMTP settings.
   * @param host the SMTP host
   * @param port the SMTP port, may be {@code null}
   * @param connectionSecurity the connection security mode
   * @param auth whether authentication is required
   * @param username the SMTP username
   * @param encryptedPassword the encrypted SMTP password
   */
  private void setupUserEmailConfigMock(String host, Long port, String connectionSecurity,
      boolean auth, String username, String encryptedPassword) {
    when(userEmailConfig.getMailHost()).thenReturn(host);
    when(userEmailConfig.getSmtpPort()).thenReturn(port);
    when(userEmailConfig.getSmtpConnectionSecurity()).thenReturn(connectionSecurity);
    when(userEmailConfig.isSMTPAuthentification()).thenReturn(auth);
    when(userEmailConfig.getSmtpServerAccount()).thenReturn(username);
    when(userEmailConfig.getSmtpServerPassword()).thenReturn(encryptedPassword);
  }

  /**
   * Configures the {@link EmailServerConfiguration} mock with the given SMTP settings.
   * @param host the SMTP server host
   * @param port the SMTP port, may be {@code null}
   * @param connectionSecurity the connection security mode
   * @param auth whether authentication is required
   * @param username the SMTP username
   * @param encryptedPassword the encrypted SMTP password
   * @param timeoutSeconds the connection timeout in seconds, may be {@code null}
   */
  private void setupPocConfigMock(String host, Long port, String connectionSecurity,
      boolean auth, String username, String encryptedPassword, Long timeoutSeconds) {
    when(pocConfig.getSmtpServer()).thenReturn(host);
    when(pocConfig.getSmtpPort()).thenReturn(port);
    when(pocConfig.getSmtpConnectionSecurity()).thenReturn(connectionSecurity);
    when(pocConfig.isSMTPAuthentification()).thenReturn(auth);
    when(pocConfig.getSmtpServerAccount()).thenReturn(username);
    when(pocConfig.getSmtpServerPassword()).thenReturn(encryptedPassword);
    when(pocConfig.getSmtpConnectionTimeout()).thenReturn(timeoutSeconds);
  }
}
