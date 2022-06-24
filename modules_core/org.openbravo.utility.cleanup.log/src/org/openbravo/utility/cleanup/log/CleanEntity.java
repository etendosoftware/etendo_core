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
 * All portions are Copyright (C) 2013-2019 Openbravo SLU
 * All Rights Reserved. 
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.utility.cleanup.log;

import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.client.kernel.ComponentProvider.Qualifier;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.system.Client;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.scheduling.ProcessLogger;

/**
 * Default implementation to clean up an entity.
 * 
 * In case specific actions are required for a concrete entity, this class can be extended using as
 * {@code Qualifier} the name of the entity, in this way that class will be invoked instead of this
 * default one.
 * 
 * @author alostale
 * 
 */
@ApplicationScoped
@Qualifier("Default")
public class CleanEntity {
  protected static final String SYSTEM = "0";

  private static final Logger log = LogManager.getLogger();

  /**
   * Clean logs for an entity
   * 
   * @param config
   *          Configuration regarding how the entity should be cleaned up, such as the number of
   *          days of info to keep, where clauses, etc.
   * @param client
   *          Client to remove data from, in case it is System, all clients will be cleaned up
   * @param org
   *          If client is not System, data from this organization and the ones in its tree will be
   *          removed
   * @param bgLogger
   *          Log to be displayed in Process Request window
   * @return number of deleted rows
   */
  public int clean(LogCleanUpConfig config, Client client, Organization org,
      ProcessLogger bgLogger) {
    Entity entity = ModelProvider.getInstance().getEntityByTableId(config.getTable().getId());


    String hql = "delete from " + entity.getName();

    // An alias can be added to the main table, so it can be referenced from HQL subqueries,
    // See issue https://issues.openbravo.com/view.php?id=41977
    if (config.getEntityAlias() != null && StringUtils.isAlphanumeric(config.getEntityAlias())) {
      hql += " " + config.getEntityAlias();
    }

    String where = "";
    if (config.getOlderThan() != 0L) {
      String prop;
      if (config.getColumn() == null) {
        prop = entity.getPropertyByColumnName("created").getName();
      } else {
        prop = entity.getPropertyByColumnName(config.getColumn().getDBColumnName()).getName();
      }
      String since = prop + " < now() - " + config.getOlderThan();
      if (!StringUtils.isEmpty(since)) {
        where = " where " + since;
      }
    }

    if (!StringUtils.isEmpty(config.getHQLWhereClause())) {
      where += StringUtils.isEmpty(where) ? " where " : " and ";
      where += "(" + config.getHQLWhereClause() + ")";
    }

    String clientOrgFilter = getClientOrgFilter(client, org);
    if (!StringUtils.isEmpty(clientOrgFilter)) {
      where += StringUtils.isEmpty(where) ? " where " : " and ";
      where += clientOrgFilter;
    }

    hql += where;

    log.debug("  Query: {}", hql);

    Session s = OBDal.getInstance().getSession();
    int affectedRows = 0;
    try {
      affectedRows = s.createQuery(hql).executeUpdate();
    } catch (Exception e) {
      log.error("Error executing cleanup query \"{}\"", hql, e);
      bgLogger.log("Error executing cleanup query \"" + hql + "\":  " + e.getMessage() + "\n");
    }
    String logMsg = "Deleted " + affectedRows + " rows";

    log.debug(logMsg);
    bgLogger.log(logMsg + "\n");
    return affectedRows;
  }

  /** Returns the where clause to add to the HQL query to add client/org filtering */
  protected String getClientOrgFilter(Client client, Organization org) {
    String clientId = client.getId();
    if (SYSTEM.equals(clientId)) {
      return "";
    }

    String filter = "client.id = '" + clientId + "'";

    String orgId = org.getId();
    if (!SYSTEM.equals(orgId)) {
      OrganizationStructureProvider orgTree = OBContext.getOBContext()
          .getOrganizationStructureProvider(clientId);

      String orgFilter = "";
      for (String childOrg : orgTree.getChildTree(orgId, true)) {
        if (!StringUtils.isEmpty(orgFilter)) {
          orgFilter += ", ";
        }
        orgFilter += "'" + childOrg + "'";
      }
      orgFilter = " and organization.id in (" + orgFilter + ")";
      filter += orgFilter;
    }

    return filter;
  }
}
