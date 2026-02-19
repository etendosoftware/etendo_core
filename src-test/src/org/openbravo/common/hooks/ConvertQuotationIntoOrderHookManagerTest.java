package org.openbravo.common.hooks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.common.order.Order;

/**
 * Tests for {@link ConvertQuotationIntoOrderHookManager}.
 * Tests hook execution, ordering by getOrder(), and empty hook list handling.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ConvertQuotationIntoOrderHookManagerTest {

  private ConvertQuotationIntoOrderHookManager instance;

  @Mock
  private Instance<ConvertQuotationIntoOrderHook> mockHookInstance;

  @Mock
  private Instance<ConvertQuotationIntoOrderHook> mockSelectedInstance;

  @Mock
  private Order mockOrder;

  @Before
  public void setUp() throws Exception {
    instance = new ConvertQuotationIntoOrderHookManager();
    Field hookField = ConvertQuotationIntoOrderHookManager.class
        .getDeclaredField("convertQuotationIntoOrderHooks");
    hookField.setAccessible(true);
    hookField.set(instance, mockHookInstance);

    when(mockHookInstance.select(any(Annotation[].class))).thenReturn(mockSelectedInstance);
  }

  @Test
  public void testExecuteHooksWithNoHooks() {
    // Arrange
    when(mockSelectedInstance.iterator())
        .thenReturn(Collections.<ConvertQuotationIntoOrderHook>emptyList().iterator());

    // Act
    instance.executeHooks(mockOrder);

    // Assert - no exception thrown, no hooks called
  }

  @Test
  public void testExecuteHooksCallsSingleHook() {
    // Arrange
    ConvertQuotationIntoOrderHook hook = mock(ConvertQuotationIntoOrderHook.class);
    when(hook.getOrder()).thenReturn(1);
    when(mockSelectedInstance.iterator())
        .thenReturn(Collections.singletonList(hook).iterator());

    // Act
    instance.executeHooks(mockOrder);

    // Assert
    verify(hook).exec(mockOrder);
  }

  @Test
  public void testExecuteHooksCallsMultipleHooksInOrder() {
    // Arrange
    ConvertQuotationIntoOrderHook hook1 = mock(ConvertQuotationIntoOrderHook.class);
    when(hook1.getOrder()).thenReturn(20);
    ConvertQuotationIntoOrderHook hook2 = mock(ConvertQuotationIntoOrderHook.class);
    when(hook2.getOrder()).thenReturn(10);

    when(mockSelectedInstance.iterator())
        .thenReturn(Arrays.asList(hook1, hook2).iterator());

    // Act
    instance.executeHooks(mockOrder);

    // Assert - hook2 (order=10) should execute before hook1 (order=20)
    InOrder inOrder = inOrder(hook2, hook1);
    inOrder.verify(hook2).exec(mockOrder);
    inOrder.verify(hook1).exec(mockOrder);
  }

  @Test
  public void testExecuteHooksSkipsNullHooks() {
    // Arrange
    ConvertQuotationIntoOrderHook hook = mock(ConvertQuotationIntoOrderHook.class);
    when(hook.getOrder()).thenReturn(1);
    when(mockSelectedInstance.iterator())
        .thenReturn(Arrays.<ConvertQuotationIntoOrderHook>asList(null, hook).iterator());

    // Act
    instance.executeHooks(mockOrder);

    // Assert
    verify(hook).exec(mockOrder);
  }

  @Test
  public void testExecuteHooksWithNullInstanceField() throws Exception {
    // Arrange - set the injected field to null
    Field hookField = ConvertQuotationIntoOrderHookManager.class
        .getDeclaredField("convertQuotationIntoOrderHooks");
    hookField.setAccessible(true);
    hookField.set(instance, null);

    // Act - should not throw
    instance.executeHooks(mockOrder);

    // Assert - no exception thrown
  }

  @Test
  public void testExecuteHooksWithEqualOrderValues() {
    // Arrange
    ConvertQuotationIntoOrderHook hook1 = mock(ConvertQuotationIntoOrderHook.class);
    when(hook1.getOrder()).thenReturn(10);
    ConvertQuotationIntoOrderHook hook2 = mock(ConvertQuotationIntoOrderHook.class);
    when(hook2.getOrder()).thenReturn(10);

    when(mockSelectedInstance.iterator())
        .thenReturn(Arrays.asList(hook1, hook2).iterator());

    // Act
    instance.executeHooks(mockOrder);

    // Assert - both hooks should be called
    verify(hook1).exec(mockOrder);
    verify(hook2).exec(mockOrder);
  }
}
