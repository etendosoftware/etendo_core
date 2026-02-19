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

  private ComputeWindowActionHandler handler;

  @Before
  public void setUp() {
    ObjenesisStd objenesis = new ObjenesisStd();
    handler = objenesis.newInstance(ComputeWindowActionHandler.class);
  }

  @Test
  public void testRemoveFragmentReturnsValueWithoutFragment() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "tabId123");
    assertEquals("tabId123", result);
  }

  @Test
  public void testRemoveFragmentStripsFragmentPart() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "tabId123#fragment");
    assertEquals("tabId123", result);
  }

  @Test
  public void testRemoveFragmentReturnsNullForNull() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, (Object) null);
    assertNull(result);
  }

  @Test
  public void testRemoveFragmentHandlesEmptyString() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "");
    assertEquals("", result);
  }

  @Test
  public void testRemoveFragmentHandlesFragmentAtStart() throws Exception {
    Method removeFragment = ComputeWindowActionHandler.class.getDeclaredMethod(
        "removeFragment", String.class);
    removeFragment.setAccessible(true);

    String result = (String) removeFragment.invoke(handler, "#onlyfragment");
    assertEquals("", result);
  }
}
