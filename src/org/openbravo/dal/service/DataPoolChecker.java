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
 * All portions are Copyright (C) 2018-2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.dal.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.query.Query;
import org.openbravo.base.provider.OBProvider;
import org.openbravo.base.provider.OBSingleton;
import org.openbravo.database.ExternalConnectionPool;

/**
 * Helper class used to determine if a report should use the read-only pool to retrieve data
 */
public class DataPoolChecker implements OBSingleton {
  private static final Logger log = LogManager.getLogger();
  private static final int REPORT_ID = 0;
  private static final int DATA_POOL = 1;

  private Map<String, String> reportPoolMap = new HashMap<>();
  private final List<String> validPoolValues = Arrays.asList(ExternalConnectionPool.DEFAULT_POOL,
      ExternalConnectionPool.READONLY_POOL);
  private String defaultReadOnlyPool = ExternalConnectionPool.READONLY_POOL;

  private static DataPoolChecker instance;

  public static synchronized DataPoolChecker getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(DataPoolChecker.class);
      instance.initialize();
    }
    return instance;
  }

  /**
   * Initializes the checker caching the list of reports with a database pool assigned and the
   * default preference as well
   */
  private void initialize() {
    refreshDefaultPoolPreference();
    refreshDataPoolProcesses();
  }

  /**
   * Reload from DB the reports that should use the Read-only pool
   */
  public void refreshDataPoolProcesses() {
    reportPoolMap = findActiveDataPoolSelection();
  }

  /**
   * Queries for all active entries of DataPoolSelection and returns them in a new map object. This
   * avoids concurrency issues dealing with the cached Report-Pool mapping.
   * 
   * @return a new Map object with the mapping (Report_ID, POOL)
   */
  private Map<String, String> findActiveDataPoolSelection() {
    //@formatter:off
    String hql = 
            "select dps.report.id, dps.dataPool " +
            "  from OBUIAPP_Data_Pool_Selection dps " +
            " where dps.active = true";
    //@formatter:on
    Query<Object[]> query = OBDal.getInstance().getSession().createQuery(hql, Object[].class);
    List<Object[]> queryResults = query.list();

    Map<String, String> selection = new HashMap<>(queryResults.size());
    for (Object[] values : queryResults) {
      selection.put(values[REPORT_ID].toString(), values[DATA_POOL].toString());
    }
    return selection;
  }

  private void refreshDefaultPoolPreference() {
    //@formatter:off
    String hql = 
            "select p.searchKey " +
            "  from ADPreference p " +
            " where p.property='OBUIAPP_DefaultDBPoolForReports' " +
            "   and p.active = true " +
            "   and p.visibleAtClient.id = '0' " +
            "   and p.visibleAtOrganization.id = '0' ";
    //@formatter:on
    Query<String> defaultPoolQuery = OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setMaxResults(1);
    setDefaultReadOnlyPool(defaultPoolQuery.uniqueResult());
  }

  /**
   * Set the default pool used when requesting the read-only pool
   *
   * @param defaultPool
   *          the ID of the default pool returned when requesting a read-only instance
   */
  private void setDefaultReadOnlyPool(String defaultPool) {
    if (validPoolValues.contains(defaultPool)) {
      log.debug("Pool {} is set as the default to use with reports", defaultPool);
      defaultReadOnlyPool = defaultPool;
    } else {
      log.warn(
          "Preference value {} is not a valid Database pool. Using READONLY_POOL as the default value",
          defaultPool);
    }
  }

  /**
   * Verifies whether the current report should use the default pool. Reports can be defined either
   * in Report and Process or in Process Definition.
   *
   * @return true if the current report should use the default pool
   */
  boolean shouldUseDefaultPool(String processId) {
    String poolForProcess = null;
    if (!StringUtils.isBlank(processId)) {
      poolForProcess = reportPoolMap.get(processId);
    }

    String poolUsedForProcess = poolForProcess != null ? poolForProcess : defaultReadOnlyPool;

    if (processId != null) {
      log.debug("Using pool {} for report with id {}", poolUsedForProcess, processId);
    }

    return ExternalConnectionPool.DEFAULT_POOL.equals(poolUsedForProcess);
  }

}
