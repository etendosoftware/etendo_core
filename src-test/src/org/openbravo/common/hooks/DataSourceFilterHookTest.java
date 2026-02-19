package org.openbravo.common.hooks;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Tests for {@link DataSourceFilterHook}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DataSourceFilterHookTest {

  @Mock
  private Instance<DataSourceFilterHook> mockHookInstance;

  @Test
  public void testDefaultGetPriorityReturnsZero() {
    DataSourceFilterHook hook = new DataSourceFilterHook() {};
    assertEquals(0, hook.getPriority());
  }

  @Test
  public void testDefaultPreProcessDoesNothing() {
    DataSourceFilterHook hook = new DataSourceFilterHook() {};
    Map<String, String> params = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    // Should not throw
    hook.preProcess(params, filters);
  }

  @Test
  public void testDefaultPostProcessDoesNothing() {
    DataSourceFilterHook hook = new DataSourceFilterHook() {};
    Map<String, String> params = new HashMap<>();
    Map<String, Object> filters = new HashMap<>();
    // Should not throw
    hook.postProcess(params, filters);
  }

  @Test
  public void testSortHooksByPriorityAscending() {
    DataSourceFilterHook hook1 = createHookWithPriority(30);
    DataSourceFilterHook hook2 = createHookWithPriority(10);
    DataSourceFilterHook hook3 = createHookWithPriority(20);

    Iterator<DataSourceFilterHook> iterator = Arrays.asList(hook1, hook2, hook3).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);

    List<DataSourceFilterHook> sorted = DataSourceFilterHook.sortHooksByPriority(mockHookInstance);

    assertEquals(3, sorted.size());
    assertEquals(hook2, sorted.get(0)); // priority 10
    assertEquals(hook3, sorted.get(1)); // priority 20
    assertEquals(hook1, sorted.get(2)); // priority 30
  }

  @Test
  public void testSortHooksByPriorityEmptyList() {
    Iterator<DataSourceFilterHook> emptyIterator = Collections
        .<DataSourceFilterHook>emptyList().iterator();
    when(mockHookInstance.iterator()).thenReturn(emptyIterator);

    List<DataSourceFilterHook> sorted = DataSourceFilterHook.sortHooksByPriority(mockHookInstance);

    assertEquals(0, sorted.size());
  }

  @Test
  public void testSortHooksByPrioritySingleElement() {
    DataSourceFilterHook hook = createHookWithPriority(5);
    Iterator<DataSourceFilterHook> iterator = Collections.singletonList(hook).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);

    List<DataSourceFilterHook> sorted = DataSourceFilterHook.sortHooksByPriority(mockHookInstance);

    assertEquals(1, sorted.size());
    assertEquals(hook, sorted.get(0));
  }

  @Test
  public void testSortHooksByPriorityEqualPriorities() {
    DataSourceFilterHook hook1 = createHookWithPriority(5);
    DataSourceFilterHook hook2 = createHookWithPriority(5);

    Iterator<DataSourceFilterHook> iterator = Arrays.asList(hook1, hook2).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);

    List<DataSourceFilterHook> sorted = DataSourceFilterHook.sortHooksByPriority(mockHookInstance);

    assertEquals(2, sorted.size());
  }

  private DataSourceFilterHook createHookWithPriority(int priority) {
    return new DataSourceFilterHook() {
      @Override
      public int getPriority() {
        return priority;
      }
    };
  }
}
