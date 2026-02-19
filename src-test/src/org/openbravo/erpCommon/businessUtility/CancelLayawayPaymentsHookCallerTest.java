package org.openbravo.erpCommon.businessUtility;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.enterprise.inject.Instance;

import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openbravo.model.common.order.Order;

/**
 * Tests for {@link CancelLayawayPaymentsHookCaller}.
 */
@SuppressWarnings({"java:S120"})
@RunWith(MockitoJUnitRunner.class)
public class CancelLayawayPaymentsHookCallerTest {

  private CancelLayawayPaymentsHookCaller caller;

  @Mock
  private Instance<CancelLayawayPaymentsHook> mockHookInstance;

  @Mock
  private JSONObject mockJsonOrder;

  @Mock
  private Order mockInverseOrder;
  /**
   * Sets up test fixtures.
   * @throws Exception if an error occurs
   */

  @Before
  public void setUp() throws Exception {
    caller = new CancelLayawayPaymentsHookCaller();
    Field hookField = CancelLayawayPaymentsHookCaller.class.getDeclaredField(
        "cancelLayawayPaymentsHook");
    hookField.setAccessible(true);
    hookField.set(caller, mockHookInstance);
  }
  /**
   * Execute hook calls all registered hooks.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHookCallsAllRegisteredHooks() throws Exception {
    CancelLayawayPaymentsHook hook1 = mock(CancelLayawayPaymentsHook.class);
    CancelLayawayPaymentsHook hook2 = mock(CancelLayawayPaymentsHook.class);
    Iterator<CancelLayawayPaymentsHook> iterator = Arrays.asList(hook1, hook2).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);

    caller.executeHook(mockJsonOrder, mockInverseOrder);

    verify(hook1).exec(mockJsonOrder, mockInverseOrder);
    verify(hook2).exec(mockJsonOrder, mockInverseOrder);
  }
  /**
   * Execute hook with no registered hooks.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHookWithNoRegisteredHooks() throws Exception {
    Iterator<CancelLayawayPaymentsHook> emptyIterator = Collections
        .<CancelLayawayPaymentsHook>emptyList().iterator();
    when(mockHookInstance.iterator()).thenReturn(emptyIterator);

    caller.executeHook(mockJsonOrder, mockInverseOrder);
    // No exception should be thrown
  }
  /**
   * Execute hook calls single hook.
   * @throws Exception if an error occurs
   */

  @Test
  public void testExecuteHookCallsSingleHook() throws Exception {
    CancelLayawayPaymentsHook hook = mock(CancelLayawayPaymentsHook.class);
    Iterator<CancelLayawayPaymentsHook> iterator = Collections.singletonList(hook).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);

    caller.executeHook(mockJsonOrder, mockInverseOrder);

    verify(hook).exec(mockJsonOrder, mockInverseOrder);
  }
  /**
   * Execute hook propagates exception.
   * @throws Exception if an error occurs
   */

  @Test(expected = Exception.class)
  public void testExecuteHookPropagatesException() throws Exception {
    CancelLayawayPaymentsHook hook = mock(CancelLayawayPaymentsHook.class);
    Iterator<CancelLayawayPaymentsHook> iterator = Collections.singletonList(hook).iterator();
    when(mockHookInstance.iterator()).thenReturn(iterator);
    org.mockito.Mockito.doThrow(new RuntimeException("Hook failed"))
        .when(hook).exec(mockJsonOrder, mockInverseOrder);

    caller.executeHook(mockJsonOrder, mockInverseOrder);
  }
}
