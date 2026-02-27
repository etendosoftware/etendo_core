package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
/** Tests for {@link EmailConfiguration_Port}. */
@SuppressWarnings({"java:S101", "java:S120"})

@RunWith(MockitoJUnitRunner.class)
public class EmailConfiguration_PortTest {

  private static final String INPSMTPCONNECTIONSECURITY = "inpsmtpconnectionsecurity";
  private static final String EXECUTE = "execute";
  private static final String INPSMTPPORT = "inpsmtpport";

  private EmailConfiguration_Port instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(EmailConfiguration_Port.class);
  }
  /**
   * Execute with no security suggests port25.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNoSecuritySuggestsPort25() throws Exception {
    when(mockInfo.getStringParameter(INPSMTPCONNECTIONSECURITY, null)).thenReturn("N");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod(EXECUTE,
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult(INPSMTPPORT, "25");
  }
  /**
   * Execute with start tls suggests port587.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithStartTlsSuggestsPort587() throws Exception {
    when(mockInfo.getStringParameter(INPSMTPCONNECTIONSECURITY, null)).thenReturn("STARTTLS");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod(EXECUTE,
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult(INPSMTPPORT, "587");
  }
  /**
   * Execute with ssl suggests port465.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithSslSuggestsPort465() throws Exception {
    when(mockInfo.getStringParameter(INPSMTPCONNECTIONSECURITY, null)).thenReturn("SSL");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod(EXECUTE,
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult(INPSMTPPORT, "465");
  }
  /**
   * Execute with unknown security suggests empty port.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithUnknownSecuritySuggestsEmptyPort() throws Exception {
    when(mockInfo.getStringParameter(INPSMTPCONNECTIONSECURITY, null)).thenReturn("UNKNOWN");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod(EXECUTE,
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult(INPSMTPPORT, "");
  }
}
