/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2014-2017 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.financial.FinancialUtils;
import org.openbravo.model.common.currency.ConversionRate;
import org.openbravo.model.common.currency.ConversionRateDoc;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.invoice.InvoiceLine;
import org.openbravo.model.materialmgmt.cost.LCMatched;
import org.openbravo.model.materialmgmt.cost.LandedCostCost;
import org.openbravo.service.db.DbUtility;

public class LCCostMatchFromInvoiceHandler extends BaseProcessActionHandler {
  private static final Logger log = LogManager.getLogger();

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    JSONObject jsonResponse = new JSONObject();
    JSONObject jsonMessage = null;
    OBContext.setAdminMode(true);
    try {
      JSONObject jsonRequest = new JSONObject(content);
      JSONObject jsonparams = jsonRequest.getJSONObject("_params");

      final String strInvoiceLineId = jsonRequest.getString("C_InvoiceLine_ID");
      final InvoiceLine il = OBDal.getInstance().get(InvoiceLine.class, strInvoiceLineId);
      List<String> existingMatchings = new ArrayList<String>();
      for (LCMatched invmatch : il.getLandedCostMatchedList()) {
        existingMatchings.add(invmatch.getId());
      }

      JSONArray selectedLines = jsonparams.getJSONObject("LCCosts").getJSONArray("_selection");
      jsonMessage = processSelectedLines(il, selectedLines, existingMatchings);

      if (jsonMessage == null) {
        jsonMessage = new JSONObject();
        jsonMessage.put("severity", "success");
        jsonMessage.put("text", OBMessageUtils.messageBD("Success"));
      }
      jsonResponse.put("message", jsonMessage);

    } catch (JSONException e) {
      log.error("Error parsing JSON object", e);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log.error("Exception matching invoices to LC Costs", e);

      try {
        jsonResponse = new JSONObject();
        Throwable ex = DbUtility.getUnderlyingSQLException(e);
        String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
        JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
      } catch (Exception ignore) {
      }

    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }

  private JSONObject processSelectedLines(InvoiceLine il, JSONArray selectedLines,
      List<String> existingMatchings) throws JSONException {
    InvoiceLine localIl = il;
    JSONObject message = null;
    for (int i = 0; i < selectedLines.length(); i++) {
      JSONObject line = selectedLines.getJSONObject(i);
      final String strLCMatchedId = line.getString("matchedLandedCost");
      final String strLCCostId = line.getString("landedCostCost");
      boolean isMatchingAdjusted = line.getBoolean("isMatchingAdjusted");
      boolean isMatched = line.getBoolean("matched");
      boolean processMatching = line.getBoolean("processMatching");
      LCMatched match = null;
      LandedCostCost lcc = (LandedCostCost) OBDal.getInstance()
          .getProxy(LandedCostCost.ENTITY_NAME, strLCCostId);

      if (strLCMatchedId.isEmpty()) {
        // Create new match record
        match = OBProvider.getInstance().get(LCMatched.class);
        match.setOrganization(lcc.getOrganization());
        match.setLandedCostCost(lcc);
        match.setInvoiceLine(localIl);
        match.setAmount(BigDecimal.ZERO);
      } else {
        // Update existing record.
        match = OBDal.getInstance().get(LCMatched.class, strLCMatchedId);
        existingMatchings.remove(strLCMatchedId);
      }

      if (isMatched) {
        continue;
      }

      BigDecimal amount = new BigDecimal(line.getString("matchedAmt"));
      if (amount.compareTo(match.getAmount()) != 0) {
        match.setAmountInInvoiceCurrency(amount);
        if (lcc.getCurrency() != localIl.getInvoice().getCurrency()) {
          amount = FinancialUtils.getConvertedAmount(amount, localIl.getInvoice().getCurrency(),
              lcc.getCurrency(), lcc.getAccountingDate(), lcc.getOrganization(),
              FinancialUtils.PRECISION_STANDARD);
        }
        match.setAmount(amount);
        OBDal.getInstance().save(match);
      }
      // load landedcostcost
      LandedCostCost lcCost = OBDal.getInstance().get(LandedCostCost.class, strLCCostId);
      // update isMatchingAdj flag
      if (lcCost.isMatchingAdjusted() != isMatchingAdjusted) {
        lcCost.setMatchingAdjusted(isMatchingAdjusted);
        OBDal.getInstance().save(lcCost);
      }

      final OBCriteria<ConversionRateDoc> conversionRateDoc = OBDal.getInstance()
          .createCriteria(ConversionRateDoc.class);
      conversionRateDoc
          .add(Restrictions.eq(ConversionRateDoc.PROPERTY_INVOICE, localIl.getInvoice()));
      ConversionRateDoc invoiceconversionrate = (ConversionRateDoc) conversionRateDoc
          .uniqueResult();

      Currency currency = lcc.getOrganization().getCurrency() != null
          ? lcc.getOrganization().getCurrency()
          : lcc.getOrganization().getClient().getCurrency();
      ConversionRate landedCostrate = FinancialUtils.getConversionRate(
          lcc.getLandedCost().getReferenceDate(), lcc.getCurrency(), currency,
          lcc.getOrganization(), lcc.getClient());

      if (invoiceconversionrate != null
          && invoiceconversionrate.getRate() != landedCostrate.getMultipleRateBy()) {
        amount = amount.multiply(invoiceconversionrate.getRate())
            .subtract(amount.multiply(landedCostrate.getMultipleRateBy()))
            .divide(landedCostrate.getMultipleRateBy(), currency.getStandardPrecision().intValue(),
                RoundingMode.HALF_UP);
        LCMatched lcmCm = OBProvider.getInstance().get(LCMatched.class);
        lcmCm.setOrganization(lcc.getOrganization());
        lcmCm.setLandedCostCost(lcc);
        lcmCm.setAmount(amount);
        lcmCm.setInvoiceLine(localIl);
        lcmCm.setConversionmatching(true);
        OBDal.getInstance().save(lcmCm);

        lcc.setMatched(Boolean.FALSE);
        lcc.setProcessed(Boolean.FALSE);
        lcc.setMatchingAdjusted(true);
      }
      OBDal.getInstance().flush();
      if (processMatching) {
        message = LCMatchingProcess.doProcessLCMatching(lcCost);
      }
    }
    // Delete unselected matches
    if (!existingMatchings.isEmpty()) {
      LandedCostCost lcCost = null;
      localIl = OBDal.getInstance().get(InvoiceLine.class, localIl.getId());
      OBDal.getInstance().refresh(localIl);

      for (String strLCMatchId : existingMatchings) {
        LCMatched matchToRemove = OBDal.getInstance().get(LCMatched.class, strLCMatchId);

        // load landedcostcost
        lcCost = OBDal.getInstance()
            .get(LandedCostCost.class, matchToRemove.getLandedCostCost().getId());
        lcCost.getLandedCostMatchedList().remove(matchToRemove);
        localIl.getLandedCostMatchedList().remove(matchToRemove);
        OBDal.getInstance().save(lcCost);
        OBDal.getInstance().remove(matchToRemove);
      }
      OBDal.getInstance().save(localIl);
    }
    return message;
  }
}
