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
 * All portions are Copyright (C) 2015 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.erpCommon.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.service.db.DalConnectionProvider;

/**
 * Utilities to get Currency
 */
public class OBCurrencyUtils {
  private static Logger log4j = LogManager.getLogger();

  /**
   * Returns the currency id for the given organization id.
   * 
   * If the org id is empty, it returns null. If the given organization has no currency, it tries to
   * get its legal entity's currency. If not found, it returns the organization client's currency
   * 
   * @param orgId
   *          Organization Id whose currency is needed
   * 
   * @return String currencyId currency id for the given organization. Null if not found
   */
  public static String getOrgCurrency(String orgId) {
    if (StringUtils.isBlank(orgId)) {
      return null;
    }

    OBContext.setAdminMode(true);
    try {
      final Organization org = OBDal.getInstance().get(Organization.class, orgId);
      if (org == null) {
        // No organization
        return null;
      } else if (org.getCurrency() != null) {
        // Get currency of organization
        return org.getCurrency().getId();
      } else {
        final Organization legalEntity = OBContext.getOBContext()
            .getOrganizationStructureProvider()
            .getLegalEntity(org);
        if (legalEntity != null && legalEntity.getCurrency() != null) {
          // Get currency from legal entity of organization
          return legalEntity.getCurrency().getId();
        } else {
          // Get client base currency
          return Utility.stringBaseCurrencyId(new DalConnectionProvider(false),
              StringUtils.equals(orgId, "0") ? OBContext.getOBContext().getCurrentClient().getId()
                  : org.getClient().getId());
        }
      }
    } catch (Exception e) {
      log4j.error("Impossible to get currency for organization id " + orgId, e);
    } finally {
      OBContext.restorePreviousMode();
    }

    return null;
  }
}
