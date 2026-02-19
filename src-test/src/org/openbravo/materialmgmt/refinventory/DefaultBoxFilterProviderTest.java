package org.openbravo.materialmgmt.refinventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DefaultBoxFilterProvider}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultBoxFilterProviderTest {

  private final DefaultBoxFilterProvider provider = new DefaultBoxFilterProvider();

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
  public void testGetFilterClauseReturnsExpectedClause() {
    String result = provider.getFilterClause(new HashMap<>());
    assertEquals("and mgt.referencedInventory is null", result);
  }

  @Test
  public void testGetFilterClauseReturnsExpectedClauseWithNullMap() {
    String result = provider.getFilterClause(null);
    assertEquals("and mgt.referencedInventory is null", result);
  }
}
