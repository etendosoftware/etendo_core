/*
 * Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 */
package org.openbravo.erpCommon.businessUtility;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.model.common.order.Order;

/**
 * Tests for {@link CloneOrderHookCaller}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CloneOrderHookCallerTest {

  private CloneOrderHookCaller caller;

  @Mock
  private Instance<CloneOrderHook> hookInstance;

  @Mock
  private Order mockOrder;

  @Mock
  private CloneOrderHook hook1;

  @Mock
  private CloneOrderHook hook2;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    caller = objenesis.newInstance(CloneOrderHookCaller.class);

    Field field = CloneOrderHookCaller.class.getDeclaredField("cloneOrderHookProcess");
    field.setAccessible(true);
    field.set(caller, hookInstance);
  }

  @Test
  public void testExecuteHookCallsAllHooks() throws Exception {
    Iterator<CloneOrderHook> iterator = Arrays.asList(hook1, hook2).iterator();
    when(hookInstance.iterator()).thenReturn(iterator);

    caller.executeHook(mockOrder);

    verify(hook1).exec(mockOrder);
    verify(hook2).exec(mockOrder);
  }

  @Test
  public void testExecuteHookWithNoHooks() throws Exception {
    Iterator<CloneOrderHook> iterator = Collections.<CloneOrderHook> emptyList().iterator();
    when(hookInstance.iterator()).thenReturn(iterator);

    caller.executeHook(mockOrder);

    verify(hook1, never()).exec(mockOrder);
  }

  @Test
  public void testExecuteHookWithSingleHook() throws Exception {
    Iterator<CloneOrderHook> iterator = Collections.singletonList(hook1).iterator();
    when(hookInstance.iterator()).thenReturn(iterator);

    caller.executeHook(mockOrder);

    verify(hook1).exec(mockOrder);
    verify(hook2, never()).exec(mockOrder);
  }

  @Test(expected = Exception.class)
  public void testExecuteHookPropagatesException() throws Exception {
    Iterator<CloneOrderHook> iterator = Collections.singletonList(hook1).iterator();
    when(hookInstance.iterator()).thenReturn(iterator);
    doThrow(new RuntimeException("Hook failed")).when(hook1).exec(mockOrder);

    caller.executeHook(mockOrder);
  }
}
