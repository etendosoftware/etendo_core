package org.openbravo.materialmgmt.refinventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultProcessorProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultProcessorProviderTest {

  private final DefaultProcessorProvider provider = new DefaultProcessorProvider();
  /** Supports returns false. */

  @Test
  public void testSupportsReturnsFalse() {
    assertFalse(provider.supports("anyType"));
  }
  /** Supports returns false for null. */

  @Test
  public void testSupportsReturnsFalseForNull() {
    assertFalse(provider.supports(null));
  }
  /** Supports returns false for empty. */

  @Test
  public void testSupportsReturnsFalseForEmpty() {
    assertFalse(provider.supports(""));
  }
  /** Get box processor class returns box processor. */

  @Test
  public void testGetBoxProcessorClassReturnsBoxProcessor() {
    assertEquals(BoxProcessor.class, provider.getBoxProcessorClass());
  }
  /** Get unbox processor class returns unbox processor. */

  @Test
  public void testGetUnboxProcessorClassReturnsUnboxProcessor() {
    assertEquals(UnboxProcessor.class, provider.getUnboxProcessorClass());
  }
}
