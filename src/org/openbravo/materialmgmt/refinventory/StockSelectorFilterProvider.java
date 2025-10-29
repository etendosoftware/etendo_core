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
 * All portions are Copyright © 2021–2025 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
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
   * Returns the select clause to be applied in the stock selector HQL query, based on the provided request parameters.
   * <p>
   * Implementations can override this method to inject custom select logic depending on the referenced inventory type
   * and request context. By default, it returns an empty string, meaning no additional select is applied.
   *
   * @param requestParameters
   *     a map of request parameters
   * @return the select clause as a String, or an empty String if no select is required
   */
  default String getSelectClause(Map<String, String> requestParameters) {
    return "";
  }
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
