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

  private ComputeTranslatedNameActionHandler handler;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ComputeTranslatedNameActionHandler.class);
  }

  @Test
  public void testRemoveFragmentReturnsValueWithoutFragment() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "abc123");
    assertEquals("abc123", result);
  }

  @Test
  public void testRemoveFragmentStripsFragmentPart() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "abc123#fragment");
    assertEquals("abc123", result);
  }

  @Test
  public void testRemoveFragmentHandlesMultipleFragments() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "abc#first#second");
    assertEquals("abc", result);
  }

  @Test
  public void testRemoveFragmentReturnsNullForNull() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, (Object) null);
    assertNull(result);
  }

  @Test
  public void testRemoveFragmentHandlesFragmentAtStart() throws Exception {
    Method removeFragment = ComputeTranslatedNameActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "#onlyfragment");
    assertEquals("", result);
  }
}
