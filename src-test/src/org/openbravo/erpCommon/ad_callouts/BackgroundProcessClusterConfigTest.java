package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.Utility;

@RunWith(MockitoJUnitRunner.Silent.class)
public class BackgroundProcessClusterConfigTest {

  private static final String TEST_LANGUAGE = "en_US";
  private static final String TEST_MESSAGE = "Please reschedule process requests";

  private BackgroundProcessClusterConfig instance;
  private MockedStatic<Utility> utilityStatic;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(BackgroundProcessClusterConfig.class);
    utilityStatic = mockStatic(Utility.class);
  }

  @After
  public void tearDown() {
    if (utilityStatic != null) {
      utilityStatic.close();
    }
  }

  @Test
  public void testExecuteShowsWarningMessage() throws Exception {
    // Arrange
    SimpleCallout.CalloutInfo info = mock(SimpleCallout.CalloutInfo.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    info.vars = vars;
    when(vars.getLanguage()).thenReturn(TEST_LANGUAGE);
    utilityStatic.when(() -> Utility.messageBD(any(BackgroundProcessClusterConfig.class),
        eq("ShouldRescheduleProcessRequests"), eq(TEST_LANGUAGE)))
        .thenReturn(TEST_MESSAGE);

    // Act
    Method executeMethod = BackgroundProcessClusterConfig.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    executeMethod.setAccessible(true);
    executeMethod.invoke(instance, info);

    // Assert
    verify(info).showWarning(TEST_MESSAGE);
  }

  @Test
  public void testExecuteCallsUtilityMessageBD() throws Exception {
    // Arrange
    SimpleCallout.CalloutInfo info = mock(SimpleCallout.CalloutInfo.class);
    VariablesSecureApp vars = mock(VariablesSecureApp.class);
    info.vars = vars;
    when(vars.getLanguage()).thenReturn("es_ES");
    utilityStatic.when(() -> Utility.messageBD(any(BackgroundProcessClusterConfig.class),
        eq("ShouldRescheduleProcessRequests"), eq("es_ES")))
        .thenReturn("Mensaje de prueba");

    // Act
    Method executeMethod = BackgroundProcessClusterConfig.class.getDeclaredMethod("execute",
        SimpleCallout.CalloutInfo.class);
    executeMethod.setAccessible(true);
    executeMethod.invoke(instance, info);

    // Assert
    utilityStatic.verify(() -> Utility.messageBD(any(BackgroundProcessClusterConfig.class),
        eq("ShouldRescheduleProcessRequests"), eq("es_ES")));
  }
}
