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
 * All portions are Copyright (C) 2013-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.financial;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.hibernate.query.Query;
import org.openbravo.base.exception.OBException;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.OBDateUtils;
import org.openbravo.model.ad.datamodel.Table;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.financialmgmt.accounting.AccountingFact;
import org.openbravo.model.financialmgmt.calendar.Period;

public class ResetAccounting {
  static final int FETCH_SIZE = 1000;
  private static final Logger log4j = LogManager.getLogger();

  public static HashMap<String, Integer> delete(String adClientId, String adOrgId,
      List<String> tableIds, String strdatefrom, String strdateto) throws OBException {
    if (tableIds.isEmpty()) {
      return delete(adClientId, adOrgId, "", null, strdatefrom, strdateto);
    } else {
      HashMap<String, Integer> results = new HashMap<>();
      results.put("deleted", 0);
      results.put("updated", 0);
      for (String tableId : tableIds) {
        HashMap<String, Integer> partial = delete(adClientId, adOrgId, tableId, null, strdatefrom,
            strdateto);
        results.put("deleted", results.get("deleted") + partial.get("deleted"));
        results.put("updated", results.get("updated") + partial.get("updated"));
      }
      return results;
    }
  }

  public static HashMap<String, Integer> delete(final String adClientId, final String adOrgId,
      final String adTableId, final String recordId, final String strdatefrom,
      final String strdateto) {
    String localRecordId = recordId;
    if (localRecordId == null) {
      localRecordId = "";
    }
    final long totalProcess = System.currentTimeMillis();
    long start = 0l;
    long end = 0l;
    long totalselect = 0l;
    int deleted = 0;
    int updated = 0;
    final HashMap<String, Integer> results = new HashMap<>();
    results.put("deleted", 0);
    results.put("updated", 0);
    results.put("totaldeleted", 0);
    results.put("totalupdated", 0);
    final String clientId = adClientId;
    final List<String> tables = getTables(adTableId);
    try {
      final Organization org = OBDal.getInstance().get(Organization.class, adOrgId);
      final Set<String> orgIds = StringUtils.equals(org.getOrganizationType().getId(), "0")
          ? getLegalOrBusinessOrgsChilds(clientId, adOrgId)
          : new OrganizationStructureProvider().getChildTree(adOrgId, true);
      // Delete only if exists some organization to be affected.
      if (CollectionUtils.isNotEmpty(orgIds)) {
        for (String tableId : tables) {
          final List<String> docbasetypes = getDocbasetypes(clientId, tableId, localRecordId);
          if (CollectionUtils.isEmpty(docbasetypes)) {
            final String tableName = OBDal.getInstance()
                .getProxy(Table.class, tableId)
                .getIdentifier();
            throw new OBException("@NotDocumentTypeDefinedForTable@: " + tableName);
          }
          //@formatter:off
          String myQuery = 
                    "select distinct e.recordID" +
                    "  from FinancialMgmtAccountingFact e" +
                    " where e.organization.id in (:orgIds)" +
                    "   and e.client.id = :clientId" +
                    "   and e.table.id = :tableId";
          //@formatter:on
          if (localRecordId != null && !"".equals(localRecordId)) {
            //@formatter:off
            myQuery +=
                    "   and e.recordID = :recordId ";
            //@formatter:on
          }
          for (String dbt : docbasetypes) {
            List<Date[]> periods = new ArrayList<>();
            // organizationPeriod: hashmap with organizations allow period control and their open
            // periods
            final Map<String, List<Date[]>> organizationPeriod = new HashMap<>();
            // organizationPeriodControl: hashmap with organizations and their organization allow
            // period control associated
            final Map<String, String> organizationPeriodControl = new HashMap<>();
            //@formatter:off
            final String myQuery1 = 
                    "select ad_org_id" +
                    "  , ad_periodcontrolallowed_org_id" +
                    "  from ad_org" +
                    " where ad_org_id in (:orgIds)";
            //@formatter:on

            final ScrollableResults scroll = OBDal.getInstance()
                .getSession()
                .createNativeQuery(myQuery1)
                .setParameterList("orgIds", orgIds)
                .scroll(ScrollMode.FORWARD_ONLY);
            int i = 0;
            try {
              while (scroll.next()) {
                final Object[] resultSet = scroll.get();
                final String organization = (String) resultSet[0];
                final String orgperiodcontrol = (String) resultSet[1];

                if (orgperiodcontrol != null) {
                  organizationPeriodControl.put(organization, orgperiodcontrol);
                  if (!organizationPeriod.keySet().contains(orgperiodcontrol)) {
                    periods = getPeriodsDates(
                        getOpenPeriods(clientId, dbt, orgIds, getCalendarId(organization), tableId,
                            localRecordId, strdatefrom, strdateto, orgperiodcontrol));
                    organizationPeriod.put(orgperiodcontrol, periods);
                  }
                }

                i++;
                if (i % 100 == 0) {
                  OBDal.getInstance().flush();
                  OBDal.getInstance().getSession().clear();
                }
              }
            } finally {
              scroll.close();
            }

            int docUpdated = 0;
            int docDeleted = 0;
            for (final String organization : orgIds) {
              final String orgAllow = organizationPeriodControl.get(organization);
              periods = organizationPeriod.get(orgAllow);
              for (final Date[] p : periods) {
                //@formatter:off
                String consDate =
                    "   and e.documentCategory = :dbt" +
                    "   and e.organization.id = :organizationId" +
                    "   and e.accountingDate >= :dateFrom" +
                    "   and e.accountingDate <= :dateTo";
                final String exceptionsSql = myQuery + consDate;
                consDate += " and e.accountingDate >= :periodStartDate "+
                            " and e.accountingDate <= :periodEndDate ";
                final Query<String> query = OBDal.getInstance()
                    .getSession()
                    .createQuery(myQuery + consDate, String.class)
                    .setParameterList("orgIds", orgIds)
                    .setParameter("clientId", clientId)
                    .setParameter("dbt", dbt)
                    .setParameter("tableId", tableId)
                    .setParameter("dateFrom",
                        StringUtils.isNotEmpty(strdatefrom) ? OBDateUtils.getDate(strdatefrom)
                            : p[0])
                    .setParameter("dateTo",
                        StringUtils.isNotEmpty(strdateto) ? OBDateUtils.getDate(strdateto) : p[1])
                    .setParameter("periodStartDate", p[0])
                    .setParameter("periodEndDate", p[1])
                    .setParameter("organizationId", organization);

                if (localRecordId != null && !"".equals(localRecordId)) {
                  query.setParameter("recordId", localRecordId).setMaxResults(1);
                } else {
                  query.setFetchSize(FETCH_SIZE);
                }

                start = System.currentTimeMillis();
                List<String> transactions = query.list();
                end = System.currentTimeMillis();
                totalselect = totalselect + end - start;
                while (!transactions.isEmpty()) {
                  final HashMap<String, Integer> partial = delete(transactions, tableId, clientId);
                  deleted = deleted + partial.get("deleted");
                  updated = updated + partial.get("updated");
                  docUpdated = docUpdated + partial.get("updated");
                  docDeleted = docDeleted + partial.get("deleted");
                  start = System.currentTimeMillis();
                  transactions = query.list();
                  end = System.currentTimeMillis();
                  totalselect = totalselect + end - start;
                }
                // Documents with postings in different periods are treated separately to validate
                // all
                // dates are within an open period
                final HashMap<String, Integer> partial = treatExceptions(exceptionsSql, localRecordId,
                    tableId, orgIds, clientId, p[0], p[1], getCalendarId(organization), strdatefrom,
                    strdateto, dbt, orgAllow, organization);
                deleted = deleted + partial.get("deleted");
                updated = updated + partial.get("updated");
                docUpdated = docUpdated + partial.get("updated");
                docDeleted = docDeleted + partial.get("deleted");
              }
            }
            log4j.debug("docBaseType: " + dbt);
            log4j.debug("updated: " + docUpdated);
            log4j.debug("deleted: " + docDeleted);
          }
        }
      }

    } catch (final OBException e) {
      throw e;
    } catch (final Exception e) {
      throw new OBException("Delete failed", e);
    }
    results.put("deleted", deleted);
    results.put("updated", updated);
    log4j.debug("total totalProcess (milies): " + (System.currentTimeMillis() - totalProcess));
    if (localRecordId != null && !"".equals(localRecordId) && deleted == 0 && updated == 0) {
      if (localRecordId != null && !"".equals(localRecordId) && adTableId != null
          && !"".equals(adTableId)) {
        // If record exists but there is no entry in fact table then unpost record
        try {
          OBContext.setAdminMode(false);
          final Table table = OBDal.getInstance().get(Table.class, adTableId);
          final boolean isQueryResultEmpty = OBDal.getInstance()
              .createCriteria(AccountingFact.class)
              .setFilterOnReadableClients(false)
              .setFilterOnReadableOrganization(false)
              .setFilterOnActive(false)
              .add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, localRecordId))
              .add(Restrictions.eq(AccountingFact.PROPERTY_TABLE, table))
              .setMaxResults(1)
              .list()
              .isEmpty();

          if (isQueryResultEmpty && !table.isView()) {
            final String tableName = table.getDBTableName();
            final String tableIdName = table.getDBTableName() + "_Id";

            final String strUpdate = updateTableOneRecord(tableName, tableIdName, table.getId());

            updated = OBDal.getInstance()
                .getSession()
                .createNativeQuery(strUpdate)
                .setParameter("recordID", localRecordId)
                .executeUpdate();

            return results;
          }
        } finally {
          OBContext.restorePreviousMode();
        }
      }
      throw new OBException("@PeriodClosedForUnPosting@");
    }
    return results;
  }

  private static HashMap<String, Integer> delete(final List<String> transactionIds, final String tableId,
      final String clientId) {
    final HashMap<String, Integer> result = new HashMap<>();
    if (transactionIds.isEmpty()) {
      result.put("deleted", 0);
      result.put("updated", 0);
      return result;
    }
    String tableName = "";
    String tableIdName = "";
    OBContext.setAdminMode(false);
    try {
      // First undo date balancing for those balanced entries
      //@formatter:off
      final String strUpdateBalanced = 
              "update FinancialMgmtAccountingFact fact" +
              "  set dateBalanced = null " +
              " where fact.dateBalanced is not null" +
              "   and exists" +
              "     (" +
              "       select 1" +
              "         from FinancialMgmtAccountingFact f" +
              "        where f.recordID in :transactionIds"+
              "          and f.table.id = :tableId" +
              "          and f.client.id=:clientId" +
              "          and f.recordID2=fact.recordID2" +
              "     )";
      //@formatter:on

      OBDal.getInstance()
          .getSession()
          .createQuery(strUpdateBalanced)
          .setParameter("tableId", tableId)
          .setParameterList("transactionIds", transactionIds)
          .setParameter("clientId", clientId)
          .executeUpdate();

      final Table table = OBDal.getInstance().get(Table.class, tableId);
      if (!table.isView()) {
        tableName = table.getDBTableName();
        tableIdName = table.getDBTableName() + "_Id";

        final String strUpdate = updateTableMultipleRecords(tableName, tableIdName, table.getId());

        final int updated = OBDal.getInstance()
            .getSession()
            .createNativeQuery(strUpdate)
            .setParameterList("transactionIds", transactionIds)
            .executeUpdate();

        //@formatter:off 
        final String strDelete = 
                      "delete " +
                      "  from FinancialMgmtAccountingFact" +
                      " where table.id = :tableId" +
                      "   and recordID in (:transactionIds)" +
                      "   and client.id=:clientId";
        //@formatter:on

        final int deleted = OBDal.getInstance()
            .getSession()
            .createQuery(strDelete)
            .setParameter("tableId", tableId)
            .setParameterList("transactionIds", transactionIds)
            .setParameter("clientId", clientId)
            .executeUpdate();

        result.put("deleted", deleted);
        result.put("updated", updated);
        OBDal.getInstance().getConnection().commit();
        OBDal.getInstance().getSession().clear();
      }
      return result;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException("Error Deleting Accounting", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  private static String updateTableOneRecord(final String tableName, final String tableIdName,
      final String tableId) {
    return updateTable(tableName, tableIdName, tableId, false);
  }

  private static String updateTableMultipleRecords(final String tableName, final String tableIdName,
      final String tableId) {
    return updateTable(tableName, tableIdName, tableId, true);
  }

  private static String updateTable(final String tableName, final String tableIdName,
      final String tableId, final boolean multipleRecords) {
    String strUpdate;
    if (hasProcessingColumn(tableId)) {
    //@formatter:off
      strUpdate =
          " update " + tableName +
          "  set posted='N', " +
          "      processing='N' " +
          " where " +
          "   (" +
          "     posted<>'N' " +
          "     or posted is null " +
          "     or processing='N' " +
          "   )";
    //@formatter:on
    } else {
      //@formatter:off
      strUpdate =
          " update " + tableName +
          "  set posted='N', " +
          " where " +
          "   (" +
          "     posted<>'N' " +
          "     or posted is null " +
          "   )";
    //@formatter:on
    }

    if (multipleRecords) {
      //@formatter:off
      strUpdate +=
          "   and " + tableIdName + " in (:transactionIds) ";
      //@formatter:on
    } else {
    //@formatter:off
      strUpdate +=
          "   and " + tableIdName + " = :recordID ";
    //@formatter:on
    }

    return strUpdate;
  }

  public static HashMap<String, Integer> restore(final String clientId, final String adOrgId,
      final String datefrom, final String dateto) {
    final List<String> tableIds = null;
    return restore(clientId, adOrgId, tableIds, datefrom, dateto);
  }

  public static HashMap<String, Integer> restore(final String clientId, final String adOrgId,
      final List<String> tableIds, final String datefrom, final String dateto) {
    final HashMap<String, Integer> results = new HashMap<>();
    results.put("deleted", 0);
    results.put("updated", 0);
    final List<String> tableIdList = CollectionUtils.isEmpty(tableIds)
        ? getActiveTables(clientId, adOrgId)
        : tableIds;
    for (final String tableId : tableIdList) {
      final HashMap<String, Integer> partial = restore(clientId, adOrgId, tableId, datefrom,
          dateto);
      results.put("deleted", results.get("deleted") + partial.get("deleted"));
      results.put("updated", results.get("updated") + partial.get("updated"));
    }
    return results;
  }

  public static HashMap<String, Integer> restore(final String clientId, final String adOrgId,
      final String tableId, final String datefrom, final String dateto) {
    final HashMap<String, Integer> results = new HashMap<>();
    results.put("deleted", 0);
    results.put("updated", 0);
    String tableName = "";
    String tableDate = "";
    OBContext.setAdminMode(false);
    try {
      final Table table = OBDal.getInstance().get(Table.class, tableId);
      if (!table.isView()) {
        tableName = table.getDBTableName();
        tableDate = ModelProvider.getInstance()
            .getEntityByTableName(table.getDBTableName())
            .getPropertyByColumnName(table.getAcctdateColumn().getDBColumnName())
            .getColumnName();

        //@formatter:off
        String strUpdate = 
            "update " + tableName +
            "  set posted='N'";
        //@formatter:on 

        if (hasProcessingColumn(table.getId())) {
          //@formatter:off
          strUpdate += 
            "    , processing='N'";
          //@formatter:on 
        }
        //@formatter:off
        strUpdate += 
            " where posted not in ('Y')" +
            "   and processed = 'Y'" +
            "   and AD_Org_ID in (:orgIds)";
        //@formatter:on

        if (!("".equals(datefrom))) {
          //@formatter:off
          strUpdate += 
            "   and :tableDate >= :dateFrom";
          //@formatter:on
        }
        if (!("".equals(dateto))) {
          //@formatter:off
          strUpdate += 
            "   and :tableDate <= :dateTo";
          //@formatter:on
        }

        @SuppressWarnings("rawtypes")
        final Query update = OBDal.getInstance()
            .getSession()
            .createNativeQuery(strUpdate)
            .setParameterList("orgIds",
                new OrganizationStructureProvider().getNaturalTree(adOrgId));
        if (!("".equals(dateto)) || !("".equals(datefrom))) {
          update.setParameter("tableDate", tableDate);
        }
        try {
          if (!("".equals(datefrom))) {
            update.setParameter("dateFrom", OBDateUtils.getDate(datefrom));
          }
          if (!("".equals(dateto))) {
            update.setParameter("dateTo", OBDateUtils.getDate(dateto));
          }
        } catch (final ParseException e) {
          log4j.error("Restore - Error parsisng dates", e);
        }

        final int updated = update.executeUpdate();
        results.put("updated", updated);
        OBDal.getInstance().getConnection().commit();
        OBDal.getInstance().getSession().clear();
      }
      return results;
    } catch (final Exception e) {
      OBDal.getInstance().rollbackAndClose();
      throw new OBException("Error Reseting Accounting", e);
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<String> getTables(final String adTableId) {
    OBContext.setAdminMode(false);
    try {
      if (!"".equals(adTableId)) {
        final List<String> accountingTables = new ArrayList<>();
        final Table myTable = OBDal.getInstance().get(Table.class, adTableId);
        accountingTables.add(myTable.getId());
        return accountingTables;
      }
      //@formatter:off
      final String myQuery = 
                    "select distinct t.id" +
                    "  from ADTable t" +
                    " where t.id  <> '145' " +
                    "   and exists (" +
                    "     select 1" +
                    "       from FinancialMgmtAccountingFact e" +
                    "      where e.table.id=t.id" +
                    "   ) ";
      //@formatter:on
      return OBDal.getInstance().getSession().createQuery(myQuery).list();
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  @SuppressWarnings("unused")
  private static List<Organization> getOrganizations(final Client client,
      final Set<String> orgIds) {
    final String CLIENT_SYSTEM = "0";
    final OBCriteria<Organization> obc = OBDal.getInstance().createCriteria(Organization.class);
    if (!CLIENT_SYSTEM.equals(client.getId())) {
      obc.add(Restrictions.eq(Organization.PROPERTY_CLIENT, client));
    }
    return obc.add(Restrictions.in(Organization.PROPERTY_ID, orgIds))
        .setFilterOnReadableClients(false)
        .setFilterOnReadableOrganization(false)
        .list();
  }

  private static List<String> getDocbasetypes(final String clientId, final String tableId,
      final String recordId) {
    //@formatter:off
    String myQuery = 
            "select distinct d.documentCategory" +
            "  from DocumentType d" +
            " where d.client.id = :clientId" +
            "   and d.table.id = :tableId" +
            "   and exists (" +
            "     select 1" +
            "       from FinancialMgmtAccountingFact e" +
            "      where e.documentCategory=d.documentCategory";
    //@formatter:on

    if (!"".equals(recordId)) {
      //@formatter:off
      myQuery += 
            "        and e.table.id =:tableId" +
            "        and e.recordID=:recordId";
      //@formatter:on
    }
    //@formatter:off
    myQuery +=  
            "   )";
    //@formatter:on
    final Query<String> query = OBDal.getInstance()
        .getSession()
        .createQuery(myQuery, String.class)
        .setParameter("clientId", clientId)
        .setParameter("tableId", tableId);
    if (!"".equals(recordId)) {
      query.setParameter("recordId", recordId);
      query.setMaxResults(1);
    }

    return query.list();
  }

  private static List<Period> getOpenPeriods(final String clientId, final String docBaseType,
      final Set<String> orgIds, final String calendarId, final String tableId,
      final String recordId, final String datefrom, final String dateto,
      final String orgPeriodControl) {
    if (!"".equals(recordId)) {
      final List<Period> periods = new ArrayList<>();
      periods.add(
          getDocumentPeriod(clientId, tableId, recordId, docBaseType, orgPeriodControl, orgIds));
      return periods;

    }
    //@formatter:off
    String myQuery = 
            "select distinct p" +
            " from FinancialMgmtPeriodControl e" +
            "   left join e.period p" +
            "   left join p.year y" +
            "   left join y.calendar c" +
            " where c.id = :calendarId" +
            "   and e.client.id = :clientId" +
            "   and e.documentCategory = :docbasetype" +
            "   and e.periodStatus = 'O'" +
            "   and e.organization.id = :orgPeriodControl";
    //@formatter:on

    if (!("".equals(datefrom)) && !("".equals(dateto))) {
      //@formatter:off
      myQuery += 
            "   and p.startingDate <= :dateTo" +
            "   and p.endingDate >= :dateFrom";
      //@formatter:on
    } else if (!("".equals(datefrom)) && ("".equals(dateto))) {
      //@formatter:off
      myQuery += 
            "   and p.endingDate >= :dateFrom";
      //@formatter:on
    } else if (("".equals(datefrom)) && !("".equals(dateto))) {
      //@formatter:off
      myQuery += 
            "   and p.startingDate <= :dateTo";
      //@formatter:on
    }
    final Query<Period> query = OBDal.getInstance()
        .getSession()
        .createQuery(myQuery, Period.class)
        .setParameter("calendarId", calendarId)
        .setParameter("clientId", clientId)
        .setParameter("docbasetype", docBaseType)
        .setParameter("orgPeriodControl", orgPeriodControl);
    // TODO: Review orgIds
    // query.setParameterList("orgIds", orgIds);
    try {
      if (!("".equals(datefrom))) {
        query.setParameter("dateFrom", OBDateUtils.getDate(datefrom));
      }
      if (!("".equals(dateto))) {
        query.setParameter("dateTo", OBDateUtils.getDate(dateto));
      }
    } catch (ParseException e) {
      log4j.error("GetOpenPeriods - error parsing dates", e);
    }
    return query.list();
  }

  private static Period getDocumentPeriod(final String clientId, final String tableId,
      final String recordId, final String docBaseType, final String orgPeriodControl,
      final Set<String> orgIds) {
    //@formatter:off
    final String myQuery = 
            "select distinct e.period" +
            "  from FinancialMgmtAccountingFact e" +
            "    , FinancialMgmtPeriodControl p" +
            " where p.period.id=e.period.id" +
            "   and p.periodStatus = 'O'" +
            "   and e.client.id = :clientId" +
            "   and e.table.id = :tableId" +
            "   and e.recordID=:recordId" +
            "   and p.documentCategory = :docbasetype" +
            "   and p.organization.id  = :orgPeriodControl" +
            "   and e.organization.id in (:orgIds)";
    //@formatter:on

    final Period period = OBDal.getInstance()
        .getSession()
        .createQuery(myQuery, Period.class)
        .setParameter("clientId", clientId)
        .setParameter("tableId", tableId)
        .setParameter("recordId", recordId)
        .setParameter("docbasetype", docBaseType)
        .setParameter("orgPeriodControl", orgPeriodControl)
        .setParameterList("orgIds", orgIds)
        .setMaxResults(1)
        .uniqueResult();
    if (period == null) {
      throw new OBException("@PeriodClosedForUnPosting@");
    }
    return period;
  }

  private static List<Date[]> getPeriodsDates(final List<Period> periods) {
    final List<Date[]> result = new ArrayList<>();
    OBContext.setAdminMode();
    try {
      for (Period period : periods) {
        final Date[] dates = new Date[2];
        dates[0] = period.getStartingDate();
        dates[1] = period.getEndingDate();
        result.add(dates);
      }
    } finally {
      OBContext.restorePreviousMode();
    }
    return result;
  }

  private static String getCalendarId(final String adOrgId) {
    final Organization organization = OBDal.getInstance().get(Organization.class, adOrgId);
    if (organization.getCalendar() != null) {
      return organization.getCalendar().getId();
    } else {
      return getCalendarId(new OrganizationStructureProvider().getParentOrg(adOrgId));
    }
  }

  private static List<String> getActiveTables(final String clientId, final String adOrgId) {
    //@formatter:off
    final String myQuery = 
             "select distinct table.id" +
             "  from FinancialMgmtAcctSchemaTable" +
             " where accountingSchema.id in (:accountingSchemaIds)" +
             "   and client.id = :clientId" +
             "   and active= true";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(myQuery, String.class)
        .setParameterList("accountingSchemaIds", getAccountingSchemaIds(clientId, adOrgId))
        .setParameter("clientId", clientId)
        .list();
  }

  private static List<String> getAccountingSchemaIds(final String clientId, final String orgIg) {
    //@formatter:off
    final String myQuery = 
              "select distinct accountingSchema.id" +
              "  from OrganizationAcctSchema" +
              " where client.id = :clientId" +
              "   and active= true" +
              "   and organization.id in (:orgIds)";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(myQuery, String.class)
        .setParameter("clientId", clientId)
        .setParameterList("orgIds", new OrganizationStructureProvider().getNaturalTree(orgIg))
        .list();
  }

  private static HashMap<String, Integer> treatExceptions(final String myQuery,
      final String recordId, final String tableId, final Set<String> orgIds, final String clientId,
      final Date periodStartingDate, final Date periodEndingDate, final String calendarId,
      final String parameterDateFrom, final String parameterDateTo, final String dbt,
      final String orgPeriodControl, final String targetOrganizationId) {
    HashMap<String, Integer> results = new HashMap<>();
    try {
      results.put("deleted", 0);
      results.put("updated", 0);
      final Query<String> query = OBDal.getInstance()
          .getSession()
          .createQuery(myQuery, String.class)
          .setParameterList("orgIds", orgIds)
          .setParameter("clientId", clientId)
          .setParameter("dbt", dbt)
          .setParameter("tableId", tableId)
          .setParameter("dateFrom",
              StringUtils.isNotEmpty(parameterDateFrom) ? OBDateUtils.getDate(parameterDateFrom)
                  : periodStartingDate)
          .setParameter("dateTo",
              StringUtils.isNotEmpty(parameterDateTo) ? OBDateUtils.getDate(parameterDateTo)
                  : periodEndingDate)
          .setParameter("organizationId", targetOrganizationId);
      if (recordId != null && !"".equals(recordId)) {
        query.setParameter("recordId", recordId).setMaxResults(1);
      }

      final List<String> transactions = query.list();
      for (String transaction : transactions) {
        final List<AccountingFact> facts = OBDal.getInstance()
            .createCriteria(AccountingFact.class)
            .add(Restrictions.eq(AccountingFact.PROPERTY_RECORDID, transaction))
            .add(Restrictions.eq(AccountingFact.PROPERTY_TABLE,
                OBDal.getInstance().get(Table.class, tableId)))
            .add(Restrictions.eq(AccountingFact.PROPERTY_CLIENT,
                OBDal.getInstance().get(Client.class, clientId)))
            .list();
        final Set<Date> exceptionDates = new HashSet<>();
        for (AccountingFact fact : facts) {
          if (periodStartingDate.compareTo(fact.getAccountingDate()) != 0
              || periodEndingDate.compareTo(fact.getAccountingDate()) != 0) {
            exceptionDates.add(fact.getAccountingDate());
          }
        }
        if (checkDates(exceptionDates, clientId, orgIds, facts.get(0).getDocumentCategory(),
            calendarId, parameterDateFrom, parameterDateTo, orgPeriodControl)) {
          final List<String> toDelete = new ArrayList<>();
          toDelete.add(transaction);
          results = delete(toDelete, tableId, clientId);
        } else {
          if (recordId != null && !"".equals(recordId)) {
            throw new OBException("@PeriodClosedForUnPosting@");
          }
        }
      }
    } catch (final ParseException e) {
      log4j.error("treatExceptions - error parsing dates", e);
    }
    return results;
  }

  private static boolean checkDates(final Set<Date> exceptionDates, final String clientId,
      final Set<String> orgIds, final String documentCategory, final String calendarId,
      final String datefrom, final String dateto, final String orgPeriodControl) {
    final List<Period> openPeriods = getOpenPeriods(clientId, documentCategory, orgIds, calendarId,
        "", "", datefrom, dateto, orgPeriodControl);
    int validDates = 0;
    for (Period period : openPeriods) {
      for (Date date : exceptionDates) {
        if (date.compareTo(period.getStartingDate()) >= 0
            && date.compareTo(period.getEndingDate()) <= 0) {
          validDates++;
        }
      }
    }
    return exceptionDates.size() == validDates;
  }

  private static boolean hasProcessingColumn(final String strTableId) {
    //@formatter:off
    final String hql = 
            "select count(*)" +
            "  from ADColumn" +
            " where table.id = :tableId" +
            "   and lower(dBColumnName) = 'processing'";
    //@formatter:on

    return (OBDal.getInstance()
        .getSession()
        .createQuery(hql, Long.class)
        .setParameter("tableId", strTableId)
        .list()
        .get(0)
        .intValue() == 1);
  }

  private static Set<String> getLegalOrBusinessOrgsChilds(final String clientId,
      final String orgId) {
    //@formatter:off
    final String hql =
                  "select o1.id" +
                  "  from Organization as o1" +
                  "    , Organization as o2" +
                  "      join o2.organizationType as ot" +
                  " where o2.client.id = :clientId" +
                  "   and ad_isorgincluded(o2.id, :orgId, o2.client.id) <> -1" +
                  "   and ad_isorgincluded(o1.id, o2.id, o1.client.id) <> -1" +
                  "   and (" +
                  "     ot.legalEntity = true" +
                  "     or ot.businessUnit = true" +
                  "   )" +
                  "   and o2.active = true" +
                  "   and o2.ready = true" +
                  " order by o2.name" +
                  "   , ad_isorgincluded(o1.id, o2.id, o1.client.id)" +
                  "   , o1.name";
    //@formatter:on

    return new HashSet<>(OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("clientId", clientId)
        .setParameter("orgId", orgId)
        .list());
  }
}
