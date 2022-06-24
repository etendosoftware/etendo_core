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
 * All portions are Copyright (C) 2018 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 *************************************************************************
 */
package org.openbravo.test.generalsetup.enterprise.organization;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openbravo.test.base.OBBaseTest;

/**
 * Test corner cases organization persist information
 */
public class ADOrgPersistInfoCornerCaseOrgTest extends OBBaseTest {

  @Test
  public void testCornerCaseStarOrganization() {
    runTest(ADOrgPersistInfoConstants.ORG_0);
  }

  @Test
  public void testCornerCaseNullOrganization() {
    runTest(null);
  }

  @Test
  public void testCornerCaseNonExistingOrganization() {
    runTest("XX");
  }

  private void runTest(String orgId) {
    assertEquals("AD_Org_GetCalendarOwner for " + orgId + " Organization",
        ADOrgPersistInfoUtility
            .runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETCALENDAROWNERTN, orgId),
        ADOrgPersistInfoUtility
            .runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETCALENDAROWNER, orgId));

    assertEquals("AD_Org_GetPeriodControlAllow for " + orgId + " Organization",
        ADOrgPersistInfoUtility
            .runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETPERIODCONTROLALLOWTN, orgId),
        ADOrgPersistInfoUtility
            .runFunction(ADOrgPersistInfoConstants.FUNCTION_AD_ORG_GETPERIODCONTROLALLOW, orgId));
  }
}
