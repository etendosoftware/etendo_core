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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.User;
import org.openbravo.model.ad.access.UserEmailConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Unit tests for {@link SmtpCascadeResolver}.
 * Validates the cascade resolution logic (User → Organization → Client) and
 * the individual helper methods: {@code resolveUserLevel}, {@code resolveOrgOrClientLevel},
 * {@code findActiveUserEmailConfig}, and {@code determineLevel}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SmtpCascadeResolverTest {
  
  private static final String USER_CONFIG_ID = "USER-CONFIG-001";
  private static final String ORG_CONFIG_ID = "ORG-CONFIG-001";
  private static final String CLIENT_CONFIG_ID = "CLIENT-CONFIG-001";
  private static final String USER_ID = "USER-001";
  private static final String ORG_ID = "ORG-001";
  private static final String ROOT_ORG_ID = "0";
  private static final String USER_SMTP_HOST = "smtp.user.example.com";
  private static final String ORG_SMTP_HOST = "smtp.org.example.com";
  private static final String CLIENT_SMTP_HOST = "smtp.client.example.com";
  private static final long PORT_587 = 587L;
  private static final long PORT_465 = 465L;
  private static final long PORT_25 = 25L;
  private static final String USER_FROM_ADDRESS = "user@example.com";
  private static final String ORG_FROM_ADDRESS = "noreply@org.example.com";
  private static final String CLIENT_FROM_ADDRESS = "erp@client.example.com";
  private static final String USER_FROM_NAME = "John Doe";
  private static final String ORG_FROM_NAME = "Org Name";
  private static final String SECURITY_STARTTLS = "STARTTLS";
  private static final String SECURITY_SSL = "SSL";
  private static final String SECURITY_NONE = "N";
  private static final String ENCRYPTED_PASSWORD = "encryptedPwd";
  private static final long TIMEOUT_SECONDS = 600L;
  
  private MockedStatic<OBContext> mockedOBContext;
  private MockedStatic<OBDal> mockedOBDal;
  private MockedStatic<EmailUtils> mockedEmailUtils;

  @Mock private OBContext mockContext;
  @Mock private OBDal mockDal;
  @Mock private User mockUser;
  @Mock private Organization mockOrg;

  @SuppressWarnings("unchecked")
  @Mock private OBCriteria<UserEmailConfig> mockUserCriteria;

  /**
   * Opens static mocks and configures default behavior for OBContext, OBDal, and
   * the user criteria before each test.
   */
  @BeforeEach
  void setUp() {
    mockedOBContext = mockStatic(OBContext.class);
    mockedOBDal = mockStatic(OBDal.class);
    mockedEmailUtils = mockStatic(EmailUtils.class);

    mockedOBContext.when(OBContext::getOBContext).thenReturn(mockContext);
    mockedOBDal.when(OBDal::getInstance).thenReturn(mockDal);

    when(mockContext.getUser()).thenReturn(mockUser);
    when(mockContext.getCurrentOrganization()).thenReturn(mockOrg);
    when(mockUser.getId()).thenReturn(USER_ID);
    when(mockOrg.getId()).thenReturn(ORG_ID);
    when(mockDal.createCriteria(UserEmailConfig.class)).thenReturn(mockUserCriteria);
    when(mockUserCriteria.add(any())).thenReturn(mockUserCriteria);
  }

  /**
   * Closes all static mocks after each test.
   */
  @AfterEach
  void tearDown() {
    mockedOBContext.close();
    mockedOBDal.close();
    mockedEmailUtils.close();
  }

  /**
   * Verifies that {@code resolve()} returns a USER-level config when the current
   * user has an active SMTP configuration.
   */
  @Test
  void testResolveReturnsUserLevelConfig() {
    UserEmailConfig userConfig = buildUserEmailConfig(
        USER_CONFIG_ID, USER_SMTP_HOST, PORT_587, SECURITY_STARTTLS,
        true, USER_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, null);
    when(mockUserCriteria.list()).thenReturn(List.of(userConfig));
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolve();
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.USER, result.getLevel());
    assertEquals(USER_CONFIG_ID, result.getConfigId());
    assertEquals(USER_SMTP_HOST, result.getHost());
    assertEquals(587, result.getPort());
    assertEquals(USER_FROM_ADDRESS, result.getFromAddress());
    assertEquals(USER_FROM_NAME, result.getFromName());
    assertEquals(SECURITY_STARTTLS, result.getConnectionSecurity());
  }

  /**
   * Verifies that {@code resolve()} falls through to ORGANIZATION level when the
   * user has no config but the organization does.
   */
  @Test
  void testResolveReturnsOrgLevelConfig() {
    when(mockUserCriteria.list()).thenReturn(Collections.emptyList());
    EmailServerConfiguration orgConfig = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_ID, ORG_SMTP_HOST, PORT_465, SECURITY_SSL,
        true, ORG_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        ORG_FROM_ADDRESS, ORG_FROM_NAME, null, TIMEOUT_SECONDS);
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(orgConfig);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolve();
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, result.getLevel());
    assertEquals(ORG_CONFIG_ID, result.getConfigId());
    assertEquals(ORG_SMTP_HOST, result.getHost());
    assertEquals(465, result.getPort());
    assertEquals(SECURITY_SSL, result.getConnectionSecurity());
    assertEquals(ORG_FROM_ADDRESS, result.getFromAddress());
    assertEquals(ORG_FROM_NAME, result.getFromName());
  }

  /**
   * Verifies that {@code resolve()} falls through to CLIENT level when neither user
   * nor organization configs exist.
   */
  @Test
  void testResolveReturnsClientLevelConfig() {
    when(mockUserCriteria.list()).thenReturn(Collections.emptyList());
    EmailServerConfiguration clientConfig = buildEmailServerConfig(
        CLIENT_CONFIG_ID, ROOT_ORG_ID, CLIENT_SMTP_HOST, PORT_25, SECURITY_NONE,
        false, null, null,
        CLIENT_FROM_ADDRESS, null, null, TIMEOUT_SECONDS);
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(clientConfig);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolve();
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.CLIENT, result.getLevel());
    assertEquals(CLIENT_CONFIG_ID, result.getConfigId());
    assertEquals(CLIENT_SMTP_HOST, result.getHost());
    assertEquals(25, result.getPort());
    assertEquals(CLIENT_FROM_ADDRESS, result.getFromAddress());
    assertNull(result.getFromName());
  }

  /**
   * Verifies that {@code resolve()} returns {@code null} when no configuration exists
   * at any level.
   */
  @Test
  void testResolveReturnsNullWhenNoConfig() {
    when(mockUserCriteria.list()).thenReturn(Collections.emptyList());
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(null);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolve();
    assertNull(result);
  }

  /**
   * Verifies that {@code resolve()} skips the user level and proceeds to org/client
   * when the user-level query throws an exception.
   */
  @Test
  void testResolveSkipsUserLevelOnException() {
    when(mockUserCriteria.list()).thenThrow(new RuntimeException("Database error"));
    EmailServerConfiguration orgConfig = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_ID, ORG_SMTP_HOST, PORT_465, SECURITY_SSL,
        true, ORG_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        ORG_FROM_ADDRESS, ORG_FROM_NAME, null, TIMEOUT_SECONDS);
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(orgConfig);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolve();
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, result.getLevel());
  }

  /**
   * Verifies that {@code resolveUserLevel} returns a USER-level config when an active
   * record exists for the given user.
   */
  @Test
  void testResolveUserLevelWithActiveConfig() {
    UserEmailConfig userConfig = buildUserEmailConfig(
        USER_CONFIG_ID, USER_SMTP_HOST, PORT_587, SECURITY_STARTTLS,
        true, USER_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, null);
    when(mockUserCriteria.list()).thenReturn(List.of(userConfig));
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveUserLevel(mockUser);
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.USER, result.getLevel());
    assertEquals(USER_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that {@code resolveUserLevel} returns {@code null} when no active config
   * exists for the given user.
   */
  @Test
  void testResolveUserLevelWithNoConfig() {
    when(mockUserCriteria.list()).thenReturn(Collections.emptyList());
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveUserLevel(mockUser);
    assertNull(result);
  }

  /**
   * Verifies that {@code resolveUserLevel} returns {@code null} and does not propagate
   * the exception when the criteria query fails.
   */
  @Test
  void testResolveUserLevelReturnsNullOnException() {
    when(mockUserCriteria.list()).thenThrow(new RuntimeException("Database error"));
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveUserLevel(mockUser);
    assertNull(result);
  }

  /**
   * Verifies that {@code findActiveUserEmailConfig} returns the first matching active
   * config for the given user.
   */
  @Test
  void testFindActiveUserEmailConfigFound() {
    UserEmailConfig userConfig = buildUserEmailConfig(
        USER_CONFIG_ID, USER_SMTP_HOST, PORT_587, SECURITY_STARTTLS,
        true, USER_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        USER_FROM_ADDRESS, USER_FROM_NAME, null);
    when(mockUserCriteria.list()).thenReturn(List.of(userConfig));
    UserEmailConfig result = SmtpCascadeResolver.findActiveUserEmailConfig(mockUser);
    assertNotNull(result);
    assertEquals(USER_CONFIG_ID, result.getId());
  }

  /**
   * Verifies that {@code findActiveUserEmailConfig} returns {@code null} when no active
   * config exists.
   */
  @Test
  void testFindActiveUserEmailConfigNotFound() {
    when(mockUserCriteria.list()).thenReturn(Collections.emptyList());
    UserEmailConfig result = SmtpCascadeResolver.findActiveUserEmailConfig(mockUser);
    assertNull(result);
  }

  /**
   * Verifies that {@code resolveOrgOrClientLevel} returns an ORGANIZATION-level config
   * when the resolved configuration belongs to a non-root organization.
   */
  @Test
  void testResolveOrgOrClientLevelReturnsOrgLevel() {
    EmailServerConfiguration orgConfig = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_ID, ORG_SMTP_HOST, PORT_465, SECURITY_SSL,
        true, ORG_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        ORG_FROM_ADDRESS, ORG_FROM_NAME, null, TIMEOUT_SECONDS);
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(orgConfig);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveOrgOrClientLevel(mockOrg);
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, result.getLevel());
    assertEquals(ORG_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that {@code resolveOrgOrClientLevel} returns a CLIENT-level config
   * when the resolved configuration belongs to the root organization ({@code '0'}).
   */
  @Test
  void testResolveOrgOrClientLevelReturnsClientLevel() {
    EmailServerConfiguration clientConfig = buildEmailServerConfig(
        CLIENT_CONFIG_ID, ROOT_ORG_ID, CLIENT_SMTP_HOST, PORT_25, SECURITY_NONE,
        false, null, null,
        CLIENT_FROM_ADDRESS, null, null, TIMEOUT_SECONDS);
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(clientConfig);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveOrgOrClientLevel(mockOrg);
    assertNotNull(result);
    assertEquals(ResolvedSmtpConfig.Level.CLIENT, result.getLevel());
    assertEquals(CLIENT_CONFIG_ID, result.getConfigId());
  }

  /**
   * Verifies that {@code resolveOrgOrClientLevel} returns {@code null} when
   * {@link EmailUtils#getEmailConfiguration} returns {@code null}.
   */
  @Test
  void testResolveOrgOrClientLevelReturnsNullWhenNoConfig() {
    mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(mockOrg))
        .thenReturn(null);
    ResolvedSmtpConfig result = SmtpCascadeResolver.resolveOrgOrClientLevel(mockOrg);
    assertNull(result);
  }

  /**
   * Verifies that {@code determineLevel} returns {@code CLIENT} when the configuration's
   * organization ID is the root ({@code '0'}).
   */
  @Test
  void testDetermineLevelReturnsClientForRootOrg() {
    EmailServerConfiguration config = buildEmailServerConfig(
        CLIENT_CONFIG_ID, ROOT_ORG_ID, CLIENT_SMTP_HOST, PORT_25, SECURITY_NONE,
        false, null, null,
        CLIENT_FROM_ADDRESS, null, null, TIMEOUT_SECONDS);
    ResolvedSmtpConfig.Level level = SmtpCascadeResolver.determineLevel(config);
    assertEquals(ResolvedSmtpConfig.Level.CLIENT, level);
  }

  /**
   * Verifies that {@code determineLevel} returns {@code ORGANIZATION} when the
   * configuration's organization ID is not the root.
   */
  @Test
  void testDetermineLevelReturnsOrganizationForNonRootOrg() {
    EmailServerConfiguration config = buildEmailServerConfig(
        ORG_CONFIG_ID, ORG_ID, ORG_SMTP_HOST, PORT_465, SECURITY_SSL,
        true, ORG_FROM_ADDRESS, ENCRYPTED_PASSWORD,
        ORG_FROM_ADDRESS, ORG_FROM_NAME, null, TIMEOUT_SECONDS);
    ResolvedSmtpConfig.Level level = SmtpCascadeResolver.determineLevel(config);
    assertEquals(ResolvedSmtpConfig.Level.ORGANIZATION, level);
  }

  /**
   * Builds a mocked {@link UserEmailConfig} with the given SMTP settings.
   * @param id the configuration record ID
   * @param host the SMTP host
   * @param port the SMTP port
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
   * @param orgId the organization ID ({@code '0'} for client level)
   * @param host the SMTP server host
   * @param port the SMTP port
   * @param security the connection security mode
   * @param auth whether authentication is required
   * @param account the SMTP account username
   * @param password the encrypted SMTP password
   * @param senderAddress the sender email address
   * @param fromName the sender display name
   * @param replyTo the reply-to address
   * @param timeout the connection timeout in seconds
   * @return a mocked {@link EmailServerConfiguration}
   */
  private EmailServerConfiguration buildEmailServerConfig(String id, String orgId,
      String host, Long port, String security, boolean auth, String account,
      String password, String senderAddress, String fromName, String replyTo,
      Long timeout) {
    Organization org = mock(Organization.class);
    when(org.getId()).thenReturn(orgId);
    EmailServerConfiguration config = mock(EmailServerConfiguration.class);
    when(config.getId()).thenReturn(id);
    when(config.getOrganization()).thenReturn(org);
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
