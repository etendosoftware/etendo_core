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

import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.erpCommon.utility.poc.EmailManager;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.FormatUtilities;

/**
 * Default {@link EmailSender}: delivers the message over SMTP exactly as core has always
 * done. It is the guaranteed fallback of the {@link EmailSenderDispatcher}: it reports
 * itself as always configured and sits at the lowest possible priority, so it is only
 * chosen when no alternative sender applies.
 * <p>
 * The configuration validation, password decryption and timeout resolution previously
 * performed by {@code EmailManager} before sending live here now; the low-level SMTP
 * transport itself remains in {@code EmailManager}. This class extends
 * {@link EmailManager} solely to reach that protected legacy transport without widening
 * the {@code EmailManager} API.
 * </p>
 */
@ApplicationScoped
public class DefaultSmtpEmailSender extends EmailManager implements EmailSender {

  private static final long DEFAULT_SMTP_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);

  /**
   * Always available as the baseline transport. Whether the SMTP configuration is actually
   * complete is validated at {@link #send(EmailSendContext)} time, so that a configuration
   * error is reported to the caller instead of silently skipping this sender.
   * @param context the send context
   * @return always {@code true}
   */
  @Override
  public boolean isConfigured(EmailSendContext context) {
    return true;
  }

  /**
   * Floor priority: any module-provided sender with a higher priority wins when configured.
   * @return {@link Integer#MIN_VALUE}
   */
  @Override
  public int getPriority() {
    return Integer.MIN_VALUE;
  }

  /**
   * Sends the message over SMTP using the configuration carried by the context, preferring
   * the cascade-resolved configuration when present.
   * @param context the send context
   * @throws Exception if the SMTP configuration is missing or incomplete, or the send fails
   */
  @Override
  public void send(EmailSendContext context) throws Exception {
    if (context.getResolvedSmtpConfig() != null) {
      sendWithResolvedConfig(context.getResolvedSmtpConfig(), context.getEmail());
    } else if (context.getSmtpConfig() != null) {
      sendWithServerConfig(context.getSmtpConfig(), context.getEmail());
    } else {
      throw new ServletException("No email configuration available: configure SMTP at User,"
          + " Organization or Client level, or install and configure an alternative email"
          + " sender module.");
    }
  }

  /**
   * Sends using a cascade-resolved SMTP configuration, validating that the minimum SMTP
   * fields are present before attempting the connection.
   * @param conf the resolved SMTP configuration
   * @param email the message to send
   * @throws Exception if validation or the send fails
   */
  private void sendWithResolvedConfig(ResolvedSmtpConfig conf, EmailInfo email)
      throws Exception {
    if (StringUtils.isBlank(conf.getHost())) {
      throw new ServletException("SMTP Host is not configured in the "
          + conf.getLevel() + " email configuration (id=" + conf.getConfigId()
          + "). Please complete the SMTP setup.");
    }
    if (StringUtils.isBlank(conf.getFromAddress())) {
      throw new ServletException("SMTP From Address (sender) is not configured in the "
          + conf.getLevel() + " email configuration (id=" + conf.getConfigId()
          + "). Please complete the SMTP setup.");
    }
    String decryptedPassword = EmailManager.safeDecrypt(conf.getPassword());
    long timeoutMillis = conf.getTimeoutSeconds() != null
        ? TimeUnit.SECONDS.toMillis(conf.getTimeoutSeconds())
        : DEFAULT_SMTP_TIMEOUT_MILLIS;
    EmailManager.sendEmail(conf.getHost(), conf.isAuth(), conf.getAccount(), decryptedPassword,
        conf.getConnectionSecurity(), conf.getPort(), conf.getFromAddress(), conf.getFromName(),
        email.getRecipientTO(), email.getRecipientCC(), email.getRecipientBCC(),
        email.getReplyTo() != null ? email.getReplyTo() : conf.getReplyTo(),
        email.getSubject(), email.getContent(), email.getContentType(), email.getAttachments(),
        email.getSentDate(), email.getHeaderExtras(), (int) timeoutMillis);
  }

  /**
   * Sends using an {@link EmailServerConfiguration} record, decrypting the stored password.
   * @param conf the SMTP server configuration record
   * @param email the message to send
   * @throws Exception if decryption or the send fails
   */
  private void sendWithServerConfig(EmailServerConfiguration conf, EmailInfo email)
      throws Exception {
    String decryptedPassword = FormatUtilities.encryptDecrypt(conf.getSmtpServerPassword(), false);
    long timeoutMillis = conf.getSmtpConnectionTimeout() != null
        ? TimeUnit.SECONDS.toMillis(conf.getSmtpConnectionTimeout())
        : DEFAULT_SMTP_TIMEOUT_MILLIS;
    EmailManager.sendEmail(conf.getSmtpServer(), conf.isSMTPAuthentification(),
        conf.getSmtpServerAccount(), decryptedPassword, conf.getSmtpConnectionSecurity(),
        conf.getSmtpPort().intValue(), conf.getSmtpServerSenderAddress(), conf.getFromName(),
        email.getRecipientTO(), email.getRecipientCC(), email.getRecipientBCC(),
        email.getReplyTo(), email.getSubject(), email.getContent(), email.getContentType(),
        email.getAttachments(), email.getSentDate(), email.getHeaderExtras(),
        (int) timeoutMillis);
  }
}
