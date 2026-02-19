package org.openbravo.erpCommon.ad_callouts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.data.FieldProvider;

@RunWith(MockitoJUnitRunner.class)
public class CalloutHelperTest {

  private CalloutHelper instance;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(TestableCalloutHelper.class);
  }

  @Test
  public void testGenerateArrayWithNullData() throws Exception {
    Method method = CalloutHelper.class.getDeclaredMethod("generateArray", FieldProvider[].class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, (Object) null);
    assertEquals("null", result);
  }

  @Test
  public void testGenerateArrayWithEmptyData() throws Exception {
    Method method = CalloutHelper.class.getDeclaredMethod("generateArray", FieldProvider[].class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, (Object) new FieldProvider[0]);
    assertEquals("null", result);
  }

  @Test
  public void testGenerateArrayWithSingleElement() throws Exception {
    FieldProvider fp = mock(FieldProvider.class);
    when(fp.getField("id")).thenReturn("100");
    when(fp.getField("name")).thenReturn("TestName");

    Method method = CalloutHelper.class.getDeclaredMethod("generateArray", FieldProvider[].class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, (Object) new FieldProvider[] { fp });

    assertTrue(result.startsWith("new Array("));
    assertTrue(result.contains("\"100\""));
    assertTrue(result.contains("TestName"));
    assertTrue(result.contains("\"false\""));
  }

  @Test
  public void testGenerateArrayWithSelectedElement() throws Exception {
    FieldProvider fp = mock(FieldProvider.class);
    when(fp.getField("id")).thenReturn("100");
    when(fp.getField("name")).thenReturn("TestName");

    Method method = CalloutHelper.class.getDeclaredMethod("generateArray",
        FieldProvider[].class, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, new FieldProvider[] { fp }, "100");

    assertTrue(result.contains("\"true\""));
  }

  @Test
  public void testGenerateArrayWithNonSelectedElement() throws Exception {
    FieldProvider fp = mock(FieldProvider.class);
    when(fp.getField("id")).thenReturn("100");
    when(fp.getField("name")).thenReturn("TestName");

    Method method = CalloutHelper.class.getDeclaredMethod("generateArray",
        FieldProvider[].class, String.class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, new FieldProvider[] { fp }, "999");

    assertTrue(result.contains("\"false\""));
  }

  @Test
  public void testGenerateArrayWithMultipleElements() throws Exception {
    FieldProvider fp1 = mock(FieldProvider.class);
    when(fp1.getField("id")).thenReturn("1");
    when(fp1.getField("name")).thenReturn("First");

    FieldProvider fp2 = mock(FieldProvider.class);
    when(fp2.getField("id")).thenReturn("2");
    when(fp2.getField("name")).thenReturn("Second");

    Method method = CalloutHelper.class.getDeclaredMethod("generateArray", FieldProvider[].class);
    method.setAccessible(true);
    String result = (String) method.invoke(instance, (Object) new FieldProvider[] { fp1, fp2 });

    assertTrue(result.contains("\"1\""));
    assertTrue(result.contains("\"2\""));
    assertTrue(result.contains("First"));
    assertTrue(result.contains("Second"));
  }

  @Test
  public void testCommandInCommandListMatchFound() {
    boolean result = CalloutHelper.commandInCommandList("SAVE", "SAVE", "DELETE", "UPDATE");
    assertTrue(result);
  }

  @Test
  public void testCommandInCommandListNoMatch() {
    boolean result = CalloutHelper.commandInCommandList("INSERT", "SAVE", "DELETE", "UPDATE");
    assertFalse(result);
  }

  @Test
  public void testCommandInCommandListEmptyList() {
    boolean result = CalloutHelper.commandInCommandList("SAVE");
    assertFalse(result);
  }

  @Test
  public void testCommandInCommandListExactMatch() {
    boolean result = CalloutHelper.commandInCommandList("DEFAULT", "DEFAULT");
    assertTrue(result);
  }

  /**
   * Concrete subclass to allow instantiation of abstract CalloutHelper for testing.
   */
  private static class TestableCalloutHelper extends CalloutHelper {
    @Override
    void printPage(javax.servlet.http.HttpServletResponse response,
        org.openbravo.base.secureApp.VariablesSecureApp vars,
        String strTabId, String windowId) {
      // no-op for testing
    }
  }
}
