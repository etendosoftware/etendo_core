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
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.activation.FileDataSource;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.email.EmailUtils;
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
        conf.getSmtpServerSenderAddress(), email.getRecipientTO(), email.getRecipientCC(),
        email.getRecipientBCC(), email.getReplyTo(), email.getSubject(), email.getContent(),
        email.getContentType(), email.getAttachments(), email.getSentDate(),
        email.getHeaderExtras(), timeoutMillis.intValue());
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

    sendEmail(host, auth, username, password, connSecurity, port, senderAddress, recipientTO,
        recipientCC, recipientBCC, replyTo, subject, content, contentType, attachments, sentDate,
        headerExtras, timeoutMillis.intValue());
  }

  private static Long getSmtpConnectionTimeout(EmailServerConfiguration configuration) {
    return (configuration != null && configuration.getSmtpConnectionTimeout() != null)
        ? TimeUnit.SECONDS.toMillis(configuration.getSmtpConnectionTimeout())
        : DEFAULT_SMTP_TIMEOUT;
  }

  private static void sendEmail(String host, boolean auth, String username, String password,
      String connSecurity, int port, String senderAddress, String recipientTO, String recipientCC,
      String recipientBCC, String replyTo, String subject, String content, String contentType,
      List<File> attachments, Date sentDate, List<String> headerExtras, int smtpServerTimeout)
      throws Exception {
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

      message.setFrom(new InternetAddress(senderAddress));

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

  private static class SMTPAuthenticator extends jakarta.mail.Authenticator {
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
   */
  @Deprecated
  private InternetAddress[] getAddressesFrom(String[] textualAddresses) {
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
