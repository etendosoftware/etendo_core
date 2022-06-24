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
import java.math.RoundingMode;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.enterprise.context.Dependent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.InternalConsumptionLine;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionLine;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

@Dependent
public abstract class CostingAlgorithmAdjustmentImp {
  private static final Logger log4j = LogManager.getLogger();
  protected String strCostAdjLineId;
  protected String strCostAdjId;
  protected String strTransactionId;
  protected String strCostOrgId;
  protected String strCostCurrencyId;
  protected int costCurPrecission;
  protected int stdCurPrecission;
  protected TrxType trxType;
  protected String strCostingRuleId;
  protected Date startingDate;
  protected String strClientId;
  protected boolean isManufacturingProduct;
  protected boolean areBackdatedTrxFixed;
  protected boolean checkNegativeStockCorrection;
  protected Long nextLineNo;
  protected HashMap<CostDimension, String> costDimensionIds = new HashMap<CostDimension, String>();

  /**
   * Initializes class variables to perform the cost adjustment process. Variables are stored by the
   * ids instead of the BaseOBObject to be safe of session clearing.
   * 
   * @param costAdjLine
   *          The Cost Adjustment Line that it is processed.
   */
  protected void init(final CostAdjustmentLine costAdjLine) {
    strCostAdjLineId = costAdjLine.getId();
    strCostAdjId = costAdjLine.getCostAdjustment().getId();
    MaterialTransaction transaction = costAdjLine.getInventoryTransaction();
    strTransactionId = transaction.getId();
    isManufacturingProduct = transaction.getProduct().isProduction();
    final CostingServer costingServer = new CostingServer(transaction);
    strCostOrgId = costingServer.getOrganization().getId();
    strCostCurrencyId = transaction.getCurrency().getId();
    costCurPrecission = transaction.getCurrency().getCostingPrecision().intValue();
    stdCurPrecission = transaction.getCurrency().getStandardPrecision().intValue();
    trxType = CostingServer.TrxType.getTrxType(transaction);
    final CostingRule costingRule = costingServer.getCostingRule();
    strCostingRuleId = costingRule.getId();
    startingDate = CostingUtils.getCostingRuleStartingDate(costingRule);
    strClientId = costingRule.getClient().getId();
    areBackdatedTrxFixed = costingRule.isBackdatedTransactionsFixed()
        && !transaction.getTransactionProcessDate()
            .before(CostingUtils.getCostingRuleFixBackdatedFrom(costingRule));

    final HashMap<CostDimension, BaseOBObject> costDimensions = CostingUtils.getEmptyDimensions();
    // Production products cannot be calculated by warehouse dimension.
    if (costingRule.isWarehouseDimension()) {
      costDimensions.put(CostDimension.Warehouse, transaction.getStorageBin().getWarehouse());
    }
    for (CostDimension costDimension : costDimensions.keySet()) {
      String value = null;
      if (costDimensions.get(costDimension) != null) {
        value = (String) costDimensions.get(costDimension).getId();
      }
      costDimensionIds.put(costDimension, value);
    }
    try {
      checkNegativeStockCorrection = Preferences
          .getPreferenceValue(CostAdjustmentUtils.ENABLE_NEGATIVE_STOCK_CORRECTION_PREF, true,
              OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), null)
          .equals(Preferences.YES);
    } catch (PropertyException e1) {
      checkNegativeStockCorrection = false;
    }
  }

  /**
   * Process to include in the Cost Adjustment the required lines of transactions whose cost needs
   * to be adjusted as a consequence of other lines already included.
   */
  protected void searchRelatedTransactionCosts(final CostAdjustmentLine costAdjLine) {
    boolean searchRelatedTransactions = true;
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
      searchRelatedTransactions = false;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }

    // Backdated transactions are inserted with a null adjustment amount, in case we are not
    // adjusting a Inventory Amount Update transaction.
    if (currentCostAdjLine.isBackdatedTrx() && currentCostAdjLine.getAdjustmentAmount() == null) {
      calculateBackdatedTrxAdjustment(currentCostAdjLine);
    }

    // Negative stock correction are inserted with a null adjustment amount, in case we are not
    // adjusting a Inventory Closing transaction.
    if (currentCostAdjLine.isNegativeStockCorrection()
        && currentCostAdjLine.getAdjustmentAmount() == null) {
      calculateNegativeStockCorrectionAdjustmentAmount(currentCostAdjLine);
    }

    if (currentCostAdjLine.isSource()) {
      addCostDependingTrx(null);
      if (BigDecimal.ZERO.compareTo(currentCostAdjLine.getAdjustmentAmount()) == 0) {
        currentCostAdjLine.setNeedsPosting(Boolean.FALSE);
      }
    }

    if (searchRelatedTransactions) {
      getRelatedTransactionsByAlgorithm();
    }
  }

  protected void addCostDependingTrx(final CostAdjustmentLine costAdjLine) {
    // Some transaction costs are directly related to other transaction costs. These relationships
    // must be kept when the original transaction cost is adjusted adjusting as well the dependent
    // transactions.
    TrxType currentTrxType = trxType;
    if (costAdjLine != null) {
      currentTrxType = TrxType.getTrxType(costAdjLine.getInventoryTransaction());
    }
    switch (currentTrxType) {
      case Shipment:
        searchReturnShipments(costAdjLine);
      case Receipt:
        searchVoidInOut(costAdjLine);
        break;
      case IntMovementFrom:
        searchIntMovementTo(costAdjLine);
        break;
      case InternalCons:
        searchVoidInternalConsumption(costAdjLine);
        break;
      case BOMPart:
        searchBOMProducts(costAdjLine);
        break;
      case ManufacturingConsumed:
        searchManufacturingProduced(costAdjLine);
        break;
      case InventoryDecrease:
        break;
      case InventoryIncrease:
        searchOpeningInventory(costAdjLine);
      default:
        break;
    }
  }

  protected CostAdjustmentLine insertCostAdjustmentLine(
      final CostAdjustmentLineParameters lineParameters) {
    return insertCostAdjustmentLine(lineParameters, null);
  }

  @Deprecated
  protected CostAdjustmentLine insertCostAdjustmentLine(final MaterialTransaction trx,
      final BigDecimal adjustmentamt, final CostAdjustmentLine parentLine) {
    final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
        adjustmentamt, getCostAdj());
    return insertCostAdjustmentLine(lineParameters, parentLine);

  }

  /**
   * Inserts a new cost adjustment line
   *
   * @param parentLine
   *          Cost Adjustment Line
   * 
   */
  protected CostAdjustmentLine insertCostAdjustmentLine(
      final CostAdjustmentLineParameters lineParameters, final CostAdjustmentLine parentLine) {
    Date dateAcct = lineParameters.getTransaction().getMovementDate();

    CostAdjustmentLine currentParentLine;
    if (parentLine == null) {
      currentParentLine = getCostAdjLine();
    } else {
      currentParentLine = parentLine;
    }
    Date parentAcctDate = currentParentLine.getAccountingDate();
    if (parentAcctDate == null) {
      parentAcctDate = currentParentLine.getInventoryTransaction().getMovementDate();
    }

    if (dateAcct.before(parentAcctDate)) {
      dateAcct = parentAcctDate;
    }

    final CostAdjustmentLine newCAL = CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters,
        dateAcct, getNextLineNo());
    if (!newCAL.getId().equals(currentParentLine.getId())) {
      newCAL.setParentCostAdjustmentLine(currentParentLine);
      OBDal.getInstance().save(newCAL);
    }

    addCostDependingTrx(newCAL);
    return newCAL;
  }

  private Long getNextLineNo() {
    if (nextLineNo == null) {
      //@formatter:off
      final String hql =
                  "select max(lineNo)" +
                  "  from CostAdjustmentLine as cal"+
                  " where cal.costAdjustment.id = :costAdjustmentId";
      //@formatter:on

      nextLineNo = OBDal.getInstance()
          .getSession()
          .createQuery(hql, Long.class)
          .setParameter("costAdjustmentId", strCostAdjId)
          .setMaxResults(1)
          .uniqueResult();
    }
    nextLineNo += 10L;
    return nextLineNo;
  }

  /**
   * When the cost of a Closing Inventory is adjusted it is needed to adjust with the same amount
   * the related Opening Inventory.
   */
  protected void searchOpeningInventory(final CostAdjustmentLine costAdjLine) {
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    final InventoryCountLine invline = currentCostAdjLine.getInventoryTransaction()
        .getPhysicalInventoryLine()
        .getRelatedInventory();
    if (invline == null) {
      return;
    }
    MaterialTransaction deptrx = invline.getMaterialMgmtMaterialTransactionList().get(0);
    if (!deptrx.isCostCalculated() || deptrx.isCostPermanent()) {
      return;
    }

    final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(deptrx,
        currentCostAdjLine.getAdjustmentAmount(), getCostAdj());
    insertCostAdjustmentLine(lineParameters, costAdjLine);
  }

  protected void searchManufacturingProduced(final CostAdjustmentLine costAdjLine) {
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    final ProductionLine pl = currentCostAdjLine.getInventoryTransaction().getProductionLine();

    final OBCriteria<ProductionLine> critPL = OBDal.getInstance()
        .createCriteria(ProductionLine.class)
        .add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN, pl.getProductionPlan()))
        .add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONTYPE, "+"))
        .addOrderBy(ProductionLine.PROPERTY_COMPONENTCOST, true);
    critPL.createAlias(ProductionLine.PROPERTY_PRODUCT, "pr");

    BigDecimal pendingAmt = currentCostAdjLine.getAdjustmentAmount();
    CostAdjustmentLine lastAdjLine = null;
    for (ProductionLine pline : critPL.list()) {
      BigDecimal adjAmt = currentCostAdjLine.getAdjustmentAmount();
      if (pline.getComponentCost() != null) {
        adjAmt = currentCostAdjLine.getAdjustmentAmount().multiply(pline.getComponentCost());
        pendingAmt = pendingAmt.subtract(adjAmt);
      }
      if (!pline.getProduct().isStocked() || !"I".equals(pline.getProduct().getProductType())) {
        continue;
      }
      if (pline.getMaterialMgmtMaterialTransactionList().isEmpty()) {
        log4j.error("Production Line with id {} has no related transaction (M_Transaction).",
            pline.getId());
        continue;
      }
      final MaterialTransaction prodtrx = pline.getMaterialMgmtMaterialTransactionList().get(0);
      if (!prodtrx.isCostCalculated() || prodtrx.isCostPermanent()) {
        continue;
      }

      final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(prodtrx,
          adjAmt, getCostAdj());
      final CostAdjustmentLine newCAL = insertCostAdjustmentLine(lineParameters, costAdjLine);

      lastAdjLine = newCAL;
    }
    // If there is more than one P+ product there can be some amount left to assign due to rounding.
    if (pendingAmt.signum() != 0 && lastAdjLine != null) {
      lastAdjLine.setAdjustmentAmount(lastAdjLine.getAdjustmentAmount().add(pendingAmt));
      OBDal.getInstance().save(lastAdjLine);
    }
  }

  protected void searchBOMProducts(final CostAdjustmentLine costAdjLine) {
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    final ProductionLine pl = currentCostAdjLine.getInventoryTransaction().getProductionLine();
    final OBCriteria<ProductionLine> critBOM = OBDal.getInstance()
        .createCriteria(ProductionLine.class)
        .add(Restrictions.eq(ProductionLine.PROPERTY_PRODUCTIONPLAN, pl.getProductionPlan()))
        .add(Restrictions.gt(ProductionLine.PROPERTY_MOVEMENTQUANTITY, BigDecimal.ZERO))
        .add(Restrictions.eq("pr." + Product.PROPERTY_STOCKED, true))
        .add(Restrictions.eq("pr." + Product.PROPERTY_PRODUCTTYPE, "I"));
    critBOM.createAlias(ProductionLine.PROPERTY_PRODUCT, "pr");
    for (ProductionLine pline : critBOM.list()) {
      if (pline.getMaterialMgmtMaterialTransactionList().isEmpty()) {
        log4j.error("BOM Produced with id {} has no related transaction (M_Transaction).",
            pline.getId());
        continue;
      }
      MaterialTransaction prodtrx = pline.getMaterialMgmtMaterialTransactionList().get(0);
      if (!prodtrx.isCostCalculated() || prodtrx.isCostPermanent()) {
        continue;
      }
      final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(prodtrx,
          currentCostAdjLine.getAdjustmentAmount(), getCostAdj());
      lineParameters.setUnitCost(true);
      insertCostAdjustmentLine(lineParameters, costAdjLine);
    }
  }

  protected void searchVoidInternalConsumption(final CostAdjustmentLine costAdjLine) {
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }

    final List<InternalConsumptionLine> intConsVoidedList = currentCostAdjLine
        .getInventoryTransaction()
        .getInternalConsumptionLine()
        .getMaterialMgmtInternalConsumptionLineVoidedInternalConsumptionLineList();

    if (intConsVoidedList.isEmpty()) {
      return;
    }
    final InternalConsumptionLine intCons = intConsVoidedList.get(0);
    final MaterialTransaction voidedTrx = intCons.getMaterialMgmtMaterialTransactionList().get(0);
    if (!voidedTrx.isCostCalculated() || voidedTrx.isCostPermanent()) {
      return;
    }
    final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(voidedTrx,
        currentCostAdjLine.getAdjustmentAmount(), getCostAdj());
    insertCostAdjustmentLine(lineParameters, costAdjLine);
  }

  protected void searchIntMovementTo(final CostAdjustmentLine costAdjLine) {
    CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    final MaterialTransaction transaction = currentCostAdjLine.getInventoryTransaction();
    for (MaterialTransaction movementTransaction : transaction.getMovementLine()
        .getMaterialMgmtMaterialTransactionList()) {
      if (movementTransaction.getId().equals(transaction.getId())) {
        continue;
      }
      if (!movementTransaction.isCostCalculated() || movementTransaction.isCostPermanent()) {
        continue;
      }
      final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
          movementTransaction, currentCostAdjLine.getAdjustmentAmount(), getCostAdj());
      insertCostAdjustmentLine(lineParameters, costAdjLine);
    }
  }

  protected void searchVoidInOut(final CostAdjustmentLine costAdjLine) {
    final CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    ShipmentInOutLine voidedinoutline = currentCostAdjLine.getInventoryTransaction()
        .getGoodsShipmentLine()
        .getCanceledInoutLine();
    if (voidedinoutline == null) {
      return;
    }
    for (MaterialTransaction trx : voidedinoutline.getMaterialMgmtMaterialTransactionList()) {
      if (!trx.isCostCalculated() || trx.isCostPermanent()) {
        continue;
      }
      final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
          currentCostAdjLine.getAdjustmentAmount(), getCostAdj());
      insertCostAdjustmentLine(lineParameters, costAdjLine);
    }
  }

  protected void searchReturnShipments(final CostAdjustmentLine costAdjLine) {
    final CostAdjustmentLine currentCostAdjLine;
    if (costAdjLine != null) {
      currentCostAdjLine = costAdjLine;
    } else {
      currentCostAdjLine = getCostAdjLine();
    }
    final ShipmentInOutLine inoutline = currentCostAdjLine.getInventoryTransaction()
        .getGoodsShipmentLine();
    final BigDecimal costAdjAmt = currentCostAdjLine.getAdjustmentAmount();
    final int precission = getCostCurrency().getStandardPrecision().intValue();
    //@formatter:off
    final String hql =
                  "as trx" +
                  "  join trx.goodsShipmentLine as iol" +
                  "  join iol.shipmentReceipt as io" +
                  "  join iol.salesOrderLine as ol" +
                  " where ol.goodsShipmentLine.id = :shipmentId" +
                  "   and io.documentStatus <> 'VO'";
    //@formatter:on

    final ScrollableResults trxs = OBDal.getInstance()
        .createQuery(MaterialTransaction.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("shipmentId", inoutline.getId())
        .scroll(ScrollMode.FORWARD_ONLY);
    try {
      int counter = 0;
      while (trxs.next()) {
        counter++;

        final MaterialTransaction trx = (MaterialTransaction) trxs.get()[0];
        if (trx.isCostCalculated() && !trx.isCostPermanent()) {
          BigDecimal adjAmt = costAdjAmt.multiply(trx.getMovementQuantity().abs())
              .divide(inoutline.getMovementQuantity().abs(), precission, RoundingMode.HALF_UP);
          final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
              adjAmt, getCostAdj());
          insertCostAdjustmentLine(lineParameters, costAdjLine);
        }

        if (counter % 1000 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
      }
    } finally {
      trxs.close();
    }
  }

  protected abstract void calculateNegativeStockCorrectionAdjustmentAmount(
      final CostAdjustmentLine costAdjLine);

  protected abstract void getRelatedTransactionsByAlgorithm();

  protected void calculateBackdatedTrxAdjustment(final CostAdjustmentLine costAdjLine) {
    BigDecimal adjAmt = BigDecimal.ZERO;
    final TrxType calTrxType = TrxType.getTrxType(costAdjLine.getInventoryTransaction());

    if (costAdjLine.getInventoryTransaction().isCostPermanent() && costAdjLine.isUnitCost()) {
      costAdjLine.setCurrency(
          (Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, strCostCurrencyId));
      costAdjLine.setAdjustmentAmount(BigDecimal.ZERO);
      OBDal.getInstance().save(costAdjLine);
      return;
    }

    // Incoming transactions does not modify the calculated cost
    switch (calTrxType) {
      case ShipmentVoid:
      case ReceiptVoid:
      case InternalConsVoid:
      case BOMProduct:
      case ManufacturingProduced:
        // The cost of these transaction types does not depend on the date it is calculated.
        break;

      case IntMovementTo:
        // get adjustment amount for related Movement From Transaction
        adjAmt = getAdjAmtFromRelatedMovementFrom(costAdjLine);
        break;

      case Receipt:
        if (hasOrder(costAdjLine)) {
          // If the receipt has a related order the cost amount does not depend on the date.
          break;
        }
        // Check receipt default on backdated date.
        adjAmt = getDefaultCostDifference(calTrxType, costAdjLine);
        break;
      case ShipmentReturn:
        // If the return receipt has a original receipt the cost amount does not depend on the
        // date.
        break;
      case ShipmentNegative:
        // These transaction types are calculated using the default cost. Check if there is a
        // difference.
        adjAmt = getDefaultCostDifference(calTrxType, costAdjLine);
        break;
      case InventoryIncrease:
      case InventoryOpening:
        // If the inventory line defines a unit cost it does not depend on the date.
        break;
      case InternalConsNegative:
        // These transaction types are calculated using the default cost. Check if there is a
        // difference.
        adjAmt = getDefaultCostDifference(calTrxType, costAdjLine);
        break;
      case InventoryClosing:
        adjAmt = getInventoryClosingAmt(costAdjLine);
        break;
      case Shipment:
      case ReceiptReturn:
      case ReceiptNegative:
      case InventoryDecrease:
      case IntMovementFrom:
      case InternalCons:
      case BOMPart:
      case ManufacturingConsumed:
        // These transactions are calculated as regular outgoing transactions. The adjustment amount
        // needs to be calculated by the algorithm.
        adjAmt = getOutgoingBackdatedTrxAdjAmt(costAdjLine);
        break;
      default:
        break;
    }
    costAdjLine.setCurrency(
        (Currency) OBDal.getInstance().getProxy(Currency.ENTITY_NAME, strCostCurrencyId));
    costAdjLine.setAdjustmentAmount(adjAmt);
    OBDal.getInstance().save(costAdjLine);

  }

  protected abstract BigDecimal getOutgoingBackdatedTrxAdjAmt(CostAdjustmentLine costAdjLine);

  protected BigDecimal getDefaultCostDifference(final TrxType calTrxType,
      final CostAdjustmentLine costAdjLine) {
    final MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    final BusinessPartner bp = CostingUtils.getTrxBusinessPartner(trx, calTrxType);
    final Organization costOrg = getCostOrg();
    Date trxDate = CostAdjustmentUtils.getLastTrxDateOfMvmntDate(trx.getMovementDate(),
        trx.getProduct(), costOrg, getCostDimensions());
    if (trxDate == null) {
      trxDate = trx.getTransactionProcessDate();
    }

    final BigDecimal defaultCost = CostingUtils.getDefaultCost(trx.getProduct(),
        trx.getMovementQuantity(), costOrg, trxDate, trx.getMovementDate(), bp, getCostCurrency(),
        getCostDimensions());
    final BigDecimal trxCalculatedCost = CostAdjustmentUtils.getTrxCost(trx, true,
        getCostCurrency());
    return defaultCost.subtract(trxCalculatedCost);
  }

  private BigDecimal getInventoryClosingAmt(final CostAdjustmentLine costAdjLine) {
    final MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    // currentBalanceOnDate already includes the cost of the inventory closing. The balance after an
    // inventory closing should be zero, so the adjustment amount should be de current balance
    // negated.
    final BigDecimal currentBalanceOnDate = CostAdjustmentUtils
        .getValuedStockOnMovementDateByAttrAndLocator(trx.getProduct(), getCostOrg(),
            trx.getMovementDate(), getCostDimensions(), trx.getStorageBin(),
            trx.getAttributeSetValue(), getCostCurrency(), true);

    return currentBalanceOnDate.negate();
  }

  /**
   * Checks if the goods receipt line of the adjustment line has a related purchase order line.
   * 
   * @param costAdjLine
   *          the adjustment line to check.
   * @return true if there is a related order line.
   */
  private boolean hasOrder(final CostAdjustmentLine costAdjLine) {
    return costAdjLine.getInventoryTransaction().getGoodsShipmentLine() != null
        && costAdjLine.getInventoryTransaction().getGoodsShipmentLine().getSalesOrderLine() != null;
  }

  public CostAdjustmentLine getCostAdjLine() {
    return OBDal.getInstance().get(CostAdjustmentLine.class, strCostAdjLineId);
  }

  public CostAdjustment getCostAdj() {
    return OBDal.getInstance().get(CostAdjustment.class, strCostAdjId);
  }

  public MaterialTransaction getTransaction() {
    return OBDal.getInstance().get(MaterialTransaction.class, strTransactionId);
  }

  public Organization getCostOrg() {
    return OBDal.getInstance().get(Organization.class, strCostOrgId);
  }

  public Currency getCostCurrency() {
    return OBDal.getInstance().get(Currency.class, strCostCurrencyId);
  }

  public CostingRule getCostingRule() {
    return OBDal.getInstance().get(CostingRule.class, strCostingRuleId);
  }

  public HashMap<CostDimension, BaseOBObject> getCostDimensions() {
    final HashMap<CostDimension, BaseOBObject> costDimensions = new HashMap<>();
    for (CostDimension costDimension : costDimensionIds.keySet()) {
      switch (costDimension) {
        case Warehouse:
          Warehouse warehouse = null;
          if (costDimensionIds.get(costDimension) != null) {
            warehouse = OBDal.getInstance()
                .get(Warehouse.class, costDimensionIds.get(costDimension));
          }
          costDimensions.put(costDimension, warehouse);
          break;
        default:
          break;
      }
    }

    return costDimensions;
  }

  private BigDecimal getAdjAmtFromRelatedMovementFrom(final CostAdjustmentLine costAdjLine) {
    // get Adjusted Amount from related Movement From Transaction
    final MaterialTransaction trx = costAdjLine.getInventoryTransaction();
    BigDecimal totalAdjAmt = BigDecimal.ZERO;
    for (final MaterialTransaction movementTransaction : trx.getMovementLine()
        .getMaterialMgmtMaterialTransactionList()) {
      if (skipAdjustmentsFromTransaction(trx, movementTransaction)) {
        continue;
      }
      for (final TransactionCost trxCost : getNotSourceCostAdjustmentLines(movementTransaction)) {
        totalAdjAmt = totalAdjAmt.add(trxCost.getCost());
      }
    }
    return totalAdjAmt;
  }

  private boolean skipAdjustmentsFromTransaction(final MaterialTransaction trx,
      final MaterialTransaction movementTransaction) {
    return movementTransaction.getId().equals(trx.getId())
        || !movementTransaction.isCostCalculated() || movementTransaction.isCostPermanent();
  }

  private List<TransactionCost> getNotSourceCostAdjustmentLines(
      final MaterialTransaction movementTransaction) {
    return OBDal.getInstance()
        .createCriteria(TransactionCost.class)
        .add(Restrictions.eq(TransactionCost.PROPERTY_INVENTORYTRANSACTION, movementTransaction))
        .add(Restrictions.isNotNull(TransactionCost.PROPERTY_COSTADJUSTMENTLINE))
        .list();
  }
}
