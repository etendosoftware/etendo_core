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
import java.util.Map;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class LandedCostProcess {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<LandedCostProcessCheck> landedCostProcessChecks;

  /**
   * Method to process a Landed Cost.
   * 
   * @param landedCost
   *          the landed cost to be processed.
   * @return the message to be shown to the user properly formatted and translated to the user
   *         language.
   */
  public JSONObject processLandedCost(final LandedCost landedCost) {
    LandedCost currentLandedCost = landedCost;
    final JSONObject message = new JSONObject();
    OBContext.setAdminMode(false);
    try {
      message.put("severity", "success");
      message.put("title", "");
      message.put("text", OBMessageUtils.messageBD("Success"));
      try {
        log.debug("Start doChecks");
        doChecks(currentLandedCost, message);
      } catch (OBException e) {
        message.put("severity", "error");
        message.put("text", e.getMessage());
        return message;
      }
      log.debug("Start Distribute Amounts");
      distributeAmounts(currentLandedCost);
      log.debug("Start generateCostAdjustment");

      currentLandedCost = OBDal.getInstance().get(LandedCost.class, currentLandedCost.getId());

      // If active costing rule uses Standard Algorithm, cost adjustment will not be created
      final Organization org = OBContext.getOBContext()
          .getOrganizationStructureProvider(currentLandedCost.getClient().getId())
          .getLegalEntity(currentLandedCost.getOrganization());
      if (!StringUtils.equals(CostingUtils.getCostDimensionRule(org, new Date())
          .getCostingAlgorithm()
          .getJavaClassName(), "org.openbravo.costing.StandardAlgorithm")) {
        final CostAdjustment ca = generateCostAdjustment(currentLandedCost.getId(), message);
        currentLandedCost.setCostAdjustment(ca);
        message.put("documentNo", ca.getDocumentNo());
      }

      currentLandedCost.setDocumentStatus("CO");
      currentLandedCost.setProcessed(Boolean.TRUE);
      OBDal.getInstance().save(currentLandedCost);
      OBDal.getInstance().flush();
    } catch (JSONException ignore) {
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  private void doChecks(final LandedCost landedCost, final JSONObject message) {
    // Check there are Receipt Lines and Costs.
    OBCriteria<LandedCost> critLC = OBDal.getInstance().createCriteria(LandedCost.class);
    critLC.add(Restrictions.sizeEq(LandedCost.PROPERTY_LANDEDCOSTCOSTLIST, 0));
    critLC.add(Restrictions.eq(LandedCost.PROPERTY_ID, landedCost.getId()));
    if (critLC.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD("LandedCostNoCosts"));
    }

    critLC = OBDal.getInstance().createCriteria(LandedCost.class);
    critLC.add(Restrictions.sizeEq(LandedCost.PROPERTY_LANDEDCOSTRECEIPTLIST, 0));
    critLC.add(Restrictions.eq(LandedCost.PROPERTY_ID, landedCost.getId()));
    if (critLC.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD("LandedCostNoReceipts"));
    }

    // Check that all related receipt lines with movementqty >=0 have their cost already calculated.
    //@formatter:off
    final String hql =
                  "as lcr " +
                  "  left join lcr.goodsShipment lcrr" +
                  "  left join lcr.goodsShipmentLine lcrrl" +
                  " where exists (" +
                  "   select 1" +
                  "     from MaterialMgmtMaterialTransaction as trx" +
                  "       join trx.goodsShipmentLine as iol" +
                  "       join iol.shipmentReceipt as io" +
                  "     where trx.isCostCalculated = false" +
                  "       and iol.movementQuantity >= 0" +
                  "       and ((lcrrl is not null and lcrrl.id = iol.id)" +
                  "       or (lcrrl is null and lcrr.id = io.id))" +
                  "   )" +
                  "   and lcr.landedCost.id = :landedCostId";
    //@formatter:on

    final OBQuery<LCReceipt> qryTrx = OBDal.getInstance()
        .createQuery(LCReceipt.class, hql)
        .setNamedParameter("landedCostId", landedCost.getId());

    if (qryTrx.count() > 0) {
      String strReceiptNumbers = "";
      for (LCReceipt lcrl : qryTrx.list()) {
        if (strReceiptNumbers.length() > 0) {
          strReceiptNumbers += ", ";
        }
        if (lcrl.getGoodsShipmentLine() != null) {
          strReceiptNumbers += lcrl.getGoodsShipmentLine().getIdentifier();
        } else {
          strReceiptNumbers += lcrl.getGoodsShipment().getIdentifier();
        }
      }
      final String errorMsg = OBMessageUtils.messageBD("LandedCostReceiptWithoutCosts");
      log.error("Processed and Cost Calculated check error");
      throw new OBException(errorMsg + "\n" + strReceiptNumbers);
    }

    // Execute checks added implementing LandedCostProcessCheck interface.
    for (LandedCostProcessCheck checksInstance : landedCostProcessChecks) {
      checksInstance.doCheck(landedCost, message);
    }
  }

  private void distributeAmounts(final LandedCost landedCost) {
    final OBCriteria<LandedCostCost> criteria = OBDal.getInstance()
        .createCriteria(LandedCostCost.class);
    criteria.add(Restrictions.eq(LandedCostCost.PROPERTY_LANDEDCOST, landedCost));
    criteria.addOrderBy(LandedCostCost.PROPERTY_LINENO, true);
    for (LandedCostCost lcCost : criteria.list()) {
      lcCost = OBDal.getInstance().get(LandedCostCost.class, lcCost.getId());
      log.debug("Start Distributing lcCost {}", lcCost.getIdentifier());
      // Load distribution algorithm
      LandedCostDistributionAlgorithm lcDistAlg = getDistributionAlgorithm(
          lcCost.getLandedCostDistributionAlgorithm());

      lcDistAlg.distributeAmount(lcCost, false);
      lcCost = OBDal.getInstance().get(LandedCostCost.class, lcCost.getId());
      if (lcCost.getInvoiceLine() != null) {
        log.debug("Match with invoice line {}", lcCost.getInvoiceLine().getIdentifier());
        matchCostWithInvoiceLine(lcCost);
      }
    }
    OBDal.getInstance().flush();
  }

  private CostAdjustment generateCostAdjustment(final String strLandedCostId,
      final JSONObject message) {
    final LandedCost landedCost = OBDal.getInstance().get(LandedCost.class, strLandedCostId);
    final Date referenceDate = landedCost.getReferenceDate();
    CostAdjustment ca = CostAdjustmentUtils.insertCostAdjustmentHeader(landedCost.getOrganization(),
        "LC");

    final String strResult = OBMessageUtils.messageBD("LandedCostProcessed");
    final Map<String, String> map = new HashMap<>();
    map.put("documentNo", ca.getDocumentNo());
    try {
      message.put("title", OBMessageUtils.messageBD("Success"));
      message.put("text", OBMessageUtils.parseTranslation(strResult, map));
    } catch (JSONException ignore) {
    }

    //@formatter:off
    final String hql =
                  " select sum(rla.amount) as amt" +
                  "   , rla.landedCostCost.currency.id as lcCostCurrency" +
                  "   , gsl.id as receipt" +
                  "   , (select transactionProcessDate " +
                  "      from MaterialMgmtMaterialTransaction as transaction " +
                  "      where goodsShipmentLine.id = gsl.id" +
                  "     ) as trxprocessdate" +
                  "  from LandedCostReceiptLineAmt as rla" +
                  "    join rla.landedCostReceipt as rl" +
                  "    join rl.goodsShipment as gs" +
                  "    join rla.goodsShipmentLine as gsl" +
                  " where rl.landedCost.id = :landedCostId" +
                  "   and rla.isMatchingAdjustment = false " +
                  " group by rla.landedCostCost.currency.id" +
                  "   , gsl.id" +
                  "   , gs.documentNo" +
                  "   , gsl.lineNo" +
                  " order by trxprocessdate" +
                  "   , gs.documentNo" +
                  "   , gsl.lineNo" +
                  "   , amt";
    //@formatter:on

    final ScrollableResults receiptamts = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("landedCostId", landedCost.getId())
        .scroll(ScrollMode.FORWARD_ONLY);

    int i = 0;
    try {
      while (receiptamts.next()) {
        log.debug("Process receipt amounts");
        final Object[] receiptAmt = receiptamts.get();
        final BigDecimal amt = (BigDecimal) receiptAmt[0];
        final Currency lcCostCurrency = OBDal.getInstance().get(Currency.class, receiptAmt[1]);
        final ShipmentInOutLine receiptLine = OBDal.getInstance()
            .get(ShipmentInOutLine.class, receiptAmt[2]);
        final MaterialTransaction trx = receiptLine.getMaterialMgmtMaterialTransactionList().get(0);
        final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
            amt, ca, lcCostCurrency);
        lineParameters.setSource(true);
        lineParameters.setUnitCost(false);
        lineParameters.setNeedPosting(false);
        final Long lineNo = (i + 1) * 10L;
        CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters, referenceDate, lineNo);

        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
          ca = OBDal.getInstance().get(CostAdjustment.class, ca.getId());
        }
        i++;
      }
    } finally {
      receiptamts.close();
    }

    CostAdjustmentProcess.doProcessCostAdjustment(ca);

    return ca;
  }

  private LandedCostDistributionAlgorithm getDistributionAlgorithm(
      final LCDistributionAlgorithm lcDistAlg) {
    LandedCostDistributionAlgorithm lcDistAlgInstance;
    try {
      Class<?> clz = null;
      clz = OBClassLoader.getInstance().loadClass(lcDistAlg.getJavaClassName());
      lcDistAlgInstance = (LandedCostDistributionAlgorithm) WeldUtils
          .getInstanceFromStaticBeanManager(clz);
    } catch (Exception e) {
      log.error("Error loading distribution algorithm: " + lcDistAlg.getJavaClassName(), e);
      final String strError = OBMessageUtils.messageBD("LCDistributionAlgorithmNotFound");
      final Map<String, String> map = new HashMap<>();
      map.put("distalg", lcDistAlg.getIdentifier());
      throw new OBException(OBMessageUtils.parseTranslation(strError, map));
    }
    return lcDistAlgInstance;
  }

  public static JSONObject doProcessLandedCost(final LandedCost landedCost) {
    final LandedCostProcess lcp = WeldUtils
        .getInstanceFromStaticBeanManager(LandedCostProcess.class);
    return lcp.processLandedCost(landedCost);
  }

  private void matchCostWithInvoiceLine(final LandedCostCost lcc) {
    final LCMatched lcm = OBProvider.getInstance().get(LCMatched.class);
    lcm.setOrganization(lcc.getOrganization());
    lcm.setLandedCostCost(lcc);
    lcm.setAmount(lcc.getAmount());
    lcm.setInvoiceLine(lcc.getInvoiceLine());
    OBDal.getInstance().save(lcm);

    final OBCriteria<ConversionRateDoc> conversionRateDoc = OBDal.getInstance()
        .createCriteria(ConversionRateDoc.class);
    conversionRateDoc.add(
        Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE, lcm.getInvoiceLine().getInvoice()));
    final ConversionRateDoc invoiceconversionrate = (ConversionRateDoc) conversionRateDoc
        .uniqueResult();
    final Currency currency = lcc.getOrganization().getCurrency() != null
        ? lcc.getOrganization().getCurrency()
        : lcc.getOrganization().getClient().getCurrency();
    final ConversionRate landedCostrate = FinancialUtils.getConversionRate(
        lcc.getLandedCost().getReferenceDate(), lcc.getCurrency(), currency, lcc.getOrganization(),
        lcc.getClient());

    if (invoiceconversionrate != null
        && invoiceconversionrate.getRate() != landedCostrate.getMultipleRateBy()) {
      final BigDecimal amount = lcc.getAmount()
          .multiply(invoiceconversionrate.getRate())
          .subtract(lcc.getAmount().multiply(landedCostrate.getMultipleRateBy()))
          .divide(landedCostrate.getMultipleRateBy(), currency.getStandardPrecision().intValue(),
              RoundingMode.HALF_UP);
      final LCMatched lcmCm = OBProvider.getInstance().get(LCMatched.class);
      lcmCm.setOrganization(lcc.getOrganization());
      lcmCm.setLandedCostCost(lcc);
      lcmCm.setAmount(amount);
      lcmCm.setInvoiceLine(lcc.getInvoiceLine());
      lcmCm.setConversionmatching(true);
      OBDal.getInstance().save(lcmCm);

      lcc.setMatched(Boolean.FALSE);
      lcc.setProcessed(Boolean.FALSE);
      lcc.setMatchingAdjusted(true);
      OBDal.getInstance().flush();
      LCMatchingProcess.doProcessLCMatching(lcc);
    }

    lcc.setMatched(Boolean.TRUE);
    lcc.setProcessed(Boolean.TRUE);
    final OBCriteria<LCMatched> critMatched = OBDal.getInstance().createCriteria(LCMatched.class);
    critMatched.add(Restrictions.eq(LCMatched.PROPERTY_LANDEDCOSTCOST, lcc));
    critMatched.setProjection(Projections.sum(LCMatched.PROPERTY_AMOUNT));
    BigDecimal matchedAmt = (BigDecimal) critMatched.uniqueResult();
    if (matchedAmt == null) {
      matchedAmt = lcc.getAmount();
    }
    lcc.setMatchingAmount(matchedAmt);
  }
}
