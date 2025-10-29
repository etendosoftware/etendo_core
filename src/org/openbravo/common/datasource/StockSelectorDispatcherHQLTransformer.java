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
package org.openbravo.common.datasource;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.DefaultBoxFilterProvider;
import org.openbravo.materialmgmt.refinventory.StockSelectorFilterProvider;
import org.openbravo.model.materialmgmt.onhandquantity.ReferencedInventory;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

/**
 * Unique dispatcher for the M_Storage_Detail table HQL transformation.
 * <p>
 * This class injects the appropriate filter clause into the HQL query based on the referenced inventory type.
 * It selects the correct {@link StockSelectorFilterProvider} implementation according to the inventory type,
 * ensuring that the stock selector applies the correct filtering logic for each scenario.
 * <p>
 * If no provider matches, it applies the default filter clause for historical behavior.
 */
@ApplicationScoped
@ComponentProvider.Qualifier("F99379660FC24436839F70E195FFBD09") // UUID for the table
public class StockSelectorDispatcherHQLTransformer extends HqlQueryTransformer {

  /**
   * Default filter clause applied when no provider matches the inventory type.
   * This maintains historical behavior by filtering items with no referenced inventory.
   */
  private static final String DEFAULT_FILTER =
      "and mgt.referencedInventory is null";

  /**
   * Default join clause applied when no provider matches the inventory type.
   */
  private static final String DEFAULT_JOIN_CLAUSE =
      "";

  /**
   * Default select clause applied when no provider matches the inventory type.
   */
  private static final String DEFAULT_SELECT_CLAUSE =
      "";

  /**
   * Instance of {@link StockSelectorFilterProvider} to be used for filtering the stock selector.
   */
  private final Instance<StockSelectorFilterProvider> providers;

  /**
   * Constructs a new StockSelectorDispatcherHQLTransformer with the given providers.
   *
   * @param providers
   *     the instance of {@link StockSelectorFilterProvider} implementations to be used for filtering
   */
  @Inject
  public StockSelectorDispatcherHQLTransformer(@Any Instance<StockSelectorFilterProvider> providers) {
    /**
     * Injects the {@link StockSelectorFilterProvider} instance.
     */
    this.providers = providers;
  }


  /**
   * Transforms the HQL query for the stock selector by injecting the appropriate join and filter clauses.
   * <p>
   * This method determines the referenced inventory type from the request parameters, selects the best matching
   * {@link StockSelectorFilterProvider} (by priority), and applies its join and filter clauses. If no provider matches,
   * the default provider ({@link DefaultBoxFilterProvider}) is used, which applies historical filtering behavior.
   * <p>
   * The method replaces the placeholders <code>@joinClause@</code> and <code>@whereClause@</code> in the original HQL query
   * with the clauses provided by the selected provider.
   *
   * @param hql
   *     the original HQL query containing placeholders for join and where clauses
   * @param requestParams
   *     the request parameters, including referenced inventory id
   * @param namedParams
   *     named parameters for the query (not used in this implementation)
   * @return the transformed HQL query with the correct join and filter clauses
   */
  @Override
  public String transformHqlQuery(String hql,
      Map<String, String> requestParams,
      Map<String, Object> namedParams) {

    String refInventoryId = requestParams.get("@MaterialMgmtReferencedInventory.id@");

    if (StringUtils.isBlank(refInventoryId)) {
      throw new OBException("Referenced inventory id is mandatory");
    }
    ReferencedInventory ri = OBDal.getInstance()
        .get(ReferencedInventory.class, refInventoryId);
    final String typeId = ri.getReferencedInventoryType().getId();


    // Select the provider with highest priority that supports the type
    Optional<StockSelectorFilterProvider> provider = providers.stream()
        .filter(p -> p.supports(typeId))
        .sorted(Comparator.comparingInt(StockSelectorFilterProvider::getProviderPriority)
            .reversed())
        .findFirst();

    // If no provider matches, use the default provider
    if (provider.isEmpty()) {
      provider = Optional.of(new DefaultBoxFilterProvider());
    }

    // Get the select clause from the provider, or use the default
    String selectClause = provider
        .map(p -> p.getSelectClause(requestParams))
        .orElse(DEFAULT_SELECT_CLAUSE);

    hql = hql.replace("@selectClause@", selectClause);

    // Get the join clause from the provider, or use the default
    String joinClause = provider
        .map(p -> p.getJoinClause(requestParams))
        .orElse(DEFAULT_JOIN_CLAUSE);

    hql = hql.replace("@joinClause@", joinClause);

    // Get the filter clause from the provider, or use the default
    String extraClause = provider
        .map(p -> p.getFilterClause(requestParams))
        .orElse(DEFAULT_FILTER);

    // Replace where clause placeholder with the selected filter clause
    return hql.replace("@whereClause@", extraClause);
  }
}
