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

/**
 * Transport-level extension point for email sending. Implementations receive an
 * already-resolved message (recipients, subject, rendered body, attachments) together with
 * the client/organization context, and are responsible for delivering it.
 * <p>
 * Implementations are discovered through CDI ({@code WeldUtils.getInstances}), so a module
 * can take over email sending by providing a bean implementing this interface with a
 * priority above the default. When no alternative sender applies, the
 * {@link DefaultSmtpEmailSender} guarantees the current SMTP behavior.
 * </p>
 *
 * @see EmailSenderDispatcher
 */
public interface EmailSender {

  /**
   * Determines whether this sender is able to handle the message for the given context.
   * <p>
   * Implementations must be cheap and side-effect free: this method is invoked on every
   * send, and may also be invoked with a context that has no {@link EmailSendContext#getEmail()
   * email} as a pure capability probe (e.g. UI checks asking "can this client send email?").
   * Returning {@code false} causes a silent fallback to the next candidate, so an incomplete
   * configuration must never break a send.
   * </p>
   * @param context the send context (the email may be {@code null} for capability probes)
   * @return {@code true} if this sender can handle the message for this client/organization
   */
  boolean isConfigured(EmailSendContext context);

  /**
   * Sends the resolved message. Transport failures must be propagated as exceptions; the
   * dispatcher never retries with another sender, to avoid double sends.
   * @param context the send context carrying the message and client/organization information
   * @throws Exception if the message cannot be delivered
   */
  void send(EmailSendContext context) throws Exception;

  /**
   * Ordering hint used by the dispatcher to pick a sender; higher wins. The default SMTP
   * sender sits at {@link Integer#MIN_VALUE} so any module-provided sender with a higher
   * priority is preferred when configured.
   * @return the priority of this sender
   */
  default int getPriority() {
    return 0;
  }
}
