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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.weld.WeldUtils;

/**
 * Selects the {@link EmailSender} that handles a given send and delegates to it.
 * <p>
 * Candidates are discovered through CDI, ordered by {@link EmailSender#getPriority()}
 * descending, and the first one reporting {@link EmailSender#isConfigured(EmailSendContext)}
 * wins. The {@link DefaultSmtpEmailSender} sits at the priority floor and is always
 * configured, so SMTP remains the guaranteed fallback: behavior is unchanged when no
 * alternative sender module is installed, and an installed-but-unconfigured module falls
 * through to SMTP silently.
 * </p>
 * <p>
 * The dispatcher only decides <i>which</i> sender runs: a failure thrown by the selected
 * sender's {@code send()} propagates unchanged, with no automatic retry through another
 * transport (avoiding double sends).
 * </p>
 */
@ApplicationScoped
public class EmailSenderDispatcher {

  private static final Logger log = LogManager.getLogger();

  private EmailSenderDispatcher() {
  }

  /**
   * Dispatches the send to the highest-priority configured sender.
   * @param context the send context
   * @throws Exception if the selected sender fails to deliver the message
   */
  public static void dispatch(EmailSendContext context) throws Exception {
    EmailSender sender = selectSender(discoverSenders(), context);
    log.debug("Dispatching email send to {}", sender.getClass().getName());
    sender.send(context);
  }

  /**
   * Determines whether any sender other than the default SMTP one reports itself as
   * configured for the current execution context. Used by callers that today pre-check the
   * SMTP configuration before composing an email, so a client relying exclusively on an
   * alternative sender (no SMTP configured) is not blocked.
   * @return {@code true} if an alternative configured sender is available
   */
  public static boolean hasAlternativeSenderConfigured() {
    EmailSendContext probe = EmailSendContext.create(null, null, null);
    return discoverSenders().stream()
        .filter(sender -> !(sender instanceof DefaultSmtpEmailSender))
        .anyMatch(sender -> isConfiguredSafe(sender, probe));
  }

  /**
   * Discovers the {@link EmailSender} candidates through CDI. When the container is not
   * available (e.g. plain unit tests), returns an empty list so selection falls back to the
   * default SMTP sender.
   * @return the discovered senders, may be empty
   */
  private static List<EmailSender> discoverSenders() {
    try {
      return WeldUtils.getInstances(EmailSender.class);
    } catch (Exception e) {
      log.debug("CDI discovery of EmailSender implementations unavailable,"
          + " using default SMTP sender", e);
      return Collections.emptyList();
    }
  }

  /**
   * Selects the sender to use: candidates ordered by priority descending, first one whose
   * {@code isConfigured} returns {@code true}. Falls back to a {@link DefaultSmtpEmailSender}
   * when no candidate applies, so a sender is always returned.
   * @param candidates the discovered sender candidates
   * @param context the send context
   * @return the sender that must handle the send, never {@code null}
   */
  static EmailSender selectSender(List<EmailSender> candidates, EmailSendContext context) {
    return candidates.stream()
        .sorted(Comparator.comparingInt(EmailSender::getPriority).reversed())
        .filter(sender -> isConfiguredSafe(sender, context))
        .findFirst()
        .orElseGet(DefaultSmtpEmailSender::new);
  }

  /**
   * Evaluates {@link EmailSender#isConfigured(EmailSendContext)} treating any thrown
   * exception as "not configured", so a broken capability check in one module can never
   * break email sending.
   * @param sender the candidate sender
   * @param context the send context
   * @return {@code true} only if the sender reports itself as configured without errors
   */
  private static boolean isConfiguredSafe(EmailSender sender, EmailSendContext context) {
    try {
      return sender.isConfigured(context);
    } catch (Exception e) {
      log.warn("EmailSender {} failed evaluating isConfigured; skipping it",
          sender.getClass().getName(), e);
      return false;
    }
  }
}
