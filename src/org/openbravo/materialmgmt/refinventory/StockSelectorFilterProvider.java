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
   * @param inventoryType the referenced inventory type to check
   * @return true if the type is supported, false otherwise
   */
  boolean supports(String inventoryType);

  /**
   * Returns the filter clause to be applied in the stock selector, based on the provided request parameters.
   *
   * @param requestParameters a map of request parameters
   * @return the filter clause as a String
   */
  String getFilterClause(Map<String, String> requestParameters);

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
