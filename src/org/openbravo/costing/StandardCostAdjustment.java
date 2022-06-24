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
 * All portions are Copyright (C) 2014-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

@ComponentProvider.Qualifier("org.openbravo.costing.StandardAlgorithm")
public class StandardCostAdjustment extends CostingAlgorithmAdjustmentImp {

  @Override
  protected void getRelatedTransactionsByAlgorithm() {
    ScrollableResults trxs;
    final MaterialTransaction transaction = getTransaction();

    // Case Inventory Amount Update backdated (modifying the cost in the past)
    if (trxType == TrxType.InventoryOpening) {
      // Search transactions with movement/process date after backdated Inventory Amount Update and
      // before next Inventory Amount Update and create an adjustment line for each transaction
      trxs = getLaterTransactions(transaction);
    }

    // Case transaction backdated (modifying the stock in the past)
    else {
      // Search opening inventories with movement date after backdated transaction and create an
      // adjustment line for any of opening transactions of each Inventory Amount Update
      trxs = getLaterOpeningTransactions(transaction);
    }

    int i = 0;
    try {
      while (trxs.next()) {
        final MaterialTransaction trx = OBDal.getInstance()
            .get(MaterialTransaction.class, trxs.get()[0]);
        BigDecimal adjAmount;

        if (trxType == TrxType.InventoryOpening) {
          final BigDecimal cost = transaction.getPhysicalInventoryLine().getCost();
          adjAmount = trx.getMovementQuantity().abs().multiply(cost).subtract(trx.getTotalCost());
        } else {
          adjAmount = transaction.getMovementQuantity();
        }

        final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
            adjAmount, getCostAdj());
        lineParameters.setRelatedTransactionAdjusted(true);
        insertCostAdjustmentLine(lineParameters);

        i++;
        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      trxs.close();
    }
  }

  @Override
  protected BigDecimal getOutgoingBackdatedTrxAdjAmt(final CostAdjustmentLine costAdjLine) {
    // Calculate the standard cost on the transaction's movement date and adjust the cost if needed.
    final MaterialTransaction trx = costAdjLine.getInventoryTransaction();

    Date trxDate = CostAdjustmentUtils.getLastTrxDateOfMvmntDate(trx.getMovementDate(),
        trx.getProduct(), getCostOrg(), getCostDimensions());
    if (trxDate == null) {
      trxDate = trx.getTransactionProcessDate();
    }

    final BigDecimal cost = CostingUtils.getStandardCost(trx.getProduct(), getCostOrg(), trxDate,
        getCostDimensions(), getCostCurrency());

    final BigDecimal expectedCostAmt = trx.getMovementQuantity().abs().multiply(cost);
    final BigDecimal currentCost = trx.getTransactionCost();
    return expectedCostAmt.subtract(currentCost);
  }

  @Override
  protected void calculateNegativeStockCorrectionAdjustmentAmount(
      final CostAdjustmentLine costAdjLine) {
    // Do nothing
  }

  @Override
  protected void addCostDependingTrx(final CostAdjustmentLine costAdjLine) {
    // Do nothing.
    // All transactions are calculated using the current standard cost so there is no need to
    // specifically search for dependent transactions.
  }

  /**
   * Returns transactions with movement/process date after trx and before next Inventory Amount
   * Update
   */
  private ScrollableResults getLaterTransactions(final MaterialTransaction trx) {

    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    Set<String> orgs = osp.getChildTree(strCostOrgId, true);
    HashMap<CostDimension, BaseOBObject> costDimensions = getCostDimensions();
    if (trx.getProduct().isProduction()) {
      orgs = osp.getChildTree("0", false);
      costDimensions = CostingUtils.getEmptyDimensions();
    }
    final Warehouse warehouse = (Warehouse) costDimensions.get(CostDimension.Warehouse);

    // Get the movement date of the first Inventory Amount Update after trx
    //@formatter:off
    String hqlDateWhere =
            "select trx.movementDate as trxdate" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            "    join trx.physicalInventoryLine as il" +
            "    join il.physInventory as i" +
            " where trx.client.id = :clientId" +
            "   and trx.organization.id in (:orgIds)" +
            "   and trx.product.id = :productId" +
            "   and trx.isCostCalculated = true" +
            "   and trx.movementDate > :date" +
            "   and trx.transactionProcessDate > :startdate" +
            "   and i.inventoryType = 'O'";
    //@formatter:on
    if (warehouse != null) {
      //@formatter:off
      hqlDateWhere +=
            "   and i.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hqlDateWhere +=
            " order by trx.movementDate";
    //@formatter:on

    final Query<Date> dateQry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlDateWhere, Date.class)
        .setParameter("clientId", trx.getClient().getId())
        .setParameterList("orgIds", orgs)
        .setParameter("productId", trx.getProduct().getId())
        .setParameter("date", trx.getMovementDate())
        .setParameter("startdate", startingDate);

    if (warehouse != null) {
      dateQry.setParameter("warehouseId", warehouse.getId());
    }

    final Date date = dateQry.setMaxResults(1).uniqueResult();

    // Get transactions with movement/process date after trx and before next Inventory Amount Update
    // (include closing inventory lines and exclude opening inventory lines of it)
    //@formatter:off
    String hqlWhere =
            "select trx.id as trxid" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            "    join trx.storageBin as l" +
            "    left join trx.physicalInventoryLine as il" +
            "    left join il.physInventory as i" +
            "    left join i.inventoryAmountUpdateLineInventoriesCloseInventoryList as iaui" +
            " where trx.client.id = :clientId" +
            "   and trx.organization.id in (:orgIds)" +
            "   and trx.product.id = :productId" +
            "   and coalesce(iaui.caInventoryamtline.id, '0') <> :inventoryAmountUpdateLineId" +
            "   and trx.isCostCalculated = true" +
            "   and trx.transactionProcessDate > :startdate" +
            "   and coalesce(i.inventoryType, 'N') <> 'O'";
    //@formatter:on
    if (warehouse != null) {
      //@formatter:off
      hqlWhere +=
            "   and l.warehouse.id = :warehouseId";
      //@formatter:on
    }
    if (areBackdatedTrxFixed) {
      //@formatter:off
      hqlWhere +=
            "   and trx.movementDate > :dateFrom";
      //@formatter:on
      if (date != null) {
        //@formatter:off
        hqlWhere +=
            "   and trx.movementDate <= :dateTo";
        //@formatter:on
      }
      //@formatter:off
      hqlWhere +=
            "   order by trx.movementDate";
      //@formatter:on
    } else {
      //@formatter:off
      hqlWhere +=
            "   and case when coalesce(i.inventoryType, 'N') <> 'N' " +
            "       then trx.movementDate " +
            "       else trx.transactionProcessDate " +
            "       end > :dateFrom";
      //@formatter:on
      if (date != null) {
        //@formatter:off
        hqlWhere +=
            "   and case when coalesce(i.inventoryType, 'N') <> 'N' " +
            "       then trx.movementDate " +
            "       else trx.transactionProcessDate " +
            "       end <= :dateTo";
        //@formatter:on
      }
      //@formatter:off
      hqlWhere +=
            " order by trx.transactionProcessDate";
      //@formatter:on
    }

    final Query<String> qry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlWhere, String.class)
        .setParameter("clientId", trx.getClient().getId())
        .setParameterList("orgIds", orgs)
        .setParameter("productId", trx.getProduct().getId())
        .setParameter("inventoryAmountUpdateLineId",
            trx.getPhysicalInventoryLine()
                .getPhysInventory()
                .getInventoryAmountUpdateLineInventoriesInitInventoryList()
                .get(0)
                .getCaInventoryamtline()
                .getId())
        .setParameter("startdate", startingDate);

    if (warehouse != null) {
      qry.setParameter("warehouseId", warehouse.getId());
    }
    qry.setParameter("dateFrom", trx.getMovementDate());
    if (date != null) {
      qry.setParameter("dateTo", date);
    }
    return qry.scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Returns opening physical inventory transactions created by a Inventory Amount Update and
   * created after trx
   */
  private ScrollableResults getLaterOpeningTransactions(final MaterialTransaction trx) {

    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    Set<String> orgs = osp.getChildTree(strCostOrgId, true);
    HashMap<CostDimension, BaseOBObject> costDimensions = getCostDimensions();
    if (trx.getProduct().isProduction()) {
      orgs = osp.getChildTree("0", false);
      costDimensions = CostingUtils.getEmptyDimensions();
    }
    final Warehouse warehouse = (Warehouse) costDimensions.get(CostDimension.Warehouse);

    //@formatter:off
    String hqlWhere =
            "select min(trx.id) as trxid" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            "    join trx.physicalInventoryLine as il" +
            "    join il.physInventory as i" +
            "    join i.inventoryAmountUpdateLineInventoriesInitInventoryList as iaui" +
            " where trx.client.id = :clientId" +
            "   and trx.organization.id in (:orgIds)" +
            "   and trx.product.id = :productId" +
            "   and trx.isCostCalculated = true" +
            "   and trx.movementDate > :date" +
            "   and trx.transactionProcessDate > :startdate" +
            "   and i.inventoryType = 'O'";
    //@formatter:on
    if (warehouse != null) {
      //@formatter:off
      hqlWhere +=
            "   and iaui.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hqlWhere +=
            " group by iaui.caInventoryamtline";
    //@formatter:on
    if (warehouse != null) {
      //@formatter:off
      hqlWhere +=
            "   , iaui.warehouse.id";
      //@formatter:on
    }
    //@formatter:off
    hqlWhere +=
            " order by min(trx.movementDate)";
    //@formatter:on

    final Query<String> qry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlWhere, String.class)
        .setParameter("clientId", trx.getClient().getId())
        .setParameterList("orgIds", orgs)
        .setParameter("productId", trx.getProduct().getId())
        .setParameter("date", trx.getMovementDate())
        .setParameter("startdate", startingDate);

    if (warehouse != null) {
      qry.setParameter("warehouseId", warehouse.getId());
    }
    return qry.scroll(ScrollMode.FORWARD_ONLY);
  }
}
