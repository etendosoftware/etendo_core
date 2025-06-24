package org.openbravo.common.hooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.Instance;

/**
 * Interface for applying custom filters to datasources.
 * <p>
 * This interface defines methods that can be implemented to apply pre-processing
 * and post-processing logic when handling datasource operations. Both methods
 * have default implementations, making their implementation optional.
 * </p>
 */
public interface DataSourceFilterHook {

  /**
   * Gets the priority of the filter hook.
   * <p>
   * This method returns the priority of the filter hook, which is used to
   * determine the order in which the hooks are executed. A higher priority
   * means that the filter hook will be executed before others with lower
   * priorities.
   * </p>
   * <p>
   * The default implementation returns 0.
   * </p>
   *
   * @return the priority of the filter hook
   */
  default int getPriority() {
    return 0;
  }

  /**
   * Applies pre-processing logic when handling datasource operations.
   * <p>
   * This method will be called before any datasource operation is
   * executed. It can be used to modify the parameters and the filters
   * criteria that will be used in the operation.
   * </p>
   *
   * @param parameters
   *     the operation parameters
   * @param filtersCriteria
   *     the filters criteria
   */
  default void preProcess(Map<String, String> parameters, Map<String, Object> filtersCriteria) {
    // Default implementation does nothing
  }

  /**
   * Applies post-processing logic when handling datasource operations.
   * <p>
   * This method will be called after any datasource operation is executed.
   * It can be used to modify the result of the operation.
   * </p>
   *
   * @param parameters
   *     the operation parameters
   * @param filtersCriteria
   *     the filters criteria
   */
  default void postProcess(Map<String, String> parameters, Map<String, Object> filtersCriteria) {
    // Default implementation does nothing
  }

  /**
   * Sorts the given list of datasource filter hooks by their priority.
   * <p>
   * Hooks with lower priority values are executed first.
   * </p>
   *
   * @param hooks
   *     the list (Instance) of datasource filter hooks
   * @return a new list of hooks, sorted by ascending priority
   */
  static List<DataSourceFilterHook> sortHooksByPriority(Instance<DataSourceFilterHook> hooks) {
    List<DataSourceFilterHook> hookList = new ArrayList<>();
    for (DataSourceFilterHook hook : hooks) {
      hookList.add(hook);
    }

    hookList.sort(Comparator.comparingInt(DataSourceFilterHook::getPriority));
    return hookList;
  }

}
