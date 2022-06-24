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
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.time.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.materialmgmt.InventoryCountProcess;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Locator;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.Warehouse;
import org.openbravo.model.common.plm.AttributeSetInstance;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.common.plm.ProductUOM;
import org.openbravo.model.common.uom.UOM;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.cost.CostingRuleInit;
import org.openbravo.model.materialmgmt.cost.TransactionCost;
import org.openbravo.model.materialmgmt.transaction.InventoryCount;
import org.openbravo.model.materialmgmt.transaction.InventoryCountLine;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.TransactionLast;
import org.openbravo.scheduling.Process;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.scheduling.ProcessLogger;
import org.openbravo.service.db.DbUtility;

public class CostingRuleProcess implements Process {
  private ProcessLogger logger;
  private static final Logger log4j = LogManager.getLogger();

  @Override
  public void execute(final ProcessBundle bundle) throws Exception {
    long start = System.currentTimeMillis();
    log4j.debug("Starting CostingRuleProcess at: " + new Date());
    logger = bundle.getLogger();
    final OBError msg = new OBError();
    msg.setType("Success");
    msg.setTitle(OBMessageUtils.messageBD("Success"));
    try {
      OBContext.setAdminMode(false);
      final String ruleId = (String) bundle.getParams().get("M_Costing_Rule_ID");
      CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);

      // Checks
      if (rule.getOrganization().getCurrency() == null) {
        throw new OBException("@NoCurrencyInCostingRuleOrg@");
      }
      if (rule.isBackdatedTransactionsFixed()) {
        CostAdjustmentProcess
            .doGetAlgorithmAdjustmentImp(rule.getCostingAlgorithm().getJavaClassName());
      }

      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(rule.getClient().getId());
      final Set<String> childOrgs = osp.getChildTree(rule.getOrganization().getId(), true);
      final Set<String> naturalOrgs = osp.getNaturalTree(rule.getOrganization().getId());

      final CostingRule prevCostingRule = getPreviousRule(rule);
      boolean existsPreviousRule = prevCostingRule != null;
      boolean existsTransactions = existsTransactions(naturalOrgs, childOrgs);
      if (existsPreviousRule) {
        // Product with costing rule. All trx must be calculated.
        checkAllTrxCalculated(naturalOrgs, childOrgs);
      } else if (existsTransactions) {
        // Product configured to have cost not calculated cannot have transactions with cost
        // calculated.
        checkNoTrxWithCostCalculated(naturalOrgs, childOrgs);
        if (rule.getStartingDate() != null) {
          // First rule of an instance that does not need migration. Old transactions costs are not
          // calculated. They are initialized with ZERO cost.
          initializeOldTrx(childOrgs, rule.getStartingDate());
        }
      }
      // Inventories are only needed:
      // - if the costing rule is updating a previous rule
      // - or legacy cost was never used and the first validated rule has a starting date different
      // than null. If the date is old enough that there are not prior transactions no inventories
      // are created.
      if (existsPreviousRule || rule.getStartingDate() != null) {
        Date startingDate = rule.getStartingDate();
        if (existsPreviousRule) {
          // Set valid from date
          startingDate = DateUtils.truncate(new Date(), Calendar.SECOND);
          rule.setStartingDate(startingDate);
          log4j.debug("Setting starting date " + startingDate);
          prevCostingRule.setEndingDate(startingDate);
          OBDal.getInstance().save(prevCostingRule);
          OBDal.getInstance().flush();
        }
        if (rule.getFixbackdatedfrom() == null && rule.isBackdatedTransactionsFixed()) {
          rule.setFixbackdatedfrom(startingDate);
        }
        createCostingRuleInits(ruleId, childOrgs, startingDate);

        // Update cost of inventories and process starting physical inventories.
        updateInventoriesCostAndProcessInitInventories(ruleId, startingDate, existsPreviousRule);

        // Delete M_Transaction_Last
        if (existsPreviousRule) {
          deleteLastTransaction();
        }
      }

      if (rule.getStartingDate() != null && rule.getFixbackdatedfrom() != null
          && rule.isBackdatedTransactionsFixed()
          && rule.getFixbackdatedfrom().before(rule.getStartingDate())) {
        throw new OBException("@FixBackdateFromBeforeStartingDate@");
      }

      // Reload rule after possible session clear.
      rule = OBDal.getInstance().get(CostingRule.class, ruleId);
      rule.setValidated(true);
      OBDal.getInstance().save(rule);
    } catch (final OBException e) {
      OBDal.getInstance().rollbackAndClose();
      final String resultMsg = OBMessageUtils.parseTranslation(e.getMessage());
      logger.log(resultMsg);
      log4j.error(e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(resultMsg);
      bundle.setResult(msg);

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      final String message = DbUtility.getUnderlyingSQLException(e).getMessage();
      logger.log(message);
      log4j.error(message, e);
      msg.setType("Error");
      msg.setTitle(OBMessageUtils.messageBD("Error"));
      msg.setMessage(message);
      bundle.setResult(msg);
    } finally {
      OBContext.restorePreviousMode();
    }
    bundle.setResult(msg);
    long end = System.currentTimeMillis();
    log4j.debug(
        "Ending CostingRuleProcess at: " + new Date() + ". Duration: " + (end - start) + " ms.");
  }

  private CostingRule getPreviousRule(final CostingRule rule) {
    //@formatter:off
    final String hql =
                  "as cr" +
                  " where cr.organization.id = :ruleOrgId" +
                  "   and cr.validated = true" +
                  " order by cr.startingDate desc";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(CostingRule.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("ruleOrgId", rule.getOrganization().getId())
        .setMaxResult(1)
        .uniqueResult();
  }

  private boolean existsTransactions(final Set<String> naturalOrgs, final Set<String> childOrgs) {
    //@formatter:off
    final String hql =
                  "as p" +
                  " where p.productType = 'I'" +
                  "   and p.stocked = true" +
                  "   and p.organization.id in (:porgs)" +
                  "   and exists (" +
                  "     select 1 from MaterialMgmtMaterialTransaction" +
                  "      where product = p" +
                  "        and organization.id in (:childOrgs)" +
                  "     )";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(Product.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("porgs", naturalOrgs)
        .setNamedParameter("childOrgs", childOrgs)
        .setMaxResult(1)
        .uniqueResult() != null;
  }

  private void checkAllTrxCalculated(final Set<String> naturalOrgs, final Set<String> childOrgs) {
    //@formatter:off
    final String hql =
                  "as p" +
                  " where p.productType = 'I'" +
                  "   and p.stocked = true" +
                  "   and p.organization.id in (:porgs)" +
                  "   and exists (" +
                  "     select 1 from MaterialMgmtMaterialTransaction as trx " +
                  "      where trx.product = p" +
                  "        and trx.organization.id in (:childOrgs)" +
                  "        and trx.isCostCalculated = false" +
                  "     )";
    //@formatter:on

    Product productResultQuery = OBDal.getInstance()
        .createQuery(Product.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("porgs", naturalOrgs)
        .setNamedParameter("childOrgs", childOrgs)
        .setMaxResult(1)
        .uniqueResult();

    if (productResultQuery != null) {
      throw new OBException("@TrxWithCostNoCalculated@");
    }
  }

  private void checkNoTrxWithCostCalculated(final Set<String> naturalOrgs,
      final Set<String> childOrgs) {
    //@formatter:off
    final String hql =
                  "as p" +
                  " where p.productType = 'I'" +
                  "   and p.stocked = true" +
                  "   and p.organization.id in (:porgs)" +
                  "   and exists (" +
                  "     select 1 from MaterialMgmtMaterialTransaction as trx " +
                  "      where trx.product.id = p.id" +
                  "        and trx.isCostCalculated = true" +
                  "        and trx.organization.id in (:childOrgs)" +
                  "     )";
    //@formatter:on

    final Product productResultQuery = OBDal.getInstance()
        .createQuery(Product.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("porgs", naturalOrgs)
        .setNamedParameter("childOrgs", childOrgs)
        .setMaxResult(1)
        .uniqueResult();

    if (productResultQuery != null) {
      throw new OBException("@ProductsWithTrxCalculated@");
    }
  }

  private void initializeOldTrx(final Set<String> childOrgs, final Date date) throws SQLException {
    final Client client = OBDal.getInstance()
        .get(Client.class, OBContext.getOBContext().getCurrentClient().getId());

    long t1 = System.currentTimeMillis();
    //@formatter:off
    final String hqlInsert =
                  "insert into TransactionCost" +
                  "  (id, client, organization, " + 
                  "   creationDate, createdBy, updated, updatedBy, " +
                  "   active, inventoryTransaction, cost, " + 
                  "   costDate, currency, accountingDate" +
                  "  )" +
                  " select get_uuid(), t.client, t.organization, " + 
                  "   now(), t.createdBy, now(), t.updatedBy, " +
                  "   t.active, t, cast(0 as big_decimal), " + 
                  "   t.transactionProcessDate, t.client.currency , coalesce(io.accountingDate, t.movementDate)" +
                  "   from MaterialMgmtMaterialTransaction as t" +
                  "     left join t.goodsShipmentLine as iol" +
                  "     left join iol.shipmentReceipt as io" +
                  "  where t.organization.id in (:orgs)" +
                  "    and t.transactionProcessDate < :date" +
                  "    and t.isProcessed = false" +
                  "    and t.active = true" +
                  "    and t.client.id = :clientId";
    //@formatter:on

    final int n1 = OBDal.getInstance()
        .getSession()
        .createQuery(hqlInsert)
        .setParameterList("orgs", childOrgs)
        .setParameter("date", date)
        .setParameter("clientId", client.getId())
        .executeUpdate();

    log4j.debug("InitializeOldTrx inserted " + n1 + " records. Took: "
        + (System.currentTimeMillis() - t1) + " ms.");

    final long t2 = System.currentTimeMillis();
    //@formatter:off
    final String hqlUpdate =
                  "update MaterialMgmtMaterialTransaction" +
                  "  set isCostCalculated = true" +
                  "    , costingStatus = 'CC'" +
                  "    , transactionCost = :zero" +
                  "    , currency = :currency" +
                  "    , isProcessed = true" +
                  " where organization.id in (:orgs)" +
                  "   and transactionProcessDate < :date" +
                  "   and isProcessed = false" +
                  "   and active = true" +
                  "   and client.id = :clientId";
    //@formatter:on

    final int n2 = OBDal.getInstance()
        .getSession()
        .createQuery(hqlUpdate)
        .setParameter("zero", BigDecimal.ZERO)
        .setParameter("currency", client.getCurrency())
        .setParameterList("orgs", childOrgs)
        .setParameter("date", date)
        .setParameter("clientId", client.getId())
        .executeUpdate();

    log4j.debug("InitializeOldTrx updated " + n2 + " records. Took: "
        + (System.currentTimeMillis() - t2) + " ms.");

    OBDal.getInstance().getSession().flush();
    OBDal.getInstance().getSession().clear();
  }

  @Deprecated
  protected void createCostingRuleInits(final CostingRule rule, final Set<String> childOrgs) {
    createCostingRuleInits(rule.getId(), childOrgs, null);
  }

  protected void createCostingRuleInits(final String ruleId, final Set<String> childOrgs,
      final Date date) {
    long t1 = System.currentTimeMillis();
    CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
    final ScrollableResults stockLines = getStockLines(childOrgs, date);
    log4j.debug("GetStockLines took: " + (System.currentTimeMillis() - t1) + " ms.");

    // The key of the Map is the concatenation of orgId and warehouseId
    final long t2 = System.currentTimeMillis();
    final Map<String, String> initLines = new HashMap<>();
    final Map<String, Long> maxLineNumbers = new HashMap<>();
    InventoryCountLine closingInventoryLine = null;
    InventoryCountLine openInventoryLine = null;
    int i = 0;
    try {
      while (stockLines.next()) {
        final long t3 = System.currentTimeMillis();
        final Object[] stockLine = stockLines.get();
        final String productId = (String) stockLine[0];
        final String attrSetInsId = (String) stockLine[1];
        final String uomId = (String) stockLine[2];
        final String orderUOMId = (String) stockLine[3];
        final String locatorId = (String) stockLine[4];
        final String warehouseId = (String) stockLine[5];
        final BigDecimal qty = (BigDecimal) stockLine[6];
        final BigDecimal orderQty = (BigDecimal) stockLine[7];

        final String criId = initLines.get(warehouseId);
        CostingRuleInit cri = null;
        if (criId == null) {
          cri = createCostingRuleInitLine(rule, warehouseId, date);

          initLines.put(warehouseId, cri.getId());
        } else {
          cri = OBDal.getInstance().get(CostingRuleInit.class, criId);
        }
        final Long lineNo = (maxLineNumbers.get(criId) == null ? 0L : maxLineNumbers.get(criId))
            + 10L;
        maxLineNumbers.put(criId, lineNo);

        if (BigDecimal.ZERO.compareTo(qty) < 0) {
          // Do not insert negative values in Inventory lines, instead reverse the Quantity Count
          // and the Book Quantity. For example:
          // Instead of CountQty=0 and BookQty=-5 insert CountQty=5 and BookQty=0
          // By doing so the difference between both quantities remains the same and no negative
          // values have been inserted.

          openInventoryLine = insertInventoryLine(cri.getInitInventory(), productId, attrSetInsId,
              uomId, orderUOMId, locatorId, qty, BigDecimal.ZERO, orderQty, BigDecimal.ZERO, lineNo,
              null);
          insertInventoryLine(cri.getCloseInventory(), productId, attrSetInsId, uomId, orderUOMId,
              locatorId, BigDecimal.ZERO, qty, BigDecimal.ZERO, orderQty, lineNo,
              openInventoryLine);

        } else {
          openInventoryLine = insertInventoryLine(cri.getInitInventory(), productId, attrSetInsId,
              uomId, orderUOMId, locatorId, BigDecimal.ZERO, qty.abs(), BigDecimal.ZERO,
              orderQty == null ? null : orderQty.abs(), lineNo, closingInventoryLine);
          insertInventoryLine(cri.getCloseInventory(), productId, attrSetInsId, uomId, orderUOMId,
              locatorId, qty == null ? null : qty.abs(), BigDecimal.ZERO,
              orderQty == null ? null : orderQty.abs(), BigDecimal.ZERO, lineNo, openInventoryLine);
        }

        i++;
        if ((i % 100) == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          // Reload rule after clear session.
          rule = OBDal.getInstance().get(CostingRule.class, ruleId);
        }

        log4j.debug("Create closing/opening inventory line took: "
            + (System.currentTimeMillis() - t3) + " ms.");
      }
    } finally {
      stockLines.close();
    }
    log4j.debug("Create " + i + " closing/opening inventory lines took: "
        + (System.currentTimeMillis() - t2) + " ms.");

    // Process closing physical inventories.
    final long t4 = System.currentTimeMillis();
    rule = OBDal.getInstance().get(CostingRule.class, ruleId);
    i = 0;
    for (CostingRuleInit cri : rule.getCostingRuleInitList()) {
      final long t5 = System.currentTimeMillis();
      new InventoryCountProcess().processInventory(cri.getCloseInventory(), false);
      log4j.debug(
          "Processing closing inventory took: " + (System.currentTimeMillis() - t5) + " ms.");
      i++;
    }
    log4j.debug("Processing " + i + " closing inventories took: "
        + (System.currentTimeMillis() - t4) + " ms.");

    log4j
        .debug("CreateCostingRuleInits method took: " + (System.currentTimeMillis() - t1) + " ms.");
  }

  private ScrollableResults getStockLines(final Set<String> childOrgs, final Date date) {
    //@formatter:off
    String hql =
            "select trx.product.id, trx.attributeSetValue.id, trx.uOM.id, " + 
            "    trx.orderUOM.id, trx.storageBin.id, loc.warehouse.id, " + 
            "    sum(trx.movementQuantity), sum(trx.orderQuantity)" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            "    join trx.storageBin as loc" +
            " where trx.organization.id in (:orgsIds)";
    //@formatter:on
    if (date != null) {
      //@formatter:off
      hql +=      
            "   and trx.transactionProcessDate < :date";
      //@formatter:on
    }
    //@formatter:off
    hql +=
            "   and trx.product.productType = 'I'" +
            "   and trx.product.stocked = true" +
            " group by trx.product.id, trx.attributeSetValue.id, trx.uOM.id, " + 
            "   trx.orderUOM.id, trx.storageBin.id, loc.warehouse.id" +
            "   having sum(trx.movementQuantity) <> 0" +
            "     or sum(trx.orderQuantity) <> 0" +
            " order by loc.warehouse.id, trx.product.id, trx.storageBin.id, " +
            "   trx.attributeSetValue.id, trx.uOM.id, trx.orderUOM.id";
    //@formatter:on

    @SuppressWarnings("rawtypes")
    final Query stockLinesQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameterList("orgsIds", childOrgs);

    if (date != null) {
      stockLinesQry.setParameter("date", date);
    }

    return stockLinesQry.setFetchSize(1000).scroll(ScrollMode.FORWARD_ONLY);
  }

  private CostingRuleInit createCostingRuleInitLine(final CostingRule rule,
      final String warehouseId, final Date date) {
    Date localDate = date;
    if (localDate == null) {
      localDate = new Date();
    }
    final String clientId = rule.getClient().getId();
    final String orgId = rule.getOrganization().getId();
    final Warehouse warehouse = (Warehouse) OBDal.getInstance()
        .getProxy(Warehouse.ENTITY_NAME, warehouseId);

    final CostingRuleInit cri = OBProvider.getInstance().get(CostingRuleInit.class);
    cri.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    cri.setOrganization(
        (Organization) OBDal.getInstance().getProxy(Organization.ENTITY_NAME, orgId));
    cri.setWarehouse(warehouse);
    cri.setCostingRule(rule);

    List<CostingRuleInit> criList = rule.getCostingRuleInitList();
    criList.add(cri);
    rule.setCostingRuleInitList(criList);

    final Organization invOrg = CostingUtils.getOrganizationForCloseAndOpenInventories(orgId,
        warehouse);
    final InventoryCount closeInv = OBProvider.getInstance().get(InventoryCount.class);
    closeInv.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    closeInv.setOrganization(invOrg);
    closeInv.setName(OBMessageUtils.messageBD("CostCloseInventory"));
    closeInv.setWarehouse(warehouse);
    closeInv.setMovementDate(localDate);
    closeInv.setInventoryType("C");
    cri.setCloseInventory(closeInv);

    final InventoryCount initInv = OBProvider.getInstance().get(InventoryCount.class);
    initInv.setClient((Client) OBDal.getInstance().getProxy(Client.ENTITY_NAME, clientId));
    initInv.setOrganization(invOrg);
    initInv.setName(OBMessageUtils.messageBD("CostInitInventory"));
    initInv.setWarehouse(warehouse);
    initInv.setMovementDate(localDate);
    initInv.setInventoryType("O");

    cri.setInitInventory(initInv);

    OBDal.getInstance().save(rule);
    OBDal.getInstance().save(closeInv);
    OBDal.getInstance().save(initInv);

    OBDal.getInstance().flush();

    return cri;
  }

  private InventoryCountLine insertInventoryLine(final InventoryCount inventory,
      final String productId, final String attrSetInsId, final String uomId,
      final String orderUOMId, final String locatorId, final BigDecimal qtyCount,
      final BigDecimal qtyBook, final BigDecimal orderQtyCount, final BigDecimal orderQtyBook,
      final Long lineNo, InventoryCountLine relatedInventoryLine) {
    final InventoryCountLine icl = OBProvider.getInstance().get(InventoryCountLine.class);
    icl.setClient(inventory.getClient());
    icl.setOrganization(inventory.getOrganization());
    icl.setPhysInventory(inventory);
    icl.setLineNo(lineNo);
    icl.setStorageBin((Locator) OBDal.getInstance().getProxy(Locator.ENTITY_NAME, locatorId));
    icl.setProduct((Product) OBDal.getInstance().getProxy(Product.ENTITY_NAME, productId));
    icl.setAttributeSetValue((AttributeSetInstance) OBDal.getInstance()
        .getProxy(AttributeSetInstance.ENTITY_NAME, attrSetInsId));
    icl.setQuantityCount(qtyCount);
    icl.setBookQuantity(qtyBook);
    icl.setUOM((UOM) OBDal.getInstance().getProxy(UOM.ENTITY_NAME, uomId));
    if (orderUOMId != null) {
      icl.setOrderQuantity(orderQtyCount);
      icl.setQuantityOrderBook(orderQtyBook);
      icl.setOrderUOM(
          (ProductUOM) OBDal.getInstance().getProxy(ProductUOM.ENTITY_NAME, orderUOMId));
    }
    icl.setRelatedInventory(relatedInventoryLine);

    final List<InventoryCountLine> invLines = inventory.getMaterialMgmtInventoryCountLineList();
    invLines.add(icl);
    inventory.setMaterialMgmtInventoryCountLineList(invLines);
    OBDal.getInstance().save(inventory);
    OBDal.getInstance().flush();
    return icl;
  }

  private void updateInventoriesCostAndProcessInitInventories(final String ruleId,
      final Date startingDate, final boolean existsPreviousRule) {
    final long t1 = System.currentTimeMillis();
    final CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
    int i = 0;
    for (CostingRuleInit cri : rule.getCostingRuleInitList()) {
      final long t2 = System.currentTimeMillis();
      final ScrollableResults trxs = getInventoryLineTransactions(cri.getCloseInventory());
      log4j.debug(
          "GetInventoryLineTransactions took: " + (System.currentTimeMillis() - t2) + " ms.");
      final long t3 = System.currentTimeMillis();
      int j = 0;
      try {
        while (trxs.next()) {
          final long t4 = System.currentTimeMillis();
          MaterialTransaction trx = (MaterialTransaction) trxs.get(0);
          // Remove 1 second from transaction date to ensure that cost is calculated with previous
          // costing rule.
          trx.setTransactionProcessDate(DateUtils.addSeconds(startingDate, -1));
          BigDecimal trxCost = BigDecimal.ZERO;
          BigDecimal cost = null;
          Currency cur = FinancialUtils.getLegalEntityCurrency(trx.getOrganization());
          if (existsPreviousRule) {
            trxCost = CostingUtils.getTransactionCost(trx, startingDate, true, cur);
            if (trx.getMovementQuantity().compareTo(BigDecimal.ZERO) != 0) {
              if (trxCost == null) {
                throw new OBException("@NoCostCalculated@: " + trx.getIdentifier());
              }
              cost = trxCost.divide(trx.getMovementQuantity().abs(),
                  cur.getCostingPrecision().intValue(), RoundingMode.HALF_UP);
              trx = OBDal.getInstance().get(MaterialTransaction.class, trx.getId());
            }
          } else {
            // Insert transaction cost record big ZERO cost.
            cur = trx.getClient().getCurrency();
            final TransactionCost transactionCost = OBProvider.getInstance()
                .get(TransactionCost.class);
            transactionCost.setInventoryTransaction(trx);
            transactionCost.setCostDate(trx.getTransactionProcessDate());
            transactionCost.setClient(trx.getClient());
            transactionCost.setOrganization(trx.getOrganization());
            transactionCost.setCost(BigDecimal.ZERO);
            transactionCost.setCurrency(trx.getClient().getCurrency());
            transactionCost.setAccountingDate(trx.getGoodsShipmentLine() != null
                ? trx.getGoodsShipmentLine().getShipmentReceipt().getAccountingDate()
                : trx.getMovementDate());
            final List<TransactionCost> trxCosts = trx.getTransactionCostList();
            trxCosts.add(transactionCost);
            trx.setTransactionCostList(trxCosts);
            OBDal.getInstance().save(trx);
          }

          trx.setCostCalculated(true);
          trx.setCostingStatus("CC");
          trx.setCurrency(cur);
          trx.setTransactionCost(trxCost);
          trx.setProcessed(true);
          OBDal.getInstance().save(trx);

          final InventoryCountLine initICL = trx.getPhysicalInventoryLine().getRelatedInventory();
          initICL.setCost(cost);
          OBDal.getInstance().save(initICL);

          j++;
          if ((j % 100) == 0) {
            OBDal.getInstance().flush();
            OBDal.getInstance().getSession().clear();
            cri = OBDal.getInstance().get(CostingRuleInit.class, cri.getId());
          }

          log4j.debug(
              "Update inventory line cost took: " + (System.currentTimeMillis() - t4) + " ms.");
        }
      } finally {
        trxs.close();
      }
      OBDal.getInstance().flush();
      log4j.debug("Update " + j + "inventory line costs took: " + (System.currentTimeMillis() - t3)
          + " ms.");

      final long t5 = System.currentTimeMillis();
      cri = OBDal.getInstance().get(CostingRuleInit.class, cri.getId());
      new InventoryCountProcess().processInventory(cri.getInitInventory(), false);
      log4j.debug(
          "Processing opening inventory took: " + (System.currentTimeMillis() - t5) + " ms.");
      i++;
    }
    log4j.debug("Processing " + i + " opening inventories took: "
        + (System.currentTimeMillis() - t1) + " ms.");

    if (!existsPreviousRule) {
      final long t6 = System.currentTimeMillis();
      updateInitInventoriesTrxDate(startingDate, ruleId);
      log4j.debug(
          "UpdateInitInventoriesTrxDate took: " + (System.currentTimeMillis() - t6) + " ms.");
    }

    log4j.debug("UpdateInventoriesCostAndProcessInitInventories method took: "
        + (System.currentTimeMillis() - t1) + " ms.");
  }

  protected MaterialTransaction getInventoryLineTransaction(final InventoryCountLine icl) {
    return OBDal.getInstance()
        .createQuery(MaterialTransaction.class, "physicalInventoryLine.id = :invlineId")
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("invlineId", icl.getId())
        .uniqueResult();
  }

  protected InventoryCountLine getInitIcl(final InventoryCount initInventory,
      final InventoryCountLine icl) {
    //@formatter:off
    String hql =
            "  physInventory.id = :inventoryId" +
            "  and product.id = :productId" +
            "  and attributeSetValue.id = :asiId" +
            "  and storageBin.id = :storageBinId";
    //@formatter:on
    if (icl.getOrderUOM() == null) {
      //@formatter:off
      hql +=
            "  and orderUOM is null";
      //@formatter:on
    } else {
      //@formatter:off
      hql +=
            "  and orderUOM.id = :orderuomId";
      //@formatter:on
    }
    final OBQuery<InventoryCountLine> iclQry = OBDal.getInstance()
        .createQuery(InventoryCountLine.class, hql)
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("inventoryId", initInventory.getId())
        .setNamedParameter("productId", icl.getProduct().getId())
        .setNamedParameter("asiId", icl.getAttributeSetValue().getId())
        .setNamedParameter("storageBinId", icl.getStorageBin().getId());

    if (icl.getOrderUOM() != null) {
      iclQry.setNamedParameter("orderuomId", icl.getOrderUOM().getId());
    }
    return iclQry.uniqueResult();
  }

  private ScrollableResults getInventoryLineTransactions(final InventoryCount inventory) {
    //@formatter:off
    final String hql =
                  "   physicalInventoryLine.physInventory.id = :inventoryId" +
                  " order by movementQuantity desc, id";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(MaterialTransaction.class, hql)
        .setNamedParameter("inventoryId", inventory.getId())
        .scroll(ScrollMode.FORWARD_ONLY);
  }

  private void updateInitInventoriesTrxDate(final Date startingDate, final String ruleId) {
    //@formatter:off
    final String hql =
                  "update MaterialMgmtMaterialTransaction as trx" +
                  "  set trx.transactionProcessDate = :date" +
                  " where exists (" +
                  "   select 1" +
                  "     from MaterialMgmtInventoryCountLine as il" +
                  "       join il.physInventory as i" +
                  "       join i.costingRuleInitInitInventoryList as cri" +
                  "    where cri.costingRule.id = :crId" +
                  "      and il.id = trx.physicalInventoryLine.id" +
                  "   )";
    //@formatter:on

    OBDal.getInstance()
        .getSession()
        .createQuery(hql)
        .setParameter("date", startingDate)
        .setParameter("crId", ruleId)
        .executeUpdate();

    OBDal.getInstance().flush();
    OBDal.getInstance().getSession().clear();
  }

  private void deleteLastTransaction() {
    OBDal.getInstance()
        .getSession()
        .createQuery("delete from " + TransactionLast.ENTITY_NAME)
        .executeUpdate();
  }
}
