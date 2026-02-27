package org.openbravo.client.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link ComputeWindowActionHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComputeWindowActionHandlerTest {

  private static final String REMOVE_FRAGMENT = "removeFragment";
  private static final String TAB_ID123 = "tabId123";

  private ComputeWindowActionHandler handler;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ComputeWindowActionHandler.class);
  }
  /**
   * Remove fragment returns value without fragment.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentReturnsValueWithoutFragment() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, TAB_ID123);
    assertEquals(TAB_ID123, result);
  }
  /**
   * Remove fragment strips fragment part.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentStripsFragmentPart() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "tabId123#fragment");
    assertEquals(TAB_ID123, result);
  }
  /**
   * Remove fragment returns null for null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentReturnsNullForNull() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, (Object) null);
    assertNull(result);
  }
  /**
   * Remove fragment handles empty string.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentHandlesEmptyString() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "");
    assertEquals("", result);
  }
  /**
   * Remove fragment handles fragment at start.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentHandlesFragmentAtStart() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "#onlyfragment");
    assertEquals("", result);
  }
}
