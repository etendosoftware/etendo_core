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

  private final Instance<StockSelectorFilterProvider> providers;

  @Inject
  public StockSelectorDispatcherHQLTransformer(@Any Instance<StockSelectorFilterProvider> providers) {
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

    // Get the join clause from the provider, or use the default
    String joinClause = provider
        .map(p -> p.getJoinClause(requestParams))
        .orElse(DEFAULT_JOIN_CLAUSE);


    // Get the filter clause from the provider, or use the default
    String extraClause = provider
        .map(p -> p.getFilterClause(requestParams))
        .orElse(DEFAULT_FILTER);

    hql = hql.replace("@joinClause@", joinClause);

    // Replace where clause placeholder with the selected filter clause
    return hql.replace("@whereClause@", extraClause);
  }
}