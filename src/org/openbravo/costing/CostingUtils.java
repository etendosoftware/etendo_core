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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.costing.CostingAlgorithm.CostDimension;
import org.openbravo.costing.CostingServer.TrxType;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.businesspartner.BusinessPartner;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.order.OrderLine;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.materialmgmt.cost.Costing;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.openbravo.service.db.DalConnectionProvider;

public class CostingUtils {
  private static Logger log4j = LogManager.getLogger();
  public static final String propADListPriority = org.openbravo.model.ad.domain.List.PROPERTY_SEQUENCENUMBER;
  public static final String propADListValue = org.openbravo.model.ad.domain.List.PROPERTY_SEARCHKEY;
  public static final String propADListReference = org.openbravo.model.ad.domain.List.PROPERTY_REFERENCE;
  public static final String MovementTypeRefID = "189";

  /**
   * Calls {@link #getTransactionCost(MaterialTransaction, Date, boolean, Currency)} setting the
   * calculateTrx flag to false.
   */
  public static BigDecimal getTransactionCost(MaterialTransaction transaction, Date date,
      Currency currency) {
    return getTransactionCost(transaction, date, false, currency);
  }

  /**
   * Calculates the total cost amount of a transaction including the cost adjustments done until the
   * given date.
   * 
   * @param transaction
   *          MaterialTransaction to get its cost.
   * @param date
   *          The Date it is desired to know the cost.
   * @param calculateTrx
   *          boolean flag to force the calculation of the transaction cost if it is not calculated.
   * @param currency
   *          The Currency to calculate the amount.
   * @return The total cost amount.
   */
  public static BigDecimal getTransactionCost(MaterialTransaction transaction, Date date,
      boolean calculateTrx, Currency currency) {
    log4j.debug("Get Transaction Cost");
    OBError result = new OBError();
    try {
      OBContext.setAdminMode(true);
      result.setType("Success");
      result.setTitle(OBMessageUtils.messageBD("Success"));
      if (!transaction.isCostCalculated()) {
        // Transaction hasn't been calculated yet.
        if (calculateTrx) {
          log4j.debug(
              "  *** Cost for transaction will be calculated." + transaction.getIdentifier());
          CostingServer transactionCost = new CostingServer(transaction);
          transactionCost.process();
          return transactionCost.getTransactionCost();
        }
        log4j.error("  *** No cost found for transaction " + transaction.getIdentifier()
            + " with id " + transaction.getId() + " on date " + OBDateUtils.formatDate(date));
        throw new OBException("@NoCostFoundForTrxOnDate@ @Transaction@: "
            + transaction.getIdentifier() + " @Date@ " + OBDateUtils.formatDate(date));
      }
      BigDecimal cost = BigDecimal.ZERO;
      for (TransactionCost trxCost : transaction.getTransactionCostList()) {
        if (!trxCost.getCostDate().after(date)) {
          cost = cost.add(FinancialUtils.getConvertedAmount(trxCost.getCost(),
              trxCost.getCurrency(), currency, trxCost.getCostDate(), trxCost.getOrganization(),
              FinancialUtils.PRECISION_COSTING));
        }
      }
      return cost;
    } catch (OBException e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error(result.getMessage(), e);
      return null;
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error(result.getMessage(), e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static BigDecimal getDefaultCost(Product product, BigDecimal qty, Organization org,
      Date costDate, Date movementDate, BusinessPartner bp, Currency currency,
      HashMap<CostDimension, BaseOBObject> costDimensions) {
    Costing stdCost = getStandardCostDefinition(product, org, costDate, costDimensions);
    PriceList pricelist = null;
    if (bp != null) {
      pricelist = bp.getPurchasePricelist();
    }
    ProductPrice pp = FinancialUtils.getProductPrice(product, movementDate, false, pricelist, false,
        false);
    if (stdCost == null && pp == null) {
      throw new OBException(
          "@NoPriceListOrStandardCostForProduct@ @Organization@: " + org.getName() + ", @Product@: "
              + product.getSearchKey() + ", @Date@: " + OBDateUtils.formatDate(costDate));
    } else if (stdCost != null && pp == null) {
      BigDecimal standardCost = getStandardCost(product, org, costDate, costDimensions, currency);
      return qty.abs().multiply(standardCost);
    } else if (stdCost == null && pp != null) {
      BigDecimal cost = pp.getStandardPrice().multiply(qty.abs());
      if (pp.getPriceListVersion().getPriceList().getCurrency().getId().equals(currency.getId())) {
        // no conversion needed
        return cost;
      }
      return FinancialUtils.getConvertedAmount(cost,
          pp.getPriceListVersion().getPriceList().getCurrency(), currency, movementDate, org,
          FinancialUtils.PRECISION_STANDARD);

    } else if (stdCost != null && pp != null
        && stdCost.getStartingDate().before(pp.getPriceListVersion().getValidFromDate())) {
      BigDecimal cost = pp.getStandardPrice().multiply(qty.abs());
      if (pp.getPriceListVersion().getPriceList().getCurrency().getId().equals(currency.getId())) {
        // no conversion needed
        return cost;
      }
      return FinancialUtils.getConvertedAmount(cost,
          pp.getPriceListVersion().getPriceList().getCurrency(), currency, movementDate, org,
          FinancialUtils.PRECISION_STANDARD);
    } else {
      BigDecimal standardCost = getStandardCost(product, org, costDate, costDimensions, currency);
      return qty.abs().multiply(standardCost);
    }
  }

  /**
   * Calls {@link #getStandardCost(Product, Organization, Date, HashMap, boolean, Currency)} setting
   * the recheckWithoutDimensions flag to true.
   */
  public static BigDecimal getStandardCost(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, Currency convCurrency)
      throws OBException {
    return getStandardCost(product, org, date, costDimensions, true, convCurrency);
  }

  /**
   * Calculates the standard cost of a product on the given date and cost dimensions.
   * 
   * @param product
   *          The Product to get its Standard Cost
   * @param date
   *          The Date to get the Standard Cost
   * @param costDimensions
   *          The cost dimensions to get the Standard Cost if it is defined by some of them.
   * @param recheckWithoutDimensions
   *          boolean flag to force a recall the method to get the Standard Cost at client level if
   *          no cost is found in the given cost dimensions.
   * @param convCurrency
   *          The Currency to calculate the amount.
   * @return the Standard Cost.
   * @throws OBException
   *           when no standard cost is found.
   */
  public static BigDecimal getStandardCost(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean recheckWithoutDimensions,
      Currency convCurrency) throws OBException {
    Costing stdCost = getStandardCostDefinition(product, org, date, costDimensions,
        recheckWithoutDimensions);
    if (stdCost == null) {
      // If no standard cost is found throw an exception.
      throw new OBException("@NoStandardCostDefined@ @Organization@:" + org.getName()
          + ", @Product@: " + product.getName() + ", @Date@: " + OBDateUtils.formatDate(date));
    }
    return FinancialUtils.getConvertedAmount(stdCost.getCost(), stdCost.getCurrency(), convCurrency,
        date, org, FinancialUtils.PRECISION_COSTING);
  }

  /**
   * Calls {@link #hasStandardCostDefinition(Product, Organization, Date, HashMap, boolean)} setting
   * the recheckWithoutDimensions flag to true.
   */
  public static boolean hasStandardCostDefinition(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions) {
    return hasStandardCostDefinition(product, org, date, costDimensions, true);
  }

  /**
   * Check the existence of a standard cost definition of a product on the given date and cost
   * dimensions.
   * 
   * @param product
   *          The Product to get its Standard Cost
   * @param date
   *          The Date to get the Standard Cost
   * @param costDimensions
   *          The cost dimensions to get the Standard Cost if it is defined by some of them.
   * @param recheckWithoutDimensions
   *          boolean flag to force a recall the method to get the Standard Cost at client level if
   *          no cost is found in the given cost dimensions.
   * @return the Standard Cost. Null when no definition is found.
   */
  public static boolean hasStandardCostDefinition(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean recheckWithoutDimensions) {
    return getStandardCostDefinition(product, org, date, costDimensions,
        recheckWithoutDimensions) != null;
  }

  /**
   * Calls {@link #getStandardCostDefinition(Product, Organization, Date, HashMap, boolean)} setting
   * the recheckWithoutDimensions flag to true.
   */
  public static Costing getStandardCostDefinition(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions) {
    return getStandardCostDefinition(product, org, date, costDimensions, true);
  }

  /**
   * Calculates the standard cost definition of a product on the given date and cost dimensions.
   * 
   * @param product
   *          The Product to get its Standard Cost
   * @param date
   *          The Date to get the Standard Cost
   * @param costDimensions
   *          The cost dimensions to get the Standard Cost if it is defined by some of them.
   * @param recheckWithoutDimensions
   *          boolean flag to force a recall the method to get the Standard Cost at client level if
   *          no cost is found in the given cost dimensions.
   * @return the Standard Cost. Null when no definition is found.
   */
  public static Costing getStandardCostDefinition(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean recheckWithoutDimensions) {
    Costing stdCost = getStandardCostDefinition(product, org, date, costDimensions,
        recheckWithoutDimensions, "STA");
    if (stdCost != null) {
      return stdCost;
    } else {
      // If no cost is found, search valid legacy cost
      return getStandardCostDefinition(product, org, date, costDimensions, recheckWithoutDimensions,
          "ST");
    }
  }

  /**
   * Calculates the standard cost definition of a product on the given date and cost dimensions.
   * 
   * @param product
   *          The Product to get its Standard Cost
   * @param date
   *          The Date to get the Standard Cost
   * @param costDimensions
   *          The cost dimensions to get the Standard Cost if it is defined by some of them.
   * @param recheckWithoutDimensions
   *          boolean flag to force a recall the method to get the Standard Cost at client level if
   *          no cost is found in the given cost dimensions.
   * @return the Standard Cost. Null when no definition is found.
   */
  public static Costing getStandardCostDefinition(Product product, Organization org, Date date,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean recheckWithoutDimensions,
      String costtype) {
    // Get cost from M_Costing for given date.
    OBCriteria<Costing> obcCosting = OBDal.getInstance()
        .createCriteria(Costing.class)
        .add(Restrictions.eq(Costing.PROPERTY_PRODUCT, product))
        .add(Restrictions.le(Costing.PROPERTY_STARTINGDATE, date))
        .add(Restrictions.gt(Costing.PROPERTY_ENDINGDATE, date))
        .add(Restrictions.eq(Costing.PROPERTY_COSTTYPE, costtype))
        .add(Restrictions.isNotNull(Costing.PROPERTY_COST));

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      obcCosting.add(
          Restrictions.eq(Costing.PROPERTY_WAREHOUSE, costDimensions.get(CostDimension.Warehouse)));
    }

    List<Costing> obcCostingList = obcCosting
        .add(Restrictions.eq(Costing.PROPERTY_ORGANIZATION, org))
        .setFilterOnReadableOrganization(false)
        .setMaxResults(2)
        .list();

    int size = obcCostingList.size();
    if (size != 0) {
      if (size > 1) {
        log4j.warn("More than one cost found for same date: " + OBDateUtils.formatDate(date)
            + " for product: " + product.getName() + " (" + product.getId() + ")");
      }
      return obcCostingList.get(0);
    } else if (recheckWithoutDimensions) {
      return getStandardCostDefinition(product, org, date, getEmptyDimensions(), false);
    }
    return null;
  }

  /**
   * @return The costDimensions HashMap with null values for the dimensions.
   */
  public static HashMap<CostDimension, BaseOBObject> getEmptyDimensions() {
    HashMap<CostDimension, BaseOBObject> costDimensions = new HashMap<CostDimension, BaseOBObject>();
    costDimensions.put(CostDimension.Warehouse, null);
    return costDimensions;
  }

  /**
   * Calculates the stock of the product on the given date and for the given cost dimensions. It
   * only takes transactions that have its cost calculated.
   */
  public static BigDecimal getCurrentStock(MaterialTransaction trx, Organization org,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean areBackdatedTrxFixed,
      Currency currency) {
    Product product = trx.getProduct();
    Date date = areBackdatedTrxFixed ? trx.getMovementDate() : trx.getTransactionProcessDate();
    Costing costing = AverageAlgorithm.getLastCumulatedCosting(date, product, costDimensions, org);
    return getCurrentStock(product, org, trx.getTransactionProcessDate(), costDimensions, currency,
        costing);
  }

  /**
   * Calculates the stock of the product on the given date and for the given cost dimensions. It
   * only takes transactions that have its cost calculated.
   */
  public static BigDecimal getCurrentStock(Product product, Organization costorg, Date dateTo,
      HashMap<CostDimension, BaseOBObject> costDimensions, Currency currency, Costing costing) {
    // Get child tree of organizations.
    Set<String> orgs = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getChildTree(costorg.getId(), true);
    CostingRule costingRule = CostingUtils.getCostDimensionRule(costorg, dateTo);

    MaterialTransaction ctrx = costing != null ? costing.getInventoryTransaction() : null;
    boolean existsCumulatedStock = ctrx != null && costing.getTotalMovementQuantity() != null;

    //@formatter:off
    String hql =
            "select sum(trx.movementQuantity) as stock" +
            "  from MaterialMgmtMaterialTransaction as trx";
    //@formatter:on

    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }
    if (existsCumulatedStock) {
      //@formatter:off
      hql +=
            "    , ADList as trxtype";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " where trx.product.id = :productId" +
            "   and trx.transactionProcessDate <= :dateTo";
    //@formatter:on
    if (existsCumulatedStock) {
      //@formatter:off
      hql +=
            "   and trxtype.searchKey = trx.movementType" +
            "   and trxtype.reference.id = :refId";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   and  trx.transactionProcessDate >= :fixbdt" +
            "   and (trx.movementDate > :mvtdate" +
            "   or (trx.movementDate = :mvtdate";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   and (trx.transactionProcessDate > :dateFrom" +
            "   or (trx.transactionProcessDate = :dateFrom";
      //@formatter:on

      // If the costing Transaction is an M- exclude the M+ Transactions with same movementDate and
      // TrxProcessDate due to how data is going to be ordered in further queries using the priority
      if (costing.getInventoryTransaction().getMovementType().equals("M-")) {
        //@formatter:off
        hql +=
            "   and (( trx.movementType <> 'M+'" + 
            "   and trxtype.sequenceNumber > :ctrxtypeprio)";
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
            "   and trx.id > :ctrxId" +
            "   ))))))";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   ))";
        //@formatter:on
      }
    }
    // Include only transactions that have its cost calculated
    //@formatter:off
    hql +=
            "   and trx.isCostCalculated = true";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    if (product.isProduction()) {
      //@formatter:off
      hql +=
            "   and trx.client.id = :clientId";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and trx.organization.id in (:orgIds)";
      //@formatter:on
    }
    Query<BigDecimal> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, BigDecimal.class)
        .setParameter("productId", product.getId())
        .setParameter("dateTo", dateTo);

    if (existsCumulatedStock) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        trxQry.setParameter("fixbdt", getCostingRuleFixBackdatedFrom(costingRule))
            .setParameter("mvtdate", costing.getInventoryTransaction().getMovementDate());
      }
      trxQry.setParameter("refId", CostAdjustmentUtils.MovementTypeRefID)
          .setParameter("dateFrom", costing.getInventoryTransaction().getTransactionProcessDate())
          .setParameter("ctrxtypeprio", CostAdjustmentUtils.getTrxTypePrio(ctrx.getMovementType()))
          .setParameter("ctrxqty", ctrx.getMovementQuantity())
          .setParameter("ctrxId", ctrx.getId());
    }
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId", costDimensions.get(CostDimension.Warehouse).getId());
    }
    if (product.isProduction()) {
      trxQry.setParameter("clientId", product.getClient().getId());
    } else {
      trxQry.setParameterList("orgIds", orgs);
    }
    BigDecimal stock = trxQry.uniqueResult();
    if (stock == null) {
      stock = BigDecimal.ZERO;
    }
    if (existsCumulatedStock) {
      stock = stock.add(costing.getTotalMovementQuantity());
    }

    int costingPrecision = currency.getCostingPrecision().intValue();
    return stock.setScale(costingPrecision, RoundingMode.HALF_UP);
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getCurrentValuedStock(MaterialTransaction trx, Organization org,
      HashMap<CostDimension, BaseOBObject> costDimensions, boolean areBackdatedTrxFixed,
      Currency currency) {
    Product product = trx.getProduct();
    Date date = areBackdatedTrxFixed ? trx.getMovementDate() : trx.getTransactionProcessDate();
    Costing costing = AverageAlgorithm.getLastCumulatedCosting(date, product, costDimensions, org);
    return getCurrentValuedStock(product, org, trx.getTransactionProcessDate(), costDimensions,
        currency, costing);
  }

  /**
   * Calculates the value of the stock of the product on the given date, for the given cost
   * dimensions and for the given currency. It only takes transactions that have its cost
   * calculated.
   */
  public static BigDecimal getCurrentValuedStock(Product product, Organization costorg, Date dateTo,
      HashMap<CostDimension, BaseOBObject> costDimensions, Currency currency, Costing costing) {
    // Get child tree of organizations.
    Set<String> orgs = OBContext.getOBContext()
        .getOrganizationStructureProvider()
        .getChildTree(costorg.getId(), true);
    CostingRule costingRule = CostingUtils.getCostDimensionRule(costorg, dateTo);

    MaterialTransaction ctrx = costing != null ? costing.getInventoryTransaction() : null;
    boolean existsCumulatedValuation = ctrx != null && costing.getTotalStockValuation() != null;

    //@formatter:off
    String hql =
            "select sum(case when trx.movementQuantity < 0 " +
            "           then -tc.cost " +
            "           else tc.cost " +
            "           end ) as cost," +
            "  tc.currency.id as currency," +
            "  tc.accountingDate as mdate" +
            "  from TransactionCost as tc" +
            "    join tc.inventoryTransaction as trx";
    //@formatter:off
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "    join trx.storageBin as locator";
      //@formatter:on
    }
    if (existsCumulatedValuation) {
      //@formatter:off
      hql +=
            "    , ADList as trxtype";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " where trx.product.id = :productId" +
            "   and trx.transactionProcessDate <= :dateTo";
    //@formatter:on

    if (existsCumulatedValuation) {
      //@formatter:off
      hql +=
            "   and trxtype.searchKey = trx.movementType" +
            "   and trxtype.reference.id = :refId";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   and  trx.transactionProcessDate >= :fixbdt" +
            "   and (trx.movementDate > :mvtdate" +
            "   or (trx.movementDate = :mvtdate";
        //@formatter:on
      }
      //@formatter:off
      hql +=
            "   and (trx.transactionProcessDate > :dateFrom" +
            "   or (trx.transactionProcessDate = :dateFrom";
      //@formatter:on
      // If the costing Transaction is an M- exclude the M+ Transactions with same movementDate and
      // TrxProcessDate due to how data is going to be ordered in further queries using the priority
      if (costing.getInventoryTransaction().getMovementType().equals("M-")) {
        //@formatter:off
        hql +=
            "   and (( trx.movementType <> 'M+'" +
            "   and trxtype.sequenceNumber > :ctrxtypeprio)";
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
            "   and trx.id > :ctrxId" +
            "   ))))))";
      //@formatter:on
      if (costingRule.isBackdatedTransactionsFixed()) {
        //@formatter:off
        hql +=
            "   ))";
        //@formatter:on
      }
    }
    // Include only transactions that have its cost calculated
    //@formatter:off
    hql +=
            "   and trx.isCostCalculated = true";
    //@formatter:on
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      //@formatter:off
      hql +=
            "   and locator.warehouse.id = :warehouseId";
      //@formatter:on
    }
    if (product.isProduction()) {
      //@formatter:off
      hql +=
            "   and trx.client.id = :clientId";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "   and trx.organization.id in (:orgIds)";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " group by tc.currency" +
            "   , tc.accountingDate";
    //@formatter:on

    Query<Object[]> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("productId", product.getId())
        .setParameter("dateTo", dateTo);

    if (existsCumulatedValuation) {
      if (costingRule.isBackdatedTransactionsFixed()) {
        trxQry.setParameter("fixbdt", getCostingRuleFixBackdatedFrom(costingRule))
            .setParameter("mvtdate", costing.getInventoryTransaction().getMovementDate());
      }
      trxQry.setParameter("refId", CostAdjustmentUtils.MovementTypeRefID)
          .setParameter("dateFrom", costing.getInventoryTransaction().getTransactionProcessDate())
          .setParameter("ctrxtypeprio", CostAdjustmentUtils.getTrxTypePrio(ctrx.getMovementType()))
          .setParameter("ctrxqty", ctrx.getMovementQuantity())
          .setParameter("ctrxId", ctrx.getId());
    }
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      trxQry.setParameter("warehouseId", costDimensions.get(CostDimension.Warehouse).getId());
    }
    if (product.isProduction()) {
      trxQry.setParameter("clientId", product.getClient().getId());
    } else {
      trxQry.setParameterList("orgIds", orgs);
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

    if (existsCumulatedValuation) {
      BigDecimal costingValuedStock = costing.getTotalStockValuation();
      if (!StringUtils.equals(costing.getCurrency().getId(), currency.getId())) {
        costingValuedStock = FinancialUtils.getConvertedAmount(costingValuedStock,
            costing.getCurrency(), currency, costing.getStartingDate(), costorg,
            FinancialUtils.PRECISION_COSTING);
      }
      sum = sum.add(costingValuedStock);
    }

    int costingPrecision = currency.getCostingPrecision().intValue();
    return sum.setScale(costingPrecision, RoundingMode.HALF_UP);
  }

  public static BusinessPartner getTrxBusinessPartner(MaterialTransaction transaction,
      TrxType trxType) {
    switch (trxType) {
      case Receipt:
      case ReceiptNegative:
      case ReceiptReturn:
      case ReceiptVoid:
      case Shipment:
      case ShipmentNegative:
      case ShipmentReturn:
      case ShipmentVoid:
        return transaction.getGoodsShipmentLine().getShipmentReceipt().getBusinessPartner();
      default:
        return null;
    }
  }

  /**
   * Returns the newer order line for the given product, business partner and organization.
   */
  public static OrderLine getOrderLine(Product product, BusinessPartner bp, Organization org) {
    OrganizationStructureProvider osp = OBContext.getOBContext().getOrganizationStructureProvider();

    //@formatter:off
    final String hql =
            " as ol" +
            "   join ol.salesOrder as o" +
            "   join o.documentType as dt" +
            " where o.businessPartner.id = :bpId" +
            "   and ol.product.id = :productId" +
            "   and o.organization.id in :orgIds" +
            "   and o.documentStatus in ('CO', 'CL')" +
            "   and o.salesTransaction = false" +
            "   and dt.return = false" +
            " order by o.orderDate desc";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(OrderLine.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("bpId", bp.getId())
        .setNamedParameter("productId", product.getId())
        .setNamedParameter("orgIds", osp.getChildTree(org.getId(), true))
        .setMaxResult(1)
        .uniqueResult();
  }

  public static CostingRule getCostDimensionRule(Organization org, Date date) {
    //@formatter:off
    final String hql =
                  "   organization.id = :organizationId" +
                  "   and (startingDate is null " +
                  "   or startingDate <= :startdate)" +
                  "   and (endingDate is null" +
                  "   or endingDate >= :enddate )" +
                  "   and validated = true" +
                  " order by case when startingDate is null " +
                  "          then 1 " +
                  "          else 0 " +
                  "          end, startingDate desc";
    //@formatter:on

    CostingRule costRule = OBDal.getInstance()
        .createQuery(CostingRule.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("organizationId", org.getId())
        .setNamedParameter("startdate", date)
        .setNamedParameter("enddate", date)
        .setMaxResult(1)
        .uniqueResult();

    if (costRule == null) {
      throw new OBException("@NoCostingRuleFoundForOrganizationAndDate@ @Organization@: "
          + org.getName() + ", @Date@: " + OBDateUtils.formatDate(date));
    }
    return costRule;
  }

  /**
   * Returns the max transaction date with cost calculated
   */
  public static Date getMaxTransactionDate(Organization org) {
    // Get child tree of organizations.
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(org.getClient().getId());
    Set<String> orgs = osp.getChildTree(org.getId(), true);

    //@formatter:off
    final String hql =
                  "select max(trx.movementDate) as date" +
                  "  from MaterialMgmtMaterialTransaction as trx" +
                  " where trx.isCostCalculated = true" +
                  "   and trx.organization.id in (:orgIds)";
    //@formatter:on

    Date maxDate = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Date.class)
        .setParameterList("orgIds", orgs)
        .uniqueResult();

    if (maxDate != null) {
      return maxDate;
    }
    return null;
  }

  /**
   * Search period control closed between dateFrom and dateTo
   */
  public static Period periodClosed(Organization org, Date dateFrom, Date dateTo, String docType)
      throws ServletException {
    String strDateFormat = OBPropertiesProvider.getInstance()
        .getOpenbravoProperties()
        .getProperty("dateFormat.java");
    final SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);

    String strDateFrom = dateFormat.format(dateFrom);
    String strDateTo = dateFormat.format(dateTo);
    CostingUtilsData[] per = CostingUtilsData.periodClosed(new DalConnectionProvider(false),
        org.getId(), strDateFrom, strDateTo, org.getClient().getId(), docType);
    if (per.length > 0) {
      return OBDal.getInstance().get(Period.class, per[0].period);
    }
    return null;
  }

  /**
   * Returns the Starting Date of a Costing Rule, if is null returns 01/01/1900
   */
  public static Date getCostingRuleStartingDate(CostingRule rule) {
    if (rule.getStartingDate() == null) {
      SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
      try {
        return outputFormat.parse("01-01-1900");
      } catch (ParseException e) {
        // Error parsing the date.
        log4j.error("Error parsing the date.", e);
        return null;
      }
    }
    return rule.getStartingDate();
  }

  /**
   * Returns the Fix Backdated From of a Costing Rule, if is null returns 01/01/1900
   */
  public static Date getCostingRuleFixBackdatedFrom(CostingRule rule) {
    if (rule.getFixbackdatedfrom() == null) {
      SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
      try {
        return outputFormat.parse("01-01-1900");
      } catch (ParseException e) {
        // Error parsing the date.
        log4j.error("Error parsing the date.", e);
        return null;
      }
    }
    return rule.getFixbackdatedfrom();
  }

  /**
   * Throws an OBException when the processId is the CostingBackground and it is being executed by
   * an organization which has a legal entity as an ancestor.
   * 
   * @param processId
   *          This is the process Id being executed. The method only runs the validation when the
   *          process ID is equal to CostingBackground.AD_PROCESS_ID
   * @param scheduledOrg
   *          the organization that runs the process
   */
  public static void checkValidOrganization(final String processId,
      final Organization scheduledOrg) {
    if (StringUtils.equals(processId, CostingBackground.AD_PROCESS_ID)) {
      final Organization legalEntity = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getLegalEntity(scheduledOrg);
      if (legalEntity != null && !StringUtils.equals(legalEntity.getId(), scheduledOrg.getId())) {
        throw new OBException(OBMessageUtils.messageBD("CostBackgroundWrongOrganization"));
      }
    }
  }

  /**
   * Check if exists processed transactions for this product
   */
  public static boolean existsProcessedTransactions(Product product,
      HashMap<CostDimension, BaseOBObject> _costDimensions, Organization costorg,
      MaterialTransaction trx, boolean isManufacturingProduct) {

    // Get child tree of organizations.
    OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(trx.getClient().getId());
    Set<String> orgs = osp.getChildTree(costorg.getId(), true);
    HashMap<CostDimension, BaseOBObject> costDimensions = _costDimensions;
    if (isManufacturingProduct) {
      orgs = osp.getChildTree("0", false);
      costDimensions = CostingUtils.getEmptyDimensions();
    }

    OBCriteria<MaterialTransaction> criteria = OBDal.getInstance()
        .createCriteria(MaterialTransaction.class);
    criteria.createAlias(MaterialTransaction.PROPERTY_STORAGEBIN, "sb")
        .add(Restrictions.eq(MaterialTransaction.PROPERTY_PRODUCT, product))
        .add(Restrictions.eq(MaterialTransaction.PROPERTY_ISPROCESSED, true))
        .add(Restrictions.in(MaterialTransaction.PROPERTY_ORGANIZATION + ".id", orgs));
    if (costDimensions.get(CostDimension.Warehouse) != null) {
      criteria.add(Restrictions.eq("sb." + Locator.PROPERTY_WAREHOUSE + ".id",
          costDimensions.get(CostDimension.Warehouse).getId()));
    }

    return criteria.setFilterOnReadableOrganization(false).setMaxResults(1).uniqueResult() != null;
  }

  /**
   * Check if trx is the last opening one of an Inventory Amount Update
   */
  public static boolean isLastOpeningTransaction(MaterialTransaction trx,
      boolean includeWarehouseDimension) {

    //@formatter:off
    String hql =
            "select trx.id as trxid" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            "    join trx.physicalInventoryLine as il" +
            "    join il.physInventory as i" +
            "    join i.inventoryAmountUpdateLineInventoriesInitInventoryList as iaui" +
            "    join iaui.warehouse as w" +
            " where i.inventoryType = 'O'" +
            "   and iaui.caInventoryamtline.id = :inventoryAmountUpdateLineId";
    //@formatter:on
    if (includeWarehouseDimension) {
      //@formatter:off
      hql +=
            "   and iaui.warehouse.id = :warehouseId";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            " order by w.name desc" +
            "   , il.lineNo desc";
    //@formatter:on

    OBDal.getInstance().refresh(trx.getPhysicalInventoryLine().getPhysInventory());
    Query<String> qry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("inventoryAmountUpdateLineId",
            trx.getPhysicalInventoryLine()
                .getPhysInventory()
                .getInventoryAmountUpdateLineInventoriesInitInventoryList()
                .get(0)
                .getCaInventoryamtline()
                .getId());
    if (includeWarehouseDimension) {
      qry.setParameter("warehouseId", trx.getStorageBin().getWarehouse().getId());
    }
    return StringUtils.equals(trx.getId(), qry.setMaxResults(1).uniqueResult());
  }

  @Deprecated
  public static boolean isAllowNegativeStock(Client client) {
    try {
      OBContext.setAdminMode(true);
      return isNegativeStockEnabledForAnyStorageBin(client);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  public static boolean isNegativeStockEnabledForAnyStorageBin(Client client) {
    //@formatter:off
    final String hql =
                  "select c" +
                  "  from ADClient as c" +
                  " where exists (" +
                  "   select 1" +
                  "     from Locator as l" +
                  "       join l.inventoryStatus invs" +
                  "      where invs.overissue = true" +
                  "        and l.client.id = c.id"+
                  "   )" +
                  "   and c.id = :clientId";
    //@formatter:on

    return !OBDal.getInstance()
        .getSession()
        .createQuery(hql, Client.class)
        .setParameter("clientId", client.getId())
        .setMaxResults(1)
        .list()
        .isEmpty();
  }

  public static boolean isNegativeStockAllowedForShipmentInout(String shipmentInOutId) {
    //@formatter:off
    final String hql =
                  "select c" +
                  "  from ADClient as c" +
                  " where exists (" +
                  "   select 1" +
                  "     from MaterialMgmtShipmentInOutLine as iol" +
                  "       join iol.shipmentReceipt as io" +
                  "       join iol.storageBin as l" +
                  "       left join l.inventoryStatus invs" +
                  "    where invs.overissue = true" +
                  "      and io.id = :shipmentInOutID" +
                  "   )";
    //@formatter:on

    return !OBDal.getInstance()
        .getSession()
        .createQuery(hql, Client.class)
        .setParameter("shipmentInOutID", shipmentInOutId)
        .setMaxResults(1)
        .list()
        .isEmpty();
  }

  /**
   * For the given {@link Warehouse} parameter, this method returns an {@link Organization} that
   * allows to create Transactions. If the Organization of the Warehouse is valid, it returns this
   * Organization, if not, it looks for a valid one in the parent Organizations of the Warehouse
   * Organization. If no valid Organization is found, the Organization of the id given as a
   * parameter is returned.
   * 
   * @param OrgId
   *          The identifier of an {@link Organization} object
   * @param warehouse
   *          The {@link Warehouse} for which the Opening and Close Inventories are going to be
   *          created
   * @return An {@link Organization} that allows to create transactions that is the Organization or
   *         one of the parent Organizations of the given {@link Warehouse}. If no one is found, it
   *         returns the Organization of the given id as a parameter
   */
  public static Organization getOrganizationForCloseAndOpenInventories(final String OrgId,
      final Warehouse warehouse) {
    final Organization invOrg = getTransactionAllowedOrg(warehouse.getOrganization());
    if (invOrg == null) {
      return (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, OrgId);
    }
    return invOrg;
  }

  private static Organization getTransactionAllowedOrg(final Organization org) {
    if (org.getOrganizationType().isTransactionsAllowed()) {
      return org;
    } else {
      final Organization parentOrg = OBContext.getOBContext()
          .getOrganizationStructureProvider()
          .getParentOrg(org);
      if (parentOrg != null && !isStarOrganization(parentOrg)) {
        return getTransactionAllowedOrg(parentOrg);
      } else {
        return null;
      }
    }
  }

  private static boolean isStarOrganization(final Organization parentOrg) {
    return StringUtils.equals(parentOrg.getId(), "0");
  }

  public static Date getLastDate() {
    final SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy");
    try {
      return outputFormat.parse("31-12-9999");
    } catch (final ParseException e) {
      // Error parsing the date.
      log4j.error("Error parsing the date.", e);
      return null;
    }
  }
}
