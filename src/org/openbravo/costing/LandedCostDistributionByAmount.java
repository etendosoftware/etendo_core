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

import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.LCReceipt;
import org.openbravo.model.materialmgmt.cost.LCReceiptLineAmt;
import org.openbravo.model.materialmgmt.cost.LandedCost;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.model.materialmgmt.transaction.ShipmentInOutLine;

public class LandedCostDistributionByAmount extends LandedCostDistributionAlgorithm {

  @Override
  public void distributeAmount(final LandedCostCost lcCost, final boolean isMatching) {
    // Calculate total amount of all receipt lines assigned to the landed cost.
    LandedCostCost localLcCost = lcCost;
    localLcCost = (LandedCostCost) OBDal.getInstance()
        .getProxy(LandedCostCost.ENTITY_NAME, localLcCost.getId());
    final LandedCost landedCost = localLcCost.getLandedCost();
    // Get the currency of the Landed Cost Cost
    final String strCurId = localLcCost.getCurrency().getId();
    final String strOrgId = landedCost.getOrganization().getId();
    final Date dateReference = landedCost.getReferenceDate();
    final int precission = localLcCost.getCurrency().getCostingPrecision().intValue();
    BigDecimal baseAmt;
    if (isMatching) {
      baseAmt = localLcCost.getMatchingAmount().subtract(localLcCost.getAmount());
    } else {
      baseAmt = localLcCost.getAmount();
    }

    BigDecimal totalAmt = BigDecimal.ZERO;

    // Loop to get all receipts amounts and calculate the total.
    final OBCriteria<LCReceipt> critLCRL = OBDal.getInstance().createCriteria(LCReceipt.class);
    critLCRL.add(Restrictions.eq(LCReceipt.PROPERTY_LANDEDCOST, landedCost));
    ScrollableResults receiptCosts = getReceiptCosts(landedCost, false);
    int i = 0;
    try {
      while (receiptCosts.next()) {
        final String strTrxCur = (String) receiptCosts.get()[2];
        BigDecimal trxAmt = (BigDecimal) receiptCosts.get()[3];
        if (!strTrxCur.equals(strCurId)) {
          trxAmt = getConvertedAmount(trxAmt, strTrxCur, strCurId, dateReference, strOrgId);
        }

        totalAmt = totalAmt.add(trxAmt.abs());

        if (i % 100 == 0) {
          OBDal.getInstance().flush();
          OBDal.getInstance().getSession().clear();
        }
        i++;
      }
    } finally {
      receiptCosts.close();
    }

    BigDecimal pendingAmt = baseAmt;
    // Loop to calculate the corresponding adjustment amount for each receipt line.
    receiptCosts = getReceiptCosts(landedCost, true);
    i = 0;
    while (receiptCosts.next()) {
      final ShipmentInOutLine receiptline = OBDal.getInstance()
          .get(ShipmentInOutLine.class, receiptCosts.get()[1]);
      final String strTrxCurId = (String) receiptCosts.get()[2];
      BigDecimal trxAmt = (BigDecimal) receiptCosts.get()[3];

      if (!strTrxCurId.equals(strCurId)) {
        trxAmt = getConvertedAmount(trxAmt, strTrxCurId, strCurId, dateReference, strOrgId);
      }

      BigDecimal receiptAmt = BigDecimal.ZERO;
      if (receiptCosts.isLast()) {
        // Insert pending amount on receipt with higher cost to avoid rounding issues.
        receiptAmt = pendingAmt;
      } else {
        receiptAmt = baseAmt.multiply(trxAmt.abs());
        if (totalAmt.compareTo(BigDecimal.ZERO) != 0) {
          receiptAmt = receiptAmt.divide(totalAmt, precission, RoundingMode.HALF_UP);
        }
      }
      pendingAmt = pendingAmt.subtract(receiptAmt);
      final LCReceipt lcrl = (LCReceipt) OBDal.getInstance()
          .getProxy(LCReceipt.ENTITY_NAME, receiptCosts.get()[0]);
      final LCReceiptLineAmt lcrla = OBProvider.getInstance().get(LCReceiptLineAmt.class);
      lcrla.setLandedCostCost((LandedCostCost) OBDal.getInstance()
          .getProxy(LandedCostCost.ENTITY_NAME, localLcCost.getId()));
      localLcCost = (LandedCostCost) OBDal.getInstance()
          .getProxy(LandedCostCost.ENTITY_NAME, localLcCost.getId());
      lcrla.setLandedCostReceipt(lcrl);
      lcrla.setGoodsShipmentLine(receiptline);
      lcrla.setMatchingAdjustment(isMatching);
      lcrla.setAmount(receiptAmt);
      lcrla.setOrganization(localLcCost.getOrganization());
      OBDal.getInstance().save(lcrla);
      if (i % 100 == 0) {
        OBDal.getInstance().flush();
        OBDal.getInstance().getSession().clear();
      }
      i++;
    }
  }

  private ScrollableResults getReceiptCosts(final LandedCost landedCost, final boolean doOrderBy) {
    //@formatter:off
    String hql =
            "select lcr.id as lcreceipt" +
            "  , iol.id as receiptline" +
            "  , trx.currency.id as currency" +
            "  , sum(tc.cost) as cost" +
            " from TransactionCost as tc" +
            "   join tc.inventoryTransaction as trx" +
            "   join trx.goodsShipmentLine as iol" +
            "     , LandedCostReceipt as lcr" +
            " where tc.unitCost = true" +
            "   and iol.movementQuantity >= 0" +
            "   and ((lcr.goodsShipmentLine is not null" +
            "   and lcr.goodsShipmentLine.id = iol.id)" +
            "   or (lcr.goodsShipmentLine is null" +
            "   and lcr.goodsShipment.id = iol.shipmentReceipt.id))" +
            "   and lcr.landedCost.id = :landedCostId" +
            " group by lcr.id" +
            "   , iol.id" +
            "   , trx.currency.id" +
            "   , iol.lineNo";
    //@formatter:on
    if (doOrderBy) {
      //@formatter:off
      hql +=
            " order by iol.lineNo" +
            "   , sum(tc.cost)";
      //@formatter:on
    }

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, Object[].class)
        .setParameter("landedCostId", landedCost.getId())
        .scroll();
  }

  private BigDecimal getConvertedAmount(final BigDecimal trxAmt, final String strCurFromId,
      final String strCurToId, final Date dateReference, final String strOrgId) {
    return FinancialUtils.getConvertedAmount(trxAmt,
        OBDal.getInstance().get(Currency.class, strCurFromId),
        OBDal.getInstance().get(Currency.class, strCurToId), dateReference,
        OBDal.getInstance().get(Organization.class, strOrgId), "C");
  }
}
