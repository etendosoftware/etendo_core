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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterContext;
import org.openbravo.advpaymentmngt.hook.BankStatementLineFilterHook;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.weld.WeldUtils;

/**
 * Builds the HQL condition contributed by all the {@link BankStatementLineFilterHook}
 * implementations, so it can be appended to any query over {@code FIN_BankStatementLine}.
 */
public final class BankStatementLineFilterHelper {

  private static final Logger log = LogManager.getLogger();

  /**
   * Utility class, not meant to be instantiated.
   */
  private BankStatementLineFilterHelper() {
  }

  /**
   * Builds the combined condition of all the available hooks, sorted by priority.
   * <p>
   * The named parameters required by the hooks are added to {@code namedParameters}. Each hook
   * receives its own empty map, and its keys are checked against the ones already present, so a
   * hook can never overwrite a parameter of the original query or of another hook.
   *
   * @param bslAlias
   *          the alias of the {@code FIN_BankStatementLine} entity in the query
   * @param context
   *          information about the query being built
   * @param namedParameters
   *          the named parameters of the query, where the parameters of the hooks are added
   * @return an empty string if no hook restricts the lines, or a string starting with
   *         {@code " and "} followed by every hook condition between brackets, for example
   *         {@code " and (c1) and (c2)"}
   * @throws OBException
   *           if a hook uses a parameter key that is already in use
   */
  public static String buildHqlCondition(String bslAlias, BankStatementLineFilterContext context,
      Map<String, Object> namedParameters) {
    final StringBuilder condition = new StringBuilder();
    for (BankStatementLineFilterHook hook : getSortedHooks()) {
      final Map<String, Object> hookParameters = new HashMap<>();
      final String hookCondition = hook.getHqlCondition(bslAlias, context, hookParameters);
      if (StringUtils.isBlank(hookCondition)) {
        continue;
      }
      addHookParameters(hook, hookParameters, namedParameters);
      condition.append(" and (").append(hookCondition).append(")");
    }
    return condition.toString();
  }

  /**
   * Copies the parameters of a hook into the parameters of the query, rejecting duplicated keys.
   *
   * @param hook
   *          the hook that provided the parameters, used in the error message
   * @param hookParameters
   *          the parameters provided by the hook
   * @param namedParameters
   *          the parameters of the query
   * @throws OBException
   *           if one of the keys of the hook is already in use
   */
  private static void addHookParameters(BankStatementLineFilterHook hook,
      Map<String, Object> hookParameters, Map<String, Object> namedParameters) {
    for (Map.Entry<String, Object> parameter : hookParameters.entrySet()) {
      if (namedParameters.containsKey(parameter.getKey())) {
        throw new OBException("The bank statement line filter hook " + hook.getClass().getName()
            + " uses the named parameter '" + parameter.getKey() + "', which is already in use");
      }
      namedParameters.put(parameter.getKey(), parameter.getValue());
    }
  }

  /**
   * Returns the available hooks sorted by ascending priority. When no CDI container is available,
   * for example in some unit tests or standalone processes, no hook is returned.
   *
   * @return the sorted list of hooks, never {@code null}
   */
  private static List<BankStatementLineFilterHook> getSortedHooks() {
    final List<BankStatementLineFilterHook> hooks;
    try {
      hooks = new ArrayList<>(WeldUtils.getInstances(BankStatementLineFilterHook.class));
    } catch (RuntimeException e) {
      log.debug("Bank statement line filter hooks are not available: {}", e.getMessage());
      return Collections.emptyList();
    }
    hooks.sort(Comparator.comparingInt(BankStatementLineFilterHook::getPriority));
    return hooks;
  }
}
