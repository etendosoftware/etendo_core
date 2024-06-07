package org.openbravo.common.hooks;


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Instance;

public abstract class PrintControllerOrderHook {

  /**
   * Sorts a list of hooks by their priority.
   *
   * @param hooks
   *     the hooks to be sorted
   * @return a list of hooks sorted by priority
   */
  public static List<PrintControllerHook> sortHooksByPriority(Instance<PrintControllerHook> hooks) {
    List<PrintControllerHook> hookList = new ArrayList<>();
    hooks.forEach(hookList::add);

    hookList.sort((Comparator<Object>) (o1, o2) -> {
      int o1Priority = ((PrintControllerOrderHook) o1).getPriority();
      int o2Priority = ((PrintControllerOrderHook) o2).getPriority();
      return (int) Math.signum((float) o1Priority - (float) o2Priority);
    });

    return hookList;
  }

  /**
   * Returns the priority of the hook.
   *
   * @return the priority of the hook
   */
  public abstract int getPriority();
}
