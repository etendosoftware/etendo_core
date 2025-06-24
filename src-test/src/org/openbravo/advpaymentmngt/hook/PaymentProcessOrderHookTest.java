package org.openbravo.advpaymentmngt.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for the PaymentProcessOrderHook class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PaymentProcessOrderHookTest {

  @Mock
  private Instance<PaymentProcessHook> mockHookInstance;

  /**
   * Tests that hooks are sorted by priority in the correct order.
   */
  @Test
  public void testSortHooksByPriorityCorrectOrder() {
    // GIVEN
    PaymentProcessHook highPriorityHook = new TestProcessOrderHook(10);
    PaymentProcessHook mediumPriorityHook = new TestProcessOrderHook(50);
    PaymentProcessHook lowPriorityHook = new TestProcessOrderHook(100);

    when(mockHookInstance.iterator()).thenReturn(
        List.of(mediumPriorityHook, highPriorityHook, lowPriorityHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should contain 3 hooks", 3, sortedHooks.size());
    assertEquals("First hook should be the high priority hook", highPriorityHook, sortedHooks.get(0));
    assertEquals("Second hook should be the medium priority hook", mediumPriorityHook, sortedHooks.get(1));
    assertEquals("Third hook should be the low priority hook", lowPriorityHook, sortedHooks.get(2));
  }

  /**
   * Tests that an empty list is returned when there are no hooks.
   */
  @Test
  public void testSortHooksByPriorityEmptyList() {
    // GIVEN
    when(mockHookInstance.iterator()).thenReturn(Collections.emptyIterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should be empty", 0, sortedHooks.size());
  }

  /**
   * Tests that a single hook is correctly sorted.
   */
  @Test
  public void testSortHooksByPrioritySingleHook() {
    // GIVEN
    PaymentProcessHook singleHook = new TestProcessOrderHook(50);

    when(mockHookInstance.iterator()).thenReturn(List.of(singleHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should contain 1 hook", 1, sortedHooks.size());
    assertEquals("First hook should be the single hook", singleHook, sortedHooks.get(0));
  }

  /**
   * Tests that hooks with the same priority are correctly handled.
   */
  @Test
  public void testSortHooksByPrioritySamePriority() {
    // GIVEN
    PaymentProcessHook firstHook = new TestProcessOrderHook(50);
    PaymentProcessHook secondHook = new TestProcessOrderHook(50);

    when(mockHookInstance.iterator()).thenReturn(List.of(firstHook, secondHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should contain 2 hooks", 2, sortedHooks.size());
    assertTrue("List should contain both hooks", sortedHooks.contains(firstHook) && sortedHooks.contains(secondHook));
  }

  /**
   * Test implementation of PaymentProcessOrderHook for testing purposes.
   */
  private static class TestProcessOrderHook extends PaymentProcessOrderHook implements PaymentProcessHook {
    private final int priority;

    public TestProcessOrderHook(int priority) {
      this.priority = priority;
    }

    @Override
    public int getPriority() {
      return priority;
    }

    @Override
    public JSONObject preProcess(JSONObject params) {
      return params;
    }

    @Override
    public JSONObject posProcess(JSONObject params) {
      return params;
    }

    @Override
    public String toString() {
      return "TestHook-" + priority;
    }
  }
}
