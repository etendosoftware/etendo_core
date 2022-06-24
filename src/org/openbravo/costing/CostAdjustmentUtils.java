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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.advpaymentmngt.utility.FIN_Utility;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.DocumentType;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostAdjustmentLine;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.TransactionLast;

public class CostAdjustmentUtils {
  private static final Logger log4j = LogManager.getLogger();
  public static final String strCategoryCostAdj = "CAD";
  public static final String strTableCostAdj = "M_CostAdjustment";
  public static final String propADListPriority = org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER;
  public static final String propADListReference = org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE;
  public static final String propADListValue = org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY;
  public static final String MovementTypeRefID = "189";
  public static final String ENABLE_AUTO_PRICE_CORRECTION_PREF = "enableAutomaticPriceCorrectionTrxs";
  public static final String ENABLE_NEGATIVE_STOCK_CORRECTION_PREF = "enableNegativeStockCorrections";

  /**
   * Returns a new header for a Cost Adjustment
   * 
   * @param org
   *          organization set in record
   * 
   * @param sourceProcess
   *          the process that origin the Cost Adjustment: - MCC: Manual Cost Correction - IAU:
   *          Inventory Amount Update - PDC: Price Difference Correction - LC: Landed Cost - BDT:
   *          Backdated Transaction
   */
  public static CostAdjustment insertCostAdjustmentHeader(final Organization org,
      final String sourceProcess) {

    final DocumentType docType = FIN_Utility.getDocumentType(org, strCategoryCostAdj);
    final String docNo = FIN_Utility.getDocumentNo(docType, strTableCostAdj);
    final Organization orgLegal = OBContext.getOBContext()
        .getOrganizationStructureProvider(OBContext.getOBContext().getCurrentClient().getId())
        .getLegalEntity(org);

    final CostAdjustment costAdjustment = OBProvider.getInstance().get(CostAdjustment.class);
    costAdjustment.setOrganization(orgLegal);
    costAdjustment.setDocumentType(docType);
    costAdjustment.setDocumentNo(docNo);
    costAdjustment.setReferenceDate(new Date());
    costAdjustment.setSourceProcess(sourceProcess);
    costAdjustment.setProcessed(Boolean.FALSE);
    OBDal.getInstance().save(costAdjustment);

    return costAdjustment;
  }

  /**
   * Creates a new Cost Adjustment Line and returns it.
   * 
   * @param transaction
   *          transaction to apply the cost adjustment
   * 
   * @param costAdjustmentHeader
   *          header of line
   * 
   * @param costAdjusted
   *          amount to adjust in the cost
   * 
   * @param isSource
   */
  @Deprecated
  public static CostAdjustmentLine insertCostAdjustmentLine(final MaterialTransaction transaction,
      final CostAdjustment costAdjustmentHeader, final BigDecimal costAdjusted,
      final boolean isSource, final Date accountingDate) {
    final Long lineNo = getNewLineNo(costAdjustmentHeader);
    return insertCostAdjustmentLine(transaction, costAdjustmentHeader, costAdjusted, isSource,
        accountingDate, lineNo);
  }

  @Deprecated
  public static CostAdjustmentLine insertCostAdjustmentLine(final MaterialTransaction transaction,
      final CostAdjustment costAdjustmentHeader, final BigDecimal costAdjusted,
      final boolean isSource, final Date accountingDate, final Currency currency) {
    final Long lineNo = getNewLineNo(costAdjustmentHeader);
    return insertCostAdjustmentLine(transaction, costAdjustmentHeader, costAdjusted, isSource,
        accountingDate, lineNo, currency);
  }

  @Deprecated
  public static CostAdjustmentLine insertCostAdjustmentLine(final MaterialTransaction transaction,
      final CostAdjustment costAdjustmentHeader, final BigDecimal costAdjusted,
      final boolean isSource, final Date accountingDate, final Long lineNo) {
    return insertCostAdjustmentLine(transaction, costAdjustmentHeader, costAdjusted, isSource,
        accountingDate, lineNo, null);
  }

  @Deprecated
  public static CostAdjustmentLine insertCostAdjustmentLine(final MaterialTransaction transaction,
      final CostAdjustment costAdjustmentHeader, final BigDecimal costAdjusted,
      final boolean isSource, final Date accountingDate, final Long lineNo,
      final Currency currency) {
    final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
        transaction, costAdjusted, costAdjustmentHeader, currency);
    lineParameters.setSource(isSource);
    return insertCostAdjustmentLine(lineParameters, accountingDate, lineNo);
  }

  /**
   * Creates a new Adjustment Line and returns it
   * 
   * @param lineParameters
   *          Object that contains most of the information needed to created the Adjustment Line
   * @param accountingDate
   *          The Date for which this document will be posted
   * @return An Adjustment Line created based on the given parameters
   */
  public static CostAdjustmentLine insertCostAdjustmentLine(
      final CostAdjustmentLineParameters lineParameters, final Date accountingDate) {
    final Long lineNo = getNewLineNo(lineParameters.getCostAdjustmentHeader());
    return insertCostAdjustmentLine(lineParameters, accountingDate, lineNo);
  }

  /**
   * 
   * Creates a new Adjustment Line and returns it
   * 
   * @param lineParameters
   *          Object that contains most of the information needed to created the Adjustment Line
   * @param accountingDate
   *          The Date for which this document will be posted
   * @param lineNo
   *          Number to position the line within the Cost Adjustment Document
   * @return An Adjustment Line created based on the given parameters
   */
  public static CostAdjustmentLine insertCostAdjustmentLine(
      final CostAdjustmentLineParameters lineParameters, final Date accountingDate,
      final Long lineNo) {
    final Long stdPrecission = lineParameters.getTransaction().getCurrency().getStandardPrecision();

    CostAdjustmentLine costAdjustmentLine = getExistingCostAdjustmentLine(lineParameters,
        accountingDate);
    if (costAdjustmentLine == null) {
      costAdjustmentLine = OBProvider.getInstance().get(CostAdjustmentLine.class);
      costAdjustmentLine
          .setOrganization(lineParameters.getCostAdjustmentHeader().getOrganization());
      costAdjustmentLine.setCostAdjustment(lineParameters.getCostAdjustmentHeader());
      costAdjustmentLine.setCurrency(lineParameters.getCurrency());
      costAdjustmentLine.setInventoryTransaction(lineParameters.getTransaction());
      costAdjustmentLine.setSource(lineParameters.isSource());
      costAdjustmentLine.setAccountingDate(accountingDate);
      costAdjustmentLine.setLineNo(lineNo);
      costAdjustmentLine.setUnitCost(lineParameters.isUnitCost());
      costAdjustmentLine.setNegativeStockCorrection(lineParameters.isNegativeCorrection());
      costAdjustmentLine.setBackdatedTrx(lineParameters.isBackdatedTransaction());
      costAdjustmentLine.setNeedsPosting(lineParameters.isNeedPosting());
      costAdjustmentLine
          .setRelatedTransactionAdjusted(lineParameters.isRelatedTransactionAdjusted());
    }
    if (lineParameters.getAdjustmentAmount() == null) {
      costAdjustmentLine.setAdjustmentAmount(null);
    } else {
      BigDecimal previouslyAdjustedAmount = costAdjustmentLine.getAdjustmentAmount() == null
          ? BigDecimal.ZERO
          : costAdjustmentLine.getAdjustmentAmount();
      costAdjustmentLine.setAdjustmentAmount(lineParameters.getAdjustmentAmount()
          .add(previouslyAdjustedAmount)
          .setScale(stdPrecission.intValue(), RoundingMode.HALF_UP));
    }

    OBDal.getInstance().save(costAdjustmentLine);

    return costAdjustmentLine;
  }

  private static CostAdjustmentLine getExistingCostAdjustmentLine(
      final CostAdjustmentLineParameters lineParameters, final Date accountingDate) {
    //@formatter:off
    final String hql =
                  "   costAdjustment.id = :costAdjustmentId " +
                  "   and inventoryTransaction.id = :transactionId " +
                  "   and isRelatedTransactionAdjusted = false " +
                  "   and currency.id = :currencyId " +
                  "   and isSource = :isSource " +
                  "   and accountingDate = :accountingDate" +
                  "   and unitCost = :isUnitCost " +
                  "   and isBackdatedTrx = :isBackdatedTrx" +
                  "   and isNegativeStockCorrection = :isNegativeCorrection";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(CostAdjustmentLine.class, hql)
        .setNamedParameter("costAdjustmentId", lineParameters.getCostAdjustmentHeader().getId())
        .setNamedParameter("transactionId", lineParameters.getTransaction().getId())
        .setNamedParameter("currencyId", lineParameters.getCurrency().getId())
        .setNamedParameter("isSource", lineParameters.isSource())
        .setNamedParameter("accountingDate", accountingDate)
        .setNamedParameter("isUnitCost", lineParameters.isUnitCost())
        .setNamedParameter("isBackdatedTrx", lineParameters.isBackdatedTransaction())
        .setNamedParameter("isNegativeCorrection", lineParameters.isNegativeCorrection())
        .setMaxResult(1)
        .uniqueResult();
  }

  public static boolean isNeededBackdatedCostAdjustment(final MaterialTransaction transaction,
      final boolean includeWarehouseDimension, final Date startingDate) {
    TransactionLast lastTransaction = CostAdjustmentUtils.getLastTransaction(transaction,
        includeWarehouseDimension);
    if (lastTransaction == null) {
      lastTransaction = CostAdjustmentUtils.insertLastTransaction(transaction,
          includeWarehouseDimension);
    }

    return (lastTransaction != null && CostAdjustmentUtils.compareToLastTransaction(transaction,
        lastTransaction, startingDate) < 0);
  }

  public static TransactionLast getLastTransaction(final MaterialTransaction trx,
      final boolean includeWarehouseDimension) {
    final Organization orgLegal = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId())
        .getLegalEntity(trx.getOrganization());
    final OBCriteria<TransactionLast> obc = OBDal.getInstance()
        .createCriteria(TransactionLast.class)
        .add(Restrictions.eq(TransactionLast.PROPERTY_PRODUCT, trx.getProduct()))
        .add(Restrictions.eq(TransactionLast.PROPERTY_ORGANIZATION, orgLegal));
    if (includeWarehouseDimension) {
      obc.add(
          Restrictions.eq(TransactionLast.PROPERTY_WAREHOUSE, trx.getStorageBin().getWarehouse()));
    }
    return (TransactionLast) obc.setMaxResults(1).uniqueResult();
  }

  public static int compareToLastTransaction(final MaterialTransaction trx,
      final TransactionLast lastTransaction, final Date startingDate) {
    final MaterialTransaction lastTrx = lastTransaction.getInventoryTransaction();

    // If trx is the same as lastTransaction or is from previous costing rule, return 0
    if (trx.getId().equals(lastTrx.getId())
        || lastTrx.getTransactionProcessDate().compareTo(startingDate) <= 0) {
      return 0;
    }

    final int compareMovementDate = DateUtils.truncate(trx.getMovementDate(), Calendar.DATE)
        .compareTo(DateUtils.truncate(lastTrx.getMovementDate(), Calendar.DATE));
    final int compareProcessDate = trx.getTransactionProcessDate()
        .compareTo(lastTrx.getTransactionProcessDate());
    final Long trxPrio = CostAdjustmentUtils.getTrxTypePrio(trx.getMovementType());
    final Long lastPrio = CostAdjustmentUtils.getTrxTypePrio(lastTrx.getMovementType());
    final int comparePriority = trxPrio.compareTo(lastPrio);
    final int compareQty = trx.getMovementQuantity().compareTo(lastTrx.getMovementQuantity());

    // If trx was processed after lastTrx
    if (compareProcessDate > 0 || (compareProcessDate == 0
        && (comparePriority > 0 || (comparePriority == 0 && compareQty <= 0)))) {
      if (compareMovementDate < 0) {
        // Before
        return -1;
      } else {
        // After
        return 1;
      }
    }

    return 0;
  }

  private static TransactionLast insertLastTransaction(final MaterialTransaction trx,
      final boolean includeWarehouseDimension) {
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    final Organization orgLegal = osp.getLegalEntity(trx.getOrganization());
    final Set<String> orgs = osp.getChildTree(orgLegal.getId(), true);

    //@formatter:off
    String hql =
            "select trx.id" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (includeWarehouseDimension) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "    , ADList as trxtype" +
            " where trxtype.searchKey = trx.movementType" +
            "   and trxtype.reference.id = :refId" +
            "   and trx.product.id = :productId" +
            "   and trx.organization.id in (:orgIds)";
    //@formatter:on
    if (includeWarehouseDimension) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.isCostCalculated = true" +
            " order by trx.movementDate desc" +
            "   , trx.transactionProcessDate desc" +
            "   , trxtype.sequenceNumber desc" +
            "   , trx.movementQuantity asc" +
            "   , trx.id desc";
    //@formatter:on

    final Query<String> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("refId", CostAdjustmentUtils.MovementTypeRefID)
        .setParameter("productId", trx.getProduct().getId())
        .setParameterList("orgIds", orgs);

    if (includeWarehouseDimension) {
      trxQry.setParameter("warehouseId", trx.getStorageBin().getWarehouse().getId());
    }

    final String transactionId = trxQry.setMaxResults(1).uniqueResult();

    TransactionLast lastTransaction = null;
    if (transactionId != null) {
      final MaterialTransaction transaction = OBDal.getInstance()
          .get(MaterialTransaction.class, transactionId);
      lastTransaction = OBProvider.getInstance().get(TransactionLast.class);
      lastTransaction.setClient(transaction.getClient());
      lastTransaction.setOrganization(orgLegal);
      lastTransaction.setInventoryTransaction(transaction);
      lastTransaction.setProduct(transaction.getProduct());
      if (includeWarehouseDimension) {
        lastTransaction.setWarehouse(transaction.getStorageBin().getWarehouse());
      }
      OBDal.getInstance().save(lastTransaction);
      OBDal.getInstance().flush();
    }

    return lastTransaction;
  }

  private static Long getNewLineNo(final CostAdjustment cadj) {
    //@formatter:off
    final String hql =
                  "select max(lineNo)" +
                  "  from CostAdjustmentLine as cal" +
                  " where cal.costAdjustment.id = :costAdjustment";
    //@formatter:on

    final Long lineNo = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Long.class)
        .setParameter("costAdjustment", cadj.getId())
        .setMaxResults(1)
        .uniqueResult();

    if (lineNo != null) {
      return lineNo + 10L;
    }
    return 10L;
  }

  public static BigDecimal getTrxCost(final MaterialTransaction trx, final boolean justUnitCost,
      final Currency currency) {
    if (!trx.isCostCalculated()) {
      // Transaction hasn't been calculated yet.
      log4j.error("  *** No cost found for transaction {} with id {}", trx.getIdentifier(),
          trx.getId());
      throw new OBException("@NoCostFoundForTrxOnDate@ @Transaction@: " + trx.getIdentifier());
    }

    //@formatter:off
    String hql =
            "select sum(tc.cost) as cost" +
            "  , tc.currency.id as currency" +
            "  , tc.costDate as date" +
            "  from TransactionCost as tc" +
            " where tc.inventoryTransaction.id = :trxId";
    //@formatter:on
    if (justUnitCost) {
      //@formatter:off
      hql +=
            "   and tc.unitCost = true";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " group by tc.currency" +
            " , tc.costDate";
    //@formatter:on

    final ScrollableResults scroll = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("trxId", trx.getId())
        .scroll(ScrollMode.FORWARD_ONLY);

    BigDecimal cost = BigDecimal.ZERO;
    try {
      while (scroll.next()) {
        final Object[] resultSet = scroll.get();
        final BigDecimal costAmt = (BigDecimal) resultSet[0];
        final String origCurId = (String) resultSet[1];

        if (StringUtils.equals(origCurId, currency.getId())) {
          cost = cost.add(costAmt);
        } else {
          final Currency origCur = OBDal.getInstance().get(Currency.class, origCurId);
          final Date convDate = (Date) resultSet[2];
          cost = cost.add(FinancialUtils.getConvertedAmount(costAmt, origCur, currency, convDate,
              trx.getOrganization(), FinancialUtils.PRECISION_COSTING));
        }
      }
    } finally {
      scroll.close();
    }
    return cost;
  }

  /**
   * Calculates the stock of the product on the given date and for the given cost dimensions. It
   * only takes transactions that have its cost calculated.
   */
  public static BigDecimal getStockOnMovementDate(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean backdatedTransactionsFixed) {
    // Get child tree of organizations.
    Date currentDate = date;
    final Set<String> orgs = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getChildTree(org.getId(), true);

    //@formatter:off
    String subSelectHql =
            "select min(case when coalesce(i.inventoryType, 'N') <> 'N' " + 
            "           then trx.movementDate " + 
            "           else trx.transactionProcessDate " + 
            "           end)" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      subSelectHql +=
            "   join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    subSelectHql +=
            "   left join trx.physicalInventoryLine as il" +
            "   left join il.physInventory as i" +
            " where trx.product.id = :productId" +
            "   and trx.movementDate > :date" +
    // Include only transactions that have its cost calculated
            "   and trx.isCostCalculated = true";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      subSelectHql +=
            "  and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    subSelectHql +=
            "   and trx.organization.id in (:orgs)";
    //@formatter:on

    final Query<Date> trxsubQry = OBDal.getInstance()
        .getSession()
        .createQuery(subSelectHql, Date.class)
        .setParameter("date", currentDate)
        .setParameter("productId", product.getId());

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      trxsubQry.setParameter("warehouseId", costDimensions.get(CostDimension.Warehouse).getId());
    }

    final Date trxprocessDate = trxsubQry.setParameterList("orgs", orgs).uniqueResult();

    //@formatter:off
    String hqlSelect =
            " select sum(trx.movementQuantity) as stock" +
            " from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hqlSelect +=
              "   join trx.storageBin as locator";
      //@formatter:on
    }

    Date backdatedTrxFrom = null;
    if (backdatedTransactionsFixed) {
      final CostingRule costRule = CostingUtils.getCostDimensionRule(org, currentDate);
      backdatedTrxFrom = CostingUtils.getCostingRuleFixBackdatedFrom(costRule);
    }

    if (trxprocessDate != null
        && (!backdatedTransactionsFixed || trxprocessDate.before(backdatedTrxFrom))) {
      currentDate = trxprocessDate;
      //@formatter:off
      hqlSelect +=
              "   left join trx.physicalInventoryLine as il" +
              "   left join il.physInventory as i" +
              " where case when coalesce(i.inventoryType, 'N') <> 'N' " + 
              "       then trx.movementDate " + 
              "       else trx.transactionProcessDate " + 
              "       end < :date";
      //@formatter:on
    } else {
      //@formatter:off
      hqlSelect +=
              " where trx.movementDate <= :date";
      //@formatter:on
    }

    // Include only transactions that have its cost calculated
    //@formatter:off
    hqlSelect +=
              "   and trx.product.id = :productId" +
              "   and trx.isCostCalculated = true";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hqlSelect +=
              "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hqlSelect +=
              "   and trx.organization.id in (:orgs)";
    //@formatter:on

    final Query<BigDecimal> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlSelect, BigDecimal.class)
        .setParameter("productId", product.getId())
        .setParameter("date", currentDate);

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId", costDimensions.get(CostDimension.Warehouse).getId());
    }

    final BigDecimal stock = trxQry.setParameterList("orgs", orgs).uniqueResult();
    return (stock != null) ? stock : BigDecimal.ZERO;
  }

  /**
   * Calculates the stock of the product on the given date and for the given cost dimensions. It
   * only takes transactions that have its cost calculated.
   */
  public static BigDecimal getStockOnTransactionDate(final Organization costorg,
      final MaterialTransaction trx, final HashMap<CostDimension, BaseOBObject> costDimensions,
      final boolean isManufacturingProduct, final boolean areBackdatedTrxFixed,
      final Currency currency) {
    final Date date = areBackdatedTrxFixed ? trx.getMovementDate()
        : trx.getTransactionProcessDate();
    final Costing costing = AverageAlgorithm.getLastCumulatedCosting(date, trx.getProduct(),
        costDimensions, costorg);
    return getStockOnTransactionDate(costorg, trx, costDimensions, isManufacturingProduct,
        areBackdatedTrxFixed, currency, costing);
  }

  /**
   * Calculates the stock of the product on the given date and for the given cost dimensions. It
   * only takes transactions that have its cost calculated.
   */
  public static BigDecimal getStockOnTransactionDate(final Organization costorg,
      final MaterialTransaction trx, final HashMap<CostDimension, BaseOBObject> costDimensions,
      final boolean isManufacturingProduct, final boolean areBackdatedTrxFixed,
      final Currency currency, final Costing costing) {

    // Get child tree of organizations.
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    Set<String> orgs = osp.getChildTree(costorg.getId(), true);
    HashMap<CostDimension, BaseOBObject> currentCostDimensions = costDimensions;
    if (isManufacturingProduct) {
      orgs = osp.getChildTree("0", false);
      currentCostDimensions = CostingUtils.getEmptyDimensions();
    }
    final CostingRule costingRule = CostingUtils.getCostDimensionRule(costorg,
        trx.getTransactionProcessDate());

    BigDecimal cumulatedStock = null;
    int costingPrecision = currency.getCostingPrecision().intValue();
    final MaterialTransaction ctrx = costing != null ? costing.getInventoryTransaction() : null;
    boolean existsCumulatedStockOnTrxDate = ctrx != null
        && costing.getTotalMovementQuantity() != null;

    // Backdated transactions can't use cumulated values
    if (existsCumulatedStockOnTrxDate && costingRule.isBackdatedTransactionsFixed()) {
      final Date trxMovementDate = DateUtils.truncate(trx.getMovementDate(), Calendar.DATE);
      final Date ctrxMovementDate = DateUtils.truncate(ctrx.getMovementDate(), Calendar.DATE);
      if (trxMovementDate.compareTo(ctrxMovementDate) < 0
          || (trxMovementDate.compareTo(ctrxMovementDate) == 0 && trx.getTransactionProcessDate()
              .compareTo(ctrx.getTransactionProcessDate()) <= 0)) {
        existsCumulatedStockOnTrxDate = false;
      }
    }

    if (existsCumulatedStockOnTrxDate) {
      cumulatedStock = costing.getTotalMovementQuantity();
      if (StringUtils.equals(ctrx.getId(), trx.getId())) {
        return cumulatedStock.setScale(costingPrecision, RoundingMode.HALF_UP);
      }
    }

    //@formatter:off
    String hql =
            "select sum(trx.movementQuantity) as stock" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "    , ADList as trxtype" +
            " where trxtype.reference.id = :refid" +
            "   and trxtype.searchKey = trx.movementType" +
            "   and trx.product.id = :productId" +
    // Include only transactions that have its cost calculated. Should be all.
            "   and trx.isCostCalculated = true";
    //@formatter:on

    if (existsCumulatedStockOnTrxDate) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   and (trx.movementDate > :cmvtdate" +
            "   or (trx.movementDate = :cmvtdate";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   and (trx.transactionProcessDate > :ctrxdate" +
            "   or (trx.transactionProcessDate = :ctrxdate";
      //@formatter:on
      // If the costing Transaction is an M- exclude the M+ Transactions with same movementDate and
      // TrxProcessDate due to how data is going to be ordered in further queries using the priority
      if (costing.getInventoryTransaction().getMovementType().equals("M-")) {
        //@formatter:off
        hql +=
            "   and (( trx.movementType <> 'M+' and trxtype.sequenceNumber > :ctrxtypeprio)";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and (trxtype.sequenceNumber > :ctrxtypeprio";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   or (trxtype.sequenceNumber = :ctrxtypeprio" +
            "   and (trx.movementQuantity < :ctrxqty" +
            "   or (trx.movementQuantity = :ctrxqty" +
            "   and trx.id > :ctrxid" +
            "   ))))))";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   ))";
        //@formatter:on
      }
    }
    //@formatter:off
    hql +=
            "   and ( ";
    //@formatter:on

    if (costingRule.isBackdatedTransactionsFixed()) {
      //@formatter:off
      hql +=
            "   (trx.transactionProcessDate < :fixbdt" +
            "   and  (";
      //@formatter:on
    }

    // If there are more than one trx on the same trx process date filter out those types with less
    // priority and / or higher quantity.
    //@formatter:off
    hql +=
            "   trx.transactionProcessDate < :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber < :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity > :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id <= :trxid" +
            "   ))))";
    //@formatter:on

    if (costingRule.isBackdatedTransactionsFixed()) {
      //@formatter:off
      hql +=
            "   )) or (" +
            "   trx.transactionProcessDate >= :fixbdt" +
            "   and (trx.movementDate < :mvtdate" +
            "   or (trx.movementDate = :mvtdate" +
      // If there are more than one trx on the same trx process date filter out those types with
      // less priority and / or higher quantity.
            "   and (trx.transactionProcessDate < :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber < :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity > :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id <= :trxid" +
            "   ))))" +
            "   ))))";
      //@formatter:on
    }

    //@formatter:off
    hql +=
            "   )";
    //@formatter:on

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }

    //@formatter:off
    hql +=
            "   and trx.organization.id in (:orgs)";
    //@formatter:on

    final Query<BigDecimal> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("refid", MovementTypeRefID)
        .setParameter("productId", trx.getProduct().getId())
        .setParameter("trxdate", trx.getTransactionProcessDate())
        .setParameter("trxtypeprio", getTrxTypePrio(trx.getMovementType()))
        .setParameter("trxqty", trx.getMovementQuantity())
        .setParameter("trxid", trx.getId())
        .setParameterList("orgs", orgs);

    if (existsCumulatedStockOnTrxDate) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        trxQry.setParameter("cmvtdate", ctrx.getMovementDate());
      }
      trxQry.setParameter("ctrxdate", ctrx.getTransactionProcessDate())
          .setParameter("ctrxtypeprio", getTrxTypePrio(ctrx.getMovementType()))
          .setParameter("ctrxqty", ctrx.getMovementQuantity())
          .setParameter("ctrxid", ctrx.getId());
    }

    if (costingRule.isBackdatedTransactionsFixed()) {
      trxQry.setParameter("mvtdate", trx.getMovementDate())
          .setParameter("fixbdt", CostingUtils.getCostingRuleFixBackdatedFrom(costingRule));
    }

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId",
          currentCostDimensions.get(CostDimension.Warehouse).getId());
    }

    BigDecimal stock = trxQry.uniqueResult();
    if (stock == null) {
      stock = BigDecimal.ZERO;
    }
    if (existsCumulatedStockOnTrxDate) {
      stock = stock.add(cumulatedStock);
    }
    return stock.setScale(costingPrecision, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getValuedStockOnMovementDate(final Product product,
      final Organization org, final Date date,
      final HashMap<CostDimension, BaseOBObject> costDimensions, final Currency currency,
      final boolean backdatedTransactionsFixed) {
    return getValuedStockOnMovementDateByAttrAndLocator(product, org, date, costDimensions, null,
        null, currency, backdatedTransactionsFixed);
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getValuedStockOnMovementDateByAttrAndLocator(final Product product,
      final Organization org, final Date date,
      final HashMap<CostDimension, BaseOBObject> costDimensions, final Locator locator,
      final AttributeSetInstance asi, final Currency currency,
      final boolean backdatedTransactionsFixed) {
    Date currentDate = date;
    HashMap<CostDimension, BaseOBObject> currentCostDimensions = costDimensions;

    // Get child tree of organizations.
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(org.getClient().getId());
    Set<String> orgs = osp.getChildTree(org.getId(), true);
    if (product.isProduction()) {
      orgs = osp.getChildTree("0", false);
      currentCostDimensions = CostingUtils.getEmptyDimensions();
    }

    //@formatter:off
    String subSelectHql =
        "select min(case when coalesce(i.inventoryType, 'N') <> 'N' " + 
        "           then trx.movementDate " + 
        "           else trx.transactionProcessDate " + 
        "           end)" +
        "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      subSelectHql +=
        "    join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    subSelectHql +=
        "   left join trx.physicalInventoryLine as il" +
        "   left join il.physInventory as i" +
        " where trx.product.id = :productId" +
        "   and trx.movementDate > :date" +
    // Include only transactions that have its cost calculated
        "   and trx.isCostCalculated = true";
    //@formatter:on

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      subSelectHql +=
        "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    subSelectHql +=
        "   and trx.organization.id in (:orgs)";
    //@formatter:on

    final Query<Date> trxsubQry = OBDal.getInstance()
        .getSession()
        .createQuery(subSelectHql, Date.class)
        .setParameter("date", currentDate)
        .setParameter("productId", product.getId());
    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      trxsubQry.setParameter("warehouseId",
          currentCostDimensions.get(CostDimension.Warehouse).getId());
    }
    trxsubQry.setParameterList("orgs", orgs);
    final Date trxprocessDate = trxsubQry.uniqueResult();

    //@formatter:off
    String hqlSelect =
            "select sum(case when trx.movementQuantity < 0 " + 
            "           then -tc.cost " + 
            "           else tc.cost " + 
            "           end ) as cost" +
            " , tc.currency.id as currency" +
            " , tc.accountingDate as mdate" +
            " , sum(trx.movementQuantity) as stock" +
            "  from TransactionCost as tc" +
            "    join tc.inventoryTransaction as trx";
    //@formatter:on
    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hqlSelect +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }

    Date backdatedTrxFrom = null;
    if (backdatedTransactionsFixed) {
      CostingRule costRule = CostingUtils.getCostDimensionRule(org, currentDate);
      backdatedTrxFrom = CostingUtils.getCostingRuleFixBackdatedFrom(costRule);
    }

    if (trxprocessDate != null
        && (!backdatedTransactionsFixed || trxprocessDate.before(backdatedTrxFrom))) {
      currentDate = trxprocessDate;
      //@formatter:off
      hqlSelect +=
            "    left join trx.physicalInventoryLine as il" +
            "    left join il.physInventory as i" +
            " where case when coalesce(i.inventoryType, 'N') <> 'N' " + 
            "       then trx.movementDate " + 
            "       else trx.transactionProcessDate " + 
            "       end < :date";
      //@formatter:on
    } else {
      //@formatter:off
      hqlSelect +=
            " where trx.movementDate <= :date";
      //@formatter:on
    }
    //@formatter:off
    hqlSelect +=
            "   and trx.product.id = :productId" +
    // Include only transactions that have its cost calculated
            "   and trx.isCostCalculated = true";
    //@formatter:on

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hqlSelect +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    if (locator != null) {
      //@formatter:off
      hqlSelect +=
            "   and trx.storageBin.id = :locatorId";
      //@formatter:on
    }
    if (asi != null) {
      //@formatter:off
      hqlSelect +=
            "   and trx.attributeSetValue.id = :asiId";
      //@formatter:on
    }
    //@formatter:off
    hqlSelect +=
            "   and trx.organization.id in (:orgIds)" +
            " group by tc.currency" + 
            "   , tc.accountingDate";
    //@formatter:on

    final Query<Object[]> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlSelect, Object[].class)
        .setParameter("productId", product.getId())
        .setParameter("date", currentDate);

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId",
          currentCostDimensions.get(CostDimension.Warehouse).getId());
    }
    if (locator != null) {
      trxQry.setParameter("locatorId", locator.getId());
    }
    if (asi != null) {
      trxQry.setParameter("asiId", asi.getId());
    }
    trxQry.setParameterList("orgIds", orgs);

    final ScrollableResults scroll = trxQry.scroll(ScrollMode.FORWARD_ONLY);
    BigDecimal sum = BigDecimal.ZERO;
    try {
      while (scroll.next()) {
        final Object[] resultSet = scroll.get();
        final BigDecimal origAmt = (BigDecimal) resultSet[0];
        final String origCurId = (String) resultSet[1];

        if (StringUtils.equals(origCurId, currency.getId())) {
          sum = sum.add(origAmt);
        } else {
          final Currency origCur = OBDal.getInstance().get(Currency.class, origCurId);
          final Date convDate = (Date) resultSet[2];
          sum = sum.add(FinancialUtils.getConvertedAmount(origAmt, origCur, currency, convDate, org,
              FinancialUtils.PRECISION_COSTING));
        }
      }
    } finally {
      scroll.close();
    }
    return sum;
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getValuedStockOnTransactionDate(final Organization costorg,
      final MaterialTransaction trx, final HashMap<CostDimension, BaseOBObject> costDimensions,
      final boolean isManufacturingProduct, final boolean areBackdatedTrxFixed,
      final Currency currency) {
    final Date date = areBackdatedTrxFixed ? trx.getMovementDate()
        : trx.getTransactionProcessDate();
    final Costing costing = AverageAlgorithm.getLastCumulatedCosting(date, trx.getProduct(),
        costDimensions, costorg);
    return getValuedStockOnTransactionDate(costorg, trx, costDimensions, isManufacturingProduct,
        areBackdatedTrxFixed, currency, costing);
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getValuedStockOnTransactionDate(final Organization costorg,
      final MaterialTransaction trx, final HashMap<CostDimension, BaseOBObject> costDimensions,
      boolean isManufacturingProduct, boolean areBackdatedTrxFixed, final Currency currency,
      final Costing costing) {

    // Get child tree of organizations.
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    Set<String> orgs = osp.getChildTree(costorg.getId(), true);
    HashMap<CostDimension, BaseOBObject> currentCostDimensions = costDimensions;
    if (isManufacturingProduct) {
      orgs = osp.getChildTree("0", false);
      currentCostDimensions = CostingUtils.getEmptyDimensions();
    }
    final CostingRule costingRule = CostingUtils.getCostDimensionRule(costorg,
        trx.getTransactionProcessDate());

    BigDecimal cumulatedValuation = null;
    int costingPrecision = currency.getCostingPrecision().intValue();
    final MaterialTransaction ctrx = costing != null ? costing.getInventoryTransaction() : null;
    boolean existsCumulatedValuationOnTrxDate = ctrx != null
        && costing.getTotalStockValuation() != null;

    // Backdated transactions can't use cumulated values
    if (existsCumulatedValuationOnTrxDate && costingRule.isBackdatedTransactionsFixed()) {
      final Date trxMovementDate = DateUtils.truncate(trx.getMovementDate(), Calendar.DATE);
      final Date ctrxMovementDate = DateUtils.truncate(ctrx.getMovementDate(), Calendar.DATE);
      if (trxMovementDate.compareTo(ctrxMovementDate) < 0
          || (trxMovementDate.compareTo(ctrxMovementDate) == 0 && trx.getTransactionProcessDate()
              .compareTo(ctrx.getTransactionProcessDate()) <= 0)) {
        existsCumulatedValuationOnTrxDate = false;
      }
    }

    if (existsCumulatedValuationOnTrxDate) {
      cumulatedValuation = costing.getTotalStockValuation();
      if (!StringUtils.equals(costing.getCurrency().getId(), currency.getId())) {
        cumulatedValuation = FinancialUtils.getConvertedAmount(cumulatedValuation,
            costing.getCurrency(), currency, ctrx.getTransactionProcessDate(), costorg,
            FinancialUtils.PRECISION_COSTING);
      }
      if (StringUtils.equals(ctrx.getId(), trx.getId())) {
        return cumulatedValuation.setScale(costingPrecision, RoundingMode.HALF_UP);
      }
    }

    //@formatter:off
    String hql =
            "select sum(case when trx.movementQuantity  < 0 " + 
            "           then -tc.cost " + 
            "           else tc.cost " + 
            "           end ) as cost" +
            " , tc.currency.id as currency" +
            " , tc.accountingDate as mdate" +
            " , sum(trx.movementQuantity) as stock" +
            "  from TransactionCost as tc" +
            "    join tc.inventoryTransaction as trx";
    //@formatter:on
    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "    , ADList as trxtype" +
            " where trxtype.reference.id = :refid" +
            "   and trxtype.searchKey = trx.movementType" +
            "   and trx.product.id = :productId" +
    // Include only transactions that have its cost calculated
            "   and trx.isCostCalculated = true";
    //@formatter:on
    if (existsCumulatedValuationOnTrxDate) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   and (trx.movementDate > :cmvtdate" +
            "   or (trx.movementDate = :cmvtdate";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   and (trx.transactionProcessDate > :ctrxdate" +
            "   or (trx.transactionProcessDate = :ctrxdate";
      //@formatter:on
      // If the costing Transaction is an M- exclude the M+ Transactions with same movementDate and
      // TrxProcessDate due to how data is going to be ordered in further queries using the priority
      if (costing.getInventoryTransaction().getMovementType().equals("M-")) {
        //@formatter:off
        hql +=
            "   and (( trx.movementType <> 'M+' and trxtype.sequenceNumber > :ctrxtypeprio)";
        //@formatter:on
      } else {
        //@formatter:off
        hql +=
            "   and (trxtype.sequenceNumber > :ctrxtypeprio";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   or (trxtype.sequenceNumber = :ctrxtypeprio" +
            "   and (trx.movementQuantity < :ctrxqty" +
            "   or (trx.movementQuantity = :ctrxqty" +
            "   and trx.id > :ctrxid" +
            "   ))))))";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   ))";
        //@formatter:on
      }
    }
    //@formatter:off
    hql +=
            "   and (";
    //@formatter:on

    if (costingRule.isBackdatedTransactionsFixed()) {
      //@formatter:off
      hql +=
            "   ( trx.transactionProcessDate < :fixbdt" +
            "   and (";
      //@formatter:on
    }
    // If there are more than one trx on the same trx process date filter out those types with less
    // priority and / or higher quantity.
    //@formatter:off
    hql +=
            "   trx.transactionProcessDate < :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber < :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity > :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id <= :trxid" +
            "   ))))";
    //@formatter:on

    if (costingRule.isBackdatedTransactionsFixed()) {
      //@formatter:off
      hql +=
            "   )) or (" +
            "   trx.transactionProcessDate >= :fixbdt" +
            "   and (trx.movementDate < :mvtdate" +
            "   or (trx.movementDate = :mvtdate" +
      // If there are more than one trx on the same trx process date filter out those types with
      // less priority and / or higher quantity.
            "   and (trx.transactionProcessDate < :trxdate" +
            "   or (trx.transactionProcessDate = :trxdate" +
            "   and (trxtype.sequenceNumber < :trxtypeprio" +
            "   or (trxtype.sequenceNumber = :trxtypeprio" +
            "   and trx.movementQuantity > :trxqty" +
            "   or (trx.movementQuantity = :trxqty" +
            "   and trx.id <= :trxid" +
            "   ))))))" +
            "   ))";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   )";
    //@formatter:on

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.organization.id in (:orgIds)" +
            " group by tc.currency" +
            "   , tc.accountingDate";
    //@formatter:on

    final Query<Object[]> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("refid", MovementTypeRefID)
        .setParameter("productId", trx.getProduct().getId())
        .setParameter("trxdate", trx.getTransactionProcessDate())
        .setParameter("trxtypeprio", getTrxTypePrio(trx.getMovementType()))
        .setParameter("trxqty", trx.getMovementQuantity())
        .setParameter("trxid", trx.getId())
        .setParameterList("orgIds", orgs);

    if (existsCumulatedValuationOnTrxDate) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        trxQry.setParameter("cmvtdate", ctrx.getMovementDate());
      }
      trxQry.setParameter("ctrxdate", ctrx.getTransactionProcessDate())
          .setParameter("ctrxtypeprio", getTrxTypePrio(ctrx.getMovementType()))
          .setParameter("ctrxqty", ctrx.getMovementQuantity())
          .setParameter("ctrxid", ctrx.getId());
    }

    if (costingRule.isBackdatedTransactionsFixed()) {
      trxQry.setParameter("mvtdate", trx.getMovementDate())
          .setParameter("fixbdt", CostingUtils.getCostingRuleFixBackdatedFrom(costingRule));
    }

    if (currentCostDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId",
          currentCostDimensions.get(CostDimension.Warehouse).getId());
    }

    ScrollableResults scroll = trxQry.scroll(ScrollMode.FORWARD_ONLY);
    BigDecimal sum = BigDecimal.ZERO;
    try {
      while (scroll.next()) {
        Object[] resultSet = scroll.get();
        BigDecimal origAmt = (BigDecimal) resultSet[0];
        String origCurId = (String) resultSet[1];

        if (StringUtils.equals(origCurId, currency.getId())) {
          sum = sum.add(origAmt);
        } else {
          Currency origCur = OBDal.getInstance().get(Currency.class, origCurId);
          Date convDate = (Date) resultSet[2];
          sum = sum.add(FinancialUtils.getConvertedAmount(origAmt, origCur, currency, convDate,
              costorg, FinancialUtils.PRECISION_COSTING));
        }
      }
    } finally {
      scroll.close();
    }

    if (existsCumulatedValuationOnTrxDate) {
      sum = sum.add(cumulatedValuation);
    }
    return sum.setScale(costingPrecision, RoundingMode.HALF_UP);
  }

  /**
   * Returns the last transaction process date of a non backdated transactions for the given
   * movement date or previous date.
   */
  public static Date getLastTrxDateOfMvmntDate(final Date refDate, final Product product,
      final Organization org, final HashMap<CostDimension, BaseOBObject> costDimensions) {
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(org.getClient().getId());
    Set<String> orgs = osp.getChildTree(org.getId(), true);
    Warehouse wh = (Warehouse) costDimensions.get(CostDimension.Warehouse);

    // Calculate the transaction process date of the first transaction with a movement date
    // after the given date. Any transaction with a transaction process date after this min date on
    // the given date or before is a backdated transaction.

    //@formatter:off
    String hql =
            "select min(trx.transactionProcessDate) as date" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (wh != null) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as loc";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " where trx.isCostCalculated = true" +
            "   and trx.organization.id in (:orgIds)" +
            "   and trx.product.id = :productId" +
            "   and trx.movementDate > :mvntdate";
    //@formatter:on

    if (wh != null) {
      //@formatter:off
      hql +=
            "   and loc.warehouse.id = :warehouseId";
    //@formatter:on
    }

    Query<Date> qryMinDate = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Date.class)
        .setParameterList("orgIds", orgs)
        .setParameter("productId", product.getId())
        .setParameter("mvntdate", refDate);

    if (wh != null) {
      qryMinDate.setParameter("warehouseId", wh.getId());
    }
    Date minNextDate = qryMinDate.uniqueResult();
    if (minNextDate == null) {
      return null;
    }

    // Get the last transaction process date of transactions with movement date equal or before the
    // given date and a transaction process date before the previously calculated min date.
    //@formatter:off
    String hqlSelect =
            "select max(trx.transactionProcessDate) as date" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on
    if (wh != null) {
      //@formatter:off
      hqlSelect +=
            "    join trx.storageBin as loc";
      //@formatter:on
    }
    //@formatter:off
    hqlSelect +=
            " where trx.isCostCalculated = true" +
            "   and trx.organization.id in (:orgIds)" +
            "   and trx.product.id = :productId" +
            "   and trx.movementDate <= :mvntdate" +
            "   and trx.transactionProcessDate < :trxdate";
    //@formatter:on
    if (wh != null) {
      //@formatter:off
      hqlSelect +=
            "   and loc.warehouse.id = :warehouseId";
      //@formatter:on
    }
    final Query<Date> qryMaxDate = OBDal.getInstance()
        .getSession()
        .createQuery(hqlSelect, Date.class)
        .setParameterList("orgIds", orgs)
        .setParameter("productId", product.getId())
        .setParameter("mvntdate", refDate)
        .setParameter("trxdate", minNextDate);

    if (wh != null) {
      qryMaxDate.setParameter("warehouseId", wh.getId());
    }

    return qryMaxDate.uniqueResult();
  }

  /**
   * Returns the priority of the given movementType.
   */
  public static long getTrxTypePrio(final String mvmntType) {
    final OBCriteria<org.openbravo.model.ad.domain.List> crList = OBDal.getInstance()
        .createCriteria(org.openbravo.model.ad.domain.List.class);
    crList.createAlias(propADListReference, "ref");
    crList.add(Restrictions.eq("ref.id", MovementTypeRefID));
    crList.add(Restrictions.eq(propADListValue, mvmntType));
    return ((org.openbravo.model.ad.domain.List) crList.uniqueResult()).getSequenceNumber();
  }
}
