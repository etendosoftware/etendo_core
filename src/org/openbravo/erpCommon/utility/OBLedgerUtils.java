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
 * All portions are Copyright (C) 2016-2020 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.security.OrganizationStructureProvider;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;

/**
 * Utilities to get AcctSchema
 */
public class OBLedgerUtils {
  private static Logger log4j = LogManager.getLogger();

  /**
   * Returns the ledger id for the given organization id.
   * 
   * If the org id is empty, it returns null. If the given organization has no ledger, it tries to
   * get its legal entity's ledger. If not found, it returns the organization client's ledger
   * 
   * @param orgId
   *          Organization Id whose ledger is needed
   * 
   * @return String ledgerId ledger id for the given organization. Null if not found
   */
  public static String getOrgLedger(final String orgId) {
    try {
      OBContext.setAdminMode(true);

      if (StringUtils.isBlank(orgId)) {
        // No organization
        return null;
      }

      final Organization org = OBDal.getInstance().get(Organization.class, orgId);
      if (org == null) {
        // No organization
        return null;
      }

      final String acctSchemaId = getOrgLedgerRecursive(org);
      if (StringUtils.isNotEmpty(acctSchemaId)) {
        // Get general ledger of organization tree
        return acctSchemaId;
      }

      final String clientId = StringUtils.equals(orgId, "0")
          ? OBContext.getOBContext().getCurrentClient().getId()
          : org.getClient().getId();
      // Get client base general ledger
      return getClientLedger(clientId);

    } catch (final Exception e) {
      log4j.error("Impossible to get ledger for organization id " + orgId, e);
      return null;
    } finally {
      OBContext.restorePreviousMode();
    }
  }

  /**
   * If the Organization has a General Ledger defined return its id. If not get the parent
   * organization list and loop through it until a organization with a GeneralLedger defined is
   * found. In case none has it defined return null.
   * 
   * @param org
   *          the Organization whose General Ledger is required.
   * @return the General Ledger Id of the organization in case the organization or one of its parent
   *         has a General Ledger defined.
   */
  private static String getOrgLedgerRecursive(final Organization org) {

    if (org.getGeneralLedger() != null) {
      // Get general ledger of organization
      return org.getGeneralLedger().getId();
    }

    if (StringUtils.equals(org.getId(), "0")) {
      // * organization doesn't have parents
      return null;
    }

    // Loop through parent organization list
    final OrganizationStructureProvider osp = OBContext.getOBContext()
        .getOrganizationStructureProvider(org.getClient().getId());
    final List<String> parentOrgIds = osp.getParentList(org.getId(), false);
    for (String orgId : parentOrgIds) {
      final Organization parentOrg = OBDal.getInstance().get(Organization.class, orgId);
      if (parentOrg.getGeneralLedger() != null) {
        return parentOrg.getGeneralLedger().getId();
      }
    }

    return null;
  }

  private static String getClientLedger(final String clientId) {
    //@formatter:off
    final String hql =
                  "select id" +
                  "  from FinancialMgmtAcctSchema" +
                  " where client.id = :clientId" +
                  " order by name";
    //@formatter:on

    return OBDal.getInstance()
        .getSession()
        .createQuery(hql, String.class)
        .setParameter("clientId", clientId)
        .setMaxResults(1)
        .uniqueResult();
  }
}
