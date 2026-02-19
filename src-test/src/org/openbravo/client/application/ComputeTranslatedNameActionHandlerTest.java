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
 * Tests for {@link ComputeTranslatedNameActionHandler}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ComputeTranslatedNameActionHandlerTest {

  private static final String REMOVE_FRAGMENT = "removeFragment";
  private static final String ABC123 = "abc123";

  private ComputeTranslatedNameActionHandler handler;
  /** Sets up test fixtures. */

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ComputeTranslatedNameActionHandler.class);
  }
  /**
   * Remove fragment returns value without fragment.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentReturnsValueWithoutFragment() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, ABC123);
    assertEquals(ABC123, result);
  }
  /**
   * Remove fragment strips fragment part.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentStripsFragmentPart() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "abc123#fragment");
    assertEquals(ABC123, result);
  }
  /**
   * Remove fragment handles multiple fragments.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentHandlesMultipleFragments() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "abc#first#second");
    assertEquals("abc", result);
  }
  /**
   * Remove fragment returns null for null.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentReturnsNullForNull() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, (Object) null);
    assertNull(result);
  }
  /**
   * Remove fragment handles fragment at start.
   * @throws Exception if an error occurs
   */

  @Test
  public void testRemoveFragmentHandlesFragmentAtStart() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        REMOVE_FRAGMENT, String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "#onlyfragment");
    assertEquals("", result);
  }
}
