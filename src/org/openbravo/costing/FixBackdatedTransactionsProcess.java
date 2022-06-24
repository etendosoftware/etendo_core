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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.materialmgmt.cost.CostAdjustment;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.transaction.MaterialTransaction;
import org.openbravo.service.db.DbUtility;
import org.openbravo.service.json.JsonUtils;

public class FixBackdatedTransactionsProcess extends BaseProcessActionHandler {
  private static final String BEGINNING_OF_TIMES = "01-01-1900";
  private static final Logger log4j = LogManager.getLogger();
  private static CostAdjustment costAdjHeader = null;

  @Override
  protected JSONObject doExecute(Map<String, Object> parameters, String content) {
    costAdjHeader = null;
    final JSONObject jsonResponse = new JSONObject();

    final OBError msg = new OBError();
    final JSONObject jsonRequest;
    try {
      jsonRequest = new JSONObject(content);
      final JSONObject jsonparams = jsonRequest.getJSONObject("_params");
      final String ruleId = jsonRequest.getString("M_Costing_Rule_ID");
      Date fixbackdatedfrom = null;
      CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
      rule.setBackdatedTransactionsFixed(Boolean.TRUE);
      OBDal.getInstance().save(rule);

      if (jsonparams.has("fixbackdatedfrom")
          && !jsonparams.getString("fixbackdatedfrom").equals("null")) {
        try {
          final String repairedfixbackdatedfrom = JsonUtils
              .convertFromXSDToJavaFormat(jsonparams.getString("fixbackdatedfrom"));
          fixbackdatedfrom = JsonUtils.createDateTimeFormat().parse(repairedfixbackdatedfrom);
        } catch (ParseException ignore) {
        }
      } else {
        fixbackdatedfrom = CostingUtils.getCostingRuleStartingDate(rule);
      }
      rule.setFixbackdatedfrom(fixbackdatedfrom);
      try {
        OBContext.setAdminMode(false);

        if (rule.getStartingDate() != null && rule.getFixbackdatedfrom() != null
            && rule.isBackdatedTransactionsFixed()
            && rule.getFixbackdatedfrom().before(rule.getStartingDate())) {
          throw new OBException(
              OBMessageUtils.parseTranslation("@FixBackdateFromBeforeStartingDate2@"));
        }
        CostAdjustmentProcess
            .doGetAlgorithmAdjustmentImp(rule.getCostingAlgorithm().getJavaClassName());

        OrganizationStructureProvider osp = OBContext.getOBContext()
            .getOrganizationStructureProvider(rule.getClient().getId());
        final Set<String> childOrgs = osp.getChildTree(rule.getOrganization().getId(), true);
        final ScrollableResults transactions = getTransactions(childOrgs, fixbackdatedfrom,
            rule.getEndingDate());
        int i = 0;
        try {
          final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
          Date lastMovementDate = dateFormat.parse(BEGINNING_OF_TIMES);
          String productId = "";
          while (transactions.next()) {
            final MaterialTransaction trx = (MaterialTransaction) transactions.get()[0];
            if (!StringUtils.equals(productId, trx.getProduct().getId())) {
              productId = trx.getProduct().getId();
              lastMovementDate = dateFormat.parse(BEGINNING_OF_TIMES);
            }
            if (trx.getMovementDate().compareTo(lastMovementDate) < 0) {
              createCostAdjustmenHeader(rule.getOrganization());
              final CostAdjustmentLineParameters lineParameters = new CostAdjustmentLineParameters(
                  trx, null, costAdjHeader);
              lineParameters.setSource(true);
              lineParameters.setBackdatedTransaction(true);
              CostAdjustmentUtils.insertCostAdjustmentLine(lineParameters, trx.getMovementDate());
              i++;
              OBDal.getInstance().flush();
              if ((i % 100) == 0) {
                OBDal.getInstance().getSession().clear();
                // Reload rule after clear session.
                rule = OBDal.getInstance().get(CostingRule.class, ruleId);
              }
            } else {
              lastMovementDate = trx.getMovementDate();
            }
          }
        } finally {
          transactions.close();
        }

      } catch (final Exception e) {
        OBDal.getInstance().rollbackAndClose();
        final String message = DbUtility.getUnderlyingSQLException(e).getMessage();
        log4j.error(message, e);

        final JSONObject errorMessage = new JSONObject();
        errorMessage.put("severity", "error");
        errorMessage.put("title", OBMessageUtils.messageBD("Error"));
        errorMessage.put("text", message);
        jsonResponse.put("message", errorMessage);
        return jsonResponse;

      } finally {
        OBContext.restorePreviousMode();
      }

      if (costAdjHeader != null) {
        try {
          final JSONObject message = CostAdjustmentProcess.doProcessCostAdjustment(costAdjHeader);

          if (message.get("severity") != "success") {
            throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@") + ": "
                + costAdjHeader.getDocumentNo() + " - " + message.getString("text"));
          }

          msg.setType((String) message.get("severity"));
          msg.setTitle((String) message.get("title"));
          msg.setMessage((String) message.get("text"));
        } catch (JSONException e) {
          throw new OBException(OBMessageUtils.parseTranslation("@ErrorProcessingCostAdj@"));
        } catch (Exception e) {
          OBDal.getInstance().rollbackAndClose();
          final String message = DbUtility.getUnderlyingSQLException(e).getMessage();
          log4j.error(message, e);
          final JSONObject errorMessage = new JSONObject();

          errorMessage.put("severity", "error");
          errorMessage.put("title", OBMessageUtils.messageBD("Error"));
          errorMessage.put("text", message);
          jsonResponse.put("message", errorMessage);
          return jsonResponse;

        }
      } else {
        msg.setType("Success");
        msg.setMessage(OBMessageUtils.messageBD("Success"));
      }

      final JSONObject errorMessage = new JSONObject();

      errorMessage.put("severity", "success");
      errorMessage.put("text", msg.getMessage());
      jsonResponse.put("message", errorMessage);

    } catch (JSONException e2) {

      e2.printStackTrace();
    }
    return jsonResponse;

  }

  private ScrollableResults getTransactions(final Set<String> childOrgs, final Date startDate,
      final Date endDate) {
    //@formatter:off
    String hql =
            "select trx as trx" +
            "  from MaterialMgmtMaterialTransaction as trx" +
            " where trx.organization.id in (:orgIds)" +
            "   and trx.isCostCalculated = true" +
            "   and trx.transactionProcessDate >= (:startDate)";
    //@formatter:on

    if (endDate != null) {
      //@formatter:off
      hql +=
            " and trx.transactionProcessDate < (:endDate)";
      //@formatter:on
    }
    //@formatter:off
    hql +=
        " order by trx.product.id, trx.transactionProcessDate";
    //@formatter:on

    final Query<MaterialTransaction> stockLinesQry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, MaterialTransaction.class)
        .setParameterList("orgIds", childOrgs)
        .setParameter("startDate", startDate);

    if (endDate != null) {
      stockLinesQry.setParameter("endDate", endDate);
    }

    return stockLinesQry.setFetchSize(1000).scroll(ScrollMode.FORWARD_ONLY);
  }

  private static void createCostAdjustmenHeader(final Organization org) {
    if (costAdjHeader == null) {
      costAdjHeader = CostAdjustmentUtils.insertCostAdjustmentHeader(org, "BDT");
    }
  }

}
