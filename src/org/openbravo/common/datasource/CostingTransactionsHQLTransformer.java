/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.0  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.common.datasource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.query.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.costing.CostAdjustmentUtils;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.service.datasource.hql.HqlQueryTransformer;

@ComponentProvider.Qualifier("DFF0A9F7C26C457FA8735A09ACFD5971")
public class CostingTransactionsHQLTransformer extends HqlQueryTransformer {

  private static final String MOVEMENTTYPE_REF_ID = "189";

  @Override
  public String transformHqlQuery(String hqlQuery, Map<String, String> requestParameters,
      Map<String, Object> queryNamedParameters) {
    // Sets the named parameters

    Set<String> orgs = null;
    Map<CostDimension, BaseOBObject> costDimensions = null;

    final String costingId = requestParameters.get("@MaterialMgmtCosting.id@");
    String transformedHqlQuery = null;

    if (costingId != null && !costingId.equals("null")) {
      Costing costing = OBDal.getInstance().get(Costing.class, costingId);
      MaterialTransaction transaction = costing.getInventoryTransaction();

      if ("AVA".equals(costing.getCostType()) && transaction != null) {

        // Get cost dimensions
        OrganizationStructureProvider osp = OBContext.getOBContext()
            .getOrganizationStructureProvider(transaction.getClient().getId());

        Organization org = OBContext.getOBContext()
            .getOrganizationStructureProvider(transaction.getClient().getId())
            .getLegalEntity(transaction.getOrganization());

        costDimensions = CostingUtils.getEmptyDimensions();

        CostingRule costingRule = CostingUtils.getCostDimensionRule(org,
            transaction.getTransactionProcessDate());

        if (Boolean.TRUE.equals(costing.getProduct().isProduction())) {
          orgs = osp.getChildTree("0", false);
        } else {
          orgs = osp.getChildTree(costing.getOrganization().getId(), true);
          if (Boolean.TRUE.equals(costingRule.isWarehouseDimension())) {
            costDimensions.put(CostDimension.Warehouse, transaction.getStorageBin().getWarehouse());
          }
        }

        Costing prevCosting = getPreviousCosting(transaction, orgs, costDimensions);

        if (prevCosting == null || "AVA".equals(prevCosting.getCostType())) {

          // Transform the query
          String previousCostingCost = addCostOnQuery(prevCosting);
          transformedHqlQuery = hqlQuery.replace("@previousCostingCost@", previousCostingCost);

          String whereClause = getWhereClause(costing, prevCosting, queryNamedParameters, orgs,
              costDimensions);
          transformedHqlQuery = transformedHqlQuery.replace("@whereClause@", whereClause);

          String cumQty = addCumQty(costing, queryNamedParameters, orgs, costDimensions);
          transformedHqlQuery = transformedHqlQuery.replace("@cumQty@", cumQty);

          String cumCost = addCumCost(cumQty, costing, prevCosting);
          transformedHqlQuery = transformedHqlQuery.replace("@cumCost@", cumCost);

          return transformedHqlQuery;
        }
      }
    }

    transformedHqlQuery = hqlQuery.replace("@whereClause@", " 1 = 2 ");
    transformedHqlQuery = transformedHqlQuery.replace("@previousCostingCost@", "0");
    transformedHqlQuery = transformedHqlQuery.replace("@cumQty@", "0");
    transformedHqlQuery = transformedHqlQuery.replace("@cumCost@", "0");
    return transformedHqlQuery;
  }

  /**
   * Returns the Costing record immediately before than the one selected on the parent tab. This
   * record will be the one displayed in the first position in the grid and the one selected in the
   * parent tab will appear in the last position.
   */

  private Costing getPreviousCosting(MaterialTransaction transaction, Set<String> orgs,
      Map<CostDimension, BaseOBObject> costDimensions) {
    //@formatter:off
    String hql =
            "select c.id" +
            "  from MaterialMgmtCosting c " +
            "    join c.inventoryTransaction as trx " +
            "    join trx.storageBin as locator, " +
            "      ADList as trxtype " +
            " where trx.product.id = :productId " +
            "   and trxtype.reference.id = :refid" +
            "   and trxtype.searchKey = trx.movementType" +
            "   and trx.isCostCalculated = true" +
            "   and (trx.movementDate < :movementDate" +
            "     or (trx.movementDate = :movementDate" +
            "       and (trx.transactionProcessDate < :trxProcessDate" +
            "         or (trx.transactionProcessDate = :trxProcessDate" +
            "           and (trxtype.sequenceNumber < :trxtypeprio" +
            "             or (trxtype.sequenceNumber = :trxtypeprio" +
            "               and (trx.movementQuantity > :trxqty" +
            "                 or (trx.movementQuantity = :trxqty" +
            "                   and trx.id <> :trxid" +
            "   )))))))) ";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouse ";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.organization.id in (:orgs) " +
            "   and trx.client.id = :clientId " +
            " order by trx.movementDate desc," +
            "   trx.transactionProcessDate desc," +
            "   c.endingDate desc," +
            "   trx.movementQuantity";
    //@formatter:on

    Query<String> prevCostingQuery = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("productId", transaction.getProduct().getId())
        .setParameter("refid", MOVEMENTTYPE_REF_ID)
        .setParameter("movementDate", transaction.getMovementDate())
        .setParameter("trxProcessDate", transaction.getTransactionProcessDate())
        .setParameter("trxtypeprio",
            CostAdjustmentUtils.getTrxTypePrio(transaction.getMovementType()))
        .setParameter("trxqty", transaction.getMovementQuantity())
        .setParameter("trxid", transaction.getId());

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      prevCostingQuery.setParameter("warehouse",
          costDimensions.get(CostDimension.Warehouse).getId());
    }

    final List<String> preCostingIdList = prevCostingQuery.setParameterList("orgs", orgs)
        .setParameter("clientId", transaction.getClient().getId())
        .setMaxResults(1)
        .list();

    Costing prevCosting = null;
    if (!preCostingIdList.isEmpty()) {
      prevCosting = OBDal.getInstance().get(Costing.class, preCostingIdList.get(0));
      return prevCosting;
    }
    return null;
  }

  /**
   * Implements the where clause of the hql query. With this where clause all transactions that have
   * happened between the costing record selected in the parent Costing tab and the immediate
   * previous costing record will be displayed. It only takes into account transactions that have
   * its cost calculated.
   */

  private String getWhereClause(Costing costing, Costing prevCosting,
      Map<String, Object> queryNamedParameters, Set<String> orgs,
      Map<CostDimension, BaseOBObject> costDimensions) {

    MaterialTransaction transaction = costing.getInventoryTransaction();

    //@formatter:off
    String hql =
            " trx.product.id = c.product.id " +
            " and trxtype.reference.id = :refid" +
            " and trxtype.searchKey = trx.movementType" +
            " and c.id = :costingId " +
            " and trx.isCostCalculated = true" +
            " and ((trx.id = :trxid) ";
    //@formatter:on
    if (prevCosting != null) {
      //@formatter:off
      hql +=
            " or ((( " +
            "   trx.movementDate < trxcosting.movementDate" +
            "   or (trx.movementDate = trxcosting.movementDate" +
            "     and (trx.transactionProcessDate < trxcosting.transactionProcessDate" +
            "       or (trx.transactionProcessDate = trxcosting.transactionProcessDate" +
            "         and (trxtype.sequenceNumber < :trxtypeprio" +
            "           or (trxtype.sequenceNumber = :trxtypeprio" +
            "             and (trx.movementQuantity > :trxqty" +
            "                 or (trx.movementQuantity = :trxqty" +
            "                   and trx.id <> :trxid" +
            " )))))))) " +
            " and (trx.movementDate > :prevCostMovementDate" +
            "   or (trx.movementDate = :prevCostMovementDate" +
            "     and (trx.transactionProcessDate > :prevCostTrxProcessDate" +
            "       or (trx.transactionProcessDate = :prevCostTrxProcessDate" +
            "         and (trxtype.sequenceNumber > :prevtrxtypeprio" +
            "           or (trxtype.sequenceNumber = :prevtrxtypeprio" +
            "             and (trx.movementQuantity < :prevtrxqty" +
            "               or (trx.movementQuantity = :prevtrxqty" +
            "                 and trx.id <> :prevtrxid" +
            " ))))))))) " +
            " or (trx.id = :prevtrxid) " +
            " ) ";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " ) " +
            " and trx.organization.id in (:orgs) " +
            " and trx.client.id = :clientId ";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            " and locator.warehouse.id = :warehouse ";
      //@formatter:on
    }

    queryNamedParameters.put("refid", MOVEMENTTYPE_REF_ID);
    queryNamedParameters.put("costingId", costing.getId());
    queryNamedParameters.put("orgs", orgs);
    queryNamedParameters.put("clientId", costing.getClient().getId());
    queryNamedParameters.put("trxtypeprio",
        CostAdjustmentUtils.getTrxTypePrio(transaction.getMovementType()));
    queryNamedParameters.put("trxqty", transaction.getMovementQuantity());
    queryNamedParameters.put("trxid", transaction.getId());
    if (prevCosting != null) {
      MaterialTransaction prevCostingTrx = prevCosting.getInventoryTransaction();
      queryNamedParameters.put("prevCostMovementDate", prevCostingTrx.getMovementDate());
      queryNamedParameters.put("prevCostTrxProcessDate",
          prevCostingTrx.getTransactionProcessDate());
      queryNamedParameters.put("prevtrxtypeprio",
          CostAdjustmentUtils.getTrxTypePrio(prevCostingTrx.getMovementType()));
      queryNamedParameters.put("prevtrxqty", prevCostingTrx.getMovementQuantity());
      queryNamedParameters.put("prevtrxid", prevCostingTrx.getId());
    }
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      queryNamedParameters.put("warehouse", costDimensions.get(CostDimension.Warehouse).getId());
    }
    return hql;
  }

  /**
   * Returns the cost of the previous costing record if exits, if not, 0 is returned.
   */

  private String addCostOnQuery(Costing prevCosting) {

    if (prevCosting != null) {
      return prevCosting.getCost().toString();
    }
    return "0";
  }

  /**
   * Calculates the quantity of the product on the given date and for the given cost dimensions. It
   * only takes into account transactions that have its cost calculated.
   */

  private String addCumQty(Costing costing, Map<String, Object> queryNamedParameters,
      Set<String> orgs, Map<CostDimension, BaseOBObject> costDimensions) {

    //@formatter:off
    String hql =
            " (select sum(trxCost.movementQuantity)" +
            "\n from MaterialMgmtMaterialTransaction as trxCost" +
            "\n join trxCost.storageBin as locator" +
            "\n , ADList as trxtypeCost" +
            "\n where trxtypeCost.reference.id = :refid" +
            "  and trxtypeCost.searchKey = trxCost.movementType" +
            "   and trxCost.product.id = :productId" +
            "   and trxCost.isCostCalculated = true" +
            "   and (trxCost.movementDate < trx.movementDate" +
            "   or (trxCost.movementDate = trx.movementDate" +
            "    and (trxCost.transactionProcessDate < trx.transactionProcessDate" +
            "     or (trxCost.transactionProcessDate = trx.transactionProcessDate" +
            "      and (trxtypeCost.sequenceNumber < trxtype.sequenceNumber" +
            "       or (trxtypeCost.sequenceNumber = trxtype.sequenceNumber" +
            "        and (trxCost.movementQuantity > trx.movementQuantity" +
            "        or (trxCost.movementQuantity = trx.movementQuantity" +
            "         and trxCost.id <= trx.id" +
            "    ))))))))";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "  and locator.warehouse.id = :warehouse";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " and trxCost.organization.id in (:orgs)" +
            " and trxCost.client.id = :clientId )";
    //@formatter:on

    queryNamedParameters.put("refid", MOVEMENTTYPE_REF_ID);
    queryNamedParameters.put("productId", costing.getProduct().getId());
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      queryNamedParameters.put("warehouse", costDimensions.get(CostDimension.Warehouse).getId());
    }
    queryNamedParameters.put("orgs", orgs);
    queryNamedParameters.put("clientId", costing.getClient().getId());

    return hql;
  }

  /**
   * Returns the cumulative cost of inventory for the product on a certain date. It is calculated
   * based on the previously calculated quantity and the product cost value at that point.
   */
  private String addCumCost(String cumQty, Costing costing, Costing prevCosting) {
    //@formatter:off
    String hql =
            " case when trxcosting.id = trx.id " +
            "   then (" +cumQty + " * " + costing.getCost().toString() + "   ) " +
            "   else ";
    //@formatter:on
    if (prevCosting != null) {
      //@formatter:off
      hql +=
            "   ( " + cumQty + " * " + prevCosting.getCost().toString() + "   ) ";
    //@formatter:on
    } else {
      //@formatter:off
      hql +=
            " 0 ";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " end ";
    //@formatter:on

    return hql;
  }
}
