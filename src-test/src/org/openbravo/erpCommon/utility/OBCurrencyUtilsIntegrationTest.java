/*
 *************************************************************************
 * The contents of this file are subject to the Etendo License
 * (the "License"), you may not use this file except in compliance with
 * the License.
 * You may obtain a copy of the License at
 * https://github.com/etendosoftware/etendo_core/blob/main/legal/Etendo_license.txt
 * Software distributed under the License is distributed on an
 * "AS IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing rights
 * and limitations under the License.
 * All portions are Copyright (C) 2021-2026 FUTIT SERVICES, S.L
 * All Rights Reserved.
 * Contributor(s): Futit Services S.L.
 *************************************************************************
 */
package org.openbravo.erpCommon.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.common.enterprise.Organization;
import org.openbravo.test.base.OBBaseTest;
import org.openbravo.test.base.TestConstants;

/**
 * Integration tests for {@link OBCurrencyUtils#getOrgCurrency(String)}.
 * Requires a running database with F&B sample data.
 */
public class OBCurrencyUtilsIntegrationTest extends OBBaseTest {

  // F&B US, Inc. has currency USD (100)
  private static final String ORG_US = TestConstants.Orgs.US;
  // F&B España, S.A has currency EUR (102)
  private static final String ORG_ESP = TestConstants.Orgs.ESP;
  // F&B US East Coast — no direct currency, inherits from legal entity US
  private static final String ORG_US_EAST = TestConstants.Orgs.US_EST;
  // F&B US West Coast — no direct currency, inherits from legal entity US
  private static final String ORG_US_WEST = TestConstants.Orgs.US_WEST;

  @Test
  public void testOrgWithDirectCurrencyReturnsIt() {
    setSystemAdministratorContext();
    String currency = OBCurrencyUtils.getOrgCurrency(ORG_US);
    assertNotNull("US org should have a currency", currency);
    assertEquals("100", currency); // USD
  }

  @Test
  public void testSpainOrgReturnsCurrencyEUR() {
    setSystemAdministratorContext();
    String currency = OBCurrencyUtils.getOrgCurrency(ORG_ESP);
    assertNotNull("Spain org should have a currency", currency);
    assertEquals("102", currency); // EUR
  }

  @Test
  public void testChildOrgInheritsFromLegalEntity() {
    setSystemAdministratorContext();
    // US East Coast has no direct currency, should fall back to US legal entity (USD)
    String currency = OBCurrencyUtils.getOrgCurrency(ORG_US_EAST);
    assertNotNull("Child org should inherit currency from legal entity", currency);
    assertEquals("100", currency); // USD from parent
  }

  @Test
  public void testChildOrgWestCoastInheritsFromLegalEntity() {
    setSystemAdministratorContext();
    String currency = OBCurrencyUtils.getOrgCurrency(ORG_US_WEST);
    assertNotNull("West Coast org should inherit currency", currency);
    assertEquals("100", currency); // USD from parent
  }

  @Test
  public void testNullOrgIdReturnsNull() {
    setSystemAdministratorContext();
    assertNull(OBCurrencyUtils.getOrgCurrency(null));
  }

  @Test
  public void testEmptyOrgIdReturnsNull() {
    setSystemAdministratorContext();
    assertNull(OBCurrencyUtils.getOrgCurrency(""));
  }

  @Test
  public void testNonExistentOrgReturnsNull() {
    setSystemAdministratorContext();
    assertNull(OBCurrencyUtils.getOrgCurrency("NONEXISTENT_ID_12345"));
  }

  @Test
  public void testSystemOrgFallsBackToClientCurrency() {
    setSystemAdministratorContext();
    // Org "0" (*) has no currency, should fall back to client base currency
    OBContext.setOBContext(TestConstants.Users.ADMIN, TestConstants.Roles.FB_GRP_ADMIN,
        TestConstants.Clients.FB_GRP, TestConstants.Orgs.FB_GROUP);
    String currency = OBCurrencyUtils.getOrgCurrency("0");
    assertNotNull("System org should fall back to client currency", currency);
  }
}
