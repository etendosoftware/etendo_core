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
  /** Supports returns false. */

  @Test
  public void testSupportsReturnsFalse() {
    assertSupportsReturnsFalse("anyType");
  }
  /** Supports returns false for null. */

  @Test
  public void testSupportsReturnsFalseForNull() {
    assertSupportsReturnsFalse(null);
  }
  /** Supports returns false for empty. */

  @Test
  public void testSupportsReturnsFalseForEmpty() {
    assertSupportsReturnsFalse("");
  }

  private void assertSupportsReturnsFalse(String input) {
    assertFalse(provider.supports(input));
  }
  /** Get filter clause returns expected clause. */

  @Test
  public void testGetFilterClauseReturnsExpectedClause() {
    String result = provider.getFilterClause(new HashMap<>());
    assertEquals("and mgt.referencedInventory is null", result);
  }
  /** Get filter clause returns expected clause with null map. */

  @Test
  public void testGetFilterClauseReturnsExpectedClauseWithNullMap() {
    String result = provider.getFilterClause(null);
    assertEquals("and mgt.referencedInventory is null", result);
  }
}
