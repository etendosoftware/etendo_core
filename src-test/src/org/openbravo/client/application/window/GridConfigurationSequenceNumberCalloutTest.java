package org.openbravo.client.application.window;

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
import org.openbravo.erpCommon.ad_callouts.SimpleCallout;
/** Tests for {@link GridConfigurationSequenceNumberCallout}. */

@RunWith(MockitoJUnitRunner.Silent.class)
public class GridConfigurationSequenceNumberCalloutTest {

  private GridConfigurationSequenceNumberCallout instance;

  @Mock
  private SimpleCallout.CalloutInfo mockInfo;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(GridConfigurationSequenceNumberCallout.class);
  }
  /** Tears down test fixtures. */

  @After
  public void tearDown() {
    // No implementation needed
  }
  /**
   * Execute with invalid sequence number returns early.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithInvalidSequenceNumberReturnsEarly() throws Exception {
    when(mockInfo.getStringParameter("inpseqno", null)).thenReturn("notanumber");

    Method execute = GridConfigurationSequenceNumberCallout.class.getDeclaredMethod(
        "execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    // Should return early without calling getTabId or addResult
    verify(mockInfo, never()).addResult(any(), any());
  }
  /**
   * Execute with null sequence number returns early.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteWithNullSequenceNumberReturnsEarly() throws Exception {
    when(mockInfo.getStringParameter("inpseqno", null)).thenReturn(null);

    Method execute = GridConfigurationSequenceNumberCallout.class.getDeclaredMethod(
        "execute", SimpleCallout.CalloutInfo.class);
    execute.setAccessible(true);
    execute.invoke(instance, mockInfo);

    verify(mockInfo, never()).addResult(any(), any());
  }
  /**
   * Create warning message with count greater than zero.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateWarningMessageWithCountGreaterThanZero() throws Exception {
    // Test the private createWarningMessage method directly
    MockedStatic<org.openbravo.erpCommon.utility.Utility> utilStatic = mockStatic(
        org.openbravo.erpCommon.utility.Utility.class);
    MockedStatic<org.openbravo.dal.core.OBContext> ctxStatic = mockStatic(
        org.openbravo.dal.core.OBContext.class);

    try {
      org.openbravo.dal.core.OBContext mockCtx = mock(org.openbravo.dal.core.OBContext.class);
      org.openbravo.model.ad.system.Language mockLang = mock(org.openbravo.model.ad.system.Language.class);
      ctxStatic.when(org.openbravo.dal.core.OBContext::getOBContext).thenReturn(mockCtx);
      when(mockCtx.getLanguage()).thenReturn(mockLang);
      when(mockLang.getId()).thenReturn("en_US");

      utilStatic.when(() -> org.openbravo.erpCommon.utility.Utility.messageBD(
          any(), eq("SameSeqNoForGridConfiguration"), eq("en_US")))
          .thenReturn("Warning: duplicate seq");

      Method createWarning = GridConfigurationSequenceNumberCallout.class.getDeclaredMethod(
          "createWarningMessage", SimpleCallout.CalloutInfo.class, int.class);
      createWarning.setAccessible(true);
      createWarning.invoke(instance, mockInfo, 2);

      verify(mockInfo).addResult("WARNING", "Warning: duplicate seq");
    } finally {
      utilStatic.close();
      ctxStatic.close();
    }
  }
  /**
   * Create warning message with zero count does nothing.
   * @throws Exception if an error occurs
   */

  @Test
  public void testCreateWarningMessageWithZeroCountDoesNothing() throws Exception {
    Method createWarning = GridConfigurationSequenceNumberCallout.class.getDeclaredMethod(
        "createWarningMessage", SimpleCallout.CalloutInfo.class, int.class);
    createWarning.setAccessible(true);
    createWarning.invoke(instance, mockInfo, 0);

    verify(mockInfo, never()).addResult(any(), any());
  }
}
