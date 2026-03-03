package org.openbravo.erpCommon.utility.poc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import javax.mail.PasswordAuthentication;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.common.enterprise.EmailServerConfiguration;

/**
 * Tests for {@link EmailManager}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailManagerTest {

  private static final String GET_SMTP_CONNECTION_TIMEOUT = "getSmtpConnectionTimeout";

  @Mock
  private EmailServerConfiguration mockConfig;
  /**
   * Get smtp connection timeout with config value.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSmtpConnectionTimeoutWithConfigValue() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(30L);

    Method method = EmailManager.class.getDeclaredMethod(GET_SMTP_CONNECTION_TIMEOUT,
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(TimeUnit.SECONDS.toMillis(30)), result);
  }
  /**
   * Get smtp connection timeout with null timeout.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSmtpConnectionTimeoutWithNullTimeout() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(null);

    Method method = EmailManager.class.getDeclaredMethod(GET_SMTP_CONNECTION_TIMEOUT,
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(TimeUnit.MINUTES.toMillis(10)), result);
  }
  /**
   * Get smtp connection timeout with null config.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSmtpConnectionTimeoutWithNullConfig() throws Exception {
    Method method = EmailManager.class.getDeclaredMethod(GET_SMTP_CONNECTION_TIMEOUT,
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, (EmailServerConfiguration) null);
    assertEquals(Long.valueOf(TimeUnit.MINUTES.toMillis(10)), result);
  }
  /**
   * Smtp authenticator returns credentials.
   * @throws Exception if an error occurs
   */

  @Test
  public void testSmtpAuthenticatorReturnsCredentials() throws Exception {
    Class<?> authClass = Class.forName("org.openbravo.erpCommon.utility.poc.EmailManager$SMTPAuthenticator");
    java.lang.reflect.Constructor<?> constructor = authClass.getDeclaredConstructor(String.class, String.class);
    constructor.setAccessible(true);

    Object authenticator = constructor.newInstance("testuser", "testpass");

    Method getAuth = authClass.getDeclaredMethod("getPasswordAuthentication");
    getAuth.setAccessible(true);
    PasswordAuthentication auth = (PasswordAuthentication) getAuth.invoke(authenticator);

    assertNotNull(auth);
    assertEquals("testuser", auth.getUserName());
    assertEquals("testpass", auth.getPassword());
  }
  /**
   * Get smtp connection timeout with zero seconds.
   * @throws Exception if an error occurs
   */

  @Test
  public void testGetSmtpConnectionTimeoutWithZeroSeconds() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(0L);

    Method method = EmailManager.class.getDeclaredMethod(GET_SMTP_CONNECTION_TIMEOUT,
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(0L), result);
  }
}
