package org.openbravo.advpaymentmngt.hook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
   * Clase interna para crear implementaciones reales que cumplan con ambas interfaces
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

  @Test
  public void testSortHooksByPriority_correctOrder() {
    // GIVEN
    // Create real hook implementations with different priorities
    PaymentProcessHook highPriorityHook = new TestProcessOrderHook(10);
    PaymentProcessHook mediumPriorityHook = new TestProcessOrderHook(50);
    PaymentProcessHook lowPriorityHook = new TestProcessOrderHook(100);

    // Configure the mock instance to return our hooks in random order
    when(mockHookInstance.iterator()).thenReturn(
        List.of(mediumPriorityHook, highPriorityHook, lowPriorityHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    // Verify hooks are sorted by priority (lowest number first)
    assertEquals("List should contain 3 hooks", 3, sortedHooks.size());
    assertEquals("First hook should be the high priority hook", highPriorityHook, sortedHooks.get(0));
    assertEquals("Second hook should be the medium priority hook", mediumPriorityHook, sortedHooks.get(1));
    assertEquals("Third hook should be the low priority hook", lowPriorityHook, sortedHooks.get(2));
  }

  @Test
  public void testSortHooksByPriorityEmptyList() {
    // GIVEN
    // Configure the mock instance to return an empty list
    when(mockHookInstance.iterator()).thenReturn(List.<PaymentProcessHook>of().iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should be empty", 0, sortedHooks.size());
  }

  @Test
  public void testSortHooksByPrioritySingleHook() {
    // GIVEN
    // Create a single hook implementation
    PaymentProcessHook singleHook = new TestProcessOrderHook(50);

    // Configure the mock instance to return our single hook
    when(mockHookInstance.iterator()).thenReturn(List.of(singleHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should contain 1 hook", 1, sortedHooks.size());
    assertEquals("First hook should be the single hook", singleHook, sortedHooks.get(0));
  }

  @Test
  public void testSortHooksByPrioritySamePriority() {
    // GIVEN
    // Create hooks with the same priority
    PaymentProcessHook firstHook = new TestProcessOrderHook(50);
    PaymentProcessHook secondHook = new TestProcessOrderHook(50);

    // Configure the mock instance to return our hooks
    when(mockHookInstance.iterator()).thenReturn(List.of(firstHook, secondHook).iterator());

    // WHEN
    List<PaymentProcessHook> sortedHooks = PaymentProcessOrderHook.sortHooksByPriority(mockHookInstance);

    // THEN
    assertEquals("List should contain 2 hooks", 2, sortedHooks.size());
    assertTrue("List should contain both hooks", sortedHooks.contains(firstHook) && sortedHooks.contains(secondHook));
  }
}
