/*
 * ************************************************************************ The
 * contents of this file are subject to the Openbravo Public License Version 1.1
 * (the "License"), being the Mozilla Public License Version 1.1 with a
 * permitted attribution clause; you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * http://www.openbravo.com/legal/license.html Software distributed under the
 * License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing rights and limitations under the License. The Original Code is
 * Openbravo ERP. The Initial Developer of the Original Code is Openbravo SLU All
 * portions are Copyright (C) 2001-2019 Openbravo SLU All Rights Reserved.
 * Contributor(s): ______________________________________.
 * ***********************************************************************
 */
package org.openbravo.erpCommon.utility.poc;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.email.EmailUtils;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.utils.FormatUtilities;

public class EmailManager {
  private static Logger log4j = LogManager.getLogger();
  private static final Long DEFAULT_SMTP_TIMEOUT = TimeUnit.MINUTES.toMillis(10);

  /**
   * Sends an email using the given SMTP Server configuration and the email definition contained in
   * the email parameter.
   * 
   * @param conf
   *          The SMTP Server configuration
   * @param email
   *          The data of the email being sent
   * @throws Exception
   */
  public static void sendEmail(EmailServerConfiguration conf, EmailInfo email) throws Exception {

    String decryptedPassword = FormatUtilities.encryptDecrypt(conf.getSmtpServerPassword(), false);
    Long timeoutMillis = getSmtpConnectionTimeout(conf);

    sendEmail(conf.getSmtpServer(), conf.isSMTPAuthentification(), conf.getSmtpServerAccount(),
        decryptedPassword, conf.getSmtpConnectionSecurity(), conf.getSmtpPort().intValue(),
        conf.getSmtpServerSenderAddress(), conf.getFromName(), email.getRecipientTO(),
        email.getRecipientCC(), email.getRecipientBCC(), email.getReplyTo(), email.getSubject(),
        email.getContent(), email.getContentType(), email.getAttachments(), email.getSentDate(),
        email.getHeaderExtras(), timeoutMillis.intValue());
  }

  /**
   * Sends an email using a {@link ResolvedSmtpConfig} obtained from the cascade resolver.
   * This is the preferred method when the cascade resolution (User → Organization → Client)
   * has already been performed by {@link org.openbravo.email.SmtpCascadeResolver}.
   * @param conf The resolved SMTP configuration
   * @param email The data of the email being sent
   * @throws Exception if password decryption or email sending fails
   */
  public static void sendEmail(ResolvedSmtpConfig conf, EmailInfo email) throws Exception {
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
    String decryptedPassword = safeDecrypt(conf.getPassword());
    long timeoutMillis = conf.getTimeoutSeconds() != null
        ? TimeUnit.SECONDS.toMillis(conf.getTimeoutSeconds())
        : DEFAULT_SMTP_TIMEOUT;
    sendEmail(conf.getHost(), conf.isAuth(), conf.getAccount(), decryptedPassword,
        conf.getConnectionSecurity(), conf.getPort(), conf.getFromAddress(), conf.getFromName(),
        email.getRecipientTO(), email.getRecipientCC(), email.getRecipientBCC(),
        email.getReplyTo() != null ? email.getReplyTo() : conf.getReplyTo(),
        email.getSubject(), email.getContent(), email.getContentType(), email.getAttachments(),
        email.getSentDate(), email.getHeaderExtras(), (int) timeoutMillis);
  }

  /**
   * Attempts to decrypt the given password using {@link FormatUtilities#encryptDecrypt}.
   * @param password the password string to decrypt, may be blank or {@code null}
   * @return the decrypted password, or the original value if decryption is not applicable or fails
   * @throws ServletException if an unexpected servlet-level error occurs
   */
  protected static String safeDecrypt(String password) throws ServletException {
    if (StringUtils.isBlank(password)) {
      return password;
    }
    try {
      return FormatUtilities.encryptDecrypt(password, false);
    } catch (Exception e) {
      log4j.warn("Password decryption failed (stored as plain text?), using value as-is.", e);
      return password;
    }
  }

  /**
   * @deprecated Use {@link #sendEmail(EmailServerConfiguration, EmailInfo)} instead.
   */
  @Deprecated
  public static void sendEmail(String host, boolean auth, String username, String password,
      String connSecurity, int port, String senderAddress, String recipientTO, String recipientCC,
      String recipientBCC, String replyTo, String subject, String content, String contentType,
      List<File> attachments, Date sentDate, List<String> headerExtras) throws Exception {

    EmailServerConfiguration configuration = EmailUtils
        .getEmailConfiguration(OBContext.getOBContext().getCurrentOrganization());
    Long timeoutMillis = getSmtpConnectionTimeout(configuration);

    sendEmail(host, auth, username, password, connSecurity, port, senderAddress, null, recipientTO,
        recipientCC, recipientBCC, replyTo, subject, content, contentType, attachments, sentDate,
        headerExtras, timeoutMillis.intValue());
  }

  protected static Long getSmtpConnectionTimeout(EmailServerConfiguration configuration) {
    return (configuration != null && configuration.getSmtpConnectionTimeout() != null)
        ? TimeUnit.SECONDS.toMillis(configuration.getSmtpConnectionTimeout())
        : DEFAULT_SMTP_TIMEOUT;
  }

  protected static void sendEmail(String host, boolean auth, String username, String password,
      String connSecurity, int port, String senderAddress, String senderName, String recipientTO,
      String recipientCC, String recipientBCC, String replyTo, String subject, String content,
      String contentType, List<File> attachments, Date sentDate, List<String> headerExtras,
      int smtpServerTimeout) throws Exception {
    String localReplyTo = replyTo;
    String localRecipientTO = recipientTO;
    String localRecipientCC = recipientCC;
    String localRecipientBCC = recipientBCC;
    String localConnSecurity = connSecurity;
    String localContentType = contentType;
    try {
      Properties props = new Properties();

      if (log4j.isDebugEnabled()) {
        props.put("mail.debug", "true");
      }
      props.put("mail.transport.protocol", "smtp");
      props.put("mail.smtp.host", host);
      props.put("mail.smtp.port", String.valueOf(port));

      String timeout = String.valueOf(smtpServerTimeout);
      props.put("mail.smtp.timeout", timeout);
      props.put("mail.smtp.connectiontimeout", timeout);
      props.put("mail.smtp.writetimeout", timeout);

      if (localConnSecurity != null) {
        localConnSecurity = localConnSecurity.replaceAll(", *", ",");
        String[] connSecurityArray = localConnSecurity.split(",");
        for (int i = 0; i < connSecurityArray.length; i++) {
          if ("STARTTLS".equals(connSecurityArray[i])) {
            props.put("mail.smtp.starttls.enable", "true");
          }
          if ("SSL".equals(connSecurityArray[i])) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.fallback", "false");
            props.put("mail.smtp.socketFactory.port", port);
          }
        }
      }

      Session mailSession = null;
      if (auth) {
        props.put("mail.smtp.auth", "true");
        Authenticator authentification = new SMTPAuthenticator(username, password);
        mailSession = Session.getInstance(props, authentification);
      } else {
        mailSession = Session.getInstance(props, null);
      }

      Transport transport = mailSession.getTransport();

      MimeMessage message = new MimeMessage(mailSession);

      if (StringUtils.isNotEmpty(senderName)) {
        message.setFrom(new InternetAddress(senderAddress, senderName, "UTF-8"));
      } else {
        message.setFrom(new InternetAddress(senderAddress));
      }

      if (localRecipientTO != null) {
        localRecipientTO = localRecipientTO.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(localRecipientTO));
      }
      if (localRecipientCC != null) {
        localRecipientCC = localRecipientCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(localRecipientCC));
      }
      if (localRecipientBCC != null) {
        localRecipientBCC = localRecipientBCC.replaceAll(";", ",");
        message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(localRecipientBCC));
      }

      if (localReplyTo != null) {
        localReplyTo = localReplyTo.replaceAll(";", ",");
        localReplyTo = localReplyTo.replaceAll(", *", ",");
        String[] replyToArray = localReplyTo.split(",");

        Address[] replyToAddresses = new InternetAddress[replyToArray.length];
        for (int i = 0; i < replyToArray.length; i++) {
          replyToAddresses[i] = new InternetAddress(replyToArray[i]);
        }

        message.setReplyTo(replyToAddresses);
      }

      if (subject != null) {
        message.setSubject(subject);
      }
      if (sentDate != null) {
        message.setSentDate(sentDate);
      }

      if (headerExtras != null && headerExtras.size() > 0) {
        String[] headerExtrasArray = headerExtras.toArray(new String[headerExtras.size()]);
        for (int i = 0; i < headerExtrasArray.length - 1; i++) {
          message.addHeader(headerExtrasArray[i], headerExtrasArray[i + 1]);
          i++;
        }
      }

      if (attachments != null && attachments.size() > 0) {
        Multipart multipart = new MimeMultipart();

        if (content != null) {
          MimeBodyPart messagePart = new MimeBodyPart();
          if (localContentType == null) {
            localContentType = "text/plain; charset=utf-8";
          }
          messagePart.setContent(content, localContentType);
          multipart.addBodyPart(messagePart);
        }

        MimeBodyPart attachmentPart = null;
        for (File attachmentFile : attachments) {
          attachmentPart = new MimeBodyPart();
          if (attachmentFile.exists() && attachmentFile.canRead()) {
            attachmentPart.attachFile(attachmentFile);
            multipart.addBodyPart(attachmentPart);
          }
        }

        message.setContent(multipart);
      } else {
        if (content != null) {
          if (localContentType == null) {
            localContentType = "text/plain; charset=utf-8";
          }
          message.setContent(content, localContentType);
        }
      }

      transport.connect();
      transport.sendMessage(message, message.getAllRecipients());
      transport.close();
    } catch (final AddressException exception) {
      log4j.error(exception);
      throw new ServletException(exception);
    } catch (final MessagingException exception) {
      log4j.error(exception);
      throw new ServletException(exception.getMessage(), exception);
    }
  }

  protected static class SMTPAuthenticator extends javax.mail.Authenticator {
    private String _username;
    private String _password;

    public SMTPAuthenticator(String username, String password) {
      _username = username;
      _password = password;
    }

    @Override
    public PasswordAuthentication getPasswordAuthentication() {
      return new PasswordAuthentication(_username, _password);
    }
  }

  /**
   * Since Openbravo 3.0MP9 only
   * {@link #sendEmail(String, boolean, String, String, String, int, String, String, String, String, String, String, String, String, List, Date, List)}
   * is used for the full email sending cycle
   */
  @Deprecated
  public static Session newMailSession(ConnectionProvider connectionProvider, String clientId,
      String adOrgId) throws PocException, ServletException {
    PocConfigurationData configurations[];
    try {
      configurations = PocConfigurationData.getSmtpDetails(connectionProvider, clientId, adOrgId);
    } catch (ServletException exception) {
      throw new PocException(exception);
    }

    PocConfigurationData configuration = null;
    if (configurations.length > 0) {
      configuration = configurations[0];
      if (log4j.isDebugEnabled()) {
        log4j.debug("Crm configuration, smtp server: " + configuration.smtpserver);
        log4j.debug("Crm configuration, smtp server auth: " + configuration.issmtpauthorization);
        log4j.debug("Crm configuration, smtp server account: " + configuration.smtpserveraccount);
        log4j.debug("Crm configuration, smtp server password: " + configuration.smtpserverpassword);
        log4j.debug("Crm configuration, smtp server connection security: "
            + configuration.smtpconnectionsecurity);
        log4j.debug("Crm configuration, smtp server port: " + configuration.smtpport);
      }
    } else {
      throw new ServletException("No Poc configuration found for this client.");
    }

    Properties props = new Properties();

    if (log4j.isDebugEnabled()) {
      props.put("mail.debug", "true");
    }
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.host", configuration.smtpserver);
    props.put("mail.smtp.auth", (configuration.issmtpauthorization.equals("Y") ? "true" : "false"));
    props.put("mail.smtp.mail.sender", "email_admin@openbravo.com");
    props.put("mail.smtp.port", configuration.smtpport);
    if (configuration.smtpconnectionsecurity.equals("STARTTLS")) {
      props.put("mail.smtp.starttls.enable", "true");
    } else if (configuration.smtpconnectionsecurity.equals("SSL")) {
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.socketFactory.fallback", "false");
      props.put("mail.smtp.socketFactory.port", configuration.smtpport);
    }

    ClientAuthenticator authenticator = null;
    if (configuration.smtpserveraccount != null) {
      authenticator = new ClientAuthenticator(configuration.smtpserveraccount,
          FormatUtilities.encryptDecrypt(configuration.smtpserverpassword, false));
    }

    return Session.getInstance(props, authenticator);
  }

  /**
   * Since Openbravo 3.0MP9 only
   * {@link #sendEmail(String, boolean, String, String, String, int, String, String, String, String, String, String, String, String, List, Date, List)}
   * is used for the full email sending cycle
   */
  @Deprecated
  public void sendSimpleEmail(Session session, String from, String to, String bcc, String subject,
      String body, String attachmentFileLocations) throws PocException {
    try {
      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress(from));

      message.setRecipients(Message.RecipientType.TO, getAddressesFrom(to.split(",")));

      if (bcc != null) {
        message.setRecipients(Message.RecipientType.BCC, getAddressesFrom(bcc.split(",")));
      }

      message.setSubject(subject);

      // Content consists of 2 parts, the message body and the attachment
      // We therefore use a multipart message
      Multipart multipart = new MimeMultipart();

      // Create the message part
      MimeBodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setText(body);
      multipart.addBodyPart(messageBodyPart);

      // Create the attachment parts
      if (attachmentFileLocations != null) {
        String attachments[] = attachmentFileLocations.split(",");

        for (String attachment : attachments) {
          messageBodyPart = new MimeBodyPart();
          DataSource source = new FileDataSource(attachment);
          messageBodyPart.setDataHandler(new DataHandler(source));
          messageBodyPart.setFileName(attachment.substring(attachment.lastIndexOf("/") + 1));
          multipart.addBodyPart(messageBodyPart);
        }
      }

      message.setContent(multipart);

      // Send the email
      Transport.send(message);
    } catch (AddressException exception) {
      throw new PocException(exception);
    } catch (MessagingException exception) {
      throw new PocException(exception);
    }
  }

  /**
   * Since Openbravo 3.0MP9 only
   * {@link #sendEmail(String, boolean, String, String, String, int, String, String, String, String, String, String, String, String, List, Date, List)}
   * is used for the full email sending cycle
   * @deprecated Use {@link #sendEmail(EmailServerConfiguration, EmailInfo)} instead.
   */
  @Deprecated
  protected InternetAddress[] getAddressesFrom(String[] textualAddresses) {
    InternetAddress internetAddresses[] = new InternetAddress[textualAddresses.length];
    for (int index = 0; index < textualAddresses.length; index++) {
      try {
        internetAddresses[index] = new InternetAddress(textualAddresses[index]);
      } catch (AddressException e) {
        if (log4j.isDebugEnabled()) {
          log4j.debug("Could not create a valid email for: " + textualAddresses[index]
              + ". Address ignored");
        }
      }
    }
    return internetAddresses;
  }

}
