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
package org.openbravo.email;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;

/**
 * Unit tests for {@link ResolvedSmtpConfig}.
 * <p>
 * Validates field mapping from {@link EmailServerConfiguration} at all cascade
 * levels ({@code USER}, {@code ORGANIZATION}, {@code CLIENT}), default port
 * resolution, level assignment, and handling of {@code null} optional fields.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
public class ResolvedSmtpConfigTest {
  private static final String USER_CONFIG_ID = "USER-CFG-001";
  private static final String ORG_CONFIG_ID = "ORG-CFG-001";
  private static final String CLIENT_CONFIG_ID = "CLIENT-CFG-001";
  private static final String USER_HOST = "smtp.user.example.com";
  private static final String ORG_HOST = "smtp.org.example.com";
  private static final Long PORT_465 = 465L;
  private static final int PORT_465_INT = 465;
  private static final Long PORT_2525 = 2525L;
  private static final int PORT_2525_INT = 2525;
  private static final int DEFAULT_SMTP_PORT = 25;
  private static final String SECURITY_STARTTLS = "STARTTLS";
  private static final String SECURITY_SSL = "SSL";
  private static final String USER_ACCOUNT = "user@example.com";
  private static final String USER_FROM_ADDRESS = "sender@example.com";
  private static final String USER_FROM_NAME = "John Doe";
  private static final String USER_REPLY_TO = "reply@example.com";
  private static final String ORG_ACCOUNT = "org@example.com";
  private static final String ORG_SENDER_ADDRESS = "noreply@org.example.com";
  private static final String ORG_FROM_NAME = "Org Sender";
  private static final String ORG_REPLY_TO = "reply@org.example.com";
  private static final String ENCRYPTED_PASSWORD = "encryptedPwd";
  private static final Long TIMEOUT_SECONDS = 600L;
  private static final ResolvedSmtpConfig.Level LEVEL_USER = ResolvedSmtpConfig.Level.USER;
  private static final ResolvedSmtpConfig.Level LEVEL_ORG = ResolvedSmtpConfig.Level.ORGANIZATION;
  private static final ResolvedSmtpConfig.Level LEVEL_CLIENT = ResolvedSmtpConfig.Level.CLIENT;

  private static final int EXPECTED_LEVEL_COUNT = 3;

  /**
   * Verifies that all fields are correctly mapped from a fully populated
   * {@link EmailServerConfiguration} at {@code USER} level.
   */
  @Test
  void testUserLevelConfigMapsAllFields() {
    EmailServerConfiguration config = buildFullUserConfig();
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_USER);
    assertEquals(USER_HOST, result.getHost());
    assertEquals(PORT_465_INT, result.getPort());
    assertEquals(SECURITY_STARTTLS, result.getConnectionSecurity());
    assertTrue(result.isAuth());
    assertEquals(USER_ACCOUNT, result.getAccount());
    assertEquals(ENCRYPTED_PASSWORD, result.getPassword());
    assertEquals(USER_FROM_ADDRESS, result.getFromAddress());
    assertEquals(USER_FROM_NAME, result.getFromName());
    assertEquals(USER_REPLY_TO, result.getReplyTo());
    assertEquals(TIMEOUT_SECONDS, result.getTimeoutSeconds());
    assertEquals(LEVEL_USER, result.getLevel());
    assertEquals(USER_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the user-level constructor correctly sets the level to
   * {@code USER}.
   */
  @Test
  void testUserLevelConfigSetsUserLevel() {
    EmailServerConfiguration config = buildMinimalConfig(USER_CONFIG_ID, USER_HOST, PORT_465);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_USER);
    assertEquals(LEVEL_USER, result.getLevel());
  }

  /**
   * Verifies that the default port ({@value #DEFAULT_SMTP_PORT}) is used when
   * the user config has a {@code null} port.
   */
  @Test
  void testUserLevelConfigDefaultPortWhenNull() {
    EmailServerConfiguration config = buildMinimalConfig(USER_CONFIG_ID, USER_HOST, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_USER);
    assertEquals(DEFAULT_SMTP_PORT, result.getPort());
  }

  /**
   * Verifies that {@code null} optional fields ({@code fromName}, {@code replyTo},
   * {@code connectionSecurity}, {@code account}, {@code password}) are preserved
   * as {@code null} in the resolved config.
   */
  @Test
  void testUserLevelConfigNullOptionalFields() {
    EmailServerConfiguration config = buildMinimalConfig(USER_CONFIG_ID, USER_HOST, PORT_465);
    when(config.getSmtpServerSenderAddress()).thenReturn(USER_FROM_ADDRESS);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_USER);
    assertNull(result.getFromName());
    assertNull(result.getReplyTo());
    assertNull(result.getConnectionSecurity());
    assertNull(result.getAccount());
    assertNull(result.getPassword());
  }

  /**
   * Verifies that {@code auth = false} is correctly mapped from the user config.
   */
  @Test
  void testUserLevelConfigAuthFalse() {
    EmailServerConfiguration config = buildMinimalConfig(USER_CONFIG_ID, USER_HOST, PORT_465);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_USER);
    assertFalse(result.isAuth());
  }
  
  /**
   * Verifies that all fields are correctly mapped from a fully populated
   * {@link EmailServerConfiguration} at {@code ORGANIZATION} level.
   */
  @Test
  void testOrgConfigConstructorMapsAllFields() {
    EmailServerConfiguration config = buildFullOrgConfig();
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertEquals(ORG_HOST, result.getHost());
    assertEquals(PORT_465_INT, result.getPort());
    assertEquals(SECURITY_SSL, result.getConnectionSecurity());
    assertTrue(result.isAuth());
    assertEquals(ORG_ACCOUNT, result.getAccount());
    assertEquals(ENCRYPTED_PASSWORD, result.getPassword());
    assertEquals(ORG_SENDER_ADDRESS, result.getFromAddress());
    assertEquals(ORG_FROM_NAME, result.getFromName());
    assertEquals(ORG_REPLY_TO, result.getReplyTo());
    assertEquals(TIMEOUT_SECONDS, result.getTimeoutSeconds());
    assertEquals(LEVEL_ORG, result.getLevel());
    assertEquals(ORG_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the level is correctly set to {@code CLIENT} when passed
   * explicitly to the constructor.
   */
  @Test
  void testOrgConfigConstructorWithClientLevel() {
    EmailServerConfiguration config = buildMinimalConfig(CLIENT_CONFIG_ID, ORG_HOST, PORT_465);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_CLIENT);
    assertEquals(LEVEL_CLIENT, result.getLevel());
    assertEquals(CLIENT_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the default port ({@value #DEFAULT_SMTP_PORT}) is used when
   * the organization config has a {@code null} port.
   */
  @Test
  void testOrgConfigConstructorDefaultPortWhenNull() {
    EmailServerConfiguration config = buildMinimalConfig(ORG_CONFIG_ID, ORG_HOST, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertEquals(DEFAULT_SMTP_PORT, result.getPort());
  }

  /**
   * Verifies that a custom port is preserved when explicitly set on the
   * organization config.
   */
  @Test
  void testOrgConfigConstructorCustomPort() {
    EmailServerConfiguration config = buildMinimalConfig(ORG_CONFIG_ID, ORG_HOST, PORT_2525);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertEquals(PORT_2525_INT, result.getPort());
  }

  /**
   * Verifies that the timeout value is correctly mapped from the organization
   * config.
   */
  @Test
  void testOrgConfigConstructorMapsTimeout() {
    EmailServerConfiguration config = buildFullOrgConfig();
    when(config.getReplyToAddress()).thenReturn(null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertEquals(TIMEOUT_SECONDS, result.getTimeoutSeconds());
  }

  /**
   * Verifies that a {@code null} timeout is preserved when the organization
   * config has no timeout configured.
   */
  @Test
  void testOrgConfigConstructorNullTimeout() {
    EmailServerConfiguration config = buildMinimalConfig(ORG_CONFIG_ID, ORG_HOST, PORT_465);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertNull(result.getTimeoutSeconds());
  }

  /**
   * Verifies that {@code null} optional fields are preserved as {@code null} in
   * the resolved config built from an organization configuration.
   */
  @Test
  void testOrgConfigConstructorNullOptionalFields() {
    EmailServerConfiguration config = buildMinimalConfig(ORG_CONFIG_ID, ORG_HOST, PORT_465);
    when(config.getSmtpServerSenderAddress()).thenReturn(ORG_SENDER_ADDRESS);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, LEVEL_ORG);
    assertNull(result.getFromName());
    assertNull(result.getReplyTo());
    assertNull(result.getConnectionSecurity());
    assertNull(result.getAccount());
    assertNull(result.getPassword());
  }

  /**
   * Verifies that the {@link ResolvedSmtpConfig.Level} enum contains exactly
   * the expected three values in the correct ordinal order.
   */
  @Test
  void testLevelEnumValues() {
    ResolvedSmtpConfig.Level[] values = ResolvedSmtpConfig.Level.values();
    assertEquals(EXPECTED_LEVEL_COUNT, values.length);
    assertEquals(LEVEL_USER, values[0]);
    assertEquals(LEVEL_ORG, values[1]);
    assertEquals(LEVEL_CLIENT, values[2]);
  }

  /**
   * Builds a fully populated user-level {@link EmailServerConfiguration} mock
   * with all fields set to their {@code USER_*} constant values.
   * @return a mocked {@link EmailServerConfiguration} with user-level data
   */
  private EmailServerConfiguration buildFullUserConfig() {
    return buildEmailServerConfig(
        USER_CONFIG_ID, USER_HOST, PORT_465, SECURITY_STARTTLS,
        true, USER_ACCOUNT, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, USER_REPLY_TO, TIMEOUT_SECONDS);
  }

  /**
   * Builds a fully populated organization-level {@link EmailServerConfiguration}
   * mock with all fields set to their {@code ORG_*} constant values.
   * @return a mocked {@link EmailServerConfiguration} with org-level data
   */
  private EmailServerConfiguration buildFullOrgConfig() {
    return buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_465, SECURITY_SSL,
        true, ORG_ACCOUNT, ENCRYPTED_PASSWORD,
        ORG_SENDER_ADDRESS, ORG_FROM_NAME, ORG_REPLY_TO, TIMEOUT_SECONDS);
  }

  /**
   * Builds a minimal {@link EmailServerConfiguration} mock with only the
   * required identification and connection fields. All optional fields
   * ({@code security}, {@code account}, {@code password}, {@code senderAddress},
   * {@code fromName}, {@code replyTo}, {@code timeout}) default to {@code null},
   * and {@code auth} defaults to {@code false}.
   * @param id   the configuration record ID
   * @param host the SMTP server host
   * @param port the SMTP port, may be {@code null} to test default resolution
   * @return a mocked {@link EmailServerConfiguration} with minimal data
   */
  private EmailServerConfiguration buildMinimalConfig(String id, String host, Long port) {
    return buildEmailServerConfig(
        id, host, port, null,
        false, null, null,
        null, null, null, null);
  }

  /**
   * Builds a mocked {@link EmailServerConfiguration} with the given SMTP
   * settings.
   * @param id the configuration record ID
   * @param host the SMTP server host
   * @param port the SMTP port, may be {@code null}
   * @param security the connection security mode (e.g. {@code STARTTLS},
   *   {@code SSL}), may be {@code null}
   * @param auth SMTP authentication is required
   * @param account the SMTP account username, may be {@code null}
   * @param password the encrypted SMTP password, may be {@code null}
   * @param senderAddress the sender email address, may be {@code null}
   * @param fromName the sender display name, may be {@code null}
   * @param replyTo the reply-to address, may be {@code null}
   * @param timeout the connection timeout in seconds, may be {@code null}
   * @return a mocked {@link EmailServerConfiguration}
   */
  private EmailServerConfiguration buildEmailServerConfig(String id, String host, Long port,
      String security, boolean auth, String account, String password,
      String senderAddress, String fromName, String replyTo, Long timeout) {
    EmailServerConfiguration config = mock(EmailServerConfiguration.class);
    when(config.getId()).thenReturn(id);
    when(config.getSmtpServer()).thenReturn(host);
    when(config.getSmtpPort()).thenReturn(port);
    when(config.getSmtpConnectionSecurity()).thenReturn(security);
    when(config.isSMTPAuthentification()).thenReturn(auth);
    when(config.getSmtpServerAccount()).thenReturn(account);
    when(config.getSmtpServerPassword()).thenReturn(password);
    when(config.getSmtpServerSenderAddress()).thenReturn(senderAddress);
    when(config.getFromName()).thenReturn(fromName);
    when(config.getReplyToAddress()).thenReturn(replyTo);
    when(config.getSmtpConnectionTimeout()).thenReturn(timeout);
    return config;
  }
}
