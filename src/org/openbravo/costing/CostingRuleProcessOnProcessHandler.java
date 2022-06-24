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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.base.session.OBPropertiesProvider;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.db.DbUtility;

public class CostingRuleProcessOnProcessHandler extends BaseActionHandler {
  private static final Logger log4j = LogManager.getLogger();

  @Override
  protected JSONObject execute(final Map<String, Object> parameters, final String content) {
    final JSONObject jsonResponse = new JSONObject();
    JSONObject msg = new JSONObject();
    final VariablesSecureApp vars = RequestContext.get().getVariablesSecureApp();
    try {
      OBContext.setAdminMode(true);
      final JSONObject jsonRequest = new JSONObject(content);
      final String ruleId = jsonRequest.getString("ruleId");
      final CostingRule rule = OBDal.getInstance().get(CostingRule.class, ruleId);
      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(rule.getClient().getId());
      final Set<String> childOrgs = osp.getChildTree(rule.getOrganization().getId(), true);
      final Set<String> naturalOrgs = osp.getNaturalTree(rule.getOrganization().getId());

      String message = null;
      // Checks
      final CostingRule prevCostingRule = getPreviousRule(rule);
      boolean existsPreviousRule = prevCostingRule != null;
      boolean existsTransactions = existsTransactions(naturalOrgs, childOrgs);
      if (!existsPreviousRule && existsTransactions) {
        if (!rule.getOrganization().getOrganizationType().isLegalEntity()
            && rule.getStartingDate() == null) {
          message = Utility.parseTranslation(new DalConnectionProvider(false), vars,
              vars.getLanguage(), "@CostingRuleStartingDateNullNoPeriodClosed@");
        } else if (rule.getOrganization().getOrganizationType().isLegalEntity()) {
          final Date movementDateInClosedPeriod = checkTransactionsWithMovDateInClosedPeriod(
              naturalOrgs, childOrgs, rule);
          if (movementDateInClosedPeriod != null) {
            message = Utility.parseTranslation(new DalConnectionProvider(false), vars,
                vars.getLanguage(), "@CostNotCalculatedForTrxWithMovDateInPeriodClosed@");
          } else if (movementDateInClosedPeriod == null && rule.getStartingDate() == null) {
            message = Utility.parseTranslation(new DalConnectionProvider(false), vars,
                vars.getLanguage(), "@CostingRuleStartingDateNullNoPeriodClosed@");
          }
        }
      }
      msg.put("severity", "success");
      msg.put("title", "Success");
      msg.put("text", message);
      jsonResponse.put("message", msg);
    } catch (Exception e) {
      OBDal.getInstance().rollbackAndClose();
      final Throwable ex = DbUtility.getUnderlyingSQLException(e);
      final String message = OBMessageUtils.translateError(ex.getMessage()).getMessage();
      log4j.error(message, e);
      try {
        msg = new JSONObject();
        msg.put("severity", "error");
        msg.put("text", message);
        msg.put("title", OBMessageUtils.messageBD("Error"));
        jsonResponse.put("message", msg);
      } catch (Exception ignore) {
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return jsonResponse;
  }

  private Date checkTransactionsWithMovDateInClosedPeriod(final Set<String> naturalOrgs,
      final Set<String> childOrgs, final CostingRule rule) {
    CostingUtilsData[] data = null;
    Date movementDateInPeriodClosed = null;
    try {
      final String strDateFormat = OBPropertiesProvider.getInstance()
          .getOpenbravoProperties()
          .getProperty("dateFormat.java");
      final SimpleDateFormat dateFormat = new SimpleDateFormat(strDateFormat);
      final String strDateFrom = dateFormat.format(CostingUtils.getCostingRuleStartingDate(rule));

      data = CostingUtilsData.selectTransactionsInClosedPeriod(new DalConnectionProvider(false),
          Utility.getInStrSet(naturalOrgs), strDateFrom, Utility.getInStrSet(childOrgs),
          rule.getClient().getId(), rule.getOrganization().getId());

      if (data != null && data.length > 0) {
        movementDateInPeriodClosed = OBDateUtils.getDate(data[0].mindatemovement);
      }
    } catch (Exception e) {
      log4j.error("Error executing process", e);
    }

    return movementDateInPeriodClosed;
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

  private boolean existsTransactions(final Set<String> naturalOrgIds,
      final Set<String> childOrgIds) {
    //@formatter:off
    final String hql =
                  "as p" +
                  " where p.productType = 'I'" +
                  "   and p.stocked = true" +
                  "   and p.organization.id in (:productOrgIds)" +
                  "   and exists (" +
                  "     select 1 from MaterialMgmtMaterialTransaction" +
                  "      where product.id = p.id" +
                  "        and organization .id in (:childOrgIds)" +
                  "     )";
    //@formatter:on

    return OBDal.getInstance()
        .createQuery(Product.class, hql)
        .setFilterOnReadableOrganization(false)
        .setNamedParameter("productOrgIds", naturalOrgIds)
        .setNamedParameter("childOrgIds", childOrgIds)
        .setMaxResult(1)
        .uniqueResult() != null;
  }
}
