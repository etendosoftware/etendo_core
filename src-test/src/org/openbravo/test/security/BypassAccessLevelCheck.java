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
 * All portions are Copyright (C) 2016 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package org.openbravo.test.security;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.openbravo.base.exception.OBSecurityException;
import org.openbravo.dal.core.OBContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.model.ad.access.Role;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.test.base.OBBaseTest;

/**
 * By default access level in entity and role is checked preventing reading data if role's user
 * level is Organization and the entity trying to be accessed is Client or System. These checks can
 * be bypassed.
 * 
 * @author alostale
 *
 */
public class BypassAccessLevelCheck extends OBBaseTest {
  private static String ORG_LEVEL_ROLE;
  private static final String CURRENCY_WINDOW = "115";
  private static final String SPAIN_ORG = "357947E87C284935AD1D783CF6F099A1";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @BeforeClass
  public static void createOrgLevelRole() {
    Role role = ExplicitCrossOrganizationReference.createOrgUserLevelRole();
    ORG_LEVEL_ROLE = role.getId();
    ExplicitCrossOrganizationReference.grantWindowAccess(role, CURRENCY_WINDOW);

    OBDal.getInstance().commitAndClose();
  }

  /** By default Org level roles cannot see data in System level entities */
  @Test
  public void orgLevelShouldntGrantAccessToSystemEntity() {
    OBContext.setOBContext("100", ORG_LEVEL_ROLE, QA_TEST_CLIENT_ID, SPAIN_ORG);

    assertThat("doOrgClientAccessCheck", OBContext.getOBContext().doOrgClientAccessCheck(),
        is(true));

    exception.expect(OBSecurityException.class);
    exception.expectMessage(containsString("Entity Currency is not readable"));

    OBDal.getInstance().createCriteria(Currency.class);
  }

  /** Default behavior of for access level check can be bypassed */
  @Test
  public void orgLevelCanAccessEntityAccessIfEnabled() {
    OBContext.setOBContext("100", ORG_LEVEL_ROLE, QA_TEST_CLIENT_ID, SPAIN_ORG);
    OBContext.getOBContext().setCheckAccessLevel(false);
    OBCriteria<Currency> q = OBDal.getInstance().createCriteria(Currency.class);

    assertThat("Visible currencies", q.count(), is(greaterThan(0)));
  }

  @AfterClass
  public static void cleanUpCreatedObjects() {
    CrossOrganizationReference.removeCreatedObjects();
  }
}
