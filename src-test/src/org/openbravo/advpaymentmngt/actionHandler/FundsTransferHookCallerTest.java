package org.openbravo.advpaymentmngt.actionHandler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.inject.Instance;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.objenesis.ObjenesisStd;
import org.openbravo.advpaymentmngt.FundsTransferPostProcessHook;
import org.openbravo.model.financialmgmt.payment.FIN_FinaccTransaction;

/**
 * Tests for {@link FundsTransferHookCaller}.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class FundsTransferHookCallerTest {

  private FundsTransferHookCaller instance;

  @Mock
  private Instance<FundsTransferPostProcessHook> mockHooksInstance;

  @Before
  public void setUp() throws Exception {
    ObjenesisStd objenesis = new ObjenesisStd();
    instance = objenesis.newInstance(FundsTransferHookCaller.class);

    Field hooksField = FundsTransferHookCaller.class.getDeclaredField("hooks");
    hooksField.setAccessible(true);
    hooksField.set(instance, mockHooksInstance);
  }

  @Test
  public void testExecuteHookCallsAllHooks() throws Exception {
    FundsTransferPostProcessHook hook1 = mock(FundsTransferPostProcessHook.class);
    FundsTransferPostProcessHook hook2 = mock(FundsTransferPostProcessHook.class);
    List<FundsTransferPostProcessHook> hookList = Arrays.asList(hook1, hook2);

    org.mockito.Mockito.when(mockHooksInstance.iterator()).thenReturn(hookList.iterator());

    List<FIN_FinaccTransaction> transactions = new ArrayList<>();
    instance.executeHook(transactions);

    verify(hook1).exec(transactions);
    verify(hook2).exec(transactions);
  }

  @Test
  public void testExecuteHookWithNoHooks() throws Exception {
    List<FundsTransferPostProcessHook> emptyList = Collections.emptyList();
    org.mockito.Mockito.when(mockHooksInstance.iterator()).thenReturn(emptyList.iterator());

    List<FIN_FinaccTransaction> transactions = new ArrayList<>();
    instance.executeHook(transactions);
    // No exception, no hooks called
  }

  @Test
  public void testExecuteHookWithSingleHook() throws Exception {
    FundsTransferPostProcessHook hook = mock(FundsTransferPostProcessHook.class);
    List<FundsTransferPostProcessHook> hookList = Collections.singletonList(hook);

    org.mockito.Mockito.when(mockHooksInstance.iterator()).thenReturn(hookList.iterator());

    List<FIN_FinaccTransaction> transactions = new ArrayList<>();
    FIN_FinaccTransaction trx = mock(FIN_FinaccTransaction.class);
    transactions.add(trx);
    instance.executeHook(transactions);

    verify(hook).exec(transactions);
  }

  @Test(expected = Exception.class)
  public void testExecuteHookPropagatesException() throws Exception {
    FundsTransferPostProcessHook hook = mock(FundsTransferPostProcessHook.class);
    List<FundsTransferPostProcessHook> hookList = Collections.singletonList(hook);
    org.mockito.Mockito.when(mockHooksInstance.iterator()).thenReturn(hookList.iterator());

    List<FIN_FinaccTransaction> transactions = new ArrayList<>();
    org.mockito.Mockito.doThrow(new RuntimeException("hook error")).when(hook).exec(transactions);

    instance.executeHook(transactions);
  }
}
