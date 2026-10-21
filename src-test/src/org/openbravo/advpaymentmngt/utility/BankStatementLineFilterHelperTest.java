/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright © 2021–2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.advpaymentmngt.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext.Usage;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.model.financialmgmt.payment.FIN_FinancialAccount;

/**
 * Tests for {@link BankStatementLineFilterHelper}.
 */
public class BankStatementLineFilterHelperTest {

  private static final String ALIAS = "bsl";
  private static final String ACCOUNT_PARAMETER = "account";

  private MockedStatic<WeldUtils> mockedWeldUtils;
  private BankStatementLineFilterContext context;

  /**
   * Mocks the CDI lookup and creates a context for the tests.
   */
  @Before
  public void setUp() {
    mockedWeldUtils = mockStatic(WeldUtils.class);
    context = new BankStatementLineFilterContext(mock(FIN_FinancialAccount.class), null,
        Usage.MATCH_STATEMENT_GRID);
  }

  /**
   * Closes the static mock of the CDI lookup.
   */
  @After
  public void tearDown() {
    mockedWeldUtils.close();
  }

  /**
   * Configures the hooks returned by the CDI lookup.
   *
   * @param hooks
   *          the hooks to return
   */
  private void givenHooks(BankStatementLineFilterHook... hooks) {
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenReturn(Arrays.asList(hooks));
  }

  /**
   * Creates a hook that returns a fixed condition and adds a fixed set of parameters.
   *
   * @param priority
   *          the priority of the hook
   * @param condition
   *          the condition returned by the hook
   * @param parameters
   *          the parameters added by the hook
   * @return the hook
   */
  private BankStatementLineFilterHook hook(int priority, String condition,
      Map<String, Object> parameters) {
    return new BankStatementLineFilterHook() {
      @Override
      public int getPriority() {
        return priority;
      }

      @Override
      public String getHqlCondition(String bslAlias, BankStatementLineFilterContext ctx,
          Map<String, Object> namedParameters) {
        namedParameters.putAll(parameters);
        return condition;
      }
    };
  }

  /**
   * Without hooks the condition is empty and the parameters are not modified.
   */
  @Test
  public void testNoHooksReturnsEmptyCondition() {
    givenHooks();
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ACCOUNT_PARAMETER, "A");

    String result = BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, parameters);

    assertEquals("", result);
    assertEquals(Collections.singletonMap(ACCOUNT_PARAMETER, "A"), parameters);
  }

  /**
   * The conditions of several hooks are combined with AND in ascending order of priority, and
   * their parameters are added to the query parameters.
   */
  @Test
  public void testHooksAreCombinedByPriority() {
    givenHooks(hook(20, "bsl.second = :p2", Collections.singletonMap("p2", 2)),
        hook(10, "bsl.first = :p1", Collections.singletonMap("p1", 1)));
    Map<String, Object> parameters = new HashMap<>();

    String result = BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, parameters);

    assertEquals(" and (bsl.first = :p1) and (bsl.second = :p2)", result);
    assertEquals(1, parameters.get("p1"));
    assertEquals(2, parameters.get("p2"));
  }

  /**
   * The hooks without an explicit priority use the default one.
   */
  @Test
  public void testDefaultPriority() {
    BankStatementLineFilterHook hook = (alias, ctx, params) -> null;

    assertEquals(100, hook.getPriority());
  }

  /**
   * Blank and null conditions are ignored, together with the parameters of those hooks.
   */
  @Test
  public void testBlankConditionsAreIgnored() {
    givenHooks(hook(1, "  ", Collections.singletonMap("ignored", 1)),
        hook(2, null, Collections.emptyMap()), hook(3, "bsl.valid = true", Collections.emptyMap()));
    Map<String, Object> parameters = new HashMap<>();

    String result = BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, parameters);

    assertEquals(" and (bsl.valid = true)", result);
    assertTrue(parameters.isEmpty());
  }

  /**
   * A hook can not overwrite a parameter of the original query.
   */
  @Test
  public void testDuplicatedKeyWithQueryParameterIsRejected() {
    givenHooks(hook(1, "bsl.account.id = :account", Collections.singletonMap(ACCOUNT_PARAMETER, "B")));
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ACCOUNT_PARAMETER, "A");

    try {
      BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, parameters);
      fail("A duplicated parameter key should be rejected");
    } catch (OBException e) {
      assertTrue(e.getMessage().contains("'account'"));
    }
    assertEquals("A", parameters.get(ACCOUNT_PARAMETER));
  }

  /**
   * Two hooks can not use the same parameter key.
   */
  @Test(expected = OBException.class)
  public void testDuplicatedKeyBetweenHooksIsRejected() {
    givenHooks(hook(1, "bsl.a = :shared", Collections.singletonMap("shared", 1)),
        hook(2, "bsl.b = :shared", Collections.singletonMap("shared", 2)));

    BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, new HashMap<>());
  }

  /**
   * When the CDI container is not available no hook is applied.
   */
  @Test
  public void testNoCdiContainerReturnsEmptyCondition() {
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenThrow(new OBException("No bean manager"));

    String result = BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context,
        new HashMap<>());

    assertEquals("", result);
  }

  /**
   * The hooks receive the alias and the context of the query, and an empty map for their
   * parameters.
   */
  @Test
  public void testHookReceivesAliasAndContext() {
    BankStatementLineFilterHook hook = mock(BankStatementLineFilterHook.class);
    when(hook.getHqlCondition(anyString(), any(), anyMap())).thenReturn("x.flag = false");
    givenHooks(hook);
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(ACCOUNT_PARAMETER, "A");

    String result = BankStatementLineFilterHelper.buildHqlCondition("x", context, parameters);

    assertEquals(" and (x.flag = false)", result);
    verify(hook).getHqlCondition(eq("x"), eq(context), eq(new HashMap<>()));
  }

  /**
   * The list returned by the CDI lookup is not modified when the hooks are sorted.
   */
  @Test
  public void testCdiListIsNotModified() {
    BankStatementLineFilterHook second = hook(2, "bsl.b = true", Collections.emptyMap());
    BankStatementLineFilterHook first = hook(1, "bsl.a = true", Collections.emptyMap());
    List<BankStatementLineFilterHook> hooks = Arrays.asList(second, first);
    mockedWeldUtils.when(() -> WeldUtils.getInstances(BankStatementLineFilterHook.class))
        .thenReturn(hooks);

    BankStatementLineFilterHelper.buildHqlCondition(ALIAS, context, new HashMap<>());

    assertSame(second, hooks.get(0));
  }
}
