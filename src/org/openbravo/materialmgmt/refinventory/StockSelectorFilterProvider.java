package org.openbravo.materialmgmt.refinventory;

import java.util.Map;

/**
 * Provider interface for stock selector filter clauses.
 * <p>
 * Implementations of this interface supply filter clauses for stock selectors based on referenced inventory types
 * and request parameters. Providers can also define their priority for selection.
 */
public interface StockSelectorFilterProvider {
  /**
   * Determines if the provider supports the given referenced inventory type.
   *
   * @param inventoryType
   *     the referenced inventory type to check
   * @return true if the type is supported, false otherwise
   */
  boolean supports(String inventoryType);

  /**
   * Returns the filter clause to be applied in the stock selector HQL query, based on the provided request parameters.
   * <p>
   * Implementations can override this method to inject custom filtering logic depending on the referenced inventory type
   * and request context. By default, it returns an empty string, meaning no additional filter is applied.
   *
   * @param requestParameters
   *     a map of request parameters
   * @return the filter clause as a String, or an empty String if no filter is required
   */
  default String getFilterClause(Map<String, String> requestParameters) {
    return "";
  }

  /**
   * Returns the join clause to be applied in the stock selector HQL query, based on the provided request parameters.
   * <p>
   * Implementations can override this method to inject additional JOIN statements required for filtering logic
   * depending on the referenced inventory type and request context. By default, it returns an empty string, meaning no join is required.
   *
   * @param requestParameters
   *     a map of request parameters
   * @return the join clause as a String, or an empty String if no join is required
   */
  default String getJoinClause(Map<String, String> requestParameters) {
    return "";
  }

  /**
   * Returns the priority of this provider. Higher priority providers are preferred when multiple providers support the same type.
   * Default priority is 50.
   *
   * @return the priority value
   */
  default int getProviderPriority() {
    return 50;
  }
}
