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

  @Test
  public void testSupportsReturnsFalse() {
    assertFalse(provider.supports("anyType"));
  }

  @Test
  public void testSupportsReturnsFalseForNull() {
    assertFalse(provider.supports(null));
  }

  @Test
  public void testSupportsReturnsFalseForEmpty() {
    assertFalse(provider.supports(""));
  }

  @Test
  public void testGetBoxProcessorClassReturnsBoxProcessor() {
    assertEquals(BoxProcessor.class, provider.getBoxProcessorClass());
  }

  @Test
  public void testGetUnboxProcessorClassReturnsUnboxProcessor() {
    assertEquals(UnboxProcessor.class, provider.getUnboxProcessorClass());
  }
}
