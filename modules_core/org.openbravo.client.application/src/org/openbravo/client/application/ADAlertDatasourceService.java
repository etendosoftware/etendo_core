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
 * All portions are Copyright (C) 2015-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import static org.openbravo.erpCommon.utility.StringCollectionUtils.commaSeparated;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.query.NativeQuery;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.UsedByLink;
import org.openbravo.service.datasource.DefaultDataSourceService;
import org.openbravo.service.json.JsonConstants;

/**
 * Datasource used by the Alert Management window
 */
public class ADAlertDatasourceService extends DefaultDataSourceService {
  private static final String AD_TABLE_ID = "594";
  private static final String ALERT_STATUS = "_alertStatus";
  private static final String ALERT_RULE_TAB = "alertRule.tab.id";
  private static final int ALERT_RULE_ID = 0;
  private static final int ALERT_RULE_FILTERCLAUSE = 1;
  private static final Logger log = LogManager.getLogger();

  @Override
  public Entity getEntity() {
    return ModelProvider.getInstance().getEntityByTableId(AD_TABLE_ID);
  }

  @Override
  public String fetch(Map<String, String> parameters) {
    long t = System.currentTimeMillis();
    String alertStatus = "";
    try {
      // Retrieve the information from the request parameters
      if (parameters.get(JsonConstants.WHERE_PARAMETER) != null) {
        log.warn("_where parameter is not allowed, ignoring it");
      }
      alertStatus = parameters.get(ALERT_STATUS);
      alertStatus = StringUtils.isEmpty(alertStatus) ? "" : alertStatus.toUpperCase();

      if (parameters.get(JsonConstants.DISTINCT_PARAMETER) == null) {
        // Also return the tab id of the alert rule, just when loading the grid from the server.
        // This is used in the Alert Management window to navigate to the record related to an alert
        parameters.put(JsonConstants.ADDITIONAL_PROPERTIES_PARAMETER, ALERT_RULE_TAB);
      }

      return super.fetch(parameters, true);
    } catch (Exception ex) {
      log.error("Error while fetching alert data", ex);
      throw new OBException(ex);
    } finally {
      log.debug("Alert list with status {} retrieved in {} ms", alertStatus,
          System.currentTimeMillis() - t);
    }
  }

  @Override
  public void checkFetchDatasourceAccess(Map<String, String> parameter) {
    // Alert datasource is accessible by all roles. Fetch method implements security access based on
    // Alerts. Alerts are filtered based on each user/role.
  }

  private List<String> getAlertIds(String alertStatus) {
    // Get alert rules visible for context's the role/user.
    // @formatter:off
    final String sql =
          "SELECT ad_alertrule_id, filterclause"
        + "  FROM ad_alertrule arule"
        + " WHERE EXISTS (SELECT 1"
        + "                 FROM ad_alertrecipient arecipient"
        + "                WHERE arule.ad_alertrule_id = arecipient.ad_alertrule_id"
        + "                  AND (ad_user_id = :userId"
        + "                       OR (ad_user_id is null AND ad_role_id = :roleId)))"
        + "  AND ad_client_id in :clients"
        + "  AND ad_org_id in :orgs"
        + "  AND isactive='Y'";
    // @formatter:on

    @SuppressWarnings("rawtypes")
    final NativeQuery alertRules = OBDal.getInstance()
        .getSession()
        .createNativeQuery(sql)
        .setParameter("userId", OBContext.getOBContext().getUser().getId())
        .setParameter("roleId", OBContext.getOBContext().getRole().getId())
        .setParameterList("clients", OBContext.getOBContext().getReadableClients())
        .setParameterList("orgs", OBContext.getOBContext().getReadableOrganizations());

    return getAlertIdsFromAlertRules(getAlertRulesGroupedByFilterClause(alertRules), alertStatus);
  }

  /**
   * The method groups the AlertRule IDS by taking into account when they have the same filter
   * clause.
   */
  @SuppressWarnings("rawtypes")
  private Map<String, List<String>> getAlertRulesGroupedByFilterClause(NativeQuery alertRules) {
    Map<String, List<String>> alertRulesIdGroupByFilterClauses = new HashMap<>();
    try {
      for (Object resultObject : alertRules.list()) {
        final Object[] resultAlertRules = (Object[]) resultObject;
        String alertRuleId = resultAlertRules[ALERT_RULE_ID].toString();
        String alertRuleFilterClause = resultAlertRules[ALERT_RULE_FILTERCLAUSE] == null ? ""
            : resultAlertRules[ALERT_RULE_FILTERCLAUSE].toString();

        List<String> ids = alertRulesIdGroupByFilterClauses.get(alertRuleFilterClause);
        if (ids == null) {
          ids = new ArrayList<>();
          alertRulesIdGroupByFilterClauses.put(alertRuleFilterClause, ids);
        }
        ids.add(alertRuleId);
      }
    } catch (SQLGrammarException e) {
      log.error("An error has ocurred when trying to process the alert rules: " + e.getMessage(),
          e);
    }
    return alertRulesIdGroupByFilterClauses;
  }

  private List<String> getAlertIdsFromAlertRules(
      Map<String, List<String>> alertRulesGroupByFilterClause, String alertStatus) {
    List<String> alertIds = new ArrayList<>();
    for (Entry<String, List<String>> alertRuleList : alertRulesGroupByFilterClause.entrySet()) {
      String filterClause;
      try {
        filterClause = new UsedByLink().getWhereClause(RequestContext.get().getVariablesSecureApp(),
            "", alertRuleList.getKey());
      } catch (ServletException e) {
        throw new IllegalStateException(e);
      }

      // @formatter:off
      final String sql =
           "SELECT ad_alert_id "
          + " FROM ad_alert "
          + "WHERE isactive='Y'"
          + "  AND ad_client_id in :clients"
          + "  AND ad_org_id in :orgs"
          + "  AND ad_alertrule_id in :rules "
          + "  AND coalesce(to_char(status), 'NEW') = :status "
          + filterClause;
      // @formatter:on

      @SuppressWarnings("rawtypes")
      final NativeQuery sqlQuery = OBDal.getInstance()
          .getSession()
          .createNativeQuery(sql)
          .setParameter("status", alertStatus)
          .setParameterList("clients", OBContext.getOBContext().getReadableClients())
          .setParameterList("orgs", OBContext.getOBContext().getReadableOrganizations())
          .setParameterList("rules", alertRuleList.getValue());

      try {
        @SuppressWarnings("unchecked")
        List<String> alertsFound = sqlQuery.list();
        if (log.isDebugEnabled()) {
          log.debug("Alert rule IDs: " + alertRuleList.getValue() + ") - SQL:'" + sql + "' - Rows: "
              + alertsFound.size());
        }
        alertIds.addAll(alertsFound);
      } catch (SQLGrammarException e) {
        log.error("An error has ocurred when trying to process the alerts: " + e.getMessage(), e);
      }
    }
    return alertIds;
  }

  @Override
  protected String getWhereAndFilterClause(Map<String, String> parameters) {
    String alertStatus = "";
    alertStatus = parameters.get(ALERT_STATUS);
    alertStatus = StringUtils.isEmpty(alertStatus) ? "" : alertStatus.toUpperCase();

    List<String> alertList = getAlertIds(alertStatus);
    int chunkSize = 1000;
    String whereClause = "";

    if (alertList.isEmpty()) {
      return "1 = 2";
    }

    if (alertList.size() <= chunkSize) {
      return "e.id in (" + commaSeparated(alertList) + ")";
    }

    // There are more than 1000 alerts to include in the where clause, Oracle doesn't
    // support it, so let's split them in chunks with <=1000 elements each
    while (alertList.size() > chunkSize) {
      ArrayList<String> alertListToRemove = new ArrayList<>(alertList.subList(0, chunkSize - 1));
      if (StringUtils.isEmpty(whereClause)) {
        whereClause = "(e.id in (" + commaSeparated(alertListToRemove) + ")";
      } else {
        whereClause += " or e.id in (" + commaSeparated(alertListToRemove) + ")";
      }
      alertList.removeAll(alertListToRemove);
    }
    if (!alertList.isEmpty()) {
      whereClause += " or e.id in (" + commaSeparated(alertList) + "))";
    } else {
      whereClause += ")";
    }
    return whereClause;
  }
}
