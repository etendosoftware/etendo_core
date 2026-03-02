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
import org.openbravo.model.ad.access.UserEmailConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;

/**
 * Unit tests for {@link ResolvedSmtpConfig}.
 * Validates field mapping from both {@link UserEmailConfig} and
 * {@link EmailServerConfiguration} constructors, default port resolution,
 * level assignment, and handling of {@code null} optional fields.
 */
@ExtendWith(MockitoExtension.class)
public class ResolvedSmtpConfigTest {
  private static final String USER_CONFIG_ID = "USER-CFG-001";
  private static final String ORG_CONFIG_ID = "ORG-CFG-001";
  private static final String CLIENT_CONFIG_ID = "CLIENT-CFG-001";
  private static final String USER_HOST = "smtp.user.example.com";
  private static final String ORG_HOST = "smtp.org.example.com";
  private static final long PORT_587 = 587L;
  private static final long PORT_465 = 465L;
  private static final long PORT_2525 = 2525L;
  private static final int DEFAULT_USER_PORT = 587;
  private static final int DEFAULT_POC_PORT = 25;
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

  /**
   * Verifies that all fields are correctly mapped from a fully populated
   * {@link UserEmailConfig}.
   */
  @Test
  void testUserConfigConstructorMapsAllFields() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_587, SECURITY_STARTTLS,
        true, USER_ACCOUNT, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, USER_REPLY_TO);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertEquals(USER_HOST, result.getHost());
    assertEquals(587, result.getPort());
    assertEquals(SECURITY_STARTTLS, result.getConnectionSecurity());
    assertTrue(result.isAuth());
    assertEquals(USER_ACCOUNT, result.getAccount());
    assertEquals(ENCRYPTED_PASSWORD, result.getPassword());
    assertEquals(USER_FROM_ADDRESS, result.getFromAddress());
    assertEquals(USER_FROM_NAME, result.getFromName());
    assertEquals(USER_REPLY_TO, result.getReplyTo());
    assertEquals(ResolvedSmtpConfig.Level.USER, result.getLevel());
    assertEquals(USER_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the user-level constructor always sets the level to {@code USER}.
   */
  @Test
  void testUserConfigConstructorSetsUserLevel() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_587, null,
        false, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertEquals(ResolvedSmtpConfig.Level.USER, result.getLevel());
  }

  /**
   * Verifies that the user-level constructor always sets timeout to {@code null},
   * since timeout is not configurable at user level.
   */
  @Test
  void testUserConfigConstructorSetsNullTimeout() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_587, SECURITY_STARTTLS,
        true, USER_ACCOUNT, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertNull(result.getTimeoutSeconds());
  }

  /**
   * Verifies that the default port ({@code 587}) is used when the user config has
   * a {@code null} port.
   */
  @Test
  void testUserConfigConstructorDefaultPortWhenNull() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, null, null,
        false, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertEquals(DEFAULT_USER_PORT, result.getPort());
  }

  /**
   * Verifies that a custom port is preserved when explicitly set on the user config.
   */
  @Test
  void testUserConfigConstructorCustomPort() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_2525, null,
        false, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertEquals(2525, result.getPort());
  }

  /**
   * Verifies that {@code null} optional fields (fromName, replyTo) are preserved
   * as {@code null} in the resolved config.
   */
  @Test
  void testUserConfigConstructorNullOptionalFields() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_587, null,
        false, null, null, USER_FROM_ADDRESS, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
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
  void testUserConfigConstructorAuthFalse() {
    UserEmailConfig config = buildUserEmailConfig(
        USER_CONFIG_ID, USER_HOST, PORT_587, null,
        false, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config);
    assertFalse(result.isAuth());
  }

  /**
   * Verifies that all fields are correctly mapped from a fully populated
   * {@link EmailServerConfiguration} at ORGANIZATION level.
   */
  @Test
  void testOrgConfigConstructorMapsAllFields() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_465, SECURITY_SSL,
        true, ORG_ACCOUNT, ENCRYPTED_PASSWORD,
        ORG_SENDER_ADDRESS, ORG_FROM_NAME, ORG_REPLY_TO, TIMEOUT_SECONDS);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertEquals(ORG_HOST, result.getHost());
    assertEquals(465, result.getPort());
    assertEquals(SECURITY_SSL, result.getConnectionSecurity());
    assertTrue(result.isAuth());
    assertEquals(ORG_ACCOUNT, result.getAccount());
    assertEquals(ENCRYPTED_PASSWORD, result.getPassword());
    assertEquals(ORG_SENDER_ADDRESS, result.getFromAddress());
    assertEquals(ORG_FROM_NAME, result.getFromName());
    assertEquals(ORG_REPLY_TO, result.getReplyTo());
    assertEquals(TIMEOUT_SECONDS, result.getTimeoutSeconds());
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, result.getLevel());
    assertEquals(ORG_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the level is correctly set to {@code CLIENT} when passed explicitly.
   */
  @Test
  void testOrgConfigConstructorWithClientLevel() {
    EmailServerConfiguration config = buildEmailServerConfig(
        CLIENT_CONFIG_ID, ORG_HOST, PORT_465, null,
        false, null, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.CLIENT);
    assertEquals(ResolvedSmtpConfig.Level.CLIENT, result.getLevel());
    assertEquals(CLIENT_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that the default port ({@code 25}) is used when the POC config has
   * a {@code null} port.
   */
  @Test
  void testOrgConfigConstructorDefaultPortWhenNull() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, null, null,
        false, null, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertEquals(DEFAULT_POC_PORT, result.getPort());
  }

  /**
   * Verifies that a custom port is preserved when explicitly set on the POC config.
   */
  @Test
  void testOrgConfigConstructorCustomPort() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_2525, null,
        false, null, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertEquals(2525, result.getPort());
  }

  /**
   * Verifies that timeout is correctly mapped from the POC config.
   */
  @Test
  void testOrgConfigConstructorMapsTimeout() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_465, SECURITY_SSL,
        true, ORG_ACCOUNT, ENCRYPTED_PASSWORD,
        ORG_SENDER_ADDRESS, ORG_FROM_NAME, null, TIMEOUT_SECONDS);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertEquals(TIMEOUT_SECONDS, result.getTimeoutSeconds());
  }

  /**
   * Verifies that a {@code null} timeout is preserved when the POC config has no
   * timeout configured.
   */
  @Test
  void testOrgConfigConstructorNullTimeout() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_465, null,
        false, null, null, null, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertNull(result.getTimeoutSeconds());
  }

  /**
   * Verifies that {@code null} optional fields are preserved as {@code null} in the
   * resolved config built from a POC configuration.
   */
  @Test
  void testOrgConfigConstructorNullOptionalFields() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_HOST, PORT_465, null,
        false, null, null, ORG_SENDER_ADDRESS, null, null, null);
    ResolvedSmtpConfig result = new ResolvedSmtpConfig(config, ResolvedSmtpConfig.Level.ORGANIZATION);
    assertNull(result.getFromName());
    assertNull(result.getReplyTo());
    assertNull(result.getConnectionSecurity());
    assertNull(result.getAccount());
    assertNull(result.getPassword());
  }

  /**
   * Verifies that the {@link ResolvedSmtpConfig.Level} enum contains exactly the
   * expected three values.
   */
  @Test
  void testLevelEnumValues() {
    ResolvedSmtpConfig.Level[] values = ResolvedSmtpConfig.Level.values();
    assertEquals(3, values.length);
    assertEquals(ResolvedSmtpConfig.Level.USER, values[0]);
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, values[1]);
    assertEquals(ResolvedSmtpConfig.Level.CLIENT, values[2]);
  }

  /**
   * Builds a mocked {@link UserEmailConfig} with the given SMTP settings.
   * @param id the configuration record ID
   * @param host the SMTP host
   * @param port the SMTP port, may be {@code null}
   * @param security the connection security mode
   * @param auth whether authentication is required
   * @param account the SMTP account username
   * @param password the encrypted SMTP password
   * @param fromAddress the sender email address
   * @param fromName the sender display name
   * @param replyTo the reply-to address
   * @return a mocked {@link UserEmailConfig}
   */
  private UserEmailConfig buildUserEmailConfig(String id, String host, Long port,
      String security, boolean auth, String account, String password,
      String fromAddress, String fromName, String replyTo) {
    UserEmailConfig config = mock(UserEmailConfig.class);
    when(config.getId()).thenReturn(id);
    when(config.getMailHost()).thenReturn(host);
    when(config.getSmtpPort()).thenReturn(port);
    when(config.getSmtpConnectionSecurity()).thenReturn(security);
    when(config.isSMTPAuthentification()).thenReturn(auth);
    when(config.getSmtpServerAccount()).thenReturn(account);
    when(config.getSmtpServerPassword()).thenReturn(password);
    when(config.getSmtpserverfromaddress()).thenReturn(fromAddress);
    when(config.getSmtpserverfromname()).thenReturn(fromName);
    when(config.getSmtpreplytoaddress()).thenReturn(replyTo);
    return config;
  }

  /**
   * Builds a mocked {@link EmailServerConfiguration} with the given SMTP settings.
   * @param id the configuration record ID
   * @param host the SMTP server host
   * @param port the SMTP port, may be {@code null}
   * @param security the connection security mode
   * @param auth whether authentication is required
   * @param account the SMTP account username
   * @param password the encrypted SMTP password
   * @param senderAddress the sender email address
   * @param fromName the sender display name
   * @param replyTo the reply-to address
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
