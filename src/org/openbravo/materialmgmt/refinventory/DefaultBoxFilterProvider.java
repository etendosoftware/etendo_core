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

import javax.enterprise.context.ApplicationScoped;

/**
 * Default implementation of {@link StockSelectorFilterProvider} for referenced inventory boxing operations.
 * <p>
 * This provider supplies the default filter clause for stock selectors when boxing referenced inventory items.
 * It supports the default referenced inventory type and blank types, and has a priority of 20.
 */
@ApplicationScoped
public class DefaultBoxFilterProvider implements StockSelectorFilterProvider {
  /**
   * Checks if the given referenced inventory type is supported by this provider.
   * <p>
   * This generic implementation always returns false, indicating that it does not explicitly support any type.
   * The dispatcher {@link org.openbravo.common.datasource.StockSelectorDispatcherHQLTransformer} will select this provider only when no specific
   * implementation exists for the referenced inventory type, ensuring default behavior is applied.
   *
   * @param inventoryType
   *     the referenced inventory type to check
   * @return false, as this provider is only used as a fallback when no specific provider matches
   */
  @Override
  public boolean supports(String inventoryType) {
    return false;
  }

  /**
   * Returns the filter clause to be applied in the stock selector for boxing operations.
   * <p>
   * The default clause filters items with no referenced inventory.
   *
   * @param rp
   *     a map of request parameters (not used in this implementation)
   * @return the filter clause as a String
   */
  @Override
  public String getFilterClause(Map<String, String> rp) {
    return "and mgt.referencedInventory is null";
  }
}
