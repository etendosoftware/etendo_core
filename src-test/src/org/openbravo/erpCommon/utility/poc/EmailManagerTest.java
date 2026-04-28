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
package org.openbravo.erpCommon.utility.poc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openbravo.dal.core.OBContext;
import org.openbravo.email.EmailUtils;
import org.openbravo.email.ResolvedSmtpConfig;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.utils.FormatUtilities;

/**
 * Unit tests for {@link EmailManager}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class EmailManagerTest {

  private static final long DEFAULT_TIMEOUT_MILLIS = TimeUnit.MINUTES.toMillis(10);
  private static final long TIMEOUT_SECONDS = 600L;

  private static final String ENCRYPTED_PASSWORD = "encryptedPwd";
  private static final String DECRYPTED_PASSWORD = "decryptedPwd";

  private static final String SMTP_HOST = "smtp.example.com";
  private static final int SMTP_PORT = 587;
  private static final String LOCALHOST = "127.0.0.1";
  private static final int UNREACHABLE_PORT = 65534;

  private static final String FROM_ADDRESS = "user@example.com";
  private static final String FROM_NAME = "Test User";
  private static final String RECIPIENT_TO = "recipient@example.com";
  private static final String CONNECTION_SECURITY = "STARTTLS";

  private static final String CONFIG_REPLY_TO = "config-reply@example.com";
  private static final String EMAIL_REPLY_TO = "email-reply@example.com";
  private static final String EMAIL_EXAMPLE = "bcc@example.com";

  private static final String SIMPLE_FROM = "from@example.com";
  private static final String SIMPLE_TO = "to@example.com";
  private static final String AUTH_USER = "user@test.com";

  private static final String TEST_SUBJECT = "Subject";
  private static final String TEST_CONTENT = "Content";
  private static final String TEST_BODY = "Body";
  private static final String INVALID_ADDRESS = "@";
  private static final String BLANK_STRING = "   ";
  private static final String SMTP_ACCOUNT = "user";
  private static final String SECRET = "secret";
  private static final String TEST = "Test";
  private static final String HELLO = "Hello";
  private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
  
  private static final int SHORT_TIMEOUT_MILLIS = 1000;

  private MockedStatic<FormatUtilities> mockedFormatUtils;

  /**
   * Initializes the static mock for {@link FormatUtilities} before each test.
   */
  @BeforeEach
  void setUp() {
    mockedFormatUtils = mockStatic(FormatUtilities.class);
  }

  /**
   * Closes the static mock for {@link FormatUtilities} after each test to prevent leaks.
   */
  @AfterEach
  void tearDown() {
    mockedFormatUtils.close();
  }

  /**
   * Verifies that {@link EmailManager#safeDecrypt(String)} returns {@code null}
   * when the input is {@code null}.
   * @throws ServletException never expected in this test
   */
  @Test
  void testSafeDecryptNullReturnsNull() throws ServletException {
    String result = EmailManager.safeDecrypt(null);
    assertNull(result);
  }

  /**
   * Verifies that {@link EmailManager#safeDecrypt(String)} returns a blank string
   * unchanged, without attempting decryption.
   * @throws ServletException never expected in this test
   */
  @Test
  void testSafeDecryptBlankReturnsBlank() throws ServletException {
    String result = EmailManager.safeDecrypt(BLANK_STRING);
    assertEquals(BLANK_STRING, result);
  }

  /**
   * Verifies that {@link EmailManager#safeDecrypt(String)} returns the decrypted value
   * when {@link FormatUtilities#encryptDecrypt(String, boolean)} succeeds.
   * @throws ServletException never expected in this test
   */
  @Test
  void testSafeDecryptReturnsDecryptedValue() throws ServletException {
    mockedFormatUtils.when(() -> FormatUtilities.encryptDecrypt(ENCRYPTED_PASSWORD, false))
        .thenReturn(DECRYPTED_PASSWORD);
    String result = EmailManager.safeDecrypt(ENCRYPTED_PASSWORD);
    assertEquals(DECRYPTED_PASSWORD, result);
  }

  /**
   * Verifies that {@link EmailManager#safeDecrypt(String)} returns the original value as-is
   * when decryption throws an exception, without propagating the error.
   * @throws ServletException never expected in this test
   */
  @Test
  void testSafeDecryptFallsBackToOriginalOnException() throws ServletException {
    mockedFormatUtils.when(() -> FormatUtilities.encryptDecrypt(ENCRYPTED_PASSWORD, false))
        .thenThrow(new RuntimeException("Decryption error"));
    String result = EmailManager.safeDecrypt(ENCRYPTED_PASSWORD);
    assertEquals(ENCRYPTED_PASSWORD, result);
  }

  /**
   * Verifies that {@link EmailManager#getSmtpConnectionTimeout(EmailServerConfiguration)}
   * returns the default timeout (10 minutes in milliseconds) when the configuration
   * is {@code null}.
   */
  @Test
  void testGetSmtpConnectionTimeoutNullConfigReturnsDefault() {
    Long result = EmailManager.getSmtpConnectionTimeout(null);
    assertEquals(DEFAULT_TIMEOUT_MILLIS, result);
  }

  /**
   * Verifies that {@link EmailManager#getSmtpConnectionTimeout(EmailServerConfiguration)}
   * returns the default timeout when the configuration's timeout value is {@code null}.
   */
  @Test
  void testGetSmtpConnectionTimeoutNullValueReturnsDefault() {
    EmailServerConfiguration conf = mock(EmailServerConfiguration.class);
    when(conf.getSmtpConnectionTimeout()).thenReturn(null);
    Long result = EmailManager.getSmtpConnectionTimeout(conf);
    assertEquals(DEFAULT_TIMEOUT_MILLIS, result);
  }

  /**
   * Verifies that {@link EmailManager#getSmtpConnectionTimeout(EmailServerConfiguration)}
   * correctly converts the configured value from seconds to milliseconds.
   */
  @Test
  void testGetSmtpConnectionTimeoutConvertsSecondsToMillis() {
    EmailServerConfiguration conf = mock(EmailServerConfiguration.class);
    when(conf.getSmtpConnectionTimeout()).thenReturn(TIMEOUT_SECONDS);
    Long result = EmailManager.getSmtpConnectionTimeout(conf);
    assertEquals(TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS), result);
  }

  /**
   * Verifies that {@link EmailManager#sendEmail(ResolvedSmtpConfig, EmailInfo)} decrypts
   * the password and passes all fields from the config and email correctly to the
   * underlying 19-arg overload, using the timeout converted from seconds to milliseconds.
   * @throws Exception if an unexpected error occurs during mock setup or verification
   */
  @Test
  void testSendEmailWithResolvedConfigPassesCorrectArgs() throws Exception {
    ResolvedSmtpConfig conf = buildResolvedConfig(
        SMTP_HOST, SMTP_PORT, CONNECTION_SECURITY, true, SMTP_ACCOUNT, ENCRYPTED_PASSWORD,
        FROM_ADDRESS, FROM_NAME, CONFIG_REPLY_TO, TIMEOUT_SECONDS);
    EmailInfo email = new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .setSubject(TEST)
        .setContent(HELLO)
        .build();
    mockedFormatUtils.when(() -> FormatUtilities.encryptDecrypt(ENCRYPTED_PASSWORD, false))
        .thenReturn(DECRYPTED_PASSWORD);
    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.sendEmail(conf, email)).thenCallRealMethod();
      mockedManager.when(() -> EmailManager.safeDecrypt(any())).thenCallRealMethod();
      EmailManager.sendEmail(conf, email);
      int expectedTimeoutMillis = (int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS);
      mockedManager.verify(() -> EmailManager.sendEmail(
          eq(SMTP_HOST), eq(true), eq(SMTP_ACCOUNT), eq(DECRYPTED_PASSWORD),
          eq(CONNECTION_SECURITY), eq(SMTP_PORT), eq(FROM_ADDRESS), eq(FROM_NAME),
          eq(RECIPIENT_TO), isNull(), isNull(),
          eq(CONFIG_REPLY_TO),
          eq(TEST), eq(HELLO), isNull(),
          anyList(), isNull(), anyList(),
          eq(expectedTimeoutMillis)));
    }
  }

  /**
   * Verifies that {@link EmailManager#sendEmail(ResolvedSmtpConfig, EmailInfo)} uses the
   * default SMTP timeout (10 minutes) when the config has no timeout set ({@code null}).
   * @throws Exception if an unexpected error occurs during mock setup or verification
   */
  @Test
  void testSendEmailUsesDefaultTimeoutWhenConfigTimeoutIsNull() throws Exception {
    ResolvedSmtpConfig conf = buildResolvedConfig(
        SMTP_HOST, SMTP_PORT, CONNECTION_SECURITY, false, null, null,
        FROM_ADDRESS, null, null, null);
    EmailInfo email = new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .build();
    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.sendEmail(conf, email)).thenCallRealMethod();
      mockedManager.when(() -> EmailManager.safeDecrypt(any())).thenCallRealMethod();
      EmailManager.sendEmail(conf, email);
      mockedManager.verify(() -> EmailManager.sendEmail(
          nullable(String.class), anyBoolean(), nullable(String.class), nullable(String.class),
          nullable(String.class), anyInt(), nullable(String.class), nullable(String.class),
          nullable(String.class), nullable(String.class), nullable(String.class),
          nullable(String.class), nullable(String.class), nullable(String.class),
          nullable(String.class), anyList(), any(), anyList(),
          eq((int) DEFAULT_TIMEOUT_MILLIS)));
    }
  }

  /**
   * Verifies that when {@link EmailInfo#getReplyTo()} is non-null, it takes precedence
   * over {@link ResolvedSmtpConfig#getReplyTo()} in the outgoing email.
   * @throws Exception if an unexpected error occurs during mock setup or verification
   */
  @Test
  void testSendEmailEmailReplyToPrecedesConfigReplyTo() throws Exception {
    ResolvedSmtpConfig conf = buildResolvedConfig(
        SMTP_HOST, SMTP_PORT, CONNECTION_SECURITY, false, null, null,
        FROM_ADDRESS, null, CONFIG_REPLY_TO, null);
    EmailInfo email = new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .setReplyTo(EMAIL_REPLY_TO)
        .build();
    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.sendEmail(conf, email)).thenCallRealMethod();
      mockedManager.when(() -> EmailManager.safeDecrypt(any())).thenCallRealMethod();
      EmailManager.sendEmail(conf, email);
      mockedManager.verify(() -> EmailManager.sendEmail(
          nullable(String.class), anyBoolean(), nullable(String.class), nullable(String.class),
          nullable(String.class), anyInt(), nullable(String.class), nullable(String.class),
          nullable(String.class), nullable(String.class), nullable(String.class),
          eq(EMAIL_REPLY_TO),
          nullable(String.class), nullable(String.class), nullable(String.class),
          anyList(), any(), anyList(), anyInt()));
    }
  }

  /**
   * Verifies that when {@link EmailInfo#getReplyTo()} is {@code null}, the fallback
   * reply-to from {@link ResolvedSmtpConfig#getReplyTo()} is used.
   * @throws Exception if an unexpected error occurs during mock setup or verification
   */
  @Test
  void testSendEmailFallsBackToConfigReplyToWhenEmailReplyToIsNull() throws Exception {
    ResolvedSmtpConfig conf = buildResolvedConfig(
        SMTP_HOST, SMTP_PORT, CONNECTION_SECURITY, false, null, null,
        FROM_ADDRESS, null, CONFIG_REPLY_TO, null);
    EmailInfo email = new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .build();
    try (MockedStatic<EmailManager> mockedManager = mockStatic(EmailManager.class)) {
      mockedManager.when(() -> EmailManager.sendEmail(conf, email)).thenCallRealMethod();
      mockedManager.when(() -> EmailManager.safeDecrypt(any())).thenCallRealMethod();
      EmailManager.sendEmail(conf, email);
      mockedManager.verify(() -> EmailManager.sendEmail(
          nullable(String.class), anyBoolean(), nullable(String.class), nullable(String.class),
          nullable(String.class), anyInt(), nullable(String.class), nullable(String.class),
          nullable(String.class), nullable(String.class), nullable(String.class),
          eq(CONFIG_REPLY_TO),
          nullable(String.class), nullable(String.class), nullable(String.class),
          anyList(), any(), anyList(), anyInt()));
    }
  }

  /**
   * Verifies that {@link EmailManager#sendEmail(EmailServerConfiguration, EmailInfo)} delegates
   * correctly to the 19-arg overload and propagates {@link ServletException} on connection
   * failure to an unreachable host.
   */
  @Test
  void testSendEmailWithEmailServerConfigurationThrowsServletException() {
    EmailServerConfiguration conf = mock(EmailServerConfiguration.class);
    when(conf.getSmtpServer()).thenReturn(LOCALHOST);
    when(conf.isSMTPAuthentification()).thenReturn(false);
    when(conf.getSmtpServerAccount()).thenReturn(null);
    when(conf.getSmtpServerPassword()).thenReturn(ENCRYPTED_PASSWORD);
    when(conf.getSmtpConnectionSecurity()).thenReturn(null);
    when(conf.getSmtpPort()).thenReturn((long) UNREACHABLE_PORT);
    when(conf.getSmtpServerSenderAddress()).thenReturn(FROM_ADDRESS);
    when(conf.getFromName()).thenReturn(null);
    when(conf.getSmtpConnectionTimeout()).thenReturn(null);
    mockedFormatUtils.when(() -> FormatUtilities.encryptDecrypt(ENCRYPTED_PASSWORD, false))
        .thenReturn(DECRYPTED_PASSWORD);
    EmailInfo email = new EmailInfo.Builder()
        .setRecipientTO(RECIPIENT_TO)
        .build();
    assertThrows(ServletException.class, () -> EmailManager.sendEmail(conf, email));
  }

  /**
   * Verifies that an invalid sender address causes an
   * {@link javax.mail.internet.AddressException} to be caught and rethrown
   * as {@link ServletException}.
   */
  @Test
  void testSendEmailThrowsServletExceptionOnInvalidSenderAddress() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            null, UNREACHABLE_PORT, INVALID_ADDRESS, null,
            RECIPIENT_TO, null, null, null,
            TEST_SUBJECT, TEST_CONTENT, null,
            Collections.emptyList(), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that a {@link javax.mail.MessagingException} on connection failure
   * (with no security protocol) is caught and rethrown as {@link ServletException}.
   */
  @Test
  void testSendEmailWithNullSecurityThrowsServletException() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            null, UNREACHABLE_PORT, FROM_ADDRESS, null,
            RECIPIENT_TO, null, null, null,
            null, null, null,
            Collections.emptyList(), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that the STARTTLS security branch is executed and that connection failure
   * to an unreachable host results in {@link ServletException}.
   */
  @Test
  void testSendEmailWithStarttlsThrowsServletException() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            CONNECTION_SECURITY, UNREACHABLE_PORT, FROM_ADDRESS, null,
            RECIPIENT_TO, null, null, null,
            TEST_SUBJECT, TEST_CONTENT, null,
            Collections.emptyList(), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that the SSL security branch is executed and that connection failure
   * to an unreachable host results in {@link ServletException}.
   */
  @Test
  void testSendEmailWithSslThrowsServletException() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            "SSL", UNREACHABLE_PORT, FROM_ADDRESS, null,
            RECIPIENT_TO, null, null, null,
            null, null, null,
            Collections.emptyList(), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that the combined STARTTLS,SSL security branch (comma-separated) is parsed
   * correctly and that connection failure to an unreachable host results in
   * {@link ServletException}.
   */
  @Test
  void testSendEmailWithCombinedSecurityThrowsServletException() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            "STARTTLS, SSL", UNREACHABLE_PORT, FROM_ADDRESS, FROM_NAME,
            RECIPIENT_TO, null, null, null,
            null, null, null,
            Collections.emptyList(), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that all optional fields (auth, senderName, CC, BCC, replyTo, subject,
   * sentDate, headerExtras, contentType) are processed before failing on connection
   * to an unreachable host.
   */
  @Test
  void testSendEmailWithAllOptionalFieldsThrowsServletException() {
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, true, AUTH_USER, "pass",
            CONNECTION_SECURITY, UNREACHABLE_PORT, FROM_ADDRESS, FROM_NAME,
            RECIPIENT_TO, "cc@example.com", EMAIL_EXAMPLE,
            "reply@example.com",
            TEST_SUBJECT, "Hello World", "text/html; charset=utf-8",
            Collections.emptyList(), new Date(),
            Arrays.asList("X-Custom-Header", "value1"),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that the attachments multipart branch is entered when a non-empty attachment
   * list is provided, even if the file does not exist on disk. Connection failure to an
   * unreachable host results in {@link ServletException}.
   */
  @Test
  void testSendEmailWithAttachmentThrowsServletException() {
    File nonExistentFile = new File(System.getProperty(JAVA_IO_TMPDIR), "test_nonexistent.txt");
    assertThrows(ServletException.class, () ->
        EmailManager.sendEmail(
            LOCALHOST, false, null, null,
            null, UNREACHABLE_PORT, FROM_ADDRESS, null,
            RECIPIENT_TO, null, null, null,
            TEST_SUBJECT, "Content with attachment", null,
            Arrays.asList(nonExistentFile), null, Collections.emptyList(),
            SHORT_TIMEOUT_MILLIS));
  }

  /**
   * Verifies that {@link EmailManager.SMTPAuthenticator#getPasswordAuthentication()} returns
   * the username and password that were passed to the constructor.
   */
  @Test
  void testSmtpAuthenticatorReturnsCorrectCredentials() {
    EmailManager.SMTPAuthenticator auth =
        new EmailManager.SMTPAuthenticator(AUTH_USER, SECRET);
    PasswordAuthentication pa = auth.getPasswordAuthentication();
    assertNotNull(pa);
    assertEquals(AUTH_USER, pa.getUserName());
    assertEquals(SECRET, pa.getPassword());
  }

  /**
   * Verifies that {@link EmailManager#getAddressesFrom(String[])} correctly parses
   * a valid email address into an {@link InternetAddress} array.
   */
  @Test
  void testGetAddressesFromValidAddress() {
    EmailManager manager = new EmailManager();
    InternetAddress[] result = manager.getAddressesFrom(new String[]{"test@example.com"});
    assertNotNull(result);
    assertEquals(1, result.length);
    assertNotNull(result[0]);
  }

  /**
   * Verifies that {@link EmailManager#getAddressesFrom(String[])} silently ignores
   * invalid email addresses, leaving a {@code null} entry in the result array.
   */
  @Test
  void testGetAddressesFromIgnoresInvalidAddress() {
    EmailManager manager = new EmailManager();
    InternetAddress[] result = manager.getAddressesFrom(new String[]{INVALID_ADDRESS});
    assertNotNull(result);
    assertEquals(1, result.length);
    assertNull(result[0]);
  }

  /**
   * Verifies that the deprecated 17-arg {@code sendEmail} resolves the SMTP timeout via
   * {@link OBContext} and {@link EmailUtils}, then delegates to the 19-arg overload.
   * A connection failure to an unreachable host produces {@link ServletException}.
   * @throws Exception if an unexpected error occurs during mock setup
   */
  @Test
  void testDeprecatedSendEmailResolvesConfigAndThrowsServletException() throws Exception {
    try (MockedStatic<OBContext> mockedOBContext = mockStatic(OBContext.class);
         MockedStatic<EmailUtils> mockedEmailUtils = mockStatic(EmailUtils.class)) {
      OBContext obContextMock = mock(OBContext.class);
      Organization orgMock = mock(Organization.class);
      mockedOBContext.when(OBContext::getOBContext).thenReturn(obContextMock);
      when(obContextMock.getCurrentOrganization()).thenReturn(orgMock);
      mockedEmailUtils.when(() -> EmailUtils.getEmailConfiguration(orgMock)).thenReturn(null);
      assertThrows(ServletException.class, () ->
          EmailManager.sendEmail(
              LOCALHOST, false, null, null,
              null, UNREACHABLE_PORT, FROM_ADDRESS, RECIPIENT_TO, null,
              null, null, TEST_SUBJECT, TEST_CONTENT, null,
              Collections.emptyList(), null, Collections.emptyList()));
    }
  }

  /**
   * Verifies that {@link EmailManager#sendSimpleEmail(Session, String, String, String, String, String, String)}
   * sends a plain email without BCC or attachments when {@link Transport#send} is mocked.
   * @throws Exception if an unexpected error occurs during mock setup
   */
  @Test
  void testSendSimpleEmailWithoutAttachmentsSucceeds() throws Exception {
    Session session = Session.getInstance(new Properties());
    EmailManager manager = new EmailManager();
    try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
      mockedTransport.when(() -> Transport.send(any())).thenAnswer(inv -> null);
      manager.sendSimpleEmail(session, SIMPLE_FROM, SIMPLE_TO,
          null, TEST_SUBJECT, TEST_BODY, null);
    }
  }

  /**
   * Verifies that {@link EmailManager#sendSimpleEmail(Session, String, String, String, String, String, String)}
   * correctly handles BCC recipients when {@link Transport#send} is mocked.
   * @throws Exception if an unexpected error occurs during mock setup
   */
  @Test
  void testSendSimpleEmailWithBccSucceeds() throws Exception {
    Session session = Session.getInstance(new Properties());
    EmailManager manager = new EmailManager();
    try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
      mockedTransport.when(() -> Transport.send(any())).thenAnswer(inv -> null);
      manager.sendSimpleEmail(session, SIMPLE_FROM, SIMPLE_TO,
          EMAIL_EXAMPLE, TEST_SUBJECT, TEST_BODY, null);
    }
  }

  /**
   * Verifies that {@link EmailManager#sendSimpleEmail(Session, String, String, String, String, String, String)}
   * processes attachment file locations correctly when {@link Transport#send} is mocked.
   * @throws Exception if an unexpected error occurs during mock setup
   */
  @Test
  void testSendSimpleEmailWithAttachmentsSucceeds() throws Exception {
    Session session = Session.getInstance(new Properties());
    EmailManager manager = new EmailManager();
    try (MockedStatic<Transport> mockedTransport = mockStatic(Transport.class)) {
      mockedTransport.when(() -> Transport.send(any())).thenAnswer(inv -> null);
      manager.sendSimpleEmail(session, SIMPLE_FROM, SIMPLE_TO,
          null, TEST_SUBJECT, TEST_BODY,
          System.getProperty(JAVA_IO_TMPDIR) + "/attach1.txt");
    }
  }

  /**
   * Verifies that an invalid sender address in
   * {@link EmailManager#sendSimpleEmail(Session, String, String, String, String, String, String)}
   * causes an {@link javax.mail.internet.AddressException} wrapped as {@link PocException}.
   */
  @Test
  void testSendSimpleEmailThrowsPocExceptionOnInvalidFrom() {
    Session session = Session.getInstance(new Properties());
    EmailManager manager = new EmailManager();
    assertThrows(PocException.class, () ->
        manager.sendSimpleEmail(session, INVALID_ADDRESS, SIMPLE_TO,
            null, TEST_SUBJECT, TEST_BODY, null));
  }

  /**
   * Builds a mocked {@link ResolvedSmtpConfig} with the given parameters.
   * @param host the SMTP server hostname
   * @param port the SMTP server port
   * @param security the connection security protocol (e.g., STARTTLS, SSL)
   * @param auth whether SMTP authentication is required
   * @param account the SMTP account username
   * @param password the SMTP account password (encrypted)
   * @param fromAddress the sender email address
   * @param fromName the sender display name
   * @param replyTo the reply-to address
   * @param timeoutSeconds the SMTP connection timeout in seconds, or {@code null} for default
   * @return a mocked {@link ResolvedSmtpConfig} configured with the provided values
   */
  private ResolvedSmtpConfig buildResolvedConfig(String host, int port, String security,
      boolean auth, String account, String password, String fromAddress, String fromName,
      String replyTo, Long timeoutSeconds) {
    ResolvedSmtpConfig conf = mock(ResolvedSmtpConfig.class);
    when(conf.getHost()).thenReturn(host);
    when(conf.getPort()).thenReturn(port);
    when(conf.getConnectionSecurity()).thenReturn(security);
    when(conf.isAuth()).thenReturn(auth);
    when(conf.getAccount()).thenReturn(account);
    when(conf.getPassword()).thenReturn(password);
    when(conf.getFromAddress()).thenReturn(fromAddress);
    when(conf.getFromName()).thenReturn(fromName);
    when(conf.getReplyTo()).thenReturn(replyTo);
    when(conf.getTimeoutSeconds()).thenReturn(timeoutSeconds);
    return conf;
  }
}