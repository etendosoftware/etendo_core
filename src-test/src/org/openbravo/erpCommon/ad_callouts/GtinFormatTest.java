package org.openbravo.erpCommon.ad_callouts;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.erpCommon.utility.OBMessageUtils;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GtinFormatTest {

  private GtinFormat instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private VariablesSecureApp mockVars;

  private MockedStatic<OBMessageUtils> msgStatic;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GtinFormat.class);
    msgStatic = mockStatic(OBMessageUtils.class);

    // Set vars field on mockInfo via reflection
    java.lang.reflect.Field varsField = SimpleCallout.CalloutInfo.class.getDeclaredField("vars");
    varsField.setAccessible(true);
    varsField.set(mockInfo, mockVars);
    when(mockVars.getLanguage()).thenReturn("en_US");
  }

  @After
  public void tearDown() {
    if (msgStatic != null) {
      msgStatic.close();
    }
  }

  @Test
  public void testValidAlphanumericGtinNoError() throws Exception {
    when(mockInfo.getStringParameter("inpgtin", null)).thenReturn("ABC123");

    Method execute = GtinFormat.class.getDeclaredMethod("execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }

  @Test
  public void testInvalidGtinWithSpecialCharsShowsError() throws Exception {
    when(mockInfo.getStringParameter("inpgtin", null)).thenReturn("ABC-123!");
    msgStatic.when(() -> OBMessageUtils.messageBD(any(), eq("InvalidGTINFormat"), eq("en_US")))
        .thenReturn("Invalid GTIN");

    Method execute = GtinFormat.class.getDeclaredMethod("execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo).showError("Invalid GTIN");
  }

  @Test
  public void testNullGtinNoError() throws Exception {
    when(mockInfo.getStringParameter("inpgtin", null)).thenReturn(null);

    Method execute = GtinFormat.class.getDeclaredMethod("execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }

  @Test
  public void testEmptyGtinShowsError() throws Exception {
    when(mockInfo.getStringParameter("inpgtin", null)).thenReturn("");
    msgStatic.when(() -> OBMessageUtils.messageBD(any(), eq("InvalidGTINFormat"), eq("en_US")))
        .thenReturn("Invalid GTIN");

    Method execute = GtinFormat.class.getDeclaredMethod("execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo).showError("Invalid GTIN");
  }

  @Test
  public void testNumericOnlyGtinNoError() throws Exception {
    when(mockInfo.getStringParameter("inpgtin", null)).thenReturn("1234567890");

    Method execute = GtinFormat.class.getDeclaredMethod("execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }
}
