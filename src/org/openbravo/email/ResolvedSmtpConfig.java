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

import org.openbravo.model.common.enterprise.EmailServerConfiguration;

/**
 * Immutable data transfer object representing a fully resolved SMTP configuration.
 * Built from a {@link EmailServerConfiguration} record at any cascade level (User,
 * Organization, or Client), normalizing the field names into a single uniform structure.
 */
public class ResolvedSmtpConfig {

  /**
   * Indicates the cascade level at which the SMTP configuration was resolved.
   */
  public enum Level {
    /** User-level configuration from {@code C_POC_CONFIGURATION} with {@code AD_USER_ID} set. */
    USER,
    /** Organization-level configuration from {@code C_POC_CONFIGURATION}. */
    ORGANIZATION,
    /** Client/system-level (legacy) configuration from {@code C_POC_CONFIGURATION} with org '0'. */
    CLIENT
  }

  /** Default SMTP port for organization/client-level configurations. */
  static final int DEFAULT_POC_SMTP_PORT = 25;

  private final String host;
  private final int port;
  private final String connectionSecurity;
  private final boolean auth;
  private final String account;
  private final String password;
  private final String fromAddress;
  private final String fromName;
  private final String replyTo;
  private final Long timeoutSeconds;
  private final Level level;
  private final String configId;

  /**
   * Constructs a resolved configuration from an email server configuration record.
   * The default port is {@value #DEFAULT_POC_SMTP_PORT} when the configured port is {@code null}.
   * @param config the email server configuration record
   * @param level  the cascade level ({@link Level#USER}, {@link Level#ORGANIZATION}, or
   *   {@link Level#CLIENT})
   */
  ResolvedSmtpConfig(EmailServerConfiguration config, Level level) {
    this.host = config.getSmtpServer();
    this.port = resolvePort(config.getSmtpPort(), DEFAULT_POC_SMTP_PORT);
    this.connectionSecurity = config.getSmtpConnectionSecurity();
    this.auth = config.isSMTPAuthentification();
    this.account = config.getSmtpServerAccount();
    this.password = config.getSmtpServerPassword();
    this.fromAddress = config.getSmtpServerSenderAddress();
    this.fromName = config.getFromName();
    this.replyTo = config.getReplyToAddress();
    this.timeoutSeconds = config.getSmtpConnectionTimeout();
    this.level = level;
    this.configId = config.getId();
  }

  /**
   * Resolves the effective SMTP port, using the provided default when the configured
   * value is {@code null}.
   * @param configuredPort the port from the configuration, may be {@code null}
   * @param defaultPort    the fallback port
   * @return the resolved port number
   */
  private static int resolvePort(Long configuredPort, int defaultPort) {
    return configuredPort != null ? configuredPort.intValue() : defaultPort;
  }

  /**
   * Returns the SMTP server host.
   * @return the SMTP host
   */
  public String getHost() {
    return host;
  }

  /**
   * Returns the SMTP server port.
   * @return the SMTP port
   */
  public int getPort() {
    return port;
  }

  /**
   * Returns the connection security mode (e.g. "STARTTLS", "SSL", or {@code null}).
   * @return the connection security string
   */
  public String getConnectionSecurity() {
    return connectionSecurity;
  }

  /**
   * Returns whether SMTP authentication is required.
   * @return {@code true} if authentication is enabled
   */
  public boolean isAuth() {
    return auth;
  }

  /**
   * Returns the SMTP authentication account (username).
   * @return the SMTP username
   */
  public String getAccount() {
    return account;
  }

  /**
   * Returns the SMTP authentication password (encrypted).
   * @return the encrypted SMTP password
   */
  public String getPassword() {
    return password;
  }

  /**
   * Returns the sender email address used in the "From" header.
   * @return the from address
   */
  public String getFromAddress() {
    return fromAddress;
  }

  /**
   * Returns the display name used in the "From" header, or {@code null} if not set.
   * @return the from display name
   */
  public String getFromName() {
    return fromName;
  }

  /**
   * Returns the "Reply-To" address, or {@code null} if not set.
   * @return the reply-to address
   */
  public String getReplyTo() {
    return replyTo;
  }

  /**
   * Returns the SMTP connection timeout in seconds. Only populated for organization
   * and client-level configurations; {@code null} for user-level.
   * @return the timeout in seconds, or {@code null}
   */
  public Long getTimeoutSeconds() {
    return timeoutSeconds;
  }

  /**
   * Returns the cascade level at which this configuration was resolved.
   * @return the resolution level
   */
  public Level getLevel() {
    return level;
  }

  /**
   * Returns the database record ID of the source configuration, for audit and
   * traceability in logs.
   * @return the configuration record ID
   */
  public String getConfigId() {
    return configId;
  }
}
