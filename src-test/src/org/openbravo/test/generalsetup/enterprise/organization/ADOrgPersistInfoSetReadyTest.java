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

import org.junit.Test;
import org.openbravo.test.base.OBBaseTest;

public class ADOrgPersistInfoSetReadyTest extends OBBaseTest {

  /**
   * Create a new Organization of type Generic under F&amp;B España, S.A Organization and test
   * persist organization info after the organization is set as ready with Cascade No.
   */
  @Test
  public void testSetReadyOneGenericOrganizationNoCascade() {
    testSetReadyOneOrganization(false, ADOrgPersistInfoConstants.ORGTYPE_GENERIC);
  }

  /**
   * Create a new Organization of type Generic under F&amp;B España, S.A Organization and test
   * persist organization info after the organization is set as ready with Cascade Yes.
   */
  @Test
  public void testSetReadyOneGenericOrganizationCascade() {
    testSetReadyOneOrganization(true, ADOrgPersistInfoConstants.ORGTYPE_GENERIC);
  }

  /**
   * Create a new Organization of type Organization under * Organization and test persist
   * organization information after the organization is set as ready cascade as No.
   */
  @Test
  public void testSetReadyOrganizationTypeNoCascade() {
    testSetReadyOneOrganization(false, ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION);
  }

  /**
   * Create a new Organization of type Organization under * Organization and test persist
   * organization information after the organization is set as ready with cascade as Yes.
   */
  @Test
  public void testSetReadyOrganizationCascade() {
    testSetReadyOneOrganization(true, ADOrgPersistInfoConstants.ORGTYPE_ORGANIZATION);
  }

  private void testSetReadyOneOrganization(boolean isCascade, final String orgType) {
    ADOrgPersistInfoUtility.setTestContextFB();
    String orgId = ADOrgPersistInfoUtility.createOrganization(orgType,
        ADOrgPersistInfoConstants.ORG_FB_SPAIN, true, ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(orgId, isCascade ? "Y" : "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(orgId);
  }

  /**
   * Create a new Organization of type Generic under F&amp;B España, S.A Organization and a child
   * under it and test persist organization info after the organization is set as ready with cascade
   * as No.
   */
  @Test
  public void testSetReadyTwoGenericOrganizationNoCascade() {
    ADOrgPersistInfoUtility.setTestContextFB();
    String firstOrgId = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, ADOrgPersistInfoConstants.ORG_FB_SPAIN, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    String secondOrgId = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, firstOrgId, false,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(firstOrgId, "N");
    ADOrgPersistInfoUtility.setAsReady(secondOrgId, "N");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(firstOrgId);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(secondOrgId);
  }

  /**
   * Create a new Organization of type Generic under F&amp;B España, S.A Organization and a child
   * under it and test persist organization info after the organization is set as ready with cascade
   * as Yes.
   */
  @Test
  public void testSetReadyTwoGenericOrganizationCascade() {
    ADOrgPersistInfoUtility.setTestContextFB();
    String firstOrgId = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, ADOrgPersistInfoConstants.ORG_FB_SPAIN, true,
        ADOrgPersistInfoConstants.CUR_EURO);
    String secondOrgId = ADOrgPersistInfoUtility.createOrganization(
        ADOrgPersistInfoConstants.ORGTYPE_GENERIC, firstOrgId, false,
        ADOrgPersistInfoConstants.CUR_EURO);
    ADOrgPersistInfoUtility.setAsReady(firstOrgId, "Y");
    ADOrgPersistInfoUtility.assertPersistOrgInfo(firstOrgId);
    ADOrgPersistInfoUtility.assertPersistOrgInfo(secondOrgId);
  }

}
