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
import org.openbravo.base.util.OBClassLoader;
import org.openbravo.base.weld.WeldUtils;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.LCDistributionAlgorithm;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class LCMatchingProcess {
  private static final Logger log = LogManager.getLogger();
  @Inject
  @Any
  private Instance<LCMatchingProcessCheck> LCMatchingProcessChecks;

  /**
   * Method to process a Landed Cost.
   * 
   * @param lcCost
   *          the landed cost to be processed.
   * @return the message to be shown to the user properly formatted and translated to the user
   *         language.
   */
  public JSONObject processLCMatching(final LandedCostCost lcCost) {
    LandedCostCost currentLcCost = lcCost;
    final JSONObject message = new JSONObject();
    OBContext.setAdminMode(true);
    try {
      message.put("severity", "success");
      message.put("title", "");
      message.put("text", OBMessageUtils.messageBD("Success"));
      try {
        doChecks(currentLcCost, message);
      } catch (OBException e) {
        message.put("severity", "error");
        message.put("text", e.getMessage());
        return message;
      }
      final OBCriteria<LCMatched> critMatched = OBDal.getInstance().createCriteria(LCMatched.class);
      critMatched.add(Restrictions.eq(LCMatched.PROPERTY_LANDEDCOSTCOST, currentLcCost));
      critMatched.setProjection(Projections.sum(LCMatched.PROPERTY_AMOUNT));
      final BigDecimal matchedAmt = (BigDecimal) critMatched.uniqueResult();
      if (matchedAmt != null) {
        currentLcCost.setMatchingAmount(matchedAmt);
        OBDal.getInstance().save(currentLcCost);
      }

      if (currentLcCost.isMatchingAdjusted()
          && currentLcCost.getAmount().compareTo(matchedAmt) != 0) {
        distributeAmounts(currentLcCost);
        currentLcCost = OBDal.getInstance().get(LandedCostCost.class, currentLcCost.getId());
        // If active costing rule uses Standard Algorithm, cost adjustment will not be created
        final Organization org = OBContext.getOBContext()
            .getOrganizationStructureProvider(currentLcCost.getClient().getId())
            .getLegalEntity(currentLcCost.getOrganization());
        if (!StringUtils.equals(CostingUtils.getCostDimensionRule(org, new Date())
            .getCostingAlgorithm()
            .getJavaClassName(), "org.openbravo.costing.StandardAlgorithm")) {
          final String strMatchCAId = generateCostAdjustment(currentLcCost.getId(), message);
          currentLcCost.setMatchingCostAdjustment((CostAdjustment) OBDal.getInstance()
              .getProxy(CostAdjustment.ENTITY_NAME, strMatchCAId));
        }
        OBDal.getInstance().save(currentLcCost);
      }

      currentLcCost = OBDal.getInstance().get(LandedCostCost.class, currentLcCost.getId());
      currentLcCost.setMatched(Boolean.TRUE);
      currentLcCost.setProcessed(Boolean.TRUE);
      OBDal.getInstance().save(currentLcCost);
    } catch (JSONException ignore) {
    } finally {
      OBContext.restorePreviousMode();
    }
    return message;
  }

  private void doChecks(final LandedCostCost lcCost, final JSONObject message) {
    // Check there are Matching Lines.
    final OBCriteria<LandedCostCost> critLCMatched = OBDal.getInstance()
        .createCriteria(LandedCostCost.class);
    critLCMatched.add(Restrictions.sizeEq(LandedCostCost.PROPERTY_LANDEDCOSTMATCHEDLIST, 0));
    critLCMatched.add(Restrictions.eq(LandedCostCost.PROPERTY_ID, lcCost.getId()));
    if (critLCMatched.uniqueResult() != null) {
      throw new OBException(OBMessageUtils.messageBD("LCCostNoMatchings"));
    }

    // Execute checks added implementing LandedCostProcessCheck interface.
    for (LCMatchingProcessCheck checksInstance : LCMatchingProcessChecks) {
      checksInstance.doCheck(lcCost, message);
    }
  }

  private void distributeAmounts(final LandedCostCost lcCost) {
    // Load distribution algorithm
    final LandedCostDistributionAlgorithm lcDistAlg = getDistributionAlgorithm(
        lcCost.getLandedCostDistributionAlgorithm());

    lcDistAlg.distributeAmount(lcCost, true);
    OBDal.getInstance().flush();
  }

  private String generateCostAdjustment(final String strLCCostId, final JSONObject message)
      throws JSONException {
    final LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, strLCCostId);
    final Date referenceDate = lcCost.getAccountingDate();
    CostAdjustment ca = CostAdjustmentUtils.insertCostAdjustmentHeader(lcCost.getOrganization(),
        "LC");

    final String strResult = OBMessageUtils.messageBD("LCMatchingProcessed");
    final Map<String, String> map = new HashMap<>();
    map.put("documentNo", ca.getDocumentNo());
    message.put("title", OBMessageUtils.messageBD("Success"));
    message.put("text", OBMessageUtils.parseTranslation(strResult, map));

    //@formatter:off
    final String hql =
                  "select sum(rla.amount) as amt" +
                  "  , rla.goodsShipmentLine.id as receipt" +
                  "  , (" +
                  "      select transactionProcessDate " + 
                  "        from MaterialMgmtMaterialTransaction as transaction " +
                  "       where goodsShipmentLine.id = rla.goodsShipmentLine.id" +
                  "    ) as trxprocessdate" +
                  "  from LandedCostReceiptLineAmt as rla" +
                  " where rla.landedCostCost.id = :lccId" +
                  "   and rla.isMatchingAdjustment = true " +
                  " group by rla.goodsShipmentLine.id" +
                  " order by trxprocessdate, amt";
    //@formatter:on

    final ScrollableResults receiptamts = OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("lccId", lcCost.getId())
        .scroll(ScrollMode.FORWARD_ONLY);

    int i = 0;
    try {
      while (receiptamts.next()) {
        final Object[] receiptAmt = receiptamts.get();
        final BigDecimal amt = (BigDecimal) receiptAmt[0];
        final ShipmentInOutLine receiptLine = OBDal.getInstance()
            .get(ShipmentInOutLine.class, receiptAmt[1]);
        final MaterialTransaction trx = receiptLine.getMaterialMgmtMaterialTransactionList().get(0);
        final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(trx,
            amt, ca, lcCost.getCurrency());
        lineParameters.setSource(true);
        lineParameters.setUnitCost(false);
        lineParameters.setNeedPosting(false);
        CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters, referenceDate);

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
    ca = OBDal.getInstance().get(CostAdjustment.class, ca.getId());
    CostAdjustmentProcess.doProcessCostAdjustment(ca);
    return ca.getId();
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

  public static JSONObject doProcessLCMatching(LandedCostCost lcCost) {
    final LCMatchingProcess lcp = WeldUtils
        .getInstanceFromStaticBeanManager(LCMatchingProcess.class);
    return lcp.processLCMatching(lcCost);
  }

}
