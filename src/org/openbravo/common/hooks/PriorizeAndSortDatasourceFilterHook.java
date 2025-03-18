package org.openbravo.common.hooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.enterprise.inject.Instance;

public abstract class PriorizeAndSortDatasourceFilterHook {

  public abstract int getPriority();

  public static List<DatasourceFilterHook> sortHooksByPriority(Instance<DatasourceFilterHook> hooks) {
    List<DatasourceFilterHook> hookList = new ArrayList<>();
    for (DatasourceFilterHook hookToAdd : hooks) {
      hookList.add(hookToAdd);
    }

    hookList.sort((Comparator<Object>) (o1, o2) -> {
      int o1Priority = ((PriorizeAndSortDatasourceFilterHook) o1).getPriority();
      int o2Priority = ((PriorizeAndSortDatasourceFilterHook) o2).getPriority();
      return (int) Math.signum((float) o1Priority - (float) o2Priority);
    });

    return hookList;
  }

}

