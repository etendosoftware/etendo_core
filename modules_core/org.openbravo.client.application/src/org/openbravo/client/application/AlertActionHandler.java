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
 * All portions are Copyright (C) 2009-2020 Openbravo SLU 
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.client.application;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.query.Query;
import org.openbravo.base.secureApp.VariablesSecureApp;
import org.openbravo.client.kernel.BaseActionHandler;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.client.kernel.StaticResourceComponent;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.database.ConnectionProvider;
import org.openbravo.erpCommon.utility.UsedByLink;
import org.openbravo.model.ad.alert.AlertRule;
import org.openbravo.portal.PortalAccessible;
import org.openbravo.service.db.DalConnectionProvider;
import org.openbravo.service.json.JsonConstants;

/**
 * Action handler checks if there are alerts and if so returns these as a json object.
 * 
 * Action handler also updates the session ping value in the database.
 * 
 * @author mtaal
 * @see StaticResourceComponent
 */
@ApplicationScoped
public class AlertActionHandler extends BaseActionHandler implements PortalAccessible {
  private static final Logger log4j = LogManager.getLogger();

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.openbravo.client.kernel.BaseActionHandler#execute(javax.servlet.http.HttpServletRequest,
   * javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void execute() {
    final long t = System.currentTimeMillis();
    OBContext.setAdminMode();
    try {
      boolean updated = updateSessionPing();
      long activeAlerts = updated ? countActiveAlerts() : 0L;
      writeResponse(activeAlerts);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    } finally {
      OBContext.restorePreviousMode();
      log4j.debug("Time spent: " + (System.currentTimeMillis() - t) + " ms");
    }
  }

  private boolean updateSessionPing() throws ServletException {
    final HttpServletRequest request = RequestContext.get().getRequest();

    final HttpSession session = request.getSession(false);
    if (session == null) {
      return false;
    }
    final String dbSessionId = (String) session.getAttribute("#AD_Session_ID".toUpperCase());
    if (dbSessionId != null) {
      ConnectionProvider conn = new DalConnectionProvider(false);
      AlertActionHandlerData.setLastPing(conn, dbSessionId);

      // release locks on ad_session table
      OBDal.getInstance().commitAndClose();
    }
    return true;
  }

  private long countActiveAlerts() throws ServletException {
    // @formatter:off
    final String hql =
          "select distinct(e.alertRule)"
        + " from ADAlertRecipient e "
        + "where e.alertRule.active = true"
        + "  and (e.userContact.id= :userId"
        + "      or (e.userContact.id = null and e.role.id = :roleId))"

        // select only those rules that are client/org visible from current role
        + " and e.alertRule.client.id in :clients" 
        + " and e.alertRule.organization.id in :orgs";
    // @formatter:on

    final Query<AlertRule> qry = OBDal.getInstance()
        .getSession()
        .createQuery(hql, AlertRule.class)
        .setParameter("userId", OBContext.getOBContext().getUser().getId())
        .setParameter("roleId", OBContext.getOBContext().getRole().getId())
        .setParameterList("clients", OBContext.getOBContext().getReadableClients())
        .setParameterList("orgs", OBContext.getOBContext().getReadableOrganizations());

    final VariablesSecureApp vars = new VariablesSecureApp(RequestContext.get().getRequest());

    return qry.stream()
        .collect(groupingBy(rule -> Objects.toString(rule.getFilterClause(), ""))) // null can't be
                                                                                   // key
        .values()
        .stream()
        .mapToLong(rulesByFilterClause -> countActiveAlertsForRules(rulesByFilterClause, vars))
        .sum();
  }

  private long countActiveAlertsForRules(List<AlertRule> rules, VariablesSecureApp vars) {
    String commonFilterClause = rules.get(0).getFilterClause();
    List<String> ruleIds = rules.stream().map(AlertRule::getId).collect(toList());

    // @formatter:off
    final String sql = 
        " select count(*) "
        + " from AD_ALERT "
        + "where COALESCE(STATUS, 'NEW') = 'NEW'"
        + " AND AD_CLIENT_ID IN :clients"
        + " AND AD_ORG_ID IN :orgs"
        + " AND AD_ALERTRULE_ID IN :rules"
        + getFilterSQL(commonFilterClause, vars);
    // @formatter:on

    try {
      Number cnt = (Number) OBDal.getInstance()
          .getSession()
          .createNativeQuery(sql)
          .setParameterList("clients", OBContext.getOBContext().getReadableClients())
          .setParameterList("orgs", OBContext.getOBContext().getReadableOrganizations())
          .setParameterList("rules", ruleIds)
          .uniqueResult();
      return cnt.longValue();
    } catch (Exception e) {
      log4j.error("An error has ocurred when trying to process the alerts: " + e.getMessage(), e);
      return 0L;
    }
  }

  private String getFilterSQL(String filterClause, VariablesSecureApp vars) {
    String whereClause;
    try {
      whereClause = new UsedByLink().getWhereClause(vars, "", Objects.toString(filterClause, ""));
    } catch (ServletException ignore) {
      log4j.error("Could not convert filter clause into SQL: " + filterClause, ignore);
      whereClause = " AND 1=2"; // do not count if where clause is broken
    }
    return whereClause;
  }

  private void writeResponse(long activeAlerts) throws JSONException, IOException {
    final JSONObject result = new JSONObject();
    result.put("cnt", activeAlerts);
    result.put("result", "success");

    final HttpServletResponse response = RequestContext.get().getResponse();
    response.setContentType(JsonConstants.JSON_CONTENT_TYPE);
    response.setHeader("Content-Type", JsonConstants.JSON_CONTENT_TYPE);
    response.getWriter().write(result.toString());
  }

  @Override
  protected JSONObject execute(Map<String, Object> parameters, String data) {
    throw new UnsupportedOperationException();
  }
}
