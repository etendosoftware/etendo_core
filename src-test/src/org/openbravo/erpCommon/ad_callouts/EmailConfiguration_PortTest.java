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

@RunWith(MockitoJUnitRunner.class)
public class EmailConfiguration_PortTest {

  private EmailConfiguration_Port instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(EmailConfiguration_Port.class);
  }

  @Test
  public void testExecuteWithNoSecuritySuggestsPort25() throws Exception {
    when(mockInfo.getStringParameter("inpsmtpconnectionsecurity", null)).thenReturn("N");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult("inpsmtpport", "25");
  }

  @Test
  public void testExecuteWithStartTlsSuggestsPort587() throws Exception {
    when(mockInfo.getStringParameter("inpsmtpconnectionsecurity", null)).thenReturn("STARTTLS");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult("inpsmtpport", "587");
  }

  @Test
  public void testExecuteWithSslSuggestsPort465() throws Exception {
    when(mockInfo.getStringParameter("inpsmtpconnectionsecurity", null)).thenReturn("SSL");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult("inpsmtpport", "465");
  }

  @Test
  public void testExecuteWithUnknownSecuritySuggestsEmptyPort() throws Exception {
    when(mockInfo.getStringParameter("inpsmtpconnectionsecurity", null)).thenReturn("UNKNOWN");

    Method method = EmailConfiguration_Port.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    method.setAccessible(true);
    method.invoke(instance, mockInfo);

    verify(mockInfo).addResult("inpsmtpport", "");
  }
}
