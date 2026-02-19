package org.openbravo.client.application.attachment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;

/**
 * Tests for {@link CoreAttachImplementation}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CoreAttachImplementationTest {

  @Test
  public void testSplitPathSplitsIntoChunksOfThree() {
    String result = CoreAttachImplementation.splitPath("123456789");
    assertEquals("123/456/789", result);
  }

  @Test
  public void testSplitPathHandlesNonMultipleOfThree() {
    String result = CoreAttachImplementation.splitPath("12345");
    assertEquals("123/45", result);
  }

  @Test
  public void testSplitPathHandlesShortString() {
    String result = CoreAttachImplementation.splitPath("AB");
    assertEquals("AB", result);
  }

  @Test
  public void testSplitPathHandlesExactlyThreeChars() {
    String result = CoreAttachImplementation.splitPath("ABC");
    assertEquals("ABC", result);
  }

  @Test
  public void testSplitPathHandlesEmptyString() {
    String result = CoreAttachImplementation.splitPath("");
    assertEquals("", result);
  }

  @Test
  public void testSplitPathHandlesUUID() {
    String result = CoreAttachImplementation.splitPath("0F3A10E019754BACA5844387FB37B0D5");
    assertEquals("0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5", result);
  }

  @Test
  public void testSplitPathHandlesSingleChar() {
    String result = CoreAttachImplementation.splitPath("X");
    assertEquals("X", result);
  }

  @Test
  public void testGetPathReturnsNullForOldFormat() {
    assertNull(CoreAttachImplementation.getPath("259-0F3A10E019754BACA5844387FB37B0D5"));
  }

  @Test
  public void testGetPathReturnsValueForNewFormat() {
    String path = "259/0F3/A10/E01/975/4BA/CA5/844/387/FB3/7B0/D5";
    assertEquals(path, CoreAttachImplementation.getPath(path));
  }

  @Test
  public void testGetPathReturnsNullForNull() {
    assertNull(CoreAttachImplementation.getPath(null));
  }

  @Test
  public void testGetPathReturnsEmptyForEmpty() {
    assertEquals("", CoreAttachImplementation.getPath(""));
  }

  @Test
  public void testIsTempFileReturnsFalse() {
    ObjenesisStd objenesis = new ObjenesisStd();
    CoreAttachImplementation instance = objenesis.newInstance(CoreAttachImplementation.class);
    assertFalse(instance.isTempFile());
  }
}
