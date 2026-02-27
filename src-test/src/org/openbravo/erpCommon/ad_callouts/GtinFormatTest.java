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
/** Tests for {@link GtinFormat}. */
@SuppressWarnings({"java:S120"})

@RunWith(MockitoJUnitRunner.Silent.class)
public class GtinFormatTest {

  private static final String EN_US = "en_US";
  private static final String INPGTIN = "inpgtin";
  private static final String EXECUTE = "execute";
  private static final String INVALID_GTIN = "Invalid GTIN";

  private GtinFormat instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;

  @Mock
  private VariablesSecureApp mockVars;

  private MockedStatic<OBMessageUtils> msgStatic;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GtinFormat.class);
    msgStatic = mockStatic(OBMessageUtils.class);

    // Set vars field on mockInfo via reflection
    java.lang.reflect.Field varsField = SimpleCallout.CalloutInfo.class.getDeclaredField("vars");
    varsField.setAccessible(true);
    varsField.set(mockInfo, mockVars);
    when(mockVars.getLanguage()).thenReturn(EN_US);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    if (msgStatic != null) {
      msgStatic.close();
    }
  }
  /**
   * Valid alphanumeric gtin no error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testValidAlphanumericGtinNoError() throws Exception {
    when(mockInfo.getStringParameter(INPGTIN, null)).thenReturn("ABC123");

    Method execute = GtinFormat.class.getDeclaredMethod(EXECUTE, SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }
  /**
   * Invalid gtin with special chars shows error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testInvalidGtinWithSpecialCharsShowsError() throws Exception {
    when(mockInfo.getStringParameter(INPGTIN, null)).thenReturn("ABC-123!");
    msgStatic.when(() -> OBMessageUtils.messageBD(any(), eq("InvalidGTINFormat"), eq(EN_US)))
        .thenReturn(INVALID_GTIN);

    Method execute = GtinFormat.class.getDeclaredMethod(EXECUTE, SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo).showError(INVALID_GTIN);
  }
  /**
   * Null gtin no error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNullGtinNoError() throws Exception {
    when(mockInfo.getStringParameter(INPGTIN, null)).thenReturn(null);

    Method execute = GtinFormat.class.getDeclaredMethod(EXECUTE, SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }
  /**
   * Empty gtin shows error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testEmptyGtinShowsError() throws Exception {
    when(mockInfo.getStringParameter(INPGTIN, null)).thenReturn("");
    msgStatic.when(() -> OBMessageUtils.messageBD(any(), eq("InvalidGTINFormat"), eq(EN_US)))
        .thenReturn(INVALID_GTIN);

    Method execute = GtinFormat.class.getDeclaredMethod(EXECUTE, SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo).showError(INVALID_GTIN);
  }
  /**
   * Numeric only gtin no error.
   * @throws Exception if an error occurs
   */

  @Test
  public void testNumericOnlyGtinNoError() throws Exception {
    when(mockInfo.getStringParameter(INPGTIN, null)).thenReturn("1234567890");

    Method execute = GtinFormat.class.getDeclaredMethod(EXECUTE, SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).showError(any());
  }
}
