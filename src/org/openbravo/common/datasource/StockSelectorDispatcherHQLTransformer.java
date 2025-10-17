package org.openbravo.common.datasource;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.materialmgmt.refinventory.ReferencedInventoryUtil;
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

  @Inject
  @Any
  private Instance<StockSelectorFilterProvider> providers;  // All registered filter providers

  /**
   * Transforms the HQL query for the stock selector by injecting the appropriate filter clause.
   * <p>
   * The method determines the referenced inventory type from the request parameters, selects the best matching
   * {@link StockSelectorFilterProvider} (by priority), and applies its filter clause. If no provider matches,
   * the default filter is used.
   *
   * @param hql
   *     the original HQL query containing placeholders
   * @param requestParams
   *     the request parameters, including referenced inventory id
   * @param namedParams
   *     named parameters for the query (not used here)
   * @return the transformed HQL query with the correct filter clause
   */
  @Override
  public String transformHqlQuery(String hql,
      Map<String, String> requestParams,
      Map<String, Object> namedParams) {

    // Determine the referenced inventory type (default if not provided)
    String tmpTypeId = ReferencedInventoryUtil.DEFAULT_REFERENCED_INVENTORY_TYPE;
    String refInventoryId = requestParams.get("@MaterialMgmtReferencedInventory.id@");

    if (!StringUtils.isBlank(refInventoryId)) {
      // Fetch the referenced inventory and get its type
      ReferencedInventory ri = OBDal.getInstance()
          .get(ReferencedInventory.class, refInventoryId);
      tmpTypeId = ri.getReferencedInventoryType().getId();
    }

    final String typeId = tmpTypeId;

    // Select the provider with highest priority that supports the type
    Optional<StockSelectorFilterProvider> provider = providers.stream()
        .filter(p -> p.supports(typeId))
        .sorted(Comparator.comparingInt(StockSelectorFilterProvider::getProviderPriority)
            .reversed())
        .findFirst();

    // Get the filter clause from the provider, or use the default
    String extraClause = provider
        .map(p -> p.getFilterClause(requestParams))
        .orElse(DEFAULT_FILTER);

    // Remove join clause placeholder if present
    if (hql.contains("@joinClause@")) {
      hql = hql.replace("@joinClause@", "");
    }
    // Replace where clause placeholder with the selected filter clause
    return hql.replace("@whereClause@", extraClause);
  }
}