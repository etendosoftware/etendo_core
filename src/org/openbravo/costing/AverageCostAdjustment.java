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
 * All portions are Copyright (C) 2014-2021 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.client.kernel.ComponentProvider;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.core.TriggerHandler;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;

@ComponentProvider.Qualifier("org.openbravo.costing.AverageAlgorithm")
public class AverageCostAdjustment extends CostingAlgorithmAdjustmentImp {
  private static final String PRICE_DIFFERENCE_CORRECTION_SEARCHKEY = "PDC";
  private static final Logger log = LogManager.getLogger();
  private String bdCostingId;

  @Override
  protected void init(CostAdjustmentLine costAdjLine) {
    super.init(costAdjLine);
    bdCostingId = "";
  }

  @Override
  protected void getRelatedTransactionsByAlgorithm() {
    OBDal.getInstance().flush();
    OBDal.getInstance().getSession().clear();
    // Search all transactions after the date of the adjusted line and recalculate the costs of them
    // to adjust differences
    final MaterialTransaction basetrx = getTransaction();
    // Transactions of closing inventories are managed by generic CostAdjustmentProcess adjusting
    // the cost of the related opening inventory.
    if (basetrx.getPhysicalInventoryLine() != null
        && basetrx.getPhysicalInventoryLine().getRelatedInventory() != null) {
      return;
    }
    final BigDecimal signMultiplier = new BigDecimal(basetrx.getMovementQuantity().signum());
    final Date trxDate = basetrx.getTransactionProcessDate();

    BigDecimal adjustmentBalance = BigDecimal.ZERO;
    BigDecimal negativeStockAdjustmentBalance = BigDecimal.ZERO;

    // Initialize adjustment balance looping through all cost adjustment lines of current
    // transaction.
    log.debug("Initialize adjustment balance");
    final CostAdjustmentLine baseCAL = getCostAdjLine();
    for (CostAdjustmentLine costAdjLine : getTrxAdjustmentLines(basetrx)) {
      BigDecimal adjustmentAmt = costAdjLine.getAdjustmentAmount();
      if (!strCostCurrencyId.equals(costAdjLine.getCurrency().getId()) && adjustmentAmt != null) {
        adjustmentAmt = FinancialUtils.getConvertedAmount(adjustmentAmt, costAdjLine.getCurrency(),
            getCostCurrency(), costAdjLine.getAccountingDate(), getCostOrg(),
            FinancialUtils.PRECISION_STANDARD);
      }

      if (isFromThisCostAdjustment(costAdjLine)) {
        if (costAdjLine.isSource() && !costAdjLine.isRelatedTransactionAdjusted()
            && !costAdjLine.getId().equals(strCostAdjLineId)) {
          searchRelatedTransactionCosts(costAdjLine);
          if (adjustmentAmt == null) {
            adjustmentAmt = costAdjLine.getAdjustmentAmount();
            if (!strCostCurrencyId.equals(costAdjLine.getCurrency().getId())) {
              adjustmentAmt = FinancialUtils.getConvertedAmount(adjustmentAmt,
                  costAdjLine.getCurrency(), getCostCurrency(), costAdjLine.getAccountingDate(),
                  getCostOrg(), FinancialUtils.PRECISION_STANDARD);
            }
          }
        }

        costAdjLine.setRelatedTransactionAdjusted(Boolean.TRUE);
        if (!costAdjLine.getId().equals(strCostAdjLineId)) {
          costAdjLine.setParentCostAdjustmentLine(baseCAL);
        }
        // If the cost adjustment line has Transaction Costs those adjustment amount are included
        // in the Current Value Amount and not in the Adjustment Balance
        if (!costAdjLine.getTransactionCostList().isEmpty()) {
          continue;
        }
        adjustmentBalance = adjustmentBalance.add(adjustmentAmt.multiply(signMultiplier));
      }
      if (costAdjLine.isNegativeStockCorrection()) {
        negativeStockAdjustmentBalance = negativeStockAdjustmentBalance.add(adjustmentAmt);
      }
    }

    // Initialize current stock qty and value amt.
    BigDecimal currentStock = CostAdjustmentUtils.getStockOnTransactionDate(getCostOrg(), basetrx,
        getCostDimensions(), isManufacturingProduct, areBackdatedTrxFixed, getCostCurrency());
    BigDecimal currentValueAmt = CostAdjustmentUtils.getValuedStockOnTransactionDate(getCostOrg(),
        basetrx, getCostDimensions(), isManufacturingProduct, areBackdatedTrxFixed,
        getCostCurrency());
    log.debug(
        "Adjustment balance: " + adjustmentBalance.toPlainString()
            + ", current stock {}, current value {}",
        currentStock.toPlainString(), currentValueAmt.toPlainString());

    // Initialize current unit cost including the cost adjustments.
    final Costing costing = AverageAlgorithm.getProductCost(trxDate, basetrx.getProduct(),
        getCostDimensions(), getCostOrg());
    if (costing == null) {
      throw new OBException(
          "@NoAvgCostDefined@ @Organization@: " + getCostOrg().getName() + ", @Product@: "
              + basetrx.getProduct().getName() + ", @Date@: " + OBDateUtils.formatDate(trxDate));
    }
    BigDecimal cost = null;
    // If current stock is zero the cost is not modified until a related transaction that modifies
    // the stock is found.
    if (currentStock.signum() != 0) {
      cost = currentValueAmt.add(adjustmentBalance)
          .divide(currentStock, costCurPrecission, RoundingMode.HALF_UP);
    }
    log.debug("Starting average cost {}", cost == null ? "not cost" : cost.toPlainString());
    if (AverageAlgorithm.modifiesAverage(trxType) || !baseCAL.isBackdatedTrx()) {
      final BigDecimal trxCost = CostAdjustmentUtils.getTrxCost(basetrx, false, getCostCurrency());
      final BigDecimal trxPrice = getTransactionPrice(
          trxCost.add(adjustmentBalance.multiply(signMultiplier)), negativeStockAdjustmentBalance,
          basetrx.getMovementQuantity());
      if (cost == null) {
        cost = trxPrice;
      }
      if (checkNegativeStockCorrection && currentStock.compareTo(basetrx.getMovementQuantity()) < 0
          && cost.compareTo(trxPrice) != 0 && !baseCAL.isNegativeStockCorrection()
          && AverageAlgorithm.modifiesAverage(trxType)) {
        // stock was negative and cost different than trx price then Negative Stock Correction
        // is added
        final BigDecimal trxSignMultiplier = new BigDecimal(basetrx.getMovementQuantity().signum());
        final BigDecimal negCorrAmt = trxPrice.multiply(currentStock)
            .setScale(stdCurPrecission, RoundingMode.HALF_UP)
            .subtract(currentValueAmt)
            .subtract(adjustmentBalance);
        adjustmentBalance = adjustmentBalance.add(negCorrAmt.multiply(trxSignMultiplier));
        // If there is a difference insert a cost adjustment line.
        final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
            basetrx, negCorrAmt, getCostAdj());
        lineParameters.setNegativeCorrection(true);
        lineParameters.setRelatedTransactionAdjusted(true);
        insertCostAdjustmentLine(lineParameters);
        cost = trxPrice;
        log.debug("Negative stock correction. Amount: {}, new cost {}", negCorrAmt.toPlainString(),
            cost.toPlainString());
      }
      if (basetrx.getMaterialMgmtCostingList().isEmpty()) {
        final Date newDate = basetrx.getTransactionProcessDate();
        final Date dateTo = costing.getEndingDate();
        costing.setEndingDate(newDate);
        final Costing newCosting = OBProvider.getInstance().get(Costing.class);
        newCosting.setCost(cost);
        newCosting.setCurrency(
            (Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, strCostCurrencyId));
        newCosting.setStartingDate(newDate);
        newCosting.setEndingDate(dateTo);
        newCosting.setInventoryTransaction(basetrx);
        newCosting.setProduct(basetrx.getProduct());
        if (isManufacturingProduct) {
          newCosting.setOrganization(
              (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, "0"));
        } else {
          newCosting.setOrganization(
              (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, strCostOrgId));
        }
        newCosting.setQuantity(basetrx.getMovementQuantity());
        newCosting.setTotalMovementQuantity(null);
        newCosting.setTotalStockValuation(null);
        newCosting.setPrice(trxPrice);
        newCosting.setCostType("AVA");
        newCosting.setManual(Boolean.FALSE);
        newCosting.setPermanent(Boolean.TRUE);
        newCosting.setProduction(trxType == TrxType.ManufacturingProduced);
        newCosting.setWarehouse((Warehouse) getCostDimensions().get(CostDimension.Warehouse));
        OBDal.getInstance().save(newCosting);
        OBDal.getInstance().flush();
      } else {
        final Costing curCosting = basetrx.getMaterialMgmtCostingList().get(0);

        if (curCosting.getCost().compareTo(cost) != 0
            || (curCosting.getTotalMovementQuantity() != null
                && curCosting.getTotalMovementQuantity().compareTo(currentStock) != 0)
            || (curCosting.getTotalStockValuation() != null && curCosting.getTotalStockValuation()
                .compareTo(currentValueAmt.add(adjustmentBalance)) != 0)) {
          updateCosting(curCosting, cost, trxPrice);
        }
      }
    }

    // Modify isManufacturingProduct flag in case it has changed at some point.
    isManufacturingProduct = (costing.getOrganization().getId()).equals("0");

    final ScrollableResults trxs = getRelatedTransactions();
    String strCurrentCurId = strCostCurrencyId;
    boolean forceExit = false;
    try {
      while (trxs.next() && !forceExit) {
        final MaterialTransaction trx = (MaterialTransaction) trxs.get()[0];
        log.debug("Process related transaction {}", trx.getIdentifier());
        final BigDecimal trxSignMultiplier = new BigDecimal(trx.getMovementQuantity().signum());
        // trxAdjAmt: Sum of cost adjustment lines that still do not have transaction cost.
        BigDecimal trxAdjAmt = BigDecimal.ZERO;
        BigDecimal trxUnitCostAdjAmt = BigDecimal.ZERO;
        BigDecimal trxNegativeStockAdjAmt = BigDecimal.ZERO;
        if (StringUtils.isNotEmpty(bdCostingId) && !isBackdatedTransaction(trx)) {
          // If there is a backdated source adjustment pending modify the dates of its m_costing.
          updateBDCostingTimeRange(trx);
          // This update is done only on the first related transaction.
          bdCostingId = "";
        }

        if (!strCurrentCurId.equals(trx.getCurrency().getId())) {
          final Currency curCurrency = OBDal.getInstance().get(Currency.class, strCurrentCurId);
          final Organization costOrg = getCostOrg();

          currentValueAmt = FinancialUtils.getConvertedAmount(currentValueAmt, curCurrency,
              trx.getCurrency(), trx.getMovementDate(), costOrg, FinancialUtils.PRECISION_STANDARD);
          if (cost != null) {
            cost = FinancialUtils.getConvertedAmount(cost, curCurrency, trx.getCurrency(),
                trx.getMovementDate(), costOrg, FinancialUtils.PRECISION_COSTING);
          }

          strCurrentCurId = trx.getCurrency().getId();
        }

        final List<CostAdjustmentLine> existingAdjLines = getTrxAdjustmentLines(trx);
        for (CostAdjustmentLine existingCAL : existingAdjLines) {
          BigDecimal adjustmentAmt = existingCAL.getAdjustmentAmount();
          if (!strCurrentCurId.equals(existingCAL.getCurrency().getId()) && adjustmentAmt != null) {
            final Currency curCurrency = OBDal.getInstance().get(Currency.class, strCurrentCurId);
            adjustmentAmt = FinancialUtils.getConvertedAmount(adjustmentAmt,
                existingCAL.getCurrency(), curCurrency, existingCAL.getAccountingDate(),
                getCostOrg(), FinancialUtils.PRECISION_STANDARD);
          }
          if (isFromThisCostAdjustment(existingCAL)) {
            if (existingCAL.isSource() && !existingCAL.isRelatedTransactionAdjusted()) {
              searchRelatedTransactionCosts(existingCAL);
              if (adjustmentAmt == null) {
                adjustmentAmt = existingCAL.getAdjustmentAmount();
                if (!strCurrentCurId.equals(existingCAL.getCurrency().getId())) {
                  final Currency curCurrency = OBDal.getInstance()
                      .get(Currency.class, strCurrentCurId);
                  adjustmentAmt = FinancialUtils.getConvertedAmount(adjustmentAmt,
                      existingCAL.getCurrency(), curCurrency, existingCAL.getAccountingDate(),
                      getCostOrg(), FinancialUtils.PRECISION_STANDARD);
                }
              }
            }
            if (existingCAL.getTransactionCostList().isEmpty()) {
              trxAdjAmt = trxAdjAmt.add(adjustmentAmt);
              if (existingCAL.isUnitCost()) {
                trxUnitCostAdjAmt = trxUnitCostAdjAmt.add(adjustmentAmt);
              }
              adjustmentBalance = adjustmentBalance.add(adjustmentAmt.multiply(trxSignMultiplier));
            }

            existingCAL.setRelatedTransactionAdjusted(Boolean.TRUE);
            existingCAL.setParentCostAdjustmentLine((CostAdjustmentLine) OBDal.getInstance()
                .getProxy(CostAdjustmentLine.ENTITY_NAME, strCostAdjLineId));
          }
          if (existingCAL.isNegativeStockCorrection()) {
            trxNegativeStockAdjAmt = trxNegativeStockAdjAmt.add(adjustmentAmt);
          }
        }
        log.debug("Current trx adj amount of existing CALs {}", trxAdjAmt.toPlainString());

        final BigDecimal trxCost = CostAdjustmentUtils.getTrxCost(trx, false,
            OBDal.getInstance().get(Currency.class, strCurrentCurId));
        final BigDecimal trxUnitCost = CostAdjustmentUtils.getTrxCost(trx, true,
            OBDal.getInstance().get(Currency.class, strCurrentCurId));
        currentValueAmt = currentValueAmt.add(trxCost.multiply(trxSignMultiplier));
        currentStock = currentStock.add(trx.getMovementQuantity());
        log.debug("Updated current stock {} and, current value {}", currentStock.toPlainString(),
            currentValueAmt.toPlainString());

        final TrxType currentTrxType = TrxType.getTrxType(trx);

        if (AverageAlgorithm.modifiesAverage(currentTrxType)) {
          // Recalculate average, if current stock is zero the average is not modified
          if (currentStock.signum() != 0) {
            if (modifiesAverageCostAndUsesActualAverageCost(trx) && !trx.isCostPermanent()) {
              if (cost == null) {
                cost = currentValueAmt.add(adjustmentBalance)
                    .divide(currentStock, costCurPrecission, RoundingMode.HALF_UP);
              }
              // Check current trx unit cost matches new expected cost
              final BigDecimal expectedCost = cost.multiply(trx.getMovementQuantity().abs())
                  .setScale(stdCurPrecission, RoundingMode.HALF_UP);
              BigDecimal unitCost = trxUnitCost;
              unitCost = unitCost.add(trxUnitCostAdjAmt);
              log.debug("Is adjustment needed? Expected {} vs Current {}",
                  expectedCost.toPlainString(), unitCost.toPlainString());
              BigDecimal unitCostDifference = expectedCost.subtract(unitCost);
              if (unitCostDifference.signum() != 0) {
                trxAdjAmt = trxAdjAmt.add(unitCostDifference);
                trxUnitCostAdjAmt = trxUnitCostAdjAmt.add(unitCostDifference);
                adjustmentBalance = adjustmentBalance
                    .add(unitCostDifference.multiply(trxSignMultiplier));
                // If there is a difference insert a cost adjustment line.
                final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
                    trx, unitCostDifference, getCostAdj());
                lineParameters.setRelatedTransactionAdjusted(true);
                insertCostAdjustmentLine(lineParameters);
                log.debug("Adjustment added. Amount {}.", unitCostDifference.toPlainString());
              }

            }

            cost = currentValueAmt.add(adjustmentBalance)
                .divide(currentStock, costCurPrecission, RoundingMode.HALF_UP);

          }
          if (cost == null) {
            continue;
          }
          log.debug("New average cost: {}", cost.toPlainString());
          Costing curCosting = trx.getMaterialMgmtCostingList().get(0);
          BigDecimal trxPrice = getTransactionPrice(trxCost.add(trxAdjAmt), trxNegativeStockAdjAmt,
              trx.getMovementQuantity());

          if (checkNegativeStockCorrection && currentStock.compareTo(trx.getMovementQuantity()) < 0
              && cost.compareTo(trxPrice) != 0) {
            // stock was negative and cost different than trx price then Negative Stock Correction
            // is added
            BigDecimal negCorrAmt = trxPrice.multiply(currentStock)
                .setScale(stdCurPrecission, RoundingMode.HALF_UP)
                .subtract(currentValueAmt)
                .subtract(adjustmentBalance);
            adjustmentBalance = adjustmentBalance.add(negCorrAmt.multiply(trxSignMultiplier));
            // If there is a difference insert a cost adjustment line.
            final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
                trx, negCorrAmt, getCostAdj());
            lineParameters.setNegativeCorrection(true);
            lineParameters.setRelatedTransactionAdjusted(true);
            insertCostAdjustmentLine(lineParameters);
            cost = trxPrice;
            log.debug("Negative stock correction. Amount: {}, new cost {}",
                negCorrAmt.toPlainString(), cost.toPlainString());
          } else if (checkNegativeStockCorrection
              && currentStock.compareTo(trx.getMovementQuantity()) >= 0) {
            List<CostAdjustmentLine> costAdjustmentLineList = getNegativeStockAdjustments(trx);
            BigDecimal revertedNegativeAdjustment = BigDecimal.ZERO;
            if (!costAdjustmentLineList.isEmpty()) {
              for (CostAdjustmentLine costAdjustmentLine : costAdjustmentLineList) {
                revertedNegativeAdjustment = revertedNegativeAdjustment
                    .add(costAdjustmentLine.getAdjustmentAmount().negate());
              }
              adjustmentBalance = adjustmentBalance.add(revertedNegativeAdjustment);
              // If there is a difference insert a cost adjustment line.
              final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
                  trx, revertedNegativeAdjustment, getCostAdj());
              lineParameters.setNegativeCorrection(true);
              lineParameters.setRelatedTransactionAdjusted(true);
              insertCostAdjustmentLine(lineParameters);
              if (currentStock.signum() != 0) {
                cost = currentValueAmt.add(adjustmentBalance)
                    .divide(currentStock, costCurPrecission, RoundingMode.HALF_UP);
                log.debug("Revert Negative stock correction. Amount: {}, new cost {}",
                    revertedNegativeAdjustment.toPlainString(), cost.toPlainString());
              }
            }
          }

          if (curCosting.getCost().compareTo(cost) == 0 && StringUtils.isEmpty(bdCostingId)
              && (curCosting.getTotalMovementQuantity() != null
                  && curCosting.getTotalMovementQuantity().compareTo(currentStock) == 0)
              && (curCosting.getTotalStockValuation() != null && curCosting.getTotalStockValuation()
                  .compareTo(currentValueAmt.add(adjustmentBalance)) == 0)) {
            // new cost hasn't changed and total movement qty is equal to current stock, following
            // transactions will have the same cost, so no more
            // related transactions are needed to include.
            // If bdCosting is not empty it is needed to loop through the next related transaction
            // to set the new time ringe of the costing.
            log.debug("New cost matches existing cost. Adjustment finished.");
            forceExit = true;
          } else {
            updateCosting(curCosting, cost, trxPrice);
          }
        } else if (cost != null && !isVoidedTrx(trx, currentTrxType)) {
          if (!trx.isCostPermanent()) {
            // Check current trx unit cost matches new expected cost
            BigDecimal expectedCost = cost.multiply(trx.getMovementQuantity().abs())
                .setScale(stdCurPrecission, RoundingMode.HALF_UP);
            BigDecimal unitCost = CostAdjustmentUtils.getTrxCost(trx, true,
                OBDal.getInstance().get(Currency.class, strCurrentCurId));
            unitCost = unitCost.add(trxUnitCostAdjAmt);
            log.debug("Is adjustment needed? Expected {} vs Current {}",
                expectedCost.toPlainString(), unitCost.toPlainString());
            if (expectedCost.compareTo(unitCost) != 0) {
              BigDecimal newAdjAmt = expectedCost.subtract(unitCost);
              trxAdjAmt = trxAdjAmt.add(newAdjAmt);
              trxUnitCostAdjAmt = trxUnitCostAdjAmt.add(newAdjAmt);
              adjustmentBalance = adjustmentBalance.add(newAdjAmt.multiply(trxSignMultiplier));
              // If there is a difference insert a cost adjustment line.
              final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
                  trx, newAdjAmt, getCostAdj());
              lineParameters.setRelatedTransactionAdjusted(true);
              insertCostAdjustmentLine(lineParameters);
              log.debug("Adjustment added. Amount {}.", newAdjAmt.toPlainString());
            }
          }
          if (!trx.getMaterialMgmtCostingList().isEmpty()) {
            Costing curCosting = trx.getMaterialMgmtCostingList().get(0);
            if (currentStock.signum() != 0) {
              cost = currentValueAmt.add(adjustmentBalance)
                  .divide(currentStock, costCurPrecission, RoundingMode.HALF_UP);
            }
            BigDecimal trxPrice = getTransactionPrice(trxCost.add(trxAdjAmt),
                trxNegativeStockAdjAmt, trx.getMovementQuantity());
            if (curCosting.getCost().compareTo(cost) != 0
                || (curCosting.getTotalMovementQuantity() != null
                    && curCosting.getTotalMovementQuantity().compareTo(currentStock) != 0)
                || (curCosting.getTotalStockValuation() != null
                    && curCosting.getTotalStockValuation()
                        .compareTo(currentValueAmt.add(adjustmentBalance)) != 0)) {
              updateCosting(curCosting, cost, trxPrice);
            }
          }
        }
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
    } finally {
      trxs.close();
    }

    if (getCostingRule().getEndingDate() == null && cost != null) {
      // This is the current costing rule. Check if current average cost needs to be updated.
      Costing currentCosting = AverageAlgorithm.getProductCost(new Date(), basetrx.getProduct(),
          getCostDimensions(), getCostOrg());
      if (currentCosting == null) {
        throw new OBException("@NoAvgCostDefined@ @Organization@: " + getCostOrg().getName()
            + ", @Product@: " + basetrx.getProduct().getName() + ", @Date@: "
            + OBDateUtils.formatDate(new Date()));
      }

      if (currentCosting.getCost().compareTo(cost) != 0) {
        // Update existing costing
        TriggerHandler.getInstance().disable();
        if (currentCosting.getOriginalCost() == null) {
          currentCosting.setOriginalCost(currentCosting.getCost());
        }
        currentCosting.setPrice(cost);
        currentCosting.setCost(cost);
        currentCosting.setTotalMovementQuantity(null);
        currentCosting.setTotalStockValuation(null);
        currentCosting.setManual(Boolean.FALSE);
        currentCosting.setPermanent(Boolean.TRUE);
        TriggerHandler.getInstance().enable();
      }
    }
  }

  /**
   * Calculates the price per unit in the cost currency precision of the MaterialTransaction
   * considering any adjustment made to it except the NegativeStock corrections.
   * 
   * @param trxCost
   *          the total TransactionCost considering all adjustments.
   * @param trxNegativeStockAdjAmt
   *          the total NegativeStock adjustments.
   * @param movementQuantity
   *          the MovementQuantity of the transaction.
   * @return The transaction price
   */
  private BigDecimal getTransactionPrice(BigDecimal trxCost, BigDecimal trxNegativeStockAdjAmt,
      BigDecimal movementQuantity) {
    BigDecimal trxPrice = null;
    if (movementQuantity.signum() == 0) {
      trxPrice = BigDecimal.ZERO;
    } else {
      trxPrice = trxCost.subtract(trxNegativeStockAdjAmt)
          .divide(movementQuantity.abs(), costCurPrecission, RoundingMode.HALF_UP);
    }
    return trxPrice;
  }

  private boolean isFromThisCostAdjustment(CostAdjustmentLine existingCAL) {
    return existingCAL.getCostAdjustment().getId().equals(getCostAdj().getId());
  }

  private void updateCosting(Costing curCosting, BigDecimal cost, BigDecimal trxPrice) {
    // Update existing costing
    TriggerHandler.getInstance().disable();
    if (curCosting.getCost().compareTo(cost) != 0) {
      if (curCosting.getOriginalCost() == null) {
        curCosting.setOriginalCost(curCosting.getCost());
      }
      curCosting.setCost(cost);
      curCosting.setPrice(trxPrice);
    }
    curCosting.setTotalMovementQuantity(null);
    curCosting.setTotalStockValuation(null);
    TriggerHandler.getInstance().enable();
  }

  private boolean modifiesAverageCostAndUsesActualAverageCost(MaterialTransaction trx) {
    TrxType currentTrxType = TrxType.getTrxType(trx);
    switch (currentTrxType) {
      case InventoryIncrease:
      case InventoryOpening:
        return trx.getPhysicalInventoryLine().getCost() == null;
      case Receipt:
        return trx.getGoodsShipmentLine().getSalesOrderLine() == null && getProductCost(trx) != null
            && !isReceiptAdjustedByPriceDifferenceCorrection();
      case ShipmentNegative:
      case InternalConsNegative:
        return getProductCost(trx) != null;
      case ShipmentReturn:
        return true;
      default:
        return false;
    }
  }

  private boolean isReceiptAdjustedByPriceDifferenceCorrection() {
    //@formatter:off
    final String hql =
            "as ca" +
            "  join ca.costAdjustmentLineList as cal" +
            " where ca.sourceProcess = :priceDifferenceCorrection" +
            "   and cal.inventoryTransaction = :trx";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(CostAdjustment.class, hql)
        .setNamedParameter("priceDifferenceCorrection", PRICE_DIFFERENCE_CORRECTION_SEARCHKEY)
        .setNamedParameter("trx", getTransaction())
        .setMaxResult(1)
        .uniqueResult() != null;
  }

  private Costing getProductCost(MaterialTransaction trx) {
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider();
    Organization legalEntity = osp.getLegalEntity(trx.getOrganization());

    return AverageAlgorithm.getProductCost(trx.getTransactionProcessDate(), trx.getProduct(),
        getCostDimensions(), legalEntity);
  }

  @Override
  protected void calculateBackdatedTrxAdjustment(CostAdjustmentLine costAdjLine) {
    MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    TrxType calTrxType = TrxType.getTrxType(trx);
    if (AverageAlgorithm.modifiesAverage(calTrxType)) {
      // The bdCosting average related to the backdated transaction needs to be moved to its correct
      // date range, the last costing is the average cost that currently finishes when the costing
      // that needs to be moved starts. The "lastCosting" ending date needs to be updated to end in
      // the same date than the backdated costing so there is no gap between average costs.
      // The bdCosting dates are updated later when the first related transaction is checked.
      Costing bdCosting = trx.getMaterialMgmtCostingList().get(0);
      extendPreviousCosting(bdCosting);
    }
    super.calculateBackdatedTrxAdjustment(costAdjLine);
  }

  @Override
  protected BigDecimal getOutgoingBackdatedTrxAdjAmt(CostAdjustmentLine costAdjLine) {
    // Calculate the average cost on the transaction's movement date and adjust the cost if needed.
    MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    Costing costing = getAvgCostOnMovementDate(trx, getCostDimensions(), getCostOrg(),
        areBackdatedTrxFixed);

    if (costing == null) {
      // In case the backdated transaction is on a date where the stock was not initialized there
      // isn't any costing entry related to an inventory transaction which results in a null
      // costing.
      // Try again with average algorithm getProductCost method using the movement date as
      // parameter.
      costing = AverageAlgorithm.getProductCost(trx.getMovementDate(), trx.getProduct(),
          getCostDimensions(), getCostOrg());
    }

    if (costing == null) {
      String errorMessage = OBMessageUtils.parseTranslation("@NoAvgCostDefined@ @Organization@: "
          + getCostOrg().getName() + ", @Product@: " + trx.getProduct().getName() + ", @Date@: "
          + OBDateUtils.formatDate(trx.getMovementDate()));
      throw new OBException(errorMessage);
    }
    BigDecimal cost = costing.getCost();
    Currency costCurrency = getCostCurrency();
    if (costing.getCurrency() != costCurrency) {
      cost = FinancialUtils.getConvertedAmount(costing.getCost(), costing.getCurrency(),
          costCurrency, trx.getTransactionProcessDate(), getCostOrg(),
          FinancialUtils.PRECISION_COSTING);
    }
    BigDecimal expectedCostAmt = trx.getMovementQuantity()
        .abs()
        .multiply(cost)
        .setScale(stdCurPrecission, RoundingMode.HALF_UP);
    final BigDecimal currentCostWithAdjustments = CostAdjustmentUtils.getTrxCost(trx, true,
        getCostCurrency());
    return expectedCostAmt.subtract(currentCostWithAdjustments);
  }

  @Override
  protected BigDecimal getDefaultCostDifference(TrxType calTrxType,
      CostAdjustmentLine costAdjLine) {
    MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    Costing costing = getAvgCostOnMovementDate(trx, getCostDimensions(), getCostOrg(),
        areBackdatedTrxFixed);
    if (costing == null) {
      // In case the backdated transaction is on a date where the stock was not initialized there
      // isn't any costing entry related to an inventory transaction which results in a null
      // costing. Try again with average algorithm getProductCost method using the movement date as
      // parameter.
      costing = AverageAlgorithm.getProductCost(trx.getMovementDate(), trx.getProduct(),
          getCostDimensions(), getCostOrg());
    }
    if (costing != null) {
      BigDecimal defaultCost = costing.getCost();
      Currency costCurrency = getCostCurrency();
      if (costing.getCurrency() != costCurrency) {
        defaultCost = FinancialUtils.getConvertedAmount(costing.getCost(), costing.getCurrency(),
            costCurrency, trx.getTransactionProcessDate(), getCostOrg(),
            FinancialUtils.PRECISION_COSTING);
      }
      BigDecimal trxCalculatedCost = CostAdjustmentUtils.getTrxCost(trx, true, getCostCurrency());
      defaultCost = trx.getMovementQuantity()
          .abs()
          .multiply(defaultCost)
          .setScale(stdCurPrecission, RoundingMode.HALF_UP);
      return defaultCost.subtract(trxCalculatedCost);
    }
    return super.getDefaultCostDifference(calTrxType, costAdjLine);
  }

  private ScrollableResults getRelatedTransactions() {
    CostingRule costingRule = getCostingRule();
    HashMap<CostDimension, BaseOBObject> costDimensions = getCostDimensions();
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(costingRule.getClient().getId());
    Set<String> orgs = osp.getChildTree(strCostOrgId, true);
    if (isManufacturingProduct) {
      orgs = osp.getChildTree("0", false);
      costDimensions = CostingUtils.getEmptyDimensions();
    }
    MaterialTransaction trx = getTransaction();

    //@formatter:off
    String hql =
            "as trx";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "  join trx.storageBin as loc";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "  ,ADList as trxtype" +
            " where trxtype.reference.id = :refid" +
            "   and trxtype.searchKey = trx.movementType" +
            "   and trx.isCostCalculated = true" +
            "   and trx.product = :product" +
            "   and (";
    //@formatter:on

    // Consider only transactions with movement date equal or later than the movement date of the
    // adjusted transaction. But for transactions with the same movement date only those with a
    // transaction date after the process date of the adjusted transaction.
    if (costingRule.isBackdatedTransactionsFixed()) {
      //@formatter:off
      hql +=
            "   (trx.transactionProcessDate < :fixbdt" +
            "   and (";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   trx.transactionProcessDate > :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber > :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity < :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id > :trxid" +
            "   )))))";
    //@formatter:on

    if (costingRule.isBackdatedTransactionsFixed()) {
      // If there are more than one trx on the same trx process date filter out those types with
      // less priority and / or higher quantity.
      //@formatter:off
      hql +=
            "   ) or (trx.transactionProcessDate >= :fixbdt" +
            "   and (trx.movementDate > :mvtdate" +
            "   or (trx.movementDate = :mvtdate" +
            "   and (trx.transactionProcessDate > :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber > :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity < :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id > :trxid" +
            "   )))))" +
            "   ))))";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.organization.id in (:orgs)";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and loc.warehouse.id = :warehouse";
      //@formatter:on
    }
    if (costingRule.getEndingDate() != null) {
      //@formatter:off
      hql +=
            "   and trx.transactionProcessDate <= :enddate";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.transactionProcessDate > :startdate " +
            " order by ";
    //@formatter:on
    if (areBackdatedTrxFixed) {
      //@formatter:off
      hql +=
            "   trx.movementDate, " ;
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   trx.transactionProcessDate" +
            "   , trxtype.sequenceNumber" +
            "   , trx.movementQuantity desc" +
            "   , trx.id";
    //@formatter:on

    final OBQuery<MaterialTransaction> trxQry = OBDal.getInstance()
        .createQuery(MaterialTransaction.class, hql)
        .setFilterOnReadableOrganization(false)
        .setFilterOnReadableClients(false)
        .setNamedParameter("refid", CostAdjustmentUtils.MovementTypeRefID)
        .setNamedParameter("product", trx.getProduct());

    if (costingRule.isBackdatedTransactionsFixed()) {
      trxQry.setNamedParameter("mvtdate", trx.getMovementDate())
          .setNamedParameter("fixbdt", CostingUtils.getCostingRuleFixBackdatedFrom(costingRule));
    }
    trxQry
        .setNamedParameter("trxtypeprio", CostAdjustmentUtils.getTrxTypePrio(trx.getMovementType()))
        .setNamedParameter("trxdate", trx.getTransactionProcessDate())
        .setNamedParameter("trxqty", trx.getMovementQuantity())
        .setNamedParameter("trxid", trx.getId())
        .setNamedParameter("orgs", orgs);

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setNamedParameter("warehouse", costDimensions.get(CostDimension.Warehouse).getId());
    }
    if (costingRule.getEndingDate() != null) {
      trxQry.setNamedParameter("enddate", costingRule.getEndingDate());
    }

    return trxQry
        .setNamedParameter("startdate", CostingUtils.getCostingRuleStartingDate(costingRule))
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  /**
   * Get all Cost Adjustment Lines related to the MaterialTransaction. Including those from other
   * Cost Adjustments
   */
  private List<CostAdjustmentLine> getTrxAdjustmentLines(MaterialTransaction trx) {
    OBCriteria<CostAdjustmentLine> critLines = OBDal.getInstance()
        .createCriteria(CostAdjustmentLine.class);
    critLines.createAlias(CostAdjustmentLine.PROPERTY_COSTADJUSTMENT, "ca");
    critLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION, trx));
    critLines.add(Restrictions.or(//
        Restrictions.eq("ca.id", getCostAdj().getId()), //
        Restrictions.not(Restrictions.eq("ca." + CostAdjustment.PROPERTY_DOCUMENTSTATUS, "DR"))));

    return critLines.list();
  }

  @Override
  protected void calculateNegativeStockCorrectionAdjustmentAmount(CostAdjustmentLine costAdjLine) {
    MaterialTransaction basetrx = costAdjLine.getInventoryTransaction();
    boolean areBaseTrxBackdatedFixed = getCostingRule().isBackdatedTransactionsFixed()
        && !CostingUtils.getCostingRuleFixBackdatedFrom(getCostingRule())
            .before(basetrx.getTransactionProcessDate());
    BigDecimal currentStock = CostAdjustmentUtils.getStockOnTransactionDate(getCostOrg(), basetrx,
        getCostDimensions(), isManufacturingProduct, areBaseTrxBackdatedFixed, getCostCurrency());
    BigDecimal currentValueAmt = CostAdjustmentUtils.getValuedStockOnTransactionDate(getCostOrg(),
        basetrx, getCostDimensions(), isManufacturingProduct, areBaseTrxBackdatedFixed,
        getCostCurrency());

    Costing curCosting = basetrx.getMaterialMgmtCostingList().get(0);
    BigDecimal trxPrice = curCosting.getPrice();
    BigDecimal adjustAmt = currentStock.multiply(trxPrice)
        .setScale(stdCurPrecission, RoundingMode.HALF_UP)
        .subtract(currentValueAmt);

    costAdjLine.setCurrency(
        (Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, strCostCurrencyId));
    costAdjLine.setAdjustmentAmount(adjustAmt);
    OBDal.getInstance().save(costAdjLine);
  }

  /**
   * Calculates the average cost value of the transaction.
   */
  protected static Costing getAvgCostOnMovementDate(MaterialTransaction trx,
      HashMap<CostDimension, BaseOBObject> costDimensions, Organization costOrg,
      boolean areBackdatedTrxFixed) {

    // Get child tree of organizations.
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(costOrg.getClient().getId());
    Set<String> orgs = osp.getChildTree(costOrg.getId(), true);

    //@formatter:off
    String hql =
            "as c" +
            "  join c.inventoryTransaction as trx";
    //@formatter:on

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "  join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "  , ADList as trxtype" +
            " where trxtype.reference.id = :refid" +
            "   and trxtype.searchKey = trx.movementType" +
            "   and trx.product.id = :product";
    //@formatter:on
    if (areBackdatedTrxFixed) {
      //@formatter:off
      hql +=
            "   and (trx.movementDate < :mvtdate" +
            "   or (trx.movementDate = :mvtdate";
      //@formatter:on
    }
    // If there are more than one trx on the same trx process date filter out those types with less
    // priority and / or higher quantity.
    //@formatter:off
    hql +=
            "   and (trx.transactionProcessDate < :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber < :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity >= :trxqty" +
            "   ))))";
    
    //@formatter:on
    if (areBackdatedTrxFixed) {
      //@formatter:off
      hql +=
            "  ))";
      //@formatter:on
    }

    // Include only transactions that have its cost calculated
    //@formatter:off
    hql += "  and trx.isCostCalculated = true";
    //@formatter:on

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouse";
      //@formatter:on

    }
    //@formatter:off
    hql +=
            "   and trx.organization.id in (:orgs)" +
            " order by ";
    //@formatter:on
    if (areBackdatedTrxFixed) {
      //@formatter:off
      hql +=
            "   trx.movementDate desc, ";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   trx.transactionProcessDate desc " +
            "   , trxtype.sequenceNumber desc " +
            "   , trx.movementQuantity" +
            "   , trx.id" ;
    //@formatter:on

    OBQuery<Costing> qryCost = OBDal.getInstance()
        .createQuery(Costing.class, hql)
        .setNamedParameter("refid", CostAdjustmentUtils.MovementTypeRefID)
        .setNamedParameter("product", trx.getProduct().getId())
        .setNamedParameter("mvtdate", trx.getMovementDate())
        .setNamedParameter("trxdate", trx.getTransactionProcessDate())
        .setNamedParameter("trxtypeprio", CostAdjustmentUtils.getTrxTypePrio(trx.getMovementType()))
        .setNamedParameter("trxqty", trx.getMovementQuantity());

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      qryCost.setNamedParameter("warehouse", costDimensions.get(CostDimension.Warehouse).getId());
    }

    return qryCost.setNamedParameter("orgs", orgs).setMaxResult(1).uniqueResult();
  }

  /**
   * Extends the Average Costing ending date to include the time range that leaves the given
   * backdated average costing when this is moved to the correct time range.
   * 
   * It stored the backdated costing id in a local field to be updated with the new time range when
   * the next related transaction is processed.
   * 
   * @param bdCosting
   *          the backdated costing
   */
  private void extendPreviousCosting(Costing bdCosting) {
    //@formatter:off
    String hql =
            "as c" +
            "  left join c.inventoryTransaction as trx" +
            " where c.product = :product";
    //@formatter:on
    // FIXME: remove when manufacturing costs are fully migrated
    if (bdCosting.getProduct().isProduction()) {
      //@formatter:off
      hql +=
            "   and c.client = :client";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and c.organization = :org";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and c.costType = 'AVA'";
    //@formatter:on
    if (bdCosting.getWarehouse() == null) {
      //@formatter:off
      hql +=
            "   and c.warehouse is null";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and c.warehouse = :warehouse";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and c.endingDate <= :endDate" +
            " order by " +
            "   trx.movementDate desc, " +
            "   trx.transactionProcessDate desc," +
            "   c.endingDate desc";
    //@formatter:on

    final OBQuery<Costing> qryCosting = OBDal.getInstance()
        .createQuery(Costing.class, hql)
        .setNamedParameter("product", bdCosting.getProduct());
    // FIXME: remove when manufacturing costs are fully migrated
    if (bdCosting.getProduct().isProduction()) {
      qryCosting.setNamedParameter("client", bdCosting.getClient());
    } else {
      qryCosting.setNamedParameter("org", bdCosting.getOrganization());
    }
    if (bdCosting.getWarehouse() != null) {
      qryCosting.setNamedParameter("warehouse", bdCosting.getWarehouse());
    }

    final Costing lastCosting = qryCosting.setNamedParameter("endDate", bdCosting.getStartingDate())
        .setFetchSize(1)
        .setMaxResult(1)
        .uniqueResult();

    bdCostingId = bdCosting.getId();
    lastCosting.setEndingDate(bdCosting.getEndingDate());
    OBDal.getInstance().save(lastCosting);
  }

  /**
   * Updates the backdated average costing time range.
   * 
   * <br>
   * The starting date of the bdCosting is the transaction process date of the given trx. The ending
   * date is defined by the average costing that is being shortened.
   * 
   * @param trx
   *          The material transaction that is used as a reference to set the new time range of the
   *          backdated average costing.
   */
  private void updateBDCostingTimeRange(MaterialTransaction trx) {
    Costing bdCosting = OBDal.getInstance().get(Costing.class, bdCostingId);
    bdCosting.setPermanent(Boolean.FALSE);
    OBDal.getInstance().save(bdCosting);
    // Fire trigger to allow to modify the average cost and starting date.
    OBDal.getInstance().flush();

    Costing curCosting = getTrxCurrentCosting(trx);
    if (curCosting != null) {
      bdCosting.setEndingDate(curCosting.getEndingDate());
      curCosting.setEndingDate(trx.getTransactionProcessDate());
      OBDal.getInstance().save(curCosting);
    } else {
      // There isn't any previous costing.
      bdCosting.setEndingDate(trx.getTransactionProcessDate());

    }
    bdCosting.setStartingDate(trx.getTransactionProcessDate());
    bdCosting.setPermanent(Boolean.TRUE);
    OBDal.getInstance().save(bdCosting);
  }

  /**
   * Returns the average costing that is valid on the given transaction process date.
   * 
   * @param trx
   *          MaterialTransaction to be used as time reference.
   * @return The average Costing
   */
  private Costing getTrxCurrentCosting(MaterialTransaction trx) {
    HashMap<CostDimension, BaseOBObject> costDimensions = getCostDimensions();
    //@formatter:off
    String hql =
            "as c" +
            " where c.product = :product";
    //@formatter:on
    // FIXME: remove when manufacturing costs are fully migrated
    if (isManufacturingProduct) {
      //@formatter:off
      hql +=
            "   and c.client = :client";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and c.organization = :org";
      //@formatter:on
    }
    if (costDimensions.get(CostDimension.Warehouse) == null) {
      //@formatter:off
      hql +=
            "   and c.warehouse is null";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and c.warehouse = :warehouse";
      //@formatter:on
    }
    // The starting date of the costing needs to be before the reference date to avoid the case when
    // the given transaction has a related average costing.
    //@formatter:off
    hql +=
            "   and c.id != :sourceid" +
            "   and c.endingDate >= :trxdate" +
            "   and c.startingDate < :trxdate" +
            " order by c.startingDate desc";
    //@formatter:on

    OBQuery<Costing> qryCosting = OBDal.getInstance()
        .createQuery(Costing.class, hql)
        .setNamedParameter("product", trx.getProduct());

    // FIXME: remove when manufacturing costs are fully migrated
    if (isManufacturingProduct) {
      qryCosting.setNamedParameter("client", OBDal.getInstance().get(Client.class, strClientId));
    } else {
      qryCosting.setNamedParameter("org", getCostOrg());
    }
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      qryCosting.setNamedParameter("warehouse", costDimensions.get(CostDimension.Warehouse));
    }

    return qryCosting.setNamedParameter("sourceid", bdCostingId)
        .setNamedParameter("trxdate", trx.getTransactionProcessDate())
        .setMaxResult(1)
        .uniqueResult();
  }

  private boolean isVoidedTrx(MaterialTransaction trx, TrxType currentTrxType) {
    // Transactions of voided documents do not need adjustment
    switch (currentTrxType) {
      case ReceiptVoid:
      case ShipmentVoid:
      case InternalConsVoid:
        return true;
      case Receipt:
      case ReceiptNegative:
      case ReceiptReturn:
      case Shipment:
      case ShipmentNegative:
      case ShipmentReturn:
        if (trx.getGoodsShipmentLine().getShipmentReceipt().getDocumentStatus().equals("VO")) {
          return true;
        }
        break;
      case InternalCons:
      case InternalConsNegative:
        if (trx.getInternalConsumptionLine().getInternalConsumption().getStatus().equals("VO")) {
          return true;
        }
        break;
      default:
        break;
    }
    return false;
  }

  /**
   * Returns true if a transaction is a backdated transaction (has related backdated transaction
   * adjustments).
   * 
   * @param trx
   *          MaterialTransaction to check if is backdated or not.
   * @return boolean
   */
  private boolean isBackdatedTransaction(MaterialTransaction trx) {
    OBCriteria<CostAdjustmentLine> critLines = OBDal.getInstance()
        .createCriteria(CostAdjustmentLine.class);
    critLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION, trx));
    critLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_ISBACKDATEDTRX, true));
    critLines.setMaxResults(1);
    return critLines.uniqueResult() != null;
  }

  /**
   * Get negative cost adjustment lines related to trx
   * 
   * @param trx
   *          MaterialTransaction to get related negative cost adjustment lines
   * @return CostAdjustmentLine list
   */
  private List<CostAdjustmentLine> getNegativeStockAdjustments(MaterialTransaction trx) {
    OBCriteria<CostAdjustmentLine> critLines = OBDal.getInstance()
        .createCriteria(CostAdjustmentLine.class);
    critLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_INVENTORYTRANSACTION, trx));
    critLines.add(Restrictions.eq(CostAdjustmentLine.PROPERTY_ISNEGATIVESTOCKCORRECTION, true));
    return critLines.list();
  }
}
