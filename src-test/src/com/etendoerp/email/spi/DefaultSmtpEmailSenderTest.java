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
package com.etendoerp.email.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.FormatUtilities;

/**
 * Unit tests for {@link DefaultSmtpEmailSender}: baseline contract (always configured,
 * floor priority), SMTP configuration validation and delegation to the low-level SMTP
 * transport in {@link EmailManager}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultSmtpEmailSenderTest {

  private static final String SMTP_HOST = "smtp.example.com";
  private static final int SMTP_PORT = 587;
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String FROM_NAME = "Sender Name";
  private static final String RECIPIENT_TO = "recipient@example.com";
  private static final String ENCRYPTED_PASSWORD = "encryptedPwd";
  private static final String DECRYPTED_PASSWORD = "decryptedPwd";
  private static final String SMTP_ACCOUNT = "account";
  private static final String SECURITY = "STARTTLS";
  private static final String CONFIG_ID = "CONFIG-001";
  private static final Long TIMEOUT_SECONDS = 600L;
  private static final String SUBJECT = "Subject";
  private static final String CONTENT = "Content";

  private final DefaultSmtpEmailSender sender = new DefaultSmtpEmailSender();

  /**
   * Verifies the baseline contract: the default sender is always configured and sits at
   * the lowest possible priority.
   */
  @Test
  void testBaselineContract() {
    assertTrue(sender.isConfigured(new EmailSendContext.Builder().build()));
    assertEquals(Integer.MIN_VALUE, sender.getPriority());
  }

  /**
   * Verifies that sending with a context carrying no SMTP configuration at all fails with
   * a configuration error instead of attempting any transport.
   */
  @Test
  void testSendWithoutAnyConfigThrowsServletException() {
    EmailSendContext context = new EmailSendContext.Builder()
        .setEmail(new EmailInfo.Builder().setRecipientTO(RECIPIENT_TO).build())
        .build();

    ServletException thrown = assertThrows(ServletException.class, () -> sender.send(context));

    assertTrue(thrown.getMessage().contains("No email configuration available"));
  }

  /**
   * Verifies that a resolved configuration missing the SMTP host is rejected with the same
   * error message previously raised by {@code EmailManager}.
   */
  @Test
  void testSendWithResolvedConfigMissingHostThrowsServletException() {
    ResolvedSmtpConfig conf = mockResolvedConfig("  ", FROM_ADDRESS);
    EmailSendContext context = contextWithResolvedConfig(conf);

    ServletException thrown = assertThrows(ServletException.class, () -> sender.send(context));

    assertTrue(thrown.getMessage().contains("SMTP Host is not configured"));
  }

  /**
   * Verifies that a resolved configuration missing the From Address is rejected with the
   * same error message previously raised by {@code EmailManager}.
   */
  @Test
  void testSendWithResolvedConfigMissingFromAddressThrowsServletException() {
    ResolvedSmtpConfig conf = mockResolvedConfig(SMTP_HOST, null);
    EmailSendContext context = contextWithResolvedConfig(conf);

    ServletException thrown = assertThrows(ServletException.class, () -> sender.send(context));

    assertTrue(thrown.getMessage().contains("SMTP From Address"));
  }

  /**
   * Verifies that sending with a complete resolved configuration decrypts the password and
   * delegates to the low-level SMTP transport with the configured values and the timeout
   * converted from seconds to milliseconds.
   * @throws Exception never expected in this test
   */
  @Test
  void testSendWithResolvedConfigDelegatesToSmtpTransport() throws Exception {
    ResolvedSmtpConfig conf = mockResolvedConfig(SMTP_HOST, FROM_ADDRESS);
    EmailSendContext context = contextWithResolvedConfig(conf);

    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.safeDecrypt(ENCRYPTED_PASSWORD))
          .thenReturn(DECRYPTED_PASSWORD);

      sender.send(context);

      int expectedTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
      mockedManager.verify(() -> EmailManager.sendEmail(
          eq(SMTP_HOST), eq(true), eq(SMTP_ACCOUNT), eq(DECRYPTED_PASSWORD),
          eq(SECURITY), eq(SMTP_PORT), eq(FROM_ADDRESS), eq(FROM_NAME),
          eq(RECIPIENT_TO), isNull(), isNull(), isNull(),
          eq(SUBJECT), eq(CONTENT), isNull(),
          anyList(), isNull(), anyList(),
          eq(expectedTimeoutMillis)));
    }
  }

  /**
   * Verifies that sending with an {@link EmailServerConfiguration} record decrypts the
   * stored password and delegates to the low-level SMTP transport.
   * @throws Exception never expected in this test
   */
  @Test
  void testSendWithServerConfigDelegatesToSmtpTransport() throws Exception {
    EmailServerConfiguration conf = mock(EmailServerConfiguration.class);
    when(conf.getSmtpServer()).thenReturn(SMTP_HOST);
    when(conf.isSMTPAuthentification()).thenReturn(true);
    when(conf.getSmtpServerAccount()).thenReturn(SMTP_ACCOUNT);
    when(conf.getSmtpServerPassword()).thenReturn(ENCRYPTED_PASSWORD);
    when(conf.getSmtpConnectionSecurity()).thenReturn(SECURITY);
    when(conf.getSmtpPort()).thenReturn((long) SMTP_PORT);
    when(conf.getSmtpServerSenderAddress()).thenReturn(FROM_ADDRESS);
    when(conf.getFromName()).thenReturn(FROM_NAME);
    when(conf.getSmtpConnectionTimeout()).thenReturn(TIMEOUT_SECONDS);

    EmailSendContext context = new EmailSendContext.Builder()
        .setSmtpConfig(conf)
        .setEmail(buildEmail())
        .build();

    try (MockedStatic<FormatUtilities> mockedFormat = mockStatic(FormatUtilities.class);
         MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedFormat.when(() -> FormatUtilities.encryptDecrypt(ENCRYPTED_PASSWORD, false))
          .thenReturn(DECRYPTED_PASSWORD);
      mockedManager.when(() -> EmailManager.getSmtpConnectionTimeout(conf))
          .thenReturn(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));

      sender.send(context);

      int expectedTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
      mockedManager.verify(() -> EmailManager.sendEmail(
          eq(SMTP_HOST), eq(true), eq(SMTP_ACCOUNT), eq(DECRYPTED_PASSWORD),
          eq(SECURITY), eq(SMTP_PORT), eq(FROM_ADDRESS), eq(FROM_NAME),
          eq(RECIPIENT_TO), isNull(), isNull(), isNull(),
          eq(SUBJECT), eq(CONTENT), isNull(),
          anyList(), isNull(), anyList(),
          eq(expectedTimeoutMillis)));
    }
  }

  /**
   * Verifies that the resolved configuration path is preferred when the context carries
   * both configuration flavors.
   * @throws Exception never expected in this test
   */
  @Test
  void testSendPrefersResolvedConfigWhenBothPresent() throws Exception {
    ResolvedSmtpConfig resolved = mockResolvedConfig(SMTP_HOST, FROM_ADDRESS);
    EmailServerConfiguration record = mock(EmailServerConfiguration.class);
    EmailSendContext context = new EmailSendContext.Builder()
        .setSmtpConfig(record)
        .setResolvedSmtpConfig(resolved)
        .setEmail(buildEmail())
        .build();

    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.safeDecrypt(any())).thenReturn(DECRYPTED_PASSWORD);

      sender.send(context);

      mockedManager.verify(() -> EmailManager.sendEmail(
          eq(SMTP_HOST), eq(true), eq(SMTP_ACCOUNT), eq(DECRYPTED_PASSWORD),
          eq(SECURITY), eq(SMTP_PORT), eq(FROM_ADDRESS), eq(FROM_NAME),
          eq(RECIPIENT_TO), isNull(), isNull(), isNull(),
          eq(SUBJECT), eq(CONTENT), isNull(),
          anyList(), isNull(), anyList(),
          eq((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS))));
    }
  }

  private ResolvedSmtpConfig mockResolvedConfig(String host, String fromAddress) {
    ResolvedSmtpConfig conf = mock(ResolvedSmtpConfig.class);
    when(conf.getHost()).thenReturn(host);
    when(conf.getPort()).thenReturn(SMTP_PORT);
    when(conf.getConnectionSecurity()).thenReturn(SECURITY);
    when(conf.isAuth()).thenReturn(true);
    when(conf.getAccount()).thenReturn(SMTP_ACCOUNT);
    when(conf.getPassword()).thenReturn(ENCRYPTED_PASSWORD);
    when(conf.getFromAddress()).thenReturn(fromAddress);
    when(conf.getFromName()).thenReturn(FROM_NAME);
    when(conf.getReplyTo()).thenReturn(null);
    when(conf.getTimeoutSeconds()).thenReturn(TIMEOUT_SECONDS);
    when(conf.getLevel()).thenReturn(ResolvedSmtpConfig.Level.CLIENT);
    when(conf.getConfigId()).thenReturn(CONFIG_ID);
    return conf;
  }

  private EmailSendContext contextWithResolvedConfig(ResolvedSmtpConfig conf) {
    return new EmailSendContext.Builder()
        .setResolvedSmtpConfig(conf)
        .setEmail(buildEmail())
        .build();
  }

  private EmailInfo buildEmail() {
    return new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .setSubject(SUBJECT)
        .setContent(CONTENT)
        .build();
  }
}
