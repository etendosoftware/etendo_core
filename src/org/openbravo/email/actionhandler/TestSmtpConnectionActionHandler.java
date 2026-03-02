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

import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.client.application.process.ResponseActionsBuilder.MessageType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.ad.access.UserEmailConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.FormatUtilities;

/**
 * Action handler for the "Test SMTP Connection" button. Validates SMTP connectivity and
 * authentication for both user-level ({@link UserEmailConfig}) and organization/client-level
 * ({@link EmailServerConfiguration}) email configurations.
 */
public class TestSmtpConnectionActionHandler extends BaseProcessActionHandler {

  private static final Logger log = LogManager.getLogger();

  private static final long DEFAULT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);
  private static final int DEFAULT_USER_SMTP_PORT = 587;
  private static final int DEFAULT_POC_SMTP_PORT = 25;
  private static final String SMTP_PROTOCOL = "smtp";

  static final String KEY_USER_CONFIG_ID = "AD_User_Email_Config_ID";
  static final String KEY_POC_CONFIG_ID = "C_Poc_Configuration_ID";
  private static final String MSG_KEY_SUCCESS = "SmtpTestSuccess";
  private static final String MSG_KEY_ERROR = "Error";
  private static final String MSG_KEY_SUCCESS_TITLE = "Success";

  private static final String SECURITY_STARTTLS = "STARTTLS";
  private static final String SECURITY_SSL = "SSL";

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    OBContext.setAdminMode(true);
    try {
      JSONObject request = new JSONObject(content);
      return routeTestByConfigType(request);
    } catch (Exception e) {
      log.error("Unexpected error in TestSmtpConnectionActionHandler", e);
      return buildErrorResponse(e.getMessage());
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * Routes the test execution to the appropriate handler based on the configuration type
   * present in the request JSON.
   *
   * @param request the incoming JSON object containing the configuration record identifier
   * @return a JSON response with the test result message
   * @throws Exception if any processing error occurs
   */
  protected JSONObject routeTestByConfigType(JSONObject request) throws Exception {
    String userConfigId = request.optString(KEY_USER_CONFIG_ID, null);
    String pocConfigId = request.optString(KEY_POC_CONFIG_ID, null);
    if (StringUtils.isNotBlank(userConfigId)) {
      return testUserEmailConfig(userConfigId);
    }
    if (StringUtils.isNotBlank(pocConfigId)) {
      return testPocConfiguration(pocConfigId);
    }
    log.error("No recognizable record ID found in request content");
    return buildErrorResponse("No SMTP configuration record identified in the request.");
  }

  /**
   * Tests SMTP connectivity for a user-level email configuration.
   * @param configId the {@code AD_User_Email_Config_ID} to test
   * @return a JSON response indicating success or failure
   * @throws ServletException if password decryption fails
   */
  protected JSONObject testUserEmailConfig(String configId) throws ServletException {
    UserEmailConfig config = OBDal.getInstance().get(UserEmailConfig.class, configId);
    if (config == null) {
      return buildErrorResponse("SMTP configuration record not found: " + configId);
    }
    int port = resolvePort(config.getSmtpPort(), DEFAULT_USER_SMTP_PORT);
    SmtpTestParams params = new SmtpTestParams(
        config.getMailHost(),
        port,
        config.getSmtpConnectionSecurity(),
        config.isSMTPAuthentification(),
        config.getSmtpServerAccount(),
        decryptPassword(config.getSmtpServerPassword()),
        DEFAULT_TIMEOUT_MS
    );
    Exception testError = attemptSmtpConnection(params);
    persistTestResult(config, testError == null);
    return buildTestResponse(testError, config.getMailHost(), port, params.auth);
  }

  /**
   * Tests SMTP connectivity for an organization or client-level email configuration.
   * @param configId the {@code C_Poc_Configuration_ID} to test
   * @return a JSON response indicating success or failure
   * @throws ServletException if password decryption fails
   */
  protected JSONObject testPocConfiguration(String configId) throws ServletException {
    EmailServerConfiguration config = OBDal.getInstance()
        .get(EmailServerConfiguration.class, configId);
    if (config == null) {
      return buildErrorResponse("SMTP configuration record not found: " + configId);
    }
    int port = resolvePort(config.getSmtpPort(), DEFAULT_POC_SMTP_PORT);
    long timeoutMs = resolveTimeout(config.getSmtpConnectionTimeout());
    SmtpTestParams params = new SmtpTestParams(
        config.getSmtpServer(),
        port,
        config.getSmtpConnectionSecurity(),
        config.isSMTPAuthentification(),
        config.getSmtpServerAccount(),
        decryptPassword(config.getSmtpServerPassword()),
        timeoutMs
    );
    Exception testError = attemptSmtpConnection(params);
    persistTestResult(config, testError == null);
    return buildTestResponse(testError, config.getSmtpServer(), port, params.auth);
  }

  /**
   * Attempts an SMTP transport connection using the given parameters. Only tests connectivity
   * and authentication; no message is sent.
   * @param params the SMTP connection parameters
   * @return {@code null} if the connection succeeded, or the caught exception on failure
   */
  protected Exception attemptSmtpConnection(SmtpTestParams params) {
    Transport transport = null;
    try {
      Session mailSession = createMailSession(params);
      transport = mailSession.getTransport(SMTP_PROTOCOL);
      connectTransport(transport, params);
      return null;
    } catch (AuthenticationFailedException e) {
      log.debug("SMTP authentication failed for {}:{} — {}", params.host, params.port,
          e.getMessage());
      return e;
    } catch (MessagingException e) {
      log.debug("SMTP connection test failed for {}:{} — {}", params.host, params.port,
          e.getMessage());
      return e;
    } catch (Exception e) {
      log.debug("Unexpected error during SMTP connection test for {}:{} — {}", params.host,
          params.port, e.getMessage());
      return e;
    } finally {
      closeTransportQuietly(transport);
    }
  }

  /**
   * Creates a JavaMail {@link Session} with the appropriate SMTP properties and optional
   * authenticator.
   * @param params the SMTP connection parameters
   * @return a configured mail session
   */
  protected Session createMailSession(SmtpTestParams params) {
    Properties props = buildSmtpProperties(params);
    if (params.auth) {
      return Session.getInstance(props, new javax.mail.Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(params.username, params.password);
        }
      });
    }
    return Session.getInstance(props);
  }

  /**
   * Connects the SMTP transport, passing credentials only when authentication is enabled.
   * @param transport the SMTP transport to connect
   * @param params the connection parameters
   * @throws MessagingException if the connection or authentication fails
   */
  protected void connectTransport(Transport transport, SmtpTestParams params)
      throws MessagingException {
    if (params.auth) {
      transport.connect(params.host, params.port, params.username, params.password);
    } else {
      transport.connect(params.host, params.port, null, null);
    }
  }

  /**
   * Closes the given transport connection, suppressing any exception that occurs during close.
   * @param transport the transport to close, may be {@code null}
   */
  protected void closeTransportQuietly(Transport transport) {
    if (transport == null) {
      return;
    }
    try {
      if (transport.isConnected()) {
        transport.close();
      }
    } catch (MessagingException e) {
      log.warn("Could not close SMTP transport after test: {}", e.getMessage());
    }
  }

  /**
   * Builds the JavaMail SMTP properties for the given connection parameters.
   *
   * @param params the SMTP connection parameters
   * @return a populated {@link Properties} object
   */
  protected Properties buildSmtpProperties(SmtpTestParams params) {
    Properties props = new Properties();
    props.put("mail.transport.protocol", SMTP_PROTOCOL);
    props.put("mail.smtp.host", params.host);
    props.put("mail.smtp.port", String.valueOf(params.port));
    String timeout = String.valueOf(params.timeoutMs);
    props.put("mail.smtp.timeout", timeout);
    props.put("mail.smtp.connectiontimeout", timeout);
    props.put("mail.smtp.writetimeout", timeout);
    if (params.auth) {
      props.put("mail.smtp.auth", "true");
    }
    applyConnectionSecurity(props, params.connectionSecurity, params.port);
    return props;
  }

  /**
   * Applies the connection security settings (STARTTLS and/or SSL) to the given SMTP properties.
   * Supports comma-separated values in the security string.
   * @param props the properties to modify
   * @param connectionSecurity the security mode string (e.g. "STARTTLS", "SSL", "STARTTLS,SSL")
   * @param port the SMTP port, used for SSL socket factory configuration
   */
  protected void applyConnectionSecurity(Properties props, String connectionSecurity, int port) {
    if (StringUtils.isBlank(connectionSecurity)) {
      return;
    }
    String normalized = StringUtils.deleteWhitespace(connectionSecurity);
    for (String mode : StringUtils.split(normalized, ',')) {
      if (SECURITY_STARTTLS.equals(mode)) {
        props.put("mail.smtp.starttls.enable", "true");
      } else if (SECURITY_SSL.equals(mode)) {
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        props.put("mail.smtp.socketFactory.port", String.valueOf(port));
      }
    }
  }

  /**
   * Decrypts the stored SMTP password. Returns {@code null} if the input is blank.
   * @param encryptedPassword the encrypted password value from the database
   * @return the decrypted password, or {@code null} if input is blank
   * @throws ServletException if decryption fails
   */
  protected String decryptPassword(String encryptedPassword) throws ServletException {
    if (StringUtils.isBlank(encryptedPassword)) {
      return null;
    }
    try {
      return FormatUtilities.encryptDecrypt(encryptedPassword, false);
    } catch (Exception e) {
      log.warn("Password decryption failed (stored as plain text?), using value as-is: {}", e.getMessage());
      return encryptedPassword;
    }
  }

  /**
   * Resolves the effective SMTP port, falling back to the given default when the configured
   * value is {@code null}.
   * @param configuredPort the port from the configuration record, may be {@code null}
   * @param defaultPort the default port to use as fallback
   * @return the resolved port number
   */
  protected int resolvePort(Long configuredPort, int defaultPort) {
    return configuredPort != null ? configuredPort.intValue() : defaultPort;
  }

  /**
   * Resolves the effective connection timeout in milliseconds. Falls back to
   * {@link #DEFAULT_TIMEOUT_MS} when the configured value is {@code null}.
   * @param configuredTimeoutSeconds the timeout in seconds from the configuration, may be
   *   {@code null}
   * @return the timeout in milliseconds
   */
  protected long resolveTimeout(Long configuredTimeoutSeconds) {
    if (configuredTimeoutSeconds == null) {
      return DEFAULT_TIMEOUT_MS;
    }
    return TimeUnit.SECONDS.toMillis(configuredTimeoutSeconds);
  }

  /**
   * Persists the SMTP connection test result on a {@link UserEmailConfig} record.
   * @param config  the user email configuration to update
   * @param success whether the test was successful
   */
  private void persistTestResult(UserEmailConfig config, boolean success) {
    config.setTestsuccessful(success);
    config.setLasttestdate(new Date());
    OBDal.getInstance().save(config);
    OBDal.getInstance().flush();
  }

  /**
   * Persists the SMTP connection test result on an {@link EmailServerConfiguration} record.
   * @param config  the email server configuration to update
   * @param success whether the test was successful
   */
  private void persistTestResult(EmailServerConfiguration config, boolean success) {
    config.setTestSuccessful(success);
    config.setLastTestDate(new Date());
    OBDal.getInstance().save(config);
    OBDal.getInstance().flush();
  }

  /**
   * Builds the appropriate JSON response based on the test outcome.
   * @param testError the exception from the test, or {@code null} if successful
   * @param host the SMTP host that was tested
   * @param port the SMTP port that was tested
   * @param auth whether authentication was attempted during the test
   * @return a success or error JSON response
   */
  private JSONObject buildTestResponse(Exception testError, String host, int port, boolean auth) {
    if (testError == null) {
      return buildSuccessResponse(host, port, auth);
    }
    return buildErrorResponse(testError.getMessage());
  }

  /**
   * Builds a success JSON response for a successful SMTP connection test.
   * When {@code auth} is {@code true}, the message confirms both connectivity and authentication.
   * When {@code auth} is {@code false}, the message only confirms connectivity, since no
   * credentials were verified.
   * @param host the SMTP host
   * @param port the SMTP port
   * @param auth whether authentication was attempted
   * @return a JSON response with a success message
   */
  protected JSONObject buildSuccessResponse(String host, int port, boolean auth) {
    String text = OBMessageUtils.messageBD(MSG_KEY_SUCCESS);
    if (StringUtils.isBlank(text) || StringUtils.equals(text, MSG_KEY_SUCCESS)) {
      text = auth
          ? String.format("Connection to %s:%d established and authenticated successfully.", host, port)
          : String.format("Connection to %s:%d established successfully (authentication disabled — credentials not verified).", host, port);
    }
    return getResponseBuilder()
        .showMsgInView(MessageType.SUCCESS, OBMessageUtils.messageBD(MSG_KEY_SUCCESS_TITLE), text)
        .build();
  }

  /**
   * Builds an error JSON response for a failed SMTP connection test or processing error.
   * @param errorMessage the error detail message
   * @return a JSON response with an error message
   */
  protected JSONObject buildErrorResponse(String errorMessage) {
    String title = OBMessageUtils.messageBD(MSG_KEY_ERROR);
    return getResponseBuilder()
        .showMsgInView(MessageType.ERROR, title, errorMessage)
        .build();
  }

  /**
   * Immutable parameter object encapsulating all values needed for an SMTP connection test.
   * Facilitates passing SMTP configuration across methods without long parameter lists.
   */
  static class SmtpTestParams {
    final String host;
    final int port;
    final String connectionSecurity;
    final boolean auth;
    final String username;
    final String password;
    final long timeoutMs;

    SmtpTestParams(String host, int port, String connectionSecurity, boolean auth,
        String username, String password, long timeoutMs) {
      this.host = host;
      this.port = port;
      this.connectionSecurity = connectionSecurity;
      this.auth = auth;
      this.username = username;
      this.password = password;
      this.timeoutMs = timeoutMs;
    }
  }
}