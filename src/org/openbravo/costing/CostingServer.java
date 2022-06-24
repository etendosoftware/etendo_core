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
 * All portions are Copyright (C) 2012-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.persistence.Tuple;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.businessUtility.Preferences;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.PropertyException;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.InternalConsumption;
import org.openbravo.model.materialmgmt.transaction.InternalMovement;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ProductionTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOut;
import org.openbravo.model.materialmgmt.transaction.TransactionLast;
import org.openbravo.model.procurement.ReceiptInvoiceMatch;

/**
 * @author gorkaion
 * 
 */
public class CostingServer {
  private MaterialTransaction transaction;
  private BigDecimal trxCost;
  protected static Logger log4j = LogManager.getLogger();
  private CostingRule costingRule;
  private CostingAlgorithm costingAlgorithm;
  private Currency currency;
  private Organization organization;
  static final String STR_CATEGORY_LANDED_COST = "LDC";
  static final String STR_TABLE_LANDED_COST = "M_LandedCost";

  public CostingServer(final MaterialTransaction transaction) {
    this.transaction = transaction;
    init();
  }

  private void init() {
    organization = getOrganization();
    costingRule = getCostDimensionRule();
    currency = getCostCurrency();
    trxCost = transaction.getTransactionCost();
    costingAlgorithm = getCostingAlgorithm();
  }

  /**
   * Calculates and stores in the database the cost of the transaction.
   * 
   * 
   */
  public void process() {
    try {
      if (trxCost != null) {
        // Transaction cost has already been calculated. Nothing to do.
        return;
      }
      log4j.debug("Process cost");
      try {
        OBContext.setAdminMode(false);

        trxCost = costingAlgorithm.getTransactionCost();
        if (trxCost == null && !transaction.getCostingStatus().equals("P")) {
          throw new OBException("@NoCostCalculated@: " + transaction.getIdentifier());
        }
        if (transaction.getCostingStatus().equals("P")) {
          return;
        }

        trxCost = trxCost.setScale(
            costingAlgorithm.getCostCurrency().getStandardPrecision().intValue(),
            RoundingMode.HALF_UP);
        log4j.debug("  *** Transaction cost amount: " + trxCost.toString());
        // Save calculated cost on M_Transaction.
        transaction = OBDal.getInstance().get(MaterialTransaction.class, transaction.getId());
        transaction.setTransactionCost(trxCost);
        transaction.setCurrency(currency);
        transaction.setCostCalculated(true);
        transaction.setCostingStatus("CC");
        // Flush the changes in the Transaction to fire the Triggers in the database before creating
        // the TransactionCost. If not done here, the Trigger for the TransactionCost will be
        // launched before the one in the Transaction generating wrong data in some tables
        OBDal.getInstance().flush();
        // insert on m_transaction_cost
        createTransactionCost();
        updateLastTransaction();
        // Flush the changes in the TransactionCost and in the Transaction to fire the Triggers in
        // the database before doing Cost Adjustments
        OBDal.getInstance().flush();

        setNotPostedTransaction();
        checkCostAdjustments();
      } finally {
        OBContext.restorePreviousMode();
      }
    } finally {
      // Every Transaction must be set as Processed = 'Y' after going through this method
      transaction.setProcessed(true);
      OBDal.getInstance().flush();
    }
  }

  private void updateLastTransaction() {
    TransactionLast lastTransaction = CostAdjustmentUtils.getLastTransaction(transaction,
        costingRule.isWarehouseDimension());
    boolean needsUpdate = true;
    if (lastTransaction == null) {
      lastTransaction = OBProvider.getInstance().get(TransactionLast.class);
    } else {
      needsUpdate = CostAdjustmentUtils.compareToLastTransaction(transaction, lastTransaction,
          CostingUtils.getCostingRuleStartingDate(costingRule)) > 0;
    }

    if (needsUpdate) {
      lastTransaction.setClient(transaction.getClient());
      lastTransaction.setOrganization(organization);
      lastTransaction.setInventoryTransaction(transaction);
      lastTransaction.setProduct(transaction.getProduct());
      if (costingRule.isWarehouseDimension()) {
        lastTransaction.setWarehouse(transaction.getStorageBin().getWarehouse());
      }
      OBDal.getInstance().save(lastTransaction);
    }
  }

  private void checkCostAdjustments() {
    final TrxType trxType = TrxType.getTrxType(transaction);
    boolean adjustmentAlreadyCreated = false;

    boolean checkPriceCorrectionTrxs = false;
    boolean checkNegativeStockCorrectionTrxs = false;
    // check if price correction is needed
    try {
      checkPriceCorrectionTrxs = Preferences
          .getPreferenceValue(CostAdjustmentUtils.ENABLE_AUTO_PRICE_CORRECTION_PREF, true,
              OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), null)
          .equals(Preferences.YES);
    } catch (PropertyException e1) {
      checkPriceCorrectionTrxs = false;
    }
    if (checkPriceCorrectionTrxs && transaction.isCheckpricedifference()
        && !StringUtils.equals(transaction.getCostingAlgorithm().getJavaClassName(),
            "org.openbravo.costing.StandardAlgorithm")) {
      final JSONObject message = PriceDifferenceProcess
          .processPriceDifferenceTransaction(transaction);
      if (message.has("documentNo")) {
        adjustmentAlreadyCreated = true;
      }
    }

    // check if landed cost need to be processed
    if (trxType == TrxType.Receipt || trxType == TrxType.ReceiptReturn
        || trxType == TrxType.ReceiptNegative) {
      //@formatter:off
      final String hql =
                    "as lc" +
                    " where not exists (" +
                    "   select 1 from MaterialMgmtMaterialTransaction mtrans" +
                    "     join mtrans.goodsShipmentLine iol" +
                    "    where iol.shipmentReceipt.id = :inoutId" +
                    "      and mtrans.isCostCalculated= false" +
                    "   )" +
                    "   and lc.landedCost is null" +
                    "   and lc.goodsShipment.id = :inoutId";
      //@formatter:on

      final ScrollableResults lcLines = OBDal.getInstance()
          .createQuery(LandedCostCost.class, hql)
          .setNamedParameter("inoutId",
              transaction.getGoodsShipmentLine().getShipmentReceipt().getId())
          .scroll(ScrollMode.FORWARD_ONLY);
      try {
        LandedCost landedCost = null;

        while (lcLines.next()) {
          if (landedCost == null) {
            final DocumentType docType = FIN_Utility.getDocumentType(organization,
                STR_CATEGORY_LANDED_COST);
            final String docNo = FIN_Utility.getDocumentNo(docType, STR_TABLE_LANDED_COST);

            landedCost = OBProvider.getInstance().get(LandedCost.class);
            landedCost.setReferenceDate(new Date());
            landedCost.setDocumentType(docType);
            landedCost.setDocumentNo(docNo);
            landedCost.setOrganization(organization);
            OBDal.getInstance().save(landedCost);

            final LCReceipt lcReceipt = OBProvider.getInstance().get(LCReceipt.class);
            lcReceipt.setLandedCost(landedCost);
            lcReceipt.setOrganization(organization);
            lcReceipt.setGoodsShipment(transaction.getGoodsShipmentLine().getShipmentReceipt());
            OBDal.getInstance().save(lcReceipt);

          }
          final LandedCostCost landedCostCost = (LandedCostCost) lcLines.get()[0];
          landedCostCost.setLandedCost(landedCost);
          landedCost.getLandedCostCostList().add(landedCostCost);
          OBDal.getInstance().save(landedCost);
          OBDal.getInstance().save(landedCostCost);
        }

        if (landedCost != null) {
          OBDal.getInstance().flush();
          final JSONObject message = LandedCostProcess.doProcessLandedCost(landedCost);
          if (message.has("documentNo")) {
            adjustmentAlreadyCreated = true;
          }

          if (message.get("severity") != "success") {
            throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingLandedCost@")
                + ": " + landedCost.getDocumentNo() + " - " + message.getString("text"));
          }
        }
      } catch (JSONException e) {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingLandedCost@"));
      } finally {
        lcLines.close();
      }
    }

    // With Standard Algorithm, only BDT are checked
    if (StringUtils.equals(transaction.getCostingAlgorithm().getJavaClassName(),
        "org.openbravo.costing.StandardAlgorithm")) {

      if (CostAdjustmentUtils.isNeededBackdatedCostAdjustment(transaction,
          getCostingRule().isWarehouseDimension(),
          CostingUtils.getCostingRuleStartingDate(getCostingRule()))) {

        // Case transaction backdated (modifying the stock in the past)
        if (trxType != TrxType.InventoryClosing && trxType != TrxType.InventoryOpening
            && getCostingRule().isBackdatedTransactionsFixed()
            && DateUtils.truncate(transaction.getMovementDate(), Calendar.DATE)
                .compareTo(DateUtils.truncate(
                    CostingUtils.getCostingRuleFixBackdatedFrom(getCostingRule()),
                    Calendar.DATE)) >= 0) {
          // BDT = Backdated transaction
          createAdjustment("BDT", BigDecimal.ZERO);
        }

        // Case Inventory Amount Update backdated (modifying the cost in the past)
        if (trxType == TrxType.InventoryOpening && transaction.getMovementDate()
            .compareTo(CostingUtils.getCostingRuleStartingDate(getCostingRule())) >= 0) {
          OBDal.getInstance().refresh(transaction.getPhysicalInventoryLine().getPhysInventory());
          if (!transaction.getPhysicalInventoryLine()
              .getPhysInventory()
              .getInventoryAmountUpdateLineInventoriesInitInventoryList()
              .isEmpty()
              && CostingUtils.isLastOpeningTransaction(transaction,
                  getCostingRule().isWarehouseDimension())) {
            // BDT = Backdated transaction
            createAdjustment("BDT", BigDecimal.ZERO);
          }
        }
      }

      // update trxCost after cost adjustments
      transaction = OBDal.getInstance().get(MaterialTransaction.class, transaction.getId());
      trxCost = CostAdjustmentUtils.getTrxCost(transaction, false, getCostCurrency());

      // With Standard Algorithm, no more adjustments are needed
      return;
    }

    if (getCostingRule().isBackdatedTransactionsFixed()
        && DateUtils.truncate(transaction.getMovementDate(), Calendar.DATE)
            .compareTo(DateUtils.truncate(
                CostingUtils.getCostingRuleFixBackdatedFrom(getCostingRule()), Calendar.DATE)) >= 0
        && CostAdjustmentUtils.isNeededBackdatedCostAdjustment(transaction,
            getCostingRule().isWarehouseDimension(),
            CostingUtils.getCostingRuleStartingDate(getCostingRule()))) {
      // BDT = Backdated transaction
      adjustmentAlreadyCreated = createAdjustment("BDT", null);
    }

    // check if negative stock correction should be done
    try {
      checkNegativeStockCorrectionTrxs = Preferences
          .getPreferenceValue(CostAdjustmentUtils.ENABLE_NEGATIVE_STOCK_CORRECTION_PREF, true,
              OBContext.getOBContext().getCurrentClient(),
              OBContext.getOBContext().getCurrentOrganization(), OBContext.getOBContext().getUser(),
              OBContext.getOBContext().getRole(), null)
          .equals(Preferences.YES);
    } catch (PropertyException e1) {
      checkNegativeStockCorrectionTrxs = false;
    }

    // the stock previous to transaction was zero or negative
    if (checkNegativeStockCorrectionTrxs
        && AverageAlgorithm.modifiesAverage(TrxType.getTrxType(transaction))
        && !adjustmentAlreadyCreated) {
      final BigDecimal currentStock = CostAdjustmentUtils.getStockOnTransactionDate(
          getOrganization(), transaction, costingAlgorithm.costDimensions,
          transaction.getProduct().isProduction(), costingRule.isBackdatedTransactionsFixed(),
          transaction.getCurrency());
      if (currentStock.compareTo(transaction.getMovementQuantity()) < 0
          || (trxType != TrxType.InventoryOpening
              && currentStock.compareTo(transaction.getMovementQuantity()) == 0
              && CostingUtils.existsProcessedTransactions(transaction.getProduct(),
                  costingAlgorithm.costDimensions, getOrganization(), transaction,
                  transaction.getProduct().isProduction()))) {

        // NSC = Negative Stock Correction
        createAdjustment("NSC", null);
      }
    }

    // check if closing inventory needs to be adjusted due to a remainder value
    if (trxType == TrxType.InventoryClosing) {
      final BigDecimal currentStock = CostAdjustmentUtils.getStockOnTransactionDate(
          getOrganization(), transaction, costingAlgorithm.costDimensions,
          transaction.getProduct().isProduction(), costingRule.isBackdatedTransactionsFixed(),
          transaction.getCurrency());

      if (BigDecimal.ZERO.compareTo(currentStock) == 0) {
        final BigDecimal currentValuedStock = CostAdjustmentUtils.getValuedStockOnTransactionDate(
            getOrganization(), transaction, costingAlgorithm.costDimensions,
            transaction.getProduct().isProduction(), costingRule.isBackdatedTransactionsFixed(),
            transaction.getCurrency());

        if (BigDecimal.ZERO.compareTo(currentValuedStock) != 0) {
          // NSC = Negative Stock Correction
          createAdjustment("NSC", currentValuedStock);
        }
      }
    }

    if (trxType == TrxType.ReceiptVoid || trxType == TrxType.ShipmentVoid) {
      createCostAdjustmentForVoidedReceiptOrShipment();
    }
    // update trxCost after cost adjustments
    transaction = OBDal.getInstance().get(MaterialTransaction.class, transaction.getId());
    trxCost = CostAdjustmentUtils.getTrxCost(transaction, false, getCostCurrency());
  }

  private void createCostAdjustmentForVoidedReceiptOrShipment() {
    final MaterialTransaction origInOutLineTrx = transaction.getGoodsShipmentLine()
        .getCanceledInoutLine()
        .getMaterialMgmtMaterialTransactionList()
        .get(0);
    final boolean isCostPermanent = transaction.isCostPermanent();
    transaction.setCostPermanent(false);
    OBDal.getInstance().save(transaction);
    try (ScrollableResults scroll = getCostAdjustmentLines(origInOutLineTrx)) {
      while (scroll.next()) {
        final Tuple result = (Tuple) scroll.get()[0];
        final BigDecimal cost = (BigDecimal) result.get("cost");
        final String sourceProcess = (String) result.get("sourceProcess");
        createAdjustment(sourceProcess, cost);
      }
    }
    OBDal.getInstance().refresh(transaction);
    transaction.setCostPermanent(isCostPermanent);
    OBDal.getInstance().flush();
  }

  private ScrollableResults getCostAdjustmentLines(final MaterialTransaction origInOutLineTrx) {

    //@formatter:off
    final String hqlQuery = 
                  " select tc.cost as cost, " + 
                  "   ca.sourceProcess as sourceProcess " +
                  " from TransactionCost tc " + 
                  "   join tc.costAdjustmentLine tal " + 
                  "   join tal.costAdjustment ca " +
                  " where tc.inventoryTransaction.id = :transactionId";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hqlQuery, Tuple.class)
        .setParameter("transactionId", origInOutLineTrx.getId())
        .setFetchSize(1000)
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private boolean createAdjustment(String type, BigDecimal amount) {
    final CostAdjustment costAdjustmentHeader = CostAdjustmentUtils
        .insertCostAdjustmentHeader(transaction.getOrganization(), type);
    final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
        transaction, amount, costAdjustmentHeader);
    lineParameters.setSource(true);
    if (StringUtils.equals(type, "BDT")) {
      lineParameters.setBackdatedTransaction(true);
    } else if (StringUtils.equals(type, "NSC")) {
      lineParameters.setNegativeCorrection(true);
    }

    CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters, transaction.getMovementDate());

    OBDal.getInstance().flush();
    JSONObject message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjustmentHeader);

    try {
      if (message.get("severity") != "success") {
        throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
            + costAdjustmentHeader.getDocumentNo() + " - " + message.getString("text"));
      }
      return true;
    } catch (JSONException ignore) {
      throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
    }
  }

  private void setNotPostedTransaction() {
    TrxType trxType = TrxType.getTrxType(transaction);
    switch (trxType) {
      case Shipment:
      case ShipmentReturn:
      case ShipmentVoid:
      case ShipmentNegative:
      case Receipt:
      case ReceiptReturn:
      case ReceiptVoid:
      case ReceiptNegative:
        unpostShipmentsAndMatchedInvoices();
        break;
      case InventoryDecrease:
      case InventoryIncrease:
      case InventoryOpening:
      case InventoryClosing:
        unpostPhysicalInventory();
        break;
      case IntMovementFrom:
      case IntMovementTo:
        unpostGoodsMovement();
        break;
      case InternalCons:
      case InternalConsNegative:
      case InternalConsVoid:
        unpostInternalConsumption();
        break;
      case BOMPart:
      case BOMProduct:
      case ManufacturingConsumed:
      case ManufacturingProduced:
        unpostProduction();
        break;
      case Unknown:
        throw new OBException("@UnknownTrxType@: " + transaction.getIdentifier());
      default:
        throw new OBException("@UnknownTrxType@: " + transaction.getIdentifier());
    }
  }

  private void unpostShipmentsAndMatchedInvoices() {
    final ShipmentInOut inout = transaction.getGoodsShipmentLine().getShipmentReceipt();
    if (!"N".equals(inout.getPosted()) || !"Y".equals(inout.getPosted())) {
      inout.setPosted("N");
      OBDal.getInstance().save(inout);
      // Set for the Match Invoices associated
      final List<ReceiptInvoiceMatch> invoiceMatchList = transaction.getGoodsShipmentLine()
          .getProcurementReceiptInvoiceMatchList();
      if (invoiceMatchList != null && !invoiceMatchList.isEmpty()) {
        for (ReceiptInvoiceMatch invoiceMatch : invoiceMatchList) {
          if (!"N".equals(invoiceMatch.getPosted()) || !"Y".equals(invoiceMatch.getPosted())) {
            invoiceMatch.setPosted("N");
            OBDal.getInstance().save(invoiceMatch);
          }
        }
      }
    }
  }

  private void unpostPhysicalInventory() {
    final InventoryCount inventory = transaction.getPhysicalInventoryLine().getPhysInventory();
    if (!"N".equals(inventory.getPosted()) || !"Y".equals(inventory.getPosted())) {
      inventory.setPosted("N");
      OBDal.getInstance().save(inventory);
    }
  }

  private void unpostGoodsMovement() {
    final InternalMovement movement = transaction.getMovementLine().getMovement();
    if (!"N".equals(movement.getPosted()) || !"Y".equals(movement.getPosted())) {
      movement.setPosted("N");
      OBDal.getInstance().save(movement);
    }
  }

  private void unpostInternalConsumption() {
    final InternalConsumption consumption = transaction.getInternalConsumptionLine()
        .getInternalConsumption();
    if (!"N".equals(consumption.getPosted()) || !"Y".equals(consumption.getPosted())) {
      consumption.setPosted("N");
      OBDal.getInstance().save(consumption);
    }
  }

  private void unpostProduction() {
    final ProductionTransaction production = transaction.getProductionLine()
        .getProductionPlan()
        .getProduction();
    if (!"N".equals(production.getPosted()) || !"Y".equals(production.getPosted())) {
      production.setPosted("N");
      OBDal.getInstance().save(production);
    }
  }

  private CostingAlgorithm getCostingAlgorithm() {
    // Algorithm class is retrieved from costDimensionRule
    org.openbravo.model.materialmgmt.cost.CostingAlgorithm costAlgorithm = costingRule
        .getCostingAlgorithm();
    // FIXME: remove when manufacturing costs are fully migrated
    // In case the product is Manufacturing type it is forced to use Average Algorithm
    if (transaction.getProduct().isProduction()
        && !"org.openbravo.costing.StandardAlgorithm".equals(costAlgorithm.getJavaClassName())) {
      final OBQuery<org.openbravo.model.materialmgmt.cost.CostingAlgorithm> caQry = OBDal
          .getInstance()
          .createQuery(org.openbravo.model.materialmgmt.cost.CostingAlgorithm.class,
              org.openbravo.model.materialmgmt.cost.CostingAlgorithm.PROPERTY_JAVACLASSNAME
                  + " = 'org.openbravo.costing.AverageAlgorithm'");
      caQry.setFilterOnReadableClients(false);
      caQry.setFilterOnReadableOrganization(false);
      costAlgorithm = caQry.uniqueResult();
    }
    transaction.setCostingAlgorithm(costAlgorithm);

    try {
      final Class<?> clz = OBClassLoader.getInstance().loadClass(costAlgorithm.getJavaClassName());
      final CostingAlgorithm algorithm = (CostingAlgorithm) clz.getDeclaredConstructor()
          .newInstance();
      algorithm.init(this);
      return algorithm;
    } catch (Exception e) {
      log4j.error("Exception loading Algorithm class: " + costAlgorithm.getJavaClassName()
          + " algorithm: " + costAlgorithm.getIdentifier());
      throw new OBException("@AlgorithmClassNotLoaded@: " + costAlgorithm.getName(), e);
    }
  }

  private void createTransactionCost() {
    final TransactionCost transactionCost = OBProvider.getInstance().get(TransactionCost.class);
    transactionCost.setInventoryTransaction(transaction);
    transactionCost.setOrganization(transaction.getOrganization());
    transactionCost.setCost(trxCost);
    transactionCost.setCurrency(currency);
    transactionCost.setCostDate(transaction.getTransactionProcessDate());
    transactionCost.setAccountingDate(transaction.getMovementDate());
    transactionCost.setUnitCost(Boolean.TRUE);
    OBDal.getInstance().save(transactionCost);
    transaction.getTransactionCostList().add(transactionCost);
  }

  public BigDecimal getTransactionCost() {
    return trxCost;
  }

  private CostingRule getCostDimensionRule() {
    //@formatter:off
    final String hql = 
                  " organization.id = :organizationId" +
                  "   and (startingDate is null " +
                  "   or startingDate <= :startdate)" +
                  "   and (endingDate is null" +
                  "   or endingDate >= :enddate )" +
                  "   and validated = true" +
                  " order by case when startingDate is null then 1 else 0 end" +
                  "   , startingDate desc";
    //@formatter:on

    final List<CostingRule> costRules = OBDal.getInstance()
        .createQuery(CostingRule.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("organizationId", organization.getId())
        .setNamedParameter("startdate", transaction.getTransactionProcessDate())
        .setNamedParameter("enddate", transaction.getTransactionProcessDate())
        .setMaxResult(1)
        .list();

    if (costRules.isEmpty()) {
      throw new OBException(
          "@NoCostingRuleFoundForOrganizationAndDate@ @Organization@: " + organization.getName()
              + ", @Date@: " + OBDateUtils.formatDate(transaction.getTransactionProcessDate()));
    }
    return costRules.get(0);
  }

  public Currency getCostCurrency() {
    if (currency != null) {
      return currency;
    }
    // FIXME: remove when manufacturing costs are fully migrated
    // Production product costs are calculated in clients currency
    if (transaction.getProduct().isProduction()) {
      return transaction.getClient().getCurrency();
    }
    if (organization != null) {
      if (organization.getCurrency() != null) {
        return organization.getCurrency();
      }
      return organization.getClient().getCurrency();
    } else {
      init();
      return getCostCurrency();
    }
  }

  public Organization getOrganization() {
    if (organization != null) {
      return organization;
    }
    final Organization org = OBContext.getOBContext()
        .getOrganizationStructureProvider(transaction.getClient().getId())
        .getLegalEntity(transaction.getOrganization());
    if (org == null) {
      throw new OBException("@WrongCostOrganization@" + transaction.getIdentifier());
    }
    return org;
  }

  public CostingRule getCostingRule() {
    return costingRule;
  }

  public MaterialTransaction getTransaction() {
    return transaction;
  }

  /**
   * Transaction types implemented on the cost engine.
   */
  public enum TrxType {
    Shipment,
    ShipmentReturn,
    ShipmentVoid,
    ShipmentNegative,
    Receipt,
    ReceiptReturn,
    ReceiptVoid,
    ReceiptNegative,
    InventoryIncrease,
    InventoryDecrease,
    InventoryOpening,
    InventoryClosing,
    IntMovementFrom,
    IntMovementTo,
    InternalCons,
    InternalConsNegative,
    InternalConsVoid,
    BOMPart,
    BOMProduct,
    ManufacturingConsumed,
    ManufacturingProduced,
    Unknown;
    /**
     * Given a Material Management transaction returns its type.
     */
    public static TrxType getTrxType(final MaterialTransaction transaction) {
      if (transaction.getGoodsShipmentLine() != null) {
        // Receipt / Shipment
        final org.openbravo.model.materialmgmt.transaction.ShipmentInOut inout = transaction
            .getGoodsShipmentLine()
            .getShipmentReceipt();
        if (inout.isSalesTransaction()) {
          // Shipment
          if (inout.getDocumentStatus().equals("VO")
              && transaction.getGoodsShipmentLine().getCanceledInoutLine() != null) {
            log4j.debug("Void shipment: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ShipmentVoid;
          } else if (inout.getDocumentType().isReturn()) {
            log4j.debug("Reversal shipment: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ShipmentReturn;
          } else if (transaction.getGoodsShipmentLine()
              .getMovementQuantity()
              .compareTo(BigDecimal.ZERO) < 0) {
            log4j.debug("Negative Shipment: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ShipmentNegative;
          } else {
            log4j.debug("Shipment: " + transaction.getGoodsShipmentLine().getIdentifier());
            return Shipment;
          }
        } else {
          // Receipt
          if (inout.getDocumentStatus().equals("VO")
              && transaction.getGoodsShipmentLine().getCanceledInoutLine() != null) {
            log4j.debug("Void receipt: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ReceiptVoid;
          } else if (inout.getDocumentType().isReturn()) {
            log4j.debug("Reversal Receipt: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ReceiptReturn;
          } else if (transaction.getGoodsShipmentLine()
              .getMovementQuantity()
              .compareTo(BigDecimal.ZERO) < 0) {
            log4j.debug("Negative Receipt: " + transaction.getGoodsShipmentLine().getIdentifier());
            return ReceiptNegative;
          } else {
            log4j.debug("Receipt: " + transaction.getGoodsShipmentLine().getIdentifier());
            return Receipt;
          }
        }
      } else if (transaction.getPhysicalInventoryLine() != null) {
        // Physical Inventory
        final String invType = transaction.getPhysicalInventoryLine()
            .getPhysInventory()
            .getInventoryType();
        if ("O".equals(invType)) {
          return InventoryOpening;
        } else if ("C".equals(invType)) {
          return InventoryClosing;
        }
        if (transaction.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) {
          log4j.debug("Physical inventory, increments stock: "
              + transaction.getPhysicalInventoryLine().getIdentifier());
          return InventoryIncrease;
        } else {
          log4j.debug("Physical inventory, decreases stock "
              + transaction.getPhysicalInventoryLine().getIdentifier());
          return InventoryDecrease;
        }
      } else if (transaction.getMovementLine() != null) {
        // Internal movement
        if (transaction.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) {
          log4j.debug("Internal Movement to: " + transaction.getMovementLine().getIdentifier());
          return IntMovementTo;
        } else {
          log4j.debug("Internal Movement from: " + transaction.getMovementLine().getIdentifier());
          return IntMovementFrom;
        }
      } else if (transaction.getInternalConsumptionLine() != null) {
        if (transaction.getInternalConsumptionLine().getVoidedInternalConsumptionLine() != null) {
          return InternalConsVoid;
        } else if (transaction.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) {
          log4j.debug("Negative Internal Consumption: "
              + transaction.getInternalConsumptionLine().getIdentifier());
          return InternalConsNegative;
        } else {
          log4j.debug(
              "Internal Consumption: " + transaction.getInternalConsumptionLine().getIdentifier());
          return InternalCons;
        }
      } else if (transaction.getProductionLine() != null) {
        // Production Line
        if (transaction.getProductionLine()
            .getProductionPlan()
            .getProduction()
            .isSalesTransaction()) {
          // BOM Production
          if (transaction.getMovementQuantity().compareTo(BigDecimal.ZERO) > 0) {
            log4j.debug("Produced BOM product: " + transaction.getProductionLine().getIdentifier());
            return BOMProduct;
          } else {
            log4j.debug("Used BOM Part: " + transaction.getProductionLine().getIdentifier());
            // Used parts
            return BOMPart;
          }
        } else {
          log4j.debug("Manufacturing Product");
          // Work Effort
          if ("+".equals(transaction.getProductionLine().getProductionType())) {
            return ManufacturingProduced;
          } else {
            return ManufacturingConsumed;
          }
        }
      }
      return Unknown;
    }
  }
}
