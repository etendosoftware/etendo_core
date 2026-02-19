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
@RunWith(MockitoJUnitRunner.Silent.class)
public class EmailManagerTest {

  @Mock
  private EmailServerConfiguration mockConfig;

  @Test
  public void testGetSmtpConnectionTimeoutWithConfigValue() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(30L);

    Method method = EmailManager.class.getDeclaredMethod("getSmtpConnectionTimeout",
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(TimeUnit.SECONDS.toMillis(30)), result);
  }

  @Test
  public void testGetSmtpConnectionTimeoutWithNullTimeout() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(null);

    Method method = EmailManager.class.getDeclaredMethod("getSmtpConnectionTimeout",
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(TimeUnit.MINUTES.toMillis(10)), result);
  }

  @Test
  public void testGetSmtpConnectionTimeoutWithNullConfig() throws Exception {
    Method method = EmailManager.class.getDeclaredMethod("getSmtpConnectionTimeout",
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, (EmailServerConfiguration) null);
    assertEquals(Long.valueOf(TimeUnit.MINUTES.toMillis(10)), result);
  }

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

  @Test
  public void testGetSmtpConnectionTimeoutWithZeroSeconds() throws Exception {
    when(mockConfig.getSmtpConnectionTimeout()).thenReturn(0L);

    Method method = EmailManager.class.getDeclaredMethod("getSmtpConnectionTimeout",
        EmailServerConfiguration.class);
    method.setAccessible(true);

    Long result = (Long) method.invoke(null, mockConfig);
    assertEquals(Long.valueOf(0L), result);
  }
}
