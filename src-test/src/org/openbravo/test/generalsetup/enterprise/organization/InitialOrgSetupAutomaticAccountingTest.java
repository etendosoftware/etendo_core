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
 * All portions are Copyright (C) 2026 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.hibernate.criterion.Restrictions;
import org.junit.Test;
import org.openbravo.base.structure.BaseOBObject;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.businessUtility.InitialOrgSetup;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.model.common.enterprise.OrganizationAcctSchema;
import org.openbravo.model.financialmgmt.calendar.PeriodControl;
import org.openbravo.model.financialmgmt.tax.TaxCategory;
import org.openbravo.model.financialmgmt.tax.TaxRate;
import org.openbravo.model.financialmgmt.tax.TaxRateAccounts;
import org.openbravo.test.base.OBBaseTest;

public class InitialOrgSetupAutomaticAccountingTest extends OBBaseTest {

  @Test
  public void legalWithAccountingOrgIsAutomaticallyWiredAndReadied() {
    ADOrgPersistInfoUtility.setTestContextFB();

    InitialOrgSetup initialOrg = new InitialOrgSetup(OBContext.getOBContext().getCurrentClient());
    OBError result = initialOrg.createOrganizationWithAutomaticAccounting(
        "Auto EUR " + System.currentTimeMillis(), "",
        ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHACCOUNTING,
        ADOrgPersistInfoConstants.ORG_FB_FBGROUP, "", "", "",
        ADOrgPersistInfoConstants.CUR_EURO);

    assertEquals("Success", result.getType());

    Organization org = OBDal.getInstance().get(Organization.class, initialOrg.getOrgId());
    assertNotNull(org);
    assertEquals(ADOrgPersistInfoConstants.ORGTYPE_LEGALWITHACCOUNTING,
        org.getOrganizationType().getId());
    assertEquals(ADOrgPersistInfoConstants.CUR_EURO, org.getCurrency().getId());
    assertTrue(org.isAllowPeriodControl());
    assertTrue(org.isReady());
    assertNotNull(org.getGeneralLedger());
    assertNotNull(org.getCalendar());
    assertNotNull(org.getPeriodControlAllowedOrganization());
    assertNotNull(org.getCalendarOwnerOrganization());
    assertNotNull(org.getInheritedCalendar());

    assertTrue(count(OrganizationAcctSchema.class, "organization.id", org.getId()) > 0);
    assertTrue(count(PeriodControl.class, "organization.id", org.getId()) > 0);
    assertTrue(count(TaxCategory.class, "organization.id", org.getId()) > 0);
    assertTrue(count(TaxRate.class, "organization.id", org.getId()) > 0);
    assertTrue(countTaxAccounts(org.getId()) > 0);

    assertEquals(org.getId(), org.getPeriodControlAllowedOrganization().getId());
    assertEquals(org.getId(), org.getCalendarOwnerOrganization().getId());
    assertEquals(org.getCalendar().getId(), org.getInheritedCalendar().getId());
  }

  private <T extends BaseOBObject> long count(Class<T> entityClass, String propertyPath,
      String value) {
    OBCriteria<T> criteria = OBDal.getInstance().createCriteria(entityClass);
    criteria.setFilterOnReadableClients(false);
    criteria.setFilterOnReadableOrganization(false);
    criteria.add(Restrictions.eq(propertyPath, value));
    return criteria.count();
  }

  private long countTaxAccounts(String orgId) {
    return OBDal.getInstance().createQuery(TaxRateAccounts.class,
        "as e where e.tax.organization.id = :orgId")
        .setNamedParameter("orgId", orgId)
        .count();
  }
}
