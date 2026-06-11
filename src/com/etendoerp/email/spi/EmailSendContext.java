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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.erpCommon.utility.poc.EmailInfo;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Immutable value object carrying everything an {@link EmailSender} could need to deliver a
 * message, decoupled from any specific transport.
 * <p>
 * All fields are nullable: SMTP configuration objects are only meaningful to SMTP senders
 * (an alternative sender resolves its own configuration from {@link #getClient() client} /
 * {@link #getOrganization() organization} and ignores them), client/organization may be
 * unavailable when sending outside a request context (e.g. background threads), and the
 * email itself is {@code null} when the context is used as a pure capability probe.
 * </p>
 */
public class EmailSendContext {

  private static final Logger log = LogManager.getLogger();

  private final Client client;
  private final Organization organization;
  private final EmailServerConfiguration smtpConfig;
  private final ResolvedSmtpConfig resolvedSmtpConfig;
  private final EmailInfo email;

  private EmailSendContext(Client client, Organization organization,
      EmailServerConfiguration smtpConfig, ResolvedSmtpConfig resolvedSmtpConfig,
      EmailInfo email) {
    this.client = client;
    this.organization = organization;
    this.smtpConfig = smtpConfig;
    this.resolvedSmtpConfig = resolvedSmtpConfig;
    this.email = email;
  }

  /**
   * Builds a context taking client and organization from the current {@link OBContext},
   * when available. This is the factory used by the core entry points.
   * @param smtpConfig the SMTP server configuration record, or {@code null} when not resolved
   * @param resolvedSmtpConfig the cascade-resolved SMTP configuration, or {@code null}
   * @param email the resolved message to send, or {@code null} for capability probes
   * @return a new context populated from the current execution context
   */
  public static EmailSendContext create(EmailServerConfiguration smtpConfig,
      ResolvedSmtpConfig resolvedSmtpConfig, EmailInfo email) {
    Client client = null;
    Organization organization = null;
    try {
      OBContext obContext = OBContext.getOBContext();
      if (obContext != null) {
        client = obContext.getCurrentClient();
        organization = obContext.getCurrentOrganization();
      }
    } catch (Exception e) {
      log.debug("Could not read client/organization from OBContext", e);
    }
    return new EmailSendContext(client, organization, smtpConfig, resolvedSmtpConfig, email);
  }

  /**
   * Returns the client in whose context the email is sent, or {@code null} if unavailable.
   * @return the client, may be {@code null}
   */
  public Client getClient() {
    return client;
  }

  /**
   * Returns the organization in whose context the email is sent, or {@code null} if
   * unavailable.
   * @return the organization, may be {@code null}
   */
  public Organization getOrganization() {
    return organization;
  }

  /**
   * Returns the SMTP server configuration record, only meaningful to SMTP senders.
   * @return the SMTP configuration, may be {@code null}
   */
  public EmailServerConfiguration getSmtpConfig() {
    return smtpConfig;
  }

  /**
   * Returns the cascade-resolved SMTP configuration, only meaningful to SMTP senders.
   * @return the resolved SMTP configuration, may be {@code null}
   */
  public ResolvedSmtpConfig getResolvedSmtpConfig() {
    return resolvedSmtpConfig;
  }

  /**
   * Returns the resolved message to send (recipients, subject, rendered body, attachments).
   * @return the email data, or {@code null} when the context is a capability probe
   */
  public EmailInfo getEmail() {
    return email;
  }

  /**
   * Builder used to create an {@link EmailSendContext} with explicit values, mainly for
   * tests and non-standard entry points. Prefer
   * {@link EmailSendContext#create(EmailServerConfiguration, ResolvedSmtpConfig, EmailInfo)}
   * in production code.
   */
  public static class Builder {
    private Client client;
    private Organization organization;
    private EmailServerConfiguration smtpConfig;
    private ResolvedSmtpConfig resolvedSmtpConfig;
    private EmailInfo email;

    public Builder setClient(Client client) {
      this.client = client;
      return this;
    }

    public Builder setOrganization(Organization organization) {
      this.organization = organization;
      return this;
    }

    public Builder setSmtpConfig(EmailServerConfiguration smtpConfig) {
      this.smtpConfig = smtpConfig;
      return this;
    }

    public Builder setResolvedSmtpConfig(ResolvedSmtpConfig resolvedSmtpConfig) {
      this.resolvedSmtpConfig = resolvedSmtpConfig;
      return this;
    }

    public Builder setEmail(EmailInfo email) {
      this.email = email;
      return this;
    }

    public EmailSendContext build() {
      return new EmailSendContext(client, organization, smtpConfig, resolvedSmtpConfig, email);
    }
  }
}
