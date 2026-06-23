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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.ServletException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.poc.EmailInfo;

/**
 * Unit tests for {@link DefaultSmtpEmailSender}: baseline contract (always configured,
 * floor priority) and SMTP configuration validation. The delegation to the low-level SMTP
 * transport is covered in {@code EmailManagerTest}, which lives in the same package as the
 * protected legacy transport.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class DefaultSmtpEmailSenderTest {

  private static final String SMTP_HOST = "smtp.example.com";
  private static final int SMTP_PORT = 587;
  private static final String FROM_ADDRESS = "noreply@example.com";
  private static final String RECIPIENT_TO = "recipient@example.com";
  private static final String CONFIG_ID = "CONFIG-001";

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

  private ResolvedSmtpConfig mockResolvedConfig(String host, String fromAddress) {
    ResolvedSmtpConfig conf = mock(ResolvedSmtpConfig.class);
    when(conf.getHost()).thenReturn(host);
    when(conf.getPort()).thenReturn(SMTP_PORT);
    when(conf.getFromAddress()).thenReturn(fromAddress);
    when(conf.getLevel()).thenReturn(ResolvedSmtpConfig.Level.CLIENT);
    when(conf.getConfigId()).thenReturn(CONFIG_ID);
    return conf;
  }

  private EmailSendContext contextWithResolvedConfig(ResolvedSmtpConfig conf) {
    return new EmailSendContext.Builder()
        .setResolvedSmtpConfig(conf)
        .setEmail(new EmailInfo.Builder().setRecipientTO(RECIPIENT_TO).build())
        .build();
  }
}
