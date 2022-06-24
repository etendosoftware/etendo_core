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
 * All portions are Copyright (C) 2016-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */

package org.openbravo.materialmgmt;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.client.application.process.BaseProcessActionHandler;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.dal.service.OBQuery;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.calendar.Period;
import org.openbravo.model.materialmgmt.cost.CostingRule;
import org.openbravo.model.materialmgmt.onhandquantity.ValuedStockAggregated;
import org.openbravo.service.db.DalConnectionProvider;

public class ResetValuedStockAggregated extends BaseProcessActionHandler {

  private static final Logger log4j = LogManager.getLogger();

  /*
   * Resets the values of the Aggregated Table for the selected Legal Entity
   */
  @Override
  protected JSONObject doExecute(final Map<String, Object> parameters, final String content) {
    JSONObject request;
    JSONObject result = new JSONObject();

    try {
      OBContext.setAdminMode(true);

      request = new JSONObject(content);
      final JSONObject params = request.getJSONObject("_params");
      result.put("retryExecution", true);

      final JSONObject msg = new JSONObject();
      final Organization legalEntity = OBDal.getInstance()
          .get(Organization.class, params.getString("ad_org_id"));

      // Remove existing data in Aggregated Table
      deleteAggregatedValuesFromDate(null, legalEntity);

      // Get Closed Periods that need to be aggregated
      final List<Period> periodList = getClosedPeriodsToAggregate(new Date(),
          legalEntity.getClient().getId(), legalEntity.getId());

      final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
      Date startingDate = formatter.parse("01-01-0000");
      final int totalNumberOfPeriods = periodList.size();
      int contPeriodNumber = 0;
      final long start = System.currentTimeMillis();

      log4j.debug("[ResetValuedStockAggregated] Total number of Periods to aggregate: "
          + totalNumberOfPeriods);

      for (final Period period : periodList) {
        final long startPeriod = System.currentTimeMillis();
        if (noAggregatedDataForPeriod(period)
            && costingRuleDefindedForPeriod(legalEntity, period)) {
          insertValuesIntoValuedStockAggregated(legalEntity, period, startingDate);
          startingDate = period.getEndingDate();
        }
        final long elapsedTimePeriod = (System.currentTimeMillis() - startPeriod);
        contPeriodNumber++;
        log4j.debug("[ResetValuedStockAggregated] Periods processed: " + contPeriodNumber + " of "
            + totalNumberOfPeriods);
        log4j.debug("[ResetValuedStockAggregated] Time to process period: " + elapsedTimePeriod);
      }
      final long elapsedTime = (System.currentTimeMillis() - start);
      log4j.debug("[ResetValuedStockAggregated] Time to process all periods: " + elapsedTime);

      msg.put("severity", "success");
      msg.put("text", OBMessageUtils.messageBD("Success"));
      result.put("message", msg);
      return result;

    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      log4j.error("Error in doExecute() method of ResetValuedStockAggregated class", e);
      try {
        final JSONObject msg = new JSONObject();
        msg.put("severity", "error");
        msg.put("text", OBMessageUtils.messageBD("ErrorAggregatingData"));
        result.put("message", msg);
      } catch (final JSONException e1) {
        log4j.error("Error in doExecute() method of ResetValuedStockAggregated class", e1);
      }
      return result;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /*
   * Remove aggregated values for the selected Legal Entity and from the selected date
   */
  private void deleteAggregatedValuesFromDate(final Date date, final Organization legalEntity) {
    try {
      Date dateFrom = date;
      if (dateFrom == null) {
        final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        dateFrom = formatter.parse("01-01-0000");
      }
      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(legalEntity.getClient().getId());
      final Set<String> orgIds = osp.getNaturalTree(legalEntity.getId());

      //@formatter:off
      final String hqlDelete = 
                    "delete from ValuedStockAggregated" +
                    " where startingDate >= :dateFrom" +
                    "   and organization.id in :orgIds";
      //@formatter:on

      final int deleted = OBDal.getInstance()
          .getSession()
          .createQuery(hqlDelete)
          .setParameter("dateFrom", dateFrom)
          .setParameterList("orgIds", orgIds)
          .executeUpdate();
      log4j.debug(
          "[ResetValuedStockAggregated] No. of records deleted from aggregated table: " + deleted);

    } catch (final ParseException e) {
      log4j.error(
          "Error in deleteAggregatedValuesFromDate() method of ResetValuedStockAggregated class",
          e);
    }
  }

  /*
   * Return true if there is not Aggregated data for the selected Period
   */
  public static boolean noAggregatedDataForPeriod(final Period period) {
    final OBCriteria<ValuedStockAggregated> obc = OBDal.getInstance()
        .createCriteria(ValuedStockAggregated.class)
        .add(Restrictions.eq(ValuedStockAggregated.PROPERTY_PERIOD, period));

    return obc.list().isEmpty();
  }

  // Creates aggregated information for the selected Legal Entity and Period
  public static void insertValuesIntoValuedStockAggregated(final Organization legalEntity,
      final Period period, final Date startingDate) {
    try {

      final OrganizationStructureProvider osp = OBContext.getOBContext()
          .getOrganizationStructureProvider(legalEntity.getClient().getId());
      final Set<String> orgs = osp.getNaturalTree(legalEntity.getId());
      final String orgIds = Utility.getInStrSet(orgs);

      final List<CostingRule> costingRulesList = getCostingRules(legalEntity,
          period.getStartingDate(), period.getEndingDate());
      for (final CostingRule costingRule : costingRulesList) {
        final String crStartingDate = costingRule.getStartingDate() == null ? null
            : OBDateUtils.formatDate(costingRule.getStartingDate());
        final String crEndingDate = costingRule.getEndingDate() == null ? null
            : OBDateUtils.formatDate(costingRule.getEndingDate());
        GenerateValuedStockAggregatedData.insertData(OBDal.getInstance().getConnection(),
            new DalConnectionProvider(), legalEntity.getId(), period.getId(),
            OBDateUtils.formatDate(period.getStartingDate()),
            OBDateUtils.formatDate(period.getEndingDate()), legalEntity.getCurrency().getId(),
            costingRule.getId(), startingDate == null ? null : OBDateUtils.formatDate(startingDate),
            crStartingDate, crEndingDate, legalEntity.getClient().getId(), orgIds,
            legalEntity.getId());
      }

    } catch (final ServletException e) {
      log4j.error(
          "Error in insertValuesIntoValuedStockAggregated() method of ResetValuedStockAggregated class",
          e);
    }
  }

  private static List<CostingRule> getCostingRules(final Organization legalEntity,
      final Date startingDate, final Date endingDate) {

    //@formatter:off
    final String hqlWhere = 
                  "as cr" +
                  " where cr.organization.id = :orgId" +
                  "   and" +
                  "     (" +
                  "       (" +
                  "         (" +
                  "           cr.startingDate <= :startingDate" +
                  "           or cr.startingDate is null" +
                  "         )" +
                  "         and " +
                  "           (" +
                  "             cr.endingDate > :startingDate" +
                  "             or cr.endingDate is null" +
                  "           )" +
                  "       )" +
                  "       or" +
                  "       (" +
                  "         (" +
                  "           cr.startingDate < :endingDate" +
                  "           or cr.startingDate is null" +
                  "         )" +
                  "         and " +
                  "           (" +
                  "             cr.endingDate >= :endingDate" +
                  "             or cr.endingDate is null" +
                  "           )" +
                  "       )" +
                  "     )";
    //@formatter:on

    final OBQuery<CostingRule> query = OBDal.getInstance()
        .createQuery(CostingRule.class, hqlWhere)
        .setNamedParameter("orgId", legalEntity.getId())
        .setNamedParameter("startingDate", startingDate)
        .setNamedParameter("endingDate", endingDate);

    return query.list();
  }

  public static boolean costingRuleDefindedForPeriod(final Organization legalEntity,
      final Period period) {
    //@formatter:off
    final String hqlWhere =
                  "as cr" +
                  " where cr.organization.id in (:orgId)" +
                  "   and" +
                  "     (" +
                  "       cr.startingDate is null" +
                  "       or cr.startingDate <= :endingDate" +
                  "     )" +
                  "     and" +
                  "       (" +
                  "         cr.endingDate is null" +
                  "         or cr.endingDate >= :startingDate" +
                  "       )";
    //@formatter:on

    final OBQuery<CostingRule> query = OBDal.getInstance()
        .createQuery(CostingRule.class, hqlWhere)
        .setNamedParameter("orgId", legalEntity.getId())
        .setNamedParameter("endingDate", period.getEndingDate())
        .setNamedParameter("startingDate", period.getStartingDate());

    return !query.list().isEmpty();
  }

  /*
   * Returns a list of the Periods that needs to be aggregated for the selected Legal Entity
   */
  public static List<Period> getClosedPeriodsToAggregate(final Date endDate, final String clientId,
      final String organizationID) {

    final Organization org = OBDal.getInstance().get(Organization.class, organizationID);
    final Organization legalEntity = OBContext.getOBContext()
        .getOrganizationStructureProvider(clientId)
        .getLegalEntity(org);

    final Date firstNotClosedPeriodStartingDate = getStartingDateFirstNotClosedPeriod(legalEntity);
    final Date lastAggregatedPeriodDateTo = getLastDateToFromAggregatedTable(legalEntity);

    //@formatter:off
    final String hqlWhere =
                  "as p" +
                  " where p.organization.id in (:orgId)" +
                  "   and p.periodType = 'S'" +
                  "   and p.endingDate <= :endDate" +
                  "   and p.endingDate <= :firstNotClosedPeriodStartingDate" +
                  "   and p.startingDate >= :lastAggregatedPeriodDateTo" +
                  " order by p.endingDate asc";
    //@formatter:on

    final OBQuery<Period> query = OBDal.getInstance()
        .createQuery(Period.class, hqlWhere)
        .setNamedParameter("orgId", legalEntity.getId())
        .setNamedParameter("endDate", endDate)
        .setNamedParameter("firstNotClosedPeriodStartingDate", firstNotClosedPeriodStartingDate)
        .setNamedParameter("lastAggregatedPeriodDateTo", lastAggregatedPeriodDateTo);

    return query.list();
  }

  /*
   * Get last Date of for which the data has been aggregated for this Legal Entity
   */
  private static Date getLastDateToFromAggregatedTable(final Organization legalEntity) {
    Date dateTo = null;
    try {
      dateTo = (Date) OBDal.getInstance()
          .createCriteria(ValuedStockAggregated.class)
          .add(Restrictions.eq(ValuedStockAggregated.PROPERTY_ORGANIZATION, legalEntity))
          .setProjection(Projections.max(ValuedStockAggregated.PROPERTY_ENDINGDATE))
          .uniqueResult();
      if (dateTo == null) {
        final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        dateTo = formatter.parse("01-01-0001");
      }
    } catch (final Exception e) {
      log4j.error(
          "Error in getDateToFromLastAggregatedPeriod() method of ResetValuedStockAggregated class",
          e);
    }
    return dateTo;
  }

  /*
   * Get the starting date of the first Period that is not closed for this Legal Entity
   */
  private static Date getStartingDateFirstNotClosedPeriod(final Organization legalEntity) {
    Date startingDate = null;

    //@formatter:off
    final String hqlSelect =
            "select min(p.startingDate)" +
            "  from FinancialMgmtPeriod p" +
            " where p.periodType = 'S'" +
            "   and" +
            "     (" +
            "       'C' <> " +
            "         (" +
            "           select case" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'O') then 'O'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'C') then 'C'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'P') then 'P'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'N') then 'N'" +
            "               else 'M' end" +
            "             from FinancialMgmtPeriodControl pc" +
            "            where pc.period = p" +
            "         )" +
            "       and 'P' <> " +
            "         (" +
            "           select case" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'O') then 'O'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'C') then 'C'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'P') then 'P'" +
            "             when (max(pc.periodStatus) = min(pc.periodStatus) and min(pc.periodStatus) = 'N') then 'N'" +
            "               else 'M' end" +
            "             from FinancialMgmtPeriodControl pc" +
            "            where pc.period = p" +
            "         )" +
            "     )" +
            "   and p.organization.id in (:orgId)";
    //@formatter:on

    final Query<Date> trxQry = OBDal.getInstance()
        .getSession()
        .createQuery(hqlSelect, Date.class)
        .setParameter("orgId", legalEntity.getId())
        .setMaxResults(1);

    try {
      final List<Date> objetctList = trxQry.list();
      if (!objetctList.isEmpty()) {
        startingDate = objetctList.get(0);
      } else {
        final DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy");
        startingDate = formatter.parse("01-01-9999");
      }
    } catch (final Exception e) {
      log4j.error(
          "Error in getStartingDateFirstNotClosedPeriod() method of ResetValuedStockAggregated class",
          e);
    }
    return startingDate;
  }
}
